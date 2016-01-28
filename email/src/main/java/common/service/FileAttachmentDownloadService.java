package common.service;

import com.google.common.util.concurrent.AbstractIdleService;
import common.config.EmailConfig;
import io.clickhandler.queue.LocalQueueServiceFactory;
import io.clickhandler.queue.QueueFactory;
import io.clickhandler.queue.QueueService;
import io.clickhandler.queue.QueueServiceConfig;
import io.clickhandler.sql.db.SqlExecutor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;
import common.data.DownloadRequest;
import common.handler.FileAttachmentQueueHandler;

/**
 * File attachment download queue manager.
 *
 * @author Brad Behnke
 */
public class FileAttachmentDownloadService extends AbstractIdleService {

    private final QueueService<DownloadRequest> queueService;

    public FileAttachmentDownloadService(EmailConfig config, SqlExecutor db, FileService fileService) {
        final QueueServiceConfig<DownloadRequest> queueConfig = new QueueServiceConfig<>("AttachmentDLQueue", DownloadRequest.class, true, config.getAttachmentParallelism(), config.getAttachmentBatchSize());
        queueConfig.setHandler(new FileAttachmentQueueHandler(db, fileService));

        QueueFactory factory = new LocalQueueServiceFactory();
        this.queueService = factory.build(queueConfig);
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