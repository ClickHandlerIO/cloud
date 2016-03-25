package io.clickhandler.action;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.hazelcast.core.*;
import com.hazelcast.map.listener.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.MessageConsumer;
import javaslang.control.Try;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import rx.Observable;

import javax.inject.Provider;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages a single Actor type. The local cache
 *
 * @author Clay Molocznik
 */
public class ActorManager extends AbstractVerticle {
    public static final int ASK = 0;
    public static final int REPLY = 1;
    public static final int ACTION_NOT_FOUND = 2;
    public static final int STORE_NOT_FOUND = 3;
    public static final int REMOTE_FAILURE = 4;
    public static final int REMOTE_ACTION_TIMEOUT = 5;
    public static final int BAD_ENVELOPE = 6;
    public static final int NODE_CHANGED = 7;
    public static final int BAD_REQUEST_FORMAT = 8;
    public static final int EXPECTED_ASK = 9;

    protected final Logger log;
    protected final Vertx vertx;
    protected final HazelcastInstance hazelcast;
    protected final PartitionService partitionService;
    protected final ActionManager actionManager;
    protected final Provider<? extends AbstractActor> actorProvider;

    private final MigrationListener migrationListener = new MigrationListenerImpl();
    private final DistributedMapListener entryListener = new DistributedMapListener();
    private final ConcurrentMap<Integer, PartitionProxy> partitionMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ActorActionProvider<?, ?, ?, ?>> actionProviderMap = new ConcurrentHashMap<>();
    private final Cache<String, AbstractActor> localMap = CacheBuilder.newBuilder().build();
    private IMap<String, byte[]> hzMap;

    private String migrationListenerId = null;
    private String entryListenerId = null;
    private String actorName = "";
    private Member localMember;

    public ActorManager(Vertx vertx,
                        HazelcastInstance hazelcast,
                        ActionManager actionManager,
                        Provider<? extends AbstractActor> actorProvider) {
        this.vertx = vertx;
        this.hazelcast = hazelcast;
        this.partitionService = hazelcast == null ? null : hazelcast.getPartitionService();
        this.actionManager = actionManager;
        this.actorProvider = actorProvider;
        this.actorName = actorProvider.get().getName();
        this.log = LoggerFactory.getLogger(getClass() + "." + actorName);
    }

    protected String clusterMapName() {
        return "___actor-" + actorName;
    }

    void addStoreActionProvider(ActorActionProvider<?, ?, ?, ?> actorActionProvider) {
        actorActionProvider.setActorManager(this);
        actionProviderMap.put(actorActionProvider.getName(), actorActionProvider);
    }

    @Override
    public void start(io.vertx.core.Future<Void> startFuture) throws Exception {
        actorName = actorProvider.get().getName();

        if (hazelcast != null) {
            vertx.executeBlockingObservable(event -> {
                migrationListenerId = partitionService.addMigrationListener(migrationListener);
                hzMap = hazelcast.getMap(clusterMapName());
                entryListenerId = hzMap.addLocalEntryListener(entryListener);

                localMember = hazelcast.getCluster().getLocalMember();

                hazelcast.getPartitionService().getPartitions().forEach(p -> {
                    if (!p.getOwner().equals(localMember))
                        return;

                    final PartitionProxy partition = new PartitionProxy(p.getPartitionId());
                    partition.subscribe();

                    partitionMap.put(p.getPartitionId(), partition);
                });
            }).subscribe(
                r -> {
                },
                startFuture::fail
            );
        } else {
            Try.run(startFuture::complete);
        }
    }

    @Override
    public void stop(io.vertx.core.Future<Void> stopFuture) throws Exception {
        partitionMap.values().forEach(p -> p.unsubscribe());
        if (migrationListenerId != null) {
            partitionService.removeMigrationListener(migrationListenerId);
            migrationListenerId = null;
        }
        if (entryListenerId != null) {
            hzMap.removeEntryListener(entryListenerId);
            entryListenerId = null;
        }
        stopFuture.complete();
    }

