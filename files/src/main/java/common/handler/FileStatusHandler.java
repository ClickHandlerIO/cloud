package common.handler;

/**
 * Created by admin on 1/29/16.
 */
public interface FileStatusHandler {
    void onSuccess();
    void onFailure(Throwable throwable);
}
