package com.plotsquared.fabric.listener.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;

public interface EntityHandleInsidePortalCallback {
    Event<EntityHandleInsidePortalCallback> EVENT = EventFactory.createArrayBacked(
            EntityHandleInsidePortalCallback.class,
            callbacks -> ( entity) -> {
                for (EntityHandleInsidePortalCallback callback : callbacks) {
                    InteractionResult result = callback.entityHandleInsidePortalCallback(
                             entity
                    );
                    if (result != InteractionResult.PASS) {
                        return result;
                    }
                }
                return InteractionResult.PASS;
            }
    );


    InteractionResult entityHandleInsidePortalCallback(
            Entity entity
    );


}
