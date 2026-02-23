package com.example.hyarpg.interactions;

// Hytale Imports
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.components.Component_CraftingKnowledge;
import static com.example.hyarpg.modules.Module_RPG_Stats.componentTypeCraftingKnowledge;

// Java Imports
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Interaction_LearnRandomGearRecipe_Uncommon extends SimpleInstantInteraction {
    // Create the CODEC - this is required for serialization
    public static final BuilderCodec<Interaction_LearnRandomGearRecipe_Uncommon> CODEC = BuilderCodec.builder(
            Interaction_LearnRandomGearRecipe_Uncommon.class,
            Interaction_LearnRandomGearRecipe_Uncommon::new,
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
        final Player player = store.getComponent(entityRef, Player.getComponentType());

        // Get the players crafting knowledge component
        Component_CraftingKnowledge craftingKnowledge = store.getComponent(entityRef, componentTypeCraftingKnowledge);
        if (craftingKnowledge == null) return;

        // filter the players discovered recipes down to only uncommons
        List<String> uncommonRecipes = craftingKnowledge.discoveredDroppableRecipes.stream()
            .filter(s -> s.contains("_Uncommon"))
            .toList();

        // filter the players discovered recipes down to only commons
        List<String> commonRecipes = craftingKnowledge.discoveredDroppableRecipes.stream()
            .filter(
                s -> s.contains("_Common")
                && !uncommonRecipes.contains(s.replace("_Common", "_Uncommon"))
            )
            .toList();

        // if there are no items that can be unlocked, notify
        if(commonRecipes.isEmpty()) {
            player.sendMessage(Message.raw("There are no more recipes you can unlock of this tier at this time.").color(Color.GRAY));
            context.getState().state = InteractionState.Failed;
            return;
        }

        // pick a random recipe
        String randomRecipe = commonRecipes.isEmpty() ? null
            : commonRecipes.get(ThreadLocalRandom.current().nextInt(commonRecipes.size()));

        // try to learn the recipe
        craftingKnowledge.addDiscoveredRecipe(entityRef, store, randomRecipe.replace("_Common", "_Uncommon"));
    }
}
