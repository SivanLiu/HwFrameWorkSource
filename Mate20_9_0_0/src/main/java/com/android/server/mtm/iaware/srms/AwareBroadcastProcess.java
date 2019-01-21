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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

public class AwareBroadcastProcess {
    private static final int IAWARE_FIRST_UNPROXY_DELAY_TIME = 5;
    private static final int IAWARE_PARALLEL_EXCEPTION_LENGTH = 10000;
    private static final int IAWARE_PARALLEL_PROXY_LIST_HIGH_ID = 1;
    private static final int IAWARE_PARALLEL_PROXY_LIST_LOW_ID = 3;
    private static final int IAWARE_PARALLEL_PROXY_LIST_MIDDLE_ID = 2;
    private static final int IAWARE_PARALLEL_PROXY_LIST_NUM = 4;
    private static final int IAWARE_PARALLEL_PROXY_LIST_TOP_ID = 0;
    private static final int IAWARE_PROXY_QUEUE_MIN_LENGTH = 10;
    private static final int IAWARE_UNPROXY_SCREEN_OFF_TIME = 5000;
    private static final int MSG_ENQUEUE_PARALL_BR = 102;
    private static final int MSG_PROCESS_FGAPP_PARALL_BR = 103;
    private static final int MSG_PROCESS_PARALL_BR = 101;
    static final String TAG = "AwareBroadcastProcess";
    private String mBrName;
    private final IawareBroadcastHandler mHandler;
    private AwareBroadcastPolicy mIawareBrPolicy;
    private final HashMap<Integer, ArrayList<HwBroadcastRecord>> mIawareParallelProxyBrMap = new HashMap();
    private int mIawareUnProxyTime;
    private int mProxyKeyBrIndex;
    private boolean mStartUnproxy;
    private int mUnproxyHighSpeed;
    private int mUnproxyMaxDuration;
    private int mUnproxyMaxSpeed;
    private int mUnproxyMiddleSpeed;
    private int mUnproxyMinSpeed;

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
                        AwareBroadcastProcess.this.insertProxyParalledBroadcast(msg.obj);
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

