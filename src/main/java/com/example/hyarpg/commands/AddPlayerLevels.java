package com.example.hyarpg.commands;

// Hytale Imports

import com.example.hyarpg.components.Component_RPG_Player;
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

public class AddPlayerLevels extends CommandBase {

    private final RequiredArg<String> PLAYER;
    private final RequiredArg<Integer> VALUE;

    public AddPlayerLevels() {
        // Name, Description, Requires OP
        super("HyARPG_Add_Player_Levels", "Adds X levels to a targeted player.", false);

        this.PLAYER = this.withRequiredArg("PLAYER", "The name of the player to target.", ArgTypes.STRING);
        this.VALUE = this.withRequiredArg("VALUE", "The amount of levels to add to the targeted player.", ArgTypes.INTEGER);

        // make sure this command can only be used by admins
        requirePermission(HytalePermissions.fromCommand("admin"));
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        String playerName = commandContext.get(PLAYER);
        Integer levelsToAdd = commandContext.get(VALUE);
        if (levelsToAdd < 1) return;

        // Loop over all players and check for the targeted player
        PlayerRef targetedPlayer = null;
        for (PlayerRef player : Universe.get().getPlayers()) {
            // check if this is the targeted player
            if (!player.getUsername().equalsIgnoreCase(playerName)) continue;
            targetedPlayer = player;
        }

        // if the targeted player was not found, bail
        if(targetedPlayer == null) return;
        final PlayerRef lambdaSafeTargetedPlayer = targetedPlayer;

        // get the targeted refs
        Ref<EntityStore> ref = targetedPlayer.getReference();
        Store<EntityStore> store = ref.getStore();

        // get the players world and execute command on next world tick
        World world = Universe.get().getWorld(targetedPlayer.getWorldUuid());
        world.execute(() -> {
            // get RPG player component
            Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPGSystem.componentTypeRPGPlayer);
            if (rpgPlayer == null) return;

            // set the players available skill points
            double requiredXP = rpgPlayer.calculateTotalXPRequiredToReachTargetLevel(rpgPlayer.level + levelsToAdd) - rpgPlayer.xp;
            rpgPlayer.awardXP(lambdaSafeTargetedPlayer, requiredXP);
        });
    }
}
