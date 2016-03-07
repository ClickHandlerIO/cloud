package io.clickhandler.remoting;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * WebSocket Remoting Envelope
 */
public class WsEnvelope {
    // Method
    public static final int OUT = 0;
    public static final int IN = 1;
    public static final int SUB = 2;
    public static final int USUB = 3;
    public static final int PUSH = 4;
    public static final int PING = 5;
    public static final int PONG = 6;

    @JsonProperty("m")
    private int method;
    @JsonProperty("i")
    private int id;
    @JsonProperty("s")
    private int sequence;
    @JsonProperty("c")
    private int code;
    @JsonProperty("t")
    private String type;
    @JsonProperty("b")
    private String body;

    public WsEnvelope() {
    }

    public WsEnvelope(int method, int id, int code, String type, String body) {
        this.method = method;
        this.id = id;
        this.code = code;
        this.type = type;
        this.body = body;
    }

    public int getMethod() {
        return method;
    }

    public void setMethod(int method) {
        this.method = method;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
