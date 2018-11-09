package com.huawei.android.pushagent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.utils.a.b;

class a extends BroadcastReceiver {
    final /* synthetic */ PushService jf;

    private a(PushService pushService) {
        this.jf = pushService;
    }

    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            b.y(PushService.TAG, "context== null or intent == null");
            return;
        }
        try {
            b.z(PushService.TAG, "action is " + intent.getAction());
            PushService.aax(intent);
        } catch (Throwable e) {
            b.aa(PushService.TAG, "call PushInnerReceiver:onReceive cause " + e.toString(), e);
        }
    }
}
