package move.action;

import com.netflix.hystrix.HystrixObservableCommand;

/**
 *
 */
public abstract class BaseObservableAction<IN, OUT> extends AbstractAction<IN, OUT> implements ObservableAction<IN, OUT> {
    abstract void setCommandSetter(HystrixObservableCommand.Setter setter);
}
