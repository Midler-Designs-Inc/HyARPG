package com.example.hyarpg.modules;

// Hytale Imports
import com.example.hyarpg.components.Component_Hunger;
import com.example.hyarpg.events.Event_PlayerDeath;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

// Internal Mode Imports
import com.example.hyarpg.events.Event_PlayerJoin;
import com.example.hyarpg.events.Event_PlayerDisconnect;
import com.example.hyarpg.ModEventBus;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Java Imports
import java.awt.*;
import java.util.UUID;

/*
    Greeter: A module for notifying all players on the server of global events like when a player joins
    and when a player disconnects.
 */
public class Module_Greeter {

    public Module_Greeter() {
        // Listen to applicable events on the mods internal event bus
        ModEventBus.register(Event_PlayerJoin.class, this::onPlayerJoin);
        ModEventBus.register(Event_PlayerDisconnect.class, this::onPlayerDisconnect);
        ModEventBus.register(Event_PlayerDeath.class, this::onPlayerDeath);
    }

    // This function runs whenever a PlayerJoin event is posted
    private void onPlayerJoin(Event_PlayerJoin event) {
        // get the joning player from the event and formulate our message
        Message systemMessage = Message.raw("Player " + event.getPlayer().getUsername() + " has come online.")
                .color(Color.GREEN);

        // loop over all players and broadcast the message
        for (PlayerRef player : Universe.get().getPlayers()) {
            player.sendMessage(systemMessage);
        }
    }

    // This function runs whenever a PlayerDisconnect event is posted
    private void onPlayerDisconnect(Event_PlayerDisconnect event) {
        // get the joning player from the event and formulate our message
        Message systemMessage = Message.raw("Player " + event.getPlayer().getUsername() + " has gone offline.")
                .color(Color.GRAY);

        // loop over all players and broadcast the message
        for (PlayerRef player : Universe.get().getPlayers()) {
            player.sendMessage(systemMessage);
        }
    }

    // This function runs whenever a player has died
    private void onPlayerDeath(Event_PlayerDeath event) {
        // get playerRef of the player that died
        PlayerRef playerRef = event.getPlayer();

        // set the system death message
        Message systemMessage = Message.raw("Player " + playerRef.getUsername() + " has died.")
                .color(Color.RED);

        // loop over all players and broadcast the message
        for (PlayerRef player : Universe.get().getPlayers()) {
            player.sendMessage(systemMessage);
        }
    }
}
