package dev.xframe.jdbc.builder.analyse;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class CaseIgnoredMap<V> {
    
    private Map<String, V> internal = new LinkedHashMap<>();
    
    public V put(String key, V val) {
        return internal.put(key.toLowerCase(), val);
    }
    
    public V get(String key) {
        return internal.get(key.toLowerCase());
    }
    
    public V getOrDefault(String key, V val) {
        return internal.getOrDefault(key.toLowerCase(), val);
    }

    public Set<Map.Entry<String, V>> entrySet() {
        return internal.entrySet();
    }

    public Set<String> keySet() {
        return internal.keySet();
    }

}
