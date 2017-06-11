package move;

import java.lang.Override;
import javax.inject.Inject;
import javax.inject.Singleton;
import move.action.ActionLocator;

@Singleton
public final class Action_LocatorRoot extends ActionLocator {
  public final Move_Locator move;

  @Inject
  Action_LocatorRoot(final Move_Locator move) {
    this.move = move;
  }

  public Move_Locator move() {
    return move;
  }

  @Override
  protected void initActions() {
  }

  @Override
  protected void initChildren() {
    children.add(move);
  }
}
