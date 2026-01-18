package com.plotsquared.fabric.listener.mixin;

import com.plotsquared.fabric.listener.event.EntityHandleInsidePortalCallback;
import com.plotsquared.fabric.listener.event.EntityOnInsideBlockCallback;
import com.plotsquared.fabric.listener.event.EntityTeleportToCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.block.state.BlockState;
import org.checkerframework.checker.units.qual.A;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    public abstract void remove(final Entity.RemovalReason removalReason);

    @Inject(method = "onInsideBlock", at = @At("HEAD"), cancellable = true)
    public void onInsideBlock(BlockState blockState, CallbackInfo ci) {
        InteractionResult result =
                EntityOnInsideBlockCallback.EVENT.invoker().entityOnInsideBlockCallback(
                        blockState,
                        (Entity) (Object) this
                );
        if (result != InteractionResult.PASS) {
            ci.cancel();
        }
    }

    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z", at = @At("HEAD"), cancellable = true)
    public void onTeleportTo(
            ServerLevel serverLevel,
            double d,
            double e,
            double f,
            Set<RelativeMovement> set,
            float g,
            float h,
            CallbackInfoReturnable<Boolean> cir
    ) {
        InteractionResult result =
                EntityTeleportToCallback.EVENT.invoker().entityTeleportToCallback(
                        serverLevel,
                        d,
                        e,
                        f,
                        set,
                        g,
                        h,
                        (Entity) (Object) this
                );
        if (result != InteractionResult.PASS) {
            cir.cancel();
        }
    }

    @Inject(method = "handlePortal", at = @At("HEAD"), cancellable = true)
    public void onHandleInsidePortal(CallbackInfo ci) {
        InteractionResult result =
                EntityHandleInsidePortalCallback.EVENT.invoker().entityHandleInsidePortalCallback(
                        (Entity) (Object) this
                );
        if (result != InteractionResult.PASS) {
            ci.cancel();
        }

    }


}
