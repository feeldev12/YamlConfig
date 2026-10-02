package me.feeldev.yamlconfig;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationSectionTest {

    @Test
    void parsesNumericKeysAsStringKeys() {
        String yaml = """
            templates:
              0:
                type: HUD
                display-name: "Main"
              100:
                type: SCREEN
            """;
        ConfigurationSection config = ConfigurationSection.parse(yaml);

        ConfigurationSection templates = config.getSection("templates");
        assertNotNull(templates);
        assertTrue(templates.getKeys().contains("0"));
        assertTrue(templates.getKeys().contains("100"));

        assertEquals("HUD", config.getString("templates.0.type"));
        assertEquals("Main", config.getString("templates.0.display-name"));
        assertEquals("SCREEN", config.getString("templates.100.type"));
    }

    @Test
    void supportsRelativeSectionNavigation() {
        String yaml = """
            server:
              host: 127.0.0.1
              port: 25565
              nested:
                enabled: true
            """;
        ConfigurationSection config = ConfigurationSection.parse(yaml);
        ConfigurationSection server = config.getSection("server");

        assertNotNull(server);
        assertEquals("127.0.0.1", server.getString("host"));
        assertEquals(25565, server.getInt("port"));

        ConfigurationSection nested = server.getSection("nested");
        assertNotNull(nested);
        assertTrue(nested.getBoolean("enabled"));
    }

    @Test
    void supportsStringListsAndTypes() {
        String yaml = """
            items:
              - apple
              - banana
              - orange
            count: 42
            price: 19.99
            active: true
            """;
        ConfigurationSection config = ConfigurationSection.parse(yaml);

        List<String> items = config.getStringList("items");
        assertEquals(3, items.size());
        assertEquals("apple", items.get(0));

        assertEquals(42, config.getInt("count"));
        assertEquals(19.99, config.getDouble("price"));
        assertTrue(config.getBoolean("active"));
    }

    @Test
    void supportsSetAndDump() {
        ConfigurationSection config = ConfigurationSection.empty();
        config.set("database.host", "localhost");
        config.set("database.port", 3306);

        assertEquals("localhost", config.getString("database.host"));
        assertEquals(3306, config.getInt("database.port"));

        String dumped = config.dump();
        assertNotNull(dumped);

        ConfigurationSection reloaded = ConfigurationSection.parse(dumped);
        assertEquals("localhost", reloaded.getString("database.host"));
        assertEquals(3306, reloaded.getInt("database.port"));
    }
}
