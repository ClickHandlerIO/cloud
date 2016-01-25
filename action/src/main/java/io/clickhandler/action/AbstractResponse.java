package io.clickhandler.action;

/**
 *
 */
public abstract class AbstractResponse<C extends Enum<C>> implements ActionResponse<C> {
    protected C code;
    private boolean success;

    public AbstractResponse() {
    }

    public AbstractResponse(C code) {
        this.code = code;
    }

    @Override
    public C getCode() {
        return code;
    }

    public void setCode(C code) {
        this.code = code;
    }

    public C code() {
        return this.code;
    }

    public boolean success() {
        return this.success;
    }

    public AbstractResponse code(final C code) {
        this.code = code;
        return this;
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    @Override
    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    public boolean isFailure() {
        return !success;
    }
}
