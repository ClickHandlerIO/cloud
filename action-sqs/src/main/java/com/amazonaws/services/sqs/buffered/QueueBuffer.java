/*
 * Copyright 2012-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.services.sqs.buffered;

import com.amazonaws.AmazonClientException;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityRequest;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityResult;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteMessageResult;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A buffer to operate on an SQS queue. The buffer batches outbound ( {@code SendMessage},
 * {@code DeleteMessage}, {@code ChangeMessageVisibility}) requests to the queue and pre-fetches
 * messages to receive. In practice, the buffer does almost no work itself, and delegates it to
 * SendQueueBufer and ReceiveQueueBuffer classes.
 * <p>
 * Any errors encountered are passed through to the callers, either as the appropriate Result
 * objects or as exceptions.
 * <p>
 * When the buffer is not used, all internal processing associated with the buffer stops when any
 * outstanding _request to SQS completes. In that idle state, the buffer uses neither connections nor
 * threads.
 * <p>
 * Instances of {@code QueueBuffer} are thread-safe.
 */

public class QueueBuffer {

  /**
   * This executor that will be shared among all queue buffers. Since a single JVM can access
   * hundreds of queues, it won't do to have hundreds of executors spinning up hundreds of threads
   * for each queue. The DaemonThreadFactory creates daemon threads, which means they won't coroutineBlock
   * the JVM from exiting if only they are still around.
   */
//  public static ExecutorService executor = null;//Executors.newCachedThreadPool(new DaemonThreadFactory());

  private final SendQueueBuffer sendBuffer;
  private final ReceiveQueueBuffer receiveBuffer;
  private final AmazonSQSAsync realSqs;
  public final ExecutorService executor;
  QueueBufferConfig config;

  QueueBuffer(QueueBufferConfig paramConfig, String url, AmazonSQSAsync sqs,
      ExecutorService executor) {
    realSqs = sqs;
    config = paramConfig;
    sendBuffer = new SendQueueBuffer(sqs, executor, paramConfig, url);
    receiveBuffer = new ReceiveQueueBuffer(sqs, executor, paramConfig, url);
    this.executor = executor;
  }

  /**
   * asynchronously enqueues a message to SQS.
   *
   * @return a Future object that will be notified when the operation is completed; never null
   */
  public Future<SendMessageResult> sendMessage(SendMessageRequest request,
      AsyncHandler<SendMessageRequest, SendMessageResult> handler) {
    QueueBufferCallback<SendMessageRequest, SendMessageResult> callback = null;
    if (handler != null) {
      callback = new QueueBufferCallback<SendMessageRequest, SendMessageResult>(handler, request);
    }
    QueueBufferFuture<SendMessageRequest, SendMessageResult> future = sendBuffer
        .sendMessage(request, callback);
    future.setBuffer(this);
    return future;
  }

  /**
   * Sends a message to SQS and returns the SQS reply.
   *
   * @return never null
   */
  public SendMessageResult sendMessageSync(SendMessageRequest request) {
    Future<SendMessageResult> future = sendMessage(request, null);
    return waitForFuture(future);
  }

  /**
   * Asynchronously deletes a message from SQS.
   *
   * @return a Future object that will be notified when the operation is completed; never null
   */

  public Future<DeleteMessageResult> deleteMessage(DeleteMessageRequest request,
      AsyncHandler<DeleteMessageRequest, DeleteMessageResult> handler) {
    QueueBufferCallback<DeleteMessageRequest, DeleteMessageResult> callback = null;
    if (handler != null) {
      callback = new QueueBufferCallback<DeleteMessageRequest, DeleteMessageResult>(handler,
          request);
    }

    QueueBufferFuture<DeleteMessageRequest, DeleteMessageResult> future = sendBuffer
        .deleteMessage(request, callback);
    future.setBuffer(this);
    return future;
  }

  /**
   * Deletes a message from SQS. Does not return until a confirmation from SQS has been received
   *
   * @return never null
   */
  public DeleteMessageResult deleteMessageSync(DeleteMessageRequest request) {
    Future<DeleteMessageResult> future = deleteMessage(request, null);
    return waitForFuture(future);
  }

  /**
   * asynchronously adjust a message's visibility deadline to SQS.
   *
   * @return a Future object that will be notified when the operation is completed; never null
   */

  public Future<ChangeMessageVisibilityResult> changeMessageVisibility(
      ChangeMessageVisibilityRequest request,
      AsyncHandler<ChangeMessageVisibilityRequest, ChangeMessageVisibilityResult> handler) {
    QueueBufferCallback<ChangeMessageVisibilityRequest, ChangeMessageVisibilityResult> callback = null;
    if (handler != null) {
      callback = new QueueBufferCallback<ChangeMessageVisibilityRequest, ChangeMessageVisibilityResult>(
          handler, request);
    }

    QueueBufferFuture<ChangeMessageVisibilityRequest, ChangeMessageVisibilityResult> future =
        sendBuffer.changeMessageVisibility(request, callback);
    future.setBuffer(this);
    return future;
  }

