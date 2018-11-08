package com.android.server.rms.iaware.appmng;

import android.app.mtm.iaware.appmng.AppMngConstant.AppMngFeature;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.rms.iaware.AwareLog;
import android.util.ArraySet;
import com.android.internal.os.SomeArgs;
import com.android.server.AlarmManagerService.Alarm;
import com.android.server.mtm.iaware.appmng.AwareAppMngSort;
import com.android.server.mtm.iaware.appmng.DecisionMaker;
import com.android.server.mtm.iaware.appmng.appstart.datamgr.SystemUnremoveUidCache;
import com.android.server.rms.iaware.feature.AlarmManagerFeature;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

public class AwareWakeUpManager {
    private static final /* synthetic */ int[] -com-android-server-rms-iaware-appmng-AwareWakeUpManager$ControlTypeSwitchesValues = null;
    private static final int ALARMTAGLENGTH = 2;
    private static final long BUFFER_TIME = 10000;
    private static final int CONTROL_PARAM_LENGTH = 7;
    private static final int DEFAULT_TOPN = -1;
    private static final int INDEX_PACKAGE_NAME = 0;
    private static final int MIN_WHITE_LIST_LENGTH = 1;
    private static final int MSG_ALARM_WAKEUP = 1;
    private static final int MSG_SYS_WAKEUP = 2;
    private static final int NAT_DETECT_START = -1;
    private static final String SEPARATOR = "\\|";
    private static final String TAG = "AwareWakeUpManager";
    private static final String TAG_CONTROL_PARAM = "control_param";
    private static final String TAG_WHITE_LIST = "white_list";
    private static volatile AwareWakeUpManager mAwareWakeUpManager;
    private HashMap<Integer, HashMap<String, PackageControlPolicy>> mAlarmControlPolicy = new HashMap();
    private HashMap<Integer, HashMap<String, PackageWakeupInfo>> mAlarmWakeupInfo = new HashMap();
    private Long mDebugDelay = Long.valueOf(0);
    private List<String> mDebugLog = new ArrayList();
    private String mDebugPkg = "";
    private String mDebugTag = "";
    private int mDebugUserId = -1;
    private boolean mHWPushNatDetecting = false;
    private Handler mHandler;
    private long mIntervalOverload = this.mIntervalOverloadDefault;
    private long mIntervalOverloadDefault = AwareAppMngSort.PREVIOUS_APP_DIRCACTIVITY_DECAYTIME;
    private long mIntervalWakeup = 1000;
    private long mIntervalWindowLength = AwareAppMngSort.PREVIOUS_APP_DIRCACTIVITY_DECAYTIME;
    private boolean mIsDebugMode = false;
    private boolean mIsInit = false;
    private AtomicBoolean mIsScreenOn = new AtomicBoolean(true);
    private ArrayList<ArrayList<TagWakeupInfo>> mLastRecentWakeupAlarm = new ArrayList();
    private long mLastSysWakeup = 0;
    private long mNatTime = 300000;
    private ArraySet<String> mPushTags = new ArraySet();
    private SystemUnremoveUidCache mSystemUnremoveUidCache;
    private ArraySet<TagWakeupInfo> mSystemWakeupQueue = new ArraySet();
    private List<Long> mSystemWakeupTimeQueue = new ArrayList();
    private int mThresholdPkgOverload = 4;
    private int mThresholdSysOverload = 6;
    private int mThresholdTagOverload = 3;
    private HashMap<String, ArraySet<String>> mWhiteList = new HashMap();

    public enum ControlType {
        DO_NOTHING,
        IMPORTANT,
        UNKNOWN,
        PERCEPTIBLE,
        EXTEND {
            protected void apply(Alarm alarm) {
                AwareWakeUpManager.getInstance().extend(alarm);
            }
        },
        EXTEND_TOPN {
            protected void apply(Alarm alarm) {
                AwareWakeUpManager.getInstance().extend(alarm);
            }
        },
        EXTEND_AND_MUTE {
            protected void apply(Alarm alarm) {
                AwareWakeUpManager.getInstance().extend(alarm);
                AwareWakeUpManager.getInstance().mute(alarm);
            }
        },
        DECIDE_OVERLOAD {
            protected void apply(Alarm alarm) {
                AwareWakeUpManager.getInstance().extend(alarm);
                AwareWakeUpManager.getInstance().mute(alarm);
            }
        };

