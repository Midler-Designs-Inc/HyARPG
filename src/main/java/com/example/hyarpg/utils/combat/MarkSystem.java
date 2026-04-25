package com.example.hyarpg.utils.combat;

// Hytale Mods
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Java Mod
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class MarkSystem {

    // Enum list of mark types
    private enum MarkType {
        ASSASSIN (10, 10_000L);

        final int      maxStacks;
        final Long     durationMs;

        MarkType(int maxStacks, Long durationMs) {
            this.maxStacks      = maxStacks;
            this.durationMs     = durationMs;
        }
    }

    // Stack class for internal use
    private static final class Stack {
        private final MarkType type;
        private final ConcurrentLinkedDeque<Long> timestamps = new ConcurrentLinkedDeque<>();

        Stack(MarkType type) { this.type = type; }

        int pruneAndCount() {
            if (type.durationMs != null) {
                long cutoff = System.nanoTime() - type.durationMs * 1_000_000L;
                timestamps.removeIf(ts -> ts < cutoff);
            }
            return timestamps.size();
        }

        void add(int count) {
            pruneAndCount();
            for (int i = 0; i < count; i++) {
                if (timestamps.size() >= type.maxStacks)
                    timestamps.pollFirst(); // evict oldest to make room
                timestamps.addLast(System.nanoTime());
            }
        }

        void reset(int count) {
            timestamps.clear();
            add(count);
        }

        void clear() { timestamps.clear(); }
    }

    // mark stacks and last hit target props
    private final Map<MarkType, Stack> stacks = new ConcurrentHashMap<>();
    private Ref<EntityStore> lastHitTarget = null;

    public MarkSystem() {
        for (MarkType type : MarkType.values())
            stacks.put(type, new Stack(type));
    }

    // Handles mark logic for when the owning entity hits a target
    public void onHit(Ref<EntityStore> target, Map<String, Integer> markCounts) {
        if (target == null) return;
        boolean newTarget = lastHitTarget == null || !lastHitTarget.equals(target);
        lastHitTarget = target;

        for (Map.Entry<String, Integer> entry : markCounts.entrySet()) {
            MarkType type = resolveType(entry.getKey());
            if (type == null || entry.getValue() <= 0) continue;

            Stack stack = stacks.get(type);
            if (newTarget) stack.reset(entry.getValue());
            else stack.add(entry.getValue());
        }
    }

    // Prunes expired marks and clears everything if the last hit target despawned.
    public void tick() {
        if (lastHitTarget != null && !lastHitTarget.isValid()) {
            clearAll();
            lastHitTarget = null;
            return;
        }
        for (Stack stack : stacks.values())
            stack.pruneAndCount();
    }

    // Clears all mark stacks without touching lastHitTarget.
    public void clearAll() {
        stacks.values().forEach(Stack::clear);
    }

    // Clears a specific mark type by name.
    public void clear(String markName) {
        Stack stack = stacks.get(resolveType(markName));
        if (stack != null) stack.clear();
    }

    // Returns the live stack count for a specific mark type.
    public int count(String markName) {
        Stack stack = stacks.get(resolveType(markName));
        return stack != null ? stack.pruneAndCount() : 0;
    }

    // Removes up to n stacks of the given mark type, oldest first. Returns count actually removed.
    public int consume(String markName, int count) {
        Stack stack = stacks.get(resolveType(markName));
        if (stack == null) return 0;
        stack.pruneAndCount();
        int removed = 0;
        for (int i = 0; i < count && !stack.timestamps.isEmpty(); i++) {
            stack.timestamps.pollFirst(); // oldest first
            removed++;
        }
        return removed;
    }

    // get the last target hit by the owning entity
    public Ref<EntityStore> getLastHitTarget() { return lastHitTarget; }

    // get enum type from passed string (caps the string to search for enums)
    private MarkType resolveType(String name) {
        try { return MarkType.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}