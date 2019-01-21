package android.os;

import android.os.Parcelable.Creator;

public class BatteryProperty implements Parcelable {
    public static final Creator<BatteryProperty> CREATOR = new Creator<BatteryProperty>() {
        public BatteryProperty createFromParcel(Parcel p) {
            return new BatteryProperty(p, null);
        }

        public BatteryProperty[] newArray(int size) {
            return new BatteryProperty[size];
        }
    };
    private long mValueLong;

    /* synthetic */ BatteryProperty(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public BatteryProperty() {
        this.mValueLong = Long.MIN_VALUE;
    }

    public long getLong() {
        return this.mValueLong;
    }

    public void setLong(long val) {
        this.mValueLong = val;
    }

    private BatteryProperty(Parcel p) {
        readFromParcel(p);
    }

    public void readFromParcel(Parcel p) {
        this.mValueLong = p.readLong();
    }

    public void writeToParcel(Parcel p, int flags) {
        p.writeLong(this.mValueLong);
    }

    public int describeContents() {
        return 0;
    }
}
