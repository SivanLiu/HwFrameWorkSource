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
import com.huawei.android.pushagent.model.d.c;
import com.huawei.android.pushagent.model.d.d;
import com.huawei.android.pushagent.model.prefs.e;
import com.huawei.android.pushagent.model.prefs.f;
import com.huawei.android.pushagent.utils.b.a;
import com.huawei.android.pushagent.utils.tools.ShutdownReceiver;
import com.huawei.android.pushagent.utils.tools.b;
import java.io.File;
import java.util.LinkedList;

public class PushService extends Service {
    private static String TAG = "PushLog3414";
    private static PushService jk = null;
    private Context jj = null;
    private c jl = null;
    private NetworkCallback jm = new e(this);
    private a jn;
    private LinkedList<c> jo = new LinkedList();
    private long jp = 0;
    private ShutdownReceiver jq = null;
    private boolean jr = false;

    public boolean setParam(Service service, Bundle bundle) {
        this.jj = service;
        return true;
    }

    public void onCreate() {
        if (this.jj == null) {
            Log.e(TAG, "context is null, oncreate failed");
            this.jj = this;
        }
        Thread.setDefaultUncaughtExceptionHandler(new f(this));
        super.onCreate();
        if (new File("/data/misc/hwpush").exists()) {
            try {
                a.tb(this.jj);
                a.sv(TAG, "PushService:onCreate()");
                this.jp = System.currentTimeMillis();
                try {
                    this.jn = new a(this.jj);
                    this.jn.start();
                    int i = 0;
                    while (this.jn.jd == null) {
                        int i2 = i + 1;
                        if (i > 80) {
                            a.su(TAG, "call mReceiverDispatcher run after " + i2 + " times, " + " but handler is null");
                            stopSelf();
                            return;
                        }
                        Thread.sleep(100);
                        if (i2 % 10 == 0) {
                            a.st(TAG, "waiting for hander created times: " + i2);
                        }
                        i = i2;
                    }
                    ach(this);
                    if (b.sf()) {
                        b.sg(1, 0);
                    }
                    com.huawei.android.pushagent.utils.threadpool.c.sq(new g(this));
                    return;
                } catch (Exception e) {
                    a.sy(TAG, "create ReceiverDispatcher thread or get channelMgr exception ,stopself, " + e.toString(), e);
                    stopSelf();
                    return;
                }
            } catch (Exception e2) {
                a.sy(TAG, "Exception:Log.init: " + e2.toString(), e2);
                stopSelf();
                return;
            }
        }
        Log.e(TAG, "hwpush_files dir is not exist, can not work!");
        stopSelf();
    }

    private static void ach(PushService pushService) {
        jk = pushService;
    }

    private synchronized void aca() {
        acd(new com.huawei.android.pushagent.model.d.b(this.jj));
        acd(new com.huawei.android.pushagent.model.d.a(this.jj));
        acd(new d(this.jj));
    }

    private void acd(c cVar) {
        this.jo.add(cVar);
    }

    private void acb() {
        try {
            a.st(TAG, "initSystem(),and mReceivers  " + this.jo.size());
            com.huawei.android.pushagent.a.c.aan(this.jj);
            aca();
            com.huawei.android.pushagent.b.a.abg(this.jj);
            com.huawei.android.pushagent.b.a.abj();
            com.huawei.android.pushagent.b.a.abd(0);
            acc(this.jj);
            ace(this.jj);
            acg(this.jj);
            f.ke(this.jj).kg();
            abv(new Intent("com.huawei.action.CONNECT_PUSHSRV"));
            abv(new Intent("com.huawei.action.push.intent.CHECK_CHANNEL_CYCLE"));
            a.sv(TAG, "initProcess success");
        } catch (Exception e) {
            a.sy(TAG, "Exception:registerMyReceiver: " + e.toString(), e);
            stopSelf();
        }
    }

    public static synchronized PushService abt() {
        PushService pushService;
        synchronized (PushService.class) {
            pushService = jk;
        }
        return pushService;
    }

