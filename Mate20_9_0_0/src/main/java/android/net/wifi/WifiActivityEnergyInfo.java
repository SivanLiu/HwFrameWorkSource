package android.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.Arrays;

public final class WifiActivityEnergyInfo implements Parcelable {
    public static final Creator<WifiActivityEnergyInfo> CREATOR = new Creator<WifiActivityEnergyInfo>() {
        public WifiActivityEnergyInfo createFromParcel(Parcel in) {
            return new WifiActivityEnergyInfo(in.readLong(), in.readInt(), in.readLong(), in.createLongArray(), in.readLong(), in.readLong(), in.readLong(), in.readLong());
        }

        public WifiActivityEnergyInfo[] newArray(int size) {
            return new WifiActivityEnergyInfo[size];
        }
    };
    public static final int STACK_STATE_INVALID = 0;
    public static final int STACK_STATE_STATE_ACTIVE = 1;
    public static final int STACK_STATE_STATE_IDLE = 3;
    public static final int STACK_STATE_STATE_SCANNING = 2;
    public long mControllerEnergyUsed;
    public long mControllerIdleTimeMs;
    public long mControllerRxTimeMs;
    public long mControllerScanTimeMs;
    public long mControllerTxTimeMs;
    public long[] mControllerTxTimePerLevelMs;
    public int mStackState;
    public long mTimestamp;

    public WifiActivityEnergyInfo(long timestamp, int stackState, long txTime, long[] txTimePerLevel, long rxTime, long scanTime, long idleTime, long energyUsed) {
        this.mTimestamp = timestamp;
        this.mStackState = stackState;
        this.mControllerTxTimeMs = txTime;
        this.mControllerTxTimePerLevelMs = txTimePerLevel;
        this.mControllerRxTimeMs = rxTime;
        this.mControllerScanTimeMs = scanTime;
        this.mControllerIdleTimeMs = idleTime;
        this.mControllerEnergyUsed = energyUsed;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WifiActivityEnergyInfo{ timestamp=");
        stringBuilder.append(this.mTimestamp);
        stringBuilder.append(" mStackState=");
        stringBuilder.append(this.mStackState);
        stringBuilder.append(" mControllerTxTimeMs=");
        stringBuilder.append(this.mControllerTxTimeMs);
        stringBuilder.append(" mControllerTxTimePerLevelMs=");
        stringBuilder.append(Arrays.toString(this.mControllerTxTimePerLevelMs));
        stringBuilder.append(" mControllerRxTimeMs=");
        stringBuilder.append(this.mControllerRxTimeMs);
        stringBuilder.append(" mControllerScanTimeMs=");
        stringBuilder.append(this.mControllerScanTimeMs);
        stringBuilder.append(" mControllerIdleTimeMs=");
        stringBuilder.append(this.mControllerIdleTimeMs);
        stringBuilder.append(" mControllerEnergyUsed=");
        stringBuilder.append(this.mControllerEnergyUsed);
        stringBuilder.append(" }");
        return stringBuilder.toString();
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(this.mTimestamp);
        out.writeInt(this.mStackState);
        out.writeLong(this.mControllerTxTimeMs);
        out.writeLongArray(this.mControllerTxTimePerLevelMs);
        out.writeLong(this.mControllerRxTimeMs);
        out.writeLong(this.mControllerScanTimeMs);
        out.writeLong(this.mControllerIdleTimeMs);
        out.writeLong(this.mControllerEnergyUsed);
    }

    public int describeContents() {
        return 0;
    }

    public int getStackState() {
        return this.mStackState;
    }

    public long getControllerTxTimeMillis() {
        return this.mControllerTxTimeMs;
    }

    public long getControllerTxTimeMillisAtLevel(int level) {
        if (level < this.mControllerTxTimePerLevelMs.length) {
            return this.mControllerTxTimePerLevelMs[level];
        }
        return 0;
    }

    public long getControllerRxTimeMillis() {
        return this.mControllerRxTimeMs;
    }

    public long getControllerScanTimeMillis() {
        return this.mControllerScanTimeMs;
    }

    public long getControllerIdleTimeMillis() {
        return this.mControllerIdleTimeMs;
    }

    public long getControllerEnergyUsed() {
        return this.mControllerEnergyUsed;
    }

    public long getTimeStamp() {
        return this.mTimestamp;
    }

    public boolean isValid() {
        return this.mControllerTxTimeMs >= 0 && this.mControllerRxTimeMs >= 0 && this.mControllerScanTimeMs >= 0 && this.mControllerIdleTimeMs >= 0;
    }
}
