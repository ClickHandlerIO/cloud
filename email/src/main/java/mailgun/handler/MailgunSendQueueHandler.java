package mailgun.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import common.handler.EmailSendQueueHandler;
import common.handler.FileGetHandler;
import common.service.FileService;
import entity.EmailAttachmentEntity;
import entity.EmailEntity;
import io.clickhandler.sql.db.SqlExecutor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import mailgun.config.MailgunConfig;
import mailgun.data.MailgunSendRequest;
import mailgun.data.json.MailgunSendResponse;
import mailgun.event.MailgunEmailSentEvent;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Brad Behnke
 */
public class MailgunSendQueueHandler extends EmailSendQueueHandler<MailgunSendRequest> {

    private final static Logger LOG = LoggerFactory.getLogger(MailgunSendQueueHandler.class);
    private final EventBus eventBus;
    private final HttpClient client;
    private final String host;
    private final String domain;
    private final String apiKey;
    private final int ALLOWED_ATTEMPTS;

    public MailgunSendQueueHandler(MailgunConfig config, EventBus eventBus, SqlExecutor db, FileService fileService) {
        super(db, fileService);
        this.eventBus = eventBus;
        this.domain = config.getDomain();
        this.host = "api.mailgun.net/v3";
        this.apiKey = config.getApiKey();
        this.ALLOWED_ATTEMPTS = config.getSendRetryMax();
        this.client = Vertx.vertx().createHttpClient(new HttpClientOptions()
                .setSsl(true)
                .setTrustAll(true)
                .setDefaultHost(host)
                .setDefaultPort(443)
                .setKeepAlive(true));
    }

    @Override
    public void receive(List<MailgunSendRequest> sendRequests) {
        sendRequests.forEach(this::sendEmail);
    }

    private void sendEmail(final MailgunSendRequest sendRequest) {
        sendRequest.incrementAttempts();
        final HttpClientRequest clientRequest = client.post("/"+domain+"/messages")
                .handler(new HttpResponseHandler(sendRequest))
                .exceptionHandler(throwable -> failure(sendRequest,throwable))
                .setChunked(true);
        clientRequest.putHeader(HttpHeaders.AUTHORIZATION, "api:"+apiKey);
        clientRequest.putHeader(HttpHeaders.CONTENT_TYPE, ContentType.MULTIPART_FORM_DATA.getMimeType() + "; boundary=" + MultiPartUtility.BOUNDARY);
        writeBodyObservable(clientRequest, sendRequest)
                .doOnError(throwable -> {
                    if(sendRequest.getCompletionHandler() != null) {
                        sendRequest.getCompletionHandler().handle(Future.failedFuture(throwable));
                    }
                })
                .doOnNext(aVoid -> {
                    if(sendRequest.getEmailEntity().isAttachments()) {
                        writeAttachmentsObservable(clientRequest, sendRequest)
                                .doOnError(throwable -> failure(sendRequest,throwable))
                                .doOnNext(aVoid1 -> clientRequest.end());
                    } else {
                        clientRequest.end();
                    }
                });
    }

    private Observable<Void> writeBodyObservable(HttpClientRequest clientRequest, MailgunSendRequest sendRequest) {
        ObservableFuture<Void> observableFuture = RxHelper.observableFuture();
        writeBody(clientRequest, sendRequest, observableFuture.toHandler());
        return observableFuture;
    }

    private void writeBody(HttpClientRequest clientRequest, MailgunSendRequest sendRequest, Handler<AsyncResult<Void>> completionHandler) {
        MultiPartUtility mpu = new MultiPartUtility();
        // TODO build with mpu

        // write body
        clientRequest.write(mpu.get());
    }

    private Observable<Void> writeAttachmentsObservable(HttpClientRequest clientRequest, MailgunSendRequest sendRequest) {
        ObservableFuture<Void> observableFuture = RxHelper.observableFuture();
        writeAttachments(clientRequest, sendRequest, observableFuture.toHandler());
        return observableFuture;
    }

