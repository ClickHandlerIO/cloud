package io.clickhandler.sql.annotations;

/**
 *
 */
public interface HasVersion extends HasId {
    long getVersion();

    void setVersion(long version);
}
