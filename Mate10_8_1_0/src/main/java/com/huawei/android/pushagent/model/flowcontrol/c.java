package com.huawei.android.pushagent.model.flowcontrol;

import android.text.TextUtils;
import com.huawei.android.pushagent.utils.a.b;

class c implements Comparable<c> {
    private boolean ci;
    private long cj;

    private c() {
    }

    public boolean it(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        try {
            String[] split = str.split(";");
            if (split.length < 2) {
                b.y("PushLog2976", "load connectinfo " + str + " error");
                return false;
            }
            this.cj = Long.parseLong(split[0]);
            this.ci = Boolean.parseBoolean(split[1]);
            return true;
        } catch (Throwable e) {
            b.aa("PushLog2976", "load connectinfo " + str + " error:" + e.toString(), e);
            return false;
        }
    }

    public long ir() {
        return this.cj;
    }

    public void iv(long j) {
        this.cj = j;
    }

    public boolean is() {
        return this.ci;
    }

    public void iu(boolean z) {
        this.ci = z;
    }

    public String toString() {
        if (this.cj <= 0) {
            return "";
        }
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(this.cj).append(";").append(this.ci);
        return stringBuffer.toString();
    }

    public /* bridge */ /* synthetic */ int compareTo(Object obj) {
        return iq((c) obj);
    }

    public int iq(c cVar) {
        return (int) ((ir() - cVar.ir()) / 1000);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return obj != null && getClass() == obj.getClass() && (obj instanceof c) && this.ci == ((c) obj).ci && this.cj == ((c) obj).cj;
    }

    public int hashCode() {
        return (this.ci ? 1 : 0) + ((((int) (this.cj ^ (this.cj >>> 32))) + 527) * 31);
    }
}
