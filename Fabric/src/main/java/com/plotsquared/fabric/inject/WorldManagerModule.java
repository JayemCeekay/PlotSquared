package com.plotsquared.fabric.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.plotsquared.core.util.PlatformWorldManager;
import com.plotsquared.fabric.managers.WorldManagerDimensionManager;
import me.drex.worldmanager.WorldManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerLevel;

public class WorldManagerModule extends AbstractModule {

    @SuppressWarnings("removal") // Internal use only
    @Provides
    @Singleton
    PlatformWorldManager<ServerLevel> provideWorldManager() {
        if (FabricLoader.getInstance().isModLoaded(WorldManager.MOD_ID)) {
            return new WorldManagerDimensionManager();
        } else {
            throw new RuntimeException("WORLDMANAGER MOD NOT LOADED");
        }
    }

}
