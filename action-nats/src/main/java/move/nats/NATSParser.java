package move.nats;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import java.nio.charset.StandardCharsets;

/**
 * NATS Protocol Parser optimized for Netty's ByteBuf.
 *
 * @auth Clay Molocznik
 */
public class NATSParser {

  static final int RECV_BUFFER_SIZE = 2 * 1024 * 1024;
  static final int SEND_BUFFER_SIZE = 2 * 1024 * 1024;

  static final int ASCII_0 = 48;
  static final int ASCII_9 = 57;

  static final byte ZERO = (byte) '0';
  static final byte ONE = (byte) '1';
  static final byte TWO = (byte) '2';
  static final byte THREE = (byte) '3';
  static final byte FOUR = (byte) '4';
  static final byte FIVE = (byte) '5';
  static final byte SIX = (byte) '6';
  static final byte SEVEN = (byte) '7';
  static final byte EIGHT = (byte) '8';
  static final byte NINE = (byte) '9';
  static final byte[] DIGITS = new byte[]{
      ZERO,
      ONE,
      TWO,
      THREE,
      FOUR,
      FIVE,
      SIX,
      SEVEN,
      EIGHT,
      NINE
  };

  static final byte i = (byte) 'i';
  static final byte I = (byte) 'I';

  static final byte n = (byte) 'n';
  static final byte N = (byte) 'N';

  static final byte f = (byte) 'f';
  static final byte F = (byte) 'F';

  static final byte o = (byte) 'o';
  static final byte O = (byte) 'O';

  static final byte g = (byte) 'g';
  static final byte G = (byte) 'G';

  static final byte p = (byte) 'p';
  static final byte P = (byte) 'P';

  static final byte m = (byte) 'm';
  static final byte M = (byte) 'M';

  static final byte s = (byte) 's';
  static final byte S = (byte) 'S';

  static final byte k = (byte) 'k';
  static final byte K = (byte) 'K';

  static final byte r = (byte) 'r';
  static final byte R = (byte) 'R';

  static final byte e = (byte) 'e';
  static final byte E = (byte) 'E';

  static final byte CR = (byte) '\r';
  static final byte LF = (byte) '\n';

  static final byte SPACE = (byte) ' ';
  static final byte PLUS = (byte) '+';
  static final byte MINUS = (byte) '-';


  final Listener listener;
  String errorMessage = null;
  private State state = State.IDLE;
  private String error = "";
  private Buffer payload;
  private long sid = 0;
  private ByteBuf replyTo;
  private int bytes = 0;
  private int bytesRemaining = 0;
  private byte[] replyToBuffer = new byte[1024];
  private int replyToLength;

  public NATSParser(Listener listener) {
    this.listener = listener;
  }

  public State state() {
    return state;
  }

  void reset() {
    state = State.I;
    error = "";
    payload = null;
    sid = -1;
    replyTo = null;
    bytes = -1;
  }

  @SuppressWarnings("all")
  public void parse(ByteBuf buffer) {
    while (parse0(buffer)) {
    }
  }

  private boolean protocolFailure(String message) {
    state = State.FAILED;
    error = message;
    listener.protocolErr(message);
    return false;
  }

