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
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.permissions.Permission;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.flag.implementations.BlockBurnFlag;
import com.plotsquared.core.plot.flag.implementations.BlockIgnitionFlag;
import com.plotsquared.core.plot.flag.implementations.BreakFlag;
import com.plotsquared.core.plot.flag.implementations.ConcreteHardenFlag;
import com.plotsquared.core.plot.flag.implementations.CoralDryFlag;
import com.plotsquared.core.plot.flag.implementations.CropGrowFlag;
import com.plotsquared.core.plot.flag.implementations.DisablePhysicsFlag;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import com.plotsquared.core.plot.flag.implementations.ExplosionFlag;
import com.plotsquared.core.plot.flag.implementations.GrassGrowFlag;
import com.plotsquared.core.plot.flag.implementations.IceFormFlag;
import com.plotsquared.core.plot.flag.implementations.IceMeltFlag;
import com.plotsquared.core.plot.flag.implementations.InstabreakFlag;
import com.plotsquared.core.plot.flag.implementations.KelpGrowFlag;
import com.plotsquared.core.plot.flag.implementations.LeafDecayFlag;
import com.plotsquared.core.plot.flag.implementations.LiquidFlowFlag;
import com.plotsquared.core.plot.flag.implementations.MycelGrowFlag;
import com.plotsquared.core.plot.flag.implementations.PlaceFlag;
import com.plotsquared.core.plot.flag.implementations.SnowFormFlag;
import com.plotsquared.core.plot.flag.implementations.SnowMeltFlag;
import com.plotsquared.core.plot.flag.implementations.SoilDryFlag;
import com.plotsquared.core.plot.flag.implementations.VineGrowFlag;
import com.plotsquared.core.plot.flag.types.BlockTypeWrapper;
import com.plotsquared.core.plot.flag.types.BooleanFlag;
import com.plotsquared.core.plot.world.PlotAreaManager;
import com.plotsquared.core.util.task.TaskManager;
import com.plotsquared.core.util.task.TaskTime;
import com.plotsquared.fabric.FabricPlatform;
import com.plotsquared.fabric.listener.event.CauldronInteractionInteractCallback;
import com.plotsquared.fabric.listener.event.ConfiguredFeaturePlaceEvent;
import com.plotsquared.fabric.listener.event.FarmBlockDryEvent;
import com.plotsquared.fabric.listener.event.FarmBlockMoistureChangeEvent;
import com.plotsquared.fabric.listener.event.LevelSetBlockAndUpdateCallback;
import com.plotsquared.fabric.listener.event.LevelSetBlockEvent;
import com.plotsquared.fabric.listener.event.PistonMoveBlocksEvent;
import com.plotsquared.fabric.listener.event.SpongeAbsorbEvent;
import com.plotsquared.fabric.player.FabricPlayer;
import com.plotsquared.fabric.util.FabricUtil;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.world.block.BlockType;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.stimuli.Stimuli;
import xyz.nucleoid.stimuli.event.block.BlockPlaceEvent;
import xyz.nucleoid.stimuli.event.block.BlockPunchEvent;
import xyz.nucleoid.stimuli.event.block.BlockRandomTickEvent;
import xyz.nucleoid.stimuli.event.block.CoralDeathEvent;
import xyz.nucleoid.stimuli.event.block.DispenserActivateEvent;
import xyz.nucleoid.stimuli.event.world.ExplosionDetonatedEvent;
import xyz.nucleoid.stimuli.event.world.FireTickEvent;
import xyz.nucleoid.stimuli.event.world.FluidFlowEvent;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static net.minecraft.core.cauldron.CauldronInteraction.BANNER;
import static net.minecraft.core.cauldron.CauldronInteraction.DYED_ITEM;
import static net.minecraft.tags.BlockTags.CORALS;
import static net.minecraft.tags.BlockTags.CORAL_BLOCKS;
import static net.minecraft.tags.BlockTags.ICE;
import static net.minecraft.tags.BlockTags.SNOW;
import static net.minecraft.tags.BlockTags.WALL_CORALS;


@SuppressWarnings("unused")
public class BlockEventListener {

    private final PlotAreaManager plotAreaManager;
    private final WorldEdit worldEdit;

    @Inject
    public BlockEventListener(final @NonNull PlotAreaManager plotAreaManager, final @NonNull WorldEdit worldEdit) {
        this.plotAreaManager = plotAreaManager;
        this.worldEdit = worldEdit;
        Stimuli.global().listen(BlockPlaceEvent.BEFORE, this::blockCreate);
        PlayerBlockBreakEvents.BEFORE.register(this::blockDestroy);
        Stimuli.global().listen(BlockRandomTickEvent.EVENT, this::onBlockSpread);
        CauldronInteractionInteractCallback.EVENT.register(this::onCauldronEmpty);
        LevelSetBlockAndUpdateCallback.EVENT.register(this::onBlockForm);
        LevelSetBlockAndUpdateCallback.EVENT.register(this::onEntityBlockForm);
        Stimuli.global().listen(BlockPunchEvent.EVENT, this::onBlockDamage);
        Stimuli.global().listen(CoralDeathEvent.EVENT, this::onCoralDeath);
        Stimuli.global().listen(BlockRandomTickEvent.EVENT, this::onMelt);
        Stimuli.global().listen(FarmBlockDryEvent.EVENT, this::onSoilDry);
        Stimuli.global().listen(FarmBlockMoistureChangeEvent.EVENT, this::onMoistureChange);
        Stimuli.global().listen(BlockRandomTickEvent.EVENT, this::onGrow);
        Stimuli.global().listen(DispenserActivateEvent.EVENT, this::onBlockDispense);
        Stimuli.global().listen(PistonMoveBlocksEvent.EVENT, this::onBlockPistonExtend);
        Stimuli.global().listen(PistonMoveBlocksEvent.EVENT, this::onBlockPistonRetract);
        ConfiguredFeaturePlaceEvent.EVENT.register(this::onStructureGrow);
        Stimuli.global().listen(ExplosionDetonatedEvent.EVENT, this::onBigBoom);
        Stimuli.global().listen(FireTickEvent.EVENT, this::onBlockBurn);
        LevelSetBlockEvent.EVENT.register(this::onBlockIgnite);
        Stimuli.global().listen(BlockRandomTickEvent.EVENT, this::onLeavesDecay);
        Stimuli.global().listen(SpongeAbsorbEvent.EVENT, this::onSpongeAbsorb);
        Stimuli.global().listen(FluidFlowEvent.EVENT, this::onLiquidFlow);
    }

