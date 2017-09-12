//package move.presence;
//
//import com.google.common.base.Strings;
//import com.google.common.collect.Lists;
//import com.google.common.util.concurrent.AbstractExecutionThreadService;
//import com.google.common.util.concurrent.AbstractIdleService;
//import com.hazelcast.config.Config;
//import com.hazelcast.config.EvictionPolicy;
//import com.hazelcast.config.GroupConfig;
//import com.hazelcast.config.InMemoryFormat;
//import com.hazelcast.config.MapConfig;
//import com.hazelcast.core.ExecutionCallback;
//import com.hazelcast.core.Hazelcast;
//import com.hazelcast.core.HazelcastInstance;
//import com.hazelcast.core.IMap;
//import com.hazelcast.core.MembershipAdapter;
//import com.hazelcast.core.MembershipEvent;
//import com.hazelcast.map.AbstractEntryProcessor;
//import com.hazelcast.map.EntryBackupProcessor;
//import com.hazelcast.map.EntryProcessor;
//import com.hazelcast.map.listener.EntryEvictedListener;
//import com.hazelcast.nio.ObjectDataInput;
//import com.hazelcast.nio.ObjectDataOutput;
//import com.hazelcast.nio.serialization.DataSerializable;
//import io.vertx.rxjava.core.Vertx;
//import io.vertx.rxjava.core.eventbus.EventBus;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.Future;
//import java.util.concurrent.LinkedBlockingDeque;
//import java.util.concurrent.TimeUnit;
//import java.util.stream.Collectors;
//import javaslang.control.Try;
//import javax.inject.Inject;
//import javax.inject.Singleton;
//import move.cluster.HazelcastProvider;
//import move.common.UID;
//import move.common.WireFormat;
//import move.remoting.PushEnvelope;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import rx.Observable;
//
///**
// * Clustered Presence Service provides a "Here Now" feature.
// */
//@Singleton
//public class PresenceService extends AbstractIdleService {
//
//  public static final Logger LOG = LoggerFactory.getLogger(PresenceService.class);
//
//  public static final Presence NULL_PRESENCE = new Presence();
//  public static final String MAP_NAME = "presence";
//  public static final int MAX_OCCUPANTS = 100;
//  public static final int MAX_OCCUPANTS_JOIN = MAX_OCCUPANTS - 1;
//  public static final long IDLE_TIMEOUT = TimeUnit.SECONDS.toMillis(120);
//  public static final long OCCUPANT_IDLE_TIMEOUT = TimeUnit.SECONDS.toMillis(60);
//
//  private static EventBus BUS;
//  private final LinkedBlockingDeque<LeaveRequest> leaveQueue = new LinkedBlockingDeque<>();
//  @Inject
//  Vertx vertx;
//  @Inject
//  HazelcastProvider hazelcastProvider;
//  @Inject
//  EventBus eventBus;
//
//  private int maxOccupants = MAX_OCCUPANTS;
//  private int maxOccupantsJoin = MAX_OCCUPANTS_JOIN;
//  private long idleTimeout = IDLE_TIMEOUT;
//  private long occupantIdleTimeout = OCCUPANT_IDLE_TIMEOUT;
//
//  private IMap<String, Presence> map;
//  private String membershipListenerId;
//  private Cleaner cleaner;
//
//  @Inject
//  PresenceService() {
//  }
//
//  public static void main(String[] args) throws Throwable {
//    final String ip = "192.168.0.10";//App.getIp();
//    Config config = new Config("presence");
//    config.setGroupConfig(new GroupConfig("presence"));
////        config.setNetworkConfig(new NetworkConfig()
////            .setInterfaces(new InterfacesConfig()
////                .setEnabled(true)
////                .setInterfaces(Lists.newArrayList(ip)))
////            .setJoin(new JoinConfig()
////                .setTcpIpConfig(new TcpIpConfig()
////                    .setEnabled(false))
////                .setAwsConfig(new AwsConfig()
////                    .setEnabled(false))
////                .setMulticastConfig(new MulticastConfig()
////                    .setEnabled(true)
////                    .setMulticastGroup("224.2.2.3")
////                    .setMulticastPort(54327)
////                    .setMulticastTimeoutSeconds(1)
////                    .setMulticastTimeToLive(32)
////                    .setTrustedInterfaces(Sets.newHashSet(ip)))));
//    // Configure Presence map.
//    final MapConfig mapConfig = new MapConfig(PresenceService.MAP_NAME)
//        .setBackupCount(0)
//        .setAsyncBackupCount(0)
//        .setEvictionPolicy(EvictionPolicy.LFU)
//        // Cleanup stale entries.
//        .setMaxIdleSeconds((int) TimeUnit.MILLISECONDS.toSeconds(PresenceService.IDLE_TIMEOUT))
//        .setInMemoryFormat(InMemoryFormat.OBJECT);
//    config.addMapConfig(mapConfig);
//
//    HazelcastInstance hz = Hazelcast.newHazelcastInstance(config);
//    IMap<String, Presence> map = hz.getMap(MAP_NAME);
//
//    hz.getPartitionService().addPartitionLostListener(partitionLostEvent -> {
//
//    });
//
//    System.out.println(hz.getCluster().getLocalMember().getUuid());
//
//    // Handle local eviction.
//    map.addLocalEntryListener((EntryEvictedListener<String, Presence>) event -> {
//      final Presence presence = event.getOldValue();
//
//      if (presence == null) {
//        return;
//      }
//
//      final List<PresenceOccupant> occupants = presence.occupants;
//      if (occupants == null || occupants.isEmpty()) {
//        return;
//      }
//
//      final String key = event.getKey();
//
//      System.out.println("Evicting " + occupants.size() + " occupants");
//
//      occupants.stream()
//          .collect(Collectors
//              .groupingBy(
//                  $ -> $.nodeId,
//                  Collectors.mapping(
//                      $ -> $.id,
//                      Collectors.toSet())))
//          .forEach((nodeId, sessIds) -> {
//            if (BUS != null) {
//              BUS.publish(
//                  PresenceMessage.REMOVED + nodeId,
//                  WireFormat.byteify(
//                      new PresenceMessage()
//                          .key(key)
//                          .sessionIds(sessIds)
//                  )
//              );
//            }
//          });
//    });
//
////        App.wire(args, app -> {
////
////        });
//
//    final PresenceJoin addRequest = new PresenceJoin()
//        .device("IPHONE")
//        .userId(UID.next())
//        .name("John Doe")
//        .nodeId(hz.getCluster().getLocalMember().getUuid())
//        .sessionId(UID.next())
//        .state("");
//
//    final PresenceJoin addRequest2 = new PresenceJoin()
//        .device("WEB_BROWSER")
//        .userId(UID.next())
//        .name("Mary Doe")
//        .nodeId(hz.getCluster().getLocalMember().getUuid())
//        .sessionId(UID.next())
//        .state("");
//
//    final PresenceJoin addRequest3 = new PresenceJoin()
//        .device("WEB_BROWSER")
//        .userId(UID.next())
//        .name("Mike Bilko")
//        .nodeId(hz.getCluster().getLocalMember().getUuid())
//        .sessionId(UID.next())
//        .state("");
//
//    PresenceOccupant occupant = (PresenceOccupant) map
//        .submitToKey("1", new JoinProcessor().request(addRequest)).get();
////        occupant = (PresenceOccupant) map.submitToKey("1", new JoinProcessor()._request(addRequest)).get();
//    occupant = (PresenceOccupant) map.submitToKey("1", new JoinProcessor().request(addRequest2))
//        .get();
//    occupant = (PresenceOccupant) map.submitToKey("1", new JoinProcessor().request(addRequest3))
//        .get();
//
//    map.submitToKey("1",
//        new ChangeStateProcessor().sessionId(addRequest2.sessionId).state("TYPING")).get();
//
//    Presence presence = map.get("1");
//    System.out.println(presence);
//
//    Try.run(() -> Thread.sleep(500));
//
////        map.submitToKey("1", new LeaveProcessor().sessionId(occupant.id).timestamp(occupant.joined)).get();
//    presence = (Presence) map.submitToKey("1", new GetProcessor()).get();
//    System.out.println(presence);
//
//    Thread.sleep(1000000);
//  }
//
//  private static List<PresenceOccupant> maybeEvict(Presence presence) {
//    return maybeEvict(presence, MAX_OCCUPANTS);
//  }
//
//  private static List<PresenceOccupant> maybeEvict(Presence presence, int maxSize) {
//    if (presence == null) {
//      return null;
//    }
//
//    final List<PresenceOccupant> occupants = presence.occupants;
//    if (occupants == null || occupants.isEmpty()) {
//      return null;
//    }
//
//    final long timestamp = System.currentTimeMillis();
//
//    // TODO: Re-evaluate this
////        if (presence.lastCheck != 0L) {
////            if (presence.lastCheck < (timestamp - OCCUPANT_IDLE_TIMEOUT)) {
////                if (maxSize < occupants.size()) {
////                    final List<PresenceOccupant> evicted = new ArrayList<>(occupants.size() - maxSize);
////                    for (int i = maxSize; i < occupants.size(); i++) {
////                        evicted.add(occupants.get(i));
////                    }
////
////                    final List<PresenceOccupant> newOccupants = new ArrayList<>(maxSize);
////                    newOccupants.addAll(occupants.subList(0, maxSize));
////                    presence.occupants = newOccupants;
////
////                    return evicted;
////                }
////
////                return null;
////            }
////        }
//
//    presence.lastCheck = timestamp;
//
//    final long cutoff = timestamp - OCCUPANT_IDLE_TIMEOUT;
//    List<PresenceOccupant> evicted = null;
//
//    for (int i = 0; i < occupants.size(); i++) {
//      final PresenceOccupant occupant = occupants.get(i);
//
//      if (occupant.lastPing == 0L) {
//        occupant.lastPing = timestamp;
//      } else if (occupant.lastPing < cutoff) {
//        if (evicted == null) {
//          evicted = new ArrayList<>(1);
//        }
//        evicted.add(occupant);
//      }
//    }
//
//    // Were there any deadline evictions?
//    if (evicted == null) {
//      if (maxSize < occupants.size()) {
//        evicted = new ArrayList<>(occupants.size() - maxSize);
//        for (int i = maxSize; i < occupants.size(); i++) {
//          evicted.add(occupants.get(i));
//        }
//
//        final List<PresenceOccupant> newOccupants = new ArrayList<>(maxSize);
//        newOccupants.addAll(occupants.subList(0, maxSize));
//        presence.occupants = newOccupants;
//      }
//
//      return evicted;
//    }
//
//    final List<PresenceOccupant> _evicted = evicted;
//    presence.occupants = occupants.stream()
//        .filter($ -> !_evicted.contains($))
//        .collect(Collectors.toList());
//
//    // Are there still too many occupants?
//    // This shouldn't happen since a single Join operation would only
//    // ever add a single Occupant and since there was at least 1 eviction
//    // already, then this would be impossible.
//    if (maxSize < occupants.size()) {
//      for (int i = maxSize; i < occupants.size(); i++) {
//        evicted.add(occupants.get(i));
//      }
//
//      final List<PresenceOccupant> newOccupants = new ArrayList<>(maxSize);
//      newOccupants.addAll(occupants.subList(0, maxSize));
//      presence.occupants = newOccupants;
//    }
//
//    return evicted;
//  }
//
//  @Override
//  protected void startUp() throws Exception {
//    // Set static reference to EventBus so Hazelcast MapProcessors can access EventBus.
//    BUS = eventBus;
//
//    cleaner = new Cleaner();
//    cleaner.startAsync().awaitRunning();
//
//    // Get Hazelcast map.
//    map = hazelcastProvider.get().getMap(MAP_NAME);
//
//    // Listen to Hazelcast Member Removed events.
//    membershipListenerId = hazelcastProvider.get()
//        .getCluster()
//        .addMembershipListener(new MembershipAdapter() {
//          @Override
//          public void memberRemoved(MembershipEvent membershipEvent) {
//            // Remove all occupants associated to the recently removed node from
//            // all locally owned map entries.
//            map.executeOnKeys(
//                map.localKeySet(),
//                new NodeRemovedProcessor().nodeId(membershipEvent.getMember().getUuid())
//            );
//          }
//        });
//
//    // Handle local eviction.
//    map.addLocalEntryListener((EntryEvictedListener<String, Presence>) event -> {
//      final Presence presence = event.getOldValue();
//
//      if (presence == null) {
//        return;
//      }
//
//      final List<PresenceOccupant> occupants = presence.occupants;
//      if (occupants == null || occupants.isEmpty()) {
//        return;
//      }
//
//      final String key = event.getKey();
//
//      occupants.stream()
//          .collect(Collectors
//              .groupingBy(
//                  $ -> $.nodeId,
//                  Collectors.mapping(
//                      $ -> $.id,
//                      Collectors.toSet())))
//          .forEach((nodeId, sessIds) -> {
//            if (BUS != null) {
//              BUS.publish(
//                  PresenceMessage.REMOVED + nodeId,
//                  WireFormat.byteify(
//                      new PresenceMessage()
//                          .key(key)
//                          .sessionIds(sessIds)
//                  )
//              );
//            }
//          });
//    });
//
//    LOG.info("Started");
//  }
//
//  @Override
//  protected void shutDown() throws Exception {
//    Try.run(() -> cleaner.stopAsync().awaitTerminated(5, TimeUnit.SECONDS));
//
//    // Remove Hazelcast membership listener.
//    Try.run(() -> hazelcastProvider.get()
//        .getCluster()
//        .removeMembershipListener(membershipListenerId));
//  }
//
//  /**
//   * @param key
//   * @return
//   */
//  public Observable<Presence> get(String key) {
//    if (key == null) {
//      return Observable.just(null);
//    }
//    return Observable.create(subscriber -> {
//      map.submitToKey(
//          key,
//          new GetProcessor(),
//          new ExecutionCallback() {
//            @Override
//            public void onResponse(Object response) {
//              if (subscriber.isUnsubscribed()) {
//                return;
//              }
//              subscriber.onNext((Presence) response);
//              subscriber.onCompleted();
//            }
//
//            @Override
//            public void onFailure(Throwable t) {
//              if (subscriber.isUnsubscribed()) {
//                return;
//              }
//              subscriber.onError(t);
//            }
//          });
//    });
//  }
//
////    public Observable<PresenceOccupant> join(String key, RequestContext eventLoop) {
////        if (eventLoop == null
////            || eventLoop.getSessionId() == null
////            || eventLoop.getSessionId().isEmpty()
////            || eventLoop.getUser() == null) {
////            return Observable.just(null);
////        }
////
////        final UserDeviceSessionEntity deviceSession = eventLoop.getDevice();
////
////        final PresenceJoin join = new PresenceJoin()
////            .name(
////                eventLoop.getUser().getContactEntity() != null ?
////                    eventLoop.getUser().getContactEntity().getFormattedName() :
////                    ""
////            )
////            .nodeId(hazelcastProvider.get().getCluster().getLocalMember().getUuid())
////            .sessionId(eventLoop.getSessionId())
////            .device(
////                deviceSession != null && deviceSession.getDeviceType() != null ?
////                    deviceSession.getDeviceType().name() :
////                    ""
////            )
////            .userId(eventLoop.getUser().getEntity().getId());
////
////        return join(key, join);
////    }
//
////    /**
////     * @param key
////     * @param eventLoop
////     * @return
////     */
////    public Observable<Boolean> leave(String key, RequestContext eventLoop) {
////        if (eventLoop == null
////            || eventLoop.getSessionId() == null
////            || eventLoop.getSessionId().isEmpty()
////            || eventLoop.getUser() == null) {
////            return Observable.just(false);
////        }
////
////        enqueueLeave();
////
////        return leave(key, eventLoop.getSessionId());
////    }
//
//  /**
//   * @param key
//   * @param request
//   * @return
//   */
//  public Observable<PresenceOccupant> join(String key, PresenceJoin request) {
//    if (request == null) {
//      return Observable.just(null);
//    }
//    return Observable.create(subscriber -> {
//      map.submitToKey(
//          key,
//          new JoinProcessor()
//              .request(request),
//          new ExecutionCallback() {
//            @Override
//            public void onResponse(Object response) {
//              if (subscriber.isUnsubscribed()) {
//                return;
//              }
//              subscriber.onNext((PresenceOccupant) response);
//              subscriber.onCompleted();
//            }
//
//            @Override
//            public void onFailure(Throwable t) {
//              if (subscriber.isUnsubscribed()) {
//                return;
//              }
//              subscriber.onError(t);
//            }
//          });
//    });
//  }
//
//  public void enqueueLeave(String key, String sessionId, long timestamp) {
//    leaveQueue.add(new LeaveRequest(key, sessionId, timestamp));
//  }
//
//  /**
//   * @param key
//   * @param sessionId
//   * @param state
//   * @return
//   */
//  public Observable<Boolean> changeState(String key,
//      String sessionId,
//      String state) {
//    if (sessionId == null) {
//      return Observable.just(false);
//    }
//
//    return Observable.create(subscriber -> {
//      map.submitToKey(
//          key,
//          new ChangeStateProcessor()
//              .sessionId(sessionId)
//              .state(state),
//          new ExecutionCallback() {
//            @Override
//            public void onResponse(Object response) {
//              if (subscriber.isUnsubscribed()) {
//                return;
//              }
//              subscriber.onNext(response == Boolean.TRUE);
//              subscriber.onCompleted();
//            }
//
//            @Override
//            public void onFailure(Throwable t) {
//              if (subscriber.isUnsubscribed()) {
//                return;
//              }
//              subscriber.onError(t);
//            }
//          });
//    });
//  }
//
//  /**
//   * @param key
//   * @param sessionId
//   * @param timestamp
//   * @return
//   */
//  public Observable<Boolean> ping(String key,
//      String sessionId,
//      long timestamp) {
//    if (sessionId == null) {
//      return Observable.just(false);
//    }
//
//    return Observable.create(subscriber -> {
//      map.submitToKey(
//          key,
//          new PingProcessor()
//              .sessionId(sessionId)
//              .timestamp(timestamp),
//          new ExecutionCallback() {
//            @Override
//            public void onResponse(Object response) {
//              if (subscriber.isUnsubscribed()) {
//                return;
//              }
//              subscriber.onNext(response == Boolean.TRUE);
//              subscriber.onCompleted();
//            }
//
//            @Override
//            public void onFailure(Throwable t) {
//              if (subscriber.isUnsubscribed()) {
//                return;
//              }
//              subscriber.onError(t);
//            }
//          });
//    });
//  }
//
////    /**
////     * @param key
////     * @param message
////     * @return
////     */
////    public Observable<Boolean> push(String sessionId, String key, PushMessage message) {
////        return push(sessionId, key, new PushEnvelope()
////            .address(message.name())
////            .payload(WireFormat.stringify(message)));
////    }
////
////    /**
////     * @param key
////     * @param message
////     * @return
////     */
////    public Observable<Boolean> push(String key, PushMessage message) {
////        return push(null, key, new PushEnvelope()
////            .address(message.name())
////            .payload(WireFormat.stringify(message)));
////    }
////
////    /**
////     * @param eventLoop
////     * @param key
////     * @param message
////     * @return
////     */
////    public Observable<Boolean> push(RequestContext eventLoop, String key, PushMessage message) {
////        return push(eventLoop.getSessionId(), key, new PushEnvelope()
////            .address(message.name())
////            .payload(WireFormat.stringify(message)));
////    }
////
////    /**
////     * @param key
////     * @param message
////     * @return
////     */
////    public Observable<Boolean> push(String sessionId, String key, String contextId, PushMessage message) {
////        return push(sessionId, key, new PushEnvelope()
////            .address(contextId == null ? message.name() : contextId + "|" + message.name())
////            .payload(WireFormat.stringify(message)));
////    }
//
//  /**
//   * @param key
//   * @param pushEnvelope
//   * @return
//   */
//  public Observable<Boolean> push(String sessionId, String key, PushEnvelope pushEnvelope) {
//    return push(sessionId, key, WireFormat.byteify(pushEnvelope));
//  }
//
//  /**
//   * @param key
//   * @param pushEnvelope
//   * @return
//   */
//  public Observable<Boolean> push(String sessionId, String key, byte[] pushEnvelope) {
//    if (pushEnvelope == null || pushEnvelope.length == 0) {
//      return Observable.just(true);
//    }
//    return Observable.create(subscriber -> {
//      map.submitToKey(
//          key,
//          new PushProcessor()
//              .sessionId(sessionId)
//              .pushEnvelope(pushEnvelope),
//          new ExecutionCallback() {
//            @Override
//            public void onResponse(Object response) {
//              if (subscriber.isUnsubscribed()) {
//                return;
//              }
//              subscriber.onNext(response == Boolean.TRUE);
//              subscriber.onCompleted();
//            }
//
//            @Override
//            public void onFailure(Throwable t) {
//              if (subscriber.isUnsubscribed()) {
//                return;
//              }
//              subscriber.onError(t);
//            }
//          });
//    });
//  }
//
//  /**
//   * Retrieves Presence and evicts stale occupants.
//   */
//  protected static class GetProcessor
//      implements DataSerializable, EntryProcessor<String, Presence>,
//      EntryBackupProcessor<String, Presence> {
//
//    private String sessionId;
//    private long timestamp;
//
//    public GetProcessor sessionId(final String sessionId) {
//      this.sessionId = sessionId;
//      return this;
//    }
//
//    public GetProcessor state(final long timestamp) {
//      this.timestamp = timestamp;
//      return this;
//    }
//
//    @Override
//    public void processBackup(Map.Entry<String, Presence> entry) {
//      process(entry);
//    }
//
//    @Override
//    public EntryBackupProcessor<String, Presence> getBackupProcessor() {
//      return this;
//    }
//
//    @Override
//    public Object process(Map.Entry<String, Presence> entry) {
//      final Presence presence = entry.getValue();
//      if (presence == null) {
//        return null;
//      }
//
//      final List<PresenceOccupant> evicted = maybeEvict(presence);
//      final List<PresenceLeave> left = evicted != null && !evicted.isEmpty() ?
//          evicted
//              .stream()
//              .map($ -> new PresenceLeave()
//                  .id($.id)
//                  .userId($.userId))
//              .collect(Collectors.toList()) :
//          null;
//
//      // Notify Removals.
//      if (evicted != null && !evicted.isEmpty()) {
//        evicted.stream()
//            .collect(Collectors
//                .groupingBy(
//                    $ -> $.nodeId,
//                    Collectors.mapping(
//                        $ -> $.id,
//                        Collectors.toSet())))
//            .forEach((nodeId, sessIds) -> {
//              if (BUS != null) {
//                BUS.publish(
//                    PresenceMessage.REMOVED + nodeId,
//                    WireFormat.byteify(
//                        new PresenceMessage()
//                            .key(entry.getKey())
//                            .sessionIds(sessIds)
//                    )
//                );
//              }
//            });
//      }
//
//      // Remove presence if necessary.
//      if (presence.occupants == null || presence.occupants.isEmpty()) {
//        entry.setValue(null);
//        return null;
//      }
//
//      final String sessionId = Strings.nullToEmpty(this.sessionId).trim();
//      if (!sessionId.isEmpty()) {
//        presence.occupants
//            .stream()
//            .filter($ -> sessionId.equals($.id))
//            .findFirst()
//            .ifPresent($ -> $.lastPing = System.currentTimeMillis());
//      }
//
//      // Were there no changes?
//      if (left == null || left.isEmpty()) {
//        return presence;
//      }
//
//      // Update sequence.
//      presence.seq += 1;
//
//      // Create change.
//      final PresenceChange change = new PresenceChange()
//          .key(entry.getKey())
//          .mod(presence.mod)
//          .seq(presence.seq)
//          .left(left);
//
//      // Notify Occupants of change.
//      presence.occupants.stream()
//          .collect(Collectors
//              .groupingBy(
//                  $ -> $.nodeId,
//                  Collectors.mapping(
//                      $ -> $.id,
//                      Collectors.toSet())))
//          .forEach((nodeId, sessIds) -> {
//            if (BUS != null) {
//              BUS.publish(
//                  PresenceMessage.CHANGED + nodeId,
//                  WireFormat.byteify(
//                      new PresenceMessage()
//                          .key(entry.getKey())
//                          .sessionIds(sessIds)
//                          .change(change)
//                  )
//              );
//            }
//          });
//
//      // Update entry.
//      entry.setValue(presence);
//
//      // Return presence.
//      return presence;
//    }
//
//    @Override
//    public void writeData(ObjectDataOutput out) throws IOException {
//      out.writeUTF(sessionId);
//      out.writeLong(timestamp);
//    }
//
//    @Override
//    public void readData(ObjectDataInput in) throws IOException {
//      sessionId = in.readUTF();
//      timestamp = in.readLong();
//    }
//  }
//
//  /**
//   * Adds a single Occupant.
//   * Evicts stale occupants.
//   */
//  protected static class JoinProcessor
//      implements DataSerializable, EntryProcessor<String, Presence>,
//      EntryBackupProcessor<String, Presence> {
//
//    private PresenceJoin request;
//
//    public JoinProcessor request(final PresenceJoin request) {
//      this.request = request;
//      return this;
//    }
//
//    @Override
//    public EntryBackupProcessor<String, Presence> getBackupProcessor() {
//      return this;
//    }
//
//    @Override
//    public void processBackup(Map.Entry<String, Presence> entry) {
//      process(entry);
//    }
//
//    @Override
//    public Object process(Map.Entry<String, Presence> entry) {
//      if (request == null) {
//        return null;
//      }
//
//      final Presence presence = entry.getValue();
//
//      // New Presence?
//      if (presence == null) {
//        final PresenceOccupant occupant = new PresenceOccupant()
//            .id(request.sessionId)
//            .userId(request.userId)
//            .nodeId(request.nodeId)
//            .name(request.name)
//            .imageUrl(request.imageUrl)
//            .joined(System.currentTimeMillis())
//            .device(request.device)
//            .state(request.state)
//            .lastPing(System.currentTimeMillis());
//
//        final Presence newPresence = new Presence()
//            .key(entry.getKey())
//            .mod(UID.next())
//            .seq(0L)
//            .lastCheck(System.currentTimeMillis())
//            .occupants(Collections.singletonList(occupant));
//
//        if (BUS != null) {
//          Try.run(() -> BUS.publish(
//              PresenceMessage.JOINED + occupant.nodeId,
//              WireFormat.byteify(
//                  new PresenceMessage()
//                      .key(entry.getKey())
//                      .sessionIds(Collections.singleton(occupant.id))
//                      .presence(newPresence)
//              )
//          ));
//        }
//
//        entry.setValue(newPresence);
//
//        return occupant;
//      }
//
//      // Maybe evict.
//      final List<PresenceOccupant> evicted = maybeEvict(presence, MAX_OCCUPANTS_JOIN);
//      final List<PresenceLeave> left = evicted != null && !evicted.isEmpty() ?
//          evicted
//              .stream()
//              .map($ -> new PresenceLeave()
//                  .id($.id)
//                  .userId($.userId))
//              .collect(Collectors.toList()) :
//          null;
//
//      // Notify Removals.
//      if (evicted != null && !evicted.isEmpty()) {
//        evicted.stream()
//            .collect(Collectors
//                .groupingBy(
//                    $ -> $.nodeId,
//                    Collectors.mapping(
//                        $ -> $.id,
//                        Collectors.toSet())))
//            .forEach((nodeId, sessIds) -> {
//              if (BUS != null) {
//                BUS.publish(
//                    PresenceMessage.REMOVED + nodeId,
//                    WireFormat.byteify(
//                        new PresenceMessage()
//                            .key(entry.getKey())
//                            .sessionIds(sessIds)
//                    )
//                );
//              }
//            });
//      }
//
//      final PresenceOccupant existingOccupant = presence.occupants != null ?
//          presence.occupants.stream()
//              .filter($ -> $.id.equalsIgnoreCase(request.sessionId))
//              .findFirst()
//              .orElse(null) :
//          null;
//
//      final PresenceOccupant newOccupant = existingOccupant == null ?
//          new PresenceOccupant()
//              .id(request.sessionId)
//              .userId(request.userId)
//              .nodeId(request.nodeId)
//              .name(request.name)
//              .imageUrl(request.imageUrl)
//              .joined(System.currentTimeMillis())
//              .device(request.device)
//              .state(request.state)
//              .lastPing(System.currentTimeMillis()) :
//          null;
//
//      // Create node map before adding the new occupant.
//      final Map<String, Set<String>> nodeMap = presence.occupants != null ?
//          presence.occupants.stream()
//              .collect(Collectors
//                  .groupingBy(
//                      $ -> $.nodeId,
//                      Collectors.mapping(
//                          $ -> $.id,
//                          Collectors.toSet()))) :
//          Collections.emptyMap();
//
//      if (existingOccupant != null) {
//        existingOccupant.lastPing = System.currentTimeMillis();
//      } else {
//        if (evicted == null || evicted.isEmpty()) {
//          if (presence.occupants == null || presence.occupants.isEmpty()) {
//            presence.occupants = Collections.singletonList(newOccupant);
//          } else {
//            final List<PresenceOccupant> occupants = new ArrayList<>(presence.occupants.size() + 1);
//            occupants.addAll(presence.occupants);
//            occupants.add(newOccupant);
//            presence.occupants = occupants;
//          }
//        } else {
//          presence.occupants.add(newOccupant);
//        }
//      }
//
//      if (!nodeMap.isEmpty() && (left != null || newOccupant != null)) {
//        presence.seq += 1;
//
//        // Set value.
//        final PresenceChange change = new PresenceChange()
//            .key(entry.getKey())
//            .mod(presence.mod)
//            .seq(presence.seq)
//            .left(left);
//
//        if (newOccupant != null) {
//          change.joined(Collections.singletonList(newOccupant));
//        }
//
//        nodeMap
//            .forEach((nodeId, sessIds) -> {
//              if (BUS != null) {
//                BUS.publish(
//                    PresenceMessage.CHANGED + nodeId,
//                    WireFormat.byteify(
//                        new PresenceMessage()
//                            .key(entry.getKey())
//                            .sessionIds(sessIds)
//                            .change(change)
//                    )
//                );
//              }
//            });
//      }
//
//      // Always send a JOINED message to the requester.
//      final PresenceOccupant joiner = existingOccupant != null ? existingOccupant : newOccupant;
//      if (BUS != null) {
//        Try.run(() -> BUS.publish(
//            PresenceMessage.JOINED + joiner.nodeId,
//            WireFormat.byteify(
//                new PresenceMessage()
//                    .key(entry.getKey())
//                    .sessionIds(Collections.singleton(joiner.id))
//                    .presence(presence)
//            )
//        ));
//      }
//
//      // Update entry.
//      entry.setValue(presence);
//
//      // Return joiner.
//      return joiner;
//    }
//
//    @Override
//    public void writeData(ObjectDataOutput out) throws IOException {
//      request.writeData(out);
//    }
//
//    @Override
//    public void readData(ObjectDataInput in) throws IOException {
//      request = PresenceJoin.from(in);
//    }
//  }
//
//  /**
//   * Removes a single occupant.
//   * Evicts stale occupants.
//   */
//  protected static class LeaveProcessor
//      extends AbstractEntryProcessor<String, Presence>
//      implements DataSerializable {
//
//    private String sessionId;
//    private long timestamp;
//
//    public LeaveProcessor sessionId(final String sessionId) {
//      this.sessionId = sessionId;
//      return this;
//    }
//
//    public LeaveProcessor timestamp(final long timestamp) {
//      this.timestamp = timestamp;
//      return this;
//    }
//
//    @Override
//    public Object process(Map.Entry<String, Presence> entry) {
//      sessionId = Strings.nullToEmpty(sessionId).trim();
//      if (sessionId.isEmpty()) {
//        return false;
//      }
//
//      final Presence presence = entry.getValue();
//      if (presence == null) {
//        return false;
//      }
//
//      final List<PresenceOccupant> occupants = presence.occupants == null
//          ? Collections.emptyList()
//          : presence.occupants;
//
//      if (occupants.isEmpty()) {
//        entry.setValue(null);
//        return false;
//      }
//
//      final List<PresenceOccupant> evicted = Lists.newArrayListWithCapacity(
//          1
//      );
//      final List<PresenceOccupant> newOccupants = new ArrayList<>(
//          occupants.size() - 1 < 1 ?
//              1 :
//              occupants.size() - 1
//      );
//
//      final long timestamp = System.currentTimeMillis();
//      final long cutoff = timestamp - OCCUPANT_IDLE_TIMEOUT;
//      boolean removed = false;
//      presence.lastCheck = timestamp;
//
//      for (int i = 0; i < occupants.size(); i++) {
//        final PresenceOccupant occupant = occupants.get(i);
//
//        if (occupant.lastPing == 0L) {
//          occupant.lastPing = timestamp;
//          newOccupants.add(occupant);
//        } else if (occupant.lastPing < cutoff) {
//          evicted.add(occupant);
//        } else if (sessionId.equals(occupant.id) && occupant.joined == timestamp) {
//          evicted.add(occupant);
//          removed = true;
//        } else {
//          newOccupants.add(occupant);
//        }
//      }
//
//      if (evicted.isEmpty()) {
//        return false;
//      }
//
//      // Update presence.
//      presence
//          .seq(presence.seq + 1)
//          .occupants(newOccupants);
//
//      if (!evicted.isEmpty()) {
//        evicted.stream()
//            .collect(Collectors
//                .groupingBy(
//                    $ -> $.nodeId,
//                    Collectors.mapping(
//                        $ -> $.id,
//                        Collectors.toSet())))
//            .forEach((nodeId, sessIds) -> {
//              if (BUS != null) {
//                BUS.publish(
//                    PresenceMessage.REMOVED + nodeId,
//                    WireFormat.byteify(
//                        new PresenceMessage()
//                            .key(entry.getKey())
//                            .sessionIds(sessIds)
//                    )
//                );
//              }
//            });
//      }
//
//      // Send change notification to all remaining occupants.
//      if (!newOccupants.isEmpty()) {
//        // Set value.
//        final PresenceChange change = new PresenceChange()
//            .key(entry.getKey())
//            .mod(presence.mod)
//            .seq(presence.seq)
//            .left(evicted
//                .stream()
//                .map($ -> new PresenceLeave()
//                    .id($.id)
//                    .userId($.userId))
//                .collect(Collectors.toList()));
//
//        newOccupants.stream()
//            .collect(Collectors
//                .groupingBy(
//                    $ -> $.nodeId,
//                    Collectors.mapping(
//                        $ -> $.id,
//                        Collectors.toSet())))
//            .forEach((nodeId, sessIds) -> {
//              if (BUS != null) {
//                BUS.publish(
//                    PresenceMessage.CHANGED + nodeId,
//                    WireFormat.byteify(
//                        new PresenceMessage()
//                            .key(entry.getKey())
//                            .sessionIds(sessIds)
//                            .change(change)
//                    )
//                );
//              }
//            });
//
//        entry.setValue(presence);
//      } else {
//        entry.setValue(null);
//      }
//
//      return removed;
//    }
//
//    @Override
//    public void writeData(ObjectDataOutput out) throws IOException {
//      out.writeUTF(sessionId);
//      out.writeLong(timestamp);
//    }
//
//    @Override
//    public void readData(ObjectDataInput in) throws IOException {
//      sessionId = in.readUTF();
//      timestamp = in.readLong();
//    }
//  }
//
//  /**
//   * Changes the state of the occupant associated with a sessionId.
//   * Evicts stale occupants.
//   */
//  protected static class ChangeStateProcessor
//      implements DataSerializable, EntryProcessor<String, Presence>,
//      EntryBackupProcessor<String, Presence> {
//
//    private String sessionId;
//    private String state;
//
//    public ChangeStateProcessor sessionId(final String sessionId) {
//      this.sessionId = sessionId;
//      return this;
//    }
//
//    public ChangeStateProcessor state(final String state) {
//      this.state = state;
//      return this;
//    }
//
//    @Override
//    public void processBackup(Map.Entry<String, Presence> entry) {
//      process(entry);
//    }
//
//    @Override
//    public EntryBackupProcessor<String, Presence> getBackupProcessor() {
//      return this;
//    }
//
//    @Override
//    public Object process(Map.Entry<String, Presence> entry) {
//      final String sessionId = Strings.nullToEmpty(this.sessionId).trim();
//      final String state = Strings.nullToEmpty(this.state).trim();
//
//      final Presence presence = entry.getValue();
//      if (presence == null) {
//        return false;
//      }
//
//      // Maybe evict.
//      final List<PresenceOccupant> evicted = maybeEvict(presence);
//      final List<PresenceLeave> left = evicted != null && !evicted.isEmpty() ?
//          evicted
//              .stream()
//              .map($ -> new PresenceLeave()
//                  .id($.id)
//                  .userId($.userId))
//              .collect(Collectors.toList()) :
//          null;
//
//      // Notify Removals.
//      if (evicted != null && !evicted.isEmpty()) {
//        evicted.stream()
//            .collect(Collectors
//                .groupingBy(
//                    $ -> $.nodeId,
//                    Collectors.mapping(
//                        $ -> $.id,
//                        Collectors.toSet())))
//            .forEach((nodeId, sessIds) -> {
//              if (BUS != null) {
//                BUS.publish(
//                    PresenceMessage.REMOVED + nodeId,
//                    WireFormat.byteify(
//                        new PresenceMessage()
//                            .key(entry.getKey())
//                            .sessionIds(sessIds)
//                    )
//                );
//              }
//            });
//      }
//
//      // Remove presence if necessary.
//      if (presence.occupants == null || presence.occupants.isEmpty()) {
//        entry.setValue(null);
//        return false;
//      }
//
//      // Find occupant.
//      final PresenceOccupant occupant = presence.occupants.stream()
//          .filter($ -> sessionId.equals($.id))
//          .findFirst()
//          .orElse(null);
//
//      final PresenceStateChanged stateChanged;
//      if (occupant == null) {
//        stateChanged = null;
//      } else {
//        occupant.lastPing = System.currentTimeMillis();
//        stateChanged = state.equals(occupant.state) ?
//            new PresenceStateChanged()
//                .id(sessionId)
//                .from(occupant.state)
//                .to(state) :
//            null;
//
//        if (!state.equals(occupant.state)) {
//          occupant.state = state;
//        }
//      }
//
//      if (left == null && stateChanged == null) {
//        return false;
//      }
//
//      // Update sequence.
//      presence.seq += 1;
//
//      final PresenceChange change = new PresenceChange()
//          .key(entry.getKey())
//          .mod(presence.mod)
//          .seq(presence.seq)
//          .left(left);
//      if (stateChanged != null) {
//        change.changed(Collections.singletonList(stateChanged));
//      }
//
//      // Notify Occupants of change.
//      presence.occupants.stream()
//          .collect(Collectors
//              .groupingBy(
//                  $ -> $.nodeId,
//                  Collectors.mapping(
//                      $ -> $.id,
//                      Collectors.toSet())))
//          .forEach((nodeId, sessIds) -> {
//            if (BUS != null) {
//              BUS.publish(
//                  PresenceMessage.CHANGED + nodeId,
//                  WireFormat.byteify(
//                      new PresenceMessage()
//                          .key(entry.getKey())
//                          .sessionIds(sessIds)
//                          .change(change)
//                  )
//              );
//            }
//          });
//
//      // Update entry.
//      entry.setValue(presence);
//
//      // Return success.
//      return true;
//    }
//
//    @Override
//    public void writeData(ObjectDataOutput out) throws IOException {
//      out.writeUTF(sessionId);
//      out.writeUTF(state);
//    }
//
//    @Override
//    public void readData(ObjectDataInput in) throws IOException {
//      sessionId = in.readUTF();
//      state = in.readUTF();
//    }
//  }
//
//  /**
//   * Updates occupant's ping timestamp.
//   * Evicts stale occupants.
//   */
//  protected static class PingProcessor
//      extends AbstractEntryProcessor<String, Presence>
//      implements DataSerializable {
//
//    private String sessionId;
//    private long timestamp;
//
//    public PingProcessor sessionId(final String sessionId) {
//      this.sessionId = sessionId;
//      return this;
//    }
//
//    public PingProcessor timestamp(final long timestamp) {
//      this.timestamp = timestamp;
//      return this;
//    }
//
//    @Override
//    public Object process(Map.Entry<String, Presence> entry) {
//      final Presence presence = entry.getValue();
//      if (presence == null) {
//        return false;
//      }
//
//      final List<PresenceOccupant> evicted = maybeEvict(presence);
//      final List<PresenceLeave> left = evicted != null && !evicted.isEmpty() ?
//          evicted
//              .stream()
//              .map($ -> new PresenceLeave()
//                  .id($.id)
//                  .userId($.userId))
//              .collect(Collectors.toList()) :
//          null;
//
//      // Notify Removals.
//      if (evicted != null && !evicted.isEmpty()) {
//        evicted.stream()
//            .collect(Collectors
//                .groupingBy(
//                    $ -> $.nodeId,
//                    Collectors.mapping(
//                        $ -> $.id,
//                        Collectors.toSet())))
//            .forEach((nodeId, sessIds) -> {
//              if (BUS != null) {
//                BUS.publish(
//                    PresenceMessage.REMOVED + nodeId,
//                    WireFormat.byteify(
//                        new PresenceMessage()
//                            .key(entry.getKey())
//                            .sessionIds(sessIds)
//                    )
//                );
//              }
//            });
//      }
//
//      // Remove presence if necessary.
//      if (presence.occupants == null || presence.occupants.isEmpty()) {
//        entry.setValue(null);
//        return false;
//      }
//
//      final String sessionId = Strings.nullToEmpty(this.sessionId).trim();
//      final PresenceOccupant occupant = presence.occupants
//          .stream()
//          .filter($ -> sessionId.equals($.id) && timestamp == $.joined)
//          .findFirst()
//          .orElse(null);
//
//      if (occupant != null) {
//        occupant.lastPing = System.currentTimeMillis();
//      }
//
//      // Were there no changes?
//      if (left == null || left.isEmpty()) {
//        return occupant != null;
//      }
//
//      // Update sequence.
//      presence.seq += 1;
//
//      // Create change.
//      final PresenceChange change = new PresenceChange()
//          .key(entry.getKey())
//          .mod(presence.mod)
//          .seq(presence.seq)
//          .left(left);
//
//      // Notify Occupants of change.
//      presence.occupants.stream()
//          .collect(Collectors
//              .groupingBy(
//                  $ -> $.nodeId,
//                  Collectors.mapping(
//                      $ -> $.id,
//                      Collectors.toSet())))
//          .forEach((nodeId, sessIds) -> {
//            if (BUS != null) {
//              BUS.publish(
//                  PresenceMessage.CHANGED + nodeId,
//                  WireFormat.byteify(
//                      new PresenceMessage()
//                          .key(entry.getKey())
//                          .sessionIds(sessIds)
//                          .change(change)
//                  )
//              );
//            }
//          });
//
//      // Update entry.
//      entry.setValue(presence);
//
//      // Return presence.
//      return occupant != null;
//    }
//
//    @Override
//    public void writeData(ObjectDataOutput out) throws IOException {
//      out.writeUTF(sessionId);
//      out.writeLong(timestamp);
//    }
//
//    @Override
//    public void readData(ObjectDataInput in) throws IOException {
//      sessionId = in.readUTF();
//      timestamp = in.readLong();
//    }
//  }
//
//  /**
//   * Used when a Node has left the cluster.
//   * Any occupants with the associated "nodeId" will be removed.
//   */
//  protected static class NodeRemovedProcessor
//      extends AbstractEntryProcessor<String, Presence>
//      implements DataSerializable {
//
//    private String nodeId;
//
//    public NodeRemovedProcessor nodeId(final String nodeId) {
//      this.nodeId = nodeId;
//      return this;
//    }
//
//    @Override
//    public Object process(Map.Entry<String, Presence> entry) {
//      nodeId = Strings.nullToEmpty(nodeId).trim();
//
//      final Presence presence = entry.getValue();
//      if (presence == null) {
//        return true;
//      }
//
//      final List<PresenceOccupant> occupants = presence.occupants == null
//          ? Collections.emptyList()
//          : presence.occupants;
//
//      if (occupants.isEmpty()) {
//        entry.setValue(null);
//        return true;
//      }
//
//      final Set<PresenceOccupant> removed = occupants
//          .stream()
//          .filter($ -> nodeId.equals($.nodeId))
//          .collect(Collectors.toSet());
//
//      if (removed.isEmpty()) {
//        return true;
//      }
//
//      final List<PresenceOccupant> newOccupants = occupants
//          .stream()
//          .filter($ -> !nodeId.equals($.nodeId))
//          .collect(Collectors.toList());
//
//      final long seq = presence.seq + 1;
//
//      presence
//          .seq(seq)
//          .occupants(newOccupants);
//
//      if (!removed.isEmpty()) {
//        removed.stream()
//            .collect(Collectors
//                .groupingBy(
//                    $ -> $.nodeId,
//                    Collectors.mapping(
//                        $ -> $.id,
//                        Collectors.toSet())))
//            .forEach((nodeId, sessIds) -> {
//              BUS.publish(
//                  PresenceMessage.REMOVED + nodeId,
//                  WireFormat.byteify(
//                      new PresenceMessage()
//                          .key(entry.getKey())
//                          .sessionIds(sessIds)
//                  )
//              );
//            });
//      }
//
//      // Send change notification to all occupants.
//      if (!newOccupants.isEmpty()) {
//        // Set value.
//        final PresenceChange change = new PresenceChange()
//            .key(entry.getKey())
//            .mod(presence.mod)
//            .seq(seq)
//            .left(removed
//                .stream()
//                .map($ -> new PresenceLeave()
//                    .id($.id)
//                    .userId($.userId))
//                .collect(Collectors.toList()));
//
//        newOccupants.stream()
//            .collect(Collectors
//                .groupingBy(
//                    $ -> $.nodeId,
//                    Collectors.mapping(
//                        $ -> $.id,
//                        Collectors.toSet())))
//            .forEach((nodeId, sessIds) -> {
//              BUS.publish(
//                  PresenceMessage.CHANGED + nodeId,
//                  WireFormat.byteify(
//                      new PresenceMessage()
//                          .key(entry.getKey())
//                          .sessionIds(sessIds)
//                          .change(change)
//                  )
//              );
//            });
//
//        entry.setValue(presence);
//      } else {
//        entry.setValue(null);
//      }
//
//      return true;
//    }
//
//    @Override
//    public void writeData(ObjectDataOutput out) throws IOException {
//      out.writeUTF(nodeId);
//    }
//
//    @Override
//    public void readData(ObjectDataInput in) throws IOException {
//      nodeId = in.readUTF();
//    }
//  }
//
//  /**
//   * Pushes a PushEnvelope to all Occupants.
//   * Evicts stale occupants.
//   */
//  protected static class PushProcessor
//      extends AbstractEntryProcessor<String, Presence>
//      implements DataSerializable {
//
//    private String sessionId;
//    private byte[] pushEnvelope;
//
//    public PushProcessor() {
//    }
//
//    public PushProcessor sessionId(final String sessionId) {
//      this.sessionId = sessionId;
//      return this;
//    }
//
//    public PushProcessor pushEnvelope(final byte[] pushEnvelope) {
//      this.pushEnvelope = pushEnvelope;
//      return this;
//    }
//
//    @Override
//    public Object process(Map.Entry<String, Presence> entry) {
//      final Presence presence = entry.getValue();
//      if (presence == null) {
//        return false;
//      }
//
//      final List<PresenceOccupant> evicted = maybeEvict(presence);
//      final List<PresenceLeave> left = evicted != null && !evicted.isEmpty() ?
//          evicted
//              .stream()
//              .map($ -> new PresenceLeave()
//                  .id($.id)
//                  .userId($.userId))
//              .collect(Collectors.toList()) :
//          null;
//
//      // Notify Removals.
//      if (evicted != null && !evicted.isEmpty()) {
//        evicted.stream()
//            .collect(Collectors
//                .groupingBy(
//                    $ -> $.nodeId,
//                    Collectors.mapping(
//                        $ -> $.id,
//                        Collectors.toSet())))
//            .forEach((nodeId, sessIds) -> {
//              if (BUS != null) {
//                BUS.publish(
//                    PresenceMessage.REMOVED + nodeId,
//                    WireFormat.byteify(
//                        new PresenceMessage()
//                            .key(entry.getKey())
//                            .sessionIds(sessIds)
//                    )
//                );
//              }
//            });
//      }
//
//      // Remove presence if necessary.
//      if (presence.occupants == null || presence.occupants.isEmpty()) {
//        entry.setValue(null);
//        return false;
//      }
//
//      // Ensure permission if necessary.
//      if (sessionId != null
//          && !sessionId.isEmpty()
//          && presence.occupants.stream().noneMatch($ -> sessionId.equals($.id))) {
//        return false;
//      }
//
//      presence.occupants.stream()
//          .collect(Collectors
//              .groupingBy(
//                  $ -> $.nodeId,
//                  Collectors.mapping(
//                      $ -> $.id,
//                      Collectors.toSet())))
//          .forEach((nodeId, sessIds) -> {
//            if (BUS != null) {
//              BUS.publish(
//                  PresenceMessage.PUSH + nodeId,
//                  WireFormat.byteify(
//                      new PresenceMessage()
//                          .key(entry.getKey())
//                          .sessionIds(sessIds)
//                          .pushEnvelope(pushEnvelope)
//                  )
//              );
//            }
//          });
//
//      return true;
//    }
//
//    @Override
//    public void writeData(ObjectDataOutput out) throws IOException {
//      out.writeUTF(sessionId);
//      out.writeByteArray(pushEnvelope);
//    }
//
//    @Override
//    public void readData(ObjectDataInput in) throws IOException {
//      sessionId = Strings.nullToEmpty(in.readUTF());
//      pushEnvelope = in.readByteArray();
//    }
//  }
//
//  /**
//   *
//   */
//  private static final class LeaveRequest {
//
//    public final String key;
//    public final String sessionId;
//    public final long timestamp;
//
//    private Future<Object> future;
//    private boolean success;
//
//    public LeaveRequest(String key, String sessionId, long timestamp) {
//      this.key = key;
//      this.sessionId = sessionId;
//      this.timestamp = timestamp;
//    }
//
//    private void get() {
//      try {
//        future.get();
//      } catch (Throwable e) {
//        e.printStackTrace();
//      }
//    }
//  }
//
//  /**
//   *
//   */
//  private final class Cleaner extends AbstractExecutionThreadService {
//
//    @Override
//    @SuppressWarnings("all")
//    protected void run() throws Exception {
//      final List<LeaveRequest> requests = new ArrayList<>(1000);
//
//      while (isRunning()) {
//        try {
//          final LeaveRequest next = leaveQueue.poll(2, TimeUnit.SECONDS);
//          if (next == null) {
//            continue;
//          }
//
//          requests.add(next);
//          leaveQueue.drainTo(requests, 999);
//
//          requests.forEach(request ->
//              request.future = map.submitToKey(
//                  next.key,
//                  new LeaveProcessor()
//                      .sessionId(next.sessionId)
//                      .timestamp(next.timestamp)
//              )
//          );
//
//          try {
//            join(requests);
//          } finally {
//            requests.clear();
//          }
//        } catch (Throwable e) {
//          LOG.error("Failure occured while waiting for 'leave' responses", e);
//        }
//      }
//    }
//
//    private void join(List<LeaveRequest> requests) {
//      List<LeaveRequest> failed = null;
//
//      for (LeaveRequest request : requests) {
//        try {
//          final Object result = request.future.get(5, TimeUnit.SECONDS);
//        } catch (Throwable e) {
//          LOG.error(
//              "Failed to 'leave' for key [" + request.key + "'] and session ['" + request.sessionId
//                  + "']", e);
//          request.future = null;
//
//          if (failed == null) {
//            failed = new ArrayList<>();
//          }
//          failed.add(request);
//        }
//      }
//
//      if (failed != null) {
//        for (int i = failed.size() - 1; i > -1; i--) {
//          leaveQueue.addFirst(failed.get(i));
//        }
//      }
//    }
//  }
//}
