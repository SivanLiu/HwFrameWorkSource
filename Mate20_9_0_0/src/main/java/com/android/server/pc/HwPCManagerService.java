package com.android.server.pc;

import android.app.ActivityManager;
import android.app.ActivityManager.TaskDescription;
import android.app.ActivityManager.TaskSnapshot;
import android.app.AlarmManager;
import android.app.AlarmManager.OnAlarmListener;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.HwRecentTaskInfo;
import android.app.ITaskStackListener;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.Notification.Action;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackListener;
import android.app.UserSwitchObserver;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.display.WifiDisplayStatus;
import android.hardware.input.InputManager;
import android.hardware.input.InputManager.InputDeviceListener;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.os.UEventObserver.UEvent;
import android.os.UserHandle;
import android.os.UserManagerInternal;
import android.os.storage.StorageManager;
import android.pc.IHwPCManager.Stub;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.HwPCUtils;
import android.util.HwPCUtils.ProjectionMode;
import android.util.HwVRUtils;
import android.util.Log;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.SurfaceControl;
import android.view.View;
import android.view.Window;
import android.view.WindowManagerPolicyConstants.PointerEventListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.UiThread;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.HwActivityManagerService;
import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.input.HwInputManagerService.HwInputManagerLocalService;
import com.android.server.mtm.iaware.brjob.AwareJobSchedulerConstants;
import com.android.server.pc.HwPCMultiDisplaysManager.CastingDisplay;
import com.android.server.pc.decision.DecisionUtil;
import com.android.server.pc.vassist.HwPCVAssistCmdExecutor;
import com.android.server.pc.vassist.HwPCVAssistCmdExecutor.WindowStateData;
import com.android.server.pc.whiltestrategy.WhiteListAppStrategyManager;
import com.android.server.policy.HwPhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.rms.iaware.appmng.AwareIntelligentRecg;
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.android.server.statusbar.StatusBarManagerService;
import com.android.server.wm.WindowManagerInternal;
import com.android.server.wm.WindowState;
import com.huawei.android.view.HwWindowManager;
import com.huawei.displayengine.IDisplayEngineService;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONException;
import org.json.JSONObject;

