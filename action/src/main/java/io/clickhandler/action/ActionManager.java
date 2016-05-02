package io.clickhandler.action;

import com.hazelcast.core.*;
import com.hazelcast.partition.PartitionLostEvent;
import com.hazelcast.partition.PartitionLostListener;
import io.clickhandler.cloud.cluster.HazelcastProvider;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.Message;
import io.vertx.rxjava.core.eventbus.MessageConsumer;
import javaslang.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

import static io.clickhandler.action.EnvelopeHeader.*;

/**
 * Monitors and manages all Actions.
 *
 * @author Clay Molocznik
 */
@Singleton
public class ActionManager extends AbstractVerticle {
    private final static Logger log = LoggerFactory.getLogger(ActionManager.class);
    private final static Map<Object, ActionProvider<?, ?, ?>> actionProviderMap = new HashMap<>();
    private final static Map<Object, RemoteActionProvider<?, ?, ?>> remoteActionMap = new HashMap<>();
    private final static Map<Object, QueueActionProvider<?, ?, ?>> queueActionMap = new HashMap<>();
    private final static Map<Object, InternalActionProvider<?, ?, ?>> internalActionMap = new HashMap<>();
    private final static Map<Object, ActorActionProvider<?, ?, ?, ?>> actorActionMap = new HashMap<>();
    private final static Map<String, ActorManager> actorManagerMap = new HashMap<>();


    private final PartitionLostListener partitionLostListener = new PartitionLostListenerImpl();
    private final MigrationListener migrationListener = new MigrationListenerImpl();
    private final MembershipListener membershipListener = new MembershipListenerImpl();

    @Inject
    Vertx vertx;
    @Inject
    HazelcastProvider hazelcast;

    private javaslang.collection.HashMap<Integer, PartitionConsumer> partitionConsumers =
        javaslang.collection.HashMap.empty();
    private String membershipListenerId;
    private String migrationListenerId;
    private String partitionLostListenerId;
    private ActorActionSerializer actorActionSerializer = new ActorActionSerializerImpl();

    private Member localMember;

    @Inject
    public ActionManager() {
    }

    public ActorActionSerializer getActorActionSerializer() {
        return actorActionSerializer;
    }

    public void setActorActionSerializer(ActorActionSerializer actorActionSerializer) {
        this.actorActionSerializer = actorActionSerializer;
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        if (hazelcast.get() != null) {
            vertx.executeBlockingObservable(event -> {
                membershipListenerId = hazelcast.get().getCluster().addMembershipListener(membershipListener);
                migrationListenerId = hazelcast.get().getPartitionService().addMigrationListener(migrationListener);
                partitionLostListenerId = hazelcast.get().getPartitionService().addPartitionLostListener(partitionLostListener);

                localMember = hazelcast.get().getCluster().getLocalMember();

                syncPartitions();
            }).subscribe(
                r -> startFuture.complete(),
                startFuture::fail
            );
        } else {
            // TODO: Load all ActorManagers.
            startFuture.complete();
        }
    }

    private void finishStart(Future<Void> startFuture) {
        actorManagerMap.forEach((key, value) -> {
            ((io.vertx.core.Vertx) vertx.getDelegate()).deployVerticle(value);
        });

        startFuture.complete();
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        stopFuture.complete();

        if (hazelcast.get() != null) {
            Try.run(() -> hazelcast.get().getCluster().removeMembershipListener(membershipListenerId));
            Try.run(() -> hazelcast.get().getPartitionService().removeMigrationListener(migrationListenerId));
            Try.run(() -> hazelcast.get().getPartitionService().removePartitionLostListener(partitionLostListenerId));
        }
    }