        protected void apply(Alarm alarm) {
        }
    }

    protected class PackageControlPolicy {
        protected String mPkg;
        protected HashMap<String, TagControlPolicy> mTagPolicyMap = new HashMap();
        protected int mUid;

        protected PackageControlPolicy(int uid, String pkg) {
            this.mUid = uid;
            this.mPkg = pkg;
        }

        protected void overload(String tag) {
            if (tag == null) {
                HashMap<String, PackageWakeupInfo> packageWakeupInfos = (HashMap) AwareWakeUpManager.this.mAlarmWakeupInfo.get(Integer.valueOf(UserHandle.getUserId(this.mUid)));
                if (packageWakeupInfos != null) {
                    PackageWakeupInfo packageWakeupInfo = (PackageWakeupInfo) packageWakeupInfos.get(this.mPkg);
                    if (packageWakeupInfo != null) {
                        for (Entry<String, TagWakeupInfo> entry : packageWakeupInfo.mWakeUpMap.entrySet()) {
                            tagOverload(((TagWakeupInfo) entry.getValue()).mTag);
                        }
                        return;
                    }
                    return;
                }
                return;
            }
            tagOverload(tag);
        }

        private void tagOverload(String tag) {
            TagControlPolicy tagPolicy = (TagControlPolicy) this.mTagPolicyMap.get(tag);
            if (tagPolicy == null) {
                tagPolicy = new TagControlPolicy(this.mUid, this.mPkg, tag);
            }
            tagPolicy.overload();
            this.mTagPolicyMap.put(tag, tagPolicy);
        }

        protected void apply(Alarm alarm) {
            TagControlPolicy tagPolicy = (TagControlPolicy) this.mTagPolicyMap.get(alarm.statsTag);
            if (tagPolicy != null) {
                tagPolicy.apply(alarm);
            }
        }
    }

    protected class PackageWakeupInfo {
        protected String mPkg;
        protected int mUid;
        protected HashMap<String, TagWakeupInfo> mWakeUpMap = new HashMap();
        protected List<Long> mWakeUpQueue = new ArrayList();

        protected PackageWakeupInfo(int uid, String pkg) {
            this.mUid = uid;
            this.mPkg = pkg;
        }

        protected void wakeUp(String tag, long currentTime) {
            TagWakeupInfo tagInfo = (TagWakeupInfo) this.mWakeUpMap.get(tag);
            if (tagInfo == null) {
                tagInfo = new TagWakeupInfo(this.mUid, this.mPkg, tag, currentTime);
            }
            this.mWakeUpQueue.add(Long.valueOf(currentTime));
            if (this.mWakeUpQueue.size() >= AwareWakeUpManager.this.mThresholdPkgOverload && currentTime - ((Long) this.mWakeUpQueue.remove(0)).longValue() < AwareWakeUpManager.this.mIntervalOverload) {
                AlarmManagerDumpRadar.getInstance().reportAlarmEvent(1, this.mUid, this.mPkg, null, null);
                if (!AwareWakeUpManager.this.isSystemUnRemoveApp(this.mUid)) {
                    AwareWakeUpManager.this.reportAlarmOverload(this.mUid, this.mPkg, null);
                }
                if (AwareWakeUpManager.this.mIsDebugMode) {
                    AwareWakeUpManager.this.debugLog("EVENT_PKG_OVERLOAD uid = " + this.mUid + ", pkg = " + this.mPkg);
                }
                this.mWakeUpQueue.clear();
            }
            tagInfo.wakeUp(currentTime);
            this.mWakeUpMap.put(tag, tagInfo);
        }

