/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.http.websocketx.extensions.compression;

import static io.netty.handler.codec.http.websocketx.extensions.compression.PerMessageDeflateDecoder.FRAME_TAIL;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketExtensionEncoder;
import java.util.List;
import move.http.WebSocketMessageTooBigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deflate implementation of a payload compressor for
 * <tt>io.netty.handler.codec.http.websocketx.WebSocketFrame</tt>.
 */
abstract class DeflateEncoder extends WebSocketExtensionEncoder {

  static final Logger LOG = LoggerFactory.getLogger(DeflateEncoder.class);

  private final int compressionLevel;
  private final int windowSize;
  private final boolean noContext;
  private final int maxSize;

  private EmbeddedChannel encoder;

  /**
   * Constructor
   *
   * @param compressionLevel compression level of the compressor.
   * @param windowSize maximum size of the window compressor buffer.
   * @param noContext true to disable context takeover.
   */
  public DeflateEncoder(int compressionLevel, int windowSize, boolean noContext, int maxSize) {
    this.compressionLevel = compressionLevel;
    this.windowSize = windowSize;
    this.noContext = noContext;
    this.maxSize = maxSize;
  }

  /**
   * @param msg the current frame.
   * @return the rsv bits to set in the compressed frame.
   */
  protected abstract int rsv(WebSocketFrame msg);

  /**
   * @param msg the current frame.
   * @return true if compressed payload tail needs to be removed.
   */
  protected abstract boolean removeFrameTail(WebSocketFrame msg);

  @Override
  protected void encode(ChannelHandlerContext ctx, WebSocketFrame msg,
      List<Object> out) throws Exception {
    if (encoder == null) {
      encoder = new EmbeddedChannel(ZlibCodecFactory.newZlibEncoder(
          ZlibWrapper.NONE, compressionLevel, windowSize, 8));
    }

    encoder.writeOutbound(msg.content().retain());

    if (maxSize > 0 && msg.content().capacity() > maxSize) {
      throw new WebSocketMessageTooBigException(maxSize);
    }

    final int uncompressedSize = msg.content().capacity();
    CompositeByteBuf fullCompressedContent = ctx.alloc().compositeBuffer();
    int compressedSize = 0;
    for (; ; ) {
      ByteBuf partCompressedContent = encoder.readOutbound();
      if (partCompressedContent == null) {
        break;
      }
      if (!partCompressedContent.isReadable()) {
        partCompressedContent.release();
        continue;
      }

      compressedSize += partCompressedContent.capacity();
      fullCompressedContent.addComponent(true, partCompressedContent);
    }
    System.out.println("Compression Saved: " + (uncompressedSize - compressedSize));
    if (fullCompressedContent.numComponents() <= 0) {
      fullCompressedContent.release();
      throw new CodecException("cannot read compressed buffer");
    }

    if (msg.isFinalFragment() && noContext) {
      cleanup();
    }

    ByteBuf compressedContent;
    if (removeFrameTail(msg)) {
      int realLength = fullCompressedContent.readableBytes() - FRAME_TAIL.length;
      compressedContent = fullCompressedContent.slice(0, realLength);
    } else {
      compressedContent = fullCompressedContent;
    }

    WebSocketFrame outMsg;
    if (msg instanceof TextWebSocketFrame) {
      outMsg = new TextWebSocketFrame(msg.isFinalFragment(), rsv(msg), compressedContent);
    } else if (msg instanceof BinaryWebSocketFrame) {
      outMsg = new BinaryWebSocketFrame(msg.isFinalFragment(), rsv(msg), compressedContent);
    } else if (msg instanceof ContinuationWebSocketFrame) {
      outMsg = new ContinuationWebSocketFrame(msg.isFinalFragment(), rsv(msg), compressedContent);
    } else {
      throw new CodecException("unexpected frame type: " + msg.getClass().getName());
    }
    out.add(outMsg);
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    cleanup();
    super.handlerRemoved(ctx);
  }

  private void cleanup() {
    if (encoder != null) {
      // Clean-up the previous encoder if not cleaned up correctly.
      if (encoder.finish()) {
        for (; ; ) {
          ByteBuf buf = encoder.readOutbound();
          if (buf == null) {
            break;
          }
          // Release the buffer
          buf.release();
        }
      }
      encoder = null;
    }
  }
}