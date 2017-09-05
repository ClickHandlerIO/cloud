package move.action;

import javax.inject.Inject;
import move.action.AllocateInventory.Reply;
import move.action.AllocateInventory.Request;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class JavaFactory extends InternalActionFactory<MyAction, String, String> {

  @Inject
  MyAction_Provider provider;

  public JavaFactory() {
  }

  @NotNull
  @Override
  public InternalActionProvider<MyAction, String, String> getProvider() {
    MyAction.Companion.rx("");

    return provider;
  }

  public static class AI extends InternalActionFactory<MyAction, String, String> {

    @NotNull
    @Override
    public InternalActionProvider<MyAction, String, String> getProvider() {
      return null;
    }
  }

  public static class F2 extends
      InternalActionFactoryWithStringParam<AllocateInventory, Request, Reply> {

    @Inject
    AllocateInventory_Provider provider;

    @NotNull
    @Override
    public Request createWithParam(String param) {
      return new Request(param);
    }

    @NotNull
    @Override
    public InternalActionProvider<AllocateInventory, Request, Reply> getProvider() {
      return provider;
    }
  }
}
