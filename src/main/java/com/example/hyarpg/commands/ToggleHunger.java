package com.example.hyarpg.commands;

// Hytale Imports

import com.example.hyarpg.configs.ModConfig;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;

import javax.annotation.Nonnull;

public class ToggleHunger extends CommandBase {

    private final RequiredArg<Boolean> ENABLED;

    public ToggleHunger() {
        // Name, Description, Requires OP
        super("HyARPG_Hunger_TickEnabled", "Turn the hunger system on or off. You will need to relog for the HUD to show/hide the bar.", false);

        this.ENABLED = this.withRequiredArg("ENABLED", "Rather or not the hunger tick is enabled.", ArgTypes.BOOLEAN);

        // make sure this command can only be used by admins
        requirePermission(HytalePermissions.fromCommand("admin"));
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        boolean enabled = commandContext.get(ENABLED);
        ModConfig.get().hunger.enabled = enabled;
        ModConfig.get().save();
    }
}
