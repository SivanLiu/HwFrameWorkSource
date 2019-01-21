package android.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public final class BluetoothHidDeviceAppSdpSettings implements Parcelable {
    public static final Creator<BluetoothHidDeviceAppSdpSettings> CREATOR = new Creator<BluetoothHidDeviceAppSdpSettings>() {
        public BluetoothHidDeviceAppSdpSettings createFromParcel(Parcel in) {
            return new BluetoothHidDeviceAppSdpSettings(in.readString(), in.readString(), in.readString(), in.readByte(), in.createByteArray());
        }

        public BluetoothHidDeviceAppSdpSettings[] newArray(int size) {
            return new BluetoothHidDeviceAppSdpSettings[size];
        }
    };
    private final String mDescription;
    private final byte[] mDescriptors;
    private final String mName;
    private final String mProvider;
    private final byte mSubclass;

    public BluetoothHidDeviceAppSdpSettings(String name, String description, String provider, byte subclass, byte[] descriptors) {
        this.mName = name;
        this.mDescription = description;
        this.mProvider = provider;
        this.mSubclass = subclass;
        this.mDescriptors = (byte[]) descriptors.clone();
    }

    public String getName() {
        return this.mName;
    }

    public String getDescription() {
        return this.mDescription;
    }

    public String getProvider() {
        return this.mProvider;
    }

    public byte getSubclass() {
        return this.mSubclass;
    }

    public byte[] getDescriptors() {
        return this.mDescriptors;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.mName);
        out.writeString(this.mDescription);
        out.writeString(this.mProvider);
        out.writeByte(this.mSubclass);
        out.writeByteArray(this.mDescriptors);
    }
}
