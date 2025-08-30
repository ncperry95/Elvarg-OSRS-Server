package com.elvarg;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.elvarg.cache.CacheLoader;
import com.elvarg.cache.impl.definitions.ItemDefinition;
import com.elvarg.cache.impl.definitions.NpcDefinition;
import com.elvarg.cache.impl.definitions.ObjectDefinition;
import com.elvarg.cache.impl.definitions.ShopDefinition;
import com.elvarg.cache.impl.definitions.WeaponInterfaces;
import com.elvarg.engine.GameEngine;
import com.elvarg.engine.task.impl.CombatPoisonEffect.CombatPoisonData;
import com.elvarg.net.NetworkConstants;
import com.elvarg.net.channel.ChannelPipelineHandler;
import com.elvarg.util.ShutdownHook;
import com.elvarg.world.World;
import com.elvarg.world.collision.region.RegionClipping;
import com.elvarg.world.content.PlayerPunishment;
import com.elvarg.world.content.clan.ClanChatManager;
import com.elvarg.world.model.dialogue.DialogueManager;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import jaggrab.Jaggrab;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Entry point for Elvarg.
 */
public final class Elvarg {

    /** Game engine (600ms tick). */
    private static final GameEngine engine = new GameEngine();

    /** Cache loader (defs, clipping, etc). */
    private static final CacheLoader cacheLoader = new CacheLoader();

    /** Updating flag. */
    private static boolean updating;

    /** Logger. */
    private static final Logger logger = Logger.getLogger("Elvarg");

    public static void main(String[] params) {
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
        try {
            logger.info("Initializing the game...");

            final ExecutorService serviceLoader =
                    Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                            .setNameFormat("GameLoadingThread").build());

            // Load cache + optional JAGGRAB
            serviceLoader.execute(() -> {
                try {
                    cacheLoader.init();

                    if (GameConstants.JAGGRAB_ENABLED) {
                        // Jaggrab.init() declares throws Exception; wrap it here
                        try {
                            new Jaggrab().init();
                        } catch (Exception e) {
                            logger.warning("JAGGRAB failed to start: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            // DEFINITIONS
            serviceLoader.execute(() -> ItemDefinition.parseItems().load());
            serviceLoader.execute(() -> NpcDefinition.parseNpcs().load());
            serviceLoader.execute(() -> ObjectDefinition.parseObjects().load());
            serviceLoader.execute(() -> ShopDefinition.parseShops().load());
            serviceLoader.execute(() -> WeaponInterfaces.parseInterfaces().load());
            serviceLoader.execute(() -> DialogueManager.parseDialogues().load());

            // OTHER SYSTEMS
            serviceLoader.execute(() -> ClanChatManager.init());
            serviceLoader.execute(() -> CombatPoisonData.init());
            serviceLoader.execute(() -> RegionClipping.init());
            serviceLoader.execute(() -> World.init());
            serviceLoader.execute(() -> PlayerPunishment.init());

            // Shutdown the loader and await completion
            serviceLoader.shutdown();
            if (!serviceLoader.awaitTermination(15, TimeUnit.MINUTES)) {
                throw new IllegalStateException("The background service load took too long!");
            }

            // Bind game port
            logger.info("Binding port " + NetworkConstants.GAME_PORT + "...");
            ResourceLeakDetector.setLevel(Level.DISABLED);
            EventLoopGroup loopGroup = new NioEventLoopGroup();
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(loopGroup)
                     .channel(NioServerSocketChannel.class)
                     .childHandler(new ChannelPipelineHandler())
                     .bind(NetworkConstants.GAME_PORT)
                     .syncUninterruptibly();

            // Start game engine loop
            logger.info("Starting game engine...");
            final ScheduledExecutorService executor =
                    Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                            .setNameFormat("GameThread").build());
            executor.scheduleAtFixedRate(
                    engine, 0, GameConstants.ENGINE_PROCESSING_CYCLE_RATE, TimeUnit.MILLISECONDS);

            logger.info("Elvarg successfully started.");
        } catch (Exception ex) {
            logger.severe("Could not start Elvarg! Program terminated.");
            ex.printStackTrace();
            System.exit(1);
        }
    }

    /** Expose cache loader if needed elsewhere. */
    public static CacheLoader getCache() {
        return cacheLoader;
    }

    /**
     * Provide RegionClipping (and others) access to cache files.
     * Matches the old call sites: Elvarg.getFile(archive, file).
     */
    public static ByteBuf getFile(int archive, int file) {
        try {
            return cacheLoader.getFile(archive, file);
        } catch (IOException e) {
            e.printStackTrace();
            return Unpooled.buffer(0); // safe empty buffer on failure
        }
    }

    public static Logger getLogger() {
        return logger;
    }

    public static void setUpdating(boolean updating) {
        Elvarg.updating = updating;
    }

    public static boolean isUpdating() {
        return Elvarg.updating;
    }

    public static final class GameConstants {
        public static final boolean JAGGRAB_ENABLED = false;
        public static final int ENGINE_PROCESSING_CYCLE_RATE = 600;
        public static final boolean LOG_PLAYER_COMMANDS = false;
        public static final int LOGIN_LIMIT_PER_COMPUTER = 2;
        public static final int PLAYERS_LIMIT = 350;
        public static final int GAME_PORT = 43594;
        public static final int NEW_CONNECTIONS_LIMIT_PER_SECOND = 30;
        public static final int PACKETS_LIMIT_PER_SECOND = 80;
        public static final int CONNECTIONS_LIMIT_PER_CYCLE = 15;
        public static final int MESSAGES_LIMIT_PER_CYCLE = 25;
        public static final int PACKETS_LIMIT_PER_CYCLE = 40;
        public static final int MAX_PACKET_PAYLOAD_SIZE = 10000;

        private GameConstants() {}
    }
}
