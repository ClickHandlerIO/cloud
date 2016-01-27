package io.clickhandler.action;

import dagger.Component;
import dagger.Module;
import dagger.Provides;
import io.Action_LocatorRoot;
import io.Io_Locator;
import rx.Observable;

import javax.inject.Singleton;

/**
 *
 */
public class Main {
    public static void main(String[] args) {
        Observable<String> observable = actions().myAsyncAction().observe("HI");
        observable.subscribe(result -> {
            System.err.println("Another Subscriber: " + result);
        });

        actions().myAsyncAction()
            .observe("Bye")
            .subscribe(System.err::println);
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
    }
}
