package io.clickhandler.action.store;

import io.clickhandler.action.AbstractActor;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MyActor extends AbstractActor {
    private List<String> watchers = new ArrayList<>();

    @Inject
    public MyActor() {
    }

    public void addWatcher(String watcher) {
        watchers.add(watcher);
    }

    public List<String> getWatchers() {
        return watchers;
    }

    @Override
    protected void started() {

    }
}
