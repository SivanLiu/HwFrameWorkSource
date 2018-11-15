package com.android.server.rms.iaware.memory.data.handle;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.AwareLog;
import android.text.TextUtils;
import android.util.ArrayMap;
import com.android.server.am.HwActivityManagerService;
import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.rms.iaware.appmng.AwareAppAssociate;
import com.android.server.rms.iaware.appmng.AwareAppMngDFX;
import com.android.server.rms.iaware.feature.AppQuickStartFeature;
import com.android.server.rms.iaware.feature.MemoryFeature2;
import com.android.server.rms.iaware.memory.action.GpuCompressAction;
import com.android.server.rms.iaware.memory.data.content.AttrSegments;
import com.android.server.rms.iaware.memory.utils.BigDataStore;
import com.android.server.rms.iaware.memory.utils.BigMemoryInfo;
import com.android.server.rms.iaware.memory.utils.EventTracker;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.rms.iaware.memory.utils.MemoryUtils;
import com.android.server.rms.iaware.memory.utils.PackageTracker;
import com.android.server.rms.iaware.memory.utils.PrereadUtils;
import com.android.server.rms.iaware.sysload.SysLoadManager;
import com.android.server.security.tsmagent.logic.spi.tsm.laser.LaserTSMServiceImpl;
import java.util.ArrayList;
import java.util.List;

public class DataAppHandle extends AbsDataHandle {
    private static final long ACTIVITY_START_TIMEOUT = 5000;
    private static final String SYSTEM_UI = "com.android.systemui";
    private static final String TAG = "AwareMem_AppHandle";
    private static DataAppHandle sDataHandle;
    private long mAcvityLaunchBeginTimestamp = 0;
    private String mFgPkgName = null;
    private HwActivityManagerService mHwAMS = HwActivityManagerService.self();
    private boolean mIsActivityLaunching = false;
    private boolean mIsInBigMemoryMode = false;
    private String mLastPrereadPkg = null;
    private TrimMemoryHandler mTrimHandler = new TrimMemoryHandler();

    private final class TrimMemoryHandler extends Handler {
        private static final int MSG_COMPRESS_GPUMEMORY = 200;
        private static final int MSG_TRIM_MEMORY = 100;
        private static final int ONTRIM_DELAY_TIME = 3000;
        private Integer mLastPid;
        private String mLastPkgName;
        private List<OnTrimTask> waitForOnTrimPids;

        private class OnTrimTask {
            public int mPid;
            public long mTime;

            public OnTrimTask(int pid, long t) {
                this.mPid = pid;
                this.mTime = t;
            }
        }

        private TrimMemoryHandler() {
            this.mLastPid = Integer.valueOf(0);
            this.mLastPkgName = null;
            this.waitForOnTrimPids = new ArrayList();
        }

        private void interuptTrimMemoryForPid(int curPid) {
            synchronized (this.waitForOnTrimPids) {
                for (int i = this.waitForOnTrimPids.size() - 1; i >= 0; i--) {
                    OnTrimTask tsk = (OnTrimTask) this.waitForOnTrimPids.get(i);
                    if (tsk.mPid == curPid) {
                        String str = DataAppHandle.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("interuptTrimMemoryForPid! curPid:");
                        stringBuilder.append(curPid);
                        stringBuilder.append(" tPid:");
                        stringBuilder.append(tsk.mPid);
                        AwareLog.i(str, stringBuilder.toString());
                        this.waitForOnTrimPids.remove(i);
                    }
                }
            }
        }

        private void doTrimMemory(int curPid, String pkgName) {
            if (!DataAppHandle.this.isLauncher(pkgName)) {
                interuptTrimMemoryForPid(curPid);
            }
            if (this.mLastPkgName != null && this.mLastPid.intValue() != 0 && !"com.android.systemui".equals(this.mLastPkgName) && !DataAppHandle.this.isLauncher(this.mLastPkgName) && this.mLastPid.intValue() != curPid) {
                synchronized (this.waitForOnTrimPids) {
                    this.waitForOnTrimPids.add(new OnTrimTask(this.mLastPid.intValue(), SystemClock.elapsedRealtime()));
                }
                Message message = Message.obtain();
                message.arg1 = this.mLastPid.intValue();
                message.what = 100;
                sendMessageDelayed(message, HwArbitrationDEFS.NotificationMonitorPeriodMillis);
            }
        }

