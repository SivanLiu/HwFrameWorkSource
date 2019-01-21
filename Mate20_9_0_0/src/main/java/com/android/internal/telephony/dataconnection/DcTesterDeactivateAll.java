package com.android.internal.telephony.dataconnection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.telephony.Rlog;
import com.android.internal.telephony.Phone;
import java.util.Iterator;

public class DcTesterDeactivateAll {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "DcTesterDeacativateAll";
    public static String sActionDcTesterDeactivateAll = "com.android.internal.telephony.dataconnection.action_deactivate_all";
    private DcController mDcc;
    private Phone mPhone;
    protected BroadcastReceiver sIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sIntentReceiver.onReceive: action=");
            stringBuilder.append(action);
            DcTesterDeactivateAll.log(stringBuilder.toString());
            if (action.equals(DcTesterDeactivateAll.sActionDcTesterDeactivateAll) || action.equals(DcTesterDeactivateAll.this.mPhone.getActionDetached())) {
                DcTesterDeactivateAll.log("Send DEACTIVATE to all Dcc's");
                if (DcTesterDeactivateAll.this.mDcc != null) {
                    Iterator it = DcTesterDeactivateAll.this.mDcc.mDcListAll.iterator();
                    while (it.hasNext()) {
                        ((DataConnection) it.next()).tearDownNow();
                    }
                    return;
                }
                DcTesterDeactivateAll.log("onReceive: mDcc is null, ignoring");
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("onReceive: unknown action=");
            stringBuilder.append(action);
            DcTesterDeactivateAll.log(stringBuilder.toString());
        }
    };

    DcTesterDeactivateAll(Phone phone, DcController dcc, Handler handler) {
        this.mPhone = phone;
        this.mDcc = dcc;
        if (Build.IS_DEBUGGABLE) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(sActionDcTesterDeactivateAll);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("register for intent action=");
            stringBuilder.append(sActionDcTesterDeactivateAll);
            log(stringBuilder.toString());
            filter.addAction(this.mPhone.getActionDetached());
            stringBuilder = new StringBuilder();
            stringBuilder.append("register for intent action=");
            stringBuilder.append(this.mPhone.getActionDetached());
            log(stringBuilder.toString());
            phone.getContext().registerReceiver(this.sIntentReceiver, filter, null, handler);
        }
    }

    void dispose() {
        if (Build.IS_DEBUGGABLE) {
            this.mPhone.getContext().unregisterReceiver(this.sIntentReceiver);
        }
    }

    private static void log(String s) {
        Rlog.d(LOG_TAG, s);
    }
}
