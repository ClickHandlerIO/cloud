/**
 * License: GPL with Classpath exception (see below). This file is based on ConcurrentSkipListMap in
 * standard java library, but the class is optimized for primitive long key type.
 *
 * Some of the methods in ConcurrentSkipListMap are not supported. LongSkipListMap supports the
 * following APIs:
 *
 * - Map API methods public boolean containsKey(long key) public V get(long key) public V put(long
 * key, V value) public V remove(long key) public boolean containsValue(Object value) public int
 * size() public boolean isEmpty() public void clear()
 *
 * - ConcurrentMap API methods public V putIfAbsent(long key, V value) public boolean remove(long
 * key, Object value) public boolean replace(long key, V oldValue, V newValue) public V replace(long
 * key, V value) public long firstKey() public long lastKey()
 *
 * - Relational operations public MapEntry.LongKeyEntry<V> lowerEntry(long key) public long
 * lowerKey(long key) public MapEntry.LongKeyEntry<V> floorEntry(long key) public long floorKey(long
 * key) { public MapEntry.LongKeyEntry<V> ceilingEntry(long key) public long ceilingKey(long key) {
 * public MapEntry.LongKeyEntry<V> higherEntry(long key) public long higherKey(long key)
 */

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 */

package move.action;

import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Modified version of ConcurrentSkipListMap. The "Concurrent" pieces were mostly removed or
 * simplified and the key is a "long" primitive to save boxing operations and memory
 * and GC pressure. This class is "Not" Thread-Safe.
 *
 * This Map is intended to be used within a single thread and can efficiently keep keys in ascending
 * sorted order. Descending or "tail" operations are much more expensive and not advised. If
 * descending order is desired perhaps create another instance and invert the keys.
 *
 * This map is extremely efficient at removing the first key based on a simple compare.
 *
 * @param <V> Value Type
 * @author Clay Molocznik
 */
public class LongSkipListMap<V> {

  private static final Random seedGenerator = new Random();
  private static final Object BASE_HEADER = new Object();
  private static final int EQ = 1;
  private static final int LT = 2;
  private static final int GT = 0; // Actually checked as !LT
  static NullCallback nullCallback = new NullCallback();
  private volatile HeadIndex<V> head;
  /**
   * Seed for simple random number generator.  Not volatile since it doesn't matter too much if
   * different threads don't see updates.
   */
  private transient int randomSeed;
  public LongSkipListMap() {
    initialize();
  }

  public static void main(String[] args) {
//    {
//      final LongSkipListMap<Long> cMap = new LongSkipListMap<>();
//
//      long start = System.currentTimeMillis();
//      for (int x = 0; x < 100; x++) {
//        long _start = System.currentTimeMillis();
//        cMap.clear();
//        for (long i = 0; i < 1000000; i++) {
//          cMap.put(i, i);
//        }
//        System.out.println(System.currentTimeMillis() - _start);
//      }
//      long end = System.currentTimeMillis() - start;
//    }
    {
      final LongHashMap<Long> cMap = new LongHashMap<>();

      final Long value = new Long(0L);
      long start = System.currentTimeMillis();
      for (int x = 0; x < 100; x++) {
        long _start = System.currentTimeMillis();
        cMap.clear();
        for (long i = 0; i < 1000000; i++) {
          cMap.put(i, value);
        }
        System.out.println(System.currentTimeMillis() - _start);
      }
      long end = System.currentTimeMillis() - start;
    }

  }

    /* ---------------- Nodes -------------- */

  /**
   * Initializes or resets state. Needed by constructors, clone, clear, readObject. and
   * ConcurrentSkipListSet.clone. (Note that comparator must be separately initialized.)
   */
  final void initialize() {
    randomSeed = seedGenerator.nextInt() | 0x0100; // ensure nonzero
    head = new HeadIndex<>(new Node<>(Integer.MIN_VALUE, BASE_HEADER, null),
        null, null, 1);
  }

    /* ---------------- Indexing -------------- */

  private boolean casHead(HeadIndex<V> cmp, HeadIndex<V> val) {
    // Single threaded so this always succeeds.
    head = val;
    return true;
  }

    /* ---------------- Head nodes -------------- */

  int compare(long k1, long k2) {
    if (k1 > k2) {
      return 1;
    } else if (k1 == k2) {
      return 0;
    } else {
      return -1;
    }
  }

