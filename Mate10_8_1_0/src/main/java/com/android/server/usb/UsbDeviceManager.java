package com.android.server.usb;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.Notification.BigTextStyle;
import android.app.Notification.Builder;
import android.app.Notification.TvExtender;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbConfiguration;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;
import android.net.Uri;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
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
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.FgThread;
import com.android.server.job.controllers.JobStatus;
import com.android.server.utils.LogBufferUtil;
import java.io.File;
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
import java.util.Scanner;
import java.util.Set;

public class UsbDeviceManager extends AbsUsbDeviceManager {
    private static final int ACCESSORY_REQUEST_TIMEOUT = 10000;
    private static final String ACCESSORY_START_MATCH = "DEVPATH=/devices/virtual/misc/usb_accessory";
    private static final String ACTION_USB_USER_UPDATE = "android.hardware.usb.action.USB_UPDATE";
    private static final String ADB_NOTIFICATION_CHANNEL_ID_TV = "usbdevicemanager.adb.tv";
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
    private static final int MSG_LOCALE_CHANGED = 11;
    private static final int MSG_MIRRORLINK_REQUESTED = 103;
    private static final int MSG_SET_CURRENT_FUNCTIONS = 2;
    protected static final int MSG_SIM_COMPLETED = 102;
    private static final int MSG_SYSTEM_READY = 3;
    private static final int MSG_UPDATE_CHARGING_STATE = 9;
    private static final int MSG_UPDATE_HOST_STATE = 10;
    private static final int MSG_UPDATE_PORT_STATE = 7;
    private static final int MSG_UPDATE_STATE = 0;
    private static final int MSG_UPDATE_USER_RESTRICTIONS = 6;
    private static final int MSG_USER_SWITCHED = 5;
    private static final String NORMAL_BOOT = "normal";
    private static final String RNDIS_ETH_ADDR_PATH = "/sys/class/android_usb/android0/f_rndis/ethaddr";
    private static final String STATE_PATH = "/sys/class/android_usb/android0/state";
    private static final String SUITE_STATE_FILE = "android_usb/f_mass_storage/suitestate";
    private static final String SUITE_STATE_PATH = "/sys/class";
    private static final String TAG = "UsbDeviceManager";
    private static final int UPDATE_DELAY = 1000;
    private static final String USBDATA_UNLOCKED = "usbdata_unlocked";
    private static final String USB_CONFIG_PROPERTY = "sys.usb.config";
    protected static final String USB_PERSISTENT_CONFIG_PROPERTY = "persist.sys.usb.config";
    private static final String USB_STATE_MATCH = "DEVPATH=/devices/virtual/android_usb/android0";
    private static final String USB_STATE_PROPERTY = "sys.usb.state";
    private static boolean isUSBLiquidOccur = false;
    private static Set<Integer> sBlackListedInterfaces = new HashSet();
    private String ALLOW_CHARGING_ADB = "allow_charging_adb";
    private String[] mAccessoryStrings;
    private boolean mAdbEnabled;
    private boolean mAudioSourceEnabled;
    private boolean mBootCompleted;
    private Intent mBroadcastedIntent;
    private boolean mChargingOnlySelected = true;
    protected final ContentResolver mContentResolver;
    private final Context mContext;
    @GuardedBy("mLock")
    private UsbProfileGroupSettingsManager mCurrentSettings;
    private UsbDebuggingManager mDebuggingManager;
    private UsbHandler mHandler;
    private final boolean mHasUsbAccessory;
    private boolean mHdbEnabled;
    private boolean mLastChargingState;
    private final Object mLock = new Object();
    private int mMidiCard;
    private int mMidiDevice;
    private boolean mMidiEnabled;
    private NotificationManager mNotificationManager;
    private HashMap<String, HashMap<String, Pair<String, String>>> mOemModeMap;
    private boolean mPendingBootBroadcast;
    private boolean mPowerCharging;
    private final UEventObserver mPowerSupplyObserver = new UEventObserver() {
        public void onUEvent(UEvent event) {
            try {
                if (UsbDeviceManager.NORMAL_BOOT.equals(SystemProperties.get("ro.runmode", UsbDeviceManager.NORMAL_BOOT))) {
                    String state = FileUtils.readTextFile(new File(UsbDeviceManager.CHARGE_WATER_INSTRUSED_TYPE_PATH), 0, null).trim();
                    Slog.i(UsbDeviceManager.TAG, "water_intrused state= " + state);
                    if (!UsbDeviceManager.this.mBootCompleted) {
                        Slog.i(UsbDeviceManager.TAG, "boot not completed, do not send smart-notify broadcast");
                    } else if (state.equals("1")) {
                        UsbDeviceManager.this.sendUSBLiquidBroadcast("1");
                        UsbDeviceManager.isUSBLiquidOccur = true;
                    } else if (UsbDeviceManager.isUSBLiquidOccur) {
                        UsbDeviceManager.this.sendUSBLiquidBroadcast("0");
                        UsbDeviceManager.isUSBLiquidOccur = false;
                    }
                }
            } catch (IOException e) {
                Slog.e(UsbDeviceManager.TAG, "Error reading charge file", e);
            }
        }
    };
    private final UsbSettingsManager mSettingsManager;
    private boolean mSystemReady;
    private final UEventObserver mUEventObserver = new UEventObserver() {
        public void onUEvent(UEvent event) {
            Flog.i(1306, "UsbDeviceManagerUSB UEVENT: " + event.toString());
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
    };
    private boolean mUSBPlugType;
    private final UsbAlsaManager mUsbAlsaManager;
    private boolean mUseUsbNotification;
    private UserManager mUserManager;
    private boolean settingsHdbEnabled;

    private class AdbSettingsObserver extends ContentObserver {
        public AdbSettingsObserver() {
            super(null);
        }

        public void onChange(boolean selfChange) {
            boolean enable = Global.getInt(UsbDeviceManager.this.mContentResolver, "adb_enabled", 0) > 0;
            if (enable && UsbDeviceManager.this.isAdbDisabled()) {
                Flog.i(1306, "UsbDeviceManager Adb is disabled by dpm");
                return;
            }
            Flog.i(1306, "UsbDeviceManager Adb Settings enable:" + enable);
            UsbDeviceManager.this.mHandler.sendMessage(1, enable);
            LogBufferUtil.closeLogBufferAsNeed(UsbDeviceManager.this.mContext);
        }
    }

    private class AllowChargingAdbSettingsObserver extends ContentObserver {
        public AllowChargingAdbSettingsObserver() {
            super(null);
        }

        public void onChange(boolean selfChange) {
            boolean enable = Global.getInt(UsbDeviceManager.this.mContentResolver, UsbDeviceManager.this.ALLOW_CHARGING_ADB, 0) > 0;
            Flog.i(1306, "UsbDeviceManager AllowChargingAdb Settings enable:" + enable);
            UsbDeviceManager.this.mHandler.sendMessage(104, enable);
        }
    }

    private class HdbSettingsObserver extends ContentObserver {
        public HdbSettingsObserver() {
            super(null);
        }

        public void onChange(boolean selfChange, Uri uri, int userId) {
            boolean enable = System.getIntForUser(UsbDeviceManager.this.mContentResolver, "hdb_enabled", 0, userId) > 0;
            Flog.i(1306, "UsbDeviceManager Hdb Settings enable:" + enable);
            UsbDeviceManager.this.mHandler.sendMessage(101, enable);
        }
    }

    private class SuitestateObserver extends ContentObserver {
        public SuitestateObserver() {
            super(null);
        }

        public void onChange(boolean selfChange) {
            UsbDeviceManager.this.writeSuitestate();
        }
    }

    private final class UsbHandler extends Handler {
        private boolean mAdbNotificationShown;
        private boolean mAudioAccessoryConnected;
        private boolean mAudioAccessorySupported;
        private boolean mConfigured;
        private boolean mConnected;
        private UsbAccessory mCurrentAccessory;
        private String mCurrentFunctions;
        private boolean mCurrentFunctionsApplied;
        private String mCurrentOemFunctions;
        private int mCurrentUser = -10000;
        private boolean mHideUsbNotification;
        private boolean mHostConnected;
        private boolean mMirrorlinkRequested;
        private boolean mSinkPower;
        private boolean mSourcePower;
        private boolean mSupportsAllCombinations;
        private boolean mUsbCharging;
        private boolean mUsbDataUnlocked;
        private int mUsbNotificationId;

        public UsbHandler(Looper looper) {
            super(looper);
            try {
                if (isNormalBoot()) {
                    this.mCurrentFunctions = SystemProperties.get(UsbDeviceManager.USB_CONFIG_PROPERTY, "none");
                    this.mCurrentFunctionsApplied = this.mCurrentFunctions.equals(SystemProperties.get(UsbDeviceManager.USB_STATE_PROPERTY));
                } else {
                    this.mCurrentFunctions = SystemProperties.get(UsbDeviceManager.getPersistProp(true), "none");
                    this.mCurrentFunctionsApplied = SystemProperties.get(UsbDeviceManager.USB_CONFIG_PROPERTY, "none").equals(SystemProperties.get(UsbDeviceManager.USB_STATE_PROPERTY));
                }
                Flog.i(1306, "UsbDeviceManager mCurrentFunctions:" + this.mCurrentFunctions + ",mCurrentFunctionsApplied:" + this.mCurrentFunctionsApplied + ",persistProp:" + SystemProperties.get(UsbDeviceManager.USB_PERSISTENT_CONFIG_PROPERTY));
                if (System.getInt(UsbDeviceManager.this.mContentResolver, "hdb_enabled", 0) > 0) {
                    UsbDeviceManager.this.settingsHdbEnabled = true;
                }
                UsbDeviceManager.this.mAdbEnabled = UsbManager.containsFunction(SystemProperties.get(UsbDeviceManager.USB_PERSISTENT_CONFIG_PROPERTY), "adb");
                updateState(FileUtils.readTextFile(new File(UsbDeviceManager.STATE_PATH), 0, null).trim());
                UsbDeviceManager.this.mContentResolver.registerContentObserver(Global.getUriFor("adb_enabled"), false, new AdbSettingsObserver());
                UsbDeviceManager.this.mContentResolver.registerContentObserver(Global.getUriFor(UsbDeviceManager.this.ALLOW_CHARGING_ADB), false, new AllowChargingAdbSettingsObserver());
                if (SystemProperties.get("persist.service.hdb.enable", "false").equals("true")) {
                    UsbDeviceManager.this.mContentResolver.registerContentObserver(System.getUriFor("hdb_enabled"), false, new HdbSettingsObserver());
                }
                UsbDeviceManager.this.mContentResolver.registerContentObserver(Secure.getUriFor("suitestate"), false, new SuitestateObserver());
                UsbDeviceManager.this.mUEventObserver.startObserving(UsbDeviceManager.USB_STATE_MATCH);
                UsbDeviceManager.this.mUEventObserver.startObserving(UsbDeviceManager.ACCESSORY_START_MATCH);
                if (new File(UsbDeviceManager.CHARGE_WATER_INSTRUSED_TYPE_PATH).exists()) {
                    UsbDeviceManager.this.mPowerSupplyObserver.startObserving("SUBSYSTEM=power_supply");
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
            m.arg1 = arg ? 1 : 0;
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
            m.arg1 = arg1 ? 1 : 0;
            sendMessage(m);
        }

        public void updateState(String state) {
            int connected;
            int configured;
            int i = 0;
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
                Slog.e(UsbDeviceManager.TAG, "unknown state " + state);
                return;
            }
            removeMessages(0);
            Message msg = Message.obtain(this, 0);
            msg.arg1 = connected;
            msg.arg2 = configured;
            if (connected == 0) {
                i = 1000;
            }
            sendMessageDelayed(msg, (long) i);
        }

        public void updateHostState(UsbPort port, UsbPortStatus status) {
            if (UsbDeviceManager.DEBUG) {
                Slog.i(UsbDeviceManager.TAG, "updateHostState " + port + " status=" + status);
            }
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = port;
            args.arg2 = status;
            removeMessages(7);
            sendMessageDelayed(obtainMessage(7, args), 1000);
        }

        private boolean waitForState(String state) {
            String value = null;
            for (int i = 0; i < 40; i++) {
                value = SystemProperties.get(UsbDeviceManager.USB_STATE_PROPERTY);
                if (state.equals(value)) {
                    return true;
                }
                SystemClock.sleep(50);
            }
            Flog.e(1306, "UsbDeviceManager waitForState(" + state + ") FAILED: got " + value);
            return false;
        }

        private void setUsbConfig(String config) {
            Flog.i(1306, "UsbDeviceManager setUsbConfig(" + config + ")");
            SystemProperties.set(UsbDeviceManager.USB_CONFIG_PROPERTY, config);
        }

        private void setAdbEnabled(boolean enable) {
            if (UsbDeviceManager.DEBUG) {
                Slog.d(UsbDeviceManager.TAG, "setAdbEnabled: " + enable);
            }
            if (enable != UsbDeviceManager.this.mAdbEnabled) {
                UsbDeviceManager.this.mAdbEnabled = enable;
                String oldFunctions = this.mCurrentFunctions;
                boolean isRepairMode = UsbDeviceManager.this.getUserManager().getUserInfo(ActivityManager.getCurrentUser()).isRepairMode();
                if (enable && isChargingOnly_N() && (isRepairMode ^ 1) != 0) {
                    UsbDeviceManager.this.mAdbEnabled = false;
                    return;
                }
                String newFunction = applyHdbFunction(applyAdbFunction(SystemProperties.get(UsbDeviceManager.USB_PERSISTENT_CONFIG_PROPERTY, "none")));
                Flog.i(1306, "UsbDeviceManager setAdbEnabled -> USB_PERSISTENT_CONFIG_PROPERTY : " + newFunction);
                SystemProperties.set(UsbDeviceManager.USB_PERSISTENT_CONFIG_PROPERTY, newFunction);
                if (oldFunctions.equals("mtp") && (this.mUsbDataUnlocked ^ 1) != 0 && enable) {
                    oldFunctions = "none";
                }
                setEnabledFunctions(oldFunctions, true, this.mUsbDataUnlocked);
                updateAdbNotification(false);
            }
            if (UsbDeviceManager.this.mDebuggingManager != null) {
                UsbDeviceManager.this.mDebuggingManager.setAdbEnabled(UsbDeviceManager.this.mAdbEnabled);
            }
        }

        private void setHdbEnabled(boolean enable) {
            if (UsbDeviceManager.DEBUG) {
                Slog.d(UsbDeviceManager.TAG, "setHdbEnabled: " + enable);
            }
            if (enable != UsbDeviceManager.this.settingsHdbEnabled) {
                UsbDeviceManager.this.settingsHdbEnabled = enable;
                String oldFunctions = this.mCurrentFunctions;
                String defaultFunctions = SystemProperties.get(UsbDeviceManager.USB_PERSISTENT_CONFIG_PROPERTY, "none");
                String newFunctions = applyHdbFunction(defaultFunctions);
                if (!defaultFunctions.equals(newFunctions)) {
                    Flog.i(1306, "UsbDeviceManager setHdbEnabled -> USB_PERSISTENT_CONFIG_PROPERTY : " + newFunctions);
                    SystemProperties.set(UsbDeviceManager.USB_PERSISTENT_CONFIG_PROPERTY, newFunctions);
                }
                if (UsbDeviceManager.this.getUserManager().getUserInfo(ActivityManager.getCurrentUser()).isRepairMode() && UsbDeviceManager.this.settingsHdbEnabled) {
                    oldFunctions = "mass_storage,hdb";
                }
                setEnabledFunctions(oldFunctions, false, this.mUsbDataUnlocked);
            }
            UsbDeviceManager.this.setHdbEnabledEx(UsbDeviceManager.this.settingsHdbEnabled);
        }

        private void setEnabledFunctions(String functions, boolean forceRestart, boolean usbDataUnlocked) {
            setEnabledFunctions(functions, forceRestart, usbDataUnlocked, false);
        }

        private void setEnabledFunctions(String functions, boolean forceRestart, boolean usbDataUnlocked, boolean makeDefault) {
            if (UsbDeviceManager.DEBUG) {
                Slog.d(UsbDeviceManager.TAG, "setEnabledFunctions functions=" + functions + ", " + "forceRestart=" + forceRestart + ", usbDataUnlocked=" + usbDataUnlocked + ", makeDefault=" + makeDefault);
            }
            if (!UsbDeviceManager.this.interceptSetEnabledFunctions(functions)) {
                if (usbDataUnlocked != this.mUsbDataUnlocked) {
                    this.mUsbDataUnlocked = usbDataUnlocked;
                    updateUsbNotification(false);
                    forceRestart = true;
                }
                String oldFunctions = this.mCurrentFunctions;
                boolean oldFunctionsApplied = this.mCurrentFunctionsApplied;
                if (!trySetEnabledFunctions(functions, forceRestart, makeDefault)) {
                    if (oldFunctionsApplied && (oldFunctions.equals(functions) ^ 1) != 0) {
                        Slog.e(UsbDeviceManager.TAG, "Failsafe 1: Restoring previous USB functions.");
                        if (trySetEnabledFunctions(oldFunctions, false)) {
                            return;
                        }
                    }
                    Slog.e(UsbDeviceManager.TAG, "Failsafe 2: Restoring default USB functions.");
                    if (!trySetEnabledFunctions(null, false)) {
                        Slog.e(UsbDeviceManager.TAG, "Failsafe 3: Restoring empty function list (with ADB if enabled).");
                        if (!trySetEnabledFunctions("none", false)) {
                            Slog.e(UsbDeviceManager.TAG, "Unable to set any USB functions!");
                        }
                    }
                }
            }
        }

        private boolean isNormalBoot() {
            String bootMode = SystemProperties.get(UsbDeviceManager.BOOT_MODE_PROPERTY, Shell.NIGHT_MODE_STR_UNKNOWN);
            return !bootMode.equals(UsbDeviceManager.NORMAL_BOOT) ? bootMode.equals(Shell.NIGHT_MODE_STR_UNKNOWN) : true;
        }

        private boolean trySetEnabledFunctions(String functions, boolean forceRestart) {
            return trySetEnabledFunctions(functions, forceRestart, false);
        }

        private boolean trySetEnabledFunctions(String functions, boolean forceRestart, boolean makeDefault) {
            if (functions == null || applyAdbFunction(functions).equals("none")) {
                functions = getDefaultFunctions();
                if (isAllowedAdbHdbApply() || (functions.contains("manufacture") ^ 1) == 0) {
                    functions = applyHdbFunction(applyAdbFunction(functions));
                } else {
                    functions = UsbManager.removeFunction(UsbManager.removeFunction(functions, "adb"), "hdb");
                }
            } else if (!isChargingOnly_N()) {
                functions = applyHdbFunction(applyAdbFunction(functions));
            }
            functions = applyUserRestrictions(functions);
            String oemFunctions = UsbDeviceManager.this.applyOemOverrideFunction(functions);
            if (makeDefault && !getDefaultFunctions().equals(oemFunctions)) {
                forceRestart = true;
            }
            if (!(isNormalBoot() || (this.mCurrentFunctions.equals(functions) ^ 1) == 0)) {
                SystemProperties.set(UsbDeviceManager.getPersistProp(true), functions);
            }
            if ((!functions.equals(oemFunctions) && (this.mCurrentOemFunctions == null || (this.mCurrentOemFunctions.equals(oemFunctions) ^ 1) != 0)) || (this.mCurrentFunctions.equals(functions) ^ 1) != 0 || (this.mCurrentFunctionsApplied ^ 1) != 0 || r10) {
                Slog.i(UsbDeviceManager.TAG, "Setting USB config to " + functions);
                this.mCurrentOemFunctions = oemFunctions;
                this.mCurrentFunctionsApplied = false;
                setUsbConfig("none");
                if (waitForState("none")) {
                    if (makeDefault) {
                        Flog.i(1306, "UsbDeviceManager setDefaultFunctions -> USB_PERSISTENT_CONFIG_PROPERTY : " + oemFunctions);
                        SystemProperties.set(UsbDeviceManager.USB_PERSISTENT_CONFIG_PROPERTY, oemFunctions);
                    } else {
                        setUsbConfig(oemFunctions);
                    }
                    if (UsbDeviceManager.this.mBootCompleted && (UsbManager.containsFunction(functions, "mtp") || UsbManager.containsFunction(functions, "ptp"))) {
                        updateUsbStateBroadcastIfNeeded(true);
                    }
                    if (waitForState(oemFunctions)) {
                        if (isChargingOnly_N()) {
                            updateUsbNotification(false);
                            if (UsbDeviceManager.this.mNotificationManager != null && this.mAdbNotificationShown) {
                                if (UsbDeviceManager.DEBUG) {
                                    Slog.v(UsbDeviceManager.TAG, "ChargingOnly, cancel adb notification");
                                }
                                UsbDeviceManager.this.mNotificationManager.cancelAsUser(null, 26, UserHandle.ALL);
                                this.mAdbNotificationShown = false;
                            }
                        }
                        this.mCurrentFunctions = functions;
                        this.mCurrentFunctionsApplied = true;
                    } else {
                        Slog.e(UsbDeviceManager.TAG, "Failed to switch USB config to " + functions);
                        return false;
                    }
                }
                Slog.e(UsbDeviceManager.TAG, "Failed to kick USB config");
                return false;
            }
            return true;
        }

        private boolean isAllowedAdbHdbApply() {
            return !UsbDeviceManager.this.mChargingOnlySelected || ((Global.getInt(UsbDeviceManager.this.mContentResolver, UsbDeviceManager.this.ALLOW_CHARGING_ADB, 0) == 1) ^ 1) == 0;
        }

        private boolean isChargingOnly_N() {
            if (isAllowedAdbHdbApply() || !UsbDeviceManager.this.mChargingOnlySelected) {
                return false;
            }
            return true;
        }

        private void updateUsbState(boolean enable) {
            if (!enable && isChargingOnly_N()) {
                Global.putInt(UsbDeviceManager.this.mContentResolver, "adb_enabled", 0);
                System.putInt(UsbDeviceManager.this.mContentResolver, "hdb_enabled", 0);
                setEnabledFunctions(null, false, false);
            }
        }

        private String applyAdbFunction(String functions) {
            if (functions == null) {
                functions = "";
            }
            if (UsbDeviceManager.this.mAdbEnabled) {
                return UsbManager.addFunction(functions, "adb");
            }
            return UsbDeviceManager.this.removeAdbFunction(functions, "adb");
        }

        private String applyHdbFunction(String functions) {
            if (UsbManager.containsFunction(functions, "hdb")) {
                functions = UsbManager.removeFunction(functions, "hdb");
            }
            if (!UsbDeviceManager.this.HdbIsEnableFunction(functions)) {
                return functions;
            }
            if (UsbDeviceManager.this.mHdbEnabled) {
                if (!UsbManager.containsFunction(functions, "hdb")) {
                    functions = UsbManager.addFunction(functions, "hdb");
                }
                Slog.i(UsbDeviceManager.TAG, "add hdb is " + functions);
                return functions;
            }
            functions = UsbManager.removeFunction(functions, "hdb");
            Slog.i(UsbDeviceManager.TAG, "remove hdb is " + functions);
            return functions;
        }

        private String applyUserRestrictions(String functions) {
            if (!((UserManager) UsbDeviceManager.this.mContext.getSystemService("user")).hasUserRestriction("no_usb_file_transfer")) {
                return functions;
            }
            functions = UsbManager.removeFunction(UsbManager.removeFunction(UsbManager.removeFunction(UsbManager.removeFunction(UsbManager.removeFunction(functions, "mtp"), "ptp"), "mass_storage"), "hisuite"), "hdb");
            if ("none".equals(functions)) {
                return "mtp";
            }
            return functions;
        }

        private boolean isUsbTransferAllowed() {
            return ((UserManager) UsbDeviceManager.this.mContext.getSystemService("user")).hasUserRestriction("no_usb_file_transfer") ^ 1;
        }

        private void updateCurrentAccessory() {
            boolean enteringAccessoryMode = hasMessages(8);
            if (this.mConfigured && enteringAccessoryMode) {
                if (UsbDeviceManager.this.mAccessoryStrings != null) {
                    this.mCurrentAccessory = new UsbAccessory(UsbDeviceManager.this.mAccessoryStrings);
                    Slog.d(UsbDeviceManager.TAG, "entering USB accessory mode: " + this.mCurrentAccessory);
                    if (UsbDeviceManager.this.mBootCompleted) {
                        UsbDeviceManager.this.getCurrentSettings().accessoryAttached(this.mCurrentAccessory);
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
            setEnabledFunctions(null, false, false);
            if (this.mCurrentAccessory != null) {
                if (UsbDeviceManager.this.mBootCompleted) {
                    UsbDeviceManager.this.mSettingsManager.usbAccessoryRemoved(this.mCurrentAccessory);
                }
                this.mCurrentAccessory = null;
                UsbDeviceManager.this.mAccessoryStrings = null;
            }
        }

        private boolean isUsbStateChanged(Intent intent) {
            Set<String> keySet = intent.getExtras().keySet();
            if (UsbDeviceManager.this.mBroadcastedIntent == null) {
                for (String key : keySet) {
                    if (intent.getBooleanExtra(key, false)) {
                        return true;
                    }
                }
            } else if (!keySet.equals(UsbDeviceManager.this.mBroadcastedIntent.getExtras().keySet())) {
                return true;
            } else {
                for (String key2 : keySet) {
                    if (intent.getBooleanExtra(key2, false) != UsbDeviceManager.this.mBroadcastedIntent.getBooleanExtra(key2, false)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private void updateUsbStateBroadcastIfNeeded(boolean configChanged) {
            Intent intent = new Intent("android.hardware.usb.action.USB_STATE");
            intent.addFlags(822083584);
            intent.putExtra("connected", this.mConnected);
            intent.putExtra("host_connected", this.mHostConnected);
            intent.putExtra("configured", this.mConfigured);
            intent.putExtra("unlocked", isUsbTransferAllowed() ? this.mUsbDataUnlocked : false);
            intent.putExtra("config_changed", configChanged);
            intent.putExtra("only_charging", UsbDeviceManager.this.mPowerCharging);
            intent.putExtra("ncm_requested", this.mMirrorlinkRequested);
            if (this.mCurrentFunctions != null) {
                String[] functions = this.mCurrentFunctions.split(",");
                for (String function : functions) {
                    if (!"none".equals(function)) {
                        intent.putExtra(function, true);
                    }
                }
            }
            if (isUsbStateChanged(intent) || (configChanged ^ 1) == 0) {
                Flog.i(1306, "UsbDeviceManagerbroadcasting " + intent + " extras: " + intent.getExtras());
                UsbDeviceManager.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
                UsbDeviceManager.this.mBroadcastedIntent = intent;
                return;
            }
            Flog.i(1306, "UsbDeviceManagerskip broadcasting " + intent + " extras: " + intent.getExtras());
        }

        private void updateUserBroadcast() {
            UsbDeviceManager.this.mContext.sendBroadcastAsUser(new Intent(UsbDeviceManager.ACTION_USB_USER_UPDATE), UserHandle.ALL, "android.permission.MANAGE_USB");
        }

        private void updateUsbFunctions() {
            updateAudioSourceFunction();
            updateMidiFunction();
        }

        private void updateAudioSourceFunction() {
            FileNotFoundException e;
            Throwable th;
            boolean enabled = UsbManager.containsFunction(this.mCurrentFunctions, "audio_source");
            if (enabled != UsbDeviceManager.this.mAudioSourceEnabled) {
                int card = -1;
                int device = -1;
                if (enabled) {
                    Scanner scanner = null;
                    try {
                        Scanner scanner2 = new Scanner(new File(UsbDeviceManager.AUDIO_SOURCE_PCM_PATH));
                        try {
                            card = scanner2.nextInt();
                            device = scanner2.nextInt();
                            if (scanner2 != null) {
                                scanner2.close();
                            }
                        } catch (FileNotFoundException e2) {
                            e = e2;
                            scanner = scanner2;
                            try {
                                Slog.e(UsbDeviceManager.TAG, "could not open audio source PCM file", e);
                                if (scanner != null) {
                                    scanner.close();
                                }
                                UsbDeviceManager.this.mUsbAlsaManager.setAccessoryAudioState(enabled, card, device);
                                UsbDeviceManager.this.mAudioSourceEnabled = enabled;
                            } catch (Throwable th2) {
                                th = th2;
                                if (scanner != null) {
                                    scanner.close();
                                }
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            scanner = scanner2;
                            if (scanner != null) {
                                scanner.close();
                            }
                            throw th;
                        }
                    } catch (FileNotFoundException e3) {
                        e = e3;
                        Slog.e(UsbDeviceManager.TAG, "could not open audio source PCM file", e);
                        if (scanner != null) {
                            scanner.close();
                        }
                        UsbDeviceManager.this.mUsbAlsaManager.setAccessoryAudioState(enabled, card, device);
                        UsbDeviceManager.this.mAudioSourceEnabled = enabled;
                    }
                }
                UsbDeviceManager.this.mUsbAlsaManager.setAccessoryAudioState(enabled, card, device);
                UsbDeviceManager.this.mAudioSourceEnabled = enabled;
            }
        }

        private void updateMidiFunction() {
            FileNotFoundException e;
            Throwable th;
            boolean enabled = UsbManager.containsFunction(this.mCurrentFunctions, "midi");
            if (enabled != UsbDeviceManager.this.mMidiEnabled) {
                if (enabled) {
                    Scanner scanner = null;
                    try {
                        Scanner scanner2 = new Scanner(new File(UsbDeviceManager.MIDI_ALSA_PATH));
                        try {
                            UsbDeviceManager.this.mMidiCard = scanner2.nextInt();
                            UsbDeviceManager.this.mMidiDevice = scanner2.nextInt();
                            if (scanner2 != null) {
                                scanner2.close();
                            }
                        } catch (FileNotFoundException e2) {
                            e = e2;
                            scanner = scanner2;
                            try {
                                Slog.e(UsbDeviceManager.TAG, "could not open MIDI file", e);
                                enabled = false;
                                if (scanner != null) {
                                    scanner.close();
                                }
                                UsbDeviceManager.this.mMidiEnabled = enabled;
                                UsbDeviceManager.this.mUsbAlsaManager.setPeripheralMidiState(UsbDeviceManager.this.mMidiEnabled ? false : this.mConfigured, UsbDeviceManager.this.mMidiCard, UsbDeviceManager.this.mMidiDevice);
                            } catch (Throwable th2) {
                                th = th2;
                                if (scanner != null) {
                                    scanner.close();
                                }
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            scanner = scanner2;
                            if (scanner != null) {
                                scanner.close();
                            }
                            throw th;
                        }
                    } catch (FileNotFoundException e3) {
                        e = e3;
                        Slog.e(UsbDeviceManager.TAG, "could not open MIDI file", e);
                        enabled = false;
                        if (scanner != null) {
                            scanner.close();
                        }
                        UsbDeviceManager.this.mMidiEnabled = enabled;
                        if (UsbDeviceManager.this.mMidiEnabled) {
                        }
                        UsbDeviceManager.this.mUsbAlsaManager.setPeripheralMidiState(UsbDeviceManager.this.mMidiEnabled ? false : this.mConfigured, UsbDeviceManager.this.mMidiCard, UsbDeviceManager.this.mMidiDevice);
                    }
                }
                UsbDeviceManager.this.mMidiEnabled = enabled;
            }
            if (UsbDeviceManager.this.mMidiEnabled) {
            }
            UsbDeviceManager.this.mUsbAlsaManager.setPeripheralMidiState(UsbDeviceManager.this.mMidiEnabled ? false : this.mConfigured, UsbDeviceManager.this.mMidiCard, UsbDeviceManager.this.mMidiDevice);
        }

        public void handleMessage(Message msg) {
            boolean z;
            switch (msg.what) {
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
                    if (!(isChargingOnly_N() && (this.mConnected ^ 1) == 0)) {
                        updateAdbNotification(false);
                    }
                    if (UsbDeviceManager.this.mBootCompleted) {
                        updateUsbStateBroadcastIfNeeded(false);
                    }
                    if (UsbManager.containsFunction(this.mCurrentFunctions, "accessory")) {
                        updateCurrentAccessory();
                    }
                    if (UsbDeviceManager.this.mBootCompleted) {
                        if (!this.mConnected) {
                            UsbDeviceManager.this.mChargingOnlySelected = true;
                            setEnabledFunctions(null, UsbDeviceManager.this.mAdbEnabled ^ 1, false);
                        }
                        if (UsbDeviceManager.this.mSystemReady) {
                            updateUsbFunctions();
                            return;
                        }
                        return;
                    }
                    UsbDeviceManager.this.mPendingBootBroadcast = true;
                    return;
                case 1:
                    setAdbEnabled(msg.arg1 == 1);
                    return;
                case 2:
                    String functions = msg.obj;
                    Slog.i(UsbDeviceManager.TAG, "Getting setFunction command for " + functions);
                    if (functions != null) {
                        UsbDeviceManager.this.mChargingOnlySelected = false;
                    } else {
                        UsbDeviceManager.this.mChargingOnlySelected = true;
                    }
                    if (!UsbDeviceManager.this.isDefaultFunction(functions) || functions == null) {
                        setEnabledFunctions(functions, false, msg.arg1 == 1);
                        return;
                    }
                    if (msg.arg1 == 1) {
                        z = true;
                    } else {
                        z = false;
                    }
                    setEnabledFunctions(functions, false, z, true);
                    return;
                case 3:
                    updateUsbNotification(false);
                    if (!isChargingOnly_N()) {
                        updateAdbNotification(false);
                    }
                    updateUsbFunctions();
                    UsbDeviceManager.this.mSystemReady = true;
                    return;
                case 4:
                    UsbDeviceManager.this.mBootCompleted = true;
                    if (UsbDeviceManager.this.mPendingBootBroadcast) {
                        updateUsbStateBroadcastIfNeeded(false);
                        UsbDeviceManager.this.mPendingBootBroadcast = false;
                    }
                    if ("factory".equals(SystemProperties.get("ro.runmode", UsbDeviceManager.NORMAL_BOOT)) && this.mCurrentFunctions != null && UsbManager.containsFunction(this.mCurrentFunctions, "accessory")) {
                        Slog.i(UsbDeviceManager.TAG, "Boot complete, skip setting default functions in factory and accessory mode");
                    } else {
                        Slog.i(UsbDeviceManager.TAG, "Boot complete, setting default functions");
                        setEnabledFunctions(null, false, false);
                    }
                    if (this.mCurrentAccessory != null) {
                        UsbDeviceManager.this.getCurrentSettings().accessoryAttached(this.mCurrentAccessory);
                    }
                    if (UsbDeviceManager.this.mDebuggingManager != null) {
                        UsbDeviceManager.this.mDebuggingManager.setAdbEnabled(UsbDeviceManager.this.mAdbEnabled);
                    }
                    UsbDeviceManager.this.setHdbEnabledEx(UsbDeviceManager.this.settingsHdbEnabled);
                    return;
                case 5:
                    if (this.mCurrentUser != msg.arg1) {
                        if (this.mUsbDataUnlocked && isUsbDataTransferActive() && this.mCurrentUser != -10000) {
                            Slog.v(UsbDeviceManager.TAG, "Current user switched to " + msg.arg1 + "; resetting USB host stack for MTP or PTP");
                            setEnabledFunctions(null, true, false);
                        }
                        this.mCurrentUser = msg.arg1;
                        if (UsbDeviceManager.this.mBootCompleted) {
                            updateUserBroadcast();
                            handleRepairModeHdb();
                            return;
                        }
                        return;
                    }
                    return;
                case 6:
                    boolean isUsbTransferAllowed;
                    if (this.mUsbDataUnlocked && isUsbDataTransferActive()) {
                        isUsbTransferAllowed = isUsbTransferAllowed() ^ 1;
                    } else {
                        isUsbTransferAllowed = false;
                    }
                    Slog.i(UsbDeviceManager.TAG, "Updating user restrictions, force restart is " + String.valueOf(isUsbTransferAllowed));
                    setEnabledFunctions(this.mCurrentFunctions, isUsbTransferAllowed, this.mUsbDataUnlocked ? isUsbTransferAllowed ^ 1 : false);
                    return;
                case 7:
                    SomeArgs args = msg.obj;
                    boolean prevHostConnected = this.mHostConnected;
                    UsbPort port = args.arg1;
                    UsbPortStatus status = args.arg2;
                    this.mHostConnected = status.getCurrentDataRole() == 1;
                    this.mSourcePower = status.getCurrentPowerRole() == 1;
                    this.mSinkPower = status.getCurrentPowerRole() == 2;
                    this.mAudioAccessoryConnected = status.getCurrentMode() == 4;
                    this.mAudioAccessorySupported = port.isModeSupported(4);
                    if (status.isRoleCombinationSupported(1, 1) && status.isRoleCombinationSupported(2, 1) && status.isRoleCombinationSupported(1, 2)) {
                        z = status.isRoleCombinationSupported(2, 1);
                    } else {
                        z = false;
                    }
                    this.mSupportsAllCombinations = z;
                    args.recycle();
                    updateUsbNotification(false);
                    if (!UsbDeviceManager.this.mBootCompleted) {
                        UsbDeviceManager.this.mPendingBootBroadcast = true;
                        return;
                    } else if (this.mHostConnected || prevHostConnected) {
                        updateUsbStateBroadcastIfNeeded(false);
                        return;
                    } else {
                        return;
                    }
                case 8:
                    if (UsbDeviceManager.DEBUG) {
                        Slog.v(UsbDeviceManager.TAG, "Accessory mode enter timeout: " + this.mConnected);
                    }
                    if (!this.mConnected || (UsbManager.containsFunction(this.mCurrentFunctions, "accessory") ^ 1) != 0) {
                        notifyAccessoryModeExit();
                        return;
                    }
                    return;
                case 9:
                    this.mUsbCharging = msg.arg1 == 1;
                    updateUsbNotification(false);
                    return;
                case 10:
                    Iterator devices = msg.obj;
                    boolean connected = msg.arg1 == 1;
                    if (UsbDeviceManager.DEBUG) {
                        Slog.i(UsbDeviceManager.TAG, "HOST_STATE connected:" + connected);
                    }
                    this.mHideUsbNotification = false;
                    while (devices.hasNext()) {
                        Entry pair = (Entry) devices.next();
                        if (UsbDeviceManager.DEBUG) {
                            Slog.i(UsbDeviceManager.TAG, pair.getKey() + " = " + pair.getValue());
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
                case 101:
                    setHdbEnabled(msg.arg1 == 1);
                    return;
                case 102:
                    UsbDeviceManager.this.dueSimStatusCompletedMsg();
                    return;
                case 103:
                    this.mMirrorlinkRequested = true;
                    if (UsbDeviceManager.this.mBootCompleted) {
                        updateUsbStateBroadcastIfNeeded(false);
                    }
                    this.mMirrorlinkRequested = false;
                    return;
                case 104:
                    updateUsbState(msg.arg1 == 1);
                    return;
                default:
                    return;
            }
        }

        private boolean isUsbDataTransferActive() {
            if (UsbManager.containsFunction(this.mCurrentFunctions, "mtp")) {
                return true;
            }
            return UsbManager.containsFunction(this.mCurrentFunctions, "ptp");
        }

        public UsbAccessory getCurrentAccessory() {
            return this.mCurrentAccessory;
        }

        private void handleRepairModeHdb() {
            if (this.mCurrentUser == 127) {
                UsbDeviceManager.this.mContentResolver.registerContentObserver(System.getUriFor("hdb_enabled"), false, new HdbSettingsObserver(), 127);
                if (SystemProperties.get("ro.product.locale.region", "null").equals("CN")) {
                    try {
                        System.putIntForUser(UsbDeviceManager.this.mContentResolver, "hdb_enabled", 1, 127);
                    } catch (Exception e) {
                        Flog.w(1306, "UsbDeviceManager set KEY_CONTENT_HDB_ALLOWED failed: " + e);
                    }
                }
            }
        }

        private void updateUsbNotification(boolean force) {
            if (UsbDeviceManager.this.mNotificationManager != null && (UsbDeviceManager.this.mUseUsbNotification ^ 1) == 0 && !"0".equals(SystemProperties.get("persist.charging.notify"))) {
                if (UsbDeviceManager.DEBUG) {
                    Slog.v(UsbDeviceManager.TAG, "update usb notification - mConnetced = " + this.mConnected);
                }
                if (!this.mHideUsbNotification || (this.mSupportsAllCombinations ^ 1) == 0) {
                    Resources r = UsbDeviceManager.this.mContext.getResources();
                    if (!this.mAudioAccessoryConnected || (this.mAudioAccessorySupported ^ 1) == 0) {
                        boolean z;
                        if (this.mConnected) {
                            if (!this.mUsbDataUnlocked) {
                                z = this.mSourcePower;
                            } else if (!(UsbManager.containsFunction(this.mCurrentFunctions, "mtp") || UsbManager.containsFunction(this.mCurrentFunctions, "ptp") || UsbManager.containsFunction(this.mCurrentFunctions, "midi"))) {
                                z = UsbManager.containsFunction(this.mCurrentFunctions, "accessory");
                            }
                        } else if (this.mHostConnected && this.mSinkPower) {
                            z = this.mUsbCharging;
                        }
                    }
                    if ((this.mUsbNotificationId != 0 || force) && this.mUsbNotificationId != 0) {
                        UsbDeviceManager.this.mNotificationManager.cancelAsUser(null, this.mUsbNotificationId, UserHandle.ALL);
                        Slog.d(UsbDeviceManager.TAG, "Clear notification");
                        this.mUsbNotificationId = 0;
                    }
                    return;
                }
                if (this.mUsbNotificationId != 0) {
                    UsbDeviceManager.this.mNotificationManager.cancelAsUser(null, this.mUsbNotificationId, UserHandle.ALL);
                    this.mUsbNotificationId = 0;
                    Slog.d(UsbDeviceManager.TAG, "Clear notification");
                }
            }
        }

        private void updateAdbNotification(boolean force) {
            if (UsbDeviceManager.this.mNotificationManager != null) {
                if (UsbDeviceManager.this.mAdbEnabled && this.mConnected && ("none".equals(SystemProperties.get(UsbDeviceManager.USB_PERSISTENT_CONFIG_PROPERTY)) ^ 1) != 0) {
                    if (!"0".equals(SystemProperties.get("persist.adb.notify"))) {
                        if (force && this.mAdbNotificationShown) {
                            this.mAdbNotificationShown = false;
                            UsbDeviceManager.this.mNotificationManager.cancelAsUser(null, 26, UserHandle.ALL);
                        }
                        if (!this.mAdbNotificationShown) {
                            if (UsbDeviceManager.DEBUG) {
                                Slog.v(UsbDeviceManager.TAG, "update adb notification");
                            }
                            Resources r = UsbDeviceManager.this.mContext.getResources();
                            CharSequence title = r.getText(17039549);
                            CharSequence message = r.getText(33685802);
                            Intent intent = new Intent("android.settings.APPLICATION_DEVELOPMENT_SETTINGS");
                            intent.addFlags(268468224);
                            Notification notification = new Builder(UsbDeviceManager.this.mContext, SystemNotificationChannels.DEVELOPER).setSmallIcon(33751155).setWhen(0).setOngoing(true).setTicker(title).setDefaults(0).setContentTitle(title).setContentText(message).setContentIntent(PendingIntent.getActivityAsUser(UsbDeviceManager.this.mContext, 0, intent, 0, null, UserHandle.CURRENT)).setVisibility(1).extend(new TvExtender().setChannelId(UsbDeviceManager.ADB_NOTIFICATION_CHANNEL_ID_TV)).setVisibility(1).setStyle(new BigTextStyle().bigText(message)).build();
                            this.mAdbNotificationShown = true;
                            UsbDeviceManager.this.mNotificationManager.notifyAsUser(null, 26, notification, UserHandle.ALL);
                        }
                    }
                } else if (this.mAdbNotificationShown) {
                    if (UsbDeviceManager.DEBUG) {
                        Slog.v(UsbDeviceManager.TAG, "cancel adb notification");
                    }
                    this.mAdbNotificationShown = false;
                    UsbDeviceManager.this.mNotificationManager.cancelAsUser(null, 26, UserHandle.ALL);
                }
            }
        }

        private String getDefaultFunctions() {
            String func = SystemProperties.get(UsbDeviceManager.getPersistProp(true), "none");
            if ("none".equals(func)) {
                return "mtp";
            }
            return func;
        }

        public void dump(IndentingPrintWriter pw) {
            pw.println("USB Device State:");
            pw.println("  mCurrentFunctions: " + this.mCurrentFunctions);
            pw.println("  mCurrentOemFunctions: " + this.mCurrentOemFunctions);
            pw.println("  mCurrentFunctionsApplied: " + this.mCurrentFunctionsApplied);
            pw.println("  mConnected: " + this.mConnected);
            pw.println("  mConfigured: " + this.mConfigured);
            pw.println("  mUsbDataUnlocked: " + this.mUsbDataUnlocked);
            pw.println("  mCurrentAccessory: " + this.mCurrentAccessory);
            pw.println("  mHostConnected: " + this.mHostConnected);
            pw.println("  mSourcePower: " + this.mSourcePower);
            pw.println("  mSinkPower: " + this.mSinkPower);
            pw.println("  mUsbCharging: " + this.mUsbCharging);
            pw.println("  mHideUsbNotification: " + this.mHideUsbNotification);
            pw.println("  mAudioAccessoryConnected: " + this.mAudioAccessoryConnected);
            try {
                pw.println("  Kernel state: " + FileUtils.readTextFile(new File(UsbDeviceManager.STATE_PATH), 0, null).trim());
                pw.println("  Kernel function list: " + FileUtils.readTextFile(new File(UsbDeviceManager.FUNCTIONS_PATH), 0, null).trim());
            } catch (IOException e) {
                pw.println("IOException: " + e);
            }
        }
    }

    private native String[] nativeGetAccessoryStrings();

    private native int nativeGetAudioMode();

    private native boolean nativeIsStartRequested();

    private native ParcelFileDescriptor nativeOpenAccessory();

    static {
        boolean z;
        if (Log.HWINFO) {
            z = true;
        } else if (Log.HWModuleLog) {
            z = Log.isLoggable(TAG, 4);
        } else {
            z = false;
        }
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
        sBlackListedInterfaces.add(Integer.valueOf(224));
    }

    public UsbDeviceManager(Context context, UsbAlsaManager alsaManager, UsbSettingsManager settingsManager) {
        this.mContext = context;
        this.mUsbAlsaManager = alsaManager;
        this.mSettingsManager = settingsManager;
        this.mContentResolver = context.getContentResolver();
        this.mHasUsbAccessory = this.mContext.getPackageManager().hasSystemFeature("android.hardware.usb.accessory");
        initRndisAddress();
        readOemUsbOverrideConfig();
        this.mHdbEnabled = SystemProperties.get("persist.service.hdb.enable", "false").equals("true");
        this.mHandler = new UsbHandler(FgThread.get().getLooper());
        if (nativeIsStartRequested()) {
            if (DEBUG) {
                Slog.d(TAG, "accessory attached at boot");
            }
            startAccessoryMode();
        }
        boolean secureAdbEnabled = SystemProperties.getBoolean("ro.adb.secure", false);
        boolean dataEncrypted = "1".equals(SystemProperties.get("vold.decrypt"));
        if (secureAdbEnabled && (dataEncrypted ^ 1) != 0) {
            this.mDebuggingManager = new UsbDebuggingManager(context);
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
                Iterator devices = ((UsbManager) context.getSystemService("usb")).getDeviceList().entrySet().iterator();
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
    }

    private UsbProfileGroupSettingsManager getCurrentSettings() {
        UsbProfileGroupSettingsManager usbProfileGroupSettingsManager;
        synchronized (this.mLock) {
            usbProfileGroupSettingsManager = this.mCurrentSettings;
        }
        return usbProfileGroupSettingsManager;
    }

    public void systemReady() {
        boolean z;
        int i = 1;
        if (DEBUG) {
            Slog.d(TAG, "systemReady");
        }
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        if (isTv()) {
            this.mNotificationManager.createNotificationChannel(new NotificationChannel(ADB_NOTIFICATION_CHANNEL_ID_TV, this.mContext.getString(17039550), 4));
        }
        StorageVolume primary = StorageManager.from(this.mContext).getPrimaryVolume();
        if (primary != null ? primary.allowMassStorage() : false) {
            z = false;
        } else {
            z = this.mContext.getResources().getBoolean(17957048);
        }
        this.mUseUsbNotification = z;
        if (SystemProperties.get("ro.product.custom", "NULL").contains("docomo")) {
            z = true;
        } else {
            z = false;
        }
        Boolean isDocomo = Boolean.valueOf(z);
        if (SystemProperties.getInt("ro.debuggable", 0) == 1) {
            Global.putInt(this.mContentResolver, this.ALLOW_CHARGING_ADB, 1);
            this.mAdbEnabled = true;
        } else if (isDocomo.booleanValue()) {
            Global.putInt(this.mContentResolver, this.ALLOW_CHARGING_ADB, 1);
        }
        try {
            Flog.i(1306, "UsbDeviceManager make sure ADB_ENABLED setting value when systemReady, mAdbEnabled is " + this.mAdbEnabled);
            ContentResolver contentResolver = this.mContentResolver;
            String str = "adb_enabled";
            if (!this.mAdbEnabled) {
                i = 0;
            }
            Global.putInt(contentResolver, str, i);
        } catch (SecurityException e) {
            Flog.w(1306, "UsbDeviceManager ADB_ENABLED is restricted.");
        }
        if (SystemProperties.get("persist.service.hdb.enable", "false").equals("true") && System.getInt(this.mContentResolver, "hdb_enabled", -1) < 0) {
            if (UsbManager.containsFunction(SystemProperties.get("ro.default.userportmode", "null"), "hdb")) {
                try {
                    Flog.i(1306, "UsbDeviceManager ro.default.userportmode:" + SystemProperties.get("ro.default.userportmode", "null"));
                    System.putInt(this.mContentResolver, "hdb_enabled", 1);
                } catch (Exception e2) {
                    Flog.e(1306, "UsbDeviceManager set KEY_CONTENT_HDB_ALLOWED failed: " + e2);
                }
            } else if (SystemProperties.get("ro.product.locale.region", "null").equals("CN")) {
                try {
                    Flog.i(1306, "UsbDeviceManager ro.product.locale.region:" + SystemProperties.get("ro.product.locale.region", "null"));
                    System.putInt(this.mContentResolver, "hdb_enabled", 1);
                } catch (Exception e22) {
                    Flog.w(1306, "UsbDeviceManager set KEY_CONTENT_HDB_ALLOWED failed: " + e22);
                }
            } else {
                try {
                    Flog.i(1306, "UsbDeviceManager System.KEY_CONTENT_HDB_ALLOWED : 0");
                    System.putInt(this.mContentResolver, "hdb_enabled", 0);
                } catch (Exception e222) {
                    Flog.w(1306, "UsbDeviceManager set KEY_CONTENT_HDB_ALLOWED failed: " + e222);
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
            boolean enableAudio = nativeGetAudioMode() == 1;
            boolean enableAccessory = (this.mAccessoryStrings == null || this.mAccessoryStrings[0] == null) ? false : this.mAccessoryStrings[1] != null;
            String functions = null;
            if (enableAccessory && enableAudio) {
                functions = "accessory,audio_source";
            } else if (enableAccessory) {
                functions = "accessory";
            } else if (enableAudio) {
                functions = "audio_source";
            }
            if (functions != null) {
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(8), JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                setCurrentFunctions(functions, false);
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

    private boolean isTv() {
        return this.mContext.getPackageManager().hasSystemFeature("android.software.leanback");
    }

    private boolean HdbIsEnableFunction(String functions) {
        if (isCmccUsbLimit()) {
            Slog.i(TAG, "cmcc_usb_limit do not set hdb");
            return false;
        } else if (getUserManager().getUserInfo(ActivityManager.getCurrentUser()).isRepairMode()) {
            return true;
        } else {
            return (functions.equals("mtp") || functions.equals("mtp,adb") || functions.equals("ptp") || functions.equals("ptp,adb") || functions.equals("hisuite,mtp,mass_storage") || functions.equals("hisuite,mtp,mass_storage,adb") || functions.equals("bicr") || functions.equals("bicr,adb") || functions.equals("rndis") || functions.equals("rndis,adb")) && this.settingsHdbEnabled;
        }
    }

    private boolean isDefaultFunction(String functions) {
        if (functions == null) {
            return false;
        }
        if (functions.equals("hisuite,mtp,mass_storage") || functions.equals("mtp")) {
            return true;
        }
        return false;
    }

    public void writeSuitestate() {
        OutputStreamWriter osw;
        IOException ex;
        Throwable th;
        OutputStreamWriter outputStreamWriter = null;
        FileOutputStream fos = null;
        try {
            File newfile = new File(SUITE_STATE_PATH, SUITE_STATE_FILE);
            if (newfile.exists()) {
                FileOutputStream fos2 = new FileOutputStream(newfile);
                try {
                    osw = new OutputStreamWriter(fos2, "UTF-8");
                } catch (IOException e) {
                    ex = e;
                    fos = fos2;
                    try {
                        Slog.e(TAG, "IOException in writeCommand hisuite", ex);
                        if (outputStreamWriter != null) {
                            try {
                                outputStreamWriter.close();
                            } catch (IOException e2) {
                                Slog.e(TAG, "IOException in close fw");
                            }
                        }
                        if (fos == null) {
                            try {
                                fos.close();
                            } catch (IOException e3) {
                                Slog.e(TAG, "IOException in close fos");
                                return;
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (outputStreamWriter != null) {
                            try {
                                outputStreamWriter.close();
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
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    fos = fos2;
                    if (outputStreamWriter != null) {
                        outputStreamWriter.close();
                    }
                    if (fos != null) {
                        fos.close();
                    }
                    throw th;
                }
                try {
                    osw.write("0");
                    osw.flush();
                    fos = fos2;
                    outputStreamWriter = osw;
                } catch (IOException e6) {
                    ex = e6;
                    fos = fos2;
                    outputStreamWriter = osw;
                    Slog.e(TAG, "IOException in writeCommand hisuite", ex);
                    if (outputStreamWriter != null) {
                        outputStreamWriter.close();
                    }
                    if (fos == null) {
                        fos.close();
                    }
                } catch (Throwable th4) {
                    th = th4;
                    fos = fos2;
                    outputStreamWriter = osw;
                    if (outputStreamWriter != null) {
                        outputStreamWriter.close();
                    }
                    if (fos != null) {
                        fos.close();
                    }
                    throw th;
                }
            }
            if (outputStreamWriter != null) {
                try {
                    outputStreamWriter.close();
                } catch (IOException e7) {
                    Slog.e(TAG, "IOException in close fw");
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e8) {
                    Slog.e(TAG, "IOException in close fos");
                }
            }
        } catch (IOException e9) {
            ex = e9;
            Slog.e(TAG, "IOException in writeCommand hisuite", ex);
            if (outputStreamWriter != null) {
                outputStreamWriter.close();
            }
            if (fos == null) {
                fos.close();
            }
        }
    }

    private UserManager getUserManager() {
        if (this.mUserManager == null) {
            this.mUserManager = UserManager.get(getContext());
        }
        return this.mUserManager;
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
            throw new IllegalArgumentException(accessory.toString() + " does not match current accessory " + currentAccessory);
        }
    }

    public boolean isFunctionEnabled(String function) {
        return UsbManager.containsFunction(SystemProperties.get(USB_CONFIG_PROPERTY), function);
    }

    public void setCurrentFunctions(String functions, boolean usbDataUnlocked) {
        Flog.i(1306, "UsbDeviceManager setCurrentFunctions(" + functions + ")");
        if (DEBUG) {
            Slog.d(TAG, "setCurrentFunctions(" + functions + ", " + usbDataUnlocked + ")");
        }
        this.mHandler.sendMessage(2, functions, usbDataUnlocked);
    }

    private void readOemUsbOverrideConfig() {
        String[] configList = this.mContext.getResources().getStringArray(17236024);
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
                            overrideMap.put(items[1], new Pair(items[2], ""));
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
        String bootMode = SystemProperties.get(BOOT_MODE_PROPERTY, Shell.NIGHT_MODE_STR_UNKNOWN);
        Slog.d(TAG, "applyOemOverride usbfunctions=" + usbFunctions + " bootmode=" + bootMode);
        Map<String, Pair<String, String>> overridesMap = (Map) this.mOemModeMap.get(bootMode);
        if (overridesMap != null) {
            int i;
            if (bootMode.equals(NORMAL_BOOT)) {
                i = 1;
            } else {
                i = bootMode.equals(Shell.NIGHT_MODE_STR_UNKNOWN);
            }
            if ((i ^ 1) != 0) {
                Pair<String, String> overrideFunctions = (Pair) overridesMap.get(usbFunctions);
                if (overrideFunctions != null) {
                    Slog.d(TAG, "OEM USB override: " + usbFunctions + " ==> " + ((String) overrideFunctions.first) + " persist across reboot " + ((String) overrideFunctions.second));
                    if (!((String) overrideFunctions.second).equals("")) {
                        String newFunction;
                        if (this.mAdbEnabled) {
                            newFunction = UsbManager.addFunction((String) overrideFunctions.second, "adb");
                        } else {
                            newFunction = (String) overrideFunctions.second;
                        }
                        Slog.d(TAG, "OEM USB override persisting: " + newFunction + "in prop: " + getPersistProp(false));
                        SystemProperties.set(getPersistProp(false), newFunction);
                    }
                    return (String) overrideFunctions.first;
                } else if (this.mAdbEnabled) {
                    SystemProperties.set(getPersistProp(false), UsbManager.addFunction("none", "adb"));
                } else {
                    SystemProperties.set(getPersistProp(false), "none");
                }
            }
        }
        return usbFunctions;
    }

    public static String getPersistProp(boolean functions) {
        String bootMode = SystemProperties.get(BOOT_MODE_PROPERTY, Shell.NIGHT_MODE_STR_UNKNOWN);
        String persistProp = USB_PERSISTENT_CONFIG_PROPERTY;
        if (!bootMode.equals(NORMAL_BOOT) ? bootMode.equals(Shell.NIGHT_MODE_STR_UNKNOWN) : true) {
            return persistProp;
        }
        if (functions) {
            return "persist.sys.usb." + bootMode + ".func";
        }
        return "persist.sys.usb." + bootMode + ".config";
    }

    public void allowUsbDebugging(boolean alwaysAllow, String publicKey) {
        if (this.mDebuggingManager != null) {
            Flog.i(1306, "UsbDeviceManager allowUsbDebugging...");
            this.mDebuggingManager.allowUsbDebugging(alwaysAllow, publicKey);
        }
    }

    public void denyUsbDebugging() {
        if (this.mDebuggingManager != null) {
            Flog.i(1306, "UsbDeviceManager denyUsbDebugging...");
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

    public void dump(IndentingPrintWriter pw) {
        if (this.mHandler != null) {
            this.mHandler.dump(pw);
        }
        if (this.mDebuggingManager != null) {
            this.mDebuggingManager.dump(pw);
        }
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
        return UsbManager.containsFunction(functions, function);
    }

    protected void setEnabledFunctionsEx(String functions, boolean forceRestart) {
        if (this.mHandler != null) {
            this.mHandler.setEnabledFunctions(functions, forceRestart, false);
        }
    }

    protected String removeAdbFunction(String functions, String function) {
        return UsbManager.removeFunction(functions, function);
    }

    protected boolean setUsbConfigEx(String config) {
        if (this.mHandler == null) {
            return false;
        }
        this.mHandler.setUsbConfig(config);
        return this.mHandler.waitForState(config);
    }
}
