package sns.routing.email;

import io.vertx.rxjava.core.http.HttpServerRequest;
import org.apache.http.HttpStatus;
import io.vertx.rxjava.ext.web.RoutingContext;
import sns.data.json.email.notify.EmailNotifyMessage;
import sns.routing.common.SNSRouteHandler;
import sns.service.SNSService;

/**
 * Vertx route for all email notifications
 *
 * @see io.vertx.rxjava.ext.web.Route
 * @author Brad Behnke
 */

public class SNSEmailNotifyRouteHandler extends SNSRouteHandler<EmailNotifyMessage> {

    public SNSEmailNotifyRouteHandler(SNSService snsService) {
        super(snsService, EmailNotifyMessage.class);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        // get header info
        HttpServerRequest request = routingContext.request();
        HeaderInfo headerInfo = processHeaders(request.headers());
        if(!headerInfo.isComplete()) {
            routingContext.fail(HttpStatus.SC_BAD_REQUEST);
            return;
        }
        MessageBuilder messageBuilder = new MessageBuilder(routingContext);
        request.bodyHandler(messageBuilder::chunkReceived)
                .endHandler(aVoid -> messageBuilder.onComplete())
                .exceptionHandler(throwable -> messageBuilder.onFailure());
    }
}
