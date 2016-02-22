package io.clickhandler.sql;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Date;

/**
 *
 */
public abstract class AbstractEntity implements HasId, HasVersion {
    public static final String ID = "id";
    public static final String VERSION = "v";
    public static final String CHANGED = "c";

    @JsonProperty(ID)
    @Column(name = ID, length = 32, nullable = false)
    protected String id;
    @JsonProperty(VERSION)
    @Column(name = VERSION, nullable = false)
    protected long v;
    @JsonProperty(CHANGED)
    @Column(name = CHANGED, nullable = false)
    protected Date c;

    public AbstractEntity(String id, long version, Date changed) {
        this.id = id;
        this.v = version;
        this.c = changed;
    }

    public AbstractEntity() {
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    @JsonProperty(VERSION)
    public long getV() {
        return v;
    }

    @Override
    public void setV(long version) {
        this.v = version;
    }

    @JsonProperty(CHANGED)
    public Date getC() {
        return c;
    }

    public void setC(Date changed) {
        this.c = changed;
    }
}
