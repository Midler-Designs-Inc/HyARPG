package com.example.hyarpg.utils;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.role.support.WorldSupport;

public final class NPCUtils {

    private NPCUtils() {
    }

    public static boolean isFriendly(
        Ref<EntityStore> observerRef,
        Ref<EntityStore> targetRef,
        ComponentAccessor<EntityStore> accessor
    ) {
        WorldSupport worldSupport = accessor.getComponent(observerRef, WorldSupport.getComponentType());
        if (worldSupport == null) {
            throw new IllegalStateException("NPC is missing its WorldSupport component");
        }

        Attitude attitude = worldSupport.getAttitude(observerRef, targetRef, accessor);
        return attitude == Attitude.FRIENDLY || attitude == Attitude.REVERED;
    }
}
