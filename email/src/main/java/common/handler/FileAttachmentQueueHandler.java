package common.handler;

import common.service.FileService;
import entity.FileEntity;
import io.clickhandler.queue.QueueHandler;
import io.clickhandler.sql.db.SqlExecutor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;
import common.data.DownloadRequest;

import java.util.List;

/**
 * Retrieves byte[] data for files requested from S3Service and returns data or Exception back to caller.
 *
 * @author Brad Behnke
 */
public class FileAttachmentQueueHandler implements QueueHandler<DownloadRequest> {

    private final SqlExecutor db;
    private final FileService fileService;

    public FileAttachmentQueueHandler(SqlExecutor db, FileService fileService) {
        this.db = db;
        this.fileService = fileService;
    }

    @Override
    public void receive(List<DownloadRequest> downloadRequests) {
        downloadRequests.forEach(this::download);
    }

    public void download(final DownloadRequest request) {
        getFileEntityObservable(request.getFileId())
                .doOnError(throwable -> {
                    if(request.getCompletionHandler() != null) {
                        request.getCompletionHandler().handle(Future.failedFuture(throwable));
                    }
                })
                .doOnNext(fileEntity -> fileService.getObservable(fileEntity)
                        .doOnError(throwable -> {
                            if(request.getCompletionHandler() != null) {
                                request.getCompletionHandler().handle(Future.failedFuture(throwable));
                            }
                        })
                        .doOnNext(buffer -> {
                            if(request.getCompletionHandler() != null) {
                                if (buffer == null || buffer.length() <= 0) {
                                    request.getCompletionHandler().handle(Future.failedFuture(new Exception("S3 Service Failed to Retrieve File Data.")));
                                }
                                request.getCompletionHandler().handle(Future.succeededFuture(buffer));
                            }
                        }));
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
