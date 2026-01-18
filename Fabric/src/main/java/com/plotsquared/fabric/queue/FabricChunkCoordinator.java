package com.plotsquared.fabric.queue;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.queue.ChunkCoordinator;
import com.plotsquared.core.queue.subscriber.ProgressSubscriber;
import com.plotsquared.core.util.task.PlotSquaredTask;
import com.plotsquared.core.util.task.TaskManager;
import com.plotsquared.core.util.task.TaskTime;
import com.plotsquared.fabric.FabricPlatform;
import com.plotsquared.fabric.util.FabricUtil;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.world.World;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class FabricChunkCoordinator extends ChunkCoordinator {

    private final List<ProgressSubscriber> progressSubscribers = new LinkedList<>();

    private final Queue<BlockVector2> requestedChunks;
    private final Queue<ChunkAccess> availableChunks;
    private final long maxIterationTime;
    private final Consumer<BlockVector2> chunkConsumer;
    private final ServerLevel serverLevel;
    private final Runnable whenDone;
    private final Consumer<Throwable> throwableConsumer;
    private final boolean unloadAfter;
    private final int totalSize;
    private final AtomicInteger expectedSize;
    private final AtomicInteger loadingChunks = new AtomicInteger();
    private final boolean forceSync;

    private int batchSize;
    private PlotSquaredTask task;
    private volatile boolean shouldCancel;
    private boolean finished;

    @Inject
    private FabricChunkCoordinator(
            @Assisted final long maxIterationTime,
            @Assisted final int initialBatchSize,
            @Assisted final @NonNull Consumer<BlockVector2> chunkConsumer,
            @Assisted final @NonNull World world,
            @Assisted final @NonNull Collection<BlockVector2> requestedChunks,
            @Assisted final @NonNull Runnable whenDone,
            @Assisted final @NonNull Consumer<Throwable> throwableConsumer,
            @Assisted("unloadAfter") final boolean unloadAfter,
            @Assisted final @NonNull Collection<ProgressSubscriber> progressSubscribers,
            @Assisted("forceSync") final boolean forceSync
    ) {
        this.requestedChunks = new LinkedBlockingQueue<>(requestedChunks);
        this.availableChunks = new LinkedBlockingQueue<>();
        this.totalSize = requestedChunks.size();
        this.expectedSize = new AtomicInteger(this.totalSize);
        this.batchSize = initialBatchSize;
        this.chunkConsumer = chunkConsumer;
        this.maxIterationTime = maxIterationTime;
        this.whenDone = whenDone;
        this.throwableConsumer = throwableConsumer;
        this.unloadAfter = unloadAfter;
        this.serverLevel = FabricUtil.getWorld(world.getNameUnsafe());
        this.progressSubscribers.addAll(progressSubscribers);
        this.forceSync = forceSync;
    }

    @Override
    public void start() {
        if (!forceSync) {
            // Request initial batch
            this.requestBatch();
            // Wait until next tick to give the chunks a chance to be loaded
            TaskManager.runTaskLater(() -> task = TaskManager.runTaskRepeat(this, TaskTime.ticks(1)), TaskTime.ticks(1));
        } else {
            try {
                while (!shouldCancel && !requestedChunks.isEmpty()) {
                    chunkConsumer.accept(requestedChunks.poll());
                }
            } catch (Throwable t) {
                throwableConsumer.accept(t);
            } finally {
                finish();
            }
        }
    }

    @Override
    public void cancel() {
        shouldCancel = true;
    }

    private void finish() {
        try {
            this.whenDone.run();
        } catch (final Throwable throwable) {
            this.throwableConsumer.accept(throwable);
        } finally {
            for (final ProgressSubscriber subscriber : this.progressSubscribers) {
                subscriber.notifyEnd();
            }
            if (task != null) {
                task.cancel();
            }
            finished = true;
        }
    }

    @Override
    public void run() {
        if (shouldCancel) {
            if (unloadAfter) {
                ChunkAccess chunk;
                while ((chunk = availableChunks.poll()) != null) {
                    freeChunk(chunk);
                }
            }
            finish();
            return;
        }

        ChunkAccess chunk = this.availableChunks.poll();
        if (chunk == null) {
            if (this.availableChunks.isEmpty()) {
                if (this.requestedChunks.isEmpty() && loadingChunks.get() == 0) {
                    finish();
                } else {
                    requestBatch();
                }
            }
            return;
        }
        long[] iterationTime = new long[2];
        int processedChunks = 0;
        do {
            final long start = System.currentTimeMillis();
            try {
                this.chunkConsumer.accept(BlockVector2.at(chunk.getPos().x, chunk.getPos().z));
            } catch (final Throwable throwable) {
                this.throwableConsumer.accept(throwable);
            }
            if (unloadAfter) {
                this.freeChunk(chunk);
            }
            processedChunks++;
            final long end = System.currentTimeMillis();
            // Update iteration time
            iterationTime[0] = iterationTime[1];
            iterationTime[1] = end - start;
        } while (iterationTime[0] + iterationTime[1] < this.maxIterationTime * 2 && (chunk = availableChunks.poll()) != null);
        if (processedChunks < this.batchSize) {
            // Adjust batch size based on the amount of processed chunks per tick
            this.batchSize = processedChunks;
        }
        final int expected = this.expectedSize.addAndGet(-processedChunks);
        if (expected <= 0) {
            finish();
        } else {
            if (this.availableChunks.size() < processedChunks) {
                /*final double progress = ((double) totalSize - (double) expected) / (double) totalSize;
                for (final ProgressSubscriber subscriber : this.progressSubscribers) {
                    subscriber.notifyProgress(this, progress);
                }*/
                this.requestBatch();
            }
        }
    }

    /**
     * Requests a batch of chunks to be loaded
     */
    private void requestBatch() {
        BlockVector2 chunk;
        for (int i = 0; i < this.batchSize && (chunk = this.requestedChunks.poll()) != null; i++) {
            // This required PaperLib to be bumped to version 1.0.4 to mark the request as urgent
            loadingChunks.incrementAndGet();
            /*serverLevel.getChunkSource().getChunkFuture(chunk.getX(), chunk.getZ(), ChunkStatus.FULL, true)
                    .whenComplete((chunkObject, throwable) -> {
                        loadingChunks.decrementAndGet();
                        if (throwable != null) {
                            throwable.printStackTrace();
                            // We want one less because this couldn't be processed
                            this.expectedSize.decrementAndGet();
                        } else if (PlotSquared.get().isMainThread(FabricPlatform.SERVER.getRunningThread())) {
                            this.processChunk(chunkObject.left().get());
                        } else {
                            TaskManager.runTask(() -> this.processChunk(chunkObject.left().get()));
                        }
                    });*/
            final BlockVector2 finalChunk = chunk;
            serverLevel.getChunkSource()
                    .getChunkFuture(chunk.x(), chunk.z(), ChunkStatus.FULL, true)
                    .whenComplete((chunkObject, throwable) -> {
                        loadingChunks.decrementAndGet();

                        if (throwable != null) {
                            throwable.printStackTrace();
                            this.expectedSize.decrementAndGet();
                            return;
                        }

                        if (!chunkObject.isSuccess()) {
                            System.err.println("Chunk at (" + finalChunk.x() + ", " + finalChunk.z() + ") is not generated. " +
                                    "Generating...");

                            // Force the chunk to generate synchronously in a worker thread
                            TaskManager.runTask(() -> {
                                ChunkAccess generatedChunk = serverLevel.getChunk(
                                        finalChunk.x(),
                                        finalChunk.z(),
                                        ChunkStatus.FULL,
                                        true
                                );
                                System.out.println("Chunk at (" + finalChunk.x() + ", " + finalChunk.z() + ") has been " +
                                        "generated.");
                                this.processChunk(generatedChunk);
                            });
                        } else {
                            ChunkAccess chunkAccess = chunkObject.orElseThrow(() -> new RuntimeException("Chunk was null"));
                            if (PlotSquared.get().isMainThread(FabricPlatform.SERVER.getRunningThread())) {
                                this.processChunk(chunkAccess);
                            } else {
                                TaskManager.runTask(() -> this.processChunk(chunkAccess));
                            }
                        }
                    });
        }
    }

    /**
     * Once a chunk has been loaded, process it (add a plugin ticket and add to
     * available chunks list). It is important that this gets executed on the
     * server's main thread.
     */
    private void processChunk(final @NonNull ChunkAccess chunk) {
        /* Chunk#isLoaded does not necessarily return true shortly after PaperLib#getChunkAtAsync completes, but the chunk is
        still loaded.
        if (!chunk.isLoaded()) {
            throw new IllegalArgumentException(String.format("Chunk %d;%d is is not loaded", chunk.getX(), chunk.getZ());
        }*/
        if (finished) {
            return;
        }
        serverLevel.getChunkSource().addRegionTicket(TicketType.UNKNOWN, chunk.getPos(), 11, chunk.getPos());
        this.availableChunks.add(chunk);
    }

    /**
     * Once a chunk has been used, free it up for unload by removing the plugin ticket
     */
    private void freeChunk(final @NonNull ChunkAccess chunk) {
        if (!serverLevel.isLoaded(chunk.getPos().getWorldPosition())) {
            throw new IllegalArgumentException(String.format(
                    "Chunk %d;%d is is not loaded", chunk.getPos().x,
                    chunk.getPos().z
            ));
        }
        serverLevel.getChunkSource().removeRegionTicket(TicketType.UNKNOWN, chunk.getPos(), 11, chunk.getPos());
    }

    @Override
    public int getRemainingChunks() {
        return this.expectedSize.get();
    }

    @Override
    public int getTotalChunks() {
        return this.totalSize;
    }

    /**
     * Subscribe to coordinator progress updates
     *
     * @param subscriber Subscriber
     */
    public void subscribeToProgress(final @NonNull ProgressSubscriber subscriber) {
        this.progressSubscribers.add(subscriber);
    }

}
