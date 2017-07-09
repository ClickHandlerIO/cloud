package move.action

import com.google.common.base.Throwables
import com.netflix.hystrix.HystrixObservableCommand
import com.netflix.hystrix.exception.HystrixRuntimeException
import com.netflix.hystrix.exception.HystrixTimeoutException
import com.netflix.hystrix.isTimedOut
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.core.WorkerExecutor
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.rx1.await
import kotlinx.coroutines.experimental.rx1.rxSingle
import rx.Observable
import rx.Single
import rx.SingleSubscriber
import rx.Subscription
import java.util.concurrent.TimeoutException

import javax.inject.Provider
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.startCoroutine

/**
 * @author Clay Molocznik
 */
abstract class Action<IN : Any, OUT : Any> : IAction<IN, OUT>() {
   lateinit var context: ActionContext
   lateinit var request: IN
   lateinit var vertx: Vertx
   lateinit var requestProvider: Provider<IN>
   lateinit var replyProvider: Provider<OUT>
   lateinit var command: HystrixObservableCommand<OUT>

   private var timedOut: Boolean = false
   private var executeException: Throwable? = null
   private var executeCause: Throwable? = null
   private var fallbackException: Throwable? = null
   private var fallbackCause: Throwable? = null
   private lateinit var dispatcher: CoroutineDispatcher

   lateinit internal var setter: HystrixObservableCommand.Setter

   open val isFallbackEnabled: Boolean
      get() = false

   internal open fun init(vertx: Vertx,
                          context: ActionContext,
                          request: IN,
                          requestProvider: Provider<IN>,
                          replyProvider: Provider<OUT>,
                          setter: HystrixObservableCommand.Setter) {
      this.vertx = vertx
      this.context = context
      this.request = request
      this.requestProvider = requestProvider
      this.replyProvider = replyProvider
      this.setter = setter
      this.dispatcher = createDispatcher()
      this.command = createCommand()

      afterInit()
   }

   open fun afterInit() {

   }

   open fun createDispatcher(): CoroutineDispatcher {
      return VertxContextDispatcher(context, vertx.orCreateContext)
   }

   /**
    * @return
    */
   open fun createCommand(): HystrixObservableCommand<OUT> {
      return Command(setter)
   }

   /**
    * Runs a block on the default vertx WorkerExecutor in a coroutine.
    */
   suspend fun <T> blocking(block: suspend () -> T): T = blocking(false, block)

   /**
    * Runs a block on the default vertx WorkerExecutor in a coroutine.
    */
   suspend fun <T> blocking(ordered: Boolean, block: suspend () -> T): T =
      vertx.rxExecuteBlocking<T>({
         try {
            it.complete(runBlocking { block() })
         } catch (e: Throwable) {
            it.fail(e)
         }
      }, ordered).await()

   /**
    * Runs a block on a specified vertx WorkerExecutor in a coroutine.
    */
   suspend fun <T> blocking(executor: WorkerExecutor, block: suspend () -> T): T =
      executor.rxExecuteBlocking<T> {
         try {
            it.complete(runBlocking { block() })
         } catch (e: Throwable) {
            it.fail(e)
         }
      }.await()

   /**
    * Runs a block on a specified vertx WorkerExecutor in a coroutine.
    */
   suspend fun <T> blocking(executor: WorkerExecutor, ordered: Boolean, block: suspend () -> T): T =
      executor.rxExecuteBlocking<T>({
         try {
            it.complete(runBlocking { block() })
         } catch (e: Throwable) {
            it.fail(e)
         }
      }, ordered).await()

   /**
    * Runs a block on the default vertx WorkerExecutor in a coroutine.
    */
   suspend fun <T> worker(block: suspend () -> T): Single<T> = worker(false, block)

   /**
    * Runs a block on the default vertx WorkerExecutor in a coroutine.
    */
   suspend fun <T> worker(ordered: Boolean, block: suspend () -> T): Single<T> =
      vertx.rxExecuteBlocking<T>({
         try {
            it.complete(runBlocking { block() })
         } catch (e: Throwable) {
            it.fail(e)
         }
      }, ordered)

   /**
    * Runs a block on a specified vertx WorkerExecutor in a coroutine.
    */
   suspend fun <T> worker(executor: WorkerExecutor, block: suspend () -> T): Single<T> = worker(executor, false, block)

   /**
    * Runs a block on a specified vertx WorkerExecutor in a coroutine.
    */
   suspend fun <T> worker(executor: WorkerExecutor, ordered: Boolean, block: suspend () -> T): Single<T> =
      executor.rxExecuteBlocking<T>({
         try {
            it.complete(runBlocking { block() })
         } catch (e: Throwable) {
            it.fail(e)
         }
      }, ordered)

   /**
    * Creates cold [Single] that runs a given [block] in a coroutine.
    * Every time the returned single is subscribed, it starts a new coroutine in the specified [context].
    * Coroutine returns a single value. Unsubscribing cancels running coroutine.
    *
    * | **Coroutine action**                  | **Signal to subscriber**
    * | ------------------------------------- | ------------------------
    * | Returns a value                       | `onSuccess`
    * | Failure with exception or unsubscribe | `onError`
    */
   protected fun <T> single(block: suspend CoroutineScope.() -> T): Single<T> = rxSingle(dispatcher, block)

   /**
    *
    */
   protected inline fun request(block: IN.() -> Unit): IN = requestProvider.get().apply(block)

