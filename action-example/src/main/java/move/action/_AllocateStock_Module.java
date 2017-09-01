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
public class _AllocateStock_Module {

  @Provides
  AllocateStock _1() {
    return new AllocateStock();
  }

  @Provides
  @IntoMap
  @ClassKey(AllocateStock.class)
  ActionProvider<?, ?, ?> _2(
      AllocateStock_Provider provider) {
    return provider;
  }

  @Singleton
  static class AllocateStock_Provider extends InternalActionProvider<AllocateStock, AllocateStock.Request, AllocateStock.Reply> {

    @Inject
    AllocateStock_Provider(@NotNull Vertx vertx,
        @NotNull Provider<AllocateStock> actionProvider) {
      super(vertx, actionProvider);
    }
  }
}
