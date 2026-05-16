package com.example.hyarpg;

// Hytale Imports
import com.hypixel.hytale.builtin.deployables.config.DeployableConfig;
import com.hypixel.hytale.component.ComponentType;
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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.example.hyarpg.components.Component_HomingMissile;
import com.example.hyarpg.components.Component_Simulacrum;
import com.example.hyarpg.ticking_systems.System_HomingMissile;
import com.example.hyarpg.ticking_systems.System_Simulacrum;
import com.example.hyarpg.utils.items.ItemFactory;

// Java Imports
import javax.annotation.Nonnull;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.List;

// HyARPG Root Class
public class HyARPGPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static HyARPGPlugin instance;
    public Module_RPGSystem rpgSystem;
    public Module_RaidSystem raidSystem;
    public Module_CombatSystem combatSystem;

    // components
    public ComponentType<EntityStore, Component_HomingMissile> componentTypeHomingMissile;
    public ComponentType<EntityStore, Component_Simulacrum> componentTypeSimulacrum;

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

        // register components
        registerComponents();

        // Register event listeners
        registerListeners();

        // Register mod modules
        registerModules();

        // Register mod commands
        registerCommands();

        // force load my mod classes
        preloadClasses();

        LOGGER.at(Level.INFO).log("[HyARPG] Setup complete!");
    }

    // Register mod components
    private void registerComponents() {
        componentTypeHomingMissile = getEntityStoreRegistry().registerComponent(Component_HomingMissile.class, "HomingMissileComponent", Component_HomingMissile.CODEC);
        componentTypeSimulacrum = getEntityStoreRegistry().registerComponent(Component_Simulacrum.class, "SimulacrumComponent", Component_Simulacrum.CODEC);
    }

    // Register event listeners
    private void registerListeners() {
        EventRegistry eventBus = getEventRegistry();

        try {
            // Register world listeners
            new Listeners_WorldStart().register(eventBus);

            // Register Player Listeners
            new Listeners_Player().register(eventBus, this);
            new Listeners_PlayerInteractions().register();

            // Register Module listeners
            getEntityStoreRegistry().registerSystem(new Listeners_PlayerInventory());
            getEntityStoreRegistry().registerSystem(new Listeners_Death());
            getEntityStoreRegistry().registerSystem(new Listeners_Damage());
            getEntityStoreRegistry().registerSystem(new Listeners_Entity_PrePost());
            getEntityStoreRegistry().registerSystem(new Listeners_Entity_PostPre());
            getEntityStoreRegistry().registerSystem(new Listeners_Crafting());
            getEntityStoreRegistry().registerSystem(new Listeners_PlaceBlock());
            getEntityStoreRegistry().registerSystem(new Listeners_BreakBlock());

            // Register component ticking systems
            getEntityStoreRegistry().registerSystem(new System_HomingMissile(componentTypeHomingMissile));
            getEntityStoreRegistry().registerSystem(new System_Simulacrum(componentTypeSimulacrum, componentTypeHomingMissile));

            // Register chunk listeners
            getChunkStoreRegistry().registerSystem(new Listeners_ContainerSpawn());

            // Ore distance zone system
            new OreDistanceListener(buildOreConfig()).register(eventBus);
            LOGGER.at(Level.INFO).log("[HyARPG] Registered OreDistanceListener");

            // prefab placement registry — tracks every placed prefab for compass and explorer systems
            PrefabRegistry.register();

            // prefab loading system
            Path prefabFolder = Paths.get("").toAbsolutePath().resolve("mods/HyARPG/prefabs");
            new PrefabWorldGenListener(prefabFolder).register(getEventRegistry());

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
            rpgSystem = new Module_RPGSystem(this);
            combatSystem = new Module_CombatSystem();
            new Module_PlayerHud(this);
            new Module_BuildSystem();
            raidSystem = new Module_RaidSystem();

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
            getCommandRegistry().registerCommand(new ShowDiscovered());
            getCommandRegistry().registerCommand(new ToggleHunger());
            getCommandRegistry().registerCommand(new ToggleThirst());
            getCommandRegistry().registerCommand(new RefundSkillLibrary());
            getCommandRegistry().registerCommand(new SetSkillPoints());
            getCommandRegistry().registerCommand(new AddPlayerLevels());
            getCommandRegistry().registerCommand(new ResetDiscoveredIngredients());
            getCommandRegistry().registerCommand(new ResetDiscoveredRecipes());
            getCommandRegistry().registerCommand(new ResetDiscoveredRooms());
            getCommandRegistry().registerCommand(new SetShowCombatTextSetting());
            getCommandRegistry().registerCommand(new TriggerRaid(this));
            getCommandRegistry().registerCommand(new TriggerRaid(this));
            getCommandRegistry().registerCommand(new ClearCurrentTerritory());
            getCommandRegistry().registerCommand(new ShowSalvagePage());

            // log the instantiation
            LOGGER.at(Level.INFO).log("[HyARPG] Instantiated commands");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[HyARPG] Failed to register commands");
        }
    }

    @Override
    protected void start() {
        preLoadCraftingComponents();

        LOGGER.at(Level.INFO).log("[HyARPG] Started!");
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
                                ModConfig.get().world.copper_veins_per_chunk,
                                ModConfig.get().world.copper_min_vein_size,
                                ModConfig.get().world.copper_max_vein_size,
                                ModConfig.get().world.copper_min_y,
                                ModConfig.get().world.copper_max_y
                        ),

                        // ── Iron: 10k–30k, peaks at 20k ──────────────────────────────
                        new OreDistanceConfig.OreZone(
                                "Ore_Iron_Basalt", allStones,
                                ModConfig.get().world.min_distance_for_iron_spawn,
                                ModConfig.get().world.max_distance_for_iron_spawn,
                                ModConfig.get().world.iron_veins_per_chunk,
                                ModConfig.get().world.iron_min_vein_size,
                                ModConfig.get().world.iron_max_vein_size,
                                ModConfig.get().world.iron_min_y,
                                ModConfig.get().world.iron_max_y
                        ),
                        new OreDistanceConfig.OreZone(
                                "Ore_Iron_Basalt_Cracked", allStones,
                                ModConfig.get().world.min_distance_for_iron_spawn,
                                ModConfig.get().world.max_distance_for_iron_spawn,
                                ModConfig.get().world.iron_cracked_veins_per_chunk,
                                ModConfig.get().world.iron_cracked_min_vein_size,
                                ModConfig.get().world.iron_cracked_max_vein_size,
                                ModConfig.get().world.iron_cracked_min_y,
                                ModConfig.get().world.iron_cracked_max_y
                        ),

                        // ── Thorium: 20k–40k, peaks at 30k ──────────────────────────
                        new OreDistanceConfig.OreZone(
                                "Ore_Thorium_Mud", allStones,
                                ModConfig.get().world.min_distance_for_thorium_spawn,
                                ModConfig.get().world.max_distance_for_thorium_spawn,
                                ModConfig.get().world.thorium_veins_per_chunk,
                                ModConfig.get().world.thorium_min_vein_size,
                                ModConfig.get().world.thorium_max_vein_size,
                                ModConfig.get().world.thorium_min_y,
                                ModConfig.get().world.thorium_max_y
                        ),
                        new OreDistanceConfig.OreZone(
                                "Ore_Thorium_Mud_Cracked", allStones,
                                ModConfig.get().world.min_distance_for_thorium_spawn,
                                ModConfig.get().world.max_distance_for_thorium_spawn,
                                ModConfig.get().world.thorium_cracked_veins_per_chunk,
                                ModConfig.get().world.thorium_cracked_min_vein_size,
                                ModConfig.get().world.thorium_cracked_max_vein_size,
                                ModConfig.get().world.thorium_cracked_min_y,
                                ModConfig.get().world.thorium_cracked_max_y
                        ),

                        // ── Cobalt: 30k–50k, peaks at 40k ───────────────────────────
                        new OreDistanceConfig.OreZone(
                                "Ore_Cobalt_Slate", allStones,
                                ModConfig.get().world.min_distance_for_cobalt_spawn,
                                ModConfig.get().world.max_distance_for_cobalt_spawn,
                                ModConfig.get().world.cobalt_veins_per_chunk,
                                ModConfig.get().world.cobalt_min_vein_size,
                                ModConfig.get().world.cobalt_max_vein_size,
                                ModConfig.get().world.cobalt_min_y,
                                ModConfig.get().world.cobalt_max_y
                        ),
                        new OreDistanceConfig.OreZone(
                                "Ore_Cobalt_Slate_Cracked", allStones,
                                ModConfig.get().world.min_distance_for_cobalt_spawn,
                                ModConfig.get().world.max_distance_for_cobalt_spawn,
                                ModConfig.get().world.cobalt_cracked_veins_per_chunk,
                                ModConfig.get().world.cobalt_cracked_min_vein_size,
                                ModConfig.get().world.cobalt_cracked_max_vein_size,
                                ModConfig.get().world.cobalt_cracked_min_y,
                                ModConfig.get().world.cobalt_cracked_max_y
                        ),

                        // ── Adamantite: 40k–60k, peaks at 50k ───────────────────────
                        new OreDistanceConfig.OreZone(
                                "Ore_Adamantite_Magma", allStones,
                                ModConfig.get().world.min_distance_for_adamantite_spawn,
                                ModConfig.get().world.max_distance_for_adamantite_spawn,
                                ModConfig.get().world.adamantite_veins_per_chunk,
                                ModConfig.get().world.adamantite_min_vein_size,
                                ModConfig.get().world.adamantite_max_vein_size,
                                ModConfig.get().world.adamantite_min_y,
                                ModConfig.get().world.adamantite_max_y
                        ),
                        new OreDistanceConfig.OreZone(
                                "Ore_Adamantite_Magma_Cracked", allStones,
                                ModConfig.get().world.min_distance_for_adamantite_spawn,
                                ModConfig.get().world.max_distance_for_adamantite_spawn,
                                ModConfig.get().world.adamantite_cracked_veins_per_chunk,
                                ModConfig.get().world.adamantite_cracked_min_vein_size,
                                ModConfig.get().world.adamantite_cracked_max_vein_size,
                                ModConfig.get().world.adamantite_cracked_min_y,
                                ModConfig.get().world.adamantite_cracked_max_y
                        ),

                        // ── Mithril: 50k–70k, peaks at 60k ──────────────────────────
                        // Rarest tier — small veins, deep only
                        new OreDistanceConfig.OreZone(
                                "Ore_Mithril_Stone", allStones,
                                ModConfig.get().world.min_distance_for_mithril_spawn,
                                ModConfig.get().world.max_distance_for_mithril_spawn,
                                ModConfig.get().world.mithril_veins_per_chunk,
                                ModConfig.get().world.mithril_min_vein_size,
                                ModConfig.get().world.mithril_max_vein_size,
                                ModConfig.get().world.mithril_min_y,
                                ModConfig.get().world.mithril_max_y
                        )
                )
        );
    }

    private void preloadClasses() {
        String[] packages = {
                "com/example/hyarpg/"
        };

        ClassLoader cl = getClass().getClassLoader();
        URL jarUrl = getClass().getProtectionDomain().getCodeSource().getLocation();

        try (JarFile jar = new JarFile(jarUrl.toURI().getPath())) {
            jar.stream()
                    .filter(e -> !e.isDirectory() && e.getName().endsWith(".class"))
                    .filter(e -> Arrays.stream(packages).anyMatch(p -> e.getName().startsWith(p)))
                    .forEach(e -> {
                        String className = e.getName()
                                .replace('/', '.')
                                .replace(".class", "");
                        try {
                            Class.forName(className, true, cl);
                        } catch (ClassNotFoundException ex) {
                            LOGGER.at(Level.WARNING).log("[HyARPG] Failed to preload: " + className);
                        }
                    });
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[HyARPG] Class preload scan failed");
        }

        // Explicitly preload HyUI classes that are loaded lazily and can cause
        // NoClassDefFoundError if first accessed mid-tick
        String[] hyuiClasses = {
                "au.ellie.hyui.builders.HyUIPage",
                "au.ellie.hyui.builders.PageBuilder"
        };
        for (String className : hyuiClasses) {
            try {
                Class.forName(className, true, cl);
            } catch (ClassNotFoundException ex) {
                LOGGER.at(Level.WARNING).log("[HyARPG] Failed to preload HyUI class: " + className);
            }
        }
    }

    private void preLoadCraftingComponents() {
        try {
            // pre-build component index for item factory
            ItemFactory.buildComponentIndex();
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[HyARPG] Crafting component preload scan failed");
        }
    }
}