package com.android.server.usb;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.debug.HdbManagerInternal;
import android.hardware.usb.UsbManager;
import android.hdm.HwDeviceManager;
import android.os.FileUtils;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Flog;
import android.util.Log;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.hidata.appqoe.HwAPPQoEUserAction;
import com.android.server.intellicom.common.SmartDualCardConsts;
import java.io.File;
import java.io.IOException;

public class HwUsbDeviceManager extends UsbDeviceManager {
    private static final String ACTION_USBLIQUID = "huawei.intent.action.USB_LIQUID";
    private static final String ALLOW_CHARGING_ADB = "allow_charging_adb";
    private static final String CHARGE_WATER_INSTRUSED_TYPE_PATH = "sys/class/hw_power/power_ui/water_status";
    private static final String CLASS_NAME_USBLIQUID = "com.huawei.hwdetectrepair.smartnotify.eventlistener.USBLiquidReceiver";
    /* access modifiers changed from: private */
    public static boolean DEBUG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final String MSG_USBLIQUID_TYPE = "MSG_USBLIQUID_TYPE";
    private static final String PACKAGE_NAME_USBLIQUID = "com.huawei.hwdetectrepair";
    private static final String PERMISSION_SEND_USB_LIQUID = "huawei.permission.SMART_NOTIFY_FAULT";
    private static final String PERSIST_CMCC_USB_LIMIT = "persist.sys.cmcc_usb_limit";
    private static final String SYS_CMCC_USB_LIMIT = "cmcc_usb_limit";
    /* access modifiers changed from: private */
    public static final String TAG = HwUsbDeviceManager.class.getSimpleName();
    private static final String USB_STATE_PROPERTY = "sys.usb.state";
    /* access modifiers changed from: private */
    public String mEventState;
    /* access modifiers changed from: private */
    public boolean mIsBootCompleted = false;
    private final UEventObserver mPowerSupplyObserver = new UEventObserver() {
        /* class com.android.server.usb.HwUsbDeviceManager.AnonymousClass1 */

        public void onUEvent(UEventObserver.UEvent event) {
            try {
                if ("normal".equals(SystemProperties.get("ro.runmode", "normal"))) {
                    String state = FileUtils.readTextFile(new File(HwUsbDeviceManager.CHARGE_WATER_INSTRUSED_TYPE_PATH), 0, null).trim();
                    String access$000 = HwUsbDeviceManager.TAG;
                    Slog.i(access$000, "water_intrused state= " + state);
                    if (!HwUsbDeviceManager.this.mIsBootCompleted) {
                        Slog.i(HwUsbDeviceManager.TAG, "boot not completed, do not send smart-notify broadcast");
                    } else if (state != null && !state.equals(HwUsbDeviceManager.this.mEventState)) {
                        HwUsbDeviceManager.this.sendUsbLiquidBroadcast(HwUsbDeviceManager.this.mContext, state);
                        String unused = HwUsbDeviceManager.this.mEventState = state;
                    }
                }
            } catch (IOException e) {
                Slog.i(HwUsbDeviceManager.TAG, "Error reading charge file.");
            }
        }
    };
    private final BroadcastReceiver mSimStatusCompletedReceiver = new BroadcastReceiver() {
        /* class com.android.server.usb.HwUsbDeviceManager.AnonymousClass2 */

        public void onReceive(Context context, Intent intent) {
            if (HwUsbDeviceManager.DEBUG) {
                Slog.d(HwUsbDeviceManager.TAG, "sim status completed");
            }
            boolean unused = HwUsbDeviceManager.this.sendHandlerEmptyMessage(102);
        }
    };
    private UserManager mUserManager;

