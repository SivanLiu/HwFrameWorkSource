package com.android.internal.telephony;

public class LinkCapacityEstimate {
    public static final int INVALID = -1;
    public static final int STATUS_ACTIVE = 0;
    public static final int STATUS_SUSPENDED = 1;
    public final int confidence;
    public final int downlinkCapacityKbps;
    public final int status;
    public final int uplinkCapacityKbps;

    public LinkCapacityEstimate(int downlinkCapacityKbps, int confidence, int status) {
        this.downlinkCapacityKbps = downlinkCapacityKbps;
        this.confidence = confidence;
        this.status = status;
        this.uplinkCapacityKbps = -1;
    }

    public LinkCapacityEstimate(int downlinkCapacityKbps, int uplinkCapacityKbps) {
        this.downlinkCapacityKbps = downlinkCapacityKbps;
        this.uplinkCapacityKbps = uplinkCapacityKbps;
        this.confidence = -1;
        this.status = -1;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{downlinkCapacityKbps=");
        stringBuilder.append(this.downlinkCapacityKbps);
        stringBuilder.append(", uplinkCapacityKbps=");
        stringBuilder.append(this.uplinkCapacityKbps);
        stringBuilder.append(", confidence=");
        stringBuilder.append(this.confidence);
        stringBuilder.append(", status=");
        stringBuilder.append(this.status);
        return stringBuilder.toString();
    }
}
