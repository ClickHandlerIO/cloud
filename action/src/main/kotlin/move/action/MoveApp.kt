package move.action

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueServerSocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.vertx.core.VertxOptions
import io.vertx.core.cli.Argument
import io.vertx.core.cli.Option
import io.vertx.core.metrics.MetricsOptions
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.core.cli.CLI
import io.vertx.rxjava.core.cli.CommandLine
import kotlinx.coroutines.experimental.runBlocking
import move.NUID
import move.Wire
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files

interface MoveComponent {
   fun actions(): ActionManager
}

internal var _MOVE: MoveApp<MoveComponent>? = null

val NATIVE_TRANSPORT
   get() = _MOVE?.nativeTransport ?: true

fun setupNettyTransport(bootstrap: ServerBootstrap) {
   if (NATIVE_TRANSPORT && Epoll.isAvailable()) {
      bootstrap.channel(EpollServerSocketChannel::class.java)
   } else if (NATIVE_TRANSPORT && KQueue.isAvailable()) {
      bootstrap.channel(KQueueServerSocketChannel::class.java)
   } else {
      bootstrap.channel(NioServerSocketChannel::class.java)
   }
}

val MOVE
   get () = _MOVE!!

val VERTX
   get() = locateVertx()

val ROLE
   get() = MOVE.role

val ROLE_WORKER
   get() = when (MOVE.role) {
      NodeRole.WORKER, NodeRole.ALL -> true
      else -> false
   }

val ROLE_REMOTE
   get() = when (MOVE.role) {
      NodeRole.REMOTE, NodeRole.ALL -> true
      else -> false
   }

val MODE
   get() = MOVE.mode

val NODE_ID
   get() = MOVE.nodeId

fun locateVertx(): Vertx {
//   if (_MOVE!!.lo)
   return _MOVE!!.vertx
}

/**
 *
 */
abstract class MoveApp<G : MoveComponent> {
   val log by lazy { LoggerFactory.getLogger("app") }

   val OPTION_HELP = Option()
      .setLongName("help")
      .setShortName("h")
      .setFlag(true)
      .setHelp(true)

   val OPTION_MODE = Option()
      .setLongName("mode")
      .setShortName("m")
      .setDescription("Mode to run in")
      .setChoices(setOf("dev", "test", "prod"))
      .setDefaultValue("DEV")
      .setRequired(false)

   val OPTION_WORKER = Option()
      .setLongName("worker")
      .setShortName("w")
      .setDescription("Enables 'WORKER' role")
      .setFlag(true)

   val OPTION_REMOTE = Option()
      .setLongName("remote")
      .setShortName("r")
      .setDescription("Enables 'REMOTE' role")
      .setFlag(true)

   open val OPTION_CONFIG = Option()
      .setLongName("config")
      .setShortName("c")
      .setDescription("Config file path")
      .setRequired(false)

   open val OPTION_NODE_ID = Option()
      .setLongName("id")
      .setShortName("i")
      .setDescription("Node's Unique ID")
      .setRequired(false)
      .setDefaultValue(NUID.nextGlobal())

   open val OPTION_NATIVE_TRANSPORT = Option()
      .setLongName("native")
      .setShortName("n")
      .setDescription("Use Native transport 'epoll' or 'kqueue' if available")
      .setRequired(false)
      .setFlag(true)

   open val OPTION_LIST_WORKERS = Option()
      .setLongName("list")
      .setShortName("l")
      .setDescription("List Worker Actions")
      .setRequired(false)
      .setChoices(setOf("public", "internal", "private", "all"))
      .setDefaultValue("public")

   lateinit var vertxOptions: VertxOptions

   var nodeId: String = NUID.nextGlobal()
      get
      private set

   var mode = Mode.DEV
   var worker: Boolean = true
      get
      private set
   var remote: Boolean = true
      get
      private set

   val role
      get() = if (worker && remote)
         NodeRole.ALL
      else if (worker)
         NodeRole.WORKER
      else
         NodeRole.REMOTE

   var nativeTransport: Boolean = true
      get
      private set


   lateinit var cli: CLI
      get
      private set
   lateinit var line: CommandLine
      get
      private set
   lateinit var vertx: Vertx
      get
      private set
   lateinit var component: G
      get
      private set

