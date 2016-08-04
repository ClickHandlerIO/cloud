package io.clickhandler.action;

import io.vertx.core.Future;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Monitors and manages all Actions.
 *
 * @author Clay Molocznik
 */
@Singleton
public class ActionManager extends AbstractVerticle {
    private final static Logger log = LoggerFactory.getLogger(ActionManager.class);
    private final static Map<Object, ActionProvider<?, ?, ?>> actionProviderMap = new HashMap<>();
    private final static Map<Object, RemoteActionProvider<?, ?, ?>> remoteActionMap = new HashMap<>();
    private final static Map<Object, InternalActionProvider<?, ?, ?>> internalActionMap = new HashMap<>();
    private final static Map<Object, WorkerActionProvider<?, ?>> workerActionMap = new HashMap<>();

    @Inject
    Vertx vertx;

    @Inject
    public ActionManager() {
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        startFuture.complete();
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        stopFuture.complete();
    }

    public static Map<Object, ActionProvider<?, ?, ?>> getActionProviderMap() {
        return Collections.unmodifiableMap(actionProviderMap);
    }

    public static Map<Object, RemoteActionProvider<?, ?, ?>> getRemoteActionMap() {
        return Collections.unmodifiableMap(remoteActionMap);
    }

    public static Map<Object, InternalActionProvider<?, ?, ?>> getInternalActionMap() {
        return Collections.unmodifiableMap(internalActionMap);
    }

    public static Map<Object, WorkerActionProvider<?, ?>> getWorkerActionMap() {
        return Collections.unmodifiableMap(workerActionMap);
    }

    public static WorkerActionProvider<? , ?> getWorkerAction(String name) {
        return workerActionMap.get(name);
    }

    synchronized void register(Map<Object, ActionProvider<?, ?, ?>> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        actionProviderMap.putAll(map);

        map.forEach((key, value) -> {
            if (value == null) {
                return;
            }
            if (value instanceof RemoteActionProvider) {
                if (key instanceof String) {
                    if (remoteActionMap.containsKey(key)) {
                        final RemoteActionProvider actionProvider = remoteActionMap.get(key);
                        throw new RuntimeException("Duplicate RemoteAction Entry for key [" + key + "]. " + value.getActionClass().getCanonicalName() + " and " + actionProvider.getActionClass().getCanonicalName());
                    }
                }
                remoteActionMap.put(key, (RemoteActionProvider<?, ?, ?>) value);
            } else if (value instanceof InternalActionProvider) {
                internalActionMap.put(key, (InternalActionProvider<?, ?, ?>) value);
            } else if (value instanceof WorkerActionProvider) {
                workerActionMap.put(key, (WorkerActionProvider<?, ?>)value);
                workerActionMap.put(value.getActionClass().getCanonicalName(), (WorkerActionProvider<?, ?>)value);
            }
        });
    }

    public void setExecutionTimeoutEnabled(boolean enabled) {
        actionProviderMap.forEach((k, v) -> v.setExecutionTimeoutEnabled(enabled));
    }
}
