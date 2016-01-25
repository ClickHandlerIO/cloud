package io.clickhandler.remoting;

/**
 *
 */
public enum Compression {
    NONE((byte) 0),
    GZIP((byte) 1),
    DEFLATE((byte) 2),;

    public final byte code;

    Compression(byte code) {
        this.code = code;
    }

    public static Compression fromCode(int code) {
        switch (code) {
            case 0:
            default:
                return NONE;

            case 1:
                return GZIP;

            case 2:
                return DEFLATE;
        }
    }
}
