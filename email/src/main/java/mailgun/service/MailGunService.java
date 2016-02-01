package mailgun.service;

import com.sun.istack.internal.NotNull;
import common.service.EmailService;
import common.service.FileService;
import entity.EmailEntity;
import io.clickhandler.sql.db.SqlExecutor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import mailgun.config.MailgunConfig;
import mailgun.data.MailgunSendRequest;
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
public class MailgunService extends EmailService<MailgunSendRequest>{
    private final static Logger LOG = LoggerFactory.getLogger(MailgunService.class);

    private final MailgunSendService sendService;

    @Inject
    public MailgunService(@NotNull MailgunConfig config, @NotNull EventBus eventBus, @NotNull SqlExecutor db, @NotNull FileService fileService) {
        this.sendService = new MailgunSendService(config, eventBus, db, fileService);
    }

    @Override
    protected void startUp() throws Exception {
        this.sendService.startAsync();
        LOG.info("Mailgun Service Started.");
    }

    @Override
    protected void shutDown() throws Exception {
        this.sendService.stopAsync();
        LOG.info("Mailgun Service Shutdown.");
    }

    @Override
    public Observable<EmailEntity> sendObservable(MailgunSendRequest sendRequest) {
        ObservableFuture<EmailEntity> observableFuture = RxHelper.observableFuture();
        send(sendRequest, observableFuture.toHandler());
        return observableFuture;
    }

    private void send(MailgunSendRequest sendRequest, Handler<AsyncResult<EmailEntity>> completionHandler) {
        if(sendRequest.getEmailEntity() == null) {
            completionHandler.handle(Future.failedFuture(new Exception("Null EmailEntity.")));
            return;
        }
        if(sendRequest.getEmailEntity().getId() == null || sendRequest.getEmailEntity().getId().isEmpty()) {
            completionHandler.handle(Future.failedFuture(new Exception("Null or Empty EmailEntity Id")));
            return;
        }
        sendRequest.setCompletionHandler(completionHandler);
        this.sendService.enqueue(sendRequest);
    }
}
