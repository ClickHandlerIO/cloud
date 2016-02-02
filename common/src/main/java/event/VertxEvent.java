package event;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Abstract object for standardizing creating and receiving events over the Vert.x EventBus
 *
 * @author Brad Behnke
 */
public abstract class VertxEvent<T> {
    private final static ObjectMapper jsonMapper = new ObjectMapper();
    private Class<T> valueClass;
    private T value;

    public VertxEvent(T value, Class<T> valueClass) {
        this.value = value;
        this.valueClass = valueClass;
    }

    public T getValue() {
        return value;
    }

    public Class<T> getValueClass() {
        return valueClass;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public void setValueClass(Class<T> valueClass) {
        this.valueClass = valueClass;
    }

    public String toJson() {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "Failed to get json from event value.";
        }
    }

    public T fromJson(String json) {
        try {
            return jsonMapper.readValue(json, valueClass);
        } catch (Exception e) {
            return null;
        }
    }
}
