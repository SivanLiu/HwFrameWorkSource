package com.android.internal.telephony.fullnetwork;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;

public class HwFullNetworkDefaultStateHisi1_0 extends HwFullNetworkDefaultStateHisiBase {
    private static final String LOG_TAG = "HwFullNetworkDefaultStateHisi1_0";

    public HwFullNetworkDefaultStateHisi1_0(Context c, CommandsInterface[] ci, Handler h) {
        super(c, ci, h);
        logd("HwFullNetworkDefaultStateHisi1_0 constructor");
    }

    protected void onRadioUnavailable(Integer index) {
        super.onRadioUnavailable(index);
        this.mChipHisi.mOldMainSwitchTypes[index.intValue()] = -1;
        boolean[] zArr = this.mChipHisi.mRadioOn;
        int length = zArr.length;
        int i = 0;
        while (i < length) {
            if (zArr[i]) {
                i++;
            } else {
                return;
            }
        }
        if (true) {
            this.mChipHisi.setCommrilRestartRild(true);
        }
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
