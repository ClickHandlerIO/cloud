package move.action;

import com.netflix.hystrix.HystrixCommand;

import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public abstract class AbstractBlockingAction<IN, OUT>
    extends BaseBlockingAction<IN, OUT> {
    private final AtomicReference<HystrixCommand<OUT>> command = new AtomicReference<>();
    private HystrixCommand.Setter setter;

    void configureCommand(HystrixCommand.Setter setter) {
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

            @Override
            protected OUT getFallback() {
                return AbstractBlockingAction.this.executeFallback();
            }
        };
    }

    /**
     * @return
     */
    public final HystrixCommand<OUT> command() {
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
    OUT execute() throws Exception {
        contextLocal.set(actionContext());
        try {
            return handle();
        } finally {
            contextLocal.remove();
        }
    }

    OUT executeFallback() {
        contextLocal.set(actionContext());
        try {
            return handleFallback();
        } finally {
            contextLocal.remove();
        }
    }

    /**
     * @return
     */
    protected OUT handle() {
        return handle(getRequest());
    }

    protected OUT handleFallback() {
        return handleFallback(getRequest());
    }

    /**
     * @param request
     * @return
     */
    public abstract OUT handle(IN request);

    public OUT handleFallback(IN request) {
        throw new ActionFallbackException();
    }
}
