package jaggrab.net;
import java.util.Objects;

/**
 * Points to a file inside a cache archive.
 *
 * type  = archive/index id   (0..65535)
 * file  = file id within archive (0..65535)
 * id    = (type << 16) | (file & 0xFFFF)
 */
public final class FileDescriptor {

    /** Archive/index id (aka “type”). */
    private final int type;

    /** File id within the archive. */
    private final int file;

    /**
     * Creates a descriptor from archive (type) and file ids.
     */
    public FileDescriptor(int type, int file) {
        if ((type & ~0xFFFF) != 0 || type < 0) {
            throw new IllegalArgumentException("type out of range: " + type);
        }
        if ((file & ~0xFFFF) != 0 || file < 0) {
            throw new IllegalArgumentException("file out of range: " + file);
        }
        this.type = type;
        this.file = file;
    }

    /**
     * Creates a descriptor from the packed 32-bit id.
     * id = (type << 16) | (file & 0xFFFF)
     */
    public static FileDescriptor fromId(int id) {
        int type = (id >>> 16) & 0xFFFF;
        int file = id & 0xFFFF;
        return new FileDescriptor(type, file);
    }

    /** Archive/index id. */
    public int getType() {
        return type;
    }

    /** File id within the archive. */
    public int getFile() {
        return file;
    }

    /** Packed 32-bit id = (type << 16) | file. */
    public int getId() {
        return (type << 16) | (file & 0xFFFF);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileDescriptor)) return false;
        FileDescriptor that = (FileDescriptor) o;
        return type == that.type && file == that.file;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, file);
    }

    @Override
    public String toString() {
        return "FileDescriptor{type=" + type + ", file=" + file + ", id=" + getId() + '}';
    }
}
