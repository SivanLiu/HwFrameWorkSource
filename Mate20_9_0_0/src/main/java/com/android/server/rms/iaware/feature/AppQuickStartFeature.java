package com.android.server.rms.iaware.feature;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.rms.iaware.AwareConfig;
import android.rms.iaware.AwareConfig.Item;
import android.rms.iaware.AwareConfig.SubItem;
import android.rms.iaware.AwareConstant.FeatureType;
import android.rms.iaware.AwareConstant.ResourceType;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import android.rms.iaware.IAwareCMSManager;
import android.rms.iaware.IAwaredConnection;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.internal.util.MemInfoReader;
import com.android.server.rms.algorithm.AwareUserHabit;
import com.android.server.rms.iaware.IRDataRegister;
import com.android.server.rms.iaware.memory.data.content.AttrSegments;
import com.android.server.rms.iaware.memory.data.content.AttrSegments.Builder;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.huawei.android.app.HwActivityManager;
import com.huawei.displayengine.IDisplayEngineService;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppQuickStartFeature extends RFeature {
    private static final String AQS_EXCTPRD_ACTNAME = "ActName";
    private static final String AQS_EXCTPRD_APPNAME = "AppName";
    private static final String AQS_EXCTPRD_COLLECT_TIMEOUT = "CollectTimeout";
    private static final String AQS_EXCTPRD_EXCLUDED_APP = "ExcludedApp";
    private static final String AQS_EXCTPRD_INCLUDED_ACT = "IncludedAct";
    private static final String AQS_EXCTPRD_LAUNCH_TIMEOUT = "LaunchTimeout";
    private static final String AQS_EXCTPRD_NAME = "ExactPreread";
    private static final String AQS_EXCTPRD_PAUSE_DELAY = "PauseDelay";
    private static final String AQS_EXCTPRD_PAUSE_TIMEOUT = "PauseTimeout";
    private static final String AQS_EXCTPRD_RAMSIZE = "ramsize";
    private static final String AQS_EXCTPRD_SWITCH = "Switch";
    private static final String AQS_EXCTPRD_TYPE = "type";
    private static final String AQS_FEATURENAME = "AppQuickStart";
    private static final int FEATURE_MIN_VERSION = 3;
    private static final String TAG = "AppQuickStart";
    private static boolean mExactPrereadFeature = false;
    private static final ArraySet<String> mExctprdExcApps = new ArraySet();
    private static final ArraySet<String> mExctprdIncActs = new ArraySet();
    private static final AtomicBoolean mRunning = new AtomicBoolean(false);
    private ExactPrereadWorkHandler mExactPrereadWorkHandler;
    private int mLastColdBootAppID = -1;
    private String mLastColdBootPkgName = null;
    private long mLastDisplayBeginTime = -1;
    private long mLastInputTime = 0;

    /* renamed from: com.android.server.rms.iaware.feature.AppQuickStartFeature$1 */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$android$rms$iaware$AwareConstant$ResourceType = new int[ResourceType.values().length];

        static {
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[ResourceType.RES_APP.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[ResourceType.RES_INPUT.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[ResourceType.RESOURCE_USERHABIT.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[ResourceType.RESOURCE_SHUTDOWN.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    private final class ExactPrereadWorkHandler extends Handler {
        private static final int INVALID_ID = -1;
        private static final String INVALID_NAME = "";
        private static final int INVALID_TYPE = -1;
        private static final int MAX_LAUNCH_TIMEOUT = 20000;
        private static final int MAX_PAUSECOLLECT_TIMEOUT = 20000;
        private static final int MAX_PAUSEDELAY_TIMEOUT = 3000;
        private static final int MAX_PAUSE_TIMEOUT = 10000;
        private static final int MIN_LAUNCH_TIMEOUT = 5000;
        private static final int MSG_DELAY_PAUSE_TIMEOUT = 300;
        private static final int MSG_LAUNCH_TIMEOUT = 100;
        private static final int MSG_PAUSE_COLLECT_TIMEOUT = 400;
        private static final int MSG_PAUSE_TIMEOUT = 200;
        private int mDelayPauseTimeout = 0;
        private int mLaunchTimeout = 10000;
        private int mPauseCollectTimeout = 8000;
        private int mPauseTimeout = 1000;
        private ExactPrereadWork mWork = new ExactPrereadWork(-1, -1, "", -1);

        private class ExactPrereadWork {
            public int mAppId;
            public String mOwnerName;
            public int mPid;
            public int mType;

            public ExactPrereadWork(int appId, int pid, String ownerName, int type) {
                this.mAppId = appId;
                this.mPid = pid;
                this.mOwnerName = ownerName;
                this.mType = type;
            }

            public void workReset() {
                this.mAppId = -1;
                this.mPid = -1;
                this.mOwnerName = "";
                this.mType = -1;
            }
        }

        public ExactPrereadWorkHandler(Long totalMemMb) {
            if (totalMemMb.longValue() > 4096) {
                this.mDelayPauseTimeout = 200;
                this.mPauseTimeout = 3000;
            } else if (totalMemMb.longValue() > 2048) {
                this.mDelayPauseTimeout = 0;
                this.mPauseTimeout = 2000;
            } else if (totalMemMb.longValue() > 1024) {
                this.mDelayPauseTimeout = 0;
                this.mPauseTimeout = 1500;
            } else {
                this.mDelayPauseTimeout = 0;
                this.mPauseTimeout = 1000;
            }
        }

        private void setWorkCollectTime(int launchTimeout, int collectTimeout, int pauseTimeout, int pauseDelay) {
            if (launchTimeout >= 0) {
                if (launchTimeout < MIN_LAUNCH_TIMEOUT) {
                    this.mLaunchTimeout = MIN_LAUNCH_TIMEOUT;
                } else if (launchTimeout > 20000) {
                    this.mLaunchTimeout = 20000;
                } else {
                    this.mLaunchTimeout = launchTimeout;
                }
            }
            if (collectTimeout >= 0) {
                if (collectTimeout > 20000) {
                    this.mPauseCollectTimeout = 20000;
                } else {
                    this.mPauseCollectTimeout = collectTimeout;
                }
            }
            if (pauseTimeout >= 0) {
                if (pauseTimeout > 10000) {
                    this.mPauseTimeout = 10000;
                } else {
                    this.mPauseTimeout = pauseTimeout;
                }
            }
            if (this.mPauseTimeout > this.mPauseCollectTimeout) {
                this.mPauseTimeout = this.mPauseCollectTimeout;
            }
            if (pauseDelay >= 0) {
                if (pauseDelay > 3000) {
                    this.mDelayPauseTimeout = 3000;
                } else {
                    this.mDelayPauseTimeout = pauseDelay;
                }
            }
            if (this.mDelayPauseTimeout > this.mPauseTimeout) {
                this.mDelayPauseTimeout = this.mPauseTimeout;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setWorkCollectTime, launchTimeout: ");
            stringBuilder.append(this.mLaunchTimeout);
            stringBuilder.append(", collectTimeout: ");
            stringBuilder.append(this.mPauseCollectTimeout);
            stringBuilder.append(", pauseTimeout: ");
            stringBuilder.append(this.mPauseTimeout);
            stringBuilder.append(", pauseDelay: ");
            stringBuilder.append(this.mDelayPauseTimeout);
            AwareLog.i("AppQuickStart", stringBuilder.toString());
        }

        private boolean hasValidWorkExisted() {
            if (this.mWork.mAppId == -1 || this.mWork.mPid == -1 || this.mWork.mType == -1 || this.mWork.mOwnerName.equals("")) {
                return false;
            }
            return true;
        }

        private boolean hasWorkExisted(int appId, String ownerName) {
            if (this.mWork.mAppId == appId && this.mWork.mOwnerName.equals(ownerName)) {
                return true;
            }
            return false;
        }

        private boolean hasPauseWorkExisted() {
            if (this.mWork.mType == 200 || this.mWork.mType == 400) {
                return true;
            }
            return false;
        }

        private void startCollectWork(int appId, int pid, String ownerName) {
            if (ownerName != null) {
                removeMessages(100);
                removeMessages(200);
                removeMessages(400);
                removeMessages(300);
                if (!hasWorkExisted(appId, ownerName)) {
                    stopCurrentWork();
                    this.mWork.mAppId = appId;
                    this.mWork.mOwnerName = ownerName;
                } else if (this.mWork.mType == 100 && this.mWork.mPid != pid) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("workingset: startCollectWork, curPid: ");
                    stringBuilder.append(pid);
                    stringBuilder.append(", oldPid: ");
                    stringBuilder.append(this.mWork.mPid);
                    AwareLog.d("AppQuickStart", stringBuilder.toString());
                    AppQuickStartFeature.this.exactPrereadSendCmd(MemoryConstant.MSG_WORKINGSET_TOUCHEDFILES_PAUSE_COLLECT, 0, this.mWork.mAppId, this.mWork.mPid, this.mWork.mOwnerName);
                }
                this.mWork.mPid = pid;
                this.mWork.mType = 100;
                Message message = Message.obtain();
                message.obj = this.mWork;
                message.what = 100;
                sendMessageDelayed(message, (long) this.mLaunchTimeout);
            }
        }

        private void sendPauseMsg() {
            this.mWork.mType = 200;
            Message message = Message.obtain();
            message.what = 200;
            sendMessageDelayed(message, (long) (this.mPauseTimeout - this.mDelayPauseTimeout));
        }

        private void sendDelayPauseMsg() {
            removeMessages(100);
            this.mWork.mType = 300;
            Message message = Message.obtain();
            message.what = 300;
            sendMessageDelayed(message, (long) this.mDelayPauseTimeout);
        }

        private void sendPauseCollectMsg() {
            this.mWork.mType = 400;
            Message message = Message.obtain();
            message.what = 400;
            sendMessageDelayed(message, (long) (this.mPauseCollectTimeout - this.mPauseTimeout));
        }

        private void stopCurrentWork() {
            if (hasValidWorkExisted()) {
                AppQuickStartFeature.this.exactPrereadSendCmd(MemoryConstant.MSG_WORKINGSET_TOUCHEDFILES_STOP, 0, this.mWork.mAppId, this.mWork.mPid, this.mWork.mOwnerName);
                this.mWork.workReset();
                removeMessages(100);
                removeMessages(200);
                removeMessages(400);
                removeMessages(300);
            }
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int i = msg.what;
            if (i == 100) {
                AppQuickStartFeature.this.exactPrereadSendCmd(MemoryConstant.MSG_WORKINGSET_TOUCHEDFILES_ABORT, 0, this.mWork.mAppId, this.mWork.mPid, this.mWork.mOwnerName);
                this.mWork.workReset();
            } else if (i == 200) {
                sendPauseCollectMsg();
                AppQuickStartFeature.this.exactPrereadSendCmd(MemoryConstant.MSG_WORKINGSET_TOUCHEDFILES_PAUSE_COLLECT, 0, this.mWork.mAppId, this.mWork.mPid, this.mWork.mOwnerName);
            } else if (i == 300) {
                sendPauseMsg();
                AppQuickStartFeature.this.exactPrereadSendCmd(MemoryConstant.MSG_WORKINGSET_TOUCHEDFILES_PAUSE, 0, this.mWork.mAppId, this.mWork.mPid, this.mWork.mOwnerName);
            } else if (i == 400) {
                AppQuickStartFeature.this.exactPrereadSendCmd(MemoryConstant.MSG_WORKINGSET_TOUCHEDFILES_STOP, 0, this.mWork.mAppId, this.mWork.mPid, this.mWork.mOwnerName);
                this.mWork.workReset();
            }
        }
    }

    public AppQuickStartFeature(Context context, FeatureType featureType, IRDataRegister dataRegister) {
        super(context, featureType, dataRegister);
        MemInfoReader minfo = new MemInfoReader();
        minfo.readMemInfo();
        Long totalMemMb = Long.valueOf(minfo.getTotalSize() / MemoryConstant.MB_SIZE);
        this.mExactPrereadWorkHandler = new ExactPrereadWorkHandler(totalMemMb);
        loadExctprdConfig(false, totalMemMb);
        loadExctprdConfig(true, totalMemMb);
    }

    public boolean enable() {
        AwareLog.i("AppQuickStart", "AppQuickStartFeature is a iaware3.0 feature, don't allow enable!");
        return false;
    }

    public boolean enableFeatureEx(int realVersion) {
        if (realVersion < 3) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("the min version of AppQuickStartFeature is 3, but current version is ");
            stringBuilder.append(realVersion);
            stringBuilder.append(", don't allow enable!");
            AwareLog.i("AppQuickStart", stringBuilder.toString());
            return false;
        }
        AwareLog.i("AppQuickStart", "AppQuickStartFeature enabled");
        subscribleEvents();
        enableAppQuickStart();
        return true;
    }

    public boolean disable() {
        AwareLog.i("AppQuickStart", "AppQuickStartFeature disabled");
        unSubscribeEvents();
        disableAppQuickStart();
        return true;
    }

    private static void enableAppQuickStart() {
        mRunning.set(true);
    }

    private static void disableAppQuickStart() {
        mRunning.set(false);
    }

    public boolean reportData(CollectData data) {
        boolean ret = false;
        if (!mRunning.get() || data == null) {
            AwareLog.e("AppQuickStart", "DataDispatch not start");
            return false;
        } else if (!mExactPrereadFeature) {
            return false;
        } else {
            long timestamp = data.getTimeStamp();
            AttrSegments attrSegments;
            switch (AnonymousClass1.$SwitchMap$android$rms$iaware$AwareConstant$ResourceType[ResourceType.getResourceType(data.getResId()).ordinal()]) {
                case 1:
                    attrSegments = parseCollectData(data);
                    if (attrSegments.isValid()) {
                        ret = appDataHandle(attrSegments.getEvent().intValue(), attrSegments);
                        break;
                    }
                    break;
                case 2:
                    attrSegments = parseCollectData(data);
                    if (attrSegments.isValid()) {
                        ret = inputDataHandle(timestamp, attrSegments.getEvent().intValue(), attrSegments);
                        break;
                    }
                    break;
                case 3:
                    Bundle bundle = data.getBundle();
                    if (bundle != null && 2 == bundle.getInt(AwareUserHabit.USERHABIT_INSTALL_APP_UPDATE)) {
                        String pkgName = bundle.getString(AwareUserHabit.USERHABIT_PACKAGE_NAME);
                        if (pkgName != null) {
                            clearRecordsOfPackage(UserHandle.getAppId(bundle.getInt("uid")), pkgName);
                        }
                        ret = true;
                        break;
                    }
                case 4:
                    exactPrereadSendCmd(MemoryConstant.MSG_WORKINGSET_TOUCHEDFILES_BACKUP, 0, 0, 0, "All");
                    ret = true;
                    break;
                default:
                    AwareLog.e("AppQuickStart", "Invalid ResourceType");
                    break;
            }
            return ret;
        }
    }

    private boolean appDataHandle(int event, AttrSegments attrSegments) {
        boolean ret = false;
        ArrayMap<String, String> appInfo = attrSegments.getSegment("calledApp");
        if (appInfo == null) {
            AwareLog.w("AppQuickStart", "appInfo is NULL");
            return false;
        }
        if (event == 15001) {
            ret = handleProcessLaunchBegin(appInfo);
        } else if (event == 15005) {
            ret = handleActivityBegin(appInfo);
        } else if (event == 15013) {
            ret = handleDisplayedBegin(appInfo);
        } else if (event == 85001) {
            ret = handleProcessLaunchFinish(appInfo);
        } else if (event == 85013) {
            ret = handleDisplayedFinish(appInfo);
        }
        return ret;
    }

    private AwareConfig getConfig(String featureName, String configName, boolean isCustConfig) {
        AwareConfig configList = null;
        if (featureName == null || featureName.equals("") || configName == null || configName.equals("")) {
            AwareLog.w("AppQuickStart", "featureName or configName is null");
            return null;
        }
        try {
            IBinder awareservice = IAwareCMSManager.getICMSManager();
            if (awareservice != null) {
                configList = isCustConfig ? IAwareCMSManager.getCustConfig(awareservice, featureName, configName) : IAwareCMSManager.getConfig(awareservice, featureName, configName);
            } else {
                AwareLog.w("AppQuickStart", "can not find service awareservice.");
            }
        } catch (RemoteException e) {
            AwareLog.e("AppQuickStart", "getConfig RemoteException!");
        }
        return configList;
    }

    private boolean checkRamSize(String ramSize, Long totalMemMb) {
        if (ramSize == null) {
            return false;
        }
        try {
            long ramSizeL = Long.parseLong(ramSize.trim());
            if (totalMemMb.longValue() > ramSizeL || totalMemMb.longValue() <= ramSizeL - 1024) {
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            AwareLog.e("AppQuickStart", "parse ramsize error!");
            return false;
        }
    }

    private void loadExctprdConfig(boolean isCustConfig, Long totalMemMb) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("loadExctprdConfig isCustConfig = ");
        stringBuilder.append(isCustConfig);
        AwareLog.i("AppQuickStart", stringBuilder.toString());
        AwareConfig configList = getConfig("AppQuickStart", AQS_EXCTPRD_NAME, isCustConfig);
        if (configList == null) {
            AwareLog.w("AppQuickStart", "loadExctprdConfig failure, configList is null!");
            return;
        }
        for (Item item : configList.getConfigList()) {
            if (item == null || item.getProperties() == null) {
                AwareLog.w("AppQuickStart", "loadExctprdConfig skip a item because it is null!");
            } else {
                Map<String, String> configPropertries = item.getProperties();
                String typeName = (String) configPropertries.get("type");
                if (typeName != null) {
                    Object obj = -1;
                    int hashCode = typeName.hashCode();
                    if (hashCode != -1805606060) {
                        if (hashCode != -1423802409) {
                            if (hashCode == 1943461398 && typeName.equals(AQS_EXCTPRD_INCLUDED_ACT)) {
                                obj = 2;
                            }
                        } else if (typeName.equals(AQS_EXCTPRD_EXCLUDED_APP)) {
                            obj = 1;
                        }
                    } else if (typeName.equals(AQS_EXCTPRD_SWITCH)) {
                        obj = null;
                    }
                    switch (obj) {
                        case null:
                            applyExctprdSwitchConfig(item);
                            break;
                        case 1:
                            applyExctprdExcludedAppsConfig(item);
                            break;
                        case 2:
                            applyExctprdIncludedActsConfig(item);
                            break;
                    }
                }
                String ramSize = (String) configPropertries.get("ramsize");
                if (checkRamSize(ramSize, totalMemMb)) {
                    applyExctprdCollectTimeConfig(item);
                } else {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("checkRamSize failure! ramSize: ");
                    stringBuilder2.append(ramSize);
                    stringBuilder2.append(" totalMemMb: ");
                    stringBuilder2.append(totalMemMb);
                    AwareLog.d("AppQuickStart", stringBuilder2.toString());
                }
            }
        }
    }

    private static void applyExctprdSwitchConfig(Item item) {
        for (SubItem subItem : item.getSubItemList()) {
            String itemName = subItem.getName();
            String itemValue = subItem.getValue();
            if (itemName != null) {
                if (itemValue != null) {
                    Object obj = -1;
                    try {
                        if (itemName.hashCode() == -1805606060 && itemName.equals(AQS_EXCTPRD_SWITCH)) {
                            obj = null;
                        }
                        if (obj != null) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("applyExctprdSwitchConfig no such configuration. ");
                            stringBuilder.append(itemName);
                            AwareLog.w("AppQuickStart", stringBuilder.toString());
                        } else {
                            boolean z = true;
                            if (Integer.parseInt(itemValue.trim()) != 1) {
                                z = false;
                            }
                            mExactPrereadFeature = z;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("applyExctprdSwitchConfig Switch = ");
                            stringBuilder2.append(itemValue);
                            AwareLog.i("AppQuickStart", stringBuilder2.toString());
                        }
                    } catch (NumberFormatException e) {
                        AwareLog.e("AppQuickStart", "parse applyExctprdSwitchConfig error!");
                    }
                }
            }
        }
    }

    private static void applyExctprdExcludedAppsConfig(Item item) {
        mExctprdExcApps.clear();
        for (SubItem subItem : item.getSubItemList()) {
            String itemName = subItem.getName();
            String itemValue = subItem.getValue();
            if (itemName != null) {
                if (itemValue != null) {
                    Object obj = -1;
                    try {
                        if (itemName.hashCode() == 870516780 && itemName.equals(AQS_EXCTPRD_APPNAME)) {
                            obj = null;
                        }
                        StringBuilder stringBuilder;
                        if (obj != null) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("applyExctprdExcludedAppsConfig no such configuration.");
                            stringBuilder.append(itemName);
                            AwareLog.w("AppQuickStart", stringBuilder.toString());
                        } else {
                            mExctprdExcApps.add(itemValue.trim());
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("applyExctprdExcludedAppsConfig AppName = ");
                            stringBuilder.append(itemValue);
                            AwareLog.i("AppQuickStart", stringBuilder.toString());
                        }
                    } catch (NumberFormatException e) {
                        AwareLog.e("AppQuickStart", "parse applyExctprdExcludedAppsConfig error!");
                    }
                }
            }
        }
    }

    private static void applyExctprdIncludedActsConfig(Item item) {
        mExctprdIncActs.clear();
        for (SubItem subItem : item.getSubItemList()) {
            String itemName = subItem.getName();
            String itemValue = subItem.getValue();
            if (itemName != null) {
                if (itemValue != null) {
                    Object obj = -1;
                    try {
                        if (itemName.hashCode() == 502031901 && itemName.equals(AQS_EXCTPRD_ACTNAME)) {
                            obj = null;
                        }
                        StringBuilder stringBuilder;
                        if (obj != null) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("applyExctprdIncludedActsConfig no such configuration. ");
                            stringBuilder.append(itemName);
                            AwareLog.w("AppQuickStart", stringBuilder.toString());
                        } else {
                            mExctprdIncActs.add(itemValue.trim());
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("applyExctprdIncludedActsConfig ActName = ");
                            stringBuilder.append(itemValue);
                            AwareLog.i("AppQuickStart", stringBuilder.toString());
                        }
                    } catch (NumberFormatException e) {
                        AwareLog.e("AppQuickStart", "parse applyExctprdIncludedActsConfig error!");
                    }
                }
            }
        }
    }

    private void applyExctprdCollectTimeConfig(Item item) {
        int launchTimeout = -1;
        int collectTimeout = -1;
        int pauseTimeout = -1;
        int delayPause = -1;
        for (SubItem subItem : item.getSubItemList()) {
            String itemName = subItem.getName();
            String itemValue = subItem.getValue();
            if (itemName != null && itemValue != null) {
                Object obj = -1;
                try {
                    int hashCode = itemName.hashCode();
                    if (hashCode != 590910123) {
                        if (hashCode != 1545794062) {
                            if (hashCode != 1920911693) {
                                if (hashCode == 1974045943 && itemName.equals(AQS_EXCTPRD_COLLECT_TIMEOUT)) {
                                    obj = 2;
                                }
                            } else if (itemName.equals(AQS_EXCTPRD_PAUSE_DELAY)) {
                                obj = null;
                            }
                        } else if (itemName.equals(AQS_EXCTPRD_LAUNCH_TIMEOUT)) {
                            obj = 3;
                        }
                    } else if (itemName.equals(AQS_EXCTPRD_PAUSE_TIMEOUT)) {
                        obj = 1;
                    }
                    StringBuilder stringBuilder;
                    switch (obj) {
                        case null:
                            delayPause = Integer.parseInt(itemValue.trim());
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("applyExctprdCollectTimeConfig PauseDelay = ");
                            stringBuilder.append(delayPause);
                            AwareLog.i("AppQuickStart", stringBuilder.toString());
                            break;
                        case 1:
                            pauseTimeout = Integer.parseInt(itemValue.trim());
                            if (delayPause <= pauseTimeout) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("applyExctprdCollectTimeConfig PauseTimeout = ");
                                stringBuilder.append(pauseTimeout);
                                AwareLog.i("AppQuickStart", stringBuilder.toString());
                                break;
                            }
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("applyExctprdCollectTimeConfig invalid parameters: PauseDelay=");
                            stringBuilder.append(delayPause);
                            stringBuilder.append(", ");
                            stringBuilder.append(AQS_EXCTPRD_PAUSE_TIMEOUT);
                            stringBuilder.append("=");
                            stringBuilder.append(pauseTimeout);
                            AwareLog.w("AppQuickStart", stringBuilder.toString());
                            return;
                        case 2:
                            collectTimeout = Integer.parseInt(itemValue.trim());
                            if (pauseTimeout <= collectTimeout && delayPause <= collectTimeout) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("applyExctprdCollectTimeConfig CollectTimeout = ");
                                stringBuilder.append(collectTimeout);
                                AwareLog.i("AppQuickStart", stringBuilder.toString());
                                break;
                            }
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("applyExctprdCollectTimeConfig invalid parameters: PauseDelay=");
                            stringBuilder.append(delayPause);
                            stringBuilder.append(", ");
                            stringBuilder.append(AQS_EXCTPRD_PAUSE_TIMEOUT);
                            stringBuilder.append("=");
                            stringBuilder.append(pauseTimeout);
                            stringBuilder.append(", ");
                            stringBuilder.append(AQS_EXCTPRD_COLLECT_TIMEOUT);
                            stringBuilder.append("=");
                            stringBuilder.append(collectTimeout);
                            AwareLog.w("AppQuickStart", stringBuilder.toString());
                            return;
                            break;
                        case 3:
                            launchTimeout = Integer.parseInt(itemValue.trim());
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("applyExctprdCollectTimeConfig LaunchTimeout = ");
                            stringBuilder.append(launchTimeout);
                            AwareLog.i("AppQuickStart", stringBuilder.toString());
                            break;
                        default:
                            String str = "AppQuickStart";
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("applyExctprdCollectTimeConfig no such configuration. ");
                            stringBuilder.append(itemName);
                            AwareLog.w(str, stringBuilder.toString());
                            break;
                    }
                } catch (NumberFormatException e) {
                    AwareLog.e("AppQuickStart", "parse applyExctprdCollectTimeConfig error!");
                }
            } else {
                return;
            }
        }
        this.mExactPrereadWorkHandler.setWorkCollectTime(launchTimeout, collectTimeout, pauseTimeout, delayPause);
    }

    private void clearRecordsOfPackage(int appId, String packageName) {
        exactPrereadSendCmd(MemoryConstant.MSG_WORKINGSET_TOUCHEDFILES_CLEAN, 0, appId, 0, packageName);
        int actSize = mExctprdIncActs.size();
        for (int idx = 0; idx < actSize; idx++) {
            String actName = (String) mExctprdIncActs.valueAt(idx);
            if (actName != null) {
                ComponentName cpName = ComponentName.unflattenFromString(actName);
                if (cpName != null && packageName.equals(cpName.getPackageName())) {
                    exactPrereadSendCmd(MemoryConstant.MSG_WORKINGSET_TOUCHEDFILES_CLEAN, 0, appId, 0, actName.replace('/', '.'));
                }
            }
        }
    }

    private boolean handleActivityBegin(ArrayMap<String, String> appInfo) {
        String appName = (String) appInfo.get("packageName");
        String activityName = (String) appInfo.get("activityName");
        String processName = (String) appInfo.get("processName");
        try {
            int uid = Integer.parseInt((String) appInfo.get("uid"));
            try {
                int appId = UserHandle.getAppId(uid);
                tryStopPreviousOwnerWorks(appId, appName);
                if (appName == null) {
                    return false;
                }
                if ((HwActivityManager.isProcessExistPidsSelfLocked(processName, uid) ^ 1) && !mExctprdExcApps.contains(appName)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("workingset: handleActivityBegin, appId: ");
                    stringBuilder.append(appId);
                    stringBuilder.append(", pkgName: ");
                    stringBuilder.append(appName);
                    stringBuilder.append(", className: ");
                    stringBuilder.append(activityName);
                    AwareLog.d("AppQuickStart", stringBuilder.toString());
                    exactPrereadSendCmd(MemoryConstant.MSG_WORKINGSET_TOUCHEDFILES_PREREAD, 1, appId, 0, appName);
                    this.mLastColdBootAppID = appId;
                    this.mLastColdBootPkgName = appName;
                }
                return true;
            } catch (NumberFormatException e) {
                NumberFormatException numberFormatException = e;
                int i = uid;
                AwareLog.e("AppQuickStart", "uid is not right");
                return false;
            }
        } catch (NumberFormatException e2) {
            AwareLog.e("AppQuickStart", "uid is not right");
            return false;
        }
    }

    private boolean handleProcessLaunchBegin(ArrayMap<String, String> appInfo) {
        if ("activity".equals((String) appInfo.get("launchMode"))) {
            try {
                String packageName = (String) appInfo.get("packageName");
                int appId = UserHandle.getAppId(Integer.parseInt((String) appInfo.get("uid")));
                tryStopPreviousOwnerWorks(appId, packageName);
                if (!(packageName == null || mExctprdExcApps.contains(packageName))) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("workingset: handleProcessLaunchBegin, appId: ");
                    stringBuilder.append(appId);
                    stringBuilder.append(", packageName: ");
                    stringBuilder.append(packageName);
                    AwareLog.d("AppQuickStart", stringBuilder.toString());
                    exactPrereadSendCmd(MemoryConstant.MSG_WORKINGSET_TOUCHEDFILES_PREREAD, 1, appId, 0, packageName);
                    this.mLastColdBootAppID = appId;
                    this.mLastColdBootPkgName = packageName;
                }
            } catch (NumberFormatException e) {
                AwareLog.e("AppQuickStart", "handleProcessLaunchBegin get info failed!");
                return false;
            }
        }
        return true;
    }

    private boolean handleProcessLaunchFinish(ArrayMap<String, String> appInfo) {
        if ("activity".equals((String) appInfo.get("launchMode"))) {
            try {
                String packageName = (String) appInfo.get("packageName");
                int uid = Integer.parseInt((String) appInfo.get("uid"));
                int pid = Integer.parseInt((String) appInfo.get("pid"));
                int appId = UserHandle.getAppId(uid);
                tryStopPreviousOwnerWorks(appId, packageName);
                if (!(packageName == null || mExctprdExcApps.contains(packageName))) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("workingset: handleProcessLaunchFinish, appId: ");
                    stringBuilder.append(appId);
                    stringBuilder.append(", pid: ");
                    stringBuilder.append(pid);
                    stringBuilder.append(", packageName: ");
                    stringBuilder.append(packageName);
                    AwareLog.d("AppQuickStart", stringBuilder.toString());
                    this.mExactPrereadWorkHandler.startCollectWork(appId, pid, packageName);
                    exactPrereadSendCmd(350, 1, appId, pid, packageName);
                    this.mLastColdBootAppID = appId;
                    this.mLastColdBootPkgName = packageName;
                    this.mLastDisplayBeginTime = SystemClock.uptimeMillis();
                }
            } catch (NumberFormatException e) {
                AwareLog.e("AppQuickStart", "handleProcessLaunchFinish get info failed!");
                return false;
            }
        }
        return true;
    }

    private void tryStopPreviousOwnerWorks(int appId, String appName) {
        if (this.mLastColdBootAppID != -1 && this.mLastColdBootPkgName != null) {
            if (this.mLastInputTime > this.mLastDisplayBeginTime || appId != this.mLastColdBootAppID || !this.mLastColdBootPkgName.equals(appName)) {
                this.mExactPrereadWorkHandler.stopCurrentWork();
                this.mLastColdBootAppID = -1;
                this.mLastColdBootPkgName = null;
            }
        }
    }

    private boolean handleDisplayedBegin(ArrayMap<String, String> appInfo) {
        try {
            String activityName = (String) appInfo.get("activityName");
            String packageName = null;
            if (activityName != null) {
                ComponentName cpName = ComponentName.unflattenFromString(activityName);
                if (cpName != null) {
                    packageName = cpName.getPackageName();
                }
            }
            int uid = Integer.parseInt((String) appInfo.get("uid"));
            int pid = Integer.parseInt((String) appInfo.get("pid"));
            int appId = UserHandle.getAppId(uid);
            if (activityName == null || packageName == null) {
                return false;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("workingset: handleDisplayedBegin, appId: ");
            stringBuilder.append(appId);
            stringBuilder.append(", pid: ");
            stringBuilder.append(pid);
            stringBuilder.append(", activityName: ");
            stringBuilder.append(activityName);
            AwareLog.d("AppQuickStart", stringBuilder.toString());
            if (this.mExactPrereadWorkHandler.hasWorkExisted(appId, packageName)) {
                if (this.mExactPrereadWorkHandler.hasPauseWorkExisted()) {
                    exactPrereadSendCmd(350, 1, appId, pid, packageName);
                }
                this.mExactPrereadWorkHandler.startCollectWork(appId, pid, packageName);
                this.mLastDisplayBeginTime = SystemClock.uptimeMillis();
            } else if (mExctprdIncActs.contains(activityName)) {
                String changedActivityName = activityName.replace('/', '.');
                this.mExactPrereadWorkHandler.startCollectWork(appId, pid, changedActivityName);
                exactPrereadSendCmd(350, 0, appId, pid, changedActivityName);
            }
            return true;
        } catch (NumberFormatException e) {
            AwareLog.e("AppQuickStart", "handleDisplayedBegin get pid or time failed");
            return false;
        }
    }

    private boolean handleDisplayedFinish(ArrayMap<String, String> appInfo) {
        try {
            String activityName = (String) appInfo.get("activityName");
            String packageName = null;
            if (activityName != null) {
                ComponentName cpName = ComponentName.unflattenFromString(activityName);
                if (cpName != null) {
                    packageName = cpName.getPackageName();
                }
            }
            int uid = Integer.parseInt((String) appInfo.get("uid"));
            int pid = Integer.parseInt((String) appInfo.get("pid"));
            uid = UserHandle.getAppId(uid);
            if (activityName == null || packageName == null) {
                return false;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("workingset: handleDisplayedFinish, appId: ");
            stringBuilder.append(uid);
            stringBuilder.append(", pid: ");
            stringBuilder.append(pid);
            stringBuilder.append(", activityName: ");
            stringBuilder.append(activityName);
            AwareLog.d("AppQuickStart", stringBuilder.toString());
            if (this.mExactPrereadWorkHandler.hasWorkExisted(uid, packageName)) {
                this.mExactPrereadWorkHandler.sendDelayPauseMsg();
            } else {
                if (this.mExactPrereadWorkHandler.hasWorkExisted(uid, activityName.replace('/', '.'))) {
                    this.mExactPrereadWorkHandler.sendDelayPauseMsg();
                }
            }
            return true;
        } catch (NumberFormatException e) {
            AwareLog.e("AppQuickStart", "handleDisplayedFinish get pid or time failed");
            return false;
        }
    }

    private boolean inputDataHandle(long timestamp, int event, AttrSegments attrSegments) {
        if (event == IDisplayEngineService.DE_ACTION_PG_BROWSER_FRONT || event == 80001) {
            this.mLastInputTime = timestamp;
            return true;
        }
        AwareLog.w("AppQuickStart", "Input event invalid");
        return false;
    }

    private AttrSegments parseCollectData(CollectData data) {
        String eventData = data.getData();
        Builder builder = new Builder();
        builder.addCollectData(eventData);
        return builder.build();
    }

    public boolean configUpdate() {
        return true;
    }

    private void subscribleEvents() {
        if (this.mIRDataRegister != null) {
            this.mIRDataRegister.subscribeData(ResourceType.RES_APP, this.mFeatureType);
            this.mIRDataRegister.subscribeData(ResourceType.RES_DEV_STATUS, this.mFeatureType);
            this.mIRDataRegister.subscribeData(ResourceType.RES_INPUT, this.mFeatureType);
            this.mIRDataRegister.subscribeData(ResourceType.RESOURCE_USERHABIT, this.mFeatureType);
            this.mIRDataRegister.subscribeData(ResourceType.RESOURCE_SHUTDOWN, this.mFeatureType);
        }
    }

    private void unSubscribeEvents() {
        if (this.mIRDataRegister != null) {
            this.mIRDataRegister.unSubscribeData(ResourceType.RES_APP, this.mFeatureType);
            this.mIRDataRegister.unSubscribeData(ResourceType.RES_DEV_STATUS, this.mFeatureType);
            this.mIRDataRegister.unSubscribeData(ResourceType.RES_INPUT, this.mFeatureType);
            this.mIRDataRegister.unSubscribeData(ResourceType.RESOURCE_USERHABIT, this.mFeatureType);
            this.mIRDataRegister.unSubscribeData(ResourceType.RESOURCE_SHUTDOWN, this.mFeatureType);
        }
    }

    public static boolean isExactPrereadFeatureEnable() {
        return mExactPrereadFeature && mRunning.get();
    }

    private void exactPrereadSendCmd(int action, int coldboot, int appId, int pid, String ownerName) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("workingset, action: ");
        stringBuilder.append(action);
        stringBuilder.append(", coldboot:");
        stringBuilder.append(coldboot);
        stringBuilder.append(", appId: ");
        stringBuilder.append(appId);
        stringBuilder.append(", pid: ");
        stringBuilder.append(pid);
        stringBuilder.append(", ");
        stringBuilder.append(ownerName);
        AwareLog.d("AppQuickStart", stringBuilder.toString());
        try {
            byte[] nameBytes = ownerName.getBytes("UTF-8");
            if (nameBytes.length <= 0 || nameBytes.length > 256) {
                AwareLog.w("AppQuickStart", "ComponentName is invalid!");
                return;
            }
            ByteBuffer buffer = ByteBuffer.allocate(16 + nameBytes.length);
            buffer.putInt(action);
            buffer.putInt(coldboot);
            buffer.putInt(appId);
            buffer.putInt(pid);
            buffer.put(nameBytes);
            IAwaredConnection.getInstance().sendPacket(buffer.array());
        } catch (UnsupportedEncodingException e) {
            AwareLog.w("AppQuickStart", "UnsupportedEncodingException: transform ComponentName failed!");
        }
    }
}
