package me.nedudis.nwv.screen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ScreenRegistry extends SavedData {
    private final Map<String, ScreenData> screens = new HashMap<>();

    public static final Codec<ScreenRegistry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, ScreenData.CODEC)
                    .optionalFieldOf("screens", new HashMap<>())
                    .forGetter(r -> r.screens)
    ).apply(instance, map -> {
        ScreenRegistry registry = new ScreenRegistry();
        registry.screens.putAll(map);
        return registry;
    }));

    public static final SavedDataType<ScreenRegistry> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("nwv", "screen_registry"),
            ScreenRegistry::new,
            CODEC,
            null
    );

    public static ScreenRegistry get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public Map<String, ScreenData> getScreens() { return screens; }

    public Optional<ScreenData> getScreen(String name) {
        return Optional.ofNullable(screens.get(name));
    }

    public void addOrUpdateScreen(ScreenData newData) {
        this.screens.put(newData.name(), newData);
        this.setDirty();
    }

    public void removeScreen(String name) {
        this.screens.remove(name);
        this.setDirty();
    }
}
