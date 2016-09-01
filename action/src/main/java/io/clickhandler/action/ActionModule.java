package io.clickhandler.action;

import com.amazonaws.services.s3.event.S3EventNotification;
import dagger.Module;
import dagger.Provides;

import javax.annotation.Nullable;
import java.util.Collections;

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
    S3EventNotification s3EventNotification() {
        return new S3EventNotification(Collections.emptyList());
    }
}
