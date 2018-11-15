package com.android.server.rms.iaware;

import android.appwidget.HwAppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.rms.iaware.AwareConstant.FeatureType;
import android.rms.iaware.AwareConstant.ResourceType;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import android.rms.iaware.DumpData;
import android.rms.iaware.IAwareCMSManager;
import android.rms.iaware.IAwaredConnection;
import android.rms.iaware.IReportDataCallback;
import android.rms.iaware.StatisticsData;
import android.util.Xml;
import com.android.internal.os.SomeArgs;
import com.android.server.rms.algorithm.AwareUserHabit;
import com.android.server.rms.iaware.feature.RFeature;
import com.huawei.android.app.HwActivityManager;
import com.huawei.android.os.HwPowerManager;
import com.huawei.android.view.HwWindowManager;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;

public class RFeatureManager implements IRDataRegister {
    private static final String CUST_PATH_DIR = "/data/cust/xml";
    private static final int FEATURE_MAX_DUMP_SIZE = 5;
    public static final int FEATURE_STATUS_DISABLE = 0;
    public static final int FEATURE_STATUS_ENABLE = 1;
    private static final String FEATURE_SWITCH_FILE_NAME = "iAwareFeatureSwitch.xml";
    private static int IAWARE_VERSION_DEFAULT = 1;
    private static String KEY_IAWARE_VERSION = "iAwareVersion";
    private static final int MSG_CONFIG_UPDATE = 3;
    private static final int MSG_ENABLE_DISABLE_FEATURE = 1;
    private static final int MSG_INIT_FEATURE = 0;
    private static final int MSG_REPORT_DATA = 2;
    private static final int MSG_REPORT_DATA_CALLBACK = 4;
    private static final int MSG_UPDATE_ARG_CONFIG = 0;
    private static final int MSG_UPDATE_ARG_CUST_CONFIG = 1;
    private static final String TAG = "RFeatureManager";
    private static final int UNKNOWN_FEATURE_STATUS = -1;
    private static final String USER_HABIT_RECV_TRAIN_COMPLETED_PERMISSION = "com.huawei.iaware.userhabit.USERHABIT_PERMISSION";
    private static final String USER_HABIT_TRAIN_COMPLETED_ACTION = "com.huawei.iaware.userhabit.TRAIN_COMPLETED";
    private static final String XML_TAG_FEATURE_ID = "featureid";
    private static final String XML_TAG_FEATURE_SWITCH = "switch";
    private static final String XML_TAG_ITEM = "item";
    private static final String mAPPMngFeatureName = "com.android.server.rms.iaware.feature.APPMngFeature";
    private static final FeatureType mAPPMngFeatureType = FeatureType.FEATURE_APPMNG;
    private AwareAWSIMonitorCallback mAwsiCallback = null;
    private Context mContext = null;
    private final String[] mFeatureNames = new String[]{"com.android.server.rms.iaware.feature.AppRecgFeature", "com.android.server.rms.iaware.cpu.CPUFeature", "com.android.server.rms.iaware.srms.ResourceFeature", "com.android.server.rms.iaware.feature.APPHiberFeature", "com.android.server.rms.iaware.feature.MemoryFeature", "com.android.server.rms.iaware.feature.IOFeature", "com.android.server.rms.iaware.srms.BroadcastFeature", "com.android.server.rms.iaware.feature.VsyncFeature", "com.android.server.rms.iaware.feature.MemoryFeature2", "com.android.server.rms.iaware.srms.AppStartupFeature", "com.android.server.rms.iaware.feature.AppFakeFeature", "com.android.server.rms.iaware.feature.NetworkFeature", "com.android.server.rms.iaware.feature.APPFreezeFeature", "com.android.server.rms.iaware.feature.APPIoLimitFeature", "com.android.server.rms.iaware.srms.AppCleanupFeature", "com.android.server.rms.iaware.feature.DevSchedFeatureRT", "com.android.server.rms.iaware.feature.AlarmManagerFeature", "com.android.server.rms.iaware.feature.BlitParallelFeature", "com.android.server.rms.iaware.srms.BroadcastExFeature", "com.android.server.rms.iaware.feature.SysLoadFeature", "com.android.server.rms.iaware.feature.AppQuickStartFeature", "com.android.server.rms.iaware.feature.NetworkTcpNodelayFeature", "com.android.server.rms.iaware.feature.StartWindowFeature", "com.android.server.rms.iaware.feature.PreloadResourceFeature"};
    private final FeatureType[] mFeatureTypes = new FeatureType[]{FeatureType.FEATURE_INTELLI_REC, FeatureType.FEATURE_CPU, FeatureType.FEATURE_RESOURCE, FeatureType.FEATURE_APPHIBER, FeatureType.FEATURE_MEMORY, FeatureType.FEATURE_IO, FeatureType.FEATURE_BROADCAST, FeatureType.FEATURE_VSYNC, FeatureType.FEATURE_MEMORY2, FeatureType.FEATURE_APPSTARTUP, FeatureType.FEATURE_RECG_FAKEACTIVITY, FeatureType.FEATURE_NETWORK_TCP, FeatureType.FEATURE_APPFREEZE, FeatureType.FEATURE_IO_LIMIT, FeatureType.FEATURE_APPCLEANUP, FeatureType.FEATURE_DEVSCHED, FeatureType.FEATURE_ALARM_MANAGER, FeatureType.FEATURE_BLIT_PARALLEL, FeatureType.FEATURE_BROADCASTEX, FeatureType.FEATURE_SYSLOAD, FeatureType.FEATURE_APP_QUICKSTART, FeatureType.FEATURE_NETWORK_TCP_NODELAY, FeatureType.FEATURE_STARTWINDOW, FeatureType.PRELOADRESOURCE};
    private boolean mIAwareEnabled = false;
    private int mIAwareVersion = IAWARE_VERSION_DEFAULT;
    private XmlPullParser mParser = null;
    private IAwareDeviceStateReceiver mReceiver = null;
    private boolean mRegisterAWMSuccess = false;
    private boolean mRegisterWMSuccess = false;
    private Map<FeatureType, FeatureWrapper> mRegisteredFeatures = null;
    private ReportedDataHandler mReportedDataHandler;
    private Map<ResourceType, ArrayList<FeatureType>> mSubscribeDataMap = null;
    private final int mSubscribedFeatureTypeNum;
    private final FeatureType[] mSubscribedFeatureTypes;
    private int mWMRetryTime = 50;
    private AwareWmsMonitorCallback mWmsCallback = null;

