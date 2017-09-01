package move.action

import dagger.Module
import dagger.Provides
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.core.cli.CLI
import javax.inject.Singleton

object App {
   val vertx: Vertx by lazy { Vertx.vertx() }
   val graph = DaggerAppComponent.create()

   init {
      graph.actions()
   }

   @JvmStatic
   fun main(args: Array<String>) {
      KotlinAction.main(args)
   }
}

@Module
class AppModule {
   @Provides
   fun string(): String {
      return ""
   }

   @Provides
   @Singleton
   fun vertx(): Vertx {
      return App.vertx
   }
}
