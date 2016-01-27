package common.handler;

import entity.EmailEntity;

/**
 * Created by Brad Behnke on 1/26/16.
 */
public interface SendHandler {
    // return email entity with message id set
    void onSuccess(EmailEntity emailEntity);
    // return failure reason
    void onFailure(Throwable e);
}