    private void writeAttachments(HttpClientRequest clientRequest, MailgunSendRequest sendRequest, Handler<AsyncResult<Void>> completionHandler) {
        getAttachmentEntitiesObservable(sendRequest.getEmailEntity())
                .doOnError(throwable -> {
                    if(completionHandler != null) {
                        completionHandler.handle(Future.failedFuture(throwable));
                    }
                })
                .doOnNext(attachmentEntities -> {
                    final AtomicInteger activeDownloads = new AtomicInteger();
                    final AtomicBoolean failed = new AtomicBoolean();
                    for(final EmailAttachmentEntity attachmentEntity:attachmentEntities) {
                        if(failed.get())
                            break;
                        final MultiPartUtility mpu = new MultiPartUtility();
                        activeDownloads.incrementAndGet();
                        downloadFile(attachmentEntity.getFileId(), new FileGetHandler() {
                            @Override
                            public void chunkReceived(Buffer chunk) {
                                if(mpu.length() == 0) {
                                    mpu.attachment(attachmentEntity.getName(), chunk);
                                    return;
                                }
                                mpu.append(chunk);
                            }

                            @Override
                            public void onComplete() {
                                clientRequest.write(mpu.get());
                                if(activeDownloads.decrementAndGet() <= 0 && completionHandler != null) {
                                    completionHandler.handle(Future.succeededFuture());
                                }
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                failed.set(true);
                                if (completionHandler != null) {
                                    completionHandler.handle(Future.failedFuture(throwable));
                                }
                            }
                        });
                    }
                });
    }

    private void publishEvent(EmailEntity emailEntity, boolean success) {
        eventBus.publish(MailgunEmailSentEvent.ADDRESS, new MailgunEmailSentEvent(emailEntity, success));
    }

    protected class MultiPartUtility {
        public final static String BOUNDARY = "===boundary===";
        private static final String LINE_FEED = "\r\n";
        private String charset = "UTF-8";
        private Buffer buffer;
        private String attachmentName = "attachment";

        public MultiPartUtility() {
            this.buffer = Buffer.buffer();
        }

        public MultiPartUtility formField(String name, String value) {
            buffer.appendString("--" + BOUNDARY).appendString(LINE_FEED);
            buffer.appendString("Content-Disposition: form-data; name=\"" + name + "\"").appendString(LINE_FEED);
            buffer.appendString("Content-Type: text/plain; charset=" + charset).appendString(LINE_FEED);
            buffer.appendString(LINE_FEED);
            buffer.appendString(value).appendString(LINE_FEED);
            return this;
        }

        public MultiPartUtility attachment(String fileName, Buffer firstBuffer) {
            buffer.appendString("--" + BOUNDARY).appendString(LINE_FEED);
            buffer.appendString("Content-Disposition: form-data; name=\"" + attachmentName + "\"; filename=\"" + fileName + "\"")
                    .appendString(LINE_FEED);
            buffer.appendString("Content-Type: " + URLConnection.guessContentTypeFromName(fileName))
                    .appendString(LINE_FEED);
            buffer.appendString("Content-Transfer-Encoding: binary").appendString(LINE_FEED);
            buffer.appendString(LINE_FEED);
            buffer.appendBuffer(firstBuffer);
            return this;
        }

        public MultiPartUtility append(Buffer chunk) {
            buffer.appendBuffer(chunk);
            return  this;
        }

        public Buffer get() {
            return buffer;
        }

        public int length() {
            return buffer.length();
        }
    }


    protected class HttpResponseHandler implements Handler<HttpClientResponse> {

        private MailgunSendRequest sendRequest;
        private Buffer totalBuffer;

        public HttpResponseHandler(MailgunSendRequest sendRequest) {
            this.sendRequest = sendRequest;
            this.totalBuffer = Buffer.buffer();
        }

        @Override
        public void handle(HttpClientResponse httpClientResponse) {
            if(httpClientResponse.statusCode() == HttpStatus.SC_OK) {
                httpClientResponse.bodyHandler(buffer -> {
                    totalBuffer.appendBuffer(buffer);
                });
                httpClientResponse.endHandler(aVoid -> {
                    try {
                        // todo make mapper global static
                        MailgunSendResponse response = new ObjectMapper().readValue(totalBuffer.toString(), MailgunSendResponse.class);
                        if(response == null || response.getId() == null || response.getId().isEmpty()) {
                            failure(sendRequest, new Exception("Invalid Response to Send Request"));
                            return;
                        }
                        EmailEntity emailEntity = sendRequest.getEmailEntity();
                        emailEntity.setMessageId(response.getId());
                        updateRecords(emailEntity);
                        publishEvent(emailEntity, true);
                        success(sendRequest, emailEntity);
                    } catch (Throwable throwable) {
                        failure(sendRequest, throwable);
                    }
                });
            } else {
                if(sendRequest.getAttempts() < ALLOWED_ATTEMPTS) {
                    sendEmail(sendRequest);
                } else {
                    updateRecords(sendRequest.getEmailEntity());
                    publishEvent(sendRequest.getEmailEntity(), false);
                    failure(sendRequest, new Exception("Failed to Send Email Using " + ALLOWED_ATTEMPTS + " Attempts with Response Code: " + httpClientResponse.statusCode()));
                }
            }
        }
    }
}
