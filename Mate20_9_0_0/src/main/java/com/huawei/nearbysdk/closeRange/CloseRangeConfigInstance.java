package com.huawei.nearbysdk.closeRange;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class CloseRangeConfigInstance implements Parcelable {
    public static final Creator<CloseRangeConfigInstance> CREATOR = new Creator<CloseRangeConfigInstance>() {
        public CloseRangeConfigInstance createFromParcel(Parcel source) {
            return new CloseRangeConfigInstance(source.readString(), source.readString(), source.readInt(), source.readInt(), source.readInt(), source.readInt());
        }

        public CloseRangeConfigInstance[] newArray(int size) {
            return new CloseRangeConfigInstance[size];
        }
    };
    private static final String TAG = "CloseRangeConfigInstance";
    private final int findNearbyCount;
    private final int findNearbyTime;
    private final String modelId;
    private final String name;
    private final int rssiLowerLimit;
    private final int rssiReference;

    public CloseRangeConfigInstance(String name, String modelId, int rssiReference, int rssiLowerLimit, int findNearbyTime, int findNearbyCount) {
        this.name = name;
        this.modelId = modelId;
        this.rssiReference = rssiReference;
        this.rssiLowerLimit = rssiLowerLimit;
        this.findNearbyTime = findNearbyTime;
        this.findNearbyCount = findNearbyCount;
    }

    public String getName() {
        return this.name;
    }

    public String getModelId() {
        return this.modelId;
    }

    public int getRssiReference() {
        return this.rssiReference;
    }

    public int getRssiLowerLimit() {
        return this.rssiLowerLimit;
    }

    public int getFindNearbyTime() {
        return this.findNearbyTime;
    }

    public int getFindNearbyCount() {
        return this.findNearbyCount;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.modelId);
        dest.writeInt(this.rssiReference);
        dest.writeInt(this.rssiLowerLimit);
        dest.writeInt(this.findNearbyTime);
        dest.writeInt(this.findNearbyCount);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CloseRangeConfigInstance)) {
            return false;
        }
        CloseRangeConfigInstance that = (CloseRangeConfigInstance) o;
        if (getRssiReference() == that.getRssiReference() && getName().equals(that.getName())) {
            return getModelId().equals(that.getModelId());
        }
        return false;
    }

    public int hashCode() {
        return (31 * ((31 * ((31 * ((31 * ((31 * getName().hashCode()) + getModelId().hashCode())) + getRssiReference())) + getRssiLowerLimit())) + getFindNearbyTime())) + getFindNearbyCount();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CloseRangeConfigInstance{name='");
        stringBuilder.append(this.name);
        stringBuilder.append('\'');
        stringBuilder.append(", modelId='");
        stringBuilder.append(this.modelId);
        stringBuilder.append('\'');
        stringBuilder.append(", rssiReference=");
        stringBuilder.append(this.rssiReference);
        stringBuilder.append(", rssiLowerLimit=");
        stringBuilder.append(this.rssiLowerLimit);
        stringBuilder.append(", FindNearbyTime=");
        stringBuilder.append(this.findNearbyTime);
        stringBuilder.append(", FindNearbyCount=");
        stringBuilder.append(this.findNearbyCount);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
