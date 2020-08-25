package com.huawei.android.pushagent;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import com.huawei.android.content.ContextEx;
import com.huawei.android.feature.compat.InstallCompat;
import com.huawei.android.feature.install.InstallStorageManager;
import com.huawei.android.feature.install.localinstall.FeatureLocalInstallManager;
import com.huawei.android.os.UserHandleEx;
import com.huawei.android.pushagent.dynamicload.IPushManager;
import java.io.File;
import java.util.concurrent.CountDownLatch;

public class PushService extends Service {
    /* access modifiers changed from: private */
    public static final byte[] G = new byte[0];
    private static PushService H;
    ac I;
    ad J;
    public IPushManager K;
    /* access modifiers changed from: private */
    public FeatureLocalInstallManager L;
    /* access modifiers changed from: private */
    public Context M;
    public boolean N = false;
    public CountDownLatch O;
    private HandlerThread P = new HandlerThread("sys push");

    public static /* synthetic */ void a(PushService pushService, byte b) {
        synchronized (G) {
            if (2 == b) {
                ai.b(pushService.M, pushService.L).e();
            } else if (1 == b) {
                ae a = ae.a(pushService.M, pushService.L);
                Log.i("PushLogSys", "begin install HMS push core");
                a.b("package://com.huawei.hwid/feature/pushcore.fpk", "HMS");
            } else {
                aj c = aj.c(pushService.M, pushService.L);
                Log.i("PushLogSys", "begin install NC push core");
                c.b("package://com.huawei.android.pushagent/feature/pushcore.fpk", "NC");
            }
        }
    }

    /* access modifiers changed from: private */
    public static boolean a(Context context, Intent intent) {
        if (context == null || intent == null) {
            Log.e("PushLogSys", "PushInnerReceiver context is null or intent is null");
            return false;
        }
        try {
            intent.getStringExtra("TestIntent");
            return true;
        } catch (Exception e) {
            Log.e("PushLogSys", "intent has some error");
            return false;
        }
    }

    public static synchronized PushService b() {
        PushService pushService;
        synchronized (PushService.class) {
            pushService = H;
        }
        return pushService;
    }

    /* access modifiers changed from: private */
    public byte c() {
        long versionCode;
        long versionCode2;
        long versionCode3;
        synchronized (G) {
            versionCode = ai.b(this.M, this.L).getVersionCode();
            versionCode2 = aj.c(this.M, this.L).getVersionCode();
        }
        synchronized (G) {
            versionCode3 = ae.a(this.M, this.L).getVersionCode();
        }
        byte b = versionCode >= ((versionCode2 > versionCode3 ? 1 : (versionCode2 == versionCode3 ? 0 : -1)) > 0 ? versionCode2 : versionCode3) ? 2 : versionCode2 >= versionCode3 ? (byte) 0 : 1;
        Log.i("PushLogSys", "local pushcore version is " + versionCode + ". NC pushcore version is " + versionCode2 + ". HMS pushcore version is " + versionCode3 + ". selected version is " + ((int) b) + ", [0:NC, 1:HMS, 2:LOCAL]");
        return b;
    }

    public static void d() {
        Log.i("PushLogSys", "sys push process exit");
        Process.killProcess(Process.myPid());
    }

    public IBinder onBind(Intent intent) {
        Log.i("PushLogSys", "system push begin await");
        try {
            this.O.await();
        } catch (InterruptedException e) {
            Log.e("PushLogSys", "push core manager latch await error");
        }
        Log.i("PushLogSys", "system push await over");
        try {
            if (this.K != null) {
                Log.i("PushLogSys", "pushCoreManager onBind");
                return this.K.onBind(intent);
            }
            Log.e("PushLogSys", "onBind pushCoreManager is null");
            return null;
        } catch (Exception e2) {
            Log.e("PushLogSys", "onBind pushCoreManager onBind exception");
        }
    }

    public void onCreate() {
        if (this.M == null) {
            Log.e("PushLogSys", "context is null, oncreate failed");
            this.M = this;
        }
        super.onCreate();
        H = this;
        InstallStorageManager.initBaseDir(new File("/data/misc/hwpush"));
        synchronized (G) {
            this.L = new FeatureLocalInstallManager(this.M);
        }
        int i = -100;
        try {
            i = InstallCompat.install(this.M);
        } catch (Exception e) {
            Log.e("PushLogSys", "pre install pushcore from local error: " + ao.a(e));
        }
        Log.i("PushLogSys", "pre install pushcore result is " + i);
        this.O = new CountDownLatch(1);
        this.P.start();
        Looper looper = this.P.getLooper();
        if (looper != null) {
            new Handler(looper).post(new ab(this, i));
        }
        this.I = new ac(this, (byte) 0);
        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("com.huawei.android.push.intent.CHECK_HWPUSH_VERSION");
            ContextEx.registerReceiverAsUser(this.M, this.I, UserHandleEx.ALL, intentFilter, "com.huawei.pushagent.permission.INNER_RECEIVER", (Handler) null);
        } catch (Exception e2) {
            Log.e("PushLogSys", "register sys push inner receiver error");
        }
        this.J = new ad(this, (byte) 0);
        try {
            IntentFilter intentFilter2 = new IntentFilter();
            intentFilter2.addAction("android.intent.action.PACKAGE_ADDED");
            intentFilter2.addDataScheme("package");
            ContextEx.registerReceiverAsUser(this.M, this.J, UserHandleEx.ALL, intentFilter2, (String) null, (Handler) null);
        } catch (Exception e3) {
            Log.e("PushLogSys", "register sys push inner receiver error");
        }
    }

    public void onDestroy() {
        Log.i("PushLogSys", "sys push on destroy");
        try {
            this.M.unregisterReceiver(this.I);
        } catch (Exception e) {
            Log.e("PushLogSys", "unregister sys push inner receiver error");
        }
        try {
            this.M.unregisterReceiver(this.J);
        } catch (Exception e2) {
            Log.e("PushLogSys", "unregister sys push system receiver error");
        }
        try {
            if (this.K != null) {
                this.K.destroyPushService();
            }
        } catch (Exception e3) {
            Log.e("PushLogSys", "destroy pushcore service error");
        }
        super.onDestroy();
    }

    public int onStartCommand(Intent intent, int i, int i2) {
        Log.d("PushLogSys", "sys push onStartCommand");
        if (intent != null) {
            return 1;
        }
        Log.i("PushLogSys", "onStartCommand, intent is null, maybe restart service called by android system");
        return 1;
    }

    public boolean setParam(Service service, Bundle bundle) {
        this.M = service;
        return true;
    }
}
