package move.action;

import com.hazelcast.core.HazelcastInstance;
import dagger.Component;
import dagger.Module;
import dagger.Provides;
import io.vertx.rxjava.core.Vertx;
import move.Action_LocatorRoot;
import move.cluster.HazelcastProvider;

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

        WireUp.instance.actions().move().action().allocateInventory2.observe("Hi").subscribe(r -> System.out.println(Thread.currentThread().getName()), e -> e.printStackTrace());


        Thread.sleep(5000000);

//        actions().myAsyncAction()
//            .execute("Bye")
//            .subscribe(System.err::println);
    }

    public static Action_Locator actions() {
        return WireUp.instance.actions().move().action();
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
        @Nullable
        Object object() {
            return new Object();
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
