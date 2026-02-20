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
            Transaction transaction = changeEvent.transaction();
            if (!transaction.succeeded()) return;

            // this works to detect when an item is removed or changed
            if (transaction instanceof ItemStackSlotTransaction slotTx) {
                ModEventBus.post(new Event_PlayerInventoryChange(ref, store, changeEvent, slotTx.getAction(), slotTx, null));
            }

            // this works to detect add item
            else if (transaction instanceof ItemStackTransaction itemTx) {
                ModEventBus.post(new Event_PlayerInventoryChange(ref, store, changeEvent, itemTx.getAction(), null, itemTx));
            }
        });
    }
}