package com.example.hyarpg.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Event_PlayerInteraction {
    private final Ref<EntityStore> ref;
    private final Store<EntityStore> store;
    private final String interactionID;

    public Event_PlayerInteraction(Ref<EntityStore> ref, Store<EntityStore> store, String interactionID) {
        this.ref = ref;
        this.store = store;
        this.interactionID = interactionID;
    }

    public Ref<EntityStore> getRef() {
        return ref;
    }
    public Store<EntityStore> getStore() {
        return store;
    }
    public String getInteractionID() {
        return interactionID;
    }
}