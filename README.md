# YamlConfig

Small YAML config layer for JVM projects (Minecraft mods/plugins or plain Java), extracted from Animorph's `common` module. No Minecraft/loader coupling — only `slf4j-api` and `snakeyaml`.

## Usage

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.feeldev12:YamlConfig:1.0.0'
}
```

```java
ConfigFile config = new ConfigFile(logger, dataFolder, "config");
String value = config.getString("some.path", "default");
config.set("some.path", "new-value");
config.save();
```

If a file with the same name exists on the classpath (e.g. bundled under `src/main/resources/config.yml`), `ConfigFile` copies it byte-for-byte into the data folder the first time it's missing on disk — the same behavior as Bukkit's `saveDefaultConfig()` / `saveResource()`. Comments and formatting in the bundled default are preserved, since the copy skips YAML parsing entirely.

`ConfigFiles` manages a folder of `ConfigFile`s (recursive `.yml` discovery, batch load/save/reload). Its `addGeneratedDefaultFile(...)` mirrors that same resource-copy behavior for whole default folders — resource lookup includes the folder prefix, so bundle defaults under `src/main/resources/<pathName>/<file>.yml`.
