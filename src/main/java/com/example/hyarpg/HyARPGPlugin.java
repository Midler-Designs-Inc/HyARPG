package com.example.hyarpg;

// Hytale Imports

import com.example.hyarpg.subclasses.FixedDeployableAoeConfig;
import com.example.hyarpg.subclasses.FixedDeployableTurretConfig;
import com.hypixel.hytale.builtin.deployables.config.DeployableConfig;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;

// Mod Imports
import com.example.hyarpg.listeners.*;
import com.example.hyarpg.modules.*;
import com.example.hyarpg.worldgen.*;
import com.example.hyarpg.commands.*;
import com.example.hyarpg.configs.ModConfig;

// Java Imports
import javax.annotation.Nonnull;
import java.util.logging.Level;
import java.util.List;

// HyARPG Root Class
public class HyARPGPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static HyARPGPlugin instance;
    public Module_RPG_System rpgSystem;

    // required super function??
    public HyARPGPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;

    }

    // Get the plugin instance.
    public static HyARPGPlugin getInstance() { return instance; }

    // mod one time setup
    @Override
    protected void setup() {
        LOGGER.at(Level.INFO).log("[HyARPG] Setting up...");

        // Create or Read a Config File
        ModConfig.load();

        // Register event listeners
        registerListeners();

        // Register mod modules
        registerModules();

        // Register mod commands
        registerCommands();

        // Register codec
        registerCodecs();

        LOGGER.at(Level.INFO).log("[HyARPG] Setup complete!");
    }

    // Register event listeners
    private void registerListeners() {
        EventRegistry eventBus = getEventRegistry();

        try {
            // register these listeners which have their own logic
            new Listeners_PlayerInventory();
            new Listeners_PlayerInteractions().register();

            // register these listeners with the main event bus
            new Listeners_Player().register(eventBus, this);

            // register these listeners in the entity registry system
            getEntityStoreRegistry().registerSystem(new Listeners_Death());
            getEntityStoreRegistry().registerSystem(new Listeners_Damage());
            getEntityStoreRegistry().registerSystem(new Listeners_Entity_PrePost());
            getEntityStoreRegistry().registerSystem(new Listeners_Entity_PostPre());
            getEntityStoreRegistry().registerSystem(new Listeners_Crafting());
            getEntityStoreRegistry().registerSystem(new Listeners_UtilitySlot());

            // Ore distance zone system
            new OreDistanceListener(buildOreConfig()).register(eventBus);
            LOGGER.at(Level.INFO).log("[HyARPG] Registered OreDistanceListener");

            // log the registration
            LOGGER.at(Level.INFO).log("[HyARPG] Registered listeners");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[HyARPG] Failed to register listeners");
        }
    }

    // Register the modules for this mod
    private void registerModules() {
        try {
            // instantiate each module
            new Module_Greeter();
            new Module_Hunger(this);
            new Module_Thirst(this);
            rpgSystem = new Module_RPG_System(this);
            new Module_PlayerHud(this);

            // create an instance of our global tick event (not OOP but better for processing I guess)
            new Module_ModTickLoop(this, HytaleServer.SCHEDULED_EXECUTOR).start();

            // log the instantiation
            LOGGER.at(Level.INFO).log("[HyARPG] Instantiated modules");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[HyARPG] Failed to register modules");
        }
    }

    // Register the commends for this mod
    private void registerCommands(){
        try {
            getCommandRegistry().registerCommand(new ShowStats());
            getCommandRegistry().registerCommand(new ShowSkills());
            getCommandRegistry().registerCommand(new ToggleHunger());
            getCommandRegistry().registerCommand(new ToggleThirst());
            getCommandRegistry().registerCommand(new RefundSkillLibrary());
            getCommandRegistry().registerCommand(new SetSkillPoints());
            getCommandRegistry().registerCommand(new AddPlayerLevels());
            getCommandRegistry().registerCommand(new ResetDiscoveredIngredients());

            // log the instantiation
            LOGGER.at(Level.INFO).log("[HyARPG] Instantiated commands");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[HyARPG] Failed to register commands");
        }
    }

    // Register codecs for this mod
    private void registerCodecs(){
        getCodecRegistry(DeployableConfig.CODEC).register("FixedTurret", FixedDeployableTurretConfig.class, FixedDeployableTurretConfig.CODEC);
        getCodecRegistry(DeployableConfig.CODEC).register("FixedAoe", FixedDeployableAoeConfig.class, FixedDeployableAoeConfig.CODEC);
    }

    @Override
    protected void start() {
        LOGGER.at(Level.INFO).log("[HyARPG] Started!");
        LOGGER.at(Level.INFO).log("[HyARPG] Use /hya help for commands");
    }

    @Override
    protected void shutdown() {
        LOGGER.at(Level.INFO).log("[HyARPG] Shutting down...");
        instance = null;
    }

    private OreDistanceConfig buildOreConfig() {
        // All stone types across all biome zones — ensures every ore can generate
        // regardless of which vanilla zone the distance ring falls in
        String[] allStones = new String[]{
            "Rock_Stone", "Rock_Basalt", "Rock_Marble", "Rock_Quartzite",
            "Rock_Sandstone", "Rock_Sandstone_White", "Rock_Sandstone_Red",
            "Rock_Volcanic", "Rock_Shale", "Rock_Slate", "Rock_Magma_Cooled",
            "Soil_Dirt_Dry", "Soil_Mud_Dry", "Soil_Dirt_Cold", "Soil_Gravel"
        };

        return new OreDistanceConfig(
                ModConfig.get().world.origin_spawn_point_x,
                ModConfig.get().world.origin_spawn_point_y,
                ModConfig.get().world.origin_spawn_point_z,
                List.of(
                    // ── Copper: 0k–20k, peaks at 10k ─────────────────────────────
                    new OreDistanceConfig.OreZone(
                        "Ore_Copper_Stone", allStones,
                        ModConfig.get().world.min_distance_for_copper_spawn,
                        ModConfig.get().world.max_distance_for_copper_spawn,
                        8, 3, 8, 10, 80
                    ),

                    // ── Iron: 10k–30k, peaks at 20k ──────────────────────────────
                    new OreDistanceConfig.OreZone(
                        "Ore_Iron_Basalt", allStones,
                        ModConfig.get().world.min_distance_for_iron_spawn,
                        ModConfig.get().world.max_distance_for_iron_spawn,
                        6, 3, 7, 5, 60
                    ),
                    new OreDistanceConfig.OreZone(
                        "Ore_Iron_Basalt_Cracked", allStones,
                        ModConfig.get().world.min_distance_for_iron_spawn,
                        ModConfig.get().world.max_distance_for_iron_spawn,
                        3, 2, 5, 5, 60
                    ),

                    // ── Thorium: 20k–40k, peaks at 30k ──────────────────────────
                    new OreDistanceConfig.OreZone(
                        "Ore_Thorium_Mud", allStones,
                        ModConfig.get().world.min_distance_for_thorium_spawn,
                        ModConfig.get().world.max_distance_for_thorium_spawn,
                        5, 3, 7, 5, 70
                    ),
                    new OreDistanceConfig.OreZone(
                        "Ore_Thorium_Mud_Cracked", allStones,
                        ModConfig.get().world.min_distance_for_thorium_spawn,
                        ModConfig.get().world.max_distance_for_thorium_spawn,
                        3, 2, 5, 5, 70
                    ),

                    // ── Cobalt: 20k–40k, peaks at 30k ───────────────────────────
                    new OreDistanceConfig.OreZone(
                        "Ore_Cobalt_Slate", allStones,
                        ModConfig.get().world.min_distance_for_cobalt_spawn,
                        ModConfig.get().world.max_distance_for_cobalt_spawn,
                        5, 3, 7, 5, 60
                    ),
                    new OreDistanceConfig.OreZone(
                        "Ore_Cobalt_Slate_Cracked", allStones,
                        ModConfig.get().world.min_distance_for_cobalt_spawn,
                        ModConfig.get().world.max_distance_for_cobalt_spawn,
                        3, 2, 5, 5, 60
                    ),

                    // ── Adamantite: 30k–50k, peaks at 40k ───────────────────────
                    new OreDistanceConfig.OreZone(
                        "Ore_Adamantite_Magma", allStones,
                        ModConfig.get().world.min_distance_for_adamantite_spawn,
                        ModConfig.get().world.max_distance_for_adamantite_spawn,
                        4, 3, 7, 1, 50
                    ),
                    new OreDistanceConfig.OreZone(
                        "Ore_Adamantite_Magma_Cracked", allStones,
                        ModConfig.get().world.min_distance_for_adamantite_spawn,
                        ModConfig.get().world.max_distance_for_adamantite_spawn,
                        3, 2, 5, 1, 50
                    ),

                    // ── Mithril: 40k–60k, peaks at 50k ──────────────────────────
                    // Rarest tier — small veins, deep only
                    new OreDistanceConfig.OreZone(
                        "Ore_Mithril_Stone", allStones,
                        ModConfig.get().world.min_distance_for_mithril_spawn,
                        ModConfig.get().world.max_distance_for_mithril_spawn,
                        3, 2, 5, 1, 30
                    )
                )
        );
    }
}