    /* ---------------- Comparison utilities -------------- */

  boolean inHalfOpenRange(long key, long least, long fence) {
    return (compare(key, least) >= 0 && compare(key, fence) < 0);
  }

  boolean inOpenRange(long key, long least, long fence) {
    return (compare(key, least) >= 0 && compare(key, fence) <= 0);
  }

  public V findGrandPredecessor(long key, CallbackOnFound callback) {
    V v = findGrandPredecessorReally(key, callback);
    if (v == BASE_HEADER) {
      return null;
    }
    return v;
  }

    /* ---------------- Traversal -------------- */

  public V findGrandPredecessor(long key) {
    V v = findGrandPredecessorReally(key, nullCallback);
    if (v == BASE_HEADER) {
      return null;
    }
    return v;
  }

  /**
   * If the specified key is not already associated with a value, attempts to compute its value
   * using the given mapping function and enters it into this map unless {@code null}.  The function
   * is <em>NOT</em> guaranteed to be applied once atomically only if the value is not present.
   *
   * @param key key with which the specified value is to be associated
   * @param mappingFunction the function to compute a value
   * @return the current (existing or computed) value associated with the specified key, or null if
   * the computed value is null
   * @throws NullPointerException if the specified key is null or the mappingFunction is null
   * @since 1.8
   */
  public V computeIfAbsent(long key,
      KeyMapping<V> mappingFunction) {
    V v, p, r;
    if ((v = doGet(key)) == null &&
        (r = mappingFunction.apply(key)) != null) {
      v = (p = doPut(key, r, true)) == null ? r : p;
    }
    return v;
  }

  public V compute(long key,
      KeyMapping<V> mappingFunction,
      Consumer<V> existingFunction) {
    V v;

    v = doGet(key);

    if (v == null) {
      v = mappingFunction.apply(key);
      doPut(key, v, false);
      return v;
    } else {
      existingFunction.accept(v);
      return v;
    }
  }

  V findGrandPredecessorReally(long key, CallbackOnFound callback) {
    for (; ; ) {
      Node<V> b = findGrandPredecessorWithIndex(key);
      Node<V> n = b.next;
      for (; ; ) {
        if (n == null) {
          return null;
        }

        Node<V> f = n.next;
        long nkey = n.key;
        Object nval = n.value;
        long bkey = b.key;
        Object bval = b.value;
        if (n != b.next) {
          break;    // inconsistent read
        }
        if (nval == null) {        // n is deleted
          n.helpDelete(b, f);
          break;
        }
        if (nval == n || bval == null) {
          break; // b is deleted
        }
        if (f == null) {
          if (nkey > key) {
            return null;
          } else {
            if (bval == BASE_HEADER) {
              return (V) bval;
            }
            callback.found(bval);
            if (b.next != n || bval != b.value) {  // inconsistent read
              callback.cancel(bval);
              break;
            }
            return (V) bval;
          }
        }
        Object fv = f.value;
        long fkey = f.key;
        if (fv == null) {
          break;    // f is deleted
        }
        if (fv == f) {
          break;       // n is deleted
        }
        if (key <= fkey) {
          if (bval == BASE_HEADER) {
            return (V) bval;
          }
          callback.found((V) bval);
          if (b.next != n || n.next != f || bval != b.value) {  // inconsistent read
            callback.cancel((V) bval);
            break;
          }
          return (V) bval;
        }

        b = n;
        n = f;
      }
    }
  }

  Node<V> findGrandPredecessorWithIndex(long key) {
    for (; ; ) {
      Index<V> q = head;
      Index<V> r = q.right;
      for (; ; ) {
        if (r != null) {
          Node<V> n = r.node;
          if (n.value == null) {
            if (!q.unlink(r)) {
              break;          // restart
            }
            r = q.right;        // reread r
            continue;
          }
          Node<V> f = n.next;
          if (f != null) {
            long fkey = f.key;
            V fval = (V) f.value;
            if (fval == null) {
              break; // f is deleted, restart
            } else if (fval == f) {
              break; // n is deleted, restart
            } else if (key > fkey) {
              q = r;
              r = r.right;
              continue;
            } /* else go down one level */
          }
        }
        Index<V> d = q.down;
        if (d != null) {
          q = d;
          r = d.right;
        } else {
          return q.node;
        }
      }
    }
  }

