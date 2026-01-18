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
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.plotsquared.core.command.Command;
import com.plotsquared.core.command.MainCommand;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.flag.FlagContainer;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import com.plotsquared.core.plot.flag.implementations.TileDropFlag;
import com.plotsquared.core.plot.flag.types.BooleanFlag;
import com.plotsquared.core.plot.world.PlotAreaManager;
import com.plotsquared.fabric.listener.event.EntityPathfindEvent;
import com.plotsquared.fabric.listener.event.MobSpawnEvent;
import com.plotsquared.fabric.listener.event.ReceiveCommandSuggestionsPacketEvent;
import com.plotsquared.fabric.util.CommandSuggestionProvider;
import com.plotsquared.fabric.util.FabricUtil;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.nucleoid.stimuli.Stimuli;
import xyz.nucleoid.stimuli.event.block.BlockDropItemsEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Events specific to Paper. Some toit nups here
 */
@SuppressWarnings("unused")
public class PaperListener {

    private final PlotAreaManager plotAreaManager;
    //private Chunk lastChunk;

    @Inject
    public PaperListener(final @NonNull PlotAreaManager plotAreaManager) {
        this.plotAreaManager = plotAreaManager;
        Stimuli.global().listen(BlockDropItemsEvent.EVENT, this::onBlockDropItems);
        EntityPathfindEvent.EVENT.register(this::onEntityPathfind);
        MobSpawnEvent.EVENT.register(this::onPreCreatureSpawnEvent);
        ReceiveCommandSuggestionsPacketEvent.EVENT.register(this::onAsyncTabCompletion);


    }


