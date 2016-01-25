package _engine.sns.routeHandler;

import _engine.sns.SNSService;
import _engine.sns.json.SNSEmailMessage;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by admin on 1/22/16.
 */

public class SNSEmailRouteHandler extends SNSRouteHandler<SNSEmailMessage> {
    private final static Logger LOG = LoggerFactory.getLogger(SNSEmailRouteHandler.class);

    public SNSEmailRouteHandler(SNSService snsService) {
        super(snsService, SNSEmailMessage.class);
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

        // get message from body json
        SNSEmailMessage message = getMessage(routingContext.getBody().getBytes());
        if (message == null /*|| !isMessageSignatureValid(message)*/) {
            routingContext.fail(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        passToService(message);
        request.response().setStatusCode(HttpStatus.SC_OK).end();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Message Validation
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // TODO
}
