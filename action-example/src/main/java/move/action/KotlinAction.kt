package move.action

import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx1.awaitFirst
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
    @JvmStatic
    fun main(args: Array<String>) {
        // Setup JBoss logging provider for Undertow.
        System.setProperty("org.jboss.logging.provider", "slf4j")
        // Setup IP stack. We want IPv4.
        System.setProperty("java.net.preferIPv6Addresses", "false")
        System.setProperty("java.net.preferIPv4Stack", "true")

        val Actions: Action_Locator = AppComponent.instance.actions().move.action

//        actions().register()

        async(Unconfined) {
            try {
//            Main.actions().kAllocateInventoryBlocking.await(BaseAllocateInventoryBlocking.Request())



                val result = Actions.allocateInventory {
                    id = ""
                }

                println(result.code)
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
