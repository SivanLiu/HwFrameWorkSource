package com.huawei.android.pushagent.datatype.a;

public class a {
    private String ca;
    private int cb;
    private boolean cc;

    public a(String str, int i, boolean z) {
        this.ca = str;
        this.cb = i;
        this.cc = z;
    }

    public String toString() {
        return new StringBuffer().append("ip:").append(this.ca).append(" port:").append(this.cb).append(" useProxy:").append(this.cc).toString();
    }

    public String ka() {
        return this.ca;
    }

    public void kd(String str) {
        this.ca = str;
    }

    public int kb() {
        return this.cb;
    }

    public void ke(int i) {
        this.cb = i;
    }

    public boolean kc() {
        return this.cc;
    }
}