        private void saveLastActivityInfo(int pid, String pkgName) {
            this.mLastPkgName = pkgName;
            this.mLastPid = Integer.valueOf(pid);
        }

        /* JADX WARNING: Missing block: B:11:0x0036, code:
            if (r0 == 0) goto L_?;
     */
        /* JADX WARNING: Missing block: B:12:0x0038, code:
            com.android.server.rms.iaware.memory.utils.MemoryUtils.trimMemory(com.android.server.rms.iaware.memory.data.handle.DataAppHandle.access$400(r8.this$0), java.lang.String.valueOf(r0), 40);
     */
        /* JADX WARNING: Missing block: B:19:?, code:
            return;
     */
        /* JADX WARNING: Missing block: B:20:?, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 100) {
                int pid = 0;
                synchronized (this.waitForOnTrimPids) {
                    if (this.waitForOnTrimPids.size() > 0) {
                        OnTrimTask ott = (OnTrimTask) this.waitForOnTrimPids.get(0);
                        if (SystemClock.elapsedRealtime() - ott.mTime >= HwArbitrationDEFS.NotificationMonitorPeriodMillis) {
                            pid = ott.mPid;
                            this.waitForOnTrimPids.remove(0);
                        }
                    }
                }
            }
        }
    }

    public static DataAppHandle getInstance() {
        DataAppHandle dataAppHandle;
        synchronized (DataAppHandle.class) {
            if (sDataHandle == null) {
                sDataHandle = new DataAppHandle();
            }
            dataAppHandle = sDataHandle;
        }
        return dataAppHandle;
    }

    public int reportData(long timestamp, int event, AttrSegments attrSegments) {
        int result = -1;
        ArrayMap<String, String> appInfo = attrSegments.getSegment("calledApp");
        if (appInfo == null) {
            AwareLog.w(TAG, "appInfo is NULL");
            return -1;
        }
        if (event == 15001) {
            result = handleProcessBegin(timestamp, event, appInfo);
        } else if (event == 15005) {
            result = handleActivityBegin(timestamp, event, appInfo);
        } else if (event == 15010) {
            result = handleAppPrepareMem(timestamp, event, appInfo);
        } else if (event == 15013) {
            result = handleDisplayedBegin(timestamp, event, appInfo);
        } else if (event == 85003) {
            result = handleProcessExitFinish(timestamp, event, appInfo);
        } else if (event == 85005) {
            result = handleActivityFinish(timestamp, event, appInfo);
        } else if (event == 85013) {
            result = handleDisplayedFinish(timestamp, event, appInfo);
        }
        return result;
    }

    private int handleAppPrepareMem(long timestamp, int event, ArrayMap<String, String> appInfo) {
        ArrayMap<String, String> arrayMap = appInfo;
        if (!MemoryFeature2.isUpMemoryFeature.get()) {
            return -1;
        }
        Bundle extras = new Bundle();
        try {
            int reqMemKB = (Integer.valueOf(Integer.parseInt((String) arrayMap.get("requestMem"))).intValue() & 65535) * 1024;
            String str;
            StringBuilder stringBuilder;
            if (reqMemKB <= 0) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("handleAppPrepareMem error  reqMemKB ");
                stringBuilder.append(reqMemKB);
                AwareLog.w(str, stringBuilder.toString());
                return -1;
            }
            Integer uid = Integer.valueOf(Integer.valueOf(Integer.parseInt((String) arrayMap.get("uid"))).intValue() % LaserTSMServiceImpl.EXCUTE_OTA_RESULT_SUCCESS);
            if (uid.intValue() == MemoryConstant.getSystemCameraUid() || uid.intValue() == 1000) {
                extras.putLong("reqMem", (long) reqMemKB);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("handleAppPrepareMem reqMemKB ");
                stringBuilder.append(reqMemKB);
                stringBuilder.append("kb");
                AwareLog.i(str, stringBuilder.toString());
                try {
                    this.mDMEServer.execute(MemoryConstant.MEM_SCENE_BIGMEM, extras, event, timestamp);
                    return 0;
                } catch (NumberFormatException e) {
                    AwareLog.e(TAG, "reqMem is not right");
                    return -1;
                }
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("invalid uid:");
            stringBuilder.append(uid);
            AwareLog.i(str, stringBuilder.toString());
            return -1;
        } catch (NumberFormatException e2) {
            AwareLog.e(TAG, "reqMem is not right");
            return -1;
        }
    }

    public boolean isActivityLaunching() {
        long endTimeStamp = SystemClock.uptimeMillis();
        if (!this.mIsActivityLaunching || endTimeStamp - this.mAcvityLaunchBeginTimestamp >= ACTIVITY_START_TIMEOUT) {
            return false;
        }
        return true;
    }

    private DataAppHandle() {
    }

    private void mayNeedExitSpecialScene() {
        if (this.mIsInBigMemoryMode) {
            MemoryUtils.exitSpecialSceneNotify();
            this.mIsInBigMemoryMode = false;
        }
    }

    private void mayNeedEnterSpecialScene(String appName, String activityName) {
        if (!MemoryConstant.CAMERA_PACKAGE_NAME.equals(appName)) {
            if (BigMemoryInfo.getInstance().isBigMemoryApp(activityName)) {
                this.mIsInBigMemoryMode = true;
                MemoryUtils.enterSpecialSceneNotify(MemoryConstant.getCameraPowerUPMemory(), 16746243, 1);
            } else {
                mayNeedExitSpecialScene();
            }
        }
    }

    private int handleActivityBegin(long timestamp, int event, ArrayMap<String, String> appInfo) {
        ArrayMap<String, String> arrayMap = appInfo;
        String appName = (String) arrayMap.get("packageName");
        String activityName = (String) arrayMap.get("activityName");
        int uid = -1;
        try {
            uid = Integer.parseInt((String) arrayMap.get("uid"));
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "uid is not right");
        }
        dispatchPrereadMsg(appName, uid);
        MemoryConstant.resetTotalAPIRequestMemory();
        if (isLauncher(appName)) {
            mayNeedExitSpecialScene();
            return 0;
        }
        if (MemoryFeature2.isUpMemoryFeature.get() && MemoryConstant.getConfigGmcSwitch() != 0 && uid >= 0) {
            GpuCompressAction.removeUidFromGMCMap(uid);
        }
        Bundle extras = createBundleFromAppInfo(uid, appName);
        if (BigMemoryInfo.getInstance().isBigMemoryApp(activityName)) {
            extras.putString("appName", activityName);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("app event ");
        stringBuilder.append(appName);
        stringBuilder.append(" activity begin");
        AwareLog.d(str, stringBuilder.toString());
        setActivityLaunching(true);
        if (BigMemoryInfo.getInstance().isBigMemoryApp(appName) || BigMemoryInfo.getInstance().isBigMemoryApp(activityName)) {
            long j = timestamp;
            int i = event;
            MemoryConstant.enableBigMemCriticalMemory();
            long j2 = j;
            EventTracker.getInstance().trackEvent(1000, i, j2, null);
            this.mDMEServer.execute(MemoryConstant.MEM_SCENE_BIGMEM, extras, event, j2);
        } else {
            MemoryConstant.disableBigMemCriticalMemory();
            this.mDMEServer.stopExecute(timestamp, event);
        }
        mayNeedEnterSpecialScene(appName, activityName);
        return 0;
    }

    private void dispatchPrereadMsg(String appName, int uid) {
        if (appName != null && !appName.equals(this.mLastPrereadPkg)) {
            this.mLastPrereadPkg = appName;
            if (!("com.android.systemui".equals(appName) || this.mHwAMS == null || isLauncher(appName))) {
                if (MemoryConstant.getCameraPrereadFileMap().containsKey(appName)) {
                    PrereadUtils.getInstance();
                    PrereadUtils.sendPrereadMsg(appName);
                } else if (!AppQuickStartFeature.isExactPrereadFeatureEnable() && this.mHwAMS.numOfPidWithActivity(uid) == 0) {
                    PrereadUtils.getInstance();
                    if (PrereadUtils.addPkgFilesIfNecessary(appName)) {
                        PrereadUtils.getInstance();
                        PrereadUtils.sendPrereadMsg(appName);
                    }
                }
            }
        }
    }

    private int handleActivityFinish(long timestamp, int event, ArrayMap<String, String> appInfo) {
        String appName = (String) appInfo.get("packageName");
        String activityName = (String) appInfo.get("activityName");
        int uid = -1;
        try {
            uid = Integer.parseInt((String) appInfo.get("uid"));
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "uid is not right");
        }
        handleActivityFinishDFX(appInfo, appName);
        if (MemoryFeature2.isUpMemoryFeature.get() && MemoryConstant.getConfigGmcSwitch() != 0) {
            int pid = 0;
            try {
                pid = Integer.parseInt((String) appInfo.get("pid"));
            } catch (NumberFormatException e2) {
                AwareLog.e(TAG, "pid is not right");
            }
            if (pid != 0) {
                this.mTrimHandler.doTrimMemory(pid, appName);
                this.mTrimHandler.saveLastActivityInfo(pid, appName);
            }
            if (uid >= 0 && !isLauncher(appName)) {
                GpuCompressAction.removeUidFromGMCMap(uid);
            }
        }
        if (BigMemoryInfo.getInstance().isBigMemoryApp(appName) || BigMemoryInfo.getInstance().isBigMemoryApp(activityName)) {
            MemoryConstant.enableBigMemCriticalMemory();
        } else {
            MemoryConstant.disableBigMemCriticalMemory();
            mayNeedExitSpecialScene();
        }
        if (isLauncher(appName)) {
            SysLoadManager.getInstance().enterLauncher();
            return 0;
        }
        Bundle extras = createBundleFromAppInfo(uid, appName);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("app event ");
        stringBuilder.append(appName);
        stringBuilder.append(" activity finish");
        AwareLog.d(str, stringBuilder.toString());
        setActivityLaunching(false);
        this.mDMEServer.execute(MemoryConstant.MEM_SCENE_DEFAULT, extras, event, timestamp);
        return 0;
    }

    private void handleActivityFinishDFX(ArrayMap<String, String> appInfo, String appName) {
        if (AwareConstant.CURRENT_USER_TYPE == 3 && appName != null) {
            String processName = (String) appInfo.get("processName");
            if (processName != null) {
                String str;
                StringBuilder stringBuilder;
                AwareAppMngDFX.getInstance().trackeAppStartInfo(appName, processName, 11);
                if (!("com.android.systemui".equals(appName) || isLauncher(appName) || appName.equals(this.mFgPkgName))) {
                    BigDataStore instance = BigDataStore.getInstance();
                    instance.warmLaunch++;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("pkg: ");
                    stringBuilder.append(appName);
                    stringBuilder.append(", warmLaunch: ");
                    stringBuilder.append(BigDataStore.getInstance().warmLaunch);
                    AwareLog.d(str, stringBuilder.toString());
                }
                this.mFgPkgName = appName;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("fgPkg: ");
                stringBuilder.append(this.mFgPkgName);
                AwareLog.d(str, stringBuilder.toString());
            }
        }
    }

    private int handleProcessBegin(long timestamp, int event, ArrayMap<String, String> appInfo) {
        AwareLog.d(TAG, "app event process launch begin");
        return traceProcess(true, (String) appInfo.get("launchMode"), appInfo, timestamp);
    }

    private int handleProcessExitFinish(long timestamp, int event, ArrayMap<String, String> appInfo) {
        AwareLog.d(TAG, "app event process exit finish");
        return traceProcess(false, (String) appInfo.get("exitMode"), appInfo, timestamp);
    }

    private int handleDisplayedBegin(long timestamp, int event, ArrayMap<String, String> appInfo) {
        AwareLog.d(TAG, "handleDisplayedBegin");
        if (!MemoryFeature2.isUpMemoryFeature.get() || AwareConstant.CURRENT_USER_TYPE != 3) {
            return -1;
        }
        try {
            String activityName = (String) appInfo.get("activityName");
            int pid = Integer.parseInt((String) appInfo.get("pid"));
            if (activityName == null) {
                return -1;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("activity displayed time : ");
            stringBuilder.append(activityName);
            stringBuilder.append(" , ");
            stringBuilder.append(pid);
            stringBuilder.append(", 0");
            AwareLog.d(str, stringBuilder.toString());
            MemoryUtils.sendActivityDisplayedTime(activityName, pid, 0);
            return 0;
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "handleDisplayedBegin get pid or time failed");
            return -1;
        }
    }

    private int handleDisplayedFinish(long timestamp, int event, ArrayMap<String, String> appInfo) {
        AwareLog.d(TAG, "handleDisplayedFinish");
        if (!MemoryFeature2.isUpMemoryFeature.get() || AwareConstant.CURRENT_USER_TYPE != 3) {
            return -1;
        }
        try {
            String activityName = (String) appInfo.get("activityName");
            int pid = Integer.parseInt((String) appInfo.get("pid"));
            long thisTime = Long.parseLong((String) appInfo.get("displayedTime"));
            if (activityName == null) {
                return -1;
            }
            int thisIntTime;
            if (thisTime < 0) {
                thisIntTime = 0;
            } else if (thisTime > 2147483647L) {
                thisIntTime = Integer.MAX_VALUE;
            } else {
                thisIntTime = (int) thisTime;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("activity displayed time : ");
            stringBuilder.append(activityName);
            stringBuilder.append(" , ");
            stringBuilder.append(pid);
            stringBuilder.append(", ");
            stringBuilder.append(thisIntTime);
            AwareLog.d(str, stringBuilder.toString());
            MemoryUtils.sendActivityDisplayedTime(activityName, pid, thisIntTime);
            return 0;
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "handleDisplayedFinish get pid or time failed");
            return -1;
        }
    }

    private Bundle createBundleFromAppInfo(int uid, String appName) {
        Bundle extras = new Bundle();
        if (uid >= 0) {
            extras.putInt("appUid", uid);
        }
        extras.putString("appName", appName);
        return extras;
    }

    private int traceProcess(boolean launched, String reason, ArrayMap<String, String> appInfo, long timestamp) {
        ArrayMap<String, String> arrayMap = appInfo;
        if (TextUtils.isEmpty(reason)) {
            return -1;
        }
        String str;
        try {
            String packageName = (String) arrayMap.get("packageName");
            String processName = (String) arrayMap.get("processName");
            Integer uid = Integer.valueOf(Integer.parseInt((String) arrayMap.get("uid")));
            PackageTracker tracker = PackageTracker.getInstance();
            if (launched) {
                tracker.addStartRecord(reason, packageName, uid.intValue(), processName, timestamp);
                if (AwareConstant.CURRENT_USER_TYPE == 3) {
                    try {
                        if (!"activity".equals(reason) || packageName == null || processName == null) {
                        } else {
                            try {
                                this.mFgPkgName = packageName;
                                BigDataStore instance = BigDataStore.getInstance();
                                instance.coldLaunch++;
                                String str2 = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("pkg: ");
                                stringBuilder.append(packageName);
                                stringBuilder.append(", coldLaunch: ");
                                stringBuilder.append(BigDataStore.getInstance().coldLaunch);
                                AwareLog.d(str2, stringBuilder.toString());
                                AwareAppMngDFX.getInstance().trackeAppStartInfo(packageName, processName, 10);
                            } catch (NumberFormatException e) {
                                AwareLog.e(TAG, "failed to get uid");
                                return -1;
                            }
                        }
                    } catch (NumberFormatException e2) {
                        AwareLog.e(TAG, "failed to get uid");
                        return -1;
                    }
                }
                str = reason;
            } else {
                tracker.addExitRecord(reason, packageName, uid.intValue(), processName, timestamp);
            }
            return 0;
        } catch (NumberFormatException e3) {
            str = reason;
            AwareLog.e(TAG, "failed to get uid");
            return -1;
        }
    }

    private void setActivityLaunching(boolean status) {
        if (status) {
            this.mIsActivityLaunching = true;
            this.mAcvityLaunchBeginTimestamp = SystemClock.uptimeMillis();
            return;
        }
        this.mIsActivityLaunching = false;
    }

    private boolean isLauncher(String packageName) {
        if (Process.myUid() != 1000 || packageName == null || packageName.trim().isEmpty()) {
            return false;
        }
        if (GestureNavConst.DEFAULT_LAUNCHER_PACKAGE.equals(packageName) || AwareAppAssociate.getInstance().getDefaultHomePackages().contains(packageName)) {
            return true;
        }
        return false;
    }
}
