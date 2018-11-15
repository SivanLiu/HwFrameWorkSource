package com.android.server.rms.iaware;

import android.app.mtm.MultiTaskManager;
import android.os.Binder;
import android.os.Bundle;
import android.rms.HwSysResManager;
import android.rms.iaware.AwareConstant.ResourceType;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import android.rms.iaware.DataContract.Apps;
import android.rms.iaware.DataContract.Apps.Builder;
import android.rms.iaware.DataContract.Input;
import android.rms.iaware.LogIAware;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.mtm.iaware.appmng.AwareAppMngSort;
import com.android.server.rms.iaware.appmng.AwareDefaultConfigList;
import com.android.server.rms.iaware.appmng.AwareFakeActivityRecg;
import com.android.server.rms.iaware.appmng.AwareIntelligentRecg;
import com.android.server.rms.iaware.cpu.CPUFeatureAMSCommunicator;
import com.android.server.rms.iaware.cpu.CPUKeyBackground;
import com.android.server.rms.iaware.cpu.CPUResourceConfigControl;
import com.android.server.rms.iaware.cpu.CPUVipThread;
import com.android.server.rms.iaware.memory.utils.MemoryUtils;
import com.android.server.rms.memrepair.ProcStateStatisData;
import com.huawei.android.app.IHwDAMonitorCallback.Stub;
import com.huawei.displayengine.IDisplayEngineService;
import java.util.ArrayList;

class AwareAmsMonitorCallback extends Stub {
    private static final String ACTIVITY_DESTROYED = "DESTROYED";
    private static final String ACTIVITY_RESUME = "RESUMED";
    private static final int ACTIVITY_STATE_INFO_LENGTH = 5;
    private static final String ACTIVITY_STOPPED = "STOPPED";
    private static final int FOREGROUND_INFO_LENGTH = 3;
    private static final String TAG = "AwareDAMonitorCallback";

    AwareAmsMonitorCallback() {
    }

    public int getActivityImportCount() {
        return 2;
    }

    public String getRecentTask() {
        return AwareAppMngSort.ACTIVITY_RECENT_TASK;
    }

    public int isCPUConfigWhiteList(String processName) {
        return CPUResourceConfigControl.getInstance().isWhiteList(processName);
    }

    public int getCPUConfigGroupBG() {
        return 1;
    }

    public int getFirstDevSchedEventId() {
        return 2100;
    }

