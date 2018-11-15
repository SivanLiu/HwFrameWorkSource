package com.huawei.android.pushagent.model.flowcontrol;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.model.flowcontrol.a.a;
import com.huawei.android.pushagent.model.flowcontrol.a.b;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.model.prefs.l;
import com.huawei.android.pushagent.utils.g;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

public class c {
    private static c io = null;
    private Context in = null;
    private List<a> ip = new LinkedList();
    private List<a> iq = new LinkedList();
    private List<a> ir = new LinkedList();
    private List<a> is = new LinkedList();
    private List<a> it = new LinkedList();
    private List<a> iu = new LinkedList();

    private c(Context context) {
        this.in = context;
        abf();
        if (this.ip.size() == 0 && this.iq.size() == 0 && this.ir.size() == 0 && this.is.size() == 0 && this.it.size() == 0 && this.iu.size() == 0) {
            com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "Connect Control is not set, begin to config it");
            abb();
        }
    }

    public static synchronized boolean aba(Context context) {
        synchronized (c.class) {
            io = abc(context);
            if (io == null) {
                com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "cannot get ConnectControlMgr instance, may be system err!!");
                return false;
            }
            boolean aaz = io.aaz();
            return aaz;
        }
    }

    public static synchronized boolean aay(Context context, int i) {
        synchronized (c.class) {
            io = abc(context);
            if (io == null) {
                com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "cannot get ConnectControlMgr instance, may be system err!!");
                return false;
            }
            boolean aax = io.aax(i);
            return aax;
        }
    }

    public static synchronized c abc(Context context) {
        c cVar;
        synchronized (c.class) {
            if (io == null) {
                io = new c(context);
            }
            cVar = io;
        }
        return cVar;
    }

    public static void abh(Context context) {
        io = abc(context);
        if (io != null && (io.abe() ^ 1) != 0) {
            com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "TRS cfg change, need reload");
            io.abb();
        }
    }

    private boolean abd(List<a> list, List<a> list2) {
        if (list == null && list2 == null) {
            return true;
        }
        if (list == null || list2 == null || list.size() != list2.size()) {
            return false;
        }
        for (a aVar : list) {
            boolean z;
            for (a zs : list2) {
                if (aVar.zs(zs)) {
                    z = true;
                    continue;
                    break;
                }
            }
            z = false;
            continue;
            if (!z) {
                return false;
            }
        }
        return true;
    }

    private boolean abe() {
        List linkedList = new LinkedList();
        linkedList.add(new b(86400000, k.rh(this.in).th()));
        linkedList.add(new b(3600000, k.rh(this.in).ti()));
        if (abd(linkedList, this.ip)) {
            linkedList = new LinkedList();
            linkedList.add(new b(86400000, k.rh(this.in).tj()));
            if (abd(linkedList, this.iq)) {
                List linkedList2 = new LinkedList();
                for (Entry entry : k.rh(this.in).tk().entrySet()) {
                    linkedList2.add(new b(((Long) entry.getKey()).longValue() * 1000, ((Long) entry.getValue()).longValue()));
                }
                if (abd(linkedList2, this.ir)) {
                    linkedList = new LinkedList();
                    linkedList.add(new b(86400000, k.rh(this.in).tl()));
                    linkedList.add(new b(3600000, k.rh(this.in).tm()));
                    if (abd(linkedList, this.is)) {
                        linkedList = new LinkedList();
                        linkedList.add(new b(86400000, k.rh(this.in).tn()));
                        if (abd(linkedList, this.it)) {
                            linkedList2 = new LinkedList();
                            for (Entry entry2 : k.rh(this.in).to().entrySet()) {
                                linkedList2.add(new b(((Long) entry2.getKey()).longValue() * 1000, ((Long) entry2.getValue()).longValue()));
                            }
                            if (abd(linkedList2, this.iu)) {
                                com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "cur control is equal trs cfg");
                                return true;
                            }
                            com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "wifiVolumeControl cfg is change!!");
                            return false;
                        }
                        com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "wifiTrsFlowControl cfg is change!!");
                        return false;
                    }
                    com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "wifiTrsFirstFlowControl cfg is change!");
                    return false;
                }
                com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "flowcControl cfg is change!!");
                return false;
            }
            com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "trsFlowControl cfg is change!!");
            return false;
        }
        com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "trsFirstFlowControl cfg is change!");
        return false;
    }

    private boolean abb() {
        this.ip.clear();
        this.ip.add(new b(86400000, k.rh(this.in).th()));
        this.ip.add(new b(3600000, k.rh(this.in).ti()));
        this.iq.clear();
        this.iq.add(new b(86400000, k.rh(this.in).tj()));
        this.ir.clear();
        for (Entry entry : k.rh(this.in).tk().entrySet()) {
            this.ir.add(new b(((Long) entry.getKey()).longValue() * 1000, ((Long) entry.getValue()).longValue()));
        }
        this.is.clear();
        this.is.add(new b(86400000, k.rh(this.in).tl()));
        this.is.add(new b(3600000, k.rh(this.in).tm()));
        this.it.clear();
        this.it.add(new b(86400000, k.rh(this.in).tn()));
        this.iu.clear();
        for (Entry entry2 : k.rh(this.in).to().entrySet()) {
            this.iu.add(new b(((Long) entry2.getKey()).longValue() * 1000, ((Long) entry2.getValue()).longValue()));
        }
        abi();
        return true;
    }

    private boolean aaz() {
        if (1 == g.fw(this.in)) {
            return aaw(this.is, this.it);
        }
        return aaw(this.ip, this.iq);
    }

    private boolean aax(int i) {
        if (1 == g.fw(this.in)) {
            return aav(this.iu);
        }
        return aav(this.ir);
    }

    private boolean aau(List<a> list, long j) {
        if (list == null || list.size() == 0) {
            com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "there is no volome control");
            return true;
        }
        for (a zr : list) {
            if (!zr.zr(j)) {
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "push connect time exceed volum");
                return false;
            }
        }
        com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "check control flow pass");
        return true;
    }

    /* JADX WARNING: Missing block: B:7:0x000c, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized boolean aat(List<a> list, long j) {
        if (list != null) {
            if (list.size() != 0) {
                for (a aVar : list) {
                    if (!aVar.zq(j)) {
                        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", " control info:" + aVar);
                        return false;
                    }
                }
                return true;
            }
        }
    }

    private boolean abi() {
        try {
            com.huawei.android.pushagent.utils.f.a aVar = new com.huawei.android.pushagent.utils.f.a(this.in, "PushConnectControl");
            if (abj(aVar, this.is, "wifiTrsFirstFlowControlData") && abj(aVar, this.it, "wifiTrsFlowControlData") && abj(aVar, this.iu, "wifiVolumeControlData") && abj(aVar, this.ip, "trsFirstFlowControlData") && abj(aVar, this.iq, "trsFlowControlData") && abj(aVar, this.ir, "volumeControlData")) {
                return true;
            }
            return false;
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.f.c.es("PushLog3413", e.toString(), e);
            return false;
        }
    }

    private boolean abf() {
        try {
            com.huawei.android.pushagent.utils.f.a aVar = new com.huawei.android.pushagent.utils.f.a(this.in, "PushConnectControl");
            abg(aVar, this.ip, "trsFirstFlowControlData");
            abg(aVar, this.iq, "trsFlowControlData");
            abg(aVar, this.ir, "volumeControlData");
            abg(aVar, this.is, "wifiTrsFirstFlowControlData");
            abg(aVar, this.it, "wifiTrsFlowControlData");
            abg(aVar, this.iu, "wifiVolumeControlData");
            return true;
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.f.c.es("PushLog3413", e.toString(), e);
            return false;
        }
    }

    private boolean abg(com.huawei.android.pushagent.utils.f.a aVar, List<a> list, String str) {
        int i = 0;
        String str2 = "\\|";
        list.clear();
        String ec = aVar.ec(str);
        if (TextUtils.isEmpty(ec)) {
            com.huawei.android.pushagent.utils.f.c.er("PushLog3413", str + " is not set");
        } else {
            com.huawei.android.pushagent.utils.f.c.er("PushLog3413", str + "=" + ec);
            String[] split = ec.split(str2);
            if (split == null || split.length == 0) {
                com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", str + " len 0, maybe system err");
                return false;
            }
            int length = split.length;
            while (i < length) {
                String str3 = split[i];
                b bVar = new b();
                if (bVar.zu(str3)) {
                    list.add(bVar);
                }
                i++;
            }
        }
        return true;
    }

    private boolean abj(com.huawei.android.pushagent.utils.f.a aVar, List<a> list, String str) {
        String str2 = "|";
        StringBuffer stringBuffer = new StringBuffer();
        for (a zt : list) {
            stringBuffer.append(zt.zt()).append(str2);
        }
        if (aVar.ee(str, stringBuffer.toString())) {
            return true;
        }
        com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "save " + str + " failed!!");
        return false;
    }

    private boolean aaw(List<a> list, List<a> list2) {
        if (0 == l.ul(this.in).uv()) {
            if (aau(list, 1)) {
                aat(list, 1);
            } else {
                com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "trsFirstFlowControl not allowed to pass!!");
                return false;
            }
        } else if (aau(list2, 1)) {
            aat(list2, 1);
        } else {
            com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "trsFlowControl not allowed to pass!!");
            return false;
        }
        abi();
        return true;
    }

    private boolean aav(List<a> list) {
        if (aau(list, 1)) {
            aat(list, 1);
            abi();
            return true;
        }
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "volumeControl not allow to pass!!");
        return false;
    }
}
