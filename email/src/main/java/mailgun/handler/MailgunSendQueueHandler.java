package mailgun.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import common.handler.EmailSendQueueHandler;
import common.service.FileAttachmentDownloadService;
import entity.EmailAttachmentEntity;
import entity.EmailEntity;
import io.clickhandler.sql.db.SqlExecutor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
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
    private final FileAttachmentDownloadService downloadService;
    private final HttpClient client;
    private final String host;
    private final String domain;
    private final String apiKey;
    private final int ALLOWED_ATTEMPTS;

    public MailgunSendQueueHandler(MailgunConfig config, EventBus eventBus, SqlExecutor db, FileAttachmentDownloadService downloadService) {
        super(db);
        this.eventBus = eventBus;
        this.downloadService = downloadService;
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

    private void sendEmail(MailgunSendRequest sendRequest) {
        MultiPartUtility mpu = new MultiPartUtility();
        sendRequest.incrementAttempts();
        HttpClientRequest clientRequest = client.post("/"+domain+"/messages")
                .handler(new HttpResponseHandler(sendRequest))
                .exceptionHandler(throwable -> failure(sendRequest,throwable))
                .setChunked(true);
        clientRequest.putHeader(HttpHeaders.AUTHORIZATION, "api:"+apiKey);
        clientRequest.putHeader(HttpHeaders.CONTENT_TYPE, ContentType.MULTIPART_FORM_DATA.getMimeType() + "; boundary=" + mpu.getBoundary());
        writeBodyObservable(clientRequest, sendRequest, mpu)
                .doOnError(throwable -> failure(sendRequest,throwable))
                .doOnNext(clientRequest1 -> {
                    if(sendRequest.getEmailEntity().isAttachments()) {
                        mpu.clear();
                        writeAttachmentsObservable(clientRequest1, sendRequest, mpu)
                                .doOnError(throwable -> failure(sendRequest,throwable))
                                .doOnNext(HttpClientRequest::end);
                    } else {
                        clientRequest1.end();
                    }
                });
    }

    private Observable<HttpClientRequest> writeBodyObservable(HttpClientRequest clientRequest, MailgunSendRequest sendRequest, MultiPartUtility mpu) {
        ObservableFuture<HttpClientRequest> observableFuture = RxHelper.observableFuture();
        writeBody(clientRequest, sendRequest, mpu, observableFuture.toHandler());
        return observableFuture;
    }

    private void writeBody(HttpClientRequest clientRequest, MailgunSendRequest sendRequest, MultiPartUtility mpu, Handler<AsyncResult<HttpClientRequest>> completionHandler) {
        // TODO build

        // write body
        clientRequest.write(mpu.get());
    }

    private Observable<HttpClientRequest> writeAttachmentsObservable(HttpClientRequest clientRequest, MailgunSendRequest sendRequest, MultiPartUtility mpu) {
        ObservableFuture<HttpClientRequest> observableFuture = RxHelper.observableFuture();
        writeAttachments(clientRequest, sendRequest, mpu, observableFuture.toHandler());
        return observableFuture;
    }

    private void writeAttachments(HttpClientRequest clientRequest, MailgunSendRequest sendRequest, MultiPartUtility mpu, Handler<AsyncResult<HttpClientRequest>> completionHandler) {
        getAttachmentEntitiesObservable(sendRequest.getEmailEntity())
                .doOnError(throwable -> {
                    if(completionHandler != null) {
                        completionHandler.handle(Future.failedFuture(throwable));
                    }
                })
                .doOnNext(attachmentEntities -> {
                    final AtomicInteger activeDownloads = new AtomicInteger();
                    final AtomicBoolean failed = new AtomicBoolean();
                    for(EmailAttachmentEntity attachmentEntity:attachmentEntities) {
                        if(failed.get())
                            break;
                        activeDownloads.incrementAndGet();
                        downloadService.downloadObservable(attachmentEntity.getFileId())
                                .doOnError(throwable -> {
                                    if(completionHandler != null) {
                                        completionHandler.handle(Future.failedFuture(throwable));
                                    }
                                })
                                .doOnNext(buffer -> {
                                    // TODO build with mpu

                                    // write attachment
                                    clientRequest.write(mpu.get());
                                    if(activeDownloads.decrementAndGet() <= 0){
                                        if(completionHandler != null) {
                                            completionHandler.handle(Future.succeededFuture(clientRequest));
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
        private final String boundary;
        private static final String LINE_FEED = "\r\n";
        private String charset = "UTF-8";
        private Buffer buffer;
        private String attachmentName = "attachment";

        public MultiPartUtility() {
            this.boundary = "===" + System.currentTimeMillis() + "===";
            this.buffer = Buffer.buffer();
        }

        public MultiPartUtility formField(String name, String value) {
            buffer.appendString("--" + boundary).appendString(LINE_FEED);
            buffer.appendString("Content-Disposition: form-data; name=\"" + name + "\"").appendString(LINE_FEED);
            buffer.appendString("Content-Type: text/plain; charset=" + charset).appendString(LINE_FEED);
            buffer.appendString(LINE_FEED);
            buffer.appendString(value).appendString(LINE_FEED);
            return this;
        }

        public MultiPartUtility attachment(String fileName, Buffer data) {
            buffer.appendString("--" + boundary).appendString(LINE_FEED);
            buffer.appendString("Content-Disposition: form-data; name=\"" + attachmentName + "\"; filename=\"" + fileName + "\"")
                    .appendString(LINE_FEED);
            buffer.appendString("Content-Type: " + URLConnection.guessContentTypeFromName(fileName))
                    .appendString(LINE_FEED);
            buffer.appendString("Content-Transfer-Encoding: binary").appendString(LINE_FEED);
            buffer.appendString(LINE_FEED);
            buffer.appendBuffer(data);
            return this;
        }

        public String getBoundary() {
            return boundary;
        }

        public Buffer get() {
            return buffer;
        }

        public void clear() {
            buffer = Buffer.buffer();
        }
    }


    protected class HttpResponseHandler implements Handler<HttpClientResponse> {

        private MailgunSendRequest sendRequest;

        public HttpResponseHandler(MailgunSendRequest sendRequest) {
            this.sendRequest = sendRequest;
        }

        @Override
        public void handle(HttpClientResponse httpClientResponse) {
            if(httpClientResponse.statusCode() == HttpStatus.SC_OK) {
                httpClientResponse.bodyHandler(buffer -> {
                    try {
                        MailgunSendResponse response = new ObjectMapper().readValue(buffer.toString(), MailgunSendResponse.class);
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
