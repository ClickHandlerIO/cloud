package sns.routing;

import io.vertx.rxjava.core.http.HttpServerRequest;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.slf4j.LoggerFactory;
import sns.data.SNSEmailMessage;
import sns.service.SNSService;

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
        SNSEmailMessage message = getMessage(routingContext.getBody().toString());
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