    /**
     * Determines if the current Node is the owner of the key.
     *
     * @param key key to test
     * @return true if the Local Node is the owner; otherwise, false
     */
    public boolean isOwnedLocally(String key) {
        if (partitionService != null) {
            return partitionService.getPartition(key).getOwner().equals(hazelcast.getCluster().getLocalMember());
        }
        return true;
    }

    /**
     * Determines whether a particular
     *
     * @param key
     * @return
     */
    public Observable<Boolean> containsKey(String key) {
        if (hzMap == null) {
            return Observable.just(localMap.asMap().containsKey(key));
        }

        return Observable.create(subscriber -> {
            ((ICompletableFuture<byte[]>) hzMap.getAsync(key)).andThen(new ExecutionCallback<byte[]>() {
                @Override
                public void onResponse(byte[] response) {
                    if (subscriber.isUnsubscribed())
                        return;

                    Try.run(() -> subscriber.onNext(response != null));
                    Try.run(() -> subscriber.onCompleted());
                }

                @Override
                public void onFailure(Throwable t) {
                    if (subscriber.isUnsubscribed())
                        return;

                    Try.run(() -> subscriber.onError(t));
                }
            });
        });
    }

    /**
     * @param partition
     * @return
     */
    protected String buildAddress(int partition) {
        return "__store|" + partition + "|" + actorName;
    }

