package com.example.hyarpg.interactions;

// Hytale Imports
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

// Mod Imports
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.modules.Module_RPGSystem;

public class Interaction_RespecSkillTree extends SimpleInstantInteraction {

    public static final BuilderCodec<Interaction_RespecSkillTree> CODEC = BuilderCodec.builder(Interaction_RespecSkillTree.class, Interaction_RespecSkillTree::new, SimpleInstantInteraction.CODEC).build();
    @Override
    protected void firstRun(@NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext context, @NonNullDecl CooldownHandler cooldownHandler) {
        final Ref<EntityStore> ref = context.getEntity();
        final Store<EntityStore> store = ref.getStore();
        final World world = store.getExternalData().getWorld();

        world.execute(() -> {
            // get RPG player component
            Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPGSystem.componentTypeRPGPlayer);
            if (rpgPlayer == null) return;

            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;

            // Refund the players library
            rpgPlayer.skillPoints += rpgPlayer.skillLibrary.refund(rpgPlayer);

            // Refresh player stats
            rpgPlayer.calculateGearScore(ref, store);
            rpgPlayer.calculateAffixStats(ref, store);
        });
    }
}