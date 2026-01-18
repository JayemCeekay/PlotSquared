package com.plotsquared.fabric.listener.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;

public interface MobSpawnEvent {


    Event<MobSpawnEvent> EVENT = EventFactory.createArrayBacked(
            MobSpawnEvent.class,
            callbacks -> (
                    serverLevelAccessor,
                    difficultyInstance,
                    mobSpawnType,
                    spawnGroupData,
                    mob
            ) -> {
                for (MobSpawnEvent callback : callbacks) {
                    InteractionResult result = callback.onMobSpawn(
                            serverLevelAccessor,
                            difficultyInstance,
                            mobSpawnType,
                            spawnGroupData,
                            mob
                    );
                    if (result != InteractionResult.PASS) {
                        return result;
                    }
                }
                return InteractionResult.PASS;
            }
    );

    InteractionResult onMobSpawn(
            ServerLevelAccessor serverLevelAccessor,
            DifficultyInstance difficultyInstance,
            MobSpawnType mobSpawnType,
            SpawnGroupData spawnGroupData,
            Mob mob
    );


}
