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

    // register the command name/description and its two required arguments
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
        // get command context — sender ref, their store, and the world that owns that store
        Ref<EntityStore> ref = commandContext.senderAsPlayerRef();
        Store<EntityStore> store = ref.getStore();
        World senderWorld = store.getExternalData().getWorld();

        // get command variables
        String playerName = commandContext.get(PLAYER);
        String raidType = commandContext.get(RAID_TYPE).toLowerCase();

        // dispatch onto the sender's world thread before touching any of their component data
        senderWorld.execute(() -> {
            // fetch the sender's PlayerRef now that we're safely on their owning world thread
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) return;

            // validate the raid type argument
            if (!raidType.equals("base") && !raidType.equals("player")) {
                playerRef.sendMessage(Message.raw("[TriggerRaid] Invalid raid type '" + raidType + "' — must be 'base' or 'player'."));
                return;
            }

            // loop over all players and find the targeted player
            PlayerRef targetedPlayer = null;
            for (PlayerRef player : Universe.get().getPlayers()) {
                if (!player.getUsername().equalsIgnoreCase(playerName)) continue;
                targetedPlayer = player;
            }

            // if the targeted player was not found, bail
            if (targetedPlayer == null) {
                playerRef.sendMessage(Message.raw("[TriggerRaid] Player '" + playerName + "' not found."));
                return;
            }

            final PlayerRef lambdaSafeTargetedPlayer = targetedPlayer;
            final String lambdaSafeRaidType = raidType;

            // get the targeted entity ref and the entity store for their world
            Ref<EntityStore> targetdRef = targetedPlayer.getReference();
            World world = Universe.get().getWorld(targetedPlayer.getWorldUuid());

            // dispatch again onto the target's world thread to safely read/modify their components
            world.execute(() -> {
                // fetch the target's RPG player data, bail if they don't have it
                Component_RPG_Player rpgPlayer = targetdRef.getStore().getComponent(targetdRef, Module_RPGSystem.componentTypeRPGPlayer);
                if (rpgPlayer == null) return;

                // bail if a raid is already in progress for this player
                if (rpgPlayer.activeRaidHudState != null) {
                    playerRef.sendMessage(Message.raw("[TriggerRaid] Player " + lambdaSafeTargetedPlayer.getUsername() + " already has an active raid in progress."));
                    return;
                }

                // trigger the raid and notify the admin who ran the command
                plugin.raidSystem.triggerRaidByCommand(lambdaSafeTargetedPlayer, targetdRef, targetdRef.getStore(), world, lambdaSafeRaidType);
                playerRef.sendMessage(Message.raw("[TriggerRaid] Manually triggered '" + lambdaSafeRaidType + "' raid for player " + lambdaSafeTargetedPlayer.getUsername()));
            });
        });
    }
}