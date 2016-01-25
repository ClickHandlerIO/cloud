package io.clickhandler.action;

/**
 *
 */
public interface ActionResponse<C extends Enum<C>> {
    C getCode();

    boolean isSuccess();

    void setSuccess(boolean success);

    boolean isFailure();
}
