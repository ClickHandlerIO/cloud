package move.nats;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;

/**
 * NATS Protocol Parser optimized for Netty ByteBuf.
 *
 * @auth Clay Molocznik
 */
public class MParser {

  static final int RECV_BUFFER_SIZE = 2 * 1024 * 1024;
  static final int SEND_BUFFER_SIZE = 2 * 1024 * 1024;
  static final int ascii_0 = 48;
  static final int ascii_9 = 57;

  static final byte ZERO = (byte) '0';
  static final byte[] digits = new byte[]{(byte) '0', (byte) '1', (byte) '2', (byte) '3',
      (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9'};

  static final byte i = (byte)'i';
  static final byte I = (byte)'I';

  static final byte n = (byte)'n';
  static final byte N = (byte)'N';

  static final byte f = (byte)'f';
  static final byte F = (byte)'F';

  static final byte o = (byte)'o';
  static final byte O = (byte)'O';

  static final byte g = (byte)'g';
  static final byte G = (byte)'G';

  static final byte p = (byte)'p';
  static final byte P = (byte)'P';

  static final byte m = (byte)'m';
  static final byte M = (byte)'M';

  static final byte s = (byte)'s';
  static final byte S = (byte)'S';

  static final byte k = (byte)'k';
  static final byte K = (byte)'K';

  static final byte r = (byte)'r';
  static final byte R = (byte)'R';

  static final byte e = (byte)'e';
  static final byte E = (byte)'E';

  static final byte CR = (byte)'\r';
  static final byte LF = (byte)'\n';

  static final byte SPACE = (byte)' ';
  static final byte PLUS = (byte)'+';
  static final byte MINUS = (byte)'-';

  final Connection connection;

  public MParser(Connection connection) {
    this.connection = connection;
  }

  private State state;
  private Buffer info = Buffer.buffer();

  private Buffer payload = Buffer.buffer();

  private long sid;
  private ByteBuf replyTo;
  private int bytes;

  @SuppressWarnings("all")
  public void parse(ByteBuf buffer) {
    while (parse0(buffer)) {}
  }

  @SuppressWarnings("all")
  private boolean parse0(ByteBuf buffer) {
    switch (state) {
      case M_OR_P_OR_I_PLUS_OR_MINUS:
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          default:
            switch (buffer.readByte()) {
              case m:
              case M:
                state = State.MS;
                return buffer.readableBytes() > 0;

              case p:
              case P:
                if (buffer.readableBytes() == 0) {
                  state = State.P_;
                  return false;
                }

                switch (buffer.readByte()) {
                  case o:
                  case O:
                    state = State.PO;
                    return buffer.readableBytes() > 0;

                  case i:
                  case I:
                    state = State.PI;
                    return buffer.readableBytes() > 0;

                  default:
                    connection.protocolErr("Expected 'o' or 'O' or 'i' or 'I' after 'P'");
                    return false;
                }

              case i:
              case I:
                state = State.IN;
                return buffer.readableBytes() > 0;
            }
            break;
        }
        break;

      case I:
        switch (buffer.readableBytes()) {
          case 0:
            return false;

          case 1:
            switch (buffer.readByte()) {
              case i:
              case I:
                break;

              default:
                connection.protocolErr("Expected 'I' or 'i' in INFO");
                return false;
            }
            state = State.IN;

            break;

          case 2:
            switch (buffer.readByte()) {
              case i:
              case I:
                break;

              default:
                connection.protocolErr("Expected 'I' or 'i' in INFO");
                return false;
            }
            switch (buffer.readByte()) {
              case n:
              case N:
                break;

              default:
                connection.protocolErr("Expected 'N' or 'n' in INFO");
                return false;
            }

            state = State.INF;
            break;

          case 3:
            switch (buffer.readByte()) {
              case i:
              case I:
                break;

              default:
                connection.protocolErr("Expected 'I' or 'i' in INFO");
                return false;
            }
            switch (buffer.readByte()) {
              case n:
              case N:
                break;

              default:
                connection.protocolErr("Expected 'N' or 'n' in INFO");
                return false;
            }
            switch (buffer.readByte()) {
              case f:
              case F:
                break;

              default:
                connection.protocolErr("Expected 'F' or 'f' in INFO");
                return false;
            }

            state = State.INFO;
            break;

          case 4:
            switch (buffer.readByte()) {
              case i:
              case I:
                break;

              default:
                connection.protocolErr("Expected 'I' or 'i' in INFO");
                return false;
            }
            switch (buffer.readByte()) {
              case n:
              case N:
                break;

              default:
                connection.protocolErr("Expected 'N' or 'n' in INFO");
                return false;
            }
            switch (buffer.readByte()) {
              case f:
              case F:
                break;

              default:
                connection.protocolErr("Expected 'F' or 'f' in INFO");
                return false;
            }
            switch (buffer.readByte()) {
              case o:
              case O:
                break;

              default:
                connection.protocolErr("Expected 'O' or 'o' in INFO");
                return false;
            }

            state = State.INFO_SPACE;
            break;

          case 5:
            switch (buffer.readByte()) {
              case i:
              case I:
                break;

              default:
                connection.protocolErr("Expected 'I' or 'i' in INFO");
                return false;
            }
            switch (buffer.readByte()) {
              case n:
              case N:
                break;

              default:
                connection.protocolErr("Expected 'N' or 'n' in INFO");
                return false;
            }
            switch (buffer.readByte()) {
              case f:
              case F:
                break;

              default:
                connection.protocolErr("Expected 'F' or 'f' in INFO");
                return false;
            }
            switch (buffer.readByte()) {
              case o:
              case O:
                break;

              default:
                connection.protocolErr("Expected 'O' or 'o' in INFO");
                return false;
            }

            if (buffer.readByte() != SPACE) {
              connection.protocolErr("Expected ' ' after INFO");
            }

            state = State.INFO_PAYLOAD_CR;
            break;

          default:
            switch (buffer.readByte()) {
              case i:
              case I:
                break;

              default:
                connection.protocolErr("Expected 'I' or 'i' in INFO");
                return false;
            }
            switch (buffer.readByte()) {
              case n:
              case N:
                break;

              default:
                connection.protocolErr("Expected 'N' or 'n' in INFO");
                return false;
            }
            switch (buffer.readByte()) {
              case f:
              case F:
                break;

              default:
                connection.protocolErr("Expected 'F' or 'f' in INFO");
                return false;
            }
            switch (buffer.readByte()) {
              case o:
              case O:
                break;

              default:
                connection.protocolErr("Expected 'O' or 'o' in INFO");
                return false;
            }

            if (buffer.readByte() != SPACE) {
              connection.protocolErr("Expected ' ' after INFO");
            }

            final int index = buffer.readerIndex();
            while (buffer.readableBytes() > 0) {
              final byte b = buffer.readByte();

              if (b == CR) {
                if (buffer.readableBytes() == 0) {
                  info = Buffer.buffer(buffer.slice(index, buffer.readerIndex() - 1 - index));
                  state = State.INFO_PAYLOAD_CRLF;
                  return false;
                }

                if (buffer.readByte() == LF) {
                  info = Buffer.buffer(buffer.slice(index, buffer.readerIndex() - 2 - index));
                }

                state = State.M_OR_P_OR_I_PLUS_OR_MINUS;
                return buffer.readableBytes() > 0;
              }
            }

            info = Buffer.buffer(buffer.slice(index, buffer.readerIndex() - index));
            return false;
        }
        break;

      case IN:
        break;

      case INF:
        break;

      case INFO:
        break;

      case INFO_SPACE:
        break;


      case MS:
        break;

      case MSG:
        break;

      case MSG_SPACE:
        break;

      case MSG_SPACE_SPACE:
        break;

      case MSG_SPACE_SPACE_SPACE:
        break;
    }

    return false;
  }

  enum State {
    I,
    IN,
    INF,
    INFO,
    INFO_SPACE,
    INFO_PAYLOAD_CR,
    INFO_PAYLOAD_CRLF,

    M_OR_P_OR_I_PLUS_OR_MINUS,
    MS,
    MSG,
    MSG_SPACE,
    MSG_SPACE_SPACE,
    MSG_SPACE_SPACE_SPACE,
    MSG_SPACE_SPACE_CR,
    MSG_CR,
    MSG_CRLF,

    PLUS_,
    PLUS_O,
    PLUS_OK,
    PLUS_OK_CR,
    PLUS_OK_CRLF,
    MINUS_,
    MINUS_E,
    MINUS_ER,
    MINUS_ERR,
    MINUS_ERR_SPACE,
    MINUS_ERR_SPACE_CR,
    MINUS_ERR_SPACE_CRLF,

    P_,

    PI,
    PIN,
    PING,
    PING_SPACE,

    PO,
    PON,
    PONG,
    PONG_SPACE,
    PONG_SPACE_CR,
    PONG_SPACE_CRLF,
  }
}
