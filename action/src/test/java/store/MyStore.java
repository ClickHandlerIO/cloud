package store;

import io.clickhandler.action.AbstractStore;
import io.vertx.rxjava.core.Future;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MyStore extends AbstractStore {
    private List<String> watchers = new ArrayList<>();

    @Override
    protected void startUp(Future startFuture) {
        startFuture.complete();
    }

    @Override
    protected void shutDown(Future stopFuture) {
        stopFuture.complete();
    }

    public void addWatcher(String watcher) {
        watchers.add(watcher);
    }

    public List<String> getWatchers() {
        return watchers;
    }
}
