package com.android.server.usage;

import android.app.usage.AppStandbyInfo;
import android.app.usage.UsageStatsManager;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.util.Xml;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.job.JobPackageTracker;
import com.android.server.job.controllers.JobStatus;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import com.android.server.voiceinteraction.DatabaseHelper.SoundModelContract;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;

public class AppIdleHistory {
    @VisibleForTesting
    static final String APP_IDLE_FILENAME = "app_idle_stats.xml";
    private static final String ATTR_BUCKETING_REASON = "bucketReason";
    private static final String ATTR_BUCKET_ACTIVE_TIMEOUT_TIME = "activeTimeoutTime";
    private static final String ATTR_BUCKET_WORKING_SET_TIMEOUT_TIME = "workingSetTimeoutTime";
    private static final String ATTR_CURRENT_BUCKET = "appLimitBucket";
    private static final String ATTR_ELAPSED_IDLE = "elapsedIdleTime";
    private static final String ATTR_LAST_PREDICTED_TIME = "lastPredictedTime";
    private static final String ATTR_LAST_RUN_JOB_TIME = "lastJobRunTime";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_SCREEN_IDLE = "screenIdleTime";
    private static final boolean DEBUG = false;
    private static final long ONE_MINUTE = 60000;
    private static final int STANDBY_BUCKET_UNKNOWN = -1;
    private static final String TAG = "AppIdleHistory";
    private static final String TAG_PACKAGE = "package";
    private static final String TAG_PACKAGES = "packages";
    private long mElapsedDuration;
    private long mElapsedSnapshot;
    private SparseArray<ArrayMap<String, AppUsageHistory>> mIdleHistory = new SparseArray();
    private boolean mScreenOn;
    private long mScreenOnDuration;
    private long mScreenOnSnapshot;
    private final File mStorageDir;

    static class AppUsageHistory {
        long bucketActiveTimeoutTime;
        long bucketWorkingSetTimeoutTime;
        int bucketingReason;
        int currentBucket;
        int lastInformedBucket;
        long lastJobRunTime;
        int lastPredictedBucket = -1;
        long lastPredictedTime;
        long lastUsedElapsedTime;
        long lastUsedScreenTime;

        AppUsageHistory() {
        }
    }

    AppIdleHistory(File storageDir, long elapsedRealtime) {
        this.mElapsedSnapshot = elapsedRealtime;
        this.mScreenOnSnapshot = elapsedRealtime;
        this.mStorageDir = storageDir;
        readScreenOnTime();
    }

    public void updateDisplay(boolean screenOn, long elapsedRealtime) {
        if (screenOn != this.mScreenOn) {
            this.mScreenOn = screenOn;
            if (this.mScreenOn) {
                this.mScreenOnSnapshot = elapsedRealtime;
            } else {
                this.mScreenOnDuration += elapsedRealtime - this.mScreenOnSnapshot;
                this.mElapsedDuration += elapsedRealtime - this.mElapsedSnapshot;
                this.mElapsedSnapshot = elapsedRealtime;
            }
        }
    }

    public long getScreenOnTime(long elapsedRealtime) {
        long screenOnTime = this.mScreenOnDuration;
        if (this.mScreenOn) {
            return screenOnTime + (elapsedRealtime - this.mScreenOnSnapshot);
        }
        return screenOnTime;
    }

    @VisibleForTesting
    File getScreenOnTimeFile() {
        return new File(this.mStorageDir, "screen_on_time");
    }

