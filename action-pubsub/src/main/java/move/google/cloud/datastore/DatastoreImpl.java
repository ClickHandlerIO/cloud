/*
 * Copyright (c) 2011-2015 Spotify AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package move.google.cloud.datastore;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.protobuf.ProtoHttpContent;
import com.google.api.client.util.IOUtils;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.datastore.v1.AllocateIdsRequest;
import com.google.datastore.v1.AllocateIdsResponse;
import com.google.datastore.v1.BeginTransactionRequest;
import com.google.datastore.v1.BeginTransactionResponse;
import com.google.datastore.v1.CommitRequest;
import com.google.datastore.v1.CommitResponse;
import com.google.datastore.v1.LookupRequest;
import com.google.datastore.v1.LookupResponse;
import com.google.datastore.v1.Mutation;
import com.google.datastore.v1.PartitionId;
import com.google.datastore.v1.ReadOptions;
import com.google.datastore.v1.RollbackRequest;
import com.google.datastore.v1.RollbackResponse;
import com.google.datastore.v1.RunQueryRequest;
import com.google.datastore.v1.RunQueryResponse;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import rx.Single;

/**
 * The Datastore implementation.
 */
final class DatastoreImpl implements Datastore {

  private static final Logger log = LoggerFactory.getLogger(Datastore.class);

  private static final String VERSION = "1.0.0";
  private static final String USER_AGENT = "Datastore-Java-Client/" + VERSION + " (gzip)";

  private final DatastoreConfig config;
  private final AsyncHttpClient client;
  private final String prefixUri;

  private final ScheduledExecutorService executor;
  Vertx vertx = Vertx.vertx();
  private volatile String accessToken;
  private HttpClient httpClient;

  DatastoreImpl(final DatastoreConfig config) {
    this.config = config;
    final AsyncHttpClientConfig httpConfig = new DefaultAsyncHttpClientConfig.Builder()
        .setConnectTimeout(config.getConnectTimeout())
        .setRequestTimeout(config.getRequestTimeout())
        .setMaxConnections(config.getMaxConnections())
        .setMaxRequestRetry(config.getRequestRetry())
        .setCompressionEnforced(true)
        .build();

    client = new DefaultAsyncHttpClient(httpConfig);
    prefixUri = String
        .format("/%s/projects/%s:", config.getVersion(), config.getProject());

    executor = Executors.newSingleThreadScheduledExecutor();

    HttpClientOptions clientOptions = new HttpClientOptions()
        .setDefaultHost(config.getHost().split(":")[0])
        .setDefaultPort(Integer.parseInt(config.getHost().split(":")[1]))
        .setTryUseCompression(true);
//        .setMaxPoolSize(config.getMaxConnections());
    httpClient = vertx.createHttpClient(clientOptions);

    if (config.getCredential() != null) {
      // block while retrieving an access token for the first time
      refreshAccessToken();

      // wake up every 10 seconds to check if access token has expired
      executor.scheduleAtFixedRate(this::refreshAccessToken, 10, 10, TimeUnit.SECONDS);
    }
  }

  private static boolean isSuccessful(final int statusCode) {
    return statusCode >= 200 && statusCode < 300;
  }

