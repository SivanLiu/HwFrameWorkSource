package com.android.server.usb;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityManagerInternal.ScreenObserver;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.Notification.BigTextStyle;
import android.app.Notification.Builder;
import android.app.Notification.TvExtender;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbConfiguration;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;
import android.hardware.usb.gadget.V1_0.GadgetFunction;
import android.hardware.usb.gadget.V1_0.IUsbGadget;
import android.hardware.usb.gadget.V1_0.IUsbGadgetCallback;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification.Stub;
import android.net.Uri;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IHwBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.os.UEventObserver.UEvent;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.util.Flog;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.os.SomeArgs;
import com.android.internal.usb.DumpUtils;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.job.controllers.JobStatus;
import com.android.server.usb.descriptors.UsbDescriptor;
import com.android.server.utils.LogBufferUtil;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;

public class UsbDeviceManager extends AbsUsbDeviceManager implements ScreenObserver {
    private static final int ACCESSORY_REQUEST_TIMEOUT = 10000;
    private static final String ACCESSORY_START_MATCH = "DEVPATH=/devices/virtual/misc/usb_accessory";
    private static final String ACTION_USB_USER_UPDATE = "android.hardware.usb.action.USB_UPDATE";
    private static final String ADB_NOTIFICATION_CHANNEL_ID_TV = "usbdevicemanager.adb.tv";
    private static final String ALLOW_CHARGING_ADB = "allow_charging_adb";
    private static final int AUDIO_MODE_SOURCE = 1;
    private static final String AUDIO_SOURCE_PCM_PATH = "/sys/class/android_usb/android0/f_audio_source/pcm";
    private static final String BOOT_MODE_PROPERTY = "ro.bootmode";
    private static final String CHARGE_WATER_INSTRUSED_TYPE_PATH = "sys/class/hw_power/charger/charge_data/water_intrused";
    private static boolean DEBUG = false;
    private static final String FUNCTIONS_PATH = "/sys/class/android_usb/android0/functions";
    private static final String MIDI_ALSA_PATH = "/sys/class/android_usb/android0/f_midi/alsa";
    private static final int MSG_ACCESSORY_MODE_ENTER_TIMEOUT = 8;
    private static final int MSG_BOOT_COMPLETED = 4;
    private static final int MSG_ENABLE_ADB = 1;
    private static final int MSG_ENABLE_ALLOWCHARGINGADB = 104;
    private static final int MSG_ENABLE_HDB = 101;
    private static final int MSG_FUNCTION_SWITCH_TIMEOUT = 17;
    private static final int MSG_GET_CURRENT_USB_FUNCTIONS = 16;
    private static final int MSG_LOCALE_CHANGED = 11;
    private static final int MSG_MIRRORLINK_REQUESTED = 103;
    private static final int MSG_SET_CHARGING_FUNCTIONS = 14;
    private static final int MSG_SET_CURRENT_FUNCTIONS = 2;
    private static final int MSG_SET_FUNCTIONS_TIMEOUT = 15;
    private static final int MSG_SET_SCREEN_UNLOCKED_FUNCTIONS = 12;
    protected static final int MSG_SIM_COMPLETED = 102;
    private static final int MSG_SYSTEM_READY = 3;
    private static final int MSG_UPDATE_CHARGING_STATE = 9;
    private static final int MSG_UPDATE_HOST_STATE = 10;
    private static final int MSG_UPDATE_PORT_STATE = 7;
    private static final int MSG_UPDATE_SCREEN_LOCK = 13;
    private static final int MSG_UPDATE_STATE = 0;
    private static final int MSG_UPDATE_USER_RESTRICTIONS = 6;
    private static final int MSG_USER_SWITCHED = 5;
    private static final String NORMAL_BOOT = "normal";
    private static final String RNDIS_ETH_ADDR_PATH = "/sys/class/android_usb/android0/f_rndis/ethaddr";
    private static final String STATE_PATH = "/sys/class/android_usb/android0/state";
    private static final String SUITE_STATE_FILE = "android_usb/f_mass_storage/suitestate";
    private static final String SUITE_STATE_PATH = "/sys/class";
    private static final String TAG = UsbDeviceManager.class.getSimpleName();
    static final String UNLOCKED_CONFIG_PREF = "usb-screen-unlocked-config-%d";
    private static final int UPDATE_DELAY = 1000;
    private static final String USBDATA_UNLOCKED = "usbdata_unlocked";
    private static final String USB_PREFS_XML = "UsbDeviceManagerPrefs.xml";
    private static final String USB_STATE_MATCH = "DEVPATH=/devices/virtual/android_usb/android0";
    private static boolean isUSBLiquidOccur = false;
    private static boolean mHdbEnabled;
    private static Set<Integer> sBlackListedInterfaces = new HashSet();
    @GuardedBy("mLock")
    private String[] mAccessoryStrings;
    protected final ContentResolver mContentResolver;
    private final Context mContext;
    private HashMap<Long, FileDescriptor> mControlFds;
    @GuardedBy("mLock")
    private UsbProfileGroupSettingsManager mCurrentSettings;
    private UsbDebuggingManager mDebuggingManager;
    private UsbHandler mHandler;
    private final boolean mHasUsbAccessory;
    private boolean mLastChargingState;
    private final Object mLock = new Object();
    private final UEventObserver mUEventObserver;
    private boolean mUSBPlugType;
    private UserManager mUserManager;

    private class AdbSettingsObserver extends ContentObserver {
        public AdbSettingsObserver() {
            super(null);
        }

        public void onChange(boolean selfChange) {
            boolean z = false;
            if (Global.getInt(UsbDeviceManager.this.mContentResolver, "adb_enabled", 0) > 0) {
                z = true;
            }
            boolean enable = z;
            if (enable && UsbDeviceManager.this.isAdbDisabled()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(UsbDeviceManager.TAG);
                stringBuilder.append(" Adb is disabled by dpm");
                Flog.i(1306, stringBuilder.toString());
                return;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(UsbDeviceManager.TAG);
            stringBuilder2.append(" Adb Settings enable:");
            stringBuilder2.append(enable);
            Flog.i(1306, stringBuilder2.toString());
            UsbDeviceManager.this.mHandler.sendMessage(1, enable);
            LogBufferUtil.closeLogBufferAsNeed(UsbDeviceManager.this.mContext);
        }
    }

    abstract class UsbHandler extends Handler {
        private static final String ALLOW_CHARGING_ADB = "allow_charging_adb";
        protected static final String USB_PERSISTENT_CONFIG_PROPERTY = "persist.sys.usb.config";
        protected boolean mAdbEnabled;
        private boolean mAdbNotificationShown;
        private boolean mAudioAccessoryConnected;
        private boolean mAudioAccessorySupported;
        private boolean mAudioSourceEnabled;
        protected boolean mBootCompleted;
        private Intent mBroadcastedIntent;
        private boolean mChargingOnlySelected = true;
        private boolean mConfigured;
        private boolean mConnected;
        protected final ContentResolver mContentResolver;
        private final Context mContext;
        private UsbAccessory mCurrentAccessory;
        protected long mCurrentFunctions;
        protected boolean mCurrentFunctionsApplied;
        protected boolean mCurrentUsbFunctionsReceived;
        protected int mCurrentUser;
        private final UsbDebuggingManager mDebuggingManager;
        private boolean mHideUsbNotification;
        private boolean mHostConnected;
        private int mMidiCard;
        private int mMidiDevice;
        private boolean mMidiEnabled;
        private boolean mMirrorlinkRequested;
        private NotificationManager mNotificationManager;
        private boolean mPendingBootBroadcast;
        private boolean mPowerCharging;
        private final UEventObserver mPowerSupplyObserver = new UEventObserver() {
            public void onUEvent(UEvent event) {
                try {
                    if (UsbDeviceManager.NORMAL_BOOT.equals(SystemProperties.get("ro.runmode", UsbDeviceManager.NORMAL_BOOT))) {
                        String state = FileUtils.readTextFile(new File(UsbDeviceManager.CHARGE_WATER_INSTRUSED_TYPE_PATH), 0, null).trim();
                        String access$000 = UsbDeviceManager.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("water_intrused state= ");
                        stringBuilder.append(state);
                        Slog.i(access$000, stringBuilder.toString());
                        if (!UsbHandler.this.mBootCompleted) {
                            Slog.i(UsbDeviceManager.TAG, "boot not completed, do not send smart-notify broadcast");
                        } else if (state.equals("1")) {
                            UsbHandler.this.this$0.sendUSBLiquidBroadcast(UsbHandler.this.mContext, "1");
                            UsbDeviceManager.isUSBLiquidOccur = true;
                        } else if (UsbDeviceManager.isUSBLiquidOccur) {
                            UsbHandler.this.this$0.sendUSBLiquidBroadcast(UsbHandler.this.mContext, "0");
                            UsbDeviceManager.isUSBLiquidOccur = false;
                        }
                    }
                } catch (IOException e) {
                    Slog.e(UsbDeviceManager.TAG, "Error reading charge file", e);
                }
            }
        };
        private boolean mScreenLocked;
        protected long mScreenUnlockedFunctions;
        protected SharedPreferences mSettings;
        private final UsbSettingsManager mSettingsManager;
        private boolean mSinkPower;
        private boolean mSourcePower;
        private boolean mSupportsAllCombinations;
        private boolean mSystemReady;
        private final UsbAlsaManager mUsbAlsaManager;
        private boolean mUsbCharging;
        protected final UsbDeviceManager mUsbDeviceManager;
        private int mUsbNotificationId;
        protected boolean mUseUsbNotification;
        private boolean settingsHdbEnabled;
        final /* synthetic */ UsbDeviceManager this$0;

        private class AllowChargingAdbSettingsObserver extends ContentObserver {
            public AllowChargingAdbSettingsObserver() {
                super(null);
            }

            public void onChange(boolean selfChange) {
                boolean z = false;
                if (Global.getInt(UsbHandler.this.mContentResolver, UsbHandler.ALLOW_CHARGING_ADB, 0) > 0) {
                    z = true;
                }
                boolean enable = z;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(UsbDeviceManager.TAG);
                stringBuilder.append(" AllowChargingAdb Settings enable:");
                stringBuilder.append(enable);
                Flog.i(1306, stringBuilder.toString());
                UsbHandler.this.sendMessage(104, enable);
            }
        }

        private class HdbSettingsObserver extends ContentObserver {
            public HdbSettingsObserver() {
                super(null);
            }

            public void onChange(boolean selfChange, Uri uri, int userId) {
                boolean z = false;
                if (System.getIntForUser(UsbHandler.this.mContentResolver, "hdb_enabled", 0, userId) > 0) {
                    z = true;
                }
                boolean enable = z;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(UsbDeviceManager.TAG);
                stringBuilder.append(" Hdb Settings enable:");
                stringBuilder.append(enable);
                Flog.i(1306, stringBuilder.toString());
                UsbHandler.this.sendMessage(101, enable);
            }
        }

        private class SuitestateObserver extends ContentObserver {
            public SuitestateObserver() {
                super(null);
            }

            public void onChange(boolean selfChange) {
                UsbDeviceManager.writeSuitestate();
            }
        }

        protected abstract String addFunction(String str, String str2);

        protected abstract String removeFunction(String str, String str2);

        protected abstract void setEnabledFunctions(long j, boolean z);

        protected abstract void setEnabledFunctions(long j, boolean z, boolean z2);

        protected abstract void setUsbConfig(String str);

        protected abstract boolean waitForState(String str);

