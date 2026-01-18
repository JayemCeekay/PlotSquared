package com.plotsquared.fabric.util;

import com.google.inject.Singleton;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.configuration.caption.Caption;
import com.plotsquared.core.configuration.caption.LocaleHolder;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.util.BlockUtil;
import com.plotsquared.core.util.MathMan;
import com.plotsquared.core.util.PlayerManager;
import com.plotsquared.core.util.StringComparison;
import com.plotsquared.core.util.WorldUtil;
import com.plotsquared.core.util.task.TaskManager;
import com.plotsquared.fabric.FabricPlatform;
import com.plotsquared.fabric.player.FabricPlayer;
import com.plotsquared.fabric.player.FabricPlayerManager;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockCategories;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.entity.EntityTypes;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Stream;

@SuppressWarnings({"unused", "WeakerAccess"})
@Singleton
public class FabricUtil extends WorldUtil {

    private volatile FabricServerAudiences adventure;

    public FabricServerAudiences adventure() {
        FabricServerAudiences ret = this.adventure;
        if (ret == null) {
            throw new IllegalStateException("Tried to access Adventure without a running server!");
        }
        return ret;
    }

    public static final FabricServerAudiences FABRIC_AUDIENCES =
            FabricServerAudiences.of(FabricPlatform.SERVER);
    public static final LegacyComponentSerializer LEGACY_COMPONENT_SERIALIZER = LegacyComponentSerializer.legacySection();
    public static final MiniMessage MINI_MESSAGE = MiniMessage.builder().build();
    private static final Logger LOGGER = LogManager.getLogger("PlotSquared/" + FabricUtil.class.getSimpleName());
    private final Collection<BlockType> tileEntityTypes = new HashSet<>();

    /**
     * Turn a Fabric {@link net.minecraft.server.level.ServerPlayer} into a PlotSquared {@link PlotPlayer}
     *
     * @param player Fabric ServerPlayer
     * @return PlotSquared player
     */
    public static @NonNull FabricPlayer adapt(final @NonNull ServerPlayer player) {
        final PlayerManager<?, ?> playerManager = PlotSquared.platform().playerManager();
        return ((FabricPlayerManager) playerManager).getPlayer(player);
    }

    /**
     * Turn a Fabric {@link GlobalPos} into a PlotSquared {@link Location}.
     * This only copies the 4-tuple (world,x,y,z) and does not include the yaw and the pitch
     *
     * @param globalPos Fabric globalPos
     * @return PlotSquared location
     */
    public static @NonNull Location adapt(final @NotNull GlobalPos globalPos) {
        return Location
                .at(
                        FabricWorld.of(globalPos.dimension().location().getPath()),
                        MathMan.roundInt(globalPos.pos().getX()),
                        MathMan.roundInt(globalPos.pos().getY()),
                        MathMan.roundInt(globalPos.pos().getZ())
                );
    }

    /**
     * Turn a Fabric {@link GlobalPos} into a PlotSquared {@link Location}.
     * This copies the entire 6-tuple (world,x,y,z,yaw,pitch).
     *
     * @param location Bukkit location
     * @return PlotSquared location
     */
    public static @NonNull Location adaptComplete(final @NonNull GlobalPos location, float yaw, float pitch) {
        Location location1 = Location
                .at(
                        FabricWorld.of(location.dimension().location().getPath()),
                        MathMan.roundInt(location.pos().getX()),
                        MathMan.roundInt(location.pos().getY()),
                        MathMan.roundInt(location.pos().getZ()),
                        yaw,
                        pitch
                );
        return location1;
    }

    /**
     * Turn a PlotSquared {@link Location} into a Fabric {@link GlobalPos}.
     * This only copies the 4-tuple (world,x,y,z) and does not include the yaw and the pitch
     *
     * @param location PlotSquared location
     * @return Fabric GlobalPos
     */
    public static @NotNull GlobalPos adapt(final @NonNull Location location) {
        return GlobalPos.of(((ServerLevel) location.getWorld().getPlatformWorld()).dimension(), new BlockPos(location.getX(),
                location.getY(), location.getZ()
        ));

    }

