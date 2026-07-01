package com.example.hyarpg.events;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record Event_PlayerJoin(
        Holder<EntityStore> holder,
        PlayerConnectEvent event
) {}