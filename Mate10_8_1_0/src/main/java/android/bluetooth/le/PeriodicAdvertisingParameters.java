package android.bluetooth.le;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public final class PeriodicAdvertisingParameters implements Parcelable {
    public static final Creator<PeriodicAdvertisingParameters> CREATOR = new Creator<PeriodicAdvertisingParameters>() {
        public PeriodicAdvertisingParameters[] newArray(int size) {
            return new PeriodicAdvertisingParameters[size];
        }

        public PeriodicAdvertisingParameters createFromParcel(Parcel in) {
            return new PeriodicAdvertisingParameters(in);
        }
    };
    private static final int INTERVAL_MAX = 65519;
    private static final int INTERVAL_MIN = 80;
    private final boolean includeTxPower;
    private final int interval;

    public static final class Builder {
        private boolean includeTxPower = false;
        private int interval = PeriodicAdvertisingParameters.INTERVAL_MAX;

        public Builder setIncludeTxPower(boolean includeTxPower) {
            this.includeTxPower = includeTxPower;
            return this;
        }

        public Builder setInterval(int interval) {
            if (interval < 80 || interval > PeriodicAdvertisingParameters.INTERVAL_MAX) {
                throw new IllegalArgumentException("Invalid interval (must be 80-65519)");
            }
            this.interval = interval;
            return this;
        }

        public PeriodicAdvertisingParameters build() {
            return new PeriodicAdvertisingParameters(this.includeTxPower, this.interval);
        }
    }

    private PeriodicAdvertisingParameters(boolean includeTxPower, int interval) {
        this.includeTxPower = includeTxPower;
        this.interval = interval;
    }

    private PeriodicAdvertisingParameters(Parcel in) {
        boolean z = false;
        if (in.readInt() != 0) {
            z = true;
        }
        this.includeTxPower = z;
        this.interval = in.readInt();
    }

    public boolean getIncludeTxPower() {
        return this.includeTxPower;
    }

    public int getInterval() {
        return this.interval;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.includeTxPower ? 1 : 0);
        dest.writeInt(this.interval);
    }
}