    /**
     * Get a Fabric {@link ServerLevel} from its name
     *
     * @param string World name
     * @return World if it exists, or {@code null}
     */
    public static @Nullable ServerLevel getWorld(final @NonNull String string) {
        for (ServerLevel serverLevel : FabricPlatform.SERVER.getAllLevels()) {
            if (serverLevel.dimension().location().getPath().equals(string)) {
                return serverLevel;
            }
        }
        return null;
    }

    private static void ensureLoaded(
            final @NonNull String world,
            final int x,
            final int z,
            final @NonNull Consumer<ChunkAccess> chunkConsumer
    ) {
        Objects.requireNonNull(getWorld(world)).hasChunk(x >> 4, z >> 4);
        //Objects.requireNonNull(getWorld(world)).getChunkSource().getChunkFuture(x >> 4, z >> 4, ChunkStatus.FULL, true)
        // .thenAccept(chunk -> ensureMainThread(chunkConsumer, chunk));
    }

    private static void ensureLoaded(final @NonNull Location location, final @NonNull Consumer<LevelChunk> chunkConsumer) {
        //Objects.requireNonNull(getWorld(location.getWorldName())).hasChunk(location.getX() >> 4, location.getZ() >> 4);
        // getWorld(location.getWorldName()).getChunkSource().getChunkFuture()
        //PaperLib.getChunkAtAsync(adapt(location), true).thenAccept(chunk -> ensureMainThread(chunkConsumer, chunk));
    }

    private static <T> void ensureMainThread(final @NonNull Consumer<T> consumer, final @NonNull T value) {
        if (FabricPlatform.SERVER.isSameThread()) {
            consumer.accept(value);
        } else {
            FabricPlatform.SERVER.execute(() -> consumer.accept(value));
            //Bukkit.getScheduler().runTask(BukkitPlatform.getPlugin(BukkitPlatform.class), () -> consumer.accept(value));
        }
    }

    @Override
    public boolean isBlockSame(final @NonNull BlockState block1, final @NonNull BlockState block2) {
        if (block1.equals(block2)) {
            return true;
        }
        final Block mat1 = FabricAdapter.adapt(block1.getBlockType());
        final Block mat2 = FabricAdapter.adapt(block2.getBlockType());
        return mat1 == mat2;
    }

    @Override
    public boolean isWorld(final @NonNull String worldName) {
        return getWorld(worldName) != null;
    }

    @Override
    public void getBiome(final @NonNull String world, final int x, final int z, final @NonNull Consumer<BiomeType> result) {
        /*ensureLoaded(world, x, z,
                chunk -> */
        result.accept(getWeWorld(world).getBiome(BlockVector2.at(x, z)));
    }

    @Override
    public @NonNull BiomeType getBiomeSynchronous(final @NonNull String world, final int x, final int z) {
        return FabricAdapter.adapt(Objects.requireNonNull(getWorld(world))).getBiome(BlockVector2.at(x, z));
    }

    @Override
    public void getHighestBlock(final @NonNull String world, final int x, final int z, final @NonNull IntConsumer result) {
        //ensureLoaded(world, x, z, chunk -> {
        final ServerLevel fabricWorld = Objects.requireNonNull(getWorld(world));
        // Skip top and bottom block
        int air = 1;
        int maxY = FabricWorld.getMaxWorldHeight(fabricWorld);
        int minY = FabricWorld.getMinWorldHeight(fabricWorld);
        for (int y = maxY - 1; y >= minY; y--) {
            net.minecraft.world.level.block.state.BlockState block = fabricWorld.getBlockState(new BlockPos(x, y, z));
            if (block.isSolid()) {
                if (air > 1) {
                    result.accept(y);
                    return;
                }
                air = 0;
            } else {
                if (block.liquid()) {
                    result.accept(y);
                    return;
                }
                air++;
            }
        }
        result.accept(fabricWorld.getMaxBuildHeight() - 1);
        //});
    }

