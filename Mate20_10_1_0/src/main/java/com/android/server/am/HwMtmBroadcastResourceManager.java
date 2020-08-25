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
    private HashMap<ReceiverList, String> mBRIdMap = new HashMap<>();
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
        Iterator<String> actions;
        if (filter == null) {
            AwareLog.e(TAG, "iawareCheckCombinedConditon param error!");
            return;
        }
        if (this.mAwareBRRegister == null) {
            this.mAwareBRRegister = AwareBroadcastRegister.getInstance();
        }
        String acId = this.mAwareBRRegister.findMatchedAssembleConditionId(filter.getIdentifier());
        if (acId != null && (actions = filter.actionsIterator()) != null) {
            while (actions.hasNext()) {
                String condition = this.mAwareBRRegister.getBRAssembleCondition(acId, actions.next());
                if (condition != null) {
                    filter.addCategory(condition);
                    if (AwareBroadcastDebug.getDebugDetail()) {
                        AwareLog.i(TAG, "brreg: add condition: " + condition + " for " + filter.getIdentifier());
                    }
                }
            }
        }
    }

    public void iawareCountDuplicatedReceiver(boolean isRegister, ReceiverList rl, IntentFilter filter) {
        String brId;
        if (rl == null || (isRegister && filter == null)) {
            AwareLog.e(TAG, "iawareCountDuplicatedReceiver param error!");
            return;
        }
        if (this.mAwareBRRegister == null) {
            this.mAwareBRRegister = AwareBroadcastRegister.getInstance();
        }
        if (isRegister) {
            brId = filter.getIdentifier();
            this.mBRIdMap.put(rl, brId);
        } else {
            brId = this.mBRIdMap.remove(rl);
        }
        int brCount = this.mAwareBRRegister.countReceiverRegister(isRegister, brId);
        if (AwareBroadcastDebug.getDebugDetail()) {
            StringBuilder sb = new StringBuilder();
            sb.append("brreg: regCounter, ");
            sb.append(isRegister ? "register" : "unregister");
            sb.append(" brId: ");
            sb.append(brId);
            sb.append(" count:");
            sb.append(brCount);
            AwareLog.i(TAG, sb.toString());
        }
    }

    public boolean iawareNeedSkipBroadcastSend(String action, Object[] data) {
        if (action == null || data == null) {
            AwareLog.e(TAG, "iawareNeedSkipBroadcastSend param error!");
            return false;
        } else if (!BroadcastExFeature.isFeatureEnabled(2)) {
            if (AwareBroadcastDebug.getDebugDetail()) {
                AwareLog.i(TAG, "BrSend feature not enabled!");
            }
            return false;
        } else {
            if (this.mAwareBRSend == null) {
                this.mAwareBRSend = AwareBroadcastSend.getInstance();
            }
            if (this.mAwareBRSend.setData(action, data)) {
                return this.mAwareBRSend.needSkipBroadcastSend(action);
            }
            return false;
        }
    }

    private boolean processBroadcastScheduler(boolean isParallel, BroadcastRecord r, Object target) {
        String packageName;
        if (isParallel || r == null || target == null || !(target instanceof ResolveInfo)) {
            AwareLog.e(TAG, "iaware_brjob processBroadcastScheduler param error!");
            return false;
        }
        ResolveInfo info = (ResolveInfo) target;
        ComponentInfo ci = info.getComponentInfo();
        if (ci == null || ci.applicationInfo == null) {
            packageName = null;
        } else {
            packageName = ci.applicationInfo.packageName;
        }
        String action = r.intent.getAction();
        IntentFilter filter = info.filter;
        if (filter == null || filter.countActionFilters() <= 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("iaware_brjob not process: ");
            sb.append(info);
            sb.append(", filter: ");
            Object obj = "null";
            sb.append(filter == null ? obj : filter);
            sb.append(", count: ");
            if (filter != null) {
                obj = Integer.valueOf(filter.countActionFilters());
            }
            sb.append(obj);
            AwareLog.w(TAG, sb.toString());
            StartupByBroadcastRecorder.getInstance().recordStartupTimeByBroadcast(packageName, action, System.currentTimeMillis());
            trackImplicitBr(false, true, packageName, action);
        } else if (r.iawareCtrlType == 2) {
            StartupByBroadcastRecorder.getInstance().recordStartupTimeByBroadcast(packageName, action, System.currentTimeMillis());
            trackImplicitBr(true, true, packageName, action);
        } else if (r.queue == null || !enqueueBroadcastScheduler(isParallel, r, target)) {
            trackImplicitBr(false, true, packageName, action);
        } else {
            trackImplicitBr(true, false, packageName, action);
            AwareAppStartupPolicy policy = AwareAppStartupPolicy.self();
            if (policy != null) {
                policy.updateBroadJobCtrlBigData(packageName);
            }
            r.queue.finishReceiverLocked(r, r.resultCode, r.resultData, r.resultExtras, r.resultAbort, false);
            r.queue.scheduleBroadcastsLocked();
            r.state = 0;
            return true;
        }
        return false;
    }

    private boolean enqueueBroadcastScheduler(boolean isParallel, BroadcastRecord r, Object target) {
        if (getAwareJobSchedulerService() == null) {
            return false;
        }
        if (!isSystemApplication(target)) {
            AwareLog.w(TAG, "iaware_brjob not system app");
            return false;
        }
        IIntentReceiver resultTo = r.resultTo;
        if (r.resultTo != null) {
            AwareLog.d(TAG, "reset resultTo null");
            resultTo = null;
        }
        List<Object> receiver = new ArrayList<>();
        receiver.add(target);
        return this.mAwareJobSchedulerService.schedule(new HwBroadcastRecord(new BroadcastRecord(r.queue, r.intent, r.callerApp, r.callerPackage, r.callingPid, r.callingUid, r.callerInstantApp, r.resolvedType, r.requiredPermissions, r.appOp, r.options, receiver, resultTo, r.resultCode, r.resultData, r.resultExtras, r.ordered, r.sticky, r.initialSticky, r.userId, r.allowBackgroundActivityStarts, r.timeoutExempt)));
    }

    private boolean enqueueIawareProxyBroacast(boolean isParallel, BroadcastRecord r, Object target) {
        String action;
        if (isAbnormalParameters(isParallel, r, target) || getIawareBrPolicy() == null || r.iawareCtrlType == 1 || !this.mIawareBrPolicy.isProxyedAllowedCondition() || r.callingPid != this.mSysServicePid || isThirdOrKeyBroadcast()) {
            return false;
        }
        String pkg = getPkg(target);
        int pid = getPid(target);
        int uid = getUid(target);
        if (isAbnormalValue(pkg, pid, uid) || pid == this.mSysServicePid) {
            return false;
        }
        if (r.intent != null) {
            action = r.intent.getAction();
        } else {
            action = null;
        }
        if (!this.mIawareBrPolicy.isNotProxySysPkg(pkg, action)) {
            if (!isInstrumentationApp(target)) {
                boolean isSystemApp = isSystemApplication(target);
                int curProcState = getcurProcState(target);
                int curAdj = getProcessCurrentAdj(target);
                if (-1 != curProcState) {
                    if (-10000 != curAdj) {
                        if (!this.mIawareBrPolicy.shouldIawareProxyBroadcast(action, r.callingPid, uid, pid, pkg)) {
                            return false;
                        }
                        List<Object> receiver = new ArrayList<>();
                        receiver.add(target);
                        BroadcastRecord proxyBR = new BroadcastRecord(r.queue, r.intent, r.callerApp, r.callerPackage, r.callingPid, r.callingUid, r.callerInstantApp, r.resolvedType, r.requiredPermissions, r.appOp, r.options, receiver, r.resultTo, r.resultCode, r.resultData, r.resultExtras, r.ordered, r.sticky, r.initialSticky, r.userId, r.allowBackgroundActivityStarts, r.timeoutExempt);
                        proxyBR.dispatchClockTime = r.dispatchClockTime;
                        proxyBR.dispatchTime = r.dispatchTime;
                        proxyBR.iawareCtrlType = 1;
                        HwBroadcastRecord hwBr = new HwBroadcastRecord(proxyBR);
                        hwBr.setReceiverUid(uid);
                        hwBr.setReceiverPid(pid);
                        hwBr.setReceiverCurAdj(curAdj);
                        hwBr.setReceiverPkg(pkg);
                        hwBr.setSysApp(isSystemApp);
                        hwBr.setReceiverCurProcState(curProcState);
                        return this.mIawareBrPolicy.enqueueIawareProxyBroacast(isParallel, hwBr);
                    }
                }
                return false;
            }
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
        return !isParallel || r == null || target == null;
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
            BroadcastQueue queue = parallelList.get(0).getBroacastQueue();
            synchronized (queue.mService) {
                int listSize = parallelList.size();
                for (int i = 0; i < listSize; i++) {
                    queue.mParallelBroadcasts.add(i, parallelList.get(i).getBroadcastRecord());
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
        boolean systemApp = false;
        if (target instanceof BroadcastFilter) {
            BroadcastFilter filter = (BroadcastFilter) target;
            if (filter.receiverList != null && filter.receiverList.app != null) {
                int flags = filter.receiverList.app.info.flags;
                int privateFlags = filter.receiverList.app.info.privateFlags;
                if (!((flags & 1) == 0 && (flags & 128) == 0 && (privateFlags & 8) == 0)) {
                    systemApp = true;
                }
                return systemApp;
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
                    systemApp = true;
                }
                return systemApp;
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
            if (!(filter.receiverList == null || filter.receiverList.app == null || filter.receiverList.app.mInstr == null)) {
                instrumentationApp = true;
                if (AwareBroadcastDebug.getDebugDetail()) {
                    AwareLog.d(TAG, "instrumentation app do not proxy!");
                }
            }
        }
        return instrumentationApp;
    }

    private boolean isThirdOrKeyBroadcast() {
        return !MemoryConstant.MEM_REPAIR_CONSTANT_BG.equals(this.mQueue.mQueueName) && !MemoryConstant.MEM_REPAIR_CONSTANT_FG.equals(this.mQueue.mQueueName);
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
                this.mBrDumpRadar.trackBrFlowSpeed(enqueue, isProxyed, !policy.isSpeedNoCtrol(), true ^ policy.isScreenOff());
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
        BroadcastRecord r;
        if (hwBr != null && (r = hwBr.getBroadcastRecord()) != null && r.queue != null) {
            synchronized (r.queue.mService) {
                r.iawareCtrlType = 2;
                r.queue.enqueueOrderedBroadcastLocked(r);
                r.queue.scheduleBroadcastsLocked();
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
            return filter.receiverList.app.getCurProcState();
        }
        if (!AwareBroadcastDebug.getDebugDetail()) {
            return -1;
        }
        AwareLog.d(TAG, "getProcessCurrentAdj BroadcastFilter: filter something is null");
        return -1;
    }

    private boolean isAbnormalValue(String pkg, int pid, int uid) {
        return pkg == null || pid == -1 || uid == -1;
    }

    private int filterRegisteredReceiver(Intent intent, BroadcastFilter filter, AwareProcessInfo pInfo) {
        if (filter.countActionFilters() > 0 && ((pInfo.procProcInfo.mType == 2 || pInfo.procPid == this.mSysServicePid) && this.mIawareBrPolicy.assemFilterBr(intent, filter))) {
            return AwareBroadcastPolicy.BrCtrlType.DISCARDBR.ordinal();
        }
        if (pInfo.procPid == this.mSysServicePid || pInfo.procProcInfo.mCurAdj < 0) {
            return AwareBroadcastPolicy.BrCtrlType.NONE.ordinal();
        }
        int configListPolicy = getPolicyFromConfigList(intent, filter.packageName, pInfo);
        if (configListPolicy != -1) {
            return configListPolicy;
        }
        int policy = this.mIawareBrPolicy.filterBr(intent, pInfo);
        if (AwareBroadcastDebug.getFilterDebug()) {
            AwareLog.i(TAG, "iaware_brFilter : reg policy:" + policy + ", pkgname:" + filter.packageName + ", action:" + intent.getAction() + ", proc state:" + pInfo.getState() + ", proc type:" + pInfo.procProcInfo.mType);
        }
        return policy;
    }

    private int filterResolveInfo(Intent intent, ResolveInfo resolveInfo, AwareProcessInfo pInfo) {
        int configListPolicy = getPolicyFromConfigList(intent, resolveInfo.activityInfo.applicationInfo.packageName, pInfo);
        if (configListPolicy != -1) {
            if (AwareBroadcastDebug.getFilterDebug()) {
                AwareLog.i(TAG, "iaware_brFilter resolve config policy: " + configListPolicy + ", pkgname:" + resolveInfo.activityInfo.applicationInfo.packageName + ", action:" + intent.getAction());
            }
            return configListPolicy;
        }
        int policy = this.mIawareBrPolicy.filterBr(intent, pInfo);
        if (AwareBroadcastDebug.getFilterDebug()) {
            AwareLog.i(TAG, "iaware_brFilter : resolve policy:" + policy + ", pkgname:" + resolveInfo.activityInfo.applicationInfo.packageName + ", action:" + intent.getAction() + ", proc state:" + pInfo.getState() + ", proc type:" + pInfo.procProcInfo.mType);
        }
        return policy;
    }

    public void iawareFilterBroadcast(Intent intent, ProcessRecord callerApp, String callerPackage, int callingPid, int callingUid, boolean callerInstantApp, String resolvedType, String[] requiredPermissions, int appOp, BroadcastOptions options, List receivers, List<BroadcastFilter> registeredReceivers, IIntentReceiver resultTo, int resultCode, String resultData, Bundle resultExtras, boolean ordered, boolean sticky, boolean initialSticky, int userId, boolean _allowBackgroundActivityStarts, boolean timeoutExempt) {
        IIntentReceiver proxyResultTo;
        if (isCondtionSatisfy(intent)) {
            this.mIawareBrPolicy.getStateFromSendBr(intent);
            if (!ordered || resultTo == null) {
                proxyResultTo = resultTo;
            } else {
                if (AwareBroadcastDebug.getFilterDebug()) {
                    AwareLog.i(TAG, "reset resultTo null");
                }
                proxyResultTo = null;
            }
            processRegisteredReceiver(intent, callerApp, callerPackage, callingPid, callingUid, callerInstantApp, resolvedType, requiredPermissions, appOp, options, receivers, registeredReceivers, proxyResultTo, resultCode, resultData, resultExtras, ordered, sticky, initialSticky, userId, _allowBackgroundActivityStarts, timeoutExempt);
            processResolveReceiver(intent, callerApp, callerPackage, callingPid, callingUid, callerInstantApp, resolvedType, requiredPermissions, appOp, options, receivers, registeredReceivers, proxyResultTo, resultCode, resultData, resultExtras, ordered, sticky, initialSticky, userId, _allowBackgroundActivityStarts, timeoutExempt);
        }
    }

    private void processRegisteredReceiver(Intent intent, ProcessRecord callerApp, String callerPackage, int callingPid, int callingUid, boolean callerInstantApp, String resolvedType, String[] requiredPermissions, int appOp, BroadcastOptions options, List receivers, List<BroadcastFilter> registeredReceivers, IIntentReceiver resultTo, int resultCode, String resultData, Bundle resultExtras, boolean ordered, boolean sticky, boolean initialSticky, int userId, boolean _allowBackgroundActivityStarts, boolean timeoutExempt) {
        if (registeredReceivers != null) {
            String action = intent.getAction();
            AwareBroadcastDumpRadar.increatBrBeforeCount(registeredReceivers.size());
            Iterator<BroadcastFilter> regIterator = registeredReceivers.iterator();
            while (regIterator.hasNext()) {
                BroadcastFilter filter = regIterator.next();
                AwareProcessInfo pInfo = getProcessInfo(filter);
                if (pInfo == null) {
                    AwareBroadcastDumpRadar.increatBrNoProcessCount(1);
                } else {
                    int policy = filterRegisteredReceiver(intent, filter, pInfo);
                    if (policy == AwareBroadcastPolicy.BrCtrlType.DISCARDBR.ordinal()) {
                        regIterator.remove();
                        AwareBroadcastDumpRadar.increatBrAfterCount(1);
                        trackBrFilter(action, pInfo, policy, true, filter);
                    } else if (policy == AwareBroadcastPolicy.BrCtrlType.CACHEBR.ordinal()) {
                        List<Object> cacheReceiver = new ArrayList<>();
                        cacheReceiver.add(filter);
                        HwBroadcastRecord hwBr = new HwBroadcastRecord(new BroadcastRecord(this.mQueue, intent, callerApp, callerPackage, callingPid, callingUid, callerInstantApp, resolvedType, requiredPermissions, appOp, options, cacheReceiver, resultTo, resultCode, resultData, resultExtras, ordered, sticky, initialSticky, userId, _allowBackgroundActivityStarts, timeoutExempt));
                        hwBr.setCurrentReceiverPid(pInfo.procPid);
                        hwBr.setReceiverPkg(filter.packageName);
                        boolean trim = this.mIawareBrPolicy.awareTrimAndEnqueueBr(!ordered, hwBr, false, pInfo.procPid, filter.packageName);
                        regIterator.remove();
                        trackBrFilter(action, pInfo, policy, trim, filter);
                    } else if (policy == AwareBroadcastPolicy.BrCtrlType.NONE.ordinal()) {
                        trackBrFilter(action, pInfo, policy, false, filter);
                    }
                }
            }
        }
    }

    private void processResolveReceiver(Intent intent, ProcessRecord callerApp, String callerPackage, int callingPid, int callingUid, boolean callerInstantApp, String resolvedType, String[] requiredPermissions, int appOp, BroadcastOptions options, List receivers, List<BroadcastFilter> list, IIntentReceiver resultTo, int resultCode, String resultData, Bundle resultExtras, boolean ordered, boolean sticky, boolean initialSticky, int userId, boolean _allowBackgroundActivityStarts, boolean timeoutExempt) {
        if (receivers != null) {
            String action = intent.getAction();
            AwareBroadcastDumpRadar.increatBrBeforeCount(receivers.size());
            Iterator<ResolveInfo> iterator = receivers.iterator();
            while (iterator.hasNext()) {
                ResolveInfo resolveInfo = iterator.next();
                AwareProcessInfo pInfo = getProcessInfo(resolveInfo);
                if (pInfo == null) {
                    AwareBroadcastDumpRadar.increatBrNoProcessCount(1);
                } else {
                    int policy = filterResolveInfo(intent, resolveInfo, pInfo);
                    if (policy == AwareBroadcastPolicy.BrCtrlType.DISCARDBR.ordinal()) {
                        iterator.remove();
                        AwareBroadcastDumpRadar.increatBrAfterCount(1);
                        trackBrFilter(action, pInfo, policy, true, resolveInfo);
                    } else if (policy == AwareBroadcastPolicy.BrCtrlType.CACHEBR.ordinal()) {
                        List<Object> cacheReceiver = new ArrayList<>();
                        cacheReceiver.add(resolveInfo);
                        HwBroadcastRecord hwBr = new HwBroadcastRecord(new BroadcastRecord(this.mQueue, intent, callerApp, callerPackage, callingPid, callingUid, callerInstantApp, resolvedType, requiredPermissions, appOp, options, cacheReceiver, resultTo, resultCode, resultData, resultExtras, ordered, sticky, initialSticky, userId, _allowBackgroundActivityStarts, timeoutExempt));
                        hwBr.setCurrentReceiverPid(pInfo.procPid);
                        hwBr.setReceiverPkg(resolveInfo.activityInfo.applicationInfo.packageName);
                        boolean trim = this.mIawareBrPolicy.awareTrimAndEnqueueBr(false, hwBr, false, pInfo.procPid, resolveInfo.activityInfo.applicationInfo.packageName);
                        iterator.remove();
                        trackBrFilter(action, pInfo, policy, trim, resolveInfo);
                    } else if (policy == AwareBroadcastPolicy.BrCtrlType.NONE.ordinal()) {
                        trackBrFilter(action, pInfo, policy, false, resolveInfo);
                    }
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
                awareProcessInfo.procProcInfo.mCurAdj = getProcessCurrentAdj(target);
            }
        } else if (target instanceof ResolveInfo) {
            ResolveInfo info = (ResolveInfo) target;
            ProcessRecord app = this.mQueue.mService.getProcessRecordLocked(info.activityInfo.processName, info.activityInfo.applicationInfo.uid, false);
            if (app != null) {
                if (app.mInstr != null) {
                    return null;
                }
                awareProcessInfo = ProcessInfoCollector.getInstance().getAwareProcessInfo(app.pid);
                if (awareProcessInfo != null) {
                    awareProcessInfo.procProcInfo.mCurAdj = app.curAdj;
                }
            }
        }
        return awareProcessInfo;
    }

    private int getPolicyFromConfigList(Intent intent, String pkgName, AwareProcessInfo pInfo) {
        if (BroadcastExFeature.isBrFilterWhiteList(pkgName) || BroadcastExFeature.isBrFilterWhiteApp(intent.getAction(), pkgName)) {
            return AwareBroadcastPolicy.BrCtrlType.NONE.ordinal();
        }
        if (pInfo.getState() != 1 || !BroadcastExFeature.isBrFilterBlackApp(intent.getAction(), pkgName)) {
            return -1;
        }
        return AwareBroadcastPolicy.BrCtrlType.CACHEBR.ordinal();
    }

    private boolean isCondtionSatisfy(Intent intent) {
        if (intent == null || intent.getAction() == null || !BroadcastExFeature.isFeatureEnabled(1) || getIawareBrPolicy() == null) {
            return false;
        }
        if (intent.getPackage() == null && intent.getComponent() == null) {
            return true;
        }
        if (AwareBroadcastDebug.getFilterDebug()) {
            AwareLog.i(TAG, "it is explicitly : " + intent.getAction());
        }
        return false;
    }

    public static void unProxyCachedBr(ArrayList<HwBroadcastRecord> awareParallelBrs, ArrayList<HwBroadcastRecord> awareOrderedBrs) {
        BroadcastQueue queue = null;
        if (awareParallelBrs != null && awareParallelBrs.size() > 0) {
            queue = awareParallelBrs.get(0).getBroacastQueue();
            if (AwareBroadcastDebug.getFilterDebug()) {
                AwareLog.i(TAG, "unproxy " + queue.mQueueName + " Broadcast pkg Parallel Broadcasts (" + awareParallelBrs + ")");
            }
            int count = awareParallelBrs.size();
            for (int i = 0; i < count; i++) {
                queue.mParallelBroadcasts.add(i, awareParallelBrs.get(i).getBroadcastRecord());
            }
        }
        if (awareOrderedBrs != null && awareOrderedBrs.size() > 0) {
            if (queue == null) {
                queue = awareOrderedBrs.get(0).getBroacastQueue();
            }
            boolean pending = queue.mPendingBroadcastTimeoutMessage;
            if (pending) {
                movePendingBroadcastToProxyList(queue.mDispatcher.getActiveBroadcastLocked(), awareOrderedBrs, awareOrderedBrs.get(0).getCurrentReceiverPid());
            }
            if (AwareBroadcastDebug.getFilterDebug()) {
                AwareLog.i(TAG, "unproxy " + queue.mQueueName + " pending:" + pending + " Broadcast pkg Orded Broadcasts (" + awareOrderedBrs + ")");
            }
            int orderedBroadcastsSize = queue.mDispatcher.getOrderedBroadcastsSize();
            int count2 = awareOrderedBrs.size();
            for (int i2 = 0; i2 < count2; i2++) {
                if (!pending || orderedBroadcastsSize <= 0) {
                    queue.mDispatcher.enqueueOrderedBroadcastLocked(i2, awareOrderedBrs.get(i2).getBroadcastRecord());
                } else {
                    queue.mDispatcher.enqueueOrderedBroadcastLocked(i2 + 1, awareOrderedBrs.get(i2).getBroadcastRecord());
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
        if (o1 == o2) {
            return true;
        }
        if (!(o1 instanceof BroadcastFilter) || !(o2 instanceof BroadcastFilter)) {
            if (!(o1 instanceof ResolveInfo) || !(o2 instanceof ResolveInfo)) {
                return false;
            }
            ResolveInfo info1 = (ResolveInfo) o1;
            ResolveInfo info2 = (ResolveInfo) o2;
            if (info1.activityInfo == info2.activityInfo && info1.providerInfo == info2.providerInfo && info1.serviceInfo == info2.serviceInfo) {
                return true;
            }
            return false;
        } else if (((BroadcastFilter) o1).receiverList != ((BroadcastFilter) o2).receiverList) {
            return false;
        } else {
            return true;
        }
    }

    private static void movePendingBroadcastToProxyList(BroadcastRecord bR, ArrayList<HwBroadcastRecord> orderedProxyBroadcasts, int pid) {
        int i = pid;
        if (orderedProxyBroadcasts.size() == 0) {
            return;
        }
        if (bR != null) {
            List<Object> needMoveReceivers = new ArrayList<>();
            List<Object> receivers = bR.receivers;
            if (receivers == null) {
                return;
            }
            if (i > 0) {
                int recIdx = bR.nextReceiver;
                int numReceivers = receivers.size();
                int i2 = recIdx;
                while (i2 < numReceivers) {
                    Object target = receivers.get(i2);
                    int resolvePid = getResolvePid(bR.queue, target);
                    if (resolvePid > 0 && resolvePid == i) {
                        needMoveReceivers.add(target);
                        List<Object> receiver = new ArrayList<>();
                        receiver.add(target);
                        orderedProxyBroadcasts.add(new HwBroadcastRecord(new BroadcastRecord(bR.queue, bR.intent, bR.callerApp, bR.callerPackage, bR.callingPid, bR.callingUid, bR.callerInstantApp, bR.resolvedType, bR.requiredPermissions, bR.appOp, bR.options, receiver, (IIntentReceiver) null, bR.resultCode, bR.resultData, bR.resultExtras, bR.ordered, bR.sticky, bR.initialSticky, bR.userId, bR.allowBackgroundActivityStarts, bR.timeoutExempt)));
                    }
                    i2++;
                    i = pid;
                }
                if (needMoveReceivers.size() > 0) {
                    receivers.removeAll(needMoveReceivers);
                    if (AwareBroadcastDebug.getFilterDebug()) {
                        AwareLog.i(TAG, "unproxy, moving receivers in Ordered Broadcasts (" + bR + ") to proxyList, Move receivers : " + needMoveReceivers);
                    }
                }
            }
        }
    }

    private static int getResolvePid(BroadcastQueue queue, Object target) {
        if (!(target instanceof ResolveInfo)) {
            return -1;
        }
        ResolveInfo info = (ResolveInfo) target;
        ProcessRecord app = queue.mService.getProcessRecordLocked(info.activityInfo.processName, info.activityInfo.applicationInfo.uid, false);
        if (app != null) {
            return app.pid;
        }
        return -1;
    }

    private void trackBrFilter(String action, AwareProcessInfo pInfo, int policy, boolean droped, Object receiver) {
        String processType;
        if (AwareBroadcastDumpRadar.isBetaUser() && getBrDumpRadar() != null) {
            if (!droped) {
                if (pInfo.procPid == this.mSysServicePid) {
                    AwareBroadcastDumpRadar.increatSsNoDropCount(1);
                } else if (pInfo.procProcInfo.mCurAdj < 0) {
                    AwareBroadcastDumpRadar.increatPerAppNoDropCount(1);
                }
            }
            if (pInfo.procPid == this.mSysServicePid) {
                pInfo.procProcInfo.mProcessName = "system_server";
            }
            String id = "unknow";
            if (receiver instanceof BroadcastFilter) {
                id = AwareBroadcastRegister.removeBRIdUncommonData(((BroadcastFilter) receiver).getIdentifier());
            }
            if (droped) {
                processType = "drop";
            } else {
                processType = "nodrop";
            }
            this.mBrDumpRadar.addBrFilterDetail(action + "," + pInfo.procProcInfo.mProcessName + "," + id + "," + pInfo.getState() + "," + policy + "," + processType);
        }
    }
}
