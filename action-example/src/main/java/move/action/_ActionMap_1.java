package move.action;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ClassKey;
import dagger.multibindings.IntoMap;
import move.action._Move_Module.Allocate2_Provider;
import move.action._Move_Module.AllocateInventory_Provider;
import move.action._Move_Module.AllocateStock_Provider;
import move.action._Move_Module.Allocate_Provider;

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
  @ClassKey(AllocateStock.class)
  ActionProvider<?, ?, ?> provideAllocateStock(
      AllocateStock_Provider provider) {
    return provider;
  }

  @Provides
  @IntoMap
  @ClassKey(AllocateInventory.class)
  ActionProvider<?, ?, ?> provideAllocateInventory(
      AllocateInventory_Provider provider) {
    return provider;
  }
}
