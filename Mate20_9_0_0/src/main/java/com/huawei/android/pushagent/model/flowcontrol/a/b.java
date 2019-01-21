package com.huawei.android.pushagent.model.flowcontrol.a;

import android.text.TextUtils;
import com.huawei.android.pushagent.utils.b.a;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class b implements a {
    private long eb;
    private long ec;
    private long ed;
    private long ee;

    public b(long j, long j2) {
        this.ed = j;
        this.ee = j2;
        this.eb = 0;
        this.ec = 0;
    }

    public boolean nf(a aVar) {
        boolean z = false;
        if (!(aVar instanceof b)) {
            return false;
        }
        b bVar = (b) aVar;
        if (this.ed == bVar.ed && this.ee == bVar.ee) {
            z = true;
        }
        return z;
    }

    public boolean ne(long j) {
        a.st("PushLog3414", "enter FlowSimpleControl::canApply(num:" + j + ", curVol:" + this.eb + ", maxVol:" + this.ee + ")");
        Long valueOf = Long.valueOf(System.currentTimeMillis());
        if (valueOf.longValue() < this.ec || valueOf.longValue() - this.ec >= this.ed) {
            a.st("PushLog3414", " fistrControlTime:" + new Date(this.ec) + " interval:" + (valueOf.longValue() - this.ec) + " statInterval:" + this.ed + " change fistrControlTime to cur");
            this.ec = valueOf.longValue();
            this.eb = 0;
        } else {
            try {
                Calendar instance = Calendar.getInstance(Locale.getDefault());
                instance.setTimeInMillis(this.ec);
                int i = instance.get(2);
                instance.setTimeInMillis(valueOf.longValue());
                if (i != instance.get(2)) {
                    this.ec = valueOf.longValue();
                    this.eb = 0;
                }
            } catch (IllegalArgumentException e) {
                a.sw("PushLog3414", e.toString(), e);
            } catch (ArrayIndexOutOfBoundsException e2) {
                a.sw("PushLog3414", e2.toString(), e2);
            } catch (Exception e3) {
                a.sw("PushLog3414", e3.toString(), e3);
            }
        }
        if (this.eb + j <= this.ee) {
            return true;
        }
        return false;
    }

    public boolean nd(long j) {
        this.eb += j;
        return true;
    }

    public String ng() {
        String str = ";";
        return new StringBuffer().append(4).append(str).append(this.ed).append(str).append(this.ee).append(str).append(this.eb).append(str).append(this.ec).toString();
    }

    public boolean nh(String str) {
        try {
            if (TextUtils.isEmpty(str)) {
                a.sv("PushLog3414", "in loadFromString, info is empty!");
                return false;
            }
            a.st("PushLog3414", "begin to parse:" + str);
            String[] split = str.split(";");
            if (split.length == 0) {
                return false;
            }
            int parseInt = Integer.parseInt(split[0]);
            if (parseInt == 4 && parseInt == split.length - 1) {
                this.ed = Long.parseLong(split[1]);
                this.ee = Long.parseLong(split[2]);
                this.eb = Long.parseLong(split[3]);
                this.ec = Long.parseLong(split[4]);
                return true;
            }
            a.su("PushLog3414", "in fileNum:" + parseInt + ", but need " + 4 + " parse " + str + " failed");
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
