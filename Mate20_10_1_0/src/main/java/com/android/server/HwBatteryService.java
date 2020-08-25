package com.android.server;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.drawable.Icon;
import android.hardware.health.V1_0.HealthInfo;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.util.Flog;
import android.util.Slog;
import com.android.server.hidata.wavemapping.modelservice.ModelBaseService;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import com.android.server.multiwin.HwMultiWinConstants;
import com.android.server.power.HwAutoPowerOffController;
import com.android.server.wifipro.WifiProCommonUtils;
import com.android.server.wm.HwActivityStartInterceptor;
import com.huawei.msdp.devicestatus.DeviceStatusConstant;
import huawei.cust.HwCustUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

public final class HwBatteryService extends BatteryService {
    private static final String ACTION_BATTERY_ISCD_ERROR = "huawei.intent.action.BATTERY_ISCD_ERROR";
    private static final String ACTION_CALL_BNT_CLICKED = "huawei.intent.action.CALL_BNT_CLICKED";
    private static final String ACTION_QUICK_CHARGE = "huawei.intent.action.BATTERY_QUICK_CHARGE";
    private static final String ACTION_VIEW_CHARGE_LINE = "huawei.intent.action.chargeline";
    private static final String ACTION_WIRELESS_CHARGE_POSITION = "huawei.intent.action.WIRELESS_CHARGE_POSITION";
    private static final String ACTION_WIRELESS_TX_CHARGE_ERROR = "huawei.intent.action.WIRELESS_TX_CHARGE_ERROR";
    private static final String ACTION_WIRELESS_TX_STATUS_CHANGE = "huawei.intent.action.WIRELESS_TX_STATUS_CHANGE";
    private static final String BATTERY_ERR_NOTIFICATION_ID = "n_id";
    private static final String BATTERY_ISCD_STATUS_ERROR = "1";
    private static final String BATTERY_ISCD_STATUS_NORMAL = "0";
    private static final String BATTERY_OVP_STATUS_ERROR = "1";
    private static final String BATTERY_OVP_STATUS_NORMAL = "0";
    private static final int CHARGE_TIME_BIT = 24;
    private static final int CHARGE_TIME_MASK = 16777215;
    private static final int CHARGE_TIME_MAX_HOURS = 43200;
    private static final int CHARGE_TIME_VERIFY = 85;
    private static final String DEFAULT_CHARGE_TIME_REAMINING = "-1";
    private static final String DEFAULT_LANGUAGE_CH = "zh_CN_#Hans";
    private static final String FACTORY_VERSION = "factory";
    private static final int GET_NON_STANDARD_CABLE_DELAY_MS = 1000;
    private static final int GET_NON_STANDARD_CABLE_LOOP_TIMES = 4;
    private static final String HISI_PLATFORM_SUPPLY_NAME = "Battery";
    private static final String HUAWEI_CHINA_CUSTOMER_SERVICE_HOTLINE_NUMBER = "950800";
    /* access modifiers changed from: private */
    public static final boolean IS_CHINA_REGION = SystemProperties.get("ro.config.hw_optb", "0").equals("156");
    private static final boolean IS_HWBATTERY_THREAD_DISABLED = SystemProperties.getBoolean("ro.config.hwbatterythread.disable", false);
    /* access modifiers changed from: private */
    public static final boolean IS_ISCD_CHECK = SystemProperties.getBoolean("ro.config.check_battery_error", true);
    /* access modifiers changed from: private */
    public static final boolean IS_LED_CLOSE_BY_CAMERA = SystemProperties.getBoolean("ro.config.led_close_by_camera", false);
    /* access modifiers changed from: private */
    public static final boolean IS_SCREEN_ON_TURN_OFF_LED = SystemProperties.getBoolean("ro.config.screenon_turnoff_led", false);
    /* access modifiers changed from: private */
    public static final boolean IS_TWO_COLOR_LIGHT = SystemProperties.getBoolean("ro.config.hw_two_color_light", false);
    private static final int LOW_BATTERY_SHUTDOWN_LEVEL = 2;
    private static final int LOW_BATTERY_WARNING_LEVEL = 4;
    private static final int MAX_BATTERY_PROP_REGISTER_TIMES = 5;
    private static final int MSG_BATTERY_ISCD_ERROR = 6;
    private static final int MSG_BATTERY_OVP_ERROR = 2;
    private static final int MSG_BATTERY_PROP_REGISTER = 1;
    private static final int MSG_CHECK_NON_STANDARD_CABLE = 10;
    private static final int MSG_UPDATE_QUICK_CHARGE_STATE = 3;
    private static final int MSG_WIRELWSS_CHARGE_POSITION = 7;
    private static final String MTK_PLATFORM_SUPPLY_NAME = "battery";
    private static final String NON_STANDARD_CHARGE_LINE_NOTIFICATION_ID = "non_standard_charge_id";
    private static final int NON_STANDARD_CHARGE_LINE_STATUS = 1;
    private static final int NO_HW_CHARGE_TIME_COMPUTE = -1;
    private static final String OVP_ERR_CHANNEL_ID = "battery_error_c_id";
    private static final String PERMISSION_HW_BATTERY_CHANGE = "com.huawei.permission.BATTERY_CHARGE";
    private static final String PERMISSION_TX_CHARGE_ERROR = "com.huawei.batteryservice.permission.WIRELESS_TX_CHARGE_ERROR";
    private static final String PERMISSION_TX_STATUS_CHANGE = "com.huawei.batteryservice.permission.WIRELESS_TX_STATUS_CHANGE";
    private static final int PLUGGED_NONE = 0;
    private static final String PPWER_CONNECTED_RINGTONE = "PowerConnected.ogg";
    private static final String QCOM_PLATFORM_SUPPLY_NAME = "usb";
    private static final String QUICK_CHARGE_FCP_STATUS = "1";
    private static final String QUICK_CHARGE_NODE_NOT_EXIST = "0";
    private static final String QUICK_CHARGE_NONE_STATUS = "0";
    private static final String QUICK_CHARGE_SCP_STATUS = "2";
    private static final String QUICK_CHARGE_STATUS_NORMAL = "0";
    private static final String QUICK_CHARGE_STATUS_WORKING = "1";
    private static final String QUICK_CHARGE_WIRELESS_FCP_STATUS = "3";
    private static final String QUICK_CHARGE_WIRELESS_SCP_STATUS = "4";
    private static final String RUN_MODE_PROPERTY = "ro.runmode";
    private static final int SHUTDOWN_LEVEL_FLASHINGARGB = -1;
    private static final String SUPER_CHARGE_LINE_NODE = "sys/class/hw_power/power_ui/cable_type";
    private static final String TAG = "HwBatteryService";
    private static final String WIRELESS_TX_DIR = "sys/class/hw_power/charger/wireless_tx";
    private static final int WIRELESS_TX_END = 0;
    private static final int WIRELESS_TX_ERROR = -1;
    private static final int WIRELESS_TX_FLAG_CLOSE = 2;
    private static final int WIRELESS_TX_FLAG_OPEN = 1;
    private static final int WIRELESS_TX_FLAG_UNKNOWN = 0;
    private static final String WIRELESS_TX_LOW_BATTERY = "20%";
    private static final String WIRELESS_TX_OPEN = "sys/class/hw_power/charger/wireless_tx/tx_open";
    private static final int WIRELESS_TX_START = 1;
    private static final String WIRELESS_TX_STATUS = "sys/class/hw_power/charger/wireless_tx/tx_status";
    private static final int WIRELESS_TX_STATUS_WAIT_TIME = 100;
    private static final int WIRELESS_TX_SWITCH_CLOSE = 0;
    private static final int WIRELESS_TX_SWITCH_OPEN = 1;
    private static final String WIRELSSS_CONNECTED_RINGTONE = "WirelessPowerConnected.ogg";
    private static final int WL_TX_STATUS_CHARGE_DONE = 4;
    private static final int WL_TX_STATUS_DEFAULT = 0;
    private static final int WL_TX_STATUS_FAULT_BASE = 16;
    private static final int WL_TX_STATUS_IN_CHARGING = 3;
    private static final int WL_TX_STATUS_IN_WL_CHARGING = 22;
    private static final int WL_TX_STATUS_PING = 1;
    private static final int WL_TX_STATUS_PING_SUCC = 2;
    private static final int WL_TX_STATUS_PING_TIMEOUT = 19;
    private static final int WL_TX_STATUS_RX_DISCONNECT = 18;
    private static final int WL_TX_STATUS_SOC_ERROR = 23;
    private static final int WL_TX_STATUS_TBATT_HIGH = 21;
    private static final int WL_TX_STATUS_TBATT_LOW = 20;
    private static final int WL_TX_STATUS_TX_CLOSE = 17;
    private AudioAttributes mAudioAttributes;
    /* access modifiers changed from: private */
    public final UEventObserver mBatteryFcpObserver = new UEventObserver() {
        /* class com.android.server.HwBatteryService.AnonymousClass1 */

        public void onUEvent(UEventObserver.UEvent event) {
            String scpStatus = event.get("POWER_SUPPLY_SCP_STATUS", "0");
            String ovpStatus = event.get("POWER_SUPPLY_BAT_OVP", "0");
            String supplyName = event.get("POWER_SUPPLY_NAME");
            if (HwBatteryService.HISI_PLATFORM_SUPPLY_NAME.equals(supplyName) || HwBatteryService.QCOM_PLATFORM_SUPPLY_NAME.equals(supplyName) || HwBatteryService.MTK_PLATFORM_SUPPLY_NAME.equals(supplyName)) {
                String fcpStatus = event.get("POWER_SUPPLY_FCP_STATUS", "0");
                HwBatteryService.this.mHwBatteryHandler.updateQuickChargeState(HwBatteryService.this.getQuickChargeBroadcastStatus(fcpStatus, scpStatus));
                Slog.d(HwBatteryService.TAG, "onUEvent fcpStatus = " + fcpStatus);
            }
            Slog.d(HwBatteryService.TAG, "onUEvent scpStatus = " + scpStatus + " ,ovpStatus = " + ovpStatus);
            HwBatteryService.this.handleBatteryOvpStatus(ovpStatus);
            HwBatteryService.this.handleWirelessTxStatus();
            HwBatteryService.this.handerChargeTimeRemaining(event.get("POWER_SUPPLY_CHARGE_TIME_REMAINING", HwBatteryService.DEFAULT_CHARGE_TIME_REAMINING));
            HwBatteryService.this.handleWirelessChangerPosition();
        }
    };
    /* access modifiers changed from: private */
    public final UEventObserver mBatteryIscdObserver = new UEventObserver() {
        /* class com.android.server.HwBatteryService.AnonymousClass2 */

        public void onUEvent(UEventObserver.UEvent event) {
            Slog.d(HwBatteryService.TAG, "onUEvent battery iscd error");
            String unused = HwBatteryService.this.mBatteryIscdStatus = "1";
            HwBatteryService.this.handleBatteryIscdStatus();
        }
    };
    /* access modifiers changed from: private */
    public String mBatteryIscdStatus = "0";
    private String mBatteryOvpStatus = "0";
    /* access modifiers changed from: private */
    public int mBatteryPropRegisterTryTimes = 0;
    /* access modifiers changed from: private */
    public final Context mContext;
    private HwCustBatteryService mCust = ((HwCustBatteryService) HwCustUtils.createObj(HwCustBatteryService.class, new Object[0]));
    /* access modifiers changed from: private */
    public int mFlashingARGB;
    private HwAutoPowerOffController mHwAutoPowerOffController;
    /* access modifiers changed from: private */
    public HwBatteryHandler mHwBatteryHandler;
    private HandlerThread mHwBatteryThread;
    /* access modifiers changed from: private */
    public HwLed mHwLed;
    /* access modifiers changed from: private */
    public boolean mIsBootFinish = false;
    /* access modifiers changed from: private */
    public boolean mIsFlagScreenOn = true;
    /* access modifiers changed from: private */
    public boolean mIsFrontCameraOpening = false;
    private boolean mIsHwChargeTimeValid = false;
    private boolean mIsLowBattery = false;
    /* access modifiers changed from: private */
    public boolean mIsNotificationExisting;
    private boolean mIsSystemReady = false;
    private int mLastWirelessTxStatus = 0;
    private final Object mLock = new Object();
    /* access modifiers changed from: private */
    public int mNotificationLedOff;
    /* access modifiers changed from: private */
    public int mNotificationLedOn;
    /* access modifiers changed from: private */
    public NotificationManager mNotificationManager;
    /* access modifiers changed from: private */
    public PowerManager mPowerManager;
    /* access modifiers changed from: private */
    public String mQuickChargeStatus = "0";
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        /* class com.android.server.HwBatteryService.AnonymousClass3 */

