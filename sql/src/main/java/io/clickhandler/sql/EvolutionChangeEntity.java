package io.clickhandler.sql;

import java.time.LocalDateTime;

/**
 *
 */
@Table
public class EvolutionChangeEntity extends AbstractEntity {
    @Column
    private ChangeType type;
    @Column
    private String sql;
    @Column
    private LocalDateTime started;
    @Column
    private LocalDateTime end;
    @Column
    private boolean success;
    @Column
    private long affected;
    @Column
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

    public LocalDateTime getStarted() {
        return started;
    }

    public void setStarted(LocalDateTime started) {
        this.started = started;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
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
