package move.action;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;

/**
 *
 */
public class DB {

  public static void main(String[] args) {
    m2();
    m3();
  }

  void db() {
    final Environment env = Environments.newInstance("/Users/clay/MoveData");
    env.clear();

    for (int b = 0; b < 5; b++) {
      long start = System.currentTimeMillis();
      env.executeInTransaction(txn -> {

        env.truncateStore("Messages", txn);
//        final Store store = env.openStore("Messages", StoreConfig.WITHOUT_DUPLICATES, txn);
//
//
//        for (int i = 0; i < 1000000; i++) {
//          store.put(txn, StringBinding.stringToEntry("Hello " + i),
//              StringBinding.stringToEntry("World! " + i));
//        }
      });
      System.out.println(System.currentTimeMillis() - start);
    }
    env.close();
  }

  static void m() {
    ChronicleMap<Integer, Integer> map = ChronicleMapBuilder
        .of(Integer.class, Integer.class)
        .entries(10_000_000)
//        .averageKey(2000000)
//        .averageValue(String.valueOf(2000000))
//        .averageKey(1000000000)
//        .entries(50_000)
        .create();

    for (int x = 0; x < 10; x++) {
      long start = System.currentTimeMillis();
      for (int i = 0; i < 2000000; i++) {
        map.put(i, i);
      }
      System.out.println(System.currentTimeMillis() - start);
    }
  }

  static void m2() {
    ConcurrentMap<Integer, Integer> map = new ConcurrentHashMap<>();
    int count = 2000000;

    for (int x = 0; x < 10; x++) {
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
        map.put(i, i);
      }
      long elapsed = System.currentTimeMillis() - start;
      double runsPerSecond = 1000.0 / elapsed;
      System.out.println((count * runsPerSecond) + " / sec");
    }
  }

  static void m3() {
    ConcurrentMap<Integer, Integer> map = new ConcurrentSkipListMap<>();
    int count = 2000000;

    for (int x = 0; x < 10; x++) {
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
        map.put(i, i);
      }
      long elapsed = System.currentTimeMillis() - start;
      double runsPerSecond = 1000.0 / elapsed;
      System.out.println((count * runsPerSecond) + " / sec");
    }
  }
}
