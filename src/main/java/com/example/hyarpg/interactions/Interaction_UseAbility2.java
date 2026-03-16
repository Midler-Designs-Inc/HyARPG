package com.example.hyarpg.interactions;

// Hytale Imports

import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.modules.Module_RPG_System;
import com.example.hyarpg.utils.skills.SkillNode;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.logging.Level;

public class Interaction_UseAbility2 extends SimpleInstantInteraction {
    // Create the CODEC - this is required for serialization
    public static final BuilderCodec<Interaction_UseAbility2> CODEC = BuilderCodec.builder(
        Interaction_UseAbility2.class,
        Interaction_UseAbility2::new,
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
            // get applicable entity components and validate we got them
            Player player = store.getComponent(entityRef, Player.getComponentType());
            Component_RPG_Player rpgPlayer = store.getComponent(entityRef, Module_RPG_System.componentTypeRPGPlayer);
            ComponentType<EntityStore, EntityStatMap> statMapType = EntityStatsModule.get().getEntityStatMapComponentType();
            EntityStatMap statMap = store.getComponent(entityRef, statMapType);
            if (player == null || rpgPlayer == null || statMap == null) return;

            // if no ability set, alert the player
            if (rpgPlayer.primaryAbility == null) {
                player.sendMessage(Message.raw("You do not have an ability equipped in that slot.").color(Color.GRAY));
                return;
            }

            // check that we can find the skill node which has the ability data, bail if we cant find it
            SkillNode node = rpgPlayer.skillLibrary.findNode(rpgPlayer.primaryAbility);
            if(node == null) {
                player.sendMessage(Message.raw("Ability not found.").color(Color.RED));
                return;
            }

            // Look up the root interaction and bail if we cant find it
            String rootInteractionId = "Root_Interaction_" + node.abilityId;
            RootInteraction rootInteraction = RootInteraction.getAssetMap().getAsset(rootInteractionId);
            if (rootInteraction == null) {
                player.sendMessage(Message.raw("Ability not found: " + rootInteractionId).color(Color.RED));
                return;
            }

            // validate the player has enough of the resource to file the ability
            EntityStatValue resourceStat = statMap.get(node.abilityResourceStatIndex);
            float currentValue = resourceStat.get();
            if (currentValue < node.abilityResourceCost) {
                player.sendMessage(Message.raw("You do not have enough " + resourceStat.getId() + " to do that.").color(Color.RED));
                return;
            }

            // Get commandBuffer from context
            var commandBuffer = context.getCommandBuffer();
            if (commandBuffer == null) return;

            // Get interaction manager
            InteractionManager interactionManager = store.getComponent(entityRef, InteractionModule.get().getInteractionManagerComponent());
            if (interactionManager == null) return;

            // clear the players signature energy
            statMap.setStatValue(node.abilityResourceStatIndex, Math.max(0, (currentValue - node.abilityResourceCost)));

            // create a new context for the interaction and init a new interaction chain
            InteractionContext newCtx = InteractionContext.forInteraction(interactionManager, entityRef, InteractionType.Use, commandBuffer);
            InteractionChain chain = interactionManager.initChain(InteractionType.Use, newCtx, rootInteraction, false);

            // queue the interaction
            interactionManager.queueExecuteChain(chain);
        } catch (NoClassDefFoundError e) {
            // Class not yet loaded, retry on next tick or log
            HytaleLogger.getLogger().at(Level.WARNING).log("Page_RPGStats not loaded yet: %s", e.getMessage());
        }
    }
}
