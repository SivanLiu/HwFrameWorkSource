package com.huawei.android.pushagent.model.flowcontrol;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.model.flowcontrol.a.b;
import com.huawei.android.pushagent.model.prefs.e;
import com.huawei.android.pushagent.utils.d;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

public class a {
    private static a eg = null;
    private Context ef = null;
    private List<com.huawei.android.pushagent.model.flowcontrol.a.a> eh = new LinkedList();
    private List<com.huawei.android.pushagent.model.flowcontrol.a.a> ei = new LinkedList();
    private List<com.huawei.android.pushagent.model.flowcontrol.a.a> ej = new LinkedList();
    private List<com.huawei.android.pushagent.model.flowcontrol.a.a> ek = new LinkedList();
    private List<com.huawei.android.pushagent.model.flowcontrol.a.a> el = new LinkedList();
    private List<com.huawei.android.pushagent.model.flowcontrol.a.a> em = new LinkedList();

    private a(Context context) {
        this.ef = context;
        nv();
        if (this.eh.size() == 0 && this.ei.size() == 0 && this.ej.size() == 0 && this.ek.size() == 0 && this.el.size() == 0 && this.em.size() == 0) {
            com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "Connect Control is not set, begin to config it");
            nr();
        }
    }

    public static synchronized boolean nk(Context context) {
        synchronized (a.class) {
            eg = ns(context);
            if (eg == null) {
                com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "cannot get ConnectControlMgr instance, may be system err!!");
                return false;
            }
            boolean nq = eg.nq();
            return nq;
        }
    }

    public static synchronized boolean ni(Context context, int i) {
        synchronized (a.class) {
            eg = ns(context);
            if (eg == null) {
                com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "cannot get ConnectControlMgr instance, may be system err!!");
                return false;
            }
            boolean np = eg.np(i);
            return np;
        }
    }

    public static synchronized a ns(Context context) {
        a aVar;
        synchronized (a.class) {
            if (eg == null) {
                eg = new a(context);
            }
            aVar = eg;
        }
        return aVar;
    }

    public static void nj(Context context) {
        eg = ns(context);
        if (eg != null && (eg.nu() ^ 1) != 0) {
            com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "TRS cfg change, need reload");
            eg.nr();
        }
    }

    private boolean nt(List<com.huawei.android.pushagent.model.flowcontrol.a.a> list, List<com.huawei.android.pushagent.model.flowcontrol.a.a> list2) {
        if (list == null && list2 == null) {
            return true;
        }
        if (list == null || list2 == null || list.size() != list2.size()) {
            return false;
        }
        for (com.huawei.android.pushagent.model.flowcontrol.a.a aVar : list) {
            boolean z;
            for (com.huawei.android.pushagent.model.flowcontrol.a.a nf : list2) {
                if (aVar.nf(nf)) {
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

    private boolean nu() {
        LinkedList linkedList = new LinkedList();
        linkedList.add(new b(86400000, com.huawei.android.pushagent.model.prefs.a.ff(this.ef).gg()));
        linkedList.add(new b(3600000, com.huawei.android.pushagent.model.prefs.a.ff(this.ef).gh()));
        if (nt(linkedList, this.eh)) {
            linkedList = new LinkedList();
            linkedList.add(new b(86400000, com.huawei.android.pushagent.model.prefs.a.ff(this.ef).gr()));
            if (nt(linkedList, this.ei)) {
                LinkedList linkedList2 = new LinkedList();
                for (Entry entry : com.huawei.android.pushagent.model.prefs.a.ff(this.ef).gi().entrySet()) {
                    linkedList2.add(new b(((Long) entry.getKey()).longValue() * 1000, ((Long) entry.getValue()).longValue()));
                }
                if (nt(linkedList2, this.ej)) {
                    linkedList = new LinkedList();
                    linkedList.add(new b(86400000, com.huawei.android.pushagent.model.prefs.a.ff(this.ef).hz()));
                    linkedList.add(new b(3600000, com.huawei.android.pushagent.model.prefs.a.ff(this.ef).ia()));
                    if (nt(linkedList, this.ek)) {
                        linkedList = new LinkedList();
                        linkedList.add(new b(86400000, com.huawei.android.pushagent.model.prefs.a.ff(this.ef).ic()));
                        if (nt(linkedList, this.el)) {
                            linkedList2 = new LinkedList();
                            for (Entry entry2 : com.huawei.android.pushagent.model.prefs.a.ff(this.ef).ib().entrySet()) {
                                linkedList2.add(new b(((Long) entry2.getKey()).longValue() * 1000, ((Long) entry2.getValue()).longValue()));
                            }
                            if (nt(linkedList2, this.em)) {
                                com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "cur control is equal trs cfg");
                                return true;
                            }
                            com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "wifiVolumeControl cfg is change!!");
                            return false;
                        }
                        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "wifiTrsFlowControl cfg is change!!");
                        return false;
                    }
                    com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "wifiTrsFirstFlowControl cfg is change!");
                    return false;
                }
                com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "flowcControl cfg is change!!");
                return false;
            }
            com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "trsFlowControl cfg is change!!");
            return false;
        }
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "trsFirstFlowControl cfg is change!");
        return false;
    }

    private boolean nr() {
        this.eh.clear();
        this.eh.add(new b(86400000, com.huawei.android.pushagent.model.prefs.a.ff(this.ef).gg()));
        this.eh.add(new b(3600000, com.huawei.android.pushagent.model.prefs.a.ff(this.ef).gh()));
        this.ei.clear();
        this.ei.add(new b(86400000, com.huawei.android.pushagent.model.prefs.a.ff(this.ef).gr()));
        this.ej.clear();
        for (Entry entry : com.huawei.android.pushagent.model.prefs.a.ff(this.ef).gi().entrySet()) {
            this.ej.add(new b(((Long) entry.getKey()).longValue() * 1000, ((Long) entry.getValue()).longValue()));
        }
        this.ek.clear();
        this.ek.add(new b(86400000, com.huawei.android.pushagent.model.prefs.a.ff(this.ef).hz()));
        this.ek.add(new b(3600000, com.huawei.android.pushagent.model.prefs.a.ff(this.ef).ia()));
        this.el.clear();
        this.el.add(new b(86400000, com.huawei.android.pushagent.model.prefs.a.ff(this.ef).ic()));
        this.em.clear();
        for (Entry entry2 : com.huawei.android.pushagent.model.prefs.a.ff(this.ef).ib().entrySet()) {
            this.em.add(new b(((Long) entry2.getKey()).longValue() * 1000, ((Long) entry2.getValue()).longValue()));
        }
        nx();
        return true;
    }

    private boolean nq() {
        if (1 == d.yh(this.ef)) {
            return no(this.ek, this.el);
        }
        return no(this.eh, this.ei);
    }

    private boolean np(int i) {
        if (1 == d.yh(this.ef)) {
            return nn(this.em);
        }
        return nn(this.ej);
    }

    private boolean nm(List<com.huawei.android.pushagent.model.flowcontrol.a.a> list, long j) {
        if (list == null || list.size() == 0) {
            com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "there is no volome control");
            return true;
        }
        for (com.huawei.android.pushagent.model.flowcontrol.a.a ne : list) {
            if (!ne.ne(j)) {
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "push connect time exceed volum");
                return false;
            }
        }
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "check control flow pass");
        return true;
    }

    /* JADX WARNING: Missing block: B:7:0x000c, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized boolean nl(List<com.huawei.android.pushagent.model.flowcontrol.a.a> list, long j) {
        if (list != null) {
            if (list.size() != 0) {
                for (com.huawei.android.pushagent.model.flowcontrol.a.a aVar : list) {
                    if (!aVar.nd(j)) {
                        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", " control info:" + aVar);
                        return false;
                    }
                }
                return true;
            }
        }
    }

    private boolean nx() {
        try {
            com.huawei.android.pushagent.utils.b.b bVar = new com.huawei.android.pushagent.utils.b.b(this.ef, "PushConnectControl");
            if (ny(bVar, this.ek, "wifiTrsFirstFlowControlData") && ny(bVar, this.el, "wifiTrsFlowControlData") && ny(bVar, this.em, "wifiVolumeControlData") && ny(bVar, this.eh, "trsFirstFlowControlData") && ny(bVar, this.ei, "trsFlowControlData") && ny(bVar, this.ej, "volumeControlData")) {
                return true;
            }
            return false;
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", e.toString(), e);
            return false;
        }
    }

    private boolean nv() {
        try {
            com.huawei.android.pushagent.utils.b.b bVar = new com.huawei.android.pushagent.utils.b.b(this.ef, "PushConnectControl");
            nw(bVar, this.eh, "trsFirstFlowControlData");
            nw(bVar, this.ei, "trsFlowControlData");
            nw(bVar, this.ej, "volumeControlData");
            nw(bVar, this.ek, "wifiTrsFirstFlowControlData");
            nw(bVar, this.el, "wifiTrsFlowControlData");
            nw(bVar, this.em, "wifiVolumeControlData");
            return true;
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", e.toString(), e);
            return false;
        }
    }

    private boolean nw(com.huawei.android.pushagent.utils.b.b bVar, List<com.huawei.android.pushagent.model.flowcontrol.a.a> list, String str) {
        int i = 0;
        String str2 = "\\|";
        list.clear();
        String tg = bVar.tg(str);
        if (TextUtils.isEmpty(tg)) {
            com.huawei.android.pushagent.utils.b.a.st("PushLog3414", str + " is not set");
        } else {
            com.huawei.android.pushagent.utils.b.a.st("PushLog3414", str + "=" + tg);
            String[] split = tg.split(str2);
            if (split == null || split.length == 0) {
                com.huawei.android.pushagent.utils.b.a.su("PushLog3414", str + " len 0, maybe system err");
                return false;
            }
            int length = split.length;
            while (i < length) {
                String str3 = split[i];
                b bVar2 = new b();
                if (bVar2.nh(str3)) {
                    list.add(bVar2);
                }
                i++;
            }
        }
        return true;
    }

    private boolean ny(com.huawei.android.pushagent.utils.b.b bVar, List<com.huawei.android.pushagent.model.flowcontrol.a.a> list, String str) {
        String str2 = "|";
        StringBuffer stringBuffer = new StringBuffer();
        for (com.huawei.android.pushagent.model.flowcontrol.a.a ng : list) {
            stringBuffer.append(ng.ng()).append(str2);
        }
        if (bVar.tm(str, stringBuffer.toString())) {
            return true;
        }
        com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "save " + str + " failed!!");
        return false;
    }

    private boolean no(List<com.huawei.android.pushagent.model.flowcontrol.a.a> list, List<com.huawei.android.pushagent.model.flowcontrol.a.a> list2) {
        if (0 == e.jj(this.ef).jp()) {
            if (nm(list, 1)) {
                nl(list, 1);
            } else {
                com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "trsFirstFlowControl not allowed to pass!!");
                return false;
            }
        } else if (nm(list2, 1)) {
            nl(list2, 1);
        } else {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "trsFlowControl not allowed to pass!!");
            return false;
        }
        nx();
        return true;
    }

    private boolean nn(List<com.huawei.android.pushagent.model.flowcontrol.a.a> list) {
        if (nm(list, 1)) {
            nl(list, 1);
            nx();
            return true;
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "volumeControl not allow to pass!!");
        return false;
    }
}
