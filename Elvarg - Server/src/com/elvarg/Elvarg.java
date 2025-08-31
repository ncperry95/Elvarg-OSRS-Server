package com.elvarg;

import com.elvarg.cache.CacheLoader;
import io.netty.buffer.ByteBuf;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Elvarg bootstrap – compile-first.
 *
 * Adds:
 *  - Static logger + getLogger()
 *  - Updating flag + isUpdating()
 *  - Static getFile(int,int) passthrough used by RegionClipping, etc.
 *  - Keeps definition/world loaders commented until APIs are aligned.
 */
public class Elvarg {

    private static final ExecutorService serviceLoader = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2)
    );

    // Simple logger for call sites like LoginDecoder
    private static final Logger LOGGER = Logger.getLogger(Elvarg.class.getName());

    // Server updating flag used by LoginResponses/World
    private static volatile boolean UPDATING = false;

    // Global cache access
    private static final CacheLoader CACHE = new CacheLoader();

    public static CacheLoader getCache() {
        return CACHE;
    }

    /** Expose logger for call sites. */
    public static Logger getLogger() {
        return LOGGER;
    }

    /** Expose updating flag for call sites. */
    public static boolean isUpdating() {
        return UPDATING;
    }

    /** Optional setter if you later want to flip maintenance mode. */
    public static void setUpdating(boolean updating) {
        UPDATING = updating;
    }

    /** Some call sites expect Elvarg.getFile(...). Provide a static passthrough. */
    public static ByteBuf getFile(int archive, int file) {
        return CACHE.getFile(archive, file);
    }

    /** Convenience used elsewhere too. */
    public static ByteBuf getCacheFile(int archive, int file) {
        return CACHE.getFile(archive, file);
    }

    public static void main(String[] args) {
        // Safe no-op (exists to satisfy calls in existing code)
        CACHE.init();

        // These caused “cannot find symbol” in your current tree.
        // Re-enable after wiring to real methods that exist in your codebase.
        //
        // serviceLoader.execute(() -> ItemDefinition.parseItems().load());
        // serviceLoader.execute(() -> NpcDefinition.parseNpcs().load());
        // serviceLoader.execute(() -> World.init());

        LOGGER.info("Elvarg bootstrap started.");
    }
}
