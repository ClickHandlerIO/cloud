package io.clickhandler.action;

import com.hazelcast.core.HazelcastInstance;
import dagger.Component;
import dagger.Module;
import dagger.Provides;
import io.Action_LocatorRoot;
import io.Io_Locator;
import io.clickhandler.action.store.MyActorAction;
import io.vertx.rxjava.core.Vertx;

import javax.inject.Singleton;

/**
 *
 */
public class Main {
    public static void main(String[] args) {

//        Observable<String> observable = actions().myAsyncAction().observe("HI");
//        observable.subscribe(result -> {
//            System.err.println("Another Subscriber: " + result);
//        });

        actions().register();

        for (int p = 0; p < 4; p++)
        for (int i = 0; i < 2; i++) {
            final String key = "KEY_" + i;
            actions().store().myActorAction().ask(key, new MyActorAction.Request()).subscribe(
                r -> {
                    synchronized (Main.class) {
                        System.out.println(key + " on " + r.threadName());
                        r.watchers().forEach(watcher -> System.out.println(watcher));

                        System.out.println();
                        System.out.println();
                        System.out.println();
                    }
                },
                e -> {
                }
            );
        }

//        actions().myAsyncAction()
//            .observe("Bye")
//            .subscribe(System.err::println);
    }

    public static Action_Locator actions() {
        return WireUp.instance.actions().io().clickhandler().action();
    }

    @Singleton
    @Component(modules = M.class)
    public interface WireUp {
        WireUp instance = DaggerMain_WireUp.create();

        Io_Locator locator();

        Action_LocatorRoot actions();
    }

    @Module
    public static class M {
        @Provides
        String string() {
            return "";
        }

        @Provides
        @Singleton
        Vertx vertx() {
            return Vertx.vertx();
        }

        @Provides
        @Singleton
        HazelcastProvider hazelcastProvider() {
            return new HazelcastProvider(null);
        }

        @Provides
        @Singleton
        HazelcastInstance hazelcast() {
            return hazelcastProvider().get();
        }
    }
}
