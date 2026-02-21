package com.example.hyarpg.events;

// Hytale Imports
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ActionType;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;

public class Event_PlayerInventoryChange {
    private final Ref<EntityStore> ref;
    private final Store<EntityStore> store;
    private final ItemContainer.ItemContainerChangeEvent changeEvent;
    private final List<ItemStackSlotTransaction> slotTransactions;

    public Event_PlayerInventoryChange(Ref<EntityStore> ref, Store<EntityStore> store, ItemContainer.ItemContainerChangeEvent changeEvent, List<ItemStackSlotTransaction> slotTransactions) {
        this.ref = ref;
        this.store = store;
        this.changeEvent = changeEvent;
        this.slotTransactions = slotTransactions;
    }

    public Ref<EntityStore> getRef() { return ref; }

    public Store<EntityStore> getStore() { return store; }

    public ItemContainer.ItemContainerChangeEvent getChangeEvent() { return changeEvent; }

    public List<ItemStackSlotTransaction> getSlotTransactions() { return slotTransactions; }
}
