package com.example.hyarpg.commands;

// Hytale Imports
import com.example.hyarpg.ui.CustomForgeCraftingPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Java imports
import javax.annotation.Nonnull;

public class ShowCraftingPage extends CommandBase {

    public ShowCraftingPage() {
        // Name, Description
        super("crafting", "Show the craftingUI.", false);
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        Player sender = commandContext.senderAs(Player.class);
        Ref<EntityStore> ref = sender.getReference();
        Store<EntityStore> store = ref.getStore();

        World world = sender.getWorld();

        world.execute(() -> {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

            sender.getPageManager().openCustomPage(ref, store, new CustomForgeCraftingPage(playerRef));
        });
    }
}
