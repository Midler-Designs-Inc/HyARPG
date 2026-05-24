package com.example.hyarpg.ui;

// Hytale Imports
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

// Java Imports
import javax.annotation.Nonnull;

public class CustomHUD_Player extends CustomUIHud {

    public CustomHUD_Player(@Nonnull PlayerRef playerRef) {
        super(playerRef, "HyARPG_HUD");
    }

    // load the UI layout file — called once by show() when the HUD is first registered
    @Override
    protected void build(@Nonnull UICommandBuilder cmd) {
        cmd.append("HUD/CustomHUD_Player.ui");
    }

    // push a delta update to the client — only patches changed properties, no teardown
    public void pushUpdate(@Nonnull HudState state) {
        UICommandBuilder cmd = new UICommandBuilder();

        // level bar and stat summary label above the hotbar
        cmd.set("#xpBar.Value", state.levelPercent);
        cmd.set("#xpLevelCurrent.Text", "GS " + state.gearScore + "  |  Lv " + state.playerLevel);

        // resource bars flanking the hotbar
        cmd.set("#thirstBar.Value", state.thirstPercent);
        cmd.set("#hungerBar.Value", state.hungerPercent);
        cmd.set("#barrierBar.Value", state.barrierOnBlockPercent);

        // skill slot icons — hidden when no ability is assigned to that slot
        cmd.set("#skillIconQ.Background", state.ultimateAbilityIcon == null ? "" : "Skills/Icons/" + state.ultimateAbilityIcon);
        cmd.set("#skillIconQ.Visible", state.ultimateAbilityIcon != null);
        cmd.set("#skillIconE.Background", state.primaryAbilityIcon == null ? "" : "Skills/Icons/" + state.primaryAbilityIcon);
        cmd.set("#skillIconE.Visible", state.primaryAbilityIcon != null);
        cmd.set("#skillIconR.Background", state.secondaryAbilityIcon == null ? "" : "Skills/Icons/" + state.secondaryAbilityIcon);
        cmd.set("#skillIconR.Visible", state.secondaryAbilityIcon != null);

        // cooldown overlays — dim the slot and show remaining seconds when an ability is on cooldown
        cmd.set("#skillIconOverlayE.Visible", state.secondsLeft_E > 0);
        cmd.set("#skillSlotOverlayLabelE.Visible", state.secondsLeft_E > 0);
        cmd.set("#skillSlotOverlayLabelE.Text", String.valueOf(state.secondsLeft_E));
        cmd.set("#skillIconOverlayR.Visible", state.secondsLeft_R > 0);
        cmd.set("#skillSlotOverlayLabelR.Visible", state.secondsLeft_R > 0);
        cmd.set("#skillSlotOverlayLabelR.Text", String.valueOf(state.secondsLeft_R));

        // assassin mark stack count shown to the left of the hotbar
        cmd.set("#assassinMarkIcon.Visible", state.assassinMarkCount > 0);
        cmd.set("#assassinMarkCount.Visible", state.assassinMarkCount > 0);
        cmd.set("#assassinMarkCount.Text", String.valueOf(state.assassinMarkCount));

        // world tier and estimated enemy level in the bottom left corner
        cmd.set("#worldTierLabel.Text", "World Tier " + state.worldTier);
        cmd.set("#worldTierEnemyLevel.Text", "Avg Enemy Lv " + state.avgEnemyLevel);

        // room label shown top center when the player is inside a claimed territory
        cmd.set("#currentRoomBorder.Visible", state.showRoomInfo);
        cmd.set("#currentRoom.Visible", state.showRoomInfo);
        if (state.showRoomInfo) cmd.set("#currentRoom.Text", state.roomText);

        // raid panel shown on the left during an active raid — hidden entirely otherwise
        cmd.set("#raidHudIcon.Visible", state.raidActive);
        cmd.set("#raidHudWaveStatus.Visible", state.raidActive);
        cmd.set("#raidHudCountdown.Visible", state.raidActive);
        cmd.set("#raidHudRemainingEnemies.Visible", state.raidActive);
        cmd.set("#raidHudExplosionWarning.Visible", state.raidActive && state.showExplosionWarning);
        if (state.raidActive) {
            cmd.set("#raidHudWaveStatus.Text", state.raidWaveStatus);
            cmd.set("#raidHudCountdown.Text", state.raidCountdown);
            cmd.set("#raidHudRemainingEnemies.Text", state.raidEnemiesRemaining + " Enemies Remain");
        }

        update(false, cmd);
    }

    // all HUD values captured in a single snapshot on the world thread each tick
    public static final class HudState {
        // resource bars
        public float thirstPercent;
        public float hungerPercent;
        public float barrierOnBlockPercent;

        // level and progression
        public float levelPercent;
        public int playerLevel;
        public int gearScore;
        public int skillPoints;

        // ability slots and cooldowns
        public String ultimateAbilityIcon;
        public String primaryAbilityIcon;
        public String secondaryAbilityIcon;
        public int secondsLeft_E;
        public int secondsLeft_R;

        // debuff stacks
        public int assassinMarkCount;

        // world position context
        public int worldTier;
        public int avgEnemyLevel;

        // territory and room context
        public boolean showRoomInfo;
        public String roomText = "";

        // raid state
        public boolean raidActive;
        public boolean showExplosionWarning;
        public String raidWaveStatus = "";
        public String raidCountdown = "";
        public int raidEnemiesRemaining;
    }
}