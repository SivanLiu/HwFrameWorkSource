package com.huawei.android.pushagent.b.a;

import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.model.channel.a;
import com.huawei.android.pushagent.model.channel.entity.a.c;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.utils.f;
import java.util.ArrayList;
import java.util.List;

public class b extends c {
    private List<String> ic = new ArrayList();
    private long id;
    private boolean ie = false;
    private long if = 0;
    private int ig;
    private int ih;
    private long ii;
    private int ij;
    private String ik = null;

    protected void aaf(String str, String str2) {
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "reportEvent eventId:" + str + "; extra:" + str2);
        if (this.ic.size() >= this.ih) {
            com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", "events overflow, abandon");
        } else {
            if (str2 == null) {
                str2 = "";
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(aab()).append("|").append(f.fp(this.appCtx)).append("|").append(str).append("|").append(str2);
            this.ic.add(stringBuilder.toString());
        }
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "reportEvent eventList.size:" + this.ic.size());
        if (this.ic.size() >= this.ij && this.ik == null) {
            StringBuilder stringBuilder2 = new StringBuilder();
            int i = this.ij;
            int size = this.ic.size();
            int i2 = 0;
            int i3 = 0;
            int i4 = i;
            while (i2 < size) {
                String str3 = (String) this.ic.remove(0);
                i3 += aaa(str3);
                if (i3 < this.ig) {
                    if (i4 != this.ij) {
                        stringBuilder2.append("#");
                    }
                    stringBuilder2.append(str3);
                    i4--;
                    if (i4 == 0) {
                        break;
                    }
                    i2++;
                } else {
                    this.ic.add(0, str3);
                    break;
                }
            }
            this.ik = stringBuilder2.toString();
        }
        if (aad()) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "reportEvent begin to send events");
            this.ie = false;
            this.if = System.currentTimeMillis();
            aah(this.ik);
            aai(this.appCtx);
        }
    }

    private int aaa(String str) {
        int length = "#".length();
        if (str == null) {
            return length;
        }
        return length + str.length();
    }

    private boolean aad() {
        long currentTimeMillis = System.currentTimeMillis();
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "reportEvent now:" + currentTimeMillis + "; lastReportTime:" + this.if + "; AvailableNetwork:" + f.fp(this.appCtx) + "; hasConnection:" + a.wr().vc() + "; lastReportSuccess:" + this.ie);
        if (currentTimeMillis <= (this.ie ? this.ii : this.id) + this.if || 1 != f.fp(this.appCtx) || this.ik == null) {
            return false;
        }
        com.huawei.android.pushagent.model.channel.entity.a wr = a.wr();
        if (!(wr instanceof c)) {
            return false;
        }
        c cVar = (c) wr;
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "reportEvent hasRegist:" + cVar.ut());
        return cVar.vc() ? cVar.ut() : false;
    }

    private void aah(String str) {
        PushService.aax(new Intent("com.huawei.action.push.intent.REPORT_EVENTS").setPackage(this.appCtx.getPackageName()).putExtra("events", str));
    }

    protected void aae(boolean z) {
        if (z) {
            this.ie = true;
            this.ik = null;
        }
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "reportEvent report result is:" + z);
    }

    private String aab() {
        return String.valueOf(System.currentTimeMillis());
    }

    protected void aac(Context context) {
        aai(context);
    }

    protected void aag(long j, String str, String str2, String str3) {
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "reportEvent eventId:" + str2 + "; extra:" + str3);
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "reportEvent eventList:" + this.ic.toString());
        if (this.ic.size() >= this.ih) {
            com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", "reportEvent events overflow, abandon");
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(j).append("|").append(str).append("|").append(str2).append("|").append(str3);
            this.ic.add(stringBuilder.toString());
        }
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "reportEvent eventList.size:" + this.ic.size());
    }

    private void aai(Context context) {
        this.ii = i.mj(context).op();
        this.id = i.mj(context).oq();
        this.ih = i.mj(context).or();
        this.ij = i.mj(context).os();
        this.ig = i.mj(context).ot();
    }
}
