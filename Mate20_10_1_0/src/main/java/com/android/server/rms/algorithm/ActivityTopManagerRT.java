package com.android.server.rms.algorithm;

import android.content.Context;
import android.os.Bundle;
import java.util.ArrayList;

public class ActivityTopManagerRT {
    private static final String TAG = "ActivityTopManagerRT";
    private static final String TOP_ACTIVITY_SORTED = "topActivitySorted";
    private static ActivityTopManagerRT mActivityManager = null;
    private Context mContext = null;
    private final ArrayList<String> mSortedTopA = new ArrayList<>();

    private ActivityTopManagerRT(Context context) {
        if (context != null) {
            this.mContext = context;
        }
    }

    public static synchronized ActivityTopManagerRT getInstance(Context context) {
        ActivityTopManagerRT activityTopManagerRT;
        synchronized (ActivityTopManagerRT.class) {
            if (mActivityManager == null) {
                mActivityManager = new ActivityTopManagerRT(context);
            }
            activityTopManagerRT = mActivityManager;
        }
        return activityTopManagerRT;
    }

    public static synchronized ActivityTopManagerRT obtainExistInstance() {
        ActivityTopManagerRT activityTopManagerRT;
        synchronized (ActivityTopManagerRT.class) {
            activityTopManagerRT = mActivityManager;
        }
        return activityTopManagerRT;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0018, code lost:
        if (r2 == -1) goto L_0x001e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x001a, code lost:
        if (r2 >= r6) goto L_0x001e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x001c, code lost:
        return true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x001e, code lost:
        return false;
     */
    public boolean isTopActivity(String activityName, int n) {
        synchronized (this.mSortedTopA) {
            if (this.mSortedTopA.isEmpty()) {
                return false;
            }
            int index = this.mSortedTopA.indexOf(activityName);
        }
    }

    public ArrayList<String> getTopActivityDumpInfo() {
        ArrayList<String> topList = new ArrayList<>();
        synchronized (this.mSortedTopA) {
            topList.addAll(this.mSortedTopA);
        }
        return topList;
    }

    public void reportTopAData(Bundle bdl) {
        if (bdl != null) {
            synchronized (this.mSortedTopA) {
                this.mSortedTopA.clear();
                this.mSortedTopA.addAll(bdl.getStringArrayList(TOP_ACTIVITY_SORTED));
            }
        }
    }
}
