package me.feeldev.yamlconfig;

import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigFiles {

    private final Logger logger;
    private final File dataFolder;
    private final Map<String, ConfigFile> files = new ConcurrentHashMap<>();
    private final String pathName;
    private final Set<String> generatedDefaultFiles = new HashSet<>();

    public ConfigFiles(Logger logger, File dataFolder, String pathName) {
        this.logger = logger;
        this.dataFolder = dataFolder;
        this.pathName = pathName;
    }

    public Map<String, ConfigFile> getFiles() {
        return files;
    }

    public ConfigFile getFile(String name) {
        return files.get(normalizeFileKey(name));
    }

    public void addGeneratedDefaultFile(String... fileName) {
        generatedDefaultFiles.addAll(Arrays.asList(fileName));
    }

    private boolean generateDefaultFiles() {
        File path = new File(dataFolder, pathName);
        if (path.exists()) return false;
        path.mkdirs();
        // Resource lookup must include the pathName prefix (e.g. "animations/default.yml"),
        // otherwise ConfigFile.copyFromResources() won't find defaults nested under a subfolder.
        generatedDefaultFiles.forEach(file -> files.put(file, new ConfigFile(logger, dataFolder, pathName + "/" + file)));
        return true;
    }

    public void loadFiles() {
        if (generateDefaultFiles()) return;
        File path = new File(dataFolder, pathName);
        for (File file : listFilesRecursively(path, ".yml")) {
            String relativePath = toRelativePath(path, file);
            logger.info("Loading file {}", relativePath);
            this.files.put(relativePath, new ConfigFile(logger, path, relativePath));
        }
    }

    public void loadFile(String name) {
        name = normalizeFileKey(name);
        logger.info("Loading file : {}", name);
        files.put(name, new ConfigFile(logger, new File(dataFolder, pathName), name));
    }

    public void saveFiles() {
        for (ConfigFile file : files.values()) {
            logger.info("Saving file {}", file.getFileName());
            file.save();
        }
    }

    public void saveFile(String name) {
        name = normalizeFileKey(name);
        logger.info("Saving file {}", name);
        ConfigFile configFile = files.get(name);
        if (configFile != null) {
            configFile.save();
        }
    }

    public void reloadFiles() {
        if (generateDefaultFiles()) return;
        for (ConfigFile file : files.values()) {
            if (!file.exists()) {
                deleteFile(file.getFileName());
                continue;
            }
            logger.info("Reloading file {}", file.getFileName());
            file.reload();
        }
        File path = new File(dataFolder, pathName);
        for (File file : listFilesRecursively(path, ".yml")) {
            loadFile(toRelativePath(path, file));
        }
    }

    public void reloadFile(String name) {
        name = normalizeFileKey(name);
        logger.info("Reloading file {}", name);
        ConfigFile configFile = files.get(name);
        if (configFile != null) {
            configFile.reload();
        }
    }

    public void deleteFile(String name) {
        name = normalizeFileKey(name);
        logger.info("Deleting file {}", name);
        files.remove(name);
    }

    public File[] getJsonFiles() {
        File path = new File(dataFolder, pathName);
        if (!path.exists()) return new File[0];
        List<File> listed = listFilesRecursively(path, ".json");
        return listed.toArray(new File[0]);
    }

    public Optional<ConfigFile> getFileById(String id) {
        return files.values().stream().filter(file -> file.getFileName().equals(id)).findFirst();
    }

    public String getPathName() {
        return pathName;
    }

    public File getDataFolder() {
        return dataFolder;
    }

    private String normalizeFileKey(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String normalized = name.replace('\\', '/');
        return normalized.endsWith(".yml") ? normalized : normalized + ".yml";
    }

    private String toRelativePath(File basePath, File file) {
        Path relative = basePath.toPath().relativize(file.toPath());
        return relative.toString().replace('\\', '/');
    }

    private List<File> listFilesRecursively(File root, String extension) {
        if (root == null || !root.exists()) {
            return Collections.emptyList();
        }
        List<File> result = new ArrayList<>();
        Deque<File> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            File current = stack.pop();
            File[] listed = current.listFiles();
            if (listed == null) {
                continue;
            }
            for (File file : listed) {
                if (file.isDirectory()) {
                    stack.push(file);
                    continue;
                }
                if (file.getName().endsWith(extension)) {
                    result.add(file);
                }
            }
        }
        return result;
    }
}
