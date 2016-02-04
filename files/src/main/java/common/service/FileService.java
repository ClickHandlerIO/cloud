package common.service;

import common.handler.FileGetChunksHandler;
import common.handler.FileGetPipeHandler;
import common.handler.FileStatusHandler;
import entity.FileEntity;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;

/**
 * Abstraction for all cloud file services
 *
 * @author Brad Behnke
 */
public abstract class FileService {
    public abstract void getAsyncChunks(FileEntity fileEntity, FileGetChunksHandler handler);
    public abstract void getAsyncPipe(FileEntity fileEntity, HttpClientRequest clientRequest, FileGetPipeHandler handler);
    public abstract void putAsync(FileEntity fileEntity, Buffer data, FileStatusHandler handler);
    public abstract void deleteAsync(FileEntity fileEntity, FileStatusHandler handler);
}
