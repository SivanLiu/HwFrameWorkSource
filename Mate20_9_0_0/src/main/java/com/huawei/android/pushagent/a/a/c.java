package com.huawei.android.pushagent.a.a;

import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.model.channel.a;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.g;
import java.util.ArrayList;
import java.util.List;

public class c extends a {
    private List<String> be = new ArrayList();
    private long bf;
    private boolean bg = false;
    private long bh = 0;
    private int bi;
    private int bj;
    private long bk;
    private int bl;
    private String bm = null;

    protected void hj(String str, String str2) {
        com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "reportEvent eventId:" + str + "; extra:" + str2);
        if (this.be.size() >= this.bj) {
            com.huawei.android.pushagent.utils.f.c.eo("PushLog3413", "events overflow, abandon");
        } else {
            if (str2 == null) {
                str2 = "";
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(hm()).append("|").append(g.fw(this.appCtx)).append("|").append(str).append("|").append(str2);
            this.be.add(stringBuilder.toString());
        }
        com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "reportEvent eventList.size:" + this.be.size());
        if (this.be.size() >= this.bl && this.bm == null) {
            StringBuilder stringBuilder2 = new StringBuilder();
            int i = this.bl;
            int size = this.be.size();
            int i2 = 0;
            int i3 = 0;
            int i4 = i;
            while (i2 < size) {
                String str3 = (String) this.be.remove(0);
                i3 += hl(str3);
                if (i3 < this.bi) {
                    if (i4 != this.bl) {
                        stringBuilder2.append("#");
                    }
                    stringBuilder2.append(str3);
                    i4--;
                    if (i4 == 0) {
                        break;
                    }
                    i2++;
                } else {
                    this.be.add(0, str3);
                    break;
                }
            }
            this.bm = stringBuilder2.toString();
        }
        if (hn()) {
            com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "reportEvent begin to send events");
            this.bg = false;
            this.bh = System.currentTimeMillis();
            ho(this.bm);
            hp(this.appCtx);
        }
    }

    private int hl(String str) {
        int length = "#".length();
        if (str == null) {
            return length;
        }
        return length + str.length();
    }

    private boolean hn() {
        long currentTimeMillis = System.currentTimeMillis();
        com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "reportEvent now:" + currentTimeMillis + "; lastReportTime:" + this.bh + "; AvailableNetwork:" + g.fw(this.appCtx) + "; hasConnection:" + a.ns().mk() + "; lastReportSuccess:" + this.bg);
        if (currentTimeMillis <= (this.bg ? this.bk : this.bf) + this.bh || 1 != g.fw(this.appCtx) || this.bm == null) {
            return false;
        }
        com.huawei.android.pushagent.model.channel.entity.a ns = a.ns();
        if (!(ns instanceof com.huawei.android.pushagent.model.channel.entity.a.c)) {
            return false;
        }
        com.huawei.android.pushagent.model.channel.entity.a.c cVar = (com.huawei.android.pushagent.model.channel.entity.a.c) ns;
        com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "reportEvent hasRegist:" + cVar.ls());
        return cVar.mk() ? cVar.ls() : false;
    }

    private void ho(String str) {
        PushService.abr(new Intent("com.huawei.action.push.intent.REPORT_EVENTS").setPackage(this.appCtx.getPackageName()).putExtra("events", str));
    }

    protected void hi(boolean z) {
        if (z) {
            this.bg = true;
            this.bm = null;
        }
        com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "reportEvent report result is:" + z);
    }

    private String hm() {
        return String.valueOf(System.currentTimeMillis());
    }

    protected void hh(Context context) {
        hp(context);
    }

    protected void hk(long j, String str, String str2, String str3) {
        com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "reportEvent eventId:" + str2 + "; extra:" + str3);
        com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "reportEvent eventList:" + this.be.toString());
        if (this.be.size() >= this.bj) {
            com.huawei.android.pushagent.utils.f.c.eo("PushLog3413", "reportEvent events overflow, abandon");
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(j).append("|").append(str).append("|").append(str2).append("|").append(str3);
            this.be.add(stringBuilder.toString());
        }
        com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "reportEvent eventList.size:" + this.be.size());
    }

    private void hp(Context context) {
        this.bk = k.rh(context).tx();
        this.bf = k.rh(context).tu();
        this.bj = k.rh(context).ub();
        this.bl = k.rh(context).uc();
        this.bi = k.rh(context).ua();
    }
}