  private Node<V> findPredecessor(long key) {
    for (; ; ) {
      Index<V> q = head;
      Index<V> r = q.right;
      for (; ; ) {
        if (r != null) {
          Node<V> n = r.node;
          long k = n.key;
          if (n.value == null) {
            if (!q.unlink(r)) {
              break;           // restart
            }
            r = q.right;         // reread r
            continue;
          }
          if (key > k) {
            q = r;
            r = r.right;
            continue;
          }
        }
        Index<V> d = q.down;
        if (d != null) {
          q = d;
          r = d.right;
        } else {
          return q.node;
        }
      }
    }
  }

  private Node<V> findNode(long key) {
    for (; ; ) {
      Node<V> b = findPredecessor(key);
      Node<V> n = b.next;
      for (; ; ) {
        if (n == null) {
          return null;
        }
        Node<V> f = n.next;
        if (n != b.next)                // inconsistent read
        {
          break;
        }
        Object v = n.value;
        if (v == null) {                // n is deleted
          n.helpDelete(b, f);
          break;
        }
        if (v == n || b.value == null)  // b is deleted
        {
          break;
        }
        if (key == n.key) {
          return n;
        }
        if (key < n.key) {
          return null;
        }
        b = n;
        n = f;
      }
    }
  }

  private V doGet(long okey) {
    long key = okey;
    Node<V> bound = null;
    Index<V> q = head;
    Index<V> r = q.right;
    Node<V> n;
    long k;
    for (; ; ) {
      Index<V> d;
      // Traverse rights
      if (r != null && (n = r.node) != bound && !n.isMarker() && !n.isBaseHeader()) {
        k = n.key;
        if (key > k) {
          q = r;
          r = r.right;
          continue;
        } else if (key == k) {
          Object v = n.value;
          return (v != null) ? (V) v : getUsingFindNode(key);
        } else {
          bound = n;
        }
      }

      // Traverse down
      if ((d = q.down) != null) {
        q = d;
        r = d.right;
      } else {
        break;
      }
    }

    // Traverse nexts
    for (n = q.node.next; n != null; n = n.next) {
      if (!n.isMarker() && !n.isBaseHeader()) {
        k = n.key;
        if (key == k) {
          Object v = n.value;
          return (v != null) ? (V) v : getUsingFindNode(key);
        } else if (key < k) {
          break;
        }
      }
    }
    return null;
  }

  private V getUsingFindNode(long key) {
    for (; ; ) {
      Node<V> n = findNode(key);
      if (n == null) {
        return null;
      }
      Object v = n.value;
      if (v != null) {
        return (V) v;
      }
    }
  }

  private V doPut(long kkey, V value, boolean onlyIfAbsent) {
    long key = kkey;
    for (; ; ) {
      Node<V> b = findPredecessor(key);
      Node<V> n = b.next;
      for (; ; ) {
        if (n != null) {
          Node<V> f = n.next;
          Object v = n.value;
          if (v == null) {               // n is deleted
            n.helpDelete(b, f);
            break;
          }
          if (v == n || b.value == null) // b is deleted
          {
            break;
          }
          if (key > n.key) {
            b = n;
            n = f;
            continue;
          }
          if (key == n.key) {
            if (onlyIfAbsent || n.casValue(v, value)) {
              return (V) v;
            } else {
              break; // restart if lost race to replace value
            }
          }
          // else c < 0; fall through
        }

        Node<V> z = new Node<>(kkey, value, n);
        b.casNext(n, z);
        int level = randomLevel();
        if (level > 0) {
          insertIndex(z, level);
        }
        return null;
      }
    }
  }

  private int randomLevel() {
    int x = randomSeed;
    x ^= x << 13;
    x ^= x >>> 17;
    randomSeed = x ^= x << 5;
    if ((x & 0x8001) != 0) // test highest and lowest bits
    {
      return 0;
    }
    int level = 1;
    while (((x >>>= 1) & 1) != 0) {
      ++level;
    }
    return level;
  }

