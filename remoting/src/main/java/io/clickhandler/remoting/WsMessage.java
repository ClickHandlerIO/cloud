package io.clickhandler.remoting;

/**
 *
 */
public class WsMessage {
    public WsHeader header;
    public String body;

    public WsMessage(WsHeader header, String body) {
        this.header = header;
        this.body = body;
    }
}
