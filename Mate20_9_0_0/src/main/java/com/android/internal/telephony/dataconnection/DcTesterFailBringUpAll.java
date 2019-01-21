package com.android.internal.telephony.dataconnection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.telephony.Rlog;
import com.android.internal.telephony.Phone;

public class DcTesterFailBringUpAll {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "DcTesterFailBrinupAll";
    private String mActionFailBringUp;
    private DcFailBringUp mFailBringUp = new DcFailBringUp();
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            DcTesterFailBringUpAll dcTesterFailBringUpAll = DcTesterFailBringUpAll.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sIntentReceiver.onReceive: action=");
            stringBuilder.append(action);
            dcTesterFailBringUpAll.log(stringBuilder.toString());
            if (action.equals(DcTesterFailBringUpAll.this.mActionFailBringUp)) {
                DcTesterFailBringUpAll.this.mFailBringUp.saveParameters(intent, "sFailBringUp");
            } else if (action.equals(DcTesterFailBringUpAll.this.mPhone.getActionDetached())) {
                DcTesterFailBringUpAll.this.log("simulate detaching");
                DcTesterFailBringUpAll.this.mFailBringUp.saveParameters(KeepaliveStatus.INVALID_HANDLE, DcFailCause.LOST_CONNECTION.getErrorCode(), -1);
            } else if (action.equals(DcTesterFailBringUpAll.this.mPhone.getActionAttached())) {
                DcTesterFailBringUpAll.this.log("simulate attaching");
                DcTesterFailBringUpAll.this.mFailBringUp.saveParameters(0, DcFailCause.NONE.getErrorCode(), -1);
            } else {
                dcTesterFailBringUpAll = DcTesterFailBringUpAll.this;
                stringBuilder = new StringBuilder();
                stringBuilder.append("onReceive: unknown action=");
                stringBuilder.append(action);
                dcTesterFailBringUpAll.log(stringBuilder.toString());
            }
        }
    };
    private Phone mPhone;

    DcTesterFailBringUpAll(Phone phone, Handler handler) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(DcFailBringUp.INTENT_BASE);
        stringBuilder.append(".");
        stringBuilder.append("action_fail_bringup");
        this.mActionFailBringUp = stringBuilder.toString();
        this.mPhone = phone;
        if (Build.IS_DEBUGGABLE) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(this.mActionFailBringUp);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("register for intent action=");
            stringBuilder2.append(this.mActionFailBringUp);
            log(stringBuilder2.toString());
            filter.addAction(this.mPhone.getActionDetached());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("register for intent action=");
            stringBuilder2.append(this.mPhone.getActionDetached());
            log(stringBuilder2.toString());
            filter.addAction(this.mPhone.getActionAttached());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("register for intent action=");
            stringBuilder2.append(this.mPhone.getActionAttached());
            log(stringBuilder2.toString());
            phone.getContext().registerReceiver(this.mIntentReceiver, filter, null, handler);
        }
    }

    void dispose() {
        if (Build.IS_DEBUGGABLE) {
            this.mPhone.getContext().unregisterReceiver(this.mIntentReceiver);
        }
    }

    public DcFailBringUp getDcFailBringUp() {
        return this.mFailBringUp;
    }

    private void log(String s) {
        Rlog.d(LOG_TAG, s);
    }
}
