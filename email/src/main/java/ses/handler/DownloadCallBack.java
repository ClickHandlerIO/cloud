package ses.handler;

/**
 *  Callback for ASync attachment file downloads.
 *
 *  @author Brad Behnke
 */
public interface DownloadCallBack {
    void onSuccess(byte[] data);

    void onFailure(Exception e);
}
