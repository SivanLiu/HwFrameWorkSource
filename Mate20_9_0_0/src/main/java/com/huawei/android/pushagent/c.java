package com.huawei.android.pushagent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.utils.b.a;

class c extends BroadcastReceiver {
    final /* synthetic */ PushService js;

    /* synthetic */ c(PushService pushService, c cVar) {
        this(pushService);
    }

    private c(PushService pushService) {
        this.js = pushService;
    }

    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            a.su(PushService.TAG, "context== null or intent == null");
            return;
        }
        try {
            a.sv(PushService.TAG, "action is " + intent.getAction());
            PushService.abv(intent);
        } catch (Exception e) {
            a.sw(PushService.TAG, "call PushInnerReceiver:onReceive cause " + e.toString(), e);
        }
    }
}
