package move.action;

import static kotlin.coroutines.experimental.CoroutinesKt.suspendCoroutine;

import kotlin.Unit;
import kotlin.coroutines.experimental.Continuation;
import kotlin.jvm.functions.Function1;
import kotlinx.coroutines.experimental.CoroutineContextKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by clay on 6/13/17.
 */
public class JavaAction extends Action<String, String> {

  @Nullable
  @Override
  protected Object recover(@NotNull Throwable caught, @NotNull Throwable cause, boolean isFallback,
      @NotNull Continuation<? super String> $continuation) {
    return null;
  }

  @Nullable
  @Override
  protected Object execute(@NotNull Continuation<? super String> $continuation) {
    suspend(continuation -> {
      continuation.resume("Hi");
      return null;
    }, $continuation);
    return null;
  }
}
