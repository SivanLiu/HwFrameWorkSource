package android.net.wifi.aware;

import android.net.wifi.aware.TlvBufferUtils.TlvConstructor;
import android.net.wifi.aware.TlvBufferUtils.TlvIterable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import libcore.util.HexEncoding;

public final class SubscribeConfig implements Parcelable {
    public static final Creator<SubscribeConfig> CREATOR = new Creator<SubscribeConfig>() {
        public SubscribeConfig[] newArray(int size) {
            return new SubscribeConfig[size];
        }

        public SubscribeConfig createFromParcel(Parcel in) {
            byte[] serviceName = in.createByteArray();
            byte[] ssi = in.createByteArray();
            byte[] matchFilter = in.createByteArray();
            int subscribeType = in.readInt();
            int ttlSec = in.readInt();
            boolean enableTerminateNotification = in.readInt() != 0;
            int minDistanceMm = in.readInt();
            return new SubscribeConfig(serviceName, ssi, matchFilter, subscribeType, ttlSec, enableTerminateNotification, in.readInt() != 0, minDistanceMm, in.readInt() != 0, in.readInt());
        }
    };
    public static final int SUBSCRIBE_TYPE_ACTIVE = 1;
    public static final int SUBSCRIBE_TYPE_PASSIVE = 0;
    public final boolean mEnableTerminateNotification;
    public final byte[] mMatchFilter;
    public final int mMaxDistanceMm;
    public final boolean mMaxDistanceMmSet;
    public final int mMinDistanceMm;
    public final boolean mMinDistanceMmSet;
    public final byte[] mServiceName;
    public final byte[] mServiceSpecificInfo;
    public final int mSubscribeType;
    public final int mTtlSec;

    public static final class Builder {
        private boolean mEnableTerminateNotification = true;
        private byte[] mMatchFilter;
        private int mMaxDistanceMm;
        private boolean mMaxDistanceMmSet = false;
        private int mMinDistanceMm;
        private boolean mMinDistanceMmSet = false;
        private byte[] mServiceName;
        private byte[] mServiceSpecificInfo;
        private int mSubscribeType = 0;
        private int mTtlSec = 0;

        public Builder setServiceName(String serviceName) {
            if (serviceName != null) {
                this.mServiceName = serviceName.getBytes(StandardCharsets.UTF_8);
                return this;
            }
            throw new IllegalArgumentException("Invalid service name - must be non-null");
        }

        public Builder setServiceSpecificInfo(byte[] serviceSpecificInfo) {
            this.mServiceSpecificInfo = serviceSpecificInfo;
            return this;
        }

        public Builder setMatchFilter(List<byte[]> matchFilter) {
            this.mMatchFilter = new TlvConstructor(0, 1).allocateAndPut(matchFilter).getArray();
            return this;
        }

        public Builder setSubscribeType(int subscribeType) {
            if (subscribeType < 0 || subscribeType > 1) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid subscribeType - ");
                stringBuilder.append(subscribeType);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            this.mSubscribeType = subscribeType;
            return this;
        }

        public Builder setTtlSec(int ttlSec) {
            if (ttlSec >= 0) {
                this.mTtlSec = ttlSec;
                return this;
            }
            throw new IllegalArgumentException("Invalid ttlSec - must be non-negative");
        }

        public Builder setTerminateNotificationEnabled(boolean enable) {
            this.mEnableTerminateNotification = enable;
            return this;
        }

        public Builder setMinDistanceMm(int minDistanceMm) {
            this.mMinDistanceMm = minDistanceMm;
            this.mMinDistanceMmSet = true;
            return this;
        }

        public Builder setMaxDistanceMm(int maxDistanceMm) {
            this.mMaxDistanceMm = maxDistanceMm;
            this.mMaxDistanceMmSet = true;
            return this;
        }

