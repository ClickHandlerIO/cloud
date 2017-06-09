package move.action;

import com.hazelcast.core.HazelcastInstance;
import dagger.Component;
import dagger.Module;
import dagger.Provides;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import move.Action_LocatorRoot;
import move.cluster.HazelcastProvider;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;

import javax.annotation.Nullable;
import javax.inject.Singleton;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.PageRanges;
import java.awt.print.PrinterJob;
import java.io.File;
import java.util.Arrays;

/**
 *
 */
public class Main {
    static final Vertx vertx = Vertx.vertx();

    public static void main(String[] args) throws Throwable {
//        PDDocument pdf = PDDocument.load(new File("/Users/clay/Desktop/StatementPdf.pdf"), MemoryUsageSetting.setupMainMemoryOnly());
        PDDocument pdf = PDDocument.load(new File("/Users/clay/Desktop/invoicesample.pdf"), MemoryUsageSetting.setupMainMemoryOnly());

        System.out.println("Page Count: " + pdf.getNumberOfPages());

        Arrays.stream(PrinterJob.lookupPrintServices()).forEach($ -> System.out.println($));


        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPageable(new PDFPageable(pdf));
            PrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();
            attr.add(new PageRanges(1, 1)); // pages 1 to 1
            job.print(attr);
        }
        catch (Exception e) {
            System.out.println(e);
        }

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

        KAllocateInventoryBlockingProvider action1();

//        Set<? extends ActionProvider> actionProviders();
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

//        @Provides
//        @Singleton
//        @ElementsIntoSet
//        static InternalActionProvider<KAllocateInventory, KAllocateInventory.Request, KAllocateInventory.Reply> provider1(InternalActionProvider<KAllocateInventory, KAllocateInventory.Request, KAllocateInventory.Reply> provider) {
//            return provider;
//        }
    }
}
