package io.clickhandler.email.ses.service;

import com.sun.istack.internal.NotNull;
import io.clickhandler.email.service.EmailService;
import io.clickhandler.files.service.FileService;
import io.clickhandler.email.entity.EmailEntity;
import io.clickhandler.sql.SqlExecutor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import io.clickhandler.email.ses.config.SESConfig;
import io.clickhandler.email.ses.data.MimeSendRequest;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * SES Email services manager.
 *
 * @author Brad Behnke
 */
@Singleton
public class SESService extends EmailService<MimeSendRequest> {
    private final static Logger LOG = LoggerFactory.getLogger(SESService.class);

    // io.clickhandler.email.ses services
    private final SESSendService sesSendService;

    @Inject
    public SESService(@NotNull SESConfig config, @NotNull EventBus eventBus, @NotNull SqlExecutor db, @NotNull FileService fileService) {
        this.sesSendService = new SESSendService(config, eventBus, db, fileService);
    }

    @Override
    protected void startUp() throws Exception {
        this.sesSendService.startAsync();
        LOG.info("SES Service Started.");
    }

    @Override
    protected void shutDown() throws Exception {
        this.sesSendService.stopAsync();
        LOG.info("SES Service Shutdown.");
    }

    @Override
    public Observable<EmailEntity> sendObservable(MimeSendRequest sendRequest) {
        ObservableFuture<EmailEntity> observableFuture = RxHelper.observableFuture();
        send(sendRequest, observableFuture.toHandler());
        return observableFuture;
    }

    private void send(MimeSendRequest sendRequest, Handler<AsyncResult<EmailEntity>> completionHandler) {
        if(sendRequest.getEmailEntity() == null) {
            completionHandler.handle(Future.failedFuture(new Exception("Null EmailEntity.")));
            return;
        }
        if(sendRequest.getEmailEntity().getId() == null || sendRequest.getEmailEntity().getId().isEmpty()) {
            completionHandler.handle(Future.failedFuture(new Exception("Null or Empty EmailEntity Id")));
            return;
        }
        sendRequest.setCompletionHandler(completionHandler);
        this.sesSendService.enqueue(sendRequest);
    }
}
