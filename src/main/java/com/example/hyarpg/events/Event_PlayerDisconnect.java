package com.example.hyarpg.events;

import com.hypixel.hytale.server.core.universe.PlayerRef;

public class Event_PlayerDisconnect {
    private final PlayerRef player;

    public Event_PlayerDisconnect(PlayerRef player) {
        this.player = player;
    }

    public PlayerRef getPlayer() {
        return player;
    }
}
