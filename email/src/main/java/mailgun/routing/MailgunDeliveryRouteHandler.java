package mailgun.routing;

import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RoutingContext;
import mailgun.config.MailgunConfig;
import mailgun.data.DeliveryMessage;
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
public class MailgunDeliveryRouteHandler extends MailgunRouteHandler<DeliveryMessage> {
    private final static Logger LOG = LoggerFactory.getLogger(MailgunDeliveryRouteHandler.class);

    public MailgunDeliveryRouteHandler(MailgunConfig config, MailgunService mailgunService) {
        super(config, mailgunService);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        verifyRequest(routingContext.request(), new VerifyCallback() {
            @Override
            public void isValid() {
                buildMessage(routingContext.request(), new BuildCallback<DeliveryMessage>() {
                    @Override
                    public void success(DeliveryMessage message) {
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
                LOG.error("Mailgun Delivery Notification Verification Failed.");
                routingContext.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
            }
        });
    }

    @Override
    protected void buildMessage(HttpServerRequest request, BuildCallback<DeliveryMessage> callback) {
        MultiMap params = request.params();
        DeliveryMessage deliveryMessage = new DeliveryMessage();
        deliveryMessage.setEvent(params.get("event"));
        deliveryMessage.setRecipient(params.get("recipient"));
        deliveryMessage.setDomain(params.get("domain"));
        deliveryMessage.setMessageHeaders(params.get("message-headers"));
        deliveryMessage.setMessageId(params.get("Message-Id"));
        deliveryMessage.setTimestamp(params.get("timestamp"));
        deliveryMessage.setToken(params.get("token"));
        deliveryMessage.setSignature(params.get("signature"));
        if (!deliveryMessage.getEvent().equals("delivered")) {
            callback.failure(new Exception("Invalid Event Type: " + deliveryMessage.getEvent()));
            return;
        }
        callback.success(deliveryMessage);
    }
}
