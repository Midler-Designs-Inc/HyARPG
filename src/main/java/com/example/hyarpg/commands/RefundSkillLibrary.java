package com.example.hyarpg.commands;

// Hytale Imports

import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.configs.ModConfig;
import com.example.hyarpg.modules.Module_RPG_System;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.awt.*;

public class RefundSkillLibrary extends CommandBase {

    private final RequiredArg<String> PLAYER;

    public RefundSkillLibrary() {
        // Name, Description, Requires OP
        super("HyARPG_Refund_Skills", "Refund all skill trees for a given player.", false);

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
        if (world == null) return;

        world.execute(() -> {
            // get RPG player component
            Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPG_System.componentTypeRPGPlayer);
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