        public SubscribeConfig build() {
            return new SubscribeConfig(this.mServiceName, this.mServiceSpecificInfo, this.mMatchFilter, this.mSubscribeType, this.mTtlSec, this.mEnableTerminateNotification, this.mMinDistanceMmSet, this.mMinDistanceMm, this.mMaxDistanceMmSet, this.mMaxDistanceMm);
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface SubscribeTypes {
    }

    public SubscribeConfig(byte[] serviceName, byte[] serviceSpecificInfo, byte[] matchFilter, int subscribeType, int ttlSec, boolean enableTerminateNotification, boolean minDistanceMmSet, int minDistanceMm, boolean maxDistanceMmSet, int maxDistanceMm) {
        this.mServiceName = serviceName;
        this.mServiceSpecificInfo = serviceSpecificInfo;
        this.mMatchFilter = matchFilter;
        this.mSubscribeType = subscribeType;
        this.mTtlSec = ttlSec;
        this.mEnableTerminateNotification = enableTerminateNotification;
        this.mMinDistanceMm = minDistanceMm;
        this.mMinDistanceMmSet = minDistanceMmSet;
        this.mMaxDistanceMm = maxDistanceMm;
        this.mMaxDistanceMmSet = maxDistanceMmSet;
    }

    public String toString() {
        String str;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SubscribeConfig [mServiceName='");
        if (this.mServiceName == null) {
            str = "<null>";
        } else {
            str = String.valueOf(HexEncoding.encode(this.mServiceName));
        }
        stringBuilder.append(str);
        stringBuilder.append(", mServiceName.length=");
        int i = 0;
        stringBuilder.append(this.mServiceName == null ? 0 : this.mServiceName.length);
        stringBuilder.append(", mServiceSpecificInfo='");
        stringBuilder.append(this.mServiceSpecificInfo == null ? "<null>" : String.valueOf(HexEncoding.encode(this.mServiceSpecificInfo)));
        stringBuilder.append(", mServiceSpecificInfo.length=");
        stringBuilder.append(this.mServiceSpecificInfo == null ? 0 : this.mServiceSpecificInfo.length);
        stringBuilder.append(", mMatchFilter=");
        stringBuilder.append(new TlvIterable(0, 1, this.mMatchFilter).toString());
        stringBuilder.append(", mMatchFilter.length=");
        if (this.mMatchFilter != null) {
            i = this.mMatchFilter.length;
        }
        stringBuilder.append(i);
        stringBuilder.append(", mSubscribeType=");
        stringBuilder.append(this.mSubscribeType);
        stringBuilder.append(", mTtlSec=");
        stringBuilder.append(this.mTtlSec);
        stringBuilder.append(", mEnableTerminateNotification=");
        stringBuilder.append(this.mEnableTerminateNotification);
        stringBuilder.append(", mMinDistanceMm=");
        stringBuilder.append(this.mMinDistanceMm);
        stringBuilder.append(", mMinDistanceMmSet=");
        stringBuilder.append(this.mMinDistanceMmSet);
        stringBuilder.append(", mMaxDistanceMm=");
        stringBuilder.append(this.mMaxDistanceMm);
        stringBuilder.append(", mMaxDistanceMmSet=");
        stringBuilder.append(this.mMaxDistanceMmSet);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(this.mServiceName);
        dest.writeByteArray(this.mServiceSpecificInfo);
        dest.writeByteArray(this.mMatchFilter);
        dest.writeInt(this.mSubscribeType);
        dest.writeInt(this.mTtlSec);
        dest.writeInt(this.mEnableTerminateNotification);
        dest.writeInt(this.mMinDistanceMm);
        dest.writeInt(this.mMinDistanceMmSet);
        dest.writeInt(this.mMaxDistanceMm);
        dest.writeInt(this.mMaxDistanceMmSet);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SubscribeConfig)) {
            return false;
        }
        SubscribeConfig lhs = (SubscribeConfig) o;
        if (!Arrays.equals(this.mServiceName, lhs.mServiceName) || !Arrays.equals(this.mServiceSpecificInfo, lhs.mServiceSpecificInfo) || !Arrays.equals(this.mMatchFilter, lhs.mMatchFilter) || this.mSubscribeType != lhs.mSubscribeType || this.mTtlSec != lhs.mTtlSec || this.mEnableTerminateNotification != lhs.mEnableTerminateNotification || this.mMinDistanceMmSet != lhs.mMinDistanceMmSet || this.mMaxDistanceMmSet != lhs.mMaxDistanceMmSet) {
            return false;
        }
        if (this.mMinDistanceMmSet && this.mMinDistanceMm != lhs.mMinDistanceMm) {
            return false;
        }
        if (!this.mMaxDistanceMmSet || this.mMaxDistanceMm == lhs.mMaxDistanceMm) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = Objects.hash(new Object[]{this.mServiceName, this.mServiceSpecificInfo, this.mMatchFilter, Integer.valueOf(this.mSubscribeType), Integer.valueOf(this.mTtlSec), Boolean.valueOf(this.mEnableTerminateNotification), Boolean.valueOf(this.mMinDistanceMmSet), Boolean.valueOf(this.mMaxDistanceMmSet)});
        if (this.mMinDistanceMmSet) {
            result = Objects.hash(new Object[]{Integer.valueOf(result), Integer.valueOf(this.mMinDistanceMm)});
        }
        if (!this.mMaxDistanceMmSet) {
            return result;
        }
        return Objects.hash(new Object[]{Integer.valueOf(result), Integer.valueOf(this.mMaxDistanceMm)});
    }

