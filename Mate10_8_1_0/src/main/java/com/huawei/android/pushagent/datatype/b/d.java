package com.huawei.android.pushagent.datatype.b;

public class d {
    private String ht;
    private int hu;
    private boolean hv;

    public d(String str, int i, boolean z) {
        this.ht = str;
        this.hu = i;
        this.hv = z;
    }

    public String toString() {
        return new StringBuffer().append("ip:").append(this.ht).append(" port:").append(this.hu).append(" useProxy:").append(this.hv).toString();
    }

    public String yj() {
        return this.ht;
    }

    public void ym(String str) {
        this.ht = str;
    }

    public int yk() {
        return this.hu;
    }

    public void yn(int i) {
        this.hu = i;
    }

    public boolean yl() {
        return this.hv;
    }
}
