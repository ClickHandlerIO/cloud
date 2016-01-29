package common.handler;

import io.vertx.core.buffer.Buffer;

/**
 * Created by admin on 1/29/16.
 */
public interface FileGetHandler {
    void chunkReceived(Buffer chunk);
    void onComplete();
    void onFailure(Throwable throwable);
}
