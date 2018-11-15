package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.utils.f.a;
import com.huawei.android.pushagent.utils.f.c;
import com.huawei.android.pushagent.utils.g;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class f {
    private static final byte[] fn = new byte[0];
    private static f fo;
    private final a fp;

    private f(Context context) {
        this.fp = new a(context, "HeartBeatControl");
    }

    public static f py(Context context) {
        return qn(context);
    }

    private static f qn(Context context) {
        f fVar;
        synchronized (fn) {
            if (fo == null) {
                fo = new f(context);
            }
            fVar = fo;
        }
        return fVar;
    }

    public String qg(Context context, int i) {
        String str = "";
        switch (i) {
            case 0:
                str = g.fy(context);
                Object fz = g.fz(context);
                if (TextUtils.isEmpty(fz)) {
                    return "unknow";
                }
                return "data_" + str + "_" + fz;
            case 1:
                return "wifi_" + g.fx(context);
            case 999:
                return "bastet";
            default:
                return "unknow";
        }
    }

    private String[] ql(Context context, int i) {
        Object ec = this.fp.ec(qg(context, i));
        if (TextUtils.isEmpty(ec)) {
            return null;
        }
        ec = ec.split("\\|");
        if (ec.length == 0) {
            return null;
        }
        if (ec.length >= HeartbeatControlSp$HeartBeatKey.NUM.ordinal()) {
            return ec;
        }
        Object obj = new String[]{"", "", "", "", ""};
        System.arraycopy(ec, 0, obj, 0, ec.length);
        return obj;
    }

    private boolean qo(Context context, int i, String str) {
        String qg = qg(context, i);
        if (this.fp.eh() >= 50) {
            c.ep("PushLog3413", "heartbeat record more than 50, delete the old 30.");
            qk();
        }
        return this.fp.ee(qg, str);
    }

    private Map<String, Object> qp(Map<String, ?> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        Map<String, Object> linkedHashMap = new LinkedHashMap();
        List arrayList = new ArrayList(map.entrySet());
        Collections.sort(arrayList, new g());
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

    private void qk() {
        Map qp = qp(this.fp.getAll());
        this.fp.dz();
        this.fp.ei(qp);
    }

    private String qm(String str, String str2, String str3, String str4, String str5) {
        return str + "|" + str2 + "|" + str3 + "|" + str4 + "|" + str5;
    }

    public long qe(Context context, int i) {
        String[] ql = ql(context, i);
        if (ql == null) {
            return -1;
        }
        try {
            return Long.parseLong(ql[HeartbeatControlSp$HeartBeatKey.LastBestHBTime.ordinal()]);
        } catch (Exception e) {
            c.eo("PushLog3413", "parse last best heartbeat time exception.");
            return -1;
        }
    }

    public void pz(Context context, int i) {
        String qm;
        long currentTimeMillis = System.currentTimeMillis();
        String[] ql = ql(context, i);
        String str = "";
        if (ql == null) {
            try {
                qm = qm(String.valueOf(currentTimeMillis), "", "", "", "");
            } catch (ArrayIndexOutOfBoundsException e) {
                qm = qm(String.valueOf(currentTimeMillis), "", "", "", "");
            }
        } else {
            qm = qm(String.valueOf(currentTimeMillis), ql[HeartbeatControlSp$HeartBeatKey.HasFindBestHB.ordinal()], ql[HeartbeatControlSp$HeartBeatKey.LastInterval.ordinal()], ql[HeartbeatControlSp$HeartBeatKey.BestHB.ordinal()], ql[HeartbeatControlSp$HeartBeatKey.IsBack.ordinal()]);
        }
        qo(context, i, qm);
    }

    public boolean qf(Context context, int i) {
        String[] ql = ql(context, i);
        if (ql == null) {
            return false;
        }
        try {
            return Boolean.parseBoolean(ql[HeartbeatControlSp$HeartBeatKey.HasFindBestHB.ordinal()]);
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }

    public void qi(Context context, int i, boolean z) {
        String qm;
        String[] ql = ql(context, i);
        String str = "";
        if (ql == null) {
            try {
                qm = qm("", String.valueOf(z), "", "", "");
            } catch (ArrayIndexOutOfBoundsException e) {
                qm = qm("", String.valueOf(z), "", "", "");
            }
        } else {
            qm = qm(ql[HeartbeatControlSp$HeartBeatKey.LastBestHBTime.ordinal()], String.valueOf(z), ql[HeartbeatControlSp$HeartBeatKey.LastInterval.ordinal()], ql[HeartbeatControlSp$HeartBeatKey.BestHB.ordinal()], ql[HeartbeatControlSp$HeartBeatKey.IsBack.ordinal()]);
        }
        qo(context, i, qm);
    }

    public long qh(Context context, int i) {
        String[] ql = ql(context, i);
        if (ql == null) {
            return -1;
        }
        try {
            return Long.parseLong(ql[HeartbeatControlSp$HeartBeatKey.LastInterval.ordinal()]);
        } catch (Exception e) {
            c.eo("PushLog3413", "parse last interval exception.");
            return -1;
        }
    }

    public void qj(Context context, int i, long j) {
        String qm;
        String[] ql = ql(context, i);
        String str = "";
        if (ql == null) {
            try {
                qm = qm("", "", String.valueOf(j), "", "");
            } catch (ArrayIndexOutOfBoundsException e) {
                qm = qm("", "", String.valueOf(j), "", "");
            }
        } else {
            qm = qm(ql[HeartbeatControlSp$HeartBeatKey.LastBestHBTime.ordinal()], ql[HeartbeatControlSp$HeartBeatKey.HasFindBestHB.ordinal()], String.valueOf(j), ql[HeartbeatControlSp$HeartBeatKey.BestHB.ordinal()], ql[HeartbeatControlSp$HeartBeatKey.IsBack.ordinal()]);
        }
        qo(context, i, qm);
    }

    public long qa(Context context, int i) {
        String[] ql = ql(context, i);
        if (ql == null) {
            return -1;
        }
        try {
            return Long.parseLong(ql[HeartbeatControlSp$HeartBeatKey.BestHB.ordinal()]);
        } catch (Exception e) {
            c.eo("PushLog3413", "parse best heartbeat exception.");
            return -1;
        }
    }

    public void qb(Context context, int i, long j) {
        String[] ql = ql(context, i);
        long currentTimeMillis = System.currentTimeMillis();
        String str = "";
        boolean z = j > 0;
        if (ql == null) {
            try {
                str = qm(String.valueOf(currentTimeMillis), String.valueOf(z), "", String.valueOf(j), "");
            } catch (ArrayIndexOutOfBoundsException e) {
                str = qm(String.valueOf(currentTimeMillis), String.valueOf(false), "", String.valueOf(j), "");
            }
        } else {
            str = qm(String.valueOf(currentTimeMillis), String.valueOf(z), ql[HeartbeatControlSp$HeartBeatKey.LastInterval.ordinal()], String.valueOf(j), ql[HeartbeatControlSp$HeartBeatKey.IsBack.ordinal()]);
        }
        qo(context, i, str);
    }

    public boolean qc(Context context, int i) {
        String[] ql = ql(context, i);
        if (ql == null) {
            return false;
        }
        try {
            return Boolean.parseBoolean(ql[HeartbeatControlSp$HeartBeatKey.IsBack.ordinal()]);
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }

    public void qd(Context context, int i, boolean z) {
        String qm;
        String[] ql = ql(context, i);
        String str = "";
        if (ql == null) {
            try {
                qm = qm("", "", "", "", String.valueOf(z));
            } catch (ArrayIndexOutOfBoundsException e) {
                qm = qm("", "", "", "", String.valueOf(z));
            }
        } else {
            qm = qm(ql[HeartbeatControlSp$HeartBeatKey.LastBestHBTime.ordinal()], ql[HeartbeatControlSp$HeartBeatKey.HasFindBestHB.ordinal()], ql[HeartbeatControlSp$HeartBeatKey.LastInterval.ordinal()], ql[HeartbeatControlSp$HeartBeatKey.BestHB.ordinal()], String.valueOf(z));
        }
        qo(context, i, qm);
    }
}
