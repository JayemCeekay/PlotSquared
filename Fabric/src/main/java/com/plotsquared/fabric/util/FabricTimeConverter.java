package com.plotsquared.fabric.util;

import com.plotsquared.core.util.task.TaskTime;
import com.plotsquared.fabric.FabricPlatform;
import org.checkerframework.checker.index.qual.NonNegative;

public class FabricTimeConverter implements TaskTime.TimeConverter{

    private static final long MIN_MS_PER_TICKS = 50L;

    @Override
    public @NonNegative long msToTicks(@NonNegative final long ms) {
        return Math.max(1L, (long) (ms / Math.max(MIN_MS_PER_TICKS, FabricPlatform.SERVER.getAverageTickTimeNanos())));
    }

    @Override
    public @NonNegative long ticksToMs(@NonNegative final long ticks) {
        return Math.max(1L, (long) (ticks * Math.max(MIN_MS_PER_TICKS, FabricPlatform.SERVER.getAverageTickTimeNanos())));
    }

}
