package move.action;

import dagger.Subcomponent;
import java.util.Map;
import move.Action_LocatorRoot;

/**
 *
 */
@Subcomponent(modules = {_ActionMap.class})
public interface _Move_Component extends ActionComponent {

  Map<Class<?>, ActionProvider<?, ?, ?>> byClass();

  Action_LocatorRoot locator();

  @Subcomponent.Builder
  interface Builder {

    _Move_Component build();
  }
}
