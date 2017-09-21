package move.action;

import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 *
 */
@Singleton
public class ActionRegistry {

  public final Set<ActorProvider<?>> actorProviders;
  public final Set<ActorProducer<?, ?>> actorProducers;
  public final Set<ActionProvider<?, ?, ?>> actionProviders;
  public final Set<ActionProducer<?, ?, ?, ?>> actionProducers;

  @Inject
  ActionRegistry(Set<ActorProvider<?>> actorProviders,
      Set<ActorProducer<?, ?>> actorProducers,
      Set<ActionProvider<?, ?, ?>> actionProviders,
      Set<ActionProducer<?, ?, ?, ?>> actionProducers) {
    this.actorProviders = actorProviders;
    this.actorProducers = actorProducers;
    this.actionProviders = actionProviders;
    this.actionProducers = actionProducers;
  }
}
