package com.huawei.android.pushagent.model.flowcontrol;

import android.text.TextUtils;
import com.huawei.android.pushagent.utils.b.a;

class c implements Comparable<c> {
    private boolean ff;
    private long fg;

    /* synthetic */ c(c cVar) {
        this();
    }

    private c() {
    }

    public boolean ot(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        try {
            String[] split = str.split(";");
            if (split.length < 2) {
                a.su("PushLog3414", "load connectinfo " + str + " error");
                return false;
            }
            this.fg = Long.parseLong(split[0]);
            this.ff = Boolean.parseBoolean(split[1]);
            return true;
        } catch (Exception e) {
            a.sw("PushLog3414", "load connectinfo " + str + " error:" + e.toString(), e);
            return false;
        }
    }

    public long or() {
        return this.fg;
    }

    public void ov(long j) {
        this.fg = j;
    }

    public boolean os() {
        return this.ff;
    }

    public void ou(boolean z) {
        this.ff = z;
    }

    public String toString() {
        if (this.fg <= 0) {
            return "";
        }
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(this.fg).append(";").append(this.ff);
        return stringBuffer.toString();
    }

    /* renamed from: oq */
    public int compareTo(c cVar) {
        return (int) ((or() - cVar.or()) / 1000);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return obj != null && getClass() == obj.getClass() && (obj instanceof c) && this.ff == ((c) obj).ff && this.fg == ((c) obj).fg;
    }

    public int hashCode() {
        return (this.ff ? 1 : 0) + ((((int) (this.fg ^ (this.fg >>> 32))) + 527) * 31);
    }
}
