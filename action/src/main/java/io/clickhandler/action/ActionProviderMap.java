package io.clickhandler.action;

import io.clickhandler.action.Action;

import javax.inject.Provider;
import java.util.Map;

/**
 *
 */
public interface ActionProviderMap extends Provider<Map<Class, Provider<? extends Action>>> {
}
