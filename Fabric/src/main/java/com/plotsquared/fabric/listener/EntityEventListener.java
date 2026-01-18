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
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.flag.implementations.DisablePhysicsFlag;
import com.plotsquared.core.plot.flag.implementations.ExplosionFlag;
import com.plotsquared.core.plot.flag.implementations.InvincibleFlag;
import com.plotsquared.core.plot.world.PlotAreaManager;
import com.plotsquared.core.util.EventDispatcher;
import com.plotsquared.core.util.PlotFlagUtil;
import com.plotsquared.fabric.FabricPlatform;
import com.plotsquared.fabric.data.PlotSquaredDataAttachments;
import com.plotsquared.fabric.listener.event.ExplosionPrimedEvent;
import com.plotsquared.fabric.listener.event.FallingBlockEntityEvent;
import com.plotsquared.fabric.listener.event.MobSpawnEvent;
import com.plotsquared.fabric.util.FabricEntityUtil;
import com.plotsquared.fabric.util.FabricUtil;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.nucleoid.stimuli.Stimuli;
import xyz.nucleoid.stimuli.event.entity.EntityDamageEvent;
import xyz.nucleoid.stimuli.event.entity.EntitySpawnEvent;

import java.util.List;


@SuppressWarnings("unused")
public class EntityEventListener {

    private final FabricPlatform platform;
    private final PlotAreaManager plotAreaManager;
    private final EventDispatcher eventDispatcher;
    private float lastRadius;

    @Inject
    public EntityEventListener(
            final @NonNull FabricPlatform platform,
            final @NonNull PlotAreaManager plotAreaManager,
            final @NonNull EventDispatcher eventDispatcher
    ) {
        this.platform = platform;
        this.plotAreaManager = plotAreaManager;
        this.eventDispatcher = eventDispatcher;
        Stimuli.global().listen(EntityDamageEvent.EVENT, this::onEntityCombustByEntity);
        Stimuli.global().listen(EntityDamageEvent.EVENT, this::onEntityDamageByEntityEvent);
        MobSpawnEvent.EVENT.register(this::creatureSpawnEvent);
        FallingBlockEntityEvent.EVENT.register(this::onEntityFall);
        Stimuli.global().listen(EntityDamageEvent.EVENT, this::onDamage);
        Stimuli.global().listen(EntityDamageEvent.EVENT, this::onBigBoom);
        ExplosionPrimedEvent.EVENT.register(this::onPrime);
        Stimuli.global().listen(EntitySpawnEvent.EVENT, this::onVehicleCreate);

    }

