package move.action

import com.hazelcast.core.HazelcastInstance
import dagger.Module
import dagger.Provides
import io.vertx.core.http.HttpServerOptions
import io.vertx.rxjava.core.Vertx
import move.cluster.HazelcastProvider
import move.http.WebSocketMessageTooBigException
import javax.inject.Singleton

/**

 */
object Main {
    internal val vertx = Vertx.vertx()

    @JvmStatic fun main(args: Array<String>) {
        //        PDDocument pdf = PDDocument.load(new File("/Users/clay/Desktop/StatementPdf.pdf"), MemoryUsageSetting.setupMainMemoryOnly());
//        val pdf = PDDocument.load(File("/Users/clay/Desktop/invoicesample.pdf"), MemoryUsageSetting.setupMainMemoryOnly())
//
//        println("Page Count: " + pdf.numberOfPages)
//
//        Arrays.stream<PrintService>(PrinterJob.lookupPrintServices()).forEach { `$` -> println(`$`) }
//
//
//
//        try {
//            val job = PrinterJob.getPrinterJob()
//            job.setPageable(PDFPageable(pdf))
//            val attr = HashPrintRequestAttributeSet()
//            attr.add(PageRanges(1, 1)) // pages 1 to 1
//            job.print(attr)
//        } catch (e: Exception) {
//            println(e)
//        }

        //        Observable<String> observable = actions().myAsyncAction().execute("HI");
        //        observable.subscribe(result -> {
        //            System.err.println("Another Subscriber: " + result);
        //        });

//        actions().register()

        AppComponent.instance.actionManager().startAsync().awaitRunning()

        AppComponent.instance.vertx().createHttpServer(HttpServerOptions().setCompressionSupported(true)
                .setMaxWebsocketMessageSize(1024 * 1024 * 2)
//                .setMaxWebsocketMessageSize(1024 * 64)
        )
                .websocketHandler { ws ->
                    ws.textMessageHandler {
                        ws.writeFinalTextFrame(it)
                    }
                    ws.exceptionHandler {
                        it.printStackTrace()
                        ws.writeFinalTextFrame("MESSAGE SIZE IS TOO BIG: " + (it as WebSocketMessageTooBigException).size)
//                        it.printStackTrace()
                    }
                }.listen(9001)

//        AppComponent.instance.actions().move().action().allocateInventory2.observe("Hi").subscribe({ r -> println(Thread.currentThread().name) }) { e -> e.printStackTrace() }


        Thread.sleep(5000000)


        //        actions().myAsyncAction()
        //            .execute("Bye")
        //            .subscribe(System.err::println);
    }

//    fun actions(): Action_Locator {
//        return AppComponent.instance.actions().move().action()
//    }


}





@Module
class Actions {
}

@Module
class M {
    @Provides
    internal fun bool(): Boolean? {
        return java.lang.Boolean.FALSE
    }

    @Provides
    internal fun string(): String {
        return ""
    }

    @Provides
    internal fun provideVoid(): Void? {
        return null
    }

    @Provides
    internal fun any(): Any? {
        return Any()
    }

    @Provides
    @Singleton
    internal fun vertx(): Vertx {
        return Main.vertx
    }

    @Provides
    @Singleton
    internal fun hazelcastProvider(): HazelcastProvider {
        return HazelcastProvider(null)
    }

    @Provides
    @Singleton
    internal fun hazelcast(): HazelcastInstance {
        return hazelcastProvider().get()
    }

    //        @Provides
    //        @Singleton
    //        WorkerService workerService(SQSService sqsService) {
    //            sqsService.setSqsClient(new AmazonSQSClient());
    //            return sqsService;
    //        }

    @Provides
    @Singleton
    internal fun workerService(workerService: LocalWorkerService): WorkerService {
        return workerService
    }

    //        @Provides
    //        @Singleton
    //        @ElementsIntoSet
    //        static InternalActionProvider<AllocateInventory, AllocateInventory.Request, AllocateInventory.Reply> provider1(InternalActionProvider<AllocateInventory, AllocateInventory.Request, AllocateInventory.Reply> provider) {
    //            return provider;
    //        }
}
