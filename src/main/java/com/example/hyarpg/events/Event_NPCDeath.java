package com.example.hyarpg.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Event_NPCDeath {
    private final Ref<EntityStore> ref;
    private final Store<EntityStore> store;

    public Event_NPCDeath(Ref<EntityStore> ref, Store<EntityStore> store) {
        this.ref = ref;
        this.store = store;
    }

    public Ref<EntityStore> getRef() {
        return ref;
    }

    public Store<EntityStore> getStore() {
        return store;
    }
}
