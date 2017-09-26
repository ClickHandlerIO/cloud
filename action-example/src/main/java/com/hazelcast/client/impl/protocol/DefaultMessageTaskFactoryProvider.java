//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.hazelcast.client.impl.protocol;

//import com.hazelcast.client.impl.protocol.codec.SetRemoveListenerCodec.RequestParameters;
//import com.hazelcast.client.impl.protocol.task.AddDistributedObjectListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.AddMembershipListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.AddPartitionLostListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.AuthenticationCustomCredentialsMessageTask;
//import com.hazelcast.client.impl.protocol.task.AuthenticationMessageTask;
//import com.hazelcast.client.impl.protocol.task.ClientStatisticsMessageTask;
//import com.hazelcast.client.impl.protocol.task.CreateProxyMessageTask;
//import com.hazelcast.client.impl.protocol.task.DeployClassesMessageTask;
//import com.hazelcast.client.impl.protocol.task.DestroyProxyMessageTask;
//import com.hazelcast.client.impl.protocol.task.GetDistributedObjectsMessageTask;
//import com.hazelcast.client.impl.protocol.task.GetPartitionsMessageTask;
//import com.hazelcast.client.impl.protocol.task.MessageTask;
//import com.hazelcast.client.impl.protocol.task.PingMessageTask;
//import com.hazelcast.client.impl.protocol.task.RemoveAllListenersMessageTask;
//import com.hazelcast.client.impl.protocol.task.RemoveDistributedObjectListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.RemovePartitionLostListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomiclong.AtomicLongAddAndGetMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomiclong.AtomicLongAlterAndGetMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomiclong.AtomicLongAlterMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomiclong.AtomicLongApplyMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomiclong.AtomicLongCompareAndSetMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomiclong.AtomicLongDecrementAndGetMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomiclong.AtomicLongGetAndAddMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomiclong.AtomicLongGetAndAlterMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomiclong.AtomicLongGetAndIncrementMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomiclong.AtomicLongGetAndSetMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomiclong.AtomicLongGetMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomiclong.AtomicLongIncrementAndGetMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomiclong.AtomicLongSetMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomicreference.AtomicReferenceAlterAndGetMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomicreference.AtomicReferenceAlterMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomicreference.AtomicReferenceApplyMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomicreference.AtomicReferenceClearMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomicreference.AtomicReferenceCompareAndSetMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomicreference.AtomicReferenceContainsMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomicreference.AtomicReferenceGetAndAlterMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomicreference.AtomicReferenceGetAndSetMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomicreference.AtomicReferenceGetMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomicreference.AtomicReferenceIsNullMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomicreference.AtomicReferenceSetAndGetMessageTask;
//import com.hazelcast.client.impl.protocol.task.atomicreference.AtomicReferenceSetMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheAddEntryListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheAddNearCacheInvalidationListenerTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheAddPartitionLostListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheAssignAndGetUuidsMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheClearMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheContainsKeyMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheCreateConfigMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheDestroyMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheEntryProcessorMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheEventJournalReadTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheEventJournalSubscribeTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheFetchNearCacheInvalidationMetadataTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheGetAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheGetAndRemoveMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheGetAndReplaceMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheGetConfigMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheGetMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheIterateEntriesMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheIterateMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheListenerRegistrationMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheLoadAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheManagementConfigMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CachePutAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CachePutIfAbsentMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CachePutMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheRemoveAllKeysMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheRemoveAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheRemoveEntryListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheRemoveInvalidationListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheRemoveMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheRemovePartitionLostListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheReplaceMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.CacheSizeMessageTask;
//import com.hazelcast.client.impl.protocol.task.cache.Pre38CacheAddInvalidationListenerTask;
//import com.hazelcast.client.impl.protocol.task.cardinality.CardinalityEstimatorAddMessageTask;
//import com.hazelcast.client.impl.protocol.task.cardinality.CardinalityEstimatorEstimateMessageTask;
//import com.hazelcast.client.impl.protocol.task.condition.ConditionAwaitMessageTask;
//import com.hazelcast.client.impl.protocol.task.condition.ConditionBeforeAwaitMessageTask;
//import com.hazelcast.client.impl.protocol.task.condition.ConditionSignalAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.condition.ConditionSignalMessageTask;
//import com.hazelcast.client.impl.protocol.task.countdownlatch.CountDownLatchAwaitMessageTask;
//import com.hazelcast.client.impl.protocol.task.countdownlatch.CountDownLatchCountDownMessageTask;
//import com.hazelcast.client.impl.protocol.task.countdownlatch.CountDownLatchGetCountMessageTask;
//import com.hazelcast.client.impl.protocol.task.countdownlatch.CountDownLatchTrySetCountMessageTask;
//import com.hazelcast.client.impl.protocol.task.dynamicconfig.AddCacheConfigMessageTask;
//import com.hazelcast.client.impl.protocol.task.dynamicconfig.AddCardinalityEstimatorConfigMessageTask;
//import com.hazelcast.client.impl.protocol.task.dynamicconfig.AddDurableExecutorConfigMessageTask;
//import com.hazelcast.client.impl.protocol.task.dynamicconfig.AddExecutorConfigMessageTask;
//import com.hazelcast.client.impl.protocol.task.dynamicconfig.AddListConfigMessageTask;
//import com.hazelcast.client.impl.protocol.task.dynamicconfig.AddLockConfigMessageTask;
//import com.hazelcast.client.impl.protocol.task.dynamicconfig.AddMapConfigMessageTask;
//import com.hazelcast.client.impl.protocol.task.dynamicconfig.AddMultiMapConfigMessageTask;
//import com.hazelcast.client.impl.protocol.task.dynamicconfig.AddQueueConfigMessageTask;
//import com.hazelcast.client.impl.protocol.task.dynamicconfig.AddReliableTopicConfigMessageTask;
//import com.hazelcast.client.impl.protocol.task.dynamicconfig.AddReplicatedMapConfigMessageTask;
//import com.hazelcast.client.impl.protocol.task.dynamicconfig.AddRingbufferConfigMessageTask;
//import com.hazelcast.client.impl.protocol.task.dynamicconfig.AddScheduledExecutorConfigMessageTask;
//import com.hazelcast.client.impl.protocol.task.dynamicconfig.AddSemaphoreConfigMessageTask;
//import com.hazelcast.client.impl.protocol.task.dynamicconfig.AddSetConfigMessageTask;
//import com.hazelcast.client.impl.protocol.task.dynamicconfig.AddTopicConfigMessageTask;
//import com.hazelcast.client.impl.protocol.task.executorservice.ExecutorServiceCancelOnAddressMessageTask;
//import com.hazelcast.client.impl.protocol.task.executorservice.ExecutorServiceCancelOnPartitionMessageTask;
//import com.hazelcast.client.impl.protocol.task.executorservice.ExecutorServiceIsShutdownMessageTask;
//import com.hazelcast.client.impl.protocol.task.executorservice.ExecutorServiceShutdownMessageTask;
//import com.hazelcast.client.impl.protocol.task.executorservice.ExecutorServiceSubmitToAddressMessageTask;
//import com.hazelcast.client.impl.protocol.task.executorservice.ExecutorServiceSubmitToPartitionMessageTask;
//import com.hazelcast.client.impl.protocol.task.executorservice.durable.DurableExecutorDisposeResultMessageTask;
//import com.hazelcast.client.impl.protocol.task.executorservice.durable.DurableExecutorIsShutdownMessageTask;
//import com.hazelcast.client.impl.protocol.task.executorservice.durable.DurableExecutorRetrieveAndDisposeResultMessageTask;
//import com.hazelcast.client.impl.protocol.task.executorservice.durable.DurableExecutorRetrieveResultMessageTask;
//import com.hazelcast.client.impl.protocol.task.executorservice.durable.DurableExecutorShutdownMessageTask;
//import com.hazelcast.client.impl.protocol.task.executorservice.durable.DurableExecutorSubmitToPartitionMessageTask;
//import com.hazelcast.client.impl.protocol.task.list.ListAddAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.list.ListAddAllWithIndexMessageTask;
//import com.hazelcast.client.impl.protocol.task.list.ListAddListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.list.ListAddMessageTask;
//import com.hazelcast.client.impl.protocol.task.list.ListAddWithIndexMessageTask;
//import com.hazelcast.client.impl.protocol.task.list.ListClearMessageTask;
//import com.hazelcast.client.impl.protocol.task.list.ListCompareAndRemoveAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.list.ListCompareAndRetainAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.list.ListContainsAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.list.ListContainsMessageTask;
//import com.hazelcast.client.impl.protocol.task.list.ListGetAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.list.ListGetMessageTask;
//import com.hazelcast.client.impl.protocol.task.list.ListIndexOfMessageTask;
//import com.hazelcast.client.impl.protocol.task.list.ListIsEmptyMessageTask;
//import com.hazelcast.client.impl.protocol.task.list.ListIteratorMessageTask;
//import com.hazelcast.client.impl.protocol.task.list.ListLastIndexOfMessageTask;
//import com.hazelcast.client.impl.protocol.task.list.ListListIteratorMessageTask;
//import com.hazelcast.client.impl.protocol.task.list.ListRemoveListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.list.ListRemoveMessageTask;
//import com.hazelcast.client.impl.protocol.task.list.ListRemoveWithIndexMessageTask;
//import com.hazelcast.client.impl.protocol.task.list.ListSetMessageTask;
//import com.hazelcast.client.impl.protocol.task.list.ListSizeMessageTask;
//import com.hazelcast.client.impl.protocol.task.list.ListSubMessageTask;
//import com.hazelcast.client.impl.protocol.task.lock.LockForceUnlockMessageTask;
//import com.hazelcast.client.impl.protocol.task.lock.LockGetLockCountMessageTask;
//import com.hazelcast.client.impl.protocol.task.lock.LockGetRemainingLeaseTimeMessageTask;
//import com.hazelcast.client.impl.protocol.task.lock.LockIsLockedByCurrentThreadMessageTask;
//import com.hazelcast.client.impl.protocol.task.lock.LockIsLockedMessageTask;
//import com.hazelcast.client.impl.protocol.task.lock.LockLockMessageTask;
//import com.hazelcast.client.impl.protocol.task.lock.LockTryLockMessageTask;
//import com.hazelcast.client.impl.protocol.task.lock.LockUnlockMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapAddEntryListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapAddEntryListenerToKeyMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapAddEntryListenerToKeyWithPredicateMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapAddEntryListenerWithPredicateMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapAddIndexMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapAddInterceptorMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapAddListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapAddNearCacheInvalidationListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapAddPartitionLostListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapAggregateMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapAggregateWithPredicateMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapAssignAndGetUuidsMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapClearMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapClearNearCacheMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapContainsKeyMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapContainsValueMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapDeleteMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapDestroyCacheMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapEntriesWithPagingPredicateMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapEntriesWithPredicateMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapEntrySetMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapEventJournalReadTask;
//import com.hazelcast.client.impl.protocol.task.map.MapEventJournalSubscribeTask;
//import com.hazelcast.client.impl.protocol.task.map.MapEvictAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapEvictMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapExecuteOnAllKeysMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapExecuteOnKeyMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapExecuteOnKeysMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapExecuteWithPredicateMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapFetchEntriesMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapFetchKeysMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapFetchNearCacheInvalidationMetadataTask;
//import com.hazelcast.client.impl.protocol.task.map.MapFetchWithQueryMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapFlushMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapForceUnlockMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapGetAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapGetEntryViewMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapGetMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapIsEmptyMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapIsLockedMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapKeySetMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapKeySetWithPagingPredicateMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapKeySetWithPredicateMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapLoadAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapLoadGivenKeysMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapLockMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapMadePublishableMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapProjectionMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapProjectionWithPredicateMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapPublisherCreateMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapPublisherCreateWithValueMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapPutAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapPutIfAbsentMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapPutMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapPutTransientMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapRemoveAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapRemoveEntryListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapRemoveIfSameMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapRemoveInterceptorMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapRemoveMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapRemovePartitionLostListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapReplaceIfSameMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapReplaceMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapSetMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapSetReadCursorMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapSizeMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapSubmitToKeyMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapTryLockMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapTryPutMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapTryRemoveMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapUnlockMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapValuesMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapValuesWithPagingPredicateMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.MapValuesWithPredicateMessageTask;
//import com.hazelcast.client.impl.protocol.task.map.Pre38MapAddNearCacheEntryListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.mapreduce.MapReduceCancelMessageTask;
//import com.hazelcast.client.impl.protocol.task.mapreduce.MapReduceForCustomMessageTask;
//import com.hazelcast.client.impl.protocol.task.mapreduce.MapReduceForListMessageTask;
//import com.hazelcast.client.impl.protocol.task.mapreduce.MapReduceForMapMessageTask;
//import com.hazelcast.client.impl.protocol.task.mapreduce.MapReduceForMultiMapMessageTask;
//import com.hazelcast.client.impl.protocol.task.mapreduce.MapReduceForSetMessageTask;
//import com.hazelcast.client.impl.protocol.task.mapreduce.MapReduceJobProcessInformationMessageTask;
//import com.hazelcast.client.impl.protocol.task.multimap.MultiMapAddEntryListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.multimap.MultiMapAddEntryListenerToKeyMessageTask;
//import com.hazelcast.client.impl.protocol.task.multimap.MultiMapClearMessageTask;
//import com.hazelcast.client.impl.protocol.task.multimap.MultiMapContainsEntryMessageTask;
//import com.hazelcast.client.impl.protocol.task.multimap.MultiMapContainsKeyMessageTask;
//import com.hazelcast.client.impl.protocol.task.multimap.MultiMapContainsValueMessageTask;
//import com.hazelcast.client.impl.protocol.task.multimap.MultiMapEntrySetMessageTask;
//import com.hazelcast.client.impl.protocol.task.multimap.MultiMapForceUnlockMessageTask;
//import com.hazelcast.client.impl.protocol.task.multimap.MultiMapGetMessageTask;
//import com.hazelcast.client.impl.protocol.task.multimap.MultiMapIsLockedMessageTask;
//import com.hazelcast.client.impl.protocol.task.multimap.MultiMapKeySetMessageTask;
//import com.hazelcast.client.impl.protocol.task.multimap.MultiMapLockMessageTask;
//import com.hazelcast.client.impl.protocol.task.multimap.MultiMapPutMessageTask;
//import com.hazelcast.client.impl.protocol.task.multimap.MultiMapRemoveEntryListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.multimap.MultiMapRemoveEntryMessageTask;
//import com.hazelcast.client.impl.protocol.task.multimap.MultiMapRemoveMessageTask;
//import com.hazelcast.client.impl.protocol.task.multimap.MultiMapSizeMessageTask;
//import com.hazelcast.client.impl.protocol.task.multimap.MultiMapTryLockMessageTask;
//import com.hazelcast.client.impl.protocol.task.multimap.MultiMapUnlockMessageTask;
//import com.hazelcast.client.impl.protocol.task.multimap.MultiMapValueCountMessageTask;
//import com.hazelcast.client.impl.protocol.task.multimap.MultiMapValuesMessageTask;
//import com.hazelcast.client.impl.protocol.task.queue.QueueAddAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.queue.QueueAddListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.queue.QueueClearMessageTask;
//import com.hazelcast.client.impl.protocol.task.queue.QueueCompareAndRemoveAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.queue.QueueCompareAndRetainAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.queue.QueueContainsAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.queue.QueueContainsMessageTask;
//import com.hazelcast.client.impl.protocol.task.queue.QueueDrainMaxSizeMessageTask;
//import com.hazelcast.client.impl.protocol.task.queue.QueueDrainMessageTask;
//import com.hazelcast.client.impl.protocol.task.queue.QueueIsEmptyMessageTask;
//import com.hazelcast.client.impl.protocol.task.queue.QueueIteratorMessageTask;
//import com.hazelcast.client.impl.protocol.task.queue.QueueOfferMessageTask;
//import com.hazelcast.client.impl.protocol.task.queue.QueuePeekMessageTask;
//import com.hazelcast.client.impl.protocol.task.queue.QueuePollMessageTask;
//import com.hazelcast.client.impl.protocol.task.queue.QueuePutMessageTask;
//import com.hazelcast.client.impl.protocol.task.queue.QueueRemainingCapacityMessageTask;
//import com.hazelcast.client.impl.protocol.task.queue.QueueRemoveListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.queue.QueueRemoveMessageTask;
//import com.hazelcast.client.impl.protocol.task.queue.QueueSizeMessageTask;
//import com.hazelcast.client.impl.protocol.task.queue.QueueTakeMessageTask;
//import com.hazelcast.client.impl.protocol.task.replicatedmap.ReplicatedMapAddEntryListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.replicatedmap.ReplicatedMapAddEntryListenerToKeyMessageTask;
//import com.hazelcast.client.impl.protocol.task.replicatedmap.ReplicatedMapAddEntryListenerToKeyWithPredicateMessageTask;
//import com.hazelcast.client.impl.protocol.task.replicatedmap.ReplicatedMapAddEntryListenerWithPredicateMessageTask;
//import com.hazelcast.client.impl.protocol.task.replicatedmap.ReplicatedMapAddNearCacheListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.replicatedmap.ReplicatedMapClearMessageTask;
//import com.hazelcast.client.impl.protocol.task.replicatedmap.ReplicatedMapContainsKeyMessageTask;
//import com.hazelcast.client.impl.protocol.task.replicatedmap.ReplicatedMapContainsValueMessageTask;
//import com.hazelcast.client.impl.protocol.task.replicatedmap.ReplicatedMapEntrySetMessageTask;
//import com.hazelcast.client.impl.protocol.task.replicatedmap.ReplicatedMapGetMessageTask;
//import com.hazelcast.client.impl.protocol.task.replicatedmap.ReplicatedMapIsEmptyMessageTask;
//import com.hazelcast.client.impl.protocol.task.replicatedmap.ReplicatedMapKeySetMessageTask;
//import com.hazelcast.client.impl.protocol.task.replicatedmap.ReplicatedMapPutAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.replicatedmap.ReplicatedMapPutMessageTask;
//import com.hazelcast.client.impl.protocol.task.replicatedmap.ReplicatedMapRemoveEntryListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.replicatedmap.ReplicatedMapRemoveMessageTask;
//import com.hazelcast.client.impl.protocol.task.replicatedmap.ReplicatedMapSizeMessageTask;
//import com.hazelcast.client.impl.protocol.task.replicatedmap.ReplicatedMapValuesMessageTask;
//import com.hazelcast.client.impl.protocol.task.ringbuffer.RingbufferAddAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.ringbuffer.RingbufferAddMessageTask;
//import com.hazelcast.client.impl.protocol.task.ringbuffer.RingbufferCapacityMessageTask;
//import com.hazelcast.client.impl.protocol.task.ringbuffer.RingbufferHeadSequenceMessageTask;
//import com.hazelcast.client.impl.protocol.task.ringbuffer.RingbufferReadManyMessageTask;
//import com.hazelcast.client.impl.protocol.task.ringbuffer.RingbufferReadOneMessageTask;
//import com.hazelcast.client.impl.protocol.task.ringbuffer.RingbufferRemainingCapacityMessageTask;
//import com.hazelcast.client.impl.protocol.task.ringbuffer.RingbufferSizeMessageTask;
//import com.hazelcast.client.impl.protocol.task.ringbuffer.RingbufferTailSequenceMessageTask;
//import com.hazelcast.client.impl.protocol.task.scheduledexecutor.ScheduledExecutorGetAllScheduledMessageTask;
//import com.hazelcast.client.impl.protocol.task.scheduledexecutor.ScheduledExecutorShutdownMessageTask;
//import com.hazelcast.client.impl.protocol.task.scheduledexecutor.ScheduledExecutorSubmitToAddressMessageTask;
//import com.hazelcast.client.impl.protocol.task.scheduledexecutor.ScheduledExecutorSubmitToPartitionMessageTask;
//import com.hazelcast.client.impl.protocol.task.scheduledexecutor.ScheduledExecutorTaskCancelFromAddressMessageTask;
//import com.hazelcast.client.impl.protocol.task.scheduledexecutor.ScheduledExecutorTaskCancelFromPartitionMessageTask;
//import com.hazelcast.client.impl.protocol.task.scheduledexecutor.ScheduledExecutorTaskDisposeFromAddressMessageTask;
//import com.hazelcast.client.impl.protocol.task.scheduledexecutor.ScheduledExecutorTaskDisposeFromPartitionMessageTask;
//import com.hazelcast.client.impl.protocol.task.scheduledexecutor.ScheduledExecutorTaskGetDelayFromAddressMessageTask;
//import com.hazelcast.client.impl.protocol.task.scheduledexecutor.ScheduledExecutorTaskGetDelayFromPartitionMessageTask;
//import com.hazelcast.client.impl.protocol.task.scheduledexecutor.ScheduledExecutorTaskGetResultFromAddressMessageTask;
//import com.hazelcast.client.impl.protocol.task.scheduledexecutor.ScheduledExecutorTaskGetResultFromPartitionMessageTask;
//import com.hazelcast.client.impl.protocol.task.scheduledexecutor.ScheduledExecutorTaskGetStatisticsFromAddressMessageTask;
//import com.hazelcast.client.impl.protocol.task.scheduledexecutor.ScheduledExecutorTaskGetStatisticsFromPartitionMessageTask;
//import com.hazelcast.client.impl.protocol.task.scheduledexecutor.ScheduledExecutorTaskIsCancelledFromAddressMessageTask;
//import com.hazelcast.client.impl.protocol.task.scheduledexecutor.ScheduledExecutorTaskIsCancelledFromPartitionMessageTask;
//import com.hazelcast.client.impl.protocol.task.scheduledexecutor.ScheduledExecutorTaskIsDoneFromAddressMessageTask;
//import com.hazelcast.client.impl.protocol.task.scheduledexecutor.ScheduledExecutorTaskIsDoneFromPartitionMessageTask;
//import com.hazelcast.client.impl.protocol.task.semaphore.SemaphoreAcquireMessageTask;
//import com.hazelcast.client.impl.protocol.task.semaphore.SemaphoreAvailablePermitsMessageTasks;
//import com.hazelcast.client.impl.protocol.task.semaphore.SemaphoreDrainPermitsMessageTask;
//import com.hazelcast.client.impl.protocol.task.semaphore.SemaphoreInitMessageTask;
//import com.hazelcast.client.impl.protocol.task.semaphore.SemaphoreReducePermitsMessageTask;
//import com.hazelcast.client.impl.protocol.task.semaphore.SemaphoreReleaseMessageTask;
//import com.hazelcast.client.impl.protocol.task.semaphore.SemaphoreTryAcquireMessageTask;
//import com.hazelcast.client.impl.protocol.task.set.SetAddAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.set.SetAddListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.set.SetAddMessageTask;
//import com.hazelcast.client.impl.protocol.task.set.SetClearMessageTask;
//import com.hazelcast.client.impl.protocol.task.set.SetCompareAndRemoveAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.set.SetCompareAndRetainAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.set.SetContainsAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.set.SetContainsMessageTask;
//import com.hazelcast.client.impl.protocol.task.set.SetGetAllMessageTask;
//import com.hazelcast.client.impl.protocol.task.set.SetIsEmptyMessageTask;
//import com.hazelcast.client.impl.protocol.task.set.SetRemoveListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.set.SetRemoveMessageTask;
//import com.hazelcast.client.impl.protocol.task.set.SetSizeMessageTask;
//import com.hazelcast.client.impl.protocol.task.topic.TopicAddMessageListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.topic.TopicPublishMessageTask;
//import com.hazelcast.client.impl.protocol.task.topic.TopicRemoveMessageListenerMessageTask;
//import com.hazelcast.client.impl.protocol.task.transaction.TransactionCommitMessageTask;
//import com.hazelcast.client.impl.protocol.task.transaction.TransactionCreateMessageTask;
//import com.hazelcast.client.impl.protocol.task.transaction.TransactionRollbackMessageTask;
//import com.hazelcast.client.impl.protocol.task.transaction.XAClearRemoteTransactionMessageTask;
//import com.hazelcast.client.impl.protocol.task.transaction.XACollectTransactionsMessageTask;
//import com.hazelcast.client.impl.protocol.task.transaction.XAFinalizeTransactionMessageTask;
//import com.hazelcast.client.impl.protocol.task.transaction.XATransactionCommitMessageTask;
//import com.hazelcast.client.impl.protocol.task.transaction.XATransactionCreateMessageTask;
//import com.hazelcast.client.impl.protocol.task.transaction.XATransactionPrepareMessageTask;
//import com.hazelcast.client.impl.protocol.task.transaction.XATransactionRollbackMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionallist.TransactionalListAddMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionallist.TransactionalListRemoveMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionallist.TransactionalListSizeMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalmap.TransactionalMapContainsKeyMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalmap.TransactionalMapDeleteMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalmap.TransactionalMapGetForUpdateMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalmap.TransactionalMapGetMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalmap.TransactionalMapIsEmptyMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalmap.TransactionalMapKeySetMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalmap.TransactionalMapKeySetWithPredicateMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalmap.TransactionalMapPutIfAbsentMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalmap.TransactionalMapPutMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalmap.TransactionalMapRemoveIfSameMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalmap.TransactionalMapRemoveMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalmap.TransactionalMapReplaceIfSameMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalmap.TransactionalMapReplaceMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalmap.TransactionalMapSetMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalmap.TransactionalMapSizeMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalmap.TransactionalMapValuesMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalmap.TransactionalMapValuesWithPredicateMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalmultimap.TransactionalMultiMapGetMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalmultimap.TransactionalMultiMapPutMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalmultimap.TransactionalMultiMapRemoveEntryMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalmultimap.TransactionalMultiMapRemoveMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalmultimap.TransactionalMultiMapSizeMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalmultimap.TransactionalMultiMapValueCountMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalqueue.TransactionalQueueOfferMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalqueue.TransactionalQueuePeekMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalqueue.TransactionalQueuePollMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalqueue.TransactionalQueueSizeMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalqueue.TransactionalQueueTakeMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalset.TransactionalSetAddMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalset.TransactionalSetRemoveMessageTask;
//import com.hazelcast.client.impl.protocol.task.transactionalset.TransactionalSetSizeMessageTask;
//
//import com.hazelcast.nio.Connection;

