package com.huawei.android.pushagent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.utils.f.c;

class a extends BroadcastReceiver {
    final /* synthetic */ PushService jk;

    /* synthetic */ a(PushService pushService, a aVar) {
        this(pushService);
    }

    private a(PushService pushService) {
        this.jk = pushService;
    }

    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            c.eq(PushService.TAG, "context== null or intent == null");
            return;
        }
        try {
            c.ep(PushService.TAG, "action is " + intent.getAction());
            PushService.abr(intent);
        } catch (Throwable e) {
            c.es(PushService.TAG, "call PushInnerReceiver:onReceive cause " + e.toString(), e);
        }
    }
}
