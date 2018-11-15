package com.android.server.rms.iaware.feature;

import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.rms.iaware.AwareConfig;
import android.rms.iaware.AwareConfig.Item;
import android.rms.iaware.AwareConfig.SubItem;
import android.rms.iaware.AwareConstant.FeatureType;
import android.rms.iaware.AwareConstant.ResourceType;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import android.rms.iaware.DumpData;
import android.rms.iaware.IAwareCMSManager;
import android.rms.iaware.IAwaredConnection;
import android.rms.iaware.StatisticsData;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.internal.util.MemInfoReader;
import com.android.server.rms.iaware.IRDataRegister;
import com.android.server.rms.iaware.memory.action.Action;
import com.android.server.rms.iaware.memory.action.GpuCompressAction;
import com.android.server.rms.iaware.memory.action.KillAction;
import com.android.server.rms.iaware.memory.action.QuickKillAction;
import com.android.server.rms.iaware.memory.action.ReclaimAction;
import com.android.server.rms.iaware.memory.action.SystemTrimAction;
import com.android.server.rms.iaware.memory.data.dispatch.DataDispatch;
import com.android.server.rms.iaware.memory.policy.DMEServer;
import com.android.server.rms.iaware.memory.policy.MemoryExecutorServer;
import com.android.server.rms.iaware.memory.policy.MemoryScenePolicy;
import com.android.server.rms.iaware.memory.policy.MemoryScenePolicyList;
import com.android.server.rms.iaware.memory.utils.BigDataStore;
import com.android.server.rms.iaware.memory.utils.BigMemoryInfo;
import com.android.server.rms.iaware.memory.utils.EventTracker;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.rms.iaware.memory.utils.MemoryReader;
import com.android.server.rms.iaware.memory.utils.MemoryUtils;
import com.android.server.rms.iaware.memory.utils.PackageInfoCollector;
import com.android.systemui.shared.recents.hwutil.HwRecentsTaskUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONException;
import org.json.JSONObject;

public class MemoryFeature extends RFeature {
    private static final int INTERVAL_SAVE_DATA_IN_MSEC = 7200000;
    private static final String TAG = "AwareMem_MemFeature";
    private static final FeatureHandler mFeatureHandler = new FeatureHandler();
    private BigDataStore mBigDataStore = BigDataStore.getInstance();
    private final AtomicBoolean mRunning = new AtomicBoolean(false);

    private static final class FeatureHandler extends Handler {
        private static final int MSG_CONFIG_PROTECTLRU = 102;
        private static final int MSG_DISABLE_PROTECTLRU = 101;
        private boolean mFirstAccess;

        private FeatureHandler() {
            this.mFirstAccess = true;
        }

        public void removeProtectLruMsg() {
            removeMessages(101);
        }

        public void enableProtectLru() {
            removeProtectLruMsg();
            if (this.mFirstAccess) {
                sendEmptyMessageDelayed(102, HwRecentsTaskUtils.MAX_REMOVE_TASK_TIME);
            }
            DMEServer.getInstance().notifyProtectLruState(0);
            this.mFirstAccess = false;
        }

