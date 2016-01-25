package io.clickhandler.action;

/**
 *
 */
public interface ActionOutcome<OUT> {
    boolean isSuccess(OUT out);
}
