package com.example.hyarpg.listeners;

import com.example.hyarpg.events.Event_PlayerInteraction;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.example.hyarpg.ModEventBus;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Listeners_PlayerInteractions {

    public void register() {
        PacketAdapters.registerInbound((PlayerPacketWatcher) (playerRef, packet) -> {
            if (packet.getId() != 290 && packet.getId() != 108) return;
            if (!(packet instanceof SyncInteractionChains chains)) return;

            for (SyncInteractionChain chain : chains.updates) {
                handleInteraction(playerRef, chain);
            }
        });
    }

    private void handleInteraction(PlayerRef playerRef, SyncInteractionChain chain) {
        // Only fire on initial press, not held state
        if (!chain.initial) return;

        // get the world and fire our event during world execute
        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            InteractionType type = chain.interactionType;
            switch (type) {
                case Primary   -> ModEventBus.post(new Event_PlayerInteraction(playerRef.getReference(), store, "Primary"));
                case Secondary -> ModEventBus.post(new Event_PlayerInteraction(playerRef.getReference(), store, "Secondary"));
                case Ability1  -> ModEventBus.post(new Event_PlayerInteraction(playerRef.getReference(), store, "Ability1"));
                case Ability2  -> ModEventBus.post(new Event_PlayerInteraction(playerRef.getReference(), store, "Ability2"));
                case Ability3  -> ModEventBus.post(new Event_PlayerInteraction(playerRef.getReference(), store, "Ability3"));
                case Use       -> ModEventBus.post(new Event_PlayerInteraction(playerRef.getReference(), store, "Use"));
                default        -> {} // ignore CollisionEnter, ProjectileHit, etc.
            }
        });
    }
}