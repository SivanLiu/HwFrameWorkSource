package com.android.server.rms.iaware;

import android.app.AppGlobals;
import android.content.pm.ApplicationInfo;
import android.hwrme.HwPrommEventManager;
import android.hwrme.HwResMngEngine;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.rms.HwSysResManager;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import android.rms.iaware.DataContract;
import com.android.server.rms.iaware.appmng.AwareFakeActivityRecg;
import com.android.server.rms.iaware.cpu.CPUCustBaseConfig;
import com.android.server.rms.iaware.cpu.CPUFeatureAMSCommunicator;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.huawei.android.app.IHwAtmDAMonitorCallback;

class AwareAtmsMonitorCallback extends IHwAtmDAMonitorCallback.Stub {
    private static final String ACTIVITY_DESTROYED = "DESTROYED";
    private static final String ACTIVITY_RESUME = "RESUMED";
    private static final int ACTIVITY_STATE_INFO_LENGTH = 5;
    private static final String ACTIVITY_STOPPED = "STOPPED";
    private static final String EMPTY_STRING = "";
    private static final String TAG = "AwareAtmDAMonitorCallback";
    private boolean isStartedActivity = false;

    AwareAtmsMonitorCallback() {
    }

    public void noteActivityStart(String packageName, String processName, String activityName, int pid, int uid, boolean started) {
        int event;
        HwSysResManager resManager = HwSysResManager.getInstance();
        if (resManager != null && resManager.isResourceNeeded(AwareConstant.ResourceType.getReousrceId(AwareConstant.ResourceType.RES_APP))) {
            DataContract.Apps.Builder builder = DataContract.Apps.builder();
            if (started) {
                event = 15005;
            } else {
                event = 85005;
            }
            builder.addEvent(event);
            builder.addCalledApp(packageName, processName, activityName, pid, uid);
            CollectData appsData = builder.build();
            long id = Binder.clearCallingIdentity();
            resManager.reportData(appsData);
            Binder.restoreCallingIdentity(id);
        }
    }

    public void notifyAppEventToIaware(int type, String packageName) {
        CPUFeatureAMSCommunicator.getInstance().setTopAppToBoost(type, packageName);
        if (type == 3) {
            try {
                ApplicationInfo ai = AppGlobals.getPackageManager().getApplicationInfo(packageName, 0, 0);
                if (ai != null) {
                    HwResMngEngine.getInstance().sendMmEvent(0, ai.uid);
                }
            } catch (RemoteException e) {
                AwareLog.e(TAG, "promm package not found!");
            }
        }
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
        return AwareConstant.ResourceType.getReousrceId(AwareConstant.ResourceType.RESOURCE_INVALIDE_TYPE);
    }

    public void reportData(String resourceid, long timestamp, Bundle args) {
        HwSysResManager resManager;
        if (args != null && (resManager = HwSysResManager.getInstance()) != null) {
            CollectData data = new CollectData(getReousrceId(resourceid), timestamp, args);
            long id = Binder.clearCallingIdentity();
            resManager.reportData(data);
            Binder.restoreCallingIdentity(id);
        }
    }

    public void recognizeFakeActivity(String compName, int pid, int uid) {
        AwareFakeActivityRecg.self().recognizeFakeActivity(compName, pid, uid);
    }

    private boolean isInvalidActivityInfo(String packageName, String activityName, String state, int uid, int pid) {
        if (packageName == null || activityName == null || state == null || uid <= 1000 || pid < 0) {
            return true;
        }
        return false;
    }

    private boolean isInvalidStr(String str) {
        return str == null || str.trim().isEmpty();
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

    public void noteActivityDisplayed(String componentName, int uid, int pid, boolean isStart) {
        if (componentName != null && pid > 0) {
            if (isStart || this.isStartedActivity) {
                this.isStartedActivity = isStart;
                HwSysResManager resManager = HwSysResManager.getInstance();
                if (resManager != null && resManager.isResourceNeeded(AwareConstant.ResourceType.getReousrceId(AwareConstant.ResourceType.RES_APP))) {
                    DataContract.Apps.Builder builder = DataContract.Apps.builder();
                    if (!isStart) {
                        MemoryConstant.setDisplayStartedActivityName("");
                        builder.addEvent(85013);
                    } else if (!componentName.equals(MemoryConstant.getDisplayStartedActivityName())) {
                        MemoryConstant.setDisplayStartedActivityName(componentName);
                        builder.addEvent(15013);
                    } else {
                        return;
                    }
                    builder.addActivityDisplayedInfoWithUid(componentName, uid, pid, 0);
                    CollectData appsData = builder.build();
                    long id = Binder.clearCallingIdentity();
                    resManager.reportData(appsData);
                    Binder.restoreCallingIdentity(id);
                    HwPrommEventManager prommEventMng = HwPrommEventManager.getInstance();
                    if (prommEventMng != null) {
                        prommEventMng.getActivityDisplayed(uid);
                    }
                }
            }
        }
    }
}
