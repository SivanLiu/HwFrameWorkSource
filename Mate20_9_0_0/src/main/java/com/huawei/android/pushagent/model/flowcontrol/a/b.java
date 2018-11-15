package com.huawei.android.pushagent.model.flowcontrol.a;

import android.text.TextUtils;
import com.huawei.android.pushagent.utils.f.c;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class b implements a {
    private long hp;
    private long hq;
    private long hr;
    private long hs;

    public b(long j, long j2) {
        this.hr = j;
        this.hs = j2;
        this.hp = 0;
        this.hq = 0;
    }

    public boolean zs(a aVar) {
        boolean z = false;
        if (!(aVar instanceof b)) {
            return false;
        }
        b bVar = (b) aVar;
        if (this.hr == bVar.hr && this.hs == bVar.hs) {
            z = true;
        }
        return z;
    }

    public boolean zr(long j) {
        c.er("PushLog3413", "enter FlowSimpleControl::canApply(num:" + j + ", curVol:" + this.hp + ", maxVol:" + this.hs + ")");
        Long valueOf = Long.valueOf(System.currentTimeMillis());
        if (valueOf.longValue() < this.hq || valueOf.longValue() - this.hq >= this.hr) {
            c.er("PushLog3413", " fistrControlTime:" + new Date(this.hq) + " interval:" + (valueOf.longValue() - this.hq) + " statInterval:" + this.hr + " change fistrControlTime to cur");
            this.hq = valueOf.longValue();
            this.hp = 0;
        } else {
            try {
                Calendar instance = Calendar.getInstance(Locale.getDefault());
                instance.setTimeInMillis(this.hq);
                int i = instance.get(2);
                instance.setTimeInMillis(valueOf.longValue());
                if (i != instance.get(2)) {
                    this.hq = valueOf.longValue();
                    this.hp = 0;
                }
            } catch (Throwable e) {
                c.es("PushLog3413", e.toString(), e);
            } catch (Throwable e2) {
                c.es("PushLog3413", e2.toString(), e2);
            } catch (Throwable e22) {
                c.es("PushLog3413", e22.toString(), e22);
            }
        }
        if (this.hp + j <= this.hs) {
            return true;
        }
        return false;
    }

    public boolean zq(long j) {
        this.hp += j;
        return true;
    }

    public String zt() {
        String str = ";";
        return new StringBuffer().append(4).append(str).append(this.hr).append(str).append(this.hs).append(str).append(this.hp).append(str).append(this.hq).toString();
    }

    public boolean zu(String str) {
        try {
            if (TextUtils.isEmpty(str)) {
                c.ep("PushLog3413", "in loadFromString, info is empty!");
                return false;
            }
            c.er("PushLog3413", "begin to parse:" + str);
            String[] split = str.split(";");
            if (split.length == 0) {
                return false;
            }
            int parseInt = Integer.parseInt(split[0]);
            if (parseInt == 4 && parseInt == split.length - 1) {
                this.hr = Long.parseLong(split[1]);
                this.hs = Long.parseLong(split[2]);
                this.hp = Long.parseLong(split[3]);
                this.hq = Long.parseLong(split[4]);
                return true;
            }
            c.eq("PushLog3413", "in fileNum:" + parseInt + ", but need " + 4 + " parse " + str + " failed");
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
