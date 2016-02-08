package io.clickhandler.files.handler;


import io.vertx.rxjava.core.buffer.Buffer;

/**
 * Created by admin on 1/29/16.
 */
public interface FileGetChunksHandler {
    void chunkReceived(Buffer chunk);
    void onComplete();
    void onFailure(Throwable throwable);
}
