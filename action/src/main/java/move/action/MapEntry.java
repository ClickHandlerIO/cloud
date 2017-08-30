package move.action;

public class MapEntry {
  public static class LongKeyEntry<V> {

    long key;
    V val;

    public LongKeyEntry(long key, V val) {
      this.key = key;
      this.val = val;
    }

    public long getKey() {
      return key;
    }

    public V getValue() {
      return val;
    }
  }
}