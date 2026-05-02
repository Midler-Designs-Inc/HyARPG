package com.example.hyarpg.interactions;

// Hytale Imports
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Java Imports
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;

public class Interaction_WaywardShrineCompassFindPuzzleKey extends SimpleInstantInteraction {

    public static final BuilderCodec<Interaction_WaywardShrineCompassFindPuzzleKey> CODEC = BuilderCodec.builder(Interaction_WaywardShrineCompassFindPuzzleKey.class, Interaction_WaywardShrineCompassFindPuzzleKey::new, SimpleInstantInteraction.CODEC).build();

    @Override
    protected void firstRun(@NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext context, @NonNullDecl CooldownHandler cooldownHandler) {
        // get the entity ref
        Ref<EntityStore> ref = context.getEntity();
        if (!ref.isValid()) return;

        // get the store and player ref from the entity ref
        Store<EntityStore> entityStore = ref.getStore();
        PlayerRef playerRef = entityStore.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;

        playerRef.sendMessage(Message.raw("The compass starts spinning wildly and jerking directions abruptly, almost as if something was fighting back against being found...").color(Color.GRAY));
    }
}