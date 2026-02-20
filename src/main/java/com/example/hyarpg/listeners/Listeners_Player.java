package com.example.hyarpg.listeners;

// Hytale Jar Imports
import com.example.hyarpg.HyARPGPlugin;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;

// Mod Imports
import com.example.hyarpg.events.Event_PlayerJoin;
import com.example.hyarpg.events.Event_PlayerDisconnect;
import com.example.hyarpg.events.Event_PlayerReady;
import com.example.hyarpg.ModEventBus;

// Java Imports
import java.util.logging.Level;

// Class for registering player event listener handler logic
public class Listeners_Player {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // Register all player event listeners
    public void register(EventRegistry eventBus, HyARPGPlugin plugin) {
        // Start listening for the PlayerConnectEvent - When a player connects
        try {
            eventBus.register(PlayerConnectEvent.class, this::onPlayerConnect);
            LOGGER.at(Level.INFO).log("[HyARPG] Registered PlayerConnectEvent listener");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[HyARPG] Failed to register PlayerConnectEvent");
        }

        // Start listening for the PlayerDisconnectEvent - When a player disconnects
        try {
            eventBus.register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
            LOGGER.at(Level.INFO).log("[HyARPG] Registered PlayerDisconnectEvent listener");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[HyARPG] Failed to register PlayerDisconnectEvent");
        }

        // Start listening for the PlayerJoinEvent - When a player joins
        try {
            eventBus.registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
            LOGGER.at(Level.INFO).log("[HyARPG] Registered PlayerReadyEvent listener");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[HyARPG] Failed to register PlayerReadyEvent");
        }

    }

    // Handle player connect event
    private void onPlayerConnect(PlayerConnectEvent event) {
        // get player and world details
        String playerName = event.getPlayerRef() != null ? event.getPlayerRef().getUsername() : "Unknown";
        String worldName = event.getWorld() != null ? event.getWorld().getName() : "unknown";

        // Log player world join
        LOGGER.at(Level.INFO).log("[HyARPG] Player %s connected to world %s", playerName, worldName);

        // Emit the event on the internal mod bus
        ModEventBus.post(new Event_PlayerJoin(event.getPlayerRef(), event.getWorld()));
    }

    // Handle player disconnect event
    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        // get player name
        String playerName = event.getPlayerRef() != null ? event.getPlayerRef().getUsername() : "Unknown";

        // log player disconnect
        LOGGER.at(Level.INFO).log("[HyARPG] Player %s disconnected", playerName);

        // Emit the event on the internal mod bus
        ModEventBus.post(new Event_PlayerDisconnect(event.getPlayerRef()));
    }

    // Handle player ready event
    private void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();

        // Log player world join
        LOGGER.at(Level.INFO).log("[HyARPG] Player %s is ready", player.getDisplayName());

        // Emit the event on the internal mod bus
        ModEventBus.post(new Event_PlayerReady(player, event.getPlayer().getWorld()));
    }
}