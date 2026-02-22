package com.example.hyarpg.modules;

// Hytale Imports
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;

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
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Level;

public class Module_Hunger {

    private final HyARPGPlugin plugin;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
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

        // Start the hunger tick system
        startHungerTickSystem();
    }

    // Start a repeating task that drains hunger and applies starvation damage
    private void startHungerTickSystem() {
        ScheduledFuture scheduleFuture = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
            // Iterate through all online players
            for (PlayerRef playerRef : Universe.get().getPlayers()) {
                if (!playerRef.isValid()) continue;

                Ref<EntityStore> entityRef = playerRef.getReference();
                if (entityRef == null) continue;

                // Get world UUID from PlayerRef, then get the World from Universe
                UUID worldUuid = playerRef.getWorldUuid();
                if (worldUuid == null) continue;

                World world = Universe.get().getWorld(worldUuid);
                if (world == null) continue;

                // Execute on the world thread
                world.execute(() -> {
                    try {
                        // get the world entity store and player
                        Store<EntityStore> store = world.getEntityStore().getStore();
                        Player player = store.getComponent(entityRef, Player.getComponentType());
                        if (player == null) return;

                        // Get the hunger component
                        Component_Hunger hunger = store.getComponent(entityRef, componentTypeHunger);
                        if (hunger == null) return;

                        // Don't drain hunger in creative mode
                        if (player.getGameMode() != GameMode.Creative) {
                            hunger.drain(hunger.drainRate);
                            store.putComponent(entityRef, componentTypeHunger, hunger);
                        }

                        // Apply starvation damage if starving
                        if (hunger.isStarving() && player.getGameMode() != GameMode.Creative) {
                            // get the stat map component from the player
                            ComponentType<EntityStore, EntityStatMap> statMapType =
                                    EntityStatsModule.get().getEntityStatMapComponentType();
                            EntityStatMap statMap = store.getComponent(entityRef, statMapType);
                            if (statMap == null) return;

                            // Get the health stat from the stat map
                            int healthIndex = DefaultEntityStatTypes.getHealth();
                            EntityStatValue healthStat = statMap.get(healthIndex);
                            if (healthStat == null) return;

                            // get teh current and max values from health stat
                            float currentHealth = healthStat.get();
                            float maxHealth = healthStat.getMax();

                            // Only damage if health > 1 (don't kill the player)
                            if (currentHealth > 1.0f) {
                                float healthDMG = maxHealth / 1200f; // full drain over 20 minutes seconds
                                float newHealth = Math.max(1.0f, currentHealth - healthDMG);
                                statMap.setStatValue(healthIndex, newHealth);
                            }
                        }
                    }
                    catch (Exception e) {
                        LOGGER.at(Level.INFO).log("[HyARPG] Setting up...");
                    }
                });
            }
        }, 0, 1, TimeUnit.SECONDS);
        plugin.getTaskRegistry().registerTask(scheduleFuture);
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