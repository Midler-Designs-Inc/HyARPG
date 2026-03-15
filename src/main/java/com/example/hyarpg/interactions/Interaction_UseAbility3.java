package com.example.hyarpg.interactions;

// Hytale Imports

import com.example.hyarpg.ui.Page_RPGStats;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.logging.Level;

public class Interaction_UseAbility3 extends SimpleInstantInteraction {
    // Create the CODEC - this is required for serialization
    public static final BuilderCodec<Interaction_UseAbility3> CODEC = BuilderCodec.builder(
        Interaction_UseAbility3.class,
        Interaction_UseAbility3::new,
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
            player.sendMessage(Message.raw("You have not yet learned a secondary ability.").color(Color.GRAY));
        } catch (NoClassDefFoundError e) {
            // Class not yet loaded, retry on next tick or log
            HytaleLogger.getLogger().at(Level.WARNING).log("Page_RPGStats not loaded yet: %s", e.getMessage());
        }
    }
}
