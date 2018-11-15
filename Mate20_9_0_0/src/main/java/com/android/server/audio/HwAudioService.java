package com.android.server.audio;

import android.aft.HwAftPolicyManager;
import android.aft.IHwAftPolicyService;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManagerNative;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.soundtrigger.SoundTrigger.GenericSoundModel;
import android.hdm.HwDeviceManager;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioRoutesInfo;
import android.media.AudioSystem;
import android.media.HwMediaMonitorManager;
import android.media.IAudioRoutesObserver;
import android.media.PlayerBase.PlayerIdCard;
import android.media.SoundPool;
import android.media.soundtrigger.SoundTriggerDetector;
import android.media.soundtrigger.SoundTriggerDetector.Callback;
import android.media.soundtrigger.SoundTriggerDetector.EventPayload;
import android.media.soundtrigger.SoundTriggerManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.provider.Settings.Secure;
import android.provider.SettingsEx.Systemex;
import android.rms.iaware.IAwareSdk;
import android.rms.iaware.IForegroundAppTypeCallback;
import android.rms.iaware.LogIAware;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.Xml;
import android.view.ContextThemeWrapper;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;
import com.android.internal.app.ISoundTriggerService;
import com.android.internal.content.PackageMonitor;
import com.android.server.DeviceIdleController.LocalService;
import com.android.server.HwServiceFactory;
import com.android.server.LocalServices;
import com.android.server.am.ActivityManagerService;
import com.android.server.audio.AbsAudioService.DeviceVolumeState;
import com.android.server.audio.AudioService.SetModeDeathHandler;
import com.android.server.audio.report.AudioExceptionMsg;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.notification.HwCustZenModeHelper;
import com.android.server.pm.UserManagerService;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.huawei.android.app.ActivityManagerEx;
import com.huawei.android.app.IGameObserver.Stub;
import com.huawei.displayengine.IDisplayEngineService;
import huawei.cust.HwCustUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsCompModeID;

