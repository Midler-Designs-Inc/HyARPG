package com.example.hyarpg.events;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Event_NPCPreSpawn {
    private final Holder<EntityStore> holder;
    private final Store<EntityStore> store;

    public Event_NPCPreSpawn(Holder<EntityStore> holder, Store<EntityStore> store) {
        this.holder = holder;
        this.store = store;
    }

    public Holder<EntityStore> getHolder() {
        return holder;
    }

    public Store<EntityStore> getStore() {
        return store;
    }
}
