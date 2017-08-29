package move.action;

import dagger.Subcomponent;
import java.util.Map;
import move.Action_LocatorRoot;

/**
 *
 */
@Subcomponent(modules = {_ActionMap.class})
public interface _ActionComponent_1 extends ActionComponent {

  Map<Class<?>, ActionProvider<?, ?, ?>> byClass();

  Action_LocatorRoot actions();

  @Subcomponent.Builder
  interface Builder {

//    Builder generatedActionModule(_ActionMap module);

    _ActionComponent_1 build();
  }
}
