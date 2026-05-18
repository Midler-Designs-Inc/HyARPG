package com.example.hyarpg.events;

// Hytale Imports
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record Event_DamageBlock(
    Ref<EntityStore> ref,
    Store<EntityStore> store,
    CommandBuffer<EntityStore> commandBuffer,
    DamageBlockEvent event
) {}
