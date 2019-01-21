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

public final class PublishConfig implements Parcelable {
    public static final Creator<PublishConfig> CREATOR = new Creator<PublishConfig>() {
        public PublishConfig[] newArray(int size) {
            return new PublishConfig[size];
        }

        public PublishConfig createFromParcel(Parcel in) {
            return new PublishConfig(in.createByteArray(), in.createByteArray(), in.createByteArray(), in.readInt(), in.readInt(), in.readInt() != 0, in.readInt() != 0);
        }
    };
    public static final int PUBLISH_TYPE_SOLICITED = 1;
    public static final int PUBLISH_TYPE_UNSOLICITED = 0;
    public final boolean mEnableRanging;
    public final boolean mEnableTerminateNotification;
    public final byte[] mMatchFilter;
    public final int mPublishType;
    public final byte[] mServiceName;
    public final byte[] mServiceSpecificInfo;
    public final int mTtlSec;

    public static final class Builder {
        private boolean mEnableRanging = false;
        private boolean mEnableTerminateNotification = true;
        private byte[] mMatchFilter;
        private int mPublishType = 0;
        private byte[] mServiceName;
        private byte[] mServiceSpecificInfo;
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

        public Builder setPublishType(int publishType) {
            if (publishType < 0 || publishType > 1) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid publishType - ");
                stringBuilder.append(publishType);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            this.mPublishType = publishType;
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

        public Builder setRangingEnabled(boolean enable) {
            this.mEnableRanging = enable;
            return this;
        }

        public PublishConfig build() {
            return new PublishConfig(this.mServiceName, this.mServiceSpecificInfo, this.mMatchFilter, this.mPublishType, this.mTtlSec, this.mEnableTerminateNotification, this.mEnableRanging);
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface PublishTypes {
    }

    public PublishConfig(byte[] serviceName, byte[] serviceSpecificInfo, byte[] matchFilter, int publishType, int ttlSec, boolean enableTerminateNotification, boolean enableRanging) {
        this.mServiceName = serviceName;
        this.mServiceSpecificInfo = serviceSpecificInfo;
        this.mMatchFilter = matchFilter;
        this.mPublishType = publishType;
        this.mTtlSec = ttlSec;
        this.mEnableTerminateNotification = enableTerminateNotification;
        this.mEnableRanging = enableRanging;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PublishConfig [mServiceName='");
        stringBuilder.append(this.mServiceName == null ? "<null>" : String.valueOf(HexEncoding.encode(this.mServiceName)));
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
        stringBuilder.append(", mPublishType=");
        stringBuilder.append(this.mPublishType);
        stringBuilder.append(", mTtlSec=");
        stringBuilder.append(this.mTtlSec);
        stringBuilder.append(", mEnableTerminateNotification=");
        stringBuilder.append(this.mEnableTerminateNotification);
        stringBuilder.append(", mEnableRanging=");
        stringBuilder.append(this.mEnableRanging);
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
        dest.writeInt(this.mPublishType);
        dest.writeInt(this.mTtlSec);
        dest.writeInt(this.mEnableTerminateNotification);
        dest.writeInt(this.mEnableRanging);
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (!(o instanceof PublishConfig)) {
            return false;
        }
        PublishConfig lhs = (PublishConfig) o;
        if (!(Arrays.equals(this.mServiceName, lhs.mServiceName) && Arrays.equals(this.mServiceSpecificInfo, lhs.mServiceSpecificInfo) && Arrays.equals(this.mMatchFilter, lhs.mMatchFilter) && this.mPublishType == lhs.mPublishType && this.mTtlSec == lhs.mTtlSec && this.mEnableTerminateNotification == lhs.mEnableTerminateNotification && this.mEnableRanging == lhs.mEnableRanging)) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.mServiceName, this.mServiceSpecificInfo, this.mMatchFilter, Integer.valueOf(this.mPublishType), Integer.valueOf(this.mTtlSec), Boolean.valueOf(this.mEnableTerminateNotification), Boolean.valueOf(this.mEnableRanging)});
    }

    public void assertValid(Characteristics characteristics, boolean rttSupported) throws IllegalArgumentException {
        WifiAwareUtils.validateServiceName(this.mServiceName);
        if (!TlvBufferUtils.isValid(this.mMatchFilter, 0, 1)) {
            throw new IllegalArgumentException("Invalid txFilter configuration - LV fields do not match up to length");
        } else if (this.mPublishType < 0 || this.mPublishType > 1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid publishType - ");
            stringBuilder.append(this.mPublishType);
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
            if (!rttSupported && this.mEnableRanging) {
                throw new IllegalArgumentException("Ranging is not supported");
            }
        } else {
            throw new IllegalArgumentException("Invalid ttlSec - must be non-negative");
        }
    }
}
