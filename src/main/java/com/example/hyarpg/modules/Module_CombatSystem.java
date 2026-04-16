package com.example.hyarpg.modules;

// Hytale Imports
import com.example.hyarpg.utils.affixes.StatType;
import com.example.hyarpg.utils.items.ItemFactory;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.Frozen;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
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
import com.example.hyarpg.components.Component_CraftingKnowledge;
import com.example.hyarpg.utils.combat.SwingDamageGroup;
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.components.Component_RPG_Enemy;
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.configs.ModConfig;
import com.example.hyarpg.events.*;
import com.example.hyarpg.utils.affixes.EntityStats;
import static com.example.hyarpg.modules.Module_RPGSystem.*;

// Java Imports
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.annotation.Nullable;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;

public class Module_CombatSystem {

    // Create a map for damage message colors
    public static final Map<String, Color> DAMAGE_COLORS = Map.of(
        "Fire", new Color(249, 146, 32),
        "Ice", new Color(219, 241, 253),
        "Lightning", new Color(0, 92, 165),
        "Poison",  new Color(0, 255, 0),
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

    // Used for weapon damage swap
    private static class ResolvedDamage {
        final DamageCause cause;
        final float amount;
        final String[] weaponTypes;

        ResolvedDamage(DamageCause cause, float amount, @Nullable String... weaponTypes) {
            this.cause = cause;
            this.amount = amount;
            this.weaponTypes = weaponTypes;
        }
    }

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

        // get the NPC component
        NPCEntity npcComponent = commandBuffer.getComponent(ref, NPCEntity.getComponentType());
        if (npcComponent == null) return;

        // get the role component
        Role role = npcComponent.getRole();
        if (role == null) return;

        // get the drop list from the role!?
        String dropListId = role.getDropListId();
        if (dropListId == null) return;

        // get the item module from teh drop list!?
        ItemModule itemModule = ItemModule.get();
        if (!itemModule.isEnabled()) return;

        // get random items to be dropped
        var drops = itemModule.getRandomItemDrops(dropListId);

        // filter out vanilla weapons and armor and ingots
        List<ItemStack> filteredDrops = new ObjectArrayList();
        for (ItemStack drop : drops) {
            Item item = drop.getItem();
            if (item.getWeapon() != null || item.getArmor() != null || item.getId().contains("Ingredient_Bar") || item.getId().contains("Ore_") || item.getId().contains("Weapon_") || item.getId().contains("Armor_")) {
                continue;
            };
            filteredDrops.add(drop);
        }

        // get a list of players that attacked the enemy in the last 30 seconds
        List<Ref<EntityStore>> players = getAttackingPlayers(ref, store);

        // add drops to the pool
        rollLoot(ref, store, players, filteredDrops);

        // spawn filtered drops
        if (!filteredDrops.isEmpty()) {
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            HeadRotation headRotation = store.getComponent(ref, HeadRotation.getComponentType());
            assert transform != null && headRotation != null;

            Vector3d dropPosition = transform.getPosition().clone().add(0.0, 1.0, 0.0);
            Holder<EntityStore>[] dropEntities = ItemComponent.generateItemDrops(store, filteredDrops, dropPosition, headRotation.getRotation().clone());
            commandBuffer.addEntities(dropEntities, AddReason.SPAWN);
        }

        // do this last because it removes the enemy from the damage registry
        awardXPToPlayers(event);
    }

