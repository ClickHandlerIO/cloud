package move.cluster;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.shareddata.AsyncMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 */
public class LocalAsyncMap<K, V> implements AsyncMap<K, V> {

  private final ConcurrentMap<K, V> map = new ConcurrentHashMap<>();

  @Override
  public void get(K k, Handler<AsyncResult<V>> asyncResultHandler) {
    asyncResultHandler.handle(Future.succeededFuture(map.get(k)));
  }

  @Override
  public void put(K k, V v, Handler<AsyncResult<Void>> completionHandler) {
    map.put(k, v);
    completionHandler.handle(Future.succeededFuture());
  }

  @Override
  public void put(K k, V v, long ttl, Handler<AsyncResult<Void>> completionHandler) {
    map.put(k, v);
    completionHandler.handle(Future.succeededFuture());
  }

  @Override
  public void putIfAbsent(K k, V v, Handler<AsyncResult<V>> completionHandler) {
    completionHandler.handle(Future.succeededFuture(map.putIfAbsent(k, v)));
  }

  @Override
  public void putIfAbsent(K k, V v, long ttl, Handler<AsyncResult<V>> completionHandler) {
    completionHandler.handle(Future.succeededFuture(map.putIfAbsent(k, v)));
  }

  @Override
  public void remove(K k, Handler<AsyncResult<V>> asyncResultHandler) {
    asyncResultHandler.handle(Future.succeededFuture(map.remove(k)));
  }

  @Override
  public void removeIfPresent(K k, V v, Handler<AsyncResult<Boolean>> resultHandler) {
    resultHandler.handle(Future.succeededFuture(map.remove(k, v)));
  }

  @Override
  public void replace(K k, V v, Handler<AsyncResult<V>> asyncResultHandler) {
    asyncResultHandler.handle(Future.succeededFuture(map.replace(k, v)));
  }

  @Override
  public void replaceIfPresent(K k, V oldValue, V newValue,
      Handler<AsyncResult<Boolean>> resultHandler) {
    resultHandler.handle(Future.succeededFuture(map.replace(k, oldValue, newValue)));
  }

  @Override
  public void clear(Handler<AsyncResult<Void>> resultHandler) {
    map.clear();
    resultHandler.handle(Future.succeededFuture());
  }

  @Override
  public void size(Handler<AsyncResult<Integer>> resultHandler) {
    resultHandler.handle(Future.succeededFuture(map.size()));
  }
}
