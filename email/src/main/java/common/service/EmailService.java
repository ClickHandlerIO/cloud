package common.service;

import com.google.common.util.concurrent.AbstractIdleService;
import common.data.SendRequest;
import entity.EmailEntity;
import rx.Observable;

/**
 *  Abstract for cloud email services.
 *
 *  @author Brad Behnke
 */
public abstract class EmailService<T extends SendRequest> extends AbstractIdleService {

    public abstract Observable<EmailEntity> sendObservable(T sendRequest);
}
