package com.example.hyarpg;

// Hytale Imports
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;

// Mod Imports
import com.example.hyarpg.listeners.*;
import com.example.hyarpg.modules.*;

// Java Imports
import javax.annotation.Nonnull;
import java.util.logging.Level;

// HyARPG Root Class
public class HyARPGPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static HyARPGPlugin instance;

    // required super function??
    public HyARPGPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    // Get the plugin instance.
    public static HyARPGPlugin getInstance() {
        return instance;
    }

    // mod one time setup
    @Override
    protected void setup() {
        LOGGER.at(Level.INFO).log("[HyARPG] Setting up...");

        // Register event listeners
        registerListeners();

        // Register mod modules
        registerModules();

        LOGGER.at(Level.INFO).log("[HyARPG] Setup complete!");
    }

    // Register event listeners
    private void registerListeners() {
        EventRegistry eventBus = getEventRegistry();

        try {
            // register these listeners which have their own logic
            new Listeners_PlayerInventory();

            // register these listeners with the main event bus
            new Listeners_Player().register(eventBus, this);

            // register these listeners in the entity registry system
            getEntityStoreRegistry().registerSystem(new Listeners_Death());
            getEntityStoreRegistry().registerSystem(new Listeners_Damage());
            getEntityStoreRegistry().registerSystem(new Listeners_Entity_PrePost());
            getEntityStoreRegistry().registerSystem(new Listeners_Entity_PostPre());

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
            new Module_RPG_Stats(this);
            new Module_PlayerHud(this);

            // log the instantiation
            LOGGER.at(Level.INFO).log("[HyARPG] Instantiated modules");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[HyARPG] Failed to register modules");
        }
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
}