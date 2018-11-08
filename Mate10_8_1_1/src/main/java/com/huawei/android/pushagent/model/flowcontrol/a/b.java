package com.huawei.android.pushagent.model.flowcontrol.a;

import android.text.TextUtils;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class b implements a {
    private long be;
    private long bf;
    private long bg;
    private long bh;

    public b(long j, long j2) {
        this.bg = j;
        this.bh = j2;
        this.be = 0;
        this.bf = 0;
    }

    public boolean hd(a aVar) {
        boolean z = false;
        if (!(aVar instanceof b)) {
            return false;
        }
        b bVar = (b) aVar;
        if (this.bg == bVar.bg && this.bh == bVar.bh) {
            z = true;
        }
        return z;
    }

    public boolean he(long j) {
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "enter FlowSimpleControl::canApply(num:" + j + ", curVol:" + this.be + ", maxVol:" + this.bh + ")");
        Long valueOf = Long.valueOf(System.currentTimeMillis());
        if (valueOf.longValue() < this.bf || valueOf.longValue() - this.bf >= this.bg) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", " fistrControlTime:" + new Date(this.bf) + " interval:" + (valueOf.longValue() - this.bf) + " statInterval:" + this.bg + " change fistrControlTime to cur");
            this.bf = valueOf.longValue();
            this.be = 0;
        } else {
            try {
                Calendar instance = Calendar.getInstance(Locale.getDefault());
                instance.setTimeInMillis(this.bf);
                int i = instance.get(2);
                instance.setTimeInMillis(valueOf.longValue());
                if (i != instance.get(2)) {
                    this.bf = valueOf.longValue();
                    this.be = 0;
                }
            } catch (Throwable e) {
                com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", e.toString(), e);
            } catch (Throwable e2) {
                com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", e2.toString(), e2);
            } catch (Throwable e22) {
                com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", e22.toString(), e22);
            }
        }
        if (this.be + j <= this.bh) {
            return true;
        }
        return false;
    }

    public boolean hf(long j) {
        this.be += j;
        return true;
    }

    public String hg() {
        String str = ";";
        return new StringBuffer().append(4).append(str).append(this.bg).append(str).append(this.bh).append(str).append(this.be).append(str).append(this.bf).toString();
    }

    public boolean hh(String str) {
        try {
            if (TextUtils.isEmpty(str)) {
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "in loadFromString, info is empty!");
                return false;
            }
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "begin to parse:" + str);
            String[] split = str.split(";");
            if (split.length == 0) {
                return false;
            }
            int parseInt = Integer.parseInt(split[0]);
            if (parseInt == 4 && parseInt == split.length - 1) {
                this.bg = Long.parseLong(split[1]);
                this.bh = Long.parseLong(split[2]);
                this.be = Long.parseLong(split[3]);
                this.bf = Long.parseLong(split[4]);
                return true;
            }
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "in fileNum:" + parseInt + ", but need " + 4 + " parse " + str + " failed");
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
