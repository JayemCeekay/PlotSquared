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

import com.google.common.base.Charsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.configuration.caption.Caption;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.listener.PlayerBlockEventType;
import com.plotsquared.core.listener.PlotListener;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.permissions.Permission;
import com.plotsquared.core.player.ConsolePlayer;
import com.plotsquared.core.player.MetaDataAccess;
import com.plotsquared.core.player.PlayerMetaDataKeys;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.PlotId;
import com.plotsquared.core.plot.PlotInventory;
import com.plotsquared.core.plot.flag.FlagContainer;
import com.plotsquared.core.plot.flag.implementations.AnimalInteractFlag;
import com.plotsquared.core.plot.flag.implementations.BlockedCmdsFlag;
import com.plotsquared.core.plot.flag.implementations.ChatFlag;
import com.plotsquared.core.plot.flag.implementations.DenyPortalTravelFlag;
import com.plotsquared.core.plot.flag.implementations.DenyPortalsFlag;
import com.plotsquared.core.plot.flag.implementations.DenyTeleportFlag;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import com.plotsquared.core.plot.flag.implementations.DropProtectionFlag;
import com.plotsquared.core.plot.flag.implementations.EditSignFlag;
import com.plotsquared.core.plot.flag.implementations.HangingBreakFlag;
import com.plotsquared.core.plot.flag.implementations.HangingPlaceFlag;
import com.plotsquared.core.plot.flag.implementations.HostileInteractFlag;
import com.plotsquared.core.plot.flag.implementations.ItemDropFlag;
import com.plotsquared.core.plot.flag.implementations.KeepInventoryFlag;
import com.plotsquared.core.plot.flag.implementations.LecternReadBookFlag;
import com.plotsquared.core.plot.flag.implementations.MiscInteractFlag;
import com.plotsquared.core.plot.flag.implementations.PlayerInteractFlag;
import com.plotsquared.core.plot.flag.implementations.PreventCreativeCopyFlag;
import com.plotsquared.core.plot.flag.implementations.TamedInteractFlag;
import com.plotsquared.core.plot.flag.implementations.TileDropFlag;
import com.plotsquared.core.plot.flag.implementations.UntrustedVisitFlag;
import com.plotsquared.core.plot.flag.implementations.VehicleBreakFlag;
import com.plotsquared.core.plot.flag.implementations.VehicleUseFlag;
import com.plotsquared.core.plot.flag.implementations.VillagerInteractFlag;
import com.plotsquared.core.plot.world.PlotAreaManager;
import com.plotsquared.core.util.EventDispatcher;
import com.plotsquared.core.util.MathMan;
import com.plotsquared.core.util.PlotFlagUtil;
import com.plotsquared.core.util.entity.EntityCategories;
import com.plotsquared.core.util.task.TaskManager;
import com.plotsquared.core.util.task.TaskTime;
import com.plotsquared.fabric.FabricPlatform;
import com.plotsquared.fabric.listener.event.BaseFireBlockOnPlaceCallback;
import com.plotsquared.fabric.listener.event.EmptyContentsCallback;
import com.plotsquared.fabric.listener.event.EntityHandleInsidePortalCallback;
import com.plotsquared.fabric.listener.event.HandleContainerCloseCallback;
import com.plotsquared.fabric.listener.event.HandleMoveVehicleCallback;
import com.plotsquared.fabric.listener.event.HandlePlayerMoveCallback;
import com.plotsquared.fabric.listener.event.LecternTakeButtonCallback;
import com.plotsquared.fabric.listener.event.ServerPlayerTeleportToCallback;
import com.plotsquared.fabric.player.FabricPlayer;
import com.plotsquared.fabric.util.FabricEntityUtil;
import com.plotsquared.fabric.util.FabricUtil;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.entity.EntityTypes;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.tag.convention.v1.TagUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.FireworkStarItem;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.stimuli.Stimuli;
import xyz.nucleoid.stimuli.event.block.BlockDropItemsEvent;
import xyz.nucleoid.stimuli.event.block.BlockTrampleEvent;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.entity.EntityDeathEvent;
import xyz.nucleoid.stimuli.event.entity.EntitySpawnEvent;
import xyz.nucleoid.stimuli.event.entity.EntityUseEvent;
import xyz.nucleoid.stimuli.event.item.ItemPickupEvent;
import xyz.nucleoid.stimuli.event.item.ItemThrowEvent;
import xyz.nucleoid.stimuli.event.player.PlayerChatEvent;
import xyz.nucleoid.stimuli.event.player.PlayerCommandEvent;
import xyz.nucleoid.stimuli.event.player.PlayerInventoryActionEvent;
import xyz.nucleoid.stimuli.event.world.EndPortalOpenEvent;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

import static com.plotsquared.fabric.data.PlotSquaredDataAttachments.PLOT_DATA;
import static net.minecraft.world.item.Items.BOOK;
import static net.minecraft.world.item.Items.CHEST_MINECART;
import static net.minecraft.world.item.Items.COMMAND_BLOCK_MINECART;
import static net.minecraft.world.item.Items.FURNACE_MINECART;
import static net.minecraft.world.item.Items.HOPPER_MINECART;
import static net.minecraft.world.item.Items.KNOWLEDGE_BOOK;
import static net.minecraft.world.item.Items.MINECART;
import static net.minecraft.world.item.Items.TNT_MINECART;
import static net.minecraft.world.item.Items.WRITABLE_BOOK;
import static net.minecraft.world.item.Items.WRITTEN_BOOK;

/**
 * Player Events involving plots.
 */
@SuppressWarnings("unused")
public class PlayerEventListener {

    private static final Set<Item> MINECARTS = Set.of(
            MINECART,
            TNT_MINECART,
            CHEST_MINECART,
            COMMAND_BLOCK_MINECART,
            FURNACE_MINECART,
            HOPPER_MINECART
    );
    private static final Set<Item> BOOKS = Set.of(
            BOOK,
            KNOWLEDGE_BOOK,
            WRITABLE_BOOK,
            WRITTEN_BOOK
    );
    private static final Set<String> DYES;

    static {
        Set<String> mutableDyes = new HashSet<>(Set.of(
                "WHITE_DYE",
                "LIGHT_GRAY_DYE",
                "GRAY_DYE",
                "BLACK_DYE",
                "BROWN_DYE",
                "RED_DYE",
                "ORANGE_DYE",
                "YELLOW_DYE",
                "LIME_DYE",
                "GREEN_DYE",
                "CYAN_DYE",
                "LIGHT_BLUE_DYE",
                "BLUE_DYE",
                "PURPLE_DYE",
                "MAGENTA_DYE",
                "PINK_DYE",
                "GLOW_INK_SAC"
        ));
        int[] version = PlotSquared.platform().serverVersion();
        if (version[1] >= 20 && version[2] >= 1) {
            mutableDyes.add("HONEYCOMB");
        }
        DYES = Set.copyOf(mutableDyes);
    }

    private final EventDispatcher eventDispatcher;
    private final WorldEdit worldEdit;
    private final PlotAreaManager plotAreaManager;
    private final PlotListener plotListener;
    // To prevent recursion
    private boolean tmpTeleport = true;
    private String internalVersion;

