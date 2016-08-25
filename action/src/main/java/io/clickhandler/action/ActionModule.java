package io.clickhandler.action;

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
}
