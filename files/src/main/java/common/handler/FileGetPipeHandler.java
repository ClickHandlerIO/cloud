package common.handler;

/**
 * Created by admin on 1/29/16.
 */
public interface FileGetPipeHandler {
    void onComplete();
    void onFailure(Throwable throwable);
}
