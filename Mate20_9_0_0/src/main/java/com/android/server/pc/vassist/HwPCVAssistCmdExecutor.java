package com.android.server.pc.vassist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.HwPCUtils;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent.PointerCoords;
import com.android.server.LocalServices;
import com.android.server.am.HwActivityManagerService;
import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.input.HwInputManagerService.HwInputManagerLocalService;
import com.android.server.pc.HwPCManagerService;
import com.android.server.pc.HwPCMkManager;
import com.android.server.rms.iaware.appmng.AwareIntelligentRecg;
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.android.server.rms.shrinker.ProcessStopShrinker;
import com.huawei.android.app.HwActivityManager;
import com.huawei.android.view.HwWindowManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class HwPCVAssistCmdExecutor {
    private static final String BROADCAST_NOTIFY_DESKTOP_MODE = "com.android.server.pc.action.desktop_mode";
    private static final String BROADCAST_WPS_OPEN_DOC = "com.huawei.audio.assist.action_open";
    private static final String BROADCAST_WPS_OPEN_DOC_RESULT = "com.huawei.audio.assist.action_open_result";
    private static final int CMD_BACK_APP = 5;
    private static final int CMD_CLOSE_APP = 1;
    private static final int CMD_FULL_SCREEN_APP = 4;
    private static final int CMD_INVALID = -1;
    private static final int CMD_MAX_APP = 3;
    private static final int CMD_MIN_APP = 2;
    private static final int CMD_NEXT_PAGE = 9;
    private static final int CMD_OPEN_DOC = 7;
    private static final int CMD_PREV_PAGE = 8;
    private static final int CMD_SHOW_HOME = 6;
    private static final int CMD_START_APP = 0;
    private static final int CMD_START_PLAY = 10;
    private static final int CMD_STOP_PLAY = 11;
    private static final String COLUME_CMD_CLOUD = "contexts";
    private static final String COLUME_CMD_TERMINAL = "command";
    private static final String COLUME_CMD_UNWAKED = "wake";
    static final int FLAG_FROM_CLOUD = 0;
    static final int FLAG_FROM_TERMINAL = 1;
    static final int FLAG_FROM_UNWAKED = 2;
    static final int FLAG_LAST = 2;
    private static final int IDX_NEXT_PAGE = 4;
    private static final int IDX_NEXT_PAGE1 = 5;
    private static final int IDX_NEXT_PAGE2 = 6;
    private static final int IDX_NEXT_PAGE3 = 7;
    private static final int IDX_PLAY_PPT = 8;
    private static final int IDX_PLAY_PPT1 = 9;
    private static final int IDX_PREV_PAGE = 0;
    private static final int IDX_PREV_PAGE1 = 1;
    private static final int IDX_PREV_PAGE2 = 2;
    private static final int IDX_PREV_PAGE3 = 3;
    static final int ID_VASSIST_SERVICE = 272;
    private static final int INDEX_INITIATED = 0;
    private static final int INDEX_LAST_NEXT_PAGE = 7;
    private static final int INDEX_LAST_PREV_PAGE = 3;
    private static final int INDEX_LAST_START_PLAY = 9;
    private static final String JSON_KEY_ACTION = "action";
    private static final String JSON_KEY_CMD = "intent";
    private static final String JSON_KEY_EXTRA = "ext";
    private static final String JSON_KEY_LABEL = "label";
    private static final String JSON_KEY_NAME = "name";
    private static final String JSON_KEY_NUMBER = "number";
    private static final String JSON_KEY_ORIG_VALUE = "origValue";
    private static final String JSON_KEY_PAY_LOAD = "payload";
    private static final String JSON_KEY_PKG_NAME = "packageName";
    private static final String JSON_KEY_SLOTS = "slots";
    private static final String JSON_KEY_SLOTS_APP = "targetApp";
    private static final String JSON_KEY_SLOTS_SCREEN = "targetScreen";
    private static final String JSON_KEY_SLOTS_SCREEN_EXT = "externalDisplay";
    private static final String JSON_KEY_SLOTS_SCREEN_PHONE = "phone";
    private static final String JSON_KEY_SLOTS_SEQ = "targetSeq";
    private static final String JSON_KEY_SYNONYM_WORD = "synonymWord";
    private static final String JSON_KEY_VALUE = "value";
    private static final int JSON_VALUE_DEFAULT_DISPLAY = 0;
    private static final int JSON_VALUE_EXT_DISPLAY = 1;
    private static final int MSG_DESKTOP_MODE_CHANGED = 3;
    private static final int MSG_INVALID = -1;
    private static final int MSG_LOAD_RES = 1;
    private static final int MSG_RESULT_OPEN_DOC = 2;
    private static final int MSG_VOICE_CMD = 0;
    private static final String PERMISSION_BROADCAST_VASSIST_DESKTOP = "com.huawei.permission.VASSIST_DESKTOP";
    private static final String PERMISSION_BROADCAST_VASSIST_DESKTOP_WPS = "com.huawei.permission.VASSIST_DESKTOP_WPS";
    static final int RESULT_CANNOT_DO_IT = 3;
    static final int RESULT_CANNOT_FOUND_APP = 4;
    static final int RESULT_FAILED = 1;
    static final int RESULT_MAX_FULLSC_CANNOT_LARGER = 6;
    static final int RESULT_MIN_MAX_FULLSC_NOT_OPERATION = 5;
    static final int RESULT_NOT_IN_DESKTOP_MODE = 2;
    static final int RESULT_OPEN_DOC_NOT_FOUND = 7;
    static final int RESULT_SUCCESS = 0;
    static final int RESULT_SUCCESS_BACK_SHOWHOME = -4;
    static final int RESULT_SUCCESS_CLOSE_APP = -2;
    static final int RESULT_SUCCESS_MIN_MAX_APP = -3;
    static final int RESULT_SUCCESS_PAGE_UP_DOWN_FULLSCN = -6;
    static final int RESULT_SUCCESS_START_OPEN = -1;
    static final int RESULT_SUCCESS_START_PLAY = -5;
    private static final String[] SPECIAL_APPS_FOR_PAGE = new String[]{"com.android.gallery3d", "cn.wps.moffice_eng", "com.kingsoft.moffice_pro_hw", "com.microsoft.office.officehub"};
    private static final String[] SPECIAL_APPS_FOR_PPT = new String[]{"cn.wps.moffice_eng", "com.kingsoft.moffice_pro_hw", "com.microsoft.office.officehub"};
    private static final String TAG = "HwPCVAssistCmdExecutor";
    private HwActivityManagerService mAMS;
    final List<String> mAppStartSuccStrs = new ArrayList();
    private HwPCVAssistAppStarter mAppStarter;
    private volatile int mCastingDisplayId = -1;
    final List<String> mCloseSuccResultStrs = new ArrayList();
    private Context mContext;
    private volatile boolean mDesktopMode = false;
    private LocalHandler mHandler;
    private HandlerThread mHandlerThread;
    final List<String> mNotConnDisplayResultStrs = new ArrayList();
    private OpenDocResultReceiver mOpenDocResultReceiver;
    final List<String> mOpenDocResultStrs = new ArrayList();
    private int mReplayCount = 0;
    private HwPCManagerService mService;
    final List<String> mStartPlayResultStrs = new ArrayList();
    private HashMap<String, Integer> mUnWakedStrs = new HashMap();
    private HashMap<String, Integer> mWakedCmdsMap = new HashMap();

    private class LocalHandler extends Handler {
        private VoiceCmd mVoiceCmd = null;

        public VoiceCmd getVoiceCmdForOpenDoc() {
            return this.mVoiceCmd;
        }

        public void setVoiceCmdForOpenDoc(VoiceCmd cmd) {
            this.mVoiceCmd = cmd;
        }

        public LocalHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    boolean desktopMode = HwPCVAssistCmdExecutor.this.mDesktopMode;
                    int displayId = HwPCVAssistCmdExecutor.this.mCastingDisplayId;
                    String str = HwPCVAssistCmdExecutor.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("MSG_VOICE_CMD displayId = ");
                    stringBuilder.append(displayId);
                    stringBuilder.append(", desktopMode = ");
                    stringBuilder.append(desktopMode);
                    HwPCUtils.log(str, stringBuilder.toString());
                    VoiceCmd voiceCmd = HwPCVAssistCmdExecutor.this.resolveJsonCmd(msg, displayId);
                    if (voiceCmd == null) {
                        HwPCUtils.log(HwPCVAssistCmdExecutor.TAG, "MSG_VOICE_CMD refuse to exec voice command");
                        return;
                    } else if (!desktopMode || !HwPCUtils.isValidExtDisplayId(displayId)) {
                        HwPCUtils.log(HwPCVAssistCmdExecutor.TAG, "MSG_VOICE_CMD is not in desktop mode or is invalid displayid, abort this command");
                        HwPCVAssistCmdExecutor.this.replyResultToVAssist(2, voiceCmd);
                        return;
                    } else if (voiceCmd.cmd == -1) {
                        HwPCUtils.log(HwPCVAssistCmdExecutor.TAG, "MSG_VOICE_CMD invalid cmd");
                        HwPCVAssistCmdExecutor.this.replyResultToVAssist(3, voiceCmd);
                        return;
                    } else {
                        HwPCVAssistCmdExecutor.this.execVoiceCmdImpl(voiceCmd);
                        return;
                    }
                case 1:
                    HwPCVAssistCmdExecutor.this.loadStrings();
                    return;
                case 2:
                    HwPCVAssistCmdExecutor.this.replyResultOfOpenDoc(msg);
                    return;
                default:
                    return;
            }
        }
    }

    private class OpenDocResultReceiver extends BroadcastReceiver {
        private OpenDocResultReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                HwPCUtils.log(HwPCVAssistCmdExecutor.TAG, "OpenDocReusltReceiver received a null intent");
                return;
            }
            if (HwPCVAssistCmdExecutor.BROADCAST_WPS_OPEN_DOC_RESULT.equals(intent.getAction())) {
                int result = intent.getIntExtra("result", 0);
                int sessionId = intent.getIntExtra("session_id", -1);
                String str = HwPCVAssistCmdExecutor.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("receive: BROADCAST_WPS_OPEN_DOC_RESULT, result = ");
                stringBuilder.append(result);
                stringBuilder.append(", sessionId = ");
                stringBuilder.append(sessionId);
                HwPCUtils.log(str, stringBuilder.toString());
                HwPCVAssistCmdExecutor.this.mHandler.removeMessages(2);
                Message msg = HwPCVAssistCmdExecutor.this.mHandler.obtainMessage(2);
                msg.arg1 = result;
                msg.arg2 = sessionId;
                HwPCVAssistCmdExecutor.this.mHandler.sendMessage(msg);
            }
        }
    }

    static class VoiceCmd {
        String action;
        int castingDisplay = 0;
        int cmd = -1;
        String extra;
        int flag;
        Messenger messenger;
        String pkgName;
        int sessionId;
        int targetDisplay = 0;

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("cmd = ");
            sb.append(this.cmd);
            sb.append(", sessionId = ");
            sb.append(this.sessionId);
            sb.append(", pkgName = ");
            sb.append(this.pkgName);
            sb.append(", action = ");
            sb.append(this.action);
            sb.append(", extra = ");
            sb.append(this.extra);
            sb.append(", flag = ");
            sb.append(this.flag);
            sb.append(", targetDisplay = ");
            sb.append(this.targetDisplay);
            sb.append(", castingDisplay = ");
            sb.append(this.castingDisplay);
            return sb.toString();
        }
    }

    public static final class WindowStateData {
        public Rect bounds;
        public String pkgName;
        public boolean topWinApp;

        public WindowStateData() {
            reset();
        }

        public void reset() {
            this.pkgName = null;
            this.topWinApp = true;
            this.bounds = null;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("pkgName = ");
            sb.append(this.pkgName);
            sb.append(", topWinApp = ");
            sb.append(this.topWinApp);
            sb.append(", bounds = ");
            sb.append(this.bounds);
            return sb.toString();
        }
    }

    public HwPCVAssistCmdExecutor(Context context, HwPCManagerService service, HwActivityManagerService ams) {
        createLocalizedContext(context, Locale.SIMPLIFIED_CHINESE);
        this.mService = service;
        this.mAMS = ams;
        this.mHandlerThread = new HandlerThread(TAG, -2);
        this.mHandlerThread.start();
        this.mHandler = new LocalHandler(this.mHandlerThread.getLooper());
        initCmdMap();
        this.mAppStarter = new HwPCVAssistAppStarter(this.mContext, this, service);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(1));
        this.mOpenDocResultReceiver = new OpenDocResultReceiver();
        IntentFilter openDocResultFilter = new IntentFilter();
        openDocResultFilter.addAction(BROADCAST_WPS_OPEN_DOC_RESULT);
        this.mContext.registerReceiver(this.mOpenDocResultReceiver, openDocResultFilter);
    }

    private void createLocalizedContext(Context context, Locale desiredLocale) {
        Configuration conf = new Configuration(context.getResources().getConfiguration());
        conf.setLocale(desiredLocale);
        this.mContext = context.createConfigurationContext(conf);
    }

    private void initCmdMap() {
        this.mWakedCmdsMap.put("startApp", Integer.valueOf(0));
        this.mWakedCmdsMap.put("closeApp", Integer.valueOf(1));
        this.mWakedCmdsMap.put("minApp", Integer.valueOf(2));
        this.mWakedCmdsMap.put("maxApp", Integer.valueOf(3));
        this.mWakedCmdsMap.put("fullscreenApp", Integer.valueOf(4));
        this.mWakedCmdsMap.put("backApp", Integer.valueOf(5));
        this.mWakedCmdsMap.put("showHome", Integer.valueOf(6));
        this.mWakedCmdsMap.put("openDoc", Integer.valueOf(7));
        this.mWakedCmdsMap.put("prevPage", Integer.valueOf(8));
        this.mWakedCmdsMap.put("nextPage", Integer.valueOf(9));
        this.mWakedCmdsMap.put("startPlay", Integer.valueOf(10));
        this.mWakedCmdsMap.put("stopPlay", Integer.valueOf(11));
    }

    private void loadStrings() {
        loadStringsForExecResults();
        loadStringsForUnWaked();
    }

    private void loadStringsForExecResults() {
        this.mAppStartSuccStrs.add(this.mContext.getString(33686158));
        this.mAppStartSuccStrs.add(this.mContext.getString(33686157));
        this.mCloseSuccResultStrs.add(this.mContext.getString(33686137));
        this.mCloseSuccResultStrs.add(this.mContext.getString(33686135));
        this.mStartPlayResultStrs.add(this.mContext.getString(33686147));
        this.mStartPlayResultStrs.add(this.mContext.getString(33686146));
        this.mOpenDocResultStrs.addAll(this.mAppStartSuccStrs);
        this.mNotConnDisplayResultStrs.add(this.mContext.getString(33686138));
        this.mNotConnDisplayResultStrs.add(this.mContext.getString(33686139));
    }

    private void loadStringsForUnWaked() {
        this.mUnWakedStrs.put(this.mContext.getString(33686151), Integer.valueOf(0));
        this.mUnWakedStrs.put(this.mContext.getString(33686152), Integer.valueOf(1));
        this.mUnWakedStrs.put(this.mContext.getString(33686153), Integer.valueOf(2));
        this.mUnWakedStrs.put(this.mContext.getString(33686154), Integer.valueOf(3));
        this.mUnWakedStrs.put(this.mContext.getString(33686142), Integer.valueOf(4));
        this.mUnWakedStrs.put(this.mContext.getString(33686143), Integer.valueOf(5));
        this.mUnWakedStrs.put(this.mContext.getString(33686144), Integer.valueOf(6));
        this.mUnWakedStrs.put(this.mContext.getString(33686145), Integer.valueOf(7));
        this.mUnWakedStrs.put(this.mContext.getString(33686149), Integer.valueOf(8));
        this.mUnWakedStrs.put(this.mContext.getString(33686150), Integer.valueOf(9));
    }

    public void execVoiceCmd(Message message) {
        HwPCUtils.log(TAG, "execVoiceCmd");
        this.mHandler.sendMessage(this.mHandler.obtainMessage(0, message));
    }

    private boolean isCmdConcernAppNotFound(VoiceCmd voiceCmd) {
        return voiceCmd.cmd == 2 || voiceCmd.cmd == 3 || voiceCmd.cmd == 4 || voiceCmd.cmd == 1;
    }

    /* JADX WARNING: Removed duplicated region for block: B:32:0x0118 A:{ExcHandler: org.json.JSONException (e org.json.JSONException), Splitter: B:1:0x0007} */
    /* JADX WARNING: Missing block: B:33:0x0119, code:
            android.util.HwPCUtils.log(TAG, "resolveSlotsOfCloud ParseException occurred");
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void resolveSlotsOfCloud(VoiceCmd voiceCmd, String name, JSONObject jsonObjSlots) {
        HwPCUtils.log(TAG, "resolveSlotsOfCloud");
        try {
            JSONObject jsonValue;
            String appName;
            StringBuilder stringBuilder;
            if (JSON_KEY_SLOTS_APP.equals(name)) {
                jsonValue = (JSONObject) jsonObjSlots.opt("value");
                if (jsonValue != null) {
                    appName = String.valueOf(jsonValue.opt(JSON_KEY_SYNONYM_WORD));
                    HwPCVAssistAppStarter hwPCVAssistAppStarter = this.mAppStarter;
                    voiceCmd.pkgName = HwPCVAssistAppStarter.getPackageName(this.mContext, appName);
                    if (TextUtils.isEmpty(voiceCmd.pkgName) && isCmdConcernAppNotFound(voiceCmd)) {
                        HwPCUtils.log(TAG, "resolveSlotsOfCloud close special app but app not found.");
                        voiceCmd.extra = appName;
                    } else if (voiceCmd.cmd == 0) {
                        voiceCmd.extra = appName;
                    }
                }
            } else if (JSON_KEY_SLOTS_SCREEN.equals(name)) {
                String origValue = (String) jsonObjSlots.opt(JSON_KEY_ORIG_VALUE);
                appName = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("resolveSlotsOfCloud origValue = ");
                stringBuilder.append(origValue);
                HwPCUtils.log(appName, stringBuilder.toString());
                appName = String.valueOf(jsonObjSlots.opt("value"));
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("resolveSlotsOfCloud value = ");
                stringBuilder2.append(appName);
                HwPCUtils.log(str, stringBuilder2.toString());
                if (JSON_KEY_SLOTS_SCREEN_PHONE.equals(appName)) {
                    voiceCmd.targetDisplay = 0;
                } else if (!TextUtils.isEmpty(appName)) {
                    if (JSON_KEY_SLOTS_SCREEN_PHONE.equals((String) new JSONObject(appName).opt(JSON_KEY_SYNONYM_WORD))) {
                        voiceCmd.targetDisplay = 0;
                    }
                }
            } else if (JSON_KEY_SLOTS_SEQ.equals(name)) {
                jsonValue = (JSONObject) jsonObjSlots.opt("value");
                appName = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("resolveSlotsOfCloud value = ");
                stringBuilder.append(jsonValue);
                HwPCUtils.log(appName, stringBuilder.toString());
                if (jsonValue == null) {
                    HwPCUtils.log(TAG, "resolveSlotsOfCloud value = null");
                    return;
                }
                voiceCmd.extra = (String) jsonValue.opt(JSON_KEY_NUMBER);
                appName = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("resolveSlotsOfCloud extra = ");
                stringBuilder.append(voiceCmd.extra);
                HwPCUtils.log(appName, stringBuilder.toString());
            }
        } catch (JSONException e) {
        }
    }

    private VoiceCmd createVoiceCmdForCloud(VoiceCmd voiceCmd, Bundle bundle) {
        VoiceCmd voiceCmd2 = voiceCmd;
        String jsonStr = bundle.getString(COLUME_CMD_CLOUD);
        if (jsonStr == null) {
            HwPCUtils.log(TAG, "createVoiceCmdForCloud null command");
            return voiceCmd2;
        }
        String cmd = null;
        StringBuilder stringBuilder;
        try {
            JSONArray jsonA = new JSONArray(jsonStr);
            if (jsonA.length() <= 0) {
                HwPCUtils.log(TAG, "createVoiceCmdForCloud jsonA is empty");
                return voiceCmd2;
            }
            JSONObject jsonCmd = jsonA.getJSONObject(0);
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("createVoiceCmdForCloud jsonCmd = ");
            stringBuilder2.append(jsonCmd);
            HwPCUtils.log(str, stringBuilder2.toString());
            if (jsonCmd == null) {
                HwPCUtils.log(TAG, "createVoiceCmdForCloud jsonCmd is empty");
                return voiceCmd2;
            }
            JSONObject jsonPayload = (JSONObject) jsonCmd.opt(JSON_KEY_PAY_LOAD);
            String str2 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("createVoiceCmdForCloud jsonPayload = ");
            stringBuilder3.append(jsonPayload);
            HwPCUtils.log(str2, stringBuilder3.toString());
            if (jsonPayload == null) {
                HwPCUtils.log(TAG, "createVoiceCmdForCloud jsonPayload is empty");
                return voiceCmd2;
            }
            Integer intCmd = (Integer) this.mWakedCmdsMap.get((String) jsonPayload.opt(JSON_KEY_CMD));
            str2 = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("createVoiceCmdForCloud intCmd = ");
            stringBuilder3.append(intCmd);
            HwPCUtils.log(str2, stringBuilder3.toString());
            if (intCmd == null) {
                HwPCUtils.log(TAG, "createVoiceCmdForCloud command not found");
                return voiceCmd2;
            }
            StringBuilder stringBuilder4;
            voiceCmd2.cmd = intCmd.intValue();
            JSONArray jsonArraySlots = (JSONArray) jsonPayload.opt(JSON_KEY_SLOTS);
            int N_SLOTS = jsonArraySlots.length();
            if (N_SLOTS <= 0) {
                String str3 = TAG;
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("createVoiceCmdForCloud jsonArraySlots = ");
                stringBuilder4.append(jsonArraySlots);
                HwPCUtils.log(str3, stringBuilder4.toString());
            }
            int i = 0;
            while (true) {
                int i2 = i;
                String name;
                if (i2 < N_SLOTS) {
                    JSONObject jsonObjSlots = jsonArraySlots.getJSONObject(i2);
                    String str4 = TAG;
                    stringBuilder = new StringBuilder();
                    JSONArray jsonA2 = jsonA;
                    stringBuilder.append("createVoiceCmdForCloud jsonObjSlots = ");
                    stringBuilder.append(jsonObjSlots);
                    HwPCUtils.log(str4, stringBuilder.toString());
                    name = (String) jsonObjSlots.opt("name");
                    String str5 = TAG;
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("createVoiceCmdForCloud name = ");
                    stringBuilder4.append(name);
                    HwPCUtils.log(str5, stringBuilder4.toString());
                    resolveSlotsOfCloud(voiceCmd2, name, jsonObjSlots);
                    i = i2 + 1;
                    jsonA = jsonA2;
                    Bundle bundle2 = bundle;
                } else {
                    name = TAG;
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("createVoiceCmdForCloud voiceCmd = ");
                    stringBuilder5.append(voiceCmd2);
                    HwPCUtils.log(name, stringBuilder5.toString());
                    return voiceCmd2;
                }
            }
        } catch (JSONException e) {
            HwPCUtils.log(TAG, "createVoiceCmdForCloud ParseException occurred");
            voiceCmd2.cmd = -1;
            return voiceCmd2;
        } catch (Exception e2) {
            String str6 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("createVoiceCmdForCloud ");
            stringBuilder.append(e2.toString());
            HwPCUtils.log(str6, stringBuilder.toString());
            voiceCmd2.cmd = -1;
            return voiceCmd2;
        }
    }

    private VoiceCmd createVoiceCmdForTerminal(VoiceCmd voiceCmd, Bundle bundle) {
        String jsonStr = bundle.getString(COLUME_CMD_TERMINAL);
        if (jsonStr == null) {
            HwPCUtils.log(TAG, "createVoiceCmdForTerminal null command");
            return voiceCmd;
        }
        try {
            JSONObject json = new JSONObject(jsonStr);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("createVoiceCmdForTerminal json = ");
            stringBuilder.append(json);
            HwPCUtils.log(str, stringBuilder.toString());
            str = (String) json.opt("action");
            Integer intCmd = (Integer) this.mWakedCmdsMap.get(str);
            if (intCmd == null) {
                HwPCUtils.log(TAG, "createVoiceCmdForTerminal command not found");
                return voiceCmd;
            } else if (intCmd.intValue() != 0) {
                return voiceCmd;
            } else {
                String labelForTerminal = (String) json.opt("label");
                int extraForTerminal = json.optInt(JSON_KEY_EXTRA, 1);
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("createVoiceCmdForTerminal action = ");
                stringBuilder2.append(str);
                stringBuilder2.append(", label = ");
                stringBuilder2.append(labelForTerminal);
                stringBuilder2.append(", extra = ");
                stringBuilder2.append(extraForTerminal);
                HwPCUtils.log(str2, stringBuilder2.toString());
                voiceCmd.pkgName = HwPCVAssistAppStarter.getPackageName(this.mContext, labelForTerminal);
                voiceCmd.extra = labelForTerminal;
                voiceCmd.cmd = intCmd.intValue();
                if (extraForTerminal == 0) {
                    voiceCmd.targetDisplay = 0;
                }
                return voiceCmd;
            }
        } catch (JSONException e) {
            HwPCUtils.log(TAG, "createVoiceCmdForTerminal ParseException occurred");
            return voiceCmd;
        }
    }

    private VoiceCmd createVoiceCmdForUnwaked(VoiceCmd voiceCmd, Bundle bundle) {
        String cmdStr = bundle.getString(COLUME_CMD_UNWAKED);
        if (cmdStr == null) {
            HwPCUtils.log(TAG, "createVoiceCmdForUnwaked null command");
            return voiceCmd;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("createVoiceCmdForUnwaked cmdStr = ");
        stringBuilder.append(cmdStr);
        HwPCUtils.log(str, stringBuilder.toString());
        Integer intCmd = (Integer) this.mUnWakedStrs.get(cmdStr);
        if (intCmd == null) {
            HwPCUtils.log(TAG, "createVoiceCmdForUnwaked command not found");
            return voiceCmd;
        }
        if (intCmd.intValue() >= 0 && intCmd.intValue() <= 3) {
            intCmd = Integer.valueOf(8);
        } else if (intCmd.intValue() > 3 && intCmd.intValue() <= 7) {
            intCmd = Integer.valueOf(9);
        } else if (intCmd.intValue() <= 7 || intCmd.intValue() > 9) {
            intCmd = Integer.valueOf(-1);
        } else {
            intCmd = Integer.valueOf(10);
        }
        if (intCmd.intValue() == -1) {
            HwPCUtils.log(TAG, "createVoiceCmdForUnwaked invalid command");
            return voiceCmd;
        }
        voiceCmd.cmd = intCmd.intValue();
        return voiceCmd;
    }

    private VoiceCmd resolveJsonCmd(Message msg, int castingDisplay) {
        Message message = msg.obj;
        Bundle bundle = message.getData();
        Messenger messenger = message.replyTo;
        if (messenger == null || bundle == null) {
            HwPCUtils.log(TAG, "resolveJsonCmd messenger or data is null");
            return null;
        }
        int serviceId = message.what;
        int flag = message.arg1;
        int sessionId = message.arg2;
        VoiceCmd voiceCmd = new VoiceCmd();
        voiceCmd.flag = flag;
        voiceCmd.sessionId = serviceId;
        voiceCmd.messenger = messenger;
        voiceCmd.castingDisplay = castingDisplay;
        voiceCmd.targetDisplay = castingDisplay;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("resolveJsonCmd serviceId = ");
        stringBuilder.append(serviceId);
        stringBuilder.append(", flag = ");
        stringBuilder.append(flag);
        stringBuilder.append(", sessionId = ");
        stringBuilder.append(sessionId);
        HwPCUtils.log(str, stringBuilder.toString());
        if (serviceId != ID_VASSIST_SERVICE || flag > 2 || sessionId < 0) {
            HwPCUtils.log(TAG, "resolveJsonCmd is not vassist service id or invalid flag or sessionId");
            return voiceCmd;
        }
        switch (flag) {
            case 1:
                return createVoiceCmdForTerminal(voiceCmd, bundle);
            case 2:
                return createVoiceCmdForUnwaked(voiceCmd, bundle);
            default:
                return createVoiceCmdForCloud(voiceCmd, bundle);
        }
    }

    public void execVoiceCmdImpl(VoiceCmd cmd) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("execVoiceCmdImpl cmd = ");
        stringBuilder.append(cmd);
        HwPCUtils.log(str, stringBuilder.toString());
        switch (cmd.cmd) {
            case 0:
                HwPCUtils.log(TAG, "CMD_START_APP");
                HwPCUtils.bdReport(this.mContext, 10059, "START_APP");
                this.mAppStarter.startApp(cmd);
                return;
            case 1:
                HwPCUtils.log(TAG, "CMD_CLOSE_APP");
                HwPCUtils.bdReport(this.mContext, 10059, "CLOSE_APP");
                closeApp(cmd);
                return;
            case 2:
                HwPCUtils.log(TAG, "CMD_MIN_APP");
                HwPCUtils.bdReport(this.mContext, 10059, "MIN_APP");
                minTask(cmd);
                return;
            case 3:
                HwPCUtils.log(TAG, "CMD_MAX_APP");
                HwPCUtils.bdReport(this.mContext, 10059, "MAX_APP");
                maxTask(cmd);
                return;
            case 4:
                HwPCUtils.log(TAG, "CMD_FULL_SCREEN_APP");
                HwPCUtils.bdReport(this.mContext, 10059, "FULL_SCREEN_APP");
                fullScreenTask(cmd);
                return;
            case 5:
                HwPCUtils.log(TAG, "CMD_BACK_APP");
                HwPCUtils.bdReport(this.mContext, 10059, "BACK_APP");
                backWindow(cmd);
                return;
            case 6:
                HwPCUtils.log(TAG, "CMD_SHOW_HOME");
                HwPCUtils.bdReport(this.mContext, 10059, "SHOW_HOME");
                this.mAMS.toggleHome();
                replyResultToVAssist(-4, cmd);
                return;
            case 7:
                HwPCUtils.log(TAG, "CMD_OPEN_DOC");
                HwPCUtils.bdReport(this.mContext, 10059, "OPEN_DOC");
                openDocByIndex(cmd);
                return;
            case 8:
                HwPCUtils.log(TAG, "CMD_PREV_PAGE");
                HwPCUtils.bdReport(this.mContext, 10059, "PREV_PAGE");
                pageUpDown(cmd);
                return;
            case 9:
                HwPCUtils.log(TAG, "CMD_NEXT_PAGE");
                HwPCUtils.bdReport(this.mContext, 10059, "NEXT_PAGE");
                pageUpDown(cmd);
                return;
            case 10:
                HwPCUtils.log(TAG, "CMD_START_PLAY");
                HwPCUtils.bdReport(this.mContext, 10059, "START_PLAY");
                startStopPlayPPT(cmd);
                return;
            case 11:
                HwPCUtils.log(TAG, "CMD_STOP_PLAY");
                HwPCUtils.bdReport(this.mContext, 10059, "STOP_PLAY");
                startStopPlayPPT(cmd);
                return;
            default:
                return;
        }
    }

    private boolean checkAppExists(VoiceCmd cmd) {
        if (TextUtils.isEmpty(cmd.extra) || !TextUtils.isEmpty(cmd.pkgName)) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("checkAppExists cannot found App ");
        stringBuilder.append(cmd.extra);
        HwPCUtils.log(str, stringBuilder.toString());
        return false;
    }

    private int getHandledTaskId(VoiceCmd cmd, int displayId) {
        return getHandledTaskId(cmd, displayId, false, false);
    }

    private int getHandledTaskId(VoiceCmd cmd, int displayId, boolean findInDefaultDisplay, boolean invisibleAlso) {
        String packageName = cmd.pkgName;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getHandledTaskId packageName = ");
        stringBuilder.append(packageName);
        stringBuilder.append(", displayId = ");
        stringBuilder.append(displayId);
        HwPCUtils.log(str, stringBuilder.toString());
        boolean emptyPkg = TextUtils.isEmpty(cmd.pkgName);
        int taskId = HwActivityManager.getTopTaskIdInDisplay(displayId, cmd.pkgName, invisibleAlso);
        if (emptyPkg || taskId >= 0 || displayId == 0 || !findInDefaultDisplay) {
            return taskId;
        }
        return HwActivityManager.getTopTaskIdInDisplay(0, cmd.pkgName, invisibleAlso);
    }

    private void closeApp(VoiceCmd cmd) {
        if (checkAppExists(cmd)) {
            int taskId = getHandledTaskId(cmd, cmd.targetDisplay, true, true);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("closeApp taskId = ");
            stringBuilder.append(taskId);
            HwPCUtils.log(str, stringBuilder.toString());
            if (taskId < 0) {
                HwPCUtils.log(TAG, "closeApp invalid taskId");
            }
            this.mAMS.removeTask(taskId);
            this.mService.setFocusedPCDisplayId("CMD_CLOSE_APP");
            replyResultToVAssist(-2, cmd);
            return;
        }
        replyResultToVAssist(4, cmd);
    }

    private void minTask(VoiceCmd cmd) {
        if (checkAppExists(cmd)) {
            int taskId = getHandledTaskId(cmd, cmd.castingDisplay, true, true);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("minTask taskId = ");
            stringBuilder.append(taskId);
            HwPCUtils.log(str, stringBuilder.toString());
            if (taskId < 0) {
                HwPCUtils.log(TAG, "minTask invalid taskId");
                replyResultToVAssist(5, cmd);
                return;
            } else if (HwActivityManager.isTaskVisible(taskId)) {
                this.mAMS.moveTaskBackwards(taskId);
                this.mService.setFocusedPCDisplayId("CMD_MIN_APP");
                replyResultToVAssist(-3, cmd);
                return;
            } else {
                HwPCUtils.log(TAG, "minTask task is invisible already");
                replyResultToVAssist(-3, cmd);
                return;
            }
        }
        replyResultToVAssist(5, cmd);
    }

    private void maxTask(VoiceCmd cmd) {
        if (checkAppExists(cmd)) {
            int taskId = getHandledTaskId(cmd, cmd.castingDisplay);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("maxTask taskId = ");
            stringBuilder.append(taskId);
            HwPCUtils.log(str, stringBuilder.toString());
            if (taskId < 0) {
                replyResultToVAssist(5, cmd);
                return;
            } else if (HwActivityManager.isTaskSupportResize(taskId, false, true)) {
                this.mService.hwResizeTask(taskId, new Rect(0, 0, 0, 0));
                replyResultToVAssist(-3, cmd);
                this.mService.setFocusedPCDisplayId("CMD_MAX_APP");
                return;
            } else {
                HwPCUtils.log(TAG, "maxTask cannot larger");
                replyResultToVAssist(6, cmd);
                return;
            }
        }
        replyResultToVAssist(5, cmd);
    }

    private void fullScreenTask(VoiceCmd cmd) {
        if (checkAppExists(cmd)) {
            int taskId = getHandledTaskId(cmd, cmd.castingDisplay);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("fullScreenTask taskId = ");
            stringBuilder.append(taskId);
            HwPCUtils.log(str, stringBuilder.toString());
            if (taskId < 0) {
                HwPCUtils.log(TAG, "invalid taskid");
                replyResultToVAssist(5, cmd);
                return;
            } else if (!HwActivityManager.isTaskSupportResize(taskId, true, false)) {
                HwPCUtils.log(TAG, "maxTask cannot larger");
                replyResultToVAssist(6, cmd);
                return;
            } else if (HwActivityManager.isTaskVisible(taskId)) {
                this.mService.hwResizeTask(taskId, new Rect(-1, -1, -1, -1));
                this.mService.setFocusedPCDisplayId("CMD_FULL_SCREEN_APP");
                replyResultToVAssist(-6, cmd);
                return;
            } else {
                HwPCUtils.log(TAG, "minTask task is invisible already");
                replyResultToVAssist(5, cmd);
                return;
            }
        }
        replyResultToVAssist(5, cmd);
    }

    public static boolean specialAppFocused(int displayId, WindowStateData outData, boolean forPPT, boolean ignoreLaserView) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("specialAppFocused displayId = ");
        stringBuilder.append(displayId);
        HwPCUtils.log(str, stringBuilder.toString());
        boolean z = false;
        if (ignoreLaserView || !HwWindowManager.hasLighterViewInPCCastMode()) {
            String[] special_apps;
            Bundle outBundle = new Bundle();
            HwWindowManager.getCurrFocusedWinInExtDisplay(outBundle);
            String focusableAppPkg = outBundle.getString(AwareIntelligentRecg.CMP_PKGNAME, null);
            boolean topWinApp = outBundle.getBoolean("isApp", false);
            Rect bounds = (Rect) outBundle.getParcelable("bounds");
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("specialAppFocused focusableAppPkg = ");
            stringBuilder2.append(focusableAppPkg);
            stringBuilder2.append(", topWinApp = ");
            stringBuilder2.append(topWinApp);
            stringBuilder2.append(", bounds = ");
            stringBuilder2.append(bounds);
            HwPCUtils.log(str2, stringBuilder2.toString());
            boolean topSpecialApp = false;
            if (forPPT) {
                special_apps = SPECIAL_APPS_FOR_PPT;
            } else {
                special_apps = SPECIAL_APPS_FOR_PAGE;
            }
            if (focusableAppPkg != null) {
                for (CharSequence contains : special_apps) {
                    if (focusableAppPkg.contains(contains)) {
                        topSpecialApp = true;
                        break;
                    }
                }
            }
            if (outData != null) {
                outData.pkgName = focusableAppPkg;
                outData.topWinApp = topSpecialApp;
                outData.bounds = bounds;
            }
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("specialAppFocused topWinApp = ");
            stringBuilder3.append(topWinApp);
            stringBuilder3.append(", topSpecialApp = ");
            stringBuilder3.append(topSpecialApp);
            HwPCUtils.log(str3, stringBuilder3.toString());
            if (topWinApp && topSpecialApp) {
                z = true;
            }
            return z;
        }
        HwPCUtils.log(TAG, "specialAppFocused has laser lighter view");
        return false;
    }

    private boolean injectInputEventInternal(InputEvent event, int mode) {
        HwInputManagerLocalService inputManager = (HwInputManagerLocalService) LocalServices.getService(HwInputManagerLocalService.class);
        if (inputManager != null) {
            return inputManager.injectInputEvent(event, mode);
        }
        return false;
    }

    private void sendEvent(int code, int metaState, int action) {
        long downTime = SystemClock.uptimeMillis();
        KeyEvent ev = new KeyEvent(downTime, downTime, action, code, 0, metaState, -1, 0, 4104, 257);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("send key event, code:");
        stringBuilder.append(code);
        stringBuilder.append(",metaState:");
        stringBuilder.append(metaState);
        stringBuilder.append(", isExternal = ");
        stringBuilder.append(ev.getHwFlags());
        HwPCUtils.log(str, stringBuilder.toString());
        injectInputEventInternal(ev, 0);
    }

    private void openDocByIndex(VoiceCmd cmd) {
        HwPCUtils.log(TAG, "openDocByIndex");
        if (specialAppFocused(cmd.castingDisplay, new WindowStateData(), true, false)) {
            boolean hasNoException = true;
            int index = -1;
            if (!TextUtils.isEmpty(cmd.extra)) {
                try {
                    index = Integer.parseInt(cmd.extra);
                } catch (NumberFormatException e) {
                    hasNoException = false;
                    HwPCUtils.log(TAG, "openDocByIndex NumberFormatException ocurred.");
                }
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("index = ");
            stringBuilder.append(index);
            HwPCUtils.log(str, stringBuilder.toString());
            if (hasNoException) {
                this.mHandler.setVoiceCmdForOpenDoc(cmd);
                Intent intent = new Intent(BROADCAST_WPS_OPEN_DOC);
                intent.putExtra("file_index", index);
                intent.putExtra("session_id", cmd.sessionId);
                this.mContext.sendBroadcast(intent, PERMISSION_BROADCAST_VASSIST_DESKTOP);
            } else {
                replyResultToVAssist(5, cmd);
            }
            return;
        }
        HwPCUtils.log(TAG, "openDocByIndex special app not focused");
        replyResultToVAssist(3, cmd);
    }

    private void broadcastDesktopMode(boolean desktopMode) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("broadcastDesktopMode desktopMode = ");
        stringBuilder.append(desktopMode);
        HwPCUtils.log(str, stringBuilder.toString());
        Intent intent = new Intent(BROADCAST_NOTIFY_DESKTOP_MODE);
        intent.putExtra(ProcessStopShrinker.MODE_KEY, desktopMode);
        this.mContext.sendBroadcast(intent, PERMISSION_BROADCAST_VASSIST_DESKTOP);
    }

    private void replyResultOfOpenDoc(Message msg) {
        int result = msg.arg1;
        int sessionId = msg.arg2;
        HwPCUtils.log(TAG, "replyResultOfOpenDoc");
        VoiceCmd cmd = this.mHandler.getVoiceCmdForOpenDoc();
        if (cmd == null || sessionId != cmd.sessionId) {
            HwPCUtils.log(TAG, "replyResultOfOpenDoc invalid reply from WPS");
            return;
        }
        if (result <= 0) {
            replyResultToVAssist(-1, cmd);
        } else {
            replyResultToVAssist(7, cmd);
        }
        this.mHandler.setVoiceCmdForOpenDoc(null);
    }

    private PointerCoords getScollPoint(int displayId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getScollPoint displayId = ");
        stringBuilder.append(displayId);
        HwPCUtils.log(str, stringBuilder.toString());
        PointerCoords point = new PointerCoords();
        point.x = -1.0f;
        point.y = -1.0f;
        DisplayManager dm = (DisplayManager) this.mContext.getSystemService("display");
        if (dm == null) {
            HwPCUtils.log(TAG, "getScollPoint dm  = null");
            return point;
        }
        Display display = dm.getDisplay(displayId);
        if (display == null) {
            HwPCUtils.log(TAG, "getScollPoint display  = null");
            return point;
        }
        DisplayInfo outDisplayInfo = new DisplayInfo();
        display.getDisplayInfo(outDisplayInfo);
        Rect bound = HwActivityManager.getPCTopTaskBounds(displayId);
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getScollPoint bound = ");
        stringBuilder2.append(bound);
        HwPCUtils.log(str2, stringBuilder2.toString());
        if (bound == null) {
            point.x = ((float) outDisplayInfo.logicalWidth) / 2.0f;
            point.y = ((float) outDisplayInfo.logicalHeight) / 2.0f;
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getScollPoint point.x = ");
            stringBuilder2.append(point.x);
            stringBuilder2.append(", point.y = ");
            stringBuilder2.append(point.y);
            HwPCUtils.log(str2, stringBuilder2.toString());
            return point;
        }
        if (bound.right > outDisplayInfo.logicalWidth) {
            bound.right = outDisplayInfo.logicalWidth;
        }
        if (bound.bottom > outDisplayInfo.logicalHeight) {
            bound.bottom = outDisplayInfo.logicalHeight;
        }
        point.x = ((float) (bound.left + bound.right)) / 2.0f;
        point.y = ((float) (bound.top + bound.bottom)) / 2.0f;
        str2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getScollPoint point.x = ");
        stringBuilder2.append(point.x);
        stringBuilder2.append(", point.y = ");
        stringBuilder2.append(point.y);
        HwPCUtils.log(str2, stringBuilder2.toString());
        if (point.x < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
            point.x = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        }
        return point;
    }

    private void pageUpDown(VoiceCmd voiceCmd) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("pageUpDown cmd = ");
        stringBuilder.append(voiceCmd.cmd);
        HwPCUtils.log(str, stringBuilder.toString());
        WindowStateData wsd = new WindowStateData();
        if (specialAppFocused(voiceCmd.castingDisplay, wsd, false, true)) {
            boolean pageDown = voiceCmd.cmd == 9;
            int keyCode;
            if (wsd.pkgName != null && wsd.pkgName.contains(SPECIAL_APPS_FOR_PAGE[2])) {
                keyCode = pageDown ? 93 : 92;
                sendEvent(keyCode, 0, 0);
                sendEvent(keyCode, 0, 1);
                replyResultToVAssist(-6, voiceCmd);
                return;
            } else if (wsd.topWinApp) {
                this.mService.setFocusedPCDisplayId("pageUpDown");
                PointerCoords pointer = getScollPoint(voiceCmd.castingDisplay);
                if (pointer.x < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
                    HwPCUtils.log(TAG, "pageUpDown special app not focused");
                    replyResultToVAssist(3, voiceCmd);
                    return;
                }
                onScrollEvent(pointer.x, pointer.y, pageDown);
                replyResultToVAssist(-6, voiceCmd);
                return;
            } else {
                keyCode = pageDown ? 20 : 19;
                sendEvent(keyCode, 0, 0);
                sendEvent(keyCode, 0, 1);
                replyResultToVAssist(-6, voiceCmd);
                return;
            }
        }
        HwPCUtils.log(TAG, "pageUpDown special app not focused");
        replyResultToVAssist(3, voiceCmd);
    }

    private void onScrollEvent(float x, float y, boolean pageDown) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onScrollEvent x:");
        stringBuilder.append(x);
        stringBuilder.append(", y = ");
        stringBuilder.append(y);
        stringBuilder.append(", pageDown = ");
        stringBuilder.append(pageDown);
        HwPCUtils.log(str, stringBuilder.toString());
        injectInputEventInternal(HwPCMkManager.obtainMotionEvent(HwPCMkManager.obtainMouseEvent(8, 0, x, y, 0, 0), x, y, 8, 0, pageDown ? -1.0f : 1.0f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO), 0);
    }

    private void backWindow(VoiceCmd cmd) {
        int taskId = getHandledTaskId(cmd, cmd.targetDisplay);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("backWindow taskId = ");
        stringBuilder.append(taskId);
        HwPCUtils.log(str, stringBuilder.toString());
        if (taskId < 0) {
            HwPCUtils.log(TAG, "backWindow invalid taskid");
            replyResultToVAssist(5, cmd);
        } else if (HwActivityManager.isTaskVisible(taskId)) {
            sendEvent(4, 0, 0);
            sendEvent(4, 0, 1);
            replyResultToVAssist(-4, cmd);
        } else {
            HwPCUtils.log(TAG, "backWindow task is invisible already");
            replyResultToVAssist(5, cmd);
        }
    }

    private void startStopPlayPPT(VoiceCmd cmd) {
        boolean startPlay = cmd.cmd == 10;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startStopPlayPPT startPlay = ");
        stringBuilder.append(startPlay);
        HwPCUtils.log(str, stringBuilder.toString());
        if (specialAppFocused(cmd.targetDisplay, new WindowStateData(), true, false)) {
            int keyCode = startPlay ? CPUFeature.MSG_SET_BOOST_CPUS : HwArbitrationDEFS.MSG_INSTANT_PAY_APP_START;
            sendEvent(keyCode, 0, 0);
            sendEvent(keyCode, 0, 1);
            replyResultToVAssist(-5, cmd);
            return;
        }
        HwPCUtils.log(TAG, "pageUpDown special app not focused");
        replyResultToVAssist(3, cmd);
    }

    public void notifyDesktopModeChanged(boolean desktopMode, int displayId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyDesktopModeChanged desktopMode = ");
        stringBuilder.append(desktopMode);
        stringBuilder.append(", mDesktopMode = ");
        stringBuilder.append(this.mDesktopMode);
        stringBuilder.append(", displayId = ");
        stringBuilder.append(displayId);
        HwPCUtils.log(str, stringBuilder.toString());
        this.mDesktopMode = desktopMode;
        this.mCastingDisplayId = displayId;
        broadcastDesktopMode(desktopMode);
    }

    private int getResponseErrCode(int errCode) {
        if (errCode > 0) {
            return 1;
        }
        return 0;
    }

    public String getRandomResponseStr(List<String> list) {
        if (list == null) {
            HwPCUtils.log(TAG, "getRandomResponseStr list = null");
            return "";
        }
        int reportIndex = getRadomReportStringIndex(list.size());
        if (reportIndex < 0) {
            return (String) list.get(0);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getRandomResponseStr reportIndex = ");
        stringBuilder.append(reportIndex);
        stringBuilder.append(", size = ");
        stringBuilder.append(list.size());
        HwPCUtils.log(str, stringBuilder.toString());
        return (String) list.get(reportIndex);
    }

    private String getResponseMsg(int errCode) {
        switch (errCode) {
            case -6:
                return "";
            case -5:
                return getRandomResponseStr(this.mStartPlayResultStrs);
            case -4:
            case -3:
                return this.mContext.getString(33686148);
            case -2:
                return getRandomResponseStr(this.mCloseSuccResultStrs);
            case -1:
                return getRandomResponseStr(this.mAppStartSuccStrs);
            case 2:
                return getRandomResponseStr(this.mNotConnDisplayResultStrs);
            case 3:
                return this.mContext.getString(33686131);
            case 4:
                return this.mContext.getString(33686129);
            case 5:
                return this.mContext.getString(33686130);
            case 6:
                return this.mContext.getString(33686133);
            case 7:
                return this.mContext.getString(33686140);
            default:
                return this.mContext.getString(33686158);
        }
    }

    private void replyResultToVAssist(int errCode, VoiceCmd cmd) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("replyResultToVAssist errCode = ");
        stringBuilder.append(errCode);
        stringBuilder.append(", cmd = ");
        stringBuilder.append(cmd);
        HwPCUtils.log(str, stringBuilder.toString());
        if (cmd == null || cmd.messenger == null) {
            HwPCUtils.log(TAG, "replyResultToVAssist messenger = null, cannot reply to vassist");
            return;
        }
        try {
            JSONObject obj = new JSONObject();
            obj.put("errorCode", getResponseErrCode(errCode));
            String response = getResponseMsg(errCode);
            obj.put("responseText", response);
            obj.put("ttsText", response);
            obj.put("isFinish", "true");
            replyResultToVAssist(obj, cmd);
        } catch (JSONException e) {
            HwPCUtils.log(TAG, "replyResultToVAssist JSONException occurred");
        }
    }

    public void replyResultToVAssist(JSONObject jObj, VoiceCmd cmd) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("replyResultToVAssist jObj = ");
        stringBuilder.append(jObj);
        stringBuilder.append(", cmd = ");
        stringBuilder.append(cmd);
        HwPCUtils.log(str, stringBuilder.toString());
        if (cmd == null || cmd.messenger == null) {
            HwPCUtils.log(TAG, "replyResultToVAssist messenger = null, cannot reply to vassist");
            return;
        }
        Message msg = Message.obtain(null, ID_VASSIST_SERVICE, cmd.sessionId, cmd.flag);
        Bundle bundle = new Bundle();
        bundle.putString("serviceReply", jObj.toString());
        msg.setData(bundle);
        try {
            cmd.messenger.send(msg);
        } catch (RemoteException e) {
            HwPCUtils.log(TAG, "replyResultToVAssist exception occurred");
        }
    }

    public int getRadomReportStringIndex(int size) {
        if (size <= 0) {
            HwPCUtils.log(TAG, "getResponseMsg size = 0");
            return -1;
        }
        if (this.mReplayCount == 2147483646) {
            this.mReplayCount = 0;
        }
        this.mReplayCount++;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getResponseMsg mReplayCount = ");
        stringBuilder.append(this.mReplayCount);
        stringBuilder.append(", size = ");
        stringBuilder.append(size);
        HwPCUtils.log(str, stringBuilder.toString());
        return this.mReplayCount % size;
    }
}
