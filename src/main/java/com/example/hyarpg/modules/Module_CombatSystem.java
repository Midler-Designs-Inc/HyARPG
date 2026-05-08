package com.example.hyarpg.modules;

// Hytale Imports
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.Frozen;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

// Mod Imports
import com.example.hyarpg.utils.combat.SwingDamageGroup;
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.components.Component_RPG_Enemy;
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.configs.ModConfig;
import com.example.hyarpg.events.*;
import com.example.hyarpg.utils.affixes.EntityStats;
import com.example.hyarpg.configs.Config_World;
import com.example.hyarpg.utils.affixes.StatType;
import com.example.hyarpg.utils.items.ItemFactory;
import static com.example.hyarpg.modules.Module_RPGSystem.*;

// Java Imports
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class Module_CombatSystem {

    // Create a map for damage message colors
    public static final Map<String, Color> DAMAGE_COLORS = Map.of(
        "Fire", new Color(249, 146, 32),
        "Ice", new Color(219, 241, 253),
        "Lightning", new Color(0, 92, 165),
        "Poison",  new Color(0, 175, 0),
        "Magic", new Color(242, 89, 255),
        "Physical",  new Color(175, 175, 175)
    );

    // create some maps to use for filtering
    private static final Set<String> MOD_DAMAGE_TYPES = Set.of(
        "Physical", "Magic", "Poison", "Fire", "Ice", "Lightning"
    );

    // Map<defender_ref, Map<attacker_ref, timestamp>, damage registry to track when a player damages an enemy for xp/loot awards
    private final ConcurrentHashMap<Ref<EntityStore>, ConcurrentHashMap<Ref<EntityStore>, Long>> damageRegistry = new ConcurrentHashMap<>();

    // Properties to pool damage ticks that happen in quick succession from the same source
    private final ConcurrentHashMap<String, SwingDamageGroup> swingGroups = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final long BUCKET_MS = 10;
    private static final long FLUSH_DELAY_MS = 5;
    private String swingKey(Ref<EntityStore> attacker, Ref<EntityStore> defender) {
        return attacker + "->" + defender + "@" + (System.currentTimeMillis() / BUCKET_MS);
    }

    // exclude items with these strings in their ID
    private static final Set<String> EXCLUDED_ID_SUBSTRINGS = Set.of(
        "Ingredient_Bar",
        "Ore_",
        "Weapon_",
        "Armor_",
        "Ingredient_Leather_",
        "Ingredient_Bolt_",
        "Ingredient_Hide",
        "Ingredient_Fabric"
    );

    private static final String[] TIER_DROP_LISTS = {
        "HyARPG_Container_Tier0",
        "HyARPG_Container_Tier1",
        "HyARPG_Container_Tier2",
        "HyARPG_Container_Tier3",
        "HyARPG_Container_Tier4",
        "HyARPG_Container_Tier5",
        "HyARPG_Container_Tier6",
    };

    private static final Set<String> WEAPON_CAUSE_IDS = Set.of(
        "MainHand", "OffHand",
        "MainHand_Scalar",
        "MainHand_Fire_Scalar", "MainHand_Lightning_Scalar", "MainHand_Ice_Scalar",
        "MainHand_Poison_Scalar", "MainHand_Magic_Scalar", "MainHand_Physical_Scalar"
    );

    // initialize this module
    public Module_CombatSystem() {

        // Listen to applicable events on the mods internal event bus
        ModEventBus.register(Event_EntityPreDamaged.class, this::onEntityPreDamage);
        ModEventBus.register(Event_PlayerInteraction.class, this::onPlayerInteraction);
        ModEventBus.register(Event_NPCDeath.class, this::onEnemyKilled);

    }

    // function to flush ready damage groups
    public void tickDamageGroups(Store<EntityStore> store) {
        swingGroups.entrySet().removeIf(entry -> {
            SwingDamageGroup group = entry.getValue();
            if (!group.readyToApply) return false;
            applyDamageGroup(group);
            return true;
        });
    }

    // method for when the player performs an interaction
    private void onPlayerInteraction(Event_PlayerInteraction event){
        // get ref and store reference
        Ref<EntityStore> ref = event.getRef();
        Store<EntityStore> store = event.getStore();

        // validate the ref has an rpg player comp
        Component_RPG_Player rpgPlayer = store.getComponent(ref, componentTypeRPGPlayer);
        if(rpgPlayer == null) return;

        // if secondary action possibly blocking so timestamp it (we will check for this in damage pipeline if there is blocked damage)
        if(Objects.equals(event.getInteractionID(), "Secondary")) {
            // set the block start
            rpgPlayer.blockStart = System.nanoTime();
        }
    }

    // This function fires when an enemy dies to award XP and modify loot
    private void onEnemyKilled(Event_NPCDeath event) {
        // get event props
        CommandBuffer<EntityStore> commandBuffer = event.getCommandBuffer();
        Ref<EntityStore> ref = event.getRef();
        Store<EntityStore> store = event.getStore();

        // get the NPC component and rpgEnemy components
        NPCEntity npcComponent = commandBuffer.getComponent(ref, NPCEntity.getComponentType());
        Component_RPG_Enemy rpgEnemy = commandBuffer.getComponent(ref, componentTypeRPGEnemy);
        if (npcComponent == null || rpgEnemy == null) return;

        // get the role component
        Role role = npcComponent.getRole();
        if (role == null) return;

        // get the drop list from the role
        String dropListId = role.getDropListId();
        if (dropListId == null) return;

        // get the item module
        ItemModule itemModule = ItemModule.get();
        if (!itemModule.isEnabled()) return;

        // claim the drop so deathInstruction skips its own loot spawn
        role.setDeathItemsDropped();

        // clear NPC inventory to prevent inventoryContentsDropList items from dropping
        InventoryComponent.Storage npcStorage = commandBuffer.getComponent(ref, InventoryComponent.Storage.getComponentType());
        InventoryComponent.Hotbar npcHotbar = commandBuffer.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        if (npcStorage != null) npcStorage.getInventory().clear();
        if (npcHotbar != null) npcHotbar.getInventory().clear();

        // only do loot and XP if a player attacked this enemy recently
        if (getAttackingPlayers(ref, store).isEmpty()) return;

        // get transform early — needed for drop position and distance calculation
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        HeadRotation headRotation = store.getComponent(ref, HeadRotation.getComponentType());
        assert transform != null && headRotation != null;

        // filter vanilla drops — remove weapons, armor, and raw resource items
        List<ItemStack> allDrops = new ObjectArrayList<>();
        for (ItemStack drop : itemModule.getRandomItemDrops(dropListId)) {
            Item item = drop.getItem();
            boolean isExcluded = item.getWeapon() != null || item.getArmor() != null || EXCLUDED_ID_SUBSTRINGS.stream().anyMatch(item.getId()::contains);
            if (!isExcluded) allDrops.add(drop);
        }

        // determine tier from horizontal distance to world origin
        Config_World worldConfig = ModConfig.get().world;
        Vector3d pos = transform.getPosition();
        double distance = Math.sqrt( Math.pow(pos.x - worldConfig.origin_spawn_point_x, 2) + Math.pow(pos.z - worldConfig.origin_spawn_point_z, 2));
        int tier = getTierByDistance(distance);
        String tierDropList = TIER_DROP_LISTS[tier];

        // resolve tier drops and replace any mod gear with factory-generated equivalents
        for (ItemStack drop : itemModule.getRandomItemDrops(tierDropList)) {
            Item item = drop.getItem();
            if (item == null) continue;
            String[] categories = item.getCategories();
            boolean isModGear = categories != null && Arrays.asList(categories).contains("Items.HyARPG.Gear");
            if (!isModGear) { allDrops.add(drop); continue; }
            ItemStack generated = ItemFactory.createItem(drop.getItemId(), rpgEnemy.level, null, null, null);
            if (generated != null) allDrops.add(generated);
        }

        // spawn all drops
        if (!allDrops.isEmpty()) {
            Vector3d dropPosition = pos.clone().add(0.0, 1.0, 0.0);
            Holder<EntityStore>[] dropEntities = ItemComponent.generateItemDrops(store, allDrops, dropPosition, headRotation.getRotation().clone());
            commandBuffer.addEntities(dropEntities, AddReason.SPAWN);
        }

        // do this last because it removes the enemy from the damage registry
        awardXPToPlayers(event);
    }

    // this function fires right before an entity is due to take damage
    private void onEntityPreDamage(Event_EntityPreDamaged event) {
        Ref<EntityStore> attacker = event.getAttacker();
        Ref<EntityStore> defender = event.getDefender();
        Store<EntityStore> store = event.getStore();
        Damage damage = event.getDamage();

        // get the damage cause
        int causeIndex = damage.getDamageCauseIndex();
        DamageCause cause = DamageCause.getAssetMap().getAsset(causeIndex);

        // handle fall damage first — no attacker/defender needed
        if (Objects.equals(cause.getId(), "Fall")) {
            Component_RPG_Player defenderRPGStats = store.getComponent(defender, componentTypeRPGPlayer);
            if (defenderRPGStats == null) return;
            float fallResist = defenderRPGStats.stats.getResistance("Fall");
            float multiplier = 1f - (Math.max(0f, Math.min(100f, fallResist)) / 100f);
            damage.setAmount(damage.getAmount() * multiplier);
            absorbWithBarrier(damage, store, defender);
            return;
        }

        // bail if no attacker or command damage
        if (attacker == null || Objects.equals(cause.getId(), "Command")) {
            absorbWithBarrier(damage, store, defender);
            return;
        }

        // bail if neither party is a mod entity — check both player and enemy components
        Component_RPG_Player attackerRPGStats = store.getComponent(attacker, componentTypeRPGPlayer);
        Component_RPG_Player defenderRPGStats = store.getComponent(defender, componentTypeRPGPlayer);
        Component_RPG_Enemy attackerRPGEnemy = store.getComponent(attacker, componentTypeRPGEnemy);
        Component_RPG_Enemy defenderRPGEnemy = store.getComponent(defender, componentTypeRPGEnemy);
        if (attackerRPGStats == null && defenderRPGStats == null && attackerRPGEnemy == null && defenderRPGEnemy == null) return;

        // prep swing group keys shared across all paths
        String key = swingKey(attacker, defender);
        boolean blocked = Boolean.TRUE.equals(damage.getMetaStore().getMetaObject(Damage.BLOCKED));
        boolean isProjectile = damage.getSource() instanceof Damage.ProjectileSource;

        // weapon damage path — MainHand, OffHand, or Weapon (ability) causes
        if (WEAPON_CAUSE_IDS.contains(cause.getId())) {
            String causeId = cause.getId();
            boolean isScalar = causeId.equals("MainHand_Scalar") || causeId.startsWith("MainHand_") && causeId.endsWith("_Scalar");

            // extract the forced type from typed scalars (e.g. "MainHand_Fire_Scalar" → "Fire"), null for untyped
            String forcedDamageType = null;
            if (isScalar && !causeId.equals("MainHand_Scalar")) {
                // strip "MainHand_" prefix and "_Scalar" suffix
                forcedDamageType = causeId.substring("MainHand_".length(), causeId.length() - "_Scalar".length());
            }

            // check for the appropriate hand item from the RPGPlayer comp
            ItemStack weaponStack = causeId.equals("OffHand") ? (attackerRPGStats != null ? attackerRPGStats.offHandItem : null) : (attackerRPGStats != null ? attackerRPGStats.mainHandItem : null);

            // Get the weapon type and the weapon damage implicits
            String weaponType = weaponStack != null ? ItemFactory.deriveItemType(weaponStack.getItem().getId()) : null;
            List<String> damageImplicits = weaponStack != null ? ItemFactory.getWeaponDamageImplicits(weaponStack) : Collections.emptyList();

            // Add the damage group if it does not yet exist
            String groupWeaponType = forcedDamageType != null ? null : weaponType;
            SwingDamageGroup group = swingGroups.computeIfAbsent(key, k -> {
                SwingDamageGroup g = new SwingDamageGroup(attacker, defender, blocked, isProjectile, groupWeaponType, false, 0);
                scheduler.schedule(() -> {
                    SwingDamageGroup pending = swingGroups.get(key);
                    if (pending != null) pending.readyToApply = true;
                }, FLUSH_DELAY_MS, TimeUnit.MILLISECONDS);
                return g;
            });

            // apply the damage implicits
            if (!damageImplicits.isEmpty()) {
                for (String implicit : damageImplicits) {
                    String[] parts = implicit.split("\\|");
                    if (parts.length < 3) continue;
                    StatType stat;
                    try { stat = StatType.valueOf(parts[0]); } catch (Exception e) { continue; }
                    float implicitValue = Float.parseFloat(parts[1]);

                    float finalValue = isScalar ? implicitValue * damage.getInitialAmount() : implicitValue;

                    String dmgTypeId = switch (stat) {
                        case MAIN_HAND_FIRE_DAMAGE_FLAT,      OFF_HAND_FIRE_DAMAGE_FLAT      -> "Fire";
                        case MAIN_HAND_LIGHTNING_DAMAGE_FLAT, OFF_HAND_LIGHTNING_DAMAGE_FLAT -> "Lightning";
                        case MAIN_HAND_ICE_DAMAGE_FLAT,       OFF_HAND_ICE_DAMAGE_FLAT       -> "Ice";
                        case MAIN_HAND_POISON_DAMAGE_FLAT,    OFF_HAND_POISON_DAMAGE_FLAT    -> "Poison";
                        case MAIN_HAND_MAGIC_DAMAGE_FLAT,     OFF_HAND_MAGIC_DAMAGE_FLAT     -> "Magic";
                        case MAIN_HAND_PHYSICAL_DAMAGE_FLAT,  OFF_HAND_PHYSICAL_DAMAGE_FLAT  -> "Physical";
                        default -> null;
                    };
                    if (dmgTypeId == null) continue;

                    // typed scalar overrides the implicit's natural damage type
                    String resolvedTypeId = forcedDamageType != null ? forcedDamageType : dmgTypeId;

                    DamageCause resolvedCause = DamageCause.getAssetMap().getAsset(resolvedTypeId);
                    if (resolvedCause == null) continue;
                    group.add(resolvedCause, finalValue);
                }
            }

            // no implicits — fall back to physical (or forced type) scaled by scalar or flat
            else {
                String fallbackTypeId = forcedDamageType != null ? forcedDamageType : "Physical";
                group.add(DamageCause.getAssetMap().getAsset(fallbackTypeId), damage.getInitialAmount());
            }
        }

        // Not a special replacement type damage, so check if it's a standard mod damage
        else {
            if (!MOD_DAMAGE_TYPES.contains(cause.getId())) cause = DamageCause.getAssetMap().getAsset("Physical");

            SwingDamageGroup group = swingGroups.computeIfAbsent(key, k -> {
                SwingDamageGroup g = new SwingDamageGroup(attacker, defender, blocked, isProjectile, null, false, 0);
                scheduler.schedule(() -> {
                    SwingDamageGroup pending = swingGroups.get(key);
                    if (pending != null) pending.readyToApply = true;
                }, FLUSH_DELAY_MS, TimeUnit.MILLISECONDS);
                return g;
            });

            // if attacker is a known enemy replace vanilla damage with their level-scaled base hit
            float baseDamage = damage.getInitialAmount();
            if (attackerRPGEnemy != null && attackerRPGEnemy.damageType != null) {
                baseDamage = attackerRPGEnemy.level * ModConfig.get().combat.enemy_base_damage * attackerRPGEnemy.damageMultiplier;
                DamageCause enemyCause = DamageCause.getAssetMap().getAsset(attackerRPGEnemy.damageType);
                if (enemyCause != null) cause = enemyCause;
            }

            group.add(cause, baseDamage);
        }

        damage.setCancelled(true);
    }

    // Consume grouped damage into a single hit and apply it to the target
    private void applyDamageGroup(SwingDamageGroup damageGroup) {
        if (damageGroup == null) return;

        // get event properties
        Ref<EntityStore> attacker = damageGroup.attacker;
        Ref<EntityStore> defender = damageGroup.defender;
        Store<EntityStore> store = attacker.getStore();

        // get attacker components
        Component_RPG_Player attackerRPGStats = store.getComponent(attacker, componentTypeRPGPlayer);
        Component_RPG_Enemy attackerRPGEnemy = store.getComponent(attacker, componentTypeRPGEnemy);

        // default attacker values
        int attackerLevel = 1;
        int attackerRarity = 0;
        String attackerName = "Entity";
        EntityStats attackerStats = new EntityStats();

        // determine attacker values
        if (attackerRPGStats != null) {
            attackerLevel = attackerRPGStats.gearScore;
            attackerStats = attackerRPGStats.stats;
            attackerName = store.getComponent(attacker, Player.getComponentType()).getDisplayName();
        }
        else if(attackerRPGEnemy != null) {
            attackerLevel = attackerRPGEnemy.level;
            attackerRarity = attackerRPGEnemy.monsterRarity;
            attackerStats = attackerRPGEnemy.stats;
            attackerName = store.getComponent(attacker, NPCEntity.getComponentType()).getRoleName().replace("_", " ");
        };

        // check if the defender is a player or NPC
        Component_RPG_Player defenderRPGStats = store.getComponent(defender, componentTypeRPGPlayer);
        Component_RPG_Enemy defenderRPGEnemy = store.getComponent(defender, componentTypeRPGEnemy);

        // default defender values
        int defenderLevel = 1;
        int defenderRarity = 0;
        boolean defenderUsingShield = false;
        EntityStatMap defenderStatMap = null;
        String defenderName = "Entity";
        EntityStats defenderStats = new EntityStats();

        // determine defender values
        if(defenderRPGStats != null) {
            defenderLevel = defenderRPGStats.gearScore;
            defenderStats = defenderRPGStats.stats;
            defenderName = store.getComponent(defender, Player.getComponentType()).getDisplayName();

            // defender is a player, set some things
            defenderStatMap = store.getComponent(defender, EntityStatsModule.get().getEntityStatMapComponentType());

            // player had a utility item equipped, check if it was a shield
            if(defenderRPGStats.offHandItem != null && defenderRPGStats.offHandItem.getItemId().contains("Weapon_Shield"))
                defenderUsingShield = true;
        }
        else if(defenderRPGEnemy != null) {
            defenderLevel = defenderRPGEnemy.level;
            defenderRarity = defenderRPGEnemy.monsterRarity;
            defenderStats = defenderRPGEnemy.stats;
            defenderName = store.getComponent(defender, NPCEntity.getComponentType()).getRoleName().replace("_", " ");
        };

        // if attacker is a player and defender is an enemy register the damage for XP awarding
        if(attackerRPGStats != null && defenderRPGEnemy != null)
            damageRegistry.computeIfAbsent(defender, k -> new ConcurrentHashMap<>()).put(attacker, System.currentTimeMillis());

        // apply added flat damage
        if (attackerStats.getFlatDamage("Fire") > 0)
            damageGroup.add(DamageCause.getAssetMap().getAsset("Fire"), attackerStats.getFlatDamage("Fire"));
        if (attackerStats.getFlatDamage("Ice") > 0)
            damageGroup.add(DamageCause.getAssetMap().getAsset("Ice"), attackerStats.getFlatDamage("Ice"));
        if (attackerStats.getFlatDamage("Lightning") > 0)
            damageGroup.add(DamageCause.getAssetMap().getAsset("Lightning"), attackerStats.getFlatDamage("Lightning"));
        if (attackerStats.getFlatDamage("Physical") > 0)
            damageGroup.add(DamageCause.getAssetMap().getAsset("Physical"), attackerStats.getFlatDamage("Physical"));
        if (attackerStats.getFlatDamage("Elemental") > 0)
            damageGroup.add(DamageCause.getAssetMap().getAsset("Elemental"), attackerStats.getFlatDamage("Elemental"));
        if (attackerStats.getFlatDamage("Magic") > 0)
            damageGroup.add(DamageCause.getAssetMap().getAsset("Magic"), attackerStats.getFlatDamage("Magic"));
        if (attackerStats.getFlatDamage("Poison") > 0)
            damageGroup.add(DamageCause.getAssetMap().getAsset("Poison"), attackerStats.getFlatDamage("Poison"));

        // roll for crit which applies to all packets
        float critChance = attackerStats.getCriticalStrikeChance();
        float critRoll = (float) (Math.random() * 100.0) ;
        boolean crit = damageGroup.forceCrit || critRoll < critChance;

        // check assassin mark count against this target before the damage loop — same target only
        int assassinMarks = (attackerRPGStats != null && defender.equals(attackerRPGStats.marks.getLastHitTarget())) ? attackerRPGStats.marks.count("ASSASSIN") : 0;

        // loop over the damage packets for each type and adjust them accordingly
        double finalDamage = 0;
        for (var entry : damageGroup.packets()) {
            // get the damage cause and total value
            DamageCause cause = entry.getKey();
            double totalAmount = entry.getValue();

            // adjust based on config settings for damage multipliers
            if(attackerRPGStats != null) totalAmount *= ModConfig.get().combat.damage_from_player_multiplier;
            if(defenderRPGStats != null) totalAmount *= ModConfig.get().combat.damage_to_player_multiplier;

            // adjust damage based on player/enemy level
            totalAmount = adjustDamageBasedOnLevel(attackerLevel, attackerRarity, defenderLevel, defenderRarity, totalAmount);

            // apply increased damages
            totalAmount += totalAmount * (attackerStats.getIncreasedDamage(cause.getId()) / 100);

            // apply increased weapon damage if applicable
            if (damageGroup.weaponType != null) totalAmount += totalAmount * (attackerStats.getIncreasedDamage(damageGroup.weaponType) / 100);

            // apply assassin mark damage if applicable
            if (assassinMarks > 0)
                totalAmount += totalAmount * (assassinMarks * .02);

            // adjust the damage based on crit
            if (crit) totalAmount *= attackerStats.getCriticalStrikeDamage() + (damageGroup.critDamageBonus / 100.0);

            // reduce the damage based on the defenders resistance stat
            totalAmount -= totalAmount * (defenderStats.getResistance(cause.getId()) / 100);

            // total this damage into the final amount
            finalDamage += totalAmount;

            // don't process the message if no damage was dealt
            if(totalAmount <= 0) continue;

            // ping damage packet to chat for combat analysis
            if(ModConfig.get().combat.broadcast_combat_logs_in_chat && (attackerRPGStats != null || defenderRPGStats != null)) {
                PlayerRef playerRef = null;

                // try to get a player ref
                if (attackerRPGStats != null && attackerRPGStats.showCombatText)
                    playerRef = store.getComponent(attacker, PlayerRef.getComponentType());
                else if (defenderRPGStats != null && defenderRPGStats.showCombatText)
                    playerRef = store.getComponent(defender, PlayerRef.getComponentType());

                // if we found a player ref and they have the setting enabled, broadcast the message
                if (playerRef != null) {
                    float truncated = (float) (Math.floor(totalAmount * 10.0) / 10.0);
                    playerRef.sendMessage(Message.join(
                            Message.raw(attackerName).color("#ffffff").bold(true),
                            Message.raw(" dealt ").color(Color.GRAY),
                            Message.raw(truncated + " (" + cause.getId() + ")").color(DAMAGE_COLORS.getOrDefault(cause.getId(), Color.WHITE)).bold(true),
                            Message.raw(" damage to ").color(Color.GRAY),
                            Message.raw(defenderName).color("#ffffff").bold(true))
                    );
                }
            }
        }

        // check if the conditions were met to try to parry
        if (!damageGroup.isProjectile && damageGroup.blocked && defenderRPGStats != null) {
            try {
                // check if was within timing window
                long blockStart = defenderRPGStats.blockStart;
                double totalSeconds = ModConfig.get().combat.base_parry_window_in_seconds + defenderStats.getParryWindow();
                long parryWindowModified = (long)(totalSeconds * 1_000_000_000L);

                if(System.nanoTime() - blockStart <= parryWindowModified) {
                    // set the damage threshold for parrying based on stability
                    double damageThreshold = finalDamage * (1f - (defenderStats.getStabilityPercent(defenderUsingShield) / 100));

                    // get the stat map component from the player
                    ComponentType<EntityStore, EntityStatMap> statMapType = EntityStatsModule.get().getEntityStatMapComponentType();
                    EntityStatMap statMap = store.getComponent(defender, statMapType);

                    // Get the stamina stat from the stat map
                    int staminaIndex = DefaultEntityStatTypes.getStamina();

                    // get the stats by index
                    EntityStatValue staminaStat = statMap.get(staminaIndex);

                    // apply the stun and knockback to NPCs
                    if (damageThreshold <= staminaStat.get() && attackerRPGEnemy != null) {
                        // get the effect controller for the attacker to set stun effect
                        EffectControllerComponent effectController = store.getComponent(attacker, EffectControllerComponent.getComponentType());
                        EntityEffect effect = EntityEffect.getAssetMap().getAsset("ParryStun");
                        effectController.addEffect(attacker, effect, 2, OverlapBehavior.OVERWRITE, store);

                        // reduce the damage to 0
                        finalDamage = 0;

                        // freeze the entity and set a scheduler to remove the freeze
                        World world = attacker.getStore().getExternalData().getWorld();
                        world.execute(() -> {
                            // stun via frozen component
                            store.addComponent(attacker, Frozen.getComponentType(), Frozen.get());

                            Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                                world.execute(() -> {
                                    if (attacker.isValid()) {
                                        store.removeComponent(attacker, Frozen.getComponentType());
                                    }
                                });
                            }, (long)(3 * 1000), TimeUnit.MILLISECONDS);
                        });
                    }
                }
            } catch (Exception e) {}
        }

        // now handle blocking
        if(damageGroup.blocked) {
            // set the damage threshold for blocking based on stability
            double damageThreshold = finalDamage * (1f - (defenderStats.getStabilityPercent(defenderUsingShield) / 100));

            // get the stat map component from the player
            ComponentType<EntityStore, EntityStatMap> statMapType = EntityStatsModule.get().getEntityStatMapComponentType();
            EntityStatMap statMap = store.getComponent(defender, statMapType);

            // Get the stamina stat from the stat map
            int staminaIndex = DefaultEntityStatTypes.getStamina();

            // get the defenders current stamina
            EntityStatValue staminaStat = statMap.get(staminaIndex);
            float staminaValue = staminaStat.get();

            // subtract the stamina from the damage
            double remainingDmg = damageThreshold - staminaValue;
            double remainingStamina = staminaValue - damageThreshold;

            // set stamina to the new value ceiled to 0
            statMap.setStatValue(staminaIndex, Math.max(0, (float) remainingStamina));

            // set remaining amount of damage ceiled to 0
            finalDamage = Math.max(0, (float) remainingDmg);
        }

        // now handle barrier which absorbs damage after parry/block, before dodge
        if(defenderStatMap != null && finalDamage > 0) {
            int barrierOnBlockStatIndex = EntityStatType.getAssetMap().getIndex("BarrierOnBlock");
            EntityStatValue barrierOnBlockStat = defenderStatMap.get(barrierOnBlockStatIndex);

            if (barrierOnBlockStat != null) {
                double barrier = barrierOnBlockStat.get();

                if (barrier > 0) {
                    double absorbed = Math.min(barrier, finalDamage);

                    finalDamage -= absorbed;
                    barrier -= absorbed;

                    defenderStatMap.setStatValue(barrierOnBlockStatIndex, (float) barrier);
                }
            }
        }

        // now handle dodging
        if(finalDamage > 0f && defenderStats.getDodgeChance() > 0f) {
            // get teh players dodge chance and roll a random number between 0 and 100
            float dodgeChance = defenderStats.getDodgeChance();
            float dodgeRoll = (float) (Math.random() * 100.0) ;

            // if the roll is under the dodge chance the player dodged so negate the damage
            if (dodgeRoll < dodgeChance) {
                // successful dodge
                finalDamage = 0f;
            }
        }

        // apply damage taken from mana/stamina — redirects a portion of damage away from health
        if (defenderRPGStats != null && defenderStatMap != null && finalDamage > 0) {
            ComponentType<EntityStore, EntityStatMap> statMapType = EntityStatsModule.get().getEntityStatMapComponentType();

            float damageTakenFromMana = defenderStats.getDamageTakenFrom("Mana");
            if (damageTakenFromMana > 0) {
                double redirect = finalDamage * (damageTakenFromMana / 100f);
                int manaIndex = DefaultEntityStatTypes.getMana();
                EntityStatValue manaStat = defenderStatMap.get(manaIndex);
                if (manaStat != null && manaStat.get() > 0) {
                    double absorbed = Math.min(manaStat.get(), redirect);
                    defenderStatMap.setStatValue(manaIndex, Math.max(0, manaStat.get() - (float) absorbed));
                    finalDamage -= absorbed;
                }
            }

            float damageTakenFromStamina = defenderStats.getDamageTakenFrom("Stamina");
            if (damageTakenFromStamina > 0) {
                double redirect = finalDamage * (damageTakenFromStamina / 100f);
                int staminaIndex = DefaultEntityStatTypes.getStamina();
                EntityStatValue staminaStat = defenderStatMap.get(staminaIndex);
                if (staminaStat != null && staminaStat.get() > 0) {
                    double absorbed = Math.min(staminaStat.get(), redirect);
                    defenderStatMap.setStatValue(staminaIndex, Math.max(0, staminaStat.get() - (float) absorbed));
                    finalDamage -= absorbed;
                }
            }
        }

        // if final damage at this point is 0 or less nothing else needs to happen
        if(finalDamage <= 0) return;

        // apply leech — attacker recovers a portion of damage dealt as a resource
        if (attackerRPGStats != null) {
            EntityStatMap attackerStatMap = store.getComponent(attacker, EntityStatsModule.get().getEntityStatMapComponentType());
            if (attackerStatMap != null) {
                float lifeLeech = attackerStats.getLeech("Life");
                if (lifeLeech > 0) {
                    double leechAmount = finalDamage * (lifeLeech / 100f);
                    int healthIndex = DefaultEntityStatTypes.getHealth();
                    EntityStatValue healthStat = attackerStatMap.get(healthIndex);
                    if (healthStat != null) attackerStatMap.setStatValue(healthIndex, Math.min(healthStat.getMax(), healthStat.get() + (float) leechAmount));
                }

                float manaLeech = attackerStats.getLeech("Mana");
                if (manaLeech > 0) {
                    double leechAmount = finalDamage * (manaLeech / 100f);
                    int manaIndex = DefaultEntityStatTypes.getMana();
                    EntityStatValue manaStat = attackerStatMap.get(manaIndex);
                    if (manaStat != null)
                        attackerStatMap.setStatValue(manaIndex, Math.min(manaStat.getMax(), manaStat.get() + (float) leechAmount));
                }

                float staminaLeech = attackerStats.getLeech("Stamina");
                if (staminaLeech > 0) {
                    double leechAmount = finalDamage * (staminaLeech / 100f);
                    int staminaIndex = DefaultEntityStatTypes.getStamina();
                    EntityStatValue staminaStat = attackerStatMap.get(staminaIndex);
                    if (staminaStat != null)
                        attackerStatMap.setStatValue(staminaIndex, Math.min(staminaStat.getMax(), staminaStat.get() + (float) leechAmount));
                }
            }
        }

        // check for signature energy awarding
        Ref<EntityStore> refPlayer = attackerRPGStats != null ? attacker : defenderRPGStats != null ? defender : null;
        if (refPlayer != null) {
            // get the players stat map
            EntityStatMap playerStatMap = store.getComponent(refPlayer, EntityStatMap.getComponentType());

            // get the players signature energy
            int sigEnergyIndex = EntityStatType.getAssetMap().getIndex("SignatureEnergy");
            EntityStatValue sigEnergy = playerStatMap != null ? playerStatMap.get(sigEnergyIndex) : null;
            if (sigEnergy != null) playerStatMap.setStatValue(sigEnergyIndex, sigEnergy.get() + 1f);
        }

        // capture last target hit and apply marks to the target now that damage has been calculated
        if (attackerRPGStats != null) {
            // update last target hit
            attackerRPGStats.lastEnemyHit = defender;

            // apply marks
            int toApply = attackerStats.getFlatApplyMarks("Assassin");
            if (toApply > 0) attackerRPGStats.marks.onHit(defender, Map.of("ASSASSIN", toApply));
        }

        // apply the final damage to the player
        DamageSystems.executeDamage(defender, store,
                new Damage(
                        new Damage.EntitySource(attacker),
                        DamageCause.getAssetMap().getAsset("Command"),
                        (float) finalDamage
                )
        );
    }

    // check for deaths and award XP on repeat
    private void awardXPToPlayers(Event_NPCDeath event) {
        Ref<EntityStore> defender = event.getRef();
        Store<EntityStore> store = event.getStore();
        long cutoff = System.currentTimeMillis() - 30_000;

        // Pull and remove the attacker map for this specific enemy
        ConcurrentHashMap<Ref<EntityStore>, Long> attackers = damageRegistry.remove(defender);
        if (attackers == null) return;

        // get the world and then on it's next tick continue functionality
        World world = store.getExternalData().getWorld();
        world.execute(() -> {
            attackers.forEach((attacker, timestamp) -> {
                // Skip stale or invalid entries
                if (timestamp < cutoff || !attacker.isValid()) return;

                // get the playerRef that dealt damage and check they are still valid
                PlayerRef playerRef = store.getComponent(attacker, PlayerRef.getComponentType());
                if (playerRef == null) return;

                // get the killed enemies level or default to 1
                Component_RPG_Enemy rpgEnemy = store.getComponent(defender, componentTypeRPGEnemy);
                int enemyLevel = (rpgEnemy != null) ? rpgEnemy.level : 1;
                int enemyRarity = (rpgEnemy != null) ? rpgEnemy.monsterRarity : 0;

                // award XP to the player
                Component_RPG_Player attackerRPGStats = store.getComponent(attacker, componentTypeRPGPlayer);
                attackerRPGStats.awardXP(enemyLevel, enemyRarity, playerRef);
            });
        });
    }

    // adjust damage packets based on the enemies level
    private double adjustDamageBasedOnLevel(int attackerLevel, int attackerRarity, int defenderLevel, int defenderRarity, double damage) {
        // Tunable constants (safe for infinite scaling)
        final double LEVEL_MULTIPLIER = ModConfig.get().combat.level_diff_damage_multiplier;
        final double RARITY_MULTIPLIER = ModConfig.get().combat.rarity_diff_damage_multiplier;

        // determine delta based on level/rarity difference
        int levelDelta = attackerLevel - defenderLevel;
        int rarityDelta = attackerRarity - defenderRarity;

        // use the deltas to determine a level/rarity scale
        double levelScale = Math.pow(LEVEL_MULTIPLIER, levelDelta);
        double rarityScale = Math.pow(RARITY_MULTIPLIER, rarityDelta);

        // apply the damage scales to the damage amount
        damage *= levelScale;
        damage *= rarityScale;

        // Clamp result to prevent degenerate damage
        damage = Math.max(0, damage);

        // return the scaled damage number
        return damage;
    }

    // get the world tier at the current location
    private static int getTierByDistance(double distance) {
        Config_World world = ModConfig.get().world;
        if (distance >= world.min_distance_for_mithril_spawn)     return 6;
        if (distance >= world.min_distance_for_adamantite_spawn)  return 5;
        if (distance >= world.min_distance_for_cobalt_spawn)      return 4;
        if (distance >= world.min_distance_for_thorium_spawn)     return 3;
        if (distance >= world.min_distance_for_iron_spawn)        return 2;
        if (distance >= world.min_distance_for_copper_spawn)        return 1;
        return 0;
    }

    // get valid attackers from the damage registry
    private List<Ref<EntityStore>> getAttackingPlayers(Ref<EntityStore> defender, Store<EntityStore> store) {
        ConcurrentHashMap<Ref<EntityStore>, Long> attackers = damageRegistry.get(defender);
        long cutoff = System.currentTimeMillis() - 30_000;
        List<Ref<EntityStore>> attackingRefs = new ArrayList<Ref<EntityStore>>();
        if(attackers == null) return Collections.emptyList();

        // loop over attackers
        attackers.forEach((attacker, timestamp) -> {
            // Skip stale or invalid entries
            if (timestamp < cutoff || !attacker.isValid()) return;

            // get the playerRef that dealt damage and check they are still valid
            PlayerRef playerRef = store.getComponent(attacker, PlayerRef.getComponentType());
            if (playerRef == null) return;

            // add ref to attacking refs
            attackingRefs.add(attacker);
        });

        // return the attacking refs
        return attackingRefs;
    }

    // inject a pre-built damage group into the swing queue (used by abilities)
    public void injectDamageGroup(Ref<EntityStore> attacker, Ref<EntityStore> defender, SwingDamageGroup group) {
        String key = swingKey(attacker, defender);
        swingGroups.computeIfAbsent(key, k -> {
            scheduler.schedule(() -> {
                SwingDamageGroup pending = swingGroups.get(key);
                if (pending != null) pending.readyToApply = true;
            }, FLUSH_DELAY_MS, TimeUnit.MILLISECONDS);
            return group;
        });
    }

    // calculate damage absorption due to barrier
    private void absorbWithBarrier(Damage damage, Store<EntityStore> store, Ref<EntityStore> defender) {
        EntityStatMap defenderStatMap = store.getComponent(defender, EntityStatsModule.get().getEntityStatMapComponentType());
        if (defenderStatMap == null) return;

        int barrierIndex = EntityStatType.getAssetMap().getIndex("BarrierOnBlock");
        EntityStatValue barrierStat = defenderStatMap.get(barrierIndex);
        if (barrierStat == null || barrierStat.get() <= 0) return;

        float barrier = barrierStat.get();
        float incoming = damage.getAmount();
        float absorbed = Math.min(barrier, incoming);

        defenderStatMap.setStatValue(barrierIndex, barrier - absorbed);
        damage.setAmount(incoming - absorbed);
    }

}
