package defpackage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.huawei.android.pushagent.PushService;

/* renamed from: ac  reason: default package */
public final class ac extends BroadcastReceiver {
    final /* synthetic */ PushService R;

    private ac(PushService pushService) {
        this.R = pushService;
    }

    public /* synthetic */ ac(PushService pushService, byte b) {
        this(pushService);
    }

    public final void onReceive(Context context, Intent intent) {
        long versionCode;
        byte c;
        if (PushService.a(context, intent)) {
            String action = intent.getAction();
            Log.i("PushLogSys", "sys push inner receiver get action is " + action);
            if ("com.huawei.android.push.intent.CHECK_HWPUSH_VERSION".equals(action) && context.getPackageName().equals(intent.getStringExtra("Remote_Package_Name"))) {
                long j = -1L;
                Object obj = ((al) new an(context)).ac.get("latestVersion");
                if (obj != null) {
                    j = obj;
                }
                long intValue = j instanceof Integer ? (long) ((Integer) j).intValue() : j instanceof Long ? j.longValue() : -1;
                synchronized (PushService.G) {
                    versionCode = ai.b(context, this.R.L).getVersionCode();
                }
                Log.i("PushLogSys", "check pushcore version trs version is " + intValue + ". localVersion is " + versionCode);
                if (versionCode <= intValue && 2 != (c = this.R.c())) {
                    Log.i("PushLogSys", "TRS update pushcore version , need install push again");
                    PushService.a(this.R, c);
                }
            }
        }
    }
}