    @Inject
    public PlayerEventListener(
            final @NonNull PlotAreaManager plotAreaManager,
            final @NonNull EventDispatcher eventDispatcher,
            final @NonNull WorldEdit worldEdit,
            final @NonNull PlotListener plotListener
    ) {
        this.eventDispatcher = eventDispatcher;
        this.worldEdit = worldEdit;
        this.plotAreaManager = plotAreaManager;
        this.plotListener = plotListener;

        Stimuli.global().listen(ItemThrowEvent.EVENT, this::onItemDrop);
        Stimuli.global().listen(ItemPickupEvent.EVENT, this::onItemPickup);
        Stimuli.global().listen(BlockTrampleEvent.EVENT, this::onTrample);
        EmptyContentsCallback.EVENT.register(this::onBucketEmpty);
        UseBlockCallback.EVENT.register(this::onBucketFill);
        HandleContainerCloseCallback.EVENT.register(this::onInventoryClose);
        ServerPlayerEvents.AFTER_RESPAWN.register(this::onDeath);
        PlayerBlockBreakEvents.AFTER.register(this::afterBlockBreak);
        UseBlockCallback.EVENT.register(PlayerEventListener::interact);
        Stimuli.global().listen(PlayerCommandEvent.EVENT, PlayerEventListener::onPlayerCommand);

        ServerPlayConnectionEvents.INIT.register(PlayerEventListener::onLoginInit);

        Stimuli.global().listen(BlockUseEvent.EVENT, this::onHangingPlace);

        Stimuli.global().listen(EntitySpawnEvent.EVENT, entity -> {
            Location location = FabricUtil.adapt(GlobalPos.of(entity.level().dimension(), entity.blockPosition()));
            PlotArea area = location.getPlotArea();
            if (area == null) {
                return InteractionResult.PASS;
            }
            EntitySpawnListener.testNether(entity);
            Plot plot = location.getPlotAbs();
            if (FabricEntityUtil.checkEntity(entity, plot)) {
                entity.remove(Entity.RemovalReason.DISCARDED);
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        Stimuli.global().listen(EntityDeathEvent.EVENT, this::onHangingBreakByEntity);
        Stimuli.global().listen(EntityUseEvent.EVENT, this::onPlayerInteractEntity);
        Stimuli.global().listen(EntityDeathEvent.EVENT, this::onVehicleDestroy);
        Stimuli.global().listen(EndPortalOpenEvent.EVENT, this::onEndPortalCreation);
        BaseFireBlockOnPlaceCallback.EVENT.register(this::onNetherPortalCreation);
        EntityHandleInsidePortalCallback.EVENT.register(this::onPortalEnter);
        LecternTakeButtonCallback.EVENT.register(this::onPlayerTakeLecternBook);
        HandlePlayerMoveCallback.EVENT.register((serverboundMovePlayerPacket, serverPlayer) -> {
            if (!serverboundMovePlayerPacket.hasPosition()) {
                return InteractionResult.PASS;
            }

            BlockPos fromBlockPos = new BlockPos(
                    serverPlayer.getBlockX(),
                    serverPlayer.getBlockY(),
                    serverPlayer.getBlockZ()
            );
            BlockPos toBlockPos = new BlockPos(
                    (int) serverboundMovePlayerPacket.getX(0.0),
                    (int) serverboundMovePlayerPacket.getY(0.0),
                    (int) serverboundMovePlayerPacket.getZ(0.0)
            );
            GlobalPos from = GlobalPos.of(serverPlayer.serverLevel().dimension(), fromBlockPos);
            GlobalPos to = GlobalPos.of(serverPlayer.serverLevel().dimension(), toBlockPos);
            int x2;
            if (MathMan.roundInt(from.pos().getX()) != (x2 = MathMan.roundInt(to.pos().getX()))) {
                ServerPlayer player = serverPlayer;
                FabricPlayer pp;
                try {
                    pp = FabricUtil.adapt(player);
                } catch (Exception e) {
                    return InteractionResult.PASS;
                }
                // Cancel teleport
                if (TaskManager.removeFromTeleportQueue(pp.getName())) {
                    pp.sendMessage(TranslatableCaption.of("teleport.teleport_failed"));
                }
                // Set last location
                Location location = FabricUtil.adapt(to);
                try (final MetaDataAccess<Location> lastLocationAccess =
                             pp.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_LOCATION)) {
                    lastLocationAccess.remove();
                }
                PlotArea area = location.getPlotArea();
                if (area == null) {
                    try (final MetaDataAccess<Plot> lastPlotAccess =
                                 pp.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_LAST_PLOT)) {
                        lastPlotAccess.remove();
                    }
                    return InteractionResult.PASS;
                }
                Plot now = area.getPlot(location);
                Plot lastPlot;
                try (final MetaDataAccess<Plot> lastPlotAccess =
                             pp.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_LAST_PLOT)) {
                    lastPlot = lastPlotAccess.get().orElse(null);
                }
                if (now == null) {
                    try (final MetaDataAccess<Boolean> kickAccess =
                                 pp.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_KICK)) {
                        if (lastPlot != null && !plotListener.plotExit(pp, lastPlot) && this.tmpTeleport && !kickAccess
                                .get()
                                .orElse(
                                        false)) {
                            pp.sendMessage(
                                    TranslatableCaption.of("permission.no_permission_event"),
                                    TagResolver.resolver(
                                            "node",
                                            Tag.inserting(Permission.PERMISSION_ADMIN_EXIT_DENIED)
                                    )
                            );
                            this.tmpTeleport = false;
                            if (lastPlot.equals(FabricUtil.adapt(from).getPlot())) {
                                player.teleportTo(from.pos().getX(), from.pos().getY(), from.pos().getZ());
                            } else {
                                player.teleportTo(
                                        player.serverLevel().getSharedSpawnPos().getX(),
                                        player.serverLevel().getSharedSpawnPos().getY(),
                                        player.serverLevel().getSharedSpawnPos().getZ()
                                );
                            }
                            this.tmpTeleport = true;
                            return InteractionResult.FAIL;
                        }
                    }
                } else if (now.equals(lastPlot)) {
                    ForceFieldListener.handleForcefield(player, pp, now);
                } else if (!plotListener.plotEntry(pp, now) && this.tmpTeleport) {
                    pp.sendMessage(
                            TranslatableCaption.of("deny.no_enter"),
                            TagResolver.resolver("plot", Tag.inserting(Component.text(now.toString())))
                    );
                    this.tmpTeleport = false;
                    to = GlobalPos.of(to.dimension(), from.pos());
                    player.teleportTo(to.pos().getX(), to.pos().getY(), to.pos().getZ());
                    this.tmpTeleport = true;
                    return InteractionResult.PASS;
                }
                int border = area.getBorder(true);
                int x1;
                if (x2 > border && this.tmpTeleport) {
                    if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BYPASS_BORDER)) {

                        to = GlobalPos.of(to.dimension(), new BlockPos(border - 1, to.pos().getY(), to.pos().getZ()));
                        this.tmpTeleport = false;
                        player.teleportTo(to.pos().getX(), to.pos().getY(), to.pos().getZ());
                        this.tmpTeleport = true;
                        pp.sendMessage(TranslatableCaption.of("border.denied"));
                    } else if (MathMan.roundInt(from.pos().getX()) <= border) { // Only send if they just moved out of the border
                        pp.sendMessage(TranslatableCaption.of("border.bypass.exited"));
                    }
                } else if (x2 < -border && this.tmpTeleport) {
                    if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BYPASS_BORDER)) {
                        to = GlobalPos.of(to.dimension(), new BlockPos(-border + 1, to.pos().getY(), to.pos().getZ()));
                        this.tmpTeleport = false;
                        player.teleportTo(to.pos().getX(), to.pos().getY(), to.pos().getZ());
                        this.tmpTeleport = true;
                        pp.sendMessage(TranslatableCaption.of("border.denied"));
                    } else if (MathMan.roundInt(from.pos().getX()) >= -border) { // Only send if they just moved out of the border
                        pp.sendMessage(TranslatableCaption.of("border.bypass.exited"));
                    }
                } else if (((x1 = MathMan.roundInt(from
                        .pos()
                        .getX())) >= border && x2 <= border) || (x1 <= -border && x2 >= -border)) {
                    if (pp.hasPermission(Permission.PERMISSION_ADMIN_BYPASS_BORDER)) {
                        pp.sendMessage(TranslatableCaption.of("border.bypass.entered"));
                    }
                }
            }
            int z2;
            if (MathMan.roundInt(from.pos().getZ()) != (z2 = MathMan.roundInt(to.pos().getZ()))) {
                ServerPlayer player = serverPlayer;
                FabricPlayer pp = FabricUtil.adapt(player);
                // Cancel teleport
                if (TaskManager.removeFromTeleportQueue(pp.getName())) {
                    pp.sendMessage(TranslatableCaption.of("teleport.teleport_failed"));
                }
                // Set last location
                Location location = FabricUtil.adapt(to);
                try (final MetaDataAccess<Location> lastLocationAccess =
                             pp.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_LOCATION)) {
                    lastLocationAccess.set(location);
                }
                PlotArea area = location.getPlotArea();
                if (area == null) {
                    try (final MetaDataAccess<Plot> lastPlotAccess =
                                 pp.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_LAST_PLOT)) {
                        lastPlotAccess.remove();
                    }
                    return InteractionResult.PASS;
                }
                Plot plot = area.getPlot(location);
                Plot lastPlot;
                try (final MetaDataAccess<Plot> lastPlotAccess =
                             pp.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_LAST_PLOT)) {
                    lastPlot = lastPlotAccess.get().orElse(null);
                }
                if (plot == null) {
                    try (final MetaDataAccess<Boolean> kickAccess =
                                 pp.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_KICK)) {
                        if (lastPlot != null && !plotListener.plotExit(pp, lastPlot) && this.tmpTeleport && !kickAccess
                                .get()
                                .orElse(
                                        false)) {
                            pp.sendMessage(
                                    TranslatableCaption.of("permission.no_permission_event"),
                                    TagResolver.resolver(
                                            "node",
                                            Tag.inserting(Permission.PERMISSION_ADMIN_EXIT_DENIED)
                                    )
                            );
                            this.tmpTeleport = false;
                            if (lastPlot.equals(FabricUtil.adapt(from).getPlot())) {
                                player.teleportTo(from.pos().getX(), from.pos().getY(), from.pos().getZ());
                            } else {
                                player.teleportTo(
                                        player.serverLevel().getSharedSpawnPos().getX(),
                                        player.serverLevel().getSharedSpawnPos().getY(),
                                        player.serverLevel().getSharedSpawnPos().getZ()
                                );
                            }
                            this.tmpTeleport = true;
                            return InteractionResult.FAIL;
                        }
                    }
                } else if (plot.equals(lastPlot)) {
                    ForceFieldListener.handleForcefield(player, pp, plot);
                } else if (!plotListener.plotEntry(pp, plot) && this.tmpTeleport) {
                    pp.sendMessage(
                            TranslatableCaption.of("deny.no_enter"),
                            TagResolver.resolver("plot", Tag.inserting(Component.text(plot.toString())))
                    );
                    this.tmpTeleport = false;
                    player.teleportTo(from.pos().getX(), from.pos().getY(), from.pos().getZ());
                    this.tmpTeleport = true;
                    return InteractionResult.PASS;
                }
                int border = area.getBorder(true);
                int z1;
                if (z2 > border && this.tmpTeleport) {
                    if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BYPASS_BORDER)) {
                        to = GlobalPos.of(to.dimension(), new BlockPos(to.pos().getX(), to.pos().getY(), border - 1));
                        this.tmpTeleport = false;
                        player.teleportTo(to.pos().getX(), to.pos().getY(), to.pos().getZ());
                        this.tmpTeleport = true;
                        pp.sendMessage(TranslatableCaption.of("border.denied"));
                    } else if (MathMan.roundInt(from.pos().getZ()) <= border) { // Only send if they just moved out of the border
                        pp.sendMessage(TranslatableCaption.of("border.bypass.exited"));
                    }
                } else if (z2 < -border && this.tmpTeleport) {
                    if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BYPASS_BORDER)) {
                        to = GlobalPos.of(to.dimension(), new BlockPos(to.pos().getX(), to.pos().getY(), -border + 1));
                        this.tmpTeleport = false;
                        player.teleportTo(to.pos().getX(), to.pos().getY(), to.pos().getZ());
                        this.tmpTeleport = true;
                        pp.sendMessage(TranslatableCaption.of("border.denied"));
                    } else if (MathMan.roundInt(from.pos().getZ()) >= -border) { // Only send if they just moved out of the border
                        pp.sendMessage(TranslatableCaption.of("border.bypass.exited"));
                    }
                } else if (((z1 = MathMan.roundInt(from
                        .pos()
                        .getZ())) >= border && z2 <= border) || (z1 <= -border && z2 >= -border)) {
                    if (pp.hasPermission(Permission.PERMISSION_ADMIN_BYPASS_BORDER)) {
                        pp.sendMessage(TranslatableCaption.of("border.bypass.entered"));
                    }
                }
            }
            return InteractionResult.PASS;
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            TaskManager.runTaskLater(() -> {
                final ServerPlayer player = handler.player;
                PlotSquared.platform().playerManager().removePlayer(player.getUUID());
                final PlotPlayer<ServerPlayer> pp = FabricUtil.adapt(player);

                // we're stripping the country code as we don't want to differ between countries
                //pp.setLocale(Locale.forLanguageTag(player.getLocale().substring(0, 2)));

                Location location = pp.getLocation();
                PlotArea area = location.getPlotArea();
                if (area != null) {
                    Plot plot = area.getPlot(location);
                    if (plot != null) {
                        plotListener.plotEntry(pp, plot);
                    }
                }
                // Async
                TaskManager.runTaskLaterAsync(() -> {
                    /* TODO CHECK ON THIS */
                /*if (!player.hasPlayedBefore() && player.isLocalPlayer()) {
                    player.saveData();
                }*/
                    this.eventDispatcher.doJoinTask(pp);
                }, TaskTime.seconds(1L));
            }, TaskTime.seconds(3L));

        });
        ServerPlayConnectionEvents.DISCONNECT.register(this::onLeave);

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            PlotPlayer<ServerPlayer> pp = FabricUtil.adapt(newPlayer);
            this.eventDispatcher.doRespawnTask(pp);
        });


        ServerPlayerTeleportToCallback.EVENT.register((serverLevel, x, y, z, set, g, h, serverPlayer) -> {
            ServerPlayer player = serverPlayer;
            //We need to account for bad plugins like NoCheatPlus that teleports player on/before login -_-
            if (!player.connection.isAcceptingMessages()) {
                return InteractionResult.FAIL;
            }
            FabricPlayer pp = FabricUtil.adapt(player);
            try (final MetaDataAccess<Plot> lastPlotAccess =
                         pp.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_LAST_PLOT)) {
                Plot lastPlot = lastPlotAccess.get().orElse(null);
                GlobalPos to = GlobalPos.of(serverLevel.dimension(), new BlockPos((int) x, (int) y, (int) z));
                //noinspection ConstantConditions
                if (to != null) {
                    Location location = FabricUtil.adapt(to);
                    PlotArea area = location.getPlotArea();
                    if (area == null) {
                        if (lastPlot != null) {
                            plotListener.plotExit(pp, lastPlot);
                            lastPlotAccess.remove();
                        }
                        try (final MetaDataAccess<Location> lastLocationAccess =
                                     pp.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_LOCATION)) {
                            lastLocationAccess.remove();
                        }
                        return InteractionResult.PASS;
                    }
                    Plot plot = area.getPlot(location);
                    if (plot != null) {
                        final boolean result = DenyTeleportFlag.allowsTeleport(pp, plot);
                        // there is one possibility to still allow teleportation:
                        // to is identical to the plot's home location, and untrusted-visit is true
                        // i.e. untrusted-visit can override deny-teleport
                        // this is acceptable, because otherwise it wouldn't make sense to have both flags set
                        if (!result && !(plot.getFlag(UntrustedVisitFlag.class) && plot
                                .getHomeSynchronous()
                                .equals(FabricUtil.adaptComplete(to, g, h)))) {
                            pp.sendMessage(
                                    TranslatableCaption.of("deny.no_enter"),
                                    TagResolver.resolver("plot", Tag.inserting(Component.text(plot.toString())))
                            );
                            return InteractionResult.FAIL;
                        }
                    }
                }
            }
            return InteractionResult.PASS;
        });
        HandleMoveVehicleCallback.EVENT.register((serverboundMoveVehiclePacket, serverPlayer) -> {
            final Vec3 from = serverPlayer.getRootVehicle().position();
            final Vec3 to = new Vec3(serverboundMoveVehiclePacket.getX(), serverboundMoveVehiclePacket.getY(),
                    serverboundMoveVehiclePacket.getZ()
            );

            int toX, toZ;
            if ((toX = MathMan.roundInt(to.x)) != MathMan.roundInt(from.x) | (toZ = MathMan.roundInt(to.z)) != MathMan
                    .roundInt(from.z)) {
                Entity vehicle = serverPlayer.getRootVehicle();

                // Check allowed
                if (!vehicle.getPassengers().isEmpty()) {
                    Entity passenger = vehicle.getPassengers().get(0);

                    if (passenger instanceof final ServerPlayer player) {
                        List<Entity> passengers = vehicle.getPassengers();
                        InteractionResult result =
                                HandlePlayerMoveCallback.EVENT
                                        .invoker()
                                        .handlePlayerMoveCallback(new ServerboundMovePlayerPacket.PosRot(to.x, to.y, to.z,
                                                serverboundMoveVehiclePacket.getXRot(), serverboundMoveVehiclePacket.getYRot(),
                                                serverPlayer.onGround()
                                        ), serverPlayer);
                        Vec3 dest;
                        if (result == InteractionResult.FAIL) {
                            dest = from;
                        } else if (MathMan.roundInt(to.x) != toX || MathMan.roundInt(to.z) != toZ) {
                            dest = to;
                        } else {
                            dest = null;
                        }
                        if (dest != null) {
                            vehicle.ejectPassengers();
                            vehicle.setDeltaMovement(new Vec3(0d, 0d, 0d));
                            vehicle.teleportTo(dest.x, dest.y, dest.z);
                            passengers.forEach(entity -> entity.startRiding(vehicle));
                            return InteractionResult.PASS;
                        }
                    }
                    if (Settings.Enabled_Components.KILL_ROAD_VEHICLES) {
                        final com.sk89q.worldedit.world.entity.EntityType entityType =
                                EntityTypes.get(BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType()).toString());
                        // Horses etc are vehicles, but they're also animals
                        // so this filters out all living entities
                        if (EntityCategories.VEHICLE.contains(entityType) && !EntityCategories.ANIMAL.contains(entityType)) {

                            Plot toPlot =
                                    FabricUtil.adapt(GlobalPos.of(serverPlayer.serverLevel().dimension(), new BlockPos(
                                            (int) to.x,
                                            (int) to.y,
                                            (int) to.z
                                    ))).getPlot();
                            if (vehicle.hasAttached(PLOT_DATA)) {
                                Plot origin = Plot.fromString(null, vehicle.getAttached(PLOT_DATA));
                                if (origin != null && !origin.getBasePlot(false).equals(toPlot)) {
                                    vehicle.remove(Entity.RemovalReason.DISCARDED);
                                }
                            } else if (toPlot != null) {
                                vehicle.setAttached(PLOT_DATA, toPlot.toString());
                            }
                        }
                    }
                }

            }
            return InteractionResult.PASS;
        });
        Stimuli.global().listen(PlayerChatEvent.EVENT, (serverPlayer, playerChatMessage, bound) -> {
            FabricPlayer plotPlayer = FabricUtil.adapt(serverPlayer);
            Location location = plotPlayer.getLocation();
            PlotArea area = location.getPlotArea();
            if (area == null) {
                return InteractionResult.PASS;
            }
            Plot plot = area.getPlot(location);
            if (plot == null) {
                return InteractionResult.PASS;
            }
            if (!((plot.getFlag(ChatFlag.class) && area.isPlotChat() && plotPlayer.getAttribute("chat"))
                    || area.isForcingPlotChat())) {
                return InteractionResult.FAIL;
            }
            if (plot.isDenied(plotPlayer.getUUID()) && !plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_CHAT_BYPASS)) {
                return InteractionResult.FAIL;
            }
            /*
            event.setCancelled(true);
            Set<Player> recipients = event.getRecipients();
            recipients.clear();*/
            Set<PlotPlayer<?>> spies = new HashSet<>();
            Set<PlotPlayer<?>> plotRecipients = new HashSet<>();
            for (final PlotPlayer<?> pp : PlotSquared.platform().playerManager().getPlayers()) {
                if (pp.getAttribute("chatspy")) {
                    spies.add(pp);
                } else {
                    Plot current = pp.getCurrentPlot();
                    if (current != null && current.getBasePlot(false).equals(plot)) {
                        plotRecipients.add(pp);
                    }
                }
            }
            String message = playerChatMessage.message();
            String sender = serverPlayer.getDisplayName().getString();
            PlotId id = plot.getId();
            String worldName = plot.getWorldName();
            Caption msg = TranslatableCaption.of("chat.plot_chat_format");
            TagResolver.Builder builder = TagResolver.builder();
            builder.tag("world", Tag.inserting(Component.text(worldName)));
            builder.tag("plot_id", Tag.inserting(Component.text(id.toString())));
            builder.tag("sender", Tag.inserting(Component.text(sender)));
            if (plotPlayer.hasPermission("plots.chat.color")) {
                builder.tag("msg", Tag.inserting(MiniMessage.miniMessage().deserialize(
                        message,
                        TagResolver.resolver(StandardTags.color(), StandardTags.gradient(),
                                StandardTags.rainbow(), StandardTags.decorations()
                        )
                )));
            } else {
                builder.tag("msg", Tag.inserting(Component.text(message)));
            }
            for (PlotPlayer<?> receiver : plotRecipients) {
                receiver.sendMessage(msg, builder.build());
            }
            if (!spies.isEmpty()) {
                Caption spymsg = TranslatableCaption.of("chat.plot_chat_spy_format");
                for (PlotPlayer<?> player : spies) {
                    player.sendMessage(spymsg, builder.tag("message", Tag.inserting(Component.text(message))).build());
                }
            }
            if (Settings.Chat.LOG_PLOTCHAT_TO_CONSOLE) {
                Caption spymsg = TranslatableCaption.of("chat.plot_chat_spy_format");
                ConsolePlayer.getConsole().sendMessage(
                        spymsg,
                        builder.tag("message", Tag.inserting(Component.text(message))).build()
                );
            }
            //cancel the original message
            return InteractionResult.FAIL;
        });

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            TaskManager.runTaskLater(() -> {
                FabricPlayer pp = FabricUtil.adapt(player);
                // Delete last location
                Plot plot;
                try (final MetaDataAccess<Plot> lastPlotAccess =
                             pp.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_LAST_PLOT)) {
                    plot = lastPlotAccess.remove();
                }
                try (final MetaDataAccess<Location> lastLocationAccess =
                             pp.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_LOCATION)) {
                    lastLocationAccess.remove();
                }
                if (plot != null) {
                    plotListener.plotExit(pp, plot);
                }
                if (this.worldEdit != null) {
                    if (!pp.hasPermission(Permission.PERMISSION_WORLDEDIT_BYPASS)) {
                        if (pp.getAttribute("worldedit")) {
                            pp.removeAttribute("worldedit");
                        }
                    }
                }
                Location location = pp.getLocation();
                PlotArea area = location.getPlotArea();
                if (location.isPlotArea()) {
                    plot = location.getPlot();
                    if (plot != null) {
                        plotListener.plotEntry(pp, plot);
                    }
                }
            }, TaskTime.seconds(3));

        });
        Stimuli.global().listen(PlayerInventoryActionEvent.EVENT, (serverPlayer, i, clickType, i1) -> {
        /*if (!event.isLeftClick() || (event.getAction() != InventoryAction.PLACE_ALL) || event
            .isShiftClick()) {
            return;
        }*/
            if (!this.plotAreaManager
                    .hasPlotArea(serverPlayer.serverLevel().dimension().location().getPath().toString())) {
                return InteractionResult.PASS;
            }

            FabricPlayer pp = FabricUtil.adapt(serverPlayer);
            final PlotInventory inventory = PlotInventory.getOpenPlotInventory(pp);
            if (inventory != null) {
                if (!inventory.onClick(i)) {
                    inventory.close();
                    return InteractionResult.FAIL;
                }
            }
            Inventory inv = serverPlayer.getInventory();
            int slot = inv.selected;
            if ((slot > 8) || !serverPlayer.isCreative()) {
                return InteractionResult.PASS;
            }
            ItemStack oldItem = serverPlayer.getMainHandItem();

            List<String> oldMeta = getLore(oldItem);
            ItemStack newItem = inv.getSelected();
            List<String> newMeta = getLore(newItem);

            if (clickType == ClickType.CLONE) {
                final Plot plot = pp.getCurrentPlot();
                if (plot != null) {
                    if (plot.getFlag(PreventCreativeCopyFlag.class) && !plot
                            .isAdded(serverPlayer.getUUID()) && !pp.hasPermission(Permission.PERMISSION_ADMIN_INTERACT_OTHER)) {
                        final ItemStack newStack =
                                new ItemStack(newItem.getItem(), newItem.getCount());
                        inv.setItem(slot, newStack);
                        plot.debug(serverPlayer.getName()
                                + " could not creative-copy an item because prevent-creative-copy = true");
                    }
                } else {
                    PlotArea area = pp.getPlotAreaAbs();
                    if (area != null && PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, PreventCreativeCopyFlag.class, true)) {
                        final ItemStack newStack =
                                new ItemStack(newItem.getItem(), newItem.getCount());
                        inv.setItem(slot, newStack);
                    }
                }
                return InteractionResult.PASS;
            }

            String newLore = "";
            if (newMeta != null) {
                List<String> lore = newMeta;
                if (lore != null) {
                    newLore = lore.toString();
                }
            }
            String oldLore = "";
            if (oldMeta != null) {
                List<String> lore = oldMeta;
                if (lore != null) {
                    oldLore = lore.toString();
                }
            }
            Item itemType = newItem.getItem();
            if (!"[(+NBT)]".equals(newLore) || (oldItem.equals(newItem) && newLore.equals(oldLore))) {
                if (newMeta == null || (itemType != Items.PLAYER_HEAD)) {
                    return InteractionResult.PASS;
                }
            }
            HitResult hit = serverPlayer.pick(7, 1, false);
            BlockState state = serverPlayer.serverLevel().getBlockState(new BlockPos(
                    (int) hit.getLocation().x,
                    (int) hit.getLocation().y,
                    (int) hit.getLocation().z
            ));
            Block stateType = state.getBlock();
            if (stateType.asItem() != itemType) {
                return InteractionResult.FAIL;
            }
            Location location = FabricUtil.adapt(GlobalPos.of(serverPlayer.serverLevel().dimension(), new BlockPos(
                    (int) hit.getLocation().x,
                    (int) hit.getLocation().y,
                    (int) hit.getLocation().z
            )));
            PlotArea area = location.getPlotArea();
            if (area == null) {
                return InteractionResult.PASS;
            }
            Plot plot = area.getPlotAbs(location);
            boolean cancelled = false;
            if (plot == null) {
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_INTERACT_ROAD)) {
                    pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_INTERACT_ROAD)
                            )
                    );
                    cancelled = true;
                }
            } else if (!plot.hasOwner()) {
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_INTERACT_UNOWNED)) {
                    pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_INTERACT_UNOWNED)
                            )
                    );
                    cancelled = true;
                }
            } else {
                UUID uuid = pp.getUUID();
                if (!plot.isAdded(uuid)) {
                    if (!pp.hasPermission(Permission.PERMISSION_ADMIN_INTERACT_OTHER)) {
                        pp.sendMessage(
                                TranslatableCaption.of("permission.no_permission_event"),
                                TagResolver.resolver(
                                        "node",
                                        Tag.inserting(Permission.PERMISSION_ADMIN_INTERACT_OTHER)
                                )
                        );
                        cancelled = true;
                    }
                }
            }
            if (cancelled) {
                ItemStack newItemCopy = newItem.copyWithCount(newItem.getCount());
                newItemCopy.setDamageValue(newItem.getDamageValue());
                if ((oldItem.getItem() == newItem.getItem()) && (oldItem.getDamageValue()) == newItem
                        .getDamageValue()) {
                    serverPlayer.inventoryMenu.setCarried(newItemCopy);
                    return InteractionResult.PASS;
                }
                serverPlayer.inventoryMenu.setCarried(
                        newItemCopy);
            }
            return InteractionResult.PASS;
        });
        Stimuli.global().listen(EntityUseEvent.EVENT, (serverPlayer, entity, interactionHand, entityHitResult) -> {
            if (!(entity instanceof ArmorStand) && !(entity instanceof ItemFrame)) {
                return InteractionResult.PASS;
            }
            Location location = FabricUtil.adapt(GlobalPos.of(entity.level().dimension(), entity.blockPosition()));
            PlotArea area = location.getPlotArea();
            if (area == null) {
                return InteractionResult.PASS;
            }
            EntitySpawnListener.testNether(entity);
            Plot plot = location.getPlotAbs();
            FabricPlayer pp = FabricUtil.adapt(serverPlayer);
            if (plot == null) {
                if (!PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, MiscInteractFlag.class, true) && !pp.hasPermission(
                        Permission.PERMISSION_ADMIN_INTERACT_ROAD
                )) {
                    pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_INTERACT_ROAD)
                            )
                    );
                    return InteractionResult.FAIL;
                }
            } else {
                if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot)) {
                    if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                        pp.sendMessage(TranslatableCaption.of("done.building_restricted"));
                        return InteractionResult.FAIL;
                    }
                }
                if (!plot.hasOwner()) {
                    if (!pp.hasPermission("plots.admin.interact.unowned")) {
                        pp.sendMessage(
                                TranslatableCaption.of("permission.no_permission_event"),
                                TagResolver.resolver(
                                        "node",
                                        Tag.inserting(Permission.PERMISSION_ADMIN_INTERACT_UNOWNED)
                                )
                        );
                        return InteractionResult.FAIL;
                    }
                } else {
                    UUID uuid = pp.getUUID();
                    if (plot.isAdded(uuid)) {
                        return InteractionResult.PASS;
                    }
                    if (plot.getFlag(MiscInteractFlag.class)) {
                        return InteractionResult.PASS;
                    }
                    if (!pp.hasPermission(Permission.PERMISSION_ADMIN_INTERACT_OTHER)) {
                        pp.sendMessage(
                                TranslatableCaption.of("permission.no_permission_event"),
                                TagResolver.resolver(
                                        "node",
                                        Tag.inserting(Permission.PERMISSION_ADMIN_INTERACT_OTHER)
                                )
                        );
                        plot.debug(pp.getName() + " could not interact with " + entity.getType()
                                + " because misc-interact = false");
                        return InteractionResult.FAIL;
                    }
                }
            }
            return InteractionResult.PASS;
        });
    }

    private static InteractionResult interact(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        boolean cancelled = false;
        ItemStack itemStack = player.getItemInHand(hand);
        BlockState blockstate = world.getBlockState(hitResult.getBlockPos());
        Block block = blockstate.getBlock();
        if (block instanceof SignBlock) {
            if (/*DYES.contains(itemStack.getItem().toString())*/itemStack.getItem() instanceof DyeItem) {
                Location location = FabricUtil.adapt(GlobalPos.of(world.dimension(), hitResult.getBlockPos()));
                PlotArea area = location.getPlotArea();
                if (area == null) {
                    return InteractionResult.FAIL;
                }
                Plot plot = location.getOwnedPlot();
                ServerPlayer serverPlayer = player.getServer().getPlayerList().getPlayer(player.getUUID());
                if (plot == null) {
                    if (PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, EditSignFlag.class, false)
                            && !FabricUtil
                            .adapt(serverPlayer)
                            .hasPermission(Permission.PERMISSION_ADMIN_INTERACT_ROAD.toString())) {
                        cancelled = true;
                    }
                    return cancelled ? InteractionResult.FAIL : InteractionResult.PASS;
                }
                if (plot.isAdded(player.getUUID())) {
                    return InteractionResult.PASS; // allow for added players
                }
                if (!plot.getFlag(EditSignFlag.class)
                        && !FabricUtil
                        .adapt(serverPlayer)
                        .hasPermission(Permission.PERMISSION_ADMIN_INTERACT_OTHER.toString())) {
                    plot.debug(player.getName() + " could not color the sign because of edit-sign = false");
                    cancelled = true;
                }
            }
        }
        return cancelled ? InteractionResult.FAIL : InteractionResult.PASS;
    }

    private static InteractionResult onPlayerCommand(ServerPlayer serverPlayer, String s) {
        String msg = s.replace("/", "").toLowerCase(Locale.ROOT).trim();
        if (msg.isEmpty()) {
            return InteractionResult.PASS;
        }
        ServerPlayer player = serverPlayer;
        PlotPlayer<ServerPlayer> plotPlayer = FabricUtil.adapt(player);
        Location location = plotPlayer.getLocation();
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        }
        String[] parts = msg.split(" ");
        Plot plot = plotPlayer.getCurrentPlot();
        // Check WorldEdit
        switch (parts[0]) {
            case "up", "worldedit:up" -> {
                if (plot == null || (!plot.isAdded(plotPlayer.getUUID()) && !plotPlayer.hasPermission(
                        Permission.PERMISSION_ADMIN_BUILD_OTHER,
                        true
                ))) {
                    return InteractionResult.FAIL;
                }
            }
        }
        if (plot == null && !area.isRoadFlags()) {
            return InteractionResult.PASS;
        }

        List<String> blockedCommands = plot != null ?
                plot.getFlag(BlockedCmdsFlag.class) :
                area.getFlag(BlockedCmdsFlag.class);
        if (blockedCommands.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_INTERACT_BLOCKED_CMDS)) {
            return InteractionResult.PASS;
        }
        // When using namespaced commands, we're not interested in the namespace
        /*
        String part = parts[0];
        if (part.contains(":")) {
            String[] namespaced = part.split(":");
            part = namespaced[1];
            msg = msg.substring(namespaced[0].length() + 1);
        }
        msg = replaceAliases(msg, part);*/
        for (String blocked : blockedCommands) {
            if (blocked.equalsIgnoreCase(msg)) {
                String perm;
                if (plot != null && plot.isAdded(plotPlayer.getUUID())) {
                    perm = "plots.admin.command.blocked-cmds.shared";
                } else {
                    perm = "plots.admin.command.blocked-cmds.road";
                }
                if (!plotPlayer.hasPermission(perm)) {
                    plotPlayer.sendMessage(TranslatableCaption.of("blockedcmds.command_blocked"));
                    return InteractionResult.FAIL;
                }
                return InteractionResult.PASS;
            }
        }
        return InteractionResult.PASS;
    }

    private static void onLoginInit(ServerGamePacketListenerImpl handler, MinecraftServer server) {
        final UUID uuid;
        if (Settings.UUID.OFFLINE) {
            if (Settings.UUID.FORCE_LOWERCASE) {
                uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + handler.player.getName().getString()
                        .toLowerCase()).getBytes(Charsets.UTF_8));
            } else {
                uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + handler
                        .getPlayer()
                        .getName()
                        .getString()).getBytes(Charsets.UTF_8));
            }
        } else {
            uuid = UUID.fromString(handler.player.getStringUUID());
            //server.getProfileRepository().findProfilesByNames().get(handler.getUserName()).get().getId();
        }
        PlotSquared.get().getImpromptuUUIDPipeline().storeImmediately(handler.player.getName().getString(), uuid);
    }

    public static String getUUID(String playerName) throws IOException {
        String url = "https://api.mojang.com/users/profiles/minecraft/" + playerName;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        // Reading the response
        Scanner scanner = new Scanner(connection.getInputStream());
        String response = scanner.useDelimiter("\\A").next();
        scanner.close();

        JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
        return jsonObject.get("id").getAsString();
    }

    public void afterBlockBreak(
            Level world,
            Player player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity entity
    ) {
        Location location = FabricUtil.adapt(GlobalPos.of(world.dimension(), pos));
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Plot plot = area.getPlot(location);
        if (plot != null) {
            Stimuli.global().listen(BlockDropItemsEvent.EVENT, (entity1, serverLevel, blockPos, blockState, list) -> {
                if (plot.getFlag(TileDropFlag.class)) {
                    return InteractionResultHolder.pass(list);
                }
                return InteractionResultHolder.fail(list);
            });
        }
    }

    public List<String> getLore(ItemStack stack) {
        if (stack.hasTag() && stack.getOrCreateTag().contains("display", CompoundTag.TAG_COMPOUND)) {
            CompoundTag displayTag = stack.getTag().getCompound("display");
            if (displayTag.contains("Lore", ListTag.TAG_LIST)) {
                ListTag loreList = displayTag.getList("Lore", StringTag.TAG_STRING);
                List<String> lore = new ArrayList<>();
                for (int i = 0; i < loreList.size(); i++) {
                    lore.add(loreList.getString(i));
                }
                return lore;
            }
        }
        return Collections.emptyList();
    }

    public void setLore(ItemStack stack, List<String> lore) {
        CompoundTag displayTag;
        if (stack.hasTag() && stack.getOrCreateTag().contains("display", CompoundTag.TAG_COMPOUND)) {
            displayTag = stack.getTag().getCompound("display");
        } else {
            displayTag = new CompoundTag();
            stack.getOrCreateTag().put("display", displayTag);
        }

        ListTag loreList = new ListTag();
        for (String line : lore) {
            loreList.add(StringTag.valueOf(line));
        }
        displayTag.put("Lore", loreList);
    }

    /*
    @EventHandler(priority = EventPriority.LOW)
    @SuppressWarnings("deprecation") // Paper deprecation
    public void onCancelledInteract(PlayerInteractEvent event) {
        if (event.isCancelled() && event.getAction() == Action.RIGHT_CLICK_AIR) {
            Player player = event.getPlayer();
            FabricPlayer pp = FabricUtil.adapt(player);
            PlotArea area = pp.getPlotAreaAbs();
            if (area == null) {
                return;
            }
            if (event.getAction() == Action.RIGHT_CLICK_AIR) {
                Material item = event.getMaterial();
                if (item.toString().toLowerCase().endsWith("_egg")) {
                    event.setCancelled(true);
                    event.setUseItemInHand(Event.Result.DENY);
                }
            }
            ItemStack hand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();
            Material type = hand.getType();
            Material offType = offHand.getType();
            if (type == Material.AIR) {
                type = offType;
            }
            if (type.toString().toLowerCase().endsWith("_egg")) {
                Block block = player.getTargetBlockExact(5, FluidCollisionMode.SOURCE_ONLY);
                if (block != null && block.getType() != Material.AIR) {
                    Location location = FabricUtil.adapt(block.getLocation());
                    if (!this.eventDispatcher.checkPlayerBlockEvent(pp, PlayerBlockEventType.SPAWN_MOB, location, null, true)) {
                        event.setCancelled(true);
                        event.setUseItemInHand(Event.Result.DENY);
                    }
                }
            }
        }
    }*/


    public InteractionResult onAttackBlock(
            ServerPlayer serverPlayer, ServerLevel serverLevel, InteractionHand hand, BlockPos pos,
            Direction direction
    ) {
        ServerPlayer player = serverPlayer;
        FabricPlayer pp = FabricUtil.adapt(player);
        PlotArea area = pp.getPlotAreaAbs();
        if (area == null) {
            return InteractionResult.PASS;
        }
        PlayerBlockEventType eventType;
        BlockType blocktype1;
        Block blockType = serverLevel.getBlockState(pos).getBlock();
        Location location = FabricUtil.adapt(GlobalPos.of(serverLevel.dimension(), pos));
        // todo: when the code above is rearranged, it would be great to beautify this as well.
        // will code this as a temporary, specific bug fix (for dragon eggs)
        if (blockType != Blocks.DRAGON_EGG) {
            return InteractionResult.FAIL;
        }

        eventType = PlayerBlockEventType.INTERACT_BLOCK;
        blocktype1 = FabricAdapter.adapt(blockType);

        if (this.worldEdit != null && pp.getAttribute("worldedit")) {
            if (serverPlayer
                    .getItemInHand(hand)
                    .getItem() == BuiltInRegistries.ITEM.get(new ResourceLocation(this.worldEdit.getConfiguration().wandItem))) {
                return InteractionResult.PASS;
            }
        }
        if (!this.eventDispatcher.checkPlayerBlockEvent(pp, eventType, location, blocktype1, true)) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onUseBlock(
            ServerPlayer serverPlayer,
            ServerLevel serverLevel,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        ServerPlayer player = serverPlayer;
        FabricPlayer pp = FabricUtil.adapt(player);
        PlotArea area = pp.getPlotAreaAbs();
        if (area == null) {
            return InteractionResult.PASS;
        }
        PlayerBlockEventType eventType;
        BlockType blocktype1;
        Block blockType = serverLevel.getBlockState(hitResult.getBlockPos()).getBlock();
        Location location = FabricUtil.adapt(GlobalPos.of(serverLevel.dimension(), hitResult.getBlockPos()));

        eventType = PlayerBlockEventType.INTERACT_BLOCK;
        blocktype1 = FabricAdapter.adapt(blockType);

        if (player.isCrouching()) {

            ItemStack handItemStack = player.getMainHandItem();
            ItemStack offHandItemStack = player.getOffhandItem();

            // sneaking players interact with blocks if both hands are empty
            if (handItemStack.isEmpty() && offHandItemStack.isEmpty()) {
                /*
                if (this.worldEdit != null && pp.getAttribute("worldedit")) {
                    if (event.getMaterial() == Material.getMaterial(this.worldEdit.getConfiguration().wandItem)) {
                        return InteractionResult.PASS;
                    }
                }*/
                if (!this.eventDispatcher.checkPlayerBlockEvent(pp, eventType, location, blocktype1, true)) {
                    return InteractionResult.FAIL;
                }
            }
        }

        Item type = player.getItemInHand(hand).getItem();

        // in the following, lb needs to have the material of the item in hand i.e. type
        switch (type.toString()) {
            case "redstone", "string", "pumpkin_seeds", "melon_seeds", "cocoa_beans", "wheat_seeds", "beetroot_seeds",
                    "sweet_berries", "glow_berries" -> {
                return InteractionResult.PASS;
            }
            default -> {
                //eventType = PlayerBlockEventType.PLACE_BLOCK;
                if (type instanceof BlockItem) {
                    return InteractionResult.PASS;
                }
            }
        }
        /*if (PaperLib.isPaper()) {
            if (MaterialTags.SPAWN_EGGS.isTagged(type) || Material.EGG.equals(type)) {
                eventType = PlayerBlockEventType.SPAWN_MOB;
                if (this.worldEdit != null && pp.getAttribute("worldedit")) {
                    if (event.getMaterial() == Material.getMaterial(this.worldEdit.getConfiguration().wandItem)) {
                        return InteractionResult.PASS;
                    }
                }
                if (!this.eventDispatcher.checkPlayerBlockEvent(pp, eventType, location, blocktype1, true)) {
                    return InteractionResult.FAIL;
                }
            }
        } else {*/
        if (type instanceof SpawnEggItem) {
            eventType = PlayerBlockEventType.SPAWN_MOB;
            if (this.worldEdit != null && pp.getAttribute("worldedit")) {
                if (type == BuiltInRegistries.ITEM.get(new ResourceLocation(this.worldEdit.getConfiguration().wandItem))) {
                    return InteractionResult.PASS;
                }
            }
            if (!this.eventDispatcher.checkPlayerBlockEvent(pp, eventType, location, blocktype1, true)) {
                return InteractionResult.FAIL;
            }
        }
        //}
        if (type.isEdible()) {
            //Allow all players to eat while also allowing the block place event to be fired
            return InteractionResult.PASS;
        }
        if (type == Items.ARMOR_STAND) {
            location =
                    FabricUtil.adapt(GlobalPos.of(
                            serverLevel.dimension(),
                            hitResult.getBlockPos().relative(hitResult.getDirection().getOpposite())
                    ));
            eventType = PlayerBlockEventType.PLACE_MISC;
        }
        if (TagUtil.isIn(ItemTags.BOATS, type) || MINECARTS.contains(type)) {
            eventType = PlayerBlockEventType.PLACE_VEHICLE;
            if (this.worldEdit != null && pp.getAttribute("worldedit")) {
                if (type == BuiltInRegistries.ITEM.get(new ResourceLocation(this.worldEdit.getConfiguration().wandItem))) {
                    return InteractionResult.PASS;
                }
            }
            if (!this.eventDispatcher.checkPlayerBlockEvent(pp, eventType, location, blocktype1, true)) {
                return InteractionResult.FAIL;
            }
        }
        if (type instanceof FireworkRocketItem || type instanceof FireworkStarItem) {
            eventType = PlayerBlockEventType.SPAWN_MOB;
            if (this.worldEdit != null && pp.getAttribute("worldedit")) {
                if (type == BuiltInRegistries.ITEM.get(new ResourceLocation(this.worldEdit.getConfiguration().wandItem))) {
                    return InteractionResult.PASS;
                }
            }
            if (!this.eventDispatcher.checkPlayerBlockEvent(pp, eventType, location, blocktype1, true)) {
                return InteractionResult.FAIL;
            }
        }
        if (BOOKS.contains(type)) {
            eventType = PlayerBlockEventType.READ;
            if (this.worldEdit != null && pp.getAttribute("worldedit")) {
                if (type == BuiltInRegistries.ITEM.get(new ResourceLocation(this.worldEdit.getConfiguration().wandItem))) {
                    return InteractionResult.PASS;
                }
            }
            if (!this.eventDispatcher.checkPlayerBlockEvent(pp, eventType, location, blocktype1, true)) {
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onTrample(
            LivingEntity livingEntity,
            ServerLevel serverLevel,
            BlockPos pos,
            BlockState from,
            BlockState to
    ) {
        if (livingEntity instanceof ServerPlayer serverPlayer) {
            FabricPlayer pp = FabricUtil.adapt(serverPlayer);
            PlotArea area = pp.getPlotAreaAbs();
            if (area == null) {
                return InteractionResult.PASS;
            }
            PlayerBlockEventType eventType;
            BlockType blocktype1;
            Block blockType = serverLevel.getBlockState(pos).getBlock();
            Location location = FabricUtil.adapt(GlobalPos.of(serverLevel.dimension(), pos));

            eventType = PlayerBlockEventType.TRIGGER_PHYSICAL;
            blocktype1 = FabricAdapter.adapt(blockType);
            if (this.worldEdit != null && pp.getAttribute("worldedit")) {
                if (serverPlayer
                        .getUseItem()
                        .getItem() == BuiltInRegistries.ITEM.get(new ResourceLocation(this.worldEdit.getConfiguration().wandItem))) {
                    return InteractionResult.PASS;
                }
            }
            if (!this.eventDispatcher.checkPlayerBlockEvent(pp, eventType, location, blocktype1, true)) {
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    /* TODO CHECK ON THIS BOAT */
    // Boats can sometimes be placed on interactable blocks such as levers,
    // see PS-175. Armor stands, minecarts and end crystals (the other entities
    // supported by this event) don't have this issue.
    /*public InteractionResultHolder<ItemStack> onBoatPlace(ServerPlayer serverPlayer, InteractionHand hand) {
        if (serverPlayer.getUseItem().getItem() instanceof BoatItem) {
            ServerPlayer player = serverPlayer;
            if (player == null) {
                return InteractionResultHolder.pass(ItemStack.EMPTY);
            }
            Entity placed = event.getEntity();
            if (!(placed instanceof Boat)) {
                return InteractionResultHolder.pass(ItemStack.EMPTY);
            }
            FabricPlayer pp = FabricUtil.adapt(serverPlayer);
            PlotArea area = pp.getPlotAreaAbs();
            if (area == null) {
                return InteractionResultHolder.pass(ItemStack.EMPTY);
            }
            PlayerBlockEventType eventType = PlayerBlockEventType.PLACE_VEHICLE;
            Block block = event.getBlock();
            BlockType blockType = FabricAdapter.adapt(block);
            Location location = FabricUtil.adapt(block.getLocation());
            if (!PlotSquared.get().getEventDispatcher()
                    .checkPlayerBlockEvent(pp, eventType, location, blockType, true)) {
                return InteractionResultHolder.fail(ItemStack.EMPTY);
            }
        }
        return InteractionResultHolder.pass(ItemStack.EMPTY);
    }*/

    public InteractionResult onBucketEmpty(@Nullable Player player, Level level, BlockPos blockPos, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer && player.getUseItem().getItem() instanceof BucketItem bucketItem) {
            Direction direction = hitResult.getDirection();
            // Note: a month after Bukkit 1.14.4 released, they added the API method
            // PlayerBucketEmptyEvent#getBlock(), which returns the block the
            // bucket contents is going to be placed at. Currently we determine this
            // block ourselves to retain compatibility with 1.13.
            BlockState blockState = serverPlayer.serverLevel().getBlockState(hitResult.getBlockPos());
            final BlockPos TargetBlockPos;
            // if the block can be waterlogged, the event might waterlog the block
            // sometimes
            if (blockState.hasProperty(BlockStateProperties.WATERLOGGED) && !blockState.getValue(BlockStateProperties.WATERLOGGED) && bucketItem != Items.LAVA_BUCKET) {
                TargetBlockPos = hitResult.getBlockPos();
            } else {
                TargetBlockPos = hitResult.getBlockPos().relative(direction);
            }
            Location location = FabricUtil.adapt(GlobalPos.of(level.dimension(), TargetBlockPos));
            PlotArea area = location.getPlotArea();
            if (area == null) {
                return InteractionResult.PASS;
            }
            FabricPlayer pp = FabricUtil.adapt(serverPlayer);
            Plot plot = area.getPlot(location);
            if (plot == null) {
                if (pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_ROAD)) {
                    return InteractionResult.PASS;
                }
                pp.sendMessage(
                        TranslatableCaption.of("permission.no_permission_event"),
                        TagResolver.resolver("node", Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_ROAD))
                );
                return InteractionResult.FAIL;
            } else if (!plot.hasOwner()) {
                if (pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_UNOWNED)) {
                    return InteractionResult.PASS;
                }
                pp.sendMessage(
                        TranslatableCaption.of("permission.no_permission_event"),
                        TagResolver.resolver(
                                "node",
                                Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_UNOWNED)
                        )
                );
                return InteractionResult.FAIL;
            } else if (!plot.isAdded(pp.getUUID())) {
                if (pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                    return InteractionResult.PASS;
                }
                pp.sendMessage(
                        TranslatableCaption.of("permission.no_permission_event"),
                        TagResolver.resolver(
                                "node",
                                Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_OTHER)
                        )
                );
                return InteractionResult.FAIL;
            } else if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot)) {
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                    pp.sendMessage(
                            TranslatableCaption.of("done.building_restricted")
                    );
                    return InteractionResult.FAIL;
                }
            }
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onInventoryClose(
            ServerboundContainerClosePacket serverboundContainerClosePacket,
            ServerPlayer serverPlayer
    ) {
        TaskManager.runTaskLaterAsync(() -> {
            PlotInventory.removePlotInventoryOpen(FabricUtil.adapt(serverPlayer));
        }, TaskTime.seconds(3L));
        return InteractionResult.PASS;
    }

    public void onLeave(ServerGamePacketListenerImpl handler, MinecraftServer server) {
        TaskManager.removeFromTeleportQueue(handler.player.getName().getString());
        FabricPlayer pp = FabricUtil.adapt(handler.player);
        pp.unregister();
        plotListener.logout(pp.getUUID());
    }

    public InteractionResult onBucketFill(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.getUseItem().getItem() instanceof BucketItem) {
                BlockPos blockClicked = hitResult.getBlockPos();
                Location location = FabricUtil.adapt(GlobalPos.of(level.dimension(), blockClicked));
                PlotArea area = location.getPlotArea();
                if (area == null) {
                    return InteractionResult.PASS;
                }
                FabricPlayer plotPlayer = FabricUtil.adapt(serverPlayer);
                Plot plot = area.getPlot(location);
                if (plot == null) {
                    if (plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_ROAD)) {
                        return InteractionResult.PASS;
                    }
                    plotPlayer.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver("node", Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_ROAD))
                    );
                    return InteractionResult.FAIL;
                } else if (!plot.hasOwner()) {
                    if (plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_UNOWNED)) {
                        return InteractionResult.PASS;
                    }
                    plotPlayer.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_UNOWNED)
                            )
                    );
                    return InteractionResult.FAIL;
                } else if (!plot.isAdded(plotPlayer.getUUID())) {
                    if (plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                        return InteractionResult.PASS;
                    }
                    plotPlayer.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_OTHER)
                            )
                    );
                    return InteractionResult.FAIL;
                } else if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot)) {
                    if (!plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                        plotPlayer.sendMessage(
                                TranslatableCaption.of("done.building_restricted")
                        );
                        return InteractionResult.FAIL;
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onHangingPlace(ServerPlayer player, InteractionHand hand, BlockHitResult hitResult) {
        if (player.getUseItem().getItem() instanceof HangingEntityItem || player
                .getUseItem()
                .getItem()
                .equals(Items.PAINTING) || player.getUseItem().getItem() instanceof LeadItem) {
            Block block = player.serverLevel().getBlockState(hitResult.getBlockPos()).getBlock();
            Location location = FabricUtil.adapt(GlobalPos.of(player.serverLevel().dimension(), hitResult.getBlockPos()));
            PlotArea area = location.getPlotArea();
            if (area == null) {
                return InteractionResult.PASS;
            }
            ServerPlayer p = player;
            if (p == null) {
                return InteractionResult.FAIL;
            }
            FabricPlayer pp = FabricUtil.adapt(p);
            Plot plot = area.getPlot(location);
            if (plot == null) {
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_ROAD)) {
                    pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_ROAD)
                            )
                    );
                    return InteractionResult.FAIL;
                }
            } else {
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
                    return InteractionResult.PASS;
                }
                if (!plot.isAdded(pp.getUUID())) {
                    if (!plot.getFlag(HangingPlaceFlag.class)) {
                        if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                            pp.sendMessage(
                                    TranslatableCaption.of("permission.no_permission_event"),
                                    TagResolver.resolver(
                                            "node",
                                            Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_OTHER)
                                    )
                            );
                            return InteractionResult.FAIL;
                        }
                        return InteractionResult.PASS;
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onHangingBreakByEntity(LivingEntity entity, DamageSource damageSource) {
        Entity remover = damageSource.getEntity();
        if (remover instanceof ServerPlayer p) {
            Location location = FabricUtil.adapt(GlobalPos.of(entity.level().dimension(), entity.blockPosition()));
            PlotArea area = location.getPlotArea();
            if (area == null) {
                return InteractionResult.PASS;
            }
            FabricPlayer pp = FabricUtil.adapt(p);
            Plot plot = area.getPlot(location);
            if (plot == null) {
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_ROAD)) {
                    pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_DESTROY_ROAD)
                            )
                    );
                    return InteractionResult.FAIL;
                }
            } else if (!plot.hasOwner()) {
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_UNOWNED)) {
                    pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_DESTROY_UNOWNED)
                            )
                    );
                    return InteractionResult.FAIL;
                }
            } else if (!plot.isAdded(pp.getUUID())) {
                if (plot.getFlag(HangingBreakFlag.class)) {
                    return InteractionResult.PASS;
                }
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_OTHER)) {
                    pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_DESTROY_OTHER)
                            )
                    );
                    plot.debug(p.getName()
                            + " could not break hanging entity because hanging-break = false");
                    return InteractionResult.FAIL;
                }
            }
        } else if (remover instanceof Projectile p) {
            if (p.getOwner() instanceof ServerPlayer shooter) {
                Location location = FabricUtil.adapt(GlobalPos.of(entity.level().dimension(), entity.blockPosition()));
                PlotArea area = location.getPlotArea();
                if (area == null) {
                    return InteractionResult.PASS;
                }
                FabricPlayer player = FabricUtil.adapt(shooter);
                Plot plot = area.getPlot(FabricUtil.adapt(GlobalPos.of(entity.level().dimension(), entity.blockPosition())));
                if (plot != null) {
                    if (!plot.hasOwner()) {
                        if (!player.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_UNOWNED)) {
                            player.sendMessage(
                                    TranslatableCaption.of("permission.no_permission_event"),
                                    TagResolver.resolver(
                                            "node",
                                            Tag.inserting(Permission.PERMISSION_ADMIN_DESTROY_UNOWNED)
                                    )
                            );
                            return InteractionResult.FAIL;
                        }
                    } else if (!plot.isAdded(player.getUUID())) {
                        if (!plot.getFlag(HangingBreakFlag.class)) {
                            if (!player.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_OTHER)) {
                                player.sendMessage(
                                        TranslatableCaption.of("permission.no_permission_event"),
                                        TagResolver.resolver(
                                                "node",
                                                Tag.inserting(Permission.PERMISSION_ADMIN_DESTROY_OTHER)
                                        )
                                );
                                plot.debug(player.getName()
                                        + " could not break hanging entity because hanging-break = false");
                                return InteractionResult.FAIL;
                            }
                        }
                    }
                }
            }
        } else {
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onPlayerInteractEntity(
            ServerPlayer serverPlayer, Entity entity, InteractionHand hand,
            EntityHitResult entityHitResult
    ) {
       /* if (event.getRightClicked().getType() == EntityType.UNKNOWN) {
            return;
        }*/
        Location location = FabricUtil.adapt(GlobalPos.of(serverPlayer.serverLevel().dimension(), entity
                .getOnPos()));
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        }
        ServerPlayer p = serverPlayer;
        FabricPlayer pp = FabricUtil.adapt(p);
        Plot plot = area.getPlot(location);
        if (plot == null && !area.isRoadFlags()) {
            if (!pp.hasPermission(Permission.PERMISSION_ADMIN_INTERACT_ROAD)) {
                pp.sendMessage(
                        TranslatableCaption.of("permission.no_permission_event"),
                        TagResolver.resolver(
                                "node",
                                Tag.inserting(Permission.PERMISSION_ADMIN_INTERACT_ROAD)
                        )
                );
                return InteractionResult.FAIL;
            }
        } else if (plot != null && !plot.hasOwner()) {
            if (!pp.hasPermission(Permission.PERMISSION_ADMIN_INTERACT_UNOWNED)) {
                pp.sendMessage(
                        TranslatableCaption.of("permission.no_permission_event"),
                        TagResolver.resolver(
                                "node",
                                Tag.inserting(Permission.PERMISSION_ADMIN_INTERACT_UNOWNED)
                        )
                );
                return InteractionResult.FAIL;
            }
        } else if ((plot != null && !plot.isAdded(pp.getUUID())) || (plot == null && area
                .isRoadFlags())) {
            final com.sk89q.worldedit.world.entity.EntityType entityType = EntityType.REGISTRY.get(entity
                    .getType()
                    .toShortString());

            FlagContainer flagContainer;
            if (plot == null) {
                flagContainer = area.getRoadFlagContainer();
            } else {
                flagContainer = plot.getFlagContainer();
            }

            if (EntityCategories.HOSTILE.contains(entityType) && flagContainer
                    .getFlag(HostileInteractFlag.class).getValue()) {
                return InteractionResult.PASS;
            }

            if (EntityCategories.ANIMAL.contains(entityType) && flagContainer
                    .getFlag(AnimalInteractFlag.class).getValue()) {
                return InteractionResult.PASS;
            }

            // This actually makes use of the interface, so we don't use the
            // category
            if (entity instanceof OwnableEntity && ((OwnableEntity) entity).getOwner() != null && flagContainer
                    .getFlag(TamedInteractFlag.class).getValue()) {
                return InteractionResult.PASS;
            }

            if (EntityCategories.VEHICLE.contains(entityType) && flagContainer
                    .getFlag(VehicleUseFlag.class).getValue()) {
                return InteractionResult.PASS;
            }

            if (EntityCategories.PLAYER.contains(entityType) && flagContainer
                    .getFlag(PlayerInteractFlag.class).getValue()) {
                return InteractionResult.PASS;
            }

            if (EntityCategories.VILLAGER.contains(entityType) && flagContainer
                    .getFlag(VillagerInteractFlag.class).getValue()) {
                return InteractionResult.PASS;
            }

            if ((EntityCategories.HANGING.contains(entityType) || EntityCategories.OTHER
                    .contains(entityType)) && flagContainer.getFlag(MiscInteractFlag.class)
                    .getValue()) {
                return InteractionResult.PASS;
            }

            if (!pp.hasPermission(Permission.PERMISSION_ADMIN_INTERACT_OTHER)) {
                pp.sendMessage(
                        TranslatableCaption.of("permission.no_permission_event"),
                        TagResolver.resolver(
                                "node",
                                Tag.inserting(Permission.PERMISSION_ADMIN_INTERACT_OTHER)
                        )
                );
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onVehicleDestroy(LivingEntity entity, DamageSource damageSource) {
        Location location = FabricUtil.adapt(GlobalPos.of(entity.level().dimension(), entity.blockPosition()));
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        }
        Entity attacker = damageSource.getEntity();
        if (attacker instanceof ServerPlayer p) {
            FabricPlayer pp = FabricUtil.adapt(p);
            Plot plot = area.getPlot(location);
            if (plot == null) {
                if (!PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, VehicleBreakFlag.class, true) && !pp.hasPermission(
                        Permission.PERMISSION_ADMIN_DESTROY_VEHICLE_ROAD
                )) {
                    pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_DESTROY_VEHICLE_ROAD)
                            )
                    );
                    return InteractionResult.FAIL;
                }
            } else {
                if (!plot.hasOwner()) {
                    if (!pp.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_VEHICLE_UNOWNED)) {
                        pp.sendMessage(
                                TranslatableCaption.of("permission.no_permission_event"),
                                TagResolver.resolver(
                                        "node",
                                        Tag.inserting(Permission.PERMISSION_ADMIN_DESTROY_VEHICLE_UNOWNED)
                                )
                        );
                        return InteractionResult.FAIL;
                    }
                    return InteractionResult.PASS;
                }
                if (!plot.isAdded(pp.getUUID())) {
                    if (plot.getFlag(VehicleBreakFlag.class)) {
                        return InteractionResult.PASS;
                    }
                    if (!pp.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_VEHICLE_OTHER)) {
                        pp.sendMessage(
                                TranslatableCaption.of("permission.no_permission_event"),
                                TagResolver.resolver(
                                        "node",
                                        Tag.inserting(Permission.PERMISSION_ADMIN_DESTROY_VEHICLE_OTHER)
                                )
                        );
                        plot.debug(pp.getName()
                                + " could not break vehicle because vehicle-break = false");
                        return InteractionResult.FAIL;
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onItemDrop(ServerPlayer serverPlayer, int Slot, ItemStack itemStack) {
        ServerPlayer player = serverPlayer;
        FabricPlayer pp = FabricUtil.adapt(player);
        Location location = pp.getLocation();
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        }
        Plot plot = location.getOwnedPlot();
        if (plot == null) {
            if (PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, ItemDropFlag.class, false)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }
        UUID uuid = pp.getUUID();
        if (!plot.isAdded(uuid)) {
            if (!plot.getFlag(ItemDropFlag.class)) {
                plot.debug(player.getName() + " could not drop item because of item-drop = false");
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onItemPickup(ServerPlayer player, ItemEntity item, ItemStack itemStack) {
        FabricPlayer pp = FabricUtil.adapt(player);
        Location location = pp.getLocation();
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        }
        Plot plot = location.getOwnedPlot();
        if (plot == null) {
            if (PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, DropProtectionFlag.class, true)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }
        UUID uuid = pp.getUUID();
        if (!plot.isAdded(uuid) && plot.getFlag(DropProtectionFlag.class)) {
            plot.debug(player.getName() + " could not pick up item because of drop-protection = true");
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onDeath(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
        Location location = FabricUtil.adapt(GlobalPos.of(newPlayer.serverLevel().dimension(), newPlayer.blockPosition()));
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        }
        Plot plot = location.getOwnedPlot();
        if (plot == null) {
            if (PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, KeepInventoryFlag.class, true)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }
        if (plot.getFlag(KeepInventoryFlag.class)) {
            plot.debug(newPlayer.getName() + " kept their inventory because of keep-inventory = true");
            newPlayer.getInventory().replaceWith(oldPlayer.getInventory());
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    /*
    @SuppressWarnings("deprecation") // #getLocate is needed for Spigot compatibility
    public void onLocaleChange(final PlayerLocaleChangeEvent event) {
        // The event is fired before the player is deemed online upon login
        if (!event.getPlayer().isOnline()) {
            return;
        }
        FabricPlayer player = FabricUtil.adapt(event.getPlayer());
        // we're stripping the country code as we don't want to differ between countries
        player.setLocale(Locale.forLanguageTag(event.getLocale().substring(0, 2)));
    }*/

    public InteractionResult onPortalEnter(BlockPos blockPos, Entity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            Location location = FabricUtil.adapt(GlobalPos.of(entity.level().dimension(), entity.blockPosition()));
            PlotArea area = location.getPlotArea();
            if (area == null) {
                return InteractionResult.PASS;
            }
            Plot plot = location.getOwnedPlot();
            if (plot == null) {
                if (PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, DenyPortalTravelFlag.class, true)) {
                    return InteractionResult.FAIL;
                }
                return InteractionResult.PASS;
            }
            if (plot.getFlag(DenyPortalTravelFlag.class)) {
                plot.debug(serverPlayer.getName() + " did not travel thru a portal because of deny-portal-travel = true");
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onNetherPortalCreation(
            LevelAccessor levelAccessor,
            BlockPos blockPos,
            Direction.Axis axis,
            Operation<Optional<PortalShape>> original
    ) {
        if (levelAccessor instanceof ServerLevel serverLevel) {
            Location location = FabricUtil.adapt(GlobalPos.of(serverLevel.dimension(), blockPos));
            PlotArea area = location.getPlotArea();
            if (area == null) {
                return InteractionResult.PASS;
            }
            Plot plot = location.getOwnedPlot();
            if (plot == null) {
                if (PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, DenyPortalsFlag.class, true)) {
                    return InteractionResult.FAIL;
                }
                return InteractionResult.PASS;
            }
            if (plot.getFlag(DenyPortalsFlag.class)) {
                StringBuilder builder = new StringBuilder();
                if (serverLevel.getServer().getPlayerList().getPlayer(plot.getOwner()) != null) {
                    builder.append(serverLevel.getServer().getPlayerList().getPlayer(plot.getOwner()).getName()).append(
                            " did not create a portal");
                } else {
                    builder.append("Portal creation cancelled");
                }
                plot.debug(builder.append(" because of deny-portals = true").toString());
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }
        return InteractionResult.PASS;

    }

    public InteractionResult onEndPortalCreation(UseOnContext useOnContext, BlockPattern.BlockPatternMatch blockPatternMatch) {
        String world =
                useOnContext
                        .getPlayer()
                        .getServer()
                        .getLevel(useOnContext.getLevel().dimension())
                        .dimension()
                        .location()
                        .getPath()
                        .toString();
        if (PlotSquared.get().getPlotAreaManager().getPlotAreasSet(world).size() == 0) {
            return InteractionResult.FAIL;
        }
        FabricPlayer pp = (useOnContext.getPlayer() instanceof ServerPlayer player) ? FabricUtil.adapt(player) : null;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockInWorld state : blockPatternMatch.cache.asMap().values()) {
            minX = Math.min(state.getPos().getX(), minX);
            maxX = Math.max(state.getPos().getX(), maxX);
            minZ = Math.min(state.getPos().getZ(), minZ);
            maxZ = Math.max(state.getPos().getZ(), maxZ);
        }
        int y = blockPatternMatch.cache
                .asMap()
                .values()
                .stream()
                .findFirst()
                .get()
                .getPos()
                .getY(); // Don't need to worry about this
        // too much
        for (Location location : List.of( // We don't care about duplicate locations
                Location.at(world, minX, y, minZ),
                Location.at(world, minX, y, maxZ),
                Location.at(world, maxX, y, minZ),
                Location.at(world, maxX, y, maxZ)
        )) {
            PlotArea area = location.getPlotArea();
            if (area == null) {
                continue;
            }
            if (area.notifyIfOutsideBuildArea(pp, location.getY())) {
                return InteractionResult.FAIL;
            }
            Plot plot = location.getOwnedPlot();
            if (plot == null) {
                if (PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, DenyPortalsFlag.class, true)) {
                    return InteractionResult.FAIL;
                }
                continue;
            }
            if (plot.getFlag(DenyPortalsFlag.class)) {
                StringBuilder builder = new StringBuilder();
                if (useOnContext.getPlayer() != null) {
                    builder.append(useOnContext.getPlayer().getName()).append(" did not create a portal");
                } else {
                    builder.append("Portal creation cancelled");
                }
                plot.debug(builder.append(" because of deny-portals = true").toString());
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onPlayerTakeLecternBook(
            ServerboundContainerButtonClickPacket serverboundContainerButtonClickPacket,
            ServerPlayer player
    ) {
        FabricPlayer pp = FabricUtil.adapt(player);
        Location location = pp.getLocation();
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        }
        Plot plot = location.getOwnedPlot();
        if (plot == null) {
            if (PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, LecternReadBookFlag.class, true)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }
        if (!plot.isAdded(pp.getUUID())) {
            if (plot.getFlag(LecternReadBookFlag.class)) {
                plot.debug(player.getName() + " could not take the book because of lectern-read-book = true");
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

}
