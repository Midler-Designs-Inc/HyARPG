package com.example.hyarpg.listeners;

// Hytale Imports
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.*;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.*;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;

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

        player.getInventory().getCombinedEverything().registerChangeEvent(changeEvent -> {
            Transaction transaction = changeEvent.transaction();
            if (!transaction.succeeded()) return;

            if (transaction instanceof MoveTransaction<?> moveTx) {
                SlotTransaction removeTx = moveTx.getRemoveTransaction();
                if (!(removeTx instanceof ItemStackSlotTransaction removeSlotTx)) return;

                short slot = removeSlotTx.getSlot();
                ItemStack movedItem = removeSlotTx.getSlotBefore();
                ItemContainer destContainer = moveTx.getOtherContainer();
                ItemContainer armorContainer = player.getInventory().getArmor();

                if (moveTx.getMoveType() == MoveType.MOVE_FROM_SELF) {
                    boolean isEquip = destContainer == armorContainer;
                    if (isEquip)
                        ModEventBus.post(new Event_PlayerInventoryItemEquip(ref, store, changeEvent, slot, movedItem));
                    else
                        ModEventBus.post(new Event_PlayerInventoryItemSwapped(ref, store, changeEvent, slot, movedItem, null));
                } else if (moveTx.getMoveType() == MoveType.MOVE_TO_SELF) {
                    boolean isUnequip = destContainer == armorContainer; // armor is now the "other" container
                    if (isUnequip)
                        ModEventBus.post(new Event_PlayerInventoryItemUnEquip(ref, store, changeEvent, slot, movedItem));
                }

                return;
            }

            List<ItemStackSlotTransaction> slotTransactions = null;
            if (transaction instanceof ItemStackTransaction itemTx) slotTransactions = itemTx.getSlotTransactions();
            else if (transaction instanceof ItemStackSlotTransaction slotTx) slotTransactions = List.of(slotTx);
            if (slotTransactions == null) return;

            for (ItemStackSlotTransaction tx : slotTransactions) {
                if (!tx.succeeded()) continue;

                short slot = tx.getSlot();
                ItemStack before = tx.getSlotBefore();
                ItemStack after = tx.getSlotAfter();

                boolean beforeEmpty = ItemStack.isEmpty(before);
                boolean afterEmpty = ItemStack.isEmpty(after);

                if (beforeEmpty && !afterEmpty)
                    ModEventBus.post(new Event_PlayerInventoryItemAdded(ref, store, changeEvent, slot, after));
                else if (!beforeEmpty && afterEmpty)
                    ModEventBus.post(new Event_PlayerInventoryItemRemoved(ref, store, changeEvent, slot, before));
                else if (!beforeEmpty && !afterEmpty && before.getItem() == after.getItem() && after.getQuantity() > before.getQuantity())
                    ModEventBus.post(new Event_PlayerInventoryItemAdded(ref, store, changeEvent, slot, after));
            }
        });
    }
}