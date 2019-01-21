package com.huawei.android.pushagent.model.flowcontrol;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.datatype.exception.PushException.ErrorType;
import com.huawei.android.pushagent.model.c.e;
import com.huawei.android.pushagent.model.prefs.f;
import com.huawei.android.pushagent.utils.b.a;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class b {
    private static long eo = 300000;
    private static long ep = 600000;
    private static long eq = 1800000;
    private static int er = 4;
    private static String et = "00:00";
    private static String eu = "06:00";
    private static long ev = 300000;
    private static long ew = 600000;
    private static b ex = null;
    private static final /* synthetic */ int[] ey = null;
    private ArrayList<c> en = new ArrayList();
    private int es = 0;

    private static /* synthetic */ int[] op() {
        if (ey != null) {
            return ey;
        }
        int[] iArr = new int[ReconnectMgr$RECONNECTEVENT.values().length];
        try {
            iArr[ReconnectMgr$RECONNECTEVENT.NETWORK_CHANGE.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[ReconnectMgr$RECONNECTEVENT.SOCKET_CLOSE.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[ReconnectMgr$RECONNECTEVENT.SOCKET_CONNECTED.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[ReconnectMgr$RECONNECTEVENT.SOCKET_REG_SUCCESS.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[ReconnectMgr$RECONNECTEVENT.TRS_QUERIED.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        ey = iArr;
        return iArr;
    }

    private b() {
    }

    public static synchronized b nz(Context context) {
        b bVar;
        synchronized (b.class) {
            if (ex == null) {
                ex = new b();
            }
            if (ex.en.isEmpty()) {
                ex.ok(context);
            }
            bVar = ex;
        }
        return bVar;
    }

    private void ok(Context context) {
        int i;
        int size;
        ol(context);
        String kk = f.ke(context).kk();
        if (!TextUtils.isEmpty(kk)) {
            a.st("PushLog3414", "connectPushSvrInfos is " + kk);
            for (String str : kk.split("\\|")) {
                c cVar = new c();
                if (cVar.ot(str)) {
                    this.en.add(cVar);
                }
            }
        }
        Collections.sort(this.en);
        if (this.en.size() > er) {
            ArrayList arrayList = new ArrayList();
            size = this.en.size() - er;
            for (i = 0; i < size; i++) {
                arrayList.add((c) this.en.get(i));
            }
            this.en.removeAll(arrayList);
        }
    }

    private void om(Context context, boolean z) {
        a.st("PushLog3414", "save connection info " + z);
        long currentTimeMillis = System.currentTimeMillis();
        long j = oj() ? eq : ep;
        ArrayList arrayList = new ArrayList();
        for (c cVar : this.en) {
            if (currentTimeMillis < cVar.or() || currentTimeMillis - cVar.or() > j) {
                arrayList.add(cVar);
            }
        }
        if (!arrayList.isEmpty()) {
            a.st("PushLog3414", "some connection info is expired:" + arrayList.size());
            this.en.removeAll(arrayList);
        }
        c cVar2 = new c();
        cVar2.ou(z);
        cVar2.ov(System.currentTimeMillis());
        if (this.en.size() < er) {
            this.en.add(cVar2);
        } else {
            this.en.remove(0);
            this.en.add(cVar2);
        }
        String str = "|";
        StringBuffer stringBuffer = new StringBuffer();
        for (c cVar22 : this.en) {
            stringBuffer.append(cVar22.toString());
            stringBuffer.append(str);
        }
        stringBuffer.deleteCharAt(stringBuffer.length() - 1);
        f.ke(context).kn(stringBuffer.toString());
    }

    private void oo(Context context) {
        if (!oh(context)) {
            a.st("PushLog3414", "It is not bad network mode, do nothing");
        } else if (this.en.isEmpty()) {
            on(context, false);
        } else {
            c cVar = (c) this.en.get(this.en.size() - 1);
            if (cVar.os()) {
                a.st("PushLog3414", "last connection is success");
                long currentTimeMillis = System.currentTimeMillis();
                long or = cVar.or();
                if (currentTimeMillis - or > eo || currentTimeMillis < or) {
                    a.st("PushLog3414", eo + " has passed since last connect");
                    on(context, false);
                } else {
                    a.st("PushLog3414", "connection keep too short , still in bad network mode");
                }
            } else {
                a.st("PushLog3414", "last connection result is false , still in bad network mode");
            }
        }
    }

    public long oa(Context context) {
        long of = of(context);
        long oe = oe(context);
        if (of > 0 && of >= oe) {
            com.huawei.android.pushagent.b.a.abc(44, String.valueOf(of));
        } else if (oe > 0 && of < oe) {
            com.huawei.android.pushagent.b.a.abc(45, String.valueOf(oe));
        }
        return Math.max(of, oe);
    }

    private long of(Context context) {
        if (this.en.isEmpty()) {
            a.st("PushLog3414", "first connection, return 0");
            return 0;
        }
        long hj;
        switch (this.es) {
            case 0:
                hj = com.huawei.android.pushagent.model.prefs.a.ff(context).hj() * 1000;
                break;
            case 1:
                hj = com.huawei.android.pushagent.model.prefs.a.ff(context).hk() * 1000;
                break;
            case 2:
                hj = com.huawei.android.pushagent.model.prefs.a.ff(context).hl() * 1000;
                break;
            case 3:
                hj = com.huawei.android.pushagent.model.prefs.a.ff(context).hm() * 1000;
                break;
            case 4:
                hj = com.huawei.android.pushagent.model.prefs.a.ff(context).hn() * 1000;
                break;
            case 5:
                hj = com.huawei.android.pushagent.model.prefs.a.ff(context).ho() * 1000;
                break;
            case 6:
                hj = com.huawei.android.pushagent.model.prefs.a.ff(context).hp() * 1000;
                break;
            default:
                hj = com.huawei.android.pushagent.model.prefs.a.ff(context).hq() * 1000;
                break;
        }
        if (((long) this.es) == com.huawei.android.pushagent.model.prefs.a.ff(context).ht()) {
            e.pw(context).qd();
            a.sv("PushLog3414", "reconnect pushserver failed " + this.es + " times, set force query TRS at next connect.");
        }
        long currentTimeMillis = System.currentTimeMillis();
        long ow = ((c) this.en.get(this.en.size() - 1)).fg;
        if (currentTimeMillis < ow) {
            a.st("PushLog3414", "now is less than last connect time");
            ow = 0;
        } else {
            ow = Math.max((ow + hj) - currentTimeMillis, 0);
        }
        a.sv("PushLog3414", "reconnect pushserver failed, the next reconnect time is:" + this.es + " after " + ow + " ms");
        return ow;
    }

    private long oe(Context context) {
        if (oi()) {
            on(context, true);
        }
        boolean oh = oh(context);
        a.st("PushLog3414", "bad network mode is " + oh);
        if (!oh || this.en.isEmpty()) {
            return 0;
        }
        long currentTimeMillis = System.currentTimeMillis();
        long ow = ((c) this.en.get(this.en.size() - 1)).fg;
        long j = oj() ? ew : ev;
        if (currentTimeMillis < ow) {
            a.st("PushLog3414", "now is less than last connect time");
            j = 0;
        } else {
            j = Math.max((j + ow) - currentTimeMillis, 0);
        }
        a.st("PushLog3414", "It is in bad network mode, connect limit interval is " + j);
        return j;
    }

    private boolean oh(Context context) {
        return com.huawei.android.pushagent.model.prefs.e.jj(context).jw();
    }

    private void on(Context context, boolean z) {
        a.st("PushLog3414", "set bad network mode " + z);
        com.huawei.android.pushagent.model.prefs.e.jj(context).jx(z);
    }

    private boolean oj() {
        try {
            String format = new SimpleDateFormat("HH:mm").format(new Date());
            if (format.compareTo(et) > 0 && format.compareTo(eu) < 0) {
                a.st("PushLog3414", "It is in Idle period.");
                return true;
            }
        } catch (RuntimeException e) {
            a.st("PushLog3414", "format idle perild time RuntimeException.");
        } catch (Exception e2) {
            a.st("PushLog3414", "format idle perild time exception.");
        }
        return false;
    }

    private boolean oi() {
        long currentTimeMillis = System.currentTimeMillis();
        long j = oj() ? eq : ep;
        int i = 0;
        for (c cVar : this.en) {
            if (currentTimeMillis > cVar.or() && currentTimeMillis - cVar.or() < j) {
                i++;
            }
            i = i;
        }
        a.sv("PushLog3414", "The connect range limit is: " + er + " times in " + j + ", " + "current count is:" + i);
        if (i < er) {
            return false;
        }
        return true;
    }

    private void od() {
        this.es = 0;
    }

    private void og() {
        this.es++;
    }

    public void ob(Context context, ReconnectMgr$RECONNECTEVENT reconnectMgr$RECONNECTEVENT, Bundle bundle) {
        a.st("PushLog3414", "receive reconnectevent:" + reconnectMgr$RECONNECTEVENT);
        switch (op()[reconnectMgr$RECONNECTEVENT.ordinal()]) {
            case 1:
                od();
                return;
            case 2:
                ErrorType errorType = ErrorType.Err_unKnown;
                oo(context);
                if (bundle.containsKey("errorType")) {
                    if (ErrorType.Err_Connect == ((ErrorType) bundle.getSerializable("errorType"))) {
                        om(context, false);
                        com.huawei.android.pushagent.b.a.abd(54);
                    } else {
                        a.st("PushLog3414", "socket close not caused by connect error, do not need save connection info");
                    }
                } else {
                    a.st("PushLog3414", "socket close not caused by pushException");
                }
                og();
                PushService.abv(new Intent("com.huawei.action.CONNECT_PUSHSRV"));
                return;
            case 3:
                om(context, true);
                return;
            case 4:
                od();
                return;
            case 5:
                od();
                return;
            default:
                return;
        }
    }

    private void ol(Context context) {
        er = com.huawei.android.pushagent.model.prefs.a.ff(context).gp();
        ep = com.huawei.android.pushagent.model.prefs.a.ff(context).ga();
        eq = com.huawei.android.pushagent.model.prefs.a.ff(context).gb();
        eo = com.huawei.android.pushagent.model.prefs.a.ff(context).go();
        ev = com.huawei.android.pushagent.model.prefs.a.ff(context).hd();
        ew = com.huawei.android.pushagent.model.prefs.a.ff(context).he();
        et = com.huawei.android.pushagent.model.prefs.a.ff(context).gm();
        eu = com.huawei.android.pushagent.model.prefs.a.ff(context).gn();
    }

    public void oc(Context context) {
        a.sv("PushLog3414", "enter resetReconnectConfig");
        ol(context);
        this.en.clear();
        on(context, false);
        f.ke(context).km();
    }
}
