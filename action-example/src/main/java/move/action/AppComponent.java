package move.action;

import dagger.Component;
import io.vertx.rxjava.core.Vertx;
import javax.inject.Singleton;
import move.Action_LocatorRoot;
import move.model.DB;

@Singleton
@Component(modules = {ActionModule.class, Actions.class, M.class})
interface AppComponent {

  AppComponent instance = DaggerAppComponent.create();

  Vertx vertx();

  ActionManager actionManager();

  DB db();

  Action_LocatorRoot actions();
}
