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
import com.huawei.android.pushagent.model.a.c;
import com.huawei.android.pushagent.model.a.d;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.f;
import com.huawei.android.pushagent.utils.tools.ShutdownReceiver;
import com.huawei.android.pushagent.utils.tools.a;
import java.io.File;
import java.util.LinkedList;

public class PushService extends Service {
    private static String TAG = "PushLog2976";
    private static PushService ix = null;
    private Context iw = null;
    private a iy = null;
    private NetworkCallback iz = new d(this);
    private b ja;
    private LinkedList<d> jb = new LinkedList();
    private long jc = 0;
    private ShutdownReceiver jd = null;
    private boolean je = false;

    public boolean setParam(Service service, Bundle bundle) {
        this.iw = service;
        return true;
    }

    public void onCreate() {
        if (this.iw == null) {
            Log.e(TAG, "context is null, oncreate failed");
            this.iw = this;
        }
        Thread.setDefaultUncaughtExceptionHandler(new e(this));
        super.onCreate();
        if (new File("/data/misc/hwpush").exists()) {
            try {
                b.ad(this.iw);
                b.z(TAG, "PushService:onCreate()");
                this.jc = System.currentTimeMillis();
                try {
                    this.ja = new b(this.iw);
                    this.ja.start();
                    int i = 0;
                    while (this.ja.jg == null) {
                        int i2 = i + 1;
                        if (i > 80) {
                            b.y(TAG, "call mReceiverDispatcher run after " + i2 + " times, " + " but handler is null");
                            stopSelf();
                            return;
                        }
                        Thread.sleep(100);
                        if (i2 % 10 == 0) {
                            b.x(TAG, "waiting for hander created times: " + i2);
                        }
                        i = i2;
                    }
                    abl(this);
                    if (a.j()) {
                        a.g(1, 0);
                    }
                    com.huawei.android.pushagent.utils.threadpool.b.c(new f(this));
                    return;
                } catch (Throwable e) {
                    b.ae(TAG, "create ReceiverDispatcher thread or get channelMgr exception ,stopself, " + e.toString(), e);
                    stopSelf();
                    return;
                }
            } catch (Throwable e2) {
                b.ae(TAG, "Exception:Log.init: " + e2.toString(), e2);
                stopSelf();
                return;
            }
        }
        Log.e(TAG, "hwpush_files dir is not exist, can not work!");
        stopSelf();
    }

    private static void abl(PushService pushService) {
        ix = pushService;
    }

    private synchronized void abe() {
        abh(new c(this.iw));
        abh(new com.huawei.android.pushagent.model.a.b(this.iw));
        abh(new com.huawei.android.pushagent.model.a.a(this.iw));
    }

    private void abh(d dVar) {
        this.jb.add(dVar);
    }

    private void abf() {
        try {
            b.x(TAG, "initSystem(),and mReceivers  " + this.jb.size());
            com.huawei.android.pushagent.a.a.xi(this.iw);
            abe();
            com.huawei.android.pushagent.b.a.aam(this.iw);
            com.huawei.android.pushagent.b.a.aan();
            com.huawei.android.pushagent.b.a.aak(0);
            abg(this.iw);
            abi(this.iw);
            abk(this.iw);
            aax(new Intent("com.huawei.action.CONNECT_PUSHSRV"));
            aax(new Intent("com.huawei.action.push.intent.CHECK_CHANNEL_CYCLE"));
            b.z(TAG, "initProcess success");
        } catch (Throwable e) {
            b.ae(TAG, "Exception:registerMyReceiver: " + e.toString(), e);
            stopSelf();
        }
    }

    public static synchronized PushService abd() {
        PushService pushService;
        synchronized (PushService.class) {
            pushService = ix;
        }
        return pushService;
    }

