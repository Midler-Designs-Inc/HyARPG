package com.example.hyarpg.interactions;

// Hytale Imports
import com.example.hyarpg.ui.CustomPage_HowToPlayPage;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Java Imports
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Level;

public class Interaction_ShowHowToPlay extends SimpleInstantInteraction {
    // Create the CODEC - this is required for serialization
    public static final BuilderCodec<Interaction_ShowHowToPlay> CODEC = BuilderCodec.builder(
        Interaction_ShowHowToPlay.class,
        Interaction_ShowHowToPlay::new,
        SimpleInstantInteraction.CODEC
    ).build();

    // mandatory function, executed when the interaction fires
    @Override
    protected void firstRun(
            @NonNullDecl InteractionType interactionType,
            @NonNullDecl InteractionContext context,
            @NonNullDecl CooldownHandler cooldownHandler
    ) {
        final Ref<EntityStore> entityRef = context.getEntity();
        final Store<EntityStore> store = entityRef.getStore();

        try {
            Player player = store.getComponent(entityRef, Player.getComponentType());
            PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
            player.getPageManager().openCustomPage(entityRef, store, new CustomPage_HowToPlayPage(playerRef));
        } catch (Exception e) {
            HytaleLogger.getLogger().at(Level.WARNING).log("Open Forge Crafting Window failed: %s", e.getMessage());
        }
    }
}