    public InteractionResultHolder<List<ItemStack>> onBlockDropItems(
            Entity entity, ServerLevel serverLevel, BlockPos blockPos,
            BlockState blockState,
            List<ItemStack> dropStacks
    ) {
        Location location = FabricUtil.adapt(GlobalPos.of(serverLevel.dimension(), blockPos));
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResultHolder.pass(dropStacks);
        }
        Plot plot = area.getPlot(location);
        if (plot != null) {
            if (plot.getFlag(TileDropFlag.class)) {
                return InteractionResultHolder.pass(dropStacks);
            }
            return InteractionResultHolder.fail(List.of());
        }
        return InteractionResultHolder.pass(dropStacks);
    }

    public InteractionResult onEntityPathfind(Entity entity, BlockPos blockPos, LevelReader levelReader) {

        if (!Settings.Paper_Components.ENTITY_PATHING) {
            return InteractionResult.PASS;
        }
        Location toLoc = FabricUtil.adapt(GlobalPos.of(entity.level().dimension(), blockPos));
        Location fromLoc = FabricUtil.adapt(GlobalPos.of(entity.level().dimension(), entity.blockPosition()));
        PlotArea tarea = toLoc.getPlotArea();
        if (tarea == null) {
            return InteractionResult.PASS;
        }
        PlotArea farea = fromLoc.getPlotArea();
        if (farea == null) {
            return InteractionResult.PASS;
        }
        if (tarea != farea) {
            return InteractionResult.FAIL;
        }
        Plot tplot = toLoc.getPlot();
        Plot fplot = fromLoc.getPlot();
        if (tplot == null ^ fplot == null) {
            return InteractionResult.FAIL;
        }
        if (tplot == null || tplot.getId().hashCode() == fplot.getId().hashCode()) {
            return InteractionResult.PASS;
        }
        if (fplot.isMerged() && fplot.getConnectedPlots().contains(fplot)) {
            return InteractionResult.PASS;
        }
        return InteractionResult.FAIL;
    }
    /*
    public InteractionResult onSlimePathfind(Entity entity, BlockPos blockPos, LevelReader levelReader) {
        if (!Settings.Paper_Components.ENTITY_PATHING) {
            return InteractionResult.PASS;
        }
        Slime slime = (Slime) entity;

        BlockPos b = slime.tra;
        if (b == null) {
            return InteractionResult.PASS;
        }

        Location toLoc = FabricUtil.adapt(GlobalPos.of(entity.level().dimension(), blockPos));
        Location fromLoc = FabricUtil.adapt(GlobalPos.of(entity.level().dimension(), entity.blockPosition()));
        PlotArea tarea = toLoc.getPlotArea();
        if (tarea == null) {
            return InteractionResult.PASS;
        }
        PlotArea farea = fromLoc.getPlotArea();
        if (farea == null) {
            return InteractionResult.PASS;
        }

        if (tarea != farea) {
            return InteractionResult.FAIL;
        }
        Plot tplot = toLoc.getPlot();
        Plot fplot = fromLoc.getPlot();
        if (tplot == null ^ fplot == null) {
            return InteractionResult.FAIL;
        }
        if (tplot == null || tplot.getId().hashCode() == fplot.getId().hashCode()) {
            return InteractionResult.PASS;
        }
        if (fplot.isMerged() && fplot.getConnectedPlots().contains(fplot)) {
            return InteractionResult.PASS;
        }
        return InteractionResult.FAIL;
    }*/

    public InteractionResult onPreCreatureSpawnEvent(
            ServerLevelAccessor serverLevelAccessor,
            DifficultyInstance difficultyInstance,
            MobSpawnType mobSpawnType,
            SpawnGroupData spawnGroupData,
            Mob mob
    ) {
        if (!Settings.Paper_Components.CREATURE_SPAWN) {
            return InteractionResult.PASS;
        }
        Location location = FabricUtil.adapt(GlobalPos.of(mob.level().dimension(), mob.blockPosition()));
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        }
        // Armour-stands are handled elsewhere and should not be handled by area-wide entity-spawn options
        if (mob.getType() == EntityType.ARMOR_STAND) {
            return InteractionResult.PASS;
        }
        // If entities are spawning... the chunk should be loaded?
        Entity[] entities =
                FabricUtil.getEntitiesInChunk(
                        serverLevelAccessor.getLevel(),
                        serverLevelAccessor.getLevel().getChunkAt(mob.blockPosition())
                ).toArray(new Entity[0]);
        if (entities.length >= Settings.Chunk_Processor.MAX_ENTITIES) {
            ((Entity) mob).remove(Entity.RemovalReason.DISCARDED);
            return InteractionResult.FAIL;
        }
        switch (mobSpawnType.name().toUpperCase()) {
            case "DISPENSE_EGG", "EGG", "OCELOT_BABY", "SPAWNER_EGG" -> {
                if (!area.isSpawnEggs()) {
                    ((Entity) mob).remove(Entity.RemovalReason.DISCARDED);
                    return InteractionResult.FAIL;
                }
            }
            case "REINFORCEMENTS", "NATURAL", "MOUNT", "PATROL", "RAID", "SHEARED", "SILVERFISH_BLOCK", "ENDER_PEARL", "TRAP",
                 "VILLAGE_DEFENSE", "VILLAGE_INVASION", "BEEHIVE", "CHUNK_GEN" -> {
                if (!area.isMobSpawning()) {
                    ((Entity) mob).remove(Entity.RemovalReason.DISCARDED);
                    return InteractionResult.FAIL;
                }
            }
            case "BREEDING" -> {
                if (!area.isSpawnBreeding()) {
                    ((Entity) mob).remove(Entity.RemovalReason.DISCARDED);
                    return InteractionResult.FAIL;
                }
            }
            case "BUILD_IRONGOLEM", "BUILD_SNOWMAN", "BUILD_WITHER", "CUSTOM" -> {
                if (!area.isSpawnCustom()) {
                    ((Entity) mob).remove(Entity.RemovalReason.DISCARDED);
                    return InteractionResult.FAIL;
                }
            }
            case "SPAWNER" -> {
                if (!area.isMobSpawnerSpawning()) {
                    ((Entity) mob).remove(Entity.RemovalReason.DISCARDED);
                    return InteractionResult.FAIL;
                }
            }
        }
        Plot plot = location.getOwnedPlotAbs();
        if (plot == null) {
            EntityType<?> type = mob.getType();
            // PreCreatureSpawnEvent **should** not be called for DROPPED_ITEM, just for the sake of consistency
            if (type == EntityType.ITEM) {
                if (Settings.Enabled_Components.KILL_ROAD_ITEMS) {
                    ((Entity) mob).remove(Entity.RemovalReason.DISCARDED);
                    return InteractionResult.FAIL;
                }
                return InteractionResult.PASS;
            }
            if (!area.isMobSpawning()) {
                if (type == EntityType.PLAYER) {
                    return InteractionResult.PASS;
                }
                if (mob instanceof LivingEntity) {
                    ((Entity) mob).remove(Entity.RemovalReason.DISCARDED);
                    return InteractionResult.FAIL;
                }
            }
            if (!area.isMiscSpawnUnowned() && !(mob instanceof LivingEntity)) {
                ((Entity) mob).remove(Entity.RemovalReason.DISCARDED);
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }
        if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot)) {
            ((Entity) mob).remove(Entity.RemovalReason.DISCARDED);
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    /*

    public InteractionResult onPlayerNaturallySpawnCreaturesEvent(PlayerNaturallySpawnCreaturesEvent event) {
        if (Settings.Paper_Components.CANCEL_CHUNK_SPAWN) {
            Location location = FabricUtil.adapt(event.getPlayer().getLocation());
            PlotArea area = location.getPlotArea();
            if (area != null && !area.isMobSpawning()) {
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }


    public void onPreSpawnerSpawnEvent(PreSpawnerSpawnEvent event) {
        if (Settings.Paper_Components.SPAWNER_SPAWN) {
            Location location = FabricUtil.adapt(event.getSpawnerLocation());
            PlotArea area = location.getPlotArea();
            if (area != null && !area.isMobSpawnerSpawning()) {
                return InteractionResult.FAIL;
                event.setShouldAbortSpawn(true);
            }
        }
    }

    public void onBlockPlace(BlockPlaceEvent event) {
        if (!Settings.Paper_Components.TILE_ENTITY_CHECK || !Settings.Enabled_Components.CHUNK_PROCESSOR) {
            return InteractionResult.PASS;
        }
        if (!(event.getBlock().getState(false) instanceof TileState)) {
            return InteractionResult.PASS;
        }
        final Location location = FabricUtil.adapt(event.getBlock().getLocation());
        final PlotArea plotArea = location.getPlotArea();
        if (plotArea == null) {
            return InteractionResult.PASS;
        }
        final int tileEntityCount = event.getBlock().getChunk().getTileEntities(false).length;
        if (tileEntityCount >= Settings.Chunk_Processor.MAX_TILES) {
            final PlotPlayer<?> plotPlayer = FabricUtil.adapt(event.getPlayer());
            plotPlayer.sendMessage(
                    TranslatableCaption.of("errors.tile_entity_cap_reached"),
                    TagResolver.resolver("amount", Tag.inserting(Component.text(Settings.Chunk_Processor.MAX_TILES)))
            );
            return InteractionResult.FAIL;
            event.setBuild(false);
        }
    }

    /**
     * Unsure if this will be any performance improvement over the spigot version,
     * but here it is anyway :)
     *
     * @param event Paper's PlayerLaunchProjectileEvent
     *

    public void onProjectileLaunch(PlayerLaunchProjectileEvent event) {
        if (!Settings.Paper_Components.PLAYER_PROJECTILE) {
            return InteractionResult.PASS;
        }
        Projectile entity = event.getProjectile();
        ProjectileSource shooter = entity.getShooter();
        if (!(shooter instanceof Player)) {
            return InteractionResult.PASS;
        }
        Location location = FabricUtil.adapt(entity.getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        }
        PlotPlayer<Player> pp = FabricUtil.adapt((Player) shooter);
        Plot plot = location.getOwnedPlot();

        if (plot == null) {
            if (!PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, ProjectilesFlag.class, true) && !pp.hasPermission(
                    Permission.PERMISSION_ADMIN_PROJECTILE_ROAD
            )) {
                pp.sendMessage(
                        TranslatableCaption.of("permission.no_permission_event"),
                        TagResolver.resolver(
                                "node",
                                Tag.inserting(Permission.PERMISSION_ADMIN_PROJECTILE_ROAD)
                        )
                );
                entity.remove();
                return InteractionResult.FAIL;
            }
        } else if (!plot.hasOwner()) {
            if (!pp.hasPermission(Permission.PERMISSION_ADMIN_PROJECTILE_UNOWNED)) {
                pp.sendMessage(
                        TranslatableCaption.of("permission.no_permission_event"),
                        TagResolver.resolver(
                                "node",
                                Tag.inserting(Permission.PERMISSION_ADMIN_PROJECTILE_UNOWNED)
                        )
                );
                entity.remove();
                return InteractionResult.FAIL;
            }
        } else if (!plot.isAdded(pp.getUUID())) {
            if (entity.getType().equals(EntityType.FISHING_HOOK)) {
                if (plot.getFlag(FishingFlag.class)) {
                    return InteractionResult.PASS;
                }
            }
            if (!plot.getFlag(ProjectilesFlag.class)) {
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_PROJECTILE_OTHER)) {
                    pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_PROJECTILE_OTHER)
                            )
                    );
                    entity.remove();
                    return InteractionResult.FAIL;
                }
            }
        }
    }
*/
    public InteractionResult onAsyncTabCompletion(
            ServerboundCommandSuggestionPacket serverboundCommandSuggestionPacket,
            ServerPlayer serverPlayer
    ) {
        /*if (!Settings.Paper_Components.ASYNC_TAB_COMPLETION) {
            return InteractionResult.PASS;
        }*/
        String buffer = serverboundCommandSuggestionPacket.getCommand();
       /* if (!(event.getSender() instanceof ServerPlayer)) {
            return InteractionResult.PASS;
        }*/
        if ((!buffer.startsWith("/")) || buffer.indexOf(' ') == -1) {
            return InteractionResult.PASS;
        }
        if (buffer.startsWith("/")) {
            buffer = buffer.substring(1);
        }
        final String[] unprocessedArgs = buffer.split(Pattern.quote(" "));
        if (unprocessedArgs.length == 1) {
            return InteractionResult.PASS; // We don't do anything in this case
        } else if (!Settings.Enabled_Components.TAB_COMPLETED_ALIASES
                .contains(unprocessedArgs[0].toLowerCase(Locale.ENGLISH))) {
            return InteractionResult.PASS;
        }
        String[] args = new String[unprocessedArgs.length - 1];
        System.arraycopy(unprocessedArgs, 1, args, 0, args.length);
        if (buffer.endsWith(" ")) {
            args = new String[unprocessedArgs.length];
            System.arraycopy(unprocessedArgs, 1, args, 0, args.length - 1);
            args[args.length - 1] = "";
        }
        try {
            final PlotPlayer<?> player = FabricUtil.adapt(serverPlayer);
            final Collection<Command> objects = MainCommand.getInstance().tab(player, args, buffer.endsWith(" "));
            if (objects == null) {
                return InteractionResult.PASS;
            }
            final List<String> result = new ArrayList<>();
            for (final Command o : objects) {
                result.add(o.toString());
            }

            StringReader stringReader = new StringReader(serverboundCommandSuggestionPacket.getCommand());
            if (stringReader.canRead() && stringReader.peek() == '/') {
                stringReader.skip();
            }
            ParseResults<CommandSourceStack> parseResults = serverPlayer.server.getCommands().getDispatcher().parse(
                    stringReader,
                    serverPlayer.server.createCommandSourceStack()
            );

            final String finalBuffer = buffer;
            final String[] finalArgs = args;
            serverPlayer.server.getCommands().getDispatcher().getCompletionSuggestions(parseResults).thenAccept((suggestions) -> {
                Suggestions replacements =
                        new Suggestions(
                                new StringRange(
                                        finalBuffer.length() - finalArgs[finalArgs.length - 1].length() + 1,
                                        finalBuffer.length() + 1
                                ),
                                new ArrayList<>()
                        );
                result.forEach(s -> {
                    if (s.startsWith(finalArgs[finalArgs.length - 1])) {
                        replacements.getList().add(new Suggestion(
                                StringRange.between(
                                        finalBuffer.length() - finalArgs[finalArgs.length - 1].length(),
                                        finalBuffer.length() + 1
                                ), s
                        ));
                    }
                });
                serverPlayer.connection.send(new ClientboundCommandSuggestionsPacket(
                        serverboundCommandSuggestionPacket.getId(),
                        replacements
                ));
            });

        } catch (final Exception ignored) {
        }
        return InteractionResult.PASS;
    }

    /*
        @EventHandler(ignoreCancelled = true)
        public void onBeaconEffect(final BeaconEffectEvent event) {
            Block block = event.getBlock();
            Location beaconLocation = FabricUtil.adapt(block.getLocation());
            Plot beaconPlot = beaconLocation.getPlot();

            PlotArea area = beaconLocation.getPlotArea();
            if (area == null) {
                return InteractionResult.PASS;
            }

            ServerPlayer player = event.getPlayer();
            Location playerLocation = FabricUtil.adapt(player.getLocation());

            PlotPlayer<ServerPlayer> plotPlayer = FabricUtil.adapt(player);
            Plot playerStandingPlot = playerLocation.getPlot();
            if (playerStandingPlot == null) {
                FlagContainer container = area.getRoadFlagContainer();
                if (!getBooleanFlagValue(container, BeaconEffectsFlag.class, true) ||
                        (beaconPlot != null && Settings.Enabled_Components.DISABLE_BEACON_EFFECT_OVERFLOW)) {
                    return InteractionResult.FAIL;
                }
                return InteractionResult.PASS;
            }

            FlagContainer container = playerStandingPlot.getFlagContainer();
            boolean plotBeaconEffects = getBooleanFlagValue(container, BeaconEffectsFlag.class, true);
            if (playerStandingPlot.equals(beaconPlot)) {
                if (!plotBeaconEffects) {
                    return InteractionResult.FAIL;
                }
                return InteractionResult.PASS;
            }

            if (!plotBeaconEffects || Settings.Enabled_Components.DISABLE_BEACON_EFFECT_OVERFLOW) {
                return InteractionResult.FAIL;
            }
        }
    */
    private boolean getBooleanFlagValue(
            @NonNull FlagContainer container,
            @NonNull Class<? extends BooleanFlag<?>> flagClass,
            boolean defaultValue
    ) {
        BooleanFlag<?> flag = container.getFlag(flagClass);
        return flag == null ? defaultValue : flag.getValue();
    }

}
