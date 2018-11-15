package com.android.server.rms;

import android.app.ActivityManagerNative;
import android.app.IProcessObserver;
import android.app.IProcessObserver.Stub;
import android.app.mtm.MultiTaskManager;
import android.app.mtm.MultiTaskPolicy;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.IContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.rms.HwSysResource;
import android.rms.IHwSysResManager;
import android.rms.IProcessStateChangeObserver;
import android.rms.IUpdateWhiteListCallback;
import android.rms.config.ResourceConfig;
import android.rms.iaware.AppTypeRecoManager;
import android.rms.iaware.AwareConstant.FeatureType;
import android.rms.iaware.AwareConstant.ResourceType;
import android.rms.iaware.CollectData;
import android.rms.iaware.DumpData;
import android.rms.iaware.IDeviceSettingCallback;
import android.rms.iaware.IReportDataCallback;
import android.rms.iaware.ISceneCallback;
import android.rms.iaware.NetLocationStrategy;
import android.rms.iaware.RPolicyData;
import android.rms.iaware.StatisticsData;
import android.rms.iaware.memrepair.MemRepairPkgInfo;
import android.rms.resource.PidsResource;
import android.rms.utils.Utils;
import android.util.Log;
import android.util.Slog;
import com.android.internal.os.SomeArgs;
import com.android.server.SystemService;
import com.android.server.rms.algorithm.AwareUserHabit;
import com.android.server.rms.collector.ProcMemInfoReader;
import com.android.server.rms.config.HwConfigReader;
import com.android.server.rms.dump.DumpCase;
import com.android.server.rms.handler.HwSysResHandler;
import com.android.server.rms.handler.ResourceDispatcher;
import com.android.server.rms.iaware.RDAService;
import com.android.server.rms.iaware.RDMEDispatcher;
import com.android.server.rms.iaware.appmng.AwareAppAssociate;
import com.android.server.rms.iaware.appmng.AwareIntelligentRecg;
import com.android.server.rms.iaware.appmng.AwareWakeUpManager;
import com.android.server.rms.iaware.appmng.FreezeDataManager;
import com.android.server.rms.iaware.feature.DevSchedFeatureRT;
import com.android.server.rms.iaware.memory.utils.MemoryReader;
import com.android.server.rms.iaware.sysload.SysLoadManager;
import com.android.server.rms.io.IOStatsService;
import com.android.server.rms.ipcchecker.HwIpcMonitorImpl;
import com.android.server.rms.memrepair.MemRepairPolicy;
import com.android.server.rms.record.AppUsageFileRecord;
import com.android.server.rms.record.ResourceRecordStore;
import com.android.server.rms.record.ResourceUtils;
import com.android.server.rms.resource.HwSysInnerResImpl;
import com.android.server.security.tsmagent.logic.spi.tsm.laser.LaserTSMServiceImpl;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class HwSysResManagerService extends SystemService {
    private static final String APP_UID = "uid";
    private static long BIGDATAINFOS_UPLOAD_PERIOD = (Utils.DEBUG ? (long) Utils.getCompactPeriodInterval() : 86400000);
    public static final int MSG_ADD_BIGDATA_INFOS = 12;
    private static final int MSG_CLEAN_MAP = 3;
    private static final int MSG_DISPATCH_PROCESS_LAUNCHER = 8;
    private static final int MSG_INIT = 1;
    private static final int MSG_IO_SERVICE = 6;
    private static final int MSG_NOTIFY_STATUS = 4;
    public static final int MSG_RECHECK_BLOCK_IPC_PROCESS = 13;
    private static final int MSG_REPORT_APPTYPE_DATA = 10;
    private static final int MSG_REPORT_STATUS = 2;
    private static final int MSG_SAMPLE = 5;
    public static final int MSG_UPLOAD_BIGDATA_INFOS = 11;
    private static final int MSG_WHITENAME_FILE_UPDATE = 7;
    private static int RECHECK_BLOCK_IPC_PROCESS_TIME = 10000;
    private static final String TAG = "RMS.HwSysResManagerService";
    private static boolean enableIaware = SystemProperties.getBoolean("persist.sys.enable_iaware", false);
    private static boolean enableRms = SystemProperties.getBoolean("ro.config.enable_rms", false);
    private static HwSysResManagerService mSelf;
    private AppUsageFileRecord mAppUsageFile = null;
    private final AtomicBoolean mCloudFileUpdate = new AtomicBoolean(false);
    private HwConfigReader mConfig;
    private final Context mContext;
    private final Handler mHandler;
    private Callback mHandlerCallback = new Callback() {
        public boolean handleMessage(Message msg) {
            if (Utils.DEBUG) {
                String str = HwSysResManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleMessage /");
                stringBuilder.append(msg.what);
                Log.d(str, stringBuilder.toString());
            }
            switch (msg.what) {
                case 1:
                    HwSysResManagerService.this.handleInitService();
                    return true;
                case 2:
                    HwSysResManagerService.this.mResourceRecordStore.recordResourceOverloadStatus(msg);
                    return true;
                case 3:
                    HwSysResManagerService.this.mResourceRecordStore.cleanResourceRecordMap(msg);
                    return true;
                case 4:
                    HwSysResManagerService.this.mResourceRecordStore.notifyResourceStatus(msg);
                    return true;
                case 6:
                    if (HwSysResManagerService.enableRms && (Utils.RMSVERSION & 1) != 0) {
                        HwSysResManagerService.this.mIOStatsService.startIOStatsService();
                    }
                    return true;
                case 7:
                    HwSysResManagerService.this.mCloudFileUpdate.getAndSet(HwSysResManagerService.this.mConfig.updateResConfig(HwSysResManagerService.this.mContext));
                    return true;
                case 8:
                    HwSysResManagerService.this.handleDispatchMessage(msg);
                    return true;
                case 10:
                    Bundle args = msg.getData();
                    String pkgName = args.getString(AwareIntelligentRecg.CMP_PKGNAME);
                    if ("APPTYPE_INIT_ACTION".equals(pkgName)) {
                        AppTypeRecoManager.getInstance().init(HwSysResManagerService.this.mContext);
                        return true;
                    }
                    int apptype = args.getInt("appType");
                    boolean status = args.getBoolean("appsSatus");
                    int attr = args.getInt("appAttr");
                    if (status) {
                        AppTypeRecoManager.getInstance().addAppType(pkgName, apptype, attr);
                    } else {
                        AppTypeRecoManager.getInstance().removeAppType(pkgName);
                    }
                    return true;
                case 11:
                    HwSysResManagerService.this.mResourceRecordStore.uploadBigDataInfos();
                    HwSysResManagerService.this.repeatlyCheckUploadBigDataInfos();
                    return true;
                case 12:
                    HwSysResManagerService.this.mResourceRecordStore.createAndCheckUploadBigDataInfos(msg);
                    return true;
                case 13:
                    if (HwSysResManagerService.this.mRecheckIpcMonitor != null) {
                        HwSysResManagerService.this.mRecheckIpcMonitor.recoverBlockingProcess(false);
                    }
                    return true;
                default:
                    return false;
            }
        }
    };
    private IOStatsService mIOStatsService = null;
    private final ProcMemInfoReader mProcMemInfoReader = new ProcMemInfoReader();
    private IProcessObserver mProcessObserver = new Stub() {
        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            if (!(foregroundActivities || HwSysResManagerService.this.mResourceRecordStore == null)) {
                HwSysResManagerService.this.mResourceRecordStore.handleOverloadResource(uid, pid, 29, 0);
                HwSysResManagerService.this.mResourceRecordStore.handleOverloadResource(uid, pid, 30, 0);
                HwSysResManagerService.this.mResourceRecordStore.handleOverloadResource(uid, pid, 13, 0);
            }
            if (Utils.DEBUG) {
                String str = HwSysResManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onForegroundActivitiesChanged pid = ");
                stringBuilder.append(pid);
                stringBuilder.append(", uid = ");
                stringBuilder.append(uid);
                stringBuilder.append(", foregroundActivities=");
                stringBuilder.append(foregroundActivities);
                Log.d(str, stringBuilder.toString());
            }
        }

        public void onProcessDied(int pid, int uid) {
            if (Utils.DEBUG) {
                String str = HwSysResManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onProcessDied pid = ");
                stringBuilder.append(pid);
                stringBuilder.append(", uid = ");
                stringBuilder.append(uid);
                Log.i(str, stringBuilder.toString());
            }
            HwSysResManagerService.this.mResourceRecordStore.cleanResRecordAppDied(uid, pid);
        }
    };
    private RemoteCallbackList<IProcessStateChangeObserver> mProcessStateChangeObserver = new RemoteCallbackList();
    private RDAService mRDAService = null;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.ACTION_SHUTDOWN".equals(action)) {
                if (HwSysResManagerService.this.mAppUsageFile != null) {
                    HwSysResManagerService.this.mAppUsageFile.saveUsageInfo();
                }
            } else if ("android.intent.action.PACKAGE_REMOVED".equals(action) || "android.intent.action.PACKAGE_REPLACED".equals(action)) {
                String pkg = intent.getData().getSchemeSpecificPart();
                if (Utils.DEBUG) {
                    String str = HwSysResManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(" receiver for  ");
                    stringBuilder.append(intent.getAction());
                    stringBuilder.append("  pkg ");
                    stringBuilder.append(pkg);
                    Log.i(str, stringBuilder.toString());
                }
                if (HwSysResManagerService.this.mAppUsageFile != null) {
                    HwSysResManagerService.this.mAppUsageFile.updateUsageInfo(pkg);
                }
                int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                if (Utils.DEBUG || Log.HWLog) {
                    String str2 = HwSysResManagerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" remove uid ");
                    stringBuilder2.append(uid);
                    Log.i(str2, stringBuilder2.toString());
                }
                Message msg = HwSysResManagerService.this.mHandler.obtainMessage();
                msg.arg1 = uid;
                msg.arg2 = 18;
                HwSysResManagerService.this.mResourceRecordStore.cleanResourceRecordMap(msg);
                synchronized (HwSysResManagerService.this.mThirdAppOverloadRecord) {
                    HwSysResManagerService.this.mThirdAppOverloadRecord.remove(pkg);
                }
            }
        }
    };
    private HwIpcMonitorImpl mRecheckIpcMonitor;
    private RemoteCallbackList<IUpdateWhiteListCallback> mRegisteredResourceCallback = new RemoteCallbackList();
    private ResourceRecordStore mResourceRecordStore;
    private final IBinder mService = new IHwSysResManager.Stub() {
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            int resIDGestureScroll;
            Bundle bundleArgs;
            long curtime;
            int resIDGestureScroll2;
            int resIDGestureScroll3;
            if (i == 15007) {
                parcel.enforceInterface("android.rms.IHwSysResManager");
                resIDGestureScroll = ResourceType.getReousrceId(ResourceType.RESOURCE_SCENE_REC);
                bundleArgs = new Bundle();
                curtime = System.currentTimeMillis();
                bundleArgs.putInt("relationType", 13);
                reportData(new CollectData(resIDGestureScroll, curtime, bundleArgs));
                return true;
            } else if (i == 15009) {
                parcel.enforceInterface("android.rms.IHwSysResManager");
                resIDGestureScroll = data.readInt();
                resIDGestureScroll2 = ResourceType.getReousrceId(ResourceType.RESOURCE_SCENE_REC);
                Bundle bundleArgs2 = new Bundle();
                long curtime2 = System.currentTimeMillis();
                bundleArgs2.putInt("relationType", 15);
                bundleArgs2.putInt("scroll_duration", resIDGestureScroll);
                reportData(new CollectData(resIDGestureScroll2, curtime2, bundleArgs2));
                return true;
            } else if (i == 20017) {
                parcel.enforceInterface("android.rms.IHwSysResManager");
                resIDGestureScroll2 = data.readInt();
                resIDGestureScroll3 = ResourceType.getReousrceId(ResourceType.RESOURCE_MEDIA_BTN);
                Bundle bundleArgs3 = new Bundle();
                long curtime3 = System.currentTimeMillis();
                bundleArgs3.putInt("eventid", 20017);
                bundleArgs3.putInt("callUid", resIDGestureScroll2);
                reportData(new CollectData(resIDGestureScroll3, curtime3, bundleArgs3));
                return true;
            } else if (i != 85007) {
                switch (i) {
                    case 15015:
                        parcel.enforceInterface("android.rms.IHwSysResManager");
                        resIDGestureScroll = data.readInt();
                        resIDGestureScroll2 = data.readInt();
                        resIDGestureScroll3 = data.readInt();
                        String num = data.readString();
                        String time = data.readString();
                        String pkgName = data.readString();
                        Bundle bundle = new Bundle();
                        bundle.putInt("eventId", 2);
                        bundle.putInt("dexopt_pid", resIDGestureScroll);
                        bundle.putInt("ppid", resIDGestureScroll2);
                        bundle.putInt("status", resIDGestureScroll3);
                        bundle.putString("num", num);
                        bundle.putString("time", time);
                        bundle.putString(AwareUserHabit.USERHABIT_PACKAGE_NAME, pkgName);
                        reportData(new CollectData(ResourceType.getReousrceId(ResourceType.RESOURCE_INSTALLER_MANAGER), System.currentTimeMillis(), bundle));
                        return true;
                    case 15016:
                        parcel.enforceInterface("android.rms.IHwSysResManager");
                        reportData(new CollectData(ResourceType.getReousrceId(ResourceType.RESOURCE_INSTALLER_MANAGER), System.currentTimeMillis(), data.readBundle()));
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            } else {
                parcel.enforceInterface("android.rms.IHwSysResManager");
                resIDGestureScroll = ResourceType.getReousrceId(ResourceType.RESOURCE_SCENE_REC);
                bundleArgs = new Bundle();
                curtime = System.currentTimeMillis();
                bundleArgs.putInt("relationType", 14);
                reportData(new CollectData(resIDGestureScroll, curtime, bundleArgs));
                return true;
            }
        }

        public ResourceConfig[] getResourceConfig(int resourceType) throws RemoteException {
            if (!HwSysResManagerService.this.isServiceReady()) {
                return null;
            }
            int subTypeNum = HwSysResManagerService.this.mConfig.getSubTypeNum(resourceType);
            ResourceConfig[] config = null;
            if (subTypeNum > 0) {
                config = new ResourceConfig[subTypeNum];
            }
            if (config != null) {
                for (int i = 0; i < subTypeNum; i++) {
                    config[i] = HwSysResManagerService.this.mConfig.getResConfig(resourceType, i);
                }
            }
            return config;
        }

        public boolean registerResourceUpdateCallback(IUpdateWhiteListCallback updateCallback) throws RemoteException {
            if (updateCallback == null || !HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return false;
            }
            synchronized (HwSysResManagerService.this.mRegisteredResourceCallback) {
                HwSysResManagerService.this.mRegisteredResourceCallback.register(updateCallback);
            }
            return true;
        }

        public boolean registerSceneCallback(IBinder callback, int scenes) {
            if (!HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return false;
            }
            SysLoadManager.getInstance().registerCallback(ISceneCallback.Stub.asInterface(callback), scenes);
            return true;
        }

        public boolean isScene(int scene) {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return SysLoadManager.getInstance().isScene(scene);
            }
            return false;
        }

        public List<String> getIAwareProtectList(int num) {
            if (!HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return null;
            }
            AwareUserHabit habit = AwareUserHabit.getInstance();
            if (habit != null && habit.isEnable()) {
                return habit.getForceProtectApps(num);
            }
            if (Utils.DEBUG) {
                Log.d(HwSysResManagerService.TAG, "habit is null or habit is disable");
            }
            return null;
        }

        public boolean isVisibleWindow(int userid, String pkg, int type) {
            if (!HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return false;
            }
            boolean result = false;
            if (type == 1 || type == 3) {
                result = AwareAppAssociate.getInstance().isVisibleWindows(userid, pkg);
            }
            if (type == 2 || (!result && type == 3)) {
                result = AwareIntelligentRecg.getInstance().isToastWindows(userid, pkg);
            }
            return result;
        }

        public String getWhiteList(int resourceType, int type) throws RemoteException {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(resourceType)) {
                return HwSysResManagerService.this.mConfig.getWhiteList(resourceType, type);
            }
            return null;
        }

        public void triggerUpdateWhiteList() throws RemoteException {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                HwSysResManagerService.this.updateWhiteList();
            }
        }

        public List<String> getLongTimeRunningApps() {
            if (!HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return null;
            }
            List<String> list = null;
            AwareUserHabit habit = AwareUserHabit.getInstance();
            if (habit != null && habit.isEnable()) {
                list = habit.recognizeLongTimeRunningApps();
            }
            return list;
        }

        public List<String> getMostFrequentUsedApps(int n, int minCount) {
            if (!HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return null;
            }
            List<String> list = null;
            AwareUserHabit habit = AwareUserHabit.getInstance();
            if (habit != null && habit.isEnable()) {
                list = habit.getMostFrequentUsedApp(n, minCount);
            }
            return list;
        }

        public void reportAppType(String pkgName, int appType, boolean status, int attr) {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                Message msg = HwSysResManagerService.this.mHandler.obtainMessage();
                msg.what = 10;
                Bundle data = msg.getData();
                data.putString(AwareIntelligentRecg.CMP_PKGNAME, pkgName);
                data.putInt("appType", appType);
                data.putBoolean("appsSatus", status);
                data.putInt("appAttr", attr);
                HwSysResManagerService.this.mHandler.sendMessage(msg);
            }
        }

        public void reportHabitData(Bundle habitData) {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10) && habitData != null) {
                AwareUserHabit habit = AwareUserHabit.getInstance();
                if (habit != null && habit.isEnable()) {
                    habit.reportHabitData(habitData);
                }
            }
        }

        public void recordResourceOverloadStatus(int uid, String pkg, int resourceType, int overloadNum, int speedOverLoadPeriod, int totalNum, Bundle extra) {
            if (HwSysResManagerService.this.isServiceReady()) {
                Message msg = HwSysResManagerService.this.mHandler.obtainMessage();
                msg.what = 2;
                msg.arg1 = uid;
                msg.arg2 = resourceType;
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = pkg;
                args.argi1 = overloadNum;
                args.argi2 = speedOverLoadPeriod;
                args.argi3 = totalNum;
                msg.obj = args;
                if (extra != null) {
                    if (Utils.DEBUG) {
                        Log.e(HwSysResManagerService.TAG, "recordResourceOverloadStatus has NonNull Bundle");
                    }
                    if (resourceType == 18) {
                        extra.putInt("third_party_app_usetime", HwSysResManagerService.this.mAppUsageFile.getUsageTimeforUpload(pkg));
                    }
                    msg.setData(extra);
                }
                HwSysResManagerService.this.mHandler.sendMessage(msg);
            }
        }

        public void clearResourceStatus(int uid, int resourceType) throws RemoteException {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                Message msg = HwSysResManagerService.this.mHandler.obtainMessage();
                msg.what = 3;
                msg.arg1 = uid;
                msg.arg2 = resourceType;
                HwSysResManagerService.this.mHandler.sendMessage(msg);
            }
        }

        public void notifyResourceStatus(int resourceType, String resourceName, int resourceStatus, Bundle bd) throws RemoteException {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                Message msg = HwSysResManagerService.this.mHandler.obtainMessage();
                msg.what = 4;
                msg.arg1 = resourceType;
                msg.arg2 = resourceStatus;
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = resourceName;
                args.arg2 = bd;
                msg.obj = args;
                HwSysResManagerService.this.mHandler.sendMessage(msg);
            }
        }

        public void dispatch(int resourceType, MultiTaskPolicy policy) throws RemoteException {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                HwSysResHandler resHandler = ResourceDispatcher.dispath(resourceType, HwSysResManagerService.this.mContext);
                if (resHandler != null) {
                    resHandler.execute(policy);
                }
            }
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (HwSysResManagerService.this.getContext().checkCallingPermission("android.permission.DUMP") != 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Permission Denial: can't dump HwResourceManager service from from pid=");
                stringBuilder.append(Binder.getCallingPid());
                stringBuilder.append(", uid=");
                stringBuilder.append(Binder.getCallingUid());
                pw.println(stringBuilder.toString());
            } else if (Binder.getCallingUid() > 1000 || !DumpCase.dump(HwSysResManagerService.this.mContext, pw, args)) {
                HwSysResManagerService.this.mRDAService.dump(fd, pw, args);
                HwSysResManagerService.this.mResourceRecordStore.dumpImpl(pw);
                CompactJobService.dumpLog(fd, pw, args);
            }
        }

        public int acquireSysRes(int resourceType, Uri uri, IContentObserver observer, Bundle args) {
            HwSysResource resource = HwSysInnerResImpl.getResource(resourceType);
            int uid = args.getInt("uid");
            if (resource == null || ResourceUtils.checkAppUidPermission(uid) != 0) {
                return 2;
            }
            return resource.acquire(uri, observer, args);
        }

        public void reportData(CollectData data) {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                HwSysResManagerService.this.mRDAService.reportData(data);
            }
        }

        public void reportDataWithCallback(CollectData data, IReportDataCallback callback) throws RemoteException {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                HwSysResManagerService.this.mRDAService.reportDataWithCallback(data, callback);
            }
        }

        public void enableFeature(int type) {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                HwSysResManagerService.this.mRDAService.enableFeature(type);
            }
        }

        public void disableFeature(int type) {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                HwSysResManagerService.this.mRDAService.disableFeature(type);
            }
        }

        public boolean configUpdate() {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return HwSysResManagerService.this.mRDAService.configUpdate();
            }
            return false;
        }

        public boolean custConfigUpdate() {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return HwSysResManagerService.this.mRDAService.custConfigUpdate();
            }
            return false;
        }

        /* JADX WARNING: Missing block: B:13:0x0038, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void requestAppClean(List<String> pkgNameList, int[] userIdArray, int level, String reason, int source) {
            if (!(pkgNameList == null || userIdArray == null || !HwSysResManagerService.this.checkServiceReadyAndPermission(10) || MultiTaskManager.getInstance() == null)) {
                List<Integer> userIdList = new ArrayList();
                for (int valueOf : userIdArray) {
                    userIdList.add(Integer.valueOf(valueOf));
                }
                MultiTaskManager.getInstance().requestAppClean(pkgNameList, userIdList, level, reason, source);
            }
        }

        public void init(Bundle args) {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                HwSysResManagerService.this.mRDAService.init(args);
            }
        }

        public void dispatchRPolicy(RPolicyData policy) {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                RDMEDispatcher.dispatchRPolicy(policy);
            }
        }

        public boolean isResourceNeeded(int resourceid) {
            if (HwSysResManagerService.this.isServiceReady()) {
                return HwSysResManagerService.this.mRDAService.isResourceNeeded(resourceid);
            }
            return false;
        }

        public int getDumpData(int time, List<DumpData> dumpData) {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return HwSysResManagerService.this.mRDAService.getDumpData(time, dumpData);
            }
            return 0;
        }

        public int getStatisticsData(List<StatisticsData> statisticsData) {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return HwSysResManagerService.this.mRDAService.getStatisticsData(statisticsData);
            }
            return 0;
        }

        public String saveBigData(int featureId, boolean clear) {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return HwSysResManagerService.this.mRDAService.saveBigData(featureId, clear);
            }
            return null;
        }

        public String fetchBigDataByVersion(int iVer, int fId, boolean beta, boolean clear) {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return HwSysResManagerService.this.mRDAService.fetchBigDataByVersion(iVer, fId, beta, clear);
            }
            return null;
        }

        public String fetchDFTDataByVersion(int iVer, int fId, boolean beta, boolean clear, boolean betaEncode) {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return HwSysResManagerService.this.mRDAService.fetchDFTDataByVersion(iVer, fId, beta, clear, betaEncode);
            }
            return null;
        }

        public void updateFakeForegroundList(List<String> processList) {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                HwSysResManagerService.this.mRDAService.updateFakeForegroundList(processList);
            }
        }

        public boolean isFakeForegroundProcess(String process) {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return HwSysResManagerService.this.mRDAService.isFakeForegroundProcess(process);
            }
            return false;
        }

        public boolean isEnableFakeForegroundControl() {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return HwSysResManagerService.this.mRDAService.isEnableFakeForegroundControl();
            }
            return false;
        }

        public int getPid(String procName) {
            if (!HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return 0;
            }
            return HwSysResManagerService.this.mProcMemInfoReader.getPidForProcName(new String[]{procName});
        }

        public long getPss(int pid) {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return 1024 * HwSysResManagerService.this.mProcMemInfoReader.getProcessPssByPID(pid);
            }
            return 0;
        }

        public long getMemAvaliable() {
            if (!HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return 0;
            }
            long memAvaliable = 0;
            MemoryReader memoryReader = MemoryReader.getInstance();
            if (memoryReader != null) {
                memAvaliable = memoryReader.getMemAvailable();
            }
            return memAvaliable;
        }

        public boolean registerProcessStateChangeObserver(IProcessStateChangeObserver observer) throws RemoteException {
            if (!HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return false;
            }
            synchronized (HwSysResManagerService.this.mProcessStateChangeObserver) {
                HwSysResManagerService.this.mProcessStateChangeObserver.register(observer);
            }
            return true;
        }

        public boolean unRegisterProcessStateChangeObserver(IProcessStateChangeObserver observer) throws RemoteException {
            if (!HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return false;
            }
            synchronized (HwSysResManagerService.this.mProcessStateChangeObserver) {
                HwSysResManagerService.this.mProcessStateChangeObserver.unregister(observer);
            }
            return true;
        }

        public void noteProcessStart(String packageName, String processName, int pid, int uid, boolean started, String launcherMode, String reason) {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10) && ResourceUtils.getProcessTypeId(uid, packageName, -1) == 0 && "activity".equals(launcherMode)) {
                Message msg = HwSysResManagerService.this.mHandler.obtainMessage();
                msg.what = 8;
                msg.arg1 = uid;
                msg.arg2 = pid;
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = packageName;
                args.arg2 = processName;
                args.arg3 = Boolean.valueOf(started);
                args.arg4 = launcherMode;
                args.arg5 = reason;
                msg.obj = args;
                HwSysResManagerService.this.mHandler.sendMessage(msg);
            }
        }

        public List<MemRepairPkgInfo> getMemRepairProcGroup(int sceneType) {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return MemRepairPolicy.getInstance().getMemRepairPolicy(sceneType);
            }
            return null;
        }

        public List<String> getFrequentIM(int count) {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return FreezeDataManager.getInstance().getFrequentIM(count);
            }
            return null;
        }

        public void reportSysWakeUp(String reason) {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                AwareWakeUpManager.getInstance().reportWakeupSystem(reason);
            }
        }

        public NetLocationStrategy getNetLocationStrategy(String pkgName, int uid, int type) {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10) || checkClientPermission(1001)) {
                return DevSchedFeatureRT.getNetLocationStrategy(pkgName, uid, type);
            }
            return null;
        }

        private boolean checkClientPermission(int uid) {
            boolean z = false;
            if (HwSysResManagerService.this.isServiceReady()) {
                if (UserHandle.getAppId(Binder.getCallingUid()) % LaserTSMServiceImpl.EXCUTE_OTA_RESULT_SUCCESS == uid) {
                    z = true;
                }
                return z;
            }
            Log.e(HwSysResManagerService.TAG, "service not ready!");
            return false;
        }

        public List<String> getHabitTopN(int n) {
            if (!HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return null;
            }
            List<String> list = null;
            AwareUserHabit habit = AwareUserHabit.getInstance();
            if (habit != null && habit.isEnable()) {
                list = habit.getTopN(n);
            }
            return list;
        }

        public Bundle getTypeTopN(int[] appTypes) {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                return AwareIntelligentRecg.getInstance().getTypeTopN(appTypes);
            }
            return null;
        }

        public void registerDevModeMethod(int deviceId, IDeviceSettingCallback callback, Bundle args) {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                DevSchedFeatureRT.registerDevModeMethod(deviceId, callback, args);
            }
        }

        public void unregisterDevModeMethod(int deviceId, IDeviceSettingCallback callback, Bundle args) {
            if (HwSysResManagerService.this.checkServiceReadyAndPermission(10)) {
                DevSchedFeatureRT.unregisterDevModeMethod(deviceId, callback, args);
            }
        }
    };
    private boolean mServiceReady = false;
    private HashMap<String, Long> mThirdAppOverloadRecord = new HashMap();
    private int mThirdAppUploadInterval;

    public HwSysResManagerService(Context context) {
        super(context);
        this.mContext = context;
        setInstance(this);
        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        this.mHandler = new Handler(handlerThread.getLooper(), this.mHandlerCallback);
        this.mResourceRecordStore = ResourceRecordStore.getInstance(context);
        this.mResourceRecordStore.setMessageHandler(this.mHandler);
        repeatlyCheckUploadBigDataInfos();
        if (enableRms && (1 & Utils.RMSVERSION) != 0) {
            if (Utils.HWFLOW) {
                Log.i(TAG, "create the IOStatsService instance");
            }
            this.mIOStatsService = IOStatsService.getInstance(context, handlerThread.getLooper());
        }
        this.mRDAService = new RDAService(this.mContext, handlerThread);
        AwareWakeUpManager.getInstance().init(this.mHandler, this.mContext);
    }

    private static void setInstance(HwSysResManagerService service) {
        mSelf = service;
    }

    private boolean checkServiceReadyAndPermission(int resourceType) {
        if (!isServiceReady()) {
            Log.e(TAG, "checkServiceReadyAndPermission service not ready!");
            return false;
        } else if (resourceType == 12 || resourceType == 16) {
            return true;
        } else {
            int pid = Binder.getCallingPid();
            int uid = UserHandle.getAppId(Binder.getCallingUid());
            if (ResourceUtils.checkSysProcPermission(pid, uid) == 0) {
                return true;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Process Permission error! pid:");
            stringBuilder.append(pid);
            stringBuilder.append(" uid:");
            stringBuilder.append(uid);
            stringBuilder.append(" Process.myPid:");
            stringBuilder.append(Process.myPid());
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    public void onStart() {
        if (Utils.DEBUG || Utils.HWFLOW) {
            Log.i(TAG, "publishBinderService()");
        }
        publishBinderService("hwsysresmanager", this.mService);
    }

    public void onBootPhase(int phase) {
        if (phase == 550 && this.mHandler != null) {
            this.mHandler.sendEmptyMessage(1);
            this.mHandler.sendEmptyMessage(6);
        }
    }

    public void cloudFileUpate() {
        if (Utils.DEBUG || Utils.HWFLOW || Log.HWLog) {
            Log.d(TAG, "cloudFileUpate()");
        }
        Message msg = this.mHandler.obtainMessage();
        msg.what = 7;
        this.mHandler.sendMessage(msg);
    }

    public IHwSysResManager getHwSysResManagerService() {
        if (this.mService != null) {
            return IHwSysResManager.Stub.asInterface(this.mService);
        }
        return null;
    }

    public void recheckBlockIpcProcess(HwIpcMonitorImpl monitor) {
        if (this.mHandler != null) {
            this.mRecheckIpcMonitor = monitor;
            if (!this.mHandler.hasMessages(13)) {
                Message msg = Message.obtain();
                msg.what = 13;
                this.mHandler.sendMessageDelayed(msg, (long) RECHECK_BLOCK_IPC_PROCESS_TIME);
            }
        }
    }

    private void repeatlyCheckUploadBigDataInfos() {
        if (!(this.mHandler == null || this.mHandler.hasMessages(11))) {
            Message msg = Message.obtain();
            msg.what = 11;
            this.mHandler.sendMessageDelayed(msg, BIGDATAINFOS_UPLOAD_PERIOD);
        }
    }

    private void dispatchProcessDied(String packageName, String processName, int pid, int uid, boolean started, String launcherMode, String reason) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("dispatchProcessDied pkg:");
        stringBuilder.append(packageName);
        Log.i(str, stringBuilder.toString());
        synchronized (this.mThirdAppOverloadRecord) {
            String str2;
            StringBuilder stringBuilder2;
            Long record = (Long) this.mThirdAppOverloadRecord.get(packageName);
            long currentTime = SystemClock.uptimeMillis();
            if (Utils.DEBUG && record != null) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("pkg ");
                stringBuilder2.append(packageName);
                stringBuilder2.append(" inteval ");
                stringBuilder2.append(currentTime - record.longValue());
                Log.d(str2, stringBuilder2.toString());
            }
            if ((record != null && currentTime - record.longValue() >= ((long) this.mThirdAppUploadInterval)) || record == null) {
                dispatchProcessLauncher(packageName, processName, pid, uid, started, launcherMode, reason);
                this.mThirdAppOverloadRecord.put(packageName, Long.valueOf(currentTime));
                if (Utils.DEBUG) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("got it. pkg ");
                    stringBuilder2.append(packageName);
                    stringBuilder2.append(" report time ");
                    stringBuilder2.append(currentTime);
                    Log.d(str2, stringBuilder2.toString());
                }
            }
        }
    }

    private void handleDispatchMessage(Message msg) {
        Message message = msg;
        if (message != null) {
            int uid = message.arg1;
            int pid = message.arg2;
            SomeArgs args = message.obj;
            String packageName = args.arg1;
            String processName = args.arg2;
            boolean started = ((Boolean) args.arg3).booleanValue();
            String launcherMode = args.arg4;
            String reason = args.arg5;
            args.recycle();
            if (this.mResourceRecordStore.isOverloadResourceRecord(uid, pid, 18)) {
                if (Utils.HWFLOW) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("noteProcessStart -uid =");
                    stringBuilder.append(uid);
                    stringBuilder.append(" should be forbidden!");
                    Log.i(str, stringBuilder.toString());
                }
                uid = 18;
                dispatchProcessDied(packageName, processName, pid, uid, started, launcherMode, reason);
                message.arg2 = uid;
                this.mResourceRecordStore.cleanResourceRecordMap(message);
            }
        }
    }

    public void handleInitService() {
        this.mConfig = new HwConfigReader();
        if (this.mConfig.loadResConfig(this.mContext)) {
            this.mServiceReady = true;
        } else {
            Log.e(TAG, "handleInitService read xml file error");
        }
        try {
            ActivityManagerNative.getDefault().registerProcessObserver(this.mProcessObserver);
        } catch (RemoteException e) {
            Slog.e(TAG, "RMS register process observer failed");
        }
        initPidCtrollGroup();
        try {
            System.loadLibrary("sysrms_jni");
        } catch (UnsatisfiedLinkError e2) {
            this.mServiceReady = false;
            Slog.e(TAG, "libsysrms_jni library not found!");
        }
        if (enableRms) {
            CompactJobService.schedule(this.mContext);
        }
        setSRMSFeature();
        setAppStartupFeature();
        monitorUsageInfo();
        this.mThirdAppUploadInterval = this.mConfig.getResourceThreshold(18, 3);
    }

    private void monitorUsageInfo() {
        boolean appUsageFileEnabled = (Utils.RMSVERSION & 2) != 0;
        if (appUsageFileEnabled) {
            IntentFilter pkgFilter = new IntentFilter();
            pkgFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
            this.mContext.registerReceiver(this.mReceiver, pkgFilter, null, this.mHandler);
            pkgFilter.addAction("android.intent.action.PACKAGE_REMOVED");
            pkgFilter.addAction("android.intent.action.PACKAGE_REPLACED");
            pkgFilter.addDataScheme("package");
            this.mContext.registerReceiver(this.mReceiver, pkgFilter, null, this.mHandler);
        }
        this.mAppUsageFile = new AppUsageFileRecord(this.mContext, this.mConfig.getResourceMaxPeroid(18, 3), appUsageFileEnabled);
        if (appUsageFileEnabled) {
            this.mAppUsageFile.loadUsageInfo();
        }
    }

    private void setSRMSFeature() {
        if (this.mRDAService != null) {
            int srmsFeature = this.mRDAService.isFeatureEnabled(FeatureType.getFeatureId(FeatureType.FEATURE_RESOURCE));
            if (srmsFeature == 0) {
                SystemProperties.set("persist.sys.srms.enable", "false");
            } else if (1 == srmsFeature) {
                SystemProperties.set("persist.sys.srms.enable", "true");
            } else {
                Log.e(TAG, "get SRMS feature failed");
            }
        }
    }

    private void setAppStartupFeature() {
        if (this.mRDAService != null) {
            int startupFeature = this.mRDAService.isFeatureEnabled(FeatureType.getFeatureId(FeatureType.FEATURE_APPSTARTUP));
            if (startupFeature == 0) {
                SystemProperties.set("persist.sys.appstart.enable", "false");
            } else if (1 == startupFeature) {
                SystemProperties.set("persist.sys.appstart.enable", "true");
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setAppStartupFeature: ");
            stringBuilder.append(startupFeature);
            Log.i(str, stringBuilder.toString());
        }
    }

    public void updateWhiteList() {
        String str;
        StringBuilder stringBuilder;
        if (Utils.DEBUG || Log.HWLog) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateWhiteList(), CloudFileUpdate=");
            stringBuilder.append(this.mCloudFileUpdate.get());
            Log.d(str, stringBuilder.toString());
        }
        if (enableIaware && this.mCloudFileUpdate.get()) {
            int i = 0;
            this.mCloudFileUpdate.getAndSet(false);
            synchronized (this.mRegisteredResourceCallback) {
                int len = this.mRegisteredResourceCallback.beginBroadcast();
                while (i < len) {
                    try {
                        ((IUpdateWhiteListCallback) this.mRegisteredResourceCallback.getBroadcastItem(i)).update();
                    } catch (RemoteException e) {
                        Log.e(TAG, "Trigger the rms client registered callback error");
                    }
                    i++;
                }
                if (Utils.DEBUG) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("updateWhiteList Compact trigger cloud config update all registered resource  [num:");
                    stringBuilder2.append(len);
                    stringBuilder2.append("]");
                    Log.d(str2, stringBuilder2.toString());
                }
                this.mRegisteredResourceCallback.finishBroadcast();
            }
            return;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Compact trigger cloud config update, get config file failed   isEnable:");
        stringBuilder.append(enableIaware);
        Log.e(str, stringBuilder.toString());
    }

    public static HwSysResManagerService self() {
        return mSelf;
    }

    private boolean isServiceReady() {
        return this.mServiceReady;
    }

    private void dispatchProcessLauncher(String packageName, String processName, int pid, int uid, boolean started, String launcherMode, String reason) {
        String str;
        StringBuilder stringBuilder;
        String str2 = packageName;
        if (Utils.DEBUG || Log.HWINFO) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("begin diapatch Process observer! pkg ");
            stringBuilder.append(str2);
            Log.i(str, stringBuilder.toString());
        }
        synchronized (this.mProcessStateChangeObserver) {
            int len = this.mProcessStateChangeObserver.beginBroadcast();
            int i = 0;
            while (true) {
                int i2 = i;
                if (i2 < len) {
                    IProcessStateChangeObserver observer = (IProcessStateChangeObserver) this.mProcessStateChangeObserver.getBroadcastItem(i2);
                    if (observer != null) {
                        try {
                            if (Utils.DEBUG || Log.HWINFO) {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("calling dispatchProcessLauncher, pkg ");
                                stringBuilder.append(str2);
                                Log.d(str, stringBuilder.toString());
                            }
                            observer.onProcessLauncher(str2, processName, pid, uid, started, launcherMode, reason);
                        } catch (RemoteException e) {
                            Log.e(TAG, "Trigger the rms client registered observer error");
                        }
                    }
                    i = i2 + 1;
                } else {
                    this.mProcessStateChangeObserver.finishBroadcast();
                }
            }
        }
    }

    public void dispatchProcessDiedOverload(String packageName, int uid) {
        if (Utils.DEBUG || Log.HWINFO) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("begin diapatch Process Died Overload! pkg ");
            stringBuilder.append(packageName);
            Log.i(str, stringBuilder.toString());
        }
        Bundle data = new Bundle();
        data.putString("pkg", packageName);
        synchronized (this.mProcessStateChangeObserver) {
            int len = this.mProcessStateChangeObserver.beginBroadcast();
            for (int i = 0; i < len; i++) {
                IProcessStateChangeObserver observer = (IProcessStateChangeObserver) this.mProcessStateChangeObserver.getBroadcastItem(i);
                if (observer != null) {
                    try {
                        if (Utils.DEBUG || Log.HWINFO) {
                            String str2 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("calling observer.onProcessDiedOverload, pkg ");
                            stringBuilder2.append(packageName);
                            Log.d(str2, stringBuilder2.toString());
                        }
                        observer.onProcessDiedOverload(data);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Trigger the rms client registered observer error");
                    }
                }
            }
            this.mProcessStateChangeObserver.finishBroadcast();
        }
        dispatchProcessDied(packageName, "", 0, 0, false, "", "");
    }

    private void initPidCtrollGroup() {
        if (Utils.DEBUG) {
            Log.d(TAG, "initPidCtrollGroup");
        }
        PidsResource pids = (PidsResource) HwFrameworkFactory.getHwResource(15);
        String[] thresholds = getPidCgroupConfig();
        if (pids != null && thresholds != null) {
            pids.init(thresholds);
            ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
            try {
                pids.acquire(Process.myPid(), "system_server", 2);
            } finally {
                StrictMode.setThreadPolicy(savedPolicy);
            }
        }
    }

    private String[] getPidCgroupConfig() {
        int subTypeNum = this.mConfig.getSubTypeNum(15);
        String[] thresholds = null;
        if (subTypeNum > 0) {
            thresholds = new String[subTypeNum];
        }
        if (thresholds != null) {
            for (int i = 0; i < subTypeNum; i++) {
                StringBuffer buffer = new StringBuffer(String.valueOf(this.mConfig.getResourceThreshold(15, i)));
                buffer.append(",");
                buffer.append(String.valueOf(this.mConfig.getResourceStrategy(15, i)));
                if (Utils.DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("handleInitService Pids [%d");
                    stringBuilder.append(i);
                    stringBuilder.append("]=");
                    stringBuilder.append(buffer.toString());
                    Log.d(str, stringBuilder.toString());
                }
                thresholds[i] = buffer.toString();
            }
        }
        return thresholds;
    }
}
