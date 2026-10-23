package com.example.hyarpg.interactions;

// Hytale Imports
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.ListTransaction;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.components.Component_Grave;
import com.example.hyarpg.modules.Module_RPGSystem;
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.utils.WorldBlockAccess;

// Java Imports
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3d;

public class Interaction_RecallPlayerGrave extends SimpleInstantInteraction {

    public static final BuilderCodec<Interaction_RecallPlayerGrave> CODEC = BuilderCodec.builder(Interaction_RecallPlayerGrave.class, Interaction_RecallPlayerGrave::new, SimpleInstantInteraction.CODEC).build();

    @Override
    protected void firstRun(@NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext context, @NonNullDecl CooldownHandler cooldownHandler) {
        final Ref<EntityStore> ref = context.getEntity();
        final Store<EntityStore> store = ref.getStore();
        final World world = store.getExternalData().getWorld();

        world.execute(() -> {
            // get the interacting player's RPG component and resolve their grave position
            Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPGSystem.componentTypeRPGPlayer);
            if (rpgPlayer == null || rpgPlayer.gravePosition == null) return;

            String[] parts = rpgPlayer.gravePosition.split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);

            world.getChunkStore().getChunkSectionReferenceAtBlockAsync(x, y, z).thenAccept(sectionRef -> {
                if (sectionRef == null || !sectionRef.isValid()) return;

                // verify the grave component is still there
                var graveEntityRefResult = WorldBlockAccess.getBlockEntityReference(world, x, y, z);
                if (graveEntityRefResult.isEmpty()) return;
                Ref<ChunkStore> graveEntityRef = graveEntityRefResult.get();
                Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
                Component_Grave grave = chunkStore.getComponent(graveEntityRef, Module_RPGSystem.componentTypeGrave);
                if (grave == null) return;

                // get the grave's item container
                ItemContainerBlock graveContainer = chunkStore.getComponent(graveEntityRef, ItemContainerBlock.getComponentType());
                if (graveContainer == null) return;

                // collect all items out of the grave container
                List<ItemStack> graveItems = new ArrayList<>(graveContainer.getItemContainer().removeAllItemStacks());

                // push all items into the player's inventory, collect overflow that didn't fit
                List<ItemStack> overflow = new ArrayList<>();
                InventoryComponent.Storage storage = store.getComponent(ref, InventoryComponent.Storage.getComponentType());
                if (storage != null) {
                    ListTransaction<ItemStackTransaction> transaction = storage.getInventory().addItemStacks(graveItems);
                    for (ItemStackTransaction t : transaction.getList()) {
                        ItemStack remainder = t.getRemainder();
                        if (remainder != null && !remainder.isEmpty()) overflow.add(remainder);
                    }
                }
                else overflow.addAll(graveItems);

                // spawn overflow items around the player
                if (!overflow.isEmpty()) {
                    TransformComponent ownerTransform = store.getComponent(ref, TransformComponent.getComponentType());
                    HeadRotation ownerHeadRotation = store.getComponent(ref, HeadRotation.getComponentType());
                    if (ownerTransform != null && ownerHeadRotation != null) {
                        Vector3d dropPosition = new Vector3d(ownerTransform.getPosition()).add(0.0, 1.0, 0.0);
                        Holder<EntityStore>[] dropEntities = ItemComponent.generateItemDrops(store, overflow, dropPosition, ownerHeadRotation.getRotation().clone());
                        store.addEntities(dropEntities, AddReason.SPAWN);
                    }
                }

                // clear the grave before breaking so the block drop doesn't duplicate items
                graveContainer.getItemContainer().clear();

                // break the grave block
                if (!WorldBlockAccess.breakBlockIfLoaded(world, x, y, z, 0, null)) {
                    System.err.println("[HyARPG] Grave block could not be broken after its contents were recalled");
                    return;
                }

                // clear the grave position off the player now that it's been recalled
                rpgPlayer.gravePosition = null;
            });
        });
    }
}