package io.clickhandler.action;

import com.hazelcast.core.HazelcastInstance;
import dagger.Component;
import dagger.Module;
import dagger.Provides;
import io.Action_LocatorRoot;
import io.Io_Locator;
import io.clickhandler.cloud.cluster.HazelcastProvider;
import io.vertx.rxjava.core.Vertx;

import javax.inject.Singleton;

/**
 *
 */
public class Main {
    public static void main(String[] args) {

//        Observable<String> observable = actions().myAsyncAction().execute("HI");
//        observable.subscribe(result -> {
//            System.err.println("Another Subscriber: " + result);
//        });

        actions().register();

//        actions().myAsyncAction()
//            .execute("Bye")
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
