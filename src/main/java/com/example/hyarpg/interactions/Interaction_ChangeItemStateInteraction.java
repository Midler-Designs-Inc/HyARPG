package com.example.hyarpg.interactions;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.builder.BuilderField;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class Interaction_ChangeItemStateInteraction extends SimpleInstantInteraction {

    // Add a configurable target state via CODEC
    private String targetState;

    public static final BuilderCodec<Interaction_ChangeItemStateInteraction> CODEC = BuilderCodec.builder(
        Interaction_ChangeItemStateInteraction.class, Interaction_ChangeItemStateInteraction::new, SimpleInstantInteraction.CODEC
    ).addField(new KeyedCodec<>("TargetState", BuilderCodec.STRING),
        (instance, value) -> instance.targetState = value,
        instance -> instance.targetState
    )
    .build();

    @Override
    protected void firstRun(@Nonnull InteractionType interactionType, @Nonnull InteractionContext interactionContext, @Nonnull CooldownHandler cooldownHandler) {

        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
        if (commandBuffer == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        Ref<EntityStore> ref = interactionContext.getEntity();
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        ItemStack heldItem = interactionContext.getHeldItem();
        if (heldItem == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        // Replace held item with same item ID but new state
        try {
            Inventory inventory = player.getInventory();
            ItemStack newItem = heldItem.withState(targetState);
            byte activeSlot = inventory.getActiveHotbarSlot();
            inventory.getHotbar().replaceItemStackInSlot(activeSlot, heldItem, newItem);
        } catch (IllegalArgumentException e) {
            interactionContext.getState().state = InteractionState.Failed;
        }
    }
}
