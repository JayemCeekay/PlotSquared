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

import com.moulberry.axiom.packets.AxiomServerboundSpawnEntity;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import com.plotsquared.fabric.data.PlotSquaredDataAttachments;
import com.plotsquared.fabric.listener.event.EntityOnInsideBlockCallback;
import com.plotsquared.fabric.listener.event.EntityTeleportToCallback;
import com.plotsquared.fabric.listener.event.EntityTickEvent;
import com.plotsquared.fabric.listener.event.HandleMoveVehicleCallback;
import com.plotsquared.fabric.util.FabricEntityUtil;
import com.plotsquared.fabric.util.FabricUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.phys.AABB;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.nucleoid.stimuli.Stimuli;
import xyz.nucleoid.stimuli.event.entity.EntitySpawnEvent;

import java.util.Set;

public class EntitySpawnListener {

    private static boolean ignoreTP = false;
    private static boolean hasPlotArea = false;
    private static String areaName = null;

    public EntitySpawnListener() {
        Stimuli.global().listen(EntitySpawnEvent.EVENT, entity -> {
            if(entity.isRemoved()) {
                return InteractionResult.FAIL;
            }
            Location location = FabricUtil.adapt(GlobalPos.of(entity.level().dimension(), entity.blockPosition()));
            PlotArea area = location.getPlotArea();
            if (!location.isPlotArea() || area == null) {
                return InteractionResult.PASS;
            }
            Plot plot = location.getOwnedPlotAbs();
            EntityType<?> type = entity.getType();
            if (plot == null) {
                if (type == EntityType.ITEM) {
                    if (Settings.Enabled_Components.KILL_ROAD_ITEMS) {
                        entity.remove(Entity.RemovalReason.DISCARDED);
                        return InteractionResult.FAIL;
                    }
                    return InteractionResult.PASS;
                }
                if (!area.isMobSpawning()) {
                    if (type == EntityType.PLAYER) {
                        return InteractionResult.PASS;
                    }
                    if (entity instanceof LivingEntity) {
                        entity.remove(Entity.RemovalReason.DISCARDED);
                        return InteractionResult.FAIL;
                    }

                }
                if (!area.isMiscSpawnUnowned() && !(entity instanceof LivingEntity)) {
                    entity.remove(Entity.RemovalReason.DISCARDED);
                    return InteractionResult.FAIL;
                }
                return InteractionResult.PASS;
            }
            if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot)) {
                entity.remove(Entity.RemovalReason.DISCARDED);
                return InteractionResult.FAIL;
            }
            if (type == EntityType.END_CRYSTAL || type == EntityType.ARMOR_STAND) {
                if (FabricEntityUtil.checkEntity(entity, plot)) {
                    return InteractionResult.FAIL;
                }
                return InteractionResult.PASS;
            }
            if (type == EntityType.SHULKER) {
                if (!entity.hasAttached(PlotSquaredDataAttachments.SHULKER_PLOT)) {
                    entity.setAttached(PlotSquaredDataAttachments.SHULKER_PLOT, plot.toString());
                }
            }
            return InteractionResult.PASS;
        });

        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            for (final Entity entity : world.getEntitiesOfClass(
                    Entity.class,
                    new AABB(chunk.getPos().getMinBlockX(), chunk.getMaxBuildHeight(), chunk.getPos().getMinBlockZ(),
                            chunk.getPos().getMaxBlockX(), chunk.getMaxBuildHeight(), chunk.getPos().getMaxBlockZ()
                    ),
                    entity -> true
            )) {
                testCreate(entity);
            }
        });

        Stimuli.global().listen(EntitySpawnEvent.EVENT, entity -> {
            if (entity.getType() == EntityType.ARMOR_STAND) {
                testCreate(entity);
            }
            return InteractionResult.PASS;
        });

        Stimuli.global().listen(EntitySpawnEvent.EVENT, entity -> {
            if (entity.isVehicle()) {
                testCreate(entity);
            }
            return InteractionResult.PASS;
        });

        HandleMoveVehicleCallback.EVENT.register((serverboundMoveVehiclePacket, serverPlayer) -> {
            testNether(serverPlayer.getRootVehicle());
            return InteractionResult.PASS;
        });

        EntityOnInsideBlockCallback.EVENT.register((blockState, entity) -> {
            testNether(entity);
            return InteractionResult.PASS;
        });


        EntityTeleportToCallback.EVENT.register((serverLevel, d, e, f, set, g, h, entity) -> {
            Entity fromLocation = entity;
            BlockPos toLocation = new BlockPos((int) d, (int) e, (int) f);
            final Location fromLocLocation = FabricUtil.adapt(GlobalPos.of(entity.level().dimension(), entity.blockPosition()));
            final PlotArea fromArea = fromLocLocation.getPlotArea();
            Location toLocLocation = FabricUtil.adapt(GlobalPos.of(serverLevel.dimension(), toLocation));
            PlotArea toArea = toLocLocation.getPlotArea();
            if (toArea == null) {
                if (fromLocation.getType() == EntityType.SHULKER && fromArea != null) {
                    return InteractionResult.FAIL;
                }
                return InteractionResult.PASS;
            }
            Plot toPlot = toArea.getOwnedPlot(toLocLocation);
            if (fromLocation.getType() == EntityType.SHULKER && fromArea != null) {
                final Plot fromPlot = fromArea.getOwnedPlot(fromLocLocation);

                if (fromPlot != null || toPlot != null) {
                    if ((fromPlot == null || !fromPlot.equals(toPlot)) && (toPlot == null || !toPlot.equals(fromPlot))) {
                        return InteractionResult.FAIL;
                    }
                }
            }
            if (entity.isVehicle() || entity instanceof ArmorStand) {
                testNether(entity);
            }
            return InteractionResult.PASS;
        });

        EntityTickEvent.EVENT.register(this::onVehicle);
    }

    public static void testNether(final Entity entity) {
        @NonNull ServerLevel world = entity.getServer().getLevel(entity.level().dimension());
        if (!world.dimensionTypeRegistration().is(BuiltinDimensionTypes.NETHER) && !(world
                .dimensionTypeRegistration().is(BuiltinDimensionTypes.END))) {
            return;
        }
        test(entity);
    }

    public static void testCreate(final Entity entity) {
        @NonNull ServerLevel world = entity.getServer().getLevel(entity.level().dimension());
        if (!world.dimension().location().getPath().equals(areaName)) {
            areaName = world.dimension().location().getPath();
            hasPlotArea = PlotSquared.get().getPlotAreaManager().hasPlotArea(areaName);
        }
        if (!hasPlotArea) {
            return;
        }
        test(entity);
    }

    public static void test(Entity entity) {
        @NonNull ServerLevel world = entity.getServer().getLevel(entity.level().dimension());
        if (!entity.hasAttached(PlotSquaredDataAttachments.P2)) {
            if (PlotSquared.get().getPlotAreaManager().hasPlotArea(world.dimension().location().getPath())) {
                entity.setAttached(PlotSquaredDataAttachments.P2, GlobalPos.of(world.dimension(), entity.blockPosition()));
            }
        } else {
            GlobalPos origin = entity.getAttached(PlotSquaredDataAttachments.P2);
            ServerLevel originWorld = entity.getServer().getLevel(origin.dimension());
            if (!originWorld.equals(world)) {
                if (!ignoreTP) {
                    if (!world.dimension().location().getPath().toString().equalsIgnoreCase(originWorld + "_the_end")) {
                        if (entity.getType() == EntityType.PLAYER) {
                            return;
                        }
                        try {
                            ignoreTP = true;
                            entity.teleportTo(entity.getServer().getLevel(origin.dimension()), origin.pos().getX(),
                                    origin.pos().getY(), origin.pos().getZ(), Set.of(), 0, 0
                            );
                        } finally {
                            ignoreTP = false;
                        }
                        if (entity.getServer().getLevel(entity.level().dimension()).equals(world)) {
                            entity.remove(Entity.RemovalReason.DISCARDED);
                        }
                    }
                } else {
                    if (entity.getType() == EntityType.PLAYER) {
                        return;
                    }
                    entity.remove(Entity.RemovalReason.DISCARDED);
                }
            }
        }
    }

    public InteractionResult onVehicle(Entity entity) {
        if (entity.isVehicle()) {
            testNether(entity);
        }
        return InteractionResult.PASS;
    }

}
