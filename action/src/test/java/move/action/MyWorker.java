package move.action;

import javax.inject.Inject;

/**
 *
 */
@WorkerAction(fifo = true)
public class MyWorker extends AbstractWorkerAction<MyWorker.Request> {
    @Inject
    MyWorker() {
    }

    @Override
    protected void start(Request request) {
        System.out.println("MyWorker: " + System.currentTimeMillis());
        processed();
    }

    public static class Request {
        @Inject
        public Request() {
        }
    }
}
