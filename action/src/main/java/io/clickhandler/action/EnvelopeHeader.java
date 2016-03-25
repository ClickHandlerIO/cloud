package io.clickhandler.action;

import com.google.common.base.Strings;
import javaslang.control.Try;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 *
 */
class EnvelopeHeader {
    public static final int ASK = 0;
    public static final int REPLY = 1;
    public static final int ACTOR_TYPE_NOT_FOUND = 3;
    public static final int ACTION_NOT_FOUND = 4;
    public static final int ACTOR_NOT_FOUND = 5;
    public static final int REMOTE_FAILURE = 6;
    public static final int REMOTE_ACTION_TIMEOUT = 7;
    public static final int BAD_ENVELOPE = 8;
    public static final int NODE_CHANGED = 9;
    public static final int BAD_REQUEST_FORMAT = 10;
    public static final int EXPECTED_ASK = 11;
    public static final int ACTOR_UNAVAILABLE = 12;

    private final static Logger log = LoggerFactory.getLogger(EnvelopeHeader.class);

    int type;
    int partition;
    String nodeId;
    String actorName;
    String key;
    String actionName;
    int headerSize;
    int bodySize;

    static EnvelopeHeader parse(byte[] data) {
        // Create unpacker.
        final MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data);
        final EnvelopeHeader header = new EnvelopeHeader();

        try {
            header.type = unpacker.unpackInt();
            header.partition = unpacker.unpackInt();
            header.nodeId = Strings.nullToEmpty(unpacker.unpackString());
            header.actorName = Strings.nullToEmpty(unpacker.unpackString());
            header.key = Strings.nullToEmpty(unpacker.unpackString());
            header.actionName = Strings.nullToEmpty(unpacker.unpackString());
            header.headerSize = (int) unpacker.getTotalReadBytes();
            header.bodySize = data.length - header.headerSize;
        } catch (Throwable e) {
            log.warn("Failed to Parse EnvelopeHeader", e);
            return null;
        } finally {
            Try.run(unpacker::close);
        }

        return header;
    }

    static byte[] create(int code) {
        return create(code, 0, "", "", "", "");
    }

    static byte[] create(int type,
                         int partition,
                         String nodeId,
                         String actorName,
                         String actionName,
                         String key) {
        final MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        try {
            packer.packInt(type);
            packer.packInt(partition);
            packer.packString(nodeId);
            packer.packString(actorName);
            packer.packString(actionName);
            packer.packString(key);
        } catch (Throwable e) {
            log.error("Failed to Pack Response Envelope", e);
        }

        return packer.toByteArray();
    }

    public byte[] toByteArray(byte[] payload) throws IOException {
        final MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        packer.packInt(type);
        packer.packInt(partition);
        packer.packString(nodeId);
        packer.packString(actorName);
        packer.packString(key);
        packer.packString(actionName);
        packer.writePayload(payload);
        try {
            return packer.toByteArray();
        } finally {
            Try.run(packer::close);
        }
    }
}
