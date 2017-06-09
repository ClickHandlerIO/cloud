package move.action

import io.vertx.core.VertxOptions
import io.vertx.rxjava.core.Vertx
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx1.awaitFirst
import move.action.Main.actions
import move.sql.AbstractEntity
import move.sql.SqlDatabase
import move.sql.SqlResult
import move.sql.SqlSession
import kotlin.reflect.KClass

class KSqlDatabase(val db: SqlDatabase) {
    suspend fun <T> read(block: suspend (SqlSession) -> T): T {
        return db.read {
            val session = it
            kotlinx.coroutines.experimental.runBlocking {
                block(session)
            }
        }.awaitFirst()
    }

    suspend fun <T> write(block: suspend (SqlSession) -> SqlResult<T>): SqlResult<T> {
        return db.write {
            val session = it
            kotlinx.coroutines.experimental.runBlocking {
                block(session)
            }
        }.awaitFirst()
    }

    suspend fun <T : AbstractEntity> get(cls: KClass<T>, id: String): T {
        return db.get(cls.java, id).awaitFirst()
    }
}


/**
 * @author Clay Molocznik
 */
object KotlinAction {
    val options = VertxOptions()//.setBlockedThreadCheckInterval(6000000)
    val vertx = Vertx.vertx(options)
    val sql: SqlDatabase? = null

    @JvmStatic
    fun main(args: Array<String>) {
        // Setup JBoss logging provider for Undertow.
        System.setProperty("org.jboss.logging.provider", "slf4j")
        // Setup IP stack. We want IPv4.
        System.setProperty("java.net.preferIPv6Addresses", "false")
        System.setProperty("java.net.preferIPv4Stack", "true")

        actions().register()

        async(Unconfined) {
            try {
//            Main.actions().kAllocateInventoryBlocking.await(KAllocateInventoryBlocking.Request())
                Main.actions().kAllocateInventory.await(KAllocateInventory.Request())
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}
