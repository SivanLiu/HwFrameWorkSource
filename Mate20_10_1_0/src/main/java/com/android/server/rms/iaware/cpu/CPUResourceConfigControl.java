package com.android.server.rms.iaware.cpu;

import android.os.IBinder;
import android.os.RemoteException;
import android.rms.iaware.AwareConfig;
import android.rms.iaware.AwareLog;
import android.rms.iaware.IAwareCMSManager;
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
    private static final Object SLOCK = new Object();
    private static final String TAG = "CPUResourceConfigControl";
    private static CPUResourceConfigControl sInstance;
    private IBinder mCMSManager;
    private boolean mHasReadXml = false;
    private final Map<String, Integer> mProcessWhiteListMap = new ArrayMap();

    private CPUResourceConfigControl() {
    }

    public static CPUResourceConfigControl getInstance() {
        CPUResourceConfigControl cPUResourceConfigControl;
        synchronized (SLOCK) {
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
        synchronized (SLOCK) {
            this.mHasReadXml = false;
            this.mProcessWhiteListMap.clear();
        }
    }

    public AwareConfig getAwareCustConfig(String featureName, String configName) {
        try {
            if (this.mCMSManager == null) {
                int retry = 5;
                do {
                    this.mCMSManager = IAwareCMSManager.getICMSManager();
                    if (this.mCMSManager != null) {
                        break;
                    }
                    retry--;
                    try {
                        Thread.sleep(200);
                        continue;
                    } catch (InterruptedException e) {
                        AwareLog.e(TAG, "getAwareCustConfig InterruptedException occured");
                        continue;
                    }
                } while (retry > 0);
            }
            if (this.mCMSManager != null) {
                return IAwareCMSManager.getCustConfig(this.mCMSManager, featureName, configName);
            }
            AwareLog.i(TAG, "getAwareCustConfig can not find service awareservice.");
            return null;
        } catch (RemoteException e2) {
            AwareLog.e(TAG, "getAwareCustConfig RemoteException");
            this.mCMSManager = null;
            return null;
        }
    }

    private AwareConfig getAwareConfig(String featureName, String configName) {
        try {
            IBinder awareservice = IAwareCMSManager.getICMSManager();
            if (awareservice != null) {
                return IAwareCMSManager.getConfig(awareservice, featureName, configName);
            }
            AwareLog.i(TAG, "getAwareConfig can not find service awareservice.");
            return null;
        } catch (RemoteException e) {
            AwareLog.e(TAG, "getAwareConfig RemoteException");
            return null;
        }
    }

    private String getWhiteListItem(AwareConfig.Item item) {
        List<AwareConfig.SubItem> subItemList = item.getSubItemList();
        if (subItemList == null) {
            return null;
        }
        for (AwareConfig.SubItem subItem : subItemList) {
            String itemName = subItem.getName();
            String itemValue = subItem.getValue();
            if (GROUP_WHITELIST.equals(itemName)) {
                return itemValue;
            }
        }
        return null;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0014, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0015, code lost:
        r2 = r1.getConfigList();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0019, code lost:
        if (r2 != null) goto L_0x001c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x001b, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x001c, code lost:
        r0 = new android.util.ArrayMap<>();
        r0 = r2.iterator();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x002b, code lost:
        if (r0.hasNext() == false) goto L_0x0059;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x002d, code lost:
        r4 = r0.next();
        r6 = r4.getProperties();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0037, code lost:
        if (r6 != null) goto L_0x003a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0049, code lost:
        if (com.android.server.rms.iaware.cpu.CPUResourceConfigControl.GROUP_BG_TYPE.equals(r6.get("type")) == false) goto L_0x0026;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x004b, code lost:
        r8 = getWhiteListItem(r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x004f, code lost:
        if (r8 == null) goto L_0x0026;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0051, code lost:
        r0.put(1, r8);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x0059, code lost:
        r4 = com.android.server.rms.iaware.cpu.CPUResourceConfigControl.SLOCK;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x005b, code lost:
        monitor-enter(r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x005e, code lost:
        if (r14.mHasReadXml == false) goto L_0x0062;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0060, code lost:
        monitor-exit(r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x0061, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x0062, code lost:
        r0 = r0.size();
        r6 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x0067, code lost:
        if (r6 >= r0) goto L_0x009e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x0069, code lost:
        r7 = r0.valueAt(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x0073, code lost:
        if (r7.isEmpty() == false) goto L_0x0076;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:36:0x0076, code lost:
        r8 = r0.keyAt(r6);
        r9 = r7.split(";");
        r10 = r9.length;
        r11 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:37:0x0084, code lost:
        if (r11 >= r10) goto L_0x009b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:0x0086, code lost:
        r13 = r9[r11].trim();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x0091, code lost:
        if (r13.isEmpty() != false) goto L_0x0098;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:40:0x0093, code lost:
        r14.mProcessWhiteListMap.put(r13, r8);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x0098, code lost:
        r11 = r11 + 1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:42:0x009b, code lost:
        r6 = r6 + 1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:43:0x009e, code lost:
        r14.mHasReadXml = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:44:0x00a0, code lost:
        monitor-exit(r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:0x00a1, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:8:0x000a, code lost:
        r1 = getAwareConfig(com.android.server.rms.iaware.cpu.CPUResourceConfigControl.FEATURENAME, com.android.server.rms.iaware.cpu.CPUResourceConfigControl.CONFIG_CONTROL_GROUP);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x0012, code lost:
        if (r1 != null) goto L_0x0015;
     */
    private void setWhiteListFromXml() {
        synchronized (SLOCK) {
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
        synchronized (SLOCK) {
            Integer groupType = this.mProcessWhiteListMap.get(processName);
            if (groupType == null) {
                return -1;
            }
            return groupType.intValue();
        }
    }
}
