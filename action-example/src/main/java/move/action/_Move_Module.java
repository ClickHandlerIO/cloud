package move.action;

import dagger.Module;
import dagger.Provides;
import io.vertx.rxjava.core.Vertx;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
@Module()
public class _Move_Module {

  @Provides
  Allocate allocate() {
    return new Allocate();
  }

  @Provides
  Allocate2 allocate2() {
    return new Allocate2();
  }

  @Provides
  AllocateStock allocateStock() {
    return new AllocateStock();
  }

  @Singleton
  static class Allocate_Provider extends InternalActionProvider<Allocate, String, String> {

    @Inject
    Allocate_Provider(@NotNull Vertx vertx,
        @NotNull Provider<Allocate> actionProvider) {
      super(vertx, actionProvider);
    }
  }

  @Singleton
  static class Allocate2_Provider extends InternalActionProvider<Allocate2, String, String> {

    @Inject
    Allocate2_Provider(@NotNull Vertx vertx,
        @NotNull Provider<Allocate2> actionProvider) {
      super(vertx, actionProvider);
    }
  }

  @Singleton
  static class AllocateStock_Provider extends
      InternalActionProvider<AllocateStock, AllocateStock.Request, AllocateStock.Reply> {

    @Inject
    AllocateStock_Provider(@NotNull Vertx vertx,
        @NotNull Provider<AllocateStock> actionProvider) {
      super(vertx, actionProvider);
    }
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