        UsbHandler(UsbDeviceManager this$0, Looper looper, Context context, UsbDeviceManager deviceManager, UsbDebuggingManager debuggingManager, UsbAlsaManager alsaManager, UsbSettingsManager settingsManager) {
            this.this$0 = this$0;
            super(looper);
            this.mContext = context;
            this.mDebuggingManager = debuggingManager;
            this.mUsbDeviceManager = deviceManager;
            this.mUsbAlsaManager = alsaManager;
            this.mSettingsManager = settingsManager;
            this.mContentResolver = context.getContentResolver();
            this.mCurrentUser = ActivityManager.getCurrentUser();
            this.mScreenLocked = true;
            this.mAdbEnabled = containsFunction(getSystemProperty(USB_PERSISTENT_CONFIG_PROPERTY, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS), "adb");
            Boolean isDocomo = Boolean.valueOf(SystemProperties.get("ro.product.custom", "NULL").contains("docomo"));
            if (SystemProperties.getInt("ro.debuggable", 0) == 1) {
                Global.putInt(this.mContentResolver, ALLOW_CHARGING_ADB, 1);
                this.mAdbEnabled = true;
            } else if (isDocomo.booleanValue()) {
                Global.putInt(this.mContentResolver, ALLOW_CHARGING_ADB, 1);
            }
            this.mSettings = getPinnedSharedPrefs(this.mContext);
            if (this.mSettings == null) {
                Slog.e(UsbDeviceManager.TAG, "Couldn't load shared preferences");
            } else {
                this.mScreenUnlockedFunctions = UsbManager.usbFunctionsFromString(this.mSettings.getString(String.format(Locale.ENGLISH, UsbDeviceManager.UNLOCKED_CONFIG_PREF, new Object[]{Integer.valueOf(this.mCurrentUser)}), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS));
            }
            boolean massStorageSupported = false;
            StorageManager storageManager = StorageManager.from(this.mContext);
            if (storageManager != null) {
                StorageVolume primary = storageManager.getPrimaryVolume();
                boolean z = primary != null && primary.allowMassStorage();
                massStorageSupported = z;
            }
            massStorageSupported = !massStorageSupported && this.mContext.getResources().getBoolean(17957056);
            this.mUseUsbNotification = massStorageSupported;
            try {
                if (System.getInt(this.mContentResolver, "hdb_enabled", 0) > 0) {
                    this.settingsHdbEnabled = true;
                }
                this.mContentResolver.registerContentObserver(Global.getUriFor(ALLOW_CHARGING_ADB), false, new AllowChargingAdbSettingsObserver());
                if (SystemProperties.get("persist.service.hdb.enable", "false").equals("true")) {
                    this.mContentResolver.registerContentObserver(System.getUriFor("hdb_enabled"), false, new HdbSettingsObserver());
                }
                this.mContentResolver.registerContentObserver(Secure.getUriFor("suitestate"), false, new SuitestateObserver());
                if (new File(UsbDeviceManager.CHARGE_WATER_INSTRUSED_TYPE_PATH).exists()) {
                    this.mPowerSupplyObserver.startObserving("SUBSYSTEM=power_supply");
                } else {
                    Slog.d(UsbDeviceManager.TAG, "charge file doesnt exist, product doesnt support the 'CHARGE_WATER_INSTRUSED' function.");
                }
            } catch (Exception e) {
                Slog.e(UsbDeviceManager.TAG, "Error initializing UsbHandler", e);
            }
        }

        public void sendMessage(int what, boolean arg) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            m.arg1 = arg;
            sendMessage(m);
        }

        public void sendMessage(int what, Object arg) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            m.obj = arg;
            sendMessage(m);
        }

