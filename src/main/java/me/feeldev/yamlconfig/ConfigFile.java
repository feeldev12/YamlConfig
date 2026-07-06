package me.feeldev.yamlconfig;

import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ConfigFile extends AbstractConfigSection {

    private final String fileName;
    private final Logger logger;
    private final File file;

    public ConfigFile(Logger logger, String filename, String fileExtension, File folder) {
        this.logger = logger;
        this.fileName = filename + (filename.endsWith(fileExtension) ? "" : fileExtension);
        this.file = new File(folder, this.fileName);
        this.createFile();
    }

    public ConfigFile(Logger logger, File dataFolder, String fileName) {
        this(logger, fileName, ".yml", dataFolder);
    }

    public ConfigFile(Logger logger, File dataFolder, String fileName, String fileExtension) {
        this(logger, fileName, fileExtension, dataFolder);
    }

    private static Yaml createYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        return new Yaml(options);
    }

    private void createFile() {
        try {
            if (!fileName.endsWith(".yml")) {
                if (!file.exists()) {
                    copyFromResources();
                }
                return;
            }
            if (!file.exists()) {
                if (!copyFromResources()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
            }
            loadFromDisk();
        } catch (IOException e) {
            logger.error("Failed to create config file '{}'", fileName, e);
        }
    }

    /** Copies the file from the classpath resources to the data folder. Returns true if found. */
    private boolean copyFromResources() throws IOException {
        InputStream resource = ConfigFile.class.getClassLoader().getResourceAsStream(fileName);
        if (resource == null) return false;
        file.getParentFile().mkdirs();
        try (InputStream in = resource; OutputStream out = new FileOutputStream(file)) {
            in.transferTo(out);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private void loadFromDisk() throws IOException {
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Object raw = createYaml().load(reader);
            this.data = raw instanceof Map ? new LinkedHashMap<>((Map<String, Object>) raw) : new LinkedHashMap<>();
        }
    }

    public void saveDefault() {
        try {
            copyFromResources();
        } catch (IOException e) {
            logger.error("Failed to save default for '{}'", fileName, e);
        }
    }

    public void save() {
        try {
            file.getParentFile().mkdirs();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                createYaml().dump(data, writer);
            }
        } catch (IOException e) {
            logger.error("Save of the file '{}' failed.", fileName, e);
        }
    }

    public void reload() {
        try {
            loadFromDisk();
        } catch (IOException e) {
            logger.error("Reload of the file '{}' failed.", fileName, e);
        }
    }

    // --- Convenience helpers ---

    public List<String> getStringListWithComma(String path) {
        String list = getString(path);
        if (list == null || list.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(list.split(",")));
    }

    public Set<String> getStringSetWithComma(String path) {
        String list = getString(path);
        if (list == null || list.isEmpty()) return new HashSet<>();
        return new HashSet<>(Arrays.asList(list.split(",")));
    }

    // --- File info ---

    public boolean exists() {
        return file.exists();
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileNameWithoutExtension() {
        String name = fileName.replace(".yml", "");
        return name.substring(name.lastIndexOf("/") + 1);
    }

    public File getFile() {
        return file;
    }
}
