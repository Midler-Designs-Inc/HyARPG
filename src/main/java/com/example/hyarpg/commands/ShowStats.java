package com.example.hyarpg.commands;

// Hytale Imports
import com.example.hyarpg.ui.Page_RPGStats;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Java Imports
import javax.annotation.Nonnull;

public class ShowStats extends CommandBase {

    public ShowStats() {
        // Name, Description
        super("stats", "Show the player's gear affixes and character stats.", false);
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
            Ref<EntityStore> ref = player.getReference();
            Store<EntityStore> store = ref.getStore();
            Page_RPGStats.open(ref, store);
        });
    }
}
