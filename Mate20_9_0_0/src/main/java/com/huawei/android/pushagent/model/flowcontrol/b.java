package com.huawei.android.pushagent.model.flowcontrol;

import android.text.TextUtils;
import com.huawei.android.pushagent.utils.f.c;

class b implements Comparable<b> {
    private boolean il;
    private long im;

    /* synthetic */ b(b bVar) {
        this();
    }

    private b() {
    }

    public boolean aap(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        try {
            String[] split = str.split(";");
            if (split.length < 2) {
                c.eq("PushLog3413", "load connectinfo " + str + " error");
                return false;
            }
            this.im = Long.parseLong(split[0]);
            this.il = Boolean.parseBoolean(split[1]);
            return true;
        } catch (Throwable e) {
            c.es("PushLog3413", "load connectinfo " + str + " error:" + e.toString(), e);
            return false;
        }
    }

    public long aan() {
        return this.im;
    }

    public void aar(long j) {
        this.im = j;
    }

    public boolean aao() {
        return this.il;
    }

    public void aaq(boolean z) {
        this.il = z;
    }

    public String toString() {
        if (this.im <= 0) {
            return "";
        }
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(this.im).append(";").append(this.il);
        return stringBuffer.toString();
    }

    /* renamed from: aam */
    public int compareTo(b bVar) {
        return (int) ((aan() - bVar.aan()) / 1000);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return obj != null && getClass() == obj.getClass() && (obj instanceof b) && this.il == ((b) obj).il && this.im == ((b) obj).im;
    }

    public int hashCode() {
        return (this.il ? 1 : 0) + ((((int) (this.im ^ (this.im >>> 32))) + 527) * 31);
    }
}
