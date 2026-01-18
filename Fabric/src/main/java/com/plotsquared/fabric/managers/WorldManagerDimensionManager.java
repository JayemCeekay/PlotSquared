package com.plotsquared.fabric.managers;

import com.plotsquared.core.util.PlatformWorldManager;
import com.plotsquared.fabric.FabricPlatform;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldData;
import me.drex.worldmanager.save.WorldManagerSavedData;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WorldManagerDimensionManager implements PlatformWorldManager<ServerLevel> {

    @Override
    public void initialize() {
    }

    @Override
    public ServerLevel handleWorldCreation(final @NonNull String worldName, final @Nullable String generator) {
        ChunkGenerator chunkGenerator = FabricPlatform.PLATFORM.getDefaultWorldGenerator(worldName, "");
        if (chunkGenerator == null) {
            throw new RuntimeException("Failed to create world " + worldName + ": generator is null");
        }
        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setDimensionType(BuiltinDimensionTypes.OVERWORLD)
                .setDifficulty(Difficulty.HARD)
                .setGameRule(GameRules.RULE_DAYLIGHT, false)
                .setGenerator(chunkGenerator)
                .setSeed(1234L);

        RuntimeWorldHandle worldHandle = Fantasy.get(FabricPlatform.SERVER).getOrOpenPersistentWorld(
                ResourceLocation.fromNamespaceAndPath(
                        "plotsquared", worldName), worldConfig
        );

        ServerLevel world = worldHandle.asWorld();

        WorldManagerSavedData.getSavedData(FabricPlatform.SERVER).addWorld(
                ResourceLocation.fromNamespaceAndPath(
                        "plotsquared", worldName),
                new WorldConfig(
                        FabricPlatform.SERVER
                                .registryAccess()
                                .registryOrThrow(Registries.DIMENSION_TYPE)
                                .getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD),
                        chunkGenerator,
                        1234,
                        true,
                        new WorldData()
                ), worldHandle
        );
        return world;
    }

    @Override
    public String getName() {
        return "fabric";
    }

    @Override
    public Collection<String> getWorlds() {
        final List<String> worldNames = new ArrayList<>();
        for (final ServerLevel allLevel : FabricPlatform.SERVER.getAllLevels()) {
            worldNames.add(allLevel.dimension().location().getPath());
        }
        return worldNames;
    }

}
