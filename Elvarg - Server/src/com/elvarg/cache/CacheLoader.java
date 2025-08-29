package com.elvarg.cache;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.zip.CRC32;

import com.elvarg.cache.impl.Cache;
import com.elvarg.cache.impl.CacheArchive;
import com.elvarg.cache.impl.CacheConstants;
import com.elvarg.util.CompressionUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import jaggrab.net.FileDescriptor;

/**
 * Represents a file system of {@link Cache}s and {@link CacheArchive}s.
 * 
 * @author Ryley Kimmel <ryley.kimmel@live.com>
 * @author Artem Batutin <artembatutin@gmail.com>
 * @editor Swiffy96
 */
public class CacheLoader {

    /**
     * The logger for error and debug messages.
     */
    private static final Logger logger = Logger.getLogger(CacheLoader.class.getName());

    /**
     * All of the {@link Cache}s within this {@link CacheLoader}.
     */
    public Cache[] CACHES;

    /**
     * All of the {@link CacheArchive}s within this {@link CacheLoader}.
     */
    public CacheArchive[] ARCHIVES;

    /**
     * The cached archive hashes.
     */
    public ByteBuf CRC_TABLE;

    /**
     * The preloadable-files table.
     */
    public ByteBuf[] PRELOAD_FILES = new ByteBuf[CacheConstants.PRELOAD_FILES.length];

    /**
     * Constructs and initializes a {@link CacheLoader} from the specified
     * {@code directory}.
     * 
     * @throws Exception If an error occurs during initialization.
     */
    public void init() throws Exception {
        Path root = Paths.get(CacheConstants.CACHE_BASE_DIR);
        logger.info("Loading cache from: " + CacheConstants.CACHE_BASE_DIR);
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("Cache directory does not exist: " + CacheConstants.CACHE_BASE_DIR);
        }

        Path data = root.resolve(CacheConstants.DATA_PREFIX);
        if (!Files.exists(data)) {
            throw new IllegalStateException("No data file found: " + data);
        }

        // Load cache files...
        SeekableByteChannel dataChannel = Files.newByteChannel(data, READ);
        ByteBuffer buffer = ByteBuffer.allocate((int) dataChannel.size());
        dataChannel.read(buffer);
        buffer.flip();
        dataChannel.close();

        CACHES = new Cache[CacheConstants.MAXIMUM_ARCHIVES];
        ARCHIVES = new CacheArchive[CacheConstants.MAXIMUM_ARCHIVES];

        for (int archive = 0; archive < CacheConstants.MAXIMUM_ARCHIVES; archive++) {
            int length = buffer.getInt();
            if (length < 0) {
                continue;
            }
            byte[] dataBytes = new byte[length];
            buffer.get(dataBytes);
            ByteBuf archiveData = Unpooled.wrappedBuffer(dataBytes);
            // Correct Cache construction (assuming Cache requires channels and version)
            Path indexFile = root.resolve(CacheConstants.INDEX_PREFIX + archive);
            SeekableByteChannel indexChannel = Files.newByteChannel(indexFile, READ);
            CACHES[archive] = new Cache(indexChannel, dataChannel, 1); // Adjust version as needed
            indexChannel.close();
            ARCHIVES[archive] = CacheArchive.decode(archiveData);
        }

        // Load preloadable files...
        for (int index = 0; index < CacheConstants.PRELOAD_FILES.length; index++) {
            PRELOAD_FILES[index] = null;
        }
    }

    /**
     * Gets a file from the cache.
     * 
     * @param archive The archive index.
     * @param file The file index.
     * @return The file data as a ByteBuf, or null if not found.
     * @throws IOException If an I/O error occurs.
     */
    public ByteBuf getFile(int archive, int file) throws IOException {
        if (archive < 0 || archive >= ARCHIVES.length || ARCHIVES[archive] == null) {
            return null;
        }
        CacheArchive archiveData = ARCHIVES[archive];
        // Convert file ID to a string hash (temporary workaround)
        String fileName = String.valueOf(file); // This needs proper hashing logic
        ByteBuf data = archiveData.getData(fileName);
        return data != null ? data : Unpooled.buffer(0); // Return empty buffer if null
    }

    /**
     * Gets a file from the cache based on a FileDescriptor.
     * 
     * @param desc The FileDescriptor.
     * @return The file data as a ByteBuf, or null if not found.
     * @throws IOException If an I/O error occurs.
     */
    public ByteBuf getFile(FileDescriptor desc) throws IOException {
        return getFile(desc.getId() >> 16, desc.getId() & 0xFFFF); // Adjust based on FileDescriptor structure
    }

    /**
     * Returns the cached {@link #PRELOAD_FILES} if they exist, otherwise they are
     * loaded and cached for future use.
     * 
     * @param index The index of the preloadable file.
     * @return The data of the preloadable file.
     * @throws IOException If some I/O exception occurs.
     */
    public ByteBuf getPreloadFile(int index) throws IOException {
        synchronized (this) {
            if (PRELOAD_FILES[index] != null) {
                return PRELOAD_FILES[index].slice();
            }

            // Load the file data...
            byte[] data = Files.readAllBytes(Paths.get(CacheConstants.CACHE_BASE_DIR + CacheConstants.PRELOAD_FILES[index]));

            // Create a copied buffer from the file data..
            ByteBuf buf = Unpooled.copiedBuffer(data);

            synchronized (this) {
                PRELOAD_FILES[index] = buf;
                return PRELOAD_FILES[index].slice();
            }
        }
    }

    /**
     * Returns the cached {@link #CRC_TABLE} if they exist, otherwise they are
     * calculated and cached for future use.
     * 
     * @return The hashes of each {@link CacheArchive}.
     * @throws IOException If some I/O exception occurs.
     */
    public ByteBuf getCrcTable() throws IOException {
        synchronized (this) {
            if (CRC_TABLE != null) {
                return CRC_TABLE.slice();
            }

            int[] crcs = new int[CacheConstants.MAXIMUM_ARCHIVES + PRELOAD_FILES.length];

            CRC32 crc32 = new CRC32();
            for (int file = 1; file < crcs.length; file++) {
                ByteBuf buffer;

                // Should we fetch the crc for a preloadable file or a cache file?
                if (file >= CacheConstants.MAXIMUM_ARCHIVES) {
                    buffer = getPreloadFile(file - CacheConstants.MAXIMUM_ARCHIVES);
                } else {
                    try {
                        buffer = getFile(file, 0); // Example file index
                    } catch (IOException e) {
                        logger.warning("Failed to get CRC for file " + file + ": " + e.getMessage());
                        continue;
                    }
                }

                crc32.reset();
                byte[] bytes = new byte[buffer.readableBytes()];
                buffer.readBytes(bytes, 0, bytes.length);
                crc32.update(bytes, 0, bytes.length);
                crcs[file] = (int) crc32.getValue();
            }

            ByteBuf buffer = Unpooled.buffer(crcs.length * Integer.BYTES + 4);

            int hash = 1234;
            for (int crc : crcs) {
                hash = (hash << 1) + crc;
                buffer.writeInt(crc);
            }

            buffer.writeInt(hash);

            synchronized (this) {
                CRC_TABLE = buffer.asReadOnly();
                return CRC_TABLE.slice();
            }
        }
    }

    /**
     * Gets a file from the file system.
     * 
     * @param file The file to read.
     * @return The file data as a byte array.
     * @throws IOException If an I/O error occurs.
     */
    private byte[] getFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis);
             DataInputStream dis = new DataInputStream(bis)) {
            byte[] data = new byte[(int) file.length()];
            dis.readFully(data);
            return data;
        }
    }
}