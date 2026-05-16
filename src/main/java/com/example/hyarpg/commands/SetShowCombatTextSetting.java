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
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class SetShowCombatTextSetting extends CommandBase {

    private final RequiredArg<Boolean> ENABLED;

    public SetShowCombatTextSetting() {
        // Name, Description
        super("HyARPG_Player_Settings_ShowCombatMessages", "Show or hide the combat messages that are broadcasted to chat.", false);

        this.ENABLED = this.withRequiredArg("ENABLED", "true/false", ArgTypes.BOOLEAN);
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        Ref<EntityStore> ref = commandContext.senderAsPlayerRef();
        Store<EntityStore> store = ref.getStore();

        // Get world
        World world = store.getExternalData().getWorld();

        // Ensure the sender is a player before proceeding
        world.execute(() -> {
            boolean enabled = commandContext.get(ENABLED);

            // get RPG player component
            Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPGSystem.componentTypeRPGPlayer);
            if (rpgPlayer == null) return;

            // update the settings
            rpgPlayer.showCombatText = enabled;
        });
    }
}
