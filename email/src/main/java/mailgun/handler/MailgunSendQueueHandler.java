package mailgun.handler;

import common.handler.EmailSendQueueHandler;
import entity.EmailEntity;
import io.clickhandler.sql.db.SqlExecutor;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.http.HttpClient;
import mailgun.config.MailgunConfig;
import mailgun.data.MailgunSendRequest;
import mailgun.event.MailgunEmailSentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Brad Behnke
 */
public class MailgunSendQueueHandler extends EmailSendQueueHandler<MailgunSendRequest> {

    private final static Logger LOG = LoggerFactory.getLogger(MailgunSendQueueHandler.class);
    private final EventBus eventBus;
//    private final HttpClient client;
//    private final Mai
    private final String host;
    private final String domain;
    private final int ALLOWED_ATTEMPTS;

    public MailgunSendQueueHandler(MailgunConfig config, EventBus eventBus, SqlExecutor db) {
        super(db);
        this.eventBus = eventBus;
        this.host = "api.mailgun.net/v2";
        this.domain = config.getDomain();
        this.ALLOWED_ATTEMPTS = config.getSendRetryMax();
//        this.client = Vertx.vertx().createHttpClient(new HttpClientOptions()
//                .setSsl(true)
//                .setTrustAll(true)
//                .setDefaultHost(host)
//                .setDefaultPort(443));
    }

    @Override
    public void receive(List<MailgunSendRequest> sendRequests) {
        sendRequests.forEach(this::sendEmail);
    }

    private void sendEmail(MailgunSendRequest sendRequest) {
        sendRequest.incrementAttempts();





//
//        // TODO still need content after domain
//        HttpClientRequest httpClientRequest = client.post("/"+domain+"/messages", httpClientResponse -> {
//            if(httpClientResponse.statusCode() == HttpStatus.SC_OK) {
//                httpClientResponse.bodyHandler(buffer -> {
//                    try {
//                        MailgunSendResponse response = new ObjectMapper().readValue(buffer.toString(), MailgunSendResponse.class);
//                        if(response == null || response.getId() == null || response.getId().isEmpty()) {
//                            failure(sendRequest, new Exception("Invalid Response to Send Request"));
//                            return;
//                        }
//                        EmailEntity emailEntity = sendRequest.getEmailEntity();
//                        emailEntity.setMessageId(response.getId());
//                        updateRecords(emailEntity);
//                        publishEvent(emailEntity, true);
//                        success(sendRequest, emailEntity);
//                    } catch (Throwable throwable) {
//                        failure(sendRequest, throwable);
//                    }
//                });
//            } else {
//                if(sendRequest.getAttempts() < ALLOWED_ATTEMPTS) {
//                    sendEmail(sendRequest);
//                } else {
//                    updateRecords(sendRequest.getEmailEntity());
//                    publishEvent(sendRequest.getEmailEntity(), false);
//                    failure(sendRequest, new Exception("Failed to Send Email Using " + ALLOWED_ATTEMPTS + " Attempts with Response Code: " + httpClientResponse.statusCode()));
//                }
//            }
//        });
//
//        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
//        // todo build headers
    }

    private void publishEvent(EmailEntity emailEntity, boolean success) {
        eventBus.publish(MailgunEmailSentEvent.ADDRESS, new MailgunEmailSentEvent(emailEntity, success));
    }
}
