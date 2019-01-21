package android.telecom;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.telecom.Connection.RttTextStream;

public final class ConnectionRequest implements Parcelable {
    public static final Creator<ConnectionRequest> CREATOR = new Creator<ConnectionRequest>() {
        public ConnectionRequest createFromParcel(Parcel source) {
            return new ConnectionRequest(source, null);
        }

        public ConnectionRequest[] newArray(int size) {
            return new ConnectionRequest[size];
        }
    };
    private final PhoneAccountHandle mAccountHandle;
    private final Uri mAddress;
    private final Bundle mExtras;
    private final ParcelFileDescriptor mRttPipeFromInCall;
    private final ParcelFileDescriptor mRttPipeToInCall;
    private RttTextStream mRttTextStream;
    private final boolean mShouldShowIncomingCallUi;
    private final String mTelecomCallId;
    private final int mVideoState;

    public static final class Builder {
        private PhoneAccountHandle mAccountHandle;
        private Uri mAddress;
        private Bundle mExtras;
        private ParcelFileDescriptor mRttPipeFromInCall;
        private ParcelFileDescriptor mRttPipeToInCall;
        private boolean mShouldShowIncomingCallUi = false;
        private String mTelecomCallId;
        private int mVideoState = 0;

        public Builder setAccountHandle(PhoneAccountHandle accountHandle) {
            this.mAccountHandle = accountHandle;
            return this;
        }

        public Builder setAddress(Uri address) {
            this.mAddress = address;
            return this;
        }

        public Builder setExtras(Bundle extras) {
            this.mExtras = extras;
            return this;
        }

        public Builder setVideoState(int videoState) {
            this.mVideoState = videoState;
            return this;
        }

        public Builder setTelecomCallId(String telecomCallId) {
            this.mTelecomCallId = telecomCallId;
            return this;
        }

        public Builder setShouldShowIncomingCallUi(boolean shouldShowIncomingCallUi) {
            this.mShouldShowIncomingCallUi = shouldShowIncomingCallUi;
            return this;
        }

        public Builder setRttPipeFromInCall(ParcelFileDescriptor rttPipeFromInCall) {
            this.mRttPipeFromInCall = rttPipeFromInCall;
            return this;
        }

        public Builder setRttPipeToInCall(ParcelFileDescriptor rttPipeToInCall) {
            this.mRttPipeToInCall = rttPipeToInCall;
            return this;
        }

        public ConnectionRequest build() {
            return new ConnectionRequest(this.mAccountHandle, this.mAddress, this.mExtras, this.mVideoState, this.mTelecomCallId, this.mShouldShowIncomingCallUi, this.mRttPipeFromInCall, this.mRttPipeToInCall, null);
        }
    }

    /* synthetic */ ConnectionRequest(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    /* synthetic */ ConnectionRequest(PhoneAccountHandle x0, Uri x1, Bundle x2, int x3, String x4, boolean x5, ParcelFileDescriptor x6, ParcelFileDescriptor x7, AnonymousClass1 x8) {
        this(x0, x1, x2, x3, x4, x5, x6, x7);
    }

    public ConnectionRequest(PhoneAccountHandle accountHandle, Uri handle, Bundle extras) {
        this(accountHandle, handle, extras, 0, null, false, null, null);
    }

    public ConnectionRequest(PhoneAccountHandle accountHandle, Uri handle, Bundle extras, int videoState) {
        this(accountHandle, handle, extras, videoState, null, false, null, null);
    }

    public ConnectionRequest(PhoneAccountHandle accountHandle, Uri handle, Bundle extras, int videoState, String telecomCallId, boolean shouldShowIncomingCallUi) {
        this(accountHandle, handle, extras, videoState, telecomCallId, shouldShowIncomingCallUi, null, null);
    }

    private ConnectionRequest(PhoneAccountHandle accountHandle, Uri handle, Bundle extras, int videoState, String telecomCallId, boolean shouldShowIncomingCallUi, ParcelFileDescriptor rttPipeFromInCall, ParcelFileDescriptor rttPipeToInCall) {
        this.mAccountHandle = accountHandle;
        this.mAddress = handle;
        this.mExtras = extras;
        this.mVideoState = videoState;
        this.mTelecomCallId = telecomCallId;
        this.mShouldShowIncomingCallUi = shouldShowIncomingCallUi;
        this.mRttPipeFromInCall = rttPipeFromInCall;
        this.mRttPipeToInCall = rttPipeToInCall;
    }

    private ConnectionRequest(Parcel in) {
        this.mAccountHandle = (PhoneAccountHandle) in.readParcelable(getClass().getClassLoader());
        this.mAddress = (Uri) in.readParcelable(getClass().getClassLoader());
        this.mExtras = (Bundle) in.readParcelable(getClass().getClassLoader());
        this.mVideoState = in.readInt();
        this.mTelecomCallId = in.readString();
        boolean z = true;
        if (in.readInt() != 1) {
            z = false;
        }
        this.mShouldShowIncomingCallUi = z;
        this.mRttPipeFromInCall = (ParcelFileDescriptor) in.readParcelable(getClass().getClassLoader());
        this.mRttPipeToInCall = (ParcelFileDescriptor) in.readParcelable(getClass().getClassLoader());
    }

    public PhoneAccountHandle getAccountHandle() {
        return this.mAccountHandle;
    }

    public Uri getAddress() {
        return this.mAddress;
    }

    public Bundle getExtras() {
        return this.mExtras;
    }

    public int getVideoState() {
        return this.mVideoState;
    }

    public String getTelecomCallId() {
        return this.mTelecomCallId;
    }

    public boolean shouldShowIncomingCallUi() {
        return this.mShouldShowIncomingCallUi;
    }

    public ParcelFileDescriptor getRttPipeToInCall() {
        return this.mRttPipeToInCall;
    }

    public ParcelFileDescriptor getRttPipeFromInCall() {
        return this.mRttPipeFromInCall;
    }

    public RttTextStream getRttTextStream() {
        if (!isRequestingRtt()) {
            return null;
        }
        if (this.mRttTextStream == null) {
            this.mRttTextStream = new RttTextStream(this.mRttPipeToInCall, this.mRttPipeFromInCall);
        }
        return this.mRttTextStream;
    }

    public boolean isRequestingRtt() {
        return (this.mRttPipeFromInCall == null || this.mRttPipeToInCall == null) ? false : true;
    }

    public String toString() {
        Uri uri;
        String str = "ConnectionRequest %s %s";
        Object[] objArr = new Object[2];
        if (this.mAddress == null) {
            uri = Uri.EMPTY;
        } else {
            uri = "xxxxxx";
        }
        objArr[0] = uri;
        objArr[1] = this.mExtras == null ? "" : this.mExtras;
        return String.format(str, objArr);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel destination, int flags) {
        destination.writeParcelable(this.mAccountHandle, 0);
        destination.writeParcelable(this.mAddress, 0);
        destination.writeParcelable(this.mExtras, 0);
        destination.writeInt(this.mVideoState);
        destination.writeString(this.mTelecomCallId);
        destination.writeInt(this.mShouldShowIncomingCallUi);
        destination.writeParcelable(this.mRttPipeFromInCall, 0);
        destination.writeParcelable(this.mRttPipeToInCall, 0);
    }
}
