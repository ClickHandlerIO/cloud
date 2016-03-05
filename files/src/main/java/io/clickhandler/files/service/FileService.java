package io.clickhandler.files.service;

import io.clickhandler.cloud.model.FileEntity;
import io.clickhandler.files.handler.FileGetChunksHandler;
import io.clickhandler.files.handler.FileGetPipeHandler;
import io.clickhandler.files.handler.FileStatusHandler;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpServerFileUpload;

/**
 * Abstraction for all cloud file services
 *
 * @author Brad Behnke
 */
public abstract class FileService {
    public abstract void getAsyncChunks(FileEntity fileEntity, FileGetChunksHandler handler);

    public abstract void getAsyncPipe(FileEntity fileEntity, HttpClientRequest clientRequest, FileGetPipeHandler handler);

    public abstract void putAsync(FileEntity fileEntity, Buffer data, FileStatusHandler handler);

    public abstract void putAsync(FileEntity fileEntity, HttpServerFileUpload upload, FileStatusHandler handler);

    public abstract void deleteAsync(FileEntity fileEntity, FileStatusHandler handler);
}