    private class CallbackRegistration implements Runnable {
        private CallbackRegistration() {
        }

        public void run() {
            RFeatureManager rFeatureManager = RFeatureManager.this;
            boolean z = true;
            boolean z2 = RFeatureManager.this.mRegisterWMSuccess || HwWindowManager.registerWMMonitorCallback(RFeatureManager.this.mWmsCallback);
            rFeatureManager.mRegisterWMSuccess = z2;
            rFeatureManager = RFeatureManager.this;
            if (!(RFeatureManager.this.mRegisterAWMSuccess || HwAppWidgetManager.registerAWSIMonitorCallback(RFeatureManager.this.mAwsiCallback))) {
                z = false;
            }
            rFeatureManager.mRegisterAWMSuccess = z;
            if (!(RFeatureManager.this.mRegisterAWMSuccess && RFeatureManager.this.mRegisterWMSuccess) && RFeatureManager.this.mWMRetryTime = RFeatureManager.this.mWMRetryTime - 1 > 0) {
                RFeatureManager.this.mReportedDataHandler.postDelayed(this, 200);
            }
        }
    }

    private static class FeatureWrapper {
        private boolean mFeatureEnabled;
        private RFeature mFeatureInstance;
        private int mFeatureVersion;

        public FeatureWrapper(RFeature instance, boolean status, int version) {
            this.mFeatureInstance = instance;
            this.mFeatureEnabled = status;
            this.mFeatureVersion = version;
        }

        public void setFeatureEnabled(boolean enable) {
            this.mFeatureEnabled = enable;
        }

        public boolean getFeatureEnabled() {
            return this.mFeatureEnabled;
        }

        public int getFeatureVersion() {
            return this.mFeatureVersion;
        }

        public RFeature getFeatureInstance() {
            return this.mFeatureInstance;
        }
    }

