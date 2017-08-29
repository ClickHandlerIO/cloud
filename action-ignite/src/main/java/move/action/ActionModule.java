package move.action;

import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import move.action.ActionModule.PkgModule;

@Module(includes = {PkgModule.class})
public class ActionModule {

  @Provides
  @Singleton
  String string() {
    return "";
  }

  @Module
  public static class PkgModule {

    @Provides
    Net.Message2 message2() {
      return new Net.Message2();
    }
  }
}