package com.elvarg;

import com.elvarg.cache.CacheLoader;
import com.elvarg.net.channel.ChannelPipelineHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Minimal Elvarg server bootstrap.
 * - Starts a Netty listener using the existing ChannelPipelineHandler.
 * - Port defaults to 43595 (override with -Delvarg.port=43595).
 * - Leaves your existing compile-first shims in place so we can iterate.
 */
public final class Elvarg {

    private static final Logger LOGGER = Logger.getLogger("Elvarg");
    private static final ExecutorService serviceLoader = Executors.newFixedThreadPool(2);

    private static final CacheLoader CACHE = new CacheLoader();

    private static volatile boolean UPDATING = false;

    public static Logger getLogger() {
        return LOGGER;
    }

    public static boolean isUpdating() {
        return UPDATING;
    }

    public static void setUpdating(boolean updating) {
        UPDATING = updating;
    }

    /** Entry point. */
    public static void main(String[] args) {
        // Initialize cache shim (safe even if it returns empty buffers for now)
        try {
            CACHE.init();
        } catch (Throwable t) {
            LOGGER.warning("Cache init failed (continuing for now): " + t.getMessage());
        }

        // If/when you wire real definitions/world init, kick them off here.
        // serviceLoader.execute(() -> ItemDefinition.parseItems().load());
        // serviceLoader.execute(() -> NpcDefinition.parseNpcs().load());
        // serviceLoader.execute(World::init);

        final int port = getPort();
        LOGGER.info("Starting Elvarg Netty server on port " + port + "...");

        // Standard Netty bootstrap
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelPipelineHandler())
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture bindFuture = bootstrap.bind(port).syncUninterruptibly();
            LOGGER.info("Elvarg server is listening on 0.0.0.0:" + port);

            // Keep process alive until channel closes
            addShutdownHook(bossGroup, workerGroup);
            bindFuture.channel().closeFuture().syncUninterruptibly();
        } finally {
            // In case we broke out of the run loop unexpectedly
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            serviceLoader.shutdown();
            LOGGER.info("Elvarg server stopped.");
        }
    }

    private static int getPort() {
        String prop = System.getProperty("elvarg.port");
        if (prop != null && !prop.isEmpty()) {
            try { return Integer.parseInt(prop); } catch (NumberFormatException ignored) {}
        }
        return 43595;
    }
    // --- add these in com.elvarg.Elvarg (inside the class) ---
    public static com.elvarg.cache.CacheLoader getCache() {
        return ElvargCache.getCache();
    }

    public static io.netty.buffer.ByteBuf getFile(int indexId, int fileId) {
        return ElvargCache.getFile(indexId, fileId);
    }
    // --- end additions ---


    private static void addShutdownHook(EventLoopGroup boss, EventLoopGroup worker) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                LOGGER.info("Shutting down Netty event loops...");
                boss.shutdownGracefully().syncUninterruptibly();
                worker.shutdownGracefully().syncUninterruptibly();
            } catch (Throwable ignored) {
            }
        }, "elvarg-shutdown"));
    }
}
