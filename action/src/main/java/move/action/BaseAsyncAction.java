package move.action;

import com.netflix.hystrix.HystrixObservableCommand;

/**
 *
 */
public abstract class BaseAsyncAction<IN, OUT> extends Action<IN, OUT> {
    abstract void configureCommand(HystrixObservableCommand.Setter setter);
}
