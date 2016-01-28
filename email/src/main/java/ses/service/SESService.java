package ses.service;

import com.sun.istack.internal.NotNull;
import common.service.AbstractEmailService;
import entity.EmailEntity;
import io.clickhandler.sql.db.SqlDatabase;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import s3.service.S3Service;
import ses.data.SESSendRequest;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * SES Email services manager.
 *
 * @author Brad Behnke
 */
@Singleton
public class SESService extends AbstractEmailService<SESSendRequest> {
    private final static Logger LOG = LoggerFactory.getLogger(SESService.class);

    // ses services
    private final SESSendPrepService sesSendPrepService;
    private final SESAttachmentService SESAttachmentService;
    private final SESSendService sesSendService;

    @Inject
    public SESService(@NotNull EventBus eventBus, @NotNull SqlDatabase db, @NotNull S3Service s3Service) {
        this.SESAttachmentService = new SESAttachmentService(db, s3Service);
        this.sesSendService = new SESSendService(eventBus, db);
        this.sesSendPrepService = new SESSendPrepService(eventBus, db, SESAttachmentService, sesSendService);
    }

    @Override
    protected void startUp() throws Exception {
        this.sesSendService.startAsync();
        this.SESAttachmentService.startAsync();
        this.sesSendPrepService.startAsync();
        LOG.info("SES Service Started");
    }

    @Override
    protected void shutDown() throws Exception {
        this.sesSendPrepService.stopAsync();
        this.SESAttachmentService.stopAsync();
        this.sesSendService.stopAsync();
        LOG.info("SES Service Shutdown");
    }

    @Override
    public Observable<EmailEntity> sendObservable(SESSendRequest sendRequest) {
        ObservableFuture<EmailEntity> observableFuture = RxHelper.observableFuture();
        send(sendRequest, observableFuture.toHandler());
        return observableFuture;
    }

    private void send(SESSendRequest sendRequest, Handler<AsyncResult<EmailEntity>> completionHandler) {
        if(sendRequest.getEmailEntity() == null) {
            completionHandler.handle(Future.failedFuture(new Exception("Null EmailEntity.")));
            return;
        }
        if(sendRequest.getEmailEntity().getId() == null || sendRequest.getEmailEntity().getId().isEmpty()) {
            completionHandler.handle(Future.failedFuture(new Exception("Null or Empty EmailEntity Id")));
            return;
        }
        sendRequest.setCompletionHandler(completionHandler);
        this.sesSendPrepService.enqueue(sendRequest);
    }
}
