package com.example.hyarpg.events;

// Hytale Imports
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Event_PlayerInventoryItemSwapped {
    private final Ref<EntityStore> ref;
    private final Store<EntityStore> store;
    private final ItemContainer.ItemContainerChangeEvent changeEvent;
    private final short slot;
    private final ItemStack stackBefore;
    private final ItemStack stackAfter;

    public Event_PlayerInventoryItemSwapped(Ref<EntityStore> ref, Store<EntityStore> store, ItemContainer.ItemContainerChangeEvent changeEvent, short slot, ItemStack stackBefore, ItemStack stackAfter) {
        this.ref = ref;
        this.store = store;
        this.changeEvent = changeEvent;
        this.slot = slot;
        this.stackBefore = stackBefore;
        this.stackAfter = stackAfter;
    }

    public Ref<EntityStore> getRef() { return ref; }

    public Store<EntityStore> getStore() { return store; }

    public ItemContainer.ItemContainerChangeEvent getChangeEvent() { return changeEvent; }

    public short getSlot() { return slot; }

    public ItemStack getStackBefore() { return stackBefore; }

    public ItemStack getStackAfter() { return stackAfter; }
}
