package move.action;

import dagger.Module;
import dagger.Provides;
import dagger.internal.Factory;
import javax.inject.Provider;

/**
 *
 */
@Module
public class _ActionModelModule_1 {
  @Provides
  Allocate _0() {
    return new Allocate();
  }

//  @Provides
//  AllocateInventory _1(Provider<AllocateInventory> provider) {
//    return provider.get();
//  }

  @Provides
  AllocateInventory _1() {
    return new AllocateInventory();
  }

  @Provides
  AllocateInventory.Request _2() {
    return new AllocateInventory.Request();
  }

  @Provides
  Allocate2 _10() {
    return new Allocate2();
  }
}
