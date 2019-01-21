package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import com.android.internal.annotations.VisibleForTesting;
import java.util.EnumMap;

public class NetworkInfo implements Parcelable {
    public static final Creator<NetworkInfo> CREATOR = new Creator<NetworkInfo>() {
        public NetworkInfo createFromParcel(Parcel in) {
            NetworkInfo netInfo = new NetworkInfo(in.readInt(), in.readInt(), in.readString(), in.readString());
            netInfo.mState = State.valueOf(in.readString());
            netInfo.mDetailedState = DetailedState.valueOf(in.readString());
            boolean z = false;
            netInfo.mIsFailover = in.readInt() != 0;
            netInfo.mIsAvailable = in.readInt() != 0;
            if (in.readInt() != 0) {
                z = true;
            }
            netInfo.mIsRoaming = z;
            netInfo.mReason = in.readString();
            netInfo.mExtraInfo = in.readString();
            return netInfo;
        }

        public NetworkInfo[] newArray(int size) {
            return new NetworkInfo[size];
        }
    };
    private static final EnumMap<DetailedState, State> stateMap = new EnumMap(DetailedState.class);
    private DetailedState mDetailedState;
    private String mExtraInfo;
    private boolean mIsAvailable;
    private boolean mIsFailover;
    private boolean mIsRoaming;
    private int mNetworkType;
    private String mReason;
    private State mState;
    private int mSubtype;
    private String mSubtypeName;
    private String mTypeName;

    public enum DetailedState {
        IDLE,
        SCANNING,
        CONNECTING,
        AUTHENTICATING,
        OBTAINING_IPADDR,
        CONNECTED,
        SUSPENDED,
        DISCONNECTING,
        DISCONNECTED,
        FAILED,
        BLOCKED,
        VERIFYING_POOR_LINK,
        CAPTIVE_PORTAL_CHECK
    }

    public enum State {
        CONNECTING,
        CONNECTED,
        SUSPENDED,
        DISCONNECTING,
        DISCONNECTED,
        UNKNOWN
    }

    static {
        stateMap.put(DetailedState.IDLE, State.DISCONNECTED);
        stateMap.put(DetailedState.SCANNING, State.DISCONNECTED);
        stateMap.put(DetailedState.CONNECTING, State.CONNECTING);
        stateMap.put(DetailedState.AUTHENTICATING, State.CONNECTING);
        stateMap.put(DetailedState.OBTAINING_IPADDR, State.CONNECTING);
        stateMap.put(DetailedState.VERIFYING_POOR_LINK, State.CONNECTING);
        stateMap.put(DetailedState.CAPTIVE_PORTAL_CHECK, State.CONNECTING);
        stateMap.put(DetailedState.CONNECTED, State.CONNECTED);
        stateMap.put(DetailedState.SUSPENDED, State.SUSPENDED);
        stateMap.put(DetailedState.DISCONNECTING, State.DISCONNECTING);
        stateMap.put(DetailedState.DISCONNECTED, State.DISCONNECTED);
        stateMap.put(DetailedState.FAILED, State.DISCONNECTED);
        stateMap.put(DetailedState.BLOCKED, State.DISCONNECTED);
    }

