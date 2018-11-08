package com.huawei.android.pushagent.model.flowcontrol;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.a.c;
import com.huawei.android.pushagent.utils.f;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

public class a {
    private static a bj = null;
    private Context bi = null;
    private List<com.huawei.android.pushagent.model.flowcontrol.a.a> bk = new LinkedList();
    private List<com.huawei.android.pushagent.model.flowcontrol.a.a> bl = new LinkedList();
    private List<com.huawei.android.pushagent.model.flowcontrol.a.a> bm = new LinkedList();
    private List<com.huawei.android.pushagent.model.flowcontrol.a.a> bn = new LinkedList();
    private List<com.huawei.android.pushagent.model.flowcontrol.a.a> bo = new LinkedList();
    private List<com.huawei.android.pushagent.model.flowcontrol.a.a> bp = new LinkedList();

    private a(Context context) {
        this.bi = context;
        hu();
        if (this.bk.size() == 0 && this.bl.size() == 0 && this.bm.size() == 0 && this.bn.size() == 0 && this.bo.size() == 0 && this.bp.size() == 0) {
            b.x("PushLog2976", "Connect Control is not set, begin to config it");
            hq();
        }
    }

    public static synchronized boolean hp(Context context) {
        synchronized (a.class) {
            bj = hr(context);
            if (bj == null) {
                b.y("PushLog2976", "cannot get ConnectControlMgr instance, may be system err!!");
                return false;
            }
            boolean ho = bj.ho();
            return ho;
        }
    }

    public static synchronized boolean hn(Context context, int i) {
        synchronized (a.class) {
            bj = hr(context);
            if (bj == null) {
                b.y("PushLog2976", "cannot get ConnectControlMgr instance, may be system err!!");
                return false;
            }
            boolean hm = bj.hm(i);
            return hm;
        }
    }

    public static synchronized a hr(Context context) {
        a aVar;
        synchronized (a.class) {
            if (bj == null) {
                bj = new a(context);
            }
            aVar = bj;
        }
        return aVar;
    }

    public static void hw(Context context) {
        bj = hr(context);
        if (bj != null && (bj.ht() ^ 1) != 0) {
            b.x("PushLog2976", "TRS cfg change, need reload");
            bj.hq();
        }
    }

