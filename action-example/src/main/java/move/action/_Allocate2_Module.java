package move.action;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ClassKey;
import dagger.multibindings.IntoMap;
import io.vertx.rxjava.core.Vertx;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
@Module
public class _Allocate2_Module {

  @Provides
  Allocate2 _1() {
    return new Allocate2();
  }

  @Provides
  @IntoMap
  @ClassKey(Allocate2.class)
  ActionProvider<?, ?, ?> _2(
      Allocate2_Provider provider) {
    return provider;
  }

  @Singleton
  static class Allocate2_Provider extends InternalActionProvider<Allocate2, String, String> {

    @Inject
    Allocate2_Provider(@NotNull Vertx vertx,
        @NotNull Provider<Allocate2> actionProvider) {
      super(vertx, actionProvider);
    }
  }
}
