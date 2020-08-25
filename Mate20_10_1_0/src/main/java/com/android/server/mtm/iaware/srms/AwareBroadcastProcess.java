package com.android.server.mtm.iaware.srms;

import android.content.Intent;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.rms.iaware.AwareLog;
import android.util.ArraySet;
import com.android.server.am.HwBroadcastRecord;
import com.android.server.am.HwMtmBroadcastResourceManager;
import com.android.server.intellicom.common.SmartDualCardConsts;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class AwareBroadcastProcess {
    private static final int IAWARE_FIRST_UNPROXY_DELAY_TIME = 5;
    private static final int IAWARE_PARALLEL_EXCEPTION_LENGTH = 10000;
    private static final int IAWARE_PARALLEL_PROXY_LIST_HIGH_ID = 1;
    private static final int IAWARE_PARALLEL_PROXY_LIST_LOW_ID = 3;
    private static final int IAWARE_PARALLEL_PROXY_LIST_MIDDLE_ID = 2;
    private static final int IAWARE_PARALLEL_PROXY_LIST_NUM = 4;
    private static final int IAWARE_PARALLEL_PROXY_LIST_TOP_ID = 0;
    private static final int IAWARE_UNPROXY_SCREEN_OFF_TIME = 5000;
    private static final int MSG_ENQUEUE_PARALL_BR = 102;
    private static final int MSG_PROCESS_FGAPP_PARALL_BR = 103;
    private static final int MSG_PROCESS_PARALL_BR = 101;
    static final String TAG = "AwareBroadcastProcess";
    private String mBrName = null;
    private final IawareBroadcastHandler mHandler;
    private AwareBroadcastPolicy mIawareBrPolicy = null;
    private final HashMap<Integer, ArrayList<HwBroadcastRecord>> mIawareParallelProxyBrMap = new HashMap<>();
    private int mIawareUnProxyTime = 150;
    private int mProxyKeyBrIndex = -1;
    private boolean mStartUnproxy = false;
    private int mUnproxyHighSpeed = 20;
    private int mUnproxyMaxDuration = 20000;
    private int mUnproxyMaxSpeed = 150;
    private int mUnproxyMiddleSpeed = 40;
    private int mUnproxyMinSpeed = 60;

    public AwareBroadcastProcess(AwareBroadcastPolicy iawareBrPolicy, Handler handler, String name) {
        this.mIawareBrPolicy = iawareBrPolicy;
        this.mHandler = new IawareBroadcastHandler(handler.getLooper());
        this.mBrName = name;
        for (int index = 0; index < 4; index++) {
            this.mIawareParallelProxyBrMap.put(Integer.valueOf(index), new ArrayList<>());
        }
    }

    private final class IawareBroadcastHandler extends Handler {
        public IawareBroadcastHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 101:
                    AwareBroadcastProcess.this.processException();
                    AwareBroadcastProcess.this.unproxyEachBroacast();
                    return;
                case 102:
                    if (msg.obj instanceof HwBroadcastRecord) {
                        AwareBroadcastProcess.this.insertProxyParalledBroadcast((HwBroadcastRecord) msg.obj);
                        return;
                    }
                    return;
                case 103:
                    AwareBroadcastProcess.this.unProxyForegroundAppBroadcast(msg.arg1, msg.arg2);
                    return;
                default:
                    return;
            }
        }
    }

    public void enqueueIawareProxyBroacast(boolean isParallel, HwBroadcastRecord r) {
        if (isParallel) {
            Message msg = this.mHandler.obtainMessage(102, this);
            msg.obj = r;
            this.mHandler.sendMessage(msg);
        }
    }

    public void starUnproxyBroadcast() {
        if (!this.mStartUnproxy && getIawareBrSize() > 0) {
            if (AwareBroadcastDebug.getDebugDetail()) {
                AwareLog.d(TAG, "iaware_br start unproxy, mStartBgUnproxy = true, queue name : " + this.mBrName);
            }
            this.mStartUnproxy = true;
            unProxyBroadcast(5);
        }
    }

    /* access modifiers changed from: private */
    public void insertProxyParalledBroadcast(HwBroadcastRecord r) {
        synchronized (this.mIawareParallelProxyBrMap) {
            int curAdj = r.getReceiverCurAdj();
            if (2 == r.getReceiverCurProcState()) {
                if (!r.isSysApp()) {
                    trimProxyBr(r, 0);
                }
                this.mIawareParallelProxyBrMap.get(0).add(r);
            } else if (curAdj < 0) {
                trimProxyBr(r, 1);
                this.mIawareParallelProxyBrMap.get(1).add(r);
            } else if (curAdj >= 900) {
                trimProxyBr(r, 3);
                this.mIawareParallelProxyBrMap.get(3).add(r);
            } else {
                processAdjChange(r);
                trimProxyBr(r, 2);
                this.mIawareParallelProxyBrMap.get(2).add(r);
            }
        }
    }

    private void unProxyBroadcast(int unproxyTime) {
        if (!this.mHandler.hasMessages(101)) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(101, this), (long) unproxyTime);
        }
    }

    /* access modifiers changed from: private */
    public void unproxyEachBroacast() {
        if (isEmptyProxyMap()) {
            this.mStartUnproxy = false;
            return;
        }
        ArrayList<HwBroadcastRecord> parallelList = new ArrayList<>();
        synchronized (this.mIawareParallelProxyBrMap) {
            HwBroadcastRecord proxyBr = getNextUnProxyBr();
            if (proxyBr != null) {
                parallelList.add(proxyBr);
                if (AwareBroadcastDebug.getDebugDetail()) {
                    AwareLog.i(TAG, "iaware_br unproxy cost time : " + (System.currentTimeMillis() - proxyBr.getDispatchClockTime()) + " : receiver :" + proxyBr.getBrReceivers().get(0) + " : action : " + proxyBr.getAction() + " : processState : " + proxyBr.getReceiverCurProcState() + ": adj : " + proxyBr.getReceiverCurAdj() + ": unproxy speed: " + this.mIawareUnProxyTime + " : size:" + getIawareBrSize());
                }
            }
            if (proxyBr != null && proxyBr.getReceiverCurAdj() <= 0 && this.mProxyKeyBrIndex >= 0) {
                this.mProxyKeyBrIndex--;
            }
        }
        iAwareUnproxyBroadcastList(parallelList);
        parallelList.clear();
        calculateUnproxySpeed();
        unProxyBroadcast(this.mIawareUnProxyTime);
    }

    private void calculateUnproxySpeed() {
        if (!this.mIawareBrPolicy.isScreenOff()) {
            if (!isRestrictTopApp() && isProcessProxyTopList()) {
                if (this.mIawareBrPolicy.isSpeedNoCtrol()) {
                    this.mIawareUnProxyTime = this.mUnproxyHighSpeed;
                } else {
                    this.mIawareUnProxyTime = this.mUnproxyMaxSpeed;
                }
            } else if (isProcessProxyHighAdjList()) {
                this.mIawareUnProxyTime = this.mUnproxyHighSpeed;
            } else if (!this.mIawareBrPolicy.isSpeedNoCtrol()) {
                ArrayList<HwBroadcastRecord> iawareParallelBroadcasts = getProcessingList();
                int length = iawareParallelBroadcasts.size();
                if (length > 0) {
                    int i = this.mProxyKeyBrIndex;
                    if (i < 0) {
                        int minDuration = Integer.MAX_VALUE;
                        for (int i2 = 0; i2 < length; i2++) {
                            int duration = (int) (((iawareParallelBroadcasts.get(i2).getDispatchClockTime() + ((long) this.mUnproxyMaxDuration)) - System.currentTimeMillis()) / ((long) (i2 + 1)));
                            if (duration < minDuration) {
                                minDuration = duration;
                                this.mProxyKeyBrIndex = i2;
                            }
                        }
                        this.mIawareUnProxyTime = minDuration;
                    } else if (i < length - 1) {
                        int minDuration2 = this.mIawareUnProxyTime;
                        for (int i3 = i + 1; i3 < length; i3++) {
                            int duration2 = (int) (((iawareParallelBroadcasts.get(i3).getDispatchClockTime() + ((long) this.mUnproxyMaxDuration)) - System.currentTimeMillis()) / ((long) (i3 + 1)));
                            if (duration2 < minDuration2) {
                                minDuration2 = duration2;
                                this.mProxyKeyBrIndex = i3;
                            }
                        }
                        this.mIawareUnProxyTime = minDuration2;
                    }
                }
                resetUnproxyTime();
            } else if (isProcessProxyMiddleAdjList()) {
                this.mIawareUnProxyTime = this.mUnproxyMiddleSpeed;
            } else {
                this.mIawareUnProxyTime = this.mUnproxyMinSpeed;
            }
        }
    }

    private void resetUnproxyTime() {
        int tempUnProxyTime = this.mIawareUnProxyTime;
        int i = this.mUnproxyMinSpeed;
        if (tempUnProxyTime <= i) {
            tempUnProxyTime = i;
        }
        int i2 = this.mUnproxyMaxSpeed;
        if (i2 >= tempUnProxyTime) {
            i2 = tempUnProxyTime;
        }
        this.mIawareUnProxyTime = i2;
    }

    private void iAwareUnproxyBroadcastList(ArrayList<HwBroadcastRecord> parallelList) {
        if (parallelList.size() > 0) {
            HwMtmBroadcastResourceManager.insertIawareBroadcast(parallelList, this.mBrName);
        }
    }

    public int getIawareBrSize() {
        int length;
        synchronized (this.mIawareParallelProxyBrMap) {
            length = 0;
            for (Map.Entry<Integer, ArrayList<HwBroadcastRecord>> ent : this.mIawareParallelProxyBrMap.entrySet()) {
                length += ent.getValue().size();
            }
        }
        return length;
    }

    public void setUnProxySpeedScreenOff() {
        int size = getIawareBrSize();
        if (size > 0) {
            int speed = IAWARE_UNPROXY_SCREEN_OFF_TIME / size;
            int i = this.mUnproxyMinSpeed;
            if (speed < i) {
                i = speed;
            }
            this.mIawareUnProxyTime = i;
        }
    }

    public void setUnProxyMaxDuration(int duration) {
        this.mUnproxyMaxDuration = duration;
    }

    public void setUnProxyMaxSpeed(int speed) {
        this.mUnproxyMaxSpeed = speed;
    }

    public void setUnProxyMinSpeed(int speed) {
        this.mUnproxyMinSpeed = speed;
    }

    public void setUnProxyMiddleSpeed(int speed) {
        this.mUnproxyMiddleSpeed = speed;
    }

    public void setUnProxyHighSpeed(int speed) {
        this.mUnproxyHighSpeed = speed;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:31:0x0201, code lost:
        r0 = th;
     */
    public void dump(PrintWriter pw) {
        ArrayList<HwBroadcastRecord> iawareTopParallelBroadcasts;
        ArrayList<HwBroadcastRecord> iawareHighAdjParallelBroadcasts;
        ArrayList<HwBroadcastRecord> iawareMiddleAdjParallelBroadcasts;
        ArrayList<HwBroadcastRecord> iawareLowAdjParallelBroadcasts;
        if (pw != null) {
            pw.println("      Proxy broadcast [" + this.mBrName + "]");
            Set<String> proxyActions = new ArraySet<>();
            Set<String> proxyTopPkgs = new ArraySet<>();
            Set<String> proxyHighAdjPkgs = new ArraySet<>();
            Set<String> proxyMiddleAdjPkgs = new ArraySet<>();
            Set<String> proxyLowAdjPkgs = new ArraySet<>();
            synchronized (this.mIawareParallelProxyBrMap) {
                iawareTopParallelBroadcasts = new ArrayList<>(this.mIawareParallelProxyBrMap.get(0));
                iawareHighAdjParallelBroadcasts = new ArrayList<>(this.mIawareParallelProxyBrMap.get(1));
                iawareMiddleAdjParallelBroadcasts = new ArrayList<>(this.mIawareParallelProxyBrMap.get(2));
                iawareLowAdjParallelBroadcasts = new ArrayList<>(this.mIawareParallelProxyBrMap.get(3));
            }
            int lengthTop = iawareTopParallelBroadcasts.size();
            int lengthHighAdj = iawareHighAdjParallelBroadcasts.size();
            int lengthMiddleAdj = iawareMiddleAdjParallelBroadcasts.size();
            int lengthLowAdj = iawareLowAdjParallelBroadcasts.size();
            int listSize = iawareTopParallelBroadcasts.size();
            int index = 0;
            while (index < listSize) {
                HwBroadcastRecord hwBr = iawareTopParallelBroadcasts.get(index);
                proxyActions.add(hwBr.getAction());
                proxyTopPkgs.add(hwBr.getReceiverPkg());
                index++;
                iawareTopParallelBroadcasts = iawareTopParallelBroadcasts;
            }
            int index2 = 0;
            for (int listSize2 = iawareHighAdjParallelBroadcasts.size(); index2 < listSize2; listSize2 = listSize2) {
                HwBroadcastRecord hwBr2 = iawareHighAdjParallelBroadcasts.get(index2);
                proxyActions.add(hwBr2.getAction());
                proxyHighAdjPkgs.add(hwBr2.getReceiverPkg());
                index2++;
            }
            int index3 = 0;
            for (int listSize3 = iawareMiddleAdjParallelBroadcasts.size(); index3 < listSize3; listSize3 = listSize3) {
                HwBroadcastRecord hwBr3 = iawareMiddleAdjParallelBroadcasts.get(index3);
                proxyActions.add(hwBr3.getAction());
                proxyMiddleAdjPkgs.add(hwBr3.getReceiverPkg());
                index3++;
            }
            int index4 = 0;
            for (int listSize4 = iawareLowAdjParallelBroadcasts.size(); index4 < listSize4; listSize4 = listSize4) {
                HwBroadcastRecord hwBr4 = iawareLowAdjParallelBroadcasts.get(index4);
                proxyActions.add(hwBr4.getAction());
                proxyLowAdjPkgs.add(hwBr4.getReceiverPkg());
                index4++;
            }
            pw.println("      proxy action :" + proxyActions);
            pw.println("      proxy Top pkg :" + proxyTopPkgs);
            pw.println("      proxy high adj pkg :" + proxyHighAdjPkgs);
            pw.println("      proxy middle adj pkg :" + proxyMiddleAdjPkgs);
            pw.println("      proxy low adj pkg :" + proxyLowAdjPkgs);
            int length = lengthTop + lengthHighAdj + lengthMiddleAdj + lengthLowAdj;
            if (length == 0) {
                pw.println("      Unproxy speed :0");
            } else {
                pw.println("      Unproxy speed :" + this.mIawareUnProxyTime);
            }
            pw.println("      Proxy broadcast count :" + length + ", Top count:" + lengthTop + ", high adj count:" + lengthHighAdj + ", middle adj count:" + lengthMiddleAdj + ", low adj count:" + lengthLowAdj);
            return;
        }
        return;
        while (true) {
        }
    }

    public void startUnproxyFgAppBroadcast(int pid, int uid) {
        if (getIawareBrSize() != 0) {
            Message msg = this.mHandler.obtainMessage(103, this);
            msg.arg1 = pid;
            msg.arg2 = uid;
            this.mHandler.sendMessage(msg);
        }
    }

    /* access modifiers changed from: private */
    public void unProxyForegroundAppBroadcast(int pid, int uid) {
        if (AwareBroadcastDebug.getDebugDetail()) {
            AwareLog.d(TAG, "iaware_br unProxyForegroundAppBroadcast, uid:" + uid + ", pid: " + pid);
        }
        ArrayList<HwBroadcastRecord> parallelList = this.mIawareParallelProxyBrMap.get(0);
        synchronized (this.mIawareParallelProxyBrMap) {
            String pkgSys = null;
            if (1000 == uid) {
                pkgSys = getSysPkg(pid, uid);
                if (AwareBroadcastDebug.getDebugDetail()) {
                    AwareLog.i(TAG, "iaware_br unProxyForegroundAppBroadcast system app, uid:" + uid + ", pid: " + pid + ": pkg : " + pkgSys);
                }
                if (pkgSys == null) {
                    return;
                }
            }
            for (Map.Entry<Integer, ArrayList<HwBroadcastRecord>> ent : this.mIawareParallelProxyBrMap.entrySet()) {
                if (ent.getKey().intValue() != 0) {
                    iAwareUnproxyBroadcastInner(ent.getValue(), uid, parallelList, pkgSys);
                }
            }
        }
    }

    private void iAwareUnproxyBroadcastInner(ArrayList<HwBroadcastRecord> pendingBroadcasts, int unProxyUid, ArrayList<HwBroadcastRecord> unProxyBroadcasts, String unProxyPkgSys) {
        Iterator it = pendingBroadcasts.iterator();
        while (it.hasNext()) {
            HwBroadcastRecord br = it.next();
            if (unProxyUid != 1000 || unProxyPkgSys == null) {
                if (br.getReceiverUid() == unProxyUid) {
                    unProxyBroadcasts.add(br);
                    it.remove();
                }
            } else if (unProxyPkgSys.equals(br.getReceiverPkg())) {
                unProxyBroadcasts.add(br);
                it.remove();
            }
        }
    }

    private boolean isEmptyProxyMap() {
        for (Map.Entry<Integer, ArrayList<HwBroadcastRecord>> ent : this.mIawareParallelProxyBrMap.entrySet()) {
            if (ent.getValue().size() > 0) {
                return false;
            }
        }
        return true;
    }

    private boolean isProcessProxyTopList() {
        return this.mIawareParallelProxyBrMap.get(0).size() != 0;
    }

    private boolean isProcessProxyHighAdjList() {
        return this.mIawareParallelProxyBrMap.get(1).size() != 0;
    }

    private boolean isProcessProxyMiddleAdjList() {
        return this.mIawareParallelProxyBrMap.get(0).size() == 0 && this.mIawareParallelProxyBrMap.get(1).size() == 0 && this.mIawareParallelProxyBrMap.get(2).size() != 0;
    }

    private HwBroadcastRecord getNextUnProxyBr() {
        for (Map.Entry<Integer, ArrayList<HwBroadcastRecord>> ent : this.mIawareParallelProxyBrMap.entrySet()) {
            ArrayList<HwBroadcastRecord> iawareParallelBrList = ent.getValue();
            if (!isRestrictTopApp() || ent.getKey().intValue() != 0) {
                if (iawareParallelBrList.size() > 0) {
                    return iawareParallelBrList.remove(0);
                }
            } else if (AwareBroadcastDebug.getDebugDetail()) {
                AwareLog.i(TAG, "iaware_br don't unproxy top app's br");
            }
        }
        return null;
    }

    private ArrayList<HwBroadcastRecord> getProcessingList() {
        if (isProcessProxyMiddleAdjList()) {
            return this.mIawareParallelProxyBrMap.get(2);
        }
        return this.mIawareParallelProxyBrMap.get(3);
    }

    private void processAdjChange(HwBroadcastRecord r) {
        String pkg = r.getReceiverPkg();
        if (pkg != null) {
            int pid = r.getReceiverPid();
            ArrayList<HwBroadcastRecord> iawareMiddleBrList = this.mIawareParallelProxyBrMap.get(2);
            Iterator it = this.mIawareParallelProxyBrMap.get(3).iterator();
            while (it.hasNext()) {
                HwBroadcastRecord br = it.next();
                if (br.getReceiverPid() == pid && pkg.equals(br.getReceiverPkg())) {
                    if (AwareBroadcastDebug.getDebugDetail()) {
                        AwareLog.d(TAG, "iaware_br adj change, transfer: " + br.getBrReceivers().get(0));
                    }
                    iawareMiddleBrList.add(br);
                    it.remove();
                }
            }
        }
    }

    private void trimProxyBr(HwBroadcastRecord r, int level) {
        if (this.mIawareBrPolicy.isTrimAction(r.getAction())) {
            Iterator it = this.mIawareParallelProxyBrMap.get(Integer.valueOf(level)).iterator();
            while (it.hasNext()) {
                HwBroadcastRecord br = it.next();
                if (canTrim(r, br)) {
                    if (AwareBroadcastDebug.getDebugDetail()) {
                        AwareLog.d(TAG, "iaware_br brtrim success: " + br.getBrReceivers().get(0) + ": action: " + r.getAction());
                    }
                    it.remove();
                    return;
                }
            }
        }
    }

    private boolean canTrim(HwBroadcastRecord newBr, HwBroadcastRecord oldBr) {
        String pkg1 = newBr.getReceiverPkg();
        String pkg2 = oldBr.getReceiverPkg();
        if (pkg1 == null || pkg2 == null || !pkg1.equals(pkg2) || !newBr.isSameReceiver(oldBr)) {
            return false;
        }
        String action1 = newBr.getAction();
        String action2 = oldBr.getAction();
        if (action1 == null || action2 == null || !action1.equals(action2)) {
            return false;
        }
        if (SmartDualCardConsts.ACTION_CONNECTIVITY_CHANGE.equals(action1)) {
            return canConnectivityDataTrim(newBr.getIntent(), oldBr.getIntent());
        }
        if (SmartDualCardConsts.SYSTEM_STATE_NAME_WIFI_NETWORK_STATE_CHANGED.equals(action1)) {
            return canWifiDataTrim(newBr.getIntent(), oldBr.getIntent());
        }
        return true;
    }

    private boolean canConnectivityDataTrim(Intent newIntent, Intent oldIntent) {
        int newType = newIntent.getIntExtra("networkType", -1);
        if (newType != oldIntent.getIntExtra("networkType", -1)) {
            return false;
        }
        if (newType == 0 || newType == 1) {
            NetworkInfo newInfo = null;
            NetworkInfo oldInfo = null;
            try {
                newInfo = (NetworkInfo) newIntent.getExtra("networkInfo");
                oldInfo = (NetworkInfo) oldIntent.getExtra("networkInfo");
            } catch (ClassCastException e) {
                if (AwareBroadcastDebug.getDebugDetail()) {
                    AwareLog.e(TAG, "iaware_br get NetworkInfo from intent error.");
                }
            }
            if (newInfo == null || oldInfo == null || newInfo.getState() != oldInfo.getState()) {
                return false;
            }
            return true;
        }
        return false;
    }

    private boolean canWifiDataTrim(Intent newIntent, Intent oldIntent) {
        NetworkInfo newNetworkInfo = (NetworkInfo) newIntent.getParcelableExtra("networkInfo");
        NetworkInfo oldNetworkInfo = (NetworkInfo) oldIntent.getParcelableExtra("networkInfo");
        if (newNetworkInfo == null || oldNetworkInfo == null || newNetworkInfo.getState() != oldNetworkInfo.getState() || newNetworkInfo.getDetailedState() != oldNetworkInfo.getDetailedState()) {
            return false;
        }
        return true;
    }

    private String getSysPkg(int pid, int uid) {
        for (Map.Entry<Integer, ArrayList<HwBroadcastRecord>> ent : this.mIawareParallelProxyBrMap.entrySet()) {
            ArrayList<HwBroadcastRecord> iawareBrList = ent.getValue();
            int listSize = iawareBrList.size();
            int index = 0;
            while (true) {
                if (index < listSize) {
                    HwBroadcastRecord hwBr = iawareBrList.get(index);
                    if (hwBr.getReceiverPid() == pid && hwBr.getReceiverUid() == uid) {
                        return hwBr.getReceiverPkg();
                    }
                    index++;
                }
            }
        }
        return null;
    }

    private boolean isRestrictTopApp() {
        return this.mIawareBrPolicy.isInstallApp() && !this.mIawareBrPolicy.isSpeedNoCtrol() && isProcessProxyTopList();
    }

    /* access modifiers changed from: private */
    public void processException() {
        synchronized (this.mIawareParallelProxyBrMap) {
            int length = 0;
            for (Map.Entry<Integer, ArrayList<HwBroadcastRecord>> ent : this.mIawareParallelProxyBrMap.entrySet()) {
                length += ent.getValue().size();
            }
            if (length > 10000) {
                AwareLog.w(TAG, "iaware_br proxy length more than " + length + ", clear all proxy br");
                for (Map.Entry<Integer, ArrayList<HwBroadcastRecord>> ent2 : this.mIawareParallelProxyBrMap.entrySet()) {
                    ent2.getValue().clear();
                }
            }
        }
    }
}
