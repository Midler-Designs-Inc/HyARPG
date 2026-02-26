package com.example.hyarpg.modules;

// Hytale Imports
import com.example.hyarpg.interactions.Interaction_RestoreThirstT2;
import com.example.hyarpg.interactions.Interaction_RestoreThirstT3;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.HyARPGPlugin;
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.components.Component_Thirst;
import com.example.hyarpg.events.Event_PlayerDeath;
import com.example.hyarpg.events.Event_PlayerReady;
import com.example.hyarpg.interactions.Interaction_RestoreThirstT1;

// Java Imports
import javax.swing.*;

public class Module_Thirst {

    private final HyARPGPlugin plugin;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static ComponentType<EntityStore, Component_Thirst> componentTypeThirst;

    // initialize this module
    public Module_Thirst(HyARPGPlugin plugin) {
        this.plugin = plugin;

        // Register the component type using EntityStoreRegistry
        componentTypeThirst = plugin.getEntityStoreRegistry()
                .registerComponent(Component_Thirst.class, "ThirstComponent", Component_Thirst.CODEC);

        // Get the interaction registry and register the RestoreThirst interaction
        final var interactionRegistry = plugin.getCodecRegistry(Interaction.CODEC);
        interactionRegistry.register("RestoreThirst_T1", Interaction_RestoreThirstT1.class, Interaction_RestoreThirstT1.CODEC);
        interactionRegistry.register("RestoreThirst_T2", Interaction_RestoreThirstT2.class, Interaction_RestoreThirstT2.CODEC);
        interactionRegistry.register("RestoreThirst_T3", Interaction_RestoreThirstT3.class, Interaction_RestoreThirstT3.CODEC);

        // Listen to applicable events on the mods internal event bus
        ModEventBus.register(Event_PlayerReady.class, this::onPlayerReady);
        ModEventBus.register(Event_PlayerDeath.class, this::onPlayerDeath);
    }

    // This function runs whenever a PlayerReady event is posted
    private void onPlayerReady(Event_PlayerReady event) {
        // get the joining player
        Player player = event.getPlayer();
        World world = event.getWorld();

        // get the player's Ref and the world entity store
        Ref<EntityStore> entityRef = player.getReference();
        Store<EntityStore> store = world.getEntityStore().getStore();

        // ensure the component exists
        store.ensureAndGetComponent(entityRef, componentTypeThirst);
    }

    // This function runs whenever a player has died
    private void onPlayerDeath(Event_PlayerDeath event) {
        // get playerRef of the player that died
        Ref<EntityStore> ref = event.getRef();
        Store<EntityStore> store = event.getStore();

        // Get the thirst component
        Component_Thirst thirst = store.getComponent(ref, componentTypeThirst);
        if (thirst == null) return;

        // call the thirst component on death method
        thirst.setOnDeath();
    }
}