    /**
     * @param key=
     * @param addToCluster
     * @return
     */
    protected Observable<AbstractActor> getActor(String key, boolean addToCluster) {
        return Observable.create(subscriber -> {
            if (subscriber.isUnsubscribed())
                return;

            final AtomicBoolean created = new AtomicBoolean(false);

            final AbstractActor actor = Try.of(() -> localMap.get(key,
                () -> {
                    created.set(true);
                    // Create a new instance of the actor.
                    final AbstractActor s = actorProvider.get();
                    s.setKey(key);
                    s.setVertx(vertx);
                    s.setContext(vertx.getOrCreateContext());
                    s.setScheduler(RxHelper.scheduler(s.getContext()));
                    s.started();
                    return s;
                })
            ).getOrElse(() -> null);

            if (actor == null) {
                try {
                    subscriber.onNext(null);
                    subscriber.onCompleted();
                } catch (Throwable e) {
                    Try.run(() -> subscriber.onError(e));
                }
                return;
            }

            if (!created.get() || hzMap == null || partitionMap == null) {
                try {
                    subscriber.onNext(actor);
                    subscriber.onCompleted();
                } catch (Throwable e) {
                    Try.run(() -> subscriber.onError(e));
                }
                return;
            }

            final Partition p = partitionService.getPartition(key);
            // Ensure partition started.
            PartitionProxy partition = partitionMap.get(p.getPartitionId());
            if (partition == null) {
                partition = new PartitionProxy(p.getPartitionId());
                // Ensure partition is mapped.
                if (partitionMap.putIfAbsent(partition.id, partition) == null) {
                    partition.subscribe();
                }
            }

            if (addToCluster && created.get()) {
                // Ensure it's in the cluster map.
                ((ICompletableFuture<byte[]>) hzMap.putAsync(key, new byte[]{(byte) 0})).andThen(new ExecutionCallback<byte[]>() {
                    @Override
                    public void onResponse(byte[] response) {
                        if (subscriber.isUnsubscribed())
                            return;

                        try {
                            subscriber.onNext(actor);
                            subscriber.onCompleted();
                        } catch (Throwable e) {
                            Try.run(() -> subscriber.onError(e));
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Try.run(() -> subscriber.onError(t));
                    }
                });
            }
        });
    }

    /**
     * Invokes the ActorAction where the Actor lives.
     *
     * @param actionProvider
     * @param timeoutMillis
     * @param key
     * @param request
     * @param <A>
     * @param <ACTOR>
     * @param <IN>
     * @param <OUT>
     * @return
     */
    public <A extends Action<IN, OUT>, ACTOR extends AbstractActor, IN, OUT> Observable<OUT> ask(ActorActionProvider<A, ACTOR, IN, OUT> actionProvider,
                                                                                                 int timeoutMillis,
                                                                                                 String key,
                                                                                                 IN request) {
        // Is Owned locally?
        if (isOwnedLocally(key)) {
            // Invoke locally.
            return Observable.create(subscriber -> getActor(key, true).subscribe(
                actor -> {
                    if (actor == null) {
                        Try.run(() -> subscriber.onError(new RuntimeException("Actor not found for key [" + key + "]")));
                        return;
                    }

                    actor.invoke(actionProvider, request).subscribe(
                        actionResponse -> {
                            if (subscriber.isUnsubscribed())
                                return;

                            try {
                                subscriber.onNext(actionResponse);
                                subscriber.onCompleted();
                            } catch (Throwable e) {
                                Try.run(() -> subscriber.onError(e));
                            }
                        },
                        e -> {
                            if (subscriber.isUnsubscribed())
                                return;

                            Try.run(() -> subscriber.onError(e));
                        }
                    );
                },
                e -> {
                    if (subscriber.isUnsubscribed())
                        return;

                    Try.run(() -> subscriber.onError(e));
                }
            ));
        }

        // Serialize payload.
        final byte[] payload;
        try {
            payload = actionManager.getActorActionSerializer().byteify(request);
        } catch (Throwable e) {
            log.error("ActorActionSerializer.byteify threw an exception trying to serialize type [" + actionProvider.getInClass().getCanonicalName() + "]");
            return Observable.error(e);
        }

        // Find partition.
        final Partition partition = partitionService.getPartition(key);

        // Build deliver options.
        final DeliveryOptions options = new DeliveryOptions();
        options.setSendTimeout(timeoutMillis);

        // Build address.
        final String deliveryAddress = buildAddress(partition.getPartitionId());

        return Observable.create(subscriber -> {
            MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
            try {
                packer.packInt(ASK);
                packer.packInt(partition.getPartitionId());
                packer.packString(partition.getOwner().getUuid());
                packer.packString(actorName);
                packer.packString(key);
                packer.packString(actionProvider.getName());
                packer.writePayload(payload);
            } catch (Throwable e) {
                if (subscriber.isUnsubscribed())
                    return;

                Try.run(() -> subscriber.onError(e));
                return;
            }

            // Send request to partition owner.
            vertx.eventBus().<byte[]>send(deliveryAddress, packer.toByteArray(), options, event -> {
                if (subscriber.isUnsubscribed())
                    return;

                try {
                    if (event.failed()) {
                        // We should have a ReplyException.
                        if (event.cause() instanceof ReplyException) {
                            ReplyException replyException = (ReplyException) event.cause();
                            switch (replyException.failureType()) {
                                case TIMEOUT:
                                    // Remote node received the request, but we haven't heard
                                    // back in the time allotted.
                                    break;
                                case NO_HANDLERS:
                                    // No partition handler for this ActorType was found.
                                    // We should tell the ActionManager to help us out with this.
                                    break;
                                case RECIPIENT_FAILURE:
                                    // The node that processed this request didn't like something.
                                    break;
                            }
                        }
                        // Let our subscriber know of the failure.
                        Try.run(() -> subscriber.onError(event.cause()));
                        return;
                    }
                } catch (Throwable e) {
                    Try.run(() -> subscriber.onError(event.cause()));
                    return;
                }

                // We received a response Message.
                final io.vertx.rxjava.core.eventbus.Message<byte[]> message = event.result();
                if (message == null) {
                    subscriber.onError(new RuntimeException("Reply was empty"));
                    return;
                }

                // Parse Response.
                final byte[] replyBody = message.body();
                final OUT out;
                try {
                    if (replyBody == null || replyBody.length == 0)
                        out = actionProvider.getOutProvider().get();
                    else
                        out = actionManager.getActorActionSerializer().parse(
                            actionProvider.getOutClass(),
                            message.body()
                        );
                } catch (Throwable e) {
                    log.error("Failed to parse Response JSON for type [" + actionProvider.getOutClass().getCanonicalName() + "]");
                    subscriber.onError(e);
                    return;
                }

                try {
                    subscriber.onNext(out);
                    subscriber.onCompleted();
                } catch (Throwable e) {
                    subscriber.onError(e);
                }
            });
        });
    }

    /**
     * @param message
     */
    @SuppressWarnings("unchecked")
    protected void receive(io.vertx.rxjava.core.eventbus.Message<byte[]> message) {
        final byte[] body = message.body();

        if (body == null) {
            message.fail(BAD_ENVELOPE, "");
            return;
        }

        // Create unpacker.
        final EnvelopeHeader header = parseHeader(body);
        if (header == null) {
            message.fail(BAD_ENVELOPE, "Bad Envelope");
            return;
        }

        if (header.code != ASK) {
            // Respond with EXPECTED_ASK code.
            message.fail(EXPECTED_ASK, "Expected Ask");
            return;
        }

        if (!header.nodeId.equals(localMember.getUuid())) {
            message.fail(NODE_CHANGED, "Node Changed");
            return;
        }

        final ActorActionProvider actionProvider = actionProviderMap.get(header.actionName);
        if (actionProvider == null) {
            // Respond with ACTION_NOT_FOUND
            message.fail(ACTION_NOT_FOUND, "Action Not Found");
            return;
        }

        final Object request;
        try {
            if (header.bodySize < 1)
                request = actionProvider.getInProvider().get();
            else
                request = actionManager.getActorActionSerializer()
                    .parse(actionProvider.getInClass(), body, header.headerSize, header.bodySize);
        } catch (Throwable e) {
            message.fail(BAD_REQUEST_FORMAT, "Bad Request Format");
            return;
        }

        getActor(header.key, true).subscribe(
            actor -> {
                if (actor == null) {
                    // Respond with STORE_NOT_FOUND
                    message.reply(responseEnvelope(STORE_NOT_FOUND));
                    return;
                }

                try {
                    actor.invoke(actionProvider, request).subscribe(
                        actionResponse -> {
                            // Serialize response.
                            final byte[] responsePaylod;
                            try {
                                responsePaylod = actionManager.getActorActionSerializer().byteify(actionResponse);
                            } catch (Throwable e) {
                                message.fail(REMOTE_FAILURE, e.getMessage());
                                log.warn("Failed to serialize Response Type [" + actionProvider.getOutClass().getCanonicalName() + "]", e);
                                return;
                            }
                            message.reply(responsePaylod);
                        },
                        e -> {
                            message.fail(REMOTE_FAILURE, ((Throwable) e).getMessage());
                        }
                    );
                } catch (Throwable e) {
                    log.error("Failed to invoke ActorActionProvider", e);
                    message.fail(REMOTE_FAILURE, e.getMessage());
                }
            },
            e -> {
                // Respond with STORE_UNAVAILABLE.
                message.fail(STORE_NOT_FOUND, "Actor Not Found");
            }
        );
    }

    protected EnvelopeHeader parseHeader(byte[] data) {
        // Create unpacker.
        final MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data);
        final EnvelopeHeader header = new EnvelopeHeader();

        try {
            header.code = unpacker.unpackInt();
            header.partition = unpacker.unpackInt();
            header.nodeId = Strings.nullToEmpty(unpacker.unpackString());
            header.name = Strings.nullToEmpty(unpacker.unpackString());
            header.key = Strings.nullToEmpty(unpacker.unpackString());
            header.actionName = Strings.nullToEmpty(unpacker.unpackString());
            header.headerSize = (int) unpacker.getTotalReadBytes();
            header.bodySize = data.length - header.headerSize;
        } catch (Throwable e) {
            log.warn("Failed to Parse EnvelopeHeader", e);
            return null;
        }

        return header;
    }

