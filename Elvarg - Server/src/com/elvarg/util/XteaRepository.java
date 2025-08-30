package com.elvarg.util;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

/**
 * Loads Elvarg-style xteas.json: { "regionId": [k0,k1,k2,k3], ... }
 * - Ignores zero-keys.
 * - get(regionId) returns null if unknown.
 */
public final class XteaRepository {
    private static final Logger LOG = Logger.getLogger(XteaRepository.class.getName());
    private final Map<Integer,int[]> byRegion;

    private XteaRepository(Map<Integer,int[]> map) {
        this.byRegion = Collections.unmodifiableMap(map);
    }

    public static XteaRepository load(File jsonFile) throws IOException {
        if (!jsonFile.exists()) throw new FileNotFoundException(jsonFile.getAbsolutePath());
        try (Reader r = new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(r);
            if (!root.isJsonObject())
                throw new IllegalStateException("xteas.json must be an object of {\"region\":[k0,k1,k2,k3]}");

            Map<Integer,int[]> tmp = new HashMap<>();
            int total=0, zero=0;
            for (Map.Entry<String,JsonElement> e : root.getAsJsonObject().entrySet()) {
                total++;
                int region = Integer.parseInt(e.getKey());
                JsonArray arr = e.getValue().getAsJsonArray();
                if (arr.size() != 4) continue;
                int[] k = new int[4];
                for (int i=0;i<4;i++) k[i] = arr.get(i).getAsInt();
                if (k[0]==0 && k[1]==0 && k[2]==0 && k[3]==0) { zero++; continue; }
                tmp.put(region, k);
            }
            LOG.info(String.format("Loaded %d XTEA regions (ignored %d zero-keys)", tmp.size(), zero));
            return new XteaRepository(tmp);
        }
    }

    public int[] get(int regionId) { return byRegion.get(regionId); }
    public Map<Integer,int[]> view() { return byRegion; }
}
