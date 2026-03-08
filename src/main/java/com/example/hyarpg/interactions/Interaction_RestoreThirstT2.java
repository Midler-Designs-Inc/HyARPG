package com.example.hyarpg.interactions;

// Hytale Imports

import com.example.hyarpg.components.Component_Thirst;
import com.example.hyarpg.configs.ModConfig;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import static com.example.hyarpg.modules.Module_Thirst.componentTypeThirst;

public class Interaction_RestoreThirstT2 extends SimpleInstantInteraction {
    // Create the CODEC - this is required for serialization
    public static final BuilderCodec<Interaction_RestoreThirstT2> CODEC = BuilderCodec.builder(
            Interaction_RestoreThirstT2.class,
            Interaction_RestoreThirstT2::new,
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

        // Get the thirst component
        Component_Thirst thirst = store.getComponent(entityRef, componentTypeThirst);
        if (thirst == null) return;

        // restore some thirst
        float value = (float) (thirst.max * ModConfig.get().thirst.restore_t2_percent);
        thirst.restore(value);
    }
}