        public void disableProtectLru() {
            removeProtectLruMsg();
            sendEmptyMessageDelayed(101, 1000);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 101:
                    MemoryUtils.disableProtectLru();
                    return;
                case 102:
                    MemoryUtils.onProtectLruConfigUpdate();
                    return;
                default:
                    String str = MemoryFeature.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("error msg what = ");
                    stringBuilder.append(msg.what);
                    AwareLog.e(str, stringBuilder.toString());
                    return;
            }
        }
    }

    public MemoryFeature(Context context, FeatureType featureType, IRDataRegister dataRegister) {
        super(context, featureType, dataRegister);
        MemoryConstant.init(this.mContext);
        MemoryConstant.setCameraPowerUPMem();
        loadMemConfig();
    }

    public boolean enable() {
        subscribleEvents();
        DMEServer.getInstance().enable();
        DataDispatch.getInstance().start();
        setBoostKillSwitch(true);
        MemoryUtils.writeSwappiness(MemoryConstant.getConfigSwappiness());
        MemoryUtils.writeDirectSwappiness(MemoryConstant.getConfigDirectSwappiness());
        mFeatureHandler.enableProtectLru();
        this.mRunning.set(true);
        return true;
    }

    public boolean disable() {
        MemoryUtils.writeExtraFreeKbytes(MemoryConstant.DEFAULT_EXTRA_FREE_KBYTES);
        MemoryUtils.writeSwappiness(60);
        MemoryUtils.writeDirectSwappiness(60);
        setBoostKillSwitch(false);
        mFeatureHandler.disableProtectLru();
        DataDispatch.getInstance().stop();
        DMEServer.getInstance().disable();
        unSubscribeEvents();
        this.mRunning.set(false);
        return true;
    }

    public boolean reportData(CollectData data) {
        if (!this.mRunning.get()) {
            return false;
        }
        DataDispatch.getInstance().reportData(data);
        return true;
    }

    public ArrayList<DumpData> getDumpData(int time) {
        if (this.mRunning.get()) {
            return EventTracker.getInstance().getDumpData(time);
        }
        return null;
    }

    public String saveBigData(boolean clear) {
        AwareLog.d(TAG, "enter saveBigData");
        if (!this.mRunning.get()) {
            return null;
        }
        this.mBigDataStore.totalTimeEnd = SystemClock.elapsedRealtime();
        this.mBigDataStore.getMeminfoAllocCount();
        this.mBigDataStore.getLmkOccurCount();
        String rtStatisJsonStr = creatJsonStr();
        if (clear) {
            clearCache();
        }
        return rtStatisJsonStr;
    }

    public ArrayList<StatisticsData> getStatisticsData() {
        if (this.mRunning.get()) {
            return EventTracker.getInstance().getStatisticsData();
        }
        return null;
    }

    public boolean configUpdate() {
        loadMemConfig();
        MemoryUtils.onProtectLruConfigUpdate();
        DMEServer.getInstance().notifyProtectLruState(0);
        return true;
    }

    private void loadMemConfig() {
        loadMemConstantConfig(false);
        loadBigMemAppPolicyConfig(getConfig(MemoryConstant.MEM_POLICY_FEATURENAME, MemoryConstant.MEM_POLICY_BIGMEMAPP, true));
        MemoryExecutorServer.getInstance().setMemoryScenePolicyList(loadMemPolicyListConfig());
        loadFileCacheConfig();
        loadMemConstantConfig(true);
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

    public static void loadBigMemAppPolicyConfig(AwareConfig custConfigList) {
        if (custConfigList == null) {
            AwareLog.w(TAG, "loadBigMemAppConfig failure cause null configList");
            return;
        }
        for (Item item : custConfigList.getConfigList()) {
            if (item == null || item.getProperties() == null) {
                AwareLog.w(TAG, "loadBigMemAppPolicyConfig continue cause null item");
            } else {
                Map<String, String> configPropertries = item.getProperties();
                String appName = (String) configPropertries.get(MemoryConstant.MEM_POLICY_BIGAPPNAME);
                String str;
                if (appName != null) {
                    str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("big memory app is ");
                    stringBuilder.append(appName);
                    AwareLog.d(str, stringBuilder.toString());
                    saveBigMemoryAppConfig(appName, item);
                } else {
                    str = (String) configPropertries.get(MemoryConstant.MEM_CONSTANT_RAMSIZENAME);
                    Long totalMemMb = Long.valueOf(MemoryReader.getInstance().getTotalRam() / 1024);
                    if (MemoryUtils.checkRamSize(str, totalMemMb)) {
                        saveCameraIonConfig(item);
                    } else {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("checkRamSize failure cause ramSize: ");
                        stringBuilder2.append(str);
                        stringBuilder2.append(" totalMemMb: ");
                        stringBuilder2.append(totalMemMb);
                        AwareLog.d(str2, stringBuilder2.toString());
                    }
                }
            }
        }
    }

    private static void saveBigMemoryAppConfig(String appName, Item item) {
        String appNameTemp = appName;
        for (SubItem subItem : item.getSubItemList()) {
            String itemName = subItem.getName();
            String itemValue = subItem.getValue();
            if (itemName != null) {
                if (itemValue != null) {
                    Object obj = -1;
                    try {
                        if (itemName.hashCode() == -1109843021 && itemName.equals(MemoryConstant.MEM_SCENE_LAUNCH)) {
                            obj = null;
                        }
                        if (obj != null) {
                            AwareLog.w(TAG, "no such configuration!");
                        } else {
                            long launchRequestMem = Long.parseLong(itemValue.trim());
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("saveBigMemoryAppConfig ");
                            stringBuilder.append(appNameTemp);
                            stringBuilder.append(" request memory is ");
                            stringBuilder.append(launchRequestMem);
                            AwareLog.d(str, stringBuilder.toString());
                            BigMemoryInfo.getInstance().setRequestMemForLaunch(appNameTemp, launchRequestMem);
                        }
                    } catch (NumberFormatException e) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("parse app mem error: ");
                        stringBuilder2.append(e);
                        AwareLog.e(str2, stringBuilder2.toString());
                    }
                }
            }
        }
    }

    private static void saveCameraIonConfig(Item item) {
        for (SubItem subItem : item.getSubItemList()) {
            String itemName = subItem.getName();
            String itemValue = subItem.getValue();
            if (itemName != null) {
                if (itemValue != null) {
                    Object obj = -1;
                    try {
                        int hashCode = itemName.hashCode();
                        if (hashCode != -190523471) {
                            if (hashCode == 2089932130 && itemName.equals(MemoryConstant.MEM_CONSTANT_IONSPEEDUPSWITCH)) {
                                obj = null;
                            }
                        } else if (itemName.equals(MemoryConstant.MEM_CONSTANT_CAMERAPOWERUPNAME)) {
                            obj = 1;
                        }
                        int ionSpeedupSwitch;
                        String str;
                        StringBuilder stringBuilder;
                        switch (obj) {
                            case null:
                                ionSpeedupSwitch = Integer.parseInt(itemValue.trim());
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("saveCameraIonConfigs camera ion memory speedup switch: ");
                                stringBuilder.append(ionSpeedupSwitch);
                                AwareLog.i(str, stringBuilder.toString());
                                MemoryConstant.setConfigIonSpeedupSwitch(ionSpeedupSwitch);
                                break;
                            case 1:
                                ionSpeedupSwitch = Integer.parseInt(itemValue.trim());
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("saveCameraIonConfigs camera powerup memory: ");
                                stringBuilder.append(ionSpeedupSwitch * 1024);
                                AwareLog.i(str, stringBuilder.toString());
                                MemoryConstant.setCameraPowerUPMemoryDefault(ionSpeedupSwitch * 1024);
                                break;
                            default:
                                String str2 = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("saveCameraIonConfigs no such configuration. ");
                                stringBuilder2.append(itemName);
                                AwareLog.w(str2, stringBuilder2.toString());
                                break;
                        }
                    } catch (NumberFormatException e) {
                        AwareLog.e(TAG, "saveCameraIonConfigs parse memory xml error");
                    }
                }
            }
        }
    }

    private void loadFileCacheConfig() {
        AwareLog.d(TAG, "loadFileCacheConfig begin");
        AwareConfig configList = getConfig(MemoryConstant.MEM_POLICY_FEATURENAME, MemoryConstant.MEM_POLICY_FILECACHE, false);
        if (configList == null) {
            AwareLog.w(TAG, "loadFileCacheConfig failure cause null configList");
            return;
        }
        ArrayMap<Integer, ArraySet<String>> fileCacheMap = new ArrayMap();
        for (Item item : configList.getConfigList()) {
            if (item == null || item.getProperties() == null) {
                AwareLog.w(TAG, "loadFileCacheConfig continue cause null item");
            } else {
                Map<String, String> configPropertries = item.getProperties();
                String strName = (String) configPropertries.get("name");
                String str;
                if (TextUtils.isEmpty(strName)) {
                    str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("loadFileCacheConfig failure cause name: ");
                    stringBuilder.append(strName);
                    AwareLog.w(str, stringBuilder.toString());
                } else {
                    str = (String) configPropertries.get(MemoryConstant.MEM_FILECACHE_ITEM_LEVEL);
                    int fileCacheLevel = checkFileCacheLevel(str);
                    String str2;
                    StringBuilder stringBuilder2;
                    if (fileCacheLevel == -1) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("checkFileCacheLevel failure cause level: ");
                        stringBuilder2.append(str);
                        AwareLog.w(str2, stringBuilder2.toString());
                    } else {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("loadFileCacheConfig success. level: ");
                        stringBuilder2.append(str);
                        AwareLog.d(str2, stringBuilder2.toString());
                        if (item.getSubItemList() == null) {
                            AwareLog.w(TAG, "loadFileCacheConfig continue cause null item");
                        } else {
                            ArraySet<String> xmlSet = new ArraySet();
                            for (SubItem subitem : item.getSubItemList()) {
                                if (!(subitem == null || TextUtils.isEmpty(subitem.getValue()))) {
                                    xmlSet.add(subitem.getValue());
                                }
                            }
                            String str3 = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("loadFileCacheConfig xmlSet=");
                            stringBuilder3.append(xmlSet.toString());
                            AwareLog.i(str3, stringBuilder3.toString());
                            if ("file".equals(strName)) {
                                fileCacheMap.put(Integer.valueOf(fileCacheLevel), xmlSet);
                            } else if ("package".equals(strName)) {
                                ArraySet<String> pkgSet = PackageInfoCollector.getLibFilesFromPackage(this.mContext, xmlSet);
                                if (pkgSet != null && pkgSet.size() > 0) {
                                    fileCacheMap.put(Integer.valueOf(fileCacheLevel + 50), pkgSet);
                                }
                            }
                        }
                    }
                }
            }
        }
        MemoryConstant.setFileCacheMap(fileCacheMap);
        AwareLog.d(TAG, "loadFileCacheConfig end");
    }

    private int checkFileCacheLevel(String fileCacheLevel) {
        int i = -1;
        if (TextUtils.isEmpty(fileCacheLevel)) {
            return -1;
        }
        try {
            int level = Integer.parseInt(fileCacheLevel.trim());
            if (1 <= level && level <= 3) {
                i = level;
            }
            return i;
        } catch (NumberFormatException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("parse filecache index error: ");
            stringBuilder.append(e);
            AwareLog.e(str, stringBuilder.toString());
            return -1;
        }
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
                            case -1111598932:
                                if (itemName.equals(MemoryConstant.MEM_CONSTANT_RESERVEDZRAMNAME)) {
                                    obj = 6;
                                    break;
                                }
                                break;
                            case -633324046:
                                if (itemName.equals(MemoryConstant.MEM_CONSTANT_EMERGEMCYMEMORYNAME)) {
                                    obj = 2;
                                    break;
                                }
                                break;
                            case -489919045:
                                if (itemName.equals(MemoryConstant.MEM_CONSTANT_PROTECTLRULIMIT)) {
                                    obj = 10;
                                    break;
                                }
                                break;
                            case -484609525:
                                if (itemName.equals(MemoryConstant.MEM_CONSTANT_PROTECTRATIO)) {
                                    obj = 11;
                                    break;
                                }
                                break;
                            case -286045405:
                                if (itemName.equals(MemoryConstant.MEM_CONSTANT_SWAPPINESSNAME)) {
                                    obj = 7;
                                    break;
                                }
                                break;
                            case -34546488:
                                if (itemName.equals(MemoryConstant.MEM_CONSTANT_NORMALMEMORYNAME)) {
                                    obj = 5;
                                    break;
                                }
                                break;
                            case 175422052:
                                if (itemName.equals(MemoryConstant.MEM_CONSTANT_EXTRAFREEKBYTESNAME)) {
                                    obj = 9;
                                    break;
                                }
                                break;
                            case 274770233:
                                if (itemName.equals(MemoryConstant.MEM_CONSTANT_DIRECTSWAPPINESSNAME)) {
                                    obj = 8;
                                    break;
                                }
                                break;
                            case 671098540:
                                if (itemName.equals(MemoryConstant.MEM_CONSTANT_HIGHCPULOADNAME)) {
                                    obj = 1;
                                    break;
                                }
                                break;
                            case 745458209:
                                if (itemName.equals(MemoryConstant.MEM_CONSTANT_DEFAULTCRITICALMEMORYNAME)) {
                                    obj = 4;
                                    break;
                                }
                                break;
                            case 1078523066:
                                if (itemName.equals(MemoryConstant.MEM_CONSTANT_LOWCPULOADNAME)) {
                                    obj = null;
                                    break;
                                }
                                break;
                            case 1403791765:
                                if (itemName.equals(MemoryConstant.MEM_CONSTANT_BIGMEMCRITICALMEMORYNAME)) {
                                    obj = 3;
                                    break;
                                }
                                break;
                        }
                        String str;
                        StringBuilder stringBuilder;
                        int swappiness;
                        String str2;
                        StringBuilder stringBuilder2;
                        switch (obj) {
                            case null:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("loadMemConfig itemName: ");
                                stringBuilder.append(itemName);
                                AwareLog.i(str, stringBuilder.toString());
                                saveMemConstantKillItems(itemName, itemValue);
                                break;
                            case 6:
                                long reservedZram = Long.parseLong(itemValue.trim());
                                String str3 = TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("loadMemConfig reservedZram: ");
                                stringBuilder3.append(reservedZram * 1024);
                                AwareLog.i(str3, stringBuilder3.toString());
                                MemoryConstant.setReservedZramSpace(1024 * reservedZram);
                                break;
                            case 7:
                                swappiness = Integer.parseInt(itemValue.trim());
                                str2 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("loadMemConfig swappiness: ");
                                stringBuilder2.append(swappiness);
                                AwareLog.i(str2, stringBuilder2.toString());
                                MemoryConstant.setConfigSwappiness(swappiness);
                                break;
                            case 8:
                                swappiness = Integer.parseInt(itemValue.trim());
                                str2 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("loadMemConfig directswappiness: ");
                                stringBuilder2.append(swappiness);
                                AwareLog.i(str2, stringBuilder2.toString());
                                MemoryConstant.setConfigDirectSwappiness(swappiness);
                                break;
                            case 9:
                                swappiness = Integer.parseInt(itemValue.trim());
                                str2 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("loadMemConfig extra_free_kbytes: ");
                                stringBuilder2.append(swappiness);
                                AwareLog.i(str2, stringBuilder2.toString());
                                MemoryConstant.setConfigExtraFreeKbytes(swappiness);
                                break;
                            case 10:
                                str = itemValue.trim();
                                str2 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("loadMemConfig protect lru limit: ");
                                stringBuilder2.append(str);
                                AwareLog.i(str2, stringBuilder2.toString());
                                MemoryConstant.setConfigProtectLruLimit(str);
                                break;
                            case 11:
                                swappiness = Integer.parseInt(itemValue.trim());
                                str2 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("loadMemConfig protect lru ratio: ");
                                stringBuilder2.append(swappiness);
                                AwareLog.i(str2, stringBuilder2.toString());
                                MemoryConstant.setConfigProtectLruRatio(swappiness);
                                break;
                            default:
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("loadMemConfig no such configuration. ");
                                stringBuilder.append(itemName);
                                AwareLog.w(str, stringBuilder.toString());
                                break;
                        }
                    } catch (NumberFormatException e) {
                        AwareLog.e(TAG, "parse memory xml error");
                    }
                }
            }
        }
    }

    private void saveMemConstantKillItems(String itemName, String itemValue) {
        Object obj = -1;
        try {
            switch (itemName.hashCode()) {
                case -633324046:
                    if (itemName.equals(MemoryConstant.MEM_CONSTANT_EMERGEMCYMEMORYNAME)) {
                        obj = 2;
                        break;
                    }
                    break;
                case -34546488:
                    if (itemName.equals(MemoryConstant.MEM_CONSTANT_NORMALMEMORYNAME)) {
                        obj = 5;
                        break;
                    }
                    break;
                case 671098540:
                    if (itemName.equals(MemoryConstant.MEM_CONSTANT_HIGHCPULOADNAME)) {
                        obj = 1;
                        break;
                    }
                    break;
                case 745458209:
                    if (itemName.equals(MemoryConstant.MEM_CONSTANT_DEFAULTCRITICALMEMORYNAME)) {
                        obj = 4;
                        break;
                    }
                    break;
                case 1078523066:
                    if (itemName.equals(MemoryConstant.MEM_CONSTANT_LOWCPULOADNAME)) {
                        obj = null;
                        break;
                    }
                    break;
                case 1403791765:
                    if (itemName.equals(MemoryConstant.MEM_CONSTANT_BIGMEMCRITICALMEMORYNAME)) {
                        obj = 3;
                        break;
                    }
                    break;
            }
            long lowCpuLoad;
            String str;
            StringBuilder stringBuilder;
            long emergemcyMemory;
            String str2;
            StringBuilder stringBuilder2;
            switch (obj) {
                case null:
                    lowCpuLoad = Long.parseLong(itemValue.trim());
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("loadMemConfig lowCpuLoad: ");
                    stringBuilder.append(lowCpuLoad);
                    AwareLog.i(str, stringBuilder.toString());
                    MemoryConstant.setIdleThresHold(lowCpuLoad);
                    return;
                case 1:
                    lowCpuLoad = Long.parseLong(itemValue.trim());
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("loadMemConfig highCpuLoad: ");
                    stringBuilder.append(lowCpuLoad);
                    AwareLog.i(str, stringBuilder.toString());
                    MemoryConstant.setNormalThresHold(lowCpuLoad);
                    return;
                case 2:
                    emergemcyMemory = Long.parseLong(itemValue.trim());
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("loadMemConfig emergemcyMemory: ");
                    stringBuilder2.append(emergemcyMemory * 1024);
                    AwareLog.i(str2, stringBuilder2.toString());
                    MemoryConstant.setEmergencyMemory(1024 * emergemcyMemory);
                    return;
                case 3:
                    emergemcyMemory = Long.parseLong(itemValue.trim());
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("loadMemConfig bigMemAppCriticalMemory: ");
                    stringBuilder2.append(emergemcyMemory * 1024);
                    AwareLog.i(str2, stringBuilder2.toString());
                    MemoryConstant.setBigMemoryAppCriticalMemory(1024 * emergemcyMemory);
                    return;
                case 4:
                    emergemcyMemory = Long.parseLong(itemValue.trim());
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("loadMemConfig criticalMemory: ");
                    stringBuilder2.append(emergemcyMemory * 1024);
                    AwareLog.i(str2, stringBuilder2.toString());
                    MemoryConstant.setDefaultCriticalMemory(1024 * emergemcyMemory);
                    return;
                case 5:
                    emergemcyMemory = Long.parseLong(itemValue.trim());
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("loadMemConfig normalMemory: ");
                    stringBuilder2.append(emergemcyMemory * 1024);
                    AwareLog.i(str2, stringBuilder2.toString());
                    MemoryConstant.setIdleMemory(1024 * emergemcyMemory);
                    return;
                default:
                    str2 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("loadMemConfig no such configuration. ");
                    stringBuilder3.append(itemName);
                    AwareLog.w(str2, stringBuilder3.toString());
                    return;
            }
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "parse memory xml error");
        }
    }

    private MemoryScenePolicyList loadMemPolicyListConfig() {
        Map<String, MemoryScenePolicy> memoryScenePolicies = new ArrayMap();
        AwareConfig configList = getConfig(MemoryConstant.MEM_POLICY_FEATURENAME, MemoryConstant.MEM_POLICY_CONFIGNAME, false);
        if (configList == null) {
            return new MemoryScenePolicyList(createDefaultMemorPolicyList());
        }
        for (Item item : configList.getConfigList()) {
            if (item.getProperties() != null) {
                String scene = (String) item.getProperties().get(MemoryConstant.MEM_POLICY_SCENE);
                if (scene != null) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("add scene: ");
                    stringBuilder.append(scene);
                    AwareLog.d(str, stringBuilder.toString());
                    ArrayList<Action> memActions = getActionList(item);
                    if (memActions.size() > 0) {
                        memoryScenePolicies.put(scene, new MemoryScenePolicy(scene, memActions));
                    }
                }
            }
        }
        return new MemoryScenePolicyList(memoryScenePolicies);
    }

    private Map<String, MemoryScenePolicy> createDefaultMemorPolicyList() {
        Map<String, MemoryScenePolicy> memoryScenePolicies = new ArrayMap();
        ArrayList<Action> memActions = new ArrayList();
        memActions.add(new KillAction(this.mContext));
        memoryScenePolicies.put(MemoryConstant.MEM_SCENE_DEFAULT, new MemoryScenePolicy(MemoryConstant.MEM_SCENE_DEFAULT, memActions));
        ArrayList<Action> bigMemActions = new ArrayList();
        bigMemActions.add(new QuickKillAction(this.mContext));
        memoryScenePolicies.put(MemoryConstant.MEM_SCENE_BIGMEM, new MemoryScenePolicy(MemoryConstant.MEM_SCENE_BIGMEM, bigMemActions));
        ArrayList<Action> idleActions = new ArrayList();
        idleActions.add(new ReclaimAction(this.mContext));
        idleActions.add(new GpuCompressAction(this.mContext));
        idleActions.add(new SystemTrimAction(this.mContext));
        memoryScenePolicies.put(MemoryConstant.MEM_SCENE_IDLE, new MemoryScenePolicy(MemoryConstant.MEM_SCENE_IDLE, idleActions));
        AwareLog.i(TAG, "use default memory policy cause reading xml config failure");
        return memoryScenePolicies;
    }

    private ArrayList<Action> getActionList(Item item) {
        ArrayList<Action> actions = new ArrayList();
        for (SubItem subItem : item.getSubItemList()) {
            if (subItem.getProperties() != null) {
                Action action;
                String actionName = (String) subItem.getProperties().get("name");
                Object obj = -1;
                switch (actionName.hashCode()) {
                    case 102461:
                        if (actionName.equals(MemoryConstant.MEM_POLICY_GMCACTION)) {
                            obj = 3;
                            break;
                        }
                        break;
                    case 3291998:
                        if (actionName.equals(MemoryConstant.MEM_POLICY_KILLACTION)) {
                            obj = 1;
                            break;
                        }
                        break;
                    case 643839697:
                        if (actionName.equals(MemoryConstant.MEM_POLICY_SYSTEMTRIMACTION)) {
                            obj = 4;
                            break;
                        }
                        break;
                    case 1082491369:
                        if (actionName.equals(MemoryConstant.MEM_POLICY_RECLAIM)) {
                            obj = null;
                            break;
                        }
                        break;
                    case 1301455563:
                        if (actionName.equals(MemoryConstant.MEM_POLICY_QUICKKILLACTION)) {
                            obj = 2;
                            break;
                        }
                        break;
                }
                switch (obj) {
                    case null:
                        action = new ReclaimAction(this.mContext);
                        break;
                    case 1:
                        action = new KillAction(this.mContext);
                        break;
                    case 2:
                        action = new QuickKillAction(this.mContext);
                        break;
                    case 3:
                        action = new GpuCompressAction(this.mContext);
                        break;
                    case 4:
                        action = new SystemTrimAction(this.mContext);
                        break;
                    default:
                        AwareLog.e(TAG, "no such action!");
                        action = null;
                        break;
                }
                if (action != null) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("add action: ");
                    stringBuilder.append(actionName);
                    AwareLog.d(str, stringBuilder.toString());
                    actions.add(action);
                }
            }
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getActionList return: ");
        stringBuilder2.append(actions);
        AwareLog.d(str2, stringBuilder2.toString());
        return actions;
    }

    private AwareConfig getConfig(String featureName, String configName, boolean isCustConfig) {
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

    private void subscribleEvents() {
        if (this.mIRDataRegister != null) {
            this.mIRDataRegister.subscribeData(ResourceType.RES_APP, this.mFeatureType);
            this.mIRDataRegister.subscribeData(ResourceType.RES_DEV_STATUS, this.mFeatureType);
            this.mIRDataRegister.subscribeData(ResourceType.RESOURCE_SCREEN_ON, this.mFeatureType);
            this.mIRDataRegister.subscribeData(ResourceType.RESOURCE_SCREEN_OFF, this.mFeatureType);
            this.mIRDataRegister.subscribeData(ResourceType.RES_INPUT, this.mFeatureType);
            this.mIRDataRegister.subscribeData(ResourceType.RESOURCE_USERHABIT, this.mFeatureType);
        }
    }

    private void unSubscribeEvents() {
        if (this.mIRDataRegister != null) {
            this.mIRDataRegister.unSubscribeData(ResourceType.RES_APP, this.mFeatureType);
            this.mIRDataRegister.unSubscribeData(ResourceType.RES_DEV_STATUS, this.mFeatureType);
            this.mIRDataRegister.unSubscribeData(ResourceType.RESOURCE_SCREEN_ON, this.mFeatureType);
            this.mIRDataRegister.unSubscribeData(ResourceType.RESOURCE_SCREEN_OFF, this.mFeatureType);
            this.mIRDataRegister.unSubscribeData(ResourceType.RES_INPUT, this.mFeatureType);
        }
    }

    private void setBoostKillSwitch(boolean isEnable) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setBoostSigKill switch = ");
        stringBuilder.append(isEnable);
        AwareLog.i(str, stringBuilder.toString());
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(301);
        buffer.putInt(isEnable);
        IAwaredConnection.getInstance().sendPacket(buffer.array());
    }

    private String creatJsonStr() {
        JSONObject memoryAllocateJson = new JSONObject();
        JSONObject memoryControlAndLMKJson = new JSONObject();
        JSONObject availMemoryTimeJson = new JSONObject();
        try {
            memoryAllocateJson.put("memoryAllocCount", String.valueOf(this.mBigDataStore.meminfoAllocCount - this.mBigDataStore.meminfoAllocCountStash));
            memoryAllocateJson.put("slowPathAllocCount", String.valueOf(this.mBigDataStore.slowPathAllocCount - this.mBigDataStore.slowPathAllocCountStash));
            memoryControlAndLMKJson.put("LMKCount", String.valueOf(this.mBigDataStore.lmkOccurCount - this.mBigDataStore.lmkOccurCountStash));
            memoryControlAndLMKJson.put("memoryControlCount", String.valueOf(this.mBigDataStore.lowMemoryManageCount));
            availMemoryTimeJson.put("belowThresholdTime", String.valueOf(this.mBigDataStore.belowThresholdTime));
            this.mBigDataStore.aboveThresholdTime = (this.mBigDataStore.totalTimeEnd - this.mBigDataStore.totalTimeBegin) - this.mBigDataStore.belowThresholdTime;
            availMemoryTimeJson.put("aboveThresholdTime", String.valueOf(this.mBigDataStore.aboveThresholdTime));
        } catch (JSONException e) {
            AwareLog.e(TAG, "JSONException...");
        }
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("[iAwareMemoryRTStatis_Start]\n");
        strBuilder.append("{\n");
        strBuilder.append("\"");
        strBuilder.append("memoryAllocateCount");
        strBuilder.append("\"");
        strBuilder.append(":");
        strBuilder.append(memoryAllocateJson.toString());
        strBuilder.append(",");
        strBuilder.append("\n");
        strBuilder.append("\"");
        strBuilder.append("memoryControlAndLMKCount");
        strBuilder.append("\"");
        strBuilder.append(":");
        strBuilder.append(memoryControlAndLMKJson.toString());
        strBuilder.append(",");
        strBuilder.append("\n");
        strBuilder.append("\"");
        strBuilder.append("availMemoryTimeCount");
        strBuilder.append("\"");
        strBuilder.append(":");
        strBuilder.append(availMemoryTimeJson.toString());
        strBuilder.append(",");
        strBuilder.append("\n");
        strBuilder.append("}\n");
        strBuilder.append("[iAwareMemoryRTStatis_End]");
        return strBuilder.toString();
    }

    private void clearCache() {
        AwareLog.e(TAG, "enter clearCache...");
        this.mBigDataStore.lowMemoryManageCount = 0;
        this.mBigDataStore.belowThresholdTime = 0;
        this.mBigDataStore.meminfoAllocCountStash = this.mBigDataStore.meminfoAllocCount;
        this.mBigDataStore.slowPathAllocCountStash = this.mBigDataStore.slowPathAllocCount;
        this.mBigDataStore.lmkOccurCountStash = this.mBigDataStore.lmkOccurCount;
        this.mBigDataStore.aboveThresholdTime = 0;
        this.mBigDataStore.totalTimeBegin = SystemClock.elapsedRealtime();
        this.mBigDataStore.totalTimeEnd = SystemClock.elapsedRealtime();
    }
}
