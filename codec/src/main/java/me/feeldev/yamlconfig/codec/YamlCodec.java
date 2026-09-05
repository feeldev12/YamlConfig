package me.feeldev.yamlconfig.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JavaOps;

import me.feeldev.yamlconfig.AbstractConfigSection;

import java.util.Optional;

/**
 * Bridges Mojang {@link Codec}s to {@link AbstractConfigSection} (ConfigFile /
 * ConfigurationSection). Works because SnakeYAML already decodes into the same
 * plain Map/List/String/Number/Boolean tree that {@link JavaOps} operates on,
 * so no custom DynamicOps implementation is needed.
 */
public final class YamlCodec {

    private YamlCodec() {
    }

    public static <T> T get(AbstractConfigSection section, String path, Codec<T> codec) {
        return codec.parse(JavaOps.INSTANCE, section.get(path)).getOrThrow();
    }

    public static <T> Optional<T> getOptional(AbstractConfigSection section, String path, Codec<T> codec) {
        Object raw = section.get(path);
        if (raw == null) return Optional.empty();
        return codec.parse(JavaOps.INSTANCE, raw).result();
    }

    public static <T> void set(AbstractConfigSection section, String path, Codec<T> codec, T value) {
        section.set(path, codec.encodeStart(JavaOps.INSTANCE, value).getOrThrow());
    }
}
