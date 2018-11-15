package com.huawei.android.pushagent.model.flowcontrol;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.datatype.exception.PushException.ErrorType;
import com.huawei.android.pushagent.model.b.f;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.model.prefs.l;
import com.huawei.android.pushagent.utils.f.c;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

public class a {
    private static long hu = 300000;
    private static long hv = 600000;
    private static long hw = 1800000;
    private static int hx = 4;
    private static String hz = "00:00";
    private static String ia = "06:00";
    private static long ib = 300000;
    private static long ic = 600000;
    private static a id = null;
    private static final /* synthetic */ int[] ie = null;
    private ArrayList<b> ht = new ArrayList();
    private int hy = 0;

    private static /* synthetic */ int[] aal() {
        if (ie != null) {
            return ie;
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
        ie = iArr;
        return iArr;
    }

    private a() {
    }

    public static synchronized a zx(Context context) {
        a aVar;
        synchronized (a.class) {
            if (id == null) {
                id = new a();
            }
            if (id.ht.isEmpty()) {
                id.aae(context);
            }
            aVar = id;
        }
        return aVar;
    }

    private void aae(Context context) {
        int i;
        int size;
        aaf(context);
        String oi = com.huawei.android.pushagent.model.prefs.a.of(context).oi();
        if (!TextUtils.isEmpty(oi)) {
            c.er("PushLog3413", "connectPushSvrInfos is " + oi);
            for (String str : oi.split("\\|")) {
                b bVar = new b();
                if (bVar.aap(str)) {
                    this.ht.add(bVar);
                }
            }
        }
        Collections.sort(this.ht);
        if (this.ht.size() > hx) {
            Collection arrayList = new ArrayList();
            size = this.ht.size() - hx;
            for (i = 0; i < size; i++) {
                arrayList.add((b) this.ht.get(i));
            }
            this.ht.removeAll(arrayList);
        }
    }

    private void aai(Context context, boolean z) {
        c.er("PushLog3413", "save connection info " + z);
        long currentTimeMillis = System.currentTimeMillis();
        long j = aad() ? hw : hv;
        Collection arrayList = new ArrayList();
        for (b bVar : this.ht) {
            if (currentTimeMillis < bVar.aan() || currentTimeMillis - bVar.aan() > j) {
                arrayList.add(bVar);
            }
        }
        if (!arrayList.isEmpty()) {
            c.er("PushLog3413", "some connection info is expired:" + arrayList.size());
            this.ht.removeAll(arrayList);
        }
        b bVar2 = new b();
        bVar2.aaq(z);
        bVar2.aar(System.currentTimeMillis());
        if (this.ht.size() < hx) {
            this.ht.add(bVar2);
        } else {
            this.ht.remove(0);
            this.ht.add(bVar2);
        }
        String str = "|";
        StringBuffer stringBuffer = new StringBuffer();
        for (b bVar22 : this.ht) {
            stringBuffer.append(bVar22.toString());
            stringBuffer.append(str);
        }
        stringBuffer.deleteCharAt(stringBuffer.length() - 1);
        com.huawei.android.pushagent.model.prefs.a.of(context).on(stringBuffer.toString());
    }

    private void aak(Context context) {
        if (!aab(context)) {
            c.er("PushLog3413", "It is not bad network mode, do nothing");
        } else if (this.ht.isEmpty()) {
            aaj(context, false);
        } else {
            b bVar = (b) this.ht.get(this.ht.size() - 1);
            if (bVar.aao()) {
                c.er("PushLog3413", "last connection is success");
                long currentTimeMillis = System.currentTimeMillis();
                long aan = bVar.aan();
                if (currentTimeMillis - aan > hu || currentTimeMillis < aan) {
                    c.er("PushLog3413", hu + " has passed since last connect");
                    aaj(context, false);
                } else {
                    c.er("PushLog3413", "connection keep too short , still in bad network mode");
                }
            } else {
                c.er("PushLog3413", "last connection result is false , still in bad network mode");
            }
        }
    }

    public long zz(Context context) {
        long zy = zy(context);
        long zw = zw(context);
        if (zy > 0 && zy >= zw) {
            com.huawei.android.pushagent.a.a.hq(44, String.valueOf(zy));
        } else if (zw > 0 && zy < zw) {
            com.huawei.android.pushagent.a.a.hq(45, String.valueOf(zw));
        }
        return Math.max(zy, zw);
    }

    private long zy(Context context) {
        if (this.ht.isEmpty()) {
            c.er("PushLog3413", "first connection, return 0");
            return 0;
        }
        long sf;
        switch (this.hy) {
            case 0:
                sf = k.rh(context).sf() * 1000;
                break;
            case 1:
                sf = k.rh(context).sg() * 1000;
                break;
            case 2:
                sf = k.rh(context).sh() * 1000;
                break;
            case 3:
                sf = k.rh(context).si() * 1000;
                break;
            case 4:
                sf = k.rh(context).sj() * 1000;
                break;
            case 5:
                sf = k.rh(context).sk() * 1000;
                break;
            case 6:
                sf = k.rh(context).sl() * 1000;
                break;
            default:
                sf = k.rh(context).sm() * 1000;
                break;
        }
        if (((long) this.hy) == k.rh(context).sn()) {
            f.yc(context).ye();
            c.ep("PushLog3413", "reconnect pushserver failed " + this.hy + " times, set force query TRS at next connect.");
        }
        long currentTimeMillis = System.currentTimeMillis();
        long aas = ((b) this.ht.get(this.ht.size() - 1)).im;
        if (currentTimeMillis < aas) {
            c.er("PushLog3413", "now is less than last connect time");
            aas = 0;
        } else {
            aas = Math.max((aas + sf) - currentTimeMillis, 0);
        }
        c.ep("PushLog3413", "reconnect pushserver failed, the next reconnect time is:" + this.hy + " after " + aas + " ms");
        return aas;
    }

    private long zw(Context context) {
        if (aac()) {
            aaj(context, true);
        }
        boolean aab = aab(context);
        c.er("PushLog3413", "bad network mode is " + aab);
        if (!aab || this.ht.isEmpty()) {
            return 0;
        }
        long currentTimeMillis = System.currentTimeMillis();
        long aas = ((b) this.ht.get(this.ht.size() - 1)).im;
        long j = aad() ? ic : ib;
        if (currentTimeMillis < aas) {
            c.er("PushLog3413", "now is less than last connect time");
            j = 0;
        } else {
            j = Math.max((j + aas) - currentTimeMillis, 0);
        }
        c.er("PushLog3413", "It is in bad network mode, connect limit interval is " + j);
        return j;
    }

    private boolean aab(Context context) {
        return l.ul(context).up();
    }

    private void aaj(Context context, boolean z) {
        c.er("PushLog3413", "set bad network mode " + z);
        l.ul(context).uq(z);
    }

    private boolean aad() {
        try {
            String format = new SimpleDateFormat("HH:mm").format(new Date());
            if (format.compareTo(hz) > 0 && format.compareTo(ia) < 0) {
                c.er("PushLog3413", "It is in Idle period.");
                return true;
            }
        } catch (RuntimeException e) {
            c.er("PushLog3413", "format idle perild time RuntimeException.");
        } catch (Exception e2) {
            c.er("PushLog3413", "format idle perild time exception.");
        }
        return false;
    }

    private boolean aac() {
        long currentTimeMillis = System.currentTimeMillis();
        long j = aad() ? hw : hv;
        int i = 0;
        for (b bVar : this.ht) {
            if (currentTimeMillis > bVar.aan() && currentTimeMillis - bVar.aan() < j) {
                i++;
            }
            i = i;
        }
        c.ep("PushLog3413", "The connect range limit is: " + hx + " times in " + j + ", " + "current count is:" + i);
        if (i < hx) {
            return false;
        }
        return true;
    }

    private void zv() {
        this.hy = 0;
    }

    private void aaa() {
        this.hy++;
    }

    public void aag(Context context, ReconnectMgr$RECONNECTEVENT reconnectMgr$RECONNECTEVENT, Bundle bundle) {
        c.er("PushLog3413", "receive reconnectevent:" + reconnectMgr$RECONNECTEVENT);
        switch (aal()[reconnectMgr$RECONNECTEVENT.ordinal()]) {
            case 1:
                zv();
                return;
            case 2:
                ErrorType errorType = ErrorType.Err_unKnown;
                aak(context);
                if (bundle.containsKey("errorType")) {
                    if (ErrorType.Err_Connect == ((ErrorType) bundle.getSerializable("errorType"))) {
                        aai(context, false);
                        com.huawei.android.pushagent.a.a.hx(54);
                    } else {
                        c.er("PushLog3413", "socket close not caused by connect error, do not need save connection info");
                    }
                } else {
                    c.er("PushLog3413", "socket close not caused by pushException");
                }
                aaa();
                PushService.abr(new Intent("com.huawei.action.CONNECT_PUSHSRV"));
                return;
            case 3:
                aai(context, true);
                return;
            case 4:
                zv();
                return;
            case 5:
                zv();
                return;
            default:
                return;
        }
    }

    private void aaf(Context context) {
        hx = k.rh(context).so();
        hv = k.rh(context).sp();
        hw = k.rh(context).sq();
        hu = k.rh(context).sr();
        ib = k.rh(context).ss();
        ic = k.rh(context).st();
        hz = k.rh(context).su();
        ia = k.rh(context).sv();
    }

    public void aah(Context context) {
        c.ep("PushLog3413", "enter resetReconnectConfig");
        aaf(context);
        this.ht.clear();
        aaj(context, false);
        com.huawei.android.pushagent.model.prefs.a.of(context).om();
    }
}
