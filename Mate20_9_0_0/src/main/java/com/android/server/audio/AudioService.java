package com.android.server.audio;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManagerInternal;
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IUidObserver;
import android.app.IUidObserver.Stub;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.app.PendingIntent;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothHearingAid;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.content.res.Resources.NotFoundException;
import android.content.res.XmlResourceParser;
import android.database.ContentObserver;
import android.hardware.hdmi.HdmiControlManager;
import android.hardware.hdmi.HdmiPlaybackClient;
import android.hardware.hdmi.HdmiPlaybackClient.DisplayStatusCallback;
import android.hardware.hdmi.HdmiTvClient;
import android.media.AudioAttributes;
import android.media.AudioAttributes.Builder;
import android.media.AudioDevicePort;
import android.media.AudioFocusInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioManagerInternal;
import android.media.AudioManagerInternal.RingerModeDelegate;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioPort;
import android.media.AudioRecordingConfiguration;
import android.media.AudioRoutesInfo;
import android.media.AudioSystem;
import android.media.AudioSystem.DynamicPolicyCallback;
import android.media.AudioSystem.ErrorCallback;
import android.media.HwMediaMonitorManager;
import android.media.IAudioFocusDispatcher;
import android.media.IAudioRoutesObserver;
import android.media.IAudioServerStateDispatcher;
import android.media.IPlaybackConfigDispatcher;
import android.media.IRecordingConfigDispatcher;
import android.media.IRingtonePlayer;
import android.media.IVolumeController;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.PlayerBase.PlayerIdCard;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.media.VolumePolicy;
import android.media.audiopolicy.AudioMix;
import android.media.audiopolicy.AudioPolicyConfig;
import android.media.audiopolicy.IAudioPolicyCallback;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManagerInternal;
import android.os.UserManagerInternal.UserRestrictionsListener;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.service.notification.ZenModeConfig;
import android.service.vr.IVrManager;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.AndroidRuntimeException;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IntArray;
import android.util.Jlog;
import android.util.Log;
import android.util.MathUtils;
import android.util.Slog;
import android.util.SparseIntArray;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityManager.AccessibilityServicesStateChangeListener;
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener;
import android.widget.Toast;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.HwBootAnimationOeminfo;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.XmlUtils;
import com.android.server.EventLogTags;
import com.android.server.HwServiceExFactory;
import com.android.server.HwServiceFactory;
import com.android.server.HwServiceFactory.IHwAudioService;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.audio.AudioEventLogger.StringEvent;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerService;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import com.android.server.utils.PriorityDump;
import com.huawei.android.audio.IHwAudioServiceManager;
import huawei.android.security.IHwBehaviorCollectManager;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import huawei.cust.HwCustUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.xmlpull.v1.XmlPullParserException;

public class AudioService extends AbsAudioService implements TouchExplorationStateChangeListener, AccessibilityServicesStateChangeListener, IHwAudioServiceInner {
    private static final String ACTION_CHECK_MUSIC_ACTIVE = "ACTION_CHECK_MUSIC_ACTIVE";
    private static final String ASSET_FILE_VERSION = "1.0";
    private static final String ATTR_ASSET_FILE = "file";
    private static final String ATTR_ASSET_ID = "id";
    private static final String ATTR_GROUP_NAME = "name";
    private static final String ATTR_VERSION = "version";
    private static final int BTA2DP_DOCK_TIMEOUT_MILLIS = 8000;
    private static final int BT_HEADSET_CNCT_TIMEOUT_MS = 3000;
    private static final int BT_HEARING_AID_GAIN_MIN = -128;
    private static final int CHECK_MUSIC_ACTIVE_DELAY_MS = 3000;
    private static final int CHINAZONE_IDENTIFIER = 156;
    public static final String CONNECT_INTENT_KEY_ADDRESS = "address";
    public static final String CONNECT_INTENT_KEY_DEVICE_CLASS = "class";
    public static final String CONNECT_INTENT_KEY_HAS_CAPTURE = "hasCapture";
    public static final String CONNECT_INTENT_KEY_HAS_MIDI = "hasMIDI";
    public static final String CONNECT_INTENT_KEY_HAS_PLAYBACK = "hasPlayback";
    public static final String CONNECT_INTENT_KEY_PORT_NAME = "portName";
    public static final String CONNECT_INTENT_KEY_STATE = "state";
    protected static final boolean DEBUG_AP = true;
    protected static final boolean DEBUG_DEVICES = true;
    protected static final boolean DEBUG_MODE = true;
    protected static final boolean DEBUG_VOL = true;
    private static final int DEFAULT_STREAM_TYPE_OVERRIDE_DELAY_MS = 0;
    protected static final int DEFAULT_VOL_STREAM_NO_PLAYBACK = 3;
    private static final int DEVICE_MEDIA_UNMUTED_ON_PLUG = 604137356;
    private static final int DEVICE_OVERRIDE_A2DP_ROUTE_ON_PLUG = 604135436;
    private static final int FLAG_ADJUST_VOLUME = 1;
    private static final int FLAG_PERSIST_VOLUME = 2;
    private static final String GROUP_TOUCH_SOUNDS = "touch_sounds";
    private static final int INDICATE_SYSTEM_READY_RETRY_DELAY_MS = 1000;
    protected static int[] MAX_STREAM_VOLUME = new int[]{5, 7, 7, 15, 7, 7, 15, 7, 15, 15, 15};
    protected static int[] MIN_STREAM_VOLUME = new int[]{1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1};
    private static final int MSG_A2DP_DEVICE_CONFIG_CHANGE = 103;
    private static final int MSG_ACCESSORY_PLUG_MEDIA_UNMUTE = 27;
    private static final int MSG_AUDIO_SERVER_DIED = 4;
    private static final int MSG_BROADCAST_AUDIO_BECOMING_NOISY = 15;
    private static final int MSG_BROADCAST_BT_CONNECTION_STATE = 19;
    private static final int MSG_BTA2DP_DOCK_TIMEOUT = 106;
    private static final int MSG_BT_HEADSET_CNCT_FAILED = 9;
    private static final int MSG_CHECK_MUSIC_ACTIVE = 14;
    private static final int MSG_CONFIGURE_SAFE_MEDIA_VOLUME = 16;
    private static final int MSG_CONFIGURE_SAFE_MEDIA_VOLUME_FORCED = 17;
    private static final int MSG_DISABLE_AUDIO_FOR_UID = 104;
    private static final int MSG_DISPATCH_AUDIO_SERVER_STATE = 29;
    private static final int MSG_DYN_POLICY_MIX_STATE_UPDATE = 25;
    private static final int MSG_ENABLE_SURROUND_FORMATS = 30;
    private static final int MSG_INDICATE_SYSTEM_READY = 26;
    private static final int MSG_LOAD_SOUND_EFFECTS = 7;
    private static final int MSG_NOTIFY_VOL_EVENT = 28;
    private static final int MSG_PERSIST_MUSIC_ACTIVE_MS = 22;
    private static final int MSG_PERSIST_RINGER_MODE = 3;
    private static final int MSG_PERSIST_SAFE_VOLUME_STATE = 18;
    private static final int MSG_PERSIST_VOLUME = 1;
    private static final int MSG_PLAY_SOUND_EFFECT = 5;
    private static final int MSG_REPORT_NEW_ROUTES = 12;
    private static final int MSG_SET_A2DP_SINK_CONNECTION_STATE = 102;
    private static final int MSG_SET_A2DP_SRC_CONNECTION_STATE = 101;
    private static final int MSG_SET_ALL_VOLUMES = 10;
    private static final int MSG_SET_DEVICE_VOLUME = 0;
    private static final int MSG_SET_FORCE_BT_A2DP_USE = 13;
    protected static final int MSG_SET_FORCE_USE = 8;
    private static final int MSG_SET_HEARING_AID_CONNECTION_STATE = 105;
    private static final int MSG_SET_WIRED_DEVICE_CONNECTION_STATE = 100;
    private static final int MSG_SYSTEM_READY = 21;
    private static final int MSG_UNLOAD_SOUND_EFFECTS = 20;
    private static final int MSG_UNMUTE_STREAM = 24;
    private static final int MUSIC_ACTIVE_POLL_PERIOD_MS = 60000;
    private static final int NUM_SOUNDPOOL_CHANNELS = 4;
    protected static final int PERSIST_DELAY = 500;
    private static final String[] RINGER_MODE_NAMES = new String[]{"SILENT", "VIBRATE", PriorityDump.PRIORITY_ARG_NORMAL};
    private static final int SAFE_MEDIA_VOLUME_ACTIVE = 3;
    private static final int SAFE_MEDIA_VOLUME_DISABLED = 1;
    private static final int SAFE_MEDIA_VOLUME_INACTIVE = 2;
    private static final int SAFE_MEDIA_VOLUME_NOT_CONFIGURED = 0;
    private static final int SAFE_VOLUME_CONFIGURE_TIMEOUT_MS = 30000;
    private static final int SCO_MODE_MAX = 2;
    private static final int SCO_MODE_RAW = 1;
    private static final int SCO_MODE_UNDEFINED = -1;
    private static final int SCO_MODE_VIRTUAL_CALL = 0;
    private static final int SCO_MODE_VR = 2;
    private static final int SCO_STATE_ACTIVATE_REQ = 1;
    private static final int SCO_STATE_ACTIVE_EXTERNAL = 2;
    private static final int SCO_STATE_ACTIVE_INTERNAL = 3;
    private static final int SCO_STATE_DEACTIVATE_REQ = 4;
    private static final int SCO_STATE_DEACTIVATING = 5;
    private static final int SCO_STATE_INACTIVE = 0;
    protected static final int SENDMSG_NOOP = 1;
    protected static final int SENDMSG_QUEUE = 2;
    protected static final int SENDMSG_REPLACE = 0;
    private static final int SOUND_EFFECTS_LOAD_TIMEOUT_MS = 5000;
    private static final String SOUND_EFFECTS_PATH = "/media/audio/ui/";
    private static final List<String> SOUND_EFFECT_FILES = new ArrayList();
    private static final int[] STREAM_VOLUME_OPS = new int[]{34, 36, 35, 36, 37, 38, 39, 36, 36, 36, 64};
    private static final String TAG = "AudioService";
    private static final String TAG_ASSET = "asset";
    private static final String TAG_AUDIO_ASSETS = "audio_assets";
    private static final String TAG_GROUP = "group";
    private static final int TOUCH_EXPLORE_STREAM_TYPE_OVERRIDE_DELAY_MS = 1000;
    private static final int UNMUTE_STREAM_DELAY = 350;
    private static final int UNSAFE_VOLUME_MUSIC_ACTIVE_MS_MAX = SystemProperties.getInt("ro.config.hw.security_test", 72000000);
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new Builder().setContentType(4).setUsage(13).build();
    private static Long mLastDeviceConnectMsgTime = new Long(0);
    protected static int[] mStreamVolumeAlias;
    private static boolean sIndependentA11yVolume = false;
    private static int sSoundEffectVolumeDb;
    private static int sStreamOverrideDelayMs;
    final int LOG_NB_EVENTS_DYN_POLICY = 10;
    final int LOG_NB_EVENTS_FORCE_USE = 20;
    final int LOG_NB_EVENTS_PHONE_STATE = 20;
    final int LOG_NB_EVENTS_VOLUME = 40;
    final int LOG_NB_EVENTS_WIRED_DEV_CONNECTION = 30;
    private final int[][] SOUND_EFFECT_FILES_MAP = ((int[][]) Array.newInstance(int.class, new int[]{10, 2}));
    private final int[] STREAM_VOLUME_ALIAS_DEFAULT = new int[]{0, 2, 2, 3, 4, 2, 6, 2, 2, 3, 3};
    private final int[] STREAM_VOLUME_ALIAS_TELEVISION = new int[]{3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3};
    private final int[] STREAM_VOLUME_ALIAS_VOICE = new int[]{0, 2, 2, 3, 4, 2, 6, 2, 2, 3, 3};
    private BluetoothA2dp mA2dp;
    private final Object mA2dpAvrcpLock = new Object();
    private int[] mAccessibilityServiceUids;
    private final Object mAccessibilityServiceUidsLock = new Object();
    private final ActivityManagerInternal mActivityManagerInternal;
    private AlarmManager mAlarmManager = null;
    private final AppOpsManager mAppOps;
    private WakeLock mAudioEventWakeLock;
    protected AudioHandler mAudioHandler;
    private final HashMap<IBinder, AudioPolicyProxy> mAudioPolicies = new HashMap();
    @GuardedBy("mAudioPolicies")
    private int mAudioPolicyCounter = 0;
    private HashMap<IBinder, AsdProxy> mAudioServerStateListeners = new HashMap();
    private final ErrorCallback mAudioSystemCallback = new ErrorCallback() {
        public void onError(int error) {
            AudioService.this.onErrorCallBackEx(error);
            if (error == 100) {
                AudioService.sendMsg(AudioService.this.mAudioHandler, 4, 1, 0, 0, null, 0);
                AudioService.sendMsg(AudioService.this.mAudioHandler, 29, 2, 0, 0, null, 0);
            }
        }
    };
    private AudioSystemThread mAudioSystemThread;
    private boolean mAvrcpAbsVolSupported = false;
    int mBecomingNoisyIntentDevices = 738361228;
    private boolean mBluetoothA2dpEnabled;
    private final Object mBluetoothA2dpEnabledLock = new Object();
    protected BluetoothHeadset mBluetoothHeadset;
    private BluetoothDevice mBluetoothHeadsetDevice;
    private ServiceListener mBluetoothProfileServiceListener = new ServiceListener() {
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            int i = profile;
            BluetoothProfile bluetoothProfile = proxy;
            String str = AudioService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onServiceConnected profile=");
            stringBuilder.append(i);
            Log.d(str, stringBuilder.toString());
            if (i != 11) {
                int i2 = 1;
                List<BluetoothDevice> deviceList;
                BluetoothDevice btDevice;
                int state;
                if (i != 21) {
                    switch (i) {
                        case 1:
                            synchronized (AudioService.this.mScoClients) {
                                AudioService.this.mAudioHandler.removeMessages(9);
                                AudioService.this.mBluetoothHeadset = (BluetoothHeadset) bluetoothProfile;
                                AudioService.this.setBtScoActiveDevice(AudioService.this.mBluetoothHeadset.getActiveDevice());
                                AudioService.this.checkScoAudioState();
                                if (AudioService.this.mScoAudioState == 1 || AudioService.this.mScoAudioState == 4) {
                                    boolean status = false;
                                    if (AudioService.this.mBluetoothHeadsetDevice != null) {
                                        int access$2500 = AudioService.this.mScoAudioState;
                                        if (access$2500 == 1) {
                                            status = AudioService.connectBluetoothScoAudioHelper(AudioService.this.mBluetoothHeadset, AudioService.this.mBluetoothHeadsetDevice, AudioService.this.mScoAudioMode);
                                            if (status) {
                                                AudioService.this.mScoAudioState = 3;
                                            }
                                        } else if (access$2500 != 4) {
                                            Log.i(AudioService.TAG, "resolve findbugs");
                                        } else {
                                            status = AudioService.disconnectBluetoothScoAudioHelper(AudioService.this.mBluetoothHeadset, AudioService.this.mBluetoothHeadsetDevice, AudioService.this.mScoAudioMode);
                                            if (status) {
                                                AudioService.this.mScoAudioState = 5;
                                            }
                                        }
                                    }
                                    if (!status) {
                                        AudioService.this.mScoAudioState = 0;
                                        AudioService.this.broadcastScoConnectionState(0);
                                    }
                                }
                            }
                            return;
                        case 2:
                            synchronized (AudioService.this.mConnectedDevices) {
                                synchronized (AudioService.this.mA2dpAvrcpLock) {
                                    AudioService.this.mA2dp = (BluetoothA2dp) bluetoothProfile;
                                    deviceList = AudioService.this.mA2dp.getConnectedDevices();
                                    if (deviceList.size() > 0) {
                                        btDevice = (BluetoothDevice) deviceList.get(0);
                                        state = AudioService.this.mA2dp.getConnectionState(btDevice);
                                        if (state != 2) {
                                            i2 = 0;
                                        }
                                        AudioService.this.queueMsgUnderWakeLock(AudioService.this.mAudioHandler, 102, state, -1, btDevice, AudioService.this.checkSendBecomingNoisyIntent(128, i2, 0));
                                    }
                                }
                            }
                            return;
                        default:
                            return;
                    }
                }
                synchronized (AudioService.this.mConnectedDevices) {
                    synchronized (AudioService.this.mHearingAidLock) {
                        AudioService.this.mHearingAid = (BluetoothHearingAid) bluetoothProfile;
                        deviceList = AudioService.this.mHearingAid.getConnectedDevices();
                        if (deviceList.size() > 0) {
                            btDevice = (BluetoothDevice) deviceList.get(0);
                            state = AudioService.this.mHearingAid.getConnectionState(btDevice);
                            if (state != 2) {
                                i2 = 0;
                            }
                            int i3 = state;
                            AudioService.this.queueMsgUnderWakeLock(AudioService.this.mAudioHandler, 105, state, 0, btDevice, AudioService.this.checkSendBecomingNoisyIntent(134217728, i2, 0));
                        }
                    }
                }
                return;
            }
            List<BluetoothDevice> deviceList2 = proxy.getConnectedDevices();
            if (deviceList2.size() > 0) {
                BluetoothDevice btDevice2 = (BluetoothDevice) deviceList2.get(0);
                synchronized (AudioService.this.mConnectedDevices) {
                    AudioService.this.queueMsgUnderWakeLock(AudioService.this.mAudioHandler, 101, bluetoothProfile.getConnectionState(btDevice2), 0, btDevice2, 0);
                }
            }
        }

