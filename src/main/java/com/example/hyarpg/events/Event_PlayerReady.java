package com.example.hyarpg.events;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;

public class Event_PlayerReady {
    private final Player player;
    private final World world;

    public Event_PlayerReady(Player player, World world) {
        this.player = player;
        this.world = world;
    }

    public Player getPlayer() {
        return player;
    }

    public World getWorld() {
        return world;
    }
}