public class HwAudioService extends AudioService {
    private static final String ACTION_COMMFORCE = "huawei.intent.action.COMMFORCE";
    private static final String ACTION_INCALL_EXTRA = "IsForegroundActivity";
    private static final String ACTION_INCALL_SCREEN = "InCallScreenIsForegroundActivity";
    private static final String ACTION_START_SOUNDTRIGGER_DETECTOR = "com.huawei.vassistant.intent.action.START_SOUNDTRIGGER";
    private static final String ACTION_WAKEUP_SERVICE = "com.huawei.wakeup.services.WakeupService";
    private static final String BLUETOOTH_PKG_NAME = "com.android.bluetooth";
    private static final String CURRENT_PRODUCT_NAME = SystemProperties.get("ro.product.device", null);
    private static final boolean DEBUG = SystemProperties.getBoolean("ro.media.debuggable", false);
    private static final boolean DEBUG_THEME_SOUND = false;
    private static final int DEVICE_IN_HEADPHONE = -1979705328;
    private static final int DEVICE_OUT_HEADPHONE = 604004364;
    private static final int DUALPA_RCV_DEALY_DURATION = 5000;
    private static final String DUALPA_RCV_DEALY_OFF = "dualpa_security_delay=0";
    private static final String DUALPA_RCV_DEALY_ON = "dualpa_security_delay=1";
    private static final String EXCLUSIVE_PRODUCT_NAME = "HWH1711-Q";
    private static final String EXTRA_SOUNDTRIGGER_STATE = "soundtrigger_state";
    private static final int GAMEMODE_BACKGROUND = 2;
    private static final int GAMEMODE_FOREGROUND = 9;
    public static final int HW_AUDIO_EXCEPTION_OCCOUR = -1000;
    public static final int HW_AUDIO_MODE_TYPE_EXCEPTION_OCCOUR = -1001;
    public static final int HW_AUDIO_TRACK_OVERFLOW_TYPE_EXCEPTION_OCCOUR = -1002;
    public static final int HW_MIC_MUTE_EXCEPTION_OCCOUR = -2002;
    public static final int HW_RING_MODE_TYPE_EXCEPTION_OCCOUR = -2001;
    public static final int HW_SCO_CONNECT_EXCEPTION_OCCOUR = -2003;
    private static final int JUDGE_MILLISECONDS = 5000;
    private static final String KEY_SOUNDTRIGGER_SWITCH = "hw_soundtrigger_enabled";
    private static final boolean LOW_LATENCY_SUPPORT = SystemProperties.getBoolean("persist.media.lowlatency.enable", false);
    private static final String LOW_LATENCY_WHITE_LIST = "/odm/etc/audio/low_latency/white_list.xml";
    private static final UUID MODEL_UUID = UUID.fromString("7dc67ab3-eab6-4e34-b62a-f4fa3788092a");
    private static final int MSG_AUDIO_EXCEPTION_OCCUR = 10000;
    private static final int MSG_DISABLE_HEADPHONE = 91;
    private static final int MSG_DUALPA_RCV_DEALY = 71;
    private static final int MSG_RELOAD_SNDEFFS = 99;
    private static final int MSG_RINGER_MODE_CHANGE = 80;
    private static final int MSG_SHOW_DISABLE_HEADPHONE_TOAST = 92;
    private static final int MSG_SHOW_DISABLE_MICROPHONE_TOAST = 90;
    private static final String NODE_ATTR_PACKAGE = "package";
    private static final String NODE_WHITEAPP = "whiteapp";
    private static final int PACKAGE_ADDED = 1;
    private static final int PACKAGE_REMOVED = 2;
    private static final String PERMISSION_COMMFORCE = "android.permission.COMM_FORCE";
    public static final int PID_BIT_WIDE = 21;
    private static final String PROP_DESKTOP_MODE = "sys.desktop.mode";
    private static final int RADIO_UID = 1001;
    private static final int RECORDSTATE_STOPPED = 1;
    private static final String SESSION_ID = "session_id";
    private static final String SOUNDTRIGGER_MAD_OFF = "mad=off";
    private static final String SOUNDTRIGGER_MAD_ON = "mad=on";
    private static final String SOUNDTRIGGER_START = "start";
    private static final int SOUNDTRIGGER_STATUS_OFF = 0;
    private static final int SOUNDTRIGGER_STATUS_ON = 1;
    private static final String SOUNDTRIGGER_STOP = "stop";
    private static final String SOUNDTRIGGER_WAKUP_OFF = "wakeup=off";
    private static final String SOUNDTRIGGER_WAKUP_ON = "wakeup=on";
    private static final int SOUNDVIBRATE_AS_GAME = 4;
    private static final int SOUNDVIBRATE_BACKGROUND = 2;
    private static final int SOUNDVIBRATE_FOREGROUND = 1;
    private static final int SOUNDVIBRATE_PROP_OFF = 0;
    private static final int SOUNDVIBRATE_PROP_ON = 1;
    private static final int SOUNDVIBRATE_SETTINGS_OFF = 0;
    private static final int SOUNDVIBRATE_SETTINGS_ON = 1;
    private static final int SPK_RCV_STEREO_OFF = 0;
    private static final String SPK_RCV_STEREO_OFF_PARA = "stereo_landscape_portrait_enable=0";
    private static final int SPK_RCV_STEREO_ON = 1;
    private static final String SPK_RCV_STEREO_ON_PARA = "stereo_landscape_portrait_enable=1";
    private static final boolean SUPPORT_GAME_MODE = SystemProperties.getBoolean("ro.config.dolby_game_mode", false);
    private static final String SYSKEY_SOUND_HWT = "syskey_sound_hwt";
    private static final int SYSTEM_UID = 1000;
    private static final String TAG = "HwAudioService";
    private static final String TAG_CTAIFS = "ctaifs";
    private static final long THREE_SEC_LIMITED = 3000;
    private static final String VASSISTANT_PACKAGE_NAME = "com.huawei.vassistant";
    private static boolean isCallForeground = false;
    private static final boolean mIsHuaweiSafeMediaConfig = SystemProperties.getBoolean("ro.config.huawei_safe_media", true);
    private static PhoneStateListener mPhoneListener = new PhoneStateListener() {
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch (state) {
                case 0:
                    SystemProperties.set("persist.sys.audio_mode", "0");
                    return;
                case 1:
                    SystemProperties.set("persist.sys.audio_mode", "1");
                    return;
                case 2:
                    SystemProperties.set("persist.sys.audio_mode", "2");
                    return;
                default:
                    return;
            }
        }
    };
    private static final int mSecurityVolumeIndex = SystemProperties.getInt("ro.config.hw.security_volume", 0);
    private static final boolean mVoipOptimizeInGameMode = SystemProperties.getBoolean("ro.config.gameassist_voipopt", false);
    private boolean IS_IN_GAMEMODE = false;
    private long dialogShowTime = 0;
    private boolean isShowingDialog = false;
    private AudioExceptionRecord mAudioExceptionRecord = null;
    private IBinder mBinder;
    private Callback mCallBack = new Callback() {
        public void onAvailabilityChanged(int var1) {
        }

        public void onDetected(EventPayload var1) {
            String str = HwAudioService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onDetected() called with: eventPayload = [");
            stringBuilder.append(var1);
            stringBuilder.append("]");
            Slog.i(str, stringBuilder.toString());
            if (var1.getCaptureSession() != null) {
                LocalService idleController = (LocalService) LocalServices.getService(LocalService.class);
                if (idleController != null) {
                    Slog.i(HwAudioService.TAG, "addPowerSaveTempWhitelistApp#va");
                    idleController.addPowerSaveTempWhitelistApp(Process.myUid(), HwAudioService.VASSISTANT_PACKAGE_NAME, MemoryConstant.MIN_INTERVAL_OP_TIMEOUT, HwAudioService.getCurrentUserId(), false, "hivoice");
                }
                HwAudioService.this.startWakeupService(var1.getCaptureSession().intValue());
                return;
            }
            Slog.e(HwAudioService.TAG, "session invalid!");
        }

        public void onError() {
            Slog.e(HwAudioService.TAG, "onError()");
        }

        public void onRecognitionPaused() {
        }

        public void onRecognitionResumed() {
        }
    };
    private ContentResolver mContentResolver;
    private Context mContext;
    private HwCustZenModeHelper mCustZenModeHelper;
    DeathRecipient mDeathRecipient = new DeathRecipient() {
        public void binderDied() {
            Slog.e(HwAudioService.TAG, "binderDied");
            HwAudioService.this.startSoundTriggerV2(true);
        }
    };
    protected volatile int mDesktopMode = -1;
    private boolean mEnableAdjustVolume = true;
    private EnableVolumeClient mEnableVolumeClient = null;
    private final Object mEnableVolumeLock = new Object();
    private ArrayList<HeadphoneInfo> mHeadphones = new ArrayList();
    private int mHeadsetSwitchState = 0;
    private HwThemeHandler mHwThemeHandler;
    private boolean mIsFmConnected = false;
    private boolean mIsLinkDeathRecipient = false;
    private Toast mLVMToast = null;
    private long mLastSetMode3Time = -1;
    private long mLastStopAudioRecordTime = -1;
    private long mLastStopPlayBackTime = -1;
    private final BroadcastReceiver mLowlatencyReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                HwAudioService.this.isLowlatencyPkg(intent.getData().getSchemeSpecificPart());
            } else if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                String packageName = intent.getData().getSchemeSpecificPart();
                if (HwAudioService.this.isLowlatencyPkg(packageName)) {
                    HwAudioService.this.updateLowlatencyUidsMap(packageName, 1);
                }
            }
        }
    };
    private Map<String, Integer> mLowlatencyUidsMap = new ArrayMap();
    private NotificationManager mNm;
    private final PackageMonitor mPackageMonitor = new PackageMonitor() {
        public void onPackageRemoved(String packageName, int uid) {
            if (HwAudioService.VASSISTANT_PACKAGE_NAME.equals(packageName)) {
                String str = HwAudioService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onPackageRemoved uid :");
                stringBuilder.append(uid);
                Slog.i(str, stringBuilder.toString());
                HwAudioService.this.updateVAVersionCode();
            }
        }

        public void onPackageDataCleared(String packageName, int uid) {
            if (HwAudioService.VASSISTANT_PACKAGE_NAME.equals(packageName)) {
                String str = HwAudioService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onPackageDataCleared uid : ");
                stringBuilder.append(uid);
                Slog.i(str, stringBuilder.toString());
                HwAudioService.this.resetVASoundTrigger();
            }
        }

        public void onPackageAppeared(String packageName, int reason) {
            if (HwAudioService.VASSISTANT_PACKAGE_NAME.equals(packageName)) {
                String str = HwAudioService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onPackageUpdateStarted reason : ");
                stringBuilder.append(reason);
                Slog.i(str, stringBuilder.toString());
                HwAudioService.this.updateVAVersionCode();
            }
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String str;
            if (action != null && action.equals("android.intent.action.FM")) {
                int state = intent.getIntExtra("state", 0);
                if (HwAudioService.DEBUG) {
                    String str2 = HwAudioService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Broadcast Receiver: Got ACTION_FMRx_PLUG, state =");
                    stringBuilder.append(state);
                    Slog.i(str2, stringBuilder.toString());
                }
                boolean isConnected = HwAudioService.this.mIsFmConnected;
                if (state == 0 && isConnected) {
                    AudioSystem.setDeviceConnectionState(HighBitsCompModeID.MODE_COLOR_ENHANCE, 0, "", "");
                    HwAudioService.this.mIsFmConnected = false;
                } else if (state == 1 && !isConnected) {
                    AudioSystem.setDeviceConnectionState(HighBitsCompModeID.MODE_COLOR_ENHANCE, 1, "", "");
                    HwAudioService.this.mIsFmConnected = true;
                }
            } else if (action != null && action.equals("huawei.intent.action.RINGTONE_CHANGE")) {
                HwAudioService.this.mSysKeyEffectFile = intent.getStringExtra("KEYTOUCH_AUDIOEFFECT_PATH");
                if (HwAudioService.this.mSysKeyEffectFile != null) {
                    HwAudioService.this.mHwThemeHandler.sendMessage(HwAudioService.this.mHwThemeHandler.obtainMessage(99, 0, 0, null));
                }
            } else if (action != null && HwAudioService.ACTION_INCALL_SCREEN.equals(action)) {
                HwAudioService.isCallForeground = intent.getBooleanExtra(HwAudioService.ACTION_INCALL_EXTRA, true);
                if (HwAudioService.DEBUG) {
                    str = HwAudioService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("isCallForeground= ");
                    stringBuilder2.append(HwAudioService.isCallForeground);
                    Slog.v(str, stringBuilder2.toString());
                }
            } else if (action != null && "android.intent.action.PHONE_STATE".equals(action)) {
                str = intent.getStringExtra("state");
                if (HwAudioService.DEBUG) {
                    String str3 = HwAudioService.TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("phone state=");
                    stringBuilder3.append(str);
                    Slog.v(str3, stringBuilder3.toString());
                }
                if (TelephonyManager.EXTRA_STATE_IDLE.equals(str)) {
                    HwAudioService.this.force_exitLVMMode();
                }
            } else if (action != null && "android.intent.action.USER_UNLOCKED".equals(action)) {
                HwAudioService.this.onUserUnlocked(intent.getIntExtra("android.intent.extra.user_handle", -10000));
            } else if (action != null && "android.intent.action.BOOT_COMPLETED".equals(action)) {
                HwAudioService.this.bindIAwareSdk();
            }
        }
    };
    private Toast mRingerModeToast = null;
    private boolean mSetVbrlibFlag = false;
    private SoundTriggerDetector mSoundTriggerDetector;
    private String mSoundTriggerGram = null;
    private Handler mSoundTriggerHandler;
    private SoundTriggerManager mSoundTriggerManager;
    private String mSoundTriggerRes = null;
    private ISoundTriggerService mSoundTriggerService;
    private int mSoundTriggerStatus = 0;
    private int mSpkRcvStereoStatus = -1;
    private String mSysKeyEffectFile;
    private UserManager mUserManager;
    private final UserManagerInternal mUserManagerInternal;
    private int mVAVersinCode;
    private String[] mZenModeWhiteList;
    private int oldInCallDevice = 0;
    private int oldLVMDevice = 0;
    private int type = -1;

    private class EnableVolumeClient implements DeathRecipient {
        private String mCallerPkg;
        private IBinder mCb;

        public String getCallerPkg() {
            return this.mCallerPkg;
        }

        EnableVolumeClient(IBinder cb) {
            if (cb != null) {
                try {
                    cb.linkToDeath(this, 0);
                } catch (RemoteException e) {
                    String str = HwAudioService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("EnableVolumeClient() could not link to ");
                    stringBuilder.append(cb);
                    stringBuilder.append(" binder death");
                    Slog.w(str, stringBuilder.toString());
                    cb = null;
                }
            }
            this.mCb = cb;
            this.mCallerPkg = HwAudioService.this.getPackageNameByPid(Binder.getCallingPid());
        }

        public void binderDied() {
            synchronized (HwAudioService.this.mEnableVolumeLock) {
                String str = HwAudioService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" EnableVolumeClient died.pkgname:");
                stringBuilder.append(this.mCallerPkg);
                Slog.i(str, stringBuilder.toString());
                HwAudioService.this.mEnableAdjustVolume = true;
                release();
            }
        }

        public void release() {
            if (this.mCb != null) {
                this.mCb.unlinkToDeath(this, 0);
                this.mCb = null;
            }
            HwAudioService.this.mEnableVolumeClient = null;
        }
    }

    private class ForeAppTypeListener extends IForegroundAppTypeCallback {
        public void reportForegroundAppType(int type, String pkg) {
            String str = HwAudioService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("now type = ");
            stringBuilder.append(HwAudioService.this.type);
            stringBuilder.append(",reportForegroundAppType = ");
            stringBuilder.append(type);
            stringBuilder.append(",pkg = ");
            stringBuilder.append(pkg);
            stringBuilder.append(",systemload = ");
            stringBuilder.append(SystemProperties.get("sys.iaware.type", "-1"));
            stringBuilder.append("mVoipOptimizeInGameMode");
            stringBuilder.append(HwAudioService.mVoipOptimizeInGameMode);
            Slog.i(str, stringBuilder.toString());
            if (type != HwAudioService.this.type) {
                HwAudioService.this.type = type;
                stringBuilder = new StringBuilder();
                stringBuilder.append(type);
                stringBuilder.append("");
                SystemProperties.set("sys.iaware.type", stringBuilder.toString());
            }
            if (type != 9) {
                if (HwAudioService.SUPPORT_GAME_MODE && HwAudioService.this.IS_IN_GAMEMODE) {
                    HwAudioService.this.mHwAudioServiceEx.setDolbyEffect(0);
                    HwAudioService.this.IS_IN_GAMEMODE = false;
                    AudioSystem.setParameters("dolby_game_mode=off");
                }
                if (HwAudioService.mVoipOptimizeInGameMode) {
                    AudioSystem.setParameters("game_mode=off");
                    return;
                }
                return;
            }
            if (!(!HwAudioService.SUPPORT_GAME_MODE || HwAudioService.this.IS_IN_GAMEMODE || AudioSystem.isStreamActive(3, 0))) {
                Slog.i(HwAudioService.TAG, "Music steam not start");
                if (HwAudioService.this.mHwAudioServiceEx.setDolbyEffect(3)) {
                    HwAudioService.this.IS_IN_GAMEMODE = true;
                    HwAudioService.this.updateLowlatencyUidsMap(pkg, 1);
                } else {
                    Slog.d(HwAudioService.TAG, "set game mode fail.");
                }
            }
            if (HwAudioService.mVoipOptimizeInGameMode) {
                Slog.d(HwAudioService.TAG, "support voip optinization during game mode.");
                AudioSystem.setParameters("game_mode=on");
            }
        }
    }

    private static class HeadphoneInfo {
        public String mCaller;
        public String mDeviceAddress;
        public String mDeviceName;
        public int mDeviceType;

        public HeadphoneInfo(int deviceType, String deviceAddress, String deviceName, String caller) {
            this.mDeviceType = deviceType;
            this.mDeviceName = deviceName;
            this.mDeviceAddress = deviceAddress;
            this.mCaller = caller;
        }
    }

    private class HwAudioGameObserver extends Stub {
        private HwAudioGameObserver() {
        }

        /* synthetic */ HwAudioGameObserver(HwAudioService x0, AnonymousClass1 x1) {
            this();
        }

        public void onGameListChanged() {
            Slog.v(HwAudioService.TAG, "onGameListChanged !");
        }

        public void onGameStatusChanged(String packageName, int event) {
            if (packageName != null) {
                String str = HwAudioService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onGameStatusChanged packageName = ");
                stringBuilder.append(packageName);
                stringBuilder.append(", event = ");
                stringBuilder.append(event);
                Slog.d(str, stringBuilder.toString());
                if (HwAudioService.this.isSupportSoundToVibrateMode() && Secure.getInt(HwAudioService.this.mContext.getContentResolver(), "sound_to_vibrate_effect", 1) == 1) {
                    if (event != 4) {
                        switch (event) {
                            case 1:
                                break;
                            case 2:
                                AudioSystem.setParameters("vbrmode=background");
                                break;
                            default:
                                Slog.w(HwAudioService.TAG, "onGameStatusChanged event not in foreground or background!");
                                break;
                        }
                    }
                    if (!HwAudioService.this.mSetVbrlibFlag) {
                        Slog.d(HwAudioService.TAG, "HwAudioGameObserver set mSetVbrlibFalg = true means firstly load the vbr lib!");
                        AudioSystem.setParameters("vbrEnter=on");
                        HwAudioService.this.mSetVbrlibFlag = true;
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("vbrmode=front;gamepackagename=");
                    stringBuilder.append(packageName);
                    AudioSystem.setParameters(stringBuilder.toString());
                }
            }
        }
    }

    private class HwThemeHandler extends Handler {
        private HwThemeHandler() {
        }

        /* synthetic */ HwThemeHandler(HwAudioService x0, AnonymousClass1 x1) {
            this();
        }

        public void handleMessage(Message msg) {
            if (99 == msg.what) {
                HwAudioService.this.reloadSoundEffects();
            }
        }
    }

    protected boolean usingHwSafeMediaConfig() {
        return mIsHuaweiSafeMediaConfig;
    }

    protected int getHwSafeMediaVolumeIndex() {
        return mSecurityVolumeIndex * 10;
    }

    protected boolean isHwSafeMediaVolumeEnabled() {
        return mSecurityVolumeIndex > 0;
    }

    protected boolean checkMMIRunning() {
        return "true".equals(SystemProperties.get("runtime.mmitest.isrunning", "false"));
    }

    public HwAudioService(Context context) {
        super(context);
        Slog.i(TAG, TAG);
        this.mContext = context;
        this.mNm = (NotificationManager) this.mContext.getSystemService("notification");
        IntentFilter intentFilter = new IntentFilter("android.intent.action.FM");
        intentFilter.addAction("huawei.intent.action.RINGTONE_CHANGE");
        intentFilter.addAction(ACTION_INCALL_SCREEN);
        intentFilter.addAction("android.intent.action.PHONE_STATE");
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        intentFilter.addAction("android.intent.action.REBOOT");
        intentFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
        intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        context.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, intentFilter, null, null);
        this.mPackageMonitor.register(this.mContext, this.mContext.getMainLooper(), UserHandle.SYSTEM, false);
        this.mContentResolver = this.mContext.getContentResolver();
        this.mAudioExceptionRecord = new AudioExceptionRecord();
        readPersistedSettingsEx(this.mContentResolver);
        setAudioSystemParameters();
        HwServiceFactory.getHwDrmDialogService().startDrmDialogService(context);
        this.mUserManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        if (LOW_LATENCY_SUPPORT) {
            IntentFilter lowlatencyIntentFilter = new IntentFilter();
            lowlatencyIntentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
            lowlatencyIntentFilter.addAction("android.intent.action.PACKAGE_ADDED");
            lowlatencyIntentFilter.addDataScheme("package");
            this.mContext.registerReceiverAsUser(this.mLowlatencyReceiver, UserHandle.ALL, lowlatencyIntentFilter, null, null);
            initLowlatencyUidsMap();
        }
        this.mCustZenModeHelper = (HwCustZenModeHelper) HwCustUtils.createObj(HwCustZenModeHelper.class, new Object[0]);
        if (this.mCustZenModeHelper != null) {
            this.mZenModeWhiteList = this.mCustZenModeHelper.getWhiteAppsInZenMode();
        }
        ((TelephonyManager) context.getSystemService("phone")).listen(mPhoneListener, 32);
        ActivityManagerEx.registerGameObserver(new HwAudioGameObserver(this, null));
    }

    private boolean isSupportSoundToVibrateMode() {
        return SystemProperties.getInt("ro.config.gameassist_soundtovibrate", 0) == 1;
    }

    public void initHwThemeHandler() {
        this.mHwThemeHandler = new HwThemeHandler(this, null);
    }

    public void reloadSoundEffects() {
        unloadSoundEffects();
        loadSoundEffects();
    }

    private void onUserUnlocked(int userId) {
        if (userId != -10000) {
            createManagers();
            boolean isPrimary = this.mUserManager.getUserInfo(userId).isPrimary();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("user unlocked ,start soundtrigger! isPrimary : ");
            stringBuilder.append(isPrimary);
            Slog.i(str, stringBuilder.toString());
            if (isPrimary) {
                updateVAVersionCode();
                createAndStartSoundTrigger(true);
            }
        }
    }

    protected void onUserBackground(int userId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onUserBackground userId : ");
        stringBuilder.append(userId);
        Slog.i(str, stringBuilder.toString());
        checkWithUserSwith(userId, true);
    }

    protected void onUserForeground(int userId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onUserForeground userId : ");
        stringBuilder.append(userId);
        Slog.i(str, stringBuilder.toString());
        checkWithUserSwith(userId, false);
    }

    private void checkWithUserSwith(int userId, boolean background) {
        createManagers();
        if (userId >= 0) {
            boolean isPrimary = UserManagerService.getInstance().getUserInfo(userId).isPrimary();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onUserBackground isPrimary : ");
            stringBuilder.append(isPrimary);
            Slog.i(str, stringBuilder.toString());
            if (isPrimary) {
                createAndStartSoundTrigger(background ^ 1);
            }
        }
    }

    private void updateVAVersionCode() {
        int versionCode = 0;
        try {
            versionCode = this.mContext.getPackageManager().getPackageInfo(VASSISTANT_PACKAGE_NAME, 128).versionCode;
        } catch (NameNotFoundException e) {
            Slog.e(TAG, "updateVAVersionCode vassistant not found!");
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateVAVersionCode versionCode : ");
        stringBuilder.append(versionCode);
        stringBuilder.append(" mVAVersinCode = ");
        stringBuilder.append(this.mVAVersinCode);
        Slog.i(str, stringBuilder.toString());
        if (versionCode < this.mVAVersinCode) {
            resetVASoundTrigger();
        }
        this.mVAVersinCode = versionCode;
    }

    private void resetVASoundTrigger() {
        startSoundTriggerV2(false);
        Secure.putInt(this.mContentResolver, KEY_SOUNDTRIGGER_SWITCH, 0);
    }

    private boolean isSoundTriggerOn() {
        return Secure.getInt(this.mContentResolver, KEY_SOUNDTRIGGER_SWITCH, 0) == 1;
    }

    private void createAndStartSoundTrigger(boolean start) {
        startSoundTriggerV2(start);
    }

    private boolean startSoundTriggerDetector() {
        boolean z;
        createManagers();
        createSoundTriggerDetector();
        GenericSoundModel model = getCurrentModel();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startSoundTriggerDetector model : ");
        stringBuilder.append(model);
        Slog.i(str, stringBuilder.toString());
        long ident = Binder.clearCallingIdentity();
        if (model != null) {
            try {
                z = true;
                if (this.mSoundTriggerDetector.startRecognition(1)) {
                    Slog.i(TAG, "start recognition successfully!");
                    return z;
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        z = "start recognition failed!";
        Slog.e(TAG, z);
        Binder.restoreCallingIdentity(ident);
        return false;
    }

    private boolean stopSoundTriggerDetector() {
        createManagers();
        createSoundTriggerDetector();
        GenericSoundModel model = getCurrentModel();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("stopSoundTriggerDetector model : ");
        stringBuilder.append(model);
        Slog.i(str, stringBuilder.toString());
        long ident = Binder.clearCallingIdentity();
        if (model != null) {
            try {
                if (this.mSoundTriggerDetector.stopRecognition()) {
                    Slog.i(TAG, "stop recognition successfully!");
                    return true;
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        Slog.e(TAG, "stop recognition failed!");
        Binder.restoreCallingIdentity(ident);
        return false;
    }

    private void createSoundTriggerHandler() {
        if (this.mSoundTriggerHandler == null) {
            this.mSoundTriggerHandler = new Handler(Looper.getMainLooper());
        }
    }

    private void createManagers() {
        if (this.mSoundTriggerManager == null) {
            this.mSoundTriggerManager = (SoundTriggerManager) this.mContext.getSystemService("soundtrigger");
            this.mUserManager = (UserManager) this.mContext.getSystemService("user");
            this.mSoundTriggerService = ISoundTriggerService.Stub.asInterface(ServiceManager.getService("soundtrigger"));
        }
    }

    private void createSoundTriggerDetector() {
        if (this.mSoundTriggerDetector == null) {
            createSoundTriggerHandler();
            this.mSoundTriggerDetector = this.mSoundTriggerManager.createSoundTriggerDetector(MODEL_UUID, this.mCallBack, this.mSoundTriggerHandler);
        }
    }

    private static boolean isSupportWakeUpV2() {
        String wakeupV2 = AudioSystem.getParameters("audio_capability#soundtrigger_version");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("wakeupV2 : ");
        stringBuilder.append(wakeupV2);
        Slog.i(str, stringBuilder.toString());
        if ("".equals(wakeupV2)) {
            return false;
        }
        return true;
    }

    private void startSoundTriggerV2(boolean start) {
        if (isSupportWakeUpV2()) {
            createManagers();
            GenericSoundModel model = getCurrentModel();
            boolean isUserUnlocked = this.mUserManager.isUserUnlocked(UserHandle.SYSTEM);
            boolean isSoundTriggerOn = isSoundTriggerOn();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startSoundTriggerV2 model : ");
            stringBuilder.append(model);
            stringBuilder.append(" isSoundTriggerOn : ");
            stringBuilder.append(isSoundTriggerOn);
            stringBuilder.append(" isUserUnlocked : ");
            stringBuilder.append(isUserUnlocked);
            stringBuilder.append(" start : ");
            stringBuilder.append(start);
            Slog.i(str, stringBuilder.toString());
            if (isUserUnlocked && model != null && isSoundTriggerOn) {
                if (start) {
                    startSoundTriggerDetector();
                } else {
                    stopSoundTriggerDetector();
                }
            }
            return;
        }
        Slog.i(TAG, "startSoundTriggerV2 not support wakeup v2");
    }

    private GenericSoundModel getCurrentModel() {
        try {
            if (this.mSoundTriggerService != null) {
                return this.mSoundTriggerService.getSoundModel(new ParcelUuid(MODEL_UUID));
            }
            Slog.e(TAG, "getCurrentModel mSoundTriggerService is null!");
            return null;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getCurrentModel e : ");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        }
    }

    private void startWakeupService(int session) {
        Intent intent = new Intent(ACTION_WAKEUP_SERVICE);
        intent.setPackage(VASSISTANT_PACKAGE_NAME);
        intent.putExtra(SESSION_ID, session);
        this.mContext.startService(intent);
    }

    public AudioRoutesInfo startWatchingRoutes(IAudioRoutesObserver observer) {
        Slog.d(TAG, "startWatchingRoutes");
        if (linkVAtoDeathRe(observer)) {
            return null;
        }
        return super.startWatchingRoutes(observer);
    }

    private boolean linkVAtoDeathRe(IAudioRoutesObserver observer) {
        Slog.d(TAG, "linkVAtoDeathRe for vassistant");
        int uid = Binder.getCallingUid();
        if (this.mContext == null) {
            return false;
        }
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null) {
            return false;
        }
        String[] packages = pm.getPackagesForUid(uid);
        if (packages == null) {
            return false;
        }
        int i = 0;
        while (i < packages.length) {
            String packageName = packages[i];
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("packageName:");
            stringBuilder.append(packageName);
            Slog.d(str, stringBuilder.toString());
            if (VASSISTANT_PACKAGE_NAME.equals(packageName)) {
                this.mBinder = observer.asBinder();
                try {
                    this.mBinder.linkToDeath(this.mDeathRecipient, 0);
                    this.mIsLinkDeathRecipient = true;
                    return true;
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                i++;
            }
        }
        return false;
    }

    public boolean isFileReady(String filename) {
        return new File(filename).canRead();
    }

    public void unloadHwThemeSoundEffects() {
        this.mHwThemeHandler.removeMessages(99);
    }

    public int getSampleId(SoundPool soundpool, int effect, String defFilePath, int index) {
        int sampleId = 0;
        if (effect == 0) {
            String themeFilePath = this.mSysKeyEffectFile;
            if (themeFilePath == null) {
                themeFilePath = defFilePath;
            }
            if (themeFilePath == null) {
                return -1;
            }
            if (isFileReady(themeFilePath)) {
                sampleId = soundpool.load(themeFilePath, index);
                if (sampleId > 0) {
                    Systemex.putString(this.mContentResolver, SYSKEY_SOUND_HWT, themeFilePath);
                }
            }
        } else {
            sampleId = soundpool.load(defFilePath, index);
        }
        return sampleId;
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code != 1003) {
            int _arg0 = 0;
            String str;
            StringBuilder stringBuilder;
            switch (code) {
                case 101:
                    int event = data.readInt();
                    if (DEBUG) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("HwAudioService.onTransact: got event ");
                        stringBuilder.append(event);
                        Slog.v(str, stringBuilder.toString());
                    }
                    if (event == 1) {
                        _arg0 = 1;
                    }
                    sendMsg(this.mAudioHandler, 8, 2, 1, _arg0, null, 0);
                    if (DEBUG) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("setSpeakermediaOn ");
                        stringBuilder.append(_arg0);
                        Slog.v(str, stringBuilder.toString());
                    }
                    reply.writeNoException();
                    return true;
                case 102:
                    reply.writeNoException();
                    return true;
                default:
                    switch (code) {
                        case 1005:
                            data.enforceInterface("android.media.IAudioService");
                            sendMsg(this.mAudioHandler, MSG_SHOW_DISABLE_MICROPHONE_TOAST, 2, 0, 0, null, 20);
                            reply.writeNoException();
                            return true;
                        case 1006:
                            data.enforceInterface("android.media.IAudioService");
                            sendMsg(this.mAudioHandler, 91, 2, 0, 0, null, 0);
                            reply.writeNoException();
                            return true;
                        default:
                            String str2;
                            StringBuilder stringBuilder2;
                            switch (code) {
                                case 1101:
                                    _arg0 = isAdjustVolumeEnable();
                                    str2 = TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("isAdjustVolumeEnable transaction called. result:");
                                    stringBuilder2.append(_arg0);
                                    Slog.i(str2, stringBuilder2.toString());
                                    reply.writeNoException();
                                    reply.writeInt(_arg0);
                                    return true;
                                case 1102:
                                    boolean _arg02;
                                    IBinder callerToken = data.readStrongBinder();
                                    if (data.readInt() != 0) {
                                        _arg02 = true;
                                    }
                                    str = TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("enableVolumeAdjust  transaction called.enable:");
                                    stringBuilder.append(_arg02);
                                    Slog.i(str, stringBuilder.toString());
                                    enableVolumeAdjust(_arg02, Binder.getCallingUid(), callerToken);
                                    reply.writeNoException();
                                    return true;
                                case 1103:
                                    boolean _result;
                                    data.enforceInterface("android.media.IAudioService");
                                    if (data.readInt() != 0) {
                                        _result = startSoundTriggerDetector();
                                    } else {
                                        _result = stopSoundTriggerDetector();
                                    }
                                    reply.writeNoException();
                                    reply.writeInt(_result ? 1 : 0);
                                    if (this.mBinder != null && this.mIsLinkDeathRecipient) {
                                        this.mBinder.unlinkToDeath(this.mDeathRecipient, 0);
                                        this.mIsLinkDeathRecipient = false;
                                    }
                                    return true;
                                case 1104:
                                    _arg0 = data.readInt();
                                    str2 = TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("desktopModeChanged transaction mode:");
                                    stringBuilder2.append(_arg0);
                                    Slog.i(str2, stringBuilder2.toString());
                                    desktopModeChanged(_arg0);
                                    reply.writeNoException();
                                    return true;
                                case 1105:
                                    data.enforceInterface("android.media.IAudioService");
                                    reply.writeNoException();
                                    reply.writeBoolean(isScoAvailableOffCall());
                                    return true;
                                case HwArbitrationDEFS.MSG_STREAMING_VIDEO_BAD /*1106*/:
                                    data.enforceInterface("android.media.IAudioService");
                                    reply.writeNoException();
                                    reply.writeBoolean(isHwKaraokeEffectEnable());
                                    return true;
                                default:
                                    return super.onTransact(code, data, reply, flags);
                            }
                    }
            }
        }
        data.enforceInterface("android.media.IAudioService");
        String _arg03 = data.readString();
        recordLastRecordTime(data.readInt(), data.readInt());
        reply.writeNoException();
        return true;
    }

    public boolean isHwKaraokeEffectEnable() {
        boolean enable = this.mHwAudioServiceEx.isHwKaraokeEffectEnable(getPackageNameByPid(Binder.getCallingPid()));
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwKaraoke Effect enable = ");
        stringBuilder.append(enable);
        Slog.i(str, stringBuilder.toString());
        return enable;
    }

    public boolean isScoAvailableOffCall() {
        if (this.mBluetoothHeadset != null) {
            return this.mBluetoothHeadset.isScoAvailableOffCall();
        }
        return true;
    }

    public static void printCtaifsLog(String applicationName, String packageName, String callingMethod, String desciption) {
        String str = TAG_CTAIFS;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<");
        stringBuilder.append(applicationName);
        stringBuilder.append(">[");
        stringBuilder.append(applicationName);
        stringBuilder.append("][");
        stringBuilder.append(packageName);
        stringBuilder.append("][");
        stringBuilder.append(callingMethod);
        stringBuilder.append("] ");
        stringBuilder.append(desciption);
        Slog.i(str, stringBuilder.toString());
    }

    public static String getRecordAppName(Context context, String packageName) {
        if (context == null) {
            return null;
        }
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return null;
        }
        ApplicationInfo appInfo = null;
        long ident = Binder.clearCallingIdentity();
        try {
            appInfo = pm.getApplicationInfoAsUser(packageName, 0, getCurrentUserId());
        } catch (NameNotFoundException e) {
            Slog.e(TAG, "App Name Not Found");
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
        Binder.restoreCallingIdentity(ident);
        if (appInfo == null) {
            return null;
        }
        CharSequence displayName = pm.getApplicationLabel(appInfo);
        if (TextUtils.isEmpty(displayName)) {
            return null;
        }
        return String.valueOf(displayName);
    }

    protected void appendExtraInfo(Intent intent) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("appendExtraInfo mHeadsetSwitchState: ");
            stringBuilder.append(this.mHeadsetSwitchState);
            Slog.d(str, stringBuilder.toString());
        }
        if (intent != null) {
            intent.putExtra("switch_state", this.mHeadsetSwitchState);
        }
    }

    protected void sendDeviceConnectionIntentForImcs(int device, int state, String name) {
        Intent intent = new Intent();
        intent.putExtra("state", state);
        intent.putExtra("name", name);
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendDeviceConnectionIntentForImcs mHeadsetSwitchState: ");
            stringBuilder.append(this.mHeadsetSwitchState);
            Slog.d(str, stringBuilder.toString());
        }
        intent.putExtra("switch_state", this.mHeadsetSwitchState);
        if (device == 4) {
            intent.setAction("imcs.action.HEADSET_PLUG");
            intent.putExtra("microphone", 1);
        } else if (device == 8) {
            intent.setAction("imcs.action.HEADSET_PLUG");
            intent.putExtra("microphone", 0);
        } else {
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            ActivityManagerNative.broadcastStickyIntent(intent, "imcs.permission.HEADSET_PLUG", -1);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private boolean isAdjustVolumeEnable() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isAdjustVolumeEnable,mEnableAdjustVolume:");
        stringBuilder.append(this.mEnableAdjustVolume);
        Slog.i(str, stringBuilder.toString());
        return this.mEnableAdjustVolume;
    }

    private void enableVolumeAdjust(boolean enable, int callerUid, IBinder cb) {
        if (hasSystemPriv(callerUid)) {
            synchronized (this.mEnableVolumeLock) {
                String callerPkg = getPackageNameByPid(Binder.getCallingPid());
                if (this.mEnableVolumeClient == null) {
                    this.mEnableVolumeClient = new EnableVolumeClient(cb);
                    this.mEnableAdjustVolume = enable;
                } else {
                    String clientPkg = this.mEnableVolumeClient.getCallerPkg();
                    if (callerPkg.equals(clientPkg)) {
                        this.mEnableAdjustVolume = enable;
                    } else {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Just allowed one caller use the interface until older caller die.older. caller:");
                        stringBuilder.append(callerPkg);
                        stringBuilder.append(" old callser:");
                        stringBuilder.append(clientPkg);
                        Slog.w(str, stringBuilder.toString());
                    }
                }
            }
            return;
        }
        Slog.i(TAG, "caller is not system app.Can not set volumeAdjust to enable or disable.");
    }

    protected void readPersistedSettingsEx(ContentResolver cr) {
        if (HW_SOUND_TRIGGER_SUPPORT) {
            getSoundTriggerSettings(cr);
        }
        if (SPK_RCV_STEREO_SUPPORT) {
            getSpkRcvStereoState(cr);
        }
    }

    protected void onErrorCallBackEx(int exceptionId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("AudioException onErrorCallBackEx exceptionId:");
        stringBuilder.append(exceptionId);
        Slog.i(str, stringBuilder.toString());
        if (exceptionId <= -1000) {
            sendMsg(this.mAudioHandler, 10000, 1, exceptionId, 0, null, 0);
        }
    }

    protected void onScoExceptionOccur(int clientPid) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("AudioException ScoExceptionOccur,clientpid:");
        stringBuilder.append(clientPid);
        stringBuilder.append(" have more than one sco connected!");
        Slog.w(str, stringBuilder.toString());
        str = getPackageNameByPid(clientPid);
        sendMsg(this.mAudioHandler, 10000, 1, HW_SCO_CONNECT_EXCEPTION_OCCOUR, 0, new AudioExceptionMsg(HW_SCO_CONNECT_EXCEPTION_OCCOUR, str, getVersionName(str)), 0);
    }

    public void setMicrophoneMute(boolean on, String callingPackage, int userId) {
        boolean firstState = AudioSystem.isMicrophoneMuted();
        super.setMicrophoneMute(on, callingPackage, userId);
        if (!firstState && AudioSystem.isMicrophoneMuted()) {
            this.mAudioExceptionRecord.updateMuteMsg(callingPackage, getVersionName(callingPackage));
        }
    }

    protected void handleMessageEx(Message msg) {
        int i = msg.what;
        if (i == MSG_DUALPA_RCV_DEALY) {
            AudioSystem.setParameters(DUALPA_RCV_DEALY_OFF);
        } else if (i != 80) {
            switch (i) {
                case MSG_SHOW_DISABLE_MICROPHONE_TOAST /*90*/:
                    showDisableMicrophoneToast();
                    break;
                case 91:
                    disableHeadPhone();
                    break;
                case 92:
                    showDisableHeadphoneToast();
                    break;
                default:
                    switch (i) {
                        case 10000:
                            onAudioException(msg.arg1, msg.obj != null ? (AudioExceptionMsg) msg.obj : null);
                            break;
                        case IDisplayEngineService.DE_ACTION_PG_BROWSER_FRONT /*10001*/:
                            if (LOUD_VOICE_MODE_SUPPORT) {
                                handleLVMModeChangeProcess(msg.arg1, msg.obj);
                                break;
                            }
                            break;
                    }
                    break;
            }
        } else if (msg.obj == null) {
            Slog.e(TAG, "MSG_RINGER_MODE_CHANGE msg obj is null!");
        } else {
            AudioExceptionMsg tempMsg = msg.obj;
            String caller = tempMsg.getMsgPackagename();
            if (caller == null) {
                caller = "";
            }
            int fromRingerMode = msg.arg1;
            int ringerMode = msg.arg2;
            if (isFirstTimeShow(caller)) {
                Slog.i(TAG, "AudioException showRingerModeDialog");
                showRingerModeDialog(caller, fromRingerMode, ringerMode);
                savePkgNameToDB(caller);
            } else {
                Slog.i(TAG, "AudioException showRingerModeToast");
                showRingerModeToast(caller, fromRingerMode, ringerMode);
            }
            onAudioException(HW_RING_MODE_TYPE_EXCEPTION_OCCOUR, tempMsg);
        }
    }

    public void onAudioException(int exceptionId, AudioExceptionMsg exceptionMsg) {
        if (!this.mSystemReady) {
            Slog.e(TAG, "AudioException,but system is not ready! ");
        }
        if (exceptionId != HW_AUDIO_MODE_TYPE_EXCEPTION_OCCOUR) {
            switch (exceptionId) {
                case HW_SCO_CONNECT_EXCEPTION_OCCOUR /*-2003*/:
                    Slog.w(TAG, "AudioException HW_SCO_CONNECT_EXCEPTION_OCCOUR");
                    return;
                case HW_MIC_MUTE_EXCEPTION_OCCOUR /*-2002*/:
                    if (!this.mUserManagerInternal.getUserRestriction(getCurrentUserId(), "no_unmute_microphone")) {
                        Slog.w(TAG, "AudioException HW_MIC_MUTE_EXCEPTION_OCCOUR");
                        if (EXCLUSIVE_PRODUCT_NAME.equals(CURRENT_PRODUCT_NAME)) {
                            Slog.w(TAG, "Not allowded to recovery according to the us operator's rules");
                            return;
                        } else {
                            AudioSystem.muteMicrophone(false);
                            return;
                        }
                    }
                    return;
                case HW_RING_MODE_TYPE_EXCEPTION_OCCOUR /*-2001*/:
                    Slog.w(TAG, "AudioException HW_RING_MODE_TYPE_EXCEPTION_OCCOUR");
                    return;
                default:
                    int tmp = -exceptionId;
                    if ((-(tmp >> 21)) == HW_AUDIO_TRACK_OVERFLOW_TYPE_EXCEPTION_OCCOUR) {
                        int exceptionPid = tmp - 2101346304;
                        if (exceptionPid > 0) {
                            String packageName = getPackageNameByPid(exceptionPid);
                            String packageVersion = getVersionName(packageName);
                            if (packageName != null) {
                                String str = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("AudioTrack_Overflow packageName = ");
                                stringBuilder.append(packageName);
                                stringBuilder.append(" ");
                                stringBuilder.append(packageVersion);
                                stringBuilder.append(" pid = ");
                                stringBuilder.append(exceptionPid);
                                Slog.e(str, stringBuilder.toString());
                                HwMediaMonitorManager.writeLogMsg(916010207, 3, HW_AUDIO_TRACK_OVERFLOW_TYPE_EXCEPTION_OCCOUR, "OAE5");
                                return;
                            }
                            Slog.w(TAG, "AudioTrack_Overflow getPackageNameByPid failed");
                            return;
                        }
                        Slog.w(TAG, "AudioTrack_Overflow pid error");
                        return;
                    }
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("No such AudioException exceptionId:");
                    stringBuilder2.append(exceptionId);
                    Slog.w(str2, stringBuilder2.toString());
                    return;
            }
        }
        Slog.w(TAG, "AudioException HW_AUDIO_MODE_TYPE_EXCEPTION_OCCOUR, setModeInt AudioSystem.MODE_NORMAL");
    }

    protected void handleLVMModeChangeProcess(int state, Object object) {
        if (state == 1) {
            if (object != null) {
                DeviceVolumeState myObj = (DeviceVolumeState) object;
                setLVMMode(myObj.mDirection, myObj.mDevice, myObj.mOldIndex, myObj.mstreamType);
            }
        } else if (isInCall()) {
            int device = getInCallDevice();
            setLVMMode(0, device, -1, 0);
            setVoiceALGODeviceChange(device);
        }
    }

    private boolean isInCall() {
        if (getMode() != 2) {
            return false;
        }
        TelecomManager telecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
        if (telecomManager == null) {
            return false;
        }
        return telecomManager.isInCall();
    }

    protected int getOldInCallDevice(int mode) {
        if (mode == 2) {
            this.oldInCallDevice = getInCallDevice();
        } else if (mode == 0) {
            this.oldInCallDevice = 0;
        }
        return this.oldInCallDevice;
    }

    private int getInCallDevice() {
        int activeStreamType;
        if (AudioSystem.getForceUse(0) == 3) {
            activeStreamType = 6;
        } else {
            activeStreamType = 0;
        }
        return hwGetDeviceForStream(activeStreamType);
    }

    private void setVoiceALGODeviceChange(int device) {
        if (this.oldInCallDevice != device) {
            AudioSystem.setParameters("VOICE_ALGO_DeviceChange=true");
            this.oldInCallDevice = device;
        }
    }

    private void force_exitLVMMode() {
        if ("true".equals(AudioSystem.getParameters("VOICE_LVM_Enable"))) {
            AudioSystem.setParameters("VOICE_LVM_Enable=false");
            this.oldLVMDevice = 0;
            if (DEBUG) {
                Slog.i(TAG, "force disable LVM");
            }
        }
    }

    protected void setLVMMode(int direction, int device, int oldIndex, int streamType) {
        if (getMode() == 2 && this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") != 0) {
            Slog.e(TAG, "MODIFY_PHONE_STATE Permission Denial: setLVMMode");
        } else if (LOUD_VOICE_MODE_SUPPORT && isInCall() && streamType == 0) {
            boolean isLVMMode = "true".equals(AudioSystem.getParameters("VOICE_LVM_Enable"));
            boolean isLVMDevice = 2 == device || 1 == device;
            boolean isMaxVolume = getStreamMaxIndex(0) == oldIndex;
            boolean isChangeVolume = false;
            if (isMaxVolume && isLVMDevice && this.oldLVMDevice == device) {
                isChangeVolume = getStreamIndex(0, device) < oldIndex;
            }
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("direction:");
                stringBuilder.append(direction);
                stringBuilder.append(" device:");
                stringBuilder.append(device);
                stringBuilder.append(" oldIndex:");
                stringBuilder.append(oldIndex);
                stringBuilder.append(" isLVMMode:");
                stringBuilder.append(isLVMMode);
                stringBuilder.append(" isChangeVolume:");
                stringBuilder.append(isChangeVolume);
                Slog.i(str, stringBuilder.toString());
            }
            String str2;
            StringBuilder stringBuilder2;
            if (!isLVMMode && isMaxVolume && 1 == direction && isLVMDevice) {
                AudioSystem.setParameters("VOICE_LVM_Enable=true");
                if (device == 1) {
                    HwMediaMonitorManager.writeBigData(916219001, 4);
                } else if (device == 2) {
                    HwMediaMonitorManager.writeBigData(916219002, 4);
                }
                showLVMToast(33685776);
                this.oldLVMDevice = device;
                if (DEBUG) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("enable LVM after  = ");
                    stringBuilder2.append(this.oldLVMDevice);
                    Slog.i(str2, stringBuilder2.toString());
                }
            } else if (isLVMMode && ((-1 == direction || device != this.oldLVMDevice || isChangeVolume) && this.oldLVMDevice != 0)) {
                AudioSystem.setParameters("VOICE_LVM_Enable=false");
                showLVMToast(33685777);
                this.oldLVMDevice = 0;
                if (DEBUG) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("disable LVM after oldLVMDevice= ");
                    stringBuilder2.append(this.oldLVMDevice);
                    Slog.i(str2, stringBuilder2.toString());
                }
            }
        } else {
            Slog.e(TAG, "setLVMMode abort");
        }
    }

    private void showLVMToast(int message) {
        if (this.mLVMToast == null) {
            this.mLVMToast = Toast.makeText(new ContextThemeWrapper(this.mContext, this.mContext.getResources().getIdentifier("androidhwext:style/Theme.Emui.Toast", null, null)), "Unknown State", 0);
            LayoutParams windowParams = this.mLVMToast.getWindowParams();
            windowParams.privateFlags |= 16;
        }
        try {
            if (DEBUG) {
                Slog.i(TAG, "showLVMToast ");
            }
            this.mLVMToast.setText(message);
            this.mLVMToast.show();
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("showLVMToast exception: ");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        }
    }

    protected void getSoundTriggerSettings(ContentResolver cr) {
        String KEY_SOUNDTRIGGER_TYPE = "hw_soundtrigger_type";
        this.mSoundTriggerStatus = Secure.getInt(cr, KEY_SOUNDTRIGGER_SWITCH, 0);
        this.mSoundTriggerRes = Secure.getString(cr, "hw_soundtrigger_resource");
        this.mSoundTriggerGram = Secure.getString(cr, "hw_soundtrigger_grammar");
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mSoundTriggerStatus = ");
            stringBuilder.append(this.mSoundTriggerStatus);
            stringBuilder.append(" mSoundTriggerRes = ");
            stringBuilder.append(this.mSoundTriggerRes);
            stringBuilder.append(" mSoundTriggerGram = ");
            stringBuilder.append(this.mSoundTriggerGram);
            Slog.i(str, stringBuilder.toString());
        }
    }

    private void setSoundTrigger() {
        if (this.mSoundTriggerStatus == 1) {
            AudioSystem.setParameters(SOUNDTRIGGER_MAD_ON);
            AudioSystem.setParameters(SOUNDTRIGGER_WAKUP_ON);
            Slog.i(TAG, "setSoundTrigger = on");
        } else if (this.mSoundTriggerStatus == 0) {
            AudioSystem.setParameters(SOUNDTRIGGER_MAD_OFF);
            AudioSystem.setParameters(SOUNDTRIGGER_WAKUP_OFF);
            Slog.i(TAG, "setSoundTrigger = off");
            return;
        }
        if (this.mSoundTriggerRes != null && !this.mSoundTriggerRes.isEmpty() && this.mSoundTriggerGram != null && !this.mSoundTriggerGram.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mad=tslice_model=");
            stringBuilder.append(this.mSoundTriggerRes);
            AudioSystem.setParameters(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("mad=tslice_gram=");
            stringBuilder.append(this.mSoundTriggerGram);
            AudioSystem.setParameters(stringBuilder.toString());
        }
    }

    private boolean isInCallingState() {
        TelephonyManager tmgr = null;
        if (this.mContext != null) {
            tmgr = (TelephonyManager) this.mContext.getSystemService("phone");
        }
        if (tmgr == null) {
            return false;
        }
        if (tmgr.getCallState() != 0) {
            if (DEBUG) {
                Slog.v(TAG, "phone is in call state");
            }
            return true;
        }
        if (DEBUG) {
            Slog.v(TAG, "phone is NOT in call state");
        }
        return false;
    }

    private boolean hasSystemPriv(int uid) {
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null) {
            return false;
        }
        String[] pkgs = pm.getPackagesForUid(uid);
        if (pkgs == null) {
            return false;
        }
        for (String pkg : pkgs) {
            String str;
            try {
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                if (!(info == null || (info.flags & 1) == 0)) {
                    str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("system app ");
                    stringBuilder.append(pkg);
                    Slog.i(str, stringBuilder.toString());
                    return true;
                }
            } catch (Exception e) {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("not found app ");
                stringBuilder2.append(pkg);
                Slog.i(str, stringBuilder2.toString());
            }
        }
        return false;
    }

    protected boolean checkAudioSettingAllowed(String msg) {
        StringBuilder stringBuilder;
        if (isInCallingState()) {
            int uid = Binder.getCallingUid();
            if (1001 == uid || 1000 == uid || hasSystemPriv(uid)) {
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Audio Settings ALLOW from func=");
                    stringBuilder2.append(msg);
                    stringBuilder2.append(", from pid=");
                    stringBuilder2.append(Binder.getCallingPid());
                    stringBuilder2.append(", uid=");
                    stringBuilder2.append(Binder.getCallingUid());
                    Slog.v(str, stringBuilder2.toString());
                }
                return true;
            }
            if (DEBUG) {
                String str2 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Audio Settings NOT allow from func=");
                stringBuilder.append(msg);
                stringBuilder.append(", from pid=");
                stringBuilder.append(Binder.getCallingPid());
                stringBuilder.append(", uid=");
                stringBuilder.append(Binder.getCallingUid());
                Slog.v(str2, stringBuilder.toString());
            }
            return false;
        }
        String str3 = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Audio Settings ALLOW from func=");
        stringBuilder.append(msg);
        stringBuilder.append(", from pid=");
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append(", uid=");
        stringBuilder.append(Binder.getCallingUid());
        Slog.w(str3, stringBuilder.toString());
        return true;
    }

    protected String getPackageNameByPid(int pid) {
        if (pid <= 0) {
            return null;
        }
        ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService("activity");
        if (activityManager == null) {
            return null;
        }
        List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return null;
        }
        String packageName = null;
        for (RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.pid == pid) {
                packageName = appProcess.processName;
                break;
            }
        }
        int indexProcessFlag = -1;
        if (packageName != null) {
            indexProcessFlag = packageName.indexOf(58);
        }
        return indexProcessFlag > 0 ? packageName.substring(0, indexProcessFlag) : packageName;
    }

    private String getVersionName(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return null;
        }
        String versionName;
        try {
            versionName = this.mContext.getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (NameNotFoundException e) {
            versionName = "version isn't exist";
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getVersionName failed");
            stringBuilder.append(packageName);
            Slog.e(str, stringBuilder.toString());
        }
        return versionName;
    }

    public void checkMicMute() {
        Slog.i(TAG, "AudioException record is not occpied, check mic mute.");
        String recordPkg = getPackageNameByPid(Binder.getCallingPid());
        printCtaifsLog(getRecordAppName(this.mContext, recordPkg), recordPkg, "onTransact", "本地录音");
        if (AudioSystem.isMicrophoneMuted() && getMode() != 2) {
            Slog.i(TAG, "AudioException mic is muted when record! set unmute!");
            sendMsg(this.mAudioHandler, 10000, 1, HW_MIC_MUTE_EXCEPTION_OCCOUR, 0, new AudioExceptionMsg(HW_MIC_MUTE_EXCEPTION_OCCOUR, this.mAudioExceptionRecord.getMutePackageName(), this.mAudioExceptionRecord.getMutePPackageVersion()), 500);
        }
    }

    private String getApplicationLabel(String pkgName) {
        try {
            PackageManager packageManager = this.mContext.getPackageManager();
            return packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkgName, null)).toString();
        } catch (NameNotFoundException e) {
            Slog.e(TAG, "getApplicationLabel exception", e);
            return null;
        }
    }

    protected boolean checkEnbaleVolumeAdjust() {
        return isAdjustVolumeEnable();
    }

    protected void sendCommForceBroadcast() {
        if (getMode() == 2) {
            int uid = Binder.getCallingUid();
            if (1000 == uid || "com.android.bluetooth".equals(getPackageNameByPid(Binder.getCallingPid()))) {
                try {
                    this.mContext.sendBroadcastAsUser(new Intent(ACTION_COMMFORCE), UserHandle.getUserHandleForUid(UserHandle.getCallingUserId()), PERMISSION_COMMFORCE);
                } catch (Exception e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("sendCommForceBroadcast exception: ");
                    stringBuilder.append(e);
                    Slog.e(str, stringBuilder.toString());
                }
                return;
            }
            if (DEBUG) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Ignore sendCommForceBroadcast from uid = ");
                stringBuilder2.append(uid);
                Slog.v(str2, stringBuilder2.toString());
            }
        }
    }

    protected void checkMuteRcvDelay(int curMode, int mode) {
        if (mode == 0 && (curMode == 2 || curMode == 3)) {
            AudioSystem.setParameters(DUALPA_RCV_DEALY_ON);
            sendMsg(this.mAudioHandler, MSG_DUALPA_RCV_DEALY, 0, 0, 0, null, 5000);
        } else if (curMode != mode && (mode == 1 || mode == 2 || mode == 3)) {
            AudioSystem.setParameters(DUALPA_RCV_DEALY_OFF);
        } else if (DEBUG) {
            Slog.i(TAG, "checkMuteRcvDelay Do Nothing");
        }
    }

    private void setAudioSystemParameters() {
        if (HW_SOUND_TRIGGER_SUPPORT) {
            setSoundTrigger();
        }
        if (SPK_RCV_STEREO_SUPPORT) {
            setSpkRcvStereoStatus();
        }
        this.mSetVbrlibFlag = false;
    }

    void reportAudioFlingerRestarted() {
        LogIAware.report(2101, "AudioFlinger");
    }

    protected void processMediaServerRestart() {
        readPersistedSettingsEx(this.mContentResolver);
        setAudioSystemParameters();
        Slog.i(TAG, "mediaserver restart ,start soundtrigger!");
        createAndStartSoundTrigger(true);
        reportAudioFlingerRestarted();
        if (LOW_LATENCY_SUPPORT) {
            initLowlatencyUidsMap();
        }
    }

    private void getSpkRcvStereoState(ContentResolver contentResolver) {
        this.mSpkRcvStereoStatus = Secure.getInt(contentResolver, "stereo_landscape_portrait", 1);
    }

    private void setSpkRcvStereoStatus() {
        if (this.mSpkRcvStereoStatus == 1) {
            AudioSystem.setParameters(SPK_RCV_STEREO_ON_PARA);
            AudioSystem.setParameters("rotation=0");
        } else if (this.mSpkRcvStereoStatus == 0) {
            AudioSystem.setParameters(SPK_RCV_STEREO_OFF_PARA);
        } else {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setSpkRcvStereoStatus Fail ");
            stringBuilder.append(this.mSpkRcvStereoStatus);
            Slog.e(str, stringBuilder.toString());
        }
    }

    public int getRingerModeExternal() {
        int ringerMode = super.getRingerModeExternal();
        if (!(ringerMode == 0 || this.mNm == null || this.mNm.getZenMode() == 0)) {
            String pkgName = getPackageNameByPid(Binder.getCallingPid());
            if (!(pkgName == null || this.mZenModeWhiteList == null)) {
                for (String temp : this.mZenModeWhiteList) {
                    if (pkgName.equals(temp)) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Return ringer mode silent for ");
                        stringBuilder.append(pkgName);
                        stringBuilder.append(" under zen mode");
                        Slog.i(str, stringBuilder.toString());
                        return 0;
                    }
                }
            }
        }
        return ringerMode;
    }

    public void setRingerModeExternal(int toRingerMode, String caller) {
        boolean shouldControll = isSystemApp(caller) ^ 1;
        if (shouldControll) {
            boolean shouldReport = checkShouldAbortRingerModeChange(caller);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AudioException setRingerModeExternal shouldReport=");
            stringBuilder.append(shouldReport);
            stringBuilder.append(" uid:");
            stringBuilder.append(Binder.getCallingUid());
            stringBuilder.append(" caller:");
            stringBuilder.append(caller);
            Slog.i(str, stringBuilder.toString());
        }
        int fromRingerMode = getRingerModeExternal();
        super.setRingerModeExternal(toRingerMode, caller);
        if (fromRingerMode == getRingerModeInternal()) {
            Slog.d(TAG, "AudioException setRingerModeExternal ,but not change");
            return;
        }
        if (shouldControll) {
            alertUserRingerModeChange(caller, fromRingerMode, toRingerMode);
        }
    }

    private void alertUserRingerModeChange(String caller, int fromRingerMode, int ringerMode) {
        sendMsg(this.mAudioHandler, 80, 2, fromRingerMode, ringerMode, new AudioExceptionMsg(HW_RING_MODE_TYPE_EXCEPTION_OCCOUR, caller, getVersionName(caller)), 0);
    }

    private boolean isFirstTimeShow(String caller) {
        String pkgs = Secure.getStringForUser(this.mContentResolver, "change_ringer_mode_pkgs", -2);
        if (TextUtils.isEmpty(pkgs)) {
            return true;
        }
        return pkgs.contains(caller) ^ true;
    }

    private void savePkgNameToDB(String caller) {
        if (!TextUtils.isEmpty(caller)) {
            String pkgs = Secure.getStringForUser(this.mContentResolver, "change_ringer_mode_pkgs", -2);
            StringBuilder stringBuilder;
            if (TextUtils.isEmpty(pkgs)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(caller);
                stringBuilder.append("\\");
                pkgs = stringBuilder.toString();
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(pkgs);
                stringBuilder.append(caller);
                stringBuilder.append("\\");
                pkgs = stringBuilder.toString();
            }
            Secure.putStringForUser(this.mContentResolver, "change_ringer_mode_pkgs", pkgs, -2);
        }
    }

    private boolean checkShouldAbortRingerModeChange(String caller) {
        return isScreenOff() || isKeyguardLocked() || !isCallerShowing(caller);
    }

    private boolean isKeyguardLocked() {
        KeyguardManager keyguard = (KeyguardManager) this.mContext.getSystemService("keyguard");
        if (keyguard != null) {
            return keyguard.isKeyguardLocked();
        }
        return false;
    }

    private boolean isCallerShowing(String caller) {
        String pkgName = getTopApp();
        if (pkgName != null) {
            return pkgName.startsWith(caller);
        }
        Slog.e(TAG, "AudioException getTopApp ,but pkgname is null.");
        return false;
    }

    private String getTopApp() {
        return ((ActivityManagerService) ServiceManager.getService("activity")).topAppName();
    }

    private boolean isScreenOff() {
        PowerManager power = (PowerManager) this.mContext.getSystemService("power");
        if (power != null) {
            return power.isScreenOn() ^ 1;
        }
        return false;
    }

    private boolean isSystemApp(String caller) {
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null) {
            return false;
        }
        try {
            ApplicationInfo info = pm.getApplicationInfo(caller, 0);
            if (info == null || (info.flags & 1) == 0) {
                return false;
            }
            return true;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AudioException not found app:");
            stringBuilder.append(caller);
            Slog.i(str, stringBuilder.toString());
        }
    }

    private void showRingerModeToast(String caller, int fromRingerMode, int toRingerMode) {
        int fromResId = getRingerModeStrResId(fromRingerMode);
        int toResId = getRingerModeStrResId(toRingerMode);
        if (fromResId <= 0 || toResId <= 0) {
            Slog.e(TAG, "AudioException showRingerModeToast resid not found ");
            return;
        }
        String callerAppName = getApplicationLabel(caller);
        if (this.mRingerModeToast == null) {
            this.mRingerModeToast = Toast.makeText(new ContextThemeWrapper(this.mContext, this.mContext.getResources().getIdentifier("androidhwext:style/Theme.Emui.Toast", null, null)), "Unknown State", 0);
        }
        try {
            String messageStr = this.mContext.getString(33685819, new Object[]{callerAppName, this.mContext.getString(fromResId), this.mContext.getString(toResId)});
            Slog.i(TAG, "AudioException showRingerModeToast ");
            this.mRingerModeToast.setText(messageStr);
            this.mRingerModeToast.show();
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AudioException showRingerModeToast exception: ");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        }
    }

    private void showRingerModeDialog(String caller, int fromRingerMode, int toRingerMode) {
        int fromResId = getRingerModeStrResId(fromRingerMode);
        int toResId = getRingerModeStrResId(toRingerMode);
        if (fromResId <= 0 || toResId <= 0) {
            Slog.e(TAG, "AudioException showRingerModeDialog resid not found ");
            return;
        }
        String callerAppName = getApplicationLabel(caller);
        String messageStr = this.mContext.getString(33685819, new Object[]{callerAppName, this.mContext.getString(fromResId), this.mContext.getString(toResId)});
        Builder builder = new Builder(this.mContext, this.mContext.getResources().getIdentifier("androidhwext:style/Theme.Emui.Dialog.Alert", null, null));
        builder.setMessage(messageStr);
        builder.setCancelable(false);
        builder.setPositiveButton(33685821, new OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        Dialog dialog = builder.create();
        dialog.getWindow().setType(2003);
        dialog.show();
        Slog.i(TAG, "AudioException showRingerModeDialog show ");
        this.isShowingDialog = true;
    }

    private int getRingerModeStrResId(int RingerMode) {
        switch (RingerMode) {
            case 0:
                return 33685823;
            case 1:
                return 33685820;
            case 2:
                return 33685822;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getRingerModeStrResId RingerMode is error.RingerMode =");
                stringBuilder.append(RingerMode);
                Slog.e(str, stringBuilder.toString());
                return 0;
        }
    }

    private static int getCurrentUserId() {
        long ident = Binder.clearCallingIdentity();
        int i;
        try {
            i = ActivityManagerNative.getDefault().getCurrentUser().id;
            return i;
        } catch (RemoteException e) {
            i = TAG;
            Slog.w(i, "Activity manager not running, nothing we can do assume user 0.");
            return 0;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private int getUidByPkg(String pkgName) {
        int uid = -1;
        if (pkgName == null) {
            return -1;
        }
        try {
            uid = this.mContext.getPackageManager().getPackageUidAsUser(pkgName, getCurrentUserId());
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("not found uid pkgName:");
            stringBuilder.append(pkgName);
            Slog.w(str, stringBuilder.toString());
        }
        return uid;
    }

    private void updateLowlatencyUidsMap(String pkgName, int packageCmdType) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateLowlatencyUidsMap ");
        stringBuilder.append(pkgName);
        stringBuilder.append(" packageCmdType ");
        stringBuilder.append(packageCmdType);
        Slog.i(str, stringBuilder.toString());
        switch (packageCmdType) {
            case 1:
                int uid = getUidByPkg(pkgName);
                if (uid != -1) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("AddLowlatencyPkg=");
                    stringBuilder.append(uid);
                    AudioSystem.setParameters(stringBuilder.toString());
                }
                this.mLowlatencyUidsMap.put(pkgName, Integer.valueOf(uid));
                return;
            case 2:
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("RemLowlatencyPkg=");
                stringBuilder2.append(this.mLowlatencyUidsMap.get(pkgName));
                AudioSystem.setParameters(stringBuilder2.toString());
                this.mLowlatencyUidsMap.remove(pkgName);
                return;
            default:
                return;
        }
    }

    private void initLowlatencyUidsMap() {
        this.mLowlatencyUidsMap.clear();
        InputStream in = null;
        XmlPullParser xmlParser = null;
        try {
            in = new FileInputStream(new File(LOW_LATENCY_WHITE_LIST).getPath());
            xmlParser = Xml.newPullParser();
            xmlParser.setInput(in, null);
        } catch (FileNotFoundException e) {
            Slog.e(TAG, "LOW_LATENCY_WHITE_LIST not exist");
        } catch (XmlPullParserException e2) {
            Slog.e(TAG, "LOW_LATENCY_WHITE_LIST can not parse");
        }
        if (xmlParser == null) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e3) {
                    Slog.e(TAG, "LOW_LATENCY_WHITE_LIST IO Close Fail");
                }
            }
            Slog.e(TAG, "LOW_LATENCY_WHITE_LIST is null");
            return;
        }
        while (true) {
            try {
                int next = xmlParser.next();
                int eventType = next;
                if (next == 1) {
                    break;
                } else if (eventType == 2 && xmlParser.getName().equals(NODE_WHITEAPP)) {
                    String packageName = xmlParser.getAttributeValue(null, "package");
                    if (!(packageName == null || packageName.length() == 0)) {
                        updateLowlatencyUidsMap(packageName, 1);
                    }
                }
            } catch (XmlPullParserException e4) {
                Slog.e(TAG, "XmlPullParserException");
            } catch (IOException e5) {
                Slog.e(TAG, "IOException");
            }
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e6) {
                Slog.e(TAG, "LOW_LATENCY_WHITE_LIST IO Close Fail");
            }
        }
    }

    private boolean isLowlatencyPkg(String pkgName) {
        if (this.mLowlatencyUidsMap.get(pkgName) == null || ((Integer) this.mLowlatencyUidsMap.get(pkgName)).intValue() == getUidByPkg(pkgName)) {
            return false;
        }
        return true;
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        super.dump(fd, pw, args);
        try {
            this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
            pw.print("HwAudioService:");
            pw.print("DEBUG =");
            pw.println(DEBUG);
            pw.print("mIsHuaweiSafeMediaConfig =");
            pw.println(mIsHuaweiSafeMediaConfig);
            pw.print("mSecurityVolumeIndex =");
            pw.println(mSecurityVolumeIndex);
            pw.print("LOUD_VOICE_MODE_SUPPORT=");
            pw.println(LOUD_VOICE_MODE_SUPPORT);
            pw.print("Lound Voice State:");
            pw.println(AudioSystem.getParameters("VOICE_LVM_Enable"));
            pw.print("HW_SOUND_TRIGGER_SUPPORT=");
            pw.println(HW_SOUND_TRIGGER_SUPPORT);
            pw.print("mSoundTriggerStatus=");
            pw.println(this.mSoundTriggerStatus);
            pw.print("mad=on:");
            pw.println(AudioSystem.getParameters(SOUNDTRIGGER_MAD_ON));
            pw.print("wakeup=on:");
            pw.println(AudioSystem.getParameters(SOUNDTRIGGER_WAKUP_ON));
            pw.print("HW_KARAOKE_EFFECT_ENABLED=");
            pw.println(HW_KARAOKE_EFFECT_ENABLED);
            pw.print("isCallForeground=");
            pw.println(isCallForeground);
            pw.print("DUAL_SMARTPA_SUPPORT=");
            pw.println(DUAL_SMARTPA_SUPPORT);
            pw.print("SPK_RCV_STEREO_SUPPORT=");
            pw.println(SPK_RCV_STEREO_SUPPORT);
            pw.print("mSpkRcvStereoStatus=");
            pw.println(this.mSpkRcvStereoStatus);
            pw.print("mLowlatencyUidsMap=");
            pw.println(this.mLowlatencyUidsMap);
            pw.print("mVoipOptimizeInGameMode =");
            pw.println(mVoipOptimizeInGameMode);
        } catch (SecurityException e) {
            Slog.w(TAG, "enforceCallingOrSelfPermission dump failed ");
        }
    }

    private void showDisableMicrophoneToast() {
        Toast toast = Toast.makeText(new ContextThemeWrapper(this.mContext, this.mContext.getResources().getIdentifier("androidhwext:style/Theme.Emui.Toast", null, null)), 33685911, 0);
        LayoutParams windowParams = toast.getWindowParams();
        windowParams.privateFlags |= 16;
        toast.show();
    }

    private void showDisableHeadphoneToast() {
        Toast toast = Toast.makeText(new ContextThemeWrapper(this.mContext, this.mContext.getResources().getIdentifier("androidhwext:style/Theme.Emui.Toast", null, null)), 33685913, 0);
        LayoutParams windowParams = toast.getWindowParams();
        windowParams.privateFlags |= 16;
        toast.show();
    }

    public void setWiredDeviceConnectionState(int type, int state, String address, String name, String caller) {
        if ((-1375700964 & type) == 0) {
            super.setWiredDeviceConnectionState(type, state, address, name, caller);
        } else if (HwDeviceManager.disallowOp(31)) {
            Slog.i(TAG, "disallow headphone by MDM");
            if (state == 1 && type > 0) {
                sendMsg(this.mAudioHandler, 92, 2, 0, 0, null, 0);
            }
        } else {
            this.mHwAudioServiceEx.updateTypeCNotify(type, state);
            synchronized (this.mConnectedDevices) {
                if (state == 1) {
                    this.mHeadphones.add(new HeadphoneInfo(type, address, name, caller));
                } else {
                    for (int i = 0; i < this.mHeadphones.size(); i++) {
                        if (((HeadphoneInfo) this.mHeadphones.get(i)).mDeviceType == type) {
                            this.mHeadphones.remove(i);
                            break;
                        }
                    }
                }
                super.setWiredDeviceConnectionState(type, state, address, name, caller);
            }
        }
    }

    private void disableHeadPhone() {
        synchronized (this.mConnectedDevices) {
            for (int i = 0; i < this.mHeadphones.size(); i++) {
                HeadphoneInfo info = (HeadphoneInfo) this.mHeadphones.get(i);
                super.setWiredDeviceConnectionState(info.mDeviceType, 0, info.mDeviceAddress, info.mDeviceName, info.mCaller);
            }
            this.mHeadphones.clear();
        }
    }

    /* JADX WARNING: Missing block: B:14:0x002f, code:
            if (r2 == null) goto L_0x0039;
     */
    /* JADX WARNING: Missing block: B:16:?, code:
            r0.notifyIncallModeChange(r2.getPid(), r1);
     */
    /* JADX WARNING: Missing block: B:17:0x0039, code:
            r0.notifyIncallModeChange(0, 0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void updateAftPolicy() {
        IHwAftPolicyService hwAft = HwAftPolicyManager.getService();
        if (hwAft != null) {
            try {
                int mode = getMode();
                synchronized (this.mSetModeDeathHandlers) {
                    if (this.mSetModeDeathHandlers.isEmpty()) {
                        Slog.i(TAG, "updateAftPolicy SetModeDeathHandlers is empty, trun off aft");
                        hwAft.notifyIncallModeChange(0, 0);
                        return;
                    }
                    SetModeDeathHandler hdlr = (SetModeDeathHandler) this.mSetModeDeathHandlers.get(0);
                }
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("binder call throw ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        }
    }

    private void desktopModeChanged(int desktopMode) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("desktopModeChanged desktopMode = ");
        stringBuilder.append(desktopMode);
        Slog.v(str, stringBuilder.toString());
        if (this.mDesktopMode != desktopMode) {
            this.mDesktopMode = desktopMode;
            str = PROP_DESKTOP_MODE;
            stringBuilder = new StringBuilder();
            stringBuilder.append("");
            stringBuilder.append(desktopMode);
            SystemProperties.set(str, stringBuilder.toString());
            MediaFocusControl mediaFocusControl = this.mMediaFocusControl;
            boolean z = true;
            if (this.mDesktopMode != 1) {
                z = false;
            }
            mediaFocusControl.desktopModeChanged(z);
        }
    }

    public int trackPlayer(PlayerIdCard pic) {
        if (this.mDesktopMode == 1) {
            pic.setPkgName(getPackageNameByPid(Binder.getCallingPid()));
        }
        return super.trackPlayer(pic);
    }

    public void playerEvent(int piid, int event) {
        super.playerEvent(piid, event);
        this.mHwAudioServiceEx.notifyHiResIcon(event);
        recordLastPlaybackTime(piid, event);
        recoverFromExceptionMode(event);
    }

    private void recoverFromExceptionMode(int event) {
        if (event == 2) {
            SetModeDeathHandler hdlr = setMode3Handler();
            if (hdlr != null) {
                List<AudioPlaybackConfiguration> list = this.mPlaybackMonitor.getActivePlaybackConfigurations(true);
                int i = 0;
                while (i < list.size()) {
                    AudioPlaybackConfiguration conf = (AudioPlaybackConfiguration) list.get(i);
                    if ((conf.getPlayerState() != 2 && conf.getPlayerState() != 3) || conf.getClientPid() != hdlr.getPid()) {
                        i++;
                    } else {
                        return;
                    }
                }
                i = -1;
                String recordPid = AudioSystem.getParameters("active_record_pid");
                if (recordPid != null) {
                    try {
                        i = Integer.parseInt(recordPid);
                    } catch (NumberFormatException e) {
                        NumberFormatException numberFormatException = e;
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("NumberFormatException: ");
                        stringBuilder.append(recordPid);
                        Slog.v(str, stringBuilder.toString());
                    }
                }
                long recoverTime = System.currentTimeMillis();
                boolean needRecoverForRecord = recoverTime - this.mLastStopAudioRecordTime > 5000;
                boolean needRecoverForPlay = recoverTime - this.mLastStopPlayBackTime > 5000;
                boolean isSetModeTimePassBy = recoverTime - this.mLastSetMode3Time > 5000;
                if (i != hdlr.getPid() && needRecoverForRecord && needRecoverForPlay && isSetModeTimePassBy) {
                    long token = Binder.clearCallingIdentity();
                    try {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("mLastStopAudioRecordTime: ");
                        stringBuilder2.append(this.mLastStopAudioRecordTime);
                        stringBuilder2.append(" mLastStopPlayBackTime: ");
                        stringBuilder2.append(this.mLastStopPlayBackTime);
                        stringBuilder2.append(" mLastSetMode3Time: ");
                        stringBuilder2.append(this.mLastSetMode3Time);
                        stringBuilder2.append(" pid: ");
                        stringBuilder2.append(hdlr.getPid());
                        stringBuilder2.append(" PkgName: ");
                        stringBuilder2.append(getPackageNameByPid(hdlr.getPid()));
                        Slog.v(str2, stringBuilder2.toString());
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("OAE6:");
                        stringBuilder3.append(hdlr.getPid());
                        stringBuilder3.append(" :");
                        stringBuilder3.append(getPackageNameByPid(hdlr.getPid()));
                        HwMediaMonitorManager.writeLogMsg(916010201, 3, HW_AUDIO_MODE_TYPE_EXCEPTION_OCCOUR, stringBuilder3.toString());
                    } finally {
                        Binder.restoreCallingIdentity(token);
                    }
                }
            }
        }
    }

    private SetModeDeathHandler setMode3Handler() {
        if (this.mSetModeDeathHandlers == null) {
            return null;
        }
        try {
            Iterator iter = this.mSetModeDeathHandlers.iterator();
            while (iter.hasNext()) {
                SetModeDeathHandler object = iter.next();
                if (object instanceof SetModeDeathHandler) {
                    SetModeDeathHandler hdlr = object;
                    if (hdlr != null && hdlr.getMode() == 3) {
                        return hdlr;
                    }
                }
            }
        } catch (ConcurrentModificationException e) {
            Slog.i(TAG, "Exist ConcurrentModificationException", e);
        } catch (Exception e2) {
            Slog.i(TAG, "Exception: ", e2);
        }
        return null;
    }

    private void recordLastRecordTime(int state, int pid) {
        if (state == 1) {
            SetModeDeathHandler hdlr = setMode3Handler();
            if (hdlr != null && hdlr.getPid() == pid) {
                this.mLastStopAudioRecordTime = System.currentTimeMillis();
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mLastStopAudioRecordTime: ");
                stringBuilder.append(this.mLastStopAudioRecordTime);
                Slog.i(str, stringBuilder.toString());
            }
        }
    }

    private void recordLastPlaybackTime(int piid, int event) {
        if (event == 4) {
            SetModeDeathHandler hdlr = setMode3Handler();
            if (hdlr != null) {
                List<AudioPlaybackConfiguration> list = this.mPlaybackMonitor.getActivePlaybackConfigurations(true);
                int playbackNumber = list.size();
                for (int i = 0; i < playbackNumber; i++) {
                    AudioPlaybackConfiguration conf = (AudioPlaybackConfiguration) list.get(i);
                    if (conf != null && conf.getPlayerState() == 4 && conf.getClientPid() == hdlr.getPid() && conf.getPlayerInterfaceId() == piid) {
                        this.mLastStopPlayBackTime = System.currentTimeMillis();
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("mLastStopPlayBackTime: ");
                        stringBuilder.append(this.mLastStopPlayBackTime);
                        Slog.i(str, stringBuilder.toString());
                        break;
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:19:0x0050, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setMode(int mode, IBinder cb, String callingPackage) {
        if (SUPPORT_GAME_MODE) {
            if (mode == 1 && this.IS_IN_GAMEMODE) {
                this.mHwAudioServiceEx.setDolbyEffect(0);
            } else if (mode == 0 && this.IS_IN_GAMEMODE) {
                this.mHwAudioServiceEx.setDolbyEffect(3);
            }
        }
        super.setMode(mode, cb, callingPackage);
        SetModeDeathHandler hdlr = setMode3Handler();
        if (hdlr != null && mode == 3 && hdlr.getBinder() == cb) {
            this.mLastSetMode3Time = System.currentTimeMillis();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mLastSetMode3Time: ");
            stringBuilder.append(this.mLastSetMode3Time);
            Slog.i(str, stringBuilder.toString());
        }
    }

    private void bindIAwareSdk() {
        IAwareSdk.rigsterForegroundAppTypeWithCallback(new ForeAppTypeListener());
        Slog.i(TAG, "Intent.ACTION_BOOT_COMPLETED rigsterForegroundAppTypeWithCallback ForeAppTypeListener");
        IBinder b = ServiceManager.getService("IAwareSdkService");
        if (b != null) {
            try {
                b.linkToDeath(new DeathRecipient() {
                    public void binderDied() {
                        HwAudioService.this.bindIAwareSdk();
                    }
                }, 0);
                return;
            } catch (RemoteException e) {
                Slog.e(TAG, "IAwareSdkService linkToDeath error");
                return;
            }
        }
        Slog.e(TAG, "IAwareSdkService getService is null");
    }
}
