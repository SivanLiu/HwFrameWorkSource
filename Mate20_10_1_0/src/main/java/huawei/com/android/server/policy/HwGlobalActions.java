package huawei.com.android.server.policy;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.hdm.HwDeviceManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.BatteryManagerInternal;
import android.os.Bundle;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Vibrator;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.Settings;
import android.service.dreams.IDreamManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.HwPCUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.widget.Toast;
import com.android.internal.os.HwBootAnimationOeminfo;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.HwAutoUpdate;
import com.android.server.HwServiceFactory;
import com.android.server.LocalServices;
import com.android.server.hidata.appqoe.HwAPPQoEUserAction;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.policy.GlobalActionsProvider;
import com.android.server.policy.HwPhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.power.IHwShutdownThread;
import com.android.server.rms.iaware.feature.SceneRecogFeature;
import com.huawei.android.app.HwAlarmManager;
import com.huawei.android.statistical.StatisticalUtils;
import com.huawei.android.view.WindowManagerEx;
import huawei.android.hwpicaveragenoises.HwPicAverageNoises;
import huawei.com.android.server.fingerprint.FingerViewController;
import huawei.com.android.server.policy.HwGlobalActionsView;
import huawei.com.android.server.policy.recsys.HwLog;
import java.util.Locale;

