package com.example.hyarpg.modules;

// Hytale Imports
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.HyARPGPlugin;
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_PlayerReady;
import com.example.hyarpg.ui.CustomHUD_Player;

public class Module_PlayerHud {

    public Module_PlayerHud(HyARPGPlugin plugin) {
        ModEventBus.register(Event_PlayerReady.class, this::onPlayerReady);
    }

    private void onPlayerReady(Event_PlayerReady event) {
        Player player = event.getPlayer();
        World world = event.getWorld();

        // resolve the entity ref and store — bail if the player isn't fully initialized yet
        Ref<EntityStore> entityRef = player.getReference();
        Store<EntityStore> store = world.getEntityStore().getStore();
        if (entityRef == null) return;

        // get the PlayerRef component
        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef == null) return;

        // get the existing HudManager — always present on a player, holds the native HUD components
        HudManager hudManager = player.getHudManager();

        // register our HUD layer — setCustomHud calls show() internally which triggers build()
        hudManager.addCustomHud(playerRef, new CustomHUD_Player(playerRef));
    }
}