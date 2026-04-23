package com.example.hyarpg.utils;

// Hytale Imports
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.window.WindowType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.ui.CustomPage_Inventory;

// Java Imports
import com.google.gson.JsonObject;

public class InterceptPocketCraftingWindow extends Window {

    public InterceptPocketCraftingWindow() {
        super(WindowType.PocketCrafting);
    }

    @Override
    protected boolean onOpen0(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());

        if (player.getGameMode().equals(GameMode.Creative)) {
            return true; // let native inventory open normally
        }

        PlayerRef playerRef = this.getPlayerRef();
        player.getPageManager().openCustomPage(ref, store, new CustomPage_Inventory(playerRef));
        return false;
    }

    @Override
    protected void onClose0(Ref<EntityStore> ref, ComponentAccessor<EntityStore> accessor) {}

    @Override
    public JsonObject getData() {
        return new JsonObject();
    }
}