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
public class _AllocateInventory_Module {

  @Provides
  @IntoMap
  @ClassKey(AllocateInventory.class)
  ActionProvider<?, ?, ?> _2(
      AllocateInventory_Provider provider) {
    return provider;
  }

  @Singleton
  static class AllocateInventory_Provider extends
      InternalActionProvider<AllocateInventory, AllocateInventory.Request, AllocateInventory.Reply> {

    @Inject
    AllocateInventory_Provider(@NotNull Vertx vertx,
        @NotNull Provider<AllocateInventory> actionProvider) {
      super(vertx, actionProvider);
    }
  }
}
