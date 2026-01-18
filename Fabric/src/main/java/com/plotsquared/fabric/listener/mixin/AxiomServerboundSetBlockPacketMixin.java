package com.plotsquared.fabric.listener.mixin;


import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.moulberry.axiom.packets.AxiomServerboundPacket;
import com.moulberry.axiom.packets.AxiomServerboundSetBlock;
import com.plotsquared.fabric.FabricPlatform;
import com.plotsquared.fabric.listener.BlockEventListener;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Mixin(AxiomServerboundSetBlock.class)
public class AxiomServerboundSetBlockPacketMixin {

    @Shadow
    @Final
    private Map<BlockPos, BlockState> blocks;

    @Shadow
    @Final
    private Set<BlockPos> preventUpdatesAt;

    @WrapMethod(method = "handle")
    public void onHandle(MinecraftServer server, ServerPlayer player, Operation<Void> original) {
        for(Map.Entry<BlockPos, BlockState> entry : this.blocks.entrySet()) {
            InteractionResult result = FabricPlatform.PLATFORM.blockEventListener.blockCreateAxiom(player, player.serverLevel(),
                    entry.getKey(), entry.getValue(), null
            );
            if (result != InteractionResult.PASS) {
                preventUpdatesAt.add(entry.getKey());
            }
        }
        /*Iterator var32;
        Map.Entry entry;
        if (this.updateNeighbors) {
            var32 = this.blocks.entrySet().iterator();

            while (var32.hasNext()) {
                entry = (Map.Entry) var32.next();


                InteractionResult result = FabricPlatform.PLATFORM.blockEventListener.blockCreateAxiom(player, player.serverLevel(),
                        (BlockPos) entry.getKey(), (BlockState) entry.getValue(), null
                );
                if (result != InteractionResult.PASS) {
                    this.blocks.remove((BlockPos) entry.getKey());
                }
            }
        } else {
            var32 = this.blocks.entrySet().iterator();

            label113:
            while (true) {
                int by;
                int x;
                int y;
                int z;
                int cx;
                int cy;
                int cz;
                ServerLevel level;
                LevelChunk chunk;
                int sectionIndex;
                BlockPos blockPos;
                BlockState blockState;
                do {
                    do {
                        if (!var32.hasNext()) {
                            break label113;
                        }

                        entry = (Map.Entry) var32.next();
                        blockPos = (BlockPos) entry.getKey();
                        blockState = (BlockState) entry.getValue();
                        int bx = blockPos.getX();
                        by = blockPos.getY();
                        int bz = blockPos.getZ();
                        x = bx & 15;
                        y = by & 15;
                        z = bz & 15;
                        cx = bx >> 4;
                        cy = by >> 4;
                        cz = bz >> 4;
                        level = player.serverLevel();
                        chunk = level.getChunk(cx, cz);
                        chunk.setUnsaved(true);
                        sectionIndex = level.getSectionIndexFromSectionY(cy);
                        InteractionResult result = FabricPlatform.PLATFORM.blockEventListener.blockCreateAxiom(player, player.serverLevel(),
                                (BlockPos) entry.getKey(), (BlockState) entry.getValue(), null
                        );
                        if (result != InteractionResult.PASS) {
                            this.blocks.remove((BlockPos) entry.getKey());
                        }
                    } while (sectionIndex < 0);
                } while (sectionIndex >= level.getSectionsCount());
            }
        }*/
        original.call(server, player);
    }

}
