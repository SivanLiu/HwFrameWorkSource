package android.hardware.hdmi;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

@SystemApi
public final class HdmiHotplugEvent implements Parcelable {
    public static final Creator<HdmiHotplugEvent> CREATOR = new Creator<HdmiHotplugEvent>() {
        public HdmiHotplugEvent createFromParcel(Parcel p) {
            int port = p.readInt();
            boolean z = true;
            if (p.readByte() != (byte) 1) {
                z = false;
            }
            return new HdmiHotplugEvent(port, z);
        }

        public HdmiHotplugEvent[] newArray(int size) {
            return new HdmiHotplugEvent[size];
        }
    };
    private final boolean mConnected;
    private final int mPort;

    public HdmiHotplugEvent(int port, boolean connected) {
        this.mPort = port;
        this.mConnected = connected;
    }

    public int getPort() {
        return this.mPort;
    }

    public boolean isConnected() {
        return this.mConnected;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mPort);
        dest.writeByte((byte) this.mConnected);
    }
}
