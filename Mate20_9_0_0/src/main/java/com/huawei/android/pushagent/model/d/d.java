package com.huawei.android.pushagent.model.d;

import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.utils.b.a;

public class d implements c {
    private static String TAG = "PushLog3414";

    public d(Context context) {
    }

    public void onReceive(Context context, Intent intent) {
        a.st(TAG, "enter ChannelStatusReceiver:onReceive");
        if ("com.huawei.android.push.intent.GET_PUSH_STATE".equals(intent.getAction())) {
            boolean cp = com.huawei.android.pushagent.model.channel.a.dz().cp();
            String stringExtra = intent.getStringExtra("pkg_name");
            a.sv(TAG, "packageName: " + stringExtra + " get push status, current push state is:" + cp);
            rq(context, cp, stringExtra);
        }
    }

    private static void rq(Context context, boolean z, String str) {
        Intent intent = new Intent();
        a.st(TAG, "sendStateBroadcast the current push state is: " + z);
        intent.setAction("com.huawei.intent.action.PUSH_STATE").putExtra("push_state", z).setFlags(32).setPackage(str);
        context.sendBroadcast(intent);
    }
}
