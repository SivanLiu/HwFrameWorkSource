package com.android.server.rms.iaware.appmng;

import android.app.mtm.iaware.appmng.AppMngConstant;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.rms.iaware.AwareLog;
import android.util.ArraySet;
import com.android.internal.os.SomeArgs;
import com.android.server.HwAlarmManagerService;
import com.android.server.mtm.iaware.appmng.DecisionMaker;
import com.android.server.mtm.iaware.appmng.appstart.datamgr.SystemUnremoveUidCache;
import com.android.server.mtm.iaware.appmng.rule.AppMngRule;
import com.android.server.rms.iaware.feature.AlarmManagerFeature;
import com.huawei.hiai.awareness.AwarenessInnerConstants;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    private final HashMap<Integer, HashMap<String, PackageControlPolicy>> mAlarmControlPolicy = new HashMap<>();
    /* access modifiers changed from: private */
    public final HashMap<Integer, HashMap<String, PackageWakeupInfo>> mAlarmWakeupInfo = new HashMap<>();
    private Long mDebugDelay = 0L;
    private final List<String> mDebugLog = new ArrayList();
    private String mDebugPkg = "";
    private String mDebugTag = "";
    private int mDebugUserId = -1;
    private boolean mHWPushNatDetecting = false;
    private Handler mHandler;
    /* access modifiers changed from: private */
    public long mIntervalOverload = this.mIntervalOverloadDefault;
    private long mIntervalOverloadDefault = 600000;
    private long mIntervalWakeup = 1000;
    private long mIntervalWindowLength = 600000;
    /* access modifiers changed from: private */
    public boolean mIsDebugMode = false;
    private boolean mIsInit = false;
    private AtomicBoolean mIsScreenOn = new AtomicBoolean(true);
    private ArrayList<ArrayList<TagWakeupInfo>> mLastRecentWakeupAlarm = new ArrayList<>();
    private long mLastSysWakeup = 0;
    private long mNatTime = 300000;
    private ArraySet<String> mPushTags = new ArraySet<>();
    private SystemUnremoveUidCache mSystemUnremoveUidCache;
    private final ArraySet<TagWakeupInfo> mSystemWakeupQueue = new ArraySet<>();
    private final List<Long> mSystemWakeupTimeQueue = new ArrayList();
    /* access modifiers changed from: private */
    public int mThresholdPkgOverload = 4;
    private int mThresholdSysOverload = 6;
    /* access modifiers changed from: private */
    public int mThresholdTagOverload = 3;
    private HashMap<String, ArraySet<String>> mWhiteList = new HashMap<>();

    public enum ControlType {
        DO_NOTHING,
        IMPORTANT,
        UNKNOWN,
        PERCEPTIBLE,
        EXTEND {
            /* access modifiers changed from: protected */
            @Override // com.android.server.rms.iaware.appmng.AwareWakeUpManager.ControlType
            public void apply(HwAlarmManagerService.HwAlarm alarm) {
                AwareWakeUpManager.getInstance().extend(alarm);
            }
        },
        EXTEND_TOPN {
            /* access modifiers changed from: protected */
            @Override // com.android.server.rms.iaware.appmng.AwareWakeUpManager.ControlType
            public void apply(HwAlarmManagerService.HwAlarm alarm) {
                AwareWakeUpManager.getInstance().extend(alarm);
            }
        },
        EXTEND_AND_MUTE {
            /* access modifiers changed from: protected */
            @Override // com.android.server.rms.iaware.appmng.AwareWakeUpManager.ControlType
            public void apply(HwAlarmManagerService.HwAlarm alarm) {
                AwareWakeUpManager.getInstance().extend(alarm);
                AwareWakeUpManager.getInstance().mute(alarm);
            }
        },
        DECIDE_OVERLOAD {
            /* access modifiers changed from: protected */
            @Override // com.android.server.rms.iaware.appmng.AwareWakeUpManager.ControlType
            public void apply(HwAlarmManagerService.HwAlarm alarm) {
                AwareWakeUpManager.getInstance().extend(alarm);
                AwareWakeUpManager.getInstance().mute(alarm);
            }
        };

        /* access modifiers changed from: protected */
        public void apply(HwAlarmManagerService.HwAlarm alarm) {
        }
    }

    private AwareWakeUpManager() {
        this.mPushTags.add("com.huawei.intent.action.PUSH");
        this.mPushTags.add("com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT");
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0045, code lost:
        if (r7 != 0) goto L_0x0051;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0049, code lost:
        if (r20.mIsDebugMode == false) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x004b, code lost:
        debugLog("ERROR overload without wakeup");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x0051, code lost:
        r6 = r20.mNatTime;
        r8 = r20.mIntervalWindowLength;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0057, code lost:
        if (r6 <= r8) goto L_0x005a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x0059, code lost:
        r6 = r8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x005a, code lost:
        r8 = (r7 + r6) - r21.getWhenElapsed();
        r10 = r21.getWindowLength();
        r18 = android.os.SystemClock.elapsedRealtime();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x006b, code lost:
        if (r10 >= 0) goto L_0x0082;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x006e, code lost:
        r10 = com.android.server.HwAlarmManagerService.maxTriggerTime(r18, r21.getWhenElapsed(), r21.getRepeatInterval()) - r21.getWhenElapsed();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0084, code lost:
        if (r10 >= r8) goto L_0x0091;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x0086, code lost:
        r21.setMaxWhenElapsed(r21.getWhenElapsed() + r8);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x0095, code lost:
        if (r20.mIsDebugMode == false) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x0097, code lost:
        debugLog("EVENT_EXTEND tag = " + r21.getStatsTag() + ", when = " + r21.getWhenElapsed() + ", maxWhen = " + r21.getMaxWhenElapsed() + ", orignWindow = " + r21.getWindowLength() + ", androidWindow = " + r10 + ", window = " + r8);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:40:0x00e9, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:43:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:44:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:46:?, code lost:
        return;
     */
    public void extend(HwAlarmManagerService.HwAlarm alarm) {
        synchronized (this.mAlarmWakeupInfo) {
            HashMap<String, PackageWakeupInfo> packageWakeupInfos = this.mAlarmWakeupInfo.get(Integer.valueOf(UserHandle.getUserId(alarm.getUid())));
            if (packageWakeupInfos != null) {
                PackageWakeupInfo packageWakeupInfo = packageWakeupInfos.get(alarm.getPkgName());
                if (packageWakeupInfo != null) {
                    TagWakeupInfo tagWakeupInfo = packageWakeupInfo.mWakeUpMap.get(alarm.getStatsTag());
                    if (tagWakeupInfo != null) {
                        long lastWakeUpTime = tagWakeupInfo.mLastWakeUp;
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
        while (true) {
        }
    }

    /* access modifiers changed from: protected */
    public void mute(HwAlarmManagerService.HwAlarm alarm) {
        alarm.setWakeup(false);
        if (this.mIsDebugMode) {
            debugLog("EVENT_MUTE tag = " + alarm.getStatsTag());
        }
    }

    /* access modifiers changed from: protected */
    public class PackageWakeupInfo {
        protected long mLastWakeUp = 0;
        protected String mPkg;
        protected int mUid;
        protected HashMap<String, TagWakeupInfo> mWakeUpMap = new HashMap<>();
        protected List<Long> mWakeUpQueue = new ArrayList();

        protected PackageWakeupInfo(int uid, String pkg) {
            this.mUid = uid;
            this.mPkg = pkg;
        }

        /* access modifiers changed from: protected */
        public void wakeUp(String tag, long currentTime) {
            TagWakeupInfo tagInfo = this.mWakeUpMap.get(tag);
            if (tagInfo == null) {
                tagInfo = new TagWakeupInfo(this.mUid, this.mPkg, tag, currentTime);
            }
            if (currentTime != this.mLastWakeUp) {
                this.mWakeUpQueue.add(Long.valueOf(currentTime));
            }
            this.mLastWakeUp = currentTime;
            if (this.mWakeUpQueue.size() >= AwareWakeUpManager.this.mThresholdPkgOverload && currentTime - this.mWakeUpQueue.remove(0).longValue() < AwareWakeUpManager.this.mIntervalOverload) {
                AlarmManagerDumpRadar.getInstance().reportAlarmEvent(1, this.mUid, this.mPkg, null, null);
                if (!AwareWakeUpManager.this.isSystemUnRemoveApp(this.mUid)) {
                    AwareWakeUpManager.this.reportAlarmOverload(this.mUid, this.mPkg, null);
                }
                if (AwareWakeUpManager.this.mIsDebugMode) {
                    AwareWakeUpManager awareWakeUpManager = AwareWakeUpManager.this;
                    awareWakeUpManager.debugLog("EVENT_PKG_OVERLOAD uid = " + this.mUid + ", pkg = " + this.mPkg);
                }
                this.mWakeUpQueue.clear();
            }
            tagInfo.wakeUp(currentTime);
            this.mWakeUpMap.put(tag, tagInfo);
        }

        /* access modifiers changed from: protected */
        public TagWakeupInfo get(String tag) {
            return this.mWakeUpMap.get(tag);
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

        /* access modifiers changed from: protected */
        public void wakeUp(long currentTime) {
            if (this.mLastWakeUp != currentTime) {
                this.mWakeUpQueue.add(Long.valueOf(currentTime));
            }
            if (this.mWakeUpQueue.size() >= AwareWakeUpManager.this.mThresholdTagOverload && currentTime - this.mWakeUpQueue.remove(0).longValue() < AwareWakeUpManager.this.mIntervalOverload) {
                AwareWakeUpManager.this.reportAlarmOverload(this.mUid, this.mPkg, this.mTag);
                AlarmManagerDumpRadar.getInstance().reportAlarmEvent(1, this.mUid, this.mPkg, this.mTag, null);
                if (AwareWakeUpManager.this.mIsDebugMode) {
                    AwareWakeUpManager awareWakeUpManager = AwareWakeUpManager.this;
                    awareWakeUpManager.debugLog("EVENT_TAG_OVERLOAD uid = " + this.mUid + ", pkg = " + this.mPkg + ", tag = " + this.mTag);
                }
                this.mWakeUpQueue.clear();
            }
            this.mLastWakeUp = currentTime;
        }
    }

    protected class PackageControlPolicy {
        protected String mPkg;
        protected HashMap<String, TagControlPolicy> mTagPolicyMap = new HashMap<>();
        protected int mUid;

        protected PackageControlPolicy(int uid, String pkg) {
            this.mUid = uid;
            this.mPkg = pkg;
        }

        /* access modifiers changed from: protected */
        public void overload(String tag) {
            PackageWakeupInfo packageWakeupInfo;
            if (tag == null) {
                HashMap<String, PackageWakeupInfo> packageWakeupInfos = (HashMap) AwareWakeUpManager.this.mAlarmWakeupInfo.get(Integer.valueOf(UserHandle.getUserId(this.mUid)));
                if (packageWakeupInfos != null && (packageWakeupInfo = packageWakeupInfos.get(this.mPkg)) != null) {
                    for (Map.Entry<String, TagWakeupInfo> entry : packageWakeupInfo.mWakeUpMap.entrySet()) {
                        tagOverload(entry.getValue().mTag);
                    }
                    return;
                }
                return;
            }
            tagOverload(tag);
        }

        private void tagOverload(String tag) {
            TagControlPolicy tagPolicy = this.mTagPolicyMap.get(tag);
            if (tagPolicy == null) {
                tagPolicy = new TagControlPolicy(this.mUid, this.mPkg, tag);
            }
            tagPolicy.overload();
            this.mTagPolicyMap.put(tag, tagPolicy);
        }

        /* access modifiers changed from: protected */
        public void apply(HwAlarmManagerService.HwAlarm alarm) {
            TagControlPolicy tagPolicy = this.mTagPolicyMap.get(alarm.getStatsTag());
            if (tagPolicy != null) {
                tagPolicy.apply(alarm);
            }
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

        /* access modifiers changed from: protected */
        public void overload() {
            if (AwareWakeUpManager.this.isInWhiteList(this.mPkg, this.mTag)) {
                this.mPolicy = ControlType.IMPORTANT;
            }
            this.mPolicy = AwareWakeUpManager.this.increaseControlLevel(this.mUid, this.mPkg, this.mTag, this.mPolicy);
        }

        /* access modifiers changed from: protected */
        public void apply(HwAlarmManagerService.HwAlarm alarm) {
            this.mPolicy.apply(alarm);
        }
    }

    /* access modifiers changed from: private */
    public ControlType increaseControlLevel(int uid, String pkg, String tag, ControlType policy) {
        switch (policy) {
            case DO_NOTHING:
            case UNKNOWN:
            case PERCEPTIBLE:
                int alarmType = getAlarmType(uid, pkg, tag);
                if (alarmType != 4) {
                    if (alarmType != 3) {
                        policy = ControlType.UNKNOWN;
                        break;
                    } else {
                        policy = ControlType.PERCEPTIBLE;
                        break;
                    }
                } else {
                    policy = ControlType.EXTEND;
                    break;
                }
            case EXTEND:
            case EXTEND_TOPN:
                int alarmType2 = getAlarmType(uid, pkg, tag);
                if (alarmType2 != 4) {
                    if (alarmType2 != 3) {
                        policy = ControlType.UNKNOWN;
                        break;
                    } else {
                        policy = ControlType.PERCEPTIBLE;
                        break;
                    }
                } else if (isTopImEmail(pkg)) {
                    policy = ControlType.EXTEND_TOPN;
                    break;
                } else {
                    policy = ControlType.EXTEND_AND_MUTE;
                    break;
                }
            case EXTEND_AND_MUTE:
            case DECIDE_OVERLOAD:
                policy = ControlType.DECIDE_OVERLOAD;
                break;
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
            DecisionMaker.getInstance().updateRule(AppMngConstant.AppMngFeature.APP_ALARM, context);
            updateWhiteList();
            updateControlParam();
            DecisionMaker.getInstance().updateRule(AppMngConstant.AppMngFeature.BROADCAST, context);
            this.mIsInit = true;
        }
    }

    private final class WakeUpHandler extends Handler {
        public WakeUpHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                SomeArgs args = (SomeArgs) msg.obj;
                long wakeupTime = ((Long) args.arg2).longValue();
                AwareWakeUpManager.this.handleWakeupAlarm((ArrayList) args.arg1, wakeupTime);
            } else if (i == 2) {
                AwareWakeUpManager.this.handleWakeupSystem(((Long) msg.obj).longValue());
            }
        }
    }

    private boolean isTopImEmail(String pkg) {
        return AwareIntelligentRecg.getInstance().isAppMngSpecTypeFreqTopN(pkg, 1, -1) || AwareIntelligentRecg.getInstance().isAppMngSpecTypeFreqTopN(pkg, 0, -1);
    }

    private int getAlarmType(int uid, String pkg, String tag) {
        String[] strs = tag.split(AwarenessInnerConstants.COLON_KEY);
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

    /* access modifiers changed from: private */
    public void handleWakeupSystem(long wakeupTime) {
        synchronized (this.mAlarmWakeupInfo) {
            Iterator lruIter = this.mLastRecentWakeupAlarm.iterator();
            while (true) {
                if (!lruIter.hasNext()) {
                    break;
                }
                ArrayList<TagWakeupInfo> alarmList = lruIter.next();
                if (wakeupTime - alarmList.get(0).mLastWakeUp < this.mIntervalWakeup) {
                    recordWakeupAlarmLocked(alarmList);
                    recordWakeupSystemLocked(wakeupTime);
                    wakeupTime = 0;
                    break;
                }
            }
            this.mLastRecentWakeupAlarm = new ArrayList<>();
            this.mLastSysWakeup = wakeupTime;
        }
    }

    private void recordWakeupSystemLocked(long wakeupTime) {
        AlarmManagerDumpRadar.getInstance().reportSystemEvent(0);
        this.mSystemWakeupTimeQueue.add(Long.valueOf(wakeupTime));
        if (this.mSystemWakeupTimeQueue.size() >= this.mThresholdSysOverload && wakeupTime - this.mSystemWakeupTimeQueue.remove(0).longValue() < this.mIntervalOverload) {
            AlarmManagerDumpRadar.getInstance().reportSystemEvent(1);
            if (this.mIsDebugMode) {
                debugLog("EVENT_OVERLOAD_SYSTEM at " + wakeupTime);
            }
            Iterator<TagWakeupInfo> it = this.mSystemWakeupQueue.iterator();
            while (it.hasNext()) {
                TagWakeupInfo tagInfo = it.next();
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

    public void reportWakeupAlarms(ArrayList<HwAlarmManagerService.HwAlarm> alarms) {
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

    /* access modifiers changed from: private */
    public void handleWakeupAlarm(ArrayList<HwAlarmManagerService.HwAlarm> alarms, long wakeupTime) {
        synchronized (this.mAlarmWakeupInfo) {
            ArrayList<TagWakeupInfo> alarmList = new ArrayList<>();
            int size = alarms.size();
            for (int i = 0; i < size; i++) {
                alarmList.add(new TagWakeupInfo(alarms.get(i).getUid(), alarms.get(i).getPkgName(), alarms.get(i).getStatsTag(), wakeupTime));
            }
            if (wakeupTime - this.mLastSysWakeup < this.mIntervalWakeup) {
                recordWakeupAlarmLocked(alarmList);
                recordWakeupSystemLocked(this.mLastSysWakeup);
                this.mLastRecentWakeupAlarm = new ArrayList<>();
                this.mLastSysWakeup = 0;
                return;
            }
            Iterator lruIter = this.mLastRecentWakeupAlarm.iterator();
            while (lruIter.hasNext()) {
                alarmList = lruIter.next();
                if (wakeupTime - alarmList.get(0).mLastWakeUp <= this.mIntervalWakeup) {
                    break;
                }
                lruIter.remove();
            }
            this.mLastRecentWakeupAlarm.add(alarmList);
        }
    }

    private void recordWakeupAlarmLocked(ArrayList<TagWakeupInfo> alarms) {
        HashMap<String, PackageWakeupInfo> packageInfos;
        long wakeupTime = alarms.get(0).mLastWakeUp;
        int size = alarms.size();
        for (int i = 0; i < size; i++) {
            TagWakeupInfo alarm = alarms.get(i);
            int uid = alarm.mUid;
            int userId = UserHandle.getUserId(uid);
            String pkg = alarm.mPkg;
            String tag = alarm.mTag;
            if (this.mIsDebugMode) {
                debugLog("EVENT_WAKEUP uid = " + uid + ", pkg = " + pkg + ", tag = " + tag + " at " + wakeupTime);
            }
            HashMap<String, PackageWakeupInfo> packageInfos2 = this.mAlarmWakeupInfo.get(Integer.valueOf(userId));
            if (packageInfos2 == null) {
                packageInfos = new HashMap<>();
            } else {
                packageInfos = packageInfos2;
            }
            PackageWakeupInfo packageInfo = packageInfos.get(pkg);
            if (packageInfo == null) {
                packageInfo = new PackageWakeupInfo(uid, pkg);
            }
            packageInfo.wakeUp(tag, wakeupTime);
            packageInfos.put(pkg, packageInfo);
            this.mAlarmWakeupInfo.put(Integer.valueOf(userId), packageInfos);
            this.mSystemWakeupQueue.add(packageInfo.mWakeUpMap.get(tag));
            AlarmManagerDumpRadar.getInstance().reportAlarmEvent(0, uid, pkg, tag, null);
        }
        Iterator wakeupIter = this.mSystemWakeupQueue.iterator();
        while (wakeupIter.hasNext()) {
            if (wakeupTime - wakeupIter.next().mLastWakeUp > this.mIntervalOverload) {
                wakeupIter.remove();
            }
        }
    }

    /* access modifiers changed from: private */
    public void reportAlarmOverload(int uid, String pkg, String tag) {
        int userId = UserHandle.getUserId(uid);
        synchronized (this.mAlarmWakeupInfo) {
            HashMap<String, PackageControlPolicy> packageControlPolices = this.mAlarmControlPolicy.get(Integer.valueOf(userId));
            if (packageControlPolices == null) {
                packageControlPolices = new HashMap<>();
            }
            PackageControlPolicy packagePolicy = packageControlPolices.get(pkg);
            if (packagePolicy == null) {
                packagePolicy = new PackageControlPolicy(uid, pkg);
            }
            packagePolicy.overload(tag);
            packageControlPolices.put(pkg, packagePolicy);
            this.mAlarmControlPolicy.put(Integer.valueOf(userId), packageControlPolices);
        }
    }

    public void modifyAlarmIfOverload(HwAlarmManagerService.HwAlarm alarm) {
        if (AlarmManagerFeature.isEnable() && alarm != null) {
            int userId = UserHandle.getUserId(alarm.getUid());
            if (this.mIsDebugMode && userId == this.mDebugUserId && this.mDebugPkg.equals(alarm.getPkgName()) && (this.mDebugTag.equals(alarm.getStatsTag()) || AppMngRule.VALUE_ALL.equals(this.mDebugTag))) {
                alarm.setWhenElapsed(alarm.getWhenElapsed() + this.mDebugDelay.longValue());
                alarm.setMaxWhenElapsed(alarm.getMaxWhenElapsed() + this.mDebugDelay.longValue());
            } else if (this.mIsScreenOn.get()) {
                alarm.setWakeup(alarm.getType() == 2 || alarm.getType() == 0);
            } else {
                ArraySet<String> pushTags = this.mPushTags;
                if (!this.mHWPushNatDetecting || !pushTags.contains(alarm.getStatsTag())) {
                    synchronized (this.mAlarmWakeupInfo) {
                        HashMap<String, PackageControlPolicy> packageControlPolices = this.mAlarmControlPolicy.get(Integer.valueOf(userId));
                        if (packageControlPolices != null) {
                            PackageControlPolicy packagePolicy = packageControlPolices.get(alarm.getPkgName());
                            if (packagePolicy != null) {
                                packagePolicy.apply(alarm);
                            }
                        }
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
        if (!this.mIsDebugMode) {
            pw.println("debug mod off");
            return;
        }
        synchronized (this.mDebugLog) {
            int size = this.mDebugLog.size();
            for (int i = 0; i < size; i++) {
                pw.println(this.mDebugLog.get(i));
            }
        }
    }

    private String getCurTime() {
        return new SimpleDateFormat("yyyyMMdd-HH-mm-ss-SSS").format(new Date());
    }

    /* access modifiers changed from: private */
    public boolean isSystemUnRemoveApp(int uid) {
        int uid2 = UserHandle.getAppId(uid);
        if ((uid2 <= 0 || uid2 >= 10000) && !this.mSystemUnremoveUidCache.checkUidExist(uid2)) {
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
        ArrayList<String> configList = DecisionMaker.getInstance().getRawConfig(AppMngConstant.AppMngFeature.APP_ALARM.getDesc(), TAG_WHITE_LIST);
        if (configList != null) {
            HashMap<String, ArraySet<String>> whiteList = new HashMap<>();
            int i = 0;
            int size = configList.size();
            while (i < size) {
                String rawConfig = configList.get(i);
                if (rawConfig != null) {
                    String[] config = rawConfig.split("\\|");
                    if (config.length < 1) {
                        AwareLog.e(TAG, "format error in alarm manager config");
                        return;
                    }
                    ArraySet<String> tagList = new ArraySet<>();
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

    /* access modifiers changed from: private */
    public boolean isInWhiteList(String pkg, String tag) {
        ArraySet<String> tagList = this.mWhiteList.get(pkg);
        if (tagList == null) {
            return false;
        }
        if (!tagList.isEmpty() && !tagList.contains(tag)) {
            return false;
        }
        return true;
    }

    public void updateControlParam() {
        ArrayList<String> controlParam = DecisionMaker.getInstance().getRawConfig(AppMngConstant.AppMngFeature.APP_ALARM.getDesc(), TAG_CONTROL_PARAM);
        if (controlParam == null) {
            return;
        }
        if (controlParam.size() == 7) {
            try {
                int thresholdSysOverload = Integer.parseInt(controlParam.get(0));
                int thresholdPkgOverload = Integer.parseInt(controlParam.get(1));
                int thresholdTagOverload = Integer.parseInt(controlParam.get(2));
                long intervalOverload = Long.parseLong(controlParam.get(3));
                long intervalWakeup = Long.parseLong(controlParam.get(4));
                long intervalWindowLength = Long.parseLong(controlParam.get(5));
                String rawPushTags = controlParam.get(6);
                if (rawPushTags != null) {
                    if (!"".equals(rawPushTags)) {
                        String[] pushTagString = rawPushTags.split("\\|");
                        ArraySet<String> pushTags = new ArraySet<>();
                        int i = 0;
                        while (i < pushTagString.length) {
                            if (pushTagString[i] != null && !pushTagString[i].equals("")) {
                                pushTags.add(pushTagString[i]);
                            }
                            i++;
                            rawPushTags = rawPushTags;
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
                }
                AwareLog.e(TAG, "invalid push tags");
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
        long maxInterval = 2 * natTime;
        return (interval > maxInterval ? maxInterval : interval) - 10000;
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

    /* access modifiers changed from: private */
    public void debugLog(String log) {
        synchronized (this.mDebugLog) {
            List<String> list = this.mDebugLog;
            list.add(getCurTime() + ": " + log);
        }
    }
}
