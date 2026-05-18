package com.example.hyarpg.commands;

// Hytale Imports
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.ui.CustomPage_JobSkillsPage;

// Java Imports
import javax.annotation.Nonnull;

public class ShowJobs extends CommandBase {

    public ShowJobs() {
        // Name, Description
        super("jobs", "Show the available job skills.", false);
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
        world.execute(() -> {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            Player player = store.getComponent(ref, Player.getComponentType());

            player.getPageManager().openCustomPage(ref, store, new CustomPage_JobSkillsPage(playerRef));
        });
    }
}
