package com.plotsquared.fabric.player;

import com.google.common.base.Charsets;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.events.TeleportCause;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.permissions.Permission;
import com.plotsquared.core.permissions.PermissionHandler;
import com.plotsquared.core.player.ConsolePlayer;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.PlotWeather;
import com.plotsquared.core.plot.world.PlotAreaManager;
import com.plotsquared.core.util.EventDispatcher;
import com.plotsquared.core.util.MathMan;
import com.plotsquared.core.util.WorldUtil;
import com.plotsquared.fabric.FabricPlatform;
import com.plotsquared.fabric.util.FabricUtil;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.luckperms.api.LuckPermsProvider;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Map;
import java.util.UUID;

import static com.sk89q.worldedit.world.gamemode.GameModes.ADVENTURE;
import static com.sk89q.worldedit.world.gamemode.GameModes.CREATIVE;
import static com.sk89q.worldedit.world.gamemode.GameModes.SPECTATOR;
import static com.sk89q.worldedit.world.gamemode.GameModes.SURVIVAL;

public class FabricPlayer extends PlotPlayer<ServerPlayer> {

    private static boolean CHECK_EFFECTIVE = true;
    public final UUID uuid;
    private String name;

    public PlotWeather weather = PlotWeather.OFF;
    public long time = Long.MIN_VALUE;

