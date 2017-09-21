package move;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import kotlin.text.Charsets;
import move.hash.CRC16;

/**
 *
 */
public class NUIDString {

  public final byte[] value;
  private boolean hashed;
  private int crc16;

  public NUIDString(byte[] value) {
    this.value = value;
  }

  public NUIDString(String value) {
    this.value = value.getBytes(Charsets.US_ASCII);
  }

  public static NUIDString create(String value) {
    return new NUIDString(value.getBytes(Charsets.US_ASCII));
  }

  public int crc16() {
    if (hashed) {
      return crc16;
    }
    crc16 = CRC16.calc(value);
    hashed = true;
    return crc16;
  }

  @Override
  public String toString() {
    return new String(value, StandardCharsets.US_ASCII);
  }

  @Override
  public int hashCode() {
    return crc16();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj instanceof byte[]) {
      return Arrays.equals(value, (byte[]) obj);
    }

    return Objects.equals(value, obj);
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    return new NUIDString(value);
  }
}