  /**
   * Changes visibility of a message in SQS. Does not return until a confirmation from SQS has
   * been received.
   */
  public ChangeMessageVisibilityResult changeMessageVisibilitySync(
      ChangeMessageVisibilityRequest request) {
    Future<ChangeMessageVisibilityResult> future = sendBuffer
        .changeMessageVisibility(request, null);
    return waitForFuture(future);
  }

  /**
   * Submits a _request to receive some messages from SQS.
   *
   * @return a Future object that will be notified when the operation is completed; never null;
   */

  public Future<ReceiveMessageResult> receiveMessage(ReceiveMessageRequest rq,
      AsyncHandler<ReceiveMessageRequest, ReceiveMessageResult> handler) {
    if (canBeRetrievedFromQueueBuffer(rq)) {
      QueueBufferCallback<ReceiveMessageRequest, ReceiveMessageResult> callback = null;
      if (handler != null) {
        callback = new QueueBufferCallback<>(handler, rq);
      }

      QueueBufferFuture<ReceiveMessageRequest, ReceiveMessageResult> future = receiveBuffer
          .receiveMessageAsync(
              rq, callback);
      future.setBuffer(this);
      return future;
    } else if (handler != null) {
      return realSqs.receiveMessageAsync(rq, handler);
    } else {
      return realSqs.receiveMessageAsync(rq);
    }
  }

  /**
   * Retrieves messages from an SQS queue.
   *
   * @return never null
   */
  public ReceiveMessageResult receiveMessageSync(ReceiveMessageRequest rq) {
    Future<ReceiveMessageResult> future = receiveMessage(rq, null);
    return waitForFuture(future);
  }

  /**
   * Shuts down the queue buffer. Once this method has been called, the queue buffer is not
   * operational and all subsequent calls to it may fail.
   * <p>
   * Enabling {@link QueueBufferConfig#flushOnShutdown} will wait for the pending tasks in
   * {@link SendQueueBuffer} to finish before shutting down.
   */
  public void shutdown() {
    if (config.isFlushOnShutdown()) {
      flush();
    }
    receiveBuffer.shutdown();
  }

  /**
   * Flushes all outstanding outbound requests in the {@link SendQueueBuffer}.
   */
  void flush() {
    sendBuffer.flush();
  }

  /**
   * We prefetch and load results in the buffer by making basic requests. I.E. we don't _request
   * queue or message attributes and we have a default visibility deadline. If the user's _request
   * deviates from the basic _request we can't fulfill the _request directly from the buffer, we
   * have to hit SQS directly (Note that when going to SQS directly messages currently in the
   * buffer may be unavailable due to the visibility deadline).
   *
   * @return True if the _request can be fulfilled directly from the buffer, false if we have to go
   * back to the service to fetch the results
   */
  private boolean canBeRetrievedFromQueueBuffer(ReceiveMessageRequest rq) {
    return !hasRequestedQueueAttributes(rq) && !hasRequestedMessageAttributes(rq)
        && isBufferingEnabled()
        && (rq.getVisibilityTimeout() == null);
  }

  /**
   * @return True if _request has been configured to return queue attributes. False otherwise
   */
  private boolean hasRequestedQueueAttributes(ReceiveMessageRequest rq) {
    return rq.getAttributeNames() != null && !rq.getAttributeNames().isEmpty();
  }

  /**
   * @return True if _request has been configured to return message attributes. False otherwise
   */
  private boolean hasRequestedMessageAttributes(ReceiveMessageRequest rq) {
    return rq.getMessageAttributeNames() != null && !rq.getMessageAttributeNames().isEmpty();
  }

  /**
   * @return True if the client has been configured to prefetch batches of messages. False otherwise
   */
  private boolean isBufferingEnabled() {
    return (config.getMaxInflightReceiveBatches() > 0 && config.getMaxDoneReceiveBatches() > 0);
  }

  /**
   * this method carefully waits for futures. If waiting throws, it converts the exceptions to the
   * exceptions that SQS clients expect. This is what we use to turn asynchronous calls into
   * synchronous ones
   */
  private <ResultType> ResultType waitForFuture(Future<ResultType> future) {
    ResultType toReturn = null;
    try {
      toReturn = future.get();
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      AmazonClientException ce = new AmazonClientException(
          "Thread interrupted while waiting for execution result");
      ce.initCause(ie);
      throw ce;
    } catch (ExecutionException ee) {
      // if the cause of the execution exception is an SQS exception, extract it
      // and throw the extracted exception to the clients
      // otherwise, wrap ee in an SQS exception and throw that.
      Throwable cause = ee.getCause();

      if (cause instanceof AmazonClientException) {
        throw (AmazonClientException) cause;
      }

      AmazonClientException ce = new AmazonClientException(
          "Caught an exception while waiting for _request to complete...");
      ce.initCause(ee);
      throw ce;
    }

    return toReturn;

  }

  /**
   * We need daemon threads in our executor so that we don't keep the process running if our
   * executor threads are the only ones left in the process.
   */
  private static class DaemonThreadFactory implements ThreadFactory {

    static AtomicInteger threadCount = new AtomicInteger(0);

    public Thread newThread(Runnable r) {
      int threadNumber = threadCount.addAndGet(1);
      Thread thread = new Thread(r);
      thread.setDaemon(true);
      thread.setName("SQSQueueBufferWorkerThread-" + threadNumber);
      return thread;
    }

  }
}