  @SuppressWarnings("all")
  private boolean parse0(ByteBuf buffer) {
    switch (state) {
      case IDLE:
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          default:
            switch (buffer.readByte()) {
              case m:
              case M:
                state = State.MS;
                return buffer.isReadable();

              case p:
              case P:
                if (!buffer.isReadable()) {
                  state = State.P_;
                  return false;
                }

                switch (buffer.readByte()) {
                  case o:
                  case O:
                    state = State.PON;
                    return buffer.isReadable();

                  case i:
                  case I:
                    state = State.PIN;
                    return buffer.isReadable();

                  default:
                    return protocolFailure("Expected 'o' or 'O' or 'i' or 'I' after 'P'");
                }

              case i:
              case I:
                state = State.IN;
                return buffer.isReadable();
            }
            break;
        }
        break;

      case I: {
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          case 1:
            switch (buffer.readByte()) {
              case i:
              case I:
                break;

              default:
                return protocolFailure("Expected 'I' or 'i' in INFO");
            }
            state = State.IN;

            break;

          case 2:
            switch (buffer.readByte()) {
              case i:
              case I:
                break;

              default:
                return protocolFailure("Expected 'I' or 'i' in INFO");
            }
            switch (buffer.readByte()) {
              case n:
              case N:
                break;

              default:
                return protocolFailure("Expected 'N' or 'n' in INFO");
            }

            state = State.INF;
            break;

          case 3:
            switch (buffer.readByte()) {
              case i:
              case I:
                break;

              default:
                return protocolFailure("Expected 'I' or 'i' in INFO");
            }
            switch (buffer.readByte()) {
              case n:
              case N:
                break;

              default:
                return protocolFailure("Expected 'N' or 'n' in INFO");
            }
            switch (buffer.readByte()) {
              case f:
              case F:
                break;

              default:
                return protocolFailure("Expected 'F' or 'f' in INFO");
            }

            state = State.INFO;
            break;

          case 4:
            switch (buffer.readByte()) {
              case i:
              case I:
                break;

              default:
                return protocolFailure("Expected 'I' or 'i' in INFO");
            }
            switch (buffer.readByte()) {
              case n:
              case N:
                break;

              default:
                return protocolFailure("Expected 'N' or 'n' in INFO");
            }
            switch (buffer.readByte()) {
              case f:
              case F:
                break;

              default:
                return protocolFailure("Expected 'F' or 'f' in INFO");
            }
            switch (buffer.readByte()) {
              case o:
              case O:
                break;

              default:
                return protocolFailure("Expected 'O' or 'o' in INFO");
            }

            state = State.INFO_SPACE;
            break;

          case 5:
            switch (buffer.readByte()) {
              case i:
              case I:
                break;

              default:
                return protocolFailure("Expected 'I' or 'i' in INFO");
            }
            switch (buffer.readByte()) {
              case n:
              case N:
                break;

              default:
                return protocolFailure("Expected 'N' or 'n' in INFO");
            }
            switch (buffer.readByte()) {
              case f:
              case F:
                break;

              default:
                return protocolFailure("Expected 'F' or 'f' in INFO");
            }
            switch (buffer.readByte()) {
              case o:
              case O:
                break;

              default:
                return protocolFailure("Expected 'O' or 'o' in INFO");
            }

            if (buffer.readByte() != SPACE) {
              return protocolFailure("Expected ' ' after INFO");
            }

            state = State.INFO_PAYLOAD;
            break;

          default:
            switch (buffer.readByte()) {
              case i:
              case I:
                break;

              default:
                return protocolFailure("Expected 'I' or 'i' in INFO");
            }
            switch (buffer.readByte()) {
              case n:
              case N:
                break;

              default:
                return protocolFailure("Expected 'N' or 'n' in INFO");
            }
            switch (buffer.readByte()) {
              case f:
              case F:
                break;

              default:
                return protocolFailure("Expected 'F' or 'f' in INFO");
            }
            switch (buffer.readByte()) {
              case o:
              case O:
                break;

              default:
                return protocolFailure("Expected 'O' or 'o' in INFO");
            }

            if (buffer.readByte() != SPACE) {
              return protocolFailure("Expected ' ' after INFO");
            }

            state = State.INFO_PAYLOAD;
            return buffer.isReadable();
        }
      }
      break;

