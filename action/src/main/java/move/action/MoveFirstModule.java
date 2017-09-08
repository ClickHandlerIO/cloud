package move.action;

import dagger.Module;
import dagger.Provides;
import io.vertx.rxjava.core.Vertx;
import javax.inject.Singleton;

/**
 *
 */
@Module
public class MoveFirstModule {

  @Provides
  @Singleton
  Vertx vertx() {
    return MoveKt.locateVertx();
  }
}