    synchronized void register(Map<Object, ActionProvider<?, ?, ?>> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        actionProviderMap.putAll(map);

        map.forEach((key, value) -> {
            if (value == null) {
                return;
            }
            if (value instanceof RemoteActionProvider) {
                remoteActionMap.put(key, (RemoteActionProvider<?, ?, ?>) value);
            } else if (value instanceof QueueActionProvider) {
                queueActionMap.put(key, (QueueActionProvider<?, ?, ?>) value);
            } else if (value instanceof InternalActionProvider) {
                internalActionMap.put(key, (InternalActionProvider<?, ?, ?>) value);
            } else if (value instanceof ActorActionProvider<?, ?, ?, ?>) {
                ActorActionProvider<?, ?, ?, ?> actorActionProvider = (ActorActionProvider<?, ?, ?, ?>) value;

                final String actorName = actorActionProvider.getActorFactory().get().getName();

                ActorManager actorManager = actorManagerMap.get(actorName);
                if (actorManager == null) {
                    actorManager = new ActorManager(vertx, hazelcast.get(), this, actorActionProvider.getActorFactory());
                    actorManagerMap.put(actorName, actorManager);
                }
                actorManager.addAction(actorActionProvider);
                actorActionProvider.setActorManager(actorManager);

                actorActionMap.put(key, actorActionProvider);
            }
        });
    }

    public void setExecutionTimeoutEnabled(boolean enabled) {
        actionProviderMap.forEach((k, v) -> v.setExecutionTimeoutEnabled(enabled));
    }

    /**
     * @param partition
     * @return
     */
    String buildActorPartitionAddress(int partition) {
        return "___actor|" + partition;
    }

    private void syncPartitions() {
        final HazelcastInstance hz = hazelcast.get();

        if (hz == null)
            return;

        hz.getPartitionService().getPartitions().forEach(p -> {
            final PartitionConsumer consumer = partitionConsumers.get(p.getPartitionId()).getOrElse(() -> null);

            if (p.getOwner().localMember()) {
                if (consumer == null) {
                    PartitionConsumer newConsumer = new PartitionConsumer(p.getPartitionId());
                    Try.run(newConsumer::subscribe);
                    partitionConsumers = partitionConsumers.put(newConsumer.id, newConsumer);
                }
            } else if (consumer != null) {
                Try.run(consumer::unsubscribe);
                partitionConsumers = partitionConsumers.remove(consumer.id);
            }
        });
    }

    void ask(EnvelopeHeader header,
             byte[] messagePayload,
             DeliveryOptions options,
             Handler<AsyncResult<Message<byte[]>>> replyHandler) {
        vertx.eventBus().<byte[]>send(buildActorPartitionAddress(header.partition), messagePayload, options, replyHandler);
    }

    /**
     *
     */
    private class PartitionConsumer {
        private final int id;
        private final String address;
        private MessageConsumer<byte[]> consumer;

        public PartitionConsumer(int id) {
            this.id = id;
            this.address = buildActorPartitionAddress(id);
        }

        public void subscribe() {
            consumer = vertx.eventBus().consumer(address, this::receive);
        }

