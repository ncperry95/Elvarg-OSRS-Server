package jaggrab.net;

/**
 * Descriptor for addressing cache content by (container/archive, file).
 * Adds getType() which is referenced by OnDemandResponseEncoder.
 */
public final class FileDescriptor {

    private final int container;
    private final int file;

    public FileDescriptor(int container, int file) {
        this.container = container;
        this.file = file;
    }

    public int getContainer() {
        return container;
    }

    public int getFile() {
        return file;
    }

    /**
     * Some encoders call (getType() - 1). If you have multiple descriptor types,
     * adjust this to return the real type. For now, return 1 (map container).
     */
    public int getType() {
        return 1;
    }

    @Override
    public String toString() {
        return "FileDescriptor{container=" + container + ", file=" + file + "}";
    }
}
