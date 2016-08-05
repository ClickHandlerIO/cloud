package io.clickhandler.action;

import com.google.common.util.concurrent.AbstractIdleService;
import io.clickhandler.cloud.cluster.HazelcastProvider;
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
public class ActionManager extends AbstractIdleService {
    private final static Logger LOG = LoggerFactory.getLogger(ActionManager.class);
    private final static Map<Object, ActionProvider<?, ?, ?>> actionProviderMap = new HashMap<>();
    private final static Map<Object, RemoteActionProvider<?, ?, ?>> remoteActionMap = new HashMap<>();
    private final static Map<Object, InternalActionProvider<?, ?, ?>> internalActionMap = new HashMap<>();
    private final static Map<Object, WorkerActionProvider<?, ?>> workerActionMap = new HashMap<>();
    private final static Map<Object, ScheduledActionProvider<?>> scheduledActionMap = new HashMap<>();

    private static ActionManagerConfig config = new ActionManagerConfig();

    @Inject
    Vertx vertx;
    @Inject
    HazelcastProvider hazelcastProvider;
    @Inject
    WorkerService workerService;
    @Inject
    ScheduledActionManager scheduledActionManager;

    @Inject
    ActionManager() {
    }

    public static ActionManagerConfig getConfig() {
        return config;
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

    public static Map<Object, ScheduledActionProvider<?>> getScheduledActionMap() {
        return Collections.unmodifiableMap(scheduledActionMap);
    }

    public static void setExecutionTimeoutEnabled(boolean enabled) {
        actionProviderMap.forEach((k, v) -> v.setExecutionTimeoutEnabled(enabled));
    }

    @Override
    protected void startUp() throws Exception {
        workerService.startAsync().awaitRunning();
        scheduledActionManager.startAsync().awaitRunning();
    }

    @Override
    protected void shutDown() throws Exception {
        scheduledActionManager.stopAsync().awaitTerminated();
        workerService.stopAsync().awaitTerminated();
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
                workerActionMap.put(key, (WorkerActionProvider<?, ?>) value);
                workerActionMap.put(value.getActionClass().getCanonicalName(), (WorkerActionProvider<?, ?>) value);
            } else if (value instanceof ScheduledActionProvider) {
                scheduledActionMap.put(key, (ScheduledActionProvider<?>)value);
            }
        });
    }
}
