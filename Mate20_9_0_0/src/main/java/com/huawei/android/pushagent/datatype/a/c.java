package com.huawei.android.pushagent.datatype.a;

public class c {
    private String j;
    private int k;
    private boolean l;

    public c(String str, int i, boolean z) {
        this.j = str;
        this.k = i;
        this.l = z;
    }

    public String toString() {
        return new StringBuffer().append("ip:").append(this.j).append(" port:").append(this.k).append(" useProxy:").append(this.l).toString();
    }

    public String au() {
        return this.j;
    }

    public void as(String str) {
        this.j = str;
    }

    public int av() {
        return this.k;
    }

    public void at(int i) {
        this.k = i;
    }

    public boolean aw() {
        return this.l;
    }
}
