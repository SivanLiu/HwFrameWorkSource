package com.huawei.android.pushselfshow;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import com.huawei.android.pushagent.PushReceiver.ACTION;
import com.huawei.android.pushagent.PushReceiver.KEY_TYPE;
import com.huawei.android.pushagent.a.a.a.d;
import com.huawei.android.pushagent.a.a.c;
import com.huawei.android.pushagent.a.a.e;
import com.huawei.android.pushselfshow.d.g;
import com.huawei.android.pushselfshow.permission.RequestPermissionsActivity;
import com.huawei.android.pushselfshow.utils.b.b;
import java.io.File;
import org.json.JSONArray;

public class SelfShowReceiver {

    static class a extends Thread {
        Context a;
        String b;

        public a(Context context, String str) {
            this.a = context;
            this.b = str;
        }

        public void run() {
            Object -l_1_R = com.huawei.android.pushselfshow.utils.a.a.a(this.a, this.b);
            int -l_2_I = -l_1_R.size();
            c.e("PushSelfShowLog", "receive package add ,arrSize " + -l_2_I);
            for (int -l_3_I = 0; -l_3_I < -l_2_I; -l_3_I++) {
                Object -l_4_R = new com.huawei.android.pushselfshow.c.a();
                -l_4_R.g((String) -l_1_R.get(-l_3_I));
                -l_4_R.b("app");
                -l_4_R.a(this.b);
                com.huawei.android.pushselfshow.utils.a.a(this.a, "16", -l_4_R, -1);
            }
            if (-l_2_I > 0) {
                com.huawei.android.pushselfshow.utils.a.a.b(this.a, this.b);
            }
            com.huawei.android.pushselfshow.utils.a.b(new File(b.a(this.a)));
        }
    }

    private boolean a(Context context, Intent intent) {
        if (com.huawei.android.pushagent.a.a.a.a() < 12) {
            return true;
        }
        Object -l_3_R = "";
        if ("com.huawei.android.pushagent".equals(context.getPackageName())) {
            -l_3_R = com.huawei.android.pushselfshow.utils.a.e();
        } else {
            -l_3_R = !"com.huawei.hwid".equals(context.getPackageName()) ? d.b(context, new e(context, "push_client_self_info").b("push_notify_key")) : com.huawei.android.pushselfshow.utils.a.f();
        }
        Object -l_4_R = intent.getStringExtra("extra_encrypt_data");
        if (!TextUtils.isEmpty(-l_4_R)) {
            try {
                if (context.getPackageName().equals(d.b(context, -l_4_R, -l_3_R.getBytes("UTF-8")))) {
                    c.a("PushSelfShowLog", "parse msg success!");
                    return true;
                }
            } catch (Object -l_5_R) {
                c.d("PushSelfShowLog", -l_5_R.toString());
            }
        }
        return false;
    }

    public void a(Context context, Intent intent, com.huawei.android.pushselfshow.c.a aVar) {
        c.a("PushSelfShowLog", "receive a selfshow message ,the type is" + aVar.m());
        if (com.huawei.android.pushselfshow.b.a.a(aVar.m())) {
            long -l_4_J = com.huawei.android.pushselfshow.utils.a.b(aVar.j());
            if (-l_4_J == 0) {
                new g(context, aVar).start();
            } else {
                c.a("PushSelfShowLog", "waiting ……");
                intent.setPackage(context.getPackageName());
                com.huawei.android.pushselfshow.utils.a.a(context, intent, -l_4_J);
            }
            return;
        }
        com.huawei.android.pushselfshow.utils.a.a(context, "3", aVar, -1);
    }

