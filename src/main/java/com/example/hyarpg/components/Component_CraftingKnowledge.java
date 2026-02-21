package com.example.hyarpg.components;

// Hytale Imports
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;

// Java Imports
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Component_CraftingKnowledge implements Component<EntityStore> {
    // List of items the player has discovered
    private Set<String> discoveredItems = new HashSet<>();

    // persisted - raw string value from codec because storing a hashset is not practical
    public String discoveredItemsRaw = "";

    // Persisted component data
    public static final BuilderCodec<Component_CraftingKnowledge> CODEC = BuilderCodec.builder(Component_CraftingKnowledge.class, Component_CraftingKnowledge::new)
        .append(new KeyedCodec<>("DiscoveredItems", Codec.STRING),
            ((comp, value) -> {
                comp.discoveredItemsRaw = value;
                comp.discoveredItems = new HashSet<>(Arrays.asList(value.split(",")));
            }),
            comp -> comp.discoveredItemsRaw
        )
        .add()
        .build();

    // Default no-arg constructor (required for component registration)
    public Component_CraftingKnowledge() {}

    // try to register a discovered item
    public void addDiscoveredItem(PlayerRef playerRef, String itemId, String itemName) {
        // if the add is successful rebuild the raw string
        if (discoveredItems.add(itemId)) {
            // Show the discovered notification
            NotificationUtil.sendNotification(
                playerRef.getPacketHandler(),
                Message.translation("server.hyarpg.notifications.discovered_item").param("item", Message.translation(itemName)),
                NotificationStyle.Success
            );

            // update the serialized value of discovered map
            discoveredItemsRaw = String.join(",", discoveredItems);
        }

    }

    // required for Hytale ECS system
    @Override
    public Component<EntityStore> clone() {
        Component_CraftingKnowledge copy = new Component_CraftingKnowledge();
        return copy;
    }
}
