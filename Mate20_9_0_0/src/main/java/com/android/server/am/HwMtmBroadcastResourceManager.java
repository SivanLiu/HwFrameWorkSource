package com.android.server.am;

import android.app.BroadcastOptions;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ComponentInfo;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Process;
import android.rms.iaware.AwareLog;
import com.android.server.mtm.MultiTaskManagerService;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.mtm.iaware.appmng.appstart.AwareAppStartupPolicy;
import com.android.server.mtm.iaware.brjob.StartupByBroadcastRecorder;
import com.android.server.mtm.iaware.brjob.scheduler.AwareJobSchedulerService;
import com.android.server.mtm.iaware.srms.AwareBroadcastDebug;
import com.android.server.mtm.iaware.srms.AwareBroadcastDumpRadar;
import com.android.server.mtm.iaware.srms.AwareBroadcastPolicy;
import com.android.server.mtm.iaware.srms.AwareBroadcastPolicy.BrCtrlType;
import com.android.server.mtm.iaware.srms.AwareBroadcastRegister;
import com.android.server.mtm.iaware.srms.AwareBroadcastSend;
import com.android.server.mtm.taskstatus.ProcessInfo;
import com.android.server.mtm.taskstatus.ProcessInfoCollector;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.rms.iaware.srms.BroadcastExFeature;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class HwMtmBroadcastResourceManager implements AbsHwMtmBroadcastResourceManager {
    public static final int CACHED_APP_MIN_ADJ = 900;
    public static final int FOREGROUND_APP_ADJ = 0;
    private static final int INVALID_CONFIG_POLICY = -1;
    private static final String TAG = "HwMtmBroadcastResourceManager";
    public static final int TOP_APP = 2;
    private AwareBroadcastRegister mAwareBRRegister = null;
    private AwareBroadcastSend mAwareBRSend = null;
    private AwareJobSchedulerService mAwareJobSchedulerService = null;
    private HashMap<ReceiverList, String> mBRIdMap = new HashMap();
    private AwareBroadcastDumpRadar mBrDumpRadar = null;
    private AwareBroadcastPolicy mIawareBrPolicy = null;
    private final BroadcastQueue mQueue;
    private int mSysServicePid = Process.myPid();

    public HwMtmBroadcastResourceManager(BroadcastQueue queue) {
        this.mQueue = queue;
        AwareBroadcastPolicy.initBrCache(this.mQueue.mQueueName, this.mQueue.mService);
    }

    public boolean iawareProcessBroadcast(int type, boolean isParallel, BroadcastRecord r, Object target) {
        if (type == 0) {
            boolean enqueue = enqueueIawareProxyBroacast(isParallel, r, target);
            trackBrFlow(enqueue, isParallel, r, target);
            return enqueue;
        } else if (type == 1) {
            return processBroadcastScheduler(isParallel, r, target);
        } else {
            return false;
        }
    }

    public void iawareCheckCombinedConditon(IntentFilter filter) {
        if (filter == null) {
            AwareLog.e(TAG, "iawareCheckCombinedConditon param error!");
            return;
        }
        if (this.mAwareBRRegister == null) {
            this.mAwareBRRegister = AwareBroadcastRegister.getInstance();
        }
        String acId = this.mAwareBRRegister.findMatchedAssembleConditionId(filter.getIdentifier());
        if (acId != null) {
            Iterator<String> actions = filter.actionsIterator();
            if (actions != null) {
                while (actions.hasNext()) {
                    String condition = this.mAwareBRRegister.getBRAssembleCondition(acId, (String) actions.next());
                    if (condition != null) {
                        filter.addCategory(condition);
                        if (AwareBroadcastDebug.getDebugDetail()) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("brreg: add condition: ");
                            stringBuilder.append(condition);
                            stringBuilder.append(" for ");
                            stringBuilder.append(filter.getIdentifier());
                            AwareLog.i(str, stringBuilder.toString());
                        }
                    }
                }
            }
        }
    }

    public void iawareCountDuplicatedReceiver(boolean isRegister, ReceiverList rl, IntentFilter filter) {
        if (rl == null || (isRegister && filter == null)) {
            AwareLog.e(TAG, "iawareCountDuplicatedReceiver param error!");
            return;
        }
        if (this.mAwareBRRegister == null) {
            this.mAwareBRRegister = AwareBroadcastRegister.getInstance();
        }
        String brId = "";
        if (isRegister) {
            brId = filter.getIdentifier();
            this.mBRIdMap.put(rl, brId);
        } else {
            brId = (String) this.mBRIdMap.remove(rl);
        }
        int brCount = this.mAwareBRRegister.countReceiverRegister(isRegister, brId);
        if (AwareBroadcastDebug.getDebugDetail()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("brreg: regCounter, ");
            stringBuilder.append(isRegister ? "register" : "unregister");
            stringBuilder.append(" brId: ");
            stringBuilder.append(brId);
            stringBuilder.append(" count:");
            stringBuilder.append(brCount);
            AwareLog.i(str, stringBuilder.toString());
        }
    }

    public boolean iawareNeedSkipBroadcastSend(String action, Object[] data) {
        if (action == null || data == null) {
            AwareLog.e(TAG, "iawareNeedSkipBroadcastSend param error!");
            return false;
        } else if (BroadcastExFeature.isFeatureEnabled(2)) {
            if (this.mAwareBRSend == null) {
                this.mAwareBRSend = AwareBroadcastSend.getInstance();
            }
            if (this.mAwareBRSend.setData(action, data)) {
                return this.mAwareBRSend.needSkipBroadcastSend(action);
            }
            return false;
        } else {
            if (AwareBroadcastDebug.getDebugDetail()) {
                AwareLog.i(TAG, "BrSend feature not enabled!");
            }
            return false;
        }
    }

    private boolean processBroadcastScheduler(boolean isParallel, BroadcastRecord r, Object target) {
        BroadcastRecord broadcastRecord = r;
        ResolveInfo resolveInfo = target;
        if (isParallel || broadcastRecord == null || resolveInfo == null || !(resolveInfo instanceof ResolveInfo)) {
            AwareLog.e(TAG, "iaware_brjob processBroadcastScheduler param error!");
            return false;
        }
        ResolveInfo info = resolveInfo;
        String packageName = null;
        ComponentInfo ci = info.getComponentInfo();
        if (!(ci == null || ci.applicationInfo == null)) {
            packageName = ci.applicationInfo.packageName;
        }
        String packageName2 = packageName;
        String action = broadcastRecord.intent.getAction();
        IntentFilter filter = info.filter;
        boolean z;
        IntentFilter filter2;
        if (filter == null || filter.countActionFilters() <= 0) {
            z = true;
            filter2 = filter;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("iaware_brjob not process: ");
            stringBuilder.append(info);
            stringBuilder.append(", filter: ");
            IntentFilter filter3 = filter2;
            stringBuilder.append(filter3 == null ? "null" : filter3);
            stringBuilder.append(", count: ");
            stringBuilder.append(filter3 == null ? "null" : Integer.valueOf(filter3.countActionFilters()));
            AwareLog.w(str, stringBuilder.toString());
            StartupByBroadcastRecorder.getInstance().recordStartupTimeByBroadcast(packageName2, action, System.currentTimeMillis());
            trackImplicitBr(false, z, packageName2, action);
        } else {
            if (broadcastRecord.iawareCtrlType == 2) {
                z = true;
                filter2 = filter;
                StartupByBroadcastRecorder.getInstance().recordStartupTimeByBroadcast(packageName2, action, System.currentTimeMillis());
                trackImplicitBr(z, z, packageName2, action);
            } else if (broadcastRecord.queue == null || !enqueueBroadcastScheduler(isParallel, r, target)) {
                filter2 = filter;
                trackImplicitBr(false, true, packageName2, action);
            } else {
                trackImplicitBr(true, false, packageName2, action);
                AwareAppStartupPolicy policy = AwareAppStartupPolicy.self();
                if (policy != null) {
                    policy.updateBroadJobCtrlBigData(packageName2);
                }
                broadcastRecord.queue.finishReceiverLocked(broadcastRecord, broadcastRecord.resultCode, broadcastRecord.resultData, broadcastRecord.resultExtras, broadcastRecord.resultAbort, null);
                broadcastRecord.queue.scheduleBroadcastsLocked();
                broadcastRecord.state = 0;
                return true;
            }
        }
        return false;
    }

    private boolean enqueueBroadcastScheduler(boolean isParallel, BroadcastRecord r, Object target) {
        BroadcastRecord broadcastRecord = r;
        Object obj = target;
        if (getAwareJobSchedulerService() == null) {
            return false;
        }
        if (isSystemApplication(obj)) {
            IIntentReceiver resultTo = broadcastRecord.resultTo;
            if (broadcastRecord.resultTo != null) {
                AwareLog.d(TAG, "reset resultTo null");
                resultTo = null;
            }
            List<Object> receiver = new ArrayList();
            receiver.add(obj);
            List<Object> receiver2 = receiver;
            return this.mAwareJobSchedulerService.schedule(new HwBroadcastRecord(new BroadcastRecord(broadcastRecord.queue, broadcastRecord.intent, broadcastRecord.callerApp, broadcastRecord.callerPackage, broadcastRecord.callingPid, broadcastRecord.callingUid, broadcastRecord.callerInstantApp, broadcastRecord.resolvedType, broadcastRecord.requiredPermissions, broadcastRecord.appOp, broadcastRecord.options, receiver2, resultTo, broadcastRecord.resultCode, broadcastRecord.resultData, broadcastRecord.resultExtras, broadcastRecord.ordered, broadcastRecord.sticky, broadcastRecord.initialSticky, broadcastRecord.userId)));
        }
        AwareLog.w(TAG, "iaware_brjob not system app");
        return false;
    }

    private boolean enqueueIawareProxyBroacast(boolean isParallel, BroadcastRecord r, Object target) {
        BroadcastRecord broadcastRecord = r;
        Object obj = target;
        if (isAbnormalParameters(isParallel, r, target) || getIawareBrPolicy() == null || broadcastRecord.iawareCtrlType == 1 || !this.mIawareBrPolicy.isProxyedAllowedCondition() || broadcastRecord.callingPid != this.mSysServicePid || isThirdOrKeyBroadcast()) {
            return false;
        }
        String pkg = getPkg(obj);
        int pid = getPid(obj);
        int uid = getUid(obj);
        if (isAbnormalValue(pkg, pid, uid) || pid == this.mSysServicePid) {
            return false;
        }
        String action = null;
        if (broadcastRecord.intent != null) {
            action = broadcastRecord.intent.getAction();
        }
        String action2 = action;
        String str;
        int i;
        String str2;
        boolean pid2;
        if (this.mIawareBrPolicy.isNotProxySysPkg(pkg, action2)) {
            str = pkg;
            i = pid;
            str2 = action2;
            pid2 = isParallel;
        } else if (isInstrumentationApp(obj)) {
            str = pkg;
            i = pid;
            pkg = uid;
            str2 = action2;
            pid2 = isParallel;
        } else {
            boolean isSystemApp = isSystemApplication(obj);
            int curProcState = getcurProcState(obj);
            int curAdj = getProcessCurrentAdj(obj);
            int i2;
            int i3;
            boolean z;
            if (-1 == curProcState) {
                i2 = curAdj;
                i3 = curProcState;
                i = pid;
                pkg = uid;
                str2 = action2;
                z = isSystemApp;
                pid2 = isParallel;
            } else if (-10000 == curAdj) {
                str = pkg;
                i2 = curAdj;
                i3 = curProcState;
                i = pid;
                pkg = uid;
                str2 = action2;
                z = isSystemApp;
                pid2 = isParallel;
            } else {
                int curAdj2 = curAdj;
                int curProcState2 = curProcState;
                if (!this.mIawareBrPolicy.shouldIawareProxyBroadcast(action2, broadcastRecord.callingPid, uid, pid, pkg)) {
                    return false;
                }
                List<Object> receiver = new ArrayList();
                receiver.add(obj);
                boolean isSystemApp2 = isSystemApp;
                String pkg2 = pkg;
                int pid3 = pid;
                int uid2 = uid;
                BroadcastRecord proxyBR = new BroadcastRecord(broadcastRecord.queue, broadcastRecord.intent, broadcastRecord.callerApp, broadcastRecord.callerPackage, broadcastRecord.callingPid, broadcastRecord.callingUid, broadcastRecord.callerInstantApp, broadcastRecord.resolvedType, broadcastRecord.requiredPermissions, broadcastRecord.appOp, broadcastRecord.options, receiver, broadcastRecord.resultTo, broadcastRecord.resultCode, broadcastRecord.resultData, broadcastRecord.resultExtras, broadcastRecord.ordered, broadcastRecord.sticky, broadcastRecord.initialSticky, broadcastRecord.userId);
                proxyBR.dispatchClockTime = broadcastRecord.dispatchClockTime;
                proxyBR.dispatchTime = broadcastRecord.dispatchTime;
                proxyBR.iawareCtrlType = 1;
                HwBroadcastRecord hwBr = new HwBroadcastRecord(proxyBR);
                hwBr.setReceiverUid(uid2);
                hwBr.setReceiverPid(pid3);
                hwBr.setReceiverCurAdj(curAdj2);
                hwBr.setReceiverPkg(pkg2);
                hwBr.setSysApp(isSystemApp2);
                hwBr.setReceiverCurProcState(curProcState2);
                return this.mIawareBrPolicy.enqueueIawareProxyBroacast(isParallel, hwBr);
            }
            return false;
        }
        return false;
    }

    public void iawareStartCountBroadcastSpeed(boolean isParallel, BroadcastRecord r) {
        if (r != null && getIawareBrPolicy() != null && isParallel && r.iawareCtrlType != 1) {
            String action = null;
            if (r.intent != null) {
                action = r.intent.getAction();
            }
            if (action != null) {
                this.mIawareBrPolicy.iawareStartCountBroadcastSpeed(isParallel, r.dispatchClockTime, r.receivers.size());
            }
        }
    }

    public void iawareEndCountBroadcastSpeed(BroadcastRecord r) {
        if (r != null && getIawareBrPolicy() != null && r.iawareCtrlType != 1) {
            scheduleTrackBrFlowData();
            this.mIawareBrPolicy.endCheckCount();
        }
    }

    private boolean isAbnormalParameters(boolean isParallel, BroadcastRecord r, Object target) {
        if (!isParallel || r == null || target == null) {
            return true;
        }
        return false;
    }

    private AwareBroadcastPolicy getIawareBrPolicy() {
        if (this.mIawareBrPolicy == null && MultiTaskManagerService.self() != null) {
            this.mIawareBrPolicy = MultiTaskManagerService.self().getIawareBrPolicy();
        }
        return this.mIawareBrPolicy;
    }

    private AwareJobSchedulerService getAwareJobSchedulerService() {
        if (this.mAwareJobSchedulerService == null && MultiTaskManagerService.self() != null) {
            this.mAwareJobSchedulerService = MultiTaskManagerService.self().getAwareJobSchedulerService();
        }
        return this.mAwareJobSchedulerService;
    }

    public static void insertIawareBroadcast(ArrayList<HwBroadcastRecord> parallelList, String name) {
        if (parallelList != null && parallelList.size() != 0) {
            int i = 0;
            BroadcastQueue queue = ((HwBroadcastRecord) parallelList.get(0)).getBroacastQueue();
            synchronized (queue.mService) {
                int listSize = parallelList.size();
                while (i < listSize) {
                    queue.mParallelBroadcasts.add(i, ((HwBroadcastRecord) parallelList.get(i)).getBroadcastRecord());
                    i++;
                }
                queue.scheduleBroadcastsLocked();
            }
        }
    }

    private String getPkg(Object target) {
        if (target instanceof BroadcastFilter) {
            return ((BroadcastFilter) target).packageName;
        }
        return null;
    }

    private int getPid(Object target) {
        if (!(target instanceof BroadcastFilter)) {
            return -1;
        }
        BroadcastFilter filter = (BroadcastFilter) target;
        if (filter.receiverList == null) {
            return -1;
        }
        int pid = filter.receiverList.pid;
        if (pid > 0 || filter.receiverList.app == null) {
            return pid;
        }
        return filter.receiverList.app.pid;
    }

    private int getUid(Object target) {
        if (!(target instanceof BroadcastFilter)) {
            return -1;
        }
        BroadcastFilter filter = (BroadcastFilter) target;
        if (filter.receiverList == null) {
            return -1;
        }
        int uid = filter.receiverList.uid;
        if (uid > 0 || filter.receiverList.app == null) {
            return uid;
        }
        return filter.receiverList.app.uid;
    }

    private boolean isSystemApplication(Object target) {
        boolean z = false;
        if (target instanceof BroadcastFilter) {
            BroadcastFilter filter = (BroadcastFilter) target;
            if (filter.receiverList != null && filter.receiverList.app != null) {
                int flags = filter.receiverList.app.info.flags;
                int privateFlags = filter.receiverList.app.info.privateFlags;
                if (!((flags & 1) == 0 && (flags & 128) == 0 && (privateFlags & 8) == 0)) {
                    z = true;
                }
                return z;
            } else if (!AwareBroadcastDebug.getDebugDetail()) {
                return false;
            } else {
                AwareLog.d(TAG, "isSystemApplication BroadcastFilter: filter something is null");
                return false;
            }
        } else if (!(target instanceof ResolveInfo)) {
            return false;
        } else {
            ResolveInfo info = (ResolveInfo) target;
            if (info.activityInfo != null && info.activityInfo.applicationInfo != null) {
                if (info.activityInfo.applicationInfo.isSystemApp() || info.activityInfo.applicationInfo.isPrivilegedApp() || info.activityInfo.applicationInfo.isUpdatedSystemApp()) {
                    z = true;
                }
                return z;
            } else if (!AwareBroadcastDebug.getDebugDetail()) {
                return false;
            } else {
                AwareLog.w(TAG, "isSystemApplication ResolveInfo: info, info.activityInfo, info.activityInfo.applicationInfo is null ");
                return false;
            }
        }
    }

    private boolean isInstrumentationApp(Object target) {
        boolean instrumentationApp = false;
        if (target instanceof BroadcastFilter) {
            BroadcastFilter filter = (BroadcastFilter) target;
            if (!(filter.receiverList == null || filter.receiverList.app == null || filter.receiverList.app.instr == null)) {
                instrumentationApp = true;
                if (AwareBroadcastDebug.getDebugDetail()) {
                    AwareLog.d(TAG, "instrumentation app do not proxy!");
                }
            }
        }
        return instrumentationApp;
    }

    private boolean isThirdOrKeyBroadcast() {
        return (MemoryConstant.MEM_REPAIR_CONSTANT_BG.equals(this.mQueue.mQueueName) || MemoryConstant.MEM_REPAIR_CONSTANT_FG.equals(this.mQueue.mQueueName)) ? false : true;
    }

    private AwareBroadcastDumpRadar getBrDumpRadar() {
        if (this.mBrDumpRadar == null && MultiTaskManagerService.self() != null) {
            this.mBrDumpRadar = MultiTaskManagerService.self().getIawareBrRadar();
        }
        return this.mBrDumpRadar;
    }

    private void trackBrFlow(boolean enqueue, boolean isParallel, BroadcastRecord r, Object target) {
        if (getBrDumpRadar() != null && r != null && isParallel && target != null && (target instanceof BroadcastFilter)) {
            boolean isProxyed = r.iawareCtrlType == 1;
            AwareBroadcastPolicy policy = getIawareBrPolicy();
            if (policy != null) {
                this.mBrDumpRadar.trackBrFlowSpeed(enqueue, isProxyed, policy.isSpeedNoCtrol() ^ true, true ^ policy.isScreenOff());
            }
        }
    }

    private void scheduleTrackBrFlowData() {
        if (getBrDumpRadar() != null) {
            this.mBrDumpRadar.scheduleTrackBrFlowData();
        }
    }

    private void trackImplicitBr(boolean inControl, boolean startup, String packageName, String action) {
        if (getBrDumpRadar() != null && packageName != null && action != null) {
            this.mBrDumpRadar.trackImplicitBrDetail(inControl, startup, packageName, action);
            this.mBrDumpRadar.scheduleTrackBrFlowData();
        }
    }

    public static void insertIawareOrderedBroadcast(HwBroadcastRecord hwBr) {
        if (hwBr != null) {
            BroadcastRecord r = hwBr.getBroadcastRecord();
            if (r != null && r.queue != null) {
                synchronized (r.queue.mService) {
                    r.iawareCtrlType = 2;
                    r.queue.enqueueOrderedBroadcastLocked(r);
                    r.queue.scheduleBroadcastsLocked();
                }
            }
        }
    }

    private int getProcessCurrentAdj(Object target) {
        if (!(target instanceof BroadcastFilter)) {
            return -10000;
        }
        BroadcastFilter filter = (BroadcastFilter) target;
        if (filter.receiverList != null && filter.receiverList.app != null) {
            return filter.receiverList.app.curAdj;
        }
        if (!AwareBroadcastDebug.getDebugDetail()) {
            return -10000;
        }
        AwareLog.d(TAG, "getProcessCurrentAdj BroadcastFilter: filter something is null");
        return -10000;
    }

    private int getcurProcState(Object target) {
        if (!(target instanceof BroadcastFilter)) {
            return -1;
        }
        BroadcastFilter filter = (BroadcastFilter) target;
        if (filter.receiverList != null && filter.receiverList.app != null) {
            return filter.receiverList.app.curProcState;
        }
        if (!AwareBroadcastDebug.getDebugDetail()) {
            return -1;
        }
        AwareLog.d(TAG, "getProcessCurrentAdj BroadcastFilter: filter something is null");
        return -1;
    }

    private boolean isAbnormalValue(String pkg, int pid, int uid) {
        if (pkg == null || pid == -1 || uid == -1) {
            return true;
        }
        return false;
    }

    private int filterRegisteredReceiver(Intent intent, BroadcastFilter filter, AwareProcessInfo pInfo) {
        if (filter.countActionFilters() > 0 && ((pInfo.mProcInfo.mType == 2 || pInfo.mPid == this.mSysServicePid) && this.mIawareBrPolicy.assemFilterBr(intent, filter))) {
            return BrCtrlType.DISCARDBR.ordinal();
        }
        if (pInfo.mPid == this.mSysServicePid || pInfo.mProcInfo.mCurAdj < 0) {
            return BrCtrlType.NONE.ordinal();
        }
        int configListPolicy = getPolicyFromConfigList(intent, filter.packageName, pInfo);
        if (configListPolicy != -1) {
            return configListPolicy;
        }
        int policy = this.mIawareBrPolicy.filterBr(intent, pInfo);
        if (AwareBroadcastDebug.getFilterDebug()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("iaware_brFilter : reg policy:");
            stringBuilder.append(policy);
            stringBuilder.append(", pkgname:");
            stringBuilder.append(filter.packageName);
            stringBuilder.append(", action:");
            stringBuilder.append(intent.getAction());
            stringBuilder.append(", proc state:");
            stringBuilder.append(pInfo.getState());
            stringBuilder.append(", proc type:");
            stringBuilder.append(pInfo.mProcInfo.mType);
            AwareLog.i(str, stringBuilder.toString());
        }
        return policy;
    }

    private int filterResolveInfo(Intent intent, ResolveInfo resolveInfo, AwareProcessInfo pInfo) {
        int configListPolicy = getPolicyFromConfigList(intent, resolveInfo.activityInfo.applicationInfo.packageName, pInfo);
        if (configListPolicy != -1) {
            if (AwareBroadcastDebug.getFilterDebug()) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("iaware_brFilter resolve config policy: ");
                stringBuilder.append(configListPolicy);
                stringBuilder.append(", pkgname:");
                stringBuilder.append(resolveInfo.activityInfo.applicationInfo.packageName);
                stringBuilder.append(", action:");
                stringBuilder.append(intent.getAction());
                AwareLog.i(str, stringBuilder.toString());
            }
            return configListPolicy;
        }
        int policy = this.mIawareBrPolicy.filterBr(intent, pInfo);
        if (AwareBroadcastDebug.getFilterDebug()) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("iaware_brFilter : resolve policy:");
            stringBuilder2.append(policy);
            stringBuilder2.append(", pkgname:");
            stringBuilder2.append(resolveInfo.activityInfo.applicationInfo.packageName);
            stringBuilder2.append(", action:");
            stringBuilder2.append(intent.getAction());
            stringBuilder2.append(", proc state:");
            stringBuilder2.append(pInfo.getState());
            stringBuilder2.append(", proc type:");
            stringBuilder2.append(pInfo.mProcInfo.mType);
            AwareLog.i(str2, stringBuilder2.toString());
        }
        return policy;
    }

    public void iawareFilterBroadcast(Intent intent, ProcessRecord callerApp, String callerPackage, int callingPid, int callingUid, boolean callerInstantApp, String resolvedType, String[] requiredPermissions, int appOp, BroadcastOptions options, List receivers, List<BroadcastFilter> registeredReceivers, IIntentReceiver resultTo, int resultCode, String resultData, Bundle resultExtras, boolean ordered, boolean sticky, boolean initialSticky, int userId) {
        if (isCondtionSatisfy(intent)) {
            Intent intent2 = intent;
            this.mIawareBrPolicy.getStateFromSendBr(intent2);
            IIntentReceiver proxyResultTo = resultTo;
            if (ordered && resultTo != null) {
                if (AwareBroadcastDebug.getFilterDebug()) {
                    AwareLog.i(TAG, "reset resultTo null");
                }
                proxyResultTo = null;
            }
            IIntentReceiver proxyResultTo2 = proxyResultTo;
            Intent intent3 = intent2;
            ProcessRecord processRecord = callerApp;
            String str = callerPackage;
            int i = callingPid;
            int i2 = callingUid;
            boolean z = callerInstantApp;
            String str2 = resolvedType;
            String[] strArr = requiredPermissions;
            int i3 = appOp;
            BroadcastOptions broadcastOptions = options;
            List list = receivers;
            List<BroadcastFilter> list2 = registeredReceivers;
            IIntentReceiver iIntentReceiver = proxyResultTo2;
            int i4 = resultCode;
            String str3 = resultData;
            Bundle bundle = resultExtras;
            boolean z2 = ordered;
            boolean z3 = sticky;
            boolean z4 = initialSticky;
            int i5 = userId;
            processRegisteredReceiver(intent3, processRecord, str, i, i2, z, str2, strArr, i3, broadcastOptions, list, list2, iIntentReceiver, i4, str3, bundle, z2, z3, z4, i5);
            processResolveReceiver(intent, processRecord, str, i, i2, z, str2, strArr, i3, broadcastOptions, list, list2, iIntentReceiver, i4, str3, bundle, z2, z3, z4, i5);
        }
    }

    private void processRegisteredReceiver(Intent intent, ProcessRecord callerApp, String callerPackage, int callingPid, int callingUid, boolean callerInstantApp, String resolvedType, String[] requiredPermissions, int appOp, BroadcastOptions options, List receivers, List<BroadcastFilter> registeredReceivers, IIntentReceiver resultTo, int resultCode, String resultData, Bundle resultExtras, boolean ordered, boolean sticky, boolean initialSticky, int userId) {
        if (registeredReceivers != null) {
            String action = intent.getAction();
            AwareBroadcastDumpRadar.increatBrBeforeCount(registeredReceivers.size());
            Iterator<BroadcastFilter> regIterator = registeredReceivers.iterator();
            while (true) {
                Iterator<BroadcastFilter> regIterator2 = regIterator;
                if (regIterator2.hasNext()) {
                    BroadcastFilter filter = (BroadcastFilter) regIterator2.next();
                    AwareProcessInfo pInfo = getProcessInfo(filter);
                    if (pInfo == null) {
                        AwareBroadcastDumpRadar.increatBrNoProcessCount(1);
                    } else {
                        Intent intent2 = intent;
                        int policy = filterRegisteredReceiver(intent2, filter, pInfo);
                        if (policy == BrCtrlType.DISCARDBR.ordinal()) {
                            regIterator2.remove();
                            AwareBroadcastDumpRadar.increatBrAfterCount(1);
                            trackBrFilter(action, pInfo, policy, true, filter);
                        } else if (policy == BrCtrlType.CACHEBR.ordinal()) {
                            List<Object> cacheReceiver = new ArrayList();
                            cacheReceiver.add(filter);
                            int policy2 = policy;
                            HwBroadcastRecord hwBr = new HwBroadcastRecord(new BroadcastRecord(this.mQueue, intent2, callerApp, callerPackage, callingPid, callingUid, callerInstantApp, resolvedType, requiredPermissions, appOp, options, cacheReceiver, resultTo, resultCode, resultData, resultExtras, ordered, sticky, initialSticky, userId));
                            hwBr.setCurrentReceiverPid(pInfo.mPid);
                            hwBr.setReceiverPkg(filter.packageName);
                            boolean trim = this.mIawareBrPolicy.awareTrimAndEnqueueBr(ordered ^ 1, hwBr, false, pInfo.mPid, filter.packageName);
                            regIterator2.remove();
                            policy = hwBr;
                            trackBrFilter(action, pInfo, policy2, trim, filter);
                        } else {
                            int policy3 = policy;
                            if (policy3 == BrCtrlType.NONE.ordinal()) {
                                trackBrFilter(action, pInfo, policy3, false, filter);
                            }
                        }
                    }
                    regIterator = regIterator2;
                } else {
                    return;
                }
            }
        }
    }

    private void processResolveReceiver(Intent intent, ProcessRecord callerApp, String callerPackage, int callingPid, int callingUid, boolean callerInstantApp, String resolvedType, String[] requiredPermissions, int appOp, BroadcastOptions options, List receivers, List<BroadcastFilter> list, IIntentReceiver resultTo, int resultCode, String resultData, Bundle resultExtras, boolean ordered, boolean sticky, boolean initialSticky, int userId) {
        if (receivers != null) {
            String action = intent.getAction();
            AwareBroadcastDumpRadar.increatBrBeforeCount(receivers.size());
            Iterator<ResolveInfo> iterator = receivers.iterator();
            while (true) {
                Iterator<ResolveInfo> iterator2 = iterator;
                if (iterator2.hasNext()) {
                    ResolveInfo resolveInfo = (ResolveInfo) iterator2.next();
                    AwareProcessInfo pInfo = getProcessInfo(resolveInfo);
                    if (pInfo == null) {
                        AwareBroadcastDumpRadar.increatBrNoProcessCount(1);
                    } else {
                        Intent intent2 = intent;
                        int policy = filterResolveInfo(intent2, resolveInfo, pInfo);
                        if (policy == BrCtrlType.DISCARDBR.ordinal()) {
                            iterator2.remove();
                            AwareBroadcastDumpRadar.increatBrAfterCount(1);
                            trackBrFilter(action, pInfo, policy, true, resolveInfo);
                        } else if (policy == BrCtrlType.CACHEBR.ordinal()) {
                            List<Object> cacheReceiver = new ArrayList();
                            cacheReceiver.add(resolveInfo);
                            int policy2 = policy;
                            HwBroadcastRecord hwBr = new HwBroadcastRecord(new BroadcastRecord(this.mQueue, intent2, callerApp, callerPackage, callingPid, callingUid, callerInstantApp, resolvedType, requiredPermissions, appOp, options, cacheReceiver, resultTo, resultCode, resultData, resultExtras, ordered, sticky, initialSticky, userId));
                            hwBr.setCurrentReceiverPid(pInfo.mPid);
                            hwBr.setReceiverPkg(resolveInfo.activityInfo.applicationInfo.packageName);
                            boolean trim = this.mIawareBrPolicy.awareTrimAndEnqueueBr(0, hwBr, false, pInfo.mPid, resolveInfo.activityInfo.applicationInfo.packageName);
                            iterator2.remove();
                            policy = hwBr;
                            trackBrFilter(action, pInfo, policy2, trim, resolveInfo);
                        } else {
                            int policy3 = policy;
                            if (policy3 == BrCtrlType.NONE.ordinal()) {
                                trackBrFilter(action, pInfo, policy3, false, resolveInfo);
                            }
                        }
                    }
                    iterator = iterator2;
                } else {
                    return;
                }
            }
        }
    }

    private AwareProcessInfo getProcessInfo(Object target) {
        AwareProcessInfo awareProcessInfo = null;
        if (target instanceof BroadcastFilter) {
            int pid = getPid(target);
            if (pid == this.mSysServicePid) {
                return new AwareProcessInfo(pid, new ProcessInfo(pid, 1000));
            }
            if (isInstrumentationApp(target)) {
                return null;
            }
            awareProcessInfo = ProcessInfoCollector.getInstance().getAwareProcessInfo(pid);
            if (awareProcessInfo != null) {
                awareProcessInfo.mProcInfo.mCurAdj = getProcessCurrentAdj(target);
            }
        } else if (target instanceof ResolveInfo) {
            ResolveInfo info = (ResolveInfo) target;
            ProcessRecord app = this.mQueue.mService.getProcessRecordLocked(info.activityInfo.processName, info.activityInfo.applicationInfo.uid, false);
            if (app != null) {
                if (app.instr != null) {
                    return null;
                }
                awareProcessInfo = ProcessInfoCollector.getInstance().getAwareProcessInfo(app.pid);
                if (awareProcessInfo != null) {
                    awareProcessInfo.mProcInfo.mCurAdj = app.curAdj;
                }
            }
        }
        return awareProcessInfo;
    }

    private int getPolicyFromConfigList(Intent intent, String pkgName, AwareProcessInfo pInfo) {
        if (BroadcastExFeature.isBrFilterWhiteList(pkgName) || BroadcastExFeature.isBrFilterWhiteApp(intent.getAction(), pkgName)) {
            return BrCtrlType.NONE.ordinal();
        }
        if (pInfo.getState() == 1 && BroadcastExFeature.isBrFilterBlackApp(intent.getAction(), pkgName)) {
            return BrCtrlType.CACHEBR.ordinal();
        }
        return -1;
    }

    /* JADX WARNING: Missing block: B:19:0x0048, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isCondtionSatisfy(Intent intent) {
        if (intent == null || intent.getAction() == null || !BroadcastExFeature.isFeatureEnabled(1) || getIawareBrPolicy() == null) {
            return false;
        }
        if (intent.getPackage() == null && intent.getComponent() == null) {
            return true;
        }
        if (AwareBroadcastDebug.getFilterDebug()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("it is explicitly : ");
            stringBuilder.append(intent.getAction());
            AwareLog.i(str, stringBuilder.toString());
        }
        return false;
    }

    public static void unProxyCachedBr(ArrayList<HwBroadcastRecord> awareParallelBrs, ArrayList<HwBroadcastRecord> awareOrderedBrs) {
        StringBuilder stringBuilder;
        int count;
        BroadcastQueue queue = null;
        if (awareParallelBrs != null && awareParallelBrs.size() > 0) {
            queue = ((HwBroadcastRecord) awareParallelBrs.get(0)).getBroacastQueue();
            if (AwareBroadcastDebug.getFilterDebug()) {
                String str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("unproxy ");
                stringBuilder.append(queue.mQueueName);
                stringBuilder.append(" Broadcast pkg Parallel Broadcasts (");
                stringBuilder.append(awareParallelBrs);
                stringBuilder.append(")");
                AwareLog.i(str, stringBuilder.toString());
            }
            count = awareParallelBrs.size();
            for (int i = 0; i < count; i++) {
                queue.mParallelBroadcasts.add(i, ((HwBroadcastRecord) awareParallelBrs.get(i)).getBroadcastRecord());
            }
        }
        if (awareOrderedBrs != null && awareOrderedBrs.size() > 0) {
            if (queue == null) {
                queue = ((HwBroadcastRecord) awareOrderedBrs.get(0)).getBroacastQueue();
            }
            boolean pending = queue.mPendingBroadcastTimeoutMessage;
            if (pending) {
                movePendingBroadcastToProxyList(queue.mOrderedBroadcasts, awareOrderedBrs, ((HwBroadcastRecord) awareOrderedBrs.get(0)).getCurrentReceiverPid());
            }
            if (AwareBroadcastDebug.getFilterDebug()) {
                String str2 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("unproxy ");
                stringBuilder.append(queue.mQueueName);
                stringBuilder.append(" pending:");
                stringBuilder.append(pending);
                stringBuilder.append(" Broadcast pkg Orded Broadcasts (");
                stringBuilder.append(awareOrderedBrs);
                stringBuilder.append(")");
                AwareLog.i(str2, stringBuilder.toString());
            }
            count = awareOrderedBrs.size();
            for (int i2 = 0; i2 < count; i2++) {
                if (pending) {
                    queue.mOrderedBroadcasts.add(i2 + 1, ((HwBroadcastRecord) awareOrderedBrs.get(i2)).getBroadcastRecord());
                } else {
                    queue.mOrderedBroadcasts.add(i2, ((HwBroadcastRecord) awareOrderedBrs.get(i2)).getBroadcastRecord());
                }
            }
        }
        if (queue != null) {
            queue.scheduleBroadcastsLocked();
        }
    }

    public static boolean isSameReceiver(Object o1, Object o2) {
        if (o1 == null || o2 == null) {
            return false;
        }
        if (o1 != o2) {
            if ((o1 instanceof BroadcastFilter) && (o2 instanceof BroadcastFilter)) {
                if (((BroadcastFilter) o1).receiverList != ((BroadcastFilter) o2).receiverList) {
                    return false;
                }
            } else if (!(o1 instanceof ResolveInfo) || !(o2 instanceof ResolveInfo)) {
                return false;
            } else {
                ResolveInfo info1 = (ResolveInfo) o1;
                ResolveInfo info2 = (ResolveInfo) o2;
                if (!(info1.activityInfo == info2.activityInfo && info1.providerInfo == info2.providerInfo && info1.serviceInfo == info2.serviceInfo)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void movePendingBroadcastToProxyList(ArrayList<BroadcastRecord> orderedBroadcasts, ArrayList<HwBroadcastRecord> orderedProxyBroadcasts, int pid) {
        int i = pid;
        ArrayList<HwBroadcastRecord> arrayList;
        if (orderedProxyBroadcasts.size() == 0 || orderedBroadcasts.size() == 0) {
            arrayList = orderedProxyBroadcasts;
            return;
        }
        BroadcastRecord r = (BroadcastRecord) orderedBroadcasts.get(0);
        List<Object> needMoveReceivers = new ArrayList();
        List<Object> receivers = r.receivers;
        List<Object> list;
        List<Object> list2;
        if (receivers == null) {
            list = receivers;
            arrayList = orderedProxyBroadcasts;
        } else if (i <= 0) {
            list2 = needMoveReceivers;
            list = receivers;
            arrayList = orderedProxyBroadcasts;
        } else {
            List<Object> needMoveReceivers2;
            List<Object> receivers2;
            int recIdx;
            int numReceivers;
            int recIdx2 = r.nextReceiver;
            int numReceivers2 = receivers.size();
            int i2 = recIdx2;
            while (i2 < numReceivers2) {
                int i3;
                Object target = receivers.get(i2);
                int resolvePid = getResolvePid(r.queue, target);
                if (resolvePid <= 0 || resolvePid != i) {
                    needMoveReceivers2 = needMoveReceivers;
                    receivers2 = receivers;
                    recIdx = recIdx2;
                    numReceivers = numReceivers2;
                    i3 = i2;
                    arrayList = orderedProxyBroadcasts;
                } else {
                    needMoveReceivers.add(target);
                    List<Object> receiver = new ArrayList();
                    receiver.add(target);
                    recIdx = recIdx2;
                    numReceivers = numReceivers2;
                    receivers2 = receivers;
                    needMoveReceivers2 = needMoveReceivers;
                    i3 = i2;
                    arrayList = orderedProxyBroadcasts;
                    arrayList.add(new HwBroadcastRecord(new BroadcastRecord(r.queue, r.intent, r.callerApp, r.callerPackage, r.callingPid, r.callingUid, r.callerInstantApp, r.resolvedType, r.requiredPermissions, r.appOp, r.options, receiver, null, r.resultCode, r.resultData, r.resultExtras, r.ordered, r.sticky, r.initialSticky, r.userId)));
                }
                i2 = i3 + 1;
                recIdx2 = recIdx;
                numReceivers2 = numReceivers;
                receivers = receivers2;
                needMoveReceivers = needMoveReceivers2;
                i = pid;
                ArrayList<BroadcastRecord> arrayList2 = orderedBroadcasts;
            }
            needMoveReceivers2 = needMoveReceivers;
            receivers2 = receivers;
            recIdx = recIdx2;
            numReceivers = numReceivers2;
            arrayList = orderedProxyBroadcasts;
            list2 = needMoveReceivers2;
            if (list2.size() > 0) {
                receivers2.removeAll(list2);
                if (AwareBroadcastDebug.getFilterDebug()) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unproxy, moving receivers in Ordered Broadcasts (");
                    stringBuilder.append(r);
                    stringBuilder.append(") to proxyList, Move receivers : ");
                    stringBuilder.append(list2);
                    AwareLog.i(str, stringBuilder.toString());
                }
            }
        }
    }

    private static int getResolvePid(BroadcastQueue queue, Object target) {
        if (target instanceof ResolveInfo) {
            ResolveInfo info = (ResolveInfo) target;
            ProcessRecord app = queue.mService.getProcessRecordLocked(info.activityInfo.processName, info.activityInfo.applicationInfo.uid, false);
            if (app != null) {
                return app.pid;
            }
        }
        return -1;
    }

    private void trackBrFilter(String action, AwareProcessInfo pInfo, int policy, boolean droped, Object receiver) {
        if (AwareBroadcastDumpRadar.isBetaUser() && getBrDumpRadar() != null) {
            if (!droped) {
                if (pInfo.mPid == this.mSysServicePid) {
                    AwareBroadcastDumpRadar.increatSsNoDropCount(1);
                } else if (pInfo.mProcInfo.mCurAdj < 0) {
                    AwareBroadcastDumpRadar.increatPerAppNoDropCount(1);
                }
            }
            if (pInfo.mPid == this.mSysServicePid) {
                pInfo.mProcInfo.mProcessName = "system_server";
            }
            String id = "unknow";
            if (receiver instanceof BroadcastFilter) {
                id = AwareBroadcastRegister.removeBRIdUncommonData(((BroadcastFilter) receiver).getIdentifier());
            }
            String processType = "unknow";
            if (droped) {
                processType = "drop";
            } else {
                processType = "nodrop";
            }
            String key = new StringBuilder();
            key.append(action);
            key.append(",");
            key.append(pInfo.mProcInfo.mProcessName);
            key.append(",");
            key.append(id);
            key.append(",");
            key.append(pInfo.getState());
            key.append(",");
            key.append(policy);
            key.append(",");
            key.append(processType);
            this.mBrDumpRadar.addBrFilterDetail(key.toString());
        }
    }
}
