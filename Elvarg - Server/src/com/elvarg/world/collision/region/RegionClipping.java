package com.elvarg.world.collision.region;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.elvarg.Elvarg;
import com.elvarg.cache.CacheLoader;
import com.elvarg.cache.impl.CacheConstants;
import com.elvarg.cache.impl.definitions.ObjectDefinition;
import com.elvarg.world.collision.XTEA;
import com.elvarg.world.collision.buffer.ByteStreamExt;
import com.elvarg.world.entity.impl.Character;
import com.elvarg.world.entity.impl.object.GameObject;
import com.elvarg.world.model.Position;

import io.netty.buffer.ByteBuf;

/**
 * A highly modified version of the released clipping.
 * 
 * @author Relex lawl and Palidino: Gave me (Gabbe/Swiffy96) the base.
 * @editor Swiffy96: Rewrote the system, now loads regions when they're actually needed etc.
 */
public final class RegionClipping {

    private static RegionClipping[] regionArray;
    private static final ArrayList<Integer> loadedRegions = new ArrayList<Integer>();

    private final class RegionData {
        private int id;
        private int terrainFile;
        private int objectFile;

        public RegionData(int id, int mapGround, int mapObject) {
            this.id = id;
            this.terrainFile = mapGround;
            this.objectFile = mapObject;
        }
    }

    private int[][][] clips = new int[4][][];
    public GameObject[][][] gameObjects = new GameObject[4][][];

    private RegionData regionData;

    public RegionClipping(int id, int map, int mapObj) {
        this.regionData = new RegionData(id, map, mapObj);
    }

    public static void init() {
        // Placeholder for static initialization if needed
    }

    public static boolean objectExists(GameObject object) {
        RegionClipping clipping = forPosition(object.getPosition());
        if (clipping == null) return false;
        int x = object.getPosition().getX() - ((clipping.regionData.id >> 8) * 64);
        int y = object.getPosition().getY() - ((clipping.regionData.id & 0xff) * 64);
        int z = object.getPosition().getZ();
        return clipping.gameObjects[z] != null && clipping.gameObjects[z][x][y] != null;
    }

