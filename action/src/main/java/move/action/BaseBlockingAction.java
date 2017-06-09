package move.action;

import com.netflix.hystrix.HystrixCommand;

/**
 *
 */
public abstract class BaseBlockingAction<IN, OUT> extends Action<IN, OUT> {
    abstract void configureCommand(HystrixCommand.Setter setter);
}