    /* JADX WARNING: Removed duplicated region for block: B:4:0x002c A:{Splitter: B:2:0x000a, ExcHandler: java.io.IOException (e java.io.IOException)} */
    /* JADX WARNING: Missing block: B:7:?, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void readScreenOnTime() {
        File screenOnTimeFile = getScreenOnTimeFile();
        if (screenOnTimeFile.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(screenOnTimeFile));
                this.mScreenOnDuration = Long.parseLong(reader.readLine());
                this.mElapsedDuration = Long.parseLong(reader.readLine());
                reader.close();
            } catch (IOException e) {
            }
        } else {
            writeScreenOnTime();
        }
    }

    private void writeScreenOnTime() {
        AtomicFile screenOnTimeFile = new AtomicFile(getScreenOnTimeFile());
        FileOutputStream fos = null;
        try {
            fos = screenOnTimeFile.startWrite();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Long.toString(this.mScreenOnDuration));
            stringBuilder.append("\n");
            stringBuilder.append(Long.toString(this.mElapsedDuration));
            stringBuilder.append("\n");
            fos.write(stringBuilder.toString().getBytes());
            screenOnTimeFile.finishWrite(fos);
        } catch (IOException e) {
            screenOnTimeFile.failWrite(fos);
        }
    }

    public void writeAppIdleDurations() {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        this.mElapsedDuration += elapsedRealtime - this.mElapsedSnapshot;
        this.mElapsedSnapshot = elapsedRealtime;
        writeScreenOnTime();
    }

    public AppUsageHistory reportUsage(AppUsageHistory appUsageHistory, String packageName, int newBucket, int usageReason, long elapsedRealtime, long timeout) {
        if (timeout > elapsedRealtime) {
            long timeoutTime = this.mElapsedDuration + (timeout - this.mElapsedSnapshot);
            if (newBucket == 10) {
                appUsageHistory.bucketActiveTimeoutTime = Math.max(timeoutTime, appUsageHistory.bucketActiveTimeoutTime);
            } else if (newBucket == 20) {
                appUsageHistory.bucketWorkingSetTimeoutTime = Math.max(timeoutTime, appUsageHistory.bucketWorkingSetTimeoutTime);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot set a timeout on bucket=");
                stringBuilder.append(newBucket);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        if (elapsedRealtime != 0) {
            appUsageHistory.lastUsedElapsedTime = this.mElapsedDuration + (elapsedRealtime - this.mElapsedSnapshot);
            appUsageHistory.lastUsedScreenTime = getScreenOnTime(elapsedRealtime);
        }
        if (appUsageHistory.currentBucket > newBucket) {
            appUsageHistory.currentBucket = newBucket;
        }
        appUsageHistory.bucketingReason = 768 | usageReason;
        return appUsageHistory;
    }

    public AppUsageHistory reportUsage(String packageName, int userId, int newBucket, int usageReason, long nowElapsed, long timeout) {
        String str = packageName;
        return reportUsage(getPackageHistory(getUserHistory(userId), str, nowElapsed, true), str, newBucket, usageReason, nowElapsed, timeout);
    }

    private ArrayMap<String, AppUsageHistory> getUserHistory(int userId) {
        ArrayMap<String, AppUsageHistory> userHistory = (ArrayMap) this.mIdleHistory.get(userId);
        if (userHistory != null) {
            return userHistory;
        }
        userHistory = new ArrayMap();
        this.mIdleHistory.put(userId, userHistory);
        readAppIdleTimes(userId, userHistory);
        return userHistory;
    }

    private AppUsageHistory getPackageHistory(ArrayMap<String, AppUsageHistory> userHistory, String packageName, long elapsedRealtime, boolean create) {
        AppUsageHistory appUsageHistory = (AppUsageHistory) userHistory.get(packageName);
        if (appUsageHistory != null || !create) {
            return appUsageHistory;
        }
        appUsageHistory = new AppUsageHistory();
        appUsageHistory.lastUsedElapsedTime = getElapsedTime(elapsedRealtime);
        appUsageHistory.lastUsedScreenTime = getScreenOnTime(elapsedRealtime);
        appUsageHistory.lastPredictedTime = getElapsedTime(0);
        appUsageHistory.currentBucket = 50;
        appUsageHistory.bucketingReason = 256;
        appUsageHistory.lastInformedBucket = -1;
        appUsageHistory.lastJobRunTime = Long.MIN_VALUE;
        userHistory.put(packageName, appUsageHistory);
        return appUsageHistory;
    }

    public void onUserRemoved(int userId) {
        this.mIdleHistory.remove(userId);
    }

    public boolean isIdle(String packageName, int userId, long elapsedRealtime) {
        AppUsageHistory appUsageHistory = getPackageHistory(getUserHistory(userId), packageName, elapsedRealtime, true);
        boolean z = false;
        if (appUsageHistory == null) {
            return false;
        }
        if (appUsageHistory.currentBucket >= 40) {
            z = true;
        }
        return z;
    }

    public AppUsageHistory getAppUsageHistory(String packageName, int userId, long elapsedRealtime) {
        return getPackageHistory(getUserHistory(userId), packageName, elapsedRealtime, true);
    }

    public void setAppStandbyBucket(String packageName, int userId, long elapsedRealtime, int bucket, int reason) {
        setAppStandbyBucket(packageName, userId, elapsedRealtime, bucket, reason, false);
    }

    public void setAppStandbyBucket(String packageName, int userId, long elapsedRealtime, int bucket, int reason, boolean resetTimeout) {
        AppUsageHistory appUsageHistory = getPackageHistory(getUserHistory(userId), packageName, elapsedRealtime, true);
        appUsageHistory.currentBucket = bucket;
        appUsageHistory.bucketingReason = reason;
        long elapsed = getElapsedTime(elapsedRealtime);
        if ((JobPackageTracker.EVENT_STOP_REASON_MASK & reason) == 1280) {
            appUsageHistory.lastPredictedTime = elapsed;
            appUsageHistory.lastPredictedBucket = bucket;
        }
        if (resetTimeout) {
            appUsageHistory.bucketActiveTimeoutTime = elapsed;
            appUsageHistory.bucketWorkingSetTimeoutTime = elapsed;
        }
    }

    public void updateLastPrediction(AppUsageHistory app, long elapsedTimeAdjusted, int bucket) {
        app.lastPredictedTime = elapsedTimeAdjusted;
        app.lastPredictedBucket = bucket;
    }

    public void setLastJobRunTime(String packageName, int userId, long elapsedRealtime) {
        getPackageHistory(getUserHistory(userId), packageName, elapsedRealtime, true).lastJobRunTime = getElapsedTime(elapsedRealtime);
    }

    public long getTimeSinceLastJobRun(String packageName, int userId, long elapsedRealtime) {
        AppUsageHistory appUsageHistory = getPackageHistory(getUserHistory(userId), packageName, elapsedRealtime, true);
        if (appUsageHistory.lastJobRunTime == Long.MIN_VALUE) {
            return JobStatus.NO_LATEST_RUNTIME;
        }
        return getElapsedTime(elapsedRealtime) - appUsageHistory.lastJobRunTime;
    }

    public int getAppStandbyBucket(String packageName, int userId, long elapsedRealtime) {
        return getPackageHistory(getUserHistory(userId), packageName, elapsedRealtime, true).currentBucket;
    }

    public ArrayList<AppStandbyInfo> getAppStandbyBuckets(int userId, boolean appIdleEnabled) {
        ArrayMap<String, AppUsageHistory> userHistory = getUserHistory(userId);
        int size = userHistory.size();
        ArrayList<AppStandbyInfo> buckets = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            buckets.add(new AppStandbyInfo((String) userHistory.keyAt(i), appIdleEnabled ? ((AppUsageHistory) userHistory.valueAt(i)).currentBucket : 10));
        }
        return buckets;
    }

    public int getAppStandbyReason(String packageName, int userId, long elapsedRealtime) {
        AppUsageHistory appUsageHistory = getPackageHistory(getUserHistory(userId), packageName, elapsedRealtime, false);
        return appUsageHistory != null ? appUsageHistory.bucketingReason : 0;
    }

    public long getElapsedTime(long elapsedRealtime) {
        return (elapsedRealtime - this.mElapsedSnapshot) + this.mElapsedDuration;
    }

    public int setIdle(String packageName, int userId, boolean idle, long elapsedRealtime) {
        AppUsageHistory appUsageHistory = getPackageHistory(getUserHistory(userId), packageName, elapsedRealtime, true);
        if (idle) {
            appUsageHistory.currentBucket = 40;
            appUsageHistory.bucketingReason = 1024;
        } else {
            appUsageHistory.currentBucket = 10;
            appUsageHistory.bucketingReason = UsbTerminalTypes.TERMINAL_OUT_HEADMOUNTED;
        }
        return appUsageHistory.currentBucket;
    }

    public void clearUsage(String packageName, int userId) {
        getUserHistory(userId).remove(packageName);
    }

    boolean shouldInformListeners(String packageName, int userId, long elapsedRealtime, int bucket) {
        AppUsageHistory appUsageHistory = getPackageHistory(getUserHistory(userId), packageName, elapsedRealtime, true);
        if (appUsageHistory.lastInformedBucket == bucket) {
            return false;
        }
        appUsageHistory.lastInformedBucket = bucket;
        return true;
    }

    int getThresholdIndex(String packageName, int userId, long elapsedRealtime, long[] screenTimeThresholds, long[] elapsedTimeThresholds) {
        AppUsageHistory appUsageHistory = getPackageHistory(getUserHistory(userId), packageName, elapsedRealtime, false);
        if (appUsageHistory == null) {
            return screenTimeThresholds.length - 1;
        }
        long screenOnDelta = getScreenOnTime(elapsedRealtime) - appUsageHistory.lastUsedScreenTime;
        long elapsedDelta = getElapsedTime(elapsedRealtime) - appUsageHistory.lastUsedElapsedTime;
        int i = screenTimeThresholds.length - 1;
        while (i >= 0) {
            if (screenOnDelta >= screenTimeThresholds[i] && elapsedDelta >= elapsedTimeThresholds[i]) {
                return i;
            }
            i--;
        }
        return 0;
    }

    @VisibleForTesting
    File getUserFile(int userId) {
        return new File(new File(new File(this.mStorageDir, SoundModelContract.KEY_USERS), Integer.toString(userId)), APP_IDLE_FILENAME);
    }

    /* JADX WARNING: Removed duplicated region for block: B:11:0x004b A:{SYNTHETIC, Splitter: B:11:0x004b} */
    /* JADX WARNING: Removed duplicated region for block: B:8:0x0031 A:{Catch:{ IOException -> 0x0124, IOException -> 0x0124, all -> 0x0120 }} */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x0124 A:{PHI: r4 , Splitter: B:1:0x0006, ExcHandler: java.io.IOException (e java.io.IOException)} */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x00fd A:{Splitter: B:30:0x00c4, ExcHandler: java.io.IOException (e java.io.IOException)} */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x010b A:{Splitter: B:21:0x0065, ExcHandler: java.io.IOException (e java.io.IOException)} */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x00f6 A:{Splitter: B:41:0x00f0, ExcHandler: java.io.IOException (e java.io.IOException)} */
    /* JADX WARNING: Missing block: B:49:0x00fe, code:
            r3 = r19;
     */
    /* JADX WARNING: Missing block: B:50:0x0100, code:
            r4 = r15;
     */
    /* JADX WARNING: Missing block: B:55:0x010c, code:
            r3 = r19;
            r15 = r4;
     */
    /* JADX WARNING: Missing block: B:62:0x0125, code:
            r3 = r19;
     */
    /* JADX WARNING: Missing block: B:64:?, code:
            r5 = TAG;
            r6 = new java.lang.StringBuilder();
            r6.append("Unable to read app idle file for user ");
            r6.append(r2);
            android.util.Slog.e(r5, r6.toString());
     */
    /* JADX WARNING: Missing block: B:65:0x013d, code:
            libcore.io.IoUtils.closeQuietly(r4);
     */
    /* JADX WARNING: Missing block: B:67:0x0142, code:
            r0 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void readAppIdleTimes(int userId, ArrayMap<String, AppUsageHistory> userHistory) {
        Throwable th;
        int i = userId;
        String str = null;
        FileInputStream fis = null;
        ArrayMap<String, AppUsageHistory> arrayMap;
        try {
            int type;
            FileInputStream fileInputStream;
            fis = new AtomicFile(getUserFile(userId)).openRead();
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fis, StandardCharsets.UTF_8.name());
            while (true) {
                int next = parser.next();
                type = next;
                int i2 = 1;
                int i3 = 2;
                if (next == 2 || type == 1) {
                    if (type == 2) {
                        String str2 = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unable to read app idle file for user ");
                        stringBuilder.append(i);
                        Slog.e(str2, stringBuilder.toString());
                        IoUtils.closeQuietly(fis);
                        return;
                    } else if (parser.getName().equals(TAG_PACKAGES)) {
                        while (true) {
                            next = parser.next();
                            type = next;
                            if (next == i2) {
                                arrayMap = userHistory;
                                fileInputStream = fis;
                                IoUtils.closeQuietly(fis);
                                break;
                            }
                            if (type == i3) {
                                try {
                                    if (parser.getName().equals("package")) {
                                        String packageName = parser.getAttributeValue(str, "name");
                                        AppUsageHistory appUsageHistory = new AppUsageHistory();
                                        appUsageHistory.lastUsedElapsedTime = Long.parseLong(parser.getAttributeValue(str, ATTR_ELAPSED_IDLE));
                                        appUsageHistory.lastUsedScreenTime = Long.parseLong(parser.getAttributeValue(str, ATTR_SCREEN_IDLE));
                                        appUsageHistory.lastPredictedTime = getLongValue(parser, ATTR_LAST_PREDICTED_TIME, 0);
                                        String currentBucketString = parser.getAttributeValue(str, ATTR_CURRENT_BUCKET);
                                        if (currentBucketString == null) {
                                            next = 10;
                                        } else {
                                            next = Integer.parseInt(currentBucketString);
                                        }
                                        appUsageHistory.currentBucket = next;
                                        String bucketingReason = parser.getAttributeValue(str, ATTR_BUCKETING_REASON);
                                        fileInputStream = fis;
                                        try {
                                            appUsageHistory.lastJobRunTime = getLongValue(parser, ATTR_LAST_RUN_JOB_TIME, Long.MIN_VALUE);
                                            appUsageHistory.bucketActiveTimeoutTime = getLongValue(parser, ATTR_BUCKET_ACTIVE_TIMEOUT_TIME, 0);
                                            appUsageHistory.bucketWorkingSetTimeoutTime = getLongValue(parser, ATTR_BUCKET_WORKING_SET_TIMEOUT_TIME, 0);
                                            appUsageHistory.bucketingReason = 256;
                                            if (bucketingReason != null) {
                                                try {
                                                    appUsageHistory.bucketingReason = Integer.parseInt(bucketingReason, 16);
                                                } catch (NumberFormatException e) {
                                                }
                                            }
                                            appUsageHistory.lastInformedBucket = -1;
                                            try {
                                                userHistory.put(packageName, appUsageHistory);
                                            } catch (IOException e2) {
                                            } catch (Throwable th2) {
                                                th = th2;
                                            }
                                        } catch (IOException e3) {
                                        } catch (Throwable th3) {
                                            th = th3;
                                            arrayMap = userHistory;
                                        }
                                    } else {
                                        arrayMap = userHistory;
                                        fileInputStream = fis;
                                    }
                                } catch (IOException e4) {
                                } catch (Throwable th4) {
                                    th = th4;
                                    arrayMap = userHistory;
                                    fileInputStream = fis;
                                    IoUtils.closeQuietly(fis);
                                    throw th;
                                }
                            }
                            arrayMap = userHistory;
                            fileInputStream = fis;
                            fis = fileInputStream;
                            str = null;
                            i2 = 1;
                            i3 = 2;
                        }
                        return;
                    } else {
                        IoUtils.closeQuietly(fis);
                        return;
                    }
                }
            }
            if (type == 2) {
            }
            fis = fileInputStream;
            IoUtils.closeQuietly(fis);
            throw th;
        } catch (IOException e5) {
        } catch (Throwable th5) {
            th = th5;
            arrayMap = userHistory;
        }
    }

    private long getLongValue(XmlPullParser parser, String attrName, long defValue) {
        String value = parser.getAttributeValue(null, attrName);
        if (value == null) {
            return defValue;
        }
        return Long.parseLong(value);
    }

    public void writeAppIdleTimes(int userId) {
        AtomicFile appIdleFile = new AtomicFile(getUserFile(userId));
        try {
            FileOutputStream fos = appIdleFile.startWrite();
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            FastXmlSerializer xml = new FastXmlSerializer();
            xml.setOutput(bos, StandardCharsets.UTF_8.name());
            xml.startDocument(null, Boolean.valueOf(true));
            xml.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            xml.startTag(null, TAG_PACKAGES);
            ArrayMap<String, AppUsageHistory> userHistory = getUserHistory(userId);
            int N = userHistory.size();
            for (int i = 0; i < N; i++) {
                String packageName = (String) userHistory.keyAt(i);
                AppUsageHistory history = (AppUsageHistory) userHistory.valueAt(i);
                xml.startTag(null, "package");
                xml.attribute(null, "name", packageName);
                xml.attribute(null, ATTR_ELAPSED_IDLE, Long.toString(history.lastUsedElapsedTime));
                xml.attribute(null, ATTR_SCREEN_IDLE, Long.toString(history.lastUsedScreenTime));
                xml.attribute(null, ATTR_LAST_PREDICTED_TIME, Long.toString(history.lastPredictedTime));
                xml.attribute(null, ATTR_CURRENT_BUCKET, Integer.toString(history.currentBucket));
                xml.attribute(null, ATTR_BUCKETING_REASON, Integer.toHexString(history.bucketingReason));
                if (history.bucketActiveTimeoutTime > 0) {
                    xml.attribute(null, ATTR_BUCKET_ACTIVE_TIMEOUT_TIME, Long.toString(history.bucketActiveTimeoutTime));
                }
                if (history.bucketWorkingSetTimeoutTime > 0) {
                    xml.attribute(null, ATTR_BUCKET_WORKING_SET_TIMEOUT_TIME, Long.toString(history.bucketWorkingSetTimeoutTime));
                }
                if (history.lastJobRunTime != Long.MIN_VALUE) {
                    xml.attribute(null, ATTR_LAST_RUN_JOB_TIME, Long.toString(history.lastJobRunTime));
                }
                xml.endTag(null, "package");
            }
            xml.endTag(null, TAG_PACKAGES);
            xml.endDocument();
            appIdleFile.finishWrite(fos);
            N = userId;
        } catch (Exception e) {
            appIdleFile.failWrite(null);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error writing app idle file for user ");
            stringBuilder.append(userId);
            Slog.e(str, stringBuilder.toString());
        }
    }

    public void dump(IndentingPrintWriter idpw, int userId, String pkg) {
        PrintWriter printWriter = idpw;
        int i = userId;
        String str = pkg;
        printWriter.println("App Standby States:");
        idpw.increaseIndent();
        ArrayMap<String, AppUsageHistory> userHistory = (ArrayMap) this.mIdleHistory.get(i);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long totalElapsedTime = getElapsedTime(elapsedRealtime);
        long screenOnTime = getScreenOnTime(elapsedRealtime);
        if (userHistory != null) {
            int P = userHistory.size();
            int p = 0;
            while (p < P) {
                ArrayMap<String, AppUsageHistory> userHistory2;
                String packageName = (String) userHistory.keyAt(p);
                AppUsageHistory appUsageHistory = (AppUsageHistory) userHistory.valueAt(p);
                if (str == null || str.equals(packageName)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("package=");
                    stringBuilder.append(packageName);
                    printWriter.print(stringBuilder.toString());
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" u=");
                    stringBuilder2.append(i);
                    printWriter.print(stringBuilder2.toString());
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" bucket=");
                    stringBuilder2.append(appUsageHistory.currentBucket);
                    stringBuilder2.append(" reason=");
                    stringBuilder2.append(UsageStatsManager.reasonToString(appUsageHistory.bucketingReason));
                    printWriter.print(stringBuilder2.toString());
                    printWriter.print(" used=");
                    userHistory2 = userHistory;
                    TimeUtils.formatDuration(totalElapsedTime - appUsageHistory.lastUsedElapsedTime, printWriter);
                    printWriter.print(" usedScr=");
                    TimeUtils.formatDuration(screenOnTime - appUsageHistory.lastUsedScreenTime, printWriter);
                    printWriter.print(" lastPred=");
                    TimeUtils.formatDuration(totalElapsedTime - appUsageHistory.lastPredictedTime, printWriter);
                    printWriter.print(" activeLeft=");
                    TimeUtils.formatDuration(appUsageHistory.bucketActiveTimeoutTime - totalElapsedTime, printWriter);
                    printWriter.print(" wsLeft=");
                    TimeUtils.formatDuration(appUsageHistory.bucketWorkingSetTimeoutTime - totalElapsedTime, printWriter);
                    printWriter.print(" lastJob=");
                    TimeUtils.formatDuration(totalElapsedTime - appUsageHistory.lastJobRunTime, printWriter);
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" idle=");
                    stringBuilder2.append(isIdle(packageName, i, elapsedRealtime) ? "y" : "n");
                    printWriter.print(stringBuilder2.toString());
                    idpw.println();
                } else {
                    userHistory2 = userHistory;
                }
                p++;
                userHistory = userHistory2;
                str = pkg;
            }
            idpw.println();
            printWriter.print("totalElapsedTime=");
            TimeUtils.formatDuration(getElapsedTime(elapsedRealtime), printWriter);
            idpw.println();
            printWriter.print("totalScreenOnTime=");
            TimeUtils.formatDuration(getScreenOnTime(elapsedRealtime), printWriter);
            idpw.println();
            idpw.decreaseIndent();
        }
    }
}
