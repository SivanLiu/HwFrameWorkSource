package com.android.server.rms.iaware;

import android.app.mtm.MultiTaskManager;
import android.os.Binder;
import android.os.Bundle;
import android.rms.HwSysResManager;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import android.rms.iaware.DataContract;
import android.rms.iaware.LogIAware;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.mtm.iaware.appmng.AwareAppMngSort;
import com.android.server.mtm.taskstatus.ProcessCleaner;
import com.android.server.rms.iaware.appmng.AwareDefaultConfigList;
import com.android.server.rms.iaware.appmng.AwareFakeActivityRecg;
import com.android.server.rms.iaware.appmng.AwareIntelligentRecg;
import com.android.server.rms.iaware.cpu.CPUCustBaseConfig;
import com.android.server.rms.iaware.cpu.CPUKeyBackground;
import com.android.server.rms.iaware.cpu.CPUResourceConfigControl;
import com.android.server.rms.iaware.cpu.CPUVipThread;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.rms.iaware.memory.utils.MemoryUtils;
import com.android.server.rms.iaware.qos.AwareBinderSchedManager;
import com.android.server.rms.iaware.qos.AwareQosFeatureManager;
import com.android.server.rms.memrepair.ProcStateStatisData;
import com.huawei.android.app.IHwDAMonitorCallback;
import com.huawei.displayengine.IDisplayEngineService;
import com.huawei.hiai.awareness.AwarenessInnerConstants;
import java.util.ArrayList;

class AwareAmsMonitorCallback extends IHwDAMonitorCallback.Stub {
    private static final String ACTIVITY_DESTROYED = "DESTROYED";
    private static final String ACTIVITY_RESUME = "RESUMED";
    private static final int ACTIVITY_STATE_INFO_LENGTH = 5;
    private static final String ACTIVITY_STOPPED = "STOPPED";
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
        int uid;
        int pid;
        int event;
        HwSysResManager resManager = HwSysResManager.getInstance();
        if (resManager == null || !resManager.isResourceNeeded(AwareConstant.ResourceType.getReousrceId(AwareConstant.ResourceType.RES_APP))) {
            return;
        }
        if (isInvalidStr(activityInfo)) {
            AwareLog.e(TAG, "invalid str. activityInfo : " + activityInfo);
            return;
        }
        String[] info = activityInfo.split(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
        if (5 != info.length) {
            AwareLog.e(TAG, "info error. activityInfo : " + activityInfo);
            return;
        }
        try {
            String packageName = info[0];
            try {
                String activityName = info[1];
                try {
                    uid = Integer.parseInt(info[2]);
                } catch (NumberFormatException e) {
                    AwareLog.e(TAG, "NumberFormatException, noteActivityInfo : " + activityInfo);
                } catch (ArrayIndexOutOfBoundsException e2) {
                    AwareLog.e(TAG, "ArrayIndexOutOfBoundsException, noteActivityInfo : " + activityInfo);
                }
                try {
                    pid = Integer.parseInt(info[3]);
                } catch (NumberFormatException e3) {
                    AwareLog.e(TAG, "NumberFormatException, noteActivityInfo : " + activityInfo);
                } catch (ArrayIndexOutOfBoundsException e4) {
                    AwareLog.e(TAG, "ArrayIndexOutOfBoundsException, noteActivityInfo : " + activityInfo);
                }
                try {
                    String state = info[4];
                    if (isInvalidActivityInfo(packageName, activityName, state, uid, pid)) {
                        AwareLog.e(TAG, "invalid activity info, activityInfo : " + activityInfo);
                        return;
                    }
                    if (ACTIVITY_RESUME.equals(state)) {
                        event = 15019;
                    } else if ("STOPPED".equals(state) || ACTIVITY_DESTROYED.equals(state)) {
                        event = 85019;
                    } else {
                        AwareLog.e(TAG, "state out of control, state : " + state);
                        return;
                    }
                    DataContract.Apps.Builder builder = DataContract.Apps.builder();
                    builder.addEvent(event);
                    builder.addCalledApp(packageName, (String) null, activityName, pid, uid);
                    CollectData appsData = builder.build();
                    long id = Binder.clearCallingIdentity();
                    resManager.reportData(appsData);
                    Binder.restoreCallingIdentity(id);
                } catch (NumberFormatException e5) {
                    AwareLog.e(TAG, "NumberFormatException, noteActivityInfo : " + activityInfo);
                } catch (ArrayIndexOutOfBoundsException e6) {
                    AwareLog.e(TAG, "ArrayIndexOutOfBoundsException, noteActivityInfo : " + activityInfo);
                }
            } catch (NumberFormatException e7) {
                AwareLog.e(TAG, "NumberFormatException, noteActivityInfo : " + activityInfo);
            } catch (ArrayIndexOutOfBoundsException e8) {
                AwareLog.e(TAG, "ArrayIndexOutOfBoundsException, noteActivityInfo : " + activityInfo);
            }
        } catch (NumberFormatException e9) {
            AwareLog.e(TAG, "NumberFormatException, noteActivityInfo : " + activityInfo);
        } catch (ArrayIndexOutOfBoundsException e10) {
            AwareLog.e(TAG, "ArrayIndexOutOfBoundsException, noteActivityInfo : " + activityInfo);
        }
    }

