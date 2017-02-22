package move.cluster;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.spi.cluster.AsyncMultiMap;
import io.vertx.core.spi.cluster.ChoosableIterable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class LocalAsyncMultiMap<K, V> implements AsyncMultiMap<K, V> {
    private final Multimap<K, V> map = Multimaps.forMap(new ConcurrentHashMap<>());

    @Override
    public void add(K k, V v, Handler<AsyncResult<Void>> completionHandler) {
        map.put(k, v);
        completionHandler.handle(Future.succeededFuture());
    }

    @Override
    public void get(K k, Handler<AsyncResult<ChoosableIterable<V>>> asyncResultHandler) {
        final Set<V> set = new ConcurrentHashSet<V>();
        set.addAll(map.get(k));
        asyncResultHandler.handle(Future.succeededFuture(new ChoosableSet<V>(set)));
    }

    @Override
    public void remove(K k, V v, Handler<AsyncResult<Boolean>> completionHandler) {
        completionHandler.handle(Future.succeededFuture(map.remove(k, v)));
    }

    @Override
    public void removeAllForValue(V v, Handler<AsyncResult<Void>> completionHandler) {
        final Iterator<V> iterator = map.values().iterator();
        while (iterator.hasNext()) {
            final V toCompare = iterator.next();
            if (v != null && v.equals(toCompare)) {
                iterator.remove();
            }
        }

        completionHandler.handle(Future.succeededFuture());
    }

    /**
     * @author <a href="http://tfox.org">Tim Fox</a>
     */
    class ChoosableSet<T> implements ChoosableIterable<T> {

        private final Set<T> ids;
        private volatile boolean initialised;
        private volatile Iterator<T> iter;

        public ChoosableSet(int initialSize) {
            ids = new ConcurrentHashSet<>(initialSize);
        }

        public ChoosableSet(Set<T> ids) {
            this.ids = ids;
        }

        public int size() {
            return ids.size();
        }

        public boolean isInitialised() {
            return initialised;
        }

        public void setInitialised() {
            this.initialised = true;
        }

        public void add(T elem) {
            ids.add(elem);
        }

        public void remove(T elem) {
            ids.remove(elem);
        }

        public void merge(ChoosableSet<T> toMerge) {
            ids.addAll(toMerge.ids);
        }

        public boolean isEmpty() {
            return ids.isEmpty();
        }

        @Override
        public Iterator<T> iterator() {
            return ids.iterator();
        }

        public synchronized T choose() {
            if (!ids.isEmpty()) {
                if (iter == null || !iter.hasNext()) {
                    iter = ids.iterator();
                }
                try {
                    return iter.next();
                } catch (NoSuchElementException e) {
                    return null;
                }
            } else {
                return null;
            }
        }
    }
}
