package com.huawei.android.pushselfshow;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import com.huawei.android.pushagent.PushReceiver.KEY_TYPE;
import com.huawei.android.pushagent.a.a.a.d;
import com.huawei.android.pushagent.a.a.c;

public class NotificationReceiver extends BroadcastReceiver {
    private void a(Context context, Intent intent) {
        if (context != null && intent != null) {
            Object -l_3_R = intent.getStringExtra(KEY_TYPE.PKGNAME);
            c.b("PushSelfShowLog", "ACTION_CLEAR_GROUP_NUM, pkg " + -l_3_R);
            Object -l_4_R = intent.getStringExtra("auth");
            if (TextUtils.isEmpty(-l_3_R) || TextUtils.isEmpty(-l_4_R)) {
                c.d("PushSelfShowLog", "pkgName is null");
                return;
            }
            Object -l_5_R = d.b(context, -l_4_R);
            if (TextUtils.isEmpty(-l_5_R)) {
                c.b("PushSelfShowLog", "pkg is empty");
            } else if (-l_3_R.equals(-l_5_R)) {
                com.huawei.android.pushselfshow.d.d.b(-l_3_R);
            } else {
                c.b("PushSelfShowLog", "verify failed!");
            }
        }
    }

    public void onReceive(Context context, Intent intent) {
        if (context != null && intent != null) {
            try {
                c.a(context);
                if ("com.huawei.intent.action.CLEAR_GROUP_NUM".equals(intent.getAction())) {
                    a(context, intent);
                }
            } catch (Object -l_3_R) {
                c.d("PushSelfShowLog", -l_3_R.toString());
            }
        }
    }
}
