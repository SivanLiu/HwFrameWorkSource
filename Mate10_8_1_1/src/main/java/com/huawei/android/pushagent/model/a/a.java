package com.huawei.android.pushagent.model.a;

import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.utils.a.b;

public class a implements d {
    private static String TAG = "PushLog2976";

    public a(Context context) {
    }

    public void onReceive(Context context, Intent intent) {
        b.x(TAG, "enter ChannelStatusReceiver:onReceive");
        if ("com.huawei.android.push.intent.GET_PUSH_STATE".equals(intent.getAction())) {
            boolean vc = com.huawei.android.pushagent.model.channel.a.wr().vc();
            String stringExtra = intent.getStringExtra("pkg_name");
            b.z(TAG, "packageName: " + stringExtra + " get push status, current push state is:" + vc);
            ix(context, vc, stringExtra);
        }
    }

    private static void ix(Context context, boolean z, String str) {
        Intent intent = new Intent();
        b.x(TAG, "sendStateBroadcast the current push state is: " + z);
        intent.setAction("com.huawei.intent.action.PUSH_STATE").putExtra("push_state", z).setFlags(32).setPackage(str);
        context.sendBroadcast(intent);
    }
}
