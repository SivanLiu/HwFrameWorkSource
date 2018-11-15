package com.android.server.rms.iaware.appmng;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.AwareLog;
import com.android.internal.os.SomeArgs;
import com.android.server.rms.iaware.appmng.AwareWakeUpManager.ControlType;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AlarmManagerDumpRadar {
    public static final int EVENT_CONTROLED = 2;
    public static final int EVENT_OVERLOAD = 1;
    public static final int EVENT_WAKEUP = 0;
    private static final int MSG_ALARM_EVENT = 1;
    private static final String TAG = "AlarmManagerDumpRadar";
    private static final int USER_OTHER = 1;
    private static final boolean isBetaUser = (AwareConstant.CURRENT_USER_TYPE == 3);
    private static volatile AlarmManagerDumpRadar mAlarmManagerDumpRadar;
    private HashMap<Integer, HashMap<String, PackageInfo>> mAlarmWakeupInfo = new HashMap();
    private long mCleanupTime = System.currentTimeMillis();
    private Handler mHandler;
    private long mLastCheckTime = System.currentTimeMillis();
    private AtomicInteger mSystemWakeupCount = new AtomicInteger(0);
    private AtomicInteger mSystemWakeupOverloadCount = new AtomicInteger(0);

    private final class AlarmRadarHandler extends Handler {
        protected AlarmRadarHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                SomeArgs args = msg.obj;
                String pkg = args.arg1;
                String tag = args.arg2;
                ControlType policy = args.arg3;
                AlarmManagerDumpRadar.this.handleAlarmEvent(args.argi1, args.argi2, pkg, tag, policy);
            }
        }
    }

    protected static class PackageInfo {
        protected int mOverloadCount = 0;
        protected String mPkg;
        protected HashMap<String, TagInfo> mTagMap = new HashMap();
        protected int mWakeupCount = 0;

        protected PackageInfo(String pkg) {
            this.mPkg = pkg;
        }
    }

    protected static class TagInfo {
        protected int mDecideOverloadCount = 0;
        protected int mExtendCount = 0;
        protected int mMuteCount = 0;
        protected int mOverloadCount = 0;
        protected int mPerceptibleCount = 0;
        protected String mTag;
        protected int mTopNCount = 0;
        protected int mUnknownCount = 0;
        protected int mWakeupCount = 0;
        protected int mWhiteListCount = 0;

        protected TagInfo(String tag) {
            this.mTag = tag;
        }
    }

    public static AlarmManagerDumpRadar getInstance() {
        if (mAlarmManagerDumpRadar == null) {
            synchronized (AlarmManagerDumpRadar.class) {
                if (mAlarmManagerDumpRadar == null) {
                    mAlarmManagerDumpRadar = new AlarmManagerDumpRadar();
                }
            }
        }
        return mAlarmManagerDumpRadar;
    }

    public void setHandler(Handler handler) {
        if (handler != null) {
            this.mHandler = new AlarmRadarHandler(handler.getLooper());
        }
    }

    public String saveBigData(boolean clear) {
        StringBuilder data = new StringBuilder();
        data.append("\n[iAwareAlarmManager_Start]");
        data.append("\nstartTime: ");
        data.append(String.valueOf(this.mCleanupTime));
        data.append("\n");
        JSONObject bigData = new JSONObject();
        long currentTime = System.currentTimeMillis();
        synchronized (AlarmManagerDumpRadar.class) {
            try {
                JSONArray sysTitle;
                JSONArray pkgTitle;
                JSONArray tagTitle;
                JSONObject title = new JSONObject();
                JSONArray sysTitle2 = new JSONArray();
                sysTitle2.put("wakeup_count");
                sysTitle2.put("overload_count");
                title.put("sys", sysTitle2);
                JSONArray pkgTitle2 = new JSONArray();
                pkgTitle2.put("userid");
                pkgTitle2.put("wakeup_count");
                pkgTitle2.put("overload_count");
                title.put("pkg", pkgTitle2);
                JSONArray tagTitle2 = new JSONArray();
                tagTitle2.put("wakeup_count");
                tagTitle2.put("overload_count");
                tagTitle2.put("extend_count");
                tagTitle2.put("mute_count");
                tagTitle2.put("perceptible");
                tagTitle2.put("unknown");
                tagTitle2.put("topn");
                tagTitle2.put("decide_overload");
                tagTitle2.put("white_list");
                title.put("tag", tagTitle2);
                bigData.put("title", title);
                JSONArray systemData = new JSONArray();
                systemData.put(this.mSystemWakeupCount);
                systemData.put(this.mSystemWakeupOverloadCount);
                bigData.put("sys", systemData);
                JSONArray alarmData = new JSONArray();
                for (Entry<Integer, HashMap<String, PackageInfo>> userEntry : this.mAlarmWakeupInfo.entrySet()) {
                    for (Entry<String, PackageInfo> pkgEntry : ((HashMap) userEntry.getValue()).entrySet()) {
                        PackageInfo packageInfo;
                        JSONObject title2 = title;
                        title = new JSONObject();
                        sysTitle = sysTitle2;
                        sysTitle2 = new JSONArray();
                        PackageInfo packageInfo2 = (PackageInfo) pkgEntry.getValue();
                        pkgTitle = pkgTitle2;
                        sysTitle2.put(userEntry.getKey());
                        tagTitle = tagTitle2;
                        PackageInfo packageInfo3 = packageInfo2;
                        sysTitle2.put(packageInfo3.mWakeupCount);
                        sysTitle2.put(packageInfo3.mOverloadCount);
                        title.put(packageInfo3.mPkg, sysTitle2);
                        Iterator it = packageInfo3.mTagMap.entrySet().iterator();
                        while (it.hasNext()) {
                            JSONArray pkgData = sysTitle2;
                            Entry<String, TagInfo> pkgData2 = (Entry) it.next();
                            TagInfo tagInfo = (TagInfo) pkgData2.getValue();
                            Entry<String, TagInfo> tagEntry = pkgData2;
                            sysTitle2 = new JSONArray();
                            packageInfo = packageInfo3;
                            Iterator it2 = it;
                            TagInfo tagInfo2 = tagInfo;
                            sysTitle2.put(tagInfo2.mWakeupCount);
                            sysTitle2.put(tagInfo2.mOverloadCount);
                            sysTitle2.put(tagInfo2.mExtendCount);
                            sysTitle2.put(tagInfo2.mMuteCount);
                            sysTitle2.put(tagInfo2.mPerceptibleCount);
                            sysTitle2.put(tagInfo2.mUnknownCount);
                            sysTitle2.put(tagInfo2.mTopNCount);
                            sysTitle2.put(tagInfo2.mDecideOverloadCount);
                            sysTitle2.put(tagInfo2.mWhiteListCount);
                            title.put(tagInfo2.mTag, sysTitle2);
                            sysTitle2 = pkgData;
                            packageInfo3 = packageInfo;
                            it = it2;
                        }
                        packageInfo = packageInfo3;
                        alarmData.put(title);
                        title = title2;
                        sysTitle2 = sysTitle;
                        pkgTitle2 = pkgTitle;
                        tagTitle2 = tagTitle;
                    }
                    sysTitle = sysTitle2;
                    pkgTitle = pkgTitle2;
                    tagTitle = tagTitle2;
                }
                sysTitle = sysTitle2;
                pkgTitle = pkgTitle2;
                tagTitle = tagTitle2;
                bigData.put("alarm", alarmData);
            } catch (JSONException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("saveBigdata failed! catch JSONException e :");
                stringBuilder.append(e.toString());
                AwareLog.e(str, stringBuilder.toString());
            }
            if (!AwareWakeUpManager.getInstance().isDebugMode() && clear) {
                this.mAlarmWakeupInfo = new HashMap();
                this.mSystemWakeupOverloadCount.set(0);
                this.mSystemWakeupCount.set(0);
                this.mCleanupTime = currentTime;
            }
        }
        data.append(bigData.toString());
        data.append("\nendTime: ");
        data.append(String.valueOf(currentTime));
        data.append("\n[iAwareAlarmManager_End]");
        return data.toString();
    }

    public void reportSystemEvent(int type) {
        if (isBetaUser && !AwareWakeUpManager.getInstance().isScreenOn() && this.mHandler != null) {
            switch (type) {
                case 0:
                    this.mSystemWakeupCount.incrementAndGet();
                    break;
                case 1:
                    this.mSystemWakeupOverloadCount.incrementAndGet();
                    break;
            }
        }
    }

    public void reportAlarmEvent(int type, int uid, String pkg, String tag, ControlType policy) {
        if (isBetaUser && !AwareWakeUpManager.getInstance().isScreenOn() && this.mHandler != null) {
            Message msg = this.mHandler.obtainMessage();
            msg.what = 1;
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = pkg;
            args.arg2 = tag;
            args.arg3 = policy;
            args.argi1 = type;
            args.argi2 = uid;
            msg.obj = args;
            this.mHandler.sendMessage(msg);
        }
    }

    private void handleAlarmEvent(int type, int uid, String pkg, String tag, ControlType policy) {
        int userId = UserHandle.getUserId(uid);
        if (userId != 0) {
            userId = 1;
        }
        synchronized (AlarmManagerDumpRadar.class) {
            HashMap<String, PackageInfo> packageInfos = (HashMap) this.mAlarmWakeupInfo.get(Integer.valueOf(userId));
            if (packageInfos == null) {
                packageInfos = new HashMap();
            }
            PackageInfo packageInfo = (PackageInfo) packageInfos.get(pkg);
            if (packageInfo == null) {
                packageInfo = new PackageInfo(pkg);
            }
            TagInfo tagInfo = null;
            if (tag != null) {
                tagInfo = (TagInfo) packageInfo.mTagMap.get(tag);
                if (tagInfo == null) {
                    tagInfo = new TagInfo(tag);
                }
            }
            updateData(type, packageInfo, tagInfo, policy);
            if (tagInfo != null) {
                packageInfo.mTagMap.put(tag, tagInfo);
            }
            packageInfos.put(pkg, packageInfo);
            this.mAlarmWakeupInfo.put(Integer.valueOf(userId), packageInfos);
        }
    }

    private void updateData(int type, PackageInfo packageInfo, TagInfo tagInfo, ControlType policy) {
        switch (type) {
            case 0:
                packageInfo.mWakeupCount++;
                if (tagInfo != null) {
                    tagInfo.mWakeupCount++;
                    break;
                }
                break;
            case 1:
                if (tagInfo == null) {
                    packageInfo.mOverloadCount++;
                    break;
                } else {
                    tagInfo.mOverloadCount++;
                    break;
                }
            case 2:
                if (tagInfo != null) {
                    switch (policy) {
                        case PERCEPTIBLE:
                            tagInfo.mPerceptibleCount++;
                            break;
                        case UNKNOWN:
                            tagInfo.mUnknownCount++;
                            break;
                        case EXTEND:
                            tagInfo.mExtendCount++;
                            break;
                        case EXTEND_TOPN:
                            tagInfo.mTopNCount++;
                            break;
                        case EXTEND_AND_MUTE:
                            tagInfo.mMuteCount++;
                            break;
                        case DECIDE_OVERLOAD:
                            tagInfo.mDecideOverloadCount++;
                            break;
                        case IMPORTANT:
                            tagInfo.mWhiteListCount++;
                            break;
                    }
                }
                return;
                break;
        }
    }
}
