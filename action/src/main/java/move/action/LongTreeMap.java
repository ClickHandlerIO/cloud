/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package move.action;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A Red-Black tree based {@link NavigableMap} implementation. The map is sorted according to the
 * {@linkplain Comparable natural ordering} of its keys, or by a {@link Comparator} provided at map
 * creation time, depending on which constructor is used.
 *
 * <p>This implementation provides guaranteed log(n) time cost for the {@code containsKey}, {@code
 * get}, {@code put} and {@code remove} operations.  Algorithms are adaptations of those in Cormen,
 * Leiserson, and Rivest's <em>Introduction to Algorithms</em>.
 *
 * <p>Note that the ordering maintained by a tree map, like any sorted map, and whether or not an
 * explicit comparator is provided, must be <em>consistent with {@code equals}</em> if this sorted
 * map is to correctly implement the {@code Map} interface.  (See {@code Comparable} or {@code
 * Comparator} for a precise definition of <em>consistent with equals</em>.)  This is so because the
 * {@code Map} interface is defined in terms of the {@code equals} operation, but a sorted map
 * performs all key comparisons using its {@code compareTo} (or {@code compare}) method, so two keys
 * that are deemed equal by this method are, from the standpoint of the sorted map, equal.  The
 * behavior of a sorted map <em>is</em> well-defined even if its ordering is inconsistent with
 * {@code equals}; it just fails to obey the general contract of the {@code Map} interface.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong> If multiple threads access
 * a map concurrently, and at least one of the threads modifies the map structurally, it
 * <em>must</em> be synchronized externally.  (A structural modification is any operation that adds
 * or deletes one or more mappings; merely changing the value associated with an existing key is not
 * a structural modification.)  This is typically accomplished by synchronizing on some object that
 * naturally encapsulates the map. If no such object exists, the map should be "wrapped" using the
 * {@link Collections#synchronizedSortedMap Collections.synchronizedSortedMap} method.  This is best
 * done at creation time, to prevent accidental
 * unsynchronized access to the map: <pre>
 *   SortedMap m = Collections.synchronizedSortedMap(new TreeMap(...));</pre>
 *
 * <p>The iterators returned by the {@code iterator} method of the collections returned by all of
 * this class's "collection view methods" are <em>fail-fast</em>: if the map is structurally
 * modified at any time after the iterator is created, in any way except through the iterator's own
 * {@code remove} method, the iterator will throw a {@link ConcurrentModificationException}.  Thus,
 * in the face of concurrent modification, the iterator fails quickly and cleanly, rather than
 * risking arbitrary, non-deterministic behavior at an undetermined time in the future.
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed as it is, generally
 * speaking, impossible to make any hard guarantees in the presence of unsynchronized concurrent
 * modification.  Fail-fast iterators throw {@code ConcurrentModificationException} on a best-effort
 * basis. Therefore, it would be wrong to write a program that depended on this exception for its
 * correctness:   <em>the fail-fast behavior of iterators should be used only to detect bugs.</em>
 *
 * <p>All {@code Map.Entry} pairs returned by methods in this class and its views represent
 * snapshots of mappings at the time they were produced. They do <strong>not</strong> support the
 * {@code Entry.setValue} method. (Note however that it is possible to change mappings in the
 * associated map using {@code put}.)
 *
 * <p>This class is a member of the <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @author Josh Bloch and Doug Lea
 * @see Map
 * @see HashMap
 * @see Hashtable
 * @see Comparable
 * @see Comparator
 * @see Collection
 * @since 1.2
 */
@SuppressWarnings("all")
public class LongTreeMap<V>
    implements Cloneable, Serializable {

  /**
   * Dummy value serving as unmatchable fence key for unbounded SubMapIterators
   */
  private static final Object UNBOUNDED = new Object();
  private static final boolean RED = false;
  private static final boolean BLACK = true;
  private static final long serialVersionUID = 919286545866124006L;

  // Query Operations
  private transient Entry<V> root;
  /**
   * The number of entries in the tree
   */
  private transient int size = 0;
  /**
   * The number of structural modifications to the tree.
   */
  private transient int modCount = 0;

  /**
   * Constructs a new, empty tree map, using the natural ordering of its keys.  All keys inserted
   * into the map must implement the {@link Comparable} interface.  Furthermore, all such keys must
   * be <em>mutually comparable</em>: {@code k1.compareTo(k2)} must not throw a {@code
   * ClassCastException} for any keys {@code k1} and {@code k2} in the map.  If the user attempts to
   * put a key into the map that violates this constraint (for example, the user attempts to put a
   * string key into a map whose keys are integers), the {@code put(Object key, Object value)} call
   * will throw a {@code ClassCastException}.
   */
  public LongTreeMap() {
  }

  /**
   * Test two values for equality.  Differs from o1.equals(o2) only in that it copes with {@code
   * null} o1 properly.
   */
  static final boolean valEquals(Object o1, Object o2) {
    return (o1 == null ? o2 == null : o1.equals(o2));
  }

  /**
   * Return SimpleImmutableEntry for entry, or null if null
   */
  static <V> Entry<V> exportEntry(Entry<V> e) {
    return e;
  }

  /**
   * Return key for entry, or null if null
   */
  static <V> long keyOrNull(Entry<V> e) {
    return (e == null) ? 0 : e.key;
  }

  /**
   * Returns the key corresponding to the specified Entry.
   *
   * @throws NoSuchElementException if the Entry is null
   */
  static long key(Entry<?> e) {
    if (e == null) {
      return 0;
    }
    return e.key;
  }

  /**
   * Returns the successor of the specified Entry, or null if no such.
   */
  static <K, V> Entry<V> successor(Entry<V> t) {
    if (t == null) {
      return null;
    } else if (t.right != null) {
      Entry<V> p = t.right;
      while (p.left != null) {
        p = p.left;
      }
      return p;
    } else {
      Entry<V> p = t.parent;
      Entry<V> ch = t;
      while (p != null && ch == p.right) {
        ch = p;
        p = p.parent;
      }
      return p;
    }
  }

  /**
   * Returns the predecessor of the specified Entry, or null if no such.
   */
  static <K, V> Entry<V> predecessor(Entry<V> t) {
    if (t == null) {
      return null;
    } else if (t.left != null) {
      Entry<V> p = t.left;
      while (p.right != null) {
        p = p.right;
      }
      return p;
    } else {
      Entry<V> p = t.parent;
      Entry<V> ch = t;
      while (p != null && ch == p.left) {
        ch = p;
        p = p.parent;
      }
      return p;
    }
  }

  /**
   * Balancing operations.
   *
   * Implementations of rebalancings during insertion and deletion are slightly different than the
   * CLR version.  Rather than using dummy nilnodes, we use a set of accessors that deal properly
   * with null.  They are used to avoid messiness surrounding nullness checks in the main
   * algorithms.
   */

  private static <K, V> boolean colorOf(Entry<V> p) {
    return (p == null ? BLACK : p.color);
  }

  private static <K, V> Entry<V> parentOf(Entry<V> p) {
    return (p == null ? null : p.parent);
  }

  private static <K, V> void setColor(Entry<V> p, boolean c) {
    if (p != null) {
      p.color = c;
    }
  }

  private static <K, V> Entry<V> leftOf(Entry<V> p) {
    return (p == null) ? null : p.left;
  }

  // NavigableMap API methods

  private static <K, V> Entry<V> rightOf(Entry<V> p) {
    return (p == null) ? null : p.right;
  }

  /**
   * Find the level down to which to assign all nodes BLACK.  This is the last `full' level of the
   * complete binary tree produced by buildTree. The remaining nodes are colored RED. (This makes a
   * `nice' set of color assignments wrt future insertions.) This level number is computed by
   * finding the number of splits needed to reach the zeroeth node.  (The answer is ~lg(N), but in
   * any case must be computed by same quick O(lg(N)) loop.)
   */
  private static int computeRedLevel(int sz) {
    int level = 0;
    for (int m = sz - 1; m >= 0; m = m / 2 - 1) {
      level++;
    }
    return level;
  }

  /**
   * Returns the number of key-value mappings in this map.
   *
   * @return the number of key-value mappings in this map
   */
  public int size() {
    return size;
  }

  /**
   * Returns {@code true} if this map contains a mapping for the specified key.
   *
   * @param key key whose presence in this map is to be tested
   * @return {@code true} if this map contains a mapping for the specified key
   * @throws ClassCastException if the specified key cannot be compared with the keys currently in
   * the map
   * @throws NullPointerException if the specified key is null and this map uses natural ordering,
   * or its comparator does not permit null keys
   */
  public boolean containsKey(long key) {
    return getEntry(key) != null;
  }

  /**
   * Returns {@code true} if this map maps one or more keys to the specified value.  More formally,
   * returns {@code true} if and only if this map contains at least one mapping to a value {@code v}
   * such that {@code (value==null ? v==null : value.equals(v))}.  This operation will probably
   * require time linear in the map size for most implementations.
   *
   * @param value value whose presence in this map is to be tested
   * @return {@code true} if a mapping to {@code value} exists; {@code false} otherwise
   * @since 1.2
   */
  public boolean containsValue(Object value) {
    for (Entry<V> e = getFirstEntry(); e != null; e = successor(e)) {
      if (valEquals(value, e.value)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the value to which the specified key is mapped, or {@code null} if this map contains no
   * mapping for the key.
   *
   * <p>More formally, if this map contains a mapping from a key {@code k} to a value {@code v} such
   * that {@code key} compares equal to {@code k} according to the map's ordering, then this method
   * returns {@code v}; otherwise it returns {@code null}. (There can be at most one such mapping.)
   *
   * <p>A return value of {@code null} does not <em>necessarily</em> indicate that the map contains
   * no mapping for the key; it's also possible that the map explicitly maps the key to {@code
   * null}. The {@link #containsKey containsKey} operation may be used to distinguish these two
   * cases.
   *
   * @throws ClassCastException if the specified key cannot be compared with the keys currently in
   * the map
   * @throws NullPointerException if the specified key is null and this map uses natural ordering,
   * or its comparator does not permit null keys
   */
  public V get(long key) {
    Entry<V> p = getEntry(key);
    return (p == null ? null : p.value);
  }

  /**
   * @throws NoSuchElementException {@inheritDoc}
   */
  public long firstKey() {
    return key(getFirstEntry());
  }

  /**
   * @throws NoSuchElementException {@inheritDoc}
   */
  public long lastKey() {
    return key(getLastEntry());
  }

  /**
   * Returns this map's entry for the given key, or {@code null} if the map does not contain an
   * entry for the key.
   *
   * @return this map's entry for the given key, or {@code null} if the map does not contain an
   * entry for the key
   * @throws ClassCastException if the specified key cannot be compared with the keys currently in
   * the map
   * @throws NullPointerException if the specified key is null and this map uses natural ordering,
   * or its comparator does not permit null keys
   */
  final Entry<V> getEntry(long key) {
    Entry<V> p = root;
    while (p != null) {
      int cmp = Long.compare(key, p.key);
      if (cmp < 0) {
        p = p.left;
      } else if (cmp > 0) {
        p = p.right;
      } else {
        return p;
      }
    }
    return null;
  }

  /**
   * Version of getEntry using comparator. Split off from getEntry for performance. (This is not
   * worth doing for most methods, that are less dependent on comparator performance, but is
   * worthwhile here.)
   */
  final Entry<V> getEntryUsingComparator(long key) {
    Entry<V> p = root;
    while (p != null) {
      int cmp = Long.compare(key, p.key);
      if (cmp < 0) {
        p = p.left;
      } else if (cmp > 0) {
        p = p.right;
      } else {
        return p;
      }
    }
    return null;
  }

  /**
   * Gets the entry corresponding to the specified key; if no such entry exists, returns the entry
   * for the least key greater than the specified key; if no such entry exists (i.e., the greatest
   * key in the Tree is less than the specified key), returns {@code null}.
   */
  final Entry<V> getCeilingEntry(long key) {
    Entry<V> p = root;
    while (p != null) {
      int cmp = compare(key, p.key);
      if (cmp < 0) {
        if (p.left != null) {
          p = p.left;
        } else {
          return p;
        }
      } else if (cmp > 0) {
        if (p.right != null) {
          p = p.right;
        } else {
          Entry<V> parent = p.parent;
          Entry<V> ch = p;
          while (parent != null && ch == parent.right) {
            ch = parent;
            parent = parent.parent;
          }
          return parent;
        }
      } else {
        return p;
      }
    }
    return null;
  }

  /**
   * Gets the entry corresponding to the specified key; if no such entry exists, returns the entry
   * for the greatest key less than the specified key; if no such entry exists, returns {@code
   * null}.
   */
  final Entry<V> getFloorEntry(long key) {
    Entry<V> p = root;
    while (p != null) {
      int cmp = compare(key, p.key);
      if (cmp > 0) {
        if (p.right != null) {
          p = p.right;
        } else {
          return p;
        }
      } else if (cmp < 0) {
        if (p.left != null) {
          p = p.left;
        } else {
          Entry<V> parent = p.parent;
          Entry<V> ch = p;
          while (parent != null && ch == parent.left) {
            ch = parent;
            parent = parent.parent;
          }
          return parent;
        }
      } else {
        return p;
      }

    }
    return null;
  }

  /**
   * Gets the entry for the least key greater than the specified key; if no such entry exists,
   * returns the entry for the least key greater than the specified key; if no such entry exists
   * returns {@code null}.
   */
  final Entry<V> getHigherEntry(long key) {
    Entry<V> p = root;
    while (p != null) {
      int cmp = compare(key, p.key);
      if (cmp < 0) {
        if (p.left != null) {
          p = p.left;
        } else {
          return p;
        }
      } else {
        if (p.right != null) {
          p = p.right;
        } else {
          Entry<V> parent = p.parent;
          Entry<V> ch = p;
          while (parent != null && ch == parent.right) {
            ch = parent;
            parent = parent.parent;
          }
          return parent;
        }
      }
    }
    return null;
  }

  // Views

  /**
   * Returns the entry for the greatest key less than the specified key; if no such entry exists
   * (i.e., the least key in the Tree is greater than the specified key), returns {@code null}.
   */
  final Entry<V> getLowerEntry(long key) {
    Entry<V> p = root;
    while (p != null) {
      int cmp = compare(key, p.key);
      if (cmp > 0) {
        if (p.right != null) {
          p = p.right;
        } else {
          return p;
        }
      } else {
        if (p.left != null) {
          p = p.left;
        } else {
          Entry<V> parent = p.parent;
          Entry<V> ch = p;
          while (parent != null && ch == parent.left) {
            ch = parent;
            parent = parent.parent;
          }
          return parent;
        }
      }
    }
    return null;
  }

  /**
   * Associates the specified value with the specified key in this map. If the map previously
   * contained a mapping for the key, the old value is replaced.
   *
   * @param key key with which the specified value is to be associated
   * @param value value to be associated with the specified key
   * @return the previous value associated with {@code key}, or {@code null} if there was no mapping
   * for {@code key}. (A {@code null} return can also indicate that the map previously associated
   * {@code null} with {@code key}.)
   * @throws ClassCastException if the specified key cannot be compared with the keys currently in
   * the map
   * @throws NullPointerException if the specified key is null and this map uses natural ordering,
   * or its comparator does not permit null keys
   */
  public V put(long key, V value) {
    Entry<V> t = root;
    if (t == null) {
      root = new Entry<>(key, value, null);
      size = 1;
      modCount++;
      return null;
    }
    int cmp;
    Entry<V> parent;
    do {
      parent = t;
      cmp = Long.compare(key, t.key);
      if (cmp < 0) {
        t = t.left;
      } else if (cmp > 0) {
        t = t.right;
      } else {
        return t.setValue(value);
      }
    } while (t != null);

    Entry<V> e = new Entry<>(key, value, parent);
    if (cmp < 0) {
      parent.left = e;
    } else {
      parent.right = e;
    }
    fixAfterInsertion(e);
    size++;
    return null;
  }

  /**
   * Removes the mapping for this key from this TreeMap if present.
   *
   * @param key key for which mapping should be removed
   * @return the previous value associated with {@code key}, or {@code null} if there was no mapping
   * for {@code key}. (A {@code null} return can also indicate that the map previously associated
   * {@code null} with {@code key}.)
   * @throws ClassCastException if the specified key cannot be compared with the keys currently in
   * the map
   * @throws NullPointerException if the specified key is null and this map uses natural ordering,
   * or its comparator does not permit null keys
   */
  public V remove(long key) {
    Entry<V> p = getEntry(key);
    if (p == null) {
      return null;
    }

    V oldValue = p.value;
    deleteEntry(p);
    return oldValue;
  }

  /**
   * Removes all of the mappings from this map. The map will be empty after this call returns.
   */
  public void clear() {
    size = 0;
    root = null;
  }

  /**
   * @since 1.6
   */
  public Entry<V> firstEntry() {
    return getFirstEntry();
  }

  /**
   * @since 1.6
   */
  public Entry<V> lastEntry() {
    return getLastEntry();
  }

  // View class support

  public void delete(Entry<V> entry) {
    deleteEntry(entry);
  }

  /**
   * @since 1.6
   */
  public Entry<V> pollFirstEntry() {
    Entry<V> p = getFirstEntry();
    if (p != null) {
      deleteEntry(p);
    }
    return p;
  }

  public V pollFirstEntryIfLessThan(long ceil) {
    Entry<V> p = getFirstEntry();
    if (p != null) {
      final V v = p.value;
      if (p.getKey() < ceil) {
        deleteEntry(p);
      }
      return v;
    }
    return null;
  }

    /*
     * Unlike Values and EntrySet, the KeySet class is static,
     * delegating to a NavigableMap to allow use by SubMaps, which
     * outweighs the ugliness of needing type-tests for the following
     * Iterator methods that are defined appropriately in main versus
     * submap classes.
     */

  /**
   * @since 1.6
   */
  public Entry<V> pollLastEntry() {
    Entry<V> p = getLastEntry();
    if (p != null) {
      deleteEntry(p);
    }
    return p;
  }

  /**
   * @throws ClassCastException {@inheritDoc}
   * @throws NullPointerException if the specified key is null and this map uses natural ordering,
   * or its comparator does not permit null keys
   * @since 1.6
   */
  public Entry<V> lowerEntry(long key) {
    return getLowerEntry(key);
  }

  /**
   * @throws ClassCastException {@inheritDoc}
   * @throws NullPointerException if the specified key is null and this map uses natural ordering,
   * or its comparator does not permit null keys
   * @since 1.6
   */
  public long lowerKey(long key) {
    return keyOrNull(getLowerEntry(key));
  }

  /**
   * @throws ClassCastException {@inheritDoc}
   * @throws NullPointerException if the specified key is null and this map uses natural ordering,
   * or its comparator does not permit null keys
   * @since 1.6
   */
  public Entry<V> floorEntry(long key) {
    return exportEntry(getFloorEntry(key));
  }

  /**
   * @throws ClassCastException {@inheritDoc}
   * @throws NullPointerException if the specified key is null and this map uses natural ordering,
   * or its comparator does not permit null keys
   * @since 1.6
   */
  public long floorKey(long key) {
    return keyOrNull(getFloorEntry(key));
  }

  /**
   * @throws ClassCastException {@inheritDoc}
   * @throws NullPointerException if the specified key is null and this map uses natural ordering,
   * or its comparator does not permit null keys
   * @since 1.6
   */
  public Entry<V> ceilingEntry(long key) {
    return exportEntry(getCeilingEntry(key));
  }

  /**
   * @throws ClassCastException {@inheritDoc}
   * @throws NullPointerException if the specified key is null and this map uses natural ordering,
   * or its comparator does not permit null keys
   * @since 1.6
   */
  public long ceilingKey(long key) {
    return keyOrNull(getCeilingEntry(key));
  }

  // Little utilities

  /**
   * @throws ClassCastException {@inheritDoc}
   * @throws NullPointerException if the specified key is null and this map uses natural ordering,
   * or its comparator does not permit null keys
   * @since 1.6
   */
  public Entry<V> higherEntry(long key) {
    return exportEntry(getHigherEntry(key));
  }

  /**
   * @throws ClassCastException {@inheritDoc}
   * @throws NullPointerException if the specified key is null and this map uses natural ordering,
   * or its comparator does not permit null keys
   * @since 1.6
   */
  public long higherKey(long key) {
    return keyOrNull(getHigherEntry(key));
  }

  public boolean replace(long key, V oldValue, V newValue) {
    Entry<V> p = getEntry(key);
    if (p != null && Objects.equals(oldValue, p.value)) {
      p.value = newValue;
      return true;
    }
    return false;
  }

  public V replace(long key, V value) {
    Entry<V> p = getEntry(key);
    if (p != null) {
      V oldValue = p.value;
      p.value = value;
      return oldValue;
    }
    return null;
  }

  // SubMaps

  public void forEach(EntryConsumer<? super V> action) {
    Objects.requireNonNull(action);
    int expectedModCount = modCount;
    for (Entry<V> e = getFirstEntry(); e != null; e = successor(e)) {
      action.accept(e.key, e.value);

      if (expectedModCount != modCount) {
        throw new ConcurrentModificationException();
      }
    }
  }

  public void replaceAll(EntryBiFunction<? super V, ? extends V> function) {
    Objects.requireNonNull(function);
    int expectedModCount = modCount;

    for (Entry<V> e = getFirstEntry(); e != null; e = successor(e)) {
      e.value = function.apply(e.key, e.value);

      if (expectedModCount != modCount) {
        throw new ConcurrentModificationException();
      }
    }
  }

  /**
   * Compares two keys using the correct comparison method for this TreeMap.
   */
  final int compare(long k1, long k2) {
    return Long.compare(k1, k2);
  }

  /**
   * Returns the first Entry in the TreeMap (according to the TreeMap's key-sort function).  Returns
   * null if the TreeMap is empty.
   */
  public final Entry<V> getFirstEntry() {
    Entry<V> p = root;
    if (p != null) {
      while (p.left != null) {
        p = p.left;
      }
    }
    return p;
  }

  /**
   * Returns the last Entry in the TreeMap (according to the TreeMap's key-sort function).  Returns
   * null if the TreeMap is empty.
   */
  public final Entry<V> getLastEntry() {
    Entry<V> p = root;
    if (p != null) {
      while (p.right != null) {
        p = p.right;
      }
    }
    return p;
  }

  /**
   * From CLR
   */
  private void rotateLeft(Entry<V> p) {
    if (p != null) {
      Entry<V> r = p.right;
      p.right = r.left;
      if (r.left != null) {
        r.left.parent = p;
      }
      r.parent = p.parent;
      if (p.parent == null) {
        root = r;
      } else if (p.parent.left == p) {
        p.parent.left = r;
      } else {
        p.parent.right = r;
      }
      r.left = p;
      p.parent = r;
    }
  }

  /**
   * From CLR
   */
  private void rotateRight(Entry<V> p) {
    if (p != null) {
      Entry<V> l = p.left;
      p.left = l.right;
      if (l.right != null) {
        l.right.parent = p;
      }
      l.parent = p.parent;
      if (p.parent == null) {
        root = l;
      } else if (p.parent.right == p) {
        p.parent.right = l;
      } else {
        p.parent.left = l;
      }
      l.right = p;
      p.parent = l;
    }
  }

  /**
   * From CLR
   */
  private void fixAfterInsertion(Entry<V> x) {
    x.color = RED;

    while (x != null && x != root && x.parent.color == RED) {
      if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
        Entry<V> y = rightOf(parentOf(parentOf(x)));
        if (colorOf(y) == RED) {
          setColor(parentOf(x), BLACK);
          setColor(y, BLACK);
          setColor(parentOf(parentOf(x)), RED);
          x = parentOf(parentOf(x));
        } else {
          if (x == rightOf(parentOf(x))) {
            x = parentOf(x);
            rotateLeft(x);
          }
          setColor(parentOf(x), BLACK);
          setColor(parentOf(parentOf(x)), RED);
          rotateRight(parentOf(parentOf(x)));
        }
      } else {
        Entry<V> y = leftOf(parentOf(parentOf(x)));
        if (colorOf(y) == RED) {
          setColor(parentOf(x), BLACK);
          setColor(y, BLACK);
          setColor(parentOf(parentOf(x)), RED);
          x = parentOf(parentOf(x));
        } else {
          if (x == leftOf(parentOf(x))) {
            x = parentOf(x);
            rotateRight(x);
          }
          setColor(parentOf(x), BLACK);
          setColor(parentOf(parentOf(x)), RED);
          rotateLeft(parentOf(parentOf(x)));
        }
      }
    }
    root.color = BLACK;
  }

  /**
   * Delete node p, and then rebalance the tree.
   */
  private void deleteEntry(Entry<V> p) {
    modCount++;
    size--;

    // If strictly internal, copy successor's element to p and then make p
    // point to successor.
    if (p.left != null && p.right != null) {
      Entry<V> s = successor(p);
      p.key = s.key;
      p.value = s.value;
      p = s;
    } // p has 2 children

    // Start fixup at replacement node, if it exists.
    Entry<V> replacement = (p.left != null ? p.left : p.right);

    if (replacement != null) {
      // Link replacement to parent
      replacement.parent = p.parent;
      if (p.parent == null) {
        root = replacement;
      } else if (p == p.parent.left) {
        p.parent.left = replacement;
      } else {
        p.parent.right = replacement;
      }

      // Null out links so they are OK to use by fixAfterDeletion.
      p.left = p.right = p.parent = null;

      // Fix replacement
      if (p.color == BLACK) {
        fixAfterDeletion(replacement);
      }
    } else if (p.parent == null) { // return if we are the only node.
      root = null;
    } else { //  No children. Use self as phantom replacement and unlink.
      if (p.color == BLACK) {
        fixAfterDeletion(p);
      }

      if (p.parent != null) {
        if (p == p.parent.left) {
          p.parent.left = null;
        } else if (p == p.parent.right) {
          p.parent.right = null;
        }
        p.parent = null;
      }
    }
  }

  /**
   * From CLR
   */
  private void fixAfterDeletion(Entry<V> x) {
    while (x != root && colorOf(x) == BLACK) {
      if (x == leftOf(parentOf(x))) {
        Entry<V> sib = rightOf(parentOf(x));

        if (colorOf(sib) == RED) {
          setColor(sib, BLACK);
          setColor(parentOf(x), RED);
          rotateLeft(parentOf(x));
          sib = rightOf(parentOf(x));
        }

        if (colorOf(leftOf(sib)) == BLACK &&
            colorOf(rightOf(sib)) == BLACK) {
          setColor(sib, RED);
          x = parentOf(x);
        } else {
          if (colorOf(rightOf(sib)) == BLACK) {
            setColor(leftOf(sib), BLACK);
            setColor(sib, RED);
            rotateRight(sib);
            sib = rightOf(parentOf(x));
          }
          setColor(sib, colorOf(parentOf(x)));
          setColor(parentOf(x), BLACK);
          setColor(rightOf(sib), BLACK);
          rotateLeft(parentOf(x));
          x = root;
        }
      } else { // symmetric
        Entry<V> sib = leftOf(parentOf(x));

        if (colorOf(sib) == RED) {
          setColor(sib, BLACK);
          setColor(parentOf(x), RED);
          rotateRight(parentOf(x));
          sib = leftOf(parentOf(x));
        }

        if (colorOf(rightOf(sib)) == BLACK &&
            colorOf(leftOf(sib)) == BLACK) {
          setColor(sib, RED);
          x = parentOf(x);
        } else {
          if (colorOf(leftOf(sib)) == BLACK) {
            setColor(rightOf(sib), BLACK);
            setColor(sib, RED);
            rotateLeft(sib);
            sib = leftOf(parentOf(x));
          }
          setColor(sib, colorOf(parentOf(x)));
          setColor(parentOf(x), BLACK);
          setColor(leftOf(sib), BLACK);
          rotateRight(parentOf(x));
          x = root;
        }
      }
    }

    setColor(x, BLACK);
  }

  interface EntryConsumer<V> {

    void accept(long key, V value);
  }

  // Red-black mechanics
  interface EntryBiFunction<V1, V2> {

    V2 apply(long key, V1 value);
  }

  /**
   * Node in the Tree.  Doubles as a means to pass key-value pairs back to user (see Map.Entry).
   */

  public static final class Entry<V> {

    long key;
    V value;
    Entry<V> left;
    Entry<V> right;
    Entry<V> parent;
    boolean color = BLACK;

    /**
     * Make a new cell with given key, value, and parent, and with {@code null} child links, and
     * BLACK color.
     */
    Entry(long key, V value, Entry<V> parent) {
      this.key = key;
      this.value = value;
      this.parent = parent;
    }

    /**
     * Returns the key.
     *
     * @return the key
     */
    public long getKey() {
      return key;
    }

    /**
     * Returns the value associated with the key.
     *
     * @return the value associated with the key
     */
    public V getValue() {
      return value;
    }

    /**
     * Replaces the value currently associated with the key with the given value.
     *
     * @return the value associated with the key before this method was called
     */
    public V setValue(V value) {
      V oldValue = this.value;
      this.value = value;
      return oldValue;
    }

    public boolean equals(Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;

      return valEquals(key, e.getKey()) && valEquals(value, e.getValue());
    }

    public int hashCode() {
      int keyHash = Long.hashCode(key);
      int valueHash = (value == null ? 0 : value.hashCode());
      return keyHash ^ valueHash;
    }

    public String toString() {
      return key + "=" + value;
    }
  }
}
