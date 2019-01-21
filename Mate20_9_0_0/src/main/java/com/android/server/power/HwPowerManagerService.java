package com.android.server.power;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IHwBrightnessCallback;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.os.WorkSource;
import android.os.WorkSource.WorkChain;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.Jlog;
import android.util.Log;
import android.util.Slog;
import com.android.internal.app.IAppOpsService;
import com.android.internal.app.IBatteryStats;
import com.android.server.BatteryService;
import com.android.server.LocalServices;
import com.android.server.am.ActivityManagerService;
import com.android.server.display.DisplayEffectMonitor;
import com.android.server.display.DisplayEffectMonitor.MonitorModule;
import com.android.server.display.DisplayManagerService;
import com.android.server.display.Utils;
import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.appqoe.HwAPPQoEUtils;
import com.android.server.lights.LightsService;
import com.android.server.pg.PGManagerInternal;
import com.android.server.policy.PickUpWakeScreenManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.power.PowerManagerService.UidState;
import com.android.server.power.PowerManagerService.WakeLock;
import com.android.server.rms.iaware.appmng.AwareFakeActivityRecg;
import com.android.server.rms.iaware.sysload.SysLoadManager;
import com.android.server.wm.WindowManagerInternal;
import com.huawei.IntelliServer.intellilib.IIntelliListener;
import com.huawei.IntelliServer.intellilib.IIntelliService;
import com.huawei.IntelliServer.intellilib.IIntelliService.Stub;
import com.huawei.IntelliServer.intellilib.IntelliAlgoResult;
import com.huawei.displayengine.DisplayEngineManager;
import huawei.com.android.server.fingerprint.FingerViewController;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public class HwPowerManagerService extends PowerManagerService {
    private static final String COLOR_TEMPERATURE = "color_temperature";
    private static final int COLOR_TEMPERATURE_DEFAULT = 128;
    private static final String COLOR_TEMPERATURE_RGB = "color_temperature_rgb";
    private static final String DIRECT_CHG_ENABLE_PATH_HISI = "/sys/class/hw_power/charger/direct_charger/enable_charger";
    private static final int INTELLI_DETECT_FACE_PRESENCE = 1;
    private static final int MAXINUM_TEMPERATURE = 255;
    private static final int MODE_COLOR_TEMP_3_DIMENSION = 1;
    private static final String NORMAL_CHG_ENABLE_PATH = "/sys/class/hw_power/charger/charge_data/enable_charger";
    private static final String POWER_STATE_CHARGE_DISABLE = "0";
    private static final String POWER_STATE_CHARGE_ENABLE = "1";
    private static final String SERVICE_ACTION = "com.huawei.intelliServer.intelliServer";
    private static final String SERVICE_CLASS = "com.huawei.intelliServer.intelliServer.IntelliService";
    private static final String SERVICE_PACKAGE = "com.huawei.intelliServer.intelliServer";
    public static final int SUBTYPE_DROP_WAKELOCK = 1;
    public static final int SUBTYPE_PROXY_NO_WORKSOURCE = 2;
    public static final int SUBTYPE_RELEASE_NO_WORKSOURCE = 3;
    private static final String TAG = "HwPowerManagerService";
    private static final int TYPE_FACE_STAY_LIT = 3;
    private static boolean mLoadLibraryFailed;
    private ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (HwPowerManagerService.this.mIntelliServiceBound) {
                if (PowerManagerService.DEBUG) {
                    String str = HwPowerManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("IntelliService Connected, mShouldFaceDetectLater=");
                    stringBuilder.append(HwPowerManagerService.this.mShouldFaceDetectLater);
                    Slog.d(str, stringBuilder.toString());
                }
                HwPowerManagerService.this.mIRemote = Stub.asInterface(iBinder);
                if (HwPowerManagerService.this.mShouldFaceDetectLater) {
                    HwPowerManagerService.this.mShouldFaceDetectLater = false;
                    HwPowerManagerService.this.registerFaceDetect();
                }
                return;
            }
            Slog.w(HwPowerManagerService.TAG, "IntelliService not bound, ignore.");
        }

        public void onServiceDisconnected(ComponentName componentName) {
            if (PowerManagerService.DEBUG) {
                String str = HwPowerManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("IntelliService Disconnected, mIntelliServiceBound=");
                stringBuilder.append(HwPowerManagerService.this.mIntelliServiceBound);
                Slog.d(str, stringBuilder.toString());
            }
            if (HwPowerManagerService.this.mIntelliServiceBound) {
                HwPowerManagerService.this.resetFaceDetect();
            }
        }

        public void onBindingDied(ComponentName name) {
            String str = HwPowerManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("IntelliService binding died, mIntelliServiceBound=");
            stringBuilder.append(HwPowerManagerService.this.mIntelliServiceBound);
            Slog.w(str, stringBuilder.toString());
            if (HwPowerManagerService.this.mIntelliServiceBound) {
                HwPowerManagerService.this.resetFaceDetect();
            }
        }
    };
    private final Context mContext;
    private DisplayEffectMonitor mDisplayEffectMonitor;
    private DisplayEngineManager mDisplayEngineManager;
    private int mEyesProtectionMode;
    private boolean mFaceDetecting;
    private FingerSenseObserver mFingerSenseObserver;
    private final ArrayList<WakeLock> mForceReleasedWakeLocks = new ArrayList();
    private boolean mHadSendBrightnessModeToMonitor;
    private boolean mHadSendManualBrightnessToMonitor;
    private final ArrayList<HwBrightnessCallbackData> mHwBrightnessCallbacks = new ArrayList();
    private final ArrayMap<String, HwBrightnessInnerProcessor> mHwBrightnessInnerProcessors = new ArrayMap();
    private IIntelliService mIRemote;
    private Intent mIntelliIntent;
    private IIntelliListener mIntelliListener = new IIntelliListener.Stub() {
        public void onEvent(IntelliAlgoResult intelliAlgoResult) throws RemoteException {
            int result = intelliAlgoResult != null ? intelliAlgoResult.getPrecenseStatus() : 0;
            if (PowerManagerService.DEBUG) {
                String str = HwPowerManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onEvent result=");
                stringBuilder.append(result);
                stringBuilder.append(",mFaceDetecting=");
                stringBuilder.append(HwPowerManagerService.this.mFaceDetecting);
                Slog.i(str, stringBuilder.toString());
            }
            if (result == 1 && HwPowerManagerService.this.mFaceDetecting) {
                long ident = Binder.clearCallingIdentity();
                try {
                    HwPowerManagerService.this.userActivityInternal(SystemClock.uptimeMillis(), 0, 0, 1000);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }

        public void onErr(int i) throws RemoteException {
        }
    };
    private boolean mIntelliServiceBound;
    private boolean mIsGoogleEBS = false;
    private final Object mLockHwBrightnessCallbacks = new Object();
    private PGManagerInternal mPGManagerInternal;
    private WindowManagerPolicy mPolicy;
    private final ArrayList<ProxyWLProcessInfo> mProxyWLProcessList = new ArrayList();
    private final ArrayList<WakeLock> mProxyedWakeLocks = new ArrayList();
    private SettingsObserver mSettingsObserver;
    private boolean mShouldFaceDetectLater;
    private boolean mSupportDisplayEngine3DColorTemperature;
    private boolean mSystemReady = false;
    private int mWaitBrightTimeout = HwAPPQoEUtils.APP_TYPE_GENERAL_GAME;
    private WindowManagerInternal mWindowManagerInternal;

    private final class BluetoothReceiver extends BroadcastReceiver {
        private BluetoothReceiver() {
        }

        /* synthetic */ BluetoothReceiver(HwPowerManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            Throwable th;
            Intent intent2 = intent;
            long now = SystemClock.uptimeMillis();
            BluetoothDevice btDevice = (BluetoothDevice) intent2.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            int newState = intent2.getIntExtra("android.bluetooth.profile.extra.STATE", 0);
            int oldState = intent2.getIntExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", 0);
            String str = HwPowerManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("BluetoothReceiver,btDevice:");
            stringBuilder.append(btDevice);
            stringBuilder.append(",newState:");
            stringBuilder.append(newState);
            stringBuilder.append(",oldState:");
            stringBuilder.append(oldState);
            stringBuilder.append(",intent:");
            stringBuilder.append(intent.getAction());
            Slog.d(str, stringBuilder.toString());
            boolean needWakeUp = false;
            if (btDevice != null && newState == 2 && oldState == 1) {
                BluetoothClass btClass = btDevice.getBluetoothClass();
                String str2 = HwPowerManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("BluetoothReceiver btClass.getDeviceClass():");
                stringBuilder2.append(btClass);
                Slog.d(str2, stringBuilder2.toString());
                if (btClass != null) {
                    switch (btClass.getDeviceClass()) {
                        case 1024:
                        case 1028:
                        case 1032:
                        case 1040:
                        case 1044:
                        case 1048:
                        case 1052:
                        case 1056:
                        case 1060:
                        case 1064:
                        case 1068:
                        case 1072:
                        case 1076:
                        case 1080:
                        case 1084:
                        case 1088:
                        case 1096:
                            needWakeUp = true;
                            break;
                    }
                    boolean needWakeUp2 = needWakeUp;
                    if (needWakeUp2) {
                        Object obj = HwPowerManagerService.this.mLock;
                        synchronized (obj) {
                            Object obj2;
                            try {
                                obj2 = obj;
                                if (HwPowerManagerService.this.wakeUpNoUpdateLocked(now, "bluetooth.connected", 1000, HwPowerManagerService.this.mContext.getOpPackageName(), 1000) || HwPowerManagerService.this.userActivityNoUpdateLocked(now, 0, 0, 1000)) {
                                    HwPowerManagerService.this.updatePowerStateLocked();
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                throw th;
                            }
                        }
                    }
                    needWakeUp = needWakeUp2;
                }
            }
        }
    }

    private static final class FingerSenseObserver extends ContentObserver {
        private ContentResolver resolver;

        public FingerSenseObserver(Handler handler, ContentResolver resolver) {
            super(handler);
            this.resolver = resolver;
        }

        public void observe() {
            this.resolver.registerContentObserver(Global.getUriFor("fingersense_enabled"), false, this, -1);
        }

        public void onChange(boolean selfChange) {
            boolean z = true;
            if (Global.getInt(this.resolver, "fingersense_enabled", 1) != 1) {
                z = false;
            }
            PowerManagerService.nativeSetFsEnable(z);
        }
    }

    private final class HeadsetReceiver extends BroadcastReceiver {
        private HeadsetReceiver() {
        }

        /* synthetic */ HeadsetReceiver(HwPowerManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            synchronized (HwPowerManagerService.this.mLock) {
                long now = SystemClock.uptimeMillis();
                String str = HwPowerManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("HeadsetReceiver,state:");
                stringBuilder.append(intent.getIntExtra("state", 0));
                Slog.d(str, stringBuilder.toString());
                if (intent.getIntExtra("state", 0) == 1 && (HwPowerManagerService.this.wakeUpNoUpdateLocked(now, "headset.connected", 1000, HwPowerManagerService.this.mContext.getOpPackageName(), 1000) || HwPowerManagerService.this.userActivityNoUpdateLocked(now, 0, 0, 1000))) {
                    HwPowerManagerService.this.updatePowerStateLocked();
                }
            }
        }
    }

    private static final class HwBrightnessCallbackData {
        private IHwBrightnessCallback mCB;
        private List<String> mFilter = null;

        public HwBrightnessCallbackData(IHwBrightnessCallback cb, List<String> filter) {
            if (filter != null) {
                this.mFilter = new ArrayList(filter);
            }
            this.mCB = cb;
        }

        public List<String> getFilter() {
            return this.mFilter;
        }

        public IHwBrightnessCallback getCB() {
            return this.mCB;
        }
    }

    public static final class HwBrightnessInnerProcessor {
        public int setData(Bundle data) {
            Slog.w(HwPowerManagerService.TAG, "Forget to override setData()? Now in default setData(), nothing to do!");
            return -1;
        }

        public int getData(Bundle data) {
            Slog.w(HwPowerManagerService.TAG, "Forget to override getData()? Now in default getData(), nothing to do!");
            return -1;
        }
    }

    private static final class ProxyWLProcessInfo {
        public int mPid;
        public boolean mProxyWS;
        public int mUid;

        public ProxyWLProcessInfo(int pid, int uid, boolean proxyWS) {
            this.mPid = pid;
            this.mUid = uid;
            this.mProxyWS = proxyWS;
        }

        public boolean isSameProcess(int pid, int uid) {
            return (this.mPid == pid || -1 == pid) && (this.mUid == uid || -1 == uid);
        }
    }

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange, Uri uri) {
            synchronized (HwPowerManagerService.this.mLock) {
                HwPowerManagerService.this.setColorTemperatureAccordingToSetting();
            }
        }
    }

    private static native void finalize_native();

    private static native void init_SurfaceComposerClient();

    private static native void init_native();

    private static native int nativeGetDisplayFeatureSupported(int i);

    private static native int nativeGetDisplayPanelType();

    public static native String nativeReadColorTemperatureNV();

    private native int nativeSetColorTemperature(int i);

    private native int nativeUpdateRgbGamma(float f, float f2, float f3);

    static {
        mLoadLibraryFailed = false;
        try {
            System.loadLibrary("hwpwmanager_jni");
        } catch (UnsatisfiedLinkError e) {
            mLoadLibraryFailed = true;
            Slog.d(TAG, "hwpwmanager_jni library not found!");
        }
    }

    public void init(Context context, LightsService ls, ActivityManagerService am, BatteryService bs, IBatteryStats bss, IAppOpsService appOps, DisplayManagerService dm) {
    }

    public HwPowerManagerService(Context context) {
        super(context);
        this.mContext = context;
        this.mDisplayEffectMonitor = DisplayEffectMonitor.getInstance(this.mContext);
        if (this.mDisplayEffectMonitor == null) {
            Slog.e(TAG, "getDisplayEffectMonitor failed!");
        }
        if (!mLoadLibraryFailed) {
            init_native();
        }
        this.mDisplayEngineManager = new DisplayEngineManager();
        loadHwBrightnessInnerProcessors();
    }

    protected void finalize() {
        if (!mLoadLibraryFailed) {
            finalize_native();
        }
        try {
            super.finalize();
        } catch (Throwable th) {
        }
    }

    public boolean setGoogleEBS(boolean isGoogleEBS) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setGoogleEBS:");
        stringBuilder.append(isGoogleEBS);
        Slog.d(str, stringBuilder.toString());
        this.mIsGoogleEBS = isGoogleEBS;
        return true;
    }

    public int setColorTemperatureInternal(int colorTemper) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setColorTemperature:");
        stringBuilder.append(colorTemper);
        Slog.d(str, stringBuilder.toString());
        try {
            if (!mLoadLibraryFailed) {
                return nativeSetColorTemperature(colorTemper);
            }
            Slog.d(TAG, "nativeSetColorTemperature not valid!");
            return 0;
        } catch (UnsatisfiedLinkError e) {
            Slog.d(TAG, "nativeSetColorTemperature not found!");
            return -1;
        }
    }

    public boolean isDisplayFeatureSupported(int feature) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isDisplayFeatureSupported feature:");
        stringBuilder.append(feature);
        Slog.d(str, stringBuilder.toString());
        boolean z = false;
        try {
            if (mLoadLibraryFailed) {
                Slog.d(TAG, "Display feature not supported because of library not found!");
                return false;
            }
            if (nativeGetDisplayFeatureSupported(feature) != 0) {
                z = true;
            }
            return z;
        } catch (UnsatisfiedLinkError e) {
            Slog.d(TAG, "Display feature not supported because of exception!");
            return false;
        }
    }

    private void setColorRGB(float red, float green, float blue) {
        String str;
        StringBuilder stringBuilder;
        if (red < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO || red > 1.0f || green < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO || green > 1.0f || blue < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO || blue > 1.0f) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Parameters invalid: red=");
            stringBuilder2.append(red);
            stringBuilder2.append(", green=");
            stringBuilder2.append(green);
            stringBuilder2.append(", blue=");
            stringBuilder2.append(blue);
            Slog.w(str2, stringBuilder2.toString());
            return;
        }
        try {
            Class clazz = Class.forName("com.huawei.android.os.PowerManagerCustEx");
            clazz.getMethod("updateRgbGamma", new Class[]{Float.TYPE, Float.TYPE, Float.TYPE, Integer.TYPE, Integer.TYPE}).invoke(clazz, new Object[]{Float.valueOf(red), Float.valueOf(green), Float.valueOf(blue), Integer.valueOf(18), Integer.valueOf(7)});
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("setColorTemperatureAccordingToSetting and setColorRGB sucessfully:red=");
            stringBuilder3.append(red);
            stringBuilder3.append(", green=");
            stringBuilder3.append(green);
            stringBuilder3.append(", blue=");
            stringBuilder3.append(blue);
            Log.i(str3, stringBuilder3.toString());
        } catch (RuntimeException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(": reflection exception is ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (Exception ex) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(": Exception happend when setColorRGB. Message is: ");
            stringBuilder.append(ex.getMessage());
            Log.e(str, stringBuilder.toString());
        }
    }

    protected void setColorTemperatureAccordingToSetting() {
        String str;
        StringBuilder stringBuilder;
        Slog.d(TAG, "setColorTemperatureAccordingToSetting");
        float red = 1.0f;
        float green = 1.0f;
        float blue = 1.0f;
        String ctNewRGB;
        List<String> rgbarryList;
        int operation;
        if (this.mSupportDisplayEngine3DColorTemperature) {
            Slog.i(TAG, "setColorTemperatureAccordingToSetting new from displayengine.");
            try {
                this.mEyesProtectionMode = System.getIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYES_PROTECTION, 0, -2);
                if (this.mEyesProtectionMode == 1) {
                    setColorRGB(1.0f, 1.0f, 1.0f);
                    return;
                }
                ctNewRGB = System.getStringForUser(this.mContext.getContentResolver(), "color_temperature_rgb", -2);
                if (ctNewRGB != null) {
                    rgbarryList = new ArrayList(Arrays.asList(ctNewRGB.split(",")));
                    red = Float.valueOf((String) rgbarryList.get(0)).floatValue();
                    green = Float.valueOf((String) rgbarryList.get(1)).floatValue();
                    blue = Float.valueOf((String) rgbarryList.get(2)).floatValue();
                } else {
                    Slog.w(TAG, "ColorTemperature read from setting failed, and set default values");
                }
                setColorRGB(red, green, blue);
            } catch (IndexOutOfBoundsException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("IndexOutOfBoundsException:");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            } catch (Throwable th) {
                setColorRGB(1.0f, 1.0f, 1.0f);
            }
        } else if (isDisplayFeatureSupported(1)) {
            Slog.d(TAG, "setColorTemperatureAccordingToSetting new.");
            try {
                ctNewRGB = System.getStringForUser(this.mContext.getContentResolver(), "color_temperature_rgb", -2);
                if (ctNewRGB != null) {
                    rgbarryList = new ArrayList(Arrays.asList(ctNewRGB.split(",")));
                    red = Float.valueOf((String) rgbarryList.get(0)).floatValue();
                    green = Float.valueOf((String) rgbarryList.get(1)).floatValue();
                    blue = Float.valueOf((String) rgbarryList.get(2)).floatValue();
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("ColorTemperature read from setting:");
                    stringBuilder.append(ctNewRGB);
                    stringBuilder.append(red);
                    stringBuilder.append(green);
                    stringBuilder.append(blue);
                    Slog.d(str, stringBuilder.toString());
                    updateRgbGammaInternal(red, green, blue);
                } else {
                    operation = System.getIntForUser(this.mContext.getContentResolver(), "color_temperature", 128, -2);
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("ColorTemperature read from old setting:");
                    stringBuilder.append(operation);
                    Slog.d(str, stringBuilder.toString());
                    setColorTemperatureInternal(operation);
                }
            } catch (UnsatisfiedLinkError e2) {
                Slog.d(TAG, "ColorTemperature read from setting exception!");
                updateRgbGammaInternal(1.0f, 1.0f, 1.0f);
            }
        } else {
            operation = System.getIntForUser(this.mContext.getContentResolver(), "color_temperature", 128, -2);
            ctNewRGB = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("setColorTemperatureAccordingToSetting old:");
            stringBuilder2.append(operation);
            Slog.d(ctNewRGB, stringBuilder2.toString());
            setColorTemperatureInternal(operation);
        }
    }

    public int updateRgbGammaInternal(float red, float green, float blue) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateRgbGammaInternal:red=");
        stringBuilder.append(red);
        stringBuilder.append(" green=");
        stringBuilder.append(green);
        stringBuilder.append(" blue=");
        stringBuilder.append(blue);
        Slog.d(str, stringBuilder.toString());
        try {
            if (!mLoadLibraryFailed) {
                return nativeUpdateRgbGamma(red, green, blue);
            }
            Slog.d(TAG, "nativeUpdateRgbGamma not valid!");
            return 0;
        } catch (UnsatisfiedLinkError e) {
            Slog.d(TAG, "nativeUpdateRgbGamma not found!");
            return -1;
        }
    }

    private static boolean isMultiSimEnabled() {
        return false;
    }

    private boolean isPhoneInCall() {
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            int phoneCount = TelephonyManager.getDefault().getPhoneCount();
            for (int i = 0; i < phoneCount; i++) {
                if (TelephonyManager.getDefault().getCallState(i) != 0) {
                    return true;
                }
            }
            return false;
        } else if (TelephonyManager.getDefault().getCallState(SubscriptionManager.getDefaultSubscriptionId()) != 0) {
            return true;
        } else {
            return false;
        }
    }

    public void systemReady(IAppOpsService appOps) {
        super.systemReady(appOps);
        init_SurfaceComposerClient();
        this.mPolicy = (WindowManagerPolicy) getLocalService(WindowManagerPolicy.class);
        this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        this.mSystemReady = true;
        this.mSupportDisplayEngine3DColorTemperature = this.mDisplayEngineManager.getSupported(18) == 1;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("systemReady mSupportDisplayEngine3DColorTemperature=");
        stringBuilder.append(this.mSupportDisplayEngine3DColorTemperature);
        Slog.d(str, stringBuilder.toString());
        setColorTemperatureAccordingToSetting();
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        ContentResolver resolver = this.mContext.getContentResolver();
        if (this.mSupportDisplayEngine3DColorTemperature || isDisplayFeatureSupported(1)) {
            resolver.registerContentObserver(System.getUriFor("color_temperature_rgb"), false, this.mSettingsObserver, -1);
        } else {
            resolver.registerContentObserver(System.getUriFor("color_temperature"), false, this.mSettingsObserver, -1);
        }
        this.mFingerSenseObserver = new FingerSenseObserver(this.mHandler, resolver);
        this.mFingerSenseObserver.observe();
        IntentFilter headsetFilter = new IntentFilter();
        headsetFilter.addAction("android.intent.action.HEADSET_PLUG");
        this.mContext.registerReceiver(new HeadsetReceiver(this, null), headsetFilter, null, this.mHandler);
        headsetFilter = new IntentFilter();
        headsetFilter.addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED");
        headsetFilter.addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED");
        this.mContext.registerReceiver(new BluetoothReceiver(this, null), headsetFilter, null, this.mHandler);
        this.mIntelliIntent = new Intent("com.huawei.intelliServer.intelliServer");
        this.mIntelliIntent.setClassName("com.huawei.intelliServer.intelliServer", SERVICE_CLASS);
    }

    public int getAdjustedMaxTimeout(int oldtimeout, int maxv) {
        if (this.mWindowManagerInternal == null || this.mPolicy == null || this.mWindowManagerInternal.isCoverOpen() || this.mPolicy.isKeyguardLocked() || isPhoneInCall()) {
            return 0;
        }
        return 10000;
    }

    public void handleWakeLockDeath(WakeLock wakeLock) {
        synchronized (this.mLock) {
            int i;
            WakeLock wl;
            String str;
            StringBuilder stringBuilder;
            if (DEBUG_SPEW) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("handleWakeLockDeath: lock=");
                stringBuilder2.append(Objects.hashCode(wakeLock.mLock));
                stringBuilder2.append(" [");
                stringBuilder2.append(wakeLock.mTag);
                stringBuilder2.append("]");
                Slog.d(str2, stringBuilder2.toString());
            }
            for (i = this.mForceReleasedWakeLocks.size() - 1; i >= 0; i--) {
                wl = (WakeLock) this.mForceReleasedWakeLocks.get(i);
                if (wl.mLock == wakeLock.mLock) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("remove from forceReleased wl: ");
                    stringBuilder.append(wakeLock);
                    stringBuilder.append(" mForceReleasedWakeLocks:");
                    stringBuilder.append(wl);
                    Log.d(str, stringBuilder.toString());
                    this.mForceReleasedWakeLocks.remove(i);
                }
            }
            for (i = this.mProxyedWakeLocks.size() - 1; i >= 0; i--) {
                wl = (WakeLock) this.mProxyedWakeLocks.get(i);
                if (wl.mLock == wakeLock.mLock) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("remove from proxyed wl: ");
                    stringBuilder.append(wakeLock);
                    stringBuilder.append(" mProxyedWakeLocks:");
                    stringBuilder.append(wl);
                    Log.d(str, stringBuilder.toString());
                    this.mProxyedWakeLocks.remove(i);
                }
            }
            super.handleWakeLockDeath(wakeLock);
        }
    }

    private void restoreProxyWakeLockLocked(int pid, int uid) {
        for (int i = this.mProxyedWakeLocks.size() - 1; i >= 0; i--) {
            WakeLock wakelock = (WakeLock) this.mProxyedWakeLocks.get(i);
            if (((wakelock.mOwnerUid == uid || -1 == uid) && (wakelock.mOwnerPid == pid || -1 == uid)) || (wakelock.mWorkSource != null && wakelock.mWorkSource.get(0) == uid)) {
                acquireWakeLockInternalLocked(wakelock.mLock, wakelock.mFlags, wakelock.mTag, wakelock.mPackageName, wakelock.mWorkSource, wakelock.mHistoryTag, wakelock.mOwnerUid, wakelock.mOwnerPid);
                this.mProxyedWakeLocks.remove(i);
            }
        }
    }

    private void removeProxyWakeLockProcessLocked(int pid, int uid) {
        for (int i = this.mProxyWLProcessList.size() - 1; i >= 0; i--) {
            if (((ProxyWLProcessInfo) this.mProxyWLProcessList.get(i)).isSameProcess(pid, uid)) {
                if (DEBUG_SPEW) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("remove pxy wl, pid: ");
                    stringBuilder.append(pid);
                    stringBuilder.append(", uid: ");
                    stringBuilder.append(uid);
                    stringBuilder.append(" from pxy process list.");
                    Log.d(str, stringBuilder.toString());
                }
                this.mProxyWLProcessList.remove(i);
            }
        }
    }

    public void proxyWakeLockByPidUid(int pid, int uid, boolean proxy) {
        proxyWakeLockByPidUid(pid, uid, proxy, true);
    }

    private void proxyWakeLockByPidUid(int pid, int uid, boolean proxy, boolean proxyWS) {
        synchronized (this.mLock) {
            if (true == proxy) {
                try {
                    this.mProxyWLProcessList.add(new ProxyWLProcessInfo(pid, uid, proxyWS));
                } catch (Throwable th) {
                }
            } else {
                restoreProxyWakeLockLocked(pid, uid);
                removeProxyWakeLockProcessLocked(pid, uid);
            }
        }
    }

    /* JADX WARNING: Missing block: B:25:0x006b, code skipped:
            if (DEBUG_SPEW == false) goto L_0x00ba;
     */
    /* JADX WARNING: Missing block: B:28:0x0071, code skipped:
            if (dropLogs(r13, r15) != false) goto L_0x00ba;
     */
    /* JADX WARNING: Missing block: B:29:0x0073, code skipped:
            r0 = TAG;
            r1 = new java.lang.StringBuilder();
            r1.append("acquire pxy wl : lock=");
     */
    /* JADX WARNING: Missing block: B:32:?, code skipped:
            r1.append(r31);
            r1.append(", uid: ");
            r1.append(r15);
            r1.append(", ws: ");
            r1.append(r14);
            r1.append(", packageName: ");
     */
    /* JADX WARNING: Missing block: B:35:?, code skipped:
            r1.append(r27);
            r1.append(", tag: ");
            r1.append(r13);
            android.util.Log.d(r0, r1.toString());
     */
    /* JADX WARNING: Missing block: B:38:0x00b3, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:39:0x00b4, code skipped:
            r2 = r27;
            r3 = r31;
     */
    /* JADX WARNING: Missing block: B:40:0x00ba, code skipped:
            r2 = r27;
            r3 = r31;
     */
    /* JADX WARNING: Missing block: B:42:0x00c5, code skipped:
            r1 = r1;
            r16 = r4;
            r17 = r5;
            r5 = r13;
            r18 = r6;
            r19 = r7;
            r20 = r8;
            r21 = r9;
            r13 = 0;
     */
    /* JADX WARNING: Missing block: B:45:0x00e4, code skipped:
            r1 = new com.android.server.power.PowerManagerService.WakeLock(r12, r24, r25, r5, r27, r14, r29, r15, r31, new com.android.server.power.PowerManagerService.UidState(r15));
     */
    /* JADX WARNING: Missing block: B:47:?, code skipped:
            r24.linkToDeath(r1, r13);
     */
    /* JADX WARNING: Missing block: B:49:?, code skipped:
            r12.mProxyedWakeLocks.add(r1);
     */
    /* JADX WARNING: Missing block: B:50:0x00f0, code skipped:
            monitor-exit(r21);
     */
    /* JADX WARNING: Missing block: B:52:0x00f2, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:53:0x00f3, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:54:0x00f4, code skipped:
            r3 = r0;
     */
    /* JADX WARNING: Missing block: B:55:0x00fc, code skipped:
            throw new java.lang.IllegalArgumentException("HW Wake lock is already dead.");
     */
    /* JADX WARNING: Missing block: B:56:0x00fd, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:57:0x00fe, code skipped:
            r2 = r24;
     */
    /* JADX WARNING: Missing block: B:66:0x011e, code skipped:
            r0 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected boolean acquireProxyWakeLock(IBinder lock, int flags, String tag, String packageName, WorkSource ws, String historyTag, int uid, int pid) {
        Throwable th;
        String str = tag;
        WorkSource workSource = ws;
        int i = uid;
        if (this.mSystemReady) {
            boolean z;
            Object obj = this.mLock;
            synchronized (obj) {
                IBinder iBinder;
                Object obj2;
                try {
                    int listSize = this.mProxyWLProcessList.size();
                    int i2 = 0;
                    while (true) {
                        int i3 = i2;
                        if (i3 < listSize) {
                            i2 = pid;
                            int iUid = i;
                            ProxyWLProcessInfo pwi = (ProxyWLProcessInfo) this.mProxyWLProcessList.get(i3);
                            if (pwi.mProxyWS && workSource != null) {
                                i2 = -1;
                                if (ws.size() > 0) {
                                    iUid = workSource.get(0);
                                } else {
                                    ArrayList<WorkChain> workChains = ws.getWorkChains();
                                    if (workChains == null || workChains.size() <= 0) {
                                        Log.w(TAG, "acquireProxyWakeLock, workChains is empty.");
                                    } else {
                                        iUid = ((WorkChain) workChains.get(0)).getAttributionUid();
                                    }
                                }
                            }
                            int iPid = i2;
                            int iUid2 = iUid;
                            if (pwi.isSameProcess(iPid, iUid2)) {
                                break;
                            }
                            iBinder = lock;
                            int i4 = listSize;
                            obj2 = obj;
                            z = false;
                            i2 = i3 + 1;
                            str = tag;
                        } else {
                            iBinder = lock;
                            z = false;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    iBinder = lock;
                    obj2 = obj;
                    throw th;
                }
            }
            return z;
        }
        Log.w(TAG, "acquireProxyWakeLock, mSystemReady is false.");
        return false;
    }

    private int findProxyWakeLockIndexLocked(IBinder lock) {
        int count = this.mProxyedWakeLocks.size();
        for (int i = 0; i < count; i++) {
            if (((WakeLock) this.mProxyedWakeLocks.get(i)).mLock == lock) {
                return i;
            }
        }
        return -1;
    }

    protected boolean updateProxyWakeLockWorkSource(IBinder lock, WorkSource ws, String historyTag, int callingUid) {
        synchronized (this.mLock) {
            int index = findProxyWakeLockIndexLocked(lock);
            if (index < 0) {
                return false;
            }
            WakeLock wakeLock = (WakeLock) this.mProxyedWakeLocks.get(index);
            if (DEBUG_SPEW) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("update ws pxy wl : lock=");
                stringBuilder.append(Objects.hashCode(lock));
                stringBuilder.append(" [");
                stringBuilder.append(wakeLock.mTag);
                stringBuilder.append("], ws=");
                stringBuilder.append(ws);
                stringBuilder.append(", curr.ws: ");
                stringBuilder.append(wakeLock.mWorkSource);
                Slog.d(str, stringBuilder.toString());
            }
            if (!wakeLock.hasSameWorkSource(ws)) {
                wakeLock.mHistoryTag = historyTag;
                wakeLock.updateWorkSource(ws);
            }
            return true;
        }
    }

    private boolean releaseWakeLockFromListLocked(IBinder lock, ArrayList<WakeLock> list) {
        int length = list.size();
        boolean ret = false;
        for (int i = length - 1; i >= 0; i--) {
            WakeLock wakelock = (WakeLock) list.get(i);
            if (wakelock.mLock == lock) {
                if (DEBUG_SPEW && !dropLogs(wakelock.mTag, wakelock.mOwnerUid)) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("release ws pxy wl : lock= wl:");
                    stringBuilder.append(wakelock);
                    stringBuilder.append(" from list, length: ");
                    stringBuilder.append(length);
                    Log.d(str, stringBuilder.toString());
                }
                try {
                    lock.unlinkToDeath(wakelock, 0);
                } catch (NoSuchElementException e) {
                    Log.d(TAG, "release ws pxy wl, no such Element");
                }
                list.remove(i);
                ret = true;
            }
        }
        return ret;
    }

    protected boolean releaseProxyWakeLock(IBinder lock) {
        if (this.mSystemReady) {
            boolean ret;
            synchronized (this.mLock) {
                ret = (false | releaseWakeLockFromListLocked(lock, this.mProxyedWakeLocks)) | releaseWakeLockFromListLocked(lock, this.mForceReleasedWakeLocks);
            }
            return ret;
        }
        Log.w(TAG, "releaseProxyWakeLock, mSystemReady is false.");
        return false;
    }

    private void releaseWakeLockInternalLocked(IBinder lock, int flags) {
        int index = findWakeLockIndexLocked(lock);
        if (index < 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("releaseWakeLockInternalLocked: lock=");
            stringBuilder.append(Objects.hashCode(lock));
            stringBuilder.append(" [not found], flags=0x");
            stringBuilder.append(Integer.toHexString(flags));
            Slog.w(str, stringBuilder.toString());
            return;
        }
        WakeLock wakeLock = (WakeLock) this.mWakeLocks.get(index);
        if (DEBUG_SPEW && !dropLogs(wakeLock.mTag, wakeLock.mOwnerUid)) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("releaseWakeLockInternalLocked: lock=");
            stringBuilder2.append(Objects.hashCode(lock));
            stringBuilder2.append(", flags=0x");
            stringBuilder2.append(Integer.toHexString(flags));
            stringBuilder2.append(", tag=\"");
            stringBuilder2.append(wakeLock.mTag);
            stringBuilder2.append("\", packageName=");
            stringBuilder2.append(wakeLock.mPackageName);
            stringBuilder2.append("\", ws=");
            stringBuilder2.append(wakeLock.mWorkSource);
            stringBuilder2.append(", uid=");
            stringBuilder2.append(wakeLock.mOwnerUid);
            stringBuilder2.append(", pid=");
            stringBuilder2.append(wakeLock.mOwnerPid);
            Slog.d(str2, stringBuilder2.toString());
        }
        if ((flags & 1) != 0) {
            this.mRequestWaitForNegativeProximity = true;
        }
        removeWakeLockLocked(wakeLock, index);
    }

    private void acquireWakeLockInternalLocked(IBinder lock, int flags, String tag, String packageName, WorkSource ws, String historyTag, int uid, int pid) {
        String str;
        StringBuilder stringBuilder;
        String str2 = tag;
        String str3 = packageName;
        WorkSource workSource = ws;
        int i = uid;
        int i2 = pid;
        boolean dropLogs = dropLogs(str2, i);
        if (DEBUG_SPEW && !dropLogs) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("acquireWakeLockInternalLocked: lock=");
            stringBuilder.append(Objects.hashCode(lock));
            stringBuilder.append(", flags=0x");
            stringBuilder.append(Integer.toHexString(flags));
            stringBuilder.append(", tag=\"");
            stringBuilder.append(str2);
            stringBuilder.append("\", packageName=");
            stringBuilder.append(str3);
            stringBuilder.append("\", ws=");
            stringBuilder.append(workSource);
            stringBuilder.append(", uid=");
            stringBuilder.append(i);
            stringBuilder.append(", pid=");
            stringBuilder.append(i2);
            Slog.d(str, stringBuilder.toString());
        }
        if (lock.isBinderAlive()) {
            WakeLock wakeLock;
            int i3;
            WakeLock wakeLock2;
            int index = findWakeLockIndexLocked(lock);
            if (index >= 0) {
                if (DEBUG_SPEW && !dropLogs) {
                    String str4 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("acquireWakeLockInternalLocked: lock=");
                    stringBuilder2.append(Objects.hashCode(lock));
                    stringBuilder2.append(", existing wakelock");
                    Slog.d(str4, stringBuilder2.toString());
                }
                WakeLock wakeLock3 = (WakeLock) this.mWakeLocks.get(index);
                if (wakeLock3.hasSameProperties(flags, str2, workSource, i, i2)) {
                } else {
                    notifyWakeLockChangingLocked(wakeLock3, flags, str2, str3, i, i2, workSource, historyTag);
                    wakeLock3.updateProperties(flags, str2, str3, workSource, historyTag, i, i2);
                }
                wakeLock = null;
                IBinder iBinder = lock;
                i3 = i;
                wakeLock2 = wakeLock3;
            } else {
                i3 = i;
                wakeLock = new WakeLock(this, lock, flags, str2, str3, workSource, historyTag, i, pid, new UidState(i));
                try {
                    lock.linkToDeath(wakeLock, 0);
                    this.mWakeLocks.add(wakeLock);
                    setWakeLockDisabledStateLocked(wakeLock);
                    wakeLock2 = wakeLock;
                    wakeLock = 1;
                } catch (RemoteException ex) {
                    RemoteException remoteException = ex;
                    throw new IllegalArgumentException("Wake lock is already dead.");
                }
            }
            applyWakeLockFlagsOnAcquireLocked(wakeLock2, i3);
            this.mDirty = 1 | this.mDirty;
            updatePowerStateLocked();
            if (wakeLock != null) {
                notifyWakeLockAcquiredLocked(wakeLock2);
            }
            return;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("lock:");
        stringBuilder.append(Objects.hashCode(lock));
        stringBuilder.append(" is already dead, tag=\"");
        stringBuilder.append(str2);
        stringBuilder.append("\", packageName=");
        stringBuilder.append(str3);
        stringBuilder.append(", ws=");
        stringBuilder.append(workSource);
        stringBuilder.append(", uid=");
        stringBuilder.append(i);
        stringBuilder.append(", pid=");
        stringBuilder.append(i2);
        Slog.w(str, stringBuilder.toString());
    }

    public boolean proxyedWakeLock(int subType, List<String> value) {
        if (value == null || value.size() != 2) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invaild para for :");
            stringBuilder.append(subType);
            stringBuilder.append(" value:");
            stringBuilder.append(value);
            Log.w(str, stringBuilder.toString());
            return false;
        }
        boolean ret = true;
        try {
            int pid = Integer.parseInt((String) value.get(0));
            int uid = Integer.parseInt((String) value.get(1));
            switch (subType) {
                case 1:
                    dropProxyedWakeLock(pid, uid);
                    break;
                case 2:
                    proxyWakeLockByPidUid(pid, uid, true, false);
                    break;
                case 3:
                    forceReleaseWakeLockByPidUid(pid, uid, false);
                    break;
                default:
                    ret = false;
                    break;
            }
        } catch (Exception ex) {
            Log.e(TAG, "proxyedWakeLock Exception !", ex);
            ret = false;
        }
        return ret;
    }

    private void dropProxyedWakeLock(int pid, int uid) {
        synchronized (this.mLock) {
            int i;
            WakeLock wl;
            for (i = this.mForceReleasedWakeLocks.size() - 1; i >= 0; i--) {
                wl = (WakeLock) this.mForceReleasedWakeLocks.get(i);
                if (((wl.mOwnerUid == uid || -1 == uid) && (wl.mOwnerPid == pid || -1 == pid)) || (-1 == pid && wl.mWorkSource != null && wl.mWorkSource.get(0) == uid)) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("drop from forceReleased wl: ");
                    stringBuilder.append(wl);
                    stringBuilder.append(" mForceReleasedWakeLocks:");
                    stringBuilder.append(wl);
                    Log.d(str, stringBuilder.toString());
                    this.mForceReleasedWakeLocks.remove(i);
                    wl.mLock.unlinkToDeath(wl, 0);
                }
            }
            for (i = this.mProxyedWakeLocks.size() - 1; i >= 0; i--) {
                wl = (WakeLock) this.mProxyedWakeLocks.get(i);
                if ((-1 == uid && -1 == pid) || ((wl.mOwnerUid == uid && (wl.mOwnerPid == pid || -1 == pid)) || (-1 == pid && wl.mWorkSource != null && wl.mWorkSource.get(0) == uid))) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("drop from proxyed wl: ");
                    stringBuilder2.append(wl);
                    stringBuilder2.append(" mProxyedWakeLocks:");
                    stringBuilder2.append(wl);
                    Log.d(str2, stringBuilder2.toString());
                    this.mProxyedWakeLocks.remove(i);
                    wl.mLock.unlinkToDeath(wl, 0);
                }
            }
        }
    }

    public void forceReleaseWakeLockByPidUid(int pid, int uid) {
        forceReleaseWakeLockByPidUid(pid, uid, true);
    }

    private void forceReleaseWakeLockByPidUid(int pid, int uid, boolean releaseWS) {
        Throwable th;
        int length = pid;
        int i = uid;
        synchronized (this.mLock) {
            int i2;
            int i3;
            try {
                int size = this.mWakeLocks.size();
                int i4 = size - 1;
                while (true) {
                    int i5 = i4;
                    if (i5 >= 0) {
                        int size2;
                        int i6;
                        WakeLock wakelock = (WakeLock) this.mWakeLocks.get(i5);
                        String str;
                        if (!releaseWS || wakelock.mWorkSource == null) {
                            size2 = size;
                            WakeLock wakelock2 = wakelock;
                            i6 = i5;
                            if (wakelock2.mWorkSource == null) {
                                i2 = pid;
                                if (wakelock2.mOwnerPid == i2) {
                                    try {
                                        i3 = uid;
                                        if (wakelock2.mOwnerUid == i3) {
                                            if (DEBUG_SPEW) {
                                                str = TAG;
                                                StringBuilder stringBuilder = new StringBuilder();
                                                stringBuilder.append("forceReleaseWakeLockByPidUid, ws null, pid: ");
                                                stringBuilder.append(i2);
                                                stringBuilder.append(", uid: ");
                                                stringBuilder.append(i3);
                                                stringBuilder.append(", wakelock: ");
                                                stringBuilder.append(wakelock2);
                                                Log.d(str, stringBuilder.toString());
                                            }
                                            this.mForceReleasedWakeLocks.add(wakelock2);
                                            releaseWakeLockInternalLocked(wakelock2.mLock, wakelock2.mFlags);
                                        }
                                        i4 = i6 - 1;
                                        length = i2;
                                        i = i3;
                                        size = size2;
                                    } catch (Throwable th2) {
                                        th = th2;
                                        throw th;
                                    }
                                }
                                i3 = uid;
                                i4 = i6 - 1;
                                length = i2;
                                i = i3;
                                size = size2;
                            }
                        } else {
                            int length2 = wakelock.mWorkSource.size();
                            i4 = 0;
                            StringBuilder stringBuilder2;
                            if (1 == length2) {
                                if (wakelock.mWorkSource.get(0) == i) {
                                    if (DEBUG_SPEW) {
                                        str = TAG;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("forceReleaseWakeLockByPidUid, last one, wakelock: ");
                                        stringBuilder2.append(wakelock);
                                        Log.d(str, stringBuilder2.toString());
                                    }
                                    this.mForceReleasedWakeLocks.add(wakelock);
                                    releaseWakeLockInternalLocked(wakelock.mLock, wakelock.mFlags);
                                }
                                size2 = size;
                                size = wakelock;
                                i6 = i5;
                            } else if (length2 > 1) {
                                while (true) {
                                    int j = i4;
                                    if (j >= length2) {
                                        break;
                                    }
                                    int j2;
                                    if (wakelock.mWorkSource.get(j) == i) {
                                        WorkSource workSource;
                                        if (DEBUG_SPEW) {
                                            str = TAG;
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("forceReleaseWakeLockByPidUid, more than one, wakelock: ");
                                            stringBuilder2.append(wakelock);
                                            Log.d(str, stringBuilder2.toString());
                                        }
                                        String name = wakelock.mWorkSource.getName(j);
                                        if (name == null) {
                                            workSource = new WorkSource(i);
                                        } else {
                                            workSource = new WorkSource(i, name);
                                        }
                                        WorkSource workSource2 = workSource;
                                        UidState state = new UidState(i);
                                        IBinder iBinder = wakelock.mLock;
                                        int i7 = wakelock.mFlags;
                                        String str2 = wakelock.mTag;
                                        str = wakelock.mPackageName;
                                        size2 = size;
                                        size = wakelock.mHistoryTag;
                                        i6 = i5;
                                        try {
                                            String str3 = str;
                                            WakeLock wakeLock = wakeLock;
                                            WorkSource workSource3 = workSource2;
                                            name = str3;
                                            j2 = j;
                                            length = length2;
                                            String str4 = size;
                                            size = wakelock;
                                            WakeLock cacheWakelock = new WakeLock(this, iBinder, i7, str2, name, workSource3, str4, wakelock.mOwnerUid, wakelock.mOwnerPid, state);
                                            this.mForceReleasedWakeLocks.add(cacheWakelock);
                                            WorkSource newWorkSource = new WorkSource(size.mWorkSource);
                                            WorkSource workSource4 = workSource3;
                                            newWorkSource.remove(workSource4);
                                            i5 = workSource4;
                                            notifyWakeLockChangingLocked(size, size.mFlags, size.mTag, size.mPackageName, size.mOwnerUid, size.mOwnerPid, newWorkSource, size.mHistoryTag);
                                            size.mWorkSource.remove(i5);
                                        } catch (Throwable th3) {
                                            th = th3;
                                            i2 = pid;
                                            i3 = uid;
                                            throw th;
                                        }
                                    }
                                    size2 = size;
                                    j2 = j;
                                    length = length2;
                                    size = wakelock;
                                    i6 = i5;
                                    i4 = j2 + 1;
                                    wakelock = size;
                                    length2 = length;
                                    size = size2;
                                    i5 = i6;
                                    length = pid;
                                    i = uid;
                                }
                                size2 = size;
                                length = length2;
                                size = wakelock;
                                i6 = i5;
                            } else {
                                size2 = size;
                                length = length2;
                                size = wakelock;
                                i6 = i5;
                                str = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("forceReleaseWakeLockByPidUid, length invalid: ");
                                stringBuilder2.append(length);
                                Log.e(str, stringBuilder2.toString());
                            }
                        }
                        i2 = pid;
                        i3 = uid;
                        i4 = i6 - 1;
                        length = i2;
                        i = i3;
                        size = size2;
                    } else {
                        i2 = length;
                        i3 = i;
                        return;
                    }
                }
            } catch (Throwable th4) {
                th = th4;
                i2 = length;
                i3 = i;
                throw th;
            }
        }
    }

    public void forceRestoreWakeLockByPidUid(int pid, int uid) {
        int i = pid;
        int i2 = uid;
        synchronized (this.mLock) {
            int size = this.mForceReleasedWakeLocks.size();
            int i3 = size - 1;
            while (true) {
                int i4 = i3;
                if (i4 >= 0) {
                    int size2;
                    WakeLock wakelock = (WakeLock) this.mForceReleasedWakeLocks.get(i4);
                    String str;
                    StringBuilder stringBuilder;
                    if (wakelock.mWorkSource == null) {
                        if (wakelock.mOwnerPid != i) {
                            if (-1 == i) {
                            }
                        }
                        if (wakelock.mOwnerUid == i2 || -1 == i2) {
                            if (DEBUG_SPEW) {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("forceRestoreWakeLockByPidUid, WorkSource == null, wakelock: ");
                                stringBuilder.append(wakelock);
                                Log.d(str, stringBuilder.toString());
                            }
                            acquireWakeLockInternalLocked(wakelock.mLock, wakelock.mFlags, wakelock.mTag, wakelock.mPackageName, wakelock.mWorkSource, wakelock.mHistoryTag, wakelock.mOwnerUid, wakelock.mOwnerPid);
                            this.mForceReleasedWakeLocks.remove(i4);
                        }
                    } else if (wakelock.mWorkSource.get(0) == i2 || -1 == i2) {
                        int index = findWakeLockIndexLocked(wakelock.mLock);
                        if (index < 0) {
                            if (DEBUG_SPEW) {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("forceRestoreWakeLockByPidUid, not found base, wakelock: ");
                                stringBuilder.append(wakelock);
                                Log.d(str, stringBuilder.toString());
                            }
                            size2 = size;
                            size = index;
                            acquireWakeLockInternalLocked(wakelock.mLock, wakelock.mFlags, wakelock.mTag, wakelock.mPackageName, wakelock.mWorkSource, wakelock.mHistoryTag, wakelock.mOwnerUid, wakelock.mOwnerPid);
                        } else {
                            size2 = size;
                            size = index;
                            if (DEBUG_SPEW) {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("forceRestoreWakeLockByPidUid, update exist, wakelock: ");
                                stringBuilder.append(wakelock);
                                Log.d(str, stringBuilder.toString());
                            }
                            WorkSource newWorkSource = new WorkSource(((WakeLock) this.mWakeLocks.get(size)).mWorkSource);
                            newWorkSource.add(wakelock.mWorkSource);
                            notifyWakeLockChangingLocked((WakeLock) this.mWakeLocks.get(size), wakelock.mFlags, wakelock.mTag, wakelock.mPackageName, wakelock.mOwnerUid, wakelock.mOwnerPid, newWorkSource, wakelock.mHistoryTag);
                            ((WakeLock) this.mWakeLocks.get(size)).mWorkSource.add(wakelock.mWorkSource);
                        }
                        this.mForceReleasedWakeLocks.remove(i4);
                        i3 = i4 - 1;
                        size = size2;
                    }
                    size2 = size;
                    i3 = i4 - 1;
                    size = size2;
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0031, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean getWakeLockByUid(int uid, int wakeflag) {
        synchronized (this.mLock) {
            if (this.mWakeLocks.size() <= 0) {
                return false;
            }
            Iterator<WakeLock> it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                WakeLock wl = (WakeLock) it.next();
                if (wl.mOwnerUid == uid) {
                    if (-1 == wakeflag || (wl.mFlags & 65535) == wakeflag) {
                    }
                } else if (wl.mWorkSource != null) {
                    int size = wl.mWorkSource.size();
                    for (int i = 0; i < size; i++) {
                        if (uid == wl.mWorkSource.get(i) && (-1 == wakeflag || (wl.mFlags & 65535) == wakeflag)) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("worksource not null, i:");
                            stringBuilder.append(i);
                            stringBuilder.append(", size: ");
                            stringBuilder.append(size);
                            stringBuilder.append(", flags: ");
                            stringBuilder.append(wl.mFlags);
                            Log.d(str, stringBuilder.toString());
                            return true;
                        }
                    }
                    continue;
                } else {
                    continue;
                }
            }
            return false;
        }
    }

    public void setLcdRatio(int ratio, boolean autoAdjust) {
        this.mLightsManager.getLight(0).setLcdRatio(ratio, autoAdjust);
    }

    public void configBrightnessRange(int ratioMin, int ratioMax, int autoLimit) {
        this.mLightsManager.getLight(0).configBrightnessRange(ratioMin, ratioMax, autoLimit);
    }

    private void enableBrightnessWaitLocked() {
        if (!this.mBrightnessWaitModeEnabled) {
            this.mBrightnessWaitModeEnabled = true;
            this.mBrightnessWaitRet = false;
            this.mSkipWaitKeyguardDismiss = false;
            this.mDirty |= 16384;
            Message msg = this.mHandler.obtainMessage(101);
            msg.setAsynchronous(true);
            this.mHandler.sendMessageDelayed(msg, (long) this.mWaitBrightTimeout);
        }
        this.mAuthSucceeded = false;
    }

    protected void disableBrightnessWaitLocked(boolean enableBright) {
        disableBrightnessWaitLocked(enableBright, false);
    }

    protected void disableBrightnessWaitLocked(boolean enableBright, boolean skipWaitKeyguardDismiss) {
        if (this.mBrightnessWaitModeEnabled) {
            this.mHandler.removeMessages(101);
            this.mBrightnessWaitModeEnabled = false;
            this.mBrightnessWaitRet = enableBright;
            this.mSkipWaitKeyguardDismiss = skipWaitKeyguardDismiss;
            this.mDirty |= 16384;
        }
        this.mAuthSucceeded = false;
    }

    protected void setAuthSucceededInternal() {
        synchronized (this.mLock) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setAuthSucceededInternal, mBrightnessWaitModeEnabled = ");
                stringBuilder.append(this.mBrightnessWaitModeEnabled);
                Slog.d(str, stringBuilder.toString());
            }
            this.mAuthSucceeded = true;
        }
    }

    protected void startWakeUpReadyInternal(long eventTime, int uid, String opPackageName) {
        synchronized (this.mLock) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("UL_Power startWakeUpReadyInternal, mBrightnessWaitModeEnabled = ");
                stringBuilder.append(this.mBrightnessWaitModeEnabled);
                Slog.d(str, stringBuilder.toString());
            }
            if (this.mBrightnessWaitModeEnabled) {
                resetWaitBrightTimeoutLocked();
            } else {
                if (!FingerViewController.PKGNAME_OF_KEYGUARD.equals(opPackageName)) {
                    this.mPolicy.setPickUpFlag();
                    this.mPolicy.setSyncPowerStateFlag();
                }
                if (wakeUpNoUpdateWithoutInteractiveLocked(eventTime, "startWakeUpReady", uid, opPackageName)) {
                    enableBrightnessWaitLocked();
                    updatePowerStateLocked();
                }
            }
        }
    }

    protected void setPowerStateInternal(boolean state) {
        String para = state ? "1" : "0";
        setPowerStateValue(NORMAL_CHG_ENABLE_PATH, para);
        setPowerStateValue(DIRECT_CHG_ENABLE_PATH_HISI, para);
    }

    public int getDisplayPanelTypeInternal() {
        try {
            if (!mLoadLibraryFailed) {
                return nativeGetDisplayPanelType();
            }
            Slog.d(TAG, "nativeGetDisplayPanelType failed because of library not found!");
            return -1;
        } catch (UnsatisfiedLinkError e) {
            Slog.d(TAG, "nativeGetDisplayPanelType not found!");
            return -1;
        }
    }

    protected int hwBrightnessSetDataInternal(String name, Bundle data) {
        int[] result = new int[]{0};
        if (this.mDisplayManagerInternal.hwBrightnessSetData(name, data, result)) {
            return result[0];
        }
        HwBrightnessInnerProcessor processor = (HwBrightnessInnerProcessor) this.mHwBrightnessInnerProcessors.get(name);
        if (processor != null) {
            return processor.setData(data);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("There is no process to deal with setData(");
        stringBuilder.append(name);
        stringBuilder.append(")");
        Slog.w(str, stringBuilder.toString());
        return -1;
    }

    protected int hwBrightnessGetDataInternal(String name, Bundle data) {
        int[] result = new int[]{0};
        if (this.mDisplayManagerInternal.hwBrightnessGetData(name, data, result)) {
            return result[0];
        }
        HwBrightnessInnerProcessor processor = (HwBrightnessInnerProcessor) this.mHwBrightnessInnerProcessors.get(name);
        if (processor != null) {
            return processor.getData(data);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("There is no process to deal with getData(");
        stringBuilder.append(name);
        stringBuilder.append(")");
        Slog.w(str, stringBuilder.toString());
        return -1;
    }

    protected int hwBrightnessRegisterCallbackInternal(IHwBrightnessCallback cb, List<String> filter) {
        synchronized (this.mLockHwBrightnessCallbacks) {
            this.mHwBrightnessCallbacks.add(new HwBrightnessCallbackData(cb, filter));
        }
        return 0;
    }

    protected int hwBrightnessUnregisterCallbackInternal(IHwBrightnessCallback cb) {
        synchronized (this.mLockHwBrightnessCallbacks) {
            int size = this.mHwBrightnessCallbacks.size();
            for (int i = 0; i < size; i++) {
                if (((HwBrightnessCallbackData) this.mHwBrightnessCallbacks.get(i)).getCB() == cb) {
                    this.mHwBrightnessCallbacks.remove(i);
                    return 0;
                }
            }
            Slog.i(TAG, "Unknown callback!");
            return -1;
        }
    }

    private boolean needHwBrightnessNotify(String what, List<String> filter) {
        if (filter == null) {
            return true;
        }
        int size = filter.size();
        for (int i = 0; i < size; i++) {
            if (what.equals(filter.get(i))) {
                return true;
            }
        }
        return false;
    }

    protected void notifyHwBrightnessCallbacks(String what, int arg1, int arg2, Bundle data) {
        Throwable th;
        synchronized (this.mLockHwBrightnessCallbacks) {
            String str;
            try {
                int size = this.mHwBrightnessCallbacks.size();
                int i = 0;
                while (true) {
                    int i2 = i;
                    if (i2 < size) {
                        HwBrightnessCallbackData d = (HwBrightnessCallbackData) this.mHwBrightnessCallbacks.get(i2);
                        str = what;
                        if (needHwBrightnessNotify(str, d.getFilter())) {
                            final HwBrightnessCallbackData hwBrightnessCallbackData = d;
                            final String str2 = str;
                            final int i3 = arg1;
                            final int i4 = arg2;
                            final Bundle bundle = data;
                            new Thread(new Runnable() {
                                public void run() {
                                    try {
                                        hwBrightnessCallbackData.getCB().onStatusChanged(str2, i3, i4, bundle);
                                    } catch (RemoteException e) {
                                        String str = HwPowerManagerService.TAG;
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("Failed to notify callback! Error:");
                                        stringBuilder.append(e.getMessage());
                                        Slog.w(str, stringBuilder.toString());
                                    }
                                }
                            }).start();
                        }
                        i = i2 + 1;
                    } else {
                        str = what;
                        return;
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    private void loadHwBrightnessInnerProcessors() {
    }

    protected void stopPickupTrunOff() {
        if (PickUpWakeScreenManager.isPickupSensorSupport(this.mContext) && PickUpWakeScreenManager.getInstance() != null) {
            PickUpWakeScreenManager.getInstance().stopTrunOffScrren();
        }
    }

    protected void resetWaitBrightTimeoutLocked() {
        this.mHandler.removeMessages(101);
        Message msg = this.mHandler.obtainMessage(101);
        msg.setAsynchronous(true);
        this.mHandler.sendMessageDelayed(msg, (long) this.mWaitBrightTimeout);
    }

    protected void stopWakeUpReadyInternal(long eventTime, int uid, boolean enableBright, String opPackageName) {
        synchronized (this.mLock) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("UL_Power stopWakeUpReadyInternal, mBrightnessWaitModeEnabled = ");
                stringBuilder.append(this.mBrightnessWaitModeEnabled);
                stringBuilder.append(" enableBright = ");
                stringBuilder.append(enableBright);
                Slog.d(str, stringBuilder.toString());
            }
            if (this.mBrightnessWaitModeEnabled) {
                if (!FingerViewController.PKGNAME_OF_KEYGUARD.equals(opPackageName)) {
                    this.mPolicy.setSyncPowerStateFlag();
                }
                if (enableBright) {
                    this.mLastWakeTime = eventTime;
                    setWakefulnessLocked(1, 0);
                    userActivityNoUpdateLocked(eventTime, 0, 0, uid);
                    disableBrightnessWaitLocked(true, FingerViewController.PKGNAME_OF_KEYGUARD.equals(opPackageName) ^ 1);
                    updatePowerStateLocked();
                } else {
                    goToSleepNoUpdateLocked(eventTime, 0, 0, uid);
                    updatePowerStateLocked();
                }
            } else if (enableBright) {
                if (DEBUG) {
                    Slog.d(TAG, "UL_Power stopWakeUpReadyInternal, brightness wait timeout.");
                }
                if (wakeUpNoUpdateLocked(eventTime, "BrightnessWaitTimeout", uid, opPackageName, uid)) {
                    updatePowerStateLocked();
                }
            }
        }
    }

    private boolean wakeUpNoUpdateWithoutInteractiveLocked(long eventTime, String reason, int uid, String opPackageName) {
        String str;
        if (DEBUG_SPEW) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("UL_Power wakeUpNoUpdateWithoutInteractiveLocked: eventTime=");
            stringBuilder.append(eventTime);
            stringBuilder.append(", uid=");
            stringBuilder.append(uid);
            Slog.d(str, stringBuilder.toString());
        }
        if (eventTime < this.mLastSleepTime || this.mWakefulness == 1 || !this.mBootCompleted || !this.mSystemReady || this.mProximityPositive) {
            notifyWakeupResult(false);
            return false;
        }
        if (mSupportFaceDetect) {
            startIntelliService();
        }
        Trace.traceBegin(131072, "wakeUpWithoutInteractive");
        try {
            int i = this.mWakefulness;
            StringBuilder stringBuilder2;
            if (i != 0) {
                switch (i) {
                    case 2:
                        str = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("UL_Power Waking up from dream (uid ");
                        stringBuilder2.append(uid);
                        stringBuilder2.append(")...");
                        Slog.i(str, stringBuilder2.toString());
                        Jlog.d(6, "JL_PMS_WAKEFULNESS_DREAMING");
                        break;
                    case 3:
                        str = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("UL_Power Waking up from dozing (uid ");
                        stringBuilder2.append(uid);
                        stringBuilder2.append(")...");
                        Slog.i(str, stringBuilder2.toString());
                        Jlog.d(7, "JL_PMS_WAKEFULNESS_NAPPING");
                        break;
                    default:
                        break;
                }
            }
            str = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("UL_Power Waking up from sleep (uid ");
            stringBuilder2.append(uid);
            stringBuilder2.append(")...");
            Slog.i(str, stringBuilder2.toString());
            Jlog.d(5, "JL_PMS_WAKEFULNESS_ASLEEP");
            this.mLastWakeTime = eventTime;
            this.mDirty |= 2;
            this.mWakefulness = 1;
            this.mNotifier.onWakeUp(reason, uid, opPackageName, uid);
            userActivityNoUpdateLocked(eventTime, 0, 0, uid);
            return true;
        } finally {
            Trace.traceEnd(131072);
        }
    }

    protected void handleWaitBrightTimeout() {
        synchronized (this.mLock) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("UL_Power handleWaitBrightTimeout mBrightnessWaitModeEnabled = ");
                stringBuilder.append(this.mBrightnessWaitModeEnabled);
                stringBuilder.append(" mWakefulness = ");
                stringBuilder.append(this.mWakefulness);
                Slog.d(str, stringBuilder.toString());
            }
            if (this.mBrightnessWaitModeEnabled) {
                if (!goToSleepNoUpdateLocked(SystemClock.uptimeMillis(), 101, 0, 1000)) {
                    disableBrightnessWaitLocked(false);
                }
                updatePowerStateLocked();
            }
        }
    }

    public boolean isAppWakeLockFilterTag(int flags, String packageName, WorkSource ws) {
        if (this.mPGManagerInternal == null) {
            this.mPGManagerInternal = (PGManagerInternal) LocalServices.getService(PGManagerInternal.class);
        }
        if (this.mPGManagerInternal != null) {
            return this.mPGManagerInternal.isGmsWakeLockFilterTag(flags, packageName, ws);
        }
        return false;
    }

    public boolean isSkipWakeLockUsing(int uid, String tag) {
        synchronized (this.mLock) {
            if (tag == null) {
                try {
                    return false;
                } catch (Throwable th) {
                }
            } else if (this.mWakeLocks.size() <= 0) {
                return false;
            } else {
                Iterator<WakeLock> it = this.mWakeLocks.iterator();
                while (it.hasNext()) {
                    WakeLock wl = (WakeLock) it.next();
                    if (wl.mOwnerUid == uid) {
                        if (tag.equals(wl.mTag)) {
                            return true;
                        }
                    } else if (wl.mWorkSource != null) {
                        int size = wl.mWorkSource.size();
                        for (int i = 0; i < size; i++) {
                            if (uid == wl.mWorkSource.get(i) && tag.equals(wl.mTag)) {
                                return true;
                            }
                        }
                        continue;
                    } else {
                        continue;
                    }
                }
                return false;
            }
        }
    }

    public void dumpInternal(PrintWriter pw) {
        super.dumpInternal(pw);
        pw.println("Proxyed WakeLocks State");
        synchronized (this.mLock) {
            WakeLock wl;
            StringBuilder stringBuilder;
            Iterator it = this.mProxyedWakeLocks.iterator();
            while (it.hasNext()) {
                wl = (WakeLock) it.next();
                stringBuilder = new StringBuilder();
                stringBuilder.append(" Proxyed WakeLocks :");
                stringBuilder.append(wl);
                pw.println(stringBuilder.toString());
            }
            it = this.mForceReleasedWakeLocks.iterator();
            while (it.hasNext()) {
                wl = (WakeLock) it.next();
                stringBuilder = new StringBuilder();
                stringBuilder.append(" Force Released WakeLocks :");
                stringBuilder.append(wl);
                pw.println(stringBuilder.toString());
            }
        }
    }

    protected void sendTempBrightnessToMonitor(String paramType, int brightness) {
        if (this.mDisplayEffectMonitor != null) {
            String[] packageName = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid()).split(":");
            if (packageName.length != 0) {
                ArrayMap<String, Object> params = new ArrayMap();
                params.put(MonitorModule.PARAM_TYPE, paramType);
                params.put("brightness", Integer.valueOf(brightness));
                params.put("packageName", packageName[0]);
                this.mDisplayEffectMonitor.sendMonitorParam(params);
            }
        }
    }

    protected void sendBrightnessModeToMonitor(boolean manualMode, String packageName) {
        if (this.mDisplayEffectMonitor != null) {
            if (!this.mHadSendBrightnessModeToMonitor) {
                packageName = "android";
                this.mHadSendBrightnessModeToMonitor = true;
            }
            ArrayMap<String, Object> params = new ArrayMap();
            params.put(MonitorModule.PARAM_TYPE, "brightnessMode");
            params.put("brightnessMode", manualMode ? "MANUAL" : "AUTO");
            params.put("packageName", packageName);
            this.mDisplayEffectMonitor.sendMonitorParam(params);
        }
    }

    protected void sendManualBrightnessToMonitor(int brightness, String packageName) {
        if (this.mDisplayEffectMonitor != null) {
            if (!this.mHadSendManualBrightnessToMonitor) {
                packageName = "android";
                this.mHadSendManualBrightnessToMonitor = true;
            }
            ArrayMap<String, Object> params = new ArrayMap();
            params.put(MonitorModule.PARAM_TYPE, "manualBrightness");
            params.put("brightness", Integer.valueOf(brightness));
            params.put("packageName", packageName);
            this.mDisplayEffectMonitor.sendMonitorParam(params);
        }
    }

    protected void sendBootCompletedToMonitor() {
        if (this.mDisplayEffectMonitor != null) {
            ArrayMap<String, Object> params = new ArrayMap();
            params.put(MonitorModule.PARAM_TYPE, "bootCompleted");
            this.mDisplayEffectMonitor.sendMonitorParam(params);
        }
    }

    protected void notifyWakeupResult(boolean isWakenupThisTime) {
        AwareFakeActivityRecg.self().notifyWakeupResult(isWakenupThisTime);
    }

    protected void startIntelliService() {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("bind IntelliService, mIntelliServiceBound=");
            stringBuilder.append(this.mIntelliServiceBound);
            Slog.d(str, stringBuilder.toString());
        }
        if (!this.mIntelliServiceBound) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    String str;
                    StringBuilder stringBuilder;
                    try {
                        HwPowerManagerService.this.mIntelliServiceBound = true;
                        boolean success = HwPowerManagerService.this.mContext.bindServiceAsUser(HwPowerManagerService.this.mIntelliIntent, HwPowerManagerService.this.connection, 1, UserHandle.CURRENT);
                        if (PowerManagerService.DEBUG) {
                            str = HwPowerManagerService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("bind IntelliService, success=");
                            stringBuilder.append(success);
                            Slog.d(str, stringBuilder.toString());
                        }
                        if (!success) {
                            HwPowerManagerService.this.resetFaceDetect();
                        }
                    } catch (SecurityException e) {
                        HwPowerManagerService.this.resetFaceDetect();
                        str = HwPowerManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("unable to start intelli service: ");
                        stringBuilder.append(HwPowerManagerService.this.mIntelliIntent);
                        Slog.e(str, stringBuilder.toString(), e);
                    } catch (Exception e2) {
                        HwPowerManagerService.this.resetFaceDetect();
                        str = HwPowerManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("unable to start intelli service: ");
                        stringBuilder.append(HwPowerManagerService.this.mIntelliIntent);
                        Slog.e(str, stringBuilder.toString(), e2);
                    }
                }
            });
        }
    }

    protected void stopIntelliService() {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unbind IntelliService, mIntelliServiceBound=");
            stringBuilder.append(this.mIntelliServiceBound);
            Slog.d(str, stringBuilder.toString());
        }
        if (this.mIntelliServiceBound) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    try {
                        HwPowerManagerService.this.mContext.unbindService(HwPowerManagerService.this.connection);
                    } catch (Exception e) {
                        Slog.e(HwPowerManagerService.TAG, "unbindService fail: ", e);
                    }
                }
            });
            resetFaceDetect();
        }
    }

    protected int registerFaceDetect() {
        if (!this.mIntelliServiceBound) {
            Slog.i(TAG, "IntelliService not started, face detect later");
            this.mShouldFaceDetectLater = true;
            startIntelliService();
            return -1;
        } else if (this.mIRemote == null || this.mFaceDetecting) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("register err, mFaceDetecting=");
            stringBuilder.append(this.mFaceDetecting);
            Slog.e(str, stringBuilder.toString());
            return -1;
        } else {
            try {
                int result = this.mIRemote.registListener(3, this.mIntelliListener);
                if (DEBUG) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("registListener, result=");
                    stringBuilder2.append(result);
                    Slog.d(str2, stringBuilder2.toString());
                }
                if (result != -1) {
                    this.mFaceDetecting = true;
                }
                return result;
            } catch (RemoteException e) {
                Slog.e(TAG, "registListener exption: ", e);
                return -1;
            } catch (Exception e2) {
                Slog.e(TAG, "registListener exption: ", e2);
                return -1;
            }
        }
    }

    protected int unregisterFaceDetect() {
        if (!this.mFaceDetecting) {
            return -1;
        }
        if (this.mIRemote == null) {
            Slog.e(TAG, "unregister err mIRemote is null!!");
            return -1;
        }
        try {
            int result = this.mIRemote.unregistListener(3, this.mIntelliListener);
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unregistListener, result=");
                stringBuilder.append(result);
                Slog.d(str, stringBuilder.toString());
            }
            if (result != -1) {
                this.mFaceDetecting = false;
            }
            return result;
        } catch (RemoteException e) {
            Slog.e(TAG, "unregistListener exption: ", e);
            return -1;
        } catch (Exception e2) {
            Slog.e(TAG, "unregistListener exption: ", e2);
            return -1;
        }
    }

    private void resetFaceDetect() {
        this.mIntelliServiceBound = false;
        this.mFaceDetecting = false;
        this.mShouldFaceDetectLater = false;
        this.mIRemote = null;
    }

    private void setPowerStateValue(String path, String para) {
        FileOutputStream fileOutputStream = null;
        IOException e;
        String str;
        StringBuilder stringBuilder;
        try {
            fileOutputStream = new FileOutputStream(path);
            fileOutputStream.write(para.getBytes(Charset.forName("UTF-8")));
            fileOutputStream.flush();
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("setPowerState as ");
            stringBuilder2.append(para);
            stringBuilder2.append(", path:");
            stringBuilder2.append(path);
            Slog.i(str2, stringBuilder2.toString());
            try {
                fileOutputStream.close();
                return;
            } catch (IOException e2) {
                e = e2;
                str = TAG;
                stringBuilder = new StringBuilder();
            }
            stringBuilder.append("setPowerStateValue:Error closing file: ");
            stringBuilder.append(e.toString());
            Slog.e(str, stringBuilder.toString());
        } catch (FileNotFoundException e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setPowerStateValue:File not found: ");
            stringBuilder.append(e3.toString());
            Slog.e(str, stringBuilder.toString());
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e4) {
                    e = e4;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (IOException e5) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setPowerStateValue:Error accessing file: ");
            stringBuilder.append(e5.toString());
            Slog.e(str, stringBuilder.toString());
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e6) {
                    e5 = e6;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (Exception e7) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setPowerStateValue:Exception occur: ");
            stringBuilder.append(e7.toString());
            Slog.e(str, stringBuilder.toString());
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e8) {
                    e5 = e8;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (Throwable th) {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e9) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("setPowerStateValue:Error closing file: ");
                    stringBuilder.append(e9.toString());
                    Slog.e(TAG, stringBuilder.toString());
                }
            }
        }
    }

    private boolean dropLogs(String tag, int uid) {
        if ("RILJ".equals(tag) && 1001 == uid) {
            return true;
        }
        return false;
    }

    protected void notifyWakeLockToIAware(int uid, int pid, String packageName, String Tag) {
        SysLoadManager.getInstance().notifyWakeLock(uid, pid, packageName, Tag);
    }

    protected void notifyWakeLockReleaseToIAware(int uid, int pid, String packageName, String Tag) {
        SysLoadManager.getInstance().notifyWakeLockRelease(uid, pid, packageName, Tag);
    }
}