    public static void aax(Intent intent) {
        try {
            PushService abd = abd();
            if (abd == null) {
                b.y(TAG, "sendBroadcast error, pushService is null");
            } else {
                abd.aay(intent);
            }
        } catch (Throwable e) {
            b.aa(TAG, "call PushService:broadcast() cause " + e.toString(), e);
        }
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public int onStartCommand(Intent intent, int i, int i2) {
        try {
            b.x(TAG, "PushService onStartCommand");
            if (intent != null) {
                b.x(TAG, "onStartCommand, intent is:" + intent.toURI());
                aay(intent);
            } else {
                b.z(TAG, "onStartCommand, intent is null ,mybe restart service called by android system");
                com.huawei.android.pushagent.utils.d.a.cy(this.iw);
            }
        } catch (Throwable e) {
            b.aa(TAG, "call PushService:onStartCommand() cause " + e.toString(), e);
        }
        return 1;
    }

    private synchronized void aay(Intent intent) {
        if (intent == null) {
            b.y(TAG, "when broadcastToProcess, intent is null");
            return;
        }
        b.x(TAG, "broadcastToProcess, intent is:" + intent.getAction());
        for (d abq : this.jb) {
            this.ja.abq(abq, intent);
        }
        b.z(TAG, "dispatchIntent over");
    }

    public static void abb() {
        b.x(TAG, "call exitProcess");
        if (ix != null) {
            ix.je = true;
            ix.stopSelf();
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onDestroy() {
        b.z(TAG, "enter PushService:onDestroy(), needExitService is:" + ix.je);
        try {
            aay(new Intent("com.huawei.intent.action.PUSH_OFF").putExtra("Remote_Package_Name", this.iw.getPackageName()).setPackage(this.iw.getPackageName()));
        } catch (Throwable e) {
            b.aa(TAG, "call PushService:onDestroy() in broadcastToProcess cause " + e.toString(), e);
        }
        Thread.sleep(1000);
        try {
            if (this.iy != null) {
                aaz();
                this.iw.unregisterReceiver(this.iy);
            }
            ((ConnectivityManager) this.iw.getSystemService("connectivity")).unregisterNetworkCallback(this.iz);
            if (this.jd != null) {
                this.iw.unregisterReceiver(this.jd);
            }
        } catch (Throwable e2) {
            b.aa(TAG, "call PushService:onDestroy() in unregisterInnerReceiver cause " + e2.toString(), e2);
        }
        try {
            if (!(this.ja == null || this.ja.jg == null)) {
                this.ja.jg.getLooper().quit();
            }
        } catch (Throwable e22) {
            b.aa(TAG, "call PushService:onDestroy() in unregisterReceiver cause " + e22.toString(), e22);
        }
        if (!this.je) {
            long j;
            long px = k.pt(this.iw).px();
            if (System.currentTimeMillis() - this.jc > i.mj(this.iw).mx() * 1000) {
                j = 0;
            } else {
                j = px + 1;
            }
            px = j == 0 ? i.mj(this.iw).my() * 1000 : j == 1 ? i.mj(this.iw).mz() * 1000 : j == 2 ? i.mj(this.iw).na() * 1000 : j >= 3 ? i.mj(this.iw).nb() * 1000 : 0;
            b.x(TAG, "next start time will be " + (px / 1000) + " seconds later" + " run_time_less_times is " + j + "times");
            k.pt(this.iw).py(j);
            com.huawei.android.pushagent.utils.tools.d.q(this.iw, new Intent().setClassName("android", "com.huawei.android.pushagentproxy.PushService"), px);
            com.huawei.android.pushagent.utils.d.a.cy(this.iw);
        }
        super.onDestroy();
    }

    private void abg(Context context) {
        int i = 0;
        if (this.iy == null) {
            this.iy = new a();
        }
        IntentFilter intentFilter = new IntentFilter();
        for (String addAction : com.huawei.android.pushagent.constant.a.xe()) {
            intentFilter.addAction(addAction);
        }
        context.registerReceiverAsUser(this.iy, UserHandle.ALL, intentFilter, null, null);
        IntentFilter intentFilter2 = new IntentFilter();
        while (i < com.huawei.android.pushagent.constant.a.xg().length) {
            intentFilter2.addAction(com.huawei.android.pushagent.constant.a.xg()[i]);
            i++;
        }
        context.registerReceiverAsUser(this.iy, UserHandle.ALL, intentFilter2, "com.huawei.pushagent.permission.INNER_RECEIVER", null);
        intentFilter2 = new IntentFilter();
        intentFilter2.addAction("com.huawei.android.push.intent.REGISTER_SPECIAL");
        context.registerReceiverAsUser(this.iy, UserHandle.ALL, intentFilter2, "com.huawei.android.permission.ANTITHEFT", null);
        intentFilter2 = new IntentFilter();
        intentFilter2.addAction("com.huawei.android.push.intent.ACTION_TERMINAL_PROTOCAL");
        context.registerReceiverAsUser(this.iy, UserHandle.ALL, intentFilter2, "com.huawei.android.permission.TERMINAL_PROTOCAL", null);
        if ("android".equals(context.getPackageName())) {
            abj(context);
        }
        if (f.gd()) {
            b.z(TAG, "register HW network policy broadcast.");
            intentFilter2 = new IntentFilter();
            intentFilter2.addAction("com.huawei.systemmanager.changedata");
            context.registerReceiverAsUser(this.iy, UserHandle.ALL, intentFilter2, "android.permission.CONNECTIVITY_INTERNAL", null);
        }
    }

    private void abj(Context context) {
        if (this.iy == null) {
            this.iy = new a();
        }
        IntentFilter intentFilter = new IntentFilter();
        for (String addAction : com.huawei.android.pushagent.constant.a.xf()) {
            intentFilter.addAction(addAction);
        }
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter2.addDataScheme("package");
        context.registerReceiverAsUser(this.iy, UserHandle.ALL, intentFilter, null, null);
        context.registerReceiverAsUser(this.iy, UserHandle.ALL, intentFilter2, null, null);
    }

    private void abk(Context context) {
        if (this.jd == null) {
            this.jd = new ShutdownReceiver();
        }
        Context context2 = context;
        context2.registerReceiverAsUser(this.jd, UserHandle.ALL, new IntentFilter("android.intent.action.ACTION_SHUTDOWN"), null, null);
    }

    private void aaz() {
        com.huawei.android.pushagent.utils.tools.d.o(this.iw, "com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT");
        com.huawei.android.pushagent.utils.tools.d.r(this.iw, new Intent("com.huawei.intent.action.PUSH").putExtra("EXTRA_INTENT_TYPE", "com.huawei.android.push.intent.HEARTBEAT_REQ").setPackage(this.iw.getPackageName()));
    }

    public Context abc() {
        return this.iw;
    }

    private void abi(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        Builder builder = new Builder();
        builder.addCapability(13);
        builder.addTransportType(0);
        builder.addTransportType(1);
        NetworkRequest build = builder.build();
        try {
            b.z(TAG, " registerNetworkCallback success ");
            connectivityManager.registerNetworkCallback(build, this.iz);
        } catch (Exception e) {
            b.z(TAG, " registerNetworkCallback = " + e.toString());
        }
    }

    private void aba(Context context, Network network, boolean z) {
        aax(new Intent("com.huawei.push.action.NET_CHANGED"));
    }
}
