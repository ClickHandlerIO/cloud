package io.clickhandler.action;

import com.google.common.collect.ImmutableMap;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
@SuppressWarnings("all")
public abstract class ActionLocator {
    protected final List<ActionLocator> children = new ArrayList<>();
    private final Map<Object, RemoteActionProvider<?, ?, ?>> remoteActionMap = new HashMap<>();
    private final Map<Object, InternalActionProvider<?, ?, ?>> internalActionMap = new HashMap<>();
    private final Map<Object, WorkerActionProvider<?, ?>> workerActionMap = new HashMap<>();
    private final Map<Object, ScheduledActionProvider<?>> scheduledActionMap = new HashMap<>();
    protected Map<Object, ActionProvider<?, ?, ?>> actionMap = new HashMap<>();
    private ActionManager actionManager;
    private boolean inited;

    @Inject
    void setActionManager(ActionManager actionManager) {
        this.actionManager = actionManager;
    }

    public Map<Object, RemoteActionProvider<?, ?, ?>> getRemoteActionMap() {
        return ImmutableMap.copyOf(remoteActionMap);
    }

    public Map<Object, InternalActionProvider<?, ?, ?>> getInternalActionMap() {
        return ImmutableMap.copyOf(internalActionMap);
    }

    public Map<Object, WorkerActionProvider<?, ?>> getWorkerActionMap() {
        return ImmutableMap.copyOf(workerActionMap);
    }

    public Map<Object, ScheduledActionProvider<?>> getScheduledActionMap() {
        return ImmutableMap.copyOf(scheduledActionMap);
    }

    public Map<Object, ActionProvider<?, ?, ?>> getActionMap() {
        return ImmutableMap.copyOf(actionMap);
    }

    public void register() {
        ensureActionMap();
    }

    public Map<Object, ActionProvider<?, ?, ?>> ensureActionMap() {
        init();
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

    private synchronized void init() {
        if (inited)
            return;

        inited = true;

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
                if (remoteActionMap.containsKey(key)) {
                    final RemoteActionProvider actionProvider = remoteActionMap.get(key);
                    throw new RuntimeException("Duplicate RemoteAction Entry for key [" + key + "]. " +
                        value.getActionClass().getCanonicalName() + " and " + actionProvider.getActionClass().getCanonicalName());
                }
                remoteActionMap.put(key, (RemoteActionProvider<?, ?, ?>) value);
            } else if (value instanceof InternalActionProvider) {
                internalActionMap.put(key, (InternalActionProvider<?, ?, ?>) value);
            } else if (value instanceof WorkerActionProvider) {
                workerActionMap.put(key, (WorkerActionProvider<?, ?>) value);
                workerActionMap.put(value.getActionClass().getCanonicalName(), (WorkerActionProvider<?, ?>) value);
            } else if (value instanceof ScheduledActionProvider) {
                scheduledActionMap.put(key, (ScheduledActionProvider<?>)value);
            }
        });

        ActionManager.register(actionMap);
    }

    protected void initActions() {

    }

    protected void initChildren() {

    }
}