  private void insertIndex(Node<V> z, int level) {
    HeadIndex<V> h = head;
    int max = h.level;

    if (level <= max) {
      Index<V> idx = null;
      for (int i = 1; i <= level; ++i) {
        idx = new Index<V>(z, idx, null);
      }
      addIndex(idx, h, level);

    } else { // Add a new level
            /*
             * To reduce interference by other threads checking for
             * empty levels in tryReduceLevel, new levels are added
             * with initialized right pointers. Which in turn requires
             * keeping levels in an array to access them while
             * creating new head index nodes from the opposite
             * direction.
             */
      level = max + 1;
      Index<V>[] idxs = (Index<V>[]) new Index[level + 1];
      Index<V> idx = null;
      for (int i = 1; i <= level; ++i) {
        idxs[i] = idx = new Index<V>(z, idx, null);
      }

      HeadIndex<V> oldh;
      int _level;
      for (; ; ) {
        oldh = head;
        int oldLevel = oldh.level;
        if (level <= oldLevel) { // lost race to add level
          _level = level;
          break;
        }
        HeadIndex<V> newh = oldh;
        Node<V> oldbase = oldh.node;
        for (int j = oldLevel + 1; j <= level; ++j) {
          newh = new HeadIndex<V>(oldbase, newh, idxs[j], j);
        }
        if (casHead(oldh, newh)) {
          _level = oldLevel;
          break;
        }
      }
      addIndex(idxs[_level], oldh, _level);
    }
  }

  private void addIndex(Index<V> idx, HeadIndex<V> h, int indexLevel) {
    // Track next level to insert in case of retries
    int insertionLevel = indexLevel;
    long key = idx.node.key;

    // Similar to findPredecessor, but adding index nodes along
    // path to key.
    for (; ; ) {
      int j = h.level;
      Index<V> q = h;
      Index<V> r = q.right;
      Index<V> t = idx;
      for (; ; ) {
        if (r != null) {
          Node<V> n = r.node;
          // compare before deletion check avoids needing recheck
          if (n.value == null) {
            if (!q.unlink(r)) {
              break;
            }
            r = q.right;
            continue;
          }
          if (key > n.key) {
            q = r;
            r = r.right;
            continue;
          }
        }

        if (j == insertionLevel) {
          // Don't insert index if node already deleted
          if (t.indexesDeletedNode()) {
            findNode(key); // cleans up
            return;
          }
          if (!q.link(r, t)) {
            break; // restart
          }
          if (--insertionLevel == 0) {
            // need final deletion check before return
            if (t.indexesDeletedNode()) {
              findNode(key);
            }
            return;
          }
        }

        if (--j >= insertionLevel && j < indexLevel) {
          t = t.down;
        }
        q = q.down;
        r = q.right;
      }
    }
  }

  /**
   * Main deletion method. Locates node, nulls value, appends a deletion marker, unlinks
   * predecessor, removes associated index nodes, and possibly reduces head index level.
   *
   * Index nodes are cleared out simply by calling findPredecessor. which unlinks indexes to deleted
   * nodes found along path to key, which will include the indexes to this node.  This is done
   * unconditionally. We can't check beforehand whether there are index nodes because it might be
   * the case that some or all indexes hadn't been inserted yet for this node during initial search
   * for it, and we'd like to ensure lack of garbage retention, so must call to be sure.
   *
   * @param key the key
   * @param value if non-null, the value that must be associated with key
   * @return the node, or null if not found
   */
  final V doRemove(long key, V value) {
//    outer:
//    for (; ; ) {
    for (Node<V> b = findPredecessor(key), n = b.next; ; ) {
      Object v;
      int c;
      if (n == null) {
        return null;
      }
      Node<V> f = n.next;
      if (n != b.next)                    // inconsistent read
      {
        break;
      }
      if ((v = n.value) == null) {        // n is deleted
        n.helpDelete(b, f);
        break;
      }
      if (b.value == null || v == n)      // b is deleted
      {
        break;
      }
      if ((c = Long.compare(key, n.key)) < 0) {
        return null;
      }
      if (c > 0) {
        b = n;
        n = f;
        continue;
      }
      if (value != null && !value.equals(v)) {
        return null;
      }
      if (!n.casValue(v, null)) {
        break;
      }
      if (!n.appendMarker(f) || !b.casNext(n, f)) {
        findNode(key);                  // retry via findNode
      } else {
        findPredecessor(key);      // clean index
        if (head.right == null) {
          tryReduceLevel();
        }
      }
      @SuppressWarnings("unchecked") V vv = (V) v;
      return vv;
    }
//    }
    return null;
  }

