package move.action;

/**
 *
 */
public class Bits {

  public static void main(String[] args) {
//    long x = (long)Integer.MAX_VALUE * (long)2;
    long y = 9000000000L;
    long x = 106;//Integer.MAX_VALUE;
    long m = (y & PackedUnsigned1616.INT34_MASK) << 18 | (100000 & PackedUnsigned1616.INT18_MASK);
    long m1 = m >>> 18;
    System.out.println(m);
    System.out.println(m1);
    System.out.println((int)(m & PackedUnsigned1616.INT18_MASK));

    System.out.println(y);
    System.out.println(x);

    long l = pack(y, x);
    x = (int)(l >> 32);
    y = (int)l;

    System.out.println(l);
    System.out.println(y);
    System.out.println(x);

    PackedUnsigned1616 v = new PackedUnsigned1616(y, x);
    System.out.println(v.getLeft());
    System.out.println(v.getRight());
  }

  public static long pack(long y, long x) {
    return (((long)x) << 32) | (y & 0xffffffffL);
  }

  public static class PackedUnsigned1616 {
    public static long BYTE_MASK  = (1L << Byte.SIZE) - 1;
    public static long SHORT_MASK = (1L << Short.SIZE) - 1;
    public static long INT_MASK   = (1L << Integer.SIZE) - 1;
    public static long INT18_MASK   = (1L << 18) - 1;
    public static long INT34_MASK   = (1L << 36) - 1;

    public static long DATACENTER_SHIFT = Integer.SIZE + Byte.SIZE + Short.SIZE;

    public final long field;

    private static final long RIGHT = 0xFFFFFFL;

    public PackedUnsigned1616(long left, long right) {
//      field = (left & INT34_MASK) | (right & INT18_MASK);
      field = (left << 18) | (right & RIGHT);
    }

    public long getLeft() {
      return field >>> 18; // >>> operator 0-fills from left
    }

    public long getRight() {
      return field & RIGHT;
    }
  }
}
