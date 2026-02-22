package com.example.hyarpg.components;

// Hytale Imports
import com.hypixel.hytale.builtin.crafting.CraftingPlugin;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.BenchRequirement;
import com.hypixel.hytale.protocol.ItemResourceType;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;

// Java Imports
import java.util.*;

public class Component_CraftingKnowledge implements Component<EntityStore> {
    // List of items and recipes the player has discovered
    private Set<String> discoveredItems = new HashSet<>();
    private Set<String> discoveredDroppableRecipes = new HashSet<>();

    // persisted - raw string values from codec because storing a hashset is not practical
    public String discoveredItemsRaw = "";
    public String discoveredDroppableRecipesRaw = "";

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
        .append(new KeyedCodec<>("DiscoveredRecipes", Codec.STRING),
            ((comp, value) -> {
                comp.discoveredDroppableRecipesRaw = value;
                comp.discoveredDroppableRecipes = new HashSet<>(Arrays.asList(value.split(",")));
            }),
            comp -> comp.discoveredDroppableRecipesRaw
        )
        .add()
        .build();

    // Default no-arg constructor (required for component registration)
    public Component_CraftingKnowledge() {}

    // try to register a discovered item
    public boolean addDiscoveredItem(PlayerRef playerRef, Item item) {
        // set a flag so outside systems know if we actually discovered something or not
        boolean discoveredNew = false;

        // get item details
        String itemId = item.getId();
        String itemName = item.getTranslationKey();

        // if the add is successful rebuild the raw string
        if (discoveredItems.add(itemId)) {
            // check if the item has a resource type and if so silently register that also
            for(ItemResourceType resourceType : item.getResourceTypes()) {
                discoveredItems.add(resourceType.id);
            }

            // Show the discovered notification
            NotificationUtil.sendNotification(
                playerRef.getPacketHandler(),
                Message.translation("server.hyarpg.notifications.discovered_item").param("item", Message.translation(itemName)),
                NotificationStyle.Success
            );

            // update discovered flag
            discoveredNew = true;

            // update the serialized value of discovered map
            discoveredItemsRaw = String.join(",", discoveredItems);
        }

        // return the discovered flag
        return discoveredNew;
    }

    public void discoverRecipes(Ref<EntityStore> ref, Store<EntityStore> store, Item item) {
        String itemId = item.getId();
        ItemResourceType[] itemResourceTypes = item.getResourceTypes();

        // loop over all recipes
        List<CraftingRecipe> recipes = new ArrayList<>(
                CraftingRecipe.getAssetMap().getAssetMap().values()
        );
        for (CraftingRecipe recipe : recipes) {
            try {
                // check if the recipe has bench reqs and bail if not
                BenchRequirement[] requirements = recipe.getBenchRequirement();
                if (requirements == null) continue;

                // check if this recipe is discoverable
                boolean isHyARPGRecipe = false;
                for (BenchRequirement req : requirements) {
                    if(req.categories == null) continue;
                    for (String category : req.categories) {
                        if (Objects.equals(category, "Discoverable")) {
                            isHyARPGRecipe = true;
                            break;
                        }
                    }
                }
                if (!isHyARPGRecipe) continue;

                // skip if player already knows this recipe
                String recipeId = recipe.getId().replace("_Recipe_Generated_0", "");
                if (discoveredDroppableRecipes.contains(recipeId)) continue;

                // check if this recipe has an ingredient cost that matches the item we just learned
                boolean requiresDiscoveredItem = false;
                for (MaterialQuantity input : recipe.getInput()) {
                    String ingrID = input.getItemId();
                    String ingrTypeID = input.getResourceTypeId();
                    if(ingrID == null && ingrTypeID== null) break;

                    if (ingrID != null && ingrID.equals(itemId)) {
                        requiresDiscoveredItem = true;
                        break;
                    }
                    else if (ingrTypeID != null) {
                        for (ItemResourceType resourceType : itemResourceTypes) {
                            String resourceTypeID = resourceType.id;
                            if(resourceTypeID == null) break;

                            if (resourceTypeID.equals(ingrTypeID)) {
                                requiresDiscoveredItem = true;
                                break;
                            }
                        }
                    }
                }
                if (!requiresDiscoveredItem) continue;

                // check if player knows ALL ingredients for this recipe
                boolean knowsAllIngredients = true;
                String ingr = "";
                for (MaterialQuantity input : recipe.getInput()) {
                    String ingrID = input.getItemId();
                    String ingrTypeID = input.getResourceTypeId();
                    if(ingrID == null && ingrTypeID== null) break;

                    if (ingrID != null) {
                        if (!discoveredItems.contains(ingrID)) {
                            knowsAllIngredients = false;
                            break;
                        }
                        else {
                            ingr += ingrID + ", ";
                        }
                    }
                    else if (ingrTypeID != null) {
                        if (!discoveredItems.contains(ingrTypeID)) {
                            knowsAllIngredients = false;
                            break;
                        }
                        else {
                            ingr += ingrTypeID + ", ";
                        }
                    }
                }
                if (!knowsAllIngredients) continue;

                // player knows all ingredients - unlock the recipe
                CraftingPlugin.learnRecipe(ref, recipeId, store);
                discoveredDroppableRecipes.add(recipeId);

                // update the serialized value of discovered map
                discoveredDroppableRecipesRaw = String.join(",", discoveredItems);
            } catch (Exception e) {}
        }
    }

    public void alertPlayer(String msg) {
        // loop over all players and broadcast the message
        for (PlayerRef player : Universe.get().getPlayers()) {
            player.sendMessage(Message.raw(msg));
        }
    }

    // required for Hytale ECS system
    @Override
    public Component<EntityStore> clone() {
        Component_CraftingKnowledge copy = new Component_CraftingKnowledge();
        return copy;
    }
}
