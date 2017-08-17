package move.action

import com.google.common.util.concurrent.AbstractIdleService
import move.google.cloud.pubsub.Pubsub

/**
 *
 */
class GCloudPubSubService : AbstractIdleService() {
   var pubsub: Pubsub? = null

   override fun startUp() {

   }

   override fun shutDown() {

   }

   inner class Receiver {

   }
}
