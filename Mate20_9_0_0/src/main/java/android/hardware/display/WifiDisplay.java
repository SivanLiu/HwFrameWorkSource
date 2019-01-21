package android.hardware.display;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.Objects;

public final class WifiDisplay implements Parcelable {
    public static final Creator<WifiDisplay> CREATOR = new Creator<WifiDisplay>() {
        public WifiDisplay createFromParcel(Parcel in) {
            return new WifiDisplay(in.readString(), in.readString(), in.readString(), in.readInt() != 0, in.readInt() != 0, in.readInt() != 0);
        }

        public WifiDisplay[] newArray(int size) {
            return size == 0 ? WifiDisplay.EMPTY_ARRAY : new WifiDisplay[size];
        }
    };
    public static final WifiDisplay[] EMPTY_ARRAY = new WifiDisplay[0];
    private final boolean mCanConnect;
    private final String mDeviceAddress;
    private final String mDeviceAlias;
    private final String mDeviceName;
    private final boolean mIsAvailable;
    private final boolean mIsRemembered;

    public WifiDisplay(String deviceAddress, String deviceName, String deviceAlias, boolean available, boolean canConnect, boolean remembered) {
        if (deviceAddress == null) {
            throw new IllegalArgumentException("deviceAddress must not be null");
        } else if (deviceName != null) {
            this.mDeviceAddress = deviceAddress;
            this.mDeviceName = deviceName;
            this.mDeviceAlias = deviceAlias;
            this.mIsAvailable = available;
            this.mCanConnect = canConnect;
            this.mIsRemembered = remembered;
        } else {
            throw new IllegalArgumentException("deviceName must not be null");
        }
    }

    public String getDeviceAddress() {
        return this.mDeviceAddress;
    }

    public String getDeviceName() {
        return this.mDeviceName;
    }

    public String getDeviceAlias() {
        return this.mDeviceAlias;
    }

    public boolean isAvailable() {
        return this.mIsAvailable;
    }

    public boolean canConnect() {
        return this.mCanConnect;
    }

    public boolean isRemembered() {
        return this.mIsRemembered;
    }

    public String getFriendlyDisplayName() {
        return this.mDeviceAlias != null ? this.mDeviceAlias : this.mDeviceName;
    }

    public boolean equals(Object o) {
        return (o instanceof WifiDisplay) && equals((WifiDisplay) o);
    }

    public boolean equals(WifiDisplay other) {
        return other != null && this.mDeviceAddress.equals(other.mDeviceAddress) && this.mDeviceName.equals(other.mDeviceName) && Objects.equals(this.mDeviceAlias, other.mDeviceAlias);
    }

    public boolean hasSameAddress(WifiDisplay other) {
        return other != null && this.mDeviceAddress.equals(other.mDeviceAddress);
    }

    public int hashCode() {
        return this.mDeviceAddress.hashCode();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mDeviceAddress);
        dest.writeString(this.mDeviceName);
        dest.writeString(this.mDeviceAlias);
        dest.writeInt(this.mIsAvailable);
        dest.writeInt(this.mCanConnect);
        dest.writeInt(this.mIsRemembered);
    }

    public int describeContents() {
        return 0;
    }

    public String toString() {
        StringBuilder stringBuilder;
        String result = new StringBuilder();
        result.append(this.mDeviceName);
        result.append(" (");
        result.append(this.mDeviceAddress);
        result.append(")");
        result = result.toString();
        if (this.mDeviceAlias != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(result);
            stringBuilder.append(", alias ");
            stringBuilder.append(this.mDeviceAlias);
            result = stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(result);
        stringBuilder.append(", isAvailable ");
        stringBuilder.append(this.mIsAvailable);
        stringBuilder.append(", canConnect ");
        stringBuilder.append(this.mCanConnect);
        stringBuilder.append(", isRemembered ");
        stringBuilder.append(this.mIsRemembered);
        return stringBuilder.toString();
    }
}
