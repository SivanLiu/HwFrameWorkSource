package com.android.server.mtm.iaware.brjob.scheduler;

import android.content.ActionFilterEntry;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.rms.iaware.AwareLog;
import android.text.TextUtils;
import com.android.server.am.HwBroadcastRecord;
import com.android.server.mtm.iaware.brjob.AwareJobSchedulerConstants;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AwareJobStatus {
    private static boolean DEBUG = false;
    private static final String TAG = "AwareJobStatus";
    private Map<String, String> mActionFilters = new HashMap();
    private int mForceCacheTag = 2;
    private HwBroadcastRecord mHwBr;
    private AtomicBoolean mParseError = new AtomicBoolean(false);
    private ResolveInfo mReceiver;
    private Map<String, Boolean> mSatisfied = new HashMap();
    private AtomicBoolean mShouldCache = new AtomicBoolean(true);
    private AtomicBoolean mShouldRunByError = new AtomicBoolean(false);

    private AwareJobStatus() {
    }

    public AwareJobStatus(HwBroadcastRecord hwBr) {
        IntentFilter filter;
        Iterator<ActionFilterEntry> it;
        if (hwBr != null) {
            this.mHwBr = hwBr;
            List receivers = this.mHwBr.getBrReceivers();
            if (receivers != null && receivers.size() > 0) {
                Object target = receivers.get(0);
                if (target instanceof ResolveInfo) {
                    this.mReceiver = (ResolveInfo) target;
                } else {
                    return;
                }
            }
            ResolveInfo resolveInfo = this.mReceiver;
            if (resolveInfo != null && (filter = resolveInfo.filter) != null && (it = filter.actionFilterIterator()) != null) {
                while (it.hasNext()) {
                    ActionFilterEntry actionFilter = it.next();
                    if (actionFilter.getAction() != null && actionFilter.getAction().equals(hwBr.getAction())) {
                        parseActionFilterEntry(actionFilter.getFilterName(), actionFilter.getFilterValue());
                    } else if (DEBUG) {
                        AwareLog.w(TAG, "iaware_brjob, action not match. ");
                    }
                }
            }
        }
    }

    private void parseActionFilterEntry(String filterName, String filterValue) {
        if (DEBUG) {
            AwareLog.i(TAG, "iaware_brjob parseActionFilterEntry: " + filterName + ", " + filterValue);
        }
        if (filterName == null || filterName.length() == 0) {
            AwareLog.e(TAG, "iaware_brjob scheduler state key is error!");
            this.mParseError.set(true);
            return;
        }
        String filterName2 = filterName.trim().toLowerCase(Locale.ENGLISH);
        if (AwareJobSchedulerConstants.FORCE_CACHE_ACTION_FILTER_NAME.toLowerCase(Locale.ENGLISH).equals(filterName2)) {
            try {
                this.mForceCacheTag = Integer.parseInt(filterValue);
            } catch (NumberFormatException e) {
                AwareLog.e(TAG, "iaware_brjob scheduler force cache value error!");
            }
        } else {
            boolean hasMatch = false;
            String[] conditionArray = AwareJobSchedulerConstants.getConditionArray();
            int i = 0;
            while (true) {
                if (i >= conditionArray.length) {
                    break;
                }
                String condition = conditionArray[i];
                if (condition.toLowerCase(Locale.ENGLISH).equals(filterName2)) {
                    hasMatch = true;
                    this.mActionFilters.put(condition, filterValue);
                    break;
                }
                i++;
            }
            if (!hasMatch) {
                AwareLog.e(TAG, "iaware_brjob scheduler state key is error!");
                this.mParseError.set(true);
            }
        }
    }

    public boolean hasConstraint(String key) {
        return this.mActionFilters.containsKey(key);
    }

    public boolean isParamError() {
        return TextUtils.isEmpty(getAction()) || TextUtils.isEmpty(getComponentName());
    }

    public boolean isParseError() {
        return this.mParseError.get();
    }

    public void setSatisfied(String condition, boolean satisfied) {
        if (condition != null) {
            boolean shouldCache = shouldCache(condition);
            if (DEBUG) {
                AwareLog.i(TAG, "iaware_brjob, receiver: " + getComponentName() + ", condition: " + condition + " satisfied: " + satisfied);
            }
            this.mSatisfied.put(condition, Boolean.valueOf(satisfied));
            this.mShouldCache.compareAndSet(true, shouldCache);
        }
    }

    public boolean isSatisfied(String condition) {
        if (condition == null || !this.mSatisfied.containsKey(condition)) {
            return false;
        }
        return this.mSatisfied.get(condition).booleanValue();
    }

    private boolean shouldCache(String condition) {
        if (AwareJobSchedulerConstants.getCacheConditionMap().containsKey(condition)) {
            return AwareJobSchedulerConstants.getCacheConditionMap().get(condition).booleanValue();
        }
        return false;
    }

    public String getActionFilterValue(String filterName) {
        if (filterName == null) {
            return null;
        }
        return this.mActionFilters.get(filterName);
    }

    public int getActionFilterSize() {
        return this.mActionFilters.size();
    }

    public String getReceiverPkg() {
        ResolveInfo resolveInfo = this.mReceiver;
        if (resolveInfo == null || resolveInfo.getComponentInfo() == null) {
            return null;
        }
        try {
            return this.mReceiver.getComponentInfo().packageName;
        } catch (IllegalStateException e) {
            AwareLog.e(TAG, "iaware_brjob, mReceiver.getComponentInfo() error!");
            return null;
        }
    }

    public String getComponentName() {
        ResolveInfo resolveInfo = this.mReceiver;
        if (resolveInfo == null || resolveInfo.getComponentInfo() == null) {
            return null;
        }
        try {
            return this.mReceiver.getComponentInfo().getComponentName().getClassName();
        } catch (IllegalStateException e) {
            AwareLog.e(TAG, "iaware_brjob, mReceiver.getComponentInfo() error!");
            return null;
        }
    }

    public HwBroadcastRecord getHwBroadcastRecord() {
        return this.mHwBr;
    }

    public String getAction() {
        HwBroadcastRecord hwBroadcastRecord = this.mHwBr;
        if (hwBroadcastRecord != null) {
            return hwBroadcastRecord.getAction();
        }
        return null;
    }

    public Intent getIntent() {
        HwBroadcastRecord hwBroadcastRecord = this.mHwBr;
        if (hwBroadcastRecord != null) {
            return hwBroadcastRecord.getIntent();
        }
        return null;
    }

    public boolean equalJob(AwareJobStatus job) {
        if (job == null) {
            return false;
        }
        String action = job.getAction();
        String comp = job.getComponentName();
        if (action == null || !action.equals(getAction()) || comp == null || !comp.equals(getComponentName())) {
            return false;
        }
        return true;
    }

    public boolean shouldCancelled() {
        boolean shouldCancel;
        int i = this.mForceCacheTag;
        if (i == 2) {
            shouldCancel = !this.mShouldCache.get();
        } else if (i == 1) {
            shouldCancel = false;
        } else {
            shouldCancel = true;
        }
        if (DEBUG) {
            AwareLog.i(TAG, "iaware_brjob, shouldCancelled: " + shouldCancel);
        }
        return shouldCancel;
    }

    public void setShouldRunByError() {
        this.mShouldRunByError.set(true);
    }

    public boolean isShouldRunByError() {
        return this.mShouldRunByError.get();
    }

    /* JADX WARNING: Removed duplicated region for block: B:24:0x007f A[EDGE_INSN: B:24:0x007f->B:20:0x007f ?: BREAK  , SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:8:0x0026  */
    public boolean isReady() {
        if (!this.mShouldRunByError.get()) {
            boolean ready = true;
            Iterator<Map.Entry<String, String>> it = this.mActionFilters.entrySet().iterator();
            while (true) {
                if (!it.hasNext()) {
                    String condition = it.next().getKey();
                    if (DEBUG) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("iaware_brjob isReady: ");
                        sb.append(condition);
                        sb.append(", ");
                        sb.append(this.mSatisfied.containsKey(condition) ? this.mSatisfied.get(condition) : "false(not contains)");
                        AwareLog.i(TAG, sb.toString());
                    }
                    if (!this.mSatisfied.containsKey(condition) || !this.mSatisfied.get(condition).booleanValue()) {
                        ready = false;
                    }
                    if (!it.hasNext()) {
                        break;
                    }
                }
            }
            ready = false;
            if (DEBUG) {
                AwareLog.i(TAG, "iaware_brjob, receiver: " + getComponentName() + ", action: " + getAction() + ", isReady: " + ready);
            }
            return ready;
        } else if (!DEBUG) {
            return true;
        } else {
            AwareLog.i(TAG, "iaware_brjob isReady all: ShouldRunByError");
            return true;
        }
    }

    public static AwareJobStatus createFromBroadcastRecord(HwBroadcastRecord hwBr) {
        return new AwareJobStatus(hwBr);
    }

    public void dump(PrintWriter pw) {
        if (pw != null) {
            pw.print("  AwareJOB #");
            pw.print(" action:");
            HwBroadcastRecord hwBroadcastRecord = this.mHwBr;
            if (hwBroadcastRecord != null) {
                pw.print(hwBroadcastRecord.getAction());
            }
            pw.print(" receiver:");
            pw.println(getReceiverPkg());
            pw.println("    filter:");
            for (Map.Entry<String, String> entry : this.mActionFilters.entrySet()) {
                pw.print("        name: ");
                pw.print(entry.getKey());
                pw.print("  value: ");
                pw.println(entry.getValue());
            }
        }
    }

    public String toString() {
        String jobinfo = this.mHwBr.toString();
        StringBuilder sb = new StringBuilder();
        sb.append(jobinfo);
        sb.append("[" + getComponentName() + "]");
        return sb.toString();
    }

    public static final void setDebug(boolean debug) {
        DEBUG = debug;
    }
}
