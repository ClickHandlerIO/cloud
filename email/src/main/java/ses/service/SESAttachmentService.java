package ses.service;

import com.google.common.util.concurrent.AbstractIdleService;
import io.clickhandler.queue.LocalQueueServiceFactory;
import io.clickhandler.queue.QueueFactory;
import io.clickhandler.queue.QueueService;
import io.clickhandler.queue.QueueServiceConfig;
import io.clickhandler.sql.db.SqlDatabase;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;
import s3.service.S3Service;
import ses.data.DownloadRequest;
import ses.handler.AttachmentQueueHandler;

/**
 * SES Attachment download queue manager.
 *
 * @author Brad Behnke
 */
public class SESAttachmentService extends AbstractIdleService {

    private final QueueService<DownloadRequest> queueService;

    public SESAttachmentService(SqlDatabase db, S3Service s3Service) {
        final QueueServiceConfig<DownloadRequest> config = new QueueServiceConfig<>("AttachmentDLQueue", DownloadRequest.class, true, 2, 1);
        config.setHandler(new AttachmentQueueHandler(db, s3Service));

        QueueFactory factory = new LocalQueueServiceFactory();
        this.queueService = factory.build(config);
    }

    @Override
    protected void startUp() throws Exception {
        this.queueService.startAsync();
    }

    @Override
    protected void shutDown() throws Exception {
        this.queueService.stopAsync();
    }

    public Observable<Buffer> downloadObservable(String fileId) {
        ObservableFuture<Buffer> observableFuture = RxHelper.observableFuture();
        download(fileId, observableFuture.toHandler());
        return observableFuture;
    }

    private void download(String fileId, Handler<AsyncResult<Buffer>> completionHandler) {
        this.queueService.add(new DownloadRequest(fileId, completionHandler));
    }
}