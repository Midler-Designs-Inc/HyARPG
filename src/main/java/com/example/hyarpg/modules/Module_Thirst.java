package com.example.hyarpg.modules;

// Hytale Imports
import au.ellie.hyui.types.ProgressBarDirection;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.HyARPGPlugin;
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.components.Component_Thirst;
import com.example.hyarpg.events.Event_PlayerDeath;
import com.example.hyarpg.events.Event_PlayerReady;
import com.example.hyarpg.interactions.Interaction_RestoreThirstT1;

// HyUI Imports
import au.ellie.hyui.builders.*;

// Java Imports
import javax.swing.*;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

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

        // Listen to applicable events on the mods internal event bus
        ModEventBus.register(Event_PlayerReady.class, this::onPlayerReady);
        ModEventBus.register(Event_PlayerDeath.class, this::onPlayerDeath);

        // Start the thirst tick system
        startThirstTickSystem();
    }

    // Start a repeating task that drains thirst and applies starvation damage
    private void startThirstTickSystem() {
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

                        // Get the thirst component
                        Component_Thirst thirst = store.getComponent(entityRef, componentTypeThirst);
                        if (thirst == null) return;

                        // Don't drain thirst in creative mode
                        if (player.getGameMode() != GameMode.Creative) {
                            thirst.drain(thirst.drainRate);
                            store.putComponent(entityRef, componentTypeThirst, thirst);
                        }

                        // Apply starvation damage if starving
                        if (thirst.isStarving() && player.getGameMode() != GameMode.Creative) {
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
                                float healthDMG = maxHealth / 60f; // full drain over 60 seconds
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