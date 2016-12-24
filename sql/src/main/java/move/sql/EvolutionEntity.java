package move.sql;


import java.time.LocalDateTime;

/**
 *
 */
@Table
public class EvolutionEntity extends AbstractEntity {
    @Column
    private boolean success;
    @Column
    private LocalDateTime started;
    @Column
    private LocalDateTime end;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
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
}
