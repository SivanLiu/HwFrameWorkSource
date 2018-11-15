package com.huawei.android.pushagent;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.NetworkRequest.Builder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.util.Log;
import com.huawei.android.pushagent.model.a.a;
import com.huawei.android.pushagent.model.a.b;
import com.huawei.android.pushagent.model.a.d;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.model.prefs.l;
import com.huawei.android.pushagent.utils.f.c;
import com.huawei.android.pushagent.utils.g;
import com.huawei.android.pushagent.utils.tools.ShutdownReceiver;
import java.io.File;
import java.util.LinkedList;

public class PushService extends Service {
    private static String TAG = "PushLog3413";
    private static PushService jc = null;
    private Context jb = null;
    private a jd = null;
    private NetworkCallback je = new d(this);
    private b jf;
    private LinkedList<a> jg = new LinkedList();
    private long jh = 0;
    private ShutdownReceiver ji = null;
    private boolean jj = false;

    public boolean setParam(Service service, Bundle bundle) {
        this.jb = service;
        return true;
    }

    public void onCreate() {
        if (this.jb == null) {
            Log.e(TAG, "context is null, oncreate failed");
            this.jb = this;
        }
        Thread.setDefaultUncaughtExceptionHandler(new e(this));
        super.onCreate();
        if (new File("/data/misc/hwpush").exists()) {
            try {
                c.eu(this.jb);
                c.ep(TAG, "PushService:onCreate()");
                this.jh = System.currentTimeMillis();
                try {
                    this.jf = new b(this.jb);
                    this.jf.start();
                    int i = 0;
                    while (this.jf.jl == null) {
                        int i2 = i + 1;
                        if (i > 80) {
                            c.eq(TAG, "call mReceiverDispatcher run after " + i2 + " times, " + " but handler is null");
                            stopSelf();
                            return;
                        }
                        Thread.sleep(100);
                        if (i2 % 10 == 0) {
                            c.er(TAG, "waiting for hander created times: " + i2);
                        }
                        i = i2;
                    }
                    acd(this);
                    if (com.huawei.android.pushagent.utils.tools.c.cr()) {
                        com.huawei.android.pushagent.utils.tools.c.ct(1, 0);
                    }
                    com.huawei.android.pushagent.utils.threadpool.a.cj(new f(this));
                    return;
                } catch (Throwable e) {
                    c.et(TAG, "create ReceiverDispatcher thread or get channelMgr exception ,stopself, " + e.toString(), e);
                    stopSelf();
                    return;
                }
            } catch (Throwable e2) {
                c.et(TAG, "Exception:Log.init: " + e2.toString(), e2);
                stopSelf();
                return;
            }
        }
        Log.e(TAG, "hwpush_files dir is not exist, can not work!");
        stopSelf();
    }

    private static void acd(PushService pushService) {
        jc = pushService;
    }

    private synchronized void abw() {
        abz(new d(this.jb));
        abz(new b(this.jb));
        abz(new com.huawei.android.pushagent.model.a.c(this.jb));
    }

    private void abz(a aVar) {
        this.jg.add(aVar);
    }

    private void abx() {
        try {
            c.er(TAG, "initSystem(),and mReceivers  " + this.jg.size());
            com.huawei.android.pushagent.b.a.ie(this.jb);
            abw();
            com.huawei.android.pushagent.a.a.ht(this.jb);
            com.huawei.android.pushagent.a.a.hy();
            com.huawei.android.pushagent.a.a.hx(0);
            aby(this.jb);
            aca(this.jb);
            acc(this.jb);
            com.huawei.android.pushagent.model.prefs.a.of(this.jb).oh();
            abr(new Intent("com.huawei.action.CONNECT_PUSHSRV"));
            abr(new Intent("com.huawei.action.push.intent.CHECK_CHANNEL_CYCLE"));
            c.ep(TAG, "initProcess success");
        } catch (Throwable e) {
            c.et(TAG, "Exception:registerMyReceiver: " + e.toString(), e);
            stopSelf();
        }
    }

