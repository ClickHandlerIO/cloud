package io.clickhandler.action;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.hazelcast.core.*;
import com.hazelcast.map.listener.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Future;
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
 * Manages a single Store type. The local cache
 *
 * @author Clay Molocznik
 */
public class StoreManager extends AbstractVerticle {
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
    protected final Provider<? extends Store> storeProvider;

    private final MigrationListener migrationListener = new MigrationListenerImpl();
    private final DistributedMapListener entryListener = new DistributedMapListener();

    private final ConcurrentMap<Integer, PartitionProxy> partitionMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, StoreActionProvider<?, ?, ?, ?>> actionProviderMap = new ConcurrentHashMap<>();

    private IMap<String, byte[]> hzMap;
    private final Cache<String, Store> localCache = CacheBuilder.newBuilder()
        .removalListener((RemovalListener<String, Store>) removalNotification -> {
            removalNotification.getValue().stop(Future.<Void>future().setHandler(event -> {
            }));

            if (removalNotification.wasEvicted()) {
                if (hzMap != null)
                    hzMap.removeAsync(removalNotification.getKey());
            }
        })
        .build();
    private String migrationListenerId = null;
    private String entryListenerId = null;
    private String storeName = "";
    private Member localMember;

    public StoreManager(Vertx vertx, HazelcastInstance hazelcast, ActionManager actionManager, Provider<? extends Store> storeProvider) {
        this.vertx = vertx;
        this.hazelcast = hazelcast;
        this.partitionService = hazelcast == null ? null : hazelcast.getPartitionService();
        this.actionManager = actionManager;
        this.storeProvider = storeProvider;
        this.storeName = storeProvider.get().getName();
        this.log = LoggerFactory.getLogger(getClass() + "." + storeName);
    }

    void addStoreActionProvider(StoreActionProvider<?, ?, ?, ?> storeActionProvider) {
        storeActionProvider.setStoreManager(this);
        actionProviderMap.put(storeActionProvider.getName(), storeActionProvider);
    }

