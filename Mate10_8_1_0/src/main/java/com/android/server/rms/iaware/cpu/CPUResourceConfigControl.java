package com.android.server.rms.iaware.cpu;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.rms.iaware.AwareConfig;
import android.rms.iaware.AwareConfig.Item;
import android.rms.iaware.AwareConfig.SubItem;
import android.rms.iaware.AwareLog;
import android.rms.iaware.ICMSManager;
import android.rms.iaware.ICMSManager.Stub;
import android.util.ArrayMap;
import java.util.List;
import java.util.Map;

public class CPUResourceConfigControl {
    private static final String CONFIG_CONTROL_GROUP = "control_group";
    private static final String CONFIG_GROUP_TYPE = "type";
    private static final String FEATURENAME = "CPU";
    private static final int GET_CMS_RETRY_TIME = 5;
    private static final int GET_CMS_SLEEP_TIME = 200;
    private static final String GROUP_BG_TYPE = "group_bg";
    public static final int GROUP_BG_VALUE = 1;
    private static final String GROUP_WHITELIST = "whitelist";
    private static final String SEPARATOR = ";";
    private static final String TAG = "CPUResourceConfigControl";
    private static CPUResourceConfigControl sInstance;
    private ICMSManager mCMSManager;
    private boolean mHasReadXml = false;
    private final Map<String, Integer> mProcessWhiteListMap = new ArrayMap();

    private CPUResourceConfigControl() {
    }

    public static synchronized CPUResourceConfigControl getInstance() {
        CPUResourceConfigControl cPUResourceConfigControl;
        synchronized (CPUResourceConfigControl.class) {
            if (sInstance == null) {
                sInstance = new CPUResourceConfigControl();
            }
            cPUResourceConfigControl = sInstance;
        }
        return cPUResourceConfigControl;
    }

    private void initialize() {
        setWhiteListFromXml();
    }

    private void deInitialize() {
        synchronized (this) {
            this.mHasReadXml = false;
            this.mProcessWhiteListMap.clear();
        }
    }

    public AwareConfig getAwareCustConfig(String featureName, String configName) {
        AwareConfig awareConfig = null;
        try {
            if (this.mCMSManager == null) {
                int retry = 5;
                while (true) {
                    this.mCMSManager = Stub.asInterface(ServiceManager.getService("IAwareCMSService"));
                    if (this.mCMSManager == null) {
                        retry--;
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            AwareLog.e(TAG, "InterruptedException occured");
                        }
                        if (retry <= 0) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
            if (this.mCMSManager != null) {
                awareConfig = this.mCMSManager.getCustConfig(featureName, configName);
            } else {
                AwareLog.i(TAG, "getAwareCustConfig can not find service awareservice.");
            }
        } catch (RemoteException e2) {
            AwareLog.e(TAG, "getAwareCustConfig RemoteException");
            this.mCMSManager = null;
        }
        return awareConfig;
    }

    private AwareConfig getAwareConfig(String featureName, String configName) {
        try {
            ICMSManager awareservice = Stub.asInterface(ServiceManager.getService("IAwareCMSService"));
            if (awareservice != null) {
                return awareservice.getConfig(featureName, configName);
            }
            AwareLog.i(TAG, "getAwareConfig can not find service awareservice.");
            return null;
        } catch (RemoteException e) {
            AwareLog.e(TAG, "getAwareConfig RemoteException");
            return null;
        }
    }

    private String getWhiteListItem(Item item) {
        String whiteList = null;
        List<SubItem> subItemList = item.getSubItemList();
        if (subItemList == null) {
            return null;
        }
        for (SubItem subItem : subItemList) {
            String itemName = subItem.getName();
            String itemValue = subItem.getValue();
            if (GROUP_WHITELIST.equals(itemName)) {
                whiteList = itemValue;
                break;
            }
        }
        return whiteList;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setWhiteListFromXml() {
        synchronized (this) {
            if (this.mHasReadXml) {
            }
        }
    }

    public void enable() {
        initialize();
    }

    public void disable() {
        deInitialize();
    }

    public int isWhiteList(String processName) {
        if (processName == null) {
            return -1;
        }
        synchronized (this) {
            Integer groupType = (Integer) this.mProcessWhiteListMap.get(processName);
            if (groupType != null) {
                int intValue = groupType.intValue();
                return intValue;
            }
            return -1;
        }
    }
}