    private boolean isInvalidStr(String str) {
        return str == null || str.trim().isEmpty();
    }

    private boolean isInvalidActivityInfo(String packageName, String activityName, String state, int uid, int pid) {
        return packageName == null || activityName == null || state == null || uid <= 1000 || pid < 0;
    }

    public void reportScreenRecord(int uid, int pid, int status) {
        int reportStatus;
        HwSysResManager resManager = HwSysResManager.getInstance();
        if (resManager != null && resManager.isResourceNeeded(AwareConstant.ResourceType.getReousrceId(AwareConstant.ResourceType.RESOURCE_APPASSOC))) {
            if (status == 0) {
                reportStatus = 26;
            } else {
                reportStatus = 25;
            }
            Bundle bundleArgs = new Bundle();
            bundleArgs.putInt("callUid", uid);
            bundleArgs.putInt("callPid", pid);
            bundleArgs.putInt("relationType", reportStatus);
            CollectData data = new CollectData(AwareConstant.ResourceType.getReousrceId(AwareConstant.ResourceType.RESOURCE_APPASSOC), System.currentTimeMillis(), bundleArgs);
            long origId = Binder.clearCallingIdentity();
            HwSysResManager.getInstance().reportData(data);
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void reportCamera(int uid, int status) {
        HwSysResManager resManager;
        int reportStatus;
        if (uid > 0 && uid != 1000 && (resManager = HwSysResManager.getInstance()) != null && resManager.isResourceNeeded(AwareConstant.ResourceType.getReousrceId(AwareConstant.ResourceType.RESOURCE_APPASSOC))) {
            if (status == 0) {
                reportStatus = 31;
            } else {
                reportStatus = 30;
            }
            Bundle bundleArgs = new Bundle();
            bundleArgs.putInt("callUid", uid);
            bundleArgs.putInt("relationType", reportStatus);
            CollectData data = new CollectData(AwareConstant.ResourceType.getReousrceId(AwareConstant.ResourceType.RESOURCE_APPASSOC), System.currentTimeMillis(), bundleArgs);
            long origId = Binder.clearCallingIdentity();
            HwSysResManager.getInstance().reportData(data);
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void notifyProcessGroupChangeCpu(int pid, int uid, int renderThreadTid, int grp) {
        CPUKeyBackground.getInstance().notifyProcessGroupChange(pid, uid, grp);
        CPUVipThread.getInstance().notifyProcessGroupChange(pid, renderThreadTid, grp);
    }

    public void setVipThread(int uid, int pid, int renderThreadTid, boolean isSet, boolean isSetGroup) {
        ArrayList<Integer> tidStrs = new ArrayList<>();
        tidStrs.add(Integer.valueOf(pid));
        tidStrs.add(Integer.valueOf(renderThreadTid));
        CPUVipThread.getInstance().setAppVipThread(pid, tidStrs, isSet, isSetGroup);
        if (isSet) {
            AwareBinderSchedManager.getInstance().reportFgChanged(pid, uid, true);
            AwareQosFeatureManager.getInstance().setAppVipThreadForQos(pid, tidStrs);
        }
    }

    public void onPointerEvent(int action) {
        HwSysResManager resManager;
        if ((action == 0 || action == 1) && (resManager = HwSysResManager.getInstance()) != null && resManager.isResourceNeeded(AwareConstant.ResourceType.getReousrceId(AwareConstant.ResourceType.RES_INPUT))) {
            DataContract.Input.Builder builder = DataContract.Input.builder();
            if (action == 0) {
                builder.addEvent((int) IDisplayEngineService.DE_ACTION_PG_BROWSER_FRONT);
            } else {
                builder.addEvent(80001);
            }
            CollectData appsData = builder.build();
            long id = Binder.clearCallingIdentity();
            resManager.reportData(appsData);
            Binder.restoreCallingIdentity(id);
        }
    }

    public void addPssToMap(String packageName, String procName, int uid, int pid, int procState, long pss, long uss, long swapPss, boolean test) {
        ProcStateStatisData.getInstance().addPssToMap(new ProcStateStatisData.ProcAttribute(packageName, procName, uid, pid, procState), pss, uss, swapPss, test);
    }

    public void reportAppDiedMsg(int userId, String processName, String reason) {
        if (processName != null && !processName.contains(AwarenessInnerConstants.COLON_KEY) && reason != null) {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(processName);
            stringBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
            stringBuffer.append(String.valueOf(userId));
            stringBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
            stringBuffer.append(reason);
            LogIAware.report((int) HwArbitrationDEFS.MSG_SET_PINGPONG_WIFI_GOOD_FALSE, stringBuffer.toString());
        }
    }

    public int killProcessGroupForQuickKill(int uid, int pid) {
        return MemoryUtils.killProcessGroupForQuickKill(uid, pid);
    }

    public void noteProcessStart(String packageName, String processName, int pid, int uid, boolean started, String launcherMode, String reason) {
        int event;
        HwSysResManager resManager = HwSysResManager.getInstance();
        if (resManager != null && resManager.isResourceNeeded(AwareConstant.ResourceType.getReousrceId(AwareConstant.ResourceType.RES_APP))) {
            DataContract.Apps.Builder builder = DataContract.Apps.builder();
            if (started) {
                event = 15001;
            } else {
                event = 85001;
            }
            builder.addEvent(event);
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
        if (maxAdj <= 260 || !AwareAppMngSort.checkAppMngEnable() || !AwareDefaultConfigList.getInstance().isAppMngOomAdjCustomized(packageName)) {
            return maxAdj;
        }
        return AwareDefaultConfigList.HW_PERCEPTIBLE_APP_ADJ;
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
            return AwareConstant.ResourceType.getReousrceId(AwareConstant.ResourceType.RESOURCE_INVALIDE_TYPE);
        }
        if (resourceid.equals("RESOURCE_APPASSOC")) {
            return AwareConstant.ResourceType.getReousrceId(AwareConstant.ResourceType.RESOURCE_APPASSOC);
        }
        if (resourceid.equals("RESOURCE_WINSTATE")) {
            return AwareConstant.ResourceType.getReousrceId(AwareConstant.ResourceType.RESOURCE_WINSTATE);
        }
        return AwareConstant.ResourceType.getReousrceId(AwareConstant.ResourceType.RESOURCE_INVALIDE_TYPE);
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
        HwSysResManager resManager;
        if (componentName != null && pid > 0 && (resManager = HwSysResManager.getInstance()) != null && resManager.isResourceNeeded(AwareConstant.ResourceType.getReousrceId(AwareConstant.ResourceType.RES_APP))) {
            DataContract.Apps.Builder builder = DataContract.Apps.builder();
            builder.addEvent(15013);
            builder.addActivityDisplayedInfoWithUid(componentName, uid, pid, 0);
            CollectData appsData = builder.build();
            long id = Binder.clearCallingIdentity();
            resManager.reportData(appsData);
            Binder.restoreCallingIdentity(id);
        }
    }

    public boolean isFastKillSwitch(String processName, int uid) {
        ProcessCleaner cleaner;
        if ((MemoryConstant.isFastKillSwitch() || MemoryConstant.isFastQuickKillSwitch()) && (cleaner = ProcessCleaner.getInstance()) != null) {
            return cleaner.isProcessFastKillLocked(processName, uid);
        }
        return false;
    }
}
