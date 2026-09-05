# YamlConfig

Small YAML config layer for JVM projects (Minecraft mods/plugins or plain Java), extracted from Animorph's `common` module.

Two modules:

- `core` — the config layer. No Minecraft/loader coupling, only `slf4j-api` and `snakeyaml`.
- `codec` — optional bridge to Mojang `Codec`s (DataFixerUpper), for reading/writing YAML through the same codecs you already use for NBT/JSON in a mod.

## Usage

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.feeldev12.YamlConfig:core:1.1.0'
}
```

> Multi-module JitPack coordinates changed both groupId and artifactId: previously `com.github.feeldev12:YamlConfig:1.0.0`, now `com.github.feeldev12.YamlConfig:<module>:<version>` (module is `core` or `codec`). Update existing dependants when bumping past 1.0.0.

```java
ConfigFile config = new ConfigFile(logger, dataFolder, "config");
String value = config.getString("some.path", "default");
config.set("some.path", "new-value");
config.save();
```

If a file with the same name exists on the classpath (e.g. bundled under `src/main/resources/config.yml`), `ConfigFile` copies it byte-for-byte into the data folder the first time it's missing on disk — the same behavior as Bukkit's `saveDefaultConfig()` / `saveResource()`. Comments and formatting in the bundled default are preserved, since the copy skips YAML parsing entirely.

`ConfigFiles` manages a folder of `ConfigFile`s (recursive `.yml` discovery, batch load/save/reload). Its `addGeneratedDefaultFile(...)` mirrors that same resource-copy behavior for whole default folders — resource lookup includes the folder prefix, so bundle defaults under `src/main/resources/<pathName>/<file>.yml`.

## Codecs (`codec`)

```groovy
dependencies {
    implementation 'com.github.feeldev12.YamlConfig:codec:1.1.0'
    // com.mojang:datafixerupper is compileOnlyApi — provided by the Minecraft
    // runtime (Forge/Fabric/NeoForge all bundle it). Nothing extra to add in a mod.
}
```

```java
Codec<MyData> codec = ...;

MyData data = YamlCodec.get(config, "some.path", codec);
Optional<MyData> maybe = YamlCodec.getOptional(config, "some.path", codec);
YamlCodec.set(config, "some.path", codec, data);
config.save();
```

Works because SnakeYAML already loads YAML into the same plain `Map`/`List`/`String`/`Number`/`Boolean` tree that DFU's `JavaOps` operates on — no custom `DynamicOps` implementation needed. Two things to keep in mind:

- Map codec keys (`Codec.unboundedMap(...)`) must be `String` — YAML mapping keys always are.
- Re-encoding through a codec rewrites that subtree from scratch, so hand-written comments in that section of the YAML file are lost on `save()` (same limitation the core module already has when dumping any section).
