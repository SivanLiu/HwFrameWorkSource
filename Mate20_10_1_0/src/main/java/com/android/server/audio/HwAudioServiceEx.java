package com.android.server.audio;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IApplicationThread;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.cover.HallState;
import android.cover.IHallCallback;
import android.cover.IHwCoverManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.AudioRecordingConfiguration;
import android.media.AudioSystem;
import android.media.IAudioModeDispatcher;
import android.media.IAudioService;
import android.media.IRecordingConfigDispatcher;
import android.media.audiofx.AudioEffect;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IHwBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import android.view.ContextThemeWrapper;
import android.widget.Toast;
import com.android.server.hidata.wavemapping.modelservice.ModelBaseService;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.location.HwLocalLocationProvider;
import com.android.server.notification.HwCustZenModeHelper;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import com.huawei.android.app.ActivityManagerEx;
import com.huawei.android.app.IHwActivityNotifierEx;
import com.huawei.android.media.IDeviceSelectCallback;
import com.huawei.android.pgmng.plug.PowerKit;
import com.huawei.android.util.NoExtAPIException;
import com.huawei.cust.HwCfgFilePolicy;
import com.huawei.displayengine.IDisplayEngineService;
import huawei.cust.HwCustUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import vendor.huawei.hardware.dolby.dms.V1_0.IDms;
import vendor.huawei.hardware.dolby.dms.V1_0.IDmsCallbacks;
import vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsCompModeID;

