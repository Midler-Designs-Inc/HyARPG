package com.example.hyarpg.utils.combat;

// Hytale Imports
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Java Imports
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SwingDamageGroup {
    public final Ref<EntityStore> attacker;
    public final Ref<EntityStore> defender;
    public final long timestamp = System.currentTimeMillis();
    public final boolean blocked;
    public final boolean isProjectile;
    private final ConcurrentHashMap<DamageCause, Float> totals = new ConcurrentHashMap<>();
    public volatile boolean readyToApply = false;

    public SwingDamageGroup(Ref<EntityStore> attacker, Ref<EntityStore> defender, boolean blocked, boolean isProjectile) {
        this.attacker = attacker;
        this.defender = defender;
        this.blocked = blocked;
        this.isProjectile = isProjectile;
    }

    public void add(DamageCause cause, float amount) {
        totals.merge(cause, amount, Float::sum);
    }

    // Returns [{cause, totalAmount}]
    public Collection<Map.Entry<DamageCause, Float>> packets() {
        return totals.entrySet();
    }
}