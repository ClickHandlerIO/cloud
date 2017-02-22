package move.common;

import java.security.SecureRandom;

/**
 *
 */
public class UID {
    // for String ids
    static final SecureRandom numberGenerator = new SecureRandom();

    /**
     * A streamlined UUID generator optimized for strings and without hyphens.
     *
     * @return
     */
    public static String next() {
        final byte[] data = new byte[16];
        numberGenerator.nextBytes(data);

        data[6] &= 0x0f;  /* clear version        */
        data[6] |= 0x40;  /* set to version 4     */
        data[8] &= 0x3f;  /* clear variant        */
        data[8] |= 0x80;  /* set to IETF variant  */

        long mostSigBits = 0;
        long leastSigBits = 0;

        for (int i = 0; i < 8; i++)
            mostSigBits = (mostSigBits << 8) | (data[i] & 0xff);
        for (int i = 8; i < 16; i++)
            leastSigBits = (leastSigBits << 8) | (data[i] & 0xff);

        return (digits(mostSigBits >> 32, 8) +
            digits(mostSigBits >> 16, 4) +
            digits(mostSigBits, 4) +
            digits(leastSigBits >> 48, 4) +
            digits(leastSigBits, 12));
    }

    /**
     * Returns val represented by the specified number of hex digits.
     */
    private static String digits(long val, int digits) {
        long hi = 1L << (digits * 4);
        return Long.toHexString(hi | (val & (hi - 1))).substring(1);
    }

    public static boolean isValidId(String id) {
        return id != null && id.length() == 32;
    }
}