    @Override
    public void start(io.vertx.core.Future<Void> startFuture) throws Exception {
        storeName = storeProvider.get().getName();

        if (hazelcast != null) {
            vertx.executeBlockingObservable(event -> {
                migrationListenerId = partitionService.addMigrationListener(migrationListener);
                hzMap = hazelcast.getMap("___stores-" + storeName);
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
            return Observable.just(localCache.asMap().containsKey(key));
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
        return "__store|" + partition + "|" + storeName;
    }

    /**
     * @param key
     * @param state
     * @param addToCluster
     * @return
     */
    protected Observable<Store> getOrCreate(String key, byte[] state, boolean addToCluster) {
        return Observable.create(subscriber -> {
            if (subscriber.isUnsubscribed())
                return;

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

            final AtomicBoolean created = new AtomicBoolean(false);
            final Store store = Try.of(() -> localCache.get(key,
                () -> {
                    created.set(true);
                    // Create a new instance of the store.
                    final Store s = storeProvider.get();
                    s.setKey(key);
                    s.start(Future.<Void>future().setHandler(event -> {

                    }));
                    s.setScheduler(RxHelper.scheduler(vertx.getOrCreateContext()));
                    return s;
                })
            ).getOrElse(() -> null);

            if (addToCluster && created.get() && hzMap != null) {
                // Ensure it's in the cluster map.
                ((ICompletableFuture<byte[]>) hzMap.putAsync(key, state)).andThen(new ExecutionCallback<byte[]>() {
                    @Override
                    public void onResponse(byte[] response) {
                        if (subscriber.isUnsubscribed())
                            return;

                        try {
                            subscriber.onNext(store);
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
            } else {
                try {
                    subscriber.onNext(store);
                    subscriber.onCompleted();
                } catch (Throwable e) {
                    Try.run(() -> subscriber.onError(e));
                }
            }
        });
    }

    /**
     * @param actionProvider
     * @param timeoutMillis
     * @param key
     * @param request
     * @param <A>
     * @param <S>
     * @param <IN>
     * @param <OUT>
     * @return
     */
    public <A extends Action<IN, OUT>, S extends Store, IN, OUT> Observable<OUT> ask(StoreActionProvider<A, S, IN, OUT> actionProvider,
                                                                                     int timeoutMillis,
                                                                                     String key,
                                                                                     IN request) {
        // Is Owned locally?
        if (isOwnedLocally(key)) {
            return Observable.create(subscriber -> getOrCreate(key, new byte[]{(byte) 0}, true).subscribe(
                store -> {
                    if (store == null) {
                        Try.run(() -> subscriber.onError(new RuntimeException("Store not found for key [" + key + "]")));
                        return;
                    }

                    actionProvider.observe(store, request).subscribe(
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

        // Find partition.
        final Partition partition = partitionService.getPartition(key);

        final DeliveryOptions options = new DeliveryOptions();
        options.setSendTimeout(timeoutMillis);

        // Build address.
        final String deliveryAddress = buildAddress(partition.getPartitionId());
        final byte[] payload = actionManager.getStoreActionSerializer().byteify(request);

        return Observable.create(subscriber -> {
            MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();

            try {
                packer.packInt(ASK);
                packer.packInt(partition.getPartitionId());
                packer.packString(partition.getOwner().getUuid());
                packer.packString(storeName);
                packer.packString(actionProvider.getName());
                packer.packString(key);
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
                        if (event.cause() instanceof ReplyException) {
                            ReplyException replyException = (ReplyException) event.cause();
                        }
                        subscriber.onError(event.cause());
                        return;
                    }
                } catch (Throwable e) {
                    subscriber.onError(e);
                    return;
                }

                final io.vertx.rxjava.core.eventbus.Message<byte[]> message = event.result();

                if (message == null) {
                    subscriber.onError(new RuntimeException("Reply was empty"));
                    return;
                }

                final byte[] replyBody = message.body();

                try {
                    subscriber.onNext(
                        actionManager.getStoreActionSerializer().parse(
                            actionProvider.getOutClass(),
                            replyBody
                        )
                    );
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

        if (header.getCode() != ASK) {
            // Respond with EXPECTED_ASK code.
            message.fail(EXPECTED_ASK, "Expected Ask");
            return;
        }

        if (!header.getNodeId().equals(localMember.getUuid())) {
            message.fail(NODE_CHANGED, "Node Changed");
            return;
        }

        final StoreActionProvider actionProvider = actionProviderMap.get(header.getActionName());
        if (actionProvider == null) {
            // Respond with ACTION_NOT_FOUND
            message.fail(ACTION_NOT_FOUND, "Action Not Found");
            return;
        }
        final Object request;
        try {
            request = actionManager.getStoreActionSerializer()
                .parse(actionProvider.getInClass(), body, header.getHeaderSize(), header.getBodySize());
        } catch (Throwable e) {
            message.fail(BAD_REQUEST_FORMAT, "Bad Request Format");
            return;
        }

        getOrCreate(header.getKey(), new byte[]{(byte) 0}, true).subscribe(
            store -> {
                if (store == null) {
                    // Respond with STORE_NOT_FOUND
                    message.reply(buildResponseEnvelope(STORE_NOT_FOUND));
                } else {
                    try {
                        actionProvider.observe(store, request).subscribeOn(store.getScheduler()).observeOn(store.getScheduler()).subscribe(
                            actionResponse -> {
                                // Serialize response.
                                final byte[] responsePaylod;
                                try {
                                    responsePaylod = actionManager.getStoreActionSerializer().byteify(actionResponse);
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
                        log.error("Failed to invoke StoreActionProvider", e);
                        message.fail(REMOTE_FAILURE, e.getMessage());
                    }
                }
            },
            e -> {
                // Respond with STORE_UNAVAILABLE.
                message.fail(STORE_NOT_FOUND, "Store Not Found");
            }
        );
    }

    protected EnvelopeHeader parseHeader(byte[] data) {
        // Create unpacker.
        final MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data);
        final EnvelopeHeader header = new EnvelopeHeader();

        try {
            header.setCode(unpacker.unpackInt());
            header.setPartition(unpacker.unpackInt());
            header.setNodeId(Strings.nullToEmpty(unpacker.unpackString()));
            header.setName(Strings.nullToEmpty(unpacker.unpackString()));
            header.setActionName(Strings.nullToEmpty(unpacker.unpackString()));
            header.setKey(Strings.nullToEmpty(unpacker.unpackString()));
            header.setHeaderSize((int) unpacker.getTotalReadBytes());
            header.setBodySize(data.length - header.getHeaderSize());
        } catch (Throwable e) {
            log.warn("Failed to Parse EnvelopeHeader", e);
            return null;
        }

        return header;
    }

    protected byte[] buildResponseEnvelope(int code) {
        return buildResponseEnvelope(code, 0, "", "", "", "");
    }

    protected byte[] buildResponseEnvelope(int code, int partition, String nodeId, String name, String actionName, String key) {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();

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

    /**
     *
     */
    public static class EnvelopeHeader {

        private int code;
        private int partition;
        private String nodeId;
        private String name;
        private String key;
        private String actionName;
        private int headerSize;
        private int bodySize;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public int getPartition() {
            return partition;
        }

        public void setPartition(int partition) {
            this.partition = partition;
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getActionName() {
            return actionName;
        }

        public void setActionName(String actionName) {
            this.actionName = actionName;
        }

        public int getHeaderSize() {
            return headerSize;
        }

        public void setHeaderSize(int headerSize) {
            this.headerSize = headerSize;
        }

        public int getBodySize() {
            return bodySize;
        }

        public void setBodySize(int bodySize) {
            this.bodySize = bodySize;
        }
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

            consumer.handler(StoreManager.this::receive);
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
                getOrCreate(event.getKey(), event.getValue(), false);
            });
        }

        @Override
        public void entryRemoved(EntryEvent<String, byte[]> event) {
            // TODO: Remove local store.
            localCache.invalidate(event.getKey());
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
