package io.clickhandler.sql.db;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import io.clickhandler.sql.entity.AbstractEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * @author Clay Molocznik
 */
public class WriteService extends AbstractExecutionThreadService {
    private final AbstractEntity KILL_SIG = new AbstractEntity() {
    };

    private final Database db;
    private final LinkedBlockingDeque<AbstractEntity> queue = new LinkedBlockingDeque<>();

    private int batchSize = 1000;
    private int throttleMs = 0;

    public WriteService(Database db) {
        this.db = db;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getThrottleMs() {
        return throttleMs;
    }

    public void setThrottleMs(int throttleMs) {
        this.throttleMs = throttleMs;
    }

    public <T extends AbstractEntity> void add(T entity) {
        queue.add(entity);
    }

    public <T extends AbstractEntity> void addAll(List<T> entities) {
        queue.addAll(entities);
    }

    @Override
    protected void run() throws Exception {
        final ArrayList<AbstractEntity> list = Lists.newArrayList();
        final ListMultimap<Class, AbstractEntity> map = Multimaps.newListMultimap(
                Maps.newHashMap(),
                Lists::newArrayList
        );

        while (isRunning()) {
            queue.drainTo(list, batchSize - list.size());

            if (!list.isEmpty()) {
                commit(list, map);
            } else {
                final AbstractEntity entity = queue.poll(500, TimeUnit.MILLISECONDS);
                if (entity != null) {
                    list.add(entity);
                }

                if (throttleMs > 0) {
                    Thread.sleep(throttleMs);
                }
            }
        }
    }

    protected void commit(final List<AbstractEntity> entities,
                          final ListMultimap<Class, AbstractEntity> map) {
        try {
            // Sort Entities out by type.
            entities.forEach(entity -> map.put(entity.getClass(), entity));

            // Batch insert each type in it's own transaction.
            map.keySet().forEach(key -> db.execute(sql -> sql.insert(map.get(key))));
        } finally {
            entities.clear();
            map.clear();
        }
    }
}
