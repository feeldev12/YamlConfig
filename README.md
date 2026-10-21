# YamlConfig

Small YAML config layer for JVM projects (Minecraft mods/plugins or plain Java), extracted from Animorph's `common` module.

Java 17 bytecode (since 1.2.1), so the same jars load on Minecraft 1.20.1 and 1.21+.

Two modules:

- `core` — the config layer. No Minecraft/loader coupling, only `slf4j-api` and `snakeyaml`.
- `codec` — optional bridge to Mojang `Codec`s (DataFixerUpper), for reading/writing YAML through the same codecs you already use for NBT/JSON in a mod.

## Usage

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.feeldev12.YamlConfig:core:1.2.1'
}
```

> Multi-module JitPack coordinates changed both groupId and artifactId: previously `com.github.feeldev12:YamlConfig:1.0.0`, now `com.github.feeldev12.YamlConfig:<module>:<version>` (module is `core` or `codec`). Update existing dependants when bumping past 1.0.0.

Shipping it in a mod? Shade and relocate it instead of `jarJar`/`include`: see [Bundling in a mod](#bundling-in-a-mod-shade--relocate-not-jar-in-jar).

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
    implementation 'com.github.feeldev12.YamlConfig:codec:1.2.1'
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

## Bundling in a mod: shade + relocate, not jar-in-jar

Ship `core` (and SnakeYAML) **inside your mod's jar, relocated to your own package**, instead of
nesting them with Forge/NeoForge `jarJar` or Loom `include`.

Why: several mods use this library, and it has been published under different coordinates over
time (`me.feeldev:YamlConfig`, `com.github.feeldev12:YamlConfig`,
`com.github.feeldev12.YamlConfig:core`). Nested jars are shared by the whole game: two of those in
one Forge/NeoForge instance load the same `me.feeldev.yamlconfig` package twice (split package, the
game won't start), and SnakeYAML ranges that don't overlap (`[1.8,2.0)` vs `[2.3,3.0)`) fail
resolution. A relocated copy is private to your mod: none of that can happen.

### Recipe (Gradle, Shadow 8.3.6)

```groovy
// buildSrc/build.gradle (or root buildscript): put the task class on the classpath
dependencies { implementation 'com.gradleup.shadow:shadow-gradle-plugin:8.3.6' }
```

```groovy
// loader build.gradle
configurations {
    shade { canBeConsumed = false; canBeResolved = true; transitive = false }
}

dependencies {
    implementation "com.github.feeldev12.YamlConfig:core:1.2.1"   // compile + dev runs
    implementation "org.yaml:snakeyaml:2.3"
    shade "com.github.feeldev12.YamlConfig:core:1.2.1"            // what goes into the shipped jar
    shade "org.yaml:snakeyaml:2.3"
}

def modJar = tasks.named('jar', org.gradle.jvm.tasks.Jar)   // Fabric: 'remapJar' (see below)
modJar.configure { archiveClassifier = 'unshaded' }
def shadedJar = tasks.register('shadedJar', com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar) {
    archiveClassifier = ''
    dependsOn modJar
    manifest.inheritFrom(modJar.get().manifest)
    from({ zipTree(modJar.get().archiveFile) })
    // `project.configurations`: inside ShadowJar, `configurations` is the task's own property
    from({ project.configurations.shade.collect { zipTree(it) } }) {
        exclude 'META-INF/MANIFEST.MF', 'META-INF/maven/**', 'META-INF/versions/**', 'module-info.class'
    }
    relocate 'me.feeldev.yamlconfig', 'com.example.mymod.shaded.yamlconfig'
    relocate 'org.yaml.snakeyaml', 'com.example.mymod.shaded.snakeyaml'
    // relocation leaves the original folders behind, empty: drop them (Forge/NeoForge would
    // otherwise see the original package in your jar)
    doLast {
        def jar = archiveFile.get().asFile.toPath()
        java.nio.file.FileSystems.newFileSystem(jar, (ClassLoader) null).withCloseable { fs ->
            def dirs = java.nio.file.Files.walk(fs.getPath('/')).withCloseable { it.filter { java.nio.file.Files.isDirectory(it) }.toList() }
            dirs.sort { -it.nameCount }.each { dir ->
                if (dir.nameCount > 0 && java.nio.file.Files.list(dir).withCloseable { !it.findAny().isPresent() }) {
                    java.nio.file.Files.delete(dir)
                }
            }
        }
    }
}
tasks.named('assemble') { dependsOn shadedJar }
```

Shade **after** the loader has produced its final jar: `remapJar` on Fabric, `jar` on Forge 1.21 /
NeoForge, and on Forge 1.20.1 `jar` plus `tasks.named('shadedJar') { dependsOn 'reobfJar' }`
(`reobfJar` rewrites `jar` in place). This library has no Minecraft types, so it needs no remapping.
On Forge 1.20.1 dev runs, declare `core` and SnakeYAML as `minecraftLibrary` instead of `implementation`.

Rules of thumb:

- Ship the jar without classifier; `-unshaded` is only the intermediate step.
- The relocation prefix must be unique to your mod (`<your package>.shaded`).
- Since 1.2.1 the jars are Java 17 bytecode: the same version works on Minecraft 1.20.1 and 1.21+.
- Bundled defaults: `ConfigFile` copies `<name>.yml` from the class path on first run. Resource names
  are shared by every mod on Fabric's class loader, so prefer a path unique to your mod: copy
  `/<modid>/<name>.yml` yourself before building the `ConfigFile`.

Reference implementation: NerionLive (`buildSrc/src/main/groovy/multiloader-loader.gradle`,
`registerShadedJar`), Fabric/Forge/NeoForge on 1.21 and Fabric/Forge on 1.20.1.