        private void receive(Message<byte[]> message) {
            final byte[] body = message.body();

            if (body == null) {
                message.fail(BAD_ENVELOPE, "");
                return;
            }

            // Create unpacker.
            final EnvelopeHeader header = EnvelopeHeader.parse(body);
            if (header == null) {
                message.fail(BAD_ENVELOPE, "Bad Envelope");
                return;
            }

            if (header.type != ASK) {
                // Respond with EXPECTED_ASK type.
                message.fail(EXPECTED_ASK, "Expected Ask");
                return;
            }

            if (!header.nodeId.equals(localMember.getUuid())) {
                message.fail(NODE_CHANGED, "Node Changed");
                return;
            }

            final ActorManager actorManager = actorManagerMap.get(header.actorName);
            if (actorManager == null) {
                message.fail(ACTOR_TYPE_NOT_FOUND, "Actor Type [" + header.actorName + "] Not Found");
                return;
            }

            // Check the partition.
            final Partition partition = actorManager.getPartition(header.key);
            if (!partition.getOwner().localMember()) {
                message.fail(PARTITION_MOVED, partition.getOwner().getUuid());
                return;
            }

            final ActorActionProvider actionProvider = actorManager.getAction(header.actionName);
            if (actionProvider == null) {
                // Respond with ACTION_NOT_FOUND
                message.fail(ACTION_NOT_FOUND, "Action [" + header.actionName + "] Not Found on Actor [" + header.actorName + "]");
                return;
            }

            final Object request;
            try {
                if (header.bodySize < 1)
                    request = actionProvider.getInProvider().get();
                else
                    request = actorActionSerializer
                        .parse(actionProvider.getInClass(), body, header.headerSize, header.bodySize);
            } catch (Throwable e) {
                log.warn("Failed to Serialize Request Type [" + actionProvider.getInClass().getCanonicalName() + "]", e);
                message.fail(BAD_REQUEST_FORMAT, "Bad Request Format");
                return;
            }

            actorManager.getActor(header.key, true).subscribe(
                actor -> {
                    if (actor == null) {
                        // Respond with ACTOR_NOT_FOUND
                        message.fail(ACTOR_NOT_FOUND, "Actor Instance [" + header.actorName + "." + header.key + "] Not Found");
                        return;
                    }

                    try {
                        actor.invoke(actionProvider, request).subscribe(
                            actionResponse -> {
                                // Serialize response.
                                final byte[] responsePaylod;
                                try {
                                    responsePaylod = actorActionSerializer.byteify(actionResponse);
                                } catch (Throwable e) {
                                    message.fail(EnvelopeHeader.REMOTE_FAILURE, e.getMessage());
                                    log.warn("Failed to serialize Response Type [" + actionProvider.getOutClass().getCanonicalName() + "]", e);
                                    return;
                                }
                                message.reply(responsePaylod);
                            },
                            e -> {
                                if (e instanceof ActorUnavailableException) {
                                    // Respond with ACTOR_UNAVAILABLE.
                                    message.fail(EnvelopeHeader.ACTOR_UNAVAILABLE, "Actor Not Available");
                                } else {
                                    message.fail(EnvelopeHeader.REMOTE_FAILURE, ((Throwable) e).getMessage());
                                }
                            }
                        );
                    } catch (Throwable e) {
                        log.error("Failed to invoke ActorActionProvider", e);
                        message.fail(EnvelopeHeader.REMOTE_FAILURE, e.getMessage());
                    }
                },
                e -> {
                    // Respond with ACTOR_UNAVAILABLE.
                    message.fail(EnvelopeHeader.ACTOR_UNAVAILABLE, "Actor Not Available");
                }
            );
        }

        public void unsubscribe() {
            consumer.unregister();
            consumer = null;
        }
    }

    /**
     *
     */
    private final class MembershipListenerImpl implements MembershipListener {
        @Override
        public void memberAdded(MembershipEvent membershipEvent) {
            context.runOnContext(event -> syncPartitions());
        }

        @Override
        public void memberRemoved(MembershipEvent membershipEvent) {
            context.runOnContext(event -> syncPartitions());
        }

        @Override
        public void memberAttributeChanged(MemberAttributeEvent memberAttributeEvent) {
            context.runOnContext(event -> syncPartitions());
        }
    }

    /**
     *
     */
    private final class MigrationListenerImpl implements MigrationListener {
        @Override
        public void migrationStarted(MigrationEvent migrationEvent) {
        }

        @Override
        public void migrationCompleted(MigrationEvent migrationEvent) {
            context.runOnContext(event -> syncPartitions());
        }

        @Override
        public void migrationFailed(MigrationEvent migrationEvent) {
            context.runOnContext(event -> syncPartitions());
        }
    }

    /**
     *
     */
    private final class PartitionLostListenerImpl implements PartitionLostListener {
        @Override
        public void partitionLost(PartitionLostEvent event) {
            context.runOnContext(ev -> syncPartitions());
        }
    }
}