    public InteractionResult onEntityCombustByEntity(LivingEntity entity, DamageSource source, float amount) {
        if (source.is(DamageTypes.ON_FIRE)) {
            return onEntityDamageByEntityCommon(source.getEntity(), entity, source);
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onEntityDamageByEntityEvent(LivingEntity entity, DamageSource source, float amount) {
        return onEntityDamageByEntityCommon(source.getEntity(), entity, source);
    }

    private InteractionResult onEntityDamageByEntityCommon(
            final Entity damager,
            final Entity victim,
            final DamageSource cause
    ) {
        if (damager != null) {
            Location location = FabricUtil.adapt(GlobalPos.of(damager.level().dimension(), damager.blockPosition()));
            if (!this.plotAreaManager.hasPlotArea(location.getWorldName())) {
                return InteractionResult.PASS;
            }
            if (!FabricEntityUtil.entityDamage(damager, victim, cause)) {
                if (victim instanceof AgeableMob ageable) {
                    if (ageable.getAge() == -24000) {
                        ageable.setAge(0);
                        ageable.setBaby(false);
                    }
                }
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    public InteractionResult creatureSpawnEvent(
            ServerLevelAccessor serverLevelAccessor,
            DifficultyInstance difficultyInstance,
            MobSpawnType mobSpawnType,
            SpawnGroupData spawnGroupData,
            Mob mob
    ) {
        Location location = FabricUtil.adapt(GlobalPos.of(mob.level().dimension(), mob.blockPosition()));
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        }
        // Armour-stands are handled elsewhere and should not be handled by area-wide entity-spawn options
        if (mob.getType() == EntityType.ARMOR_STAND) {
            return InteractionResult.FAIL;
        }
        switch (mobSpawnType.name().toUpperCase()) {
            case "DISPENSE_EGG", "EGG", "OCELOT_BABY", "SPAWN_EGG" -> {
                if (!area.isSpawnEggs()) {
                    mob.remove(Entity.RemovalReason.DISCARDED);
                    return InteractionResult.FAIL;
                }
            }
            case "REINFORCEMENTS", "NATURAL", "MOUNT", "PATROL", "RAID", "SHEARED", "SILVERFISH_BLOCK", "ENDER_PEARL",
                    "TRAP", "VILLAGE_DEFENSE", "VILLAGE_INVASION", "BEEHIVE", "CHUNK_GEN", "NETHER_PORTAL",
                    "FROZEN", "SPELL", "DEFAULT" -> {
                if (!area.isMobSpawning()) {
                    mob.remove(Entity.RemovalReason.DISCARDED);
                    return InteractionResult.FAIL;
                }
            }
            case "BREEDING", "DUPLICATION" -> {
                if (!area.isSpawnBreeding()) {
                    mob.remove(Entity.RemovalReason.DISCARDED);
                    return InteractionResult.FAIL;
                }
            }
            case "COMMAND" -> {
                if (!area.isSpawnCustom()) {
                    mob.remove(Entity.RemovalReason.DISCARDED);
                    return InteractionResult.FAIL;
                }
                // No need to clutter metadata if running paper
                //if (!PaperLib.isPaper()) {
                mob.setAttached(PlotSquaredDataAttachments.PS_CUSTOM_SPAWNED, true);
                // }
                return InteractionResult.PASS; // Don't cancel if mob spawning is disabled
            }
            case "BUILD_IRONGOLEM", "BUILD_SNOWMAN", "BUILD_WITHER" -> {
                if (!area.isSpawnCustom()) {
                    mob.remove(Entity.RemovalReason.DISCARDED);
                    return InteractionResult.FAIL;
                }
            }
            case "SPAWNER" -> {
                if (!area.isMobSpawnerSpawning()) {
                    mob.remove(Entity.RemovalReason.DISCARDED);
                    return InteractionResult.FAIL;
                }
            }
        }
        Plot plot = area.getOwnedPlotAbs(location);
        if (plot == null) {
            if (!area.isMobSpawning()) {
                mob.remove(Entity.RemovalReason.DISCARDED);
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }
        if (FabricEntityUtil.checkEntity(mob, plot.getBasePlot(false))) {
            mob.remove(Entity.RemovalReason.DISCARDED);
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onEntityFall(Entity entity) {
        if (entity instanceof FallingBlockEntity fallingBlockEntity) {
            BlockState block = fallingBlockEntity.getBlockState();
            ServerLevel world = entity.getServer().getLevel(entity.level().dimension());
            String worldName = world.dimension().location().getPath();
            if (!this.plotAreaManager.hasPlotArea(worldName)) {
                return InteractionResult.PASS;
            }
            Location location = FabricUtil.adapt(GlobalPos.of(world.dimension(), entity.blockPosition()));
            PlotArea area = location.getPlotArea();
            if (area == null) {
                return InteractionResult.PASS;
            }
            Plot plot = area.getOwnedPlotAbs(location);
            if (plot == null || plot.getFlag(DisablePhysicsFlag.class)) {
                if (plot != null) {
                    if (!fallingBlockEntity.isNoGravity()) {
                        fallingBlockEntity.setNoGravity(true);
                        fallingBlockEntity.setDeltaMovement(Vec3.ZERO);
                        BlockEventListener.sendBlockChange(GlobalPos.of(world.dimension(), entity.blockPosition()), block);
                    }
                    plot.debug("Falling block event was cancelled because disable-physics = true");
                }
                return InteractionResult.FAIL;
            }
            if (!entity.isNoGravity()) {
                List<String> plotStrings = entity.getAttached(PlotSquaredDataAttachments.PLOT);

                List<Plot> meta = plotStrings == null ? List.of() : plotStrings.stream().map(s -> Plot.fromString(
                        null,
                        s
                )).toList();
                if (meta.isEmpty()) {
                    return InteractionResult.PASS;
                }
                Plot origin = meta.get(0);
                if (origin != null && !origin.equals(plot)) {
                    entity.remove(Entity.RemovalReason.DISCARDED);
                    return InteractionResult.FAIL;
                }
            } else if (entity.getBlockStateOn().getBlock() instanceof AirBlock) {
                entity.setAttached(PlotSquaredDataAttachments.PLOT, List.of(plot.toString()));
            }
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onDamage(LivingEntity entity, DamageSource source, float amount) {
        if (entity.getType() != EntityType.PLAYER) {
            return InteractionResult.PASS;
        }
        Location location = FabricUtil.adapt(GlobalPos.of(entity.level().dimension(), entity.blockPosition()));
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        }
        Plot plot = location.getOwnedPlot();
        if (plot == null) {
            if (PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, InvincibleFlag.class, true)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }
        if (plot.getFlag(InvincibleFlag.class)) {
            plot.debug(entity.getName() + " could not take damage because invincible = true");
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onBigBoom(LivingEntity entity, DamageSource source, float amount) {
        if (source.is(DamageTypes.EXPLOSION)) {
            Location location = FabricUtil.adapt(GlobalPos.of(entity.level().dimension(), entity.blockPosition()));
            PlotArea area = location.getPlotArea();
            boolean plotArea = location.isPlotArea();
            if (!plotArea) {
                if (!this.plotAreaManager.hasPlotArea(location.getWorldName())) {
                    return InteractionResult.PASS;
                }
                return InteractionResult.PASS;
            }
            Plot plot = area.getOwnedPlot(location);
            if (plot != null) {
                if (plot.getFlag(ExplosionFlag.class)) {
                    List<Plot> meta =
                            entity
                                    .getAttached(PlotSquaredDataAttachments.PLOT)
                                    .stream()
                                    .map(s -> Plot.fromString(null, s))
                                    .toList();
                    if (this.lastRadius != 0) {

                        List<Entity> nearby = entity.level().getEntities(entity, AABB.ofSize(Vec3.ZERO, this.lastRadius,
                                this.lastRadius, this.lastRadius
                        ));
                        for (Entity near : nearby) {
                            if (near instanceof PrimedTnt || near.getType().equals(EntityType.TNT_MINECART)) {
                                if (!near.hasAttached(PlotSquaredDataAttachments.PLOT)) {
                                    near.setAttached(PlotSquaredDataAttachments.PLOT, List.of(plot.toString()));
                                }
                            }
                        }
                        this.lastRadius = 0;
                    }
                    return InteractionResult.PASS;
                } else {
                    plot.debug("Explosion was cancelled because explosion = false");
                }
            }
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    /*
    public InteractionResult onPeskyMobsChangeTheWorldLikeWTFEvent() {
        Entity e = event.getEntity();
        Block type = event.getBlock().getType();
        Location location = FabricUtil.adapt(event.getBlock().getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return InteractionResult.PASS;
        }
        if (e instanceof FallingBlockEntity) {
            // allow falling blocks converting to blocks and vice versa
            return InteractionResult.PASS;
        } else if (e instanceof Boat) {
            // allow boats destroying lily pads
            if (type == Blocks.LILY_PAD) {
                return InteractionResult.PASS;
            }
        } else if (e instanceof ServerPlayer player) {
            FabricPlayer pp = FabricUtil.adapt(player);
            if (type.toString().equals("POWDER_SNOW")) {
                // Burning player evaporating powder snow. Use same checks as
                // trampling farmland
                BlockType blockType = FabricAdapter.adapt(type);
                if (!this.eventDispatcher.checkPlayerBlockEvent(pp,
                        PlayerBlockEventType.TRIGGER_PHYSICAL, location, blockType, true
                )) {
                    return InteractionResult.FAIL;
                }
                return InteractionResult.PASS;
            } else {
                // already handled by other flags (mainly the 'use' flag):
                // - player tilting big dripleaf by standing on it
                // - player picking glow berries from cave vine
                // - player trampling farmland
                // - player standing on or clicking redstone ore
                return InteractionResult.PASS;
            }
        } else if (e instanceof Projectile entity) {
            // Exact same as the ProjectileHitEvent listener, except that we let
            // the entity-change-block determine what to do with shooters that
            // aren't players and aren't blocks
            Plot plot = area.getPlot(location);
            ProjectileSource shooter = entity.getShooter();
            if (shooter instanceof Player) {
                PlotPlayer<?> pp = BukkitUtil.adapt((Player) shooter);
                if (plot == null) {
                    if (area.isRoadFlags() && !area.getRoadFlag(ProjectileChangeBlockFlag.class) && !pp.hasPermission(Permission.PERMISSION_ADMIN_PROJECTILE_UNOWNED)) {
                        entity.remove();
                        event.setCancelled(true);
                    }
                    return;
                }
                if (plot.isAdded(pp.getUUID()) || plot.getFlag(ProjectileChangeBlockFlag.class) || pp.hasPermission(Permission.PERMISSION_ADMIN_PROJECTILE_OTHER)) {
                    return;
                }
                entity.remove();
                event.setCancelled(true);
                return;
            }
            if (!(shooter instanceof Entity) && shooter != null) {
                if (plot == null) {
                    entity.remove();
                    event.setCancelled(true);
                    return;
                }
                Location sLoc =
                        BukkitUtil.adapt(((BlockProjectileSource) shooter).getBlock().getLocation());
                if (!area.contains(sLoc.getX(), sLoc.getZ())) {
                    entity.remove();
                    event.setCancelled(true);
                    return;
                }
                Plot sPlot = area.getOwnedPlotAbs(sLoc);
                if (sPlot == null || !PlotHandler.sameOwners(plot, sPlot)) {
                    entity.remove();
                    event.setCancelled(true);
                }
                return;
            }
            // fall back to entity-change-block flag
        }

        Plot plot = area.getOwnedPlot(location);
        if (plot != null && !plot.getFlag(EntityChangeBlockFlag.class)) {
            plot.debug(e.getType() + " could not change block because entity-change-block = false");
            event.setCancelled(true);
        }
    }*/

    public InteractionResult onPrime(float radius) {
        this.lastRadius = radius + 1;
        return InteractionResult.PASS;
    }

    public InteractionResult onVehicleCreate(Entity entity) {
        if (entity.isVehicle()) {
            Location location = FabricUtil.adapt(GlobalPos.of(entity.level().dimension(), entity.blockPosition()));
            PlotArea area = location.getPlotArea();
            if (area == null) {
                return InteractionResult.PASS;
            }
            Plot plot = area.getOwnedPlotAbs(location);
            if (plot == null || FabricEntityUtil.checkEntity(entity, plot)) {
                entity.remove(Entity.RemovalReason.DISCARDED);
                return InteractionResult.FAIL;
            }
            if (Settings.Enabled_Components.KILL_ROAD_VEHICLES) {
                entity.setAttached(PlotSquaredDataAttachments.PLOT_DATA, plot.toString());
            }
        }
        return InteractionResult.PASS;
    }

}
