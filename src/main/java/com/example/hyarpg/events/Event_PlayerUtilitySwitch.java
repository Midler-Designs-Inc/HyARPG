package com.example.hyarpg.events;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.event.events.ecs.SwitchActiveSlotEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Event_PlayerUtilitySwitch {

    private final Ref<EntityStore> playerRef;
    private final Store<EntityStore> store;
    private final CommandBuffer<EntityStore> commandBuffer;
    private final SwitchActiveSlotEvent event;

    public Event_PlayerUtilitySwitch(
        Ref<EntityStore> playerRef,
        Store<EntityStore> store,
        CommandBuffer<EntityStore> commandBuffer,
        SwitchActiveSlotEvent event
    ) {
        this.playerRef = playerRef;
        this.store = store;
        this.commandBuffer = commandBuffer;
        this.event = event;
    }

    public Ref<EntityStore> getPlayerRef() {
        return playerRef;
    }

    public Store<EntityStore> getStore() {
        return store;
    }

    public CommandBuffer<EntityStore> getCommandBuffer() {
        return commandBuffer;
    }

    public SwitchActiveSlotEvent getEvent() {
        return event;
    }
}