        public void onServiceDisconnected(int profile) {
            String str = AudioService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onServiceDisconnected profile=");
            stringBuilder.append(profile);
            Log.d(str, stringBuilder.toString());
            if (profile == 11) {
                AudioService.this.disconnectA2dpSink();
            } else if (profile != 21) {
                switch (profile) {
                    case 1:
                        AudioService.this.disconnectHeadset();
                        return;
                    case 2:
                        AudioService.this.disconnectA2dp();
                        return;
                    default:
                        return;
                }
            } else {
                AudioService.this.disconnectHearingAid();
            }
        }
    };
    @GuardedBy("mSettingsLock")
    private boolean mCameraSoundForced;
    protected final ArrayMap<String, DeviceListSpec> mConnectedDevices = new ArrayMap();
    private final ContentResolver mContentResolver;
    private final Context mContext;
    final AudioRoutesInfo mCurAudioRoutes = new AudioRoutesInfo();
    private HwCustAudioService mCust = null;
    private String mDockAddress;
    private boolean mDockAudioMediaEnabled = true;
    private int mDockState = 0;
    private final DynamicPolicyCallback mDynPolicyCallback = new DynamicPolicyCallback() {
        public void onDynamicPolicyMixStateUpdate(String regId, int state) {
            if (!TextUtils.isEmpty(regId)) {
                AudioService.sendMsg(AudioService.this.mAudioHandler, 25, 2, state, 0, regId, 0);
            }
        }
    };
    private final AudioEventLogger mDynPolicyLogger = new AudioEventLogger(10, "dynamic policy events (logged when command received by AudioService)");
    private String mEnabledSurroundFormats;
    private int mEncodedSurroundMode;
    private IAudioPolicyCallback mExtVolumeController;
    private final Object mExtVolumeControllerLock = new Object();
    private boolean mFactoryMode = "factory".equals(SystemProperties.get("ro.runmode", "normal"));
    int mFixedVolumeDevices = 2889728;
    private ForceControlStreamClient mForceControlStreamClient = null;
    private final Object mForceControlStreamLock = new Object();
    private final AudioEventLogger mForceUseLogger = new AudioEventLogger(20, "force use (logged before setForceUse() is executed)");
    private int mForcedUseForComm;
    private int mForcedUseForCommExt;
    int mFullVolumeDevices = 0;
    private boolean mHasAlarm = false;
    private final boolean mHasVibrator;
    private boolean mHdmiCecSink;
    private MyDisplayStatusCallback mHdmiDisplayStatusCallback = new MyDisplayStatusCallback(this, null);
    private HdmiControlManager mHdmiManager;
    private HdmiPlaybackClient mHdmiPlaybackClient;
    private boolean mHdmiSystemAudioSupported = false;
    private HdmiTvClient mHdmiTvClient;
    private BluetoothHearingAid mHearingAid;
    private final Object mHearingAidLock = new Object();
    protected IHwAudioServiceEx mHwAudioServiceEx = null;
    private IHwBehaviorCollectManager mHwBehaviorManager;
    HwInnerAudioService mHwInnerService = new HwInnerAudioService(this);
    private boolean mIsChineseZone = true;
    private boolean mIsHisiPlatform = false;
    private final boolean mIsSingleVolume;
    private KeyguardManager mKeyguardManager;
    private long mLoweredFromNormalToVibrateTime;
    private int mMcc = 0;
    protected final MediaFocusControl mMediaFocusControl;
    private int mMode = 0;
    private final AudioEventLogger mModeLogger = new AudioEventLogger(20, "phone state (logged after successfull call to AudioSystem.setPhoneState(int))");
    private final boolean mMonitorRotation;
    private int mMusicActiveMs;
    private int mMuteAffectedStreams;
    private NotificationManager mNm;
    private PendingIntent mPendingIntent = null;
    private StreamVolumeCommand mPendingVolumeCommand;
    private final int mPlatformType;
    protected final PlaybackActivityMonitor mPlaybackMonitor;
    private int mPrevVolDirection = 0;
    private final BroadcastReceiver mReceiver = new AudioServiceBroadcastReceiver(this, null);
    private final RecordingActivityMonitor mRecordMonitor;
    private int mRingerAndZenModeMutedStreams;
    @GuardedBy("mSettingsLock")
    private int mRingerMode;
    private int mRingerModeAffectedStreams = 0;
    private RingerModeDelegate mRingerModeDelegate;
    @GuardedBy("mSettingsLock")
    private int mRingerModeExternal = -1;
    private volatile IRingtonePlayer mRingtonePlayer;
    private ArrayList<RmtSbmxFullVolDeathHandler> mRmtSbmxFullVolDeathHandlers = new ArrayList();
    private int mRmtSbmxFullVolRefCount = 0;
    final RemoteCallbackList<IAudioRoutesObserver> mRoutesObservers = new RemoteCallbackList();
    private final int mSafeMediaVolumeDevices = 603979788;
    private int mSafeMediaVolumeIndex;
    private Integer mSafeMediaVolumeState;
    private float mSafeUsbMediaVolumeDbfs;
    private int mSafeUsbMediaVolumeIndex;
    private String mSafeVolumeCaller = null;
    private int mScoAudioMode;
    private int mScoAudioState;
    private final ArrayList<ScoClient> mScoClients = new ArrayList();
    private int mScoConnectionState;
    private boolean mScreenOn = true;
    protected final ArrayList<SetModeDeathHandler> mSetModeDeathHandlers = new ArrayList();
    private final Object mSettingsLock = new Object();
    private SettingsObserver mSettingsObserver;
    private final Object mSoundEffectsLock = new Object();
    private SoundPool mSoundPool;
    private SoundPoolCallback mSoundPoolCallBack;
    private SoundPoolListenerThread mSoundPoolListenerThread;
    private Looper mSoundPoolLooper = null;
    private VolumeStreamState[] mStreamStates;
    private boolean mSurroundModeChanged;
    protected boolean mSystemReady;
    private final IUidObserver mUidObserver = new Stub() {
        public void onUidStateChanged(int uid, int procState, long procStateSeq) {
        }

        public void onUidGone(int uid, boolean disabled) {
            disableAudioForUid(false, uid);
        }

        public void onUidActive(int uid) throws RemoteException {
        }

        public void onUidIdle(int uid, boolean disabled) {
        }

        public void onUidCachedChanged(int uid, boolean cached) {
            disableAudioForUid(cached, uid);
        }

        private void disableAudioForUid(boolean disable, int uid) {
            AudioService.this.queueMsgUnderWakeLock(AudioService.this.mAudioHandler, 104, disable, uid, null, 0);
        }
    };
    private final boolean mUseFixedVolume;
    private final UserManagerInternal mUserManagerInternal;
    private final UserRestrictionsListener mUserRestrictionsListener = new AudioServiceUserRestrictionsListener(this, null);
    private boolean mUserSelectedVolumeControlStream = false;
    private boolean mUserSwitchedReceived;
    private int mVibrateSetting;
    private Vibrator mVibrator;
    private int mVolumeControlStream = -1;
    private final VolumeController mVolumeController = new VolumeController();
    private final AudioEventLogger mVolumeLogger = new AudioEventLogger(40, "volume changes (logged when command received by AudioService)");
    private VolumePolicy mVolumePolicy = VolumePolicy.DEFAULT;
    private final AudioEventLogger mWiredDevLogger = new AudioEventLogger(30, "wired device connection (logged before onSetWiredDeviceConnectionState() is executed)");
    private int mZenModeAffectedStreams = 0;

    private class AsdProxy implements DeathRecipient {
        private final IAudioServerStateDispatcher mAsd;

        AsdProxy(IAudioServerStateDispatcher asd) {
            this.mAsd = asd;
        }

        public void binderDied() {
            synchronized (AudioService.this.mAudioServerStateListeners) {
                AudioService.this.mAudioServerStateListeners.remove(this.mAsd.asBinder());
            }
        }

        IAudioServerStateDispatcher callback() {
            return this.mAsd;
        }
    }

    private class AudioHandler extends Handler {
        private AudioHandler() {
        }

        /* synthetic */ AudioHandler(AudioService x0, AnonymousClass1 x1) {
            this();
        }

        private void setAllVolumes(VolumeStreamState streamState) {
            streamState.applyAllVolumes();
            int streamType = AudioSystem.getNumStreamTypes() - 1;
            while (streamType >= 0) {
                if (streamType != streamState.mStreamType && AudioService.mStreamVolumeAlias[streamType] == streamState.mStreamType) {
                    AudioService.this.mStreamStates[streamType].applyAllVolumes();
                }
                streamType--;
            }
        }

        private void persistVolume(VolumeStreamState streamState, int device) {
            if (!AudioService.this.checkEnbaleVolumeAdjust() || AudioService.this.mUseFixedVolume) {
                return;
            }
            if (!AudioService.this.mIsSingleVolume || streamState.mStreamType == 3) {
                if (streamState.hasValidSettingsName()) {
                    System.putIntForUser(AudioService.this.mContentResolver, streamState.getSettingNameForDevice(device), (streamState.getIndex(device) + 5) / 10, -2);
                }
                if (2 == streamState.mStreamType && 2 == device) {
                    HwBootAnimationOeminfo.setBootAnimRing((streamState.getIndex(device) + 5) / 10);
                }
            }
        }

        private void persistRingerMode(int ringerMode) {
            if (AudioService.this.checkEnbaleVolumeAdjust() && !AudioService.this.mUseFixedVolume) {
                if (2 == ringerMode) {
                    Log.i(AudioService.TAG, "set 1 to ringermode");
                    HwBootAnimationOeminfo.setBootAnimRingMode(1);
                } else {
                    Log.i(AudioService.TAG, "set 0 to ringermode");
                    HwBootAnimationOeminfo.setBootAnimRingMode(0);
                }
                Global.putInt(AudioService.this.mContentResolver, "mode_ringer", ringerMode);
            }
        }

        private String getSoundEffectFilePath(int effectType) {
            String filePath = new StringBuilder();
            filePath.append(Environment.getProductDirectory());
            filePath.append(AudioService.SOUND_EFFECTS_PATH);
            filePath.append((String) AudioService.SOUND_EFFECT_FILES.get(AudioService.this.SOUND_EFFECT_FILES_MAP[effectType][0]));
            filePath = filePath.toString();
            if (new File(filePath).isFile()) {
                return filePath;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Environment.getRootDirectory());
            stringBuilder.append(AudioService.SOUND_EFFECTS_PATH);
            stringBuilder.append((String) AudioService.SOUND_EFFECT_FILES.get(AudioService.this.SOUND_EFFECT_FILES_MAP[effectType][0]));
            return stringBuilder.toString();
        }

        /* JADX WARNING: Missing block: B:79:0x0209, code:
            if (r0 != 0) goto L_0x020d;
     */
        /* JADX WARNING: Missing block: B:80:0x020b, code:
            r3 = true;
     */
        /* JADX WARNING: Missing block: B:81:0x020d, code:
            return r3;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private boolean onLoadSoundEffects() {
            synchronized (AudioService.this.mSoundEffectsLock) {
                boolean z = false;
                if (!AudioService.this.mSystemReady) {
                    Log.w(AudioService.TAG, "onLoadSoundEffects() called before boot complete");
                    return false;
                } else if (AudioService.this.mSoundPool != null) {
                    return true;
                } else {
                    AudioService.this.loadTouchSoundAssets();
                    AudioService.this.mSoundPool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(new Builder().setUsage(13).setContentType(4).build()).build();
                    AudioService.this.mSoundPoolCallBack = null;
                    AudioService.this.mSoundPoolListenerThread = new SoundPoolListenerThread();
                    AudioService.this.mSoundPoolListenerThread.start();
                    int attempts = 3;
                    while (AudioService.this.mSoundPoolCallBack == null) {
                        int attempts2 = attempts - 1;
                        if (attempts <= 0) {
                            attempts = attempts2;
                            break;
                        }
                        try {
                            AudioService.this.mSoundEffectsLock.wait(5000);
                        } catch (InterruptedException e) {
                            Log.w(AudioService.TAG, "Interrupted while waiting sound pool listener thread.");
                        }
                        attempts = attempts2;
                    }
                    if (AudioService.this.mSoundPoolCallBack == null) {
                        Log.w(AudioService.TAG, "onLoadSoundEffects() SoundPool listener or thread creation error");
                        if (AudioService.this.mSoundPoolLooper != null) {
                            AudioService.this.mSoundPoolLooper.quit();
                            AudioService.this.mSoundPoolLooper = null;
                        }
                        AudioService.this.mSoundPoolListenerThread = null;
                        AudioService.this.mSoundPool.release();
                        AudioService.this.mSoundPool = null;
                        return false;
                    }
                    int i;
                    int sampleId;
                    int[] poolId = new int[AudioService.SOUND_EFFECT_FILES.size()];
                    int fileIdx = 0;
                    while (true) {
                        i = -1;
                        if (fileIdx >= AudioService.SOUND_EFFECT_FILES.size()) {
                            break;
                        }
                        poolId[fileIdx] = -1;
                        fileIdx++;
                    }
                    int numSamples = 0;
                    fileIdx = 0;
                    while (fileIdx < 10) {
                        if (AudioService.this.SOUND_EFFECT_FILES_MAP[fileIdx][1] != 0) {
                            if (poolId[AudioService.this.SOUND_EFFECT_FILES_MAP[fileIdx][0]] == i) {
                                String filePath = getSoundEffectFilePath(fileIdx);
                                sampleId = AudioService.this.getSampleId(AudioService.this.mSoundPool, fileIdx, filePath, 0);
                                if (sampleId <= 0) {
                                    String str = AudioService.TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("Soundpool could not load file: ");
                                    stringBuilder.append(filePath);
                                    Log.w(str, stringBuilder.toString());
                                } else {
                                    AudioService.this.SOUND_EFFECT_FILES_MAP[fileIdx][1] = sampleId;
                                    poolId[AudioService.this.SOUND_EFFECT_FILES_MAP[fileIdx][0]] = sampleId;
                                    numSamples++;
                                }
                            } else {
                                AudioService.this.SOUND_EFFECT_FILES_MAP[fileIdx][1] = poolId[AudioService.this.SOUND_EFFECT_FILES_MAP[fileIdx][0]];
                            }
                        }
                        fileIdx++;
                        i = -1;
                    }
                    if (numSamples > 0) {
                        AudioService.this.mSoundPoolCallBack.setSamples(poolId);
                        fileIdx = 3;
                        attempts = 1;
                        while (true) {
                            i = attempts;
                            if (i != 1) {
                                sampleId = fileIdx;
                                break;
                            }
                            sampleId = fileIdx - 1;
                            if (fileIdx <= 0) {
                                break;
                            }
                            try {
                                AudioService.this.mSoundEffectsLock.wait(5000);
                                attempts = AudioService.this.mSoundPoolCallBack.status();
                            } catch (InterruptedException e2) {
                                Log.w(AudioService.TAG, "Interrupted while waiting sound pool callback.");
                                attempts = i;
                            }
                            fileIdx = sampleId;
                        }
                    } else {
                        sampleId = attempts;
                        i = -1;
                    }
                    attempts = i;
                    if (AudioService.this.mSoundPoolLooper != null) {
                        AudioService.this.mSoundPoolLooper.quit();
                        AudioService.this.mSoundPoolLooper = null;
                    }
                    AudioService.this.mSoundPoolListenerThread = null;
                    if (attempts != 0) {
                        String str2 = AudioService.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("onLoadSoundEffects(), Error ");
                        stringBuilder2.append(attempts);
                        stringBuilder2.append(" while loading samples");
                        Log.w(str2, stringBuilder2.toString());
                        for (int effect = 0; effect < 10; effect++) {
                            if (AudioService.this.SOUND_EFFECT_FILES_MAP[effect][1] > 0) {
                                AudioService.this.SOUND_EFFECT_FILES_MAP[effect][1] = -1;
                            }
                        }
                        AudioService.this.mSoundPool.release();
                        AudioService.this.mSoundPool = null;
                    }
                }
            }
        }

        private void onUnloadSoundEffects() {
            synchronized (AudioService.this.mSoundEffectsLock) {
                if (AudioService.this.mSoundPool == null) {
                    return;
                }
                int fileIdx;
                AudioService.this.unloadHwThemeSoundEffects();
                int[] poolId = new int[AudioService.SOUND_EFFECT_FILES.size()];
                for (fileIdx = 0; fileIdx < AudioService.SOUND_EFFECT_FILES.size(); fileIdx++) {
                    poolId[fileIdx] = 0;
                }
                fileIdx = 0;
                while (fileIdx < 10) {
                    if (AudioService.this.SOUND_EFFECT_FILES_MAP[fileIdx][1] > 0 && poolId[AudioService.this.SOUND_EFFECT_FILES_MAP[fileIdx][0]] == 0) {
                        AudioService.this.mSoundPool.unload(AudioService.this.SOUND_EFFECT_FILES_MAP[fileIdx][1]);
                        AudioService.this.SOUND_EFFECT_FILES_MAP[fileIdx][1] = -1;
                        poolId[AudioService.this.SOUND_EFFECT_FILES_MAP[fileIdx][0]] = -1;
                    }
                    fileIdx++;
                }
                AudioService.this.mSoundPool.release();
                AudioService.this.mSoundPool = null;
            }
        }

        private void onPlaySoundEffect(int effectType, int volume) {
            String str;
            StringBuilder stringBuilder;
            synchronized (AudioService.this.mSoundEffectsLock) {
                onLoadSoundEffects();
                if (AudioService.this.mSoundPool == null) {
                    return;
                }
                float volFloat;
                if (volume < 0) {
                    volFloat = (float) Math.pow(10.0d, (double) (((float) AudioService.sSoundEffectVolumeDb) / 20.0f));
                } else {
                    volFloat = ((float) volume) / 1000.0f;
                }
                if (AudioService.this.SOUND_EFFECT_FILES_MAP[effectType][1] > 0) {
                    AudioService.this.mSoundPool.play(AudioService.this.SOUND_EFFECT_FILES_MAP[effectType][1], volFloat, volFloat, 0, 0, 1.0f);
                } else {
                    MediaPlayer mediaPlayer = new MediaPlayer();
                    try {
                        mediaPlayer.setDataSource(getSoundEffectFilePath(effectType));
                        mediaPlayer.setAudioStreamType(1);
                        mediaPlayer.prepare();
                        mediaPlayer.setVolume(volFloat);
                        mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
                            public void onCompletion(MediaPlayer mp) {
                                AudioHandler.this.cleanupPlayer(mp);
                            }
                        });
                        mediaPlayer.setOnErrorListener(new OnErrorListener() {
                            public boolean onError(MediaPlayer mp, int what, int extra) {
                                AudioHandler.this.cleanupPlayer(mp);
                                return true;
                            }
                        });
                        mediaPlayer.start();
                    } catch (IOException ex) {
                        str = AudioService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("MediaPlayer IOException: ");
                        stringBuilder.append(ex);
                        Log.w(str, stringBuilder.toString());
                    } catch (IllegalArgumentException ex2) {
                        str = AudioService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("MediaPlayer IllegalArgumentException: ");
                        stringBuilder.append(ex2);
                        Log.w(str, stringBuilder.toString());
                    } catch (IllegalStateException ex3) {
                        str = AudioService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("MediaPlayer IllegalStateException: ");
                        stringBuilder.append(ex3);
                        Log.w(str, stringBuilder.toString());
                    }
                }
            }
        }

        private void cleanupPlayer(MediaPlayer mp) {
            if (mp != null) {
                try {
                    mp.stop();
                    mp.release();
                } catch (IllegalStateException ex) {
                    String str = AudioService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("MediaPlayer IllegalStateException: ");
                    stringBuilder.append(ex);
                    Log.w(str, stringBuilder.toString());
                }
            }
        }

        private void setForceUse(int usage, int config, String eventSource) {
            synchronized (AudioService.this.mConnectedDevices) {
                AudioService.this.setForceUseInt_SyncDevices(usage, config, eventSource);
            }
        }

        private void onPersistSafeVolumeState(int state) {
            Global.putInt(AudioService.this.mContentResolver, "audio_safe_volume_state", state);
        }

        private void onNotifyVolumeEvent(IAudioPolicyCallback apc, int direction) {
            try {
                apc.notifyVolumeAdjust(direction);
            } catch (Exception e) {
            }
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i != 10002) {
                switch (i) {
                    case 0:
                        AudioService.this.setDeviceVolume((VolumeStreamState) msg.obj, msg.arg1);
                        return;
                    case 1:
                        persistVolume((VolumeStreamState) msg.obj, msg.arg1);
                        return;
                    default:
                        switch (i) {
                            case 3:
                                persistRingerMode(AudioService.this.getRingerModeInternal());
                                return;
                            case 4:
                                AudioService.this.onAudioServerDied();
                                return;
                            case 5:
                                onPlaySoundEffect(msg.arg1, msg.arg2);
                                Jlog.d(175, msg.arg1, "playSoundEffect end");
                                return;
                            default:
                                int i2 = -1;
                                boolean z = false;
                                switch (i) {
                                    case 7:
                                        boolean loaded = onLoadSoundEffects();
                                        if (msg.obj != null) {
                                            LoadSoundEffectReply reply = msg.obj;
                                            synchronized (reply) {
                                                if (loaded) {
                                                    i2 = 0;
                                                }
                                                reply.mStatus = i2;
                                                reply.notify();
                                            }
                                            return;
                                        }
                                        return;
                                    case 8:
                                        break;
                                    case 9:
                                        AudioService.this.resetBluetoothSco();
                                        return;
                                    case 10:
                                        setAllVolumes((VolumeStreamState) msg.obj);
                                        return;
                                    default:
                                        AudioService audioService;
                                        switch (i) {
                                            case 12:
                                                i = AudioService.this.mRoutesObservers.beginBroadcast();
                                                if (i > 0) {
                                                    AudioRoutesInfo routes;
                                                    synchronized (AudioService.this.mCurAudioRoutes) {
                                                        routes = new AudioRoutesInfo(AudioService.this.mCurAudioRoutes);
                                                        Slog.i(AudioService.TAG, routes.toString());
                                                    }
                                                    while (i > 0) {
                                                        i--;
                                                        try {
                                                            ((IAudioRoutesObserver) AudioService.this.mRoutesObservers.getBroadcastItem(i)).dispatchAudioRoutesChanged(routes);
                                                        } catch (RemoteException e) {
                                                        }
                                                    }
                                                }
                                                AudioService.this.mRoutesObservers.finishBroadcast();
                                                AudioService.this.observeDevicesForStreams(-1);
                                                return;
                                            case 13:
                                                break;
                                            case 14:
                                                if (AudioService.this.mIsChineseZone || !AudioService.this.mHasAlarm) {
                                                    AudioService.this.onCheckMusicActive((String) msg.obj);
                                                    return;
                                                }
                                                return;
                                            case 15:
                                                AudioService.this.onSendBecomingNoisyIntent();
                                                return;
                                            case 16:
                                            case 17:
                                                audioService = AudioService.this;
                                                if (msg.what == 17) {
                                                    z = true;
                                                }
                                                audioService.onConfigureSafeVolume(z, (String) msg.obj);
                                                return;
                                            case 18:
                                                onPersistSafeVolumeState(msg.arg1);
                                                return;
                                            case 19:
                                                AudioService.this.onBroadcastScoConnectionState(msg.arg1);
                                                return;
                                            case 20:
                                                onUnloadSoundEffects();
                                                return;
                                            case 21:
                                                AudioService.this.onSystemReady();
                                                return;
                                            case 22:
                                                Secure.putIntForUser(AudioService.this.mContentResolver, "unsafe_volume_music_active_ms", msg.arg1, -2);
                                                return;
                                            default:
                                                switch (i) {
                                                    case 24:
                                                        AudioService.this.onUnmuteStream(msg.arg1, msg.arg2);
                                                        return;
                                                    case 25:
                                                        AudioService.this.onDynPolicyMixStateUpdate((String) msg.obj, msg.arg1);
                                                        return;
                                                    case 26:
                                                        AudioService.this.onIndicateSystemReady();
                                                        return;
                                                    case AudioService.MSG_ACCESSORY_PLUG_MEDIA_UNMUTE /*27*/:
                                                        AudioService.this.onAccessoryPlugMediaUnmute(msg.arg1);
                                                        return;
                                                    case 28:
                                                        onNotifyVolumeEvent((IAudioPolicyCallback) msg.obj, msg.arg1);
                                                        return;
                                                    case 29:
                                                        audioService = AudioService.this;
                                                        if (msg.arg1 == 1) {
                                                            z = true;
                                                        }
                                                        audioService.onDispatchAudioServerStateChange(z);
                                                        return;
                                                    case 30:
                                                        AudioService.this.onEnableSurroundFormats((ArrayList) msg.obj);
                                                        return;
                                                    default:
                                                        switch (i) {
                                                            case 100:
                                                                Log.i(AudioService.TAG, "handle msg:wired device connection");
                                                                WiredDeviceConnectionState connectState = msg.obj;
                                                                AudioService.this.mWiredDevLogger.log(new WiredDevConnectEvent(connectState));
                                                                AudioService.this.onSetWiredDeviceConnectionState(connectState.mType, connectState.mState, connectState.mAddress, connectState.mName, connectState.mCaller);
                                                                AudioService.this.mAudioEventWakeLock.release();
                                                                AudioService.this.updateAftPolicy();
                                                                return;
                                                            case 101:
                                                                AudioService.this.onSetA2dpSourceConnectionState((BluetoothDevice) msg.obj, msg.arg1);
                                                                AudioService.this.mAudioEventWakeLock.release();
                                                                return;
                                                            case 102:
                                                                AudioService.this.onSetA2dpSinkConnectionState((BluetoothDevice) msg.obj, msg.arg1, msg.arg2);
                                                                AudioService.this.mAudioEventWakeLock.release();
                                                                return;
                                                            case 103:
                                                                AudioService.this.onBluetoothA2dpDeviceConfigChange((BluetoothDevice) msg.obj);
                                                                AudioService.this.mAudioEventWakeLock.release();
                                                                return;
                                                            case 104:
                                                                PlaybackActivityMonitor playbackActivityMonitor = AudioService.this.mPlaybackMonitor;
                                                                if (msg.arg1 == 1) {
                                                                    z = true;
                                                                }
                                                                playbackActivityMonitor.disableAudioForUid(z, msg.arg2);
                                                                AudioService.this.mAudioEventWakeLock.release();
                                                                return;
                                                            case 105:
                                                                AudioService.this.onSetHearingAidConnectionState((BluetoothDevice) msg.obj, msg.arg1);
                                                                AudioService.this.mAudioEventWakeLock.release();
                                                                return;
                                                            case 106:
                                                                synchronized (AudioService.this.mConnectedDevices) {
                                                                    AudioService.this.makeA2dpDeviceUnavailableNow((String) msg.obj);
                                                                }
                                                                AudioService.this.mAudioEventWakeLock.release();
                                                                return;
                                                            default:
                                                                AudioService.this.handleMessageEx(msg);
                                                                return;
                                                        }
                                                }
                                        }
                                }
                                setForceUse(msg.arg1, msg.arg2, (String) msg.obj);
                                return;
                        }
                }
            } else if (AbsAudioService.SOUND_EFFECTS_SUPPORT) {
                AudioService.this.mHwAudioServiceEx.onSetSoundEffectState(msg.arg1, msg.arg2);
                AudioService.this.mAudioEventWakeLock.release();
            }
        }
    }

    public class AudioPolicyProxy extends AudioPolicyConfig implements DeathRecipient {
        private static final String TAG = "AudioPolicyProxy";
        int mFocusDuckBehavior = 0;
        final boolean mHasFocusListener;
        boolean mIsFocusPolicy = false;
        final boolean mIsVolumeController;
        final IAudioPolicyCallback mPolicyCallback;

        AudioPolicyProxy(AudioPolicyConfig config, IAudioPolicyCallback token, boolean hasFocusListener, boolean isFocusPolicy, boolean isVolumeController) {
            super(config);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(config.hashCode());
            stringBuilder.append(":ap:");
            stringBuilder.append(AudioService.this.mAudioPolicyCounter = AudioService.this.mAudioPolicyCounter + 1);
            setRegistration(new String(stringBuilder.toString()));
            this.mPolicyCallback = token;
            this.mHasFocusListener = hasFocusListener;
            this.mIsVolumeController = isVolumeController;
            if (this.mHasFocusListener) {
                AudioService.this.mMediaFocusControl.addFocusFollower(this.mPolicyCallback);
                if (isFocusPolicy) {
                    this.mIsFocusPolicy = true;
                    AudioService.this.mMediaFocusControl.setFocusPolicy(this.mPolicyCallback);
                }
            }
            if (this.mIsVolumeController) {
                AudioService.this.setExtVolumeController(this.mPolicyCallback);
            }
            connectMixes();
        }

        public void binderDied() {
            synchronized (AudioService.this.mAudioPolicies) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("audio policy ");
                stringBuilder.append(this.mPolicyCallback);
                stringBuilder.append(" died");
                Log.i(str, stringBuilder.toString());
                release();
                AudioService.this.mAudioPolicies.remove(this.mPolicyCallback.asBinder());
            }
            if (this.mIsVolumeController) {
                synchronized (AudioService.this.mExtVolumeControllerLock) {
                    AudioService.this.mExtVolumeController = null;
                }
            }
        }

        String getRegistrationId() {
            return getRegistration();
        }

        void release() {
            if (this.mIsFocusPolicy) {
                AudioService.this.mMediaFocusControl.unsetFocusPolicy(this.mPolicyCallback);
            }
            if (this.mFocusDuckBehavior == 1) {
                AudioService.this.mMediaFocusControl.setDuckingInExtPolicyAvailable(false);
            }
            if (this.mHasFocusListener) {
                AudioService.this.mMediaFocusControl.removeFocusFollower(this.mPolicyCallback);
            }
            long identity = Binder.clearCallingIdentity();
            AudioSystem.registerPolicyMixes(this.mMixes, false);
            Binder.restoreCallingIdentity(identity);
        }

        boolean hasMixAffectingUsage(int usage) {
            Iterator it = this.mMixes.iterator();
            while (it.hasNext()) {
                if (((AudioMix) it.next()).isAffectingUsage(usage)) {
                    return true;
                }
            }
            return false;
        }

        void addMixes(ArrayList<AudioMix> mixes) {
            synchronized (this.mMixes) {
                AudioSystem.registerPolicyMixes(this.mMixes, false);
                add(mixes);
                AudioSystem.registerPolicyMixes(this.mMixes, true);
            }
        }

        void removeMixes(ArrayList<AudioMix> mixes) {
            synchronized (this.mMixes) {
                AudioSystem.registerPolicyMixes(this.mMixes, false);
                remove(mixes);
                AudioSystem.registerPolicyMixes(this.mMixes, true);
            }
        }

        void connectMixes() {
            long identity = Binder.clearCallingIdentity();
            AudioSystem.registerPolicyMixes(this.mMixes, true);
            Binder.restoreCallingIdentity(identity);
        }
    }

    private class AudioServiceBroadcastReceiver extends BroadcastReceiver {
        private AudioServiceBroadcastReceiver() {
        }

        /* synthetic */ AudioServiceBroadcastReceiver(AudioService x0, AnonymousClass1 x1) {
            this();
        }

        /* JADX WARNING: Missing block: B:67:0x0150, code:
            r1 = false;
     */
        /* JADX WARNING: Missing block: B:69:0x0152, code:
            if (r1 == false) goto L_0x039e;
     */
        /* JADX WARNING: Missing block: B:70:0x0154, code:
            com.android.server.audio.AudioService.access$2400(r13.this$0, r6);
            r2 = new android.content.Intent("android.media.SCO_AUDIO_STATE_CHANGED");
            r2.putExtra("android.media.extra.SCO_AUDIO_STATE", r6);
            com.android.server.audio.AudioService.access$9200(r13.this$0, r2);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int config = 0;
            int dockState;
            if (action.equals("android.intent.action.DOCK_EVENT")) {
                dockState = intent.getIntExtra("android.intent.extra.DOCK_STATE", 0);
                switch (dockState) {
                    case 1:
                        config = 7;
                        break;
                    case 2:
                        config = 6;
                        break;
                    case 3:
                        config = 8;
                        break;
                    case 4:
                        config = 9;
                        break;
                }
                if (!(dockState == 3 || (dockState == 0 && AudioService.this.mDockState == 3))) {
                    AudioService.this.mForceUseLogger.log(new ForceUseEvent(3, config, "ACTION_DOCK_EVENT intent"));
                    AudioSystem.setForceUse(3, config);
                }
                AudioService.this.mDockState = dockState;
            } else if (action.equals("android.bluetooth.headset.profile.action.ACTIVE_DEVICE_CHANGED")) {
                AudioService.this.setBtScoActiveDevice((BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE"));
            } else {
                boolean z = true;
                if (action.equals("android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED")) {
                    boolean broadcast = false;
                    int scoAudioState = -1;
                    synchronized (AudioService.this.mScoClients) {
                        int btState = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1);
                        if (!AudioService.this.mScoClients.isEmpty() && (AudioService.this.mScoAudioState == 3 || AudioService.this.mScoAudioState == 1 || AudioService.this.mScoAudioState == 4 || AudioService.this.mScoAudioState == 5)) {
                            broadcast = true;
                        }
                        switch (btState) {
                            case 10:
                                BluetoothDevice scoDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                                if (scoDevice == null || AudioService.this.mBluetoothHeadsetDevice == null || scoDevice.equals(AudioService.this.mBluetoothHeadsetDevice)) {
                                    AudioService.this.setBluetoothScoOn(false);
                                    scoAudioState = 0;
                                    if (AudioService.this.mScoAudioState == 1 && AudioService.this.mBluetoothHeadset != null && AudioService.this.mBluetoothHeadsetDevice != null && AudioService.connectBluetoothScoAudioHelper(AudioService.this.mBluetoothHeadset, AudioService.this.mBluetoothHeadsetDevice, AudioService.this.mScoAudioMode)) {
                                        AudioService.this.mScoAudioState = 3;
                                        broadcast = false;
                                        break;
                                    }
                                    AudioService audioService = AudioService.this;
                                    if (AudioService.this.mScoAudioState != 3) {
                                        z = false;
                                    }
                                    audioService.clearAllScoClients(0, z);
                                    AudioService.this.mScoAudioState = 0;
                                    break;
                                }
                                return;
                            case 11:
                                if (!(AudioService.this.mScoAudioState == 3 || AudioService.this.mScoAudioState == 4)) {
                                    AudioService.this.mScoAudioState = 2;
                                    break;
                                }
                            case 12:
                                scoAudioState = 1;
                                if (!(AudioService.this.mScoAudioState == 3 || AudioService.this.mScoAudioState == 4)) {
                                    AudioService.this.mScoAudioState = 2;
                                }
                                AudioService.this.setBluetoothScoOn(true);
                                break;
                        }
                    }
                } else if (action.equals("android.intent.action.SCREEN_ON")) {
                    AudioService.this.mScreenOn = true;
                    if (AudioService.this.mMonitorRotation) {
                        RotationHelper.enable();
                    }
                    AudioSystem.setParameters("screen_state=on");
                    if (!AudioService.this.mIsChineseZone && AudioService.this.mHasAlarm) {
                        AudioService.this.mAlarmManager.cancel(AudioService.this.mPendingIntent);
                        AudioService.this.mHasAlarm = false;
                        AudioService.sendMsg(AudioService.this.mAudioHandler, 14, 0, 0, 0, AudioService.this.mSafeVolumeCaller, 0);
                    }
                } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                    AudioService.this.mScreenOn = false;
                    if (AudioService.this.mMonitorRotation) {
                        RotationHelper.disable();
                    }
                    AudioSystem.setParameters("screen_state=off");
                    if (!AudioService.this.mIsChineseZone && AudioService.this.mAudioHandler.hasMessages(14) && AudioSystem.isStreamActive(3, 3000)) {
                        AudioService.this.setCheckMusicActiveAlarm();
                    }
                } else if (action.equals("android.intent.action.USER_PRESENT") && AbsAudioService.SPK_RCV_STEREO_SUPPORT) {
                    if (AudioService.this.mMonitorRotation) {
                        RotationHelper.updateOrientation();
                    }
                } else if (action.equals("android.intent.action.CONFIGURATION_CHANGED")) {
                    AudioService.this.handleConfigurationChanged(context);
                } else if (action.equals("android.intent.action.USER_SWITCHED")) {
                    if (AudioService.this.mUserSwitchedReceived) {
                        AudioService.sendMsg(AudioService.this.mAudioHandler, 15, 0, 0, 0, null, 0);
                    }
                    AudioService.this.mUserSwitchedReceived = true;
                    AudioService.this.mMediaFocusControl.discardAudioFocusOwner();
                    AudioService.this.readAudioSettings(true);
                    AudioService.sendMsg(AudioService.this.mAudioHandler, 10, 2, 0, 0, AudioService.this.mStreamStates[3], 0);
                } else if (action.equals("android.intent.action.FM")) {
                    dockState = intent.getIntExtra(AudioService.CONNECT_INTENT_KEY_STATE, 0);
                    synchronized (AudioService.this.mConnectedDevices) {
                        String device_out_fm_key = AudioService.this.makeDeviceListKey(DumpState.DUMP_DEXOPT, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                        boolean isConnected = AudioService.this.mConnectedDevices.get(device_out_fm_key) != null;
                        if (dockState == 0 && isConnected) {
                            AudioSystem.setDeviceConnectionState(DumpState.DUMP_DEXOPT, 0, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                            AudioService.this.mConnectedDevices.remove(device_out_fm_key);
                        } else if (1 == dockState && !isConnected) {
                            AudioSystem.setDeviceConnectionState(DumpState.DUMP_DEXOPT, 1, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                            AudioService.this.mConnectedDevices.put(device_out_fm_key, new DeviceListSpec(DumpState.DUMP_DEXOPT, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS));
                        }
                    }
                } else if (action.equals("android.intent.action.USER_BACKGROUND")) {
                    dockState = intent.getIntExtra("android.intent.extra.user_handle", -1);
                    if (dockState >= 0) {
                        UserInfo userInfo = UserManagerService.getInstance().getUserInfo(dockState);
                        AudioService.this.onUserBackground(dockState);
                        AudioService.this.killBackgroundUserProcessesWithRecordAudioPermission(userInfo);
                    }
                    UserManagerService.getInstance().setUserRestriction("no_record_audio", true, dockState);
                } else if (action.equals("android.intent.action.USER_FOREGROUND")) {
                    dockState = intent.getIntExtra("android.intent.extra.user_handle", -1);
                    UserManagerService.getInstance().setUserRestriction("no_record_audio", false, dockState);
                    AudioService.this.onUserForeground(dockState);
                } else if (action.equals("android.bluetooth.adapter.action.STATE_CHANGED")) {
                    dockState = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", -1);
                    if (dockState == 10 || dockState == 13) {
                        if (dockState == 13 && AudioService.this.wasStreamActiveRecently(3, 0) && AudioService.this.isA2dpDeviceConnected()) {
                            AudioService.this.disconnectHeadset();
                            AudioService.this.disconnectA2dp();
                            AudioService.this.disconnectA2dpSink();
                        } else {
                            AudioService.this.disconnectAllBluetoothProfiles();
                        }
                    }
                } else if (action.equals("android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION") || action.equals("android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION")) {
                    AudioService.this.handleAudioEffectBroadcast(context, intent);
                } else if (!AudioService.this.mIsChineseZone && action.equals(AudioService.ACTION_CHECK_MUSIC_ACTIVE) && AudioSystem.isStreamActive(3, 3000)) {
                    AudioService.this.onCheckMusicActive(AudioService.this.mSafeVolumeCaller);
                    AudioService.this.setCheckMusicActiveAlarm();
                }
            }
        }
    }

    final class AudioServiceInternal extends AudioManagerInternal {
        AudioServiceInternal() {
        }

        public void setRingerModeDelegate(RingerModeDelegate delegate) {
            AudioService.this.mRingerModeDelegate = delegate;
            if (AudioService.this.mRingerModeDelegate != null) {
                synchronized (AudioService.this.mSettingsLock) {
                    AudioService.this.updateRingerAndZenModeAffectedStreams();
                }
                setRingerModeInternal(getRingerModeInternal(), "AudioService.setRingerModeDelegate");
            }
        }

        public void adjustSuggestedStreamVolumeForUid(int streamType, int direction, int flags, String callingPackage, int uid) {
            AudioService.this.adjustSuggestedStreamVolume(direction, streamType, flags, callingPackage, callingPackage, uid);
        }

        public void adjustStreamVolumeForUid(int streamType, int direction, int flags, String callingPackage, int uid) {
            AudioService.this.adjustStreamVolume(streamType, direction, flags, callingPackage, callingPackage, uid);
        }

        public void setStreamVolumeForUid(int streamType, int direction, int flags, String callingPackage, int uid) {
            AudioService.this.setStreamVolume(streamType, direction, flags, callingPackage, callingPackage, uid);
        }

        public int getRingerModeInternal() {
            return AudioService.this.getRingerModeInternal();
        }

        public void setRingerModeInternal(int ringerMode, String caller) {
            AudioService.this.setRingerModeInternal(ringerMode, caller);
        }

        public void silenceRingerModeInternal(String caller) {
            AudioService.this.silenceRingerModeInternal(caller);
        }

        public void updateRingerModeAffectedStreamsInternal() {
            synchronized (AudioService.this.mSettingsLock) {
                if (AudioService.this.updateRingerAndZenModeAffectedStreams()) {
                    AudioService.this.setRingerModeInt(getRingerModeInternal(), false);
                }
            }
        }

        public void setAccessibilityServiceUids(IntArray uids) {
            synchronized (AudioService.this.mAccessibilityServiceUidsLock) {
                if (uids.size() == 0) {
                    AudioService.this.mAccessibilityServiceUids = null;
                } else {
                    int i = 0;
                    boolean changed = AudioService.this.mAccessibilityServiceUids == null || AudioService.this.mAccessibilityServiceUids.length != uids.size();
                    if (!changed) {
                        while (i < AudioService.this.mAccessibilityServiceUids.length) {
                            if (uids.get(i) != AudioService.this.mAccessibilityServiceUids[i]) {
                                changed = true;
                                break;
                            }
                            i++;
                        }
                    }
                    if (changed) {
                        AudioService.this.mAccessibilityServiceUids = uids.toArray();
                    }
                }
            }
        }
    }

    private class AudioServiceUserRestrictionsListener implements UserRestrictionsListener {
        private AudioServiceUserRestrictionsListener() {
        }

        /* synthetic */ AudioServiceUserRestrictionsListener(AudioService x0, AnonymousClass1 x1) {
            this();
        }

        public void onUserRestrictionsChanged(int userId, Bundle newRestrictions, Bundle prevRestrictions) {
            boolean wasRestricted = prevRestrictions.getBoolean("no_unmute_microphone");
            boolean isRestricted = newRestrictions.getBoolean("no_unmute_microphone");
            if (wasRestricted != isRestricted) {
                AudioService.this.setMicrophoneMuteNoCallerCheck(isRestricted, userId);
            }
            isRestricted = true;
            wasRestricted = prevRestrictions.getBoolean("no_adjust_volume") || prevRestrictions.getBoolean("disallow_unmute_device");
            if (!(newRestrictions.getBoolean("no_adjust_volume") || newRestrictions.getBoolean("disallow_unmute_device"))) {
                isRestricted = false;
            }
            if (wasRestricted != isRestricted) {
                AudioService.this.setMasterMuteInternalNoCallerCheck(isRestricted, 0, userId);
            }
        }
    }

    private class AudioSystemThread extends Thread {
        AudioSystemThread() {
            super(AudioService.TAG);
        }

        public void run() {
            Looper.prepare();
            synchronized (AudioService.this) {
                AudioService.this.mAudioHandler = new AudioHandler(AudioService.this, null);
                AudioService.this.initHwThemeHandler();
                AudioService.this.notify();
            }
            Looper.loop();
        }
    }

    private class DeviceListSpec {
        String mDeviceAddress;
        String mDeviceName;
        int mDeviceType;

        public DeviceListSpec(int deviceType, String deviceName, String deviceAddress) {
            this.mDeviceType = deviceType;
            this.mDeviceName = deviceName;
            this.mDeviceAddress = deviceAddress;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[type:0x");
            stringBuilder.append(Integer.toHexString(this.mDeviceType));
            stringBuilder.append(" name:");
            stringBuilder.append(this.mDeviceName);
            stringBuilder.append(" address:");
            stringBuilder.append(this.mDeviceAddress);
            stringBuilder.append("]");
            return stringBuilder.toString();
        }
    }

    private class ForceControlStreamClient implements DeathRecipient {
        private IBinder mCb;

        ForceControlStreamClient(IBinder cb) {
            if (cb != null) {
                try {
                    cb.linkToDeath(this, 0);
                } catch (RemoteException e) {
                    String str = AudioService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ForceControlStreamClient() could not link to ");
                    stringBuilder.append(cb);
                    stringBuilder.append(" binder death");
                    Log.w(str, stringBuilder.toString());
                    cb = null;
                }
            }
            this.mCb = cb;
        }

        public void binderDied() {
            synchronized (AudioService.this.mForceControlStreamLock) {
                Log.w(AudioService.TAG, "SCO client died");
                if (AudioService.this.mForceControlStreamClient != this) {
                    Log.w(AudioService.TAG, "unregistered control stream client died");
                } else {
                    AudioService.this.mForceControlStreamClient = null;
                    AudioService.this.mVolumeControlStream = -1;
                    AudioService.this.mUserSelectedVolumeControlStream = false;
                }
            }
        }

        public void release() {
            if (this.mCb != null) {
                this.mCb.unlinkToDeath(this, 0);
                this.mCb = null;
            }
        }

        public IBinder getBinder() {
            return this.mCb;
        }
    }

    public class HwInnerAudioService extends IHwAudioServiceManager.Stub {
        private AudioService mAudioService;

        HwInnerAudioService(AudioService as) {
            this.mAudioService = as;
        }

        public int setSoundEffectState(boolean restore, String packageName, boolean isOnTop, String reserved) {
            int res = AudioService.this.mHwAudioServiceEx.setSoundEffectState(restore, packageName, isOnTop, reserved);
            AudioService.this.mHwAudioServiceEx.hideHiResIconDueKilledAPP(restore, packageName);
            return res;
        }

        public boolean checkRecordActive() {
            return AudioService.this.mHwAudioServiceEx.checkRecordActive(Binder.getCallingPid());
        }

        public void checkMicMute() {
            this.mAudioService.checkMicMute();
        }

        public void sendRecordStateChangedIntent(String sender, int state, int pid, String packageName) {
            AudioService.this.mHwAudioServiceEx.sendAudioRecordStateChangedIntent(sender, state, pid, packageName);
        }

        public int getRecordConcurrentType(String packageName) {
            return AudioService.this.mHwAudioServiceEx.getRecordConcurrentType(packageName);
        }
    }

    class LoadSoundEffectReply {
        public int mStatus = 1;

        LoadSoundEffectReply() {
        }
    }

    private class MyDisplayStatusCallback implements DisplayStatusCallback {
        private MyDisplayStatusCallback() {
        }

        /* synthetic */ MyDisplayStatusCallback(AudioService x0, AnonymousClass1 x1) {
            this();
        }

        public void onComplete(int status) {
            if (AudioService.this.mHdmiManager != null) {
                synchronized (AudioService.this.mHdmiManager) {
                    AudioService.this.mHdmiCecSink = status != -1;
                    if (AudioService.this.isPlatformTelevision() && !AudioService.this.mHdmiCecSink) {
                        AudioService audioService = AudioService.this;
                        audioService.mFixedVolumeDevices &= -1025;
                    }
                    AudioService.this.checkAllFixedVolumeDevices();
                }
            }
        }
    }

    private class RmtSbmxFullVolDeathHandler implements DeathRecipient {
        private IBinder mICallback;

        RmtSbmxFullVolDeathHandler(IBinder cb) {
            this.mICallback = cb;
            try {
                cb.linkToDeath(this, 0);
            } catch (RemoteException e) {
                Log.e(AudioService.TAG, "can't link to death", e);
            }
        }

        boolean isHandlerFor(IBinder cb) {
            return this.mICallback.equals(cb);
        }

        void forget() {
            try {
                this.mICallback.unlinkToDeath(this, 0);
            } catch (NoSuchElementException e) {
                Log.e(AudioService.TAG, "error unlinking to death", e);
            }
        }

        public void binderDied() {
            String str = AudioService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Recorder with remote submix at full volume died ");
            stringBuilder.append(this.mICallback);
            Log.w(str, stringBuilder.toString());
            AudioService.this.forceRemoteSubmixFullVolume(false, this.mICallback);
        }
    }

    private class ScoClient implements DeathRecipient {
        private IBinder mCb;
        private int mCreatorPid = Binder.getCallingPid();
        private int mStartcount = 0;

        ScoClient(IBinder cb) {
            this.mCb = cb;
        }

        public void binderDied() {
            synchronized (AudioService.this.mScoClients) {
                Log.w(AudioService.TAG, "SCO client died");
                if (AudioService.this.mScoClients.indexOf(this) < 0) {
                    Log.w(AudioService.TAG, "unregistered SCO client died");
                } else {
                    clearCount(true);
                    AudioService.this.mScoClients.remove(this);
                }
            }
        }

        public void incCount(int scoAudioMode) {
            synchronized (AudioService.this.mScoClients) {
                requestScoState(12, scoAudioMode);
                if (this.mStartcount == 0) {
                    try {
                        this.mCb.linkToDeath(this, 0);
                    } catch (RemoteException e) {
                        String str = AudioService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("ScoClient  incCount() could not link to ");
                        stringBuilder.append(this.mCb);
                        stringBuilder.append(" binder death");
                        Log.w(str, stringBuilder.toString());
                    }
                }
                this.mStartcount++;
                if (this.mStartcount > 1) {
                    AudioService.this.onScoExceptionOccur(getPid());
                    this.mStartcount = 1;
                }
            }
        }

        public void decCount() {
            synchronized (AudioService.this.mScoClients) {
                if (this.mStartcount == 0) {
                    Log.w(AudioService.TAG, "ScoClient.decCount() already 0");
                } else {
                    this.mStartcount--;
                    if (this.mStartcount == 0) {
                        try {
                            this.mCb.unlinkToDeath(this, 0);
                        } catch (NoSuchElementException e) {
                            Log.w(AudioService.TAG, "decCount() going to 0 but not registered to binder");
                        }
                    }
                    requestScoState(10, 0);
                }
            }
        }

        public void clearCount(boolean stopSco) {
            synchronized (AudioService.this.mScoClients) {
                if (this.mStartcount != 0) {
                    try {
                        this.mCb.unlinkToDeath(this, 0);
                    } catch (NoSuchElementException e) {
                        String str = AudioService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("clearCount() mStartcount: ");
                        stringBuilder.append(this.mStartcount);
                        stringBuilder.append(" != 0 but not registered to binder");
                        Log.w(str, stringBuilder.toString());
                    }
                }
                this.mStartcount = 0;
                if (stopSco) {
                    requestScoState(10, 0);
                }
            }
        }

        public int getCount() {
            return this.mStartcount;
        }

        public IBinder getBinder() {
            return this.mCb;
        }

        public int getPid() {
            return this.mCreatorPid;
        }

        public int totalCount() {
            int count;
            synchronized (AudioService.this.mScoClients) {
                count = 0;
                Iterator it = AudioService.this.mScoClients.iterator();
                while (it.hasNext()) {
                    count += ((ScoClient) it.next()).getCount();
                }
            }
            return count;
        }

        private void requestScoState(int state, int scoAudioMode) {
            AudioService.this.checkScoAudioState();
            int clientCount = totalCount();
            String str;
            StringBuilder stringBuilder;
            if (clientCount != 0) {
                str = AudioService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("requestScoState: state=");
                stringBuilder.append(state);
                stringBuilder.append(", scoAudioMode=");
                stringBuilder.append(scoAudioMode);
                stringBuilder.append(", clientCount=");
                stringBuilder.append(clientCount);
                Log.i(str, stringBuilder.toString());
                return;
            }
            if (state == 12) {
                AudioService.this.broadcastScoConnectionState(2);
                synchronized (AudioService.this.mSetModeDeathHandlers) {
                    int modeOwnerPid = AudioService.this.mSetModeDeathHandlers.isEmpty() ? 0 : ((SetModeDeathHandler) AudioService.this.mSetModeDeathHandlers.get(0)).getPid();
                    String str2;
                    StringBuilder stringBuilder2;
                    if (modeOwnerPid == 0 || modeOwnerPid == this.mCreatorPid) {
                        int access$2500 = AudioService.this.mScoAudioState;
                        if (access$2500 != 0) {
                            switch (access$2500) {
                                case 4:
                                    AudioService.this.mScoAudioState = 3;
                                    AudioService.this.broadcastScoConnectionState(1);
                                    break;
                                case 5:
                                    AudioService.this.mScoAudioState = 1;
                                    break;
                                default:
                                    str2 = AudioService.TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("requestScoState: failed to connect in state ");
                                    stringBuilder2.append(AudioService.this.mScoAudioState);
                                    stringBuilder2.append(", scoAudioMode=");
                                    stringBuilder2.append(scoAudioMode);
                                    Log.w(str2, stringBuilder2.toString());
                                    AudioService.this.broadcastScoConnectionState(0);
                                    break;
                            }
                        }
                        AudioService.this.mScoAudioMode = scoAudioMode;
                        if (scoAudioMode == -1) {
                            AudioService.this.mScoAudioMode = 0;
                            if (AudioService.this.mBluetoothHeadsetDevice != null) {
                                AudioService audioService = AudioService.this;
                                ContentResolver access$2800 = AudioService.this.mContentResolver;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("bluetooth_sco_channel_");
                                stringBuilder3.append(AudioService.this.mBluetoothHeadsetDevice.getAddress());
                                audioService.mScoAudioMode = Global.getInt(access$2800, stringBuilder3.toString(), 0);
                                if (AudioService.this.mScoAudioMode > 2 || AudioService.this.mScoAudioMode < 0) {
                                    AudioService.this.mScoAudioMode = 0;
                                }
                            }
                        }
                        if (AudioService.this.mBluetoothHeadset == null) {
                            if (AudioService.this.getBluetoothHeadset()) {
                                AudioService.this.mScoAudioState = 1;
                            } else {
                                str2 = AudioService.TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("requestScoState: getBluetoothHeadset failed during connection, mScoAudioMode=");
                                stringBuilder2.append(AudioService.this.mScoAudioMode);
                                Log.w(str2, stringBuilder2.toString());
                                AudioService.this.broadcastScoConnectionState(0);
                            }
                        } else if (AudioService.this.mBluetoothHeadsetDevice == null) {
                            str2 = AudioService.TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("requestScoState: no active device while connecting, mScoAudioMode=");
                            stringBuilder2.append(AudioService.this.mScoAudioMode);
                            Log.w(str2, stringBuilder2.toString());
                            AudioService.this.broadcastScoConnectionState(0);
                        } else if (AudioService.connectBluetoothScoAudioHelper(AudioService.this.mBluetoothHeadset, AudioService.this.mBluetoothHeadsetDevice, AudioService.this.mScoAudioMode)) {
                            AudioService.this.mScoAudioState = 3;
                        } else {
                            str2 = AudioService.TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("requestScoState: connect to ");
                            stringBuilder2.append(AudioService.this.mBluetoothHeadsetDevice);
                            stringBuilder2.append(" failed, mScoAudioMode=");
                            stringBuilder2.append(AudioService.this.mScoAudioMode);
                            Log.w(str2, stringBuilder2.toString());
                            AudioService.this.broadcastScoConnectionState(0);
                        }
                    } else {
                        str2 = AudioService.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("requestScoState: audio mode is not NORMAL and modeOwnerPid ");
                        stringBuilder2.append(modeOwnerPid);
                        stringBuilder2.append(" != creatorPid ");
                        stringBuilder2.append(this.mCreatorPid);
                        Log.w(str2, stringBuilder2.toString());
                        AudioService.this.broadcastScoConnectionState(0);
                    }
                }
            } else if (state == 10) {
                int access$25002 = AudioService.this.mScoAudioState;
                if (access$25002 == 1) {
                    AudioService.this.mScoAudioState = 0;
                    AudioService.this.broadcastScoConnectionState(0);
                } else if (access$25002 != 3) {
                    str = AudioService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("requestScoState: failed to disconnect in state ");
                    stringBuilder.append(AudioService.this.mScoAudioState);
                    stringBuilder.append(", scoAudioMode=");
                    stringBuilder.append(scoAudioMode);
                    Log.w(str, stringBuilder.toString());
                    AudioService.this.broadcastScoConnectionState(0);
                } else if (AudioService.this.mBluetoothHeadset == null) {
                    if (AudioService.this.getBluetoothHeadset()) {
                        AudioService.this.mScoAudioState = 4;
                    } else {
                        str = AudioService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("requestScoState: getBluetoothHeadset failed during disconnection, mScoAudioMode=");
                        stringBuilder.append(AudioService.this.mScoAudioMode);
                        Log.w(str, stringBuilder.toString());
                        AudioService.this.mScoAudioState = 0;
                        AudioService.this.broadcastScoConnectionState(0);
                    }
                } else if (AudioService.this.mBluetoothHeadsetDevice == null) {
                    AudioService.this.mScoAudioState = 0;
                    AudioService.this.broadcastScoConnectionState(0);
                } else if (AudioService.disconnectBluetoothScoAudioHelper(AudioService.this.mBluetoothHeadset, AudioService.this.mBluetoothHeadsetDevice, AudioService.this.mScoAudioMode)) {
                    AudioService.this.mScoAudioState = 5;
                } else {
                    AudioService.this.mScoAudioState = 0;
                    AudioService.this.broadcastScoConnectionState(0);
                }
            }
        }
    }

    protected class SetModeDeathHandler implements DeathRecipient {
        private IBinder mCb;
        private int mMode = 0;
        private int mPid;

        SetModeDeathHandler(IBinder cb, int pid) {
            this.mCb = cb;
            this.mPid = pid;
        }

        public void binderDied() {
            int oldModeOwnerPid = 0;
            int newModeOwnerPid = 0;
            synchronized (AudioService.this.mSetModeDeathHandlers) {
                Log.w(AudioService.TAG, "setMode() client died");
                if (!AudioService.this.mSetModeDeathHandlers.isEmpty()) {
                    oldModeOwnerPid = ((SetModeDeathHandler) AudioService.this.mSetModeDeathHandlers.get(0)).getPid();
                }
                if (AudioService.this.mSetModeDeathHandlers.indexOf(this) < 0) {
                    Log.w(AudioService.TAG, "unregistered setMode() client died");
                } else {
                    newModeOwnerPid = AudioService.this.setModeInt(0, this.mCb, this.mPid, AudioService.TAG);
                }
            }
            if (newModeOwnerPid != oldModeOwnerPid && newModeOwnerPid != 0) {
                long ident = Binder.clearCallingIdentity();
                AudioService.this.disconnectBluetoothSco(newModeOwnerPid);
                Binder.restoreCallingIdentity(ident);
            }
        }

        public int getPid() {
            return this.mPid;
        }

        public void setMode(int mode) {
            this.mMode = mode;
        }

        public int getMode() {
            return this.mMode;
        }

        public IBinder getBinder() {
            return this.mCb;
        }

        public void dump(PrintWriter pw) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Mode=");
            stringBuilder.append(this.mMode);
            stringBuilder.append("; Pid=");
            stringBuilder.append(this.mPid);
            stringBuilder.append("; PkgName=");
            stringBuilder.append(AudioService.this.getPackageNameByPid(this.mPid));
            pw.println(stringBuilder.toString());
        }
    }

    private class SettingsObserver extends ContentObserver {
        SettingsObserver() {
            super(new Handler());
            AudioService.this.mContentResolver.registerContentObserver(Global.getUriFor("zen_mode"), false, this);
            AudioService.this.mContentResolver.registerContentObserver(Global.getUriFor("zen_mode_config_etag"), false, this);
            AudioService.this.mContentResolver.registerContentObserver(System.getUriFor("mode_ringer_streams_affected"), false, this);
            AudioService.this.mContentResolver.registerContentObserver(Global.getUriFor("dock_audio_media_enabled"), false, this);
            AudioService.this.mContentResolver.registerContentObserver(System.getUriFor("master_mono"), false, this);
            AudioService.this.mEncodedSurroundMode = Global.getInt(AudioService.this.mContentResolver, "encoded_surround_output", 0);
            AudioService.this.mContentResolver.registerContentObserver(Global.getUriFor("encoded_surround_output"), false, this);
            AudioService.this.mEnabledSurroundFormats = Global.getString(AudioService.this.mContentResolver, "encoded_surround_output_enabled_formats");
            AudioService.this.mContentResolver.registerContentObserver(Global.getUriFor("encoded_surround_output_enabled_formats"), false, this);
        }

        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            synchronized (AudioService.this.mSettingsLock) {
                if (AudioService.this.updateRingerAndZenModeAffectedStreams()) {
                    AudioService.this.setRingerModeInt(AudioService.this.getRingerModeInternal(), false);
                }
                AudioService.this.readDockAudioSettings(AudioService.this.mContentResolver);
                AudioService.this.updateMasterMono(AudioService.this.mContentResolver);
                updateEncodedSurroundOutput();
                AudioService.this.sendEnabledSurroundFormats(AudioService.this.mContentResolver, AudioService.this.mSurroundModeChanged);
            }
        }

        private void updateEncodedSurroundOutput() {
            int newSurroundMode = Global.getInt(AudioService.this.mContentResolver, "encoded_surround_output", 0);
            if (AudioService.this.mEncodedSurroundMode != newSurroundMode) {
                AudioService.this.sendEncodedSurroundMode(newSurroundMode, "SettingsObserver");
                synchronized (AudioService.this.mConnectedDevices) {
                    if (((DeviceListSpec) AudioService.this.mConnectedDevices.get(AudioService.this.makeDeviceListKey(1024, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS))) != null) {
                        AudioService.this.setWiredDeviceConnectionState(1024, 0, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, PackageManagerService.PLATFORM_PACKAGE_NAME);
                        AudioService.this.setWiredDeviceConnectionState(1024, 1, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, PackageManagerService.PLATFORM_PACKAGE_NAME);
                    }
                }
                AudioService.this.mEncodedSurroundMode = newSurroundMode;
                AudioService.this.mSurroundModeChanged = true;
                return;
            }
            AudioService.this.mSurroundModeChanged = false;
        }
    }

    private final class SoundPoolCallback implements OnLoadCompleteListener {
        List<Integer> mSamples;
        int mStatus;

        private SoundPoolCallback() {
            this.mStatus = 1;
            this.mSamples = new ArrayList();
        }

        /* synthetic */ SoundPoolCallback(AudioService x0, AnonymousClass1 x1) {
            this();
        }

        public int status() {
            return this.mStatus;
        }

        public void setSamples(int[] samples) {
            for (int i = 0; i < samples.length; i++) {
                if (samples[i] > 0) {
                    this.mSamples.add(Integer.valueOf(samples[i]));
                }
            }
        }

        public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
            synchronized (AudioService.this.mSoundEffectsLock) {
                int i = this.mSamples.indexOf(Integer.valueOf(sampleId));
                if (i >= 0) {
                    this.mSamples.remove(i);
                }
                if (status != 0 || this.mSamples.isEmpty()) {
                    this.mStatus = status;
                    AudioService.this.mSoundEffectsLock.notify();
                }
            }
        }
    }

    class SoundPoolListenerThread extends Thread {
        public SoundPoolListenerThread() {
            super("SoundPoolListenerThread");
        }

        public void run() {
            Looper.prepare();
            AudioService.this.mSoundPoolLooper = Looper.myLooper();
            synchronized (AudioService.this.mSoundEffectsLock) {
                if (AudioService.this.mSoundPool != null) {
                    AudioService.this.mSoundPoolCallBack = new SoundPoolCallback(AudioService.this, null);
                    AudioService.this.mSoundPool.setOnLoadCompleteListener(AudioService.this.mSoundPoolCallBack);
                }
                AudioService.this.mSoundEffectsLock.notify();
            }
            Looper.loop();
        }
    }

    class StreamVolumeCommand {
        public final int mDevice;
        public final int mFlags;
        public final int mIndex;
        public final int mStreamType;

        StreamVolumeCommand(int streamType, int index, int flags, int device) {
            this.mStreamType = streamType;
            this.mIndex = index;
            this.mFlags = flags;
            this.mDevice = device;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{streamType=");
            stringBuilder.append(this.mStreamType);
            stringBuilder.append(",index=");
            stringBuilder.append(this.mIndex);
            stringBuilder.append(",flags=");
            stringBuilder.append(this.mFlags);
            stringBuilder.append(",device=");
            stringBuilder.append(this.mDevice);
            stringBuilder.append('}');
            return stringBuilder.toString();
        }
    }

    public static class VolumeController {
        private static final String TAG = "VolumeController";
        private IVolumeController mController;
        private int mLongPressTimeout;
        private long mNextLongPress;
        private boolean mVisible;
        private IVrManager mVrManager;

        public void setController(IVolumeController controller) {
            this.mController = controller;
            this.mVisible = false;
        }

        public void loadSettings(ContentResolver cr) {
            this.mLongPressTimeout = Secure.getIntForUser(cr, "long_press_timeout", 500, -2);
        }

        public boolean suppressAdjustment(int resolvedStream, int flags, boolean isMute) {
            if (isMute) {
                return false;
            }
            boolean suppress = false;
            if (resolvedStream == 2 && this.mController != null) {
                long now = SystemClock.uptimeMillis();
                if ((flags & 1) != 0 && !this.mVisible) {
                    if (this.mNextLongPress < now) {
                        this.mNextLongPress = ((long) this.mLongPressTimeout) + now;
                    }
                    suppress = true;
                } else if (this.mNextLongPress > 0) {
                    if (now > this.mNextLongPress) {
                        this.mNextLongPress = 0;
                    } else {
                        suppress = true;
                    }
                }
            }
            return suppress;
        }

        public void setVisible(boolean visible) {
            this.mVisible = visible;
        }

        public boolean isSameBinder(IVolumeController controller) {
            return Objects.equals(asBinder(), binder(controller));
        }

        public IBinder asBinder() {
            return binder(this.mController);
        }

        private static IBinder binder(IVolumeController controller) {
            return controller == null ? null : controller.asBinder();
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("VolumeController(");
            stringBuilder.append(asBinder());
            stringBuilder.append(",mVisible=");
            stringBuilder.append(this.mVisible);
            stringBuilder.append(")");
            return stringBuilder.toString();
        }

        public void postDisplaySafeVolumeWarning(int flags) {
            this.mVrManager = IVrManager.Stub.asInterface(ServiceManager.getService("vrmanager"));
            try {
                if (this.mVrManager != null && this.mVrManager.getVrModeState()) {
                    return;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "checkSafeMediaVolume cannot get VR mode");
            } catch (SecurityException e2) {
                Log.e(TAG, "checkSafeMediaVolume cannot get android.permission.ACCESS_VR_MANAGER, android.permission.ACCESS_VR_STATE");
            }
            if (this.mController != null && !HwFrameworkFactory.getVRSystemServiceManager().isVRMode()) {
                try {
                    this.mController.displaySafeVolumeWarning(flags | 1);
                } catch (RemoteException e3) {
                    Log.w(TAG, "Error calling displaySafeVolumeWarning", e3);
                }
            }
        }

        public void postVolumeChanged(int streamType, int flags) {
            if (this.mController != null && !HwFrameworkFactory.getVRSystemServiceManager().isVRMode()) {
                try {
                    this.mController.volumeChanged(streamType, flags);
                } catch (RemoteException e) {
                    Log.w(TAG, "Error calling volumeChanged", e);
                } catch (NullPointerException e2) {
                    Log.e(TAG, "Error Controller is Null");
                }
            }
        }

        public void postMasterMuteChanged(int flags) {
            if (this.mController != null && !HwFrameworkFactory.getVRSystemServiceManager().isVRMode()) {
                try {
                    this.mController.masterMuteChanged(flags);
                } catch (RemoteException e) {
                    Log.w(TAG, "Error calling masterMuteChanged", e);
                }
            }
        }

        public void setLayoutDirection(int layoutDirection) {
            if (this.mController != null && !HwFrameworkFactory.getVRSystemServiceManager().isVRMode()) {
                try {
                    this.mController.setLayoutDirection(layoutDirection);
                } catch (RemoteException e) {
                    Log.w(TAG, "Error calling setLayoutDirection", e);
                }
            }
        }

        public void postDismiss() {
            if (this.mController != null) {
                try {
                    this.mController.dismiss();
                } catch (RemoteException e) {
                    Log.w(TAG, "Error calling dismiss", e);
                }
            }
        }

        public void setA11yMode(int a11yMode) {
            if (this.mController != null) {
                try {
                    this.mController.setA11yMode(a11yMode);
                } catch (RemoteException e) {
                    Log.w(TAG, "Error calling setA11Mode", e);
                }
            }
        }
    }

    public class VolumeStreamState {
        private final SparseIntArray mIndexMap;
        private int mIndexMax;
        private int mIndexMin;
        private boolean mIsMuted;
        private int mObservedDevices;
        private final Intent mStreamDevicesChanged;
        private final int mStreamType;
        private HwCustAudioServiceVolumeStreamState mVSSCust;
        private final Intent mVolumeChanged;
        private String mVolumeIndexSettingName;

        /* synthetic */ VolumeStreamState(AudioService x0, String x1, int x2, AnonymousClass1 x3) {
            this(x1, x2);
        }

        private VolumeStreamState(String settingName, int streamType) {
            this.mIndexMap = new SparseIntArray(8);
            this.mVSSCust = null;
            this.mVolumeIndexSettingName = settingName;
            this.mStreamType = streamType;
            this.mIndexMin = AudioService.MIN_STREAM_VOLUME[streamType] * 10;
            this.mIndexMax = AudioService.MAX_STREAM_VOLUME[streamType] * 10;
            AudioSystem.initStreamVolume(streamType, this.mIndexMin / 10, this.mIndexMax / 10);
            this.mVSSCust = (HwCustAudioServiceVolumeStreamState) HwCustUtils.createObj(HwCustAudioServiceVolumeStreamState.class, new Object[]{this$0.mContext});
            readSettings();
            this.mVolumeChanged = new Intent("android.media.VOLUME_CHANGED_ACTION");
            this.mVolumeChanged.putExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", this.mStreamType);
            this.mStreamDevicesChanged = new Intent("android.media.STREAM_DEVICES_CHANGED_ACTION");
            this.mStreamDevicesChanged.putExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", this.mStreamType);
        }

        public int observeDevicesForStream_syncVSS(boolean checkOthers) {
            int devices = AudioSystem.getDevicesForStream(this.mStreamType);
            if (devices == this.mObservedDevices) {
                return devices;
            }
            int prevDevices = this.mObservedDevices;
            this.mObservedDevices = devices;
            if (checkOthers) {
                AudioService.this.observeDevicesForStreams(this.mStreamType);
            }
            if (AudioService.mStreamVolumeAlias[this.mStreamType] == this.mStreamType) {
                EventLogTags.writeStreamDevicesChanged(this.mStreamType, prevDevices, devices);
            }
            AudioService.this.sendBroadcastToAll(this.mStreamDevicesChanged.putExtra("android.media.EXTRA_PREV_VOLUME_STREAM_DEVICES", prevDevices).putExtra("android.media.EXTRA_VOLUME_STREAM_DEVICES", devices));
            return devices;
        }

        public String getSettingNameForDevice(int device) {
            if (!hasValidSettingsName()) {
                return null;
            }
            String suffix = AudioSystem.getOutputDeviceName(device);
            if (suffix.isEmpty()) {
                return this.mVolumeIndexSettingName;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mVolumeIndexSettingName);
            stringBuilder.append("_");
            stringBuilder.append(suffix);
            return stringBuilder.toString();
        }

        private boolean hasValidSettingsName() {
            return (this.mVolumeIndexSettingName == null || this.mVolumeIndexSettingName.isEmpty()) ? false : true;
        }

        /* JADX WARNING: Missing block: B:21:0x002e, code:
            r1 = com.android.server.audio.AudioService.VolumeStreamState.class;
     */
        /* JADX WARNING: Missing block: B:22:0x0030, code:
            monitor-enter(r1);
     */
        /* JADX WARNING: Missing block: B:23:0x0031, code:
            r6 = 1879048191;
            r0 = 0;
     */
        /* JADX WARNING: Missing block: B:24:0x0037, code:
            if (r6 == 0) goto L_0x00a2;
     */
        /* JADX WARNING: Missing block: B:25:0x0039, code:
            r7 = 1 << r0;
     */
        /* JADX WARNING: Missing block: B:26:0x003d, code:
            if ((r7 & r6) != 0) goto L_0x0041;
     */
        /* JADX WARNING: Missing block: B:27:0x0041, code:
            r6 = r6 & (~r7);
     */
        /* JADX WARNING: Missing block: B:30:0x0046, code:
            if (r15.mStreamType != 3) goto L_0x004d;
     */
        /* JADX WARNING: Missing block: B:32:0x0049, code:
            if (r7 != 2) goto L_0x004d;
     */
        /* JADX WARNING: Missing block: B:33:0x004b, code:
            r8 = true;
     */
        /* JADX WARNING: Missing block: B:34:0x004d, code:
            r8 = false;
     */
        /* JADX WARNING: Missing block: B:36:0x004f, code:
            if (r7 == 1073741824) goto L_0x0056;
     */
        /* JADX WARNING: Missing block: B:37:0x0051, code:
            if (r8 == false) goto L_0x0054;
     */
        /* JADX WARNING: Missing block: B:38:0x0054, code:
            r11 = -1;
     */
        /* JADX WARNING: Missing block: B:39:0x0056, code:
            r11 = android.media.AudioSystem.DEFAULT_STREAM_VOLUME[r15.mStreamType];
     */
        /* JADX WARNING: Missing block: B:41:0x0060, code:
            if (hasValidSettingsName() != false) goto L_0x0065;
     */
        /* JADX WARNING: Missing block: B:42:0x0062, code:
            r13 = r11;
     */
        /* JADX WARNING: Missing block: B:43:0x0065, code:
            r13 = android.provider.Settings.System.getIntForUser(com.android.server.audio.AudioService.access$2800(r15.this$0), getSettingNameForDevice(r7), r11, -2);
     */
        /* JADX WARNING: Missing block: B:44:0x0074, code:
            r12 = r13;
     */
        /* JADX WARNING: Missing block: B:45:0x0075, code:
            if (r12 != -1) goto L_0x0086;
     */
        /* JADX WARNING: Missing block: B:47:0x0079, code:
            if (r15.mStreamType != 3) goto L_0x0086;
     */
        /* JADX WARNING: Missing block: B:49:0x007d, code:
            if (r7 != 128) goto L_0x0086;
     */
        /* JADX WARNING: Missing block: B:50:0x007f, code:
            r12 = android.media.AudioSystem.DEFAULT_STREAM_VOLUME[r15.mStreamType];
     */
        /* JADX WARNING: Missing block: B:51:0x0086, code:
            if (r12 != -1) goto L_0x0094;
     */
        /* JADX WARNING: Missing block: B:53:0x008a, code:
            if (r15.mVSSCust == null) goto L_0x009f;
     */
        /* JADX WARNING: Missing block: B:54:0x008c, code:
            r15.mVSSCust.readSettings(r15.mStreamType, r7);
     */
        /* JADX WARNING: Missing block: B:55:0x0094, code:
            r15.mIndexMap.put(r7, getValidIndex(10 * r12));
     */
        /* JADX WARNING: Missing block: B:56:0x009f, code:
            r0 = r0 + 1;
     */
        /* JADX WARNING: Missing block: B:57:0x00a2, code:
            monitor-exit(r1);
     */
        /* JADX WARNING: Missing block: B:58:0x00a3, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void readSettings() {
            synchronized (AudioService.this.mSettingsLock) {
                synchronized (VolumeStreamState.class) {
                    if (AudioService.this.mUseFixedVolume) {
                        this.mIndexMap.put(1073741824, this.mIndexMax);
                    } else if (this.mStreamType == 1 || this.mStreamType == 7) {
                        int index = 10 * AudioSystem.DEFAULT_STREAM_VOLUME[this.mStreamType];
                        if (AudioService.this.mCameraSoundForced) {
                            index = this.mIndexMax;
                        }
                        this.mIndexMap.put(1073741824, index);
                    }
                }
            }
        }

        private int getAbsoluteVolumeIndex(int index) {
            if (index == 0) {
                return 0;
            }
            if (index == 1) {
                return ((int) (((double) this.mIndexMax) * 0.5d)) / 10;
            }
            if (index == 2) {
                return ((int) (((double) this.mIndexMax) * 0.7d)) / 10;
            }
            if (index == 3) {
                return ((int) (((double) this.mIndexMax) * 0.85d)) / 10;
            }
            return (this.mIndexMax + 5) / 10;
        }

        public void applyDeviceVolume_syncVSS(int device) {
            boolean isTurnOff = false;
            if (this.mVSSCust != null) {
                isTurnOff = this.mVSSCust.isTurnOffAllSound();
            }
            int index = (this.mIsMuted || isTurnOff) ? 0 : ((device & 896) == 0 || !AudioService.this.mAvrcpAbsVolSupported) ? (AudioService.this.mFullVolumeDevices & device) != 0 ? (this.mIndexMax + 5) / 10 : (134217728 & device) != 0 ? (this.mIndexMax + 5) / 10 : (getIndex(device) + 5) / 10 : getAbsoluteVolumeIndex((getIndex(device) + 5) / 10);
            AudioSystem.setStreamVolumeIndex(this.mStreamType, index, device);
        }

        public void applyAllVolumes() {
            synchronized (VolumeStreamState.class) {
                int i;
                boolean isTurnOff = false;
                if (this.mVSSCust != null) {
                    isTurnOff = this.mVSSCust.isTurnOffAllSound();
                }
                for (i = 0; i < this.mIndexMap.size(); i++) {
                    int device = this.mIndexMap.keyAt(i);
                    if (device != 1073741824) {
                        int index = (this.mIsMuted || isTurnOff) ? 0 : ((device & 896) == 0 || !AudioService.this.mAvrcpAbsVolSupported) ? (AudioService.this.mFullVolumeDevices & device) != 0 ? (this.mIndexMax + 5) / 10 : (134217728 & device) != 0 ? (this.mIndexMax + 5) / 10 : (this.mIndexMap.valueAt(i) + 5) / 10 : getAbsoluteVolumeIndex((getIndex(device) + 5) / 10);
                        AudioSystem.setStreamVolumeIndex(this.mStreamType, index, device);
                    }
                }
                if (this.mIsMuted || isTurnOff) {
                    i = 0;
                } else {
                    i = (getIndex(1073741824) + 5) / 10;
                }
                AudioSystem.setStreamVolumeIndex(this.mStreamType, i, 1073741824);
                if (this.mVSSCust != null) {
                    this.mVSSCust.applyAllVolumes(this.mIsMuted, this.mStreamType);
                }
            }
        }

        public boolean adjustIndex(int deltaIndex, int device, String caller) {
            return setIndex(getIndex(device) + deltaIndex, device, caller);
        }

        public boolean setIndex(int index, int device, String caller) {
            int oldIndex;
            boolean changed;
            synchronized (AudioService.this.mSettingsLock) {
                boolean changed2;
                synchronized (VolumeStreamState.class) {
                    oldIndex = getIndex(device);
                    index = getValidIndex(index);
                    if (this.mStreamType == 7 && AudioService.this.mCameraSoundForced) {
                        index = this.mIndexMax;
                    }
                    this.mIndexMap.put(device, index);
                    int i = 0;
                    boolean isCurrentDevice = true;
                    changed2 = oldIndex != index;
                    if (device != AudioService.this.getDeviceForStream(this.mStreamType)) {
                        isCurrentDevice = false;
                    }
                    int streamType = AudioSystem.getNumStreamTypes() - 1;
                    while (streamType >= 0) {
                        VolumeStreamState aliasStreamState = AudioService.this.mStreamStates[streamType];
                        if (streamType != this.mStreamType && AudioService.mStreamVolumeAlias[streamType] == this.mStreamType && (changed2 || !aliasStreamState.hasIndexForDevice(device))) {
                            int scaledIndex = AudioService.this.rescaleIndex(index, this.mStreamType, streamType);
                            aliasStreamState.setIndex(scaledIndex, device, caller);
                            if (isCurrentDevice) {
                                aliasStreamState.setIndex(scaledIndex, AudioService.this.getDeviceForStream(streamType), caller);
                            }
                        }
                        streamType--;
                    }
                    if (changed2 && this.mStreamType == 2 && device == 2) {
                        while (i < this.mIndexMap.size()) {
                            streamType = this.mIndexMap.keyAt(i);
                            if ((streamType & 112) != 0) {
                                this.mIndexMap.put(streamType, index);
                            }
                            i++;
                        }
                    }
                }
                changed = changed2;
            }
            if (changed) {
                oldIndex = (oldIndex + 5) / 10;
                index = (index + 5) / 10;
                if (AudioService.mStreamVolumeAlias[this.mStreamType] == this.mStreamType) {
                    if (caller == null) {
                        Log.w(AudioService.TAG, "No caller for volume_changed event", new Throwable());
                    }
                    EventLogTags.writeVolumeChanged(this.mStreamType, oldIndex, index, this.mIndexMax / 10, caller);
                }
                this.mVolumeChanged.putExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", index);
                this.mVolumeChanged.putExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", oldIndex);
                this.mVolumeChanged.putExtra("android.media.EXTRA_VOLUME_STREAM_TYPE_ALIAS", AudioService.mStreamVolumeAlias[this.mStreamType]);
                AudioService.this.sendBroadcastToAll(this.mVolumeChanged);
            }
            return changed;
        }

        public int getIndex(int device) {
            int index;
            synchronized (VolumeStreamState.class) {
                index = this.mIndexMap.get(device, -1);
                if (index == -1) {
                    index = this.mIndexMap.get(1073741824);
                }
            }
            return index;
        }

        public boolean hasIndexForDevice(int device) {
            boolean z;
            synchronized (VolumeStreamState.class) {
                z = this.mIndexMap.get(device, -1) != -1;
            }
            return z;
        }

        public int getMaxIndex() {
            return this.mIndexMax;
        }

        public int getMinIndex() {
            return this.mIndexMin;
        }

        @GuardedBy("VolumeStreamState.class")
        public void refreshRange(int sourceStreamType) {
            this.mIndexMin = AudioService.MIN_STREAM_VOLUME[sourceStreamType] * 10;
            this.mIndexMax = AudioService.MAX_STREAM_VOLUME[sourceStreamType] * 10;
            for (int i = 0; i < this.mIndexMap.size(); i++) {
                this.mIndexMap.put(this.mIndexMap.keyAt(i), getValidIndex(this.mIndexMap.valueAt(i)));
            }
        }

        @GuardedBy("VolumeStreamState.class")
        public void setAllIndexes(VolumeStreamState srcStream, String caller) {
            if (this.mStreamType != srcStream.mStreamType) {
                int srcStreamType = srcStream.getStreamType();
                int index = AudioService.this.rescaleIndex(srcStream.getIndex(1073741824), srcStreamType, this.mStreamType);
                int i = 0;
                for (int i2 = 0; i2 < this.mIndexMap.size(); i2++) {
                    this.mIndexMap.put(this.mIndexMap.keyAt(i2), index);
                }
                SparseIntArray srcMap = srcStream.mIndexMap;
                while (i < srcMap.size()) {
                    setIndex(AudioService.this.rescaleIndex(srcMap.valueAt(i), srcStreamType, this.mStreamType), srcMap.keyAt(i), caller);
                    i++;
                }
            }
        }

        @GuardedBy("VolumeStreamState.class")
        public void setAllIndexesToMax() {
            for (int i = 0; i < this.mIndexMap.size(); i++) {
                this.mIndexMap.put(this.mIndexMap.keyAt(i), this.mIndexMax);
            }
        }

        public void mute(boolean state) {
            boolean changed = false;
            synchronized (VolumeStreamState.class) {
                if (state != this.mIsMuted) {
                    changed = true;
                    this.mIsMuted = state;
                    AudioService.sendMsg(AudioService.this.mAudioHandler, 10, 2, 0, 0, this, 0);
                }
            }
            if (changed) {
                Intent intent = new Intent("android.media.STREAM_MUTE_CHANGED_ACTION");
                intent.putExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", this.mStreamType);
                intent.putExtra("android.media.EXTRA_STREAM_VOLUME_MUTED", state);
                AudioService.this.sendBroadcastToAll(intent);
            }
        }

        public int getStreamType() {
            return this.mStreamType;
        }

        public void checkFixedVolumeDevices() {
            synchronized (VolumeStreamState.class) {
                if (AudioService.mStreamVolumeAlias[this.mStreamType] == 3) {
                    for (int i = 0; i < this.mIndexMap.size(); i++) {
                        int device = this.mIndexMap.keyAt(i);
                        int index = this.mIndexMap.valueAt(i);
                        if (!((AudioService.this.mFullVolumeDevices & device) == 0 && ((AudioService.this.mFixedVolumeDevices & device) == 0 || index == 0))) {
                            this.mIndexMap.put(device, this.mIndexMax);
                        }
                        applyDeviceVolume_syncVSS(device);
                    }
                }
            }
        }

        private int getValidIndex(int index) {
            if (index < this.mIndexMin) {
                return this.mIndexMin;
            }
            if (AudioService.this.mUseFixedVolume || index > this.mIndexMax) {
                return this.mIndexMax;
            }
            return index;
        }

        private void dump(PrintWriter pw) {
            int i;
            int device;
            pw.print("   Muted: ");
            pw.println(this.mIsMuted);
            pw.print("   Min: ");
            pw.println((this.mIndexMin + 5) / 10);
            pw.print("   Max: ");
            pw.println((this.mIndexMax + 5) / 10);
            pw.print("   Current: ");
            int n = 0;
            for (i = 0; i < this.mIndexMap.size(); i++) {
                String deviceName;
                if (i > 0) {
                    pw.print(", ");
                }
                device = this.mIndexMap.keyAt(i);
                pw.print(Integer.toHexString(device));
                if (device == 1073741824) {
                    deviceName = HealthServiceWrapper.INSTANCE_VENDOR;
                } else {
                    deviceName = AudioSystem.getOutputDeviceName(device);
                }
                if (!deviceName.isEmpty()) {
                    pw.print(" (");
                    pw.print(deviceName);
                    pw.print(")");
                }
                pw.print(": ");
                pw.print((this.mIndexMap.valueAt(i) + 5) / 10);
            }
            pw.println();
            pw.print("   Devices: ");
            i = AudioService.this.getDevicesForStream(this.mStreamType);
            device = 0;
            while (true) {
                int i2 = 1 << device;
                int device2 = i2;
                if (i2 != 1073741824) {
                    if ((i & device2) != 0) {
                        i2 = n + 1;
                        if (n > 0) {
                            pw.print(", ");
                        }
                        pw.print(AudioSystem.getOutputDeviceName(device2));
                        n = i2;
                    }
                    device++;
                } else {
                    return;
                }
            }
        }
    }

    class WiredDeviceConnectionState {
        public final String mAddress;
        public final String mCaller;
        public final String mName;
        public final int mState;
        public final int mType;

        public WiredDeviceConnectionState(int type, int state, String address, String name, String caller) {
            this.mType = type;
            this.mState = state;
            this.mAddress = address;
            this.mName = name;
            this.mCaller = caller;
        }
    }

    public static final class Lifecycle extends SystemService {
        private AudioService mService;

        public Lifecycle(Context context) {
            super(context);
            IHwAudioService audioService = HwServiceFactory.getHwAudioService();
            if (audioService != null) {
                this.mService = audioService.getInstance(context);
            } else {
                this.mService = new AudioService(context);
            }
        }

        public void onStart() {
            publishBinderService("audio", this.mService);
        }

        public void onBootPhase(int phase) {
            if (phase == 550) {
                this.mService.systemReady();
            }
        }
    }

    /*  JADX ERROR: NullPointerException in pass: BlockFinish
        java.lang.NullPointerException
        	at jadx.core.dex.visitors.blocksmaker.BlockFinish.fixSplitterBlock(BlockFinish.java:45)
        	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:29)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    protected void adjustStreamVolume(int r33, int r34, int r35, java.lang.String r36, java.lang.String r37, int r38) {
        /*
        r32 = this;
        r8 = r32;
        r9 = r33;
        r10 = r34;
        r1 = r35;
        r0 = r32.checkEnbaleVolumeAdjust();
        if (r0 != 0) goto L_0x000f;
    L_0x000e:
        return;
    L_0x000f:
        r0 = r8.mUseFixedVolume;
        if (r0 == 0) goto L_0x0014;
    L_0x0013:
        return;
    L_0x0014:
        r0 = "AudioService";
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "adjustStreamVolume() stream=";
        r2.append(r3);
        r2.append(r9);
        r3 = ", dir=";
        r2.append(r3);
        r2.append(r10);
        r3 = ", flags=";
        r2.append(r3);
        r2.append(r1);
        r3 = ", caller=";
        r2.append(r3);
        r3 = android.os.Binder.getCallingPid();
        r2.append(r3);
        r2 = r2.toString();
        android.util.Log.d(r0, r2);
        r8.ensureValidDirection(r10);
        r32.ensureValidStreamType(r33);
        r11 = r8.isMuteAdjust(r10);
        if (r11 == 0) goto L_0x0059;
    L_0x0052:
        r0 = r32.isStreamAffectedByMute(r33);
        if (r0 != 0) goto L_0x0059;
    L_0x0058:
        return;
    L_0x0059:
        if (r11 == 0) goto L_0x008e;
    L_0x005b:
        if (r9 != 0) goto L_0x008e;
    L_0x005d:
        r0 = r8.mContext;
        r2 = "android.permission.MODIFY_PHONE_STATE";
        r0 = r0.checkCallingOrSelfPermission(r2);
        if (r0 == 0) goto L_0x008e;
    L_0x0067:
        r0 = "AudioService";
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "MODIFY_PHONE_STATE Permission Denial: adjustStreamVolume from pid=";
        r2.append(r3);
        r3 = android.os.Binder.getCallingPid();
        r2.append(r3);
        r3 = ", uid=";
        r2.append(r3);
        r3 = android.os.Binder.getCallingUid();
        r2.append(r3);
        r2 = r2.toString();
        android.util.Log.w(r0, r2);
        return;
    L_0x008e:
        r0 = mStreamVolumeAlias;
        r12 = r0[r9];
        r0 = r8.mStreamStates;
        r13 = r0[r12];
        r14 = r8.getDeviceForStream(r12);
        r2 = r13.getIndex(r14);
        r15 = 1;
        r16 = 0;
        r0 = r14 & 896;
        if (r0 != 0) goto L_0x00aa;
    L_0x00a5:
        r0 = r1 & 64;
        if (r0 == 0) goto L_0x00aa;
    L_0x00a9:
        return;
    L_0x00aa:
        r0 = 1000; // 0x3e8 float:1.401E-42 double:4.94E-321;
        r3 = r38;
        if (r3 != r0) goto L_0x00be;
    L_0x00b0:
        r0 = r32.getCurrentUserId();
        r4 = android.os.UserHandle.getAppId(r38);
        r0 = android.os.UserHandle.getUid(r0, r4);
        r7 = r0;
        goto L_0x00bf;
    L_0x00be:
        r7 = r3;
    L_0x00bf:
        r0 = r8.mAppOps;	 Catch:{ SecurityException -> 0x0377 }
        r3 = STREAM_VOLUME_OPS;	 Catch:{ SecurityException -> 0x0377 }
        r3 = r3[r12];	 Catch:{ SecurityException -> 0x0377 }
        r6 = r36;	 Catch:{ SecurityException -> 0x0377 }
        r0 = r0.noteOp(r3, r7, r6);	 Catch:{ SecurityException -> 0x0377 }
        if (r0 == 0) goto L_0x00ce;
    L_0x00cd:
        return;
        r3 = r8.mSafeMediaVolumeState;
        monitor-enter(r3);
        r0 = 0;
        r8.mPendingVolumeCommand = r0;	 Catch:{ all -> 0x036e }
        monitor-exit(r3);	 Catch:{ all -> 0x036e }
        r0 = r1 & -33;
        r5 = 3;
        if (r12 != r5) goto L_0x00fd;
    L_0x00db:
        r1 = r8.mFixedVolumeDevices;
        r1 = r1 & r14;
        if (r1 == 0) goto L_0x00fd;
    L_0x00e0:
        r0 = r0 | 32;
        r1 = r8.mSafeMediaVolumeState;
        r1 = r1.intValue();
        if (r1 != r5) goto L_0x00f5;
    L_0x00ea:
        r1 = 603979788; // 0x2400000c float:2.7755615E-17 double:2.98405664E-315;
        r1 = r1 & r14;
        if (r1 == 0) goto L_0x00f5;
    L_0x00f0:
        r1 = r8.safeMediaVolumeIndex(r14);
        goto L_0x00f9;
    L_0x00f5:
        r1 = r13.getMaxIndex();
    L_0x00f9:
        if (r2 == 0) goto L_0x0103;
    L_0x00fb:
        r2 = r1;
        goto L_0x0103;
    L_0x00fd:
        r1 = 10;
        r1 = r8.rescaleIndex(r1, r9, r12);
    L_0x0103:
        r17 = r2;
        r2 = LOUD_VOICE_MODE_SUPPORT;
        if (r2 == 0) goto L_0x011b;
    L_0x0109:
        if (r9 != 0) goto L_0x011b;
    L_0x010b:
        r2 = "true";
        r3 = "VOICE_LVM_Enable";
        r3 = android.media.AudioSystem.getParameters(r3);
        r2 = r2.equals(r3);
        if (r2 == 0) goto L_0x011b;
    L_0x011a:
        r1 = 0;
    L_0x011b:
        r18 = r1;
        r1 = r0 & 2;
        r4 = 0;
        r3 = 1;
        if (r1 != 0) goto L_0x0130;
    L_0x0123:
        r1 = r32.getUiSoundsStreamType();
        if (r12 != r1) goto L_0x012a;
    L_0x0129:
        goto L_0x0130;
    L_0x012a:
        r19 = r7;
        r2 = r15;
        r7 = r0;
        r15 = r3;
        goto L_0x0170;
    L_0x0130:
        r2 = r32.getRingerModeInternal();
        if (r2 != r3) goto L_0x0138;
    L_0x0136:
        r0 = r0 & -17;
        r19 = r13.mIsMuted;
        r1 = r8;
        r20 = r2;
        r2 = r17;
        r21 = r15;
        r15 = r3;
        r3 = r10;
        r4 = r18;
        r5 = r19;
        r6 = r36;
        r19 = r7;
        r7 = r0;
        r1 = r1.checkForRingerModeChange(r2, r3, r4, r5, r6, r7);
        r2 = r1 & 1;
        if (r2 == 0) goto L_0x0159;
    L_0x0157:
        r2 = r15;
        goto L_0x015a;
    L_0x0159:
        r2 = 0;
    L_0x015a:
        r3 = r1 & 2;
        if (r3 == 0) goto L_0x0160;
    L_0x015e:
        r3 = r15;
        goto L_0x0161;
    L_0x0160:
        r3 = 0;
    L_0x0161:
        r16 = r3;
        r3 = r1 & 128;
        if (r3 == 0) goto L_0x0169;
    L_0x0167:
        r0 = r0 | 128;
    L_0x0169:
        r3 = r1 & 2048;
        if (r3 == 0) goto L_0x016f;
    L_0x016d:
        r0 = r0 | 2048;
    L_0x016f:
        r7 = r0;
    L_0x0170:
        r0 = r8.volumeAdjustmentAllowedByDnd(r12, r7);
        if (r0 != 0) goto L_0x0177;
    L_0x0176:
        r2 = 0;
    L_0x0177:
        r20 = r2;
        r0 = r8.mStreamStates;
        r0 = r0[r9];
        r6 = r0.getIndex(r14);
        if (r20 == 0) goto L_0x02e5;
    L_0x0183:
        if (r10 == 0) goto L_0x02e5;
    L_0x0185:
        r0 = r8.mAudioHandler;
        r5 = 24;
        r0.removeMessages(r5);
        r0 = -1;
        if (r11 == 0) goto L_0x01cf;
    L_0x018f:
        r1 = 101; // 0x65 float:1.42E-43 double:5.0E-322;
        if (r10 != r1) goto L_0x0199;
    L_0x0193:
        r1 = r13.mIsMuted;
        r1 = r1 ^ r15;
        goto L_0x01a0;
    L_0x0199:
        r1 = -100;
        if (r10 != r1) goto L_0x019f;
    L_0x019d:
        r1 = r15;
        goto L_0x01a0;
    L_0x019f:
        r1 = 0;
    L_0x01a0:
        r4 = 3;
        if (r12 != r4) goto L_0x01a6;
    L_0x01a3:
        r8.setSystemAudioMute(r1);
    L_0x01a6:
        r2 = 0;
    L_0x01a7:
        r3 = r8.mStreamStates;
        r3 = r3.length;
        if (r2 >= r3) goto L_0x01f4;
    L_0x01ac:
        r3 = mStreamVolumeAlias;
        r3 = r3[r2];
        if (r12 != r3) goto L_0x01ca;
    L_0x01b2:
        r3 = r32.readCameraSoundForced();
        if (r3 == 0) goto L_0x01c3;
    L_0x01b8:
        r3 = r8.mStreamStates;
        r3 = r3[r2];
        r3 = r3.getStreamType();
        r4 = 7;
        if (r3 == r4) goto L_0x01ca;
    L_0x01c3:
        r3 = r8.mStreamStates;
        r3 = r3[r2];
        r3.mute(r1);
    L_0x01ca:
        r2 = r2 + 1;
        r4 = 3;
        goto L_0x01a7;
    L_0x01ce:
        goto L_0x01f4;
    L_0x01cf:
        if (r10 != r15) goto L_0x01fc;
    L_0x01d1:
        r1 = r17 + r18;
        r1 = r8.checkSafeMediaVolume(r12, r1, r14);
        if (r1 != 0) goto L_0x01fc;
    L_0x01d9:
        r1 = "AudioService";
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "adjustStreamVolume() safe volume index = ";
        r2.append(r3);
        r2.append(r6);
        r2 = r2.toString();
        android.util.Log.e(r1, r2);
        r1 = r8.mVolumeController;
        r1.postDisplaySafeVolumeWarning(r7);
    L_0x01f4:
        r21 = r5;
        r27 = r6;
        r28 = r7;
        r15 = 3;
        goto L_0x0256;
    L_0x01fc:
        r1 = r10 * r18;
        r4 = r37;
        r1 = r13.adjustIndex(r1, r14, r4);
        if (r1 != 0) goto L_0x020c;
    L_0x0206:
        r1 = r13.mIsMuted;
        if (r1 == 0) goto L_0x01f4;
    L_0x020c:
        r1 = r13.mIsMuted;
        if (r1 == 0) goto L_0x0244;
    L_0x0212:
        if (r10 != r15) goto L_0x0220;
    L_0x0214:
        r3 = 0;
        r13.mute(r3);
        r21 = r5;
        r27 = r6;
        r28 = r7;
        r15 = 3;
        goto L_0x024b;
    L_0x0220:
        r3 = 0;
        if (r10 != r0) goto L_0x0244;
    L_0x0223:
        r1 = r8.mIsSingleVolume;
        if (r1 == 0) goto L_0x0244;
    L_0x0227:
        r1 = r8.mAudioHandler;
        r2 = 24;
        r21 = 2;
        r22 = 0;
        r23 = 350; // 0x15e float:4.9E-43 double:1.73E-321;
        r3 = r21;
        r15 = 3;
        r4 = r12;
        r21 = r5;
        r5 = r7;
        r27 = r6;
        r6 = r22;
        r28 = r7;
        r7 = r23;
        sendMsg(r1, r2, r3, r4, r5, r6, r7);
        goto L_0x024b;
    L_0x0244:
        r21 = r5;
        r27 = r6;
        r28 = r7;
        r15 = 3;
    L_0x024b:
        r1 = r8.mAudioHandler;
        r2 = 0;
        r3 = 2;
        r5 = 0;
        r7 = 0;
        r4 = r14;
        r6 = r13;
        sendMsg(r1, r2, r3, r4, r5, r6, r7);
    L_0x0256:
        r1 = r8.mStreamStates;
        r1 = r1[r9];
        r1 = r1.getIndex(r14);
        if (r12 != r15) goto L_0x0281;
    L_0x0260:
        r2 = r14 & 896;
        if (r2 == 0) goto L_0x0281;
    L_0x0264:
        r7 = r28;
        r2 = r7 & 64;
        if (r2 != 0) goto L_0x0283;
    L_0x026a:
        r2 = r8.mA2dpAvrcpLock;
        monitor-enter(r2);
        r3 = r8.mA2dp;
        if (r3 == 0) goto L_0x027c;
    L_0x0271:
        r3 = r8.mAvrcpAbsVolSupported;
        if (r3 == 0) goto L_0x027c;
    L_0x0275:
        r3 = r8.mA2dp;
        r4 = r1 / 10;
        r3.setAvrcpAbsoluteVolume(r4);
    L_0x027c:
        monitor-exit(r2);
        goto L_0x0283;
    L_0x027e:
        r0 = move-exception;
        monitor-exit(r2);
        throw r0;
    L_0x0281:
        r7 = r28;
    L_0x0283:
        r2 = 134217728; // 0x8000000 float:3.85186E-34 double:6.63123685E-316;
        r2 = r2 & r14;
        if (r2 == 0) goto L_0x028b;
    L_0x0288:
        r8.setHearingAidVolume(r1, r9);
    L_0x028b:
        if (r12 != r15) goto L_0x0297;
    L_0x028d:
        r2 = r32.getStreamMaxVolume(r33);
        r6 = r27;
        r8.setSystemAudioVolume(r6, r1, r2, r7);
        goto L_0x0299;
    L_0x0297:
        r6 = r27;
    L_0x0299:
        r2 = r8.mHdmiManager;
        if (r2 == 0) goto L_0x02e0;
    L_0x029d:
        r2 = r8.mHdmiManager;
        monitor-enter(r2);
        r3 = r8.mHdmiCecSink;
        if (r3 == 0) goto L_0x02db;
    L_0x02a4:
        if (r12 != r15) goto L_0x02db;
    L_0x02a6:
        if (r6 == r1) goto L_0x02db;
    L_0x02a8:
        r3 = r8.mHdmiPlaybackClient;
        monitor-enter(r3);
        if (r10 != r0) goto L_0x02b0;
    L_0x02ad:
        r5 = 25;
        goto L_0x02b2;
    L_0x02b0:
        r5 = r21;
    L_0x02b2:
        r4 = r5;
        r21 = android.os.Binder.clearCallingIdentity();	 Catch:{ all -> 0x02cf }
        r29 = r21;
        r0 = r8.mHdmiPlaybackClient;	 Catch:{ all -> 0x02cf }
        r5 = 1;	 Catch:{ all -> 0x02cf }
        r0.sendKeyEvent(r4, r5);	 Catch:{ all -> 0x02cf }
        r0 = r8.mHdmiPlaybackClient;	 Catch:{ all -> 0x02cf }
        r5 = 0;	 Catch:{ all -> 0x02cf }
        r0.sendKeyEvent(r4, r5);	 Catch:{ all -> 0x02cf }
        r31 = r4;
        r4 = r29;
        android.os.Binder.restoreCallingIdentity(r4);	 Catch:{ all -> 0x02cf }
        monitor-exit(r3);	 Catch:{ all -> 0x02cf }
        goto L_0x02db;	 Catch:{ all -> 0x02cf }
    L_0x02cf:
        r0 = move-exception;	 Catch:{ all -> 0x02cf }
        r31 = r4;	 Catch:{ all -> 0x02cf }
        r4 = r29;	 Catch:{ all -> 0x02cf }
        android.os.Binder.restoreCallingIdentity(r4);	 Catch:{ all -> 0x02cf }
        throw r0;	 Catch:{ all -> 0x02cf }
    L_0x02d8:
        r0 = move-exception;	 Catch:{ all -> 0x02cf }
        monitor-exit(r3);	 Catch:{ all -> 0x02cf }
        throw r0;
    L_0x02db:
        monitor-exit(r2);
        goto L_0x02e0;
    L_0x02dd:
        r0 = move-exception;
        monitor-exit(r2);
        throw r0;
        r15 = r6;
        r21 = r7;
        goto L_0x02fa;
    L_0x02e5:
        if (r16 == 0) goto L_0x02f7;
    L_0x02e7:
        r1 = r8.mAudioHandler;
        r2 = 0;
        r3 = 2;
        r5 = 0;
        r0 = 0;
        r4 = r14;
        r15 = r6;
        r6 = r13;
        r21 = r7;
        r7 = r0;
        sendMsg(r1, r2, r3, r4, r5, r6, r7);
        goto L_0x02fa;
    L_0x02f7:
        r15 = r6;
        r21 = r7;
    L_0x02fa:
        r0 = r8.mStreamStates;
        r0 = r0[r9];
        r0 = r0.getIndex(r14);
        r1 = r21 & 4;
        if (r1 == 0) goto L_0x0315;
    L_0x0306:
        r1 = r8.mKeyguardManager;
        if (r1 == 0) goto L_0x0315;
    L_0x030a:
        r1 = r8.mKeyguardManager;
        r1 = r1.isKeyguardLocked();
        if (r1 == 0) goto L_0x0315;
    L_0x0312:
        r7 = r21 & -5;
        goto L_0x0317;
    L_0x0315:
        r7 = r21;
    L_0x0317:
        r1 = r7 & 1;
        if (r1 == 0) goto L_0x0321;
    L_0x031b:
        r1 = r8.mScreenOn;
        if (r1 != 0) goto L_0x0321;
    L_0x031f:
        r7 = r7 & -2;
    L_0x0321:
        r1 = "AudioService";
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "adjustStreamVolume() stream=";
        r2.append(r3);
        r2.append(r9);
        r3 = ", flags=";
        r2.append(r3);
        r2.append(r7);
        r3 = ",mScreenOn= ";
        r2.append(r3);
        r3 = r8.mScreenOn;
        r2.append(r3);
        r2 = r2.toString();
        android.util.Log.d(r1, r2);
        r8.sendVolumeUpdate(r9, r15, r0, r7);
        r1 = LOUD_VOICE_MODE_SUPPORT;
        if (r1 == 0) goto L_0x036d;
    L_0x0350:
        r6 = r8.mAudioHandler;
        r22 = 10001; // 0x2711 float:1.4014E-41 double:4.941E-320;
        r23 = 0;
        r24 = 1;
        r25 = 0;
        r26 = new com.android.server.audio.AbsAudioService$DeviceVolumeState;
        r1 = r26;
        r2 = r8;
        r3 = r10;
        r4 = r14;
        r5 = r15;
        r21 = r6;
        r6 = r9;
        r1.<init>(r3, r4, r5, r6);
        r27 = 0;
        sendMsg(r21, r22, r23, r24, r25, r26, r27);
    L_0x036d:
        return;
    L_0x036e:
        r0 = move-exception;
        r19 = r7;
        r21 = r15;
    L_0x0373:
        monitor-exit(r3);	 Catch:{ all -> 0x0375 }
        throw r0;
    L_0x0375:
        r0 = move-exception;
        goto L_0x0373;
    L_0x0377:
        r0 = move-exception;
        r19 = r7;
        r21 = r15;
        r3 = "AudioService";
        r4 = "mAppOps.noteOp cannot match the uid and packagename";
        android.util.Log.e(r3, r4);
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.audio.AudioService.adjustStreamVolume(int, int, int, java.lang.String, java.lang.String, int):void");
    }

    private boolean isPlatformVoice() {
        return this.mPlatformType == 1;
    }

    private boolean isPlatformTelevision() {
        return this.mPlatformType == 2;
    }

    private boolean isPlatformAutomotive() {
        return this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive");
    }

    private String makeDeviceListKey(int device, String deviceAddress) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(device));
        stringBuilder.append(":");
        stringBuilder.append(deviceAddress);
        return stringBuilder.toString();
    }

    public static String makeAlsaAddressString(int card, int device) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("card=");
        stringBuilder.append(card);
        stringBuilder.append(";device=");
        stringBuilder.append(device);
        stringBuilder.append(";");
        return stringBuilder.toString();
    }

    public AudioService(Context context) {
        int maxStreamVolumeFromDtsi;
        int i;
        int i2;
        Context context2 = context;
        this.mContext = context2;
        this.mContentResolver = context.getContentResolver();
        this.mCust = (HwCustAudioService) HwCustUtils.createObj(HwCustAudioService.class, new Object[]{this.mContext});
        this.mAppOps = (AppOpsManager) context2.getSystemService("appops");
        this.mPlatformType = AudioSystem.getPlatformType(context);
        this.mIsSingleVolume = AudioSystem.isSingleVolume(context);
        this.mUserManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mAudioEventWakeLock = ((PowerManager) context2.getSystemService("power")).newWakeLock(1, "handleAudioEvent");
        this.mVibrator = (Vibrator) context2.getSystemService("vibrator");
        this.mHasVibrator = this.mVibrator == null ? false : this.mVibrator.hasVibrator();
        String maxStreamVolumeFromDtsiString = AudioSystem.getParameters("audio_capability#max_stream_volume");
        if (maxStreamVolumeFromDtsiString != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("max_stream_volume: ");
            stringBuilder.append(maxStreamVolumeFromDtsiString);
            Log.i(str, stringBuilder.toString());
            try {
                maxStreamVolumeFromDtsi = Integer.parseInt(maxStreamVolumeFromDtsiString);
                for (i = 0; i < MAX_STREAM_VOLUME.length; i++) {
                    MAX_STREAM_VOLUME[i] = maxStreamVolumeFromDtsi;
                }
            } catch (NumberFormatException e) {
                Log.i(TAG, "cannot parse max_stream_volume, use default.");
            }
        }
        maxStreamVolumeFromDtsi = SystemProperties.getInt("ro.config.media_vol_steps", -1);
        if (maxStreamVolumeFromDtsi != -1) {
            MAX_STREAM_VOLUME[3] = maxStreamVolumeFromDtsi;
        }
        int defaultMusicVolume = SystemProperties.getInt("ro.config.media_vol_default", -1);
        if (defaultMusicVolume != -1 && defaultMusicVolume <= MAX_STREAM_VOLUME[3]) {
            AudioSystem.DEFAULT_STREAM_VOLUME[3] = defaultMusicVolume;
        } else if (isPlatformTelevision()) {
            AudioSystem.DEFAULT_STREAM_VOLUME[3] = MAX_STREAM_VOLUME[3] / 4;
        } else {
            AudioSystem.DEFAULT_STREAM_VOLUME[3] = MAX_STREAM_VOLUME[3] / 3;
        }
        int maxVolume = SystemProperties.getInt("ro.config.vol_steps", -1);
        if (maxVolume > 0) {
            for (i = 0; i < MAX_STREAM_VOLUME.length; i++) {
                MAX_STREAM_VOLUME[i] = maxVolume;
            }
        }
        int voiceCallMaxVolume = SystemProperties.getInt("ro.config.vc_call_vol_steps", -1);
        if (voiceCallMaxVolume > 0) {
            MAX_STREAM_VOLUME[0] = voiceCallMaxVolume;
            AudioSystem.DEFAULT_STREAM_VOLUME[0] = (voiceCallMaxVolume * 4) / 5;
        }
        int maxAlarmVolume = SystemProperties.getInt("ro.config.alarm_vol_steps", -1);
        if (maxAlarmVolume != -1) {
            MAX_STREAM_VOLUME[4] = maxAlarmVolume;
        }
        i = SystemProperties.getInt("ro.config.alarm_vol_default", -1);
        if (i == -1 || i > MAX_STREAM_VOLUME[4]) {
            AudioSystem.DEFAULT_STREAM_VOLUME[4] = (6 * MAX_STREAM_VOLUME[4]) / 7;
        } else {
            AudioSystem.DEFAULT_STREAM_VOLUME[4] = i;
        }
        int maxSystemVolume = SystemProperties.getInt("ro.config.system_vol_steps", -1);
        if (maxSystemVolume != -1) {
            MAX_STREAM_VOLUME[1] = maxSystemVolume;
        }
        int defaultSystemVolume = SystemProperties.getInt("ro.config.system_vol_default", -1);
        if (defaultSystemVolume == -1 || defaultSystemVolume > MAX_STREAM_VOLUME[1]) {
            AudioSystem.DEFAULT_STREAM_VOLUME[1] = MAX_STREAM_VOLUME[1];
        } else {
            AudioSystem.DEFAULT_STREAM_VOLUME[1] = defaultSystemVolume;
        }
        sSoundEffectVolumeDb = context.getResources().getInteger(17694867);
        this.mForcedUseForComm = 0;
        createAudioSystemThread();
        AudioSystem.setErrorCallback(this.mAudioSystemCallback);
        boolean cameraSoundForced = readCameraSoundForced();
        this.mCameraSoundForced = new Boolean(cameraSoundForced).booleanValue();
        Handler handler = this.mAudioHandler;
        if (cameraSoundForced) {
            i2 = 11;
        } else {
            i2 = 0;
        }
        sendMsg(handler, 8, 2, 4, i2, new String("AudioService ctor"), 0);
        int maxSystemVolume2 = maxSystemVolume;
        this.mSafeMediaVolumeState = new Integer(Global.getInt(this.mContentResolver, "audio_safe_volume_state", 0));
        this.mSafeMediaVolumeIndex = this.mContext.getResources().getInteger(17694852) * 10;
        if (usingHwSafeMediaConfig()) {
            this.mSafeMediaVolumeIndex = getHwSafeMediaVolumeIndex();
            this.mSafeMediaVolumeState = Integer.valueOf(isHwSafeMediaVolumeEnabled() ? new Integer(Global.getInt(this.mContentResolver, "audio_safe_volume_state", 0)).intValue() : 1);
        }
        this.mIsChineseZone = SystemProperties.getInt("ro.config.hw_optb", CHINAZONE_IDENTIFIER) == CHINAZONE_IDENTIFIER;
        this.mUseFixedVolume = this.mContext.getResources().getBoolean(17957059);
        updateStreamVolumeAlias(false, TAG);
        readPersistedSettings();
        readUserRestrictions();
        this.mSettingsObserver = new SettingsObserver();
        createStreamStates();
        this.mSafeUsbMediaVolumeIndex = getSafeUsbMediaVolumeIndex();
        this.mPlaybackMonitor = new PlaybackActivityMonitor(context2, MAX_STREAM_VOLUME[4]);
        MediaFocusControl mediaFocusControl = HwServiceFactory.getHwMediaFocusControl(this.mContext, this.mPlaybackMonitor);
        if (mediaFocusControl != null) {
            this.mMediaFocusControl = mediaFocusControl;
        } else {
            this.mMediaFocusControl = new MediaFocusControl(this.mContext, this.mPlaybackMonitor);
        }
        this.mPlaybackMonitor.setMediaFocusControl(this.mMediaFocusControl);
        this.mRecordMonitor = new RecordingActivityMonitor(this.mContext);
        readAndSetLowRamDevice();
        this.mRingerAndZenModeMutedStreams = 0;
        setRingerModeInt(getRingerModeInternal(), false);
        IntentFilter intentFilter = new IntentFilter("android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.headset.profile.action.ACTIVE_DEVICE_CHANGED");
        intentFilter.addAction("android.intent.action.DOCK_EVENT");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.USER_PRESENT");
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_BACKGROUND");
        intentFilter.addAction("android.intent.action.USER_FOREGROUND");
        intentFilter.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        intentFilter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        intentFilter.addAction("android.intent.action.FM");
        intentFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        boolean z = SystemProperties.getBoolean("ro.audio.monitorRotation", false) || SPK_RCV_STEREO_SUPPORT;
        this.mMonitorRotation = z;
        if (this.mMonitorRotation) {
            RotationHelper.init(this.mContext, this.mAudioHandler);
        }
        intentFilter.addAction("android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION");
        intentFilter.addAction("android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION");
        if (!this.mIsChineseZone) {
            intentFilter.addAction(ACTION_CHECK_MUSIC_ACTIVE);
        }
        context2.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, intentFilter, null, null);
        LocalServices.addService(AudioManagerInternal.class, new AudioServiceInternal());
        this.mUserManagerInternal.addUserRestrictionsListener(this.mUserRestrictionsListener);
        this.mRecordMonitor.initMonitor();
        this.mIsHisiPlatform = isHisiPlatform();
        if (!this.mIsChineseZone) {
            Intent intentCheckMusicActive = new Intent();
            intentCheckMusicActive.setAction(ACTION_CHECK_MUSIC_ACTIVE);
            this.mPendingIntent = PendingIntent.getBroadcast(this.mContext, 0, intentCheckMusicActive, 268435456);
            this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        }
        this.mHwAudioServiceEx = HwServiceExFactory.getHwAudioServiceEx(this, context);
    }

    public void systemReady() {
        sendMsg(this.mAudioHandler, 21, 2, 0, 0, null, 0);
    }

    public void onSystemReady() {
        this.mSystemReady = true;
        this.mHwAudioServiceEx.setSystemReady();
        sendMsg(this.mAudioHandler, 7, 2, 0, 0, null, 0);
        this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        this.mScoConnectionState = -1;
        resetBluetoothSco();
        getBluetoothHeadset();
        Intent newIntent = new Intent("android.media.SCO_AUDIO_STATE_CHANGED");
        boolean z = false;
        newIntent.putExtra("android.media.extra.SCO_AUDIO_STATE", 0);
        sendStickyBroadcastToAll(newIntent);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.getProfileProxy(this.mContext, this.mBluetoothProfileServiceListener, 2);
            adapter.getProfileProxy(this.mContext, this.mBluetoothProfileServiceListener, 21);
        }
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.hdmi.cec")) {
            this.mHdmiManager = (HdmiControlManager) this.mContext.getSystemService(HdmiControlManager.class);
            synchronized (this.mHdmiManager) {
                this.mHdmiTvClient = this.mHdmiManager.getTvClient();
                if (this.mHdmiTvClient != null) {
                    this.mFixedVolumeDevices &= -2883587;
                }
                this.mHdmiPlaybackClient = this.mHdmiManager.getPlaybackClient();
                this.mHdmiCecSink = false;
            }
        }
        this.mNm = (NotificationManager) this.mContext.getSystemService("notification");
        Handler handler = this.mAudioHandler;
        String str = TAG;
        if (!SystemProperties.getBoolean("audio.safemedia.bypass", false)) {
            z = true;
        }
        sendMsg(handler, 17, 0, 0, 0, str, z);
        initA11yMonitoring();
        onIndicateSystemReady();
    }

    void onIndicateSystemReady() {
        if (AudioSystem.systemReady() != 0) {
            sendMsg(this.mAudioHandler, 26, 0, 0, 0, null, 1000);
        }
    }

    public void onAudioServerDied() {
        if (this.mSystemReady && AudioSystem.checkAudioFlinger() == 0) {
            int forDock;
            int i;
            int forSys;
            int i2;
            Log.e(TAG, "Audioserver started.");
            AudioSystem.setParameters("restarting=true");
            readAndSetLowRamDevice();
            synchronized (this.mConnectedDevices) {
                forDock = 0;
                for (i = 0; i < this.mConnectedDevices.size(); i++) {
                    DeviceListSpec spec = (DeviceListSpec) this.mConnectedDevices.valueAt(i);
                    AudioSystem.setDeviceConnectionState(spec.mDeviceType, 1, spec.mDeviceAddress, spec.mDeviceName);
                }
            }
            if (AudioSystem.setPhoneState(this.mMode) == 0) {
                AudioEventLogger audioEventLogger = this.mModeLogger;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onAudioServerDied causes setPhoneState(");
                stringBuilder.append(AudioSystem.modeToString(this.mMode));
                stringBuilder.append(")");
                audioEventLogger.log(new StringEvent(stringBuilder.toString()));
            }
            this.mForceUseLogger.log(new ForceUseEvent(0, this.mForcedUseForComm, "onAudioServerDied"));
            AudioSystem.setForceUse(0, this.mForcedUseForComm);
            this.mForceUseLogger.log(new ForceUseEvent(2, this.mForcedUseForComm, "onAudioServerDied"));
            AudioSystem.setForceUse(2, this.mForcedUseForComm);
            synchronized (this.mSettingsLock) {
                forSys = this.mCameraSoundForced ? 11 : 0;
            }
            this.mForceUseLogger.log(new ForceUseEvent(4, forSys, "onAudioServerDied"));
            AudioSystem.setForceUse(4, forSys);
            sendCommForceBroadcast();
            updateAftPolicy();
            i = AudioSystem.getNumStreamTypes() - 1;
            while (true) {
                i2 = 10;
                if (i < 0) {
                    break;
                }
                VolumeStreamState streamState = this.mStreamStates[i];
                if (streamState != null) {
                    AudioSystem.initStreamVolume(i, streamState.mIndexMin / 10, streamState.mIndexMax / 10);
                    streamState.applyAllVolumes();
                }
                i--;
            }
            updateMasterMono(this.mContentResolver);
            setRingerModeInt(getRingerModeInternal(), false);
            this.mHwAudioServiceEx.processAudioServerRestart();
            processMediaServerRestart();
            if (this.mMonitorRotation) {
                RotationHelper.updateOrientation();
            }
            synchronized (this.mBluetoothA2dpEnabledLock) {
                if (this.mBluetoothA2dpEnabled) {
                    i2 = 0;
                }
                i = i2;
                this.mForceUseLogger.log(new ForceUseEvent(1, i, "onAudioServerDied"));
                AudioSystem.setForceUse(1, i);
            }
            synchronized (this.mSettingsLock) {
                if (this.mDockAudioMediaEnabled) {
                    forDock = 8;
                }
                this.mForceUseLogger.log(new ForceUseEvent(3, forDock, "onAudioServerDied"));
                AudioSystem.setForceUse(3, forDock);
                sendEncodedSurroundMode(this.mContentResolver, "onAudioServerDied");
                sendEnabledSurroundFormats(this.mContentResolver, true);
            }
            if (this.mHdmiManager != null) {
                synchronized (this.mHdmiManager) {
                    if (this.mHdmiTvClient != null) {
                        setHdmiSystemAudioSupported(this.mHdmiSystemAudioSupported);
                    }
                }
            }
            synchronized (this.mAudioPolicies) {
                for (AudioPolicyProxy policy : this.mAudioPolicies.values()) {
                    policy.connectMixes();
                }
            }
            onIndicateSystemReady();
            AudioSystem.setParameters("restarting=false");
            if (LOUD_VOICE_MODE_SUPPORT) {
                sendMsg(this.mAudioHandler, 10001, 0, 0, 0, null, 500);
            }
            sendMsg(this.mAudioHandler, 29, 2, 1, 0, null, 0);
            return;
        }
        Log.e(TAG, "Audioserver died.");
        sendMsg(this.mAudioHandler, 4, 1, 0, 0, null, 500);
    }

    private void onDispatchAudioServerStateChange(boolean state) {
        synchronized (this.mAudioServerStateListeners) {
            for (AsdProxy asdp : this.mAudioServerStateListeners.values()) {
                try {
                    asdp.callback().dispatchAudioServerStateChange(state);
                } catch (RemoteException e) {
                    Log.w(TAG, "Could not call dispatchAudioServerStateChange()", e);
                }
            }
        }
    }

    private void createAudioSystemThread() {
        this.mAudioSystemThread = new AudioSystemThread();
        this.mAudioSystemThread.start();
        waitForAudioHandlerCreation();
    }

    private void waitForAudioHandlerCreation() {
        synchronized (this) {
            while (this.mAudioHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupted while waiting on volume handler.");
                }
            }
        }
    }

    private void checkAllAliasStreamVolumes() {
        synchronized (this.mSettingsLock) {
            synchronized (VolumeStreamState.class) {
                int numStreamTypes = AudioSystem.getNumStreamTypes();
                for (int streamType = 0; streamType < numStreamTypes; streamType++) {
                    this.mStreamStates[streamType].setAllIndexes(this.mStreamStates[mStreamVolumeAlias[streamType]], TAG);
                    if (!this.mStreamStates[streamType].mIsMuted) {
                        this.mStreamStates[streamType].applyAllVolumes();
                    }
                }
            }
        }
    }

    private void checkAllFixedVolumeDevices() {
        int numStreamTypes = AudioSystem.getNumStreamTypes();
        for (int streamType = 0; streamType < numStreamTypes; streamType++) {
            this.mStreamStates[streamType].checkFixedVolumeDevices();
        }
    }

    private void checkAllFixedVolumeDevices(int streamType) {
        this.mStreamStates[streamType].checkFixedVolumeDevices();
    }

    private void checkMuteAffectedStreams() {
        for (VolumeStreamState vss : this.mStreamStates) {
            if (vss.mIndexMin > 0 && vss.mStreamType != 0) {
                this.mMuteAffectedStreams &= ~(1 << vss.mStreamType);
            }
        }
    }

    private void createStreamStates() {
        int numStreamTypes = AudioSystem.getNumStreamTypes();
        VolumeStreamState[] streams = new VolumeStreamState[numStreamTypes];
        this.mStreamStates = streams;
        for (int i = 0; i < numStreamTypes; i++) {
            streams[i] = new VolumeStreamState(this, System.VOLUME_SETTINGS_INT[mStreamVolumeAlias[i]], i, null);
        }
        checkAllFixedVolumeDevices();
        checkAllAliasStreamVolumes();
        checkMuteAffectedStreams();
        updateDefaultVolumes();
    }

    private void updateDefaultVolumes() {
        for (int stream = 0; stream < this.mStreamStates.length; stream++) {
            if (stream != mStreamVolumeAlias[stream]) {
                AudioSystem.DEFAULT_STREAM_VOLUME[stream] = rescaleIndex(AudioSystem.DEFAULT_STREAM_VOLUME[mStreamVolumeAlias[stream]], mStreamVolumeAlias[stream], stream);
            }
        }
    }

    private void dumpStreamStates(PrintWriter pw) {
        pw.println("\nStream volumes (device: index)");
        int numStreamTypes = AudioSystem.getNumStreamTypes();
        for (int i = 0; i < numStreamTypes; i++) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("- ");
            stringBuilder.append(AudioSystem.STREAM_NAMES[i]);
            stringBuilder.append(":");
            pw.println(stringBuilder.toString());
            this.mStreamStates[i].dump(pw);
            pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        }
        pw.print("\n- mute affected streams = 0x");
        pw.println(Integer.toHexString(this.mMuteAffectedStreams));
    }

    private void updateStreamVolumeAlias(boolean updateVolumes, String caller) {
        String str = caller;
        int dtmfStreamAlias = 3;
        int a11yStreamAlias = sIndependentA11yVolume ? 10 : 3;
        if (this.mIsSingleVolume) {
            mStreamVolumeAlias = this.STREAM_VOLUME_ALIAS_TELEVISION;
            dtmfStreamAlias = 3;
        } else if (this.mPlatformType != 1) {
            mStreamVolumeAlias = this.STREAM_VOLUME_ALIAS_DEFAULT;
        } else {
            mStreamVolumeAlias = this.STREAM_VOLUME_ALIAS_VOICE;
            dtmfStreamAlias = 2;
        }
        int dtmfStreamAlias2 = dtmfStreamAlias;
        if (this.mIsSingleVolume) {
            this.mRingerModeAffectedStreams = 0;
        } else if (isInCommunication()) {
            dtmfStreamAlias2 = 0;
            this.mRingerModeAffectedStreams &= -257;
        } else {
            this.mRingerModeAffectedStreams |= 256;
        }
        dtmfStreamAlias = dtmfStreamAlias2;
        mStreamVolumeAlias[8] = dtmfStreamAlias;
        mStreamVolumeAlias[10] = a11yStreamAlias;
        if (updateVolumes && this.mStreamStates != null) {
            updateDefaultVolumes();
            synchronized (this.mSettingsLock) {
                synchronized (VolumeStreamState.class) {
                    this.mStreamStates[8].setAllIndexes(this.mStreamStates[dtmfStreamAlias], str);
                    this.mStreamStates[10].mVolumeIndexSettingName = System.VOLUME_SETTINGS_INT[a11yStreamAlias];
                    this.mStreamStates[10].setAllIndexes(this.mStreamStates[a11yStreamAlias], str);
                    this.mStreamStates[10].refreshRange(mStreamVolumeAlias[10]);
                }
            }
            if (sIndependentA11yVolume) {
                this.mStreamStates[10].readSettings();
            }
            setRingerModeInt(getRingerModeInternal(), false);
            sendMsg(this.mAudioHandler, 10, 2, 0, 0, this.mStreamStates[8], 0);
            sendMsg(this.mAudioHandler, 10, 2, 0, 0, this.mStreamStates[10], 0);
        }
    }

    private void readDockAudioSettings(ContentResolver cr) {
        int i = 0;
        boolean z = true;
        if (Global.getInt(cr, "dock_audio_media_enabled", 0) != 1) {
            z = false;
        }
        this.mDockAudioMediaEnabled = z;
        Handler handler = this.mAudioHandler;
        if (this.mDockAudioMediaEnabled) {
            i = 8;
        }
        sendMsg(handler, 8, 2, 3, i, new String("readDockAudioSettings"), 0);
    }

    private void updateMasterMono(ContentResolver cr) {
        Log.d(TAG, String.format("Master mono %b", new Object[]{Boolean.valueOf(System.getIntForUser(cr, "master_mono", 0, -2) == 1)}));
        AudioSystem.setMasterMono(masterMono);
    }

    private void sendEncodedSurroundMode(ContentResolver cr, String eventSource) {
        sendEncodedSurroundMode(Global.getInt(cr, "encoded_surround_output", 0), eventSource);
    }

    private void sendEncodedSurroundMode(int encodedSurroundMode, String eventSource) {
        int forceSetting = 18;
        switch (encodedSurroundMode) {
            case 0:
                forceSetting = 0;
                break;
            case 1:
                forceSetting = 13;
                break;
            case 2:
                forceSetting = 14;
                break;
            case 3:
                forceSetting = 15;
                break;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateSurroundSoundSettings: illegal value ");
                stringBuilder.append(encodedSurroundMode);
                Log.e(str, stringBuilder.toString());
                break;
        }
        if (forceSetting != 18) {
            sendMsg(this.mAudioHandler, 8, 2, 6, forceSetting, eventSource, 0);
        }
    }

    private void sendEnabledSurroundFormats(ContentResolver cr, boolean forceUpdate) {
        if (this.mEncodedSurroundMode == 3) {
            String enabledSurroundFormats = Global.getString(cr, "encoded_surround_output_enabled_formats");
            if (enabledSurroundFormats == null) {
                enabledSurroundFormats = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            }
            if (forceUpdate || !TextUtils.equals(enabledSurroundFormats, this.mEnabledSurroundFormats)) {
                this.mEnabledSurroundFormats = enabledSurroundFormats;
                String[] surroundFormats = TextUtils.split(enabledSurroundFormats, ",");
                ArrayList<Integer> formats = new ArrayList();
                for (String format : surroundFormats) {
                    try {
                        int audioFormat = Integer.valueOf(format).intValue();
                        boolean isSurroundFormat = false;
                        for (int sf : AudioFormat.SURROUND_SOUND_ENCODING) {
                            if (sf == audioFormat) {
                                isSurroundFormat = true;
                                break;
                            }
                        }
                        if (isSurroundFormat && !formats.contains(Integer.valueOf(audioFormat))) {
                            formats.add(Integer.valueOf(audioFormat));
                        }
                    } catch (Exception e) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Invalid enabled surround format:");
                        stringBuilder.append(format);
                        Log.e(str, stringBuilder.toString());
                    }
                }
                Global.putString(this.mContext.getContentResolver(), "encoded_surround_output_enabled_formats", TextUtils.join(",", formats));
                sendMsg(this.mAudioHandler, 30, 2, 0, 0, formats, 0);
            }
        }
    }

    private void onEnableSurroundFormats(ArrayList<Integer> enabledSurroundFormats) {
        for (int surroundFormat : AudioFormat.SURROUND_SOUND_ENCODING) {
            boolean enabled = enabledSurroundFormats.contains(Integer.valueOf(surroundFormat));
            int ret = AudioSystem.setSurroundFormatEnabled(surroundFormat, enabled);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enable surround format:");
            stringBuilder.append(surroundFormat);
            stringBuilder.append(" ");
            stringBuilder.append(enabled);
            stringBuilder.append(" ");
            stringBuilder.append(ret);
            Log.i(str, stringBuilder.toString());
        }
    }

    private void readPersistedSettings() {
        ContentResolver cr = this.mContentResolver;
        int i = 2;
        int ringerModeFromSettings = Global.getInt(cr, "mode_ringer", 2);
        int ringerMode = ringerModeFromSettings;
        if (!isValidRingerMode(ringerMode)) {
            ringerMode = 2;
        }
        if (ringerMode == 1 && !this.mHasVibrator) {
            ringerMode = 0;
        }
        if (ringerMode != ringerModeFromSettings) {
            Global.putInt(cr, "mode_ringer", ringerMode);
        }
        if (this.mUseFixedVolume || this.mIsSingleVolume) {
            ringerMode = 2;
        }
        synchronized (this.mSettingsLock) {
            this.mRingerMode = ringerMode;
            if (this.mRingerModeExternal == -1) {
                this.mRingerModeExternal = this.mRingerMode;
            }
            this.mVibrateSetting = AudioSystem.getValueForVibrateSetting(0, 1, this.mHasVibrator ? 2 : 0);
            int i2 = this.mVibrateSetting;
            if (!this.mHasVibrator) {
                i = 0;
            }
            this.mVibrateSetting = AudioSystem.getValueForVibrateSetting(i2, 0, i);
            updateRingerAndZenModeAffectedStreams();
            readDockAudioSettings(cr);
            sendEncodedSurroundMode(cr, "readPersistedSettings");
            sendEnabledSurroundFormats(cr, true);
        }
        this.mMuteAffectedStreams = System.getIntForUser(cr, "mute_streams_affected", 47, -2);
        updateMasterMono(cr);
        readPersistedSettingsEx(cr);
        broadcastRingerMode("android.media.RINGER_MODE_CHANGED", this.mRingerModeExternal);
        broadcastRingerMode("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION", this.mRingerMode);
        broadcastVibrateSetting(0);
        broadcastVibrateSetting(1);
        this.mVolumeController.loadSettings(cr);
    }

    private void readUserRestrictions() {
        int currentUser = getCurrentUserId();
        boolean masterMute = this.mUserManagerInternal.getUserRestriction(currentUser, "disallow_unmute_device") || this.mUserManagerInternal.getUserRestriction(currentUser, "no_adjust_volume");
        if (this.mUseFixedVolume) {
            masterMute = false;
            AudioSystem.setMasterVolume(1.0f);
        }
        Log.d(TAG, String.format("Master mute %s, user=%d", new Object[]{Boolean.valueOf(masterMute), Integer.valueOf(currentUser)}));
        setSystemAudioMute(masterMute);
        AudioSystem.setMasterMute(masterMute);
        broadcastMasterMuteStatus(masterMute);
        Log.d(TAG, String.format("Mic mute %s, user=%d", new Object[]{Boolean.valueOf(this.mUserManagerInternal.getUserRestriction(currentUser, "no_unmute_microphone")), Integer.valueOf(currentUser)}));
        AudioSystem.muteMicrophone(microphoneMute);
    }

    private int rescaleIndex(int index, int srcStream, int dstStream) {
        int rescaled = ((this.mStreamStates[dstStream].getMaxIndex() * index) + (this.mStreamStates[srcStream].getMaxIndex() / 2)) / this.mStreamStates[srcStream].getMaxIndex();
        if (rescaled < this.mStreamStates[dstStream].getMinIndex()) {
            return this.mStreamStates[dstStream].getMinIndex();
        }
        return rescaled;
    }

    public void adjustSuggestedStreamVolume(int direction, int suggestedStreamType, int flags, String callingPackage, String caller) {
        IAudioPolicyCallback extVolCtlr;
        synchronized (this.mExtVolumeControllerLock) {
            extVolCtlr = this.mExtVolumeController;
        }
        if (extVolCtlr != null) {
            sendMsg(this.mAudioHandler, 28, 2, direction, 0, extVolCtlr, 0);
            return;
        }
        adjustSuggestedStreamVolume(direction, suggestedStreamType, flags, callingPackage, caller, Binder.getCallingUid());
    }

    private void adjustSuggestedStreamVolume(int direction, int suggestedStreamType, int flags, String callingPackage, String caller, int uid) {
        int maybeActiveStreamType;
        int direction2;
        int flags2;
        int i = suggestedStreamType;
        int flags3 = flags;
        String str = caller;
        String str2 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("adjustSuggestedStreamVolume() stream=");
        stringBuilder.append(i);
        stringBuilder.append(", flags=");
        stringBuilder.append(flags3);
        stringBuilder.append(", caller=");
        stringBuilder.append(str);
        stringBuilder.append(", volControlStream=");
        stringBuilder.append(this.mVolumeControlStream);
        stringBuilder.append(", userSelect=");
        stringBuilder.append(this.mUserSelectedVolumeControlStream);
        Log.d(str2, stringBuilder.toString());
        AudioEventLogger audioEventLogger = this.mVolumeLogger;
        String str3 = callingPackage;
        stringBuilder = new StringBuilder(str3);
        stringBuilder.append(SliceAuthority.DELIMITER);
        stringBuilder.append(str);
        stringBuilder.append(" uid:");
        int i2 = uid;
        stringBuilder.append(i2);
        audioEventLogger.log(new VolumeEvent(0, i, direction, flags3, stringBuilder.toString()));
        synchronized (this.mForceControlStreamLock) {
            int streamType;
            if (this.mUserSelectedVolumeControlStream) {
                streamType = this.mVolumeControlStream;
            } else {
                boolean activeForReal;
                maybeActiveStreamType = getActiveStreamType(i);
                if (maybeActiveStreamType == 2 || maybeActiveStreamType == 5) {
                    activeForReal = wasStreamActiveRecently(maybeActiveStreamType, 0);
                } else {
                    activeForReal = AudioSystem.isStreamActive(maybeActiveStreamType, 0);
                }
                if (activeForReal || this.mVolumeControlStream == -1) {
                    streamType = maybeActiveStreamType;
                } else {
                    streamType = this.mVolumeControlStream;
                }
            }
            maybeActiveStreamType = streamType;
        }
        boolean isMute = isMuteAdjust(direction);
        ensureValidStreamType(maybeActiveStreamType);
        int resolvedStream = mStreamVolumeAlias[maybeActiveStreamType];
        if (!((flags3 & 4) == 0 || resolvedStream == 2)) {
            flags3 &= -5;
        }
        if (this.mVolumeController.suppressAdjustment(resolvedStream, flags3, isMute)) {
            int flags4 = (flags3 & -5) & -17;
            Log.d(TAG, "Volume controller suppressed adjustment");
            direction2 = 0;
            flags2 = flags4;
        } else {
            direction2 = direction;
            flags2 = flags3;
        }
        adjustStreamVolume(maybeActiveStreamType, direction2, flags2, str3, str, i2);
    }

    public void adjustStreamVolume(int streamType, int direction, int flags, String callingPackage) {
        sendBehavior(BehaviorId.AUDIO_ADJUSTSTREAMVOLUME, new Object[0]);
        if (streamType != 10 || canChangeAccessibilityVolume()) {
            this.mVolumeLogger.log(new VolumeEvent(1, streamType, direction, flags, callingPackage));
            adjustStreamVolume(streamType, direction, flags, callingPackage, callingPackage, Binder.getCallingUid());
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Trying to call adjustStreamVolume() for a11y withoutCHANGE_ACCESSIBILITY_VOLUME / callingPackage=");
        stringBuilder.append(callingPackage);
        Log.w(str, stringBuilder.toString());
    }

    private void onUnmuteStream(int stream, int flags) {
        this.mStreamStates[stream].mute(false);
        int index = this.mStreamStates[stream].getIndex(getDeviceForStream(stream));
        sendVolumeUpdate(stream, index, index, flags);
    }

    private void setSystemAudioVolume(int oldVolume, int newVolume, int maxVolume, int flags) {
        if (this.mHdmiManager != null && this.mHdmiTvClient != null && oldVolume != newVolume && (flags & 256) == 0) {
            synchronized (this.mHdmiManager) {
                if (this.mHdmiSystemAudioSupported) {
                    synchronized (this.mHdmiTvClient) {
                        long token = Binder.clearCallingIdentity();
                        try {
                            this.mHdmiTvClient.setSystemAudioVolume(oldVolume, newVolume, maxVolume);
                        } finally {
                            Binder.restoreCallingIdentity(token);
                        }
                    }
                    return;
                }
            }
        }
    }

    private int getNewRingerMode(int stream, int index, int flags) {
        if (this.mIsSingleVolume) {
            return getRingerModeExternal();
        }
        if ((flags & 2) == 0 && stream != getUiSoundsStreamType()) {
            return getRingerModeExternal();
        }
        int newRingerMode = 2;
        if (index == 0) {
            if (this.mHasVibrator) {
                newRingerMode = 1;
            } else if (this.mVolumePolicy.volumeDownToEnterSilent) {
                newRingerMode = 0;
            }
        }
        return newRingerMode;
    }

    private boolean isAndroidNPlus(String caller) {
        try {
            if (this.mContext.getPackageManager().getApplicationInfoAsUser(caller, 0, UserHandle.getUserId(Binder.getCallingUid())).targetSdkVersion >= 24) {
                return true;
            }
            return false;
        } catch (NameNotFoundException e) {
            return true;
        }
    }

    private boolean wouldToggleZenMode(int newMode) {
        if (getRingerModeExternal() == 0 && newMode != 0) {
            return true;
        }
        if (getRingerModeExternal() == 0 || newMode != 0) {
            return false;
        }
        return true;
    }

    private void onSetStreamVolume(int streamType, int index, int flags, int device, String caller) {
        int stream = mStreamVolumeAlias[streamType];
        setStreamVolumeInt(stream, index, device, false, caller);
        boolean z = false;
        if ((flags & 2) != 0 || stream == getUiSoundsStreamType()) {
            setRingerMode(getNewRingerMode(stream, index, flags), "AudioService.onSetStreamVolume", false);
        }
        VolumeStreamState volumeStreamState = this.mStreamStates[stream];
        if (index == 0) {
            z = true;
        }
        volumeStreamState.mute(z);
    }

    public void setStreamVolume(int streamType, int index, int flags, String callingPackage) {
        int i = streamType;
        String str = callingPackage;
        sendBehavior(BehaviorId.AUDIO_SETSTREAMVOLUME, new Object[0]);
        String str2;
        StringBuilder stringBuilder;
        if (i == 10 && !canChangeAccessibilityVolume()) {
            str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Trying to call setStreamVolume() for a11y without CHANGE_ACCESSIBILITY_VOLUME  callingPackage=");
            stringBuilder.append(str);
            Log.w(str2, stringBuilder.toString());
        } else if (i == 0 && index == 0 && this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") != 0) {
            str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Trying to call setStreamVolume() for STREAM_VOICE_CALL and index 0 without MODIFY_PHONE_STATE  callingPackage=");
            stringBuilder.append(str);
            Log.w(str2, stringBuilder.toString());
        } else {
            String str3 = str;
            this.mVolumeLogger.log(new VolumeEvent(2, i, index, flags, str3));
            setStreamVolume(i, index, flags, str, str3, Binder.getCallingUid());
        }
    }

    /* JADX WARNING: Missing block: B:18:0x002b, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean canChangeAccessibilityVolume() {
        synchronized (this.mAccessibilityServiceUidsLock) {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.CHANGE_ACCESSIBILITY_VOLUME") == 0) {
                return true;
            } else if (this.mAccessibilityServiceUids != null) {
                int callingUid = Binder.getCallingUid();
                for (int i : this.mAccessibilityServiceUids) {
                    if (i == callingUid) {
                        return true;
                    }
                }
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:101:0x0174 A:{Catch:{ all -> 0x01c0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:97:0x015e A:{Catch:{ all -> 0x01c5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x010b A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x0124 A:{Catch:{ all -> 0x00de }} */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x0129 A:{Catch:{ all -> 0x00de }} */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x013d A:{Catch:{ all -> 0x00de }} */
    /* JADX WARNING: Removed duplicated region for block: B:97:0x015e A:{Catch:{ all -> 0x01c5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:101:0x0174 A:{Catch:{ all -> 0x01c0 }} */
    /* JADX WARNING: Missing block: B:104:0x018f, code:
            if ((r16 & 1) == 0) goto L_0x0199;
     */
    /* JADX WARNING: Missing block: B:106:0x0193, code:
            if (r7.mScreenOn != false) goto L_0x0199;
     */
    /* JADX WARNING: Missing block: B:107:0x0195, code:
            r0 = r16 & -2;
     */
    /* JADX WARNING: Missing block: B:108:0x0199, code:
            r0 = r16;
     */
    /* JADX WARNING: Missing block: B:109:0x019b, code:
            sendVolumeUpdate(r8, r14, r15, r0);
     */
    /* JADX WARNING: Missing block: B:110:0x01a0, code:
            if (LOUD_VOICE_MODE_SUPPORT == false) goto L_0x01bf;
     */
    /* JADX WARNING: Missing block: B:111:0x01a2, code:
            sendMsg(r7.mAudioHandler, 10001, 0, 1, 0, new com.android.server.audio.AbsAudioService.DeviceVolumeState(r7, 0, r11, r14, r8), 0);
     */
    /* JADX WARNING: Missing block: B:112:0x01bf, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setStreamVolume(int streamType, int index, int flags, String callingPackage, String caller, int uid) {
        Throwable th;
        VolumeStreamState volumeStreamState;
        int i = streamType;
        int index2 = index;
        int flags2 = flags;
        String str = callingPackage;
        if (checkEnbaleVolumeAdjust()) {
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setStreamVolume(stream=");
            stringBuilder.append(i);
            stringBuilder.append(", index=");
            stringBuilder.append(index2);
            stringBuilder.append(", calling=");
            stringBuilder.append(str);
            stringBuilder.append(")");
            Log.d(str2, stringBuilder.toString());
            if (!this.mUseFixedVolume) {
                ensureValidStreamType(streamType);
                int streamTypeAlias = mStreamVolumeAlias[i];
                VolumeStreamState streamState = this.mStreamStates[streamTypeAlias];
                if (this.mCust == null || !this.mCust.isTurningAllSound()) {
                    int device = getDeviceForStream(streamTypeAlias);
                    if ((device & 896) != 0 || (flags2 & 64) == 0) {
                        int uid2;
                        int i2 = uid;
                        if (i2 == 1000) {
                            uid2 = UserHandle.getUid(getCurrentUserId(), UserHandle.getAppId(uid));
                        } else {
                            uid2 = i2;
                        }
                        if (this.mAppOps.noteOp(STREAM_VOLUME_OPS[streamTypeAlias], uid2, str) == 0) {
                            if (isAndroidNPlus(str) && wouldToggleZenMode(getNewRingerMode(streamTypeAlias, index2, flags2)) && !this.mNm.isNotificationPolicyAccessGrantedForPackage(str)) {
                                throw new SecurityException("Not allowed to change Do Not Disturb state");
                            } else if (volumeAdjustmentAllowedByDnd(streamTypeAlias, flags2)) {
                                synchronized (this.mSafeMediaVolumeState) {
                                    try {
                                        int index3;
                                        int index4;
                                        int flags3;
                                        this.mPendingVolumeCommand = null;
                                        int oldIndex = streamState.getIndex(device);
                                        if (index2 < (streamState.getMinIndex() + 5) / 10) {
                                            try {
                                                index3 = (streamState.getMinIndex() + 5) / 10;
                                            } catch (Throwable th2) {
                                                th = th2;
                                                volumeStreamState = streamState;
                                            }
                                        } else {
                                            if (index2 > (streamState.getMaxIndex() + 5) / 10) {
                                                index3 = (streamState.getMaxIndex() + 5) / 10;
                                            }
                                            index2 = rescaleIndex(index2 * 10, i, streamTypeAlias);
                                            if (streamTypeAlias == 3 && (device & 896) != 0 && (flags2 & 64) == 0) {
                                                synchronized (this.mA2dpAvrcpLock) {
                                                    if (this.mA2dp != null && this.mAvrcpAbsVolSupported) {
                                                        this.mA2dp.setAvrcpAbsoluteVolume(index2 / 10);
                                                    }
                                                }
                                            }
                                            if ((134217728 & device) != 0) {
                                                setHearingAidVolume(index2, i);
                                            }
                                            if (streamTypeAlias == 3) {
                                                setSystemAudioVolume(oldIndex, index2, getStreamMaxVolume(streamType), flags2);
                                            }
                                            flags2 &= -33;
                                            if (streamTypeAlias == 3 && (this.mFixedVolumeDevices & device) != 0) {
                                                flags2 |= 32;
                                                if (index2 != 0) {
                                                    if (this.mSafeMediaVolumeState.intValue() != 3 || (603979788 & device) == 0) {
                                                        index3 = streamState.getMaxIndex();
                                                    } else {
                                                        index3 = safeMediaVolumeIndex(device);
                                                    }
                                                    index4 = index3;
                                                    flags3 = flags2;
                                                    int flags4;
                                                    if (checkSafeMediaVolume(streamTypeAlias, index4, device)) {
                                                        flags4 = flags3;
                                                        volumeStreamState = streamState;
                                                        onSetStreamVolume(i, index4, flags4, device, caller);
                                                        index4 = this.mStreamStates[i].getIndex(device);
                                                    } else {
                                                        this.mVolumeController.postDisplaySafeVolumeWarning(flags3);
                                                        StreamVolumeCommand streamVolumeCommand = streamVolumeCommand;
                                                        flags4 = flags3;
                                                        try {
                                                            this.mPendingVolumeCommand = new StreamVolumeCommand(i, index4, flags3, device);
                                                        } catch (Throwable th3) {
                                                            th = th3;
                                                            index2 = index4;
                                                            flags2 = flags4;
                                                            while (true) {
                                                                try {
                                                                    break;
                                                                } catch (Throwable th4) {
                                                                    th = th4;
                                                                }
                                                            }
                                                            throw th;
                                                        }
                                                    }
                                                }
                                            }
                                            index4 = index2;
                                            flags3 = flags2;
                                            if (checkSafeMediaVolume(streamTypeAlias, index4, device)) {
                                            }
                                        }
                                        index2 = index3;
                                        index2 = rescaleIndex(index2 * 10, i, streamTypeAlias);
                                        synchronized (this.mA2dpAvrcpLock) {
                                        }
                                        if ((134217728 & device) != 0) {
                                        }
                                        if (streamTypeAlias == 3) {
                                        }
                                        flags2 &= -33;
                                        flags2 |= 32;
                                        if (index2 != 0) {
                                        }
                                        index4 = index2;
                                        flags3 = flags2;
                                        try {
                                            if (checkSafeMediaVolume(streamTypeAlias, index4, device)) {
                                            }
                                        } catch (Throwable th5) {
                                            th = th5;
                                            volumeStreamState = streamState;
                                            index2 = index4;
                                            flags2 = flags3;
                                            while (true) {
                                                break;
                                            }
                                            throw th;
                                        }
                                    } catch (Throwable th6) {
                                        th = th6;
                                        volumeStreamState = streamState;
                                        while (true) {
                                            break;
                                        }
                                        throw th;
                                    }
                                }
                            } else {
                                return;
                            }
                        }
                        return;
                    }
                    return;
                }
                sendMsg(this.mAudioHandler, 10, 2, 1, 0, streamState, 0);
            }
        }
    }

    private boolean volumeAdjustmentAllowedByDnd(int streamTypeAlias, int flags) {
        boolean z = true;
        switch (this.mNm.getZenMode()) {
            case 0:
                return true;
            case 1:
            case 2:
            case 3:
                boolean isTotalSilence = Global.getInt(this.mContext.getContentResolver(), "total_silence_mode", 0) == 1;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("isTotalSilence : ");
                stringBuilder.append(isTotalSilence);
                stringBuilder.append(", mRingerAndZenModeMutedStreams :");
                stringBuilder.append(this.mRingerAndZenModeMutedStreams);
                Log.d(str, stringBuilder.toString());
                if ((isStreamMutedByRingerOrZenMode(streamTypeAlias) || (this.mNm.getZenMode() != 1 && streamTypeAlias == 3)) && streamTypeAlias != getUiSoundsStreamType() && (flags & 2) == 0 && isTotalSilence) {
                    z = false;
                }
                return z;
            default:
                return true;
        }
    }

    public void forceVolumeControlStream(int streamType, IBinder cb) {
        Log.d(TAG, String.format("forceVolumeControlStream(%d)", new Object[]{Integer.valueOf(streamType)}));
        synchronized (this.mForceControlStreamLock) {
            if (!(this.mVolumeControlStream == -1 || streamType == -1)) {
                this.mUserSelectedVolumeControlStream = true;
            }
            this.mVolumeControlStream = streamType;
            if (this.mVolumeControlStream == -1) {
                if (this.mForceControlStreamClient != null) {
                    this.mForceControlStreamClient.release();
                    this.mForceControlStreamClient = null;
                }
                this.mUserSelectedVolumeControlStream = false;
            } else if (this.mForceControlStreamClient == null) {
                this.mForceControlStreamClient = new ForceControlStreamClient(cb);
            } else if (this.mForceControlStreamClient.getBinder() == cb) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("forceVolumeControlStream cb:");
                stringBuilder.append(cb);
                stringBuilder.append(" is already linked.");
                Log.d(str, stringBuilder.toString());
            } else {
                this.mForceControlStreamClient.release();
                this.mForceControlStreamClient = new ForceControlStreamClient(cb);
            }
        }
    }

    private void sendBroadcastToAll(Intent intent) {
        intent.addFlags(67108864);
        intent.addFlags(268435456);
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void sendStickyBroadcastToAll(Intent intent) {
        intent.addFlags(268435456);
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private int getCurrentUserId() {
        long ident = Binder.clearCallingIdentity();
        try {
            UserInfo currentUser = ActivityManager.getService().getCurrentUser();
            if (currentUser == null) {
                Log.e(TAG, "currentUser is null");
                Binder.restoreCallingIdentity(ident);
                return 0;
            }
            int i = currentUser.id;
            Binder.restoreCallingIdentity(ident);
            return i;
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
    }

    protected void sendVolumeUpdate(int streamType, int oldIndex, int index, int flags) {
        streamType = mStreamVolumeAlias[streamType];
        if (streamType == 3) {
            flags = updateFlagsForSystemAudio(flags);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendVolumeUpdate() stream=");
        stringBuilder.append(streamType);
        stringBuilder.append(" flags=");
        stringBuilder.append(flags);
        stringBuilder.append(" index=");
        stringBuilder.append(index);
        stringBuilder.append(" oldIndex=");
        stringBuilder.append(oldIndex);
        Log.d(str, stringBuilder.toString());
        this.mVolumeController.postVolumeChanged(streamType, flags);
    }

    private int updateFlagsForSystemAudio(int flags) {
        if (this.mHdmiTvClient != null) {
            synchronized (this.mHdmiTvClient) {
                if (this.mHdmiSystemAudioSupported && (flags & 256) == 0) {
                    flags &= -2;
                }
            }
        }
        return flags;
    }

    private void sendMasterMuteUpdate(boolean muted, int flags) {
        this.mVolumeController.postMasterMuteChanged(updateFlagsForSystemAudio(flags));
        broadcastMasterMuteStatus(muted);
    }

    private void broadcastMasterMuteStatus(boolean muted) {
        Intent intent = new Intent("android.media.MASTER_MUTE_CHANGED_ACTION");
        intent.putExtra("android.media.EXTRA_MASTER_VOLUME_MUTED", muted);
        intent.addFlags(603979776);
        sendStickyBroadcastToAll(intent);
    }

    private void setStreamVolumeInt(int streamType, int index, int device, boolean force, String caller) {
        VolumeStreamState streamState = this.mStreamStates[streamType];
        if (streamState.setIndex(index, device, caller) || force) {
            sendMsg(this.mAudioHandler, 0, 2, device, 0, streamState, 0);
        }
    }

    private void setSystemAudioMute(boolean state) {
        if (this.mHdmiManager != null && this.mHdmiTvClient != null) {
            synchronized (this.mHdmiManager) {
                if (this.mHdmiSystemAudioSupported) {
                    synchronized (this.mHdmiTvClient) {
                        long token = Binder.clearCallingIdentity();
                        try {
                            this.mHdmiTvClient.setSystemAudioMute(state);
                        } finally {
                            Binder.restoreCallingIdentity(token);
                        }
                    }
                    return;
                }
            }
        }
    }

    public boolean isStreamMute(int streamType) {
        boolean access$500;
        if (streamType == Integer.MIN_VALUE) {
            streamType = getActiveStreamType(streamType);
        }
        synchronized (VolumeStreamState.class) {
            ensureValidStreamType(streamType);
            access$500 = this.mStreamStates[streamType].mIsMuted;
        }
        return access$500;
    }

    private boolean discardRmtSbmxFullVolDeathHandlerFor(IBinder cb) {
        Iterator<RmtSbmxFullVolDeathHandler> it = this.mRmtSbmxFullVolDeathHandlers.iterator();
        while (it.hasNext()) {
            RmtSbmxFullVolDeathHandler handler = (RmtSbmxFullVolDeathHandler) it.next();
            if (handler.isHandlerFor(cb)) {
                handler.forget();
                this.mRmtSbmxFullVolDeathHandlers.remove(handler);
                return true;
            }
        }
        return false;
    }

    private boolean hasRmtSbmxFullVolDeathHandlerFor(IBinder cb) {
        Iterator<RmtSbmxFullVolDeathHandler> it = this.mRmtSbmxFullVolDeathHandlers.iterator();
        while (it.hasNext()) {
            if (((RmtSbmxFullVolDeathHandler) it.next()).isHandlerFor(cb)) {
                return true;
            }
        }
        return false;
    }

    public void forceRemoteSubmixFullVolume(boolean startForcing, IBinder cb) {
        if (cb != null) {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.CAPTURE_AUDIO_OUTPUT") != 0) {
                Log.w(TAG, "Trying to call forceRemoteSubmixFullVolume() without CAPTURE_AUDIO_OUTPUT");
                return;
            }
            synchronized (this.mRmtSbmxFullVolDeathHandlers) {
                boolean applyRequired = false;
                if (startForcing) {
                    if (!hasRmtSbmxFullVolDeathHandlerFor(cb)) {
                        this.mRmtSbmxFullVolDeathHandlers.add(new RmtSbmxFullVolDeathHandler(cb));
                        if (this.mRmtSbmxFullVolRefCount == 0) {
                            this.mFullVolumeDevices |= 32768;
                            this.mFixedVolumeDevices |= 32768;
                            applyRequired = true;
                        }
                        this.mRmtSbmxFullVolRefCount++;
                    }
                } else if (discardRmtSbmxFullVolDeathHandlerFor(cb) && this.mRmtSbmxFullVolRefCount > 0) {
                    this.mRmtSbmxFullVolRefCount--;
                    if (this.mRmtSbmxFullVolRefCount == 0) {
                        this.mFullVolumeDevices &= -32769;
                        this.mFixedVolumeDevices &= -32769;
                        applyRequired = true;
                    }
                }
                if (applyRequired) {
                    checkAllFixedVolumeDevices(3);
                    this.mStreamStates[3].applyAllVolumes();
                }
            }
        }
    }

    private void setMasterMuteInternal(boolean mute, int flags, String callingPackage, int uid, int userId) {
        if (uid == 1000) {
            uid = UserHandle.getUid(userId, UserHandle.getAppId(uid));
        }
        if (!mute && this.mAppOps.noteOp(33, uid, callingPackage) != 0) {
            return;
        }
        if (userId == UserHandle.getCallingUserId() || this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == 0) {
            setMasterMuteInternalNoCallerCheck(mute, flags, userId);
        }
    }

    private void setMasterMuteInternalNoCallerCheck(boolean mute, int flags, int userId) {
        Log.d(TAG, String.format("Master mute %s, %d, user=%d", new Object[]{Boolean.valueOf(mute), Integer.valueOf(flags), Integer.valueOf(userId)}));
        if ((isPlatformAutomotive() || !this.mUseFixedVolume) && getCurrentUserId() == userId && mute != AudioSystem.getMasterMute()) {
            setSystemAudioMute(mute);
            AudioSystem.setMasterMute(mute);
            sendMasterMuteUpdate(mute, flags);
            Intent intent = new Intent("android.media.MASTER_MUTE_CHANGED_ACTION");
            intent.putExtra("android.media.EXTRA_MASTER_VOLUME_MUTED", mute);
            sendBroadcastToAll(intent);
        }
    }

    public boolean isMasterMute() {
        return AudioSystem.getMasterMute();
    }

    public void setMasterMute(boolean mute, int flags, String callingPackage, int userId) {
        sendBehavior(BehaviorId.AUDIO_SETMASTERMUTE, new Object[0]);
        setMasterMuteInternal(mute, flags, callingPackage, Binder.getCallingUid(), userId);
    }

    public int getStreamVolume(int streamType) {
        int i;
        ensureValidStreamType(streamType);
        int device = getDeviceForStream(streamType);
        synchronized (VolumeStreamState.class) {
            int index = this.mStreamStates[streamType].getIndex(device);
            if (this.mStreamStates[streamType].mIsMuted) {
                index = 0;
            }
            if (!(index == 0 || mStreamVolumeAlias[streamType] != 3 || (this.mFixedVolumeDevices & device) == 0)) {
                index = this.mStreamStates[streamType].getMaxIndex();
            }
            i = (index + 5) / 10;
        }
        return i;
    }

    public int getStreamMaxVolume(int streamType) {
        ensureValidStreamType(streamType);
        return (this.mStreamStates[streamType].getMaxIndex() + 5) / 10;
    }

    public int getStreamMinVolume(int streamType) {
        ensureValidStreamType(streamType);
        return (this.mStreamStates[streamType].getMinIndex() + 5) / 10;
    }

    public int getLastAudibleStreamVolume(int streamType) {
        ensureValidStreamType(streamType);
        return (this.mStreamStates[streamType].getIndex(getDeviceForStream(streamType)) + 5) / 10;
    }

    public int getUiSoundsStreamType() {
        return mStreamVolumeAlias[1];
    }

    public void setMicrophoneMute(boolean on, String callingPackage, int userId) {
        sendBehavior(BehaviorId.AUDIO_SETMICROPHONEMUTE, new Object[0]);
        int uid = Binder.getCallingUid();
        if (uid == 1000) {
            uid = UserHandle.getUid(userId, UserHandle.getAppId(uid));
        }
        if ((!on && this.mAppOps.noteOp(44, uid, callingPackage) != 0) || !checkAudioSettingsPermission("setMicrophoneMute()")) {
            return;
        }
        if (userId == UserHandle.getCallingUserId() || this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == 0) {
            setMicrophoneMuteNoCallerCheck(on, userId);
        }
    }

    private void setMicrophoneMuteNoCallerCheck(boolean on, int userId) {
        Log.d(TAG, String.format("Mic mute %s, user=%d", new Object[]{Boolean.valueOf(on), Integer.valueOf(userId)}));
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ASsmm");
        stringBuilder.append(on);
        if (checkAudioSettingAllowed(stringBuilder.toString()) && getCurrentUserId() == userId) {
            boolean currentMute = AudioSystem.isMicrophoneMuted();
            long identity = Binder.clearCallingIdentity();
            AudioSystem.muteMicrophone(on);
            Binder.restoreCallingIdentity(identity);
            if (on != currentMute) {
                this.mContext.sendBroadcast(new Intent("android.media.action.MICROPHONE_MUTE_CHANGED").setFlags(1073741824));
            }
        }
    }

    public int getRingerModeExternal() {
        int i;
        synchronized (this.mSettingsLock) {
            i = this.mRingerModeExternal;
        }
        return i;
    }

    public int getRingerModeInternal() {
        int i;
        synchronized (this.mSettingsLock) {
            i = this.mRingerMode;
        }
        return i;
    }

    private void ensureValidRingerMode(int ringerMode) {
        if (!isValidRingerMode(ringerMode)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Bad ringer mode ");
            stringBuilder.append(ringerMode);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public boolean isValidRingerMode(int ringerMode) {
        return ringerMode >= 0 && ringerMode <= 2;
    }

    public void setRingerModeExternal(int ringerMode, String caller) {
        if (isAndroidNPlus(caller) && wouldToggleZenMode(ringerMode) && !this.mNm.isNotificationPolicyAccessGrantedForPackage(caller)) {
            throw new SecurityException("Not allowed to change Do Not Disturb state");
        }
        setRingerMode(ringerMode, caller, true);
    }

    public void setRingerModeInternal(int ringerMode, String caller) {
        enforceVolumeController("setRingerModeInternal");
        setRingerMode(ringerMode, caller, false);
    }

    public void silenceRingerModeInternal(String reason) {
        VibrationEffect effect = null;
        int ringerMode = 0;
        int toastText = 0;
        int silenceRingerSetting = 0;
        if (this.mContext.getResources().getBoolean(17957068)) {
            silenceRingerSetting = Secure.getIntForUser(this.mContentResolver, "volume_hush_gesture", 0, -2);
        }
        switch (silenceRingerSetting) {
            case 1:
                effect = VibrationEffect.get(5);
                ringerMode = 1;
                toastText = 17041322;
                break;
            case 2:
                effect = VibrationEffect.get(1);
                ringerMode = 0;
                toastText = 17041321;
                break;
        }
        maybeVibrate(effect);
        setRingerModeInternal(ringerMode, reason);
        Toast.makeText(this.mContext, toastText, 0).show();
    }

    private boolean maybeVibrate(VibrationEffect effect) {
        if (!this.mHasVibrator) {
            return false;
        }
        if ((System.getIntForUser(this.mContext.getContentResolver(), "haptic_feedback_enabled", 0, -2) == 0) || effect == null) {
            return false;
        }
        this.mVibrator.vibrate(Binder.getCallingUid(), this.mContext.getOpPackageName(), effect, VIBRATION_ATTRIBUTES);
        return true;
    }

    private void setRingerMode(int ringerMode, String caller, boolean external) {
        if (checkEnbaleVolumeAdjust() && !this.mUseFixedVolume && !this.mIsSingleVolume) {
            if (caller == null || caller.length() == 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Bad caller: ");
                stringBuilder.append(caller);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            ensureValidRingerMode(ringerMode);
            if (ringerMode == 1 && !this.mHasVibrator) {
                ringerMode = 0;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                int ringerModeInternal = getRingerModeInternal();
                int ringerModeExternal = getRingerModeExternal();
                if (external) {
                    setRingerModeExt(ringerMode);
                    if (this.mRingerModeDelegate != null) {
                        ringerMode = this.mRingerModeDelegate.onSetRingerModeExternal(ringerModeExternal, ringerMode, caller, ringerModeInternal, this.mVolumePolicy);
                    }
                    synchronized (this.mSettingsLock) {
                        if (ringerMode != ringerModeInternal) {
                            setRingerModeInt(ringerMode, true);
                        }
                    }
                } else {
                    synchronized (this.mSettingsLock) {
                        if (ringerMode != ringerModeInternal) {
                            setRingerModeInt(ringerMode, true);
                        }
                    }
                    if (this.mRingerModeDelegate != null) {
                        ringerMode = this.mRingerModeDelegate.onSetRingerModeInternal(ringerModeInternal, ringerMode, caller, ringerModeExternal, this.mVolumePolicy);
                    }
                    setRingerModeExt(ringerMode);
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    private void setRingerModeExt(int ringerMode) {
        synchronized (this.mSettingsLock) {
            if (ringerMode == this.mRingerModeExternal) {
                return;
            }
            this.mRingerModeExternal = ringerMode;
            broadcastRingerMode("android.media.RINGER_MODE_CHANGED", ringerMode);
        }
    }

    @GuardedBy("mSettingsLock")
    private void muteRingerModeStreams() {
        int ringerMode;
        Throwable th;
        int numStreamTypes = AudioSystem.getNumStreamTypes();
        if (this.mNm == null) {
            this.mNm = (NotificationManager) this.mContext.getSystemService("notification");
        }
        int ringerMode2 = this.mRingerMode;
        boolean z = true;
        boolean ringerModeMute = ringerMode2 == 1 || ringerMode2 == 0;
        boolean shouldRingSco = ringerMode2 == 1 && isBluetoothScoOn();
        String eventSource = new StringBuilder();
        eventSource.append("muteRingerModeStreams() from u/pid:");
        eventSource.append(Binder.getCallingUid());
        eventSource.append(SliceAuthority.DELIMITER);
        eventSource.append(Binder.getCallingPid());
        sendMsg(this.mAudioHandler, 8, 2, 7, shouldRingSco ? 3 : 0, eventSource.toString(), 0);
        int streamType = numStreamTypes - 1;
        while (streamType >= 0) {
            int numStreamTypes2;
            boolean z2;
            boolean isMuted = isStreamMutedByRingerOrZenMode(streamType);
            boolean muteAllowedBySco = (shouldRingSco && streamType == 2) ? false : z;
            boolean shouldMute = (shouldZenMuteStream(streamType) || (ringerModeMute && isStreamAffectedByRingerMode(streamType) && muteAllowedBySco)) ? z : false;
            if (isMuted == shouldMute) {
                numStreamTypes2 = numStreamTypes;
                ringerMode = ringerMode2;
                z2 = z;
            } else {
                if (this.mStreamStates[streamType] == null || shouldMute) {
                    numStreamTypes2 = numStreamTypes;
                    ringerMode = ringerMode2;
                    if (this.mStreamStates[streamType] != null) {
                        z2 = true;
                        this.mStreamStates[streamType].mute(true);
                        this.mRingerAndZenModeMutedStreams |= 1 << streamType;
                    }
                } else {
                    if (mStreamVolumeAlias[streamType] == 2) {
                        synchronized (this.mSettingsLock) {
                            try {
                                synchronized (VolumeStreamState.class) {
                                    try {
                                        VolumeStreamState vss = this.mStreamStates[streamType];
                                        int i = 0;
                                        while (true) {
                                            int i2 = i;
                                            if (i2 >= vss.mIndexMap.size()) {
                                                break;
                                            }
                                            int device = vss.mIndexMap.keyAt(i2);
                                            numStreamTypes2 = numStreamTypes;
                                            try {
                                                numStreamTypes = vss.mIndexMap.valueAt(i2);
                                                if (numStreamTypes == 0) {
                                                    int value = numStreamTypes;
                                                    ringerMode = ringerMode2;
                                                    vss.setIndex(10, device, TAG);
                                                } else {
                                                    ringerMode = ringerMode2;
                                                }
                                                i = i2 + 1;
                                                numStreamTypes = numStreamTypes2;
                                                ringerMode2 = ringerMode;
                                            } catch (Throwable th2) {
                                                th = th2;
                                                throw th;
                                            }
                                        }
                                        numStreamTypes2 = numStreamTypes;
                                        ringerMode = ringerMode2;
                                        sendMsg(this.mAudioHandler, 1, 2, getDeviceForStream(streamType), 0, this.mStreamStates[streamType], 500);
                                    } catch (Throwable th3) {
                                        th = th3;
                                        numStreamTypes2 = numStreamTypes;
                                        ringerMode = ringerMode2;
                                        throw th;
                                    }
                                }
                            } catch (Throwable th4) {
                                th = th4;
                            }
                        }
                    } else {
                        numStreamTypes2 = numStreamTypes;
                        ringerMode = ringerMode2;
                    }
                    this.mStreamStates[streamType].mute(false);
                    this.mRingerAndZenModeMutedStreams &= ~(1 << streamType);
                }
                z2 = true;
            }
            streamType--;
            z = z2;
            numStreamTypes = numStreamTypes2;
            ringerMode2 = ringerMode;
        }
        ringerMode = ringerMode2;
        return;
        throw th;
    }

    private boolean isAlarm(int streamType) {
        return streamType == 4;
    }

    private boolean isNotificationOrRinger(int streamType) {
        return streamType == 5 || streamType == 2;
    }

    private boolean isMedia(int streamType) {
        return streamType == 3;
    }

    private boolean isSystem(int streamType) {
        return streamType == 1;
    }

    private void setRingerModeInt(int ringerMode, boolean persist) {
        boolean change;
        synchronized (this.mSettingsLock) {
            change = this.mRingerMode != ringerMode;
            this.mRingerMode = ringerMode;
            muteRingerModeStreams();
        }
        if (persist) {
            sendMsg(this.mAudioHandler, 3, 0, 0, 0, null, 500);
        }
        if (change) {
            broadcastRingerMode("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION", ringerMode);
        }
    }

    public boolean shouldVibrate(int vibrateType) {
        boolean z = false;
        if (!this.mHasVibrator) {
            return false;
        }
        switch (getVibrateSetting(vibrateType)) {
            case 0:
                return false;
            case 1:
                if (getRingerModeExternal() != 0) {
                    z = true;
                }
                return z;
            case 2:
                if (getRingerModeExternal() == 1) {
                    z = true;
                }
                return z;
            default:
                return false;
        }
    }

    public int getVibrateSetting(int vibrateType) {
        if (this.mHasVibrator) {
            return (this.mVibrateSetting >> (vibrateType * 2)) & 3;
        }
        return 0;
    }

    public void setVibrateSetting(int vibrateType, int vibrateSetting) {
        if (this.mHasVibrator) {
            this.mVibrateSetting = AudioSystem.getValueForVibrateSetting(this.mVibrateSetting, vibrateType, vibrateSetting);
            broadcastVibrateSetting(vibrateType);
        }
    }

    private void dumpAudioMode(PrintWriter pw) {
        pw.println("\nAudio Mode:");
        synchronized (this.mSetModeDeathHandlers) {
            Iterator iter = this.mSetModeDeathHandlers.iterator();
            while (iter.hasNext()) {
                ((SetModeDeathHandler) iter.next()).dump(pw);
            }
        }
    }

    public void setMode(int mode, IBinder cb, String callingPackage) {
        sendBehavior(BehaviorId.AUDIO_SETMODE, new Object[0]);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setMode(mode=");
        stringBuilder.append(mode);
        stringBuilder.append(", callingPackage=");
        stringBuilder.append(callingPackage);
        stringBuilder.append(")");
        Log.v(str, stringBuilder.toString());
        if (checkAudioSettingsPermission("setMode()")) {
            if (DUAL_SMARTPA_SUPPORT || DUAL_SMARTPA_DELAY) {
                checkMuteRcvDelay(this.mMode, mode);
            }
            StringBuilder stringBuilder2;
            if (mode == 2 && this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") != 0) {
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("MODIFY_PHONE_STATE Permission Denial: setMode(MODE_IN_CALL) from pid=");
                stringBuilder2.append(Binder.getCallingPid());
                stringBuilder2.append(", uid=");
                stringBuilder2.append(Binder.getCallingUid());
                Log.w(str, stringBuilder2.toString());
            } else if (mode >= -1 && mode < 4) {
                if (this.mMode == 2 && mode == 3 && this.mIsHisiPlatform) {
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Forbid set MODE_IN_COMMUNICATION when current mode is MODE_IN_CALL from pid=");
                    stringBuilder2.append(Binder.getCallingPid());
                    stringBuilder2.append(", uid=");
                    stringBuilder2.append(Binder.getCallingUid());
                    Log.w(str, stringBuilder2.toString());
                    return;
                }
                int newModeOwnerPid;
                int oldModeOwnerPid = 0;
                synchronized (this.mSetModeDeathHandlers) {
                    if (!this.mSetModeDeathHandlers.isEmpty()) {
                        oldModeOwnerPid = ((SetModeDeathHandler) this.mSetModeDeathHandlers.get(0)).getPid();
                    }
                    if (mode == -1) {
                        mode = this.mMode;
                    }
                    newModeOwnerPid = setModeInt(mode, cb, Binder.getCallingPid(), callingPackage);
                }
                if (!(newModeOwnerPid == oldModeOwnerPid || newModeOwnerPid == 0)) {
                    disconnectBluetoothSco(newModeOwnerPid);
                }
                if (LOUD_VOICE_MODE_SUPPORT) {
                    getOldInCallDevice(mode);
                }
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:34:0x0134  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x00f8  */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x01a9  */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x014a  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected int setModeInt(int mode, IBinder cb, int pid, String caller) {
        int i = pid;
        String str = caller;
        String str2 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setModeInt(mode=");
        int mode2 = mode;
        stringBuilder.append(mode2);
        stringBuilder.append(", pid=");
        stringBuilder.append(i);
        stringBuilder.append(", caller=");
        stringBuilder.append(str);
        stringBuilder.append(")");
        Log.v(str2, stringBuilder.toString());
        int newModeOwnerPid = 0;
        if (cb == null) {
            Log.e(TAG, "setModeInt() called with null binder");
            return 0;
        }
        int status;
        int newModeOwnerPid2;
        SetModeDeathHandler hdlr = null;
        Iterator iter = this.mSetModeDeathHandlers.iterator();
        while (true) {
            Iterator iter2 = iter;
            if (!iter2.hasNext()) {
                break;
            }
            SetModeDeathHandler h = (SetModeDeathHandler) iter2.next();
            if (h.getPid() == i) {
                hdlr = h;
                iter2.remove();
                hdlr.getBinder().unlinkToDeath(hdlr, 0);
                break;
            }
            iter = iter2;
        }
        IBinder cb2 = cb;
        int status2 = 0;
        while (true) {
            SetModeDeathHandler hdlr2;
            IBinder cb3;
            int actualMode = mode2;
            if (mode2 != 0) {
                if (hdlr == null) {
                    hdlr = new SetModeDeathHandler(cb2, i);
                }
                try {
                    cb2.linkToDeath(hdlr, 0);
                } catch (RemoteException e) {
                    RemoteException remoteException = e;
                    String str3 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("setMode() could not link to ");
                    stringBuilder2.append(cb2);
                    stringBuilder2.append(" binder death");
                    Log.w(str3, stringBuilder2.toString());
                }
                this.mSetModeDeathHandlers.add(0, hdlr);
                hdlr.setMode(mode2);
            } else if (!this.mSetModeDeathHandlers.isEmpty()) {
                int mode3;
                hdlr2 = (SetModeDeathHandler) this.mSetModeDeathHandlers.get(0);
                hdlr = hdlr2.getBinder();
                cb2 = hdlr2.getMode();
                String str4 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(" using mode=");
                stringBuilder3.append(mode2);
                stringBuilder3.append(" instead due to death hdlr at pid=");
                stringBuilder3.append(hdlr2.mPid);
                stringBuilder3.append(";pkgName=");
                stringBuilder3.append(getPackageNameByPid(hdlr2.mPid));
                Log.w(str4, stringBuilder3.toString());
                cb3 = hdlr;
                actualMode = cb2;
                if (actualMode == this.mMode) {
                    long identity = Binder.clearCallingIdentity();
                    int status3 = AudioSystem.setPhoneState(actualMode);
                    Binder.restoreCallingIdentity(identity);
                    if (status3 == 0) {
                        String str5 = TAG;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append(" mode successfully set to ");
                        stringBuilder4.append(actualMode);
                        Log.v(str5, stringBuilder4.toString());
                        this.mMode = actualMode;
                    } else {
                        if (hdlr2 != null) {
                            this.mSetModeDeathHandlers.remove(hdlr2);
                            cb3.unlinkToDeath(hdlr2, 0);
                        }
                        Log.w(TAG, " mode set to MODE_NORMAL after phoneState pb");
                        mode2 = 0;
                    }
                    mode3 = mode2;
                    status = status3;
                } else {
                    mode3 = mode2;
                    status = 0;
                }
                if (status != 0 && !this.mSetModeDeathHandlers.isEmpty()) {
                    hdlr = hdlr2;
                    cb2 = cb3;
                    status2 = status;
                    mode2 = mode3;
                } else if (status != 0) {
                    if (actualMode != 0) {
                        if (this.mSetModeDeathHandlers.isEmpty()) {
                            Log.e(TAG, "setMode() different from MODE_NORMAL with empty mode client stack");
                        } else {
                            newModeOwnerPid = ((SetModeDeathHandler) this.mSetModeDeathHandlers.get(0)).getPid();
                        }
                    }
                    newModeOwnerPid2 = newModeOwnerPid;
                    PhoneStateEvent hdlr3 = r1;
                    AudioEventLogger audioEventLogger = this.mModeLogger;
                    PhoneStateEvent phoneStateEvent = new PhoneStateEvent(str, i, mode3, newModeOwnerPid2, actualMode);
                    audioEventLogger.log(hdlr3);
                    hdlr2 = getActiveStreamType(Integer.MIN_VALUE);
                    i = getDeviceForStream(hdlr2);
                    int index = this.mStreamStates[mStreamVolumeAlias[hdlr2]].getIndex(i);
                    setStreamVolumeInt(mStreamVolumeAlias[hdlr2], index, i, true, str);
                    updateStreamVolumeAlias(true, str);
                    updateAftPolicy();
                } else {
                    newModeOwnerPid2 = 0;
                }
            }
            hdlr2 = hdlr;
            cb3 = cb2;
            if (actualMode == this.mMode) {
            }
            if (status != 0) {
                break;
            }
            break;
        }
        if (status != 0) {
        }
        return newModeOwnerPid2;
    }

    public int getMode() {
        return this.mMode;
    }

    private void loadTouchSoundAssetDefaults() {
        SOUND_EFFECT_FILES.add("Effect_Tick.ogg");
        for (int i = 0; i < 10; i++) {
            this.SOUND_EFFECT_FILES_MAP[i][0] = 0;
            this.SOUND_EFFECT_FILES_MAP[i][1] = -1;
        }
    }

    /* JADX WARNING: Missing block: B:32:0x00b9, code:
            if (r0 != null) goto L_0x00bb;
     */
    /* JADX WARNING: Missing block: B:33:0x00bb, code:
            r0.close();
     */
    /* JADX WARNING: Missing block: B:38:0x00c9, code:
            if (r0 == null) goto L_0x00e2;
     */
    /* JADX WARNING: Missing block: B:41:0x00d4, code:
            if (r0 == null) goto L_0x00e2;
     */
    /* JADX WARNING: Missing block: B:44:0x00df, code:
            if (r0 == null) goto L_0x00e2;
     */
    /* JADX WARNING: Missing block: B:45:0x00e2, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void loadTouchSoundAssets() {
        XmlResourceParser parser = null;
        if (SOUND_EFFECT_FILES.isEmpty()) {
            loadTouchSoundAssetDefaults();
            try {
                parser = this.mContext.getResources().getXml(18284545);
                XmlUtils.beginDocument(parser, TAG_AUDIO_ASSETS);
                boolean inTouchSoundsGroup = false;
                if (ASSET_FILE_VERSION.equals(parser.getAttributeValue(null, ATTR_VERSION))) {
                    String element;
                    while (true) {
                        XmlUtils.nextElement(parser);
                        element = parser.getName();
                        if (element == null) {
                            break;
                        } else if (element.equals(TAG_GROUP)) {
                            if (GROUP_TOUCH_SOUNDS.equals(parser.getAttributeValue(null, "name"))) {
                                inTouchSoundsGroup = true;
                                break;
                            }
                        }
                    }
                    while (inTouchSoundsGroup) {
                        XmlUtils.nextElement(parser);
                        element = parser.getName();
                        if (element == null || !element.equals(TAG_ASSET)) {
                            break;
                        }
                        String id = parser.getAttributeValue(null, ATTR_ASSET_ID);
                        String file = parser.getAttributeValue(null, ATTR_ASSET_FILE);
                        try {
                            int fx = AudioManager.class.getField(id).getInt(null);
                            int i = SOUND_EFFECT_FILES.indexOf(file);
                            if (i == -1) {
                                i = SOUND_EFFECT_FILES.size();
                                SOUND_EFFECT_FILES.add(file);
                            }
                            this.SOUND_EFFECT_FILES_MAP[fx][0] = i;
                        } catch (Exception e) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Invalid touch sound ID: ");
                            stringBuilder.append(id);
                            Log.w(str, stringBuilder.toString());
                        }
                    }
                }
            } catch (NotFoundException e2) {
                Log.w(TAG, "audio assets file not found", e2);
            } catch (XmlPullParserException e3) {
                Log.w(TAG, "XML parser exception reading touch sound assets", e3);
            } catch (IOException e4) {
                Log.w(TAG, "I/O exception reading touch sound assets", e4);
            } catch (Throwable th) {
                if (parser != null) {
                    parser.close();
                }
            }
        }
    }

    public void playSoundEffect(int effectType) {
        playSoundEffectVolume(effectType, -1.0f);
    }

    public void playSoundEffectVolume(int effectType, float volume) {
        sendBehavior(BehaviorId.AUDIO_PLAYSOUNDEFFECTVOLUME, new Object[0]);
        if (!isStreamMutedByRingerOrZenMode(1)) {
            if (effectType >= 10 || effectType < 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("AudioService effectType value ");
                stringBuilder.append(effectType);
                stringBuilder.append(" out of range");
                Log.w(str, stringBuilder.toString());
                return;
            }
            sendMsg(this.mAudioHandler, 5, 2, effectType, (int) (1000.0f * volume), null, 0);
        }
    }

    /* JADX WARNING: Missing block: B:20:0x0031, code:
            if (r1.mStatus != 0) goto L_0x0034;
     */
    /* JADX WARNING: Missing block: B:32:?, code:
            return false;
     */
    /* JADX WARNING: Missing block: B:33:?, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean loadSoundEffects() {
        int attempts = 3;
        LoadSoundEffectReply reply = new LoadSoundEffectReply();
        synchronized (reply) {
            int attempts2;
            try {
                sendMsg(this.mAudioHandler, 7, 2, 0, 0, reply, 0);
                while (reply.mStatus == 1) {
                    attempts2 = attempts - 1;
                    if (attempts <= 0) {
                        attempts = attempts2;
                        break;
                    }
                    try {
                        reply.wait(5000);
                    } catch (InterruptedException e) {
                        Log.w(TAG, "loadSoundEffects Interrupted while waiting sound pool loaded.");
                    } catch (Throwable th) {
                        attempts = th;
                    }
                    attempts = attempts2;
                }
            } catch (Throwable th2) {
                Throwable th3 = th2;
                attempts2 = attempts;
                attempts = th3;
                throw attempts;
            }
        }
    }

    public void unloadSoundEffects() {
        sendMsg(this.mAudioHandler, 20, 2, 0, 0, null, 0);
    }

    public void reloadAudioSettings() {
        readAudioSettings(false);
    }

    private void readAudioSettings(boolean userSwitch) {
        readPersistedSettings();
        readUserRestrictions();
        int numStreamTypes = AudioSystem.getNumStreamTypes();
        int streamType = 0;
        while (streamType < numStreamTypes) {
            VolumeStreamState streamState = this.mStreamStates[streamType];
            if (!userSwitch || mStreamVolumeAlias[streamType] != 3) {
                streamState.readSettings();
                synchronized (VolumeStreamState.class) {
                    if (streamState.mIsMuted && (!(isStreamAffectedByMute(streamType) || isStreamMutedByRingerOrZenMode(streamType)) || this.mUseFixedVolume)) {
                        streamState.mIsMuted = false;
                    }
                }
            }
            streamType++;
        }
        setRingerModeInt(getRingerModeInternal(), false);
        checkAllFixedVolumeDevices();
        checkAllAliasStreamVolumes();
        checkMuteAffectedStreams();
        synchronized (this.mSafeMediaVolumeState) {
            this.mMusicActiveMs = MathUtils.constrain(Secure.getIntForUser(this.mContentResolver, "unsafe_volume_music_active_ms", 0, -2), 0, UNSAFE_VOLUME_MUSIC_ACTIVE_MS_MAX);
            if (this.mSafeMediaVolumeState.intValue() == 3) {
                enforceSafeMediaVolume(TAG);
            }
        }
    }

    public void setSpeakerphoneOn(boolean on) {
        sendBehavior(BehaviorId.AUDIO_SETSPEAKERPHONEON, new Object[0]);
        if (checkAudioSettingsPermission("setSpeakerphoneOn()")) {
            String eventSource = new StringBuilder("setSpeakerphoneOn(");
            eventSource.append(on);
            eventSource.append(") from u/pid:");
            eventSource.append(Binder.getCallingUid());
            eventSource.append(SliceAuthority.DELIMITER);
            eventSource.append(Binder.getCallingPid());
            eventSource = eventSource.toString();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ASsso");
            stringBuilder.append(on);
            if (checkAudioSettingAllowed(stringBuilder.toString())) {
                if (on) {
                    if (this.mForcedUseForComm == 3) {
                        sendMsg(this.mAudioHandler, 8, 2, 2, 0, eventSource, 0);
                    }
                    this.mForcedUseForComm = 1;
                } else if (this.mForcedUseForComm == 1) {
                    this.mForcedUseForComm = 0;
                }
                this.mForcedUseForCommExt = this.mForcedUseForComm;
                sendMsg(this.mAudioHandler, 8, 2, 0, this.mForcedUseForComm, eventSource, 0);
                if (LOUD_VOICE_MODE_SUPPORT) {
                    sendMsg(this.mAudioHandler, 10001, 0, 0, 0, null, 500);
                }
                sendCommForceBroadcast();
            }
        }
    }

    public boolean isSpeakerphoneOn() {
        return this.mForcedUseForCommExt == 1;
    }

    public void setBluetoothScoOn(boolean on) {
        if (!checkAudioSettingsPermission("setBluetoothScoOn()")) {
            return;
        }
        if (UserHandle.getAppId(Binder.getCallingUid()) >= 10000) {
            int i = 3;
            if (!on) {
                i = this.mForcedUseForCommExt == 3 ? 0 : this.mForcedUseForCommExt;
            }
            this.mForcedUseForCommExt = i;
            Log.d(TAG, "Only enable calls from system components.");
            return;
        }
        String eventSource = new StringBuilder("setBluetoothScoOn(");
        eventSource.append(on);
        eventSource.append(") from u/pid:");
        eventSource.append(Binder.getCallingUid());
        eventSource.append(SliceAuthority.DELIMITER);
        eventSource.append(Binder.getCallingPid());
        eventSource = eventSource.toString();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ASsbso");
        stringBuilder.append(on);
        if (checkAudioSettingAllowed(stringBuilder.toString())) {
            setBluetoothScoOnInt(on, eventSource);
        }
    }

    /* JADX WARNING: Missing block: B:13:0x0059, code:
            r10.mForcedUseForComm = 3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setBluetoothScoOnInt(boolean on, String eventSource) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setBluetoothScoOnInt: ");
        stringBuilder.append(on);
        stringBuilder.append(" ");
        stringBuilder.append(eventSource);
        Log.i(str, stringBuilder.toString());
        if (on) {
            synchronized (this.mScoClients) {
                if (this.mBluetoothHeadset == null || this.mBluetoothHeadset.getAudioState(this.mBluetoothHeadsetDevice) == 12) {
                } else {
                    this.mForcedUseForCommExt = 3;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("setBluetoothScoOnInt(true) failed because ");
                    stringBuilder.append(this.mBluetoothHeadsetDevice);
                    stringBuilder.append(" is not in audio connected mode");
                    Log.w(str, stringBuilder.toString());
                    return;
                }
            }
        } else if (this.mForcedUseForComm == 3) {
            this.mForcedUseForComm = 0;
        }
        this.mForcedUseForCommExt = this.mForcedUseForComm;
        stringBuilder = new StringBuilder();
        stringBuilder.append("BT_SCO=");
        stringBuilder.append(on ? "on" : "off");
        AudioSystem.setParameters(stringBuilder.toString());
        String str2 = eventSource;
        sendMsg(this.mAudioHandler, 8, 2, 0, this.mForcedUseForComm, str2, 0);
        sendMsg(this.mAudioHandler, 8, 2, 2, this.mForcedUseForComm, str2, 0);
        setRingerModeInt(getRingerModeInternal(), false);
        if (LOUD_VOICE_MODE_SUPPORT) {
            sendMsg(this.mAudioHandler, 10001, 0, 0, 0, null, 500);
        }
        sendCommForceBroadcast();
    }

    public boolean isBluetoothScoOn() {
        return this.mForcedUseForCommExt == 3;
    }

    public void setBluetoothA2dpOn(boolean on) {
        String eventSource = new StringBuilder("setBluetoothA2dpOn(");
        eventSource.append(on);
        eventSource.append(") from u/pid:");
        eventSource.append(Binder.getCallingUid());
        eventSource.append(SliceAuthority.DELIMITER);
        eventSource.append(Binder.getCallingPid());
        eventSource = eventSource.toString();
        Log.d(TAG, eventSource);
        synchronized (this.mBluetoothA2dpEnabledLock) {
            if (this.mBluetoothA2dpEnabled == on) {
                return;
            }
            this.mBluetoothA2dpEnabled = on;
            sendMsg(this.mAudioHandler, 13, 2, 1, this.mBluetoothA2dpEnabled ? 0 : 10, eventSource, 0);
        }
    }

    public boolean isBluetoothA2dpOn() {
        boolean z;
        synchronized (this.mBluetoothA2dpEnabledLock) {
            z = this.mBluetoothA2dpEnabled;
        }
        return z;
    }

    public void startBluetoothSco(IBinder cb, int targetSdkVersion) {
        startBluetoothScoInt(cb, targetSdkVersion < 18 ? 0 : -1);
    }

    public void startBluetoothScoVirtualCall(IBinder cb) {
        startBluetoothScoInt(cb, 0);
    }

    void startBluetoothScoInt(IBinder cb, int scoAudioMode) {
        if (checkAudioSettingsPermission("startBluetoothSco()") && this.mSystemReady) {
            ScoClient client = getScoClient(cb, true);
            long ident = Binder.clearCallingIdentity();
            client.incCount(scoAudioMode);
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void stopBluetoothSco(IBinder cb) {
        if (checkAudioSettingsPermission("stopBluetoothSco()") && this.mSystemReady) {
            ScoClient client = getScoClient(cb, null);
            long ident = Binder.clearCallingIdentity();
            if (client != null) {
                client.decCount();
            } else if (this.mBluetoothHeadset != null) {
                boolean status = this.mBluetoothHeadset.stopScoUsingVirtualVoiceCall();
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("stopBluetoothSco(), stopScoUsingVirtualVoiceCall:");
                stringBuilder.append(status);
                Log.d(str, stringBuilder.toString());
            }
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void checkScoAudioState() {
        synchronized (this.mScoClients) {
            if (!(this.mBluetoothHeadset == null || this.mBluetoothHeadsetDevice == null || this.mScoAudioState != 0 || this.mBluetoothHeadset.getAudioState(this.mBluetoothHeadsetDevice) == 10)) {
                this.mScoAudioState = 2;
            }
        }
    }

    private ScoClient getScoClient(IBinder cb, boolean create) {
        synchronized (this.mScoClients) {
            Iterator it = this.mScoClients.iterator();
            while (it.hasNext()) {
                ScoClient existingClient = (ScoClient) it.next();
                if (existingClient.getBinder() == cb) {
                    return existingClient;
                }
            }
            if (create) {
                ScoClient newClient = new ScoClient(cb);
                this.mScoClients.add(newClient);
                return newClient;
            }
            return null;
        }
    }

    public void clearAllScoClients(int exceptPid, boolean stopSco) {
        synchronized (this.mScoClients) {
            ScoClient savedClient = null;
            Iterator it = this.mScoClients.iterator();
            while (it.hasNext()) {
                ScoClient cl = (ScoClient) it.next();
                if (cl.getPid() != exceptPid) {
                    cl.clearCount(stopSco);
                } else {
                    savedClient = cl;
                }
            }
            this.mScoClients.clear();
            if (savedClient != null) {
                this.mScoClients.add(savedClient);
            }
        }
    }

    private boolean getBluetoothHeadset() {
        boolean result = false;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            result = adapter.getProfileProxy(this.mContext, this.mBluetoothProfileServiceListener, 1);
        }
        sendMsg(this.mAudioHandler, 9, 0, 0, 0, null, result ? 3000 : 0);
        return result;
    }

    private void disconnectBluetoothSco(int exceptPid) {
        synchronized (this.mScoClients) {
            checkScoAudioState();
            if (this.mScoAudioState == 2) {
                return;
            }
            clearAllScoClients(exceptPid, true);
        }
    }

    private static boolean disconnectBluetoothScoAudioHelper(BluetoothHeadset bluetoothHeadset, BluetoothDevice device, int scoAudioMode) {
        switch (scoAudioMode) {
            case 0:
                return bluetoothHeadset.stopScoUsingVirtualVoiceCall();
            case 1:
                return bluetoothHeadset.disconnectAudio();
            case 2:
                return bluetoothHeadset.stopVoiceRecognition(device);
            default:
                return false;
        }
    }

    private static boolean connectBluetoothScoAudioHelper(BluetoothHeadset bluetoothHeadset, BluetoothDevice device, int scoAudioMode) {
        switch (scoAudioMode) {
            case 0:
                return bluetoothHeadset.startScoUsingVirtualVoiceCall();
            case 1:
                return bluetoothHeadset.connectAudio();
            case 2:
                return bluetoothHeadset.startVoiceRecognition(device);
            default:
                return false;
        }
    }

    private void resetBluetoothSco() {
        synchronized (this.mScoClients) {
            clearAllScoClients(0, false);
            this.mScoAudioState = 0;
            broadcastScoConnectionState(0);
        }
        AudioSystem.setParameters("A2dpSuspended=false");
        setBluetoothScoOnInt(false, "resetBluetoothSco");
    }

    private void broadcastScoConnectionState(int state) {
        sendMsg(this.mAudioHandler, 19, 2, state, 0, null, 0);
    }

    private void onBroadcastScoConnectionState(int state) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onBroadcastScoConnectionState() state=");
        stringBuilder.append(state);
        stringBuilder.append(", pre-state=");
        stringBuilder.append(this.mScoConnectionState);
        Log.i(str, stringBuilder.toString());
        if (state != this.mScoConnectionState) {
            Intent newIntent = new Intent("android.media.ACTION_SCO_AUDIO_STATE_UPDATED");
            newIntent.putExtra("android.media.extra.SCO_AUDIO_STATE", state);
            newIntent.putExtra("android.media.extra.SCO_AUDIO_PREVIOUS_STATE", this.mScoConnectionState);
            sendStickyBroadcastToAll(newIntent);
            this.mScoConnectionState = state;
        }
    }

    private boolean handleBtScoActiveDeviceChange(BluetoothDevice btDevice, boolean isActive) {
        boolean result = true;
        if (btDevice == null) {
            return true;
        }
        String address = btDevice.getAddress();
        BluetoothClass btClass = btDevice.getBluetoothClass();
        int[] outDeviceTypes = new int[]{16, 32, 64};
        if (btClass != null) {
            int deviceClass = btClass.getDeviceClass();
            if (deviceClass == UsbTerminalTypes.TERMINAL_BIDIR_SKRPHONE_SUPRESS || deviceClass == 1032) {
                outDeviceTypes = new int[]{32};
            } else if (deviceClass == 1056) {
                outDeviceTypes = new int[]{64};
            }
        }
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            address = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        String btDeviceName = btDevice.getName();
        boolean result2;
        if (isActive) {
            result2 = false | handleDeviceConnection(isActive, outDeviceTypes[0], address, btDeviceName);
        } else {
            boolean result3 = false;
            for (int outDeviceType : outDeviceTypes) {
                result3 |= handleDeviceConnection(isActive, outDeviceType, address, btDeviceName);
            }
            result2 = result3;
        }
        if (!(handleDeviceConnection(isActive, -2147483640, address, btDeviceName) && result2)) {
            result = false;
        }
        return result;
    }

    private void setBtScoActiveDevice(BluetoothDevice btDevice) {
        synchronized (this.mScoClients) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setBtScoActiveDevice: ");
            stringBuilder.append(this.mBluetoothHeadsetDevice);
            stringBuilder.append(" -> ");
            stringBuilder.append(btDevice);
            Log.i(str, stringBuilder.toString());
            BluetoothDevice previousActiveDevice = this.mBluetoothHeadsetDevice;
            if (!Objects.equals(btDevice, previousActiveDevice)) {
                String str2;
                StringBuilder stringBuilder2;
                if (!handleBtScoActiveDeviceChange(previousActiveDevice, false)) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("setBtScoActiveDevice() failed to remove previous device ");
                    stringBuilder2.append(previousActiveDevice);
                    Log.w(str2, stringBuilder2.toString());
                }
                if (!handleBtScoActiveDeviceChange(btDevice, true)) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("setBtScoActiveDevice() failed to add new device ");
                    stringBuilder2.append(btDevice);
                    Log.e(str2, stringBuilder2.toString());
                    btDevice = null;
                }
                this.mBluetoothHeadsetDevice = btDevice;
                if (this.mBluetoothHeadsetDevice == null) {
                    resetBluetoothSco();
                }
            }
        }
    }

    void disconnectAllBluetoothProfiles() {
        disconnectA2dp();
        disconnectA2dpSink();
        disconnectHeadset();
        disconnectHearingAid();
    }

    void disconnectA2dp() {
        synchronized (this.mConnectedDevices) {
            synchronized (this.mA2dpAvrcpLock) {
                int i;
                int i2 = 0;
                ArraySet<String> toRemove = null;
                for (i = 0; i < this.mConnectedDevices.size(); i++) {
                    DeviceListSpec deviceSpec = (DeviceListSpec) this.mConnectedDevices.valueAt(i);
                    if (deviceSpec.mDeviceType == 128) {
                        toRemove = toRemove != null ? toRemove : new ArraySet();
                        toRemove.add(deviceSpec.mDeviceAddress);
                    }
                }
                if (toRemove != null) {
                    i = checkSendBecomingNoisyIntent(128, 0, 0);
                    while (i2 < toRemove.size()) {
                        makeA2dpDeviceUnavailableLater((String) toRemove.valueAt(i2), i);
                        i2++;
                    }
                }
            }
        }
    }

    void disconnectA2dpSink() {
        synchronized (this.mConnectedDevices) {
            int i;
            int i2 = 0;
            ArraySet<String> toRemove = null;
            for (i = 0; i < this.mConnectedDevices.size(); i++) {
                DeviceListSpec deviceSpec = (DeviceListSpec) this.mConnectedDevices.valueAt(i);
                if (deviceSpec.mDeviceType == -2147352576) {
                    toRemove = toRemove != null ? toRemove : new ArraySet();
                    toRemove.add(deviceSpec.mDeviceAddress);
                }
            }
            if (toRemove != null) {
                while (true) {
                    i = i2;
                    if (i >= toRemove.size()) {
                        break;
                    }
                    makeA2dpSrcUnavailable((String) toRemove.valueAt(i));
                    i2 = i + 1;
                }
            }
        }
    }

    void disconnectHeadset() {
        synchronized (this.mScoClients) {
            setBtScoActiveDevice(null);
            this.mBluetoothHeadset = null;
        }
    }

    void disconnectHearingAid() {
        synchronized (this.mConnectedDevices) {
            synchronized (this.mHearingAidLock) {
                int i;
                int i2 = 0;
                ArraySet<String> toRemove = null;
                for (i = 0; i < this.mConnectedDevices.size(); i++) {
                    DeviceListSpec deviceSpec = (DeviceListSpec) this.mConnectedDevices.valueAt(i);
                    if (deviceSpec.mDeviceType == 134217728) {
                        toRemove = toRemove != null ? toRemove : new ArraySet();
                        toRemove.add(deviceSpec.mDeviceAddress);
                    }
                }
                if (toRemove != null) {
                    i = checkSendBecomingNoisyIntent(134217728, 0, 0);
                    while (i2 < toRemove.size()) {
                        makeHearingAidDeviceUnavailable((String) toRemove.valueAt(i2));
                        i2++;
                    }
                }
            }
        }
    }

    private void onCheckMusicActive(String caller) {
        synchronized (this.mSafeMediaVolumeState) {
            if (this.mSafeMediaVolumeState.intValue() == 2) {
                int device = getDeviceForStream(3);
                if ((603979788 & device) != 0) {
                    if (!this.mHasAlarm) {
                        sendMsg(this.mAudioHandler, 14, 0, 0, 0, caller, MUSIC_ACTIVE_POLL_PERIOD_MS);
                    }
                    this.mSafeVolumeCaller = caller;
                    int index = this.mStreamStates[3].getIndex(device);
                    if (AudioSystem.isStreamActive(3, 0) && index > safeMediaVolumeIndex(device)) {
                        this.mMusicActiveMs += MUSIC_ACTIVE_POLL_PERIOD_MS;
                        if (this.mMusicActiveMs > UNSAFE_VOLUME_MUSIC_ACTIVE_MS_MAX) {
                            setSafeMediaVolumeEnabled(true, caller);
                            this.mMusicActiveMs = 0;
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("music is active more than ");
                            stringBuilder.append(UNSAFE_VOLUME_MUSIC_ACTIVE_MS_MAX);
                            stringBuilder.append(", so reset to safe volume: ");
                            stringBuilder.append(safeMediaVolumeIndex(device));
                            stringBuilder.append(" for device: ");
                            stringBuilder.append(device);
                            Log.d(str, stringBuilder.toString());
                            HwMediaMonitorManager.writeLogMsg(916010205, 1, 0, "OCMA");
                        }
                        saveMusicActiveMs();
                    }
                }
            }
        }
    }

    private void saveMusicActiveMs() {
        this.mAudioHandler.obtainMessage(22, this.mMusicActiveMs, 0).sendToTarget();
    }

    private int getSafeUsbMediaVolumeIndex() {
        int min = MIN_STREAM_VOLUME[3];
        int max = MAX_STREAM_VOLUME[3];
        this.mSafeUsbMediaVolumeDbfs = ((float) this.mContext.getResources().getInteger(17694853)) / 100.0f;
        while (Math.abs(max - min) > 1) {
            int index = (max + min) / 2;
            float gainDB = AudioSystem.getStreamVolumeDB(3, index, 1.5046328E-36f);
            if (Float.isNaN(gainDB)) {
                break;
            } else if (gainDB == this.mSafeUsbMediaVolumeDbfs) {
                min = index;
                break;
            } else {
                if (gainDB < this.mSafeUsbMediaVolumeDbfs) {
                }
                max = index;
            }
        }
        return min * 10;
    }

    private void onConfigureSafeVolume(boolean force, String caller) {
        synchronized (this.mSafeMediaVolumeState) {
            int mcc = this.mContext.getResources().getConfiguration().mcc;
            if (this.mMcc != mcc || (this.mMcc == 0 && force)) {
                int persistedState;
                this.mSafeMediaVolumeIndex = this.mContext.getResources().getInteger(17694852) * 10;
                this.mSafeUsbMediaVolumeIndex = getSafeUsbMediaVolumeIndex();
                boolean safeMediaVolumeEnabled = SystemProperties.getBoolean("audio.safemedia.force", false) || this.mContext.getResources().getBoolean(17957011);
                boolean safeMediaVolumeBypass = SystemProperties.getBoolean("audio.safemedia.bypass", false);
                if (usingHwSafeMediaConfig()) {
                    this.mSafeMediaVolumeIndex = getHwSafeMediaVolumeIndex();
                    safeMediaVolumeEnabled = isHwSafeMediaVolumeEnabled();
                }
                if (!safeMediaVolumeEnabled || safeMediaVolumeBypass) {
                    this.mSafeMediaVolumeState = Integer.valueOf(1);
                    persistedState = 1;
                } else {
                    persistedState = 3;
                    if (this.mSafeMediaVolumeState.intValue() != 2) {
                        if (this.mMusicActiveMs == 0) {
                            this.mSafeMediaVolumeState = Integer.valueOf(3);
                            enforceSafeMediaVolume(caller);
                        } else {
                            this.mSafeMediaVolumeState = Integer.valueOf(2);
                        }
                    }
                }
                this.mMcc = mcc;
                sendMsg(this.mAudioHandler, 18, 2, persistedState, 0, null, 0);
            }
        }
    }

    private int checkForRingerModeChange(int oldIndex, int direction, int step, boolean isMuted, String caller, int flags) {
        int result = 1;
        if (isPlatformTelevision() || this.mIsSingleVolume) {
            return 1;
        }
        int ringerMode = getRingerModeInternal();
        switch (ringerMode) {
            case 0:
                if (this.mIsSingleVolume && direction == -1 && oldIndex >= 2 * step && isMuted) {
                    ringerMode = 2;
                } else if (direction == 1 || direction == 101 || direction == 100) {
                    if (!this.mVolumePolicy.volumeUpToExitSilent) {
                        result = 1 | 128;
                    } else if (this.mHasVibrator && direction == 1) {
                        ringerMode = 1;
                    } else {
                        ringerMode = 2;
                        result = 1 | 2;
                    }
                }
                result &= -2;
                break;
            case 1:
                if (!this.mHasVibrator) {
                    Log.e(TAG, "checkForRingerModeChange() current ringer mode is vibratebut no vibrator is present");
                    break;
                }
                if (direction == -1) {
                    if (this.mIsSingleVolume && oldIndex >= 2 * step && isMuted) {
                        ringerMode = 2;
                    } else if (this.mPrevVolDirection != -1) {
                        if (!this.mVolumePolicy.volumeDownToEnterSilent) {
                            result = 1 | 2048;
                        } else if (SystemClock.uptimeMillis() - this.mLoweredFromNormalToVibrateTime > ((long) this.mVolumePolicy.vibrateToSilentDebounce) && this.mRingerModeDelegate.canVolumeDownEnterSilent()) {
                            ringerMode = 0;
                        }
                    }
                } else if (direction == 1 || direction == 101 || direction == 100) {
                    ringerMode = 2;
                    result = 1 | 2;
                }
                result &= -2;
                break;
            case 2:
                if (direction != -1) {
                    if (this.mIsSingleVolume && (direction == 101 || direction == -100)) {
                        if (this.mHasVibrator) {
                            ringerMode = 1;
                        } else {
                            ringerMode = 0;
                        }
                        result = 1 & -2;
                        break;
                    }
                } else if (!this.mHasVibrator) {
                    if (oldIndex == step && this.mVolumePolicy.volumeDownToEnterSilent) {
                        ringerMode = 0;
                        break;
                    }
                } else if (step <= oldIndex && oldIndex < 2 * step) {
                    ringerMode = 1;
                    this.mLoweredFromNormalToVibrateTime = SystemClock.uptimeMillis();
                    break;
                }
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("checkForRingerModeChange() wrong ringer mode: ");
                stringBuilder.append(ringerMode);
                Log.e(str, stringBuilder.toString());
                break;
        }
        if (isAndroidNPlus(caller) && wouldToggleZenMode(ringerMode) && !this.mNm.isNotificationPolicyAccessGrantedForPackage(caller) && (flags & 4096) == 0) {
            throw new SecurityException("Not allowed to change Do Not Disturb state");
        }
        setRingerMode(ringerMode, "AudioService.checkForRingerModeChange", false);
        this.mPrevVolDirection = direction;
        return result;
    }

    public boolean isStreamAffectedByRingerMode(int streamType) {
        return (this.mRingerModeAffectedStreams & (1 << streamType)) != 0;
    }

    private boolean shouldZenMuteStream(int streamType) {
        boolean z = false;
        if (this.mNm.getZenMode() != 1) {
            return false;
        }
        Policy zenPolicy = this.mNm.getNotificationPolicy();
        boolean muteAlarms = (zenPolicy.priorityCategories & 32) == 0;
        boolean muteMedia = (zenPolicy.priorityCategories & 64) == 0;
        boolean muteSystem = (zenPolicy.priorityCategories & 128) == 0;
        boolean muteNotificationAndRing = ZenModeConfig.areAllPriorityOnlyNotificationZenSoundsMuted(this.mNm.getNotificationPolicy());
        if ((muteAlarms && isAlarm(streamType)) || ((muteMedia && isMedia(streamType)) || ((muteSystem && isSystem(streamType)) || (muteNotificationAndRing && isNotificationOrRinger(streamType))))) {
            z = true;
        }
        return z;
    }

    private boolean isStreamMutedByRingerOrZenMode(int streamType) {
        return (this.mRingerAndZenModeMutedStreams & (1 << streamType)) != 0;
    }

    private boolean updateZenModeAffectedStreams() {
        int zenModeAffectedStreams = 0;
        if (this.mSystemReady && this.mNm.getZenMode() == 1) {
            Policy zenPolicy = this.mNm.getNotificationPolicy();
            if ((zenPolicy.priorityCategories & 32) == 0) {
                zenModeAffectedStreams = 0 | 16;
            }
            if ((zenPolicy.priorityCategories & 64) == 0) {
                zenModeAffectedStreams |= 8;
            }
            if ((zenPolicy.priorityCategories & 128) == 0) {
                zenModeAffectedStreams |= 2;
            }
        }
        if (this.mZenModeAffectedStreams == zenModeAffectedStreams) {
            return false;
        }
        this.mZenModeAffectedStreams = zenModeAffectedStreams;
        return true;
    }

    @GuardedBy("mSettingsLock")
    private boolean updateRingerAndZenModeAffectedStreams() {
        boolean updatedZenModeAffectedStreams = updateZenModeAffectedStreams();
        int ringerModeAffectedStreams = System.getIntForUser(this.mContentResolver, "mode_ringer_streams_affected", 166, -2);
        if (this.mIsSingleVolume) {
            ringerModeAffectedStreams = 0;
        } else if (this.mRingerModeDelegate != null) {
            ringerModeAffectedStreams = this.mRingerModeDelegate.getRingerModeAffectedStreams(ringerModeAffectedStreams);
        }
        if (this.mCameraSoundForced) {
            ringerModeAffectedStreams &= -129;
        } else {
            ringerModeAffectedStreams |= 128;
        }
        if (mStreamVolumeAlias[8] == 2) {
            ringerModeAffectedStreams |= 256;
        } else {
            ringerModeAffectedStreams &= -257;
        }
        if (ringerModeAffectedStreams != this.mRingerModeAffectedStreams) {
            System.putIntForUser(this.mContentResolver, "mode_ringer_streams_affected", ringerModeAffectedStreams, -2);
            this.mRingerModeAffectedStreams = ringerModeAffectedStreams;
            return true;
        } else if (ringerModeAffectedStreams != this.mRingerModeAffectedStreams || ActivityManager.getCurrentUser() == 0) {
            return updatedZenModeAffectedStreams;
        } else {
            System.putIntForUser(this.mContentResolver, "mode_ringer_streams_affected", ringerModeAffectedStreams, -2);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateRingerModeAffectedStreams enter sub user ringerModeAffectedStreams:");
            stringBuilder.append(ringerModeAffectedStreams);
            Log.d(str, stringBuilder.toString());
            return true;
        }
    }

    public boolean isStreamAffectedByMute(int streamType) {
        return (this.mMuteAffectedStreams & (1 << streamType)) != 0;
    }

    private void ensureValidDirection(int direction) {
        if (direction != -100) {
            switch (direction) {
                case -1:
                case 0:
                case 1:
                    return;
                default:
                    switch (direction) {
                        case 100:
                        case 101:
                            return;
                        default:
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Bad direction ");
                            stringBuilder.append(direction);
                            throw new IllegalArgumentException(stringBuilder.toString());
                    }
            }
        }
    }

    private void ensureValidStreamType(int streamType) {
        if (streamType < 0 || streamType >= this.mStreamStates.length) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Bad stream type ");
            stringBuilder.append(streamType);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private boolean isMuteAdjust(int adjust) {
        return adjust == -100 || adjust == 100 || adjust == 101;
    }

    private boolean isInCommunication() {
        TelecomManager telecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
        long ident = Binder.clearCallingIdentity();
        boolean IsInCall = telecomManager.isInCall();
        Binder.restoreCallingIdentity(ident);
        return IsInCall || getMode() == 3 || getMode() == 2;
    }

    private boolean wasStreamActiveRecently(int stream, int delay_ms) {
        return AudioSystem.isStreamActive(stream, delay_ms) || AudioSystem.isStreamActiveRemotely(stream, delay_ms);
    }

    protected int getActiveStreamType(int suggestedStreamType) {
        if (this.mIsSingleVolume && suggestedStreamType == Integer.MIN_VALUE) {
            return 3;
        }
        if (this.mPlatformType == 1) {
            if (isInCommunication()) {
                return AudioSystem.getForceUse(0) == 3 ? 6 : 0;
            } else {
                if (suggestedStreamType == Integer.MIN_VALUE) {
                    if (wasStreamActiveRecently(3, 0)) {
                        Log.v(TAG, "getActiveStreamType: Forcing STREAM_MUSIC b/c default");
                        return 3;
                    } else if (wasStreamActiveRecently(0, 0)) {
                        if (AudioSystem.getForceUse(0) == 3) {
                            Log.v(TAG, "getActiveStreamType: STREAM_VOICE_CALL is active, but eForcing STREAM_BLUETOOTH_SCO...");
                            return 6;
                        }
                        Log.v(TAG, "getActiveStreamType: Forcing STREAM_VOICE_CALL stream active");
                        return 0;
                    } else if (wasStreamActiveRecently(6, 0)) {
                        Log.v(TAG, "getActiveStreamType: Forcing STREAM_BLUETOOTH_SCO stream active");
                        return 6;
                    } else if (wasStreamActiveRecently(2, 0) || wasStreamActiveRecently(1, 0)) {
                        Log.v(TAG, "getActiveStreamType: Forcing STREAM_RING stream active");
                        return 2;
                    } else if (wasStreamActiveRecently(5, sStreamOverrideDelayMs)) {
                        Log.v(TAG, "getActiveStreamType: Forcing STREAM_NOTIFICATION stream active");
                        return 5;
                    } else {
                        Log.v(TAG, "getActiveStreamType: Forcing DEFAULT_VOL_STREAM_NO_PLAYBACK(3) b/c default");
                        return 3;
                    }
                } else if (wasStreamActiveRecently(5, sStreamOverrideDelayMs)) {
                    Log.v(TAG, "getActiveStreamType: Forcing STREAM_NOTIFICATION stream active");
                    return 5;
                } else if (wasStreamActiveRecently(2, sStreamOverrideDelayMs)) {
                    Log.v(TAG, "getActiveStreamType: Forcing STREAM_RING stream active");
                    return 2;
                }
            }
        }
        if (isInCommunication()) {
            if (AudioSystem.getForceUse(0) == 3) {
                Log.v(TAG, "getActiveStreamType: Forcing STREAM_BLUETOOTH_SCO");
                return 6;
            }
            Log.v(TAG, "getActiveStreamType: Forcing STREAM_VOICE_CALL");
            return 0;
        } else if (AudioSystem.isStreamActive(5, sStreamOverrideDelayMs)) {
            Log.v(TAG, "getActiveStreamType: Forcing STREAM_NOTIFICATION");
            return 5;
        } else if (AudioSystem.isStreamActive(2, sStreamOverrideDelayMs)) {
            Log.v(TAG, "getActiveStreamType: Forcing STREAM_RING");
            return 2;
        } else if (suggestedStreamType != Integer.MIN_VALUE) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getActiveStreamType: Returning suggested type ");
            stringBuilder.append(suggestedStreamType);
            Log.v(str, stringBuilder.toString());
            return suggestedStreamType;
        } else if (AudioSystem.isStreamActive(5, sStreamOverrideDelayMs)) {
            Log.v(TAG, "getActiveStreamType: Forcing STREAM_NOTIFICATION");
            return 5;
        } else if (AudioSystem.isStreamActive(2, sStreamOverrideDelayMs)) {
            Log.v(TAG, "getActiveStreamType: Forcing STREAM_RING");
            return 2;
        } else {
            Log.v(TAG, "getActiveStreamType: Forcing DEFAULT_VOL_STREAM_NO_PLAYBACK(3) b/c default");
            return 3;
        }
    }

    private void broadcastRingerMode(String action, int ringerMode) {
        Intent broadcast = new Intent(action);
        broadcast.putExtra("android.media.EXTRA_RINGER_MODE", ringerMode);
        broadcast.addFlags(603979776);
        sendStickyBroadcastToAll(broadcast);
    }

    private void broadcastVibrateSetting(int vibrateType) {
        if (this.mActivityManagerInternal.isSystemReady()) {
            Intent broadcast = new Intent("android.media.VIBRATE_SETTING_CHANGED");
            broadcast.putExtra("android.media.EXTRA_VIBRATE_TYPE", vibrateType);
            broadcast.putExtra("android.media.EXTRA_VIBRATE_SETTING", getVibrateSetting(vibrateType));
            sendBroadcastToAll(broadcast);
        }
    }

    private void queueMsgUnderWakeLock(Handler handler, int msg, int arg1, int arg2, Object obj, int delay) {
        long ident = Binder.clearCallingIdentity();
        this.mAudioEventWakeLock.acquire();
        Binder.restoreCallingIdentity(ident);
        sendMsg(handler, msg, 2, arg1, arg2, obj, delay);
    }

    protected static void sendMsg(Handler handler, int msg, int existingMsgPolicy, int arg1, int arg2, Object obj, int delay) {
        if (existingMsgPolicy == 0) {
            handler.removeMessages(msg);
        } else if (existingMsgPolicy == 1 && handler.hasMessages(msg)) {
            return;
        }
        synchronized (mLastDeviceConnectMsgTime) {
            long time = SystemClock.uptimeMillis() + ((long) delay);
            if (msg == 101 || msg == 102 || msg == 105 || msg == 100 || msg == 103 || msg == 106) {
                if (mLastDeviceConnectMsgTime.longValue() >= time) {
                    time = mLastDeviceConnectMsgTime.longValue() + 30;
                }
                mLastDeviceConnectMsgTime = Long.valueOf(time);
            }
            if (!handler.sendMessageAtTime(handler.obtainMessage(msg, arg1, arg2, obj), time)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("send msg:");
                stringBuilder.append(msg);
                stringBuilder.append(" failed!");
                Log.e(str, stringBuilder.toString());
            }
        }
    }

    boolean checkAudioSettingsPermission(String method) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_AUDIO_SETTINGS") == 0) {
            return true;
        }
        String msg = new StringBuilder();
        msg.append("Audio Settings Permission Denial: ");
        msg.append(method);
        msg.append(" from pid=");
        msg.append(Binder.getCallingPid());
        msg.append(", uid=");
        msg.append(Binder.getCallingUid());
        Log.w(TAG, msg.toString());
        return false;
    }

    private int getDeviceForStream(int stream) {
        int device = getDevicesForStream(stream);
        if (((device - 1) & device) == 0) {
            return device;
        }
        if ((device & 2) != 0) {
            return 2;
        }
        if ((262144 & device) != 0) {
            return 262144;
        }
        if ((DumpState.DUMP_FROZEN & device) != 0) {
            return DumpState.DUMP_FROZEN;
        }
        if ((DumpState.DUMP_COMPILER_STATS & device) != 0) {
            return DumpState.DUMP_COMPILER_STATS;
        }
        return device & 896;
    }

    private int getDevicesForStream(int stream) {
        return getDevicesForStream(stream, true);
    }

    private int getDevicesForStream(int stream, boolean checkOthers) {
        int observeDevicesForStream_syncVSS;
        ensureValidStreamType(stream);
        synchronized (VolumeStreamState.class) {
            observeDevicesForStream_syncVSS = this.mStreamStates[stream].observeDevicesForStream_syncVSS(checkOthers);
        }
        return observeDevicesForStream_syncVSS;
    }

    private void observeDevicesForStreams(int skipStream) {
        synchronized (VolumeStreamState.class) {
            for (int stream = 0; stream < this.mStreamStates.length; stream++) {
                if (stream != skipStream) {
                    this.mStreamStates[stream].observeDevicesForStream_syncVSS(false);
                }
            }
        }
    }

    public void setWiredDeviceConnectionState(int type, int state, String address, String name, String caller) {
        Throwable th;
        int i = type;
        int i2 = state;
        ArrayMap arrayMap = this.mConnectedDevices;
        synchronized (arrayMap) {
            ArrayMap arrayMap2;
            try {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setWiredDeviceConnectionState(");
                stringBuilder.append(i2);
                stringBuilder.append(" nm: ");
                String str2 = name;
                stringBuilder.append(str2);
                stringBuilder.append(" addr:");
                String str3 = address;
                stringBuilder.append(str3);
                stringBuilder.append(") type: ");
                stringBuilder.append(i);
                stringBuilder.append(" caller: ");
                String str4 = caller;
                stringBuilder.append(str4);
                Slog.i(str, stringBuilder.toString());
                int delay = checkSendBecomingNoisyIntent(i, i2, 0);
                arrayMap2 = arrayMap;
                queueMsgUnderWakeLock(this.mAudioHandler, 100, 0, 0, new WiredDeviceConnectionState(i, i2, str3, str2, str4), delay);
                if (SOUND_EFFECTS_SUPPORT) {
                    queueMsgUnderWakeLock(this.mAudioHandler, 10002, type, state, name, delay);
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    public void setHearingAidDeviceConnectionState(BluetoothDevice device, int state) {
        Log.i(TAG, "setBluetoothHearingAidDeviceConnectionState");
        setBluetoothHearingAidDeviceConnectionState(device, state, false, 0);
    }

    public int setBluetoothHearingAidDeviceConnectionState(BluetoothDevice device, int state, boolean suppressNoisyIntent, int musicDevice) {
        int intState;
        synchronized (this.mConnectedDevices) {
            intState = 0;
            if (!suppressNoisyIntent) {
                if (state == 2) {
                    intState = 1;
                }
                intState = checkSendBecomingNoisyIntent(134217728, intState, musicDevice);
            }
            queueMsgUnderWakeLock(this.mAudioHandler, 105, state, 0, device, intState);
        }
        return intState;
    }

    public int setBluetoothA2dpDeviceConnectionState(BluetoothDevice device, int state, int profile) {
        return setBluetoothA2dpDeviceConnectionStateSuppressNoisyIntent(device, state, profile, false, -1);
    }

    public int setBluetoothA2dpDeviceConnectionStateSuppressNoisyIntent(BluetoothDevice device, int state, int profile, boolean suppressNoisyIntent, int a2dpVolume) {
        if (this.mAudioHandler.hasMessages(102, device)) {
            return 0;
        }
        return setBluetoothA2dpDeviceConnectionStateInt(device, state, profile, suppressNoisyIntent, 0, a2dpVolume);
    }

    public int setBluetoothA2dpDeviceConnectionStateInt(BluetoothDevice device, int state, int profile, boolean suppressNoisyIntent, int musicDevice, int a2dpVolume) {
        Throwable th;
        int i = state;
        int i2 = profile;
        boolean z = suppressNoisyIntent;
        StringBuilder stringBuilder;
        if (i2 == 2 || i2 == 11) {
            synchronized (this.mConnectedDevices) {
                BluetoothDevice bluetoothDevice;
                int intState = 0;
                if (i2 != 2 || z) {
                    int i3 = musicDevice;
                } else {
                    if (i == 2) {
                        intState = 1;
                    }
                    try {
                        intState = checkSendBecomingNoisyIntent(128, intState, musicDevice);
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                }
                int delay = intState;
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setBluetoothA2dpDeviceConnectionStateInt device: ");
                bluetoothDevice = device;
                stringBuilder2.append(bluetoothDevice);
                stringBuilder2.append(" state: ");
                stringBuilder2.append(i);
                stringBuilder2.append(" delay(ms): ");
                stringBuilder2.append(delay);
                stringBuilder2.append(" suppressNoisyIntent: ");
                stringBuilder2.append(z);
                Log.d(str, stringBuilder2.toString());
                queueMsgUnderWakeLock(this.mAudioHandler, i2 == 2 ? 102 : 101, i, a2dpVolume, bluetoothDevice, delay);
                String str2 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("state: ");
                stringBuilder.append(i);
                stringBuilder.append(" delay: ");
                stringBuilder.append(delay);
                Log.v(str2, stringBuilder.toString());
                return delay;
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("invalid profile ");
        stringBuilder.append(i2);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public void handleBluetoothA2dpDeviceConfigChange(BluetoothDevice device) {
        synchronized (this.mConnectedDevices) {
            queueMsgUnderWakeLock(this.mAudioHandler, 103, 0, 0, device, 0);
        }
    }

    private void onAccessoryPlugMediaUnmute(int newDevice) {
        Log.i(TAG, String.format("onAccessoryPlugMediaUnmute newDevice=%d [%s]", new Object[]{Integer.valueOf(newDevice), AudioSystem.getOutputDeviceName(newDevice)}));
        synchronized (this.mConnectedDevices) {
            if (!(this.mNm.getZenMode() == 2 || (DEVICE_MEDIA_UNMUTED_ON_PLUG & newDevice) == 0 || !this.mStreamStates[3].mIsMuted || this.mStreamStates[3].getIndex(newDevice) == 0 || ((604004352 | newDevice) & AudioSystem.getDevicesForStream(3)) == 0)) {
                Log.i(TAG, String.format(" onAccessoryPlugMediaUnmute unmuting device=%d [%s]", new Object[]{Integer.valueOf(newDevice), AudioSystem.getOutputDeviceName(newDevice)}));
                this.mStreamStates[3].mute(false);
            }
        }
    }

    private void setDeviceVolume(VolumeStreamState streamState, int device) {
        synchronized (VolumeStreamState.class) {
            streamState.applyDeviceVolume_syncVSS(device);
            int streamType = AudioSystem.getNumStreamTypes() - 1;
            while (streamType >= 0) {
                if (streamType != streamState.mStreamType && mStreamVolumeAlias[streamType] == streamState.mStreamType) {
                    int streamDevice = getDeviceForStream(streamType);
                    if (!(device == streamDevice || !this.mAvrcpAbsVolSupported || (device & 896) == 0)) {
                        this.mStreamStates[streamType].applyDeviceVolume_syncVSS(device);
                    }
                    this.mStreamStates[streamType].applyDeviceVolume_syncVSS(streamDevice);
                }
                streamType--;
            }
        }
        sendMsg(this.mAudioHandler, 1, 2, device, 0, streamState, 500);
    }

    private void makeA2dpDeviceAvailable(String address, String name, String eventSource) {
        VolumeStreamState streamState = this.mStreamStates[3];
        setBluetoothA2dpOnInt(true, eventSource);
        AudioSystem.setDeviceConnectionState(128, 1, address, name);
        AudioSystem.setParameters("A2dpSuspended=false");
        this.mConnectedDevices.put(makeDeviceListKey(128, address), new DeviceListSpec(128, name, address));
        sendMsg(this.mAudioHandler, MSG_ACCESSORY_PLUG_MEDIA_UNMUTE, 2, 128, 0, null, 0);
    }

    private void onSendBecomingNoisyIntent() {
        Log.d(TAG, "send AUDIO_BECOMING_NOISY");
        sendBroadcastToAll(new Intent("android.media.AUDIO_BECOMING_NOISY"));
    }

    private void makeA2dpDeviceUnavailableNow(String address) {
        if (address != null) {
            synchronized (this.mA2dpAvrcpLock) {
                this.mAvrcpAbsVolSupported = false;
            }
            AudioSystem.setDeviceConnectionState(128, 0, address, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            this.mConnectedDevices.remove(makeDeviceListKey(128, address));
            setCurrentAudioRouteName(null);
            if (this.mDockAddress == address) {
                this.mDockAddress = null;
            }
        }
    }

    private void makeA2dpDeviceUnavailableLater(String address, int delayMs) {
        AudioSystem.setParameters("A2dpSuspended=true");
        this.mConnectedDevices.remove(makeDeviceListKey(128, address));
        queueMsgUnderWakeLock(this.mAudioHandler, 106, 0, 0, address, delayMs);
    }

    private void makeA2dpSrcAvailable(String address) {
        AudioSystem.setDeviceConnectionState(-2147352576, 1, address, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        this.mConnectedDevices.put(makeDeviceListKey(-2147352576, address), new DeviceListSpec(-2147352576, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, address));
    }

    private void makeA2dpSrcUnavailable(String address) {
        AudioSystem.setDeviceConnectionState(-2147352576, 0, address, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        this.mConnectedDevices.remove(makeDeviceListKey(-2147352576, address));
    }

    private void setHearingAidVolume(int index, int streamType) {
        synchronized (this.mHearingAidLock) {
            if (this.mHearingAid != null) {
                int gainDB = (int) AudioSystem.getStreamVolumeDB(streamType, index / 10, 134217728);
                if (gainDB < -128) {
                    gainDB = -128;
                }
                this.mHearingAid.setVolume(gainDB);
            }
        }
    }

    private void makeHearingAidDeviceAvailable(String address, String name, String eventSource) {
        setHearingAidVolume(this.mStreamStates[3].getIndex(134217728), 3);
        AudioSystem.setDeviceConnectionState(134217728, 1, address, name);
        this.mConnectedDevices.put(makeDeviceListKey(134217728, address), new DeviceListSpec(134217728, name, address));
        sendMsg(this.mAudioHandler, MSG_ACCESSORY_PLUG_MEDIA_UNMUTE, 2, 134217728, 0, null, 0);
    }

    private void makeHearingAidDeviceUnavailable(String address) {
        AudioSystem.setDeviceConnectionState(134217728, 0, address, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        this.mConnectedDevices.remove(makeDeviceListKey(134217728, address));
        setCurrentAudioRouteName(null);
    }

    private void cancelA2dpDeviceTimeout() {
        this.mAudioHandler.removeMessages(106);
    }

    private boolean hasScheduledA2dpDockTimeout() {
        return this.mAudioHandler.hasMessages(106);
    }

    private void onSetA2dpSinkConnectionState(BluetoothDevice btDevice, int state, int a2dpVolume) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onSetA2dpSinkConnectionState btDevice= ");
        stringBuilder.append(btDevice);
        stringBuilder.append(" state= ");
        stringBuilder.append(state);
        stringBuilder.append(" is dock: ");
        stringBuilder.append(btDevice.isBluetoothDock());
        Log.d(str, stringBuilder.toString());
        if (btDevice != null) {
            str = btDevice.getAddress();
            if (!BluetoothAdapter.checkBluetoothAddress(str)) {
                str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            }
            synchronized (this.mConnectedDevices) {
                boolean isConnected = ((DeviceListSpec) this.mConnectedDevices.get(makeDeviceListKey(128, btDevice.getAddress()))) != null;
                if (isConnected && state != 2) {
                    if (!btDevice.isBluetoothDock()) {
                        makeA2dpDeviceUnavailableNow(str);
                    } else if (state == 0) {
                        makeA2dpDeviceUnavailableLater(str, 8000);
                    }
                    setCurrentAudioRouteName(null);
                } else if (!isConnected && state == 2) {
                    if (btDevice.isBluetoothDock()) {
                        cancelA2dpDeviceTimeout();
                        this.mDockAddress = str;
                    } else if (hasScheduledA2dpDockTimeout() && this.mDockAddress != null) {
                        cancelA2dpDeviceTimeout();
                        makeA2dpDeviceUnavailableNow(this.mDockAddress);
                    }
                    if (a2dpVolume != -1) {
                        VolumeStreamState streamState = this.mStreamStates[3];
                        streamState.setIndex(a2dpVolume * 10, 128, "onSetA2dpSinkConnectionState");
                        setDeviceVolume(streamState, 128);
                    }
                    makeA2dpDeviceAvailable(str, btDevice.getName(), "onSetA2dpSinkConnectionState");
                    setCurrentAudioRouteName(btDevice.getAliasName());
                }
            }
        }
    }

    private void onSetA2dpSourceConnectionState(BluetoothDevice btDevice, int state) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onSetA2dpSourceConnectionState btDevice=");
        stringBuilder.append(btDevice);
        stringBuilder.append(" state=");
        stringBuilder.append(state);
        Log.d(str, stringBuilder.toString());
        if (btDevice != null) {
            str = btDevice.getAddress();
            if (!BluetoothAdapter.checkBluetoothAddress(str)) {
                str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            }
            synchronized (this.mConnectedDevices) {
                boolean isConnected = ((DeviceListSpec) this.mConnectedDevices.get(makeDeviceListKey(-2147352576, str))) != null;
                if (isConnected && state != 2) {
                    makeA2dpSrcUnavailable(str);
                } else if (!isConnected && state == 2) {
                    makeA2dpSrcAvailable(str);
                }
            }
        }
    }

    private void onSetHearingAidConnectionState(BluetoothDevice btDevice, int state) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onSetHearingAidConnectionState btDevice=");
        stringBuilder.append(btDevice);
        stringBuilder.append(", state=");
        stringBuilder.append(state);
        Log.d(str, stringBuilder.toString());
        if (btDevice != null) {
            str = btDevice.getAddress();
            if (!BluetoothAdapter.checkBluetoothAddress(str)) {
                str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            }
            synchronized (this.mConnectedDevices) {
                boolean isConnected = ((DeviceListSpec) this.mConnectedDevices.get(makeDeviceListKey(134217728, btDevice.getAddress()))) != null;
                if (isConnected && state != 2) {
                    makeHearingAidDeviceUnavailable(str);
                    setCurrentAudioRouteName(null);
                } else if (!isConnected && state == 2) {
                    makeHearingAidDeviceAvailable(str, btDevice.getName(), "onSetHearingAidConnectionState");
                    setCurrentAudioRouteName(btDevice.getAliasName());
                }
            }
        }
    }

    private void setCurrentAudioRouteName(String name) {
        synchronized (this.mCurAudioRoutes) {
            if (!TextUtils.equals(this.mCurAudioRoutes.bluetoothName, name)) {
                this.mCurAudioRoutes.bluetoothName = name;
                sendMsg(this.mAudioHandler, 12, 1, 0, 0, null, 0);
            }
        }
    }

    /* JADX WARNING: Missing block: B:19:0x005f, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void onBluetoothA2dpDeviceConfigChange(BluetoothDevice btDevice) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onBluetoothA2dpDeviceConfigChange btDevice=");
        stringBuilder.append(btDevice);
        Log.d(str, stringBuilder.toString());
        if (btDevice != null) {
            str = btDevice.getAddress();
            if (!BluetoothAdapter.checkBluetoothAddress(str)) {
                str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            }
            synchronized (this.mConnectedDevices) {
                if (this.mAudioHandler.hasMessages(102, btDevice)) {
                    return;
                }
                if (((DeviceListSpec) this.mConnectedDevices.get(makeDeviceListKey(128, str))) != null) {
                    int musicDevice = getDeviceForStream(3);
                    if (AudioSystem.handleDeviceConfigChange(128, str, btDevice.getName()) != 0) {
                        setBluetoothA2dpDeviceConnectionStateInt(btDevice, 0, 2, false, musicDevice, -1);
                    }
                }
            }
        }
    }

    public void avrcpSupportsAbsoluteVolume(String address, boolean support) {
        synchronized (this.mA2dpAvrcpLock) {
            this.mAvrcpAbsVolSupported = support;
            sendMsg(this.mAudioHandler, 0, 2, 128, 0, this.mStreamStates[3], 0);
        }
    }

    /* JADX WARNING: Missing block: B:51:0x018c, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean handleDeviceConnection(boolean connect, int device, String address, String deviceName) {
        boolean z = connect;
        int i = device;
        String str = address;
        String str2 = deviceName;
        String str3 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleDeviceConnection(");
        stringBuilder.append(z);
        stringBuilder.append(" dev:");
        stringBuilder.append(Integer.toHexString(device));
        stringBuilder.append(" address:");
        stringBuilder.append(str);
        stringBuilder.append(" name:");
        stringBuilder.append(str2);
        stringBuilder.append(")");
        Slog.i(str3, stringBuilder.toString());
        synchronized (this.mConnectedDevices) {
            str3 = makeDeviceListKey(i, str);
            String str4 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("deviceKey:");
            stringBuilder2.append(str3);
            Slog.i(str4, stringBuilder2.toString());
            DeviceListSpec deviceSpec = (DeviceListSpec) this.mConnectedDevices.get(str3);
            boolean isConnected = deviceSpec != null;
            String str5 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("deviceSpec:");
            stringBuilder3.append(deviceSpec);
            stringBuilder3.append(" is(already)Connected:");
            stringBuilder3.append(isConnected);
            Slog.i(str5, stringBuilder3.toString());
            if (!z || isConnected) {
                boolean isConnected2 = isConnected;
                if (!z && isConnected2) {
                    AudioSystem.setDeviceConnectionState(i, 0, str, str2);
                    this.mConnectedDevices.remove(str3);
                    if (LOUD_VOICE_MODE_SUPPORT) {
                        sendMsg(this.mAudioHandler, 10001, 0, 0, 0, null, 500);
                    }
                } else if (z && isConnected2) {
                    str4 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("the device:");
                    stringBuilder2.append(str2);
                    stringBuilder2.append(" has already beeen connected, ignore");
                    Slog.i(str4, stringBuilder2.toString());
                    return true;
                } else {
                    str4 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("handleDeviceConnection() failed, deviceKey=");
                    stringBuilder2.append(str3);
                    stringBuilder2.append(", deviceSpec=");
                    stringBuilder2.append(deviceSpec);
                    stringBuilder2.append(", connect=");
                    stringBuilder2.append(z);
                    Log.w(str4, stringBuilder2.toString());
                    return false;
                }
            }
            int res = AudioSystem.setDeviceConnectionState(i, 1, str, str2);
            if (res != 0) {
                str5 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("not connecting device 0x");
                stringBuilder3.append(Integer.toHexString(device));
                stringBuilder3.append(" due to command error ");
                stringBuilder3.append(res);
                Slog.e(str5, stringBuilder3.toString());
                return false;
            }
            String device_out_key;
            this.mConnectedDevices.put(str3, new DeviceListSpec(i, str2, str));
            if (4 == i) {
                device_out_key = makeDeviceListKey(8, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                if (this.mConnectedDevices.get(device_out_key) != null) {
                    AudioSystem.setDeviceConnectionState(8, 0, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    this.mConnectedDevices.remove(device_out_key);
                }
            }
            if (8 == i) {
                str5 = makeDeviceListKey(4, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                boolean isConnectedHeadPhone = this.mConnectedDevices.get(str5) != null;
                if (isConnectedHeadPhone) {
                    AudioSystem.setDeviceConnectionState(4, 0, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    this.mConnectedDevices.remove(str5);
                }
                device_out_key = makeDeviceListKey(-2147483632, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                if (this.mConnectedDevices.get(device_out_key) != null) {
                    AudioSystem.setDeviceConnectionState(true, null, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    this.mConnectedDevices.remove(device_out_key);
                }
            }
            if (LOUD_VOICE_MODE_SUPPORT) {
                sendMsg(this.mAudioHandler, 10001, 0, 0, 0, null, 500);
            }
            sendMsg(this.mAudioHandler, MSG_ACCESSORY_PLUG_MEDIA_UNMUTE, 2, i, 0, null, false);
            return true;
        }
    }

    private int checkSendBecomingNoisyIntent(int device, int state, int musicDevice) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("checkSendBecomingNoisyIntent device:");
        stringBuilder.append(device);
        stringBuilder.append(" state:");
        stringBuilder.append(state);
        stringBuilder.append(" musicDevice:");
        stringBuilder.append(musicDevice);
        Log.d(str, stringBuilder.toString());
        if (state != 0 || (this.mBecomingNoisyIntentDevices & device) == 0) {
            return 0;
        }
        int i;
        int dev;
        int devices = 0;
        for (i = 0; i < this.mConnectedDevices.size(); i++) {
            dev = ((DeviceListSpec) this.mConnectedDevices.valueAt(i)).mDeviceType;
            if ((Integer.MIN_VALUE & dev) == 0 && (this.mBecomingNoisyIntentDevices & dev) != 0) {
                devices |= dev;
            }
        }
        if (musicDevice == 0) {
            musicDevice = getDeviceForStream(3);
            if ((536870912 & musicDevice) != 0) {
                i = -536870913 & musicDevice;
                dev = 16384;
                if (device != 16384) {
                    dev = 67108864;
                }
                musicDevice = i | dev;
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("newDevice: ");
                stringBuilder2.append(Integer.toHexString(musicDevice));
                Log.i(str2, stringBuilder2.toString());
            }
        }
        if ((device != musicDevice && !isInCommunication()) || device != devices || hasMediaDynamicPolicy()) {
            return 0;
        }
        this.mAudioHandler.removeMessages(15);
        sendMsg(this.mAudioHandler, 15, 0, 0, 0, null, 0);
        return 1000;
    }

    private boolean hasMediaDynamicPolicy() {
        synchronized (this.mAudioPolicies) {
            if (this.mAudioPolicies.isEmpty()) {
                return false;
            }
            for (AudioPolicyProxy app : this.mAudioPolicies.values()) {
                if (app.hasMixAffectingUsage(1)) {
                    return true;
                }
            }
            return false;
        }
    }

    private void updateAudioRoutes(int device, int state) {
        int connType = 0;
        if (device == 4) {
            connType = 1;
        } else if (device == 8 || device == 131072) {
            connType = 2;
        } else if (device == 1024 || device == 262144) {
            connType = 8;
        } else if (device == 67108864) {
            connType = 16;
        }
        synchronized (this.mCurAudioRoutes) {
            if (connType != 0) {
                int newConn = this.mCurAudioRoutes.mainType;
                if (state != 0) {
                    newConn |= connType;
                } else {
                    newConn &= ~connType;
                }
                if (newConn != this.mCurAudioRoutes.mainType) {
                    this.mCurAudioRoutes.mainType = newConn;
                    sendMsg(this.mAudioHandler, 12, 1, 0, 0, null, 0);
                }
            }
        }
    }

    private void sendDeviceConnectionIntent(int device, int state, String address, String deviceName) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendDeviceConnectionIntent(dev:0x");
        stringBuilder.append(Integer.toHexString(device));
        stringBuilder.append(" state:0x");
        stringBuilder.append(Integer.toHexString(state));
        stringBuilder.append(" address:");
        stringBuilder.append(address);
        stringBuilder.append(" name:");
        stringBuilder.append(deviceName);
        stringBuilder.append(");");
        Slog.i(str, stringBuilder.toString());
        Intent intent = new Intent();
        if (device == 4) {
            intent.setAction("android.intent.action.HEADSET_PLUG");
            intent.putExtra("microphone", 1);
            appendExtraInfo(intent);
        } else if (device == 8 || device == 131072) {
            intent.setAction("android.intent.action.HEADSET_PLUG");
            intent.putExtra("microphone", 0);
            appendExtraInfo(intent);
        } else if (device == 67108864) {
            intent.setAction("android.intent.action.HEADSET_PLUG");
            if (isConnectedHeadSet()) {
                intent.putExtra("microphone", 1);
            } else if (isConnectedHeadPhone()) {
                intent.putExtra("microphone", 0);
            } else if (isConnectedUsbInDevice()) {
                intent.putExtra("microphone", 1);
            } else {
                intent.putExtra("microphone", 0);
            }
        } else if (device == 1024 || device == 262144) {
            configureHdmiPlugIntent(intent, state);
        }
        if (intent.getAction() != null) {
            intent.putExtra(CONNECT_INTENT_KEY_STATE, state);
            intent.putExtra(CONNECT_INTENT_KEY_ADDRESS, address);
            intent.putExtra(CONNECT_INTENT_KEY_PORT_NAME, deviceName);
            intent.addFlags(1073741824);
            long ident = Binder.clearCallingIdentity();
            try {
                ActivityManager.broadcastStickyIntent(intent, -1);
                if (state == 0) {
                    this.mHwAudioServiceEx.updateMicIcon();
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private void onSetWiredDeviceConnectionState(int device, int state, String address, String deviceName, String caller) {
        int i = device;
        int i2 = state;
        String str = address;
        String str2 = deviceName;
        String str3 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onSetWiredDeviceConnectionState(dev:");
        stringBuilder.append(Integer.toHexString(device));
        stringBuilder.append(" state:");
        stringBuilder.append(Integer.toHexString(state));
        stringBuilder.append(" address:");
        stringBuilder.append(str);
        stringBuilder.append(" deviceName:");
        stringBuilder.append(str2);
        stringBuilder.append(" caller: ");
        String str4 = caller;
        stringBuilder.append(str4);
        stringBuilder.append(");");
        Slog.i(str3, stringBuilder.toString());
        synchronized (this.mConnectedDevices) {
            if (i2 == 0 && (i & DEVICE_OVERRIDE_A2DP_ROUTE_ON_PLUG) != 0) {
                setBluetoothA2dpOnInt(true, "onSetWiredDeviceConnectionState state 0");
            }
            if (handleDeviceConnection(i2 == 1, i, str, str2)) {
                boolean z;
                if (i2 != 0) {
                    if ((DEVICE_OVERRIDE_A2DP_ROUTE_ON_PLUG & i) != 0) {
                        setBluetoothA2dpOnInt(false, "onSetWiredDeviceConnectionState state not 0");
                    }
                    if ((i & 603979788) != 0) {
                        z = false;
                        sendMsg(this.mAudioHandler, 14, 0, 0, 0, str4, MUSIC_ACTIVE_POLL_PERIOD_MS);
                    } else {
                        z = false;
                    }
                    if (isPlatformTelevision() && (i & 1024) != 0) {
                        this.mFixedVolumeDevices |= 1024;
                        checkAllFixedVolumeDevices();
                        if (this.mHdmiManager != null) {
                            synchronized (this.mHdmiManager) {
                                if (this.mHdmiPlaybackClient != null) {
                                    this.mHdmiCecSink = z;
                                    this.mHdmiPlaybackClient.queryDisplayStatus(this.mHdmiDisplayStatusCallback);
                                }
                            }
                        }
                    }
                    if ((i & 1024) != 0) {
                        sendEnabledSurroundFormats(this.mContentResolver, true);
                    }
                } else {
                    z = false;
                    if (!(!isPlatformTelevision() || (i & 1024) == 0 || this.mHdmiManager == null)) {
                        synchronized (this.mHdmiManager) {
                            this.mHdmiCecSink = z;
                        }
                    }
                    if (!(this.mIsChineseZone || (i & 603979788) == 0 || !this.mHasAlarm)) {
                        this.mAlarmManager.cancel(this.mPendingIntent);
                        this.mHasAlarm = z;
                    }
                }
                sendDeviceConnectionIntent(device, state, address, deviceName);
                updateAudioRoutes(device, state);
                return;
            }
        }
    }

    private void configureHdmiPlugIntent(Intent intent, int state) {
        Intent intent2 = intent;
        int i = state;
        intent2.setAction("android.media.action.HDMI_AUDIO_PLUG");
        intent2.putExtra("android.media.extra.AUDIO_PLUG_STATE", i);
        if (i == 1) {
            ArrayList<AudioPort> ports = new ArrayList();
            if (AudioSystem.listAudioPorts(ports, new int[1]) == 0) {
                Iterator it = ports.iterator();
                while (it.hasNext()) {
                    AudioPort port = (AudioPort) it.next();
                    if (port instanceof AudioDevicePort) {
                        AudioDevicePort devicePort = (AudioDevicePort) port;
                        if (devicePort.type() == 1024 || devicePort.type() == 262144) {
                            int i2;
                            int[] encodingArray;
                            int[] formats = AudioFormat.filterPublicFormats(devicePort.formats());
                            int i3 = 0;
                            if (formats.length > 0) {
                                ArrayList<Integer> encodingList = new ArrayList(1);
                                for (int format : formats) {
                                    if (format != 0) {
                                        encodingList.add(Integer.valueOf(format));
                                    }
                                }
                                encodingArray = new int[encodingList.size()];
                                for (i2 = 0; i2 < encodingArray.length; i2++) {
                                    encodingArray[i2] = ((Integer) encodingList.get(i2)).intValue();
                                }
                                intent2.putExtra("android.media.extra.ENCODINGS", encodingArray);
                            }
                            int maxChannels = 0;
                            encodingArray = devicePort.channelMasks();
                            i2 = encodingArray.length;
                            while (i3 < i2) {
                                int channelCount = AudioFormat.channelCountFromOutChannelMask(encodingArray[i3]);
                                if (channelCount > maxChannels) {
                                    maxChannels = channelCount;
                                }
                                i3++;
                            }
                            intent2.putExtra("android.media.extra.MAX_CHANNEL_COUNT", maxChannels);
                        }
                    }
                }
            }
        }
    }

    private void setCheckMusicActiveAlarm() {
        this.mAlarmManager.cancel(this.mPendingIntent);
        this.mAlarmManager.setExact(0, Calendar.getInstance().getTimeInMillis() + 60000, this.mPendingIntent);
        this.mHasAlarm = true;
    }

    private boolean isA2dpDeviceConnected() {
        synchronized (this.mConnectedDevices) {
            synchronized (this.mA2dpAvrcpLock) {
                for (int i = 0; i < this.mConnectedDevices.size(); i++) {
                    if (((DeviceListSpec) this.mConnectedDevices.valueAt(i)).mDeviceType == 128) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    private void handleAudioEffectBroadcast(Context context, Intent intent) {
        String target = intent.getPackage();
        if (target != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("effect broadcast already targeted to ");
            stringBuilder.append(target);
            Log.w(str, stringBuilder.toString());
            return;
        }
        intent.addFlags(32);
        List<ResolveInfo> ril = context.getPackageManager().queryBroadcastReceivers(intent, 0);
        if (!(ril == null || ril.size() == 0)) {
            ResolveInfo ri = (ResolveInfo) ril.get(0);
            if (!(ri == null || ri.activityInfo == null || ri.activityInfo.packageName == null)) {
                intent.setPackage(ri.activityInfo.packageName);
                context.sendBroadcastAsUser(intent, UserHandle.ALL);
                return;
            }
        }
        Log.w(TAG, "couldn't find receiver package for effect intent");
    }

    private void killBackgroundUserProcessesWithRecordAudioPermission(UserInfo oldUser) {
        PackageManager pm = this.mContext.getPackageManager();
        ComponentName homeActivityName = null;
        if (!oldUser.isManagedProfile()) {
            homeActivityName = this.mActivityManagerInternal.getHomeActivityForUser(oldUser.id);
        }
        try {
            List<PackageInfo> packages = AppGlobals.getPackageManager().getPackagesHoldingPermissions(new String[]{"android.permission.RECORD_AUDIO"}, 0, oldUser.id).getList();
            for (int j = packages.size() - 1; j >= 0; j--) {
                PackageInfo pkg = (PackageInfo) packages.get(j);
                if (!(UserHandle.getAppId(pkg.applicationInfo.uid) < 10000 || pm.checkPermission("android.permission.INTERACT_ACROSS_USERS", pkg.packageName) == 0 || (homeActivityName != null && pkg.packageName.equals(homeActivityName.getPackageName()) && pkg.applicationInfo.isSystemApp()))) {
                    try {
                        int uid = pkg.applicationInfo.uid;
                        ActivityManager.getService().killUid(UserHandle.getAppId(uid), UserHandle.getUserId(uid), "killBackgroundUserProcessesWithAudioRecordPermission");
                    } catch (RemoteException e) {
                        Log.w(TAG, "Error calling killUid", e);
                    }
                }
            }
        } catch (RemoteException e2) {
            throw new AndroidRuntimeException(e2);
        }
    }

    /* JADX WARNING: Missing block: B:27:0x003f, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean forceFocusDuckingForAccessibility(AudioAttributes aa, int request, int uid) {
        if (aa == null || aa.getUsage() != 11 || request != 3) {
            return false;
        }
        Bundle extraInfo = aa.getBundle();
        if (extraInfo == null || !extraInfo.getBoolean("a11y_force_ducking")) {
            return false;
        }
        if (uid == 0) {
            return true;
        }
        synchronized (this.mAccessibilityServiceUidsLock) {
            if (this.mAccessibilityServiceUids != null) {
                int callingUid = Binder.getCallingUid();
                for (int i : this.mAccessibilityServiceUids) {
                    if (i == callingUid) {
                        return true;
                    }
                }
            }
        }
    }

    public int requestAudioFocus(AudioAttributes aa, int durationHint, IBinder cb, IAudioFocusDispatcher fd, String clientId, String callingPackageName, int flags, IAudioPolicyCallback pcb, int sdk) {
        String str;
        sendBehavior(BehaviorId.AUDIO_REQUESTAUDIOFOCUS, new Object[0]);
        if ((flags & 4) == 4) {
            str = clientId;
            if (!"AudioFocus_For_Phone_Ring_And_Calls".equals(str)) {
                synchronized (this.mAudioPolicies) {
                    if (!this.mAudioPolicies.containsKey(pcb.asBinder())) {
                        Log.e(TAG, "Invalid unregistered AudioPolicy to (un)lock audio focus");
                        return 0;
                    }
                }
            } else if (this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") != 0) {
                Log.e(TAG, "Invalid permission to (un)lock audio focus", new Exception());
                return 0;
            }
        }
        str = clientId;
        AudioAttributes audioAttributes = aa;
        int i = durationHint;
        return this.mMediaFocusControl.requestAudioFocus(audioAttributes, i, cb, fd, str, callingPackageName, flags, sdk, forceFocusDuckingForAccessibility(audioAttributes, i, Binder.getCallingUid()));
    }

    public int abandonAudioFocus(IAudioFocusDispatcher fd, String clientId, AudioAttributes aa, String callingPackageName) {
        return this.mMediaFocusControl.abandonAudioFocus(fd, clientId, aa, callingPackageName);
    }

    public void unregisterAudioFocusClient(String clientId) {
        this.mMediaFocusControl.unregisterAudioFocusClient(clientId);
    }

    public int getCurrentAudioFocus() {
        return this.mMediaFocusControl.getCurrentAudioFocus();
    }

    public int getFocusRampTimeMs(int focusGain, AudioAttributes attr) {
        MediaFocusControl mediaFocusControl = this.mMediaFocusControl;
        return MediaFocusControl.getFocusRampTimeMs(focusGain, attr);
    }

    private boolean readCameraSoundForced() {
        if (SystemProperties.getBoolean("audio.camerasound.force", false) || this.mContext.getResources().getBoolean(17956909)) {
            return true;
        }
        return false;
    }

    private void handleConfigurationChanged(Context context) {
        try {
            Configuration config = context.getResources().getConfiguration();
            sendMsg(this.mAudioHandler, 16, 0, 0, 0, TAG, 0);
            boolean cameraSoundForced = readCameraSoundForced();
            synchronized (this.mSettingsLock) {
                boolean z = false;
                boolean cameraSoundForcedChanged = cameraSoundForced != this.mCameraSoundForced;
                this.mCameraSoundForced = cameraSoundForced;
                if (cameraSoundForcedChanged) {
                    if (!this.mIsSingleVolume) {
                        synchronized (VolumeStreamState.class) {
                            VolumeStreamState s = this.mStreamStates[7];
                            if (cameraSoundForced) {
                                s.setAllIndexesToMax();
                                this.mRingerModeAffectedStreams &= -129;
                            } else {
                                s.setAllIndexes(this.mStreamStates[1], TAG);
                                this.mRingerModeAffectedStreams |= 128;
                            }
                        }
                        setRingerModeInt(getRingerModeInternal(), false);
                    }
                    Handler handler = this.mAudioHandler;
                    if (cameraSoundForced) {
                        z = true;
                    }
                    sendMsg(handler, 8, 2, 4, z, new String("handleConfigurationChanged"), 0);
                    sendMsg(this.mAudioHandler, 10, 2, 0, 0, this.mStreamStates[7], 0);
                }
            }
            this.mVolumeController.setLayoutDirection(config.getLayoutDirection());
        } catch (Exception e) {
            Log.e(TAG, "Error handling configuration change: ", e);
        }
    }

    public void setBluetoothA2dpOnInt(boolean on, String eventSource) {
        synchronized (this.mBluetoothA2dpEnabledLock) {
            this.mBluetoothA2dpEnabled = on;
            this.mAudioHandler.removeMessages(13);
            setForceUseInt_SyncDevices(1, this.mBluetoothA2dpEnabled ? 0 : 10, eventSource);
        }
    }

    private void setForceUseInt_SyncDevices(int usage, int config, String eventSource) {
        if (usage == 1) {
            sendMsg(this.mAudioHandler, 12, 1, 0, 0, null, 0);
        }
        this.mForceUseLogger.log(new ForceUseEvent(usage, config, eventSource));
        AudioSystem.setForceUse(usage, config);
        updateAftPolicy();
    }

    public void setRingtonePlayer(IRingtonePlayer player) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.REMOTE_AUDIO_PLAYBACK", null);
        this.mRingtonePlayer = player;
    }

    public IRingtonePlayer getRingtonePlayer() {
        return this.mRingtonePlayer;
    }

    public AudioRoutesInfo startWatchingRoutes(IAudioRoutesObserver observer) {
        AudioRoutesInfo routes;
        synchronized (this.mCurAudioRoutes) {
            routes = new AudioRoutesInfo(this.mCurAudioRoutes);
            this.mRoutesObservers.register(observer);
        }
        return routes;
    }

    private int safeMediaVolumeIndex(int device) {
        if ((603979788 & device) == 0) {
            return MAX_STREAM_VOLUME[3];
        }
        if (device == 67108864) {
            return this.mSafeUsbMediaVolumeIndex;
        }
        return this.mSafeMediaVolumeIndex;
    }

    private void setSafeMediaVolumeEnabled(boolean on, String caller) {
        synchronized (this.mSafeMediaVolumeState) {
            if (!(this.mSafeMediaVolumeState.intValue() == 0 || this.mSafeMediaVolumeState.intValue() == 1)) {
                if (on && this.mSafeMediaVolumeState.intValue() == 2) {
                    this.mSafeMediaVolumeState = Integer.valueOf(3);
                    enforceSafeMediaVolume(caller);
                } else if (!on && this.mSafeMediaVolumeState.intValue() == 3) {
                    this.mSafeMediaVolumeState = Integer.valueOf(2);
                    this.mMusicActiveMs = 1;
                    saveMusicActiveMs();
                    sendMsg(this.mAudioHandler, 14, 0, 0, 0, caller, MUSIC_ACTIVE_POLL_PERIOD_MS);
                }
            }
        }
    }

    private void enforceSafeMediaVolume(String caller) {
        VolumeStreamState streamState = this.mStreamStates[3];
        int devices = 603979788;
        int i = 0;
        while (devices != 0) {
            int i2 = i + 1;
            int device = 1 << i;
            if ((device & devices) != 0) {
                if (streamState.getIndex(device) > safeMediaVolumeIndex(device)) {
                    streamState.setIndex(safeMediaVolumeIndex(device), device, caller);
                    sendMsg(this.mAudioHandler, 0, 2, device, 0, streamState, 0);
                }
                devices &= ~device;
            }
            i = i2;
        }
    }

    private boolean checkSafeMediaVolume(int streamType, int index, int device) {
        synchronized (this.mSafeMediaVolumeState) {
            if (this.mFactoryMode || this.mSafeMediaVolumeState.intValue() != 3 || mStreamVolumeAlias[streamType] != 3 || (603979788 & device) == 0 || index <= safeMediaVolumeIndex(device)) {
                return true;
            }
            return false;
        }
    }

    public void disableSafeMediaVolume(String callingPackage) {
        enforceVolumeController("disable the safe media volume");
        synchronized (this.mSafeMediaVolumeState) {
            setSafeMediaVolumeEnabled(false, callingPackage);
            if (this.mPendingVolumeCommand != null) {
                onSetStreamVolume(this.mPendingVolumeCommand.mStreamType, this.mPendingVolumeCommand.mIndex, this.mPendingVolumeCommand.mFlags, this.mPendingVolumeCommand.mDevice, callingPackage);
                this.mPendingVolumeCommand = null;
            }
        }
    }

    public int setHdmiSystemAudioSupported(boolean on) {
        int device = 0;
        if (this.mHdmiManager != null) {
            synchronized (this.mHdmiManager) {
                if (this.mHdmiTvClient == null) {
                    Log.w(TAG, "Only Hdmi-Cec enabled TV device supports system audio mode.");
                    return 0;
                }
                synchronized (this.mHdmiTvClient) {
                    if (this.mHdmiSystemAudioSupported != on) {
                        int config;
                        this.mHdmiSystemAudioSupported = on;
                        if (on) {
                            config = 12;
                        } else {
                            config = 0;
                        }
                        this.mForceUseLogger.log(new ForceUseEvent(5, config, "setHdmiSystemAudioSupported"));
                        AudioSystem.setForceUse(5, config);
                    }
                    device = getDevicesForStream(3);
                }
            }
        }
        return device;
    }

    public boolean isHdmiSystemAudioSupported() {
        return this.mHdmiSystemAudioSupported;
    }

    private void initA11yMonitoring() {
        AccessibilityManager accessibilityManager = (AccessibilityManager) this.mContext.getSystemService("accessibility");
        updateDefaultStreamOverrideDelay(accessibilityManager.isTouchExplorationEnabled());
        updateA11yVolumeAlias(accessibilityManager.isAccessibilityVolumeStreamActive());
        accessibilityManager.addTouchExplorationStateChangeListener(this, null);
        accessibilityManager.addAccessibilityServicesStateChangeListener(this, null);
    }

    public void onTouchExplorationStateChanged(boolean enabled) {
        updateDefaultStreamOverrideDelay(enabled);
    }

    private void updateDefaultStreamOverrideDelay(boolean touchExploreEnabled) {
        if (touchExploreEnabled) {
            sStreamOverrideDelayMs = 1000;
        } else {
            sStreamOverrideDelayMs = 0;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Touch exploration enabled=");
        stringBuilder.append(touchExploreEnabled);
        stringBuilder.append(" stream override delay is now ");
        stringBuilder.append(sStreamOverrideDelayMs);
        stringBuilder.append(" ms");
        Log.d(str, stringBuilder.toString());
    }

    public void onAccessibilityServicesStateChanged(AccessibilityManager accessibilityManager) {
        updateA11yVolumeAlias(accessibilityManager.isAccessibilityVolumeStreamActive());
    }

    private void updateA11yVolumeAlias(boolean a11VolEnabled) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Accessibility volume enabled = ");
        stringBuilder.append(a11VolEnabled);
        Log.d(str, stringBuilder.toString());
        if (sIndependentA11yVolume != a11VolEnabled) {
            sIndependentA11yVolume = a11VolEnabled;
            int i = 1;
            updateStreamVolumeAlias(true, TAG);
            VolumeController volumeController = this.mVolumeController;
            if (!sIndependentA11yVolume) {
                i = 0;
            }
            volumeController.setA11yMode(i);
            this.mVolumeController.postVolumeChanged(10, 0);
        }
    }

    public boolean isCameraSoundForced() {
        boolean z;
        synchronized (this.mSettingsLock) {
            z = this.mCameraSoundForced;
        }
        return z;
    }

    private void dumpRingerMode(PrintWriter pw) {
        pw.println("\nRinger mode: ");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("- mode (internal) = ");
        stringBuilder.append(RINGER_MODE_NAMES[this.mRingerMode]);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("- mode (external) = ");
        stringBuilder.append(RINGER_MODE_NAMES[this.mRingerModeExternal]);
        pw.println(stringBuilder.toString());
        dumpRingerModeStreams(pw, "affected", this.mRingerModeAffectedStreams);
        dumpRingerModeStreams(pw, "muted", this.mRingerAndZenModeMutedStreams);
        pw.print("- delegate = ");
        pw.println(this.mRingerModeDelegate);
    }

    private void dumpRingerModeStreams(PrintWriter pw, String type, int streams) {
        pw.print("- ringer mode ");
        pw.print(type);
        pw.print(" streams = 0x");
        pw.print(Integer.toHexString(streams));
        if (streams != 0) {
            pw.print(" (");
            boolean first = true;
            for (int i = 0; i < AudioSystem.STREAM_NAMES.length; i++) {
                int stream = 1 << i;
                if ((streams & stream) != 0) {
                    if (!first) {
                        pw.print(',');
                    }
                    pw.print(AudioSystem.STREAM_NAMES[i]);
                    streams &= ~stream;
                    first = false;
                }
            }
            if (streams != 0) {
                if (!first) {
                    pw.print(',');
                }
                pw.print(streams);
            }
            pw.print(')');
        }
        pw.println();
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            this.mMediaFocusControl.dump(pw);
            dumpStreamStates(pw);
            dumpRingerMode(pw);
            pw.println("\nAudio routes:");
            pw.print("  mMainType=0x");
            pw.println(Integer.toHexString(this.mCurAudioRoutes.mainType));
            pw.print("  mBluetoothName=");
            pw.println(this.mCurAudioRoutes.bluetoothName);
            pw.println("\nOther state:");
            pw.print("  mVolumeController=");
            pw.println(this.mVolumeController);
            pw.print("  mSafeMediaVolumeState=");
            pw.println(safeMediaVolumeStateToString(this.mSafeMediaVolumeState));
            pw.print("  mSafeMediaVolumeIndex=");
            pw.println(this.mSafeMediaVolumeIndex);
            pw.print("  mSafeUsbMediaVolumeIndex=");
            pw.println(this.mSafeUsbMediaVolumeIndex);
            pw.print("  mSafeUsbMediaVolumeDbfs=");
            pw.println(this.mSafeUsbMediaVolumeDbfs);
            pw.print("  sIndependentA11yVolume=");
            pw.println(sIndependentA11yVolume);
            pw.print("  mPendingVolumeCommand=");
            pw.println(this.mPendingVolumeCommand);
            pw.print("  mMusicActiveMs=");
            pw.println(this.mMusicActiveMs);
            pw.print("  UNSAFE_VOLUME_MUSIC_ACTIVE_MS_MAX=");
            pw.println(UNSAFE_VOLUME_MUSIC_ACTIVE_MS_MAX);
            pw.print("  mMcc=");
            pw.println(this.mMcc);
            pw.print("  mCameraSoundForced=");
            pw.println(this.mCameraSoundForced);
            pw.print("  mHasVibrator=");
            pw.println(this.mHasVibrator);
            pw.print("  mVolumePolicy=");
            pw.println(this.mVolumePolicy);
            pw.print("  mAvrcpAbsVolSupported=");
            pw.println(this.mAvrcpAbsVolSupported);
            dumpAudioPolicies(pw);
            this.mDynPolicyLogger.dump(pw);
            this.mPlaybackMonitor.dump(pw);
            this.mRecordMonitor.dump(pw);
            pw.println("\n");
            pw.println("\nEvent logs:");
            this.mModeLogger.dump(pw);
            pw.println("\n");
            this.mWiredDevLogger.dump(pw);
            pw.println("\n");
            this.mForceUseLogger.dump(pw);
            pw.println("\n");
            this.mVolumeLogger.dump(pw);
            dumpAudioMode(pw);
        }
    }

    private static String safeMediaVolumeStateToString(Integer state) {
        switch (state.intValue()) {
            case 0:
                return "SAFE_MEDIA_VOLUME_NOT_CONFIGURED";
            case 1:
                return "SAFE_MEDIA_VOLUME_DISABLED";
            case 2:
                return "SAFE_MEDIA_VOLUME_INACTIVE";
            case 3:
                return "SAFE_MEDIA_VOLUME_ACTIVE";
            default:
                return null;
        }
    }

    private static void readAndSetLowRamDevice() {
        boolean isLowRamDevice = ActivityManager.isLowRamDeviceStatic();
        long totalMemory = 1073741824;
        try {
            MemoryInfo info = new MemoryInfo();
            ActivityManager.getService().getMemoryInfo(info);
            totalMemory = info.totalMem;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot obtain MemoryInfo from ActivityManager, assume low memory device");
            isLowRamDevice = true;
        }
        int status = AudioSystem.setLowRamDevice(isLowRamDevice, totalMemory);
        if (status != 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AudioFlinger informed of device's low RAM attribute; status ");
            stringBuilder.append(status);
            Log.w(str, stringBuilder.toString());
        }
    }

    private void enforceVolumeController(String action) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Only SystemUI can ");
        stringBuilder.append(action);
        this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", stringBuilder.toString());
    }

    public void setVolumeController(final IVolumeController controller) {
        enforceVolumeController("set the volume controller");
        if (!this.mVolumeController.isSameBinder(controller)) {
            this.mVolumeController.postDismiss();
            if (controller != null) {
                try {
                    controller.asBinder().linkToDeath(new DeathRecipient() {
                        public void binderDied() {
                            if (AudioService.this.mVolumeController.isSameBinder(controller)) {
                                Log.w(AudioService.TAG, "Current remote volume controller died, unregistering");
                                AudioService.this.setVolumeController(null);
                            }
                        }
                    }, 0);
                } catch (RemoteException e) {
                }
            }
            this.mVolumeController.setController(controller);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Volume controller: ");
            stringBuilder.append(this.mVolumeController);
            Log.d(str, stringBuilder.toString());
        }
    }

    public void notifyVolumeControllerVisible(IVolumeController controller, boolean visible) {
        enforceVolumeController("notify about volume controller visibility");
        if (this.mVolumeController.isSameBinder(controller)) {
            this.mVolumeController.setVisible(visible);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Volume controller visible: ");
            stringBuilder.append(visible);
            Log.d(str, stringBuilder.toString());
        }
    }

    public void setVolumePolicy(VolumePolicy policy) {
        enforceVolumeController("set volume policy");
        if (policy != null && !policy.equals(this.mVolumePolicy)) {
            this.mVolumePolicy = policy;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Volume policy changed: ");
            stringBuilder.append(this.mVolumePolicy);
            Log.d(str, stringBuilder.toString());
        }
    }

    public String registerAudioPolicy(AudioPolicyConfig policyConfig, IAudioPolicyCallback pcb, boolean hasFocusListener, boolean isFocusPolicy, boolean isVolumeController) {
        Throwable th;
        AudioSystem.setDynamicPolicyCallback(this.mDynPolicyCallback);
        if (this.mContext.checkCallingPermission("android.permission.MODIFY_AUDIO_ROUTING") == 0) {
            AudioEventLogger audioEventLogger = this.mDynPolicyLogger;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("registerAudioPolicy for ");
            stringBuilder.append(pcb.asBinder());
            stringBuilder.append(" with config:");
            AudioPolicyConfig audioPolicyConfig = policyConfig;
            stringBuilder.append(audioPolicyConfig);
            audioEventLogger.log(new StringEvent(stringBuilder.toString()).printLog(TAG));
            synchronized (this.mAudioPolicies) {
                try {
                    if (this.mAudioPolicies.containsKey(pcb.asBinder())) {
                        Slog.e(TAG, "Cannot re-register policy");
                        return null;
                    }
                    AudioPolicyProxy audioPolicyProxy = new AudioPolicyProxy(audioPolicyConfig, pcb, hasFocusListener, isFocusPolicy, isVolumeController);
                    pcb.asBinder().linkToDeath(audioPolicyProxy, 0);
                    String regId = audioPolicyProxy.getRegistrationId();
                    this.mAudioPolicies.put(pcb.asBinder(), audioPolicyProxy);
                    return regId;
                } catch (RemoteException e) {
                    String str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Audio policy registration failed, could not link to ");
                    stringBuilder.append(pcb);
                    stringBuilder.append(" binder death");
                    Slog.w(str, stringBuilder.toString(), e);
                    return null;
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Can't register audio policy for pid ");
        stringBuilder2.append(Binder.getCallingPid());
        stringBuilder2.append(" / uid ");
        stringBuilder2.append(Binder.getCallingUid());
        stringBuilder2.append(", need MODIFY_AUDIO_ROUTING");
        Slog.w(str2, stringBuilder2.toString());
        return null;
    }

    public void unregisterAudioPolicyAsync(IAudioPolicyCallback pcb) {
        AudioEventLogger audioEventLogger = this.mDynPolicyLogger;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unregisterAudioPolicyAsync for ");
        stringBuilder.append(pcb.asBinder());
        audioEventLogger.log(new StringEvent(stringBuilder.toString()).printLog(TAG));
        synchronized (this.mAudioPolicies) {
            AudioPolicyProxy app = (AudioPolicyProxy) this.mAudioPolicies.remove(pcb.asBinder());
            if (app == null) {
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Trying to unregister unknown audio policy for pid ");
                stringBuilder2.append(Binder.getCallingPid());
                stringBuilder2.append(" / uid ");
                stringBuilder2.append(Binder.getCallingUid());
                Slog.w(str, stringBuilder2.toString());
                return;
            }
            pcb.asBinder().unlinkToDeath(app, 0);
            app.release();
        }
    }

    @GuardedBy("mAudioPolicies")
    private AudioPolicyProxy checkUpdateForPolicy(IAudioPolicyCallback pcb, String errorMsg) {
        if (this.mContext.checkCallingPermission("android.permission.MODIFY_AUDIO_ROUTING") == 0) {
            AudioPolicyProxy app = (AudioPolicyProxy) this.mAudioPolicies.get(pcb.asBinder());
            if (app != null) {
                return app;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(errorMsg);
            stringBuilder.append(" for pid ");
            stringBuilder.append(Binder.getCallingPid());
            stringBuilder.append(" / uid ");
            stringBuilder.append(Binder.getCallingUid());
            stringBuilder.append(", unregistered policy");
            Slog.w(str, stringBuilder.toString());
            return null;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(errorMsg);
        stringBuilder2.append(" for pid ");
        stringBuilder2.append(Binder.getCallingPid());
        stringBuilder2.append(" / uid ");
        stringBuilder2.append(Binder.getCallingUid());
        stringBuilder2.append(", need MODIFY_AUDIO_ROUTING");
        Slog.w(str2, stringBuilder2.toString());
        return null;
    }

    public int addMixForPolicy(AudioPolicyConfig policyConfig, IAudioPolicyCallback pcb) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("addMixForPolicy for ");
        stringBuilder.append(pcb.asBinder());
        stringBuilder.append(" with config:");
        stringBuilder.append(policyConfig);
        Log.d(str, stringBuilder.toString());
        synchronized (this.mAudioPolicies) {
            AudioPolicyProxy app = checkUpdateForPolicy(pcb, "Cannot add AudioMix in audio policy");
            if (app == null) {
                return -1;
            }
            app.addMixes(policyConfig.getMixes());
            return 0;
        }
    }

    public int removeMixForPolicy(AudioPolicyConfig policyConfig, IAudioPolicyCallback pcb) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("removeMixForPolicy for ");
        stringBuilder.append(pcb.asBinder());
        stringBuilder.append(" with config:");
        stringBuilder.append(policyConfig);
        Log.d(str, stringBuilder.toString());
        synchronized (this.mAudioPolicies) {
            AudioPolicyProxy app = checkUpdateForPolicy(pcb, "Cannot add AudioMix in audio policy");
            if (app == null) {
                return -1;
            }
            app.removeMixes(policyConfig.getMixes());
            return 0;
        }
    }

    public int setFocusPropertiesForPolicy(int duckingBehavior, IAudioPolicyCallback pcb) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setFocusPropertiesForPolicy() duck behavior=");
        stringBuilder.append(duckingBehavior);
        stringBuilder.append(" policy ");
        stringBuilder.append(pcb.asBinder());
        Log.d(str, stringBuilder.toString());
        synchronized (this.mAudioPolicies) {
            AudioPolicyProxy app = checkUpdateForPolicy(pcb, "Cannot change audio policy focus properties");
            if (app == null) {
                return -1;
            } else if (this.mAudioPolicies.containsKey(pcb.asBinder())) {
                boolean z = true;
                if (duckingBehavior == 1) {
                    for (AudioPolicyProxy policy : this.mAudioPolicies.values()) {
                        if (policy.mFocusDuckBehavior == 1) {
                            Slog.e(TAG, "Cannot change audio policy ducking behavior, already handled");
                            return -1;
                        }
                    }
                }
                app.mFocusDuckBehavior = duckingBehavior;
                MediaFocusControl mediaFocusControl = this.mMediaFocusControl;
                if (duckingBehavior != 1) {
                    z = false;
                }
                mediaFocusControl.setDuckingInExtPolicyAvailable(z);
                return 0;
            } else {
                Slog.e(TAG, "Cannot change audio policy focus properties, unregistered policy");
                return -1;
            }
        }
    }

    private void setExtVolumeController(IAudioPolicyCallback apc) {
        if (this.mContext.getResources().getBoolean(17956980)) {
            synchronized (this.mExtVolumeControllerLock) {
                if (!(this.mExtVolumeController == null || this.mExtVolumeController.asBinder().pingBinder())) {
                    Log.e(TAG, "Cannot set external volume controller: existing controller");
                }
                this.mExtVolumeController = apc;
            }
            return;
        }
        Log.e(TAG, "Cannot set external volume controller: device not set for volume keys handled in PhoneWindowManager");
    }

    private void dumpAudioPolicies(PrintWriter pw) {
        pw.println("\nAudio policies:");
        synchronized (this.mAudioPolicies) {
            for (AudioPolicyProxy policy : this.mAudioPolicies.values()) {
                pw.println(policy.toLogFriendlyString());
            }
        }
    }

    private void onDynPolicyMixStateUpdate(String regId, int state) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onDynamicPolicyMixStateUpdate(");
        stringBuilder.append(regId);
        stringBuilder.append(", ");
        stringBuilder.append(state);
        stringBuilder.append(")");
        Log.d(str, stringBuilder.toString());
        synchronized (this.mAudioPolicies) {
            for (AudioPolicyProxy policy : this.mAudioPolicies.values()) {
                Iterator it = policy.getMixes().iterator();
                while (it.hasNext()) {
                    if (((AudioMix) it.next()).getRegistration().equals(regId)) {
                        try {
                            policy.mPolicyCallback.notifyMixStateUpdate(regId, state);
                        } catch (RemoteException e) {
                            String str2 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Can't call notifyMixStateUpdate() on IAudioPolicyCallback ");
                            stringBuilder2.append(policy.mPolicyCallback.asBinder());
                            Log.e(str2, stringBuilder2.toString(), e);
                        }
                    }
                }
            }
            return;
        }
    }

    public void registerRecordingCallback(IRecordingConfigDispatcher rcdb) {
        this.mRecordMonitor.registerRecordingCallback(rcdb, this.mContext.checkCallingPermission("android.permission.MODIFY_AUDIO_ROUTING") == 0);
    }

    public void unregisterRecordingCallback(IRecordingConfigDispatcher rcdb) {
        this.mRecordMonitor.unregisterRecordingCallback(rcdb);
    }

    public List<AudioRecordingConfiguration> getActiveRecordingConfigurations() {
        return this.mRecordMonitor.getActiveRecordingConfigurations(this.mContext.checkCallingPermission("android.permission.MODIFY_AUDIO_ROUTING") == 0);
    }

    public void disableRingtoneSync(int userId) {
        if (UserHandle.getCallingUserId() != userId) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "disable sound settings syncing for another profile");
        }
        long token = Binder.clearCallingIdentity();
        try {
            Secure.putIntForUser(this.mContentResolver, "sync_parent_sounds", 0, userId);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void registerPlaybackCallback(IPlaybackConfigDispatcher pcdb) {
        this.mPlaybackMonitor.registerPlaybackCallback(pcdb, this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_AUDIO_ROUTING") == 0);
    }

    public void unregisterPlaybackCallback(IPlaybackConfigDispatcher pcdb) {
        this.mPlaybackMonitor.unregisterPlaybackCallback(pcdb);
    }

    public List<AudioPlaybackConfiguration> getActivePlaybackConfigurations() {
        return this.mPlaybackMonitor.getActivePlaybackConfigurations(this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_AUDIO_ROUTING") == 0);
    }

    public int trackPlayer(PlayerIdCard pic) {
        return this.mPlaybackMonitor.trackPlayer(pic);
    }

    public void playerAttributes(int piid, AudioAttributes attr) {
        this.mPlaybackMonitor.playerAttributes(piid, attr, Binder.getCallingUid());
    }

    public void playerEvent(int piid, int event) {
        this.mPlaybackMonitor.playerEvent(piid, event, Binder.getCallingUid());
    }

    public void playerHasOpPlayAudio(int piid, boolean hasOpPlayAudio) {
        this.mPlaybackMonitor.playerHasOpPlayAudio(piid, hasOpPlayAudio, Binder.getCallingUid());
    }

    public void releasePlayer(int piid) {
        this.mPlaybackMonitor.releasePlayer(piid, Binder.getCallingUid());
    }

    public int dispatchFocusChange(AudioFocusInfo afi, int focusChange, IAudioPolicyCallback pcb) {
        if (afi == null) {
            throw new IllegalArgumentException("Illegal null AudioFocusInfo");
        } else if (pcb != null) {
            int dispatchFocusChange;
            synchronized (this.mAudioPolicies) {
                if (this.mAudioPolicies.containsKey(pcb.asBinder())) {
                    dispatchFocusChange = this.mMediaFocusControl.dispatchFocusChange(afi, focusChange);
                } else {
                    throw new IllegalStateException("Unregistered AudioPolicy for focus dispatch");
                }
            }
            return dispatchFocusChange;
        } else {
            throw new IllegalArgumentException("Illegal null AudioPolicy callback");
        }
    }

    public void setFocusRequestResultFromExtPolicy(AudioFocusInfo afi, int requestResult, IAudioPolicyCallback pcb) {
        if (afi == null) {
            throw new IllegalArgumentException("Illegal null AudioFocusInfo");
        } else if (pcb != null) {
            synchronized (this.mAudioPolicies) {
                if (this.mAudioPolicies.containsKey(pcb.asBinder())) {
                    this.mMediaFocusControl.setFocusRequestResultFromExtPolicy(afi, requestResult);
                } else {
                    throw new IllegalStateException("Unregistered AudioPolicy for external focus");
                }
            }
        } else {
            throw new IllegalArgumentException("Illegal null AudioPolicy callback");
        }
    }

    private void checkMonitorAudioServerStatePermission() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") != 0 && this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_AUDIO_ROUTING") != 0) {
            throw new SecurityException("Not allowed to monitor audioserver state");
        }
    }

    public void registerAudioServerStateDispatcher(IAudioServerStateDispatcher asd) {
        checkMonitorAudioServerStatePermission();
        synchronized (this.mAudioServerStateListeners) {
            if (this.mAudioServerStateListeners.containsKey(asd.asBinder())) {
                Slog.w(TAG, "Cannot re-register audio server state dispatcher");
                return;
            }
            AsdProxy asdp = new AsdProxy(asd);
            try {
                asd.asBinder().linkToDeath(asdp, 0);
            } catch (RemoteException e) {
            }
            this.mAudioServerStateListeners.put(asd.asBinder(), asdp);
        }
    }

    public void unregisterAudioServerStateDispatcher(IAudioServerStateDispatcher asd) {
        checkMonitorAudioServerStatePermission();
        synchronized (this.mAudioServerStateListeners) {
            AsdProxy asdp = (AsdProxy) this.mAudioServerStateListeners.remove(asd.asBinder());
            if (asdp == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Trying to unregister unknown audioserver state dispatcher for pid ");
                stringBuilder.append(Binder.getCallingPid());
                stringBuilder.append(" / uid ");
                stringBuilder.append(Binder.getCallingUid());
                Slog.w(str, stringBuilder.toString());
                return;
            }
            asd.asBinder().unlinkToDeath(asdp, 0);
        }
    }

    public boolean isAudioServerRunning() {
        checkMonitorAudioServerStatePermission();
        return AudioSystem.checkAudioFlinger() == 0;
    }

    protected int hwGetDeviceForStream(int activeStreamType) {
        return getDeviceForStream(mStreamVolumeAlias[activeStreamType]);
    }

    protected int getStreamIndex(int stream_type, int device) {
        if (stream_type > -1 && stream_type < AudioSystem.getNumStreamTypes()) {
            return this.mStreamStates[stream_type].getIndex(device);
        }
        Log.e(TAG, "invalid stream type!!!");
        return -1;
    }

    protected int getStreamMaxIndex(int stream_type) {
        if (stream_type > -1 && stream_type < AudioSystem.getNumStreamTypes()) {
            return this.mStreamStates[stream_type].getMaxIndex();
        }
        Log.e(TAG, "invalid stream type!!!");
        return -1;
    }

    protected boolean isConnectedUsbOutDevice() {
        for (int i = 0; i < this.mConnectedDevices.size(); i++) {
            if (((DeviceListSpec) this.mConnectedDevices.valueAt(i)).mDeviceType == 67108864) {
                return true;
            }
        }
        return false;
    }

    protected boolean isConnectedUsbInDevice() {
        return SystemProperties.getBoolean("persist.sys.usb.capture", false);
    }

    protected boolean isConnectedHeadSet() {
        return this.mConnectedDevices.get(makeDeviceListKey(4, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS)) != null;
    }

    protected boolean isConnectedHeadPhone() {
        boolean headphoneConnected = this.mConnectedDevices.get(makeDeviceListKey(8, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS)) != null;
        boolean lineConnected = this.mConnectedDevices.get(makeDeviceListKey(131072, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS)) != null;
        if (headphoneConnected || lineConnected) {
            return true;
        }
        return false;
    }

    public int getSampleId(SoundPool soundpool, int effect, String defFilePath, int index) {
        return this.mSoundPool.load(defFilePath, 0);
    }

    protected void appendExtraInfo(Intent intent) {
    }

    protected String getPackageNameByPid(int pid) {
        return null;
    }

    protected void sendCommForceBroadcast() {
    }

    protected void checkMuteRcvDelay(int curMode, int mode) {
    }

    protected boolean checkEnbaleVolumeAdjust() {
        return true;
    }

    protected void processMediaServerRestart() {
    }

    protected void onUserBackground(int userId) {
    }

    protected void onUserForeground(int userId) {
    }

    private boolean isHisiPlatform() {
        String platform = SystemProperties.get("ro.board.platform", Shell.NIGHT_MODE_STR_UNKNOWN);
        if (platform == null || !platform.startsWith("hi")) {
            return false;
        }
        return true;
    }

    private void sendBehavior(BehaviorId bid, Object... params) {
        if (this.mHwBehaviorManager == null) {
            this.mHwBehaviorManager = HwFrameworkFactory.getHwBehaviorCollectManager();
        }
        if (this.mHwBehaviorManager == null) {
            Log.w(TAG, "HwBehaviorCollectManager is null");
        } else if (params == null || params.length == 0) {
            this.mHwBehaviorManager.sendBehavior(Binder.getCallingUid(), Binder.getCallingPid(), bid);
        } else {
            this.mHwBehaviorManager.sendBehavior(Binder.getCallingUid(), Binder.getCallingPid(), bid, params);
        }
    }

    public IBinder getHwInnerService() {
        return this.mHwInnerService;
    }

    public boolean isConnectedHeadSetEx() {
        return isConnectedHeadSet();
    }

    public boolean isConnectedHeadPhoneEx() {
        return isConnectedHeadPhone();
    }

    public boolean isConnectedUsbOutDeviceEx() {
        return isConnectedUsbOutDevice();
    }

    public boolean isConnectedUsbInDeviceEx() {
        return isConnectedUsbInDevice();
    }

    public boolean checkAudioSettingsPermissionEx(String method) {
        return checkAudioSettingsPermission(method);
    }
}
