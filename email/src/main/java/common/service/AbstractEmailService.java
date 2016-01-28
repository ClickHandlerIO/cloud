package common.service;

import com.google.common.util.concurrent.AbstractIdleService;
import common.data.AbstractSendRequest;
import entity.EmailEntity;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import rx.Observable;

/**
 *  Abstract for cloud email services.
 *
 *  @author Brad Behnke
 */
public abstract class AbstractEmailService<T extends AbstractSendRequest> extends AbstractIdleService {

    public abstract Observable<EmailEntity> sendObservable(T sendRequest);

    protected abstract void send(T sendRequest, Handler<AsyncResult<EmailEntity>> completionHandler);
}
