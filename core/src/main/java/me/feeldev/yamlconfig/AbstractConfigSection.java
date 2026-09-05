package me.feeldev.yamlconfig;

import java.util.*;

public abstract class AbstractConfigSection {

    protected Map<String, Object> data = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    protected Object getValue(String path) {
        String[] keys = path.split("\\.");
        Map<String, Object> current = data;
        for (int i = 0; i < keys.length - 1; i++) {
            Object next = current.get(keys[i]);
            if (!(next instanceof Map)) return null;
            current = (Map<String, Object>) next;
        }
        return current.get(keys[keys.length - 1]);
    }

    /** Raw value at path (Map/List/String/Number/Boolean/null), as produced by SnakeYAML. */
    public Object get(String path) {
        return getValue(path);
    }

    @SuppressWarnings("unchecked")
    public void set(String path, Object value) {
        String[] keys = path.split("\\.");
        Map<String, Object> current = data;
        for (int i = 0; i < keys.length - 1; i++) {
            current = (Map<String, Object>) current.computeIfAbsent(keys[i], k -> new LinkedHashMap<>());
        }
        current.put(keys[keys.length - 1], value);
    }

    public String getString(String path) {
        Object val = getValue(path);
        return val != null ? String.valueOf(val) : null;
    }

    public String getString(String path, String def) {
        String val = getString(path);
        return val != null ? val : def;
    }

    public int getInt(String path) {
        return getInt(path, 0);
    }

    public int getInt(String path, int def) {
        Object val = getValue(path);
        return val instanceof Number ? ((Number) val).intValue() : def;
    }

    public double getDouble(String path) {
        return getDouble(path, 0.0);
    }

    public double getDouble(String path, double def) {
        Object val = getValue(path);
        return val instanceof Number ? ((Number) val).doubleValue() : def;
    }

    public long getLong(String path) {
        return getLong(path, 0L);
    }

    public long getLong(String path, long def) {
        Object val = getValue(path);
        return val instanceof Number ? ((Number) val).longValue() : def;
    }

    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    public boolean getBoolean(String path, boolean def) {
        Object val = getValue(path);
        return val instanceof Boolean ? (Boolean) val : def;
    }

    public List<String> getStringList(String path) {
        Object val = getValue(path);
        if (!(val instanceof List)) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (Object item : (List<?>) val) result.add(String.valueOf(item));
        return result;
    }

    public List<?> getList(String path) {
        Object val = getValue(path);
        return val instanceof List<?> list ? list : new ArrayList<>();
    }

    public boolean contains(String path) {
        return getValue(path) != null;
    }

    public Set<String> getKeys(boolean deep) {
        if (!deep) return new LinkedHashSet<>(data.keySet());
        Set<String> keys = new LinkedHashSet<>();
        collectKeys(data, "", keys);
        return keys;
    }

    @SuppressWarnings("unchecked")
    public ConfigurationSection getConfigurationSection(String path) {
        Object val = getValue(path);
        if (!(val instanceof Map)) return null;
        return new ConfigurationSection((Map<String, Object>) val);
    }

    @SuppressWarnings("unchecked")
    private void collectKeys(Map<String, Object> map, String prefix, Set<String> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            result.add(key);
            if (entry.getValue() instanceof Map) {
                collectKeys((Map<String, Object>) entry.getValue(), key, result);
            }
        }
    }
}
