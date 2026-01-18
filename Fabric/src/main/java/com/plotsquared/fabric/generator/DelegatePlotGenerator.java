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
package com.plotsquared.fabric.generator;

import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.generator.IndependentPlotGenerator;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.PlotId;
import com.plotsquared.core.queue.ZeroedDelegateScopedQueueCoordinator;
import com.plotsquared.core.util.MathMan;
import com.plotsquared.fabric.util.FabricUtil;
import com.sk89q.worldedit.world.biome.BiomeType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

import java.util.Random;

final class DelegatePlotGenerator extends IndependentPlotGenerator {

    private final ChunkGenerator chunkGenerator;
    private final String world;

    public DelegatePlotGenerator(ChunkGenerator chunkGenerator, String world) {
        this.chunkGenerator = chunkGenerator;
        this.world = world;
    }

    @Override
    public void initialize(PlotArea area) {
    }

    @Override
    public BiomeType getBiome(final PlotArea settings, final int x, final int y, final int z) {
        return null;
    }

    @Override
    public String getName() {
        return this.chunkGenerator.getClass().getName();
    }

    @Override
    public PlotArea getNewPlotArea(String world, String id, PlotId min, PlotId max) {
        return PlotSquared.platform().defaultGenerator().getNewPlotArea(world, id, min, max);
    }

    @Override
    public void generateChunk(final ZeroedDelegateScopedQueueCoordinator result, PlotArea settings, boolean biomes) {
        ServerLevel world = FabricUtil.getWorld(this.world);
        Location min = result.getMin();
        int chunkX = min.getX() >> 4;
        int chunkZ = min.getZ() >> 4;
        Random random = new Random(MathMan.pair((short) chunkX, (short) chunkZ));
       try {
           if(chunkGenerator instanceof FabricPlotGenerator fabricPlotGenerator) {
               fabricPlotGenerator.checkLoaded(world);
           }
           return;
       } catch (Throwable ignored) {
       }
    }

}
