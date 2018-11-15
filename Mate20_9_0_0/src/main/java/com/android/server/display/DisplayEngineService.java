package com.android.server.display;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.provider.Settings.System;
import android.util.ArrayMap;
import android.util.Xml;
import com.android.server.display.DisplayEffectMonitor.MonitorModule;
import com.android.server.display.HwLightSensorController.LightSensorCallbacks;
import com.android.server.rms.iaware.feature.DevSchedFeatureRT;
import com.android.server.rms.iaware.hiber.constant.AppHibernateCst;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.huawei.displayengine.DElog;
import com.huawei.displayengine.DisplayEngineDBManager;
import com.huawei.displayengine.DisplayEngineManager;
import com.huawei.displayengine.IDisplayEngineService;
import com.huawei.displayengine.IDisplayEngineServiceEx.Stub;
import com.huawei.pgmng.plug.PGSdk;
import com.huawei.pgmng.plug.PGSdk.Sink;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class DisplayEngineService extends Stub implements LightSensorCallbacks {
    private static final int BINDER_REBUILD_COUNT_MAX = 10;
    private static final String COLOR_MODE_SWITCH_PERMISSION = "com.huawei.android.permission.MANAGE_USERS";
    private static final int COLOR_TEMP_VALID_LUX_THRESHOLD = 50;
    private static final String KEY_COLOR_MODE_SWITCH = "color_mode_switch";
    private static final String KEY_NATURAL_TONE_SWITCH = "hw_natural_tone_display_switch";
    private static final String KEY_READING_MODE_SWITCH = "hw_reading_mode_display_switch";
    private static final String KEY_USER_PREFERENCE_TRAINING_TIMESTAMP = "hw_brightness_training_timestamp";
    private static final int LIGHT_SENSOR_RATE_MILLS = 300;
    private static final String NATURAL_TONE_SWITCH_PERMISSION = "com.huawei.android.permission.MANAGE_USERS";
    private static final int READING_TYPE = 6;
    private static final int RETURN_PARAMETER_INVALID = -2;
    private static final String SR_CONTROL_XML_FILE = "/display/effect/displayengine/SR_control.xml";
    private static final String TAG = "DE J DisplayEngineService";
    private ContentObserver mAutoBrightnessAdjObserver = null;
    private AutoBrightnessAdjSwitchedReceiver mAutoBrightnessAdjSwitchedReceiver = null;
    private int mBinderRebuildCount = 0;
    private boolean mBootComplete = false;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (context == null || intent == null) {
                DElog.e(DisplayEngineService.TAG, "Invalid input parameter!");
                return;
            }
            DElog.d(DisplayEngineService.TAG, "onReceive step in!");
            String action = intent.getAction();
            if (action != null && action.equals("huawei.intent.action.RGBW_CONFIG_ACTION")) {
                int ret = DisplayEngineService.this.getSupported(13);
                if (ret == 1) {
                    PersistableBundle data = new PersistableBundle();
                    data.putIntArray("Buffer", new int[1]);
                    data.putInt("BufferLength", 1);
                    DisplayEngineService.this.setEffect(13, 1, data);
                    DElog.d(DisplayEngineService.TAG, "setEffect feature rgbw mode param updata");
                } else {
                    String str = DisplayEngineService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("current product not support rgbw and ret:");
                    stringBuilder.append(ret);
                    DElog.e(str, stringBuilder.toString());
                }
            }
        }
    };
    private int mChargeLevelThreshold = 50;
    private final ChargingStateReceiver mChargingStateReceiver;
    private ContentObserver mColorModeObserver = null;
    private ColorModeSwitchedReceiver mColorModeSwitchedReceiver = null;
    private final Context mContext;
    private int mDefaultColorModeValue = 1;
    private DisplayEffectMonitor mDisplayEffectMonitor;
    private final DisplayEngineManager mDisplayManager;
    private final DisplayEngineHandler mHandler;
    private final HandlerThread mHandlerThread;
    private final HwBrightnessSceneRecognition mHwBrightnessSceneRecognition;
    private volatile boolean mIsBinderBuilding = false;
    private volatile boolean mIsBrightnessTrainingAborting = false;
    private volatile boolean mIsBrightnessTrainingRunning = false;
    private volatile boolean mIsTrainingTriggeredSinceLastScreenOff = false;
    private long mLastAmbientColorTempToMonitorTime;
    private final HwLightSensorController mLightSensorController;
    private boolean mLightSensorEnable = false;
    private Object mLockBinderBuilding;
    private Object mLockService;
    private long mMinimumTrainingIntervalMillis = 57600000;
    private final MotionActionReceiver mMotionActionReceiver;
    private volatile IDisplayEngineService mNativeService;
    private volatile boolean mNativeServiceInitialized = false;
    private ContentObserver mNaturalToneObserver = null;
    private NaturalToneSwitchedReceiver mNaturalToneSwitchedReceiver = null;
    private boolean mNeedPkgNameFromPG = false;
    private int mNewDragNumThreshold = 1;
    private boolean mPGEnable = false;
    private PGSdk mPGSdk = null;
    private volatile boolean mScreenOn = false;
    private final ScreenStateReceiver mScreenStateReceiver;
    private Sink mStateRecognitionListener = null;

    private class AutoBrightnessAdjSwitchedReceiver extends BroadcastReceiver {
        private AutoBrightnessAdjSwitchedReceiver() {
        }

        /* synthetic */ AutoBrightnessAdjSwitchedReceiver(DisplayEngineService x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            if (context == null || intent == null) {
                DElog.e(DisplayEngineService.TAG, "Invalid input parameter!");
                return;
            }
            String action = intent.getAction();
            Object obj = -1;
            if (action.hashCode() == 959232034 && action.equals("android.intent.action.USER_SWITCHED")) {
                obj = null;
            }
            if (obj == null) {
                if (DisplayEngineService.this.mHwBrightnessSceneRecognition != null && DisplayEngineService.this.mHwBrightnessSceneRecognition.isEnable()) {
                    DisplayEngineService.this.mHwBrightnessSceneRecognition.notifyUserChange(ActivityManager.getCurrentUser());
                }
                DisplayEngineService.this.initAutoBrightnessAdjContentObserver();
            }
        }
    }

    private class ChargingStateReceiver extends BroadcastReceiver {
        public ChargingStateReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.BATTERY_CHANGED");
            DisplayEngineService.this.mContext.registerReceiver(this, filter);
        }

        public void onReceive(Context context, Intent intent) {
            Intent intent2 = intent;
            if (context == null || intent2 == null) {
                DElog.e(DisplayEngineService.TAG, "Invalid input parameter!");
                return;
            }
            String action = intent.getAction();
            String str = DisplayEngineService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("BroadcastReceiver.onReceive() action:");
            stringBuilder.append(action);
            DElog.d(str, stringBuilder.toString());
            if ("android.intent.action.BATTERY_CHANGED".equals(action)) {
                boolean chargeStatus = false;
                int mLevel = (int) ((100.0f * ((float) intent2.getIntExtra(MemoryConstant.MEM_FILECACHE_ITEM_LEVEL, 0))) / ((float) intent2.getIntExtra("scale", 100)));
                int status = intent2.getIntExtra("status", 1);
                if (status == 5 || status == 2) {
                    chargeStatus = true;
                }
                long lastTrainingProcessTimeMillis = System.getLongForUser(DisplayEngineService.this.mContext.getContentResolver(), DisplayEngineService.KEY_USER_PREFERENCE_TRAINING_TIMESTAMP, 0, -2);
                final long elapseTime = System.currentTimeMillis() - lastTrainingProcessTimeMillis;
                String str2 = DisplayEngineService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("lastTraining elapseTimeMillis = ");
                stringBuilder2.append(elapseTime);
                DElog.d(str2, stringBuilder2.toString());
                DisplayEngineDBManager dbManager = DisplayEngineDBManager.getInstance(DisplayEngineService.this.mContext);
                if (dbManager == null) {
                    DElog.w(DisplayEngineService.TAG, "ChargingStateReceiver on recieve, dbManager is null! returned.");
                    return;
                }
                Bundle info = new Bundle();
                info.putInt("NumberLimit", DisplayEngineService.this.mNewDragNumThreshold);
                ArrayList<Bundle> items = dbManager.getAllRecords("DragInfo", info);
                int i;
                if (items == null) {
                    i = status;
                } else if (items.size() < DisplayEngineService.this.mNewDragNumThreshold) {
                    String str3 = action;
                    i = status;
                } else {
                    Bundle data = (Bundle) items.get(DisplayEngineService.this.mNewDragNumThreshold - 1);
                    if (data == null) {
                        DElog.i(DisplayEngineService.TAG, "ChargingStateReceiver on recieve, data is null! returned.");
                        return;
                    }
                    long newestDragTimeMillis = data.getLong("TimeStamp", 0);
                    if (DisplayEngineService.this.mIsTrainingTriggeredSinceLastScreenOff || newestDragTimeMillis <= lastTrainingProcessTimeMillis || DisplayEngineService.this.mScreenOn || mLevel <= DisplayEngineService.this.mChargeLevelThreshold || !chargeStatus || elapseTime <= DisplayEngineService.this.mMinimumTrainingIntervalMillis || !DisplayEngineService.this.mBootComplete) {
                        DElog.d(DisplayEngineService.TAG, "-----------No Tigger Training Reason Start-----------");
                        action = DisplayEngineService.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("newestDragTime / lastTrainingProcessTime:     ");
                        stringBuilder2.append(newestDragTimeMillis);
                        stringBuilder2.append(" / ");
                        stringBuilder2.append(lastTrainingProcessTimeMillis);
                        DElog.d(action, stringBuilder2.toString());
                        action = DisplayEngineService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("mChargeLevel / mChargeLevelThreshold:         ");
                        stringBuilder.append(mLevel);
                        stringBuilder.append(" / ");
                        stringBuilder.append(DisplayEngineService.this.mChargeLevelThreshold);
                        DElog.d(action, stringBuilder.toString());
                        action = DisplayEngineService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("chargeStatus:                                 ");
                        stringBuilder.append(chargeStatus);
                        DElog.d(action, stringBuilder.toString());
                        action = DisplayEngineService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("elapsedTime / mMinimumTrainingIntervalMillis: ");
                        stringBuilder.append(elapseTime);
                        stringBuilder.append(" / ");
                        stringBuilder.append(DisplayEngineService.this.mMinimumTrainingIntervalMillis);
                        DElog.d(action, stringBuilder.toString());
                        String str4 = DisplayEngineService.TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("mScreenOn:                                    ");
                        stringBuilder3.append(DisplayEngineService.this.mScreenOn);
                        DElog.d(str4, stringBuilder3.toString());
                        str4 = DisplayEngineService.TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("mBootComplete:                                ");
                        stringBuilder3.append(DisplayEngineService.this.mBootComplete);
                        DElog.d(str4, stringBuilder3.toString());
                        DElog.d(DisplayEngineService.TAG, "-----------No Tigger Training Reason Ended-----------");
                    } else {
                        DisplayEngineService.this.mIsTrainingTriggeredSinceLastScreenOff = true;
                        if (DisplayEngineService.this.mIsBrightnessTrainingRunning) {
                            DElog.w(DisplayEngineService.TAG, "Trigger training failed, mIsBrightnessTrainingRunning == true");
                        } else {
                            DisplayEngineService.this.mIsBrightnessTrainingRunning = true;
                            new Thread(new Runnable() {
                                public void run() {
                                    DElog.i(DisplayEngineService.TAG, "mDisplayManager.brightnessTrainingProcess start... ");
                                    if (DisplayEngineService.this.mDisplayManager.brightnessTrainingProcess() == 0) {
                                        long curTime = System.currentTimeMillis();
                                        String str = DisplayEngineService.TAG;
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("Elapsed Time since last training: ");
                                        stringBuilder.append(elapseTime);
                                        stringBuilder.append(" > mMinimumTrainingIntervalMillis: ");
                                        stringBuilder.append(DisplayEngineService.this.mMinimumTrainingIntervalMillis);
                                        stringBuilder.append(", training successfully done.");
                                        DElog.i(str, stringBuilder.toString());
                                        System.putLongForUser(DisplayEngineService.this.mContext.getContentResolver(), DisplayEngineService.KEY_USER_PREFERENCE_TRAINING_TIMESTAMP, curTime, -2);
                                    }
                                    DElog.i(DisplayEngineService.TAG, "mDisplayManager.brightnessTrainingProcess finished.");
                                    DisplayEngineService.this.mIsBrightnessTrainingRunning = false;
                                }
                            }).start();
                        }
                    }
                }
                DElog.i(DisplayEngineService.TAG, "ChargingStateReceiver on recieve, items is null || items.size < 1! returned.");
                return;
            }
        }
    }

    private class ColorModeSwitchedReceiver extends BroadcastReceiver {
        private ColorModeSwitchedReceiver() {
        }

        /* synthetic */ ColorModeSwitchedReceiver(DisplayEngineService x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            if (context == null || intent == null) {
                DElog.e(DisplayEngineService.TAG, "Invalid input parameter!");
                return;
            }
            String action = intent.getAction();
            Object obj = -1;
            if (action.hashCode() == 959232034 && action.equals("android.intent.action.USER_SWITCHED")) {
                obj = null;
            }
            if (obj == null) {
                if (System.getIntForUser(DisplayEngineService.this.mContext.getContentResolver(), DisplayEngineService.KEY_COLOR_MODE_SWITCH, DisplayEngineService.this.mDefaultColorModeValue, -2) == 0) {
                    DisplayEngineService.this.setScene(13, 16);
                } else {
                    DisplayEngineService.this.setScene(13, 17);
                }
            }
            DisplayEngineService.this.initColorContentObserver();
        }
    }

    private final class DisplayEngineHandler extends Handler {
        public static final int BEGIN_POSITION = 1;
        public static final int END_POSITION = -1;
        public static final int IMAGE_EXIT = 1004;
        public static final int IMAGE_FULLSCREEN_VIEW = 1002;
        public static final int IMAGE_THUMBNAIL = 1003;
        public static final int VIDEO_FULLSCREEN_EXIT = 1001;
        public static final int VIDEO_FULLSCREEN_START = 1000;
        boolean mAlreadyHandle = false;
        int mSceneState = 0;
        Stack mVideoStack = new Stack();

        public DisplayEngineHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            if (msg == null) {
                DElog.e(DisplayEngineService.TAG, "msg is null!");
                return;
            }
            if (msg.what != 2) {
                DElog.e(DisplayEngineService.TAG, "Invalid message");
            } else {
                Bundle B = msg.getData();
                String name = (String) B.get("SurfaceName");
                String attachWin = (String) B.get("AttachWinName");
                if (attachWin != null && WhiteList.ATTACH_IMAGE_LIST.contains(attachWin) && name.startsWith("PopupWindow")) {
                    setImageScene(B);
                } else if (WhiteList.IMAGE_LIST.contains(name)) {
                    setImageScene(B);
                } else if (WhiteList.VIDEO_LIST.contains(name)) {
                    setVideoScene(B);
                } else if (WhiteList.VO_LIST.contains(name)) {
                    setVoScene(B);
                }
            }
        }

        private void getTimer() {
            DElog.d(DisplayEngineService.TAG, "getTimer step in!");
            if (!this.mAlreadyHandle) {
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        DisplayEngineHandler.this.handleVideoStack();
                    }
                }, (long) 100);
                this.mAlreadyHandle = true;
            }
        }

        private void pushActionToStack(int action) {
            this.mVideoStack.push(Integer.valueOf(action));
            getTimer();
        }

        private void setVideoScene(Bundle B) {
            int frameLeft = B.getInt("FrameLeft");
            int frameRight = B.getInt("FrameRight");
            int frameTop = B.getInt("FrameTop");
            int frameBottom = B.getInt("FrameBottom");
            int displayWidth = B.getInt("DisplayWidth");
            int displayHeight = B.getInt("DisplayHeight");
            int position = B.getInt("Position");
            if (position == -1) {
                pushActionToStack(1001);
            } else if (position != 1) {
            } else {
                if (WhiteList.isFullScreen(frameLeft, frameTop, frameRight, frameBottom, displayWidth, displayHeight)) {
                    pushActionToStack(1000);
                } else {
                    pushActionToStack(1001);
                }
            }
        }

        private void setImageScene(Bundle B) {
            int frameLeft = B.getInt("FrameLeft");
            int frameRight = B.getInt("FrameRight");
            int frameTop = B.getInt("FrameTop");
            int frameBottom = B.getInt("FrameBottom");
            int displayWidth = B.getInt("DisplayWidth");
            int displayHeight = B.getInt("DisplayHeight");
            int position = B.getInt("Position");
            if (position == -1) {
                pushActionToStack(1004);
            } else if (position != 1) {
            } else {
                if (WhiteList.isFullScreen(frameLeft, frameTop, frameRight, frameBottom, displayWidth, displayHeight)) {
                    pushActionToStack(1002);
                } else {
                    pushActionToStack(1003);
                }
            }
        }

        private void setVoScene(Bundle B) {
            int position = B.getInt("Position");
            if (position == -1) {
                pushActionToStack(1001);
            } else if (position == 1) {
                pushActionToStack(1000);
            } else {
                DElog.e(DisplayEngineService.TAG, "[leilei] setVoScene position ERROR !");
            }
        }

        private void handleVideoStack() {
            DElog.d(DisplayEngineService.TAG, "handleVideoStack step in!");
            if (this.mVideoStack.empty()) {
                this.mAlreadyHandle = false;
                return;
            }
            if (this.mSceneState != ((Integer) this.mVideoStack.peek()).intValue()) {
                this.mSceneState = ((Integer) this.mVideoStack.peek()).intValue();
                sendToNativeScene(this.mSceneState);
            }
            while (!this.mVideoStack.empty()) {
                this.mVideoStack.pop();
            }
            this.mAlreadyHandle = false;
        }

        private int sendToNativeScene(int sceneState) {
            int ret;
            switch (sceneState) {
                case 1000:
                    return DisplayEngineService.this.setScene(1, 4);
                case 1001:
                    ret = DisplayEngineService.this.setScene(1, 8);
                    if (DisplayEngineService.this.mPGEnable) {
                        return DisplayEngineService.this.setDetailPGscene();
                    }
                    return DisplayEngineService.this.setDetailIawareScene();
                case 1002:
                    return DisplayEngineService.this.setScene(3, 12);
                case 1003:
                    return DisplayEngineService.this.setScene(3, 12);
                case 1004:
                    ret = DisplayEngineService.this.setScene(3, 13);
                    if (DisplayEngineService.this.mPGEnable) {
                        return DisplayEngineService.this.setDetailPGscene();
                    }
                    return DisplayEngineService.this.setDetailIawareScene();
                default:
                    return -1;
            }
        }
    }

    private class MotionActionReceiver extends BroadcastReceiver {
        private static final String ACTION_MOTION = "com.huawei.motion.change.noification";
        private static final String EXTRA_KEY = "category";
        private static final String EXTRA_MOTION_START = "start_motion";
        private static final String EXTRA_MOTION_STOP_BACK_APP_NOCHANGE = "back_application_nochange";
        private static final String EXTRA_MOTION_STOP_BACK_APP_TRANSATION = "back_application_transation";
        private static final String EXTRA_MOTION_STOP_HOME = "return_home";
        private static final String EXTRA_MOTION_STOP_RECENT = "enter_recent";
        private static final String PERMISSION_MOTION = "com.huawei.android.launcher.permission.HW_MOTION";

        public MotionActionReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_MOTION);
            filter.setPriority(1000);
            DisplayEngineService.this.mContext.registerReceiver(this, filter, PERMISSION_MOTION, null);
        }

        public void onReceive(Context context, Intent intent) {
            if (context == null || intent == null) {
                DElog.e(DisplayEngineService.TAG, "[MOTION_NOTIFICATION] MotionActionReceiver.onRecive() Invalid input parameter!");
                return;
            }
            String str = DisplayEngineService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[MOTION_NOTIFICATION] MotionActionReceiver.onReceive() action:");
            stringBuilder.append(intent.getAction());
            DElog.d(str, stringBuilder.toString());
            if (intent.getAction().equals(ACTION_MOTION)) {
                str = DisplayEngineService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("[MOTION_NOTIFICATION] MotionActionReceiver.onReceive() extra:");
                stringBuilder.append(intent.getStringExtra("category"));
                DElog.i(str, stringBuilder.toString());
                str = intent.getStringExtra("category");
                Object obj = -1;
                switch (str.hashCode()) {
                    case -1486606706:
                        if (str.equals(EXTRA_MOTION_STOP_HOME)) {
                            obj = 3;
                            break;
                        }
                        break;
                    case -543270494:
                        if (str.equals(EXTRA_MOTION_STOP_RECENT)) {
                            obj = 4;
                            break;
                        }
                        break;
                    case -158947789:
                        if (str.equals(EXTRA_MOTION_START)) {
                            obj = null;
                            break;
                        }
                        break;
                    case 1472278296:
                        if (str.equals(EXTRA_MOTION_STOP_BACK_APP_NOCHANGE)) {
                            obj = 1;
                            break;
                        }
                        break;
                    case 1851839988:
                        if (str.equals(EXTRA_MOTION_STOP_BACK_APP_TRANSATION)) {
                            obj = 2;
                            break;
                        }
                        break;
                }
                switch (obj) {
                    case null:
                        DisplayEngineService.this.setScene(38, 21);
                        DElog.d(DisplayEngineService.TAG, "[MOTION_NOTIFICATION] Motion start");
                        break;
                    case 1:
                    case 2:
                        DisplayEngineService.this.setScene(38, 19);
                        DElog.d(DisplayEngineService.TAG, "[MOTION_NOTIFICATION] Motion app");
                        break;
                    case 3:
                        DElog.d(DisplayEngineService.TAG, "[MOTION_NOTIFICATION] Motion home");
                        DisplayEngineService.this.setScene(38, 18);
                        break;
                    case 4:
                        DisplayEngineService.this.setScene(38, 20);
                        DElog.d(DisplayEngineService.TAG, "[MOTION_NOTIFICATION] Motion recent");
                        break;
                }
            }
        }
    }

    private class NaturalToneSwitchedReceiver extends BroadcastReceiver {
        private NaturalToneSwitchedReceiver() {
        }

        /* synthetic */ NaturalToneSwitchedReceiver(DisplayEngineService x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            if (context == null || intent == null) {
                DElog.e(DisplayEngineService.TAG, "Invalid input parameter!");
                return;
            }
            String action = intent.getAction();
            int i = -1;
            if (action.hashCode() == 959232034 && action.equals("android.intent.action.USER_SWITCHED")) {
                i = 0;
            }
            if (i == 0) {
                if (1 == System.getIntForUser(DisplayEngineService.this.mContext.getContentResolver(), DisplayEngineService.KEY_NATURAL_TONE_SWITCH, 0, -2)) {
                    DisplayEngineService.this.setScene(25, 16);
                    DElog.v(DisplayEngineService.TAG, "NaturalToneSwitchedReceiver setScene, DE_ACTION_MODE_ON");
                } else {
                    DisplayEngineService.this.setScene(25, 17);
                    DElog.v(DisplayEngineService.TAG, "NaturalToneSwitchedReceiver setScene, DE_ACTION_MODE_OFF");
                }
            }
            DisplayEngineService.this.initNaturalToneContentObserver();
        }
    }

    private class ScreenStateReceiver extends BroadcastReceiver {
        public ScreenStateReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.SCREEN_OFF");
            filter.addAction("android.intent.action.SCREEN_ON");
            filter.addAction("android.intent.action.BOOT_COMPLETED");
            filter.setPriority(1000);
            DisplayEngineService.this.mContext.registerReceiver(this, filter);
        }

        public void onReceive(Context context, Intent intent) {
            if (context == null || intent == null) {
                DElog.e(DisplayEngineService.TAG, "Invalid input parameter!");
                return;
            }
            String str = DisplayEngineService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("BroadcastReceiver.onReceive() action:");
            stringBuilder.append(intent.getAction());
            DElog.i(str, stringBuilder.toString());
            str = intent.getAction();
            boolean z = true;
            int hashCode = str.hashCode();
            if (hashCode != -2128145023) {
                if (hashCode != -1454123155) {
                    if (hashCode == 798292259 && str.equals("android.intent.action.BOOT_COMPLETED")) {
                        z = true;
                    }
                } else if (str.equals("android.intent.action.SCREEN_ON")) {
                    z = true;
                }
            } else if (str.equals("android.intent.action.SCREEN_OFF")) {
                z = false;
            }
            switch (z) {
                case false:
                    DisplayEngineService.this.setScene(10, 17);
                    DisplayEngineService.this.mIsTrainingTriggeredSinceLastScreenOff = false;
                    DisplayEngineService.this.mScreenOn = false;
                    break;
                case true:
                    DisplayEngineService.this.setScene(10, 16);
                    if (!DisplayEngineService.this.mIsBrightnessTrainingRunning || DisplayEngineService.this.mIsBrightnessTrainingAborting) {
                        DElog.i(DisplayEngineService.TAG, "Trigger training abort failed, training is NOT running or is already aborting.");
                    } else {
                        DisplayEngineService.this.mIsBrightnessTrainingAborting = true;
                        new Thread(new Runnable() {
                            public void run() {
                                DElog.i(DisplayEngineService.TAG, "mDisplayManager.brightnessTrainingAbort start... ");
                                DisplayEngineService.this.mDisplayManager.brightnessTrainingAbort();
                                DElog.i(DisplayEngineService.TAG, "mDisplayManager.brightnessTrainingAbort finished.");
                                DisplayEngineService.this.mIsBrightnessTrainingAborting = false;
                            }
                        }).start();
                    }
                    DisplayEngineService.this.mScreenOn = true;
                    break;
                case true:
                    if (DisplayEngineService.this.mPGEnable || DisplayEngineService.this.mNeedPkgNameFromPG) {
                        DisplayEngineService.this.registerPGSdk();
                    }
                    DisplayEngineService.this.initColorModeSwitch();
                    DisplayEngineService.this.initNaturalToneSwitch();
                    DisplayEngineService.this.initAutoBrightnessAdjSwitch();
                    DisplayEngineService.this.initAutoBrightnessAdjContentObserver();
                    DisplayEngineService.this.registerRgbwBroadcast();
                    DisplayEngineService.this.setScene(18, 16);
                    DisplayEngineService.this.mHwBrightnessSceneRecognition.initBootCompleteValues();
                    DisplayEngineService.this.mBootComplete = true;
                    DisplayEngineService.this.mScreenOn = true;
                    break;
            }
        }
    }

    static final class WhiteList {
        static final List<String> ATTACH_IMAGE_LIST = new ArrayList<String>() {
            {
                add("com.tencent.mm/com.tencent.mm.plugin.profile.ui.ContactInfoUI");
            }
        };
        static final List<String> IMAGE_LIST = new ArrayList<String>() {
            {
                add("com.tencent.mm/com.tencent.mm.ui.chatting.gallery.ImageGalleryUI");
                add("com.tencent.mm/com.tencent.mm.plugin.sns.ui.SnsBrowseUI");
                add("com.tencent.mm/com.tencent.mm.plugin.sns.ui.SnsGalleryUI");
                add("com.tencent.mm/com.tencent.mm.plugin.subapp.ui.gallery.GestureGalleryUI");
                add("com.tencent.mm/com.tencent.mm.plugin.gallery.ui.ImagePreviewUI");
                add("com.tencent.mm/com.tencent.mm.plugin.setting.ui.setting.PreviewHdHeadImg");
                add("com.tencent.mobileqq/com.tencent.mobileqq.activity.aio.photo.AIOGalleryActivity");
                add("com.tencent.mobileqq/cooperation.qzone.QzonePicturePluginProxyActivity");
                add("com.tencent.mobileqq/com.tencent.mobileqq.activity.photo.PhotoPreviewActivity");
                add("com.tencent.mobileqq/com.tencent.mobileqq.activity.FriendProfileImageActivity");
                add("com.baidu.tieba/com.baidu.tieba.image.ImageViewerActivity");
                add("com.sina.weibo/com.sina.weibo.imageviewer.ImageViewer");
            }
        };
        static final List<String> VIDEO_LIST = new ArrayList<String>() {
            {
                add("SurfaceView - air.tv.douyu.android/tv.douyu.view.activity.PlayerActivity");
                add("SurfaceView - air.tv.douyu.android/tv.douyu.view.activity.VideoPlayerActivity");
                add("SurfaceView - air.tv.douyu.android/tv.douyu.view.activity.MobilePlayerActivity");
                add("SurfaceView - com.panda.videoliveplatform/com.panda.videoliveplatform.activity.LiveRoomActivity");
                add("SurfaceView - com.meelive.ingkee/com.meelive.ingkee.game.activity.RoomPlayerActivity");
                add("SurfaceView - com.meelive.ingkee/com.meelive.ingkee.ui.room.activity.RoomActivity");
                add("SurfaceView - com.duowan.kiwi/com.duowan.kiwi.channelpage.ChannelPage");
                add("SurfaceView - com.duowan.kiwi/com.duowan.kiwi.mobileliving.PortraitAwesomeLivingActivity");
                add("SurfaceView - com.duowan.kiwi/com.duowan.kiwi.recordervedio.VideoShowDetailActivity");
            }
        };
        static final List<String> VO_LIST = new ArrayList<String>() {
            {
                add("SurfaceView - com.tencent.mm/com.tencent.mm.plugin.voip.ui.VideoActivity");
                add("SurfaceView - com.tencent.mobileqq/com.tencent.av.ui.AVActivity");
            }
        };

        WhiteList() {
        }

        static boolean isFullScreen(int frameLeft, int frameTop, int frameRight, int frameBottom, int displayWidth, int displayHeight) {
            int l = Math.abs(frameLeft);
            int t = Math.abs(frameTop);
            int r = Math.abs(frameRight);
            int b = Math.abs(frameBottom);
            boolean z = l + r >= displayHeight + -100 && t + b >= displayWidth - 100;
            boolean isLandscape = z;
            z = l + r >= displayWidth + -100 && t + b >= displayHeight - 100;
            boolean isPortrait = z;
            if (isLandscape || isPortrait) {
                return true;
            }
            return false;
        }
    }

    public DisplayEngineService(Context context) {
        this.mContext = context;
        HwLightSensorController controller = null;
        SensorManager manager = (SensorManager) this.mContext.getSystemService("sensor");
        if (manager == null) {
            DElog.e(TAG, "Failed to get SensorManager:sensor");
        } else {
            controller = new HwLightSensorController(this.mContext, this, manager, 300);
        }
        this.mLightSensorController = controller;
        this.mHwBrightnessSceneRecognition = new HwBrightnessSceneRecognition(this.mContext);
        this.mScreenStateReceiver = new ScreenStateReceiver();
        this.mMotionActionReceiver = new MotionActionReceiver();
        this.mHandlerThread = new HandlerThread(TAG);
        this.mHandlerThread.start();
        this.mHandler = new DisplayEngineHandler(this.mHandlerThread.getLooper());
        this.mNativeService = null;
        this.mLockService = new Object();
        this.mLockBinderBuilding = new Object();
        getConfigParam();
        sendUIScene();
        setDefaultColorModeValue();
        initColorModeValue();
        initNaturalToneValue();
        this.mDisplayEffectMonitor = DisplayEffectMonitor.getInstance(context);
        this.mChargingStateReceiver = new ChargingStateReceiver();
        this.mDisplayManager = new DisplayEngineManager(this.mContext);
    }

    private IDisplayEngineService getNativeService() throws RemoteException {
        if (this.mNativeService == null && !this.mNativeServiceInitialized) {
            synchronized (this.mLockService) {
                if (this.mNativeService == null && !this.mNativeServiceInitialized) {
                    buildBinder();
                    if (this.mNativeService != null) {
                        this.mNativeServiceInitialized = true;
                    }
                }
            }
        }
        if (this.mNativeService != null || !this.mNativeServiceInitialized) {
            return this.mNativeService;
        }
        if (this.mBinderRebuildCount < 10) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Try to rebuild binder ");
            stringBuilder.append(this.mBinderRebuildCount);
            stringBuilder.append(" times.");
            throw new RemoteException(stringBuilder.toString());
        }
        throw new RemoteException("binder rebuilding failed!");
    }

    private void buildBinder() {
        IBinder binder = ServiceManager.getService("DisplayEngineService");
        if (binder != null) {
            this.mNativeService = IDisplayEngineService.Stub.asInterface(binder);
            if (this.mNativeService == null) {
                DElog.w(TAG, "service is null!");
                return;
            }
            return;
        }
        this.mNativeService = null;
        DElog.w(TAG, "binder is null!");
    }

    private void rebuildBinder() {
        DElog.i(TAG, "wait 800ms to rebuild binder...");
        SystemClock.sleep(800);
        DElog.i(TAG, "rebuild binder...");
        synchronized (this.mLockService) {
            buildBinder();
            if (this.mNativeService != null) {
                DElog.i(TAG, "rebuild binder success.");
                if (this.mScreenOn) {
                    setScene(10, 16);
                }
            } else {
                DElog.i(TAG, "rebuild binder failed!");
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
                    new Thread(new Runnable() {
                        public void run() {
                            DisplayEngineService.this.rebuildBinder();
                        }
                    }).start();
                    this.mIsBinderBuilding = true;
                }
            }
        }
    }

    public int getSupported(int feature) {
        try {
            IDisplayEngineService service = getNativeService();
            if (service != null) {
                return service.getSupported(feature);
            }
            return 0;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getSupported(");
            stringBuilder.append(feature);
            stringBuilder.append(") has remote exception:");
            stringBuilder.append(e.getMessage());
            DElog.e(str, stringBuilder.toString());
            rebuildBinderDelayed();
            return 0;
        }
    }

    public int setScene(int scene, int action) {
        int ret = -1;
        try {
            IDisplayEngineService service = getNativeService();
            if (service != null) {
                ret = service.setScene(scene, action);
                if (scene == 24 && this.mHwBrightnessSceneRecognition != null && this.mHwBrightnessSceneRecognition.isEnable()) {
                    this.mHwBrightnessSceneRecognition.notifyScreenStatus(action == 16);
                }
            }
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setScene(");
            stringBuilder.append(scene);
            stringBuilder.append(", ");
            stringBuilder.append(action);
            stringBuilder.append(") has remote exception:");
            stringBuilder.append(e.getMessage());
            DElog.e(str, stringBuilder.toString());
            rebuildBinderDelayed();
        }
        return ret;
    }

    public int setData(int type, PersistableBundle data) {
        int ret = -1;
        String str;
        StringBuilder stringBuilder;
        try {
            IDisplayEngineService service = getNativeService();
            if (service != null) {
                if (data == null) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("setData(");
                    stringBuilder.append(type);
                    stringBuilder.append(", data): data is null!");
                    DElog.e(str, stringBuilder.toString());
                    ret = -2;
                } else if (this.mPGEnable && type == 10) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("setData(");
                    stringBuilder.append(type);
                    stringBuilder.append(", data): mPGEnable is true!");
                    DElog.d(str, stringBuilder.toString());
                    return -1;
                } else {
                    ret = service.setData(type, data);
                    if (type == 10) {
                        handleIawareSpecialScene(type, data);
                        handleIawareEbookScene(type, data);
                    }
                }
            }
        } catch (RemoteException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setData(");
            stringBuilder.append(type);
            stringBuilder.append(") has remote exception:");
            stringBuilder.append(e.getMessage());
            DElog.e(str, stringBuilder.toString());
            rebuildBinderDelayed();
        }
        return ret;
    }

    private void handleIawareEbookScene(int type, PersistableBundle data) {
        if (type == 10) {
            int scene = data.getInt("Scene");
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Scene is ");
            stringBuilder.append(scene);
            DElog.d(str, stringBuilder.toString());
            if (scene == 6) {
                str = getCurrentTopAppName();
                if (str != null) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("pkgName is ");
                    stringBuilder2.append(str);
                    DElog.d(str2, stringBuilder2.toString());
                }
                System.putIntForUser(this.mContext.getContentResolver(), KEY_READING_MODE_SWITCH, 1, -2);
            } else {
                System.putIntForUser(this.mContext.getContentResolver(), KEY_READING_MODE_SWITCH, 0, -2);
            }
        }
    }

    public int sendMessage(int messageID, Bundle data) {
        if (data != null) {
            Message msg = this.mHandler.obtainMessage(messageID);
            msg.setData(data);
            this.mHandler.sendMessage(msg);
            return 0;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendMessage(");
        stringBuilder.append(messageID);
        stringBuilder.append(", data): data is null!");
        DElog.e(str, stringBuilder.toString());
        return -2;
    }

    public int getEffect(int feature, int type, byte[] status, int length) {
        String str;
        StringBuilder stringBuilder;
        try {
            IDisplayEngineService service = getNativeService();
            if (service == null) {
                return -1;
            }
            if (status != null && status.length == length) {
                return service.getEffect(feature, type, status, length);
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getEffect(");
            stringBuilder.append(feature);
            stringBuilder.append(", ");
            stringBuilder.append(type);
            stringBuilder.append(", status, ");
            stringBuilder.append(length);
            stringBuilder.append("): data is null or status.length != length!");
            DElog.e(str, stringBuilder.toString());
            return -2;
        } catch (RemoteException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getEffect(");
            stringBuilder.append(feature);
            stringBuilder.append(", ");
            stringBuilder.append(type);
            stringBuilder.append(", ");
            stringBuilder.append(length);
            stringBuilder.append(") has remote exception:");
            stringBuilder.append(e.getMessage());
            DElog.e(str, stringBuilder.toString());
            rebuildBinderDelayed();
            return -1;
        }
    }

    public int setEffect(int feature, int mode, PersistableBundle data) {
        String str;
        StringBuilder stringBuilder;
        try {
            IDisplayEngineService service = getNativeService();
            if (service == null) {
                return -1;
            }
            if (data != null) {
                return service.setEffect(feature, mode, data);
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setEffect(");
            stringBuilder.append(feature);
            stringBuilder.append(", ");
            stringBuilder.append(mode);
            stringBuilder.append(", data): data is null!");
            DElog.e(str, stringBuilder.toString());
            return -2;
        } catch (RemoteException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setEffect(");
            stringBuilder.append(feature);
            stringBuilder.append(", ");
            stringBuilder.append(mode);
            stringBuilder.append(") has remote exception:");
            stringBuilder.append(e.getMessage());
            DElog.e(str, stringBuilder.toString());
            rebuildBinderDelayed();
            return -1;
        }
    }

    public void updateLightSensorState(boolean sensorEnable) {
        enableLightSensor(sensorEnable);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("LightSensorEnable=");
        stringBuilder.append(sensorEnable);
        DElog.i(str, stringBuilder.toString());
    }

    public List<Bundle> getAllRecords(String name, Bundle info) {
        return DisplayEngineDBManager.getInstance(this.mContext).getAllRecords(name, info);
    }

    public void processSensorData(long timeInMs, int lux, int cct) {
        int[] ambientParam = new int[]{lux, cct};
        PersistableBundle bundle = new PersistableBundle();
        bundle.putIntArray("Buffer", ambientParam);
        bundle.putInt("BufferLength", 8);
        int ret = setData(9, bundle);
        if (ret != 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("processSensorData set Data Error: ret =");
            stringBuilder.append(ret);
            DElog.i(str, stringBuilder.toString());
        }
        sendAmbientColorTempToMonitor(timeInMs, lux, cct);
    }

    private void enableLightSensor(boolean enable) {
        if (this.mLightSensorEnable != enable && this.mLightSensorController != null) {
            this.mLightSensorEnable = enable;
            if (this.mLightSensorEnable) {
                this.mLightSensorController.enableSensor();
                this.mLastAmbientColorTempToMonitorTime = 0;
                return;
            }
            this.mLightSensorController.disableSensor();
        }
    }

    private void initColorContentObserver() {
        if (this.mColorModeObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mColorModeObserver);
        }
        this.mColorModeObserver = new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange) {
                if (System.getIntForUser(DisplayEngineService.this.mContext.getContentResolver(), DisplayEngineService.KEY_COLOR_MODE_SWITCH, DisplayEngineService.this.mDefaultColorModeValue, -2) == 0) {
                    DisplayEngineService.this.setScene(13, 16);
                } else {
                    DisplayEngineService.this.setScene(13, 17);
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor(KEY_COLOR_MODE_SWITCH), true, this.mColorModeObserver, -2);
    }

    private void initColorModeSwitch() {
        if (this.mColorModeSwitchedReceiver == null) {
            this.mColorModeSwitchedReceiver = new ColorModeSwitchedReceiver(this, null);
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.USER_SWITCHED");
            this.mContext.registerReceiver(this.mColorModeSwitchedReceiver, filter, "com.huawei.android.permission.MANAGE_USERS", new Handler());
        }
        initColorContentObserver();
    }

    private void initColorModeValue() {
        if (System.getIntForUser(this.mContext.getContentResolver(), KEY_COLOR_MODE_SWITCH, this.mDefaultColorModeValue, -2) == 0) {
            setScene(13, 16);
        } else {
            setScene(13, 17);
        }
    }

    private void setDefaultColorModeValue() {
        byte[] status = new byte[1];
        String str;
        StringBuilder stringBuilder;
        if (getEffect(11, 0, status, 1) == 0) {
            this.mDefaultColorModeValue = status[0];
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("[effect] getEffect(DE_FEATURE_COLORMODE):");
            stringBuilder.append(this.mDefaultColorModeValue);
            DElog.i(str, stringBuilder.toString());
            return;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("[effect] getEffect(DE_FEATURE_COLORMODE):");
        stringBuilder.append(this.mDefaultColorModeValue);
        DElog.e(str, stringBuilder.toString());
    }

    private void initNaturalToneContentObserver() {
        if (this.mNaturalToneObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mNaturalToneObserver);
        }
        this.mNaturalToneObserver = new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange) {
                if (1 == System.getIntForUser(DisplayEngineService.this.mContext.getContentResolver(), DisplayEngineService.KEY_NATURAL_TONE_SWITCH, 0, -2)) {
                    DisplayEngineService.this.setScene(25, 16);
                    DElog.v(DisplayEngineService.TAG, "ContentObserver setScene, DE_ACTION_MODE_ON");
                    return;
                }
                DisplayEngineService.this.setScene(25, 17);
                DElog.v(DisplayEngineService.TAG, " ContentObserver setScene, DE_ACTION_MODE_OFF");
            }
        };
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor(KEY_NATURAL_TONE_SWITCH), true, this.mNaturalToneObserver, -2);
    }

    private void initNaturalToneSwitch() {
        if (this.mNaturalToneSwitchedReceiver == null) {
            this.mNaturalToneSwitchedReceiver = new NaturalToneSwitchedReceiver(this, null);
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.USER_SWITCHED");
            this.mContext.registerReceiver(this.mNaturalToneSwitchedReceiver, filter, "com.huawei.android.permission.MANAGE_USERS", new Handler());
        }
        initNaturalToneContentObserver();
    }

    private void initNaturalToneValue() {
        if (1 == System.getIntForUser(this.mContext.getContentResolver(), KEY_NATURAL_TONE_SWITCH, 0, -2)) {
            setScene(25, 16);
            DElog.v(TAG, "initNaturalToneValue setScene, DE_ACTION_MODE_ON");
            return;
        }
        setScene(25, 17);
        DElog.v(TAG, "initNaturalToneValue setScene, DE_ACTION_MODE_OFF");
    }

    private void initAutoBrightnessAdjContentObserver() {
        if (this.mAutoBrightnessAdjObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mAutoBrightnessAdjObserver);
        }
        this.mAutoBrightnessAdjObserver = new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange) {
                if (DisplayEngineService.this.mHwBrightnessSceneRecognition != null && DisplayEngineService.this.mHwBrightnessSceneRecognition.isEnable()) {
                    DisplayEngineService.this.mHwBrightnessSceneRecognition.notifyAutoBrightnessAdj();
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("hw_screen_auto_brightness_adj"), true, this.mAutoBrightnessAdjObserver, -2);
        DElog.i(TAG, "initAutoBrightnessAdjContentObserver");
    }

    private void initAutoBrightnessAdjSwitch() {
        if (this.mAutoBrightnessAdjSwitchedReceiver == null) {
            this.mAutoBrightnessAdjSwitchedReceiver = new AutoBrightnessAdjSwitchedReceiver(this, null);
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.USER_SWITCHED");
            this.mContext.registerReceiver(this.mAutoBrightnessAdjSwitchedReceiver, filter, "com.huawei.android.permission.MANAGE_USERS", new Handler());
        }
        DElog.i(TAG, "initAutoBrightnessAdjSwitch");
    }

    private int setDetailPGscene(String pkg) {
        String pkgName = pkg;
        int scene = 0;
        if (pkg == null) {
            DElog.i(TAG, "pkg is null");
            return -1;
        } else if (!this.mPGEnable) {
            DElog.i(TAG, "mPGEnable false");
            return -1;
        } else if (this.mPGSdk == null) {
            DElog.i(TAG, "mPGSdk is null");
            return -1;
        } else {
            String str;
            StringBuilder stringBuilder;
            try {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("getPkgType, pkgName: ");
                stringBuilder.append(pkgName);
                DElog.d(str, stringBuilder.toString());
                scene = this.mPGSdk.getPkgType(this.mContext, pkgName);
            } catch (RemoteException ex) {
                DElog.e(TAG, "getPkgType", ex);
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("PGSdk getPkgType, scene result:");
            stringBuilder.append(scene);
            DElog.d(str, stringBuilder.toString());
            return setScene(17, scene);
        }
    }

    private int setDetailPGscene() {
        String pkgName = getCurrentTopAppName();
        int scene = 0;
        if (pkgName == null) {
            DElog.i(TAG, "getCurrentTopAppName is null");
            return -1;
        } else if (!this.mPGEnable) {
            DElog.i(TAG, "mPGEnable false");
            return -1;
        } else if (this.mPGSdk == null) {
            DElog.i(TAG, "mPGSdk is null");
            return -1;
        } else {
            String str;
            StringBuilder stringBuilder;
            try {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("getPkgType,pkgName: ");
                stringBuilder.append(pkgName);
                DElog.d(str, stringBuilder.toString());
                scene = this.mPGSdk.getPkgType(this.mContext, pkgName);
            } catch (RemoteException ex) {
                DElog.e(TAG, "getPkgType", ex);
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("PGSdk getPkgType, scene result:");
            stringBuilder.append(scene);
            DElog.d(str, stringBuilder.toString());
            return setScene(17, scene);
        }
    }

    private int setDetailIawareScene() {
        String pkgName = getCurrentTopAppName();
        if (pkgName == null) {
            DElog.i(TAG, "getCurrentTopAppName is null");
            return -1;
        } else if (this.mPGEnable) {
            DElog.i(TAG, "mPGEnable true");
            return -1;
        } else {
            int scene = DevSchedFeatureRT.getAppTypeForLCD(pkgName);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getFrom iaware ,pkgName:");
            stringBuilder.append(pkgName);
            stringBuilder.append(",getAppType: ");
            stringBuilder.append(scene);
            DElog.d(str, stringBuilder.toString());
            PersistableBundle data = new PersistableBundle();
            data.putInt("Scene", scene);
            data.putInt("PowerLevel", -1);
            return setData(10, data);
        }
    }

    private void handleIawareSpecialScene(int type, PersistableBundle data) {
        if (type == 10) {
            int scene = data.getInt("Scene");
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" Scene is ");
            stringBuilder.append(scene);
            DElog.d(str, stringBuilder.toString());
            if (scene == 255) {
                str = getCurrentTopAppName();
                if (str == null) {
                    DElog.i(TAG, "getCurrentTopAppName is null");
                    return;
                }
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("pkgName is ");
                stringBuilder2.append(str);
                DElog.d(str2, stringBuilder2.toString());
                if (str.equals("com.huawei.mmitest")) {
                    setScene(35, 16);
                    DElog.d(TAG, "setScene (35,16) OK!");
                }
            }
        }
    }

    private String getCurrentTopAppName() {
        try {
            List<RunningTaskInfo> runningTasks = ((ActivityManager) this.mContext.getSystemService("activity")).getRunningTasks(1);
            if (runningTasks == null || runningTasks.isEmpty()) {
                return null;
            }
            return ((RunningTaskInfo) runningTasks.get(0)).topActivity.getPackageName();
        } catch (SecurityException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getCurrentTopAppName() failed to get topActivity PackageName ");
            stringBuilder.append(e);
            DElog.e(str, stringBuilder.toString());
            return null;
        }
    }

    private void initPGSdkState() {
        if (!this.mPGEnable && !this.mNeedPkgNameFromPG) {
            DElog.i(TAG, "mPGEnable false");
        } else if (this.mPGSdk == null) {
            DElog.i(TAG, "mPGSdk is null");
        } else if (this.mStateRecognitionListener == null) {
            DElog.i(TAG, "mStateRecognitionListener is null");
        } else {
            try {
                DElog.d(TAG, "enableStateEvent step in!");
                this.mPGSdk.enableStateEvent(this.mStateRecognitionListener, Integer.valueOf(10000).intValue());
                this.mPGSdk.enableStateEvent(this.mStateRecognitionListener, Integer.valueOf(IDisplayEngineService.DE_ACTION_PG_BROWSER_FRONT).intValue());
                this.mPGSdk.enableStateEvent(this.mStateRecognitionListener, Integer.valueOf(IDisplayEngineService.DE_ACTION_PG_3DGAME_FRONT).intValue());
                this.mPGSdk.enableStateEvent(this.mStateRecognitionListener, Integer.valueOf(IDisplayEngineService.DE_ACTION_PG_EBOOK_FRONT).intValue());
                this.mPGSdk.enableStateEvent(this.mStateRecognitionListener, Integer.valueOf(IDisplayEngineService.DE_ACTION_PG_INPUT_START).intValue());
                this.mPGSdk.enableStateEvent(this.mStateRecognitionListener, Integer.valueOf(IDisplayEngineService.DE_ACTION_PG_INPUT_END).intValue());
                this.mPGSdk.enableStateEvent(this.mStateRecognitionListener, Integer.valueOf(IDisplayEngineService.DE_ACTION_PG_CAMERA_FRONT).intValue());
                this.mPGSdk.enableStateEvent(this.mStateRecognitionListener, Integer.valueOf(IDisplayEngineService.DE_ACTION_PG_OFFICE_FRONT).intValue());
                this.mPGSdk.enableStateEvent(this.mStateRecognitionListener, Integer.valueOf(IDisplayEngineService.DE_ACTION_PG_VIDEO_FRONT).intValue());
                this.mPGSdk.enableStateEvent(this.mStateRecognitionListener, Integer.valueOf(IDisplayEngineService.DE_ACTION_PG_LAUNCHER_FRONT).intValue());
                this.mPGSdk.enableStateEvent(this.mStateRecognitionListener, Integer.valueOf(IDisplayEngineService.DE_ACTION_PG_3DGAME_FRONT).intValue());
                this.mPGSdk.enableStateEvent(this.mStateRecognitionListener, Integer.valueOf(IDisplayEngineService.DE_ACTION_PG_MMS_FRONT).intValue());
                this.mPGSdk.enableStateEvent(this.mStateRecognitionListener, Integer.valueOf(IDisplayEngineService.DE_ACTION_PG_VIDEO_START).intValue());
                this.mPGSdk.enableStateEvent(this.mStateRecognitionListener, Integer.valueOf(IDisplayEngineService.DE_ACTION_PG_VIDEO_END).intValue());
                this.mPGSdk.enableStateEvent(this.mStateRecognitionListener, Integer.valueOf(IDisplayEngineService.DE_ACTION_PG_CAMERA_END).intValue());
            } catch (RemoteException ex) {
                DElog.e(TAG, "enableStateEvent", ex);
            }
        }
    }

    private void registerPGSdk() {
        if (this.mPGEnable || this.mNeedPkgNameFromPG) {
            if (this.mPGSdk == null) {
                DElog.d(TAG, "mPGSdk constructor ok");
                this.mPGSdk = PGSdk.getInstance();
            }
            if (this.mStateRecognitionListener == null) {
                DElog.d(TAG, "mStateRecognitionListener constructor ok");
                this.mStateRecognitionListener = new Sink() {
                    public void onStateChanged(int stateType, int eventType, int pid, String pkg, int uid) {
                        String str = DisplayEngineService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("state type: ");
                        stringBuilder.append(stateType);
                        stringBuilder.append(" eventType:");
                        stringBuilder.append(eventType);
                        stringBuilder.append(" pid:");
                        stringBuilder.append(pid);
                        stringBuilder.append(" pkd:");
                        stringBuilder.append(pkg);
                        stringBuilder.append(" uid:");
                        stringBuilder.append(uid);
                        DElog.d(str, stringBuilder.toString());
                        if (DisplayEngineService.this.mHwBrightnessSceneRecognition != null && DisplayEngineService.this.mHwBrightnessSceneRecognition.isEnable() && DisplayEngineService.this.mNeedPkgNameFromPG && eventType == 1) {
                            if (pkg != null && pkg.length() > 0) {
                                DisplayEngineService.this.mHwBrightnessSceneRecognition.notifyTopApkChange(pkg);
                            }
                            str = DisplayEngineService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("PG pkg:");
                            stringBuilder.append(pkg);
                            DElog.i(str, stringBuilder.toString());
                        }
                        if (!DisplayEngineService.this.mPGEnable) {
                            return;
                        }
                        if (stateType == 10000) {
                            DisplayEngineService.this.setDetailPGscene(pkg);
                        } else if (stateType == IDisplayEngineService.DE_ACTION_PG_VIDEO_END || stateType == IDisplayEngineService.DE_ACTION_PG_CAMERA_END) {
                            DisplayEngineService.this.setScene(0, stateType);
                            DisplayEngineService.this.setDetailPGscene(pkg);
                        } else {
                            DisplayEngineService.this.setScene(0, stateType);
                        }
                    }
                };
            }
            initPGSdkState();
            return;
        }
        DElog.i(TAG, "mPGEnable false");
    }

    private void sendUIScene() {
        setScene(10, 16);
        this.mScreenOn = true;
    }

    private void registerRgbwBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("huawei.intent.action.RGBW_CONFIG_ACTION");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter, null, null);
    }

    private void setDefaultConfigValue() {
        this.mPGEnable = true;
        this.mNeedPkgNameFromPG = false;
    }

    private void getConfigParam() {
        try {
            if (!getConfig()) {
                DElog.e(TAG, "getConfig failed!");
                setDefaultConfigValue();
            }
        } catch (IOException e) {
            DElog.e(TAG, "getConfig failed setDefaultConfigValue!");
            setDefaultConfigValue();
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mPGEnable :");
        stringBuilder.append(this.mPGEnable);
        DElog.d(str, stringBuilder.toString());
    }

    /* JADX WARNING: Missing block: B:20:0x0046, code:
            if (r2 == null) goto L_0x0054;
     */
    /* JADX WARNING: Missing block: B:21:0x0048, code:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:23:0x004d, code:
            if (r2 == null) goto L_0x0054;
     */
    /* JADX WARNING: Missing block: B:25:0x0051, code:
            if (r2 == null) goto L_0x0054;
     */
    /* JADX WARNING: Missing block: B:26:0x0054, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean getConfig() throws IOException {
        DElog.i(TAG, "getConfig");
        File xmlFile = HwCfgFilePolicy.getCfgFile(SR_CONTROL_XML_FILE, 0);
        if (xmlFile == null) {
            DElog.w(TAG, "get xmlFile :/display/effect/displayengine/SR_control.xml failed!");
            return false;
        }
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(xmlFile);
            if (getConfigFromXML(inputStream)) {
                inputStream.close();
                inputStream.close();
                return true;
            }
            DElog.i(TAG, "get xmlFile error");
            inputStream.close();
            inputStream.close();
            return false;
        } catch (FileNotFoundException e) {
        } catch (IOException e2) {
        } catch (Exception e3) {
        } catch (Throwable th) {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:31:0x00fc A:{Catch:{ XmlPullParserException -> 0x014c, IOException -> 0x0134, NumberFormatException -> 0x011c, Exception -> 0x0104 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean getConfigFromXML(InputStream inStream) {
        String str;
        StringBuilder stringBuilder;
        DElog.i(TAG, "getConfigFromeXML");
        boolean configGroupLoadStarted = false;
        boolean loadFinished = false;
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(inStream, "UTF-8");
            for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                switch (eventType) {
                    case 2:
                        String name = parser.getName();
                        if (!name.equals("SRControl")) {
                            if (!name.equals("PGEnable")) {
                                if (!name.equals("NeedPkgNameFromPG")) {
                                    String str2;
                                    StringBuilder stringBuilder2;
                                    if (!name.equals("MinimumTrainingIntervalMinutes")) {
                                        if (!name.equals("ChargeLevelThreshold")) {
                                            if (name.equals("NewDragNumThreshold")) {
                                                this.mNewDragNumThreshold = Integer.parseInt(parser.nextText());
                                                str2 = TAG;
                                                stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("mNewDragNumThreshold = ");
                                                stringBuilder2.append(this.mNewDragNumThreshold);
                                                DElog.i(str2, stringBuilder2.toString());
                                                break;
                                            }
                                        }
                                        this.mChargeLevelThreshold = Integer.parseInt(parser.nextText());
                                        str2 = TAG;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("mChargeLevelThreshold = ");
                                        stringBuilder2.append(this.mChargeLevelThreshold);
                                        DElog.i(str2, stringBuilder2.toString());
                                        break;
                                    }
                                    this.mMinimumTrainingIntervalMillis = AppHibernateCst.DELAY_ONE_MINS * ((long) Integer.parseInt(parser.nextText()));
                                    str2 = TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("mMinimumTrainingIntervalMillis = ");
                                    stringBuilder2.append(this.mMinimumTrainingIntervalMillis);
                                    DElog.i(str2, stringBuilder2.toString());
                                    break;
                                }
                                this.mNeedPkgNameFromPG = Boolean.parseBoolean(parser.nextText());
                                break;
                            }
                            this.mPGEnable = Boolean.parseBoolean(parser.nextText());
                            break;
                        }
                        configGroupLoadStarted = true;
                        break;
                        break;
                    case 3:
                        if (parser.getName().equals("SRControl") && configGroupLoadStarted) {
                            loadFinished = true;
                            configGroupLoadStarted = false;
                            break;
                        }
                }
                if (loadFinished) {
                    if (loadFinished) {
                        DElog.i(TAG, "getConfigFromeXML success!");
                        return true;
                    }
                    DElog.e(TAG, "getConfigFromeXML false!");
                    return false;
                }
            }
            if (loadFinished) {
            }
        } catch (XmlPullParserException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("get xmlFile error: ");
            stringBuilder.append(e);
            DElog.e(str, stringBuilder.toString());
        } catch (IOException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("get xmlFile error: ");
            stringBuilder.append(e2);
            DElog.e(str, stringBuilder.toString());
        } catch (NumberFormatException e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("get xmlFile error: ");
            stringBuilder.append(e3);
            DElog.e(str, stringBuilder.toString());
        } catch (Exception e4) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("get xmlFile error: ");
            stringBuilder.append(e4);
            DElog.e(str, stringBuilder.toString());
        }
        DElog.e(TAG, "getConfigFromeXML false!");
        return false;
    }

    private void sendAmbientColorTempToMonitor(long time, int lux, int colorTemp) {
        if (this.mDisplayEffectMonitor != null) {
            if (this.mLastAmbientColorTempToMonitorTime == 0 || time <= this.mLastAmbientColorTempToMonitorTime) {
                this.mLastAmbientColorTempToMonitorTime = time;
                return;
            }
            int durationInMs = (int) (time - this.mLastAmbientColorTempToMonitorTime);
            this.mLastAmbientColorTempToMonitorTime = time;
            if (colorTemp > 0 && lux > 50) {
                ArrayMap<String, Object> params = new ArrayMap();
                params.put(MonitorModule.PARAM_TYPE, "ambientColorTempCollection");
                params.put("colorTempValue", Integer.valueOf(colorTemp));
                params.put("durationInMs", Integer.valueOf(durationInMs));
                this.mDisplayEffectMonitor.sendMonitorParam(params);
            }
        }
    }
}