    private void loadMap(int terrainFile, int objectFile) throws IOException {
        if (terrainFile != -1) {
            ByteBuf terrainDataBuf = Elvarg.getFile(0, terrainFile); // MAP_INDEX
            byte[] terrainData = new byte[terrainDataBuf.readableBytes()];
            terrainDataBuf.readBytes(terrainData);
            int[] xteaKeys = getXteaKeys(regionData.id);
            if (terrainData != null && terrainData.length > 0 && xteaKeys != null) {
                XTEA.decrypt(terrainData, xteaKeys, 0, terrainData.length);
            }
            ByteStreamExt terrain = new ByteStreamExt(terrainData);
            int absX = (regionData.id >> 8) * 64;
            int absY = (regionData.id & 0xff) * 64;
            int height = 0;
            for (int z = 0; z < 4; z++) {
                for (int tileX = 0; tileX < 64; tileX++) {
                    for (int tileY = 0; tileY < 64; tileY++) {
                        while (true) {
                            int value = terrain.readUnsignedByte();
                            if (value == 0) {
                                break;
                            } else if (value == 1) {
                                terrain.readUnsignedByte();
                                break;
                            } else if (value <= 49) {
                                terrain.readUnsignedByte();
                            } else if (value <= 81) {
                                clips[z] = clips[z] == null ? new int[64][64] : clips[z];
                                clips[z][tileX][tileY] = value - 49;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (objectFile != -1) {
            ByteBuf objectDataBuf = Elvarg.getFile(0, objectFile); // MAP_INDEX
            byte[] objectData = new byte[objectDataBuf.readableBytes()];
            objectDataBuf.readBytes(objectData);
            int[] xteaKeys = getXteaKeys(regionData.id);
            if (objectData != null && objectData.length > 0 && xteaKeys != null) {
                XTEA.decrypt(objectData, xteaKeys, 0, objectData.length);
            }
            ByteStreamExt objects = new ByteStreamExt(objectData);
            int objectId = -1;
            int incr;
            while ((incr = objects.readUnsignedByte()) != 0) {
                objectId += incr;
                int location = 0;
                int incr2;
                while ((incr2 = objects.readUnsignedByte()) != 0) {
                    gameObjects[0] = gameObjects[0] == null ? new GameObject[64][64] : gameObjects[0];
                    location += incr2 - 1;
                    int localX = location >> 6;
                    int localY = location & 0x3f;
                    int height = objects.readUnsignedByte();
                    int type = objects.readUnsignedByte();
                    int direction = objects.readUnsignedByte();
                    GameObject object = new GameObject(objectId, new Position(localX + ((regionData.id >> 8) * 64), localY + ((regionData.id & 0xff) * 64), height));
                    object.setType(type);
                    object.setFace(direction);
                    gameObjects[height][localX][localY] = object;
                }
            }
        }
    }

    private int[] getXteaKeys(int regionId) {
        try {
            File keyFile = new File("./Elvarg - Server/data/xtea/" + regionId + ".txt");
            if (!keyFile.exists()) {
                System.out.println("No XTEA keys found for region " + regionId);
                return null;
            }
            BufferedReader reader = new BufferedReader(new FileReader(keyFile));
            int[] keys = new int[4];
            for (int i = 0; i < 4; i++) {
                String line = reader.readLine();
                if (line != null && !line.isEmpty()) {
                    keys[i] = Integer.parseInt(line.trim());
                } else {
                    keys[i] = 0; // Default to 0 if key is missing
                }
            }
            reader.close();
            return keys;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void loadRegion(int x, int y) {
        if (loadedRegions.contains(regionData.id)) {
            return;
        }
        try {
            loadMap(regionData.terrainFile, regionData.objectFile);
            loadedRegions.add(regionData.id);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static RegionClipping forPosition(Position position) {
        int regionX = position.getX() >> 3;
        int regionY = position.getY() >> 3;
        int regionId = (regionX << 8) + regionY;
        int x = position.getX() & 0x7;
        int y = position.getY() & 0x7;
        if (regionArray == null) {
            regionArray = new RegionClipping[10000];
        }
        if (regionArray[regionId] == null) {
            regionArray[regionId] = new RegionClipping(regionId, -1, -1);
            regionArray[regionId].loadRegion(x, y);
        } else if (!loadedRegions.contains(regionId)) {
            regionArray[regionId].loadRegion(x, y);
        }
        return regionArray[regionId];
    }

    public static boolean canMove(int srcX, int srcY, int destX, int destY, int height, int xLength, int yLength) {
        int diffX = destX - srcX;
        int diffY = destY - srcY;
        int max = Math.max(Math.abs(diffX), Math.abs(diffY));
        for (int ii = 0; ii < max; ii++) {
            int currX = srcX + (diffX * ii) / max;
            int currY = srcY + (diffY * ii) / max;
            if ((diffX < 0 && diffY < 0) || (diffX > 0 && diffY > 0)) {
                currX += diffX < 0 ? -1 : 1;
                currY += diffY < 0 ? -1 : 1;
            }
            RegionClipping clipping = forPosition(new Position(currX, currY, height));
            if (clipping == null || clipping.clips[height] == null || !clipping.blockedNorthWest(currX, currY, height)
                    || !clipping.blockedNorthEast(currX, currY, height) || !clipping.blockedSouthWest(currX, currY, height)
                    || !clipping.blockedSouthEast(currX, currY, height)) {
                return false;
            }
        }
        return true;
    }

    public static boolean canMove(Position from, Position to, int sizeX, int sizeY) {
        int deltaX = to.getX() - from.getX();
        int deltaY = to.getY() - from.getY();
        if (deltaX == 0 && deltaY == 0) return true;

        int height = from.getZ();
        RegionClipping clipping = forPosition(from);
        if (clipping == null || clipping.clips[height] == null) return false;

        if (deltaX < 0) {
            deltaX = -1;
        } else if (deltaX > 0) {
            deltaX = 1;
        } else {
            deltaX = 0;
        }

        if (deltaY < 0) {
            deltaY = -1;
        } else if (deltaY > 0) {
            deltaY = 1;
        } else {
            deltaY = 0;
        }

        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                if (!clipping.canMove(from.getX() + x, from.getY() + y, from.getX() + x + deltaX, from.getY() + y + deltaY, height, 1, 1)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean canProjectileMove(int srcX, int srcY, int destX, int destY, int height) {
        int x = srcX - destX;
        int y = srcY - destY;
        if (x >= 0) {
            x = 1 + (srcX - destX);
        } else {
            x = 1 - (srcX - destX);
        }
        if (y >= 0) {
            y = 1 + (srcY - destY);
        } else {
            y = 1 - (srcY - destY);
        }
        x--;
        y--;
        if (x == -1 || y == -1) {
            x = srcX;
            y = srcY;
        }
        RegionClipping clipping = forPosition(new Position(srcX, srcY, height));
        for (int i = 0; i < x; i++) {
            srcX--;
            if (!clipping.traversable(srcX, srcY, height)) {
                return false;
            }
            if (!clipping.projectileTraversable(srcX, srcY, height, 1)) {
                return false;
            }
        }
        for (int i = 0; i < y; i++) {
            srcY--;
            if (!clipping.traversable(srcX, srcY, height)) {
                return false;
            }
            if (!clipping.projectileTraversable(srcX, srcY, height, 3)) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkProjectileStep(int srcX, int srcY, int x, int y, int height, int lastdir) {
        int dir = direction(srcX, srcY, x, y);
        if (dir == -1) return false;
        if ((dir & 1) != 0) {
            if (x + y - srcX - srcY >= 0) {
                int firstX = srcX;
                int firstY = srcY;
                int secondX = srcX;
                int secondY = srcY;
                switch (dir) {
                case 1:
                case 3:
                    firstX--;
                    break;
                case 5:
                case 7:
                    firstX++;
                    break;
                }
                switch (dir) {
                case 0:
                case 1:
                case 7:
                    firstY--;
                    break;
                case 3:
                case 4:
                case 5:
                    firstY++;
                    break;
                }
                switch (dir) {
                case 1:
                case 5:
                    secondX += x > srcX ? 1 : -1;
                    break;
                case 3:
                case 7:
                    secondY += y > srcY ? 1 : -1;
                    break;
                }
                RegionClipping clipping = forPosition(new Position(firstX, firstY, height));
                if (clipping == null || clipping.clips[height] == null) return false;
                if (!clipping.traversable(firstX, firstY, height)) {
                    return false;
                }
                clipping = forPosition(new Position(secondX, secondY, height));
                if (clipping == null || clipping.clips[height] == null) return false;
                if (!clipping.traversable(secondX, secondY, height)) {
                    return false;
                }
            } else {
                int firstX = x;
                int firstY = y;
                int secondX = x;
                int secondY = y;
                switch (dir) {
                case 1:
                case 3:
                    firstX++;
                    break;
                case 5:
                case 7:
                    firstX--;
                    break;
                }
                switch (dir) {
                case 0:
                case 1:
                case 7:
                    firstY++;
                    break;
                case 3:
                case 4:
                case 5:
                    firstY--;
                    break;
                }
                switch (dir) {
                case 1:
                case 5:
                    secondX += x > srcX ? 1 : -1;
                    break;
                case 3:
                case 7:
                    secondY += y > srcY ? 1 : -1;
                    break;
                }
                RegionClipping clipping = forPosition(new Position(firstX, firstY, height));
                if (clipping == null || clipping.clips[height] == null) return false;
                if (!clipping.traversable(firstX, firstY, height)) {
                    return false;
                }
                clipping = forPosition(new Position(secondX, secondY, height));
                if (clipping == null || clipping.clips[height] == null) return false;
                if (!clipping.traversable(secondX, secondY, height)) {
                    return false;
                }
            }
        }
        switch (lastdir) {
        case 0:
            if (dir == 0 || dir == 2 || dir == 4) return false;
            break;
        case 1:
            if (dir == 1 || dir == 3) return false;
            break;
        case 2:
            if (dir == 0 || dir == 2 || dir == 6) return false;
            break;
        case 3:
            if (dir == 1 || dir == 3 || dir == 5) return false;
            break;
        case 4:
            if (dir == 0 || dir == 4 || dir == 6) return false;
            break;
        case 5:
            if (dir == 1 || dir == 5 || dir == 7) return false;
            break;
        case 6:
            if (dir == 2 || dir == 4 || dir == 6) return false;
            break;
        case 7:
            if (dir == 3 || dir == 5 || dir == 7) return false;
            break;
        }
        return true;
    }

    private static int direction(int srcX, int srcY, int x, int y) {
        if (x < srcX) {
            if (y < srcY) {
                return 0; // Northwest
            } else if (y > srcY) {
                return 2; // Southwest
            } else {
                return 1; // West
            }
        } else if (x > srcX) {
            if (y < srcY) {
                return 6; // Northeast
            } else if (y > srcY) {
                return 4; // Southeast
            } else {
                return 3; // East
            }
        } else {
            if (y < srcY) {
                return 7; // North
            } else if (y > srcY) {
                return 5; // South
            } else {
                return -1; // Same spot
            }
        }
    }

    public boolean blockedNorth(int x, int y, int height) {
        if (height < 0 || height > 3) height = 0;
        loadRegion(x, y);
        if (clips[height] == null) return false;
        int xInRegion = x - ((regionData.id >> 8) * 64);
        int yInRegion = y - ((regionData.id & 0xff) * 64);
        if (xInRegion < 0 || xInRegion >= 64 || yInRegion < 0 || yInRegion >= 64) return true;
        return (clips[height][xInRegion][yInRegion] & PROJECTILE_NORTH_BLOCKED) != 0;
    }

    public boolean blockedEast(int x, int y, int height) {
        if (height < 0 || height > 3) height = 0;
        loadRegion(x, y);
        if (clips[height] == null) return false;
        int xInRegion = x - ((regionData.id >> 8) * 64);
        int yInRegion = y - ((regionData.id & 0xff) * 64);
        if (xInRegion < 0 || xInRegion >= 64 || yInRegion < 0 || yInRegion >= 64) return true;
        return (clips[height][xInRegion][yInRegion] & PROJECTILE_EAST_BLOCKED) != 0;
    }

    public boolean blockedSouth(int x, int y, int height) {
        if (height < 0 || height > 3) height = 0;
        loadRegion(x, y);
        if (clips[height] == null) return false;
        int xInRegion = x - ((regionData.id >> 8) * 64);
        int yInRegion = y - ((regionData.id & 0xff) * 64);
        if (xInRegion < 0 || xInRegion >= 64 || yInRegion < 0 || yInRegion >= 64) return true;
        return (clips[height][xInRegion][yInRegion] & PROJECTILE_SOUTH_BLOCKED) != 0;
    }

    public boolean blockedWest(int x, int y, int height) {
        if (height < 0 || height > 3) height = 0;
        loadRegion(x, y);
        if (clips[height] == null) return false;
        int xInRegion = x - ((regionData.id >> 8) * 64);
        int yInRegion = y - ((regionData.id & 0xff) * 64);
        if (xInRegion < 0 || xInRegion >= 64 || yInRegion < 0 || yInRegion >= 64) return true;
        return (clips[height][xInRegion][yInRegion] & PROJECTILE_WEST_BLOCKED) != 0;
    }

    public boolean blockedNorthWest(int x, int y, int height) {
        if (height < 0 || height > 3) height = 0;
        loadRegion(x, y);
        if (clips[height] == null) return false;
        int xInRegion = x - ((regionData.id >> 8) * 64);
        int yInRegion = y - ((regionData.id & 0xff) * 64);
        if (xInRegion < 0 || xInRegion >= 64 || yInRegion < 0 || yInRegion >= 64) return true;
        return (clips[height][xInRegion][yInRegion] & PROJECTILE_NORTH_WEST_BLOCKED) != 0;
    }

    public boolean blockedNorthEast(int x, int y, int height) {
        if (height < 0 || height > 3) height = 0;
        loadRegion(x, y);
        if (clips[height] == null) return false;
        int xInRegion = x - ((regionData.id >> 8) * 64);
        int yInRegion = y - ((regionData.id & 0xff) * 64);
        if (xInRegion < 0 || xInRegion >= 64 || yInRegion < 0 || yInRegion >= 64) return true;
        return (clips[height][xInRegion][yInRegion] & PROJECTILE_NORTH_EAST_BLOCKED) != 0;
    }

    public boolean blockedSouthEast(int x, int y, int height) {
        if (height < 0 || height > 3) height = 0;
        loadRegion(x, y);
        if (clips[height] == null) return false;
        int xInRegion = x - ((regionData.id >> 8) * 64);
        int yInRegion = y - ((regionData.id & 0xff) * 64);
        if (xInRegion < 0 || xInRegion >= 64 || yInRegion < 0 || yInRegion >= 64) return true;
        return (clips[height][xInRegion][yInRegion] & PROJECTILE_SOUTH_EAST_BLOCKED) != 0;
    }

    public boolean blockedSouthWest(int x, int y, int height) {
        if (height < 0 || height > 3) height = 0;
        loadRegion(x, y);
        if (clips[height] == null) return false;
        int xInRegion = x - ((regionData.id >> 8) * 64);
        int yInRegion = y - ((regionData.id & 0xff) * 64);
        if (xInRegion < 0 || xInRegion >= 64 || yInRegion < 0 || yInRegion >= 64) return true;
        return (clips[height][xInRegion][yInRegion] & PROJECTILE_SOUTH_WEST_BLOCKED) != 0;
    }

    public boolean traversable(int x, int y, int height) {
        if (height < 0 || height > 3) height = 0;
        loadRegion(x, y);
        if (clips[height] == null) return false;
        int xInRegion = x - ((regionData.id >> 8) * 64);
        int yInRegion = y - ((regionData.id & 0xff) * 64);
        if (xInRegion < 0 || xInRegion >= 64 || yInRegion < 0 || yInRegion >= 64) return true;
        return (clips[height][xInRegion][yInRegion] & BLOCKED_TILE) == 0;
    }

    public boolean projectileTraversable(int x, int y, int height, int direction) {
        if (height < 0 || height > 3) height = 0;
        loadRegion(x, y);
        if (clips[height] == null) return false;
        int xInRegion = x - ((regionData.id >> 8) * 64);
        int yInRegion = y - ((regionData.id & 0xff) * 64);
        if (xInRegion < 0 || xInRegion >= 64 || yInRegion < 0 || yInRegion >= 64) return true;
        if (direction == 0) { // north
            return (clips[height][xInRegion][yInRegion] & PROJECTILE_NORTH_BLOCKED) == 0;
        } else if (direction == 1) { // east
            return (clips[height][xInRegion][yInRegion] & PROJECTILE_EAST_BLOCKED) == 0;
        } else if (direction == 2) { // south
            return (clips[height][xInRegion][yInRegion] & PROJECTILE_SOUTH_BLOCKED) == 0;
        } else if (direction == 3) { // west
            return (clips[height][xInRegion][yInRegion] & PROJECTILE_WEST_BLOCKED) == 0;
        }
        return true;
    }

    public static void addObject(GameObject object) {
        RegionClipping clipping = forPosition(object.getPosition());
        if (clipping == null) return;
        int x = object.getPosition().getX() - ((clipping.regionData.id >> 8) * 64);
        int y = object.getPosition().getY() - ((clipping.regionData.id & 0xff) * 64);
        clipping.gameObjects[object.getPosition().getZ()][x][y] = object;
    }

    public static void removeObject(GameObject object) {
        RegionClipping clipping = forPosition(object.getPosition());
        if (clipping == null) return;
        int x = object.getPosition().getX() - ((clipping.regionData.id >> 8) * 64);
        int y = object.getPosition().getY() - ((clipping.regionData.id & 0xff) * 64);
        clipping.gameObjects[object.getPosition().getZ()][x][y] = null;
    }

    public static int getClipping(int x, int y, int height) {
        RegionClipping clipping = forPosition(new Position(x, y, height));
        if (clipping == null || clipping.clips[height] == null) return 0;
        int xInRegion = x - ((clipping.regionData.id >> 8) * 64);
        int yInRegion = y - ((clipping.regionData.id & 0xff) * 64);
        if (xInRegion < 0 || xInRegion >= 64 || yInRegion < 0 || yInRegion >= 64) return 0;
        return clipping.clips[height][xInRegion][yInRegion];
    }

    public static boolean blockedNorth(Position pos) {
        RegionClipping clipping = forPosition(pos);
        return clipping != null && clipping.blockedNorth(pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean blockedEast(Position pos) {
        RegionClipping clipping = forPosition(pos);
        return clipping != null && clipping.blockedEast(pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean blockedSouth(Position pos) {
        RegionClipping clipping = forPosition(pos);
        return clipping != null && clipping.blockedSouth(pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean blockedWest(Position pos) {
        RegionClipping clipping = forPosition(pos);
        return clipping != null && clipping.blockedWest(pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean blockedNorthWest(Position pos) {
        RegionClipping clipping = forPosition(pos);
        return clipping != null && clipping.blockedNorthWest(pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean blockedNorthEast(Position pos) {
        RegionClipping clipping = forPosition(pos);
        return clipping != null && clipping.blockedNorthEast(pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean blockedSouthEast(Position pos) {
        RegionClipping clipping = forPosition(pos);
        return clipping != null && clipping.blockedSouthEast(pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean blockedSouthWest(Position pos) {
        RegionClipping clipping = forPosition(pos);
        return clipping != null && clipping.blockedSouthWest(pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean traversable(Position pos) {
        RegionClipping clipping = forPosition(pos);
        return clipping != null && clipping.traversable(pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean projectileTraversable(Position pos, int direction) {
        RegionClipping clipping = forPosition(pos);
        return clipping != null && clipping.projectileTraversable(pos.getX(), pos.getY(), pos.getZ(), direction);
    }

    public static boolean isInDiagonalBlock(Character attacker, Character attacked) {
        Position attackerPos = attacker.getPosition();
        Position attackedPos = attacked.getPosition();
        return (attackedPos.getX() - 1 == attackerPos.getX() && attackedPos.getY() + 1 == attackerPos.getY()) ||
               (attackerPos.getX() - 1 == attackedPos.getX() && attackerPos.getY() + 1 == attackedPos.getY()) ||
               (attackedPos.getX() + 1 == attackerPos.getX() && attackedPos.getY() - 1 == attackerPos.getY()) ||
               (attackerPos.getX() + 1 == attackedPos.getX() && attackerPos.getY() - 1 == attackedPos.getY()) ||
               (attackedPos.getX() + 1 == attackerPos.getX() && attackedPos.getY() + 1 == attackerPos.getY()) ||
               (attackerPos.getX() + 1 == attackedPos.getX() && attackerPos.getY() + 1 == attackedPos.getY());
    }

    public static final int PROJECTILE_NORTH_WEST_BLOCKED = 0x200;
    public static final int PROJECTILE_NORTH_BLOCKED = 0x400;
    public static final int PROJECTILE_NORTH_EAST_BLOCKED = 0x800;
    public static final int PROJECTILE_EAST_BLOCKED = 0x1000;
    public static final int PROJECTILE_SOUTH_EAST_BLOCKED = 0x2000;
    public static final int PROJECTILE_SOUTH_BLOCKED = 0x4000;
    public static final int PROJECTILE_SOUTH_WEST_BLOCKED = 0x8000;
    public static final int PROJECTILE_WEST_BLOCKED = 0x10000;
    public static final int PROJECTILE_TILE_BLOCKED = 0x20000;
    public static final int UNKNOWN = 0x80000;
    public static final int BLOCKED_TILE = 0x200000;
    public static final int UNLOADED_TILE = 0x1000000;
    public static final int OCEAN_TILE = 2097152;
}