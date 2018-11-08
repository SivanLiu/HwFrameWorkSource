package com.huawei.android.pushagent.model.flowcontrol;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.b.a;
import com.huawei.android.pushagent.datatype.exception.PushException.ErrorType;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.a.c;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

public class b {
    private static long br = 300000;
    private static long bs = 600000;
    private static long bt = 1800000;
    private static int bu = 4;
    private static String bw = "00:00";
    private static String bx = "06:00";
    private static long by = 300000;
    private static long bz = 600000;
    private static b ca = null;
    private static final /* synthetic */ int[] cb = null;
    private ArrayList<c> bq = new ArrayList();
    private int bv = 0;

    private static /* synthetic */ int[] ip() {
        if (cb != null) {
            return cb;
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
        cb = iArr;
        return iArr;
    }

    private b() {
    }

    public static synchronized b ib(Context context) {
        b bVar;
        synchronized (b.class) {
            if (ca == null) {
                ca = new b();
            }
            if (ca.bq.isEmpty()) {
                ca.ii(context);
            }
            bVar = ca;
        }
        return bVar;
    }

    private void ii(Context context) {
        int i;
        int size;
        ij(context);
        String aj = new c(context, "PushConnectControl").aj("connectPushSvrInfos");
        if (!TextUtils.isEmpty(aj)) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "connectPushSvrInfos is " + aj);
            for (String str : aj.split("\\|")) {
                c cVar = new c();
                if (cVar.it(str)) {
                    this.bq.add(cVar);
                }
            }
        }
        Collections.sort(this.bq);
        if (this.bq.size() > bu) {
            Collection arrayList = new ArrayList();
            size = this.bq.size() - bu;
            for (i = 0; i < size; i++) {
                arrayList.add((c) this.bq.get(i));
            }
            this.bq.removeAll(arrayList);
        }
    }

    private void im(Context context, boolean z) {
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "save connection info " + z);
        long currentTimeMillis = System.currentTimeMillis();
        long j = ih() ? bt : bs;
        Collection arrayList = new ArrayList();
        for (c cVar : this.bq) {
            if (currentTimeMillis < cVar.ir() || currentTimeMillis - cVar.ir() > j) {
                arrayList.add(cVar);
            }
        }
        if (!arrayList.isEmpty()) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "some connection info is expired:" + arrayList.size());
            this.bq.removeAll(arrayList);
        }
        c cVar2 = new c();
        cVar2.iu(z);
        cVar2.iv(System.currentTimeMillis());
        if (this.bq.size() < bu) {
            this.bq.add(cVar2);
        } else {
            this.bq.remove(0);
            this.bq.add(cVar2);
        }
        String str = "|";
        StringBuffer stringBuffer = new StringBuffer();
        for (c cVar22 : this.bq) {
            stringBuffer.append(cVar22.toString());
            stringBuffer.append(str);
        }
        stringBuffer.deleteCharAt(stringBuffer.length() - 1);
        new c(context, "PushConnectControl").ak("connectPushSvrInfos", stringBuffer.toString());
    }

    private void io(Context context) {
        if (!if(context)) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "It is not bad network mode, do nothing");
        } else if (this.bq.isEmpty()) {
            in(context, false);
        } else {
            c cVar = (c) this.bq.get(this.bq.size() - 1);
            if (cVar.is()) {
                com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "last connection is success");
                long currentTimeMillis = System.currentTimeMillis();
                long ir = cVar.ir();
                if (currentTimeMillis - ir > br || currentTimeMillis < ir) {
                    com.huawei.android.pushagent.utils.a.b.x("PushLog2976", br + " has passed since last connect");
                    in(context, false);
                } else {
                    com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "connection keep too short , still in bad network mode");
                }
            } else {
                com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "last connection result is false , still in bad network mode");
            }
        }
    }

    public long id(Context context) {
        long ic = ic(context);
        long ia = ia(context);
        if (ic > 0 && ic >= ia) {
            a.aaj(44, String.valueOf(ic));
        } else if (ia > 0 && ic < ia) {
            a.aaj(45, String.valueOf(ia));
        }
        return Math.max(ic, ia);
    }

    private long ic(Context context) {
        if (this.bq.isEmpty()) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "first connection, return 0");
            return 0;
        }
        long nc;
        switch (this.bv) {
            case 0:
                nc = i.mj(context).nc() * 1000;
                break;
            case 1:
                nc = i.mj(context).nd() * 1000;
                break;
            case 2:
                nc = i.mj(context).ne() * 1000;
                break;
            case 3:
                nc = i.mj(context).nf() * 1000;
                break;
            case 4:
                nc = i.mj(context).ng() * 1000;
                break;
            case 5:
                nc = i.mj(context).nh() * 1000;
                break;
            case 6:
                nc = i.mj(context).ni() * 1000;
                break;
            default:
                nc = i.mj(context).nj() * 1000;
                break;
        }
        if (((long) this.bv) == i.mj(context).nk()) {
            com.huawei.android.pushagent.model.c.c.sp(context).sr();
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "reconnect pushserver failed " + this.bv + " times, set force query TRS at next connect.");
        }
        long currentTimeMillis = System.currentTimeMillis();
        long iw = ((c) this.bq.get(this.bq.size() - 1)).cj;
        if (currentTimeMillis < iw) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "now is less than last connect time");
            iw = 0;
        } else {
            iw = Math.max((iw + nc) - currentTimeMillis, 0);
        }
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "reconnect pushserver failed, the next reconnect time is:" + this.bv + " after " + iw + " ms");
        return iw;
    }

    private long ia(Context context) {
        if (ig()) {
            in(context, true);
        }
        boolean z = if(context);
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "bad network mode is " + z);
        if (!z || this.bq.isEmpty()) {
            return 0;
        }
        long currentTimeMillis = System.currentTimeMillis();
        long iw = ((c) this.bq.get(this.bq.size() - 1)).cj;
        long j = ih() ? bz : by;
        if (currentTimeMillis < iw) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "now is less than last connect time");
            j = 0;
        } else {
            j = Math.max((j + iw) - currentTimeMillis, 0);
        }
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "It is in bad network mode, connect limit interval is " + j);
        return j;
    }

    private boolean if(Context context) {
        return k.pt(context).pz();
    }

    private void in(Context context, boolean z) {
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "set bad network mode " + z);
        k.pt(context).qa(z);
    }

    private boolean ih() {
        try {
            String format = new SimpleDateFormat("HH:mm").format(new Date());
            if (format.compareTo(bw) > 0 && format.compareTo(bx) < 0) {
                com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "It is in Idle period.");
                return true;
            }
        } catch (RuntimeException e) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "format idle perild time RuntimeException.");
        } catch (Exception e2) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "format idle perild time exception.");
        }
        return false;
    }

    private boolean ig() {
        long currentTimeMillis = System.currentTimeMillis();
        long j = ih() ? bt : bs;
        int i = 0;
        for (c cVar : this.bq) {
            int i2;
            if (currentTimeMillis <= cVar.ir() || currentTimeMillis - cVar.ir() >= j) {
                i2 = i;
            } else {
                i2 = i + 1;
            }
            i = i2;
        }
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "The connect range limit is: " + bu + " times in " + j + ", " + "current count is:" + i);
        if (i < bu) {
            return false;
        }
        return true;
    }

    private void hz() {
        this.bv = 0;
    }

    private void ie() {
        this.bv++;
    }

    public void ik(Context context, ReconnectMgr$RECONNECTEVENT reconnectMgr$RECONNECTEVENT, Bundle bundle) {
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "receive reconnectevent:" + reconnectMgr$RECONNECTEVENT);
        switch (ip()[reconnectMgr$RECONNECTEVENT.ordinal()]) {
            case 1:
                hz();
                return;
            case 2:
                ErrorType errorType = ErrorType.Err_unKnown;
                io(context);
                if (bundle.containsKey("errorType")) {
                    if (ErrorType.Err_Connect == ((ErrorType) bundle.getSerializable("errorType"))) {
                        im(context, false);
                        a.aak(54);
                    } else {
                        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "socket close not caused by connect error, do not need save connection info");
                    }
                } else {
                    com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "socket close not caused by pushException");
                }
                ie();
                PushService.aax(new Intent("com.huawei.action.CONNECT_PUSHSRV"));
                return;
            case 3:
                im(context, true);
                return;
            case 4:
                hz();
                return;
            case 5:
                hz();
                return;
            default:
                return;
        }
    }

    private void ij(Context context) {
        bu = i.mj(context).nl();
        bs = i.mj(context).nm();
        bt = i.mj(context).nn();
        br = i.mj(context).no();
        by = i.mj(context).np();
        bz = i.mj(context).nq();
        bw = i.mj(context).nr();
        bx = i.mj(context).ns();
    }

    public void il(Context context) {
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "enter resetReconnectConfig");
        ij(context);
        this.bq.clear();
        in(context, false);
        new c(context, "PushConnectControl").an("connectPushSvrInfos");
    }
}
