package move.cluster;

import com.google.common.collect.Lists;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.Counter;
import io.vertx.core.shareddata.Lock;
import io.vertx.core.shareddata.impl.AsynchronousLock;
import io.vertx.core.spi.cluster.AsyncMultiMap;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.core.spi.cluster.NodeListener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class LocalClusterManager implements ClusterManager {
    private final Map<String, Map<?, ?>> mapsMap = new ConcurrentHashMap<>();
    private final Map<String, AsyncMap<?, ?>> asyncMapsMap = new ConcurrentHashMap<>();
    private final Map<String, AsyncMultiMap<?, ?>> asyncMultMapsMap = new ConcurrentHashMap<>();
    private final Map<String, Counter> countersMap = new ConcurrentHashMap<>();
    private final Map<String, Lock> locksMap = new ConcurrentHashMap<>();

    private Vertx vertx;
    private NodeListener listener;

    @Override
    public void setVertx(io.vertx.core.Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public <K, V> void getAsyncMultiMap(String name, Handler<AsyncResult<AsyncMultiMap<K, V>>> asyncResultHandler) {
        asyncResultHandler.handle(Future.succeededFuture(
            (AsyncMultiMap<K, V>) asyncMultMapsMap.getOrDefault(name, new LocalAsyncMultiMap<>())
        ));
    }

    @Override
    public <K, V> void getAsyncMap(String name, Handler<AsyncResult<AsyncMap<K, V>>> asyncResultHandler) {
        asyncResultHandler.handle(Future.succeededFuture(
            (AsyncMap<K, V>) asyncMapsMap.getOrDefault(name, new LocalAsyncMap<>())
        ));
    }

    @Override
    public <K, V> Map<K, V> getSyncMap(String name) {
        return (Map<K, V>) mapsMap.getOrDefault(name, new ConcurrentHashMap<>());
    }

    @Override
    public void getLockWithTimeout(String name, long timeout, Handler<AsyncResult<Lock>> resultHandler) {
        resultHandler.handle(Future.succeededFuture(
            locksMap.getOrDefault(name, new AsynchronousLock(vertx))
        ));
    }

    @Override
    public void getCounter(String name, Handler<AsyncResult<Counter>> resultHandler) {
        resultHandler.handle(Future.succeededFuture(
            countersMap.getOrDefault(name, new AsynchronousCounter(vertx))
        ));
    }

    @Override
    public String getNodeID() {
        return "ONLY_ONE_NODE";
    }

    @Override
    public List<String> getNodes() {
        return Lists.newArrayList(getNodeID());
    }

    @Override
    public void nodeListener(NodeListener listener) {
        this.listener = listener;
    }

    @Override
    public void join(Handler<AsyncResult<Void>> resultHandler) {
        if (listener != null) {
            listener.nodeAdded(getNodeID());
        }
        resultHandler.handle(Future.succeededFuture());
    }

    @Override
    public void leave(Handler<AsyncResult<Void>> resultHandler) {
        if (listener != null) {
            listener.nodeLeft(getNodeID());
        }
        resultHandler.handle(Future.succeededFuture());
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
