/*
 * PlotSquared, a land and world management plugin for Minecraft.
 * Copyright (C) IntellectualSites <https://intellectualsites.com>
 * Copyright (C) IntellectualSites team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.plotsquared.fabric.listener;

import com.google.inject.Inject;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.world.PlotAreaManager;
import com.plotsquared.core.plot.world.SinglePlotArea;
import com.plotsquared.core.util.task.PlotSquaredTask;
import com.plotsquared.core.util.task.TaskManager;
import com.plotsquared.core.util.task.TaskTime;
import com.plotsquared.fabric.FabricPlatform;
import com.plotsquared.fabric.util.FabricUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.nucleoid.stimuli.Stimuli;
import xyz.nucleoid.stimuli.event.entity.EntitySpawnEvent;

import java.util.Objects;

@SuppressWarnings("unused")
public class ChunkListener {

    private final PlotAreaManager plotAreaManager;
    private final int version;
    private LevelChunk lastChunk;
    private boolean ignoreUnload = false;

    @Inject
    public ChunkListener(final @NonNull PlotAreaManager plotAreaManager) {
        this.plotAreaManager = plotAreaManager;
        version = PlotSquared.platform().serverVersion()[1];
        if (!Settings.Chunk_Processor.AUTO_TRIM) {
            return;
        }
        for (ServerLevel world : FabricPlatform.SERVER.getAllLevels()) {
            world.noSave = true;
        }
        ServerChunkEvents.CHUNK_LOAD.register(this::onChunkLoad);
        ServerChunkEvents.CHUNK_UNLOAD.register(this::onChunkUnload);
        Stimuli.global().listen(EntitySpawnEvent.EVENT, this::onItemSpawn);
        Stimuli.global().listen(EntitySpawnEvent.EVENT, this::onEntitySpawn);
        ;
        /*if (version > 13) {
            return;
        }
        TaskManager.runTaskRepeat(() -> {
            try {
                HashSet<LevelChunk> toUnload = new HashSet<>();
                for (ServerLevel world : FabricPlatform.SERVER.getAllLevels()) {
                    String worldName = world.serverLevelData.getLevelName();
                    if (!this.plotAreaManager.hasPlotArea(worldName)) {
                        continue;
                    }
                    Object craftWorld = methodGetHandleWorld.of(world).call();
                    if (version == 13) {
                        Object chunkMap = craftWorld.getClass().getDeclaredMethod("getPlayerChunkMap").invoke(craftWorld);
                        Method methodIsChunkInUse =
                                chunkMap.getClass().getDeclaredMethod("isChunkInUse", int.class, int.class);
                        Chunk[] chunks = world.getLoadedChunks();
                        for (Chunk chunk : chunks) {
                            if ((boolean) methodIsChunkInUse.invoke(chunkMap, chunk.getX(), chunk.getZ())) {
                                continue;
                            }
                            int x = chunk.getX();
                            int z = chunk.getZ();
                            if (!shouldSave(worldName, x, z)) {
                                unloadChunk(worldName, chunk, false);
                                continue;
                            }
                            toUnload.add(chunk);
                        }
                    }
                }
                if (toUnload.isEmpty()) {
                    return;
                }
                long start = System.currentTimeMillis();
                for (Chunk chunk : toUnload) {
                    if (System.currentTimeMillis() - start > 5) {
                        return;
                    }
                    chunk.unload(true);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }, TaskTime.ticks(1L));*/
    }

    public boolean shouldSave(String world, int chunkX, int chunkZ) {
        int x = chunkX << 4;
        int z = chunkZ << 4;
        int x2 = x + 15;
        int z2 = z + 15;
        Location loc = Location.at(world, x, 1, z);
        PlotArea plotArea = plotAreaManager.getPlotArea(loc);
        if (plotArea != null) {
            Plot plot = plotArea.getPlot(loc);
            if (plot != null && plot.hasOwner()) {
                return true;
            }
        }
        loc = Location.at(world, x2, 1, z2);
        plotArea = plotAreaManager.getPlotArea(loc);
        if (plotArea != null) {
            Plot plot = plotArea.getPlot(loc);
            if (plot != null && plot.hasOwner()) {
                return true;
            }
        }
        loc = Location.at(world, x2, 1, z);
        plotArea = plotAreaManager.getPlotArea(loc);
        if (plotArea != null) {
            Plot plot = plotArea.getPlot(loc);
            if (plot != null && plot.hasOwner()) {
                return true;
            }
        }
        loc = Location.at(world, x, 1, z2);
        plotArea = plotAreaManager.getPlotArea(loc);
        if (plotArea != null) {
            Plot plot = plotArea.getPlot(loc);
            if (plot != null && plot.hasOwner()) {
                return true;
            }
        }
        loc = Location.at(world, x + 7, 1, z + 7);
        plotArea = plotAreaManager.getPlotArea(loc);
        if (plotArea == null) {
            return false;
        }
        Plot plot = plotArea.getPlot(loc);
        return plot != null && plot.hasOwner();
    }


    public void onChunkUnload(ServerLevel serverLevel, LevelChunk chunk) {
        if (ignoreUnload) {
            return;
        }
        if (Settings.Chunk_Processor.AUTO_TRIM) {
            String world = serverLevel.dimension().location().getPath();
            if ((!Settings.Enabled_Components.WORLDS || !SinglePlotArea.isSinglePlotWorld(world)) && this.plotAreaManager.hasPlotArea(
                    world)) {
                serverLevel.unload(chunk);
                /* TODO CHECK ON ! */
                if (!chunk.loaded) {
                    return;
                }
            }
        }
        if (processChunk(serverLevel, chunk, true)) {
            serverLevel.setChunkForced(chunk.getPos().x, chunk.getPos().z, true);
        }
    }

    public void onChunkLoad(ServerLevel serverLevel, LevelChunk chunk) {
        processChunk(serverLevel, chunk, false);
    }

    public InteractionResult onItemSpawn(Entity entity) {
        if (entity instanceof ItemEntity itemEntity) {
            ServerLevel serverLevel = FabricPlatform.SERVER.getLevel(entity.level().dimension());
            LevelChunk chunk = serverLevel.getChunk(entity.chunkPosition().x, entity.chunkPosition().z);
            if (chunk == this.lastChunk) {
                entity.remove(Entity.RemovalReason.DISCARDED);
                return InteractionResult.FAIL;
            }
            if (!this.plotAreaManager.hasPlotArea(serverLevel.dimension().location().getPath())) {
                return InteractionResult.PASS;
            }
            Entity[] entities = FabricUtil.getEntitiesInChunk(serverLevel, chunk).toArray(new Entity[0]);
            if (entities.length > Settings.Chunk_Processor.MAX_ENTITIES) {
                entity.remove(Entity.RemovalReason.DISCARDED);
                this.lastChunk = chunk;
                return InteractionResult.FAIL;
            } else {
                this.lastChunk = null;
            }
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onBlockPhysics() {
        if (Settings.Chunk_Processor.DISABLE_PHYSICS) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onEntitySpawn(Entity entity) {
        if (entity instanceof LivingEntity) {
            ServerLevel serverLevel = FabricPlatform.SERVER.getLevel(entity.level().dimension());
            LevelChunk chunk = serverLevel.getChunk(entity.chunkPosition().x, entity.chunkPosition().z);
            if (chunk == this.lastChunk) {
               entity.remove(Entity.RemovalReason.DISCARDED);
                return InteractionResult.FAIL;
            }
            if (!this.plotAreaManager.hasPlotArea(serverLevel.dimension().location().getPath())) {
                return InteractionResult.PASS;
            }
            Entity[] entities = FabricUtil.getEntitiesInChunk(serverLevel, chunk).toArray(new Entity[0]);
            if (entities.length > Settings.Chunk_Processor.MAX_ENTITIES) {
                entity.remove(Entity.RemovalReason.DISCARDED);
                this.lastChunk = chunk;
                return InteractionResult.FAIL;
            } else {
                this.lastChunk = null;
            }
        }
        return InteractionResult.PASS;
    }

    private void cleanChunk(ServerLevel serverLevel, final LevelChunk chunk) {
        final int currentIndex = TaskManager.index.incrementAndGet();
        PlotSquaredTask task = TaskManager.runTaskRepeat(() -> {
            if (!chunk.loaded) {
                Objects.requireNonNull(TaskManager.removeTask(currentIndex)).cancel();
                serverLevel.unload(chunk);
                return;
            }
            BlockEntity[] tiles = chunk.getBlockEntities().values().toArray(new BlockEntity[0]);
            if (tiles.length == 0) {
                Objects.requireNonNull(TaskManager.removeTask(currentIndex)).cancel();
                serverLevel.unload(chunk);
                return;
            }
            long start = System.currentTimeMillis();
            int i = 0;
            while (System.currentTimeMillis() - start < 250) {
                if (i >= tiles.length - Settings.Chunk_Processor.MAX_TILES) {
                    Objects.requireNonNull(TaskManager.removeTask(currentIndex)).cancel();
                    serverLevel.unload(chunk);
                    return;
                }
                chunk.setBlockState(tiles[i].getBlockPos(), Blocks.AIR.defaultBlockState(), false);
                //tiles[i].getBlockState().getBlock().setType(Material.AIR, false);
                i++;
            }
        }, TaskTime.ticks(5L));
        TaskManager.addTask(task, currentIndex);
    }

    public boolean processChunk(ServerLevel serverLevel, LevelChunk chunk, boolean unload) {
        if (!this.plotAreaManager.hasPlotArea(serverLevel.dimension().location().getPath())) {
            return false;
        }
        Entity[] entities = FabricUtil.getEntitiesInChunk(serverLevel, chunk).toArray(new Entity[0]);
        BlockEntity[] tiles = chunk.getBlockEntities().values().toArray(new BlockEntity[0]);
        if (entities.length > Settings.Chunk_Processor.MAX_ENTITIES) {
            int toRemove = entities.length - Settings.Chunk_Processor.MAX_ENTITIES;
            int index = 0;
            while (toRemove > 0 && index < entities.length) {
                final Entity entity = entities[index++];
                if (!(entity instanceof ServerPlayer)) {
                    entity.remove(Entity.RemovalReason.DISCARDED);
                    toRemove--;
                }
            }
        }
        if (tiles.length > Settings.Chunk_Processor.MAX_TILES) {
            if (unload) {
                cleanChunk(serverLevel, chunk);
                return true;
            }

            for (int i = 0; i < (tiles.length - Settings.Chunk_Processor.MAX_TILES); i++) {
                chunk.setBlockState(tiles[i].getBlockPos(), Blocks.AIR.defaultBlockState(), false);
            }
        }
        return false;
    }

}
