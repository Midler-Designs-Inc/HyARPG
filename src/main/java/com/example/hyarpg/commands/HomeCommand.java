package com.example.hyarpg.commands;

import com.hypixel.hytale.builtin.teleport.components.TeleportHistory;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class HomeCommand extends CommandBase {

    public HomeCommand() {
        super("home", "Teleport to your home respawn point.", false);
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        commandContext.senderAs(Player.class).getWorld().execute(() -> {
            Player player = commandContext.senderAs(Player.class);
            World world = player.getWorld();
            Ref<EntityStore> ref = player.getReference();
            Store<EntityStore> store = ref.getStore();

            // Save to teleport history so /tp back works
            TransformComponent transform = (TransformComponent) store.getComponent(ref, TransformComponent.getComponentType());
            HeadRotation headRotation = (HeadRotation) store.getComponent(ref, HeadRotation.getComponentType());
            if (transform != null && headRotation != null) {
                Vector3d previousPos = transform.getPosition().clone();
                Vector3f previousHeadRotation = headRotation.getRotation().clone();
                TeleportHistory history = (TeleportHistory) store.ensureAndGetComponent(ref, TeleportHistory.getComponentType());
                history.append(world, previousPos, previousHeadRotation, "Home");
            }

            // Perform the teleport
            Player.getRespawnPosition(ref, world.getName(), store).thenAcceptAsync((homeTransform) -> {
                Teleport teleportComponent = Teleport.createForPlayer((World) null, homeTransform);
                store.addComponent(ref, Teleport.getComponentType(), teleportComponent);
            }, world);
        });
    }
}