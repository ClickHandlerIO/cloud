package io.clickhandler.sql;


import java.util.Date;

/**
 *
 */
@Table
public class EvolutionEntity extends AbstractEntity {
    @Column
    private boolean success;
    @Column
    private Date started;
    @Column
    private Date end;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
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
}
