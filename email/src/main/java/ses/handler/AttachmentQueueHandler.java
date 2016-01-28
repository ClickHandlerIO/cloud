package ses.handler;

import entity.FileEntity;
import io.clickhandler.queue.QueueHandler;
import io.clickhandler.sql.db.SqlDatabase;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import s3.service.S3Service;
import ses.data.DownloadRequest;

import java.util.List;

/**
 * Retrieves byte[] data for files requested from S3Service and returns data or Exception back to caller.
 *
 * @author Brad Behnke
 */
public class AttachmentQueueHandler implements QueueHandler<DownloadRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(AttachmentQueueHandler.class);
    private final SqlDatabase db;
    private final S3Service s3Service;

    public AttachmentQueueHandler(SqlDatabase db, S3Service s3Service) {
        this.db = db;
        this.s3Service = s3Service;
    }

    @Override
    public void receive(List<DownloadRequest> downloadRequests) {
        downloadRequests.forEach(this::download);
    }

    public void download(final DownloadRequest request) {
        getFileEntityObservable(request.getFileId())
                .doOnError(throwable -> request.getCallBack().onFailure(throwable))
                .doOnNext(fileEntity -> {
                    try {
                        final byte[] data = s3Service.get(fileEntity).get().getBytes();
                        if (data == null) {
                            request.getCallBack().onFailure(new Exception("S3 Service Failed to Retrieve File Data."));
                        }
                        request.getCallBack().onSuccess(data);
                    } catch (Exception e) {
                        request.getCallBack().onFailure(e);
                    }
                });
    }

    public Observable<FileEntity> getFileEntityObservable(String fileId) {
        ObservableFuture<FileEntity> observableFuture = RxHelper.observableFuture();
        getFileEntity(fileId, observableFuture.toHandler());
        return observableFuture;
    }

    public void getFileEntity(String fileId, Handler<AsyncResult<FileEntity>> completionHandler){
        db.readObservable(session ->
                session.getEntity(FileEntity.class, fileId))
                .doOnError(e -> {
                    if (completionHandler != null) {
                        completionHandler.handle(Future.failedFuture(e));
                    }
                })
                .subscribe(fileEntity -> {
                    if (completionHandler != null) {
                        completionHandler.handle(Future.succeededFuture(fileEntity));
                    }
                });
    }
}
