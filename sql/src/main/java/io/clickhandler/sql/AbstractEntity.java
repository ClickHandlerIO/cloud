package io.clickhandler.sql;

import java.io.Serializable;
import java.util.Date;

/**
 *
 */
public class AbstractEntity implements HasId, HasVersion, Serializable {
    public static final String ID = "id";
    public static final String VERSION = "v";
    public static final String CHANGED = "c";

    @Column(name = ID, length = 32, nullable = false)
    protected String id;
    @Column(name = VERSION, nullable = false)
    protected long version;
    @Column(name = CHANGED, nullable = false)
    protected Date changed;

    public AbstractEntity(String id, long version, Date changed) {
        this.id = id;
        this.version = version;
        this.changed = changed;
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
    public long getVersion() {
        return version;
    }

    @Override
    public void setVersion(long version) {
        this.version = version;
    }

    public Date getChanged() {
        return changed;
    }

    public void setChanged(Date changed) {
        this.changed = changed;
    }
}
