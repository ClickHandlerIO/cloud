package io.clickhandler.action;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.netflix.hystrix.Hystrix;

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
    private final static Map<Object, QueueActionProvider<?, ?, ?>> internalActionMap = new HashMap<>();

    @Inject
    public ActionManager() {
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

    void register(Map<Object, ActionProvider<?, ?, ?>> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        actionProviderMap.putAll(map);

        map.forEach((key, value) -> {
            if (value == null) {
                return;
            }
            value.getActionClass().getAnnotation(ActionConfig.class);
            if (value instanceof RemoteActionProvider) {
                remoteActionMap.put(key, (RemoteActionProvider<?, ?, ?>) value);
            } else if (value instanceof QueueActionProvider) {
                queueActionMap.put(key, (QueueActionProvider<?, ?, ?>) value);
            } else if (value instanceof InternalActionProvider) {

            }
        });
    }

    enum Type {
        INTERNAL,
        QUEUE,
        REMOTE,;
    }

    private static final class ActionHolder<A extends ActionProvider> {
        private ActionConfig defaultConfig;
        private A actionProvider;
        private Type type;

        public ActionHolder(ActionConfig defaultConfig, A actionProvider, Type type) {
            this.defaultConfig = defaultConfig;
            this.actionProvider = actionProvider;
            this.type = type;
        }
    }
}
