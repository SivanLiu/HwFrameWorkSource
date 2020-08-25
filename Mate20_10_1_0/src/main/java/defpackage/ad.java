package defpackage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import com.huawei.android.feature.BuildConfig;
import com.huawei.android.pushagent.PushService;

/* renamed from: ad  reason: default package */
public final class ad extends BroadcastReceiver {
    final /* synthetic */ PushService R;

    private ad(PushService pushService) {
        this.R = pushService;
    }

    public /* synthetic */ ad(PushService pushService, byte b) {
        this(pushService);
    }

    public final void onReceive(Context context, Intent intent) {
        byte c;
        if (PushService.a(context, intent)) {
            String action = intent.getAction();
            Uri data = intent.getData();
            String str = BuildConfig.FLAVOR;
            if (data != null) {
                str = data.getSchemeSpecificPart();
            }
            Log.i("PushLogSys", "sys push system receiver get action is " + action + ". pkgName is " + str);
            if (!"android.intent.action.PACKAGE_ADDED".equals(action)) {
                return;
            }
            if (("com.huawei.hwid".equals(str) || "com.huawei.android.pushagent".equals(str)) && 2 != (c = this.R.c())) {
                Log.i("PushLogSys", "HMS or NC update pushcore version, need install push again");
                PushService.a(this.R, c);
            }
        }
    }
}
