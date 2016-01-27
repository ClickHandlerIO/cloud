package s3.service;

import common.AbstractFileService;
import entity.FileEntity;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import s3.config.S3Config;
import s3.superS3t.S3Client;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by admin on 1/26/16.
 */
@Singleton
public class S3Service extends AbstractFileService {
    private final static Logger LOG = LoggerFactory.getLogger(S3Service.class);

    private final S3Client s3Client;
    private final ExecutorService executorService;

    @Inject
    public S3Service() {
        this.s3Client = new S3Client(S3Config.getAwsAccessKey(), S3Config.getAwsSecretKey(), S3Config.getEndPoint());
        this.executorService = Executors.newFixedThreadPool(S3Config.getThreads());
    }

    @Override
    public Future<Buffer> get(final FileEntity fileEntity) {
        final Buffer responseData = Buffer.buffer();
        return executorService.submit(new Callable<Buffer>() {
            @Override
            public Buffer call() throws Exception {
                s3Client.get(fileEntity.getStoreBucket(), fileEntity.getStoreId(), new Handler<HttpClientResponse>() {
                    @Override
                    public void handle(HttpClientResponse httpClientResponse) {
                        if(httpClientResponse.statusCode() != 200) {
                            LOG.error("Response code " + httpClientResponse.statusCode() + " on GET using Bucket: " + fileEntity.getStoreBucket() + " StoreId: " + fileEntity.getStoreId());
                        } else {
                            httpClientResponse.bodyHandler(new Handler<Buffer>() {
                                @Override
                                public void handle(Buffer buffer) {
                                    responseData.appendBuffer(buffer);
                                    responseData.notify();
                                }
                            });
                        }
                    }
                });
                responseData.wait();
                return responseData;
            }
        });
    }

    @Override
    public Future<Integer> put(final FileEntity fileEntity,final Buffer data) {
        final AtomicInteger responseCode = new AtomicInteger();
        return executorService.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                s3Client.put(fileEntity.getStoreBucket(), fileEntity.getStoreId(), data, new Handler<HttpClientResponse>() {
                    @Override
                    public void handle(HttpClientResponse httpClientResponse) {
                        responseCode.set(httpClientResponse.statusCode());
                        responseCode.notify();
                    }
                });
                responseCode.wait();
                return responseCode.get();
            }
        });
    }

    @Override
    public Future<Integer> put(final FileEntity fileEntity, final HttpServerFileUpload upload) {
        final AtomicInteger responseCode = new AtomicInteger();
        return executorService.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                s3Client.put(fileEntity.getStoreBucket(), fileEntity.getStoreId(), upload, new Handler<HttpClientResponse>() {
                    @Override
                    public void handle(HttpClientResponse httpClientResponse) {
                        responseCode.set(httpClientResponse.statusCode());
                        responseCode.notify();
                    }
                });
                responseCode.wait();
                return responseCode.get();
            }
        });
    }

    @Override
    public Future<Integer> delete(FileEntity fileEntity) {
        final AtomicInteger responseCode = new AtomicInteger();
        return executorService.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                s3Client.delete(fileEntity.getStoreBucket(), fileEntity.getStoreId(), new Handler<HttpClientResponse>() {
                    @Override
                    public void handle(HttpClientResponse httpClientResponse) {
                        responseCode.set(httpClientResponse.statusCode());
                        responseCode.notify();
                    }
                });
                responseCode.wait();
                return responseCode.get();
            }
        });
    }
}
