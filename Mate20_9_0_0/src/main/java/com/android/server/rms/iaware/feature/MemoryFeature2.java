package com.android.server.rms.iaware.feature;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.rms.iaware.AwareConfig;
import android.rms.iaware.AwareConfig.Item;
import android.rms.iaware.AwareConfig.SubItem;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.AwareConstant.FeatureType;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import android.rms.iaware.IAwareCMSManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import com.android.internal.util.MemInfoReader;
import com.android.server.am.HwActivityManagerService;
import com.android.server.mtm.MultiTaskManagerService;
import com.android.server.rms.iaware.IRDataRegister;
import com.android.server.rms.iaware.hiber.constant.AppHibernateCst;
import com.android.server.rms.iaware.memory.policy.SystemTrimPolicy;
import com.android.server.rms.iaware.memory.utils.BigDataStore;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.rms.iaware.memory.utils.MemoryLockFile;
import com.android.server.rms.iaware.memory.utils.MemoryReader;
import com.android.server.rms.iaware.memory.utils.MemoryUtils;
import com.android.server.rms.iaware.memory.utils.PrereadUtils;
import com.android.server.rms.memrepair.MemRepairPolicy;
import com.android.server.rms.memrepair.ProcStateStatisData;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MemoryFeature2 extends RFeature {
    private static final int BASE_VERSION = 2;
    private static final int MAX_CACHED_APPS = SystemProperties.getInt("ro.sys.fw.bg_apps_limit", 32);
    private static final int MAX_CACHED_APP_SIZE = 512;
    private static final String MEMORY_BG_PROCS = "memory_bg_procs";
    private static final String MEMORY_COLD_WARM_LAUNCH = "memory_cold_warm_launch";
    private static final String MEMORY_MMONITOR = "memory_mmonitor";
    private static final int MIN_CACHED_APP_SIZE = 4;
    private static final String TAG = "AwareMem_MemFeature2.0";
    public static final AtomicBoolean isUpMemoryFeature = new AtomicBoolean(false);
    private int mEmptyProcessPercent;
    private HwActivityManagerService mHwAMS;
    private MemoryLockFile mLockFile;
    private int mNumProcessLimit;
    private PrereadUtils mPrereadUtils;
    private long timeStamp;

    public MemoryFeature2(Context context, FeatureType featureType, IRDataRegister dataRegister) {
        super(context, featureType, dataRegister);
        this.mHwAMS = null;
        this.mEmptyProcessPercent = SystemProperties.getInt("ro.sys.fw.empty_app_percent", 50);
        this.mLockFile = new MemoryLockFile();
        this.timeStamp = 0;
        this.mHwAMS = HwActivityManagerService.self();
        this.mNumProcessLimit = MAX_CACHED_APPS;
        this.mPrereadUtils = PrereadUtils.getInstance();
        PrereadUtils.setContext(context);
        loadMemConfig();
    }

    public boolean enable() {
        return false;
    }

    public boolean enableFeatureEx(int realVersion) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("enableFeatureEx realVersion=");
        stringBuilder.append(realVersion);
        AwareLog.d(str, stringBuilder.toString());
        if (realVersion < 2) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("enableFeatureEx failed, realVersion: ");
            stringBuilder2.append(realVersion);
            stringBuilder2.append(", baseVersion: ");
            stringBuilder2.append(2);
            AwareLog.i(str2, stringBuilder2.toString());
            return false;
        }
        isUpMemoryFeature.set(true);
        if (AwareConstant.CURRENT_USER_TYPE == 3) {
            MemoryUtils.writeMMonitorSwitch(1);
        }
        this.mLockFile.iAwareAddPinFile();
        readMemoryAPIWhiteListUid();
        PrereadUtils prereadUtils = this.mPrereadUtils;
        PrereadUtils.start();
        ProcStateStatisData.getInstance().setEnable(true);
        setEmptyProcessPercent(this.mEmptyProcessPercent);
        setProcessLimit(this.mNumProcessLimit);
        SystemTrimPolicy.getInstance().enable();
        return true;
    }

    public boolean disable() {
        isUpMemoryFeature.set(false);
        if (AwareConstant.CURRENT_USER_TYPE == 3) {
            MemoryUtils.writeMMonitorSwitch(0);
        }
        this.mLockFile.clearPinFile();
        MemoryUtils.destroySocket();
        PrereadUtils prereadUtils = this.mPrereadUtils;
        PrereadUtils.stop();
        ProcStateStatisData.getInstance().setEnable(false);
        setEmptyProcessPercent(-1);
        setProcessLimit(MAX_CACHED_APPS);
        SystemTrimPolicy.getInstance().disable();
        return true;
    }

    public boolean reportData(CollectData data) {
        return false;
    }

    public boolean configUpdate() {
        loadMemConfig();
        setProcessLimit(this.mNumProcessLimit);
        this.mLockFile.clearPinFile();
        this.mLockFile.iAwareAddPinFile();
        return true;
    }

    public String getBigDataByVersion(int iawareVer, boolean forBeta, boolean clearData) {
        return null;
    }

    public String getDFTDataByVersion(int iawareVer, boolean forBeta, boolean clearData, boolean betaEncode) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Memoryfeature2 getDFTDataByVersion. iawareVer: ");
        stringBuilder.append(iawareVer);
        stringBuilder.append(", beta: ");
        stringBuilder.append(forBeta);
        stringBuilder.append(", clear: ");
        stringBuilder.append(clearData);
        stringBuilder.append(";betaEncode=");
        stringBuilder.append(betaEncode);
        AwareLog.i(str, stringBuilder.toString());
        if (!betaEncode || !isUpMemoryFeature.get() || iawareVer < 2 || AwareConstant.CURRENT_USER_TYPE != 3) {
            return null;
        }
        long timeMillis = System.currentTimeMillis();
        StringBuilder builder = new StringBuilder();
        String result = MemoryReader.getMmonitorData();
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("result=");
        stringBuilder2.append(result);
        AwareLog.d(str2, stringBuilder2.toString());
        if (result != null) {
            builder.append("{");
            builder.append(createHeadMsg(MEMORY_MMONITOR, timeMillis));
            builder.append("\"data\":");
            builder.append(result);
            builder.append("}\n");
        }
        result = BigDataStore.getInstance().getColdWarmLaunchData(clearData);
        if (result != null) {
            builder.append("{");
            builder.append(createHeadMsg(MEMORY_COLD_WARM_LAUNCH, timeMillis));
            builder.append(result);
            builder.append("}\n");
        }
        builder.append("{");
        builder.append(createHeadMsg(MEMORY_BG_PROCS, timeMillis));
        builder.append(BigDataStore.getInstance().getBgAppData(clearData));
        builder.append("}");
        str2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Memoryfeature2 getBigDataByVersion. result: ");
        stringBuilder2.append(builder.toString());
        AwareLog.d(str2, stringBuilder2.toString());
        this.timeStamp = timeMillis;
        return builder.toString();
    }

    private String createHeadMsg(String featureName, long endTime) {
        StringBuilder sb = new StringBuilder();
        sb.append("\"feature\":\"");
        sb.append(featureName);
        sb.append("\",");
        sb.append("\"start\":");
        sb.append(this.timeStamp > 0 ? this.timeStamp : endTime);
        sb.append(",");
        sb.append("\"end\":");
        sb.append(endTime);
        sb.append(",");
        return sb.toString();
    }

    private void loadMemConfig() {
        loadMemConstantConfig(false);
        loadMemConstantConfig(true);
        loadPrereadConfig();
        loadMemRepairConfig();
        MemoryConstant.clearPinnedFilesStr();
        loadPinFileConfig();
        MemoryFeature.loadBigMemAppPolicyConfig(getConfig(MemoryConstant.MEM_POLICY_FEATURENAME, MemoryConstant.MEM_POLICY_BIGMEMAPP, true));
        MemoryFeature.loadBigMemAppPolicyConfig(getConfig(MemoryConstant.MEM_POLICY_FEATURENAME, MemoryConstant.MEM_POLICY_IONPROPERTYS, true));
        loadSysTrimConfig();
    }

    private void loadMemConstantConfig(boolean isCustConfig) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("loadMemConstantConfig, isCustConfig : ");
        stringBuilder.append(isCustConfig);
        AwareLog.i(str, stringBuilder.toString());
        AwareConfig configList = getConfig(MemoryConstant.MEM_POLICY_FEATURENAME, MemoryConstant.MEM_CONSTANT_CONFIGNAME, isCustConfig);
        if (configList == null) {
            AwareLog.w(TAG, "loadMemConstantConfig failure cause null configList");
            return;
        }
        MemInfoReader minfo = new MemInfoReader();
        minfo.readMemInfo();
        Long totalMemMb = Long.valueOf(minfo.getTotalSize() / MemoryConstant.MB_SIZE);
        for (Item item : configList.getConfigList()) {
            if (item != null && item.getProperties() != null) {
                String ramSize = (String) item.getProperties().get(MemoryConstant.MEM_CONSTANT_RAMSIZENAME);
                if (MemoryUtils.checkRamSize(ramSize, totalMemMb)) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("loadMemConstantConfig success. ramSize: ");
                    stringBuilder2.append(ramSize);
                    stringBuilder2.append(" totalMemMb: ");
                    stringBuilder2.append(totalMemMb);
                    AwareLog.i(str2, stringBuilder2.toString());
                    saveMemConstantItem(item);
                    break;
                }
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("checkRamSize failure cause ramSize: ");
                stringBuilder3.append(ramSize);
                stringBuilder3.append(" totalMemMb: ");
                stringBuilder3.append(totalMemMb);
                AwareLog.d(str3, stringBuilder3.toString());
            } else {
                AwareLog.d(TAG, "loadMemConstantConfig continue cause null item");
            }
        }
    }

    public void loadPrereadConfig() {
        AwareConfig configList = getConfig(MemoryConstant.MEM_POLICY_FEATURENAME, MemoryConstant.MEM_PREREAD_CONFIGNAME, true);
        if (configList == null) {
            AwareLog.w(TAG, "loadPrereadConfig failure, configList is empty");
            return;
        }
        Map<String, ArrayList<String>> filePathMap = new ArrayMap();
        for (Item item : configList.getConfigList()) {
            if (item == null) {
                AwareLog.w(TAG, "loadPrereadConfig failure, item is empty");
            } else {
                Map<String, String> configPropertries = item.getProperties();
                if (configPropertries == null) {
                    AwareLog.w(TAG, "loadPrereadConfig failure, configPropertries is empty");
                } else {
                    try {
                        if (Integer.parseInt((String) configPropertries.get(MemoryConstant.MEM_PREREAD_SWITCH)) == 0) {
                            AwareLog.w(TAG, "prereadSwitch off");
                        } else {
                            String pkgName = (String) configPropertries.get(MemoryConstant.MEM_PREREAD_ITEM_NAME);
                            if (pkgName != null) {
                                ArrayList<String> filePath = new ArrayList();
                                List<SubItem> subItemList = item.getSubItemList();
                                if (subItemList != null) {
                                    for (SubItem subItem : subItemList) {
                                        String itemName = subItem.getName();
                                        String itemValue = subItem.getValue();
                                        if ("file".equals(itemName)) {
                                            filePath.add(itemValue);
                                        }
                                    }
                                    filePathMap.put(pkgName, filePath);
                                }
                            }
                        }
                    } catch (NumberFormatException e) {
                        AwareLog.e(TAG, "Number Format Error !");
                    }
                }
            }
        }
        MemoryConstant.setPrereadFileMap(filePathMap);
        this.mPrereadUtils.sendPrereadDataUpdateMsg();
    }

    protected AwareConfig getConfig(String featureName, String configName, boolean isCustConfig) {
        AwareConfig configList = null;
        if (featureName == null || featureName.equals("") || configName == null || configName.equals("")) {
            AwareLog.i(TAG, "featureName or configName is null");
            return null;
        }
        try {
            IBinder awareservice = IAwareCMSManager.getICMSManager();
            if (awareservice != null) {
                configList = isCustConfig ? IAwareCMSManager.getCustConfig(awareservice, featureName, configName) : IAwareCMSManager.getConfig(awareservice, featureName, configName);
            } else {
                AwareLog.i(TAG, "can not find service awareservice.");
            }
        } catch (RemoteException e) {
            AwareLog.e(TAG, "MemoryFeature getConfig RemoteException");
        }
        return configList;
    }

    private void saveMemConstantItem(Item item) {
        for (SubItem subItem : item.getSubItemList()) {
            String itemName = subItem.getName();
            String itemValue = subItem.getValue();
            if (itemName != null) {
                if (itemValue != null) {
                    Object obj = -1;
                    try {
                        switch (itemName.hashCode()) {
                            case -1669721547:
                                if (itemName.equals(MemoryConstant.MEM_CONSTANT_PREREAD_ODEX)) {
                                    obj = 5;
                                    break;
                                }
                                break;
                            case -1470603166:
                                if (itemName.equals(MemoryConstant.MEM_CONSTANT_SYSTEMTRIMWITCH)) {
                                    obj = null;
                                    break;
                                }
                                break;
                            case -822986700:
                                if (itemName.equals(MemoryConstant.MEM_CONSTANT_API_MAX_REQUEST_MEM)) {
                                    obj = 6;
                                    break;
                                }
                                break;
                            case -798160691:
                                if (itemName.equals(MemoryConstant.MEM_CONSTANT_GPUNAME)) {
                                    obj = 2;
                                    break;
                                }
                                break;
                            case -533820387:
                                if (itemName.equals(MemoryConstant.MEM_CONSTANT_PROCESSPERCENT)) {
                                    obj = 4;
                                    break;
                                }
                                break;
                            case -470167246:
                                if (itemName.equals(MemoryConstant.MEM_CONSTANT_PROCESSLIMIT)) {
                                    obj = 3;
                                    break;
                                }
                                break;
                            case 53466486:
                                if (itemName.equals(MemoryConstant.MEM_CONSTANT_GMCSWITCH)) {
                                    obj = 1;
                                    break;
                                }
                                break;
                            default:
                                break;
                        }
                        int systemTrimSwitch;
                        String str;
                        StringBuilder stringBuilder;
                        long gpuMemoryLimit;
                        String str2;
                        StringBuilder stringBuilder2;
                        switch (obj) {
                            case null:
                                systemTrimSwitch = Integer.parseInt(itemValue.trim());
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("loadMemConfig system app ontrim Switch: ");
                                stringBuilder.append(systemTrimSwitch);
                                AwareLog.i(str, stringBuilder.toString());
                                MemoryConstant.setConfigSystemTrimSwitch(systemTrimSwitch);
                                break;
                            case 1:
                                systemTrimSwitch = Integer.parseInt(itemValue.trim());
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("loadMemConfig gmc Switch: ");
                                stringBuilder.append(systemTrimSwitch);
                                AwareLog.i(str, stringBuilder.toString());
                                MemoryConstant.setConfigGmcSwitch(systemTrimSwitch);
                                break;
                            case 2:
                                gpuMemoryLimit = Long.parseLong(itemValue.trim());
                                str2 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("loadMemConfig gpuMemoryLimit: ");
                                stringBuilder2.append(gpuMemoryLimit * 1024);
                                AwareLog.i(str2, stringBuilder2.toString());
                                MemoryConstant.setGpuMemoryLimit(1024 * gpuMemoryLimit);
                                break;
                            case 3:
                                systemTrimSwitch = Integer.parseInt(itemValue.trim());
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("loadMemConfig numProcessLimit: ");
                                stringBuilder.append(systemTrimSwitch);
                                AwareLog.i(str, stringBuilder.toString());
                                this.mNumProcessLimit = SystemProperties.getInt("ro.sys.fw.bg_apps_limit", systemTrimSwitch);
                                break;
                            case 4:
                                systemTrimSwitch = Integer.parseInt(itemValue.trim());
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("loadMemConfig numProcessPercent: ");
                                stringBuilder.append(systemTrimSwitch);
                                AwareLog.i(str, stringBuilder.toString());
                                if (systemTrimSwitch >= 0 && systemTrimSwitch <= 100) {
                                    this.mEmptyProcessPercent = SystemProperties.getInt("ro.sys.fw.empty_app_percent", systemTrimSwitch);
                                    break;
                                }
                            case 5:
                                systemTrimSwitch = Integer.parseInt(itemValue.trim());
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("loadMemConfig preread_odex_switch: ");
                                stringBuilder.append(systemTrimSwitch);
                                AwareLog.i(str, stringBuilder.toString());
                                PrereadUtils.getInstance();
                                PrereadUtils.setPrereadOdexSwitch(systemTrimSwitch);
                                break;
                            case 6:
                                gpuMemoryLimit = Long.parseLong(itemValue.trim());
                                str2 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("loadMemConfig max api request memory: ");
                                stringBuilder2.append(gpuMemoryLimit);
                                AwareLog.i(str2, stringBuilder2.toString());
                                MemoryConstant.setMaxAPIRequestMemory(gpuMemoryLimit);
                                break;
                            default:
                                String str3 = TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("loadMemConfig no such configuration. ");
                                stringBuilder3.append(itemName);
                                AwareLog.w(str3, stringBuilder3.toString());
                                break;
                        }
                    } catch (NumberFormatException e) {
                        AwareLog.e(TAG, "parse memory xml error");
                    }
                }
            }
        }
    }

    private void setProcessLimit(int max) {
        if (max >= 4 && max <= 512 && this.mHwAMS != null) {
            this.mHwAMS.setProcessLimit(max);
        }
    }

    private void setEmptyProcessPercent(int percent) {
        try {
            SystemProperties.set("sys.iaware.empty_app_percent", Integer.toString(percent));
        } catch (IllegalArgumentException e) {
            AwareLog.i(TAG, "setEmptyProcessPercent IllegalArgumentException! ");
        }
    }

    private void loadPinFilesItem(Item item, int mrSize) {
        if (mrSize > 20) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("loadPinFilesItem too long mrSize=");
            stringBuilder.append(mrSize);
            AwareLog.i(str, stringBuilder.toString());
            return;
        }
        int curIndex = 0;
        for (SubItem subItem : item.getSubItemList()) {
            if (curIndex >= mrSize) {
                curIndex = mrSize + 1;
                break;
            } else if (subItem != null) {
                String itemName = subItem.getName();
                String itemValue = subItem.getValue();
                if (TextUtils.isEmpty(itemName) || TextUtils.isEmpty(itemValue)) {
                    AwareLog.i(TAG, "loadPinFilesItem null item");
                    break;
                }
                Object obj = -1;
                if (itemName.hashCode() == 3143036 && itemName.equals("file")) {
                    obj = null;
                }
                if (obj != null) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("loadPinFilesItem no such configuration:");
                    stringBuilder2.append(itemName);
                    AwareLog.w(str2, stringBuilder2.toString());
                    curIndex = mrSize + 1;
                } else {
                    MemoryConstant.addPinnedFilesStr(itemValue.trim());
                    curIndex++;
                }
            }
        }
    }

    private void loadSysTrimConfig() {
        AwareLog.d(TAG, "loadSysTrimConfig begin");
        AwareConfig configList = getConfig(MemoryConstant.MEM_POLICY_FEATURENAME, MemoryConstant.MEM_POLICY_SYSTRIM, false);
        if (configList == null) {
            AwareLog.w(TAG, "loadSysTrimConfig failure cause null configList");
            return;
        }
        List<Item> itemList = configList.getConfigList();
        if (itemList == null) {
            AwareLog.w(TAG, "loadSysTrimConfig failure cause null itemList");
            return;
        }
        for (Item item : itemList) {
            if (item == null) {
                AwareLog.w(TAG, "loadSysTrimConfig continue cause null item");
            } else {
                List<SubItem> subItems = item.getSubItemList();
                if (subItems == null) {
                    AwareLog.w(TAG, "loadSysTrimConfig continue cause null subitem");
                } else {
                    for (SubItem subItem : subItems) {
                        Map<String, String> properties = subItem.getProperties();
                        if (properties != null) {
                            String packageName = (String) properties.get("packageName");
                            String threshold = (String) properties.get("threshold");
                            if (!TextUtils.isEmpty(packageName)) {
                                if (!TextUtils.isEmpty(threshold)) {
                                    try {
                                        long thres = Long.parseLong(threshold) * 1024;
                                        if (thres <= 0) {
                                            AwareLog.w(TAG, "loadSysTrimConfig continue cause, the threshhold is less than 0");
                                        } else {
                                            SystemTrimPolicy.getInstance().updateProcThreshold(packageName, thres);
                                        }
                                    } catch (NumberFormatException e) {
                                        AwareLog.w(TAG, "loadSysTrimConfig continue cause subitem threshhold is not long");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        AwareLog.d(TAG, "loadSysTrimConfig end");
    }

    private void loadPinFileConfig() {
        AwareLog.d(TAG, "loadPinFileConfig begin");
        AwareConfig configList = getConfig(MemoryConstant.MEM_POLICY_FEATURENAME, MemoryConstant.MEM_PIN_FILE, false);
        if (configList == null) {
            AwareLog.w(TAG, "loadPinFileConfig failure cause null configList");
            return;
        }
        List<Item> itemList = configList.getConfigList();
        if (itemList == null) {
            AwareLog.w(TAG, "loadPinFileConfig failure cause null itemList");
            return;
        }
        MemInfoReader minfo = new MemInfoReader();
        minfo.readMemInfo();
        Long totalMemMb = Long.valueOf(minfo.getTotalSize() / MemoryConstant.MB_SIZE);
        for (Item item : itemList) {
            if (item == null || item.getProperties() == null) {
                AwareLog.w(TAG, "loadPinFileConfig continue cause null item");
            } else {
                Map<String, String> configPropertries = item.getProperties();
                String strName = (String) configPropertries.get("name");
                if (TextUtils.isEmpty(strName)) {
                    AwareLog.w(TAG, "loadPinFileConfig failure null item name");
                } else if (MemoryUtils.checkRamSize((String) configPropertries.get(MemoryConstant.MEM_CONSTANT_RAMSIZENAME), totalMemMb)) {
                    int size = getSize((String) configPropertries.get("size"));
                    if (size < 1) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("loadPinFileConfig continue failure size: ");
                        stringBuilder.append(size);
                        AwareLog.w(str, stringBuilder.toString());
                    } else if (item.getSubItemList() == null) {
                        AwareLog.w(TAG, "loadPinFileConfig continue cause null subitem");
                    } else if (strName.equals(MemoryConstant.MEM_PIN_FILE_NAME)) {
                        loadPinFilesItem(item, size);
                    }
                } else {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("checkRamSize failure ramSize, totalMemMb: ");
                    stringBuilder2.append(totalMemMb);
                    AwareLog.d(str2, stringBuilder2.toString());
                }
            }
        }
        AwareLog.d(TAG, "loadPinFileConfig end");
    }

    private void loadMemRepairConfig() {
        AwareLog.d(TAG, "loadMemRepairConfig begin");
        AwareConfig configList = getConfig(MemoryConstant.MEM_POLICY_FEATURENAME, MemoryConstant.MEM_POLICY_REPAIR, false);
        if (configList == null) {
            AwareLog.w(TAG, "loadMemRepairConfig failure cause null configList");
            return;
        }
        List<Item> itemList = configList.getConfigList();
        if (itemList == null) {
            AwareLog.w(TAG, "loadMemRepairConfig failure cause null itemList");
            return;
        }
        MemInfoReader minfo = new MemInfoReader();
        minfo.readMemInfo();
        Long totalMemMb = Long.valueOf(minfo.getTotalSize() / MemoryConstant.MB_SIZE);
        for (Item item : itemList) {
            if (item == null || item.getProperties() == null) {
                AwareLog.w(TAG, "loadMemRepairConfig continue cause null item");
            } else {
                Map<String, String> configPropertries = item.getProperties();
                String strName = (String) configPropertries.get("name");
                if (TextUtils.isEmpty(strName)) {
                    AwareLog.w(TAG, "loadMemRepairConfig failure null item name");
                } else {
                    int size = getSize((String) configPropertries.get("size"));
                    if (size < 1) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("loadMemRepairConfig continue failure size: ");
                        stringBuilder.append(size);
                        AwareLog.w(str, stringBuilder.toString());
                    } else if (item.getSubItemList() == null) {
                        AwareLog.w(TAG, "loadMemRepairConfig continue cause null subitem");
                    } else if (strName.equals(MemoryConstant.MEM_REPAIR_CONSTANT_BASE)) {
                        loadMemRepairConstantItem(item, size);
                    } else if (!MemoryUtils.checkRamSize((String) configPropertries.get(MemoryConstant.MEM_CONSTANT_RAMSIZENAME), totalMemMb)) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("checkRamSize failure ramSize, totalMemMb: ");
                        stringBuilder2.append(totalMemMb);
                        AwareLog.d(str2, stringBuilder2.toString());
                    } else if (strName.equals(MemoryConstant.MEM_REPAIR_CONSTANT_MIN_MAX_THRES)) {
                        loadMemRepairMinMaxThresItem(item, size);
                    } else if (strName.equals(MemoryConstant.MEM_REPAIR_CONSTANT_PROC_EMERG_THRES)) {
                        loadMemRepairProcThresItem(totalMemMb.longValue(), item, size);
                    }
                }
            }
        }
        AwareLog.d(TAG, "loadMemRepairConfig end");
    }

    /* JADX WARNING: Removed duplicated region for block: B:41:0x00c1 A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:41:0x00c1 A:{SKIP} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void loadMemRepairConstantItem(Item item, int mrSize) {
        int index;
        int i = mrSize;
        int index2 = 0;
        int[] constValues = new int[]{0, 0, 0, 0, 0};
        boolean loadSucc = true;
        for (SubItem subItem : item.getSubItemList()) {
            if (index2 >= i) {
                index2 = i + 1;
                if (index2 != i && loadSucc) {
                    ProcStateStatisData.getInstance().updateConfig(constValues[0], constValues[1], ((long) constValues[2]) * AppHibernateCst.DELAY_ONE_MINS, ((long) constValues[3]) * AppHibernateCst.DELAY_ONE_MINS);
                    MemRepairPolicy.getInstance().updateCollectCount(constValues[0], constValues[1]);
                    MemRepairPolicy.getInstance().updateDValueFloatPercent(constValues[4]);
                    return;
                }
            } else if (subItem != null) {
                String itemName = subItem.getName();
                String itemValue = subItem.getValue();
                if (!TextUtils.isEmpty(itemName)) {
                    if (!TextUtils.isEmpty(itemValue)) {
                        Object obj = -1;
                        switch (itemName.hashCode()) {
                            case -1602877080:
                                if (itemName.equals(MemoryConstant.MEM_REPAIR_CONSTANT_BG_MIN_COUNT)) {
                                    obj = 1;
                                    break;
                                }
                                break;
                            case -1575767891:
                                if (itemName.equals(MemoryConstant.MEM_REPAIR_CONSTANT_DV_NEGA_PERCENT)) {
                                    obj = 4;
                                    break;
                                }
                                break;
                            case -1086546204:
                                if (itemName.equals(MemoryConstant.MEM_REPAIR_CONSTANT_FG_MIN_COUNT)) {
                                    obj = null;
                                    break;
                                }
                                break;
                            case -1005530493:
                                if (itemName.equals(MemoryConstant.MEM_REPAIR_CONSTANT_FG_INTERVAL)) {
                                    obj = 2;
                                    break;
                                }
                                break;
                            case 1887307647:
                                if (itemName.equals(MemoryConstant.MEM_REPAIR_CONSTANT_BG_INTERVAL)) {
                                    obj = 3;
                                    break;
                                }
                                break;
                        }
                        switch (obj) {
                            case null:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                                loadSucc = saveMemRepairConstantItem(itemName, itemValue.trim(), constValues);
                                index2 = loadSucc ? index2 + 1 : i + 1;
                                break;
                            default:
                                String str = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("loadMemRepairConstantItem no such configuration:");
                                stringBuilder.append(itemName);
                                AwareLog.w(str, stringBuilder.toString());
                                index2 = i + 1;
                                break;
                        }
                    }
                    index = index2;
                    index2 = index;
                    if (index2 != i) {
                    }
                }
                index = index2;
                index2 = index;
                if (index2 != i) {
                }
            }
        }
        index = index2;
        index2 = index;
        if (index2 != i) {
        }
    }

    /* JADX WARNING: Missing block: B:13:0x0032, code skipped:
            if (r10.equals(com.android.server.rms.iaware.memory.utils.MemoryConstant.MEM_REPAIR_CONSTANT_FG_MIN_COUNT) != false) goto L_0x004a;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean saveMemRepairConstantItem(String itemName, String itemValue, int[] constValues) {
        boolean loadSucc = false;
        if (constValues == null || constValues.length != 5) {
            return false;
        }
        switch (itemName.hashCode()) {
            case -1602877080:
                if (itemName.equals(MemoryConstant.MEM_REPAIR_CONSTANT_BG_MIN_COUNT)) {
                    loadSucc = true;
                    break;
                }
            case -1575767891:
                if (itemName.equals(MemoryConstant.MEM_REPAIR_CONSTANT_DV_NEGA_PERCENT)) {
                    loadSucc = true;
                    break;
                }
            case -1086546204:
                break;
            case -1005530493:
                if (itemName.equals(MemoryConstant.MEM_REPAIR_CONSTANT_FG_INTERVAL)) {
                    loadSucc = true;
                    break;
                }
            case 1887307647:
                if (itemName.equals(MemoryConstant.MEM_REPAIR_CONSTANT_BG_INTERVAL)) {
                    loadSucc = true;
                    break;
                }
            default:
                loadSucc = true;
                break;
        }
        String str;
        StringBuilder stringBuilder;
        switch (loadSucc) {
            case false:
                loadSucc = parseConstMinCount(itemValue.trim(), constValues, 0);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("saveMemRepairConstantItem minCount:");
                stringBuilder.append(constValues[0]);
                AwareLog.i(str, stringBuilder.toString());
                break;
            case true:
                loadSucc = parseConstMinCount(itemValue.trim(), constValues, 1);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("saveMemRepairConstantItem minCount:");
                stringBuilder.append(constValues[1]);
                AwareLog.i(str, stringBuilder.toString());
                break;
            case true:
                loadSucc = parseConstInterval(itemValue.trim(), constValues, 2);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("saveMemRepairConstantItem fg interval:");
                stringBuilder.append(constValues[2]);
                AwareLog.i(str, stringBuilder.toString());
                break;
            case true:
                loadSucc = parseConstInterval(itemValue.trim(), constValues, 3);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("saveMemRepairConstantItem bg interval:");
                stringBuilder.append(constValues[3]);
                AwareLog.i(str, stringBuilder.toString());
                break;
            case true:
                loadSucc = parseConstDValuePercent(itemValue.trim(), constValues, 4);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("saveMemRepairConstantItem percent:");
                stringBuilder.append(constValues[4]);
                AwareLog.i(str, stringBuilder.toString());
                break;
            default:
                loadSucc = false;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("saveMemRepairConstantItem no such configuration:");
                stringBuilder.append(itemName);
                AwareLog.w(str, stringBuilder.toString());
                break;
        }
        return loadSucc;
    }

    private boolean parseConstMinCount(String itemValue, int[] constValues, int index) {
        try {
            int minCount = Integer.parseInt(itemValue.trim());
            if (minCount >= 6) {
                if (minCount <= 50) {
                    constValues[index] = minCount;
                    return true;
                }
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("error minCount:");
            stringBuilder.append(minCount);
            AwareLog.i(str, stringBuilder.toString());
            return false;
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "parse memory xml error");
            return false;
        }
    }

    private boolean parseConstInterval(String itemValue, int[] constValues, int index) {
        try {
            int interval = Integer.parseInt(itemValue.trim());
            if (interval >= 2) {
                if (interval <= 30) {
                    constValues[index] = interval;
                    return true;
                }
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("error interval:");
            stringBuilder.append(interval);
            AwareLog.i(str, stringBuilder.toString());
            return false;
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "parse memory xml error");
            return false;
        }
    }

    private boolean parseConstDValuePercent(String itemValue, int[] constValues, int index) {
        try {
            int percent = Integer.parseInt(itemValue.trim());
            if (percent >= 1) {
                if (percent <= 30) {
                    constValues[index] = percent;
                    return true;
                }
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("error percent:");
            stringBuilder.append(percent);
            AwareLog.i(str, stringBuilder.toString());
            return false;
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "parse memory xml error");
            return false;
        }
    }

    private void loadMemRepairMinMaxThresItem(Item item, int mrSize) {
        if (mrSize > 20) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("loadMemRepairMinMaxThresItem too long mrSize=");
            stringBuilder.append(mrSize);
            AwareLog.i(str, stringBuilder.toString());
            return;
        }
        long[][] memThresHolds = (long[][]) Array.newInstance(long.class, new int[]{mrSize, 3});
        int index = 0;
        for (SubItem subItem : item.getSubItemList()) {
            if (index >= mrSize) {
                index = mrSize + 1;
                break;
            } else if (subItem != null) {
                String itemName = subItem.getName();
                String itemValue = subItem.getValue();
                if (TextUtils.isEmpty(itemName) || TextUtils.isEmpty(itemValue)) {
                    AwareLog.i(TAG, "loadMemRepairMinMaxThresItem null item");
                    break;
                }
                Object obj = -1;
                if (itemName.hashCode() == -1545477013 && itemName.equals("threshold")) {
                    obj = null;
                }
                if (obj != null) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("loadMemRepairMinMaxThresItem no such configuration:");
                    stringBuilder2.append(itemName);
                    AwareLog.w(str2, stringBuilder2.toString());
                    index = mrSize + 1;
                } else if (parseMinMaxThres(itemValue.trim(), memThresHolds[index])) {
                    index++;
                } else {
                    index = mrSize + 1;
                }
            }
        }
        if (index == mrSize) {
            MemRepairPolicy.getInstance().updateFloatThresHold(memThresHolds);
        }
    }

    private boolean parseMinMaxThres(String itemValue, long[] memThresHolds) {
        try {
            try {
                String[] sets = itemValue.split(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                if (sets.length != 3) {
                    AwareLog.i(TAG, "error split item");
                    return false;
                }
                long minFloatThres = Long.parseLong(sets[0].trim());
                long maxFloatThres = Long.parseLong(sets[1].trim());
                long percentage = Long.parseLong(sets[2].trim());
                if (minFloatThres >= 0 && maxFloatThres >= 0 && minFloatThres < maxFloatThres) {
                    if (percentage >= 1) {
                        memThresHolds[0] = minFloatThres * 1024;
                        memThresHolds[1] = 1024 * maxFloatThres;
                        memThresHolds[2] = percentage;
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("mem minthres:");
                        stringBuilder.append(minFloatThres);
                        stringBuilder.append(",maxthres:");
                        stringBuilder.append(maxFloatThres);
                        stringBuilder.append(",percent:");
                        stringBuilder.append(percentage);
                        AwareLog.i(str, stringBuilder.toString());
                        return true;
                    }
                }
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("error minthres:");
                stringBuilder2.append(minFloatThres);
                stringBuilder2.append(",maxthres:");
                stringBuilder2.append(maxFloatThres);
                stringBuilder2.append(",percent:");
                stringBuilder2.append(percentage);
                AwareLog.i(str2, stringBuilder2.toString());
                return false;
            } catch (NumberFormatException e) {
                AwareLog.e(TAG, "parse memory xml error");
                return false;
            }
        } catch (NumberFormatException e2) {
            String str3 = itemValue;
            AwareLog.e(TAG, "parse memory xml error");
            return false;
        }
    }

    private void loadMemRepairProcThresItem(long totalMemMb, Item item, int itemSize) {
        int i = itemSize;
        if (i == 2) {
            long[][] thresHolds = (long[][]) Array.newInstance(long.class, new int[]{1, 2});
            int index = 0;
            for (SubItem subItem : item.getSubItemList()) {
                if (index >= i) {
                    index = i + 1;
                } else if (subItem != null) {
                    String itemName = subItem.getName();
                    String itemValue = subItem.getValue();
                    if (TextUtils.isEmpty(itemName) || TextUtils.isEmpty(itemValue)) {
                        AwareLog.i(TAG, "loadMemRepairProcThresItem null item");
                    } else {
                        boolean finded = true;
                        int findIndex = 0;
                        int i2 = -1;
                        int hashCode = itemName.hashCode();
                        if (hashCode != -1332194002) {
                            if (hashCode == 1984457027 && itemName.equals(MemoryConstant.MEM_REPAIR_CONSTANT_FG)) {
                                i2 = 0;
                            }
                        } else if (itemName.equals(MemoryConstant.MEM_REPAIR_CONSTANT_BG)) {
                            i2 = 1;
                        }
                        switch (i2) {
                            case 0:
                                findIndex = 0;
                                break;
                            case 1:
                                findIndex = 1;
                                break;
                            default:
                                finded = false;
                                String str = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("loadMemRepairProcThresItem no such configuration:");
                                stringBuilder.append(itemName);
                                AwareLog.w(str, stringBuilder.toString());
                                index = i + 1;
                                break;
                        }
                        int index2 = index;
                        index = findIndex;
                        if (finded) {
                            if (parseProcThres(itemValue.trim(), thresHolds[0], index, totalMemMb)) {
                                index2++;
                            } else {
                                index = i + 1;
                            }
                        }
                        index = index2;
                    }
                }
                if (index == i && thresHolds[0][0] > thresHolds[0][1]) {
                    MemRepairPolicy.getInstance().updateProcThresHold(thresHolds);
                }
            }
            MemRepairPolicy.getInstance().updateProcThresHold(thresHolds);
        }
    }

    private boolean parseProcThres(String itemValue, long[] thresHolds, int index, long totalMemMb) {
        try {
            String str;
            StringBuilder stringBuilder;
            long thres = Long.parseLong(itemValue.trim());
            if (thres >= 1) {
                if (thres < totalMemMb) {
                    thresHolds[index] = 1024 * thres;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("process threshold:");
                    stringBuilder.append(thres);
                    AwareLog.i(str, stringBuilder.toString());
                    return true;
                }
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("error process threshold:");
            stringBuilder.append(thres);
            AwareLog.i(str, stringBuilder.toString());
            return false;
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "parse memory xml error");
            return false;
        }
    }

    private int getSize(String strSize) {
        if (strSize == null) {
            return 0;
        }
        try {
            int iSize = Integer.parseInt(strSize.trim());
            if (iSize >= 1) {
                return iSize;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("loadMemRepairMinMaxThresItem error size:");
            stringBuilder.append(iSize);
            AwareLog.w(str, stringBuilder.toString());
            return 0;
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "parse size error");
            return 0;
        }
    }

    private void readMemoryAPIWhiteListUid() {
        String cameraPackagename = MemoryConstant.CAMERA_PACKAGE_NAME;
        MultiTaskManagerService mMtmService = MultiTaskManagerService.self();
        if (mMtmService != null) {
            Context mcontext = mMtmService.context();
            if (mcontext != null) {
                PackageManager pm = mcontext.getPackageManager();
                if (pm != null) {
                    try {
                        ApplicationInfo appInfo = pm.getApplicationInfo(cameraPackagename, 1);
                        if (appInfo != null && appInfo.uid > 0) {
                            MemoryConstant.setSysCameraUid(appInfo.uid);
                        }
                    } catch (NameNotFoundException e) {
                        AwareLog.e(TAG, "can not get uid");
                    }
                }
            }
        }
    }
}
