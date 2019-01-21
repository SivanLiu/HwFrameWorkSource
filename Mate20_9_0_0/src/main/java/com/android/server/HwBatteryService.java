package com.android.server;

import android.app.Notification;
import android.app.Notification.Action;
import android.app.Notification.BigTextStyle;
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
import android.hardware.health.V1_0.HealthInfo;
import android.media.AudioAttributes;
import android.media.AudioAttributes.Builder;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.os.UEventObserver.UEvent;
import android.os.UserHandle;
import android.provider.MediaStore.Audio.Media;
import android.util.Flog;
import android.util.Slog;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import com.android.server.wifipro.WifiProCommonUtils;
import com.android.systemui.shared.recents.hwutil.HwRecentsTaskUtils;
import com.huawei.msdp.devicestatus.DeviceStatusConstant;
import huawei.cust.HwCustUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class HwBatteryService extends BatteryService {
    private static final String ACTION_BATTERY_ISCD_ERROR = "huawei.intent.action.BATTERY_ISCD_ERROR";
    private static final String ACTION_CALL_BNT_CLICKED = "huawei.intent.action.CALL_BNT_CLICKED";
    private static final String ACTION_QUICK_CHARGE = "huawei.intent.action.BATTERY_QUICK_CHARGE";
    private static final String ACTION_WIRELESS_TX_CHARGE_ERROR = "huawei.intent.action.WIRELESS_TX_CHARGE_ERROR";
    private static final String ACTION_WIRELESS_TX_STATUS_CHANGE = "huawei.intent.action.WIRELESS_TX_STATUS_CHANGE";
    private static final String BATTERY_ERR_NOTIFICATION_ID = "n_id";
    private static final String BATTERY_ISCD_STATUS_ERROR = "1";
    private static final String BATTERY_ISCD_STATUS_NORMAL = "0";
    private static final String BATTERY_OVP_STATUS_ERROR = "1";
    private static final String BATTERY_OVP_STATUS_NORMAL = "0";
    private static final String FACTORY_VERSION = "factory";
    private static final String HUAWEI_CHINA_CUSTOMER_SERVICE_HOTLINE_NUMBER = "4008308300";
    public static final int LOW_BATTERY_SHUTDOWN_LEVEL = 2;
    public static final int LOW_BATTERY_WARNING_LEVEL = 4;
    private static final int MAX_BATTERY_PROP_REGISTER_TIMES = 5;
    private static final int MSG_BATTERY_ISCD_ERROR = 6;
    private static final int MSG_BATTERY_OVP_ERROR = 2;
    private static final int MSG_BATTERY_PROP_REGISTER = 1;
    private static final int MSG_UPDATE_QUICK_CHARGE_STATE = 3;
    private static final String OVP_ERR_CHANNEL_ID = "battery_error_c_id";
    private static final String PERMISSION_TX_CHARGE_ERROR = "com.huawei.batteryservice.permission.WIRELESS_TX_CHARGE_ERROR";
    private static final String PERMISSION_TX_STATUS_CHANGE = "com.huawei.batteryservice.permission.WIRELESS_TX_STATUS_CHANGE";
    private static final int PLUGGED_NONE = 0;
    private static final String PPWER_CONNECTED_RINGTONE = "PowerConnected.ogg";
    private static final String QUICK_CHARGE_FCP_STATUS = "1";
    private static final String QUICK_CHARGE_NODE_NOT_EXIST = "0";
    private static final String QUICK_CHARGE_NONE_STATUS = "0";
    private static final String QUICK_CHARGE_SCP_STATUS = "2";
    private static final String QUICK_CHARGE_STATUS_NORMAL = "0";
    private static final String QUICK_CHARGE_STATUS_WORKING = "1";
    private static final String QUICK_CHARGE_WIRELESS_STATUS = "3";
    private static final String RUN_MODE_PROPERTY = "ro.runmode";
    private static final int SHUTDOWN_LEVEL_FLASHINGARGB = -1;
    private static final String TAG = "HwBatteryService";
    private static final String WIRELESS_TX_DIR = "sys/class/hw_power/charger/wireless_tx";
    private static final int WIRELESS_TX_END = 0;
    private static final int WIRELESS_TX_FLAG_CLOSE = 2;
    private static final int WIRELESS_TX_FLAG_OPEN = 1;
    private static final int WIRELESS_TX_FLAG_UNKNOWN = 0;
    private static final String WIRELESS_TX_LOW_BATTERY = "20%";
    private static final String WIRELESS_TX_OPEN = "sys/class/hw_power/charger/wireless_tx/tx_open";
    private static final int WIRELESS_TX_START = 1;
    private static final String WIRELESS_TX_STATUS = "sys/class/hw_power/charger/wireless_tx/tx_status";
    private static final int WIRELESS_TX_SWITCH_CLOSE = 0;
    private static final int WIRELESS_TX_SWITCH_OPEN = 1;
    private static final String WIRELSSS_CONNECTED_RINGTONE = "WirelessPowerConnected.ogg";
    private static final int WL_TX_STATUS_CHARGE_DONE = 3;
    private static final int WL_TX_STATUS_DEFAULT = 0;
    private static final int WL_TX_STATUS_FAULT_BASE = 16;
    private static final int WL_TX_STATUS_IN_CHARGING = 2;
    private static final int WL_TX_STATUS_IN_WL_CHARGING = 22;
    private static final int WL_TX_STATUS_PING_SUCC = 1;
    private static final int WL_TX_STATUS_PING_TIMEOUT = 19;
    private static final int WL_TX_STATUS_RX_DISCONNECT = 18;
    private static final int WL_TX_STATUS_SOC_ERROR = 23;
    private static final int WL_TX_STATUS_TBATT_HIGH = 21;
    private static final int WL_TX_STATUS_TBATT_LOW = 20;
    private static final int WL_TX_STATUS_TX_CLOSE = 17;
    private static final boolean isChinaRegion = SystemProperties.get("ro.config.hw_optb", "0").equals("156");
    private static final boolean isLedCloseByCamera = SystemProperties.getBoolean("ro.config.led_close_by_camera", false);
    private static final boolean isScreenOnTurnOffLed = SystemProperties.getBoolean("ro.config.screenon_turnoff_led", false);
    private static final boolean isTwoColorLight = SystemProperties.getBoolean("ro.config.hw_two_color_light", false);
    private AudioAttributes mAudioAttributes;
    private final UEventObserver mBatteryFcpObserver = new UEventObserver() {
        public void onUEvent(UEvent event) {
            String fcpStatus = event.get("POWER_SUPPLY_FCP_STATUS", "0");
            String scpStatus = event.get("POWER_SUPPLY_SCP_STATUS", "0");
            String ovpStatus = event.get("POWER_SUPPLY_BAT_OVP", "0");
            String str = HwBatteryService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onUEvent fcpStatus = ");
            stringBuilder.append(fcpStatus);
            stringBuilder.append(",scpStatus =");
            stringBuilder.append(scpStatus);
            stringBuilder.append(",ovpStatus=");
            stringBuilder.append(ovpStatus);
            Slog.d(str, stringBuilder.toString());
            HwBatteryService.this.mHwBatteryHandler.updateQuickChargeState(HwBatteryService.this.getQuickChargeBroadcastStatus(fcpStatus, scpStatus));
            HwBatteryService.this.handleBatteryOvpStatus(ovpStatus);
            HwBatteryService.this.handleWirelessTxStatus();
        }
    };
    private final UEventObserver mBatteryIscdObserver = new UEventObserver() {
        public void onUEvent(UEvent event) {
            Slog.d(HwBatteryService.TAG, "onUEvent battery iscd error");
            HwBatteryService.this.mBatteryIscdStatus = "1";
            HwBatteryService.this.handleBatteryIscdStatus();
        }
    };
    private String mBatteryIscdStatus = "0";
    private String mBatteryOvpStatus = "0";
    private int mBatteryPropRegisterTryTimes = 0;
    private final Context mContext;
    private HwCustBatteryService mCust = ((HwCustBatteryService) HwCustUtils.createObj(HwCustBatteryService.class, new Object[0]));
    private boolean mFlagScreenOn = true;
    private int mFlashingARGB;
    private boolean mFrontCameraOpening = false;
    private final HwBatteryHandler mHwBatteryHandler;
    private final HandlerThread mHwBatteryThread;
    private HwLed mHwLed;
    private boolean mIsBootFinish = false;
    private boolean mIsNotificationExisting;
    private int mLastWirelessTxStatus = 0;
    private final Object mLock = new Object();
    private boolean mLowBattery = false;
    private int mNotificationLedOff;
    private int mNotificationLedOn;
    private NotificationManager mNotificationManager;
    private String mQuickChargeStatus = "0";
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (context == null || intent == null) {
                Slog.i(HwBatteryService.TAG, "context or intent is null!");
                return;
            }
            String str = HwBatteryService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("intent = ");
            stringBuilder.append(intent);
            Slog.i(str, stringBuilder.toString());
            if (HwBatteryService.ACTION_CALL_BNT_CLICKED.equals(intent.getAction())) {
                HwBatteryService.this.closeSystemDialogs(context);
                int notificationID = intent.getIntExtra(HwBatteryService.BATTERY_ERR_NOTIFICATION_ID, 0);
                if (HwBatteryService.this.mNotificationManager != null) {
                    HwBatteryService.this.mNotificationManager.cancelAsUser(null, notificationID, UserHandle.CURRENT);
                }
                Intent callIntent = new Intent("android.intent.action.CALL");
                if (HwBatteryService.isChinaRegion) {
                    callIntent.setData(Uri.parse("tel:4008308300"));
                }
                callIntent.setFlags(276824064);
                context.startActivityAsUser(callIntent, UserHandle.CURRENT);
            } else if ("android.intent.action.LOCKED_BOOT_COMPLETED".equals(intent.getAction())) {
                HwBatteryService.this.mIsBootFinish = true;
                if (!HwBatteryService.this.mBatteryIscdStatus.equals("0")) {
                    HwBatteryService.this.handleBatteryIscdStatus();
                }
            } else if ("android.intent.action.SCREEN_ON".equals(intent.getAction())) {
                HwBatteryService.this.mFlagScreenOn = true;
            } else if ("android.intent.action.SCREEN_OFF".equals(intent.getAction())) {
                HwBatteryService.this.mFlagScreenOn = false;
            }
        }
    };
    private Ringtone mRingRingtone;
    private Uri mUri;
    private int mWirelessTxFlag = 0;

    private class HealthdDeathRecipient implements DeathRecipient {
        private IBinder mCb;

        HealthdDeathRecipient(IBinder cb) {
            if (cb != null) {
                try {
                    Slog.i(HwBatteryService.TAG, "linkToDeath Healthd.");
                    cb.linkToDeath(this, 0);
                } catch (RemoteException e) {
                    String str = HwBatteryService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("HealthdDeathRecipient() could not link to ");
                    stringBuilder.append(cb);
                    stringBuilder.append(" binder death");
                    Slog.w(str, stringBuilder.toString());
                }
            }
            this.mCb = cb;
        }

        public void binderDied() {
            Slog.w(HwBatteryService.TAG, "Healthd died.");
            if (this.mCb != null) {
                this.mCb.unlinkToDeath(this, 0);
            }
            HwBatteryService.this.mBatteryPropRegisterTryTimes = 0;
            HwBatteryService.this.mHwBatteryHandler.sendEmptyMessageDelayed(1, 2000);
        }
    }

    private final class HwBatteryHandler extends Handler {
        public HwBatteryHandler(Looper looper) {
            super(looper);
            HwBatteryService.this.mBatteryIscdObserver.startObserving("BATTERY_EVENT=FATAL_ISC");
            try {
                updateQuickChargeState(HwBatteryService.this.getQuickChargeBroadcastStatus(FileUtils.readTextFile(new File(HwBatteryService.this.getQuickChargeStatePath()), 0, null).trim(), FileUtils.readTextFile(new File(HwBatteryService.this.getDCQuickChargeStatePath()), 0, null).trim()));
                HwBatteryService.this.handleBatteryOvpStatus(FileUtils.readTextFile(new File(HwBatteryService.this.getBatteryOvpStatePath()), 0, null).trim());
                HwBatteryService.this.mBatteryIscdStatus = FileUtils.readTextFile(new File(HwBatteryService.this.getBatteryIscdStatePath()), 0, null).trim();
                String str = HwBatteryService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mBatteryIscdStatus: ");
                stringBuilder.append(HwBatteryService.this.mBatteryIscdStatus);
                Slog.i(str, stringBuilder.toString());
            } catch (Exception e) {
                Slog.e(HwBatteryService.TAG, "Error get initialized state.", e);
            }
            HwBatteryService.this.mBatteryFcpObserver.startObserving("SUBSYSTEM=power_supply");
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i != 6) {
                switch (i) {
                    case 1:
                        IBinder binder = ServiceManager.checkService("batteryproperties");
                        if (binder == null || !binder.isBinderAlive()) {
                            HwBatteryService.this.mBatteryPropRegisterTryTimes = HwBatteryService.this.mBatteryPropRegisterTryTimes + 1;
                            if (HwBatteryService.this.mBatteryPropRegisterTryTimes < 5) {
                                Slog.i(HwBatteryService.TAG, "Try to get batteryproperties service again.");
                                HwBatteryService.this.mHwBatteryHandler.sendEmptyMessageDelayed(1, 2000);
                                return;
                            }
                            Slog.e(HwBatteryService.TAG, "There is no connection between batteryservice and batteryproperties.");
                            return;
                        }
                        HwBatteryService.this.mBatteryPropRegisterTryTimes = 0;
                        HealthdDeathRecipient healthdDeathRecipient = new HealthdDeathRecipient(binder);
                        HwBatteryService.this.registerHealthCallback();
                        return;
                    case 2:
                        HwBatteryService.this.sendBatteryErrorNotification(33685926, 33685927, 33751168);
                        return;
                    case 3:
                        String status = msg.obj;
                        if (!HwBatteryService.this.mQuickChargeStatus.equals(status)) {
                            HwBatteryService.this.mQuickChargeStatus = status;
                            sendQuickChargeBroadcast();
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
            HwBatteryService.this.sendBatteryErrorNotification(33685530, 33685529, 33751449);
            HwBatteryService.this.sendBatteryIsdcErrorBroadcast();
        }

        public void updateQuickChargeState(String state) {
            removeMessages(3);
            Message msg = Message.obtain(this, 3);
            msg.obj = state;
            sendMessage(msg);
        }

        private void sendQuickChargeBroadcast() {
            Intent quickChargeIntent = new Intent(HwBatteryService.ACTION_QUICK_CHARGE);
            quickChargeIntent.addFlags(1073741824);
            quickChargeIntent.addFlags(536870912);
            quickChargeIntent.putExtra("quick_charge_status", HwBatteryService.this.mQuickChargeStatus);
            String str = HwBatteryService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stick broadcast intent: ");
            stringBuilder.append(quickChargeIntent);
            stringBuilder.append(" mQuickChargeStatus:");
            stringBuilder.append(HwBatteryService.this.mQuickChargeStatus);
            Slog.i(str, stringBuilder.toString());
            HwBatteryService.this.mContext.sendStickyBroadcastAsUser(quickChargeIntent, UserHandle.ALL);
        }
    }

    private final class HwLed {
        private final int mBatteryFullARGB;
        private final int mBatteryLedOff;
        private final int mBatteryLedOn;
        private final Light mBatteryLight;
        private final int mBatteryLowARGB;
        private final int mBatteryMediumARGB;

        public HwLed(Context context, LightsManager lights) {
            this.mBatteryLight = lights.getLight(3);
            this.mBatteryLowARGB = context.getResources().getInteger(17694840);
            if (HwBatteryService.isTwoColorLight) {
                this.mBatteryMediumARGB = context.getResources().getInteger(17694840);
            } else {
                this.mBatteryMediumARGB = context.getResources().getInteger(17694841);
            }
            this.mBatteryFullARGB = context.getResources().getInteger(17694837);
            this.mBatteryLedOn = context.getResources().getInteger(17694839);
            this.mBatteryLedOff = context.getResources().getInteger(17694838);
        }

        private void removeBatteryMediumLights(int level, int status) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeBatteryMediumLights --> level:");
            stringBuilder.append(level);
            stringBuilder.append(", status:");
            stringBuilder.append(status);
            Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, stringBuilder.toString());
            if (HwBatteryService.this.mIsNotificationExisting) {
                if (level <= 4) {
                    if (status == 2) {
                        if (HwBatteryService.this.mFlashingARGB != this.mBatteryLowARGB) {
                            this.mBatteryLight.turnOff();
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("updateLightsLocked --> level:");
                            stringBuilder.append(HwBatteryService.this.getHealthInfo().batteryLevel);
                            stringBuilder.append("-- Flashing red");
                            Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, stringBuilder.toString());
                            this.mBatteryLight.setFlashing(this.mBatteryLowARGB, 1, HwBatteryService.this.mNotificationLedOn, HwBatteryService.this.mNotificationLedOff);
                            HwBatteryService.this.mFlashingARGB = this.mBatteryLowARGB;
                        }
                    } else if (level <= 4 && level > 2) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("updateLightsLocked --> level:");
                        stringBuilder.append(HwBatteryService.this.getHealthInfo().batteryLevel);
                        stringBuilder.append(" -- Solid red");
                        Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, stringBuilder.toString());
                        this.mBatteryLight.setColor(this.mBatteryLowARGB);
                        HwBatteryService.this.mFlashingARGB = 0;
                    } else if (level <= 2 && HwBatteryService.this.mFlashingARGB != -1) {
                        this.mBatteryLight.turnOff();
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("updateLightsLocked --> level:");
                        stringBuilder.append(HwBatteryService.this.getHealthInfo().batteryLevel);
                        stringBuilder.append("--Flashing red SHUTDOWN_LEVEL");
                        Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, stringBuilder.toString());
                        this.mBatteryLight.setFlashing(this.mBatteryLowARGB, 1, this.mBatteryLedOn, this.mBatteryLedOff);
                        HwBatteryService.this.mFlashingARGB = -1;
                    }
                } else if (status != 2 && status != 5) {
                    Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, "updateLightsLocked --> mBatteryLight.turnOff");
                    this.mBatteryLight.turnOff();
                    HwBatteryService.this.mFlashingARGB = 0;
                } else if (HwBatteryService.this.mFlagScreenOn) {
                    Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, "updateLightsLocked --> mBatteryLight.turnOff");
                    this.mBatteryLight.turnOff();
                    HwBatteryService.this.mFlashingARGB = 0;
                } else if (status == 5 || level >= 90) {
                    if (HwBatteryService.this.mFlashingARGB != this.mBatteryFullARGB) {
                        this.mBatteryLight.turnOff();
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("updateLightsLocked --> level:");
                        stringBuilder.append(HwBatteryService.this.getHealthInfo().batteryLevel);
                        stringBuilder.append("-- Flashing green");
                        Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, stringBuilder.toString());
                        this.mBatteryLight.setFlashing(this.mBatteryFullARGB, 1, HwBatteryService.this.mNotificationLedOn, HwBatteryService.this.mNotificationLedOff);
                        HwBatteryService.this.mFlashingARGB = this.mBatteryFullARGB;
                    }
                } else if (HwBatteryService.this.mFlashingARGB != this.mBatteryLowARGB) {
                    this.mBatteryLight.turnOff();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("updateLightsLocked --> level:");
                    stringBuilder.append(HwBatteryService.this.getHealthInfo().batteryLevel);
                    stringBuilder.append("-- Flashing red");
                    Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, stringBuilder.toString());
                    this.mBatteryLight.setFlashing(this.mBatteryLowARGB, 1, HwBatteryService.this.mNotificationLedOn, HwBatteryService.this.mNotificationLedOff);
                    HwBatteryService.this.mFlashingARGB = this.mBatteryLowARGB;
                }
            } else if (level <= 4) {
                if (status == 2) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("updateLightsLocked --> level:");
                    stringBuilder.append(HwBatteryService.this.getHealthInfo().batteryLevel);
                    stringBuilder.append("-- Solid red");
                    Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, stringBuilder.toString());
                    this.mBatteryLight.setColor(this.mBatteryLowARGB);
                    HwBatteryService.this.mFlashingARGB = 0;
                } else if (level <= 4 && level > 2) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("updateLightsLocked --> level:");
                    stringBuilder.append(HwBatteryService.this.getHealthInfo().batteryLevel);
                    stringBuilder.append(" -- Solid red");
                    Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, stringBuilder.toString());
                    this.mBatteryLight.setColor(this.mBatteryLowARGB);
                    HwBatteryService.this.mFlashingARGB = 0;
                } else if (level <= 2 && HwBatteryService.this.mFlashingARGB != -1) {
                    this.mBatteryLight.turnOff();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("updateLightsLocked --> level:");
                    stringBuilder.append(HwBatteryService.this.getHealthInfo().batteryLevel);
                    stringBuilder.append("--Flashing red SHUTDOWN_LEVEL");
                    Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, stringBuilder.toString());
                    this.mBatteryLight.setFlashing(this.mBatteryLowARGB, 1, this.mBatteryLedOn, this.mBatteryLedOff);
                    HwBatteryService.this.mFlashingARGB = -1;
                }
            } else if (status != 2 && status != 5) {
                Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, "updateLightsLocked --> mBatteryLight.turnOff");
                this.mBatteryLight.turnOff();
                HwBatteryService.this.mFlashingARGB = 0;
            } else if (HwBatteryService.this.mFlagScreenOn) {
                Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, "updateLightsLocked --> mBatteryLight.turnOff");
                this.mBatteryLight.turnOff();
                HwBatteryService.this.mFlashingARGB = 0;
            } else if (status == 5 || level >= 90) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("updateLightsLocked --> level:");
                stringBuilder.append(HwBatteryService.this.getHealthInfo().batteryLevel);
                stringBuilder.append("-- Solid green");
                Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, stringBuilder.toString());
                this.mBatteryLight.setColor(this.mBatteryFullARGB);
                HwBatteryService.this.mFlashingARGB = 0;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("updateLightsLocked --> level:");
                stringBuilder.append(HwBatteryService.this.getHealthInfo().batteryLevel);
                stringBuilder.append(" -- Solid red");
                Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, stringBuilder.toString());
                this.mBatteryLight.setColor(this.mBatteryLowARGB);
                HwBatteryService.this.mFlashingARGB = 0;
            }
        }

        public void newUpdateLightsLocked() {
            int level = HwBatteryService.this.getHealthInfo().batteryLevel;
            int status = HwBatteryService.this.getHealthInfo().batteryStatus;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateLightsLocked --> level:");
            stringBuilder.append(level);
            stringBuilder.append(", status:");
            stringBuilder.append(status);
            stringBuilder.append(" mFrontCameraOpening ");
            stringBuilder.append(HwBatteryService.this.mFrontCameraOpening);
            Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, stringBuilder.toString());
            if (HwBatteryService.isLedCloseByCamera && ((status == 2 || status == 5) && HwBatteryService.this.mFrontCameraOpening)) {
                Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, "updateLightsLocked --> mBatteryLight.turnOff front camera open");
                this.mBatteryLight.turnOff();
                HwBatteryService.this.mFlashingARGB = 0;
            } else if (HwBatteryService.isScreenOnTurnOffLed) {
                removeBatteryMediumLights(level, status);
            } else if (HwBatteryService.this.mIsNotificationExisting) {
                if (level < HwBatteryService.this.getLowBatteryWarningLevel()) {
                    if (status == 2) {
                        if (HwBatteryService.this.mFlashingARGB != this.mBatteryLowARGB) {
                            this.mBatteryLight.turnOff();
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("updateLightsLocked --> level:");
                            stringBuilder.append(HwBatteryService.this.getHealthInfo().batteryLevel);
                            stringBuilder.append("-- Flashing red");
                            Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, stringBuilder.toString());
                            this.mBatteryLight.setFlashing(this.mBatteryLowARGB, 1, HwBatteryService.this.mNotificationLedOn, HwBatteryService.this.mNotificationLedOff);
                            HwBatteryService.this.mFlashingARGB = this.mBatteryLowARGB;
                        }
                    } else if (level <= 4 && level > 2) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("updateLightsLocked --> level:");
                        stringBuilder.append(HwBatteryService.this.getHealthInfo().batteryLevel);
                        stringBuilder.append("-- Solid red");
                        Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, stringBuilder.toString());
                        this.mBatteryLight.setColor(this.mBatteryLowARGB);
                        HwBatteryService.this.mFlashingARGB = 0;
                    } else if (level > 2) {
                        Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, "updateLightsLocked --> mBatteryLight.turnOff");
                        this.mBatteryLight.turnOff();
                        HwBatteryService.this.mFlashingARGB = 0;
                    } else if (HwBatteryService.this.mFlashingARGB != -1) {
                        this.mBatteryLight.turnOff();
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("updateLightsLocked --> level:");
                        stringBuilder.append(HwBatteryService.this.getHealthInfo().batteryLevel);
                        stringBuilder.append("--Flashing red SHUTDOWN_LEVEL");
                        Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, stringBuilder.toString());
                        this.mBatteryLight.setFlashing(this.mBatteryLowARGB, 1, this.mBatteryLedOn, this.mBatteryLedOff);
                        HwBatteryService.this.mFlashingARGB = -1;
                    }
                } else if (status != 2 && status != 5) {
                    Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, "updateLightsLocked --> mBatteryLight.turnOff");
                    this.mBatteryLight.turnOff();
                    HwBatteryService.this.mFlashingARGB = 0;
                } else if (status == 5 || level >= 90) {
                    if (HwBatteryService.this.mFlashingARGB != this.mBatteryFullARGB) {
                        this.mBatteryLight.turnOff();
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("updateLightsLocked --> level:");
                        stringBuilder.append(HwBatteryService.this.getHealthInfo().batteryLevel);
                        stringBuilder.append("-- Flashing green");
                        Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, stringBuilder.toString());
                        this.mBatteryLight.setFlashing(this.mBatteryFullARGB, 1, HwBatteryService.this.mNotificationLedOn, HwBatteryService.this.mNotificationLedOff);
                        HwBatteryService.this.mFlashingARGB = this.mBatteryFullARGB;
                    }
                } else if (HwBatteryService.this.mFlashingARGB != this.mBatteryMediumARGB) {
                    this.mBatteryLight.turnOff();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("updateLightsLocked --> level:");
                    stringBuilder.append(HwBatteryService.this.getHealthInfo().batteryLevel);
                    stringBuilder.append("-- Flashing mBatteryMediumARGB");
                    Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, stringBuilder.toString());
                    this.mBatteryLight.setFlashing(this.mBatteryMediumARGB, 1, HwBatteryService.this.mNotificationLedOn, HwBatteryService.this.mNotificationLedOff);
                    HwBatteryService.this.mFlashingARGB = this.mBatteryMediumARGB;
                }
            } else if (level < HwBatteryService.this.getLowBatteryWarningLevel()) {
                if (status == 2) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("updateLightsLocked --> level:");
                    stringBuilder.append(HwBatteryService.this.getHealthInfo().batteryLevel);
                    stringBuilder.append("-- Solid red");
                    Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, stringBuilder.toString());
                    this.mBatteryLight.setColor(this.mBatteryLowARGB);
                    HwBatteryService.this.mFlashingARGB = 0;
                } else if (level <= 4 && level > 2) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("updateLightsLocked --> level:");
                    stringBuilder.append(HwBatteryService.this.getHealthInfo().batteryLevel);
                    stringBuilder.append("-- Solid red");
                    Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, stringBuilder.toString());
                    this.mBatteryLight.setColor(this.mBatteryLowARGB);
                    HwBatteryService.this.mFlashingARGB = 0;
                } else if (level > 2) {
                    Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, "updateLightsLocked --> mBatteryLight.turnOff");
                    this.mBatteryLight.turnOff();
                    HwBatteryService.this.mFlashingARGB = 0;
                } else if (HwBatteryService.this.mFlashingARGB != -1) {
                    this.mBatteryLight.turnOff();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("updateLightsLocked --> level:");
                    stringBuilder.append(HwBatteryService.this.getHealthInfo().batteryLevel);
                    stringBuilder.append("-- Flashing red SHUTDOWN_LEVEL");
                    Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, stringBuilder.toString());
                    this.mBatteryLight.setFlashing(this.mBatteryLowARGB, 1, this.mBatteryLedOn, this.mBatteryLedOff);
                    HwBatteryService.this.mFlashingARGB = -1;
                }
            } else if (status != 2 && status != 5) {
                Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, "updateLightsLocked --> mBatteryLight.turnOff");
                this.mBatteryLight.turnOff();
                HwBatteryService.this.mFlashingARGB = 0;
            } else if (status == 5 || level >= 90) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("updateLightsLocked --> level:");
                stringBuilder.append(HwBatteryService.this.getHealthInfo().batteryLevel);
                stringBuilder.append("-- Solid green");
                Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, stringBuilder.toString());
                this.mBatteryLight.setColor(this.mBatteryFullARGB);
                HwBatteryService.this.mFlashingARGB = 0;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("updateLightsLocked --> level:");
                stringBuilder.append(HwBatteryService.this.getHealthInfo().batteryLevel);
                stringBuilder.append(" -- Solid mBatteryMediumARGB");
                Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, stringBuilder.toString());
                this.mBatteryLight.setColor(this.mBatteryMediumARGB);
                HwBatteryService.this.mFlashingARGB = 0;
            }
        }
    }

    public HwBatteryService(Context context) {
        super(context);
        this.mContext = context;
        initAndRegisterReceiver();
        this.mHwBatteryThread = new HandlerThread(TAG);
        this.mHwBatteryThread.start();
        this.mHwBatteryHandler = new HwBatteryHandler(this.mHwBatteryThread.getLooper());
        this.mHwLed = new HwLed(context, (LightsManager) getLocalService(LightsManager.class));
        this.mAudioAttributes = new Builder().setUsage(13).setContentType(4).build();
    }

    private boolean writeFile(String path, String data) {
        FileOutputStream fos = null;
        IOException e2;
        String str;
        StringBuilder stringBuilder;
        try {
            fos = new FileOutputStream(path);
            fos.write(data.getBytes());
            try {
                fos.close();
                return true;
            } catch (IOException e) {
                e2 = e;
                str = TAG;
                stringBuilder = new StringBuilder();
            }
            stringBuilder.append("closeFile ");
            stringBuilder.append(path);
            Slog.e(str, stringBuilder.toString(), e2);
            return false;
        } catch (IOException e22) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("writeFile ");
            stringBuilder.append(path);
            Slog.e(str, stringBuilder.toString(), e22);
            if (fos == null) {
                return false;
            }
            try {
                fos.close();
                return false;
            } catch (IOException e3) {
                e22 = e3;
                str = TAG;
                stringBuilder = new StringBuilder();
            }
        } catch (Throwable th) {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e23) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("closeFile ");
                    stringBuilder.append(path);
                    Slog.e(TAG, stringBuilder.toString(), e23);
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:28:0x009d, code skipped:
            return 0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected int alterWirelessTxSwitchInternal(int status) {
        String str;
        if (status == 1 || status == 0) {
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("alterWirelessTxStatus status : ");
            stringBuilder.append(status);
            Slog.i(str2, stringBuilder.toString());
            synchronized (this.mLock) {
                this.mWirelessTxFlag = 0;
                this.mLastWirelessTxStatus = 0;
                if (writeFile(WIRELESS_TX_OPEN, String.valueOf(status))) {
                    try {
                        this.mLock.wait(100);
                    } catch (InterruptedException ie) {
                        Slog.e(TAG, "Error occurs when sleep", ie);
                    }
                    int wirelessTxStatus = getWirelessTxStatus(WIRELESS_TX_STATUS);
                    if (!allowWirelessTxSwitch(status, wirelessTxStatus)) {
                        str = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("not allowed to open tx wireless : ");
                        stringBuilder2.append(wirelessTxStatus);
                        Slog.i(str, stringBuilder2.toString());
                        writeFile(WIRELESS_TX_OPEN, String.valueOf(0));
                        return wirelessTxStatus;
                    } else if (status == 1) {
                        this.mWirelessTxFlag = 1;
                    } else {
                        sendWirelessTxStatusChangeBroadcast(0);
                        this.mWirelessTxFlag = 2;
                    }
                } else {
                    Slog.e(TAG, "writeFile error sys/class/hw_power/charger/wireless_tx/tx_open");
                    return -1;
                }
            }
        }
        str = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("alterWirelessTxStatus status error : ");
        stringBuilder3.append(status);
        Slog.w(str, stringBuilder3.toString());
        return -1;
    }

    protected int getWirelessTxSwitchInternal() {
        return getWirelessTxStatus(WIRELESS_TX_OPEN);
    }

    protected boolean supportWirelessTxChargeInternal() {
        File wirelessTxDir = new File(WIRELESS_TX_DIR);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("exists : ");
        stringBuilder.append(wirelessTxDir.exists());
        Slog.i(str, stringBuilder.toString());
        return wirelessTxDir.exists() && wirelessTxDir.isDirectory();
    }

    private int getWirelessTxStatus(String wirelessTxPath) {
        String str;
        StringBuilder stringBuilder;
        int result = 0;
        String txStatus = null;
        try {
            txStatus = FileUtils.readTextFile(new File(wirelessTxPath), 0, null).trim();
            result = Integer.parseInt(txStatus);
        } catch (IOException ioe) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error occurs when read ");
            stringBuilder.append(wirelessTxPath);
            Slog.e(str, stringBuilder.toString(), ioe);
        } catch (NumberFormatException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error occurs when translate status : ");
            stringBuilder.append(txStatus);
            Slog.e(str, stringBuilder.toString());
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getWirelessTxStatus , ");
        stringBuilder2.append(wirelessTxPath);
        stringBuilder2.append(" : ");
        stringBuilder2.append(result);
        Slog.i(str2, stringBuilder2.toString());
        return result;
    }

    private boolean isWirelessTxNormal(int status) {
        switch (status) {
            case 1:
            case 2:
                return true;
            default:
                return false;
        }
    }

    private boolean isWirelessTxError(int status) {
        if (status != 3) {
            switch (status) {
                case 16:
                case 17:
                    break;
                default:
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
        }
        return true;
    }

    private boolean isWirelessTxDisconnect(int status) {
        return status == 18;
    }

    private int getWirelessTxErrorRes(int status) {
        this.mLowBattery = false;
        if (status == 3) {
            return 33686202;
        }
        if (status != 23) {
            switch (status) {
                case 19:
                    return 33686204;
                case 20:
                    return 33686206;
                case 21:
                    return 33686205;
                default:
                    return -1;
            }
        }
        this.mLowBattery = true;
        return 33686203;
    }

    private boolean allowWirelessTxSwitch(int switchStatus, int wirelessTxStatus) {
        if (switchStatus == 0) {
            return true;
        }
        boolean result = true;
        switch (wirelessTxStatus) {
            case 20:
            case 21:
            case 22:
            case 23:
                result = false;
                break;
        }
        return result;
    }

    /* JADX WARNING: Missing block: B:16:0x0059, code skipped:
            if (isWirelessTxNormal(r0) == false) goto L_0x005f;
     */
    /* JADX WARNING: Missing block: B:17:0x005b, code skipped:
            sendWirelessTxStatusChangeBroadcast(1);
     */
    /* JADX WARNING: Missing block: B:19:0x0063, code skipped:
            if (isWirelessTxError(r0) == false) goto L_0x006c;
     */
    /* JADX WARNING: Missing block: B:20:0x0065, code skipped:
            sendWirelessTxChargeErrorBroadcast();
            sendWirelessTxErrorNotification(r0);
     */
    /* JADX WARNING: Missing block: B:22:0x0070, code skipped:
            if (isWirelessTxDisconnect(r0) == false) goto L_0x0076;
     */
    /* JADX WARNING: Missing block: B:23:0x0072, code skipped:
            sendWirelessTxStatusChangeBroadcast(0);
     */
    /* JADX WARNING: Missing block: B:24:0x0076, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleWirelessTxStatus() {
        Slog.i(TAG, "handleWirelessTxStatus");
        synchronized (this.mLock) {
            String str;
            StringBuilder stringBuilder;
            if (this.mWirelessTxFlag != 1) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("switch not open, return, mWirelessTxFlag : ");
                stringBuilder.append(this.mWirelessTxFlag);
                Slog.i(str, stringBuilder.toString());
                return;
            }
            int wirelessTxStatus = getWirelessTxStatus(WIRELESS_TX_STATUS);
            if (wirelessTxStatus == this.mLastWirelessTxStatus) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("wireless_tx_status not changed, return, mLastWirelessTxStatus : ");
                stringBuilder.append(this.mLastWirelessTxStatus);
                Slog.i(str, stringBuilder.toString());
                return;
            }
            this.mLastWirelessTxStatus = wirelessTxStatus;
        }
    }

    private void sendWirelessTxStatusChangeBroadcast(int status) {
        Intent intent = new Intent(ACTION_WIRELESS_TX_STATUS_CHANGE);
        intent.addFlags(1073741824);
        intent.addFlags(536870912);
        intent.putExtra("status", status);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendWirelessTxStatusChangeBroadcast status : ");
        stringBuilder.append(status);
        Slog.i(str, stringBuilder.toString());
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
            if (this.mLowBattery) {
                message = String.format(message, new Object[]{WIRELESS_TX_LOW_BATTERY});
            }
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(HwRecentsTaskUtils.PKG_SYS_MANAGER, "com.huawei.systemmanager.power.ui.HwPowerManagerActivity"));
            PendingIntent pi = PendingIntent.getActivityAsUser(this.mContext, 0, intent, 0, null, UserHandle.CURRENT);
            makeNotificationChannel(message);
            Notification notification = new Notification.Builder(this.mContext, OVP_ERR_CHANNEL_ID).setSmallIcon(33752052).setContentText(message).setContentIntent(pi).setVibrate(new long[0]).setPriority(2).setWhen(System.currentTimeMillis()).setShowWhen(true).setVisibility(1).setStyle(new BigTextStyle().bigText(message)).setAutoCancel(true).build();
            if (this.mNotificationManager != null) {
                this.mNotificationManager.notifyAsUser(null, messageId, notification, UserHandle.ALL);
            }
        }
    }

    public void onStart() {
        super.onStart();
        HealthdDeathRecipient healthdDeathRecipient = new HealthdDeathRecipient(ServiceManager.getService("batteryproperties"));
    }

    private String getQuickChargeStatePath() {
        return "/sys/class/power_supply/Battery/fcp_status";
    }

    private String getDCQuickChargeStatePath() {
        return "/sys/class/power_supply/Battery/scp_status";
    }

    private String getBatteryOvpStatePath() {
        return "/sys/class/power_supply/Battery/bat_ovp";
    }

    private String getBatteryIscdStatePath() {
        return "/sys/class/hw_power/battery/isc";
    }

    private void initAndRegisterReceiver() {
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CALL_BNT_CLICKED);
        filter.addAction("android.intent.action.LOCKED_BOOT_COMPLETED");
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        this.mContext.registerReceiver(this.mReceiver, filter, "android.permission.DEVICE_POWER", null);
    }

    private void closeSystemDialogs(Context context) {
        context.sendBroadcastAsUser(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"), UserHandle.ALL);
    }

    private void makeNotificationChannel(String name) {
        if (this.mNotificationManager != null) {
            this.mNotificationManager.createNotificationChannel(new NotificationChannel(OVP_ERR_CHANNEL_ID, name, 4));
        }
    }

    private void sendBatteryErrorNotification(int titleId, int messageId, int iconId) {
        Slog.i(TAG, "sendBatteryErrorNotification");
        String title = this.mContext.getResources().getString(titleId);
        CharSequence message = this.mContext.getResources().getString(messageId);
        Intent dialIntent = new Intent("android.intent.action.DIAL");
        if (isChinaRegion) {
            dialIntent.setData(Uri.parse("tel:4008308300"));
        }
        PendingIntent pi = PendingIntent.getActivityAsUser(this.mContext, 0, dialIntent, 0, null, UserHandle.CURRENT);
        Intent intent = new Intent(ACTION_CALL_BNT_CLICKED);
        intent.putExtra(BATTERY_ERR_NOTIFICATION_ID, titleId);
        intent.setPackage(this.mContext.getPackageName());
        PendingIntent actionClickPI = PendingIntent.getBroadcast(this.mContext, 0, intent, 0);
        makeNotificationChannel(title);
        Notification.Builder build = new Notification.Builder(this.mContext, OVP_ERR_CHANNEL_ID).setSmallIcon(iconId).setOngoing(true).setContentTitle(title).setContentText(message).setContentIntent(pi).setTicker(title).setVibrate(new long[0]).setPriority(2).setWhen(System.currentTimeMillis()).setShowWhen(true).setVisibility(1).setAutoCancel(true);
        if (isChinaRegion) {
            build.addAction(new Action.Builder(null, this.mContext.getString(33685928), actionClickPI).build());
        }
        Notification notification = build.build();
        if (this.mNotificationManager != null) {
            this.mNotificationManager.notifyAsUser(null, titleId, notification, UserHandle.ALL);
        }
    }

    private void handleBatteryOvpStatus(String status) {
        if (!this.mBatteryOvpStatus.equals(status) && "1".equals(status)) {
            Slog.i(TAG, "battery ovp error occur, send notification before.");
            this.mBatteryOvpStatus = status;
            if (this.mHwBatteryHandler.hasMessages(2)) {
                this.mHwBatteryHandler.removeMessages(2);
            }
            this.mHwBatteryHandler.sendMessage(Message.obtain(this.mHwBatteryHandler, 2, status));
        }
    }

    private void handleBatteryIscdStatus() {
        if (this.mIsBootFinish) {
            if (this.mHwBatteryHandler.hasMessages(6)) {
                this.mHwBatteryHandler.removeMessages(6);
            }
            this.mHwBatteryHandler.sendMessage(Message.obtain(this.mHwBatteryHandler, 6));
        }
    }

    private String getQuickChargeBroadcastStatus(String fcpStatus, String scpStatus) {
        String status = "0";
        if (isWirelessCharge()) {
            if (fcpStatus.equals("1")) {
                status = "3";
            }
        } else if (fcpStatus.equals("1") && scpStatus.equals("1")) {
            status = "2";
        } else if (fcpStatus.equals("1") && scpStatus.equals("0")) {
            status = "1";
        } else if (fcpStatus.equals("0") && scpStatus.equals("1")) {
            status = "2";
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("quick charge status : ");
        stringBuilder.append(status);
        Slog.i(str, stringBuilder.toString());
        return status;
    }

    protected void updateLight() {
        this.mHwLed.newUpdateLightsLocked();
    }

    protected void updateLight(boolean enable, int ledOnMS, int ledOffMS) {
        this.mIsNotificationExisting = enable;
        if (enable) {
            this.mNotificationLedOn = ledOnMS;
            this.mNotificationLedOff = ledOffMS;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateLight --> mIsNotificationExisting: ");
        stringBuilder.append(enable);
        stringBuilder.append(" ledOnMS: ");
        stringBuilder.append(ledOnMS);
        stringBuilder.append(" ledOffMS :");
        stringBuilder.append(ledOffMS);
        Flog.i(DeviceStatusConstant.TYPE_HW_STEP_COUNTER, stringBuilder.toString());
        this.mHwLed.newUpdateLightsLocked();
    }

    protected void cameraUpdateLight(boolean enable) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("cameraUpdateLight enable ");
        stringBuilder.append(enable);
        Slog.d(str, stringBuilder.toString());
        if (this.mFrontCameraOpening != enable) {
            this.mFrontCameraOpening = enable;
            this.mHwLed.newUpdateLightsLocked();
        }
    }

    protected void playRing() {
        if (!FACTORY_VERSION.equalsIgnoreCase(SystemProperties.get(RUN_MODE_PROPERTY, "unknown"))) {
            if (this.mCust == null || !this.mCust.mutePowerConnectedTone()) {
                this.mHwBatteryHandler.post(new Runnable() {
                    public void run() {
                        boolean z = true;
                        if (HwBatteryService.this.mPlugType != 1) {
                            z = false;
                        }
                        HwBatteryService.this.mUri = HwBatteryService.queryRingMusicUri(HwBatteryService.this.mContext, z ? HwBatteryService.WIRELSSS_CONNECTED_RINGTONE : HwBatteryService.PPWER_CONNECTED_RINGTONE);
                        HwBatteryService.this.mRingRingtone = HwBatteryService.this.playRing(HwBatteryService.this.mUri, HwBatteryService.this.mRingRingtone);
                    }
                });
            }
        }
    }

    protected void stopRing() {
        if (!FACTORY_VERSION.equalsIgnoreCase(SystemProperties.get(RUN_MODE_PROPERTY, "unknown"))) {
            this.mHwBatteryHandler.post(new Runnable() {
                public void run() {
                    HwBatteryService.this.stopRing(HwBatteryService.this.mRingRingtone);
                }
            });
        }
    }

    private Ringtone playRing(Uri uri, Ringtone ringtone) {
        ringtone = RingtoneManager.getRingtone(this.mContext, uri);
        if (ringtone != null) {
            ringtone.setAudioAttributes(this.mAudioAttributes);
            ringtone.play();
        }
        return ringtone;
    }

    private void stopRing(Ringtone ringtone) {
        if (ringtone != null) {
            ringtone.stop();
        }
    }

    private static Uri queryRingMusicUri(Context context, String fileName) {
        return queryRingMusicUri(context.getContentResolver(), fileName);
    }

    /* JADX WARNING: Missing block: B:13:0x0047, code skipped:
            if (r9 != null) goto L_0x0049;
     */
    /* JADX WARNING: Missing block: B:14:0x0049, code skipped:
            r9.close();
     */
    /* JADX WARNING: Missing block: B:20:0x0055, code skipped:
            if (r9 == null) goto L_0x0058;
     */
    /* JADX WARNING: Missing block: B:21:0x0058, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static Uri queryRingMusicUri(ContentResolver resolver, String fileName) {
        if (fileName == null) {
            return null;
        }
        Uri uri = Media.INTERNAL_CONTENT_URI;
        String[] cols = new String[]{"_id"};
        StringBuilder where = new StringBuilder("_data like '%");
        where.append(fileName);
        where.append("'");
        Cursor cur = null;
        try {
            cur = resolver.query(uri, cols, where.toString(), null, null);
            if (cur != null && cur.moveToFirst()) {
                Uri withAppendedId = ContentUris.withAppendedId(uri, (long) cur.getInt(cur.getColumnIndex("_id")));
                if (cur != null) {
                    cur.close();
                }
                return withAppendedId;
            }
        } catch (Exception e) {
        } catch (Throwable th) {
            if (cur != null) {
                cur.close();
            }
        }
    }

    protected void printBatteryLog(HealthInfo oldInfo, HealthInfo newInfo, int oldPlugType, boolean updatesStopped) {
        if (oldInfo == null || newInfo == null) {
            Flog.i(WifiProCommonUtils.RESP_CODE_REDIRECTED_HOST_CHANGED, "mBatteryProps or new battery values is null");
            return;
        }
        int plugType;
        if (newInfo.chargerAcOnline) {
            plugType = 1;
        } else if (newInfo.chargerUsbOnline) {
            plugType = 2;
        } else if (newInfo.chargerWirelessOnline) {
            plugType = 4;
        } else {
            plugType = 0;
        }
        if (!(plugType == oldPlugType && oldInfo.batteryLevel == newInfo.batteryLevel)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("update battery new values: chargerAcOnline=");
            stringBuilder.append(newInfo.chargerAcOnline);
            stringBuilder.append(", chargerUsbOnline=");
            stringBuilder.append(newInfo.chargerUsbOnline);
            stringBuilder.append(", batteryStatus=");
            stringBuilder.append(newInfo.batteryStatus);
            stringBuilder.append(", batteryHealth=");
            stringBuilder.append(newInfo.batteryHealth);
            stringBuilder.append(", batteryPresent=");
            stringBuilder.append(newInfo.batteryPresent);
            stringBuilder.append(", batteryLevel=");
            stringBuilder.append(newInfo.batteryLevel);
            stringBuilder.append(", batteryTechnology=");
            stringBuilder.append(newInfo.batteryTechnology);
            stringBuilder.append(", batteryVoltage=");
            stringBuilder.append(newInfo.batteryVoltage);
            stringBuilder.append(", batteryTemperature=");
            stringBuilder.append(newInfo.batteryTemperature);
            stringBuilder.append(", mUpdatesStopped=");
            stringBuilder.append(updatesStopped);
            Flog.i(WifiProCommonUtils.RESP_CODE_REDIRECTED_HOST_CHANGED, stringBuilder.toString());
        }
    }

    private void sendBatteryIsdcErrorBroadcast() {
        Intent iscdErrorIntent = new Intent(ACTION_BATTERY_ISCD_ERROR);
        iscdErrorIntent.addFlags(1073741824);
        iscdErrorIntent.addFlags(536870912);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Stick broadcast intent: ");
        stringBuilder.append(iscdErrorIntent);
        Slog.i(str, stringBuilder.toString());
        this.mContext.sendStickyBroadcastAsUser(iscdErrorIntent, UserHandle.ALL);
    }
}
