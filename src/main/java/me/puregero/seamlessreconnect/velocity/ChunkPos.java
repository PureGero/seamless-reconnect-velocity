package me.puregero.seamlessreconnect.velocity;

public record ChunkPos(int x, int z) {
    @Override
    public boolean equals(Object other) {
        if (other instanceof ChunkPos) {
            return ((ChunkPos) other).x == x
                    && ((ChunkPos) other).z == z;
        }

        return false;
    }

    @Override
    public int hashCode() {
        // Taken from ChunkCoordIntPair
        int i = 1664525 * this.x() + 1013904223;
        int j = 1664525 * (this.z() ^ -559038737) + 1013904223;

        return i ^ j;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + x + "," + z + "]";
    }
}