   fun start(args: Array<String>) {
      @Suppress("UNCHECKED_CAST")
      _MOVE = this as MoveApp<MoveComponent>

      runBlocking {
         val args = step1_ReceiveArgs(args)

         step2_PrepareArgs()

         // Create CommandLine
         cli = step3_BuildCLI()

         // Parse CLI.
         line = cli.parse(args)

         // Exit if CLI is invalid
         if (!line.isValid || line.isAskingForHelp) {
            onInvalidCLI(line)

            val builder = StringBuilder()
            cli.delegate.usage(builder)
            println(builder.toString())

            System.exit(-1)
            return@runBlocking
         }

         // After CLI
         step4_AfterCLI(line)

         // Build Vertx.
         vertx = step5_CreateVertx()

         // Init MoveKernel
         MoveKernel.init()

         // Invoke function before the object graph is built.
         step6_BeforeBuildComponent()

         // Build Dagger component.
         component = step7_BuildComponent()

         // Init Actions.
         component.actions()

         // Process the internal Command Line Options.
         processInternalOptions(line)

         if (line.isOptionAssigned(OPTION_LIST_WORKERS)) {
            component.actions().actions.producers
               .forEach { log.info(it.provider.actionClass.canonicalName) }

            System.exit(0)
            return@runBlocking
         }

         // Start Daemons.
         step8_StartDeamons()

         // onStarted()
         onStarted()

         // Add shutdown hook.
         Runtime.getRuntime().addShutdownHook(Thread {
            runBlocking {
               shutdown()
            }
         })
      }
   }

   open val name = "move"
   open val summary = "Move Cloud Microservice"
   open val description = ""

   suspend fun loggingProvider(): String = "slf4j"

   open val IPv6 = false
   open val IPv4 = true

   /**
    * Filter raw arguments before CLI.
    */
   suspend open fun step1_ReceiveArgs(args: Array<String>) = args.toList()

   suspend open fun step2_PrepareArgs() {
      // Setup JBoss logging provider.
      System.setProperty("org.jboss.logging.provider", "slf4j")
      // Setup IP stack. We want IPv4.
      System.setProperty("java.net.preferIPv6Addresses", "false")
      System.setProperty("java.net.preferIPv4Stack", "true")
   }

   /**
    *
    */
   suspend open fun onInvalidCLI(cli: CommandLine) {
   }

   /**
    * Build the actual CommandLine object.
    */
   suspend open fun step3_BuildCLI(): CLI {
      return CLI.create(name)
         .setSummary(summary)
         .addOptions(options(mutableListOf()))
         .addArguments(arguments(mutableListOf()))
   }

   /**
    *
    */
   suspend open fun step4_AfterCLI(cli: CommandLine) {

   }

   fun <T> parseConfig(cli: CommandLine, configClass: Class<T>, defaultValue: () -> T): T {
      if (cli.isOptionAssigned(OPTION_CONFIG)) {
         val configFile = File(cli.getRawValueForOption(OPTION_CONFIG))
         val configFileContents = Files.readAllBytes(configFile.toPath())

         try {
            return Wire.parseYAML(
               configClass,
               configFileContents
            )
         } catch (e: Throwable) {
            return Wire.parse(
               configClass,
               configFileContents
            )
         }
      } else {
         return defaultValue()
      }
   }

   /**
    * Build up CommandLine Options.
    */
   suspend open fun options(options: MutableList<Option>): List<Option> {
      options.add(OPTION_HELP)
      options.add(OPTION_NODE_ID)
      options.add(OPTION_WORKER)
      options.add(OPTION_REMOTE)
      options.add(OPTION_NATIVE_TRANSPORT)
      options.add(OPTION_CONFIG)
      options.add(OPTION_LIST_WORKERS)
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

      if (cli.isOptionAssigned(OPTION_NODE_ID)) {
         nodeId = cli.getRawValueForOption(OPTION_NODE_ID)
      }
   }

   suspend open fun configureVertx(): VertxOptions {
      val options = VertxOptions()

      if (mode == Mode.DEV || mode == Mode.TEST) {
         if (mode == Mode.TEST) {
            options.eventLoopPoolSize = 1
            options.workerPoolSize = 1
         } else {
            options.eventLoopPoolSize =
               if (Runtime.getRuntime().availableProcessors() > 1)
                  2
               else
                  1

            options.workerPoolSize = options.eventLoopPoolSize
         }

         options.internalBlockingPoolSize = 1
         options.maxEventLoopExecuteTime = Long.MAX_VALUE
         options.maxWorkerExecuteTime = Long.MAX_VALUE
         options.metricsOptions = MetricsOptions().setEnabled(mode == Mode.DEV)
      } else {
         options.workerPoolSize = options.eventLoopPoolSize * 50
         options.internalBlockingPoolSize = options.eventLoopPoolSize * 50
         options.metricsOptions = MetricsOptions().setEnabled(true)
      }

      this.vertxOptions = options

      return options
   }

   /**
    * Construct Vertx.
    */
   suspend open fun step5_CreateVertx() = Vertx.vertx(configureVertx())

   /**
    * Intercept before "build()"
    */
   suspend open fun step6_BeforeBuildComponent() {}

   /**
    * Construct Dagger Object graph.
    */
   suspend abstract fun step7_BuildComponent(): G

   /**
    * Start Daemons.
    */
   suspend open fun step8_StartDeamons() {
      Actions.daemons.forEach {
         // Each Daemon is started and must receive that
         // it started successfully before the next one
         // can be started.
         // Daemons use the Actor model and can be communicated
         // with by passing messages.
      }
   }

   suspend open fun onStarted() {

   }

   suspend open fun shutdown() {
      stopDaemons()
   }

   suspend open fun stopDaemons() {

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