    private class IAwareDeviceStateReceiver extends BroadcastReceiver {
        private IAwareDeviceStateReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (context == null || intent == null) {
                AwareLog.e(RFeatureManager.TAG, "BroadcastReceiver error parameters!");
                return;
            }
            String action = intent.getAction();
            int resIDHabitStat;
            long curtime;
            String pkgName;
            Bundle bdl;
            if ("android.intent.action.SCREEN_ON".equals(action)) {
                RFeatureManager.this.reportData(new CollectData(ResourceType.getReousrceId(ResourceType.RESOURCE_SCREEN_ON), System.currentTimeMillis(), action));
            } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                RFeatureManager.this.reportData(new CollectData(ResourceType.getReousrceId(ResourceType.RESOURCE_SCREEN_OFF), System.currentTimeMillis(), action));
            } else if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                RFeatureManager.this.reportData(new CollectData(ResourceType.getReousrceId(ResourceType.RESOURCE_BOOT_COMPLETED), System.currentTimeMillis(), action));
            } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                if (intent.getData() != null) {
                    resIDHabitStat = ResourceType.getReousrceId(ResourceType.RESOURCE_USERHABIT);
                    curtime = System.currentTimeMillis();
                    pkgName = intent.getData().getSchemeSpecificPart();
                    int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                    Bundle bdl2 = new Bundle();
                    bdl2.putString(AwareUserHabit.USERHABIT_PACKAGE_NAME, pkgName);
                    bdl2.putInt("uid", uid);
                    bdl2.putInt(AwareUserHabit.USERHABIT_INSTALL_APP_UPDATE, 2);
                    RFeatureManager.this.reportData(new CollectData(resIDHabitStat, curtime, bdl2));
                }
            } else if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                if (intent.getData() != null) {
                    resIDHabitStat = ResourceType.getReousrceId(ResourceType.RESOURCE_USERHABIT);
                    curtime = System.currentTimeMillis();
                    pkgName = intent.getData().getSchemeSpecificPart();
                    bdl = new Bundle();
                    bdl.putString(AwareUserHabit.USERHABIT_PACKAGE_NAME, pkgName);
                    bdl.putInt(AwareUserHabit.USERHABIT_INSTALL_APP_UPDATE, 1);
                    RFeatureManager.this.reportData(new CollectData(resIDHabitStat, curtime, bdl));
                }
            } else if (RFeatureManager.USER_HABIT_TRAIN_COMPLETED_ACTION.equals(action)) {
                resIDHabitStat = ResourceType.getReousrceId(ResourceType.RESOURCE_USERHABIT);
                curtime = System.currentTimeMillis();
                Bundle bdl3 = new Bundle();
                bdl3.putInt(AwareUserHabit.USERHABIT_INSTALL_APP_UPDATE, 3);
                RFeatureManager.this.reportData(new CollectData(resIDHabitStat, curtime, bdl3));
            } else if ("android.intent.action.USER_PRESENT".equals(action)) {
                RFeatureManager.this.reportData(new CollectData(ResourceType.getReousrceId(ResourceType.RESOURCE_USER_PRESENT), System.currentTimeMillis(), action));
            } else if ("android.intent.action.USER_REMOVED".equals(action)) {
                resIDHabitStat = ResourceType.getReousrceId(ResourceType.RESOURCE_APPASSOC);
                curtime = System.currentTimeMillis();
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                bdl = new Bundle();
                bdl.putInt("relationType", 29);
                bdl.putInt("userid", userId);
                RFeatureManager.this.reportData(new CollectData(resIDHabitStat, curtime, bdl));
            } else if ("android.intent.action.ACTION_SHUTDOWN".equals(action)) {
                RFeatureManager.this.reportData(new CollectData(ResourceType.getReousrceId(ResourceType.RESOURCE_SHUTDOWN), System.currentTimeMillis(), action));
            }
        }
    }

    private final class ReportedDataHandler extends Handler {
        public ReportedDataHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            String str;
            StringBuilder stringBuilder;
            IBinder awareservice;
            switch (msg.what) {
                case 0:
                    registerFeatures(msg.getData());
                    awareservice = IAwareCMSManager.getICMSManager();
                    if (awareservice == null) {
                        AwareLog.e(RFeatureManager.TAG, "get IAwareCMSService failed.");
                        break;
                    }
                    try {
                        RFeatureManager.this.mIAwareEnabled = IAwareCMSManager.isIAwareEnabled(awareservice);
                        if (RFeatureManager.this.mIAwareEnabled) {
                            registerAPPMngFeature(1);
                        } else {
                            registerAPPMngFeature(0);
                        }
                        if (RFeatureManager.this.mIAwareEnabled && RFeatureManager.this.mReceiver == null) {
                            RFeatureManager.this.mReceiver = new IAwareDeviceStateReceiver();
                            RFeatureManager.this.registerRDABroadcastReceiver();
                            AwareLog.d(RFeatureManager.TAG, "register RDA broadcast Receiver");
                            break;
                        }
                    } catch (RemoteException e) {
                        str = RFeatureManager.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("call isIAwareEnabled failed.");
                        stringBuilder.append(e.getMessage());
                        AwareLog.e(str, stringBuilder.toString());
                        break;
                    }
                case 1:
                    FeatureType featureType = FeatureType.getFeatureType(msg.arg2);
                    String str2 = RFeatureManager.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("handler message featureType:");
                    stringBuilder2.append(featureType.name());
                    stringBuilder2.append("  status:");
                    stringBuilder2.append(msg.arg1);
                    AwareLog.d(str2, stringBuilder2.toString());
                    if (RFeatureManager.this.mRegisteredFeatures.containsKey(featureType)) {
                        boolean status = msg.arg1 == 1;
                        FeatureWrapper feature = (FeatureWrapper) RFeatureManager.this.mRegisteredFeatures.get(featureType);
                        if (feature != null && feature.getFeatureInstance() != null) {
                            if (feature.getFeatureEnabled() != status) {
                                controlFeature(feature.getFeatureInstance(), status, feature.getFeatureVersion(), featureType, false);
                                feature.setFeatureEnabled(status);
                                awareservice = IAwareCMSManager.getICMSManager();
                                if (awareservice == null) {
                                    AwareLog.e(RFeatureManager.TAG, "get IAwareCMSService failed.");
                                    break;
                                }
                                try {
                                    RFeatureManager.this.mIAwareEnabled = IAwareCMSManager.isIAwareEnabled(awareservice);
                                    if (!RFeatureManager.this.mIAwareEnabled || RFeatureManager.this.mReceiver != null) {
                                        if (!(RFeatureManager.this.mIAwareEnabled || RFeatureManager.this.mReceiver == null)) {
                                            enableAPPMngFeature(false);
                                            RFeatureManager.this.mContext.unregisterReceiver(RFeatureManager.this.mReceiver);
                                            AwareLog.d(RFeatureManager.TAG, "unregister RDA broadcast Receiver");
                                            RFeatureManager.this.mReceiver = null;
                                            RFeatureManager.this.mReportedDataHandler.removeMessages(2);
                                            IAwaredConnection.getInstance().destroy();
                                            break;
                                        }
                                    }
                                    enableAPPMngFeature(true);
                                    RFeatureManager.this.mReceiver = new IAwareDeviceStateReceiver();
                                    RFeatureManager.this.registerRDABroadcastReceiver();
                                    AwareLog.d(RFeatureManager.TAG, "register RDA broadcast Receiver");
                                    break;
                                } catch (RemoteException e2) {
                                    str = RFeatureManager.TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("call isIAwareEnabled failed.");
                                    stringBuilder.append(e2.getMessage());
                                    AwareLog.e(str, stringBuilder.toString());
                                    break;
                                }
                            }
                            return;
                        }
                        String str3 = RFeatureManager.TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("handleMessage ENABLE_DISABLE_FEATURE feature null: ");
                        stringBuilder3.append(featureType.name());
                        AwareLog.e(str3, stringBuilder3.toString());
                        return;
                    }
                    break;
                case 2:
                    deliveryDataToFeatures(msg.obj, null);
                    break;
                case 3:
                    notifyUpdate(msg.arg1);
                    break;
                case 4:
                    SomeArgs args = msg.obj;
                    CollectData collectData = args.arg1;
                    IReportDataCallback callback = args.arg2;
                    args.recycle();
                    deliveryDataToFeatures(collectData, callback);
                    break;
            }
        }

        private void enableAPPMngFeature(boolean enabled) {
            String str = RFeatureManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Enable APPMng Feature enable = ");
            stringBuilder.append(enabled);
            AwareLog.d(str, stringBuilder.toString());
            FeatureWrapper feature = (FeatureWrapper) RFeatureManager.this.mRegisteredFeatures.get(RFeatureManager.mAPPMngFeatureType);
            if (feature == null || feature.getFeatureInstance() == null) {
                AwareLog.e(RFeatureManager.TAG, "enableAPPMngFeature feature null!");
            } else if (feature.getFeatureEnabled() != enabled) {
                if (enabled) {
                    feature.getFeatureInstance().enable();
                } else {
                    feature.getFeatureInstance().disable();
                }
                feature.setFeatureEnabled(enabled);
            }
        }

        private void registerFeatures(Bundle bundle) {
            int featureNum = RFeatureManager.this.mFeatureNames.length;
            if (featureNum == 0) {
                AwareLog.e(RFeatureManager.TAG, "There is no feature will be registered.");
                return;
            }
            RFeatureManager.this.mIAwareVersion = getIAwareVersion(bundle);
            FeatureWrapper wrapper = null;
            for (int index = 0; index < featureNum; index++) {
                int[] featureInfo = RFeatureManager.this.getFeatureInfoFromBundle(bundle, RFeatureManager.this.mFeatureTypes[index].name());
                wrapper = createFeatureWrapper(RFeatureManager.this.mFeatureNames[index], RFeatureManager.this.mFeatureTypes[index], featureInfo[0], featureInfo[1]);
                if (wrapper != null) {
                    RFeatureManager.this.mRegisteredFeatures.put(RFeatureManager.this.mFeatureTypes[index], wrapper);
                }
            }
        }

        private int getIAwareVersion(Bundle bundle) {
            if (bundle == null) {
                return RFeatureManager.IAWARE_VERSION_DEFAULT;
            }
            return bundle.getInt(RFeatureManager.KEY_IAWARE_VERSION, RFeatureManager.IAWARE_VERSION_DEFAULT);
        }

        private void registerAPPMngFeature(int featureStatus) {
            FeatureWrapper wrapper = createFeatureWrapper(RFeatureManager.mAPPMngFeatureName, RFeatureManager.mAPPMngFeatureType, featureStatus, RFeatureManager.IAWARE_VERSION_DEFAULT);
            if (wrapper != null) {
                RFeatureManager.this.mRegisteredFeatures.put(RFeatureManager.mAPPMngFeatureType, wrapper);
            }
        }

        private void controlFeature(RFeature feature, boolean enable, int version, FeatureType featureType, boolean isInit) {
            String str = RFeatureManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("iAware2.0: feature id is ");
            stringBuilder.append(featureType.ordinal());
            AwareLog.d(str, stringBuilder.toString());
            boolean useOld = version <= RFeatureManager.IAWARE_VERSION_DEFAULT;
            if (isInit && useOld) {
                AwareLog.d(RFeatureManager.TAG, "iAware2.0: controlFeature use default init!");
                iAware1InitFeature(feature, enable, featureType);
                return;
            }
            newControlFeature(feature, enable, version);
        }

        private void iAware1InitFeature(RFeature feature, boolean enable, FeatureType featureType) {
            if (enable) {
                feature.enable();
            } else if (featureType == FeatureType.FEATURE_RESOURCE) {
                feature.disable();
            }
        }

        private void newControlFeature(RFeature feature, boolean enable, int version) {
            String str = RFeatureManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("iAware2.0: newControlFeature feature version is ");
            stringBuilder.append(version);
            AwareLog.d(str, stringBuilder.toString());
            if (!enable) {
                AwareLog.d(RFeatureManager.TAG, "iAware2.0: disable!");
                feature.disable();
            } else if (version > RFeatureManager.IAWARE_VERSION_DEFAULT) {
                int realVersion = Math.min(RFeatureManager.this.mIAwareVersion, version);
                String str2 = RFeatureManager.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("iAware2.0: use enable Ex! realVersion is ");
                stringBuilder2.append(realVersion);
                AwareLog.d(str2, stringBuilder2.toString());
                feature.enableFeatureEx(realVersion);
            } else {
                AwareLog.d(RFeatureManager.TAG, "iAware2.0: use old enable!");
                feature.enable();
            }
        }

        private FeatureWrapper createFeatureWrapper(String featureName, FeatureType featureType, int featureStatus, int featureVersion) {
            int i;
            FeatureType featureType2 = featureType;
            int i2 = featureStatus;
            String str = RFeatureManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("createFeatureWrapper name = ");
            String str2 = featureName;
            stringBuilder.append(str2);
            stringBuilder.append(" type = ");
            stringBuilder.append(featureType2);
            AwareLog.d(str, stringBuilder.toString());
            try {
                Constructor<?>[] featureConstructor = Class.forName(str2).getConstructors();
                try {
                    str = RFeatureManager.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("createFeatureWrapper constructor = ");
                    boolean z = false;
                    stringBuilder2.append(featureConstructor[0].getName());
                    AwareLog.d(str, stringBuilder2.toString());
                    RFeature feature = (RFeature) featureConstructor[0].newInstance(new Object[]{RFeatureManager.this.mContext, featureType2, RFeatureManager.this});
                    String str3 = RFeatureManager.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("createFeatureWrapper featureStatus = ");
                    stringBuilder.append(i2);
                    AwareLog.d(str3, stringBuilder.toString());
                    if (i2 == 1) {
                        z = true;
                    }
                    boolean status = z;
                    controlFeature(feature, status, featureVersion, featureType2, true);
                    return new FeatureWrapper(feature, status, featureVersion);
                } catch (IllegalArgumentException e) {
                    i = featureVersion;
                    AwareLog.e(RFeatureManager.TAG, "createFeatureWrapper newInstance IllegalArgumentException");
                    return null;
                } catch (IllegalAccessException e2) {
                    i = featureVersion;
                    AwareLog.e(RFeatureManager.TAG, "createFeatureWrapper newInstance IllegalAccessException");
                    return null;
                } catch (InstantiationException e3) {
                    i = featureVersion;
                    AwareLog.e(RFeatureManager.TAG, "createFeatureWrapper newInstance InstantiationException");
                    return null;
                } catch (InvocationTargetException e4) {
                    i = featureVersion;
                    AwareLog.e(RFeatureManager.TAG, "createFeatureWrapper newInstance InvocationTargetException");
                    return null;
                }
            } catch (ClassNotFoundException err) {
                i = featureVersion;
                ClassNotFoundException classNotFoundException = err;
                AwareLog.e(RFeatureManager.TAG, "createFeatureWrapper forName ClassNotFoundException");
                return null;
            }
        }

        /* JADX WARNING: Missing block: B:19:0x004e, code:
            r3 = 0;
     */
        /* JADX WARNING: Missing block: B:20:0x004f, code:
            if (r3 >= r2) goto L_0x0087;
     */
        /* JADX WARNING: Missing block: B:21:0x0051, code:
            r5 = (com.android.server.rms.iaware.RFeatureManager.FeatureWrapper) com.android.server.rms.iaware.RFeatureManager.access$1100(r10.this$0).get(com.android.server.rms.iaware.RFeatureManager.access$2100(r10.this$0)[r3]);
     */
        /* JADX WARNING: Missing block: B:22:0x0065, code:
            if (r5 == null) goto L_0x0084;
     */
        /* JADX WARNING: Missing block: B:24:0x006b, code:
            if (r5.getFeatureInstance() == null) goto L_0x0084;
     */
        /* JADX WARNING: Missing block: B:26:0x0071, code:
            if (r5.getFeatureEnabled() == false) goto L_0x0084;
     */
        /* JADX WARNING: Missing block: B:27:0x0073, code:
            if (r12 != null) goto L_0x007d;
     */
        /* JADX WARNING: Missing block: B:28:0x0075, code:
            r5.getFeatureInstance().reportData(r11);
     */
        /* JADX WARNING: Missing block: B:29:0x007d, code:
            r5.getFeatureInstance().reportDataWithCallback(r11, r12);
     */
        /* JADX WARNING: Missing block: B:30:0x0084, code:
            r3 = r3 + 1;
     */
        /* JADX WARNING: Missing block: B:31:0x0087, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void deliveryDataToFeatures(CollectData data, IReportDataCallback callback) {
            Throwable th;
            ResourceType resType = ResourceType.getResourceType(data.getResId());
            int index = 0;
            synchronized (RFeatureManager.this.mSubscribeDataMap) {
                try {
                    List<FeatureType> currlist = (List) RFeatureManager.this.mSubscribeDataMap.get(resType);
                    if (currlist != null) {
                        int subcribedFeatureNum = currlist.size();
                        if (subcribedFeatureNum < RFeatureManager.this.mSubscribedFeatureTypeNum) {
                            for (FeatureType feature : currlist) {
                                int index2 = index + 1;
                                try {
                                    RFeatureManager.this.mSubscribedFeatureTypes[index] = feature;
                                    index = index2;
                                } catch (Throwable th2) {
                                    th = th2;
                                    index = index2;
                                }
                            }
                        } else {
                            AwareLog.e(RFeatureManager.TAG, "deliveryDataToFeatures subscribed too much features!");
                            return;
                        }
                    }
                    String str = RFeatureManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("deliveryDataToFeatures no subscribed features resType = ");
                    stringBuilder.append(resType.name());
                    AwareLog.d(str, stringBuilder.toString());
                } catch (Throwable th3) {
                    th = th3;
                    throw th;
                }
            }
        }

        private void notifyUpdate(int type) {
            if (type == 0) {
                notifyConfigUpdate();
            } else if (type == 1) {
                notifyCustConfigUpdate();
            } else {
                AwareLog.w(RFeatureManager.TAG, "notify type wrong!");
            }
        }

        private void notifyConfigUpdate() {
            AwareLog.d(RFeatureManager.TAG, "notifyConfigUpdate");
            for (FeatureWrapper feature : RFeatureManager.this.mRegisteredFeatures.values()) {
                if (feature == null || feature.getFeatureInstance() == null) {
                    AwareLog.e(RFeatureManager.TAG, "notifyConfigUpdate feature null!");
                } else if (!feature.getFeatureInstance().configUpdate()) {
                    AwareLog.e(RFeatureManager.TAG, "notifyConfigUpdate return false");
                }
            }
        }

        private void notifyCustConfigUpdate() {
            AwareLog.d(RFeatureManager.TAG, "notifyCustConfigUpdate");
            for (FeatureWrapper feature : RFeatureManager.this.mRegisteredFeatures.values()) {
                if (feature == null || feature.getFeatureInstance() == null) {
                    AwareLog.e(RFeatureManager.TAG, "notifyCustConfigUpdate feature null!");
                } else {
                    boolean notifyResult = feature.getFeatureInstance().custConfigUpdate();
                    String str = RFeatureManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(feature.getFeatureInstance());
                    stringBuilder.append(" notifyCustConfigUpdate return ");
                    stringBuilder.append(notifyResult);
                    AwareLog.d(str, stringBuilder.toString());
                }
            }
        }
    }

    public boolean subscribeData(ResourceType resType, FeatureType featureType) {
        if (resType == null || featureType == null) {
            AwareLog.e(TAG, "subscribeData: error parameters!");
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("subscribeData resType = ");
        stringBuilder.append(resType.name());
        stringBuilder.append(" featureType = ");
        stringBuilder.append(featureType.name());
        AwareLog.d(str, stringBuilder.toString());
        synchronized (this.mSubscribeDataMap) {
            ArrayList<FeatureType> currlist = (ArrayList) this.mSubscribeDataMap.get(resType);
            if (currlist == null) {
                currlist = new ArrayList();
                this.mSubscribeDataMap.put(resType, currlist);
            }
            if (!currlist.contains(featureType)) {
                currlist.add(featureType);
            }
        }
        return true;
    }

    public boolean unSubscribeData(ResourceType resType, FeatureType featureType) {
        if (resType == null || featureType == null) {
            AwareLog.e(TAG, "unSubscribeData: error parameters!");
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unSubscribeData resType = ");
        stringBuilder.append(resType.name());
        stringBuilder.append(" featureType = ");
        stringBuilder.append(featureType.name());
        AwareLog.d(str, stringBuilder.toString());
        synchronized (this.mSubscribeDataMap) {
            List<FeatureType> currlist = (List) this.mSubscribeDataMap.get(resType);
            if (currlist != null) {
                currlist.remove(featureType);
                if (currlist.size() == 0) {
                    this.mSubscribeDataMap.remove(resType);
                }
            }
        }
        return true;
    }

    public FeatureType[] getFeatureTypes() {
        return (FeatureType[]) this.mFeatureTypes.clone();
    }

    public int getFeatureStatus(int featureid) {
        FeatureType type = FeatureType.getFeatureType(featureid);
        if (type == FeatureType.FEATURE_INVALIDE_TYPE) {
            AwareLog.e(TAG, "getFeatureStatus invalid feature type");
            return 0;
        }
        FeatureWrapper wrapper = (FeatureWrapper) this.mRegisteredFeatures.get(type);
        if (wrapper == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getFeatureStatus feature wrapper null, featureid = ");
            stringBuilder.append(featureid);
            AwareLog.e(str, stringBuilder.toString());
            return 0;
        } else if (wrapper.getFeatureEnabled()) {
            return 1;
        } else {
            return 0;
        }
    }

    public void enableFeature(int type) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("enableFeature type = ");
        stringBuilder.append(type);
        AwareLog.d(str, stringBuilder.toString());
        Message enableMessage = Message.obtain();
        enableMessage.what = 1;
        enableMessage.arg1 = 1;
        enableMessage.arg2 = type;
        this.mReportedDataHandler.sendMessage(enableMessage);
    }

    public void disableFeature(int type) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("disableFeature type = ");
        stringBuilder.append(type);
        AwareLog.d(str, stringBuilder.toString());
        Message disableMessage = Message.obtain();
        disableMessage.what = 1;
        disableMessage.arg1 = 0;
        disableMessage.arg2 = type;
        this.mReportedDataHandler.sendMessage(disableMessage);
    }

    public void reportData(CollectData data) {
        Message dataMessage = Message.obtain();
        dataMessage.what = 2;
        dataMessage.obj = data;
        this.mReportedDataHandler.sendMessage(dataMessage);
    }

    public void reportDataWithCallback(CollectData data, IReportDataCallback callback) {
        AwareLog.d(TAG, "reportDataWithCallback");
        Message dataMessage = Message.obtain();
        dataMessage.what = 4;
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = data;
        args.arg2 = callback;
        dataMessage.obj = args;
        this.mReportedDataHandler.sendMessage(dataMessage);
    }

    public void init(Bundle bundle) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("init bundle = ");
        stringBuilder.append(bundle);
        AwareLog.d(str, stringBuilder.toString());
        Message initMessage = Message.obtain();
        initMessage.what = 0;
        initMessage.setData(bundle);
        this.mReportedDataHandler.sendMessage(initMessage);
    }

    public boolean isResourceNeeded(ResourceType resourceType) {
        return this.mIAwareEnabled;
    }

    public int isFeatureEnabled(int featureId) {
        if (FeatureType.getFeatureType(featureId) != FeatureType.FEATURE_INVALIDE_TYPE) {
            return parseFeatureSwitchFormCustFile(featureId);
        }
        AwareLog.e(TAG, "Enabling error feature id!");
        return -1;
    }

    public boolean configUpdate() {
        AwareLog.d(TAG, "configUpdate ");
        Message msg = Message.obtain();
        msg.what = 3;
        msg.arg1 = 0;
        return this.mReportedDataHandler.sendMessage(msg);
    }

    public boolean custConfigUpdate() {
        AwareLog.d(TAG, "custConfigUpdate ");
        Message msg = Message.obtain();
        msg.what = 3;
        msg.arg1 = 1;
        return this.mReportedDataHandler.sendMessage(msg);
    }

    public int getDumpData(int time, List<DumpData> dumpData) {
        AwareLog.d(TAG, "getDumpData");
        for (FeatureWrapper v : this.mRegisteredFeatures.values()) {
            if (v != null && v.getFeatureInstance() != null) {
                ArrayList<DumpData> featureDumpData = v.getFeatureInstance().getDumpData(time);
                if (featureDumpData != null) {
                    int dumpDataSize = featureDumpData.size();
                    int index = 0;
                    if (dumpDataSize > 5) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("RDA getDumpData more than 5 items, size = ");
                        stringBuilder.append(dumpDataSize);
                        stringBuilder.append(" , id = ");
                        stringBuilder.append(((DumpData) featureDumpData.get(0)).getFeatureId());
                        AwareLog.e(str, stringBuilder.toString());
                        dumpDataSize = 5;
                    }
                    while (true) {
                        int index2 = index;
                        if (index2 >= dumpDataSize) {
                            break;
                        }
                        dumpData.add((DumpData) featureDumpData.get(index2));
                        index = index2 + 1;
                    }
                }
            } else {
                AwareLog.e(TAG, "getDumpData feature null!");
            }
        }
        return dumpData.size();
    }

    public int getStatisticsData(List<StatisticsData> statisticsData) {
        AwareLog.d(TAG, "getStatisticsData");
        for (FeatureWrapper v : this.mRegisteredFeatures.values()) {
            if (v == null || v.getFeatureInstance() == null) {
                AwareLog.e(TAG, "getStatisticsData feature null!");
            } else {
                ArrayList<StatisticsData> featureStatisticsData = v.getFeatureInstance().getStatisticsData();
                if (featureStatisticsData != null) {
                    statisticsData.addAll(featureStatisticsData);
                }
            }
        }
        return statisticsData.size();
    }

    public String saveBigData(int featureId, boolean clear) {
        AwareLog.d(TAG, "rt saveBigData");
        FeatureWrapper fw = (FeatureWrapper) this.mRegisteredFeatures.get(FeatureType.getFeatureType(featureId));
        if (fw == null) {
            AwareLog.d(TAG, "null FeatureWrapper");
            return null;
        }
        RFeature v = fw.getFeatureInstance();
        if (v != null) {
            return v.saveBigData(clear);
        }
        AwareLog.d(TAG, "null RFeature");
        return null;
    }

    public String fetchBigDataByVersion(int iawareVer, int featureId, boolean beta, boolean clear) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("fetchBigDataByVersion iVer = ");
        stringBuilder.append(iawareVer);
        stringBuilder.append(", fId = ");
        stringBuilder.append(featureId);
        stringBuilder.append(", beta = ");
        stringBuilder.append(beta);
        AwareLog.d(str, stringBuilder.toString());
        FeatureWrapper featureWrapper = (FeatureWrapper) this.mRegisteredFeatures.get(FeatureType.getFeatureType(featureId));
        RFeature feature = featureWrapper != null ? featureWrapper.getFeatureInstance() : null;
        if (feature != null) {
            return feature.getBigDataByVersion(iawareVer, beta, clear);
        }
        return null;
    }

    public String fetchDFTDataByVersion(int iawareVer, int featureId, boolean beta, boolean clear, boolean betaEncode) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("fetchDFTDataByVersion iVer = ");
        stringBuilder.append(iawareVer);
        stringBuilder.append(", fId = ");
        stringBuilder.append(featureId);
        stringBuilder.append(", beta = ");
        stringBuilder.append(beta);
        stringBuilder.append(", betaEncode=");
        stringBuilder.append(betaEncode);
        AwareLog.d(str, stringBuilder.toString());
        FeatureWrapper featureWrapper = (FeatureWrapper) this.mRegisteredFeatures.get(FeatureType.getFeatureType(featureId));
        RFeature feature = featureWrapper != null ? featureWrapper.getFeatureInstance() : null;
        if (feature != null) {
            return feature.getDFTDataByVersion(iawareVer, beta, clear, betaEncode);
        }
        return null;
    }

    public RFeatureManager(Context context, HandlerThread handlerThread) {
        AwareLog.d(TAG, "RFeatureManager created");
        this.mContext = context;
        this.mRegisteredFeatures = new HashMap();
        this.mSubscribeDataMap = new HashMap();
        this.mReportedDataHandler = new ReportedDataHandler(handlerThread.getLooper());
        this.mSubscribedFeatureTypeNum = this.mFeatureTypes.length + 1;
        this.mSubscribedFeatureTypes = new FeatureType[this.mSubscribedFeatureTypeNum];
        this.mParser = Xml.newPullParser();
        HwActivityManager.registerDAMonitorCallback(new AwareAmsMonitorCallback());
        this.mWmsCallback = new AwareWmsMonitorCallback();
        this.mAwsiCallback = new AwareAWSIMonitorCallback();
        this.mReportedDataHandler.post(new CallbackRegistration());
        HwPowerManager.registerPowerMonitorCallback(new AwarePowerMonitorCallback());
    }

    public RFeature getRegisteredFeature(FeatureType type) {
        if (!this.mRegisteredFeatures.containsKey(type)) {
            return null;
        }
        FeatureWrapper feature = (FeatureWrapper) this.mRegisteredFeatures.get(type);
        if (feature != null) {
            return feature.getFeatureInstance();
        }
        AwareLog.e(TAG, "getRegisteredFeature feature null!");
        return null;
    }

    private void registerRDABroadcastReceiver() {
        IntentFilter deviceStates = new IntentFilter();
        deviceStates.addAction("android.intent.action.SCREEN_ON");
        deviceStates.addAction("android.intent.action.SCREEN_OFF");
        deviceStates.setPriority(1000);
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, deviceStates, null, null);
        IntentFilter bootStates = new IntentFilter();
        bootStates.addAction("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, bootStates, null, null);
        IntentFilter appStates = new IntentFilter();
        appStates.addAction("android.intent.action.PACKAGE_ADDED");
        appStates.addAction("android.intent.action.PACKAGE_REMOVED");
        appStates.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, appStates, null, null);
        IntentFilter completedTrain = new IntentFilter();
        completedTrain.addAction(USER_HABIT_TRAIN_COMPLETED_ACTION);
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, completedTrain, USER_HABIT_RECV_TRAIN_COMPLETED_PERMISSION, null);
        IntentFilter presentFilter = new IntentFilter();
        presentFilter.addAction("android.intent.action.USER_PRESENT");
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, presentFilter, null, null);
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, userFilter, null, null);
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, new IntentFilter("android.intent.action.ACTION_SHUTDOWN"), null, null);
    }

    public int[] getFeatureInfoFromBundle(Bundle bundle, String key) {
        int[] info = bundle.getIntArray(key);
        if (info != null && info.length == 2) {
            return info;
        }
        return new int[]{0, IAWARE_VERSION_DEFAULT};
    }

    private int parseFeatureSwitchFormCustFile(int featureId) {
        StringBuilder stringBuilder;
        InputStream is = null;
        XmlPullParser parser = null;
        String tagName;
        try {
            File custConfigFile = loadCustFeatureSwitchFile();
            if (custConfigFile.exists()) {
                is = new FileInputStream(custConfigFile);
                parser = this.mParser;
                parser.setInput(is, StandardCharsets.UTF_8.name());
            }
            if (parser == null) {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        AwareLog.e(TAG, "close file input stream fail!!");
                    }
                }
                return -1;
            }
            boolean enterItemTag = false;
            Map<String, String> configItem = null;
            for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                tagName = parser.getName();
                if (eventType == 2) {
                    if (XML_TAG_ITEM.equals(tagName)) {
                        enterItemTag = true;
                        configItem = new HashMap();
                    } else if (tagName != null && enterItemTag) {
                        String value = parser.nextText();
                        if (configItem != null) {
                            configItem.put(tagName, value);
                        }
                    }
                } else if (eventType == 3 && XML_TAG_ITEM.equals(tagName)) {
                    AwareLog.d(TAG, "exit item");
                    enterItemTag = false;
                    if (configItem != null && ((String) configItem.get(XML_TAG_FEATURE_ID)).equals(Integer.toString(featureId))) {
                        int parseInt = Integer.parseInt((String) configItem.get("switch"));
                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException e2) {
                                AwareLog.e(TAG, "close file input stream fail!!");
                            }
                        }
                        return parseInt;
                    }
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e3) {
                    AwareLog.e(TAG, "close file input stream fail!!");
                }
            }
            return -1;
        } catch (IOException ioe) {
            tagName = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("read xml failed, error:");
            stringBuilder.append(ioe.getMessage());
            AwareLog.e(tagName, stringBuilder.toString());
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e4) {
                    AwareLog.e(TAG, "close file input stream fail!!");
                }
            }
            return -1;
        } catch (Exception e5) {
            tagName = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("read xml failed, error:");
            stringBuilder.append(e5.getMessage());
            AwareLog.e(tagName, stringBuilder.toString());
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e6) {
                    AwareLog.e(TAG, "close file input stream fail!!");
                }
            }
            return -1;
        } catch (Throwable th) {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e7) {
                    AwareLog.e(TAG, "close file input stream fail!!");
                }
            }
        }
    }

    File loadCustFeatureSwitchFile() {
        try {
            File cfg = HwCfgFilePolicy.getCfgFile("xml/iAwareFeatureSwitch.xml", 0);
            if (cfg != null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("cust switch file path is ");
                stringBuilder.append(cfg.getAbsolutePath());
                AwareLog.d(str, stringBuilder.toString());
                return cfg;
            }
        } catch (NoClassDefFoundError e) {
            AwareLog.e(TAG, "loadCustFeatureSwitchFile NoClassDefFoundError : HwCfgFilePolicy ");
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(CUST_PATH_DIR);
        stringBuilder2.append(File.separator);
        stringBuilder2.append(FEATURE_SWITCH_FILE_NAME);
        return new File(stringBuilder2.toString());
    }
}
