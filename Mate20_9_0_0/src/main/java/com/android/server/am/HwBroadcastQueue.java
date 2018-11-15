package com.android.server.am;

import android.app.AppGlobals;
import android.app.BroadcastOptions;
import android.common.HwFrameworkFactory;
import android.common.HwFrameworkMonitor;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Flog;
import android.util.Slog;
import com.android.server.HwServiceFactory;
import com.huawei.pgmng.common.Utils;
import com.huawei.pgmng.log.LogPower;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class HwBroadcastQueue extends BroadcastQueue {
    static final boolean DEBUG_CONSUMPTION = false;
    private static final int MIN_SEND_MSG_INTERVAL = 1000;
    private static final String MMS_PACKAGE_NAME = "com.android.mms";
    static final String TAG = "HwBroadcastQueue";
    private static final int TYPE_CONFIG_APP_ADD_ACTION = 6;
    private static final int TYPE_CONFIG_APP_REMOVE_ACTION = 7;
    private static final int TYPE_CONFIG_CLEAR = 0;
    private static final int TYPE_CONFIG_DROP_BC_ACTION = 2;
    private static final int TYPE_CONFIG_DROP_BC_BY_PID = 3;
    private static final int TYPE_CONFIG_MAX_PROXY_BC = 4;
    private static final int TYPE_CONFIG_PROXY_BC_ACTION = 1;
    private static final int TYPE_CONFIG_SAME_KIND_ACTION = 5;
    private static boolean mProxyFeature = true;
    private int MAX_PROXY_BROADCAST = 10;
    private boolean enableUploadRadar = true;
    private final HashMap<String, Set<String>> mActionExcludePkgs = new HashMap();
    private ArrayList<String> mActionWhiteList = new ArrayList();
    private final HashMap<String, ArrayList<String>> mAppAddProxyActions = new HashMap();
    private final HashMap<String, ArrayList<String>> mAppDropActions = new HashMap();
    private final HashMap<String, ArrayList<String>> mAppProxyActions = new HashMap();
    private final HashMap<String, ArrayList<String>> mAppRemoveProxyActions = new HashMap();
    private IBinder mAwareService = null;
    private HwFrameworkMonitor mBroadcastMonitor = HwFrameworkFactory.getHwFrameworkMonitor();
    private HwBroadcastRadarUtil mBroadcastRadarUtil;
    private ArrayList<BroadcastRecord> mCopyOrderedBroadcasts;
    private AbsHwMtmBroadcastResourceManager mHwMtmBroadcastResourceManager = null;
    private long mLastTime = SystemClock.uptimeMillis();
    final ArrayList<BroadcastRecord> mOrderedPendingBroadcasts = new ArrayList();
    final ArrayList<BroadcastRecord> mParallelPendingBroadcasts = new ArrayList();
    private final HashMap<Integer, ArrayList<String>> mProcessDropActions = new HashMap();
    private final ArrayList<String> mProxyActions = new ArrayList();
    final ArrayList<String> mProxyBroadcastPkgs = new ArrayList();
    final HashMap<String, Integer> mProxyPkgsCount = new HashMap();
    private HashMap<String, BroadcastRadarRecord> mRadarBroadcastMap;
    HashMap<String, String> mSameKindsActionList = new HashMap<String, String>() {
        {
            put("android.intent.action.SCREEN_ON", "android.intent.action.SCREEN_OFF");
        }
    };

    static class BroadcastRadarRecord {
        String actionName;
        int count;
        String packageName;

        public BroadcastRadarRecord() {
            this.actionName = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            this.packageName = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            this.count = 0;
        }

        public BroadcastRadarRecord(String actionName, String packageName, int count) {
            this.actionName = actionName;
            this.packageName = packageName;
            this.count = count;
        }
    }

    HwBroadcastQueue(ActivityManagerService service, Handler handler, String name, long timeoutPeriod, boolean allowDelayBehindServices) {
        super(service, handler, name, timeoutPeriod, allowDelayBehindServices);
        String closeSwitcher = SystemProperties.get("persist.sys.pg_close_action", null);
        if (closeSwitcher != null && closeSwitcher.contains("proxy_bc")) {
            Slog.w(TAG, "close proxy bc ");
            mProxyFeature = false;
        }
        this.mHwMtmBroadcastResourceManager = HwServiceFactory.getMtmBRManager(this);
        this.mCopyOrderedBroadcasts = new ArrayList();
        this.mBroadcastRadarUtil = new HwBroadcastRadarUtil();
        this.mRadarBroadcastMap = new HashMap();
        initActionWhiteList();
    }

    private static boolean isThirdParty(ProcessRecord process) {
        if (process == null || process.pid == ActivityManagerService.MY_PID || (process.info.flags & 1) != 0) {
            return false;
        }
        return true;
    }

    public void cleanupBroadcastLocked(ProcessRecord app) {
        boolean reschedule = false;
        if (isThirdParty(app)) {
            super.skipPendingBroadcastLocked(app.pid);
            for (int i = this.mOrderedBroadcasts.size() - 1; i >= 0; i--) {
                BroadcastRecord r = (BroadcastRecord) this.mOrderedBroadcasts.get(i);
                if (r.callerApp == app) {
                    boolean isSentToSelf = false;
                    List receivers = r.receivers;
                    int j = 0;
                    int N = receivers != null ? receivers.size() : 0;
                    while (j < N) {
                        ResolveInfo o = receivers.get(j);
                        if (o instanceof ResolveInfo) {
                            String appProcessName = app.processName;
                            String receiverProcessName = o.activityInfo.processName;
                            if (appProcessName != null && appProcessName.equals(receiverProcessName)) {
                                isSentToSelf = true;
                                break;
                            }
                        }
                        j++;
                    }
                    if (isSentToSelf) {
                        if (i == 0) {
                            cancelBroadcastTimeoutLocked();
                        }
                        this.mOrderedBroadcasts.remove(r);
                        reschedule = true;
                    }
                }
            }
            if (reschedule) {
                super.scheduleBroadcastsLocked();
            }
        }
    }

    private boolean canTrim(BroadcastRecord r1, BroadcastRecord r2) {
        if (r1 == null || r2 == null || r1.intent == null || r2.intent == null || r1.intent.getAction() == null || r2.intent.getAction() == null) {
            return false;
        }
        BroadcastFilter o1 = r1.receivers.get(0);
        BroadcastFilter o2 = r2.receivers.get(0);
        String pkg1 = getPkg(o1);
        String pkg2 = getPkg(o2);
        if (pkg1 != null && !pkg1.equals(pkg2)) {
            return false;
        }
        if (o1 != o2) {
            if ((o1 instanceof BroadcastFilter) && (o2 instanceof BroadcastFilter)) {
                if (o1.receiverList != o2.receiverList) {
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
        String action1 = r1.intent.getAction();
        String action2 = r2.intent.getAction();
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

    private void trimAndEnqueueBroadcast(boolean trim, boolean isParallel, BroadcastRecord r, String recevier) {
        String str;
        StringBuilder stringBuilder;
        int count = 0;
        if (this.mProxyPkgsCount.containsKey(recevier)) {
            count = ((Integer) this.mProxyPkgsCount.get(recevier)).intValue();
        }
        Iterator it;
        BroadcastRecord br;
        if (isParallel) {
            it = this.mParallelPendingBroadcasts.iterator();
            while (it.hasNext()) {
                br = (BroadcastRecord) it.next();
                if (trim && canTrim(r, br)) {
                    it.remove();
                    count--;
                    break;
                }
            }
            this.mParallelPendingBroadcasts.add(r);
        } else {
            it = this.mOrderedPendingBroadcasts.iterator();
            while (it.hasNext()) {
                br = (BroadcastRecord) it.next();
                if (trim && canTrim(r, br)) {
                    it.remove();
                    count--;
                    break;
                }
            }
            this.mOrderedPendingBroadcasts.add(r);
        }
        count++;
        this.mProxyPkgsCount.put(recevier, Integer.valueOf(count));
        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("trim and enqueue ");
            stringBuilder.append(this.mQueueName);
            stringBuilder.append(" Parallel:(");
            stringBuilder.append(this.mParallelPendingBroadcasts.size());
            stringBuilder.append(") Ordered:(");
            stringBuilder.append(this.mOrderedPendingBroadcasts.size());
            stringBuilder.append(")(");
            stringBuilder.append(r);
            stringBuilder.append(")");
            Slog.v(str, stringBuilder.toString());
        }
        if (count % this.MAX_PROXY_BROADCAST == 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("proxy max broadcasts, notify pg. recevier:");
            stringBuilder.append(recevier);
            Slog.i(str, stringBuilder.toString());
            notifyPG("overflow_bc", recevier, -1);
            if (count > this.MAX_PROXY_BROADCAST + 10) {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("warnning, proxy more broadcast, notify pg. recevier:");
                stringBuilder2.append(recevier);
                Slog.w(str, stringBuilder2.toString());
                notifyPG("overflow_exception", recevier, -1);
            }
        }
    }

    private boolean shouldProxy(String pkg, int pid) {
        boolean z = false;
        if (!mProxyFeature) {
            return false;
        }
        if (pkg != null) {
            z = this.mProxyBroadcastPkgs.contains(pkg);
        }
        return z;
    }

    private void notifyPG(String action, String pkg, int pid) {
        Utils.handleTimeOut(action, pkg, String.valueOf(pid));
    }

    private boolean shouldNotifyPG(String action, String receiverPkg) {
        if (action == null || receiverPkg == null) {
            return true;
        }
        ArrayList<String> proxyActions = (ArrayList) this.mProxyActions.clone();
        if (this.mAppProxyActions.containsKey(receiverPkg)) {
            proxyActions = (ArrayList) this.mAppProxyActions.get(receiverPkg);
        }
        ArrayList<String> addBCActions = null;
        if (this.mAppAddProxyActions.containsKey(receiverPkg)) {
            addBCActions = (ArrayList) this.mAppAddProxyActions.get(receiverPkg);
        }
        if (proxyActions != null && !proxyActions.contains(action) && (addBCActions == null || !addBCActions.contains(action))) {
            return true;
        }
        if (this.mActionExcludePkgs.containsKey(action)) {
            Set<String> pkgs = (Set) this.mActionExcludePkgs.get(action);
            if (pkgs != null && pkgs.contains(receiverPkg)) {
                return true;
            }
        }
        ArrayList<String> removeBCActions = null;
        if (this.mAppRemoveProxyActions.containsKey(receiverPkg)) {
            removeBCActions = (ArrayList) this.mAppRemoveProxyActions.get(receiverPkg);
        }
        if (removeBCActions == null || !removeBCActions.contains(action)) {
            return false;
        }
        return true;
    }

    /* JADX WARNING: Missing block: B:26:0x008f, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void reportMediaButtonToAware(BroadcastRecord r, Object target) {
        if (r != null && r.intent != null && target != null && "android.intent.action.MEDIA_BUTTON".equals(r.intent.getAction())) {
            long curTime = SystemClock.uptimeMillis();
            if (curTime - this.mLastTime >= 1000) {
                this.mLastTime = curTime;
                int uid = getUid(target);
                if (this.mAwareService == null) {
                    this.mAwareService = ServiceManager.getService("hwsysresmanager");
                }
                if (this.mAwareService != null) {
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    try {
                        data.writeInterfaceToken("android.rms.IHwSysResManager");
                        data.writeInt(uid);
                        this.mAwareService.transact(20017, data, reply, 0);
                        reply.readException();
                    } catch (RemoteException e) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("mAwareService ontransact ");
                        stringBuilder.append(e.getMessage());
                        Slog.e(str, stringBuilder.toString());
                    } catch (Throwable th) {
                        data.recycle();
                        reply.recycle();
                    }
                    data.recycle();
                    reply.recycle();
                } else {
                    Slog.e(TAG, "mAwareService is not start");
                }
            }
        }
    }

    public boolean enqueueProxyBroadcast(boolean isParallel, BroadcastRecord r, Object target) {
        boolean z = isParallel;
        BroadcastRecord broadcastRecord = r;
        Object obj = target;
        if (obj == null) {
            return false;
        }
        String pkg = getPkg(obj);
        int pid = getPid(obj);
        int uid = getUid(obj);
        if (pkg == null || !shouldProxy(pkg, pid)) {
            return false;
        }
        List<Object> receiver = new ArrayList();
        receiver.add(obj);
        IIntentReceiver resultTo = broadcastRecord.resultTo;
        if (!(z || broadcastRecord.resultTo == null)) {
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                Slog.v(TAG, "reset resultTo null");
            }
            resultTo = null;
        }
        IIntentReceiver resultTo2 = resultTo;
        String action = broadcastRecord.intent.getAction();
        boolean notify = shouldNotifyPG(action, pkg);
        if (notify) {
            LogPower.push(148, action, pkg, String.valueOf(pid), new String[]{broadcastRecord.callerPackage});
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("enqueueProxyBroadcast notify pg broadcast:");
                stringBuilder.append(action);
                stringBuilder.append(" pkg:");
                stringBuilder.append(pkg);
                stringBuilder.append(" pid:");
                stringBuilder.append(pid);
                stringBuilder.append(" uid:");
                stringBuilder.append(uid);
                Slog.v(str, stringBuilder.toString());
            }
        }
        BroadcastQueue broadcastQueue = broadcastRecord.queue;
        Intent intent = broadcastRecord.intent;
        ProcessRecord processRecord = broadcastRecord.callerApp;
        String str2 = broadcastRecord.callerPackage;
        int i = broadcastRecord.callingPid;
        int i2 = broadcastRecord.callingUid;
        int uid2 = uid;
        boolean z2 = broadcastRecord.callerInstantApp;
        int pid2 = pid;
        String str3 = broadcastRecord.resolvedType;
        String[] strArr = broadcastRecord.requiredPermissions;
        int i3 = broadcastRecord.appOp;
        String pkg2 = pkg;
        BroadcastOptions broadcastOptions = broadcastRecord.options;
        List<Object> receiver2 = receiver;
        List<Object> list = broadcastRecord.resultCode;
        String str4 = broadcastRecord.resultData;
        Bundle bundle = broadcastRecord.resultExtras;
        boolean z3 = broadcastRecord.ordered;
        boolean z4 = broadcastRecord.sticky;
        boolean z5 = broadcastRecord.initialSticky;
        int i4 = broadcastRecord.userId;
        boolean z6 = true;
        String action2 = action;
        pkg = pkg2;
        trimAndEnqueueBroadcast(notify ^ 1, isParallel, new BroadcastRecord(broadcastQueue, intent, processRecord, str2, i, i2, z2, str3, strArr, i3, broadcastOptions, receiver2, resultTo2, list, str4, bundle, z3, z4, z5, i4), pkg);
        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
            String str5 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("enqueueProxyBroadcast enqueue broadcast:");
            stringBuilder2.append(action2);
            stringBuilder2.append(" pkg:");
            stringBuilder2.append(pkg);
            stringBuilder2.append(" pid:");
            stringBuilder2.append(pid2);
            stringBuilder2.append(" uid:");
            stringBuilder2.append(uid2);
            Slog.v(str5, stringBuilder2.toString());
        } else {
            uid = pid2;
        }
        return z6;
    }

    public void setProxyBCActions(List<String> actions) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mProxyActions.clear();
                if (actions != null) {
                    if ("foreground".equals(this.mQueueName)) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("set default proxy broadcast actions:");
                        stringBuilder.append(actions);
                        Slog.i(str, stringBuilder.toString());
                    }
                    this.mProxyActions.addAll(actions);
                }
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void setActionExcludePkg(String action, String pkg) {
        if (action == null && pkg == null) {
            Slog.w(TAG, "clear mActionExcludePkgs");
            this.mActionExcludePkgs.clear();
        } else if (action == null || pkg == null) {
            Slog.w(TAG, "setActionExcludePkg invaild param");
        } else {
            Set<String> pkgs;
            if (this.mActionExcludePkgs.containsKey(action)) {
                pkgs = (Set) this.mActionExcludePkgs.get(action);
                pkgs.add(pkg);
            } else {
                pkgs = new HashSet();
                pkgs.add(pkg);
            }
            this.mActionExcludePkgs.put(action, pkgs);
        }
    }

    public void proxyBCConfig(int type, String key, List<String> value) {
        if (mProxyFeature) {
            if ("foreground".equals(this.mQueueName)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(String.format("proxy %s bc config [%d][%s][", new Object[]{this.mQueueName, Integer.valueOf(type), key}));
                stringBuilder.append(value);
                stringBuilder.append("]");
                Slog.i(str, stringBuilder.toString());
            }
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    switch (type) {
                        case 0:
                            clearConfigLocked();
                            break;
                        case 1:
                            configProxyBCActionsLocked(key, value);
                            break;
                        case 2:
                            configDropBCActionsLocked(key, value);
                            break;
                        case 3:
                            configDropBCByPidLocked(key, value);
                            break;
                        case 4:
                            configMaxProxyBCLocked(key);
                            break;
                        case 5:
                            configSameActionLocked(key, value);
                            break;
                        case 6:
                            configAddAppProxyBCActions(key, value);
                            break;
                        case 7:
                            configRemoveAppProxyBCActions(key, value);
                            break;
                    }
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
            return;
        }
        Slog.w(TAG, "proxy bc not support");
    }

    private void setAppProxyBCActions(String pkg, List<String> actions) {
        if (pkg != null) {
            if ("foreground".equals(this.mQueueName)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("set ");
                stringBuilder.append(pkg);
                stringBuilder.append(" proxy broadcast actions:");
                stringBuilder.append(actions);
                Slog.i(str, stringBuilder.toString());
            }
            if (actions != null) {
                this.mAppProxyActions.put(pkg, new ArrayList(actions));
                return;
            }
            this.mAppProxyActions.put(pkg, null);
        }
    }

    private void configAddAppProxyBCActions(String pkg, List<String> actions) {
        if (pkg == null) {
            Slog.e(TAG, "config add app bc actions error");
            return;
        }
        if (actions == null || actions.size() == 0) {
            this.mAppAddProxyActions.remove(pkg);
        } else {
            this.mAppAddProxyActions.put(pkg, new ArrayList(actions));
        }
    }

    private void configRemoveAppProxyBCActions(String pkg, List<String> actions) {
        if (pkg == null) {
            Slog.e(TAG, "config remove app bc actions error");
            return;
        }
        if (actions == null || actions.size() == 0) {
            this.mAppRemoveProxyActions.remove(pkg);
        } else {
            this.mAppRemoveProxyActions.put(pkg, new ArrayList(actions));
        }
    }

    private void configProxyBCActionsLocked(String pkg, List<String> actions) {
        if (pkg == null && actions == null) {
            Slog.i(TAG, "invaild parameter for config proxy bc actions");
            return;
        }
        if (pkg == null) {
            setProxyBCActions(actions);
        } else {
            setAppProxyBCActions(pkg, actions);
        }
    }

    private void configDropBCActionsLocked(String pkg, List<String> actions) {
        if (pkg == null) {
            Slog.e(TAG, "config drop bc actions error");
            return;
        }
        if ("foreground".equals(this.mQueueName)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(pkg);
            stringBuilder.append(" drop actions:");
            stringBuilder.append(actions);
            Slog.i(str, stringBuilder.toString());
        }
        if (actions == null) {
            this.mAppDropActions.put(pkg, null);
        } else {
            this.mAppDropActions.put(pkg, new ArrayList(actions));
        }
    }

    private void configDropBCByPidLocked(String pid, List<String> actions) {
        if (pid == null) {
            Slog.e(TAG, "config drop bc actions by pid error");
            return;
        }
        try {
            Integer iPid = Integer.valueOf(Integer.parseInt(pid));
            if ("foreground".equals(this.mQueueName)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(iPid);
                stringBuilder.append(" drop actions:");
                stringBuilder.append(actions);
                Slog.i(str, stringBuilder.toString());
            }
            if (actions == null) {
                this.mProcessDropActions.put(iPid, null);
            } else {
                this.mProcessDropActions.put(iPid, new ArrayList(actions));
            }
        } catch (Exception e) {
            Slog.w(TAG, e.getMessage());
        }
    }

    private void configMaxProxyBCLocked(String count) {
        if (count == null) {
            Slog.e(TAG, "config max proxy broadcast error");
            return;
        }
        try {
            this.MAX_PROXY_BROADCAST = Integer.parseInt(count);
            if ("foreground".equals(this.mQueueName)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("set max proxy broadcast :");
                stringBuilder.append(this.MAX_PROXY_BROADCAST);
                Slog.i(str, stringBuilder.toString());
            }
        } catch (Exception e) {
            Slog.w(TAG, e.getMessage());
        }
    }

    private void configSameActionLocked(String action1, List<String> actions) {
        if (action1 == null || actions == null) {
            Slog.e(TAG, "invaild parameter for config same kind action");
            return;
        }
        for (String action2 : actions) {
            this.mSameKindsActionList.put(action1, action2);
        }
    }

    private void clearConfigLocked() {
        if ("foreground".equals(this.mQueueName)) {
            Slog.i(TAG, "clear all config");
        }
        this.mProxyActions.clear();
        this.mAppProxyActions.clear();
        this.mAppDropActions.clear();
        this.mProcessDropActions.clear();
        this.mActionExcludePkgs.clear();
        this.mSameKindsActionList.clear();
        this.mAppAddProxyActions.clear();
        this.mAppRemoveProxyActions.clear();
    }

    private boolean dropActionLocked(String pkg, int pid, BroadcastRecord br) {
        String action = br.intent.getAction();
        if (pid == -1 || isAlivePid(pid)) {
            ArrayList<String> actions;
            String str;
            StringBuilder stringBuilder;
            if (this.mProcessDropActions.containsKey(Integer.valueOf(pid))) {
                actions = (ArrayList) this.mProcessDropActions.get(Integer.valueOf(pid));
                if (actions == null) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("process ");
                    stringBuilder.append(pid);
                    stringBuilder.append(" cache, drop all proxy broadcast, now drop :");
                    stringBuilder.append(br);
                    Slog.i(str, stringBuilder.toString());
                    return true;
                } else if (action != null && actions.contains(action)) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("process ");
                    stringBuilder.append(pid);
                    stringBuilder.append(" cache, drop list broadcast, now drop :");
                    stringBuilder.append(br);
                    Slog.i(str, stringBuilder.toString());
                    return true;
                }
            }
            if (this.mAppDropActions.containsKey(pkg)) {
                actions = (ArrayList) this.mAppDropActions.get(pkg);
                if (actions == null) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("pkg ");
                    stringBuilder.append(pkg);
                    stringBuilder.append(" cache, drop all proxy broadcast, now drop ");
                    stringBuilder.append(br);
                    Slog.i(str, stringBuilder.toString());
                    return true;
                } else if (action != null && actions.contains(action)) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("pkg ");
                    stringBuilder.append(pkg);
                    stringBuilder.append(" cache, drop list broadcast, now drop ");
                    stringBuilder.append(br);
                    Slog.i(str, stringBuilder.toString());
                    return true;
                }
            }
            return false;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("process ");
        stringBuilder2.append(pid);
        stringBuilder2.append(" has died, drop ");
        stringBuilder2.append(br);
        Slog.i(str2, stringBuilder2.toString());
        return true;
    }

    private boolean isAlivePid(int pid) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("/proc/");
        stringBuilder.append(pid);
        return new File(stringBuilder.toString()).exists();
    }

    public long proxyBroadcast(List<String> pkgs, boolean proxy) {
        if (mProxyFeature) {
            long delay;
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(proxy ? "proxy " : "unproxy ");
                stringBuilder.append(this.mQueueName);
                stringBuilder.append(" broadcast  pkgs:");
                stringBuilder.append(pkgs);
                Slog.d(str, stringBuilder.toString());
            }
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    delay = 0;
                    boolean pending = this.mPendingBroadcastTimeoutMessage;
                    List<String> pkgList = new ArrayList();
                    int i = 0;
                    if (proxy) {
                        String pkg;
                        pkgList = pkgs;
                        for (String pkg2 : pkgList) {
                            if (!this.mProxyBroadcastPkgs.contains(pkg2)) {
                                this.mProxyBroadcastPkgs.add(pkg2);
                            }
                        }
                        if (pending && this.mOrderedBroadcasts.size() > 0) {
                            BroadcastRecord i2 = (BroadcastRecord) this.mOrderedBroadcasts.get(0);
                            if (i2.nextReceiver >= 1) {
                                pkg2 = getPkg(i2.receivers.get(i2.nextReceiver - 1));
                                if (pkg2 != null && pkgList.contains(pkg2)) {
                                    delay = this.mTimeoutPeriod;
                                }
                            }
                        }
                    } else {
                        String pkg3;
                        StringBuilder stringBuilder2;
                        String str2;
                        StringBuilder stringBuilder3;
                        if (pkgs != null) {
                            pkgList = pkgs;
                        } else {
                            pkgList = (ArrayList) this.mProxyBroadcastPkgs.clone();
                        }
                        ArrayList<BroadcastRecord> orderedProxyBroadcasts = new ArrayList();
                        ArrayList<BroadcastRecord> parallelProxyBroadcasts = new ArrayList();
                        proxyBroadcastInnerLocked(this.mParallelPendingBroadcasts, pkgList, parallelProxyBroadcasts);
                        proxyBroadcastInnerLocked(this.mOrderedPendingBroadcasts, pkgList, orderedProxyBroadcasts);
                        this.mProcessDropActions.clear();
                        this.mProxyBroadcastPkgs.removeAll(pkgList);
                        for (String pkg32 : pkgList) {
                            if (this.mProxyPkgsCount.containsKey(pkg32)) {
                                this.mProxyPkgsCount.remove(pkg32);
                            }
                        }
                        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                            pkg32 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("unproxy ");
                            stringBuilder2.append(this.mQueueName);
                            stringBuilder2.append(" Broadcast pkg Parallel Broadcasts (");
                            stringBuilder2.append(this.mParallelBroadcasts);
                            stringBuilder2.append(")");
                            Slog.v(pkg32, stringBuilder2.toString());
                        }
                        for (int i3 = 0; i3 < parallelProxyBroadcasts.size(); i3++) {
                            this.mParallelBroadcasts.add(i3, (BroadcastRecord) parallelProxyBroadcasts.get(i3));
                        }
                        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                            pkg32 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("unproxy ");
                            stringBuilder2.append(this.mQueueName);
                            stringBuilder2.append(" Broadcast pkg Parallel Broadcasts (");
                            stringBuilder2.append(this.mParallelBroadcasts);
                            stringBuilder2.append(")");
                            Slog.v(pkg32, stringBuilder2.toString());
                        }
                        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                            pkg32 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("unproxy ");
                            stringBuilder2.append(this.mQueueName);
                            stringBuilder2.append(" Broadcast pkg Ordered Broadcasts (");
                            stringBuilder2.append(this.mOrderedBroadcasts);
                            stringBuilder2.append(")");
                            Slog.v(pkg32, stringBuilder2.toString());
                        }
                        if (pending) {
                            movePendingBroadcastToProxyList(this.mOrderedBroadcasts, orderedProxyBroadcasts, pkgList);
                        }
                        while (i < orderedProxyBroadcasts.size()) {
                            if (pending) {
                                this.mOrderedBroadcasts.add(i + 1, (BroadcastRecord) orderedProxyBroadcasts.get(i));
                            } else {
                                this.mOrderedBroadcasts.add(i, (BroadcastRecord) orderedProxyBroadcasts.get(i));
                            }
                            i++;
                        }
                        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                            str2 = TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("unproxy ");
                            stringBuilder3.append(this.mQueueName);
                            stringBuilder3.append(" Broadcast pkg Ordered Broadcasts (");
                            stringBuilder3.append(this.mOrderedBroadcasts);
                            stringBuilder3.append(")");
                            Slog.v(str2, stringBuilder3.toString());
                        }
                        if (parallelProxyBroadcasts.size() > 0 || orderedProxyBroadcasts.size() > 0) {
                            if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                                str2 = TAG;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("unproxy ");
                                stringBuilder3.append(this.mQueueName);
                                stringBuilder3.append(" Broadcast pkg Parallel Broadcasts (");
                                stringBuilder3.append(parallelProxyBroadcasts.size());
                                stringBuilder3.append(")(");
                                stringBuilder3.append(parallelProxyBroadcasts);
                                stringBuilder3.append(")");
                                Slog.v(str2, stringBuilder3.toString());
                            }
                            if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                                str2 = TAG;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("unproxy ");
                                stringBuilder3.append(this.mQueueName);
                                stringBuilder3.append(" Broadcast pkg Ordered Broadcasts (");
                                stringBuilder3.append(orderedProxyBroadcasts.size());
                                stringBuilder3.append(")(");
                                stringBuilder3.append(orderedProxyBroadcasts);
                                stringBuilder3.append(")");
                                Slog.v(str2, stringBuilder3.toString());
                            }
                            scheduleBroadcastsLocked();
                        }
                    }
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
            return delay;
        }
        Slog.w(TAG, "proxy bc not support");
        return -1;
    }

    private void proxyBroadcastInnerLocked(ArrayList<BroadcastRecord> pendingBroadcasts, List<String> unProxyPkgs, ArrayList<BroadcastRecord> unProxyBroadcasts) {
        Iterator it = pendingBroadcasts.iterator();
        while (it.hasNext()) {
            BroadcastRecord br = (BroadcastRecord) it.next();
            Object nextReceiver = br.receivers.get(0);
            String proxyPkg = getPkg(nextReceiver);
            if (proxyPkg != null && unProxyPkgs.contains(proxyPkg)) {
                int pid = getPid(nextReceiver);
                it.remove();
                if (!dropActionLocked(proxyPkg, pid, br)) {
                    unProxyBroadcasts.add(br);
                }
            }
        }
    }

    private String getPkg(Object target) {
        if (target instanceof BroadcastFilter) {
            return ((BroadcastFilter) target).packageName;
        }
        if (!(target instanceof ResolveInfo)) {
            return null;
        }
        ResolveInfo info = (ResolveInfo) target;
        if (info.activityInfo == null || info.activityInfo.applicationInfo == null) {
            return null;
        }
        return info.activityInfo.applicationInfo.packageName;
    }

    private int getPid(Object target) {
        if (target instanceof BroadcastFilter) {
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
        boolean z = target instanceof ResolveInfo;
        return -1;
    }

    private int getUid(Object target) {
        if (target instanceof BroadcastFilter) {
            BroadcastFilter filter = (BroadcastFilter) target;
            if (filter.receiverList == null) {
                return -1;
            }
            int uid = filter.receiverList.uid;
            if (uid > 0 || filter.receiverList.app == null) {
                return uid;
            }
            return filter.receiverList.app.uid;
        } else if (!(target instanceof ResolveInfo)) {
            return -1;
        } else {
            ResolveInfo info = (ResolveInfo) target;
            if (info.activityInfo == null || info.activityInfo.applicationInfo == null) {
                return -1;
            }
            return info.activityInfo.applicationInfo.uid;
        }
    }

    private void movePendingBroadcastToProxyList(ArrayList<BroadcastRecord> orderedBroadcasts, ArrayList<BroadcastRecord> orderedProxyBroadcasts, List<String> pkgList) {
        HwBroadcastQueue hwBroadcastQueue = this;
        List list = pkgList;
        ArrayList<BroadcastRecord> arrayList;
        HwBroadcastQueue hwBroadcastQueue2;
        if (orderedProxyBroadcasts.size() == 0 || orderedBroadcasts.size() == 0) {
            arrayList = orderedProxyBroadcasts;
            hwBroadcastQueue2 = hwBroadcastQueue;
            return;
        }
        BroadcastRecord r = (BroadcastRecord) orderedBroadcasts.get(0);
        List<Object> needMoveReceivers = new ArrayList();
        List<Object> receivers = r.receivers;
        List<Object> list2;
        List<Object> list3;
        if (receivers == null) {
            arrayList = orderedProxyBroadcasts;
            hwBroadcastQueue2 = hwBroadcastQueue;
            list2 = needMoveReceivers;
            list3 = receivers;
        } else if (list == null) {
            arrayList = orderedProxyBroadcasts;
            hwBroadcastQueue2 = hwBroadcastQueue;
            list2 = needMoveReceivers;
            list3 = receivers;
        } else {
            List<Object> receivers2;
            int recIdx;
            int numReceivers;
            int recIdx2 = r.nextReceiver;
            int numReceivers2 = receivers.size();
            int i = recIdx2;
            while (i < numReceivers2) {
                List<Object> needMoveReceivers2;
                int i2;
                Object target = receivers.get(i);
                String pkg = hwBroadcastQueue.getPkg(target);
                if (pkg == null || !list.contains(pkg)) {
                    arrayList = orderedProxyBroadcasts;
                    needMoveReceivers2 = needMoveReceivers;
                    receivers2 = receivers;
                    recIdx = recIdx2;
                    numReceivers = numReceivers2;
                    i2 = i;
                } else {
                    needMoveReceivers.add(target);
                    List<Object> receiver = new ArrayList();
                    receiver.add(target);
                    recIdx = recIdx2;
                    numReceivers = numReceivers2;
                    receivers2 = receivers;
                    needMoveReceivers2 = needMoveReceivers;
                    i2 = i;
                    arrayList = orderedProxyBroadcasts;
                    arrayList.add(new BroadcastRecord(r.queue, r.intent, r.callerApp, r.callerPackage, r.callingPid, r.callingUid, r.callerInstantApp, r.resolvedType, r.requiredPermissions, r.appOp, r.options, receiver, null, r.resultCode, r.resultData, r.resultExtras, r.ordered, r.sticky, r.initialSticky, r.userId));
                }
                i = i2 + 1;
                recIdx2 = recIdx;
                numReceivers2 = numReceivers;
                receivers = receivers2;
                needMoveReceivers = needMoveReceivers2;
                hwBroadcastQueue = this;
                List<String> list4 = pkgList;
                ArrayList<BroadcastRecord> arrayList2 = orderedBroadcasts;
            }
            arrayList = orderedProxyBroadcasts;
            receivers2 = receivers;
            recIdx = recIdx2;
            numReceivers = numReceivers2;
            list2 = needMoveReceivers;
            if (list2.size() > 0) {
                receivers2.removeAll(list2);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unproxy ");
                stringBuilder.append(this.mQueueName);
                stringBuilder.append(", moving receivers in Ordered Broadcasts (");
                stringBuilder.append(r);
                stringBuilder.append(") to proxyList, Move receivers : ");
                stringBuilder.append(list2);
                Slog.v(str, stringBuilder.toString());
            }
        }
    }

    public AbsHwMtmBroadcastResourceManager getMtmBRManager() {
        return this.mHwMtmBroadcastResourceManager;
    }

    public boolean getMtmBRManagerEnabled(int featureType) {
        return this.mService.getIawareResourceFeature(featureType) && getMtmBRManager() != null;
    }

    public boolean uploadRadarMessage(int scene, Bundle data) {
        if (scene == HwBroadcastRadarUtil.SCENE_DEF_BROADCAST_OVERLENGTH) {
            int i = 0;
            while (i < this.mOrderedBroadcasts.size()) {
                try {
                    this.mCopyOrderedBroadcasts.add((BroadcastRecord) this.mOrderedBroadcasts.get(i));
                    i++;
                } catch (Exception e) {
                    Slog.w(TAG, e.getMessage());
                } catch (Throwable th) {
                    this.mCopyOrderedBroadcasts.clear();
                }
            }
            handleBroadcastQueueOverlength(this.mCopyOrderedBroadcasts);
            this.mCopyOrderedBroadcasts.clear();
            return true;
        } else if (scene != 2803) {
            return false;
        } else {
            handleReceiverTimeOutRadar();
            return true;
        }
    }

    public void enqueueOrderedBroadcastLocked(BroadcastRecord r) {
        super.enqueueOrderedBroadcastLocked(r);
        if (SystemClock.uptimeMillis() > 1800000) {
            StringBuilder stringBuilder;
            int brSize = this.mOrderedBroadcasts.size();
            if (!this.enableUploadRadar && brSize < 150) {
                this.enableUploadRadar = true;
                stringBuilder = new StringBuilder();
                stringBuilder.append("enable radar when current queue size is ");
                stringBuilder.append(brSize);
                stringBuilder.append(".");
                Flog.i(104, stringBuilder.toString());
            }
            if (this.enableUploadRadar && brSize >= 150) {
                uploadRadarMessage(HwBroadcastRadarUtil.SCENE_DEF_BROADCAST_OVERLENGTH, null);
                this.enableUploadRadar = false;
                stringBuilder = new StringBuilder();
                stringBuilder.append("disable radar after radar uploaded, current size is ");
                stringBuilder.append(brSize);
                stringBuilder.append(".");
                Flog.i(104, stringBuilder.toString());
            }
        }
    }

    private void initActionWhiteList() {
        this.mActionWhiteList.add("android.net.wifi.SCAN_RESULTS");
        this.mActionWhiteList.add("android.net.wifi.WIFI_STATE_CHANGED");
        this.mActionWhiteList.add("android.net.conn.CONNECTIVITY_CHANGE");
        this.mActionWhiteList.add("android.intent.action.TIME_TICK");
    }

    private void handleBroadcastQueueOverlength(ArrayList<BroadcastRecord> copyOrderedBroadcasts) {
        String callerPkg = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        String curReceiverName = null;
        String curReceiverPkgName = null;
        boolean isContainsMMS = false;
        String broadcastAction = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        for (int i = 0; i < copyOrderedBroadcasts.size(); i++) {
            BroadcastRecord br = (BroadcastRecord) copyOrderedBroadcasts.get(i);
            if (!(br == null || br.intent == null)) {
                if (br.nextReceiver > 0) {
                    BroadcastFilter curReceiver = br.receivers.get(br.nextReceiver - 1);
                    if (curReceiver instanceof BroadcastFilter) {
                        curReceiverPkgName = curReceiver.packageName;
                    } else if (curReceiver instanceof ResolveInfo) {
                        ResolveInfo info = (ResolveInfo) curReceiver;
                        if (info.activityInfo != null) {
                            curReceiverName = info.activityInfo.applicationInfo.name;
                            curReceiverPkgName = info.activityInfo.applicationInfo.packageName;
                        }
                    }
                }
                String callerPkg2 = br.callerPackage;
                broadcastAction = br.intent.getAction();
                if (MMS_PACKAGE_NAME.equals(callerPkg2) || "android.provider.Telephony.SMS_DELIVER".equals(broadcastAction) || "android.provider.Telephony.SMS_RECEIVED".equals(broadcastAction)) {
                    isContainsMMS = true;
                }
                BroadcastRadarRecord broadcastRadarRecord = (BroadcastRadarRecord) this.mRadarBroadcastMap.get(broadcastAction);
                if (broadcastRadarRecord == null) {
                    broadcastRadarRecord = new BroadcastRadarRecord(broadcastAction, callerPkg2, 0);
                }
                broadcastRadarRecord.count++;
                this.mRadarBroadcastMap.put(broadcastAction, broadcastRadarRecord);
            }
        }
        callerPkg = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        int maxNum = 0;
        for (Entry<String, BroadcastRadarRecord> curActionEntry : this.mRadarBroadcastMap.entrySet()) {
            int curBroadcastNum = ((BroadcastRadarRecord) curActionEntry.getValue()).count;
            if (curBroadcastNum > maxNum) {
                maxNum = curBroadcastNum;
                callerPkg = (String) curActionEntry.getKey();
            }
        }
        BroadcastRadarRecord brRecord = (BroadcastRadarRecord) this.mRadarBroadcastMap.get(callerPkg);
        if (this.mActionWhiteList.contains(brRecord.actionName)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("The action[");
            stringBuilder.append(brRecord.actionName);
            stringBuilder.append("] should be ignored for order broadcast queue overlength.");
            Flog.i(104, stringBuilder.toString());
            return;
        }
        String versionName = Shell.NIGHT_MODE_STR_UNKNOWN;
        if (curReceiverPkgName != null) {
            try {
                if (!curReceiverPkgName.isEmpty()) {
                    PackageInfo packageInfo = AppGlobals.getPackageManager().getPackageInfo(curReceiverPkgName, 16384, 0);
                    if (packageInfo != null) {
                        versionName = packageInfo.versionName;
                    }
                }
            } catch (Exception e) {
                Slog.e(TAG, e.getMessage());
            }
        }
        Bundle data = new Bundle();
        data.putString("package", brRecord.packageName);
        data.putString(HwBroadcastRadarUtil.KEY_ACTION, brRecord.actionName);
        data.putInt(HwBroadcastRadarUtil.KEY_ACTION_COUNT, brRecord.count);
        data.putString(HwBroadcastRadarUtil.KEY_RECEIVER, curReceiverName);
        data.putString(HwBroadcastRadarUtil.KEY_VERSION_NAME, versionName);
        data.putBoolean(HwBroadcastRadarUtil.KEY_MMS_BROADCAST_FLAG, isContainsMMS);
        this.mBroadcastRadarUtil.handleBroadcastQueueOverlength(data);
        if (this.mBroadcastMonitor != null) {
            this.mBroadcastMonitor.monitor(907400002, data);
        }
        this.mRadarBroadcastMap.clear();
    }

    private void handleReceiverTimeOutRadar() {
        if (this.mOrderedBroadcasts.size() == 0) {
            Slog.w(TAG, "handleReceiverTimeOutRadar, but mOrderedBroadcasts is empty");
            return;
        }
        BroadcastRecord r = (BroadcastRecord) this.mOrderedBroadcasts.get(0);
        if (r.receivers == null || r.nextReceiver <= 0) {
            Slog.w(TAG, "handleReceiverTimeOutRadar Timeout on receiver, but receiver is invalid.");
            return;
        }
        String pkg = null;
        String receiverName = null;
        String actionName = null;
        int uid = 0;
        long receiverTime = SystemClock.uptimeMillis() - r.receiverTime;
        if (r.intent != null) {
            actionName = r.intent.getAction();
            if (receiverTime < ((r.intent.getFlags() & 268435456) != 0 ? 2000 : 5000)) {
                Slog.w(TAG, "current receiver should not report timeout.");
                return;
            }
        }
        BroadcastFilter curReceiver = r.receivers.get(r.nextReceiver - 1);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("receiver ");
        stringBuilder.append(curReceiver);
        stringBuilder.append(" took ");
        stringBuilder.append(receiverTime);
        stringBuilder.append("ms when receive ");
        stringBuilder.append(r);
        Flog.i(104, stringBuilder.toString());
        if (curReceiver instanceof BroadcastFilter) {
            pkg = curReceiver.packageName;
        } else if (curReceiver instanceof ResolveInfo) {
            ResolveInfo info = (ResolveInfo) curReceiver;
            if (info.activityInfo != null) {
                receiverName = info.activityInfo.name;
                if (info.activityInfo.applicationInfo != null) {
                    uid = info.activityInfo.applicationInfo.uid;
                    pkg = info.activityInfo.applicationInfo.packageName;
                }
            }
        }
        if (SystemClock.uptimeMillis() > 1800000) {
            String versionName = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            if (pkg != null) {
                try {
                    if (!pkg.isEmpty()) {
                        PackageInfo packageInfo = AppGlobals.getPackageManager().getPackageInfo(pkg, 16384, UserHandle.getUserId(uid));
                        if (packageInfo != null) {
                            versionName = packageInfo.versionName;
                        }
                    }
                } catch (Exception e) {
                    Slog.e(TAG, e.getMessage());
                }
            }
            Bundle data = new Bundle();
            data.putString("package", pkg);
            data.putString(HwBroadcastRadarUtil.KEY_RECEIVER, receiverName);
            data.putString(HwBroadcastRadarUtil.KEY_ACTION, actionName);
            data.putString(HwBroadcastRadarUtil.KEY_VERSION_NAME, versionName);
            data.putFloat(HwBroadcastRadarUtil.KEY_RECEIVE_TIME, ((float) receiverTime) / 1000.0f);
            data.putParcelable(HwBroadcastRadarUtil.KEY_BROADCAST_INTENT, r.intent);
            if (this.mBroadcastMonitor != null) {
                this.mBroadcastMonitor.monitor(907400003, data);
            }
            this.mBroadcastRadarUtil.handleReceiverTimeOut(data);
        }
    }

    final boolean dumpLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, String dumpPackage, boolean needSep) {
        boolean ret = super.dumpLocked(fd, pw, args, opti, dumpAll, dumpPackage, needSep);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        pw.println();
        if (mProxyFeature) {
            String app;
            Object actions;
            StringBuilder stringBuilder;
            ArrayList<String> actions2;
            Iterator it;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  Proxy broadcast [");
            stringBuilder2.append(this.mQueueName);
            stringBuilder2.append("] pkg:");
            stringBuilder2.append(this.mProxyBroadcastPkgs);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("    Default proxy actions :");
            stringBuilder2.append(this.mProxyActions);
            pw.println(stringBuilder2.toString());
            pw.println("    APP proxy actions :");
            for (Entry entry : this.mAppProxyActions.entrySet()) {
                app = (String) entry.getKey();
                actions = entry.getValue();
                if (actions == null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("        ");
                    stringBuilder.append(app);
                    stringBuilder.append(" null");
                    pw.println(stringBuilder.toString());
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("        ");
                    stringBuilder.append(app);
                    stringBuilder.append(" ");
                    stringBuilder.append((ArrayList) actions);
                    pw.println(stringBuilder.toString());
                }
            }
            pw.println("    Same kind actions :");
            for (Entry entry2 : this.mSameKindsActionList.entrySet()) {
                app = (String) entry2.getKey();
                String action2 = (String) entry2.getValue();
                stringBuilder = new StringBuilder();
                stringBuilder.append("        ");
                stringBuilder.append(app);
                stringBuilder.append(" <-> ");
                stringBuilder.append(action2);
                pw.println(stringBuilder.toString());
            }
            pw.println("    APP drop actions :");
            for (Entry entry22 : this.mAppDropActions.entrySet()) {
                app = (String) entry22.getKey();
                actions = entry22.getValue();
                if (actions == null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("        ");
                    stringBuilder.append(app);
                    stringBuilder.append(" null");
                    pw.println(stringBuilder.toString());
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("        ");
                    stringBuilder.append(app);
                    stringBuilder.append(" ");
                    stringBuilder.append((ArrayList) actions);
                    pw.println(stringBuilder.toString());
                }
            }
            pw.println("    APP add proxy actions :");
            for (Entry entry222 : this.mAppAddProxyActions.entrySet()) {
                app = (String) entry222.getKey();
                actions2 = (ArrayList) entry222.getValue();
                stringBuilder = new StringBuilder();
                stringBuilder.append("        ");
                stringBuilder.append(app);
                stringBuilder.append(" ");
                stringBuilder.append(actions2);
                pw.println(stringBuilder.toString());
            }
            pw.println("    APP remove proxy actions :");
            for (Entry entry2222 : this.mAppRemoveProxyActions.entrySet()) {
                app = (String) entry2222.getKey();
                actions2 = (ArrayList) entry2222.getValue();
                stringBuilder = new StringBuilder();
                stringBuilder.append("        ");
                stringBuilder.append(app);
                stringBuilder.append(" ");
                stringBuilder.append(actions2);
                pw.println(stringBuilder.toString());
            }
            pw.println("    Process drop actions :");
            for (Entry entry22222 : this.mProcessDropActions.entrySet()) {
                Integer process = (Integer) entry22222.getKey();
                actions = entry22222.getValue();
                if (actions == null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("        ");
                    stringBuilder.append(process);
                    stringBuilder.append(" null");
                    pw.println(stringBuilder.toString());
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("        ");
                    stringBuilder.append(process);
                    stringBuilder.append(" ");
                    stringBuilder.append((ArrayList) actions);
                    pw.println(stringBuilder.toString());
                }
            }
            pw.println("    Proxy pkgs broadcast count:");
            for (Entry entry222222 : this.mProxyPkgsCount.entrySet()) {
                app = (String) entry222222.getKey();
                Integer count = (Integer) entry222222.getValue();
                stringBuilder = new StringBuilder();
                stringBuilder.append("        ");
                stringBuilder.append(app);
                stringBuilder.append(" ");
                stringBuilder.append(count);
                pw.println(stringBuilder.toString());
            }
            pw.println("    Action exclude pkg:");
            for (Entry entry2222222 : this.mActionExcludePkgs.entrySet()) {
                app = (String) entry2222222.getKey();
                Set<String> pkgs = (Set) entry2222222.getValue();
                stringBuilder = new StringBuilder();
                stringBuilder.append("        ");
                stringBuilder.append(app);
                stringBuilder.append(" ");
                stringBuilder.append(pkgs);
                pw.println(stringBuilder.toString());
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("    MAX_PROXY_BROADCAST:");
            stringBuilder3.append(this.MAX_PROXY_BROADCAST);
            pw.println(stringBuilder3.toString());
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("  Proxy Parallel Broadcast:");
            stringBuilder3.append(this.mParallelPendingBroadcasts.size());
            pw.println(stringBuilder3.toString());
            if (this.mParallelPendingBroadcasts.size() <= 20) {
                it = this.mParallelPendingBroadcasts.iterator();
                while (it.hasNext()) {
                    ((BroadcastRecord) it.next()).dump(pw, "    ", sdf);
                }
            }
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("  Proxy Ordered Broadcast:");
            stringBuilder3.append(this.mOrderedPendingBroadcasts.size());
            pw.println(stringBuilder3.toString());
            if (this.mOrderedPendingBroadcasts.size() <= 20) {
                it = this.mOrderedPendingBroadcasts.iterator();
                while (it.hasNext()) {
                    ((BroadcastRecord) it.next()).dump(pw, "    ", sdf);
                }
            }
        }
        return ret;
    }

    public ArrayList<Integer> getIawareDumpData() {
        ArrayList<Integer> queueSizes = new ArrayList();
        queueSizes.add(Integer.valueOf(this.mParallelBroadcasts.size()));
        queueSizes.add(Integer.valueOf(this.mOrderedBroadcasts.size()));
        return queueSizes;
    }
}