    /**
     * @param plotAreaManager   PlotAreaManager instance
     * @param eventDispatcher   EventDispatcher instance
     * @param player            Bukkit player instance
     * @param permissionHandler PermissionHandler instance
     */
    FabricPlayer(
            final @NonNull PlotAreaManager plotAreaManager,
            final @NonNull EventDispatcher eventDispatcher,
            final @NonNull ServerPlayer player,
            final boolean realPlayer,
            final @NonNull PermissionHandler permissionHandler
    ) {
        super(plotAreaManager, eventDispatcher, permissionHandler);
        this.uuid = player.getUUID();
        this.setupPermissionProfile();
        if (realPlayer) {
            super.populatePersistentMetaMap();
        }


        ServerTickEvents.END_WORLD_TICK.register(server -> {
            //handle weather

            if (this.getPlatformPlayer() != null) {
                switch (weather) {
                    case CLEAR -> {
                        this.getPlatformPlayer().connection.send(new ClientboundGameEventPacket(
                                ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,
                                0.0F
                        ));
                    }
                    case RAIN -> {
                        this.getPlatformPlayer().connection.send(new ClientboundGameEventPacket(
                                ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,
                                1.0F
                        ));
                    }
                    case WORLD -> {
                        this.getPlatformPlayer().connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE
                                , this.getPlatformPlayer().serverLevel().rainLevel));
                    }
                    default -> {
                        this.getPlatformPlayer().connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE
                                , this.getPlatformPlayer().serverLevel().rainLevel));
                    }
                }
                //handle time
                if (time != Long.MIN_VALUE && time != Long.MAX_VALUE) {
                    this.getPlatformPlayer().connection.send(new ClientboundSetTimePacket(time, time, true));

                } else {
                    this.getPlatformPlayer().connection.send(new ClientboundSetTimePacket(
                            this.getPlatformPlayer().serverLevel().getGameTime(),
                            this.getPlatformPlayer().serverLevel().dayTime(),
                            this.getPlatformPlayer().serverLevel().getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)
                    ));
                }
            }
        });
    }

    @Override
    public Actor toActor() {
        return FabricAdapter.adaptPlayer(this.getPlatformPlayer());
    }

    @Override
    public ServerPlayer getPlatformPlayer() {
        return FabricPlatform.SERVER.getPlayerList().getPlayer(this.uuid);
    }

    @NonNull
    @Override
    public UUID getUUID() {
        if (Settings.UUID.OFFLINE) {
            if (Settings.UUID.FORCE_LOWERCASE) {
                return UUID.nameUUIDFromBytes(("OfflinePlayer:" +
                        getName().toLowerCase()).getBytes(Charsets.UTF_8));
            } else {
                return UUID.nameUUIDFromBytes(("OfflinePlayer:" +
                        getName()).getBytes(Charsets.UTF_8));
            }
        }
        return this.uuid;
    }

    @Override
    @NonNegative
    public long getLastPlayed() {
        return this.getPlatformPlayer().getLastActionTime();
    }

    @Override
    public boolean canTeleport(final @NonNull Location location) {
        return WorldUtil.isValidLocation(location);
    }

    /*
        private void callEvent(final @NonNull Event event) {
            final RegisteredListener[] listeners = event.getHandlers().getRegisteredListeners();
            for (final RegisteredListener listener : listeners) {
                if (listener.getPlugin().getName().equals(PlotSquared.platform().pluginName())) {
                    continue;
                }
                try {
                    listener.callEvent(event);
                } catch (final EventException e) {
                    e.printStackTrace();
                }
            }
        }
    */
    @SuppressWarnings("StringSplitter")
    @Override
    @NonNegative
    public int hasPermissionRange(
            final @NonNull String stub,
            @NonNegative final int range
    ) {
        if (hasPermission(Permission.PERMISSION_ADMIN.toString())) {
            return Integer.MAX_VALUE;
        }
        final String[] nodes = stub.split("\\.");
        final StringBuilder n = new StringBuilder();
        // Wildcard check from less specific permission to more specific permission
        for (int i = 0; i < (nodes.length - 1); i++) {
            n.append(nodes[i]).append(".");
            if (!stub.equals(n + Permission.PERMISSION_STAR.toString())) {
                if (hasPermission(n + Permission.PERMISSION_STAR.toString())) {
                    return Integer.MAX_VALUE;
                }
            }
        }
        // Wildcard check for the full permission
        if (hasPermission(stub + ".*")) {
            return Integer.MAX_VALUE;
        }
        // Permission value cache for iterative check
        int max = 0;
        if (CHECK_EFFECTIVE) {
            boolean hasAny = false;
            String stubPlus = stub + ".";

            final Map<String, Boolean> effective =
                    LuckPermsProvider
                            .get()
                            .getPlayerAdapter(ServerPlayer.class)
                            .getUser(this.getPlatformPlayer())
                            .getCachedData()
                            .getPermissionData()
                            .getPermissionMap();
            if (!effective.isEmpty()) {
                for (String attach : effective.keySet()) {
                    // Ignore all "false" permissions
                    if (!effective.get(attach)) {
                        continue;
                    }
                    String permStr = attach;
                    if (permStr.startsWith(stubPlus)) {
                        hasAny = true;
                        String end = permStr.substring(stubPlus.length());
                        if (MathMan.isInteger(end)) {
                            int val = Integer.parseInt(end);
                            if (val > range) {
                                return val;
                            }
                            if (val > max) {
                                max = val;
                            }
                        }
                    }
                }
                if (hasAny) {
                    return max;
                }
                // Workaround
                for (String attach : effective.keySet()) {
                    String permStr = attach;
                    if (permStr.startsWith("plots.") && !permStr.equals("plots.use")) {
                        return max;
                    }
                }
                CHECK_EFFECTIVE = false;
            }
        }
        for (int i = range; i > 0; i--) {
            if (hasPermission(stub + "." + i)) {
                return i;
            }
        }
        return max;
    }

    @Override
    public void teleport(final @NonNull Location location, final @NonNull TeleportCause cause) {
        /*if (!WorldUtil.isValidLocation(location)) {
            return;
        }*/
        this.getPlatformPlayer().teleportTo(FabricUtil.getWorld(location.getWorldName()), location.getX() + 0.5, location.getY(),
                location.getZ() + 0.5, location.getYaw(), location.getPitch()
        );
    }

    @Override
    public String getName() {
        if (this.name == null) {
            this.name = this.getPlatformPlayer().getName().getString();
        }
        return this.name;
    }

    @Override
    public void setCompassTarget(Location location) {
        /* TODO FIGURE OUT IMPLEMENTATION */
        /*
        CompassItemPropertyFunction
        this.getPlatformPlayer().setCompassTarget(
                new org.bukkit.Location(BukkitUtil.getWorld(location.getWorldName()), location.getX(),
                        location.getY(), location.getZ()
                ));*/
    }

    @Override
    public Location getLocationFull() {
        return FabricUtil.adaptComplete(GlobalPos.of(
                        this.getPlatformPlayer().level().dimension(),
                        this.getPlatformPlayer().blockPosition()
                ),
                this.getPlatformPlayer().getXRot(), this.getPlatformPlayer().getYRot()
        );
    }

    @Override
    public void setWeather(final @NonNull PlotWeather weather) {
        this.weather = weather;

    }


    @Override
    public com.sk89q.worldedit.world.gamemode.GameMode getGameMode() {
        return switch (this.getPlatformPlayer().gameMode.getGameModeForPlayer()) {
            case ADVENTURE -> ADVENTURE;
            case CREATIVE -> CREATIVE;
            case SPECTATOR -> SPECTATOR;
            default -> SURVIVAL;
        };
    }

    @Override
    public void setGameMode(final com.sk89q.worldedit.world.gamemode.GameMode gameMode) {
        if (ADVENTURE.equals(gameMode)) {
            this.getPlatformPlayer().setGameMode(GameType.ADVENTURE);
        } else if (CREATIVE.equals(gameMode)) {
            this.getPlatformPlayer().setGameMode(GameType.CREATIVE);
        } else if (SPECTATOR.equals(gameMode)) {
            this.getPlatformPlayer().setGameMode(GameType.SPECTATOR);
        } else {
            this.getPlatformPlayer().setGameMode(GameType.SURVIVAL);
        }
    }

    @Override
    public void setTime(final long time) {
        this.time = time;
    }

    @Override
    public boolean getFlight() {
        return this.getPlatformPlayer().getAbilities().flying;
    }

    @Override
    public void setFlight(boolean fly) {
        this.getPlatformPlayer().getAbilities().mayfly = fly;
        if (!fly) {
            this.getPlatformPlayer().getAbilities().flying = false;
        }
        this.getPlatformPlayer().onUpdateAbilities();
    }

    @Override
    public void playMusic(final @NonNull Location location, final @NonNull ItemType id) {
        if (id == ItemTypes.AIR) {
            if (PlotSquared.platform().serverVersion()[1] >= 19) {
                this.getPlatformPlayer().stopSound(SoundStop.source(Sound.Source.MUSIC));
                return;
            }
            // 1.18 and downwards require a specific Sound to stop (even tho the packet does not??)
            for (final Sound.Source sound : Sound.Source.values()) {
                if (sound.name().startsWith("MUSIC_DISC")) {
                    this.getPlatformPlayer().stopSound(SoundStop.source(Sound.Source.MUSIC));
                }
            }
            return;
        }

        try {
            Sound sound = Sound.sound(Key.key(id.getId().replace(
                    "music_disc_",
                    "music_disc."
            )), Sound.Source.MUSIC, 1f, 1f);
            this.getPlatformPlayer().playSound(sound, Sound.Emitter.self());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation") // Needed for Spigot compatibility
    @Override
    public void kick(final String message) {
        this.getPlatformPlayer().connection.disconnect(Component.translatable(message));
    }

    @Override
    public void stopSpectating() {
        if (getGameMode() == SPECTATOR) {
            this.getPlatformPlayer().setCamera(this.getPlatformPlayer().getCamera());
        }
    }

    @Override
    public boolean isBanned() {
        return this.getPlatformPlayer().server.getPlayerList().getBans().isBanned(this.getPlatformPlayer().getGameProfile());
    }

    @Override
    public @NonNull Audience getAudience() {
        return FabricUtil.FABRIC_AUDIENCES.player(this.getPlatformPlayer().getUUID());
    }

    @Override
    public void removeEffect(@NonNull String name) {
        MobEffect type = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.parse(name));
        if (type != null) {
            this.getPlatformPlayer().removeEffect(Holder.direct(type));
        }
    }

    @Override
    public boolean canSee(final PlotPlayer<?> other) {
        if (other instanceof ConsolePlayer) {
            return true;
        } else {
            return (((FabricPlayer) other).getPlatformPlayer().isInvisibleTo(this.getPlatformPlayer()));
        }
    }


    /**
     * Convert from PlotSquared's {@link TeleportCause} to Fabric's
     *
     * @param cause PlotSquared teleport cause to convert
     * @return Bukkit's equivalent teleport cause
     */
   /* public PlayerTeleportEvent.TeleportCause getTeleportCause(final @NonNull TeleportCause cause) {
        if (TeleportCause.CauseSets.COMMAND.contains(cause)) {
            return PlayerTeleportEvent.TeleportCause.COMMAND;
        } else if (cause == TeleportCause.UNKNOWN) {
            return PlayerTeleportEvent.TeleportCause.UNKNOWN;
        }
        return PlayerTeleportEvent.TeleportCause.PLUGIN;
    }*/

}