   /**
    *
    */
   protected inline fun reply(block: OUT.() -> Unit): OUT = replyProvider.get().apply(block)

   /**
    *
    */
   protected abstract suspend fun recover(caught: Throwable, cause: Throwable, isFallback: Boolean): OUT

   /**
    * @param request
    */
   protected abstract suspend fun execute(): OUT

   /**
    *
    */
   protected suspend open fun shouldExecuteFallback(caught: Throwable, cause: Throwable): Boolean {
      return when (cause) {
         is HystrixTimeoutException -> false
         is TimeoutException -> false
         else -> true
      }
   }

   /**
    * @param request
    */
   protected suspend open fun executeFallback(caught: Throwable?, cause: Throwable?): OUT {
      return execute()
   }

   /**
    * @return
    */
   internal fun observe(): Observable<OUT> {
      return command.observe()
   }

   /**
    * @return
    */
   internal fun single(): Single<OUT> {
      return command.observe().toSingle()
   }

   /**
    * @return
    */
   internal fun toObservable(): Observable<OUT> {
      return command.toObservable()
   }

   /**
    * @return
    */
   internal fun toSingle(): Single<OUT> {
      return command.toObservable().toSingle()
   }

   companion object {
      val contextLocal = ThreadLocal<ActionContext?>()

      fun currentContext(): ActionContext? {
         return contextLocal.get()
      }
   }

   suspend open fun afterExecute(reply: OUT): OUT {
      return reply
   }

   inner class Command(setter: HystrixObservableCommand.Setter?) : HystrixObservableCommand<OUT>(setter) {
      private var timedOut: Boolean = false

      /**
       * Creates cold [Single] that runs a given [block] in a coroutine.
       * Every time the returned single is subscribed, it starts a new coroutine in the specified [context].
       * Coroutine returns a single value. Unsubscribing cancels running coroutine.
       *
       * | **Coroutine action**                  | **Signal to subscriber**
       * | ------------------------------------- | ------------------------
       * | Returns a value                       | `onSuccess`
       * | Failure with exception or unsubscribe | `onError`
       */
      fun <T> hystrixSingle(
         context: CoroutineContext,
         block: suspend CoroutineScope.() -> T
      ): Single<T> = Single.create<T> { subscriber ->
         val newContext = newCoroutineContext(context)
         val coroutine = HystrixSingleCoroutine(newContext, subscriber)
         coroutine.initParentJob(context[Job])
         subscriber.add(coroutine)
         block.startCoroutine(coroutine, coroutine)
      }

      inner class HystrixSingleCoroutine<T>(
         override val parentContext: CoroutineContext,
         private val subscriber: SingleSubscriber<T>
      ) : AbstractCoroutine<T>(true), Subscription {

         @Suppress("UNCHECKED_CAST")
         override fun afterCompletion(state: Any?, mode: Int) {
            if (state is CompletedExceptionally)
               subscriber.onError(let {
                  if (timedOut)
                     HystrixTimeoutException()
                  else
                     state.exception
               })
            else
               subscriber.onSuccess(state as T)
         }

         override fun isUnsubscribed(): Boolean = isCompleted

         override fun unsubscribe() {
            // HystrixTimer calls unsubscribe right before onError() which will
            // call "cancel()" on the coroutine which will throw a Job Cancelled exception
            // instead of a HystrixTimeoutException. Flag as timedOut if the HystrixCommand
            // actually timed out.
            if (!timedOut)
               timedOut = isTimedOut()

            // Always cancel coroutine.
            cancel()
         }
      }

      override fun construct(): Observable<OUT>? {
         return hystrixSingle(dispatcher) {
            try {
               afterExecute(execute())
            } catch (e: Throwable) {
               this@Action.executeException = let {
                  if (this@Command.executionException == null)
                     e
                  else
                     this@Command.executionException
               }

               this@Action.executeCause = Throwables.getRootCause(this@Action.executeException)

               if (isActionTimeout(this@Action.executeCause!!)) {
                  throw this@Action.executeCause!!
               }

               if (isFallbackEnabled && shouldExecuteFallback(this@Action.executeException!!, this@Action.executeCause!!)) {
                  throw this@Action.executeException!!
               } else {
                  recover(this@Action.executeException!!, this@Action.executeCause!!, false)
               }
            }
         }.toObservable()
      }

      override fun resumeWithFallback(): Observable<OUT> {
         return hystrixSingle(dispatcher) {
            try {
               if (!isFallbackEnabled || !shouldExecuteFallback(this@Action.executeException!!, this@Action.executeCause!!)) {
                  val e = ActionFallbackException()
                  try {
                     recover(e, e, true)
                  } catch (e: Exception) {
                     throw e
                  }
               } else {
                  afterExecute(executeFallback(this@Action.executeException, this@Action.executeCause))
               }
            } catch (e: Throwable) {
               this@Action.fallbackException = let {
                  if (this@Command.executionException == null)
                     e
                  else
                     this@Command.executionException
               }

               this@Action.fallbackCause = Throwables.getRootCause(this@Action.fallbackException)

               if (isActionTimeout(this@Action.fallbackCause!!)) {
                  throw this@Action.fallbackCause!!
               }

               try {
                  recover(this@Action.fallbackException!!, this@Action.fallbackCause!!, true)
               } catch (e2: Throwable) {
                  if (e2 is HystrixRuntimeException && e2.cause != null)
                     throw e2.cause!!
                  throw e2
               }
            }
         }.toObservable()
      }
   }
}
