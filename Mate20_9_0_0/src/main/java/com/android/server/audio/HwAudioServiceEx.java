package com.android.server.audio;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.Notification.BigTextStyle;
import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.cover.HallState;
import android.cover.IHallCallback.Stub;
import android.cover.IHwCoverManager;
import android.media.AudioSystem;
import android.media.audiofx.AudioEffect;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IHwBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.Slog;
import android.util.Xml;
import com.android.server.pfw.autostartup.comm.XmlConst.PreciseIgnore;
import com.huawei.android.util.NoExtAPIException;
import com.huawei.cust.HwCfgFilePolicy;
import com.huawei.displayengine.IDisplayEngineService;
import com.huawei.pgmng.IPGPlugCallbacks;
import com.huawei.pgmng.PGAction;
import com.huawei.pgmng.PGPlug;
import com.huawei.pgmng.plug.PGSdk;
import com.huawei.pgmng.plug.PGSdk.Sink;
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
import java.util.UUID;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import vendor.huawei.hardware.dolby.dms.V1_0.IDms;
import vendor.huawei.hardware.dolby.dms.V1_0.IDms.getDapParamCallback;
import vendor.huawei.hardware.dolby.dms.V1_0.IDmsCallbacks;

public final class HwAudioServiceEx implements IHwAudioServiceEx {
    private static final String ACTION_ANALOG_TYPEC_NOTIFY = "ACTION_TYPEC_NONOTIFY";
    private static final String ACTION_DEVICE_OUT_USB_DEVICE_EXTEND = "huawei.intent.action.OUT_USB_DEVICE_EXTEND";
    private static final String ACTION_DIGITAL_TYPEC_NOTIFY = "ACTION_DIGITAL_TYPEC_NONOTIFY";
    private static final String ACTION_KILLED_APP_FOR_KARAOKE = "huawei.intent.action.APP_KILLED_FOR_KARAOKE_ACTION";
    private static final String ACTION_SEND_AUDIO_RECORD_STATE = "huawei.media.AUDIO_RECORD_STATE_CHANGED_ACTION";
    private static final String ACTION_START_APP_FOR_KARAOKE = "huawei.media.ACTIVITY_STARTING_FOR_KARAOKE_ACTION";
    private static final String ACTION_SWS_EQ = "huawei.intent.action.SWS_EQ";
    private static final int ANALOG_TYPEC = 1;
    private static final int ANALOG_TYPEC_CONNECTED_DISABLE = 0;
    private static final int ANALOG_TYPEC_CONNECTED_ENABLE = 1;
    private static final int ANALOG_TYPEC_CONNECTED_ID = 1;
    private static final int ANALOG_TYPEC_DEVICES = 131084;
    private static final String ANALOG_TYPEC_FLAG = "audio_capability#usb_analog_hs_report";
    private static final int BYTES_PER_INT = 4;
    private static final String CMDINTVALUE = "Integer Value";
    private static final String CONCURRENT_CAPTURE_PROPERTY = "ro.config.concurrent_capture";
    private static final String[] CONCURRENT_RECORD_OTHER = new String[]{"com.baidu.BaiduMap", "com.autonavi.minimap"};
    private static final String[] CONCURRENT_RECORD_SYSTEM = new String[]{"com.huawei.vassistant"};
    private static final String CONFIG_FILE_WHITE_BLACK_APP = "xml/hw_karaokeeffect_app_config.xml";
    private static final boolean DEBUG = SystemProperties.getBoolean("ro.media.debuggable", false);
    private static final int DIGITAL_TYPEC_CONNECTED_DISABLE = 0;
    private static final int DIGITAL_TYPEC_CONNECTED_ENABLE = 1;
    private static final int DIGITAL_TYPEC_CONNECTED_ID = 2;
    private static final String DIGITAL_TYPEC_FLAG = "typec_compatibility_check";
    private static final String DIGITAL_TYPEC_REPORT_FLAG = "audio_capability#usb_compatibility_report";
    private static final int DIGTIAL_TYPEC = 2;
    private static final String DOLBY_MODE_HEADSET_STATE = "persist.sys.dolby.state";
    private static final String DOLBY_MODE_OFF_PARA = "dolby_profile=off";
    private static final String DOLBY_MODE_ON_PARA = "dolby_profile=on";
    private static final String DOLBY_MODE_PRESTATE = "persist.sys.dolby.prestate";
    private static final String[] DOLBY_PROFILE = new String[]{"off", "smart", "movie", "music"};
    private static final boolean DOLBY_SOUND_EFFECTS_SUPPORT = SystemProperties.getBoolean("ro.config.dolby_dap", false);
    private static final String DOLBY_UPDATE_EVENT = "dolby_dap_params_update";
    private static final String DOLBY_UPDATE_EVENT_DS_STATE = "ds_state_change";
    private static final String DOLBY_UPDATE_EVENT_PROFILE = "profile_change";
    private static final String DTS_MODE_OFF_PARA = "srs_cfg:trumedia_enable=0";
    private static final String DTS_MODE_ON_PARA = "srs_cfg:trumedia_enable=1";
    private static final String DTS_MODE_PARA = "srs_cfg:trumedia_enable";
    private static final String DTS_MODE_PRESTATE = "persist.sys.dts.prestate";
    private static final int DTS_OFF = 0;
    private static final int DTS_ON = 3;
    private static final boolean DTS_SOUND_EFFECTS_SUPPORT = SystemProperties.getBoolean("ro.config.hw_dts", false);
    private static final int EFFECT_PARAM_EFF_ENAB = 16777216;
    private static final int EFFECT_PARAM_EFF_PROF = 100663296;
    private static final String EVENTNAME = "event name";
    private static final String[] FORBIDDEN_SOUND_EFFECT_WHITE_LIST = new String[]{"com.jawbone.up", "com.codoon.gps", "com.lakala.cardwatch", "com.hoolai.magic", "com.android.bankabc", "com.icbc"};
    private static final String HIDE_HIRES_ICON = "huawei.intent.action.hideHiResIcon";
    private static final String HIRES_REPORT_FLAG = "typec_need_show_hires";
    private static final String HS_NO_CHARGE_OFF = "hs_no_charge=off";
    private static final String HS_NO_CHARGE_ON = "hs_no_charge=on";
    private static final int HW_DOBBLY_SOUND_EFFECT_BIT = 4;
    private static final int HW_KARAOKE_EFFECT_BIT = 2;
    private static final boolean HW_KARAOKE_EFFECT_ENABLED = ((SystemProperties.getInt("ro.config.hw_media_flags", 0) & 2) != 0);
    private static final String ISONTOP = "isOnTop";
    private static final boolean IS_SUPPORT_SLIDE = ((SystemProperties.getInt("ro.config.hw_hall_prop", 0) & 1) != 0);
    private static final int MSG_CONNECT_DOLBY_SERVICE = 93;
    private static final int MSG_RECORD_ACTIVE = 61;
    private static final int MSG_SEND_DOLBYUPDATE_BROADCAST = 94;
    private static final int MSG_SET_SOUND_EFFECT_STATE = 95;
    private static final String NODE_ATTR_PACKAGE = "package";
    private static final String NODE_WHITEAPP = "whiteapp";
    private static final String PACKAGENAME = "packageName";
    private static final String PACKAGE_PACKAGEINSTALLER = "com.android.packageinstaller";
    private static final String PERMISSION_DEVICE_OUT_USB_DEVICE_EXTEND = "huawei.permission.OUT_USB_DEVICE_EXTEND";
    private static final String PERMISSION_HIRES_CHANGE = "huawei.permission.HIRES_ICON_CHANGE_ACTION";
    private static final String PERMISSION_KILLED_APP_FOR_KARAOKE = "huawei.permission.APP_KILLED_FOR_KARAOKE_ACTION";
    private static final String PERMISSION_SEND_AUDIO_RECORD_STATE = "com.huawei.permission.AUDIO_RECORD_STATE_CHANGED_ACTION";
    private static final String PERMISSION_START_APP_FOR_KARAOKE = "com.huawei.permission.ACTIVITY_STARTING_FOR_KARAOKE_ACTION";
    private static final String PERMISSION_SWS_EQ = "android.permission.SWS_EQ";
    private static final String[] RECORD_ACTIVE_APP_LIST = new String[]{"com.realvnc.android.remote"};
    private static final String[] RECORD_REQUEST_APP_LIST = new String[]{"com.google.android", "com.google.android.googlequicksearchbox:search", "com.google.android.googlequicksearchbox:interactor"};
    private static final int RECORD_TYPE_OTHER = 536870912;
    private static final int RECORD_TYPE_SYSTEM = 1073741824;
    private static final String RESERVED = "reserved";
    private static final String RESTORE = "restore";
    private static final int SENDMSG_NOOP = 1;
    private static final int SENDMSG_QUEUE = 2;
    private static final int SENDMSG_REPLACE = 0;
    private static final String SHOW_HIRES_ICON = "huawei.intent.action.showHiResIcon";
    private static final int SHOW_OR_HIDE_HIRES = 15;
    private static final int SHOW_OR_HIDE_HIRES_DELAY = 500;
    private static final boolean SOUND_EFFECTS_SUPPORT;
    private static final int SOUND_EFFECT_CLOSE = 1;
    private static final int SOUND_EFFECT_OPEN = 2;
    private static final String SWS_MODE_OFF_PARA = "HIFIPARA=STEREOWIDEN_Enable=false";
    private static final String SWS_MODE_ON_PARA = "HIFIPARA=STEREOWIDEN_Enable=true";
    private static final String SWS_MODE_PARA = "HIFIPARA=STEREOWIDEN_Enable";
    private static final String SWS_MODE_PRESTATE = "persist.sys.sws.prestate";
    private static final int SWS_OFF = 0;
    private static final int SWS_ON = 3;
    private static final int SWS_SCENE_AUDIO = 0;
    private static final int SWS_SCENE_VIDEO = 1;
    private static final boolean SWS_SOUND_EFFECTS_SUPPORT = SystemProperties.getBoolean("ro.config.hw_sws", false);
    private static final String SWS_VERSION = SystemProperties.get("ro.config.sws_version", "sws2");
    private static final String SYSTEMSERVER_START = "com.huawei.systemserver.START";
    private static final String TAG = "HwAudioServiceEx";
    private static final long THREE_SEC_LIMITED = 3000;
    private static final int UNKNOWN_DEVICE = -1;
    private static byte[] btArrayDsDisable = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
    private static byte[] btArrayDsEnable = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 0, (byte) 0, (byte) 0};
    private static byte[] btArrayDsGame = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 6, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 3, (byte) 0, (byte) 0, (byte) 0};
    private static byte[] btArrayDsMovie = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 6, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 0, (byte) 0, (byte) 0};
    private static byte[] btArrayDsMusic = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 6, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 2, (byte) 0, (byte) 0, (byte) 0};
    private static byte[] btArrayDsSmart = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 6, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
    private static final boolean mIsHuaweiSWS31Config;
    private static final boolean mIsSWSVideoMode;
    private static final boolean mIsUsbPowercosumeTips = SystemProperties.getBoolean("ro.config.usb_power_tips", false);
    private static int mSwsScene = 0;
    private static final int propvalue_not_support_slide = 0;
    private static final int propvalue_product_support_slide = 1;
    private static final String receiverName = "audioserver";
    private int PGSdkAppType = -1;
    int PGSdk_flag = 0;
    private IHwCoverManager coverManager;
    private long dialogShowTime = 0;
    private boolean isShowingDialog = false;
    private BroadcastReceiver mAnalogTypecReceiver = null;
    private Stub mCallback;
    private boolean mConcurrentCaptureEnable;
    private ContentResolver mContentResolver;
    private final Context mContext;
    private BroadcastReceiver mDigitalTypecReceiver = null;
    private IDms mDms;
    private DeathRecipient mDmsDeathRecipient = new DeathRecipient() {
        public void serviceDied(long cookie) {
            if (HwAudioServiceEx.this.mHwAudioHandlerEx != null) {
                Slog.e(HwAudioServiceEx.TAG, "Dolby service has died, try to reconnect 1s later.");
                HwAudioServiceEx.sendMsgEx(HwAudioServiceEx.this.mHwAudioHandlerEx, 93, 0, 0, 0, null, 1000);
            }
        }
    };
    private IDmsCallbacks mDolbyClient = new IDmsCallbacks.Stub() {
        public void onDapParamUpdate(ArrayList<Byte> params) {
            int i;
            byte[] buf = new byte[params.size()];
            for (i = 0; i < buf.length; i++) {
                buf[i] = ((Byte) params.get(i)).byteValue();
            }
            int dlbParam = HwAudioServiceEx.byteArrayToInt32(buf, 0);
            i = HwAudioServiceEx.byteArrayToInt32(buf, 4);
            String str = HwAudioServiceEx.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Got dap param update, param = ");
            stringBuilder.append(dlbParam);
            stringBuilder.append(" status = ");
            stringBuilder.append(i);
            Slog.d(str, stringBuilder.toString());
            if (dlbParam == HwAudioServiceEx.EFFECT_PARAM_EFF_ENAB || dlbParam == HwAudioServiceEx.EFFECT_PARAM_EFF_PROF) {
                if (HwAudioServiceEx.this.mSystemReady) {
                    HwAudioServiceEx.sendMsgEx(HwAudioServiceEx.this.mHwAudioHandlerEx, 94, 0, dlbParam, i, null, 1000);
                } else {
                    Slog.w(HwAudioServiceEx.TAG, "onDapParamUpdate() called before boot complete");
                }
            }
            if (dlbParam == HwAudioServiceEx.EFFECT_PARAM_EFF_ENAB) {
                HwAudioServiceEx.this.mDolbyEnable = i;
                if (HwAudioServiceEx.this.mHeadSetPlugState) {
                    SystemProperties.set(HwAudioServiceEx.DOLBY_MODE_HEADSET_STATE, HwAudioServiceEx.this.mDolbyEnable > 0 ? PreciseIgnore.COMP_SCREEN_ON_VALUE_ : "off");
                }
            }
        }
    };
    private int mDolbyEnable = 0;
    private int mDtsStatus = 0;
    private AudioEffect mEffect = null;
    private boolean mHeadSetPlugState = false;
    private HwAudioHandlerEx mHwAudioHandlerEx;
    private HwAudioHandlerExThread mHwAudioHandlerExThread;
    private IHwAudioServiceInner mIAsInner = null;
    private boolean mIsAnalogTypecNotifyAllowed = true;
    private boolean mIsAnalogTypecReceiverRegisterd = false;
    private boolean mIsDigitalTypecNotifyAllowed = true;
    private boolean mIsDigitalTypecOn = false;
    private boolean mIsDigitalTypecReceiverRegisterd = false;
    private ArrayList<String> mKaraokeWhiteList = null;
    private Handler mMyHandler = new Handler() {
        public void handleMessage(Message msg) {
            String curDTSState;
            switch (msg.what) {
                case 1:
                    if (AudioSystem.getDeviceConnectionState(8, "") == 1 || AudioSystem.getDeviceConnectionState(4, "") == 1 || AudioSystem.getDeviceConnectionState(67108864, "") == 1 || AudioSystem.getDeviceConnectionState(HwAudioServiceEx.RECORD_TYPE_OTHER, "") == 1) {
                        String str;
                        StringBuilder stringBuilder;
                        if (HwAudioServiceEx.DTS_SOUND_EFFECTS_SUPPORT) {
                            curDTSState = AudioSystem.getParameters(HwAudioServiceEx.DTS_MODE_PARA);
                            if (curDTSState != null && curDTSState.contains(HwAudioServiceEx.DTS_MODE_ON_PARA)) {
                                AudioSystem.setParameters(HwAudioServiceEx.DTS_MODE_OFF_PARA);
                                SystemProperties.set(HwAudioServiceEx.DTS_MODE_PRESTATE, PreciseIgnore.COMP_SCREEN_ON_VALUE_);
                                if (HwAudioServiceEx.DEBUG) {
                                    str = HwAudioServiceEx.TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Cur DTS mode = ");
                                    stringBuilder.append(curDTSState);
                                    stringBuilder.append(" force set DTS off");
                                    Slog.i(str, stringBuilder.toString());
                                }
                            }
                        }
                        if (HwAudioServiceEx.SWS_SOUND_EFFECTS_SUPPORT && HwAudioServiceEx.mIsHuaweiSWS31Config) {
                            curDTSState = AudioSystem.getParameters(HwAudioServiceEx.SWS_MODE_PARA);
                            if (curDTSState != null && curDTSState.contains(HwAudioServiceEx.SWS_MODE_ON_PARA)) {
                                AudioSystem.setParameters(HwAudioServiceEx.SWS_MODE_OFF_PARA);
                                SystemProperties.set(HwAudioServiceEx.SWS_MODE_PRESTATE, PreciseIgnore.COMP_SCREEN_ON_VALUE_);
                                if (HwAudioServiceEx.DEBUG) {
                                    str = HwAudioServiceEx.TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Cur SWS mode = ");
                                    stringBuilder.append(curDTSState);
                                    stringBuilder.append(" force set SWS off");
                                    Slog.i(str, stringBuilder.toString());
                                }
                            }
                        }
                        if (HwAudioServiceEx.DOLBY_SOUND_EFFECTS_SUPPORT && HwAudioServiceEx.this.mDolbyEnable > 0) {
                            SystemProperties.set(HwAudioServiceEx.DOLBY_MODE_PRESTATE, PreciseIgnore.COMP_SCREEN_ON_VALUE_);
                            HwAudioServiceEx.this.setDolbyEnable(false);
                            return;
                        }
                        return;
                    }
                    return;
                case 2:
                    if (HwAudioServiceEx.DTS_SOUND_EFFECTS_SUPPORT) {
                        curDTSState = SystemProperties.get(HwAudioServiceEx.DTS_MODE_PRESTATE, "unknown");
                        if (curDTSState != null && curDTSState.equals(PreciseIgnore.COMP_SCREEN_ON_VALUE_)) {
                            AudioSystem.setParameters(HwAudioServiceEx.DTS_MODE_ON_PARA);
                            SystemProperties.set(HwAudioServiceEx.DTS_MODE_PRESTATE, "unknown");
                            if (HwAudioServiceEx.DEBUG) {
                                Slog.i(HwAudioServiceEx.TAG, "set DTS on");
                            }
                        }
                    }
                    if (HwAudioServiceEx.SWS_SOUND_EFFECTS_SUPPORT && HwAudioServiceEx.mIsHuaweiSWS31Config) {
                        curDTSState = SystemProperties.get(HwAudioServiceEx.SWS_MODE_PRESTATE, "unknown");
                        if (curDTSState != null && curDTSState.equals(PreciseIgnore.COMP_SCREEN_ON_VALUE_)) {
                            AudioSystem.setParameters(HwAudioServiceEx.SWS_MODE_ON_PARA);
                            SystemProperties.set(HwAudioServiceEx.SWS_MODE_PRESTATE, "unknown");
                            if (HwAudioServiceEx.DEBUG) {
                                Slog.i(HwAudioServiceEx.TAG, "set SWS on");
                            }
                        }
                    }
                    if (HwAudioServiceEx.DOLBY_SOUND_EFFECTS_SUPPORT) {
                        curDTSState = SystemProperties.get(HwAudioServiceEx.DOLBY_MODE_PRESTATE, "unknown");
                        if (curDTSState != null && curDTSState.equals(PreciseIgnore.COMP_SCREEN_ON_VALUE_)) {
                            HwAudioServiceEx.this.setDolbyEnable(true);
                            SystemProperties.set(HwAudioServiceEx.DOLBY_MODE_PRESTATE, "unknown");
                            if (HwAudioServiceEx.DEBUG) {
                                Slog.i(HwAudioServiceEx.TAG, "set DOLBY on");
                                return;
                            }
                            return;
                        }
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    };
    private NotificationManager mNotificationManager = null;
    private PGSdk mPGSdk = null;
    HashMap<String, Integer> mPackageUidMap = new HashMap();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals("android.intent.action.REBOOT") || action.equals("android.intent.action.ACTION_SHUTDOWN")) {
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
                } else if (HwAudioServiceEx.IS_SUPPORT_SLIDE && action.equals(HwAudioServiceEx.SYSTEMSERVER_START)) {
                    boolean ret = HwAudioServiceEx.this.coverManager.registerHallCallback(HwAudioServiceEx.receiverName, 1, HwAudioServiceEx.this.mCallback);
                    String str = HwAudioServiceEx.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("registerHallCallback return ");
                    stringBuilder.append(ret);
                    Slog.d(str, stringBuilder.toString());
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
    private String mStartedPackageName = null;
    private Sink mStateRecognitionListener = new Sink() {
        public void onStateChanged(int stateType, int eventType, int pid, String pkg, int uid) {
            if (pid != Process.myPid() && stateType == 2) {
                String str;
                StringBuilder stringBuilder;
                if (eventType == 1) {
                    HwAudioServiceEx.this.PGSdkAppType = HwAudioServiceEx.this.getAppTypeForVBR(uid);
                    str = HwAudioServiceEx.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("VBR HwAudioService:music app enter ");
                    stringBuilder.append(HwAudioServiceEx.this.PGSdkAppType);
                    Slog.i(str, stringBuilder.toString());
                    if (HwAudioServiceEx.this.PGSdkAppType == 12) {
                        AudioSystem.setParameters("VBRMuiscState=enter");
                    } else if (HwAudioServiceEx.this.PGSdkAppType == 11) {
                        AudioSystem.setParameters("VBRIMState=enter");
                    }
                } else if (eventType == 2) {
                    HwAudioServiceEx.this.PGSdkAppType = HwAudioServiceEx.this.getAppTypeForVBR(uid);
                    str = HwAudioServiceEx.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("VBR HwAudioService:music app exit ");
                    stringBuilder.append(HwAudioServiceEx.this.PGSdkAppType);
                    Slog.i(str, stringBuilder.toString());
                    if (HwAudioServiceEx.this.PGSdkAppType == 12) {
                        AudioSystem.setParameters("VBRMuiscState=exit");
                    } else if (HwAudioServiceEx.this.PGSdkAppType == 11) {
                        AudioSystem.setParameters("VBRIMState=exit");
                    }
                }
            }
        }
    };
    private int mSwsStatus = 0;
    private boolean mSystemReady;

    private class HwAudioHandlerEx extends Handler {
        private HwAudioHandlerEx() {
        }

        /* synthetic */ HwAudioHandlerEx(HwAudioServiceEx x0, AnonymousClass1 x1) {
            this();
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 15) {
                boolean needReportHiRes = "true".equals(AudioSystem.getParameters(HwAudioServiceEx.HIRES_REPORT_FLAG));
                String str = HwAudioServiceEx.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("check HiRes Icon ");
                stringBuilder.append(needReportHiRes);
                Slog.i(str, stringBuilder.toString());
                if (HwAudioServiceEx.this.mPGSdk == null) {
                    HwAudioServiceEx.this.mPGSdk = PGSdk.getInstance();
                    if (HwAudioServiceEx.this.mPGSdk != null) {
                        Slog.i(HwAudioServiceEx.TAG, "VBR getInstance ok!");
                        HwAudioServiceEx.this.PGSdk_flag = 1;
                        try {
                            HwAudioServiceEx.this.mPGSdk.enableStateEvent(HwAudioServiceEx.this.mStateRecognitionListener, 1);
                            HwAudioServiceEx.this.mPGSdk.enableStateEvent(HwAudioServiceEx.this.mStateRecognitionListener, 2);
                        } catch (RemoteException e) {
                            Slog.i(HwAudioServiceEx.TAG, "VBR PG Exception e: initialize pgdskd error!");
                            HwAudioServiceEx.this.PGSdk_flag = 0;
                        }
                    } else {
                        Slog.i(HwAudioServiceEx.TAG, "VBR getInstance fails!");
                        HwAudioServiceEx.this.PGSdk_flag = 2;
                    }
                }
                if (needReportHiRes) {
                    HwAudioServiceEx.this.broadcastHiresIntent(true);
                } else {
                    HwAudioServiceEx.this.broadcastHiresIntent(false);
                }
            } else if (i != 61) {
                switch (i) {
                    case 93:
                        HwAudioServiceEx.this.setDolbyServiceClient();
                        break;
                    case 94:
                        HwAudioServiceEx.this.sendDolbyUpdateBroadcast(msg.arg1, msg.arg2);
                        break;
                    case 95:
                        Bundle b = msg.obj;
                        HwAudioServiceEx.this.setSoundEffectStateAsynch(b.getBoolean(HwAudioServiceEx.RESTORE), b.getString("packageName"), b.getBoolean(HwAudioServiceEx.ISONTOP), b.getString(HwAudioServiceEx.RESERVED));
                        break;
                    default:
                        Slog.i(HwAudioServiceEx.TAG, "HwAudioHandlerEx receive unknown msg");
                        break;
                }
            } else if (HwAudioServiceEx.this.isShowingDialog) {
                Slog.i(HwAudioServiceEx.TAG, "MSG_RECORD_ACTIVE should not show record warn dialog");
            } else {
                HwAudioServiceEx.this.showRecordWarnDialog(msg.arg1, msg.arg2);
            }
        }
    }

    private class HwAudioHandlerExThread extends Thread {
        HwAudioHandlerExThread() {
        }

        public void run() {
            Looper.prepare();
            synchronized (HwAudioServiceEx.this) {
                HwAudioServiceEx.this.mHwAudioHandlerEx = new HwAudioHandlerEx(HwAudioServiceEx.this, null);
                HwAudioServiceEx.this.notify();
            }
            Looper.loop();
        }
    }

    private static class IPGPClient implements IPGPlugCallbacks {
        private PGPlug mPGPlug = new PGPlug(this, HwAudioServiceEx.TAG);

        public IPGPClient() {
            new Thread(this.mPGPlug, HwAudioServiceEx.TAG).start();
        }

        public void onDaemonConnected() {
            Slog.i(HwAudioServiceEx.TAG, "HwAudioService:IPGPClient connected success!");
        }

        public boolean onEvent(int actionID, String value) {
            if (1 != PGAction.checkActionType(actionID)) {
                if (HwAudioServiceEx.DEBUG) {
                    String str = HwAudioServiceEx.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("HwAudioService:Filter application event id : ");
                    stringBuilder.append(actionID);
                    Slog.i(str, stringBuilder.toString());
                }
                return true;
            }
            int subFlag = PGAction.checkActionFlag(actionID);
            if (HwAudioServiceEx.DEBUG) {
                String str2 = HwAudioServiceEx.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("HwAudioService:IPGP onEvent actionID=");
                stringBuilder2.append(actionID);
                stringBuilder2.append(", value=");
                stringBuilder2.append(value);
                stringBuilder2.append(",  subFlag=");
                stringBuilder2.append(subFlag);
                Slog.i(str2, stringBuilder2.toString());
            }
            if (subFlag != 3) {
                if (HwAudioServiceEx.DEBUG) {
                    Slog.i(HwAudioServiceEx.TAG, "Not used non-parent scene , ignore it");
                }
                return true;
            }
            if (actionID == IDisplayEngineService.DE_ACTION_PG_VIDEO_FRONT) {
                if (HwAudioServiceEx.mSwsScene == 0) {
                    HwAudioServiceEx.mSwsScene = 1;
                    Slog.i(HwAudioServiceEx.TAG, "HwAudioService:Video_Front");
                    AudioSystem.setParameters("IPGPMode=video");
                }
            } else if (HwAudioServiceEx.mSwsScene != 0) {
                HwAudioServiceEx.mSwsScene = 0;
                Slog.i(HwAudioServiceEx.TAG, "HwAudioService:Video_Not_Front");
                AudioSystem.setParameters("IPGPMode=audio");
            }
            return true;
        }

        public void onConnectedTimeout() {
            Slog.e(HwAudioServiceEx.TAG, "HwAudioService:Client connect timeout!");
        }
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
            switch (device) {
                case 1:
                    this.mChannelName = this.mContext.getResources().getString(33685982);
                    this.mTitle = this.mContext.getResources().getString(33685983);
                    this.mContent = this.mContext.getResources().getString(33685984);
                    this.mTip = this.mContext.getResources().getString(33685985);
                    this.mTypecNotificationId = 1;
                    return;
                case 2:
                    this.mChannelName = this.mContext.getResources().getString(33685982);
                    if (HwAudioServiceEx.mIsUsbPowercosumeTips) {
                        String str = HwAudioServiceEx.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Tips for pad, mIsUsbPowercosumeTips = ");
                        stringBuilder.append(HwAudioServiceEx.mIsUsbPowercosumeTips);
                        Slog.i(str, stringBuilder.toString());
                        this.mTitle = this.mContext.getResources().getString(33686118);
                        this.mContent = this.mContext.getResources().getString(33686119);
                    } else {
                        this.mTitle = this.mContext.getResources().getString(33686004);
                        this.mContent = this.mContext.getResources().getString(33686005);
                    }
                    this.mTip = this.mContext.getResources().getString(33685985);
                    this.mTypecNotificationId = 2;
                    return;
                default:
                    Slog.e(HwAudioServiceEx.TAG, "TipRes constructor unKnown device");
                    return;
            }
        }
    }

    static {
        boolean z = false;
        boolean z2 = DTS_SOUND_EFFECTS_SUPPORT || SWS_SOUND_EFFECTS_SUPPORT || DOLBY_SOUND_EFFECTS_SUPPORT;
        SOUND_EFFECTS_SUPPORT = z2;
        z2 = "sws3".equalsIgnoreCase(SWS_VERSION) || "sws3_1".equalsIgnoreCase(SWS_VERSION) || SystemProperties.getBoolean("ro.config.sws_moviemode", false);
        mIsSWSVideoMode = z2;
        if (!("sws2".equalsIgnoreCase(SWS_VERSION) || "sws3".equalsIgnoreCase(SWS_VERSION))) {
            z = true;
        }
        mIsHuaweiSWS31Config = z;
    }

    public HwAudioServiceEx(IHwAudioServiceInner ias, Context context) {
        this.mIAsInner = ias;
        this.mContext = context;
        this.mContentResolver = this.mContext.getContentResolver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.REBOOT");
        intentFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
        intentFilter.addAction(SYSTEMSERVER_START);
        context.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, intentFilter, null, null);
        if (IS_SUPPORT_SLIDE) {
            this.coverManager = HwFrameworkFactory.getCoverManager();
            this.mCallback = new Stub() {
                public void onStateChange(HallState hallState) {
                    String str = HwAudioServiceEx.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("HallState=");
                    stringBuilder.append(hallState);
                    Slog.d(str, stringBuilder.toString());
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
        if (SWS_SOUND_EFFECTS_SUPPORT && mIsSWSVideoMode) {
            Slog.i(TAG, "HwAudioService: Start SWS3.0 IPGPClient.");
            IPGPClient iPGPClient = new IPGPClient();
        }
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
            Slog.i(TAG, "try set drm hidl stop prop");
            SystemProperties.set("odm.drm.stop", "true");
            SystemProperties.set("odm.drm.stop", "false");
            this.mConcurrentCaptureEnable = SystemProperties.getBoolean(CONCURRENT_CAPTURE_PROPERTY, false);
        } catch (Exception e) {
            Slog.e(TAG, "set drm stop prop fail");
        }
    }

    public void processAudioServerRestart() {
        Slog.i(TAG, "audioserver restart, resume audio settings and parameters");
        readPersistedSettingsEx(this.mContentResolver);
        setAudioSystemParameters();
    }

    private Intent creatIntentByMic(boolean isMic) {
        Intent intent = new Intent();
        intent.setAction(ACTION_DEVICE_OUT_USB_DEVICE_EXTEND);
        intent.putExtra("microphone", isMic);
        return intent;
    }

    private Intent getNewMicIconIntent() {
        if (this.mIAsInner.isConnectedHeadSetEx()) {
            return creatIntentByMic(true);
        }
        if (this.mIAsInner.isConnectedHeadPhoneEx()) {
            return creatIntentByMic(false);
        }
        if (!this.mIAsInner.isConnectedUsbOutDeviceEx()) {
            return null;
        }
        if (this.mIAsInner.isConnectedUsbInDeviceEx()) {
            return creatIntentByMic(true);
        }
        return creatIntentByMic(false);
    }

    private void sendNewMicIconIntent(Intent intent) {
        if (intent == null) {
            Slog.i(TAG, "intent is null");
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            ActivityManagerNative.broadcastStickyIntent(intent, PERMISSION_DEVICE_OUT_USB_DEVICE_EXTEND, -1);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void updateMicIcon() {
        sendNewMicIconIntent(getNewMicIconIntent());
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
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("identifyAudioDevice return device: ");
        stringBuilder.append(device);
        Slog.e(str, stringBuilder.toString());
        return device;
    }

    private void broadcastHiresIntent(boolean showHiResIcon) {
        Intent intent = new Intent();
        intent.setAction(showHiResIcon ? SHOW_HIRES_ICON : HIDE_HIRES_ICON);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, PERMISSION_HIRES_CHANGE);
    }

    public void notifyHiResIcon(int event) {
        if (!this.mIsDigitalTypecOn) {
            return;
        }
        if (event == 2 || event == 3 || event == 4) {
            this.mHwAudioHandlerEx.sendEmptyMessageDelayed(15, 500);
        }
    }

    public void updateTypeCNotify(int type, int state) {
        int recognizedDevice = identifyAudioDevice(type);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateTypeCNotify recognizedDevice: ");
        stringBuilder.append(recognizedDevice);
        Slog.i(str, stringBuilder.toString());
        if (state != 0) {
            this.mHwAudioHandlerEx.sendEmptyMessageDelayed(15, 1000);
            switch (recognizedDevice) {
                case 1:
                    if (System.getIntForUser(this.mContext.getContentResolver(), "typec_analog_enabled", 1, -2) == 1) {
                        notifyTypecConnected(1);
                        if (!this.mIsAnalogTypecReceiverRegisterd) {
                            registerTypecReceiver(recognizedDevice);
                            this.mIsAnalogTypecReceiverRegisterd = true;
                        }
                    }
                    System.putIntForUser(this.mContext.getContentResolver(), "typec_digital_enabled", 1, -2);
                    return;
                case 2:
                    this.mIsDigitalTypecOn = true;
                    boolean needTip = needTipForDigitalTypeC();
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("updateTypeCNotify needTip: ");
                    stringBuilder2.append(needTip);
                    Slog.i(str2, stringBuilder2.toString());
                    if (System.getIntForUser(this.mContext.getContentResolver(), "typec_digital_enabled", 1, -2) == 1 && needTip) {
                        notifyTypecConnected(2);
                        if (!this.mIsDigitalTypecReceiverRegisterd) {
                            registerTypecReceiver(recognizedDevice);
                            this.mIsDigitalTypecReceiverRegisterd = true;
                        }
                    }
                    System.putIntForUser(this.mContext.getContentResolver(), "typec_analog_enabled", 1, -2);
                    return;
                default:
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("updateTypeCNotify unknown device: ");
                    stringBuilder.append(recognizedDevice);
                    Slog.e(str, stringBuilder.toString());
                    return;
            }
        }
        if (recognizedDevice == 2) {
            this.mIsDigitalTypecOn = false;
        }
        broadcastHiresIntent(false);
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("updateTypeCNotify plug out device: ");
        stringBuilder.append(recognizedDevice);
        Slog.i(str, stringBuilder.toString());
        dismissNotification(recognizedDevice);
    }

    private boolean needTipForDigitalTypeC() {
        boolean isGoodTypec = "true".equals(AudioSystem.getParameters(DIGITAL_TYPEC_FLAG));
        boolean isNeedTip = "true".equals(AudioSystem.getParameters(DIGITAL_TYPEC_REPORT_FLAG));
        if (mIsUsbPowercosumeTips) {
            return isNeedTip;
        }
        boolean z = !isGoodTypec && isNeedTip;
        return z;
    }

    private void notifyTypecConnected(int device) {
        if (this.mContext == null) {
            Slog.i(TAG, "context is null");
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyTypecConnected device: ");
        stringBuilder.append(device);
        Slog.i(str, stringBuilder.toString());
        PendingIntent pi = getNeverNotifyIntent(device);
        TipRes tipRes = new TipRes(device, this.mContext);
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        this.mNotificationManager.createNotificationChannel(new NotificationChannel(tipRes.mChannelName, tipRes.mChannelName, 1));
        this.mNotificationManager.notifyAsUser(TAG, tipRes.mTypecNotificationId, new Builder(this.mContext).setSmallIcon(33751741).setContentTitle(tipRes.mTitle).setContentText(tipRes.mContent).setStyle(new BigTextStyle().bigText(tipRes.mContent)).addAction(0, tipRes.mTip, pi).setAutoCancel(true).setChannelId(tipRes.mChannelName).setDefaults(-1).build(), UserHandle.ALL);
    }

    private PendingIntent getNeverNotifyIntent(int device) {
        Intent intent = null;
        switch (device) {
            case 1:
                intent = new Intent(ACTION_ANALOG_TYPEC_NOTIFY);
                break;
            case 2:
                intent = new Intent(ACTION_DIGITAL_TYPEC_NOTIFY);
                break;
            default:
                Slog.e(TAG, "getNeverNotifyIntentAndUpdateTextId unKnown device");
                break;
        }
        return PendingIntent.getBroadcast(this.mContext, 0, intent, 268435456);
    }

    private void dismissNotification(int device) {
        if (this.mNotificationManager != null) {
            switch (device) {
                case 1:
                    this.mNotificationManager.cancel(TAG, 1);
                    break;
                case 2:
                    this.mNotificationManager.cancel(TAG, 2);
                    break;
                default:
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("dismissNotification unKnown device: ");
                    stringBuilder.append(device);
                    Slog.e(str, stringBuilder.toString());
                    break;
            }
        }
    }

    private String getTypecAction(int device) {
        switch (device) {
            case 1:
                return ACTION_ANALOG_TYPEC_NOTIFY;
            case 2:
                return ACTION_DIGITAL_TYPEC_NOTIFY;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getTypecAction unKnown device: ");
                stringBuilder.append(device);
                Slog.e(str, stringBuilder.toString());
                return null;
        }
    }

    private void registerTypecReceiver(int device) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("registerTypecReceiver device: ");
        stringBuilder.append(device);
        Slog.i(str, stringBuilder.toString());
        BroadcastReceiver typecReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent != null && intent.getAction() != null) {
                    String action = intent.getAction();
                    int i = -1;
                    int hashCode = action.hashCode();
                    if (hashCode != -2146785072) {
                        if (hashCode == 1549278377 && action.equals(HwAudioServiceEx.ACTION_ANALOG_TYPEC_NOTIFY)) {
                            i = 0;
                        }
                    } else if (action.equals(HwAudioServiceEx.ACTION_DIGITAL_TYPEC_NOTIFY)) {
                        i = 1;
                    }
                    switch (i) {
                        case 0:
                            System.putIntForUser(HwAudioServiceEx.this.mContext.getContentResolver(), "typec_analog_enabled", 0, -2);
                            HwAudioServiceEx.this.mNotificationManager.cancel(HwAudioServiceEx.TAG, 1);
                            HwAudioServiceEx.this.mIsAnalogTypecReceiverRegisterd = false;
                            break;
                        case 1:
                            System.putIntForUser(HwAudioServiceEx.this.mContext.getContentResolver(), "typec_digital_enabled", 0, -2);
                            HwAudioServiceEx.this.mNotificationManager.cancel(HwAudioServiceEx.TAG, 2);
                            HwAudioServiceEx.this.mIsDigitalTypecReceiverRegisterd = false;
                            break;
                        default:
                            Slog.e(HwAudioServiceEx.TAG, "registerTypecReceiver unKnown action");
                            break;
                    }
                    HwAudioServiceEx.this.mNotificationManager = null;
                    if (HwAudioServiceEx.this.mContext != null) {
                        HwAudioServiceEx.this.mContext.unregisterReceiver(this);
                    }
                }
            }
        };
        IntentFilter intentFilter;
        switch (device) {
            case 1:
                this.mAnalogTypecReceiver = typecReceiver;
                intentFilter = new IntentFilter();
                intentFilter.addAction(getTypecAction(device));
                this.mContext.registerReceiver(this.mAnalogTypecReceiver, intentFilter);
                return;
            case 2:
                this.mDigitalTypecReceiver = typecReceiver;
                intentFilter = new IntentFilter();
                intentFilter.addAction(getTypecAction(device));
                this.mContext.registerReceiver(this.mDigitalTypecReceiver, intentFilter);
                return;
            default:
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("dismissNotification unKnown device: ");
                stringBuilder2.append(device);
                Slog.e(str2, stringBuilder2.toString());
                return;
        }
    }

    private static void sendMsgEx(Handler handler, int msg, int existingMsgPolicy, int arg1, int arg2, Object obj, int delay) {
        if (existingMsgPolicy == 0) {
            handler.removeMessages(msg);
        } else if (existingMsgPolicy == 1 && handler.hasMessages(msg)) {
            return;
        }
        handler.sendMessageAtTime(handler.obtainMessage(msg, arg1, arg2, obj), SystemClock.uptimeMillis() + ((long) delay));
    }

    public boolean checkRecordActive(int requestRecordPid) {
        if (TextUtils.isEmpty(AudioSystem.getParameters("active_record_pid"))) {
            return false;
        }
        boolean isRecordOccupied;
        String requestPkgName = getPackageNameByPidEx(requestRecordPid);
        int activeRecordPid = Integer.parseInt(AudioSystem.getParameters("active_record_pid"));
        boolean z = AudioSystem.isSourceActive(1999) || AudioSystem.isSourceActive(8);
        boolean isSourceActive = z;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("AudioException checkRecordActive requestRecordPid = ");
        stringBuilder.append(requestRecordPid);
        stringBuilder.append(", activeRecordPid = ");
        stringBuilder.append(activeRecordPid);
        Slog.i(str, stringBuilder.toString());
        if (activeRecordPid == -1 || requestRecordPid == activeRecordPid || isSourceActive) {
            isRecordOccupied = false;
        } else {
            isRecordOccupied = true;
        }
        if (isRecordOccupied) {
            if (!isInRequestAppList(requestPkgName)) {
                String activePkgName = getPackageNameByPidEx(activeRecordPid);
                if (isInActiveAppList(activePkgName) || isConcurrentAllow(requestPkgName, activePkgName)) {
                    return isRecordOccupied;
                }
                sendMsgEx(this.mHwAudioHandlerEx, 61, 2, requestRecordPid, activeRecordPid, null, 0);
            }
        } else if (!(requestPkgName == null || requestPkgName.equals(""))) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("RecordCallingAppName=");
            stringBuilder2.append(requestPkgName);
            AudioSystem.setParameters(stringBuilder2.toString());
        }
        return isRecordOccupied;
    }

    private String getPackageNameByPid(int pid) {
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

    private String getPackageNameByPidEx(int pid) {
        String res = null;
        String descriptor = "android.app.IActivityManager";
        try {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInterfaceToken("android.app.IActivityManager");
            data.writeInt(pid);
            ActivityManagerNative.getDefault().asBinder().transact(504, data, reply, 0);
            reply.readException();
            res = reply.readString();
            data.recycle();
            reply.recycle();
            return res;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getPackageNameForPid ");
            stringBuilder.append(pid);
            Slog.e(str, stringBuilder.toString(), e);
            return res;
        }
    }

    private boolean isInRequestAppList(String pkgName) {
        if (TextUtils.isEmpty(pkgName)) {
            return true;
        }
        String topPackageName = getTopActivityPackageName();
        if (topPackageName != null && !pkgName.startsWith(topPackageName)) {
            return true;
        }
        boolean isValidPkgName = false;
        for (CharSequence contains : RECORD_REQUEST_APP_LIST) {
            if (pkgName.contains(contains)) {
                isValidPkgName = true;
                break;
            }
        }
        return isValidPkgName;
    }

    private boolean isInActiveAppList(String pkgName) {
        for (String equals : RECORD_ACTIVE_APP_LIST) {
            String equals2;
            if (equals2.equals(pkgName)) {
                equals2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("isInActiveAppList ");
                stringBuilder.append(pkgName);
                Slog.i(equals2, stringBuilder.toString());
                return true;
            }
        }
        return false;
    }

    private String getTopActivityPackageName() {
        try {
            List<RunningTaskInfo> tasks = ((ActivityManager) this.mContext.getSystemService("activity")).getRunningTasks(1);
            if (tasks != null) {
                if (!tasks.isEmpty()) {
                    ComponentName topActivity = ((RunningTaskInfo) tasks.get(0)).topActivity;
                    if (topActivity != null) {
                        return topActivity.getPackageName();
                    }
                    return null;
                }
            }
            return null;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failure to get topActivity PackageName ");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
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

    private void showRecordWarnDialog(int requestRecordPid, int activeRecordPid) {
        String requestPkgName = getPackageNameByPidEx(requestRecordPid);
        String requestPkgLabel = getApplicationLabel(requestPkgName);
        String activePkgName = getPackageNameByPidEx(activeRecordPid);
        String activePkgLabel = getApplicationLabel(activePkgName);
        if (activePkgName == null) {
            activePkgName = this.mContext.getString(17039914);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("showRecordWarnDialog activePkgLabel=");
        stringBuilder.append(activePkgLabel != null ? activePkgLabel : activePkgName);
        stringBuilder.append("requestPkgLabel=");
        stringBuilder.append(requestPkgLabel != null ? requestPkgLabel : requestPkgName);
        Slog.i(str, stringBuilder.toString());
        AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext, this.mContext.getResources().getIdentifier("androidhwext:style/Theme.Emui.Dialog.Alert", null, null));
        String string = this.mContext.getString(33685818);
        Object[] objArr = new Object[2];
        objArr[0] = activePkgLabel != null ? activePkgLabel : activePkgName;
        objArr[1] = requestPkgLabel != null ? requestPkgLabel : requestPkgName;
        builder.setMessage(String.format(string, objArr));
        builder.setCancelable(false);
        builder.setPositiveButton(33685817, new OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
                HwAudioServiceEx.this.isShowingDialog = false;
            }
        });
        Dialog dialog = builder.create();
        dialog.getWindow().setType(2003);
        if (SystemClock.elapsedRealtime() - this.dialogShowTime > 3000 || SystemClock.elapsedRealtime() - this.dialogShowTime < 0) {
            dialog.show();
            this.dialogShowTime = SystemClock.elapsedRealtime();
            this.isShowingDialog = true;
        }
    }

    private void getAppInWhiteBlackList(List<String> whiteAppList) {
        InputStream in = null;
        XmlPullParser xmlParser = null;
        try {
            File configFile = HwCfgFilePolicy.getCfgFile(CONFIG_FILE_WHITE_BLACK_APP, 0);
            if (configFile != null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("HwCfgFilePolicy getCfgFile not null, path = ");
                stringBuilder.append(configFile.getPath());
                Slog.v(str, stringBuilder.toString());
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
            if (in != null) {
                in.close();
            }
        } catch (FileNotFoundException e3) {
            Slog.e(TAG, "Karaoke FileNotFoundException");
            if (in != null) {
                in.close();
            }
        } catch (XmlPullParserException e4) {
            Slog.e(TAG, "Karaoke XmlPullParserException");
            if (in != null) {
                in.close();
            }
        } catch (Exception e5) {
            Slog.e(TAG, "Karaoke getAppInWhiteBlackList Exception ", e5);
            if (in != null) {
                in.close();
            }
        } catch (Throwable th) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e6) {
                    Slog.e(TAG, "Karaoke IO Close Fail");
                }
            }
        }
    }

    private void parseXmlForWhiteBlackList(XmlPullParser parser, List<String> whiteAppList) {
        while (true) {
            try {
                int next = parser.next();
                int eventType = next;
                if (next == 1) {
                    return;
                }
                if (eventType == 2 && parser.getName().equals(NODE_WHITEAPP)) {
                    String packageName = parser.getAttributeValue(null, "package");
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

    public boolean isValidCharSequence(CharSequence charSeq) {
        if (charSeq == null || charSeq.length() == 0) {
            return false;
        }
        return true;
    }

    private void sendStartIntentForKaraoke(String packageName, boolean isOnTop) {
        if (!HW_KARAOKE_EFFECT_ENABLED) {
            return;
        }
        if (this.mSystemReady) {
            initHwKaraokeWhiteList();
            if (isOnTop && !packageName.equals(this.mStartedPackageName)) {
                this.mStartedPackageName = packageName;
                if (this.mKaraokeWhiteList.contains(packageName)) {
                    startIntentForKaraoke(packageName);
                }
            }
            return;
        }
        Slog.e(TAG, "Start Karaoke system is not ready! ");
    }

    private void initHwKaraokeWhiteList() {
        if (this.mKaraokeWhiteList == null) {
            this.mKaraokeWhiteList = new ArrayList();
            getAppInWhiteBlackList(this.mKaraokeWhiteList);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("karaoke white list =");
            stringBuilder.append(this.mKaraokeWhiteList.toString());
            Slog.v(str, stringBuilder.toString());
        }
    }

    public boolean isHwKaraokeEffectEnable(String packageName) {
        if (!HW_KARAOKE_EFFECT_ENABLED) {
            Slog.e(TAG, "prop do not support");
            return false;
        } else if (this.mSystemReady) {
            initHwKaraokeWhiteList();
            if (this.mKaraokeWhiteList.contains(packageName)) {
                Slog.v(TAG, "app in white list");
                return true;
            }
            Slog.v(TAG, "not in white list");
            return false;
        } else {
            Slog.e(TAG, "Start Karaoke system is not ready! ");
            return false;
        }
    }

    public void sendAudioRecordStateChangedIntent(String sender, int state, int pid, String packageName) {
        long ident;
        Throwable th;
        String str = sender;
        int i = state;
        int i2 = pid;
        if (!HW_KARAOKE_EFFECT_ENABLED) {
            return;
        }
        String str2;
        StringBuilder stringBuilder;
        if (this.mContext.checkCallingPermission("android.permission.RECORD_AUDIO") == -1) {
            str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("sendAudioRecordStateChangedIntent dennied from ");
            stringBuilder.append(packageName);
            Slog.e(str2, stringBuilder.toString());
            return;
        }
        String str3 = packageName;
        if (DEBUG) {
            str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("sendAudioRecordStateChangedIntent=");
            stringBuilder.append(str);
            stringBuilder.append(" ");
            stringBuilder.append(i);
            stringBuilder.append(" ");
            stringBuilder.append(i2);
            Slog.i(str2, stringBuilder.toString());
        }
        Intent intent = new Intent();
        intent.setAction(ACTION_SEND_AUDIO_RECORD_STATE);
        intent.addFlags(268435456);
        intent.putExtra("sender", str);
        intent.putExtra("state", i);
        intent.putExtra("packagename", getPackageNameByPid(i2));
        long ident2 = Binder.clearCallingIdentity();
        try {
            long ident3 = ident2;
            try {
                ActivityManagerNative.getDefault().broadcastIntent(null, intent, null, null, -1, null, null, new String[]{PERMISSION_SEND_AUDIO_RECORD_STATE}, -1, null, false, false, -2);
                Binder.restoreCallingIdentity(ident3);
            } catch (RemoteException e) {
                ident = ident3;
                try {
                    Slog.e(TAG, "sendAudioRecordStateChangedIntent failed: catch RemoteException!");
                    Binder.restoreCallingIdentity(ident);
                } catch (Throwable th2) {
                    th = th2;
                    Binder.restoreCallingIdentity(ident);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                ident = ident3;
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        } catch (RemoteException e2) {
            ident = ident2;
            Slog.e(TAG, "sendAudioRecordStateChangedIntent failed: catch RemoteException!");
            Binder.restoreCallingIdentity(ident);
        } catch (Throwable th4) {
            th = th4;
            ident = ident2;
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    private void startIntentForKaraoke(String packageName) {
        RemoteException e;
        long ident;
        Throwable th;
        Intent intent;
        String str = packageName;
        String str2 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendBroadcast : activity starting for karaoke =");
        stringBuilder.append(str);
        Slog.v(str2, stringBuilder.toString());
        Intent startBroadcast = new Intent();
        startBroadcast.setAction(ACTION_START_APP_FOR_KARAOKE);
        startBroadcast.addFlags(268435456);
        startBroadcast.putExtra("packagename", str);
        startBroadcast.setPackage("com.huawei.android.karaoke");
        long ident2 = Binder.clearCallingIdentity();
        try {
            long ident3 = ident2;
            try {
                ActivityManagerNative.getDefault().broadcastIntent(null, startBroadcast, null, null, -1, null, null, new String[]{PERMISSION_SEND_AUDIO_RECORD_STATE}, -1, null, false, false, -2);
                Binder.restoreCallingIdentity(ident3);
            } catch (RemoteException e2) {
                e = e2;
                ident = ident3;
                try {
                    Slog.e(TAG, "Karaoke broadcast fail", e);
                    Binder.restoreCallingIdentity(ident);
                } catch (Throwable th2) {
                    th = th2;
                    Binder.restoreCallingIdentity(ident);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                ident = ident3;
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        } catch (RemoteException e3) {
            e = e3;
            ident = ident2;
            intent = startBroadcast;
            Slog.e(TAG, "Karaoke broadcast fail", e);
            Binder.restoreCallingIdentity(ident);
        } catch (Throwable th4) {
            th = th4;
            ident = ident2;
            intent = startBroadcast;
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    private void sendAppKilledIntentForKaraoke(boolean restore, String packageName, boolean isOnTop, String reserved) {
        if (!HW_KARAOKE_EFFECT_ENABLED) {
            return;
        }
        if (!this.mSystemReady) {
            Slog.e(TAG, "KILLED_APP system is not ready! ");
        } else if (this.mKaraokeWhiteList == null || this.mKaraokeWhiteList.size() < 1) {
            Slog.e(TAG, "Karaoke white list is empty! ");
        } else {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("restore:");
                stringBuilder.append(restore);
                stringBuilder.append(" packageName:");
                stringBuilder.append(packageName);
                stringBuilder.append(" Top:");
                stringBuilder.append(isOnTop);
                Slog.i(str, stringBuilder.toString());
            }
            if (this.mKaraokeWhiteList.contains(packageName) || PACKAGE_PACKAGEINSTALLER.equals(packageName)) {
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

    private boolean isScreenOff() {
        PowerManager power = (PowerManager) this.mContext.getSystemService("power");
        if (power != null) {
            return power.isScreenOn() ^ 1;
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

    private int getAppTypeForVBR(int uid) {
        int type = -1;
        String[] packages = this.mContext.getPackageManager().getPackagesForUid(uid);
        if (packages == null) {
            return -1;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("VBR Enter package length ");
        stringBuilder.append(packages.length);
        Slog.i(str, stringBuilder.toString());
        int i = 0;
        while (i < packages.length) {
            String packageName = packages[i];
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("VBR Enter packageName:");
            stringBuilder2.append(packageName);
            Slog.i(str2, stringBuilder2.toString());
            try {
                type = this.mPGSdk.getPkgType(this.mContext, packageName);
                break;
            } catch (RemoteException e) {
                Slog.i(TAG, "VBR getPkgType failed!");
                i++;
            }
        }
        return type;
    }

    private void getEffectsState(ContentResolver contentResolver) {
        if (DTS_SOUND_EFFECTS_SUPPORT) {
            getDtsStatus(contentResolver);
        }
        if (SWS_SOUND_EFFECTS_SUPPORT) {
            getSwsStatus(contentResolver);
        }
    }

    private void setEffectsState() {
        if (DTS_SOUND_EFFECTS_SUPPORT) {
            setDtsStatus();
        }
        if (SWS_SOUND_EFFECTS_SUPPORT) {
            if (mIsHuaweiSWS31Config) {
                sendSwsEQBroadcast();
            } else {
                setSwsStatus();
            }
        }
        if (DOLBY_SOUND_EFFECTS_SUPPORT) {
            setDolbyStatus();
        }
    }

    private void getDtsStatus(ContentResolver contentResolver) {
        this.mDtsStatus = Secure.getInt(contentResolver, "dts_mode", 0);
    }

    private void getSwsStatus(ContentResolver contentResolver) {
        this.mSwsStatus = System.getInt(contentResolver, "sws_mode", 0);
    }

    private void setDtsStatus() {
        if (this.mDtsStatus == 3) {
            AudioSystem.setParameters(DTS_MODE_ON_PARA);
            Slog.i(TAG, "setDtsStatus = on");
        } else if (this.mDtsStatus == 0) {
            AudioSystem.setParameters(DTS_MODE_OFF_PARA);
            Slog.i(TAG, "setDtsStatus = off");
        }
    }

    private void setSwsStatus() {
        if (this.mSwsStatus == 3) {
            AudioSystem.setParameters(SWS_MODE_ON_PARA);
        } else if (this.mSwsStatus == 0) {
            AudioSystem.setParameters(SWS_MODE_OFF_PARA);
        }
    }

    private void sendSwsEQBroadcast() {
        try {
            Intent intent = new Intent(ACTION_SWS_EQ);
            intent.setPackage("com.huawei.imedia.sws");
            this.mContext.sendBroadcastAsUser(intent, UserHandle.getUserHandleForUid(UserHandle.getCallingUserId()), PERMISSION_SWS_EQ);
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendSwsEQBroadcast exception: ");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        }
    }

    void onSetSoundEffectAndHSState(String packageName, boolean isOnTOP) {
        for (String equals : FORBIDDEN_SOUND_EFFECT_WHITE_LIST) {
            String equals2;
            if (equals2.equals(packageName)) {
                AudioSystem.setParameters(isOnTOP ? HS_NO_CHARGE_ON : HS_NO_CHARGE_OFF);
                this.mMyHandler.sendEmptyMessage(isOnTOP ? 1 : 2);
                if (DEBUG) {
                    equals2 = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onSetSoundEffectAndHSState message: ");
                    stringBuilder.append(isOnTOP ? "HS_NO_CHARGE_ON + SOUND_EFFECT_CLOSE" : "HS_NO_CHARGE_OFF + SOUND_EFFECT_OPEN");
                    Slog.i(equals2, stringBuilder.toString());
                }
                return;
            }
        }
    }

    void restoreSoundEffectAndHSState(String processName) {
        String preDTSState = null;
        String preSWSState = null;
        String preDOLBYState = null;
        if (DTS_SOUND_EFFECTS_SUPPORT) {
            preDTSState = SystemProperties.get(DTS_MODE_PRESTATE, "unknown");
        }
        if (SWS_SOUND_EFFECTS_SUPPORT && mIsHuaweiSWS31Config) {
            preSWSState = SystemProperties.get(SWS_MODE_PRESTATE, "unknown");
        }
        if (DOLBY_SOUND_EFFECTS_SUPPORT) {
            preDOLBYState = SystemProperties.get(DOLBY_MODE_PRESTATE, "unknown");
        }
        for (String equals : FORBIDDEN_SOUND_EFFECT_WHITE_LIST) {
            if (equals.equals(processName)) {
                AudioSystem.setParameters(HS_NO_CHARGE_OFF);
                if (DTS_SOUND_EFFECTS_SUPPORT && preDTSState != null && preDTSState.equals(PreciseIgnore.COMP_SCREEN_ON_VALUE_)) {
                    AudioSystem.setParameters(DTS_MODE_ON_PARA);
                    SystemProperties.set(DTS_MODE_PRESTATE, "unknown");
                    if (DEBUG) {
                        Slog.i(TAG, "restoreDTSAndHSState success!");
                    }
                }
                if (SWS_SOUND_EFFECTS_SUPPORT && mIsHuaweiSWS31Config && preSWSState != null && preSWSState.equals(PreciseIgnore.COMP_SCREEN_ON_VALUE_)) {
                    AudioSystem.setParameters(SWS_MODE_ON_PARA);
                    SystemProperties.set(SWS_MODE_PRESTATE, "unknown");
                    if (DEBUG) {
                        Slog.i(TAG, "restoreSWSAndHSState success!");
                    }
                }
                if (DOLBY_SOUND_EFFECTS_SUPPORT && preDOLBYState != null && preDOLBYState.equals(PreciseIgnore.COMP_SCREEN_ON_VALUE_)) {
                    setDolbyEnable(true);
                    SystemProperties.set(DOLBY_MODE_PRESTATE, "unknown");
                    if (DEBUG) {
                        Slog.i(TAG, "restoreDOLBYAndHSState success!");
                    }
                }
                return;
            }
        }
    }

    /* JADX WARNING: Missing block: B:37:0x0057, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onSetSoundEffectState(int device, int state) {
        if (!this.mIAsInner.checkAudioSettingsPermissionEx("onSetSoundEffectState()") || !SOUND_EFFECTS_SUPPORT) {
            return;
        }
        if (device == 4 || device == 8 || device == 67108864 || device == RECORD_TYPE_OTHER) {
            if (DOLBY_SOUND_EFFECTS_SUPPORT) {
                resetDolbyStateForHeadset(state);
            }
            if (!isScreenOff() && !isKeyguardLocked() && isTopActivity(FORBIDDEN_SOUND_EFFECT_WHITE_LIST)) {
                if (DTS_SOUND_EFFECTS_SUPPORT) {
                    onSetDtsState(state);
                }
                if (SWS_SOUND_EFFECTS_SUPPORT && mIsHuaweiSWS31Config) {
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
        for (String equals : appnames) {
            if (equals.equals(topPackageName)) {
                return true;
            }
        }
        return false;
    }

    private void onSetDtsState(int state) {
        String preDTSState = SystemProperties.get(DTS_MODE_PRESTATE, "unknown");
        if (state == 1) {
            String curDTSState = AudioSystem.getParameters(DTS_MODE_PARA);
            if (curDTSState != null && curDTSState.contains(DTS_MODE_ON_PARA)) {
                AudioSystem.setParameters(DTS_MODE_OFF_PARA);
                SystemProperties.set(DTS_MODE_PRESTATE, PreciseIgnore.COMP_SCREEN_ON_VALUE_);
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onSetDTSState cur DTS mode = ");
                    stringBuilder.append(curDTSState);
                    stringBuilder.append(" force set DTS off");
                    Slog.i(str, stringBuilder.toString());
                }
            }
        } else if (state == 0 && preDTSState != null && preDTSState.equals(PreciseIgnore.COMP_SCREEN_ON_VALUE_)) {
            AudioSystem.setParameters(DTS_MODE_ON_PARA);
            SystemProperties.set(DTS_MODE_PRESTATE, "unknown");
            if (DEBUG) {
                Slog.i(TAG, "onSetDTSState set DTS on");
            }
        }
    }

    private void onSetSwsstate(int state) {
        String preSWSState = SystemProperties.get(SWS_MODE_PRESTATE, "unknown");
        if (state == 1) {
            String curSWSState = AudioSystem.getParameters(SWS_MODE_PARA);
            if (curSWSState != null && curSWSState.contains(SWS_MODE_ON_PARA)) {
                AudioSystem.setParameters(SWS_MODE_OFF_PARA);
                SystemProperties.set(SWS_MODE_PRESTATE, PreciseIgnore.COMP_SCREEN_ON_VALUE_);
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onSetSwsstate cur SWS mode = ");
                    stringBuilder.append(curSWSState);
                    stringBuilder.append(" force set SWS off");
                    Slog.i(str, stringBuilder.toString());
                }
            }
        } else if (state == 0 && preSWSState != null && preSWSState.equals(PreciseIgnore.COMP_SCREEN_ON_VALUE_)) {
            AudioSystem.setParameters(SWS_MODE_ON_PARA);
            SystemProperties.set(SWS_MODE_PRESTATE, "unknown");
            if (DEBUG) {
                Slog.i(TAG, "onSetSwsstate set SWS on");
            }
        }
    }

    private static int byteArrayToInt32(byte[] ba, int index) {
        return ((((ba[index + 3] & 255) << 24) | ((ba[index + 2] & 255) << 16)) | ((ba[index + 1] & 255) << 8)) | (ba[index] & 255);
    }

    private void sendDolbyUpdateBroadcast(int dlbParam, int status) {
        Intent intent = new Intent(DOLBY_UPDATE_EVENT);
        intent.putExtra(EVENTNAME, dlbParam == EFFECT_PARAM_EFF_ENAB ? DOLBY_UPDATE_EVENT_DS_STATE : DOLBY_UPDATE_EVENT_PROFILE);
        intent.putExtra(CMDINTVALUE, status);
        this.mContext.sendBroadcastAsUser(intent, new UserHandle(ActivityManager.getCurrentUser()), "com.huawei.permission.DOLBYCONTROL");
    }

    private void resetDolbyStateForHeadset(int headsetState) {
        this.mHeadSetPlugState = headsetState == 1;
        if (headsetState != 1) {
            SystemProperties.set(DOLBY_MODE_HEADSET_STATE, this.mDolbyEnable > 0 ? PreciseIgnore.COMP_SCREEN_ON_VALUE_ : "off");
            setDolbyEnable(true);
        } else if (SystemProperties.get(DOLBY_MODE_HEADSET_STATE, PreciseIgnore.COMP_SCREEN_ON_VALUE_).equals("off")) {
            setDolbyEnable(false);
        }
    }

    private void onSetDolbyState(int state) {
        String preDolbyState = SystemProperties.get(DOLBY_MODE_PRESTATE, "unknown");
        if (state == 1) {
            if (this.mDolbyEnable == 1) {
                SystemProperties.set(DOLBY_MODE_PRESTATE, PreciseIgnore.COMP_SCREEN_ON_VALUE_);
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onSetDolbyState cur DOLBY mode = ");
                    stringBuilder.append(this.mDolbyEnable);
                    stringBuilder.append(" force set DOLBY off");
                    Slog.i(str, stringBuilder.toString());
                }
                setDolbyEnable(false);
            }
        } else if (state == 0 && preDolbyState != null && preDolbyState.equals(PreciseIgnore.COMP_SCREEN_ON_VALUE_)) {
            SystemProperties.set(DOLBY_MODE_HEADSET_STATE, preDolbyState);
            SystemProperties.set(DOLBY_MODE_PRESTATE, "unknown");
        }
    }

    private void setDolbyEnable(boolean state) {
        if (this.mDms != null) {
            ArrayList<Byte> params = new ArrayList();
            int i = 0;
            if (state) {
                while (i < 12) {
                    params.add(Byte.valueOf(btArrayDsEnable[i]));
                    i++;
                }
                this.mDms.setDapParam(params);
                AudioSystem.setParameters(DOLBY_MODE_ON_PARA);
                this.mDolbyEnable = 1;
                return;
            }
            int i2 = 0;
            while (i2 < 12) {
                try {
                    params.add(Byte.valueOf(btArrayDsDisable[i2]));
                    i2++;
                } catch (Exception e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("setDolbyEnable exception: ");
                    stringBuilder.append(e);
                    Slog.e(str, stringBuilder.toString());
                    return;
                }
            }
            this.mDms.setDapParam(params);
            AudioSystem.setParameters(DOLBY_MODE_OFF_PARA);
            this.mDolbyEnable = 0;
        }
    }

    private static int byteArrayToInt(Byte[] ba, int index) {
        return ((((ba[3 + index].byteValue() & 255) << 24) | ((ba[2 + index].byteValue() & 255) << 16)) | ((ba[1 + index].byteValue() & 255) << 8)) | (ba[index].byteValue() & 255);
    }

    private static int int32ToByteArray(int src, Byte[] dst, int index) {
        int index2 = index + 1;
        dst[index] = Byte.valueOf((byte) (src & 255));
        index = index2 + 1;
        dst[index2] = Byte.valueOf((byte) ((src >>> 8) & 255));
        index2 = index + 1;
        dst[index] = Byte.valueOf((byte) ((src >>> 16) & 255));
        dst[index2] = Byte.valueOf((byte) ((src >>> 24) & 255));
        return 4;
    }

    public boolean setDolbyEffect(int mode) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setDolbyEffect1 mode = ");
        stringBuilder.append(mode);
        Slog.i(str, stringBuilder.toString());
        int i = 0;
        if (this.mDms == null) {
            return false;
        }
        ArrayList<Byte> params = new ArrayList();
        switch (mode) {
            case 0:
                AudioSystem.setParameters("dolbyMultich=off");
                AudioSystem.setParameters("dolby_game_mode=off");
                while (i < 12) {
                    params.add(Byte.valueOf(btArrayDsSmart[i]));
                    i++;
                }
                this.mDms.setDapParam(params);
                break;
            case 1:
                AudioSystem.setParameters("dolbyMultich=off");
                AudioSystem.setParameters("dolby_game_mode=off");
                while (i < 12) {
                    params.add(Byte.valueOf(btArrayDsMovie[i]));
                    i++;
                }
                this.mDms.setDapParam(params);
                break;
            case 2:
                AudioSystem.setParameters("dolbyMultich=off");
                AudioSystem.setParameters("dolby_game_mode=off");
                while (i < 12) {
                    params.add(Byte.valueOf(btArrayDsMusic[i]));
                    i++;
                }
                this.mDms.setDapParam(params);
                break;
            case 3:
                try {
                    int i2;
                    final Byte[] byteArray = new Byte[12];
                    for (i2 = 0; i2 < 12; i2++) {
                        byteArray[i2] = Byte.valueOf((byte) 0);
                    }
                    i2 = 0 + int32ToByteArray(EFFECT_PARAM_EFF_PROF, byteArray, 0);
                    try {
                        this.mDms.getDapParam(new ArrayList(Arrays.asList(byteArray)), new getDapParamCallback() {
                            public void onValues(int retval, ArrayList<Byte> outVal) {
                                int i = 0;
                                Iterator it = outVal.iterator();
                                while (it.hasNext()) {
                                    int i2 = i + 1;
                                    byteArray[i] = (Byte) it.next();
                                    i = i2;
                                }
                            }
                        });
                    } catch (RemoteException e) {
                        Slog.e(TAG, "Failed to receive byte array to DMS.");
                    }
                    if (byteArrayToInt(byteArray, 0) == 0) {
                        AudioSystem.setParameters("dolbyMultich=on");
                        while (i < 12) {
                            params.add(Byte.valueOf(btArrayDsGame[i]));
                            i++;
                        }
                        this.mDms.setDapParam(params);
                        AudioSystem.setParameters("dolby_game_mode=on");
                        break;
                    }
                    return false;
                } catch (Exception e2) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("setDolbyEffect exception: ");
                    stringBuilder2.append(e2);
                    Slog.e(str2, stringBuilder2.toString());
                    break;
                }
        }
        return true;
    }

    private void setDolbyStatus() {
        UUID EFFECT_TYPE_NULL = UUID.fromString("ec7178ec-e5e1-4432-a3f4-4657e6795210");
        UUID EFFECT_TYPE_DS = UUID.fromString("3783c334-d3a0-4d13-874f-0032e5fb80e2");
        if ("true".equalsIgnoreCase(SystemProperties.get("ro.config.dolby_dap", "false"))) {
            Slog.d(TAG, "Init Dolby effect on system ready");
            try {
                this.mEffect = new AudioEffect(EFFECT_TYPE_NULL, EFFECT_TYPE_DS, 0, 0);
                setDolbyServiceClient();
                if (SystemProperties.get(DOLBY_MODE_HEADSET_STATE, "unknow").equals("unknow")) {
                    SystemProperties.set(DOLBY_MODE_HEADSET_STATE, PreciseIgnore.COMP_SCREEN_ON_VALUE_);
                }
            } catch (Exception e) {
                Slog.e(TAG, "create dolby effect failed, fatal problem!!!");
            }
        }
    }

    private void setDolbyServiceClient() {
        try {
            if (this.mDms != null) {
                this.mDms.unlinkToDeath(this.mDmsDeathRecipient);
            }
            this.mDms = IDms.getService();
            if (this.mDms != null) {
                this.mDms.linkToDeath(this.mDmsDeathRecipient, (long) this.mDolbyClient.hashCode());
                this.mDms.registerClient(this.mDolbyClient, 3, this.mDolbyClient.hashCode());
                this.mEffect.setEnabled(true);
                setDolbyEnable(true);
                return;
            }
            Slog.e(TAG, "Dolby service is not ready, try to reconnect 1s later.");
            sendMsgEx(this.mHwAudioHandlerEx, 93, 0, 0, 0, null, 1000);
        } catch (Exception e) {
            Slog.e(TAG, "Connect Dolby service caught exception, try to reconnect 1s later.");
            sendMsgEx(this.mHwAudioHandlerEx, 93, 0, 0, 0, null, 1000);
        }
    }

    public void hideHiResIconDueKilledAPP(boolean killed, String packageName) {
        if (killed && packageName != null) {
            this.mHwAudioHandlerEx.sendEmptyMessageDelayed(15, 500);
        }
    }

    public int setSoundEffectState(boolean restore, String packageName, boolean isOnTop, String reserved) {
        if (DEBUG) {
            Slog.i(TAG, "in setSoundEffectState");
        }
        Bundle b = new Bundle();
        b.putBoolean(RESTORE, restore);
        b.putString("packageName", packageName);
        b.putBoolean(ISONTOP, isOnTop);
        b.putString(RESERVED, reserved);
        this.mHwAudioHandlerEx.sendMessage(this.mHwAudioHandlerEx.obtainMessage(95, b));
        if (DEBUG) {
            Slog.i(TAG, "out setSoundEffectState");
        }
        return 0;
    }

    public int setSoundEffectStateAsynch(boolean restore, String packageName, boolean isOnTop, String reserved) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("restore:");
            stringBuilder.append(restore);
            stringBuilder.append(" packageName:");
            stringBuilder.append(packageName);
            stringBuilder.append(" Top:");
            stringBuilder.append(isOnTop);
            Slog.i(str, stringBuilder.toString());
        }
        sendStartIntentForKaraoke(packageName, isOnTop);
        sendAppKilledIntentForKaraoke(restore, packageName, isOnTop, reserved);
        if (!SOUND_EFFECTS_SUPPORT || !this.mIAsInner.checkAudioSettingsPermissionEx("setSoundEffectState()") || isScreenOff() || isKeyguardLocked()) {
            return -1;
        }
        if (restore) {
            restoreSoundEffectAndHSState(packageName);
        } else {
            onSetSoundEffectAndHSState(packageName, isOnTop);
        }
        return 0;
    }

    private int getRecordConcurrentTypeInternal(String pkgName) {
        if (pkgName == null) {
            return -1;
        }
        int i = 0;
        for (String name : CONCURRENT_RECORD_OTHER) {
            if (name.equals(pkgName)) {
                return RECORD_TYPE_OTHER;
            }
        }
        String[] strArr = CONCURRENT_RECORD_SYSTEM;
        int length = strArr.length;
        while (i < length) {
            if (strArr[i].equals(pkgName)) {
                return RECORD_TYPE_SYSTEM;
            }
            i++;
        }
        return -1;
    }

    private boolean isConcurrentAllow(String requestPkgName, String activePkgName) {
        if (!this.mConcurrentCaptureEnable) {
            return false;
        }
        int requestIdx = getRecordConcurrentTypeInternal(requestPkgName);
        int activeIdx = getRecordConcurrentTypeInternal(activePkgName);
        boolean res = false;
        if ((requestIdx == RECORD_TYPE_OTHER && activeIdx == RECORD_TYPE_SYSTEM) || (requestIdx == RECORD_TYPE_SYSTEM && activeIdx == RECORD_TYPE_OTHER && this.mPackageUidMap.containsKey(requestPkgName) && this.mPackageUidMap.containsKey(activePkgName))) {
            res = true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ConcurrentAllow result ");
        stringBuilder.append(res);
        stringBuilder.append(" with [pkg:");
        stringBuilder.append(requestPkgName);
        stringBuilder.append(" whitelist:");
        stringBuilder.append(requestIdx);
        stringBuilder.append("] and [pkg:");
        stringBuilder.append(activePkgName);
        stringBuilder.append(" whitelist:");
        stringBuilder.append(activeIdx);
        stringBuilder.append("]");
        Slog.i(str, stringBuilder.toString());
        return res;
    }

    private boolean checkConcurRecList(String recordApk, int concurrentType) {
        String str;
        StringBuilder stringBuilder;
        boolean authenticate = false;
        if (concurrentType == RECORD_TYPE_OTHER) {
            try {
                PackageInfo packageInfo = this.mContext.getPackageManager().getPackageInfo(recordApk, 134217728);
                Signature[] signs = null;
                if (!(packageInfo == null || packageInfo.signingInfo == null)) {
                    signs = packageInfo.signingInfo.getSigningCertificateHistory();
                }
                byte[] verifySignByte = new ConcurrentRecSignatures().getSignByPkg(recordApk);
                byte[] recordApkSignByte = null;
                if (signs != null) {
                    recordApkSignByte = signs[0].toByteArray();
                }
                if (verifySignByte != null && recordApkSignByte != null && verifySignByte.length == recordApkSignByte.length && Arrays.equals(verifySignByte, recordApkSignByte)) {
                    authenticate = true;
                }
            } catch (NameNotFoundException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("can't resolve concurrent whitelist ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        }
        if (concurrentType == RECORD_TYPE_SYSTEM || authenticate) {
            try {
                int uid = this.mContext.getPackageManager().getPackageUidAsUser(recordApk, 0);
                if (uid > 0) {
                    this.mPackageUidMap.put(recordApk, Integer.valueOf(uid));
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("add ");
                    stringBuilder.append(recordApk);
                    stringBuilder.append(" into conCurrent record list");
                    Slog.i(str, stringBuilder.toString());
                    return true;
                }
            } catch (NameNotFoundException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("can't find apkinfo for ");
                stringBuilder.append(recordApk);
                Slog.e(str, stringBuilder.toString());
            }
        } else {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("can't trust ");
            stringBuilder2.append(recordApk);
            stringBuilder2.append(" in record white list");
            Slog.v(str2, stringBuilder2.toString());
        }
        return false;
    }

    public int getRecordConcurrentType(String pkgName) {
        if (!this.mConcurrentCaptureEnable) {
            return 0;
        }
        int concurrentType = getRecordConcurrentTypeInternal(pkgName);
        if (concurrentType == -1) {
            return 0;
        }
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        boolean allow = true;
        if (!(this.mPackageUidMap.containsKey(pkgName) && ((Integer) this.mPackageUidMap.get(pkgName)).intValue() == uid)) {
            allow = checkConcurRecList(pkgName, concurrentType);
        }
        if (!allow) {
            return 0;
        }
        int activeRecordPid = Integer.parseInt(AudioSystem.getParameters("active_record_pid"));
        String str;
        StringBuilder stringBuilder;
        if (activeRecordPid == -1) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Allow concurrent capture first for ");
            stringBuilder.append(pkgName);
            Slog.i(str, stringBuilder.toString());
            return concurrentType;
        } else if (activeRecordPid == pid && concurrentType == RECORD_TYPE_SYSTEM) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Allow concurrent capture self for ");
            stringBuilder.append(pkgName);
            Slog.i(str, stringBuilder.toString());
            return concurrentType;
        } else if (isConcurrentAllow(pkgName, getPackageNameByPidEx(activeRecordPid))) {
            return concurrentType;
        } else {
            return 0;
        }
    }
}
