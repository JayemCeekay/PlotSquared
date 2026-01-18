package com.plotsquared.fabric.generator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.generator.ClassicPlotWorld;
import com.plotsquared.core.generator.GeneratorWrapper;
import com.plotsquared.core.generator.IndependentPlotGenerator;
import com.plotsquared.core.location.ChunkWrapper;
import com.plotsquared.core.location.UncheckedWorldLocation;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.world.PlotAreaManager;
import com.plotsquared.core.queue.ZeroedDelegateScopedQueueCoordinator;
import com.plotsquared.core.util.ChunkManager;
import com.plotsquared.fabric.FabricPlatform;
import com.plotsquared.fabric.queue.GenChunk;
import com.plotsquared.fabric.util.FabricUtil;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.BiomeSources;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class FabricPlotGenerator extends ChunkGenerator implements GeneratorWrapper<ChunkGenerator> {

    private static final Logger LOGGER = LogManager.getLogger("PlotSquared/" + FabricPlotGenerator.class.getSimpleName());

    private final PlotAreaManager plotAreaManager;
    private final IndependentPlotGenerator plotGenerator;
    private final ChunkGenerator platformGenerator;
    private final boolean full;
    private final String levelName;

    private List<BlockPopulator> populators;
    private final BlockStatePopulator blockStatePopulator;
    private boolean loaded = false;

    private PlotArea lastPlotArea;
    private int lastChunkX = Integer.MIN_VALUE;
    private int lastChunkZ = Integer.MIN_VALUE;

    public FabricPlotGenerator(
            final @NonNull String name,
            final @NonNull IndependentPlotGenerator generator,
            final @NonNull PlotAreaManager plotAreaManager
    ) {
        super(FabricPlatform.SERVER.overworld().getChunkSource().getGenerator().getBiomeSource());
        this.plotAreaManager = plotAreaManager;
        this.levelName = name;
        this.plotGenerator = generator;
        this.platformGenerator = this;
        this.blockStatePopulator = new BlockStatePopulator(this.plotGenerator);
        this.full = true;
    }

    public FabricPlotGenerator(
            final String world,
            final ChunkGenerator cg,
            final @NonNull PlotAreaManager plotAreaManager
    ) {
        super(cg.getBiomeSource());
        if (cg instanceof FabricPlotGenerator) {
            throw new IllegalStateException("ChunkGenerator: " + cg.getClass().getName() + " is already a FabricPlotGenerator");
        }
        this.plotAreaManager = plotAreaManager;
        this.levelName = world;
        this.full = false;
        this.platformGenerator = cg;
        this.plotGenerator = new DelegatePlotGenerator(cg, world);
        this.blockStatePopulator = new BlockStatePopulator(this.plotGenerator);
    }


    @Override
    public IndependentPlotGenerator getPlotGenerator() {
        return this.plotGenerator;
    }

    @Override
    public ChunkGenerator getPlatformGenerator() {
        return this.platformGenerator;
    }

    @Override
    public void augment(final PlotArea area) {
        FabricAugmentedGenerator.get(FabricUtil.getWorld(area.getWorldName()));
    }

    @Override
    public boolean isFull() {
        return this.full;
    }

    @Override
    protected @NotNull MapCodec<? extends ChunkGenerator> codec() {
        return null;
    }

    @Override
    public void applyCarvers(
            final WorldGenRegion worldGenRegion,
            final long l,
            final RandomState randomState,
            final BiomeManager biomeManager,
            final StructureManager structureManager,
            final ChunkAccess chunkAccess,
            final GenerationStep.Carving carving
    ) {
        if (platformGenerator != this) {
            platformGenerator.applyCarvers(worldGenRegion, l, randomState, biomeManager, structureManager, chunkAccess, carving);
        }
    }

    @Override
    public void buildSurface(
            final WorldGenRegion worldGenRegion,
            final StructureManager structureManager,
            final RandomState randomState,
            final ChunkAccess chunkAccess
    ) {
    }

    @Override
    public void spawnOriginalMobs(final WorldGenRegion worldGenRegion) {
    }


    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public @NotNull CompletableFuture<ChunkAccess> fillFromNoise(
            final Blender blender,
            final RandomState randomState,
            final StructureManager structureManager,
            final ChunkAccess chunkAccess
    ) {
        if (this.platformGenerator != this) {
            return this.platformGenerator.fillFromNoise(blender, randomState, structureManager, chunkAccess);
        }
        int minY = chunkAccess.getMinBuildHeight();
        int maxY = chunkAccess.getMaxBuildHeight();
        GenChunk result = new GenChunk(minY, maxY);
        // Set the chunk location
        result.setChunk(new ChunkWrapper(structureManager.structureCheck.dimension.location().getPath(), chunkAccess.getPos().x,
                chunkAccess.getPos().z));
        // Set the result data
        result.setChunkData(chunkAccess);
        result.result = null;

        // Catch any exceptions (as exceptions usually thrown)
        try {
            generate(BlockVector2.at(chunkAccess.getPos().x, chunkAccess.getPos().z),
                    structureManager.structureCheck.dimension.location().getPath(), result,
                    false);
        } catch (Throwable e) {
            LOGGER.error("Error attempting to generate chunk.", e);
        }
        return CompletableFuture.completedFuture(chunkAccess);
    }

    private void generate(BlockVector2 loc, String world, ZeroedDelegateScopedQueueCoordinator result, boolean biomes) {
        // Load if improperly loaded
        if (!this.loaded) {
            synchronized (this) {
                PlotSquared.get().loadWorld(world, this);
            }
        }
        // Process the chunk
        if (ChunkManager.preProcessChunk(loc, result)) {
            return;
        }
        PlotArea area = getPlotArea(world, loc.x(), loc.z());
        try {
            this.plotGenerator.generateChunk(result, area, biomes);
        } catch (Throwable e) {
            // Recover from generator error
            LOGGER.error("Error attempting to generate chunk.", e);
        }
        ChunkManager.postProcessChunk(loc, result);
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public int getBaseHeight(
            final int i,
            final int j,
            final Heightmap.Types types,
            final LevelHeightAccessor levelHeightAccessor,
            final RandomState randomState
    ) {

        PlotArea area = getPlotArea(this.levelName, i, j);
        if (area instanceof ClassicPlotWorld cpw) {
            // Default to plot height being the heighest point before decoration (i.e. roads, walls etc.)
            return cpw.PLOT_HEIGHT;
        }
        return 62;
    }

    @Override
    public NoiseColumn getBaseColumn(
            final int i,
            final int j,
            final LevelHeightAccessor levelHeightAccessor,
            final RandomState randomState
    ) {

        //return a noise column of air
        BlockState[] airColumn = new BlockState[256];
        Arrays.fill(airColumn, Blocks.AIR.defaultBlockState());
        return new NoiseColumn(0, airColumn);
    }

    @Override
    public void addDebugScreenInfo(final List<String> list, final RandomState randomState, final BlockPos blockPos) {

    }


    private synchronized PlotArea getPlotArea(String name, int chunkX, int chunkZ) {
        // Load if improperly loaded

        if (!this.loaded) {
            PlotSquared.get().loadWorld(name, this);
            //this.checkLoaded(FabricUtil.getWorld(name));
            // Do not set loaded to true as we want to ensure spawn limits are set when "loading" is actually able to be
            // completed properly.
        }
        if (lastPlotArea != null && name.equals(this.levelName) && chunkX == lastChunkX && chunkZ == lastChunkZ) {
            return lastPlotArea;
        }
        BlockVector3 loc = BlockVector3.at(chunkX << 4, 0, chunkZ << 4);
        if (lastPlotArea != null && lastPlotArea.getRegion().contains(loc) && lastPlotArea.getRegion().contains(loc)) {
            return lastPlotArea;
        }
        PlotArea area = UncheckedWorldLocation.at(name, loc).getPlotArea();
        if (area == null) {
            throw new IllegalStateException(String.format(
                    "Cannot generate chunk that does not belong to a plot area. World: %s",
                    name
            ));
        }
        this.lastChunkX = chunkX;
        this.lastChunkZ = chunkZ;
        return this.lastPlotArea = area;
    }

    public synchronized void checkLoaded(@NonNull ServerLevel world) {
        // Do not attempt to load configurations until WorldEdit has a platform ready.
        //if (!PlotSquared.get().isWeInitialised()) {
          //  return;
       // }
        if (!this.loaded) {
            String name = world.dimension().location().getPath();
            PlotSquared.get().loadWorld(name, this);
            final Set<PlotArea> areas = this.plotAreaManager.getPlotAreasSet(name);
            if (!areas.isEmpty()) {
                PlotArea area = areas.iterator().next();
                if (!area.isMobSpawning()) {
                    if (!area.isSpawnEggs()) {
                        world.setSpawnSettings(false, false);
                    }
                } else {
                    world.setSpawnSettings(true, true);
                }
            }
            this.loaded = true;
        }
    }

    @Override
    public void createStructures(
            final RegistryAccess registryAccess,
            final ChunkGeneratorStructureState chunkGeneratorStructureState,
            final StructureManager structureManager,
            final ChunkAccess chunkAccess,
            final StructureTemplateManager structureTemplateManager
    ) {
    }

    @Override
    public void createReferences(
            final WorldGenLevel worldGenLevel,
            final StructureManager structureManager,
            final ChunkAccess chunkAccess
    ) {
    }

    @Override
    public void applyBiomeDecoration(
            final WorldGenLevel worldGenLevel,
            final ChunkAccess chunkAccess,
            final StructureManager structureManager
    ) {
    }


}
