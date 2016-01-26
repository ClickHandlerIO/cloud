package ses.data;

import com.google.common.base.Preconditions;
import ses.handler.DownloadCallBack;

/**
 * Created by Brad on 1/19/16.
 */
public class  DownloadRequest {
    private String fileId;
    private DownloadCallBack callBack;

    public DownloadRequest(String fileId, DownloadCallBack callBack) {
        Preconditions.checkNotNull(callBack);
        Preconditions.checkNotNull(fileId);
        this.callBack = callBack;
        this.fileId = fileId;
    }

    public DownloadCallBack getCallBack() {
        return callBack;
    }

    public void setCallBack(DownloadCallBack callBack) {
        this.callBack = callBack;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
}
