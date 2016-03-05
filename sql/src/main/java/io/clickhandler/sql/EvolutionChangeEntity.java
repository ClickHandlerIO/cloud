package io.clickhandler.sql;

import java.util.Date;

/**
 *
 */
@Table(journal = false)
public class EvolutionChangeEntity extends AbstractEntity {
    @Column
    private ChangeType type;
    @Column(dbType = DBTypes.CLOB)
    private String sql;
    @Column
    private Date started;
    @Column
    private Date end;
    @Column
    private boolean success;
    @Column
    private long affected;
    @Column(dbType = DBTypes.CLOB)
    private String message;

    public ChangeType getType() {
        return type;
    }

    public void setType(ChangeType type) {
        this.type = type;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public Date getStarted() {
        return started;
    }

    public void setStarted(Date started) {
        this.started = started;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getAffected() {
        return affected;
    }

    public void setAffected(long affected) {
        this.affected = affected;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