        public void onReceive(Context context, Intent intent) {
            if (context == null || intent == null) {
                Slog.i(HwBatteryService.TAG, "context or intent is null!");
                return;
            }
            Slog.i(HwBatteryService.TAG, "intent = " + intent);
            if (HwBatteryService.ACTION_CALL_BNT_CLICKED.equals(intent.getAction())) {
                HwBatteryService.this.closeSystemDialogs(context);
                int notificationID = intent.getIntExtra(HwBatteryService.BATTERY_ERR_NOTIFICATION_ID, 0);
                if (HwBatteryService.this.mNotificationManager != null) {
                    HwBatteryService.this.mNotificationManager.cancelAsUser(null, notificationID, UserHandle.CURRENT);
                }
                Intent callIntent = new Intent("android.intent.action.CALL");
                if (HwBatteryService.IS_CHINA_REGION) {
                    callIntent.setData(Uri.parse("tel:950800"));
                }
                callIntent.setFlags(276824064);
                context.startActivityAsUser(callIntent, UserHandle.CURRENT);
            } else if ("android.intent.action.LOCKED_BOOT_COMPLETED".equals(intent.getAction())) {
                boolean unused = HwBatteryService.this.mIsBootFinish = true;
                if (HwBatteryService.IS_ISCD_CHECK && !HwBatteryService.this.mBatteryIscdStatus.equals("0")) {
                    HwBatteryService.this.handleBatteryIscdStatus();
                }
            } else if (SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_ON.equals(intent.getAction())) {
                boolean unused2 = HwBatteryService.this.mIsFlagScreenOn = true;
                if (HwBatteryService.IS_SCREEN_ON_TURN_OFF_LED) {
                    HwBatteryService.this.mHwLed.newUpdateLightsLocked();
                }
            } else if (SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_OFF.equals(intent.getAction())) {
                boolean unused3 = HwBatteryService.this.mIsFlagScreenOn = false;
                if (HwBatteryService.IS_SCREEN_ON_TURN_OFF_LED) {
                    HwBatteryService.this.mHwLed.newUpdateLightsLocked();
                }
            } else if (HwBatteryService.ACTION_VIEW_CHARGE_LINE.equals(intent.getAction())) {
                HwBatteryService.this.closeSystemDialogs(context);
                HwBatteryService.this.cancelChargeLineNotification(33686042);
                Intent webIntent = new Intent("android.intent.action.VIEW");
                webIntent.setData(Uri.parse(HwBatteryService.this.getPlayCardHtmlAddress()));
                webIntent.setFlags(276824576);
                context.startActivityAsUser(webIntent, UserHandle.CURRENT);
            }
        }
    };
    /* access modifiers changed from: private */
    public Ringtone mRingRingtone;
    /* access modifiers changed from: private */
    public Uri mUri;
    private int mWirelessTxFlag = 0;

    static /* synthetic */ int access$808(HwBatteryService x0) {
        int i = x0.mBatteryPropRegisterTryTimes;
        x0.mBatteryPropRegisterTryTimes = i + 1;
        return i;
    }

    public HwBatteryService(Context context) {
        super(context);
        this.mContext = context;
        initAndRegisterReceiver();
        if (IS_HWBATTERY_THREAD_DISABLED) {
            Slog.w(TAG, "HwBatteryService thread is disabled.");
        } else {
            this.mHwBatteryThread = new HandlerThread(TAG);
            this.mHwBatteryThread.start();
            this.mHwBatteryHandler = new HwBatteryHandler(this.mHwBatteryThread.getLooper());
        }
        this.mHwLed = new HwLed(context, (LightsManager) getLocalService(LightsManager.class));
        this.mAudioAttributes = new AudioAttributes.Builder().setUsage(13).setContentType(4).build();
        if (this.mCust.isAutoPowerOffOn()) {
            this.mHwAutoPowerOffController = new HwAutoPowerOffController(context);
        }
    }

    private boolean writeFile(String path, String data) {
        StringBuilder sb;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path);
            fos.write(data.getBytes("UTF-8"));
            try {
                fos.close();
                return true;
            } catch (IOException e) {
                sb = new StringBuilder();
            }
            sb.append("closeFile ");
            sb.append(path);
            Slog.e(TAG, sb.toString());
            return false;
        } catch (IOException e2) {
            Slog.w(TAG, "writeFile " + path);
            if (fos == null) {
                return false;
            }
            try {
                fos.close();
                return false;
            } catch (IOException e3) {
                sb = new StringBuilder();
            }
        } catch (Throwable th) {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e4) {
                    Slog.e(TAG, "closeFile " + path);
                }
            }
            throw th;
        }
    }

    /* access modifiers changed from: protected */
    public int alterWirelessTxSwitchInternal(int status) {
        if (status == 1 || status == 0) {
            Slog.i(TAG, "alterWirelessTxStatus status : " + status);
            synchronized (this.mLock) {
                this.mWirelessTxFlag = 0;
                this.mLastWirelessTxStatus = 0;
                if (!writeFile(WIRELESS_TX_OPEN, String.valueOf(status))) {
                    Slog.e(TAG, "writeFile error sys/class/hw_power/charger/wireless_tx/tx_open");
                    return -1;
                }
                try {
                    this.mLock.wait(100);
                } catch (InterruptedException e) {
                    Slog.w(TAG, "Error occurs when sleep");
                }
                int wirelessTxStatus = getWirelessTxStatus(WIRELESS_TX_STATUS);
                if (!allowWirelessTxSwitch(status, wirelessTxStatus)) {
                    Slog.i(TAG, "not allowed to open tx wireless : " + wirelessTxStatus);
                    writeFile(WIRELESS_TX_OPEN, String.valueOf(0));
                    return wirelessTxStatus;
                }
                if (status == 1) {
                    this.mWirelessTxFlag = 1;
                } else {
                    sendWirelessTxStatusChangeBroadcast(0);
                    this.mWirelessTxFlag = 2;
                }
                return 0;
            }
        }
        Slog.w(TAG, "alterWirelessTxStatus status error : " + status);
        return -1;
    }

    /* access modifiers changed from: protected */
    public int getWirelessTxSwitchInternal() {
        return getWirelessTxStatus(WIRELESS_TX_OPEN);
    }

    /* access modifiers changed from: protected */
    public boolean supportWirelessTxChargeInternal() {
        File wirelessTxDir = new File(WIRELESS_TX_DIR);
        Slog.i(TAG, "exists : " + wirelessTxDir.exists());
        return wirelessTxDir.exists() && wirelessTxDir.isDirectory();
    }

    private int getWirelessTxStatus(String wirelessTxPath) {
        int result = 0;
        String txStatus = null;
        try {
            txStatus = FileUtils.readTextFile(new File(wirelessTxPath), 0, null).trim();
            result = Integer.parseInt(txStatus);
        } catch (IOException e) {
            Slog.w(TAG, "Error occurs when read: " + wirelessTxPath);
        } catch (NumberFormatException e2) {
            Slog.e(TAG, "Error occurs when translate status : " + txStatus);
        }
        Slog.i(TAG, "getWirelessTxStatus , " + wirelessTxPath + " : " + result);
        return result;
    }

    private boolean isWirelessTxNormal(int status) {
        if (status == 2 || status == 3) {
            return true;
        }
        return false;
    }

    private boolean isWirelessTxError(int status) {
        if (!(status == 4 || status == 16 || status == 17)) {
            switch (status) {
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    private boolean isWirelessTxDisconnect(int status) {
        return status == 18 || status == 1;
    }

    private int getWirelessTxErrorRes(int status) {
        this.mIsLowBattery = false;
        if (status == 4) {
            return 33686274;
        }
        if (status != 23) {
            switch (status) {
                case 19:
                    return 33686276;
                case 20:
                    return 33686278;
                case 21:
                    return 33686277;
                default:
                    return -1;
            }
        } else {
            this.mIsLowBattery = true;
            return 33686275;
        }
    }

    private boolean allowWirelessTxSwitch(int switchStatus, int wirelessTxStatus) {
        if (switchStatus == 0) {
            return true;
        }
        switch (wirelessTxStatus) {
            case 20:
            case 21:
            case 22:
            case 23:
                return false;
            default:
                return true;
        }
    }

    private String getWirelessChangerPositionPath() {
        return "/sys/class/hw_power/power_ui/wl_off_pos";
    }

    /* access modifiers changed from: private */
    public void handleWirelessChangerPosition() {
        int offPositon = 0;
        try {
            offPositon = Integer.parseInt(FileUtils.readTextFile(new File(getWirelessChangerPositionPath()), 0, null).trim());
        } catch (IOException e) {
            Slog.w(TAG, "Error occurs when read");
        } catch (NumberFormatException e2) {
            Slog.e(TAG, "Error occurs when translate status : ");
        }
        if (offPositon == 1) {
            this.mHwBatteryHandler.removeMessages(7);
            this.mHwBatteryHandler.sendMessage(Message.obtain(this.mHwBatteryHandler, 7));
        }
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0059, code lost:
        if (isWirelessTxNormal(r2) == false) goto L_0x005f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x005b, code lost:
        sendWirelessTxStatusChangeBroadcast(1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0063, code lost:
        if (isWirelessTxError(r2) == false) goto L_0x006c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0065, code lost:
        sendWirelessTxChargeErrorBroadcast();
        sendWirelessTxErrorNotification(r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x0070, code lost:
        if (isWirelessTxDisconnect(r2) == false) goto L_0x0077;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0072, code lost:
        sendWirelessTxStatusChangeBroadcast(0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x0077, code lost:
        android.util.Slog.i(com.android.server.HwBatteryService.TAG, "default charger status don't exists.");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:?, code lost:
        return;
     */
    public void handleWirelessTxStatus() {
        Slog.i(TAG, "handleWirelessTxStatus");
        synchronized (this.mLock) {
            if (this.mWirelessTxFlag != 1) {
                Slog.i(TAG, "switch not open, return, mWirelessTxFlag : " + this.mWirelessTxFlag);
                return;
            }
            int wirelessTxStatus = getWirelessTxStatus(WIRELESS_TX_STATUS);
            if (wirelessTxStatus == this.mLastWirelessTxStatus) {
                Slog.i(TAG, "wireless_tx_status not changed, return, mLastWirelessTxStatus : " + this.mLastWirelessTxStatus);
                return;
            }
            this.mLastWirelessTxStatus = wirelessTxStatus;
        }
    }

    private void sendWirelessTxStatusChangeBroadcast(int status) {
        Intent intent = new Intent(ACTION_WIRELESS_TX_STATUS_CHANGE);
        intent.addFlags(1073741824);
        intent.addFlags(536870912);
        intent.putExtra(HwMultiWinConstants.STATUS_KEY_STR, status);
        Slog.i(TAG, "sendWirelessTxStatusChangeBroadcast status : " + status);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, PERMISSION_TX_STATUS_CHANGE);
    }

    private void sendWirelessTxChargeErrorBroadcast() {
        Intent intent = new Intent(ACTION_WIRELESS_TX_CHARGE_ERROR);
        intent.addFlags(1073741824);
        intent.addFlags(536870912);
        Slog.i(TAG, "sendWirelessTxChargeErrorBroadcast");
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, PERMISSION_TX_CHARGE_ERROR);
    }

    private void sendWirelessTxErrorNotification(int status) {
        int messageId = getWirelessTxErrorRes(status);
        if (messageId != -1) {
            Slog.i(TAG, "sendWirelessTxErrorNotification");
            String message = this.mContext.getResources().getString(messageId);
            if (this.mIsLowBattery) {
                message = String.format(Locale.ROOT, message, WIRELESS_TX_LOW_BATTERY);
            }
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(HwActivityStartInterceptor.ACTION_CONFIRM_APPLOCK_PACKAGENAME, "com.huawei.systemmanager.power.ui.HwPowerManagerActivity"));
            PendingIntent pi = PendingIntent.getActivityAsUser(this.mContext, 0, intent, 0, null, UserHandle.CURRENT);
            makeNotificationChannel(message);
            Notification notification = new Notification.Builder(this.mContext, OVP_ERR_CHANNEL_ID).setSmallIcon(33752152).setContentText(message).setContentIntent(pi).setVibrate(new long[0]).setPriority(2).setWhen(System.currentTimeMillis()).setShowWhen(true).setVisibility(1).setStyle(new Notification.BigTextStyle().bigText(message)).setAutoCancel(true).build();
            NotificationManager notificationManager = this.mNotificationManager;
            if (notificationManager != null) {
                notificationManager.notifyAsUser(null, messageId, notification, UserHandle.ALL);
            }
        }
    }

    public void onStart() {
        HwBatteryService.super.onStart();
        new HealthdDeathRecipient(ServiceManager.getService("batteryproperties"));
    }

    private class HealthdDeathRecipient implements IBinder.DeathRecipient {
        private IBinder mCb;

        HealthdDeathRecipient(IBinder cb) {
            if (cb != null) {
                try {
                    Slog.i(HwBatteryService.TAG, "linkToDeath Healthd.");
                    cb.linkToDeath(this, 0);
                } catch (RemoteException e) {
                    Slog.w(HwBatteryService.TAG, "HealthdDeathRecipient() could not link to " + cb + " binder death");
                }
            }
            this.mCb = cb;
        }

        public void binderDied() {
            Slog.w(HwBatteryService.TAG, "Healthd died.");
            IBinder iBinder = this.mCb;
            if (iBinder != null) {
                iBinder.unlinkToDeath(this, 0);
            }
            int unused = HwBatteryService.this.mBatteryPropRegisterTryTimes = 0;
            if (HwBatteryService.this.mHwBatteryHandler == null) {
                Slog.w(HwBatteryService.TAG, "mHwBatteryHandler is null.");
            } else {
                HwBatteryService.this.mHwBatteryHandler.sendEmptyMessageDelayed(1, 2000);
            }
        }
    }

    /* access modifiers changed from: private */
    public final class HwBatteryHandler extends Handler {
        public HwBatteryHandler(Looper looper) {
            super(looper);
            if (HwBatteryService.IS_ISCD_CHECK) {
                HwBatteryService.this.mBatteryIscdObserver.startObserving("BATTERY_EVENT=FATAL_ISC");
            }
            try {
                updateQuickChargeState(HwBatteryService.this.getQuickChargeBroadcastStatus(FileUtils.readTextFile(new File(HwBatteryService.this.getQuickChargeStatePath()), 0, null).trim(), FileUtils.readTextFile(new File(HwBatteryService.this.getDCQuickChargeStatePath()), 0, null).trim()));
            } catch (IOException e) {
                Slog.e(HwBatteryService.TAG, "Error get initialized state.");
            }
            try {
                if (HwBatteryService.IS_ISCD_CHECK) {
                    String iscdStatus = FileUtils.readTextFile(new File(HwBatteryService.this.getBatteryIscdStatePath()), 0, null).trim();
                    Slog.i(HwBatteryService.TAG, "iscdStatus: " + iscdStatus);
                }
            } catch (IOException e2) {
                Slog.e(HwBatteryService.TAG, "Error get initialized state.");
            }
            HwBatteryService.this.mBatteryFcpObserver.startObserving("SUBSYSTEM=power_supply");
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                IBinder binder = ServiceManager.checkService("batteryproperties");
                if (binder == null || !binder.isBinderAlive()) {
                    HwBatteryService.access$808(HwBatteryService.this);
                    if (HwBatteryService.this.mBatteryPropRegisterTryTimes < 5) {
                        Slog.i(HwBatteryService.TAG, "Try to get batteryproperties service again.");
                        HwBatteryService.this.mHwBatteryHandler.sendEmptyMessageDelayed(1, 2000);
                        return;
                    }
                    Slog.e(HwBatteryService.TAG, "There is no connection between batteryservice and batteryproperties.");
                    return;
                }
                int unused = HwBatteryService.this.mBatteryPropRegisterTryTimes = 0;
                new HealthdDeathRecipient(binder);
                HwBatteryService.this.registerHealthCallback();
            } else if (i == 2) {
                HwBatteryService.this.sendBatteryErrorNotification(33685926, 33685927, 33751168);
            } else if (i != 3) {
                if (i == 6) {
                    HwBatteryService.this.sendBatteryErrorNotification(33685540, 33685539, 33751082);
                    HwBatteryService.this.sendBatteryIsdcErrorBroadcast();
                } else if (i == 7) {
                    sendWirelessChargePositionBroadcast();
                    if (HwBatteryService.this.mPowerManager == null) {
                        HwBatteryService hwBatteryService = HwBatteryService.this;
                        PowerManager unused2 = hwBatteryService.mPowerManager = (PowerManager) hwBatteryService.mContext.getSystemService("power");
                    }
                    HwBatteryService.this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), 3, "wirelessDock.wakeUp");
                } else if (i == 10) {
                    sendNonStandardChargeMsg(((Integer) msg.obj).intValue());
                }
            } else if (msg.obj instanceof String) {
                String status = (String) msg.obj;
                if (!HwBatteryService.this.mQuickChargeStatus.equals(status)) {
                    String unused3 = HwBatteryService.this.mQuickChargeStatus = status;
                    sendQuickChargeBroadcast();
                }
            }
        }

        public void updateQuickChargeState(String state) {
            if (state == null) {
                Slog.i(HwBatteryService.TAG, "Quick Charge State is null, return!");
                return;
            }
            removeMessages(3);
            Message msg = Message.obtain(this, 3);
            msg.obj = state;
            sendMessage(msg);
        }

        private void sendWirelessChargePositionBroadcast() {
            Slog.i(HwBatteryService.TAG, "Send Wireless Charge Position Broadcast!");
            Intent positionIntent = new Intent(HwBatteryService.ACTION_WIRELESS_CHARGE_POSITION);
            positionIntent.addFlags(1073741824);
            positionIntent.addFlags(536870912);
            HwBatteryService.this.mContext.sendBroadcastAsUser(positionIntent, UserHandle.ALL, HwBatteryService.PERMISSION_HW_BATTERY_CHANGE);
        }

        private void sendQuickChargeBroadcast() {
            Intent quickChargeIntent = new Intent(HwBatteryService.ACTION_QUICK_CHARGE);
            quickChargeIntent.addFlags(1073741824);
            quickChargeIntent.addFlags(536870912);
            quickChargeIntent.putExtra("quick_charge_status", HwBatteryService.this.mQuickChargeStatus);
            Slog.i(HwBatteryService.TAG, "Stick broadcast intent: " + quickChargeIntent + " mQuickChargeStatus:" + HwBatteryService.this.mQuickChargeStatus);
            HwBatteryService.this.mContext.sendStickyBroadcastAsUser(quickChargeIntent, UserHandle.ALL);
        }

        private void sendNonStandardChargeMsg(int loopTimes) {
            Slog.d(HwBatteryService.TAG, "Receive msg MSG_CHECK_NON_STANDARD_CABLE with loopTimes: " + loopTimes);
            if (loopTimes <= 0) {
                return;
            }
            if (!HwBatteryService.this.isChargeLineStandard().booleanValue()) {
                HwBatteryService.this.sendNonStandardChargeLineNotification();
            } else {
                sendMessageDelayed(Message.obtain(HwBatteryService.this.mHwBatteryHandler, 10, Integer.valueOf(loopTimes - 1)), 1000);
            }
        }
    }

    /* access modifiers changed from: private */
    public String getQuickChargeStatePath() {
        return "/sys/class/power_supply/Battery/fcp_status";
    }

    /* access modifiers changed from: private */
    public String getDCQuickChargeStatePath() {
        return "/sys/class/power_supply/Battery/scp_status";
    }

    private String getBatteryOvpStatePath() {
        return "/sys/class/power_supply/Battery/bat_ovp";
    }

    /* access modifiers changed from: private */
    public String getBatteryIscdStatePath() {
        return "/sys/class/hw_power/battery/isc";
    }

    private void initAndRegisterReceiver() {
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CALL_BNT_CLICKED);
        filter.addAction("android.intent.action.LOCKED_BOOT_COMPLETED");
        filter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_ON);
        filter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_OFF);
        filter.addAction(ACTION_VIEW_CHARGE_LINE);
        this.mContext.registerReceiver(this.mReceiver, filter, "android.permission.DEVICE_POWER", null);
    }

    /* access modifiers changed from: private */
    public void closeSystemDialogs(Context context) {
        context.sendBroadcastAsUser(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"), UserHandle.ALL);
    }

    /* access modifiers changed from: private */
    public String getPlayCardHtmlAddress() {
        return this.mContext.getResources().getString(33686043);
    }

    private void makeNotificationChannel(String name) {
        if (this.mNotificationManager != null) {
            this.mNotificationManager.createNotificationChannel(new NotificationChannel(OVP_ERR_CHANNEL_ID, name, 4));
        }
    }

    /* access modifiers changed from: private */
    public void sendBatteryErrorNotification(int titleId, int messageId, int iconId) {
        Slog.i(TAG, "sendBatteryErrorNotification");
        String title = this.mContext.getResources().getString(titleId);
        Intent dialIntent = new Intent("android.intent.action.DIAL");
        if (IS_CHINA_REGION) {
            dialIntent.setData(Uri.parse("tel:950800"));
        }
        PendingIntent pi = PendingIntent.getActivityAsUser(this.mContext, 0, dialIntent, 0, null, UserHandle.CURRENT);
        Intent intent = new Intent(ACTION_CALL_BNT_CLICKED);
        intent.putExtra(BATTERY_ERR_NOTIFICATION_ID, titleId);
        intent.setPackage(this.mContext.getPackageName());
        PendingIntent actionClickPI = PendingIntent.getBroadcast(this.mContext, 0, intent, 0);
        makeNotificationChannel(title);
        Notification.Builder build = new Notification.Builder(this.mContext, OVP_ERR_CHANNEL_ID).setSmallIcon(iconId).setOngoing(true).setContentTitle(title).setContentText(this.mContext.getResources().getString(messageId)).setContentIntent(pi).setTicker(title).setVibrate(new long[0]).setPriority(2).setWhen(System.currentTimeMillis()).setShowWhen(true).setVisibility(1).setAutoCancel(true);
        if (IS_CHINA_REGION) {
            build.addAction(new Notification.Action.Builder((Icon) null, this.mContext.getString(33685928), actionClickPI).build());
        }
        Notification notification = build.build();
        NotificationManager notificationManager = this.mNotificationManager;
        if (notificationManager != null) {
            notificationManager.notifyAsUser(null, titleId, notification, UserHandle.ALL);
        }
    }

    /* access modifiers changed from: private */
    public void handleBatteryOvpStatus(String status) {
        if (!this.mBatteryOvpStatus.equals(status) && "1".equals(status)) {
            Slog.i(TAG, "battery ovp error occur, send notification before.");
            this.mBatteryOvpStatus = status;
            HwBatteryHandler hwBatteryHandler = this.mHwBatteryHandler;
            if (hwBatteryHandler != null) {
                if (hwBatteryHandler.hasMessages(2)) {
                    this.mHwBatteryHandler.removeMessages(2);
                }
                this.mHwBatteryHandler.sendMessage(Message.obtain(this.mHwBatteryHandler, 2, status));
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleBatteryIscdStatus() {
        HwBatteryHandler hwBatteryHandler;
        if (this.mIsBootFinish && (hwBatteryHandler = this.mHwBatteryHandler) != null) {
            if (hwBatteryHandler.hasMessages(6)) {
                this.mHwBatteryHandler.removeMessages(6);
            }
            this.mHwBatteryHandler.sendMessage(Message.obtain(this.mHwBatteryHandler, 6));
        }
    }

    /* access modifiers changed from: private */
    public String getQuickChargeBroadcastStatus(String fcpStatus, String scpStatus) {
        String status;
        boolean isWireless = isWirelessCharge();
        String str = "1";
        String str2 = "4";
        if (str.equals(fcpStatus) && str.equals(scpStatus)) {
            if (!isWireless) {
                str2 = "2";
            }
            status = str2;
        } else if (str.equals(fcpStatus) && "0".equals(scpStatus)) {
            if (isWireless) {
                str = "3";
            }
            status = str;
        } else if (!"0".equals(fcpStatus) || !str.equals(scpStatus)) {
            status = "0";
        } else {
            if (!isWireless) {
                str2 = "2";
            }
            status = str2;
        }
        Slog.i(TAG, "quick charge status : " + status);
        return status;
    }

    public void onBootPhase(int phase) {
        HwBatteryService.super.onBootPhase(phase);
        if (phase == 1000) {
            Slog.i(TAG, "Set mIsSystemReady to true when system boot completed");
            this.mIsSystemReady = true;
        }
    }

    /* access modifiers changed from: protected */
    public void handleNonStandardChargeLine(int plugInType) {
        if (this.mOldPlugInType != 0) {
            Slog.e(TAG, "same plug in type as the last time");
            return;
        }
        this.mOldPlugInType = plugInType;
        HwBatteryHandler hwBatteryHandler = this.mHwBatteryHandler;
        if (hwBatteryHandler != null) {
            if (hwBatteryHandler.hasMessages(10)) {
                this.mHwBatteryHandler.removeMessages(10);
            }
            this.mHwBatteryHandler.sendMessageDelayed(Message.obtain(this.mHwBatteryHandler, 10, 4), 1000);
        }
    }

    /* access modifiers changed from: protected */
    public void cancelChargeLineNotification(int titleId) {
        Slog.d(TAG, "charge line pull out, try to cancel non_standard charge line notification");
        if (!this.mIsSystemReady) {
            Slog.e(TAG, "System boot is not ready, can not cancel notification");
            return;
        }
        NotificationManager notificationManager = this.mNotificationManager;
        if (notificationManager != null) {
            notificationManager.cancelAsUser(null, titleId, UserHandle.CURRENT);
        }
    }

    /* access modifiers changed from: private */
    public void sendNonStandardChargeLineNotification() {
        Slog.i(TAG, "start to send non_stantard charge line notification");
        if (!this.mIsSystemReady) {
            Slog.e(TAG, "System boot is not ready, can not send notification");
        } else if (this.mNotificationManager == null) {
            Slog.e(TAG, "mNotificationManager is null");
        } else {
            this.mNotificationManager.createNotificationChannel(new NotificationChannel(NON_STANDARD_CHARGE_LINE_NOTIFICATION_ID, this.mContext.getResources().getString(33685537), 3));
            PendingIntent viewPendingIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(ACTION_VIEW_CHARGE_LINE), 0);
            String message = this.mContext.getResources().getString(33686041);
            String viewButtonMessage = this.mContext.getResources().getString(33686038);
            Notification.Builder builder = new Notification.Builder(this.mContext, NON_STANDARD_CHARGE_LINE_NOTIFICATION_ID).setSmallIcon(33751954).setContentText(message).setWhen(System.currentTimeMillis()).setShowWhen(true).setPriority(0).setVisibility(-1).setStyle(new Notification.BigTextStyle().bigText(message)).setAutoCancel(true);
            if (IS_CHINA_REGION && Locale.getDefault().toString().equals(DEFAULT_LANGUAGE_CH)) {
                builder.setContentIntent(viewPendingIntent).addAction(new Notification.Action.Builder((Icon) null, viewButtonMessage, viewPendingIntent).build());
            }
            this.mNotificationManager.notifyAsUser(null, 33686042, builder.build(), UserHandle.ALL);
        }
    }

    /* access modifiers changed from: private */
    public Boolean isChargeLineStandard() {
        if (getSuperChargeLineStatus() == 1) {
            return false;
        }
        return true;
    }

    private int getSuperChargeLineStatus() {
        int lineStatus = 0;
        String lineNode = null;
        try {
            lineNode = FileUtils.readTextFile(new File(SUPER_CHARGE_LINE_NODE), 0, null).trim();
            lineStatus = Integer.parseInt(lineNode);
        } catch (IOException e) {
            Slog.e(TAG, "Error occurs when read charge line status file");
        } catch (NumberFormatException e2) {
            Slog.e(TAG, "Error occurs when translate lineNode: " + lineNode);
        }
        Slog.i(TAG, "get super charge line status: " + lineStatus + " lineNode: " + lineNode);
        return lineStatus;
    }

    /* access modifiers changed from: private */
    public final class HwLed {
        private final int mBatteryFullARGB;
        private final int mBatteryLedOff;
        private final int mBatteryLedOn;
        private final Light mBatteryLight;
        private final int mBatteryLowARGB;
        private final int mBatteryMediumARGB;

        public HwLed(Context context, LightsManager lights) {
            this.mBatteryLight = lights.getLight(3);
            this.mBatteryLowARGB = context.getResources().getInteger(17694866);
            if (HwBatteryService.IS_TWO_COLOR_LIGHT) {
                this.mBatteryMediumARGB = context.getResources().getInteger(17694866);
            } else {
                this.mBatteryMediumARGB = context.getResources().getInteger(17694867);
            }
            this.mBatteryFullARGB = context.getResources().getInteger(17694863);
            this.mBatteryLedOn = context.getResources().getInteger(17694865);
            this.mBatteryLedOff = context.getResources().getInteger(17694864);
        }

        private void updatelightFlashingImpl(int flashRGB, int color, int level, boolean isNotifyExist, String lightColor) {
            if (HwBatteryService.this.mFlashingARGB == flashRGB) {
                Flog.i((int) DeviceStatusConstant.TYPE_HW_STEP_COUNTER, "current flashRGB no change, return.");
                return;
            }
            this.mBatteryLight.turnOff();
            Flog.i((int) DeviceStatusConstant.TYPE_HW_STEP_COUNTER, "updatelightFlashingImpl, level:" + level + ", Flashing: " + lightColor);
            if (isNotifyExist) {
                this.mBatteryLight.setFlashing(color, 1, HwBatteryService.this.mNotificationLedOn, HwBatteryService.this.mNotificationLedOff);
            } else {
                this.mBatteryLight.setFlashing(color, 1, this.mBatteryLedOn, this.mBatteryLedOff);
            }
            int unused = HwBatteryService.this.mFlashingARGB = flashRGB;
        }

        private void setLightColorImpl(int color, int level, String colorChar) {
            Flog.i((int) DeviceStatusConstant.TYPE_HW_STEP_COUNTER, "setLightColorImpl, level:" + level + ", Solid:" + colorChar);
            this.mBatteryLight.setColor(color);
            int unused = HwBatteryService.this.mFlashingARGB = 0;
        }

        private void updateLightNotificationExisted(int level, int status, boolean isSupportMedium) {
            if (level <= 4) {
                int i = this.mBatteryLowARGB;
                updatelightFlashingImpl(i, i, level, true, "red");
            } else if (status == 5 && (!HwBatteryService.this.mIsFlagScreenOn || isSupportMedium)) {
                int i2 = this.mBatteryFullARGB;
                updatelightFlashingImpl(i2, i2, level, true, "green");
            } else if (status != 2 || (HwBatteryService.this.mIsFlagScreenOn && !isSupportMedium)) {
                Flog.i((int) DeviceStatusConstant.TYPE_HW_STEP_COUNTER, "updateLightNotificationyExisted, mBatteryLight.turnOff");
                this.mBatteryLight.turnOff();
                int unused = HwBatteryService.this.mFlashingARGB = 0;
            } else if (level >= 90) {
                int i3 = this.mBatteryFullARGB;
                updatelightFlashingImpl(i3, i3, level, true, "green");
            } else if (!isSupportMedium || level < HwBatteryService.this.getLowBatteryWarningLevel()) {
                int i4 = this.mBatteryLowARGB;
                updatelightFlashingImpl(i4, i4, level, true, "red");
            } else {
                int i5 = this.mBatteryMediumARGB;
                updatelightFlashingImpl(i5, i5, level, true, "medium");
            }
        }

        private void updateLightNotificationNoExisted(int level, int status, boolean isSupportMedium) {
            if (level <= 4) {
                if (status == 2 || level > 2) {
                    setLightColorImpl(this.mBatteryLowARGB, level, "red");
                } else {
                    updatelightFlashingImpl(-1, this.mBatteryLowARGB, level, false, "red shutdown");
                }
            } else if (status == 5 && (!HwBatteryService.this.mIsFlagScreenOn || isSupportMedium)) {
                setLightColorImpl(this.mBatteryFullARGB, level, "Solid green");
            } else if (status != 2 || (HwBatteryService.this.mIsFlagScreenOn && !isSupportMedium)) {
                Flog.i((int) DeviceStatusConstant.TYPE_HW_STEP_COUNTER, "updateLightNotificationyNoExisted, mBatteryLight.turnOff");
                this.mBatteryLight.turnOff();
                int unused = HwBatteryService.this.mFlashingARGB = 0;
            } else if (level >= 90) {
                setLightColorImpl(this.mBatteryFullARGB, level, "Solid green");
            } else if (!isSupportMedium || level < HwBatteryService.this.getLowBatteryWarningLevel()) {
                setLightColorImpl(this.mBatteryLowARGB, level, "Solid red");
            } else {
                setLightColorImpl(this.mBatteryMediumARGB, level, "Solid medium");
            }
        }

        private void updateFlashColorLights(int level, int status, boolean isSupportMedium) {
            Flog.i((int) DeviceStatusConstant.TYPE_HW_STEP_COUNTER, "updateFlashColorLights, level:" + level + ", isMsgNotifyExist:" + HwBatteryService.this.mIsNotificationExisting + ", status:" + status + ", isSupportMedium:" + isSupportMedium);
            if (HwBatteryService.this.mIsNotificationExisting) {
                updateLightNotificationExisted(level, status, isSupportMedium);
            } else {
                updateLightNotificationNoExisted(level, status, isSupportMedium);
            }
        }

        public void newUpdateLightsLocked() {
            int level = HwBatteryService.this.getHealthInfo().batteryLevel;
            int status = HwBatteryService.this.getHealthInfo().batteryStatus;
            if (HwBatteryService.IS_LED_CLOSE_BY_CAMERA && HwBatteryService.this.mIsFrontCameraOpening && (status == 2 || status == 5)) {
                Flog.i((int) DeviceStatusConstant.TYPE_HW_STEP_COUNTER, "newUpdateLightsLocked, light turnOff when front camera open");
                this.mBatteryLight.turnOff();
                int unused = HwBatteryService.this.mFlashingARGB = 0;
            } else if (HwBatteryService.IS_SCREEN_ON_TURN_OFF_LED) {
                updateFlashColorLights(level, status, false);
            } else {
                updateFlashColorLights(level, status, true);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void updateLight() {
        this.mHwLed.newUpdateLightsLocked();
    }

    /* access modifiers changed from: protected */
    public void updateLight(boolean isEnable, int ledOnMS, int ledOffMS) {
        if (this.mIsNotificationExisting != isEnable || this.mNotificationLedOn != ledOnMS || this.mNotificationLedOff != ledOffMS) {
            Flog.i((int) DeviceStatusConstant.TYPE_HW_STEP_COUNTER, " updateLight --> mIsNotificationExisting : " + isEnable + " ledOnMS : " + ledOnMS + " ledOffMS : " + ledOffMS);
            this.mIsNotificationExisting = isEnable;
            this.mNotificationLedOn = ledOnMS;
            this.mNotificationLedOff = ledOffMS;
            this.mHwLed.newUpdateLightsLocked();
        }
    }

    /* access modifiers changed from: protected */
    public void cameraUpdateLight(boolean isEnable) {
        Slog.d(TAG, "cameraUpdateLight enable " + isEnable);
        if (this.mIsFrontCameraOpening != isEnable) {
            this.mIsFrontCameraOpening = isEnable;
            this.mHwLed.newUpdateLightsLocked();
        }
    }

    /* access modifiers changed from: protected */
    public void playRing() {
        HwBatteryHandler hwBatteryHandler;
        if (!FACTORY_VERSION.equalsIgnoreCase(SystemProperties.get(RUN_MODE_PROPERTY, ModelBaseService.UNKONW_IDENTIFY_RET))) {
            HwCustBatteryService hwCustBatteryService = this.mCust;
            if ((hwCustBatteryService == null || !hwCustBatteryService.mutePowerConnectedTone()) && (hwBatteryHandler = this.mHwBatteryHandler) != null) {
                hwBatteryHandler.post(new Runnable() {
                    /* class com.android.server.HwBatteryService.AnonymousClass4 */

                    public void run() {
                        boolean isWireless = true;
                        if (HwBatteryService.this.mPlugType != 1) {
                            isWireless = false;
                        }
                        String fileName = isWireless ? HwBatteryService.WIRELSSS_CONNECTED_RINGTONE : HwBatteryService.PPWER_CONNECTED_RINGTONE;
                        HwBatteryService hwBatteryService = HwBatteryService.this;
                        Uri unused = hwBatteryService.mUri = HwBatteryService.queryRingMusicUri(hwBatteryService.mContext, fileName);
                        HwBatteryService hwBatteryService2 = HwBatteryService.this;
                        Ringtone unused2 = hwBatteryService2.mRingRingtone = hwBatteryService2.playRing(hwBatteryService2.mUri, HwBatteryService.this.mRingRingtone);
                    }
                });
            }
        }
    }

    /* access modifiers changed from: protected */
    public void stopRing() {
        HwBatteryHandler hwBatteryHandler;
        if (!FACTORY_VERSION.equalsIgnoreCase(SystemProperties.get(RUN_MODE_PROPERTY, ModelBaseService.UNKONW_IDENTIFY_RET)) && (hwBatteryHandler = this.mHwBatteryHandler) != null) {
            hwBatteryHandler.post(new Runnable() {
                /* class com.android.server.HwBatteryService.AnonymousClass5 */

                public void run() {
                    HwBatteryService hwBatteryService = HwBatteryService.this;
                    hwBatteryService.stopRing(hwBatteryService.mRingRingtone);
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public Ringtone playRing(Uri uri, Ringtone ringtone) {
        if (uri == null) {
            return null;
        }
        Ringtone ringtone2 = RingtoneManager.getRingtone(this.mContext, uri);
        if (ringtone2 != null) {
            ringtone2.setAudioAttributes(this.mAudioAttributes);
            ringtone2.play();
        }
        return ringtone2;
    }

    /* access modifiers changed from: private */
    public void stopRing(Ringtone ringtone) {
        if (ringtone != null) {
            ringtone.stop();
        }
    }

    /* access modifiers changed from: private */
    public static Uri queryRingMusicUri(Context context, String fileName) {
        return queryRingMusicUri(context.getContentResolver(), fileName);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0044, code lost:
        if (r10 != null) goto L_0x0046;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0046, code lost:
        r10.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0056, code lost:
        if (0 == 0) goto L_0x0059;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0059, code lost:
        return null;
     */
    private static Uri queryRingMusicUri(ContentResolver resolver, String fileName) {
        if (fileName == null) {
            return null;
        }
        Uri uri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;
        String[] cols = {"_id"};
        Cursor cur = null;
        try {
            cur = resolver.query(uri, cols, "_data like '%" + fileName + "'", null, null);
            if (cur != null && cur.moveToFirst()) {
                Uri withAppendedId = ContentUris.withAppendedId(uri, (long) cur.getInt(cur.getColumnIndex("_id")));
                cur.close();
                return withAppendedId;
            }
        } catch (Exception e) {
            Flog.i((int) WifiProCommonUtils.RESP_CODE_REDIRECTED_HOST_CHANGED, "queryRingMusicUri Uri error!");
        } catch (Throwable th) {
            if (0 != 0) {
                cur.close();
            }
            throw th;
        }
    }

    /* access modifiers changed from: protected */
    public void printBatteryLog(HealthInfo oldInfo, HealthInfo newInfo, int oldPlugType, boolean isUpdatesStopped) {
        int plugType;
        if (oldInfo == null || newInfo == null) {
            Flog.i((int) WifiProCommonUtils.RESP_CODE_REDIRECTED_HOST_CHANGED, "mBatteryProps or new battery values is null");
            return;
        }
        if (newInfo.chargerAcOnline) {
            plugType = 1;
        } else if (newInfo.chargerUsbOnline) {
            plugType = 2;
        } else if (newInfo.chargerWirelessOnline) {
            plugType = 4;
        } else {
            plugType = 0;
        }
        if (plugType != oldPlugType || oldInfo.batteryLevel != newInfo.batteryLevel) {
            Flog.i((int) WifiProCommonUtils.RESP_CODE_REDIRECTED_HOST_CHANGED, "update battery new values: chargerAcOnline=" + newInfo.chargerAcOnline + ", chargerUsbOnline=" + newInfo.chargerUsbOnline + ", batteryStatus=" + newInfo.batteryStatus + ", batteryHealth=" + newInfo.batteryHealth + ", batteryPresent=" + newInfo.batteryPresent + ", batteryLevel=" + newInfo.batteryLevel + ", batteryTechnology=" + newInfo.batteryTechnology + ", batteryVoltage=" + newInfo.batteryVoltage + ", batteryTemperature=" + newInfo.batteryTemperature + ", mUpdatesStopped=" + isUpdatesStopped);
        }
    }

    /* access modifiers changed from: private */
    public void sendBatteryIsdcErrorBroadcast() {
        Intent iscdErrorIntent = new Intent(ACTION_BATTERY_ISCD_ERROR);
        iscdErrorIntent.addFlags(1073741824);
        iscdErrorIntent.addFlags(536870912);
        Slog.i(TAG, "Stick broadcast intent: " + iscdErrorIntent);
        this.mContext.sendStickyBroadcastAsUser(iscdErrorIntent, UserHandle.ALL);
    }

    /* access modifiers changed from: protected */
    public void startAutoPowerOff() {
        if (this.mHwAutoPowerOffController != null) {
            Slog.d(TAG, "startAutoPowerOff()");
            this.mHwAutoPowerOffController.startAutoPowerOff();
        }
    }

    /* access modifiers changed from: protected */
    public void stopAutoPowerOff() {
        if (this.mHwAutoPowerOffController != null) {
            Slog.d(TAG, "stopAutoPowerOff");
            this.mHwAutoPowerOffController.stopAutoPowerOff();
        }
    }

    /* access modifiers changed from: private */
    public void handerChargeTimeRemaining(String time) {
        int tempTime;
        int tempTime2;
        boolean tempTimeValid = false;
        if (this.mPlugType != 0) {
            try {
                tempTime = Integer.parseInt(time);
            } catch (NumberFormatException e) {
                tempTime = -1;
                Slog.w(TAG, "Not int number, Invalid value: " + -1);
            }
            if (checkChanrgeTimeValue(tempTime)) {
                tempTime2 = tempTime & CHARGE_TIME_MASK;
                tempTimeValid = true;
            } else {
                tempTime2 = -1;
            }
            if (this.mIsHwChargeTimeValid != tempTimeValid) {
                this.mIsHwChargeTimeValid = tempTimeValid;
                String timeValid = tempTimeValid ? "1" : "0";
                SystemProperties.set("persist.sys.hwChargeTime", timeValid);
                Slog.d(TAG, "SystemProperties.set " + timeValid);
            }
            Slog.d(TAG, "setHwChargeTimeRemaining, time = " + tempTime2 + " String time: " + time);
            try {
                if (getBatteryStats() != null) {
                    getBatteryStats().setHwChargeTimeRemaining((long) tempTime2);
                }
            } catch (RemoteException e2) {
                Slog.w(TAG, "setHwChargeTimeRemaining, Remote Exception.");
            }
        }
    }

    private boolean checkChanrgeTimeValue(int time) {
        if ((time >> 24) != CHARGE_TIME_VERIFY) {
            Slog.w(TAG, "need return. Invalid value: " + time);
            return false;
        } else if ((CHARGE_TIME_MASK & time) <= CHARGE_TIME_MAX_HOURS) {
            return true;
        } else {
            Slog.w(TAG, "Need return. bigger then 12 hours, Invalid value: " + time);
            return false;
        }
    }
}
