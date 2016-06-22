package io.clickhandler.action;

import javax.inject.Inject;

/**
 *
 */
@InternalAction
//@RemoteAction(path = "/some/path")
public class AllocateInventory extends AbstractObservableAction<String, String> {
    @Inject
    public AllocateInventory() {
    }

    @Override
    protected void start(String request) {
        String r = "T";
        respond("TEST2");
    }
}
