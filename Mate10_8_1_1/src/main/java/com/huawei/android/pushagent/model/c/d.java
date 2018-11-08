package com.huawei.android.pushagent.model.c;

import java.util.ArrayList;
import java.util.List;

public class d {
    private static List<String> en = new ArrayList();
    private static d eo = new d();
    private final Object em = new Object();

    private d() {
    }

    public static d td() {
        return eo;
    }

    public void tc(String str) {
        synchronized (this.em) {
            if (en.size() >= 50) {
                en.remove(0);
            }
            en.add(str);
        }
    }

    public boolean te(String str) {
        boolean contains;
        synchronized (this.em) {
            contains = en.contains(str);
        }
        return contains;
    }
}
