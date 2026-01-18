package com.plotsquared.fabric.util;

import com.google.inject.Singleton;
import com.plotsquared.core.util.ChunkManager;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.regions.CuboidRegion;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.concurrent.CompletableFuture;
@Singleton
public class FabricChunkManager extends ChunkManager {

    public static boolean isIn(CuboidRegion region, int x, int z) {
        return x >= region.getMinimumPoint().x() && x <= region.getMaximumPoint().x() && z >= region
                .getMinimumPoint()
                .z() && z <= region
                .getMaximumPoint().z();
    }

    @Override
    public CompletableFuture<?> loadChunk(String world, BlockVector2 chunkLoc, boolean force) {
        return FabricUtil.getWorld(world).getChunkSource().getChunkFuture(
                chunkLoc.x(),
                chunkLoc.z(),
                ChunkStatus.FULL,
                force
        );
    }

}
