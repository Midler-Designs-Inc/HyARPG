package com.example.hyarpg.listeners;

// Hytale Imports
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_PlayerInventoryChange;
import com.example.hyarpg.events.Event_PlayerReady;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.*;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.AddItemInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
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

        // Hook inventory changes to stamp item levels
        player.getInventory().getCombinedEverything().registerChangeEvent(changeEvent -> {
            player.sendMessage(Message.raw("Base level boi"));

            // get the transaction and make sure it succeeded
            Transaction transaction = changeEvent.transaction();
            if (!transaction.succeeded()) return;

            // get the transactions depending on event type??
            List<ItemStackSlotTransaction> slotTransactions = null;
            if (transaction instanceof ItemStackTransaction itemTx) slotTransactions = itemTx.getSlotTransactions();
            else if (transaction instanceof ItemStackSlotTransaction slotTx) slotTransactions = List.of(slotTx);

            // bail if we could not find slot transactions
            if (slotTransactions == null) return;

            // fire the event on the internal bus
            ModEventBus.post(new Event_PlayerInventoryChange(ref, store, changeEvent, slotTransactions));
        });
    }
}