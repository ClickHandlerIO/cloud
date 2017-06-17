package move.action

import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx1.await
import kotlinx.coroutines.experimental.rx1.awaitFirst
import move.sql.AbstractEntity
import move.sql.SqlDatabase
import move.sql.SqlResult
import move.sql.SqlSession
import kotlin.reflect.KClass

class SqlDatabaseKt(val db: SqlDatabase) {
    suspend fun <T> read(block: suspend (SqlSession) -> T): T {
        return db.read {
            val session = it
            kotlinx.coroutines.experimental.runBlocking {
                block(session)
            }
        }.await()
    }

    suspend fun <T> write(block: suspend (SqlSession) -> SqlResult<T>): SqlResult<T> {
        return db.write {
            val session = it
            kotlinx.coroutines.experimental.runBlocking {
                block(session)
            }
        }.await()
    }

    suspend fun <T : AbstractEntity> get(cls: KClass<T>, id: String): T {
        return db.get(cls.java, id).await()
    }
}


/**
 * @author Clay Molocznik
 */
object KotlinAction {
    @JvmStatic
    fun main(args: Array<String>) {
        // Setup JBoss logging provider for Undertow.
        System.setProperty("org.jboss.logging.provider", "slf4j")
        // Setup IP stack. We want IPv4.
        System.setProperty("java.net.preferIPv6Addresses", "false")
        System.setProperty("java.net.preferIPv4Stack", "true")

//        AppComponent.instance.db().startAsync().awaitRunning()

        val Actions: Action_Locator = AppComponent.instance.actions().move.action
        Actions.ensureActionMap()
        AppComponent.instance.actionManager().startAsync().awaitRunning()


        val executor = AppComponent.instance.vertx().createSharedWorkerExecutor("db", 10, Integer.MAX_VALUE.toLong());

//        actions().register()

        async(Unconfined) {
            try {
//                val result = Actions.allocateInventory { id = "" }

//                println(result.code)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}


object Another {
    @JvmStatic
    fun main(args: Array<String>) {
        println("")
    }
}