package me.feeldev.yamlconfig;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.Writer;
import java.util.*;

public abstract class AbstractConfigSection {

    protected Map<String, Object> data = new LinkedHashMap<>();

    /** Normalizes SnakeYAML map keys to String recursively so numeric keys (0:) match Bukkit parity. */
    @SuppressWarnings("unchecked")
    public static Object normalizeValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> clean = new LinkedHashMap<>();
            map.forEach((k, v) -> clean.put(String.valueOf(k), normalizeValue(v)));
            return clean;
        }
        if (value instanceof List<?> list) {
            List<Object> clean = new ArrayList<>();
            for (Object item : list) clean.add(normalizeValue(item));
            return clean;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    protected Object getValue(String path) {
        if (path == null || path.isEmpty()) return data;
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
        current.put(keys[keys.length - 1], normalizeValue(value));
    }

    public String getString(String path) {
        return getString(path, null);
    }

    public String getString(String path, String def) {
        Object val = getValue(path);
        return val != null ? String.valueOf(val) : def;
    }

    public int getInt(String path) {
        return getInt(path, 0);
    }

    public int getInt(String path, int def) {
        Object val = getValue(path);
        return val instanceof Number num ? num.intValue() : def;
    }

    public double getDouble(String path) {
        return getDouble(path, 0.0);
    }

    public double getDouble(String path, double def) {
        Object val = getValue(path);
        return val instanceof Number num ? num.doubleValue() : def;
    }

    public long getLong(String path) {
        return getLong(path, 0L);
    }

    public long getLong(String path, long def) {
        Object val = getValue(path);
        return val instanceof Number num ? num.longValue() : def;
    }

    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    public boolean getBoolean(String path, boolean def) {
        Object val = getValue(path);
        return val instanceof Boolean bool ? bool : def;
    }

    public List<String> getStringList(String path) {
        Object val = getValue(path);
        if (!(val instanceof List<?> list)) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item != null && !(item instanceof Map)) result.add(String.valueOf(item));
        }
        return result;
    }

    public List<?> getList(String path) {
        Object val = getValue(path);
        return val instanceof List<?> list ? list : new ArrayList<>();
    }

    public boolean contains(String path) {
        return getValue(path) != null;
    }

    /** Returns direct root keys in file order. */
    public Set<String> getKeys() {
        return Collections.unmodifiableSet(data.keySet());
    }

    public Set<String> getKeys(boolean deep) {
        if (!deep) return getKeys();
        Set<String> keys = new LinkedHashSet<>();
        collectKeys(data, "", keys);
        return keys;
    }

    @SuppressWarnings("unchecked")
    public ConfigurationSection getSection(String path) {
        Object val = getValue(path);
        if (!(val instanceof Map<?, ?> map)) return null;
        return new ConfigurationSection((Map<String, Object>) map);
    }

    /** Compatibility alias for Bukkit users. */
    public ConfigurationSection getConfigurationSection(String path) {
        return getSection(path);
    }

    public String dump() {
        return createYaml().dump(data);
    }

    public void dump(Writer writer) {
        createYaml().dump(data, writer);
    }

    protected static Yaml createYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        return new Yaml(options);
    }

    @SuppressWarnings("unchecked")
    private void collectKeys(Map<String, Object> map, String prefix, Set<String> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            result.add(key);
            if (entry.getValue() instanceof Map<?, ?> sub) {
                collectKeys((Map<String, Object>) sub, key, result);
            }
        }
    }
}
