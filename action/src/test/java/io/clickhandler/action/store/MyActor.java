package io.clickhandler.action.store;

import io.clickhandler.action.AbstractActor;
import javaslang.collection.List;

import javax.inject.Inject;

/**
 *
 */
public class MyActor extends AbstractActor {
    private List<String> watchers = List.of();

    @Inject
    public MyActor() {
    }

    public void addWatcher(String watcher) {
        watchers = watchers.append(watcher);
    }

    public List<String> getWatchers() {
        return watchers;
    }

    @Override
    protected void started() {
    }
}
