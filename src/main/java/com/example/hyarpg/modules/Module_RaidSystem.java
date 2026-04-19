package com.example.hyarpg.modules;

// Hytale Imports
import com.example.hyarpg.utils.rooms.WorldRoomRegistry;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

// Mod Imports
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.configs.ModConfig;
import com.example.hyarpg.utils.rooms.RoomFloodFill;
import com.example.hyarpg.utils.rooms.TerritoryData;

// Java Imports
import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class Module_RaidSystem {

    // Small class for defining raids
    private static class RaidDefinition {
        final String name;
        final int weight;
        final List<List<String>> waves;
        final String prePhrase;
        final String phrase;
        final String postPhrase;

        RaidDefinition(String name, int weight, List<List<String>> waves, String prePhrase, String phrase, String postPhrase) {
            this.name = name;
            this.weight = weight;
            this.waves = waves;
            this.prePhrase = prePhrase;
            this.phrase = phrase;
            this.postPhrase = postPhrase;
        }
    }

    // Registry of defined raids
    private static final List<RaidDefinition> RAID_REGISTRY = List.of(

            new RaidDefinition("the_risen_tide", 100, List.of(
                    List.of("Chicken_Undead", "Chicken_Undead", "Pig_Undead", "Pig_Undead", "Zombie"),
                    List.of("Cow_Undead", "Cow_Undead", "Zombie", "Zombie", "Ghoul"),
                    List.of("Ghoul", "Ghoul", "Zombie_Aberrant", "Wraith")
            ),
                    "Something stirs beneath the soil of your farm...",
                    "The dead livestock have risen — and they remember who fed them last!",
                    "The undead herd is put down. Your base smells worse than before, somehow."
            ),

            new RaidDefinition("cold_front", 95, List.of(
                    List.of("Skeleton_Frost_Scout", "Skeleton_Frost_Scout", "Skeleton_Frost_Ranger", "Zombie_Frost"),
                    List.of("Skeleton_Frost_Soldier", "Skeleton_Frost_Fighter", "Skeleton_Frost_Mage", "Spirit_Frost"),
                    List.of("Skeleton_Frost_Knight", "Skeleton_Frost_Archmage", "Golem_Crystal_Frost", "Zombie_Frost", "Spirit_Frost")
            ),
                    "An unnatural chill settles over your territory...",
                    "A frost legion descends — the temperature drops and so will you!",
                    "The cold front passes. You survived the freeze, but your toes may not recover."
            ),

            new RaidDefinition("the_goblin_economy", 110, List.of(
                    List.of("Goblin_Miner", "Goblin_Miner", "Goblin_Scavenger", "Goblin_Hermit"),
                    List.of("Goblin_Lobber", "Goblin_Lobber", "Goblin_Thief", "Goblin_Ogre"),
                    List.of("Goblin_Duke", "Goblin_Ogre", "Goblin_Lobber", "Goblin_Scavenger", "Goblin_Thief")
            ),
                    "You hear the distant jingle of stolen coins...",
                    "The goblins have assessed your property value and decided to liquidate your assets!",
                    "The goblin delegation has been... dismissed. Your stuff is mostly still here."
            ),

            new RaidDefinition("desert_awakening", 90, List.of(
                    List.of("Skeleton_Sand_Scout", "Skeleton_Sand_Ranger", "Zombie_Sand", "Zombie_Sand"),
                    List.of("Skeleton_Sand_Guard", "Skeleton_Sand_Mage", "Skeleton_Sand_Archmage", "Golem_Crystal_Sand"),
                    List.of("Golem_Crystal_Sand", "Skeleton_Sand_Archmage", "Zombie_Sand", "Skeleton_Sand_Guard", "Skeleton_Sand_Ranger")
            ),
                    "The wind carries sand from a distant, forgotten place...",
                    "Ancient guardians of the sands have found your territory unworthy of existence!",
                    "The sands recede. The ancients return to their slumber, unimpressed but defeated."
            ),

            new RaidDefinition("spirits_of_the_wild", 75, List.of(
                    List.of("Spirit_Root", "Spirit_Root", "Spirit_Ember"),
                    List.of("Spirit_Frost", "Spirit_Thunder", "Spirit_Root", "Spirit_Ember"),
                    List.of("Spirit_Thunder", "Spirit_Thunder", "Spirit_Frost", "Spirit_Ember", "Hedera")
            ),
                    "The elements grow restless around your territory...",
                    "Nature itself has taken offense to your construction — the spirits converge!",
                    "The wild spirits scatter. Nature is displeased, but apparently willing to let it go."
            ),

            new RaidDefinition("the_outlander_vanguard", 85, List.of(
                    List.of("Outlander_Peon", "Outlander_Stalker", "Outlander_Hunter", "Outlander_Hunter"),
                    List.of("Outlander_Marauder", "Outlander_Berserker", "Outlander_Brute", "Outlander_Sorcerer"),
                    List.of("Outlander_Priest", "Outlander_Cultist", "Outlander_Brute", "Outlander_Sorcerer", "Outlander_Berserker")
            ),
                    "Shadows move at the edge of your vision...",
                    "The Outlander Vanguard emerges from the dark — your territory is their next conquest!",
                    "The vanguard retreats into the shadows. They'll report back. You probably don't want to know what they say."
            ),

            new RaidDefinition("fire_and_ash", 90, List.of(
                    List.of("Skeleton_Burnt_Archer", "Skeleton_Burnt_Gunner", "Zombie_Burnt", "Zombie_Burnt"),
                    List.of("Skeleton_Burnt_Knight", "Skeleton_Burnt_Lancer", "Skeleton_Burnt_Alchemist", "Spirit_Ember"),
                    List.of("Skeleton_Burnt_Praetorian", "Skeleton_Burnt_Wizard", "Golem_Crystal_Flame", "Skeleton_Burnt_Soldier"),
                    List.of("Golem_Crystal_Flame", "Golem_Firesteel", "Skeleton_Burnt_Praetorian", "Spirit_Ember", "Zombie_Burnt")
            ),
                    "The air tastes of smoke and something older than fire...",
                    "The charred legion marches — they burned once and liked it!",
                    "The flames die down. Your base is still standing. Barely counts as a win, but it counts."
            ),

            new RaidDefinition("the_void_rupture", 70, List.of(
                    List.of("Larva_Void", "Larva_Void", "Larva_Void", "Eye_Void", "Eye_Void"),
                    List.of("Crawler_Void", "Crawler_Void", "Spawn_Void", "Eye_Void"),
                    List.of("Spawn_Void", "Spawn_Void", "Spectre_Void", "Crawler_Void", "Larva_Void"),
                    List.of("Spectre_Void", "Spectre_Void", "Spawn_Void", "Shadow_Knight")
            ),
                    "Reality flickers. Something on the other side has noticed you...",
                    "The void tears open — creatures from beyond pour through the rift!",
                    "The rift seals. Whatever came through is gone. Whatever watched from the other side is not."
            ),

            new RaidDefinition("bone_corsairs", 95, List.of(
                    List.of("Skeleton_Pirate_Gunner", "Skeleton_Pirate_Gunner", "Skeleton_Pirate_Striker", "Skeleton_Scout"),
                    List.of("Skeleton_Pirate_Captain", "Skeleton_Pirate_Striker", "Horse_Skeleton", "Skeleton_Ranger"),
                    List.of("Skeleton_Pirate_Captain", "Horse_Skeleton_Armored", "Skeleton_Pirate_Gunner", "Skeleton_Pirate_Striker", "Skeleton_Mage")
            ),
                    "You catch the distant sound of a sea shanty with no sea in sight...",
                    "The Bone Corsairs have made port — and your base is the treasure they're after!",
                    "The corsairs retreat, cursing your name in three dead languages. High praise, really."
            ),

            new RaidDefinition("thunder_and_earth", 80, List.of(
                    List.of("Golem_Crystal_Earth", "Golem_Crystal_Earth", "Spirit_Thunder"),
                    List.of("Golem_Crystal_Thunder", "Golem_Crystal_Earth", "Spirit_Thunder", "Spirit_Ember"),
                    List.of("Golem_Crystal_Thunder", "Golem_Firesteel", "Golem_Crystal_Earth", "Spirit_Thunder", "Wraith")
            ),
                    "The ground trembles and the sky crackles without a cloud in sight...",
                    "Earth and thunder converge — the golems have declared your territory a hazard!",
                    "The storm settles and the earth stills. The golems return to wherever golems go when not destroying things."
            ),

            new RaidDefinition("the_night_hunt", 75, List.of(
                    List.of("Hound_Bleached", "Hound_Bleached", "Zombie", "Ghoul", "Ghoul"),
                    List.of("Werewolf", "Ghoul", "Ghoul", "Wraith", "Zombie_Aberrant"),
                    List.of("Werewolf", "Werewolf", "Wraith", "Wraith", "Shadow_Knight")
            ),
                    "The night grows heavier than it should. Something is hunting...",
                    "The Night Hunt has chosen your territory as its quarry — run is not an option!",
                    "Dawn breaks and the hunters scatter. You were the prey that fought back."
            ),

            new RaidDefinition("the_incandescent_crusade", 85, List.of(
                    List.of("Skeleton_Incandescent_Footman", "Skeleton_Incandescent_Footman", "Skeleton_Incandescent_Fighter"),
                    List.of("Skeleton_Incandescent_Fighter", "Skeleton_Incandescent_Mage", "Skeleton_Incandescent_Head", "Skeleton_Soldier"),
                    List.of("Skeleton_Incandescent_Head", "Skeleton_Incandescent_Mage", "Skeleton_Incandescent_Fighter", "Skeleton_Archmage", "Wraith")
            ),
                    "A distant glow pulses on the horizon with unsettling regularity...",
                    "The Incandescent Crusade arrives — glowing, disciplined, and absolutely certain you must go!",
                    "The crusade dims and withdraws. Their conviction was unshaken. Their bones, less so."
            ),

            new RaidDefinition("trork_warpath", 100, List.of(
                    List.of("Trork_Brawler", "Trork_Brawler", "Trork_Guard", "Trork_Sentry"),
                    List.of("Trork_Hunter", "Trork_Mauler", "Trork_Doctor_Witch", "Trork_Guard", "Trork_Brawler"),
                    List.of("Trork_Warrior", "Trork_Chieftain", "Trork_Doctor_Witch", "Trork_Mauler", "Trork_Hunter"),
                    List.of("Trork_Chieftain", "Trork_Warrior", "Trork_Doctor_Witch", "Trork_Doctor_Witch", "Trork_Mauler", "Golem_Crystal_Earth")
            ),
                    "War drums echo from somewhere uncomfortably close...",
                    "The Trork warband has you in their sights — and they brought a witch doctor!",
                    "The warband withdraws, chanting something that sounds like a compliment. For Trorks, it might be."
            ),

            new RaidDefinition("the_grand_convergence", 40, List.of(
                    List.of("Goblin_Ogre", "Outlander_Berserker", "Zombie_Aberrant", "Trork_Warrior", "Skeleton_Burnt_Praetorian"),
                    List.of("Skeleton_Frost_Knight", "Outlander_Brute", "Werewolf", "Ghoul", "Skeleton_Incandescent_Head"),
                    List.of("Golem_Crystal_Thunder", "Golem_Crystal_Flame", "Scarak_Broodmother_Young", "Wraith", "Spawn_Void"),
                    List.of("Shadow_Knight", "Goblin_Duke", "Trork_Chieftain", "Outlander_Cultist", "Skeleton_Archmage", "Spirit_Thunder"),
                    List.of("Hedera", "Werewolf", "Wraith", "Shadow_Knight", "Spectre_Void", "Golem_Firesteel", "Scarak_Broodmother_Young")
            ),
                    "Every faction goes quiet at once. That's never good...",
                    "The Grand Convergence — every enemy you've ever made has compared notes and arrived together!",
                    "The last of them fall. Silence returns. You're not sure the world will remember what just happened, but you will."
            ),

            new RaidDefinition("broodmothers_calling", 65, List.of(
                    List.of("Larva_Void", "Larva_Void", "Larva_Void", "Larva_Void", "Eye_Void", "Eye_Void"),
                    List.of("Crawler_Void", "Crawler_Void", "Spawn_Void", "Larva_Void", "Larva_Void"),
                    List.of("Scarak_Broodmother_Young", "Spawn_Void", "Crawler_Void", "Spectre_Void", "Eye_Void", "Larva_Void")
            ),
                    "A chittering sound rises and falls just below the threshold of comfort...",
                    "The Broodmother has called her children home — and your base is the nest!",
                    "The swarm retreats. The Broodmother survives somewhere in the dark. She is patient."
            )
    );

    // Spawning parameters
    private static final int SPAWN_RING_RADIUS = TerritoryData.TERRITORY_HALF + 8;
    private static final int SPAWN_SCAN_HEIGHT_OFFSET = TerritoryData.TERRITORY_HALF + 8;
    private static final int SPAWN_SEARCH_ATTEMPTS = 8;

    // Inner tick parameters
    private static final int INNER_TICK_INTERVAL_SECONDS = 60;
    private Instant lastInnerTick;
    private final Random random;

    // Active raid groups — CopyOnWriteArrayList for thread safety between scheduler and world executor threads
    private final List<RaidGroup> activeRaids = new CopyOnWriteArrayList<>();

    // Tracks a single active raid's spawned NPCs, target player, and end time
    private static class RaidGroup {
        final Ref<EntityStore> targetPlayerRef;
        final World world;
        final List<Ref<EntityStore>> npcRefs = new ArrayList<>();
        final long raidEndMs;
        final int waveCount;
        final RaidHudState hudState;
        final TerritoryData territory;
        final RaidDefinition definition;

        RaidGroup(Ref<EntityStore> targetPlayerRef, World world, RaidDefinition definition, TerritoryData territory) {
            this.targetPlayerRef = targetPlayerRef;
            this.world = world;
            this.definition = definition;
            this.waveCount = definition.waves.size();
            this.territory = territory;

            this.raidEndMs = System.currentTimeMillis()
                    + ((long)ModConfig.get().raids.seconds_before_first_wave * 1000L)
                    + ((long)(waveCount - 1) * ModConfig.get().raids.seconds_between_waves * 1000L)
                    + ((long)ModConfig.get().raids.seconds_after_last_wave_before_raid_end * 1000L);
            long firstWaveSpawnAtMs = System.currentTimeMillis()
                    + ((long)ModConfig.get().raids.seconds_before_first_wave * 1000L);
            this.hudState = new RaidHudState(waveCount, firstWaveSpawnAtMs, raidEndMs, ModConfig.get().raids.seconds_between_waves);
        }

        void addNpc(Ref<EntityStore> npcRef) {
            npcRefs.add(npcRef);
        }

        void pruneDeadNpcs() {
            npcRefs.removeIf(ref -> !ref.isValid());
        }

        boolean isAlive() {
            for (Ref<EntityStore> ref : npcRefs) {
                if (ref.isValid()) return true;
            }
            return false;
        }
    }

    // Public snapshot of raid state for the HUD to read — set on the player component during an active raid
    public static class RaidHudState {
        public final int totalWaves;
        public final long firstWaveSpawnAtMs;
        public final long raidEndMs;
        public final int secondsBetweenWaves;
        public volatile int currentWave;

        RaidHudState(int totalWaves, long firstWaveSpawnAtMs, long raidEndMs, int secondsBetweenWaves) {
            this.totalWaves = totalWaves;
            this.firstWaveSpawnAtMs = firstWaveSpawnAtMs;
            this.raidEndMs = raidEndMs;
            this.secondsBetweenWaves = secondsBetweenWaves;
            this.currentWave = 0;
        }
    }

    public Module_RaidSystem() {
        this.random = new Random();
        this.lastInnerTick = Instant.now();
    }

    // Rolls against the raid registry weights and returns a random RaidDefinition, or null if registry is empty
    private RaidDefinition rollRaidDefinition() {
        if (RAID_REGISTRY.isEmpty()) return null;

        int totalWeight = 0;
        for (RaidDefinition def : RAID_REGISTRY) totalWeight += def.weight;

        int roll = random.nextInt(totalWeight);
        int accumulated = 0;
        for (RaidDefinition def : RAID_REGISTRY) {
            accumulated += def.weight;
            if (roll < accumulated) return def;
        }

        return RAID_REGISTRY.get(0);
    }

    // Looks up a RaidDefinition by name, or null if not found
    private static RaidDefinition findDefinitionByName(String name) {
        if (name == null) return null;
        for (RaidDefinition def : RAID_REGISTRY) {
            if (def.name.equals(name)) return def;
        }
        return null;
    }

    // Outer tick — called externally several times per second
    public void outerTick() {
        if (!ModConfig.get().building.allow_light_well_territory_claim) return;

        long secondsSinceLastTick = Instant.now().getEpochSecond() - lastInnerTick.getEpochSecond();
        if (secondsSinceLastTick >= INNER_TICK_INTERVAL_SECONDS) innerTick();

        tickActiveRaids();
    }

    // Inner tick — runs once per minute, evaluates every player independently
    private void innerTick() {
        lastInnerTick = Instant.now();

        long nowEpochSeconds = Instant.now().getEpochSecond();
        long raidTimerSeconds = ModConfig.get().raids.raid_cooldown_in_minutes * 60L;

        for (World world : Universe.get().getWorlds().values().toArray(new World[0])) {
            world.execute(() -> {
                Store<EntityStore> store = world.getEntityStore().getStore();

                for (PlayerRef playerRef : Universe.get().getPlayers()) {
                    try {
                        if (!playerRef.isValid()) continue;

                        Ref<EntityStore> ref = playerRef.getReference();
                        if (ref == null) continue;

                        Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPGSystem.componentTypeRPGPlayer);
                        if (rpgPlayer == null) continue;

                        // skip players already being raided
                        if (rpgPlayer.activeRaidHudState != null) continue;

                        // roll and assign the next raid for this player if they don't have one yet
                        if (rpgPlayer.nextRaid == null) {
                            RaidDefinition rolled = rollRaidDefinition();
                            if (rolled != null) rpgPlayer.nextRaid = rolled.name;
                        }

                        // resolve the definition we'll use if a raid fires this tick
                        RaidDefinition nextDefinition = findDefinitionByName(rpgPlayer.nextRaid);
                        if (nextDefinition == null) continue;

                        // check base raid
                        if (ModConfig.get().raids.allow_base_raids) {
                            long secondsSinceLastBaseRaid = nowEpochSeconds - rpgPlayer.lastBaseRaid;
                            if (secondsSinceLastBaseRaid >= raidTimerSeconds) {
                                // get the players territory data or bail if they don't have one
                                TerritoryData territory = getTerritory(world, playerRef);
                                if (territory == null) continue;

                                if (random.nextInt(100) < ModConfig.get().raids.raid_chance) {
                                    rpgPlayer.nextRaid = null;
                                    startBaseRaid(playerRef, ref, store, world, territory, nextDefinition);
                                } else {
                                    sendRaidMessage(playerRef, nextDefinition.prePhrase, new Color(0x888888));
                                }
                            }
                        }

                        // check player raid — only if a base raid didn't already fire
                        if (rpgPlayer.activeRaidHudState == null && ModConfig.get().raids.allow_player_raids) {
                            long secondsSinceLastPlayerRaid = nowEpochSeconds - rpgPlayer.lastPlayerRaid;
                            if (secondsSinceLastPlayerRaid >= raidTimerSeconds) {
                                if (random.nextInt(100) < ModConfig.get().raids.raid_chance) {
                                    rpgPlayer.nextRaid = null;
                                    startPlayerRaid(playerRef, ref, store, world, nextDefinition);
                                } else {
                                    sendRaidMessage(playerRef, nextDefinition.prePhrase, new Color(0x888888));
                                }
                            }
                        }

                    } catch (Exception e) {
                        System.err.println("[RaidSystem] Error evaluating raid for player: " + e.getMessage());
                    }
                }
            });
        }
    }

    // Base raid — targets the player's Light Well territory
    public void startBaseRaid(PlayerRef playerRef, Ref<EntityStore> ref, Store<EntityStore> store, World world, TerritoryData territory, RaidDefinition definition) {
        // bail if no territory
        if (territory == null) return;

        // get the rpg player component and check it's last raid
        Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPGSystem.componentTypeRPGPlayer);
        if (rpgPlayer == null) return;
        rpgPlayer.lastBaseRaid = Instant.now().getEpochSecond();

        // get the territory coords
        int cx = territory.getCenter().x;
        int cy = territory.getCenter().y;
        int cz = territory.getCenter().z;

        RaidGroup group = new RaidGroup(ref, world, definition, territory);
        spawnWaves(world, store, cx, cy, cz, true, group, definition);
        activeRaids.add(group);

        keepTerritoryChunksLoaded(world, territory, true);
        setRaidHudState(store, ref, group.hudState);

        rpgPlayer.nextRaid = null;

        sendRaidMessage(playerRef, definition.phrase, new Color(0xFF8800));
        System.out.println("[RaidSystem] Base raid '" + definition.name + "' started for " + playerRef.getUsername() + " at territory (" + cx + ", " + cy + ", " + cz + ")");
    }

    // Player raid — targets the player's current position
    public void startPlayerRaid(PlayerRef playerRef, Ref<EntityStore> ref, Store<EntityStore> store, World world, RaidDefinition definition) {
        Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPGSystem.componentTypeRPGPlayer);
        if (rpgPlayer == null) return;

        rpgPlayer.lastPlayerRaid = Instant.now().getEpochSecond();

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        int cx = (int) Math.floor(transform.getPosition().getX());
        int cy = (int) Math.floor(transform.getPosition().getY());
        int cz = (int) Math.floor(transform.getPosition().getZ());

        RaidGroup group = new RaidGroup(ref, world, definition, null);
        spawnWaves(world, store, cx, cy, cz, false, group, definition);
        activeRaids.add(group);

        setRaidHudState(store, ref, group.hudState);

        rpgPlayer.nextRaid = null;

        sendRaidMessage(playerRef, definition.phrase, new Color(0xFF8800));
        System.out.println("[RaidSystem] Player raid '" + definition.name + "' started for " + playerRef.getUsername() + " at (" + cx + ", " + cy + ", " + cz + ")");
    }

    // Wave scheduling — fires each wave seconds apart based on config, then schedules the raid end callback
    private void spawnWaves(World world, Store<EntityStore> store, int cx, int cy, int cz, boolean spawnOutsideTerritory, RaidGroup group, RaidDefinition definition) {
        long firstWaveDelayMs = (long) ModConfig.get().raids.seconds_before_first_wave * 1000L;

        for (int waveIndex = 0; waveIndex < definition.waves.size(); waveIndex++) {
            final int wave = waveIndex;
            final long delayMs = firstWaveDelayMs + ((long) wave * ModConfig.get().raids.seconds_between_waves * 1000L);

            Thread.ofVirtual().start(() -> {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                world.execute(() -> spawnWave(world, store, cx, cy, cz, wave, spawnOutsideTerritory, group, definition));
            });
        }

        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(group.raidEndMs - System.currentTimeMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            world.execute(() -> onRaidEnd(group, world));
        });
    }

    // Single wave spawn
    private void spawnWave(World world, Store<EntityStore> store, int cx, int cy, int cz, int waveIndex, boolean spawnOutsideTerritory, RaidGroup group, RaidDefinition definition) {
        List<String> npcsToSpawn = definition.waves.get(waveIndex);
        System.out.println("[RaidSystem] Spawning wave " + (waveIndex + 1) + " of raid '" + definition.name + "' (" + npcsToSpawn.size() + " enemies)");

        for (String npcId : npcsToSpawn) {
            Vector3d spawnPos = findSafeSpawnPosition(world, cx, cy, cz, spawnOutsideTerritory);
            Vector3f rotation = new Vector3f(0f, (float)(random.nextDouble() * Math.PI * 2), 0f);

            var spawnResult = NPCPlugin.get().spawnNPC(store, npcId, null, spawnPos, rotation);
            if (spawnResult == null) continue;

            Ref<EntityStore> npcRef = spawnResult.first();
            NPCEntity npcEntity = (NPCEntity) spawnResult.second();

            npcEntity.saveLeashInformation(new Vector3d(cx, cy, cz), rotation);
            group.addNpc(npcRef);
        }

        group.hudState.currentWave = waveIndex + 1;
    }

    // Raid end callback — explodes any surviving NPCs and cleans up the group from the active raids list
    private void onRaidEnd(RaidGroup group, World world) {
        Store<EntityStore> store = world.getEntityStore().getStore();
        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();

        group.pruneDeadNpcs();

        // remove this group from the active raids list
        activeRaids.remove(group);

        // explode any NPCs that are still alive
        if (group.isAlive()) {
            System.out.println("[RaidSystem] Raid ended with " + group.npcRefs.size() + " surviving NPCs — detonating.");
            for (Ref<EntityStore> npcRef : group.npcRefs) {
                if (!npcRef.isValid()) continue;
                try {
                    explodeNpc(npcRef, store, chunkStore);
                } catch (Exception e) {
                    System.err.println("[RaidSystem] Error exploding NPC: " + e.getMessage());
                }
            }
        }

        if (group.territory != null) keepTerritoryChunksLoaded(group.world, group.territory, false);

        clearRaidHudState(store, group.targetPlayerRef);

        // send the post-phrase
        try {
            if (group.targetPlayerRef.isValid()) {
                PlayerRef playerRef = store.getComponent(group.targetPlayerRef, PlayerRef.getComponentType());
                if (playerRef != null) sendRaidMessage(playerRef, group.definition.postPhrase, new Color(0xFF8800));
            }
        } catch (Exception e) {
            System.err.println("[RaidSystem] Error sending post-raid message: " + e.getMessage());
        }

        System.out.println("[RaidSystem] Raid group cleaned up.");
    }

    // Explodes a single surviving raid NPC — damages nearby blocks and players, then removes the NPC cleanly
    private static void explodeNpc(Ref<EntityStore> npcRef, Store<EntityStore> store, Store<ChunkStore> chunkStore) {
        TransformComponent transform = store.getComponent(npcRef, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        int cx = (int) Math.floor(pos.x);
        int cy = (int) Math.floor(pos.y);
        int cz = (int) Math.floor(pos.z);

        if (ModConfig.get().raids.unkilled_raid_enemies_explode) {
            for (int x = cx - ModConfig.get().raids.explosion_hit_radius_blocks; x <= cx + ModConfig.get().raids.explosion_hit_radius_blocks; x++) {
                for (int y = cy - ModConfig.get().raids.explosion_hit_radius_blocks; y <= cy + ModConfig.get().raids.explosion_hit_radius_blocks; y++) {
                    for (int z = cz - ModConfig.get().raids.explosion_hit_radius_blocks; z <= cz + ModConfig.get().raids.explosion_hit_radius_blocks; z++) {
                        if (Math.sqrt((x-cx)*(x-cx) + (y-cy)*(y-cy) + (z-cz)*(z-cz)) > ModConfig.get().raids.explosion_hit_radius_blocks) continue;
                        try {
                            long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
                            Ref<ChunkStore> chunkRef = ((ChunkStore) chunkStore.getExternalData()).getChunkReference(chunkIndex);
                            if (chunkRef == null || !chunkRef.isValid()) continue;

                            WorldChunk worldChunk = (WorldChunk) chunkStore.getComponent(chunkRef, WorldChunk.getComponentType());
                            if (worldChunk == null) continue;

                            int blockId = worldChunk.getBlock(x, y, z);
                            BlockType bt = BlockType.getAssetMap().getAsset(blockId);
                            if (bt == null || !RoomFloodFill.isStructural(bt)) continue;

                            BlockHarvestUtils.performBlockDamage(null, null, new Vector3i(x, y, z), null, null, null, false, ModConfig.get().raids.explosion_hit_damage_blocks, 2048 | 1024, chunkRef, store, chunkStore);
                        } catch (Exception e) {
                            System.err.println("[RaidSystem] Error breaking block during explosion: " + e.getMessage());
                        }
                    }
                }
            }

            Damage.EnvironmentSource explosionSource = new Damage.EnvironmentSource("explosion");
            List<Ref<EntityStore>> nearby = TargetUtil.getAllEntitiesInBox(
                    new Vector3d(pos.x - ModConfig.get().raids.explosion_hit_radius_entities, pos.y - ModConfig.get().raids.explosion_hit_radius_entities, pos.z - ModConfig.get().raids.explosion_hit_radius_entities),
                    new Vector3d(pos.x + ModConfig.get().raids.explosion_hit_radius_entities, pos.y + ModConfig.get().raids.explosion_hit_radius_entities, pos.z + ModConfig.get().raids.explosion_hit_radius_entities),
                    store
            );
            for (Ref<EntityStore> targetRef : nearby) {
                if (!targetRef.isValid() || targetRef.equals(npcRef)) continue;
                if (store.getComponent(targetRef, NPCEntity.getComponentType()) != null) continue;
                try {
                    TransformComponent targetTransform = store.getComponent(targetRef, TransformComponent.getComponentType());
                    if (targetTransform == null) continue;
                    double distance = pos.distanceTo(targetTransform.getPosition());
                    if (distance > ModConfig.get().raids.explosion_hit_radius_entities) continue;
                    float damage = ModConfig.get().raids.explosion_hit_damage_entities * (1f - (float)(distance / ModConfig.get().raids.explosion_hit_radius_entities));
                    if (damage > 0) DamageSystems.executeDamage(targetRef, store, new Damage(explosionSource, DamageCause.getAssetMap().getAsset("Environment"), damage));
                } catch (Exception e) {
                    System.err.println("[RaidSystem] Error damaging entity during explosion: " + e.getMessage());
                }
            }
        }

        store.removeEntity(npcRef, RemoveReason.REMOVE);
    }

    // Safe spawn position search
    private Vector3d findSafeSpawnPosition(World world, int cx, int cy, int cz, boolean spawnOutsideTerritory) {
        int radius = spawnOutsideTerritory ? SPAWN_RING_RADIUS : (SPAWN_RING_RADIUS / 2);
        double startAngle = random.nextDouble() * 2 * Math.PI;

        for (int attempt = 0; attempt < SPAWN_SEARCH_ATTEMPTS; attempt++) {
            double angle = startAngle + (2 * Math.PI / SPAWN_SEARCH_ATTEMPTS) * attempt;
            int offsetX = (int) Math.round(Math.cos(angle) * radius);
            int offsetZ = (int) Math.round(Math.sin(angle) * radius);

            int scanX = cx + offsetX;
            int scanZ = cz + offsetZ;

            int groundY = findGroundY(world, scanX, cy + SPAWN_SCAN_HEIGHT_OFFSET, scanZ);
            if (groundY != Integer.MIN_VALUE) {
                return new Vector3d(scanX + 0.5, groundY + 1.0, scanZ + 0.5);
            }
        }

        System.err.println("[RaidSystem] Could not find safe spawn ground after " + SPAWN_SEARCH_ATTEMPTS + " attempts, using fallback position.");
        return new Vector3d(cx + radius + 0.5, cy + 1.0, cz + 0.5);
    }

    // Scan down from (x, startY, z) and returns the Y of the first solid non-fluid block, or Integer.MIN_VALUE if none found
    private int findGroundY(World world, int x, int startY, int z) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
        if (chunk == null) return Integer.MIN_VALUE;

        int clampedStart = Math.min(startY, 319);
        for (int y = clampedStart; y >= 1; y--) {
            int blockId = chunk.getBlock(x, y, z);
            BlockType bt = BlockType.getAssetMap().getAsset(blockId);
            if (bt != null && RoomFloodFill.isStructural(bt)) return y;
        }
        return Integer.MIN_VALUE;
    }

    // Sets the raid HUD state on the player's RPG component
    private static void setRaidHudState(Store<EntityStore> store, Ref<EntityStore> playerRef, RaidHudState state) {
        Component_RPG_Player rpgPlayer = store.getComponent(playerRef, Module_RPGSystem.componentTypeRPGPlayer);
        if (rpgPlayer == null) return;
        rpgPlayer.activeRaidHudState = state;
    }

    // Clears the raid HUD state from the player's RPG component when the raid ends
    private static void clearRaidHudState(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        if (!playerRef.isValid()) return;
        Component_RPG_Player rpgPlayer = store.getComponent(playerRef, Module_RPGSystem.componentTypeRPGPlayer);
        if (rpgPlayer == null) return;
        rpgPlayer.activeRaidHudState = null;
    }

    // Method to load and keep loaded chunks of a territory actively being raided
    private void keepTerritoryChunksLoaded(World world, TerritoryData territory, boolean load) {
        int minChunkX = ChunkUtil.chunkCoordinate(territory.getMinX());
        int maxChunkX = ChunkUtil.chunkCoordinate(territory.getMaxX());
        int minChunkZ = ChunkUtil.chunkCoordinate(territory.getMinZ());
        int maxChunkZ = ChunkUtil.chunkCoordinate(territory.getMaxZ());

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                long index = ChunkUtil.indexChunk(cx, cz);
                if (load) {
                    world.getChunkAsync(index).thenAcceptAsync(chunk -> {
                        if (chunk != null) {
                            chunk.addKeepLoaded();
                            chunk.resetKeepAlive();
                            chunk.resetActiveTimer();
                            world.loadChunkIfInMemory(index);
                        }
                    }, world);
                } else {
                    WorldChunk chunk = world.getChunkIfInMemory(index);
                    if (chunk != null) chunk.removeKeepLoaded();
                }
            }
        }
    }

    // Keeps territory chunks ticking during active raids so NPCs and blocks remain active
    private void tickActiveRaids() {
        for (RaidGroup group : activeRaids) {
            if (group.territory == null) continue;
            int minChunkX = ChunkUtil.chunkCoordinate(group.territory.getMinX());
            int maxChunkX = ChunkUtil.chunkCoordinate(group.territory.getMaxX());
            int minChunkZ = ChunkUtil.chunkCoordinate(group.territory.getMinZ());
            int maxChunkZ = ChunkUtil.chunkCoordinate(group.territory.getMaxZ());
            for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                    WorldChunk chunk = group.world.getChunkIfInMemory(ChunkUtil.indexChunk(cx, cz));
                    if (chunk != null) {
                        chunk.resetActiveTimer();
                        chunk.resetKeepAlive();
                    }
                }
            }
        }
    }

    // try to get a players territory data
    private TerritoryData getTerritory(World world, PlayerRef playerRef) {
        WorldRoomRegistry registry = WorldRoomRegistry.get(world);
        if (registry == null) return null;

        TerritoryData territory = null;
        for (TerritoryData t : registry.getAllTerritories()) {
            if (playerRef.getUuid().equals(t.getOwnerUuid())) {
                territory = t;
                break;
            }
        }

        return territory;
    }

    // Sends a colored chat message to a player
    private static void sendRaidMessage(PlayerRef playerRef, String text, Color color) {
        try {
            playerRef.sendMessage(Message.raw(text).color(color));
        } catch (Exception e) {}
    }

    // Trigger a raid via command, using the player's queued nextRaid or a fresh roll
    public void triggerRaidByCommand(PlayerRef playerRef, Ref<EntityStore> ref, Store<EntityStore> store, World world, String raidType) {
        Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPGSystem.componentTypeRPGPlayer);
        if (rpgPlayer == null) return;

        RaidDefinition definition = findDefinitionByName(rpgPlayer.nextRaid);
        if (definition == null) definition = rollRaidDefinition();
        if (definition == null) return;

        if (raidType.equals("base")) {
            startBaseRaid(playerRef, ref, store, world, getTerritory(world, playerRef), definition);
        } else {
            startPlayerRaid(playerRef, ref, store, world, definition);
        }
    }
}