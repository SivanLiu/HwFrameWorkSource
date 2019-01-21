package com.android.internal.telephony.fullnetwork;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;

public class HwFullNetworkDefaultStateHisi2_0 extends HwFullNetworkDefaultStateHisiBase {
    private static final String LOG_TAG = "HwFullNetworkDefaultStateHisi2_0";

    public HwFullNetworkDefaultStateHisi2_0(Context c, CommandsInterface[] ci, Handler h) {
        super(c, ci, h);
        logd("HwFullNetworkDefaultStateHisi2_0 constructor");
    }

    protected void onRadioUnavailable(Integer index) {
        super.onRadioUnavailable(index);
    }

    protected void processSubSetUiccResult(Intent intent) {
    }

    protected void logd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    protected void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }
}
