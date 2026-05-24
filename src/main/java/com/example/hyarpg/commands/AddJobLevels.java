package com.example.hyarpg.commands;

// Hytale Imports

import com.example.hyarpg.components.Component_JobSkills;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
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

public class AddJobLevels extends CommandBase {

    private final RequiredArg<String> PLAYER;
    private final RequiredArg<String> JOB_SKILL;
    private final RequiredArg<Integer> LEVELS;

    public AddJobLevels() {
        // Name, Description, Requires OP
        super("HyARPG_Add_Job_Levels", "Adds X levels to any job skill for the targeted player.", false);

        this.PLAYER = this.withRequiredArg("PLAYER", "The name of the player to target.", ArgTypes.STRING);
        this.JOB_SKILL = this.withRequiredArg("JOB_SKILL", "The job skill to modify.", ArgTypes.STRING);
        this.LEVELS = this.withRequiredArg("LEVELS", "The amount of levels to add to the job skill for the targeted player.", ArgTypes.INTEGER);

        // make sure this command can only be used by admins
        requirePermission(HytalePermissions.fromCommand("admin"));
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        // get command context and validate it
        String playerName = commandContext.get(PLAYER);
        String jobSkill = commandContext.get(JOB_SKILL);
        Integer levelsToAdd = commandContext.get(LEVELS);
        if (playerName == null || jobSkill == null || levelsToAdd < 1) return;

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

        // get the targeted ref/player
        Ref<EntityStore> ref = targetedPlayer.getReference();
        if (ref == null || !ref.isValid()) return;

        // get the players world and create a lambda safe reference to the PlayerRef
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        PlayerRef finalTargetedPlayer = targetedPlayer;

        // execute command on next world tick
        world.execute(() -> {
            // get RPG player component
            Component_JobSkills jobSkills = store.getComponent(ref, Component_JobSkills.getComponentType());
            if (jobSkills == null) return;

            // get the job xp based on the passed job name, then find it's level
            long jobXP = switch (jobSkill) {
                case "Alchemy" -> jobSkills.alchemyXp;
                case "Bartering" -> jobSkills.barteringXp;
                case "Beastmastery" -> jobSkills.beastmasteryXp;
                case "Building" -> jobSkills.buildingXp;
                case "Cooking" -> jobSkills.cookingXp;
                case "Crafting" -> jobSkills.craftingXp;
                case "Exploring" -> jobSkills.exploringXp;
                case "Farming" -> jobSkills.farmingXp;
                case "Fishing" -> jobSkills.fishingXp;
                case "Logging" -> jobSkills.loggingXp;
                case "Mining" -> jobSkills.miningXp;
                case "Performing" -> jobSkills.performingXp;
                case "Thievery" -> jobSkills.thieveryXp;
                default -> 0;
            };

            // calculate the amount of xp needed to reach the desired level
            int jobLevel = jobSkills.calculateLevelFromXP(jobXP);
            long requiredXP = jobSkills.calculateTotalXPForLevel(jobLevel + levelsToAdd) - jobXP;

            // award the required XP to bring the players job skill up to the desired level
            jobSkills.awardXP(lambdaSafeTargetedPlayer, jobSkill, requiredXP);
        });
    }
}
