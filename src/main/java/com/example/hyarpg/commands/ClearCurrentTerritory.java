package com.example.hyarpg.commands;

// Hytale Imports
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.modules.Module_RPGSystem;
import com.example.hyarpg.utils.rooms.TerritoryData;
import com.example.hyarpg.utils.rooms.WorldRoomRegistry;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class ClearCurrentTerritory extends CommandBase {

    public ClearCurrentTerritory() {
        super("HyARPG_Clear_Current_Territory", "Clears the territory at the executing admin's current location.", false);
        requirePermission(HytalePermissions.fromCommand("admin"));
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        Player sender = commandContext.senderAs(Player.class);
        Ref<EntityStore> ref = sender.getReference();
        Store<EntityStore> store = ref.getStore();

        // Get world without any component access
        World world = sender.getWorld();

        world.execute(() -> {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

            Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPGSystem.componentTypeRPGPlayer);
            if (rpgPlayer == null) {
                playerRef.sendMessage(Message.raw("Could not find RPG player component."));
                return;
            }

            TerritoryData territory = rpgPlayer.territory;
            if (territory == null) {
                playerRef.sendMessage(Message.raw("You are not inside any registered territory."));
                return;
            }

            WorldRoomRegistry registry = WorldRoomRegistry.get(world);
            if (registry == null) {
                playerRef.sendMessage(Message.raw("Registry not found for this world."));
                return;
            }

            registry.removeTerritory(territory);
            registry.saveAsync(world);

            playerRef.sendMessage(Message.raw(
                    "Territory at (" + territory.getCenter().x + ", " + territory.getCenter().y + ", " + territory.getCenter().z + ") cleared."
            ));
        });
    }
}