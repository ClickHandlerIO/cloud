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

import io.netty.handler.codec.http.websocketx.extensions.*;

import java.util.Collections;

/**
 * <a href="https://tools.ietf.org/id/draft-tyoshino-hybi-websocket-perframe-deflate-06.txt">perframe-deflate</a>
 * handshake implementation.
 */
public final class DeflateFrameServerExtensionHandshaker implements WebSocketServerExtensionHandshaker {

    static final String X_WEBKIT_DEFLATE_FRAME_EXTENSION = "x-webkit-deflate-frame";
    static final String DEFLATE_FRAME_EXTENSION = "deflate-frame";

    private final int compressionLevel;
    private final int maxSize;

    /**
     * Constructor with default configuration.
     */
    public DeflateFrameServerExtensionHandshaker(int maxSize) {
        this(6, maxSize);
    }

    /**
     * Constructor with custom configuration.
     *
     * @param compressionLevel
     *            Compression level between 0 and 9 (default is 6).
     */
    public DeflateFrameServerExtensionHandshaker(int compressionLevel, int maxSize) {
        if (compressionLevel < 0 || compressionLevel > 9) {
            throw new IllegalArgumentException(
                    "compressionLevel: " + compressionLevel + " (expected: 0-9)");
        }
        this.compressionLevel = compressionLevel;
        this.maxSize = maxSize;
    }

    @Override
    public WebSocketServerExtension handshakeExtension(WebSocketExtensionData extensionData) {
        if (!X_WEBKIT_DEFLATE_FRAME_EXTENSION.equals(extensionData.name()) &&
            !DEFLATE_FRAME_EXTENSION.equals(extensionData.name())) {
            return null;
        }

        if (extensionData.parameters().isEmpty()) {
            return new DeflateFrameServerExtension(compressionLevel, extensionData.name(), maxSize);
        } else {
            return null;
        }
    }

    private static class DeflateFrameServerExtension implements WebSocketServerExtension {

        private final String extensionName;
        private final int compressionLevel;
        private final int maxSize;

        public DeflateFrameServerExtension(int compressionLevel, String extensionName, int maxSize) {
            this.extensionName = extensionName;
            this.compressionLevel = compressionLevel;
            this.maxSize = maxSize;
        }

        @Override
        public int rsv() {
            return RSV1;
        }

        @Override
        public WebSocketExtensionEncoder newExtensionEncoder() {
            return new PerFrameDeflateEncoder(compressionLevel, 15, false, maxSize);
        }

        @Override
        public WebSocketExtensionDecoder newExtensionDecoder() {
            return new PerFrameDeflateDecoder(false, maxSize);
        }

        @Override
        public WebSocketExtensionData newReponseData() {
            return new WebSocketExtensionData(extensionName, Collections.<String, String>emptyMap());
        }
    }

}
