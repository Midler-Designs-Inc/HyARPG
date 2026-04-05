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

public class ResetDiscoveredRooms extends CommandBase {

    private final RequiredArg<String> PLAYER;

    public ResetDiscoveredRooms() {
        super("HyARPG_Reset_Discovered_Rooms", "Reset all discovered room recipes for a given player. This allows room discovery to refire.", false);

        this.PLAYER = this.withRequiredArg("PLAYER", "The name of the player to reset.", ArgTypes.STRING);

        requirePermission(HytalePermissions.fromCommand("admin"));
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        String playerName = commandContext.get(PLAYER);

        PlayerRef targetedPlayer = null;
        for (PlayerRef player : Universe.get().getPlayers()) {
            if (!player.getUsername().equalsIgnoreCase(playerName)) continue;
            targetedPlayer = player;
        }

        if (targetedPlayer == null) return;

        Ref<EntityStore> ref = targetedPlayer.getReference();
        Store<EntityStore> store = ref.getStore();

        World world = Universe.get().getWorld(targetedPlayer.getWorldUuid());
        world.execute(() -> {
            Component_CraftingKnowledge craftingKnowledge = store.getComponent(ref, Module_RPGSystem.componentTypeCraftingKnowledge);
            if (craftingKnowledge == null) return;

            craftingKnowledge.discoveredRoomRecipes.clear();
            craftingKnowledge.discoveredRoomRecipesRaw = "";
        });
    }
}