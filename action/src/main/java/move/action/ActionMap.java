package move.action;

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 *
 */
@Singleton
public class ActionMap {

  public final Map<Class<?>, ActionProvider<?, ?, ?>> map;

  @Inject
  public ActionMap(Map<Class<?>, ActionProvider<?, ?, ?>> map) {
    this.map = map;
  }
}
