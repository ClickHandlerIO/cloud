package common.service;

import com.google.common.util.concurrent.AbstractIdleService;
import common.data.AbstractSendRequest;

/**
 *  Abstract for cloud email services.
 *
 *  @author Brad Behnke
 */
public abstract class AbstractEmailService<T extends AbstractSendRequest> extends AbstractIdleService {
    public abstract void send(T sendRequest);
}
