package move.action;

/**
 *
 */

import dagger.Component;
import javax.inject.Singleton;
import move.action.Net.MyService;

@Singleton
@Component(modules = {ActionModule.class})
public interface ApplicationComponent {
  MyService myService();
}
