package com.huawei.android.pushagent.model.a;

import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.model.channel.a;

public class c implements a {
    private static String TAG = "PushLog3413";

    public c(Context context) {
    }

    public void onReceive(Context context, Intent intent) {
        com.huawei.android.pushagent.utils.f.c.er(TAG, "enter ChannelStatusReceiver:onReceive");
        if ("com.huawei.android.push.intent.GET_PUSH_STATE".equals(intent.getAction())) {
            boolean mk = a.ns().mk();
            String stringExtra = intent.getStringExtra("pkg_name");
            com.huawei.android.pushagent.utils.f.c.ep(TAG, "packageName: " + stringExtra + " get push status, current push state is:" + mk);
            ws(context, mk, stringExtra);
        }
    }

    private static void ws(Context context, boolean z, String str) {
        Intent intent = new Intent();
        com.huawei.android.pushagent.utils.f.c.er(TAG, "sendStateBroadcast the current push state is: " + z);
        intent.setAction("com.huawei.intent.action.PUSH_STATE").putExtra("push_state", z).setFlags(32).setPackage(str);
        context.sendBroadcast(intent);
    }
}