  private void tryReduceLevel() {
    HeadIndex<V> h = head;
    HeadIndex<V> d;
    HeadIndex<V> e;
    if (h.level > 3 &&
        (d = (HeadIndex<V>) h.down) != null &&
        (e = (HeadIndex<V>) d.down) != null &&
        e.right == null &&
        d.right == null &&
        h.right == null &&
        casHead(h, d) && // try to set
        h.right != null) // recheck
    {
      casHead(d, h);   // try to backout
    }
  }

    /* ---------------- Deletion -------------- */

  public Node<V> findFirst() {
    for (; ; ) {
      Node<V> b = head.node;
      Node<V> n = b.next;
      if (n == null) {
        return null;
      }
      if (n.value != null) {
        return n;
      }
      n.helpDelete(b, n.next);
    }
  }

  public V pollFirstEntryIfLessThan(long ceil) {
    for (; ; ) {
      Node<V> b = head.node;
      Node<V> n = b.next;
      if (n == null) {
        return null;
      }

      Node<V> f = n.next;
      Object v = n.value;
      if (v == null) {
        n.helpDelete(b, f);
        continue;
      }

      if (n.key > ceil) {
        return null;
      }

      if (n.value instanceof Node) {
        b.casNext(n, f);

        clearIndexToFirst();
        return null;
      }

      final V value = (V) n.value;

      n.casValue(v, null);
      n.appendMarker(f);
      b.casNext(n, f);
      clearIndexToFirst();

      return value;
    }
  }

    /* ---------------- Finding and removing first element -------------- */

  private void clearIndexToFirst() {
    for (; ; ) {
      Index<V> q = head;
      for (; ; ) {
        Index<V> r = q.right;
        if (r != null && r.indexesDeletedNode() && !q.unlink(r)) {
          break;
        }
        if ((q = q.down) == null) {
          if (head.right == null) {
            tryReduceLevel();
          }
          return;
        }
      }
    }
  }

  Node<V> findLast() {
        /*
         * findPredecessor can't be used to traverse index level
         * because this doesn't use comparisons.  So traversals of
         * both levels are folded together.
         */
    Index<V> q = head;
    for (; ; ) {
      Index<V> d, r;
      if ((r = q.right) != null) {
        if (r.indexesDeletedNode()) {
          q.unlink(r);
          q = head; // restart
        } else {
          q = r;
        }
      } else if ((d = q.down) != null) {
        q = d;
      } else {
        Node<V> b = q.node;
        Node<V> n = b.next;
        for (; ; ) {
          if (n == null) {
            return (b.isBaseHeader()) ? null : b;
          }
          Node<V> f = n.next;            // inconsistent read
          if (n != b.next) {
            break;
          }
          Object v = n.value;
          if (v == null) {                 // n is deleted
            n.helpDelete(b, f);
            break;
          }
          if (v == n || b.value == null)   // b is deleted
          {
            break;
          }
          b = n;
          n = f;
        }
        q = head; // restart
      }
    }
  }

  private Node<V> findPredecessorOfLast() {
    for (; ; ) {
      Index<V> q = head;
      for (; ; ) {
        Index<V> d, r;
        if ((r = q.right) != null) {
          if (r.indexesDeletedNode()) {
            q.unlink(r);
            break;    // must restart
          }
          // proceed as far across as possible without overshooting
          if (r.node.next != null) {
            q = r;
            continue;
          }
        }
        if ((d = q.down) != null) {
          q = d;
        } else {
          return q.node;
        }
      }
    }
  }


    /* ---------------- Finding and removing last element -------------- */

  MapEntry.LongKeyEntry<V> doRemoveLastEntry() {
    for (; ; ) {
      Node<V> b = findPredecessorOfLast();
      Node<V> n = b.next;
      if (n == null) {
        if (b.isBaseHeader())               // empty
        {
          return null;
        } else {
          continue; // all b's successors are deleted; retry
        }
      }
      for (; ; ) {
        Node<V> f = n.next;
        if (n != b.next)                    // inconsistent read
        {
          break;
        }
        Object v = n.value;
        if (v == null) {                    // n is deleted
          n.helpDelete(b, f);
          break;
        }
        if (v == n || b.value == null)      // b is deleted
        {
          break;
        }
        if (f != null) {
          b = n;
          n = f;
          continue;
        }
        if (!n.casValue(v, null)) {
          break;
        }
        long key = n.key;
        long ck = key;
        if (!n.appendMarker(f) || !b.casNext(n, f)) {
          findNode(ck);                  // Retry via findNode
        } else {
          findPredecessor(ck);           // Clean index
          if (head.right == null) {
            tryReduceLevel();
          }
        }
        return new MapEntry.LongKeyEntry<V>(key, (V) v);
      }
    }
  }

