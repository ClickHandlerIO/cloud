package move.cluster;

import com.hazelcast.core.HazelcastInstance;

import javax.inject.Singleton;

/**
 *
 */
@Singleton
public class HazelcastProvider {
    private final HazelcastInstance value;

    public HazelcastProvider(HazelcastInstance value) {
        this.value = value;
    }

    public HazelcastInstance get() {
        return value;
    }
}
