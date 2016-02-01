package sns.routing.general;

import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.apache.http.HttpStatus;
import sns.data.json.general.GeneralMessage;
import sns.routing.common.SNSRouteHandler;
import sns.service.SNSService;

/**
 * Vertx route for all SNS topics excluding email notification and reciepts.
 *
 * @see io.vertx.rxjava.ext.web.Route
 * @author Brad Behnke
 */

public class SNSGeneralRouteHandler extends SNSRouteHandler<GeneralMessage> {

    public SNSGeneralRouteHandler(SNSService snsService) {
        super(snsService, GeneralMessage.class);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        // get header info
        HttpServerRequest request = routingContext.request();
        HeaderInfo headerInfo = processHeaders(request.headers());
        if(!headerInfo.isComplete()) {
            routingContext.response().setStatusCode(HttpStatus.SC_BAD_REQUEST).end();
            return;
        }
        MessageBuilder messageBuilder = new MessageBuilder(routingContext);
        request.bodyHandler(messageBuilder::chunkReceived)
                .endHandler(aVoid -> messageBuilder.onComplete())
                .exceptionHandler(throwable -> messageBuilder.onFailure());
    }
}
