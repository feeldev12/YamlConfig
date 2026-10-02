package me.feeldev.yamlconfig;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigurationSection extends AbstractConfigSection {

    public ConfigurationSection() {
        this.data = new LinkedHashMap<>();
    }

    public ConfigurationSection(Map<String, Object> data) {
        this.data = data != null ? data : new LinkedHashMap<>();
    }

    public static ConfigurationSection empty() {
        return new ConfigurationSection();
    }

    public static ConfigurationSection parse(String yaml) {
        if (yaml == null || yaml.isBlank()) return new ConfigurationSection();
        return load(new StringReader(yaml));
    }

    public static ConfigurationSection load(InputStream in) {
        return load(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    public static ConfigurationSection load(Reader reader) {
        Object raw = createYaml().load(reader);
        Object normalized = normalizeValue(raw);
        return new ConfigurationSection(normalized instanceof Map<?, ?> map ? (Map<String, Object>) map : new LinkedHashMap<>());
    }
}
