package com.runescape;

import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AppletStub;
import java.applet.AudioClip;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;

public final class ClientLauncher {

    // ---------- tiny reflect helpers ----------
    private static Field findField(Class<?> c, String name) throws NoSuchFieldException {
        Class<?> k = c;
        while (k != null) {
            try {
                Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                k = k.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static void hardSet(Object o, String field, Object value) {
        try {
            Field f = findField(o.getClass(), field);
            f.set(o, value);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static boolean trySet(Object o, String field, Object value) {
        try {
            Field f = findField(o.getClass(), field);
            f.set(o, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object get(Object o, String field) {
        try {
            Field f = findField(o.getClass(), field);
            return f.get(o);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void call(Object o, String method, Class<?>[] sig, Object... args) {
        try {
            Method m = null;
            Class<?> k = o.getClass();
            while (k != null) {
                try {
                    m = k.getDeclaredMethod(method, sig);
                    break;
                } catch (NoSuchMethodException e) {
                    k = k.getSuperclass();
                }
            }
            if (m == null) throw new NoSuchMethodException(method);
            m.setAccessible(true);
            m.invoke(o, args);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    // ------------------------------------------

    private static Object newRSCanvas(Object client, int w, int h) throws Exception {
        Class<?> rsCanvasCls = Class.forName("com.runescape.engine.Canvas");
        Constructor<?>[] ctors = rsCanvasCls.getDeclaredConstructors();
        for (Constructor<?> ctor : ctors) {
            ctor.setAccessible(true);
            Class<?>[] params = ctor.getParameterTypes();
            try {
                Object c;
                if (params.length == 0) {
                    c = ctor.newInstance();
                } else if (params.length == 1) {
                    if (params[0].isAssignableFrom(client.getClass())
                        || "com.runescape.engine.GameEngine".equals(params[0].getName())) {
                        c = ctor.newInstance(client);
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
                if (c instanceof Component) {
                    ((Component) c).setPreferredSize(new Dimension(w, h));
                }
                return c;
            } catch (Throwable ignore) {
                // try next ctor
            }
        }
        throw new NoSuchMethodException("No usable com.runescape.engine.Canvas constructor found");
    }

    private static boolean installNoopCallbacks(Object client) {
        try {
            Class<?> callbacksIfc = Class.forName("net.runelite.api.hooks.Callbacks");

            Object proxy = Proxy.newProxyInstance(
                callbacksIfc.getClassLoader(),
                new Class[]{ callbacksIfc },
                (p, m, a) -> {
                    Class<?> rt = m.getReturnType();
                    if (rt == Void.TYPE) return null;
                    if (a != null && a.length == 1 && a[0] != null && rt.isInstance(a[0])) {
                        return a[0]; // pass event through unchanged
                    }
                    if (rt.isPrimitive()) {
                        if (rt == Boolean.TYPE) return false;
                        if (rt == Byte.TYPE)    return (byte)0;
                        if (rt == Short.TYPE)   return (short)0;
                        if (rt == Integer.TYPE) return 0;
                        if (rt == Long.TYPE)    return 0L;
                        if (rt == Float.TYPE)   return 0f;
                        if (rt == Double.TYPE)  return 0d;
                        if (rt == Character.TYPE) return '\0';
                    }
                    return null;
                }
            );

            for (Method m : client.getClass().getMethods()) {
                if (m.getName().equals("setCallbacks")
                    && m.getParameterCount() == 1
                    && callbacksIfc.isAssignableFrom(m.getParameterTypes()[0])) {
                    m.invoke(client, proxy);
                    System.out.println("[Launcher] Installed Callbacks via setter");
                    return true;
                }
            }

            Class<?> k = client.getClass();
            while (k != null) {
                for (Field f : k.getDeclaredFields()) {
                    if (callbacksIfc.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        f.set(client, proxy);
                        System.out.println("[Launcher] Installed Callbacks via field: " + f.getName());
                        return true;
                    }
                }
                k = k.getSuperclass();
            }

            System.out.println("[Launcher] Could not find a Callbacks setter/field.");
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return false;
    }

    // Initialize the loader font + metrics so drawInitial() doesn't NPE
    private static void ensureLoginFontMetrics(Object client, Component canvas) {
        try {
            // Use any existing font if present; otherwise create one
            Font font = null;
            Object fObj = get(client, "loginScreenFont");
            if (fObj instanceof Font) font = (Font) fObj;
            if (font == null) font = new Font("Helvetica", Font.BOLD, 13);

            // Seed metrics
            FontMetrics fm = canvas.getFontMetrics(font);

            // Try instance field first
            boolean set = trySet(client, "loginScreenFontMetrics", fm);

            // If that didn't work, try to set it on the class (in case it's static)
            if (!set) {
                try {
                    Class<?> ge = Class.forName("com.runescape.engine.GameEngine");
                    Field fld = ge.getDeclaredField("loginScreenFontMetrics");
                    fld.setAccessible(true);
                    try {
                        fld.set(client, fm); // instance-style first
                    } catch (IllegalArgumentException iae) {
                        fld.set(null, fm);   // static fallback
                    }
                    set = true;
                } catch (Throwable ignored) { }
            }

            // Optional fields; ignore if missing (no stack traces)
            trySet(client, "loginScreenFont", font);
            trySet(client, "loginScreenFontBold", font);
            trySet(client, "loginScreenFontRegular", new Font("Helvetica", Font.PLAIN, 13));
        } catch (Throwable t) {
            System.out.println("[Launcher] ensureLoginFontMetrics() warning:");
            t.printStackTrace();
        }
    }

    private static void tryDrawLoadingOnce(Object client) {
        // Best-effort; ignore failures
        try {
            // Common RSPS signature: drawLoadingText(int percent, String text)
            call(client, "drawLoadingText", new Class[]{int.class, String.class}, 1, "Starting...");
        } catch (Throwable ignored) {
            try {
                // Some clients: drawLoadingText(String text, int percent)
                call(client, "drawLoadingText", new Class[]{String.class, int.class}, "Starting...", 1);
            } catch (Throwable ignored2) { }
        }
    }

    private static void installAppletStub(Applet applet) {
        try {
            AppletStub stub = new AppletStub() {
                @Override public boolean isActive() { return true; }
                @Override public URL getDocumentBase() { try { return new URL("http://localhost/"); } catch (Exception e){ return null; } }
                @Override public URL getCodeBase() { try { return new URL("http://localhost/"); } catch (Exception e){ return null; } }
                @Override public String getParameter(String name) { return null; }
                @Override public AppletContext getAppletContext() {
                    return new AppletContext() {
                        @Override public AudioClip getAudioClip(URL url) { return null; }
                        @Override public java.awt.Image getImage(URL url) { return null; }
                        @Override public Applet getApplet(String name) { return null; }
                        @Override public Enumeration<Applet> getApplets() { return Collections.emptyEnumeration(); }
                        @Override public void showDocument(URL url) {}
                        @Override public void showDocument(URL url, String target) {}
                        @Override public void showStatus(String status) {}
                        @Override public void setStream(String key, java.io.InputStream stream) {}
                        @Override public java.io.InputStream getStream(String key) { return null; }
                        @Override public Iterator<String> getStreamKeys() { return Collections.<String>emptyList().iterator(); }
                    };
                }
                @Override public void appletResize(int width, int height) {}
            };

            Method setStub = Applet.class.getDeclaredMethod("setStub", AppletStub.class);
            setStub.setAccessible(true);
            setStub.invoke(applet, stub);
        } catch (Throwable t) {
            // Non-fatal; continue.
        }
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                final int W = 765, H = 503;

                // Create the client
                Client client = new Client();

                // Host AWT frame
                Frame frame = new Frame("Elvarg");
                frame.setLayout(new BorderLayout());
                frame.addWindowListener(new WindowAdapter() {
                    @Override public void windowClosing(WindowEvent e) { System.exit(0); }
                });

                // Create the RuneScape engine Canvas (must be com.runescape.engine.Canvas)
                Component rsCanvas = (Component) newRSCanvas(client, W, H);

                frame.add(rsCanvas, BorderLayout.CENTER);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                rsCanvas.requestFocus();

                // Wire GameEngine internals
                hardSet(client, "frame", frame);     // java.awt.Frame
                hardSet(client, "canvas", rsCanvas); // com.runescape.engine.Canvas
// No-op RL callbacks that *pass events through*
                installNoopCallbacks(client);
                // Ensure GameEngine can reach callbacks via Client.instance
                try {
                    Class<?> cls = Class.forName("com.runescape.Client");
                    java.lang.reflect.Field inst = cls.getDeclaredField("instance");
                    inst.setAccessible(true);
                    inst.set(null, client);
                    System.out.println("[Launcher] Client.instance set");
                // Ensure SignLink knows the cache dir & opens main_file_cache.* before createArchive()
                try {
                    Class<?> sl = Class.forName("com.runescape.sign.SignLink");
                    java.lang.reflect.Method m = sl.getDeclaredMethod("init", java.applet.Applet.class);
                    m.setAccessible(true);
                    m.invoke(null, (java.applet.Applet) client);
                    System.out.println("[Launcher] SignLink.init() invoked");
                } catch (Throwable t) {
                    System.out.println("[Launcher] SignLink.init() failed: " + t);
                }
                // Force CacheDownloader to use our canonical dir
                try {
                    java.io.File dir = new java.io.File(System.getProperty("user.home"), "Cache\\cache");
                    Class<?> cd = Class.forName("com.runescape.CacheDownloader");
                    boolean redirected = false;

                    for (java.lang.reflect.Field f : cd.getDeclaredFields()) {
                        if (java.io.File.class.isAssignableFrom(f.getType())
                            && f.getName().toLowerCase().contains("cache")) {
                            f.setAccessible(true);
                            try { f.set(null, dir); redirected = true; } catch (IllegalArgumentException __) {}
                        }
                    }
                    if (!redirected) {
                        for (java.lang.reflect.Field f : cd.getDeclaredFields()) {
                            if (f.getType() == String.class
                                && f.getName().toLowerCase().contains("cache")) {
                                f.setAccessible(true);
                                try { f.set(null, dir.getAbsolutePath()); redirected = true; } catch (IllegalArgumentException __) {}
                            }
                        }
                    }
                    System.out.println(redirected
                        ? "[Launcher] Redirected CacheDownloader cache path -> " + dir.getAbsolutePath()
                        : "[Launcher] Did not find a cache path field to redirect.");
                } catch (Throwable t) { t.printStackTrace(); }
                // Pre-size canvas & request replace before addCanvas
                call(client, "setMaxCanvasWidth",  new Class[]{int.class}, W);
                call(client, "setMaxCanvasHeight", new Class[]{int.class}, H);
                call(client, "setReplaceCanvasNextFrame", new Class[]{boolean.class}, true);
                call(client, "setResizeCanvasNextFrame",  new Class[]{boolean.class}, true);
                } catch (Throwable __ignored) {}

                // Now it is safe to force-create engine buffers & input hooks
                try {
                    call(client, "addCanvas", new Class[]{});
                    System.out.println("[Launcher] addCanvas() invoked");
                } catch (Throwable __ignored) {}

                // Pre-seed loader font/metrics to avoid NPE in drawInitial()
                ensureLoginFontMetrics(client, rsCanvas);

                // Avoid Applet error path NPEs
                if (client instanceof Applet) {
                    installAppletStub((Applet) client);
                }

                // Kick lifecycle
                try { client.getClass().getMethod("init").invoke(client); } catch (NoSuchMethodException ignored) {}
                try { client.getClass().getMethod("start").invoke(client); } catch (NoSuchMethodException ignored) {}

                // Nudge a first draw so something appears before the cache finishes
                                // Defer first draw until rasterProvider exists (avoid NPE)
                try {
                    for (int i = 0; i < 60; i++) {
                        if (get(client, "rasterProvider") != null) { ; break; }
                        Thread.sleep(50);
                    }
                } catch (Throwable ignored) {}

                // If the engine didn't start a thread in init(), start one
                try {
                    call(client, "startRunnable", new Class[]{Runnable.class, int.class}, client, 1);
                } catch (Throwable ignored) {
                    if (client instanceof Runnable) {
                        new Thread(client, "GameEngine").start();
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }
}







