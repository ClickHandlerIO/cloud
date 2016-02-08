package io.clickhandler.files.s3.service;

import com.sun.istack.internal.NotNull;
import io.clickhandler.email.entity.FileEntity;
import io.clickhandler.files.handler.FileGetChunksHandler;
import io.clickhandler.files.handler.FileGetPipeHandler;
import io.clickhandler.files.handler.FileStatusHandler;
import io.clickhandler.files.service.FileService;
import io.clickhandler.files.s3.config.S3Config;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpServerFileUpload;
import io.vertx.rxjava.core.streams.Pump;

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
    public S3Service(@NotNull Vertx vertx, @NotNull S3Config config) {
        this.s3Client = new S3Client(vertx, config.getAwsAccessKey(), config.getAwsSecretKey(), config.getEndPoint());
    }

    @Override
    public void getAsyncChunks(FileEntity fileEntity, FileGetChunksHandler handler) {
        s3Client.get(fileEntity.getStoreBucket(), fileEntity.getStoreId(), httpClientResponse -> {
            if(handler != null) {
                httpClientResponse.exceptionHandler(handler::onFailure);
                if (httpClientResponse.statusCode() == 200) {
                    httpClientResponse
                            .bodyHandler(handler::chunkReceived)
                            .endHandler(aVoid -> handler.onComplete());
                } else {
                    handler.onFailure(new Exception("Response code " + httpClientResponse.statusCode() +
                            " on GET using Bucket: " + fileEntity.getStoreBucket() + " StoreId: " + fileEntity.getStoreId()));
                }
            }
        });
    }

    @Override
    public void getAsyncPipe(FileEntity fileEntity, HttpClientRequest clientRequest, FileGetPipeHandler handler) {
        s3Client.get(fileEntity.getStoreBucket(), fileEntity.getStoreId(), httpClientResponse -> {
            if(handler != null) {
                httpClientResponse.exceptionHandler(handler::onFailure);
                if (httpClientResponse.statusCode() == 200) {
                    Pump.pump(httpClientResponse, clientRequest).start();
                    httpClientResponse
                            .endHandler(aVoid -> handler.onComplete());
                } else {
                    handler.onFailure(new Exception("Response code " + httpClientResponse.statusCode() +
                            " on GET using Bucket: " + fileEntity.getStoreBucket() + " StoreId: " + fileEntity.getStoreId()));
                }
            }
        });
    }

    @Override
    public void putAsync(FileEntity fileEntity, Buffer data, FileStatusHandler handler) {
        s3Client.put(fileEntity.getStoreBucket(), fileEntity.getStoreId(), data, httpClientResponse -> {
            if(handler != null) {
                httpClientResponse.exceptionHandler(handler::onFailure);
                if(httpClientResponse.statusCode() == 200) {
                    handler.onSuccess();
                } else {
                    handler.onFailure(new Exception("Failure with Response Code: " + httpClientResponse.statusCode()));
                }
            }
        });
    }

    @Override
    public void putAsync(FileEntity fileEntity, HttpServerFileUpload upload, FileStatusHandler handler) {
        s3Client.put(fileEntity.getStoreBucket(), fileEntity.getStoreId(), upload, httpClientResponse -> {
            if(handler != null) {
                httpClientResponse.exceptionHandler(handler::onFailure);
                if(httpClientResponse.statusCode() == 200) {
                    handler.onSuccess();
                } else {
                    handler.onFailure(new Exception("Failure with Response Code: " + httpClientResponse.statusCode()));
                }
            }
        });
    }

    @Override
    public void deleteAsync(FileEntity fileEntity , FileStatusHandler handler) {
        s3Client.delete(fileEntity.getStoreBucket(), fileEntity.getStoreId(), httpClientResponse -> {
            if(handler != null) {
                httpClientResponse.exceptionHandler(handler::onFailure);
                if(httpClientResponse.statusCode() == 200) {
                    handler.onSuccess();
                } else {
                    handler.onFailure(new Exception("Failure with Response Code: " + httpClientResponse.statusCode()));
                }
            }
        });
    }
}
