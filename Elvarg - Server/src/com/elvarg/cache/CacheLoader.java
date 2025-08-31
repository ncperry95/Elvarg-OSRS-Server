package com.elvarg.cache;

import com.elvarg.cache.impl.CacheArchive;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import jaggrab.net.FileDescriptor;

/**
 * CacheLoader – compile-first shim.
 * 
 * NOTE:
 * - Methods currently return empty buffers/null so the project compiles.
 * - After we’re green, wire these to your real cache backend and CRC table.
 */
public class CacheLoader {

    /** Optional init hook; currently a no-op so existing calls compile. */
    public void init() {
        // TODO: initialize your real cache backend if/when needed.
    }

    /**
     * Return the archive type expected elsewhere in the codebase.
     * TODO: Wire to real cache decoding if required by your loaders, e.g.:
     *   ByteBuf buf = getFile(archiveId, someFileId);
     *   return CacheArchive.decode(buf);
     */
    public CacheArchive getArchive(int archiveId) {
        // Compile-safe placeholder; callers should null-check until wired.
        return null;
    }

    /** Primary cache accessor by (archive, file). */
    public ByteBuf getFile(int archive, int file) {
        // TODO: return actual data from cache
        return Unpooled.buffer(0, 0);
    }

    /** Overload used by OnDemand workers via FileDescriptor. */
    public ByteBuf getFile(FileDescriptor desc) {
        if (desc == null) {
            return Unpooled.buffer(0, 0);
        }
        return getFile(desc.getContainer(), desc.getFile());
    }

    /**
     * In this tree, ResourceRequester expects a ByteBuf here (NOT int[]).
     * If you later want int[], we can adjust callers and update this method.
     */
    public ByteBuf getCrcTable() {
        // TODO: build and return real CRC table bytes
        return Unpooled.buffer(0, 0);
    }

    /** Optional preload hook. */
    public ByteBuf getPreloadFile(int index) {
        // TODO: return actual preload bytes
        return Unpooled.buffer(0, 0);
    }
}
