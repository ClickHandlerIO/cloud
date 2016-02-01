package io.clickhandler.sql;

/**
 *
 */
public interface HasVersion extends HasId {
    long getVersion();

    void setVersion(long version);
}
