package move.action;

import dagger.Module;
import dagger.Provides;

/**
 *
 */
@Module
public class ActionModule {

  @Provides
  Object any() {
    return new Object();
  }
//
//    @Provides
//    S3EventNotification s3EventNotification() {
//        return new S3EventNotification(Collections.emptyList());
//    }
}
