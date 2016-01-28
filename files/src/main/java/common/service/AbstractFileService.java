package common.service;

import entity.FileEntity;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerFileUpload;
import rx.Observable;

/**
 * Abstraction for all cloud file services
 *
 * @author Brad Behnke
 */
public abstract class AbstractFileService {
    public abstract Observable<Buffer> getObservable(FileEntity fileEntity);
    public abstract Observable<Integer> putObservable(FileEntity fileEntity, Buffer data);
    public abstract Observable<Integer> putObservable(FileEntity fileEntity, HttpServerFileUpload upload);
    public abstract Observable<Integer> deleteObservable(FileEntity fileEntity);
}
