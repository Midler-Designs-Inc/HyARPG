package com.example.hyarpg.interactions;

// Hytale Imports
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.ui.CustomPage_TerritoryPanel;
import com.example.hyarpg.utils.rooms.TerritoryData;
import com.example.hyarpg.utils.rooms.WorldRoomRegistry;


// Java Imports
import java.util.logging.Level;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class Interaction_Open_Territory_Panel extends SimpleInstantInteraction {
    public static final BuilderCodec<Interaction_Open_Territory_Panel> CODEC = BuilderCodec.builder(
            Interaction_Open_Territory_Panel.class,
            Interaction_Open_Territory_Panel::new,
            SimpleInstantInteraction.CODEC
    ).build();

    @Override
    protected void firstRun(
            @NonNullDecl InteractionType interactionType,
            @NonNullDecl InteractionContext context,
            @NonNullDecl CooldownHandler cooldownHandler
    ) {
        final Ref<EntityStore> entityRef = context.getEntity();
        final Store<EntityStore> store = entityRef.getStore();

        try {
            // get needed info or bail
            Player player = store.getComponent(entityRef, Player.getComponentType());
            PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
            BlockPosition pos = context.getTargetBlock();
            WorldRoomRegistry registry = WorldRoomRegistry.get(store.getExternalData().getWorld());
            if (player == null || playerRef == null || pos == null || registry == null) return;

            // get the territory at block location or bail
            TerritoryData territory = registry.getTerritoryAt(pos.x, pos.y, pos.z);
            if (territory == null) return;

            player.getPageManager().openCustomPage(entityRef, store, new CustomPage_TerritoryPanel(playerRef, territory));
        } catch (Exception e) {
            HytaleLogger.getLogger().at(Level.WARNING).log("Open Territory Panel failed: %s", e.getMessage());
        }
    }
}
