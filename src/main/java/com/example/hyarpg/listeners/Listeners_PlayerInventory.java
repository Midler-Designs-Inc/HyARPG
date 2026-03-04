package com.example.hyarpg.listeners;

// Hytale Imports
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.*;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.*;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.awt.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// Mod Imports

// Java Imports

public class Listeners_PlayerInventory {

    public Listeners_PlayerInventory() {
        ModEventBus.register(Event_PlayerReady.class, this::onPlayerReady);
    }

    private void onPlayerReady(Event_PlayerReady event) {
        Player player = event.getPlayer();
        Ref<EntityStore> ref = player.getReference();
        Store<EntityStore> store = event.getWorld().getEntityStore().getStore();

        World world = event.getWorld();
        world.execute(() -> {
            // Bind listeners for players armor containers
            player.getInventory().getArmor().registerChangeEvent(changeEvent -> {
                Transaction transaction = changeEvent.transaction();
                if (!transaction.succeeded()) return;

                if (transaction instanceof MoveTransaction<?> moveTx) {
                    SlotTransaction removeTx = moveTx.getRemoveTransaction();
                    if (!(removeTx instanceof ItemStackSlotTransaction removeSlotTx)) return;

                    short slot = removeSlotTx.getSlot();
                    ItemStack movedItem = removeSlotTx.getSlotBefore();

                    if (moveTx.getMoveType() == MoveType.MOVE_FROM_SELF)
                        ModEventBus.post(new Event_PlayerInventoryItemUnEquip(ref, store, changeEvent, slot, movedItem));
                    else if (moveTx.getMoveType() == MoveType.MOVE_TO_SELF)
                        ModEventBus.post(new Event_PlayerInventoryItemEquip(ref, store, changeEvent, slot, movedItem));

                } else if (transaction instanceof ItemStackSlotTransaction slotTx) {
                    // item slot transaction
                    ItemStack before = slotTx.getSlotBefore();
                    ItemStack after = slotTx.getSlotAfter();
                    short slot = slotTx.getSlot();

                    if (!ItemStack.isEmpty(before) && ItemStack.isEmpty(after))
                        ModEventBus.post(new Event_PlayerInventoryItemUnEquip(ref, store, changeEvent, slot, before));
                    else if (ItemStack.isEmpty(before) && !ItemStack.isEmpty(after))
                        ModEventBus.post(new Event_PlayerInventoryItemEquip(ref, store, changeEvent, slot, after));
                }
            });

            // Bind listeners for everything else
            player.getInventory().getCombinedEverything().registerChangeEvent(changeEvent -> {
                Transaction transaction = changeEvent.transaction();
                if (!transaction.succeeded()) return;

                // two-sided event (two item slots are involved even if one is empty
                if (transaction instanceof MoveTransaction<?> moveTx) {
                    // originating slot was the players inventory (doesn't for hotbar though)
                    if (moveTx.getMoveType() == MoveType.MOVE_FROM_SELF) {
                        // get the removeTX transaction
                        SlotTransaction removeTx = (SlotTransaction) moveTx.getRemoveTransaction();

                        // get the involved items
                        ItemStack removed = removeTx.getSlotBefore();
                        ItemStack added = removeTx.getSlotAfter();
                        short slot = removeTx.getSlot();

                        // check if we added, removed or swapped something
                        if (added != null && removed != null) {
                            ModEventBus.post(new Event_PlayerInventoryItemAdded(ref, store, changeEvent, slot, added));
                            ModEventBus.post(new Event_PlayerInventoryItemRemoved(ref, store, changeEvent, slot, removed));
                        } else if (added != null)
                            ModEventBus.post(new Event_PlayerInventoryItemAdded(ref, store, changeEvent, slot, added));
                        else if (removed != null)
                            ModEventBus.post(new Event_PlayerInventoryItemRemoved(ref, store, changeEvent, slot, removed));
                    }

                    // originating slot was not the players inventory (doesn't for hotbar though)
                    if (moveTx.getMoveType() == MoveType.MOVE_TO_SELF) {
                        Transaction rawAddTx = (Transaction) moveTx.getAddTransaction();

                        // bulk take-all returns ItemStackTransaction
                        if (rawAddTx instanceof ItemStackTransaction itemTx) {
                            for (ItemStackSlotTransaction slotTx : itemTx.getSlotTransactions()) {
                                short slot = slotTx.getSlot();
                                ItemStack before = slotTx.getSlotBefore();
                                ItemStack after = slotTx.getSlotAfter();

                                boolean beforeEmpty = ItemStack.isEmpty(before);
                                boolean afterEmpty = ItemStack.isEmpty(after);

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
                            // get the addTX transaction
                            SlotTransaction addTx = (SlotTransaction) moveTx.getAddTransaction();

                            // get the involved items
                            ItemStack removed = addTx.getSlotBefore();
                            ItemStack added = addTx.getSlotAfter();
                            short slot = addTx.getSlot();

                            // check if we added, removed or swapped something
                            if (added != null && removed != null) {
                                ModEventBus.post(new Event_PlayerInventoryItemAdded(ref, store, changeEvent, slot, added));
                                ModEventBus.post(new Event_PlayerInventoryItemRemoved(ref, store, changeEvent, slot, removed));
                            } else if (added != null)
                                ModEventBus.post(new Event_PlayerInventoryItemAdded(ref, store, changeEvent, slot, added));
                            else if (removed != null)
                                ModEventBus.post(new Event_PlayerInventoryItemRemoved(ref, store, changeEvent, slot, removed));
                        }
                    }
                }
                else if (transaction instanceof ItemStackTransaction itemTx) {
                    for (ItemStackSlotTransaction slotTx : itemTx.getSlotTransactions()) {
                        short slot = slotTx.getSlot();
                        ItemStack before = slotTx.getSlotBefore();
                        ItemStack after = slotTx.getSlotAfter();

                        boolean beforeEmpty = ItemStack.isEmpty(before);
                        boolean afterEmpty = ItemStack.isEmpty(after);

                        if (beforeEmpty && !afterEmpty)
                            ModEventBus.post(new Event_PlayerInventoryItemAdded(ref, store, changeEvent, slot, after));
                        else if (!beforeEmpty && afterEmpty)
                            ModEventBus.post(new Event_PlayerInventoryItemRemoved(ref, store, changeEvent, slot, before));
                        else if (!beforeEmpty && !afterEmpty)
                            ModEventBus.post(new Event_PlayerInventoryItemAdded(ref, store, changeEvent, slot, after));
                    }
                }
            });
        });
    }

}