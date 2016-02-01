package mailgun.routing;

import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RoutingContext;
import mailgun.config.MailgunConfig;
import mailgun.data.ReceiveMessage;
import mailgun.service.MailgunService;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract Vertx route for capturing Mailgun email notifications.
 *
 * @see io.vertx.rxjava.ext.web.Route
 * @author Brad Behnke
 */
public class MailgunReceiveRouteHandler extends MailgunRouteHandler<ReceiveMessage> {
    private final static Logger LOG = LoggerFactory.getLogger(MailgunReceiveRouteHandler.class);

    public MailgunReceiveRouteHandler(MailgunConfig config, MailgunService mailgunService) {
        super(config, mailgunService);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        verifyRequest(routingContext.request(), new VerifyCallback() {
            @Override
            public void isValid() {
                buildMessage(routingContext.request(), new BuildCallback<ReceiveMessage>() {
                    @Override
                    public void success(ReceiveMessage message) {
                        passToService(message);
                        routingContext.response().setStatusCode(HttpStatus.SC_OK).end();
                    }

                    @Override
                    public void failure(Throwable throwable) {
                        LOG.error(throwable.getMessage());
                    }
                });
            }

            @Override
            public void isInvalid() {
                LOG.error("Mailgun Bounce Notification Verification Failed.");
                routingContext.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
            }
        });
    }

    @Override
    protected void buildMessage(HttpServerRequest request, BuildCallback<ReceiveMessage> callback) {
        try {
            MultiMap params = request.params();
            ReceiveMessage receiveMessage = new ReceiveMessage();
            receiveMessage.setRecipient(params.get("recipient"));
            receiveMessage.setSender(params.get("sender"));
            receiveMessage.setFrom(params.get("from"));
            receiveMessage.setSubject(params.get("subject"));
            receiveMessage.setBodyPlain(params.get("body-plain"));
            receiveMessage.setStrippedText(params.get("stripped-text"));
            receiveMessage.setStrippedSignature(params.get("stripped-signature"));
            receiveMessage.setBodyHtml(params.get("body-html"));
            receiveMessage.setStrippedHtml(params.get("stripped-html"));
            receiveMessage.setAttachmentCount(Integer.valueOf(params.get("attachment-count")));
            receiveMessage.setAttachmentX(new ArrayList<>());
            if(receiveMessage.getAttachmentCount() > 0) {
                List<String> uploads = receiveMessage.getAttachmentX();
                for(int i = 1; i <= receiveMessage.getAttachmentCount(); i++) {
                    uploads.add(params.get("attachment-"+i));
                }
            }
            receiveMessage.setTimestamp(params.get("timestamp"));
            receiveMessage.setToken(params.get("token"));
            receiveMessage.setSignature(params.get("signature"));
            receiveMessage.setMessageHeaders(params.get("message-headers"));
            receiveMessage.setContentIdMap(params.get("content-id-map"));
            callback.success(receiveMessage);
        } catch (Exception e) {
            callback.failure(e);
        }
    }
}
