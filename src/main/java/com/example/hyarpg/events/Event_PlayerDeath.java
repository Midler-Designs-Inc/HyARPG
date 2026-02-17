package com.example.hyarpg.events;

import com.hypixel.hytale.server.core.universe.PlayerRef;

public class Event_PlayerDeath {
    private final PlayerRef player;

    public Event_PlayerDeath(PlayerRef player) {
        this.player = player;
    }

    public PlayerRef getPlayer() {
        return player;
    }
}
