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
import javaslang.control.Try;
import rx.Observable;

import javax.inject.Provider;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages a single Actor type. The local cache
 *
 * @author Clay Molocznik
 */
public class ActorManager extends AbstractVerticle {

    protected final Logger log;
    protected final Vertx vertx;
    protected final HazelcastInstance hazelcast;
    protected final PartitionService partitionService;
    protected final ActionManager actionManager;
    protected final Provider<? extends AbstractActor> actorProvider;

    private final DistributedMapListener entryListener = new DistributedMapListener();
    private final ConcurrentMap<String, ActorActionProvider<?, ?, ?, ?>> actionProviderMap = new ConcurrentHashMap<>();
    private final Cache<String, AbstractActor<?>> localMap = CacheBuilder.newBuilder().build();
    private final ConcurrentLinkedDeque queue = new ConcurrentLinkedDeque();
    private IMap<String, byte[]> clusterMap;
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

    /**
     * @param actorActionProvider
     */
    void addAction(ActorActionProvider<?, ?, ?, ?> actorActionProvider) {
        actorActionProvider.setActorManager(this);
        actionProviderMap.put(actorActionProvider.getName(), actorActionProvider);
    }

    /**
     * @param name
     * @return
     */
    ActorActionProvider<?, ?, ?, ?> getAction(String name) {
        return actionProviderMap.get(name);
    }

    /**
     * @param startFuture
     * @throws Exception
     */
    @Override
    public void start(io.vertx.core.Future<Void> startFuture) throws Exception {
        actorName = actorProvider.get().getName();

        if (hazelcast != null) {
            vertx.executeBlockingObservable(event -> {
                clusterMap = hazelcast.getMap(clusterMapName());
                entryListenerId = clusterMap.addLocalEntryListener(entryListener);
                localMember = hazelcast.getCluster().getLocalMember();
            }).subscribe(
                r -> {
                },
                startFuture::fail
            );
        } else {
            Try.run(startFuture::complete);
        }
    }

    /**
     * @param stopFuture
     * @throws Exception
     */
    @Override
    public void stop(io.vertx.core.Future<Void> stopFuture) throws Exception {
        if (entryListenerId != null) {
            vertx.executeBlockingObservable(event -> {
                clusterMap.removeEntryListener(entryListenerId);
                entryListenerId = null;
            }).subscribe(
                result -> stopFuture.complete(),
                e -> {
                    log.error("Exception thrown while stopping", e);
                    stopFuture.complete();
                }
            );
        }
        stopFuture.complete();
    }

    Partition getPartition(String key) {
        return hazelcast == null ? null : hazelcast.getPartitionService().getPartition(key);
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
        if (clusterMap == null) {
            return Observable.just(localMap.asMap().containsKey(key));
        }

        return Observable.create(subscriber -> {
            ((ICompletableFuture<byte[]>) clusterMap.getAsync(key)).andThen(new ExecutionCallback<byte[]>() {
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
     * @param key=
     * @param addToCluster
     * @return
     */
    Observable<AbstractActor<?>> getActor(String key, boolean addToCluster) {
        return Observable.create(subscriber -> {
            if (subscriber.isUnsubscribed())
                return;

            final AtomicBoolean created = new AtomicBoolean(false);

            final AbstractActor<?> actor = Try.of(() -> localMap.get(key,
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

            if (!created.get() || clusterMap == null) {
                try {
                    subscriber.onNext(actor);
                    subscriber.onCompleted();
                } catch (Throwable e) {
                    Try.run(() -> subscriber.onError(e));
                }
                return;
            }

            final Partition p = getPartition(key);

            if (addToCluster && created.get()) {
                // Ensure it's in the cluster map.
                ((ICompletableFuture<byte[]>) clusterMap.putAsync(key, new byte[]{(byte) 0})).andThen(new ExecutionCallback<byte[]>() {
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

        // We need to ask a remote node.
        return Observable.create(subscriber -> {
            // Find partition.
            final Partition partition = getPartition(key);

            // Build deliver options.
            final DeliveryOptions options = new DeliveryOptions();
            options.setSendTimeout(timeoutMillis);

            final EnvelopeHeader header = new EnvelopeHeader();
            header.type = EnvelopeHeader.ASK;
            header.partition = partition.getPartitionId();
            header.nodeId = partition.getOwner().getUuid();
            header.actorName = actorName;
            header.key = Strings.nullToEmpty(key);
            header.actionName = Strings.nullToEmpty(actionProvider.getName());

            final byte[] messagePayload;
            try {
                messagePayload = header.toByteArray(payload);
            } catch (Throwable e) {
                if (subscriber.isUnsubscribed())
                    return;

                Try.run(() -> subscriber.onError(e));
                return;
            }

            // Send request to partition owner.
            actionManager.ask(header, messagePayload, options, event -> {
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

    void onStopped(AbstractActor actor) {
        if (clusterMap != null) {
            ((ICompletableFuture<byte[]>) clusterMap.removeAsync(actor.getKey())).andThen(new ExecutionCallback<byte[]>() {
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