    public static void sendBlockChange(final GlobalPos bloc, final net.minecraft.world.level.block.state.BlockState data) {
        TaskManager.runTaskLater(() -> {
            String world = bloc.dimension().location().getPath();
            int x = bloc.pos().getX();
            int z = bloc.pos().getZ();
            int distance = FabricPlatform.SERVER.getPlayerList().getViewDistance() * 16;

            for (final PlotPlayer<?> player : PlotSquared.platform().playerManager().getPlayers()) {
                Location location = player.getLocation();
                if (location.getWorldName().equals(world)) {
                    if (16 * Math.abs(location.getX() - x) / 16 > distance || 16 * Math.abs(location.getZ() - z) / 16 > distance) {
                        continue;
                    }
                    ((FabricPlayer) player).getPlatformPlayer().serverLevel().blockUpdated(bloc.pos(), data.getBlock());
                }
            }
        }, TaskTime.ticks(3L));
    }

    public InteractionResult blockCreate(
            ServerPlayer player, ServerLevel world, BlockPos pos, BlockState state, UseOnContext context
    ) {
            Location location = FabricUtil.adapt(GlobalPos.of(world.dimension(), pos));
            PlotArea area = location.getPlotArea();
            if (area == null) {
                return InteractionResult.PASS;
            }
            FabricPlayer pp = FabricUtil.adapt(player);
            Plot plot = area.getPlot(location);
            if (plot != null) {
                if (area.notifyIfOutsideBuildArea(pp, location.getY())) {
                    pp.sendMessage(
                            TranslatableCaption.of("height.height_limit"),
                            TagResolver.builder()
                                    .tag("minheight", Tag.inserting(Component.text(area.getMinBuildHeight())))
                                    .tag("maxheight", Tag.inserting(Component.text(area.getMaxBuildHeight())))
                                    .build()
                    );
                    return InteractionResult.FAIL;
                }
                if (!plot.hasOwner()) {
                    if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_UNOWNED)) {
                        pp.sendMessage(
                                TranslatableCaption.of("permission.no_permission_event"),
                                TagResolver.resolver(
                                        "node",
                                        Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_UNOWNED)
                                )
                        );
                        return InteractionResult.FAIL;
                    }
                } else if (!plot.isAdded(pp.getUUID())) {
                    List<BlockTypeWrapper> place = plot.getFlag(PlaceFlag.class);
                    if (place != null) {
                        if (place.contains(
                                BlockTypeWrapper.get(FabricAdapter.adapt(state.getBlock())))) {
                            return InteractionResult.PASS;
                        }
                    }
                    if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                        pp.sendMessage(
                                TranslatableCaption.of("permission.no_permission_event"),
                                TagResolver.resolver(
                                        "node",
                                        Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_OTHER)
                                )
                        );
                        plot.debug(player.getName() + " could not place " + state.getBlock().getName()
                                + " because of the place = false");
                        return InteractionResult.FAIL;
                    }
                } else if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot)) {
                    if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                        pp.sendMessage(
                                TranslatableCaption.of("done.building_restricted")
                        );
                        return InteractionResult.FAIL;
                    }
                }
                if (plot.getFlag(DisablePhysicsFlag.class)) {
                    if (state.getBlock() instanceof FallingBlock) {
                        sendBlockChange(GlobalPos.of(world.dimension(), pos), state);
                        plot.debug(state.getBlock().getName()
                                + " did not fall because of disable-physics = true");
                    }
                }
            } else if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_ROAD)) {
                pp.sendMessage(
                        TranslatableCaption.of("permission.no_permission_event"),
                        TagResolver.resolver(
                                "node",
                                Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_ROAD)
                        )
                );
                return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    public InteractionResult blockCreateAxiom(
            ServerPlayer player, ServerLevel world, BlockPos pos, BlockState state,
            @Nullable UseOnContext context
    ) {
        if(state.getBlock() instanceof CommandBlock) {
            return InteractionResult.FAIL;
        }
        Location location = FabricUtil.adapt(GlobalPos.of(world.dimension(), pos));
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        }
        FabricPlayer pp = FabricUtil.adapt(player);
        Plot plot = area.getPlot(location);
        if (plot != null) {
            if (area.notifyIfOutsideBuildArea(pp, location.getY())) {
                /*pp.sendMessage(
                        TranslatableCaption.of("height.height_limit"),
                        TagResolver.builder()
                                .tag("minheight", Tag.inserting(Component.text(area.getMinBuildHeight())))
                                .tag("maxheight", Tag.inserting(Component.text(area.getMaxBuildHeight())))
                                .build()
                );*/
                return InteractionResult.FAIL;
            }
            if (!plot.hasOwner()) {
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_UNOWNED)) {
                    /*pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_UNOWNED)
                            )
                    );*/
                    return InteractionResult.FAIL;
                }
            } else if (!plot.isAdded(pp.getUUID())) {
                List<BlockTypeWrapper> place = plot.getFlag(PlaceFlag.class);
                if (place != null) {
                    if (place.contains(
                            BlockTypeWrapper.get(FabricAdapter.adapt(state.getBlock())))) {
                        return InteractionResult.PASS;
                    }
                }
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                    /*pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_OTHER)
                            )
                    );
                    plot.debug(player.getName() + " could not place " + state.getBlock().getName()
                            + " because of the place = false");*/
                    return InteractionResult.FAIL;
                }
            } else if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot)) {
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                   /* pp.sendMessage(
                            TranslatableCaption.of("done.building_restricted")
                    );*/
                    return InteractionResult.FAIL;
                }
            }
            if (plot.getFlag(DisablePhysicsFlag.class)) {
                if (state.getBlock() instanceof FallingBlock) {
                    sendBlockChange(GlobalPos.of(world.dimension(), pos), state);
                   /* plot.debug(state.getBlock().getName()
                            + " did not fall because of disable-physics = true");*/
                }
            }
        } else if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_ROAD)) {
           /* pp.sendMessage(
                    TranslatableCaption.of("permission.no_permission_event"),
                    TagResolver.resolver(
                            "node",
                            Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_ROAD)
                    )
            );*/
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }


    public boolean blockDestroy(
            Level world,
            Player playerEntity,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity
    ) {
        if (playerEntity instanceof ServerPlayer player) {
            Location location = FabricUtil.adapt(GlobalPos.of(world.dimension(), pos));
            PlotArea area = location.getPlotArea();
            if (area == null) {
                return true;
            }
            Plot plot = area.getPlot(location);
            if (plot != null) {
                FabricPlayer plotPlayer = FabricUtil.adapt(player);
                // == rather than <= as we only care about the "ground level" not being destroyed
                if (pos.getY() == area.getMinGenHeight()) {
                    if (!plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_GROUNDLEVEL)) {
                        plotPlayer.sendMessage(
                                TranslatableCaption.of("permission.no_permission_event"),
                                TagResolver.resolver(
                                        "node",
                                        Tag.inserting(Permission.PERMISSION_ADMIN_DESTROY_GROUNDLEVEL)
                                )
                        );
                        return false;
                    }
                } else if (area.notifyIfOutsideBuildArea(plotPlayer, location.getY())) {
                    plotPlayer.sendMessage(
                            TranslatableCaption.of("height.height_limit"),
                            TagResolver.builder()
                                    .tag("minheight", Tag.inserting(Component.text(area.getMinBuildHeight())))
                                    .tag("maxheight", Tag.inserting(Component.text(area.getMaxBuildHeight())))
                                    .build()
                    );
                    return false;
                }
                if (!plot.hasOwner()) {
                    if (!plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_UNOWNED, true)) {
                        return false;
                    }
                    return true;
                }
                if (!plot.isAdded(plotPlayer.getUUID())) {
                    List<BlockTypeWrapper> destroy = plot.getFlag(BreakFlag.class);
                    final BlockType blockType = FabricAdapter.adapt(state.getBlock());
                    for (final BlockTypeWrapper blockTypeWrapper : destroy) {
                        if (blockTypeWrapper.accepts(blockType)) {
                            return true;
                        }
                    }
                    if (plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_OTHER)) {
                        return true;
                    }
                    plotPlayer.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_DESTROY_OTHER)
                            )
                    );
                    return false;
                } else if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot)) {
                    if (!plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                        plotPlayer.sendMessage(
                                TranslatableCaption.of("done.building_restricted")
                        );
                        return false;
                    }
                }
                return true;
            }
            FabricPlayer pp = FabricUtil.adapt(player);
            if (pp.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_ROAD)) {
                return true;
            }
            if (this.worldEdit != null && pp.getAttribute("worldedit")) {
                if (player
                        .getUseItem()
                        .getItem() == BuiltInRegistries.ITEM.get(new ResourceLocation(this.worldEdit.getConfiguration().wandItem))) {
                    return true;
                }
            }
            pp.sendMessage(
                    TranslatableCaption.of("permission.no_permission_event"),
                    TagResolver.resolver(
                            "node",
                            Tag.inserting(Permission.PERMISSION_ADMIN_DESTROY_ROAD)
                    )
            );
            return false;
        }
        return true;
    }

    public InteractionResult onBlockSpread(ServerLevel serverLevel, BlockPos blockPos, BlockState blockState) {
        Location location = FabricUtil.adapt(GlobalPos.of(serverLevel.dimension(), blockPos));
        if (location.isPlotRoad()) {
            return InteractionResult.FAIL;
        }
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        }
        Plot plot = area.getOwnedPlot(location);
        if (plot == null) {
            return InteractionResult.PASS;
        }
        switch (blockState.getBlock().toString().substring(
                blockState.getBlock().toString().indexOf(":") + 1,
                blockState.getBlock().toString().length() - 1
        ).toUpperCase()) {
            case "GRASS_BLOCK":
                if (!plot.getFlag(GrassGrowFlag.class)) {
                    plot.debug("Grass could not grow because grass-grow = false");
                    return InteractionResult.FAIL;
                }
                break;
            case "MYCELIUM":
                if (!plot.getFlag(MycelGrowFlag.class)) {
                    plot.debug("Mycelium could not grow because mycel-grow = false");
                    return InteractionResult.FAIL;
                }
                break;
            case "WEEPING_VINES":
            case "TWISTING_VINES":
            case "CAVE_VINES":
            case "VINE":
            case "GLOW_BERRIES":
                if (!plot.getFlag(VineGrowFlag.class)) {
                    plot.debug("Vine could not grow because vine-grow = false");
                    return InteractionResult.FAIL;
                }
                break;
            case "KELP":
                if (!plot.getFlag(KelpGrowFlag.class)) {
                    plot.debug("Kelp could not grow because kelp-grow = false");
                    return InteractionResult.FAIL;
                }
            case "BUDDING_AMETHYST":
                if (!plot.getFlag(CropGrowFlag.class)) {
                    plot.debug("Amethyst clusters could not grow because crop-grow = false");
                    return InteractionResult.FAIL;
                }
                break;
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onCauldronEmpty(
            BlockState blockState,
            Level level,
            BlockPos blockPos,
            Player playerEntity,
            InteractionHand interactionHand,
            ItemStack itemStack,
            CauldronInteraction cauldronInteraction
    ) {
        if (playerEntity instanceof ServerPlayer player) {
            Location location = FabricUtil.adapt(GlobalPos.of(level.dimension(), blockPos));
            PlotArea area = location.getPlotArea();
            if (area == null) {
                return InteractionResult.PASS;
            }
            Plot plot = area.getPlot(location);
            // TODO Add flags for specific control over cauldron changes (rain, dripstone...)
            // Bucket empty, Bucket fill, Bottle empty, Bottle fill are already handled in PlayerInteract event
            // Evaporation or Unknown reasons do not need to be cancelled as they are considered natural causes
            if (cauldronInteraction.equals(BANNER) || cauldronInteraction.equals(DYED_ITEM)) {
                if (player instanceof ServerPlayer) {
                    FabricPlayer plotPlayer = FabricUtil.adapt(player);
                    if (plot != null) {
                        if (!plot.hasOwner()) {
                            if (plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_INTERACT_UNOWNED)) {
                                return InteractionResult.PASS;
                            }
                        } else if (!plot.isAdded(plotPlayer.getUUID())) {
                            if (plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_INTERACT_OTHER)) {
                                return InteractionResult.PASS;
                            }
                        } else {
                            return InteractionResult.PASS;
                        }
                    } else {
                        if (plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_INTERACT_ROAD)) {
                            return InteractionResult.PASS;
                        }
                        if (this.worldEdit != null && plotPlayer.getAttribute("worldedit")) {
                            if (player
                                    .getUseItem()
                                    .getItem() == BuiltInRegistries.ITEM.get(new ResourceLocation(this.worldEdit.getConfiguration().wandItem))) {
                                return InteractionResult.PASS;
                            }
                        }
                    }
                }
                    /*
                    if (event.getReason() == CauldronLevelChangeEvent.ChangeReason.EXTINGUISH && event.getEntity() != null) {
                        event.getEntity().setFireTicks(0);
                    }*/
                // Though the players fire ticks are modified,
                // the cauldron water level change is cancelled and the event should represent that.
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onBlockForm(
            BlockPos blockPos,
            BlockState blockState,
            LivingEntity livingEntity,
            Level level
    ) {
        Location location = FabricUtil.adapt(GlobalPos.of(level.dimension(), blockPos));
        if (location.isPlotRoad()) {
            return InteractionResult.FAIL;
        }
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        }
        Plot plot = area.getOwnedPlot(location);
        if (plot == null) {
            return InteractionResult.PASS;
        }
        if (!area.buildRangeContainsY(location.getY())) {
            return InteractionResult.FAIL;
        }
        if (blockState.is(SNOW)) {
            if (!plot.getFlag(SnowFormFlag.class)) {
                plot.debug("Snow could not form because snow-form = false");
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }
        if (blockState.is(BlockTags.ICE)) {
            if (!plot.getFlag(IceFormFlag.class)) {
                plot.debug("Ice could not form because ice-form = false");
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onEntityBlockForm(
            BlockPos blockPos,
            BlockState blockState,
            LivingEntity entity,
            Level level
    ) {
        String world = entity.level().dimension().location().getPath();
        if (!this.plotAreaManager.hasPlotArea(world)) {
            return InteractionResult.PASS;
        }
        Location location = FabricUtil.adapt(GlobalPos.of(entity.level().dimension(), blockPos));
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        }
        Plot plot = area.getOwnedPlot(location);
        if (plot == null) {
            return InteractionResult.PASS;
        }
        Class<? extends BooleanFlag<?>> flag;
        if (blockState.is(SNOW)) {
            flag = SnowFormFlag.class;
        } else if (blockState.is(ICE)) {
            flag = IceFormFlag.class;
        } else {
            return InteractionResult.PASS;
        }
        boolean allowed = plot.getFlag(flag);
        if (entity instanceof ServerPlayer player) {
            FabricPlayer plotPlayer = FabricUtil.adapt(player);
            if (!plot.isAdded(plotPlayer.getUUID())) {
                if (allowed) {
                    return InteractionResult.PASS; // player is not added but forming <flag> is allowed
                }
                plot.debug(String.format(
                        "%s could not be formed because %s = false (entity is player)",
                        blockState.getBlock(),
                        flag == SnowFormFlag.class ? "snow-form" : "ice-form"
                ));
                return InteractionResult.FAIL; // player is not added and forming <flag> isn't allowed
            }
            return InteractionResult.PASS; // event is cancelled if not added and not allowed, otherwise forming <flag> is allowed
        }
        if (plot.hasOwner()) {
            if (allowed) {
                return InteractionResult.PASS;
            }
            plot.debug(String.format(
                    "%s could not be formed because %s = false (entity is not player)",
                    blockState.getBlock(),
                    flag == SnowFormFlag.class ? "snow-form" : "ice-form"
            ));
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onBlockDamage(ServerPlayer player, Direction direction, BlockPos blockPos) {
        Location location = FabricUtil.adapt(GlobalPos.of(player.serverLevel().dimension(), blockPos));
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        }
        if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
            return InteractionResult.PASS;
        }
        Plot plot = area.getPlot(location);
        if (plot != null) {
            if (plot.getFlag(InstabreakFlag.class)) {
                BlockState blockState = player.serverLevel().getBlockState(blockPos);
                boolean breakResult = this.blockDestroy(player.serverLevel(), player, blockPos,
                        blockState, null
                );
                if (!breakResult) {
                    if (Settings.Flags.INSTABREAK_CONSIDER_TOOL) {
                        player.getUseItem().mineBlock(player.serverLevel(), blockState, blockPos, player);
                    } else {
                        player.serverLevel().destroyBlock(blockPos, true);
                    }
                }
            }
            // == rather than <= as we only care about the "ground level" not being destroyed
            if (location.getY() == area.getMinGenHeight()) {
                return InteractionResult.FAIL;
            }
            if (!plot.hasOwner()) {
                FabricPlayer plotPlayer = FabricUtil.adapt(player);
                if (plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_UNOWNED)) {
                    return InteractionResult.PASS;
                }
                return InteractionResult.FAIL;
            }
            FabricPlayer plotPlayer = FabricUtil.adapt(player);
            if (!plot.isAdded(plotPlayer.getUUID())) {
                List<BlockTypeWrapper> destroy = plot.getFlag(BreakFlag.class);
                Block block = player.serverLevel().getBlockState(blockPos).getBlock();
                if (destroy
                        .contains(BlockTypeWrapper.get(FabricAdapter.adapt(block)))
                        || plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_OTHER)) {
                    return InteractionResult.PASS;
                }
                plot.debug(player.getName() + " could not break " + block
                        + " because it was not in the break flag");
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }
        FabricPlayer plotPlayer = FabricUtil.adapt(player);
        if (plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_ROAD)) {
            return InteractionResult.PASS;
        }
        return InteractionResult.FAIL;
    }

    public InteractionResult onSoilDry(Entity entity, BlockState blockState, Level level, BlockPos blockPos) {
        Location location = FabricUtil.adapt(GlobalPos.of(level.dimension(), blockPos));
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        }
        Plot plot = area.getOwnedPlot(location);
        if (plot == null) {
            return InteractionResult.FAIL;
        }
        if (blockState.getBlock() instanceof FarmBlock) {
            if (!plot.getFlag(SoilDryFlag.class)) {
                plot.debug("Soil could not dry because soil-dry = false");
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onMelt(ServerLevel serverLevel, BlockPos blockPos, BlockState blockState) {
        Location location = FabricUtil.adapt(GlobalPos.of(serverLevel.dimension(), blockPos));
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        }
        Plot plot = area.getOwnedPlot(location);
        if (plot == null) {
            return InteractionResult.FAIL;
        }
        if (blockState.is(ICE)) {
            if (!plot.getFlag(IceMeltFlag.class)) {
                plot.debug("Ice could not melt because ice-melt = false");
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }
        if (blockState.is(SNOW)) {
            if (!plot.getFlag(SnowMeltFlag.class)) {
                plot.debug("Snow could not melt because snow-melt = false");
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }


    public InteractionResult onCoralDeath(
            ServerLevel serverLevel, BlockPos blockPos, BlockState blockState,
            BlockState blockState2
    ) {
        Location location = FabricUtil.adapt(GlobalPos.of(serverLevel.dimension(), blockPos));
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        }
        Plot plot = area.getOwnedPlot(location);
        if (plot == null) {
            return InteractionResult.FAIL;
        }
        if (blockState.is(CORAL_BLOCKS) || blockState.is(CORALS) || blockState.is(WALL_CORALS)) {
            if (!plot.getFlag(CoralDryFlag.class)) {
                plot.debug("Coral could not dry because coral-dry = false");
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onMoistureChange(
            ServerLevel serverLevel, BlockPos blockPos, BlockState blockState, int i,
            BlockState from
    ) {
        Location location = FabricUtil.adapt(GlobalPos.of(serverLevel.dimension(), blockPos));
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        }

        Plot plot = area.getOwnedPlot(location);

        if (plot == null) {
            return InteractionResult.FAIL;
        }

        if (from.getBlock() instanceof FarmBlock farmland && blockState.getBlock() instanceof FarmBlock newFarmland) {
            int currentMoisture = from.getValue(FarmBlock.MOISTURE);
            int newMoisture = blockState.getValue(FarmBlock.MOISTURE);

            // farmland gets moisturizes
            if (newMoisture > currentMoisture) {
                return InteractionResult.PASS;
            }

            if (plot.getFlag(SoilDryFlag.class)) {
                return InteractionResult.PASS;
            }

            plot.debug("Soil could not dry because soil-dry = false");
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onLiquidFlow(
            ServerLevel world, BlockPos fluidPos, BlockState fluidBlock, Direction flowDirection,
            BlockPos flowTo, BlockState flowToBlock
    ) {
        Block fromBlock = fluidBlock.getBlock();

        // Check liquid flow flag inside of origin plot too
        final Location fromLocation = FabricUtil.adapt(GlobalPos.of(world.dimension(), fluidPos));
        final PlotArea fromArea = fromLocation.getPlotArea();
        if (fromArea != null) {
            final Plot fromPlot = fromArea.getOwnedPlot(fromLocation);
            if (fromPlot != null && fromPlot.getFlag(LiquidFlowFlag.class) == LiquidFlowFlag.FlowStatus.DISABLED && fluidBlock.liquid()) {
                fromPlot.debug("Liquid could not flow because liquid-flow = disabled");
                return InteractionResult.FAIL;
            }
        }
        Block toBlock = flowToBlock.getBlock();
        Location toLocation = FabricUtil.adapt(GlobalPos.of(world.dimension(), flowTo));
        PlotArea toArea = toLocation.getPlotArea();
        if (toArea == null) {
            return InteractionResult.PASS;
        }
        if (!toArea.buildRangeContainsY(toLocation.getY())) {
            return InteractionResult.FAIL;
        }
        Plot toPlot = toArea.getOwnedPlot(toLocation);

        if (toPlot != null) {
            if (!toArea.contains(fromLocation.getX(), fromLocation.getZ()) || !Objects.equals(
                    toPlot,
                    toArea.getOwnedPlot(fromLocation)
            )) {
                return InteractionResult.FAIL;
            }
            if (toPlot.getFlag(LiquidFlowFlag.class) == LiquidFlowFlag.FlowStatus.ENABLED && fluidBlock.liquid()) {
                return InteractionResult.PASS;
            }
            if (toPlot.getFlag(DisablePhysicsFlag.class)) {
                toPlot.debug(fluidBlock.getBlock() + " could not update because disable-physics = true");
                return InteractionResult.FAIL;
            }
            if (toPlot.getFlag(LiquidFlowFlag.class) == LiquidFlowFlag.FlowStatus.DISABLED && fluidBlock.liquid()) {
                toPlot.debug("Liquid could not flow because liquid-flow = disabled");
                return InteractionResult.FAIL;
            }
        } else if (!toArea.contains(fromLocation.getX(), fromLocation.getZ()) || !Objects.equals(
                null,
                toArea.getOwnedPlot(fromLocation)
        )) {
            return InteractionResult.FAIL;
        } else if (fluidBlock.liquid()) {
            final GlobalPos location = GlobalPos.of(world.dimension(), fluidPos);

            /*
                X = block location
                A-H = potential plot locations
               Z
               ^
               |    A B C
               o    D X E
               |    F G H
               v
                <-----O-----> x
             */
            if (FabricUtil.adapt(GlobalPos.of(world.dimension(), fluidPos.offset(-1, 0, 1))  /* A */).getPlot() != null
                    || FabricUtil.adapt(GlobalPos.of(world.dimension(), fluidPos.offset(1, 0, 0))   /* B */).getPlot() != null
                    || FabricUtil.adapt(GlobalPos.of(world.dimension(), fluidPos.offset(1, 0, 1))  /* C */).getPlot() != null
                    || FabricUtil.adapt(GlobalPos.of(world.dimension(), fluidPos.offset(-1, 0, 0))  /* D */).getPlot() != null
                    || FabricUtil.adapt(GlobalPos.of(world.dimension(), fluidPos.offset(1, 0, 0)) /* E */).getPlot() != null
                    || FabricUtil.adapt(GlobalPos.of(world.dimension(), fluidPos.offset(-1, 0, -1)) /* F */).getPlot() != null
                    || FabricUtil.adapt(GlobalPos.of(world.dimension(), fluidPos.offset(0, 0, -1)) /* G */).getPlot() != null
                    || FabricUtil.adapt(GlobalPos.of(world.dimension(), fluidPos.offset(1, 0, 1))  /* H */).getPlot() != null) {
                return InteractionResult.FAIL;
            }
        }

        return InteractionResult.PASS;
    }


    public InteractionResult onGrow(ServerLevel serverLevel, BlockPos blockPos, BlockState blockState) {
        Location location = FabricUtil.adapt(GlobalPos.of(serverLevel.dimension(), blockPos));

        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        }

        if (!area.buildRangeContainsY(location.getY())) {
            return InteractionResult.FAIL;
        }

        Plot plot = location.getOwnedPlot();
        if (plot == null || !plot.getFlag(CropGrowFlag.class)) {
            if (plot != null) {
                plot.debug("Crop grow event was cancelled because crop-grow = false");
            }
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onBlockPistonExtend(
            Level level, BlockPos blockPos,
            List<BlockPos> blocksPushed, List<BlockPos> blocksDestroyed, Direction direction, BlockState blockState
    ) {
        Location location = FabricUtil.adapt(GlobalPos.of(level.dimension(), blockPos));
        Vec3i relative = new Vec3i(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            if (!this.plotAreaManager.hasPlotArea(location.getWorldName())) {
                return InteractionResult.PASS;
            }
            for (BlockPos block1 : blocksPushed) {
                Location bloc = FabricUtil.adapt(GlobalPos.of(level.dimension(), block1));
                if (bloc.isPlotArea() || bloc
                        .add(relative.getX(), relative.getY(), relative.getZ())
                        .isPlotArea()) {
                    return InteractionResult.FAIL;
                }
            }
            if (location.add(relative.getX(), relative.getY(), relative.getZ()).isPlotArea()) {
                // Prevent pistons from extending if they are: bordering a plot
                // area, facing inside plot area, and not pushing any blocks
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }
        Plot plot = area.getOwnedPlot(location);
        if (plot == null) {
            return InteractionResult.FAIL;
        }
        for (BlockPos block1 : blocksPushed) {
            Location bloc = FabricUtil.adapt(GlobalPos.of(level.dimension(), block1));
            Location newLoc = bloc.add(relative.getX(), relative.getY(), relative.getZ());
            if (!area.contains(bloc.getX(), bloc.getZ()) || !area.contains(newLoc)) {
                return InteractionResult.FAIL;
            }
            if (!plot.equals(area.getOwnedPlot(bloc)) || !plot.equals(area.getOwnedPlot(newLoc))) {
                return InteractionResult.FAIL;
            }
            if (!area.buildRangeContainsY(bloc.getY()) || !area.buildRangeContainsY(newLoc.getY())) {
                return InteractionResult.FAIL;
            }
        }
        if (!plot.equals(area.getOwnedPlot(location.add(relative.getX(), relative.getY(), relative.getZ())))) {
            // This branch is only necessary to prevent pistons from extending
            // if they are: on a plot edge, facing outside the plot, and not
            // pushing any blocks
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onBlockPistonRetract(
            Level level, BlockPos blockPos,
            List<BlockPos> blocksPushed, List<BlockPos> blocksDestroyed, Direction direction, BlockState blockState
    ) {
        Location location = FabricUtil.adapt(GlobalPos.of(level.dimension(), blockPos));
        Vec3i relative = direction.getNormal();
        PlotArea area = location.getPlotArea();
        if (area == null) {
            if (!this.plotAreaManager.hasPlotArea(location.getWorldName())) {
                return InteractionResult.PASS;
            }
            for (BlockPos block1 : blocksPushed) {
                Location bloc = FabricUtil.adapt(GlobalPos.of(level.dimension(), blockPos));
                Location newLoc = bloc.add(relative.getX(), relative.getY(), relative.getZ());
                if (bloc.isPlotArea() || newLoc.isPlotArea()) {
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        }
        Plot plot = area.getOwnedPlot(location);
        if (plot == null) {
            return InteractionResult.FAIL;
        }
        for (BlockPos block1 : blocksPushed) {
            Location bloc = FabricUtil.adapt(GlobalPos.of(level.dimension(), block1));
            Location newLoc = bloc.add(relative.getX(), relative.getY(), relative.getZ());
            if (!area.contains(bloc.getX(), bloc.getZ()) || !area.contains(newLoc)) {
                return InteractionResult.FAIL;
            }
            if (!plot.equals(area.getOwnedPlot(bloc)) || !plot.equals(area.getOwnedPlot(newLoc))) {
                return InteractionResult.FAIL;
            }
            if (!area.buildRangeContainsY(bloc.getY()) || !area.buildRangeContainsY(newLoc.getY())) {
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onBlockDispense(
            ServerLevel world, BlockPos pos, DispenserBlockEntity dispenserBlockEntity, int slot,
            ItemStack stackToDispense
    ) {
        if (!this.plotAreaManager.hasPlotArea(world.dimension().location().getPath())) {
            return InteractionResult.PASS;
        }
        Item type = stackToDispense.getItem();
        switch (BuiltInRegistries.ITEM.getKey(type).getPath().toUpperCase()) {
            case "SHULKER_BOX", "WHITE_SHULKER_BOX", "ORANGE_SHULKER_BOX", "MAGENTA_SHULKER_BOX", "LIGHT_BLUE_SHULKER_BOX",
                    "YELLOW_SHULKER_BOX", "LIME_SHULKER_BOX", "PINK_SHULKER_BOX", "GRAY_SHULKER_BOX", "LIGHT_GRAY_SHULKER_BOX",
                    "CYAN_SHULKER_BOX", "PURPLE_SHULKER_BOX", "BLUE_SHULKER_BOX", "BROWN_SHULKER_BOX", "GREEN_SHULKER_BOX",
                    "RED_SHULKER_BOX", "BLACK_SHULKER_BOX", "CARVED_PUMPKIN", "WITHER_SKELETON_SKULL", "FLINT_AND_STEEL",
                    "BONE_MEAL", "SHEARS", "GLASS_BOTTLE", "GLOWSTONE", "COD_BUCKET", "PUFFERFISH_BUCKET", "SALMON_BUCKET",
                    "TROPICAL_FISH_BUCKET", "AXOLOTL_BUCKET", "BUCKET", "WATER_BUCKET", "LAVA_BUCKET", "TADPOLE_BUCKET" -> {
                if (dispenserBlockEntity.getBlockState().getBlock() instanceof DropperBlock) {
                    return InteractionResult.PASS;
                }
                Direction targetFace = dispenserBlockEntity.getBlockState().getValue(DispenserBlock.FACING);
                Location location = FabricUtil.adapt(GlobalPos.of(world.dimension(), pos.relative(targetFace)));
                if (location.isPlotRoad()) {
                    return InteractionResult.FAIL;
                }
                PlotArea area = location.getPlotArea();
                if (area != null && !area.buildRangeContainsY(location.getY())) {
                    return InteractionResult.FAIL;
                }
            }
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onStructureGrow(
            WorldGenLevel worldGenLevel, ChunkGenerator chunkGenerator, RandomSource randomSource, BlockPos blockPos,
            ConfiguredFeature<?, ?> configuredFeature
    ) {
        Map<BlockPos, BlockState> blocks = configuredFeature.feature().plotSquared$getBlocksSet();
        Map<BlockPos, Integer> blockNumbers = configuredFeature.feature().plotSquared$getBlockNumbers();
        if (blocks.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (!this.plotAreaManager.hasPlotArea(worldGenLevel.getLevel().dimension().location().getPath())) {
            blocks.forEach((blockPos1, blockState) -> {
                worldGenLevel.setBlock(blockPos1, blockState, blockNumbers.get(blockPos1), 512);
            });
            return InteractionResult.PASS;
        }
        Location location = FabricUtil.adapt(GlobalPos.of(worldGenLevel.getLevel().dimension(), blockPos));
        PlotArea area = location.getPlotArea();
        if (area == null) {
            for (BlockPos blockPos1 : blocks.keySet()) {
                location = FabricUtil.adapt(GlobalPos.of(worldGenLevel.getLevel().dimension(), blockPos1));
                if (location.isPlotArea()) {
                    blocks.remove(blockPos1);
                }
            }
            blocks.forEach((blockPos1, blockState) -> {
                worldGenLevel.setBlock(blockPos1, blockState, blockNumbers.get(blockPos1), 512);
            });
            return InteractionResult.PASS;
        } else {
            Plot origin = area.getOwnedPlot(location);
            if (origin == null) {
                return InteractionResult.FAIL;
            }
            for (BlockPos blockPos1 : blocks.keySet()) {
                Location location1 = FabricUtil.adapt(GlobalPos.of(worldGenLevel.getLevel().dimension(), blockPos1));
                if (!area.contains(location1.getX(), location1.getZ())) {
                    blocks.remove(blockPos1);
                    continue;
                }
                Plot plot = area.getOwnedPlot(location1);
                if (!Objects.equals(plot, origin)) {
                    blocks.remove(blockPos1);
                    continue;
                }
                if (!area.buildRangeContainsY(location1.getY())) {
                    blocks.remove(blockPos1);
                }
            }
        }
        Plot origin = area.getPlot(location);
        if (origin == null) {
            return InteractionResult.FAIL;
        }
        for (BlockPos blockPos1 : blocks.keySet()) {
            Location location2 = FabricUtil.adapt(GlobalPos.of(worldGenLevel.getLevel().dimension(), blockPos1));
            Plot plot = area.getOwnedPlot(location2);
            /*
             * plot → the base plot of the merged area
             * origin → the plot where the event gets called
             */

            // Are plot and origin different AND are both plots merged
            if (!Objects.equals(plot, origin) && (!plot.isMerged() && !origin.isMerged())) {
                blocks.remove(blockPos1);
            }
            worldGenLevel.setBlock(blockPos1, blocks.get(blockPos1), 11, 512);
        }
        return InteractionResult.PASS;
    }


    public InteractionResult onBigBoom(Explosion explosion, boolean particles) {
        Location location = FabricUtil.adapt(GlobalPos.of(
                explosion.getDirectSourceEntity().level().dimension(),
                explosion.getDirectSourceEntity()
                        .getOnPos()
        ));
        String world = location.getWorldName();
        if (!this.plotAreaManager.hasPlotArea(world)) {
            return InteractionResult.PASS;
        }
        PlotArea area = location.getPlotArea();
        if (area == null) {
            Iterator<BlockPos> iterator = explosion.getToBlow().iterator();
            while (iterator.hasNext()) {
                location = FabricUtil.adapt(GlobalPos.of(
                        explosion.getDirectSourceEntity().level().dimension(),
                        iterator.next()
                ));
                if (location.isPlotArea()) {
                    iterator.remove();
                }
            }
            return InteractionResult.PASS;
        }
        Plot plot = area.getOwnedPlot(location);
        if (plot == null || !plot.getFlag(ExplosionFlag.class)) {
            if (plot != null) {
                plot.debug("Explosion was cancelled because explosion = false");
            }
            explosion.clearToBlow();
            if (Settings.General.ALWAYS_SHOW_EXPLOSIONS) {
                explosion.getDirectSourceEntity().level().addParticle(ParticleTypes.EXPLOSION, location.getX(), location.getY()
                        , location.getZ(), 0.0, 0.0, 0.0);
            }
            return InteractionResult.FAIL;
        }
        explosion.getToBlow().removeIf(blox -> !plot.equals(area.getOwnedPlot(FabricUtil.adapt(GlobalPos.of(explosion
                .getDirectSourceEntity()
                .level()
                .dimension(), blox)))));
        return InteractionResult.PASS;
    }

    public InteractionResult onBlockBurn(ServerLevel serverLevel, BlockPos blockPos) {
        Location location = FabricUtil.adapt(GlobalPos.of(serverLevel.dimension(), blockPos));

        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        }

        Plot plot = location.getOwnedPlot();
        if (plot == null || !plot.getFlag(BlockBurnFlag.class)) {
            if (plot != null) {
                plot.debug("Block burning was cancelled because block-burn = false");
            }
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onBlockIgnite(BlockPos blockPos, BlockState blockState, int i, Level level) {
        if (blockState.getBlock() == Blocks.FIRE) {
            Location location1 = FabricUtil.adapt(GlobalPos.of(level.dimension(), blockPos));
            PlotArea area = location1.getPlotArea();
            if (area == null) {
                return InteractionResult.PASS;
            }

            Plot plot = area.getOwnedPlot(location1);
            if (plot == null) {
                return InteractionResult.PASS;
            }

            Plot plotIgnited = FabricUtil.adapt(GlobalPos.of(level.dimension(), blockPos)).getPlot();
            if (!plot.getFlag(BlockIgnitionFlag.class) || plotIgnited == null || !plotIgnited
                    .equals(plot)) {
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }


    public InteractionResult onLeavesDecay(ServerLevel serverLevel, BlockPos blockPos, BlockState blockState) {
        Location location = FabricUtil.adapt(GlobalPos.of(serverLevel.dimension(), blockPos));

        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        }

        Plot plot = location.getOwnedPlot();
        if (plot == null || !plot.getFlag(LeafDecayFlag.class)) {
            if (plot != null) {
                plot.debug("Leaf decaying was cancelled because leaf-decay = false");
            }
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onSpongeAbsorb(BlockPos blockPos, Level level, BlockPos blockPos2) {
        Location location = FabricUtil.adapt(GlobalPos.of(level.dimension(), blockPos2));
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        } else {
            Plot origin = area.getOwnedPlot(FabricUtil.adapt(GlobalPos.of(level.dimension(), blockPos)));
            if (origin == null) {
                return InteractionResult.FAIL;
            } else {
                if (!area.contains(blockPos2.getX(), blockPos2.getZ())) {
                    return InteractionResult.FAIL;
                }
                Plot plot = area.getOwnedPlot(location);
                if (!Objects.equals(plot, origin)) {
                    return InteractionResult.FAIL;
                }
                if (!area.buildRangeContainsY(location.getY())) {
                    return InteractionResult.FAIL;
                }
            }
        }
        return InteractionResult.PASS;
    }

    /*
     * BlockMultiPlaceEvent is called unrelated to the BlockPlaceEvent itself and therefore doesn't respect the cancellation.
     *
    public void onBlockMultiPlace(BlockMultiPlaceEvent event) {
        // Check if the generic block place event would be cancelled
        blockCreate(event);
        if (event.isCancelled()) {
            return;
        }

        FabricPlayer pp = FabricUtil.adapt(event.getPlayer());
        Location placedLocation = FabricUtil.adapt(event.getBlockReplacedState().getLocation());
        PlotArea area = placedLocation.getPlotArea();
        if (area == null) {
            return;
        }
        Plot plot = placedLocation.getPlot();

        for (final BlockState state : event.getReplacedBlockStates()) {
            Location currentLocation = FabricUtil.adapt(state.getLocation());
            if (!pp.hasPermission(
                    Permission.PERMISSION_ADMIN_BUILD_ROAD
            ) && !(Objects.equals(currentLocation.getPlot(), plot))) {
                pp.sendMessage(
                        TranslatableCaption.of("permission.no_permission_event"),
                        TagResolver.resolver("node", Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_ROAD))
                );
                event.setCancelled(true);
                break;
            }
            if (pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_HEIGHT_LIMIT)) {
                continue;
            }
            if (currentLocation.getY() >= area.getMaxBuildHeight() || currentLocation.getY() < area.getMinBuildHeight()) {
                pp.sendMessage(
                        TranslatableCaption.of("height.height_limit"),
                        TagResolver.builder()
                                .tag("minheight", Tag.inserting(Component.text(area.getMinBuildHeight())))
                                .tag("maxheight", Tag.inserting(Component.text(area.getMaxBuildHeight())))
                                .build()
                );
                if (area.notifyIfOutsideBuildArea(pp, currentLocation.getY())) {
                    event.setCancelled(true);
                    break;
                }
            }
        }

    }*/

}
