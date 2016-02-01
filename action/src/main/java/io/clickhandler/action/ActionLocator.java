package io.clickhandler.action;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public abstract class ActionLocator {
    protected final List<ActionLocator> children = new ArrayList<>();
    private final Map<Object, RemoteActionProvider<?, ?, ?>> remoteActionMap = new HashMap<>();
    private final Map<Object, QueueActionProvider<?, ?, ?>> queueActionMap = new HashMap<>();
    private final Map<Object, InternalActionProvider<?, ?, ?>> internalActionMap = new HashMap<>();
    protected Map<Object, ActionProvider<?, ?, ?>> actionMap;
    private ActionManager actionManager;

    @Inject
    void setActionManager(ActionManager actionManager) {
        this.actionManager = actionManager;
    }

    protected Map<Object, ActionProvider<?, ?, ?>> ensureActionMap() {
        if (actionMap == null) {
            synchronized (this) {
                if (actionMap != null) return actionMap;
                actionMap = new HashMap<>();
                init();
            }
        }
        return actionMap;
    }

    /**
     * @param cls
     * @param <A>
     * @param <IN>
     * @param <OUT>
     * @return
     */
    public <A extends Action<IN, OUT>, IN, OUT> ActionProvider<A, IN, OUT> locate(String cls) {
        return (ActionProvider<A, IN, OUT>) ensureActionMap().get(cls);
    }

    /**
     * @param key
     * @param <A>
     * @param <IN>
     * @param <OUT>
     * @return
     */
    public <A extends Action<IN, OUT>, IN, OUT> ActionProvider<A, IN, OUT> locate(Object key) {
        return (ActionProvider<A, IN, OUT>) ensureActionMap().get(key);
    }

    /**
     * @param key
     * @param <A>
     * @param <IN>
     * @param <OUT>
     * @return
     */
    public <A extends Action<IN, OUT>, IN, OUT> ActionProvider<A, IN, OUT> locateRemote(Object key) {
        ensureActionMap();
        return (RemoteActionProvider<A, IN, OUT>) remoteActionMap.get(key);
    }

    /**
     * @return
     */
    public Map<Object, ActionProvider<?, ?, ?>> getProviderMap() {
        return new HashMap<>(ensureActionMap());
    }

    /**
     * @return
     */
    public Map<Object, RemoteActionProvider<?, ?, ?>> getRemoteProviderMap() {
        ensureActionMap();
        return new HashMap<>(remoteActionMap);
    }

    /**
     * @return
     */
    public Map<String, RemoteActionProvider> getRemotePathMap() {
        ensureActionMap();
        final Map<String, RemoteActionProvider> map = new HashMap<>();
        remoteActionMap.forEach((key, value) -> {
            map.put(value.getRemoteAction().path(), value);
        });
        return map;
    }

    private void init() {
        // Init actions.
        initActions();

        // Load child locators.
        initChildren();

        children.forEach(locator -> {
            final Map<Object, ActionProvider<?, ?, ?>> childActions = locator.ensureActionMap();
            if (childActions != null) {
                actionMap.putAll(childActions);
            }
        });

        actionMap.forEach((key, value) -> {
            if (value == null) return;
            if (value instanceof RemoteActionProvider) {
                remoteActionMap.put(key, (RemoteActionProvider<?, ?, ?>) value);
            } else if (value instanceof QueueActionProvider) {
                queueActionMap.put(key, (QueueActionProvider<?, ?, ?>) value);
            } else if (value instanceof InternalActionProvider) {
                internalActionMap.put(key, (InternalActionProvider<?, ?, ?>) value);
            }
        });
    }

    protected void initActions() {

    }

    protected void initChildren() {

    }
}
