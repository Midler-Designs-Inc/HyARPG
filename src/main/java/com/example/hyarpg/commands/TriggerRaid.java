package com.example.hyarpg.commands;

// Hytale Imports
import com.example.hyarpg.HyARPGPlugin;
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.modules.Module_RPGSystem;
import com.example.hyarpg.modules.Module_RaidSystem;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
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

public class TriggerRaid extends CommandBase {

    private final HyARPGPlugin plugin;

    private final RequiredArg<String> PLAYER;
    private final RequiredArg<String> RAID_TYPE;

    public TriggerRaid(HyARPGPlugin plugin) {
        // Name, Description, Requires OP
        super("HyARPG_Trigger_Raid", "Manually triggers a raid on a targeted player for testing purposes.", false);

        this.plugin = plugin;
        this.PLAYER = this.withRequiredArg("PLAYER", "The name of the player to target.", ArgTypes.STRING);
        this.RAID_TYPE = this.withRequiredArg("RAID_TYPE", "The type of raid to trigger: 'base' or 'player'.", ArgTypes.STRING);

        // make sure this command can only be used by admins
        requirePermission(HytalePermissions.fromCommand("admin"));
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        Player sender = commandContext.senderAs(Player.class);
        String playerName = commandContext.get(PLAYER);
        String raidType = commandContext.get(RAID_TYPE).toLowerCase();

        if (!raidType.equals("base") && !raidType.equals("player")) {
            sender.sendMessage(Message.raw("[TriggerRaid] Invalid raid type '" + raidType + "' — must be 'base' or 'player'."));
            return;
        }

        // Loop over all players and find the targeted player
        PlayerRef targetedPlayer = null;
        for (PlayerRef player : Universe.get().getPlayers()) {
            if (!player.getUsername().equalsIgnoreCase(playerName)) continue;
            targetedPlayer = player;
        }

        // if the targeted player was not found, bail
        if (targetedPlayer == null) {
            sender.sendMessage(Message.raw("[TriggerRaid] Player '" + playerName + "' not found."));
            return;
        }

        final PlayerRef lambdaSafeTargetedPlayer = targetedPlayer;
        final String lambdaSafeRaidType = raidType;

        Ref<EntityStore> ref = targetedPlayer.getReference();
        Store<EntityStore> store = ref.getStore();

        World world = Universe.get().getWorld(targetedPlayer.getWorldUuid());
        world.execute(() -> {
            Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPGSystem.componentTypeRPGPlayer);
            if (rpgPlayer == null) return;

            if (lambdaSafeRaidType.equals("base")) {
                plugin.raidSystem.startBaseRaid(lambdaSafeTargetedPlayer, ref, store, world);
            } else {
                plugin.raidSystem.startPlayerRaid(lambdaSafeTargetedPlayer, ref, store, world);
            }

            sender.sendMessage(Message.raw("[TriggerRaid] Manually triggered '" + lambdaSafeRaidType + "' raid for player " + lambdaSafeTargetedPlayer.getUsername()));
        });
    }
}