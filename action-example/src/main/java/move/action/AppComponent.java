package move.action;

import dagger.Component;
import javax.inject.Singleton;

/**
 *
 */
@Singleton
@Component(modules = {
    move.action.MoveModule.class,
    move.Move_GeneratedModule.class
})
public interface AppComponent extends MoveComponent {

  Action_Locator locator();
}