    public void notifyActivityState(String activityInfo) {
        String str;
        StringBuilder stringBuilder;
        int uid;
        int i;
        int i2;
        String str2 = activityInfo;
        HwSysResManager resManager = HwSysResManager.getInstance();
        if (resManager == null || !resManager.isResourceNeeded(ResourceType.getReousrceId(ResourceType.RES_APP))) {
            return;
        }
        String str3;
        if (isInvalidStr(activityInfo)) {
            str3 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("invalid str. activityInfo : ");
            stringBuilder2.append(str2);
            AwareLog.e(str3, stringBuilder2.toString());
            return;
        }
        String[] info = str2.split(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
        if (5 != info.length) {
            str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("info error. activityInfo : ");
            stringBuilder3.append(str2);
            AwareLog.e(str3, stringBuilder3.toString());
            return;
        }
        int event = -1;
        String packageName;
        String activityName;
        try {
            int pid;
            packageName = info[0];
            try {
                activityName = info[1];
            } catch (NumberFormatException e) {
                activityName = null;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("NumberFormatException, noteActivityInfo : ");
                stringBuilder.append(str2);
                AwareLog.e(str, stringBuilder.toString());
            } catch (ArrayIndexOutOfBoundsException e2) {
                activityName = null;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("ArrayIndexOutOfBoundsException, noteActivityInfo : ");
                stringBuilder.append(str2);
                AwareLog.e(str, stringBuilder.toString());
            }
            try {
                uid = Integer.parseInt(info[2]);
            } catch (NumberFormatException e3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("NumberFormatException, noteActivityInfo : ");
                stringBuilder.append(str2);
                AwareLog.e(str, stringBuilder.toString());
            } catch (ArrayIndexOutOfBoundsException e4) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("ArrayIndexOutOfBoundsException, noteActivityInfo : ");
                stringBuilder.append(str2);
                AwareLog.e(str, stringBuilder.toString());
            }
            try {
                pid = Integer.parseInt(info[3]);
            } catch (NumberFormatException e5) {
                i = uid;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("NumberFormatException, noteActivityInfo : ");
                stringBuilder.append(str2);
                AwareLog.e(str, stringBuilder.toString());
            } catch (ArrayIndexOutOfBoundsException e6) {
                i = uid;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("ArrayIndexOutOfBoundsException, noteActivityInfo : ");
                stringBuilder.append(str2);
                AwareLog.e(str, stringBuilder.toString());
            }
            try {
                str3 = info[4];
                if (isInvalidActivityInfo(packageName, activityName, str3, uid, pid)) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("invalid activity info, activityInfo : ");
                    stringBuilder.append(str2);
                    AwareLog.e(str, stringBuilder.toString());
                    return;
                }
                if (ACTIVITY_RESUME.equals(str3)) {
                    str = 15019;
                } else if ("STOPPED".equals(str3) || ACTIVITY_DESTROYED.equals(str3)) {
                    str = 85019;
                } else {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("state out of control, state : ");
                    stringBuilder.append(str3);
                    AwareLog.e(str, stringBuilder.toString());
                    return;
                }
                Builder builder = Apps.builder();
                builder.addEvent(str);
                builder.addCalledApp(packageName, null, activityName, pid, uid);
                String activityName2 = builder.build();
                String state = Binder.clearCallingIdentity();
                resManager.reportData(activityName2);
                Binder.restoreCallingIdentity(state);
            } catch (NumberFormatException e7) {
                i = uid;
                i2 = pid;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("NumberFormatException, noteActivityInfo : ");
                stringBuilder.append(str2);
                AwareLog.e(str, stringBuilder.toString());
            } catch (ArrayIndexOutOfBoundsException e8) {
                i = uid;
                i2 = pid;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("ArrayIndexOutOfBoundsException, noteActivityInfo : ");
                stringBuilder.append(str2);
                AwareLog.e(str, stringBuilder.toString());
            }
        } catch (NumberFormatException e9) {
            packageName = null;
            activityName = null;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("NumberFormatException, noteActivityInfo : ");
            stringBuilder.append(str2);
            AwareLog.e(str, stringBuilder.toString());
        } catch (ArrayIndexOutOfBoundsException e10) {
            packageName = null;
            activityName = null;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("ArrayIndexOutOfBoundsException, noteActivityInfo : ");
            stringBuilder.append(str2);
            AwareLog.e(str, stringBuilder.toString());
        }
    }

    public void notifyAppToTop(String msg) {
        String str;
        StringBuilder stringBuilder;
        if (msg == null) {
            AwareLog.e(TAG, "notifyForeGroundChange, msg is null, error!");
            return;
        }
        HwSysResManager resManager = HwSysResManager.getInstance();
        if (resManager != null && resManager.isResourceNeeded(ResourceType.getReousrceId(ResourceType.RES_APP))) {
            String[] info = msg.split(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
            String str2;
            if (3 != info.length) {
                str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("info error. msg : ");
                stringBuilder2.append(msg);
                AwareLog.e(str2, stringBuilder2.toString());
                return;
            }
            int pid;
            try {
                int uid;
                pid = Integer.parseInt(info[0]);
                try {
                    uid = Integer.parseInt(info[1]);
                } catch (NumberFormatException e) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("NumberFormatException, msg : ");
                    stringBuilder.append(msg);
                    AwareLog.e(str, stringBuilder.toString());
                }
                try {
                    String processName = info[2];
                    str2 = Apps.builder();
                    str2.addEvent(15020);
                    str2.addCalledApp(null, processName, null, pid, uid);
                    CollectData appsData = str2.build();
                    long id = Binder.clearCallingIdentity();
                    resManager.reportData(appsData);
                    Binder.restoreCallingIdentity(id);
                } catch (NumberFormatException e2) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("NumberFormatException, msg : ");
                    stringBuilder.append(msg);
                    AwareLog.e(str, stringBuilder.toString());
                }
            } catch (NumberFormatException e3) {
                pid = -1;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("NumberFormatException, msg : ");
                stringBuilder.append(msg);
                AwareLog.e(str, stringBuilder.toString());
            }
        }
    }

    private boolean isInvalidStr(String str) {
        return str == null || str.trim().isEmpty();
    }

    private boolean isInvalidActivityInfo(String packageName, String activityName, String state, int uid, int pid) {
        return packageName == null || activityName == null || state == null || uid <= 1000 || pid < 0;
    }

    public int DAMonitorReport(int tag, String msg) {
        if (2100 == tag) {
            notifyAppToTop(msg);
        }
        return LogIAware.report(tag, msg);
    }

    public void reportScreenRecord(int uid, int pid, int status) {
        HwSysResManager resManager = HwSysResManager.getInstance();
        if (resManager != null && resManager.isResourceNeeded(ResourceType.getReousrceId(ResourceType.RESOURCE_APPASSOC))) {
            int reportStatus;
            if (status == 0) {
                reportStatus = 26;
            } else {
                reportStatus = 25;
            }
            Bundle bundleArgs = new Bundle();
            bundleArgs.putInt("callUid", uid);
            bundleArgs.putInt("callPid", pid);
            bundleArgs.putInt("relationType", reportStatus);
            CollectData data = new CollectData(ResourceType.getReousrceId(ResourceType.RESOURCE_APPASSOC), System.currentTimeMillis(), bundleArgs);
            long origId = Binder.clearCallingIdentity();
            HwSysResManager.getInstance().reportData(data);
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void reportCamera(int uid, int status) {
        if (uid > 0 && uid != 1000) {
            HwSysResManager resManager = HwSysResManager.getInstance();
            if (resManager != null && resManager.isResourceNeeded(ResourceType.getReousrceId(ResourceType.RESOURCE_APPASSOC))) {
                int reportStatus;
                if (status == 0) {
                    reportStatus = 31;
                } else {
                    reportStatus = 30;
                }
                Bundle bundleArgs = new Bundle();
                bundleArgs.putInt("callUid", uid);
                bundleArgs.putInt("relationType", reportStatus);
                CollectData data = new CollectData(ResourceType.getReousrceId(ResourceType.RESOURCE_APPASSOC), System.currentTimeMillis(), bundleArgs);
                long origId = Binder.clearCallingIdentity();
                HwSysResManager.getInstance().reportData(data);
                Binder.restoreCallingIdentity(origId);
            }
        }
    }

    public void notifyProcessGroupChangeCpu(int pid, int uid, int grp) {
        CPUKeyBackground.getInstance().notifyProcessGroupChange(pid, uid, grp);
    }

    public void setVipThread(int pid, int renderThreadTid, boolean isSet) {
        ArrayList<Integer> tidStrs = new ArrayList();
        tidStrs.add(Integer.valueOf(pid));
        tidStrs.add(Integer.valueOf(renderThreadTid));
        CPUVipThread.getInstance().setAppVipThread(pid, tidStrs, isSet);
    }

    public void notifyAppEventToIaware(int type, String packageName) {
        CPUFeatureAMSCommunicator.getInstance().setTopAppToBoost(type, packageName);
    }

    public void onPointerEvent(int action) {
        if (action == 0 || action == 1) {
            HwSysResManager resManager = HwSysResManager.getInstance();
            if (resManager != null && resManager.isResourceNeeded(ResourceType.getReousrceId(ResourceType.RES_INPUT))) {
                Input.Builder builder = Input.builder();
                if (action == 0) {
                    builder.addEvent(IDisplayEngineService.DE_ACTION_PG_BROWSER_FRONT);
                } else {
                    builder.addEvent(80001);
                }
                CollectData appsData = builder.build();
                long id = Binder.clearCallingIdentity();
                resManager.reportData(appsData);
                Binder.restoreCallingIdentity(id);
            }
        }
    }

    public void noteActivityStart(String packageName, String processName, String activityName, int pid, int uid, boolean started) {
        HwSysResManager resManager = HwSysResManager.getInstance();
        if (resManager != null && resManager.isResourceNeeded(ResourceType.getReousrceId(ResourceType.RES_APP))) {
            int i;
            Builder builder = Apps.builder();
            if (started) {
                i = 15005;
            } else {
                i = 85005;
            }
            builder.addEvent(i);
            builder.addCalledApp(packageName, processName, activityName, pid, uid);
            CollectData appsData = builder.build();
            long id = Binder.clearCallingIdentity();
            resManager.reportData(appsData);
            Binder.restoreCallingIdentity(id);
        }
    }

    public void addPssToMap(String procName, int uid, int pid, int procState, long pss, long now, boolean test) {
        ProcStateStatisData.getInstance().addPssToMap(procName, uid, pid, procState, pss, now, test);
    }

    public void reportAppDiedMsg(int userId, String processName, String reason) {
        if (processName != null && !processName.contains(":") && reason != null) {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(processName);
            stringBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
            stringBuffer.append(String.valueOf(userId));
            stringBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
            stringBuffer.append(reason);
            LogIAware.report(HwArbitrationDEFS.MSG_SET_PingPong_WiFi_Good_FALSE, stringBuffer.toString());
        }
    }

    public int killProcessGroupForQuickKill(int uid, int pid) {
        return MemoryUtils.killProcessGroupForQuickKill(uid, pid);
    }

    public void noteProcessStart(String packageName, String processName, int pid, int uid, boolean started, String launcherMode, String reason) {
        HwSysResManager resManager = HwSysResManager.getInstance();
        if (resManager != null && resManager.isResourceNeeded(ResourceType.getReousrceId(ResourceType.RES_APP))) {
            int i;
            Builder builder = Apps.builder();
            if (started) {
                i = 15001;
            } else {
                i = 85001;
            }
            builder.addEvent(i);
            builder.addLaunchCalledApp(packageName, processName, launcherMode, reason, pid, uid);
            CollectData appsData = builder.build();
            long id = Binder.clearCallingIdentity();
            resManager.reportData(appsData);
            Binder.restoreCallingIdentity(id);
        }
    }

    public void onWakefulnessChanged(int wakefulness) {
        AwareFakeActivityRecg.self().onWakefulnessChanged(wakefulness);
    }

    public void recognizeFakeActivity(String compName, boolean isScreenOn, int pid, int uid) {
        AwareFakeActivityRecg.self().recognizeFakeActivity(compName, isScreenOn, pid, uid);
    }

    public void notifyProcessGroupChange(int pid, int uid) {
        MultiTaskManager handler = MultiTaskManager.getInstance();
        if (handler != null) {
            handler.notifyProcessGroupChange(pid, uid);
        }
    }

    public void notifyProcessStatusChange(String pkg, String process, String hostingType, int pid, int uid) {
        MultiTaskManager handler = MultiTaskManager.getInstance();
        if (handler != null) {
            handler.notifyProcessStatusChange(pkg, process, hostingType, pid, uid);
        }
    }

    public void notifyProcessWillDie(boolean byForceStop, boolean crashed, boolean byAnr, String packageName, int pid, int uid) {
        AwareFakeActivityRecg.self().notifyProcessWillDie(byForceStop, crashed, byAnr, packageName, pid, uid);
    }

    public void notifyProcessDied(int pid, int uid) {
        MultiTaskManager handler = MultiTaskManager.getInstance();
        if (handler != null) {
            handler.notifyProcessDiedChange(pid, uid);
        }
    }

    public int resetAppMngOomAdj(int maxAdj, String packageName) {
        if (maxAdj > AwareDefaultConfigList.HW_PERCEPTIBLE_APP_ADJ && AwareAppMngSort.checkAppMngEnable() && AwareDefaultConfigList.getInstance().isAppMngOomAdjCustomized(packageName)) {
            return AwareDefaultConfigList.HW_PERCEPTIBLE_APP_ADJ;
        }
        return maxAdj;
    }

    public boolean isResourceNeeded(String resourceid) {
        HwSysResManager resManager = HwSysResManager.getInstance();
        if (resManager == null) {
            return false;
        }
        return resManager.isResourceNeeded(getReousrceId(resourceid));
    }

    private int getReousrceId(String resourceid) {
        if (resourceid == null) {
            return ResourceType.getReousrceId(ResourceType.RESOURCE_INVALIDE_TYPE);
        }
        if (resourceid.equals("RESOURCE_APPASSOC")) {
            return ResourceType.getReousrceId(ResourceType.RESOURCE_APPASSOC);
        }
        return ResourceType.getReousrceId(ResourceType.RESOURCE_INVALIDE_TYPE);
    }

    public void reportData(String resourceid, long timestamp, Bundle args) {
        HwSysResManager resManager = HwSysResManager.getInstance();
        if (resManager != null && args != null) {
            CollectData data = new CollectData(getReousrceId(resourceid), timestamp, args);
            long id = Binder.clearCallingIdentity();
            resManager.reportData(data);
            Binder.restoreCallingIdentity(id);
        }
    }

    public boolean isExcludedInBGCheck(String pkg, String action) {
        return AwareIntelligentRecg.getInstance().isExcludedInBGCheck(pkg, action);
    }

    public void noteActivityDisplayedStart(String componentName, int uid, int pid) {
        if (componentName != null && pid > 0) {
            HwSysResManager resManager = HwSysResManager.getInstance();
            if (resManager != null && resManager.isResourceNeeded(ResourceType.getReousrceId(ResourceType.RES_APP))) {
                Builder builder = Apps.builder();
                builder.addEvent(15013);
                builder.addActivityDisplayedInfoWithUid(componentName, uid, pid, 0);
                CollectData appsData = builder.build();
                long id = Binder.clearCallingIdentity();
                resManager.reportData(appsData);
                Binder.restoreCallingIdentity(id);
            }
        }
    }
}
