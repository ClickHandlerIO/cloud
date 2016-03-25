package io.clickhandler.action;

import io.clickhandler.common.WireFormat;

/**
 *
 */
public class ActorActionSerializerImpl implements ActorActionSerializer {
    @Override
    public byte[] byteify(Object value) {
        return WireFormat.byteify(value);
    }

    @Override
    public <T> T parse(Class<T> type, byte[] data) {
        return WireFormat.parse(type, data);
    }

    @Override
    public <T> T parse(Class<T> type, byte[] data, int offset, int length) {
        return WireFormat.parse(type, data, offset, length);
    }
}
