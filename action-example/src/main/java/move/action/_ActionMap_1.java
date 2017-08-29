package move.action;

import dagger.MembersInjector;
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
public class _ActionMap_1 {

  @Provides
  @IntoMap
  @ClassKey(Allocate.class)
  ActionProvider<?, ?, ?> provideAllocate(
      Allocate_Provider provider) {
    return provider;
  }

  @Provides
  @IntoMap
  @ClassKey(Allocate2.class)
  ActionProvider<?, ?, ?> provideAllocate2(
      Allocate2_Provider provider) {
    return provider;
  }

  @Provides
  @IntoMap
  @ClassKey(AllocateInventory.class)
  ActionProvider<?, ?, ?> provideAllocateInventory(
      AllocateInventory_Provider provider) {
    return provider;
  }

  @Singleton
  static class Allocate_Provider extends InternalActionProvider<Allocate, String, String> {
    @Inject
    public Allocate_Provider(@NotNull Vertx vertx,
        @NotNull Provider<Allocate> actionProvider,
        @NotNull Provider<String> stringProvider,
        @NotNull Provider<String> stringProvider2) {
      super(vertx, actionProvider, stringProvider, stringProvider2);
    }
  }

  @Singleton
  static class Allocate2_Provider extends InternalActionProvider<Allocate2, String, String> {
    @Inject
    public Allocate2_Provider(@NotNull Vertx vertx,
        @NotNull Provider<Allocate2> actionProvider,
        @NotNull Provider<String> stringProvider,
        @NotNull Provider<String> stringProvider2) {
      super(vertx, actionProvider, stringProvider, stringProvider2);
    }
  }

  @Singleton
  static class AllocateInventory_Provider extends InternalActionProvider<AllocateInventory, AllocateInventory.Request, AllocateInventory.Reply> {
    @Inject
    public AllocateInventory_Provider(@NotNull Vertx vertx,
        @NotNull Provider<AllocateInventory> actionProvider,
        @NotNull Provider<AllocateInventory.Request> stringProvider,
        @NotNull Provider<AllocateInventory.Reply> stringProvider2) {
      super(vertx, actionProvider, stringProvider, stringProvider2);
    }
  }
}