    protected byte[] responseEnvelope(int code) {
        return responseEnvelope(code, 0, "", "", "", "");
    }

    protected byte[] responseEnvelope(int code,
                                      int partition,
                                      String nodeId,
                                      String name,
                                      String actionName,
                                      String key) {
        final MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        try {
            packer.packInt(code);
            packer.packInt(partition);
            packer.packString(nodeId);
            packer.packString(name);
            packer.packString(actionName);
            packer.packString(key);
        } catch (Throwable e) {
            log.error("Failed to Pack Response Envelope", e);
        }

        return packer.toByteArray();
    }

    void onStopped(AbstractActor actor) {
        if (hzMap != null) {
            ((ICompletableFuture<byte[]>) hzMap.removeAsync(actor.getKey())).andThen(new ExecutionCallback<byte[]>() {
                @Override
                public void onResponse(byte[] response) {
                    localMap.invalidate(actor.getKey());
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Failed to remove key [" + actor.getKey() + "] from ClusterMap [" + clusterMapName() + "]", t);
                }
            });
        } else {
            localMap.invalidate(actor.getKey());
        }
    }

    /**
     *
     */
    static class EnvelopeHeader {
        private int code;
        private int partition;
        private String nodeId;
        private String name;
        private String key;
        private String actionName;
        private int headerSize;
        private int bodySize;
    }

