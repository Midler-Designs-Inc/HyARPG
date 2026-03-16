package com.example.hyarpg.utils.skills;

import com.example.hyarpg.components.Component_RPG_Player;

import java.util.Map;

public class SkillLibraryMigration {

    /**
     * Compares the player's saved SkillLibrary against the current live library.
     * Refunds and replaces at the library, tree, or node level depending on
     * what version has changed. Refunded points are returned to the player.
     *
     * @param comp    The player component holding the saved skillLibrary
     * @param current A fresh SkillLibrary instance with all latest trees registered
     */
    public static void migrate(Component_RPG_Player comp, SkillLibrary current) {
        // If the player has no library yet, just assign the current one and bail
        if (comp.skillLibrary == null) {
            comp.skillLibrary = current;
            return;
        }

        // Store ref to the comp library to a var
        SkillLibrary saved = comp.skillLibrary;

        // Library-level version check, if the whole library version changed, refund everything and replace
        if (!saved.getVersion().equals(current.getVersion())) {
            comp.skillPoints += saved.refund(comp);
            comp.skillLibrary = current;
            return;
        }

        // Tree-level version check, loop over trees check versions
        for (Map.Entry<String, SkillTree> currentEntry : current.getRegistry().entrySet()) {
            String treeId = currentEntry.getKey();
            SkillTree currentTree = currentEntry.getValue();
            SkillTree savedTree = saved.getRegistry().get(treeId);

            // Tree exists in current but not in saved — it's new, just add it
            if (savedTree == null) {
                saved.getRegistry().put(treeId, currentTree);
                continue;
            }

            // Tree version changed — refund and replace the whole tree
            if (!savedTree.getVersion().equals(currentTree.getVersion())) {
                comp.skillPoints += savedTree.refund(comp);
                saved.getRegistry().put(treeId, currentTree);
                continue;
            }

            // Node-level version check, loop over nodes and check versions
            for (Map.Entry<String, SkillNode> nodeEntry : currentTree.getNodes().entrySet()) {
                String nodeId = nodeEntry.getKey();
                SkillNode currentNode = nodeEntry.getValue();
                SkillNode savedNode = savedTree.getNodes().get(nodeId);

                // Node is new — add it (no points to refund)
                if (savedNode == null) {
                    savedTree.getNodes().put(nodeId, currentNode);
                    continue;
                }

                // Node version changed — refund and replace
                if (!savedNode.getVersion().equals(currentNode.getVersion())) {
                    comp.skillPoints += savedNode.refund(comp);
                    savedTree.getNodes().put(nodeId, currentNode);
                }
            }

            // Remove nodes from saved tree that no longer exist in current — refund first
            savedTree.getNodes().entrySet().removeIf(entry -> {
                if (!currentTree.getNodes().containsKey(entry.getKey())) {
                    comp.skillPoints += entry.getValue().refund(comp);
                    return true;
                }
                return false;
            });
        }

        // Remove trees from saved library that no longer exist in current — refund first
        saved.getRegistry().entrySet().removeIf(entry -> {
            if (!current.getRegistry().containsKey(entry.getKey())) {
                comp.skillPoints += entry.getValue().refund(comp);
                return true;
            }
            return false;
        });
    }
}