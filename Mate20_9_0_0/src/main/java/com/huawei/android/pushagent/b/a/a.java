package com.huawei.android.pushagent.b.a;

import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.model.channel.entity.b;
import com.huawei.android.pushagent.utils.d;
import java.util.ArrayList;
import java.util.List;

public class a extends b {
    private List<String> ih = new ArrayList();
    private long ii;
    private boolean ij = false;
    private long ik = 0;
    private int il;
    private int im;
    private long in;
    private int io;
    private String ip = null;

    protected void aat(String str, String str2) {
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "reportEvent eventId:" + str + "; extra:" + str2);
        if (this.ih.size() >= this.im) {
            com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "events overflow, abandon");
        } else {
            if (str2 == null) {
                str2 = "";
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(aap()).append("|").append(d.yh(this.appCtx)).append("|").append(str).append("|").append(str2);
            this.ih.add(stringBuilder.toString());
        }
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "reportEvent eventList.size:" + this.ih.size());
        if (this.ih.size() >= this.io && this.ip == null) {
            StringBuilder stringBuilder2 = new StringBuilder();
            int i = this.io;
            int size = this.ih.size();
            int i2 = 0;
            int i3 = 0;
            int i4 = i;
            while (i2 < size) {
                String str3 = (String) this.ih.remove(0);
                i3 += aao(str3);
                if (i3 < this.il) {
                    if (i4 != this.io) {
                        stringBuilder2.append("#");
                    }
                    stringBuilder2.append(str3);
                    i4--;
                    if (i4 == 0) {
                        break;
                    }
                    i2++;
                } else {
                    this.ih.add(0, str3);
                    break;
                }
            }
            this.ip = stringBuilder2.toString();
        }
        if (aar()) {
            com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "reportEvent begin to send events");
            this.ij = false;
            this.ik = System.currentTimeMillis();
            aav(this.ip);
            aaw(this.appCtx);
        }
    }

    private int aao(String str) {
        int length = "#".length();
        if (str == null) {
            return length;
        }
        return length + str.length();
    }

    private boolean aar() {
        long currentTimeMillis = System.currentTimeMillis();
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "reportEvent now:" + currentTimeMillis + "; lastReportTime:" + this.ik + "; AvailableNetwork:" + d.yh(this.appCtx) + "; hasConnection:" + com.huawei.android.pushagent.model.channel.a.dz().cp() + "; lastReportSuccess:" + this.ij);
        if (currentTimeMillis <= (this.ij ? this.in : this.ii) + this.ik || 1 != d.yh(this.appCtx) || this.ip == null) {
            return false;
        }
        b dz = com.huawei.android.pushagent.model.channel.a.dz();
        if (!(dz instanceof com.huawei.android.pushagent.model.channel.entity.a.b)) {
            return false;
        }
        com.huawei.android.pushagent.model.channel.entity.a.b bVar = (com.huawei.android.pushagent.model.channel.entity.a.b) dz;
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "reportEvent hasRegist:" + bVar.by());
        return bVar.cp() ? bVar.by() : false;
    }

    private void aav(String str) {
        PushService.abv(new Intent("com.huawei.action.push.intent.REPORT_EVENTS").setPackage(this.appCtx.getPackageName()).putExtra("events", str));
    }

    protected void aas(boolean z) {
        if (z) {
            this.ij = true;
            this.ip = null;
        }
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "reportEvent report result is:" + z);
    }

    private String aap() {
        return String.valueOf(System.currentTimeMillis());
    }

    protected void aaq(Context context) {
        aaw(context);
    }

    protected void aau(long j, String str, String str2, String str3) {
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "reportEvent eventId:" + str2 + "; extra:" + str3);
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "reportEvent eventList:" + this.ih.toString());
        if (this.ih.size() >= this.im) {
            com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "reportEvent events overflow, abandon");
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(j).append("|").append(str).append("|").append(str2).append("|").append(str3);
            this.ih.add(stringBuilder.toString());
        }
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "reportEvent eventList.size:" + this.ih.size());
    }

    private void aaw(Context context) {
        this.in = com.huawei.android.pushagent.model.prefs.a.ff(context).gs();
        this.ii = com.huawei.android.pushagent.model.prefs.a.ff(context).gf();
        this.im = com.huawei.android.pushagent.model.prefs.a.ff(context).hg();
        this.io = com.huawei.android.pushagent.model.prefs.a.ff(context).hh();
        this.il = com.huawei.android.pushagent.model.prefs.a.ff(context).hf();
    }
}
