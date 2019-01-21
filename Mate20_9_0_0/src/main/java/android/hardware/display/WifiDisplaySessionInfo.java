package android.hardware.display;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public final class WifiDisplaySessionInfo implements Parcelable {
    public static final Creator<WifiDisplaySessionInfo> CREATOR = new Creator<WifiDisplaySessionInfo>() {
        public WifiDisplaySessionInfo createFromParcel(Parcel in) {
            return new WifiDisplaySessionInfo(in.readInt() != 0, in.readInt(), in.readString(), in.readString(), in.readString());
        }

        public WifiDisplaySessionInfo[] newArray(int size) {
            return new WifiDisplaySessionInfo[size];
        }
    };
    private final boolean mClient;
    private final String mGroupId;
    private final String mIP;
    private final String mPassphrase;
    private final int mSessionId;

    public WifiDisplaySessionInfo() {
        this(true, 0, "", "", "");
    }

    public WifiDisplaySessionInfo(boolean client, int session, String group, String pp, String ip) {
        this.mClient = client;
        this.mSessionId = session;
        this.mGroupId = group;
        this.mPassphrase = pp;
        this.mIP = ip;
    }

    public boolean isClient() {
        return this.mClient;
    }

    public int getSessionId() {
        return this.mSessionId;
    }

    public String getGroupId() {
        return this.mGroupId;
    }

    public String getPassphrase() {
        return this.mPassphrase;
    }

    public String getIP() {
        return this.mIP;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mClient);
        dest.writeInt(this.mSessionId);
        dest.writeString(this.mGroupId);
        dest.writeString(this.mPassphrase);
        dest.writeString(this.mIP);
    }

    public int describeContents() {
        return 0;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WifiDisplaySessionInfo:\n    Client/Owner: ");
        stringBuilder.append(this.mClient ? "Client" : "Owner");
        stringBuilder.append("\n    GroupId: ");
        stringBuilder.append(this.mGroupId);
        stringBuilder.append("\n    Passphrase: ");
        stringBuilder.append(this.mPassphrase);
        stringBuilder.append("\n    SessionId: ");
        stringBuilder.append(this.mSessionId);
        stringBuilder.append("\n    IP Address: ");
        stringBuilder.append(this.mIP);
        return stringBuilder.toString();
    }
}
