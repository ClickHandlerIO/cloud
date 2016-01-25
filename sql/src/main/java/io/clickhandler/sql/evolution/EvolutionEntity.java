package io.clickhandler.sql.evolution;


import io.clickhandler.sql.entity.AbstractEntity;
import io.clickhandler.sql.annotations.Column;
import io.clickhandler.sql.annotations.Table;

import java.util.Date;

/**
 *
 */
@Table(journal = false)
public class EvolutionEntity extends AbstractEntity {
    @Column
    private boolean success;
    @Column
    private Date end;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }
}
