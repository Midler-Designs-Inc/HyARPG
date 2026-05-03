package com.example.hyarpg.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Event_PlayerDeath {
    private final Ref<EntityStore> ref;
    private final Store<EntityStore> store;
    private final DeathComponent deathComponent;

    public Event_PlayerDeath(Ref<EntityStore> ref, Store<EntityStore> store, DeathComponent deathComponent) {
        this.ref = ref;
        this.store = store;
        this.deathComponent = deathComponent;
    }

    public Ref<EntityStore> getRef() {
        return ref;
    }

    public Store<EntityStore> getStore() {
        return store;
    }

    public DeathComponent getDeathComponent() {
        return deathComponent;
    }
}
