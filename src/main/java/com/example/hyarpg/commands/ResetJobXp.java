package com.example.hyarpg.commands;

// Hytale Imports
import com.example.hyarpg.components.Component_JobSkills;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class ResetJobXp extends CommandBase {

    private final RequiredArg<String> PLAYER;
    private final OptionalArg<String> JOB;

    public ResetJobXp() {
        // name, description
        super("HyARPG_Reset_Job_XP", "Reset XP for a specific job or all jobs for a given player.", false);

        this.PLAYER = this.withRequiredArg("PLAYER", "The name of the player to reset.", ArgTypes.STRING);
        this.JOB    = this.withOptionalArg("JOB",    "The job ID to reset (e.g. Logging). Omit to reset all jobs.", ArgTypes.STRING);

        // make sure this command can only be used by admins
        requirePermission(HytalePermissions.fromCommand("admin"));
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        String playerName = commandContext.get(PLAYER);
        String jobId      = commandContext.get(JOB);

        // loop over all players and find the targeted one
        PlayerRef targetedPlayer = null;
        for (PlayerRef player : Universe.get().getPlayers()) {
            if (!player.getUsername().equalsIgnoreCase(playerName)) continue;
            targetedPlayer = player;
        }

        // bail if player not found
        if (targetedPlayer == null) return;

        // get the targeted refs
        Ref<EntityStore> ref   = targetedPlayer.getReference();
        Store<EntityStore> store = ref.getStore();

        // execute on next world tick
        World world = Universe.get().getWorld(targetedPlayer.getWorldUuid());
        final String finalJobId = jobId;
        world.execute(() -> {
            Component_JobSkills jobSkills = store.getComponent(ref, Component_JobSkills.getComponentType());
            if (jobSkills == null) return;

            if (finalJobId != null) {
                // reset a single job
                jobSkills.setXP(finalJobId, 0);
            } else {
                // reset all jobs
                jobSkills.resetAllXp();
            }
        });
    }
}