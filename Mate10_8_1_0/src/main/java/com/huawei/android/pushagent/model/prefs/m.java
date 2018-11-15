package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import com.huawei.android.pushagent.utils.a.c;

public class m {
    private static final byte[] dv = new byte[0];
    private static m dw;
    private final c dx;

    private m(Context context) {
        this.dx = new c(context, "PowerGenieControl");
    }

    public static m qx(Context context) {
        return rc(context);
    }

    private static m rc(Context context) {
        m mVar;
        synchronized (dv) {
            if (dw == null) {
                dw = new m(context);
            }
            mVar = dw;
        }
        return mVar;
    }

    public String qy() {
        return this.dx.aj("whiteList");
    }

    public boolean rb(String str) {
        return this.dx.ak("whiteList", str);
    }

    public int qz() {
        return this.dx.getInt("ctrlState", 0);
    }

    public void ra(int i) {
        this.dx.au("ctrlState", Integer.valueOf(i));
    }
}
