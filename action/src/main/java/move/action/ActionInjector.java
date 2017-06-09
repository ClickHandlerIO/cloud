package move.action;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 *
 */
public class ActionInjector<A extends Action> {
    public final Provider<A> provider;

    @Inject
    public ActionInjector(Provider<A> provider) {
        this.provider = provider;
    }

    public A get() {
        return provider.get();
    }
}
