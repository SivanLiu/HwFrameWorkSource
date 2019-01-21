package com.huawei.android.pushagent.model.c;

import java.util.ArrayList;
import java.util.List;

public class a {
    private static List<String> fi = new ArrayList();
    private static a fj = new a();
    private final Object fh = new Object();

    private a() {
    }

    public static a oy() {
        return fj;
    }

    public void ox(String str) {
        synchronized (this.fh) {
            if (fi.size() >= 50) {
                fi.remove(0);
            }
            fi.add(str);
        }
    }

    public boolean oz(String str) {
        boolean contains;
        synchronized (this.fh) {
            contains = fi.contains(str);
        }
        return contains;
    }
}