    public NetworkInfo(int type, int subtype, String typeName, String subtypeName) {
        if (ConnectivityManager.isNetworkTypeValid(type) || type == -1) {
            this.mNetworkType = type;
            this.mSubtype = subtype;
            this.mTypeName = typeName;
            this.mSubtypeName = subtypeName;
            setDetailedState(DetailedState.IDLE, null, null);
            this.mState = State.UNKNOWN;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid network type: ");
        stringBuilder.append(type);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public NetworkInfo(NetworkInfo source) {
        if (source != null) {
            synchronized (source) {
                this.mNetworkType = source.mNetworkType;
                this.mSubtype = source.mSubtype;
                this.mTypeName = source.mTypeName;
                this.mSubtypeName = source.mSubtypeName;
                this.mState = source.mState;
                this.mDetailedState = source.mDetailedState;
                this.mReason = source.mReason;
                this.mExtraInfo = source.mExtraInfo;
                this.mIsFailover = source.mIsFailover;
                this.mIsAvailable = source.mIsAvailable;
                this.mIsRoaming = source.mIsRoaming;
            }
        }
    }

    @Deprecated
    public int getType() {
        int i;
        synchronized (this) {
            i = this.mNetworkType;
        }
        return i;
    }

    @Deprecated
    public void setType(int type) {
        synchronized (this) {
            this.mNetworkType = type;
        }
    }

    public int getSubtype() {
        int i;
        synchronized (this) {
            i = this.mSubtype;
        }
        return i;
    }

    public void setSubtype(int subtype, String subtypeName) {
        synchronized (this) {
            this.mSubtype = subtype;
            this.mSubtypeName = subtypeName;
        }
    }

    @Deprecated
    public String getTypeName() {
        String str;
        synchronized (this) {
            str = this.mTypeName;
        }
        return str;
    }

    public String getSubtypeName() {
        String str;
        synchronized (this) {
            str = this.mSubtypeName;
        }
        return str;
    }

    @Deprecated
    public boolean isConnectedOrConnecting() {
        boolean z;
        synchronized (this) {
            if (this.mState != State.CONNECTED) {
                if (this.mState != State.CONNECTING) {
                    z = false;
                }
            }
            z = true;
        }
        return z;
    }

    public boolean isConnected() {
        boolean z;
        synchronized (this) {
            z = this.mState == State.CONNECTED;
        }
        return z;
    }

    @Deprecated
    public boolean isAvailable() {
        boolean z;
        synchronized (this) {
            z = this.mIsAvailable;
        }
        return z;
    }

    @Deprecated
    public void setIsAvailable(boolean isAvailable) {
        synchronized (this) {
            this.mIsAvailable = isAvailable;
        }
    }

    @Deprecated
    public boolean isFailover() {
        boolean z;
        synchronized (this) {
            z = this.mIsFailover;
        }
        return z;
    }

    @Deprecated
    public void setFailover(boolean isFailover) {
        synchronized (this) {
            this.mIsFailover = isFailover;
        }
    }

    @Deprecated
    public boolean isRoaming() {
        boolean z;
        synchronized (this) {
            z = this.mIsRoaming;
        }
        return z;
    }

    @VisibleForTesting
    @Deprecated
    public void setRoaming(boolean isRoaming) {
        synchronized (this) {
            this.mIsRoaming = isRoaming;
        }
    }

    @Deprecated
    public State getState() {
        State state;
        synchronized (this) {
            state = this.mState;
        }
        return state;
    }

    public DetailedState getDetailedState() {
        DetailedState detailedState;
        synchronized (this) {
            detailedState = this.mDetailedState;
        }
        return detailedState;
    }

    @Deprecated
    public void setDetailedState(DetailedState detailedState, String reason, String extraInfo) {
        synchronized (this) {
            this.mDetailedState = detailedState;
            this.mState = (State) stateMap.get(detailedState);
            this.mReason = reason;
            this.mExtraInfo = extraInfo;
        }
    }

    public void setExtraInfo(String extraInfo) {
        synchronized (this) {
            this.mExtraInfo = extraInfo;
        }
    }

    public String getReason() {
        String str;
        synchronized (this) {
            str = this.mReason;
        }
        return str;
    }

    public String getExtraInfo() {
        String str;
        synchronized (this) {
            str = this.mExtraInfo;
        }
        return str;
    }

    public String toString() {
        String stringBuilder;
        synchronized (this) {
            StringBuilder builder = new StringBuilder("[");
            builder.append("type: ");
            builder.append(getTypeName());
            builder.append("[");
            builder.append(getSubtypeName());
            builder.append("], state: ");
            builder.append(this.mState);
            builder.append("/");
            builder.append(this.mDetailedState);
            builder.append(", reason: ");
            builder.append(this.mReason == null ? "(unspecified)" : this.mReason);
            builder.append(", extra: ");
            builder.append(this.mExtraInfo == null ? "(none)" : this.mExtraInfo);
            builder.append(", failover: ");
            builder.append(this.mIsFailover);
            builder.append(", available: ");
            builder.append(this.mIsAvailable);
            builder.append(", roaming: ");
            builder.append(this.mIsRoaming);
            builder.append("]");
            stringBuilder = builder.toString();
        }
        return stringBuilder;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        synchronized (this) {
            dest.writeInt(this.mNetworkType);
            dest.writeInt(this.mSubtype);
            dest.writeString(this.mTypeName);
            dest.writeString(this.mSubtypeName);
            dest.writeString(this.mState.name());
            dest.writeString(this.mDetailedState.name());
            dest.writeInt(this.mIsFailover);
            dest.writeInt(this.mIsAvailable);
            dest.writeInt(this.mIsRoaming);
            dest.writeString(this.mReason);
            dest.writeString(this.mExtraInfo);
        }
    }
}
