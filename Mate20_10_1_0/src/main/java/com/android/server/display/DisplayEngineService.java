package com.android.server.display;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Xml;
import com.android.server.display.DisplayEffectMonitor;
import com.android.server.display.DisplayEngineService;
import com.android.server.display.HwBrightnessSceneRecognition;
import com.android.server.display.HwLightSensorController;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.multiwin.HwMultiWinConstants;
import com.android.server.rms.iaware.hiber.constant.AppHibernateCst;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.huawei.android.fsm.HwFoldScreenManagerEx;
import com.huawei.android.pgmng.plug.PowerKit;
import com.huawei.displayengine.DeLog;
import com.huawei.displayengine.DisplayEngineDbManager;
import com.huawei.displayengine.DisplayEngineManager;
import com.huawei.displayengine.IDisplayEngineCallback;
import com.huawei.displayengine.IDisplayEngineService;
import com.huawei.displayengine.IDisplayEngineServiceEx;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class DisplayEngineService extends IDisplayEngineServiceEx.Stub implements HwLightSensorController.LightSensorCallbacks {
    private static final String ACTION_FACTORY_GMP = "com.android.server.display.FACTORY_GMP";
    private static final int BINDER_REBUILD_COUNT_MAX = 10;
    private static final int COLOR_TEMP_MONITOR_SAMPLING_INTERVAL_MS = 8000;
    private static final int COLOR_TEMP_VALID_LUX_THRESHOLD = 50;
    private static final int DE_ACTION_IAWARE_APP_TYPE_OTHERS = 255;
    private static final int DE_ACTION_IAWARE_APP_VIDEO_END = 1003;
    private static final int DE_ACTION_IAWARE_APP_VIDEO_START = 1002;
    private static final String DISPLAY_ENGINE_PERMISSION = "com.huawei.permission.ACCESS_DISPLAY_ENGINE";
    private static final int FACTORY_GMP_OFF = 0;
    private static final int FACTORY_GMP_ON = 1;
    private static final int FILM_FILTER_RANGE_MAX = 100;
    private static final String KEY_COLOR_MODE = "color_mode_switch";
    private static final String KEY_DC_BRIGHTNESS = "hw_dc_brightness_dimming_switch";
    private static final String KEY_GLOBAL_READ = "hw_ebook_mode_switch";
    private static final String KEY_NATURAL_TONE = "hw_natural_tone_display_switch";
    private static final String KEY_READING_MODE_SWITCH = "hw_reading_mode_display_switch";
    private static final String KEY_USER_PREFERENCE_TRAINING_TIMESTAMP = "hw_brightness_training_timestamp";
    private static final int LIGHT_SENSOR_RATE_MILLS = 300;
    private static final int PANEL_BUFFER_SIZE = 128;
    private static final int READING_TYPE = 6;
    private static final int REBUILD_BINDER_WAIT_TIME = 800;
    private static final int RETURN_PARAMETER_INVALID = -2;
    private static final int RETURN_PARAMETER_SUCCESS = 1;
    private static final String SR_CONTROL_XML_FILE = "/display/effect/displayengine/SR_control.xml";
    private static final String TAG = "DE J DES";
    private static final String TAG_FACTORY_GMP_STATE = "state";
    private static final long THREAD_KEEP_ALIVE_TIME = 30;
    private static final int THREAD_POOL_SIZE = 2;
    private static final Set<Integer> sSetDataCheckPermissionList = new HashSet<Integer>() {
        /* class com.android.server.display.DisplayEngineService.AnonymousClass2 */

        {
            add(5);
            add(4);
            add(10);
            add(9);
            add(11);
            add(7);
            add(13);
        }
    };
    private static final Set<Integer> sSetSceneCheckPermissionList = new HashSet<Integer>() {
        /* class com.android.server.display.DisplayEngineService.AnonymousClass1 */

        {
            add(10);
            add(13);
            add(15);
            add(11);
            add(30);
            add(32);
            add(34);
            add(24);
            add(12);
            add(33);
            add(39);
            add(40);
            add(18);
            add(20);
            add(21);
            add(26);
            add(28);
            add(29);
            add(31);
            add(35);
            add(36);
            add(38);
            add(25);
            add(48);
            add(41);
            add(42);
            add(44);
            add(45);
            add(46);
            add(49);
        }
    };
    private ContentObserver mAutoBrightnessAdjObserver;
    private int mBinderRebuildCount = 0;
    /* access modifiers changed from: private */
    public boolean mBootComplete;
    /* access modifiers changed from: private */
    public int mChargeLevelThreshold = 50;
    /* access modifiers changed from: private */
    public final Context mContext;
    private final DisplayBroadcastReceiver mDisplayBroadcastReceiver;
    private DisplayEffectMonitor mDisplayEffectMonitor;
    /* access modifiers changed from: private */
    public final List<IDisplayEngineCallback> mDisplayEngineCallbacks = Collections.synchronizedList(new ArrayList());
    private final IDisplayEngineCallback mDisplayEngineNativeCallback = new IDisplayEngineCallback.Stub() {
        /* class com.android.server.display.DisplayEngineService.AnonymousClass3 */

        @Override // com.huawei.displayengine.IDisplayEngineCallback
        public void onEvent(int event, int extra) {
            DeLog.v(DisplayEngineService.TAG, "event=" + event + " extra=" + extra);
            if (event == 1) {
                DeLog.d(DisplayEngineService.TAG, "frame rate=" + extra);
                synchronized (DisplayEngineService.this.mDisplayEngineCallbacks) {
                    DisplayEngineService.this.mDisplayEngineCallbacks.forEach(new Consumer(event, extra) {
                        /* class com.android.server.display.$$Lambda$DisplayEngineService$3$6xMpRGFOwGh12re08Kpwx1ofmyc */
                        private final /* synthetic */ int f$0;
                        private final /* synthetic */ int f$1;

                        {
                            this.f$0 = r1;
                            this.f$1 = r2;
                        }

                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            DisplayEngineService.AnonymousClass3.lambda$onEvent$0(this.f$0, this.f$1, (IDisplayEngineCallback) obj);
                        }
                    });
                }
            }
        }

        static /* synthetic */ void lambda$onEvent$0(int event, int extra, IDisplayEngineCallback callback) {
            try {
                callback.onEvent(event, extra);
            } catch (RemoteException e) {
                DeLog.w(DisplayEngineService.TAG, "Failed to notify callbacks: " + e.getMessage());
            }
        }

        @Override // com.huawei.displayengine.IDisplayEngineCallback
        public void onEventWithData(int event, PersistableBundle data) {
        }
    };
    /* access modifiers changed from: private */
    public DisplayEngineManager mDisplayManager;
    private FeatureObservers mFeatureObservers = new FeatureObservers(this.mHandler);
    private int mFilmFilterVersion = 0;
    private String mFilmFilters;
    private FoldableStateListenerForCompensation mFoldableStateCallback;
    private Handler mHandler = new Handler();
    /* access modifiers changed from: private */
    public final HwBrightnessSceneRecognition mHwBrightnessSceneRecognition;
    private volatile boolean mIsBinderBuilding;
    /* access modifiers changed from: private */
    public volatile boolean mIsBrightnessTrainingAborting;
    /* access modifiers changed from: private */
    public volatile boolean mIsBrightnessTrainingRunning;
    /* access modifiers changed from: private */
    public boolean mIsFactoryGmpOn;
    /* access modifiers changed from: private */
    public volatile boolean mIsTrainingTriggeredSinceLastScreenOff;
    private long mLastAmbientColorTempToMonitorTime;
    private final HwLightSensorController mLightSensorController;
    private boolean mLightSensorEnable;
    private final Object mLockBinderBuilding = new Object();
    private final Object mLockService = new Object();
    /* access modifiers changed from: private */
    public long mMinimumTrainingIntervalMillis = 57600000;
    private final MotionReceiver mMotionReceiver;
    private volatile IDisplayEngineService mNativeService;
    private boolean mNeedPkgNameFromPg;
    /* access modifiers changed from: private */
    public int mNewDragNumThreshold = 1;
    private PowerKit.Sink mPgListener;
    /* access modifiers changed from: private */
    public volatile boolean mScreenOn;
    private SensorManager mSensorManager;
    /* access modifiers changed from: private */
    public ThreadPoolExecutor mThreadPool = new ThreadPoolExecutor(2, 2, THREAD_KEEP_ALIVE_TIME, TimeUnit.SECONDS, new LinkedBlockingQueue());

    public DisplayEngineService(Context context) {
        this.mContext = context;
        HwLightSensorController controller = null;
        Object obj = this.mContext.getSystemService("sensor");
        if (obj instanceof SensorManager) {
            this.mSensorManager = (SensorManager) obj;
            controller = new HwLightSensorController(this.mContext, this, this.mSensorManager, 300);
        } else {
            DeLog.e(TAG, "Failed to get SensorManager:sensor");
        }
        this.mLightSensorController = controller;
        this.mHwBrightnessSceneRecognition = new HwBrightnessSceneRecognition(this.mContext);
        this.mDisplayBroadcastReceiver = new DisplayBroadcastReceiver();
        this.mMotionReceiver = new MotionReceiver();
        getConfigParam();
        this.mDisplayEffectMonitor = DisplayEffectMonitor.getInstance(context);
        if (this.mHwBrightnessSceneRecognition.isFeatureEnable(HwBrightnessSceneRecognition.FeatureTag.TAG_PERSONALIZED_CURVE)) {
            this.mDisplayManager = new DisplayEngineManager(this.mContext);
        }
        registerFoldableStateCallback();
        registerDisplayEngineNativeCallback();
        initFeatureSwitch();
    }

    private IDisplayEngineService getNativeService() throws RemoteException {
        if (this.mNativeService == null) {
            synchronized (this.mLockService) {
                if (this.mNativeService == null) {
                    buildBinder();
                }
            }
        }
        if (this.mNativeService != null) {
            return this.mNativeService;
        }
        if (this.mBinderRebuildCount < 10) {
            throw new RemoteException("Try to rebuild binder " + this.mBinderRebuildCount + " times.");
        }
        throw new RemoteException("binder rebuilding failed!");
    }

    private void buildBinder() {
        IBinder binder = ServiceManager.getService("DisplayEngineService");
        if (binder != null) {
            this.mNativeService = IDisplayEngineService.Stub.asInterface(binder);
            return;
        }
        this.mNativeService = null;
        DeLog.w(TAG, "binder is null!");
    }

    /* access modifiers changed from: private */
    /* renamed from: rebuildBinder */
    public void lambda$rebuildBinderDelayed$0$DisplayEngineService() {
        DeLog.i(TAG, "wait some time to rebuild binder...");
        SystemClock.sleep(800);
        DeLog.i(TAG, "rebuild binder...");
        synchronized (this.mLockService) {
            buildBinder();
            if (this.mNativeService != null) {
                DeLog.i(TAG, "rebuild binder success.");
                if (this.mScreenOn) {
                    setScene(10, 16);
                }
            } else {
                DeLog.i(TAG, "rebuild binder failed!");
                this.mBinderRebuildCount++;
            }
        }
        synchronized (this.mLockBinderBuilding) {
            if (this.mBinderRebuildCount < 10) {
                this.mIsBinderBuilding = false;
            }
        }
    }

    private void rebuildBinderDelayed() {
        if (!this.mIsBinderBuilding) {
            synchronized (this.mLockBinderBuilding) {
                if (!this.mIsBinderBuilding) {
                    this.mThreadPool.execute(new Runnable() {
                        /* class com.android.server.display.$$Lambda$DisplayEngineService$70ZqGREGfzvGv7rE1gh4erI6Do */

                        public final void run() {
                            DisplayEngineService.this.lambda$rebuildBinderDelayed$0$DisplayEngineService();
                        }
                    });
                    this.mIsBinderBuilding = true;
                }
            }
        }
    }

    public final int getSupported(int feature) {
        if (feature == 36) {
            try {
                return isFeatureGameDisableAutoBrightnessModeSupport(feature);
            } catch (RemoteException e) {
                DeLog.e(TAG, "getSupported(" + feature + ") has remote exception:" + e.getMessage());
                rebuildBinderDelayed();
                return 0;
            }
        } else if (feature == 30) {
            DeLog.i(TAG, "Film filter Supported type is " + this.mFilmFilterVersion);
            return this.mFilmFilterVersion;
        } else {
            IDisplayEngineService service = getNativeService();
            if (service != null) {
                return service.getSupported(feature);
            }
            return 0;
        }
    }

    public int setScene(int scene, int action) {
        int ret = -1;
        try {
            if (!setSceneHasPermission(scene, action)) {
                return -1;
            }
            boolean z = true;
            if (scene == 49) {
                if (action != 16) {
                    z = false;
                }
                return setGameDisableAutoBrightnessModeStatus(scene, z);
            }
            IDisplayEngineService service = getNativeService();
            if (service == null) {
                return -1;
            }
            ret = service.setScene(scene, action);
            if (scene == 24 && this.mHwBrightnessSceneRecognition != null && this.mHwBrightnessSceneRecognition.isEnable()) {
                HwBrightnessSceneRecognition hwBrightnessSceneRecognition = this.mHwBrightnessSceneRecognition;
                if (action != 16) {
                    z = false;
                }
                hwBrightnessSceneRecognition.notifyScreenStatus(z);
            }
            return ret;
        } catch (RemoteException e) {
            DeLog.e(TAG, "setScene(" + scene + ", " + action + ") has remote exception:" + e.getMessage());
            rebuildBinderDelayed();
        }
    }

    public void registerCallback(IDisplayEngineCallback cb) {
        if (cb == null) {
            DeLog.e(TAG, "Invalid input: cb is null!");
        } else {
            this.mDisplayEngineCallbacks.add(cb);
        }
    }

    public void unregisterCallback(IDisplayEngineCallback cb) {
        if (cb == null) {
            DeLog.e(TAG, "Invalid input: cb is null!");
        } else {
            this.mDisplayEngineCallbacks.remove(cb);
        }
    }

    private boolean setSceneHasPermission(int scene, int action) {
        int uid;
        if (!sSetSceneCheckPermissionList.contains(Integer.valueOf(scene)) || (uid = Binder.getCallingUid()) == 1000 || this.mContext.checkCallingOrSelfPermission(DISPLAY_ENGINE_PERMISSION) == 0) {
            return true;
        }
        DeLog.w(TAG, "setScene requires SYSTEM_UID or com.huawei.permission.ACCESS_DISPLAY_ENGINE, scene=" + scene + ", action=" + action + ", uid=" + uid + ", pid=" + Binder.getCallingPid());
        return false;
    }

    public int setData(int type, PersistableBundle data) {
        int ret = -1;
        try {
            IDisplayEngineService service = getNativeService();
            if (service == null || !setDataHasPermission(type)) {
                return -1;
            }
            if (data == null) {
                DeLog.e(TAG, "setData(" + type + ", data): data is null!");
                return -2;
            }
            ret = service.setData(type, data);
            if (type == 10) {
                handleIawareSpecialScene(type, data);
            }
            return ret;
        } catch (RemoteException e) {
            DeLog.e(TAG, "setData(" + type + ") has remote exception:" + e.getMessage());
            rebuildBinderDelayed();
        }
    }

    private boolean setDataHasPermission(int type) {
        int uid;
        if (!sSetDataCheckPermissionList.contains(Integer.valueOf(type)) || (uid = Binder.getCallingUid()) == 1000 || this.mContext.checkCallingOrSelfPermission(DISPLAY_ENGINE_PERMISSION) == 0) {
            return true;
        }
        DeLog.w(TAG, "setData requires SYSTEM_UID or com.huawei.permission.ACCESS_DISPLAY_ENGINE, type=" + type + ", uid=" + uid + ", pid=" + Binder.getCallingPid());
        return false;
    }

    public int sendMessage(int messageID, Bundle data) {
        return 0;
    }

    public int getEffectEx(int feature, int type, Bundle data) {
        try {
            if (!getEffectExHasPermission(feature, type)) {
                return -1;
            }
            if (feature == 36 && type == 16) {
                return getFeatureGameDisableAutoBrightnessModeData(feature, data);
            }
            if (feature == 30 && type == 12) {
                data.putString("DefaultFilters", this.mFilmFilters);
                data.putInt("DefaultRanges", 100);
                return 0;
            }
            IDisplayEngineService service = getNativeService();
            if (service == null) {
                return -1;
            }
            if (data != null) {
                return GetEffectAdapter.getEffect(service, feature, type, data);
            }
            DeLog.e(TAG, "getEffectEx(" + feature + ", " + type + ", data): data is null!");
            return -2;
        } catch (RemoteException e) {
            DeLog.e(TAG, "getEffectEx(" + feature + ", " + type + ") has remote exception:" + e.getMessage());
            rebuildBinderDelayed();
            return -1;
        }
    }

    private boolean getEffectExHasPermission(int feature, int type) {
        int uid = Binder.getCallingUid();
        if (uid == 1000 || this.mContext.checkCallingOrSelfPermission(DISPLAY_ENGINE_PERMISSION) == 0) {
            return true;
        }
        DeLog.w(TAG, "getEffect requires SYSTEM_UID or com.huawei.permission.ACCESS_DISPLAY_ENGINE, feature=" + feature + ", type=" + type + ", uid=" + uid + ", pid=" + Binder.getCallingPid());
        return false;
    }

    public int getEffect(int feature, int type, byte[] status, int length) {
        try {
            IDisplayEngineService service = getNativeService();
            if (service == null || !getEffectHasPermission(feature, type)) {
                return -1;
            }
            if (status != null) {
                if (status.length == length) {
                    if (feature == 30 && type == 12) {
                        return getFilmFilters(status);
                    }
                    return service.getEffect(feature, type, status, length);
                }
            }
            DeLog.e(TAG, "getEffect(" + feature + ", " + type + ", status, " + length + "): data is null or status.length != length!");
            return -2;
        } catch (RemoteException e) {
            DeLog.e(TAG, "getEffect(" + feature + ", " + type + ", " + length + ") has remote exception:" + e.getMessage());
            rebuildBinderDelayed();
            return -1;
        }
    }

    private int getFilmFilters(byte[] status) {
        String str = this.mFilmFilters;
        if (str != null) {
            try {
                byte[] filtersData = str.getBytes("UTF-8");
                if (status.length < filtersData.length) {
                    DeLog.w(TAG, "buffer length is not large enough!" + status.length + " ," + filtersData.length);
                    return -1;
                }
                System.arraycopy(filtersData, 0, status, 0, filtersData.length);
            } catch (UnsupportedEncodingException e) {
                DeLog.w(TAG, "Do not support UTF-8");
            }
        }
        return 0;
    }

    private boolean getEffectNeedCheckPermission(int feature, int type) {
        return feature == 14 || feature == 25;
    }

    private boolean getEffectHasPermission(int feature, int type) {
        int uid;
        if (!getEffectNeedCheckPermission(feature, type) || (uid = Binder.getCallingUid()) == 1000 || this.mContext.checkCallingOrSelfPermission(DISPLAY_ENGINE_PERMISSION) == 0) {
            return true;
        }
        DeLog.w(TAG, "getEffect requires SYSTEM_UID or com.huawei.permission.ACCESS_DISPLAY_ENGINE, feature=" + feature + ", type=" + type + ", uid=" + uid + ", pid=" + Binder.getCallingPid());
        return false;
    }

    public int setEffect(int feature, int mode, PersistableBundle data) {
        try {
            IDisplayEngineService service = getNativeService();
            if (service == null || !setEffectHasPermission(feature, mode)) {
                return -1;
            }
            if (data != null) {
                return service.setEffect(feature, mode, data);
            }
            DeLog.e(TAG, "setEffect(" + feature + ", " + mode + ", data): data is null!");
            return -2;
        } catch (RemoteException e) {
            DeLog.e(TAG, "setEffect(" + feature + ", " + mode + ") has remote exception:" + e.getMessage());
            rebuildBinderDelayed();
            return -1;
        }
    }

    private boolean setEffectNeedCheckPermission(int feature, int mode) {
        return feature == 33;
    }

    private boolean setEffectHasPermission(int feature, int mode) {
        int uid;
        if (!setEffectNeedCheckPermission(feature, mode) || (uid = Binder.getCallingUid()) == 1000 || this.mContext.checkCallingOrSelfPermission(DISPLAY_ENGINE_PERMISSION) == 0) {
            return true;
        }
        DeLog.w(TAG, "setEffect requires SYSTEM_UID or com.huawei.permission.ACCESS_DISPLAY_ENGINE, feature=" + feature + ", mode=" + mode + ", uid=" + uid + ", pid=" + Binder.getCallingPid());
        return false;
    }

    public void updateLightSensorState(boolean sensorEnable) {
        int uid = Binder.getCallingUid();
        if (uid != 1000) {
            DeLog.w(TAG, "updateLightSensorState requires SYSTEM_UID, uid=" + uid + ", pid=" + Binder.getCallingPid());
            return;
        }
        enableLightSensor(sensorEnable);
        DeLog.i(TAG, "LightSensorEnable=" + sensorEnable);
    }

    public List<Bundle> getAllRecords(String name, Bundle info) {
        int uid = Binder.getCallingUid();
        if (uid == 1000) {
            return DisplayEngineDbManager.getInstance(this.mContext).getAllRecords(name, info);
        }
        DeLog.w(TAG, "getAllRecords requires SYSTEM_UID, uid=" + uid + ", pid=" + Binder.getCallingPid());
        return Collections.emptyList();
    }

    @Override // com.android.server.display.HwLightSensorController.LightSensorCallbacks
    public void processSensorData(long timeInMs, int lux, int cct) {
        PersistableBundle bundle = new PersistableBundle();
        bundle.putIntArray("Buffer", new int[]{lux, cct});
        bundle.putInt("BufferLength", 8);
        int ret = setData(9, bundle);
        if (ret != 0) {
            DeLog.i(TAG, "processSensorData set Data Error: ret =" + ret);
        }
        sendAmbientColorTempToMonitor(timeInMs, lux, cct);
    }

    private void enableLightSensor(boolean enable) {
        HwLightSensorController hwLightSensorController;
        if (this.mLightSensorEnable != enable && (hwLightSensorController = this.mLightSensorController) != null) {
            this.mLightSensorEnable = enable;
            if (this.mLightSensorEnable) {
                hwLightSensorController.enableSensor();
                this.mLastAmbientColorTempToMonitorTime = 0;
                return;
            }
            hwLightSensorController.disableSensor();
        }
    }

    /* access modifiers changed from: private */
    public class DisplayBroadcastReceiver extends BroadcastReceiver {
        public DisplayBroadcastReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_OFF);
            filter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_ON);
            filter.addAction("android.intent.action.BOOT_COMPLETED");
            filter.addAction(SmartDualCardConsts.SYSTEM_STATE_ACTION_USER_SWITCHED);
            filter.addAction("android.intent.action.BATTERY_CHANGED");
            filter.addAction(DisplayEngineService.ACTION_FACTORY_GMP);
            filter.setPriority(1000);
            DisplayEngineService.this.mContext.registerReceiver(this, filter);
        }

        private void handleBroadcastScreenOff() {
            DisplayEngineService.this.setScene(10, 17);
            boolean unused = DisplayEngineService.this.mIsTrainingTriggeredSinceLastScreenOff = false;
            boolean unused2 = DisplayEngineService.this.mScreenOn = false;
            if (DisplayEngineService.this.mIsFactoryGmpOn) {
                DisplayEngineService.this.handleFactoryGmpBroadcast(0);
            }
        }

        private void handleBroadcastScreenOn() {
            DisplayEngineService.this.setScene(10, 16);
            if (!DisplayEngineService.this.mIsBrightnessTrainingRunning || DisplayEngineService.this.mIsBrightnessTrainingAborting || DisplayEngineService.this.mDisplayManager == null) {
                DeLog.d(DisplayEngineService.TAG, "Trigger training abort failed, training is NOT running or is already aborting.");
            } else {
                boolean unused = DisplayEngineService.this.mIsBrightnessTrainingAborting = true;
                DisplayEngineService.this.mThreadPool.execute(new Runnable() {
                    /* class com.android.server.display.$$Lambda$DisplayEngineService$DisplayBroadcastReceiver$2iLaJSF1SOXAh075m0c5Ho7oUVs */

                    public final void run() {
                        DisplayBroadcastReceiver.this.lambda$handleBroadcastScreenOn$0$DisplayEngineService$DisplayBroadcastReceiver();
                    }
                });
            }
            boolean unused2 = DisplayEngineService.this.mScreenOn = true;
        }

        public /* synthetic */ void lambda$handleBroadcastScreenOn$0$DisplayEngineService$DisplayBroadcastReceiver() {
            DeLog.i(DisplayEngineService.TAG, "mDisplayManager.brightnessTrainingAbort start... ");
            DisplayEngineService.this.mDisplayManager.brightnessTrainingAbort();
            DeLog.i(DisplayEngineService.TAG, "mDisplayManager.brightnessTrainingAbort finished.");
            boolean unused = DisplayEngineService.this.mIsBrightnessTrainingAborting = false;
        }

        private void handleUserSwitched() {
            EyeProtectionConfig.setDefaultColorTemptureValue(DisplayEngineService.this.mContext);
            if (DisplayEngineService.this.mHwBrightnessSceneRecognition != null && DisplayEngineService.this.mHwBrightnessSceneRecognition.isEnable()) {
                DisplayEngineService.this.mHwBrightnessSceneRecognition.notifyUserChange(ActivityManager.getCurrentUser());
            }
            DisplayEngineService.this.lambda$initFeatureSwitch$2$DisplayEngineService();
            DisplayEngineService.this.lambda$initFeatureSwitch$3$DisplayEngineService();
            DisplayEngineService.this.lambda$initFeatureSwitch$4$DisplayEngineService();
            DisplayEngineService.this.lambda$initFeatureSwitch$6$DisplayEngineService();
            DisplayEngineService.this.lambda$initFeatureSwitch$5$DisplayEngineService();
        }

        private void handleBrightnessTraining(Intent intent, Bundle data) {
            int batteryScale = intent.getIntExtra("scale", 100);
            boolean chargeStatus = false;
            int batteryLevel = (int) ((((float) intent.getIntExtra(MemoryConstant.MEM_FILECACHE_ITEM_LEVEL, 0)) * 100.0f) / ((float) (batteryScale == 0 ? 1 : batteryScale)));
            int status = intent.getIntExtra(HwMultiWinConstants.STATUS_KEY_STR, 1);
            if (status == 5 || status == 2) {
                chargeStatus = true;
            }
            long lastTrainingProcessTimeMillis = Settings.System.getLongForUser(DisplayEngineService.this.mContext.getContentResolver(), DisplayEngineService.KEY_USER_PREFERENCE_TRAINING_TIMESTAMP, 0, -2);
            long elapseTime = System.currentTimeMillis() - lastTrainingProcessTimeMillis;
            long newestDragTimeMillis = data.getLong("TimeStamp", 0);
            if (DisplayEngineService.this.mIsTrainingTriggeredSinceLastScreenOff || newestDragTimeMillis <= lastTrainingProcessTimeMillis || DisplayEngineService.this.mScreenOn || !chargeStatus || !DisplayEngineService.this.mBootComplete || batteryLevel <= DisplayEngineService.this.mChargeLevelThreshold || elapseTime <= DisplayEngineService.this.mMinimumTrainingIntervalMillis) {
                DeLog.d(DisplayEngineService.TAG, "-----------No Tigger Training Reason Start-----------");
                DeLog.d(DisplayEngineService.TAG, "newestDragTime / lastTrainingTime:     " + newestDragTimeMillis + " / " + lastTrainingProcessTimeMillis);
                DeLog.d(DisplayEngineService.TAG, "ChargeLevel / Charge Threshold:        " + batteryLevel + " / " + DisplayEngineService.this.mChargeLevelThreshold);
                StringBuilder sb = new StringBuilder();
                sb.append("chargeStatus:                          ");
                sb.append(chargeStatus);
                DeLog.d(DisplayEngineService.TAG, sb.toString());
                DeLog.d(DisplayEngineService.TAG, "elapsedTime / Minimum Interval Millis: " + elapseTime + " / " + DisplayEngineService.this.mMinimumTrainingIntervalMillis);
                StringBuilder sb2 = new StringBuilder();
                sb2.append("ScreenOn:                              ");
                sb2.append(DisplayEngineService.this.mScreenOn);
                DeLog.d(DisplayEngineService.TAG, sb2.toString());
                DeLog.d(DisplayEngineService.TAG, "BootComplete:                          " + DisplayEngineService.this.mBootComplete);
                DeLog.d(DisplayEngineService.TAG, "-----------No Tigger Training Reason Ended-----------");
                return;
            }
            boolean unused = DisplayEngineService.this.mIsTrainingTriggeredSinceLastScreenOff = true;
            if (DisplayEngineService.this.mIsBrightnessTrainingRunning) {
                DeLog.w(DisplayEngineService.TAG, "Trigger training failed, BrightnessTraining Running == true");
                return;
            }
            boolean unused2 = DisplayEngineService.this.mIsBrightnessTrainingRunning = true;
            DisplayEngineService.this.mThreadPool.execute(new Runnable(elapseTime) {
                /* class com.android.server.display.$$Lambda$DisplayEngineService$DisplayBroadcastReceiver$WuxOaXRT22OxX_UtjY9NpT09qIk */
                private final /* synthetic */ long f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    DisplayBroadcastReceiver.this.lambda$handleBrightnessTraining$1$DisplayEngineService$DisplayBroadcastReceiver(this.f$1);
                }
            });
        }

        public /* synthetic */ void lambda$handleBrightnessTraining$1$DisplayEngineService$DisplayBroadcastReceiver(long elapseTime) {
            DeLog.i(DisplayEngineService.TAG, "brightness Training start... ");
            int ret = DisplayEngineService.this.mDisplayManager.brightnessTrainingProcess();
            boolean unused = DisplayEngineService.this.mIsBrightnessTrainingRunning = false;
            DeLog.i(DisplayEngineService.TAG, "brightness Training finished.");
            if (ret != 0) {
                DeLog.i(DisplayEngineService.TAG, "brightness Training fail!");
                return;
            }
            DeLog.i(DisplayEngineService.TAG, "Elapsed Time since last training: " + elapseTime + " > Minimum Interval Millis: " + DisplayEngineService.this.mMinimumTrainingIntervalMillis + ", training successfully done.");
            Settings.System.putLongForUser(DisplayEngineService.this.mContext.getContentResolver(), DisplayEngineService.KEY_USER_PREFERENCE_TRAINING_TIMESTAMP, System.currentTimeMillis(), -2);
        }

        private void handleBatteryChanged(Intent intent) {
            if (DisplayEngineService.this.mDisplayManager == null) {
                DeLog.w(DisplayEngineService.TAG, "ChargingStateReceiver on recieve, mDisplayManager is null! returned.");
                return;
            }
            DisplayEngineDbManager dbManager = DisplayEngineDbManager.getInstance(DisplayEngineService.this.mContext);
            if (dbManager == null) {
                DeLog.w(DisplayEngineService.TAG, "ChargingStateReceiver on recieve, dbManager is null! returned.");
                return;
            }
            Bundle info = new Bundle();
            info.putInt("NumberLimit", DisplayEngineService.this.mNewDragNumThreshold);
            ArrayList<Bundle> items = dbManager.getAllRecords("DragInfo", info);
            if (items == null || items.size() < DisplayEngineService.this.mNewDragNumThreshold) {
                DeLog.i(DisplayEngineService.TAG, "ChargingStateReceiver on recieve, items is null || items.size < 1! returned.");
                return;
            }
            Bundle data = items.get(DisplayEngineService.this.mNewDragNumThreshold - 1);
            if (data == null) {
                DeLog.i(DisplayEngineService.TAG, "ChargingStateReceiver on recieve, data is null! returned.");
            } else {
                handleBrightnessTraining(intent, data);
            }
        }

        public void onReceive(Context context, Intent intent) {
            if (context == null || intent == null) {
                DeLog.e(DisplayEngineService.TAG, "Invalid input parameter!");
                return;
            }
            String action = intent.getAction();
            if (action == null) {
                DeLog.w(DisplayEngineService.TAG, "BroadcastReceiver.getAction() is null!");
                return;
            }
            DeLog.i(DisplayEngineService.TAG, "BroadcastReceiver.onReceive() action:" + action);
            char c = 65535;
            switch (action.hashCode()) {
                case -2128145023:
                    if (action.equals(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_OFF)) {
                        c = 0;
                        break;
                    }
                    break;
                case -1538406691:
                    if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                        c = 5;
                        break;
                    }
                    break;
                case -1454123155:
                    if (action.equals(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_ON)) {
                        c = 1;
                        break;
                    }
                    break;
                case 798292259:
                    if (action.equals("android.intent.action.BOOT_COMPLETED")) {
                        c = 2;
                        break;
                    }
                    break;
                case 870164330:
                    if (action.equals(DisplayEngineService.ACTION_FACTORY_GMP)) {
                        c = 3;
                        break;
                    }
                    break;
                case 959232034:
                    if (action.equals(SmartDualCardConsts.SYSTEM_STATE_ACTION_USER_SWITCHED)) {
                        c = 4;
                        break;
                    }
                    break;
            }
            if (c == 0) {
                handleBroadcastScreenOff();
            } else if (c == 1) {
                handleBroadcastScreenOn();
            } else if (c == 2) {
                DisplayEngineService.this.handleBootCompletedBroadcast();
            } else if (c == 3) {
                DisplayEngineService.this.handleFactoryGmpBroadcast(intent.getIntExtra("state", 0));
            } else if (c == 4) {
                handleUserSwitched();
            } else if (c == 5) {
                handleBatteryChanged(intent);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleBootCompletedBroadcast() {
        this.mThreadPool.execute(new Runnable() {
            /* class com.android.server.display.$$Lambda$DisplayEngineService$VWr0lOEQdPxkdA4RPvoeYVfFVg */

            public final void run() {
                DisplayEngineService.this.lambda$handleBootCompletedBroadcast$1$DisplayEngineService();
            }
        });
    }

    public /* synthetic */ void lambda$handleBootCompletedBroadcast$1$DisplayEngineService() {
        if (this.mNeedPkgNameFromPg) {
            registerPgSdk();
        }
        setScene(18, 16);
        this.mBootComplete = true;
        this.mScreenOn = true;
        this.mHwBrightnessSceneRecognition.initBootCompleteValues();
    }

    /* access modifiers changed from: private */
    public void handleFactoryGmpBroadcast(int state) {
        DeLog.i(TAG, "handleFactoryGmpBroadcast state = " + state);
        if (state == 1) {
            this.mIsFactoryGmpOn = true;
            setScene(47, 16);
            return;
        }
        this.mIsFactoryGmpOn = false;
        setScene(47, 17);
    }

    private class MotionReceiver extends BroadcastReceiver {
        private static final String ACTION_MOTION = "com.huawei.motion.change.noification";
        private static final String EXTRA_KEY = "category";
        private static final String EXTRA_MOTION_START = "start_motion";
        private static final String EXTRA_MOTION_STOP_BACK_APP_NOCHANGE = "back_application_nochange";
        private static final String EXTRA_MOTION_STOP_BACK_APP_TRANSATION = "back_application_transation";
        private static final String EXTRA_MOTION_STOP_HOME = "return_home";
        private static final String EXTRA_MOTION_STOP_RECENT = "enter_recent";
        private static final String PERMISSION_MOTION = "com.huawei.android.launcher.permission.HW_MOTION";

        public MotionReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_MOTION);
            filter.setPriority(1000);
            DisplayEngineService.this.mContext.registerReceiver(this, filter, PERMISSION_MOTION, null);
        }

        public void onReceive(Context context, Intent intent) {
            if (context == null || intent == null) {
                DeLog.e(DisplayEngineService.TAG, "[MOTION_NOTIFICATION] onRecive() Invalid input parameter!");
                return;
            }
            String action = intent.getAction();
            if (action == null) {
                DeLog.w(DisplayEngineService.TAG, "[MOTION_NOTIFICATION] getAction() is null!");
                return;
            }
            DeLog.d(DisplayEngineService.TAG, "[MOTION_NOTIFICATION] action:" + action);
            if (!ACTION_MOTION.equals(action)) {
                DeLog.w(DisplayEngineService.TAG, "action is not right");
            }
            String stringExtra = intent.getStringExtra(EXTRA_KEY);
            if (stringExtra == null) {
                DeLog.w(DisplayEngineService.TAG, "[MOTION_NOTIFICATION] getStringExtra() is null!");
                return;
            }
            DeLog.i(DisplayEngineService.TAG, "[MOTION_NOTIFICATION] extra:" + stringExtra);
            char c = 65535;
            switch (stringExtra.hashCode()) {
                case -1486606706:
                    if (stringExtra.equals(EXTRA_MOTION_STOP_HOME)) {
                        c = 3;
                        break;
                    }
                    break;
                case -543270494:
                    if (stringExtra.equals(EXTRA_MOTION_STOP_RECENT)) {
                        c = 4;
                        break;
                    }
                    break;
                case -158947789:
                    if (stringExtra.equals(EXTRA_MOTION_START)) {
                        c = 0;
                        break;
                    }
                    break;
                case 1472278296:
                    if (stringExtra.equals(EXTRA_MOTION_STOP_BACK_APP_NOCHANGE)) {
                        c = 1;
                        break;
                    }
                    break;
                case 1851839988:
                    if (stringExtra.equals(EXTRA_MOTION_STOP_BACK_APP_TRANSATION)) {
                        c = 2;
                        break;
                    }
                    break;
            }
            if (c == 0) {
                DisplayEngineService.this.setScene(38, 24);
                DeLog.d(DisplayEngineService.TAG, "[MOTION_NOTIFICATION] Motion start");
            } else if (c == 1 || c == 2) {
                DisplayEngineService.this.setScene(38, 22);
                DeLog.d(DisplayEngineService.TAG, "[MOTION_NOTIFICATION] Motion app");
            } else if (c == 3) {
                DeLog.d(DisplayEngineService.TAG, "[MOTION_NOTIFICATION] Motion home");
                DisplayEngineService.this.setScene(38, 21);
            } else if (c == 4) {
                DisplayEngineService.this.setScene(38, 23);
                DeLog.d(DisplayEngineService.TAG, "[MOTION_NOTIFICATION] Motion recent");
            }
        }
    }

    private void handleIawareSpecialScene(int type, PersistableBundle data) {
        if (type == 10) {
            int scene = data.getInt("Scene");
            DeLog.d(TAG, " Scene is " + scene);
            if (scene == 255) {
                String pkgName = getCurrentTopAppName().orElse("");
                DeLog.d(TAG, "pkgName is " + pkgName);
                if ("com.huawei.mmitest".equals(pkgName)) {
                    setScene(35, 16);
                    DeLog.d(TAG, "setScene (DE_SCENE_MMITEST -- DE_ACTION_MODE_ON OK!");
                }
            } else if (scene == 1002) {
                HwBrightnessSceneRecognition hwBrightnessSceneRecognition = this.mHwBrightnessSceneRecognition;
                if (hwBrightnessSceneRecognition != null && hwBrightnessSceneRecognition.isEnable()) {
                    this.mHwBrightnessSceneRecognition.setVideoPlayStatus(true);
                }
            } else if (scene == 1003) {
                HwBrightnessSceneRecognition hwBrightnessSceneRecognition2 = this.mHwBrightnessSceneRecognition;
                if (hwBrightnessSceneRecognition2 != null && hwBrightnessSceneRecognition2.isEnable()) {
                    this.mHwBrightnessSceneRecognition.setVideoPlayStatus(false);
                }
            } else {
                DeLog.w(TAG, "Unexpected scenc:" + scene);
            }
        }
    }

    private Optional<String> getCurrentTopAppName() {
        try {
            return Optional.ofNullable(PowerKit.getInstance().getTopFrontApp(this.mContext));
        } catch (RemoteException e) {
            DeLog.e(TAG, "Pg get top app name error!");
            return Optional.empty();
        }
    }

    private void registerPgSdk() {
        if (this.mNeedPkgNameFromPg && this.mPgListener == null) {
            this.mPgListener = new PowerKit.Sink() {
                /* class com.android.server.display.DisplayEngineService.AnonymousClass4 */

                public void onStateChanged(int stateType, int eventType, int pid, String pkg, int uid) {
                    DeLog.d(DisplayEngineService.TAG, "state type: " + stateType + " eventType:" + eventType + " pid:" + pid + " pkd:" + pkg + " uid:" + uid);
                    if (eventType == 1 && pkg != null && pkg.length() > 0) {
                        if (DisplayEngineService.this.mHwBrightnessSceneRecognition != null && DisplayEngineService.this.mHwBrightnessSceneRecognition.isEnable()) {
                            DisplayEngineService.this.mHwBrightnessSceneRecognition.notifyTopApkChange(pkg);
                        }
                        DeLog.i(DisplayEngineService.TAG, "Pg pkg:" + pkg);
                    }
                }
            };
            if (this.mPgListener != null) {
                PowerKit pgSdk = PowerKit.getInstance();
                if (pgSdk == null) {
                    DeLog.i(TAG, "pgSdk is null");
                    return;
                }
                try {
                    pgSdk.enableStateEvent(this.mPgListener, 10000);
                    pgSdk.enableStateEvent(this.mPgListener, (int) IDisplayEngineService.DE_ACTION_PG_BROWSER_FRONT);
                    pgSdk.enableStateEvent(this.mPgListener, (int) IDisplayEngineService.DE_ACTION_PG_3DGAME_FRONT);
                    pgSdk.enableStateEvent(this.mPgListener, (int) IDisplayEngineService.DE_ACTION_PG_EBOOK_FRONT);
                    pgSdk.enableStateEvent(this.mPgListener, (int) IDisplayEngineService.DE_ACTION_PG_INPUT_START);
                    pgSdk.enableStateEvent(this.mPgListener, (int) IDisplayEngineService.DE_ACTION_PG_INPUT_END);
                    pgSdk.enableStateEvent(this.mPgListener, (int) IDisplayEngineService.DE_ACTION_PG_CAMERA_FRONT);
                    pgSdk.enableStateEvent(this.mPgListener, (int) IDisplayEngineService.DE_ACTION_PG_OFFICE_FRONT);
                    pgSdk.enableStateEvent(this.mPgListener, (int) IDisplayEngineService.DE_ACTION_PG_VIDEO_FRONT);
                    pgSdk.enableStateEvent(this.mPgListener, (int) IDisplayEngineService.DE_ACTION_PG_LAUNCHER_FRONT);
                    pgSdk.enableStateEvent(this.mPgListener, (int) IDisplayEngineService.DE_ACTION_PG_3DGAME_FRONT);
                    pgSdk.enableStateEvent(this.mPgListener, (int) IDisplayEngineService.DE_ACTION_PG_MMS_FRONT);
                    pgSdk.enableStateEvent(this.mPgListener, (int) IDisplayEngineService.DE_ACTION_PG_VIDEO_START);
                    pgSdk.enableStateEvent(this.mPgListener, (int) IDisplayEngineService.DE_ACTION_PG_VIDEO_END);
                    pgSdk.enableStateEvent(this.mPgListener, (int) IDisplayEngineService.DE_ACTION_PG_CAMERA_END);
                } catch (RemoteException e) {
                    DeLog.w(TAG, "Pg enableStateEvent fail!");
                }
            }
        }
    }

    private void setDefaultConfigValue() {
        this.mNeedPkgNameFromPg = false;
    }

    private void getConfigParam() {
        try {
            if (!getConfig()) {
                DeLog.e(TAG, "getConfig failed!");
                setDefaultConfigValue();
            }
        } catch (IOException e) {
            DeLog.e(TAG, "getConfig failed setDefaultConfigValue!");
            setDefaultConfigValue();
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0031, code lost:
        r5 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:?, code lost:
        r3.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0036, code lost:
        r6 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0037, code lost:
        r4.addSuppressed(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x003a, code lost:
        throw r5;
     */
    private boolean getConfig() throws IOException {
        DeLog.i(TAG, "getConfig");
        File xmlFile = HwCfgFilePolicy.getCfgFile(SR_CONTROL_XML_FILE, 0);
        if (xmlFile == null) {
            DeLog.w(TAG, "get xmlFile :/display/effect/displayengine/SR_control.xml failed!");
            return false;
        }
        try {
            FileInputStream inputStream = new FileInputStream(xmlFile);
            if (getConfigFromXML(inputStream)) {
                DeLog.i(TAG, "get xmlFile error");
                inputStream.close();
                return true;
            }
            inputStream.close();
            return false;
        } catch (FileNotFoundException e) {
            DeLog.i(TAG, "get xmlFile error!");
        }
    }

    private void handleXmlStartTag(String tagName, XmlPullParser parser) throws IOException, XmlPullParserException {
        if (tagName == null) {
            DeLog.w(TAG, "tag Name is null!");
            return;
        }
        char c = 65535;
        switch (tagName.hashCode()) {
            case -1276987271:
                if (tagName.equals("NewDragNumThreshold")) {
                    c = 4;
                    break;
                }
                break;
            case -1037229316:
                if (tagName.equals("FilmFilterVersion")) {
                    c = 5;
                    break;
                }
                break;
            case -525307465:
                if (tagName.equals("FilmFilters")) {
                    c = 6;
                    break;
                }
                break;
            case -197109250:
                if (tagName.equals("SRControl")) {
                    c = 0;
                    break;
                }
                break;
            case 480885602:
                if (tagName.equals("NeedPkgNameFromPG")) {
                    c = 1;
                    break;
                }
                break;
            case 1689613627:
                if (tagName.equals("ChargeLevelThreshold")) {
                    c = 3;
                    break;
                }
                break;
            case 1737563218:
                if (tagName.equals("MinimumTrainingIntervalMinutes")) {
                    c = 2;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return;
            case 1:
                this.mNeedPkgNameFromPg = Boolean.parseBoolean(parser.nextText());
                DeLog.i(TAG, "Need Pg pkg name =" + this.mNeedPkgNameFromPg);
                return;
            case 2:
                this.mMinimumTrainingIntervalMillis = ((long) Integer.parseInt(parser.nextText())) * AppHibernateCst.DELAY_ONE_MINS;
                DeLog.i(TAG, "mMinimumTrainingIntervalMillis = " + this.mMinimumTrainingIntervalMillis);
                return;
            case 3:
                this.mChargeLevelThreshold = Integer.parseInt(parser.nextText());
                DeLog.i(TAG, "mChargeLevelThreshold = " + this.mChargeLevelThreshold);
                return;
            case 4:
                this.mNewDragNumThreshold = Integer.parseInt(parser.nextText());
                DeLog.i(TAG, "mNewDragNumThreshold = " + this.mNewDragNumThreshold);
                return;
            case 5:
                this.mFilmFilterVersion = Integer.parseInt(parser.nextText());
                DeLog.i(TAG, "mFilmFilterVersion = " + this.mFilmFilterVersion);
                return;
            case 6:
                this.mFilmFilters = parser.nextText();
                this.mFilmFilters = this.mFilmFilters.replace(" ", "").replace(",", "");
                DeLog.i(TAG, "mFilmFilters = " + this.mFilmFilters);
                return;
            default:
                DeLog.w(TAG, "unSupport tag:" + tagName + "! It may be deprecated!");
                return;
        }
    }

    private boolean getConfigFromXML(InputStream inStream) {
        DeLog.i(TAG, "getConfigFromeXML");
        boolean loadFinished = false;
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(inStream, "UTF-8");
            for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                if (eventType == 2) {
                    handleXmlStartTag(parser.getName(), parser);
                } else if (eventType != 3) {
                    DeLog.w(TAG, "unexpected XML TAG!");
                } else if ("SRControl".equals(parser.getName())) {
                    loadFinished = true;
                }
            }
            if (loadFinished) {
                DeLog.i(TAG, "getConfigFromeXML success!");
                return true;
            }
        } catch (XmlPullParserException e) {
            DeLog.e(TAG, "get xmlFile parser exception");
        } catch (IOException e2) {
            DeLog.e(TAG, "get xmlFile io exception");
        } catch (NumberFormatException e3) {
            DeLog.e(TAG, "get xmlFile number format exception");
        }
        DeLog.e(TAG, "getConfigFromeXML false!");
        return false;
    }

    private void sendAmbientColorTempToMonitor(long time, int lux, int colorTemp) {
        if (this.mDisplayEffectMonitor != null) {
            long j = this.mLastAmbientColorTempToMonitorTime;
            if (j == 0 || time <= j) {
                this.mLastAmbientColorTempToMonitorTime = time;
                return;
            }
            int durationInMs = (int) (time - j);
            if (durationInMs >= COLOR_TEMP_MONITOR_SAMPLING_INTERVAL_MS) {
                this.mLastAmbientColorTempToMonitorTime = time;
                if (colorTemp > 0 && lux > 50) {
                    ArrayMap<String, Object> params = new ArrayMap<>();
                    params.put(DisplayEffectMonitor.MonitorModule.PARAM_TYPE, "ambientColorTempCollection");
                    params.put("colorTempValue", Integer.valueOf(colorTemp));
                    params.put("durationInMs", Integer.valueOf(durationInMs));
                    this.mDisplayEffectMonitor.sendMonitorParam(params);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    /* renamed from: updateDcBrightness */
    public void lambda$initFeatureSwitch$2$DisplayEngineService() {
        DeLog.i(TAG, "DcBrightnessDimmingObserver update");
        if (Settings.System.getIntForUser(this.mContext.getContentResolver(), KEY_DC_BRIGHTNESS, 0, -2) == 1) {
            setScene(44, 16);
        } else {
            setScene(44, 17);
        }
    }

    /* access modifiers changed from: private */
    /* renamed from: updateNaturalTone */
    public void lambda$initFeatureSwitch$3$DisplayEngineService() {
        DeLog.i(TAG, "NaturalToneObserver update");
        if (Settings.System.getIntForUser(this.mContext.getContentResolver(), KEY_NATURAL_TONE, 0, -2) == 1) {
            setScene(25, 16);
        } else {
            setScene(25, 17);
        }
    }

    /* access modifiers changed from: private */
    /* renamed from: updateColorMode */
    public void lambda$initFeatureSwitch$4$DisplayEngineService() {
        DeLog.i(TAG, "ColorModeObserver update");
        if (Settings.System.getIntForUser(this.mContext.getContentResolver(), KEY_COLOR_MODE, 1, -2) == 0) {
            setScene(13, 16);
        } else {
            setScene(13, 17);
        }
    }

    /* access modifiers changed from: private */
    /* renamed from: updateAutoBrightnessAdj */
    public void lambda$initFeatureSwitch$6$DisplayEngineService() {
        DeLog.i(TAG, "auto brightness adj update");
        HwBrightnessSceneRecognition hwBrightnessSceneRecognition = this.mHwBrightnessSceneRecognition;
        if (hwBrightnessSceneRecognition != null && hwBrightnessSceneRecognition.isEnable()) {
            this.mHwBrightnessSceneRecognition.notifyAutoBrightnessAdj();
        }
    }

    /* access modifiers changed from: private */
    /* renamed from: updateGlobalRead */
    public void lambda$initFeatureSwitch$5$DisplayEngineService() {
        DeLog.i(TAG, "global reading update");
        if (Settings.System.getIntForUser(this.mContext.getContentResolver(), KEY_GLOBAL_READ, 0, -2) == 1) {
            setScene(48, 16);
            Settings.System.putIntForUser(this.mContext.getContentResolver(), KEY_READING_MODE_SWITCH, 1, -2);
            return;
        }
        setScene(48, 17);
        Settings.System.putIntForUser(this.mContext.getContentResolver(), KEY_READING_MODE_SWITCH, 0, -2);
    }

    private void initFeatureSwitch() {
        if (getSupported(32) != 0) {
            lambda$initFeatureSwitch$2$DisplayEngineService();
            this.mFeatureObservers.addObserver(KEY_DC_BRIGHTNESS, new Runnable() {
                /* class com.android.server.display.$$Lambda$DisplayEngineService$J5xLxypr4VuqXN1ROZ01rER0cvc */

                public final void run() {
                    DisplayEngineService.this.lambda$initFeatureSwitch$2$DisplayEngineService();
                }
            });
        }
        if (getSupported(23) != 0) {
            lambda$initFeatureSwitch$3$DisplayEngineService();
            this.mFeatureObservers.addObserver(KEY_NATURAL_TONE, new Runnable() {
                /* class com.android.server.display.$$Lambda$DisplayEngineService$VpCaREtANrx49Pl7n2CfKWMLcgU */

                public final void run() {
                    DisplayEngineService.this.lambda$initFeatureSwitch$3$DisplayEngineService();
                }
            });
        }
        if (getSupported(11) != 0) {
            lambda$initFeatureSwitch$4$DisplayEngineService();
            this.mFeatureObservers.addObserver(KEY_COLOR_MODE, new Runnable() {
                /* class com.android.server.display.$$Lambda$DisplayEngineService$KlDA8h6q5Zpdqysj0k9lnXhg5UA */

                public final void run() {
                    DisplayEngineService.this.lambda$initFeatureSwitch$4$DisplayEngineService();
                }
            });
        }
        if (getSupported(34) != 0) {
            lambda$initFeatureSwitch$5$DisplayEngineService();
            this.mFeatureObservers.addObserver(KEY_GLOBAL_READ, new Runnable() {
                /* class com.android.server.display.$$Lambda$DisplayEngineService$LjTJE9ALXbCgCUqYlXB16RJY8Zk */

                public final void run() {
                    DisplayEngineService.this.lambda$initFeatureSwitch$5$DisplayEngineService();
                }
            });
        }
        this.mFeatureObservers.addObserver("hw_screen_auto_brightness_adj", new Runnable() {
            /* class com.android.server.display.$$Lambda$DisplayEngineService$f8_AAP5tf3L0llnM1VSD3SnSByU */

            public final void run() {
                DisplayEngineService.this.lambda$initFeatureSwitch$6$DisplayEngineService();
            }
        });
        this.mFeatureObservers.init();
        EyeProtectionConfig.initSettingsRange(getLcdPanelName().orElse(""), this.mContext);
    }

    private class FeatureObservers extends ContentObserver {
        private Map<Uri, Runnable> observerUris = new HashMap();

        public FeatureObservers(Handler handler) {
            super(handler);
        }

        public void addObserver(String key, Runnable r) {
            Uri uri = Settings.System.getUriFor(key);
            if (uri == null) {
                DeLog.w(DisplayEngineService.TAG, "get uri from key is null!");
            } else {
                this.observerUris.put(uri, r);
            }
        }

        public void addObserver(Uri uri, Runnable r) {
            this.observerUris.put(uri, r);
        }

        public void init() {
            for (Uri uri : this.observerUris.keySet()) {
                DisplayEngineService.this.mContext.getContentResolver().registerContentObserver(uri, false, this, -1);
            }
        }

        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        public void onChange(boolean selfChange, Uri uri) {
            if (uri == null) {
                DeLog.w(DisplayEngineService.TAG, "uri is null!");
                return;
            }
            Runnable handleUri = this.observerUris.get(uri);
            if (handleUri == null) {
                DeLog.w(DisplayEngineService.TAG, uri.toString() + " can not find runable!");
                return;
            }
            handleUri.run();
        }
    }

    private void registerFoldableStateCallback() {
        if (this.mFoldableStateCallback == null) {
            this.mFoldableStateCallback = new FoldableStateListenerForCompensation();
            HwFoldScreenManagerEx.registerFoldableState(this.mFoldableStateCallback, 1);
        }
    }

    private class FoldableStateListenerForCompensation implements HwFoldScreenManagerEx.FoldableStateListener {
        private FoldableStateListenerForCompensation() {
        }

        public void onStateChange(Bundle extra) {
            if (extra == null) {
                DeLog.e(DisplayEngineService.TAG, "Invalid input: extra is null!");
                return;
            }
            int state = extra.getInt("fold_state");
            if (state == 1) {
                DeLog.i(DisplayEngineService.TAG, "FoldableStateListener.onStateChange():Expand!");
                DisplayEngineService.this.setScene(43, 0);
            } else if (state == 2) {
                DeLog.i(DisplayEngineService.TAG, "FoldableStateListener.onStateChange():Folded!");
                DisplayEngineService.this.setScene(43, 1);
            } else if (state != 3) {
                DeLog.i(DisplayEngineService.TAG, "FoldableStateListener.onStateChange():Not support this state=" + state);
            } else {
                DeLog.i(DisplayEngineService.TAG, "FoldableStateListener.onStateChange():Half folded!");
                DisplayEngineService.this.setScene(43, 2);
            }
        }
    }

    private Optional<String> getLcdPanelName() {
        byte[] name = new byte[128];
        int ret = getEffect(14, 0, name, name.length);
        if (ret == 0) {
            return Optional.of(new String(name, StandardCharsets.UTF_8).trim().replace(' ', '_'));
        }
        DeLog.e(TAG, "getLcdPanelName() getEffect failed! ret=" + ret);
        return Optional.empty();
    }

    private void registerDisplayEngineNativeCallback() {
        try {
            getNativeService().registerCallback(this.mDisplayEngineNativeCallback);
        } catch (RemoteException e) {
            DeLog.e(TAG, "Faled to register displayengine native callback: " + e.getMessage());
        }
    }

    private boolean isFeatureGameDisableAutoBrightnessModeEnable() {
        HwBrightnessSceneRecognition hwBrightnessSceneRecognition = this.mHwBrightnessSceneRecognition;
        if (hwBrightnessSceneRecognition != null && hwBrightnessSceneRecognition.isEnable()) {
            return this.mHwBrightnessSceneRecognition.isFeatureEnable(HwBrightnessSceneRecognition.FeatureTag.TAG_GAME_DISABLE_AUTO_BRIGHTNESS_SCENE);
        }
        return false;
    }

    private int isFeatureGameDisableAutoBrightnessModeSupport(int feature) {
        if (feature == 36 && isFeatureGameDisableAutoBrightnessModeEnable()) {
            return 1;
        }
        return -2;
    }

    private int getFeatureGameDisableAutoBrightnessModeData(int feature, Bundle data) {
        if (feature != 36 || !isFeatureGameDisableAutoBrightnessModeEnable() || data == null) {
            return -2;
        }
        data.putBoolean("GameDisableAutoBrightnessModeEnable", this.mHwBrightnessSceneRecognition.getGameDisableAutoBrightnessModeStatus());
        return 1;
    }

    private int setGameDisableAutoBrightnessModeStatus(int scene, boolean enable) {
        if (scene != 49 || !isFeatureGameDisableAutoBrightnessModeEnable()) {
            return -2;
        }
        this.mHwBrightnessSceneRecognition.setGameDisableAutoBrightnessModeStatus(enable);
        return 1;
    }
}
