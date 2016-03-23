package io.clickhandler.action;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.hazelcast.core.HazelcastInstance;
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
    private final static Map<Object, StoreActionProvider<?, ?, ?, ?>> storeActionMap = new HashMap<>();

    private final static Map<String, StoreActionProvider<?, ?, ?, ?>> storeActionsByName = new HashMap<>();
    private final static Map<String, StoreManager> storeFactoryMap = new HashMap<>();

    @Inject
    Vertx vertx;
    @Inject
    HazelcastInstance hazelcast;

    private StoreActionSerializer storeActionSerializer = new StoreActionSerializerImpl();

    @Inject
    public ActionManager() {
    }

    public StoreActionSerializer getStoreActionSerializer() {
        return storeActionSerializer;
    }

    public void setStoreActionSerializer(StoreActionSerializer storeActionSerializer) {
        this.storeActionSerializer = storeActionSerializer;
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

    public StoreActionProvider<?, ?, ?, ?> getStoreAction(String key) {
        return storeActionsByName.get(key);
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
            } else if (value instanceof StoreActionProvider<?, ?, ?, ?>) {
                StoreActionProvider<?, ?, ?, ?> storeActionProvider = (StoreActionProvider<?, ?, ?, ?>) value;

                final StoreManager storeManager = new StoreManager(vertx, hazelcast, this, storeActionProvider.getStoreProvider());
                storeManager.addStoreActionProvider(storeActionProvider);

                storeFactoryMap.putIfAbsent(storeActionProvider.getName(), storeManager);
                storeActionsByName.put(storeActionProvider.getName(), storeActionProvider);
                storeActionMap.put(key, storeActionProvider);
            }
        });
    }

    public void setExecutionTimeoutEnabled(boolean enabled) {
        actionProviderMap.forEach((k, v) -> v.setExecutionTimeoutEnabled(enabled));
    }
}
