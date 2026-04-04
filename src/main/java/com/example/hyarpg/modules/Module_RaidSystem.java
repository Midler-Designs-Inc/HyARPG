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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Module_RaidSystem {

    // Small class for defining raids
    private static class RaidDefinition {
        final String name;
        final int weight;
        final List<List<String>> waves;

        RaidDefinition(String name, int weight, List<List<String>> waves) {
            this.name = name;
            this.weight = weight;
            this.waves = waves;
        }
    }

    // Registry of defined raids
    private static final List<RaidDefinition> RAID_REGISTRY = List.of(

            // === THE RISEN TIDE ===
            // Undead animals soften you up before the heavy hitters arrive
            new RaidDefinition("the_risen_tide", 100, List.of(
                List.of("Chicken_Undead", "Chicken_Undead", "Pig_Undead", "Pig_Undead", "Zombie"),
                List.of("Cow_Undead", "Cow_Undead", "Zombie", "Zombie", "Ghoul"),
                List.of("Ghoul", "Ghoul", "Zombie_Aberrant", "Wraith")
            )),

            // === COLD FRONT ===
            // A frost-themed ambush that escalates from scouts to siege
            new RaidDefinition("cold_front", 95, List.of(
                List.of("Skeleton_Frost_Scout", "Skeleton_Frost_Scout", "Skeleton_Frost_Ranger", "Zombie_Frost"),
                List.of("Skeleton_Frost_Soldier", "Skeleton_Frost_Fighter", "Skeleton_Frost_Mage", "Spirit_Frost"),
                List.of("Skeleton_Frost_Knight", "Skeleton_Frost_Archmage", "Golem_Crystal_Frost", "Zombie_Frost", "Spirit_Frost")
            )),

            // === THE GOBLIN ECONOMY ===
            // Goblins of every trade — ends with their leadership demanding tribute
            new RaidDefinition("the_goblin_economy", 110, List.of(
                List.of("Goblin_Miner", "Goblin_Miner", "Goblin_Scavenger", "Goblin_Hermit"),
                List.of("Goblin_Lobber", "Goblin_Lobber", "Goblin_Thief", "Goblin_Ogre"),
                List.of("Goblin_Duke", "Goblin_Ogre", "Goblin_Lobber", "Goblin_Scavenger", "Goblin_Thief")
            )),

            // === DESERT AWAKENING ===
            // Ancient sand constructs and their skeletal guardians surge forward
            new RaidDefinition("desert_awakening", 90, List.of(
                List.of("Skeleton_Sand_Scout", "Skeleton_Sand_Ranger", "Zombie_Sand", "Zombie_Sand"),
                List.of("Skeleton_Sand_Guard", "Skeleton_Sand_Mage", "Skeleton_Sand_Archmage", "Golem_Crystal_Sand"),
                List.of("Golem_Crystal_Sand", "Skeleton_Sand_Archmage", "Zombie_Sand", "Skeleton_Sand_Guard", "Skeleton_Sand_Ranger")
            )),

            // === SPIRITS OF THE WILD ===
            // Pure elemental chaos — spirits from every domain converging at once
            new RaidDefinition("spirits_of_the_wild", 75, List.of(
                List.of("Spirit_Root", "Spirit_Root", "Spirit_Ember"),
                List.of("Spirit_Frost", "Spirit_Thunder", "Spirit_Root", "Spirit_Ember"),
                List.of("Spirit_Thunder", "Spirit_Thunder", "Spirit_Frost", "Spirit_Ember", "Hedera")
            )),

            // === THE OUTLANDER VANGUARD ===
            // Starts stealthy with scouts and hunters, ends with a ritual assault
            new RaidDefinition("the_outlander_vanguard", 85, List.of(
                List.of("Outlander_Peon", "Outlander_Stalker", "Outlander_Hunter", "Outlander_Hunter"),
                List.of("Outlander_Marauder", "Outlander_Berserker", "Outlander_Brute", "Outlander_Sorcerer"),
                List.of("Outlander_Priest", "Outlander_Cultist", "Outlander_Brute", "Outlander_Sorcerer", "Outlander_Berserker")
            )),

            // === FIRE AND ASH ===
            // Burnt skeletons backed by scorching golems — a wave of pure heat
            new RaidDefinition("fire_and_ash", 90, List.of(
                List.of("Skeleton_Burnt_Archer", "Skeleton_Burnt_Gunner", "Zombie_Burnt", "Zombie_Burnt"),
                List.of("Skeleton_Burnt_Knight", "Skeleton_Burnt_Lancer", "Skeleton_Burnt_Alchemist", "Spirit_Ember"),
                List.of("Skeleton_Burnt_Praetorian", "Skeleton_Burnt_Wizard", "Golem_Crystal_Flame", "Skeleton_Burnt_Soldier"),
                List.of("Golem_Crystal_Flame", "Golem_Firesteel", "Skeleton_Burnt_Praetorian", "Spirit_Ember", "Zombie_Burnt")
            )),

            // === THE VOID RUPTURE ===
            // Small void creatures swarm first; then the heavy void beasts tear through
            new RaidDefinition("the_void_rupture", 70, List.of(
                List.of("Larva_Void", "Larva_Void", "Larva_Void", "Eye_Void", "Eye_Void"),
                List.of("Crawler_Void", "Crawler_Void", "Spawn_Void", "Eye_Void"),
                List.of("Spawn_Void", "Spawn_Void", "Spectre_Void", "Crawler_Void", "Larva_Void"),
                List.of("Spectre_Void", "Spectre_Void", "Spawn_Void", "Shadow_Knight")
            )),

            // === BONE CORSAIRS ===
            // Skeleton pirates raid with a motley mix of support and heavy hitters on horseback
            new RaidDefinition("bone_corsairs", 95, List.of(
                List.of("Skeleton_Pirate_Gunner", "Skeleton_Pirate_Gunner", "Skeleton_Pirate_Striker", "Skeleton_Scout"),
                List.of("Skeleton_Pirate_Captain", "Skeleton_Pirate_Striker", "Horse_Skeleton", "Skeleton_Ranger"),
                List.of("Skeleton_Pirate_Captain", "Horse_Skeleton_Armored", "Skeleton_Pirate_Gunner", "Skeleton_Pirate_Striker", "Skeleton_Mage")
            )),

            // === THUNDER AND EARTH ===
            // A golem clash — earthen and thunder golems supported by their spirit kin
            new RaidDefinition("thunder_and_earth", 80, List.of(
                List.of("Golem_Crystal_Earth", "Golem_Crystal_Earth", "Spirit_Thunder"),
                List.of("Golem_Crystal_Thunder", "Golem_Crystal_Earth", "Spirit_Thunder", "Spirit_Ember"),
                List.of("Golem_Crystal_Thunder", "Golem_Firesteel", "Golem_Crystal_Earth", "Spirit_Thunder", "Wraith")
            )),

            // === THE NIGHT HUNT ===
            // Werewolves and wraiths join rogue undead for a terrifying nocturnal ambush
            new RaidDefinition("the_night_hunt", 75, List.of(
                List.of("Hound_Bleached", "Hound_Bleached", "Zombie", "Ghoul", "Ghoul"),
                List.of("Werewolf", "Ghoul", "Ghoul", "Wraith", "Zombie_Aberrant"),
                List.of("Werewolf", "Werewolf", "Wraith", "Wraith", "Shadow_Knight")
            )),

            // === THE INCANDESCENT CRUSADE ===
            // Glowing incandescent skeletons march in formation — a relentless disciplined assault
            new RaidDefinition("the_incandescent_crusade", 85, List.of(
                List.of("Skeleton_Incandescent_Footman", "Skeleton_Incandescent_Footman", "Skeleton_Incandescent_Fighter"),
                List.of("Skeleton_Incandescent_Fighter", "Skeleton_Incandescent_Mage", "Skeleton_Incandescent_Head", "Skeleton_Soldier"),
                List.of("Skeleton_Incandescent_Head", "Skeleton_Incandescent_Mage", "Skeleton_Incandescent_Fighter", "Skeleton_Archmage", "Wraith")
            )),

            // === TRORK WARPATH ===
            // Trorks charge in waves with healers and shamans disrupting your defense
            new RaidDefinition("trork_warpath", 100, List.of(
                List.of("Trork_Brawler", "Trork_Brawler", "Trork_Guard", "Trork_Sentry"),
                List.of("Trork_Hunter", "Trork_Mauler", "Trork_Doctor_Witch", "Trork_Guard", "Trork_Brawler"),
                List.of("Trork_Warrior", "Trork_Chieftain", "Trork_Doctor_Witch", "Trork_Mauler", "Trork_Hunter"),
                List.of("Trork_Chieftain", "Trork_Warrior", "Trork_Doctor_Witch", "Trork_Doctor_Witch", "Trork_Mauler", "Golem_Crystal_Earth")
            )),

            // === THE GRAND CONVERGENCE ===
            // The ultimate raid — every faction sends their elites in a chaotic final assault
            new RaidDefinition("the_grand_convergence", 40, List.of(
                List.of("Goblin_Ogre", "Outlander_Berserker", "Zombie_Aberrant", "Trork_Warrior", "Skeleton_Burnt_Praetorian"),
                List.of("Skeleton_Frost_Knight", "Outlander_Brute", "Werewolf", "Ghoul", "Skeleton_Incandescent_Head"),
                List.of("Golem_Crystal_Thunder", "Golem_Crystal_Flame", "Scarak_Broodmother_Young", "Wraith", "Spawn_Void"),
                List.of("Shadow_Knight", "Goblin_Duke", "Trork_Chieftain", "Outlander_Cultist", "Skeleton_Archmage", "Spirit_Thunder"),
                List.of("Hedera", "Werewolf", "Wraith", "Shadow_Knight", "Spectre_Void", "Golem_Firesteel", "Scarak_Broodmother_Young")
            )),

            // === BROODMOTHER'S CALLING ===
            // Void swarms protect the Scarak Broodmother — a desperate bug-and-void siege
            new RaidDefinition("broodmothers_calling", 65, List.of(
                List.of("Larva_Void", "Larva_Void", "Larva_Void", "Larva_Void", "Eye_Void", "Eye_Void"),
                List.of("Crawler_Void", "Crawler_Void", "Spawn_Void", "Larva_Void", "Larva_Void"),
                List.of("Scarak_Broodmother_Young", "Spawn_Void", "Crawler_Void", "Spectre_Void", "Eye_Void", "Larva_Void")
            ))

    );

    // Spawning parameters
    private static final int SPAWN_RING_RADIUS = TerritoryData.TERRITORY_HALF + 8; // ~36 blocks from center
    private static final int SPAWN_SCAN_HEIGHT_OFFSET = TerritoryData.TERRITORY_HALF + 8;
    private static final int SPAWN_SEARCH_ATTEMPTS = 8;

    // Inner tick parameters
    private static final int INNER_TICK_INTERVAL_SECONDS = 60;
    private Instant lastInnerTick;
    private final Random random;

    // Active raid groups — each tracks the spawned NPCs and their target player for one ongoing raid
    private final List<RaidGroup> activeRaids = new ArrayList<>();

    // Tracks a single active raid's spawned NPCs, target player, and end time
    private static class RaidGroup {
        final Ref<EntityStore> targetPlayerRef;
        final World world;
        final List<Ref<EntityStore>> npcRefs = new ArrayList<>();
        final long raidEndMs;
        final int waveCount;
        final RaidHudState hudState;
        final TerritoryData territory;

        RaidGroup(Ref<EntityStore> targetPlayerRef, World world, RaidDefinition definition, TerritoryData territory) {
            this.targetPlayerRef = targetPlayerRef;
            this.world = world;
            this.waveCount = definition.waves.size();
            this.territory = territory;

            // raid ends after: pre-first-wave delay + all wave intervals + post-last-wave grace period
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

        // Prune dead/invalid NPC refs from this group
        void pruneDeadNpcs() {
            npcRefs.removeIf(ref -> !ref.isValid());
        }

        // Returns true if the group still has at least one living NPC
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
        public volatile int currentWave; // 0 = pre-first-wave, 1+ = wave that just spawned

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

        // sum all weights
        int totalWeight = 0;
        for (RaidDefinition def : RAID_REGISTRY) totalWeight += def.weight;

        // roll a value in range and walk the list until we find the winner
        int roll = random.nextInt(totalWeight);
        int accumulated = 0;
        for (RaidDefinition def : RAID_REGISTRY) {
            accumulated += def.weight;
            if (roll < accumulated) return def;
        }

        // fallback — should never reach here
        return RAID_REGISTRY.get(0);
    }

    // Outer tick — called externally several times per second
    public void outerTick(){
        // If territory claiming is disabled, raiding as a whole is disabled — do nothing
        if (!ModConfig.get().building.allow_light_well_territory_claim) return;

        // check if the inner tick should fire or not
        long secondsSinceLastTick = Instant.now().getEpochSecond() - lastInnerTick.getEpochSecond();
        if (secondsSinceLastTick >= INNER_TICK_INTERVAL_SECONDS) innerTick();

        // keep raid chunks ticking
        tickActiveRaids();
    }

    // Inner tick — runs once per minute, evaluates every player independently
    private void innerTick() {
        // update the last inner tick time to now
        lastInnerTick = Instant.now();

        // get a time stamp and a raider timer seconds for comparison
        long nowEpochSeconds = Instant.now().getEpochSecond();
        long raidTimerSeconds = ModConfig.get().raids.raid_cooldown_in_minutes * 60L;

        // loop over each world and then on that worlds next execute loop over players
        for (World world : Universe.get().getWorlds().values().toArray(new World[0])) {
            world.execute(() -> {
                Store<EntityStore> store = world.getEntityStore().getStore();

                // loop over players
                for (PlayerRef playerRef : Universe.get().getPlayers()) {
                    try {
                        // player isn't valid ignore them
                        if (!playerRef.isValid()) continue;

                        // could not get the ref for this player ignore them
                        Ref<EntityStore> ref = playerRef.getReference();
                        if (ref == null) continue;

                        // this player does not have our main mod component ignore them
                        Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPGSystem.componentTypeRPGPlayer);
                        if (rpgPlayer == null) continue;

                        // skip this player entirely if they already have an active raid in progress
                        if (rpgPlayer.activeRaidHudState != null) continue;

                        // check if base raiding is enabled and if so, check if the player's base should be raided
                        if (ModConfig.get().raids.allow_base_raids) {
                            long secondsSinceLastBaseRaid = nowEpochSeconds - rpgPlayer.lastBaseRaid;
                            if (secondsSinceLastBaseRaid >= raidTimerSeconds) {
                                if (random.nextInt(100) < ModConfig.get().raids.raid_chance) {
                                    startBaseRaid(playerRef, ref, store, world);
                                }
                            }
                        }

                        // check if player raiding is enabled and if so, check if hte player should be raided
                        if (ModConfig.get().raids.allow_player_raids) {
                            long secondsSinceLastPlayerRaid = nowEpochSeconds - rpgPlayer.lastPlayerRaid;
                            if (secondsSinceLastPlayerRaid >= raidTimerSeconds) {
                                if (random.nextInt(100) < ModConfig.get().raids.raid_chance) {
                                    startPlayerRaid(playerRef, ref, store, world);
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
    public void startBaseRaid(PlayerRef playerRef, Ref<EntityStore> ref, Store<EntityStore> store, World world) {
        Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPGSystem.componentTypeRPGPlayer);
        if (rpgPlayer == null) return;

        // Stamp the timer immediately regardless of outcome
        rpgPlayer.lastBaseRaid = Instant.now().getEpochSecond();

        // look up the player's territory from the registry for this world
        WorldRoomRegistry registry = WorldRoomRegistry.get(world);
        if (registry == null) {
            System.err.println("[RaidSystem] Base raid fired for " + playerRef.getUsername() + " but no registry found for world — skipping.");
            return;
        }

        TerritoryData territory = null;
        for (TerritoryData t : registry.getAllTerritories()) {
            if (playerRef.getUuid().equals(t.getOwnerUuid())) {
                territory = t;
                break;
            }
        }

        if (territory == null) {
            System.err.println("[RaidSystem] Base raid fired for " + playerRef.getUsername() + " but they have no territory in this world — skipping.");
            return;
        }

        // roll which raid definition to use
        RaidDefinition definition = rollRaidDefinition();
        if (definition == null) {
            System.err.println("[RaidSystem] No raid definitions in registry — skipping.");
            return;
        }

        // Spawn center is the territory's light well center
        int cx = territory.getCenter().x;
        int cy = territory.getCenter().y;
        int cz = territory.getCenter().z;

        RaidGroup group = new RaidGroup(ref, world, definition, territory);
        spawnWaves(world, store, cx, cy, cz, true, group, definition);
        activeRaids.add(group);

        // load the players base chunks into memory and keep them alive during the raid time
        keepTerritoryChunksLoaded(world, territory, true);

        // set the raid HUD state on the player component so the HUD can display raid info
        setRaidHudState(store, ref, group.hudState);
        System.out.println("[RaidSystem] Base raid '" + definition.name + "' started for " + playerRef.getUsername() + " at territory (" + cx + ", " + cy + ", " + cz + ")");
    }

    // Player raid — targets the player's current position
    public void startPlayerRaid(PlayerRef playerRef, Ref<EntityStore> ref, Store<EntityStore> store, World world) {
        Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPGSystem.componentTypeRPGPlayer);
        if (rpgPlayer == null) return;

        // Stamp the timer immediately regardless of outcome
        rpgPlayer.lastPlayerRaid = Instant.now().getEpochSecond();

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        // roll which raid definition to use
        RaidDefinition definition = rollRaidDefinition();
        if (definition == null) {
            System.err.println("[RaidSystem] No raid definitions in registry — skipping.");
            return;
        }

        int cx = (int) Math.floor(transform.getPosition().getX());
        int cy = (int) Math.floor(transform.getPosition().getY());
        int cz = (int) Math.floor(transform.getPosition().getZ());

        RaidGroup group = new RaidGroup(ref, world, definition, null);
        spawnWaves(world, store, cx, cy, cz, /* outsideTerritory */ false, group, definition);
        activeRaids.add(group);

        // set the raid HUD state on the player component so the HUD can display raid info
        setRaidHudState(store, ref, group.hudState);
        System.out.println("[RaidSystem] Player raid '" + definition.name + "' started for " + playerRef.getUsername() + " at (" + cx + ", " + cy + ", " + cz + ")");
    }

    // Wave scheduling — fires each wave seconds apart based on config, then schedules the raid end callback
    private void spawnWaves(World world, Store<EntityStore> store, int cx, int cy, int cz, boolean spawnOutsideTerritory, RaidGroup group, RaidDefinition definition) {
        long firstWaveDelayMs = (long) ModConfig.get().raids.seconds_before_first_wave * 1000L;

        for (int waveIndex = 0; waveIndex < definition.waves.size(); waveIndex++) {
            final int wave = waveIndex;
            // first wave is delayed by seconds_before_first_wave, subsequent waves are offset from there
            final long delayMs = firstWaveDelayMs + ((long) wave * ModConfig.get().raids.seconds_between_waves * 1000L);

            // Schedule each wave on a background thread, then re-enter the world executor for the actual spawn
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

        // Schedule the raid end callback to fire after all waves plus the explosion delay
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

            // set leash point to the light well center so ReturnHome walks them toward the base
            npcEntity.saveLeashInformation(new Vector3d(cx, cy, cz), rotation);

            group.addNpc(npcRef);
        }

        // advance the wave counter on the HUD state so the HUD knows which wave just spawned
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

        // load the players base chunks into memory and keep them alive during the raid time
        if (group.territory != null) keepTerritoryChunksLoaded(group.world, group.territory, false);

        // clear the raid HUD state from the player component
        clearRaidHudState(store, group.targetPlayerRef);

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

        // if explosion feature is enabled, do the thing
        if (ModConfig.get().raids.unkilled_raid_enemies_explode) {
            // damage blocks in a sphere around the NPC
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

                            // break the block suppressing drops
                            BlockHarvestUtils.performBlockDamage(null, null, new Vector3i(x, y, z), null, null, null, false, ModConfig.get().raids.explosion_hit_damage_blocks, 2048 | 1024, chunkRef, store, chunkStore);
                        } catch (Exception e) {
                            System.err.println("[RaidSystem] Error breaking block during explosion: " + e.getMessage());
                        }
                    }
                }
            }

            // damage nearby players only — skip NPCs so they don't hurt each other or drop loot
            Damage.EnvironmentSource explosionSource = new Damage.EnvironmentSource("explosion");
            List<Ref<EntityStore>> nearby = TargetUtil.getAllEntitiesInBox(
                    new Vector3d(pos.x - ModConfig.get().raids.explosion_hit_radius_entities, pos.y - ModConfig.get().raids.explosion_hit_radius_entities, pos.z - ModConfig.get().raids.explosion_hit_radius_entities),
                    new Vector3d(pos.x + ModConfig.get().raids.explosion_hit_radius_entities, pos.y + ModConfig.get().raids.explosion_hit_radius_entities, pos.z + ModConfig.get().raids.explosion_hit_radius_entities),
                    store
            );
            for (Ref<EntityStore> targetRef : nearby) {
                if (!targetRef.isValid() || targetRef.equals(npcRef)) continue;
                // skip NPCs — only damage players
                if (store.getComponent(targetRef, NPCEntity.getComponentType()) != null) continue;
                try {
                    TransformComponent targetTransform = store.getComponent(targetRef, TransformComponent.getComponentType());
                    if (targetTransform == null) continue;
                    double distance = pos.distanceTo(targetTransform.getPosition());
                    if (distance > ModConfig.get().raids.explosion_hit_radius_entities) continue;
                    // scale damage by distance falloff
                    float damage = ModConfig.get().raids.explosion_hit_damage_entities * (1f - (float)(distance / ModConfig.get().raids.explosion_hit_radius_entities));
                    if (damage > 0) DamageSystems.executeDamage(targetRef, store, new Damage(explosionSource, DamageCause.getAssetMap().getAsset("Environment"), damage));
                } catch (Exception e) {
                    System.err.println("[RaidSystem] Error damaging entity during explosion: " + e.getMessage());
                }
            }
        }

        // remove the NPC cleanly without triggering death loot or animations
        store.removeEntity(npcRef, RemoveReason.REMOVE);
    }

    // Safe spawn position search
    private Vector3d findSafeSpawnPosition(World world, int cx, int cy, int cz, boolean spawnOutsideTerritory) {
        int radius = spawnOutsideTerritory ? SPAWN_RING_RADIUS : (SPAWN_RING_RADIUS / 2);

        // randomize the starting angle so spawns aren't always in the same direction
        double startAngle = random.nextDouble() * 2 * Math.PI;

        for (int attempt = 0; attempt < SPAWN_SEARCH_ATTEMPTS; attempt++) {
            // spread attempts evenly around the ring from a random starting angle
            double angle = startAngle + (2 * Math.PI / SPAWN_SEARCH_ATTEMPTS) * attempt;
            int offsetX = (int) Math.round(Math.cos(angle) * radius);
            int offsetZ = (int) Math.round(Math.sin(angle) * radius);

            int scanX = cx + offsetX;
            int scanZ = cz + offsetZ;

            // Scan downward from cy + offset to find solid ground
            int groundY = findGroundY(world, scanX, cy + SPAWN_SCAN_HEIGHT_OFFSET, scanZ);
            if (groundY != Integer.MIN_VALUE) {
                // Spawn 1 block above the found ground block
                return new Vector3d(scanX + 0.5, groundY + 1.0, scanZ + 0.5);
            }
        }

        // Fallback: spawn at a fixed offset from center at original cy — better than nothing
        System.err.println("[RaidSystem] Could not find safe spawn ground after " + SPAWN_SEARCH_ATTEMPTS + " attempts, using fallback position.");
        return new Vector3d(cx + radius + 0.5, cy + 1.0, cz + 0.5);
    }

    // Scan down from (x, startY, z) and returns the Y of the first solid non-fluid block, or Integer.MIN_VALUE if none is found before hitting y=0
    private int findGroundY(World world, int x, int startY, int z) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
        if (chunk == null) return Integer.MIN_VALUE; // chunk not loaded, skip

        int clampedStart = Math.min(startY, 319);
        for (int y = clampedStart; y >= 1; y--) {
            int blockId = chunk.getBlock(x, y, z);
            BlockType bt = BlockType.getAssetMap().getAsset(blockId);
            if (bt != null && RoomFloodFill.isStructural(bt)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    // Sets the raid HUD state on the player's RPG component so the HUD can display raid info
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

    // function to keep chunks ticking to keep them alive during raid
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
}