public class HwGlobalActions implements DialogInterface.OnDismissListener, DialogInterface.OnClickListener, HwGlobalActionsView.ActionPressedCallback {
    private static final String ACTION_NOTIFY_HIVOICE_HIDE_NEW = "com.huawei.hiassistantoversea.action.DEVICE_SHUTDOWN_SIGNAL";
    private static final String ACTION_NOTIFY_HIVOICE_HIDE_OLD = "com.huawei.vassistant.action.DEVICE_SHUTDOWN_SIGNAL";
    private static final String ACTION_NOTIFY_STATUSBAR_HIDE = "com.android.server.pc.action.DOCKBAR_HIDE";
    private static final String ACTION_NOTIFY_STATUSBAR_SHOW = "com.android.server.pc.action.DOCKBAR_SHOW";
    private static final int AUTOROTATION_OPEN = 1;
    private static final boolean DEBUG = false;
    private static final int DEFAULT_BITMAP_FILL_COLOR = -872415232;
    private static final int DEFAULT_BITMAP_WIDTH_AND_HEIGHT = 100;
    private static final int DISMISSS_DELAY_0 = 0;
    private static final int DISMISSS_DELAY_200 = 200;
    private static final boolean FASTSHUTDOWN_CONSIDER_SDCARD = true;
    private static final boolean FASTSHUTDOWN_PROP = SystemProperties.getBoolean("ro.config.hw_emui_fastshutdown_mode", true);
    private static final String HIVOICE_PACAKGE_NEW = "com.huawei.hiassistantoversea";
    private static final String HIVOICE_PACAKGE_OLD = "com.huawei.vassistant";
    private static final int INCOMINGCALL_DISMISS_VIEW_DELAY = 200;
    private static final int MASK_PLANE_COLOR = -1728053248;
    private static final int MASK_PLANE_COLOR_TELEVISION = -872415232;
    private static final int MAXLAYER = 159999;
    private static final int MAX_BLUR_RADIUS = 25;
    private static final int MESSAGE_DISMISS = 0;
    private static final int MESSAGE_SHOW = 1;
    private static final int MESSAGE_UPDATE_AIRPLANE_MODE = 2;
    private static final int MESSAGE_UPDATE_LOCKDOWN_MODE = 6;
    private static final int MESSAGE_UPDATE_REBOOT_MODE = 4;
    private static final int MESSAGE_UPDATE_SHUTDOWN_MODE = 5;
    private static final int MESSAGE_UPDATE_SILENT_MODE = 3;
    private static final int MINLAYER = 0;
    private static final int MSG_SET_BLUR_BITMAP = 7;
    private static final int NO_REBOOT_CHARGE_FLAG = 1;
    private static final String PERMISSION_BROADCAST_GLOBALACTIONS_VIEW_STATE_CHANGED = "com.huawei.permission.GLOBALACTIONS_VIEW_STATE_CHANGED";
    private static final String PERMISSION_BROADCAST_NOTIFY_HIVOICE_HIDE_NEW = "com.huawei.hiassistantoversea.permission.SHUTDOWN_SIGNAL_SEND";
    private static final String PERMISSION_BROADCAST_NOTIFY_HIVOICE_HIDE_OLD = "com.huawei.vassistant.permission.SHUTDOWN_SIGNAL_SEND";
    private static final String REBOOT_TAG = "reboot";
    private static final int ROTATION_DEFAULT = 0;
    private static final int ROTATION_NINETY = 90;
    private static final float SCALE = 0.125f;
    private static final String SCREENOFF_TAG = "screenoff";
    private static final String SHUTDOWN_TAG = "shutdown";
    private static final String TAG = "HwGlobalActions";
    private static IHwShutdownThread iHwShutdownThread = HwServiceFactory.getHwShutdownThread();
    private static boolean mCharging = false;
    private final int ROTATION = SystemProperties.getInt("ro.panel.hw_orientation", 0);
    private String mActionNotifyHivoiceHide = ACTION_NOTIFY_HIVOICE_HIDE_OLD;
    /* access modifiers changed from: private */
    public ToggleAction mAirplaneModeAction;
    private ContentObserver mAirplaneModeObserver = new ContentObserver(new Handler()) {
        /* class huawei.com.android.server.policy.HwGlobalActions.AnonymousClass10 */

        public void onChange(boolean selfChange) {
            HwGlobalActions.this.mHandler.sendEmptyMessage(2);
        }
    };
    /* access modifiers changed from: private */
    public AudioManager mAudioManager;
    private BitmapDrawable mBlurDrawable;
    /* access modifiers changed from: private */
    public int mBlurRadius;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        /* class huawei.com.android.server.policy.HwGlobalActions.AnonymousClass7 */

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action) || SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_OFF.equals(action)) {
                    if (!"globalactions".equals(intent.getStringExtra("reason"))) {
                        HwGlobalActions.this.mHandler.sendEmptyMessage(0);
                    }
                } else if ("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED".equals(action)) {
                    if (!intent.getBooleanExtra("PHONE_IN_ECM_STATE", false) && HwGlobalActions.this.mIsWaitingForEcmExit) {
                        boolean unused = HwGlobalActions.this.mIsWaitingForEcmExit = false;
                        HwGlobalActions.this.changeAirplaneModeSystemSetting(true);
                    }
                } else if ("android.intent.action.PHONE_STATE".equals(action)) {
                    if (TelephonyManager.EXTRA_STATE_RINGING.equals(intent.getStringExtra(SceneRecogFeature.DATA_STATE))) {
                        HwGlobalActions.this.mHandler.sendEmptyMessageDelayed(0, 200);
                    }
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public final Context mContext;
    private final IDreamManager mDreamManager;
    private final GlobalActionsProvider mGlobalActionsProvider;
    private HwGlobalActionsView mGlobalActionsView;
    /* access modifiers changed from: private */
    public HwGlobalActionsData mGlobalactionsData = null;
    /* access modifiers changed from: private */
    public Handler mHandler = new Handler() {
        /* class huawei.com.android.server.policy.HwGlobalActions.AnonymousClass2 */

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    HwGlobalActions.this.doHandlerMessageDismiss(msg);
                    return;
                case 1:
                    HwGlobalActions.this.handleShow();
                    return;
                case 2:
                    if (HwGlobalActions.this.mAirplaneModeAction != null) {
                        HwGlobalActions.this.mAirplaneModeAction.updateState();
                        return;
                    }
                    return;
                case 3:
                    if (HwGlobalActions.this.mSilentModeAction != null) {
                        HwGlobalActions.this.mSilentModeAction.updateState();
                        return;
                    }
                    return;
                case 4:
                    HwGlobalActions.this.doHandlerMessageUpdateRebootMode();
                    return;
                case 5:
                    HwGlobalActions.this.doHandlerMessageUpdateShutdownMode();
                    return;
                case 6:
                    HwGlobalActions.this.doHandlerMessageUpdateLockdownMode();
                    return;
                case 7:
                    HwGlobalActions.this.doHandlerMessageBlurBitmap(msg);
                    return;
                default:
                    return;
            }
        }
    };
    /* access modifiers changed from: private */
    public boolean mHasTelephony;
    private boolean mHasVibrator;
    private String mHivoicePackage = HIVOICE_PACAKGE_OLD;
    private boolean mHwFastShutdownEnable = false;
    private boolean mHwGlobalActionsShowing;
    private HwPhoneWindowManager mHwPhoneWindowManager;
    private boolean mIsDeviceProvisioned;
    private boolean mIsTelevisionMode;
    /* access modifiers changed from: private */
    public boolean mIsWaitingForEcmExit = false;
    private boolean mKeyguardSecure;
    private boolean mKeyguardShowing;
    /* access modifiers changed from: private */
    public final Object mLock = new Object[0];
    private String mPermissionBroadcastNotifyHivoiceHide = PERMISSION_BROADCAST_NOTIFY_HIVOICE_HIDE_OLD;
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        /* class huawei.com.android.server.policy.HwGlobalActions.AnonymousClass1 */

        public void onServiceStateChanged(ServiceState serviceState) {
            if (HwGlobalActions.this.mHasTelephony) {
                HwGlobalActions.this.mHandler.sendEmptyMessage(2);
            }
        }
    };
    /* access modifiers changed from: private */
    public int mRadius;
    private BroadcastReceiver mRingerModeReceiver = new BroadcastReceiver() {
        /* class huawei.com.android.server.policy.HwGlobalActions.AnonymousClass6 */

        public void onReceive(Context context, Intent intent) {
            if ("android.media.RINGER_MODE_CHANGED".equals(intent.getAction()) && HwGlobalActions.this.mSilentModeAction != null) {
                HwGlobalActions.this.mHandler.sendEmptyMessage(3);
            }
        }
    };
    /* access modifiers changed from: private */
    public int mScale;
    /* access modifiers changed from: private */
    public ToggleAction mSilentModeAction;
    private int mSystemRotation = -1;
    private BitmapThread mThread;
    private final WindowManagerPolicy.WindowManagerFuncs mWindowManagerFuncs;

    public interface Action {
        boolean onLongPress();

        void onPress();
    }

    public HwGlobalActions(Context context, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs) {
        boolean z = false;
        this.mContext = context;
        this.mWindowManagerFuncs = windowManagerFuncs;
        this.mScale = 1;
        this.mRadius = 1;
        this.mIsTelevisionMode = false;
        this.mBlurRadius = this.mContext.getResources().getInteger(34275399);
        if (this.mBlurRadius == 0) {
            this.mBlurRadius = 25;
        }
        initPkgName();
        this.mHwPhoneWindowManager = (HwPhoneWindowManager) LocalServices.getService(WindowManagerPolicy.class);
        this.mGlobalActionsProvider = (GlobalActionsProvider) LocalServices.getService(GlobalActionsProvider.class);
        this.mDreamManager = IDreamManager.Stub.asInterface(ServiceManager.getService("dreams"));
        IntentFilter filter = new IntentFilter();
        filter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_OFF);
        filter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        filter.addAction("android.intent.action.PHONE_STATE");
        context.registerReceiver(this.mBroadcastReceiver, filter);
        ((TelephonyManager) context.getSystemService("phone")).listen(this.mPhoneStateListener, 1);
        this.mHasTelephony = ((ConnectivityManager) context.getSystemService("connectivity")).isNetworkSupported(0);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("airplane_mode_on"), true, this.mAirplaneModeObserver);
        Vibrator vibrator = (Vibrator) this.mContext.getSystemService("vibrator");
        if (vibrator != null && vibrator.hasVibrator()) {
            z = true;
        }
        this.mHasVibrator = z;
        this.mContext.registerReceiver(this.mRingerModeReceiver, new IntentFilter("android.media.RINGER_MODE_CHANGED"));
        IntentFilter filter3 = new IntentFilter();
        filter3.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter3, "android.permission.INJECT_EVENTS", null);
    }

    public void showDialog(boolean keyguardShowing, boolean keyguardSecure, boolean isDeviceProvisioned) {
        GlobalActionsProvider globalActionsProvider = this.mGlobalActionsProvider;
        if (globalActionsProvider != null && globalActionsProvider.isGlobalActionsDisabled()) {
            Log.i(TAG, "showDialog(): global actions disabled(device in Lock Task Mode)");
        } else if (!this.mHwGlobalActionsShowing) {
            if (!isDeviceProvisioned && ((UserManager) this.mContext.getSystemService("user")).getUserCount() > 1 && isOwnerUser()) {
                isDeviceProvisioned = true;
            }
            this.mKeyguardShowing = keyguardShowing;
            this.mKeyguardSecure = keyguardSecure;
            this.mIsDeviceProvisioned = isDeviceProvisioned;
            if ("television".equals(this.mContext.getResources().getString(33685671))) {
                this.mIsTelevisionMode = true;
            }
            Log.w(TAG, "showDialog(): mIsTelevisionMode = " + this.mIsTelevisionMode);
            this.mHandler.sendEmptyMessage(1);
            sendGlobalActionsIntent(true);
        }
    }

    private void sendGlobalActionsIntent(boolean isShown) {
        if (this.mContext != null) {
            Intent globalActionsIntent = new Intent("com.android.systemui.action.GLOBAL_ACTION");
            globalActionsIntent.putExtra("isShown", isShown);
            globalActionsIntent.setPackage(FingerViewController.PKGNAME_OF_KEYGUARD);
            this.mContext.sendBroadcastAsUser(globalActionsIntent, UserHandle.CURRENT, "com.android.keyguard.FINGERPRINT_UNLOCK");
        }
    }

    private boolean isOwnerUser() {
        return ActivityManager.getCurrentUser() == 0;
    }

    private void awakenIfNecessary() {
        IDreamManager iDreamManager = this.mDreamManager;
        if (iDreamManager != null) {
            try {
                if (iDreamManager.isDreaming()) {
                    this.mDreamManager.awaken();
                }
            } catch (RemoteException e) {
            }
        }
    }

    private void doHandleShowWork() {
        initGlobalActionsData(this.mKeyguardShowing, this.mKeyguardSecure, this.mIsDeviceProvisioned);
        initGlobalActions();
        createGlobalActionsView();
        HwGlobalActionsView hwGlobalActionsView = this.mGlobalActionsView;
        if (hwGlobalActionsView != null) {
            hwGlobalActionsView.setBackgroundDrawable(this.mBlurDrawable);
        }
        showGlobalActionsView();
        StatisticalUtils.reportc(this.mContext, 21);
    }

    /* access modifiers changed from: private */
    public void handleShow() {
        awakenIfNecessary();
        startBlurScreenshotThread();
    }

    @Override // huawei.com.android.server.policy.HwGlobalActionsView.ActionPressedCallback
    public void onOtherAreaPressed() {
        String stateName = "dismiss";
        if ((this.mGlobalactionsData.getState() & 512) != 0) {
            this.mHandler.sendEmptyMessage(4);
            stateName = "cancel_reboot";
        } else if ((this.mGlobalactionsData.getState() & 8192) != 0) {
            this.mHandler.sendEmptyMessage(5);
            stateName = "cancel_shutdown";
        } else if ((this.mGlobalactionsData.getState() & 131072) != 0) {
            this.mHandler.sendEmptyMessage(6);
            stateName = "cancel_lockdown";
        } else {
            dismissShutdownMenu(0);
        }
        StatisticalUtils.reporte(this.mContext, 22, String.format("{action:touch_black, state:%s}", stateName));
    }

    private void dismissShutdownMenuAfterLockdown() {
        if (this.ROTATION == ROTATION_NINETY && this.mSystemRotation >= 0) {
            Settings.System.putInt(this.mContext.getContentResolver(), "accelerometer_rotation", this.mSystemRotation);
            this.mSystemRotation = -1;
        }
        if (ShutdownMenuAnimations.isSuperLiteMode()) {
            Bundle bundle = new Bundle();
            bundle.putBoolean("is_lock_down", true);
            Message msg = this.mHandler.obtainMessage(0);
            msg.setData(bundle);
            this.mHandler.sendMessage(msg);
            return;
        }
        AnimatorSet dismissAnimSet = ShutdownMenuAnimations.getInstance(this.mContext).setShutdownMenuDismissAnim();
        dismissAnimSet.addListener(new Animator.AnimatorListener() {
            /* class huawei.com.android.server.policy.HwGlobalActions.AnonymousClass3 */

            public void onAnimationStart(Animator arg0) {
                ShutdownMenuAnimations.getInstance(HwGlobalActions.this.mContext).setIsAnimRunning(true);
                Bundle bundle = new Bundle();
                bundle.putBoolean("is_lock_down", true);
                Message msg = HwGlobalActions.this.mHandler.obtainMessage(0);
                msg.setData(bundle);
                HwGlobalActions.this.mHandler.sendMessage(msg);
            }

            public void onAnimationRepeat(Animator arg0) {
            }

            public void onAnimationEnd(Animator arg0) {
                ShutdownMenuAnimations.getInstance(HwGlobalActions.this.mContext).setIsAnimRunning(false);
            }

            public void onAnimationCancel(Animator arg0) {
                ShutdownMenuAnimations.getInstance(HwGlobalActions.this.mContext).setIsAnimRunning(false);
            }
        });
        dismissAnimSet.start();
    }

    @Override // huawei.com.android.server.policy.HwGlobalActionsView.ActionPressedCallback
    public void dismissShutdownMenu(int delayTime) {
        if (this.ROTATION == ROTATION_NINETY && this.mSystemRotation >= 0) {
            Settings.System.putInt(this.mContext.getContentResolver(), "accelerometer_rotation", this.mSystemRotation);
            this.mSystemRotation = -1;
        }
        if (ShutdownMenuAnimations.isSuperLiteMode()) {
            HwLog.d(TAG, "dismissShutdownMenu super lite mode don't need animations.");
            this.mHandler.sendEmptyMessage(0);
            return;
        }
        AnimatorSet mExitSet = ShutdownMenuAnimations.getInstance(this.mContext).setNewShutdownViewAnimation(false);
        mExitSet.addListener(new Animator.AnimatorListener() {
            /* class huawei.com.android.server.policy.HwGlobalActions.AnonymousClass4 */

            public void onAnimationStart(Animator arg0) {
                ShutdownMenuAnimations.getInstance(HwGlobalActions.this.mContext).setIsAnimRunning(true);
            }

            public void onAnimationRepeat(Animator arg0) {
            }

            public void onAnimationEnd(Animator arg0) {
                ShutdownMenuAnimations.getInstance(HwGlobalActions.this.mContext).setIsAnimRunning(false);
                HwGlobalActions.this.mHandler.sendEmptyMessage(0);
            }

            public void onAnimationCancel(Animator arg0) {
                ShutdownMenuAnimations.getInstance(HwGlobalActions.this.mContext).setIsAnimRunning(false);
            }
        });
        if (delayTime > 0) {
            mExitSet.setStartDelay((long) delayTime);
        }
        mExitSet.start();
    }

    @Override // huawei.com.android.server.policy.HwGlobalActionsView.ActionPressedCallback
    public void onAirplaneModeActionPressed() {
        ToggleAction toggleAction = this.mAirplaneModeAction;
        if (toggleAction != null) {
            toggleAction.onPress();
        }
    }

    @Override // huawei.com.android.server.policy.HwGlobalActionsView.ActionPressedCallback
    public void onSilentModeActionPressed() {
        ToggleAction toggleAction = this.mSilentModeAction;
        if (toggleAction != null) {
            toggleAction.onPress();
        }
    }

    @Override // huawei.com.android.server.policy.HwGlobalActionsView.ActionPressedCallback
    public void onRebootActionPressed() {
        StatisticalUtils.reporte(this.mContext, 22, "{action:reboot, state:pressed}");
        HwGlobalActionsData hwGlobalActionsData = this.mGlobalactionsData;
        if (hwGlobalActionsData == null) {
            return;
        }
        if ((hwGlobalActionsData.getState() & 512) == 0 && !this.mIsTelevisionMode) {
            this.mHandler.sendEmptyMessage(4);
        } else if (isOtherAreaPressedInTalkback()) {
            onOtherAreaPressed();
        } else {
            if (this.ROTATION == ROTATION_NINETY && this.mSystemRotation >= 0) {
                Settings.System.putInt(this.mContext.getContentResolver(), "accelerometer_rotation", this.mSystemRotation);
                this.mSystemRotation = -1;
            }
            if (!this.mIsTelevisionMode) {
                ShutdownMenuAnimations.getInstance(this.mContext).startNewShutdownOrRebootAnim(true, false);
            } else {
                dismissShutdownMenuWithAction(REBOOT_TAG, 34603151);
            }
            try {
                IPowerManager pm = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
                if (pm != null) {
                    if (HwAutoUpdate.getInstance().isAutoSystemUpdate(this.mContext, true)) {
                        pm.reboot(false, "recovery", false);
                    } else {
                        pm.reboot(false, "huawei_reboot", false);
                    }
                    StatisticalUtils.reporte(this.mContext, 22, "{action:reboot, state:confrim}");
                    ShutdownMenuAnimations.getInstance(this.mContext).setIsAnimRunning(true);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "PowerManager service died!", e);
            }
        }
    }

    @Override // huawei.com.android.server.policy.HwGlobalActionsView.ActionPressedCallback
    public void onLockdownActionPressed() {
        HwGlobalActionsData hwGlobalActionsData = this.mGlobalactionsData;
        if (hwGlobalActionsData == null) {
            return;
        }
        if ((hwGlobalActionsData.getState() & 131072) == 0) {
            this.mHandler.sendEmptyMessage(6);
        } else if (isOtherAreaPressedInTalkback()) {
            onOtherAreaPressed();
        } else {
            if (this.ROTATION == ROTATION_NINETY && this.mSystemRotation >= 0) {
                Settings.System.putInt(this.mContext.getContentResolver(), "accelerometer_rotation", this.mSystemRotation);
                this.mSystemRotation = -1;
            }
            dismissShutdownMenuAfterLockdown();
        }
    }

    @Override // huawei.com.android.server.policy.HwGlobalActionsView.ActionPressedCallback
    public void onScreenoffActionPressed() {
        if (isOtherAreaPressedInTalkback()) {
            onOtherAreaPressed();
            return;
        }
        if (this.ROTATION == ROTATION_NINETY && this.mSystemRotation >= 0) {
            Settings.System.putInt(this.mContext.getContentResolver(), "accelerometer_rotation", this.mSystemRotation);
            this.mSystemRotation = -1;
        }
        dismissShutdownMenuWithAction(SCREENOFF_TAG, 34603442);
    }

    public static void SetShutdownFlag(int flag) {
        try {
            Log.d(TAG, "shutdownThread: writeBootAnimShutFlag =" + flag);
            if (HwBootAnimationOeminfo.setBootChargeShutFlag(flag) != 0) {
                Log.e(TAG, "shutdownThread: writeBootAnimShutFlag error");
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "SetShutdownFlag RuntimeException");
        } catch (Exception e2) {
            Log.e(TAG, "SetShutdownFlag error");
        }
    }

    @Override // huawei.com.android.server.policy.HwGlobalActionsView.ActionPressedCallback
    public void onShutdownActionPressed(boolean isDeskClockClose, boolean isBootOnTimeClose, int listviewState) {
        if (HwDeviceManager.disallowOp(49)) {
            Toast toast = Toast.makeText(this.mContext, 33685904, 0);
            toast.getWindowParams().type = 2101;
            toast.getWindowParams().privateFlags |= 16;
            toast.show();
            return;
        }
        StatisticalUtils.reporte(this.mContext, 22, "{action:shutdown, state:pressed}");
        HwGlobalActionsData hwGlobalActionsData = this.mGlobalactionsData;
        if (hwGlobalActionsData == null) {
            return;
        }
        if ((hwGlobalActionsData.getState() & 8192) == 0 && !this.mIsTelevisionMode) {
            this.mHandler.sendEmptyMessage(5);
        } else if (isOtherAreaPressedInTalkback()) {
            onOtherAreaPressed();
        } else {
            if (this.ROTATION == ROTATION_NINETY && this.mSystemRotation >= 0) {
                Settings.System.putInt(this.mContext.getContentResolver(), "accelerometer_rotation", this.mSystemRotation);
                this.mSystemRotation = -1;
            }
            if ((HwGlobalActionsData.getSingletoneInstance().getState() & 8192) != 0) {
                HwAlarmManager.adjustHwRTCAlarm(isDeskClockClose, isBootOnTimeClose, listviewState);
            }
            BatteryManagerInternal batteryManagerInternal = (BatteryManagerInternal) LocalServices.getService(BatteryManagerInternal.class);
            if (batteryManagerInternal != null) {
                mCharging = batteryManagerInternal.isPowered(7);
                Log.d(TAG, "the current mCharging = " + mCharging);
                if (mCharging) {
                    SetShutdownFlag(1);
                }
            }
            if (!this.mIsTelevisionMode) {
                checkFastShutdownCondition();
                ShutdownMenuAnimations.getInstance(this.mContext).startNewShutdownOrRebootAnim(false, this.mHwFastShutdownEnable);
            }
            tryToDoShutdown();
        }
    }

    private void dismissShutdownMenuWithAction(final String layoutTags, int layoutId) {
        if (ShutdownMenuAnimations.getInstance(this.mContext).getIsAnimRunning()) {
            Log.w(TAG, "dismissShutdownMenuWithAction: animation is running and conflict");
            return;
        }
        AnimatorSet dismissAnimSet = ShutdownMenuAnimations.getInstance(this.mContext).getFocusConfirmEnterAnim(this.mContext, this.mIsTelevisionMode, layoutId);
        if (dismissAnimSet != null) {
            dismissAnimSet.addListener(new Animator.AnimatorListener() {
                /* class huawei.com.android.server.policy.HwGlobalActions.AnonymousClass5 */

                public void onAnimationStart(Animator animation) {
                    ShutdownMenuAnimations.getInstance(HwGlobalActions.this.mContext).setIsAnimRunning(true);
                }

                public void onAnimationRepeat(Animator animation) {
                }

                public void onAnimationEnd(Animator animation) {
                    HwGlobalActions.this.executeRelatedAction(layoutTags);
                    ShutdownMenuAnimations.getInstance(HwGlobalActions.this.mContext).setIsAnimRunning(false);
                }

                public void onAnimationCancel(Animator animation) {
                    ShutdownMenuAnimations.getInstance(HwGlobalActions.this.mContext).setIsAnimRunning(false);
                }
            });
            dismissAnimSet.start();
        }
    }

    /* access modifiers changed from: private */
    public void executeRelatedAction(String layoutTags) {
        if (REBOOT_TAG.equals(layoutTags)) {
            Log.w(TAG, "executeRelatedAction reboot tags");
        } else if (SHUTDOWN_TAG.equals(layoutTags)) {
            Log.w(TAG, "executeRelatedAction shutdown tags");
            callPowerManagerMethod(65536);
        } else if (SCREENOFF_TAG.equals(layoutTags)) {
            Log.w(TAG, "executeRelatedAction screenoff tags");
            Bundle bundle = new Bundle();
            bundle.putBoolean("is_screen_off", true);
            Message msg = this.mHandler.obtainMessage(0);
            msg.setData(bundle);
            this.mHandler.sendMessage(msg);
        } else {
            Log.w(TAG, "executeRelatedAction other tags");
        }
    }

    private void tryToDoShutdown() {
        if (!this.mIsTelevisionMode) {
            try {
                IPowerManager pm = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
                if (pm != null) {
                    if (HwAutoUpdate.getInstance().isAutoSystemUpdate(this.mContext, false)) {
                        pm.reboot(false, "recovery", false);
                    } else {
                        this.mWindowManagerFuncs.shutdown(false);
                    }
                } else {
                    return;
                }
            } catch (RemoteException e) {
                this.mWindowManagerFuncs.shutdown(false);
                Log.e(TAG, "tryToDoShutdown PowerManager or isAutoSystemUpdate service died!");
            }
        } else {
            Log.w(TAG, "tryToDoShutdown with PowerManager goToSleep!");
            dismissShutdownMenuWithAction(SHUTDOWN_TAG, 34603152);
        }
        StatisticalUtils.reporte(this.mContext, 22, "{action:shutdown, state:confrim}");
        ShutdownMenuAnimations.getInstance(this.mContext).setIsAnimRunning(true);
    }

    private void callPowerManagerMethod(int flags) {
        Object powerObject = this.mContext.getSystemService("power");
        if (powerObject instanceof PowerManager) {
            ((PowerManager) powerObject).goToSleep(SystemClock.uptimeMillis(), 4, flags);
        } else {
            Log.e(TAG, "callPowerManagerMethod, PowerManager service died!");
        }
    }

    public boolean isHwFastShutdownEnable() {
        return this.mHwFastShutdownEnable;
    }

    private void checkFastShutdownCondition() {
        IHwShutdownThread iHwShutdownThread2 = iHwShutdownThread;
        boolean z = false;
        if (iHwShutdownThread2 == null || !iHwShutdownThread2.isShutDownAnimationAvailable()) {
            if (FASTSHUTDOWN_PROP && !isSDCardMounted()) {
                z = true;
            }
            this.mHwFastShutdownEnable = z;
        } else {
            this.mHwFastShutdownEnable = false;
        }
        HwLog.d(TAG, "checkFastShutdownCondition: fastshutdown=" + this.mHwFastShutdownEnable + " ,sdcardmounted=" + isSDCardMounted());
    }

    private boolean isSDCardMounted() {
        StorageManager storageManager = (StorageManager) this.mContext.getSystemService("storage");
        if (storageManager != null) {
            StorageVolume[] volumeList = storageManager.getVolumeList();
            for (StorageVolume storageVolume : volumeList) {
                if (storageVolume.isRemovable() && !storageVolume.getPath().contains("usb") && "mounted".equals(storageManager.getVolumeState(storageVolume.getPath()))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isOtherAreaPressedInTalkback() {
        HwGlobalActionsView hwGlobalActionsView = this.mGlobalActionsView;
        if (hwGlobalActionsView == null || !hwGlobalActionsView.isAccessibilityFocused()) {
            return false;
        }
        return true;
    }

    public void onDismiss(DialogInterface dialog) {
    }

    public void onClick(DialogInterface dialog, int which) {
    }

    public abstract class ToggleAction implements Action {
        public abstract void onToggle();

        public ToggleAction() {
        }

        @Override // huawei.com.android.server.policy.HwGlobalActions.Action
        public final void onPress() {
            if (isInTransition()) {
                Log.w(HwGlobalActions.TAG, "shouldn't be able to toggle when in transition");
                return;
            }
            onToggle();
            changeStateFromPress();
        }

        @Override // huawei.com.android.server.policy.HwGlobalActions.Action
        public boolean onLongPress() {
            return false;
        }

        /* access modifiers changed from: protected */
        public void changeStateFromPress() {
        }

        public void updateState() {
        }

        /* access modifiers changed from: protected */
        public boolean isInTransition() {
            return false;
        }
    }

    private void initGlobalActions() {
        if (this.mSilentModeAction == null) {
            this.mSilentModeAction = new ToggleAction() {
                /* class huawei.com.android.server.policy.HwGlobalActions.AnonymousClass8 */

                @Override // huawei.com.android.server.policy.HwGlobalActions.ToggleAction
                public void onToggle() {
                    HwGlobalActions hwGlobalActions = HwGlobalActions.this;
                    AudioManager unused = hwGlobalActions.mAudioManager = hwGlobalActions.getAudioService(hwGlobalActions.mContext);
                    if (HwGlobalActions.this.mAudioManager == null) {
                        Log.e(HwGlobalActions.TAG, "AudioManager is null !!");
                    } else {
                        HwGlobalActions.this.tryToImplementToggle();
                    }
                }

                /* access modifiers changed from: protected */
                @Override // huawei.com.android.server.policy.HwGlobalActions.ToggleAction
                public boolean isInTransition() {
                    if ((HwGlobalActions.this.mGlobalactionsData.getState() & 128) != 0) {
                        return true;
                    }
                    return false;
                }

                /* access modifiers changed from: protected */
                @Override // huawei.com.android.server.policy.HwGlobalActions.ToggleAction
                public void changeStateFromPress() {
                    HwGlobalActions.this.mGlobalactionsData.setSilentMode(HwGlobalActions.this.mGlobalactionsData.getState() | 128);
                }

                @Override // huawei.com.android.server.policy.HwGlobalActions.ToggleAction
                public void updateState() {
                    HwGlobalActions.this.updateGlobalActionsSilentmodeState();
                }
            };
        }
    }

    /* access modifiers changed from: private */
    public void tryToImplementToggle() {
        String targetStateName = HwAPPQoEUserAction.DEFAULT_CHIP_TYPE;
        int ringerMode = this.mAudioManager.getRingerMode();
        if (ringerMode == 0) {
            this.mAudioManager.setRingerMode(2);
            targetStateName = "normal";
        } else if (ringerMode != 1) {
            if (ringerMode == 2) {
                if (this.mHasVibrator) {
                    this.mAudioManager.setRingerMode(1);
                    targetStateName = "vibrate";
                } else {
                    this.mAudioManager.setRingerMode(0);
                    targetStateName = "silent";
                }
            }
        } else if (this.mHasVibrator) {
            this.mAudioManager.setRingerMode(0);
            targetStateName = "silent";
        }
        StatisticalUtils.reporte(this.mContext, 22, String.format(Locale.ROOT, "{action:sound, state:%s}", targetStateName));
    }

    /* access modifiers changed from: private */
    public void updateGlobalActionsSilentmodeState() {
        AudioManager mAudioManager2 = getAudioService(this.mContext);
        if (mAudioManager2 == null) {
            Log.e(TAG, "AudioManager is null !!");
            return;
        }
        int ringerMode = mAudioManager2.getRingerMode();
        if (ringerMode == 0) {
            this.mGlobalactionsData.setSilentMode(16);
        } else if (ringerMode == 1) {
            this.mGlobalactionsData.setSilentMode(32);
        } else if (ringerMode == 2) {
            this.mGlobalactionsData.setSilentMode(64);
        }
    }

    /* access modifiers changed from: private */
    public AudioManager getAudioService(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService("audio");
        if (audioManager != null) {
            return audioManager;
        }
        return null;
    }

    private void updateGlobalActionsAirplanemodeState() {
        boolean airplaneModeOn = false;
        int i = 1;
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1) {
            airplaneModeOn = true;
        }
        HwGlobalActionsData hwGlobalActionsData = this.mGlobalactionsData;
        if (!airplaneModeOn) {
            i = 2;
        }
        hwGlobalActionsData.setAirplaneMode(i);
    }

    private void initGlobalActionsData(boolean keyguardShowing, boolean keyguardSecure, boolean isDeviceProvisioned) {
        this.mGlobalactionsData = HwGlobalActionsData.getSingletoneInstance();
        this.mGlobalactionsData.init(keyguardShowing, keyguardSecure, isDeviceProvisioned);
        updateGlobalActionsAirplanemodeState();
        updateGlobalActionsSilentmodeState();
        this.mGlobalactionsData.setRebootMode(256);
        this.mGlobalactionsData.setShutdownMode(4096);
        this.mGlobalactionsData.setLockdownMode(65536);
    }

    private void createGlobalActionsView() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, HwArbitrationDEFS.MSG_MPLINK_UNBIND_SUCCESS, 16909569, -2);
        lp.privateFlags |= Integer.MIN_VALUE;
        lp.layoutInDisplayCutoutMode = 1;
        new WindowManagerEx.LayoutParamsEx(lp).setDisplaySideMode(1);
        if (!ShutdownMenuAnimations.isSuperLiteMode()) {
            lp.windowAnimations = this.mContext.getResources().getIdentifier("androidhwext:style/HwAnimation.GlobalActionsView", null, null);
        }
        if (this.ROTATION == 0) {
            lp.screenOrientation = 5;
        }
        Log.d(TAG, " lp.screenOrientation = " + lp.screenOrientation);
        lp.setTitle(TAG);
        WindowManager wm = (WindowManager) this.mContext.getSystemService("window");
        HwGlobalActionsView hwGlobalActionsView = this.mGlobalActionsView;
        if (hwGlobalActionsView != null) {
            wm.removeView(hwGlobalActionsView);
            this.mGlobalActionsView = null;
        }
        this.mGlobalActionsView = (HwGlobalActionsView) LayoutInflater.from(new ContextThemeWrapper(this.mContext, this.mContext.getResources().getIdentifier("androidhwext:style/Theme.Emui.Dark", null, null))).inflate(34013228, (ViewGroup) null);
        lp.screenOrientation = convertRotationToScreenOrientation(wm.getDefaultDisplay().getRotation());
        this.mGlobalActionsView.setContentDescription(this.mContext.getResources().getString(33685738));
        this.mGlobalActionsView.setVisibility(4);
        this.mGlobalActionsView.initUI(this.mHandler.getLooper());
        this.mGlobalActionsView.registerActionPressedCallback(this);
        if (!this.mIsTelevisionMode) {
            this.mGlobalActionsView.requestFocus();
        }
        wm.addView(this.mGlobalActionsView, lp);
        this.mGlobalActionsView.setSystemUiVisibility(16909569 | 4);
    }

    private int convertRotationToScreenOrientation(int rotation) {
        if (rotation == 0) {
            return 1;
        }
        if (rotation == 1) {
            return 0;
        }
        if (rotation == 2) {
            return 9;
        }
        if (rotation == 3) {
            return 8;
        }
        Log.w(TAG, "convertRotationToScreenOrientation: not expected rotation = " + rotation);
        return 1;
    }

    public void showGlobalActionsView() {
        if (this.mGlobalActionsView != null) {
            this.mHwPhoneWindowManager.handleCloseMobileViewChanged(true);
            if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer()) {
                sendStatusbarHideNotification();
                HwPCUtils.log(TAG, "showGlobalActionsView()--show sendStatusbarHideNotification");
            }
            sendHiVoiceHideNotification();
            this.mGlobalActionsView.setVisibility(0);
            this.mHwGlobalActionsShowing = true;
            Log.i(TAG, "show GlobalActionsView finish");
        }
    }

    public void hideGlobalActionsView(final boolean isLockdown, final boolean isScreenoff) {
        if (this.mGlobalActionsView != null) {
            if (ShutdownMenuAnimations.isSuperLiteMode()) {
                HwLog.d(TAG, "Super lite mode don't need animations. isLockdown " + isLockdown);
                tryToDoLockdown(isLockdown);
                tryToDoScrennoff(isScreenoff);
                hideGlobalActionViewEnd();
            } else {
                ObjectAnimator alpha_global_action = ObjectAnimator.ofFloat(this.mGlobalActionsView, "alpha", 1.0f, 0.0f);
                if (isLockdown) {
                    alpha_global_action = ObjectAnimator.ofFloat(this.mGlobalActionsView, "alpha", 1.0f, 1.0f);
                    alpha_global_action.setInterpolator(ShutdownMenuAnimations.CubicBezier_40_0);
                    alpha_global_action.setDuration(200L);
                } else {
                    alpha_global_action.setInterpolator(ShutdownMenuAnimations.CUBIC_BEZIER_33_33);
                    alpha_global_action.setDuration(350L);
                }
                alpha_global_action.addListener(new Animator.AnimatorListener() {
                    /* class huawei.com.android.server.policy.HwGlobalActions.AnonymousClass9 */

                    public void onAnimationStart(Animator arg0) {
                        HwGlobalActions.this.tryToDoLockdown(isLockdown);
                        HwGlobalActions.this.tryToDoScrennoff(isScreenoff);
                    }

                    public void onAnimationRepeat(Animator arg0) {
                    }

                    public void onAnimationEnd(Animator arg0) {
                        HwGlobalActions.this.hideGlobalActionViewEnd();
                    }

                    public void onAnimationCancel(Animator arg0) {
                    }
                });
                alpha_global_action.start();
            }
        }
        this.mHwGlobalActionsShowing = false;
        ShutdownMenuAnimations.getInstance(this.mContext).setIsAnimRunning(false);
        if (this.ROTATION == ROTATION_NINETY && this.mSystemRotation >= 0) {
            Settings.System.putInt(this.mContext.getContentResolver(), "accelerometer_rotation", this.mSystemRotation);
            this.mSystemRotation = -1;
        }
        this.mHwPhoneWindowManager.handleCloseMobileViewChanged(false);
    }

    /* access modifiers changed from: private */
    public void tryToDoScrennoff(boolean isScreenoff) {
        if (this.mIsTelevisionMode && isScreenoff) {
            callPowerManagerMethod(0);
        }
    }

    /* access modifiers changed from: private */
    public void tryToDoLockdown(boolean isLockdown) {
        if (isLockdown) {
            new LockPatternUtils(this.mContext).requireStrongAuth(32, -1);
            try {
                WindowManagerGlobal.getWindowManagerService().lockNow((Bundle) null);
            } catch (RemoteException e) {
                Log.e(TAG, "Error while trying to lock device.", e);
            }
        }
    }

    /* access modifiers changed from: private */
    public void hideGlobalActionViewEnd() {
        HwGlobalActionsView hwGlobalActionsView = this.mGlobalActionsView;
        if (hwGlobalActionsView != null) {
            hwGlobalActionsView.setBackgroundDrawable(null);
            this.mBlurDrawable = null;
            this.mGlobalActionsView.deinitUI();
            this.mGlobalActionsView.unregisterActionPressedCallback();
            this.mGlobalActionsView.setVisibility(8);
            ((WindowManager) this.mContext.getSystemService("window")).removeView(this.mGlobalActionsView);
            this.mGlobalActionsView = null;
            sendGlobalActionsIntent(false);
        }
        if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer()) {
            sendStatusbarShowNotification();
            HwPCUtils.log(TAG, "showGlobalActionsView()--hide sendStatusbarShowNotification");
        }
    }

    public boolean isHwGlobalActionsShowing() {
        return this.mHwGlobalActionsShowing;
    }

    /* access modifiers changed from: private */
    public void changeAirplaneModeSystemSetting(boolean on) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "airplane_mode_on", on ? 1 : 0);
        Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
        intent.addFlags(536870912);
        intent.putExtra(SceneRecogFeature.DATA_STATE, on);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        dismissShutdownMenu(200);
    }

    /* access modifiers changed from: private */
    public void doHandlerMessageDismiss(Message msg) {
        boolean isLockdown = false;
        boolean isScreenoff = false;
        Bundle bundle = msg.getData();
        if (bundle != null) {
            isLockdown = bundle.getBoolean("is_lock_down", false);
            isScreenoff = bundle.getBoolean("is_screen_off", false);
        }
        hideGlobalActionsView(isLockdown, isScreenoff);
    }

    /* access modifiers changed from: private */
    public void doHandlerMessageUpdateRebootMode() {
        HwGlobalActionsData hwGlobalActionsData = this.mGlobalactionsData;
        if (hwGlobalActionsData == null) {
            return;
        }
        if ((hwGlobalActionsData.getState() & 512) == 0) {
            ShutdownMenuAnimations.getInstance(this.mContext).setImageAnimation(false).start();
            this.mGlobalactionsData.setRebootMode(512);
            return;
        }
        ShutdownMenuAnimations.getInstance(this.mContext).rebackNewShutdownMenu(this.mGlobalactionsData.getState());
        this.mGlobalactionsData.setRebootMode(256);
    }

    /* access modifiers changed from: private */
    public void doHandlerMessageUpdateShutdownMode() {
        HwGlobalActionsData hwGlobalActionsData = this.mGlobalactionsData;
        if (hwGlobalActionsData == null) {
            return;
        }
        if ((hwGlobalActionsData.getState() & 8192) == 0) {
            ShutdownMenuAnimations.getInstance(this.mContext).setImageAnimation(false).start();
            this.mGlobalactionsData.setShutdownMode(8192);
            return;
        }
        ShutdownMenuAnimations.getInstance(this.mContext).rebackNewShutdownMenu(this.mGlobalactionsData.getState());
        this.mGlobalactionsData.setShutdownMode(4096);
    }

    /* access modifiers changed from: private */
    public void doHandlerMessageUpdateLockdownMode() {
        HwGlobalActionsData hwGlobalActionsData = this.mGlobalactionsData;
        if (hwGlobalActionsData == null) {
            return;
        }
        if ((hwGlobalActionsData.getState() & 131072) == 0) {
            ShutdownMenuAnimations.getInstance(this.mContext).setImageAnimation(false).start();
            this.mGlobalactionsData.setLockdownMode(131072);
            return;
        }
        ShutdownMenuAnimations.getInstance(this.mContext).rebackNewShutdownMenu(this.mGlobalactionsData.getState());
        this.mGlobalactionsData.setLockdownMode(65536);
    }

    /* access modifiers changed from: private */
    public void doHandlerMessageBlurBitmap(Message msg) {
        Bitmap currBitmap = null;
        this.mThread = null;
        if (msg.obj instanceof Bitmap) {
            Bitmap blurBitmap = (Bitmap) msg.obj;
            BitmapDrawable bitmapDrawable = this.mBlurDrawable;
            if (bitmapDrawable != null) {
                currBitmap = bitmapDrawable.getBitmap();
            }
            if (!(currBitmap == null || currBitmap == blurBitmap)) {
                currBitmap.recycle();
            }
            this.mBlurDrawable = new BitmapDrawable(this.mContext.getResources(), blurBitmap);
        }
        setDrawableBound();
        doHandleShowWork();
    }

    private class BitmapThread extends Thread {
        public BitmapThread() {
        }

        public void run() {
            super.run();
            synchronized (HwGlobalActions.this.mLock) {
                if (!isInterrupted()) {
                    Bitmap screenShot = null;
                    if (!ShutdownMenuAnimations.isSuperLiteMode()) {
                        try {
                            screenShot = BlurUtils.screenShotBitmap(HwGlobalActions.this.mContext, 0, HwGlobalActions.MAXLAYER, HwGlobalActions.SCALE, new Rect());
                        } catch (ClassCastException e) {
                            Log.e(HwGlobalActions.TAG, "BlurUtils.screenShotBitmap() ClassCastException.");
                        } catch (Exception e2) {
                            Log.e(HwGlobalActions.TAG, "Screenshot Exception.");
                        } catch (Error err) {
                            Log.e(HwGlobalActions.TAG, "startBlurScreenshotThread  Error er = " + err.getMessage());
                        }
                    } else {
                        Log.w(HwGlobalActions.TAG, "Phone is in super lite mode, use default color instead of screenshot.");
                    }
                    if (screenShot == null) {
                        Log.e(HwGlobalActions.TAG, "start screen shot fail, we fill it with a default color.");
                        HwGlobalActions.this.notifyBlurResult(getDefaultBitmap());
                        return;
                    }
                    if (!screenShot.isMutable() || screenShot.getConfig() != Bitmap.Config.ARGB_8888) {
                        Bitmap tmp = screenShot.copy(Bitmap.Config.ARGB_8888, true);
                        screenShot.recycle();
                        screenShot = tmp;
                    }
                    int originWidth = screenShot.getWidth();
                    int originHeight = screenShot.getHeight();
                    HwGlobalActions.this.findRadius(HwGlobalActions.this.mBlurRadius);
                    Bitmap screenShot2 = HwGlobalActions.this.scaleBlurBitmap(screenShot, originWidth / HwGlobalActions.this.mScale, originHeight / HwGlobalActions.this.mScale, true);
                    if (screenShot2 == null) {
                        Log.e(HwGlobalActions.TAG, "scaleBlurBitmap return null firstly, fill it with a default color.");
                        HwGlobalActions.this.notifyBlurResult(getDefaultBitmap());
                        return;
                    }
                    BlurUtils.blurImage(HwGlobalActions.this.mContext, screenShot2, screenShot2, HwGlobalActions.this.mRadius);
                    Bitmap screenShot3 = HwGlobalActions.this.scaleBlurBitmap(screenShot2, originWidth, originHeight, true);
                    if (screenShot3 == null) {
                        Log.e(HwGlobalActions.TAG, "scaleBlurBitmap return null secondly, fill it with a default color.");
                        HwGlobalActions.this.notifyBlurResult(getDefaultBitmap());
                        return;
                    }
                    Bitmap screenShot4 = HwGlobalActions.this.addBlackBoardToBlurBitmap(screenShot3);
                    if (screenShot4 == null) {
                        Log.e(HwGlobalActions.TAG, "addBlackBoardToBlurBitmap return null, fill it with a default color.");
                        HwGlobalActions.this.notifyBlurResult(getDefaultBitmap());
                    } else if (!isInterrupted()) {
                        HwGlobalActions.this.notifyBlurResult(screenShot4);
                    }
                }
            }
        }

        private Bitmap getDefaultBitmap() {
            Bitmap defaultBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
            defaultBitmap.eraseColor(-872415232);
            return defaultBitmap;
        }
    }

    /* access modifiers changed from: private */
    public final void notifyBlurResult(Bitmap bitmap) {
        Message msg = Message.obtain();
        msg.obj = bitmap;
        msg.what = 7;
        this.mHandler.sendMessage(msg);
        Log.i(TAG, "end background blur");
    }

    private void setDrawableBound() {
        if (this.mBlurDrawable != null) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay().getRealMetrics(displayMetrics);
            this.mBlurDrawable.setBounds(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        }
    }

    private void startBlurScreenshotThread() {
        this.mThread = new BitmapThread();
        this.mThread.start();
        Log.i(TAG, "start background blur");
    }

    private void sendStatusbarHideNotification() {
        Intent intent = new Intent();
        intent.setAction(ACTION_NOTIFY_STATUSBAR_HIDE);
        this.mContext.sendBroadcast(intent, PERMISSION_BROADCAST_GLOBALACTIONS_VIEW_STATE_CHANGED);
    }

    private void sendStatusbarShowNotification() {
        Intent intent = new Intent();
        intent.setAction(ACTION_NOTIFY_STATUSBAR_SHOW);
        this.mContext.sendBroadcast(intent, PERMISSION_BROADCAST_GLOBALACTIONS_VIEW_STATE_CHANGED);
    }

    private void sendHiVoiceHideNotification() {
        Intent intent = new Intent();
        intent.setAction(this.mActionNotifyHivoiceHide);
        intent.setPackage(this.mHivoicePackage);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT, this.mPermissionBroadcastNotifyHivoiceHide);
    }

    /* access modifiers changed from: private */
    public void findRadius(int blurRadius) {
        int i = this.mScale;
        int radius = blurRadius / i;
        if (radius < 25) {
            this.mRadius = radius;
            return;
        }
        this.mScale = i + 1;
        findRadius(blurRadius);
    }

    /* access modifiers changed from: private */
    public Bitmap scaleBlurBitmap(Bitmap src, int dstWidth, int dstHeight, boolean filter) {
        if (this.mBlurRadius < 25) {
            return src;
        }
        Bitmap screenSampling = Bitmap.createScaledBitmap(src, dstWidth, dstHeight, filter);
        if (!src.isRecycled()) {
            src.recycle();
            return screenSampling;
        }
        Log.w(TAG, "Bitmap is isRecycled");
        return screenSampling;
    }

    /* access modifiers changed from: private */
    public Bitmap addBlackBoardToBlurBitmap(Bitmap screenShot) {
        Bitmap screenBitmap = null;
        int blackBoard = MASK_PLANE_COLOR;
        if (this.mIsTelevisionMode) {
            blackBoard = -872415232;
        }
        if (screenShot != null) {
            if (HwPicAverageNoises.isAverageNoiseSupported()) {
                screenBitmap = new HwPicAverageNoises().addNoiseWithBlackBoard(screenShot, blackBoard);
            } else {
                screenBitmap = BlurUtils.addBlackBoard(screenShot, blackBoard);
            }
            screenShot.recycle();
        }
        return screenBitmap;
    }

    private boolean isAppExist(Context context, String pkgName) {
        if (context == null || pkgName == null) {
            Log.e(TAG, "Parameter context or pkgName is invalid.");
            return false;
        }
        try {
            if (context.getPackageManager().getPackageInfo(pkgName, 0) != null) {
                return true;
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "PackageManager doesn't exist: " + pkgName);
        }
    }

    private void initPkgName() {
        if (isAppExist(this.mContext, HIVOICE_PACAKGE_NEW)) {
            this.mHivoicePackage = HIVOICE_PACAKGE_NEW;
            this.mActionNotifyHivoiceHide = ACTION_NOTIFY_HIVOICE_HIDE_NEW;
            this.mPermissionBroadcastNotifyHivoiceHide = PERMISSION_BROADCAST_NOTIFY_HIVOICE_HIDE_NEW;
            return;
        }
        this.mHivoicePackage = HIVOICE_PACAKGE_OLD;
        this.mActionNotifyHivoiceHide = ACTION_NOTIFY_HIVOICE_HIDE_OLD;
        this.mPermissionBroadcastNotifyHivoiceHide = PERMISSION_BROADCAST_NOTIFY_HIVOICE_HIDE_OLD;
    }
}
