package io.clickhandler.action;

import com.netflix.hystrix.HystrixCommand;

import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public abstract class AbstractBlockingAction<IN, OUT>
    extends AbstractAction<IN, OUT> {
    private final AtomicReference<HystrixCommand<OUT>> command = new AtomicReference<>();
    private HystrixCommand.Setter setter;

    void setCommandSetter(HystrixCommand.Setter setter) {
        this.setter = setter;
    }

    /**
     * @return
     */
    protected HystrixCommand.Setter getCommandSetter() {
        return setter;
    }

    /**
     * @return
     */
    protected HystrixCommand<OUT> build() {
        return new HystrixCommand<OUT>(getCommandSetter()) {
            @Override
            protected OUT run() throws Exception {
                return AbstractBlockingAction.this.execute();
            }
        };
    }

    /**
     * @return
     */
    protected final HystrixCommand<OUT> getCommand() {
        final HystrixCommand<OUT> existing = command.get();
        if (existing != null) {
            return existing;
        }

        final HystrixCommand<OUT> newCommand = build();
        if (!command.compareAndSet(null, newCommand)) {
            return command.get();
        } else {
            return newCommand;
        }
    }

    /**
     * @return
     * @throws Exception
     */
    public OUT execute() throws Exception {
        return handle();
    }

    /**
     * @return
     */
    protected OUT handle() {
        return handle(getRequest());
    }

    /**
     * @param request
     * @return
     */
    public abstract OUT handle(IN request);
}
