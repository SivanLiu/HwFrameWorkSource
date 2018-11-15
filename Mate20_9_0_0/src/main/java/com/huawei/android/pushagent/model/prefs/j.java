package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import com.huawei.android.pushagent.utils.f.a;
import com.huawei.android.pushagent.utils.f.c;
import java.util.Map;
import java.util.Map.Entry;

public class j {
    private static final byte[] gd = new byte[0];
    private static j ge;
    private final a gf;

    private j(Context context) {
        this.gf = new a(context, "token_request_flag");
    }

    public static j rb(Context context) {
        return rg(context);
    }

    private static j rg(Context context) {
        j jVar;
        synchronized (gd) {
            if (ge == null) {
                ge = new j(context);
            }
            jVar = ge;
        }
        return jVar;
    }

    public boolean rd() {
        try {
            Map all = this.gf.getAll();
            if (all == null) {
                return false;
            }
            Iterable<Entry> entrySet = all.entrySet();
            if (entrySet == null) {
                return false;
            }
            for (Entry value : entrySet) {
                if (((Boolean) value.getValue()).booleanValue()) {
                    return true;
                }
            }
            return false;
        } catch (Throwable e) {
            c.es("PushLog3413", e.toString(), e);
        }
    }

    public boolean rc(String str) {
        return this.gf.eb(str, false);
    }

    public void re(String str, boolean z) {
        this.gf.ea(str, Boolean.valueOf(z));
    }

    public void rf(String str) {
        this.gf.ed(str);
    }
}