    public static synchronized PushService abp() {
        PushService pushService;
        synchronized (PushService.class) {
            pushService = jc;
        }
        return pushService;
    }

    public static void abr(Intent intent) {
        try {
            PushService abp = abp();
            if (abp == null) {
                c.eq(TAG, "sendBroadcast error, pushService is null");
            } else {
                abp.abs(intent);
            }
        } catch (Throwable e) {
            c.es(TAG, "call PushService:broadcast() cause " + e.toString(), e);
        }
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public int onStartCommand(Intent intent, int i, int i2) {
        try {
            c.er(TAG, "PushService onStartCommand");
            if (intent != null) {
                c.er(TAG, "onStartCommand, intent is:" + intent.toURI());
                abs(intent);
            } else {
                c.ep(TAG, "onStartCommand, intent is null ,mybe restart service called by android system");
                com.huawei.android.pushagent.utils.e.a.dc(this.jb);
            }
        } catch (Throwable e) {
            c.es(TAG, "call PushService:onStartCommand() cause " + e.toString(), e);
        }
        return 1;
    }

    private synchronized void abs(Intent intent) {
        if (intent == null) {
            c.eq(TAG, "when broadcastToProcess, intent is null");
            return;
        }
        c.er(TAG, "broadcastToProcess, intent is:" + intent.getAction());
        for (a aci : this.jg) {
            this.jf.aci(aci, intent);
        }
        c.ep(TAG, "dispatchIntent over");
    }

    public static void abv() {
        c.er(TAG, "call exitProcess");
        if (jc != null) {
            jc.jj = true;
            jc.stopSelf();
        }
    }

    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:34:0x0138, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:35:0x0139, code:
            com.huawei.android.pushagent.utils.f.c.es(TAG, "call PushService:onDestroy() cause " + r0.toString(), r0);
     */
    /* JADX WARNING: Missing block: B:54:?, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onDestroy() {
        c.ep(TAG, "enter PushService:onDestroy(), needExitService is:" + jc.jj);
        try {
            abs(new Intent("com.huawei.intent.action.PUSH_OFF").putExtra("Remote_Package_Name", this.jb.getPackageName()).setPackage(this.jb.getPackageName()));
        } catch (Throwable e) {
            c.es(TAG, "call PushService:onDestroy() in broadcastToProcess cause " + e.toString(), e);
        }
        Thread.sleep(1000);
        try {
            if (this.jd != null) {
                abt();
                this.jb.unregisterReceiver(this.jd);
            }
            ((ConnectivityManager) this.jb.getSystemService("connectivity")).unregisterNetworkCallback(this.je);
            if (this.ji != null) {
                this.jb.unregisterReceiver(this.ji);
            }
        } catch (Throwable e2) {
            c.es(TAG, "call PushService:onDestroy() in unregisterInnerReceiver cause " + e2.toString(), e2);
        }
        try {
            if (!(this.jf == null || this.jf.jl == null)) {
                this.jf.jl.getLooper().quit();
            }
        } catch (Throwable e22) {
            c.es(TAG, "call PushService:onDestroy() in unregisterReceiver cause " + e22.toString(), e22);
        }
        if (!this.jj) {
            long j;
            long ur = l.ul(this.jb).ur();
            if (System.currentTimeMillis() - this.jh > k.rh(this.jb).sx() * 1000) {
                j = 0;
            } else {
                j = ur + 1;
            }
            ur = j == 0 ? k.rh(this.jb).sy() * 1000 : j == 1 ? k.rh(this.jb).sz() * 1000 : j == 2 ? k.rh(this.jb).ta() * 1000 : j >= 3 ? k.rh(this.jb).tb() * 1000 : 0;
            c.er(TAG, "next start time will be " + (ur / 1000) + " seconds later" + " run_time_less_times is " + j + "times");
            l.ul(this.jb).us(j);
            com.huawei.android.pushagent.utils.tools.d.cz(this.jb, new Intent().setClassName("android", "com.huawei.android.pushagentproxy.PushService"), ur);
            com.huawei.android.pushagent.utils.e.a.dc(this.jb);
        }
        super.onDestroy();
    }

    private void aby(Context context) {
        int i = 0;
        if (this.jd == null) {
            this.jd = new a(this, null);
        }
        IntentFilter intentFilter = new IntentFilter();
        for (String addAction : com.huawei.android.pushagent.constant.a.abl()) {
            intentFilter.addAction(addAction);
        }
        context.registerReceiverAsUser(this.jd, UserHandle.ALL, intentFilter, null, null);
        IntentFilter intentFilter2 = new IntentFilter();
        while (i < com.huawei.android.pushagent.constant.a.abm().length) {
            intentFilter2.addAction(com.huawei.android.pushagent.constant.a.abm()[i]);
            i++;
        }
        context.registerReceiverAsUser(this.jd, UserHandle.ALL, intentFilter2, "com.huawei.pushagent.permission.INNER_RECEIVER", null);
        intentFilter2 = new IntentFilter();
        intentFilter2.addAction("com.huawei.android.push.intent.REGISTER_SPECIAL");
        context.registerReceiverAsUser(this.jd, UserHandle.ALL, intentFilter2, "com.huawei.android.permission.ANTITHEFT", null);
        intentFilter2 = new IntentFilter();
        intentFilter2.addAction("com.huawei.android.push.intent.ACTION_TERMINAL_PROTOCAL");
        context.registerReceiverAsUser(this.jd, UserHandle.ALL, intentFilter2, "com.huawei.android.permission.TERMINAL_PROTOCAL", null);
        if ("android".equals(context.getPackageName())) {
            acb(context);
        }
        if (g.gr()) {
            c.ep(TAG, "register HW network policy broadcast.");
            intentFilter2 = new IntentFilter();
            intentFilter2.addAction("com.huawei.systemmanager.changedata");
            context.registerReceiverAsUser(this.jd, UserHandle.ALL, intentFilter2, "android.permission.CONNECTIVITY_INTERNAL", null);
        }
    }

    private void acb(Context context) {
        if (this.jd == null) {
            this.jd = new a(this, null);
        }
        IntentFilter intentFilter = new IntentFilter();
        for (String addAction : com.huawei.android.pushagent.constant.a.abn()) {
            intentFilter.addAction(addAction);
        }
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter2.addDataScheme("package");
        context.registerReceiverAsUser(this.jd, UserHandle.ALL, intentFilter, null, null);
        context.registerReceiverAsUser(this.jd, UserHandle.ALL, intentFilter2, null, null);
    }

    private void acc(Context context) {
        if (this.ji == null) {
            this.ji = new ShutdownReceiver();
        }
        Context context2 = context;
        context2.registerReceiverAsUser(this.ji, UserHandle.ALL, new IntentFilter("android.intent.action.ACTION_SHUTDOWN"), null, null);
    }

    private void abt() {
        com.huawei.android.pushagent.utils.tools.d.cy(this.jb, "com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT");
        com.huawei.android.pushagent.utils.tools.d.cy(this.jb, "com.huawei.push.alarm.HEARTBEAT");
    }

    public Context abq() {
        return this.jb;
    }

    private void aca(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        Builder builder = new Builder();
        builder.addCapability(13);
        builder.addTransportType(0);
        builder.addTransportType(1);
        NetworkRequest build = builder.build();
        try {
            c.ep(TAG, " registerNetworkCallback success ");
            connectivityManager.registerNetworkCallback(build, this.je);
        } catch (Exception e) {
            c.ep(TAG, " registerNetworkCallback = " + e.toString());
        }
    }

    private void abu(Context context, Network network, boolean z) {
        abr(new Intent("com.huawei.push.action.NET_CHANGED"));
    }
}
