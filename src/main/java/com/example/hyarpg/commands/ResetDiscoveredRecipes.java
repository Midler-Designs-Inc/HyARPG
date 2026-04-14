package com.example.hyarpg.commands;

// Hytale Imports

import com.example.hyarpg.components.Component_CraftingKnowledge;
import com.example.hyarpg.modules.Module_RPGSystem;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class ResetDiscoveredRecipes extends CommandBase {

    private final RequiredArg<String> PLAYER;

    public ResetDiscoveredRecipes() {
        // Name, Description
        super("HyARPG_Reset_Discovered_Recipes", "Reset all discovered component recipes for a given player. This allows recipe discovery to refire.", false);

        this.PLAYER = this.withRequiredArg("PLAYER", "The name of the player to refund.", ArgTypes.STRING);

        // make sure this command can only be used by admins
        requirePermission(HytalePermissions.fromCommand("admin"));
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        String playerName = commandContext.get(PLAYER);

        // Loop over all players and check for the targeted player
        PlayerRef targetedPlayer = null;
        for (PlayerRef player : Universe.get().getPlayers()) {
            // check if this is the targeted player
            if (!player.getUsername().equalsIgnoreCase(playerName)) continue;
            targetedPlayer = player;
        }

        // if the targeted player was not found, bail
        if(targetedPlayer == null) return;

        // get the targeted refs
        Ref<EntityStore> ref = targetedPlayer.getReference();
        Store<EntityStore> store = ref.getStore();

        // get the players world and execute command on next world tick
        World world = Universe.get().getWorld(targetedPlayer.getWorldUuid());
        world.execute(() -> {
            // get RPG player component
            Component_CraftingKnowledge craftingKnowledge = store.getComponent(ref, Module_RPGSystem.componentTypeCraftingKnowledge);
            if (craftingKnowledge == null) return;

            // reset the players discovered ingredients
            craftingKnowledge.resetDiscoveredRecipes(ref);
        });
    }
}