    public HwUsbDeviceManager(Context context, UsbAlsaManager alsaManager, UsbSettingsManager settingsManager) {
        super(context, alsaManager, settingsManager);
        setCmccUsbLimit();
        setUsbConfig();
        registerSimStatusCompletedReceiver();
        if (new File(CHARGE_WATER_INSTRUSED_TYPE_PATH).exists()) {
            this.mPowerSupplyObserver.startObserving("SUBSYSTEM=hw_power");
        } else {
            Slog.d(TAG, "charge file doesnt exist, product doesnt support the 'CHARGE_WATER_INSTRUSED' function.");
        }
    }

    /* access modifiers changed from: protected */
    public void onInitHandler() {
        try {
            this.mContentResolver.registerContentObserver(Settings.Global.getUriFor(ALLOW_CHARGING_ADB), false, new AllowChargingAdbSettingsObserver());
        } catch (IllegalArgumentException | SecurityException e) {
            Slog.i(TAG, "Error initializing UsbHandler!");
        }
    }

    private Context getContext() {
        return this.mContext;
    }

    private String getCmccUsbLimit() {
        return SystemProperties.get(PERSIST_CMCC_USB_LIMIT, "0");
    }

    private String getDebuggleMode() {
        return SystemProperties.get("ro.debuggable", "0");
    }

    private void setCmccUsbLimit() {
        String roDebuggable = getDebuggleMode();
        String usbLimit = getCmccUsbLimit();
        if (DEBUG) {
            String str = TAG;
            Slog.i(str, "roDebuggable " + roDebuggable + " usbLimit " + usbLimit);
        }
        if ("1".equals(roDebuggable) && "1".equals(usbLimit)) {
            SystemProperties.set(PERSIST_CMCC_USB_LIMIT, "0");
            if (DEBUG) {
                Slog.i(TAG, "UsbDeviceManager new init in debug mode set  to 0 !");
            }
        }
    }

