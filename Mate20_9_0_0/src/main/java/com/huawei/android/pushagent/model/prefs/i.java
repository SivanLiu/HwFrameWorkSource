package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import com.huawei.android.pushagent.utils.b.b;

public class i {
    private static final byte[] dc = new byte[0];
    private static i dd;
    private final b de;

    private i(Context context) {
        this.de = new b(context, "PowerGenieControl");
    }

    public static i lg(Context context) {
        return ll(context);
    }

    private static i ll(Context context) {
        i iVar;
        synchronized (dc) {
            if (dd == null) {
                dd = new i(context);
            }
            iVar = dd;
        }
        return iVar;
    }

    public String lh() {
        return this.de.tg("whiteList");
    }

    public boolean lk(String str) {
        return this.de.tm("whiteList", str);
    }

    public int li() {
        return this.de.getInt("ctrlState", 0);
    }

    public void lj(int i) {
        this.de.to("ctrlState", Integer.valueOf(i));
    }
}
