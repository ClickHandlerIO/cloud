package move.action

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.datastore.DatastoreOptions
import com.google.cloud.datastore.Entity
//import com.google.cloud.datastore.DatastoreOptions
//import com.google.cloud.datastore.Entity
import move.google.cloud.datastore.Datastore
import move.google.cloud.datastore.DatastoreConfig
import move.google.cloud.datastore.QueryBuilder


/**
 *
 */
object QuickstartSample {
   @Throws(Exception::class)
   @JvmStatic fun main(args: Array<String>) {

      val config = DatastoreConfig.builder()
         .requestTimeout(1000)
         .requestRetry(3)
         .project("untraceable-175800")
         .host("localhost:8397")
         .credential(GoogleCredential.getApplicationDefault())
//         .credential(CloudShellCredential(8397, JacksonFactory()))
//         .credential(GoogleCredential
//            .fromStream(credentialsInputStream)
//            .createScoped(DatastoreConfig.SCOPES))
         .build()
//
      run {
         val datastore = Datastore.create(config);

         val getResult = datastore.execute(QueryBuilder.query("employee", 12345678L))


         val insert = QueryBuilder.update("employee", 12345678L)
            .value("fullname", "Fred Blinge")
            .value("age", 42)
            .value("workdays", listOf("Monday", "Tuesday", "Friday")).upsert()

// for asynchronous call...
         val resultAsync = datastore.executeAsync(insert)
         val get = resultAsync.toBlocking().value()

// ...or for synchronous
         val result = datastore.execute(insert)

         println(result)
      }

      // Instantiates a client
//      val datastore = DatastoreOptions.getDefaultInstance().service

      val datastore = DatastoreOptions.newBuilder()
         .setProjectId("untraceable-175800")
         .setCredentials(GoogleCredentials.getApplicationDefault())
         .setHost("localhost:8397")
         .build().service
//      com.google.datastore.v1.client.DatastoreOptions.Builder().localHost("localhost:8397").build()

      // The kind for the new entity
      val kind = "Task"
      // The name/ID for the new entity
      val name = "id1"
      // The Cloud Datastore key for the new entity
      val taskKey = datastore.newKeyFactory().setKind(kind).newKey(name)

      // Prepares the new entity
      val task = Entity.newBuilder(taskKey)
         .set("description", "Buy milk")
         .build()

      // Saves the entity
      datastore.put(task)

      System.out.printf("Saved %s: %s%n", task.getKey().getName(), task.getString("description"))

      //Retrieve entity
      val retrieved = datastore.get(taskKey)

      System.out.printf("Retrieved %s: %s%n", taskKey.getName(), retrieved.getString("description"))

   }
}