package com.android.server.rms.iaware.appmng;

import android.app.ActivityManagerNative;
import android.app.IProcessObserver.Stub;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.rms.iaware.AppTypeRecoManager;
import android.rms.iaware.AwareLog;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.SparseArray;
import com.android.internal.os.BackgroundThread;
import com.android.server.am.HwActivityManagerService;
import com.android.server.mtm.iaware.appmng.AwareProcessBaseInfo;
import com.android.server.mtm.taskstatus.ProcessInfo;
import com.android.server.mtm.taskstatus.ProcessInfoCollector;
import com.android.server.mtm.utils.InnerUtils;
import com.huawei.pgmng.plug.PGSdk;
import com.huawei.pgmng.plug.PGSdk.Sink;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class AwareAppKeyBackgroup {
    private static final String[] APPTYPESTRING = new String[]{"TYPE_UNKNOW", "TYPE_LAUNCHER", "TYPE_SMS", "TYPE_EMAIL", "TYPE_INPUTMETHOD", "TYPE_GAME", "TYPE_BROWSER", "TYPE_EBOOK", "TYPE_VIDEO", "TYPE_SCRLOCK", "TYPE_CLOCK", "TYPE_IM", "TYPE_MUSIC"};
    private static final String CALL_APP_PKG = "com.android.incallui";
    private static boolean DEBUG = false;
    private static final long DECAY_TIME = 60000;
    private static final long DECAY_UPLOAD_DL_TIME = 10000;
    public static final int EVENT_TYPE_ENTER = 1;
    public static final int EVENT_TYPE_EXIT = 2;
    private static final String HUAWEI_AMAP_APP_PKG = "com.amap.android.ams";
    private static final int MSG_APP_PROCESSDIED = 2;
    private static final int MSG_ASSOCIATE_CHECK = 4;
    private static final int MSG_PGSDK_INIT = 3;
    private static final int MSG_REMOVE_DECAY_STATE = 1;
    private static final long PGSDK_REINIT_TIME = 2000;
    private static final int PID_INVALID = -1;
    private static final String[] STATESTRING = new String[]{"STATE_NULL", "STATE_AUDIO_IN", "STATE_AUDIO_OUT", "STATE_GPS", "STATE_SENSOR", "STATE_UPLOAD_DL"};
    public static final int STATE_ALL = 100;
    public static final int STATE_AUDIO_IN = 1;
    public static final int STATE_AUDIO_OUT = 2;
    public static final int STATE_GPS = 3;
    public static final int STATE_IMEMAIL = 99;
    public static final int STATE_KEY_BG = 0;
    public static final int STATE_KEY_BG_INVALID = -1;
    public static final int STATE_NAT_TIMEOUT = 11;
    public static final int STATE_SENSOR = 4;
    private static final int STATE_SIZE = STATESTRING.length;
    public static final int STATE_UPLOAD_DL = 5;
    private static final String TAG = "AwareAppKeyBackgroup";
    private static final int TYPE_SIZE = APPTYPESTRING.length;
    private static final String WECHAT_APP_PKG = "com.tencent.mm";
    private static AwareAppKeyBackgroup sInstance = null;
    private int mAmapAppUid;
    private AppKeyHandler mAppKeyHandler;
    private final ArraySet<Integer> mAudioCacheUids;
    PhoneStateListener mCallStateListener;
    private final ArrayMap<IAwareStateCallback, ArraySet<Integer>> mCallbacks;
    private Context mContext;
    private final SparseArray<SensorRecord> mHistorySensorRecords;
    private HwActivityManagerService mHwAMS;
    private boolean mIsAbroadArea;
    private AtomicBoolean mIsInitialized;
    private AtomicBoolean mIsInitializing;
    private final ArraySet<Integer> mKeyBackgroupPids;
    private final ArraySet<String> mKeyBackgroupPkgs;
    private final ArraySet<Integer> mKeyBackgroupUids;
    private AtomicBoolean mLastSetting;
    private int mNatTimeout;
    private PGSdk mPGSdk;
    private PackageManager mPM;
    private AwareABGProcessObserver mProcessObserver;
    private final ArrayList<ArraySet<Integer>> mScenePidArray;
    private final ArrayList<ArraySet<String>> mScenePkgArray;
    private final ArrayList<ArraySet<Integer>> mSceneUidArray;
    private final List<DecayInfo> mStateEventDecayInfos;
    private Sink mStateRecognitionListener;

    private class AppKeyHandler extends Handler {
        public AppKeyHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            Message message = msg;
            if (AwareAppKeyBackgroup.DEBUG) {
                String str = AwareAppKeyBackgroup.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleMessage message ");
                stringBuilder.append(message.what);
                AwareLog.e(str, stringBuilder.toString());
            }
            if (message.what == 3) {
                AwareAppKeyBackgroup.this.doInitialize();
            } else if (AwareAppKeyBackgroup.this.mIsInitialized.get()) {
                DecayInfo decayInfo = message.obj instanceof DecayInfo ? (DecayInfo) message.obj : null;
                int i = message.what;
                if (i != 4) {
                    switch (i) {
                        case 1:
                            if (decayInfo != null) {
                                if (AwareAppKeyBackgroup.DEBUG) {
                                    String str2 = AwareAppKeyBackgroup.TAG;
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Update state ");
                                    stringBuilder2.append(decayInfo.getStateType());
                                    stringBuilder2.append(" uid : ");
                                    stringBuilder2.append(decayInfo.getUid());
                                    AwareLog.d(str2, stringBuilder2.toString());
                                }
                                AwareAppKeyBackgroup.this.updateSceneState(decayInfo.getStateType(), decayInfo.getEventType(), decayInfo.getPid(), decayInfo.getPkg(), decayInfo.getUid());
                                AwareAppKeyBackgroup.this.removeDecayInfo(decayInfo.getStateType(), decayInfo.getEventType(), decayInfo.getPid(), decayInfo.getPkg(), decayInfo.getUid());
                                if (decayInfo.getStateType() == 2) {
                                    AwareAppKeyBackgroup.this.updateAudioCache(decayInfo.getPid(), decayInfo.getUid());
                                    break;
                                }
                            }
                            return;
                            break;
                        case 2:
                            int pid = message.arg1;
                            int uid = message.arg2;
                            AwareAppKeyBackgroup.this.updateSceneArrayProcessDied(uid, pid);
                            if (AwareAppKeyBackgroup.this.getStateEventDecayInfosSize() > 0) {
                                i = 1;
                                while (true) {
                                    int i2 = i;
                                    if (i2 >= AwareAppKeyBackgroup.STATE_SIZE) {
                                        break;
                                    }
                                    AwareAppKeyBackgroup.this.removeDecayForProcessDied(i2, 2, i2 == 2 ? pid : 0, null, uid);
                                    i = i2 + 1;
                                }
                            }
                            break;
                    }
                } else if (decayInfo != null && decayInfo.getStateType() == 3) {
                    AwareAppKeyBackgroup.this.checkHuaweiAmapApp(decayInfo.getStateType(), decayInfo.getEventType(), decayInfo.getUid());
                }
            }
        }
    }

    class AwareABGProcessObserver extends Stub {
        AwareABGProcessObserver() {
        }

        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
        }

        public void onProcessDied(int pid, int uid) {
            synchronized (AwareAppKeyBackgroup.this) {
                boolean isKbgPid = AwareAppKeyBackgroup.this.mKeyBackgroupPids.contains(Integer.valueOf(pid));
                boolean isKbgUid = AwareAppKeyBackgroup.this.mKeyBackgroupUids.contains(Integer.valueOf(uid));
                if (isKbgPid || isKbgUid) {
                    if (AwareAppKeyBackgroup.DEBUG) {
                        String str = AwareAppKeyBackgroup.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("onProcessDied pid ");
                        stringBuilder.append(pid);
                        stringBuilder.append(" uid ");
                        stringBuilder.append(uid);
                        AwareLog.d(str, stringBuilder.toString());
                    }
                    Message observerMsg = AwareAppKeyBackgroup.this.mAppKeyHandler.obtainMessage();
                    observerMsg.arg1 = pid;
                    observerMsg.arg2 = uid;
                    observerMsg.what = 2;
                    AwareAppKeyBackgroup.this.mAppKeyHandler.sendMessage(observerMsg);
                    return;
                }
            }
        }
    }

    static class DecayInfo {
        private int mEventType;
        private int mPid;
        private String mPkg;
        private int mStateType;
        private int mUid;

        public DecayInfo(int stateType, int eventType, int pid, String pkg, int uid) {
            this.mStateType = stateType;
            this.mEventType = eventType;
            this.mPid = pid;
            this.mUid = uid;
            this.mPkg = pkg;
        }

        public int hashCode() {
            return super.hashCode();
        }

        public boolean equals(Object obj) {
            boolean z = false;
            if (obj == null) {
                return false;
            }
            if (this == obj) {
                return true;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            DecayInfo other = (DecayInfo) obj;
            if (other.getStateType() == this.mStateType && other.getEventType() == this.mEventType && other.getPid() == this.mPid && other.getUid() == this.mUid) {
                z = true;
            }
            return z;
        }

        public int getStateType() {
            return this.mStateType;
        }

        public int getEventType() {
            return this.mEventType;
        }

        public int getPid() {
            return this.mPid;
        }

        public int getUid() {
            return this.mUid;
        }

        public String getPkg() {
            return this.mPkg;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{");
            stringBuilder.append(AwareAppKeyBackgroup.stateToString(this.mStateType));
            stringBuilder.append(",");
            stringBuilder.append(this.mPid);
            stringBuilder.append(",");
            stringBuilder.append(this.mUid);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    public interface IAwareStateCallback {
        void onStateChanged(int i, int i2, int i3, int i4);
    }

    private class SensorRecord {
        private final ArrayMap<Integer, Integer> mHandles = new ArrayMap();
        private int mUid;

        public SensorRecord(int uid, int handle) {
            this.mUid = uid;
            addSensor(handle);
        }

        public boolean hasSensor() {
            return this.mHandles.size() > 0;
        }

        public void addSensor(int handle) {
            if (!hasSensor()) {
                AwareAppKeyBackgroup.this.updateAppSensorState(true, this.mUid, 0);
            }
            Integer count = (Integer) this.mHandles.get(Integer.valueOf(handle));
            if (count == null) {
                this.mHandles.put(Integer.valueOf(handle), Integer.valueOf(1));
            } else {
                this.mHandles.put(Integer.valueOf(handle), Integer.valueOf(count.intValue() + 1));
            }
            if (AwareAppKeyBackgroup.DEBUG) {
                String str = AwareAppKeyBackgroup.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("addSensor,mHandles:");
                stringBuilder.append(this.mHandles);
                AwareLog.i(str, stringBuilder.toString());
            }
        }

        public void removeSensor(Integer handle) {
            Integer count = (Integer) this.mHandles.get(handle);
            if (count != null) {
                int value = count.intValue() - 1;
                if (value <= 0) {
                    this.mHandles.remove(handle);
                } else {
                    this.mHandles.put(handle, Integer.valueOf(value));
                }
            }
            if (AwareAppKeyBackgroup.DEBUG) {
                String str = AwareAppKeyBackgroup.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("removeSensor,mHandles:");
                stringBuilder.append(this.mHandles);
                AwareLog.i(str, stringBuilder.toString());
            }
            if (!hasSensor()) {
                AwareAppKeyBackgroup.this.updateAppSensorState(false, this.mUid, 0);
            }
        }
    }

    private static String stateToString(int state) {
        if (state < 0 || state >= STATE_SIZE) {
            return "STATE_NULL";
        }
        return STATESTRING[state];
    }

    private static String typeToString(int type) {
        if (type < 0 || type >= TYPE_SIZE) {
            return "TYPE_UNKNOW";
        }
        return APPTYPESTRING[type];
    }

    private boolean checkCallingPermission() {
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        if (pid == Process.myPid() || uid == 0 || uid == 1000) {
            return true;
        }
        return false;
    }

    private int getAppTypeFromHabit(int uid) {
        if (this.mContext == null) {
            return -1;
        }
        if (this.mPM == null) {
            this.mPM = this.mContext.getPackageManager();
            if (this.mPM == null) {
                AwareLog.e(TAG, "Failed to get PackageManager");
                return -1;
            }
        }
        String[] pkgNames = this.mPM.getPackagesForUid(uid);
        if (pkgNames == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to get package name for uid: ");
            stringBuilder.append(uid);
            AwareLog.e(str, stringBuilder.toString());
            return -1;
        }
        for (String pkgName : pkgNames) {
            int type = AppTypeRecoManager.getInstance().getAppType(pkgName);
            if (type != -1) {
                return type;
            }
        }
        return -1;
    }

    private boolean isNaviOrSportApp(int uid) {
        int type = getAppTypeFromHabit(uid);
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getAppTypeFromHabit uid ");
            stringBuilder.append(uid);
            stringBuilder.append(" type : ");
            stringBuilder.append(type);
            AwareLog.d(str, stringBuilder.toString());
        }
        boolean z = true;
        if (type != -1) {
            switch (type) {
                case 2:
                case 3:
                    break;
                default:
                    if (type <= 255) {
                        z = false;
                    }
                    return z;
            }
        }
        return true;
    }

    private boolean shouldFilter(int stateType, int uid) {
        return (stateType == 3 || stateType == 4) && !isNaviOrSportApp(uid);
    }

    private void registerProcessObserver() {
        try {
            ActivityManagerNative.getDefault().registerProcessObserver(this.mProcessObserver);
        } catch (RemoteException e) {
            AwareLog.d(TAG, "AwareAppKeyBackgroup register process observer failed");
        }
    }

    private void unregisterProcessObserver() {
        try {
            ActivityManagerNative.getDefault().unregisterProcessObserver(this.mProcessObserver);
        } catch (RemoteException e) {
            AwareLog.d(TAG, "AwareAppKeyBackgroup unregister process observer failed");
        }
    }

    protected void registerCallStateListener(Context cxt) {
        if (cxt != null) {
            ((TelephonyManager) cxt.getSystemService("phone")).listen(this.mCallStateListener, 32);
        }
    }

    protected void unregisterCallStateListener(Context cxt) {
        if (cxt != null) {
            ((TelephonyManager) cxt.getSystemService("phone")).listen(this.mCallStateListener, 0);
        }
    }

    private final void updateCallState(int state) {
        if (this.mIsInitialized.get()) {
            int eventType = state == 0 ? 2 : 1;
            synchronized (this) {
                updateSceneArrayLocked(2, eventType, 0, CALL_APP_PKG, 0);
            }
            if (eventType == 2) {
                synchronized (this.mAudioCacheUids) {
                    this.mAudioCacheUids.clear();
                }
            }
        }
    }

    private AwareAppKeyBackgroup() {
        this.mIsInitialized = new AtomicBoolean(false);
        this.mLastSetting = new AtomicBoolean(false);
        this.mIsInitializing = new AtomicBoolean(false);
        this.mPGSdk = null;
        this.mContext = null;
        this.mHwAMS = null;
        this.mPM = null;
        this.mProcessObserver = new AwareABGProcessObserver();
        this.mIsAbroadArea = false;
        this.mAmapAppUid = 0;
        this.mScenePidArray = new ArrayList();
        this.mSceneUidArray = new ArrayList();
        this.mScenePkgArray = new ArrayList();
        this.mKeyBackgroupPids = new ArraySet();
        this.mKeyBackgroupUids = new ArraySet();
        this.mKeyBackgroupPkgs = new ArraySet();
        this.mStateEventDecayInfos = new ArrayList();
        this.mCallbacks = new ArrayMap();
        this.mHistorySensorRecords = new SparseArray();
        this.mAudioCacheUids = new ArraySet();
        this.mNatTimeout = 0;
        this.mStateRecognitionListener = new Sink() {
            public void onStateChanged(int stateType, int eventType, int pid, String pkg, int uid) {
                if (!AwareAppKeyBackgroup.this.mIsInitialized.get() || !AwareAppKeyBackgroup.this.checkCallingPermission() || pid == Process.myPid()) {
                    return;
                }
                if (stateType == 11) {
                    AwareAppKeyBackgroup.this.setNatTimeout(pid);
                } else if (!AwareAppKeyBackgroup.this.shouldFilter(stateType, uid)) {
                    if (stateType == 4) {
                        AwareAppKeyBackgroup awareAppKeyBackgroup = AwareAppKeyBackgroup.this;
                        boolean z = true;
                        if (eventType != 1) {
                            z = false;
                        }
                        awareAppKeyBackgroup.handleSensorEvent(uid, pid, z);
                        return;
                    }
                    long timeStamp = 0;
                    if (AwareAppKeyBackgroup.DEBUG) {
                        AwareLog.d(AwareAppKeyBackgroup.TAG, "PGSdk Sink onStateChanged");
                        timeStamp = SystemClock.currentTimeMicro();
                    }
                    if (!AwareAppKeyBackgroup.this.isStateChangedDecay(stateType, eventType, pid, pkg, uid)) {
                        AwareAppKeyBackgroup.this.updateSceneState(stateType, eventType, pid, pkg, uid);
                        AwareAppKeyBackgroup.this.checkAssociateApp(stateType, eventType, pid, pkg, uid);
                        if (AwareAppKeyBackgroup.DEBUG) {
                            String str = AwareAppKeyBackgroup.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Update Scene state using ");
                            stringBuilder.append(SystemClock.currentTimeMicro() - timeStamp);
                            stringBuilder.append(" us");
                            AwareLog.d(str, stringBuilder.toString());
                        }
                    }
                }
            }
        };
        this.mCallStateListener = new PhoneStateListener() {
            public void onCallStateChanged(int state, String incomingNumber) {
                if (AwareAppKeyBackgroup.DEBUG) {
                    String str = AwareAppKeyBackgroup.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onCallStateChanged state :");
                    stringBuilder.append(state);
                    AwareLog.d(str, stringBuilder.toString());
                }
                AwareAppKeyBackgroup.this.updateCallState(state);
            }
        };
        this.mAppKeyHandler = new AppKeyHandler(BackgroundThread.get().getLooper());
        this.mHwAMS = HwActivityManagerService.self();
    }

    public void registerStateCallback(IAwareStateCallback callback, int stateType) {
        if (callback != null) {
            synchronized (this.mCallbacks) {
                ArraySet<Integer> states = (ArraySet) this.mCallbacks.get(callback);
                if (states == null) {
                    states = new ArraySet();
                    states.add(Integer.valueOf(stateType));
                    this.mCallbacks.put(callback, states);
                } else {
                    states.add(Integer.valueOf(stateType));
                }
            }
        }
    }

    public void unregisterStateCallback(IAwareStateCallback callback, int stateType) {
        if (callback != null) {
            synchronized (this.mCallbacks) {
                ArraySet<Integer> states = (ArraySet) this.mCallbacks.get(callback);
                if (states != null) {
                    states.remove(Integer.valueOf(stateType));
                    if (states.size() == 0) {
                        this.mCallbacks.remove(callback);
                    }
                }
            }
        }
    }

    private void notifyStateChange(int stateType, int eventType, int pid, int uid) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("keyBackgroup onStateChanged e:");
            stringBuilder.append(eventType);
            stringBuilder.append(" pid:");
            stringBuilder.append(pid);
            stringBuilder.append(" uid:");
            stringBuilder.append(uid);
            AwareLog.d(str, stringBuilder.toString());
        }
        synchronized (this.mCallbacks) {
            if (this.mCallbacks.isEmpty()) {
                return;
            }
            for (Entry<IAwareStateCallback, ArraySet<Integer>> m : this.mCallbacks.entrySet()) {
                IAwareStateCallback callback = (IAwareStateCallback) m.getKey();
                ArraySet<Integer> states = (ArraySet) m.getValue();
                if (states != null && (states.contains(Integer.valueOf(stateType)) || states.contains(Integer.valueOf(100)))) {
                    callback.onStateChanged(stateType, eventType, pid == -1 ? 0 : pid, uid);
                }
            }
        }
    }

    private void initialize(Context context) {
        this.mLastSetting.set(true);
        if (!this.mIsInitialized.get() && !this.mIsInitializing.get()) {
            this.mContext = context;
            this.mIsAbroadArea = AwareDefaultConfigList.isAbroadArea();
            registerProcessObserver();
            registerCallStateListener(this.mContext);
            if (this.mAppKeyHandler.hasMessages(3)) {
                this.mAppKeyHandler.removeMessages(3);
            }
            this.mAppKeyHandler.sendEmptyMessage(3);
        }
    }

    private void doInitialize() {
        this.mIsInitializing.set(true);
        synchronized (this) {
            if (this.mScenePidArray.isEmpty()) {
                for (int i = 0; i < STATE_SIZE; i++) {
                    this.mScenePidArray.add(new ArraySet());
                    this.mSceneUidArray.add(new ArraySet());
                    this.mScenePkgArray.add(new ArraySet());
                }
            }
        }
        if (ensureInitialize()) {
            this.mIsInitializing.set(false);
            checkLastSetting();
            return;
        }
        if (this.mAppKeyHandler.hasMessages(3)) {
            this.mAppKeyHandler.removeMessages(3);
        }
        this.mAppKeyHandler.sendEmptyMessageDelayed(3, PGSDK_REINIT_TIME);
    }

    private boolean ensureInitialize() {
        if (!this.mIsInitialized.get()) {
            this.mPGSdk = PGSdk.getInstance();
            if (this.mPGSdk == null) {
                return this.mIsInitialized.get();
            }
            try {
                this.mPGSdk.enableStateEvent(this.mStateRecognitionListener, 1);
                this.mPGSdk.enableStateEvent(this.mStateRecognitionListener, 2);
                this.mPGSdk.enableStateEvent(this.mStateRecognitionListener, 3);
                this.mPGSdk.enableStateEvent(this.mStateRecognitionListener, 4);
                this.mPGSdk.enableStateEvent(this.mStateRecognitionListener, 5);
                this.mPGSdk.enableStateEvent(this.mStateRecognitionListener, 11);
                resumeAppStates(this.mContext);
                this.mIsInitialized.set(true);
            } catch (RemoteException e) {
                AwareLog.e(TAG, "PG Exception e: initialize pgdskd error!");
            }
        }
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AwareAppKeyBackgroup ensureInitialize:");
            stringBuilder.append(this.mIsInitialized.get());
            AwareLog.d(str, stringBuilder.toString());
        }
        return this.mIsInitialized.get();
    }

    private void deInitialize() {
        unregisterProcessObserver();
        unregisterCallStateListener(this.mContext);
        this.mLastSetting.set(false);
        if (!this.mIsInitialized.get()) {
            this.mAppKeyHandler.removeMessages(3);
        } else if (this.mPGSdk != null) {
            try {
                this.mPGSdk.disableStateEvent(this.mStateRecognitionListener, 1);
                this.mPGSdk.disableStateEvent(this.mStateRecognitionListener, 2);
                this.mPGSdk.disableStateEvent(this.mStateRecognitionListener, 3);
                this.mPGSdk.disableStateEvent(this.mStateRecognitionListener, 4);
                this.mPGSdk.disableStateEvent(this.mStateRecognitionListener, 5);
                this.mPGSdk.disableStateEvent(this.mStateRecognitionListener, 11);
                synchronized (this) {
                    this.mScenePidArray.clear();
                    this.mScenePkgArray.clear();
                    this.mSceneUidArray.clear();
                    this.mKeyBackgroupPids.clear();
                    this.mKeyBackgroupUids.clear();
                    this.mKeyBackgroupPkgs.clear();
                    this.mHistorySensorRecords.clear();
                }
                synchronized (this.mStateEventDecayInfos) {
                    this.mStateEventDecayInfos.clear();
                }
                synchronized (this.mAudioCacheUids) {
                    this.mAudioCacheUids.clear();
                }
            } catch (RemoteException e) {
                try {
                    AwareLog.e(TAG, "PG Exception e: deinitialize pgsdk error!");
                    synchronized (this) {
                        this.mScenePidArray.clear();
                        this.mScenePkgArray.clear();
                        this.mSceneUidArray.clear();
                        this.mKeyBackgroupPids.clear();
                        this.mKeyBackgroupUids.clear();
                        this.mKeyBackgroupPkgs.clear();
                        this.mHistorySensorRecords.clear();
                        synchronized (this.mStateEventDecayInfos) {
                            this.mStateEventDecayInfos.clear();
                            synchronized (this.mAudioCacheUids) {
                                this.mAudioCacheUids.clear();
                            }
                        }
                    }
                } catch (Throwable th) {
                    synchronized (this) {
                        this.mScenePidArray.clear();
                        this.mScenePkgArray.clear();
                        this.mSceneUidArray.clear();
                        this.mKeyBackgroupPids.clear();
                        this.mKeyBackgroupUids.clear();
                        this.mKeyBackgroupPkgs.clear();
                        this.mHistorySensorRecords.clear();
                        synchronized (this.mStateEventDecayInfos) {
                            this.mStateEventDecayInfos.clear();
                            synchronized (this.mAudioCacheUids) {
                                this.mAudioCacheUids.clear();
                                this.mAppKeyHandler.removeCallbacksAndMessages(null);
                                this.mIsInitialized.set(false);
                            }
                        }
                    }
                }
            }
            this.mAppKeyHandler.removeCallbacksAndMessages(null);
            this.mIsInitialized.set(false);
            checkLastSetting();
        } else {
            return;
        }
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("PGFeature deInitialize:");
            stringBuilder.append(this.mIsInitialized.get());
            AwareLog.d(str, stringBuilder.toString());
        }
    }

    private void checkLastSetting() {
        if (!(this.mContext == null || this.mIsInitialized.get() == this.mLastSetting.get())) {
            if (this.mLastSetting.get()) {
                getInstance().initialize(this.mContext);
            } else {
                getInstance().deInitialize();
            }
        }
    }

    public static void enable(Context context) {
        if (DEBUG) {
            AwareLog.d(TAG, "KeyBackGroup Feature enable!!!");
        }
        getInstance().initialize(context);
    }

    public static void disable() {
        if (DEBUG) {
            AwareLog.d(TAG, "KeyBackGroup Feature disable!!!");
        }
        getInstance().deInitialize();
    }

    public static void enableDebug() {
        DEBUG = true;
    }

    public static void disableDebug() {
        DEBUG = false;
    }

    public static AwareAppKeyBackgroup getInstance() {
        AwareAppKeyBackgroup awareAppKeyBackgroup;
        synchronized (AwareAppKeyBackgroup.class) {
            if (sInstance == null) {
                sInstance = new AwareAppKeyBackgroup();
            }
            awareAppKeyBackgroup = sInstance;
        }
        return awareAppKeyBackgroup;
    }

    /* JADX WARNING: Removed duplicated region for block: B:48:0x00ea  */
    /* JADX WARNING: Removed duplicated region for block: B:47:0x00ce  */
    /* JADX WARNING: Removed duplicated region for block: B:47:0x00ce  */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x00ea  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void resumeAppStates(Context context) {
        int i;
        ArrayList<String> packages;
        boolean state;
        boolean state2;
        RemoteException e;
        int k;
        Context context2 = context;
        if (context2 != null) {
            ArrayList<ProcessInfo> procs = ProcessInfoCollector.getInstance().getProcessInfoList();
            long timeStamp = 0;
            if (DEBUG) {
                timeStamp = SystemClock.currentTimeMicro();
            }
            long timeStamp2 = timeStamp;
            int size = procs.size();
            int j = 0;
            while (true) {
                int size2 = size;
                if (j >= size2) {
                    break;
                }
                ProcessInfo procInfo = (ProcessInfo) procs.get(j);
                if (procInfo != null) {
                    int pid = procInfo.mPid;
                    int uid = procInfo.mUid;
                    ArrayList<String> packages2 = procInfo.mPackageName;
                    int i2 = 1;
                    while (true) {
                        int i3 = i2;
                        if (i3 > 5) {
                            break;
                        }
                        int uid2;
                        ArrayList<String> packages3;
                        int uid3;
                        if (shouldFilter(i3, uid)) {
                            i = i3;
                            packages = packages2;
                            uid2 = uid;
                        } else if (i3 <= 2) {
                            boolean state3;
                            state = false;
                            try {
                                state3 = this.mPGSdk.checkStateByPid(context2, pid, i3);
                            } catch (RemoteException e2) {
                                AwareLog.e(TAG, "checkStateByPid occur exception.");
                                state3 = state;
                            }
                            if (state3) {
                                uid2 = i3;
                                packages3 = packages2;
                                uid3 = uid;
                                updateSceneState(i3, 1, pid, null, uid);
                            } else {
                                uid2 = i3;
                                packages3 = packages2;
                                uid3 = uid;
                            }
                            packages = packages3;
                            i = uid2;
                            uid2 = uid3;
                        } else {
                            packages3 = packages2;
                            uid3 = uid;
                            uid = i3;
                            int uid4;
                            if (uid == 4) {
                                uid4 = uid3;
                                initAppSensorState(context2, uid4);
                                uid2 = uid4;
                                packages = packages3;
                                i = uid;
                            } else {
                                uid4 = uid3;
                                ArrayList<String> packages4 = packages3;
                                size = packages4.size();
                                int k2 = 0;
                                while (true) {
                                    int psize = size;
                                    if (k2 >= psize) {
                                        break;
                                    }
                                    state = false;
                                    try {
                                        state2 = state;
                                        try {
                                            state2 = this.mPGSdk.checkStateByPkg(context2, (String) packages4.get(k2), uid);
                                        } catch (RemoteException e3) {
                                            e = e3;
                                            AwareLog.e(TAG, "checkStateByPkg occur exception.");
                                            if (state2) {
                                            }
                                            k2 = k + 1;
                                            uid4 = uid2;
                                            uid = i;
                                            size = uid3;
                                            packages4 = packages;
                                        }
                                    } catch (RemoteException e4) {
                                        e = e4;
                                        state2 = state;
                                        AwareLog.e(TAG, "checkStateByPkg occur exception.");
                                        if (state2) {
                                        }
                                        k2 = k + 1;
                                        uid4 = uid2;
                                        uid = i;
                                        size = uid3;
                                        packages4 = packages;
                                    }
                                    if (state2) {
                                        uid3 = psize;
                                        k = k2;
                                        packages = packages4;
                                        uid2 = uid4;
                                        i = uid;
                                        updateSceneState(uid, 1, 0, null, uid2);
                                    } else {
                                        uid3 = psize;
                                        k = k2;
                                        packages = packages4;
                                        uid2 = uid4;
                                        i = uid;
                                    }
                                    k2 = k + 1;
                                    uid4 = uid2;
                                    uid = i;
                                    size = uid3;
                                    packages4 = packages;
                                }
                                packages = packages4;
                                uid2 = uid4;
                                i = uid;
                            }
                        }
                        i2 = i + 1;
                        uid = uid2;
                        packages2 = packages;
                    }
                }
                j++;
                size = size2;
            }
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("resumeAppStates done using ");
                stringBuilder.append(SystemClock.currentTimeMicro() - timeStamp2);
                stringBuilder.append(" us");
                AwareLog.d(str, stringBuilder.toString());
            }
        }
    }

    private boolean isStateChangedDecay(int stateType, int eventType, int pid, String pkg, int uid) {
        if (this.mContext == null) {
            return false;
        }
        if (!isNeedDecay(stateType, eventType)) {
            checkStateChangedDecay(stateType, eventType, pid, pkg, uid);
            return false;
        } else if (!isAppAlive(this.mContext, uid)) {
            return false;
        } else {
            sendDecayMesssage(1, addDecayInfo(stateType, eventType, pid, pkg, uid), stateType == 5 ? 10000 : 60000);
            return true;
        }
    }

    private boolean isNeedDecay(int stateType, int eventType) {
        return (stateType == 2 || stateType == 5) && eventType == 2;
    }

    private void checkStateChangedDecay(int stateType, int eventType, int pid, String pkg, int uid) {
        if (isNeedCheckDecay(stateType, eventType)) {
            DecayInfo decayInfo = removeDecayInfo(stateType, 2, pid, pkg, uid);
            if (decayInfo != null) {
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("checkStateChangedDecay start has message 1 ? ");
                    stringBuilder.append(this.mAppKeyHandler.hasMessages(1, decayInfo));
                    stringBuilder.append(" decayinfo");
                    stringBuilder.append(decayInfo);
                    stringBuilder.append(" size ");
                    stringBuilder.append(getStateEventDecayInfosSize());
                    AwareLog.d(str, stringBuilder.toString());
                }
                this.mAppKeyHandler.removeMessages(1, decayInfo);
            }
        }
    }

    private boolean isNeedCheckDecay(int stateType, int eventType) {
        return (stateType == 2 || stateType == 5) && eventType == 1 && getStateEventDecayInfosSize() > 0;
    }

    private DecayInfo getDecayInfo(int stateType, int eventType, int pid, String pkg, int uid) {
        synchronized (this.mStateEventDecayInfos) {
            for (DecayInfo info : this.mStateEventDecayInfos) {
                if (info.getStateType() == stateType && info.getEventType() == eventType && info.getPid() == pid && info.getUid() == uid) {
                    return info;
                }
            }
            return null;
        }
    }

    private boolean existDecayInfo(int stateType, int eventType, int pid, String pkg, int uid) {
        synchronized (this.mStateEventDecayInfos) {
            for (DecayInfo info : this.mStateEventDecayInfos) {
                if (info.getStateType() == stateType && info.getEventType() == eventType && info.getPid() == pid && info.getUid() == uid) {
                    return true;
                }
            }
            return false;
        }
    }

    private DecayInfo addDecayInfo(int stateType, int eventType, int pid, String pkg, int uid) {
        DecayInfo decayInfo;
        synchronized (this.mStateEventDecayInfos) {
            decayInfo = getDecayInfo(stateType, eventType, pid, pkg, uid);
            if (decayInfo == null) {
                decayInfo = new DecayInfo(stateType, eventType, pid, pkg, uid);
                this.mStateEventDecayInfos.add(decayInfo);
            }
        }
        return decayInfo;
    }

    private DecayInfo removeDecayInfo(int stateType, int eventType, int pid, String pkg, int uid) {
        synchronized (this.mStateEventDecayInfos) {
            DecayInfo decayInfo = getDecayInfo(stateType, eventType, pid, pkg, uid);
            if (decayInfo != null) {
                this.mStateEventDecayInfos.remove(decayInfo);
                return decayInfo;
            }
            return null;
        }
    }

    private int getStateEventDecayInfosSize() {
        int size;
        synchronized (this.mStateEventDecayInfos) {
            size = this.mStateEventDecayInfos.size();
        }
        return size;
    }

    private List<ProcessInfo> getProcessesByUid(int uid) {
        ArrayList<ProcessInfo> procList = ProcessInfoCollector.getInstance().getProcessInfoList();
        if (procList.isEmpty()) {
            return null;
        }
        List<ProcessInfo> procs = new ArrayList();
        int size = procList.size();
        for (int i = 0; i < size; i++) {
            ProcessInfo info = (ProcessInfo) procList.get(i);
            if (info != null && uid == info.mUid) {
                procs.add(info);
            }
        }
        return procs;
    }

    private boolean isInCalling() {
        if (checkKeyBackgroupByState(2, 0, 0, CALL_APP_PKG)) {
            return true;
        }
        ArrayList<ProcessInfo> procList = ProcessInfoCollector.getInstance().getProcessInfoList();
        if (procList.isEmpty()) {
            return false;
        }
        int i = 0;
        int size = procList.size();
        while (i < size) {
            if (procList.get(i) != null && ((ProcessInfo) procList.get(i)).mPackageName != null && ((ProcessInfo) procList.get(i)).mPackageName.contains(WECHAT_APP_PKG) && checkKeyBackgroupByState(2, ((ProcessInfo) procList.get(i)).mPid, ((ProcessInfo) procList.get(i)).mUid, WECHAT_APP_PKG)) {
                return true;
            }
            i++;
        }
        return false;
    }

    private void updateAudioCache(int pid, int uid) {
        boolean iscalling = isInCalling();
        synchronized (this.mAudioCacheUids) {
            if (iscalling) {
                try {
                    this.mAudioCacheUids.add(Integer.valueOf(uid));
                } catch (Throwable th) {
                }
            } else {
                this.mAudioCacheUids.clear();
            }
        }
    }

    public boolean isAudioCache(int uid) {
        if (!this.mIsInitialized.get()) {
            return false;
        }
        boolean contains;
        synchronized (this.mAudioCacheUids) {
            contains = this.mAudioCacheUids.contains(Integer.valueOf(uid));
        }
        return contains;
    }

    private boolean isAppAlive(Context cxt, int uid) {
        if (cxt == null) {
            return false;
        }
        Map<Integer, AwareProcessBaseInfo> baseInfos = this.mHwAMS != null ? this.mHwAMS.getAllProcessBaseInfo() : null;
        if (baseInfos == null || baseInfos.isEmpty()) {
            return false;
        }
        for (Entry entry : baseInfos.entrySet()) {
            AwareProcessBaseInfo valueInfo = (AwareProcessBaseInfo) entry.getValue();
            if (valueInfo != null) {
                if (valueInfo.copy().mUid == uid) {
                    return true;
                }
            }
        }
        return false;
    }

    private void checkAssociateApp(int stateType, int eventType, int pid, String pkg, int uid) {
        if (stateType == 3) {
            sendDecayMesssage(4, new DecayInfo(stateType, eventType, pid, pkg, uid), 0);
        }
    }

    private void checkHuaweiAmapApp(int stateType, int eventType, int uid) {
        if (this.mAmapAppUid == 0) {
            this.mAmapAppUid = getUidByPkg(HUAWEI_AMAP_APP_PKG);
        }
        if (this.mAmapAppUid > 0 && this.mAmapAppUid == uid) {
            Set<String> strong = new ArraySet();
            if (eventType == 1) {
                AwareAppAssociate.getInstance().getAssocClientListForUid(uid, strong);
                if (strong.isEmpty()) {
                    return;
                }
            }
            synchronized (this) {
                updateGPSPkgArrayLocked(stateType, eventType, strong);
            }
        }
    }

    private int getUidByPkg(String pkg) {
        ArrayList<ProcessInfo> procList = ProcessInfoCollector.getInstance().getProcessInfoList();
        if (procList.isEmpty()) {
            return 0;
        }
        int size = procList.size();
        for (int i = 0; i < size; i++) {
            ProcessInfo info = (ProcessInfo) procList.get(i);
            if (info != null && info.mPackageName != null && info.mPackageName.contains(pkg)) {
                return info.mUid;
            }
        }
        return 0;
    }

    private void updateGPSPkgArrayLocked(int stateType, int eventType, Set<String> pkgs) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateGPSPkgArrayLocked eventType : ");
            stringBuilder.append(eventType);
            stringBuilder.append(" pkgs : ");
            stringBuilder.append(pkgs);
            AwareLog.d(str, stringBuilder.toString());
        }
        if (!this.mScenePkgArray.isEmpty()) {
            if (eventType != 1) {
                ((ArraySet) this.mScenePkgArray.get(stateType)).clear();
            } else if (this.mIsAbroadArea) {
                ((ArraySet) this.mScenePkgArray.get(stateType)).addAll(pkgs);
            } else {
                for (String pkg : pkgs) {
                    if (AppTypeRecoManager.getInstance().getAppType(pkg) == 3) {
                        ((ArraySet) this.mScenePkgArray.get(stateType)).add(pkg);
                    }
                }
            }
        }
    }

    private void removeDecayForProcessDied(int stateType, int eventType, int pid, String pkg, int uid) {
        if (!isNeedDecay(stateType, eventType) || !existDecayInfo(stateType, eventType, pid, pkg, uid)) {
            return;
        }
        if (pid != 0 || !isAppAlive(this.mContext, uid)) {
            DecayInfo decayInfo = removeDecayInfo(stateType, eventType, pid, pkg, uid);
            if (decayInfo != null) {
                updateSceneState(decayInfo.getStateType(), decayInfo.getEventType(), decayInfo.getPid(), decayInfo.getPkg(), decayInfo.getUid());
                this.mAppKeyHandler.removeMessages(1, decayInfo);
            }
        }
    }

    public boolean checkIsKeyBackgroupInternal(int pid, int uid) {
        if (!this.mIsInitialized.get()) {
            return false;
        }
        synchronized (this) {
            if (this.mKeyBackgroupPids.contains(Integer.valueOf(pid))) {
                return true;
            } else if (this.mKeyBackgroupUids.contains(Integer.valueOf(uid))) {
                return true;
            } else {
                return false;
            }
        }
    }

    /* JADX WARNING: Missing block: B:12:0x001d, code skipped:
            monitor-enter(r4);
     */
    /* JADX WARNING: Missing block: B:15:0x0028, code skipped:
            if (r4.mKeyBackgroupPids.contains(java.lang.Integer.valueOf(r5)) == false) goto L_0x0030;
     */
    /* JADX WARNING: Missing block: B:16:0x002a, code skipped:
            r0 = getKeyBackgroupTypeByPidLocked(r5);
     */
    /* JADX WARNING: Missing block: B:17:0x002e, code skipped:
            monitor-exit(r4);
     */
    /* JADX WARNING: Missing block: B:18:0x002f, code skipped:
            return r0;
     */
    /* JADX WARNING: Missing block: B:20:0x003a, code skipped:
            if (r4.mKeyBackgroupUids.contains(java.lang.Integer.valueOf(r6)) == false) goto L_0x0042;
     */
    /* JADX WARNING: Missing block: B:21:0x003c, code skipped:
            r0 = getKeyBackgroupTypeByUidLocked(r6);
     */
    /* JADX WARNING: Missing block: B:22:0x0040, code skipped:
            monitor-exit(r4);
     */
    /* JADX WARNING: Missing block: B:23:0x0041, code skipped:
            return r0;
     */
    /* JADX WARNING: Missing block: B:24:0x0042, code skipped:
            if (r7 != null) goto L_0x0046;
     */
    /* JADX WARNING: Missing block: B:25:0x0044, code skipped:
            monitor-exit(r4);
     */
    /* JADX WARNING: Missing block: B:26:0x0045, code skipped:
            return -1;
     */
    /* JADX WARNING: Missing block: B:27:0x0046, code skipped:
            r0 = getKeyBackgroupTypeByPkgsLocked(r7);
     */
    /* JADX WARNING: Missing block: B:28:0x004a, code skipped:
            monitor-exit(r4);
     */
    /* JADX WARNING: Missing block: B:29:0x004b, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getKeyBackgroupTypeInternal(int pid, int uid, List<String> pkgs) {
        if (!this.mIsInitialized.get()) {
            return -1;
        }
        synchronized (this.mAudioCacheUids) {
            if (this.mAudioCacheUids.contains(Integer.valueOf(uid))) {
                return 2;
            }
        }
    }

    public boolean checkIsKeyBackgroup(int pid, int uid) {
        if (!this.mIsInitialized.get()) {
            return false;
        }
        if (AwareDefaultConfigList.getInstance().getKeyHabitAppList().contains(InnerUtils.getAwarePkgName(pid))) {
            return true;
        }
        return checkIsKeyBackgroupInternal(pid, uid);
    }

    /* JADX WARNING: Missing block: B:15:0x0022, code skipped:
            monitor-enter(r6);
     */
    /* JADX WARNING: Missing block: B:18:0x0029, code skipped:
            if (r6.mSceneUidArray.isEmpty() == false) goto L_0x002d;
     */
    /* JADX WARNING: Missing block: B:19:0x002b, code skipped:
            monitor-exit(r6);
     */
    /* JADX WARNING: Missing block: B:20:0x002c, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:22:0x003d, code skipped:
            if (((android.util.ArraySet) r6.mSceneUidArray.get(r7)).contains(java.lang.Integer.valueOf(r9)) == false) goto L_0x0041;
     */
    /* JADX WARNING: Missing block: B:23:0x003f, code skipped:
            monitor-exit(r6);
     */
    /* JADX WARNING: Missing block: B:24:0x0040, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:26:0x0051, code skipped:
            if (((android.util.ArraySet) r6.mScenePidArray.get(r7)).contains(java.lang.Integer.valueOf(r8)) == false) goto L_0x0055;
     */
    /* JADX WARNING: Missing block: B:27:0x0053, code skipped:
            monitor-exit(r6);
     */
    /* JADX WARNING: Missing block: B:28:0x0054, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:29:0x0055, code skipped:
            if (r10 == null) goto L_0x0084;
     */
    /* JADX WARNING: Missing block: B:31:0x005b, code skipped:
            if (r10.isEmpty() == false) goto L_0x005e;
     */
    /* JADX WARNING: Missing block: B:33:0x005e, code skipped:
            r0 = (android.util.ArraySet) r6.mScenePkgArray.get(r7);
            r2 = r10.iterator();
     */
    /* JADX WARNING: Missing block: B:35:0x006e, code skipped:
            if (r2.hasNext() == false) goto L_0x0082;
     */
    /* JADX WARNING: Missing block: B:36:0x0070, code skipped:
            r4 = (java.lang.String) r2.next();
     */
    /* JADX WARNING: Missing block: B:37:0x0076, code skipped:
            if (r4 != null) goto L_0x0079;
     */
    /* JADX WARNING: Missing block: B:40:0x007d, code skipped:
            if (r0.contains(r4) == false) goto L_0x0081;
     */
    /* JADX WARNING: Missing block: B:41:0x007f, code skipped:
            monitor-exit(r6);
     */
    /* JADX WARNING: Missing block: B:42:0x0080, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:44:0x0082, code skipped:
            monitor-exit(r6);
     */
    /* JADX WARNING: Missing block: B:45:0x0083, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:46:0x0084, code skipped:
            monitor-exit(r6);
     */
    /* JADX WARNING: Missing block: B:47:0x0085, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean checkKeyBackgroupByState(int state, int pid, int uid, List<String> pkgs) {
        if (!this.mIsInitialized.get()) {
            return false;
        }
        synchronized (this.mAudioCacheUids) {
            if (2 == state) {
                try {
                    if (this.mAudioCacheUids.contains(Integer.valueOf(uid))) {
                        return true;
                    }
                } catch (Throwable th) {
                    while (true) {
                    }
                }
            }
        }
    }

    private boolean checkKeyBackgroupByState(int state, int pid, int uid, String pkg) {
        if (!this.mIsInitialized.get()) {
            return false;
        }
        synchronized (this) {
            if (this.mSceneUidArray.isEmpty()) {
                return false;
            } else if (((ArraySet) this.mSceneUidArray.get(state)).contains(Integer.valueOf(uid))) {
                return true;
            } else if (((ArraySet) this.mScenePidArray.get(state)).contains(Integer.valueOf(pid))) {
                return true;
            } else if (((ArraySet) this.mScenePkgArray.get(state)).contains(pkg)) {
                return true;
            } else {
                return false;
            }
        }
    }

    private void updateSceneArrayProcessDied(int uid, int pid) {
        synchronized (this) {
            boolean appAlive = isAppAlive(this.mContext, uid);
            int size = this.mScenePidArray.size();
            for (int i = 1; i < size; i++) {
                if (((ArraySet) this.mScenePidArray.get(i)).contains(Integer.valueOf(pid))) {
                    ((ArraySet) this.mSceneUidArray.get(i)).remove(Integer.valueOf(uid));
                    ((ArraySet) this.mScenePidArray.get(i)).remove(Integer.valueOf(pid));
                    this.mKeyBackgroupUids.remove(Integer.valueOf(pid));
                } else if (!(uid == 0 || appAlive)) {
                    ((ArraySet) this.mSceneUidArray.get(i)).remove(Integer.valueOf(uid));
                }
            }
            updateKbgUidsArrayLocked();
        }
    }

    private void updateScenePidArrayForInvalidPid(int uid) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateScenePidArrayForInvalidPid uid ");
            stringBuilder.append(uid);
            AwareLog.d(str, stringBuilder.toString());
        }
        List<ProcessInfo> procs = getProcessesByUid(uid);
        if (procs != null && !procs.isEmpty()) {
            for (ProcessInfo info : procs) {
                if (info != null) {
                    if (DEBUG) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("updateScenePidArrayForInvalidPid pid ");
                        stringBuilder2.append(info.mPid);
                        AwareLog.d(str2, stringBuilder2.toString());
                    }
                    synchronized (this) {
                        int size = this.mScenePidArray.size();
                        for (int i = 1; i < size; i++) {
                            ((ArraySet) this.mScenePidArray.get(i)).remove(Integer.valueOf(info.mPid));
                        }
                        this.mKeyBackgroupUids.remove(Integer.valueOf(info.mPid));
                    }
                }
            }
        }
    }

    private void updateKbgUidsArrayLocked() {
        this.mKeyBackgroupUids.clear();
        int size = this.mSceneUidArray.size();
        for (int i = 0; i < size; i++) {
            this.mKeyBackgroupUids.addAll((ArraySet) this.mSceneUidArray.get(i));
        }
    }

    private void updateSceneArrayLocked(int stateType, int eventType, int pid, String pkg, int uid) {
        if (!this.mSceneUidArray.isEmpty()) {
            if (eventType == 1) {
                if (pid != 0) {
                    ((ArraySet) this.mScenePidArray.get(stateType)).add(Integer.valueOf(pid));
                }
                if (uid != 0) {
                    ((ArraySet) this.mSceneUidArray.get(stateType)).add(Integer.valueOf(uid));
                }
                if (!(pkg == null || pkg.isEmpty())) {
                    ((ArraySet) this.mScenePkgArray.get(stateType)).add(pkg);
                }
            } else if (eventType == 2) {
                if (pid != 0) {
                    ((ArraySet) this.mScenePidArray.get(stateType)).remove(Integer.valueOf(pid));
                }
                if (uid != 0) {
                    ((ArraySet) this.mSceneUidArray.get(stateType)).remove(Integer.valueOf(uid));
                }
                if (!(pkg == null || pkg.isEmpty())) {
                    ((ArraySet) this.mScenePkgArray.get(stateType)).remove(pkg);
                }
            }
        }
    }

    private void updateSceneState(int stateType, int eventType, int pid, String pkg, int uid) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("state type: ");
            stringBuilder.append(stateType);
            stringBuilder.append(" eventType:");
            stringBuilder.append(eventType);
            stringBuilder.append(" pid:");
            stringBuilder.append(pid);
            stringBuilder.append(" uid:");
            stringBuilder.append(uid);
            stringBuilder.append(" pkg:");
            stringBuilder.append(pkg);
            AwareLog.d(str, stringBuilder.toString());
        }
        if (stateType >= 1 && stateType < STATE_SIZE) {
            int realpid = pid;
            if (pid == -1) {
                updateScenePidArrayForInvalidPid(uid);
                realpid = 0;
            }
            notifyStateChange(stateType, eventType, realpid, uid);
            synchronized (this) {
                updateSceneArrayLocked(stateType, eventType, realpid, pkg, uid);
                if (eventType == 1 && !this.mKeyBackgroupUids.contains(Integer.valueOf(uid))) {
                    notifyStateChange(0, eventType, realpid, uid);
                }
                this.mKeyBackgroupPids.clear();
                this.mKeyBackgroupUids.clear();
                this.mKeyBackgroupPkgs.clear();
                int size = this.mScenePidArray.size();
                for (int i = 0; i < size; i++) {
                    this.mKeyBackgroupPids.addAll((ArraySet) this.mScenePidArray.get(i));
                    this.mKeyBackgroupUids.addAll((ArraySet) this.mSceneUidArray.get(i));
                    this.mKeyBackgroupPkgs.addAll((ArraySet) this.mScenePkgArray.get(i));
                }
                if (eventType == 2 && !this.mKeyBackgroupUids.contains(Integer.valueOf(uid))) {
                    notifyStateChange(0, eventType, realpid, uid);
                }
                if (DEBUG && this.mScenePidArray.size() > stateType) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("stateChanged ");
                    stringBuilder2.append(stateToString(stateType));
                    stringBuilder2.append(" mPids:");
                    stringBuilder2.append(this.mScenePidArray.get(stateType));
                    AwareLog.d(str2, stringBuilder2.toString());
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("stateChanged ");
                    stringBuilder2.append(stateToString(stateType));
                    stringBuilder2.append(" mUids:");
                    stringBuilder2.append(this.mSceneUidArray.get(stateType));
                    AwareLog.d(str2, stringBuilder2.toString());
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("stateChanged ");
                    stringBuilder2.append(stateToString(stateType));
                    stringBuilder2.append(" mPkgs:");
                    stringBuilder2.append(this.mScenePkgArray.get(stateType));
                    AwareLog.d(str2, stringBuilder2.toString());
                }
            }
        }
    }

    public void dump(PrintWriter pw) {
        if (pw != null) {
            if (this.mIsInitialized.get()) {
                pw.println("dump Important State Apps start --------");
                synchronized (this) {
                    int size = this.mScenePidArray.size();
                    for (int i = 1; i < size; i++) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("State[");
                        stringBuilder.append(stateToString(i));
                        stringBuilder.append("] Pids:");
                        stringBuilder.append(this.mScenePidArray.get(i));
                        pw.println(stringBuilder.toString());
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("State[");
                        stringBuilder.append(stateToString(i));
                        stringBuilder.append("] Uids:");
                        stringBuilder.append(this.mSceneUidArray.get(i));
                        pw.println(stringBuilder.toString());
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("State[");
                        stringBuilder.append(stateToString(i));
                        stringBuilder.append("] Pkgs:");
                        stringBuilder.append(this.mScenePkgArray.get(i));
                        pw.println(stringBuilder.toString());
                    }
                }
                synchronized (this.mAudioCacheUids) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("State[AUDIO CACHE] Uids:");
                    stringBuilder2.append(this.mAudioCacheUids);
                    pw.println(stringBuilder2.toString());
                }
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("nat timeout:");
                stringBuilder3.append(this.mNatTimeout);
                pw.println(stringBuilder3.toString());
                pw.println("dump Important State Apps end-----------");
                return;
            }
            pw.println("KeyBackGroup feature not enabled.");
        }
    }

    public void dumpCheckStateByPid(PrintWriter pw, Context context, int state, int pid) {
        if (pw != null) {
            if (this.mPGSdk == null || !this.mIsInitialized.get()) {
                pw.println("KeyBackGroup feature not enabled.");
                return;
            }
            pw.println("----------------------------------------");
            boolean result = false;
            try {
                result = this.mPGSdk.checkStateByPid(context, pid, state);
            } catch (RemoteException e) {
                AwareLog.e(TAG, "dumpCheckStateByPid occur exception.");
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CheckState Pid:");
            stringBuilder.append(pid);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("state:");
            stringBuilder.append(stateToString(state));
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("result:");
            stringBuilder.append(result);
            pw.println(stringBuilder.toString());
            pw.println("----------------------------------------");
        }
    }

    public void dumpCheckStateByPkg(PrintWriter pw, Context context, int state, String pkg) {
        if (pw != null && pkg != null && context != null) {
            if (this.mPGSdk == null || !this.mIsInitialized.get()) {
                pw.println("KeyBackGroup feature not enabled.");
                return;
            }
            pw.println("----------------------------------------");
            boolean result = false;
            try {
                result = this.mPGSdk.checkStateByPkg(context, pkg, state);
            } catch (RemoteException e) {
                AwareLog.e(TAG, "dumpCheckStateByPkg occur exception.");
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CheckState Package:");
            stringBuilder.append(pkg);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("state:");
            stringBuilder.append(stateToString(state));
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("result:");
            stringBuilder.append(result);
            pw.println(stringBuilder.toString());
            pw.println("----------------------------------------");
        }
    }

    public void dumpCheckPkgType(PrintWriter pw, Context context, String pkg) {
        if (pw != null && pkg != null && context != null) {
            if (this.mPGSdk == null || !this.mIsInitialized.get()) {
                pw.println("KeyBackGroup feature not enabled.");
                return;
            }
            int type = 0;
            pw.println("----------------------------------------");
            try {
                type = this.mPGSdk.getPkgType(context, pkg);
            } catch (RemoteException e) {
                AwareLog.e(TAG, "getAppType occur exception.");
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CheckType Package:");
            stringBuilder.append(pkg);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("type:");
            stringBuilder.append(typeToString(type));
            pw.println(stringBuilder.toString());
            pw.println("----------------------------------------");
        }
    }

    public void dumpFakeEvent(PrintWriter pw, int stateType, int eventType, int pid, String pkg, int uid) {
        if (pw != null && pkg != null) {
            if (this.mPGSdk == null || !this.mIsInitialized.get()) {
                pw.println("KeyBackGroup feature not enabled.");
                return;
            }
            pw.println("----------------------------------------");
            getInstance().updateSceneState(stateType, eventType, pid, pkg, uid);
            pw.println("Send fake event success!");
            pw.println("----------------------------------------");
        }
    }

    public void dumpCheckKeyBackGroup(PrintWriter pw, int pid, int uid) {
        if (pw != null) {
            if (this.mIsInitialized.get()) {
                pw.println("dump CheckKeyBackGroup State start --------");
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Check Pid:");
                stringBuilder.append(pid);
                pw.println(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("Check Uid:");
                stringBuilder.append(uid);
                pw.println(stringBuilder.toString());
                boolean result = checkIsKeyBackgroup(pid, uid);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("result:");
                stringBuilder2.append(result);
                pw.println(stringBuilder2.toString());
                pw.println("dump CheckKeyBackGroup State end-----------");
                return;
            }
            pw.println("KeyBackGroup feature not enabled.");
        }
    }

    private int getKeyBackgroupTypeByPidLocked(int pid) {
        if (this.mScenePidArray.isEmpty()) {
            return -1;
        }
        if (((ArraySet) this.mScenePidArray.get(2)).contains(Integer.valueOf(pid))) {
            return 2;
        }
        if (((ArraySet) this.mScenePidArray.get(1)).contains(Integer.valueOf(pid))) {
            return 1;
        }
        if (((ArraySet) this.mScenePidArray.get(3)).contains(Integer.valueOf(pid))) {
            return 3;
        }
        if (((ArraySet) this.mScenePidArray.get(5)).contains(Integer.valueOf(pid))) {
            return 5;
        }
        if (((ArraySet) this.mScenePidArray.get(4)).contains(Integer.valueOf(pid))) {
            return 4;
        }
        return -1;
    }

    private int getKeyBackgroupTypeByUidLocked(int uid) {
        if (this.mSceneUidArray.isEmpty()) {
            return -1;
        }
        if (((ArraySet) this.mSceneUidArray.get(2)).contains(Integer.valueOf(uid))) {
            return 2;
        }
        if (((ArraySet) this.mSceneUidArray.get(1)).contains(Integer.valueOf(uid))) {
            return 1;
        }
        if (((ArraySet) this.mSceneUidArray.get(3)).contains(Integer.valueOf(uid))) {
            return 3;
        }
        if (((ArraySet) this.mSceneUidArray.get(5)).contains(Integer.valueOf(uid))) {
            return 5;
        }
        if (((ArraySet) this.mSceneUidArray.get(4)).contains(Integer.valueOf(uid))) {
            return 4;
        }
        return -1;
    }

    private int getKeyBackgroupTypeByPkgsLocked(List<String> pkgs) {
        if (this.mScenePkgArray.isEmpty()) {
            return -1;
        }
        for (String pkg : pkgs) {
            if (((ArraySet) this.mScenePkgArray.get(2)).contains(pkg)) {
                return 2;
            }
            if (((ArraySet) this.mScenePkgArray.get(1)).contains(pkg)) {
                return 1;
            }
            if (((ArraySet) this.mScenePkgArray.get(3)).contains(pkg)) {
                return 3;
            }
            if (((ArraySet) this.mScenePkgArray.get(5)).contains(pkg)) {
                return 5;
            }
            if (((ArraySet) this.mScenePkgArray.get(4)).contains(pkg)) {
                return 4;
            }
        }
        return -1;
    }

    private void handleSensorEvent(int uid, int sensor, boolean enable) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sensor:");
            stringBuilder.append(sensor);
            stringBuilder.append(" enable:");
            stringBuilder.append(enable);
            stringBuilder.append(" uid:");
            stringBuilder.append(uid);
            AwareLog.i(str, stringBuilder.toString());
        }
        synchronized (this) {
            SensorRecord se = (SensorRecord) this.mHistorySensorRecords.get(uid);
            if (enable) {
                if (se == null) {
                    this.mHistorySensorRecords.put(uid, new SensorRecord(uid, sensor));
                } else {
                    se.addSensor(sensor);
                }
            } else if (se != null && se.hasSensor()) {
                se.removeSensor(Integer.valueOf(sensor));
            }
        }
    }

    /* JADX WARNING: Missing block: B:12:0x0030, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:15:?, code skipped:
            r0 = r7.mPGSdk.getSensorInfoByUid(r8, r9);
     */
    /* JADX WARNING: Missing block: B:16:0x0038, code skipped:
            if (r0 == null) goto L_0x009a;
     */
    /* JADX WARNING: Missing block: B:17:0x003a, code skipped:
            r1 = r0.entrySet().iterator();
     */
    /* JADX WARNING: Missing block: B:19:0x0046, code skipped:
            if (r1.hasNext() == false) goto L_0x006d;
     */
    /* JADX WARNING: Missing block: B:20:0x0048, code skipped:
            r2 = (java.util.Map.Entry) r1.next();
            r3 = java.lang.Integer.parseInt((java.lang.String) r2.getKey());
            r4 = java.lang.Integer.parseInt((java.lang.String) r2.getValue());
            r6 = 1;
     */
    /* JADX WARNING: Missing block: B:21:0x0064, code skipped:
            if (r6 > r4) goto L_0x006c;
     */
    /* JADX WARNING: Missing block: B:22:0x0066, code skipped:
            handleSensorEvent(r9, r3, true);
            r6 = r6 + 1;
     */
    /* JADX WARNING: Missing block: B:25:0x006f, code skipped:
            if (DEBUG == false) goto L_0x009a;
     */
    /* JADX WARNING: Missing block: B:26:0x0071, code skipped:
            r2 = TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("getSensorInfoByUid sensor handles ");
            r3.append(r0);
            android.rms.iaware.AwareLog.d(r2, r3.toString());
     */
    /* JADX WARNING: Missing block: B:28:0x0089, code skipped:
            android.rms.iaware.AwareLog.e(TAG, "integer parse error!");
     */
    /* JADX WARNING: Missing block: B:30:0x0092, code skipped:
            android.rms.iaware.AwareLog.e(TAG, "error, PG crash!");
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void initAppSensorState(Context context, int uid) {
        if (this.mPGSdk == null) {
            AwareLog.e(TAG, "KeyBackGroup feature not enabled.");
            return;
        }
        synchronized (this) {
            if (this.mHistorySensorRecords.get(uid) != null) {
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("History Sensor Records has uid ");
                    stringBuilder.append(uid);
                    AwareLog.d(str, stringBuilder.toString());
                }
            }
        }
    }

    private void updateAppSensorState(boolean sensorStart, int uid, int pid) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateAppSensorState :");
            stringBuilder.append(uid);
            stringBuilder.append(" pid ");
            stringBuilder.append(pid);
            AwareLog.i(str, stringBuilder.toString());
        }
        updateSceneState(4, sensorStart ? 1 : 2, pid, null, uid);
    }

    private void sendDecayMesssage(int message, DecayInfo decayInfo, long delayT) {
        if (this.mAppKeyHandler.hasMessages(message, decayInfo)) {
            this.mAppKeyHandler.removeMessages(message, decayInfo);
        }
        Message observerMsg = this.mAppKeyHandler.obtainMessage();
        observerMsg.what = message;
        observerMsg.obj = decayInfo;
        this.mAppKeyHandler.sendMessageDelayed(observerMsg, delayT);
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendDecayMesssage end ");
            stringBuilder.append(message);
            stringBuilder.append(" decayinfo ");
            stringBuilder.append(decayInfo);
            AwareLog.d(str, stringBuilder.toString());
        }
    }

    /* JADX WARNING: Missing block: B:19:0x004d, code skipped:
            r0 = true;
     */
    /* JADX WARNING: Missing block: B:20:0x004e, code skipped:
            if (r3 != false) goto L_0x005a;
     */
    /* JADX WARNING: Missing block: B:21:0x0050, code skipped:
            if (r4 == false) goto L_0x0053;
     */
    /* JADX WARNING: Missing block: B:22:0x0057, code skipped:
            r7 = r5;
            r9 = r6;
     */
    /* JADX WARNING: Missing block: B:23:0x005a, code skipped:
            r8 = r1.mStateEventDecayInfos;
     */
    /* JADX WARNING: Missing block: B:24:0x005c, code skipped:
            monitor-enter(r8);
     */
    /* JADX WARNING: Missing block: B:26:?, code skipped:
            r9 = r1.mStateEventDecayInfos.iterator();
     */
    /* JADX WARNING: Missing block: B:28:0x0067, code skipped:
            if (r9.hasNext() == false) goto L_0x009d;
     */
    /* JADX WARNING: Missing block: B:29:0x0069, code skipped:
            r10 = (com.android.server.rms.iaware.appmng.AwareAppKeyBackgroup.DecayInfo) r9.next();
     */
    /* JADX WARNING: Missing block: B:30:0x0073, code skipped:
            if (r10.getStateType() != 2) goto L_0x0098;
     */
    /* JADX WARNING: Missing block: B:32:0x0079, code skipped:
            if (r10.getEventType() == 2) goto L_0x007c;
     */
    /* JADX WARNING: Missing block: B:34:0x007c, code skipped:
            r11 = r10.getUid();
            r12 = r10.getPid();
     */
    /* JADX WARNING: Missing block: B:36:0x0086, code skipped:
            if (r11 != r17) goto L_0x008d;
     */
    /* JADX WARNING: Missing block: B:37:0x0088, code skipped:
            r5 = true;
     */
    /* JADX WARNING: Missing block: B:38:0x008a, code skipped:
            if (r12 != -1) goto L_0x008d;
     */
    /* JADX WARNING: Missing block: B:39:0x008c, code skipped:
            r6 = true;
     */
    /* JADX WARNING: Missing block: B:41:0x008f, code skipped:
            if (r12 != r16) goto L_0x0092;
     */
    /* JADX WARNING: Missing block: B:42:0x0091, code skipped:
            r6 = true;
     */
    /* JADX WARNING: Missing block: B:43:0x0092, code skipped:
            if (r5 == false) goto L_0x0063;
     */
    /* JADX WARNING: Missing block: B:44:0x0094, code skipped:
            if (r6 == false) goto L_0x0063;
     */
    /* JADX WARNING: Missing block: B:45:0x0098, code skipped:
            r14 = r16;
            r13 = r17;
     */
    /* JADX WARNING: Missing block: B:46:0x009d, code skipped:
            r14 = r16;
            r13 = r17;
     */
    /* JADX WARNING: Missing block: B:48:?, code skipped:
            monitor-exit(r8);
     */
    /* JADX WARNING: Missing block: B:49:0x00a2, code skipped:
            if (r3 == false) goto L_0x00a6;
     */
    /* JADX WARNING: Missing block: B:50:0x00a4, code skipped:
            if (r5 == false) goto L_0x00aa;
     */
    /* JADX WARNING: Missing block: B:51:0x00a6, code skipped:
            if (r4 == false) goto L_0x0057;
     */
    /* JADX WARNING: Missing block: B:52:0x00a8, code skipped:
            if (r6 != false) goto L_0x0057;
     */
    /* JADX WARNING: Missing block: B:53:0x00aa, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:54:0x00ab, code skipped:
            r10 = isInCalling();
            r11 = r1.mAudioCacheUids;
     */
    /* JADX WARNING: Missing block: B:55:0x00b1, code skipped:
            monitor-enter(r11);
     */
    /* JADX WARNING: Missing block: B:56:0x00b2, code skipped:
            if (r10 == false) goto L_0x00c4;
     */
    /* JADX WARNING: Missing block: B:57:0x00b4, code skipped:
            if (r7 != false) goto L_0x00c2;
     */
    /* JADX WARNING: Missing block: B:60:0x00c0, code skipped:
            if (r1.mAudioCacheUids.contains(java.lang.Integer.valueOf(r17)) == false) goto L_0x00c4;
     */
    /* JADX WARNING: Missing block: B:61:0x00c2, code skipped:
            monitor-exit(r11);
     */
    /* JADX WARNING: Missing block: B:62:0x00c3, code skipped:
            return r0;
     */
    /* JADX WARNING: Missing block: B:63:0x00c4, code skipped:
            monitor-exit(r11);
     */
    /* JADX WARNING: Missing block: B:64:0x00c5, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:68:0x00c9, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:69:0x00ca, code skipped:
            r14 = r16;
            r13 = r17;
     */
    /* JADX WARNING: Missing block: B:71:?, code skipped:
            monitor-exit(r8);
     */
    /* JADX WARNING: Missing block: B:72:0x00cf, code skipped:
            throw r0;
     */
    /* JADX WARNING: Missing block: B:73:0x00d0, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:78:0x00d9, code skipped:
            r0 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean checkAudioOutInstant(int pid, int uid, List<String> pkgs) {
        int i;
        int i2;
        if (!this.mIsInitialized.get() || pkgs == null || pkgs.isEmpty()) {
            i = pid;
            i2 = uid;
            return false;
        }
        boolean sceneContainsUid = false;
        boolean sceneContainsPid = false;
        boolean decayContainsUid = false;
        boolean decayContainsPid = false;
        synchronized (this) {
            try {
                if (this.mSceneUidArray.isEmpty()) {
                    return false;
                }
                if (((ArraySet) this.mSceneUidArray.get(2)).contains(Integer.valueOf(uid))) {
                    sceneContainsUid = true;
                }
                if (((ArraySet) this.mScenePidArray.get(2)).contains(Integer.valueOf(pid))) {
                    sceneContainsPid = true;
                }
            } finally {
                i = pid;
                i2 = uid;
                while (true) {
                }
            }
        }
    }

    private void setNatTimeout(int natTime) {
        this.mNatTimeout = natTime;
        AwareWakeUpManager.getInstance().setIntervalOverload(natTime);
    }
}
