package com.elvarg.world.entity.impl.player;

import com.elvarg.Elvarg;
import com.elvarg.util.Misc;
import com.elvarg.world.model.container.impl.Bank;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

/**
 * Writes a player's state to disk as JSON.
 * Modernized to avoid deprecated wrapper constructors and to use try-with-resources.
 */
public final class PlayerSaving {

    private PlayerSaving() {
    }

    /**
     * Save the given player's state to: ./data/saves/characters/{name}.json
     */
    public static void save(Player player) {
        if (player == null || player.getUsername() == null) {
            return;
        }

        final String formatted = Misc.formatPlayerName(player.getUsername().toLowerCase());
        final Path path = Paths.get("./data/saves/characters").resolve(formatted + ".json");
        final File file = path.toFile();

        // Ensure directory exists
        File dir = file.getParentFile();
        if (dir != null && !dir.exists()) {
            try {
                dir.mkdirs();
            } catch (SecurityException e) {
                System.out.println("Unable to create directory for player data!");
            }
        }

        // Build JSON payload
        final JsonObject object = new JsonObject();
        object.addProperty("username", formatted);
        object.addProperty("display-name", player.getUsername());

        // --- Flags that previously used deprecated wrappers ---
        object.addProperty("auto-retaliate", player.getCombat().autoRetaliate());
        object.addProperty("xp-locked", player.experienceLocked());
        object.addProperty("preserve", player.isPreserveUnlocked());
        object.addProperty("rigour", player.isRigourUnlocked());
        object.addProperty("augury", player.isAuguryUnlocked());
        object.addProperty("has-veng", player.hasVengeance());
        object.addProperty("running", player.isRunning());

        // --- Timers / numbers (pass primitives directly) ---
        object.addProperty("last-veng", player.getVengeanceTimer().secondsRemaining());
        object.addProperty("run-energy", player.getRunEnergy());
        object.addProperty("spec-percentage", player.getSpecialPercentage());
        object.addProperty("recoil-damage", player.getRecoilDamage());
        object.addProperty("poison-damage", player.getPoisonDamage());

        object.addProperty("poison-immunity", player.getCombat().getPoisonImmunityTimer().secondsRemaining());
        object.addProperty("overload-timer", player.getOverloadTimer().secondsRemaining());
        object.addProperty("fire-immunity", player.getCombat().getFireImmunityTimer().secondsRemaining());
        object.addProperty("teleblock-timer", player.getCombat().getTeleBlockTimer().secondsRemaining());
        object.addProperty("prayer-block", player.getCombat().getPrayerBlockTimer().secondsRemaining());

        object.addProperty("skull-timer", player.getSkullTimer());
        object.addProperty("target-kills", player.getBountyHunter().getTargetKills());
        object.addProperty("normal-kills", player.getBountyHunter().getNormalKills());
        object.addProperty("deaths", player.getBountyHunter().getDeaths());
        object.addProperty("pkp", player.getPkp());

        // --- Banks / containers (keep behavior; serialize contents) ---
        // If you already have custom (de)serializers for Bank or Items, register them below.
        final Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        // Example: persist basic bank meta + slots (customize as needed)
        JsonObject banks = new JsonObject();
        for (int i = 0; i < player.getBanks().length; i++) {
            Bank bank = player.getBank(i);
            if (bank != null) {
                banks.add("tab-" + i, gson.toJsonTree(bank));
            }
        }
        object.add("banks", banks);

        // --- Location, stats, inventory, equipment, etc. ---
        // Add whatever your server expects here; leave placeholders if you have custom serializers.
        object.add("position", gson.toJsonTree(player.getLocation()));
        object.add("inventory", gson.toJsonTree(player.getInventory()));
        object.add("equipment", gson.toJsonTree(player.getEquipment()));
        object.add("appearance", gson.toJsonTree(player.getAppearance()));
        object.add("skills", gson.toJsonTree(player.getSkillManager()));
        object.add("prayer", gson.toJsonTree(player.getPrayerManager()));
        object.add("slayer", gson.toJsonTree(player.getSlayer()));
        object.add("achievements", gson.toJsonTree(player.getAchievements()));
        object.add("attributes", gson.toJsonTree(player.getAttributes()));

        // --- Finally, write the file ---
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(gson.toJson(object));
        } catch (Exception e) {
            Elvarg.getLogger().log(Level.WARNING, "An error has occurred while saving a character file!", e);
        }
    }

    /**
     * @return true if a character file already exists for the provided name.
     */
    public static boolean playerExists(String name) {
        if (name == null) return false;
        String formatted = Misc.formatPlayerName(name.toLowerCase());
        return new File("./data/saves/characters/" + formatted + ".json").exists();
    }
}