  public static byte[] decompress(byte[] contentBytes) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      IOUtils.copy(new GZIPInputStream(new ByteArrayInputStream(contentBytes)), out);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return out.toByteArray();
  }

  @Override
  public void close() {
    executor.shutdown();
    try {
      client.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void refreshAccessToken() {
    final Credential credential = config.getCredential();
    final Long expiresIn = credential.getExpiresInSeconds();

    // trigger refresh if token is about to expire
    if (credential.getAccessToken() == null || expiresIn != null && expiresIn <= 60) {
      try {
        credential.refreshToken();
        final String accessTokenLocal = credential.getAccessToken();
        if (accessTokenLocal != null) {
          this.accessToken = accessTokenLocal;
        }
      } catch (final IOException e) {
        log.error("Storage exception", Throwables.getRootCause(e));
      }
    }
  }

  private Single<FullResponse> prepareRequest(final String method, final ProtoHttpContent payload) {
    return Single.create(subscriber -> {
      final HttpClientRequest request = httpClient.post(prefixUri + method);

      request.putHeader("Authorization", "Bearer " + accessToken);
      request.putHeader("Content-Type", "application/x-protobuf");
      request.putHeader("User-Agent", USER_AGENT);
      request.putHeader("Accept-Encoding", "gzip");
      try {
        request.putHeader("Content-Length", Long.toString(payload.getLength()));
      } catch (Throwable e) {
      }

      request.handler(response -> {

        response.bodyHandler(buffer -> {
          if (!isSuccessful(response.statusCode())) {
            if (!subscriber.isUnsubscribed()) {
              subscriber.onError(new DatastoreException(response.statusCode(), buffer.toString()));
            }
          } else {
            if (!subscriber.isUnsubscribed()) {
              subscriber.onSuccess(new FullResponse(response, buffer));
            }
          }
        });

        response.exceptionHandler(e -> {
          if (!subscriber.isUnsubscribed()) {
            subscriber.onError(e);
          }
        });
      });
      request.exceptionHandler(e -> {
        if (!subscriber.isUnsubscribed()) {
          subscriber.onError(e);
        }
      });

      request.end(Buffer.buffer(payload.getMessage().toByteArray()));
    });
  }

  @Override
  public TransactionResult transaction() throws DatastoreException {
    return transactionAsync().toBlocking().value();
  }

  @Override
  public Single<TransactionResult> transactionAsync() {
    return Single.create(subscriber -> {
      final BeginTransactionRequest.Builder request = BeginTransactionRequest.newBuilder();
      final ProtoHttpContent payload = new ProtoHttpContent(request.build());
      prepareRequest("beginTransaction", payload).subscribe(
          r -> {
            final BeginTransactionResponse transaction;
            try {
              transaction = BeginTransactionResponse
                  .parseFrom(r.payload.getBytes());

              if (!subscriber.isUnsubscribed()) {
                subscriber.onSuccess(TransactionResult.build(transaction));
              }
            } catch (InvalidProtocolBufferException e) {
              if (!subscriber.isUnsubscribed()) {
                subscriber.onError(e);
              }
            }
          },
          e -> {
            if (!subscriber.isUnsubscribed()) {
              subscriber.onError(e);
            }
          }
      );
    });
  }

  @Override
  public RollbackResult rollback(final TransactionResult txn) throws DatastoreException {
    try {
      return rollbackAsync(Single.just(txn)).toBlocking().value();
    } catch (Throwable e) {
      Throwable cause = Throwables.getRootCause(e);
      Throwables.throwIfUnchecked(cause);
      throw new DatastoreException(cause);
    }
  }

  @Override
  public Single<RollbackResult> rollbackAsync(final Single<TransactionResult> txn) {
    return Single.create(subscriber -> {
      txn.subscribe(
          transactionResult -> {
            final ByteString transaction = transactionResult.getTransaction();
            final RollbackRequest.Builder request = RollbackRequest.newBuilder();
            request.setTransaction(transaction);
            final ProtoHttpContent payload = new ProtoHttpContent(request.build());

            prepareRequest("rollback", payload).subscribe(
                r -> {
                  final RollbackResponse rollbackResponse;
                  try {
                    rollbackResponse = RollbackResponse
                        .parseFrom(r.payload.getBytes());

                    if (!subscriber.isUnsubscribed()) {
                      subscriber.onSuccess(RollbackResult.build(rollbackResponse));
                    }
                  } catch (InvalidProtocolBufferException e) {
                    if (!subscriber.isUnsubscribed()) {
                      subscriber.onError(e);
                    }
                  }
                },
                e -> {
                  if (!subscriber.isUnsubscribed()) {
                    subscriber.onError(e);
                  }
                }
            );
          },
          e -> {
            if (!subscriber.isUnsubscribed()) {
              subscriber.onError(e);
            }
          }
      );
    });
  }

  @Override
  public MutationResult commit(final TransactionResult txn) throws DatastoreException {
    return executeAsync((MutationStatement) null, Single.just(txn)).toBlocking().value();
  }

  @Override
  public Single<MutationResult> commitAsync(
      final Single<TransactionResult> txn) {
    return executeAsync((MutationStatement) null, txn);
  }

  @Override
  public AllocateIdsResult execute(final AllocateIds statement) throws DatastoreException {
    return executeAsync(statement).toBlocking().value();
  }

  @Override
  public Single<AllocateIdsResult> executeAsync(final AllocateIds statement) {
    return Single.create(subscriber -> {
      final AllocateIdsRequest.Builder request = AllocateIdsRequest.newBuilder()
          .addAllKeys(statement.getPb(config.getNamespace()));
      final ProtoHttpContent payload = new ProtoHttpContent(request.build());
      prepareRequest("allocateIds", payload).subscribe(
          r -> {
            final AllocateIdsResponse transaction;
            try {
              transaction = AllocateIdsResponse
                  .parseFrom(r.payload.getBytes());

              if (!subscriber.isUnsubscribed()) {
                subscriber.onSuccess(AllocateIdsResult.build(transaction));
              }
            } catch (InvalidProtocolBufferException e) {
              if (!subscriber.isUnsubscribed()) {
                subscriber.onError(e);
              }
            }
          },
          e -> {
            if (!subscriber.isUnsubscribed()) {
              subscriber.onError(e);
            }
          }
      );
    });
  }

  @Override
  public QueryResult execute(final KeyQuery statement) throws DatastoreException {
    return executeAsync(statement).toBlocking().value();
  }

  @Override
  public QueryResult execute(final List<KeyQuery> statements) throws DatastoreException {
    return executeAsync(statements).toBlocking().value();
  }

  @Override
  public Single<QueryResult> executeAsync(final KeyQuery statement) {
    return executeAsync(statement, Single.just(TransactionResult.build()));
  }

  @Override
  public Single<QueryResult> executeAsync(final List<KeyQuery> statements) {
    return executeAsync(statements, Single.just(TransactionResult.build()));
  }

  @Override
  public QueryResult execute(final KeyQuery statement, final TransactionResult txn)
      throws DatastoreException {
    return executeAsync(statement, Single.just(txn)).toBlocking().value();
  }

  @Override
  public QueryResult execute(final List<KeyQuery> statements, final TransactionResult txn)
      throws DatastoreException {
    return executeAsync(statements, Single.just(txn)).toBlocking().value();
  }

  @Override
  public Single<QueryResult> executeAsync(final KeyQuery statement,
      final Single<TransactionResult> txn) {
    return executeAsync(ImmutableList.of(statement), txn);
  }

  @Override
  public Single<QueryResult> executeAsync(final List<KeyQuery> statements,
      final Single<TransactionResult> txn) {
    return Single.create(subscriber -> {
      txn.subscribe(
          transactionResult -> {
            final List<com.google.datastore.v1.Key> keys = statements
                .stream().map(s -> s.getKey().getPb(config.getNamespace()))
                .collect(Collectors.toList());
            final LookupRequest.Builder request = LookupRequest.newBuilder().addAllKeys(keys);
            final ByteString transaction = transactionResult.getTransaction();
            if (transaction != null) {
              request.setReadOptions(ReadOptions.newBuilder().setTransaction(transaction));
            }
            final ProtoHttpContent payload = new ProtoHttpContent(request.build());

            prepareRequest("lookup", payload).subscribe(
                r -> {
                  final LookupResponse lookupResponse;
                  try {
                    lookupResponse = LookupResponse
                        .parseFrom(r.payload.getBytes());

                    if (!subscriber.isUnsubscribed()) {
                      subscriber.onSuccess(QueryResult.build(lookupResponse));
                    }
                  } catch (InvalidProtocolBufferException e) {
                    if (!subscriber.isUnsubscribed()) {
                      subscriber.onError(e);
                    }
                  }
                },
                e -> {
                  if (!subscriber.isUnsubscribed()) {
                    subscriber.onError(e);
                  }
                }
            );
          },
          e -> {
            if (!subscriber.isUnsubscribed()) {
              subscriber.onError(e);
            }
          }
      );
    });
  }

  @Override
  public MutationResult execute(final MutationStatement statement) throws DatastoreException {
    return executeAsync(statement).toBlocking().value();
  }

  @Override
  public Single<MutationResult> executeAsync(final MutationStatement statement) {
    return executeAsync(statement, Single.just(TransactionResult.build()));
  }

  @Override
  public MutationResult execute(final MutationStatement statement, final TransactionResult txn)
      throws DatastoreException {
    return executeAsync(statement, Single.just(txn)).toBlocking().value();
  }

  @Override
  public Single<MutationResult> executeAsync(final MutationStatement statement,
      final Single<TransactionResult> txn) {
    final List<Mutation> mutations = Optional
        .ofNullable(statement)
        .flatMap(s -> Optional.of(ImmutableList.of(s.getPb(config.getNamespace()))))
        .orElse(ImmutableList.of());

    return executeAsyncMutations(mutations, txn);
  }

  @Override
  public MutationResult execute(final Batch batch) throws DatastoreException {
    return executeAsync(batch).toBlocking().value();
  }

  @Override
  public Single<MutationResult> executeAsync(final Batch batch) {
    return executeAsync(batch, Single.just(TransactionResult.build()));
  }

  @Override
  public MutationResult execute(final Batch batch, final TransactionResult txn)
      throws DatastoreException {
    return executeAsync(batch, Single.just(txn)).toBlocking().value();
  }

  @Override
  public Single<MutationResult> executeAsync(final Batch batch,
      final Single<TransactionResult> txn) {
    return executeAsyncMutations(batch.getPb(config.getNamespace()), txn);
  }

  private Single<MutationResult> executeAsyncMutations(final List<Mutation> mutations,
      final Single<TransactionResult> txn) {
    return Single.create(subscriber -> {
      txn.subscribe(
          transactionResult -> {
            final CommitRequest.Builder request = CommitRequest.newBuilder();
            if (mutations != null) {
              request.addAllMutations(mutations);
            }

            final ByteString transaction = transactionResult.getTransaction();
            if (transaction != null) {
              request.setTransaction(transaction);
            } else {
              request.setMode(CommitRequest.Mode.NON_TRANSACTIONAL);
            }
            final ProtoHttpContent payload = new ProtoHttpContent(request.build());

            prepareRequest("commit", payload).subscribe(
                r -> {
                  final CommitResponse commitResponse;
                  try {
                    commitResponse = CommitResponse
                        .parseFrom(r.payload.getBytes());

                    if (!subscriber.isUnsubscribed()) {
                      subscriber.onSuccess(MutationResult.build(commitResponse));
                    }
                  } catch (InvalidProtocolBufferException e) {
                    if (!subscriber.isUnsubscribed()) {
                      subscriber.onError(e);
                    }
                  }
                },
                e -> {
                  if (!subscriber.isUnsubscribed()) {
                    subscriber.onError(e);
                  }
                }
            );
          },
          e -> {
            if (!subscriber.isUnsubscribed()) {
              subscriber.onError(e);
            }
          }
      );
    });
  }

  @Override
  public QueryResult execute(final Query statement) throws DatastoreException {
    return executeAsync(statement).toBlocking().value();
  }

  @Override
  public Single<QueryResult> executeAsync(final Query statement) {
    return executeAsync(statement, Single.just(TransactionResult.build()));
  }

  @Override
  public QueryResult execute(final Query statement, final TransactionResult txn)
      throws DatastoreException {
    return executeAsync(statement, Single.just(txn)).toBlocking().value();
  }

  @Override
  public Single<QueryResult> executeAsync(final Query statement,
      final Single<TransactionResult> txn) {
    return Single.create(subscriber -> {
      txn.subscribe(
          transactionResult -> {
            final String namespace = config.getNamespace();
            final RunQueryRequest.Builder request = RunQueryRequest.newBuilder()
                .setQuery(statement.getPb(namespace != null ? namespace : ""));
            if (namespace != null) {
              request.setPartitionId(PartitionId.newBuilder().setNamespaceId(namespace));
            }
            final ByteString transaction = transactionResult.getTransaction();
            if (transaction != null) {
              request.setReadOptions(ReadOptions.newBuilder().setTransaction(transaction));
            }
            final ProtoHttpContent payload = new ProtoHttpContent(request.build());

            prepareRequest("runQuery", payload).subscribe(
                r -> {
                  final RunQueryResponse runQueryResponse;
                  try {
                    runQueryResponse = RunQueryResponse
                        .parseFrom(r.payload.getBytes());

                    if (!subscriber.isUnsubscribed()) {
                      subscriber.onSuccess(QueryResult.build(runQueryResponse));
                    }
                  } catch (InvalidProtocolBufferException e) {
                    if (!subscriber.isUnsubscribed()) {
                      subscriber.onError(e);
                    }
                  }
                },
                e -> {
                  if (!subscriber.isUnsubscribed()) {
                    subscriber.onError(e);
                  }
                }
            );
          },
          e -> {
            if (!subscriber.isUnsubscribed()) {
              subscriber.onError(e);
            }
          }
      );
    });
  }

  static class FullResponse {

    private final HttpClientResponse response;
    private final Buffer payload;

    public FullResponse(HttpClientResponse response, Buffer payload) {
      this.response = response;
      this.payload = payload;
    }
  }
}
