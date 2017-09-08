package move.action;

import java.util.ConcurrentModificationException;

public class LongHashMap {
  transient LongHashMap.Entry[] table;
  transient int size;
  int threshold;
  final float loadFactor;
  transient int modCount;

  public LongHashMap(int initialCapacity, float loadFactor) {
    this.modCount = 0;
    if (initialCapacity < 0) {
      throw new IllegalArgumentException("Illegal Initial Capacity: " + initialCapacity);
    } else if (loadFactor > 0.0F && !Float.isNaN(loadFactor)) {
      if (initialCapacity == 0) {
        initialCapacity = 1;
      }

      this.loadFactor = loadFactor;
      this.table = new LongHashMap.Entry[initialCapacity];
      this.threshold = (int)((float)initialCapacity * loadFactor);
    } else {
      throw new IllegalArgumentException("Illegal Load factor: " + loadFactor);
    }
  }

  public LongHashMap(int initialCapacity) {
    this(initialCapacity, 0.75F);
  }

  public LongHashMap() {
    this(11, 0.75F);
  }

  public int size() {
    return this.size;
  }

  public boolean isEmpty() {
    return this.size == 0;
  }

  public Object get(long key) {
    LongHashMap.Entry e = this.getEntry(key);
    return e == null ? null : e.value;
  }

  public boolean containsKey(long key) {
    return this.getEntry(key) != null;
  }

  LongHashMap.Entry getEntry(long key) {
    LongHashMap.Entry[] tab = this.table;
    int hash = (int)key;
    int index = (hash & 2147483647) % tab.length;

    for(LongHashMap.Entry e = tab[index]; e != null; e = e.next) {
      if (e.hash == hash && e.key == key) {
        return e;
      }
    }

    return null;
  }

  public boolean containsValue(Object value) {
    LongHashMap.Entry[] tab = this.table;
    int i;
    LongHashMap.Entry e;
    if (value == null) {
      i = tab.length;

      while(i-- > 0) {
        for(e = tab[i]; e != null; e = e.next) {
          if (e.value == null) {
            return true;
          }
        }
      }
    } else {
      i = tab.length;

      while(i-- > 0) {
        for(e = tab[i]; e != null; e = e.next) {
          if (value.equals(e.value)) {
            return true;
          }
        }
      }
    }

    return false;
  }

  public Object put(long key, Object value) {
    LongHashMap.Entry[] tab = this.table;
    int hash = (int)key;
    int index = (hash & 2147483647) % tab.length;

    for(LongHashMap.Entry e = tab[index]; e != null; e = e.next) {
      if (e.hash == hash && e.key == key) {
        Object oldValue = e.value;
        e.value = value;
        return oldValue;
      }
    }

    ++this.modCount;
    if (this.size >= this.threshold) {
      this.rehash();
      tab = this.table;
      index = (hash & 2147483647) % tab.length;
    }

    ++this.size;
    tab[index] = this.newEntry(hash, key, value, tab[index]);
    return null;
  }

  public Object remove(long key) {
    LongHashMap.Entry e = this.removeEntryForKey(key);
    return e == null ? null : e.value;
  }

  LongHashMap.Entry removeEntryForKey(long key) {
    LongHashMap.Entry[] tab = this.table;
    int hash = (int)key;
    int index = (hash & 2147483647) % tab.length;
    LongHashMap.Entry e = tab[index];

    for(LongHashMap.Entry prev = null; e != null; e = e.next) {
      if (e.hash == hash && e.key == key) {
        ++this.modCount;
        if (prev != null) {
          prev.next = e.next;
        } else {
          tab[index] = e.next;
        }

        --this.size;
        return e;
      }

      prev = e;
    }

    return null;
  }

  void removeEntry(LongHashMap.Entry doomed) {
    LongHashMap.Entry[] tab = this.table;
    int index = (doomed.hash & 2147483647) % tab.length;
    LongHashMap.Entry e = tab[index];

    for(LongHashMap.Entry prev = null; e != null; e = e.next) {
      if (e == doomed) {
        ++this.modCount;
        if (prev == null) {
          tab[index] = e.next;
        } else {
          prev.next = e.next;
        }

        --this.size;
        return;
      }

      prev = e;
    }

    throw new ConcurrentModificationException();
  }

  public void clear() {
    LongHashMap.Entry[] tab = this.table;
    ++this.modCount;
    int index = tab.length;

    while(true) {
      --index;
      if (index < 0) {
        this.size = 0;
        return;
      }

      tab[index] = null;
    }
  }

  void rehash() {
    LongHashMap.Entry[] oldTable = this.table;
    int oldCapacity = oldTable.length;
    int newCapacity = oldCapacity * 2 + 1;
    LongHashMap.Entry[] newTable = new LongHashMap.Entry[newCapacity];
    ++this.modCount;
    this.threshold = (int)((float)newCapacity * this.loadFactor);
    this.table = newTable;
    int i = oldCapacity;

    LongHashMap.Entry e;
    int index;
    while(i-- > 0) {
      for(LongHashMap.Entry old = oldTable[i]; old != null; newTable[index] = e) {
        e = old;
        old = old.next;
        index = (e.hash & 2147483647) % newCapacity;
        e.next = newTable[index];
      }
    }

  }

  static boolean eq(Object o1, Object o2) {
    return o1 == null ? o2 == null : o1.equals(o2);
  }

  LongHashMap.Entry newEntry(int hash, long key, Object value, LongHashMap.Entry next) {
    return new LongHashMap.Entry(hash, key, value, next);
  }

  int capacity() {
    return this.table.length;
  }

  float loadFactor() {
    return this.loadFactor;
  }

  static class Entry {
    private int hash;
    private long key;
    private Object value;
    private LongHashMap.Entry next;

    Entry(int hash, long key, Object value, LongHashMap.Entry next) {
      this.hash = hash;
      this.key = key;
      this.value = value;
      this.next = next;
    }

    long getKey() {
      return this.key;
    }

    Object getValue() {
      return this.value;
    }

    Object setValue(Object value) {
      Object oldValue = this.value;
      this.value = value;
      return oldValue;
    }

    public boolean equals(Object o) {
      if (!(o instanceof LongHashMap.Entry)) {
        return false;
      } else {
        LongHashMap.Entry e = (LongHashMap.Entry)o;
        return this.key == e.getKey() && LongHashMap.eq(this.value, e.getValue());
      }
    }

    public int hashCode() {
      return this.hash ^ (this.value == null ? 0 : this.value.hashCode());
    }
  }
}
