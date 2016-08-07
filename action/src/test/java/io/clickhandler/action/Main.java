package io.clickhandler.action;

import com.hazelcast.core.HazelcastInstance;
import dagger.Component;
import dagger.Module;
import dagger.Provides;
import io.Action_LocatorRoot;
import io.clickhandler.cloud.cluster.HazelcastProvider;
import io.vertx.rxjava.core.Vertx;

import javax.annotation.Nullable;
import javax.inject.Singleton;

/**
 *
 */
public class Main {
    static final Vertx vertx = Vertx.vertx();
    public static void main(String[] args) throws Throwable {

//        Observable<String> observable = actions().myAsyncAction().execute("HI");
//        observable.subscribe(result -> {
//            System.err.println("Another Subscriber: " + result);
//        });

        actions().register();

        WireUp.instance.actionManager().startAsync().awaitRunning();

        Thread.sleep(5000000);

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

        Action_LocatorRoot actions();

        ActionManager actionManager();
    }

    @Module
    public static class M {
        @Provides
        Boolean bool() {
            return Boolean.FALSE;
        }

        @Provides
        String string() {
            return "";
        }

        @Provides
        @Nullable
        Void provideVoid() {
            return null;
        }

        @Provides
        @Singleton
        Vertx vertx() {
            return vertx;
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

//        @Provides
//        @Singleton
//        WorkerService workerService(SQSService sqsService) {
//            sqsService.setSqsClient(new AmazonSQSClient());
//            return sqsService;
//        }

        @Provides
        @Singleton
        WorkerService workerService(LocalWorkerService workerService) {
            return workerService;
        }
    }
}
