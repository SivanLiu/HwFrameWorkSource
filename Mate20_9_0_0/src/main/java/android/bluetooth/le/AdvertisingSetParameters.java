package android.bluetooth.le;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public final class AdvertisingSetParameters implements Parcelable {
    public static final Creator<AdvertisingSetParameters> CREATOR = new Creator<AdvertisingSetParameters>() {
        public AdvertisingSetParameters[] newArray(int size) {
            return new AdvertisingSetParameters[size];
        }

        public AdvertisingSetParameters createFromParcel(Parcel in) {
            return new AdvertisingSetParameters(in, null);
        }
    };
    public static final int INTERVAL_HIGH = 1600;
    public static final int INTERVAL_LOW = 160;
    public static final int INTERVAL_MAX = 16777215;
    public static final int INTERVAL_MEDIUM = 400;
    public static final int INTERVAL_MIN = 160;
    private static final int LIMITED_ADVERTISING_MAX_MILLIS = 180000;
    public static final int TX_POWER_HIGH = 1;
    public static final int TX_POWER_LOW = -15;
    public static final int TX_POWER_MAX = 1;
    public static final int TX_POWER_MEDIUM = -7;
    public static final int TX_POWER_MIN = -127;
    public static final int TX_POWER_ULTRA_LOW = -21;
    private final boolean mConnectable;
    private final boolean mIncludeTxPower;
    private final int mInterval;
    private final boolean mIsAnonymous;
    private final boolean mIsLegacy;
    private final int mPrimaryPhy;
    private final boolean mScannable;
    private final int mSecondaryPhy;
    private final int mTxPowerLevel;

    public static final class Builder {
        private boolean mConnectable = false;
        private boolean mIncludeTxPower = false;
        private int mInterval = 160;
        private boolean mIsAnonymous = false;
        private boolean mIsLegacy = false;
        private int mPrimaryPhy = 1;
        private boolean mScannable = false;
        private int mSecondaryPhy = 1;
        private int mTxPowerLevel = -7;

        public Builder setConnectable(boolean connectable) {
            this.mConnectable = connectable;
            return this;
        }

        public Builder setScannable(boolean scannable) {
            this.mScannable = scannable;
            return this;
        }

        public Builder setLegacyMode(boolean isLegacy) {
            this.mIsLegacy = isLegacy;
            return this;
        }

        public Builder setAnonymous(boolean isAnonymous) {
            this.mIsAnonymous = isAnonymous;
            return this;
        }

        public Builder setIncludeTxPower(boolean includeTxPower) {
            this.mIncludeTxPower = includeTxPower;
            return this;
        }

        public Builder setPrimaryPhy(int primaryPhy) {
            if (primaryPhy == 1 || primaryPhy == 3) {
                this.mPrimaryPhy = primaryPhy;
                return this;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("bad primaryPhy ");
            stringBuilder.append(primaryPhy);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        public Builder setSecondaryPhy(int secondaryPhy) {
            if (secondaryPhy == 1 || secondaryPhy == 2 || secondaryPhy == 3) {
                this.mSecondaryPhy = secondaryPhy;
                return this;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("bad secondaryPhy ");
            stringBuilder.append(secondaryPhy);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        public Builder setInterval(int interval) {
            if (interval < 160 || interval > AdvertisingSetParameters.INTERVAL_MAX) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unknown interval ");
                stringBuilder.append(interval);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            this.mInterval = interval;
            return this;
        }

        public Builder setTxPowerLevel(int txPowerLevel) {
            if (txPowerLevel < AdvertisingSetParameters.TX_POWER_MIN || txPowerLevel > 1) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unknown txPowerLevel ");
                stringBuilder.append(txPowerLevel);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            this.mTxPowerLevel = txPowerLevel;
            return this;
        }

        public AdvertisingSetParameters build() {
            if (this.mIsLegacy) {
                if (this.mIsAnonymous) {
                    throw new IllegalArgumentException("Legacy advertising can't be anonymous");
                } else if (this.mConnectable && !this.mScannable) {
                    throw new IllegalStateException("Legacy advertisement can't be connectable and non-scannable");
                } else if (this.mIncludeTxPower) {
                    throw new IllegalStateException("Legacy advertising can't include TX power level in header");
                }
            } else if (this.mConnectable && this.mScannable) {
                throw new IllegalStateException("Advertising can't be both connectable and scannable");
            } else if (this.mIsAnonymous && this.mConnectable) {
                throw new IllegalStateException("Advertising can't be both connectable and anonymous");
            }
            return new AdvertisingSetParameters(this.mConnectable, this.mScannable, this.mIsLegacy, this.mIsAnonymous, this.mIncludeTxPower, this.mPrimaryPhy, this.mSecondaryPhy, this.mInterval, this.mTxPowerLevel, null);
        }
    }

    /* synthetic */ AdvertisingSetParameters(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    /* synthetic */ AdvertisingSetParameters(boolean x0, boolean x1, boolean x2, boolean x3, boolean x4, int x5, int x6, int x7, int x8, AnonymousClass1 x9) {
        this(x0, x1, x2, x3, x4, x5, x6, x7, x8);
    }

    private AdvertisingSetParameters(boolean connectable, boolean scannable, boolean isLegacy, boolean isAnonymous, boolean includeTxPower, int primaryPhy, int secondaryPhy, int interval, int txPowerLevel) {
        this.mConnectable = connectable;
        this.mScannable = scannable;
        this.mIsLegacy = isLegacy;
        this.mIsAnonymous = isAnonymous;
        this.mIncludeTxPower = includeTxPower;
        this.mPrimaryPhy = primaryPhy;
        this.mSecondaryPhy = secondaryPhy;
        this.mInterval = interval;
        this.mTxPowerLevel = txPowerLevel;
    }

    private AdvertisingSetParameters(Parcel in) {
        boolean z = false;
        this.mConnectable = in.readInt() != 0;
        this.mScannable = in.readInt() != 0;
        this.mIsLegacy = in.readInt() != 0;
        this.mIsAnonymous = in.readInt() != 0;
        if (in.readInt() != 0) {
            z = true;
        }
        this.mIncludeTxPower = z;
        this.mPrimaryPhy = in.readInt();
        this.mSecondaryPhy = in.readInt();
        this.mInterval = in.readInt();
        this.mTxPowerLevel = in.readInt();
    }

    public boolean isConnectable() {
        return this.mConnectable;
    }

    public boolean isScannable() {
        return this.mScannable;
    }

    public boolean isLegacy() {
        return this.mIsLegacy;
    }

    public boolean isAnonymous() {
        return this.mIsAnonymous;
    }

    public boolean includeTxPower() {
        return this.mIncludeTxPower;
    }

    public int getPrimaryPhy() {
        return this.mPrimaryPhy;
    }

    public int getSecondaryPhy() {
        return this.mSecondaryPhy;
    }

    public int getInterval() {
        return this.mInterval;
    }

    public int getTxPowerLevel() {
        return this.mTxPowerLevel;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("AdvertisingSetParameters [connectable=");
        stringBuilder.append(this.mConnectable);
        stringBuilder.append(", isLegacy=");
        stringBuilder.append(this.mIsLegacy);
        stringBuilder.append(", isAnonymous=");
        stringBuilder.append(this.mIsAnonymous);
        stringBuilder.append(", includeTxPower=");
        stringBuilder.append(this.mIncludeTxPower);
        stringBuilder.append(", primaryPhy=");
        stringBuilder.append(this.mPrimaryPhy);
        stringBuilder.append(", secondaryPhy=");
        stringBuilder.append(this.mSecondaryPhy);
        stringBuilder.append(", interval=");
        stringBuilder.append(this.mInterval);
        stringBuilder.append(", txPowerLevel=");
        stringBuilder.append(this.mTxPowerLevel);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mConnectable);
        dest.writeInt(this.mScannable);
        dest.writeInt(this.mIsLegacy);
        dest.writeInt(this.mIsAnonymous);
        dest.writeInt(this.mIncludeTxPower);
        dest.writeInt(this.mPrimaryPhy);
        dest.writeInt(this.mSecondaryPhy);
        dest.writeInt(this.mInterval);
        dest.writeInt(this.mTxPowerLevel);
    }
}
