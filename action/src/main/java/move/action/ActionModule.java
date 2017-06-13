package move.action;

import dagger.Module;
import dagger.Provides;

import javax.annotation.Nullable;

/**
 *
 */
@Module
public class ActionModule {
    @Provides
    @Nullable
    Void voidValue() {
        return null;
    }

    @Provides
    Boolean booleanValue() {
        return false;
    }

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
