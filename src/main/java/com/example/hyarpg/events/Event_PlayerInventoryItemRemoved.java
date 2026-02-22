package com.example.hyarpg.events;

// Hytale Imports
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Event_PlayerInventoryItemRemoved {
    private final Ref<EntityStore> ref;
    private final Store<EntityStore> store;
    private final ItemContainer.ItemContainerChangeEvent changeEvent;
    private final short slot;
    private final ItemStack stack;

    public Event_PlayerInventoryItemRemoved(Ref<EntityStore> ref, Store<EntityStore> store, ItemContainer.ItemContainerChangeEvent changeEvent, short slot, ItemStack stack) {
        this.ref = ref;
        this.store = store;
        this.changeEvent = changeEvent;
        this.slot = slot;
        this.stack = stack;
    }

    public Ref<EntityStore> getRef() { return ref; }

    public Store<EntityStore> getStore() { return store; }

    public ItemContainer.ItemContainerChangeEvent getChangeEvent() { return changeEvent; }

    public short getSlot() { return slot; }

    public ItemStack getStack() { return stack; }
}
