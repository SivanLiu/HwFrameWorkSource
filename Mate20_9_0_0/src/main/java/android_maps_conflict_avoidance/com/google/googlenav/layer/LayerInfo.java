package android_maps_conflict_avoidance.com.google.googlenav.layer;

import android_maps_conflict_avoidance.com.google.common.io.protocol.ProtoBuf;

public class LayerInfo {
    private final String description;
    private final String id;
    private int maxZoomLevel;
    private int minZoomLevel;
    private String name;
    private ProtoBuf[] parameters;

    public String getId() {
        return this.id;
    }

    public ProtoBuf[] getParameters() {
        return this.parameters;
    }

    public boolean isValidZoomLevel(int currentZoomLevel) {
        return currentZoomLevel >= this.minZoomLevel && currentZoomLevel <= this.maxZoomLevel;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("id=");
        stringBuilder.append(this.id);
        stringBuilder.append(", name=");
        stringBuilder.append(this.name);
        stringBuilder.append(", description=");
        stringBuilder.append(this.description);
        return stringBuilder.toString();
    }
}
