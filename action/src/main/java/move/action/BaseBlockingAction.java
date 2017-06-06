package move.action;

import com.netflix.hystrix.HystrixCommand;

/**
 *
 */
public abstract class BaseBlockingAction<IN, OUT> extends AbstractAction<IN, OUT> implements Action<IN, OUT> {
    abstract void setCommandSetter(HystrixCommand.Setter setter);
}