public final class HwPCManagerService extends Stub {
    private static final String ACTION_ALARM_WAKEUP = "com.android.deskclock.ALARM_ALERT";
    private static final String ACTION_CLEAR_LIGHTER_DRAWED = "com.android.server.pc.action.clear_lighter_drawed";
    private static final String ACTION_NOTIFY_CHANGE_STATUS_BAR = "com.android.server.pc.action.CHANGE_STATUS_BAR";
    private static final String ACTION_NOTIFY_DISCONNECT = "com.android.server.pc.action.DISCONNECT";
    private static final String ACTION_NOTIFY_OPEN_EASY_PROJECTION = "com.android.server.pc.action.EASY_PROJECTION";
    private static final String ACTION_NOTIFY_SHOW_MK = "com.android.server.pc.action.SHOW_MK";
    private static final String ACTION_NOTIFY_SWITCH_MODE = "com.android.server.pc.action.SWITCH_MODE";
    private static final String ACTION_NOTIFY_UNINSTALL_APP = "com.android.server.pc.action.UNINSTALL_APP";
    private static final String ALARM_ALERT_CONFLICT = "huawei.deskclock.ALARM_ALERT_CONFLICT";
    private static final int BIT_KEYBOARD = 2;
    private static final int BIT_MOUSE = 1;
    private static final int BIT_NONE = 0;
    private static final String BROADCAST_PERMISSION = "com.huawei.deskclock.broadcast.permission";
    private static final long BROADCAST_SEND_INTERVAL = 500;
    private static final int CHECK_HARD_BROAD_DELAY = 2000;
    private static final String DP_LINK_STATE_AUX_FAILED = "AUX_FAILED";
    private static final String DP_LINK_STATE_CABLE_IN = "CABLE_IN";
    private static final String DP_LINK_STATE_CABLE_OUT = "CABLE_OUT";
    private static final String DP_LINK_STATE_EDID_FAILED = "EDID_FAILED";
    private static final String DP_LINK_STATE_HDCP_FAILED = "HDCP_FAILED";
    private static final String DP_LINK_STATE_LINK_FAILED = "LINK_FAILED";
    private static final String DP_LINK_STATE_LINK_RETRAINING = "LINK_RETRAINING";
    private static final String DP_LINK_STATE_MULTI_HPD = "MULTI_HPD";
    private static final String DP_LINK_STATE_SAFE_MODE = "SAFE_MODE";
    private static final int DREAMS_DISENABLED = 0;
    private static final int DREAMS_ENABLED = 1;
    private static final int DREAMS_INVALID = -1;
    private static final String EXCLUSIVE_DP_LINK = "DEVPATH=/devices/virtual/dp/source";
    private static final String EXCLUSIVE_KEYBOARD = "DEVPATH=/devices/virtual/hwsw_kb/hwkb";
    private static final int EXPLORER_BIND_ERROR = 2;
    private static final int EXPLORER_LAUNCH_DELAY = 4000;
    private static final String EXPLORER_SERVICE_NAME = "HwPCExplorer";
    private static final int FINGERPRINT_SLIDE_OFF = 0;
    private static final int FINGERPRINT_SLIDE_ON = 1;
    private static final String FINGERPRINT_SLIDE_SWITCH = "fingerprint_slide_switch";
    private static final int INVALID_ARG = -1;
    private static final int KEEP_RECORD_TIMEOUT = 180000;
    private static final int[] KEYBOARD_PRODUCT_ID = new int[]{4817};
    private static final int[] KEYBOARD_VENDOR_ID = new int[]{1455};
    private static final String KEY_BEFORE_BOOT_ANIM_TIME = "before_boot_anim_time";
    private static final String KEY_CURRENT_DISPLAY_UNIQUEID = "current_display_uniqueId";
    private static final String KEY_IS_WIRELESS_MODE = "is_wireless_mode";
    private static final int LAUNCH_GUIDE_DELAY = 500;
    private static final String MMI_TEST_PROPERTY = "runtime.mmitest.isrunning";
    private static final int MSG_CLEAR_LIGHTER_DRAWED = 24;
    private static final int MSG_CLOSE_CLIENT_TOP_WINDOW = 4;
    private static final int MSG_DISMISS_CLIENT_TASK_VIEW = 6;
    private static final int MSG_DISPLAY_ADDED = 6;
    private static final int MSG_DISPLAY_CHANGED = 7;
    private static final int MSG_DISPLAY_REMOVED = 8;
    private static final int MSG_DP_LINK_ERROR = 22;
    private static final int MSG_DP_STATE_CHANGED = 21;
    private static final int MSG_IME_STATUS_ICON_HIDE = 15;
    private static final int MSG_IME_STATUS_ICON_SHOW = 14;
    private static final int MSG_INPUTMETHOD_SWITCH = 12;
    private static final int MSG_KEEP_RECORD_TIMEOUT = 11;
    private static final int MSG_KEYCODE_APP_SWITCH = 9;
    private static final int MSG_KEYCODE_BACK = 11;
    private static final int MSG_KEYCODE_HOME = 10;
    private static final int MSG_LAUNCH_MK = 13;
    private static final int MSG_LOCK_SCREEN = 8;
    static final int MSG_NOTIFY_SWITCH_PROJ = 1;
    private static final int MSG_OPEN_EASY_PROJECTION = 23;
    private static final int MSG_REFRESH_NTFS = 5;
    private static final int MSG_REGISTER_ALARM = 15;
    private static final int MSG_RELAUNCH_IME = 14;
    private static final int MSG_RESTORE_APP = 10;
    private static final int MSG_SCREENSHOT_PC_DISPLAY = 3;
    private static final int MSG_SET_FOCUS_DISPLAY = 17;
    private static final int MSG_SET_PROJ_MODE = 4;
    private static final int MSG_SHOW_CLIENT_STARTUP_MENU = 2;
    private static final int MSG_SHOW_CLIENT_TASK_VIEW = 5;
    private static final int MSG_SHOW_CLIENT_TOPBAR = 1;
    private static final int MSG_SHOW_ENTER_DESKTOP_MODE = 19;
    private static final int MSG_SHOW_EXIT_DESKTOP_MODE = 20;
    private static final int MSG_START_RESTORE_APPS = 9;
    private static final int MSG_START_VOICE_ASSISTANT = 13;
    private static final int MSG_SWITCH_USER = 12;
    private static final int MSG_TASK_CREATED = 16;
    private static final int MSG_TASK_MOVE_TO_FRONT = 18;
    private static final int MSG_TASK_PROFILE_LOCKED = 19;
    private static final int MSG_TASK_REMOVED = 17;
    private static final int MSG_UNINSTALL_APP = 16;
    private static final int MSG_UPDATE_CFG = 18;
    private static final int MSG_USER_ACTIVITY_ON_DESKTOP = 7;
    private static final int NOTIFY_SWITCH_PROJ_ID = 0;
    private static final int NOTIFY_VIRTUAL_M_K_ID = 1;
    private static final String PERMISSION_BROADCAST_CHANGE_STATUS_BAR = "com.huawei.permission.pc.CHANGE_STATUS_BAR";
    private static final String PERMISSION_BROADCAST_CLEAR_LIGHTER_DRAWED = "com.huawei.permission.pc.CLEAR_LIGHTER_DRAWED";
    private static final String PERMISSION_BROADCAST_SWITCH_MODE = "com.huawei.permission.SWITCH_MODE";
    private static final String PERMISSION_PC_MANAGER_API = "com.huawei.permission.PC_MANAGER_API";
    private static final int RELAUNCH_IME_DELAY = 2000;
    private static final int RESO_1080P = 1920;
    private static final int RE_BIND_SERVICE_DELAY = 800;
    private static final int ROTATION_SWITCH_CLOSE = 0;
    private static final int ROTATION_SWITCH_OPEN = 1;
    private static final String SCREEN_POWER_DEVICE = "/sys/devices/virtual/dp/power/lcd_power";
    private static final String SCREEN_POWER_OFF = "0";
    private static final String SCREEN_POWER_ON = "1";
    private static final int START_ACTIVITY_INTERVAL = 800;
    private static final int START_RESTORE_APPS_DELAY_TIME = 3000;
    private static final int SWITCH_STATE_OFF = 0;
    private static final int SWITCH_STATE_ON = 1;
    private static final int SYSTEMUI_BIND_ERROR = 1;
    private static final String SYSTEMUI_SERVICE_NAME = "HwPCSystemUI";
    private static final String TAG = "HwPCManagerService";
    private static final int TIME_DISPALY_ADD_BEFORE_BOOT_ANIM = 4500;
    private static final int TIME_SEND_SWITCHPROJ_MSG_DELAYED = 200;
    private static final int TIME_SWITCH_MODE_BEFORE_BOOT_ANIM = 1500;
    private static final int TIME_UNLOCK_ACTION_BEFORE_BOOT_ANIM = 1500;
    private static final int TYPE_HPPCAT = 1;
    private static final int TYPE_UNKNOWN = -1;
    private static final int TYPE_WELINK = 2;
    private static final String WIRELESS_PROJECTION_STATE = "wireless_projection_state";
    private static final int devicetestmode = 2;
    private final String DEVICE_PROVISIONED_URI = "content://settings/global/device_provisioned";
    private final String SCREEN_OF_TIMEOUT_URI = "content://settings/system/screen_off_timeout";
    private boolean beginShow = false;
    private boolean isNeedEixtDesktop = false;
    private boolean isNeedEnterDesktop = false;
    private HwActivityManagerService mAMS;
    private final BroadcastReceiver mAlarmClockReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            HwPCUtils.log(HwPCManagerService.TAG, "receive clock alarm");
            HwPCManagerService.this.setScreenPower(true);
        }
    };
    private final OnAlarmListener mAlarmListener = new OnAlarmListener() {
        public void onAlarm() {
            HwPCManagerService.this.mHandler.sendEmptyMessage(11);
        }
    };
    private AlarmManager mAlarmManager;
    private float mAxisX = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private float mAxisY = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private BluetoothReminderDialog mBluetoothReminderDialog;
    private boolean mBluetoothStateOnEnter = false;
    private Toast mCallingToast;
    private final ServiceConnection mConnExplorer = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            HwPCUtils.log(HwPCManagerService.TAG, "explorer onServiceConnected");
        }

        public void onServiceDisconnected(ComponentName name) {
            HwPCUtils.log(HwPCManagerService.TAG, "explorer onServiceDisconnected");
            HwPCDataReporter.getInstance().reportFailToConnEvent(2, HwPCManagerService.EXPLORER_SERVICE_NAME, HwPCManagerService.this.mPCDisplayInfo);
            HwPCManagerService.this.mHandler.postDelayed(new Runnable() {
                public void run() {
                    if (HwPCUtils.isValidExtDisplayId(HwPCManagerService.this.get1stDisplay().mDisplayId) && HwPCManagerService.isDesktopMode(HwPCManagerService.this.mProjMode)) {
                        HwPCManagerService.this.mContext.bindService(new Intent().setComponent(HwPCManagerService.this.mExplorerComponent), HwPCManagerService.this.mConnExplorer, 1);
                    }
                }
            }, 800);
        }
    };
    private final ServiceConnection mConnSysUI = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            HwPCUtils.log(HwPCManagerService.TAG, "SysUI onServiceConnected");
            if (HwPCManagerService.this.get1stDisplay().mDisplayId != -1) {
                HwPCManagerService.this.updateDisplayOverrideConfiguration(HwPCManagerService.this.get1stDisplay().mDisplayId, 2000);
                HwPCManagerService.this.relaunchIMEDelay(2000);
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            HwPCUtils.log(HwPCManagerService.TAG, "SysUI onServiceDisconnected");
            HwPCDataReporter.getInstance().reportFailToConnEvent(1, HwPCManagerService.SYSTEMUI_SERVICE_NAME, HwPCManagerService.this.mPCDisplayInfo);
            HwPCManagerService.this.mHandler.postDelayed(new Runnable() {
                public void run() {
                    if (HwPCUtils.isValidExtDisplayId(HwPCManagerService.this.get1stDisplay().mDisplayId) && HwPCManagerService.isDesktopMode(HwPCManagerService.this.mProjMode)) {
                        HwPCManagerService.this.mContext.bindService(new Intent().setComponent(HwPCManagerService.this.mSystemUIComponent), HwPCManagerService.this.mConnSysUI, 1);
                    }
                }
            }, 800);
        }
    };
    private int mConnectedInputDevices = 0;
    private Context mContext;
    private final DisplayDriverCommunicator mDDC;
    private UEventObserver mDPLinkStateObserver = new UEventObserver() {
        public void onUEvent(UEvent event) {
            if (event != null) {
                String state = event.get("DP_LINK_EVENT");
                if (!TextUtils.isEmpty(state)) {
                    String tip1 = HwPCManagerService.this.mContext.getResources().getString(33686124);
                    String tip2 = HwPCManagerService.this.mContext.getResources().getString(33686125);
                    String tip = null;
                    boolean z = true;
                    switch (state.hashCode()) {
                        case -1790848990:
                            if (state.equals(HwPCManagerService.DP_LINK_STATE_LINK_FAILED)) {
                                z = true;
                                break;
                            }
                            break;
                        case -203035278:
                            if (state.equals(HwPCManagerService.DP_LINK_STATE_LINK_RETRAINING)) {
                                z = true;
                                break;
                            }
                            break;
                        case 18043426:
                            if (state.equals(HwPCManagerService.DP_LINK_STATE_EDID_FAILED)) {
                                z = true;
                                break;
                            }
                            break;
                        case 324233351:
                            if (state.equals(HwPCManagerService.DP_LINK_STATE_CABLE_IN)) {
                                z = false;
                                break;
                            }
                            break;
                        case 325027384:
                            if (state.equals(HwPCManagerService.DP_LINK_STATE_AUX_FAILED)) {
                                z = true;
                                break;
                            }
                            break;
                        case 615836371:
                            if (state.equals(HwPCManagerService.DP_LINK_STATE_HDCP_FAILED)) {
                                z = true;
                                break;
                            }
                            break;
                        case 1461305356:
                            if (state.equals(HwPCManagerService.DP_LINK_STATE_CABLE_OUT)) {
                                z = true;
                                break;
                            }
                            break;
                        case 1580920854:
                            if (state.equals(HwPCManagerService.DP_LINK_STATE_MULTI_HPD)) {
                                z = true;
                                break;
                            }
                            break;
                        case 1684923157:
                            if (state.equals(HwPCManagerService.DP_LINK_STATE_SAFE_MODE)) {
                                z = true;
                                break;
                            }
                            break;
                    }
                    switch (z) {
                        case false:
                            HwPCManagerService.this.beginShow = true;
                            break;
                        case true:
                            HwPCManagerService.this.dismissDpLinkErrorDialog();
                            HwPCManagerService.this.beginShow = false;
                            break;
                        case true:
                        case true:
                            tip = tip1;
                            break;
                        case true:
                        case true:
                        case true:
                        case true:
                        case true:
                            tip = tip2;
                            break;
                        default:
                            HwPCManagerService.this.beginShow = false;
                            break;
                    }
                    if (tip != null && HwPCManagerService.this.beginShow) {
                        HwPCManagerService.this.mHandler.removeMessages(22);
                        HwPCManagerService.this.mHandler.sendMessage(HwPCManagerService.this.mHandler.obtainMessage(22, tip));
                        HwPCManagerService.this.beginShow = false;
                    }
                }
            }
        }
    };
    private DisplayManager mDisplayManager;
    private int mDreamsEnabledSetting = -1;
    private AlertDialog mEnterDesktopAlertDialog = null;
    private AlertDialog mExitDesktopAlertDialog = null;
    private final ComponentName mExplorerComponent = new ComponentName("com.huawei.desktop.explorer", "com.huawei.filemanager.services.ExplorerService");
    final LocalHandler mHandler;
    final ServiceThread mHandlerThread;
    private volatile boolean mHasSwitchNtf = false;
    private HwPhoneWindowManager mHwPolicy;
    private IBinder mIBinderAudioService;
    private int mIMEWithHardKeyboardState = 1;
    private final InputDeviceListener mInputDeviceListener = new InputDeviceListener() {
        public void onInputDeviceAdded(int deviceId) {
            String str = HwPCManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onInputDeviceAdded, deviceId:");
            stringBuilder.append(deviceId);
            stringBuilder.append(", mConnectedInputDevices: ");
            stringBuilder.append(HwPCManagerService.this.mConnectedInputDevices);
            HwPCUtils.log(str, stringBuilder.toString());
            InputDevice device = InputDevice.getDevice(deviceId);
            HwPCManagerService.access$6776(HwPCManagerService.this, HwPCManagerService.whichInputDevice(device));
            if ((HwPCManagerService.whichInputDevice(device) & 1) != 0) {
                HwPCUtils.bdReport(HwPCManagerService.this.mContext, IDisplayEngineService.DE_ACTION_PG_GALLERY_FRONT, "");
            }
            if ((HwPCManagerService.whichInputDevice(device) & 2) != 0) {
                HwPCUtils.bdReport(HwPCManagerService.this.mContext, IDisplayEngineService.DE_ACTION_PG_INPUT_START, "");
            }
            if (HwPCUtils.enabledInPad()) {
                JSONObject jo = new JSONObject();
                try {
                    jo.put("START_TIME", System.currentTimeMillis());
                    if (HwPCManagerService.this.isExclusiveKeyboard(device)) {
                        jo.put("EXCLUSIVE", true);
                    } else {
                        jo.put("EXCLUSIVE", false);
                    }
                } catch (JSONException e) {
                    HwPCUtils.log(HwPCManagerService.TAG, "JSONException");
                }
                HwPCManagerService.this.mKeyboardInfo.put(Integer.valueOf(deviceId), jo);
            }
        }

        public void onInputDeviceRemoved(int deviceId) {
            String str = HwPCManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onInputDeviceRemoved, deviceId:");
            stringBuilder.append(deviceId);
            stringBuilder.append(", mConnectedInputDevices: ");
            stringBuilder.append(HwPCManagerService.this.mConnectedInputDevices);
            HwPCUtils.log(str, stringBuilder.toString());
            int connectedInputDevices = 0;
            for (InputDevice device : InputDevice.getDeviceIds()) {
                connectedInputDevices |= HwPCManagerService.whichInputDevice(InputDevice.getDevice(device));
            }
            HwPCManagerService.this.mConnectedInputDevices = connectedInputDevices;
            if (HwPCUtils.enabledInPad()) {
                try {
                    if (HwPCManagerService.this.mKeyboardInfo.containsKey(Integer.valueOf(deviceId))) {
                        JSONObject jo = (JSONObject) HwPCManagerService.this.mKeyboardInfo.get(Integer.valueOf(deviceId));
                        jo.put("END_TIME", System.currentTimeMillis());
                        String str2 = HwPCManagerService.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("report msg:");
                        stringBuilder2.append(jo.toString());
                        HwPCUtils.log(str2, stringBuilder2.toString());
                        HwPCManagerService.this.mKeyboardInfo.remove(Integer.valueOf(deviceId));
                        HwPCUtils.bdReport(HwPCManagerService.this.mContext, 10027, jo.toString());
                    }
                } catch (JSONException e) {
                    HwPCUtils.log(HwPCManagerService.TAG, "JSONException");
                }
            }
        }

        public void onInputDeviceChanged(int deviceId) {
        }
    };
    private final ComponentName mInstructionComponent = new ComponentName("com.huawei.desktop.explorer", "com.huawei.filemanager.desktopinstruction.PCHelpInformationActivity");
    private final ComponentName mInstructionComponentWirelessEnabled = new ComponentName("com.huawei.desktop.explorer", "com.huawei.filemanager.desktopinstruction.EasyProjection");
    ArrayList<Intent> mIntentList = new ArrayList();
    private boolean mIsDisplayAddedAfterSwitch = false;
    private boolean mIsDisplayLargerThan1080p = false;
    private boolean mIsNeedUnRegisterBluetoothReciver = false;
    private boolean mIsWifiBroadDone = false;
    private ConcurrentHashMap<Integer, JSONObject> mKeyboardInfo = new ConcurrentHashMap();
    private final UEventObserver mKeyboardObserver = new UEventObserver() {
        public void onUEvent(UEvent event) {
            if (event == null) {
                return;
            }
            if (HwPCManagerService.this.isExclusiveKeyboardConnect(event)) {
                HwPCUtils.log(HwPCManagerService.TAG, "Exclusive Keyboard Connect");
                if (!HwPCManagerService.isDesktopMode(HwPCManagerService.this.mProjMode)) {
                    HwPCManagerService.this.mHandler.sendMessage(HwPCManagerService.this.mHandler.obtainMessage(19));
                    return;
                }
                return;
            }
            HwPCUtils.log(HwPCManagerService.TAG, "Exclusive Keyboard Disconnect");
            if (HwPCManagerService.isDesktopMode(HwPCManagerService.this.mProjMode)) {
                HwPCManagerService.this.mHandler.sendMessage(HwPCManagerService.this.mHandler.obtainMessage(20));
            }
        }
    };
    private KeyguardManager mKeyguardManager;
    private int mLockScreenTimeout = 0;
    private Messenger mMessenger = null;
    private NotificationManager mNm;
    private final BroadcastReceiver mNotifyReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                HwPCUtils.log(HwPCManagerService.TAG, "mNotifyReceiver received a null intent");
                return;
            }
            String action = intent.getAction();
            if (HwPCManagerService.ACTION_NOTIFY_SWITCH_MODE.equals(action)) {
                HwPCUtils.bdReport(HwPCManagerService.this.mContext, IDisplayEngineService.DE_ACTION_PG_EBOOK_FRONT, "");
                HwPCManagerService.this.collapsePanels();
                HwPCManagerService.this.switchProjMode();
            } else if ("android.intent.action.LOCALE_CHANGED".equals(action)) {
                HwPCManagerService.this.refreshNotifications();
            } else if (HwPCManagerService.ACTION_NOTIFY_SHOW_MK.equals(action)) {
                HwPCUtils.bdReport(HwPCManagerService.this.mContext, 10047, "");
                HwPCManagerService.this.collapsePanels();
                HwPCManagerService.this.sendShowMkMessage();
            } else if (HwPCManagerService.ACTION_NOTIFY_DISCONNECT.equals(action)) {
                if (HwPCManagerService.this.mDisplayManager != null && HwPCManagerService.this.isConnectFromThirdApp(HwPCManagerService.this.get1stDisplay().mDisplayId) == 2) {
                    HwPCManagerService.this.launchWeLink();
                } else if (HwPCManagerService.this.mDisplayManager != null) {
                    HwPCUtils.bdReport(HwPCManagerService.this.mContext, 10048, "");
                    HwPCManagerService.this.mDisplayManager.disconnectWifiDisplay();
                }
                HwPCManagerService.this.collapsePanels();
            } else if (HwPCManagerService.ACTION_NOTIFY_OPEN_EASY_PROJECTION.equals(action)) {
                HwPCManagerService.this.sendOpenEasyProjectionMessage();
            } else if (HwPCManagerService.ACTION_NOTIFY_UNINSTALL_APP.equals(action)) {
                String pkgName = intent.getStringExtra("PACKAGE_NAME");
                if (pkgName != null) {
                    String str = HwPCManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ACTION_NOTIFY_UNINSTALL_APP onReceive: ");
                    stringBuilder.append(pkgName);
                    HwPCUtils.log(str, stringBuilder.toString());
                    HwPCManagerService.this.sendUninstallAppMessage(pkgName);
                }
            } else if ("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED".equals(action) && HwPCUtils.getIsWifiMode()) {
                WifiDisplayStatus status = intent.getParcelableExtra("android.hardware.display.extra.WIFI_DISPLAY_STATUS") != null ? (WifiDisplayStatus) intent.getParcelableExtra("android.hardware.display.extra.WIFI_DISPLAY_STATUS") : null;
                if (status != null) {
                    if (status.getActiveDisplay() != null) {
                        HwPCManagerService.this.mWireLessDeviceName = String.format(context.getResources().getString(33686115), new Object[]{status.getActiveDisplay().getDeviceName()});
                    } else {
                        HwPCUtils.log(HwPCManagerService.TAG, "wifiDisplay is null");
                    }
                } else {
                    HwPCUtils.log(HwPCManagerService.TAG, "status is null");
                }
            } else if (DecisionUtil.PC_TARGET_ACTION.equals(action)) {
                DecisionUtil.showPCRecommendDialog(HwPCManagerService.this.mContext);
            }
        }
    };
    private int mPCBeforeBootAnimTime = 2000;
    private DisplayInfo mPCDisplayInfo = null;
    private HwPCMkManager mPCMkManager;
    PCSettingsObserver mPCSettingsObserver;
    private int mPadDesktopModeLockScreenTimeout = 0;
    private int mPadLockScreenTimeout = 0;
    private boolean mPadPCDisplayIsRemoved = false;
    private HwPCMultiDisplaysManager mPcMultiDisplayMgr;
    private int mPhoneState = 0;
    PointerEventListener mPointerListener = new PointerEventListener() {
        public void onPointerEvent(MotionEvent motionEvent, int displayId) {
            if (HwPCUtils.isValidExtDisplayId(displayId) && motionEvent.getAction() == 8) {
                HwPCManagerService.this.filterScrollForPCMode();
            }
            onPointerEvent(motionEvent);
        }

        public void onPointerEvent(MotionEvent motionEvent) {
            if (motionEvent != null) {
                HwPCManagerService.this.mAxisX = motionEvent.getX();
                HwPCManagerService.this.mAxisY = motionEvent.getY();
            }
        }
    };
    private long mPrevTimeForBroadcast = SystemClock.uptimeMillis();
    volatile ProjectionMode mProjMode = ProjectionMode.DESKTOP_MODE;
    boolean mProvisioned = false;
    boolean mRestartAppsWhenUnlocked = false;
    private int mRotationSwitch = 1;
    private int mRotationValue = 0;
    private Object mScreenAccessLock = new Object();
    private boolean mScreenPowerOn = true;
    private AlertDialog mShowDpLinkErrorTipDialog = null;
    private final BroadcastReceiver mShutdownReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String act = intent.getAction();
                if (act != null && "android.intent.action.ACTION_SHUTDOWN".equals(act) && HwPCUtils.isPcCastModeInServer()) {
                    HwPCManagerService.this.restoreRotationInPad();
                    if (HwPCUtils.enabledInPad()) {
                        String str = HwPCManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("receive shut down broadcast , IME With Hard Keyboard State:");
                        stringBuilder.append(HwPCManagerService.this.mIMEWithHardKeyboardState);
                        HwPCUtils.log(str, stringBuilder.toString());
                        Secure.putInt(HwPCManagerService.this.mContext.getContentResolver(), "show_ime_with_hard_keyboard", HwPCManagerService.this.mIMEWithHardKeyboardState);
                    }
                    HwPCManagerService.this.restoreDreamSettingInPad();
                }
            }
        }
    };
    private boolean mSupportOverlay = SystemProperties.getBoolean("hw_pc_support_overlay", false);
    private boolean mSupportTouchPad = SystemProperties.getBoolean("hw_pc_support_touchpad", true);
    private final ComponentName mSystemUIComponent = new ComponentName("com.huawei.desktop.systemui", "com.huawei.systemui.SystemUIService");
    private TaskStackListener mTaskStackListener = new TaskStackListener() {
        public void onTaskSnapshotChanged(int taskId, TaskSnapshot snapshot) throws RemoteException {
            super.onTaskSnapshotChanged(taskId, snapshot);
        }

        public void onTaskStackChanged() throws RemoteException {
            super.onTaskStackChanged();
        }

        public void onActivityPinned(String packageName, int userId, int taskId, int stackId) throws RemoteException {
            super.onActivityPinned(packageName, userId, taskId, stackId);
        }

        public void onActivityUnpinned() throws RemoteException {
            super.onActivityUnpinned();
        }

        public void onPinnedActivityRestartAttempt(boolean clearedTask) throws RemoteException {
            super.onPinnedActivityRestartAttempt(clearedTask);
        }

        public void onPinnedStackAnimationStarted() throws RemoteException {
            super.onPinnedStackAnimationStarted();
        }

        public void onPinnedStackAnimationEnded() throws RemoteException {
            super.onPinnedStackAnimationEnded();
        }

        public void onActivityForcedResizable(String packageName, int taskId, int reason) throws RemoteException {
            super.onActivityForcedResizable(packageName, taskId, reason);
        }

        public void onActivityDismissingDockedStack() throws RemoteException {
            super.onActivityDismissingDockedStack();
        }

        public void onTaskCreated(int taskId, ComponentName componentName) throws RemoteException {
            super.onTaskCreated(taskId, componentName);
            try {
                if (HwPCManagerService.this.mMessenger != null) {
                    Message message = Message.obtain();
                    message.what = 16;
                    message.obj = componentName;
                    message.arg1 = taskId;
                    HwPCManagerService.this.mMessenger.send(message);
                }
            } catch (RemoteException e) {
                HwPCUtils.log(HwPCManagerService.TAG, "onTaskCreated RemoteException");
            }
        }

        public void onTaskRemoved(int taskId) throws RemoteException {
            super.onTaskRemoved(taskId);
            try {
                if (HwPCManagerService.this.mMessenger != null) {
                    Message message = Message.obtain();
                    message.what = 17;
                    message.arg1 = taskId;
                    HwPCManagerService.this.mMessenger.send(message);
                }
            } catch (RemoteException e) {
                HwPCUtils.log(HwPCManagerService.TAG, "onTaskRemoved RemoteException");
            }
        }

        public void onTaskMovedToFront(int taskId) throws RemoteException {
            super.onTaskMovedToFront(taskId);
            try {
                if (HwPCManagerService.this.mMessenger != null) {
                    Message message = Message.obtain();
                    message.what = 18;
                    message.arg1 = taskId;
                    HwPCManagerService.this.mMessenger.send(message);
                }
            } catch (RemoteException e) {
                HwPCUtils.log(HwPCManagerService.TAG, "onTaskMovedToFront RemoteException");
            }
        }

        public void onTaskRemovalStarted(int taskId) throws RemoteException {
            super.onTaskRemovalStarted(taskId);
        }

        public void onTaskDescriptionChanged(int taskId, TaskDescription td) throws RemoteException {
            super.onTaskDescriptionChanged(taskId, td);
        }

        public void onActivityRequestedOrientationChanged(int taskId, int requestedOrientation) throws RemoteException {
            super.onActivityRequestedOrientationChanged(taskId, requestedOrientation);
        }

        public void onTaskProfileLocked(int taskId, int userId) throws RemoteException {
            super.onTaskProfileLocked(taskId, userId);
            try {
                if (HwPCManagerService.this.mMessenger != null) {
                    Message message = Message.obtain();
                    message.what = 19;
                    message.arg1 = taskId;
                    message.arg2 = userId;
                    HwPCManagerService.this.mMessenger.send(message);
                }
            } catch (RemoteException e) {
                HwPCUtils.log(HwPCManagerService.TAG, "onTaskProfileLocked RemoteException");
            }
        }
    };
    private TelephonyManager mTelephonyPhone;
    int mTmpDisplayId2Unlocked;
    private final ComponentName mTouchPadComponent = new ComponentName("com.huawei.desktop.systemui", "com.huawei.systemui.mk.activity.ImitateActivity");
    Handler mUIHandler = new Handler(UiThread.get().getLooper());
    private UnlockScreenReceiver mUnlockScreenReceiver;
    private int mUserId = 0;
    UserManagerInternal mUserManagerInternal;
    private HwPCVAssistCmdExecutor mVAssistCmdExecutor;
    private BroadcastReceiver mWifiPCReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (action != null && action.equals("android.bluetooth.adapter.action.STATE_CHANGED") && intent.getIntExtra("android.bluetooth.adapter.extra.STATE", 0) == 12 && HwPCUtils.getIsWifiMode()) {
                    HwPCManagerService.this.mBluetoothReminderDialog.showCloseBluetoothTip(HwPCManagerService.this.mContext);
                }
            }
        }
    };
    private WindowManagerInternal mWindowManagerInternal;
    private String mWireLessDeviceName = "";
    private boolean restartByUnlock2SetAnimTime;

    private class LocalHandler extends Handler {
        public LocalHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            boolean forScroll = true;
            if (i != 1) {
                String str;
                switch (i) {
                    case 4:
                        if (!HwPCUtils.enabledInPad() || !HwPCManagerService.this.isMonkeyRunning()) {
                            if (HwPCManagerService.this.get1stDisplay().mDisplayId != -1) {
                                HwPCManagerService.this.mIsDisplayAddedAfterSwitch = false;
                                if (!HwPCManagerService.isDesktopMode(HwPCManagerService.this.mProjMode)) {
                                    HwPCUtils.setPCDisplayID(HwPCManagerService.this.get1stDisplay().mDisplayId);
                                    HwPCManagerService.this.mProjMode = ProjectionMode.DESKTOP_MODE;
                                    HwPCManagerService.this.mDDC.enableProjectionMode();
                                    if (!HwPCManagerService.this.mIsDisplayLargerThan1080p || HwPCManagerService.this.get1stDisplay().mType != 2) {
                                        HwPCManagerService.this.handleSwitchToDesktopMode();
                                        break;
                                    } else {
                                        Secure.putInt(HwPCManagerService.this.mContext.getContentResolver(), "selected-proj-mode", 1);
                                        break;
                                    }
                                }
                                HwPCManagerService.this.lightPhoneScreen();
                                str = HwPCManagerService.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("The current mode is DesktopMode, get1stDisplay().mType = ");
                                stringBuilder.append(HwPCManagerService.this.get1stDisplay().mType);
                                HwPCUtils.log(str, stringBuilder.toString());
                                HwPCUtils.setPhoneDisplayID(HwPCManagerService.this.get1stDisplay().mDisplayId);
                                HwPCManagerService.this.mProjMode = ProjectionMode.PHONE_MODE;
                                HwPCUtils.setPcCastModeInServerEarly(HwPCManagerService.this.mProjMode);
                                if (HwPCManagerService.this.get1stDisplay().mType == 2) {
                                    HwPCManagerService.this.mDDC.resetProjectionMode();
                                }
                                if (!HwPCUtils.enabledInPad()) {
                                    HwPCManagerService.this.bindUnbindService(false);
                                }
                                HwPCManagerService.this.mAMS.togglePCMode(HwPCManagerService.isDesktopMode(HwPCManagerService.this.mProjMode), HwPCManagerService.this.get1stDisplay().mDisplayId);
                                HwPCManagerService.this.setUsePCModeMouseIconContext(false);
                                HwPCUtils.setPcCastModeInServer(false);
                                HwPCManagerService.this.setPcCastingDisplayId(-1);
                                HwPCManagerService.this.mVAssistCmdExecutor.notifyDesktopModeChanged(false, 0);
                                HwPCManagerService.this.updateIMEWithHardKeyboardState(false);
                                Secure.putInt(HwPCManagerService.this.mContext.getContentResolver(), "selected-proj-mode", 0);
                                HwPCManagerService.this.restoreDreamSettingInPad();
                                HwPCManagerService.this.sendNotificationForSwitch(HwPCManagerService.this.mProjMode);
                                HwPCManagerService.this.bdReportDiffSrcStatus(false);
                                HwPCManagerService.this.bdReportSameSrcStatus(true);
                                HwPCManagerService.this.sendSwitchToStatusBar();
                                HwPCManagerService.this.setDesktopModeToAudioService(0);
                                HwPCManagerService.this.updateFingerprintSlideSwitch();
                                HwPCManagerService.this.relaunchIMEDelay(0);
                                if (HwPCUtils.enabledInPad()) {
                                    HwPCManagerService.this.bindUnbindService(false);
                                    DisplayManagerInternal dm = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
                                    if (dm != null) {
                                        dm.pcDisplayChange(false);
                                        HwPCManagerService.this.mPadPCDisplayIsRemoved = true;
                                        dm.pcDisplayChange(true);
                                    }
                                }
                                HwPCManagerService.this.exitDesktopModeForMk();
                                break;
                            }
                            return;
                        }
                        HwPCUtils.log(HwPCManagerService.TAG, "MSG_SET_PROJ_MODE isMonkeyRunning return !");
                        return;
                        break;
                    case 5:
                        if (HwPCManagerService.this.mHasSwitchNtf) {
                            HwPCManagerService.this.sendNotificationForSwitch(HwPCManagerService.this.mProjMode);
                            break;
                        }
                        break;
                    case 6:
                        if (HwPCManagerService.this.mKeyguardManager == null) {
                            HwPCManagerService.this.mKeyguardManager = (KeyguardManager) HwPCManagerService.this.mContext.getSystemService("keyguard");
                        }
                        if (!HwPCManagerService.this.mKeyguardManager.isKeyguardLocked() && StorageManager.isUserKeyUnlocked(HwPCManagerService.this.mUserId)) {
                            HwPCManagerService.this.onDisplayAdded(msg.arg1);
                            break;
                        }
                        HwPCManagerService.this.mTmpDisplayId2Unlocked = msg.arg1;
                        HwPCManagerService.this.mRestartAppsWhenUnlocked = true;
                        break;
                    case 7:
                        HwPCManagerService.this.onDisplayChanged(msg.arg1);
                        break;
                    case 8:
                        HwPCManagerService.this.onDisplayRemoved(msg.arg1);
                        break;
                    case 9:
                        HwPCManagerService.this.handleRestoreApps(msg.arg1);
                        break;
                    case 10:
                        if (!HwPCManagerService.this.getCastMode()) {
                            removeMessages(10);
                            break;
                        } else {
                            HwPCManagerService.this.restoreApp(msg.arg1, (Intent) msg.obj);
                            break;
                        }
                    case 11:
                        HwPCManagerService.this.mIntentList.clear();
                        break;
                    case 12:
                        HwPCManagerService.this.onSwitchUser(msg.arg1);
                        break;
                    case 13:
                        HwPCManagerService.this.launchMK();
                        break;
                    case 14:
                        HwPCManagerService.this.doRelaunchIMEIfNecessary();
                        break;
                    case 15:
                        HwPCManagerService.this.mIntentList.clear();
                        HwPCManagerService.this.mIntentList.addAll((List) msg.obj);
                        HwPCManagerService.this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + 180000, "keep_record", HwPCManagerService.this.mAlarmListener, HwPCManagerService.this.mHandler);
                        break;
                    case 16:
                        str = msg.obj;
                        String str2 = HwPCManagerService.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("handleMessage MSG_UNINSTALL_APP:");
                        stringBuilder2.append(str);
                        HwPCUtils.log(str2, stringBuilder2.toString());
                        if (str != null) {
                            HwPCManagerService.this.mContext.getPackageManager().deletePackage(str, (IPackageDeleteObserver) null, 2);
                            break;
                        }
                        break;
                    case 17:
                        HwPCManagerService.this.setFocusedPCDisplayId("unlockScreen");
                        break;
                    case 18:
                        HwPCManagerService.this.doUpdateDisplayOverrideConfiguration(msg.arg1);
                        break;
                    case 19:
                        HwPCManagerService.this.showEnterDesktopAlertDialog(HwPCManagerService.this.getCurrentContext(), true);
                        break;
                    case 20:
                        HwPCManagerService.this.showExitDesktopAlertDialog(HwPCManagerService.this.getCurrentContext(), true);
                        break;
                    case 21:
                        HwPCManagerService.this.mPcMultiDisplayMgr.notifyDpState(((Boolean) msg.obj).booleanValue());
                        break;
                    case 22:
                        HwPCManagerService.this.showDPLinkErrorDialog(HwPCManagerService.this.mContext, msg.obj);
                        break;
                    case 23:
                        HwPCManagerService.this.openEasyProjection();
                        break;
                    case 24:
                        KeyEvent ev = msg.obj;
                        if (msg.arg1 == 0) {
                            forScroll = false;
                        }
                        if (HwPCManagerService.this.shouldSendBroadcastForClearLighterDrawed(ev, forScroll)) {
                            HwPCManagerService.this.sendBroadcastForClearLighterDrawed();
                            break;
                        }
                        break;
                }
            }
            HwPCManagerService.this.sendNotificationForSwitch(msg.obj);
        }
    }

    class PCSettingsObserver extends ContentObserver {
        PCSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = HwPCManagerService.this.mContext.getContentResolver();
            if (HwPCUtils.enabledInPad()) {
                resolver.registerContentObserver(System.getUriFor("screen_off_timeout"), false, this, 0);
                updateScreenOffTimeoutSettings();
            }
            resolver.registerContentObserver(Global.getUriFor("device_provisioned"), false, this, 0);
        }

        public void onChange(boolean selfChange, Uri uri) {
            if (uri != null) {
                String str = HwPCManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("PCSettingsObserver onChange:");
                stringBuilder.append(selfChange);
                stringBuilder.append(" uri:");
                stringBuilder.append(uri.toString());
                HwPCUtils.log(str, stringBuilder.toString());
                str = uri.toString();
                Object obj = -1;
                int hashCode = str.hashCode();
                if (hashCode != -1333899149) {
                    if (hashCode == 1024070412 && str.equals("content://settings/global/device_provisioned")) {
                        obj = 1;
                    }
                } else if (str.equals("content://settings/system/screen_off_timeout")) {
                    obj = null;
                }
                switch (obj) {
                    case null:
                        updateScreenOffTimeoutSettings();
                        return;
                    case 1:
                        deviceChanged();
                        return;
                    default:
                        return;
                }
            }
        }

        private synchronized void updateScreenOffTimeoutSettings() {
            String str;
            StringBuilder stringBuilder;
            ContentResolver resolver = HwPCManagerService.this.mContext.getContentResolver();
            HwPCManagerService.this.mLockScreenTimeout = System.getIntForUser(resolver, "screen_off_timeout", 0, -2);
            if (HwPCUtils.enabledInPad()) {
                if (HwPCManagerService.isDesktopMode(HwPCManagerService.this.mProjMode) && HwPCUtils.isPcCastModeInServer()) {
                    HwPCManagerService.this.mPadDesktopModeLockScreenTimeout = HwPCManagerService.this.mLockScreenTimeout;
                    Secure.putInt(resolver, "pad_desktop_mode_screen_off_timeout", HwPCManagerService.this.mPadDesktopModeLockScreenTimeout);
                    str = HwPCManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("updateScreenOffTimeoutSettings PAD_DESKTOP_MODE_SCREEN_OFF_TIMEOUT=");
                    stringBuilder.append(HwPCManagerService.this.mPadDesktopModeLockScreenTimeout);
                    HwPCUtils.log(str, stringBuilder.toString());
                } else {
                    HwPCManagerService.this.mPadLockScreenTimeout = HwPCManagerService.this.mLockScreenTimeout;
                    Secure.putInt(resolver, "pad_screen_off_timeout", HwPCManagerService.this.mPadLockScreenTimeout);
                    str = HwPCManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("updateScreenOffTimeoutSettings PAD_SCREEN_OFF_TIMEOUT=");
                    stringBuilder.append(HwPCManagerService.this.mPadLockScreenTimeout);
                    HwPCUtils.log(str, stringBuilder.toString());
                }
            }
            str = HwPCManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateScreenOffTimeoutSettings ");
            stringBuilder.append(HwPCManagerService.this.mLockScreenTimeout);
            stringBuilder.append(" pad=");
            stringBuilder.append(HwPCManagerService.this.mPadLockScreenTimeout);
            stringBuilder.append(" pc=");
            stringBuilder.append(HwPCManagerService.this.mPadDesktopModeLockScreenTimeout);
            HwPCUtils.log(str, stringBuilder.toString());
        }

        synchronized void readScreenOffSettings() {
            if (HwPCUtils.enabledInPad()) {
                ContentResolver resolver = HwPCManagerService.this.mContext.getContentResolver();
                HwPCManagerService.this.mLockScreenTimeout = System.getIntForUser(resolver, "screen_off_timeout", 0, -2);
                HwPCManagerService.this.mPadLockScreenTimeout = Secure.getIntForUser(resolver, "pad_screen_off_timeout", HwPCManagerService.this.mLockScreenTimeout, -2);
                HwPCManagerService.this.mPadDesktopModeLockScreenTimeout = Secure.getIntForUser(resolver, "pad_desktop_mode_screen_off_timeout", 600000, -2);
                String str = HwPCManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("read screen off settings current=");
                stringBuilder.append(HwPCManagerService.this.mLockScreenTimeout);
                stringBuilder.append(" pad=");
                stringBuilder.append(HwPCManagerService.this.mPadLockScreenTimeout);
                stringBuilder.append(" pc=");
                stringBuilder.append(HwPCManagerService.this.mPadDesktopModeLockScreenTimeout);
                HwPCUtils.log(str, stringBuilder.toString());
            }
        }

        synchronized void restoreScreenOffSettings() {
            if (HwPCUtils.enabledInPad()) {
                ContentResolver resolver = HwPCManagerService.this.mContext.getContentResolver();
                String str;
                StringBuilder stringBuilder;
                if (HwPCManagerService.isDesktopMode(HwPCManagerService.this.mProjMode) && HwPCUtils.isPcCastModeInServer()) {
                    System.putIntForUser(resolver, "screen_off_timeout", HwPCManagerService.this.mPadDesktopModeLockScreenTimeout, -2);
                    str = HwPCManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("restoreScreenOffSettings mPadDesktopModeLockScreenTimeout=");
                    stringBuilder.append(HwPCManagerService.this.mPadDesktopModeLockScreenTimeout);
                    HwPCUtils.log(str, stringBuilder.toString());
                } else {
                    System.putIntForUser(resolver, "screen_off_timeout", HwPCManagerService.this.mPadLockScreenTimeout, -2);
                    str = HwPCManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("restoreScreenOffSettings mPadLockScreenTimeout=");
                    stringBuilder.append(HwPCManagerService.this.mPadLockScreenTimeout);
                    HwPCUtils.log(str, stringBuilder.toString());
                }
            }
        }

        private void deviceChanged() {
            boolean wasProvisioned = HwPCManagerService.this.mProvisioned;
            boolean isProvisioned = HwPCManagerService.this.deviceIsProvisioned();
            HwPCManagerService.this.mProvisioned = isProvisioned;
            if (isProvisioned && !wasProvisioned) {
                int displayId = -1;
                HwPCManagerService.this.mDisplayManager = (DisplayManager) HwPCManagerService.this.mContext.getSystemService("display");
                if (HwPCManagerService.this.mDisplayManager != null) {
                    Display[] displays = HwPCManagerService.this.mDisplayManager.getDisplays();
                    if (displays != null && displays.length > 0) {
                        int i = displays.length - 1;
                        while (i >= 0) {
                            if (displays[i] != null && displays[i].getDisplayId() != 0 && HwPCManagerService.this.isWiredDisplay(displays[i].getDisplayId())) {
                                displayId = displays[i].getDisplayId();
                                break;
                            }
                            i--;
                        }
                    }
                    if (displayId != -1) {
                        HwPCManagerService.this.scheduleDisplayAdded(displayId);
                    }
                }
            }
        }
    }

    private class UnlockScreenReceiver extends BroadcastReceiver {
        private UnlockScreenReceiver() {
        }

        /* synthetic */ UnlockScreenReceiver(HwPCManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                HwPCUtils.log(HwPCManagerService.TAG, "mUnlockScreenReceiver received a null intent");
                return;
            }
            if ("android.intent.action.USER_PRESENT".equals(intent.getAction())) {
                String str = HwPCManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("receive: ACTION_USER_PRESENT, mRestartAppsWhenUnlocked is ");
                stringBuilder.append(HwPCManagerService.this.mRestartAppsWhenUnlocked);
                HwPCUtils.log(str, stringBuilder.toString());
                if (HwPCManagerService.this.mRestartAppsWhenUnlocked) {
                    HwPCManagerService.this.mRestartAppsWhenUnlocked = false;
                    HwPCManagerService.this.restartByUnlock2SetAnimTime = true;
                    HwPCManagerService.this.scheduleDisplayAdded(HwPCManagerService.this.mTmpDisplayId2Unlocked);
                } else {
                    HwPCUtils.log(HwPCManagerService.TAG, "receive: ACTION_USER_PRESENT, MSG_SET_FOCUS_DISPLAY");
                    HwPCManagerService.this.mHandler.removeMessages(17);
                    HwPCManagerService.this.mHandler.sendMessage(HwPCManagerService.this.mHandler.obtainMessage(17));
                }
            }
        }
    }

    static /* synthetic */ int access$6776(HwPCManagerService x0, int x1) {
        int i = x0.mConnectedInputDevices | x1;
        x0.mConnectedInputDevices = i;
        return i;
    }

    private static boolean isDesktopMode(ProjectionMode mode) {
        return mode == ProjectionMode.DESKTOP_MODE;
    }

    private Context getCurrentContext() {
        if (!isDesktopMode(this.mProjMode)) {
            return this.mContext;
        }
        if (this.mDisplayManager == null) {
            this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
        }
        if (this.mDisplayManager != null) {
            Display display = null;
            for (Display dis : this.mDisplayManager.getDisplays()) {
                if (dis.getType() != 1 && HwPCUtils.isValidExtDisplayId(dis.getDisplayId())) {
                    display = dis;
                    break;
                }
            }
            if (display != null) {
                return this.mContext.createDisplayContext(display);
            }
        }
        return null;
    }

    private boolean isEnterDesktopModeRemembered() {
        int isRemembered = Secure.getInt(this.mContext.getContentResolver(), "enter-desktop-mode-remember", 0);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isEnterDesktopModeRemembered");
        stringBuilder.append(isRemembered);
        Log.d(str, stringBuilder.toString());
        return isRemembered == 1;
    }

    private boolean isExitDesktopModeRemembered() {
        int isRemembered = Secure.getInt(this.mContext.getContentResolver(), "exit-desktop-mode-remember", 0);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isEnterDesktopModeRemembered");
        stringBuilder.append(isRemembered);
        Log.d(str, stringBuilder.toString());
        return isRemembered == 1;
    }

    private boolean isShowEnterDialog() {
        int isRemembered = Secure.getInt(this.mContext.getContentResolver(), "show-enter-dialog-use-keyboard", 0);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isShowEnterDialog");
        stringBuilder.append(isRemembered);
        Log.d(str, stringBuilder.toString());
        if (isRemembered == 0) {
            return true;
        }
        return false;
    }

    private boolean isShowExitDialog() {
        int isRemembered = Secure.getInt(this.mContext.getContentResolver(), "show-exit-dialog-use-keyboard", 0);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isShowExitDialog");
        stringBuilder.append(isRemembered);
        Log.d(str, stringBuilder.toString());
        if (isRemembered == 0) {
            return true;
        }
        return false;
    }

    private SpannableString getSpanString(String orig, String re, String url) {
        Log.d(TAG, String.format("getSpanString: orig=%s re=%s url %s", new Object[]{orig, re, url}));
        SpannableString spannableString = new SpannableString(orig);
        int start = orig.indexOf(re);
        if (start < 0) {
            return spannableString;
        }
        spannableString.setSpan(new ClickableSpan() {
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
            }

            public void onClick(View widget) {
                Log.d(HwPCManagerService.TAG, "SpannableString onClick.");
            }
        }, start, re.length() + start, 33);
        return spannableString;
    }

    private boolean isMMIRunning() {
        return SystemProperties.get(MMI_TEST_PROPERTY, "false").equals("true");
    }

    private void showEnterDesktopAlertDialog(Context context, boolean isExclusiveKeyboard) {
        showEnterDesktopAlertDialog(context, isExclusiveKeyboard, false);
    }

    private boolean isKeyguardLocked() {
        if (this.mKeyguardManager != null) {
            return this.mKeyguardManager.isKeyguardLocked();
        }
        return false;
    }

    private void showEnterDesktopAlertDialog(Context context, boolean isExclusiveKeyboard, boolean notDisplayAdd) {
        Context context2 = context;
        boolean z = isExclusiveKeyboard;
        if (isMMIRunning() || isKeyguardLocked()) {
            HwPCUtils.log(TAG, "showEnterDesktopAlertDialog failed!");
            HwPCDataReporter.getInstance().reportFailEnterPadEvent(1, this.mPCDisplayInfo);
        } else if (deviceIsProvisioned() && isUserSetupComplete()) {
            int i = 0;
            if (isCalling()) {
                if (isDesktopMode(this.mProjMode)) {
                    i = get1stDisplay().mDisplayId;
                }
                showCallingToast(i);
                HwPCUtils.log(TAG, "switchProjMode failed! in Calling");
                HwPCDataReporter.getInstance().reportFailEnterPadEvent(3, this.mPCDisplayInfo);
            } else if (this.mUserId != 0) {
                HwPCUtils.log(TAG, "showEnterDesktopAlertDialog failed! currentUser is not UserHandle.USER_OWNER");
            } else if (context2 != null && (this.mEnterDesktopAlertDialog == null || !this.mEnterDesktopAlertDialog.isShowing())) {
                if (isEnterDesktopModeRemembered() && !isDesktopMode(this.mProjMode) && !z) {
                    backToHomeInDefaultDisplay(get1stDisplay().mDisplayId);
                    sendSwitchMsgDelayed(200);
                } else if (!z || isShowEnterDialog()) {
                    Builder buider = new Builder(context2, 33947691);
                    View view = LayoutInflater.from(buider.getContext()).inflate(34013325, null);
                    if (view == null) {
                        HwPCDataReporter.getInstance().reportFailEnterPadEvent(4, this.mPCDisplayInfo);
                        return;
                    }
                    ImageView imageView = (ImageView) view.findViewById(34603272);
                    TextView textView = (TextView) view.findViewById(34603271);
                    CheckBox checkBox = (CheckBox) view.findViewById(34603273);
                    if (imageView != null && textView != null && checkBox != null) {
                        String content;
                        if (z) {
                            imageView.setPadding(0, 0, 0, 0);
                            content = this.mContext.getResources().getString(33685943);
                        } else {
                            imageView.setImageResource(33751967);
                            content = this.mContext.getResources().getString(33685921);
                        }
                        textView.setText(getSpanString(content, this.mContext.getResources().getString(33685948), ""));
                        textView.setMovementMethod(LinkMovementMethod.getInstance());
                        checkBox.setOnCheckedChangeListener(-$$Lambda$HwPCManagerService$O9bKuYfIo9MANf97DdpWE4QaTDs.INSTANCE);
                        this.mEnterDesktopAlertDialog = buider.setTitle(33685950).setPositiveButton(33685949, new -$$Lambda$HwPCManagerService$a31z__dZ2TZtRr1fxAqtbeG9u5s(this, checkBox, z)).setNegativeButton(33685920, new -$$Lambda$HwPCManagerService$DLwW3gqo9PPDm10xR2WICv-KPQE(this)).setView(view).setOnDismissListener(new -$$Lambda$HwPCManagerService$BN4ZW4Lrv5EdDta6LzrPtHQbUN8(this)).create();
                        this.mEnterDesktopAlertDialog.setCanceledOnTouchOutside(true);
                        this.mEnterDesktopAlertDialog.getWindow().setType(HwArbitrationDEFS.MSG_MPLINK_BIND_FAIL);
                        this.mEnterDesktopAlertDialog.setOnShowListener(new OnShowListener() {
                            public void onShow(DialogInterface dialog) {
                                Button btn = HwPCManagerService.this.mEnterDesktopAlertDialog.getButton(-1);
                                btn.setFocusable(true);
                                btn.setFocusedByDefault(true);
                                btn.setFocusableInTouchMode(true);
                                btn.requestFocus();
                            }
                        });
                        this.mEnterDesktopAlertDialog.show();
                        this.mEnterDesktopAlertDialog.getWindow().getAttributes().setTitle("EnterDesktopAlertDialog");
                        Window w = this.mEnterDesktopAlertDialog.getWindow();
                        ContentResolver contentResolver = context.getContentResolver();
                        if (!(w == null || contentResolver == null)) {
                            View titleView = w.findViewById(16908716);
                            boolean isTalkbackOpen = Secure.getInt(contentResolver, "accessibility_enabled", 0) > 0;
                            if (titleView != null && !titleView.isAccessibilityFocused() && z && notDisplayAdd && isTalkbackOpen) {
                                titleView.requestAccessibilityFocus();
                            }
                        }
                    }
                }
            }
        } else {
            HwPCUtils.log(TAG, "showEnterDesktopAlertDialog failed!  Startup Guide is not Complete");
            HwPCDataReporter.getInstance().reportFailEnterPadEvent(2, this.mPCDisplayInfo);
        }
    }

    static /* synthetic */ void lambda$showEnterDesktopAlertDialog$0(CompoundButton buttonView, boolean isChecked) {
    }

    public static /* synthetic */ void lambda$showEnterDesktopAlertDialog$1(HwPCManagerService hwPCManagerService, CheckBox checkBox, boolean isExclusiveKeyboard, DialogInterface dialog, int which) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onClick:start which=");
        stringBuilder.append(which);
        Log.d(str, stringBuilder.toString());
        hwPCManagerService.backToHomeInDefaultDisplay(hwPCManagerService.get1stDisplay().mDisplayId);
        hwPCManagerService.isNeedEnterDesktop = true;
        if (checkBox.isChecked()) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onClick:start which=");
            stringBuilder2.append(which);
            Log.d(str2, stringBuilder2.toString());
            if (isExclusiveKeyboard) {
                Secure.putInt(hwPCManagerService.mContext.getContentResolver(), "show-enter-dialog-use-keyboard", 1);
            } else {
                Secure.putInt(hwPCManagerService.mContext.getContentResolver(), "enter-desktop-mode-remember", 1);
            }
        }
        dialog.dismiss();
    }

    public static /* synthetic */ void lambda$showEnterDesktopAlertDialog$2(HwPCManagerService hwPCManagerService, DialogInterface dialog, int which) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onClick:cancel which=");
        stringBuilder.append(which);
        Log.d(str, stringBuilder.toString());
        hwPCManagerService.isNeedEnterDesktop = false;
        dialog.cancel();
    }

    public static /* synthetic */ void lambda$showEnterDesktopAlertDialog$3(HwPCManagerService hwPCManagerService, DialogInterface dialog) {
        if (hwPCManagerService.isNeedEnterDesktop) {
            Log.d(TAG, "EnterDesktopAlertDialog dismiss");
            hwPCManagerService.isNeedEnterDesktop = false;
            hwPCManagerService.sendSwitchMsgDelayed(200);
        }
    }

    private void sendSwitchMsgDelayed(int delayMillis) {
        this.mHandler.removeMessages(4);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(4), (long) delayMillis);
    }

    private void showExitDesktopAlertDialog(Context context, boolean isExclusiveKeyboard) {
        if (isMMIRunning() || isKeyguardLocked()) {
            HwPCUtils.log(TAG, "showExitDesktopAlertDialog failed!");
            return;
        }
        int i = 0;
        if (isCalling()) {
            if (isDesktopMode(this.mProjMode)) {
                i = get1stDisplay().mDisplayId;
            }
            showCallingToast(i);
            HwPCUtils.log(TAG, "switchProjMode failed! in Calling");
        } else if (context != null && (this.mExitDesktopAlertDialog == null || !this.mExitDesktopAlertDialog.isShowing())) {
            if (isExitDesktopModeRemembered() && isDesktopMode(this.mProjMode) && !isExclusiveKeyboard) {
                sendSwitchMsg();
            } else if (!isExclusiveKeyboard || isShowExitDialog()) {
                Builder buider = new Builder(context, 33947691);
                View view = LayoutInflater.from(buider.getContext()).inflate(34013325, null);
                if (view != null) {
                    String enter_toast = context.getString(33685951);
                    String exit_toast = context.getString(33686009);
                    String enter_exclusive_keyboard = context.getString(33685922);
                    String exit_exclusive_keyboard = context.getString(33685975);
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("these string will be used in future:");
                    stringBuilder.append(enter_toast);
                    stringBuilder.append(exit_toast);
                    stringBuilder.append(enter_exclusive_keyboard);
                    stringBuilder.append(exit_exclusive_keyboard);
                    HwPCUtils.log(str, stringBuilder.toString());
                    ImageView imageView = (ImageView) view.findViewById(34603272);
                    TextView textView = (TextView) view.findViewById(34603271);
                    CheckBox checkBox = (CheckBox) view.findViewById(34603273);
                    if (imageView != null && textView != null && checkBox != null) {
                        imageView.setPadding(0, 0, 0, 0);
                        if (isExclusiveKeyboard) {
                            textView.setText(33685976);
                        } else {
                            textView.setText(33685969);
                        }
                        checkBox.setOnCheckedChangeListener(-$$Lambda$HwPCManagerService$cbPBgxGRcbQ3qc0VOMSqqDsH6w0.INSTANCE);
                        this.mExitDesktopAlertDialog = buider.setTitle(33686003).setPositiveButton(33685978, new -$$Lambda$HwPCManagerService$LABOusEbz-E8gVYSSL7I5jumj_8(this, checkBox, isExclusiveKeyboard)).setNegativeButton(33685964, new -$$Lambda$HwPCManagerService$FRGTPoH-eJt9tS6GrONnb_M9wPo(this)).setView(view).setOnDismissListener(new -$$Lambda$HwPCManagerService$Qdt8FUBRVqlM6MgXu-iSuIAD_vA(this)).create();
                        this.mExitDesktopAlertDialog.setCanceledOnTouchOutside(true);
                        this.mExitDesktopAlertDialog.getWindow().setType(HwArbitrationDEFS.MSG_MPLINK_BIND_FAIL);
                        this.mExitDesktopAlertDialog.setOnShowListener(new OnShowListener() {
                            public void onShow(DialogInterface dialog) {
                                Button btn = HwPCManagerService.this.mExitDesktopAlertDialog.getButton(-1);
                                btn.setFocusable(true);
                                btn.setFocusedByDefault(true);
                                btn.setFocusableInTouchMode(true);
                                btn.requestFocus();
                            }
                        });
                        this.mExitDesktopAlertDialog.show();
                        this.mExitDesktopAlertDialog.getWindow().getAttributes().setTitle("ExitDesktopAlertDialog");
                    }
                }
            }
        }
    }

    static /* synthetic */ void lambda$showExitDesktopAlertDialog$4(CompoundButton buttonView, boolean isChecked) {
    }

    public static /* synthetic */ void lambda$showExitDesktopAlertDialog$5(HwPCManagerService hwPCManagerService, CheckBox checkBox, boolean isExclusiveKeyboard, DialogInterface dialog, int which) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onClick:start which=");
        stringBuilder.append(which);
        Log.d(str, stringBuilder.toString());
        hwPCManagerService.isNeedEixtDesktop = true;
        if (checkBox.isChecked()) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onClick:start which=");
            stringBuilder2.append(which);
            Log.d(str2, stringBuilder2.toString());
            if (isExclusiveKeyboard) {
                Secure.putInt(hwPCManagerService.mContext.getContentResolver(), "show-exit-dialog-use-keyboard", 1);
            } else {
                Secure.putInt(hwPCManagerService.mContext.getContentResolver(), "exit-desktop-mode-remember", 1);
            }
        }
        dialog.dismiss();
    }

    public static /* synthetic */ void lambda$showExitDesktopAlertDialog$6(HwPCManagerService hwPCManagerService, DialogInterface dialog, int which) {
        hwPCManagerService.isNeedEixtDesktop = false;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onClick:cancel which=");
        stringBuilder.append(which);
        Log.d(str, stringBuilder.toString());
        dialog.cancel();
    }

    public static /* synthetic */ void lambda$showExitDesktopAlertDialog$7(HwPCManagerService hwPCManagerService, DialogInterface dialog) {
        if (hwPCManagerService.isNeedEixtDesktop) {
            Log.d(TAG, "EnterDesktopAlertDialog dismiss");
            hwPCManagerService.isNeedEixtDesktop = false;
            hwPCManagerService.sendSwitchMsgDelayed(200);
        }
    }

    private void handleSwitchToDesktopMode() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleSwitchToDesktopMode, mIsDisplayAddedAfterSwitch = ");
        stringBuilder.append(this.mIsDisplayAddedAfterSwitch);
        HwPCUtils.log(str, stringBuilder.toString());
        if (this.mIsDisplayAddedAfterSwitch) {
            HwPCDataReporter.getInstance().reportFailSwitchEvent(1, this.mProjMode.ordinal(), this.mPCDisplayInfo);
            return;
        }
        HwPCUtils.setPCDisplayID(get1stDisplay().mDisplayId);
        HwPCUtils.setPcCastModeInServerEarly(this.mProjMode);
        HwPCUtils.setPcCastModeInServer(true);
        setPcCastingDisplayId(get1stDisplay().mDisplayId);
        autoLaunchMK();
        this.mVAssistCmdExecutor.notifyDesktopModeChanged(true, get1stDisplay().mDisplayId);
        updateIMEWithHardKeyboardState(true);
        saveRotationInPad();
        this.mAMS.freezeOrThawRotationInPcMode();
        saveDreamSettingInPad();
        this.mPCBeforeBootAnimTime = 1500;
        bindUnbindService(true);
        this.mAMS.togglePCMode(isDesktopMode(this.mProjMode), get1stDisplay().mDisplayId);
        setUsePCModeMouseIconContext(true);
        Secure.putInt(this.mContext.getContentResolver(), "selected-proj-mode", 1);
        sendNotificationForSwitch(this.mProjMode);
        bdReportDiffSrcStatus(true);
        bdReportSameSrcStatus(false);
        scheduleRestoreApps(get1stDisplay().mDisplayId);
        sendSwitchToStatusBar();
        setDesktopModeToAudioService(1);
        uploadPcDisplaySizePro();
        enableFingerprintSlideSwitch();
        lightPhoneScreen();
        if (HwPCUtils.enabledInPad()) {
            setFocusedPCDisplayId("enterDesktop");
        }
        relaunchIMEDelay(2000);
        enterDesktopModeForMk();
    }

    UserManagerInternal getUserManagerInternal() {
        if (this.mUserManagerInternal == null) {
            this.mUserManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        }
        return this.mUserManagerInternal;
    }

    public void scheduleDisplayAdded(int displayId) {
        if (!checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("scheduleDisplayAdded checkCallingPermission failed");
            stringBuilder.append(Binder.getCallingPid());
            HwPCUtils.log(str, stringBuilder.toString());
        } else if (HwVRUtils.isVRMode()) {
            HwPCUtils.log(TAG, "vr mode should not add vr display");
        } else {
            this.mHandler.removeMessages(6);
            this.mHandler.sendMessage(this.mHandler.obtainMessage(6, displayId, -1));
        }
    }

    public void scheduleDisplayChanged(int displayId) {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            this.mHandler.removeMessages(7);
            this.mHandler.sendMessage(this.mHandler.obtainMessage(7, displayId, -1));
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("scheduleDisplayChanged checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
    }

    public void scheduleDisplayRemoved(int displayId) {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            this.mHandler.removeMessages(8);
            this.mHandler.sendMessage(this.mHandler.obtainMessage(8, displayId, -1));
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("scheduleDisplayRemoved checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
    }

    private void bindUnbindService(boolean bind) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bindUnbindService:");
        stringBuilder.append(bind);
        stringBuilder.append(" current display is:");
        stringBuilder.append(get1stDisplay().mDisplayId);
        HwPCUtils.log(str, stringBuilder.toString());
        this.mPCSettingsObserver.restoreScreenOffSettings();
        if (bind) {
            Intent intent = new Intent();
            intent.putExtra(KEY_IS_WIRELESS_MODE, HwPCUtils.getIsWifiMode());
            intent.setComponent(this.mSystemUIComponent);
            this.mContext.bindService(intent, this.mConnSysUI, 1);
            Intent intent2 = new Intent();
            intent2.putExtra(KEY_BEFORE_BOOT_ANIM_TIME, this.mPCBeforeBootAnimTime);
            intent2.putExtra(KEY_IS_WIRELESS_MODE, HwPCUtils.getIsWifiMode());
            intent2.putExtra(KEY_CURRENT_DISPLAY_UNIQUEID, getPcDisplayInfo() != null ? getPcDisplayInfo().uniqueId : "");
            intent2.setComponent(this.mExplorerComponent);
            this.mContext.bindService(intent2, this.mConnExplorer, 1);
            registerScreenOnEvent();
            registerShutdownEvent();
            if (HwPCUtils.getIsWifiMode()) {
                registerBluetoothReceiver();
            }
            this.mAMS.registerHwTaskStackListener(this.mTaskStackListener);
            return;
        }
        unbindAllPcService();
        unRegisterScreenOnEvent();
        restoreRotationInPad();
        unRegisterShutdownEvent();
        if (this.mIsNeedUnRegisterBluetoothReciver) {
            unRegisterBluetoothReceiver();
        }
        this.mAMS.unRegisterHwTaskStackListener(this.mTaskStackListener);
    }

    private void saveRotationInPad() {
        if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer()) {
            long identity = Binder.clearCallingIdentity();
            try {
                this.mRotationSwitch = System.getIntForUser(this.mContext.getContentResolver(), "accelerometer_rotation", 1, this.mUserId);
                if (this.mRotationSwitch == 0) {
                    this.mRotationValue = System.getIntForUser(this.mContext.getContentResolver(), "user_rotation", 0, this.mUserId);
                }
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("saveRotationInPad ");
                stringBuilder.append(e);
                HwPCUtils.log(str, stringBuilder.toString());
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void restoreRotationInPad() {
        if (HwPCUtils.enabledInPad()) {
            long identity = Binder.clearCallingIdentity();
            try {
                if (this.mRotationSwitch == 0) {
                    System.putIntForUser(this.mContext.getContentResolver(), "user_rotation", this.mRotationValue, this.mUserId);
                } else {
                    System.putIntForUser(this.mContext.getContentResolver(), "accelerometer_rotation", 1, this.mUserId);
                }
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("restoreRotationInPad ");
                stringBuilder.append(e);
                HwPCUtils.log(str, stringBuilder.toString());
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void registerShutdownEvent() {
        if (HwPCUtils.enabledInPad()) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.ACTION_SHUTDOWN");
            this.mContext.registerReceiver(this.mShutdownReceiver, filter);
        }
    }

    private void unRegisterShutdownEvent() {
        if (HwPCUtils.enabledInPad()) {
            try {
                this.mContext.unregisterReceiver(this.mShutdownReceiver);
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unRegisterShutdownEvent ");
                stringBuilder.append(e);
                HwPCUtils.log(str, stringBuilder.toString());
            }
        }
    }

    private void saveDreamSettingInPad() {
        if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer()) {
            ContentResolver resolver = this.mContext.getContentResolver();
            try {
                int DreamsSetting = Secure.getIntForUser(resolver, "screensaver_enabled", -1, this.mUserId);
                if (DreamsSetting == 1) {
                    Secure.putIntForUser(resolver, "screensaver_enabled", 0, this.mUserId);
                    this.mDreamsEnabledSetting = DreamsSetting;
                }
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("saveDreamSettingInPad ");
                stringBuilder.append(e);
                HwPCUtils.log(str, stringBuilder.toString());
            }
        }
    }

    private void restoreDreamSettingInPad() {
        if (HwPCUtils.enabledInPad()) {
            ContentResolver resolver = this.mContext.getContentResolver();
            try {
                int DreamsSetting = Secure.getIntForUser(resolver, "screensaver_enabled", -1, this.mUserId);
                if (this.mDreamsEnabledSetting == 1 && this.mDreamsEnabledSetting != DreamsSetting) {
                    Secure.putIntForUser(resolver, "screensaver_enabled", this.mDreamsEnabledSetting, this.mUserId);
                    this.mDreamsEnabledSetting = -1;
                }
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("restoreDreamSettingInPad ");
                stringBuilder.append(e);
                HwPCUtils.log(str, stringBuilder.toString());
            }
        }
    }

    private void unbindAllPcService() {
        HwPCUtils.log(TAG, "unbindAllPcService");
        try {
            this.mContext.unbindService(this.mConnSysUI);
            this.mContext.unbindService(this.mConnExplorer);
        } catch (Exception e) {
            HwPCUtils.log(TAG, "failed to unbind pc services");
        }
    }

    public HwPCManagerService(Context context, ActivityManagerService ams) {
        Context context2 = context;
        boolean projMode = false;
        this.mContext = context2;
        this.mAMS = (HwActivityManagerService) ams;
        this.mHandlerThread = new ServiceThread(TAG, -2, false);
        this.mHandlerThread.start();
        this.mHandler = new LocalHandler(this.mHandlerThread.getLooper());
        boolean isFactory = SystemProperties.get("ro.runmode", "normal").equals("factory");
        boolean isMmiTest = SystemProperties.get(MMI_TEST_PROPERTY, "false").equals("true");
        if (isFactory || isMmiTest) {
            HwPCUtils.setFactoryOrMmiState(true);
        }
        UserInfo userInfo = this.mAMS.getCurrentUser();
        if (userInfo != null) {
            this.mUserId = userInfo.id;
        }
        try {
            ActivityManager.getService().registerUserSwitchObserver(new UserSwitchObserver() {
                public void onUserSwitchComplete(int newUserId) throws RemoteException {
                    String str = HwPCManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onUserSwitchComplete userId: ");
                    stringBuilder.append(newUserId);
                    HwPCUtils.log(str, stringBuilder.toString());
                }

                public void onUserSwitching(int newUserId, IRemoteCallback reply) throws RemoteException {
                    String str = HwPCManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onUserSwitching userId: ");
                    stringBuilder.append(newUserId);
                    HwPCUtils.log(str, stringBuilder.toString());
                    HwPCManagerService.this.scheduleSwitchUser(newUserId);
                    if (reply != null) {
                        try {
                            reply.sendResult(null);
                        } catch (RemoteException e) {
                            HwPCUtils.log(HwPCManagerService.TAG, "onUserSwitching Exception");
                        }
                    }
                }
            }, TAG);
        } catch (RemoteException e) {
            HwPCUtils.log(TAG, "registerUserSwitchObserver RemoteException");
        }
        this.mDDC = DisplayDriverCommunicator.getInstance();
        int displayId = -1;
        this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
        if (this.mDisplayManager != null) {
            int i;
            Display[] displays = this.mDisplayManager.getDisplays();
            if (displays != null && displays.length > 0) {
                i = displays.length - 1;
                while (i >= 0) {
                    if (displays[i] != null && displays[i].getDisplayId() != 0 && isWiredDisplay(displays[i].getDisplayId())) {
                        displayId = displays[i].getDisplayId();
                        break;
                    }
                    i--;
                }
            }
            if (displayId != -1) {
                i = displayId;
                this.mUIHandler.postDelayed(new Runnable() {
                    public void run() {
                        HwPCManagerService.this.scheduleDisplayAdded(i);
                    }
                }, 5000);
            }
        }
        InputManager im = (InputManager) this.mContext.getSystemService("input");
        if (im != null) {
            for (InputDevice device : InputDevice.getDeviceIds()) {
                this.mConnectedInputDevices |= whichInputDevice(InputDevice.getDevice(device));
            }
            im.registerInputDeviceListener(this.mInputDeviceListener, null);
        }
        this.mPCSettingsObserver = new PCSettingsObserver(this.mHandler);
        this.mPCSettingsObserver.readScreenOffSettings();
        this.mPCSettingsObserver.restoreScreenOffSettings();
        this.mProvisioned = deviceIsProvisioned();
        this.mPCSettingsObserver.observe();
        if (HwPCUtils.enabledInPad()) {
            if (Secure.getInt(this.mContext.getContentResolver(), "selected-proj-mode", 0) == 1) {
                projMode = true;
            }
            if (projMode) {
                this.mProjMode = ProjectionMode.DESKTOP_MODE;
            } else {
                this.mProjMode = ProjectionMode.PHONE_MODE;
            }
        } else {
            this.mProjMode = ProjectionMode.DESKTOP_MODE;
            Secure.putInt(this.mContext.getContentResolver(), "selected-proj-mode", 1);
        }
        Global.putInt(this.mContext.getContentResolver(), "is_display_device_connected", 1);
        this.mNm = (NotificationManager) this.mContext.getSystemService("notification");
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NOTIFY_SWITCH_MODE);
        filter.addAction("android.intent.action.LOCALE_CHANGED");
        filter.addAction(ACTION_NOTIFY_UNINSTALL_APP);
        filter.addAction(ACTION_NOTIFY_SHOW_MK);
        filter.addAction(ACTION_NOTIFY_DISCONNECT);
        filter.addAction(ACTION_NOTIFY_OPEN_EASY_PROJECTION);
        filter.addAction("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED");
        filter.addAction(DecisionUtil.PC_TARGET_ACTION);
        this.mContext.registerReceiver(this.mNotifyReceiver, filter, PERMISSION_BROADCAST_SWITCH_MODE, null);
        this.mUnlockScreenReceiver = new UnlockScreenReceiver(this, null);
        IntentFilter unlockFilter = new IntentFilter();
        unlockFilter.addAction("android.intent.action.USER_PRESENT");
        this.mContext.registerReceiver(this.mUnlockScreenReceiver, unlockFilter);
        registerExternalPointerEventListener();
        this.mIBinderAudioService = ServiceManager.getService("audio");
        setDesktopModeToAudioService(-1);
        this.mAlarmManager = (AlarmManager) context2.getSystemService("alarm");
        this.mTelephonyPhone = (TelephonyManager) this.mContext.getSystemService("phone");
        HwPCUtils.setPcCastModeInServerEarly(ProjectionMode.PHONE_MODE);
        this.mKeyboardObserver.startObserving(EXCLUSIVE_KEYBOARD);
        this.mDPLinkStateObserver.startObserving(EXCLUSIVE_DP_LINK);
        this.mBluetoothReminderDialog = new BluetoothReminderDialog();
        this.mPcMultiDisplayMgr = new HwPCMultiDisplaysManager(this.mContext, this.mHandler, this);
        this.mVAssistCmdExecutor = new HwPCVAssistCmdExecutor(this.mContext, this, this.mAMS);
    }

    boolean deviceIsProvisioned() {
        return Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
    }

    boolean isUserSetupComplete() {
        return Secure.getInt(this.mContext.getContentResolver(), "user_setup_complete", 0) != 0;
    }

    private void setDesktopModeToAudioService(int mode) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setDesktopModeToAudioService, mIBinderAudioService = ");
        stringBuilder.append(this.mIBinderAudioService);
        HwPCUtils.log(str, stringBuilder.toString());
        if (this.mIBinderAudioService != null) {
            try {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                data.writeInt(mode);
                this.mIBinderAudioService.transact(1104, data, reply, 0);
            } catch (RemoteException e) {
                HwPCUtils.log(TAG, "setDesktopModeToAudioService RemoteException");
            }
        }
    }

    private void scheduleSwitchUser(int userId) {
        this.mHandler.removeMessages(12);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(12, userId, -1));
    }

    private void onSwitchUser(int userId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onSwitchUser userId =");
        stringBuilder.append(userId);
        HwPCUtils.log(str, stringBuilder.toString());
        this.mUserId = userId;
        if (get1stDisplay().mDisplayId != -1) {
            if (userId == 0) {
                HwPCUtils.log(TAG, "onSwitchUser: UserHandle.USER_OWNER");
                if (!(this.mDisplayManager == null || this.mDisplayManager.getDisplay(get1stDisplay().mDisplayId) == null)) {
                    sendNotificationForSwitch(this.mProjMode);
                }
            } else {
                if (isDesktopMode(this.mProjMode)) {
                    lightPhoneScreen();
                    HwPCUtils.log(TAG, "onSwitchUser: The current mode is DesktopMode");
                    this.mProjMode = ProjectionMode.PHONE_MODE;
                    HwPCUtils.setPcCastModeInServerEarly(this.mProjMode);
                    if (!HwPCUtils.enabledInPad()) {
                        bindUnbindService(false);
                    }
                    this.mAMS.togglePCMode(isDesktopMode(this.mProjMode), get1stDisplay().mDisplayId);
                    setUsePCModeMouseIconContext(false);
                    HwPCUtils.setPcCastModeInServer(false);
                    setPcCastingDisplayId(-1);
                    this.mVAssistCmdExecutor.notifyDesktopModeChanged(false, 0);
                    updateIMEWithHardKeyboardState(false);
                    Secure.putInt(this.mContext.getContentResolver(), "selected-proj-mode", 0);
                    if (get1stDisplay().mType == 2) {
                        this.mDDC.resetProjectionMode();
                    }
                    updateFingerprintSlideSwitch();
                    sendSwitchToStatusBar();
                    relaunchIMEDelay(0);
                    if (HwPCUtils.enabledInPad()) {
                        bindUnbindService(false);
                    }
                }
                if (this.mNm != null) {
                    this.mNm.cancelAll();
                    this.mHasSwitchNtf = false;
                }
            }
        }
    }

    private boolean isWiredDisplay(int displayId) {
        if (this.mDisplayManager == null) {
            this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
        }
        boolean z = false;
        if (this.mDisplayManager == null) {
            return false;
        }
        Display display = this.mDisplayManager.getDisplay(displayId);
        if (display == null) {
            return false;
        }
        int type = display.getType();
        if (type == 5 && "com.hpplay.happycast".equals(display.getOwnerPackageName())) {
            return true;
        }
        if (type == 5 && "com.huawei.works".equals(display.getOwnerPackageName())) {
            return true;
        }
        if (HwPCUtils.isWirelessProjectionEnabled()) {
            if (type == 2 || type == 3 || ((type == 5 || type == 4) && this.mSupportOverlay)) {
                z = true;
            }
            return z;
        }
        if (type == 2 || ((type == 5 || type == 4) && this.mSupportOverlay)) {
            z = true;
        }
        return z;
    }

    private void sendNotificationForSwitch(ProjectionMode projMode) {
        if (!HwPCUtils.enabledInPad() && this.mNm != null) {
            String mode;
            Notification.Builder builder = new Notification.Builder(this.mContext, "HW_PCM");
            builder.setSmallIcon(33751738);
            builder.setVisibility(-1);
            builder.setPriority(1);
            builder.setAppName(this.mContext.getString(33685941));
            boolean isConnectFromWelink = isConnectFromThirdApp(get1stDisplay().mDisplayId) == 2;
            if (isDesktopMode(projMode)) {
                mode = this.mContext.getString(33686116);
            } else {
                mode = this.mContext.getString(33686117);
            }
            builder.setContentTitle(mode);
            if (HwPCUtils.getIsWifiMode()) {
                if (!(this.mDisplayManager == null || this.mDisplayManager.getDisplay(get1stDisplay().mDisplayId) == null)) {
                    Display display = this.mDisplayManager.getDisplay(get1stDisplay().mDisplayId);
                    this.mWireLessDeviceName = String.format(this.mContext.getResources().getString(33686115), new Object[]{display.getName()});
                }
                builder.setContentText(this.mWireLessDeviceName);
            } else if (isConnectFromWelink) {
                this.mWireLessDeviceName = String.format(this.mContext.getResources().getString(33686115), new Object[]{this.mContext.getResources().getString(33686197)});
                builder.setContentText(this.mWireLessDeviceName);
            }
            builder.setContentIntent(PendingIntent.getBroadcastAsUser(this.mContext, 0, new Intent(ACTION_NOTIFY_OPEN_EASY_PROJECTION), 134217728, UserHandle.OWNER));
            builder.addAction(new Action.Builder(getResDrawableId(isDesktopMode(this.mProjMode)), getActionText(isDesktopMode(this.mProjMode)), PendingIntent.getBroadcastAsUser(this.mContext, 1, new Intent(ACTION_NOTIFY_SWITCH_MODE), 268435456, UserHandle.OWNER)).build());
            if (isNeedShowMKAction()) {
                builder.addAction(new Action.Builder(33751959, this.mContext.getString(33686114), PendingIntent.getBroadcastAsUser(this.mContext, 1, new Intent(ACTION_NOTIFY_SHOW_MK), 134217728, UserHandle.OWNER)).build());
            }
            if (HwPCUtils.getIsWifiMode() || isConnectFromWelink) {
                builder.addAction(new Action.Builder(33751957, this.mContext.getString(33686112), PendingIntent.getBroadcastAsUser(this.mContext, 1, new Intent(ACTION_NOTIFY_DISCONNECT), 134217728, UserHandle.OWNER)).build());
            }
            builder.setShowActionIcon(true);
            Notification notification = builder.getNotification();
            notification.flags |= 2;
            notification.flags |= 32;
            this.mNm.notify(TAG, 0, notification);
            this.mHasSwitchNtf = true;
        }
    }

    private CharSequence getActionText(boolean isDesktopMode) {
        if (isDesktopMode) {
            return this.mContext.getString(33686113);
        }
        return this.mContext.getString(33686111);
    }

    private int getResDrawableId(boolean isDesktopMode) {
        if (isDesktopMode) {
            return 33751958;
        }
        return 33751956;
    }

    private void collapsePanels() {
        StatusBarManagerService statusBarService = (StatusBarManagerService) ServiceManager.getService("statusbar");
        if (statusBarService != null) {
            statusBarService.collapsePanels();
        }
    }

    public void onDisplayChanged(int displayId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onDisplayChanged, displayId:");
        stringBuilder.append(displayId);
        HwPCUtils.log(str, stringBuilder.toString());
        if (!this.mProvisioned) {
        }
    }

    private void lockScreenWhenDisconnected() {
        if (!isScreenPowerOn()) {
            try {
                HwPCUtils.log(TAG, "Lock phone screen when PC displayer is disconnected.");
                ((PowerManager) this.mContext.getSystemService("power")).goToSleep(SystemClock.uptimeMillis(), 5, 0);
            } catch (Exception e) {
                HwPCUtils.log(TAG, "Error accured when locking screen as PC displayer is disconnected.");
            }
        }
    }

    public void onDisplayRemoved(int displayId) {
        if (HwPCUtils.enabledInPad()) {
            HwPCUtils.log(TAG, "onDisplayRemoved enabledInPad return");
        } else if (!HwPCUtils.enabled()) {
        } else {
            if (displayId == get2ndDisplay().mDisplayId) {
                HwPCUtils.log(TAG, "onDisplayRemoved ignore it if 2nd display removed.");
                if (get2ndDisplay().mType == 3) {
                    Global.putInt(this.mContext.getContentResolver(), WIRELESS_PROJECTION_STATE, 0);
                }
                get2ndDisplay().mDisplayId = -1;
                get2ndDisplay().mType = 0;
                return;
            }
            if (HwPCUtils.getIsWifiMode() || get1stDisplay().mType == 5) {
                Global.putInt(this.mContext.getContentResolver(), WIRELESS_PROJECTION_STATE, 0);
            }
            boolean isInPhoneMode = HwPCUtils.getPhoneDisplayID() == displayId && !isDesktopMode(this.mProjMode);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onDisplayRemoved, displayId:");
            stringBuilder.append(displayId);
            stringBuilder.append(", isInPhoneMode:");
            stringBuilder.append(isInPhoneMode);
            HwPCUtils.log(str, stringBuilder.toString());
            if (HwPCUtils.getPCDisplayID() != displayId && !isInPhoneMode) {
                HwPCUtils.log(TAG, "onDisplayRemoved, displayId is neither PC Display ID nor Phone Display ID.");
            } else if (this.mProvisioned || displayId == get1stDisplay().mDisplayId) {
                lockScreenWhenDisconnected();
                HwPCUtils.setPcCastModeInServerEarly(ProjectionMode.PHONE_MODE);
                if (this.mProjMode == ProjectionMode.DESKTOP_MODE) {
                    bdReportDiffSrcStatus(false);
                } else {
                    bdReportSameSrcStatus(false);
                }
                if (this.mNm != null) {
                    HwPCUtils.log(TAG, "onDisplayRemoved cancel notification.");
                    this.mNm.cancelAll();
                    this.mHasSwitchNtf = false;
                }
                if (this.mPcMultiDisplayMgr.is4KHdmi1stDisplayRemoved(displayId)) {
                    HwPCUtils.log(TAG, "onDisplayRemoved ignore it when dp is on.");
                    get1stDisplay().mDisplayId = -1;
                    if (isInPhoneMode) {
                        HwPCUtils.setPhoneDisplayID(-1);
                    } else {
                        bindUnbindService(false);
                        setUsePCModeMouseIconContext(false);
                        updateIMEWithHardKeyboardState(false);
                    }
                    HwPCUtils.setPcCastModeInServer(false);
                    setPcCastingDisplayId(-1);
                    this.mVAssistCmdExecutor.notifyDesktopModeChanged(false, 0);
                    setDesktopModeToAudioService(-1);
                    updateFingerprintSlideSwitch();
                    return;
                }
                get1stDisplay().mDisplayId = -1;
                get1stDisplay().mType = 0;
                if (isInPhoneMode) {
                    HwPCUtils.setPhoneDisplayID(-1);
                } else {
                    bindUnbindService(false);
                    setUsePCModeMouseIconContext(false);
                    updateIMEWithHardKeyboardState(false);
                }
                HwPCUtils.setPcCastModeInServer(false);
                setPcCastingDisplayId(-1);
                this.mVAssistCmdExecutor.notifyDesktopModeChanged(false, 0);
                setDesktopModeToAudioService(-1);
                updateFingerprintSlideSwitch();
                Global.putInt(this.mContext.getContentResolver(), "is_display_device_connected", 1);
                this.mPcMultiDisplayMgr.handlelstDisplayInDisplayRemoved();
                HwPCDataReporter.getInstance().stopPCDisplay();
                exitDesktopModeForMk();
            } else {
                HwPCUtils.log(TAG, "onDisplayRemoved not permitted before setup or not scheduleDisplayAdded");
            }
        }
    }

    private void onDisplayAdded(int displayId) {
        if (HwPCUtils.enabled()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onDisplayAdded, displayId:");
            stringBuilder.append(displayId);
            HwPCUtils.log(str, stringBuilder.toString());
            if (!this.mProvisioned) {
                HwPCUtils.log(TAG, "onDisplayAdded not permitted before setup");
            } else if (displayId == -1 || displayId == 0) {
                HwPCUtils.log(TAG, "context is null or is default display");
            } else if (HwPCUtils.enabledInPad() && this.mUserId != 0) {
                HwPCUtils.log(TAG, "switchProjMode failed! currentUser is not UserHandle.USER_OWNER");
            } else if (HwPCUtils.enabledInPad() && isMonkeyRunning()) {
                HwPCUtils.log(TAG, "onDisplayAdded isMonkeyRunning return !");
            } else {
                if (isWifiPCMode(displayId)) {
                    HwPCUtils.bdReport(this.mContext, 10057, "");
                } else if (isWiredDisplay(displayId)) {
                    HwPCUtils.bdReport(this.mContext, IDisplayEngineService.DE_ACTION_PG_OFFICE_FRONT, "");
                } else {
                    HwPCUtils.bdReport(this.mContext, 10057, "");
                }
                bdReportConnectDisplay(displayId);
                if (isWiredDisplay(displayId)) {
                    if (isConnectFromThirdApp(displayId) == 2) {
                        Global.putInt(this.mContext.getContentResolver(), WIRELESS_PROJECTION_STATE, 1);
                    }
                    Global.putInt(this.mContext.getContentResolver(), "is_display_device_connected", 0);
                    this.mPcMultiDisplayMgr.checkInitialDpConnectAfterBoot(displayId);
                    if (this.mPcMultiDisplayMgr.handleTwoDisplaysInDisplayAdded(displayId)) {
                        if (HwPCUtils.enabledInPad()) {
                            if (this.mPadPCDisplayIsRemoved) {
                                get1stDisplay().mDisplayId = displayId;
                                this.mPadPCDisplayIsRemoved = false;
                                HwPCUtils.log(TAG, "PadPCDisplayIsRemoved return.");
                                return;
                            } else if (!(deviceIsProvisioned() && isUserSetupComplete())) {
                                HwPCUtils.log(TAG, "Startup Guide is not Complete");
                                return;
                            }
                        }
                        if (Secure.getInt(this.mContext.getContentResolver(), "selected-proj-mode", HwPCUtils.enabledInPad() ^ 1) == 1) {
                            this.mProjMode = ProjectionMode.DESKTOP_MODE;
                        } else {
                            this.mProjMode = ProjectionMode.PHONE_MODE;
                            if (isExclusiveKeyboardConnected() && HwPCUtils.enabledInPad()) {
                                showEnterDesktopAlertDialog(getCurrentContext(), true);
                            }
                        }
                        get1stDisplay().mDisplayId = displayId;
                        get1stDisplay().mType = this.mPcMultiDisplayMgr.getDisplayType(displayId);
                        HwPCUtils.setIsWifiMode(isWifiPCMode(get1stDisplay().mDisplayId));
                        boolean exist = systemUIExist() && explorerExist();
                        boolean enterDesktopMode = exist && isDesktopMode(this.mProjMode) && this.mUserId == 0;
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("onDisplayAdded mProjMode ");
                        stringBuilder2.append(this.mProjMode);
                        stringBuilder2.append(", mConnectedInputDevices = ");
                        stringBuilder2.append(this.mConnectedInputDevices);
                        stringBuilder2.append(", exist =");
                        stringBuilder2.append(exist);
                        stringBuilder2.append(", mUserId = ");
                        stringBuilder2.append(this.mUserId);
                        stringBuilder2.append(", enterDesktopMode =");
                        stringBuilder2.append(enterDesktopMode);
                        HwPCUtils.log(str2, stringBuilder2.toString());
                        if (this.mDisplayManager != null) {
                            Display display = this.mDisplayManager.getDisplay(get1stDisplay().mDisplayId);
                            if (display != null) {
                                this.mPCDisplayInfo = new DisplayInfo();
                                if (display.getDisplayInfo(this.mPCDisplayInfo)) {
                                    int width = this.mPCDisplayInfo.getNaturalWidth();
                                    int height = this.mPCDisplayInfo.getNaturalHeight();
                                    String str3 = TAG;
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("width = ");
                                    stringBuilder3.append(width);
                                    stringBuilder3.append(", height = ");
                                    stringBuilder3.append(height);
                                    HwPCUtils.log(str3, stringBuilder3.toString());
                                    if (width > RESO_1080P) {
                                        this.mIsDisplayLargerThan1080p = true;
                                    } else {
                                        this.mIsDisplayLargerThan1080p = false;
                                    }
                                }
                            }
                        }
                        Message msg;
                        if (enterDesktopMode) {
                            HwPCUtils.setPCDisplayID(displayId);
                            if (!isDesktopMode(this.mProjMode)) {
                                Secure.putInt(this.mContext.getContentResolver(), "selected-proj-mode", 1);
                            }
                            if (!HwPCUtils.getIsWifiMode()) {
                                autoLaunchMK();
                            } else if (this.mIsWifiBroadDone) {
                                autoLaunchMK();
                                this.mIsWifiBroadDone = false;
                            }
                            this.mProjMode = ProjectionMode.DESKTOP_MODE;
                            HwPCUtils.setPcCastModeInServerEarly(this.mProjMode);
                            HwPCUtils.setPcCastModeInServer(true);
                            setPcCastingDisplayId(get1stDisplay().mDisplayId);
                            this.mVAssistCmdExecutor.notifyDesktopModeChanged(true, get1stDisplay().mDisplayId);
                            updateIMEWithHardKeyboardState(true);
                            saveRotationInPad();
                            this.mAMS.freezeOrThawRotationInPcMode();
                            this.mAMS.togglePCMode(isDesktopMode(this.mProjMode), get1stDisplay().mDisplayId);
                            setUsePCModeMouseIconContext(true);
                            if (this.restartByUnlock2SetAnimTime) {
                                this.mPCBeforeBootAnimTime = 1500;
                            } else {
                                this.mPCBeforeBootAnimTime = TIME_DISPALY_ADD_BEFORE_BOOT_ANIM;
                            }
                            bindUnbindService(true);
                            this.restartByUnlock2SetAnimTime = false;
                            this.mHandler.removeMessages(1);
                            msg = this.mHandler.obtainMessage(1);
                            msg.obj = this.mProjMode;
                            this.mHandler.sendMessage(msg);
                            bdReportSameSrcStatus(true);
                            setDesktopModeToAudioService(1);
                            this.mIsDisplayAddedAfterSwitch = true;
                            backToHomeInDefaultDisplay(get1stDisplay().mDisplayId);
                            uploadPcDisplaySizePro();
                            enableFingerprintSlideSwitch();
                            lightPhoneScreen();
                            if (HwPCUtils.enabledInPad()) {
                                setFocusedPCDisplayId("enterDesktop");
                            }
                            relaunchIMEDelay(2000);
                            if (HwPCUtils.enabledInPad()) {
                                this.mHandler.removeMessages(17);
                                this.mHandler.sendMessage(this.mHandler.obtainMessage(17));
                            }
                            enterDesktopModeForMk();
                        } else {
                            HwPCUtils.setPhoneDisplayID(displayId);
                            if (HwPCUtils.enabledInPad()) {
                                HwPCUtils.log(TAG, "onDisplayAdded there is something wrong when enter PAD PC mode !");
                                return;
                            }
                            lightPhoneScreen();
                            this.mProjMode = ProjectionMode.PHONE_MODE;
                            HwPCUtils.setPcCastModeInServerEarly(this.mProjMode);
                            this.mAMS.togglePCMode(isDesktopMode(this.mProjMode), get1stDisplay().mDisplayId);
                            setUsePCModeMouseIconContext(false);
                            HwPCUtils.setPcCastModeInServer(false);
                            setPcCastingDisplayId(-1);
                            this.mVAssistCmdExecutor.notifyDesktopModeChanged(false, 0);
                            updateIMEWithHardKeyboardState(false);
                            this.mHandler.removeMessages(1);
                            msg = this.mHandler.obtainMessage(1);
                            msg.obj = this.mProjMode;
                            this.mHandler.sendMessage(msg);
                            bdReportDiffSrcStatus(true);
                            setDesktopModeToAudioService(0);
                            updateFingerprintSlideSwitch();
                            exitDesktopModeForMk();
                        }
                        scheduleRestoreApps(displayId);
                        sendSwitchToStatusBar();
                        HwPCDataReporter.getInstance().startPCDisplay();
                        DecisionUtil.executeEnterProjectionEvent(this.mContext);
                        Display display2 = this.mDisplayManager.getDisplay(displayId);
                        if (display2 != null && display2.getType() == 5 && "com.hpplay.happycast".equals(display2.getOwnerPackageName())) {
                            HwPCUtils.bdReport(this.mContext, 10061, "Enter projection from hppcast.");
                        }
                        return;
                    }
                    HwPCUtils.log(TAG, "it's not 1st display added, need not continue.");
                    return;
                }
                HwPCUtils.log(TAG, "is not a wired display.");
            }
        }
    }

    private void autoLaunchMK() {
        boolean z = true;
        if (Secure.getInt(this.mContext.getContentResolver(), "guide-started", 0) != 1) {
            z = false;
        }
        boolean guideStarted = z;
        if ((HwPCUtils.getIsWifiMode() || guideStarted || isConnectFromThirdApp(get1stDisplay().mDisplayId) == 2) && !existMouseInputDevices()) {
            this.mHandler.postDelayed(new Runnable() {
                public void run() {
                    HwPCManagerService.this.sendShowMkMessage();
                }
            }, 2000);
        }
    }

    public void LaunchMKForWifiMode() {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            if (get1stDisplay().mDisplayId != -1) {
                autoLaunchMK();
                this.mIsWifiBroadDone = false;
            } else {
                this.mIsWifiBroadDone = true;
            }
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("LaunchMKForWifiMode checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
    }

    private void enterDesktopModeForMk() {
        HwPCUtils.log(TAG, "enterDesktopModeForMk   ");
        if (this.mHwPolicy == null) {
            this.mHwPolicy = (HwPhoneWindowManager) LocalServices.getService(WindowManagerPolicy.class);
        }
        this.mPCMkManager = HwPCMkManager.getInstance(this.mContext);
        this.mPCMkManager.initCrop(this.mContext, this);
        this.mPCMkManager.startSendEventThread();
        this.mPCMkManager.updatePointerAxis(getPointerCoordinateAxis());
    }

    private void exitDesktopModeForMk() {
        HwPCUtils.log(TAG, "exitDesktopModeForMk   ");
        if (this.mPCMkManager != null) {
            this.mPCMkManager.stopSendEventThreadAndRelease();
        }
    }

    private void backToHomeInDefaultDisplay(int curDisplayId) {
        if (HwPCUtils.enabledInPad()) {
            HwPCUtils.log(TAG, "backToHomeInDefaultDisplay");
            Intent homeIntent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME").addCategory("android.intent.category.DEFAULT");
            homeIntent.addFlags(268435456);
            try {
                this.mContext.startActivity(homeIntent);
            } catch (ActivityNotFoundException e) {
                HwPCUtils.log(TAG, "ActivityMovedToDesktopDisplay fail to start go home");
            }
        }
    }

    private void setUsePCModeMouseIconContext(boolean pcmode) {
        HwInputManagerLocalService inputManager = (HwInputManagerLocalService) LocalServices.getService(HwInputManagerLocalService.class);
        if (inputManager == null) {
            return;
        }
        if (pcmode) {
            if (this.mDisplayManager == null) {
                this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
            }
            if (this.mDisplayManager != null) {
                Display display = null;
                for (Display dis : this.mDisplayManager.getDisplays()) {
                    if (dis != null && HwPCUtils.isValidExtDisplayId(dis.getDisplayId())) {
                        display = dis;
                        break;
                    }
                }
                if (display != null) {
                    Context context = this.mContext.createDisplayContext(display);
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("setUsePCModeMouseIconContext displayId = ");
                    stringBuilder.append(display.toString());
                    HwPCUtils.log(str, stringBuilder.toString());
                    inputManager.setExternalDisplayContext(context);
                    return;
                }
                return;
            }
            return;
        }
        inputManager.setExternalDisplayContext(null);
    }

    private void sendSwitchMsg() {
        this.mHandler.removeMessages(4);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(4));
    }

    public void switchProjMode() {
        String str;
        StringBuilder stringBuilder;
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("switchProjMode, mProjMode ");
            stringBuilder.append(this.mProjMode);
            stringBuilder.append(", isEnterDesktopModeRemembered:");
            stringBuilder.append(isEnterDesktopModeRemembered());
            stringBuilder.append(" isExitDesktopModeRemembered:");
            stringBuilder.append(isExitDesktopModeRemembered());
            HwPCUtils.log(str, stringBuilder.toString());
            if (!HwPCUtils.enabledInPad()) {
                HwPCUtils.log(TAG, "Not enabledInPad()");
                sendSwitchMsg();
                return;
            } else if (this.mUserId != 0) {
                HwPCUtils.log(TAG, "switchProjMode failed! currentUser is not UserHandle.USER_OWNER");
                HwPCDataReporter.getInstance().reportFailSwitchEvent(3, this.mProjMode.ordinal(), this.mPCDisplayInfo);
                return;
            } else {
                int i = 0;
                if (isCalling()) {
                    if (isDesktopMode(this.mProjMode)) {
                        i = get1stDisplay().mDisplayId;
                    }
                    showCallingToast(i);
                    HwPCUtils.log(TAG, "switchProjMode failed! in Calling");
                    HwPCDataReporter.getInstance().reportFailSwitchEvent(4, this.mProjMode.ordinal(), this.mPCDisplayInfo);
                    return;
                }
                if (isDesktopMode(this.mProjMode)) {
                    showExitDesktopAlertDialog(getCurrentContext(), false);
                } else {
                    showEnterDesktopAlertDialog(getCurrentContext(), false);
                }
                return;
            }
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("switchProjMode failed ");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
        HwPCDataReporter.getInstance().reportFailSwitchEvent(2, this.mProjMode.ordinal(), this.mPCDisplayInfo);
    }

    private void sendSwitchToStatusBar() {
        if (HwPCUtils.enabledInPad()) {
            HwPCUtils.log(TAG, "sendSwitchToStatusBar!");
            Intent intent = new Intent();
            intent.setAction(ACTION_NOTIFY_CHANGE_STATUS_BAR);
            this.mContext.sendBroadcast(intent, PERMISSION_BROADCAST_CHANGE_STATUS_BAR);
        }
    }

    private void refreshNotifications() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("refreshNotifications mHasSwitchNtf = ");
        stringBuilder.append(this.mHasSwitchNtf);
        HwPCUtils.log(str, stringBuilder.toString());
        this.mHandler.removeMessages(5);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(5));
    }

    private static int whichInputDevice(InputDevice device) {
        int ret = 0;
        if (device != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("device=");
            stringBuilder.append(device);
            stringBuilder.append(", souces = ");
            stringBuilder.append(device.getSources());
            HwPCUtils.log(str, stringBuilder.toString());
        }
        if (device == null || !device.isExternal()) {
            HwPCUtils.log(TAG, "whichInputDevice unkown input device");
            return 0;
        }
        if ((device.getSources() & 139270) != 0) {
            ret = 0 | 1;
        }
        if ((device.getSources() & 257) != 0) {
            return ret | 2;
        }
        return ret;
    }

    private boolean isExclusiveKeyboardConnected() {
        int[] devices = InputDevice.getDeviceIds();
        for (InputDevice device : devices) {
            InputDevice device2 = InputDevice.getDevice(device2);
            if (device2 != null && isExclusiveKeyboard(device2)) {
                return true;
            }
        }
        return false;
    }

    private boolean isExclusiveKeyboard(InputDevice inputDevice) {
        if (inputDevice == null) {
            HwPCUtils.log(TAG, "isExclusiveKeyboard=false");
            return false;
        }
        int keyboardProductId = inputDevice.getProductId();
        int keyboardVendorId = inputDevice.getVendorId();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Keyboard ProductId:");
        stringBuilder.append(keyboardProductId);
        stringBuilder.append(" VendorId:");
        stringBuilder.append(keyboardVendorId);
        HwPCUtils.log(str, stringBuilder.toString());
        if (KEYBOARD_PRODUCT_ID.length != KEYBOARD_VENDOR_ID.length) {
            return false;
        }
        int i = 0;
        while (i < KEYBOARD_PRODUCT_ID.length) {
            if (keyboardProductId == KEYBOARD_PRODUCT_ID[i] && keyboardVendorId == KEYBOARD_VENDOR_ID[i]) {
                HwPCUtils.log(TAG, "isExclusiveKeyboard=true");
                return true;
            }
            i++;
        }
        HwPCUtils.log(TAG, "isExclusiveKeyboard=false");
        return false;
    }

    private void sendShowMkMessage() {
        HwPCUtils.log(TAG, "sendShowMkMessage todo launch touchpad");
        if (isNeedShowMKAction()) {
            this.mHandler.removeMessages(13);
            this.mHandler.sendMessage(this.mHandler.obtainMessage(13));
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unable to send ShowMk message, existMouse:");
        stringBuilder.append(existMouseInputDevices());
        stringBuilder.append(" get1stDisplay().mDisplayId:");
        stringBuilder.append(get1stDisplay().mDisplayId);
        stringBuilder.append(" mProjMode:");
        stringBuilder.append(this.mProjMode);
        HwPCUtils.log(str, stringBuilder.toString());
    }

    private void sendOpenEasyProjectionMessage() {
        this.mHandler.removeMessages(23);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(23));
    }

    private void launchMK() {
        if (!HwPCUtils.enabledInPad()) {
            HwPCUtils.log(TAG, "launchMK todo start touchpad activiy");
            if (isNeedShowMKAction()) {
                Intent intent = new Intent();
                intent.addFlags(268435456);
                intent.setComponent(this.mTouchPadComponent);
                try {
                    this.mContext.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    HwPCUtils.log(TAG, "fail to start mk activity");
                }
                return;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("cannot launch MK, existMouse:");
            stringBuilder.append(existMouseInputDevices());
            stringBuilder.append(" get1stDisplay().mDisplayId:");
            stringBuilder.append(get1stDisplay().mDisplayId);
            HwPCUtils.log(str, stringBuilder.toString());
        }
    }

    private void openEasyProjection() {
        Intent intent = new Intent();
        intent.addFlags(268435456);
        if (HwPCUtils.isWirelessProjectionEnabled()) {
            intent.setComponent(this.mInstructionComponentWirelessEnabled);
            HwPCUtils.bdReport(this.mContext, 10045, "");
        } else {
            intent.setComponent(this.mInstructionComponent);
            HwPCUtils.bdReport(this.mContext, 10046, "");
        }
        this.mContext.startActivity(intent);
    }

    private void sendUninstallAppMessage(String packageName) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendUninstallAppMessage: ");
        stringBuilder.append(packageName);
        HwPCUtils.log(str, stringBuilder.toString());
        Message msg = this.mHandler.obtainMessage(16);
        msg.obj = packageName;
        this.mHandler.sendMessage(msg);
    }

    private boolean isNeedShowMKAction() {
        if (this.mSupportTouchPad && this.mProjMode == ProjectionMode.DESKTOP_MODE && get1stDisplay().mDisplayId != -1) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("cannot show mk notify, get1stDisplay().mDisplayId:");
        stringBuilder.append(get1stDisplay().mDisplayId);
        stringBuilder.append(" existMouse:");
        stringBuilder.append(existMouseInputDevices());
        stringBuilder.append(" mSupportTouchPad = ");
        stringBuilder.append(this.mSupportTouchPad);
        stringBuilder.append(" mProjMode = ");
        stringBuilder.append(this.mProjMode);
        HwPCUtils.log(str, stringBuilder.toString());
        return false;
    }

    private boolean existMouseInputDevices() {
        return (this.mConnectedInputDevices & 1) != 0;
    }

    private boolean systemUIExist() {
        boolean exist = this.mContext.getPackageManager().resolveService(new Intent().setComponent(this.mSystemUIComponent), 65536) != null;
        if (!exist) {
            HwPCUtils.log(TAG, "systemUI does not Exist!!!");
        }
        return exist;
    }

    private boolean explorerExist() {
        boolean exist = this.mContext.getPackageManager().resolveService(new Intent().setComponent(this.mExplorerComponent), 65536) != null;
        if (!exist) {
            HwPCUtils.log(TAG, "Explorer does not Exist!!!");
        }
        return exist;
    }

    private boolean checkCallingPermission(String permission) {
        return this.mAMS.checkPermission(permission, Binder.getCallingPid(), UserHandle.getAppId(Binder.getCallingUid())) == 0;
    }

    public boolean getCastMode() {
        return isDesktopMode(this.mProjMode) && HwPCUtils.isValidExtDisplayId(get1stDisplay().mDisplayId);
    }

    public int getPackageSupportPcState(String packageName) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getPackageSupportPcState:");
        stringBuilder.append(packageName);
        HwPCUtils.log(str, stringBuilder.toString());
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            return WhiteListAppStrategyManager.getInstance(this.mContext).getAppSupportPCState(packageName);
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("getPackageSupportPcState checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
        return -1;
    }

    public List<String> getAllSupportPcAppList() {
        HwPCUtils.log(TAG, "getAllSupportPcAppList");
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            return WhiteListAppStrategyManager.getInstance(this.mContext).getAllSupportPcAppList();
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getAllSupportPcAppList checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
        return null;
    }

    public List<String> getMutiResumeAppList() {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            return WhiteListAppStrategyManager.getInstance(this.mContext).getMutiResumeAppList();
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getMutiResumeAppList checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
        return null;
    }

    public void relaunchIMEIfNecessary() {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            this.mHandler.removeMessages(14);
            this.mHandler.sendMessage(this.mHandler.obtainMessage(14));
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("relaunchIMEIfNecessary checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
    }

    private void relaunchIMEDelay(int delayMillis) {
        if (HwPCUtils.enabledInPad()) {
            HwPCUtils.log(TAG, "relaunchIMEDelay");
            this.mHandler.removeMessages(14);
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(14), (long) delayMillis);
        }
    }

    private void updateDisplayOverrideConfiguration(int display, int delayMillis) {
        if (HwPCUtils.enabledInPad()) {
            HwPCUtils.log(TAG, "updateDisplayOverrideConfiguration");
            this.mHandler.removeMessages(18);
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(18, display, 0), (long) delayMillis);
        }
    }

    private void doUpdateDisplayOverrideConfiguration(int displayid) {
        this.mAMS.updateDisplayOverrideConfiguration(null, displayid);
    }

    private void doRelaunchIMEIfNecessary() {
        this.mAMS.relaunchIMEIfNecessary();
    }

    public void hwRestoreTask(int taskId, float x, float y) {
        if (this.mAMS.checkTaskId(taskId) || checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            this.mAMS.hwRestoreTask(taskId, x, y);
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("hwRestoreTask checktaskId failed:");
        stringBuilder.append(taskId);
        HwPCUtils.log(str, stringBuilder.toString());
    }

    public void hwResizeTask(int taskId, Rect bounds) {
        if (this.mAMS.checkTaskId(taskId) || checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            this.mAMS.hwResizeTask(taskId, bounds);
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("hwResizeTask checktaskId failed:");
        stringBuilder.append(taskId);
        HwPCUtils.log(str, stringBuilder.toString());
    }

    public int getWindowState(IBinder token) {
        return this.mAMS.getWindowState(token);
    }

    public HwRecentTaskInfo getHwRecentTaskInfo(int taskId) {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            return this.mAMS.getHwRecentTaskInfo(taskId);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getHwRecentTaskInfo checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
        return null;
    }

    public void registerHwTaskStackListener(ITaskStackListener listener) {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            this.mAMS.registerHwTaskStackListener(listener);
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("registerHwTaskStackListener checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
    }

    public void unRegisterHwTaskStackListener(ITaskStackListener listener) {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            this.mAMS.unRegisterHwTaskStackListener(listener);
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unRegisterHwTaskStackListener checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
    }

    public Bitmap getDisplayBitmap(int displayId, int width, int height) {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            return this.mAMS.getDisplayBitmap(displayId, width, height);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getDisplayBitmap checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
        return null;
    }

    public void registHwSystemUIController(Messenger messenger) {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            this.mMessenger = messenger;
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("registHwSystemUIController checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
    }

    private Message getMessage(int what) {
        Message message = Message.obtain();
        message.what = what;
        return message;
    }

    public void showTopBar() {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            try {
                if (this.mMessenger != null) {
                    this.mMessenger.send(getMessage(1));
                }
            } catch (RemoteException e) {
                HwPCUtils.log("showTopBar", "RemoteException");
            }
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("showTopBar checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
    }

    public void showStartMenu() {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            try {
                if (this.mMessenger != null) {
                    this.mMessenger.send(getMessage(2));
                }
            } catch (RemoteException e) {
                HwPCUtils.log("showStartMenu", "RemoteException");
            }
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("showStartMenu checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
    }

    public void screenshotPc() {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            try {
                if (this.mMessenger != null) {
                    this.mMessenger.send(getMessage(3));
                }
            } catch (RemoteException e) {
                HwPCUtils.log("screenshotPc", "RemoteException");
            }
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("screenshotPc checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
    }

    public void userActivityOnDesktop() {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            try {
                if (this.mMessenger != null) {
                    this.mMessenger.send(getMessage(7));
                }
            } catch (RemoteException e) {
                HwPCUtils.log("userActivityOnDesktop", "RemoteException");
            }
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("userActivityOnDesktop checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
    }

    public void closeTopWindow() {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            try {
                if (this.mMessenger != null) {
                    this.mMessenger.send(getMessage(4));
                }
            } catch (RemoteException e) {
                HwPCUtils.log("closeTopWindow", "RemoteException");
            }
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("closeTopWindow checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
    }

    public void triggerSwitchTaskView(boolean show) {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            try {
                if (this.mMessenger != null) {
                    if (show) {
                        this.mMessenger.send(getMessage(5));
                    } else {
                        this.mMessenger.send(getMessage(6));
                    }
                }
            } catch (RemoteException e) {
                HwPCUtils.log("screenshotPc", "RemoteException");
            }
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("triggerSwitchTaskView checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
    }

    public Bitmap getTaskThumbnailEx(int id) {
        if (!checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getTaskThumbnailEx checkCallingPermission failed");
            stringBuilder.append(Binder.getCallingPid());
            HwPCUtils.log(str, stringBuilder.toString());
            return null;
        } else if (this.mAMS != null) {
            return this.mAMS.getTaskThumbnailOnPCMode(id);
        } else {
            HwPCUtils.log(TAG, "getTaskThumbnailEx failed , ams is null");
            return null;
        }
    }

    public void toggleHome() {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            this.mAMS.toggleHome();
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("toggleHome checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
    }

    public boolean injectInputEventExternal(InputEvent event, int mode) {
        if (!checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("injectInputEventExternal checkCallingPermission failed");
            stringBuilder.append(Binder.getCallingPid());
            HwPCUtils.log(str, stringBuilder.toString());
            return false;
        } else if (this.mHwPolicy == null || this.mPCMkManager == null) {
            HwPCUtils.log(TAG, " injectInputEventExternal policy or PCMkManager is null");
            return false;
        } else if (mode == 2) {
            HwInputManagerLocalService inputManager = (HwInputManagerLocalService) LocalServices.getService(HwInputManagerLocalService.class);
            if (inputManager == null) {
                return false;
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("devicetest injectInputEventExternal event = ");
            stringBuilder2.append(event);
            stringBuilder2.append(" mode = ");
            stringBuilder2.append(mode);
            HwPCUtils.log(str2, stringBuilder2.toString());
            return inputManager.injectInputEvent(event, 0);
        } else {
            WindowState focusedWin = (WindowState) this.mHwPolicy.getTopFullscreenWindow();
            if (!(event instanceof MotionEvent) || focusedWin == null || focusedWin.getDisplayId() != 0 || focusedWin.getAttrs() == null || focusedWin.getAttrs().getTitle() == null || !"com.huawei.desktop.systemui/com.huawei.systemui.mk.activity.ImitateActivity".equalsIgnoreCase(focusedWin.getAttrs().getTitle().toString())) {
                return false;
            }
            boolean shouldDrop = false;
            try {
                shouldDrop = HwWindowManager.shouldDropMotionEventForTouchPad(((MotionEvent) event).getX(), ((MotionEvent) event).getY());
            } catch (NullPointerException e) {
                HwPCUtils.log(TAG, "injectInputEventExternal NullPointerException");
            }
            if (shouldDrop) {
                HwPCUtils.log(TAG, "injectInputEventExternal should drop MotionEvent for TouchPad");
                return false;
            }
            if (this.mPCMkManager.sendEvent((MotionEvent) event, focusedWin.getVisibleFrameLw(), focusedWin.getDisplayFrameLw(), mode)) {
                return true;
            }
            return false;
        }
    }

    public void registerExternalPointerEventListener() {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            this.mAMS.registerExternalPointerEventListener(this.mPointerListener);
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("registerExternalPointerEventListener checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
    }

    public void unregisterExternalPointerEventListener() {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            this.mAMS.unregisterExternalPointerEventListener(this.mPointerListener);
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unregisterExternalPointerEventListener checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
    }

    public float[] getPointerCoordinateAxis() {
        float[] axis = new float[2];
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            axis[0] = this.mAxisX;
            axis[1] = this.mAxisY;
            return axis;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getPointerCoordinateAxis checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
        return axis;
    }

    private Context getDisplayContext(Context context, int displayId) {
        Display targetDisplay = ((DisplayManager) context.getSystemService("display")).getDisplay(displayId);
        if (targetDisplay == null) {
            return null;
        }
        return context.createDisplayContext(targetDisplay);
    }

    public void saveAppIntent(List<Intent> intents) {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            this.mHandler.removeMessages(15);
            this.mHandler.sendMessage(this.mHandler.obtainMessage(15, intents));
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("saveAppIntent checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
    }

    private void scheduleRestoreApps(int displayId) {
        if (!this.mIntentList.isEmpty()) {
            this.mAlarmManager.cancel(this.mAlarmListener);
            this.mHandler.removeMessages(10);
            this.mHandler.removeMessages(9);
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(9, displayId, 0), HwArbitrationDEFS.NotificationMonitorPeriodMillis);
        }
    }

    private void handleRestoreApps(int displayId) {
        int N = this.mIntentList.size();
        for (int i = 0; i < N; i++) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(10, displayId, 0, this.mIntentList.get(i)), (long) (i * 800));
        }
        this.mIntentList.clear();
    }

    private void restoreApp(int displayId, Intent intent) {
        Context displayContext = getDisplayContext(this.mContext, displayId);
        if (!(displayContext == null || intent == null)) {
            try {
                if (HwPCUtils.enabledInPad() && intent.getComponent() != null && "com.android.incallui".equals(intent.getComponent().getPackageName())) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(" restoreApp skip intent:");
                    stringBuilder.append(intent);
                    stringBuilder.append(",displayId:");
                    stringBuilder.append(displayId);
                    HwPCUtils.log(str, stringBuilder.toString());
                    return;
                }
                intent.addFlags(268435456);
                displayContext.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                HwPCUtils.log(TAG, "startActivity error.");
            }
        }
    }

    public void lockScreen(boolean lock) {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            try {
                if (this.mMessenger != null) {
                    Message msg = getMessage(8);
                    msg.arg1 = lock;
                    this.mMessenger.send(msg);
                }
            } catch (RemoteException e) {
                HwPCUtils.log("lockScreen", "RemoteException");
            }
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("lockScreen checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
    }

    private void bdReportSameSrcStatus(boolean isConnected) {
        if (isConnected) {
            HwPCUtils.bdReport(this.mContext, IDisplayEngineService.DE_ACTION_PG_BROWSER_FRONT, "same src is connected");
        } else {
            HwPCUtils.bdReport(this.mContext, IDisplayEngineService.DE_ACTION_PG_BROWSER_FRONT, "same src is disconnected");
        }
    }

    private void bdReportDiffSrcStatus(boolean isConnected) {
        if (isConnected) {
            HwPCUtils.bdReport(this.mContext, IDisplayEngineService.DE_ACTION_PG_3DGAME_FRONT, "diff src is connected");
            if (HwPCUtils.enabledInPad()) {
                HwPCUtils.bdReport(this.mContext, 10026, "enter");
                return;
            }
            return;
        }
        HwPCUtils.bdReport(this.mContext, IDisplayEngineService.DE_ACTION_PG_3DGAME_FRONT, "diff src is disconnected");
        if (HwPCUtils.enabledInPad()) {
            HwPCUtils.bdReport(this.mContext, 10026, "exit");
        }
    }

    private void bdReportConnectDisplay(int displayId) {
        if (this.mDisplayManager == null) {
            this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
        }
        Display display = this.mDisplayManager.getDisplay(displayId);
        if (display != null) {
            Context context;
            StringBuilder stringBuilder;
            String name = display.getName();
            int type = display.getType();
            if (type != 0) {
                switch (type) {
                    case 2:
                        context = this.mContext;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Display<");
                        stringBuilder.append(name);
                        stringBuilder.append("> type is HDMI");
                        HwPCUtils.bdReport(context, IDisplayEngineService.DE_ACTION_PG_VIDEO_FRONT, stringBuilder.toString());
                        break;
                    case 3:
                        context = this.mContext;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Display<");
                        stringBuilder.append(name);
                        stringBuilder.append("> type is WIFI");
                        HwPCUtils.bdReport(context, IDisplayEngineService.DE_ACTION_PG_VIDEO_FRONT, stringBuilder.toString());
                        break;
                    case 4:
                        context = this.mContext;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Display<");
                        stringBuilder.append(name);
                        stringBuilder.append("> type is OVERLAY");
                        HwPCUtils.bdReport(context, IDisplayEngineService.DE_ACTION_PG_VIDEO_FRONT, stringBuilder.toString());
                        break;
                    case 5:
                        context = this.mContext;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Display<");
                        stringBuilder.append(name);
                        stringBuilder.append("> type is VIRTUAL");
                        HwPCUtils.bdReport(context, IDisplayEngineService.DE_ACTION_PG_VIDEO_FRONT, stringBuilder.toString());
                        break;
                }
            }
            context = this.mContext;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Display<");
            stringBuilder.append(name);
            stringBuilder.append("> type is UNKNOWN");
            HwPCUtils.bdReport(context, IDisplayEngineService.DE_ACTION_PG_VIDEO_FRONT, stringBuilder.toString());
            Point size = new Point();
            display.getRealSize(size);
            context = this.mContext;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Display<");
            stringBuilder2.append(name);
            stringBuilder2.append("> width:");
            stringBuilder2.append(size.x);
            stringBuilder2.append(" height:");
            stringBuilder2.append(size.y);
            HwPCUtils.bdReport(context, IDisplayEngineService.DE_ACTION_PG_LAUNCHER_FRONT, stringBuilder2.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(size.x);
            stringBuilder.append("");
            BigInteger biWidth = new BigInteger(stringBuilder.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(size.y);
            stringBuilder2.append("");
            BigInteger value = biWidth.gcd(new BigInteger(stringBuilder2.toString()));
            Context context2 = this.mContext;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Display<");
            stringBuilder3.append(name);
            stringBuilder3.append("> ratio:");
            stringBuilder3.append(size.x / value.intValue());
            stringBuilder3.append(":");
            stringBuilder3.append(size.y / value.intValue());
            HwPCUtils.bdReport(context2, IDisplayEngineService.DE_ACTION_PG_2DGAME_FRONT, stringBuilder3.toString());
        }
    }

    public boolean isPackageRunningOnPCMode(String packageName, int uid) {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            boolean ret = this.mAMS.isPackageRunningOnPCMode(packageName, uid);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isPackageRunningOnPCMode ret = ");
            stringBuilder.append(ret);
            HwPCUtils.log(str, stringBuilder.toString());
            return ret;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("isPackageRunningOnPCMode checkCallingPermission failed ");
        stringBuilder2.append(Binder.getCallingPid());
        HwPCUtils.log(str2, stringBuilder2.toString());
        return false;
    }

    public boolean isScreenPowerOn() {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            boolean z;
            synchronized (this.mScreenAccessLock) {
                z = this.mScreenPowerOn;
            }
            return z;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("checkCallingPermission failed ");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
        return true;
    }

    public void setScreenPower(boolean powerOn) {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            setScreenPowerInner(powerOn, true);
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("checkCallingPermission failed ");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
    }

    private void lightPhoneScreen() {
        setScreenPowerInner(true, false);
    }

    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:17:0x0056, B:24:0x0064] */
    /* JADX WARNING: Missing block: B:21:?, code skipped:
            r4 = TAG;
            r5 = "setScreenPower IOException2";
     */
    /* JADX WARNING: Missing block: B:42:0x00a6, code skipped:
            if (r1 != null) goto L_0x00a8;
     */
    /* JADX WARNING: Missing block: B:44:?, code skipped:
            r1.close();
     */
    /* JADX WARNING: Missing block: B:47:?, code skipped:
            android.util.HwPCUtils.log(TAG, "setScreenPower IOException2");
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setScreenPowerInner(boolean powerOn, boolean checking) {
        String str;
        String str2;
        synchronized (this.mScreenAccessLock) {
            String str3 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setScreenPower old=");
            stringBuilder.append(this.mScreenPowerOn);
            stringBuilder.append(" new=");
            stringBuilder.append(powerOn);
            HwPCUtils.log(str3, stringBuilder.toString());
            if (powerOn == this.mScreenPowerOn && checking) {
            } else if (HwPCUtils.getIsWifiMode()) {
                int val = 0;
                IBinder displayToken = SurfaceControl.getBuiltInDisplay(0);
                if (powerOn) {
                    val = 2;
                }
                SurfaceControl.setDisplayPowerMode(displayToken, val);
                this.mScreenPowerOn = powerOn;
            } else {
                FileOutputStream fileDevice = null;
                String val2 = powerOn ? "1" : "0";
                try {
                    fileDevice = new FileOutputStream(new File(SCREEN_POWER_DEVICE));
                    fileDevice.write(val2.getBytes("utf-8"));
                    this.mScreenPowerOn = powerOn;
                    fileDevice.close();
                } catch (FileNotFoundException e) {
                    HwPCUtils.log(TAG, "setScreenPower FileNotFoundException");
                    HwPCDataReporter.getInstance().reportFailLightScreen(3, -1, "");
                    if (fileDevice != null) {
                        try {
                            fileDevice.close();
                        } catch (IOException e2) {
                            str = TAG;
                            str2 = "setScreenPower IOException2";
                            HwPCUtils.log(str, str2);
                        }
                    }
                } catch (IOException e3) {
                    HwPCUtils.log(TAG, "setScreenPower IOException1");
                    HwPCDataReporter.getInstance().reportFailLightScreen(3, -1, "");
                    if (fileDevice != null) {
                        try {
                            fileDevice.close();
                        } catch (IOException e4) {
                            str = TAG;
                            str2 = "setScreenPower IOException2";
                            HwPCUtils.log(str, str2);
                        }
                    }
                }
            }
        }
    }

    public void dispatchKeyEventForExclusiveKeyboard(KeyEvent ke) {
        String str;
        StringBuilder stringBuilder;
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("dispatchKeyEvent ");
            stringBuilder.append(ke);
            HwPCUtils.log(str, stringBuilder.toString());
            try {
                if (this.mMessenger != null) {
                    int keyCode = ke.getKeyCode();
                    if (ke.getAction() == 0) {
                        if (keyCode == 62) {
                            this.mMessenger.send(getMessage(12));
                        } else if (keyCode != CPUFeature.MSG_UNIPERF_BOOST_ON) {
                            switch (keyCode) {
                                case 3:
                                    this.mMessenger.send(getMessage(10));
                                    break;
                                case 4:
                                    this.mMessenger.send(getMessage(11));
                                    break;
                                default:
                                    break;
                            }
                        } else {
                            this.mMessenger.send(getMessage(13));
                        }
                    } else if (keyCode == 187) {
                        this.mMessenger.send(getMessage(9));
                    }
                }
            } catch (RemoteException e) {
                HwPCUtils.log("dispatchKeyEvent", "RemoteException");
            }
            return;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("checkCallingPermission failed ");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
    }

    private boolean isMonkeyRunning() {
        return ActivityManager.isUserAMonkey();
    }

    private void registerScreenOnEvent() {
        IntentFilter filter1 = new IntentFilter();
        filter1.addAction(ALARM_ALERT_CONFLICT);
        try {
            this.mContext.registerReceiverAsUser(this.mAlarmClockReceiver, UserHandle.ALL, filter1, BROADCAST_PERMISSION, null);
        } catch (IllegalArgumentException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("registerScreenOnEvent ");
            stringBuilder.append(e);
            HwPCUtils.log(str, stringBuilder.toString());
        }
    }

    private void unRegisterScreenOnEvent() {
        try {
            this.mContext.unregisterReceiver(this.mAlarmClockReceiver);
        } catch (IllegalArgumentException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unRegisterScreenOnEvent ");
            stringBuilder.append(e);
            HwPCUtils.log(str, stringBuilder.toString());
        }
    }

    private void enableFingerprintSlideSwitch() {
        try {
            ContentResolver resolver = this.mContext.getContentResolver();
            int userId = ActivityManager.getCurrentUser();
            if (System.getIntForUser(resolver, FINGERPRINT_SLIDE_SWITCH, 0, userId) == 0) {
                HwPCUtils.log(TAG, "enableFingerprintSlideSwitch");
                System.putIntForUser(resolver, FINGERPRINT_SLIDE_SWITCH, 1, userId);
            }
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enableFingerprintSlideSwitch ");
            stringBuilder.append(e);
            HwPCUtils.log(str, stringBuilder.toString());
        }
    }

    private void updateFingerprintSlideSwitch() {
        try {
            this.mAMS.updateFingerprintSlideSwitch();
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateFingerprintSlideSwitch ");
            stringBuilder.append(e);
            HwPCUtils.log(str, stringBuilder.toString());
        }
    }

    private void updateIMEWithHardKeyboardState(boolean switchToPcMode) {
        long ident = Binder.clearCallingIdentity();
        String str;
        StringBuilder stringBuilder;
        if (switchToPcMode) {
            try {
                this.mIMEWithHardKeyboardState = Secure.getInt(this.mContext.getContentResolver(), "show_ime_with_hard_keyboard", this.mIMEWithHardKeyboardState);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("switch to PcMode, IME With Hard Keyboard State:");
                stringBuilder.append(this.mIMEWithHardKeyboardState);
                HwPCUtils.log(str, stringBuilder.toString());
                if (HwPCUtils.enabledInPad()) {
                    Secure.putInt(this.mContext.getContentResolver(), "show_ime_with_hard_keyboard", 0);
                    return;
                }
                Secure.putInt(this.mContext.getContentResolver(), "show_ime_with_hard_keyboard", 1);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("switch To PhoneMode, update IME With Hard Keyboard State:");
            stringBuilder.append(this.mIMEWithHardKeyboardState);
            HwPCUtils.log(str, stringBuilder.toString());
            Secure.putInt(this.mContext.getContentResolver(), "show_ime_with_hard_keyboard", this.mIMEWithHardKeyboardState);
        }
    }

    private boolean isCalling() {
        this.mPhoneState = getPhoneState();
        return this.mPhoneState != 0;
    }

    private int getPhoneState() {
        StringBuilder stringBuilder;
        int simCount = this.mTelephonyPhone.getPhoneCount();
        int phoneState = 0;
        for (int i = 0; i < simCount; i++) {
            phoneState = this.mTelephonyPhone.getCallState(i);
            if (phoneState != 0) {
                String str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("simCount:");
                stringBuilder.append(simCount);
                stringBuilder.append(" phoneState:");
                stringBuilder.append(phoneState);
                HwPCUtils.log(str, stringBuilder.toString());
                return phoneState;
            }
        }
        String str2 = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("simCount:");
        stringBuilder.append(simCount);
        stringBuilder.append(" phoneState:");
        stringBuilder.append(phoneState);
        HwPCUtils.log(str2, stringBuilder.toString());
        return 0;
    }

    protected void showCallingToast(final int displayId) {
        if (HwPCUtils.enabledInPad()) {
            Context context;
            if (HwPCUtils.isValidExtDisplayId(displayId)) {
                context = HwPCUtils.getDisplayContext(this.mContext, displayId);
            } else {
                context = this.mContext;
            }
            if (context != null) {
                UiThread.getHandler().post(new Runnable() {
                    public void run() {
                        if (HwPCManagerService.this.mCallingToast != null) {
                            HwPCManagerService.this.mCallingToast.cancel();
                        }
                        if (HwPCUtils.isValidExtDisplayId(displayId)) {
                            HwPCManagerService.this.mCallingToast = Toast.makeText(context, context.getResources().getString(33685977), 1);
                        } else {
                            HwPCManagerService.this.mCallingToast = Toast.makeText(context, context.getResources().getString(33685947), 1);
                        }
                        if (HwPCManagerService.this.mCallingToast != null) {
                            HwPCManagerService.this.mCallingToast.show();
                        }
                    }
                });
            }
        }
    }

    public int forceDisplayMode(int mode) {
        String str;
        StringBuilder stringBuilder;
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("forceDisplayMode mode:");
            stringBuilder.append(mode);
            HwPCUtils.log(str, stringBuilder.toString());
            switch (mode) {
                case 1005:
                    return getOverScanMode();
                case 1006:
                    return setOverScanMode(0);
                case 1007:
                    return setOverScanMode(1);
                case 1008:
                    return setOverScanMode(2);
                default:
                    return 0;
            }
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("forceDisplayMode checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
        return 0;
    }

    public int getPCDisplayId() {
        if (!checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getPCDisplayId checkCallingPermission failed");
            stringBuilder.append(Binder.getCallingPid());
            HwPCUtils.log(str, stringBuilder.toString());
            return -1;
        } else if (HwPCUtils.isPcCastModeInServer()) {
            return get1stDisplay().mDisplayId;
        } else {
            HwPCUtils.log(TAG, "getPCDisplayId  is not in PC CastMode.");
            return -1;
        }
    }

    private int setOverScanMode(int mode) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setOverScanMode mode:");
        stringBuilder.append(mode);
        HwPCUtils.log(str, stringBuilder.toString());
        this.mAMS.setPCScreenDpMode(mode);
        return 0;
    }

    private int getOverScanMode() {
        HwPCUtils.log(TAG, "getOverScanMode");
        int mode = this.mAMS.getPCScreenDisplayMode();
        if (mode == 1) {
            return 1007;
        }
        if (mode == 2) {
            return 1008;
        }
        return 1006;
    }

    private void uploadPcDisplaySizePro() {
        int[] size = getPcDisplaySize();
        if (size.length >= 2) {
            SystemProperties.set("hw.pc.display.width", String.valueOf(size[0]));
            SystemProperties.set("hw.pc.display.height", String.valueOf(size[1]));
        }
    }

    private int[] getPcDisplaySize() {
        DisplayInfo displayInfo = getPcDisplayInfo();
        if (displayInfo == null || displayInfo.getDefaultMode() == null) {
            return new int[0];
        }
        return new int[]{displayInfo.getDefaultMode().getPhysicalWidth(), displayInfo.getDefaultMode().getPhysicalHeight()};
    }

    private DisplayInfo getPcDisplayInfo() {
        Display display = getPcDisplay(this.mContext);
        if (display == null) {
            return null;
        }
        DisplayInfo displayInfo = new DisplayInfo();
        display.getDisplayInfo(displayInfo);
        return displayInfo;
    }

    private Display getPcDisplay(Context context) {
        DisplayManager dm = (DisplayManager) context.getSystemService("display");
        if (dm != null) {
            Display[] displays = dm.getDisplays();
            if (displays != null && displays.length > 0) {
                int i = displays.length - 1;
                while (i >= 0) {
                    if (displays[i] != null && HwPCUtils.isValidExtDisplayId(displays[i].getDisplayId())) {
                        return displays[i];
                    }
                    i--;
                }
            }
        }
        HwPCUtils.log(TAG, "getPcDisplay not find PCDisplay");
        return null;
    }

    public void setFocusedPCDisplayId(String reason) {
        if (this.mWindowManagerInternal == null) {
            this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        }
        int extdisplayid = HwPCUtils.getPCDisplayID();
        if (this.mWindowManagerInternal != null && HwPCUtils.isPcCastModeInServer()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setFocusedDisplayId extdisplayid = ");
            stringBuilder.append(extdisplayid);
            HwPCUtils.log(str, stringBuilder.toString());
            this.mWindowManagerInternal.setFocusedDisplayId(extdisplayid, reason);
        }
    }

    public void showImeStatusIcon(int iconResId, String pkgName) {
        try {
            HwPCUtils.log(TAG, String.format("PCMS showImeStatusIcon:%s,%s", new Object[]{Integer.valueOf(iconResId), pkgName}));
            if (validateImeCall(pkgName) && this.mMessenger != null) {
                Message message = Message.obtain();
                message.what = 14;
                Bundle bundle = new Bundle();
                bundle.putString(AwareIntelligentRecg.CMP_PKGNAME, pkgName);
                message.obj = bundle;
                message.arg1 = iconResId;
                this.mMessenger.send(message);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "PCMS showImeStatusIcon-RemoteException");
        }
    }

    public void hideImeStatusIcon(String pkgName) {
        try {
            HwPCUtils.log(TAG, "PCMS hideImeStatusIcon");
            if (validateImeCall(pkgName) && this.mMessenger != null) {
                this.mMessenger.send(getMessage(15));
            }
        } catch (RemoteException e) {
            Log.e(TAG, "PCMS hideImeStatusIcon-RemoteException");
        }
    }

    private boolean validateImeCall(String pkgName) {
        String callingApp = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PCMS callingApp: ");
        stringBuilder.append(callingApp);
        stringBuilder.append(", pkg=");
        stringBuilder.append(pkgName);
        HwPCUtils.log(str, stringBuilder.toString());
        if (callingApp == null || !callingApp.equals(pkgName)) {
            return false;
        }
        return true;
    }

    private boolean isExclusiveKeyboardConnect(UEvent event) {
        if (event != null) {
            String kbState = event.get("KB_STATE");
            if (kbState != null && kbState.equals(AwareJobSchedulerConstants.SERVICES_STATUS_CONNECTED)) {
                return true;
            }
        }
        return false;
    }

    private void registerBluetoothReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        this.mContext.registerReceiver(this.mWifiPCReceiver, filter);
        this.mIsNeedUnRegisterBluetoothReciver = true;
        this.mBluetoothReminderDialog.dismissDialog();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            this.mBluetoothStateOnEnter = bluetoothAdapter.isEnabled();
            if (this.mBluetoothStateOnEnter) {
                this.mBluetoothReminderDialog.showCloseBluetoothTip(this.mContext);
            }
        }
    }

    private void unRegisterBluetoothReceiver() {
        this.mContext.unregisterReceiver(this.mWifiPCReceiver);
        this.mBluetoothReminderDialog.dismissDialog();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!(bluetoothAdapter == null || bluetoothAdapter.isEnabled() || !this.mBluetoothStateOnEnter)) {
            this.mBluetoothReminderDialog.showOpenBluetoothTip(this.mContext);
        }
        this.mBluetoothStateOnEnter = false;
        this.mIsNeedUnRegisterBluetoothReciver = false;
    }

    private boolean isWifiPCMode(int displayid) {
        boolean z = false;
        if (!(displayid == -1 || this.mDisplayManager == null)) {
            Display display = this.mDisplayManager.getDisplay(displayid);
            if (display != null) {
                if (display.getType() == 3) {
                    z = true;
                }
                return z;
            }
        }
        return false;
    }

    private int isConnectFromThirdApp(int displayId) {
        if (this.mDisplayManager == null) {
            this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
        }
        Display display = this.mDisplayManager.getDisplay(displayId);
        if (display != null && display.getType() == 5) {
            if ("com.hpplay.happycast".equals(display.getOwnerPackageName())) {
                HwPCUtils.bdReport(this.mContext, 10061, "");
                return 1;
            } else if ("com.huawei.works".equals(display.getOwnerPackageName())) {
                return 2;
            }
        }
        return -1;
    }

    private void launchWeLink() {
        try {
            String encodedUri = URLEncoder.encode("ui://welink.wirelessdisplay/home", "utf-8");
            Intent intent = new Intent("android.intent.action.VIEW");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("welink://works.huawei.com?uri=");
            stringBuilder.append(encodedUri);
            intent.setData(Uri.parse(stringBuilder.toString()));
            intent.setFlags(335544320);
            intent.putExtra("src", 203);
            intent.putExtra("target", 103);
            this.mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            HwPCUtils.log(TAG, "launchWeLink ActivityNotFoundException error");
        } catch (UnsupportedEncodingException e2) {
            HwPCUtils.log(TAG, "launchWeLink UnsupportedEncodingException error");
        }
    }

    public void setPointerIconType(int iconId, boolean keep) {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            HwInputManagerLocalService inputManager = (HwInputManagerLocalService) LocalServices.getService(HwInputManagerLocalService.class);
            if (inputManager != null) {
                inputManager.setPointerIconTypeAndKeep(iconId, keep);
            }
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setCustomPointerIcon checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
    }

    public void setCustomPointerIcon(PointerIcon icon, boolean keep) {
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            HwInputManagerLocalService inputManager = (HwInputManagerLocalService) LocalServices.getService(HwInputManagerLocalService.class);
            if (inputManager != null) {
                inputManager.setCustomPointerIconAndKeep(icon, keep);
            }
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setCustomPointerIcon checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
    }

    public void notifyDpState(boolean dpState) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyDpState dpState = ");
        stringBuilder.append(dpState);
        HwPCUtils.log(str, stringBuilder.toString());
        if (checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            this.mHandler.removeMessages(21);
            this.mHandler.sendMessage(this.mHandler.obtainMessage(21, Boolean.valueOf(dpState)));
            return;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("notifyDpState checkCallingPermission failed");
        stringBuilder.append(Binder.getCallingPid());
        HwPCUtils.log(str, stringBuilder.toString());
    }

    private CastingDisplay get1stDisplay() {
        return this.mPcMultiDisplayMgr.get1stDisplay();
    }

    private CastingDisplay get2ndDisplay() {
        return this.mPcMultiDisplayMgr.get2ndDisplay();
    }

    private void showDPLinkErrorDialog(Context context, String tip) {
        dismissDpLinkErrorDialog();
        Builder builder = new Builder(context, 33947691);
        this.mShowDpLinkErrorTipDialog = builder.setTitle(33685941).setPositiveButton(33685817, new OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                arg0.dismiss();
            }
        }).setMessage(String.format(tip, new Object[]{""})).create();
        this.mShowDpLinkErrorTipDialog.getWindow().setType(HwArbitrationDEFS.MSG_MPLINK_BIND_FAIL);
        this.mShowDpLinkErrorTipDialog.show();
        this.mShowDpLinkErrorTipDialog.getWindow().getAttributes().setTitle("ShowDpLinkErrorTipDialog");
    }

    private void dismissDpLinkErrorDialog() {
        if (this.mShowDpLinkErrorTipDialog != null && this.mShowDpLinkErrorTipDialog.isShowing()) {
            this.mShowDpLinkErrorTipDialog.dismiss();
            this.mShowDpLinkErrorTipDialog = null;
        }
    }

    public void execVoiceCmd(Message message) {
        if (!checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("execVoiceCmd checkCallingPermission failed ");
            stringBuilder.append(Binder.getCallingPid());
            HwPCUtils.log(str, stringBuilder.toString());
        } else if (message == null) {
            HwPCUtils.log(TAG, "execVoiceCmd message = null");
        } else {
            this.mVAssistCmdExecutor.execVoiceCmd(message);
        }
    }

    private boolean shouldDropEventsForSendBroadcast() {
        long currTime = SystemClock.uptimeMillis();
        if (currTime - this.mPrevTimeForBroadcast <= 500) {
            return true;
        }
        this.mPrevTimeForBroadcast = currTime;
        return false;
    }

    private void sendBroadcastForClearLighterDrawed() {
        HwPCUtils.log(TAG, "sendBroadcastForClearLighterDrawed");
        if (shouldDropEventsForSendBroadcast()) {
            HwPCUtils.log(TAG, "ignore because of frequent events");
            return;
        }
        Intent intent = new Intent();
        intent.setAction(ACTION_CLEAR_LIGHTER_DRAWED);
        intent.setPackage("com.huawei.desktop.systemui");
        this.mContext.sendBroadcast(intent, PERMISSION_BROADCAST_CLEAR_LIGHTER_DRAWED);
    }

    private static boolean isDapKey(int keyCode) {
        return keyCode == 20 || keyCode == 269 || keyCode == 271 || keyCode == 21 || keyCode == 22 || keyCode == 19 || keyCode == 268 || keyCode == 270 || keyCode == 23;
    }

    private static boolean isVolumeKey(KeyEvent ev) {
        int keyCode = ev.getKeyCode();
        return keyCode == 25 || keyCode == 24;
    }

    private static boolean keyForPPTSwitch(KeyEvent ev) {
        if (isDapKey(ev.getKeyCode())) {
            return true;
        }
        if (!isVolumeKey(ev) || ev.getAction() == 1) {
            return false;
        }
        return true;
    }

    private void filterScrollForPCMode() {
        if (HwPCUtils.isPcCastModeInServer() && HwWindowManager.hasLighterViewInPCCastMode()) {
            shouldInterceptInputEvent(null, true);
        }
    }

    private boolean isFullScreenApp(Rect bounds) {
        if (bounds == null) {
            return true;
        }
        if (this.mPCDisplayInfo != null && bounds.left == 0 && bounds.top == 0 && bounds.right == this.mPCDisplayInfo.logicalWidth && bounds.bottom == this.mPCDisplayInfo.logicalHeight) {
            return true;
        }
        return false;
    }

    private boolean shouldSendBroadcastForClearLighterDrawed(KeyEvent ev, boolean forScroll) {
        boolean sendBroadcastForClearDrawed = false;
        WindowStateData wsd = new WindowStateData();
        if (HwPCVAssistCmdExecutor.specialAppFocused(get1stDisplay().mDisplayId, wsd, true, true) && isFullScreenApp(wsd.bounds)) {
            if (ev != null) {
                if (keyForPPTSwitch(ev)) {
                    sendBroadcastForClearDrawed = true;
                }
            } else if (forScroll) {
                sendBroadcastForClearDrawed = true;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("shouldInterceptInputEvent sendBroadcastForClearDrawed = ");
            stringBuilder.append(sendBroadcastForClearDrawed);
            HwPCUtils.log(str, stringBuilder.toString());
            return sendBroadcastForClearDrawed;
        }
        HwPCUtils.log(TAG, "shouldInterceptInputEvent ignore it when special app not focused or not full screen");
        return false;
    }

    public boolean shouldInterceptInputEvent(KeyEvent ev, boolean forScroll) {
        if (!checkCallingPermission(PERMISSION_PC_MANAGER_API)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("shouldInterceptInputEvent checkCallingPermission failed ");
            stringBuilder.append(Binder.getCallingPid());
            HwPCUtils.log(str, stringBuilder.toString());
            return true;
        } else if (ev == null && !forScroll) {
            HwPCUtils.log(TAG, "shouldInterceptInputEvent ev = null");
            return true;
        } else if (ev == null || isDapKey(ev.getKeyCode()) || KeyEvent.isSystemKey(ev.getKeyCode())) {
            this.mHandler.removeMessages(24);
            Message msg = this.mHandler.obtainMessage(24);
            msg.obj = ev;
            msg.arg1 = forScroll;
            this.mHandler.sendMessage(msg);
            return false;
        } else {
            HwPCUtils.log(TAG, "shouldInterceptInputEvent only dap or volume for PPT accept when lighter view appear");
            return true;
        }
    }

    private void setPcCastingDisplayId(int displayId) {
        SystemProperties.set("hw.pc.casting.displayid", String.valueOf(displayId));
    }
}
