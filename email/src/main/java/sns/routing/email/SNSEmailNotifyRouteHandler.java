package sns.routing.email;

import io.vertx.rxjava.core.http.HttpServerRequest;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.slf4j.LoggerFactory;
import sns.json.email.notify.EmailNotifyMessage;
import sns.routing.common.SNSRouteHandler;
import sns.service.SNSService;

/**
 * Created by admin on 1/22/16.
 */

public class SNSEmailNotifyRouteHandler extends SNSRouteHandler<EmailNotifyMessage> {
    private final static Logger LOG = LoggerFactory.getLogger(SNSEmailNotifyRouteHandler.class);

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

        // get message from body json
        EmailNotifyMessage message = getMessage(routingContext.getBody().toString());
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
