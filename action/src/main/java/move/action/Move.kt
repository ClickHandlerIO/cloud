package move.action

import io.vertx.core.VertxOptions
import io.vertx.core.cli.Argument
import io.vertx.core.cli.Option
import io.vertx.core.metrics.MetricsOptions
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.core.cli.CLI
import io.vertx.rxjava.core.cli.CommandLine
import kotlinx.coroutines.experimental.runBlocking
import java.util.concurrent.locks.ReentrantLock

interface MoveComponent {
   fun actions(): ActionManager
}

internal var _MOVE: Move<MoveComponent>? = null

val MOVE
   get () = _MOVE!!

internal fun locateVertx(): Vertx {
//   if (_MOVE!!.lo)
   return _MOVE!!.vertx
}

/**
 *
 */
abstract class Move<G : MoveComponent> {
   val OPTION_HELP = Option()
      .setLongName("help")
      .setShortName("h")
      .setFlag(true)
      .setHelp(true)

   val OPTION_MODE = Option()
      .setLongName("mode")
      .setShortName("M")
      .setDescription("Mode to run in")
      .setChoices(setOf("dev", "test", "prod"))
//      .setDefaultValue("DEV")
//      .setRequired(false)

   val OPTION_WORKER = Option()
      .setLongName("worker")
      .setShortName("W")
      .setDescription("Enables 'WORKER' role")
      .setFlag(true)

   val OPTION_REMOTE = Option()
      .setLongName("remote")
      .setShortName("R")
      .setDescription("Enables 'REMOTE' role")
      .setFlag(true)

   open val OPTION_CONFIG = Option()
      .setLongName("config")
      .setShortName("C")
      .setDescription("Config file path")
      .setRequired(false)

   var mode = Mode.DEV
   var worker: Boolean = true
      get
      private set
   var remote: Boolean = true
      get
      private set

   lateinit var cli: CLI
   lateinit var line: CommandLine
   lateinit var vertx: Vertx
   lateinit var eventLoopGroup: MoveEventLoopGroup
   lateinit var component: G

   internal val lock = ReentrantLock()

   fun start(args: Array<String>) {
      @Suppress("UNCHECKED_CAST")
      _MOVE = this as Move<MoveComponent>

      runBlocking {
         // Create CommandLine
         cli = buildCLI()

         line = cli.parse(first(args))

         // Exit if CLI is invalid
         if (!line.isValid || line.isAskingForHelp) {
            onInvalidCLI(line)

            val builder = StringBuilder()
            cli.delegate.usage(builder)
            println(builder.toString())

            System.exit(-1)
         }

         // Process the internal Command Line Options.
         processInternalOptions(line)

         // After CLI
         onCLI(line)

         // Build Vertx.
         vertx = vertx()

         // Init EventLoop Group.
         eventLoopGroup = MoveEventLoopGroup.Companion.get(vertx)

         // Invoke function before the object graph is built.
         beforeBuild()

         // Build Dagger component.
         component = build()

         // Init Actions.
         actions()

         // Start Daemons.
         startDaemons()
      }
   }

   open val name = "move"
   open val summary = "Move Cloud Service"
   open val description = "Move Cloud Service"

   suspend fun loggingProvider(): String = "slf4j"

   open val IPv6 = false
   open val IPv4 = true

   /**
    * Filter raw arguments before CLI.
    */
   suspend open fun first(args: Array<String>) = args.toList()

   /**
    *
    */
   suspend open fun onInvalidCLI(cli: CommandLine) {
   }

   /**
    *
    */
   suspend open fun onCLI(cli: CommandLine) {

   }

   /**
    * Build the actual CommandLine object.
    */
   suspend open fun buildCLI(): CLI {
      return CLI.create(name)
         .setSummary(summary)
         .addOptions(options(mutableListOf()))
         .addArguments(arguments(mutableListOf()))
   }

   /**
    * Build up CommandLine Options.
    */
   suspend open fun options(options: MutableList<Option>): List<Option> {
      options.add(OPTION_HELP)
      options.add(OPTION_WORKER)
      options.add(OPTION_REMOTE)
      options.add(OPTION_CONFIG)
      options.add(OPTION_REMOTE)
      return options
   }

   /**
    * Build up CommandLine Arguments.
    */
   suspend open fun arguments(arguments: MutableList<Argument>): List<Argument> {
      return arguments
   }

   /**
    * Parse internal Options from the CommandLine
    */
   private fun processInternalOptions(cli: CommandLine) {
      // Set mode.
      mode = if (cli.isOptionAssigned(OPTION_MODE))
         Mode.from(cli.getRawValueForOption(OPTION_MODE))
      else
         Mode.DEV

      worker = if (cli.isOptionAssigned(OPTION_WORKER) && !cli.isOptionAssigned(OPTION_REMOTE))
         true
      else
         !cli.isOptionAssigned(OPTION_REMOTE)

      remote = if (cli.isOptionAssigned(OPTION_REMOTE) && !cli.isOptionAssigned(OPTION_WORKER))
         true
      else
         !cli.isOptionAssigned(OPTION_WORKER)
   }

   /**
    * Construct Vertx.
    */
   suspend open fun vertx() = Vertx.vertx(vertxOptions())

   suspend open fun vertxOptions(): VertxOptions {
      val options = VertxOptions()

      if (mode == Mode.DEV || mode == Mode.TEST) {
         if (mode == Mode.TEST) {
            options.eventLoopPoolSize = 1
            options.workerPoolSize = 1
         } else
            options.eventLoopPoolSize =
               if (Runtime.getRuntime().availableProcessors() > 1)
                  2
               else
                  1

         options.internalBlockingPoolSize = 1
         options.maxEventLoopExecuteTime = Long.MAX_VALUE
         options.maxWorkerExecuteTime = Long.MAX_VALUE
         options.metricsOptions = MetricsOptions().setEnabled(mode == Mode.DEV)
      } else {
         options.workerPoolSize = options.eventLoopPoolSize * 50
         options.internalBlockingPoolSize = options.eventLoopPoolSize * 50
         options.metricsOptions = MetricsOptions().setEnabled(true)
      }

      return options
   }

   /**
    * Get constructed ActionManager from Dagger Component.
    */
   suspend open fun actions(): ActionManager {
      return component.actions()
   }

   /**
    * Intercept before "build()"
    */
   suspend open fun beforeBuild() {}

   /**
    * Construct Dagger Object graph.
    */
   suspend abstract fun build(): G

   /**
    * Start Daemons.
    */
   suspend open fun startDaemons() {
      Actions.daemons.forEach {
         // Each Daemon is started and must receive that
         // it started successfully before the next one
         // can be started.
         // Daemons use the Actor model and can be communicated
         // with by passing messages.
      }
   }
}

enum class Mode {
   DEV,
   TEST,
   PROD, ;

   companion object {
      fun from(value: String?) =
         when (value?.let { it.trim().toLowerCase() } ?: "d") {
            "d", "dev" -> DEV
            "t", "test" -> TEST
            "p", "prod" -> PROD
            else -> DEV
         }
   }
}

enum class NodeRole(val worker: Boolean, val remote: Boolean) {
   WORKER(true, false),
   REMOTE(false, true),
   ALL(true, true), ;
}
