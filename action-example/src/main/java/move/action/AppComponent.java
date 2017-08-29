package move.action;

import dagger.Component;
import io.vertx.rxjava.core.Vertx;
import javax.inject.Singleton;

@Singleton
@Component(modules = {
    ActionsModule.class,

    AppModule.class
})
interface AppComponent {

  AppComponent instance = DaggerAppComponent.create();

  Vertx vertx();

  ActionStore actions();
}
