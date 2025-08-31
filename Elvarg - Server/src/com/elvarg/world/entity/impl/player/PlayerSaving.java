package com.elvarg.world.entity.impl.player;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * PlayerSaving – compile-first version.
 *
 * Changes:
 *  - Serialize the whole Location object instead of x/y/z primitives,
 *    because your Location API isn't exposing numeric getters that match.
 *  - Keep the four optional sections commented out until the Player API is wired.
 */
public class PlayerSaving {

    private static final Gson gson = new Gson();

    public static JsonObject save(Player player) {
        final JsonObject object = new JsonObject();

        object.addProperty("username", player.getUsername());
        object.addProperty("rights", player.getRights().name());

        // Store as nested object to avoid calling getX()/getY()/getZ().
        object.add("location", gson.toJsonTree(player.getLocation()));

        // TODO: Re-enable once these exist on Player
        // object.add("prayer", gson.toJsonTree(player.getPrayerManager()));
        // object.add("slayer", gson.toJsonTree(player.getSlayer()));
        // object.add("achievements", gson.toJsonTree(player.getAchievements()));
        // object.add("attributes", gson.toJsonTree(player.getAttributes()));

        return object;
    }
}