    public void assertValid(Characteristics characteristics, boolean rttSupported) throws IllegalArgumentException {
        WifiAwareUtils.validateServiceName(this.mServiceName);
        if (!TlvBufferUtils.isValid(this.mMatchFilter, 0, 1)) {
            throw new IllegalArgumentException("Invalid matchFilter configuration - LV fields do not match up to length");
        } else if (this.mSubscribeType < 0 || this.mSubscribeType > 1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid subscribeType - ");
            stringBuilder.append(this.mSubscribeType);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (this.mTtlSec >= 0) {
            if (characteristics != null) {
                int maxServiceNameLength = characteristics.getMaxServiceNameLength();
                if (maxServiceNameLength == 0 || this.mServiceName.length <= maxServiceNameLength) {
                    int maxServiceSpecificInfoLength = characteristics.getMaxServiceSpecificInfoLength();
                    if (maxServiceSpecificInfoLength == 0 || this.mServiceSpecificInfo == null || this.mServiceSpecificInfo.length <= maxServiceSpecificInfoLength) {
                        int maxMatchFilterLength = characteristics.getMaxMatchFilterLength();
                        if (!(maxMatchFilterLength == 0 || this.mMatchFilter == null || this.mMatchFilter.length <= maxMatchFilterLength)) {
                            throw new IllegalArgumentException("Match filter longer than supported by device characteristics");
                        }
                    }
                    throw new IllegalArgumentException("Service specific info longer than supported by device characteristics");
                }
                throw new IllegalArgumentException("Service name longer than supported by device characteristics");
            }
            if (this.mMinDistanceMmSet && this.mMinDistanceMm < 0) {
                throw new IllegalArgumentException("Minimum distance must be non-negative");
            } else if (this.mMaxDistanceMmSet && this.mMaxDistanceMm < 0) {
                throw new IllegalArgumentException("Maximum distance must be non-negative");
            } else if (this.mMinDistanceMmSet && this.mMaxDistanceMmSet && this.mMaxDistanceMm <= this.mMinDistanceMm) {
                throw new IllegalArgumentException("Maximum distance must be greater than minimum distance");
            } else if (!rttSupported) {
                if (this.mMinDistanceMmSet || this.mMaxDistanceMmSet) {
                    throw new IllegalArgumentException("Ranging is not supported");
                }
            }
        } else {
            throw new IllegalArgumentException("Invalid ttlSec - must be non-negative");
        }
    }
}
