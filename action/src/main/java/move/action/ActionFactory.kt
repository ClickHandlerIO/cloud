package move.action

import kotlinx.coroutines.experimental.rx1.await
import rx.Single

abstract class BaseInternalActionFactory<A : Action<IN, OUT>, IN : Any, OUT : Any> {
   abstract val provider: InternalActionProvider<A, IN, OUT>
}

abstract class WorkerActionFactory<A : WorkerAction<IN, OUT>, IN : Any, OUT : Any> {

}


abstract class ActorActionFactory<A : WorkerAction<IN, OUT>, IN : Any, OUT : Any> {

}


/**
 *
 */
abstract class InternalActionFactory<A : Action<IN, OUT>, IN : Any, OUT : Any> : BaseInternalActionFactory<A, IN, OUT>() {
   suspend operator fun invoke(request: IN): OUT {
      return provider.create().rx(request).await()
   }

   suspend infix fun await(request: IN): OUT {
      return provider.create().rx(request).await()
   }

   suspend infix fun ask(request: IN): OUT {
      return provider.create().rx(request).await()
   }

   infix fun rx(request: IN): Single<OUT> {
      return provider.create().rx(request)
   }

   infix fun defer(request: IN): Single<OUT> {
      return provider.create().rx(request)
   }

   suspend infix fun job(request: IN): Single<OUT> {
      return provider.create().rx(request)
   }

   infix fun defer(block: A.() -> IN): IN {
      return provider.create().let(block)
   }
}

abstract class RequestBuilderInternalActionFactory<A : Action<IN, OUT>, IN : Any, OUT : Any> : BaseInternalActionFactory<A, IN, OUT>() {
   suspend infix fun ask(block: IN.() -> Unit): OUT {
      val request = createRequest()
      request.apply(block)
      return provider.create().await(request)
   }

   suspend operator fun invoke(request: IN): OUT {
      return provider.create().rx(request).await()
   }

   suspend infix fun await(request: IN): OUT {
      return provider.create().rx(request).await()
   }

   suspend infix fun ask(request: IN): OUT {
      return provider.create().rx(request).await()
   }

   infix fun rx(request: IN): Single<OUT> {
      return provider.create().rx(request)
   }

   infix fun defer(request: IN): Single<OUT> {
      return provider.create().rx(request)
   }

   suspend infix fun job(request: IN): Single<OUT> {
      return provider.create().rx(request)
   }

   infix fun defer(block: A.() -> IN): IN {
      return provider.create().let(block)
   }

   abstract protected fun createRequest(): IN
}

abstract class InternalActionFactoryWithStringParam<A : Action<IN, OUT>, IN : Any, OUT : Any> : BaseInternalActionFactory<A, IN, OUT>() {
   suspend infix fun ask(block: A.() -> IN): OUT {
      val action = provider.create()
      return action.await(action.let(block))
   }

   suspend operator fun invoke(request: IN): OUT {
      return provider.create().rx(request).await()
   }

   suspend infix fun await(request: IN): OUT {
      return provider.create().rx(request).await()
   }

   suspend infix fun ask(request: IN): OUT {
      return provider.create().rx(request).await()
   }

   infix fun rx(request: IN): Single<OUT> {
      return provider.create().rx(request)
   }

   infix fun defer(request: IN): Single<OUT> {
      return provider.create().rx(request)
   }

   suspend infix fun job(request: IN): Single<OUT> {
      return provider.create().rx(request)
   }

   infix fun defer(block: A.() -> IN): IN {
      return provider.create().let(block)
   }

   suspend infix fun ask(param: String): OUT {
      val request = createWithParam(param)
      return provider.create().await(request)
   }

   suspend infix fun rxAsk(param: String): Single<OUT> {
      val request = createWithParam(param)
      return provider.create().rx(request)
   }

   abstract fun createWithParam(param: String): IN
}

abstract class InternalActionFactoryWithParam<A : Action<IN, OUT>, IN : Any, OUT : Any, P> : BaseInternalActionFactory<A, IN, OUT>() {
   suspend infix fun ask(block: A.() -> IN): OUT {
      val action = provider.create()
      return action.await(action.let(block))
   }

   suspend operator fun invoke(request: IN): OUT {
      return provider.create().rx(request).await()
   }

   suspend infix fun await(request: IN): OUT {
      return provider.create().rx(request).await()
   }

//   suspend infix fun ask(request: IN): OUT {
//      return provider.create().rx(request).await()
//   }

   infix fun rx(request: IN): Single<OUT> {
      return provider.create().rx(request)
   }

   infix fun defer(request: IN): Single<OUT> {
      return provider.create().rx(request)
   }

   suspend infix fun job(request: IN): Single<OUT> {
      return provider.create().rx(request)
   }

   infix fun defer(block: A.() -> IN): IN {
      return provider.create().let(block)
   }

   suspend infix fun ask(param: P): OUT {
      val request = createWithParam(param)
      return provider.create().await(request)
   }

   abstract fun createWithParam(param: P): IN
}

abstract class ActionFactory<A : Action<IN, OUT>, IN : Any, OUT : Any> {
   abstract val provider: ActionProvider<A, IN, OUT>
}