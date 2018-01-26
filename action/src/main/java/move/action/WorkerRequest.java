package move.action;

import io.vertx.rxjava.core.Context;
import rx.Subscriber;

/**
 *
 */
public class WorkerRequest {
   public WorkerActionProvider actionProvider;
   public int delaySeconds;
   public String groupId;
   public Object request;
   Subscriber<? super Boolean> subscriber;
   Context ctx;

   public WorkerRequest actionProvider(final WorkerActionProvider actionProvider) {
      this.actionProvider = actionProvider;
      return this;
   }

   public WorkerRequest delaySeconds(final int delaySeconds) {
      this.delaySeconds = delaySeconds;
      return this;
   }

   public WorkerRequest groupId(final String groupId) {
         this.groupId = groupId;
         return this;
      }

   public WorkerRequest request(final Object request) {
      this.request = request;
      return this;
   }
}