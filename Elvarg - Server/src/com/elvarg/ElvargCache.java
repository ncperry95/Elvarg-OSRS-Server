package com.elvarg;

import com.elvarg.cache.CacheLoader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

final class ElvargCache {

    private static volatile CacheLoader CACHE;

    static CacheLoader getCache() {
        if (CACHE == null) {
            synchronized (ElvargCache.class) {
                if (CACHE == null) {
                    CACHE = bootstrapCache();
                }
            }
        }
        return CACHE;
    }

    static ByteBuf getFile(int indexId, int fileId) {
        try {
            CacheLoader c = getCache();
            if (c == null) return Unpooled.EMPTY_BUFFER;

            // Prefer getFile(int,int) if it exists
            Method m = findMethod(c.getClass(), "getFile", int.class, int.class);
            if (m != null) {
                Object r = m.invoke(c, indexId, fileId);
                if (r instanceof ByteBuf) return (ByteBuf) r;
                if (r instanceof byte[]) return Unpooled.wrappedBuffer((byte[]) r);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return Unpooled.EMPTY_BUFFER;
    }

    private static CacheLoader bootstrapCache() {
        // Allow override with -Delvarg.cachePath=C:\path\to\cache
        String override = System.getProperty("elvarg.cachePath");
        List<File> candidates = Arrays.asList(
                override == null ? null : new File(override),
                new File(System.getProperty("user.home"), "Cache"),
                new File(System.getProperty("user.home"), "cache"),
                new File("Cache/cache"),
                new File("../Elvarg-Client-Public-master/Elvarg-Client-Public-master/Cache/cache")
        );

        for (File dir : candidates) {
            if (dir == null) continue;
            File dat2 = new File(dir, "main_file_cache.dat2");
            if (!dat2.exists()) continue;

            try {
                // Try a static open(File) if present
                Method open = findStaticMethod(CacheLoader.class, "open", File.class);
                if (open != null) {
                    return (CacheLoader) open.invoke(null, dir);
                }

                // Otherwise try constructors we can satisfy
                for (Constructor<?> ctor : CacheLoader.class.getConstructors()) {
                    Class<?>[] p = ctor.getParameterTypes();
                    Object instance = null;

                    if (p.length == 1 && p[0] == File.class) {
                        instance = ctor.newInstance(dir);
                    } else if (p.length == 1 && p[0].getName().equals("java.nio.file.Path")) {
                        instance = ctor.newInstance(dir.toPath());
                    } else if (p.length == 1 && p[0] == String.class) {
                        instance = ctor.newInstance(dir.getAbsolutePath());
                    } else if (p.length == 0) {
                        instance = ctor.newInstance();
                    }

                    if (instance != null) {
                        // Best-effort setter if needed by this CacheLoader
                        tryInvoke(instance, "setPath", File.class, dir);
                        tryInvoke(instance, "setDirectory", File.class, dir);
                        return (CacheLoader) instance;
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        System.err.println("[ElvargCache] WARNING: Could not locate OSRS cache. " +
                "Set -Delvarg.cachePath=C:\\path\\to\\cache");
        return null;
    }

    private static Method findMethod(Class<?> c, String name, Class<?>... params) {
        try { return c.getMethod(name, params); } catch (NoSuchMethodException e) { return null; }
    }

    private static Method findStaticMethod(Class<?> c, String name, Class<?>... params) {
        Method m = findMethod(c, name, params);
        return (m != null && java.lang.reflect.Modifier.isStatic(m.getModifiers())) ? m : null;
    }

    private static void tryInvoke(Object instance, String name, Class<?> paramType, Object arg) {
        try {
            Method m = instance.getClass().getMethod(name, paramType);
            m.invoke(instance, arg);
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private ElvargCache() {}
}
