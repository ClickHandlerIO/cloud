package io.clickhandler.queue;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.ExecutorService;

/**
 *
 */
public class QueueServiceConfig<T> {
    private String name;
    private Class<T> type;
    private boolean ephemeral = true;
    private QueueHandler<T> handler;
    private ExecutorService executorService;
    private int parallelism;
    private int batchSize = 10;

    public QueueServiceConfig(String name, Class<T> type, boolean ephemeral, int parallelism, int batchSize) {
        this(name, type, ephemeral, MoreExecutors.newDirectExecutorService(), parallelism, batchSize);
    }

    public QueueServiceConfig(String name,
                              Class<T> type,
                              boolean ephemeral,
                              ExecutorService executorService,
                              int parallelism,
                              int batchSize) {
        this.name = Preconditions.checkNotNull(name, "name was null");
        this.type = Preconditions.checkNotNull(type, "type was null");
        this.ephemeral = ephemeral;
        this.executorService = Preconditions.checkNotNull(executorService, "executorService was null");
        this.parallelism = parallelism < 1 ? 1 : parallelism;
        this.batchSize = batchSize < 1 ? 1 : batchSize;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<T> getType() {
        return type;
    }

    public void setType(Class<T> type) {
        this.type = type;
    }

    public boolean isEphemeral() {
        return ephemeral;
    }

    public void setEphemeral(boolean ephemeral) {
        this.ephemeral = ephemeral;
    }

    QueueHandler<T> getHandler() {
        return handler;
    }

    void setHandler(QueueHandler<T> handler) {
        this.handler = handler;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public int getParallelism() {
        return parallelism;
    }

    public void setParallelism(int parallelism) {
        this.parallelism = parallelism;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueueServiceConfig<?> that = (QueueServiceConfig<?>) o;

        if (ephemeral != that.ephemeral) return false;
        if (parallelism != that.parallelism) return false;
        if (batchSize != that.batchSize) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (handler != null ? !handler.equals(that.handler) : that.handler != null) return false;
        return !(executorService != null ? !executorService.equals(that.executorService) : that.executorService != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (ephemeral ? 1 : 0);
        result = 31 * result + (handler != null ? handler.hashCode() : 0);
        result = 31 * result + (executorService != null ? executorService.hashCode() : 0);
        result = 31 * result + parallelism;
        result = 31 * result + batchSize;
        return result;
    }
}