    public AwareBroadcastProcess(AwareBroadcastPolicy iawareBrPolicy, Handler handler, String name) {
        int index = 0;
        this.mStartUnproxy = false;
        this.mIawareUnProxyTime = 150;
        this.mProxyKeyBrIndex = -1;
        this.mIawareBrPolicy = null;
        this.mBrName = null;
        this.mUnproxyMaxDuration = 20000;
        this.mUnproxyMaxSpeed = 150;
        this.mUnproxyMinSpeed = 60;
        this.mUnproxyMiddleSpeed = 40;
        this.mUnproxyHighSpeed = 20;
        this.mIawareBrPolicy = iawareBrPolicy;
        this.mHandler = new IawareBroadcastHandler(handler.getLooper());
        this.mBrName = name;
        while (index < 4) {
            this.mIawareParallelProxyBrMap.put(Integer.valueOf(index), new ArrayList());
            index++;
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
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("iaware_br start unproxy, mStartBgUnproxy = true, queue name : ");
                stringBuilder.append(this.mBrName);
                AwareLog.d(str, stringBuilder.toString());
            }
            this.mStartUnproxy = true;
            unProxyBroadcast(5);
        }
    }

    private void insertProxyParalledBroadcast(HwBroadcastRecord r) {
        synchronized (this.mIawareParallelProxyBrMap) {
            int curAdj = r.getReceiverCurAdj();
            if (2 == r.getReceiverCurProcState()) {
                if (!r.isSysApp()) {
                    trimProxyBr(r, 0);
                }
                ((ArrayList) this.mIawareParallelProxyBrMap.get(Integer.valueOf(0))).add(r);
            } else if (curAdj < 0) {
                trimProxyBr(r, 1);
                ((ArrayList) this.mIawareParallelProxyBrMap.get(Integer.valueOf(1))).add(r);
            } else if (curAdj >= 900) {
                trimProxyBr(r, 3);
                ((ArrayList) this.mIawareParallelProxyBrMap.get(Integer.valueOf(3))).add(r);
            } else {
                processAdjChange(r);
                trimProxyBr(r, 2);
                ((ArrayList) this.mIawareParallelProxyBrMap.get(Integer.valueOf(2))).add(r);
            }
        }
    }

    private void unProxyBroadcast(int unproxyTime) {
        if (!this.mHandler.hasMessages(101)) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(101, this), (long) unproxyTime);
        }
    }

    private void unproxyEachBroacast() {
        if (isEmptyProxyMap()) {
            this.mStartUnproxy = false;
            return;
        }
        ArrayList<HwBroadcastRecord> parallelList = new ArrayList();
        synchronized (this.mIawareParallelProxyBrMap) {
            HwBroadcastRecord proxyBr = getNextUnProxyBr();
            if (proxyBr != null) {
                parallelList.add(proxyBr);
                if (AwareBroadcastDebug.getDebugDetail()) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("iaware_br unproxy cost time : ");
                    stringBuilder.append(System.currentTimeMillis() - proxyBr.getDispatchClockTime());
                    stringBuilder.append(" : receiver :");
                    stringBuilder.append(proxyBr.getBrReceivers().get(0));
                    stringBuilder.append(" : action : ");
                    stringBuilder.append(proxyBr.getAction());
                    stringBuilder.append(" : processState : ");
                    stringBuilder.append(proxyBr.getReceiverCurProcState());
                    stringBuilder.append(": adj : ");
                    stringBuilder.append(proxyBr.getReceiverCurAdj());
                    stringBuilder.append(": unproxy speed: ");
                    stringBuilder.append(this.mIawareUnProxyTime);
                    stringBuilder.append(" : size:");
                    stringBuilder.append(getIawareBrSize());
                    AwareLog.i(str, stringBuilder.toString());
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
            } else if (this.mIawareBrPolicy.isSpeedNoCtrol()) {
                if (isProcessProxyMiddleAdjList()) {
                    this.mIawareUnProxyTime = this.mUnproxyMiddleSpeed;
                } else {
                    this.mIawareUnProxyTime = this.mUnproxyMinSpeed;
                }
            } else {
                ArrayList<HwBroadcastRecord> iawareParallelBroadcasts = getProcessingList();
                int length = iawareParallelBroadcasts.size();
                if (length > 0) {
                    int minDuration;
                    int i;
                    int duration;
                    if (this.mProxyKeyBrIndex < 0) {
                        minDuration = Integer.MAX_VALUE;
                        for (i = 0; i < length; i++) {
                            duration = (int) (((((HwBroadcastRecord) iawareParallelBroadcasts.get(i)).getDispatchClockTime() + ((long) this.mUnproxyMaxDuration)) - System.currentTimeMillis()) / ((long) (i + 1)));
                            if (duration < minDuration) {
                                minDuration = duration;
                                this.mProxyKeyBrIndex = i;
                            }
                        }
                        this.mIawareUnProxyTime = minDuration;
                    } else if (this.mProxyKeyBrIndex < length - 1) {
                        minDuration = this.mIawareUnProxyTime;
                        i = this.mProxyKeyBrIndex;
                        while (true) {
                            i++;
                            if (i >= length) {
                                break;
                            }
                            duration = (int) (((((HwBroadcastRecord) iawareParallelBroadcasts.get(i)).getDispatchClockTime() + ((long) this.mUnproxyMaxDuration)) - System.currentTimeMillis()) / ((long) (i + 1)));
                            if (duration < minDuration) {
                                minDuration = duration;
                                this.mProxyKeyBrIndex = i;
                            }
                        }
                        this.mIawareUnProxyTime = minDuration;
                    }
                }
                resetUnproxyTime();
            }
        }
    }

    private void resetUnproxyTime() {
        int tempUnProxyTime = this.mIawareUnProxyTime > this.mUnproxyMinSpeed ? this.mIawareUnProxyTime : this.mUnproxyMinSpeed;
        this.mIawareUnProxyTime = this.mUnproxyMaxSpeed < tempUnProxyTime ? this.mUnproxyMaxSpeed : tempUnProxyTime;
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
            for (Entry<Integer, ArrayList<HwBroadcastRecord>> ent : this.mIawareParallelProxyBrMap.entrySet()) {
                length += ((ArrayList) ent.getValue()).size();
            }
        }
        return length;
    }

    public void setUnProxySpeedScreenOff() {
        int size = getIawareBrSize();
        if (size > 0) {
            int speed = IAWARE_UNPROXY_SCREEN_OFF_TIME / size;
            this.mIawareUnProxyTime = speed < this.mUnproxyMinSpeed ? speed : this.mUnproxyMinSpeed;
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

    public void dump(PrintWriter pw) {
        PrintWriter printWriter = pw;
        if (printWriter != null) {
            ArrayList<HwBroadcastRecord> iawareTopParallelBroadcasts;
            ArrayList<HwBroadcastRecord> iawareHighAdjParallelBroadcasts;
            ArrayList<HwBroadcastRecord> iawareMiddleAdjParallelBroadcasts;
            ArrayList<HwBroadcastRecord> iawareLowAdjParallelBroadcasts;
            int listSize;
            String action;
            HwBroadcastRecord hwBr;
            StringBuilder stringBuilder;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("      Proxy broadcast [");
            stringBuilder2.append(this.mBrName);
            stringBuilder2.append("]");
            printWriter.println(stringBuilder2.toString());
            Set<String> proxyActions = new ArraySet();
            ArraySet proxyTopPkgs = new ArraySet();
            ArraySet proxyHighAdjPkgs = new ArraySet();
            ArraySet proxyMiddleAdjPkgs = new ArraySet();
            ArraySet proxyLowAdjPkgs = new ArraySet();
            synchronized (this.mIawareParallelProxyBrMap) {
                try {
                    iawareTopParallelBroadcasts = new ArrayList((Collection) this.mIawareParallelProxyBrMap.get(Integer.valueOf(0)));
                    iawareHighAdjParallelBroadcasts = new ArrayList((Collection) this.mIawareParallelProxyBrMap.get(Integer.valueOf(1)));
                    iawareMiddleAdjParallelBroadcasts = new ArrayList((Collection) this.mIawareParallelProxyBrMap.get(Integer.valueOf(2)));
                    iawareLowAdjParallelBroadcasts = new ArrayList((Collection) this.mIawareParallelProxyBrMap.get(Integer.valueOf(3)));
                } catch (Throwable th) {
                    Set<String> set = proxyActions;
                    while (true) {
                    }
                }
            }
            int lengthTop = iawareTopParallelBroadcasts.size();
            int lengthHighAdj = iawareHighAdjParallelBroadcasts.size();
            int lengthMiddleAdj = iawareMiddleAdjParallelBroadcasts.size();
            int lengthLowAdj = iawareLowAdjParallelBroadcasts.size();
            int index = 0;
            int listSize2 = iawareTopParallelBroadcasts.size();
            while (true) {
                listSize = listSize2;
                if (index >= listSize) {
                    break;
                }
                ArrayList<HwBroadcastRecord> iawareTopParallelBroadcasts2 = iawareTopParallelBroadcasts;
                HwBroadcastRecord iawareTopParallelBroadcasts3 = (HwBroadcastRecord) iawareTopParallelBroadcasts.get(index);
                int listSize3 = listSize;
                action = iawareTopParallelBroadcasts3.getAction();
                proxyActions.add(action);
                proxyTopPkgs.add(iawareTopParallelBroadcasts3.getReceiverPkg());
                index++;
                iawareTopParallelBroadcasts = iawareTopParallelBroadcasts2;
                listSize2 = listSize3;
            }
            int index2 = 0;
            listSize = iawareHighAdjParallelBroadcasts.size();
            while (index2 < listSize) {
                hwBr = (HwBroadcastRecord) iawareHighAdjParallelBroadcasts.get(index2);
                int listSize4 = listSize;
                action = hwBr.getAction();
                proxyActions.add(action);
                proxyHighAdjPkgs.add(hwBr.getReceiverPkg());
                index2++;
                listSize = listSize4;
            }
            index2 = 0;
            listSize = iawareMiddleAdjParallelBroadcasts.size();
            while (index2 < listSize) {
                hwBr = (HwBroadcastRecord) iawareMiddleAdjParallelBroadcasts.get(index2);
                int listSize5 = listSize;
                action = hwBr.getAction();
                proxyActions.add(action);
                proxyMiddleAdjPkgs.add(hwBr.getReceiverPkg());
                index2++;
                listSize = listSize5;
            }
            index2 = 0;
            listSize = iawareLowAdjParallelBroadcasts.size();
            while (index2 < listSize) {
                hwBr = (HwBroadcastRecord) iawareLowAdjParallelBroadcasts.get(index2);
                int listSize6 = listSize;
                action = hwBr.getAction();
                proxyActions.add(action);
                proxyLowAdjPkgs.add(hwBr.getReceiverPkg());
                index2++;
                listSize = listSize6;
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("      proxy action :");
            stringBuilder2.append(proxyActions);
            printWriter.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("      proxy Top pkg :");
            stringBuilder2.append(proxyTopPkgs);
            printWriter.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("      proxy high adj pkg :");
            stringBuilder2.append(proxyHighAdjPkgs);
            printWriter.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("      proxy middle adj pkg :");
            stringBuilder2.append(proxyMiddleAdjPkgs);
            printWriter.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("      proxy low adj pkg :");
            stringBuilder2.append(proxyLowAdjPkgs);
            printWriter.println(stringBuilder2.toString());
            index2 = ((lengthTop + lengthHighAdj) + lengthMiddleAdj) + lengthLowAdj;
            if (index2 == 0) {
                printWriter.println("      Unproxy speed :0");
                Object obj = proxyActions;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("      Unproxy speed :");
                stringBuilder.append(this.mIawareUnProxyTime);
                printWriter.println(stringBuilder.toString());
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("      Proxy broadcast count :");
            stringBuilder.append(index2);
            stringBuilder.append(", Top count:");
            stringBuilder.append(lengthTop);
            stringBuilder.append(", high adj count:");
            stringBuilder.append(lengthHighAdj);
            stringBuilder.append(", middle adj count:");
            stringBuilder.append(lengthMiddleAdj);
            stringBuilder.append(", low adj count:");
            stringBuilder.append(lengthLowAdj);
            printWriter.println(stringBuilder.toString());
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

    private void unProxyForegroundAppBroadcast(int pid, int uid) {
        if (AwareBroadcastDebug.getDebugDetail()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("iaware_br unProxyForegroundAppBroadcast, uid:");
            stringBuilder.append(uid);
            stringBuilder.append(", pid: ");
            stringBuilder.append(pid);
            AwareLog.d(str, stringBuilder.toString());
        }
        ArrayList<HwBroadcastRecord> parallelList = (ArrayList) this.mIawareParallelProxyBrMap.get(Integer.valueOf(0));
        synchronized (this.mIawareParallelProxyBrMap) {
            String pkgSys = null;
            if (1000 == uid) {
                try {
                    pkgSys = getSysPkg(pid, uid);
                    if (AwareBroadcastDebug.getDebugDetail()) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("iaware_br unProxyForegroundAppBroadcast system app, uid:");
                        stringBuilder2.append(uid);
                        stringBuilder2.append(", pid: ");
                        stringBuilder2.append(pid);
                        stringBuilder2.append(": pkg : ");
                        stringBuilder2.append(pkgSys);
                        AwareLog.i(str2, stringBuilder2.toString());
                    }
                    if (pkgSys == null) {
                        return;
                    }
                } catch (Throwable th) {
                }
            }
            for (Entry<Integer, ArrayList<HwBroadcastRecord>> ent : this.mIawareParallelProxyBrMap.entrySet()) {
                if (((Integer) ent.getKey()).intValue() != 0) {
                    iAwareUnproxyBroadcastInner((ArrayList) ent.getValue(), uid, parallelList, pkgSys);
                }
            }
        }
    }

    private void iAwareUnproxyBroadcastInner(ArrayList<HwBroadcastRecord> pendingBroadcasts, int unProxyUid, ArrayList<HwBroadcastRecord> unProxyBroadcasts, String unProxyPkgSys) {
        Iterator it = pendingBroadcasts.iterator();
        while (it.hasNext()) {
            HwBroadcastRecord br = (HwBroadcastRecord) it.next();
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
        for (Entry<Integer, ArrayList<HwBroadcastRecord>> ent : this.mIawareParallelProxyBrMap.entrySet()) {
            if (((ArrayList) ent.getValue()).size() > 0) {
                return false;
            }
        }
        return true;
    }

    private boolean isProcessProxyTopList() {
        return ((ArrayList) this.mIawareParallelProxyBrMap.get(Integer.valueOf(0))).size() != 0;
    }

    private boolean isProcessProxyHighAdjList() {
        return ((ArrayList) this.mIawareParallelProxyBrMap.get(Integer.valueOf(1))).size() != 0;
    }

    private boolean isProcessProxyMiddleAdjList() {
        return ((ArrayList) this.mIawareParallelProxyBrMap.get(Integer.valueOf(0))).size() == 0 && ((ArrayList) this.mIawareParallelProxyBrMap.get(Integer.valueOf(1))).size() == 0 && ((ArrayList) this.mIawareParallelProxyBrMap.get(Integer.valueOf(2))).size() != 0;
    }

    private HwBroadcastRecord getNextUnProxyBr() {
        for (Entry<Integer, ArrayList<HwBroadcastRecord>> ent : this.mIawareParallelProxyBrMap.entrySet()) {
            ArrayList<HwBroadcastRecord> iawareParallelBrList = (ArrayList) ent.getValue();
            if (isRestrictTopApp() && ((Integer) ent.getKey()).intValue() == 0) {
                if (AwareBroadcastDebug.getDebugDetail()) {
                    AwareLog.i(TAG, "iaware_br don't unproxy top app's br");
                }
            } else if (iawareParallelBrList.size() > 0) {
                return (HwBroadcastRecord) iawareParallelBrList.remove(0);
            }
        }
        return null;
    }

    private ArrayList<HwBroadcastRecord> getProcessingList() {
        if (isProcessProxyMiddleAdjList()) {
            return (ArrayList) this.mIawareParallelProxyBrMap.get(Integer.valueOf(2));
        }
        return (ArrayList) this.mIawareParallelProxyBrMap.get(Integer.valueOf(3));
    }

    private void processAdjChange(HwBroadcastRecord r) {
        String pkg = r.getReceiverPkg();
        if (pkg != null) {
            int pid = r.getReceiverPid();
            ArrayList<HwBroadcastRecord> iawareMiddleBrList = (ArrayList) this.mIawareParallelProxyBrMap.get(Integer.valueOf(2));
            Iterator it = ((ArrayList) this.mIawareParallelProxyBrMap.get(Integer.valueOf(3))).iterator();
            while (it.hasNext()) {
                HwBroadcastRecord br = (HwBroadcastRecord) it.next();
                if (br.getReceiverPid() == pid && pkg.equals(br.getReceiverPkg())) {
                    if (AwareBroadcastDebug.getDebugDetail()) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("iaware_br adj change, transfer: ");
                        stringBuilder.append(br.getBrReceivers().get(0));
                        AwareLog.d(str, stringBuilder.toString());
                    }
                    iawareMiddleBrList.add(br);
                    it.remove();
                }
            }
        }
    }

    private void trimProxyBr(HwBroadcastRecord r, int level) {
        if (this.mIawareBrPolicy.isTrimAction(r.getAction())) {
            Iterator it = ((ArrayList) this.mIawareParallelProxyBrMap.get(Integer.valueOf(level))).iterator();
            while (it.hasNext()) {
                HwBroadcastRecord br = (HwBroadcastRecord) it.next();
                if (canTrim(r, br)) {
                    if (AwareBroadcastDebug.getDebugDetail()) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("iaware_br brtrim success: ");
                        stringBuilder.append(br.getBrReceivers().get(0));
                        stringBuilder.append(": action: ");
                        stringBuilder.append(r.getAction());
                        AwareLog.d(str, stringBuilder.toString());
                    }
                    it.remove();
                    return;
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:25:0x005c, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:26:0x005d, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
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
        if ("android.net.conn.CONNECTIVITY_CHANGE".equals(action1)) {
            return canConnectivityDataTrim(newBr.getIntent(), oldBr.getIntent());
        }
        if ("android.net.wifi.STATE_CHANGE".equals(action1)) {
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
        for (Entry<Integer, ArrayList<HwBroadcastRecord>> ent : this.mIawareParallelProxyBrMap.entrySet()) {
            ArrayList<HwBroadcastRecord> iawareBrList = (ArrayList) ent.getValue();
            int listSize = iawareBrList.size();
            for (int index = 0; index < listSize; index++) {
                HwBroadcastRecord hwBr = (HwBroadcastRecord) iawareBrList.get(index);
                if (hwBr.getReceiverPid() == pid && hwBr.getReceiverUid() == uid) {
                    return hwBr.getReceiverPkg();
                }
            }
        }
        return null;
    }

    private boolean isRestrictTopApp() {
        return this.mIawareBrPolicy.isInstallApp() && !this.mIawareBrPolicy.isSpeedNoCtrol() && isProcessProxyTopList();
    }

    private void processException() {
        synchronized (this.mIawareParallelProxyBrMap) {
            int length = 0;
            for (Entry<Integer, ArrayList<HwBroadcastRecord>> ent : this.mIawareParallelProxyBrMap.entrySet()) {
                length += ((ArrayList) ent.getValue()).size();
            }
            if (length > 10000) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("iaware_br proxy length more than ");
                stringBuilder.append(length);
                stringBuilder.append(", clear all proxy br");
                AwareLog.w(str, stringBuilder.toString());
                for (Entry<Integer, ArrayList<HwBroadcastRecord>> ent2 : this.mIawareParallelProxyBrMap.entrySet()) {
                    ((ArrayList) ent2.getValue()).clear();
                }
            }
        }
    }
}
