package move.model

import io.vertx.rxjava.core.Vertx
import move.sql.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 */
@Table
class OrderEntity(
        @Column
        var number: String? = null
) : AbstractEntity()


@Singleton
class DB @Inject constructor(val vertx: Vertx) : SqlDatabase(vertx, SqlConfig(), arrayOf("move.action.model"), arrayOf("move.action.model")) {

}