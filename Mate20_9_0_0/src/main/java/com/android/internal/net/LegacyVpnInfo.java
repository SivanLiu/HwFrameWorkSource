package com.android.internal.net;

import android.app.PendingIntent;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;

public class LegacyVpnInfo implements Parcelable {
    public static final Creator<LegacyVpnInfo> CREATOR = new Creator<LegacyVpnInfo>() {
        public LegacyVpnInfo createFromParcel(Parcel in) {
            LegacyVpnInfo info = new LegacyVpnInfo();
            info.key = in.readString();
            info.state = in.readInt();
            info.intent = (PendingIntent) in.readParcelable(null);
            return info;
        }

        public LegacyVpnInfo[] newArray(int size) {
            return new LegacyVpnInfo[size];
        }
    };
    public static final int STATE_CONNECTED = 3;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_FAILED = 5;
    public static final int STATE_INITIALIZING = 1;
    public static final int STATE_TIMEOUT = 4;
    private static final String TAG = "LegacyVpnInfo";
    public PendingIntent intent;
    public String key;
    public int state = -1;

    /* renamed from: com.android.internal.net.LegacyVpnInfo$2 */
    static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$android$net$NetworkInfo$DetailedState = new int[DetailedState.values().length];

        static {
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[DetailedState.CONNECTING.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[DetailedState.CONNECTED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[DetailedState.DISCONNECTED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[DetailedState.FAILED.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.key);
        out.writeInt(this.state);
        out.writeParcelable(this.intent, flags);
    }

    public static int stateFromNetworkInfo(NetworkInfo info) {
        switch (AnonymousClass2.$SwitchMap$android$net$NetworkInfo$DetailedState[info.getDetailedState().ordinal()]) {
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 0;
            case 4:
                return 5;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unhandled state ");
                stringBuilder.append(info.getDetailedState());
                stringBuilder.append(" ; treating as disconnected");
                Log.w(str, stringBuilder.toString());
                return 0;
        }
    }
}
