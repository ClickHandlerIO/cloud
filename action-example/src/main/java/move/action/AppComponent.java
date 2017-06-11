package move.action;

import dagger.Component;
import io.vertx.rxjava.core.Vertx;
import move.Action_LocatorRoot;
import move.action.model.DB;

import javax.inject.Singleton;

@Singleton
@Component(modules = {Actions.class, M.class})
interface AppComponent {
    AppComponent instance = DaggerAppComponent.create();

    Vertx vertx();

    ActionManager actionManager();

    DB db();

    Action_LocatorRoot actions();
}
