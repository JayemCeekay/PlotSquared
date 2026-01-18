package com.plotsquared.fabric.listener.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.plotsquared.fabric.listener.event.GameEventEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @WrapMethod(method = "gameEvent")
    public void onGameEvent(Holder<GameEvent> holder, Vec3 vec3, GameEvent.Context context, Operation<Void> original) {
        InteractionResult result = GameEventEvent.EVENT.invoker().onGameEvent(holder.value(), vec3, context,
                (ServerLevel) (Object) this);
        if (result != InteractionResult.PASS) {
            return;
        }
        original.call(holder, vec3, context);
    }

}
