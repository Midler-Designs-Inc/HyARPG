package com.example.hyarpg.events;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

public class Event_PlayerJoin {
    private final PlayerRef player;
    private final World world;

    public Event_PlayerJoin(PlayerRef player, World world) {
        this.player = player;
        this.world = world;
    }

    public PlayerRef getPlayer() {
        return player;
    }

    public World getWorld() {
        return world;
    }
}