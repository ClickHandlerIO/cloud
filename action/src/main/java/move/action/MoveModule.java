package move.action;

import dagger.Module;
import dagger.Provides;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.EventBus;
import javax.inject.Singleton;

/**
 *
 */
@Module
public class MoveModule {

  @Provides
  @Singleton
  Vertx vertx() {
    return MoveAppKt.locateVertx();
  }

  @Provides
  @Singleton
  io.vertx.core.Vertx vertxCore(Vertx vertx) {
    return vertx.getDelegate();
  }

  @Provides
  @Singleton
  EventBus eventBus(Vertx vertx) {
    return vertx.eventBus();
  }

  @Provides
  @Singleton
  io.vertx.core.eventbus.EventBus eventBusCore(Vertx vertx) {
    return vertx.getDelegate().eventBus();
  }
}
