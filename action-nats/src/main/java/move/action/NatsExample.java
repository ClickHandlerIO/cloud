package move.action;

import io.nats.client.AsyncSubscription;
import io.nats.client.Connection;
import io.nats.client.Nats;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import move.NUID;

/**
 *
 */
public class NatsExample {

  public static void main(String[] args) throws Throwable {

// Connect to default URL ("nats://localhost:4222")
    int total = 10_000_000;
    int connCount = 10;
    int max_per_conn = 10_000;
    int iterations = total / max_per_conn;

    final List<NConn> connections = new ArrayList<>(connCount);
    for (int i = 0; i < connCount; i++) {
      connections.add(new NConn(Nats.connect(), "" + i, max_per_conn));
    }

    ExecutorService executorService = Executors.newFixedThreadPool(connections.size());
    final AtomicInteger count = new AtomicInteger(0);

    IntStream.range(0, connCount).forEach(e -> executorService.submit(() -> {
    }));

    final long start = System.currentTimeMillis();
    final List<Future<Long>> futures = IntStream.range(0, iterations)
        .mapToObj(id -> {
          Future<Long> future = executorService
              .submit(() -> connections.get(id % connections.size()).await());
          return future;
        }).collect(Collectors.toList());

    long totalTime = futures.stream().mapToLong(f -> {
      try {
        return f.get();
      } catch (Throwable e) {
        return 0;
      }
    }).sum();

    System.out.println(totalTime);

    System.out.println("Requests/Sec -> " + ((total) / (
        (double) (System.currentTimeMillis() - start) / 1000.0)));

    System.in.read();

    System.out.println("Done");
  }

  private static void sample(Connection nc, String subject, int iterations) throws Throwable {
    final AtomicInteger counter = new AtomicInteger(0);
    final AtomicReference<CountDownLatch> latch = new AtomicReference<>(
        new CountDownLatch(iterations));
    // Simple Async Subscriber
    final AtomicLong started = new AtomicLong();

    final AsyncSubscription sub = nc.subscribe(subject, m -> {

//      System.out.printf("Received a message: %s\n", new String(m.getData()));

    });

    started.set(System.currentTimeMillis());

    final long pubStart = System.currentTimeMillis();

//    System.out.println("Sent in " + (System.currentTimeMillis() - pubStart));

    latch.get().await();

    System.out.println("Batch " + (System.currentTimeMillis() - started.get()));

    sub.unsubscribe();
  }

  private static class NConn {

    public final Connection connection;
    public final String subject;
    final AtomicLong started = new AtomicLong(0);
    final AtomicReference<CountDownLatch> latch;
    final AsyncSubscription subscription;
    final AtomicLong counter = new AtomicLong(0L);
    final int iterations;

    public NConn(Connection connection, String subject, int iterations) {
      this.connection = connection;
      this.subject = subject;
      this.iterations = iterations;
      latch = new AtomicReference<>(new CountDownLatch(0));

      subscription = connection.subscribe(subject, m -> {
        latch.get().countDown();
        if (counter.incrementAndGet() == iterations) {
//        System.out.println("Received in " + (System.currentTimeMillis() - started.get()));
//          System.out.println(Thread.currentThread().getName());
        }
      });
    }

    public void close() {
      try {
        subscription.unsubscribe();
      } catch (IOException e) {
        e.printStackTrace();
      }
      connection.close();
    }

    public synchronized long await() {
      long start = System.currentTimeMillis();
      latch.set(new CountDownLatch(iterations));

      for (int i = 0; i < iterations; i++) {
        try {
          connection.publish(subject,
              ("Hello World Hello World Hello World Hello World Hello World Hello World Hello World Hello World Hello World "
                  + i).getBytes());
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      try {
        latch.get().await();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      long ret = System.currentTimeMillis() - start;
      System.out.println("Connection: " + subject + " -> " + ret);
      return ret;
    }
  }

  public static class Main {

    public static void main(String[] args) {
      final NUID nuid = new NUID();

      for (int y = 0; y < 100; y++) {
        NUID.get().randomizePrefix();
        System.out.println(NUID.nextGlobal());
      }

      int i = 1_000_000;

      for (int b = 0; b < 5; b++) {
        long start = System.currentTimeMillis();
        for (int x = 0; x < i; x++) {
          nuid.next();
        }
        System.out.println(System.currentTimeMillis() - start);

        start = System.currentTimeMillis();
        for (int x = 0; x < i; x++) {
          NUID.get().next();
        }
        System.out.println(System.currentTimeMillis() - start);
        System.out.println();
      }

      System.out.println(nuid.next());
    }
  }
}
