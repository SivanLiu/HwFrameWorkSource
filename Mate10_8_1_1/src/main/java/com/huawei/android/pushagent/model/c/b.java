package com.huawei.android.pushagent.model.c;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.model.prefs.m;
import com.huawei.android.pushagent.utils.c.c;
import com.huawei.android.pushagent.utils.f;
import com.huawei.android.pushagent.utils.tools.a;
import com.huawei.android.pushagent.utils.tools.d;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class b {
    private static final byte[] ee = new byte[0];
    private static final b eg = new b();
    private List<com.huawei.android.pushagent.datatype.b.b> ef = new ArrayList();
    private String[] eh;

    private b() {
    }

    public static b sc() {
        return eg;
    }

    public void sg(Context context) {
        if (a.j()) {
            sf(m.qx(context).qy());
        }
    }

    private void sf(String str) {
        synchronized (ee) {
            if (TextUtils.isEmpty(str)) {
                this.eh = new String[0];
            } else {
                this.eh = str.split("\t");
            }
        }
    }

    public boolean sb(Context context, String str, int i) {
        int qz = m.qx(context).qz();
        boolean fu = f.fu();
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "receive a push msg,ctrl state:" + qz + ", isDawn:" + fu);
        switch (qz) {
            case 0:
                return false;
            case 1:
                if (!fu) {
                    return (i != 0 || sh(context, str) || si(str)) ? false : true;
                } else {
                    if (sh(context, str)) {
                        return false;
                    }
                    sn(context, 1, sd());
                    com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "receive a wrong push msg in dawn period ctrl state, cache this message, and correct the state of push server.");
                    com.huawei.android.pushagent.b.a.aak(105);
                    return true;
                }
            default:
                m.qx(context).ra(0);
                com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", "local ctrl state is offnet, but receive push message, modify the ctrl state to allow, and display.");
                return false;
        }
    }

    private boolean sh(Context context, String str) {
        Object trim = i.mj(context).mt().trim();
        if (TextUtils.isEmpty(trim)) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "dawn white list is empty.");
            return false;
        } else if (!Arrays.asList(trim.split("#")).contains(str)) {
            return false;
        } else {
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", str + " is in dawn white list.");
            return true;
        }
    }

    private boolean si(String str) {
        synchronized (ee) {
            if (this.eh == null || this.eh.length <= 0) {
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "power genie white list is empty");
                return true;
            } else if (Arrays.asList(this.eh).contains(str)) {
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", str + " is in power genie white list.");
                return true;
            } else {
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", str + " is not in power genie white list.");
                return false;
            }
        }
    }

    public void sj(Context context, Intent intent) {
        int i = 1;
        try {
            boolean booleanExtra = intent.getBooleanExtra("ctrl_socket_status", false);
            String stringExtra = intent.getStringExtra("ctrl_socket_list");
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "power genie limit :" + booleanExtra + ", white list:" + stringExtra);
            if (f.fu()) {
                if (booleanExtra) {
                    sn(context, 1, sd());
                    com.huawei.android.pushagent.b.a.aak(101);
                } else {
                    com.huawei.android.pushagent.b.a.aak(100);
                    i = 2;
                }
            } else if (!booleanExtra) {
                com.huawei.android.pushagent.b.a.aak(103);
                i = 2;
            }
            sf(stringExtra);
            m.qx(context).rb(stringExtra);
            m.qx(context).ra(i);
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "push network state:" + i + "[0:not ctrl, 1:ctrl, 2:network off]");
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "parse message from power genie exception.");
        }
    }

    private void sn(Context context, int i, long j) {
        PushService.aax(new Intent("com.huawei.action.push.intent.UPDATE_CHANNEL_STATE").setPackage(context.getPackageName()).putExtra("networkState", i).putExtra("duration", sd()));
    }

    public void sk(Context context) {
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "power genie allow all packages to use push, handle all the cached messages");
        m.qx(context).ra(0);
        com.huawei.android.pushagent.b.a.aak(102);
        sn(context, 0, 0);
        synchronized (ee) {
            this.eh = new String[0];
        }
        for (com.huawei.android.pushagent.datatype.b.b bVar : this.ef) {
            if (!(bVar.getToken() == null || bVar.xz() == null)) {
                if (bVar.ya() == 0) {
                    sl(context, bVar.yb(), bVar.getToken(), bVar.xz(), bVar.yc(), bVar.yd());
                } else {
                    sm(context, bVar.yb(), bVar.getToken(), bVar.xz(), bVar.yc());
                }
            }
        }
        this.ef.clear();
    }

    public void so(Context context, int i, byte[] bArr, String str, byte[] bArr2, String str2, int i2) {
        if (1000 <= this.ef.size()) {
            this.ef.remove(0);
        }
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "Push is ctrl by powerGenie, cache this message after screen on.");
        this.ef.add(new com.huawei.android.pushagent.datatype.b.b(i, str, bArr2, bArr, i2, str2));
    }

    private void sl(Context context, String str, byte[] bArr, byte[] bArr2, int i, String str2) {
        if (a.j()) {
            a.g(2, 180);
        } else {
            f.fv(2, 180);
        }
        Intent intent = new Intent("com.huawei.android.push.intent.RECEIVE");
        intent.setPackage(str).putExtra("msg_data", bArr2).putExtra("device_token", bArr).putExtra("msgIdStr", c.bc(str2)).setFlags(32);
        a.ry().rx(str, str2);
        f.fw(context, intent, i);
        d.p(context, new Intent("com.huawei.android.push.intent.MSG_RSP_TIMEOUT").setPackage(context.getPackageName()), i.mj(context).ms());
    }

    private void sm(Context context, String str, byte[] bArr, byte[] bArr2, int i) {
        Intent intent = new Intent("com.huawei.intent.action.PUSH");
        intent.putExtra("selfshow_info", bArr2);
        intent.putExtra("selfshow_token", bArr);
        intent.setFlags(32);
        if (f.fx(context, "com.huawei.android.pushagent", i)) {
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "send selfshow msg to NC to display.");
            intent.setPackage("com.huawei.android.pushagent");
            try {
                intent.putExtra("extra_encrypt_data", c.bd("com.huawei.android.pushagent", f.fy().getBytes("UTF-8")));
            } catch (UnsupportedEncodingException e) {
                com.huawei.android.pushagent.utils.a.b.y("PushLog2976", e.toString());
            }
        } else {
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "NC is disable, send selfshow msg to [" + str + "] to display.");
            intent.setPackage(str);
        }
        f.fw(context, intent, i);
    }

    public static long sd() {
        Calendar instance = Calendar.getInstance();
        instance.setTime(new Date());
        int i = instance.get(11);
        int i2 = instance.get(12);
        if (i >= 6) {
            return 0;
        }
        return ((((long) (30 - i2)) * 60000) + (((long) (6 - i)) * 3600000)) / 1000;
    }

    public static int se(Context context) {
        if (m.qx(context).qz() == 0 || (f.fu() ^ 1) != 0) {
            return 0;
        }
        return 1;
    }
}
