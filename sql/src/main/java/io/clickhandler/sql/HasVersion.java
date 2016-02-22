package io.clickhandler.sql;

/**
 *
 */
public interface HasVersion extends HasId {
    long getV();

    void setV(long version);
}
