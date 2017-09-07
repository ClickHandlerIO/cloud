package move.action;

import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 *
 */
@Singleton
public class ActionRegistry {

  public final Set<ActionProvider<?, ?, ?>> providers;
  public final Set<ActionProducer<?, ?, ?, ?>> producers;

  @Inject
  public ActionRegistry(Set<ActionProvider<?, ?, ?>> providers,
      Set<ActionProducer<?, ?, ?, ?>> producers) {
    this.providers = providers;
    this.producers = producers;
  }
}
