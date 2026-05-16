package com.example.hyarpg.interactions;

// Hytale Imports
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.modules.Module_RPGSystem;
import com.example.hyarpg.components.Component_Grave;

// Java Imports
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.joml.Vector3d;
import java.util.UUID;

public class Interaction_RezPlayer extends SimpleInstantInteraction {

    public static final BuilderCodec<Interaction_RezPlayer> CODEC = BuilderCodec.builder(Interaction_RezPlayer.class, Interaction_RezPlayer::new, SimpleInstantInteraction.CODEC).build();

    @Override
    protected void firstRun(@NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext context, @NonNullDecl CooldownHandler cooldownHandler) {
        Ref<EntityStore> rezzingPlayerRef = context.getEntity();
        if (rezzingPlayerRef == null || !rezzingPlayerRef.isValid()) return;

        Store<EntityStore> entityStore = rezzingPlayerRef.getStore();
        World world = entityStore.getExternalData().getWorld();

        // get the block position of the grave
        BlockPosition targetBlock = context.getTargetBlock();
        if (targetBlock == null) return;

        int x = targetBlock.x;
        int y = targetBlock.y;
        int z = targetBlock.z;

        world.execute(() -> {
            // get the chunk the grave block is in
            long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
            WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
            if (chunk == null) return;

            BlockComponentChunk bcc = chunk.getBlockComponentChunk();
            if (bcc == null) return;

            int blockInColumnIndex = ChunkUtil.indexBlockInColumn(x, y, z);

            // read the grave component directly off the block
            Component_Grave grave = bcc.getComponent(blockInColumnIndex, Module_RPGSystem.componentTypeGrave);
            if (grave == null) return;

            // get the dead player — bail if they're not found or already alive
            UUID deadUuid = grave.deadPlayerUuid;
            PlayerRef deadPlayerRef = Universe.get().getPlayer(deadUuid);
            if (deadPlayerRef == null) return;

            Ref<EntityStore> deadRef = deadPlayerRef.getReference();
            if (deadRef == null || !deadRef.isValid()) return;

            // bail if the player is not actually dead — no death component means they're alive
            DeathComponent deathComponent = entityStore.getComponent(deadRef, DeathComponent.getComponentType());
            if (deathComponent == null) return;

            // get the grave's item container and return all items to the dead player's storage
            Ref<ChunkStore> graveRef = chunk.getBlockComponentEntity(x, y, z);
            if (graveRef != null && graveRef.isValid()) {
                Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
                ItemContainerBlock graveContainer = chunkStore.getComponent(graveRef, ItemContainerBlock.getComponentType());

                // collect all items from the grave and add them back to the player's storage
                if (graveContainer != null) {
                    InventoryComponent.Storage storage = entityStore.getComponent(deadRef, InventoryComponent.Storage.getComponentType());
                    if (storage != null) {
                        for (short i = 0; i < graveContainer.getItemContainer().getCapacity(); i++) {
                            ItemStack stack = graveContainer.getItemContainer().getItemStack(i);
                            if (stack != null && !stack.isEmpty()) storage.getInventory().addItemStack(stack);
                        }
                        graveContainer.getItemContainer().clear();
                    }
                }
            }

            // break the grave block now that items are returned
            world.breakBlock(x, y, z, 0);

            // reposition the player at the grave and remove their death component to respawn them
            TransformComponent deadTransform = entityStore.getComponent(deadRef, TransformComponent.getComponentType());
            if (deadTransform != null) deadTransform.setPosition(new Vector3d(x + 0.5, y, z + 0.5));
            entityStore.removeComponent(deadRef, DeathComponent.getComponentType());
        });
    }
}