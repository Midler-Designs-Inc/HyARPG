package com.example.hyarpg.listeners;

// Hytale Imports
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.InventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.*;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.*;

// Checker annotations
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class Listeners_PlayerInventory extends EntityEventSystem<EntityStore, InventoryChangeEvent> {

    public Listeners_PlayerInventory() {
        super(InventoryChangeEvent.class);
    }

    // Only fire for entities that have a PlayerRef — same filter pattern as Listeners_Death
    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void handle(int index, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl InventoryChangeEvent event) {
        Transaction transaction = event.getTransaction();
        if (!transaction.succeeded()) return;

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        ComponentType<EntityStore, ? extends InventoryComponent> componentType = event.getComponentType();

        if (componentType == InventoryComponent.Armor.getComponentType()) {
            handleArmorChange(ref, store, event, transaction);
        } else if (isEverythingSection(componentType)) {
            handleGeneralChange(ref, store, event, transaction);
        }
    }

    // ── Armor change handler ─────────────────────────────────────────────────────
    private void handleArmorChange(Ref<EntityStore> ref, Store<EntityStore> store, InventoryChangeEvent changeEvent, Transaction transaction) {
        if (transaction instanceof MoveTransaction<?> moveTx) {

            if (moveTx.getMoveType() == MoveType.MOVE_TO_SELF) {
                Transaction rawAddTx = moveTx.getAddTransaction();
                if (!(rawAddTx instanceof SlotTransaction addTx)) return;

                short slot       = addTx.getSlot();
                ItemStack before = addTx.getSlotBefore();
                ItemStack after  = addTx.getSlotAfter();

                if (!ItemStack.isEmpty(before))
                    ModEventBus.post(new Event_PlayerInventoryItemUnEquip(ref, store, changeEvent, slot, before));
                if (!ItemStack.isEmpty(after))
                    ModEventBus.post(new Event_PlayerInventoryItemEquip(ref, store, changeEvent, slot, after));

            } else if (moveTx.getMoveType() == MoveType.MOVE_FROM_SELF) {
                SlotTransaction removeTx = moveTx.getRemoveTransaction();
                short slot       = removeTx.getSlot();
                ItemStack before = removeTx.getSlotBefore();
                ItemStack after  = removeTx.getSlotAfter();

                if (!ItemStack.isEmpty(before))
                    ModEventBus.post(new Event_PlayerInventoryItemUnEquip(ref, store, changeEvent, slot, before));
                if (!ItemStack.isEmpty(after))
                    ModEventBus.post(new Event_PlayerInventoryItemEquip(ref, store, changeEvent, slot, after));
            }

        } else if (transaction instanceof ItemStackSlotTransaction slotTx) {
            ItemStack before = slotTx.getSlotBefore();
            ItemStack after  = slotTx.getSlotAfter();
            short slot       = slotTx.getSlot();

            if (!ItemStack.isEmpty(before) && ItemStack.isEmpty(after))
                ModEventBus.post(new Event_PlayerInventoryItemUnEquip(ref, store, changeEvent, slot, before));
            else if (ItemStack.isEmpty(before) && !ItemStack.isEmpty(after))
                ModEventBus.post(new Event_PlayerInventoryItemEquip(ref, store, changeEvent, slot, after));
            else if (!ItemStack.isEmpty(before) && !ItemStack.isEmpty(after)) {
                ModEventBus.post(new Event_PlayerInventoryItemUnEquip(ref, store, changeEvent, slot, before));
                ModEventBus.post(new Event_PlayerInventoryItemEquip(ref, store, changeEvent, slot, after));
            }
        }
    }

    // ── General inventory change handler ─────────────────────────────────────────
    private void handleGeneralChange(Ref<EntityStore> ref, Store<EntityStore> store, InventoryChangeEvent changeEvent, Transaction transaction) {

        if (transaction instanceof MoveTransaction<?> moveTx) {

            if (moveTx.getMoveType() == MoveType.MOVE_FROM_SELF) {
                // Originating slot was the player's inventory
                SlotTransaction removeTx = (SlotTransaction) moveTx.getRemoveTransaction();

                ItemStack removed = removeTx.getSlotBefore();
                ItemStack added   = removeTx.getSlotAfter();
                short slot        = removeTx.getSlot();

                if (added != null && removed != null) {
                    ModEventBus.post(new Event_PlayerInventoryItemAdded(ref, store, changeEvent, slot, added));
                    ModEventBus.post(new Event_PlayerInventoryItemRemoved(ref, store, changeEvent, slot, removed));
                } else if (added != null)
                    ModEventBus.post(new Event_PlayerInventoryItemAdded(ref, store, changeEvent, slot, added));
                else if (removed != null)
                    ModEventBus.post(new Event_PlayerInventoryItemRemoved(ref, store, changeEvent, slot, removed));
            }

            if (moveTx.getMoveType() == MoveType.MOVE_TO_SELF) {
                // Originating slot was outside the player's inventory
                Transaction rawAddTx = moveTx.getAddTransaction();

                if (rawAddTx instanceof ItemStackTransaction itemTx) {
                    // Bulk take-all: wraps multiple slot transactions
                    for (ItemStackSlotTransaction slotTx : itemTx.getSlotTransactions()) {
                        short slot       = slotTx.getSlot();
                        ItemStack before = slotTx.getSlotBefore();
                        ItemStack after  = slotTx.getSlotAfter();

                        boolean beforeEmpty = ItemStack.isEmpty(before);
                        boolean afterEmpty  = ItemStack.isEmpty(after);

                        if (beforeEmpty && !afterEmpty)
                            ModEventBus.post(new Event_PlayerInventoryItemAdded(ref, store, changeEvent, slot, after));
                        else if (!beforeEmpty && afterEmpty)
                            ModEventBus.post(new Event_PlayerInventoryItemRemoved(ref, store, changeEvent, slot, before));
                        else if (!beforeEmpty) {
                            ModEventBus.post(new Event_PlayerInventoryItemAdded(ref, store, changeEvent, slot, after));
                            ModEventBus.post(new Event_PlayerInventoryItemRemoved(ref, store, changeEvent, slot, before));
                        }
                    }
                } else {
                    // Single-slot move
                    SlotTransaction addTx = (SlotTransaction) moveTx.getAddTransaction();

                    ItemStack removed = addTx.getSlotBefore();
                    ItemStack added   = addTx.getSlotAfter();
                    short slot        = addTx.getSlot();

                    if (added != null && removed != null) {
                        ModEventBus.post(new Event_PlayerInventoryItemAdded(ref, store, changeEvent, slot, added));
                        ModEventBus.post(new Event_PlayerInventoryItemRemoved(ref, store, changeEvent, slot, removed));
                    } else if (added != null)
                        ModEventBus.post(new Event_PlayerInventoryItemAdded(ref, store, changeEvent, slot, added));
                    else if (removed != null)
                        ModEventBus.post(new Event_PlayerInventoryItemRemoved(ref, store, changeEvent, slot, removed));
                }
            }

        } else if (transaction instanceof ItemStackTransaction itemTx) {
            for (ItemStackSlotTransaction slotTx : itemTx.getSlotTransactions()) {
                short slot       = slotTx.getSlot();
                ItemStack before = slotTx.getSlotBefore();
                ItemStack after  = slotTx.getSlotAfter();

                boolean beforeEmpty = ItemStack.isEmpty(before);
                boolean afterEmpty  = ItemStack.isEmpty(after);

                if (beforeEmpty && !afterEmpty)
                    ModEventBus.post(new Event_PlayerInventoryItemAdded(ref, store, changeEvent, slot, after));
                else if (!beforeEmpty && afterEmpty)
                    ModEventBus.post(new Event_PlayerInventoryItemRemoved(ref, store, changeEvent, slot, before));
                else if (!beforeEmpty && !afterEmpty) {
                    // fire added if item type changed (swap) or quantity increased (stack merge/pickup)
                    if (!before.getItemId().equals(after.getItemId())) {
                        ModEventBus.post(new Event_PlayerInventoryItemAdded(ref, store, changeEvent, slot, after));
                        ModEventBus.post(new Event_PlayerInventoryItemRemoved(ref, store, changeEvent, slot, before));
                    } else if (after.getQuantity() > before.getQuantity()) {
                        ModEventBus.post(new Event_PlayerInventoryItemAdded(ref, store, changeEvent, slot, after));
                    }
                }
            }
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────────────

    /**
     * Returns true for every inventory section the old getCombinedEverything() covered.
     * Armor is excluded — it has its own handler above.
     */
    private static boolean isEverythingSection(
            ComponentType<EntityStore, ? extends InventoryComponent> type) {
        return type == InventoryComponent.Hotbar.getComponentType()
                || type == InventoryComponent.Storage.getComponentType()
                || type == InventoryComponent.Utility.getComponentType()
                || type == InventoryComponent.Backpack.getComponentType()
                || type == InventoryComponent.Tool.getComponentType();
    }
}