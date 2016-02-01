package mailgun.routing;

import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RoutingContext;
import mailgun.config.MailgunConfig;
import mailgun.data.BounceMessage;
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
public class MailgunBounceRouteHandler extends MailgunRouteHandler<BounceMessage> {
    private final static Logger LOG = LoggerFactory.getLogger(MailgunBounceRouteHandler.class);

    public MailgunBounceRouteHandler(MailgunConfig config, MailgunService mailgunService) {
        super(config, mailgunService);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        verifyRequest(routingContext.request(), new VerifyCallback() {
            @Override
            public void isValid() {
                buildMessage(routingContext.request(), new BuildCallback<BounceMessage>() {
                    @Override
                    public void success(BounceMessage message) {
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
    protected void buildMessage(HttpServerRequest request, BuildCallback<BounceMessage> callback) {
        MultiMap params = request.params();
        BounceMessage bounceMessage = new BounceMessage();
        bounceMessage.setEvent(params.get("event"));
        bounceMessage.setRecipient(params.get("recipient"));
        bounceMessage.setDomain(params.get("domain"));
        bounceMessage.setMessageHeaders(params.get("message-headers"));
        bounceMessage.setCode(params.get("code"));
        bounceMessage.setError(params.get("error"));
        bounceMessage.setNotification(params.get("notification"));
        bounceMessage.setCampaignId(params.get("campaign-id"));
        bounceMessage.setCampaignName(params.get("campaign-name"));
        bounceMessage.setTag(params.get("tag"));
        bounceMessage.setMailingList(params.get("mailing-list"));
        bounceMessage.setTimestamp(params.get("timestamp"));
        bounceMessage.setToken(params.get("token"));
        bounceMessage.setSignature(params.get("signature"));
        if (!bounceMessage.getEvent().equals("bounced")) {
            callback.failure(new Exception("Invalid Event Type: " + bounceMessage.getEvent()));
            return;
        }
        callback.success(bounceMessage);
    }
}
