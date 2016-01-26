package ses.handler;

/**
 * Created by admin on 1/19/16.
 */
public interface DownloadCallBack {
    void onSuccess(byte[] data);

    void onFailure(Exception e);
}
