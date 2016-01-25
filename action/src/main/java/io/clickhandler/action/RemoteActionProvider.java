package io.clickhandler.action;

import javax.inject.Inject;

/**
 *
 */
public class RemoteActionProvider<A extends Action<IN, OUT>, IN, OUT> extends ActionProvider<A, IN, OUT> {
    private RemoteAction remoteAction;

    @Inject
    public RemoteActionProvider() {
    }

    @Override
    protected void init() {
        super.init();
        remoteAction = getActionClass().getAnnotation(RemoteAction.class);
    }
}
