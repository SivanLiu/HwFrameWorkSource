package android_maps_conflict_avoidance.com.google.googlenav.map;

import java.io.IOException;

public class NullMapTileStorage implements MapTileStorage {
    private int textSize = -1;
    private int tileEdition = -1;

    public void close(boolean saveState) {
    }

    public MapTile getMapTile(Tile tile) {
        return null;
    }

    public void mapChanged() {
    }

    public boolean writeCache() throws IOException {
        return false;
    }

    public boolean setTileEditionAndTextSize(int newTileEdition, int newTextSize) {
        boolean changed;
        if (newTileEdition == this.tileEdition || this.tileEdition == -1) {
            if (newTextSize != this.textSize) {
                if (this.textSize == -1) {
                }
            }
            changed = false;
            this.tileEdition = newTileEdition;
            this.textSize = newTextSize;
            return changed;
        }
        changed = true;
        this.tileEdition = newTileEdition;
        this.textSize = newTextSize;
        return changed;
    }
}
