package net.runelite.client;

import java.io.File;

public final class RuneLite {
    public static final File RUNELITE_DIR = new File(System.getProperty("user.home"), ".runelite");
    public static final File CACHE_DIR    = new File(RUNELITE_DIR, "cache");
    public static final String VERSION    = "stub";

    public static void main(String[] args) {
        RUNELITE_DIR.mkdirs();
        CACHE_DIR.mkdirs();
        System.out.println("RuneLite stub main ran. Cache: " + CACHE_DIR.getAbsolutePath());
    }
}
