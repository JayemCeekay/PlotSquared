/*package com.plotsquared.fabric.managers;

import com.google.inject.Singleton;
import com.plotsquared.core.util.PlatformWorldManager;
import com.plotsquared.fabric.FabricPlatform;
import me.isaiah.multiworld.MultiworldMod;
import me.isaiah.multiworld.command.CreateCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Singleton
public class MultiworldDimensionManager implements PlatformWorldManager<ServerLevel> {

    @Override
    public void initialize() {
    }

    @Override
    public @Nullable ServerLevel handleWorldCreation(final @NonNull String worldName, final @Nullable String generator) {
        ServerLevel newWorld = MultiworldMod.create_world("plotsquared:" + worldName,
                BuiltinDimensionTypes.OVERWORLD.location(),
                FabricPlatform.PLATFORM.getDefaultWorldGenerator(worldName, ""),
                Difficulty.NORMAL, 1234);
        CreateCommand.make_config(newWorld, worldName, 1234, generator);
        return newWorld;

    }

    @Override
    public String getName() {
        return "fabric";
    }

    @Override
    public Collection<String> getWorlds() {
        final List<String> worldNames = new ArrayList<>();
        for (final ServerLevel allLevel : MultiworldMod.mc.getAllLevels()) {
            worldNames.add(allLevel.dimension().location().getPath());
        }
        return worldNames;
    }

}
*/
