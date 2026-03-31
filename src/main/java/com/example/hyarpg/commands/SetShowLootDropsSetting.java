package com.example.hyarpg.commands;

// Hytale Imports
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.modules.Module_RPG_System;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class SetShowLootDropsSetting extends CommandBase {

    private final RequiredArg<Boolean> ENABLED;

    public SetShowLootDropsSetting() {
        // Name, Description
        super("HyARPG_Player_Settings_ShowLootDropMessages", "Show or hide the loot drop messages that are broadcasted to chat.", false);

        this.ENABLED = this.withRequiredArg("ENABLED", "true/false", ArgTypes.BOOLEAN);
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        // Ensure the sender is a player before proceeding
        commandContext.senderAs(Player.class).getWorld().execute(() -> {
            Player player = commandContext.senderAs(Player.class);
            boolean enabled = commandContext.get(ENABLED);

            Ref<EntityStore> ref = player.getReference();
            Store<EntityStore> store = ref.getStore();

            // get RPG player component
            Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPG_System.componentTypeRPGPlayer);
            if (rpgPlayer == null) return;

            // update the settings
            rpgPlayer.showLootDrops = enabled;
        });
    }
}
