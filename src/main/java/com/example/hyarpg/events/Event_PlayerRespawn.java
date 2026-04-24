package com.example.hyarpg.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record Event_PlayerRespawn(Ref<EntityStore> ref, Store<EntityStore> store) {}