        protected TagWakeupInfo get(String tag) {
            return (TagWakeupInfo) this.mWakeUpMap.get(tag);
        }
    }

    protected class TagControlPolicy {
        protected String mPkg;
        protected ControlType mPolicy = ControlType.DO_NOTHING;
        protected String mTag;
        protected int mUid;

        protected TagControlPolicy(int uid, String pkg, String tag) {
            this.mUid = uid;
            this.mPkg = pkg;
            this.mTag = tag;
        }

        protected void overload() {
            if (AwareWakeUpManager.this.isInWhiteList(this.mPkg, this.mTag)) {
                this.mPolicy = ControlType.IMPORTANT;
            }
            this.mPolicy = AwareWakeUpManager.this.increaseControlLevel(this.mUid, this.mPkg, this.mTag, this.mPolicy);
        }

        protected void apply(Alarm alarm) {
            this.mPolicy.apply(alarm);
        }
    }

    protected class TagWakeupInfo {
        protected long mLastWakeUp = 0;
        protected String mPkg;
        protected String mTag;
        protected int mUid;
        protected List<Long> mWakeUpQueue = new ArrayList();

        protected TagWakeupInfo(int uid, String pkg, String tag, long currentTime) {
            this.mUid = uid;
            this.mPkg = pkg;
            this.mTag = tag;
            this.mLastWakeUp = currentTime;
        }

        protected void wakeUp(long currentTime) {
            this.mWakeUpQueue.add(Long.valueOf(currentTime));
            if (this.mWakeUpQueue.size() >= AwareWakeUpManager.this.mThresholdTagOverload && currentTime - ((Long) this.mWakeUpQueue.remove(0)).longValue() < AwareWakeUpManager.this.mIntervalOverload) {
                AwareWakeUpManager.this.reportAlarmOverload(this.mUid, this.mPkg, this.mTag);
                AlarmManagerDumpRadar.getInstance().reportAlarmEvent(1, this.mUid, this.mPkg, this.mTag, null);
                if (AwareWakeUpManager.this.mIsDebugMode) {
                    AwareWakeUpManager.this.debugLog("EVENT_TAG_OVERLOAD uid = " + this.mUid + ", pkg = " + this.mPkg + ", tag = " + this.mTag);
                }
                this.mWakeUpQueue.clear();
            }
            this.mLastWakeUp = currentTime;
        }
    }

