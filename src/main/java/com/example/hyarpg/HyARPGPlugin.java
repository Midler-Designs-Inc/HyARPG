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

        // Register commands
//        registerCommands();

        // Register event listeners
        registerListeners();

        // Register mod modules
        registerModules();

        LOGGER.at(Level.INFO).log("[HyARPG] Setup complete!");
    }

    // Register plugin commands.
//    private void registerCommands() {
//        try {
//            getCommandRegistry().registerCommand(new HyARPGPluginCommand());
//            LOGGER.at(Level.INFO).log("[HyARPG] Registered /hya command");
//        } catch (Exception e) {
//            LOGGER.at(Level.WARNING).withCause(e).log("[HyARPG] Failed to register commands");
//        }
//    }

    // Register event listeners
    private void registerListeners() {
        EventRegistry eventBus = getEventRegistry();

        try {
            // register each listener with the main event bus
            new Listeners_Player().register(eventBus);

            // log the registration
            LOGGER.at(Level.INFO).log("[HyARPG] Registered listener: PlayerListener");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[HyARPG] Failed to register listener: PlayerListener");
        }
    }

    // Register the modules for this mod
    private void registerModules() {
        try {
            // instantiate each module
            Module_Greeter moduleGreeter = new Module_Greeter();
//            PlayerHud modulePlayerHud = new PlayerHud();
            Module_Hunger moduleHunger = new Module_Hunger(this);

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