import com.hazelcast.instance.Node;
import com.hazelcast.spi.NodeEngine;
import com.hazelcast.spi.impl.NodeEngineImpl;

public class DefaultMessageTaskFactoryProvider implements MessageTaskFactoryProvider {
  private final MessageTaskFactory[] factories = new MessageTaskFactory[32767];
  private final Node node;

  public DefaultMessageTaskFactoryProvider(NodeEngine nodeEngine) {
    this.node = ((NodeEngineImpl)nodeEngine).getNode();
//    this.initFactories();
  }

//  public void initFactories() {
//    this.factories[RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new SetRemoveListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.SetClearCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new SetClearMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.SetCompareAndRemoveAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new SetCompareAndRemoveAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.SetContainsAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new SetContainsAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.SetIsEmptyCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new SetIsEmptyMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.SetAddAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new SetAddAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.SetAddCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new SetAddMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.SetCompareAndRetainAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new SetCompareAndRetainAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.SetGetAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new SetGetAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.SetRemoveCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new SetRemoveMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.SetAddListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new SetAddListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.SetContainsCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new SetContainsMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.SetSizeCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new SetSizeMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.RingbufferReadOneCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new RingbufferReadOneMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.RingbufferAddAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new RingbufferAddAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.RingbufferCapacityCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new RingbufferCapacityMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.RingbufferTailSequenceCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new RingbufferTailSequenceMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.RingbufferAddCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new RingbufferAddMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.RingbufferRemainingCapacityCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new RingbufferRemainingCapacityMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.RingbufferReadManyCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new RingbufferReadManyMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.RingbufferHeadSequenceCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new RingbufferHeadSequenceMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.RingbufferSizeCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new RingbufferSizeMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.LockUnlockCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new LockUnlockMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.LockIsLockedCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new LockIsLockedMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.LockForceUnlockCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new LockForceUnlockMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.LockGetRemainingLeaseTimeCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new LockGetRemainingLeaseTimeMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.LockIsLockedByCurrentThreadCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new LockIsLockedByCurrentThreadMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.LockLockCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new LockLockMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.LockTryLockCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new LockTryLockMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.LockGetLockCountCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new LockGetLockCountMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheClearCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheClearMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheAssignAndGetUuidsCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheAssignAndGetUuidsMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheFetchNearCacheInvalidationMetadataCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheFetchNearCacheInvalidationMetadataTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheReplaceCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheReplaceMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheContainsKeyCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheContainsKeyMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheCreateConfigCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheCreateConfigMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheGetAndReplaceCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheGetAndReplaceMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheGetAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheGetAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CachePutCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CachePutMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheAddInvalidationListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new Pre38CacheAddInvalidationListenerTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheAddNearCacheInvalidationListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheAddNearCacheInvalidationListenerTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CachePutAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CachePutAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheLoadAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheLoadAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheListenerRegistrationCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheListenerRegistrationMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheAddEntryListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheAddEntryListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheRemoveEntryListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheRemoveEntryListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheRemoveInvalidationListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheRemoveInvalidationListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheDestroyCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheDestroyMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheRemoveCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheRemoveMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheEntryProcessorCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheEntryProcessorMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheGetAndRemoveCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheGetAndRemoveMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheManagementConfigCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheManagementConfigMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CachePutIfAbsentCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CachePutIfAbsentMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheRemoveAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheRemoveAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheRemoveAllKeysCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheRemoveAllKeysMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheIterateCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheIterateMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheAddPartitionLostListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheAddPartitionLostListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheGetConfigCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheGetConfigMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheGetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheGetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheRemovePartitionLostListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheRemovePartitionLostListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheSizeCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheSizeMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheIterateEntriesCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheIterateEntriesMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheEventJournalSubscribeCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheEventJournalSubscribeTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CacheEventJournalReadCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CacheEventJournalReadTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapReduceJobProcessInformationCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapReduceJobProcessInformationMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapReduceCancelCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapReduceCancelMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapReduceForCustomCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapReduceForCustomMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapReduceForMapCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapReduceForMapMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapReduceForListCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapReduceForListMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapReduceForSetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapReduceForSetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapReduceForMultiMapCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapReduceForMultiMapMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ReplicatedMapSizeCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ReplicatedMapSizeMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ReplicatedMapRemoveEntryListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ReplicatedMapRemoveEntryListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ReplicatedMapAddEntryListenerToKeyWithPredicateCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ReplicatedMapAddEntryListenerToKeyWithPredicateMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ReplicatedMapIsEmptyCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ReplicatedMapIsEmptyMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ReplicatedMapPutAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ReplicatedMapPutAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ReplicatedMapContainsKeyCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ReplicatedMapContainsKeyMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ReplicatedMapContainsValueCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ReplicatedMapContainsValueMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ReplicatedMapAddNearCacheEntryListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ReplicatedMapAddNearCacheListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ReplicatedMapGetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ReplicatedMapGetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ReplicatedMapAddEntryListenerWithPredicateCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ReplicatedMapAddEntryListenerWithPredicateMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ReplicatedMapAddEntryListenerToKeyCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ReplicatedMapAddEntryListenerToKeyMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ReplicatedMapRemoveCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ReplicatedMapRemoveMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ReplicatedMapClearCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ReplicatedMapClearMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ReplicatedMapValuesCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ReplicatedMapValuesMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ReplicatedMapEntrySetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ReplicatedMapEntrySetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ReplicatedMapPutCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ReplicatedMapPutMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ReplicatedMapAddEntryListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ReplicatedMapAddEntryListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ReplicatedMapKeySetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ReplicatedMapKeySetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicLongApplyCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicLongApplyMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicLongDecrementAndGetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicLongDecrementAndGetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicLongGetAndAddCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicLongGetAndAddMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicLongAlterAndGetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicLongAlterAndGetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicLongAddAndGetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicLongAddAndGetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicLongGetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicLongGetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicLongCompareAndSetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicLongCompareAndSetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicLongSetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicLongSetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicLongAlterCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicLongAlterMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicLongIncrementAndGetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicLongIncrementAndGetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicLongGetAndSetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicLongGetAndSetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicLongGetAndAlterCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicLongGetAndAlterMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicLongGetAndIncrementCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicLongGetAndIncrementMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.SemaphoreDrainPermitsCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new SemaphoreDrainPermitsMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.SemaphoreAvailablePermitsCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new SemaphoreAvailablePermitsMessageTasks(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.SemaphoreInitCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new SemaphoreInitMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.SemaphoreAcquireCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new SemaphoreAcquireMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.SemaphoreReducePermitsCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new SemaphoreReducePermitsMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.SemaphoreTryAcquireCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new SemaphoreTryAcquireMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.SemaphoreReleaseCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new SemaphoreReleaseMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalListSizeCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalListSizeMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalListRemoveCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalListRemoveMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalListAddCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalListAddMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalMultiMapPutCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalMultiMapPutMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalMultiMapRemoveEntryCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalMultiMapRemoveEntryMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalMultiMapGetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalMultiMapGetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalMultiMapRemoveCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalMultiMapRemoveMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalMultiMapSizeCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalMultiMapSizeMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalMultiMapValueCountCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalMultiMapValueCountMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ConditionSignalCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ConditionSignalMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ConditionBeforeAwaitCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ConditionBeforeAwaitMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ConditionAwaitCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ConditionAwaitMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ConditionSignalAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ConditionSignalAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ListGetAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ListGetAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ListListIteratorCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ListListIteratorMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ListSetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ListSetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ListAddAllWithIndexCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ListAddAllWithIndexMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ListCompareAndRemoveAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ListCompareAndRemoveAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ListGetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ListGetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ListRemoveListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ListRemoveListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ListRemoveWithIndexCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ListRemoveWithIndexMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ListAddListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ListAddListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ListIteratorCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ListIteratorMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ListClearCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ListClearMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ListAddAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ListAddAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ListAddCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ListAddMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ListAddWithIndexCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ListAddWithIndexMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ListLastIndexOfCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ListLastIndexOfMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ListRemoveCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ListRemoveMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ListSubCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ListSubMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ListContainsCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ListContainsMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ListIndexOfCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ListIndexOfMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ListSizeCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ListSizeMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ListContainsAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ListContainsAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ListIsEmptyCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ListIsEmptyMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ListCompareAndRetainAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ListCompareAndRetainAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CountDownLatchAwaitCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CountDownLatchAwaitMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CountDownLatchCountDownCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CountDownLatchCountDownMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CountDownLatchGetCountCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CountDownLatchGetCountMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CountDownLatchTrySetCountCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CountDownLatchTrySetCountMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalQueueSizeCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalQueueSizeMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalQueueOfferCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalQueueOfferMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalQueuePeekCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalQueuePeekMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalQueuePollCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalQueuePollMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalQueueTakeCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalQueueTakeMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MultiMapClearCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MultiMapClearMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MultiMapGetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MultiMapGetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MultiMapRemoveEntryCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MultiMapRemoveEntryMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MultiMapContainsKeyCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MultiMapContainsKeyMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MultiMapSizeCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MultiMapSizeMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MultiMapAddEntryListenerToKeyCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MultiMapAddEntryListenerToKeyMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MultiMapAddEntryListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MultiMapAddEntryListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MultiMapRemoveCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MultiMapRemoveMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MultiMapTryLockCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MultiMapTryLockMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MultiMapIsLockedCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MultiMapIsLockedMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MultiMapContainsValueCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MultiMapContainsValueMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MultiMapKeySetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MultiMapKeySetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MultiMapPutCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MultiMapPutMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MultiMapEntrySetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MultiMapEntrySetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MultiMapValueCountCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MultiMapValueCountMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MultiMapUnlockCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MultiMapUnlockMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MultiMapLockCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MultiMapLockMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MultiMapRemoveEntryListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MultiMapRemoveEntryListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MultiMapContainsEntryCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MultiMapContainsEntryMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MultiMapForceUnlockCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MultiMapForceUnlockMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MultiMapValuesCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MultiMapValuesMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicReferenceClearCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicReferenceClearMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicReferenceCompareAndSetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicReferenceCompareAndSetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicReferenceGetAndAlterCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicReferenceGetAndAlterMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicReferenceGetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicReferenceGetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicReferenceGetAndSetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicReferenceGetAndSetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicReferenceApplyCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicReferenceApplyMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicReferenceIsNullCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicReferenceIsNullMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicReferenceSetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicReferenceSetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicReferenceAlterAndGetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicReferenceAlterAndGetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicReferenceSetAndGetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicReferenceSetAndGetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicReferenceAlterCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicReferenceAlterMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.AtomicReferenceContainsCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AtomicReferenceContainsMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TopicPublishCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TopicPublishMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TopicAddMessageListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TopicAddMessageListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TopicRemoveMessageListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TopicRemoveMessageListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalMapValuesCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalMapValuesMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalMapSizeCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalMapSizeMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalMapPutIfAbsentCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalMapPutIfAbsentMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalMapRemoveCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalMapRemoveMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalMapGetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalMapGetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalMapGetForUpdateCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalMapGetForUpdateMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalMapIsEmptyCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalMapIsEmptyMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalMapKeySetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalMapKeySetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalMapKeySetWithPredicateCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalMapKeySetWithPredicateMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalMapReplaceIfSameCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalMapReplaceIfSameMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalMapContainsKeyCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalMapContainsKeyMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalMapRemoveIfSameCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalMapRemoveIfSameMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalMapSetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalMapSetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalMapReplaceCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalMapReplaceMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalMapPutCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalMapPutMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalMapDeleteCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalMapDeleteMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalMapValuesWithPredicateCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalMapValuesWithPredicateMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ExecutorServiceCancelOnPartitionCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ExecutorServiceCancelOnPartitionMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ExecutorServiceSubmitToPartitionCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ExecutorServiceSubmitToPartitionMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ExecutorServiceCancelOnAddressCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ExecutorServiceCancelOnAddressMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ExecutorServiceIsShutdownCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ExecutorServiceIsShutdownMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ExecutorServiceShutdownCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ExecutorServiceShutdownMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ExecutorServiceSubmitToAddressCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ExecutorServiceSubmitToAddressMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.DurableExecutorSubmitToPartitionCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new DurableExecutorSubmitToPartitionMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.DurableExecutorIsShutdownCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new DurableExecutorIsShutdownMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.DurableExecutorShutdownCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new DurableExecutorShutdownMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.DurableExecutorRetrieveResultCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new DurableExecutorRetrieveResultMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.DurableExecutorDisposeResultCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new DurableExecutorDisposeResultMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.DurableExecutorRetrieveAndDisposeResultCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new DurableExecutorRetrieveAndDisposeResultMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionCreateCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionCreateMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.XATransactionClearRemoteCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new XAClearRemoteTransactionMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.XATransactionFinalizeCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new XAFinalizeTransactionMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionCommitCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionCommitMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.XATransactionCollectTransactionsCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new XACollectTransactionsMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.XATransactionPrepareCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new XATransactionPrepareMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.XATransactionCreateCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new XATransactionCreateMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionRollbackCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionRollbackMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.XATransactionCommitCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new XATransactionCommitMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.XATransactionRollbackCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new XATransactionRollbackMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalSetSizeCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalSetSizeMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalSetAddCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalSetAddMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.TransactionalSetRemoveCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new TransactionalSetRemoveMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapEntriesWithPagingPredicateCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapEntriesWithPagingPredicateMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapClearNearCacheCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapClearNearCacheMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapAddEntryListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapAddEntryListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapAssignAndGetUuidsCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapAssignAndGetUuidsMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapFetchNearCacheInvalidationMetadataCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapFetchNearCacheInvalidationMetadataTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapRemoveIfSameCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapRemoveIfSameMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapAddInterceptorCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapAddInterceptorMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapEntriesWithPredicateCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapEntriesWithPredicateMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapPutTransientCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapPutTransientMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapContainsValueCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapContainsValueMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapIsEmptyCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapIsEmptyMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapReplaceCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapReplaceMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapRemoveInterceptorCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapRemoveInterceptorMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapAddNearCacheEntryListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new Pre38MapAddNearCacheEntryListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapAddNearCacheInvalidationListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapAddNearCacheInvalidationListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapExecuteOnAllKeysCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapExecuteOnAllKeysMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapFlushCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapFlushMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapSetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapSetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapTryLockCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapTryLockMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapAddEntryListenerToKeyCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapAddEntryListenerToKeyMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapEntrySetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapEntrySetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapClearCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapClearMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapLockCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapLockMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapGetEntryViewCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapGetEntryViewMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapRemovePartitionLostListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapRemovePartitionLostListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapLoadGivenKeysCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapLoadGivenKeysMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapExecuteWithPredicateCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapExecuteWithPredicateMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapRemoveAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapRemoveAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapPutIfAbsentCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapPutIfAbsentMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapTryRemoveCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapTryRemoveMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapPutCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapPutMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapUnlockCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapUnlockMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapSizeCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapSizeMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapValuesWithPredicateCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapValuesWithPredicateMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapAddEntryListenerToKeyWithPredicateCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapAddEntryListenerToKeyWithPredicateMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapEvictCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapEvictMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapGetAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapGetAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapForceUnlockCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapForceUnlockMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapLoadAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapLoadAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapAddIndexCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapAddIndexMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapExecuteOnKeyCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapExecuteOnKeyMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapKeySetWithPagingPredicateCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapKeySetWithPagingPredicateMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapRemoveEntryListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapRemoveEntryListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapIsLockedCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapIsLockedMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapEvictAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapEvictAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapSubmitToKeyCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapSubmitToKeyMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapValuesCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapValuesMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapAddEntryListenerWithPredicateCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapAddEntryListenerWithPredicateMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapDeleteCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapDeleteMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapAddPartitionLostListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapAddPartitionLostListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapPutAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapPutAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapRemoveCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapRemoveMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapKeySetWithPredicateCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapKeySetWithPredicateMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapExecuteOnKeysCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapExecuteOnKeysMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapReplaceIfSameCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapReplaceIfSameMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapContainsKeyCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapContainsKeyMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapTryPutCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapTryPutMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapValuesWithPagingPredicateCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapValuesWithPagingPredicateMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapGetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapGetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapKeySetCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapKeySetMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapFetchKeysCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapFetchKeysMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapFetchEntriesCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapFetchEntriesMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapAggregateCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapAggregateMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapAggregateWithPredicateCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapAggregateWithPredicateMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapProjectCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapProjectionMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapProjectWithPredicateCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapProjectionWithPredicateMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapFetchWithQueryCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapFetchWithQueryMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapEventJournalSubscribeCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapEventJournalSubscribeTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.MapEventJournalReadCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapEventJournalReadTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ClientAddPartitionLostListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AddPartitionLostListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ClientRemovePartitionLostListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new RemovePartitionLostListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ClientCreateProxyCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CreateProxyMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ClientGetDistributedObjectsCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new GetDistributedObjectsMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ClientAddDistributedObjectListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AddDistributedObjectListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ClientDestroyProxyCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new DestroyProxyMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ClientPingCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new PingMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ClientAddMembershipListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AddMembershipListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ClientAuthenticationCustomCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AuthenticationCustomCredentialsMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ClientRemoveAllListenersCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new RemoveAllListenersMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ClientRemoveDistributedObjectListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new RemoveDistributedObjectListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ClientGetPartitionsCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new GetPartitionsMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ClientAuthenticationCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AuthenticationMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ClientStatisticsCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ClientStatisticsMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ClientDeployClassesCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new DeployClassesMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.QueueCompareAndRemoveAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new QueueCompareAndRemoveAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.QueueContainsAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new QueueContainsAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.QueueAddAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new QueueAddAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.QueueTakeCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new QueueTakeMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.QueueAddListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new QueueAddListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.QueueCompareAndRetainAllCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new QueueCompareAndRetainAllMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.QueueOfferCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new QueueOfferMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.QueuePeekCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new QueuePeekMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.QueueRemoveCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new QueueRemoveMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.QueueIsEmptyCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new QueueIsEmptyMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.QueueIteratorCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new QueueIteratorMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.QueueSizeCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new QueueSizeMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.QueuePutCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new QueuePutMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.QueueContainsCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new QueueContainsMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.QueuePollCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new QueuePollMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.QueueDrainToCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new QueueDrainMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.QueueRemoveListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new QueueRemoveListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.QueueRemainingCapacityCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new QueueRemainingCapacityMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.QueueClearCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new QueueClearMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.QueueDrainToMaxSizeCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new QueueDrainMaxSizeMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CardinalityEstimatorAddCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CardinalityEstimatorAddMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.CardinalityEstimatorEstimateCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new CardinalityEstimatorEstimateMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ScheduledExecutorSubmitToPartitionCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ScheduledExecutorSubmitToPartitionMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ScheduledExecutorSubmitToAddressCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ScheduledExecutorSubmitToAddressMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ScheduledExecutorShutdownCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ScheduledExecutorShutdownMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ScheduledExecutorDisposeFromPartitionCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ScheduledExecutorTaskDisposeFromPartitionMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ScheduledExecutorDisposeFromAddressCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ScheduledExecutorTaskDisposeFromAddressMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ScheduledExecutorCancelFromPartitionCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ScheduledExecutorTaskCancelFromPartitionMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ScheduledExecutorCancelFromAddressCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ScheduledExecutorTaskCancelFromAddressMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ScheduledExecutorIsDoneFromPartitionCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ScheduledExecutorTaskIsDoneFromPartitionMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ScheduledExecutorIsDoneFromAddressCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ScheduledExecutorTaskIsDoneFromAddressMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ScheduledExecutorGetDelayFromPartitionCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ScheduledExecutorTaskGetDelayFromPartitionMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ScheduledExecutorGetDelayFromAddressCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ScheduledExecutorTaskGetDelayFromAddressMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ScheduledExecutorGetStatsFromPartitionCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ScheduledExecutorTaskGetStatisticsFromPartitionMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ScheduledExecutorGetStatsFromAddressCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ScheduledExecutorTaskGetStatisticsFromAddressMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ScheduledExecutorGetResultFromPartitionCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ScheduledExecutorTaskGetResultFromPartitionMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ScheduledExecutorGetResultFromAddressCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ScheduledExecutorTaskGetResultFromAddressMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ScheduledExecutorGetAllScheduledFuturesCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ScheduledExecutorGetAllScheduledMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ScheduledExecutorIsCancelledFromPartitionCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ScheduledExecutorTaskIsCancelledFromPartitionMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ScheduledExecutorIsCancelledFromAddressCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new ScheduledExecutorTaskIsCancelledFromAddressMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ContinuousQueryDestroyCacheCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapDestroyCacheMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ContinuousQueryPublisherCreateCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapPublisherCreateMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ContinuousQuerySetReadCursorCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapSetReadCursorMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ContinuousQueryAddListenerCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapAddListenerMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ContinuousQueryMadePublishableCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapMadePublishableMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.ContinuousQueryPublisherCreateWithValueCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new MapPublisherCreateWithValueMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.DynamicConfigAddMultiMapConfigCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AddMultiMapConfigMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.DynamicConfigAddCardinalityEstimatorConfigCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AddCardinalityEstimatorConfigMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.DynamicConfigAddExecutorConfigCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AddExecutorConfigMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.DynamicConfigAddDurableExecutorConfigCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AddDurableExecutorConfigMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.DynamicConfigAddScheduledExecutorConfigCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AddScheduledExecutorConfigMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.DynamicConfigAddRingbufferConfigCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AddRingbufferConfigMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.DynamicConfigAddLockConfigCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AddLockConfigMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.DynamicConfigAddListConfigCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AddListConfigMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.DynamicConfigAddSetConfigCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AddSetConfigMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.DynamicConfigAddSemaphoreConfigCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AddSemaphoreConfigMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.DynamicConfigAddTopicConfigCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AddTopicConfigMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.DynamicConfigAddReplicatedMapConfigCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AddReplicatedMapConfigMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.DynamicConfigAddQueueConfigCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AddQueueConfigMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.DynamicConfigAddMapConfigCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AddMapConfigMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.DynamicConfigAddReliableTopicConfigCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AddReliableTopicConfigMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//    this.factories[com.hazelcast.client.impl.protocol.codec.DynamicConfigAddCacheConfigCodec.RequestParameters.TYPE.id()] = new MessageTaskFactory() {
//      public MessageTask create(ClientMessage clientMessage, Connection connection) {
//        return new AddCacheConfigMessageTask(clientMessage, DefaultMessageTaskFactoryProvider.this.node, connection);
//      }
//    };
//  }


  public MessageTaskFactory[] getFactories() {
    return this.factories;
  }
}
