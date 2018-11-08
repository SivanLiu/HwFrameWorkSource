package com.android.ims;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.telecom.Log;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

public class ImsConferenceState implements Parcelable {
    public static final Creator<ImsConferenceState> CREATOR = new Creator<ImsConferenceState>() {
        public ImsConferenceState createFromParcel(Parcel in) {
            return new ImsConferenceState(in);
        }

        public ImsConferenceState[] newArray(int size) {
            return new ImsConferenceState[size];
        }
    };
    public static final String DISPLAY_TEXT = "display-text";
    public static final String ENDPOINT = "endpoint";
    public static final String SIP_STATUS_CODE = "sipstatuscode";
    public static final String STATUS = "status";
    public static final String STATUS_ALERTING = "alerting";
    public static final String STATUS_CONNECTED = "connected";
    public static final String STATUS_CONNECT_FAIL = "connect-fail";
    public static final String STATUS_DIALING_IN = "dialing-in";
    public static final String STATUS_DIALING_OUT = "dialing-out";
    public static final String STATUS_DISCONNECTED = "disconnected";
    public static final String STATUS_DISCONNECTING = "disconnecting";
    public static final String STATUS_MUTED_VIA_FOCUS = "muted-via-focus";
    public static final String STATUS_ON_HOLD = "on-hold";
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_SEND_ONLY = "sendonly";
    public static final String STATUS_SEND_RECV = "sendrecv";
    public static final String USER = "user";
    public HashMap<String, Bundle> mParticipants = new HashMap();

    public ImsConferenceState(Parcel in) {
        readFromParcel(in);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.mParticipants.size());
        if (this.mParticipants.size() > 0) {
            Set<Entry<String, Bundle>> entries = this.mParticipants.entrySet();
            if (entries != null) {
                for (Entry<String, Bundle> entry : entries) {
                    out.writeString((String) entry.getKey());
                    out.writeParcelable((Parcelable) entry.getValue(), 0);
                }
            }
        }
    }

    private void readFromParcel(Parcel in) {
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            this.mParticipants.put(in.readString(), (Bundle) in.readParcelable(null));
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static int getConnectionStateForStatus(String status) {
        if (status.equals(STATUS_PENDING)) {
            return 0;
        }
        if (status.equals(STATUS_DIALING_IN)) {
            return 2;
        }
        if (status.equals(STATUS_ALERTING) || status.equals(STATUS_DIALING_OUT)) {
            return 3;
        }
        if (status.equals(STATUS_ON_HOLD) || status.equals(STATUS_SEND_ONLY)) {
            return 5;
        }
        if (status.equals(STATUS_CONNECTED) || status.equals(STATUS_MUTED_VIA_FOCUS) || status.equals(STATUS_DISCONNECTING) || status.equals(STATUS_SEND_RECV) || !status.equals(STATUS_DISCONNECTED)) {
            return 4;
        }
        return 6;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(ImsConferenceState.class.getSimpleName());
        sb.append(" ");
        if (this.mParticipants.size() > 0) {
            Set<Entry<String, Bundle>> entries = this.mParticipants.entrySet();
            if (entries != null) {
                sb.append("<");
                for (Entry<String, Bundle> entry : entries) {
                    sb.append((String) entry.getKey());
                    sb.append(": ");
                    Bundle participantData = (Bundle) entry.getValue();
                    for (String key : participantData.keySet()) {
                        sb.append(key);
                        sb.append("=");
                        if (ENDPOINT.equals(key) || "user".equals(key)) {
                            sb.append(Log.pii(participantData.get(key)));
                        } else {
                            sb.append(participantData.get(key));
                        }
                        sb.append(", ");
                    }
                }
                sb.append(">");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
