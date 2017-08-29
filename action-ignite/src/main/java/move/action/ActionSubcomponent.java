package move.action;

import dagger.Module;
import dagger.Provides;
import dagger.Subcomponent;
import move.action.ActionSubcomponent.M;

/**
 *
 */
@Subcomponent(modules = {M.class})
interface ActionSubcomponent {

  @Subcomponent.Builder
  interface Builder {
    Builder mModule(M module);

    ActionSubcomponent build();
  }

  @Module
  class M {

    @Provides
    Net.Message2 message2() {
      return new Net.Message2();
    }

    @Provides
    Net.Message message() {
      return new Net.Message("", "");
    }
  }
}
