package move;

import java.lang.Override;
import javax.inject.Inject;
import javax.inject.Singleton;
import move.action.ActionLocator;
import move.action.Action_Locator;

@Singleton
public final class Move_Locator extends ActionLocator {
  public final Action_Locator action;

  @Inject
  Move_Locator(final Action_Locator action) {
    this.action = action;
  }

  public Action_Locator action() {
    return action;
  }

  @Override
  protected void initActions() {
  }

  @Override
  protected void initChildren() {
    children.add(action);
  }
}
