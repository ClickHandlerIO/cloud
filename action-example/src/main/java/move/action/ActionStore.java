package move.action;

import java.util.Map;
import javax.inject.Inject;

/**
 *
 */
public class ActionStore {
  public final Map<Class<?>, ActionProvider<?, ?, ?>> actions;

  @Inject
  public ActionStore(Map<Class<?>, ActionProvider<?, ?, ?>> actions) {
    this.actions = actions;
    ActionManager.Companion.put(actions);
  }
}
