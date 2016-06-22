package io.clickhandler.action.actor.sub;

import io.clickhandler.action.AbstractObservableAction;
import io.clickhandler.action.InternalAction;

import javax.inject.Inject;

/**
 *
 */
@InternalAction
//@RemoteAction(path = "/some/path")
public class AllocateInventory2 extends AbstractObservableAction<String, String> {
    @Inject
    public AllocateInventory2() {
    }

    @Override
    protected void start(String request) {
        String r = "T";
        respond("TEST2");
    }
}
