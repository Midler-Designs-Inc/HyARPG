package com.example.hyarpg.commands;

// Hytale Imports

import com.example.hyarpg.configs.ModConfig;
import com.example.hyarpg.ui.Page_SkillTree;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.Argument;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class ToggleHunger extends CommandBase {

    private final DefaultArg<Boolean> ENABLED;

    public ToggleHunger() {
        // Name, Description, Requires OP
        super("HyARPG_Hunger_TickEnabled", "Turn the hunger system on or off. You will need to relog for the HUD to show/hide the bar.", false);

        this.ENABLED = this.withDefaultArg("ENABLED", "Rather or not the hunger tick is enabled.", ArgTypes.BOOLEAN, true, "Default: true");

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
