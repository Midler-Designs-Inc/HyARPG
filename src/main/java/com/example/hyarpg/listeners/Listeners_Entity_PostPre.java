package com.example.hyarpg.listeners;

// Hytale Imports
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_NPCSpawn;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

// Listen on Holder system to get NPC Post Spawn and NPC Pre Remove
public class Listeners_Entity_PostPre extends RefSystem<EntityStore> {

    // Broadcast event when an NPC is spawned
    @Override
    public void onEntityAdded(Ref<EntityStore> ref, AddReason reason, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        // broadcast event on the internal mod bus
        ModEventBus.post(new Event_NPCSpawn(ref, store, commandBuffer));
    }

    // Broadcast event when an NPC is about to be despawned
    @Override
    public void onEntityRemove(Ref<EntityStore> ref, RemoveReason reason, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {}

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            NPCEntity.getComponentType()
        );
    }
}
