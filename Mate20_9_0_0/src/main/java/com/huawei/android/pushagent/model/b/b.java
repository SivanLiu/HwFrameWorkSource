package com.huawei.android.pushagent.model.b;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.a.a;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.g;
import com.huawei.android.pushagent.utils.tools.c;
import com.huawei.android.pushagent.utils.tools.d;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class b {
    private static final byte[] gs = new byte[0];
    private static final b gu = new b();
    private List<com.huawei.android.pushagent.datatype.a.b> gt = new ArrayList();
    private String[] gv;

    private b() {
    }

    public static b xd() {
        return gu;
    }

    public void xe(Context context) {
        if (c.cr()) {
            xk(com.huawei.android.pushagent.model.prefs.c.pc(context).pd());
        }
    }

    private void xk(String str) {
        synchronized (gs) {
            if (TextUtils.isEmpty(str)) {
                this.gv = new String[0];
            } else {
                this.gv = str.split("\t");
            }
        }
    }

    public boolean xj(Context context, String str, int i) {
        int pb = com.huawei.android.pushagent.model.prefs.c.pc(context).pb();
        boolean gl = g.gl();
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "receive a push msg,ctrl state:" + pb + ", isDawn:" + gl);
        switch (pb) {
            case 0:
                return false;
            case 1:
                if (!gl) {
                    return (i != 0 || xl(context, str) || xm(str)) ? false : true;
                } else {
                    if (xl(context, str)) {
                        return false;
                    }
                    xp(context, 1, xi());
                    com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "receive a wrong push msg in dawn period ctrl state, cache this message, and correct the state of push server.");
                    a.hx(105);
                    return true;
                }
            default:
                com.huawei.android.pushagent.model.prefs.c.pc(context).pf(0);
                com.huawei.android.pushagent.utils.f.c.eo("PushLog3413", "local ctrl state is offnet, but receive push message, modify the ctrl state to allow, and display.");
                return false;
        }
    }

    private boolean xl(Context context, String str) {
        Object trim = k.rh(context).sd().trim();
        if (TextUtils.isEmpty(trim)) {
            com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "dawn white list is empty.");
            return false;
        } else if (!Arrays.asList(trim.split("#")).contains(str)) {
            return false;
        } else {
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", str + " is in dawn white list.");
            return true;
        }
    }

    private boolean xm(String str) {
        synchronized (gs) {
            if (this.gv == null || this.gv.length <= 0) {
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "power genie white list is empty");
                return true;
            } else if (Arrays.asList(this.gv).contains(str)) {
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", str + " is in power genie white list.");
                return true;
            } else {
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", str + " is not in power genie white list.");
                return false;
            }
        }
    }

    public void xg(Context context, Intent intent) {
        int i = 1;
        try {
            boolean booleanExtra = intent.getBooleanExtra("ctrl_socket_status", false);
            String stringExtra = intent.getStringExtra("ctrl_socket_list");
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "power genie limit :" + booleanExtra + ", white list:" + stringExtra);
            if (g.gl()) {
                if (booleanExtra) {
                    xp(context, 1, xi());
                    a.hx(101);
                } else {
                    a.hx(100);
                    i = 2;
                }
            } else if (!booleanExtra) {
                a.hx(103);
                i = 2;
            }
            xk(stringExtra);
            com.huawei.android.pushagent.model.prefs.c.pc(context).pg(stringExtra);
            com.huawei.android.pushagent.model.prefs.c.pc(context).pf(i);
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "push network state:" + i + "[0:not ctrl, 1:ctrl, 2:network off]");
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "parse message from power genie exception.");
        }
    }

    private void xp(Context context, int i, long j) {
        PushService.abr(new Intent("com.huawei.action.push.intent.UPDATE_CHANNEL_STATE").setPackage(context.getPackageName()).putExtra("networkState", i).putExtra("duration", xi()));
    }

    public void xf(Context context) {
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "power genie allow all packages to use push, handle all the cached messages");
        com.huawei.android.pushagent.model.prefs.c.pc(context).pf(0);
        a.hx(102);
        xp(context, 0, 0);
        synchronized (gs) {
            this.gv = new String[0];
        }
        for (com.huawei.android.pushagent.datatype.a.b bVar : this.gt) {
            if (!(bVar.getToken() == null || bVar.kf() == null)) {
                if (bVar.kh() == 0) {
                    xn(context, bVar.kj(), bVar.getToken(), bVar.kf(), bVar.ki(), bVar.kg());
                } else {
                    xo(context, bVar.kj(), bVar.getToken(), bVar.kf(), bVar.ki());
                }
            }
        }
        this.gt.clear();
    }

    public void xq(Context context, int i, byte[] bArr, String str, byte[] bArr2, String str2, int i2) {
        if (1000 <= this.gt.size()) {
            this.gt.remove(0);
        }
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "Push is ctrl by powerGenie, cache this message after screen on.");
        this.gt.add(new com.huawei.android.pushagent.datatype.a.b(i, str, bArr2, bArr, i2, str2));
    }

    private void xn(Context context, String str, byte[] bArr, byte[] bArr2, int i, String str2) {
        if (c.cr()) {
            c.ct(2, 180);
        } else {
            g.ge(2, 180);
        }
        Intent intent = new Intent("com.huawei.android.push.intent.RECEIVE");
        intent.setPackage(str).putExtra("msg_data", bArr2).putExtra("device_token", bArr).putExtra("msgIdStr", com.huawei.android.pushagent.utils.a.c.k(str2)).setFlags(32);
        c.xr().xu(str, str2);
        g.gm(context, intent, i);
        d.cw(context, new Intent("com.huawei.android.push.intent.MSG_RSP_TIMEOUT").setPackage(context.getPackageName()), k.rh(context).se());
    }

    private void xo(Context context, String str, byte[] bArr, byte[] bArr2, int i) {
        Intent intent = new Intent("com.huawei.intent.action.PUSH");
        intent.putExtra("selfshow_info", bArr2);
        intent.putExtra("selfshow_token", bArr);
        intent.setFlags(32);
        if (g.gn(context, "com.huawei.android.pushagent", i)) {
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "send selfshow msg to NC to display.");
            intent.setPackage("com.huawei.android.pushagent");
            try {
                intent.putExtra("extra_encrypt_data", com.huawei.android.pushagent.utils.a.c.o("com.huawei.android.pushagent", g.go().getBytes("UTF-8")));
            } catch (UnsupportedEncodingException e) {
                com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", e.toString());
            }
        } else {
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "NC is disable, send selfshow msg to [" + str + "] to display.");
            intent.setPackage(str);
        }
        g.gm(context, intent, i);
    }

    public static long xi() {
        Calendar instance = Calendar.getInstance();
        instance.setTime(new Date());
        int i = instance.get(11);
        int i2 = instance.get(12);
        if (i >= 6) {
            return 0;
        }
        return ((((long) (30 - i2)) * 60000) + (((long) (6 - i)) * 3600000)) / 1000;
    }

    public static int xh(Context context) {
        if (com.huawei.android.pushagent.model.prefs.c.pc(context).pb() == 0 || (g.gl() ^ 1) != 0) {
            return 0;
        }
        return 1;
    }
}
