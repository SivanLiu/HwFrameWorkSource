package android_maps_conflict_avoidance.com.google.googlenav.layer;

import android_maps_conflict_avoidance.com.google.common.io.protocol.ProtoBuf;

public class LayerItem {
    private final ProtoBuf activitySnippet;
    private final boolean isRoutable;
    private final String itemId;
    private final String layerId;
    private final String name;
    private final int ranking;
    private final ProtoBuf rating;
    private final String snippet;

    public LayerItem(ProtoBuf layerItem) {
        this.layerId = layerItem.getString(1);
        this.itemId = layerItem.getString(2);
        this.name = layerItem.has(3) ? layerItem.getString(3) : "";
        this.snippet = layerItem.has(4) ? layerItem.getString(4) : "";
        int i = 0;
        this.isRoutable = layerItem.has(5) ? layerItem.getBool(5) : false;
        ProtoBuf protoBuf = null;
        this.rating = layerItem.has(6) ? layerItem.getProtoBuf(6) : null;
        if (layerItem.has(7)) {
            i = layerItem.getInt(7);
        }
        this.ranking = i;
        if (layerItem.has(9)) {
            protoBuf = layerItem.getProtoBuf(9);
        }
        this.activitySnippet = protoBuf;
    }

    public String getLayerId() {
        return this.layerId;
    }

    public String getItemId() {
        return this.itemId;
    }

    public String getName() {
        return this.name;
    }

    public String getSnippet() {
        return this.snippet;
    }

    public ProtoBuf getBuzzSnippet() {
        return this.activitySnippet;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("layerId: ");
        stringBuilder.append(this.layerId);
        stringBuilder.append(", itemId: ");
        stringBuilder.append(this.itemId);
        stringBuilder.append(", name: ");
        stringBuilder.append(this.name);
        stringBuilder.append(", snippet: ");
        stringBuilder.append(this.snippet);
        return stringBuilder.toString();
    }
}