    public static void abv(Intent intent) {
        try {
            PushService abt = abt();
            if (abt == null) {
                a.su(TAG, "sendBroadcast error, pushService is null");
            } else {
                abt.abx(intent);
            }
        } catch (Exception e) {
            a.sw(TAG, "call PushService:broadcast() cause " + e.toString(), e);
        }
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public int onStartCommand(Intent intent, int i, int i2) {
        try {
            a.st(TAG, "PushService onStartCommand");
            if (intent != null) {
                a.st(TAG, "onStartCommand, intent is:" + intent.toURI());
                abx(intent);
            } else {
                a.sv(TAG, "onStartCommand, intent is null ,mybe restart service called by android system");
                com.huawei.android.pushagent.utils.f.a.xt(this.jj);
            }
        } catch (Exception e) {
            a.sw(TAG, "call PushService:onStartCommand() cause " + e.toString(), e);
        }
        return 1;
    }

    private synchronized void abx(Intent intent) {
        if (intent == null) {
            a.su(TAG, "when broadcastToProcess, intent is null");
            return;
        }
        a.st(TAG, "broadcastToProcess, intent is:" + intent.getAction());
        for (c abp : this.jo) {
            this.jn.abp(abp, intent);
        }
        a.sv(TAG, "dispatchIntent over");
    }

    public static void abw() {
        a.st(TAG, "call exitProcess");
        if (jk != null) {
            jk.jr = true;
            jk.stopSelf();
        }
    }

    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:34:0x0138, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:35:0x0139, code skipped:
            com.huawei.android.pushagent.utils.b.a.sw(TAG, "call PushService:onDestroy() cause " + r0.toString(), r0);
     */
    /* JADX WARNING: Missing block: B:54:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onDestroy() {
        a.sv(TAG, "enter PushService:onDestroy(), needExitService is:" + jk.jr);
        try {
            abx(new Intent("com.huawei.intent.action.PUSH_OFF").putExtra("Remote_Package_Name", this.jj.getPackageName()).setPackage(this.jj.getPackageName()));
        } catch (Exception e) {
            a.sw(TAG, "call PushService:onDestroy() in broadcastToProcess cause " + e.toString(), e);
        }
        Thread.sleep(1000);
        try {
            if (this.jl != null) {
                aby();
                this.jj.unregisterReceiver(this.jl);
            }
            ((ConnectivityManager) this.jj.getSystemService("connectivity")).unregisterNetworkCallback(this.jm);
            if (this.jq != null) {
                this.jj.unregisterReceiver(this.jq);
            }
        } catch (Exception e2) {
            a.sw(TAG, "call PushService:onDestroy() in unregisterInnerReceiver cause " + e2.toString(), e2);
        }
        try {
            if (!(this.jn == null || this.jn.jd == null)) {
                this.jn.jd.getLooper().quit();
            }
        } catch (Exception e22) {
            a.sw(TAG, "call PushService:onDestroy() in unregisterReceiver cause " + e22.toString(), e22);
        }
        if (!this.jr) {
            long j;
            long jt = e.jj(this.jj).jt();
            if (System.currentTimeMillis() - this.jp > com.huawei.android.pushagent.model.prefs.a.ff(this.jj).hc() * 1000) {
                j = 0;
            } else {
                j = jt + 1;
            }
            jt = j == 0 ? com.huawei.android.pushagent.model.prefs.a.ff(this.jj).gx() * 1000 : j == 1 ? com.huawei.android.pushagent.model.prefs.a.ff(this.jj).gy() * 1000 : j == 2 ? com.huawei.android.pushagent.model.prefs.a.ff(this.jj).gz() * 1000 : j >= 3 ? com.huawei.android.pushagent.model.prefs.a.ff(this.jj).ha() * 1000 : 0;
            a.st(TAG, "next start time will be " + (jt / 1000) + " seconds later" + " run_time_less_times is " + j + "times");
            e.jj(this.jj).kb(j);
            com.huawei.android.pushagent.utils.tools.a.sd(this.jj, new Intent().setClassName("android", "com.huawei.android.pushagentproxy.PushService"), jt);
            com.huawei.android.pushagent.utils.f.a.xt(this.jj);
        }
        super.onDestroy();
    }

    private void acc(Context context) {
        int i = 0;
        if (this.jl == null) {
            this.jl = new c(this, null);
        }
        IntentFilter intentFilter = new IntentFilter();
        for (String addAction : com.huawei.android.pushagent.constant.b.bc()) {
            intentFilter.addAction(addAction);
        }
        context.registerReceiverAsUser(this.jl, UserHandle.ALL, intentFilter, null, null);
        IntentFilter intentFilter2 = new IntentFilter();
        while (i < com.huawei.android.pushagent.constant.b.be().length) {
            intentFilter2.addAction(com.huawei.android.pushagent.constant.b.be()[i]);
            i++;
        }
        context.registerReceiverAsUser(this.jl, UserHandle.ALL, intentFilter2, "com.huawei.pushagent.permission.INNER_RECEIVER", null);
        intentFilter2 = new IntentFilter();
        intentFilter2.addAction("com.huawei.android.push.intent.REGISTER_SPECIAL");
        context.registerReceiverAsUser(this.jl, UserHandle.ALL, intentFilter2, "com.huawei.android.permission.ANTITHEFT", null);
        intentFilter2 = new IntentFilter();
        intentFilter2.addAction("com.huawei.android.push.intent.ACTION_TERMINAL_PROTOCAL");
        context.registerReceiverAsUser(this.jl, UserHandle.ALL, intentFilter2, "com.huawei.android.permission.TERMINAL_PROTOCAL", null);
        if ("android".equals(context.getPackageName())) {
            acf(context);
        }
        if (com.huawei.android.pushagent.utils.d.zn()) {
            a.sv(TAG, "register HW network policy broadcast.");
            intentFilter2 = new IntentFilter();
            intentFilter2.addAction("com.huawei.systemmanager.changedata");
            context.registerReceiverAsUser(this.jl, UserHandle.ALL, intentFilter2, "android.permission.CONNECTIVITY_INTERNAL", null);
        }
    }

    private void acf(Context context) {
        if (this.jl == null) {
            this.jl = new c(this, null);
        }
        IntentFilter intentFilter = new IntentFilter();
        for (String addAction : com.huawei.android.pushagent.constant.b.bd()) {
            intentFilter.addAction(addAction);
        }
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter2.addDataScheme("package");
        context.registerReceiverAsUser(this.jl, UserHandle.ALL, intentFilter, null, null);
        context.registerReceiverAsUser(this.jl, UserHandle.ALL, intentFilter2, null, null);
    }

    private void acg(Context context) {
        if (this.jq == null) {
            this.jq = new ShutdownReceiver();
        }
        Context context2 = context;
        context2.registerReceiverAsUser(this.jq, UserHandle.ALL, new IntentFilter("android.intent.action.ACTION_SHUTDOWN"), null, null);
    }

    private void aby() {
        com.huawei.android.pushagent.utils.tools.a.sc(this.jj, "com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT");
        com.huawei.android.pushagent.utils.tools.a.sc(this.jj, "com.huawei.push.alarm.HEARTBEAT");
    }

    public Context abu() {
        return this.jj;
    }

    private void ace(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        Builder builder = new Builder();
        builder.addCapability(13);
        builder.addTransportType(0);
        builder.addTransportType(1);
        NetworkRequest build = builder.build();
        try {
            a.sv(TAG, " registerNetworkCallback success ");
            connectivityManager.registerNetworkCallback(build, this.jm);
        } catch (Exception e) {
            a.sv(TAG, " registerNetworkCallback = " + e.toString());
        }
    }

    private void abz(Context context, Network network, boolean z) {
        abv(new Intent("com.huawei.push.action.NET_CHANGED"));
    }
}
