package android.telecom;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import com.android.internal.telecom.IVideoProvider;
import com.android.internal.telecom.IVideoProvider.Stub;
import java.util.ArrayList;
import java.util.List;

public final class ParcelableConference implements Parcelable {
    public static final Creator<ParcelableConference> CREATOR = new Creator<ParcelableConference>() {
        public ParcelableConference createFromParcel(Parcel source) {
            Parcel parcel = source;
            ClassLoader classLoader = ParcelableConference.class.getClassLoader();
            PhoneAccountHandle phoneAccount = (PhoneAccountHandle) parcel.readParcelable(classLoader);
            int state = source.readInt();
            int capabilities = source.readInt();
            List<String> connectionIds = new ArrayList(2);
            parcel.readList(connectionIds, classLoader);
            long connectTimeMillis = source.readLong();
            IVideoProvider videoCallProvider = Stub.asInterface(source.readStrongBinder());
            int videoState = source.readInt();
            StatusHints statusHints = (StatusHints) parcel.readParcelable(classLoader);
            Bundle extras = parcel.readBundle(classLoader);
            return new ParcelableConference(phoneAccount, state, capabilities, source.readInt(), connectionIds, videoCallProvider, videoState, connectTimeMillis, source.readLong(), statusHints, extras);
        }

        public ParcelableConference[] newArray(int size) {
            return new ParcelableConference[size];
        }
    };
    private long mConnectElapsedTimeMillis = 0;
    private long mConnectTimeMillis = 0;
    private int mConnectionCapabilities;
    private List<String> mConnectionIds;
    private int mConnectionProperties;
    private Bundle mExtras;
    private PhoneAccountHandle mPhoneAccount;
    private int mState;
    private StatusHints mStatusHints;
    private final IVideoProvider mVideoProvider;
    private final int mVideoState;

    public ParcelableConference(PhoneAccountHandle phoneAccount, int state, int connectionCapabilities, int connectionProperties, List<String> connectionIds, IVideoProvider videoProvider, int videoState, long connectTimeMillis, long connectElapsedTimeMillis, StatusHints statusHints, Bundle extras) {
        this.mPhoneAccount = phoneAccount;
        this.mState = state;
        this.mConnectionCapabilities = connectionCapabilities;
        this.mConnectionProperties = connectionProperties;
        this.mConnectionIds = connectionIds;
        this.mVideoProvider = videoProvider;
        this.mVideoState = videoState;
        this.mConnectTimeMillis = connectTimeMillis;
        this.mStatusHints = statusHints;
        this.mExtras = extras;
        this.mConnectElapsedTimeMillis = connectElapsedTimeMillis;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("account: ");
        stringBuffer.append(this.mPhoneAccount);
        stringBuffer.append(", state: ");
        stringBuffer.append(Connection.stateToString(this.mState));
        stringBuffer.append(", capabilities: ");
        stringBuffer.append(Connection.capabilitiesToString(this.mConnectionCapabilities));
        stringBuffer.append(", properties: ");
        stringBuffer.append(Connection.propertiesToString(this.mConnectionProperties));
        stringBuffer.append(", connectTime: ");
        stringBuffer.append(this.mConnectTimeMillis);
        stringBuffer.append(", children: ");
        stringBuffer.append(this.mConnectionIds);
        stringBuffer.append(", VideoState: ");
        stringBuffer.append(this.mVideoState);
        stringBuffer.append(", VideoProvider: ");
        stringBuffer.append(this.mVideoProvider);
        return stringBuffer.toString();
    }

    public PhoneAccountHandle getPhoneAccount() {
        return this.mPhoneAccount;
    }

    public int getState() {
        return this.mState;
    }

    public int getConnectionCapabilities() {
        return this.mConnectionCapabilities;
    }

    public int getConnectionProperties() {
        return this.mConnectionProperties;
    }

    public List<String> getConnectionIds() {
        return this.mConnectionIds;
    }

    public long getConnectTimeMillis() {
        return this.mConnectTimeMillis;
    }

    public long getConnectElapsedTimeMillis() {
        return this.mConnectElapsedTimeMillis;
    }

    public IVideoProvider getVideoProvider() {
        return this.mVideoProvider;
    }

    public int getVideoState() {
        return this.mVideoState;
    }

    public StatusHints getStatusHints() {
        return this.mStatusHints;
    }

    public Bundle getExtras() {
        return this.mExtras;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel destination, int flags) {
        destination.writeParcelable(this.mPhoneAccount, 0);
        destination.writeInt(this.mState);
        destination.writeInt(this.mConnectionCapabilities);
        destination.writeList(this.mConnectionIds);
        destination.writeLong(this.mConnectTimeMillis);
        destination.writeStrongBinder(this.mVideoProvider != null ? this.mVideoProvider.asBinder() : null);
        destination.writeInt(this.mVideoState);
        destination.writeParcelable(this.mStatusHints, 0);
        destination.writeBundle(this.mExtras);
        destination.writeInt(this.mConnectionProperties);
        destination.writeLong(this.mConnectElapsedTimeMillis);
    }
}