    private boolean hs(List<com.huawei.android.pushagent.model.flowcontrol.a.a> list, List<com.huawei.android.pushagent.model.flowcontrol.a.a> list2) {
        if (list == null && list2 == null) {
            return true;
        }
        if (list == null || list2 == null || list.size() != list2.size()) {
            return false;
        }
        for (com.huawei.android.pushagent.model.flowcontrol.a.a aVar : list) {
            boolean z;
            for (com.huawei.android.pushagent.model.flowcontrol.a.a hd : list2) {
                if (aVar.hd(hd)) {
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

    private boolean ht() {
        List linkedList = new LinkedList();
        linkedList.add(new com.huawei.android.pushagent.model.flowcontrol.a.b(86400000, i.mj(this.bi).mk()));
        linkedList.add(new com.huawei.android.pushagent.model.flowcontrol.a.b(3600000, i.mj(this.bi).ml()));
        if (hs(linkedList, this.bk)) {
            linkedList = new LinkedList();
            linkedList.add(new com.huawei.android.pushagent.model.flowcontrol.a.b(86400000, i.mj(this.bi).mm()));
            if (hs(linkedList, this.bl)) {
                List linkedList2 = new LinkedList();
                for (Entry entry : i.mj(this.bi).mn().entrySet()) {
                    linkedList2.add(new com.huawei.android.pushagent.model.flowcontrol.a.b(((Long) entry.getKey()).longValue() * 1000, ((Long) entry.getValue()).longValue()));
                }
                if (hs(linkedList2, this.bm)) {
                    linkedList = new LinkedList();
                    linkedList.add(new com.huawei.android.pushagent.model.flowcontrol.a.b(86400000, i.mj(this.bi).mo()));
                    linkedList.add(new com.huawei.android.pushagent.model.flowcontrol.a.b(3600000, i.mj(this.bi).mp()));
                    if (hs(linkedList, this.bn)) {
                        linkedList = new LinkedList();
                        linkedList.add(new com.huawei.android.pushagent.model.flowcontrol.a.b(86400000, i.mj(this.bi).mq()));
                        if (hs(linkedList, this.bo)) {
                            linkedList2 = new LinkedList();
                            for (Entry entry2 : i.mj(this.bi).mr().entrySet()) {
                                linkedList2.add(new com.huawei.android.pushagent.model.flowcontrol.a.b(((Long) entry2.getKey()).longValue() * 1000, ((Long) entry2.getValue()).longValue()));
                            }
                            if (hs(linkedList2, this.bp)) {
                                b.x("PushLog2976", "cur control is equal trs cfg");
                                return true;
                            }
                            b.x("PushLog2976", "wifiVolumeControl cfg is change!!");
                            return false;
                        }
                        b.x("PushLog2976", "wifiTrsFlowControl cfg is change!!");
                        return false;
                    }
                    b.x("PushLog2976", "wifiTrsFirstFlowControl cfg is change!");
                    return false;
                }
                b.x("PushLog2976", "flowcControl cfg is change!!");
                return false;
            }
            b.x("PushLog2976", "trsFlowControl cfg is change!!");
            return false;
        }
        b.x("PushLog2976", "trsFirstFlowControl cfg is change!");
        return false;
    }

    private boolean hq() {
        this.bk.clear();
        this.bk.add(new com.huawei.android.pushagent.model.flowcontrol.a.b(86400000, i.mj(this.bi).mk()));
        this.bk.add(new com.huawei.android.pushagent.model.flowcontrol.a.b(3600000, i.mj(this.bi).ml()));
        this.bl.clear();
        this.bl.add(new com.huawei.android.pushagent.model.flowcontrol.a.b(86400000, i.mj(this.bi).mm()));
        this.bm.clear();
        for (Entry entry : i.mj(this.bi).mn().entrySet()) {
            this.bm.add(new com.huawei.android.pushagent.model.flowcontrol.a.b(((Long) entry.getKey()).longValue() * 1000, ((Long) entry.getValue()).longValue()));
        }
        this.bn.clear();
        this.bn.add(new com.huawei.android.pushagent.model.flowcontrol.a.b(86400000, i.mj(this.bi).mo()));
        this.bn.add(new com.huawei.android.pushagent.model.flowcontrol.a.b(3600000, i.mj(this.bi).mp()));
        this.bo.clear();
        this.bo.add(new com.huawei.android.pushagent.model.flowcontrol.a.b(86400000, i.mj(this.bi).mq()));
        this.bp.clear();
        for (Entry entry2 : i.mj(this.bi).mr().entrySet()) {
            this.bp.add(new com.huawei.android.pushagent.model.flowcontrol.a.b(((Long) entry2.getKey()).longValue() * 1000, ((Long) entry2.getValue()).longValue()));
        }
        hx();
        return true;
    }

    private boolean ho() {
        if (1 == f.fp(this.bi)) {
            return hl(this.bn, this.bo);
        }
        return hl(this.bk, this.bl);
    }

    private boolean hm(int i) {
        if (1 == f.fp(this.bi)) {
            return hk(this.bp);
        }
        return hk(this.bm);
    }

    private boolean hj(List<com.huawei.android.pushagent.model.flowcontrol.a.a> list, long j) {
        if (list == null || list.size() == 0) {
            b.x("PushLog2976", "there is no volome control");
            return true;
        }
        for (com.huawei.android.pushagent.model.flowcontrol.a.a he : list) {
            if (!he.he(j)) {
                b.z("PushLog2976", "push connect time exceed volum");
                return false;
            }
        }
        b.x("PushLog2976", "check control flow pass");
        return true;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized boolean hi(List<com.huawei.android.pushagent.model.flowcontrol.a.a> list, long j) {
        if (list != null) {
            if (list.size() != 0) {
                for (com.huawei.android.pushagent.model.flowcontrol.a.a aVar : list) {
                    if (!aVar.hf(j)) {
                        b.z("PushLog2976", " control info:" + aVar);
                        return false;
                    }
                }
                return true;
            }
        }
    }

    private boolean hx() {
        try {
            c cVar = new c(this.bi, "PushConnectControl");
            if (hy(cVar, this.bn, "wifiTrsFirstFlowControlData") && hy(cVar, this.bo, "wifiTrsFlowControlData") && hy(cVar, this.bp, "wifiVolumeControlData") && hy(cVar, this.bk, "trsFirstFlowControlData") && hy(cVar, this.bl, "trsFlowControlData") && hy(cVar, this.bm, "volumeControlData")) {
                return true;
            }
            return false;
        } catch (Throwable e) {
            b.aa("PushLog2976", e.toString(), e);
            return false;
        }
    }

    private boolean hu() {
        try {
            c cVar = new c(this.bi, "PushConnectControl");
            hv(cVar, this.bk, "trsFirstFlowControlData");
            hv(cVar, this.bl, "trsFlowControlData");
            hv(cVar, this.bm, "volumeControlData");
            hv(cVar, this.bn, "wifiTrsFirstFlowControlData");
            hv(cVar, this.bo, "wifiTrsFlowControlData");
            hv(cVar, this.bp, "wifiVolumeControlData");
            return true;
        } catch (Throwable e) {
            b.aa("PushLog2976", e.toString(), e);
            return false;
        }
    }

    private boolean hv(c cVar, List<com.huawei.android.pushagent.model.flowcontrol.a.a> list, String str) {
        int i = 0;
        String str2 = "\\|";
        list.clear();
        String aj = cVar.aj(str);
        if (TextUtils.isEmpty(aj)) {
            b.x("PushLog2976", str + " is not set");
        } else {
            b.x("PushLog2976", str + "=" + aj);
            String[] split = aj.split(str2);
            if (split == null || split.length == 0) {
                b.y("PushLog2976", str + " len 0, maybe system err");
                return false;
            }
            int length = split.length;
            while (i < length) {
                String str3 = split[i];
                com.huawei.android.pushagent.model.flowcontrol.a.b bVar = new com.huawei.android.pushagent.model.flowcontrol.a.b();
                if (bVar.hh(str3)) {
                    list.add(bVar);
                }
                i++;
            }
        }
        return true;
    }

    private boolean hy(c cVar, List<com.huawei.android.pushagent.model.flowcontrol.a.a> list, String str) {
        String str2 = "|";
        StringBuffer stringBuffer = new StringBuffer();
        for (com.huawei.android.pushagent.model.flowcontrol.a.a hg : list) {
            stringBuffer.append(hg.hg()).append(str2);
        }
        if (cVar.ak(str, stringBuffer.toString())) {
            return true;
        }
        b.y("PushLog2976", "save " + str + " failed!!");
        return false;
    }

    private boolean hl(List<com.huawei.android.pushagent.model.flowcontrol.a.a> list, List<com.huawei.android.pushagent.model.flowcontrol.a.a> list2) {
        if (0 == k.pt(this.bi).pu()) {
            if (hj(list, 1)) {
                hi(list, 1);
            } else {
                b.y("PushLog2976", "trsFirstFlowControl not allowed to pass!!");
                return false;
            }
        } else if (hj(list2, 1)) {
            hi(list2, 1);
        } else {
            b.y("PushLog2976", "trsFlowControl not allowed to pass!!");
            return false;
        }
        hx();
        return true;
    }

    private boolean hk(List<com.huawei.android.pushagent.model.flowcontrol.a.a> list) {
        if (hj(list, 1)) {
            hi(list, 1);
            hx();
            return true;
        }
        b.z("PushLog2976", "volumeControl not allow to pass!!");
        return false;
    }
}
