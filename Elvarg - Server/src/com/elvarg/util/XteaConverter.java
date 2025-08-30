package com.elvarg.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;


import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * XteaConverter
 *
 * Converts XTEA sources into Elvarg-friendly outputs.
 *
 * Supported inputs:
 *  1) OpenRS2 cache-specific keys.json (RECOMMENDED):
 *     https://archive.openrs2.org/caches/runescape/<cacheId>/keys.json
 *     Objects look like:
 *     {
 *       "archive": 5,
 *       "group": 1,
 *       "name_hash": -1153472937,
 *       "name": "l40_55",
 *       "mapsquare": 10295,
 *       "key": [-1920480496,-1423914110,951774544,-1419269290]
 *     }
 *
 *  2) Legacy validated list with explicit region mapping:
 *     [
 *       {"region_id": 10295, "keys":[-1920480496,-1423914110,951774544,-1419269290]},
 *       ...
 *     ]
 *
 * Outputs (both are written):
 *  - <outDir>/xteas.json           {"10295":[k0,k1,k2,k3], ...}
 *  - <outDir>/regions/<region>.txt four lines, one int per line
 */
public final class XteaConverter {

    private static final class OpenRs2Key {
        int archive;
        int group;
        @SerializedName("name_hash") Integer nameHash;
        String name;
        Integer mapsquare;
        int[] key;
    }

    private static final class RegionKey {
        @SerializedName(value="region_id", alternate={"mapsquare","region"})
        Integer regionId;
        int[] keys;
        int[] key; // allow alternate field name
    }

    private static Reader openReader(String pathOrUrl) throws IOException {
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            return new InputStreamReader(new URL(pathOrUrl).openStream(), StandardCharsets.UTF_8);
        }
        return new InputStreamReader(new FileInputStream(pathOrUrl), StandardCharsets.UTF_8);
    }

    private static boolean isZero(int[] k) {
        return k == null || (k.length == 4 && k[0]==0 && k[1]==0 && k[2]==0 && k[3]==0);
    }

    private static Map<Integer,int[]> parseOpenRs2(JsonArray arr) {
        Map<Integer,int[]> map = new TreeMap<>();
        Gson gson = new Gson();

        int seen=0, kept=0, wrongArchive=0, zero=0, noRegion=0;
        for (JsonElement e : arr) {
            seen++;
            OpenRs2Key k = gson.fromJson(e, OpenRs2Key.class);
            if (k.archive != 5) { wrongArchive++; continue; }     // only map archive
            if (k.mapsquare == null) { noRegion++; continue; }
            if (isZero(k.key)) { zero++; continue; }
            map.putIfAbsent(k.mapsquare, Arrays.copyOf(k.key, 4));
            kept++;
        }
        System.out.printf(
            "OpenRS2: seen=%d kept=%d zero=%d noRegion=%d wrongArchive=%d unique=%d%n",
            seen, kept, zero, noRegion, wrongArchive, map.size()
        );
        return map;
    }

    private static Map<Integer,int[]> parseRegionList(JsonArray arr) {
        Map<Integer,int[]> map = new TreeMap<>();
        Gson gson = new Gson();

        int seen=0, kept=0, zero=0, noRegion=0, badLen=0;
        for (JsonElement e : arr) {
            seen++;
            RegionKey rk = gson.fromJson(e, RegionKey.class);
            Integer region = rk.regionId;
            int[] keys = rk.keys != null ? rk.keys : rk.key;
            if (region == null) { noRegion++; continue; }
            if (keys == null || keys.length != 4) { badLen++; continue; }
            if (isZero(keys)) { zero++; continue; }
            map.putIfAbsent(region, Arrays.copyOf(keys, 4));
            kept++;
        }
        System.out.printf(
            "RegionList: seen=%d kept=%d zero=%d noRegion=%d badLen=%d unique=%d%n",
            seen, kept, zero, noRegion, badLen, map.size()
        );
        return map;
    }

    private static Map<Integer,int[]> parseInput(String pathOrUrl) throws IOException {
        try (Reader r = openReader(pathOrUrl)) {
            JsonElement root = JsonParser.parseReader(r);
            if (!root.isJsonArray()) {
                throw new IllegalStateException("Top-level must be a JSON array: " + pathOrUrl);
            }
            JsonArray arr = root.getAsJsonArray();

            // Heuristic: OpenRS2 keys.json has "archive" and "mapsquare"
            if (arr.size() > 0 && arr.get(0).isJsonObject()
                    && arr.get(0).getAsJsonObject().has("archive")) {
                return parseOpenRs2(arr);
            }

            // Otherwise, try region list format
            return parseRegionList(arr);
        }
    }

    private static void writeOutputs(Map<Integer,int[]> map, File outDir) throws IOException {
        File regionsDir = new File(outDir, "regions");
        if (!regionsDir.exists() && !regionsDir.mkdirs()) {
            throw new IOException("Failed creating dir: " + regionsDir.getAbsolutePath());
        }

        // 1) Combined JSON
        JsonObject out = new JsonObject();
        for (Map.Entry<Integer,int[]> e : map.entrySet()) {
            JsonArray arr = new JsonArray();
            for (int v : e.getValue()) arr.add(v);
            out.add(e.getKey().toString(), arr);
        }
        try (Writer w = new OutputStreamWriter(
                new FileOutputStream(new File(outDir, "xteas.json")), StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(out, w);
        }

        // 2) Per-region text files
        for (Map.Entry<Integer,int[]> e : map.entrySet()) {
            File f = new File(regionsDir, e.getKey() + ".txt");
            try (PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
                int[] k = e.getValue();
                pw.println(k[0]);
                pw.println(k[1]);
                pw.println(k[2]);
                pw.println(k[3]);
            }
        }
        System.out.printf("Wrote %d region files + xteas.json -> %s%n", map.size(), outDir.getAbsolutePath());
    }

    /**
     * Usage:
     *   XteaConverter <input_path_or_url> [output_dir]
     *
     * Defaults:
     *   input_path_or_url = "https://archive.openrs2.org/caches/runescape/139/keys.json"  // OSRS rev 189
     *   output_dir        = "./Elvarg - Server/data/xtea"
     */
    public static void main(String[] args) throws Exception {
        String input = args.length >= 1 ? args[0] : "https://archive.openrs2.org/caches/runescape/139/keys.json";
        String outDir = args.length >= 2 ? args[1] : "./Elvarg - Server/data/xtea";
        Map<Integer,int[]> map = parseInput(input);
        writeOutputs(map, new File(outDir));
    }
}