    public void a(Context context, Intent intent, String str, com.huawei.android.pushselfshow.c.a aVar, int i) {
        Object -l_6_R;
        c.a("PushSelfShowLog", "receive a selfshow userhandle message");
        if ("-1".equals(str)) {
            com.huawei.android.pushselfshow.utils.a.a(context, i);
        } else {
            com.huawei.android.pushselfshow.utils.a.b(context, intent);
        }
        if ("1".equals(str)) {
            new com.huawei.android.pushselfshow.b.a(context, aVar).a();
            if (aVar.l() != null) {
                try {
                    -l_6_R = new JSONArray(aVar.l());
                    Object -l_7_R = new Intent(ACTION.ACTION_NOTIFICATION_MSG_CLICK);
                    -l_7_R.putExtra(KEY_TYPE.PUSH_KEY_CLICK, -l_6_R.toString()).setPackage(aVar.k()).setFlags(32);
                    context.sendBroadcast(-l_7_R);
                } catch (Object -l_6_R2) {
                    c.d("PushSelfShowLog", "message.extras is not a json format,err info " + -l_6_R2.toString());
                }
            }
        }
        if (!TextUtils.isEmpty(aVar.e())) {
            -l_6_R2 = aVar.k() + aVar.e();
            c.a("PushSelfShowLog", "groupMap key is " + -l_6_R2);
            com.huawei.android.pushselfshow.d.d.a(-l_6_R2);
        }
        com.huawei.android.pushselfshow.utils.a.a(context, str, aVar, i);
    }

    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            try {
                c.a("PushSelfShowLog", "enter SelfShowReceiver receiver, context or intent is null");
            } catch (Throwable -l_3_R) {
                c.a("PushSelfShowLog", -l_3_R.toString(), -l_3_R);
            }
        } else {
            c.a(context);
            Object -l_3_R2 = intent.getAction();
            Object -l_4_R;
            Object -l_5_R;
            if ("android.intent.action.PACKAGE_ADDED".equals(-l_3_R2)) {
                -l_4_R = intent.getData();
                if (-l_4_R != null) {
                    -l_5_R = -l_4_R.getSchemeSpecificPart();
                    c.e("PushSelfShowLog", "receive package add ,the pkgName is " + -l_5_R);
                    if (!TextUtils.isEmpty(-l_5_R)) {
                        new a(context, -l_5_R).start();
                    }
                }
            } else if ("com.huawei.intent.action.PUSH".equals(-l_3_R2)) {
                if (!"com.huawei.android.pushagent".equals(context.getPackageName()) && RequestPermissionsActivity.a(context)) {
                    c.b("PushSelfShowLog", "needStartPermissionActivity");
                    RequestPermissionsActivity.a(context, intent);
                    return;
                }
                String -l_6_R = null;
                if (intent.hasExtra("selfshow_info")) {
                    -l_4_R = intent.getByteArrayExtra("selfshow_info");
                    if (intent.hasExtra("selfshow_token")) {
                        -l_5_R = intent.getByteArrayExtra("selfshow_token");
                        if (intent.hasExtra("selfshow_event_id")) {
                            -l_6_R = intent.getStringExtra("selfshow_event_id");
                        }
                        int -l_7_I = 0;
                        if (intent.hasExtra("selfshow_notify_id")) {
                            -l_7_I = intent.getIntExtra("selfshow_notify_id", 0);
                            c.b("PushSelfShowLog", "get notifyId:" + -l_7_I);
                        }
                        if (a(context, intent)) {
                            Object -l_8_R = new com.huawei.android.pushselfshow.c.a(-l_4_R, -l_5_R);
                            if (-l_8_R.b()) {
                                c.a("PushSelfShowLog", " onReceive the msg id = " + -l_8_R.a() + ",and cmd is" + -l_8_R.m() + ",and the eventId is " + -l_6_R);
                                if (-l_6_R != null) {
                                    a(context, intent, -l_6_R, -l_8_R, -l_7_I);
                                } else {
                                    a(context, intent, -l_8_R);
                                }
                                com.huawei.android.pushselfshow.utils.a.b(new File(b.a(context)));
                            } else {
                                c.a("PushSelfShowLog", "parseMessage failed");
                                return;
                            }
                        }
                        c.b("PushSelfShowLog", "msg is invalid!");
                    }
                }
            }
        }
    }
}
