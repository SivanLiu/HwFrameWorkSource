package com.android.server.audio;

import android.aft.HwAftPolicyManager;
import android.aft.IHwAftPolicyService;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.bluetooth.BluetoothDevice;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.soundtrigger.SoundTrigger;
import android.hdm.HwDeviceManager;
import android.media.AudioAttributes;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioRoutesInfo;
import android.media.AudioSystem;
import android.media.HwMediaMonitorManager;
import android.media.IAudioRoutesObserver;
import android.media.PlayerBase;
import android.media.audiopolicy.AudioVolumeGroup;
import android.media.soundtrigger.SoundTriggerDetector;
import android.media.soundtrigger.SoundTriggerManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.provider.Settings;
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
import android.widget.Toast;
import com.android.internal.app.ISoundTriggerService;
import com.android.internal.content.PackageMonitor;
import com.android.server.DeviceIdleController;
import com.android.server.HwServiceFactory;
import com.android.server.LocalServices;
import com.android.server.audio.AbsAudioService;
import com.android.server.audio.AudioService;
import com.android.server.audio.report.AudioExceptionMsg;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.pm.UserManagerService;
import com.android.server.policy.HwPhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.rms.iaware.feature.SceneRecogFeature;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.wifipro.WifiProCommonUtils;
import com.huawei.android.app.ActivityManagerEx;
import com.huawei.android.app.IGameObserver;
import com.huawei.sidetouch.IHwSideStatusManager;
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
    private static final String AIACTION_WAKEUP_INFO = "com.huawei.ai.wakeup.service.WAKEUP_REPORT";
    private static final String AIACTION_WAKEUP_SERVICE = "com.huawei.ai.wakeup.service.WAKEUP2";
    private static final String AIVASSISTANT_PACKAGE_NAME = "com.huawei.hishow";
    private static final String ASSISTANT_VENDOR = SystemProperties.get("hw_sc.assistant.vendor", "huawei");
    private static final String BLUETOOTH_PKG_NAME = "com.android.bluetooth";
    private static final String CHIP_PLATFORM_NAME = SystemProperties.get("ro.board.platform", "UNDEFINED");
    private static final String CURRENT_PRODUCT_NAME = SystemProperties.get("ro.product.device", (String) null);
    private static final int DEFAULT_STREAM_TYPE = 1;
    private static final int DEVICE_IN_HEADPHONE = -2111825904;
    private static final int DEVICE_OUT_HEADPHONE = 604004364;
    private static final String DEV_PATH_SMART_HOLDER = "DEVPATH=/devices/virtual/misc/wakeup";
    private static final String DEV_PATH_SMART_HOLDER_VAR_INFO = "wakeup_info";
    private static final String DEV_PATH_SMART_HOLDER_VAR_REPORT = "wakeup_report";
    private static final int DUALPA_RCV_DEALY_DURATION = 5000;
    private static final String DUALPA_RCV_DEALY_OFF = "dualpa_security_delay=0";
    private static final String DUALPA_RCV_DEALY_ON = "dualpa_security_delay=1";
    private static final String EXCLUSIVE_APP_NAME = "com.rohdeschwarz.sit.topsecapp";
    private static final String EXCLUSIVE_PRODUCT_NAME = "HWH1711-Q";
    private static final String EXTRA_SOUNDTRIGGER_STATE = "soundtrigger_state";
    private static final int GAMEMODE_BACKGROUND = 2;
    private static final int GAMEMODE_FOREGROUND = 9;
    public static final int HW_AUDIO_EXCEPTION_OCCOUR = -1000;
    public static final int HW_AUDIO_MODE_TYPE_EXCEPTION_OCCOUR = -1001;
    public static final int HW_AUDIO_TRACK_OVERFLOW_TYPE_EXCEPTION_OCCOUR = -1002;
    private static final int HW_KARAOKE_EFFECT_BIT = 2;
    /* access modifiers changed from: private */
    public static final boolean HW_KARAOKE_EFFECT_ENABLED = ((SystemProperties.getInt("ro.config.hw_media_flags", 0) & 2) != 0);
    public static final int HW_MIC_MUTE_EXCEPTION_OCCOUR = -2002;
    public static final int HW_RING_MODE_TYPE_EXCEPTION_OCCOUR = -2001;
    public static final int HW_SCO_CONNECT_EXCEPTION_OCCOUR = -2003;
    private static final int IAWARE_DELAY_MESSAGE = 0;
    private static final int IAWARE_DELAY_TIME = 2000;
    private static final int IAWARE_RECONNECT_COUNT = 5;
    private static final int JUDGE_MILLISECONDS = 5000;
    private static final String KEY_SOUNDTRIGGER_SWITCH = "hw_soundtrigger_enabled";
    /* access modifiers changed from: private */
    public static final boolean LOW_LATENCY_SUPPORT = SystemProperties.getBoolean("persist.media.lowlatency.enable", false);
    private static final String LOW_LATENCY_WHITE_LIST = "/odm/etc/audio/low_latency/white_list.xml";
    private static final int MILLISECONDS_TO_SECONDS_BASE = 1000;
    private static final UUID MODEL_UUID = UUID.fromString("7dc67ab3-eab6-4e34-b62a-f4fa3788092a");
    private static final int MSG_AUDIO_EXCEPTION_OCCUR = 10000;
    private static final int MSG_DISABLE_HEADPHONE = 91;
    private static final int MSG_DUALPA_RCV_DEALY = 71;
    private static final int MSG_RINGER_MODE_CHANGE = 80;
    private static final int MSG_SHOW_DISABLE_HEADPHONE_TOAST = 92;
    private static final int MSG_SHOW_DISABLE_MICROPHONE_TOAST = 90;
    private static final int MSG_UPDATE_AFT_POLICY = 93;
    private static final String NODE_ATTR_PACKAGE = "package";
    private static final String NODE_WHITEAPP = "whiteapp";
    private static final int PACKAGE_ADDED = 1;
    private static final int PACKAGE_REMOVED = 2;
    private static final String PERMISSION_COMMFORCE = "android.permission.COMM_FORCE";
    public static final int PID_BIT_WIDE = 21;
    private static final String PLATFORM_NAME_HIDE_LVM_TOAST = "kirin990";
    private static final String PROP_DESKTOP_MODE = "sys.desktop.mode";
    private static final int RADIO_UID = 1001;
    private static final int RECORDSTATE_STOPPED = 1;
    private static final String SESSION_ID = "session_id";
    private static final int SETTINGS_RING_TYPE = 2;
    private static final String SOUNDTRIGGER_START = "start";
    private static final int SOUNDTRIGGER_STATUS_ON = 1;
    private static final String SOUNDTRIGGER_STOP = "stop";
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
    /* access modifiers changed from: private */
    public static final boolean SUPPORT_GAME_MODE = SystemProperties.getBoolean("ro.config.dolby_game_mode", false);
    private static final boolean SUPPORT_SIDE_TOUCH = (!SystemProperties.get("ro.config.hw_curved_side_disp", "").equals(""));
    private static final int SYSTEM_UID = 1000;
    private static final String TAG = "HwAudioService";
    private static final String TAG_CTAIFS = "ctaifs ";
    private static final long THREE_SEC_LIMITED = 3000;
    private static final String TV_WAKEUP_PKG = "com.huawei.hiai";
    private static final int UNKNOWN_IAWARE_APP_TYPE = -1;
    private static final String YANDEX_ASSISTANT_PKG = "ru.yandex.searchplugin";
    /* access modifiers changed from: private */
    public static boolean isCallForeground = false;
    private static final boolean mIsHuaweiSafeMediaConfig = SystemProperties.getBoolean("ro.config.huawei_safe_media", true);
    private static PhoneStateListener mPhoneListener = new PhoneStateListener() {
        /* class com.android.server.audio.HwAudioService.AnonymousClass1 */

        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            if (state == 0) {
                SystemProperties.set("persist.sys.audio_mode", "0");
            } else if (state == 1) {
                SystemProperties.set("persist.sys.audio_mode", "1");
            } else if (state == 2) {
                SystemProperties.set("persist.sys.audio_mode", "2");
            }
        }
    };
    private static final int mSecurityVolumeIndex = SystemProperties.getInt("ro.config.hw.security_volume", 0);
    /* access modifiers changed from: private */
    public static final boolean mVoipOptimizeInGameMode = SystemProperties.getBoolean("ro.config.gameassist_voipopt", false);
    /* access modifiers changed from: private */
    public boolean IS_IN_GAMEMODE = false;
    private long dialogShowTime = 0;
    private boolean isShowingDialog = false;
    private AudioExceptionRecord mAudioExceptionRecord = null;
    /* access modifiers changed from: private */
    public int mBindIAwareCount;
    private IBinder mBinder;
    private SoundTriggerDetector.Callback mCallBack = new SoundTriggerDetector.Callback() {
        /* class com.android.server.audio.HwAudioService.AnonymousClass6 */

        public void onAvailabilityChanged(int var1) {
        }

        public void onDetected(SoundTriggerDetector.EventPayload var1) {
            Slog.i(HwAudioService.TAG, "onDetected() called with: eventPayload = [" + var1 + "]");
            if (var1.getCaptureSession() != null) {
                DeviceIdleController.LocalService idleController = (DeviceIdleController.LocalService) LocalServices.getService(DeviceIdleController.LocalService.class);
                if (idleController != null) {
                    Slog.i(HwAudioService.TAG, "addPowerSaveTempWhitelistApp#va");
                    idleController.addPowerSaveTempWhitelistApp(Process.myUid(), HwAudioService.this.mVassistantPkg, 10000, HwAudioService.getCurrentUserId(), false, "hivoice");
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
    /* access modifiers changed from: private */
    public Context mContext;
    IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        /* class com.android.server.audio.HwAudioService.AnonymousClass2 */

        public void binderDied() {
            Slog.e(HwAudioService.TAG, "binderDied");
            HwAudioService.this.startSoundTriggerV2(true);
        }
    };
    protected volatile int mDesktopMode = -1;
    /* access modifiers changed from: private */
    public boolean mEnableAdjustVolume = true;
    /* access modifiers changed from: private */
    public EnableVolumeClient mEnableVolumeClient = null;
    /* access modifiers changed from: private */
    public final Object mEnableVolumeLock = new Object();
    private ForeAppTypeListener mForeTypelistener = new ForeAppTypeListener();
    private ArrayList<HeadphoneInfo> mHeadphones = new ArrayList<>();
    private int mHeadsetSwitchState = 0;
    private IAwareDeathRecipient mIAwareDeathRecipient = new IAwareDeathRecipient();
    private IAwareHandler mIAwareHandler;
    private IBinder mIAwareSdkService;
    /* access modifiers changed from: private */
    public boolean mIsFmConnected = false;
    private boolean mIsInSuperReceiverMode = false;
    private boolean mIsLinkDeathRecipient = false;
    private Toast mLVMToast = null;
    private long mLastSetMode3Time = -1;
    private long mLastStopAudioRecordTime = -1;
    private long mLastStopPlayBackTime = -1;
    private final BroadcastReceiver mLowlatencyReceiver = new BroadcastReceiver() {
        /* class com.android.server.audio.HwAudioService.AnonymousClass3 */

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                    String packageName = intent.getData().getSchemeSpecificPart();
                    if (HwAudioService.LOW_LATENCY_SUPPORT && HwAudioService.this.isLowlatencyPkg(packageName)) {
                        HwAudioService.this.updateLowlatencyUidsMap(packageName, 2);
                    }
                    if (HwAudioService.HW_KARAOKE_EFFECT_ENABLED && HwAudioService.this.mHwAudioServiceEx.isKaraokeWhiteListApp(packageName)) {
                        Slog.i(HwAudioService.TAG, "uninstall combine white app" + packageName);
                        HwAudioService.this.mHwAudioServiceEx.removeKaraokeWhiteAppUIDByPkgName(packageName);
                    }
                } else if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                    String packageName2 = intent.getData().getSchemeSpecificPart();
                    if (HwAudioService.LOW_LATENCY_SUPPORT && HwAudioService.this.isLowlatencyPkg(packageName2)) {
                        HwAudioService.this.updateLowlatencyUidsMap(packageName2, 1);
                    }
                    if (HwAudioService.HW_KARAOKE_EFFECT_ENABLED && HwAudioService.this.mHwAudioServiceEx.isKaraokeWhiteListApp(packageName2)) {
                        HwAudioService.this.mHwAudioServiceEx.setKaraokeWhiteAppUIDByPkgName(packageName2);
                    }
                }
            }
        }
    };
    private Map<String, Integer> mLowlatencyUidsMap = new ArrayMap();
    private final PackageMonitor mPackageMonitor = new PackageMonitor() {
        /* class com.android.server.audio.HwAudioService.AnonymousClass5 */

        public void onPackageRemoved(String packageName, int uid) {
            String assistantPkg = HwAudioService.this.getAssistantPackageName();
            if (packageName.equals(assistantPkg)) {
                Slog.i(HwAudioService.TAG, "onPackageRemoved uid :" + uid);
                HwAudioService.this.updateVAVersionCode(assistantPkg);
            }
        }

        public void onPackageDataCleared(String packageName, int uid) {
            if (packageName.equals(HwAudioService.this.getAssistantPackageName())) {
                Slog.i(HwAudioService.TAG, "onPackageDataCleared uid : " + uid);
                HwAudioService.this.resetVASoundTrigger();
            }
        }

        public void onPackageAppeared(String packageName, int reason) {
            String assistantPkg = HwAudioService.this.getAssistantPackageName();
            if (packageName.equals(assistantPkg)) {
                Slog.i(HwAudioService.TAG, "onPackageUpdateStarted reason : " + reason);
                HwAudioService.this.updateVAVersionCode(assistantPkg);
            }
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        /* class com.android.server.audio.HwAudioService.AnonymousClass4 */

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (action != null && action.equals("android.intent.action.FM")) {
                    int state = intent.getIntExtra(SceneRecogFeature.DATA_STATE, 0);
                    boolean isConnected = HwAudioService.this.mIsFmConnected;
                    if (state == 0 && isConnected) {
                        AudioSystem.setDeviceConnectionState((int) HighBitsCompModeID.MODE_COLOR_ENHANCE, 0, "", "", 0);
                        boolean unused = HwAudioService.this.mIsFmConnected = false;
                    } else if (state == 1 && !isConnected) {
                        AudioSystem.setDeviceConnectionState((int) HighBitsCompModeID.MODE_COLOR_ENHANCE, 1, "", "", 0);
                        boolean unused2 = HwAudioService.this.mIsFmConnected = true;
                    }
                } else if (action != null && HwAudioService.ACTION_INCALL_SCREEN.equals(action)) {
                    boolean unused3 = HwAudioService.isCallForeground = intent.getBooleanExtra(HwAudioService.ACTION_INCALL_EXTRA, true);
                } else if (action != null && "android.intent.action.PHONE_STATE".equals(action)) {
                    if (TelephonyManager.EXTRA_STATE_IDLE.equals(intent.getStringExtra(SceneRecogFeature.DATA_STATE))) {
                        HwAudioService.this.force_exitLVMMode();
                    }
                    if (AbsAudioService.IS_SUPER_RECEIVER_ENABLED) {
                        HwAudioService.this.closeSuperReceiverMode();
                    }
                } else if (action != null && "android.intent.action.USER_UNLOCKED".equals(action)) {
                    HwAudioService.this.onUserUnlocked(intent.getIntExtra("android.intent.extra.user_handle", -10000));
                } else if (action != null && "android.intent.action.BOOT_COMPLETED".equals(action)) {
                    HwAudioService.this.bindIAwareSdk();
                }
            }
        }
    };
    private Toast mRingerModeToast = null;
    /* access modifiers changed from: private */
    public SetICallBackForKaraokeHandler mSetICallBackForKaraokeHandler = null;
    /* access modifiers changed from: private */
    public boolean mSetVbrlibFlag = false;
    private final UEventObserver mSmartHolderObserver = new UEventObserver() {
        /* class com.android.server.audio.HwAudioService.AnonymousClass8 */

        public void onUEvent(UEventObserver.UEvent event) {
            if (event != null) {
                String shState = event.get(HwAudioService.DEV_PATH_SMART_HOLDER_VAR_REPORT);
                String info = event.get(HwAudioService.DEV_PATH_SMART_HOLDER_VAR_INFO);
                if (info != null && info.length() > 0) {
                    HwAudioService.this.startAIWakeupServiceReport(info);
                }
                if (shState != null && shState.length() > 0) {
                    HwAudioService.this.startAIWakeupService(shState);
                }
            }
        }
    };
    private SoundTriggerDetector mSoundTriggerDetector;
    private Handler mSoundTriggerHandler;
    private SoundTriggerManager mSoundTriggerManager;
    private ISoundTriggerService mSoundTriggerService;
    private int mSpkRcvStereoStatus = -1;
    private long mSuperReceiverStartTime = 0;
    private UserManager mUserManager;
    private final UserManagerInternal mUserManagerInternal;
    private int mVAVersinCode;
    /* access modifiers changed from: private */
    public String mVassistantPkg = "com.huawei.vassistant";
    private List<String> mWhiteAppName = new ArrayList();
    private int oldInCallDevice = 0;
    private int oldLVMDevice = 0;
    /* access modifiers changed from: private */
    public int type = -1;

    static /* synthetic */ int access$2608(HwAudioService x0) {
        int i = x0.mBindIAwareCount;
        x0.mBindIAwareCount = i + 1;
        return i;
    }

    /* access modifiers changed from: protected */
    public boolean usingHwSafeMediaConfig() {
        return mIsHuaweiSafeMediaConfig;
    }

    /* access modifiers changed from: protected */
    public int getHwSafeMediaVolumeIndex() {
        return mSecurityVolumeIndex * 10;
    }

    /* access modifiers changed from: protected */
    public boolean isHwSafeMediaVolumeEnabled() {
        return mSecurityVolumeIndex > 0;
    }

    /* access modifiers changed from: protected */
    public boolean checkMMIRunning() {
        return "true".equals(SystemProperties.get("runtime.mmitest.isrunning", "false"));
    }

    public HwAudioService(Context context) {
        super(context);
        Slog.i(TAG, TAG);
        this.mContext = context;
        if (isOversea() && !isAppInstalled(context, this.mVassistantPkg)) {
            this.mVassistantPkg = "com.huawei.hiassistantoversea";
        }
        IntentFilter intentFilter = new IntentFilter("android.intent.action.FM");
        intentFilter.addAction(ACTION_INCALL_SCREEN);
        intentFilter.addAction("android.intent.action.PHONE_STATE");
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        intentFilter.addAction("android.intent.action.REBOOT");
        intentFilter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_ACTION_SHUTDOWN);
        intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        context.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, intentFilter, null, null);
        this.mSmartHolderObserver.startObserving(DEV_PATH_SMART_HOLDER);
        PackageMonitor packageMonitor = this.mPackageMonitor;
        Context context2 = this.mContext;
        packageMonitor.register(context2, context2.getMainLooper(), UserHandle.SYSTEM, false);
        this.mContentResolver = this.mContext.getContentResolver();
        this.mAudioExceptionRecord = new AudioExceptionRecord();
        readPersistedSettingsEx(this.mContentResolver);
        setAudioSystemParameters();
        HwServiceFactory.getHwDrmDialogService().startDrmDialogService(context);
        this.mUserManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        if (LOW_LATENCY_SUPPORT || HW_KARAOKE_EFFECT_ENABLED) {
            IntentFilter lowlatencyIntentFilter = new IntentFilter();
            lowlatencyIntentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
            lowlatencyIntentFilter.addAction("android.intent.action.PACKAGE_ADDED");
            lowlatencyIntentFilter.addDataScheme(NODE_ATTR_PACKAGE);
            this.mContext.registerReceiverAsUser(this.mLowlatencyReceiver, UserHandle.ALL, lowlatencyIntentFilter, null, null);
            if (LOW_LATENCY_SUPPORT) {
                initLowlatencyUidsMap();
            }
        }
        ((TelephonyManager) context.getSystemService("phone")).listen(mPhoneListener, 32);
        ActivityManagerEx.registerGameObserver(new HwAudioGameObserver());
    }

    private boolean isAppInstalled(Context context, String packageName) {
        List<PackageInfo> installedPackages = context.getPackageManager().getInstalledPackages(0);
        for (int i = 0; i < installedPackages.size(); i++) {
            if (installedPackages.get(i).packageName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    /* access modifiers changed from: private */
    public String getAssistantPackageName() {
        String assistantPkg = this.mVassistantPkg;
        if ("yandex".equals(ASSISTANT_VENDOR)) {
            return YANDEX_ASSISTANT_PKG;
        }
        return assistantPkg;
    }

    /* access modifiers changed from: private */
    public boolean isSupportSoundToVibrateMode() {
        return SystemProperties.getInt("ro.config.gameassist_soundtovibrate", 0) == 1;
    }

    private class HwAudioGameObserver extends IGameObserver.Stub {
        private HwAudioGameObserver() {
        }

        public void onGameListChanged() {
            Slog.v(HwAudioService.TAG, "onGameListChanged !");
        }

        public void onGameStatusChanged(String packageName, int event) {
            if (packageName != null) {
                Slog.d(HwAudioService.TAG, "onGameStatusChanged packageName = " + packageName + ", event = " + event);
                if (HwAudioService.this.isSupportSoundToVibrateMode() && Settings.Secure.getInt(HwAudioService.this.mContext.getContentResolver(), "sound_to_vibrate_effect", 1) == 1) {
                    if (event != 1) {
                        if (event == 2) {
                            AudioSystem.setParameters("vbrmode=background");
                            return;
                        } else if (event != 4) {
                            Slog.w(HwAudioService.TAG, "onGameStatusChanged event not in foreground or background!");
                            return;
                        }
                    }
                    if (!HwAudioService.this.mSetVbrlibFlag) {
                        Slog.d(HwAudioService.TAG, "HwAudioGameObserver set mSetVbrlibFalg = true means firstly load the vbr lib!");
                        AudioSystem.setParameters("vbrEnter=on");
                        boolean unused = HwAudioService.this.mSetVbrlibFlag = true;
                    }
                    AudioSystem.setParameters("vbrmode=front;gamepackagename=" + packageName);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void onUserUnlocked(int userId) {
        if (userId != -10000) {
            createManagers();
            boolean isPrimary = this.mUserManager.getUserInfo(userId).isPrimary();
            Slog.i(TAG, "user unlocked, start soundtrigger! isPrimary : " + isPrimary);
            if (isPrimary) {
                updateVAVersionCode(getAssistantPackageName());
                createAndStartSoundTrigger(true);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onUserBackground(int userId) {
        Slog.i(TAG, "onUserBackground userId : " + userId);
        checkWithUserSwith(userId, true);
    }

    /* access modifiers changed from: protected */
    public void onUserForeground(int userId) {
        Slog.i(TAG, "onUserForeground userId : " + userId);
        checkWithUserSwith(userId, false);
    }

    private void checkWithUserSwith(int userId, boolean background) {
        createManagers();
        if (userId >= 0) {
            boolean isPrimary = UserManagerService.getInstance().getUserInfo(userId).isPrimary();
            Slog.i(TAG, "onUserBackground isPrimary : " + isPrimary);
            if (isPrimary) {
                createAndStartSoundTrigger(!background);
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateVAVersionCode(String packageName) {
        int versionCode = 0;
        try {
            versionCode = this.mContext.getPackageManager().getPackageInfo(packageName, 128).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "updateVAVersionCode vassistant not found!");
        }
        Slog.i(TAG, "updateVAVersionCode versionCode : " + versionCode + " mVAVersinCode = " + this.mVAVersinCode);
        if (versionCode < this.mVAVersinCode) {
            resetVASoundTrigger();
        }
        this.mVAVersinCode = versionCode;
    }

    /* access modifiers changed from: private */
    public void resetVASoundTrigger() {
        startSoundTriggerV2(false);
        Settings.Secure.putInt(this.mContentResolver, KEY_SOUNDTRIGGER_SWITCH, 0);
    }

    private boolean isSoundTriggerOn() {
        if (Settings.Secure.getInt(this.mContentResolver, KEY_SOUNDTRIGGER_SWITCH, 0) == 1) {
            return true;
        }
        return false;
    }

    private void createAndStartSoundTrigger(boolean start) {
        startSoundTriggerV2(start);
    }

    private boolean startSoundTriggerDetector() {
        createManagers();
        createSoundTriggerDetector();
        SoundTrigger.GenericSoundModel model = getCurrentModel();
        Slog.i(TAG, "startSoundTriggerDetector model : " + model);
        long ident = Binder.clearCallingIdentity();
        if (model != null) {
            try {
                if (this.mSoundTriggerDetector.startRecognition(1)) {
                    Slog.i(TAG, "start recognition successfully!");
                    return true;
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        Slog.e(TAG, "start recognition failed!");
        Binder.restoreCallingIdentity(ident);
        return false;
    }

    private boolean stopSoundTriggerDetector() {
        createManagers();
        createSoundTriggerDetector();
        SoundTrigger.GenericSoundModel model = getCurrentModel();
        Slog.i(TAG, "stopSoundTriggerDetector model : " + model);
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
        Slog.i(TAG, "wakeupV2 : " + wakeupV2);
        if ("".equals(wakeupV2)) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void startSoundTriggerV2(boolean start) {
        if (!isSupportWakeUpV2()) {
            Slog.i(TAG, "startSoundTriggerV2 not support wakeup v2");
            return;
        }
        createManagers();
        SoundTrigger.GenericSoundModel model = getCurrentModel();
        boolean isUserUnlocked = this.mUserManager.isUserUnlocked(UserHandle.SYSTEM);
        boolean isSoundTriggerOn = isSoundTriggerOn();
        Slog.i(TAG, "startSoundTriggerV2 model : " + model + " isSoundTriggerOn : " + isSoundTriggerOn + " isUserUnlocked : " + isUserUnlocked + " start : " + start);
        if (isUserUnlocked && model != null && isSoundTriggerOn) {
            if (start) {
                startSoundTriggerDetector();
            } else {
                stopSoundTriggerDetector();
            }
        }
    }

    private SoundTrigger.GenericSoundModel getCurrentModel() {
        try {
            if (this.mSoundTriggerService != null) {
                return this.mSoundTriggerService.getSoundModel(new ParcelUuid(MODEL_UUID));
            }
            Slog.e(TAG, "getCurrentModel mSoundTriggerService is null!");
            return null;
        } catch (Exception e) {
            Slog.e(TAG, "getCurrentModel err");
            return null;
        }
    }

    /* access modifiers changed from: private */
    public void startWakeupService(int session) {
        Slog.d(TAG, "startWakeupService: " + this.mVassistantPkg);
        Intent intent = new Intent(ACTION_WAKEUP_SERVICE);
        intent.setPackage(this.mVassistantPkg);
        intent.putExtra(SESSION_ID, session);
        try {
            this.mContext.startService(intent);
        } catch (IllegalStateException | SecurityException e) {
            Slog.w(TAG, "startWakeupService failed");
        }
    }

    public AudioRoutesInfo startWatchingRoutes(IAudioRoutesObserver observer) {
        Slog.d(TAG, "startWatchingRoutes");
        if (!linkVAtoDeathRe(observer)) {
            return HwAudioService.super.startWatchingRoutes(observer);
        }
        return null;
    }

    private boolean linkVAtoDeathRe(IAudioRoutesObserver observer) {
        PackageManager pm;
        String[] packages;
        Slog.d(TAG, "linkVAtoDeathRe for vassistant");
        int uid = Binder.getCallingUid();
        Context context = this.mContext;
        if (context == null || (pm = context.getPackageManager()) == null || (packages = pm.getPackagesForUid(uid)) == null) {
            return false;
        }
        int i = 0;
        while (i < packages.length) {
            String packageName = packages[i];
            Slog.d(TAG, "packageName:" + packageName);
            if (this.mVassistantPkg.equals(packageName)) {
                this.mBinder = observer.asBinder();
                try {
                    this.mBinder.linkToDeath(this.mDeathRecipient, 0);
                    this.mIsLinkDeathRecipient = true;
                    return true;
                } catch (RemoteException e) {
                    Slog.e(TAG, "linkVAtoDeathRe err");
                }
            } else {
                i++;
            }
        }
        return false;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r25v0, resolved type: android.os.Parcel */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r5v2, types: [int, boolean] */
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        boolean _result;
        boolean _arg0 = false;
        if (code == 101) {
            sendMsg(this.mAudioHandler, 8, 2, 1, data.readInt() == 1 ? 1 : 0, null, 0);
            reply.writeNoException();
            return true;
        } else if (code == 102) {
            reply.writeNoException();
            return true;
        } else if (code == 1003) {
            data.enforceInterface("android.media.IAudioService");
            data.readString();
            recordLastRecordTime(data.readInt(), data.readInt());
            reply.writeNoException();
            return true;
        } else if (code == 1005) {
            data.enforceInterface("android.media.IAudioService");
            sendMsg(this.mAudioHandler, MSG_SHOW_DISABLE_MICROPHONE_TOAST, 2, 0, 0, null, 20);
            reply.writeNoException();
            return true;
        } else if (code != 1006) {
            switch (code) {
                case 1101:
                    ?? isAdjustVolumeEnable = isAdjustVolumeEnable();
                    Slog.i(TAG, "isAdjustVolumeEnable transaction called. result:" + (isAdjustVolumeEnable == true ? 1 : 0));
                    reply.writeNoException();
                    reply.writeInt(isAdjustVolumeEnable);
                    return true;
                case 1102:
                    IBinder callerToken = data.readStrongBinder();
                    if (data.readInt() != 0) {
                        _arg0 = true;
                    }
                    Slog.i(TAG, "enableVolumeAdjust  transaction called.enable:" + _arg0);
                    enableVolumeAdjust(_arg0, Binder.getCallingUid(), callerToken);
                    reply.writeNoException();
                    return true;
                case 1103:
                    data.enforceInterface("android.media.IAudioService");
                    if (data.readInt() != 0) {
                        _result = startSoundTriggerDetector();
                    } else {
                        _result = stopSoundTriggerDetector();
                    }
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    IBinder iBinder = this.mBinder;
                    if (iBinder != null && this.mIsLinkDeathRecipient) {
                        iBinder.unlinkToDeath(this.mDeathRecipient, 0);
                        this.mIsLinkDeathRecipient = false;
                    }
                    return true;
                case 1104:
                    int _arg02 = data.readInt();
                    Slog.i(TAG, "desktopModeChanged transaction mode:" + _arg02);
                    desktopModeChanged(_arg02);
                    reply.writeNoException();
                    return true;
                case 1105:
                    data.enforceInterface("android.media.IAudioService");
                    reply.writeNoException();
                    reply.writeBoolean(isScoAvailableOffCall());
                    return true;
                case HwArbitrationDEFS.MSG_STREAMING_VIDEO_BAD /*{ENCODED_INT: 1106}*/:
                    data.enforceInterface("android.media.IAudioService");
                    reply.writeNoException();
                    reply.writeBoolean(isHwKaraokeEffectEnable());
                    return true;
                case HwArbitrationDEFS.MSG_GAME_WAR_STATE_BAD /*{ENCODED_INT: 1107}*/:
                    data.enforceInterface("android.media.IAudioService");
                    setICallBackForKaraoke(data.readStrongBinder());
                    reply.writeNoException();
                    return true;
                default:
                    return HwAudioService.super.onTransact(code, data, reply, flags);
            }
        } else {
            data.enforceInterface("android.media.IAudioService");
            sendMsg(this.mAudioHandler, 91, 2, 0, 0, null, 0);
            reply.writeNoException();
            return true;
        }
    }

    public boolean isHwKaraokeEffectEnable() {
        boolean enable = this.mHwAudioServiceEx.isHwKaraokeEffectEnable(getPackageNameByPid(Binder.getCallingPid()));
        Slog.i(TAG, "HwKaraoke Effect enable = " + enable);
        return enable;
    }

    private void setICallBackForKaraoke(IBinder cb) {
        if (cb != null) {
            Slog.d(TAG, "setICallBackForKaraoke in audioService caller pid = " + Binder.getCallingPid());
            SetICallBackForKaraokeHandler setICallBackForKaraokeHandler = this.mSetICallBackForKaraokeHandler;
            if (setICallBackForKaraokeHandler == null) {
                this.mSetICallBackForKaraokeHandler = new SetICallBackForKaraokeHandler(cb);
            } else if (setICallBackForKaraokeHandler.getBinder() == cb) {
                Slog.d(TAG, "mSetICallBackForKaraokeHandler cb:" + cb + " is already linked.");
            } else {
                this.mSetICallBackForKaraokeHandler.release();
                this.mSetICallBackForKaraokeHandler = new SetICallBackForKaraokeHandler(cb);
            }
        }
    }

    /* access modifiers changed from: private */
    public class SetICallBackForKaraokeHandler implements IBinder.DeathRecipient {
        private String mCallerPkg;
        private IBinder mCb;

        SetICallBackForKaraokeHandler(IBinder cb) {
            if (cb != null) {
                try {
                    cb.linkToDeath(this, 0);
                } catch (RemoteException e) {
                    Slog.w(HwAudioService.TAG, "SetICallBackForKaraokeHandler() could not link to " + cb + " binder death");
                    cb = null;
                }
            }
            this.mCb = cb;
            this.mCallerPkg = HwAudioService.this.getPackageNameByPid(Binder.getCallingPid());
        }

        public void binderDied() {
            Slog.w(HwAudioService.TAG, "Karaoke client died,pkgname = " + this.mCallerPkg);
            AudioSystem.setParameters("Karaoke_enable=disable");
            release();
        }

        public void release() {
            IBinder iBinder = this.mCb;
            if (iBinder != null) {
                iBinder.unlinkToDeath(this, 0);
                this.mCb = null;
            }
            SetICallBackForKaraokeHandler unused = HwAudioService.this.mSetICallBackForKaraokeHandler = null;
        }

        public IBinder getBinder() {
            return this.mCb;
        }
    }

    public boolean isScoAvailableOffCall() {
        return this.mDeviceBroker.isScoAvailableOffCall();
    }

    public static void printCtaifsLog(String applicationName, String packageName, String callingMethod, String desciption) {
        Slog.i(TAG_CTAIFS, "<" + applicationName + ">[" + applicationName + "][" + packageName + "][" + callingMethod + "] " + desciption);
    }

    public static String getRecordAppName(Context context, String packageName) {
        PackageManager pm;
        if (context == null || (pm = context.getPackageManager()) == null) {
            return null;
        }
        ApplicationInfo appInfo = null;
        long ident = Binder.clearCallingIdentity();
        try {
            appInfo = pm.getApplicationInfoAsUser(packageName, 0, getCurrentUserId());
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "App Name Not Found");
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
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

    private boolean isAdjustVolumeEnable() {
        boolean z;
        synchronized (this.mEnableVolumeLock) {
            z = this.mEnableAdjustVolume;
        }
        return z;
    }

    private void enableVolumeAdjust(boolean enable, int callerUid, IBinder cb) {
        if (!hasSystemPriv(callerUid)) {
            Slog.i(TAG, "caller is not system app.Can not set volumeAdjust to enable or disable.");
            return;
        }
        synchronized (this.mEnableVolumeLock) {
            String callerPkg = getPackageNameByPid(Binder.getCallingPid());
            if (this.mEnableVolumeClient == null) {
                this.mEnableVolumeClient = new EnableVolumeClient(cb);
                this.mEnableAdjustVolume = enable;
            } else {
                String clientPkg = this.mEnableVolumeClient.getCallerPkg();
                if (callerPkg == null || !callerPkg.equals(clientPkg)) {
                    Slog.w(TAG, "Just allowed one caller use the interface until older caller die.older. caller:" + callerPkg + " old callser:" + clientPkg);
                } else {
                    this.mEnableAdjustVolume = enable;
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public class EnableVolumeClient implements IBinder.DeathRecipient {
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
                    Slog.w(HwAudioService.TAG, "EnableVolumeClient() could not link to " + cb + " binder death");
                    cb = null;
                }
            }
            this.mCb = cb;
            this.mCallerPkg = HwAudioService.this.getPackageNameByPid(Binder.getCallingPid());
        }

        public void binderDied() {
            synchronized (HwAudioService.this.mEnableVolumeLock) {
                Slog.i(HwAudioService.TAG, " EnableVolumeClient died.pkgname:" + this.mCallerPkg);
                boolean unused = HwAudioService.this.mEnableAdjustVolume = true;
                release();
            }
        }

        public void release() {
            IBinder iBinder = this.mCb;
            if (iBinder != null) {
                iBinder.unlinkToDeath(this, 0);
                this.mCb = null;
            }
            EnableVolumeClient unused = HwAudioService.this.mEnableVolumeClient = null;
        }
    }

    /* access modifiers changed from: protected */
    public void readPersistedSettingsEx(ContentResolver cr) {
        if (SPK_RCV_STEREO_SUPPORT) {
            getSpkRcvStereoState(cr);
        }
    }

    /* access modifiers changed from: protected */
    public void onErrorCallBackEx(int exceptionId) {
        Slog.i(TAG, "AudioException onErrorCallBackEx exceptionId:" + exceptionId);
        if (exceptionId <= -1000) {
            sendMsg(this.mAudioHandler, 10000, 1, exceptionId, 0, null, 0);
        }
    }

    /* access modifiers changed from: protected */
    public void onScoExceptionOccur(int clientPid) {
        Slog.w(TAG, "AudioException ScoExceptionOccur,clientpid:" + clientPid + " have more than one sco connected!");
        String packageName = getPackageNameByPid(clientPid);
        sendMsg(this.mAudioHandler, 10000, 1, HW_SCO_CONNECT_EXCEPTION_OCCOUR, 0, new AudioExceptionMsg(HW_SCO_CONNECT_EXCEPTION_OCCOUR, packageName, getVersionName(packageName)), 0);
    }

    public void setMicrophoneMute(boolean on, String callingPackage, int userId) {
        boolean firstState = AudioSystem.isMicrophoneMuted();
        HwAudioService.super.setMicrophoneMute(on, callingPackage, userId);
        if (!firstState && AudioSystem.isMicrophoneMuted()) {
            this.mAudioExceptionRecord.updateMuteMsg(callingPackage, getVersionName(callingPackage));
        }
    }

    private boolean hasCameraRecordPermission(String packageName) {
        PackageManager pm;
        if (packageName == null || packageName.length() == 0 || (pm = this.mContext.getPackageManager()) == null || pm.checkPermission("android.permission.CAMERA", packageName) != 0 || pm.checkPermission("android.permission.RECORD_AUDIO", packageName) != 0) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void handleMessageEx(Message msg) {
        int i = msg.what;
        if (i == MSG_DUALPA_RCV_DEALY) {
            AudioSystem.setParameters(DUALPA_RCV_DEALY_OFF);
        } else if (i != 80) {
            if (i == 10099) {
                handleSuperReceiverProcess(msg);
            } else if (i == 10000) {
                onAudioException(msg.arg1, msg.obj != null ? (AudioExceptionMsg) msg.obj : null);
            } else if (i != 10001) {
                switch (i) {
                    case MSG_SHOW_DISABLE_MICROPHONE_TOAST /*{ENCODED_INT: 90}*/:
                        showDisableMicrophoneToast();
                        return;
                    case 91:
                        disableHeadPhone();
                        return;
                    case 92:
                        showDisableHeadphoneToast();
                        return;
                    case 93:
                        updateAftPolicyInternal();
                        return;
                    default:
                        return;
                }
            } else if (LOUD_VOICE_MODE_SUPPORT) {
                handleLVMModeChangeProcess(msg.arg1, msg.obj);
            }
        } else if (msg.obj == null) {
            Slog.e(TAG, "MSG_RINGER_MODE_CHANGE msg obj is null!");
        } else {
            AudioExceptionMsg tempMsg = (AudioExceptionMsg) msg.obj;
            String caller = tempMsg.getMsgPackagename();
            if (caller == null) {
                caller = "";
            }
            int fromRingerMode = msg.arg1;
            int ringerMode = msg.arg2;
            if (!isFirstTimeShow(caller) || hasCameraRecordPermission(caller)) {
                Slog.i(TAG, "AudioException showRingerModeToast");
                showRingerModeToast(caller, fromRingerMode, ringerMode);
            } else {
                Slog.i(TAG, "AudioException showRingerModeDialog");
                showRingerModeDialog(caller, fromRingerMode, ringerMode);
                savePkgNameToDB(caller);
            }
            onAudioException(HW_RING_MODE_TYPE_EXCEPTION_OCCOUR, tempMsg);
        }
    }

    private void openSuperReceiverMode() {
        Slog.i(TAG, "open super receiver");
        if (!this.mIsInSuperReceiverMode) {
            this.mIsInSuperReceiverMode = true;
            this.mSuperReceiverStartTime = System.currentTimeMillis();
        }
        AudioSystem.setParameters("super_receiver_mode=on");
    }

    /* access modifiers changed from: private */
    public void closeSuperReceiverMode() {
        Slog.i(TAG, "close super receiver");
        if (this.mIsInSuperReceiverMode) {
            this.mIsInSuperReceiverMode = false;
            HwMediaMonitorManager.writeBigData(916600020, "SUPER_RECIVIER_USE_TIME", (int) ((System.currentTimeMillis() - this.mSuperReceiverStartTime) / 1000), 1000);
        }
        AudioSystem.setParameters("super_receiver_mode=off");
    }

    private void handleSuperReceiverProcess(Message msg) {
        if (msg == null) {
            Slog.e(TAG, "Super Receiver msg is null");
        } else if (this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") != 0) {
            Slog.e(TAG, "Super Receiver Permission Denial: setSuperReceiverMode");
        } else if (isInCall()) {
            if (msg.arg1 == 0) {
                closeSuperReceiverMode();
            } else if (msg.obj == null || !(msg.obj instanceof AbsAudioService.DeviceVolumeState)) {
                Slog.e(TAG, "Message object error");
            } else {
                AbsAudioService.DeviceVolumeState myObj = (AbsAudioService.DeviceVolumeState) msg.obj;
                if (myObj.mstreamType != 0) {
                    Slog.e(TAG, "handleSuperReceiverProcess abort for not stream error");
                } else if (myObj.mDevice == 1) {
                    changeSuperReceiverMode(myObj);
                } else {
                    closeSuperReceiverMode();
                }
            }
        }
    }

    private void changeSuperReceiverMode(AbsAudioService.DeviceVolumeState myObj) {
        if (getStreamMaxIndex(0) == myObj.mOldIndex) {
            Slog.i(TAG, "changeSuperReceiverMode");
            if (myObj.mDirection == 1) {
                openSuperReceiverMode();
            }
            if (myObj.mDirection == -1) {
                closeSuperReceiverMode();
            }
            if (myObj.mDirection == 0 && getStreamIndex(0, myObj.mDevice) - myObj.mOldIndex < 0) {
                closeSuperReceiverMode();
            }
        }
    }

    public void onAudioException(int exceptionId, AudioExceptionMsg exceptionMsg) {
        if (!this.mSystemReady) {
            Slog.e(TAG, "AudioException,but system is not ready! ");
        }
        if (exceptionId != -1001) {
            switch (exceptionId) {
                case HW_SCO_CONNECT_EXCEPTION_OCCOUR /*{ENCODED_INT: -2003}*/:
                    Slog.w(TAG, "AudioException HW_SCO_CONNECT_EXCEPTION_OCCOUR");
                    return;
                case HW_MIC_MUTE_EXCEPTION_OCCOUR /*{ENCODED_INT: -2002}*/:
                    if (!this.mUserManagerInternal.getUserRestriction(getCurrentUserId(), "no_unmute_microphone")) {
                        Slog.w(TAG, "AudioException HW_MIC_MUTE_EXCEPTION_OCCOUR");
                        if (EXCLUSIVE_PRODUCT_NAME.equals(CURRENT_PRODUCT_NAME)) {
                            Slog.w(TAG, "Not allowded to recovery according to the us operator's rules");
                            return;
                        } else if (exceptionMsg == null || !EXCLUSIVE_APP_NAME.equals(exceptionMsg.getMsgPackagename())) {
                            AudioSystem.muteMicrophone(false);
                            return;
                        } else {
                            Slog.w(TAG, "Not allowded to recovery according to this App");
                            return;
                        }
                    } else {
                        return;
                    }
                case HW_RING_MODE_TYPE_EXCEPTION_OCCOUR /*{ENCODED_INT: -2001}*/:
                    Slog.w(TAG, "AudioException HW_RING_MODE_TYPE_EXCEPTION_OCCOUR");
                    return;
                default:
                    int tmp = -exceptionId;
                    if ((-(tmp >> 21)) == -1002) {
                        int exceptionPid = tmp - 2101346304;
                        if (exceptionPid > 0) {
                            String packageName = getPackageNameByPid(exceptionPid);
                            String packageVersion = getVersionName(packageName);
                            if (packageName != null) {
                                Slog.e(TAG, "AudioTrack_Overflow packageName = " + packageName + " " + packageVersion + " pid = " + exceptionPid);
                                HwMediaMonitorManager.writeLogMsg(916010207, 3, (int) HW_AUDIO_TRACK_OVERFLOW_TYPE_EXCEPTION_OCCOUR, "OAE5");
                                return;
                            }
                            Slog.w(TAG, "AudioTrack_Overflow getPackageNameByPid failed");
                            return;
                        }
                        Slog.w(TAG, "AudioTrack_Overflow pid error");
                        return;
                    }
                    Slog.w(TAG, "No such AudioException exceptionId:" + exceptionId);
                    return;
            }
        } else {
            Slog.w(TAG, "AudioException HW_AUDIO_MODE_TYPE_EXCEPTION_OCCOUR, setModeInt AudioSystem.MODE_NORMAL");
        }
    }

    /* access modifiers changed from: protected */
    public void handleLVMModeChangeProcess(int state, Object object) {
        if (state == 1) {
            if (object != null) {
                AbsAudioService.DeviceVolumeState myObj = (AbsAudioService.DeviceVolumeState) object;
                setLVMMode(myObj.mDirection, myObj.mDevice, myObj.mOldIndex, myObj.mstreamType);
            }
        } else if (isInCall()) {
            int device = getInCallDevice();
            setLVMMode(0, device, -1, 0);
            setVoiceALGODeviceChange(device);
        }
    }

    private boolean isInCall() {
        TelecomManager telecomManager;
        if (getMode() == 2 && (telecomManager = (TelecomManager) this.mContext.getSystemService("telecom")) != null) {
            return telecomManager.isInCall();
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public int getOldInCallDevice(int mode) {
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

    /* access modifiers changed from: private */
    public void force_exitLVMMode() {
        if ("true".equals(AudioSystem.getParameters("VOICE_LVM_Enable"))) {
            AudioSystem.setParameters("VOICE_LVM_Enable=false");
            this.oldLVMDevice = 0;
        }
    }

    /* access modifiers changed from: protected */
    public void setLVMMode(int direction, int device, int oldIndex, int streamType) {
        if (getMode() == 2 && this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") != 0) {
            Slog.e(TAG, "MODIFY_PHONE_STATE Permission Denial: setLVMMode");
        } else if (!LOUD_VOICE_MODE_SUPPORT || !isInCall() || streamType != 0) {
            Slog.e(TAG, "setLVMMode abort");
        } else {
            boolean isLVMMode = "true".equals(AudioSystem.getParameters("VOICE_LVM_Enable"));
            boolean isLVMDevice = 2 == device || 1 == device;
            boolean isMaxVolume = getStreamMaxIndex(0) == oldIndex;
            boolean isChangeVolume = false;
            if (isMaxVolume && isLVMDevice && this.oldLVMDevice == device) {
                isChangeVolume = getStreamIndex(0, device) < oldIndex;
            }
            if (!isLVMMode && isMaxVolume && 1 == direction && isLVMDevice) {
                AudioSystem.setParameters("VOICE_LVM_Enable=true");
                if (device == 1) {
                }
                showLVMToast(33685776);
                this.oldLVMDevice = device;
            } else if (!isLVMMode) {
            } else {
                if ((-1 == direction || device != this.oldLVMDevice || isChangeVolume) && this.oldLVMDevice != 0) {
                    AudioSystem.setParameters("VOICE_LVM_Enable=false");
                    showLVMToast(33685777);
                    this.oldLVMDevice = 0;
                }
            }
        }
    }

    private void showLVMToast(int message) {
        if (PLATFORM_NAME_HIDE_LVM_TOAST.equals(CHIP_PLATFORM_NAME)) {
            Slog.i(TAG, "do not show LVM Toast for kirin990");
            return;
        }
        Slog.i(TAG, "show LVM Toast except kirin990");
        if (this.mLVMToast == null) {
            this.mLVMToast = Toast.makeText(new ContextThemeWrapper(this.mContext, this.mContext.getResources().getIdentifier("androidhwext:style/Theme.Emui.Toast", null, null)), "Unknown State", 0);
            this.mLVMToast.getWindowParams().privateFlags |= 16;
        }
        try {
            this.mLVMToast.setText(message);
            this.mLVMToast.show();
        } catch (Exception e) {
            Slog.e(TAG, "showLVMToast exception");
        }
    }

    private boolean isInCallingState() {
        TelephonyManager tmgr = null;
        Context context = this.mContext;
        if (context != null) {
            tmgr = (TelephonyManager) context.getSystemService("phone");
        }
        if (tmgr == null || tmgr.getCallState() == 0) {
            return false;
        }
        return true;
    }

    private boolean hasSystemPriv(int uid) {
        String[] pkgs;
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null || (pkgs = pm.getPackagesForUid(uid)) == null) {
            return false;
        }
        for (String pkg : pkgs) {
            try {
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                if (!(info == null || (info.flags & 1) == 0)) {
                    Slog.i(TAG, "system app " + pkg);
                    return true;
                }
            } catch (Exception e) {
                Slog.i(TAG, "not found app " + pkg);
            }
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean checkAudioSettingAllowed(String msg) {
        if (isInCallingState()) {
            int uid = Binder.getCallingUid();
            if (1001 == uid || 1000 == uid || hasSystemPriv(uid)) {
                return true;
            }
            return false;
        }
        Slog.w(TAG, "Audio Settings ALLOW from func=" + msg + ", from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
        return true;
    }

    /* access modifiers changed from: protected */
    public String getPackageNameByPid(int pid) {
        ActivityManager activityManager;
        List<ActivityManager.RunningAppProcessInfo> appProcesses;
        if (pid <= 0 || (activityManager = (ActivityManager) this.mContext.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG)) == null || (appProcesses = activityManager.getRunningAppProcesses()) == null) {
            return null;
        }
        String packageName = null;
        Iterator<ActivityManager.RunningAppProcessInfo> it = appProcesses.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            ActivityManager.RunningAppProcessInfo appProcess = it.next();
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
        try {
            return this.mContext.getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "getVersionName failed" + packageName);
            return "version isn't exist";
        }
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
            return packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkgName, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "getApplicationLabel exception", e);
            return null;
        }
    }

    /* access modifiers changed from: protected */
    public boolean checkEnbaleVolumeAdjust() {
        return isAdjustVolumeEnable();
    }

    /* access modifiers changed from: protected */
    public void sendCommForceBroadcast() {
        if (getMode() == 2) {
            if (1000 == Binder.getCallingUid() || "com.android.bluetooth".equals(getPackageNameByPid(Binder.getCallingPid()))) {
                try {
                    this.mContext.sendBroadcastAsUser(new Intent(ACTION_COMMFORCE), UserHandle.getUserHandleForUid(UserHandle.getCallingUserId()), PERMISSION_COMMFORCE);
                } catch (Exception e) {
                    Slog.e(TAG, "sendCommForceBroadcast exception");
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public void checkMuteRcvDelay(int curMode, int mode) {
        if (mode == 0 && (curMode == 2 || curMode == 3)) {
            AudioSystem.setParameters(DUALPA_RCV_DEALY_ON);
            sendMsg(this.mAudioHandler, MSG_DUALPA_RCV_DEALY, 0, 0, 0, null, 5000);
        } else if (curMode == mode) {
        } else {
            if (mode == 1 || mode == 2 || mode == 3) {
                AudioSystem.setParameters(DUALPA_RCV_DEALY_OFF);
            }
        }
    }

    private void setAudioSystemParameters() {
        if (SPK_RCV_STEREO_SUPPORT) {
            setSpkRcvStereoStatus();
        }
        this.mSetVbrlibFlag = false;
    }

    /* access modifiers changed from: package-private */
    public void reportAudioFlingerRestarted() {
        LogIAware.report(2101, "AudioFlinger");
    }

    /* access modifiers changed from: protected */
    public void processMediaServerRestart() {
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
        this.mSpkRcvStereoStatus = Settings.Secure.getInt(contentResolver, "stereo_landscape_portrait", 1);
    }

    private void setSpkRcvStereoStatus() {
        int i = this.mSpkRcvStereoStatus;
        if (i == 1) {
            AudioSystem.setParameters(SPK_RCV_STEREO_ON_PARA);
            AudioSystem.setParameters("rotation=0");
        } else if (i == 0) {
            AudioSystem.setParameters(SPK_RCV_STEREO_OFF_PARA);
        } else {
            Slog.e(TAG, "setSpkRcvStereoStatus Fail " + this.mSpkRcvStereoStatus);
        }
    }

    public void setRingerModeExternal(int toRingerMode, String caller) {
        boolean shouldControll = !isSystemApp(caller);
        if (shouldControll) {
            Slog.i(TAG, "AudioException setRingerModeExternal shouldReport=" + checkShouldAbortRingerModeChange(caller) + " uid:" + Binder.getCallingUid() + " caller:" + caller);
        }
        int fromRingerMode = getRingerModeExternal();
        HwAudioService.super.setRingerModeExternal(toRingerMode, caller);
        HwMediaMonitorManager.writeBigData(916600008, "SET_RINGER_MODE", caller, "2: " + String.valueOf(toRingerMode));
        if (fromRingerMode == getRingerModeInternal()) {
            Slog.d(TAG, "AudioException setRingerModeExternal ,but not change");
        } else if (shouldControll) {
            alertUserRingerModeChange(caller, fromRingerMode, toRingerMode);
        }
    }

    private void alertUserRingerModeChange(String caller, int fromRingerMode, int ringerMode) {
        sendMsg(this.mAudioHandler, 80, 2, fromRingerMode, ringerMode, new AudioExceptionMsg(HW_RING_MODE_TYPE_EXCEPTION_OCCOUR, caller, getVersionName(caller)), 0);
    }

    private boolean isFirstTimeShow(String caller) {
        String pkgs = Settings.Secure.getStringForUser(this.mContentResolver, "change_ringer_mode_pkgs", -2);
        if (TextUtils.isEmpty(pkgs)) {
            return true;
        }
        return !pkgs.contains(caller);
    }

    private void savePkgNameToDB(String caller) {
        String pkgs;
        if (!TextUtils.isEmpty(caller)) {
            String pkgs2 = Settings.Secure.getStringForUser(this.mContentResolver, "change_ringer_mode_pkgs", -2);
            if (TextUtils.isEmpty(pkgs2)) {
                pkgs = caller + "\\";
            } else {
                pkgs = pkgs2 + caller + "\\";
            }
            Settings.Secure.putStringForUser(this.mContentResolver, "change_ringer_mode_pkgs", pkgs, -2);
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
        String pkgName = getTopActivityPackageName();
        if (pkgName != null) {
            return pkgName.startsWith(caller);
        }
        Slog.e(TAG, "AudioException getTopApp ,but pkgname is null.");
        return false;
    }

    private String getTopActivityPackageName() {
        Object service = this.mContext.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG);
        if (service != null && (service instanceof ActivityManager)) {
            try {
                List<ActivityManager.RunningTaskInfo> tasks = ((ActivityManager) service).getRunningTasks(1);
                if (tasks != null) {
                    if (!tasks.isEmpty()) {
                        ComponentName topActivity = tasks.get(0).topActivity;
                        if (topActivity != null) {
                            return topActivity.getPackageName();
                        }
                    }
                }
                return null;
            } catch (SecurityException e) {
                Slog.e(TAG, "getTopActivityPackageName SecurityException");
            }
        }
        return null;
    }

    private boolean isScreenOff() {
        PowerManager power = (PowerManager) this.mContext.getSystemService("power");
        if (power != null) {
            return !power.isScreenOn();
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
            Slog.i(TAG, "AudioException not found app:" + caller);
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
            String messageStr = this.mContext.getString(33685819, callerAppName, this.mContext.getString(fromResId), this.mContext.getString(toResId));
            Slog.i(TAG, "AudioException showRingerModeToast ");
            this.mRingerModeToast.setText(messageStr);
            this.mRingerModeToast.show();
        } catch (Exception e) {
            Slog.e(TAG, "AudioException showRingerModeToast exception");
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
        Context context = this.mContext;
        String messageStr = context.getString(33685819, callerAppName, context.getString(fromResId), this.mContext.getString(toResId));
        AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext, this.mContext.getResources().getIdentifier("androidhwext:style/Theme.Emui.Dialog.Alert", null, null));
        builder.setMessage(messageStr);
        builder.setCancelable(false);
        builder.setPositiveButton(33685821, new DialogInterface.OnClickListener() {
            /* class com.android.server.audio.HwAudioService.AnonymousClass7 */

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
        if (RingerMode == 0) {
            return 33685823;
        }
        if (RingerMode == 1) {
            return 33685820;
        }
        if (RingerMode == 2) {
            return 33685822;
        }
        Slog.e(TAG, "getRingerModeStrResId RingerMode is error.RingerMode =" + RingerMode);
        return 0;
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: private */
    public static int getCurrentUserId() {
        long ident = Binder.clearCallingIdentity();
        try {
            int i = ActivityManagerNative.getDefault().getCurrentUser().id;
            Binder.restoreCallingIdentity(ident);
            return i;
        } catch (RemoteException e) {
            Slog.w(TAG, "Activity manager not running, nothing we can do assume user 0.");
            Binder.restoreCallingIdentity(ident);
            return 0;
        } catch (Throwable currentUser) {
            Binder.restoreCallingIdentity(ident);
            throw currentUser;
        }
    }

    private int getUidByPkg(String pkgName) {
        if (pkgName == null) {
            return -1;
        }
        try {
            return this.mContext.getPackageManager().getPackageUidAsUser(pkgName, getCurrentUserId());
        } catch (Exception e) {
            Slog.w(TAG, "not found uid pkgName:" + pkgName);
            return -1;
        }
    }

    /* access modifiers changed from: private */
    public void updateLowlatencyUidsMap(String pkgName, int packageCmdType) {
        Slog.i(TAG, "updateLowlatencyUidsMap " + pkgName + " packageCmdType " + packageCmdType);
        if (packageCmdType == 1) {
            int uid = getUidByPkg(pkgName);
            if (uid != -1) {
                AudioSystem.setParameters("AddLowlatencyPkg=" + uid);
            }
            this.mLowlatencyUidsMap.put(pkgName, Integer.valueOf(uid));
        } else if (packageCmdType == 2) {
            AudioSystem.setParameters("RemLowlatencyPkg=" + this.mLowlatencyUidsMap.get(pkgName));
            this.mLowlatencyUidsMap.remove(pkgName);
        }
    }

    private void initLowlatencyUidsMap() {
        String packageName;
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
                int eventType = xmlParser.next();
                if (eventType == 1) {
                    break;
                } else if (eventType == 2 && xmlParser.getName().equals(NODE_WHITEAPP) && (packageName = xmlParser.getAttributeValue(null, NODE_ATTR_PACKAGE)) != null && packageName.length() != 0) {
                    this.mWhiteAppName.add(packageName);
                }
            } catch (XmlPullParserException e4) {
                Slog.e(TAG, "XmlPullParserException");
            } catch (IOException e5) {
                Slog.e(TAG, "IOException");
            }
        }
        List<PackageInfo> packageInfo = this.mContext.getPackageManager().getInstalledPackagesAsUser(0, getCurrentUserId());
        if (packageInfo != null) {
            int packageSize = packageInfo.size();
            int whiteAppSize = this.mWhiteAppName.size();
            for (int i = 0; i < packageSize; i++) {
                String pn = packageInfo.get(i).packageName;
                for (int j = 0; j < whiteAppSize; j++) {
                    if (pn.contains(this.mWhiteAppName.get(j))) {
                        updateLowlatencyUidsMap(pn, 1);
                    }
                }
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

    /* access modifiers changed from: private */
    public boolean isLowlatencyPkg(String pkgName) {
        int whiteAppSize = this.mWhiteAppName.size();
        for (int j = 0; j < whiteAppSize; j++) {
            if (pkgName.contains(this.mWhiteAppName.get(j))) {
                return true;
            }
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        HwAudioService.super.dump(fd, pw, args);
        try {
            this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
            pw.print("HwAudioService:");
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
            pw.print("mWhiteAppName=");
            pw.println(this.mWhiteAppName);
            pw.print("mVoipOptimizeInGameMode =");
            pw.println(mVoipOptimizeInGameMode);
        } catch (SecurityException e) {
            Slog.w(TAG, "enforceCallingOrSelfPermission dump failed ");
        }
    }

    private void showDisableMicrophoneToast() {
        Toast toast = Toast.makeText(new ContextThemeWrapper(this.mContext, this.mContext.getResources().getIdentifier("androidhwext:style/Theme.Emui.Toast", null, null)), 33685911, 0);
        toast.getWindowParams().privateFlags |= 16;
        toast.show();
    }

    private void showDisableHeadphoneToast() {
        Toast toast = Toast.makeText(new ContextThemeWrapper(this.mContext, this.mContext.getResources().getIdentifier("androidhwext:style/Theme.Emui.Toast", null, null)), 33685913, 0);
        toast.getWindowParams().privateFlags |= 16;
        toast.show();
    }

    public void setWiredDeviceConnectionState(int type2, int state, String address, String name, String caller) {
        if (type2 == 33554432 || type2 == -2130706432) {
            Slog.i(TAG, "update virtual audio state" + type2);
            if (this.mContext.getPackageManager() == null) {
                Slog.e(TAG, "PackageManager is null");
            } else if (this.mContext.checkCallingOrSelfPermission("com.huawei.permission.INVOKE_VIRTUAL_AUDIO") != 0) {
                Slog.e(TAG, "No com.huawei.permission.INVOKE_VIRTUAL_AUDIO permission");
            } else {
                HwAudioService.super.setWiredDeviceConnectionState(type2, state, address, name, caller);
            }
        } else if ((-1507821540 & type2) == 0) {
            HwAudioService.super.setWiredDeviceConnectionState(type2, state, address, name, caller);
        } else if (HwDeviceManager.disallowOp(31)) {
            Slog.i(TAG, "disallow headphone by MDM");
            if (state == 1 && type2 > 0) {
                sendMsg(this.mAudioHandler, 92, 2, 0, 0, null, 0);
            }
        } else {
            this.mHwAudioServiceEx.updateTypeCNotify(type2, state, name);
            if (state == 1) {
                this.mHeadphones.add(new HeadphoneInfo(type2, address, name, caller));
            } else {
                int i = 0;
                while (true) {
                    if (i >= this.mHeadphones.size()) {
                        break;
                    } else if (this.mHeadphones.get(i).mDeviceType == type2) {
                        this.mHeadphones.remove(i);
                        break;
                    } else {
                        i++;
                    }
                }
            }
            HwAudioService.super.setWiredDeviceConnectionState(type2, state, address, name, caller);
        }
    }

    private void disableHeadPhone() {
        for (int i = 0; i < this.mHeadphones.size(); i++) {
            HeadphoneInfo info = this.mHeadphones.get(i);
            HwAudioService.super.setWiredDeviceConnectionState(info.mDeviceType, 0, info.mDeviceAddress, info.mDeviceName, info.mCaller);
        }
        this.mHeadphones.clear();
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

    /* access modifiers changed from: protected */
    public void updateAftPolicy() {
        sendMsg(this.mAudioHandler, 93, 2, 0, 0, null, 0);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0029, code lost:
        if (r0 == 0) goto L_0x0033;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x002b, code lost:
        notifyUpdateAftPolicy(r0, getMode());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0033, code lost:
        notifyUpdateAftPolicy(0, 0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:?, code lost:
        return;
     */
    private void updateAftPolicyInternal() {
        if (this.mSystemReady) {
            int ownerPid = 0;
            synchronized (this.mDeviceBroker.mSetModeLock) {
                if (this.mSetModeDeathHandlers.isEmpty()) {
                    notifyUpdateAftPolicy(0, 0);
                    return;
                }
                SetModeDeathHandler modeDeathHandler = (SetModeDeathHandler) this.mSetModeDeathHandlers.get(0);
                if (modeDeathHandler != null) {
                    ownerPid = modeDeathHandler.getPid();
                }
            }
        }
    }

    private void notifyUpdateAftPolicy(int ownerPid, int mode) {
        IHwAftPolicyService hwAft = HwAftPolicyManager.getService();
        if (hwAft != null) {
            try {
                hwAft.notifyIncallModeChange(ownerPid, mode);
            } catch (RemoteException e) {
                Slog.e(TAG, "notifyIncallModeChange RemoteException");
            }
        }
        Object hwPolicyRelatedObject = LocalServices.getService(WindowManagerPolicy.class);
        if (hwPolicyRelatedObject != null && (hwPolicyRelatedObject instanceof HwPhoneWindowManager)) {
            ((HwPhoneWindowManager) hwPolicyRelatedObject).notifyUpdateAftPolicy(ownerPid, mode);
        }
    }

    private void desktopModeChanged(int desktopMode) {
        Slog.v(TAG, "desktopModeChanged desktopMode = " + desktopMode);
        if (this.mDesktopMode != desktopMode) {
            this.mDesktopMode = desktopMode;
            SystemProperties.set(PROP_DESKTOP_MODE, "" + desktopMode);
            MediaFocusControl mediaFocusControl = this.mMediaFocusControl;
            boolean z = true;
            if (this.mDesktopMode != 1) {
                z = false;
            }
            mediaFocusControl.desktopModeChanged(z);
        }
    }

    public int trackPlayer(PlayerBase.PlayerIdCard pic) {
        pic.setPkgName(getPackageNameByPid(Binder.getCallingPid()));
        return HwAudioService.super.trackPlayer(pic);
    }

    public void playerEvent(int piid, int event) {
        HwAudioService.super.playerEvent(piid, event);
        this.mHwAudioServiceEx.notifyHiResIcon(event);
        this.mHwAudioServiceEx.notifyStartDolbyDms(event);
        recordLastPlaybackTime(piid, event);
        recoverFromExceptionMode(event);
    }

    private void recoverFromExceptionMode(int event) {
        SetModeDeathHandler hdlr;
        if (event == 2 && (hdlr = setMode3Handler()) != null) {
            List<AudioPlaybackConfiguration> list = this.mPlaybackMonitor.getActivePlaybackConfigurations(true);
            int i = 0;
            while (i < list.size()) {
                AudioPlaybackConfiguration conf = list.get(i);
                if (conf == null || conf.getClientPid() != hdlr.getPid()) {
                    i++;
                } else {
                    return;
                }
            }
            int rePid = -1;
            String recordPid = AudioSystem.getParameters("active_record_pid");
            if (recordPid != null) {
                try {
                    rePid = Integer.parseInt(recordPid);
                } catch (NumberFormatException e) {
                    Slog.v(TAG, "NumberFormatException: " + recordPid);
                }
            }
            long recoverTime = System.currentTimeMillis();
            boolean needRecoverForRecord = recoverTime - this.mLastStopAudioRecordTime > 5000;
            boolean needRecoverForPlay = recoverTime - this.mLastStopPlayBackTime > 5000;
            boolean isSetModeTimePassBy = recoverTime - this.mLastSetMode3Time > 5000;
            if (rePid != hdlr.getPid() && !isBluetoothScoOn() && needRecoverForRecord && needRecoverForPlay && isSetModeTimePassBy) {
                long token = Binder.clearCallingIdentity();
                try {
                    String pkgName = getPackageNameByPid(hdlr.getPid());
                    StringBuilder sb = new StringBuilder();
                    sb.append("mLastStopAudioRecordTime: ");
                    try {
                        sb.append(this.mLastStopAudioRecordTime);
                        sb.append(" mLastStopPlayBackTime: ");
                        sb.append(this.mLastStopPlayBackTime);
                        sb.append(" mLastSetMode3Time: ");
                        sb.append(this.mLastSetMode3Time);
                        sb.append(" pid: ");
                        sb.append(hdlr.getPid());
                        sb.append(" PkgName: ");
                        sb.append(pkgName);
                        Slog.v(TAG, sb.toString());
                        HwMediaMonitorManager.writeLogMsg(916010201, 3, (int) HW_AUDIO_MODE_TYPE_EXCEPTION_OCCOUR, "OAE6:" + hdlr.getPid() + " :" + pkgName);
                        if (pkgName != null) {
                            setModeInt(0, hdlr.getBinder(), hdlr.getPid(), pkgName);
                        } else {
                            Slog.e(TAG, "packageName is null for pid: " + hdlr.getPid());
                        }
                        Binder.restoreCallingIdentity(token);
                    } catch (Throwable th) {
                        th = th;
                        Binder.restoreCallingIdentity(token);
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    Binder.restoreCallingIdentity(token);
                    throw th;
                }
            }
        }
    }

    private SetModeDeathHandler setMode3Handler() {
        SetModeDeathHandler hdlr;
        if (this.mSetModeDeathHandlers == null) {
            return null;
        }
        try {
            Iterator iter = this.mSetModeDeathHandlers.iterator();
            while (iter.hasNext()) {
                Object object = iter.next();
                if ((object instanceof SetModeDeathHandler) && (hdlr = (SetModeDeathHandler) object) != null && hdlr.getMode() == 3) {
                    return hdlr;
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
        SetModeDeathHandler hdlr;
        if (state == 1 && (hdlr = setMode3Handler()) != null && hdlr.getPid() == pid) {
            this.mLastStopAudioRecordTime = System.currentTimeMillis();
            Slog.i(TAG, "mLastStopAudioRecordTime: " + this.mLastStopAudioRecordTime);
        }
    }

    private void recordLastPlaybackTime(int piid, int event) {
        SetModeDeathHandler hdlr;
        if (event == 4 && (hdlr = setMode3Handler()) != null) {
            List<AudioPlaybackConfiguration> list = this.mPlaybackMonitor.getActivePlaybackConfigurations(true);
            int playbackNumber = list.size();
            for (int i = 0; i < playbackNumber; i++) {
                AudioPlaybackConfiguration conf = list.get(i);
                if (conf != null && conf.getPlayerState() == 4 && conf.getClientPid() == hdlr.getPid() && conf.getPlayerInterfaceId() == piid) {
                    this.mLastStopPlayBackTime = System.currentTimeMillis();
                    Slog.i(TAG, "mLastStopPlayBackTime: " + this.mLastStopPlayBackTime);
                    return;
                }
            }
        }
    }

    public void setMode(int mode, IBinder cb, String callingPackage) {
        if (SUPPORT_GAME_MODE) {
            if (mode == 1 && this.IS_IN_GAMEMODE) {
                this.mHwAudioServiceEx.setDolbyEffect(0);
            } else if (mode == 0 && this.IS_IN_GAMEMODE) {
                this.mHwAudioServiceEx.setDolbyEffect(3);
            }
        }
        HwAudioService.super.setMode(mode, cb, callingPackage);
        HwMediaMonitorManager.writeBigData(916600008, "SET_MODE", callingPackage, "1: " + String.valueOf(mode));
        SetModeDeathHandler hdlr = setMode3Handler();
        if (hdlr != null && mode == 3 && hdlr.getBinder() == cb) {
            this.mLastSetMode3Time = System.currentTimeMillis();
            Slog.i(TAG, "mLastSetMode3Time: " + this.mLastSetMode3Time);
        }
    }

    private class IAwareDeathRecipient implements IBinder.DeathRecipient {
        private IAwareDeathRecipient() {
        }

        public void binderDied() {
            int unused = HwAudioService.this.mBindIAwareCount = 0;
            HwAudioService.this.bindIAwareSdk();
        }
    }

    private class IAwareHandler extends Handler {
        public IAwareHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                HwAudioService.this.bindIAwareSdk();
                HwAudioService.access$2608(HwAudioService.this);
            }
        }
    }

    /* access modifiers changed from: private */
    public void bindIAwareSdk() {
        IBinder iBinder = this.mIAwareSdkService;
        if (iBinder != null) {
            iBinder.unlinkToDeath(this.mIAwareDeathRecipient, 0);
        }
        Slog.i(TAG, "Intent.ACTION_BOOT_COMPLETED rigsterForegroundAppTypeWithCallback ForeAppTypeListener");
        this.mIAwareSdkService = ServiceManager.getService("IAwareSdkService");
        IBinder iBinder2 = this.mIAwareSdkService;
        if (iBinder2 != null) {
            try {
                iBinder2.linkToDeath(this.mIAwareDeathRecipient, 0);
                IAwareSdk.rigsterForegroundAppTypeWithCallback(this.mForeTypelistener);
            } catch (RemoteException e) {
                Slog.e(TAG, "IAwareSdkService linkToDeath error");
                this.mIAwareSdkService = null;
                postDelayBindIAware();
            }
        } else {
            Slog.e(TAG, "IAwareSdkService getService is null");
            postDelayBindIAware();
        }
    }

    private void postDelayBindIAware() {
        if (this.mIAwareHandler == null) {
            this.mIAwareHandler = new IAwareHandler(this.mContext.getMainLooper());
            this.mBindIAwareCount = 0;
        }
        if (this.mBindIAwareCount < 5) {
            this.mIAwareHandler.sendEmptyMessageDelayed(0, 2000);
        } else {
            IAwareSdk.rigsterForegroundAppTypeWithCallback(this.mForeTypelistener);
        }
    }

    private class ForeAppTypeListener extends IForegroundAppTypeCallback {
        public ForeAppTypeListener() {
        }

        public void reportForegroundAppType(int type, String pkg) {
            Slog.i(HwAudioService.TAG, "now type = " + HwAudioService.this.type + ",reportForegroundAppType = " + type + ",pkg = " + pkg + ",systemload = " + SystemProperties.get("sys.iaware.type", "-1") + "mVoipOptimizeInGameMode" + HwAudioService.mVoipOptimizeInGameMode);
            if (type != HwAudioService.this.type) {
                int unused = HwAudioService.this.type = type;
                SystemProperties.set("sys.iaware.type", type + "");
                if (type != -1) {
                    SystemProperties.set("sys.iaware.filteredType", type + "");
                }
            }
            if (type != 9) {
                if (HwAudioService.SUPPORT_GAME_MODE && HwAudioService.this.IS_IN_GAMEMODE) {
                    HwAudioService.this.mHwAudioServiceEx.setDolbyEffect(0);
                    boolean unused2 = HwAudioService.this.IS_IN_GAMEMODE = false;
                    AudioSystem.setParameters("dolby_game_mode=off");
                }
                if (HwAudioService.mVoipOptimizeInGameMode) {
                    AudioSystem.setParameters("game_mode=off");
                    return;
                }
                return;
            }
            if (HwAudioService.SUPPORT_GAME_MODE && !HwAudioService.this.IS_IN_GAMEMODE && !AudioSystem.isStreamActive(3, 0)) {
                Slog.i(HwAudioService.TAG, "Music steam not start");
                if (HwAudioService.this.mHwAudioServiceEx.setDolbyEffect(3)) {
                    boolean unused3 = HwAudioService.this.IS_IN_GAMEMODE = true;
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

    /* access modifiers changed from: protected */
    public void updateDefaultStream() {
        ContentResolver contentResolver = this.mContentResolver;
        if (contentResolver == null) {
            Slog.i(TAG, "mContentResolver is null.");
            return;
        }
        int streamType = Settings.System.getIntForUser(contentResolver, "default_volume_key_control", 1, -2);
        Slog.i(TAG, "get default stream : " + streamType);
        if (streamType == 2) {
            this.mDefaultVolStream = 2;
        } else {
            this.mDefaultVolStream = 3;
        }
    }

    /* access modifiers changed from: protected */
    public boolean shouldStopAdjustVolume(int uid) {
        IHwSideStatusManager sideStatusManager;
        boolean shouldStopAdjustVolume;
        if (!SUPPORT_SIDE_TOUCH) {
            return false;
        }
        if (uid != 1000) {
            Slog.d(TAG, "volumetrigger not system uid : " + uid);
            return false;
        }
        Context context = this.mContext;
        if (context == null || (sideStatusManager = HwFrameworkFactory.getHwSideStatusManager(context)) == null || !(shouldStopAdjustVolume = sideStatusManager.isVolumeTriggered())) {
            return false;
        }
        Slog.d(TAG, "volumetrigger shouldStopAdjustVolume is: " + shouldStopAdjustVolume);
        sideStatusManager.resetVolumeTriggerStatus();
        return true;
    }

    /* access modifiers changed from: private */
    public void startAIWakeupServiceReport(String wakeup_info) {
        Intent intent = new Intent(AIACTION_WAKEUP_INFO);
        intent.setPackage(getTargetPackageForWakeup());
        intent.putExtra(DEV_PATH_SMART_HOLDER_VAR_INFO, wakeup_info);
        try {
            this.mContext.startService(intent);
        } catch (IllegalStateException | SecurityException e) {
            Slog.w(TAG, "startAIWakeupServiceReport failed");
        }
    }

    private void addPowerSaveTempWhitelistApp(String packageName) {
        DeviceIdleController.LocalService idleController = (DeviceIdleController.LocalService) LocalServices.getService(DeviceIdleController.LocalService.class);
        if (idleController != null) {
            Slog.i(TAG, "addPowerSaveTempWhitelistApp#va");
            idleController.addPowerSaveTempWhitelistApp(Process.myUid(), packageName, 10000, getCurrentUserId(), false, "hivoice");
        }
    }

    /* access modifiers changed from: private */
    public void startAIWakeupService(String wakeup_info) {
        addPowerSaveTempWhitelistApp(getTargetPackageForWakeup());
        Intent intent = new Intent(AIACTION_WAKEUP_SERVICE);
        intent.setPackage(getTargetPackageForWakeup());
        intent.putExtra(DEV_PATH_SMART_HOLDER_VAR_INFO, wakeup_info);
        try {
            this.mContext.startService(intent);
            Slog.i(TAG, "Hal Audio Wakeup2");
        } catch (IllegalStateException | SecurityException e) {
            Slog.w(TAG, "startAIWakeupService failed");
        }
    }

    private static boolean isOversea() {
        return !"CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    }

    public void adjustStreamVolume(int streamType, int direction, int flags, String callingPackage) {
        HwAudioService.super.adjustStreamVolume(streamType, direction, flags, callingPackage);
        HwMediaMonitorManager.writeBigData(916600001, "ADJUST_STREAM_VOLUME", callingPackage, "3: " + String.valueOf(streamType) + ", " + String.valueOf(direction) + ", " + String.valueOf(flags), streamType);
    }

    /* access modifiers changed from: protected */
    public void adjustSuggestedStreamVolume(int direction, int suggestedStreamType, int flags, String callingPackage, String caller, int uid) {
        HwAudioService.super.adjustSuggestedStreamVolume(direction, suggestedStreamType, flags, callingPackage, caller, uid);
        HwMediaMonitorManager.writeBigData(916600001, "ADJUST_SUGGESTED_STREAM_VOLUME", callingPackage, "4: " + String.valueOf(direction) + ", " + String.valueOf(suggestedStreamType) + ", " + String.valueOf(flags), suggestedStreamType);
    }

    public void setStreamVolume(int streamType, int index, int flags, String callingPackage) {
        HwAudioService.super.setStreamVolume(streamType, index, flags, callingPackage);
        HwMediaMonitorManager.writeBigData(916600001, "SET_STREAM_VOLUME", callingPackage, "1: " + String.valueOf(streamType) + ", " + String.valueOf(index) + ", " + String.valueOf(flags), streamType);
    }

    public void setVolumeIndexForAttributes(AudioAttributes attr, int index, int flags, String callingPackage) {
        HwAudioService.super.setVolumeIndexForAttributes(attr, index, flags, callingPackage);
        String param = "2:" + String.valueOf(index) + ", " + String.valueOf(flags);
        AudioVolumeGroup avg = getAudioVolumeGroupById(getVolumeGroupIdForAttributes(attr));
        if (avg == null) {
            Slog.w(TAG, "avg == null, failed to setVolumeIndexForAttributes");
            return;
        }
        for (int groupedStream : avg.getLegacyStreamTypes()) {
            HwMediaMonitorManager.writeBigData(916600001, "SET_VOLUME_INDEX_FOR_ATTR", callingPackage, param, groupedStream);
        }
    }

    public void setMasterMute(boolean mute, int flags, String callingPackage, int userId) {
        HwAudioService.super.setMasterMute(mute, flags, callingPackage, userId);
        HwMediaMonitorManager.writeBigData(916600008, "SET_MASTER_MUTE", callingPackage, "3: " + String.valueOf(mute) + ", " + String.valueOf(flags));
    }

    public void setSpeakerphoneOn(boolean on) {
        HwAudioService.super.setSpeakerphoneOn(on);
        readyForWriteBigData(916600007, "SET_SPEAKERPHONE_ON", "4: " + String.valueOf(on));
    }

    public void startBluetoothSco(IBinder cb, int targetSdkVersion) {
        HwAudioService.super.startBluetoothSco(cb, targetSdkVersion);
        readyForWriteBigData(916600005, "START_BLUETOOTH_SCO", "1: start");
    }

    public void startBluetoothScoVirtualCall(IBinder cb) {
        HwAudioService.super.startBluetoothScoVirtualCall(cb);
        readyForWriteBigData(916600005, "START_BLUETOOTH_SCO_VIRTUAL", "3: start");
    }

    public void stopBluetoothSco(IBinder cb) {
        HwAudioService.super.stopBluetoothSco(cb);
        readyForWriteBigData(916600005, "STOP_BLUETOOTH_SCO", "2: stop");
    }

    public void setBluetoothScoOn(boolean on) {
        HwAudioService.super.setBluetoothScoOn(on);
        readyForWriteBigData(916600005, "SET_BLUETOOTH_SCO_ON", "4: " + String.valueOf(on));
    }

    public void setBluetoothA2dpDeviceConnectionStateSuppressNoisyIntent(BluetoothDevice device, int state, int profile, boolean suppressNoisyIntent, int a2dpVolume) {
        HwAudioService.super.setBluetoothA2dpDeviceConnectionStateSuppressNoisyIntent(device, state, profile, suppressNoisyIntent, a2dpVolume);
        readyForWriteBigData(916600004, "SET_A2DP_CONNECTION_NOISY", "1: " + String.valueOf(state) + ", " + String.valueOf(suppressNoisyIntent) + ", " + String.valueOf(a2dpVolume));
    }

    public void handleBluetoothA2dpDeviceConfigChange(BluetoothDevice device) {
        HwAudioService.super.handleBluetoothA2dpDeviceConfigChange(device);
        readyForWriteBigData(916600004, "HANDLE_A2DP_DEVICE_CHANGE", "2: handle");
    }

    private void readyForWriteBigData(int eventId, String subType, String param) {
        String pkgName = getPackageNameByPid(Binder.getCallingPid());
        if (pkgName == null) {
            pkgName = "null";
            Slog.w(TAG, "could not get pkgName for big data");
        }
        HwMediaMonitorManager.writeBigData(eventId, subType, pkgName, param);
    }

    private boolean isTv() {
        boolean isTv = "tv".equals(getProductType());
        Slog.d(TAG, "isTv = " + isTv);
        return isTv;
    }

    private boolean isTablet() {
        boolean isTablet = "tablet".equals(getProductType());
        Slog.d(TAG, "isTablet = " + isTablet);
        return isTablet;
    }

    private String getProductType() {
        return SystemProperties.get("ro.build.characteristics", MemoryConstant.MEM_SCENE_DEFAULT);
    }

    private String getTargetPackageForWakeup() {
        if (isTv()) {
            return "com.huawei.hiai";
        }
        return isTablet() ? AIVASSISTANT_PACKAGE_NAME : AIVASSISTANT_PACKAGE_NAME;
    }
}
