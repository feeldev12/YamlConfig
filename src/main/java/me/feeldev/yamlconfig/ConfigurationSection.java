package me.feeldev.yamlconfig;

import java.util.Map;

public class ConfigurationSection extends AbstractConfigSection {

    public ConfigurationSection(Map<String, Object> data) {
        this.data = data;
    }
}
