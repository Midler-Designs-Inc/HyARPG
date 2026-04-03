package com.example.hyarpg.listeners;

// Hytale Imports
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.SwitchActiveSlotEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_PlayerUtilitySwitch;

// Java Imports
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class Listeners_UtilitySlot extends EntityEventSystem<EntityStore, SwitchActiveSlotEvent> {

    public Listeners_UtilitySlot() {
        super(SwitchActiveSlotEvent.class);
    }

    @Override
    public void handle(
        int i,
        @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
        @NonNullDecl Store<EntityStore> store,
        @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
        @NonNullDecl SwitchActiveSlotEvent event
    ) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
        ModEventBus.post(new Event_PlayerUtilitySwitch(ref, store, commandBuffer, event));
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}