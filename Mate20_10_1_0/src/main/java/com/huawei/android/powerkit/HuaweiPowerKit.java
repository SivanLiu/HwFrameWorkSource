package com.huawei.android.powerkit;

import android.content.Context;
import android.os.RemoteException;
import com.huawei.android.powerkit.adapter.PowerKitApi;
import java.util.List;

public class HuaweiPowerKit {
    public static final int RES_TYPE_ALARM = 256;
    public static final int RES_TYPE_ALL = 65535;
    public static final int RES_TYPE_AUTOSTART = 64;
    public static final int RES_TYPE_BT_SCN = 8;
    public static final int RES_TYPE_CPU = 1;
    public static final int RES_TYPE_GPS = 4;
    public static final int RES_TYPE_HIGH_CURRENT = 128;
    public static final int RES_TYPE_KWAKELOCK = 1024;
    public static final int RES_TYPE_NETLOCATION = 32;
    public static final int RES_TYPE_NETWORK = 512;
    public static final int RES_TYPE_WAKELOCK = 2;
    public static final int RES_TYPE_WIFI_SCN = 16;
    public static final int STATE_RESOURCE_ABNORMAL = 50;
    public static final int STATE_THERMAL = 9;
    private static final String TAG = "HuaweiPowerKit";
    private static HuaweiPowerKit sInstance = null;
    private PowerKitApi mApi = null;
    private Context mContext;

    private HuaweiPowerKit(Context context, PowerKitConnection pkConnection) {
        this.mContext = context;
        this.mApi = new PowerKitApi(context, pkConnection);
    }

    public static HuaweiPowerKit getInstance(Context context, PowerKitConnection pkConnection) {
        HuaweiPowerKit huaweiPowerKit;
        synchronized (HuaweiPowerKit.class) {
            if (sInstance == null) {
                sInstance = new HuaweiPowerKit(context, pkConnection);
            }
            huaweiPowerKit = sInstance;
        }
        return huaweiPowerKit;
    }

    public String getPowerKitVersion() throws RemoteException {
        return this.mApi.getPowerKitVersion(this.mContext);
    }

    public float getCurrentResolutionRatio() throws RemoteException {
        return this.mApi.getCurrentResolutionRatio(this.mContext);
    }

    public int getCurrentFps() throws RemoteException {
        return this.mApi.getCurrentFps(this.mContext);
    }

    public int setFps(int fps) throws RemoteException {
        return this.mApi.setFps(this.mContext, fps);
    }

    public boolean applyForResourceUse(String module, int resourceType, long timeoutInMS, String reason) throws RemoteException {
        return this.mApi.applyForResourceUse(this.mContext, true, module, resourceType, timeoutInMS, reason);
    }

    public boolean unapplyForResourceUse(String module, int resourceType) throws RemoteException {
        return this.mApi.applyForResourceUse(this.mContext, false, module, resourceType, -1, null);
    }

    public boolean unapplyForResourceUse(String module, int resourceType, String reason) throws RemoteException {
        return this.mApi.applyForResourceUse(this.mContext, false, module, resourceType, -1, reason);
    }

    public boolean notifyCallingModules(Context context, String self, List<String> callingModules) throws RemoteException {
        return this.mApi.notifyCallingModules(context.getPackageName(), self, callingModules);
    }

    public boolean registerListener(Sink sink, int stateType) throws RemoteException {
        return this.mApi.enableStateEvent(sink, stateType);
    }

    public boolean unregisterListener(Sink sink, int stateType) throws RemoteException {
        return this.mApi.disableStateEvent(sink, stateType);
    }

    public boolean isUserSleeping() throws RemoteException {
        return this.mApi.isUserSleeping(this.mContext);
    }

    public int getPowerMode() throws RemoteException {
        return this.mApi.getPowerMode(this.mContext);
    }

    public boolean registerMaintenanceTime(String module, long inactiveTime, long activeTime) throws RemoteException {
        return this.mApi.registerMaintenanceTime(this.mContext, true, module, inactiveTime, activeTime);
    }

    public boolean unRegisterMaintenanceTime(String module) throws RemoteException {
        return this.mApi.registerMaintenanceTime(this.mContext, false, module, -1, -1);
    }

    public boolean setPowerOptimizeType(int state, int appType, int optimizeType) throws RemoteException {
        boolean isSet = true;
        if (state != 1) {
            isSet = false;
        }
        return this.mApi.setPowerOptimizeType(this.mContext, isSet, appType, optimizeType);
    }

    public int getPowerOptimizeType() throws RemoteException {
        return this.mApi.getPowerOptimizeType(this.mContext);
    }

    public boolean setActiveState(int stateType, int eventType) throws RemoteException {
        return this.mApi.setActiveState(this.mContext, stateType, eventType);
    }
}
