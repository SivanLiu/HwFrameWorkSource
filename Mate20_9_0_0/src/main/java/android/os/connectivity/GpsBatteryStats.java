package android.os.connectivity;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.Arrays;

public final class GpsBatteryStats implements Parcelable {
    public static final Creator<GpsBatteryStats> CREATOR = new Creator<GpsBatteryStats>() {
        public GpsBatteryStats createFromParcel(Parcel in) {
            return new GpsBatteryStats(in, null);
        }

        public GpsBatteryStats[] newArray(int size) {
            return new GpsBatteryStats[size];
        }
    };
    private long mEnergyConsumedMaMs;
    private long mLoggingDurationMs;
    private long[] mTimeInGpsSignalQualityLevel;

    /* synthetic */ GpsBatteryStats(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public GpsBatteryStats() {
        initialize();
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(this.mLoggingDurationMs);
        out.writeLong(this.mEnergyConsumedMaMs);
        out.writeLongArray(this.mTimeInGpsSignalQualityLevel);
    }

    public void readFromParcel(Parcel in) {
        this.mLoggingDurationMs = in.readLong();
        this.mEnergyConsumedMaMs = in.readLong();
        in.readLongArray(this.mTimeInGpsSignalQualityLevel);
    }

    public long getLoggingDurationMs() {
        return this.mLoggingDurationMs;
    }

    public long getEnergyConsumedMaMs() {
        return this.mEnergyConsumedMaMs;
    }

    public long[] getTimeInGpsSignalQualityLevel() {
        return this.mTimeInGpsSignalQualityLevel;
    }

    public void setLoggingDurationMs(long t) {
        this.mLoggingDurationMs = t;
    }

    public void setEnergyConsumedMaMs(long e) {
        this.mEnergyConsumedMaMs = e;
    }

    public void setTimeInGpsSignalQualityLevel(long[] t) {
        this.mTimeInGpsSignalQualityLevel = Arrays.copyOfRange(t, 0, Math.min(t.length, 2));
    }

    public int describeContents() {
        return 0;
    }

    private GpsBatteryStats(Parcel in) {
        initialize();
        readFromParcel(in);
    }

    private void initialize() {
        this.mLoggingDurationMs = 0;
        this.mEnergyConsumedMaMs = 0;
        this.mTimeInGpsSignalQualityLevel = new long[2];
    }
}
