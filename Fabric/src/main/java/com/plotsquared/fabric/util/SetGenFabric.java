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
package com.plotsquared.fabric.util;

import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.generator.GeneratorWrapper;
import com.plotsquared.core.util.SetupUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.status.WorldGenContext;

public class SetGenFabric {

    public static void setGenerator(ServerLevel world) {
        PlotSquared.platform().setupUtils().updateGenerators(false);
        PlotSquared.get().removePlotAreas(world.dimension().location().getPath());
        ChunkGenerator gen = world.getChunkSource().chunkMap.worldGenContext.generator();
        //FabricPlatform.SERVER.registryAccess().registry(Registries.CHUNK_GENERATOR).get().get().decode().get().left().get()
        // .getFirst();
        String name = gen.getClass().getCanonicalName();
        boolean set = false;

        for (GeneratorWrapper<?> wrapper : SetupUtils.generators.values()) {
            ChunkGenerator newGen = (ChunkGenerator) wrapper.getPlatformGenerator();
            if (newGen == null) {
                newGen = (ChunkGenerator) wrapper;
            }
            world.getChunkSource().chunkMap.worldGenContext = new WorldGenContext(world, newGen,
                    world.getChunkSource().chunkMap.worldGenContext.structureManager(), world.getChunkSource().getLightEngine()
                    , world.getChunkSource().chunkMap.worldGenContext.mainThreadMailBox());
            // if (newGen.getClass().equals(gen.getClass())) {
            // Set generator
            //if (newGen instanceof FabricPlotGenerator fabricPlotGenerator) {
            //     fabricPlotGenerator.checkLoaded(world);
            //   }
            // end
            set = true;
            break;
            //  }
        }/*
        if (!set) {
            world.getPopulators()
                    .removeIf(blockPopulator -> blockPopulator instanceof FabricAugmentedGenerator);
        }*/
        PlotSquared.get().loadWorld(
                world.dimension().location().getPath(),
                PlotSquared.platform().getGenerator(world.dimension().location().getPath(), null)
        );
    }

}
