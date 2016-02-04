package mailgun.service;

import com.sun.istack.internal.NotNull;
import common.data.Message;
import common.service.EmailService;
import common.service.FileService;
import entity.EmailEntity;
import io.clickhandler.sql.SqlExecutor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import mailgun.config.MailgunConfig1;
import mailgun.data.MailgunSendRequest1;
import mailgun.routing.MailgunBounceRouteHandler;
import mailgun.routing.MailgunDeliveryRouteHandler;
import mailgun.routing.MailgunFailureRouteHandler;
import mailgun.routing.MailgunReceiveRouteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 *  Email service using Mailgun to send/receive emails.
 *
 * @author Brad Behnke
 */
@Singleton
public class MailgunService extends EmailService<MailgunSendRequest1>{
    private final static Logger LOG = LoggerFactory.getLogger(MailgunService.class);

    private final MailgunSendService sendService;
    private final MailgunMessageService messageService;
    private final MailgunDeliveryRouteHandler deliveryRouteHandler;
    private final MailgunBounceRouteHandler bounceRouteHandler;
    private final MailgunFailureRouteHandler failureRouteHandler;
    private final MailgunReceiveRouteHandler receiveRouteHandler;

    @Inject
    public MailgunService(@NotNull MailgunConfig1 config, @NotNull EventBus eventBus, @NotNull SqlExecutor db, @NotNull FileService fileService) {
        this.sendService = new MailgunSendService(config, eventBus, db, fileService);
        this.messageService = new MailgunMessageService(config, eventBus, db);
        this.deliveryRouteHandler = new MailgunDeliveryRouteHandler(config, this);
        this.bounceRouteHandler = new MailgunBounceRouteHandler(config, this);
        this.failureRouteHandler = new MailgunFailureRouteHandler(config, this);
        this.receiveRouteHandler = new MailgunReceiveRouteHandler(config, this);
    }

    @Override
    protected void startUp() throws Exception {
        this.sendService.startAsync();
        this.messageService.startAsync();
        LOG.info("Mailgun Service Started.");
    }

    @Override
    protected void shutDown() throws Exception {
        this.sendService.stopAsync();
        this.messageService.stopAsync();
        LOG.info("Mailgun Service Shutdown.");
    }

    @Override
    public Observable<EmailEntity> sendObservable(MailgunSendRequest1 sendRequest) {
        ObservableFuture<EmailEntity> observableFuture = RxHelper.observableFuture();
        send(sendRequest, observableFuture.toHandler());
        return observableFuture;
    }

    private void send(MailgunSendRequest1 sendRequest, Handler<AsyncResult<EmailEntity>> completionHandler) {
        if(sendRequest.getEmailEntity() == null) {
            if(completionHandler != null)
                completionHandler.handle(Future.failedFuture(new Exception("Null EmailEntity.")));
            return;
        }
        if(sendRequest.getEmailEntity().getId() == null || sendRequest.getEmailEntity().getId().isEmpty()) {
            if(completionHandler != null)
                completionHandler.handle(Future.failedFuture(new Exception("Null or Empty EmailEntity Id")));
            return;
        }
        sendRequest.setCompletionHandler(completionHandler);
        this.sendService.enqueue(sendRequest);
    }

    public void enqueueMessage(Message message) {
        this.messageService.enqueueMessage(message);
    }

    public MailgunBounceRouteHandler getBounceRouteHandler() {
        return bounceRouteHandler;
    }

    public MailgunDeliveryRouteHandler getDeliveryRouteHandler() {
        return deliveryRouteHandler;
    }

    public MailgunFailureRouteHandler getFailureRouteHandler() {
        return failureRouteHandler;
    }

    public MailgunReceiveRouteHandler getReceiveRouteHandler() {
        return receiveRouteHandler;
    }
}
