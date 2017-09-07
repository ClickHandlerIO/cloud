package move.action;

import java.util.Map;

/**
 *
 */
public interface ActionComponent {

  Map<Class<?>, ActionProvider<?, ?, ?>> byClass();

  Map<Class<? extends ActionProducer<?, ?, ?, ?>>, ActionProducer<?, ?, ?, ?>> producerMap();
}
