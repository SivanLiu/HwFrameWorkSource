package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.utils.b.a;
import com.huawei.android.pushagent.utils.b.b;
import com.huawei.android.pushagent.utils.d;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class m {
    /* renamed from: do */
    private static final byte[] f1do = new byte[0];
    private static m dp;
    private final b dq;

    private m(Context context) {
        this.dq = new b(context, "HeartBeatControl");
    }

    public static m mc(Context context) {
        return mr(context);
    }

    private static m mr(Context context) {
        m mVar;
        synchronized (f1do) {
            if (dp == null) {
                dp = new m(context);
            }
            mVar = dp;
        }
        return mVar;
    }

    public String mk(Context context, int i) {
        String str = "";
        switch (i) {
            case 0:
                str = d.yj(context);
                String yk = d.yk(context);
                if (TextUtils.isEmpty(yk)) {
                    return "unknow";
                }
                return "data_" + str + "_" + yk;
            case 1:
                return "wifi_" + d.yi(context);
            case 999:
                return "bastet";
            default:
                return "unknow";
        }
    }

    private String[] mp(Context context, int i) {
        String tg = this.dq.tg(mk(context, i));
        if (TextUtils.isEmpty(tg)) {
            return null;
        }
        String[] split = tg.split("\\|");
        if (split.length == 0) {
            return null;
        }
        if (split.length >= HeartbeatControlSp$HeartBeatKey.NUM.ordinal()) {
            return split;
        }
        String[] strArr = new String[]{"", "", "", "", ""};
        System.arraycopy(split, 0, strArr, 0, split.length);
        return strArr;
    }

    private boolean ms(Context context, int i, String str) {
        String mk = mk(context, i);
        if (this.dq.tq() >= 50) {
            a.sv("PushLog3414", "heartbeat record more than 50, delete the old 30.");
            mo();
        }
        return this.dq.tm(mk, str);
    }

    private Map<String, Object> mt(Map<String, ?> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        ArrayList arrayList = new ArrayList(map.entrySet());
        Collections.sort(arrayList, new n());
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

    private void mo() {
        try {
            Map mt = mt(this.dq.getAll());
            this.dq.tj();
            this.dq.tn(mt);
        } catch (Exception e) {
            a.sw("PushLog3414", "deleteInvalidRecord exception is ", e);
        }
    }

    private String mq(String str, String str2, String str3, String str4, String str5) {
        return str + "|" + str2 + "|" + str3 + "|" + str4 + "|" + str5;
    }

    public long mi(Context context, int i) {
        String[] mp = mp(context, i);
        if (mp == null) {
            return -1;
        }
        try {
            return Long.parseLong(mp[HeartbeatControlSp$HeartBeatKey.LastBestHBTime.ordinal()]);
        } catch (Exception e) {
            a.sx("PushLog3414", "parse last best heartbeat time exception.");
            return -1;
        }
    }

    public void md(Context context, int i) {
        String mq;
        long currentTimeMillis = System.currentTimeMillis();
        String[] mp = mp(context, i);
        String str = "";
        if (mp == null) {
            try {
                mq = mq(String.valueOf(currentTimeMillis), "", "", "", "");
            } catch (ArrayIndexOutOfBoundsException e) {
                mq = mq(String.valueOf(currentTimeMillis), "", "", "", "");
            }
        } else {
            mq = mq(String.valueOf(currentTimeMillis), mp[HeartbeatControlSp$HeartBeatKey.HasFindBestHB.ordinal()], mp[HeartbeatControlSp$HeartBeatKey.LastInterval.ordinal()], mp[HeartbeatControlSp$HeartBeatKey.BestHB.ordinal()], mp[HeartbeatControlSp$HeartBeatKey.IsBack.ordinal()]);
        }
        ms(context, i, mq);
    }

    public boolean mj(Context context, int i) {
        String[] mp = mp(context, i);
        if (mp == null) {
            return false;
        }
        try {
            return Boolean.parseBoolean(mp[HeartbeatControlSp$HeartBeatKey.HasFindBestHB.ordinal()]);
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }

    public void mm(Context context, int i, boolean z) {
        String mq;
        String[] mp = mp(context, i);
        String str = "";
        if (mp == null) {
            try {
                mq = mq("", String.valueOf(z), "", "", "");
            } catch (ArrayIndexOutOfBoundsException e) {
                mq = mq("", String.valueOf(z), "", "", "");
            }
        } else {
            mq = mq(mp[HeartbeatControlSp$HeartBeatKey.LastBestHBTime.ordinal()], String.valueOf(z), mp[HeartbeatControlSp$HeartBeatKey.LastInterval.ordinal()], mp[HeartbeatControlSp$HeartBeatKey.BestHB.ordinal()], mp[HeartbeatControlSp$HeartBeatKey.IsBack.ordinal()]);
        }
        ms(context, i, mq);
    }

    public long ml(Context context, int i) {
        String[] mp = mp(context, i);
        if (mp == null) {
            return -1;
        }
        try {
            return Long.parseLong(mp[HeartbeatControlSp$HeartBeatKey.LastInterval.ordinal()]);
        } catch (Exception e) {
            a.sx("PushLog3414", "parse last interval exception.");
            return -1;
        }
    }

    public void mn(Context context, int i, long j) {
        String mq;
        String[] mp = mp(context, i);
        String str = "";
        if (mp == null) {
            try {
                mq = mq("", "", String.valueOf(j), "", "");
            } catch (ArrayIndexOutOfBoundsException e) {
                mq = mq("", "", String.valueOf(j), "", "");
            }
        } else {
            mq = mq(mp[HeartbeatControlSp$HeartBeatKey.LastBestHBTime.ordinal()], mp[HeartbeatControlSp$HeartBeatKey.HasFindBestHB.ordinal()], String.valueOf(j), mp[HeartbeatControlSp$HeartBeatKey.BestHB.ordinal()], mp[HeartbeatControlSp$HeartBeatKey.IsBack.ordinal()]);
        }
        ms(context, i, mq);
    }

    public long me(Context context, int i) {
        String[] mp = mp(context, i);
        if (mp == null) {
            return -1;
        }
        try {
            return Long.parseLong(mp[HeartbeatControlSp$HeartBeatKey.BestHB.ordinal()]);
        } catch (Exception e) {
            a.sx("PushLog3414", "parse best heartbeat exception.");
            return -1;
        }
    }

    public void mf(Context context, int i, long j) {
        String[] mp = mp(context, i);
        long currentTimeMillis = System.currentTimeMillis();
        String str = "";
        boolean z = j > 0;
        if (mp == null) {
            try {
                str = mq(String.valueOf(currentTimeMillis), String.valueOf(z), "", String.valueOf(j), "");
            } catch (ArrayIndexOutOfBoundsException e) {
                str = mq(String.valueOf(currentTimeMillis), String.valueOf(false), "", String.valueOf(j), "");
            }
        } else {
            str = mq(String.valueOf(currentTimeMillis), String.valueOf(z), mp[HeartbeatControlSp$HeartBeatKey.LastInterval.ordinal()], String.valueOf(j), mp[HeartbeatControlSp$HeartBeatKey.IsBack.ordinal()]);
        }
        ms(context, i, str);
    }

    public boolean mg(Context context, int i) {
        String[] mp = mp(context, i);
        if (mp == null) {
            return false;
        }
        try {
            return Boolean.parseBoolean(mp[HeartbeatControlSp$HeartBeatKey.IsBack.ordinal()]);
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }

    public void mh(Context context, int i, boolean z) {
        String mq;
        String[] mp = mp(context, i);
        String str = "";
        if (mp == null) {
            try {
                mq = mq("", "", "", "", String.valueOf(z));
            } catch (ArrayIndexOutOfBoundsException e) {
                mq = mq("", "", "", "", String.valueOf(z));
            }
        } else {
            mq = mq(mp[HeartbeatControlSp$HeartBeatKey.LastBestHBTime.ordinal()], mp[HeartbeatControlSp$HeartBeatKey.HasFindBestHB.ordinal()], mp[HeartbeatControlSp$HeartBeatKey.LastInterval.ordinal()], mp[HeartbeatControlSp$HeartBeatKey.BestHB.ordinal()], String.valueOf(z));
        }
        ms(context, i, mq);
    }
}
