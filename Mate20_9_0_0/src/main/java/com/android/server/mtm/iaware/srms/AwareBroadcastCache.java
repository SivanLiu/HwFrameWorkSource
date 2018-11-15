package com.android.server.mtm.iaware.srms;

import android.rms.iaware.AwareLog;
import com.android.server.am.HwBroadcastRecord;
import com.android.server.am.HwMtmBroadcastResourceManager;
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.huawei.pgmng.common.Utils;
import com.huawei.pgmng.log.LogPower;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class AwareBroadcastCache {
    private static final int AWARE_MAX_PROXY_BROADCAST = 60;
    static final String TAG = "AwareBroadcastCache";
    private Object mAms;
    final HashMap<Integer, ArrayList<HwBroadcastRecord>> mAwareOrderedPendingBrMap = new HashMap();
    final HashMap<Integer, ArrayList<HwBroadcastRecord>> mAwareParallelPendingBrMap = new HashMap();
    private String mBrName = null;
    final HashMap<Integer, Integer> mProxyPidsCount = new HashMap();
    HashMap<String, String> mSameKindsActionList = new HashMap();

    public AwareBroadcastCache(String name, Object ams) {
        this.mBrName = name;
        this.mAms = ams;
        this.mSameKindsActionList.put("android.intent.action.SCREEN_ON", "android.intent.action.SCREEN_OFF");
    }

    public boolean awareTrimAndEnqueueBr(boolean isParallel, HwBroadcastRecord r, boolean notify, int pid, String pkgName, AwareBroadcastPolicy policy) {
        if (r == null || pkgName == null) {
            return false;
        }
        boolean trim = false;
        notifyPgUnFreeze(r, notify, pid, pkgName);
        int count = 0;
        if (this.mProxyPidsCount.containsKey(Integer.valueOf(pid))) {
            count = ((Integer) this.mProxyPidsCount.get(Integer.valueOf(pid))).intValue();
        }
        ArrayList<HwBroadcastRecord> awareParallelPendingBroadcasts;
        Iterator it;
        HwBroadcastRecord br;
        if (isParallel) {
            if (this.mAwareParallelPendingBrMap.containsKey(Integer.valueOf(pid))) {
                awareParallelPendingBroadcasts = (ArrayList) this.mAwareParallelPendingBrMap.get(Integer.valueOf(pid));
            } else {
                awareParallelPendingBroadcasts = new ArrayList();
                this.mAwareParallelPendingBrMap.put(Integer.valueOf(pid), awareParallelPendingBroadcasts);
            }
            it = awareParallelPendingBroadcasts.iterator();
            while (it.hasNext()) {
                br = (HwBroadcastRecord) it.next();
                if (!notify && canTrim(r, br)) {
                    it.remove();
                    count--;
                    AwareBroadcastDumpRadar.increatBrAfterCount(1);
                    trim = true;
                    break;
                }
            }
            awareParallelPendingBroadcasts.add(r);
        } else {
            if (this.mAwareOrderedPendingBrMap.containsKey(Integer.valueOf(pid))) {
                awareParallelPendingBroadcasts = (ArrayList) this.mAwareOrderedPendingBrMap.get(Integer.valueOf(pid));
            } else {
                awareParallelPendingBroadcasts = new ArrayList();
                this.mAwareOrderedPendingBrMap.put(Integer.valueOf(pid), awareParallelPendingBroadcasts);
            }
            it = awareParallelPendingBroadcasts.iterator();
            while (it.hasNext()) {
                br = (HwBroadcastRecord) it.next();
                if (!notify && canTrim(r, br)) {
                    it.remove();
                    count--;
                    AwareBroadcastDumpRadar.increatBrAfterCount(1);
                    trim = true;
                    break;
                }
            }
            awareParallelPendingBroadcasts.add(r);
        }
        count++;
        this.mProxyPidsCount.put(Integer.valueOf(pid), Integer.valueOf(count));
        if (AwareBroadcastDebug.getFilterDebug()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("trim and enqueue ");
            stringBuilder.append(this.mBrName);
            stringBuilder.append(" pid:");
            stringBuilder.append(pid);
            stringBuilder.append(" count:");
            stringBuilder.append(count);
            stringBuilder.append(")(");
            stringBuilder.append(r);
            stringBuilder.append(")");
            AwareLog.i(str, stringBuilder.toString());
        }
        notifyPgOverFlowUnFreeze(pid, pkgName, count, policy);
        return trim;
    }

    private void notifyPgUnFreeze(HwBroadcastRecord r, boolean notify, int pid, String pkgName) {
        if (notify) {
            LogPower.push(CPUFeature.MSG_EXIT_GAME_SCENE, r.getAction(), pkgName, String.valueOf(pid), new String[]{r.getCallerPackage()});
            if (AwareBroadcastDebug.getFilterDebug()) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("enqueueProxyBroadcast notify pg broadcast:");
                stringBuilder.append(r.getAction());
                stringBuilder.append(" pkg:");
                stringBuilder.append(pkgName);
                stringBuilder.append(" pid:");
                stringBuilder.append(pid);
                AwareLog.i(str, stringBuilder.toString());
            }
        }
    }

    private void notifyPgOverFlowUnFreeze(int pid, String pkgName, int count, AwareBroadcastPolicy policy) {
        if (count >= 60 && policy != null) {
            policy.notifyOverFlow(pid);
        }
    }

    private boolean canTrim(HwBroadcastRecord r1, HwBroadcastRecord r2) {
        if (r1 == null || r2 == null || r1.getIntent() == null || r2.getIntent() == null || r1.getAction() == null || r2.getAction() == null) {
            return false;
        }
        Object o1 = r1.getBrReceivers().get(0);
        Object o2 = r2.getBrReceivers().get(0);
        String pkg1 = r1.getReceiverPkg();
        String pkg2 = r2.getReceiverPkg();
        if ((pkg1 != null && !pkg1.equals(pkg2)) || !HwMtmBroadcastResourceManager.isSameReceiver(o1, o2)) {
            return false;
        }
        String action1 = r1.getAction();
        String action2 = r2.getAction();
        if (action1.equals(action2)) {
            return true;
        }
        String a1 = (String) this.mSameKindsActionList.get(action1);
        String a2 = (String) this.mSameKindsActionList.get(action2);
        if ((a1 == null || !a1.equals(action2)) && (a2 == null || !a2.equals(action1))) {
            return false;
        }
        return true;
    }

    private void notifyPG(String action, String pkg, int pid) {
        Utils.handleTimeOut(action, pkg, String.valueOf(pid));
    }

    public void unproxyCacheBr(int pid) {
        synchronized (this.mAms) {
            ArrayList<HwBroadcastRecord> parallelBrs = (ArrayList) this.mAwareParallelPendingBrMap.get(Integer.valueOf(pid));
            ArrayList<HwBroadcastRecord> orderedBrs = (ArrayList) this.mAwareOrderedPendingBrMap.get(Integer.valueOf(pid));
            if (parallelBrs == null && orderedBrs == null) {
                return;
            }
            ArrayList<HwBroadcastRecord> awareParallelBrs = new ArrayList();
            ArrayList<HwBroadcastRecord> awareOrderedBrs = new ArrayList();
            if (parallelBrs != null) {
                awareParallelBrs.addAll(parallelBrs);
            }
            if (orderedBrs != null) {
                awareOrderedBrs.addAll(orderedBrs);
            }
            this.mAwareParallelPendingBrMap.remove(Integer.valueOf(pid));
            this.mAwareOrderedPendingBrMap.remove(Integer.valueOf(pid));
            this.mProxyPidsCount.remove(Integer.valueOf(pid));
            HwMtmBroadcastResourceManager.unProxyCachedBr(awareParallelBrs, awareOrderedBrs);
        }
    }

    public void clearCacheBr(int pid) {
        synchronized (this.mAms) {
            this.mAwareParallelPendingBrMap.remove(Integer.valueOf(pid));
            this.mAwareOrderedPendingBrMap.remove(Integer.valueOf(pid));
            this.mProxyPidsCount.remove(Integer.valueOf(pid));
        }
    }
}
