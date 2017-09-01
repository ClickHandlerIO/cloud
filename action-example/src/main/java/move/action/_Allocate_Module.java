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
public class _Allocate_Module {

  @Provides
  Allocate allocate() {
    return new Allocate();
  }

  @Provides
  @IntoMap
  @ClassKey(Allocate.class)
  ActionProvider<?, ?, ?> provideAllocate(
      Allocate_Provider provider) {
    return provider;
  }

  @Singleton
  static class Allocate_Provider extends InternalActionProvider<Allocate, String, String> {

    @Inject
    Allocate_Provider(@NotNull Vertx vertx,
        @NotNull Provider<Allocate> actionProvider) {
      super(vertx, actionProvider);
    }
  }
}
