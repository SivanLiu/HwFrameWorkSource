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
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
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
    private long mNatTime = HwArbitrationDEFS.DelayTimeMillisB;
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
        protected long mLastWakeUp = 0;
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
            if (currentTime != this.mLastWakeUp) {
                this.mWakeUpQueue.add(Long.valueOf(currentTime));
            }
            this.mLastWakeUp = currentTime;
            if (this.mWakeUpQueue.size() >= AwareWakeUpManager.this.mThresholdPkgOverload && currentTime - ((Long) this.mWakeUpQueue.remove(0)).longValue() < AwareWakeUpManager.this.mIntervalOverload) {
                AlarmManagerDumpRadar.getInstance().reportAlarmEvent(1, this.mUid, this.mPkg, null, null);
                if (!AwareWakeUpManager.this.isSystemUnRemoveApp(this.mUid)) {
                    AwareWakeUpManager.this.reportAlarmOverload(this.mUid, this.mPkg, null);
                }
                if (AwareWakeUpManager.this.mIsDebugMode) {
                    AwareWakeUpManager awareWakeUpManager = AwareWakeUpManager.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("EVENT_PKG_OVERLOAD uid = ");
                    stringBuilder.append(this.mUid);
                    stringBuilder.append(", pkg = ");
                    stringBuilder.append(this.mPkg);
                    awareWakeUpManager.debugLog(stringBuilder.toString());
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
            this.mWakeUpQueue.add(Long.valueOf(currentTime));
        }

        protected void wakeUp(long currentTime) {
            if (this.mLastWakeUp != currentTime) {
                this.mWakeUpQueue.add(Long.valueOf(currentTime));
            }
            if (this.mWakeUpQueue.size() >= AwareWakeUpManager.this.mThresholdTagOverload && currentTime - ((Long) this.mWakeUpQueue.remove(0)).longValue() < AwareWakeUpManager.this.mIntervalOverload) {
                AwareWakeUpManager.this.reportAlarmOverload(this.mUid, this.mPkg, this.mTag);
                AlarmManagerDumpRadar.getInstance().reportAlarmEvent(1, this.mUid, this.mPkg, this.mTag, null);
                if (AwareWakeUpManager.this.mIsDebugMode) {
                    AwareWakeUpManager awareWakeUpManager = AwareWakeUpManager.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("EVENT_TAG_OVERLOAD uid = ");
                    stringBuilder.append(this.mUid);
                    stringBuilder.append(", pkg = ");
                    stringBuilder.append(this.mPkg);
                    stringBuilder.append(", tag = ");
                    stringBuilder.append(this.mTag);
                    awareWakeUpManager.debugLog(stringBuilder.toString());
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

    private AwareWakeUpManager() {
        this.mPushTags.add("com.huawei.intent.action.PUSH");
        this.mPushTags.add("com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT");
    }

    /* JADX WARNING: Missing block: B:18:0x0041, code:
            if (r3 != 0) goto L_0x004d;
     */
    /* JADX WARNING: Missing block: B:20:0x0045, code:
            if (r1.mIsDebugMode == false) goto L_0x004c;
     */
    /* JADX WARNING: Missing block: B:21:0x0047, code:
            debugLog("ERROR overload without wakeup");
     */
    /* JADX WARNING: Missing block: B:22:0x004c, code:
            return;
     */
    /* JADX WARNING: Missing block: B:24:0x0053, code:
            if (r1.mNatTime <= r1.mIntervalWindowLength) goto L_0x0058;
     */
    /* JADX WARNING: Missing block: B:25:0x0055, code:
            r7 = r1.mIntervalWindowLength;
     */
    /* JADX WARNING: Missing block: B:26:0x0058, code:
            r7 = r1.mNatTime;
     */
    /* JADX WARNING: Missing block: B:27:0x005a, code:
            r9 = (r3 + r7) - r2.whenElapsed;
            r11 = r2.windowLength;
            r19 = android.os.SystemClock.elapsedRealtime();
     */
    /* JADX WARNING: Missing block: B:28:0x0067, code:
            if (r11 >= 0) goto L_0x007a;
     */
    /* JADX WARNING: Missing block: B:29:0x0069, code:
            r11 = com.android.server.AlarmManagerService.maxTriggerTime(r19, r2.whenElapsed, r2.repeatInterval) - r2.whenElapsed;
     */
    /* JADX WARNING: Missing block: B:31:0x007c, code:
            if (r11 >= r9) goto L_0x0083;
     */
    /* JADX WARNING: Missing block: B:32:0x007e, code:
            r2.maxWhenElapsed = r2.whenElapsed + r9;
     */
    /* JADX WARNING: Missing block: B:34:0x0085, code:
            if (r1.mIsDebugMode == false) goto L_0x00cb;
     */
    /* JADX WARNING: Missing block: B:35:0x0087, code:
            r0 = new java.lang.StringBuilder();
            r0.append("EVENT_EXTEND tag = ");
            r0.append(r2.statsTag);
            r0.append(", when = ");
            r0.append(r2.whenElapsed);
            r0.append(", maxWhen = ");
            r0.append(r2.maxWhenElapsed);
            r0.append(", orignWindow = ");
            r0.append(r2.windowLength);
            r0.append(", androidWindow = ");
            r0.append(r11);
            r0.append(", window = ");
            r0.append(r9);
            debugLog(r0.toString());
     */
    /* JADX WARNING: Missing block: B:36:0x00cb, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void extend(Alarm alarm) {
        Alarm alarm2 = alarm;
        synchronized (this.mAlarmWakeupInfo) {
            HashMap<String, PackageWakeupInfo> packageWakeupInfos = (HashMap) this.mAlarmWakeupInfo.get(Integer.valueOf(UserHandle.getUserId(alarm2.uid)));
            if (packageWakeupInfos == null) {
                return;
            }
            PackageWakeupInfo packageWakeupInfo = (PackageWakeupInfo) packageWakeupInfos.get(alarm2.packageName);
            if (packageWakeupInfo == null) {
                return;
            }
            TagWakeupInfo tagWakeupInfo = (TagWakeupInfo) packageWakeupInfo.mWakeUpMap.get(alarm2.statsTag);
            if (tagWakeupInfo == null) {
                return;
            }
            long lastWakeUpTime = tagWakeupInfo.mLastWakeUp;
        }
    }

    protected void mute(Alarm alarm) {
        alarm.wakeup = false;
        if (this.mIsDebugMode) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("EVENT_MUTE tag = ");
            stringBuilder.append(alarm.statsTag);
            debugLog(stringBuilder.toString());
        }
    }

    private ControlType increaseControlLevel(int uid, String pkg, String tag, ControlType policy) {
        int alarmType;
        switch (policy) {
            case DO_NOTHING:
            case UNKNOWN:
            case PERCEPTIBLE:
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
            case EXTEND:
            case EXTEND_TOPN:
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
            case EXTEND_AND_MUTE:
            case DECIDE_OVERLOAD:
                policy = ControlType.DECIDE_OVERLOAD;
                break;
        }
        AlarmManagerDumpRadar.getInstance().reportAlarmEvent(2, uid, pkg, tag, policy);
        if (this.mIsDebugMode) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("EVENT_CONTROLED uid = ");
            stringBuilder.append(uid);
            stringBuilder.append(", pkg = ");
            stringBuilder.append(pkg);
            stringBuilder.append(", tag = ");
            stringBuilder.append(tag);
            stringBuilder.append(", policy = ");
            stringBuilder.append(policy);
            debugLog(stringBuilder.toString());
        }
        String str;
        StringBuilder stringBuilder2;
        if (policy.ordinal() > ControlType.PERCEPTIBLE.ordinal()) {
            str = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("alarm overload uid = ");
            stringBuilder2.append(uid);
            stringBuilder2.append(", pkg = ");
            stringBuilder2.append(pkg);
            stringBuilder2.append(", tag = ");
            stringBuilder2.append(tag);
            stringBuilder2.append(", policy = ");
            stringBuilder2.append(policy);
            AwareLog.i(str, stringBuilder2.toString());
        } else {
            str = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("alarm overload but not control uid = ");
            stringBuilder2.append(uid);
            stringBuilder2.append(", pkg = ");
            stringBuilder2.append(pkg);
            stringBuilder2.append(", tag = ");
            stringBuilder2.append(tag);
            stringBuilder2.append(", policy = ");
            stringBuilder2.append(policy);
            AwareLog.i(str, stringBuilder2.toString());
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
            DecisionMaker.getInstance().updateRule(AppMngFeature.BROADCAST, context);
            this.mIsInit = true;
        }
    }

    private boolean isTopImEmail(String pkg) {
        return AwareIntelligentRecg.getInstance().isAppMngSpecTypeFreqTopN(pkg, 1, -1) || AwareIntelligentRecg.getInstance().isAppMngSpecTypeFreqTopN(pkg, 0, -1);
    }

    private int getAlarmType(int uid, String pkg, String tag) {
        String[] strs = tag.split(":");
        if (strs.length != 2) {
            return -1;
        }
        return AwareIntelligentRecg.getInstance().getAlarmActionType(uid, pkg, strs[1]);
    }

    public void reportWakeupSystem(String reason) {
        if (AlarmManagerFeature.isEnable() && this.mIsInit && !this.mIsScreenOn.get() && reason != null && reason.contains("RTC")) {
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
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("EVENT_OVERLOAD_SYSTEM at ");
                stringBuilder.append(wakeupTime);
                debugLog(stringBuilder.toString());
            }
            Iterator it = this.mSystemWakeupQueue.iterator();
            while (it.hasNext()) {
                TagWakeupInfo tagInfo = (TagWakeupInfo) it.next();
                if (!isSystemUnRemoveApp(tagInfo.mUid)) {
                    reportAlarmOverload(tagInfo.mUid, tagInfo.mPkg, tagInfo.mTag);
                }
            }
            this.mSystemWakeupQueue.clear();
            this.mSystemWakeupTimeQueue.clear();
        }
        if (this.mIsDebugMode) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("EVENT_WAKEUP_SYSTEM at ");
            stringBuilder2.append(wakeupTime);
            debugLog(stringBuilder2.toString());
        }
    }

    public void reportWakeupAlarms(ArrayList<Alarm> alarms) {
        if (AlarmManagerFeature.isEnable() && !this.mIsScreenOn.get() && this.mIsInit && alarms != null && !alarms.isEmpty()) {
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
                if (wakeupTime - ((TagWakeupInfo) alarmList.get(0)).mLastWakeUp <= this.mIntervalWakeup) {
                    break;
                }
                lruIter.remove();
            }
            this.mLastRecentWakeupAlarm.add(alarmList);
        }
    }

    private void recordWakeupAlarmLocked(ArrayList<TagWakeupInfo> alarms) {
        ArrayList<TagWakeupInfo> arrayList = alarms;
        long wakeupTime = ((TagWakeupInfo) arrayList.get(0)).mLastWakeUp;
        int size = alarms.size();
        for (int i = 0; i < size; i++) {
            TagWakeupInfo alarm = (TagWakeupInfo) arrayList.get(i);
            int uid = alarm.mUid;
            int userId = UserHandle.getUserId(uid);
            String pkg = alarm.mPkg;
            String tag = alarm.mTag;
            if (this.mIsDebugMode) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("EVENT_WAKEUP uid = ");
                stringBuilder.append(uid);
                stringBuilder.append(", pkg = ");
                stringBuilder.append(pkg);
                stringBuilder.append(", tag = ");
                stringBuilder.append(tag);
                stringBuilder.append(" at ");
                stringBuilder.append(wakeupTime);
                debugLog(stringBuilder.toString());
            }
            HashMap<String, PackageWakeupInfo> packageInfos = (HashMap) this.mAlarmWakeupInfo.get(Integer.valueOf(userId));
            if (packageInfos == null) {
                packageInfos = new HashMap();
            }
            HashMap<String, PackageWakeupInfo> packageInfos2 = packageInfos;
            PackageWakeupInfo packageInfo = (PackageWakeupInfo) packageInfos2.get(pkg);
            if (packageInfo == null) {
                packageInfo = new PackageWakeupInfo(uid, pkg);
            }
            PackageWakeupInfo packageInfo2 = packageInfo;
            packageInfo2.wakeUp(tag, wakeupTime);
            packageInfos2.put(pkg, packageInfo2);
            this.mAlarmWakeupInfo.put(Integer.valueOf(userId), packageInfos2);
            this.mSystemWakeupQueue.add((TagWakeupInfo) packageInfo2.mWakeUpMap.get(tag));
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
        if (AlarmManagerFeature.isEnable() && alarm != null) {
            int userId = UserHandle.getUserId(alarm.uid);
            if (this.mIsDebugMode && userId == this.mDebugUserId && this.mDebugPkg.equals(alarm.packageName) && (this.mDebugTag.equals(alarm.statsTag) || "all".equals(this.mDebugTag))) {
                alarm.whenElapsed += this.mDebugDelay.longValue();
                alarm.maxWhenElapsed += this.mDebugDelay.longValue();
            } else if (this.mIsScreenOn.get()) {
                boolean z = alarm.type == 2 || alarm.type == 0;
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
        if ((uid <= 0 || uid >= 10000) && !this.mSystemUnremoveUidCache.checkUidExist(uid)) {
            return false;
        }
        return true;
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
                    String[] config = rawConfig.split("\\|");
                    int j = 1;
                    if (config.length < 1) {
                        AwareLog.e(TAG, "format error in alarm manager config");
                        return;
                    }
                    ArraySet<String> tagList = new ArraySet();
                    while (j < config.length) {
                        tagList.add(config[j]);
                        j++;
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
        if (tagList.isEmpty() || tagList.contains(tag)) {
            return true;
        }
        return false;
    }

    public void updateControlParam() {
        ArrayList<String> controlParam = DecisionMaker.getInstance().getRawConfig(AppMngFeature.APP_ALARM.getDesc(), TAG_CONTROL_PARAM);
        ArrayList<String> arrayList;
        if (controlParam == null) {
        } else if (controlParam.size() != 7) {
            arrayList = controlParam;
        } else {
            String rawPushTags = "";
            int i = 0;
            int i2;
            try {
                int thresholdSysOverload = Integer.parseInt((String) controlParam.get(0));
                try {
                    int thresholdPkgOverload = Integer.parseInt((String) controlParam.get(1));
                    int thresholdTagOverload = Integer.parseInt((String) controlParam.get(2));
                    long intervalOverload = Long.parseLong((String) controlParam.get(3));
                    long intervalWakeup = Long.parseLong((String) controlParam.get(4));
                    long intervalWindowLength = Long.parseLong((String) controlParam.get(5));
                    rawPushTags = (String) controlParam.get(6);
                    if (rawPushTags == null) {
                        i2 = thresholdSysOverload;
                    } else if ("".equals(rawPushTags)) {
                        arrayList = controlParam;
                        i2 = thresholdSysOverload;
                    } else {
                        String[] pushTagString = rawPushTags.split("\\|");
                        ArraySet<String> pushTags = new ArraySet();
                        while (i < pushTagString.length) {
                            if (pushTagString[i] != null) {
                                arrayList = controlParam;
                                if (!pushTagString[i].equals("")) {
                                    pushTags.add(pushTagString[i]);
                                }
                            } else {
                                arrayList = controlParam;
                            }
                            i++;
                            controlParam = arrayList;
                        }
                        this.mThresholdSysOverload = thresholdSysOverload;
                        this.mThresholdPkgOverload = thresholdPkgOverload;
                        this.mThresholdTagOverload = thresholdTagOverload;
                        this.mIntervalOverloadDefault = intervalOverload;
                        this.mIntervalOverload = computeIntervalOverload(this.mNatTime, intervalOverload);
                        this.mIntervalWakeup = intervalWakeup;
                        this.mIntervalWindowLength = intervalWindowLength;
                        this.mPushTags = pushTags;
                        return;
                    }
                    AwareLog.e(TAG, "invalid push tags");
                } catch (NumberFormatException e) {
                    arrayList = controlParam;
                    i2 = thresholdSysOverload;
                    AwareLog.e(TAG, "control param in wrong format");
                }
            } catch (NumberFormatException e2) {
                arrayList = controlParam;
                i2 = 0;
                AwareLog.e(TAG, "control param in wrong format");
            }
        }
    }

    public void setIntervalOverload(int time) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("nat time changed : ");
        stringBuilder.append(time);
        AwareLog.i(str, stringBuilder.toString());
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
        long maxInterval = 2 * natTime;
        return (interval > maxInterval ? maxInterval : interval) - 10000;
    }

    public void dumpParam(PrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("thresholdSysOverload : ");
        stringBuilder.append(this.mThresholdSysOverload);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("thresholdPkgOverload : ");
        stringBuilder.append(this.mThresholdPkgOverload);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("thresholdTagOverload : ");
        stringBuilder.append(this.mThresholdTagOverload);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("intervalOverloadDefault : ");
        stringBuilder.append(this.mIntervalOverloadDefault);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("intervalOverload : ");
        stringBuilder.append(this.mIntervalOverload);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("intervalWakeup : ");
        stringBuilder.append(this.mIntervalWakeup);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("intervalWindowLength : ");
        stringBuilder.append(this.mIntervalWindowLength);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("natTime : ");
        stringBuilder.append(this.mNatTime);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("pushNatDetecting : ");
        stringBuilder.append(this.mHWPushNatDetecting);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("pushTags : ");
        stringBuilder.append(this.mPushTags);
        pw.println(stringBuilder.toString());
    }

    private void debugLog(String log) {
        synchronized (this.mDebugLog) {
            List list = this.mDebugLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getCurTime());
            stringBuilder.append(": ");
            stringBuilder.append(log);
            list.add(stringBuilder.toString());
        }
    }
}
