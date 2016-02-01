package mailgun.handler;

import common.handler.EmailSendQueueHandler;
import common.handler.FileGetPipeHandler;
import common.service.FileService;
import entity.EmailAttachmentEntity;
import entity.EmailEntity;
import io.clickhandler.sql.SqlExecutor;
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
import rx.Observable;

import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;

/**
 * @author Brad Behnke
 */
public class MailgunSendQueueHandler extends EmailSendQueueHandler<MailgunSendRequest> {

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

    public void shutdown(){
        this.client.close();
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
                        getAttachmentEntitiesObservable(sendRequest.getEmailEntity())
                                .doOnError(throwable -> failure(sendRequest,throwable))
                                .doOnNext(attachmentEntities -> new FilePipeIterator(clientRequest, attachmentEntities.iterator(), new FileGetPipeHandler() {
                                    @Override
                                    public void onComplete() {
                                        clientRequest.end();

                                    }

                                    @Override
                                    public void onFailure(Throwable throwable) {
                                        failure(sendRequest,throwable);
                                    }
                                }).start());
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
        EmailEntity emailEntity = sendRequest.getEmailEntity();
        if(emailEntity == null) {
            if(completionHandler != null) {
                completionHandler.handle(Future.failedFuture(new Exception("Null Email Entity.")));
            }
            return;
        }
        if(emailEntity.getFrom() == null || emailEntity.getFrom().isEmpty()) {
            if(completionHandler != null) {
                completionHandler.handle(Future.failedFuture(new Exception("Null or Empty From Field.")));
            }
            return;
        }
        if(emailEntity.getTo() == null || emailEntity.getTo().isEmpty()) {
            if(completionHandler != null) {
                completionHandler.handle(Future.failedFuture(new Exception("Null or Empty To Field.")));
            }
            return;
        }
        MultiPartUtility mpu = new MultiPartUtility();
        // from
        mpu.formField("from", emailEntity.getFrom());
        // to
        String[] toList = emailEntity.getTo().split(";|,|\\s");
        for (String to : toList) {
            mpu.formField("to", to);
        }
        // cc
        if(emailEntity.getCc() != null && !emailEntity.getCc().isEmpty()) {
            String[] ccList = emailEntity.getCc().split(";|,|\\s");
            for (String cc : ccList) {
                mpu.formField("cc", cc);
            }
        }
        // subject
        mpu.formField("subject", (emailEntity.getSubject() == null ? "":sendRequest.getEmailEntity().getSubject()));
        // text
        if(emailEntity.getTextBody() != null && !emailEntity.getTextBody().isEmpty()) {
            mpu.formField("text", emailEntity.getTextBody());
        }
        // html
        if(emailEntity.getHtmlBody() != null && !emailEntity.getHtmlBody().isEmpty()) {
            mpu.formField("html", emailEntity.getHtmlBody());
        }
        // write to client
        clientRequest.write(mpu.get());
    }

    private class FilePipeIterator implements FileGetPipeHandler {

        private Iterator<EmailAttachmentEntity> it;
        private HttpClientRequest clientRequest;
        private FileGetPipeHandler ownerHandler;

        public FilePipeIterator(HttpClientRequest clientRequest, Iterator<EmailAttachmentEntity> it, FileGetPipeHandler ownerHandler) {
            this.clientRequest = clientRequest;
            this.it = it;
            this.ownerHandler = ownerHandler;
        }

        @Override
        public void onComplete() {
            if(it.hasNext()) {
                next();
                return;
            }
            ownerHandler.onComplete();
        }

        @Override
        public void onFailure(Throwable throwable) {
            ownerHandler.onFailure(throwable);
        }

        public void next() {
            if(it.hasNext()) {
                EmailAttachmentEntity attachmentEntity = it.next();
                clientRequest.write(new MultiPartUtility().attachmentHead(attachmentEntity.getName()).get());
                writeAttachment(clientRequest, attachmentEntity, this);
                return;
            }
            onComplete();
        }

        public void start() {
            if(it != null) {
                next();
                return;
            }
            if(ownerHandler != null) {
                ownerHandler.onFailure(new Exception("Null Attachment List."));
            }
        }
    }

    private void writeAttachment(HttpClientRequest clientRequest, EmailAttachmentEntity attachmentEntity, FileGetPipeHandler handler) {
        getFileEntityObservable(attachmentEntity.getFileId())
                .doOnError(throwable -> {
                    if (handler != null) {
                        handler.onFailure(throwable);
                    }
                })
                .doOnNext(fileEntity -> getFileService().getAsyncPipe(fileEntity, clientRequest, handler));
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

        public MultiPartUtility attachmentHead(String fileName) {
            buffer.appendString("--" + BOUNDARY).appendString(LINE_FEED);
            buffer.appendString("Content-Disposition: form-data; name=\"" + attachmentName + "\"; filename=\"" + fileName + "\"")
                    .appendString(LINE_FEED);
            buffer.appendString("Content-Type: " + URLConnection.guessContentTypeFromName(fileName))
                    .appendString(LINE_FEED);
            buffer.appendString("Content-Transfer-Encoding: binary").appendString(LINE_FEED);
            buffer.appendString(LINE_FEED);
            return this;
        }

        public Buffer get() {
            return buffer;
        }
    }

    private class HttpResponseHandler implements Handler<HttpClientResponse> {

        private MailgunSendRequest sendRequest;
        private Buffer totalBuffer;

        public HttpResponseHandler(MailgunSendRequest sendRequest) {
            this.sendRequest = sendRequest;
            this.totalBuffer = Buffer.buffer();
        }

        @Override
        public void handle(HttpClientResponse httpClientResponse) {
            if(httpClientResponse.statusCode() == HttpStatus.SC_OK) {
                httpClientResponse.bodyHandler(buffer -> totalBuffer.appendBuffer(buffer));
                httpClientResponse.endHandler(aVoid -> {
                    try {
                        MailgunSendResponse response = jsonMapper.readValue(totalBuffer.getBytes(), MailgunSendResponse.class);
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
                        updateRecords(sendRequest.getEmailEntity());
                        publishEvent(sendRequest.getEmailEntity(), false);
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
