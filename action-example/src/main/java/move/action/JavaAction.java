package move.action;

import kotlin.coroutines.experimental.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by clay on 6/13/17.
 */
public class JavaAction extends Action<String, String> {
    @Nullable
    @Override
    protected Object recover(@NotNull Throwable caught, @NotNull Throwable cause, boolean isFallback, @NotNull Continuation<? super String> $continuation) {
        return null;
    }

    @Nullable
    @Override
    protected Object execute(@NotNull Continuation<? super String> $continuation) {
        return null;
    }
}
