package android.rms.iaware;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class DumpData implements Parcelable, Comparable<DumpData> {
    public static final Creator<DumpData> CREATOR = new Creator<DumpData>() {
        public DumpData createFromParcel(Parcel source) {
            return new DumpData(source.readLong(), source.readInt(), source.readString(), source.readInt(), source.readString());
        }

        public DumpData[] newArray(int size) {
            return new DumpData[size];
        }
    };
    private int mExeTime;
    private int mFeatureId;
    private String mOperation;
    private long mOperationTimeStamp;
    private String mReason;

    public DumpData(long time, int featureid, String operation, int exetime, String reason) {
        this.mOperationTimeStamp = time;
        this.mFeatureId = featureid;
        this.mOperation = operation;
        this.mExeTime = exetime;
        this.mReason = reason;
    }

    public int compareTo(DumpData other) {
        if (other == null) {
            return 1;
        }
        if (this.mOperationTimeStamp < other.getTime()) {
            return -1;
        }
        if (this.mOperationTimeStamp > other.getTime()) {
            return 1;
        }
        return 0;
    }

    public boolean equals(Object other) {
        boolean z = false;
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        if (this == other) {
            return true;
        }
        DumpData otherData = (DumpData) other;
        if (this.mOperationTimeStamp == otherData.getTime() && this.mFeatureId == otherData.getFeatureId() && this.mExeTime == otherData.getExeTime() && (((this.mOperation != null && this.mOperation.equals(otherData.getOperation())) || (this.mOperation == null && otherData.getOperation() == null)) && ((this.mReason != null && this.mReason.equals(otherData.getReson())) || (this.mReason == null && otherData.getReson() == null)))) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        int i = 0;
        int result = 31 * ((31 * ((31 * ((31 * ((31 * 1) + ((int) (this.mOperationTimeStamp ^ (this.mOperationTimeStamp >>> 32))))) + this.mFeatureId)) + (this.mOperation != null ? this.mOperation.hashCode() : 0))) + this.mExeTime);
        if (this.mReason != null) {
            i = this.mReason.hashCode();
        }
        return result + i;
    }

    public long getTime() {
        return this.mOperationTimeStamp;
    }

    public void setTime(long time) {
        this.mOperationTimeStamp = time;
    }

    public int getFeatureId() {
        return this.mFeatureId;
    }

    public void setFeatureId(int featureId) {
        this.mFeatureId = featureId;
    }

    public String getOperation() {
        return this.mOperation;
    }

    public void setOperation(String operation) {
        this.mOperation = operation;
    }

    public String getReson() {
        return this.mReason;
    }

    public void setReason(String reason) {
        this.mReason = reason;
    }

    public int getExeTime() {
        return this.mExeTime;
    }

    public void setExeTime(int exetime) {
        this.mExeTime = exetime;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.mOperationTimeStamp);
        dest.writeInt(this.mFeatureId);
        dest.writeString(this.mOperation);
        dest.writeInt(this.mExeTime);
        dest.writeString(this.mReason);
    }

    public int describeContents() {
        return 0;
    }
}
