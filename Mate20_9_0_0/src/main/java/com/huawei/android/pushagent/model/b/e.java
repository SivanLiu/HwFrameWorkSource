package com.huawei.android.pushagent.model.b;

import java.util.ArrayList;
import java.util.List;

public class e {
    private static List<String> hb = new ArrayList();
    private static e hc = new e();
    private final Object ha = new Object();

    private e() {
    }

    public static e xz() {
        return hc;
    }

    public void yb(String str) {
        synchronized (this.ha) {
            if (hb.size() >= 50) {
                hb.remove(0);
            }
            hb.add(str);
        }
    }

    public boolean ya(String str) {
        boolean contains;
        synchronized (this.ha) {
            contains = hb.contains(str);
        }
        return contains;
    }
}