        public void sendMessage(int what, Object arg, boolean arg1) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            m.obj = arg;
            m.arg1 = arg1;
            sendMessage(m);
        }

        public void sendMessage(int what, boolean arg1, boolean arg2) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            m.arg1 = arg1;
            m.arg2 = arg2;
            sendMessage(m);
        }

        public void sendMessageDelayed(int what, boolean arg, long delayMillis) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            m.arg1 = arg;
            sendMessageDelayed(m, delayMillis);
        }

        public void updateState(String state) {
            int connected;
            int configured;
            if ("DISCONNECTED".equals(state)) {
                connected = 0;
                configured = 0;
            } else if ("CONNECTED".equals(state)) {
                connected = 1;
                configured = 0;
            } else if ("CONFIGURED".equals(state)) {
                connected = 1;
                configured = 1;
            } else {
                String access$000 = UsbDeviceManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unknown state ");
                stringBuilder.append(state);
                Slog.e(access$000, stringBuilder.toString());
                return;
            }
            removeMessages(0);
            if (connected == 1) {
                removeMessages(17);
            }
            Message msg = Message.obtain(this, 0);
            msg.arg1 = connected;
            msg.arg2 = configured;
            sendMessageDelayed(msg, connected == 0 ? 1000 : 0);
        }

        public void updateHostState(UsbPort port, UsbPortStatus status) {
            if (UsbDeviceManager.DEBUG) {
                String access$000 = UsbDeviceManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateHostState ");
                stringBuilder.append(port);
                stringBuilder.append(" status=");
                stringBuilder.append(status);
                Slog.i(access$000, stringBuilder.toString());
            }
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = port;
            args.arg2 = status;
            removeMessages(7);
            sendMessageDelayed(obtainMessage(7, args), 1000);
        }

        private void setAdbEnabled(boolean enable) {
            if (UsbDeviceManager.DEBUG) {
                String access$000 = UsbDeviceManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setAdbEnabled: ");
                stringBuilder.append(enable);
                Slog.d(access$000, stringBuilder.toString());
            }
            if (enable != this.mAdbEnabled) {
                this.mAdbEnabled = enable;
                boolean isRepairMode = getUserManager().getUserInfo(ActivityManager.getCurrentUser()).isRepairMode();
                if (enable && isChargingOnly_N() && !isRepairMode) {
                    this.mAdbEnabled = false;
                    return;
                }
                String newFunction = SystemProperties.get(USB_PERSISTENT_CONFIG_PROPERTY, "none");
                if (this.this$0.mHandler != null) {
                    if (enable) {
                        newFunction = this.this$0.mHandler.addFunction(newFunction, "adb");
                    } else {
                        newFunction = this.this$0.mHandler.removeFunction(newFunction, "adb");
                    }
                }
                newFunction = applyHdbFunction(newFunction);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(UsbDeviceManager.TAG);
                stringBuilder2.append(" setAdbEnabled -> USB_PERSISTENT_CONFIG_PROPERTY : ");
                stringBuilder2.append(newFunction);
                Flog.i(1306, stringBuilder2.toString());
                setSystemProperty(USB_PERSISTENT_CONFIG_PROPERTY, newFunction);
                setEnabledFunctions(this.mCurrentFunctions, true);
                updateAdbNotification(false);
            }
            if (this.mDebuggingManager != null) {
                this.mDebuggingManager.setAdbEnabled(this.mAdbEnabled);
            }
        }

        private void setHdbEnabled(boolean enable) {
            if (UsbDeviceManager.DEBUG) {
                String access$000 = UsbDeviceManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setHdbEnabled: ");
                stringBuilder.append(enable);
                Slog.d(access$000, stringBuilder.toString());
            }
            if (enable != this.settingsHdbEnabled) {
                this.settingsHdbEnabled = enable;
                long oldFunctions = this.mCurrentFunctions;
                String defaultFunctions = SystemProperties.get(USB_PERSISTENT_CONFIG_PROPERTY, "none");
                String newFunctions = applyHdbFunction(defaultFunctions);
                if (!defaultFunctions.equals(newFunctions)) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(UsbDeviceManager.TAG);
                    stringBuilder2.append(" setHdbEnabled -> USB_PERSISTENT_CONFIG_PROPERTY : ");
                    stringBuilder2.append(newFunctions);
                    Flog.i(1306, stringBuilder2.toString());
                    SystemProperties.set(USB_PERSISTENT_CONFIG_PROPERTY, newFunctions);
                }
                if (getUserManager().getUserInfo(ActivityManager.getCurrentUser()).isRepairMode() && this.settingsHdbEnabled) {
                    oldFunctions = UsbManager.usbFunctionsFromString("mass_storage,hdb");
                }
                setEnabledFunctions(oldFunctions, true);
            }
            this.this$0.setHdbEnabledEx(this.settingsHdbEnabled);
        }

        protected boolean isAllowedAdbHdbApply() {
            return !this.mChargingOnlySelected || (Global.getInt(this.mContentResolver, ALLOW_CHARGING_ADB, 0) == 1);
        }

        protected boolean isChargingOnly_N() {
            if (isAllowedAdbHdbApply() || !this.mChargingOnlySelected) {
                return false;
            }
            return true;
        }

        private void updateUsbState(boolean enable) {
            if (!enable && isChargingOnly_N()) {
                Global.putInt(this.mContentResolver, "adb_enabled", 0);
                System.putInt(this.mContentResolver, "hdb_enabled", 0);
                setEnabledFunctions(0, false);
            }
        }

        protected String applyHdbFunction(String functions) {
            if (containsFunction(functions, "hdb")) {
                functions = removeFunction(functions, "hdb");
            }
            if (!HdbIsEnableFunction(functions)) {
                return functions;
            }
            String access$000;
            StringBuilder stringBuilder;
            if (UsbDeviceManager.mHdbEnabled) {
                if (!containsFunction(functions, "hdb")) {
                    functions = addFunction(functions, "hdb");
                }
                access$000 = UsbDeviceManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("add hdb is ");
                stringBuilder.append(functions);
                Slog.i(access$000, stringBuilder.toString());
                return functions;
            }
            functions = removeFunction(functions, "hdb");
            access$000 = UsbDeviceManager.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("remove hdb is ");
            stringBuilder.append(functions);
            Slog.i(access$000, stringBuilder.toString());
            return functions;
        }

        private boolean HdbIsEnableFunction(String functions) {
            if (this.this$0.isCmccUsbLimit()) {
                Slog.i(UsbDeviceManager.TAG, "cmcc_usb_limit do not set hdb");
                return false;
            } else if (getUserManager().getUserInfo(ActivityManager.getCurrentUser()).isRepairMode()) {
                return true;
            } else {
                if ((functions.equals("mtp") || functions.equals("mtp,adb") || functions.equals("ptp") || functions.equals("ptp,adb") || functions.equals("hisuite,mtp,mass_storage") || functions.equals("hisuite,mtp,mass_storage,adb") || functions.equals("bicr") || functions.equals("bicr,adb") || functions.equals("rndis") || functions.equals("rndis,adb")) && this.settingsHdbEnabled) {
                    return true;
                }
                return false;
            }
        }

        protected String applyUserRestrictions(String functions) {
            if (!((UserManager) this.mContext.getSystemService("user")).hasUserRestriction("no_usb_file_transfer")) {
                return functions;
            }
            functions = removeFunction(removeFunction(removeFunction(removeFunction(removeFunction(functions, "mtp"), "ptp"), "mass_storage"), "hisuite"), "hdb");
            if ("none".equals(functions)) {
                return "mtp";
            }
            return functions;
        }

        protected boolean isUsbTransferAllowed() {
            return ((UserManager) this.mContext.getSystemService("user")).hasUserRestriction("no_usb_file_transfer") ^ 1;
        }

        private void updateCurrentAccessory() {
            boolean enteringAccessoryMode = hasMessages(true);
            if (this.mConfigured && enteringAccessoryMode) {
                String[] accessoryStrings = this.mUsbDeviceManager.getAccessoryStrings();
                if (accessoryStrings != null) {
                    this.mCurrentAccessory = new UsbAccessory(accessoryStrings);
                    String access$000 = UsbDeviceManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("entering USB accessory mode: ");
                    stringBuilder.append(this.mCurrentAccessory);
                    Slog.d(access$000, stringBuilder.toString());
                    if (this.mBootCompleted) {
                        this.mUsbDeviceManager.getCurrentSettings().accessoryAttached(this.mCurrentAccessory);
                        return;
                    }
                    return;
                }
                Slog.e(UsbDeviceManager.TAG, "nativeGetAccessoryStrings failed");
            } else if (!enteringAccessoryMode) {
                notifyAccessoryModeExit();
            } else if (UsbDeviceManager.DEBUG) {
                Slog.v(UsbDeviceManager.TAG, "Debouncing accessory mode exit");
            }
        }

        private void notifyAccessoryModeExit() {
            Slog.d(UsbDeviceManager.TAG, "exited USB accessory mode");
            setEnabledFunctions(0, false);
            if (this.mCurrentAccessory != null) {
                if (this.mBootCompleted) {
                    this.mSettingsManager.usbAccessoryRemoved(this.mCurrentAccessory);
                }
                this.mCurrentAccessory = null;
            }
        }

        protected SharedPreferences getPinnedSharedPrefs(Context context) {
            return context.createDeviceProtectedStorageContext().getSharedPreferences(new File(Environment.getDataSystemDeDirectory(0), UsbDeviceManager.USB_PREFS_XML), 0);
        }

        private boolean isUsbStateChanged(Intent intent) {
            Set<String> keySet = intent.getExtras().keySet();
            if (this.mBroadcastedIntent == null) {
                for (String key : keySet) {
                    if (intent.getBooleanExtra(key, false)) {
                        return true;
                    }
                }
            } else if (!keySet.equals(this.mBroadcastedIntent.getExtras().keySet())) {
                return true;
            } else {
                for (String key2 : keySet) {
                    if (intent.getBooleanExtra(key2, false) != this.mBroadcastedIntent.getBooleanExtra(key2, false)) {
                        return true;
                    }
                }
            }
            return false;
        }

        protected void updateUsbStateBroadcastIfNeeded(long functions) {
            boolean z;
            Intent intent = new Intent("android.hardware.usb.action.USB_STATE");
            intent.addFlags(822083584);
            intent.putExtra("connected", this.mConnected);
            intent.putExtra("host_connected", this.mHostConnected);
            intent.putExtra("configured", this.mConfigured);
            String str = "unlocked";
            if (isUsbTransferAllowed() && isUsbDataTransferActive(this.mCurrentFunctions)) {
                z = true;
            } else {
                z = false;
            }
            intent.putExtra(str, z);
            intent.putExtra("only_charging", this.mPowerCharging);
            intent.putExtra("ncm_requested", this.mMirrorlinkRequested);
            for (long remainingFunctions = functions; remainingFunctions != 0; remainingFunctions -= Long.highestOneBit(remainingFunctions)) {
                intent.putExtra(UsbManager.usbFunctionsToString(Long.highestOneBit(remainingFunctions)), true);
            }
            StringBuilder stringBuilder;
            if (isUsbStateChanged(intent)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(UsbDeviceManager.TAG);
                stringBuilder.append("broadcasting ");
                stringBuilder.append(intent);
                stringBuilder.append(" extras: ");
                stringBuilder.append(intent.getExtras());
                Flog.i(1306, stringBuilder.toString());
                sendStickyBroadcast(intent);
                this.mBroadcastedIntent = intent;
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(UsbDeviceManager.TAG);
            stringBuilder.append("skip broadcasting ");
            stringBuilder.append(intent);
            stringBuilder.append(" extras: ");
            stringBuilder.append(intent.getExtras());
            Flog.i(1306, stringBuilder.toString());
        }

        protected void sendStickyBroadcast(Intent intent) {
            this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void updateUserBroadcast() {
            this.mContext.sendBroadcastAsUser(new Intent(UsbDeviceManager.ACTION_USB_USER_UPDATE), UserHandle.ALL, "android.permission.MANAGE_USB");
        }

        private void updateUsbFunctions() {
            updateMidiFunction();
        }

        private void updateMidiFunction() {
            boolean z = false;
            boolean enabled = (this.mCurrentFunctions & 8) != 0;
            if (enabled != this.mMidiEnabled) {
                if (enabled) {
                    Scanner scanner = null;
                    try {
                        scanner = new Scanner(new File(UsbDeviceManager.MIDI_ALSA_PATH));
                        this.mMidiCard = scanner.nextInt();
                        this.mMidiDevice = scanner.nextInt();
                    } catch (FileNotFoundException e) {
                        Slog.e(UsbDeviceManager.TAG, "could not open MIDI file", e);
                        enabled = false;
                        this.mMidiEnabled = enabled;
                    } finally {
                        if (scanner != null) {
                            scanner.close();
                        }
                    }
                }
                this.mMidiEnabled = enabled;
            }
            UsbAlsaManager usbAlsaManager = this.mUsbAlsaManager;
            if (this.mMidiEnabled && this.mConfigured) {
                z = true;
            }
            usbAlsaManager.setPeripheralMidiState(z, this.mMidiCard, this.mMidiDevice);
        }

        private void setScreenUnlockedFunctions() {
            setEnabledFunctions(this.mScreenUnlockedFunctions, false);
        }

        long getAppliedFunctions(long functions) {
            if (functions == 0) {
                return getChargingFunctions();
            }
            if (this.mAdbEnabled) {
                return 1 | functions;
            }
            return functions;
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            boolean z = false;
            boolean z2 = true;
            String access$000;
            StringBuilder stringBuilder;
            String access$0002;
            boolean prevHostConnected;
            switch (i) {
                case 0:
                    this.mConnected = msg.arg1 == 1;
                    this.mConfigured = msg.arg2 == 1;
                    if (UsbDeviceManager.DEBUG) {
                        Slog.v(UsbDeviceManager.TAG, "message update state ");
                    }
                    if (!this.mConnected) {
                        this.mMirrorlinkRequested = false;
                        updateMidiFunction();
                    }
                    updateUsbNotification(false);
                    if (!(isChargingOnly_N() && this.mConnected)) {
                        updateAdbNotification(false);
                    }
                    if (this.mBootCompleted) {
                        updateUsbStateBroadcastIfNeeded(getAppliedFunctions(this.mCurrentFunctions));
                    }
                    if ((this.mCurrentFunctions & 2) != 0) {
                        updateCurrentAccessory();
                    }
                    if (this.mBootCompleted) {
                        if (!(this.mConnected || hasMessages(8) || hasMessages(17))) {
                            this.mChargingOnlySelected = true;
                            if (this.mScreenLocked || this.mScreenUnlockedFunctions == 0) {
                                setEnabledFunctions(0, false);
                            } else {
                                setScreenUnlockedFunctions();
                            }
                        }
                        if (this.mSystemReady) {
                            updateUsbFunctions();
                            return;
                        }
                        return;
                    }
                    this.mPendingBootBroadcast = true;
                    return;
                case 1:
                    if (msg.arg1 == 1) {
                        z = true;
                    }
                    setAdbEnabled(z);
                    return;
                case 2:
                    long functions = ((Long) msg.obj).longValue();
                    access$000 = UsbDeviceManager.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Getting setFunction command for ");
                    stringBuilder.append(functions);
                    Slog.i(access$000, stringBuilder.toString());
                    if (functions != 0) {
                        this.mChargingOnlySelected = false;
                    } else {
                        this.mChargingOnlySelected = true;
                    }
                    if (!UsbDeviceManager.isDefaultFunction(functions) || functions == 0) {
                        setEnabledFunctions(functions, false);
                        return;
                    } else {
                        setEnabledFunctions(functions, false, true);
                        return;
                    }
                case 3:
                    this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
                    if (isTv()) {
                        this.mNotificationManager.createNotificationChannel(new NotificationChannel(UsbDeviceManager.ADB_NOTIFICATION_CHANNEL_ID_TV, this.mContext.getString(17039551), 4));
                    }
                    this.mSystemReady = true;
                    finishBoot();
                    return;
                case 4:
                    this.mBootCompleted = true;
                    finishBoot();
                    this.this$0.setHdbEnabledEx(this.settingsHdbEnabled);
                    return;
                case 5:
                    if (this.mCurrentUser != msg.arg1) {
                        if (UsbDeviceManager.DEBUG) {
                            access$0002 = UsbDeviceManager.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Current user switched to ");
                            stringBuilder2.append(msg.arg1);
                            Slog.v(access$0002, stringBuilder2.toString());
                        }
                        this.mCurrentUser = msg.arg1;
                        this.mScreenLocked = true;
                        this.mScreenUnlockedFunctions = 0;
                        if (this.mSettings != null) {
                            this.mScreenUnlockedFunctions = UsbManager.usbFunctionsFromString(this.mSettings.getString(String.format(Locale.ENGLISH, UsbDeviceManager.UNLOCKED_CONFIG_PREF, new Object[]{Integer.valueOf(this.mCurrentUser)}), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS));
                        }
                        setEnabledFunctions(0, false);
                        if (this.mBootCompleted) {
                            updateUserBroadcast();
                            handleRepairModeHdb();
                            return;
                        }
                        return;
                    }
                    return;
                case 6:
                    if (isUsbDataTransferActive(this.mCurrentFunctions) && !isUsbTransferAllowed()) {
                        setEnabledFunctions(0, true);
                        return;
                    }
                    return;
                case 7:
                    SomeArgs args = msg.obj;
                    prevHostConnected = this.mHostConnected;
                    UsbPort port = args.arg1;
                    UsbPortStatus status = args.arg2;
                    this.mHostConnected = status.getCurrentDataRole() == 1;
                    this.mSourcePower = status.getCurrentPowerRole() == 1;
                    this.mSinkPower = status.getCurrentPowerRole() == 2;
                    this.mAudioAccessoryConnected = status.getCurrentMode() == 4;
                    this.mAudioAccessorySupported = port.isModeSupported(4);
                    boolean z3 = status.isRoleCombinationSupported(1, 1) && status.isRoleCombinationSupported(2, 1) && status.isRoleCombinationSupported(1, 2) && status.isRoleCombinationSupported(2, 1);
                    this.mSupportsAllCombinations = z3;
                    args.recycle();
                    updateUsbNotification(false);
                    if (!this.mBootCompleted) {
                        this.mPendingBootBroadcast = true;
                        return;
                    } else if (this.mHostConnected || prevHostConnected) {
                        updateUsbStateBroadcastIfNeeded(getAppliedFunctions(this.mCurrentFunctions));
                        return;
                    } else {
                        return;
                    }
                case 8:
                    if (UsbDeviceManager.DEBUG) {
                        access$0002 = UsbDeviceManager.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Accessory mode enter timeout: ");
                        stringBuilder.append(this.mConnected);
                        Slog.v(access$0002, stringBuilder.toString());
                    }
                    if (!this.mConnected || (this.mCurrentFunctions & 2) == 0) {
                        notifyAccessoryModeExit();
                        return;
                    }
                    return;
                case 9:
                    if (msg.arg1 != 1) {
                        z2 = false;
                    }
                    this.mUsbCharging = z2;
                    updateUsbNotification(false);
                    return;
                case 10:
                    Iterator devices = msg.obj;
                    prevHostConnected = msg.arg1 == 1;
                    if (UsbDeviceManager.DEBUG) {
                        access$000 = UsbDeviceManager.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("HOST_STATE connected:");
                        stringBuilder.append(prevHostConnected);
                        Slog.i(access$000, stringBuilder.toString());
                    }
                    this.mHideUsbNotification = false;
                    while (devices.hasNext()) {
                        Entry pair = (Entry) devices.next();
                        if (UsbDeviceManager.DEBUG) {
                            String access$0003 = UsbDeviceManager.TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append(pair.getKey());
                            stringBuilder3.append(" = ");
                            stringBuilder3.append(pair.getValue());
                            Slog.i(access$0003, stringBuilder3.toString());
                        }
                        UsbDevice device = (UsbDevice) pair.getValue();
                        int configurationCount = device.getConfigurationCount() - 1;
                        while (configurationCount >= 0) {
                            UsbConfiguration config = device.getConfiguration(configurationCount);
                            configurationCount--;
                            int interfaceCount = config.getInterfaceCount() - 1;
                            while (interfaceCount >= 0) {
                                UsbInterface intrface = config.getInterface(interfaceCount);
                                interfaceCount--;
                                if (UsbDeviceManager.sBlackListedInterfaces.contains(Integer.valueOf(intrface.getInterfaceClass()))) {
                                    this.mHideUsbNotification = true;
                                }
                            }
                        }
                    }
                    updateUsbNotification(false);
                    return;
                case 11:
                    updateAdbNotification(true);
                    updateUsbNotification(true);
                    return;
                case 12:
                    this.mScreenUnlockedFunctions = ((Long) msg.obj).longValue();
                    if (this.mSettings != null) {
                        Editor editor = this.mSettings.edit();
                        editor.putString(String.format(Locale.ENGLISH, UsbDeviceManager.UNLOCKED_CONFIG_PREF, new Object[]{Integer.valueOf(this.mCurrentUser)}), UsbManager.usbFunctionsToString(this.mScreenUnlockedFunctions));
                        editor.commit();
                    }
                    if (!this.mScreenLocked && this.mScreenUnlockedFunctions != 0) {
                        setScreenUnlockedFunctions();
                        return;
                    }
                    return;
                case 13:
                    if ((msg.arg1 == 1) != this.mScreenLocked) {
                        if (msg.arg1 != 1) {
                            z2 = false;
                        }
                        this.mScreenLocked = z2;
                        if (!this.mBootCompleted) {
                            return;
                        }
                        if (this.mScreenLocked) {
                            if (!this.mConnected) {
                                setEnabledFunctions(0, false);
                                return;
                            }
                            return;
                        } else if (this.mScreenUnlockedFunctions != 0 && this.mCurrentFunctions == 0) {
                            setScreenUnlockedFunctions();
                            return;
                        } else {
                            return;
                        }
                    }
                    return;
                default:
                    switch (i) {
                        case 101:
                            if (msg.arg1 == 1) {
                                z = true;
                            }
                            setHdbEnabled(z);
                            return;
                        case 102:
                            this.this$0.dueSimStatusCompletedMsg();
                            return;
                        case 103:
                            this.mMirrorlinkRequested = true;
                            if (this.mBootCompleted) {
                                updateUsbStateBroadcastIfNeeded(getAppliedFunctions(this.mCurrentFunctions));
                            }
                            this.mMirrorlinkRequested = false;
                            return;
                        case 104:
                            if (msg.arg1 == 1) {
                                z = true;
                            }
                            updateUsbState(z);
                            return;
                        default:
                            return;
                    }
            }
        }

        private void handleRepairModeHdb() {
            if (this.mCurrentUser == 127) {
                this.mContentResolver.registerContentObserver(System.getUriFor("hdb_enabled"), false, new HdbSettingsObserver(), 127);
                if (SystemProperties.get("ro.product.locale.region", "null").equals("CN")) {
                    try {
                        System.putIntForUser(this.mContentResolver, "hdb_enabled", 1, 127);
                    } catch (Exception e) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(UsbDeviceManager.TAG);
                        stringBuilder.append(" set KEY_CONTENT_HDB_ALLOWED failed: ");
                        stringBuilder.append(e);
                        Flog.w(1306, stringBuilder.toString());
                    }
                }
            }
        }

        protected void finishBoot() {
            if (this.mBootCompleted && this.mCurrentUsbFunctionsReceived && this.mSystemReady) {
                if (this.mPendingBootBroadcast) {
                    updateUsbStateBroadcastIfNeeded(getAppliedFunctions(this.mCurrentFunctions));
                    this.mPendingBootBroadcast = false;
                }
                if (!this.mScreenLocked && this.mScreenUnlockedFunctions != 0) {
                    setScreenUnlockedFunctions();
                } else if ("factory".equals(SystemProperties.get("ro.runmode", UsbDeviceManager.NORMAL_BOOT)) && UsbManager.usbFunctionsToString(this.mCurrentFunctions) != null && containsFunction(UsbManager.usbFunctionsToString(this.mCurrentFunctions), "accessory")) {
                    Slog.i(UsbDeviceManager.TAG, "Boot complete, skip setting default functions in factory and accessory mode");
                } else {
                    Slog.i(UsbDeviceManager.TAG, "Boot complete, setting default functions");
                    setEnabledFunctions(0, false);
                }
                if (this.mCurrentAccessory != null) {
                    this.mUsbDeviceManager.getCurrentSettings().accessoryAttached(this.mCurrentAccessory);
                }
                if (this.mDebuggingManager != null) {
                    this.mDebuggingManager.setAdbEnabled(this.mAdbEnabled);
                }
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(UsbDeviceManager.TAG);
                    stringBuilder.append(" make sure ADB_ENABLED setting value when systemReady, mAdbEnabled is ");
                    stringBuilder.append(this.mAdbEnabled);
                    Flog.i(1306, stringBuilder.toString());
                    putGlobalSettings(this.mContentResolver, "adb_enabled", this.mAdbEnabled);
                } catch (SecurityException e) {
                    Slog.d(UsbDeviceManager.TAG, "ADB_ENABLED is restricted.");
                }
                updateUsbNotification(false);
                if (!isChargingOnly_N()) {
                    updateAdbNotification(false);
                }
                updateUsbFunctions();
            }
        }

        protected boolean isUsbDataTransferActive(long functions) {
            return ((4 & functions) == 0 && (16 & functions) == 0) ? false : true;
        }

        public UsbAccessory getCurrentAccessory() {
            return this.mCurrentAccessory;
        }

        private UserManager getUserManager() {
            if (this.this$0.mUserManager == null) {
                this.this$0.mUserManager = UserManager.get(this.this$0.getContext());
            }
            return this.this$0.mUserManager;
        }

        protected void updateUsbNotification(boolean force) {
            if (this.mNotificationManager != null && this.mUseUsbNotification && !"0".equals(getSystemProperty("persist.charging.notify", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS))) {
                if (UsbDeviceManager.DEBUG) {
                    String access$000 = UsbDeviceManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("update usb notification - mConnetced = ");
                    stringBuilder.append(this.mConnected);
                    Slog.v(access$000, stringBuilder.toString());
                }
                if (!this.mHideUsbNotification || this.mSupportsAllCombinations) {
                    Resources r = this.mContext.getResources();
                    CharSequence message = r.getText(17041284);
                    if (!this.mAudioAccessoryConnected || this.mAudioAccessorySupported) {
                        if (this.mConnected) {
                            if (!(this.mCurrentFunctions == 4 || this.mCurrentFunctions == 16 || this.mCurrentFunctions == 8 || this.mCurrentFunctions == 32)) {
                                int i = (this.mCurrentFunctions > 2 ? 1 : (this.mCurrentFunctions == 2 ? 0 : -1));
                            }
                        } else if (this.mHostConnected && this.mSinkPower) {
                            boolean z = this.mUsbCharging;
                        }
                    }
                    if (0 != this.mUsbNotificationId || force) {
                        if (this.mUsbNotificationId != 0) {
                            this.mNotificationManager.cancelAsUser(null, this.mUsbNotificationId, UserHandle.ALL);
                            Slog.d(UsbDeviceManager.TAG, "Clear notification");
                            this.mUsbNotificationId = 0;
                        }
                        if (null != null) {
                            String channel;
                            CharSequence title = r.getText(0);
                            PendingIntent pi;
                            if (null != 17041303) {
                                pi = PendingIntent.getActivityAsUser(this.mContext, 0, Intent.makeRestartActivityTask(new ComponentName("com.android.settings", "com.android.settings.Settings$UsbDetailsActivity")), 0, null, UserHandle.CURRENT);
                                channel = SystemNotificationChannels.USB;
                            } else {
                                Intent intent = new Intent();
                                intent.setClassName("com.android.settings", "com.android.settings.HelpTrampoline");
                                intent.putExtra("android.intent.extra.TEXT", "help_url_audio_accessory_not_supported");
                                if (this.mContext.getPackageManager().resolveActivity(intent, 0) != null) {
                                    pi = PendingIntent.getActivity(this.mContext, 0, intent, 0);
                                } else {
                                    pi = null;
                                }
                                channel = SystemNotificationChannels.ALERTS;
                                message = r.getText(17041302);
                            }
                            Builder builder = new Builder(this.mContext, channel).setSmallIcon(17303482).setWhen(0).setOngoing(true).setTicker(title).setDefaults(0).setColor(this.mContext.getColor(17170784)).setContentTitle(title).setContentText(message).setVisibility(1);
                            if (null == 17041303) {
                                builder.setStyle(new BigTextStyle().bigText(message));
                            }
                            this.mNotificationManager.notifyAsUser(null, 0, builder.build(), UserHandle.ALL);
                            String access$0002 = UsbDeviceManager.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("push notification:");
                            stringBuilder2.append(title);
                            Slog.d(access$0002, stringBuilder2.toString());
                            this.mUsbNotificationId = 0;
                        }
                    }
                    return;
                }
                if (this.mUsbNotificationId != 0) {
                    this.mNotificationManager.cancelAsUser(null, this.mUsbNotificationId, UserHandle.ALL);
                    this.mUsbNotificationId = 0;
                    Slog.d(UsbDeviceManager.TAG, "Clear notification");
                }
            }
        }

        protected void updateAdbNotification(boolean force) {
            if (this.mNotificationManager != null) {
                if (this.mAdbEnabled && this.mConnected && !"none".equals(SystemProperties.get(USB_PERSISTENT_CONFIG_PROPERTY))) {
                    if (!"0".equals(getSystemProperty("persist.adb.notify", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS))) {
                        if (force && this.mAdbNotificationShown) {
                            this.mAdbNotificationShown = false;
                            this.mNotificationManager.cancelAsUser(null, 26, UserHandle.ALL);
                        }
                        if (!this.mAdbNotificationShown) {
                            if (UsbDeviceManager.DEBUG) {
                                Slog.v(UsbDeviceManager.TAG, "update adb notification");
                            }
                            Resources r = this.mContext.getResources();
                            CharSequence title = r.getText(17039550);
                            CharSequence message = r.getText(33685802);
                            Intent intent = new Intent("android.settings.APPLICATION_DEVELOPMENT_SETTINGS");
                            intent.addFlags(268468224);
                            Notification notification = new Builder(this.mContext, SystemNotificationChannels.DEVELOPER).setSmallIcon(33751155).setWhen(0).setOngoing(true).setTicker(title).setDefaults(0).setContentTitle(title).setContentText(message).setContentIntent(PendingIntent.getActivityAsUser(this.mContext, 0, intent, 0, null, UserHandle.CURRENT)).setVisibility(1).extend(new TvExtender().setChannelId(UsbDeviceManager.ADB_NOTIFICATION_CHANNEL_ID_TV)).setVisibility(1).setStyle(new BigTextStyle().bigText(message)).build();
                            this.mAdbNotificationShown = true;
                            this.mNotificationManager.notifyAsUser(null, 26, notification, UserHandle.ALL);
                        }
                    }
                } else if (this.mAdbNotificationShown) {
                    if (UsbDeviceManager.DEBUG) {
                        Slog.v(UsbDeviceManager.TAG, "cancel adb notification");
                    }
                    this.mAdbNotificationShown = false;
                    this.mNotificationManager.cancelAsUser(null, 26, UserHandle.ALL);
                }
            }
        }

        private boolean isTv() {
            return this.mContext.getPackageManager().hasSystemFeature("android.software.leanback");
        }

        protected long getChargingFunctions() {
            String func = SystemProperties.get(getPersistProp(true), "none");
            if (UsbDeviceManager.DEBUG) {
                String access$000 = UsbDeviceManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" getChargingFunctions  = ");
                stringBuilder.append(func);
                Slog.d(access$000, stringBuilder.toString());
            }
            if ("none".equals(func)) {
                func = "mtp";
            }
            return UsbManager.usbFunctionsFromString(func);
        }

        private String getPersistProp(boolean functions) {
            String bootMode = SystemProperties.get(UsbDeviceManager.BOOT_MODE_PROPERTY, Shell.NIGHT_MODE_STR_UNKNOWN);
            String persistProp = USB_PERSISTENT_CONFIG_PROPERTY;
            if (bootMode.equals(UsbDeviceManager.NORMAL_BOOT) || bootMode.equals(Shell.NIGHT_MODE_STR_UNKNOWN)) {
                return persistProp;
            }
            StringBuilder stringBuilder;
            if (functions) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("persist.sys.usb.");
                stringBuilder.append(bootMode);
                stringBuilder.append(".func");
                return stringBuilder.toString();
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("persist.sys.usb.");
            stringBuilder.append(bootMode);
            stringBuilder.append(".config");
            return stringBuilder.toString();
        }

        protected void setSystemProperty(String prop, String val) {
            SystemProperties.set(prop, val);
        }

        protected String getSystemProperty(String prop, String def) {
            return SystemProperties.get(prop, def);
        }

        protected void putGlobalSettings(ContentResolver contentResolver, String setting, int val) {
            Global.putInt(contentResolver, setting, val);
        }

        public long getEnabledFunctions() {
            return this.mCurrentFunctions;
        }

        public long getScreenUnlockedFunctions() {
            return this.mScreenUnlockedFunctions;
        }

        private void dumpFunctions(DualDumpOutputStream dump, String idName, long id, long functions) {
            DualDumpOutputStream dualDumpOutputStream;
            String str;
            long j;
            for (int i = 0; i < 63; i++) {
                if ((functions & (1 << i)) != 0) {
                    if (dump.isProto()) {
                        dump.write(idName, id, 1 << i);
                    } else {
                        dump.write(idName, id, GadgetFunction.toString(1 << i));
                    }
                }
                dualDumpOutputStream = dump;
                str = idName;
                j = id;
            }
            dualDumpOutputStream = dump;
            str = idName;
            j = id;
        }

        public void dump(DualDumpOutputStream dump, String idName, long id) {
            DualDumpOutputStream dualDumpOutputStream = dump;
            long token = dump.start(idName, id);
            dumpFunctions(dualDumpOutputStream, "current_functions", 2259152797697L, this.mCurrentFunctions);
            dualDumpOutputStream.write("current_functions_applied", 1133871366146L, this.mCurrentFunctionsApplied);
            dumpFunctions(dualDumpOutputStream, "screen_unlocked_functions", 2259152797699L, this.mScreenUnlockedFunctions);
            dualDumpOutputStream.write("screen_locked", 1133871366148L, this.mScreenLocked);
            dualDumpOutputStream.write("connected", 1133871366149L, this.mConnected);
            dualDumpOutputStream.write("configured", 1133871366150L, this.mConfigured);
            if (this.mCurrentAccessory != null) {
                DumpUtils.writeAccessory(dualDumpOutputStream, "current_accessory", 1146756268039L, this.mCurrentAccessory);
            }
            dualDumpOutputStream.write("host_connected", 1133871366152L, this.mHostConnected);
            dualDumpOutputStream.write("source_power", 1133871366153L, this.mSourcePower);
            dualDumpOutputStream.write("sink_power", 1133871366154L, this.mSinkPower);
            dualDumpOutputStream.write("usb_charging", 1133871366155L, this.mUsbCharging);
            dualDumpOutputStream.write("hide_usb_notification", 1133871366156L, this.mHideUsbNotification);
            dualDumpOutputStream.write("audio_accessory_connected", 1133871366157L, this.mAudioAccessoryConnected);
            dualDumpOutputStream.write("adb_enabled", 1133871366158L, this.mAdbEnabled);
            try {
                com.android.internal.util.dump.DumpUtils.writeStringIfNotNull(dualDumpOutputStream, "kernel_state", 1138166333455L, FileUtils.readTextFile(new File(UsbDeviceManager.STATE_PATH), 0, null).trim());
            } catch (Exception e) {
                Slog.e(UsbDeviceManager.TAG, "Could not read kernel state", e);
            }
            try {
                com.android.internal.util.dump.DumpUtils.writeStringIfNotNull(dualDumpOutputStream, "kernel_function_list", 1138166333456L, FileUtils.readTextFile(new File(UsbDeviceManager.FUNCTIONS_PATH), 0, null).trim());
            } catch (Exception e2) {
                Slog.e(UsbDeviceManager.TAG, "Could not read kernel function list", e2);
            }
            dualDumpOutputStream.end(token);
        }

        protected boolean containsFunction(String functions, String function) {
            int index = functions.indexOf(function);
            if (index < 0) {
                return false;
            }
            if (index > 0 && functions.charAt(index - 1) != ',') {
                return false;
            }
            int charAfter = function.length() + index;
            if (charAfter >= functions.length() || functions.charAt(charAfter) == ',') {
                return true;
            }
            return false;
        }
    }

    private final class UsbUEventObserver extends UEventObserver {
        private UsbUEventObserver() {
        }

        /* synthetic */ UsbUEventObserver(UsbDeviceManager x0, AnonymousClass1 x1) {
            this();
        }

        public void onUEvent(UEvent event) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(UsbDeviceManager.TAG);
            stringBuilder.append("USB UEVENT: ");
            stringBuilder.append(event.toString());
            Flog.i(1306, stringBuilder.toString());
            String mirrorlink = event.get("MIRRORLINK");
            String state = event.get("USB_STATE");
            String accessory = event.get("ACCESSORY");
            if ("REQUESTED".equals(mirrorlink)) {
                UsbDeviceManager.this.mHandler.sendMessage(103, true);
            }
            if (state != null) {
                UsbDeviceManager.this.mHandler.updateState(state);
            } else if ("START".equals(accessory)) {
                if (UsbDeviceManager.DEBUG) {
                    Slog.d(UsbDeviceManager.TAG, "got accessory start");
                }
                UsbDeviceManager.this.startAccessoryMode();
            }
        }
    }

    private final class UsbHandlerHal extends UsbHandler {
        protected static final String ADBD = "adbd";
        protected static final String CTL_START = "ctl.start";
        protected static final String CTL_STOP = "ctl.stop";
        private static final int ENUMERATION_TIME_OUT_MS = 2000;
        private static final int SET_FUNCTIONS_LEEWAY_MS = 500;
        private static final int SET_FUNCTIONS_TIMEOUT_MS = 3000;
        private static final int USB_GADGET_HAL_DEATH_COOKIE = 2000;
        private int mCurrentRequest = 0;
        protected boolean mCurrentUsbFunctionsRequested;
        @GuardedBy("mGadgetProxyLock")
        private IUsbGadget mGadgetProxy;
        private final Object mGadgetProxyLock = new Object();

        final class ServiceNotification extends Stub {
            ServiceNotification() {
            }

            public void onRegistration(String fqName, String name, boolean preexisting) {
                String access$000 = UsbDeviceManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Usb gadget hal service started ");
                stringBuilder.append(fqName);
                stringBuilder.append(" ");
                stringBuilder.append(name);
                Slog.i(access$000, stringBuilder.toString());
                synchronized (UsbHandlerHal.this.mGadgetProxyLock) {
                    try {
                        UsbHandlerHal.this.mGadgetProxy = IUsbGadget.getService();
                        UsbHandlerHal.this.mGadgetProxy.linkToDeath(new UsbGadgetDeathRecipient(), 2000);
                        if (!(UsbHandlerHal.this.mCurrentFunctionsApplied || UsbHandlerHal.this.mCurrentUsbFunctionsRequested)) {
                            UsbHandlerHal.this.setEnabledFunctions(UsbHandlerHal.this.mCurrentFunctions, false);
                        }
                    } catch (NoSuchElementException e) {
                        Slog.e(UsbDeviceManager.TAG, "Usb gadget hal not found", e);
                    } catch (RemoteException e2) {
                        Slog.e(UsbDeviceManager.TAG, "Usb Gadget hal not responding", e2);
                    }
                }
            }
        }

        final class UsbGadgetDeathRecipient implements DeathRecipient {
            UsbGadgetDeathRecipient() {
            }

            public void serviceDied(long cookie) {
                if (cookie == 2000) {
                    String access$000 = UsbDeviceManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Usb Gadget hal service died cookie: ");
                    stringBuilder.append(cookie);
                    Slog.e(access$000, stringBuilder.toString());
                    synchronized (UsbHandlerHal.this.mGadgetProxyLock) {
                        UsbHandlerHal.this.mGadgetProxy = null;
                    }
                }
            }
        }

        private class UsbGadgetCallback extends IUsbGadgetCallback.Stub {
            boolean mChargingFunctions;
            long mFunctions;
            int mRequest;

            UsbGadgetCallback() {
            }

            UsbGadgetCallback(int request, long functions, boolean chargingFunctions) {
                this.mRequest = request;
                this.mFunctions = functions;
                this.mChargingFunctions = chargingFunctions;
            }

            public void setCurrentUsbFunctionsCb(long functions, int status) {
                if (UsbHandlerHal.this.mCurrentRequest == this.mRequest && UsbHandlerHal.this.hasMessages(15) && this.mFunctions == functions) {
                    UsbHandlerHal.this.removeMessages(15);
                    String access$000 = UsbDeviceManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("notifyCurrentFunction request:");
                    stringBuilder.append(this.mRequest);
                    stringBuilder.append(" status:");
                    stringBuilder.append(status);
                    Slog.e(access$000, stringBuilder.toString());
                    if (status == 0) {
                        UsbHandlerHal.this.mCurrentFunctionsApplied = true;
                    } else if (!this.mChargingFunctions) {
                        Slog.e(UsbDeviceManager.TAG, "Setting default fuctions");
                        UsbHandlerHal.this.sendEmptyMessage(14);
                    }
                }
            }

            public void getCurrentUsbFunctionsCb(long functions, int status) {
                UsbHandlerHal.this.sendMessage(16, (Object) Long.valueOf(functions), status == 2);
            }
        }

        UsbHandlerHal(Looper looper, Context context, UsbDeviceManager deviceManager, UsbDebuggingManager debuggingManager, UsbAlsaManager alsaManager, UsbSettingsManager settingsManager) {
            super(UsbDeviceManager.this, looper, context, deviceManager, debuggingManager, alsaManager, settingsManager);
            try {
                if (IServiceManager.getService().registerForNotifications(IUsbGadget.kInterfaceName, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, new ServiceNotification())) {
                    synchronized (this.mGadgetProxyLock) {
                        this.mGadgetProxy = IUsbGadget.getService(true);
                        this.mGadgetProxy.linkToDeath(new UsbGadgetDeathRecipient(), 2000);
                        this.mCurrentFunctions = 0;
                        this.mGadgetProxy.getCurrentUsbFunctions(new UsbGadgetCallback());
                        this.mCurrentUsbFunctionsRequested = true;
                    }
                    updateState(FileUtils.readTextFile(new File(UsbDeviceManager.STATE_PATH), 0, null).trim());
                    return;
                }
                Slog.e(UsbDeviceManager.TAG, "Failed to register usb gadget service start notification");
            } catch (NoSuchElementException e) {
                Slog.e(UsbDeviceManager.TAG, "Usb gadget hal not found", e);
            } catch (RemoteException e2) {
                Slog.e(UsbDeviceManager.TAG, "Usb Gadget hal not responding", e2);
            } catch (Exception e3) {
                Slog.e(UsbDeviceManager.TAG, "Error initializing UsbHandler", e3);
            }
        }

        public void handleMessage(Message msg) {
            boolean z = false;
            switch (msg.what) {
                case 14:
                    setEnabledFunctions(0, false);
                    return;
                case 15:
                    Slog.e(UsbDeviceManager.TAG, "Set functions timed out! no reply from usb hal");
                    if (msg.arg1 != 1) {
                        setEnabledFunctions(0, false);
                        return;
                    }
                    return;
                case 16:
                    Slog.e(UsbDeviceManager.TAG, "prcessing MSG_GET_CURRENT_USB_FUNCTIONS");
                    this.mCurrentUsbFunctionsReceived = true;
                    if (this.mCurrentUsbFunctionsRequested) {
                        Slog.e(UsbDeviceManager.TAG, "updating mCurrentFunctions");
                        this.mCurrentFunctions = ((Long) msg.obj).longValue() & -2;
                        String access$000 = UsbDeviceManager.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("mCurrentFunctions:");
                        stringBuilder.append(this.mCurrentFunctions);
                        stringBuilder.append("applied:");
                        stringBuilder.append(msg.arg1);
                        Slog.e(access$000, stringBuilder.toString());
                        if (msg.arg1 == 1) {
                            z = true;
                        }
                        this.mCurrentFunctionsApplied = z;
                    }
                    finishBoot();
                    return;
                case 17:
                    if (msg.arg1 != 1) {
                        setEnabledFunctions(0, this.mAdbEnabled ^ 1);
                        return;
                    }
                    return;
                default:
                    super.handleMessage(msg);
                    return;
            }
        }

        private void setUsbConfig(long config, boolean chargingFunctions) {
            String access$000 = UsbDeviceManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setUsbConfig( long ");
            stringBuilder.append(config);
            stringBuilder.append(") request:");
            int i = this.mCurrentRequest + 1;
            this.mCurrentRequest = i;
            stringBuilder.append(i);
            Slog.d(access$000, stringBuilder.toString());
            removeMessages(17);
            removeMessages(15);
            removeMessages(14);
            synchronized (this.mGadgetProxyLock) {
                if (this.mGadgetProxy == null) {
                    Slog.e(UsbDeviceManager.TAG, "setUsbConfig mGadgetProxy is null");
                    return;
                }
                if ((1 & config) != 0) {
                    try {
                        setSystemProperty(CTL_START, ADBD);
                    } catch (RemoteException e) {
                        Slog.e(UsbDeviceManager.TAG, "Remoteexception while calling setCurrentUsbFunctions", e);
                        return;
                    }
                }
                setSystemProperty(CTL_STOP, ADBD);
                this.mGadgetProxy.setCurrentUsbFunctions(config, new UsbGadgetCallback(this.mCurrentRequest, config, chargingFunctions), 2500);
                sendMessageDelayed(15, chargingFunctions, 3000);
                sendMessageDelayed(17, chargingFunctions, 5000);
                if (UsbDeviceManager.DEBUG) {
                    Slog.d(UsbDeviceManager.TAG, "timeout message queued");
                }
            }
        }

        protected String removeFunction(String functions, String function) {
            return functions;
        }

        protected String addFunction(String functions, String function) {
            return functions;
        }

        protected void setUsbConfig(String config) {
        }

        protected boolean waitForState(String state) {
            return state != null;
        }

        protected void setEnabledFunctions(long functions, boolean forceRestart) {
            setEnabledFunctions(functions, forceRestart, false);
        }

        protected void setEnabledFunctions(long functions, boolean forceRestart, boolean makeDefault) {
            String access$000;
            StringBuilder stringBuilder;
            if (UsbDeviceManager.DEBUG) {
                access$000 = UsbDeviceManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("setEnabledFunctions functions=");
                stringBuilder.append(functions);
                stringBuilder.append(", forceRestart=");
                stringBuilder.append(forceRestart);
                stringBuilder.append(", makeDefault=");
                stringBuilder.append(makeDefault);
                Slog.d(access$000, stringBuilder.toString());
            }
            if (this.mCurrentFunctions != functions || !this.mCurrentFunctionsApplied || forceRestart) {
                access$000 = UsbDeviceManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Setting USB config to ");
                stringBuilder.append(UsbManager.usbFunctionsToString(functions));
                Slog.i(access$000, stringBuilder.toString());
                this.mCurrentFunctions = functions;
                boolean chargingFunctions = false;
                this.mCurrentFunctionsApplied = false;
                this.mCurrentUsbFunctionsRequested = false;
                if (functions == 0) {
                    chargingFunctions = true;
                }
                functions = getAppliedFunctions(functions);
                setUsbConfig(functions, chargingFunctions);
                if (this.mBootCompleted && isUsbDataTransferActive(functions)) {
                    updateUsbStateBroadcastIfNeeded(functions);
                }
            }
        }
    }

    private final class UsbHandlerLegacy extends UsbHandler {
        private static final String USB_CONFIG_PROPERTY = "sys.usb.config";
        private static final String USB_STATE_PROPERTY = "sys.usb.state";
        private String mCurrentFunctionsStr;
        private String mCurrentOemFunctions;
        private HashMap<String, HashMap<String, Pair<String, String>>> mOemModeMap;
        private boolean mUsbDataUnlocked;

        UsbHandlerLegacy(Looper looper, Context context, UsbDeviceManager deviceManager, UsbDebuggingManager debuggingManager, UsbAlsaManager alsaManager, UsbSettingsManager settingsManager) {
            super(UsbDeviceManager.this, looper, context, deviceManager, debuggingManager, alsaManager, settingsManager);
            try {
                readOemUsbOverrideConfig(context);
                this.mCurrentOemFunctions = getSystemProperty(getPersistProp(false), "none");
                if (isNormalBoot()) {
                    this.mCurrentFunctionsStr = getSystemProperty(USB_CONFIG_PROPERTY, "none");
                    this.mCurrentFunctionsApplied = this.mCurrentFunctionsStr.equals(getSystemProperty(USB_STATE_PROPERTY, "none"));
                } else {
                    this.mCurrentFunctionsStr = getSystemProperty(getPersistProp(true), "none");
                    this.mCurrentFunctionsApplied = getSystemProperty(USB_CONFIG_PROPERTY, "none").equals(getSystemProperty(USB_STATE_PROPERTY, "none"));
                }
                this.mCurrentFunctions = UsbManager.usbFunctionsFromString(this.mCurrentFunctionsStr);
                this.mCurrentUsbFunctionsReceived = true;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(UsbDeviceManager.TAG);
                stringBuilder.append(" mCurrentFunctions:");
                stringBuilder.append(this.mCurrentFunctionsStr);
                stringBuilder.append(",mCurrentFunctionsApplied:");
                stringBuilder.append(this.mCurrentFunctionsApplied);
                stringBuilder.append(",persistProp:");
                stringBuilder.append(SystemProperties.get("persist.sys.usb.config"));
                Flog.i(1306, stringBuilder.toString());
                updateState(FileUtils.readTextFile(new File(UsbDeviceManager.STATE_PATH), 0, null).trim());
            } catch (Exception e) {
                Slog.e(UsbDeviceManager.TAG, "Error initializing UsbHandler", e);
            }
        }

        private void readOemUsbOverrideConfig(Context context) {
            String[] configList = context.getResources().getStringArray(17236027);
            if (configList != null) {
                for (String config : configList) {
                    String[] items = config.split(":");
                    if (items.length == 3 || items.length == 4) {
                        if (this.mOemModeMap == null) {
                            this.mOemModeMap = new HashMap();
                        }
                        HashMap<String, Pair<String, String>> overrideMap = (HashMap) this.mOemModeMap.get(items[0]);
                        if (overrideMap == null) {
                            overrideMap = new HashMap();
                            this.mOemModeMap.put(items[0], overrideMap);
                        }
                        if (!overrideMap.containsKey(items[1])) {
                            if (items.length == 3) {
                                overrideMap.put(items[1], new Pair(items[2], BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS));
                            } else {
                                overrideMap.put(items[1], new Pair(items[2], items[3]));
                            }
                        }
                    }
                }
            }
        }

        private String applyOemOverrideFunction(String usbFunctions) {
            if (usbFunctions == null || this.mOemModeMap == null) {
                return usbFunctions;
            }
            String bootMode = getSystemProperty(UsbDeviceManager.BOOT_MODE_PROPERTY, Shell.NIGHT_MODE_STR_UNKNOWN);
            String access$000 = UsbDeviceManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("applyOemOverride usbfunctions=");
            stringBuilder.append(usbFunctions);
            stringBuilder.append(" bootmode=");
            stringBuilder.append(bootMode);
            Slog.d(access$000, stringBuilder.toString());
            Map<String, Pair<String, String>> overridesMap = (Map) this.mOemModeMap.get(bootMode);
            if (!(overridesMap == null || bootMode.equals(UsbDeviceManager.NORMAL_BOOT) || bootMode.equals(Shell.NIGHT_MODE_STR_UNKNOWN))) {
                Pair<String, String> overrideFunctions = (Pair) overridesMap.get(usbFunctions);
                if (overrideFunctions != null) {
                    String access$0002 = UsbDeviceManager.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("OEM USB override: ");
                    stringBuilder2.append(usbFunctions);
                    stringBuilder2.append(" ==> ");
                    stringBuilder2.append((String) overrideFunctions.first);
                    stringBuilder2.append(" persist across reboot ");
                    stringBuilder2.append((String) overrideFunctions.second);
                    Slog.d(access$0002, stringBuilder2.toString());
                    if (!((String) overrideFunctions.second).equals(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS)) {
                        if (this.mAdbEnabled) {
                            access$0002 = addFunction((String) overrideFunctions.second, "adb");
                        } else {
                            access$0002 = (String) overrideFunctions.second;
                        }
                        String access$0003 = UsbDeviceManager.TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("OEM USB override persisting: ");
                        stringBuilder3.append(access$0002);
                        stringBuilder3.append("in prop: ");
                        stringBuilder3.append(getPersistProp(false));
                        Slog.d(access$0003, stringBuilder3.toString());
                        setSystemProperty(getPersistProp(false), access$0002);
                    }
                    return (String) overrideFunctions.first;
                } else if (this.mAdbEnabled) {
                    setSystemProperty(getPersistProp(false), addFunction("none", "adb"));
                } else {
                    setSystemProperty(getPersistProp(false), "none");
                }
            }
            return usbFunctions;
        }

        protected void setEnabledFunctions(long usbFunctions, boolean forceRestart) {
            setEnabledFunctions(usbFunctions, forceRestart, false);
        }

        protected void setEnabledFunctions(long usbFunctions, boolean forceRestart, boolean makeDefault) {
            boolean usbDataUnlocked = isUsbDataTransferActive(usbFunctions);
            if (UsbDeviceManager.DEBUG) {
                String access$000 = UsbDeviceManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setEnabledFunctions usbFunctions=");
                stringBuilder.append(usbFunctions);
                stringBuilder.append(", forceRestart=");
                stringBuilder.append(forceRestart);
                stringBuilder.append(", usbDataUnlocked=");
                stringBuilder.append(usbDataUnlocked);
                stringBuilder.append(", makeDefault=");
                stringBuilder.append(makeDefault);
                Slog.d(access$000, stringBuilder.toString());
            }
            if (!UsbDeviceManager.this.interceptSetEnabledFunctions(UsbManager.usbFunctionsToString(usbFunctions))) {
                if (usbDataUnlocked != this.mUsbDataUnlocked) {
                    this.mUsbDataUnlocked = usbDataUnlocked;
                    updateUsbNotification(false);
                    forceRestart = true;
                }
                long oldFunctions = this.mCurrentFunctions;
                boolean oldFunctionsApplied = this.mCurrentFunctionsApplied;
                if (!trySetEnabledFunctions(usbFunctions, forceRestart, makeDefault)) {
                    if (oldFunctionsApplied && oldFunctions != usbFunctions) {
                        Slog.e(UsbDeviceManager.TAG, "Failsafe 1: Restoring previous USB functions.");
                        if (trySetEnabledFunctions(oldFunctions, false)) {
                            return;
                        }
                    }
                    Slog.e(UsbDeviceManager.TAG, "Failsafe 2: Restoring default USB functions.");
                    if (!trySetEnabledFunctions(0, false)) {
                        Slog.e(UsbDeviceManager.TAG, "Failsafe 3: Restoring empty function list (with ADB if enabled).");
                        if (!trySetEnabledFunctions(0, false)) {
                            Slog.e(UsbDeviceManager.TAG, "Unable to set any USB functions!");
                        }
                    }
                }
            }
        }

        private boolean isNormalBoot() {
            String bootMode = getSystemProperty(UsbDeviceManager.BOOT_MODE_PROPERTY, Shell.NIGHT_MODE_STR_UNKNOWN);
            return bootMode.equals(UsbDeviceManager.NORMAL_BOOT) || bootMode.equals(Shell.NIGHT_MODE_STR_UNKNOWN);
        }

        protected String applyAdbFunction(String functions) {
            if (functions == null) {
                functions = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            }
            if (this.mAdbEnabled) {
                return addFunction(functions, "adb");
            }
            if (functions.contains("manufacture")) {
                return functions;
            }
            return removeFunction(functions, "adb");
        }

        private boolean trySetEnabledFunctions(long usbFunctions, boolean forceRestart) {
            return trySetEnabledFunctions(usbFunctions, forceRestart, false);
        }

        private boolean trySetEnabledFunctions(long usbFunctions, boolean forceRestart, boolean makeDefault) {
            String functions = null;
            if (usbFunctions != 0) {
                functions = UsbManager.usbFunctionsToString(usbFunctions);
            }
            this.mCurrentFunctions = usbFunctions;
            if (functions == null || applyAdbFunction(functions).equals("none")) {
                functions = UsbManager.usbFunctionsToString(getChargingFunctions());
                if (isAllowedAdbHdbApply() || functions.contains("manufacture")) {
                    functions = applyHdbFunction(applyAdbFunction(functions));
                } else {
                    functions = removeFunction(removeFunction(functions, "adb"), "hdb");
                }
            } else if (!isChargingOnly_N()) {
                functions = applyHdbFunction(applyAdbFunction(functions));
            }
            functions = applyUserRestrictions(functions);
            String oemFunctions = applyOemOverrideFunction(functions);
            if (makeDefault && !UsbManager.usbFunctionsToString(getChargingFunctions()).equals(oemFunctions)) {
                forceRestart = true;
            }
            if (!(isNormalBoot() || this.mCurrentFunctionsStr.equals(functions))) {
                setSystemProperty(getPersistProp(true), functions);
            }
            if (!((functions.equals(oemFunctions) || this.mCurrentOemFunctions.equals(oemFunctions)) && this.mCurrentFunctionsStr.equals(functions) && this.mCurrentFunctionsApplied && !forceRestart)) {
                String access$000 = UsbDeviceManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Setting USB config to ");
                stringBuilder.append(functions);
                stringBuilder.append(" oemFunctions = ");
                stringBuilder.append(oemFunctions);
                Slog.i(access$000, stringBuilder.toString());
                this.mCurrentFunctionsStr = functions;
                this.mCurrentOemFunctions = oemFunctions;
                this.mCurrentFunctionsApplied = false;
                setUsbConfig("none");
                if (waitForState("none")) {
                    if (makeDefault) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(UsbDeviceManager.TAG);
                        stringBuilder2.append(" setDefaultFunctions -> USB_PERSISTENT_CONFIG_PROPERTY : ");
                        stringBuilder2.append(oemFunctions);
                        Flog.i(1306, stringBuilder2.toString());
                        SystemProperties.set("persist.sys.usb.config", oemFunctions);
                    }
                    setUsbConfig(oemFunctions);
                    if (this.mBootCompleted && (containsFunction(functions, "mtp") || containsFunction(functions, "ptp"))) {
                        updateUsbStateBroadcastIfNeeded(getAppliedFunctions(this.mCurrentFunctions));
                    }
                    if (waitForState(oemFunctions)) {
                        this.mCurrentFunctionsStr = functions;
                        this.mCurrentFunctionsApplied = true;
                    } else {
                        String access$0002 = UsbDeviceManager.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to switch USB config to ");
                        stringBuilder.append(functions);
                        Slog.e(access$0002, stringBuilder.toString());
                        return false;
                    }
                }
                Slog.e(UsbDeviceManager.TAG, "Failed to kick USB config");
                return false;
            }
            return true;
        }

        private String getPersistProp(boolean functions) {
            String bootMode = getSystemProperty(UsbDeviceManager.BOOT_MODE_PROPERTY, Shell.NIGHT_MODE_STR_UNKNOWN);
            String persistProp = "persist.sys.usb.config";
            if (bootMode.equals(UsbDeviceManager.NORMAL_BOOT) || bootMode.equals(Shell.NIGHT_MODE_STR_UNKNOWN)) {
                return persistProp;
            }
            StringBuilder stringBuilder;
            if (functions) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("persist.sys.usb.");
                stringBuilder.append(bootMode);
                stringBuilder.append(".func");
                return stringBuilder.toString();
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("persist.sys.usb.");
            stringBuilder.append(bootMode);
            stringBuilder.append(".config");
            return stringBuilder.toString();
        }

        protected boolean waitForState(String state) {
            String value = null;
            for (int i = 0; i < 40; i++) {
                value = getSystemProperty(USB_STATE_PROPERTY, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                if (state.equals(value)) {
                    return true;
                }
                SystemClock.sleep(50);
            }
            String access$000 = UsbDeviceManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("waitForState(");
            stringBuilder.append(state);
            stringBuilder.append(") FAILED: got ");
            stringBuilder.append(value);
            Slog.e(access$000, stringBuilder.toString());
            return false;
        }

        protected void setUsbConfig(String config) {
            if (UsbDeviceManager.DEBUG) {
                String access$000 = UsbDeviceManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setUsbConfig(string ");
                stringBuilder.append(config);
                stringBuilder.append(")");
                Slog.d(access$000, stringBuilder.toString());
            }
            setSystemProperty(USB_CONFIG_PROPERTY, config);
        }

        protected String addFunction(String functions, String function) {
            if ("none".equals(functions)) {
                return function;
            }
            if (!containsFunction(functions, function)) {
                StringBuilder stringBuilder;
                if (functions.length() > 0) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(functions);
                    stringBuilder.append(",");
                    functions = stringBuilder.toString();
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append(functions);
                stringBuilder.append(function);
                functions = stringBuilder.toString();
            }
            return functions;
        }

        protected String removeFunction(String functions, String function) {
            String[] split = functions.split(",");
            int i = 0;
            for (int i2 = 0; i2 < split.length; i2++) {
                if (function.equals(split[i2])) {
                    split[i2] = null;
                }
            }
            if (split.length == 1 && split[0] == null) {
                return "none";
            }
            StringBuilder builder = new StringBuilder();
            while (i < split.length) {
                String s = split[i];
                if (s != null) {
                    if (builder.length() > 0) {
                        builder.append(",");
                    }
                    builder.append(s);
                }
                i++;
            }
            return builder.toString();
        }
    }

    private native String[] nativeGetAccessoryStrings();

    private native int nativeGetAudioMode();

    private native boolean nativeIsStartRequested();

    private native ParcelFileDescriptor nativeOpenAccessory();

    private native FileDescriptor nativeOpenControl(String str);

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG = z;
        sBlackListedInterfaces.add(Integer.valueOf(1));
        sBlackListedInterfaces.add(Integer.valueOf(2));
        sBlackListedInterfaces.add(Integer.valueOf(3));
        sBlackListedInterfaces.add(Integer.valueOf(7));
        sBlackListedInterfaces.add(Integer.valueOf(8));
        sBlackListedInterfaces.add(Integer.valueOf(9));
        sBlackListedInterfaces.add(Integer.valueOf(10));
        sBlackListedInterfaces.add(Integer.valueOf(11));
        sBlackListedInterfaces.add(Integer.valueOf(13));
        sBlackListedInterfaces.add(Integer.valueOf(14));
        sBlackListedInterfaces.add(Integer.valueOf(UsbDescriptor.CLASSID_WIRELESS));
    }

    public void onKeyguardStateChanged(boolean isShowing) {
        int userHandle = ActivityManager.getCurrentUser();
        KeyguardManager keyguardmanager = (KeyguardManager) this.mContext.getSystemService(KeyguardManager.class);
        if (keyguardmanager != null) {
            boolean secure = keyguardmanager.isDeviceSecure(userHandle);
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onKeyguardStateChanged: isShowing:");
                stringBuilder.append(isShowing);
                stringBuilder.append(" secure:");
                stringBuilder.append(secure);
                stringBuilder.append(" user:");
                stringBuilder.append(userHandle);
                Slog.v(str, stringBuilder.toString());
            }
            UsbHandler usbHandler = this.mHandler;
            boolean z = isShowing && secure;
            usbHandler.sendMessage(13, z);
            return;
        }
        Slog.i(TAG, "KeyguradManager service is null");
    }

    public void onAwakeStateChanged(boolean isAwake) {
    }

    public void onUnlockUser(int userHandle) {
        onKeyguardStateChanged(false);
    }

    public UsbDeviceManager(Context context, UsbAlsaManager alsaManager, UsbSettingsManager settingsManager) {
        Context context2 = context;
        this.mContext = context2;
        this.mContentResolver = context.getContentResolver();
        this.mHasUsbAccessory = this.mContext.getPackageManager().hasSystemFeature("android.hardware.usb.accessory");
        initRndisAddress();
        boolean halNotPresent = false;
        try {
            IUsbGadget.getService(true);
        } catch (RemoteException e) {
            RemoteException remoteException = e;
            Slog.e(TAG, "USB GADGET HAL present but exception thrown", e);
        } catch (NoSuchElementException e2) {
            NoSuchElementException noSuchElementException = e2;
            halNotPresent = true;
            Slog.i(TAG, "USB GADGET HAL not present in the device", e2);
        }
        boolean halNotPresent2 = halNotPresent;
        this.mControlFds = new HashMap();
        FileDescriptor mtpFd = nativeOpenControl("mtp");
        if (mtpFd == null) {
            Slog.e(TAG, "Failed to open control for mtp");
        }
        this.mControlFds.put(Long.valueOf(4), mtpFd);
        FileDescriptor ptpFd = nativeOpenControl("ptp");
        if (mtpFd == null) {
            Slog.e(TAG, "Failed to open control for mtp");
        }
        this.mControlFds.put(Long.valueOf(16), ptpFd);
        boolean secureAdbEnabled = SystemProperties.getBoolean("ro.adb.secure", false);
        boolean dataEncrypted = "1".equals(SystemProperties.get("vold.decrypt"));
        if (secureAdbEnabled && !dataEncrypted) {
            this.mDebuggingManager = new UsbDebuggingManager(context2);
        }
        if (halNotPresent2) {
            UsbHandler usbHandler = r1;
            UsbHandler usbHandlerLegacy = new UsbHandlerLegacy(FgThread.get().getLooper(), this.mContext, this, this.mDebuggingManager, alsaManager, settingsManager);
            this.mHandler = usbHandler;
        } else {
            this.mHandler = new UsbHandlerHal(FgThread.get().getLooper(), this.mContext, this, this.mDebuggingManager, alsaManager, settingsManager);
        }
        mHdbEnabled = SystemProperties.get("persist.service.hdb.enable", "false").equals("true");
        if (nativeIsStartRequested()) {
            if (DEBUG) {
                Slog.d(TAG, "accessory attached at boot");
            }
            startAccessoryMode();
        }
        BroadcastReceiver portReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                UsbDeviceManager.this.mHandler.updateHostState((UsbPort) intent.getParcelableExtra("port"), (UsbPortStatus) intent.getParcelableExtra("portStatus"));
            }
        };
        BroadcastReceiver chargingReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                UsbDeviceManager.this.mHandler.sendMessage(9, intent.getIntExtra("plugged", -1) == 2);
            }
        };
        BroadcastReceiver hostReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                UsbManager usbManager = (UsbManager) context.getSystemService("usb");
                if (usbManager == null) {
                    Slog.e(UsbDeviceManager.TAG, "usbManager is null, return!!");
                    return;
                }
                Object devices = usbManager.getDeviceList().entrySet().iterator();
                if (intent.getAction().equals("android.hardware.usb.action.USB_DEVICE_ATTACHED")) {
                    UsbDeviceManager.this.mHandler.sendMessage(10, devices, true);
                } else {
                    UsbDeviceManager.this.mHandler.sendMessage(10, devices, false);
                }
            }
        };
        BroadcastReceiver languageChangedReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                UsbDeviceManager.this.mHandler.sendEmptyMessage(11);
            }
        };
        this.mContext.registerReceiver(portReceiver, new IntentFilter("android.hardware.usb.action.USB_PORT_CHANGED"));
        this.mContext.registerReceiver(chargingReceiver, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        IntentFilter filter = new IntentFilter("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        filter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
        this.mContext.registerReceiver(hostReceiver, filter);
        this.mContext.registerReceiver(languageChangedReceiver, new IntentFilter("android.intent.action.LOCALE_CHANGED"));
        this.mUEventObserver = new UsbUEventObserver(this, null);
        this.mUEventObserver.startObserving(USB_STATE_MATCH);
        this.mUEventObserver.startObserving(ACCESSORY_START_MATCH);
        this.mContentResolver.registerContentObserver(Global.getUriFor("adb_enabled"), false, new AdbSettingsObserver());
    }

    UsbProfileGroupSettingsManager getCurrentSettings() {
        UsbProfileGroupSettingsManager usbProfileGroupSettingsManager;
        synchronized (this.mLock) {
            usbProfileGroupSettingsManager = this.mCurrentSettings;
        }
        return usbProfileGroupSettingsManager;
    }

    String[] getAccessoryStrings() {
        String[] strArr;
        synchronized (this.mLock) {
            strArr = this.mAccessoryStrings;
        }
        return strArr;
    }

    public void systemReady() {
        StringBuilder stringBuilder;
        if (DEBUG) {
            Slog.d(TAG, "systemReady");
        }
        ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).registerScreenObserver(this);
        if (SystemProperties.get("persist.service.hdb.enable", "false").equals("true") && System.getInt(this.mContentResolver, "hdb_enabled", -1) < 0) {
            StringBuilder stringBuilder2;
            if (this.mHandler.containsFunction(SystemProperties.get("ro.default.userportmode", "null"), "hdb")) {
                try {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(TAG);
                    stringBuilder2.append(" ro.default.userportmode:");
                    stringBuilder2.append(SystemProperties.get("ro.default.userportmode", "null"));
                    Flog.i(1306, stringBuilder2.toString());
                    System.putInt(this.mContentResolver, "hdb_enabled", 1);
                } catch (Exception e) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(TAG);
                    stringBuilder.append(" set KEY_CONTENT_HDB_ALLOWED failed: ");
                    stringBuilder.append(e);
                    Flog.e(1306, stringBuilder.toString());
                }
            } else if (SystemProperties.get("ro.product.locale.region", "null").equals("CN")) {
                try {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(TAG);
                    stringBuilder2.append(" ro.product.locale.region:");
                    stringBuilder2.append(SystemProperties.get("ro.product.locale.region", "null"));
                    Flog.i(1306, stringBuilder2.toString());
                    System.putInt(this.mContentResolver, "hdb_enabled", 1);
                } catch (Exception e2) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(TAG);
                    stringBuilder.append(" set KEY_CONTENT_HDB_ALLOWED failed: ");
                    stringBuilder.append(e2);
                    Flog.w(1306, stringBuilder.toString());
                }
            } else {
                try {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(TAG);
                    stringBuilder2.append(" System.KEY_CONTENT_HDB_ALLOWED : 0");
                    Flog.i(1306, stringBuilder2.toString());
                    System.putInt(this.mContentResolver, "hdb_enabled", 0);
                } catch (Exception e22) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(TAG);
                    stringBuilder.append(" set KEY_CONTENT_HDB_ALLOWED failed: ");
                    stringBuilder.append(e22);
                    Flog.w(1306, stringBuilder.toString());
                }
            }
        }
        Slog.i(TAG, "send message for ready to delay 1 second");
        this.mHandler.sendEmptyMessageDelayed(3, 1000);
    }

    public void bootCompleted() {
        if (DEBUG) {
            Slog.d(TAG, "boot completed");
        }
        this.mHandler.sendEmptyMessage(4);
    }

    public void setCurrentUser(int newCurrentUserId, UsbProfileGroupSettingsManager settings) {
        synchronized (this.mLock) {
            this.mCurrentSettings = settings;
            this.mHandler.obtainMessage(5, newCurrentUserId, 0).sendToTarget();
        }
    }

    public void updateUserRestrictions() {
        this.mHandler.sendEmptyMessage(6);
    }

    private void startAccessoryMode() {
        if (this.mHasUsbAccessory) {
            this.mAccessoryStrings = nativeGetAccessoryStrings();
            boolean enableAccessory = false;
            boolean enableAudio = nativeGetAudioMode() == 1;
            if (!(this.mAccessoryStrings == null || this.mAccessoryStrings[0] == null || this.mAccessoryStrings[1] == null)) {
                enableAccessory = true;
            }
            long functions = 0;
            if (enableAccessory) {
                functions = 0 | 2;
            }
            if (enableAudio) {
                functions |= 64;
            }
            if (functions != 0) {
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(8), JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                setCurrentFunctions(functions);
            }
        }
    }

    private static void initRndisAddress() {
        int[] address = new int[6];
        address[0] = 2;
        String serial = SystemProperties.get("ro.serialno", "1234567890ABCDEF");
        int serialLength = serial.length();
        for (int i = 0; i < serialLength; i++) {
            int i2 = (i % 5) + 1;
            address[i2] = address[i2] ^ serial.charAt(i);
        }
        try {
            FileUtils.stringToFile(RNDIS_ETH_ADDR_PATH, String.format(Locale.US, "%02X:%02X:%02X:%02X:%02X:%02X", new Object[]{Integer.valueOf(address[0]), Integer.valueOf(address[1]), Integer.valueOf(address[2]), Integer.valueOf(address[3]), Integer.valueOf(address[4]), Integer.valueOf(address[5])}));
        } catch (IOException e) {
            Slog.e(TAG, "failed to write to /sys/class/android_usb/android0/f_rndis/ethaddr");
        }
    }

    private static boolean isDefaultFunction(long functions) {
        if (functions == 0) {
            return false;
        }
        String func = UsbManager.usbFunctionsToString(functions);
        if (func == null) {
            return false;
        }
        if (func.equals("hisuite,mtp,mass_storage") || func.equals("mtp")) {
            return true;
        }
        return false;
    }

    public static void writeSuitestate() {
        OutputStreamWriter osw = null;
        FileOutputStream fos = null;
        try {
            File newfile = new File(SUITE_STATE_PATH, SUITE_STATE_FILE);
            if (newfile.exists()) {
                fos = new FileOutputStream(newfile);
                osw = new OutputStreamWriter(fos, "UTF-8");
                osw.write("0");
                osw.flush();
            }
            if (osw != null) {
                try {
                    osw.close();
                } catch (IOException e) {
                    Slog.e(TAG, "IOException in close fw");
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e2) {
                    Slog.e(TAG, "IOException in close fos");
                }
            }
        } catch (IOException ex) {
            Slog.e(TAG, "IOException in writeCommand hisuite", ex);
            if (osw != null) {
                try {
                    osw.close();
                } catch (IOException e3) {
                    Slog.e(TAG, "IOException in close fw");
                }
            }
            if (fos != null) {
                fos.close();
            }
        } catch (Throwable th) {
            if (osw != null) {
                try {
                    osw.close();
                } catch (IOException e4) {
                    Slog.e(TAG, "IOException in close fw");
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e5) {
                    Slog.e(TAG, "IOException in close fos");
                }
            }
        }
    }

    public UsbAccessory getCurrentAccessory() {
        return this.mHandler.getCurrentAccessory();
    }

    public ParcelFileDescriptor openAccessory(UsbAccessory accessory, UsbUserSettingsManager settings) {
        UsbAccessory currentAccessory = this.mHandler.getCurrentAccessory();
        if (currentAccessory == null) {
            throw new IllegalArgumentException("no accessory attached");
        } else if (currentAccessory.equals(accessory)) {
            settings.checkPermission(accessory);
            return nativeOpenAccessory();
        } else {
            String error = new StringBuilder();
            error.append(accessory.toString());
            error.append(" does not match current accessory ");
            error.append(currentAccessory);
            throw new IllegalArgumentException(error.toString());
        }
    }

    public long getCurrentFunctions() {
        return this.mHandler.getEnabledFunctions();
    }

    public ParcelFileDescriptor getControlFd(long usbFunction) {
        FileDescriptor fd = (FileDescriptor) this.mControlFds.get(Long.valueOf(usbFunction));
        if (fd == null) {
            return null;
        }
        try {
            return ParcelFileDescriptor.dup(fd);
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not dup fd for ");
            stringBuilder.append(usbFunction);
            Slog.e(str, stringBuilder.toString());
            return null;
        }
    }

    public long getScreenUnlockedFunctions() {
        return this.mHandler.getScreenUnlockedFunctions();
    }

    public void setCurrentFunctions(long functions) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setCurrentFunctions(");
            stringBuilder.append(UsbManager.usbFunctionsToString(functions));
            stringBuilder.append(")");
            Slog.d(str, stringBuilder.toString());
        }
        if (functions == 0) {
            MetricsLogger.action(this.mContext, 1275);
        } else if (functions == 4) {
            MetricsLogger.action(this.mContext, 1276);
        } else if (functions == 16) {
            MetricsLogger.action(this.mContext, 1277);
        } else if (functions == 8) {
            MetricsLogger.action(this.mContext, 1279);
        } else if (functions == 32) {
            MetricsLogger.action(this.mContext, 1278);
        } else if (functions == 2) {
            MetricsLogger.action(this.mContext, 1280);
        }
        this.mHandler.sendMessage(2, Long.valueOf(functions));
    }

    public void setScreenUnlockedFunctions(long functions) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setScreenUnlockedFunctions(");
            stringBuilder.append(UsbManager.usbFunctionsToString(functions));
            stringBuilder.append(")");
            Slog.d(str, stringBuilder.toString());
        }
        this.mHandler.sendMessage(12, Long.valueOf(functions));
    }

    public void allowUsbDebugging(boolean alwaysAllow, String publicKey) {
        if (this.mDebuggingManager != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(TAG);
            stringBuilder.append(" allowUsbDebugging...");
            Flog.i(1306, stringBuilder.toString());
            this.mDebuggingManager.allowUsbDebugging(alwaysAllow, publicKey);
        }
    }

    public void denyUsbDebugging() {
        if (this.mDebuggingManager != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(TAG);
            stringBuilder.append(" denyUsbDebugging...");
            Flog.i(1306, stringBuilder.toString());
            this.mDebuggingManager.denyUsbDebugging();
        }
    }

    public void clearUsbDebuggingKeys() {
        if (this.mDebuggingManager != null) {
            this.mDebuggingManager.clearUsbDebuggingKeys();
            return;
        }
        throw new RuntimeException("Cannot clear Usb Debugging keys, UsbDebuggingManager not enabled");
    }

    public void dump(DualDumpOutputStream dump, String idName, long id) {
        long token = dump.start(idName, id);
        if (this.mHandler != null) {
            this.mHandler.dump(dump, "handler", 1146756268033L);
        }
        if (this.mDebuggingManager != null) {
            this.mDebuggingManager.dump(dump, "debugging_manager", 1146756268034L);
        }
        dump.end(token);
    }

    protected boolean getUsbHandlerConnected() {
        if (this.mHandler != null) {
            return this.mHandler.mConnected;
        }
        return false;
    }

    protected boolean sendHandlerEmptyMessage(int what) {
        if (this.mHandler != null) {
            return this.mHandler.sendEmptyMessage(what);
        }
        return false;
    }

    protected Context getContext() {
        return this.mContext;
    }

    protected boolean containsFunctionOuter(String functions, String function) {
        if (this.mHandler != null) {
            return this.mHandler.containsFunction(functions, function);
        }
        return false;
    }

    protected void setEnabledFunctionsEx(String functions, boolean forceRestart) {
        if (this.mHandler != null) {
            this.mHandler.setEnabledFunctions(UsbManager.usbFunctionsFromString(functions), forceRestart, false);
        }
    }

    protected String removeAdbFunction(String functions, String function) {
        if (this.mHandler != null) {
            return this.mHandler.removeFunction(functions, function);
        }
        return functions;
    }

    protected boolean setUsbConfigEx(String config) {
        if (this.mHandler == null) {
            return false;
        }
        this.mHandler.setUsbConfig(config);
        return this.mHandler.waitForState(config);
    }
}
