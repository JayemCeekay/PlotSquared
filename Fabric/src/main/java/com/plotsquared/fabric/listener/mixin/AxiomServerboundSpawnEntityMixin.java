package com.plotsquared.fabric.listener.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.moulberry.axiom.AxiomServer;
import com.moulberry.axiom.packets.AxiomServerboundSpawnEntity;
import com.moulberry.axiom.restrictions.AxiomPermission;
import net.luckperms.api.LuckPermsProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AxiomServerboundSpawnEntity.class)
public class AxiomServerboundSpawnEntityMixin {


    @WrapMethod(method = "handle")
    public void onHandle(MinecraftServer server, ServerPlayer player, Operation<Void> original){
        if(AxiomServer.hasPermission(player, AxiomPermission.ENTITY)) {
            original.call(server, player);
        }
    }

}
