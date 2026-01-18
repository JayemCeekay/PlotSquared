package com.plotsquared.fabric.listener.mixin;

import com.plotsquared.fabric.listener.event.MobSpawnEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public class MobMixin {

    @Inject(method = "finalizeSpawn", at = @At("HEAD"), cancellable = true)
    public void onFinalizeSpawn(
            ServerLevelAccessor serverLevelAccessor,
            DifficultyInstance difficultyInstance,
            MobSpawnType mobSpawnType,
            SpawnGroupData spawnGroupData,
            CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        InteractionResult result = MobSpawnEvent.EVENT.invoker().onMobSpawn(serverLevelAccessor,
                difficultyInstance,
                mobSpawnType,
                spawnGroupData,
                (Mob) (Object) this);
        if(result != InteractionResult.PASS) {
            cir.setReturnValue(null);
            cir.cancel();
        }
    }

}
