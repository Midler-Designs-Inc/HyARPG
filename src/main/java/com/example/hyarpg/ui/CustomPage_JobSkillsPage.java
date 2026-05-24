package com.example.hyarpg.ui;

// Hytale Imports
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

// Mod Imports
import com.example.hyarpg.components.Component_JobSkills;
import com.example.hyarpg.utils.jobs.JobSkill;
import com.example.hyarpg.utils.jobs.JobSkill_Mining;
import com.example.hyarpg.utils.jobs.JobSkill_Logging;

// Java Imports
import javax.annotation.Nonnull;
import java.util.List;

public class CustomPage_JobSkillsPage extends InteractiveCustomUIPage<CustomPage_JobSkillsPage.PageData> {

    // max perk rows pre-declared in the UI file
    private static final int MAX_PERK_ROWS = 20;

    // ordered list matching the nav slot indices 0-12 in the UI
    private static final List<JobSkill> ALL_JOBS = List.of(
//            JobSkill_Alchemy.INSTANCE,
//            JobSkill_Bartering.INSTANCE,
//            JobSkill_Beastmastery.INSTANCE,
//            JobSkill_Building.INSTANCE,
//            JobSkill_Cooking.INSTANCE,
//            JobSkill_Crafting.INSTANCE,
//            JobSkill_Exploring.INSTANCE,
//            JobSkill_Farming.INSTANCE,
//            JobSkill_Fishing.INSTANCE,
            JobSkill_Logging.INSTANCE,
            JobSkill_Mining.INSTANCE
//            JobSkill_Performing.INSTANCE,
//            JobSkill_Thievery.INSTANCE
    );

    // index of the currently selected job
    private int activeJobIndex = 0;

    public CustomPage_JobSkillsPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        // load the UI file
        cmd.append("CustomPage_JobSkillsPage.ui");

        // bind nav button clicks by index
        for (int i = 0; i < ALL_JOBS.size(); i++) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#JobNavBtn" + i, EventData.of("Action", "nav:" + i));
        }

        // push full initial state
        pushNavState(cmd, ref, store);
        pushPerkState(cmd, ref, store);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        // route incoming actions
        if (data.action == null)            { sendUpdate((UICommandBuilder) null, false); return; }
        if (data.action.startsWith("nav:")) { handleNav(Integer.parseInt(data.action.substring("nav:".length())), ref, store); }
        else                                { sendUpdate((UICommandBuilder) null, false); }
    }

    // switch active job and refresh nav highlight and perk list
    private void handleNav(int index, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        if (index < 0 || index >= ALL_JOBS.size()) { sendUpdate((UICommandBuilder) null, false); return; }
        UICommandBuilder cmd = new UICommandBuilder();
        this.activeJobIndex = index;
        pushNavState(cmd, ref, store);
        pushPerkState(cmd, ref, store);
        sendUpdate(cmd, false);
    }

    // push all nav slot state including labels, highlights, level and XP for every job
    private void pushNavState(@Nonnull UICommandBuilder cmd, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        Component_JobSkills jobSkills = getJobSkills(ref, store);
        if (jobSkills == null) return;

        for (int i = 0; i < ALL_JOBS.size(); i++) {
            String jobId = ALL_JOBS.get(i).getId();
            int jobXp = jobSkills.getXP(jobId);
            int jobLevel = jobSkills.calculateLevelFromXP(jobXp);
            int percent = (int)(jobSkills.calculateLevelProgress(jobXp) * 100);
            boolean isActive = i == activeJobIndex;

            cmd.set("#JobNavLabel"      + i + ".Text",    ALL_JOBS.get(i).getDisplayName());
            cmd.set("#JobNavHighlight"  + i + ".Visible", isActive);
            cmd.set("#JobNavLevelLabel" + i + ".Text",    "Lv." + jobLevel);
            cmd.set("#JobNavXpLabel" + i + ".Text", percent + "%");
        }

        cmd.set("#ActiveJobNameLabel.Text", "— " + ALL_JOBS.get(activeJobIndex).getDisplayName());
    }

    // push perk rows for the active job — unlocked rows bright, locked rows dimmed
    private void pushPerkState(@Nonnull UICommandBuilder cmd, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        Component_JobSkills jobSkills = getJobSkills(ref, store);
        if (jobSkills == null) return;

        JobSkill job   = ALL_JOBS.get(activeJobIndex);
        String jobId   = job.getId();
        int xp         = jobSkills.getXP(jobId);
        int level      = jobSkills.calculateLevelFromXP(xp);

        // push job description
        cmd.set("#JobDescriptionLabel.Text", job.getDescription());

        // populate perk rows — perks are already ordered by unlock level from the job definition
        List<JobSkill.JobPerk> perks = job.getPerks();
        for (int i = 0; i < MAX_PERK_ROWS; i++) {
            if (i < perks.size()) {
                JobSkill.JobPerk perk = perks.get(i);
                boolean unlocked      = level >= perk.unlockLevel();

                // dim text and badge when locked, full brightness when unlocked
                String nameColor    = unlocked ? "#dddddd" : "#555555";
                String descColor    = unlocked ? "#888888" : "#333333";
                String badgeColor   = unlocked ? "#c8a84b" : "#444444";

                cmd.set("#PerkRow"   + i + ".Visible",              true);
                cmd.set("#PerkName"  + i + ".Text",                 perk.displayName());
                cmd.set("#PerkName"  + i + ".Style.TextColor",      nameColor);
                cmd.set("#PerkDesc"  + i + ".Text",                 perk.description());
                cmd.set("#PerkDesc"  + i + ".Style.TextColor",      descColor);
                cmd.set("#PerkLevel" + i + ".Text",                 String.valueOf(perk.unlockLevel()));
                cmd.set("#PerkLevel" + i + ".Style.TextColor",      badgeColor);
            } else {
                // hide unused slots
                cmd.set("#PerkRow" + i + ".Visible", false);
            }
        }
    }

    private Component_JobSkills getJobSkills(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        return store.getComponent(ref, Component_JobSkills.getComponentType());
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec
                .<PageData>builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action).add()
                .build();

        public String action;
    }
}