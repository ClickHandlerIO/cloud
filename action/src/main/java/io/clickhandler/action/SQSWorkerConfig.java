package io.clickhandler.action;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public class SQSWorkerConfig {
    @JsonProperty
    public String url;
    @JsonProperty
    public int bufferSize = 1000;
    @JsonProperty
    public int threads = 1;
    @JsonProperty
    public int receiveSize = 10;
    @JsonProperty
    public int minimumVisibility = 30;
}
