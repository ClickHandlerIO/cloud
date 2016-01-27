package common.service;

import entity.FileEntity;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerFileUpload;

import java.util.concurrent.Future;

/**
 * Created by admin on 1/27/16.
 */
public abstract class AbstractFileService {
    public abstract Future<Buffer> get(final FileEntity fileEntity);
    public abstract Future<Integer> put(final FileEntity fileEntity, final Buffer data);
    public abstract Future<Integer> put(final FileEntity fileEntity, final HttpServerFileUpload upload);
    public abstract Future<Integer> delete(final FileEntity fileEntity);
}
