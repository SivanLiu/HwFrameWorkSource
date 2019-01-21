package com.huawei.android.pushagent.model.c;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.datatype.a.a;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.utils.d;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class b {
    private static final byte[] fk = new byte[0];
    private static final b fm = new b();
    private List<a> fl = new ArrayList();
    private String[] fn;

    private b() {
    }

    public static b pa() {
        return fm;
    }

    public void pb(Context context) {
        if (com.huawei.android.pushagent.utils.tools.b.sf()) {
            ph(i.lg(context).lh());
        }
    }

    private void ph(String str) {
        synchronized (fk) {
            if (TextUtils.isEmpty(str)) {
                this.fn = new String[0];
            } else {
                this.fn = str.split("\t");
            }
        }
    }

    public boolean pg(Context context, String str, int i) {
        int li = i.lg(context).li();
        boolean zl = d.zl();
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "receive a push msg,ctrl state:" + li + ", isDawn:" + zl);
        switch (li) {
            case 0:
                return false;
            case 1:
                if (!zl) {
                    return (i != 0 || pi(context, str) || pj(str)) ? false : true;
                } else {
                    if (pi(context, str)) {
                        return false;
                    }
                    pm(context, 1, pf());
                    com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "receive a wrong push msg in dawn period ctrl state, cache this message, and correct the state of push server.");
                    com.huawei.android.pushagent.b.a.abd(105);
                    return true;
                }
            default:
                i.lg(context).lj(0);
                com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "local ctrl state is offnet, but receive push message, modify the ctrl state to allow, and display.");
                return false;
        }
    }

    private boolean pi(Context context, String str) {
        String trim = com.huawei.android.pushagent.model.prefs.a.ff(context).ge().trim();
        if (TextUtils.isEmpty(trim)) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "dawn white list is empty.");
            return false;
        } else if (!Arrays.asList(trim.split("#")).contains(str)) {
            return false;
        } else {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", str + " is in dawn white list.");
            return true;
        }
    }

    private boolean pj(String str) {
        synchronized (fk) {
            if (this.fn == null || this.fn.length <= 0) {
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "power genie white list is empty");
                return true;
            } else if (Arrays.asList(this.fn).contains(str)) {
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", str + " is in power genie white list.");
                return true;
            } else {
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", str + " is not in power genie white list.");
                return false;
            }
        }
    }

    public void pd(Context context, Intent intent) {
        int i = 1;
        try {
            boolean booleanExtra = intent.getBooleanExtra("ctrl_socket_status", false);
            String stringExtra = intent.getStringExtra("ctrl_socket_list");
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "power genie limit :" + booleanExtra + ", white list:" + stringExtra);
            if (d.zl()) {
                if (booleanExtra) {
                    pm(context, 1, pf());
                    com.huawei.android.pushagent.b.a.abd(101);
                } else {
                    com.huawei.android.pushagent.b.a.abd(100);
                    i = 2;
                }
            } else if (!booleanExtra) {
                com.huawei.android.pushagent.b.a.abd(103);
                i = 2;
            }
            ph(stringExtra);
            i.lg(context).lk(stringExtra);
            i.lg(context).lj(i);
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "push network state:" + i + "[0:not ctrl, 1:ctrl, 2:network off]");
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "parse message from power genie exception.");
        }
    }

    private void pm(Context context, int i, long j) {
        PushService.abv(new Intent("com.huawei.action.push.intent.UPDATE_CHANNEL_STATE").setPackage(context.getPackageName()).putExtra("networkState", i).putExtra("duration", pf()));
    }

    public void pc(Context context) {
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "power genie allow all packages to use push, handle all the cached messages");
        i.lg(context).lj(0);
        com.huawei.android.pushagent.b.a.abd(102);
        pm(context, 0, 0);
        synchronized (fk) {
            this.fn = new String[0];
        }
        for (a aVar : this.fl) {
            if (!(aVar.getToken() == null || aVar.ai() == null)) {
                if (aVar.ak() == 0) {
                    pk(context, aVar.am(), aVar.getToken(), aVar.ai(), aVar.al(), aVar.aj());
                } else {
                    pl(context, aVar.am(), aVar.getToken(), aVar.ai(), aVar.al());
                }
            }
        }
        this.fl.clear();
    }

    public void pn(Context context, int i, byte[] bArr, String str, byte[] bArr2, String str2, int i2) {
        if (1000 <= this.fl.size()) {
            this.fl.remove(0);
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "Push is ctrl by powerGenie, cache this message after screen on.");
        this.fl.add(new a(i, str, bArr2, bArr, i2, str2));
    }

    private void pk(Context context, String str, byte[] bArr, byte[] bArr2, int i, String str2) {
        if (com.huawei.android.pushagent.utils.tools.b.sf()) {
            com.huawei.android.pushagent.utils.tools.b.sg(2, 180);
        } else {
            d.ym(2, 180);
        }
        Intent intent = new Intent("com.huawei.android.push.intent.RECEIVE");
        intent.setPackage(str).putExtra("msg_data", bArr2).putExtra("device_token", bArr).putExtra("msgIdStr", com.huawei.android.pushagent.utils.e.a.vu(str2)).setFlags(32);
        c.po().pr(str, str2);
        d.zq(context, intent, i);
        com.huawei.android.pushagent.utils.tools.a.sa(context, new Intent("com.huawei.android.push.intent.MSG_RSP_TIMEOUT").setPackage(context.getPackageName()), com.huawei.android.pushagent.model.prefs.a.ff(context).gu());
    }

    private void pl(Context context, String str, byte[] bArr, byte[] bArr2, int i) {
        Intent intent = new Intent("com.huawei.intent.action.PUSH");
        intent.putExtra("selfshow_info", bArr2);
        intent.putExtra("selfshow_token", bArr);
        intent.setFlags(32);
        if (d.yy(context, "com.huawei.android.pushagent", i)) {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "send selfshow msg to NC to display.");
            intent.setPackage("com.huawei.android.pushagent");
            try {
                intent.putExtra("extra_encrypt_data", com.huawei.android.pushagent.utils.e.a.vv("com.huawei.android.pushagent", d.zd().getBytes("UTF-8")));
            } catch (UnsupportedEncodingException e) {
                com.huawei.android.pushagent.utils.b.a.su("PushLog3414", e.toString());
            }
        } else {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "NC is disable, send selfshow msg to [" + str + "] to display.");
            intent.setPackage(str);
        }
        d.zq(context, intent, i);
    }

    public static long pf() {
        Calendar instance = Calendar.getInstance();
        instance.setTime(new Date());
        int i = instance.get(11);
        int i2 = instance.get(12);
        if (i >= 6) {
            return 0;
        }
        return ((((long) (30 - i2)) * 60000) + (((long) (6 - i)) * 3600000)) / 1000;
    }

    public static int pe(Context context) {
        if (i.lg(context).li() == 0 || (d.zl() ^ 1) != 0) {
            return 0;
        }
        return 1;
    }
}
