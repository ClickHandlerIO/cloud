package io.clickhandler.action;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import io.vertx.rxjava.core.Vertx;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * Monitors and manages all Actions.
 *
 * @author Clay Molocznik
 */
@Singleton
public class ActionManager extends AbstractExecutionThreadService {
    private final static Map<Object, ActionProvider<?, ?, ?>> actionProviderMap = new HashMap<>();
    private final static Map<Object, RemoteActionProvider<?, ?, ?>> remoteActionMap = new HashMap<>();
    private final static Map<Object, QueueActionProvider<?, ?, ?>> queueActionMap = new HashMap<>();
    private final static Map<Object, InternalActionProvider<?, ?, ?>> internalActionMap = new HashMap<>();
    private final static Map<Object, ActorActionProvider<?, ?, ?, ?>> actionActionMap = new HashMap<>();

    private final static Map<String, ActorActionProvider<?, ?, ?, ?>> actorActionsByNameMap = new HashMap<>();
    private final static Map<Object, ActorManager> actorManagerMap = new HashMap<>();

    @Inject
    Vertx vertx;
    @Inject
    HazelcastProvider hazelcast;

    private ActorActionSerializer actorActionSerializer = new ActorActionSerializerImpl();

    @Inject
    public ActionManager() {
    }

    public ActorActionSerializer getActorActionSerializer() {
        return actorActionSerializer;
    }

    public void setActorActionSerializer(ActorActionSerializer actorActionSerializer) {
        this.actorActionSerializer = actorActionSerializer;
    }

    @Override
    protected void startUp() throws Exception {
    }

    @Override
    protected void shutDown() throws Exception {
    }

    @Override
    protected void run() throws Exception {
        while (isRunning()) {
            Thread.sleep(5000);
        }
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
                remoteActionMap.put(key, (RemoteActionProvider<?, ?, ?>) value);
            } else if (value instanceof QueueActionProvider) {
                queueActionMap.put(key, (QueueActionProvider<?, ?, ?>) value);
            } else if (value instanceof InternalActionProvider) {
                internalActionMap.put(key, (InternalActionProvider<?, ?, ?>) value);
            } else if (value instanceof ActorActionProvider<?, ?, ?, ?>) {
                ActorActionProvider<?, ?, ?, ?> actorActionProvider = (ActorActionProvider<?, ?, ?, ?>) value;

                ActorManager actorManager = actorManagerMap.get(actorActionProvider.getActorFactory());
                if (actorManager == null) {
                    actorManager = new ActorManager(vertx, hazelcast.get(), this, actorActionProvider.getActorFactory());
                    actorManagerMap.put(actorActionProvider.getActorFactory(), actorManager);
                }
                actorManager.addStoreActionProvider(actorActionProvider);
                actorActionProvider.setActorManager(actorManager);

                actorActionsByNameMap.put(actorActionProvider.getName(), actorActionProvider);
                actionActionMap.put(key, actorActionProvider);
            }
        });
    }

    public void setExecutionTimeoutEnabled(boolean enabled) {
        actionProviderMap.forEach((k, v) -> v.setExecutionTimeoutEnabled(enabled));
    }
}
