package com.example.hyarpg.modules;

// Hytale Imports
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.World;

// Mod Imports
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_PlayerReady;
import com.example.hyarpg.events.Event_PlayerDeath;
import com.example.hyarpg.HyARPGPlugin;
import com.example.hyarpg.components.Component_Hunger;
import com.example.hyarpg.interactions.Interaction_RestoreHungerT1;
import com.example.hyarpg.interactions.Interaction_RestoreHungerT2;
import com.example.hyarpg.interactions.Interaction_RestoreHungerT3;

// Java Imports
import java.awt.*;
import java.util.concurrent.*;

public class Module_Hunger {

    private final HyARPGPlugin plugin;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // Component Type References
    public static ComponentType<EntityStore, Component_Hunger> componentTypeHunger;

    // initialize this module
    public Module_Hunger(HyARPGPlugin plugin) {
        this.plugin = plugin;

        // Register the component type using EntityStoreRegistry
        componentTypeHunger = plugin.getEntityStoreRegistry()
                .registerComponent(Component_Hunger.class, "HungerComponent", Component_Hunger.CODEC);

        // Get the interaction registry and register the RestoreHunger interaction
        final var interactionRegistry = plugin.getCodecRegistry(Interaction.CODEC);
        interactionRegistry.register("RestoreHunger_T1", Interaction_RestoreHungerT1.class, Interaction_RestoreHungerT1.CODEC);
        interactionRegistry.register("RestoreHunger_T2", Interaction_RestoreHungerT2.class, Interaction_RestoreHungerT2.CODEC);
        interactionRegistry.register("RestoreHunger_T3", Interaction_RestoreHungerT3.class, Interaction_RestoreHungerT3.CODEC);

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

        // ensure the component exists (supposedly this will putComponent internally if not??)
        store.ensureAndGetComponent(entityRef, componentTypeHunger);
    }

    // This function runs whenever a player has died
    private void onPlayerDeath(Event_PlayerDeath event) {
        // get playerRef of the player that died
        Ref<EntityStore> ref = event.getRef();
        Store<EntityStore> store = event.getStore();

        // Get the hunger component
        Component_Hunger hunger = store.getComponent(ref, componentTypeHunger);
        if (hunger == null) return;

        // call the hunger component on death method
        hunger.setOnDeath();
    }

}