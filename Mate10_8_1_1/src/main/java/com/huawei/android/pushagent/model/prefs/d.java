package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.a.c;
import com.huawei.android.pushagent.utils.f;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class d {
    private static final byte[] cu = new byte[0];
    private static d cv;
    private final c cw;

    private d(Context context) {
        this.cw = new c(context, "HeartBeatControl");
    }

    public static d lc(Context context) {
        return ln(context);
    }

    private static d ln(Context context) {
        d dVar;
        synchronized (cu) {
            if (cv == null) {
                cv = new d(context);
            }
            dVar = cv;
        }
        return dVar;
    }

    public String ld(Context context, int i) {
        String str = "";
        switch (i) {
            case 0:
                str = f.ga(context);
                return "data_" + str + "_" + f.gb(context);
            case 1:
                return "wifi_" + f.gc(context);
            case 999:
                return "bastet";
            default:
                return "unknow";
        }
    }

    private String[] li(Context context, int i) {
        Object aj = this.cw.aj(ld(context, i));
        if (TextUtils.isEmpty(aj)) {
            return null;
        }
        aj = aj.split("\\|");
        if (aj.length == 0) {
            return null;
        }
        if (aj.length >= HeartbeatControlSp$HeartBeatKey.NUM.ordinal()) {
            return aj;
        }
        Object obj = new String[]{"", "", "", ""};
        System.arraycopy(aj, 0, obj, 0, aj.length);
        return obj;
    }

    private boolean lo(Context context, int i, String str) {
        String ld = ld(context, i);
        if (this.cw.aq() >= 50) {
            b.z("PushLog2976", "heartbeat record more than 50, delete the old 30.");
            lg();
        }
        return this.cw.ak(ld, str);
    }

    private Map<String, Object> lr(Map<String, ?> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        Map<String, Object> linkedHashMap = new LinkedHashMap();
        List arrayList = new ArrayList(map.entrySet());
        Collections.sort(arrayList, new e());
        Iterator it = arrayList.iterator();
        int size = arrayList.size() - 20;
        int i = 0;
        while (it.hasNext() && size > 0) {
            if (i < size) {
                int i2 = i + 1;
                it.next();
                i = i2;
            } else {
                Entry entry = (Entry) it.next();
                linkedHashMap.put((String) entry.getKey(), entry.getValue());
            }
        }
        return linkedHashMap;
    }

    private void lg() {
        Map lr = lr(this.cw.getAll());
        this.cw.ao();
        this.cw.ar(lr);
    }

    private String lk(String str, String str2, String str3, String str4) {
        return str + "|" + str2 + "|" + str3 + "|" + str4;
    }

    public long ll(Context context, int i) {
        String[] li = li(context, i);
        if (li == null) {
            return -1;
        }
        try {
            return Long.parseLong(li[HeartbeatControlSp$HeartBeatKey.LastBestHBTime.ordinal()]);
        } catch (Exception e) {
            b.ab("PushLog2976", "parse last best heartbeat time exception.");
            return -1;
        }
    }

    public void le(Context context, int i) {
        String lk;
        long currentTimeMillis = System.currentTimeMillis();
        String[] li = li(context, i);
        String str = "";
        if (li == null) {
            try {
                lk = lk(String.valueOf(currentTimeMillis), "", "", "");
            } catch (ArrayIndexOutOfBoundsException e) {
                lk = lk(String.valueOf(currentTimeMillis), "", "", "");
            }
        } else {
            lk = lk(String.valueOf(currentTimeMillis), li[HeartbeatControlSp$HeartBeatKey.HasFindBestHB.ordinal()], li[HeartbeatControlSp$HeartBeatKey.LastInterval.ordinal()], li[HeartbeatControlSp$HeartBeatKey.BestHB.ordinal()]);
        }
        lo(context, i, lk);
    }

    public boolean lj(Context context, int i) {
        String[] li = li(context, i);
        if (li == null) {
            return false;
        }
        try {
            return Boolean.parseBoolean(li[HeartbeatControlSp$HeartBeatKey.HasFindBestHB.ordinal()]);
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }

    public void lp(Context context, int i, boolean z) {
        String lk;
        String[] li = li(context, i);
        String str = "";
        if (li == null) {
            try {
                lk = lk("", String.valueOf(z), "", "");
            } catch (ArrayIndexOutOfBoundsException e) {
                lk = lk("", String.valueOf(z), "", "");
            }
        } else {
            lk = lk(li[HeartbeatControlSp$HeartBeatKey.LastBestHBTime.ordinal()], String.valueOf(z), li[HeartbeatControlSp$HeartBeatKey.LastInterval.ordinal()], li[HeartbeatControlSp$HeartBeatKey.BestHB.ordinal()]);
        }
        lo(context, i, lk);
    }

    public long lm(Context context, int i) {
        String[] li = li(context, i);
        if (li == null) {
            return -1;
        }
        try {
            return Long.parseLong(li[HeartbeatControlSp$HeartBeatKey.LastInterval.ordinal()]);
        } catch (Exception e) {
            b.ab("PushLog2976", "parse last interval exception.");
            return -1;
        }
    }

    public void lq(Context context, int i, long j) {
        String lk;
        String[] li = li(context, i);
        String str = "";
        if (li == null) {
            try {
                lk = lk("", "", String.valueOf(j), "");
            } catch (ArrayIndexOutOfBoundsException e) {
                lk = lk("", "", String.valueOf(j), "");
            }
        } else {
            lk = lk(li[HeartbeatControlSp$HeartBeatKey.LastBestHBTime.ordinal()], li[HeartbeatControlSp$HeartBeatKey.HasFindBestHB.ordinal()], String.valueOf(j), li[HeartbeatControlSp$HeartBeatKey.BestHB.ordinal()]);
        }
        lo(context, i, lk);
    }

    public long lh(Context context, int i) {
        String[] li = li(context, i);
        if (li == null) {
            return -1;
        }
        try {
            return Long.parseLong(li[HeartbeatControlSp$HeartBeatKey.BestHB.ordinal()]);
        } catch (Exception e) {
            b.ab("PushLog2976", "parse best heartbeat exception.");
            return -1;
        }
    }

    public void lf(Context context, int i, long j) {
        String[] li = li(context, i);
        long currentTimeMillis = System.currentTimeMillis();
        String str = "";
        boolean z = j > 0;
        if (li == null) {
            try {
                str = lk(String.valueOf(currentTimeMillis), String.valueOf(z), "", String.valueOf(j));
            } catch (ArrayIndexOutOfBoundsException e) {
                str = lk(String.valueOf(currentTimeMillis), String.valueOf(false), "", String.valueOf(j));
            }
        } else {
            str = lk(String.valueOf(currentTimeMillis), String.valueOf(z), li[HeartbeatControlSp$HeartBeatKey.LastInterval.ordinal()], String.valueOf(j));
        }
        lo(context, i, str);
    }
}