      case IN: {
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          case 1:
            switch (buffer.readByte()) {
              case n:
              case N:
                state = State.INF;
                return false;

              default:
                return protocolFailure("Expected 'N' or 'n' after 'I'");
            }

          case 2:
            switch (buffer.readByte()) {
              case n:
              case N:
                break;

              default:
                return protocolFailure("Expected 'N' or 'n' after 'I'");
            }

            switch (buffer.readByte()) {
              case f:
              case F:
                break;

              default:
                return protocolFailure("Expected 'F' or 'f' after 'IN'");
            }

            state = State.INFO;
            return false;

          case 3:
            switch (buffer.readByte()) {
              case n:
              case N:
                break;

              default:
                return protocolFailure("Expected 'N' or 'n' after 'I'");
            }

            switch (buffer.readByte()) {
              case f:
              case F:
                break;

              default:
                return protocolFailure("Expected 'F' or 'f' after 'IN'");
            }

            switch (buffer.readByte()) {
              case o:
              case O:
                break;

              default:
                return protocolFailure("Expected 'O' or 'o' after 'INF'");
            }
            state = State.INFO_SPACE;
            return false;

          default:
          case 4:
            switch (buffer.readByte()) {
              case n:
              case N:
                break;

              default:
                return protocolFailure("Expected 'N' or 'n' after 'I'");
            }

            switch (buffer.readByte()) {
              case f:
              case F:
                break;

              default:
                return protocolFailure("Expected 'F' or 'f' after 'IN'");
            }

            switch (buffer.readByte()) {
              case o:
              case O:
                break;

              default:
                return protocolFailure("Expected 'O' or 'o' after 'INF'");
            }

            if (buffer.readByte() != SPACE) {
              return protocolFailure("Expected ' ' after 'INFO'");
            }

            state = State.INFO_PAYLOAD;

            return buffer.isReadable();
        }
      }

      case INF: {
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          case 1:
            switch (buffer.readByte()) {
              case f:
              case F:
                state = State.INFO;
                return false;

              default:
                return protocolFailure("Expected 'F' or 'f' after 'IN'");
            }

          case 2:
            switch (buffer.readByte()) {
              case f:
              case F:
                break;

              default:
                return protocolFailure("Expected 'F' or 'f' after 'IN'");
            }

            switch (buffer.readByte()) {
              case o:
              case O:
                break;

              default:
                return protocolFailure("Expected 'O' or 'O' after 'INF'");
            }

            state = State.INFO;
            return false;

          default:
          case 3:
            switch (buffer.readByte()) {
              case f:
              case F:
                break;

              default:
                return protocolFailure("Expected 'F' or 'f' after 'IN'");
            }

            switch (buffer.readByte()) {
              case o:
              case O:
                break;

              default:
                return protocolFailure("Expected 'O' or 'O' after 'INF'");
            }

            if (buffer.readByte() != SPACE) {
              return protocolFailure("Expected ' ' after 'INFO'");
            }

            state = State.INFO_PAYLOAD;
            return buffer.isReadable();
        }
      }

      case INFO: {
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          case 1:
            switch (buffer.readByte()) {
              case o:
              case O:
                state = State.INFO_SPACE;
                return false;

              default:
                return protocolFailure("Expected 'O' or 'O' after 'INF'");
            }

          default:
          case 2:
            switch (buffer.readByte()) {
              case o:
              case O:
                break;

              default:
                return protocolFailure("Expected 'O' or 'O' after 'INF'");
            }

            if (buffer.readByte() != SPACE) {
              return protocolFailure("Expected ' ' after 'INFO'");
            }

            state = State.INFO_PAYLOAD;
            return buffer.isReadable();
        }
      }

      case INFO_SPACE: {
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          default:
          case 1:
            if (buffer.readByte() != SPACE) {
              return protocolFailure("Expected ' ' after 'INFO'");
            }

            state = State.INFO_PAYLOAD;
            return buffer.isReadable();
        }
      }

      case INFO_PAYLOAD: {
        if (!buffer.isReadable()) {
          return false;
        }

        final int index = buffer.readerIndex();
        while (buffer.isReadable()) {
          final byte b = buffer.readByte();

          if (b == CR) {
            if (!buffer.isReadable()) {
              int l = buffer.readerIndex() - 1 - index;
              if (l > 0) {
                if (payload == null) {
                  payload = Buffer.buffer(buffer.slice(index, l).copy());
                } else {
                  payload = payload.appendBuffer(Buffer.buffer(buffer.slice(index, l).copy()));
                }
              }
              state = State.INFO_PAYLOAD_CRLF;
              return false;
            }

            if (buffer.readByte() == LF) {
              int l = buffer.readerIndex() - 2 - index;
              if (l > 0) {
                if (payload == null) {
                  payload = Buffer.buffer(buffer.slice(index, l).copy());
                } else {
                  payload = payload.appendBuffer(
                      Buffer.buffer(buffer.slice(index, l).copy()));
                }
              }
            }

            listener.onInfo(payload.getByteBuf());
            payload = null;
            state = State.IDLE;
            return buffer.isReadable();
          }
        }
        int l = buffer.readerIndex() - index;
        if (l > 0) {
          if (payload == null) {
            payload = Buffer.buffer(buffer.slice(index, l).copy());
          } else {
            final Buffer p = payload;
            payload = payload.appendBuffer(Buffer.buffer(buffer.slice(index, l).copy()));
          }
        }
        state = State.INFO_PAYLOAD;
        return false;
      }

      case INFO_PAYLOAD_CRLF: {
        if (!buffer.isReadable()) {
          return false;
        }

        final int index = buffer.readerIndex();
        byte b = buffer.readByte();

        if (b == LF) {
          listener.onInfo(payload.getByteBuf());
          payload = null;
          state = State.IDLE;
          return buffer.isReadable();
        }

        // Rewind 1 byte.
        buffer.readerIndex(buffer.readerIndex() - 1);
        state = State.INFO_PAYLOAD;
        return buffer.isReadable();
      }

      case MS:
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          case 1:
            switch (buffer.readByte()) {
              case s:
              case S:
                state = State.MSG;
                return false;

              default:
                return protocolFailure("Expected 'S' or 's' after 'M'");
            }

          case 2:
            switch (buffer.readByte()) {
              case s:
              case S:
                break;

              default:
                return protocolFailure("Expected 'S' or 's' after 'M'");
            }

            switch (buffer.readByte()) {
              case g:
              case G:
                state = State.MSG_SPACE;
                return false;

              default:
                return protocolFailure("Expected 'G' or 'g' after 'MS'");
            }

          default:
          case 3:
            switch (buffer.readByte()) {
              case s:
              case S:
                break;

              default:
                return protocolFailure("Expected 'S' or 's' after 'M'");
            }

            switch (buffer.readByte()) {
              case g:
              case G:
                break;

              default:
                return protocolFailure("Expected 'G' or 'g' after 'MS'");
            }

            if (buffer.readByte() != SPACE) {
              return protocolFailure("Expected ' ' after 'MSG'");
            }

            state = State.MSG_SUBJECT;
            return buffer.isReadable();
        }

      case MSG:
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          case 1:
            switch (buffer.readByte()) {
              case g:
              case G:
                state = State.MSG_SPACE;
                return false;

              default:
                return protocolFailure("Expected 'G' or 'g' after 'MS'");
            }

          default:
          case 2:
            switch (buffer.readByte()) {
              case g:
              case G:
                break;

              default:
                return protocolFailure("Expected 'G' or 'g' after 'MS'");
            }

            if (buffer.readByte() != SPACE) {
              return protocolFailure("Expected ' ' after 'MSG'");
            }

            state = State.MSG_SPACE;
            return buffer.isReadable();
        }

      case MSG_SPACE:
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          default:
          case 1:
            if (buffer.readByte() != SPACE) {
              return protocolFailure("Expected ' ' after 'MSG'");
            }
            state = State.MSG_SUBJECT;
            return buffer.isReadable();
        }

      case MSG_SUBJECT: {
        if (!buffer.isReadable()) {
          return false;
        }

        final int index = buffer.readerIndex();
        while (buffer.isReadable()) {
          final byte b = buffer.readByte();

          if (b == SPACE) {
            int l = buffer.readerIndex() - 1 - index;
            if (l > 0) {
              if (payload == null) {
                listener.onSubject(buffer.slice(index, l));
              } else {
                payload = payload.appendBuffer(Buffer.buffer(buffer.slice(index, l).copy()));
              }
            } else {
              if (payload == null) {
                listener.onSubject(null);
              } else {
                listener.onSubject(payload.getByteBuf());
                payload = null;
              }
            }

            state = State.MSG_SID;
            return buffer.isReadable();
          }
        }
        int l = buffer.readerIndex() - index;
        if (l > 0) {
          if (payload == null) {
            payload = Buffer.buffer(buffer.slice(index, l).copy());
          } else {
            payload = payload.appendBuffer(Buffer.buffer(buffer.slice(index, l).copy()));
          }
        }
        return false;
      }

      case MSG_SID: {
        if (!buffer.isReadable()) {
          return false;
        }

        int index = buffer.readerIndex();
        while (buffer.isReadable()) {
          byte b = buffer.readByte();

          if (b == SPACE) {
            state = State.MSG_REPLY_OR_BYTES;

            replyToLength = 0;
            payload = null;
            return buffer.isReadable();
          } else {
            if (b < ASCII_0 || b > ASCII_9) {
              return protocolFailure("sid bytes must be an ascii digit");
            }
            sid = (sid * 10) + b - ASCII_0;
          }
        }
        return false;
      }

      case MSG_REPLY_OR_BYTES: {
        if (!buffer.isReadable()) {
          return false;
        }

        this.bytes = 0;
        int mbytes = 0;
        int count = 0;

        final int index = buffer.readerIndex();
        while (buffer.isReadable()) {
          final byte b = buffer.readByte();

          switch (b) {
            case SPACE:
              // We received a reply to that's numeric.
              if (count > 0) {
                replyToLength = 0;
                // Parse to buffer.
                int bytes = mbytes;
                while (bytes > 0) {
                  replyToBuffer[replyToLength++] = DIGITS[bytes % 10];
                  bytes /= 10;
                }
              }

              state = State.MSG_BYTES;
              return buffer.isReadable();

            case CR:
              // This is definitely bytes.
              this.bytes = mbytes;
              bytesRemaining = mbytes;

              if (!buffer.isReadable()) {
                state = State.MSG_BYTES_CRLF;
                return false;
              }

              if (buffer.readByte() != LF) {
                return protocolFailure("Expected LF after bytes CR on MSG");
              }

              if (bytesRemaining > 0) {
                state = State.MSG_PAYLOAD;
              } else {
                state = State.MSG_PAYLOAD_CR;
              }
              return buffer.isReadable();

            default:
              if (b < ASCII_0 || b > ASCII_9) {
                if (count > 0) {
                  replyToLength = 0;
                  // Reply To started with DIGITS.
                  int bytes = mbytes;
                  while (bytes > 0) {
                    replyToBuffer[replyToLength++] = DIGITS[bytes % 10];
                    bytes /= 10;
                  }
                }

                replyToBuffer[replyToLength++] = b;
                state = State.MSG_REPLY;
                return buffer.isReadable();
              }

              count++;
              mbytes = (mbytes * 10) + b - ASCII_0;
              break;
          }
        }
      }
      break;

      case MSG_REPLY: {
        if (!buffer.isReadable()) {
          return false;
        }

        final int index = buffer.readerIndex();
        while (buffer.isReadable()) {
          final byte b = buffer.readByte();

          switch (b) {
            case SPACE:
              state = State.MSG_BYTES;
              return buffer.isReadable();

            case CR:
            case LF:
              // Failure.
              return protocolFailure("Expecting [replyTo] SPACE as terminator");

            default:
              if (replyToLength >= replyToBuffer.length) {
                return protocolFailure(
                    "[replyTo] is too long. Limit of " + replyToBuffer.length + " was reached");
              }
              replyToBuffer[replyToLength++] = b;
              break;
          }
        }
        return false;
      }

      case MSG_BYTES: {
        if (!buffer.isReadable()) {
          return false;
        }

        final int index = buffer.readerIndex();
        while (buffer.isReadable()) {
          final byte b = buffer.readByte();

          switch (b) {
            case CR:
              if (!buffer.isReadable()) {
                state = State.MSG_BYTES_CRLF;
                return false;
              }

              if (buffer.readByte() != LF) {
                return protocolFailure("Expected LF after bytes for MSG");
              }

              if (bytes > 0) {
                bytesRemaining = bytes;
                state = State.MSG_PAYLOAD;
              } else {
                state = State.MSG_PAYLOAD_CR;
              }
              return buffer.isReadable();

            default:
              if (b < ASCII_0 || b > ASCII_9) {
                return protocolFailure("bytes must be an ascii digit");
              }
              bytes = (bytes * 10) + b - ASCII_0;
              break;
          }
        }

        return false;
      }

      case MSG_BYTES_CRLF: {
        if (!buffer.isReadable()) {
          return false;
        }

        if (buffer.readByte() != LF) {
          return protocolFailure("Expected LF after bytes for MSG");
        }

        if (bytes > 0) {
          bytesRemaining = bytes;
          state = State.MSG_PAYLOAD;
        } else {
          state = State.MSG_PAYLOAD_CR;
        }

        return buffer.isReadable();
      }

      case MSG_PAYLOAD: {
        if (!buffer.isReadable()) {
          return false;
        }

        int index = buffer.readerIndex();
        if (buffer.readableBytes() >= bytesRemaining) {
          state = State.MSG_PAYLOAD_CR;

          if (payload == null) {
            onMsg(buffer.slice(index, bytesRemaining));
          } else {
            onMsg(payload.appendBuffer(Buffer.buffer(buffer.slice(index, bytesRemaining)))
                .getByteBuf());
          }

          buffer.readerIndex(buffer.readerIndex() + bytesRemaining);
          bytesRemaining = 0;

          return buffer.isReadable();
        } else {
          int length = buffer.readableBytes() - index;
          if (payload == null) {
            // TODO: Maybe avoid copy?
            payload = Buffer.buffer(buffer.slice(index, length).copy());
          } else {
            // TODO: Maybe avoid copy?
            payload.appendBuffer(Buffer.buffer(buffer.slice(index, length).copy()));
          }
          bytesRemaining -= length;
          return false;
        }
      }

      case MSG_PAYLOAD_CR: {
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          case 1:
            if (buffer.readByte() != CR) {
              return protocolFailure("Expected CR after payload");
            }
            state = State.MSG_PAYLOAD_CRLF;
            return false;

          default:
          case 2:
            if (buffer.readByte() != CR) {
              return protocolFailure("Expected CR after payload");
            }
            if (buffer.readByte() != LF) {
              return protocolFailure("Expected LF after CR");
            }
            state = State.IDLE;
            return buffer.isReadable();
        }
      }

      case MSG_PAYLOAD_CRLF: {
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          default:
          case 1:
            if (buffer.readByte() != LF) {
              return protocolFailure("Expected LF after CR");
            }
            state = State.IDLE;
            return buffer.isReadable();
        }
      }

      case PLUS_O: {
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          case 1:
            switch (buffer.readByte()) {
              case o:
              case O:
                state = State.PLUS_OK;
                return false;

              default:
                return protocolFailure("Expected 'O' or 'o' after +");
            }

          default:
          case 2:
            switch (buffer.readByte()) {
              case o:
              case O:
                break;

              default:
                return protocolFailure("Expected 'O' or 'o' after +");
            }

            switch (buffer.readByte()) {
              case k:
              case K:
                break;

              default:
                return protocolFailure("Expected 'K' or 'k' after +O");
            }

            state = State.PLUS_OK_CR;
            return buffer.isReadable();

        }
      }

      case PLUS_OK: {
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          case 1:
            switch (buffer.readByte()) {
              case k:
              case K:
                state = State.PLUS_OK_CR;
                return false;

              default:
                return protocolFailure("Expected 'K' or 'k' after +O");
            }

          default:
          case 2:

            switch (buffer.readByte()) {
              case k:
              case K:
                break;

              default:
                return protocolFailure("Expected 'K' or 'k' after +O");
            }

            state = State.PLUS_OK_CR;
            return true;
        }
      }

      case PLUS_OK_CR: {
        if (!buffer.isReadable()) {
          return false;
        }

        switch (buffer.readableBytes()) {
          case 0:
            return false;

          case 1:
            if (buffer.readByte() != CR) {
              return protocolFailure("Expected CR after +OK");
            }
            state = State.PLUS_OK_CRLF;
            return false;

          default:
          case 2:
            if (buffer.readByte() != CR) {
              return protocolFailure("Expected CR after +OK");
            }
            if (buffer.readByte() != LF) {
              return protocolFailure("Expected LF after +OK\r");
            }
            state = State.IDLE;
            return buffer.isReadable();
        }
      }

      case MINUS_E: {
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          case 1:
            switch (buffer.readByte()) {
              case e:
              case E:
                state = State.MINUS_ER;
                return false;

              default:
                return protocolFailure("Expected 'E' or 'e' after -");
            }

          case 2:
            switch (buffer.readByte()) {
              case e:
              case E:
                break;

              default:
                return protocolFailure("Expected 'E' or 'e' after -");
            }

            switch (buffer.readByte()) {
              case r:
              case R:
                break;

              default:
                return protocolFailure("Expected 'R' or 'r' after -E");
            }

            state = State.MINUS_ERR;
            return buffer.isReadable();

          default:
          case 3: {
            switch (buffer.readByte()) {
              case e:
              case E:
                break;

              default:
                return protocolFailure("Expected 'E' or 'e' after -");
            }

            switch (buffer.readByte()) {
              case r:
              case R:
                break;

              default:
                return protocolFailure("Expected 'R' or 'r' after -E");
            }

            switch (buffer.readByte()) {
              case r:
              case R:
                break;

              default:
                return protocolFailure("Expected 'R' or 'r' after -ER");
            }

            state = State.MINUS_ERR_MESSAGE;
            return buffer.isReadable();
          }
        }
      }

      case MINUS_ER: {
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          case 1:
            switch (buffer.readByte()) {
              case r:
              case R:
                return false;

              default:
                return protocolFailure("Expected 'R' or 'r' after -E");
            }

          default:
          case 2:
            switch (buffer.readByte()) {
              case r:
              case R:
                break;

              default:
                return protocolFailure("Expected 'R' or 'r' after -E");
            }

            switch (buffer.readByte()) {
              case r:
              case R:
                break;

              default:
                return protocolFailure("Expected 'R' or 'r' after -ER");
            }

            state = State.MINUS_ERR_MESSAGE;
            return buffer.isReadable();
        }
      }

      case MINUS_ERR: {
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          case 1:
            switch (buffer.readByte()) {
              case r:
              case R:
                return false;

              default:
                return protocolFailure("Expected 'R' or 'r' after -ER");
            }

          default:
          case 2:
            switch (buffer.readByte()) {
              case r:
              case R:
                break;

              default:
                return protocolFailure("Expected 'R' or 'r' after -E");
            }

            state = State.MINUS_ERR_MESSAGE;
            return buffer.isReadable();
        }
      }

      case MINUS_ERR_MESSAGE: {
        if (!buffer.isReadable()) {
          return false;
        }

        int index = buffer.readerIndex();
        while (buffer.isReadable()) {
          final byte b = buffer.readByte();

          switch (b) {
            case CR: {
              int length = buffer.readerIndex() - index;
              if (length > 0) {
                if (errorMessage == null) {
                  errorMessage = "";
                }
                errorMessage += buffer.slice(index, length).toString(StandardCharsets.UTF_8);
                errorMessage = errorMessage.trim();
              }

              if (!buffer.isReadable()) {
                state = State.MINUS_ERR_MESSAGE_CRLF;
                onErrorMessage();
                return false;
              } else if (buffer.readByte() != LF) {
                onErrorMessage();
                return protocolFailure("Expected LF after -ERR \r");
              } else {
                onErrorMessage();
                return false;
              }
            }
          }
        }

        int length = buffer.readerIndex() - index;
        if (length > 0) {
          if (errorMessage == null) {
            errorMessage = "";
          }
          errorMessage += buffer.slice(index, length).toString(StandardCharsets.UTF_8);
        }
        return false;
      }

      case MINUS_ERR_MESSAGE_CRLF: {
        if (!buffer.isReadable()) {
          return false;
        }

        if (buffer.readByte() != LF) {
          return protocolFailure("Expected LF after -ERR \r");
        }

        if (state != State.FAILED) {
          state = State.IDLE;
          return buffer.isReadable();
        }

        return false;
      }

      case P_: {
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          default:
          case 1:
            switch (buffer.readByte()) {
              case i:
              case I:
                state = State.PIN;
                return buffer.isReadable();

              case o:
              case O:
                state = State.PON;
                return buffer.isReadable();

              default:
                return protocolFailure("Expected eith 'I' or 'O' after P");
            }
        }
      }

      case PIN: {
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          case 1:
            switch (buffer.readByte()) {
              case n:
              case N:
                state = State.PING;
                return false;

              default:
                return protocolFailure("Expected 'N' or 'n' after PI");
            }

          case 2:
            switch (buffer.readByte()) {
              case n:
              case N:
                break;

              default:
                return protocolFailure("Expected 'N' or 'n' after PI");
            }

            switch (buffer.readByte()) {
              case g:
              case G:
                state = State.PING_CR;
                return false;

              default:
                return protocolFailure("Expected 'G' or 'g' after PIN");
            }

          case 3:
            switch (buffer.readByte()) {
              case n:
              case N:
                break;

              default:
                return protocolFailure("Expected 'N' or 'n' after PI");
            }

            switch (buffer.readByte()) {
              case g:
              case G:
                break;

              default:
                return protocolFailure("Expected 'G' or 'g' after PIN");
            }
            if (buffer.readByte() != CR) {
              return protocolFailure("Expected CR after PING");
            }

            state = State.PING_CRLF;
            return buffer.isReadable();

          default:
          case 4:
            switch (buffer.readByte()) {
              case n:
              case N:
                break;

              default:
                return protocolFailure("Expected 'N' or 'n' after PI");
            }

            switch (buffer.readByte()) {
              case g:
              case G:
                break;

              default:
                return protocolFailure("Expected 'G' or 'g' after PIN");
            }
            if (buffer.readByte() != CR) {
              return protocolFailure("Expected CR after PING");
            }
            if (buffer.readByte() != LF) {
              return protocolFailure("Expected LF after PING<CR>");
            }

            onPing();
            state = State.IDLE;
            return buffer.isReadable();
        }
      }

      case PING: {
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          case 1:
            switch (buffer.readByte()) {
              case g:
              case G:
                state = State.PING_CR;
                return false;

              default:
                return protocolFailure("Expected 'G' or 'g' after PIN");
            }

          default:
          case 2:
            switch (buffer.readByte()) {
              case g:
              case G:
                break;

              default:
                return protocolFailure("Expected 'G' or 'g' after PIN");
            }
            if (buffer.readByte() != CR) {
              return protocolFailure("Expected CR after PING");
            }
            state = State.PING_CRLF;
            return buffer.isReadable();
        }
      }

      case PING_CR: {
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          default:
          case 1:
            if (buffer.readByte() != CR) {
              return protocolFailure("Expected CR after PING");
            }
            state = State.PING_CRLF;
            return buffer.isReadable();
        }
      }

      case PING_CRLF: {
        if (!buffer.isReadable()) {
          return false;
        }

        if (buffer.readByte() != LF) {
          return protocolFailure("Expected LF after PING<CR>");
        }

        onPing();
        state = State.IDLE;
        return buffer.isReadable();
      }

      case PON: {
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          case 1:
            switch (buffer.readByte()) {
              case n:
              case N:
                state = State.PONG;
                return false;

              default:
                return protocolFailure("Expected 'N' or 'n' after PO");
            }

          case 2:
            switch (buffer.readByte()) {
              case n:
              case N:
                break;

              default:
                return protocolFailure("Expected 'N' or 'n' after PO");
            }

            switch (buffer.readByte()) {
              case g:
              case G:
                state = State.PONG_CR;
                return false;

              default:
                return protocolFailure("Expected 'G' or 'g' after PON");
            }

          case 3:
            switch (buffer.readByte()) {
              case n:
              case N:
                break;

              default:
                return protocolFailure("Expected 'N' or 'n' after PO");
            }

            switch (buffer.readByte()) {
              case g:
              case G:
                break;

              default:
                return protocolFailure("Expected 'G' or 'g' after PON");
            }
            if (buffer.readByte() != CR) {
              return protocolFailure("Expected CR after PONG");
            }

            state = State.PONG_CRLF;
            return buffer.isReadable();

          default:
          case 4:
            switch (buffer.readByte()) {
              case n:
              case N:
                break;

              default:
                return protocolFailure("Expected 'N' or 'n' after PO");
            }

            switch (buffer.readByte()) {
              case g:
              case G:
                break;

              default:
                return protocolFailure("Expected 'G' or 'g' after PON");
            }
            if (buffer.readByte() != CR) {
              return protocolFailure("Expected CR after PONG");
            }
            if (buffer.readByte() != LF) {
              return protocolFailure("Expected LF after PONG<CR>");
            }

            onPong();
            state = State.IDLE;
            return buffer.isReadable();
        }
      }

      case PONG: {
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          case 1:
            switch (buffer.readByte()) {
              case g:
              case G:
                state = State.PONG_CR;
                return false;

              default:
                return protocolFailure("Expected 'G' or 'g' after PON");
            }

          case 2:
            switch (buffer.readByte()) {
              case g:
              case G:
                break;

              default:
                return protocolFailure("Expected 'G' or 'g' after PON");
            }
            if (buffer.readByte() != CR) {
              return protocolFailure("Expected CR after PING");
            }
            state = State.PONG_CRLF;
            return false;

          default:
          case 3:
            switch (buffer.readByte()) {
              case g:
              case G:
                break;

              default:
                return protocolFailure("Expected 'G' or 'g' after PON");
            }
            if (buffer.readByte() != CR) {
              return protocolFailure("Expected CR after PONG");
            }
            if (buffer.readByte() != LF) {
              return protocolFailure("Expected LF after PONG<CR>");
            }
            onPong();
            state = State.IDLE;
            return buffer.isReadable();
        }
      }

      case PONG_CR: {
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          case 1:
            if (buffer.readByte() != CR) {
              return protocolFailure("Expected CR after PONG");
            }
            state = State.PONG_CRLF;
            return false;

          default:
          case 2:
            if (buffer.readByte() != CR) {
              return protocolFailure("Expected CR after PONG");
            }
            if (buffer.readByte() != LF) {
              return protocolFailure("Expected LF after PONG<CR>");
            }
            onPong();
            state = State.IDLE;
            return buffer.isReadable();
        }
      }

      case PONG_CRLF: {
        if (!buffer.isReadable()) {
          return false;
        }

        if (buffer.readByte() != LF) {
          return protocolFailure("Expected LF after PONG<CR>");
        }

        onPong();
        state = State.IDLE;
        return buffer.isReadable();
      }
    }

    return false;
  }

  private void onPing() {
    listener.onPing();
  }

  private void onPong() {
    listener.onPong();
  }

  private void onErrorMessage() {
    if (errorMessage == null) {
      listener.onErr(Reason.UNKNOWN, "");
      return;
    }

    final String e = errorMessage.trim().toLowerCase();
    Reason reason = Reason.UNKNOWN;
    if (e.contains("unknown protocol")) {
      reason = Reason.UNKNOWN_PROTOCOL_OPERATION;
    } else if (e.contains("attempted to connect")) {
      reason = Reason.ATTEMPTED_TO_CONNECT_TO_ROUTE_PORT;
    } else if (e.contains("authorization violation")) {
      reason = Reason.AUTHORIZATION_VIOLATION;
    } else if (e.contains("authorization timeout")) {
      reason = Reason.AUTHORIZATION_TIMEOUT;
    } else if (e.contains("invalid client protocol")) {
      reason = Reason.INVALID_CLIENT_PROTOCOL;
    } else if (e.contains("maximum control line")) {
      reason = Reason.MAXIMUM_CONTROL_LINE_EXCEEDED;
    } else if (e.contains("parser error")) {
      reason = Reason.PARSER_ERROR;
    } else if (e.contains("secure listener")) {
      reason = Reason.SECURE_CONNECTION_TLS_REQUIRED;
    } else if (e.contains("stale listener")) {
      reason = Reason.STALE_CONNECTION;
    } else if (e.contains("maximum connections")) {
      reason = Reason.MAXIMUM_CONNECTIONS_EXCEEDED;
    } else if (e.contains("slow consumer")) {
      reason = Reason.SLOW_CONSUMER;
    } else if (e.contains("maximum payload violation")) {
      reason = Reason.MAXIMUM_PAYLOAD_VIOLATION;
    } else if (e.contains("invalid subject")) {
      reason = Reason.INVALID_SUBJECT;
    } else if (e.contains("permissions violation for subscription")) {
      reason = Reason.PERMISSIONS_VIOLATION_FOR_SUBSCRIPTION;
    } else if (e.contains("permissions violation for publish")) {
      reason = Reason.PERMISSIONS_VIOLATION_FOR_PUBLISH;
    }

    listener.onErr(reason, errorMessage);
  }

  void onMsg(ByteBuf buffer) {
    listener.onMessage(sid, replyToBuffer, replyToLength, buffer);
    payload = null;
    sid = 0;
    replyToLength = 0;
  }

  enum State {
    /**
     * Protocol failure. Connection must close if it hasn't already been closed by NATS server.
     */
    FAILED,

    /**
     * Initial state. NATS server sends an "INFO" first thing. Then we send back a CONNECT Then we
     * have a valid listener.
     */
    I,
    IN,
    INF,
    INFO,
    INFO_SPACE,
    INFO_PAYLOAD,
    INFO_PAYLOAD_CRLF,

    /**
     * Ready for next command.
     */
    IDLE,

    MS,
    MSG,
    MSG_SPACE,
    MSG_SUBJECT,
    MSG_SID,
    MSG_REPLY_OR_BYTES,
    MSG_REPLY,
    MSG_BYTES,
    MSG_BYTES_CRLF,
    MSG_PAYLOAD,
    MSG_PAYLOAD_CR,
    MSG_PAYLOAD_CRLF,


    PLUS_O,
    PLUS_OK,
    PLUS_OK_CR,
    PLUS_OK_CRLF,
    MINUS_E,
    MINUS_ER,
    MINUS_ERR,
    MINUS_ERR_MESSAGE,
    MINUS_ERR_MESSAGE_CRLF,

    P_,

    PIN,
    PING,
    PING_CR,
    PING_CRLF,

    PON,
    PONG,
    PONG_CR,
    PONG_CRLF,
  }

  public enum Reason {
    UNKNOWN,
    /**
     * Unknown protocol error.
     */
    UNKNOWN_PROTOCOL_OPERATION,
    /**
     * Client attempted to connect to a route port instead of the client port.
     */
    ATTEMPTED_TO_CONNECT_TO_ROUTE_PORT,
    /**
     * Client failed to authenticate to the server with credentials specified in the CONNECT
     * message.
     */
    AUTHORIZATION_VIOLATION,
    /**
     * Client took too long to authenticate to the server after establishing a listener (default 1
     * second)
     */
    AUTHORIZATION_TIMEOUT,
    /**
     * Client specified an invalid protocol version in the CONNECT message
     */
    INVALID_CLIENT_PROTOCOL,
    /**
     * Message destination subject and reply subject length exceeded the maximum control line value
     * specified by the max_control_line server option. The default is 1024 bytes.
     */
    MAXIMUM_CONTROL_LINE_EXCEEDED,
    /**
     * Cannot parse the protocol message sent by the client.
     */
    PARSER_ERROR,
    /**
     * The server requires TLS and the client does not have TLS enabled.
     */
    SECURE_CONNECTION_TLS_REQUIRED,
    /**
     * PING/PONG interval expired.
     */
    STALE_CONNECTION,
    /**
     * This error is sent by the server when creating a new listener and the server has exceeded the
     * maximum number of connections specified by the max_connections server option. The default is
     * 64k.
     */
    MAXIMUM_CONNECTIONS_EXCEEDED,
    /**
     * The server pending data size for the listener has reached the maximum size (default 10MB).
     */
    SLOW_CONSUMER,
    /**
     * Client attempted to publish a message with a payload size that exceeds the max_payload size
     * configured on the server. This value is supplied to the client upon listener in the initial
     * INFO message. The client is expected to do proper accounting of byte size to be sent to the
     * server in order to handle this error synchronously.
     */
    MAXIMUM_PAYLOAD_VIOLATION,

    /**
     * Client sent a malformed subject (e.g. sub foo. 90)
     */
    INVALID_SUBJECT(false),
    /**
     * The user specified in the CONNECT message does not have permission to subscribe to the
     * subject.
     */
    PERMISSIONS_VIOLATION_FOR_SUBSCRIPTION(false),
    /**
     * The user specified in the CONNECT message does not have permissions to publish to the
     * subject.
     */
    PERMISSIONS_VIOLATION_FOR_PUBLISH(false),;

    public final boolean fatal;

    Reason() {
      fatal = true;
    }

    Reason(boolean fatal) {
      this.fatal = fatal;
    }
  }

  public interface Listener {

    void onInfo(ByteBuf byteBuf);

    void onSubject(ByteBuf byteBuf);

    void onMessage(long sid, byte[] replyTo, int replyToLength, ByteBuf payload);

    void protocolErr(String s);

    default void onErr(Reason reason, String msg) {

    }

    default void onOK() {

    }

    default void onPing() {

    }

    default void onPong() {

    }
  }
}