    private final class WakeUpHandler extends Handler {
        public WakeUpHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    SomeArgs args = msg.obj;
                    AwareWakeUpManager.this.handleWakeupAlarm(args.arg1, ((Long) args.arg2).longValue());
                    return;
                case 2:
                    AwareWakeUpManager.this.handleWakeupSystem(((Long) msg.obj).longValue());
                    return;
                default:
                    return;
            }
        }
    }

    private static /* synthetic */ int[] -getcom-android-server-rms-iaware-appmng-AwareWakeUpManager$ControlTypeSwitchesValues() {
        if (-com-android-server-rms-iaware-appmng-AwareWakeUpManager$ControlTypeSwitchesValues != null) {
            return -com-android-server-rms-iaware-appmng-AwareWakeUpManager$ControlTypeSwitchesValues;
        }
        int[] iArr = new int[ControlType.values().length];
        try {
            iArr[ControlType.DECIDE_OVERLOAD.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[ControlType.DO_NOTHING.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[ControlType.EXTEND.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[ControlType.EXTEND_AND_MUTE.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[ControlType.EXTEND_TOPN.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[ControlType.IMPORTANT.ordinal()] = 8;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[ControlType.PERCEPTIBLE.ordinal()] = 6;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[ControlType.UNKNOWN.ordinal()] = 7;
        } catch (NoSuchFieldError e8) {
        }
        -com-android-server-rms-iaware-appmng-AwareWakeUpManager$ControlTypeSwitchesValues = iArr;
        return iArr;
    }

    private AwareWakeUpManager() {
        this.mPushTags.add("com.huawei.intent.action.PUSH");
        this.mPushTags.add("com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void extend(Alarm alarm) {
        synchronized (this.mAlarmWakeupInfo) {
            HashMap<String, PackageWakeupInfo> packageWakeupInfos = (HashMap) this.mAlarmWakeupInfo.get(Integer.valueOf(UserHandle.getUserId(alarm.uid)));
            if (packageWakeupInfos == null) {
                return;
            }
            PackageWakeupInfo packageWakeupInfo = (PackageWakeupInfo) packageWakeupInfos.get(alarm.packageName);
            if (packageWakeupInfo == null) {
                return;
            }
            TagWakeupInfo tagWakeupInfo = (TagWakeupInfo) packageWakeupInfo.mWakeUpMap.get(alarm.statsTag);
            if (tagWakeupInfo == null) {
                return;
            }
            long lastWakeUpTime = tagWakeupInfo.mLastWakeUp;
        }
    }

    protected void mute(Alarm alarm) {
        alarm.wakeup = false;
        if (this.mIsDebugMode) {
            debugLog("EVENT_MUTE tag = " + alarm.statsTag);
        }
    }

    private ControlType increaseControlLevel(int uid, String pkg, String tag, ControlType policy) {
        int alarmType;
        switch (-getcom-android-server-rms-iaware-appmng-AwareWakeUpManager$ControlTypeSwitchesValues()[policy.ordinal()]) {
            case 1:
            case 4:
                policy = ControlType.DECIDE_OVERLOAD;
                break;
            case 2:
            case 6:
            case 7:
                alarmType = getAlarmType(uid, pkg, tag);
                if (alarmType != 4) {
                    if (alarmType != 3) {
                        policy = ControlType.UNKNOWN;
                        break;
                    }
                    policy = ControlType.PERCEPTIBLE;
                    break;
                }
                policy = ControlType.EXTEND;
                break;
            case 3:
            case 5:
                alarmType = getAlarmType(uid, pkg, tag);
                if (alarmType != 4) {
                    if (alarmType != 3) {
                        policy = ControlType.UNKNOWN;
                        break;
                    }
                    policy = ControlType.PERCEPTIBLE;
                    break;
                } else if (!isTopImEmail(pkg)) {
                    policy = ControlType.EXTEND_AND_MUTE;
                    break;
                } else {
                    policy = ControlType.EXTEND_TOPN;
                    break;
                }
        }
        AlarmManagerDumpRadar.getInstance().reportAlarmEvent(2, uid, pkg, tag, policy);
        if (this.mIsDebugMode) {
            debugLog("EVENT_CONTROLED uid = " + uid + ", pkg = " + pkg + ", tag = " + tag + ", policy = " + policy);
        }
        if (policy.ordinal() > ControlType.PERCEPTIBLE.ordinal()) {
            AwareLog.i(TAG, "alarm overload uid = " + uid + ", pkg = " + pkg + ", tag = " + tag + ", policy = " + policy);
        } else {
            AwareLog.i(TAG, "alarm overload but not control uid = " + uid + ", pkg = " + pkg + ", tag = " + tag + ", policy = " + policy);
        }
        return policy;
    }

    public static AwareWakeUpManager getInstance() {
        if (mAwareWakeUpManager == null) {
            synchronized (AwareWakeUpManager.class) {
                if (mAwareWakeUpManager == null) {
                    mAwareWakeUpManager = new AwareWakeUpManager();
                }
            }
        }
        return mAwareWakeUpManager;
    }

    public void init(Handler handler, Context context) {
        if (handler != null && context != null) {
            this.mHandler = new WakeUpHandler(handler.getLooper());
            this.mSystemUnremoveUidCache = SystemUnremoveUidCache.getInstance(context);
            AlarmManagerDumpRadar.getInstance().setHandler(handler);
            DecisionMaker.getInstance().updateRule(AppMngFeature.APP_ALARM, context);
            updateWhiteList();
            updateControlParam();
            this.mIsInit = true;
        }
    }

    private boolean isTopImEmail(String pkg) {
        if (AwareIntelligentRecg.getInstance().isAppMngSpecTypeFreqTopN(pkg, 1, -1)) {
            return true;
        }
        return AwareIntelligentRecg.getInstance().isAppMngSpecTypeFreqTopN(pkg, 0, -1);
    }

    private int getAlarmType(int uid, String pkg, String tag) {
        String[] strs = tag.split(":");
        if (strs.length != 2) {
            return -1;
        }
        return AwareIntelligentRecg.getInstance().getAlarmActionType(uid, pkg, strs[1]);
    }

    public void reportWakeupSystem(String reason) {
        if (AlarmManagerFeature.isEnable() && (this.mIsInit ^ 1) == 0 && !this.mIsScreenOn.get() && reason != null && (reason.contains("RTC") ^ 1) == 0) {
            long currentTime = SystemClock.elapsedRealtime();
            Message msg = this.mHandler.obtainMessage();
            msg.what = 2;
            msg.obj = Long.valueOf(currentTime);
            this.mHandler.sendMessage(msg);
        }
    }

    private void handleWakeupSystem(long wakeupTime) {
        synchronized (this.mAlarmWakeupInfo) {
            Iterator lruIter = this.mLastRecentWakeupAlarm.iterator();
            while (lruIter.hasNext()) {
                ArrayList<TagWakeupInfo> alarmList = (ArrayList) lruIter.next();
                if (wakeupTime - ((TagWakeupInfo) alarmList.get(0)).mLastWakeUp < this.mIntervalWakeup) {
                    recordWakeupAlarmLocked(alarmList);
                    recordWakeupSystemLocked(wakeupTime);
                    wakeupTime = 0;
                    break;
                }
            }
            this.mLastRecentWakeupAlarm = new ArrayList();
            this.mLastSysWakeup = wakeupTime;
        }
    }

    private void recordWakeupSystemLocked(long wakeupTime) {
        AlarmManagerDumpRadar.getInstance().reportSystemEvent(0);
        this.mSystemWakeupTimeQueue.add(Long.valueOf(wakeupTime));
        if (this.mSystemWakeupTimeQueue.size() >= this.mThresholdSysOverload && wakeupTime - ((Long) this.mSystemWakeupTimeQueue.remove(0)).longValue() < this.mIntervalOverload) {
            AlarmManagerDumpRadar.getInstance().reportSystemEvent(1);
            if (this.mIsDebugMode) {
                debugLog("EVENT_OVERLOAD_SYSTEM at " + wakeupTime);
            }
            for (TagWakeupInfo tagInfo : this.mSystemWakeupQueue) {
                if (!isSystemUnRemoveApp(tagInfo.mUid)) {
                    reportAlarmOverload(tagInfo.mUid, tagInfo.mPkg, tagInfo.mTag);
                }
            }
            this.mSystemWakeupQueue.clear();
            this.mSystemWakeupTimeQueue.clear();
        }
        if (this.mIsDebugMode) {
            debugLog("EVENT_WAKEUP_SYSTEM at " + wakeupTime);
        }
    }

    public void reportWakeupAlarms(ArrayList<Alarm> alarms) {
        if (AlarmManagerFeature.isEnable() && !this.mIsScreenOn.get() && (this.mIsInit ^ 1) == 0 && alarms != null && !alarms.isEmpty()) {
            Message msg = this.mHandler.obtainMessage();
            msg.what = 1;
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = alarms;
            args.arg2 = Long.valueOf(SystemClock.elapsedRealtime());
            msg.obj = args;
            this.mHandler.sendMessage(msg);
        }
    }

    private void handleWakeupAlarm(ArrayList<Alarm> alarms, long wakeupTime) {
        synchronized (this.mAlarmWakeupInfo) {
            ArrayList<TagWakeupInfo> alarmList = new ArrayList();
            int size = alarms.size();
            for (int i = 0; i < size; i++) {
                alarmList.add(new TagWakeupInfo(((Alarm) alarms.get(i)).uid, ((Alarm) alarms.get(i)).packageName, ((Alarm) alarms.get(i)).statsTag, wakeupTime));
            }
            if (wakeupTime - this.mLastSysWakeup < this.mIntervalWakeup) {
                recordWakeupAlarmLocked(alarmList);
                recordWakeupSystemLocked(this.mLastSysWakeup);
                this.mLastRecentWakeupAlarm = new ArrayList();
                this.mLastSysWakeup = 0;
                return;
            }
            Iterator lruIter = this.mLastRecentWakeupAlarm.iterator();
            while (lruIter.hasNext()) {
                alarmList = (ArrayList) lruIter.next();
                if (wakeupTime - ((TagWakeupInfo) alarmList.get(0)).mLastWakeUp > this.mIntervalWakeup) {
                    lruIter.remove();
                }
            }
            this.mLastRecentWakeupAlarm.add(alarmList);
        }
    }

    private void recordWakeupAlarmLocked(ArrayList<TagWakeupInfo> alarms) {
        long wakeupTime = ((TagWakeupInfo) alarms.get(0)).mLastWakeUp;
        int size = alarms.size();
        for (int i = 0; i < size; i++) {
            TagWakeupInfo alarm = (TagWakeupInfo) alarms.get(i);
            int uid = alarm.mUid;
            int userId = UserHandle.getUserId(uid);
            String pkg = alarm.mPkg;
            String tag = alarm.mTag;
            if (this.mIsDebugMode) {
                debugLog("EVENT_WAKEUP uid = " + uid + ", pkg = " + pkg + ", tag = " + tag + " at " + wakeupTime);
            }
            HashMap<String, PackageWakeupInfo> packageInfos = (HashMap) this.mAlarmWakeupInfo.get(Integer.valueOf(userId));
            if (packageInfos == null) {
                packageInfos = new HashMap();
            }
            PackageWakeupInfo packageInfo = (PackageWakeupInfo) packageInfos.get(pkg);
            if (packageInfo == null) {
                packageInfo = new PackageWakeupInfo(uid, pkg);
            }
            packageInfo.wakeUp(tag, wakeupTime);
            packageInfos.put(pkg, packageInfo);
            this.mAlarmWakeupInfo.put(Integer.valueOf(userId), packageInfos);
            this.mSystemWakeupQueue.add((TagWakeupInfo) packageInfo.mWakeUpMap.get(tag));
            AlarmManagerDumpRadar.getInstance().reportAlarmEvent(0, uid, pkg, tag, null);
        }
        Iterator wakeupIter = this.mSystemWakeupQueue.iterator();
        while (wakeupIter.hasNext()) {
            if (wakeupTime - ((TagWakeupInfo) wakeupIter.next()).mLastWakeUp > this.mIntervalOverload) {
                wakeupIter.remove();
            }
        }
    }

    private void reportAlarmOverload(int uid, String pkg, String tag) {
        int userId = UserHandle.getUserId(uid);
        synchronized (this.mAlarmWakeupInfo) {
            HashMap<String, PackageControlPolicy> packageControlPolices = (HashMap) this.mAlarmControlPolicy.get(Integer.valueOf(userId));
            if (packageControlPolices == null) {
                packageControlPolices = new HashMap();
            }
            PackageControlPolicy packagePolicy = (PackageControlPolicy) packageControlPolices.get(pkg);
            if (packagePolicy == null) {
                packagePolicy = new PackageControlPolicy(uid, pkg);
            }
            packagePolicy.overload(tag);
            packageControlPolices.put(pkg, packagePolicy);
            this.mAlarmControlPolicy.put(Integer.valueOf(userId), packageControlPolices);
        }
    }

    public void modifyAlarmIfOverload(Alarm alarm) {
        boolean z = true;
        if (AlarmManagerFeature.isEnable() && alarm != null) {
            int userId = UserHandle.getUserId(alarm.uid);
            if (this.mIsDebugMode && userId == this.mDebugUserId && this.mDebugPkg.equals(alarm.packageName) && (this.mDebugTag.equals(alarm.statsTag) || "all".equals(this.mDebugTag))) {
                alarm.whenElapsed += this.mDebugDelay.longValue();
                alarm.maxWhenElapsed += this.mDebugDelay.longValue();
            } else if (this.mIsScreenOn.get()) {
                if (!(alarm.type == 2 || alarm.type == 0)) {
                    z = false;
                }
                alarm.wakeup = z;
            } else {
                ArraySet<String> pushTags = this.mPushTags;
                if (!this.mHWPushNatDetecting || !pushTags.contains(alarm.statsTag)) {
                    synchronized (this.mAlarmWakeupInfo) {
                        HashMap<String, PackageControlPolicy> packageControlPolices = (HashMap) this.mAlarmControlPolicy.get(Integer.valueOf(userId));
                        if (packageControlPolices == null) {
                            return;
                        }
                        PackageControlPolicy packagePolicy = (PackageControlPolicy) packageControlPolices.get(alarm.packageName);
                        if (packagePolicy == null) {
                            return;
                        }
                        packagePolicy.apply(alarm);
                    }
                }
            }
        }
    }

    public void screenOn() {
        this.mIsScreenOn.set(true);
        if (this.mIsDebugMode) {
            debugLog("EVENT_SCREEN_ON");
        }
        synchronized (this.mAlarmWakeupInfo) {
            this.mAlarmWakeupInfo.clear();
            this.mSystemWakeupQueue.clear();
            this.mSystemWakeupTimeQueue.clear();
            this.mLastRecentWakeupAlarm.clear();
            this.mAlarmControlPolicy.clear();
        }
    }

    public void screenOff() {
        this.mIsScreenOn.set(false);
        if (this.mIsDebugMode) {
            debugLog("EVENT_SCREEN_OFF");
        }
    }

    public void setDebugSwitch(boolean isDebugMode) {
        this.mIsDebugMode = isDebugMode;
        if (!this.mIsDebugMode) {
            synchronized (this.mDebugLog) {
                this.mDebugLog.clear();
            }
        }
    }

    public void setDebugParam(int userId, String pkg, String tag, long delay) {
        this.mIsDebugMode = true;
        this.mDebugUserId = userId;
        this.mDebugPkg = pkg;
        this.mDebugTag = tag;
        this.mDebugDelay = Long.valueOf(delay);
    }

    public void dumpDebugLog(PrintWriter pw) {
        if (this.mIsDebugMode) {
            synchronized (this.mDebugLog) {
                int size = this.mDebugLog.size();
                for (int i = 0; i < size; i++) {
                    pw.println((String) this.mDebugLog.get(i));
                }
            }
            return;
        }
        pw.println("debug mod off");
    }

    private String getCurTime() {
        return new SimpleDateFormat("yyyyMMdd-HH-mm-ss-SSS").format(new Date());
    }

    private boolean isSystemUnRemoveApp(int uid) {
        uid = UserHandle.getAppId(uid);
        return (uid > 0 && uid < 10000) || this.mSystemUnremoveUidCache.checkUidExist(uid);
    }

    public boolean isDebugMode() {
        return this.mIsDebugMode;
    }

    public boolean isScreenOn() {
        return this.mIsScreenOn.get();
    }

    public void updateWhiteList() {
        ArrayList<String> configList = DecisionMaker.getInstance().getRawConfig(AppMngFeature.APP_ALARM.getDesc(), TAG_WHITE_LIST);
        if (configList != null) {
            HashMap<String, ArraySet<String>> whiteList = new HashMap();
            int i = 0;
            int size = configList.size();
            while (i < size) {
                String rawConfig = (String) configList.get(i);
                if (rawConfig != null) {
                    String[] config = rawConfig.split(SEPARATOR);
                    if (config.length < 1) {
                        AwareLog.e(TAG, "format error in alarm manager config");
                        return;
                    }
                    ArraySet<String> tagList = new ArraySet();
                    for (int j = 1; j < config.length; j++) {
                        tagList.add(config[j]);
                    }
                    whiteList.put(config[0], tagList);
                    i++;
                } else {
                    return;
                }
            }
            this.mWhiteList = whiteList;
        }
    }

    private boolean isInWhiteList(String pkg, String tag) {
        ArraySet<String> tagList = (ArraySet) this.mWhiteList.get(pkg);
        if (tagList == null) {
            return false;
        }
        return tagList.isEmpty() || tagList.contains(tag);
    }

    public void updateControlParam() {
        ArrayList<String> controlParam = DecisionMaker.getInstance().getRawConfig(AppMngFeature.APP_ALARM.getDesc(), TAG_CONTROL_PARAM);
        if (controlParam != null && controlParam.size() == 7) {
            String rawPushTags = "";
            try {
                int thresholdSysOverload = Integer.parseInt((String) controlParam.get(0));
                int thresholdPkgOverload = Integer.parseInt((String) controlParam.get(1));
                int thresholdTagOverload = Integer.parseInt((String) controlParam.get(2));
                long intervalOverload = Long.parseLong((String) controlParam.get(3));
                long intervalWakeup = Long.parseLong((String) controlParam.get(4));
                long intervalWindowLength = Long.parseLong((String) controlParam.get(5));
                rawPushTags = (String) controlParam.get(6);
                if (rawPushTags == null || "".equals(rawPushTags)) {
                    AwareLog.e(TAG, "invalid push tags");
                    return;
                }
                String[] pushTagString = rawPushTags.split(SEPARATOR);
                ArraySet<String> pushTags = new ArraySet();
                int i = 0;
                while (i < pushTagString.length) {
                    if (!(pushTagString[i] == null || pushTagString[i].equals(""))) {
                        pushTags.add(pushTagString[i]);
                    }
                    i++;
                }
                this.mThresholdSysOverload = thresholdSysOverload;
                this.mThresholdPkgOverload = thresholdPkgOverload;
                this.mThresholdTagOverload = thresholdTagOverload;
                this.mIntervalOverloadDefault = intervalOverload;
                this.mIntervalOverload = computeIntervalOverload(this.mNatTime, intervalOverload);
                this.mIntervalWakeup = intervalWakeup;
                this.mIntervalWindowLength = intervalWindowLength;
                this.mPushTags = pushTags;
            } catch (NumberFormatException e) {
                AwareLog.e(TAG, "control param in wrong format");
            }
        }
    }

    public void setIntervalOverload(int time) {
        AwareLog.i(TAG, "nat time changed : " + time);
        if (time == -1) {
            this.mHWPushNatDetecting = true;
        } else {
            this.mHWPushNatDetecting = false;
        }
        if (time > 0) {
            this.mNatTime = (long) time;
            this.mIntervalOverload = computeIntervalOverload((long) time, this.mIntervalOverloadDefault);
        }
    }

    private long computeIntervalOverload(long natTime, long interval) {
        long maxInterval = natTime * 2;
        if (interval <= maxInterval) {
            maxInterval = interval;
        }
        return maxInterval - 10000;
    }

    public void dumpParam(PrintWriter pw) {
        pw.println("thresholdSysOverload : " + this.mThresholdSysOverload);
        pw.println("thresholdPkgOverload : " + this.mThresholdPkgOverload);
        pw.println("thresholdTagOverload : " + this.mThresholdTagOverload);
        pw.println("intervalOverloadDefault : " + this.mIntervalOverloadDefault);
        pw.println("intervalOverload : " + this.mIntervalOverload);
        pw.println("intervalWakeup : " + this.mIntervalWakeup);
        pw.println("intervalWindowLength : " + this.mIntervalWindowLength);
        pw.println("natTime : " + this.mNatTime);
        pw.println("pushNatDetecting : " + this.mHWPushNatDetecting);
        pw.println("pushTags : " + this.mPushTags);
    }

    private void debugLog(String log) {
        synchronized (this.mDebugLog) {
            this.mDebugLog.add(getCurTime() + ": " + log);
        }
    }
}
