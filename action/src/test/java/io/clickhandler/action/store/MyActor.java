package io.clickhandler.action.store;

import io.clickhandler.action.AbstractActor;
import io.vertx.rxjava.core.Future;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MyActor extends AbstractActor {
    private List<String> watchers = new ArrayList<>();

    @Override
    protected void finishStart(Future startFuture) {
        startFuture.complete();
    }

    @Override
    protected void finishStop(Future stopFuture) {
        stopFuture.complete();
    }

    public void addWatcher(String watcher) {
        watchers.add(watcher);
    }

    public List<String> getWatchers() {
        return watchers;
    }
}
