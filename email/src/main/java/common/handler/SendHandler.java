package common.handler;

import entity.EmailEntity;

/**
 *  ASync callback interface for email sending service return.
 *
 *  @author Brad Behnke
 */
public interface SendHandler {
    /**
     *  return email entity with message id set
     */
    void onSuccess(EmailEntity emailEntity);
    // return failure reason
    void onFailure(Throwable e);
}