    // This function fires right before an entity takes damage
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
            return;
        }

        // bail if no attacker or command damage
        if (attacker == null || Objects.equals(cause.getId(), "Command")) return;

        // bail if neither party is a mod entity
        Component_RPG_Player attackerRPGStats = store.getComponent(attacker, componentTypeRPGPlayer);
        Component_RPG_Player defenderRPGStats = store.getComponent(defender, componentTypeRPGPlayer);
        if (attackerRPGStats == null && defenderRPGStats == null) return;

        // prep swing group keys shared across all paths
        String key = swingKey(attacker, defender);
        boolean blocked = Boolean.TRUE.equals(damage.getMetaStore().getMetaObject(Damage.BLOCKED));
        boolean isProjectile = damage.getSource() instanceof Damage.ProjectileSource;

        // weapon damage path — MainHand, OffHand, or Weapon (ability) causes
        if (Set.of("MainHand", "OffHand", "Weapon").contains(cause.getId())) {
            // get the cause id and appropriate item stack for evaluation
            String causeId = cause.getId();
            ItemStack weaponStack = causeId.equals("OffHand")
                    ? (attackerRPGStats != null ? attackerRPGStats.offHandItem : null)
                    : (attackerRPGStats != null ? attackerRPGStats.mainHandItem : null);

            // derive the weapon sub-type e.g. "Axe", "Sword" from the item id for weapon bonus lookup
            String weaponType = weaponStack != null ? ItemFactory.deriveWeaponType(weaponStack.getItem().getId()) : null;

            // read weapon damage implicits from the item
            List<String> damageImplicits = weaponStack != null ? ItemFactory.getWeaponDamageImplicits(weaponStack) : Collections.emptyList();

            // get or create the swing group for this attacker/defender pair
            SwingDamageGroup group = swingGroups.computeIfAbsent(key, k -> {
                SwingDamageGroup g = new SwingDamageGroup(attacker, defender, blocked, isProjectile, weaponType);
                scheduler.schedule(() -> {
                    SwingDamageGroup pending = swingGroups.get(key);
                    if (pending != null) pending.readyToApply = true;
                }, FLUSH_DELAY_MS, TimeUnit.MILLISECONDS);
                return g;
            });

            // if implicits were found add one packet per damage type scaled by the initial amount
            if (!damageImplicits.isEmpty()) {
                for (String implicit : damageImplicits) {
                    // parse the implicit string format: "STAT_TYPE|value|display"
                    String[] parts = implicit.split("\\|");
                    if (parts.length < 3) continue;

                    // resolve the stat type from the encoded string
                    StatType stat;
                    try { stat = StatType.valueOf(parts[0]); } catch (Exception e) { continue; }

                    // parse the implicit value and map the stat type to a damage cause id
                    float implicitValue = Float.parseFloat(parts[1]);
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

                    // look up the damage cause asset and add the scaled packet to the group
                    DamageCause resolvedCause = DamageCause.getAssetMap().getAsset(dmgTypeId);
                    if (resolvedCause == null) continue;
                    group.add(resolvedCause, implicitValue * damage.getInitialAmount());
                }
            }
            else {
                // no implicits found on the weapon, fall back to physical
                group.add(DamageCause.getAssetMap().getAsset("Physical"), damage.getInitialAmount());
            }
        }
        else {
            // remap unrecognized damage types to physical
            if (!MOD_DAMAGE_TYPES.contains(cause.getId()))
                cause = DamageCause.getAssetMap().getAsset("Physical");

            // get or create the swing group for this attacker/defender pair
            SwingDamageGroup group = swingGroups.computeIfAbsent(key, k -> {
                SwingDamageGroup g = new SwingDamageGroup(attacker, defender, blocked, isProjectile, null);
                scheduler.schedule(() -> {
                    SwingDamageGroup pending = swingGroups.get(key);
                    if (pending != null) pending.readyToApply = true;
                }, FLUSH_DELAY_MS, TimeUnit.MILLISECONDS);
                return g;
            });

            // add the damage packet
            group.add(cause, damage.getInitialAmount());
        }

        // cancel the original damage event, will be handled by the mod pipeline
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
        boolean crit = critRoll < critChance;

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

            // adjust the damage based on crit
            if (crit) totalAmount *= attackerStats.getCriticalStrikeDamage();

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

        // damage will be dealt so check for signature energy awarding
        Ref<EntityStore> refPlayer = attackerRPGStats != null ? attacker : defenderRPGStats != null ? defender : null;
        if (refPlayer != null) {
            // get the players stat map
            EntityStatMap playerStatMap = store.getComponent(refPlayer, EntityStatMap.getComponentType());

            // get the players signature energy
            int sigEnergyIndex = EntityStatType.getAssetMap().getIndex("SignatureEnergy");
            EntityStatValue sigEnergy = playerStatMap != null ? playerStatMap.get(sigEnergyIndex) : null;
            if (sigEnergy != null) playerStatMap.setStatValue(sigEnergyIndex, sigEnergy.get() + 1f);
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

    // function for spawning loot drops
    private void rollLoot(Ref<EntityStore> defender, Store<EntityStore> store, List<Ref<EntityStore>> players, List<ItemStack> dropPool) {
        // Get the killed enemies rpg component
        Component_RPG_Enemy rpgEnemy = store.getComponent(defender, componentTypeRPGEnemy);
        if(rpgEnemy == null) return;

        // get the enemies level and rarity
        String rarity = rpgEnemy.getRarityString();
        int level = rpgEnemy.level;

        // roll to see if gear or recipes should drop
        boolean shouldLootDrop = shouldLootDrop(rarity, ModConfig.get().loot.loot_drop_chance_modifier);
        boolean shouldRecipeDrop = shouldLootDrop(rarity, ModConfig.get().loot.recipe_drop_chance_modifier);

        // loop over all players who damaged the defender in the last 30 seconds
        if (ModConfig.get().loot.broadcast_drops_in_global_chat) {

        }
        for (Ref<EntityStore> ref : players) {
            // resolve the player component
            Player player = store.getComponent(ref, Player.getComponentType());

            // get the player's applicable components or bail
            Component_CraftingKnowledge craftingKnowledge = store.getComponent(ref, componentTypeCraftingKnowledge);
            Component_RPG_Player rpgPlayer = store.getComponent(ref, componentTypeRPGPlayer);
            if (craftingKnowledge == null || rpgPlayer == null) continue;

            // add a recipe if applicable
            if(shouldRecipeDrop && ModConfig.get().loot.broadcast_drops_in_global_chat && rpgPlayer.showLootDrops) {
                // roll recipe rarity and add it to the drop pool
                String recipeRarity = rollRarity();
                String recipeID = "Recipe_Page_" + recipeRarity;
                dropPool.add(new ItemStack(recipeID, 1));

                // get item color based on rarity
                Color color = Module_RPGSystem.colorUtils.getRarityColor(recipeRarity);

                // notify players of the loot roll
                alertPlayers(new Message[]{
                        Message.raw(player.getDisplayName()).color("#ffffff").bold(true),
                        Message.raw(" rolled a ").color(Color.GRAY),
                        Message.raw(recipeID.replace("_", " ")).color(color).bold(true)
                });
            }

            // bail now if loot should not drop
            if(!shouldLootDrop) continue;

            // bail if the player's recipes are empty
            Set<String> recipes = craftingKnowledge.discoveredDroppableRecipes;
            int recipeCount = recipes.size();
            if (recipeCount == 0) continue;

            // get the rarity the random item should be
            String rolledRarity = rollRarity();

            // get a random item based on the rarity rolled (falling down rarity order otherwise)
            String randomItemId = rollItemForRarity(recipes, rolledRarity);
            if (randomItemId == null) continue;

            // assign gear score based on enemy level and then add the item stack to the drop pool
            ItemStack newStack = new ItemStack(randomItemId, 1);
//            newStack = assignGearScoreAndAffixes(newStack, level);
            dropPool.add(newStack);

            // resolve the player component for messaging and alert players of the roll
            if (player != null && ModConfig.get().loot.broadcast_drops_in_global_chat && rpgPlayer.showLootDrops) {
                // get rarity of the item actually returned
                String[] parts = randomItemId.split("_");
                String actualRarity = parts[parts.length - 1];

                // format item name for display
                String itemName = randomItemId
                        .replace("Weapon_", "")
                        .replace("Armor_", "")
                        .replace("_", " ");

                // get the item rarity color
                Color color = Module_RPGSystem.colorUtils.getRarityColor(actualRarity);

                // notify players of the loot roll
                alertPlayers(new Message[]{
                        Message.raw(player.getDisplayName()).color("#ffffff").bold(true),
                        Message.raw(" rolled a ").color(Color.GRAY),
                        Message.raw(itemName).color(color).bold(true)
                });
            }
        }
    }

    // Returns true if the modifier application succeeds
    private boolean shouldLootDrop(String rarity, float modifier) {
        // Defensive default (treat unknown rarity as worst case)
        if (rarity == null) return false;

        // make a random roll between 0 and 1
        double roll = ThreadLocalRandom.current().nextDouble(); // 0.0 <= roll < 1.0
        double failChance;

        switch (rarity) {
            case "Common":
                failChance = 0.95; // 95% fail → 5% success
                break;

            case "Uncommon":
                failChance = 0.85; // 85% fail → 15% success
                break;

            case "Rare":
                failChance = 0.65; // 65% fail → 35% success
                break;

            case "Epic":
                failChance = 0.40; // 40% fail → 60% success
                break;

            case "Legendary":
                failChance = 0.10; // 10% fail → 90% success
                break;

            default:
                failChance = 0.95; // Safe fallback
                break;
        }

        // get the success chance and apply the passed modifier
        double successChance = 1.0 - failChance;
        successChance *= modifier;

        // Clamp to [0, 1]
        successChance = Math.max(0.0, Math.min(1.0, successChance));


        // Success occurs when roll exceeds failure probability
        return roll < successChance;
    }

    // Rolls a rarity tier based on fixed probabilities
    private String rollRarity() {
        double roll = ThreadLocalRandom.current().nextDouble(); // 0.0 <= roll < 1.0

        // Cumulative probability bands (must sum to 1.0)
        if (roll < 0.03) return "Legendary";   // 3%
        else if (roll < 0.10) return "Epic";   // 7% (0.10 total)
        else if (roll < 0.25) return "Rare";   // 15% (0.25 total)
        else return "Uncommon";                // 75% (0.70 total)
    }

    // Rolls a random item based on the passed rarity/recipes
    private String rollItemForRarity(Set<String> recipes, String rolledRarity) {
        // use ThreadLocalRandom for efficient RNG in game loops
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // Define fallback order (highest → lowest)
        String[] rarityOrder = { "Legendary", "Epic", "Rare", "Uncommon", "Common" };

        // Find starting rarity order index based on rolled rarity
        int startIndex = -1;
        for (int i = 0; i < rarityOrder.length; i++) {
            if (rarityOrder[i].equals(rolledRarity)) {
                startIndex = i;
                break;
            }
        }
        if (startIndex == -1) return null;

        // loop over rarity order and try to get a random item or fall down the chain
        for (int r = startIndex; r < rarityOrder.length; r++) {
            String rarity = rarityOrder[r];

            // Collect matching recipes
            List<String> matches = new ArrayList<>();
            for (String id : recipes) {
                if (id.endsWith("_" + rarity)) {
                    matches.add(id);
                }
            }

            // If matches exist → roll one
            if (!matches.isEmpty()) {
                return matches.get(rng.nextInt(matches.size()));
            }
        }

        // Should never occur if Common recipes exist
        return null;
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

    // helper function to show chat messages to all players
    public void alertPlayers(Message[] messages) {
        // loop over all players and broadcast the message
        for (PlayerRef player : Universe.get().getPlayers()) {
            player.sendMessage(Message.join(messages));
        }
    }
}
