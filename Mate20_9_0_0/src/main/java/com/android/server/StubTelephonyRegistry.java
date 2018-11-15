package com.android.server;

import android.content.Context;
import android.os.Binder;
import android.util.Log;
import com.android.internal.telephony.IPhoneStateListener;
import com.huawei.hsm.permission.LocationPermission;

class StubTelephonyRegistry extends TelephonyRegistry {
    private static final String TAG = "StubTelephonyRegistry";
    private Context mContext;
    private LocationPermission mLocationPermission = new LocationPermission(this.mContext);

    StubTelephonyRegistry(Context context) {
        super(context);
        this.mContext = context;
    }

    public void listenForSubscriber(int subId, String pkgForDebug, IPhoneStateListener callback, int events, boolean notifyNow) {
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("uid:");
        stringBuilder.append(uid);
        stringBuilder.append(" pid:");
        stringBuilder.append(pid);
        stringBuilder.append(" PhoneStateListener.LISTEN_CELL_LOCATION:");
        stringBuilder.append(16);
        stringBuilder.append(" events:");
        stringBuilder.append(events);
        Log.d(str, stringBuilder.toString());
        if ((events & 16) != 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("LISTEN_CELL_LOCATION uid:");
            stringBuilder.append(uid);
            Log.d(str, stringBuilder.toString());
            if (this.mLocationPermission.isLocationBlocked()) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("LISTEN_CELL_LOCATION is blocked by huawei's permission manager uid:");
                stringBuilder.append(uid);
                Log.d(str, stringBuilder.toString());
                events &= -17;
            }
        }
        super.listenForSubscriber(subId, pkgForDebug, callback, events, notifyNow);
    }
}
