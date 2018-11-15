package com.android.server.rms.iaware.cpu;

import android.os.IBinder;
import android.os.RemoteException;
import android.rms.iaware.AwareConfig;
import android.rms.iaware.AwareConfig.Item;
import android.rms.iaware.AwareConfig.SubItem;
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
    private static final String TAG = "CPUResourceConfigControl";
    private static CPUResourceConfigControl sInstance;
    private IBinder mCMSManager;
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
                        AwareLog.e(TAG, "InterruptedException occured");
                        continue;
                    }
                } while (retry > 0);
            }
            if (this.mCMSManager != null) {
                return IAwareCMSManager.getCustConfig(this.mCMSManager, featureName, configName);
            }
            AwareLog.i(TAG, "getAwareCustConfig can not find service awareservice.");
            return awareConfig;
        } catch (RemoteException e2) {
            AwareLog.e(TAG, "getAwareCustConfig RemoteException");
            this.mCMSManager = null;
            return awareConfig;
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

    /* JADX WARNING: Missing block: B:7:0x0008, code:
            r0 = getAwareConfig(FEATURENAME, CONFIG_CONTROL_GROUP);
     */
    /* JADX WARNING: Missing block: B:8:0x0010, code:
            if (r0 != null) goto L_0x0013;
     */
    /* JADX WARNING: Missing block: B:9:0x0012, code:
            return;
     */
    /* JADX WARNING: Missing block: B:10:0x0013, code:
            r1 = r0.getConfigList();
     */
    /* JADX WARNING: Missing block: B:11:0x0017, code:
            if (r1 != null) goto L_0x001a;
     */
    /* JADX WARNING: Missing block: B:12:0x0019, code:
            return;
     */
    /* JADX WARNING: Missing block: B:13:0x001a, code:
            r2 = new android.util.ArrayMap();
            r3 = r1.iterator();
     */
    /* JADX WARNING: Missing block: B:15:0x0028, code:
            if (r3.hasNext() == false) goto L_0x0056;
     */
    /* JADX WARNING: Missing block: B:16:0x002a, code:
            r4 = (android.rms.iaware.AwareConfig.Item) r3.next();
            r6 = r4.getProperties();
     */
    /* JADX WARNING: Missing block: B:17:0x0034, code:
            if (r6 != null) goto L_0x0037;
     */
    /* JADX WARNING: Missing block: B:20:0x0046, code:
            if (GROUP_BG_TYPE.equals((java.lang.String) r6.get("type")) == false) goto L_0x0055;
     */
    /* JADX WARNING: Missing block: B:21:0x0048, code:
            r8 = getWhiteListItem(r4);
     */
    /* JADX WARNING: Missing block: B:22:0x004c, code:
            if (r8 == null) goto L_0x0055;
     */
    /* JADX WARNING: Missing block: B:23:0x004e, code:
            r2.put(java.lang.Integer.valueOf(1), r8);
     */
    /* JADX WARNING: Missing block: B:25:0x0056, code:
            monitor-enter(r14);
     */
    /* JADX WARNING: Missing block: B:28:0x0059, code:
            if (r14.mHasReadXml == false) goto L_0x005d;
     */
    /* JADX WARNING: Missing block: B:29:0x005b, code:
            monitor-exit(r14);
     */
    /* JADX WARNING: Missing block: B:30:0x005c, code:
            return;
     */
    /* JADX WARNING: Missing block: B:31:0x005d, code:
            r3 = r2.size();
            r6 = 0;
     */
    /* JADX WARNING: Missing block: B:32:0x0063, code:
            if (r6 >= r3) goto L_0x009a;
     */
    /* JADX WARNING: Missing block: B:33:0x0065, code:
            r7 = (java.lang.String) r2.valueAt(r6);
     */
    /* JADX WARNING: Missing block: B:34:0x006f, code:
            if (r7.isEmpty() == false) goto L_0x0072;
     */
    /* JADX WARNING: Missing block: B:35:0x0072, code:
            r8 = (java.lang.Integer) r2.keyAt(r6);
            r9 = r7.split(";");
            r10 = r9.length;
            r11 = 0;
     */
    /* JADX WARNING: Missing block: B:36:0x0080, code:
            if (r11 >= r10) goto L_0x0097;
     */
    /* JADX WARNING: Missing block: B:37:0x0082, code:
            r12 = r9[r11].trim();
     */
    /* JADX WARNING: Missing block: B:38:0x008d, code:
            if (r12.isEmpty() != false) goto L_0x0094;
     */
    /* JADX WARNING: Missing block: B:39:0x008f, code:
            r14.mProcessWhiteListMap.put(r12, r8);
     */
    /* JADX WARNING: Missing block: B:40:0x0094, code:
            r11 = r11 + 1;
     */
    /* JADX WARNING: Missing block: B:41:0x0097, code:
            r6 = r6 + 1;
     */
    /* JADX WARNING: Missing block: B:42:0x009a, code:
            r14.mHasReadXml = true;
     */
    /* JADX WARNING: Missing block: B:43:0x009c, code:
            monitor-exit(r14);
     */
    /* JADX WARNING: Missing block: B:44:0x009d, code:
            return;
     */
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