  Node<V> findNear(long kkey, int rel) {
    long key = kkey;
    for (; ; ) {
      Node<V> b = findPredecessor(key);
      Node<V> n = b.next;
      for (; ; ) {
        if (n == null) {
          return ((rel & LT) == 0 || b.isBaseHeader()) ? null : b;
        }
        Node<V> f = n.next;
        if (n != b.next)                  // inconsistent read
        {
          break;
        }
        Object v = n.value;
        if (v == null) {                  // n is deleted
          n.helpDelete(b, f);
          break;
        }
        if (v == n || b.value == null)    // b is deleted
        {
          break;
        }
        if ((key == n.key && (rel & EQ) != 0) ||
            (key < n.key && (rel & LT) == 0)) {
          return n;
        }
        if (key <= n.key && (rel & LT) != 0) {
          return (b.isBaseHeader()) ? null : b;
        }
        b = n;
        n = f;
      }
    }
  }

  MapEntry.LongKeyEntry<V> getNear(long key, int rel) {
    for (; ; ) {
      Node<V> n = findNear(key, rel);
      if (n == null) {
        return null;
      }
      MapEntry.LongKeyEntry<V> e = n.createSnapshot();
      if (e != null) {
        return e;
      }
    }
  }

    /* ---------------- Relational operations -------------- */

  // Control values OR'ed as arguments to findNear

  public boolean containsKey(long key) {
    return doGet(key) != null;
  }

  public V get(long key) {
    return doGet(key);
  }

  public V put(long key, V value) {
    return doPut(key, value, false);
  }

  public V remove(long key) {
    return doRemove(key, null);
  }

  public boolean containsValue(Object value) {
    if (value == null) {
      throw new NullPointerException();
    }
    for (Node<V> n = findFirst(); n != null; n = n.next) {
      V v = n.getValidValue();
      if (v != null && value.equals(v)) {
        return true;
      }
    }
    return false;
  }


    /* ---------------- Constructors -------------- */

  public int size() {
    long count = 0;
    for (Node<V> n = findFirst(); n != null; n = n.next) {
      if (n.getValidValue() != null) {
        ++count;
      }
    }
    return (count >= Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) count;
  }

    /* ------ Map API methods ------ */

  public boolean isEmpty() {
    return findFirst() == null;
  }

  public void clear() {
    initialize();
  }

  public V putIfAbsent(long key, V value) {
    if (value == null) {
      throw new NullPointerException();
    }
    return doPut(key, value, true);
  }

  public boolean remove(long key, V value) {
    if (value == null) {
      return false;
    }
    return doRemove(key, value) != null;
  }

  public boolean replace(long key, V oldValue, V newValue) {
    if (oldValue == null || newValue == null) {
      throw new NullPointerException();
    }
    long k = key;
    for (; ; ) {
      Node<V> n = findNode(k);
      if (n == null) {
        return false;
      }
      Object v = n.value;
      if (v != null) {
        if (!oldValue.equals(v)) {
          return false;
        }
        if (n.casValue(v, newValue)) {
          return true;
        }
      }
    }
  }

  public V replace(long key, V value) {
    if (value == null) {
      throw new NullPointerException();
    }
    long k = key;
    for (; ; ) {
      Node<V> n = findNode(k);
      if (n == null) {
        return null;
      }
      Object v = n.value;
      if (v != null && n.casValue(v, value)) {
        return (V) v;
      }
    }
  }

  public long firstKey() {
    Node<V> n = findFirst();
    return n.key;
  }

  public Node<V> firstEntry() {
    Node<V> n = findFirst();

    if (n == null) {
      return null;
    }

    return n;
  }

  public long lastKey() {
    Node<V> n = findLast();
    if (n == null) {
      throw new NoSuchElementException();
    }
    return n.key;
  }


    /* ------ ConcurrentMap API methods ------ */

  public MapEntry.LongKeyEntry<V> lowerEntry(long key) {
    return getNear(key, LT);
  }

  public long lowerKey(long key) {
    Node<V> n = findNear(key, LT);
    return (n == null) ? null : n.key;
  }

