package move.action;

import dagger.Component;
import javax.inject.Singleton;

/**
 *
 */
@Singleton
@Component(modules = {
    MoveFirstModule.class,
    move.MoveSecondModule.class
})
public interface AppComponent extends MoveComponent {

  Action_Locator locator();
}
