package com.example.hyarpg.interactions;

// Hytale Imports
import com.example.hyarpg.components.Component_Hunger;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Java Imports
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

// Mod Imports
import static com.example.hyarpg.modules.Module_Hunger.componentTypeHunger;

public class Interaction_RestoreHungerT3 extends SimpleInstantInteraction {
    // Create the CODEC - this is required for serialization
    public static final BuilderCodec<Interaction_RestoreHungerT3> CODEC = BuilderCodec.builder(
            Interaction_RestoreHungerT3.class,
            Interaction_RestoreHungerT3::new,
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

        // Get the hunger component
        Component_Hunger hunger = store.getComponent(entityRef, componentTypeHunger);
        if (hunger == null) return;

        // restore some hunger
        float value = hunger.max;
        hunger.restore(value);
    }
}