    @Override
    @NonNegative
    public int getHighestBlockSynchronous(final @NonNull String world, final int x, final int z) {
        final ServerLevel fabricWorld = Objects.requireNonNull(getWorld(world));
        // Skip top and bottom block
        int air = 1;
        int maxY = fabricWorld.getMaxBuildHeight();
        int minY = fabricWorld.getMinBuildHeight();
        for (int y = maxY - 1; y >= minY; y--) {
            net.minecraft.world.level.block.state.BlockState block = fabricWorld.getBlockState(new BlockPos(x, y, z));
            if (block.isSolid()) {
                if (air > 1) {
                    return y;
                }
                air = 0;
            } else {
                if (block.liquid()) {
                    return y;
                }
                air++;
            }
        }
        return fabricWorld.getMaxBuildHeight() - 1;
    }

    @Override
    public @NonNull String[] getSignSynchronous(final @NonNull Location location) {
        net.minecraft.world.level.block.state.BlockState block = Objects
                .requireNonNull(getWorld(location.getWorldName()))
                .getBlockState(new BlockPos(
                        location.getX(),
                        location.getY(),
                        location.getZ()
                ));
        try {
            return (String[]) TaskManager.getPlatformImplementation().sync(() -> {
                if (block.getBlock() instanceof SignBlock sign) {
                    BlockEntity blockEntity = getWorld(location.getWorldName()).getBlockEntity(new BlockPos(
                            location.getX(),
                            location.getY(),
                            location.getZ()
                    ));
                    if (blockEntity instanceof SignBlockEntity signBlockEntity) {
                        return Arrays.stream(signBlockEntity.getText(true).getMessages(false)).toArray();
                    }

                }
                return new String[0];
            });
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return new String[0];
    }

    @Override
    public @NonNull Location getSpawn(final @NonNull String world) {
        final GlobalPos temp = GlobalPos.of(getWorld(world).dimension(), getWorld(world).getSharedSpawnPos());
        return Location.at(world, temp.pos().getX(), temp.pos().getY(), temp.pos().getZ());
    }

    @Override
    public void setSpawn(final @NonNull Location location) {
        final ServerLevel world = getWorld(location.getWorldName());
        if (world != null) {
            world.setDefaultSpawnPos(new BlockPos(location.getX(), location.getY(), location.getZ()), 0f);
        }
    }

    @Override
    public void saveWorld(final @NonNull String worldName) {
        final ServerLevel world = getWorld(worldName);
        if (world != null) {
            /* TODO check on this */
            world.save(null, true, false);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setSign(
            final @NonNull Location location, final @NonNull Caption[] lines,
            final @NonNull TagResolver... replacements
    ) {
        PlotArea area = location.getPlotArea();
        final ServerLevel world = getWorld(location.getWorldName());
        BlockPos signBlockPos = new BlockPos(location.getX(), location.getY(), location.getZ());
        final net.minecraft.world.level.block.state.BlockState blockstate = world.getBlockState(signBlockPos);
        final Block block = blockstate.getBlock();
        if (!(block instanceof SignBlock wallSignBlock)) {
            Direction facing = Direction.NORTH;
            if (!world.getBlockState(new BlockPos(location.getX(), location.getY(), location.getZ() + 1))
                    .blocksMotion()) {
                if (world.getBlockState(new BlockPos(location.getX() - 1, location.getY(), location.getZ()))
                        .blocksMotion()) {
                    facing = Direction.EAST;
                } else if (world.getBlockState(new BlockPos(location.getX() + 1, location.getY(), location.getZ()))
                        .blocksMotion()) {
                    facing = Direction.WEST;
                } else if (world.getBlockState(new BlockPos(location.getX(), location.getY(), location.getZ() - 1))
                        .blocksMotion()) {
                    facing = Direction.SOUTH;
                }
            }
            WoodType woodType =
                    WoodType.values().filter(woodType1 -> area
                            .getSignMaterial()
                            .startsWith(woodType1.name().toUpperCase())).findFirst().get();
            net.minecraft.world.level.block.state.BlockState sign = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(
                    "minecraft",
                    area.getSignMaterial().toLowerCase()
            )).defaultBlockState();
            sign = sign.setValue(WallSignBlock.FACING, facing);
            world.setBlock(signBlockPos, sign, 3, 512);
            if (world.getBlockEntity(signBlockPos) != null) {
                List<MutableComponent> signTextLines = Arrays
                        .stream(lines)
                        .map(caption -> convertToMinecraftText(MINI_MESSAGE.deserialize(
                                caption.getComponent(LocaleHolder.console()),
                                replacements
                        )))
                        .toList();

                for (int i = 0; i < 4; i++) {
                    SignText text = ((SignBlockEntity) world.getBlockEntity(signBlockPos)).getFrontText().setMessage(
                            i,
                            signTextLines.get(i)
                    );
                    ((SignBlockEntity) world.getBlockEntity(signBlockPos)).setText(text, true);
                }
            }
        }
    }

    public static MutableComponent convertToMinecraftText(net.kyori.adventure.text.Component adventureComponent) {
        if (adventureComponent instanceof TextComponent) {
            TextComponent textComponent = (TextComponent) adventureComponent;

            // Convert the content of the TextComponent
            MutableComponent minecraftText = Component.literal(textComponent.content());

            // Convert and apply the style
            Style style = convertStyle(textComponent.style());
            minecraftText.setStyle(style);

            // Recursively convert children
            for (net.kyori.adventure.text.Component child : textComponent.children()) {
                minecraftText.append(convertToMinecraftText(child));
            }

            return minecraftText;
        }

        // Handle other types of components if needed
        return Component.literal(""); // Return an empty text for unsupported components
    }

    private static Style convertStyle(net.kyori.adventure.text.format.Style adventureStyle) {
        Style style = Style.EMPTY;

        // Convert color
        if (adventureStyle.color() != null) {
            style = style.withColor(TextColor.fromRgb(adventureStyle.color().value()));
        }

        // Convert other formatting
        if (adventureStyle.hasDecoration(TextDecoration.BOLD)) {
            style = style.withBold(true);
        }
        if (adventureStyle.hasDecoration(TextDecoration.ITALIC)) {
            style = style.withItalic(true);
        }
        if (adventureStyle.hasDecoration(TextDecoration.UNDERLINED)) {
            style = style.withUnderlined(true);
        }
        if (adventureStyle.hasDecoration(TextDecoration.STRIKETHROUGH)) {
            style = style.withStrikethrough(true);
        }
        if (adventureStyle.hasDecoration(TextDecoration.OBFUSCATED)) {
            style = style.withObfuscated(true);
        }

        return style;
    }

    @Override
    public @NonNull StringComparison<BlockState>.ComparisonResult getClosestBlock(@NonNull String name) {
        BlockState state = BlockUtil.get(name);
        return new StringComparison<BlockState>().new ComparisonResult(1, state);
    }

    @Override
    public com.sk89q.worldedit.world.@NonNull World getWeWorld(final @NonNull String world) {
        return FabricAdapter.adapt((getWorld(world)));
    }

    @Override
    public void refreshChunk(int x, int z, String world) {
        getWorld(world).getChunk(x, z);
    }

    @Override
    public void getBlock(final @NonNull Location location, final @NonNull Consumer<BlockState> result) {
        //ensureLoaded(location, chunk -> {
        final ServerLevel world = getWorld(location.getWorldName());
        final Block block = Objects.requireNonNull(world).getBlockState(new BlockPos(location.getX(), location.getY(),
                location.getZ()
        )).getBlock();
        result.accept(Objects.requireNonNull(FabricAdapter.adapt(block)).getDefaultState());
        // });
    }

    @Override
    public @NonNull BlockState getBlockSynchronous(final @NonNull Location location) {
        final ServerLevel world = getWorld(location.getWorldName());
        final Block block = Objects.requireNonNull(world).getBlockState(new BlockPos(location.getX(), location.getY(),
                location.getZ()
        )).getBlock();
        return Objects.requireNonNull(FabricAdapter.adapt(block)).getDefaultState();
    }

    @Override
    @NonNegative
    public double getHealth(final @NonNull PlotPlayer<?> player) {
        return Objects.requireNonNull(FabricPlatform.SERVER.getPlayerList().getPlayer(player.getUUID())).getHealth();
    }

    @Override
    @NonNegative
    public int getFoodLevel(final @NonNull PlotPlayer<?> player) {
        return Objects
                .requireNonNull(FabricPlatform.SERVER.getPlayerList().getPlayer(player.getUUID()))
                .getFoodData()
                .getFoodLevel();
    }

    @Override
    public void setHealth(final @NonNull PlotPlayer<?> player, @NonNegative final double health) {
        Objects.requireNonNull(FabricPlatform.SERVER.getPlayerList().getPlayer(player.getUUID())).setHealth((float) health);
    }

    @Override
    public void setFoodLevel(final @NonNull PlotPlayer<?> player, @NonNegative final int foodLevel) {
        FabricPlatform.SERVER.getPlayerList().getPlayer(player.getUUID()).getFoodData().setFoodLevel(foodLevel);
    }

    @Override
    public @NonNull Set<com.sk89q.worldedit.world.entity.EntityType> getTypesInCategory(final @NonNull String category) {
        final Collection<Class<?>> allowedInterfaces = new HashSet<>();
        switch (category) {
            case "animal" -> {
                allowedInterfaces.add(IronGolem.class);
                allowedInterfaces.add(SnowGolem.class);
                allowedInterfaces.add(Animal.class);
                allowedInterfaces.add(WaterAnimal.class);
                allowedInterfaces.add(AmbientCreature.class);
                if (PlotSquared.platform().serverVersion()[1] >= 19) {
                    allowedInterfaces.add(Allay.class);
                }
            }
            case "tameable" -> allowedInterfaces.add(TamableAnimal.class);
            case "vehicle" -> {
                allowedInterfaces.add(Boat.class);
                allowedInterfaces.add(AbstractMinecart.class);
            }
            case "hostile" -> {
                allowedInterfaces.add(Shulker.class);
                allowedInterfaces.add(Monster.class);
                allowedInterfaces.add(EnderDragonPart.class);
                allowedInterfaces.add(EnderDragon.class);
                allowedInterfaces.add(WitherBoss.class);
                allowedInterfaces.add(Slime.class);
                allowedInterfaces.add(Ghast.class);
                allowedInterfaces.add(Phantom.class);
                allowedInterfaces.add(EndCrystal.class);
            }
            case "hanging" -> allowedInterfaces.add(HangingEntity.class);
            case "villager" -> allowedInterfaces.add(Npc.class);
            case "projectile" -> allowedInterfaces.add(Projectile.class);
            case "other" -> {
                allowedInterfaces.add(ArmorStand.class);
                allowedInterfaces.add(FallingBlockEntity.class);
                allowedInterfaces.add(ItemEntity.class);
                allowedInterfaces.add(PrimedTnt.class);
                allowedInterfaces.add(AreaEffectCloud.class);
                allowedInterfaces.add(EvokerFangs.class);
                allowedInterfaces.add(LightningBolt.class);
                allowedInterfaces.add(ExperienceOrb.class);
                allowedInterfaces.add(EyeOfEnder.class);
                allowedInterfaces.add(FireworkRocketEntity.class);
            }
            case "player" -> allowedInterfaces.add(ServerPlayer.class);
            default -> LOGGER.error("Unknown entity category requested: {}", category);
        }
        final Set<com.sk89q.worldedit.world.entity.EntityType> types = new HashSet<>();
        outer:
        for (final net.minecraft.world.entity.EntityType<? extends Entity> fabricType :
                BuiltInRegistries.ENTITY_TYPE.stream().toList()) {
            try {
                final Class<?> entityClass = fabricType.create(FabricPlatform.SERVER.overworld()).getClass();
                for (final Class<?> allowedInterface : allowedInterfaces) {
                    if (allowedInterface.isAssignableFrom(entityClass)) {
                        types.add(EntityTypes.get(fabricType.toShortString()));
                        continue outer;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return types;
    }

    @Override
    public @NonNull Collection<BlockType> getTileEntityTypes() {
        if (this.tileEntityTypes.isEmpty()) {
            // Categories
            tileEntityTypes.addAll(BlockCategories.BANNERS.getAll());
            tileEntityTypes.addAll(BlockCategories.SIGNS.getAll());
            tileEntityTypes.addAll(BlockCategories.BEDS.getAll());
            tileEntityTypes.addAll(BlockCategories.FLOWER_POTS.getAll());
            // Individual Types
            // Add these from strings
            Stream.of(
                            "barrel",
                            "beacon",
                            "beehive",
                            "bee_nest",
                            "bell",
                            "blast_furnace",
                            "brewing_stand",
                            "campfire",
                            "chest",
                            "ender_chest",
                            "trapped_chest",
                            "command_block",
                            "end_gateway",
                            "hopper",
                            "jigsaw",
                            "jubekox",
                            "lectern",
                            "note_block",
                            "black_shulker_box",
                            "blue_shulker_box",
                            "brown_shulker_box",
                            "cyan_shulker_box",
                            "gray_shulker_box",
                            "green_shulker_box",
                            "light_blue_shulker_box",
                            "light_gray_shulker_box",
                            "lime_shulker_box",
                            "magenta_shulker_box",
                            "orange_shulker_box",
                            "pink_shulker_box",
                            "purple_shulker_box",
                            "red_shulker_box",
                            "shulker_box",
                            "white_shulker_box",
                            "yellow_shulker_box",
                            "smoker",
                            "structure_block",
                            "structure_void"
                    )
                    .map(BlockTypes::get).filter(Objects::nonNull).forEach(tileEntityTypes::add);
        }
        return this.tileEntityTypes;
    }

    @Override
    @NonNegative
    public int getTileEntityCount(final @NonNull String world, final @NonNull BlockVector2 chunk) {
        return Objects.requireNonNull(getWorld(world)).
                getChunk(chunk.getBlockX(), chunk.getBlockZ()).getBlockEntities().size();
    }

    @Override
    public Set<BlockVector2> getChunkChunks(String world) {
        Set<BlockVector2> chunks = super.getChunkChunks(world);
        if (FabricPlatform.SERVER.isSameThread()) {
            for (ChunkHolder chunk :
                    Objects.requireNonNull(getWorld(world).getChunkSource().chunkMap.visibleChunkMap.values())) {
                BlockVector2 loc = BlockVector2.at(chunk.getPos().x >> 5, chunk.getPos().z >> 5);
                chunks.add(loc);
            }
        } else {
            throw new RuntimeException("getChunkChunks FabricUtil NotMainThread");
            /*
            final Semaphore semaphore = new Semaphore(1);
            try {
                semaphore.acquire();
                Bukkit.getScheduler().runTask(BukkitPlatform.getPlugin(BukkitPlatform.class), () -> {
                    for (Chunk chunk : Objects.requireNonNull(Bukkit.getWorld(world)).getLoadedChunks()) {
                        BlockVector2 loc = BlockVector2.at(chunk.getX() >> 5, chunk.getZ() >> 5);
                        chunks.add(loc);
                    }
                    semaphore.release();
                });
                semaphore.acquireUninterruptibly();
            } catch (final Exception e) {
                e.printStackTrace();
            }*/
        }
        return chunks;
    }

    public static List<Entity> getEntitiesInChunk(ServerLevel world, LevelChunk chunk) {
        List<Entity> entities = new ArrayList<>();
        world.getEntities().get(
                new AABB(chunk.getPos().getMinBlockX(), chunk.getMaxBuildHeight(), chunk.getPos().getMinBlockZ(),
                        chunk.getPos().getMaxBlockX(), chunk.getMaxBuildHeight(), chunk.getPos().getMaxBlockZ()
                ), entities::add
        );
        return entities;
    }

}
