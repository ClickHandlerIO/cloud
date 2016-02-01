package common.data;

import common.handler.FileGetChunksHandler;

/**
 *  Request object for ses email attachment downloads.
 *
 *  @author Brad Behnke
 */
public class  DownloadRequest {
    private String fileId;
    private FileGetChunksHandler handler;

    public DownloadRequest(String fileId, FileGetChunksHandler handler) {
        this.fileId = fileId;
        this.handler = handler;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public FileGetChunksHandler getHandler() {
        return handler;
    }
}
