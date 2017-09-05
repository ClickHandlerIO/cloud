package move.action

import io.vertx.ext.web.RoutingContext

/**
 *
 */
abstract class HttpAction : Action<RoutingContext, Unit>() {
   val req
      get() = request.request()

   val resp
      get() = request.response()

   protected fun param(name: String) =
      req.getParam(name)
}
