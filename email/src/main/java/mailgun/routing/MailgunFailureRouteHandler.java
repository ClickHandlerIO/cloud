package mailgun.routing;

import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RoutingContext;
import mailgun.config.MailgunConfig;
import mailgun.data.FailureMessage;
import mailgun.service.MailgunService;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract Vertx route for capturing Mailgun email notifications.
 *
 * @see io.vertx.rxjava.ext.web.Route
 * @author Brad Behnke
 */
public class MailgunFailureRouteHandler extends MailgunRouteHandler<FailureMessage> {
    private final static Logger LOG = LoggerFactory.getLogger(MailgunFailureRouteHandler.class);

    public MailgunFailureRouteHandler(MailgunConfig config, MailgunService mailgunService) {
        super(config, mailgunService);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        verifyRequest(routingContext.request(), new VerifyCallback() {
            @Override
            public void isValid() {
                buildMessage(routingContext.request(), new BuildCallback<FailureMessage>() {
                    @Override
                    public void success(FailureMessage message) {
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
                LOG.error("Mailgun Failure Notification Verification Failed.");
                routingContext.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
            }
        });
    }

    @Override
    protected void buildMessage(HttpServerRequest request, BuildCallback<FailureMessage> callback) {
        try {
            MultiMap params = request.params();
            FailureMessage failureMessage = new FailureMessage();
            failureMessage.setEvent(params.get("event"));
            failureMessage.setRecipient(params.get("recipient"));
            failureMessage.setDomain(params.get("domain"));
            failureMessage.setMessageHeaders(params.get("message-headers"));
            failureMessage.setReason(params.get("reason"));
            failureMessage.setCode(params.get("code"));
            failureMessage.setDescription(params.get("description"));
            failureMessage.setTimestamp(params.get("timestamp"));
            failureMessage.setToken(params.get("token"));
            failureMessage.setSignature(params.get("signature"));
            if (!failureMessage.getEvent().equals("dropped")) {
                callback.failure(new Exception("Invalid Event Type: " + failureMessage.getEvent()));
                return;
            }
            callback.success(failureMessage);
        } catch (Exception e) {
            callback.failure(e);
        }
    }
}
