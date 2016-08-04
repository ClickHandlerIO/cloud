package io.clickhandler.action;

import com.google.common.util.concurrent.AbstractIdleService;
import rx.Observable;

import javax.inject.Inject;

/**
 *
 */
public class LocalWorkerService extends AbstractIdleService implements WorkerSender, WorkerReceiver {
    @Inject
    ActionManager actionManager;

    @Inject
    public LocalWorkerService() {
    }

    @Override
    protected void startUp() throws Exception {
    }

    @Override
    protected void shutDown() throws Exception {
    }

    @Override
    public Observable<Boolean> send(WorkerRequest request) {
        return null;
    }
}
