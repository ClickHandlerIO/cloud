package move.action;

import dagger.Component;
import javax.inject.Singleton;
import move.Move_Root_Module;

/**
 *
 */
@Singleton
@Component(modules = {
    Move_Root_Module.class,
    AppModule.class
})
public interface AppComponent {
  AppComponent instance = DaggerAppComponent.create();

  ActionManager actions();
}