    private void setUsbConfig() {
        String curUsbConfig = SystemProperties.get("persist.sys.usb.config", "adb");
        String usbLimit = getCmccUsbLimit();
        if (DEBUG) {
            String str = TAG;
            Slog.i(str, "setUsbConfig curUsbConfig " + curUsbConfig + " usbLimit " + usbLimit);
        }
        if ("1".equals(usbLimit) && !containsFunctionOuter(curUsbConfig, "manufacture")) {
            boolean result = setUsbConfigEx("mass_storage");
            if (DEBUG) {
                String str2 = TAG;
                Slog.i(str2, "UsbDeviceManager new init setusbconfig result: " + result);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void registerSimStatusCompletedReceiver() {
        if (DEBUG) {
            Slog.d(TAG, "registerSimStatusCompletedReceiver");
        }
        if (getContext() != null) {
            getContext().registerReceiver(this.mSimStatusCompletedReceiver, new IntentFilter(SmartDualCardConsts.SYSTEM_STATE_NAME_SIM_STATE_CHANGED));
        }
    }

    /* access modifiers changed from: protected */
    public void handleSimStatusCompleted() {
        String usbLimit = getCmccUsbLimit();
        if (DEBUG) {
            String str = TAG;
            Slog.i(str, "simcardstate at receive sim_status_change usbLimit = " + usbLimit);
        }
        if (!"0".equals(usbLimit)) {
            int simCardState = 0;
            if (getContext() != null) {
                TelephonyManager tm = (TelephonyManager) getContext().getSystemService("phone");
                if (tm == null) {
                    Slog.i(TAG, "TelephonyManager is null, return!");
                    return;
                }
                simCardState = tm.getSimState();
            }
            if (DEBUG) {
                String str2 = TAG;
                Slog.i(str2, "simcardstate at boot completed is " + simCardState);
            }
            if (simCardState != 0 && simCardState != 1 && simCardState != 8 && simCardState != 6) {
                Slog.i(TAG, "persist.sys.cmcc_usb_limit to 0 ");
                SystemProperties.set(PERSIST_CMCC_USB_LIMIT, "0");
                setEnabledFunctionsEx("hisuite,mtp,mass_storage", true);
                if (getContext() != null && getUsbHandlerConnected()) {
                    Slog.i(TAG, "Secure SYS_CMCC_USB_LIMIT 0 ");
                    Settings.Secure.putInt(getContext().getContentResolver(), SYS_CMCC_USB_LIMIT, 0);
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public boolean interceptSetEnabledFunctions(String functions) {
        boolean isManufacturePort = false;
        if (DEBUG) {
            String str = TAG;
            Slog.i(str, "interceptSetEnabledFunctions  functions:" + functions);
        }
        if (functions != null) {
            isManufacturePort = containsFunctionOuter(functions, "manufacture");
        }
        String value = SystemProperties.get(USB_STATE_PROPERTY, "");
        if (!value.equals(functions) || !isManufacturePort || isAdbDisabledByDevicePolicy()) {
            String str2 = TAG;
            Slog.i(str2, "function: " + functions + " sys.usb.state: " + value);
            String usbLimit = getCmccUsbLimit();
            if ("0".equals(usbLimit)) {
                return false;
            }
            int simCardState = 0;
            if (getContext() != null) {
                TelephonyManager tm = (TelephonyManager) getContext().getSystemService("phone");
                if (tm == null) {
                    Slog.w(TAG, "TelephonyManager is null, return!");
                    return false;
                }
                simCardState = tm.getSimState();
            }
            if (DEBUG) {
                String str3 = TAG;
                Slog.i(str3, "interceptSetEnabledFunctions simcardstate = " + simCardState + " IsManufacturePort:" + isManufacturePort);
            }
            if (!(simCardState == 0 || simCardState == 1 || simCardState == 8 || simCardState == 6)) {
                SystemProperties.set(PERSIST_CMCC_USB_LIMIT, "0");
                usbLimit = "0";
            }
            if (!"1".equals(usbLimit) || isManufacturePort) {
                return false;
            }
            if (DEBUG) {
                Slog.i(TAG, "cmcc usb_limit return !");
            }
            return true;
        }
        String str4 = TAG;
        Slog.i(str4, "The current function: " + functions + " has been set, return!");
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean isCmccUsbLimit() {
        if ("1".equals(SystemProperties.get(PERSIST_CMCC_USB_LIMIT, "0"))) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean isAdbDisabled() {
        if (!HwDeviceManager.disallowOp(11)) {
            return false;
        }
        Settings.Global.putInt(this.mContentResolver, "adb_enabled", 0);
        return true;
    }

    public void bootCompleted() {
        HwUsbDeviceManager.super.bootCompleted();
        this.mIsBootCompleted = true;
    }

    /* access modifiers changed from: private */
    public void sendUsbLiquidBroadcast(Context context, String msg) {
        Intent intent = new Intent(ACTION_USBLIQUID);
        intent.setClassName(PACKAGE_NAME_USBLIQUID, CLASS_NAME_USBLIQUID);
        intent.putExtra(MSG_USBLIQUID_TYPE, msg);
        context.sendBroadcastAsUser(intent, UserHandle.ALL, PERMISSION_SEND_USB_LIQUID);
    }

    private UserManager getUserManager() {
        if (this.mUserManager == null) {
            this.mUserManager = UserManager.get(getContext());
        }
        return this.mUserManager;
    }

    /* access modifiers changed from: protected */
    public boolean isRepairMode() {
        return getUserManager().getUserInfo(ActivityManager.getCurrentUser()).isRepairMode();
    }

    /* access modifiers changed from: protected */
    public String applyUserRestrictions(String functions) {
        UserManager userManager = (UserManager) this.mContext.getSystemService("user");
        if (userManager == null || !userManager.hasUserRestriction("no_usb_file_transfer")) {
            return functions;
        }
        String functions2 = removeFunctionOuter(removeFunctionOuter(removeFunctionOuter(removeFunctionOuter(removeFunctionOuter(functions, "mtp"), "ptp"), "mass_storage"), "hisuite"), "hdb");
        if (HwAPPQoEUserAction.DEFAULT_CHIP_TYPE.equals(functions2)) {
            return "mtp";
        }
        return functions2;
    }

    private boolean isHdbEnabled() {
        HdbManagerInternal hdbManagerInternal = (HdbManagerInternal) LocalServices.getService(HdbManagerInternal.class);
        return hdbManagerInternal != null && hdbManagerInternal.isHdbEnabled();
    }

    /* access modifiers changed from: protected */
    public String applyHdbFunction(String functions) {
        if (containsFunctionOuter(functions, "hdb")) {
            functions = removeFunctionOuter(functions, "hdb");
        }
        if (shouldApplyHdbFunction(functions)) {
            if (!containsFunctionOuter(functions, "hdb")) {
                functions = addFunctionOuter(functions, "hdb");
            }
            String str = TAG;
            Slog.i(str, "add hdb is " + functions);
        }
        return functions;
    }

    private boolean shouldApplyHdbFunction(String functions) {
        if (isCmccUsbLimit()) {
            Slog.i(TAG, "cmcc_usb_limit do not set hdb");
            return false;
        } else if (isRepairMode()) {
            return true;
        } else {
            if (!isHdbEnabled() || (!"mtp".equals(functions) && !"mtp,adb".equals(functions) && !"ptp".equals(functions) && !"ptp,adb".equals(functions) && !"hisuite,mtp,mass_storage".equals(functions) && !"hisuite,mtp,mass_storage,adb".equals(functions) && !"bicr".equals(functions) && !"bicr,adb".equals(functions) && !"rndis".equals(functions) && !"rndis,adb".equals(functions))) {
                return false;
            }
            return true;
        }
    }

    private class AllowChargingAdbSettingsObserver extends ContentObserver {
        AllowChargingAdbSettingsObserver() {
            super(null);
        }

        public void onChange(boolean isChange) {
            boolean enable = false;
            if (Settings.Global.getInt(HwUsbDeviceManager.this.mContentResolver, HwUsbDeviceManager.ALLOW_CHARGING_ADB, 0) > 0) {
                enable = true;
            }
            Flog.i(1306, HwUsbDeviceManager.TAG + " AllowChargingAdb Settings enable:" + enable);
            if (HwUsbDeviceManager.this.mHandler != null) {
                HwUsbDeviceManager.this.mHandler.sendMessage(104, enable);
            }
        }
    }

    private boolean getUsbHandlerConnected() {
        if (this.mHandler != null) {
            return this.mHandler.mConnected;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean sendHandlerEmptyMessage(int what) {
        if (this.mHandler != null) {
            return this.mHandler.sendEmptyMessage(what);
        }
        return false;
    }

    private boolean containsFunctionOuter(String functions, String function) {
        if (this.mHandler != null) {
            return this.mHandler.containsFunction(functions, function);
        }
        return false;
    }

    private String addFunctionOuter(String functions, String function) {
        if (this.mHandler != null) {
            return this.mHandler.addFunction(functions, function);
        }
        return functions;
    }

    private String removeFunctionOuter(String functions, String function) {
        if (this.mHandler != null) {
            return this.mHandler.removeFunction(functions, function);
        }
        return functions;
    }

    private void setEnabledFunctionsEx(String functions, boolean isRestart) {
        if (this.mHandler != null) {
            this.mHandler.setEnabledFunctions(UsbManager.usbFunctionsFromString(functions), isRestart, false);
        }
    }

    private boolean setUsbConfigEx(String config) {
        if (this.mHandler == null) {
            return false;
        }
        this.mHandler.setUsbConfig(config);
        return this.mHandler.waitForState(config);
    }

    private boolean isAdbDisabledByDevicePolicy() {
        if (this.mHandler != null) {
            return this.mHandler.isAdbDisabledByDevicePolicy();
        }
        return false;
    }
}
