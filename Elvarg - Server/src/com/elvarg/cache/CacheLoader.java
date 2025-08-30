package com.elvarg.cache;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Minimal, dependency-free cache facade that keeps the rest of the code compiling.
 * - No external cache libraries required.
 * - Provides init() and getPreload(int) used by client/server loaders.
 *
 * Swap internals later without changing the public API.
 */
public final class CacheLoader {

    private volatile boolean initialized;
    private Path cacheRoot;

    /** Keep Elvarg's "new CacheLoader()" happy. */
    public CacheLoader() {
        this.initialized = false;
        this.cacheRoot = Paths.get(".").toAbsolutePath().normalize();
    }

    /** Called during startup. Safe to call multiple times. */
    public synchronized void init() {
        if (initialized) return;

        // Try a few common cache folders; fall back to project root if none exist.
        String[] candidates = {
                "data/cache",
                "cache",
                "data",
                "../data/cache"
        };
        for (String c : candidates) {
            Path p = Paths.get(c).toAbsolutePath().normalize();
            if (Files.isDirectory(p)) {
                cacheRoot = p;
                initialized = true;
                System.out.println("[CacheLoader] Using cache at: " + cacheRoot);
                return;
            }
        }

        initialized = true;
        System.out.println("[CacheLoader] No cache folder found; continuing with empty cache.");
    }

    /**
     * Optional "preload" binary fetch helper.
     * Looks for: <cacheRoot>/preload/{id}.bin
     * Returns EMPTY if not found.
     */
    public ByteBuf getPreload(int id) {
        try {
            Path p = cacheRoot.resolve("preload").resolve(id + ".bin");
            if (Files.isRegularFile(p)) {
                return Unpooled.wrappedBuffer(Files.readAllBytes(p));
            }
        } catch (Exception ignored) {
        }
        return Unpooled.EMPTY_BUFFER;
    }

    /** Expose the root if you need it. */
    public Path getCacheRoot() {
        return cacheRoot;
    }
}