    /**
     *
     */
    class PartitionProxy {
        private final int id;
        private final String address;
        private MessageConsumer<byte[]> consumer;

        public PartitionProxy(int id) {
            this.id = id;
            this.address = buildAddress(id);
        }

        public void subscribe() {
            synchronized (this) {
                if (consumer != null)
                    return;
                consumer = vertx.eventBus().consumer(address);
            }

            consumer.handler(ActorManager.this::receive);
        }

        public void unsubscribe() {
            synchronized (this) {
                if (consumer == null)
                    return;

                consumer.unregister();
            }
        }
    }

    /**
     *
     */
    class MigrationListenerImpl implements MigrationListener {
        @Override
        public void migrationStarted(MigrationEvent migrationEvent) {
            // If we are to receive
        }

        @Override
        public void migrationCompleted(MigrationEvent migrationEvent) {

        }

        @Override
        public void migrationFailed(MigrationEvent migrationEvent) {
        }
    }

    /**
     *
     */
    class DistributedMapListener implements EntryAddedListener<String, byte[]>,
        EntryRemovedListener<String, byte[]>,
        EntryUpdatedListener<String, byte[]>,
        EntryEvictedListener<String, byte[]>,
        MapEvictedListener,
        MapClearedListener {

        @Override
        public void entryAdded(EntryEvent<String, byte[]> event) {
            vertx.runOnContext($ -> {
                getActor(event.getKey(), false).subscribe(
                    r -> {
                    },
                    e -> {
                    }
                );
            });
        }

        @Override
        public void entryRemoved(EntryEvent<String, byte[]> event) {
            final AbstractActor actor = localMap.getIfPresent(event.getKey());
            if (actor != null) {
                actor.stop();
            }
        }

        @Override
        public void entryUpdated(EntryEvent<String, byte[]> event) {
            System.out.println("Entry Updated:" + event);
        }

        @Override
        public void entryEvicted(EntryEvent<String, byte[]> event) {
            System.out.println("Entry Evicted:" + event);
        }

        @Override
        public void mapEvicted(MapEvent event) {
            System.out.println("Map Evicted:" + event);
        }

        @Override
        public void mapCleared(MapEvent event) {
            System.out.println("Map Cleared:" + event);
        }
    }
}
