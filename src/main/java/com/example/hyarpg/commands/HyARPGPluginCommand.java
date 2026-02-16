package com.example.hyarpg.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/**
 * Main command for HyARPG plugin.
 *
 * Usage:
 * - /hya help - Show available commands
 * - /hya info - Show plugin information
 * - /hya reload - Reload plugin configuration
 * - /hya ui - Open the plugin dashboard
 */
public class HyARPGPluginCommand extends AbstractCommandCollection {

    public HyARPGPluginCommand() {
        super("hya", "HyARPG plugin commands");

        // Add subcommands
        this.addSubCommand(new HelpSubCommand());
        this.addSubCommand(new InfoSubCommand());
        this.addSubCommand(new ReloadSubCommand());
        this.addSubCommand(new UISubCommand());
    }

    @Override
    protected boolean canGeneratePermission() {
        return false; // No permission required for base command
    }
}