package common.data;

import common.handler.FileGetHandler;

/**
 *  Request object for ses email attachment downloads.
 *
 *  @author Brad Behnke
 */
public class  DownloadRequest {
    private String fileId;
    private FileGetHandler handler;

    public DownloadRequest(String fileId, FileGetHandler handler) {
        this.fileId = fileId;
        this.handler = handler;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public FileGetHandler getHandler() {
        return handler;
    }
}
