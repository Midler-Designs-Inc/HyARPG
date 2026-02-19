package com.example.hyarpg.events;

// Hytale Imports
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Event_NPCSpawn {
    private final Ref<EntityStore> ref;
    private final Store<EntityStore> store;
    private final CommandBuffer<EntityStore> commandBuffer;

    public Event_NPCSpawn(Ref<EntityStore> ref, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        this.ref = ref;
        this.store = store;
        this.commandBuffer = commandBuffer;
    }

    public Ref<EntityStore> getRef() { return ref; }

    public Store<EntityStore> getStore() { return store; }

    public CommandBuffer<EntityStore> getCommandBuffer() { return commandBuffer; }
}
