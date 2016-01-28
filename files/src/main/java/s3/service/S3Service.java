package s3.service;

import com.sun.istack.internal.NotNull;
import common.service.FileService;
import entity.FileEntity;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;
import s3.config.S3Config;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * File service using Amazon S3 for storage. Allows for uploading, downloading, and deleting of remote files.
 *
 * @author Brad Behnke
 */
@Singleton
public class S3Service extends FileService {

    private final S3Client s3Client;

    @Inject
    public S3Service(@NotNull S3Config config) {
        this.s3Client = new S3Client(config.getAwsAccessKey(), config.getAwsSecretKey(), config.getEndPoint());
    }

    @Override
    public Observable<Buffer> getObservable(FileEntity fileEntity) {
        ObservableFuture<Buffer> observableFuture = RxHelper.observableFuture();
        get(fileEntity, observableFuture.toHandler());
        return observableFuture;
    }

    private void get(FileEntity fileEntity, Handler<AsyncResult<Buffer>> completionHandler){
        s3Client.get(fileEntity.getStoreBucket(), fileEntity.getStoreId(), httpClientResponse -> {
            if(httpClientResponse.statusCode() != 200) {
                if(completionHandler != null) {
                    completionHandler.handle(Future.failedFuture(new Exception("Response code " + httpClientResponse.statusCode() +
                            " on GET using Bucket: " + fileEntity.getStoreBucket() + " StoreId: " + fileEntity.getStoreId())));
                }
            } else {
                httpClientResponse.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {
                        if(completionHandler != null) {
                            completionHandler.handle(Future.succeededFuture(buffer));
                        }
                    }
                });
            }
        });
    }

    @Override
    public Observable<Integer> putObservable(FileEntity fileEntity, Buffer data) {
        ObservableFuture<Integer> observableFuture = RxHelper.observableFuture();
        put(fileEntity, data, observableFuture.toHandler());
        return observableFuture;
    }

    private void put(FileEntity fileEntity, Buffer data, Handler<AsyncResult<Integer>> completionHandler) {
        s3Client.put(fileEntity.getStoreBucket(), fileEntity.getStoreId(), data, httpClientResponse -> {
            if(completionHandler != null) {
                completionHandler.handle(Future.succeededFuture(httpClientResponse.statusCode()));
            }
        });
    }

    @Override
    public Observable<Integer> putObservable(FileEntity fileEntity, HttpServerFileUpload upload) {
        ObservableFuture<Integer> observableFuture = RxHelper.observableFuture();
        put(fileEntity, upload, observableFuture.toHandler());
        return observableFuture;
    }

    private void put(FileEntity fileEntity, HttpServerFileUpload upload, Handler<AsyncResult<Integer>> completionHandler) {
        s3Client.put(fileEntity.getStoreBucket(), fileEntity.getStoreId(), upload, httpClientResponse -> {
            if(completionHandler != null) {
                completionHandler.handle(Future.succeededFuture(httpClientResponse.statusCode()));
            }
        });
    }

    @Override
    public Observable<Integer> deleteObservable(FileEntity fileEntity) {
        ObservableFuture<Integer> observableFuture = RxHelper.observableFuture();
        delete(fileEntity, observableFuture.toHandler());
        return observableFuture;
    }

    private void delete(FileEntity fileEntity , Handler<AsyncResult<Integer>> completionHandler) {
        s3Client.delete(fileEntity.getStoreBucket(), fileEntity.getStoreId(), httpClientResponse -> {
            if(completionHandler != null) {
                completionHandler.handle(Future.succeededFuture(httpClientResponse.statusCode()));
            }
        });
    }
}
