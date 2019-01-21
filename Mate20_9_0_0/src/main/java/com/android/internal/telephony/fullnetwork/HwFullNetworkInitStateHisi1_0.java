package com.android.internal.telephony.fullnetwork;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;

public class HwFullNetworkInitStateHisi1_0 extends HwFullNetworkInitStateHisiBase {
    private static final String LOG_TAG = "InitStateHisi1_0";

    public HwFullNetworkInitStateHisi1_0(Context c, CommandsInterface[] ci, Handler h) {
        super(c, ci, h);
        logd("HwFullNetworkInitStateHisi1_0 constructor");
    }

    public void handleMessage(Message msg) {
        if (msg == null) {
            loge("msg is null, return!");
            return;
        }
        Integer index = mChipCommon.getCiIndex(msg);
        if (index.intValue() < 0 || index.intValue() >= this.mCis.length) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid index : ");
            stringBuilder.append(index);
            stringBuilder.append(" received with event ");
            stringBuilder.append(msg.what);
            loge(stringBuilder.toString());
            return;
        }
        int i = msg.what;
        if (i == 1001) {
            onIccStatusChanged(index);
        } else if (i != HwFullNetworkConstants.EVENT_QUERY_CARD_TYPE_DONE) {
            super.handleMessage(msg);
        } else {
            AsyncResult ar = null;
            if (msg.obj != null && (msg.obj instanceof AsyncResult)) {
                ar = msg.obj;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Received EVENT_QUERY_CARD_TYPE_DONE on index ");
            stringBuilder2.append(index);
            logd(stringBuilder2.toString());
            if (ar == null || ar.exception != null) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Received EVENT_QUERY_CARD_TYPE_DONE got exception, ar  = ");
                stringBuilder2.append(ar);
                logd(stringBuilder2.toString());
            } else {
                onQueryCardTypeDone(ar, index);
            }
        }
    }

    public void onIccStatusChanged(Integer index) {
        super.onIccStatusChanged(index);
    }

    public synchronized void onQueryCardTypeDone(AsyncResult ar, Integer index) {
        super.onQueryCardTypeDone(ar, index);
    }

    protected void logd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    protected void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }
}
