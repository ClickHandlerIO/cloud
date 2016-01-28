package ses.service;

import com.sun.istack.internal.NotNull;
import common.service.EmailService;
import common.service.FileService;
import common.service.FileAttachmentDownloadService;
import entity.EmailEntity;
import io.clickhandler.sql.db.SqlExecutor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import ses.config.SESConfig;
import ses.data.SESSendRequest;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * SES Email services manager.
 *
 * @author Brad Behnke
 */
@Singleton
public class SESService extends EmailService<SESSendRequest> {
    private final static Logger LOG = LoggerFactory.getLogger(SESService.class);

    // ses services
    private final SESSendPrepService sesSendPrepService;
    private final FileAttachmentDownloadService fileAttachmentDownloadService;
    private final SESSendService sesSendService;

    @Inject
    public SESService(@NotNull SESConfig config, @NotNull EventBus eventBus, @NotNull SqlExecutor db, @NotNull FileService fileService) {
        this.fileAttachmentDownloadService = new FileAttachmentDownloadService(config, db, fileService);
        this.sesSendService = new SESSendService(config, eventBus, db);
        this.sesSendPrepService = new SESSendPrepService(config, eventBus, db, fileAttachmentDownloadService, sesSendService);
    }

    @Override
    protected void startUp() throws Exception {
        this.sesSendService.startAsync();
        this.fileAttachmentDownloadService.startAsync();
        this.sesSendPrepService.startAsync();
        LOG.info("SES Service Started");
    }

    @Override
    protected void shutDown() throws Exception {
        this.sesSendPrepService.stopAsync();
        this.fileAttachmentDownloadService.stopAsync();
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
