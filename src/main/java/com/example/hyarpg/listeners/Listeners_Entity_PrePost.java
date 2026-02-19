package com.example.hyarpg.listeners;

// Hytale Imports
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.HolderSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

// Mod Imports
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_NPCPreSpawn;

// Java Imports
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

// Listen on Holder system to get NPC Pre Spawn and NPC Post Remove
public class Listeners_Entity_PrePost extends HolderSystem<EntityStore> {
    // Broadcast event when an NPC is about to be spawned
    @Override
    public void onEntityAdd(Holder<EntityStore> holder, AddReason reason, Store<EntityStore> store)
    {
        // broadcast event on the internal mod bus
        ModEventBus.post(new Event_NPCPreSpawn(holder, store));
    }

    // Broadcast event when an NPC is despawned
    @Override
    public void onEntityRemoved(Holder<EntityStore> holder, RemoveReason reason, Store<EntityStore> store) {}

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
                NPCEntity.getComponentType()
        );
    }
}