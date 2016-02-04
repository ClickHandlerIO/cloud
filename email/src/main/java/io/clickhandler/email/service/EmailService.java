package io.clickhandler.email.service;

import com.google.common.util.concurrent.AbstractIdleService;
import io.clickhandler.email.common.data.SendRequest;
import io.clickhandler.email.entity.EmailEntity;
import rx.Observable;

/**
 *  Abstract for cloud email services.
 *
 *  @author Brad Behnke
 */
public abstract class EmailService<T extends SendRequest> extends AbstractIdleService {

    public abstract Observable<EmailEntity> sendObservable(T sendRequest);
}
