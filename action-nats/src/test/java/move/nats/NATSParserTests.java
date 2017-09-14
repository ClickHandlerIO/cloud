package move.nats;

import static move.nats.NATSParser.CR;
import static move.nats.NATSParser.DIGITS;
import static move.nats.NATSParser.E;
import static move.nats.NATSParser.F;
import static move.nats.NATSParser.G;
import static move.nats.NATSParser.I;
import static move.nats.NATSParser.LF;
import static move.nats.NATSParser.M;
import static move.nats.NATSParser.N;
import static move.nats.NATSParser.O;
import static move.nats.NATSParser.P;
import static move.nats.NATSParser.S;
import static move.nats.NATSParser.SPACE;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.internal.PlatformDependent;
import io.vertx.core.buffer.Buffer;
import java.nio.charset.StandardCharsets;
import move.NUID;
import move.nats.NATSParser.Listener;
import move.nats.NATSParser.Reason;
import move.nats.NATSParser.State;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class NATSParserTests {

  @Test
  public void testInfo() {
    final ParserProxy parser = new ParserProxy();

    parser.parse(Buffer.buffer(new byte[]{I, N, NATSParser.F, O, NATSParser.SPACE}).getByteBuf());

    System.out.println(parser.state());
  }

  @Test
  public void testInfoOneByOne() {
    final ParserProxy parser = new ParserProxy();

    parser.parse(I);
    parser.assertState(State.IN);
    parser.parse(E);
    parser.assertFailed();

    parser.reset();
    parser.parse(I);
    parser.parse(N);
    parser.assertState(State.INF);
    parser.parse(E);
    parser.assertFailed();

    parser.reset();
    parser.parse(I);
    parser.parse(N);
    parser.parse(F);
    parser.assertState(State.INFO);
    parser.parse(E);
    parser.assertFailed();

    parser.reset();
    parser.parse(I);
    parser.parse(N);
    parser.parse(F);
    parser.parse(O);
    parser.assertState(State.INFO_SPACE);
    parser.parse(E);
    parser.assertFailed();

    parser.reset();
    parser.parse(I);
    parser.parse(N);
    parser.parse(F);
    parser.parse(O);
    parser.parse(SPACE);
    parser.assertState(State.INFO_PAYLOAD);

    parser.reset();
    parser.parse(I);
    parser.parse(N);
    parser.parse(F);
    parser.parse(O);
    parser.parse(SPACE);
    parser.parse((byte) '{');
    parser.assertState(State.INFO_PAYLOAD);
    parser.parse((byte) '}');
    parser.parse(CR);
    parser.assertState(State.INFO_PAYLOAD_CRLF);
    parser.parse(LF);
    parser.assertState(State.IDLE);
    Assert.assertEquals("{}", parser.info);
  }

  @Test
  public void testMSGOneByOne() {
    final ParserProxy parser = new ParserProxy();

    parser.parse(M);
    parser.assertState(State.MS);
    parser.parse(E);
    parser.assertFailed();

    parser.reset();
    parser.parse(M);
    parser.parse(S);
    parser.assertState(State.MSG);
    parser.parse(E);
    parser.assertFailed();

    parser.reset();
    parser.parse(M);
    parser.parse(S);
    parser.parse(G);
    parser.assertState(State.MSG_SPACE);
    parser.parse(E);
    parser.assertFailed();

    parser.reset();
    parser.parse(M);
    parser.parse(S);
    parser.parse(G);
    parser.parse(SPACE);
    parser.assertState(State.MSG_SUBJECT);
    parser.parse(E);
    parser.assertState(State.MSG_SUBJECT);
    parser.parse(SPACE);
    parser.assertState(State.MSG_SID);
    parser.parse(DIGITS[1]);
    parser.parse(SPACE);
    parser.assertState(State.MSG_REPLY_OR_BYTES);
    parser.parse(DIGITS[1]);
    parser.assertState(State.MSG_REPLY_OR_BYTES);
    parser.parse(DIGITS[2]);
    parser.assertState(State.MSG_REPLY_OR_BYTES);
    parser.parse(SPACE);
    parser.assertState(State.MSG_BYTES);

    // MSG E 1 12 1\r\nM\r\n
    parser.reset();
    parser.parse(M);
    parser.parse(S);
    parser.parse(G);
    parser.parse(SPACE);
    parser.assertState(State.MSG_SUBJECT);
    parser.parse(E);
    parser.assertState(State.MSG_SUBJECT);
    parser.parse(SPACE);
    parser.assertState(State.MSG_SID);
    parser.parse(DIGITS[1]);
    parser.parse(SPACE);
    parser.assertState(State.MSG_REPLY_OR_BYTES);
    parser.parse(DIGITS[1]);
    parser.assertState(State.MSG_REPLY_OR_BYTES);
    parser.parse(DIGITS[2]);
    parser.assertState(State.MSG_REPLY_OR_BYTES);
    parser.parse(SPACE);
    parser.assertState(State.MSG_BYTES);
    parser.parse(DIGITS[1]);
    parser.assertState(State.MSG_BYTES);
    parser.parse(CR);
    parser.assertState(State.MSG_BYTES_CRLF);
    parser.parse(LF);
    parser.assertState(State.MSG_PAYLOAD);
    parser.parse(M);
    parser.assertState(State.MSG_PAYLOAD_CR);
    parser.parse(CR);
    parser.assertState(State.MSG_PAYLOAD_CRLF);
    parser.parse(LF);


  }

  @Test
  public void testMsg() {
    final ParserProxy parser = new ParserProxy();

    parser.parse("MSG subject INBOX.0 2\r\nHI\r\n");
    parser.assertState(State.FAILED);

    parser.parseAssert("MSG subject 1 INBOX.0 2\r\nHI\r\n", "subject", 1, "INBOX.0", "HI");
  }

//  @Test
//  public void testMSGBenchmark() {
//    final ParserProxy parser = new ParserProxy();
//
//    int samples = 10;
//    int count = 2000000;
//    String payload = NUID.nextGlobal()
//        + "HI HIHI HIHI HIHI HIHI HIHI HIHI HIHI HIHI HIHI HIHI HIHI HIHI HIHI HIHI HI";
//
////    final byte[] BUFFER = ("MSG m.0 1 " + payload.length() + "\r\n" + payload + "\r\n").getBytes(StandardCharsets.US_ASCII);
//    final byte[] BUFFER = ("MSG m 1 " + payload.length() + "\r\n" + payload + "\r\n")
//        .getBytes(StandardCharsets.UTF_8);
////    final byte[] BUFFER = "MSG subject 1 2\r\nHI\r\n".getBytes(StandardCharsets.US_ASCII);
//
//    final Buffer buffer = Buffer.buffer(BUFFER);
//    final ByteBuf byteBuf = buffer.getByteBuf();
//
//    final ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer();
//    final ByteBuf buf2 = PooledByteBufAllocator.DEFAULT.directBuffer();
//    buf.writeBytes(BUFFER);
//
//    for (int x = 0; x < samples; x++) {
//      long start = System.currentTimeMillis();
//      for (int i = 0; i < count; i++) {
//        parser.parse(buf);
//        PlatformDependent.copyMemory(buf.memoryAddress(), buf2.memoryAddress(), buf.readerIndex());
//        buf.resetReaderIndex();
//
//        byte b = PlatformDependent.getByte(buf2.memoryAddress());
//        byte b2 = PlatformDependent.getByte(buf2.memoryAddress() + 1);
////        System.out.println(String.valueOf((char)b));
////        System.out.println(String.valueOf((char)b2));
//      }
//      long elapsed = System.currentTimeMillis() - start;
//      double runsPerSecond = 1000.0 / elapsed;
//      System.out.println((count * runsPerSecond) + " / sec");
//    }
//  }


  @Test
  public void testPING() {
    final ParserProxy parser = new ParserProxy();
    int pingCounter = 0;

    // Full
    parser.parse(P, I, N, G, CR, LF);
    Assert.assertEquals(++pingCounter, parser.pingCount);
    parser.reset();

    // One by One.
    parser.parse(P);
    parser.assertState(State.P_);
    parser.parse(I);
    parser.assertState(State.PIN);
    parser.parse(N);
    parser.assertState(State.PING);
    parser.parse(G);
    parser.assertState(State.PING_CR);
    parser.parse(CR);
    parser.assertState(State.PING_CRLF);
    parser.parse(LF);
    parser.assertState(State.IDLE);
    Assert.assertEquals(++pingCounter, parser.pingCount);

    parser.parse(P);
    parser.assertState(State.P_);
    parser.parse(I, N);
    parser.assertState(State.PING);
    parser.parse(G, CR);
    parser.assertState(State.PING_CRLF);
    parser.parse(LF);
    parser.assertState(State.IDLE);
    Assert.assertEquals(++pingCounter, parser.pingCount);

    parser.parse(P);
    parser.assertState(State.P_);
    parser.parse(I, N, G);
    parser.assertState(State.PING_CR);
    parser.parse(CR, LF);
    parser.assertState(State.IDLE);
    Assert.assertEquals(++pingCounter, parser.pingCount);

    parser.parse(P);
    parser.assertState(State.P_);
    parser.parse(I, N, G, CR);
    parser.assertState(State.PING_CRLF);
    parser.parse(LF);
    parser.assertState(State.IDLE);
    Assert.assertEquals(++pingCounter, parser.pingCount);

    parser.parse(P, I);
    parser.assertState(State.PIN);
    parser.parse(N, G, CR);
    parser.assertState(State.PING_CRLF);
    parser.parse(LF);
    parser.assertState(State.IDLE);
    Assert.assertEquals(++pingCounter, parser.pingCount);

    // Test PIN
    parser.parse(P, I);
    parser.assertState(State.PIN);
    parser.parse(N);
    parser.assertState(State.PING);
    parser.reset();
    parser.parse(P, I);
    parser.assertState(State.PIN);
    parser.parse(N, G);
    parser.assertState(State.PING_CR);
    parser.reset();
    parser.parse(P, I);
    parser.assertState(State.PIN);
    parser.parse(N, G, CR);
    parser.assertState(State.PING_CRLF);
    parser.reset();
    parser.parse(P, I);
    parser.assertState(State.PIN);
    parser.parse(N, G, CR, LF);
    parser.assertState(State.IDLE);
    Assert.assertEquals(++pingCounter, parser.pingCount);
    parser.reset();
    parser.parse(P, I);
    parser.assertState(State.PIN);
    parser.parse(N, G, CR, LF, P);
    parser.assertState(State.P_);
    Assert.assertEquals(++pingCounter, parser.pingCount);
    parser.reset();

    // Test PING
    parser.parse(P, I, N);
    parser.assertState(State.PING);
    parser.parse(G);
    parser.assertState(State.PING_CR);
    parser.reset();
    parser.parse(P, I, N);
    parser.assertState(State.PING);
    parser.parse(G, CR);
    parser.assertState(State.PING_CRLF);
    parser.reset();
    parser.parse(P, I, N);
    parser.assertState(State.PING);
    parser.parse(G, CR, LF);
    parser.assertState(State.IDLE);
    Assert.assertEquals(++pingCounter, parser.pingCount);
    parser.reset();
    parser.parse(P, I, N);
    parser.assertState(State.PING);
    parser.parse(G, CR, LF, M);
    parser.assertState(State.MS);
    Assert.assertEquals(++pingCounter, parser.pingCount);
    parser.reset();

    // Test PING<CR>
    parser.parse(P, I, N, G);
    parser.assertState(State.PING_CR);
    parser.parse(CR);
    parser.assertState(State.PING_CRLF);
    parser.reset();
    parser.parse(P, I, N, G);
    parser.assertState(State.PING_CR);
    parser.parse(CR, LF);
    parser.assertState(State.IDLE);
    Assert.assertEquals(++pingCounter, parser.pingCount);
    parser.reset();
    parser.parse(P, I, N, G);
    parser.assertState(State.PING_CR);
    parser.parse(CR, LF, M);
    parser.assertState(State.MS);
    Assert.assertEquals(++pingCounter, parser.pingCount);
    parser.reset();

    // Test PING<CR><LF>
    parser.parse(P, I, N, G, CR);
    parser.assertState(State.PING_CRLF);
    parser.parse(LF);
    parser.assertState(State.IDLE);
    Assert.assertEquals(++pingCounter, parser.pingCount);
    parser.reset();
    parser.parse(P, I, N, G, CR);
    parser.assertState(State.PING_CRLF);
    parser.parse(LF, M);
    parser.assertState(State.MS);
    Assert.assertEquals(++pingCounter, parser.pingCount);
  }

//  @Test
//  public void testPINGBenchmark() {
//    final ParserProxy parser = new ParserProxy();
//
//    int samples = 10;
//    int count = 2000000;
//
//    for (int x = 0; x < samples; x++) {
//      long start = System.currentTimeMillis();
//      for (int i = 0; i < count; i++) {
//        final byte[] BUFFER = new byte[]{P, I, N, G, CR, LF};
//        parser.parse(BUFFER);
//      }
//      long elapsed = System.currentTimeMillis() - start;
//      double runsPerSecond = 1000.0 / elapsed;
//      System.out.println((count * runsPerSecond) + " / sec");
//
//      Assert.assertEquals((x + 1) * count, parser.pingCount);
//    }
//  }
//
//  @Test
//  public void testPONGBenchmark() {
//    final ParserProxy parser = new ParserProxy();
//
//    int samples = 10;
//    int count = 2000000;
//
//    for (int x = 0; x < samples; x++) {
//      long start = System.currentTimeMillis();
//      for (int i = 0; i < count; i++) {
//        final byte[] BUFFER = new byte[]{P, O, N, G, CR, LF};
//        parser.parse(BUFFER);
//      }
//      long elapsed = System.currentTimeMillis() - start;
//      double runsPerSecond = 1000.0 / elapsed;
//      System.out.println((count * runsPerSecond) + " / sec");
//
//      Assert.assertEquals((x + 1) * count, parser.pongCount);
//    }
//  }

  @Test
  public void testPONG() {
    final ParserProxy parser = new ParserProxy();
    int counter = 0;

    // Full
    parser.parse(P, O, N, G, CR, LF);
    Assert.assertEquals(++counter, parser.pongCount);
    parser.reset();

    // One by One.
    parser.parse(P);
    parser.assertState(State.P_);
    parser.parse(O);
    parser.assertState(State.PON);
    parser.parse(N);
    parser.assertState(State.PONG);
    parser.parse(G);
    parser.assertState(State.PONG_CR);
    parser.parse(CR);
    parser.assertState(State.PONG_CRLF);
    parser.parse(LF);
    parser.assertState(State.IDLE);
    Assert.assertEquals(++counter, parser.pongCount);

    parser.parse(P);
    parser.assertState(State.P_);
    parser.parse(O, N);
    parser.assertState(State.PONG);
    parser.parse(G, CR);
    parser.assertState(State.PONG_CRLF);
    parser.parse(LF);
    parser.assertState(State.IDLE);
    Assert.assertEquals(++counter, parser.pongCount);

    parser.parse(P);
    parser.assertState(State.P_);
    parser.parse(O, N, G);
    parser.assertState(State.PONG_CR);
    parser.parse(CR, LF);
    parser.assertState(State.IDLE);
    Assert.assertEquals(++counter, parser.pongCount);

    parser.parse(P);
    parser.assertState(State.P_);
    parser.parse(O, N, G, CR);
    parser.assertState(State.PONG_CRLF);
    parser.parse(LF);
    parser.assertState(State.IDLE);
    Assert.assertEquals(++counter, parser.pongCount);

    parser.parse(P, O);
    parser.assertState(State.PON);
    parser.parse(N, G, CR);
    parser.assertState(State.PONG_CRLF);
    parser.parse(LF);
    parser.assertState(State.IDLE);
    Assert.assertEquals(++counter, parser.pongCount);

    // Test PON
    parser.parse(P, O);
    parser.assertState(State.PON);
    parser.parse(N);
    parser.assertState(State.PONG);
    parser.reset();
    parser.parse(P, O);
    parser.assertState(State.PON);
    parser.parse(N, G);
    parser.assertState(State.PONG_CR);
    parser.reset();
    parser.parse(P, O);
    parser.assertState(State.PON);
    parser.parse(N, G, CR);
    parser.assertState(State.PONG_CRLF);
    parser.reset();
    parser.parse(P, O);
    parser.assertState(State.PON);
    parser.parse(N, G, CR, LF);
    parser.assertState(State.IDLE);
    Assert.assertEquals(++counter, parser.pongCount);
    parser.reset();
    parser.parse(P, O);
    parser.assertState(State.PON);
    parser.parse(N, G, CR, LF, P);
    parser.assertState(State.P_);
    Assert.assertEquals(++counter, parser.pongCount);
    parser.reset();

    // Test PONG
    parser.parse(P, O, N);
    parser.assertState(State.PONG);
    parser.parse(G);
    parser.assertState(State.PONG_CR);
    parser.reset();
    parser.parse(P, O, N);
    parser.assertState(State.PONG);
    parser.parse(G, CR);
    parser.assertState(State.PONG_CRLF);
    parser.reset();
    parser.parse(P, O, N);
    parser.assertState(State.PONG);
    parser.parse(G, CR, LF);
    parser.assertState(State.IDLE);
    Assert.assertEquals(++counter, parser.pongCount);
    parser.reset();
    parser.parse(P, O, N);
    parser.assertState(State.PONG);
    parser.parse(G, CR, LF, M);
    parser.assertState(State.MS);
    Assert.assertEquals(++counter, parser.pongCount);
    parser.reset();

    // Test PONG<CR>
    parser.parse(P, O, N, G);
    parser.assertState(State.PONG_CR);
    parser.parse(CR);
    parser.assertState(State.PONG_CRLF);
    parser.reset();
    parser.parse(P, O, N, G);
    parser.assertState(State.PONG_CR);
    parser.parse(CR, LF);
    parser.assertState(State.IDLE);
    Assert.assertEquals(++counter, parser.pongCount);
    parser.reset();
    parser.parse(P, O, N, G);
    parser.assertState(State.PONG_CR);
    parser.parse(CR, LF, M);
    parser.assertState(State.MS);
    Assert.assertEquals(++counter, parser.pongCount);
    parser.reset();

    // Test PONG<CR><LF>
    parser.parse(P, O, N, G, CR);
    parser.assertState(State.PONG_CRLF);
    parser.parse(LF);
    parser.assertState(State.IDLE);
    Assert.assertEquals(++counter, parser.pongCount);
    parser.reset();
    parser.parse(P, O, N, G, CR);
    parser.assertState(State.PONG_CRLF);
    parser.parse(LF, M);
    parser.assertState(State.MS);
    Assert.assertEquals(++counter, parser.pongCount);
  }

  ByteBuf buffer(byte... bytes) {
    return Buffer.buffer(bytes).getByteBuf();
  }

  public class ParserProxy implements Listener {

    public NATSParser parser = new NATSParser(this);
    public String info;
    public String error;
    private ByteBuf subject;
    private long sid;
    private String replyTo;
    private String payload;

    private int okCount = 0;
    private int pingCount = 0;
    private int pongCount = 0;

    private Reason reason;
    private String reasonMessage;
    private boolean benchmark = false;

    public NATSParser.State state() {
      return parser.state();
    }

    public void parse(String str) {
      parse(str.getBytes(StandardCharsets.US_ASCII));
    }

    public void parseAssert(String command, String subject, long sid, String replyTo,
        String payload) {
      reset();
      parse(command);
      assertState(State.IDLE);
      Assert.assertEquals(this.sid, sid);
      Assert.assertEquals(this.replyTo, replyTo);
      Assert.assertEquals(this.payload, payload);
    }

    public void parse(byte... bytes) {
      parse(buffer(bytes));
    }

    public void parse(ByteBuf buf) {
      parser.parse(buf);
    }

    public void assertState(State state) {
      try {
        Assert.assertEquals(state, state());
      } catch (AssertionError e) {
        System.out.println(error);
        throw e;
      }
    }

    public void assertFailed() {
      Assert.assertEquals(State.FAILED, state());
    }

    public void reset() {
      parser = new NATSParser(this);
      info = "";
      error = "";
    }

    @Override
    public void onErr(Reason reason, String msg) {
      this.reason = reason;
      this.reasonMessage = msg;
    }

    @Override
    public void onOK() {
      okCount++;
    }

    @Override
    public void onPing() {
      pingCount++;
    }

    @Override
    public void onPong() {
      pongCount++;
    }

    @Override
    public void protocolErr(String s) {
      error = s;
    }

    @Override
    public void onInfo(ByteBuf byteBuf) {
      info = byteBuf.toString(StandardCharsets.UTF_8);
    }

    @Override
    public void onSubject(ByteBuf byteBuf) {
      subject = byteBuf;
    }

    @Override
    public void onMessage(long sid, byte[] replyTo, int replyToLength, ByteBuf payload) {
      this.sid = sid;

      if (benchmark) {
        return;
      }

      this.replyTo = (replyToLength > 0 ? new String(replyTo, 0, replyToLength) : null);
      this.payload = payload == null ? "" : payload.toString(StandardCharsets.UTF_8);

//      System.out.println("Subject:  " + subject.toString(StandardCharsets.UTF_8));
//      System.out.println("SID:      " + sid);
//      System.out.println("ReplyTo:  " + (replyToLength > 0 ? new String(replyTo, 0, replyToLength)
//          : "<NO_REPLY_TO>"));
//      System.out.println("Payload:  " + payload.toString(StandardCharsets.UTF_8));
    }
  }

}
