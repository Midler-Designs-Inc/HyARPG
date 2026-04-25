package com.example.hyarpg.interactions;

// Hytale Imports

import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.modules.Module_RPGSystem;
import com.example.hyarpg.utils.skills.SkillNode;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
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
            // get applicable entity components and validate we got them
            Player player = store.getComponent(entityRef, Player.getComponentType());
            Component_RPG_Player rpgPlayer = store.getComponent(entityRef, Module_RPGSystem.componentTypeRPGPlayer);
            ComponentType<EntityStore, EntityStatMap> statMapType = EntityStatsModule.get().getEntityStatMapComponentType();
            EntityStatMap statMap = store.getComponent(entityRef, statMapType);
            if (player == null || rpgPlayer == null || statMap == null) return;

            // if no ability set, alert the player
            if (rpgPlayer.secondaryAbility == null) {
                player.sendMessage(Message.raw("You do not have an ability equipped in that slot.").color(Color.GRAY));
                return;
            }

            // check that we can find the skill node which has the ability data, bail if we cant find it
            SkillNode node = rpgPlayer.skillLibrary.findNode(rpgPlayer.secondaryAbility);
            if(node == null) {
                player.sendMessage(Message.raw("Ability not found.").color(Color.RED));
                return;
            }

            // Look up the root interaction and bail if we cant find it
            String rootInteractionId = "Root_Interaction_" + node.ability.abilityId;
            RootInteraction rootInteraction = RootInteraction.getAssetMap().getAsset(rootInteractionId);
            if (rootInteraction == null) {
                player.sendMessage(Message.raw("Ability not found: " + rootInteractionId).color(Color.RED));
                return;
            }

            // validate the player has enough of the resource to file the ability
            EntityStatValue resourceStat = statMap.get(node.ability.abilityResourceStatIndex);
            float currentValue = resourceStat.get();
            if (currentValue < node.ability.abilityResourceCost) {
                player.sendMessage(Message.raw("You do not have enough " + resourceStat.getId() + " to do that.").color(Color.RED));
                return;
            }

            // validate the abilities cooldown
            long now = System.nanoTime();
            long cooldownNanos = node.ability.cooldownSeconds * 1_000_000_000L;
            if (now - node.ability.getLastUse() < cooldownNanos) {
                long remainingSeconds = (cooldownNanos - (now - node.ability.getLastUse())) / 1_000_000_000L;
                player.sendMessage(Message.raw("Ability on cooldown for " + remainingSeconds + "s.").color(Color.RED));
                return;
            }

            // validate any weapon requirements
            if (node.ability.requiredWeapons != null && !node.ability.requiredWeapons.isEmpty()) {
                boolean requirementMet = false;

                for (ItemStack hand : new ItemStack[]{rpgPlayer.mainHandItem, rpgPlayer.offHandItem}) {
                    if (hand == null) continue;
                    Item handItem = hand.getItem();
                    if (handItem.getData() == null) continue;
                    String[] family = handItem.getData().getRawTags().get("Family");
                    if (family == null) continue;
                    for (String tag : family) {
                        if (node.ability.requiredWeapons.contains(tag)) {
                            requirementMet = true;
                            break;
                        }
                    }
                    if (requirementMet) break;
                }

                if (!requirementMet) {
                    player.sendMessage(Message.raw("You are not wielding the required weapon to use this ability.").color(Color.RED));
                    return;
                }
            }

            // Get commandBuffer from context
            var commandBuffer = context.getCommandBuffer();
            if (commandBuffer == null) return;

            // Get interaction manager
            InteractionManager interactionManager = store.getComponent(entityRef, InteractionModule.get().getInteractionManagerComponent());
            if (interactionManager == null) return;

            // Deduct the resource cost from teh resource
            if (node.ability.abilityResourceCost > 0) {
                statMap.setStatValue(node.ability.abilityResourceStatIndex, Math.max(0, (currentValue - node.ability.abilityResourceCost)));

                // if ability costs stamina, set the stamina regen delay
                if (node.ability.abilityResourceStatIndex == DefaultEntityStatTypes.getStamina()) {
                    int staminaRegenDelayStatIndex = EntityStatType.getAssetMap().getIndex("StaminaRegenDelay");
                    statMap.setStatValue(staminaRegenDelayStatIndex, -1);
                }
            }

            // only queue an interaction chain if the root interaction has operations defined
            if (rootInteraction.getOperationMax() > 0) {
                InteractionContext newCtx = InteractionContext.forInteraction(interactionManager, entityRef, InteractionType.Use, commandBuffer);
                InteractionChain chain = interactionManager.initChain(InteractionType.Use, newCtx, rootInteraction, false);
                interactionManager.queueExecuteChain(chain);
            }

            // call the ability execute for any additional functionality that is ability dependent
            node.ability.execute(entityRef);
            node.ability.setLastUse(now);
        } catch (NoClassDefFoundError e) {
            // Class not yet loaded, retry on next tick or log
            HytaleLogger.getLogger().at(Level.WARNING).log("Page_RPGStats not loaded yet: %s", e.getMessage());
        }
    }
}
