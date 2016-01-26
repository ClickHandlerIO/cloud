package common;

import com.google.common.util.concurrent.AbstractIdleService;

/**
 * Created by admin on 1/26/16.
 */
public abstract class AbstractEmailService extends AbstractIdleService {
    public abstract void send(AbstractSendRequest sendRequest);
}