public final class HwAudioServiceEx implements IHwAudioServiceEx {
    private static final String ACTION_ADJUST_STREAM_VOLUME = "AdjustStreamVolume";
    private static final String ACTION_ANALOG_TYPEC_NOTIFY = "ACTION_TYPEC_NONOTIFY";
    private static final String ACTION_DIGITAL_TYPEC_NOTIFY = "ACTION_DIGITAL_TYPEC_NONOTIFY";
    private static final String ACTION_KILLED_APP_FOR_KARAOKE = "huawei.intent.action.APP_KILLED_FOR_KARAOKE_ACTION";
    private static final String ACTION_KILLED_APP_FOR_KIT = "huawei.intent.action.APP_KILLED_FOR_KIT_ACTION";
    private static final String ACTION_RECORDNAME_APP_FOR_KIT = "huawei.intent.action.APP_RECORDNAME_FOR_KIT_ACTION";
    private static final String ACTION_SEND_AUDIO_RECORD_STATE = "huawei.media.AUDIO_RECORD_STATE_CHANGED_ACTION";
    private static final String ACTION_START_APP_FOR_KARAOKE = "huawei.media.ACTIVITY_STARTING_FOR_KARAOKE_ACTION";
    private static final String ACTION_SWS_EQ = "huawei.intent.action.SWS_EQ";
    private static final String ACTIVE_PKG_STR = "activePkg";
    static final String ACTIVITY_NOTIFY_COMPONENTNAME = "comp";
    static final String ACTIVITY_NOTIFY_ONPAUSE = "onPause";
    static final String ACTIVITY_NOTIFY_ONRESUME = "onResume";
    static final String ACTIVITY_NOTIFY_REASON = "activityLifeState";
    static final String ACTIVITY_NOTIFY_STATE = "state";
    private static final int ANALOG_TYPEC = 1;
    private static final int ANALOG_TYPEC_CONNECTED_DISABLE = 0;
    private static final int ANALOG_TYPEC_CONNECTED_ENABLE = 1;
    private static final int ANALOG_TYPEC_CONNECTED_ID = 1;
    private static final int ANALOG_TYPEC_DEVICES = 131084;
    private static final String ANALOG_TYPEC_FLAG = "audio_capability#usb_analog_hs_report";
    private static final String BOOT_VOLUME_PROPERTY = "persist.sys.volume.ringIndex";
    private static final int BYTES_PER_INT = 4;
    private static final String CMDINTVALUE = "Integer Value";
    private static final String CONCURRENT_CAPTURE_PROPERTY = "ro.config.concurrent_capture";
    private static final String[] CONCURRENT_RECORD_OTHER = {"com.baidu.BaiduMap", "com.autonavi.minimap"};
    private static final String[] CONCURRENT_RECORD_SYSTEM = {"com.huawei.vassistant"};
    private static final String CONFIG_FILE_WHITE_BLACK_APP = "xml/hw_karaokeeffect_app_config.xml";
    private static final int DEFAULT_CHANNEL_CNT = 2;
    private static final int DEVICE_ID_MAX_LENGTH = 128;
    private static final int DIGITAL_TYPEC_CONNECTED_DISABLE = 0;
    private static final int DIGITAL_TYPEC_CONNECTED_ENABLE = 1;
    private static final int DIGITAL_TYPEC_CONNECTED_ID = 2;
    private static final String DIGITAL_TYPEC_FLAG = "typec_compatibility_check";
    private static final String DIGITAL_TYPEC_REPORT_FLAG = "audio_capability#usb_compatibility_report";
    private static final int DIGTIAL_TYPEC = 2;
    private static final int DISABLE_VIRTUAL_AUDIO = 0;
    private static final int DOLBY_HIGH_PRIO = 1;
    private static final int DOLBY_LOW_PRIO = -1;
    private static final String DOLBY_MODE_HEADSET_STATE = "persist.sys.dolby.state";
    private static final String DOLBY_MODE_OFF_PARA = "dolby_profile=off";
    private static final String DOLBY_MODE_ON_PARA = "dolby_profile=on";
    private static final String DOLBY_MODE_PRESTATE = "persist.sys.dolby.prestate";
    private static final String[] DOLBY_PROFILE = {"off", "smart", "movie", "music"};
    /* access modifiers changed from: private */
    public static final boolean DOLBY_SOUND_EFFECTS_SUPPORT = SystemProperties.getBoolean("ro.config.dolby_dap", false);
    private static final String DOLBY_UPDATE_EVENT = "dolby_dap_params_update";
    private static final String DOLBY_UPDATE_EVENT_DS_STATE = "ds_state_change";
    private static final String DOLBY_UPDATE_EVENT_PROFILE = "profile_change";
    private static final byte[] DS_DISABLE_ARRAY = {0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0};
    private static final byte[] DS_ENABLE_ARRAY = {0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0};
    private static final byte[] DS_GAME_MODE_ARRAY = {0, 0, 0, 10, 1, 0, 0, 0, 3, 0, 0, 0};
    private static final byte[] DS_MOVIE_MODE_ARRAY = {0, 0, 0, 10, 1, 0, 0, 0, 1, 0, 0, 0};
    private static final byte[] DS_MUSIC_MODE_ARRAY = {0, 0, 0, 10, 1, 0, 0, 0, 2, 0, 0, 0};
    private static final byte[] DS_SMART_MODE_ARRAY = {0, 0, 0, 10, 1, 0, 0, 0, 0, 0, 0, 0};
    private static final int EFFECT_PARAM_EFF_ENAB = 0;
    private static final int EFFECT_PARAM_EFF_PROF = 167772160;
    private static final int EFFECT_SUPPORT_DEVICE = 603980172;
    private static final UUID EFFECT_TYPE_DS = UUID.fromString("9d4921da-8225-4f29-aefa-39537a04bcaa");
    private static final UUID EFFECT_TYPE_NULL = UUID.fromString("ec7178ec-e5e1-4432-a3f4-4657e6795210");
    private static final int ENABLE_VIRTUAL_AUDIO = 1;
    private static final String ENGINE_PACKAGE_NAME = "com.huawei.multimedia.audioengine";
    private static final String EVENTNAME = "event name";
    private static final String EXTRA_VOLUME_STREAM_DIRECTION = "Direction";
    private static final String EXTRA_VOLUME_STREAM_FLAGS = "Flags";
    private static final String[] FORBIDDEN_SOUND_EFFECT_WHITE_LIST = {"com.jawbone.up", "com.codoon.gps", "com.lakala.cardwatch", "com.hoolai.magic", "com.android.bankabc", "com.icbc", "com.icbc.wapc", "com.chinamworld.klb", "com.yitong.bbw.mbank.android", "com.chinamworld.bocmbci", "com.cloudcore.emobile.szrcb", "com.chinamworld.main", "com.nbbank", "com.cmb.ubank.UBUI", "com.nxy.mobilebank.ln", "cn.com.cmbc.newmbank"};
    private static final int GLOBAL_SESSION_ID = 0;
    private static final String HIDE_HIRES_ICON = "huawei.intent.action.hideHiResIcon";
    private static final String HIRES_REPORT_FLAG = "typec_need_show_hires";
    private static final String HS_NO_CHARGE_OFF = "hs_no_charge=off";
    private static final String HS_NO_CHARGE_ON = "hs_no_charge=on";
    /* access modifiers changed from: private */
    public static final boolean HUAWEI_SWS31_CONFIG = (!"sws2".equalsIgnoreCase(SWS_VERSION) && !"sws3".equalsIgnoreCase(SWS_VERSION));
    private static final int HW_DOBBLY_SOUND_EFFECT_BIT = 4;
    private static final int HW_KARAOKE_EFFECT_BIT = 2;
    private static final boolean HW_KARAOKE_EFFECT_ENABLED = ((2 & SystemProperties.getInt("ro.config.hw_media_flags", 0)) != 0);
    private static final int INVALID_RECORD_STATE = -1;
    private static final String ISONTOP = "isOnTop";
    /* access modifiers changed from: private */
    public static final boolean IS_SUPPORT_SLIDE = ((SystemProperties.getInt("ro.config.hw_hall_prop", 0) & 1) != 0);
    private static final int MSG_CONNECT_DOLBY_SERVICE = 11;
    private static final int MSG_INIT_POWERKIT = 15;
    private static final int MSG_SEND_AUDIOKIT_BROADCAST = 19;
    private static final int MSG_SEND_DOLBYUPDATE_BROADCAST = 12;
    private static final int MSG_SET_DEVICE_STATE = 17;
    private static final int MSG_SET_SOUND_EFFECT_STATE = 13;
    private static final int MSG_SET_TYPEC_PARAM = 14;
    private static final int MSG_SHOW_OR_HIDE_HIRES = 10;
    private static final int MSG_SHOW_RECORD_SILENCE_TOAST = 18;
    private static final int MSG_START_DOLBY_DMS = 16;
    private static final int MSG_START_DOLBY_DMS_DELAY_FOUR_CHANNEL = 1000;
    private static final String NODE_ATTR_PACKAGE = "package";
    private static final String NODE_WHITEAPP = "whiteapp";
    private static final String PACKAGENAME = "packageName";
    private static final String PACKAGENAME_LIST = "packageNameList";
    private static final String PACKAGE_PACKAGEINSTALLER = "com.android.packageinstaller";
    private static final String PERMISSION_ADJUST_STREAM_VOLUME = "com.huawei.android.permission.ADJUSTSTREAMVOLUME";
    private static final String PERMISSION_HIRES_CHANGE = "huawei.permission.HIRES_ICON_CHANGE_ACTION";
    private static final String PERMISSION_KILLED_APP_FOR_KARAOKE = "com.huawei.permission.APP_KILLED_FOR_KARAOKE_ACTION";
    private static final String PERMISSION_KILLED_APP_FOR_KIT = "huawei.permission.APP_KILLED_FOR_KIT_ACTION";
    private static final String PERMISSION_RECORDNAME_APP_FOR_KIT = "huawei.permission.APP_RECORDNAME_FOR_KIT_ACTION";
    private static final String PERMISSION_SEND_AUDIO_RECORD_STATE = "com.huawei.permission.AUDIO_RECORD_STATE_CHANGED_ACTION";
    private static final String PERMISSION_SWS_EQ = "android.permission.SWS_EQ";
    private static final String PHONE_PKG = "com.android.phone";
    private static final int QUAD_CHANNEL_CNT = 4;
    private static final String[] RECORD_ACTIVE_APP_LIST = {"com.realvnc.android.remote"};
    private static final String[] RECORD_REQUEST_APP_LIST = {"com.google.android", "com.google.android.googlequicksearchbox:search", "com.google.android.googlequicksearchbox:interactor"};
    private static final int RECORD_TYPE_OTHER = 536870912;
    private static final int RECORD_TYPE_SYSTEM = 1073741824;
    private static final String RESERVED = "reserved";
    private static final String RESTORE = "restore";
    private static final int SENDMSG_NOOP = 1;
    private static final int SENDMSG_QUEUE = 2;
    private static final int SENDMSG_REPLACE = 0;
    private static final int SERVICE_ID_MAX_LENGTH = 64;
    private static final int SERVICE_TYPE_MIC = 2;
    private static final int SERVICE_TYPE_SPEAKER = 3;
    private static final String SHOW_HIRES_ICON = "huawei.intent.action.showHiResIcon";
    private static final int SHOW_OR_HIDE_HIRES_DELAY = 500;
    private static final String SILENCE_PKG_STR = "silencePkg";
    private static final boolean SOUND_EFFECTS_SUPPORT;
    private static final int SOUND_EFFECT_CLOSE = 1;
    private static final int SOUND_EFFECT_OPEN = 2;
    private static final int START_DOLBY_DMS_DELAY = 200;
    private static final String SWS_MODE_OFF_PARA = "HIFIPARA=STEREOWIDEN_Enable=false";
    private static final String SWS_MODE_ON_PARA = "HIFIPARA=STEREOWIDEN_Enable=true";
    private static final String SWS_MODE_PARA = "HIFIPARA=STEREOWIDEN_Enable";
    private static final String SWS_MODE_PRESTATE = "persist.sys.sws.prestate";
    private static final int SWS_OFF = 0;
    private static final int SWS_ON = 3;
    private static final int SWS_SCENE_AUDIO = 0;
    private static final int SWS_SCENE_VIDEO = 1;
    /* access modifiers changed from: private */
    public static final boolean SWS_SOUND_EFFECTS_SUPPORT = SystemProperties.getBoolean("ro.config.hw_sws", false);
    private static final boolean SWS_TYPEC_ADAPTE_SUPPORT = (SWS_SOUND_EFFECTS_SUPPORT && ("0600".equals(SWS_VERSION) || "0610".equals(SWS_VERSION)));
    private static final String SWS_TYPEC_MANUFACTURER = "SWS_TYPEC_MANUFACTURER";
    private static final String SWS_TYPEC_PRODUCT_NAME = "SWS_TYPEC_PRODUCT_NAME";
    private static final String SWS_TYPEC_RESTORE = "SWS_TYPEC_RESTORE";
    private static final String SWS_VERSION = SystemProperties.get("ro.config.sws_version", "sws2");
    private static final boolean SWS_VIDEO_MODE = ("sws3".equalsIgnoreCase(SWS_VERSION) || "sws3_1".equalsIgnoreCase(SWS_VERSION) || SystemProperties.getBoolean("ro.config.sws_moviemode", false));
    private static final String SYSTEMSERVER_START = "com.huawei.systemserver.START";
    private static final String TAG = "HwAudioServiceEx";
    private static final String TOAST_THEME = "androidhwext:style/Theme.Emui.Toast";
    private static final int UNKNOWN_DEVICE = -1;
    private static final int VOLUME_INDEX_UNIT = 10;
    /* access modifiers changed from: private */
    public static final int mDolbyChCnt = SystemProperties.getInt("ro.config.dolby_channel", 2);
    /* access modifiers changed from: private */
    public static final boolean mIsUsbPowercosumeTips = SystemProperties.getBoolean("ro.config.usb_power_tips", false);
    private static final boolean mNotShowTypecTip = SystemProperties.getBoolean("ro.config.typec_not_show", false);
    /* access modifiers changed from: private */
    public static int mSwsScene = 0;
    private static final int mUsbSecurityVolumeIndex = SystemProperties.getInt("ro.config.hw.usb_security_volume", 0);
    private static final int propvalue_not_support_slide = 0;
    private static final int propvalue_product_support_slide = 1;
    private static final String receiverName = "audioserver";
    /* access modifiers changed from: private */
    public int PowerKitAppType = -1;
    /* access modifiers changed from: private */
    public IHwCoverManager coverManager;
    private BroadcastReceiver mAnalogTypecReceiver = null;
    /* access modifiers changed from: private */
    public List<String> mAudioKitList = new ArrayList();
    /* access modifiers changed from: private */
    public int mAudioMode = 0;
    /* access modifiers changed from: private */
    public IHallCallback.Stub mCallback;
    private final ArrayList<AudioModeClient> mClients = new ArrayList<>();
    private boolean mConcurrentCaptureEnable;
    private ContentResolver mContentResolver;
    /* access modifiers changed from: private */
    public final Context mContext;
    private HwCustZenModeHelper mCustZenModeHelper;
    private BroadcastReceiver mDigitalTypecReceiver = null;
    /* access modifiers changed from: private */
    public IDms mDms = null;
    /* access modifiers changed from: private */
    public IHwBinder.DeathRecipient mDmsDeathRecipient = new IHwBinder.DeathRecipient() {
        /* class com.android.server.audio.HwAudioServiceEx.AnonymousClass7 */

        public void serviceDied(long cookie) {
            if (HwAudioServiceEx.this.mHwAudioHandlerEx != null) {
                Slog.e(HwAudioServiceEx.TAG, "Dolby service has died, try to reconnect 1s later.");
                HwAudioServiceEx.sendMsgEx(HwAudioServiceEx.this.mHwAudioHandlerEx, 11, 0, 0, 0, null, 1000);
            }
        }
    };
    private boolean mDmsStarted = false;
    /* access modifiers changed from: private */
    public IDmsCallbacks mDolbyClient = new IDmsCallbacks.Stub() {
        /* class com.android.server.audio.HwAudioServiceEx.AnonymousClass9 */

        @Override // vendor.huawei.hardware.dolby.dms.V1_0.IDmsCallbacks
        public void onDapParamUpdate(ArrayList<Byte> params) {
            if (params.size() != 0) {
                byte[] buf = new byte[params.size()];
                for (int i = 0; i < buf.length; i++) {
                    buf[i] = params.get(i).byteValue();
                }
                int dlbParam = HwAudioServiceEx.byteArrayToInt32(buf, 0);
                int status = HwAudioServiceEx.byteArrayToInt32(buf, 4);
                Slog.d(HwAudioServiceEx.TAG, "Got dap param update, param = " + dlbParam + " status = " + status);
                if (dlbParam == 0 || dlbParam == HwAudioServiceEx.EFFECT_PARAM_EFF_PROF) {
                    if (!HwAudioServiceEx.this.mSystemReady) {
                        Slog.w(HwAudioServiceEx.TAG, "onDapParamUpdate() called before boot complete");
                    } else {
                        HwAudioServiceEx.sendMsgEx(HwAudioServiceEx.this.mHwAudioHandlerEx, 12, 0, dlbParam, status, null, 1000);
                    }
                }
                if (dlbParam == 0) {
                    int unused = HwAudioServiceEx.this.mDolbyEnable = status;
                    if (HwAudioServiceEx.this.mHeadSetPlugState) {
                        SystemProperties.set(HwAudioServiceEx.DOLBY_MODE_HEADSET_STATE, HwAudioServiceEx.this.mDolbyEnable > 0 ? "on" : "off");
                    }
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public int mDolbyEnable = 0;
    private AudioEffect mEffect = null;
    private final UEventObserver mFanAndPhoneCoverObserver = new UEventObserver() {
        /* class com.android.server.audio.HwAudioServiceEx.AnonymousClass1 */

        public void onUEvent(UEventObserver.UEvent event) {
            String wlanFanStatus = event.get("UI_WL_FAN_STATUS");
            String wlanCoverStatus = event.get("UI_WL_COVER_STATUS");
            String vbusDisconnect = event.get("VBUS_DISCONNECT");
            if (wlanFanStatus != null && wlanFanStatus.equals(HwAudioServiceEx.this.mWlanFanConnected)) {
                Log.i(HwAudioServiceEx.TAG, "wlanCharge status of fan is connected");
                boolean unused = HwAudioServiceEx.this.mIsFanOn = true;
            }
            if (wlanCoverStatus != null && wlanCoverStatus.equals(HwAudioServiceEx.this.mWlanCoverConnected)) {
                Log.i(HwAudioServiceEx.TAG, "wlanCharge status of cover is connected");
                boolean unused2 = HwAudioServiceEx.this.mIsCoverOn = true;
            }
            boolean isVbusDisconnect = false;
            if (vbusDisconnect != null && TextUtils.isEmpty(vbusDisconnect)) {
                isVbusDisconnect = true;
            }
            if (isVbusDisconnect) {
                Log.i(HwAudioServiceEx.TAG, "wlanCharge status of vbus is disconnected.");
                AudioSystem.setParameters("wireless_charge=off");
                boolean unused3 = HwAudioServiceEx.this.mIsFanOn = false;
                boolean unused4 = HwAudioServiceEx.this.mIsCoverOn = false;
                boolean unused5 = HwAudioServiceEx.this.mIsWirelessChargeOn = false;
            } else if ((HwAudioServiceEx.this.mIsFanOn || HwAudioServiceEx.this.mIsCoverOn) && !HwAudioServiceEx.this.mIsWirelessChargeOn) {
                AudioSystem.setParameters("wireless_charge=on");
                boolean unused6 = HwAudioServiceEx.this.mIsWirelessChargeOn = true;
            } else {
                Log.w(HwAudioServiceEx.TAG, "wrong state from observer, do nothing.");
            }
        }
    };
    private boolean mFmDeviceOn = false;
    /* access modifiers changed from: private */
    public boolean mHeadSetPlugState = false;
    /* access modifiers changed from: private */
    public HwAudioHandlerEx mHwAudioHandlerEx;
    private HwAudioHandlerExThread mHwAudioHandlerExThread;
    private IHwAudioServiceInner mIAsInner = null;
    /* access modifiers changed from: private */
    public boolean mIsAnalogTypecReceiverRegisterd = false;
    /* access modifiers changed from: private */
    public boolean mIsCoverOn = false;
    private boolean mIsDigitalTypecOn = false;
    /* access modifiers changed from: private */
    public boolean mIsDigitalTypecReceiverRegisterd = false;
    /* access modifiers changed from: private */
    public boolean mIsFanOn = false;
    /* access modifiers changed from: private */
    public boolean mIsWirelessChargeOn = false;
    private Map<String, Integer> mKaraokeUidsMap = new HashMap();
    private ArrayList<String> mKaraokeWhiteList = null;
    /* access modifiers changed from: private */
    public List<AudioRecordingConfiguration> mLastRecordingConfigs = new ArrayList();
    /* access modifiers changed from: private */
    public AudioRecordingConfiguration mLastSilenceRec = null;
    private Handler mMyHandler = new Handler() {
        /* class com.android.server.audio.HwAudioServiceEx.AnonymousClass6 */

        public void handleMessage(Message msg) {
            String curSWSState;
            String preDolbyState;
            String preSWSState;
            int i = msg.what;
            if (i != 1) {
                if (i == 2) {
                    if (HwAudioServiceEx.SWS_SOUND_EFFECTS_SUPPORT && HwAudioServiceEx.HUAWEI_SWS31_CONFIG && (preSWSState = SystemProperties.get(HwAudioServiceEx.SWS_MODE_PRESTATE, ModelBaseService.UNKONW_IDENTIFY_RET)) != null && preSWSState.equals("on")) {
                        AudioSystem.setParameters(HwAudioServiceEx.SWS_MODE_ON_PARA);
                        SystemProperties.set(HwAudioServiceEx.SWS_MODE_PRESTATE, ModelBaseService.UNKONW_IDENTIFY_RET);
                    }
                    if (HwAudioServiceEx.DOLBY_SOUND_EFFECTS_SUPPORT && (preDolbyState = SystemProperties.get(HwAudioServiceEx.DOLBY_MODE_PRESTATE, ModelBaseService.UNKONW_IDENTIFY_RET)) != null && preDolbyState.equals("on")) {
                        HwAudioServiceEx.this.setDolbyEnable(true);
                        SystemProperties.set(HwAudioServiceEx.DOLBY_MODE_PRESTATE, ModelBaseService.UNKONW_IDENTIFY_RET);
                    }
                }
            } else if (AudioSystem.getDeviceConnectionState(8, "") == 1 || AudioSystem.getDeviceConnectionState(4, "") == 1 || AudioSystem.getDeviceConnectionState(67108864, "") == 1 || AudioSystem.getDeviceConnectionState((int) HwAudioServiceEx.RECORD_TYPE_OTHER, "") == 1) {
                if (HwAudioServiceEx.SWS_SOUND_EFFECTS_SUPPORT && HwAudioServiceEx.HUAWEI_SWS31_CONFIG && (curSWSState = AudioSystem.getParameters(HwAudioServiceEx.SWS_MODE_PARA)) != null && curSWSState.contains(HwAudioServiceEx.SWS_MODE_ON_PARA)) {
                    AudioSystem.setParameters(HwAudioServiceEx.SWS_MODE_OFF_PARA);
                    SystemProperties.set(HwAudioServiceEx.SWS_MODE_PRESTATE, "on");
                }
                if (HwAudioServiceEx.DOLBY_SOUND_EFFECTS_SUPPORT && HwAudioServiceEx.this.mDolbyEnable > 0) {
                    SystemProperties.set(HwAudioServiceEx.DOLBY_MODE_PRESTATE, "on");
                    HwAudioServiceEx.this.setDolbyEnable(false);
                }
            }
        }
    };
    private NotificationManager mNm;
    /* access modifiers changed from: private */
    public NotificationManager mNotificationManager = null;
    private IHwActivityNotifierEx mNotifierForLifeState = new IHwActivityNotifierEx() {
        /* class com.android.server.audio.HwAudioServiceEx.AnonymousClass3 */

        public void call(Bundle extras) {
            if (extras == null) {
                Slog.w(HwAudioServiceEx.TAG, "AMS callback, but extras=null");
                return;
            }
            ComponentName comp = (ComponentName) extras.getParcelable("comp");
            if (comp != null) {
                String packageName = comp.getPackageName();
                String flag = extras.getString("state");
                boolean isTop = extras.getBoolean("isTop");
                Slog.i(HwAudioServiceEx.TAG, "ComponentInfo : className = " + comp.getClassName() + ", flag=" + flag + ", isHomeActivity=, isTop=" + isTop);
                if ("onResume".equals(flag)) {
                    HwAudioServiceEx.this.setSoundEffectState(false, packageName, true, null);
                } else if ("onPause".equals(flag)) {
                    HwAudioServiceEx.this.setSoundEffectState(false, packageName, false, null);
                }
            } else {
                Slog.e(HwAudioServiceEx.TAG, "ComponentName is null");
            }
        }
    };
    private String mOldPackageName = "";
    private HashMap<String, Integer> mPackageUidMap = new HashMap<>();
    private PowerKit mPowerKit = null;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        /* class com.android.server.audio.HwAudioServiceEx.AnonymousClass8 */

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals("android.intent.action.REBOOT") || action.equals(SmartDualCardConsts.SYSTEM_STATE_NAME_ACTION_SHUTDOWN)) {
                    if (HwAudioServiceEx.DOLBY_SOUND_EFFECTS_SUPPORT && HwAudioServiceEx.this.mDms == null) {
                        HwAudioServiceEx.this.setDolbyServiceClient();
                    }
                    Slog.i(HwAudioServiceEx.TAG, "Release Dolby service HIDL client");
                    if (!(HwAudioServiceEx.this.mDms == null || HwAudioServiceEx.this.mDolbyClient == null)) {
                        try {
                            HwAudioServiceEx.this.mDms.unregisterClient(HwAudioServiceEx.this.mDolbyClient, 3, HwAudioServiceEx.this.mDolbyClient.hashCode());
                            HwAudioServiceEx.this.mDms.unlinkToDeath(HwAudioServiceEx.this.mDmsDeathRecipient);
                        } catch (RuntimeException e) {
                            Slog.e(HwAudioServiceEx.TAG, "Release Dolby RuntimeException");
                        } catch (Exception e2) {
                            Slog.e(HwAudioServiceEx.TAG, "Release Dolby error");
                        }
                    }
                    SystemProperties.set(HwAudioServiceEx.BOOT_VOLUME_PROPERTY, Integer.toString(AudioSystem.getStreamVolumeIndex(7, 2)));
                } else if (HwAudioServiceEx.IS_SUPPORT_SLIDE && action.equals(HwAudioServiceEx.SYSTEMSERVER_START)) {
                    boolean ret = HwAudioServiceEx.this.coverManager.registerHallCallback(HwAudioServiceEx.receiverName, 1, HwAudioServiceEx.this.mCallback);
                    Slog.d(HwAudioServiceEx.TAG, "registerHallCallback return " + ret);
                    if (2 == HwAudioServiceEx.this.coverManager.getHallState(1)) {
                        AudioSystem.setParameters("action_slide=true");
                        Slog.i(HwAudioServiceEx.TAG, "originSlideOnStatus True");
                    } else if (HwAudioServiceEx.this.coverManager.getHallState(1) == 0) {
                        AudioSystem.setParameters("action_slide=false");
                        Slog.i(HwAudioServiceEx.TAG, "originSlideOnStatus false");
                    } else {
                        Slog.e(HwAudioServiceEx.TAG, "no support hall");
                    }
                }
            }
        }
    };
    private final IRecordingConfigDispatcher mRecordingListener = new IRecordingConfigDispatcher.Stub() {
        /* class com.android.server.audio.HwAudioServiceEx.AnonymousClass11 */

        /* JADX WARNING: Code restructure failed: missing block: B:34:0x00f3, code lost:
            if (com.android.server.audio.HwAudioServiceEx.access$4700(r3, com.android.server.audio.HwAudioServiceEx.access$4200(r3).getClientAudioSource()) != false) goto L_0x00f5;
         */
        public void dispatchRecordingConfigChange(List<AudioRecordingConfiguration> configs) {
            if (configs == null) {
                AudioRecordingConfiguration unused = HwAudioServiceEx.this.mLastSilenceRec = null;
                return;
            }
            ArrayList<String> packageNameList = new ArrayList<>();
            if (HwAudioServiceEx.this.mAudioKitList != null && HwAudioServiceEx.this.mAudioKitList.size() > 0) {
                for (AudioRecordingConfiguration audioRecordingConfig : configs) {
                    if (audioRecordingConfig != null) {
                        packageNameList.add(audioRecordingConfig.getClientPackageName());
                    }
                    Slog.i(HwAudioServiceEx.TAG, "dispatchRecordingConfigChange, sendBroadcastForAudioKit");
                    Bundle bundle = new Bundle();
                    bundle.putStringArrayList(HwAudioServiceEx.PACKAGENAME_LIST, packageNameList);
                    HwAudioServiceEx.this.mHwAudioHandlerEx.sendMessage(HwAudioServiceEx.this.mHwAudioHandlerEx.obtainMessage(19, bundle));
                }
            }
            HwAudioServiceEx.this.findLastSilenceRecord(configs);
            if (HwAudioServiceEx.this.mAudioMode == 2 && HwAudioServiceEx.this.mLastSilenceRec != null) {
                AudioRecordingConfiguration unused2 = HwAudioServiceEx.this.mLastSilenceRec = null;
            }
            if (HwAudioServiceEx.this.mLastSilenceRec != null) {
                AudioRecordingConfiguration activeRecord = null;
                Iterator<AudioRecordingConfiguration> it = configs.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    AudioRecordingConfiguration arc = it.next();
                    if (!arc.isClientSilenced() && HwAudioServiceEx.this.isMicSource(arc.getClientAudioSource())) {
                        activeRecord = arc;
                        Slog.i(HwAudioServiceEx.TAG, "Audio record from active uid:" + HwAudioServiceEx.this.mLastSilenceRec.getClientUid() + " to silenced uid:" + activeRecord.getClientUid());
                        break;
                    }
                }
                if (activeRecord != null) {
                    if (!HwAudioServiceEx.this.isPrivacySensitiveSource(activeRecord.getClientAudioSource())) {
                        HwAudioServiceEx hwAudioServiceEx = HwAudioServiceEx.this;
                    }
                    HwAudioServiceEx hwAudioServiceEx2 = HwAudioServiceEx.this;
                    hwAudioServiceEx2.sendMicSilencedToastMesg(hwAudioServiceEx2.mLastSilenceRec.getClientPackageName(), activeRecord.getClientPackageName());
                    AudioRecordingConfiguration unused3 = HwAudioServiceEx.this.mLastSilenceRec = null;
                }
            }
            HwAudioServiceEx.this.mLastRecordingConfigs.clear();
            HwAudioServiceEx.this.mLastRecordingConfigs.addAll(configs);
        }
    };
    private boolean mShowHiResIcon = false;
    private String mStartedPackageName = null;
    private PowerKit.Sink mStateRecognitionListener = new PowerKit.Sink() {
        /* class com.android.server.audio.HwAudioServiceEx.AnonymousClass5 */

        public void onStateChanged(int stateType, int eventType, int pid, String pkg, int uid) {
            if (pid != Process.myPid()) {
                if (stateType == 2) {
                    if (eventType == 1) {
                        HwAudioServiceEx hwAudioServiceEx = HwAudioServiceEx.this;
                        int unused = hwAudioServiceEx.PowerKitAppType = hwAudioServiceEx.getAppTypeForVBR(uid);
                        Slog.i(HwAudioServiceEx.TAG, "VBR HwAudioService:music app enter " + HwAudioServiceEx.this.PowerKitAppType);
                        if (HwAudioServiceEx.this.PowerKitAppType == 12) {
                            AudioSystem.setParameters("VBRMuiscState=enter");
                        } else if (HwAudioServiceEx.this.PowerKitAppType == 11) {
                            AudioSystem.setParameters("VBRIMState=enter");
                        } else {
                            Slog.v(HwAudioServiceEx.TAG, "no process apptype");
                        }
                    } else if (eventType == 2) {
                        HwAudioServiceEx hwAudioServiceEx2 = HwAudioServiceEx.this;
                        int unused2 = hwAudioServiceEx2.PowerKitAppType = hwAudioServiceEx2.getAppTypeForVBR(uid);
                        Slog.i(HwAudioServiceEx.TAG, "VBR HwAudioService:music app exit " + HwAudioServiceEx.this.PowerKitAppType);
                        if (HwAudioServiceEx.this.PowerKitAppType == 12) {
                            AudioSystem.setParameters("VBRMuiscState=exit");
                        } else if (HwAudioServiceEx.this.PowerKitAppType == 11) {
                            AudioSystem.setParameters("VBRIMState=exit");
                        } else {
                            Slog.v(HwAudioServiceEx.TAG, "no process apptype");
                        }
                    } else {
                        Slog.v(HwAudioServiceEx.TAG, "no process eventType");
                    }
                }
                if (stateType != 10009) {
                    return;
                }
                if (eventType == 1) {
                    if (HwAudioServiceEx.mSwsScene == 0) {
                        int unused3 = HwAudioServiceEx.mSwsScene = 1;
                        Slog.i(HwAudioServiceEx.TAG, "Video_Front state");
                        AudioSystem.setParameters("IPGPMode=video");
                    }
                } else if (eventType != 2) {
                    Slog.v(HwAudioServiceEx.TAG, "no process eventType");
                } else if (HwAudioServiceEx.mSwsScene != 0) {
                    int unused4 = HwAudioServiceEx.mSwsScene = 0;
                    Slog.i(HwAudioServiceEx.TAG, "Video_Not_Front state");
                    AudioSystem.setParameters("IPGPMode=audio");
                }
            }
        }
    };
    private int mSupportDevcieRef = 0;
    private int mSwsStatus = 0;
    /* access modifiers changed from: private */
    public boolean mSystemReady;
    private Toast mToast;
    /* access modifiers changed from: private */
    public String mWlanCoverConnected = "1";
    /* access modifiers changed from: private */
    public String mWlanFanConnected = "1";
    private String[] mZenModeWhiteList;
    IDeviceSelectCallback mcb = null;
    private IAudioService sService;

    static {
        boolean z = false;
        if (SWS_SOUND_EFFECTS_SUPPORT || DOLBY_SOUND_EFFECTS_SUPPORT) {
            z = true;
        }
        SOUND_EFFECTS_SUPPORT = z;
    }

    public HwAudioServiceEx(IHwAudioServiceInner ias, Context context) {
        this.mIAsInner = ias;
        this.mContext = context;
        this.mContentResolver = this.mContext.getContentResolver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.REBOOT");
        intentFilter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_ACTION_SHUTDOWN);
        intentFilter.addAction(SYSTEMSERVER_START);
        context.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, intentFilter, null, null);
        if (IS_SUPPORT_SLIDE) {
            this.coverManager = HwFrameworkFactory.getCoverManager();
            this.mCallback = new IHallCallback.Stub() {
                /* class com.android.server.audio.HwAudioServiceEx.AnonymousClass2 */

                public void onStateChange(HallState hallState) {
                    Slog.d(HwAudioServiceEx.TAG, "HallState=" + hallState);
                    if (hallState.state == 2) {
                        AudioSystem.setParameters("VOICE_SLIDE_STATUS=open");
                        Slog.i(HwAudioServiceEx.TAG, "set VOICE_SLIDE_STATUS open");
                    } else if (hallState.state == 0) {
                        AudioSystem.setParameters("VOICE_SLIDE_STATUS=close");
                        Slog.i(HwAudioServiceEx.TAG, "set VOICE_SLIDE_STATUS close");
                    } else {
                        Slog.e(HwAudioServiceEx.TAG, "hallState is not recognized");
                    }
                }
            };
        }
        createHwAudioHandlerExThread();
        readPersistedSettingsEx(this.mContentResolver);
        setAudioSystemParameters();
        ActivityManagerEx.registerHwActivityNotifier(this.mNotifierForLifeState, "activityLifeState");
        this.mNm = (NotificationManager) this.mContext.getSystemService("notification");
        this.mCustZenModeHelper = (HwCustZenModeHelper) HwCustUtils.createObj(HwCustZenModeHelper.class, new Object[0]);
        HwCustZenModeHelper hwCustZenModeHelper = this.mCustZenModeHelper;
        if (hwCustZenModeHelper != null) {
            this.mZenModeWhiteList = hwCustZenModeHelper.getWhiteAppsInZenMode();
        }
        AudioModeClient.sHwAudioService = this;
        this.mFanAndPhoneCoverObserver.startObserving("SUBSYSTEM=hw_power");
    }

    private void createHwAudioHandlerExThread() {
        this.mHwAudioHandlerExThread = new HwAudioHandlerExThread();
        this.mHwAudioHandlerExThread.start();
        waitForHwAudioHandlerExCreation();
    }

    private void waitForHwAudioHandlerExCreation() {
        synchronized (this) {
            while (this.mHwAudioHandlerEx == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Slog.e(TAG, "Interrupted while waiting on HwAudioHandlerEx.");
                }
            }
        }
    }

    private class HwAudioHandlerExThread extends Thread {
        HwAudioHandlerExThread() {
        }

        public void run() {
            Looper.prepare();
            synchronized (HwAudioServiceEx.this) {
                HwAudioHandlerEx unused = HwAudioServiceEx.this.mHwAudioHandlerEx = new HwAudioHandlerEx();
                HwAudioServiceEx.this.notify();
            }
            Looper.loop();
        }
    }

    private void readPersistedSettingsEx(ContentResolver cr) {
        if (SOUND_EFFECTS_SUPPORT) {
            getEffectsState(cr);
        }
    }

    private void setAudioSystemParameters() {
        if (SOUND_EFFECTS_SUPPORT) {
            setEffectsState();
        }
    }

    public void setSystemReady() {
        this.mSystemReady = true;
        try {
            this.mConcurrentCaptureEnable = SystemProperties.getBoolean(CONCURRENT_CAPTURE_PROPERTY, false);
            if (mDolbyChCnt == 4) {
                this.mHwAudioHandlerEx.sendEmptyMessageDelayed(16, 1000);
            }
            getAudioService().registerRecordingCallback(this.mRecordingListener);
        } catch (Exception e) {
            Slog.e(TAG, "set drm stop prop fail");
        }
    }

    public void processAudioServerRestart() {
        Slog.i(TAG, "audioserver restart, resume audio settings and parameters");
        readPersistedSettingsEx(this.mContentResolver);
        setAudioSystemParameters();
    }

    private int identifyAudioDevice(int type) {
        int device = -1;
        if ((ANALOG_TYPEC_DEVICES & type) != 0) {
            if ("true".equals(AudioSystem.getParameters(ANALOG_TYPEC_FLAG))) {
                device = 1;
            }
        } else if (type == 67108864) {
            device = 2;
        }
        Slog.e(TAG, "identifyAudioDevice return device: " + device);
        return device;
    }

    /* access modifiers changed from: private */
    public void broadcastHiresIntent(boolean showHiResIcon) {
        if (showHiResIcon != this.mShowHiResIcon) {
            this.mShowHiResIcon = showHiResIcon;
            Intent intent = new Intent();
            intent.setAction(showHiResIcon ? SHOW_HIRES_ICON : HIDE_HIRES_ICON);
            try {
                Slog.i(TAG, "sendBroadcastAsUser broadcastHiresIntent " + showHiResIcon);
                this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, PERMISSION_HIRES_CHANGE);
            } catch (IllegalStateException e) {
                Slog.e(TAG, "broadcastHiresIntent meet exception");
            }
        }
    }

    public void notifyHiResIcon(int event) {
        if (!this.mIsDigitalTypecOn) {
            return;
        }
        if (event == 2 || event == 3 || event == 4) {
            this.mHwAudioHandlerEx.sendEmptyMessageDelayed(10, 500);
        }
    }

    public void notifyStartDolbyDms(int event) {
        if (!this.mDmsStarted && event == 2) {
            this.mHwAudioHandlerEx.sendEmptyMessageDelayed(16, 200);
        }
    }

    public void updateTypeCNotify(int type, int state, String name) {
        int recognizedDevice = identifyAudioDevice(type);
        Slog.i(TAG, "updateTypeCNotify recognizedDevice: " + recognizedDevice);
        if (state != 0) {
            this.mHwAudioHandlerEx.sendEmptyMessageDelayed(10, 1000);
            if (recognizedDevice == 1) {
                if (Settings.System.getIntForUser(this.mContext.getContentResolver(), "typec_analog_enabled", 1, -2) == 1) {
                    notifyTypecConnected(1);
                    if (!this.mIsAnalogTypecReceiverRegisterd) {
                        registerTypecReceiver(recognizedDevice);
                        this.mIsAnalogTypecReceiverRegisterd = true;
                    }
                }
                Settings.System.putIntForUser(this.mContext.getContentResolver(), "typec_digital_enabled", 1, -2);
            } else if (recognizedDevice != 2) {
                Slog.e(TAG, "updateTypeCNotify unknown device: " + recognizedDevice);
            } else {
                this.mIsDigitalTypecOn = true;
                boolean needTip = needTipForDigitalTypeC();
                Slog.i(TAG, "updateTypeCNotify needTip: " + needTip);
                if (Settings.System.getIntForUser(this.mContext.getContentResolver(), "typec_digital_enabled", 1, -2) == 1 && needTip) {
                    notifyTypecConnected(2);
                    if (!this.mIsDigitalTypecReceiverRegisterd) {
                        registerTypecReceiver(recognizedDevice);
                        this.mIsDigitalTypecReceiverRegisterd = true;
                    }
                }
                Settings.System.putIntForUser(this.mContext.getContentResolver(), "typec_analog_enabled", 1, -2);
            }
        } else {
            if (recognizedDevice == 2) {
                this.mIsDigitalTypecOn = false;
            }
            broadcastHiresIntent(false);
            Slog.i(TAG, "updateTypeCNotify plug out device: " + recognizedDevice);
            dismissNotification(recognizedDevice);
        }
        if (SWS_TYPEC_ADAPTE_SUPPORT) {
            setTypecParamAsync(state, type, name);
        }
    }

    private boolean needTipForDigitalTypeC() {
        boolean isGoodTypec = "true".equals(AudioSystem.getParameters(DIGITAL_TYPEC_FLAG));
        boolean isNeedTip = "true".equals(AudioSystem.getParameters(DIGITAL_TYPEC_REPORT_FLAG));
        if (mIsUsbPowercosumeTips) {
            return isNeedTip;
        }
        return !isGoodTypec && isNeedTip;
    }

    private void notifyTypecConnected(int device) {
        if (this.mContext == null || mNotShowTypecTip) {
            Slog.i(TAG, "context is null or mNotShowTypecTip: " + mNotShowTypecTip);
            return;
        }
        Slog.i(TAG, "notifyTypecConnected device: " + device);
        PendingIntent pi = getNeverNotifyIntent(device);
        TipRes tipRes = new TipRes(device, this.mContext);
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        this.mNotificationManager.createNotificationChannel(new NotificationChannel(tipRes.mChannelName, tipRes.mChannelName, 1));
        this.mNotificationManager.notifyAsUser(TAG, tipRes.mTypecNotificationId, new Notification.Builder(this.mContext).setSmallIcon(33751741).setContentTitle(tipRes.mTitle).setContentText(tipRes.mContent).setStyle(new Notification.BigTextStyle().bigText(tipRes.mContent)).addAction(0, tipRes.mTip, pi).setAutoCancel(true).setChannelId(tipRes.mChannelName).setDefaults(-1).build(), UserHandle.ALL);
    }

    private static class TipRes {
        public String mChannelName = null;
        public String mContent = null;
        public Context mContext = null;
        public String mTip = null;
        public String mTitle = null;
        public int mTypecNotificationId = 0;

        public TipRes(int device, Context context) {
            this.mContext = context;
            if (device == 1) {
                this.mChannelName = this.mContext.getResources().getString(33685982);
                this.mTitle = this.mContext.getResources().getString(33685983);
                this.mContent = this.mContext.getResources().getString(33685984);
                this.mTip = this.mContext.getResources().getString(33685985);
                this.mTypecNotificationId = 1;
            } else if (device != 2) {
                Slog.e(HwAudioServiceEx.TAG, "TipRes constructor unKnown device");
            } else {
                this.mChannelName = this.mContext.getResources().getString(33685982);
                if (HwAudioServiceEx.mIsUsbPowercosumeTips) {
                    Slog.i(HwAudioServiceEx.TAG, "Tips for pad, mIsUsbPowercosumeTips = " + HwAudioServiceEx.mIsUsbPowercosumeTips);
                    this.mTitle = this.mContext.getResources().getString(33686071);
                    this.mContent = this.mContext.getResources().getString(33686072);
                } else {
                    this.mTitle = this.mContext.getResources().getString(33686004);
                    this.mContent = this.mContext.getResources().getString(33686005);
                }
                this.mTip = this.mContext.getResources().getString(33685985);
                this.mTypecNotificationId = 2;
            }
        }
    }

    private PendingIntent getNeverNotifyIntent(int device) {
        Intent intent = null;
        if (device == 1) {
            intent = new Intent(ACTION_ANALOG_TYPEC_NOTIFY);
        } else if (device != 2) {
            Slog.e(TAG, "getNeverNotifyIntentAndUpdateTextId unKnown device");
        } else {
            intent = new Intent(ACTION_DIGITAL_TYPEC_NOTIFY);
        }
        return PendingIntent.getBroadcast(this.mContext, 0, intent, 268435456);
    }

    private void dismissNotification(int device) {
        NotificationManager notificationManager = this.mNotificationManager;
        if (notificationManager != null) {
            if (device == 1) {
                notificationManager.cancel(TAG, 1);
            } else if (device != 2) {
                Slog.e(TAG, "dismissNotification unKnown device: " + device);
            } else {
                notificationManager.cancel(TAG, 2);
            }
        }
    }

    private String getTypecAction(int device) {
        if (device == 1) {
            return ACTION_ANALOG_TYPEC_NOTIFY;
        }
        if (device == 2) {
            return ACTION_DIGITAL_TYPEC_NOTIFY;
        }
        Slog.e(TAG, "getTypecAction unKnown device: " + device);
        return null;
    }

    private void registerTypecReceiver(int device) {
        Slog.i(TAG, "registerTypecReceiver device: " + device);
        BroadcastReceiver typecReceiver = new BroadcastReceiver() {
            /* class com.android.server.audio.HwAudioServiceEx.AnonymousClass4 */

            public void onReceive(Context context, Intent intent) {
                if (intent == null) {
                    Slog.e(HwAudioServiceEx.TAG, "registerTypecReceiver null intent");
                    return;
                }
                String action = intent.getAction();
                if (action == null) {
                    Slog.e(HwAudioServiceEx.TAG, "registerTypecReceiver null action");
                    return;
                }
                char c = 65535;
                int hashCode = action.hashCode();
                if (hashCode != -2146785072) {
                    if (hashCode == 1549278377 && action.equals(HwAudioServiceEx.ACTION_ANALOG_TYPEC_NOTIFY)) {
                        c = 0;
                    }
                } else if (action.equals(HwAudioServiceEx.ACTION_DIGITAL_TYPEC_NOTIFY)) {
                    c = 1;
                }
                if (c == 0) {
                    Settings.System.putIntForUser(HwAudioServiceEx.this.mContext.getContentResolver(), "typec_analog_enabled", 0, -2);
                    HwAudioServiceEx.this.mNotificationManager.cancel(HwAudioServiceEx.TAG, 1);
                    boolean unused = HwAudioServiceEx.this.mIsAnalogTypecReceiverRegisterd = false;
                } else if (c != 1) {
                    Slog.e(HwAudioServiceEx.TAG, "registerTypecReceiver unKnown action");
                } else {
                    Settings.System.putIntForUser(HwAudioServiceEx.this.mContext.getContentResolver(), "typec_digital_enabled", 0, -2);
                    HwAudioServiceEx.this.mNotificationManager.cancel(HwAudioServiceEx.TAG, 2);
                    boolean unused2 = HwAudioServiceEx.this.mIsDigitalTypecReceiverRegisterd = false;
                }
                NotificationManager unused3 = HwAudioServiceEx.this.mNotificationManager = null;
                if (HwAudioServiceEx.this.mContext != null) {
                    HwAudioServiceEx.this.mContext.unregisterReceiver(this);
                }
            }
        };
        if (device == 1) {
            this.mAnalogTypecReceiver = typecReceiver;
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(getTypecAction(device));
            this.mContext.registerReceiver(this.mAnalogTypecReceiver, intentFilter);
        } else if (device != 2) {
            Slog.e(TAG, "dismissNotification unKnown device: " + device);
        } else {
            this.mDigitalTypecReceiver = typecReceiver;
            IntentFilter intentFilter2 = new IntentFilter();
            intentFilter2.addAction(getTypecAction(device));
            this.mContext.registerReceiver(this.mDigitalTypecReceiver, intentFilter2);
        }
    }

    /* access modifiers changed from: private */
    public class HwAudioHandlerEx extends Handler {
        private HwAudioHandlerEx() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 10:
                    if ("true".equals(AudioSystem.getParameters(HwAudioServiceEx.HIRES_REPORT_FLAG))) {
                        HwAudioServiceEx.this.broadcastHiresIntent(true);
                    } else {
                        HwAudioServiceEx.this.broadcastHiresIntent(false);
                    }
                    HwAudioServiceEx.this.initPowerKit();
                    return;
                case 11:
                    HwAudioServiceEx.this.setDolbyServiceClient();
                    return;
                case 12:
                    HwAudioServiceEx.this.sendDolbyUpdateBroadcast(msg.arg1, msg.arg2);
                    return;
                case 13:
                    if (msg.obj != null && (msg.obj instanceof Bundle)) {
                        Bundle b = (Bundle) msg.obj;
                        int unused = HwAudioServiceEx.this.setSoundEffectStateAsynch(b.getBoolean(HwAudioServiceEx.RESTORE), b.getString("packageName"), b.getBoolean(HwAudioServiceEx.ISONTOP), b.getString(HwAudioServiceEx.RESERVED));
                        return;
                    }
                    return;
                case 14:
                    if (msg.obj != null && (msg.obj instanceof String)) {
                        String name = (String) msg.obj;
                        if (msg.arg1 == 1) {
                            HwAudioServiceEx.this.setTypecParamToAudioSystem(name);
                            return;
                        } else {
                            HwAudioServiceEx.this.restoreTypecParam(name);
                            return;
                        }
                    } else {
                        return;
                    }
                case 15:
                    HwAudioServiceEx.this.initPowerKit();
                    return;
                case 16:
                    boolean isMusicActive = AudioSystem.isStreamActive(3, 0);
                    boolean isRingActive = AudioSystem.isStreamActive(2, 0);
                    boolean isAlarmActive = AudioSystem.isStreamActive(4, 0);
                    Slog.i(HwAudioServiceEx.TAG, "restart dms isMusicActive : " + isMusicActive + ", isRingActive : " + isRingActive + ", isAlarmActive : " + isAlarmActive);
                    if (isMusicActive || isRingActive || isAlarmActive || HwAudioServiceEx.mDolbyChCnt == 4) {
                        HwAudioServiceEx.this.setDolbyStatus();
                        return;
                    }
                    return;
                case 17:
                    HwAudioServiceEx.this.checkAndSetSoundEffectState(msg.arg1, msg.arg2);
                    return;
                case 18:
                    if (msg.obj != null && (msg.obj instanceof Bundle)) {
                        Bundle bundle = (Bundle) msg.obj;
                        HwAudioServiceEx.this.showMicSilencedToast(bundle.getString(HwAudioServiceEx.SILENCE_PKG_STR), bundle.getString(HwAudioServiceEx.ACTIVE_PKG_STR));
                        return;
                    }
                    return;
                case 19:
                    Slog.i(HwAudioServiceEx.TAG, "MSG_SEND_AUDIOKIT_BROADCAST");
                    if (msg.obj != null && (msg.obj instanceof Bundle)) {
                        HwAudioServiceEx.this.sendRecordNameBroadCastForAudioKit((Bundle) msg.obj);
                        return;
                    }
                    return;
                default:
                    Slog.i(HwAudioServiceEx.TAG, "HwAudioHandlerEx receive unknown msg");
                    return;
            }
        }
    }

    /* access modifiers changed from: private */
    public void sendRecordNameBroadCastForAudioKit(Bundle bundle) {
        Slog.i(TAG, "sendRecordNameBroadCastForAudioKit");
        if (bundle != null) {
            try {
                ArrayList<String> packageNameList = bundle.getStringArrayList(PACKAGENAME_LIST);
                ActivityInfo activityInfo = ActivityManagerEx.getLastResumedActivity();
                Slog.i(TAG, "sendRecordNameBroadCastForAudioKit, bundle not null");
                String topPackageName = null;
                if (activityInfo != null) {
                    topPackageName = activityInfo.packageName;
                    Slog.i(TAG, "sendRecordNameBroadCastForAudioKit, topPackageName");
                }
                if (packageNameList == null) {
                    Slog.i(TAG, "sendRecordNameBroadCastForAudioKit, packageNameList is null");
                    return;
                }
                Iterator<String> it = packageNameList.iterator();
                while (it.hasNext()) {
                    String packageName = it.next();
                    if (packageName != null && packageName.equals(topPackageName)) {
                        Intent intent = new Intent();
                        intent.setAction(ACTION_RECORDNAME_APP_FOR_KIT);
                        intent.putExtra("packageName", packageName);
                        if (this.mKaraokeWhiteList != null && this.mKaraokeWhiteList.contains(packageName)) {
                            Slog.i(TAG, "isKaraokeEnable");
                            intent.putExtra("isKaraokeEnable", true);
                        }
                        Slog.i(TAG, "sendBroadcastAsUser");
                        intent.setPackage(ENGINE_PACKAGE_NAME);
                        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT, PERMISSION_RECORDNAME_APP_FOR_KIT);
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                Slog.i(TAG, "HwAudioKaraokeFeature, IndexOutOfBoundsException");
            }
        }
    }

    /* access modifiers changed from: private */
    public static void sendMsgEx(Handler handler, int msg, int existingMsgPolicy, int arg1, int arg2, Object obj, int delay) {
        if (existingMsgPolicy == 0) {
            handler.removeMessages(msg);
        } else if (existingMsgPolicy == 1 && handler.hasMessages(msg)) {
            return;
        }
        handler.sendMessageAtTime(handler.obtainMessage(msg, arg1, arg2, obj), SystemClock.uptimeMillis() + ((long) delay));
    }

    private String getPackageNameByPid(int pid) {
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

    private String getApplicationLabel(String pkgName) {
        try {
            PackageManager packageManager = this.mContext.getPackageManager();
            return packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkgName, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "getApplicationLabel exception", e);
            return null;
        }
    }

    private void getAppInWhiteBlackList(List<String> whiteAppList) {
        InputStream in = null;
        XmlPullParser xmlParser = null;
        try {
            File configFile = HwCfgFilePolicy.getCfgFile(CONFIG_FILE_WHITE_BLACK_APP, 0);
            if (configFile != null) {
                Slog.v(TAG, "HwCfgFilePolicy getCfgFile not null, path = " + configFile.getPath());
                in = new FileInputStream(configFile.getPath());
                xmlParser = Xml.newPullParser();
                xmlParser.setInput(in, null);
            } else {
                Slog.e(TAG, "HwCfgFilePolicy getCfgFile is null");
            }
            if (xmlParser != null) {
                parseXmlForWhiteBlackList(xmlParser, whiteAppList);
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Slog.e(TAG, "Karaoke IO Close Fail");
                }
            }
        } catch (NoExtAPIException e2) {
            Slog.e(TAG, "Karaoke NoExtAPIException");
            if (0 != 0) {
                in.close();
            }
        } catch (FileNotFoundException e3) {
            Slog.e(TAG, "Karaoke FileNotFoundException");
            if (0 != 0) {
                in.close();
            }
        } catch (XmlPullParserException e4) {
            Slog.e(TAG, "Karaoke XmlPullParserException");
            if (0 != 0) {
                in.close();
            }
        } catch (Exception e5) {
            Slog.e(TAG, "Karaoke getAppInWhiteBlackList Exception ", e5);
            if (0 != 0) {
                in.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    in.close();
                } catch (IOException e6) {
                    Slog.e(TAG, "Karaoke IO Close Fail");
                }
            }
            throw th;
        }
    }

    private void parseXmlForWhiteBlackList(XmlPullParser parser, List<String> whiteAppList) {
        while (true) {
            try {
                int eventType = parser.next();
                if (eventType == 1) {
                    return;
                }
                if (eventType == 2 && parser.getName().equals(NODE_WHITEAPP)) {
                    String packageName = parser.getAttributeValue(null, NODE_ATTR_PACKAGE);
                    if (isValidCharSequence(packageName)) {
                        whiteAppList.add(packageName);
                    }
                }
            } catch (XmlPullParserException e) {
                Slog.e(TAG, "Karaoke XmlPullParserException");
                return;
            } catch (IOException e2) {
                Slog.e(TAG, "Karaoke IOException");
                return;
            }
        }
    }

    private boolean isValidCharSequence(CharSequence charSeq) {
        if (charSeq == null || charSeq.length() == 0) {
            return false;
        }
        return true;
    }

    private void sendStartIntentForKaraoke(String packageName, boolean isOnTop) {
        if (HW_KARAOKE_EFFECT_ENABLED) {
            if (!this.mSystemReady) {
                Slog.e(TAG, "Start Karaoke system is not ready! ");
                return;
            }
            initHwKaraokeWhiteList();
            checkKaraokeWhiteAppUIDByPkgName(packageName);
            int uid = UserHandle.getAppId(getUidByPkg(packageName));
            if (isOnTop && !packageName.equals(this.mStartedPackageName)) {
                this.mStartedPackageName = packageName;
                if (this.mKaraokeWhiteList.contains(packageName)) {
                    if ("true".equals(AudioSystem.getParameters("queryKaraokeWhitePkg=" + uid))) {
                        startIntentForKaraoke(packageName, false);
                    }
                }
            }
        }
    }

    /* JADX INFO: finally extract failed */
    private static int getCurrentUserId() {
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
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    public boolean isKaraokeWhiteListApp(String pkgName) {
        ArrayList<String> arrayList;
        if (pkgName == null || "".equals(pkgName) || (arrayList = this.mKaraokeWhiteList) == null) {
            return false;
        }
        int arraySize = arrayList.size();
        for (int i = 0; i < arraySize; i++) {
            if (pkgName.equals(this.mKaraokeWhiteList.get(i))) {
                return true;
            }
        }
        return false;
    }

    public void setKaraokeWhiteAppUIDByPkgName(String pkgName) {
        if (pkgName != null && !"".equals(pkgName)) {
            int uid = getUidByPkg(pkgName);
            if (uid == -1) {
                Slog.i(TAG, "combine white app package uid not found" + pkgName);
                return;
            }
            int uid2 = UserHandle.getAppId(uid);
            this.mKaraokeUidsMap.put(pkgName, Integer.valueOf(uid2));
            ((AudioManager) this.mContext.getSystemService("audio")).setParameters("AddKaraokeWhiteUID=" + String.valueOf(uid2));
        }
    }

    public void removeKaraokeWhiteAppUIDByPkgName(String pkgName) {
        Map<String, Integer> map;
        if (pkgName != null && !"".equals(pkgName) && (map = this.mKaraokeUidsMap) != null) {
            if (map.get(pkgName) == null) {
                Slog.e(TAG, "mKaraokeUidsMap get pkgName is null");
                return;
            }
            int uid = this.mKaraokeUidsMap.get(pkgName).intValue();
            ((AudioManager) this.mContext.getSystemService("audio")).setParameters("RemoveKaraokeWhiteUID=" + String.valueOf(uid));
            this.mKaraokeUidsMap.remove(pkgName);
        }
    }

    public void setKaraokeWhiteListUID() {
        ArrayList<String> arrayList = this.mKaraokeWhiteList;
        if (arrayList != null) {
            int arraySize = arrayList.size();
            for (int i = 0; i < arraySize; i++) {
                setKaraokeWhiteAppUIDByPkgName(this.mKaraokeWhiteList.get(i));
            }
        }
    }

    private void initHwKaraokeWhiteList() {
        if (this.mKaraokeWhiteList == null) {
            this.mKaraokeWhiteList = new ArrayList<>();
            getAppInWhiteBlackList(this.mKaraokeWhiteList);
            Slog.i(TAG, "karaoke white list =" + this.mKaraokeWhiteList.toString());
            setKaraokeWhiteListUID();
        }
    }

    private void checkKaraokeWhiteAppUIDByPkgName(String pkgName) {
        Map<String, Integer> map;
        if (!(pkgName == null || "".equals(pkgName)) && (map = this.mKaraokeUidsMap) != null && this.mKaraokeWhiteList != null && map.get(pkgName) == null && this.mKaraokeWhiteList.contains(pkgName)) {
            setKaraokeWhiteAppUIDByPkgName(pkgName);
        }
    }

    public boolean isHwKaraokeEffectEnable(String packageName) {
        if (!HW_KARAOKE_EFFECT_ENABLED) {
            Slog.e(TAG, "prop do not support");
            return false;
        } else if (!this.mSystemReady) {
            Slog.e(TAG, "Start Karaoke system is not ready! ");
            return false;
        } else {
            initHwKaraokeWhiteList();
            if (this.mKaraokeWhiteList.contains(packageName)) {
                Slog.v(TAG, "app in white list");
                return true;
            }
            Slog.v(TAG, "not in white list");
            return false;
        }
    }

    public void sendAudioRecordStateChangedIntent(String sender, int state, int pid, String packageName) {
        if (state == -1) {
            if (this.mAudioMode == 2) {
                sendMicSilencedToastMesg(packageName, PHONE_PKG);
            }
        } else if (HW_KARAOKE_EFFECT_ENABLED) {
            sendAudioRecordStateForKaraoke(sender, state, pid, packageName);
        }
    }

    private void sendAudioRecordStateForKaraoke(String sender, int state, int pid, String packageName) {
        if (this.mContext.checkCallingPermission("android.permission.RECORD_AUDIO") == -1) {
            Slog.e(TAG, "sendAudioRecordStateChangedIntent dennied from pid:" + pid);
            return;
        }
        Intent intent = new Intent();
        intent.setAction(ACTION_SEND_AUDIO_RECORD_STATE);
        intent.addFlags(268435456);
        intent.putExtra("sender", sender);
        intent.putExtra("state", state);
        intent.putExtra("packagename", packageName);
        long ident = Binder.clearCallingIdentity();
        try {
            try {
                ActivityManagerNative.getDefault().broadcastIntent((IApplicationThread) null, intent, (String) null, (IIntentReceiver) null, -1, (String) null, (Bundle) null, new String[]{PERMISSION_SEND_AUDIO_RECORD_STATE}, -1, (Bundle) null, false, false, -2);
            } catch (RemoteException e) {
            }
        } catch (RemoteException e2) {
            try {
                Slog.e(TAG, "sendAudioRecordStateChangedIntent failed: catch RemoteException!");
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                th = th;
            }
        } catch (Throwable th2) {
            th = th2;
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
        Binder.restoreCallingIdentity(ident);
    }

    public void notifySendBroadcastForKaraoke(int uid) {
        PackageManager pm;
        Context context = this.mContext;
        if (context != null && (pm = context.getPackageManager()) != null) {
            String pkgName = pm.getNameForUid(uid);
            Slog.i(TAG, "notifySendBroadCastForKaraoke, pkgName = " + pkgName);
            if (pkgName != null && !this.mAudioKitList.contains(pkgName)) {
                this.mAudioKitList.add(pkgName);
            }
            startIntentForKaraoke(pkgName, true);
        }
    }

    private void startIntentForKaraoke(String packageName, boolean isAudioKit) {
        Slog.v(TAG, "sendBroadcast : activity starting for karaoke =" + packageName);
        Intent startBroadcast = new Intent();
        startBroadcast.setAction(ACTION_START_APP_FOR_KARAOKE);
        startBroadcast.addFlags(268435456);
        startBroadcast.putExtra("packagename", packageName);
        startBroadcast.setPackage("com.huawei.android.karaoke");
        UserHandle.getAppId(getUidByPkg(packageName));
        if (isAudioKit) {
            startBroadcast.putExtra("sdkInitialize", true);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            try {
                ActivityManagerNative.getDefault().broadcastIntent((IApplicationThread) null, startBroadcast, (String) null, (IIntentReceiver) null, -1, (String) null, (Bundle) null, new String[]{PERMISSION_SEND_AUDIO_RECORD_STATE}, -1, (Bundle) null, false, false, -2);
            } catch (RemoteException e) {
                e = e;
            }
        } catch (RemoteException e2) {
            e = e2;
            try {
                Slog.e(TAG, "Karaoke broadcast fail", e);
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                th = th;
            }
        } catch (Throwable th2) {
            th = th2;
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
        Binder.restoreCallingIdentity(ident);
    }

    private void sendAppKilledIntentForKaraoke(boolean restore, String packageName, boolean isOnTop, String reserved) {
        if (HW_KARAOKE_EFFECT_ENABLED) {
            if (!this.mSystemReady) {
                Slog.e(TAG, "KILLED_APP system is not ready! ");
                return;
            }
            ArrayList<String> arrayList = this.mKaraokeWhiteList;
            if (arrayList == null || arrayList.size() < 1) {
                Slog.e(TAG, "Karaoke white list is empty! ");
                return;
            }
            int uid = UserHandle.getAppId(getUidByPkg(packageName));
            if (this.mKaraokeWhiteList.contains(packageName) || PACKAGE_PACKAGEINSTALLER.equals(packageName)) {
                if ("true".equals(AudioSystem.getParameters("queryKaraokeWhitePkg=" + uid))) {
                    Intent intent = new Intent();
                    intent.setAction(ACTION_KILLED_APP_FOR_KARAOKE);
                    intent.putExtra(RESTORE, restore);
                    intent.putExtra("packageName", packageName);
                    intent.putExtra(ISONTOP, isOnTop);
                    intent.putExtra(RESERVED, reserved);
                    this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT, PERMISSION_KILLED_APP_FOR_KARAOKE);
                }
            }
        }
    }

    private void sendAppKilledIntentForKit(boolean restore, String packageName, boolean isOnTop, String reserved) {
        if (HW_KARAOKE_EFFECT_ENABLED) {
            if (!this.mSystemReady) {
                Slog.e(TAG, "KILLED_APP system is not ready!");
                return;
            }
            boolean isKitPackageKill = true;
            boolean isKitPackageOnPause = !packageName.equals(this.mOldPackageName) && this.mAudioKitList.contains(this.mOldPackageName);
            boolean isKitPackageOnResume = !packageName.equals(this.mOldPackageName) && this.mAudioKitList.contains(packageName);
            if (!this.mAudioKitList.contains(packageName) || !restore) {
                isKitPackageKill = false;
            }
            Slog.i(TAG, "sendAppKilledIntentForKit, isKitPackageOnPause = " + isKitPackageOnPause + ", isKitPackageKill = " + isKitPackageKill + ", isKitPackageOnResume = " + isKitPackageOnResume);
            if (isKitPackageOnPause || isKitPackageKill || isKitPackageOnResume) {
                Intent intent = new Intent();
                intent.setAction(ACTION_KILLED_APP_FOR_KIT);
                intent.putExtra(RESTORE, restore);
                intent.putExtra("packageName", packageName);
                intent.putExtra(ISONTOP, isOnTop);
                intent.setPackage(ENGINE_PACKAGE_NAME);
                Slog.i(TAG, "sendAppKilledIntentForKit, isOnTop = " + isOnTop);
                this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT, PERMISSION_KILLED_APP_FOR_KIT);
            }
            this.mOldPackageName = packageName;
        }
    }

    private boolean isScreenOff() {
        PowerManager power = (PowerManager) this.mContext.getSystemService("power");
        if (power != null) {
            return !power.isScreenOn();
        }
        return false;
    }

    private boolean isKeyguardLocked() {
        KeyguardManager keyguard = (KeyguardManager) this.mContext.getSystemService("keyguard");
        if (keyguard != null) {
            return keyguard.isKeyguardLocked();
        }
        return false;
    }

    /* access modifiers changed from: private */
    public int getAppTypeForVBR(int uid) {
        String[] packages = this.mContext.getPackageManager().getPackagesForUid(uid);
        if (packages == null) {
            return -1;
        }
        int i = 0;
        while (i < packages.length) {
            try {
                return this.mPowerKit.getPkgType(this.mContext, packages[i]);
            } catch (RemoteException e) {
                Slog.i(TAG, "VBR getPkgType failed!");
                i++;
            }
        }
        return -1;
    }

    private void getEffectsState(ContentResolver contentResolver) {
        if (SWS_SOUND_EFFECTS_SUPPORT) {
            getSwsStatus(contentResolver);
        }
    }

    private void setEffectsState() {
        if (SWS_SOUND_EFFECTS_SUPPORT) {
            if (HUAWEI_SWS31_CONFIG) {
                sendSwsEQBroadcast();
            } else {
                setSwsStatus();
            }
        }
        if (this.mDmsStarted && DOLBY_SOUND_EFFECTS_SUPPORT) {
            setDolbyStatus();
        }
    }

    private void getSwsStatus(ContentResolver contentResolver) {
        this.mSwsStatus = Settings.System.getInt(contentResolver, "sws_mode", 0);
    }

    private void setSwsStatus() {
        int i = this.mSwsStatus;
        if (i == 3) {
            AudioSystem.setParameters(SWS_MODE_ON_PARA);
        } else if (i == 0) {
            AudioSystem.setParameters(SWS_MODE_OFF_PARA);
        }
    }

    private void sendSwsEQBroadcast() {
        try {
            Intent intent = new Intent(ACTION_SWS_EQ);
            intent.setPackage("com.huawei.imedia.sws");
            this.mContext.sendBroadcastAsUser(intent, UserHandle.getUserHandleForUid(UserHandle.getCallingUserId()), PERMISSION_SWS_EQ);
        } catch (Exception e) {
            Slog.e(TAG, "sendSwsEQBroadcast exception");
        }
    }

    private void onSetSoundEffectAndHSState(String packageName, boolean isOnTOP) {
        int i = 0;
        while (true) {
            String[] strArr = FORBIDDEN_SOUND_EFFECT_WHITE_LIST;
            if (i >= strArr.length) {
                return;
            }
            if (strArr[i].equals(packageName)) {
                AudioSystem.setParameters(isOnTOP ? HS_NO_CHARGE_ON : HS_NO_CHARGE_OFF);
                this.mMyHandler.sendEmptyMessage(isOnTOP ? 1 : 2);
                return;
            }
            i++;
        }
    }

    private void restoreSoundEffectAndHSState(String processName) {
        String preSWSState = null;
        String preDOLBYState = null;
        if (SWS_SOUND_EFFECTS_SUPPORT && HUAWEI_SWS31_CONFIG) {
            preSWSState = SystemProperties.get(SWS_MODE_PRESTATE, ModelBaseService.UNKONW_IDENTIFY_RET);
        }
        if (DOLBY_SOUND_EFFECTS_SUPPORT) {
            preDOLBYState = SystemProperties.get(DOLBY_MODE_PRESTATE, ModelBaseService.UNKONW_IDENTIFY_RET);
        }
        int i = 0;
        while (true) {
            String[] strArr = FORBIDDEN_SOUND_EFFECT_WHITE_LIST;
            if (i >= strArr.length) {
                return;
            }
            if (strArr[i].equals(processName)) {
                AudioSystem.setParameters(HS_NO_CHARGE_OFF);
                if (SWS_SOUND_EFFECTS_SUPPORT && HUAWEI_SWS31_CONFIG && preSWSState != null && preSWSState.equals("on")) {
                    AudioSystem.setParameters(SWS_MODE_ON_PARA);
                    SystemProperties.set(SWS_MODE_PRESTATE, ModelBaseService.UNKONW_IDENTIFY_RET);
                }
                if (DOLBY_SOUND_EFFECTS_SUPPORT && preDOLBYState != null && preDOLBYState.equals("on")) {
                    setDolbyEnable(true);
                    SystemProperties.set(DOLBY_MODE_PRESTATE, ModelBaseService.UNKONW_IDENTIFY_RET);
                    return;
                }
                return;
            }
            i++;
        }
    }

    public void onSetSoundEffectState(int device, int state) {
        sendMsgEx(this.mHwAudioHandlerEx, 17, 2, device, state, null, 0);
    }

    /* access modifiers changed from: private */
    public void checkAndSetSoundEffectState(int device, int state) {
        if (this.mIAsInner.checkAudioSettingsPermissionEx("onSetSoundEffectState()") && SOUND_EFFECTS_SUPPORT && (EFFECT_SUPPORT_DEVICE & device) != 0) {
            if (state == 1) {
                this.mSupportDevcieRef++;
                if (this.mSupportDevcieRef > 1) {
                    return;
                }
            } else {
                this.mSupportDevcieRef--;
                if (this.mSupportDevcieRef > 0) {
                    return;
                }
            }
            if (DOLBY_SOUND_EFFECTS_SUPPORT) {
                resetDolbyStateForHeadset(state);
            }
            if (!isScreenOff() && !isKeyguardLocked() && isTopActivity(FORBIDDEN_SOUND_EFFECT_WHITE_LIST)) {
                if (SWS_SOUND_EFFECTS_SUPPORT && HUAWEI_SWS31_CONFIG) {
                    onSetSwsstate(state);
                }
                if (DOLBY_SOUND_EFFECTS_SUPPORT) {
                    onSetDolbyState(state);
                }
            }
        }
    }

    private boolean isTopActivity(String[] appnames) {
        String topPackageName = getTopActivityPackageName();
        if (topPackageName == null) {
            return false;
        }
        for (String str : appnames) {
            if (str.equals(topPackageName)) {
                return true;
            }
        }
        return false;
    }

    private void onSetSwsstate(int state) {
        String preSWSState = SystemProperties.get(SWS_MODE_PRESTATE, ModelBaseService.UNKONW_IDENTIFY_RET);
        if (state == 1) {
            String curSWSState = AudioSystem.getParameters(SWS_MODE_PARA);
            if (curSWSState != null && curSWSState.contains(SWS_MODE_ON_PARA)) {
                AudioSystem.setParameters(SWS_MODE_OFF_PARA);
                SystemProperties.set(SWS_MODE_PRESTATE, "on");
            }
        } else if (state == 0 && preSWSState != null && preSWSState.equals("on")) {
            AudioSystem.setParameters(SWS_MODE_ON_PARA);
            SystemProperties.set(SWS_MODE_PRESTATE, ModelBaseService.UNKONW_IDENTIFY_RET);
        }
    }

    /* access modifiers changed from: private */
    public static int byteArrayToInt32(byte[] ba, int index) {
        return ((ba[index + 3] & HwLocalLocationProvider.LOCATION_TYPE_ACCURACY_PRIORITY) << 24) | ((ba[index + 2] & HwLocalLocationProvider.LOCATION_TYPE_ACCURACY_PRIORITY) << 16) | ((ba[index + 1] & HwLocalLocationProvider.LOCATION_TYPE_ACCURACY_PRIORITY) << 8) | (ba[index] & HwLocalLocationProvider.LOCATION_TYPE_ACCURACY_PRIORITY);
    }

    /* access modifiers changed from: private */
    public void sendDolbyUpdateBroadcast(int dlbParam, int status) {
        String str;
        Intent intent = new Intent(DOLBY_UPDATE_EVENT);
        if (dlbParam == 0) {
            str = DOLBY_UPDATE_EVENT_DS_STATE;
        } else {
            str = DOLBY_UPDATE_EVENT_PROFILE;
        }
        intent.putExtra(EVENTNAME, str);
        intent.putExtra(CMDINTVALUE, status);
        this.mContext.sendBroadcastAsUser(intent, new UserHandle(ActivityManager.getCurrentUser()), "com.huawei.permission.DOLBYCONTROL");
    }

    private void resetDolbyStateForHeadset(int headsetState) {
        this.mHeadSetPlugState = headsetState == 1;
        String str = "on";
        if (headsetState != 1) {
            if (this.mDolbyEnable <= 0) {
                str = "off";
            }
            SystemProperties.set(DOLBY_MODE_HEADSET_STATE, str);
            setDolbyEnable(true);
        } else if (!this.mDmsStarted) {
            setDolbyStatus();
        } else if (SystemProperties.get(DOLBY_MODE_HEADSET_STATE, str).equals("off")) {
            setDolbyEnable(false);
        }
    }

    private void onSetDolbyState(int state) {
        String preDolbyState = SystemProperties.get(DOLBY_MODE_PRESTATE, ModelBaseService.UNKONW_IDENTIFY_RET);
        if (state == 1) {
            if (this.mDolbyEnable == 1) {
                SystemProperties.set(DOLBY_MODE_PRESTATE, "on");
                setDolbyEnable(false);
            }
        } else if (state == 0 && preDolbyState != null && preDolbyState.equals("on")) {
            SystemProperties.set(DOLBY_MODE_HEADSET_STATE, preDolbyState);
            SystemProperties.set(DOLBY_MODE_PRESTATE, ModelBaseService.UNKONW_IDENTIFY_RET);
        }
    }

    /* access modifiers changed from: private */
    public void setDolbyEnable(boolean state) {
        if (this.mDms != null) {
            ArrayList<Byte> params = new ArrayList<>();
            if (!state) {
                int i = 0;
                while (i < 12) {
                    try {
                        params.add(Byte.valueOf(DS_DISABLE_ARRAY[i]));
                        i++;
                    } catch (Exception e) {
                        Slog.e(TAG, "setDolbyEnable exception");
                        return;
                    }
                }
                this.mDms.setDapParam(params);
                AudioSystem.setParameters(DOLBY_MODE_OFF_PARA);
                this.mDolbyEnable = 0;
                if (this.mEffect == null || !this.mEffect.hasControl()) {
                    AudioEffect dolbyEffect = new AudioEffect(EFFECT_TYPE_NULL, EFFECT_TYPE_DS, 1, 0);
                    dolbyEffect.setEnabled(false);
                    dolbyEffect.release();
                    return;
                }
                this.mEffect.setEnabled(false);
                return;
            }
            for (int i2 = 0; i2 < 12; i2++) {
                params.add(Byte.valueOf(DS_ENABLE_ARRAY[i2]));
            }
            this.mDms.setDapParam(params);
            AudioSystem.setParameters(DOLBY_MODE_ON_PARA);
            this.mDolbyEnable = 1;
            if (this.mEffect == null || !this.mEffect.hasControl()) {
                AudioEffect dolbyEffect2 = new AudioEffect(EFFECT_TYPE_NULL, EFFECT_TYPE_DS, 1, 0);
                dolbyEffect2.setEnabled(true);
                dolbyEffect2.release();
                return;
            }
            this.mEffect.setEnabled(true);
        }
    }

    private static int byteArrayToInt(Byte[] ba, int index) {
        return ((ba[index + 3].byteValue() & HwLocalLocationProvider.LOCATION_TYPE_ACCURACY_PRIORITY) << 24) | ((ba[index + 2].byteValue() & HwLocalLocationProvider.LOCATION_TYPE_ACCURACY_PRIORITY) << 16) | ((ba[index + 1].byteValue() & HwLocalLocationProvider.LOCATION_TYPE_ACCURACY_PRIORITY) << 8) | (ba[index].byteValue() & 255);
    }

    private static int int32ToByteArray(int src, Byte[] dst, int index) {
        int index2 = index + 1;
        dst[index] = Byte.valueOf((byte) (src & 255));
        int index3 = index2 + 1;
        dst[index2] = Byte.valueOf((byte) ((src >>> 8) & 255));
        dst[index3] = Byte.valueOf((byte) ((src >>> 16) & 255));
        dst[index3 + 1] = Byte.valueOf((byte) ((src >>> 24) & 255));
        return 4;
    }

    public boolean setDolbyEffect(int mode) {
        Slog.i(TAG, "setDolbyEffect1 mode = " + mode);
        if (this.mDms == null) {
            setDolbyStatus();
        }
        if (this.mDms == null) {
            return false;
        }
        ArrayList<Byte> params = new ArrayList<>();
        if (mode == 0) {
            AudioSystem.setParameters("dolbyMultich=off");
            AudioSystem.setParameters("dolby_game_mode=off");
            for (int i = 0; i < 12; i++) {
                params.add(Byte.valueOf(DS_SMART_MODE_ARRAY[i]));
            }
            this.mDms.setDapParam(params);
        } else if (mode == 1) {
            AudioSystem.setParameters("dolbyMultich=off");
            AudioSystem.setParameters("dolby_game_mode=off");
            for (int i2 = 0; i2 < 12; i2++) {
                params.add(Byte.valueOf(DS_MOVIE_MODE_ARRAY[i2]));
            }
            this.mDms.setDapParam(params);
        } else if (mode == 2) {
            AudioSystem.setParameters("dolbyMultich=off");
            AudioSystem.setParameters("dolby_game_mode=off");
            for (int i3 = 0; i3 < 12; i3++) {
                params.add(Byte.valueOf(DS_MUSIC_MODE_ARRAY[i3]));
            }
            this.mDms.setDapParam(params);
        } else if (mode == 3) {
            try {
                final Byte[] byteArray = new Byte[12];
                for (int i4 = 0; i4 < 12; i4++) {
                    byteArray[i4] = (byte) 0;
                }
                int index = 0 + int32ToByteArray(EFFECT_PARAM_EFF_PROF, byteArray, 0);
                try {
                    this.mDms.getDapParam(new ArrayList<>(Arrays.asList(byteArray)), new IDms.getDapParamCallback() {
                        /* class com.android.server.audio.HwAudioServiceEx.AnonymousClass10 */

                        @Override // vendor.huawei.hardware.dolby.dms.V1_0.IDms.getDapParamCallback
                        public void onValues(int retval, ArrayList<Byte> outVal) {
                            int i = 0;
                            Iterator<Byte> it = outVal.iterator();
                            while (it.hasNext()) {
                                byteArray[i] = it.next();
                                i++;
                            }
                        }
                    });
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to receive byte array to DMS.");
                }
                if (byteArrayToInt(byteArray, 0) != 0) {
                    return false;
                }
                AudioSystem.setParameters("dolbyMultich=on");
                for (int i5 = 0; i5 < 12; i5++) {
                    params.add(Byte.valueOf(DS_GAME_MODE_ARRAY[i5]));
                }
                this.mDms.setDapParam(params);
                AudioSystem.setParameters("dolby_game_mode=on");
            } catch (Exception e2) {
                Slog.e(TAG, "setDolbyEffect exception");
            }
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void setDolbyStatus() {
        if (DOLBY_SOUND_EFFECTS_SUPPORT) {
            Slog.d(TAG, "Init Dolby effect on system ready");
            try {
                this.mEffect = new AudioEffect(EFFECT_TYPE_NULL, EFFECT_TYPE_DS, -1, 0);
                this.mEffect.setEnabled(true);
                setDolbyServiceClient();
                if (SystemProperties.get(DOLBY_MODE_HEADSET_STATE, "unknow").equals("unknow")) {
                    SystemProperties.set(DOLBY_MODE_HEADSET_STATE, "on");
                }
            } catch (RuntimeException e) {
                Slog.e(TAG, "create dolby effect failed, fatal problem!!!");
                this.mEffect = null;
                this.mDmsStarted = false;
            }
        }
    }

    /* access modifiers changed from: private */
    public void setDolbyServiceClient() {
        try {
            if (this.mDms != null) {
                this.mDms.unlinkToDeath(this.mDmsDeathRecipient);
            }
            this.mDms = IDms.getService();
            if (this.mDms != null) {
                AudioSystem.setParameters(DOLBY_MODE_ON_PARA);
                String headSetDolbyState = SystemProperties.get(DOLBY_MODE_HEADSET_STATE, "on");
                if (!this.mHeadSetPlugState || !headSetDolbyState.equals("off")) {
                    setDolbyEnable(true);
                } else {
                    setDolbyEnable(false);
                }
                this.mDms.linkToDeath(this.mDmsDeathRecipient, (long) this.mDolbyClient.hashCode());
                this.mDms.registerClient(this.mDolbyClient, 3, this.mDolbyClient.hashCode());
                this.mDmsStarted = true;
                return;
            }
            Slog.e(TAG, "Dolby service is not ready, try to reconnect 1s later.");
            sendMsgEx(this.mHwAudioHandlerEx, 11, 0, 0, 0, null, 1000);
        } catch (Exception e) {
            Slog.e(TAG, "Connect Dolby service caught exception, try to reconnect 1s later.");
            sendMsgEx(this.mHwAudioHandlerEx, 11, 0, 0, 0, null, 1000);
        }
    }

    public void hideHiResIconDueKilledAPP(boolean killed, String packageName) {
        if (killed && packageName != null) {
            this.mHwAudioHandlerEx.sendEmptyMessageDelayed(10, 500);
        }
    }

    public int setSoundEffectState(boolean restore, String packageName, boolean isOnTop, String reserved) {
        Bundle b = new Bundle();
        b.putBoolean(RESTORE, restore);
        b.putString("packageName", packageName);
        b.putBoolean(ISONTOP, isOnTop);
        b.putString(RESERVED, reserved);
        HwAudioHandlerEx hwAudioHandlerEx = this.mHwAudioHandlerEx;
        hwAudioHandlerEx.sendMessage(hwAudioHandlerEx.obtainMessage(13, b));
        return 0;
    }

    private IAudioService getAudioService() {
        IAudioService iAudioService = this.sService;
        if (iAudioService != null) {
            return iAudioService;
        }
        this.sService = IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
        return this.sService;
    }

    public int startVirtualAudio(String deviceId, String serviceId, int serviceType, Map<String, Object> dataMap) {
        int type;
        Slog.i(TAG, "enter method startVirtualAudio");
        if (deviceId == null || serviceId == null || dataMap == null || deviceId.length() > 128 || serviceId.length() > 64) {
            Slog.e(TAG, "param of startVirtualAudio is illegal");
            return -1;
        }
        IAudioService mAudioService = getAudioService();
        if (mAudioService == null) {
            Slog.e(TAG, "mAudioService is null");
            return -1;
        }
        if (serviceType == 3) {
            type = 33554432;
        } else if (serviceType == 2) {
            type = -2130706432;
        } else {
            Slog.e(TAG, "type is illegal");
            return -1;
        }
        try {
            mAudioService.setWiredDeviceConnectionState(type, 1, serviceId, "name_out", TAG);
            return 0;
        } catch (RemoteException e) {
            Slog.e(TAG, "mAudioService is unavailable ");
            return -1;
        }
    }

    public boolean isVirtualAudio(int newDevice) {
        Slog.i(TAG, "isVirtualAudio,newDevice is:" + newDevice);
        return newDevice == 33554432;
    }

    public int removeVirtualAudio(String deviceId, String serviceId, int serviceType, Map<String, Object> dataMap) {
        int type;
        Slog.i(TAG, "enter method removeVirtualAudio");
        if (deviceId == null || serviceId == null || dataMap == null || deviceId.length() > 128 || serviceId.length() > 64) {
            Slog.e(TAG, "param of removeVirtualAudio is illegal");
            return -1;
        }
        IAudioService mAudioService = getAudioService();
        if (mAudioService == null) {
            Slog.e(TAG, "mAudioService is null");
            return -1;
        }
        if (serviceType == 3) {
            type = 33554432;
        } else if (serviceType == 2) {
            type = -2130706432;
        } else {
            Slog.e(TAG, "type is illegal");
            return -1;
        }
        try {
            mAudioService.setWiredDeviceConnectionState(type, 0, serviceId, "name_out", TAG);
            return 0;
        } catch (RemoteException e) {
            Slog.e(TAG, "mAudioService is unavailable ");
            return -1;
        }
    }

    /* access modifiers changed from: private */
    public int setSoundEffectStateAsynch(boolean restore, String packageName, boolean isOnTop, String reserved) {
        sendStartIntentForKaraoke(packageName, isOnTop);
        sendAppKilledIntentForKaraoke(restore, packageName, isOnTop, reserved);
        sendAppKilledIntentForKit(restore, packageName, isOnTop, reserved);
        if (!SOUND_EFFECTS_SUPPORT || !this.mIAsInner.checkAudioSettingsPermissionEx("setSoundEffectState()") || isScreenOff() || isKeyguardLocked()) {
            return -1;
        }
        if (!restore) {
            onSetSoundEffectAndHSState(packageName, isOnTop);
            return 0;
        }
        restoreSoundEffectAndHSState(packageName);
        return 0;
    }

    private boolean getRecordConcurrentTypeInternal(String pkgName) {
        if (pkgName == null) {
            return false;
        }
        for (String name : CONCURRENT_RECORD_OTHER) {
            if (name.equals(pkgName)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkConcurRecList(String recordApk, int apkUid) {
        boolean authenticate = false;
        try {
            PackageInfo packageInfo = this.mContext.getPackageManager().getPackageInfoAsUser(recordApk, 134217728, UserHandle.getUserId(apkUid));
            Signature[] signs = null;
            if (!(packageInfo == null || packageInfo.signingInfo == null)) {
                signs = packageInfo.signingInfo.getSigningCertificateHistory();
            }
            byte[] verifySignByte = new ConcurrentRecSignatures().getSignByPkg(recordApk);
            byte[] recordApkSignByte = null;
            if (signs != null && signs.length > 0) {
                recordApkSignByte = signs[0].toByteArray();
            }
            if (verifySignByte != null && recordApkSignByte != null && verifySignByte.length == recordApkSignByte.length && Arrays.equals(verifySignByte, recordApkSignByte)) {
                authenticate = true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "can't resolve concurrent whitelist for " + recordApk);
        }
        if (authenticate) {
            this.mPackageUidMap.put(recordApk, Integer.valueOf(apkUid));
            Slog.i(TAG, "add " + recordApk + " into conCurrent record list");
            return true;
        }
        Slog.v(TAG, "can't trust " + recordApk + " in record white list");
        return false;
    }

    public int getRecordConcurrentType(String pkgName) {
        if (!this.mConcurrentCaptureEnable || !getRecordConcurrentTypeInternal(pkgName)) {
            return 0;
        }
        int uid = Binder.getCallingUid();
        Binder.getCallingPid();
        boolean allow = true;
        if (!this.mPackageUidMap.containsKey(pkgName) || this.mPackageUidMap.get(pkgName).intValue() != uid) {
            allow = checkConcurRecList(pkgName, uid);
        }
        if (allow) {
            return RECORD_TYPE_OTHER;
        }
        return 0;
    }

    private static final class AudioModeClient implements IBinder.DeathRecipient {
        static HwAudioServiceEx sHwAudioService;
        final IAudioModeDispatcher mDispatcherCb;

        AudioModeClient(IAudioModeDispatcher pcdb) {
            this.mDispatcherCb = pcdb;
        }

        public void binderDied() {
            Log.w(HwAudioServiceEx.TAG, "client died");
            sHwAudioService.unregisterAudioModeCallback(this.mDispatcherCb);
        }

        /* access modifiers changed from: package-private */
        public boolean init() {
            Log.w(HwAudioServiceEx.TAG, "client init");
            try {
                this.mDispatcherCb.asBinder().linkToDeath(this, 0);
                return true;
            } catch (RemoteException e) {
                Log.w(HwAudioServiceEx.TAG, "Could not link to client death");
                return false;
            }
        }

        /* access modifiers changed from: package-private */
        public void release() {
            Log.w(HwAudioServiceEx.TAG, "client release");
            this.mDispatcherCb.asBinder().unlinkToDeath(this, 0);
        }
    }

    public void registerAudioModeCallback(IAudioModeDispatcher pcdb) {
        Log.i(TAG, "registerAudioModeCallback. ");
        if (pcdb != null) {
            synchronized (this.mClients) {
                AudioModeClient pmc = new AudioModeClient(pcdb);
                if (pmc.init()) {
                    this.mClients.add(pmc);
                }
            }
        }
    }

    public void unregisterAudioModeCallback(IAudioModeDispatcher pcdb) {
        Log.i(TAG, "unregisterAudioModeCallback. ");
        if (pcdb != null) {
            synchronized (this.mClients) {
                Iterator<AudioModeClient> clientIterator = this.mClients.iterator();
                while (clientIterator.hasNext()) {
                    AudioModeClient pmc = clientIterator.next();
                    if (!(pmc == null || pmc.mDispatcherCb == null || !pcdb.equals(pmc.mDispatcherCb))) {
                        pmc.release();
                        clientIterator.remove();
                    }
                }
            }
        }
    }

    public void dipatchAudioModeChanged(int actualMode) {
        Log.i(TAG, "dipatchAudioModeChanged mode = " + actualMode);
        this.mAudioMode = actualMode;
        synchronized (this.mClients) {
            if (!this.mClients.isEmpty()) {
                Iterator<AudioModeClient> clientIterator = this.mClients.iterator();
                while (clientIterator.hasNext()) {
                    AudioModeClient pmc = clientIterator.next();
                    if (pmc != null) {
                        try {
                            if (pmc.mDispatcherCb != null) {
                                pmc.mDispatcherCb.dispatchAudioModeChange(actualMode);
                            }
                        } catch (RemoteException e) {
                            Log.i(TAG, "failed to dispatch audio mode changes");
                        }
                    }
                }
            }
        }
    }

    public IBinder getDeviceSelectCallback() {
        IDeviceSelectCallback iDeviceSelectCallback = this.mcb;
        if (iDeviceSelectCallback == null) {
            return null;
        }
        return iDeviceSelectCallback.asBinder();
    }

    public boolean registerAudioDeviceSelectCallback(IBinder cb) {
        IDeviceSelectCallback iCb = IDeviceSelectCallback.Stub.asInterface(cb);
        if (iCb == null) {
            return false;
        }
        this.mcb = iCb;
        return true;
    }

    public boolean unregisterAudioDeviceSelectCallback(IBinder cb) {
        if (this.mcb != IDeviceSelectCallback.Stub.asInterface(cb)) {
            return false;
        }
        this.mcb = null;
        return true;
    }

    public int getHwSafeUsbMediaVolumeIndex() {
        return mUsbSecurityVolumeIndex * 10;
    }

    public boolean isHwSafeUsbMediaVolumeEnabled() {
        return mUsbSecurityVolumeIndex > 0;
    }

    private void setTypecParamAsync(int state, int type, String name) {
        if ((604004352 & type) != 0) {
            sendMsgEx(this.mHwAudioHandlerEx, 14, 2, state, 0, name, 0);
        }
    }

    /* access modifiers changed from: private */
    public void restoreTypecParam(String name) {
        if (name == null || name.length() <= 0) {
            Slog.e(TAG, "name is null or empty");
        } else {
            AudioSystem.setParameters("SWS_TYPEC_RESTORE=;");
        }
    }

    /* access modifiers changed from: private */
    public void setTypecParamToAudioSystem(String name) {
        if (name == null || name.length() <= 0) {
            Slog.e(TAG, "name is null or empty");
            return;
        }
        Slog.i(TAG, "setTypecParamToAudioSystem enter name:" + name);
        UsbManager usbMgr = (UsbManager) this.mContext.getSystemService("usb");
        if (usbMgr == null) {
            Slog.e(TAG, "get usb service failed");
            return;
        }
        HashMap<String, UsbDevice> deviceMap = usbMgr.getDeviceList();
        if (deviceMap == null || deviceMap.size() <= 0) {
            Slog.e(TAG, "usb list is empty");
            return;
        }
        for (Map.Entry<String, UsbDevice> entry : deviceMap.entrySet()) {
            UsbDevice device = entry.getValue();
            if (device != null && setUsbDeviceParamToAudioSystem(name, device)) {
                return;
            }
        }
    }

    private boolean setUsbDeviceParamToAudioSystem(String name, UsbDevice device) {
        String product = device.getProductName();
        String manufacture = device.getManufacturerName();
        if (product == null || name.indexOf(product) < 0 || manufacture == null) {
            return false;
        }
        AudioSystem.setParameters(("SWS_TYPEC_MANUFACTURER=" + manufacture + ";") + SWS_TYPEC_PRODUCT_NAME + "=" + product + ";");
        return true;
    }

    /* access modifiers changed from: private */
    public void initPowerKit() {
        if (this.mPowerKit == null) {
            this.mPowerKit = PowerKit.getInstance();
            if (this.mPowerKit != null) {
                Slog.i(TAG, "powerkit getInstance ok!");
                try {
                    this.mPowerKit.enableStateEvent(this.mStateRecognitionListener, 1);
                    this.mPowerKit.enableStateEvent(this.mStateRecognitionListener, 2);
                    if ((SWS_SOUND_EFFECTS_SUPPORT && SWS_VIDEO_MODE) || mDolbyChCnt == 4) {
                        this.mPowerKit.enableStateEvent(this.mStateRecognitionListener, (int) IDisplayEngineService.DE_ACTION_PG_VIDEO_FRONT);
                    }
                } catch (RemoteException | SecurityException e) {
                    Slog.w(TAG, "PG Exception: initialize powerkit error!");
                }
            } else {
                Slog.w(TAG, "powerkit getInstance fails!");
            }
        }
    }

    public boolean checkMuteZenMode() {
        String pkgName;
        String[] strArr;
        NotificationManager notificationManager = this.mNm;
        if (!(notificationManager == null || notificationManager.getZenMode() == 0 || (pkgName = getPackageNameByPid(Binder.getCallingPid())) == null || (strArr = this.mZenModeWhiteList) == null)) {
            for (String temp : strArr) {
                if (pkgName.equals(temp)) {
                    Slog.i(TAG, "need mute for " + pkgName + " under zen mode");
                    return true;
                }
            }
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isMicSource(int source) {
        if (source == 0 || source == 1 || source == 5 || source == 6 || source == 7) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isPrivacySensitiveSource(int source) {
        if (source == 5 || source == 7) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void sendMicSilencedToastMesg(String silencedPkgName, String activePkgName) {
        if (!shouldIgnoreSilenceToast(silencedPkgName)) {
            Message msg = Message.obtain();
            Bundle bundle = new Bundle();
            bundle.putString(SILENCE_PKG_STR, silencedPkgName);
            bundle.putString(ACTIVE_PKG_STR, activePkgName);
            msg.obj = bundle;
            msg.what = 18;
            this.mHwAudioHandlerEx.sendMessage(msg);
        }
    }

    /* access modifiers changed from: private */
    public void showMicSilencedToast(String silencedPkgName, String activePkgName) {
        String activePkgLabel;
        if (silencedPkgName != null && activePkgName != null) {
            String silencedPkgLabel = getApplicationLabel(silencedPkgName);
            if (!activePkgName.equals(PHONE_PKG) || this.mAudioMode != 2) {
                String activePkgLabel2 = getApplicationLabel(activePkgName);
                String string = this.mContext.getString(33685818);
                Object[] objArr = new Object[2];
                objArr[0] = activePkgLabel2 != null ? activePkgLabel2 : activePkgName;
                objArr[1] = silencedPkgLabel != null ? silencedPkgLabel : silencedPkgName;
                activePkgLabel = String.format(string, objArr);
            } else {
                String string2 = this.mContext.getString(33686170);
                Object[] objArr2 = new Object[1];
                objArr2[0] = silencedPkgLabel != null ? silencedPkgLabel : silencedPkgName;
                activePkgLabel = String.format(string2, objArr2);
            }
            ContextThemeWrapper themeContext = new ContextThemeWrapper(this.mContext, this.mContext.getResources().getIdentifier(TOAST_THEME, null, null));
            Toast toast = this.mToast;
            if (toast == null) {
                this.mToast = Toast.makeText(themeContext, activePkgLabel, 1);
                this.mToast.getWindowParams().privateFlags |= 16;
            } else {
                toast.setText(activePkgLabel);
            }
            this.mToast.show();
        }
    }

    /* access modifiers changed from: private */
    public void findLastSilenceRecord(List<AudioRecordingConfiguration> configs) {
        AudioRecordingConfiguration silencedRecord = null;
        boolean hasSilencdRecord = false;
        Iterator<AudioRecordingConfiguration> it = configs.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            AudioRecordingConfiguration arc = it.next();
            boolean found = false;
            if (arc.isClientSilenced() && isMicSource(arc.getClientAudioSource())) {
                hasSilencdRecord = true;
                Iterator<AudioRecordingConfiguration> it2 = this.mLastRecordingConfigs.iterator();
                while (true) {
                    if (!it2.hasNext()) {
                        break;
                    }
                    AudioRecordingConfiguration oldArc = it2.next();
                    if (oldArc.getClientAudioSessionId() == arc.getClientAudioSessionId()) {
                        found = true;
                        if (!oldArc.isClientSilenced()) {
                            silencedRecord = arc;
                            break;
                        }
                    }
                }
                if (silencedRecord != null) {
                    break;
                } else if (!found) {
                    silencedRecord = arc;
                    break;
                }
            } else {
                AudioRecordingConfiguration audioRecordingConfiguration = this.mLastSilenceRec;
                if (audioRecordingConfiguration != null && audioRecordingConfiguration.getClientAudioSessionId() == arc.getClientAudioSessionId()) {
                    this.mLastSilenceRec = null;
                }
            }
        }
        if (silencedRecord != null) {
            this.mLastSilenceRec = silencedRecord;
        }
        if (!hasSilencdRecord) {
            this.mLastSilenceRec = null;
        }
    }

    private boolean shouldIgnoreSilenceToast(String silencePkgName) {
        if (!TextUtils.isEmpty(silencePkgName) && !silencePkgName.contains(RECORD_REQUEST_APP_LIST[0])) {
            return false;
        }
        return true;
    }

    public boolean checkRecordActive(int requestRecordPid) {
        return false;
    }

    public boolean bypassVolumeProcessForTV(int curActiveDevice, int streamType, int direction, int flags) {
        if (curActiveDevice != 524288 && curActiveDevice != 262144) {
            return false;
        }
        Intent intent = new Intent();
        intent.setAction(ACTION_ADJUST_STREAM_VOLUME);
        intent.putExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", streamType);
        intent.putExtra(EXTRA_VOLUME_STREAM_DIRECTION, direction);
        intent.putExtra(EXTRA_VOLUME_STREAM_FLAGS, flags);
        intent.putExtra("android.media.EXTRA_VOLUME_STREAM_DEVICES", curActiveDevice);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, PERMISSION_ADJUST_STREAM_VOLUME);
        Slog.d(TAG, "bypassVolumeProcessForTV sendBroadcast adjustvolume streamType = " + streamType + " direction = " + direction);
        return true;
    }

    public boolean setFmDeviceAvailable(int state, boolean isNeedToCheckPermission) {
        if (!isNeedToCheckPermission || this.mContext.checkCallingOrSelfPermission("com.huawei.permission.ACCESS_FM") == 0) {
            Log.i(TAG, "setFmDeviceAvailable state = " + state + ", mFmDeviceOn = " + this.mFmDeviceOn);
            if (state == 0 && this.mFmDeviceOn) {
                AudioSystem.setDeviceConnectionState((int) HighBitsCompModeID.MODE_COLOR_ENHANCE, 0, "", "", 0);
                this.mFmDeviceOn = false;
            } else if (state != 1 || this.mFmDeviceOn) {
                Log.w(TAG, "failed to set FM device!");
                return false;
            } else {
                AudioSystem.setDeviceConnectionState((int) HighBitsCompModeID.MODE_COLOR_ENHANCE, 1, "", "", 0);
                this.mFmDeviceOn = true;
            }
            return true;
        }
        Log.w(TAG, "not allowed to set FM device available.");
        return false;
    }
}
