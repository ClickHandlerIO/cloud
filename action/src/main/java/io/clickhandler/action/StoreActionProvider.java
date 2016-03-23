package io.clickhandler.action;

import com.google.common.base.Strings;
import io.vertx.rxjava.core.Vertx;
import rx.Observable;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Objects;

/**
 *
 */
public class StoreActionProvider<A extends Action<IN, OUT>, STORE extends Store, IN, OUT> extends ActionProvider<A, IN, OUT> {
    private static final Object DEFAULT_CONTEXT = new Object();
    private static final int LATENCY_OFFSET_MILLIS = 500;

    @Inject
    Vertx vertx;
    @Inject
    Provider<STORE> storeProvider;

    private StoreAction storeAction;
    private String name = "";
    private StoreManager storeManager;

    @Inject
    public StoreActionProvider() {
    }

    public String getName() {
        return name;
    }

    public Provider<STORE> getStoreProvider() {
        return storeProvider;
    }

    public StoreManager getStoreManager() {
        return storeManager;
    }

    void setStoreManager(StoreManager storeManager) {
        this.storeManager = storeManager;
    }

    @Override
    protected void init() {
        storeAction = getActionClass().getAnnotation(StoreAction.class);
        name = Objects.toString(
            Strings.emptyToNull(storeAction.name()),
            getActionClass().getClass().getCanonicalName()
        );
        super.init();
    }

    /**
     *
     * @param store
     * @param request
     * @return
     */
    protected Observable<OUT> observe(STORE store, IN request) {
        return observe(store, request);
    }

    public Observable<OUT> ask(String key, IN request) {
        return ask(key, isExecutionTimeoutEnabled() ? (int) getTimeoutMillis() + LATENCY_OFFSET_MILLIS : 0, request);
    }

    public Observable<OUT> ask(String key, int timeoutMillis, IN request) {
        return storeManager.ask(this, timeoutMillis, key, request);
    }

    /**
     * @param request
     * @return
     */
    public Observable<OUT> observe(final IN request) {
        return observe(
            DEFAULT_CONTEXT,
            request,
            create()
        );
    }
}
