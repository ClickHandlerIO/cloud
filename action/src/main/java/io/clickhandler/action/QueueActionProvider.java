package io.clickhandler.action;

import com.google.common.collect.Lists;
import rx.Observable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public class QueueActionProvider<A extends Action<IN, OUT>, IN, OUT> extends ActionProvider<A, IN, OUT> {
    private QueueAction queueAction;

    /**
     * @return
     */
    public QueueAction getQueueAction() {
        return queueAction;
    }

    /**
     *
     */
    @Override
    protected void init() {
        super.init();
        queueAction = getActionClass().getAnnotation(QueueAction.class);
    }

    /**
     * @param request
     * @return
     */
    public Observable<Boolean> add(final IN request) {
        return Observable.empty();
    }

    /**
     * @param callback
     * @return
     */
    public Observable<Boolean> add(final Func.Run1<IN> callback) {
        final IN request = getInProvider().get();
        if (callback != null) {
            callback.run(request);
        }
        return add(request);
    }

    /**
     * @param requests
     * @return
     */
    public List<Observable<Boolean>> add(final Collection<IN> requests) {
        if (requests == null || requests.isEmpty()) {
            return Lists.newArrayListWithCapacity(0);
        }

        final ArrayList<Observable<Boolean>> responseList = Lists.newArrayListWithCapacity(requests.size());
        return responseList;
    }
}
