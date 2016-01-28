package common.data;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;

/**
 *  Request object for ses email attachment downloads.
 *
 *  @author Brad Behnke
 */
public class  DownloadRequest {
    private String fileId;
    private Handler<AsyncResult<Buffer>> completionHandler;

    public DownloadRequest(String fileId, Handler<AsyncResult<Buffer>> completionHandler) {
        this.fileId = fileId;
        this.completionHandler = completionHandler;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public Handler<AsyncResult<Buffer>> getCompletionHandler() {
        return completionHandler;
    }
}
