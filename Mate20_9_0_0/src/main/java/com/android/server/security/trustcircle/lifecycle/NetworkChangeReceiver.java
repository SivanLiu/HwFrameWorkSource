package com.android.server.security.trustcircle.lifecycle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import com.android.server.security.trustcircle.utils.LogHelper;
import com.android.server.security.trustcircle.utils.Status.ExceptionStep;
import com.android.server.security.trustcircle.utils.Utils;

public class NetworkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = NetworkChangeReceiver.class.getSimpleName();
    private static volatile long lastNetworkAvailableTime = System.currentTimeMillis();

    public void onReceive(Context context, Intent intent) {
        if (intent == null || context == null) {
            LogHelper.e(TAG, "error: intent or context is null");
            return;
        }
        if ("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())) {
            NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
            String str;
            StringBuilder stringBuilder;
            if (info == null) {
                LogHelper.e(TAG, "error: NetworkInfo is null");
            } else if (State.CONNECTED != info.getState() || !info.isAvailable()) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(getConnectionType(info.getType()));
                stringBuilder.append(" disconnected");
                LogHelper.i(str, stringBuilder.toString());
            } else if (info.getType() == 1 || info.getType() == 0) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(getConnectionType(info.getType()));
                stringBuilder.append(" connected");
                LogHelper.i(str, stringBuilder.toString());
                long currentTime = System.currentTimeMillis();
                if (!(isHwIdAndTaLoginedStateConsistent(context) && ExceptionStep.NO_EXCEPTION.ordinal() == LifeCycleProcessor.getExceptionStepOfCurrentUserId())) {
                    if (LifeCycleProcessor.startTcisService(context) == null) {
                        LogHelper.e(TAG, "error: could not start tcis service");
                    } else {
                        updateTime(currentTime);
                    }
                }
            }
        } else {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("error: receive unexpected intent ");
            stringBuilder2.append(intent.getAction());
            LogHelper.e(str2, stringBuilder2.toString());
        }
    }

    private String getConnectionType(int type) {
        if (type == 0) {
            return "mobile network";
        }
        if (type == 1) {
            return "WIFI network";
        }
        return "unknown network";
    }

    public static void updateTime(long time) {
        synchronized (NetworkChangeReceiver.class) {
            lastNetworkAvailableTime = time;
        }
    }

    public static boolean isHwIdAndTaLoginedStateConsistent(Context context) {
        return Utils.hasLoginAccount(context) == ((LifeCycleProcessor.getLoginedUserID() > -1 ? 1 : (LifeCycleProcessor.getLoginedUserID() == -1 ? 0 : -1)) != 0);
    }
}
