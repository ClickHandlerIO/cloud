package io.clickhandler.remoting.codegen;

import io.clickhandler.action.ActionProvider;

/**
 *
 */
public class ActionSpec extends Namespace {
    private ActionProvider provider;
    private StandardType inSpec;
    private StandardType outSpec;

    public ActionSpec() {
        isClass(true);
    }

    public ActionProvider provider() {
        return this.provider;
    }

    public StandardType inSpec() {
        return this.inSpec;
    }

    public StandardType outSpec() {
        return this.outSpec;
    }

    public ActionSpec provider(final ActionProvider provider) {
        this.provider = provider;
        return this;
    }

    public ActionSpec inSpec(final StandardType inSpec) {
        this.inSpec = inSpec;
        return this;
    }

    public ActionSpec outSpec(final StandardType outSpec) {
        this.outSpec = outSpec;
        return this;
    }

    @Override
    public String path() {
        return parent().path();
    }
}