  public MapEntry.LongKeyEntry<V> floorEntry(long key) {
    return getNear(key, LT | EQ);
  }

  public long floorKey(long key) {
    Node<V> n = findNear(key, LT | EQ);
    return (n == null) ? null : n.key;
  }

  public MapEntry.LongKeyEntry<V> ceilingEntry(long key) {
    return getNear(key, GT | EQ);
  }

  public long ceilingKey(long key) {
    Node<V> n = findNear(key, GT | EQ);
    return (n == null) ? null : n.key;
  }

    /* ---------------- Relational operations -------------- */

  public MapEntry.LongKeyEntry<V> higherEntry(long key) {
    return getNear(key, GT);
  }

  public long higherKey(long key) {
    Node<V> n = findNear(key, GT);
    return (n == null) ? null : n.key;
  }

  @FunctionalInterface
  public interface KeyMapping<V> {

    V apply(long key);
  }

  public interface CallbackOnFound<V> {

    public void found(V val);

    public void cancel(V val);
  }

  static class NullCallback implements CallbackOnFound {

    NullCallback() {
    }

    public void found(Object val) {
    }

    public void cancel(Object val) {
    }
  }

  /**
   * Nodes hold keys and values, and are singly linked in sorted order, possibly with some
   * intervening marker nodes. The list is headed by a dummy node accessible as head.node. The value
   * field is declared only as Object because it takes special non-V values for marker and header
   * nodes.
   */
  public static final class Node<V> {

    //    static final AtomicReferenceFieldUpdater<Node, Node>
//        nextUpdater = AtomicReferenceFieldUpdater.newUpdater
//        (Node.class, Node.class, "next");
//    static final AtomicReferenceFieldUpdater<Node, Object>
//        valueUpdater = AtomicReferenceFieldUpdater.newUpdater
//        (Node.class, Object.class, "value");
    public final long key;
    public Object value;
    public Node<V> next;

    /**
     * Creates a new regular node.
     */
    Node(long key, Object value, Node<V> next) {
      this.key = key;
      this.value = value;
      this.next = next;
    }

    /**
     * Creates a new marker node. A marker is distinguished by having its value field point to
     * itself.  Marker nodes also have null keys, a fact that is exploited in a few places, but this
     * doesn't distinguish markers from the base-level header node (head.node), which also has a
     * null key.
     */
    Node(Node<V> next) {
      this.key = 0;
      this.value = this;
      this.next = next;
    }

    boolean casValue(Object cmp, Object val) {
      value = val;
      return true;
    }

    boolean casNext(Node<V> cmp, Node<V> val) {
      next = val;
      return true;
    }

    boolean isMarker() {
      return value == this;
    }

    boolean isBaseHeader() {
      return value == BASE_HEADER;
    }

    boolean appendMarker(Node<V> f) {
      return casNext(f, new Node<V>(f));
    }

    void helpDelete(Node<V> b, Node<V> f) {
      if (f == next && this == b.next) {
        if (f == null || f.value != f) // not already marked
        {
          appendMarker(f);
        } else {
          b.casNext(this, f.next);
        }
      }
    }

    V getValidValue() {
      Object v = value;
      if (v == this || v == BASE_HEADER) {
        return null;
      }
      return (V) v;
    }

    MapEntry.LongKeyEntry<V> createSnapshot() {
      V v = getValidValue();
      if (v == null) {
        return null;
      }
      return new MapEntry.LongKeyEntry<>(key, v);
    }
  }

  static class Index<V> {

    final Node<V> node;
    final Index<V> down;
    Index<V> right;

    Index(Node<V> node, Index<V> down, Index<V> right) {
      this.node = node;
      this.down = down;
      this.right = right;
    }

    final boolean casRight(Index<V> cmp, Index<V> val) {
      right = val;
      return true;
    }

    final boolean indexesDeletedNode() {
      return node.value == null;
    }

    final boolean link(Index<V> succ, Index<V> newSucc) {
      Node<V> n = node;
      newSucc.right = succ;
      return n.value != null && casRight(succ, newSucc);
    }

    final boolean unlink(Index<V> succ) {
      return !indexesDeletedNode() && casRight(succ, succ.right);
    }
  }

  static final class HeadIndex<V> extends Index<V> {

    final int level;

    HeadIndex(Node<V> node, Index<V> down, Index<V> right, int level) {
      super(node, down, right);
      this.level = level;
    }
  }

}