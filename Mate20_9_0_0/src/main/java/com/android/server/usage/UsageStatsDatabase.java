package com.android.server.usage;

import android.app.usage.TimeSparseArray;
import android.app.usage.UsageStats;
import android.os.Build.VERSION;
import android.os.SystemProperties;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.server.job.controllers.JobStatus;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

class UsageStatsDatabase {
    static final int BACKUP_VERSION = 1;
    private static final String BAK_SUFFIX = ".bak";
    private static final String CHECKED_IN_SUFFIX = "-c";
    private static final int CURRENT_VERSION = 3;
    private static final boolean DEBUG = false;
    static final String KEY_USAGE_STATS = "usage_stats";
    private static final String RETENTION_LEN_KEY = "ro.usagestats.chooser.retention";
    private static final int SELECTION_LOG_RETENTION_LEN = SystemProperties.getInt(RETENTION_LEN_KEY, 14);
    private static final String TAG = "UsageStatsDatabase";
    private final UnixCalendar mCal;
    private boolean mFirstUpdate;
    private final File[] mIntervalDirs;
    private final Object mLock = new Object();
    private boolean mNewUpdate;
    private final TimeSparseArray<AtomicFile>[] mSortedStatFiles;
    private final File mVersionFile;

    public interface CheckinAction {
        boolean checkin(IntervalStats intervalStats);
    }

    interface StatCombiner<T> {
        void combine(IntervalStats intervalStats, boolean z, List<T> list);
    }

    public UsageStatsDatabase(File dir) {
        this.mIntervalDirs = new File[]{new File(dir, "daily"), new File(dir, "weekly"), new File(dir, "monthly"), new File(dir, "yearly")};
        this.mVersionFile = new File(dir, "version");
        this.mSortedStatFiles = new TimeSparseArray[this.mIntervalDirs.length];
        this.mCal = new UnixCalendar(0);
    }

    public void init(long currentTimeMillis) {
        synchronized (this.mLock) {
            File[] fileArr = this.mIntervalDirs;
            int length = fileArr.length;
            int i = 0;
            int i2 = 0;
            while (i2 < length) {
                File f = fileArr[i2];
                f.mkdirs();
                if (f.exists()) {
                    i2++;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to create directory ");
                    stringBuilder.append(f.getAbsolutePath());
                    throw new IllegalStateException(stringBuilder.toString());
                }
            }
            checkVersionAndBuildLocked();
            indexFilesLocked();
            TimeSparseArray[] timeSparseArrayArr = this.mSortedStatFiles;
            length = timeSparseArrayArr.length;
            while (i < length) {
                TimeSparseArray<AtomicFile> files = timeSparseArrayArr[i];
                int startIndex = files.closestIndexOnOrAfter(currentTimeMillis);
                if (startIndex >= 0) {
                    int i3;
                    int fileCount = files.size();
                    for (i3 = startIndex; i3 < fileCount; i3++) {
                        ((AtomicFile) files.valueAt(i3)).delete();
                        String str = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(((AtomicFile) files.valueAt(i3)).getBaseFile().getName());
                        stringBuilder2.append(" is deleted because it is on or after current time ");
                        stringBuilder2.append(currentTimeMillis);
                        Slog.d(str, stringBuilder2.toString());
                    }
                    for (i3 = startIndex; i3 < fileCount; i3++) {
                        files.removeAt(i3);
                    }
                }
                i++;
            }
        }
    }

    public boolean checkinDailyFiles(CheckinAction checkinAction) {
        synchronized (this.mLock) {
            int i;
            TimeSparseArray<AtomicFile> files = this.mSortedStatFiles[0];
            int fileCount = files.size();
            int lastCheckin = -1;
            for (i = 0; i < fileCount - 1; i++) {
                if (((AtomicFile) files.valueAt(i)).getBaseFile().getPath().endsWith(CHECKED_IN_SUFFIX)) {
                    lastCheckin = i;
                }
            }
            i = lastCheckin + 1;
            if (i == fileCount - 1) {
                return true;
            }
            try {
                IntervalStats stats = new IntervalStats();
                int i2 = i;
                while (i2 < fileCount - 1) {
                    UsageStatsXml.read((AtomicFile) files.valueAt(i2), stats);
                    if (checkinAction.checkin(stats)) {
                        i2++;
                    } else {
                        return false;
                    }
                }
                int i3 = i;
                while (i3 < fileCount - 1) {
                    AtomicFile file = (AtomicFile) files.valueAt(i3);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(file.getBaseFile().getPath());
                    stringBuilder.append(CHECKED_IN_SUFFIX);
                    File checkedInFile = new File(stringBuilder.toString());
                    if (file.getBaseFile().renameTo(checkedInFile)) {
                        files.setValueAt(i3, new AtomicFile(checkedInFile));
                        i3++;
                    } else {
                        String str = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Failed to mark file ");
                        stringBuilder2.append(file.getBaseFile().getPath());
                        stringBuilder2.append(" as checked-in");
                        Slog.e(str, stringBuilder2.toString());
                        return true;
                    }
                }
                return true;
            } catch (IOException e) {
                Slog.e(TAG, "Failed to check-in", e);
                return false;
            }
        }
    }

    private void indexFilesLocked() {
        FilenameFilter backupFileFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(UsageStatsDatabase.BAK_SUFFIX) ^ 1;
            }
        };
        for (int i = 0; i < this.mSortedStatFiles.length; i++) {
            if (this.mSortedStatFiles[i] == null) {
                this.mSortedStatFiles[i] = new TimeSparseArray();
            } else {
                this.mSortedStatFiles[i].clear();
            }
            File[] files = this.mIntervalDirs[i].listFiles(backupFileFilter);
            if (files != null) {
                for (File f : files) {
                    AtomicFile af = new AtomicFile(f);
                    try {
                        this.mSortedStatFiles[i].put(UsageStatsXml.parseBeginTime(af), af);
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("indexFilesLocked: Put sortedStatFiles ");
                        stringBuilder.append(f.getName());
                        stringBuilder.append(" for interval ");
                        stringBuilder.append(i);
                        Slog.d(str, stringBuilder.toString());
                    } catch (IOException e) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("failed to index file: ");
                        stringBuilder2.append(f);
                        Slog.e(str2, stringBuilder2.toString(), e);
                    }
                }
            } else {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("IntervalDir is empty and sortedStatFiles is cleared for interval ");
                stringBuilder3.append(i);
                Slog.d(str3, stringBuilder3.toString());
            }
        }
    }

    boolean isFirstUpdate() {
        return this.mFirstUpdate;
    }

    boolean isNewUpdate() {
        return this.mNewUpdate;
    }

    /* JADX WARNING: Removed duplicated region for block: B:23:0x0042 A:{Splitter: B:1:0x000b, ExcHandler: java.lang.NumberFormatException (e java.lang.NumberFormatException)} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void checkVersionAndBuildLocked() {
        Throwable th;
        Throwable th2;
        String currentFingerprint = getBuildFingerprint();
        this.mFirstUpdate = true;
        this.mNewUpdate = true;
        int version = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(this.mVersionFile));
            try {
                int version2 = Integer.parseInt(reader.readLine());
                String buildFingerprint = reader.readLine();
                if (buildFingerprint != null) {
                    this.mFirstUpdate = false;
                }
                if (currentFingerprint.equals(buildFingerprint)) {
                    this.mNewUpdate = false;
                }
                $closeResource(null, reader);
                version = version2;
                if (version != 3) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Upgrading from version ");
                    stringBuilder.append(version);
                    stringBuilder.append(" to ");
                    stringBuilder.append(3);
                    Slog.i(str, stringBuilder.toString());
                    doUpgradeLocked(version);
                }
                if (version != 3 || this.mNewUpdate) {
                    BufferedWriter writer;
                    try {
                        writer = new BufferedWriter(new FileWriter(this.mVersionFile));
                        writer.write(Integer.toString(3));
                        writer.write("\n");
                        writer.write(currentFingerprint);
                        writer.write("\n");
                        writer.flush();
                        $closeResource(null, writer);
                        return;
                    } catch (IOException e) {
                        Slog.e(TAG, "Failed to write new version");
                        throw new RuntimeException(e);
                    } catch (Throwable th3) {
                        $closeResource(r1, writer);
                    }
                }
                return;
            } catch (Throwable th22) {
                Throwable th4 = th22;
                th22 = th;
                th = th4;
            }
            $closeResource(th22, reader);
            throw th;
        } catch (NumberFormatException e2) {
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    private String getBuildFingerprint() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(VERSION.RELEASE);
        stringBuilder.append(";");
        stringBuilder.append(VERSION.CODENAME);
        stringBuilder.append(";");
        stringBuilder.append(VERSION.INCREMENTAL);
        return stringBuilder.toString();
    }

    private void doUpgradeLocked(int thisVersion) {
        if (thisVersion < 2) {
            Slog.i(TAG, "Deleting all usage stats files");
            for (File[] files : this.mIntervalDirs) {
                File[] files2 = files2.listFiles();
                if (files2 != null) {
                    for (File f : files2) {
                        f.delete();
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:21:0x0097, code:
            r18 = r10;
            r11.clear();
            r6 = r6 + 1;
            r10 = r13;
            r0 = r18;
            r2 = r21;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onTimeChanged(long timeDiffMillis) {
        long j = timeDiffMillis;
        synchronized (this.mLock) {
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("Time changed by ");
            TimeUtils.formatDuration(j, logBuilder);
            logBuilder.append(".");
            int filesDeleted = 0;
            TimeSparseArray[] timeSparseArrayArr = this.mSortedStatFiles;
            int length = timeSparseArrayArr.length;
            int filesMoved = 0;
            int filesMoved2 = 0;
            while (filesMoved2 < length) {
                TimeSparseArray<AtomicFile> files = timeSparseArrayArr[filesMoved2];
                int fileCount = files.size();
                int filesMoved3 = filesMoved;
                filesMoved = filesDeleted;
                filesDeleted = 0;
                while (true) {
                    int i = filesDeleted;
                    if (i >= fileCount) {
                        break;
                    }
                    AtomicFile file = (AtomicFile) files.valueAt(i);
                    int filesDeleted2 = filesMoved;
                    long newTime = files.keyAt(i) + j;
                    if (newTime < 0) {
                        filesDeleted = filesDeleted2 + 1;
                        file.delete();
                        filesMoved = filesDeleted;
                    } else {
                        try {
                            file.openRead().close();
                        } catch (IOException e) {
                        }
                        String newName = Long.toString(newTime);
                        if (file.getBaseFile().getName().endsWith(CHECKED_IN_SUFFIX)) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(newName);
                            stringBuilder.append(CHECKED_IN_SUFFIX);
                            newName = stringBuilder.toString();
                        }
                        filesMoved3++;
                        file.getBaseFile().renameTo(new File(file.getBaseFile().getParentFile(), newName));
                        filesMoved = filesDeleted2;
                    }
                    filesDeleted = i + 1;
                    j = timeDiffMillis;
                }
            }
            logBuilder.append(" files deleted: ");
            logBuilder.append(filesDeleted);
            logBuilder.append(" files moved: ");
            logBuilder.append(filesMoved);
            Slog.i(TAG, logBuilder.toString());
            indexFilesLocked();
        }
    }

    public IntervalStats getLatestUsageStats(int intervalType) {
        synchronized (this.mLock) {
            if (intervalType >= 0) {
                if (intervalType < this.mIntervalDirs.length) {
                    int fileCount = this.mSortedStatFiles[intervalType].size();
                    if (fileCount == 0) {
                        return null;
                    }
                    try {
                        AtomicFile f = (AtomicFile) this.mSortedStatFiles[intervalType].valueAt(fileCount - 1);
                        IntervalStats stats = new IntervalStats();
                        UsageStatsXml.read(f, stats);
                        return stats;
                    } catch (IOException e) {
                        Slog.e(TAG, "Failed to read usage stats file", e);
                        return null;
                    }
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Bad interval type ");
            stringBuilder.append(intervalType);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public long getLatestUsageStatsBeginTime(int intervalType) {
        synchronized (this.mLock) {
            if (intervalType >= 0) {
                if (intervalType < this.mIntervalDirs.length) {
                    int statsFileCount = this.mSortedStatFiles[intervalType].size();
                    if (statsFileCount > 0) {
                        long keyAt = this.mSortedStatFiles[intervalType].keyAt(statsFileCount - 1);
                        return keyAt;
                    }
                    return -1;
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Bad interval type ");
            stringBuilder.append(intervalType);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public <T> List<T> queryUsageStats(int intervalType, long beginTime, long endTime, StatCombiner<T> combiner) {
        IOException e;
        int i = intervalType;
        long j = beginTime;
        long j2 = endTime;
        synchronized (this.mLock) {
            if (i >= 0) {
                if (i < this.mIntervalDirs.length) {
                    TimeSparseArray<AtomicFile> intervalStats = this.mSortedStatFiles[i];
                    if (j2 <= j) {
                        return null;
                    }
                    int startIndex = intervalStats.closestIndexOnOrBefore(j);
                    if (startIndex < 0) {
                        startIndex = 0;
                    }
                    int startIndex2 = startIndex;
                    startIndex = intervalStats.closestIndexOnOrBefore(j2);
                    if (startIndex < 0) {
                        return null;
                    }
                    if (intervalStats.keyAt(startIndex) == j2) {
                        startIndex--;
                        if (startIndex < 0) {
                            return null;
                        }
                    }
                    int endIndex = startIndex;
                    IntervalStats stats = new IntervalStats();
                    ArrayList<T> results = new ArrayList();
                    startIndex = startIndex2;
                    while (true) {
                        int i2 = startIndex;
                        if (i2 <= endIndex) {
                            StatCombiner<T> statCombiner;
                            try {
                                UsageStatsXml.read((AtomicFile) intervalStats.valueAt(i2), stats);
                                if (j < stats.endTime) {
                                    try {
                                        combiner.combine(stats, false, results);
                                    } catch (IOException e2) {
                                        e = e2;
                                    }
                                } else {
                                    statCombiner = combiner;
                                }
                            } catch (IOException e3) {
                                e = e3;
                                statCombiner = combiner;
                                Slog.e(TAG, "Failed to read usage stats file", e);
                                startIndex = i2 + 1;
                                j = beginTime;
                            }
                            startIndex = i2 + 1;
                            j = beginTime;
                        } else {
                            return results;
                        }
                    }
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Bad interval type ");
            stringBuilder.append(i);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public int findBestFitBucket(long beginTimeStamp, long endTimeStamp) {
        int bestBucket;
        synchronized (this.mLock) {
            bestBucket = -1;
            long smallestDiff = JobStatus.NO_LATEST_RUNTIME;
            for (int i = this.mSortedStatFiles.length - 1; i >= 0; i--) {
                int index = this.mSortedStatFiles[i].closestIndexOnOrBefore(beginTimeStamp);
                int size = this.mSortedStatFiles[i].size();
                if (index >= 0 && index < size) {
                    long diff = Math.abs(this.mSortedStatFiles[i].keyAt(index) - beginTimeStamp);
                    if (diff < smallestDiff) {
                        smallestDiff = diff;
                        bestBucket = i;
                    }
                }
            }
        }
        return bestBucket;
    }

    public void prune(long currentTimeMillis) {
        synchronized (this.mLock) {
            this.mCal.setTimeInMillis(currentTimeMillis);
            this.mCal.addYears(-3);
            pruneFilesOlderThan(this.mIntervalDirs[3], this.mCal.getTimeInMillis());
            this.mCal.setTimeInMillis(currentTimeMillis);
            this.mCal.addMonths(-6);
            pruneFilesOlderThan(this.mIntervalDirs[2], this.mCal.getTimeInMillis());
            this.mCal.setTimeInMillis(currentTimeMillis);
            this.mCal.addWeeks(-4);
            pruneFilesOlderThan(this.mIntervalDirs[1], this.mCal.getTimeInMillis());
            this.mCal.setTimeInMillis(currentTimeMillis);
            this.mCal.addDays(-10);
            int i = 0;
            pruneFilesOlderThan(this.mIntervalDirs[0], this.mCal.getTimeInMillis());
            this.mCal.setTimeInMillis(currentTimeMillis);
            this.mCal.addDays(-SELECTION_LOG_RETENTION_LEN);
            while (true) {
                int i2 = i;
                if (i2 < this.mIntervalDirs.length) {
                    pruneChooserCountsOlderThan(this.mIntervalDirs[i2], this.mCal.getTimeInMillis());
                    i = i2 + 1;
                } else {
                    indexFilesLocked();
                }
            }
        }
    }

    private static void pruneFilesOlderThan(File dir, long expiryTime) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                File f2;
                long beginTime;
                String path = f2.getPath();
                if (path.endsWith(BAK_SUFFIX)) {
                    f2 = new File(path.substring(0, path.length() - BAK_SUFFIX.length()));
                }
                try {
                    beginTime = UsageStatsXml.parseBeginTime(f2);
                } catch (IOException e) {
                    beginTime = 0;
                }
                if (beginTime < expiryTime) {
                    new AtomicFile(f2).delete();
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("prune files: ");
                    stringBuilder.append(f2.getName());
                    stringBuilder.append(" is deleted because beginTime(");
                    stringBuilder.append(beginTime);
                    stringBuilder.append(") < expiryTime(");
                    stringBuilder.append(expiryTime);
                    stringBuilder.append(")");
                    Slog.d(str, stringBuilder.toString());
                }
            }
        }
    }

    private static void pruneChooserCountsOlderThan(File dir, long expiryTime) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                File f2;
                long beginTime;
                String path = f.getPath();
                if (path.endsWith(BAK_SUFFIX)) {
                    f2 = new File(path.substring(0, path.length() - BAK_SUFFIX.length()));
                } else {
                    f2 = f;
                }
                try {
                    beginTime = UsageStatsXml.parseBeginTime(f2);
                } catch (IOException e) {
                    IOException iOException = e;
                    beginTime = 0;
                }
                if (beginTime < expiryTime) {
                    try {
                        AtomicFile af = new AtomicFile(f2);
                        IntervalStats stats = new IntervalStats();
                        UsageStatsXml.read(af, stats);
                        int pkgCount = stats.packageStats.size();
                        for (int i = 0; i < pkgCount; i++) {
                            UsageStats pkgStats = (UsageStats) stats.packageStats.valueAt(i);
                            if (!(pkgStats == null || pkgStats.mChooserCounts == null)) {
                                pkgStats.mChooserCounts.clear();
                            }
                        }
                        UsageStatsXml.write(af, stats);
                    } catch (IOException e2) {
                        Slog.e(TAG, "Failed to delete chooser counts from usage stats file", e2);
                    }
                }
            }
        }
    }

    public void putUsageStats(int intervalType, IntervalStats stats) throws IOException {
        if (stats != null) {
            synchronized (this.mLock) {
                if (intervalType >= 0) {
                    if (intervalType < this.mIntervalDirs.length) {
                        AtomicFile f = (AtomicFile) this.mSortedStatFiles[intervalType].get(stats.beginTime);
                        if (f == null) {
                            f = new AtomicFile(new File(this.mIntervalDirs[intervalType], Long.toString(stats.beginTime)));
                            this.mSortedStatFiles[intervalType].put(stats.beginTime, f);
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("putUsageStats: put SortedStatFiles ");
                            stringBuilder.append(f.getBaseFile().getName());
                            stringBuilder.append(" for interval ");
                            stringBuilder.append(intervalType);
                            Slog.d(str, stringBuilder.toString());
                        }
                        UsageStatsXml.write(f, stats);
                        stats.lastTimeSaved = f.getLastModifiedTime();
                    }
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Bad interval type ");
                stringBuilder2.append(intervalType);
                throw new IllegalArgumentException(stringBuilder2.toString());
            }
        }
    }

    byte[] getBackupPayload(String key) {
        byte[] toByteArray;
        synchronized (this.mLock) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (KEY_USAGE_STATS.equals(key)) {
                prune(System.currentTimeMillis());
                DataOutputStream out = new DataOutputStream(baos);
                try {
                    int i;
                    int i2;
                    out.writeInt(1);
                    int i3 = 0;
                    out.writeInt(this.mSortedStatFiles[0].size());
                    for (i = 0; i < this.mSortedStatFiles[0].size(); i++) {
                        writeIntervalStatsToStream(out, (AtomicFile) this.mSortedStatFiles[0].valueAt(i));
                    }
                    out.writeInt(this.mSortedStatFiles[1].size());
                    for (i = 0; i < this.mSortedStatFiles[1].size(); i++) {
                        writeIntervalStatsToStream(out, (AtomicFile) this.mSortedStatFiles[1].valueAt(i));
                    }
                    out.writeInt(this.mSortedStatFiles[2].size());
                    for (i2 = 0; i2 < this.mSortedStatFiles[2].size(); i2++) {
                        writeIntervalStatsToStream(out, (AtomicFile) this.mSortedStatFiles[2].valueAt(i2));
                    }
                    out.writeInt(this.mSortedStatFiles[3].size());
                    while (true) {
                        i2 = i3;
                        if (i2 >= this.mSortedStatFiles[3].size()) {
                            break;
                        }
                        writeIntervalStatsToStream(out, (AtomicFile) this.mSortedStatFiles[3].valueAt(i2));
                        i3 = i2 + 1;
                    }
                } catch (IOException ioe) {
                    Slog.d(TAG, "Failed to write data to output stream", ioe);
                    baos.reset();
                }
            }
            toByteArray = baos.toByteArray();
        }
        return toByteArray;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: RegionMakerVisitor
        jadx.core.utils.exceptions.JadxRuntimeException: Exception block dominator not found, method:com.android.server.usage.UsageStatsDatabase.applyRestoredPayload(java.lang.String, byte[]):void, dom blocks: [B:33:0x00bb, B:43:0x00cd]
        	at jadx.core.dex.visitors.regions.ProcessTryCatchRegions.searchTryCatchDominators(ProcessTryCatchRegions.java:89)
        	at jadx.core.dex.visitors.regions.ProcessTryCatchRegions.process(ProcessTryCatchRegions.java:45)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.postProcessRegions(RegionMakerVisitor.java:63)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:58)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    void applyRestoredPayload(java.lang.String r18, byte[] r19) {
        /*
        r17 = this;
        r1 = r17;
        r2 = r1.mLock;
        monitor-enter(r2);
        r0 = "usage_stats";	 Catch:{ all -> 0x00e3 }
        r3 = r18;
        r0 = r0.equals(r3);	 Catch:{ all -> 0x00e1 }
        if (r0 == 0) goto L_0x00dd;	 Catch:{ all -> 0x00e1 }
        r0 = 0;	 Catch:{ all -> 0x00e1 }
        r4 = r1.getLatestUsageStats(r0);	 Catch:{ all -> 0x00e1 }
        r5 = 1;	 Catch:{ all -> 0x00e1 }
        r6 = r1.getLatestUsageStats(r5);	 Catch:{ all -> 0x00e1 }
        r7 = 2;	 Catch:{ all -> 0x00e1 }
        r8 = r1.getLatestUsageStats(r7);	 Catch:{ all -> 0x00e1 }
        r9 = 3;	 Catch:{ all -> 0x00e1 }
        r10 = r1.getLatestUsageStats(r9);	 Catch:{ all -> 0x00e1 }
        r11 = new java.io.DataInputStream;	 Catch:{ IOException -> 0x00ca, all -> 0x00c6 }
        r12 = new java.io.ByteArrayInputStream;	 Catch:{ IOException -> 0x00ca, all -> 0x00c6 }
        r13 = r19;
        r12.<init>(r13);	 Catch:{ IOException -> 0x00c4 }
        r11.<init>(r12);	 Catch:{ IOException -> 0x00c4 }
        r12 = r11.readInt();	 Catch:{ IOException -> 0x00c4 }
        if (r12 < r5) goto L_0x00bf;	 Catch:{ IOException -> 0x00c4 }
    L_0x003a:
        if (r12 <= r5) goto L_0x003e;	 Catch:{ IOException -> 0x00c4 }
    L_0x003c:
        goto L_0x00bf;	 Catch:{ IOException -> 0x00c4 }
    L_0x003e:
        r14 = r0;	 Catch:{ IOException -> 0x00c4 }
    L_0x003f:
        r15 = r1.mIntervalDirs;	 Catch:{ IOException -> 0x00c4 }
        r15 = r15.length;	 Catch:{ IOException -> 0x00c4 }
        if (r14 >= r15) goto L_0x004e;	 Catch:{ IOException -> 0x00c4 }
    L_0x0044:
        r15 = r1.mIntervalDirs;	 Catch:{ IOException -> 0x00c4 }
        r15 = r15[r14];	 Catch:{ IOException -> 0x00c4 }
        deleteDirectoryContents(r15);	 Catch:{ IOException -> 0x00c4 }
        r14 = r14 + 1;	 Catch:{ IOException -> 0x00c4 }
        goto L_0x003f;	 Catch:{ IOException -> 0x00c4 }
    L_0x004e:
        r14 = r11.readInt();	 Catch:{ IOException -> 0x00c4 }
        r15 = r0;	 Catch:{ IOException -> 0x00c4 }
    L_0x0053:
        if (r15 >= r14) goto L_0x006a;	 Catch:{ IOException -> 0x00c4 }
    L_0x0055:
        r9 = getIntervalStatsBytes(r11);	 Catch:{ IOException -> 0x00c4 }
        r9 = deserializeIntervalStats(r9);	 Catch:{ IOException -> 0x00c4 }
        r16 = r1.mergeStats(r9, r4);	 Catch:{ IOException -> 0x00c4 }
        r9 = r16;	 Catch:{ IOException -> 0x00c4 }
        r1.putUsageStats(r0, r9);	 Catch:{ IOException -> 0x00c4 }
        r15 = r15 + 1;	 Catch:{ IOException -> 0x00c4 }
        r9 = 3;	 Catch:{ IOException -> 0x00c4 }
        goto L_0x0053;	 Catch:{ IOException -> 0x00c4 }
    L_0x006a:
        r9 = r11.readInt();	 Catch:{ IOException -> 0x00c4 }
        r14 = r0;	 Catch:{ IOException -> 0x00c4 }
    L_0x006f:
        if (r14 >= r9) goto L_0x0085;	 Catch:{ IOException -> 0x00c4 }
    L_0x0071:
        r15 = getIntervalStatsBytes(r11);	 Catch:{ IOException -> 0x00c4 }
        r15 = deserializeIntervalStats(r15);	 Catch:{ IOException -> 0x00c4 }
        r16 = r1.mergeStats(r15, r6);	 Catch:{ IOException -> 0x00c4 }
        r15 = r16;	 Catch:{ IOException -> 0x00c4 }
        r1.putUsageStats(r5, r15);	 Catch:{ IOException -> 0x00c4 }
        r14 = r14 + 1;	 Catch:{ IOException -> 0x00c4 }
        goto L_0x006f;	 Catch:{ IOException -> 0x00c4 }
    L_0x0085:
        r5 = r11.readInt();	 Catch:{ IOException -> 0x00c4 }
        r9 = r0;	 Catch:{ IOException -> 0x00c4 }
    L_0x008a:
        if (r9 >= r5) goto L_0x009f;	 Catch:{ IOException -> 0x00c4 }
    L_0x008c:
        r14 = getIntervalStatsBytes(r11);	 Catch:{ IOException -> 0x00c4 }
        r14 = deserializeIntervalStats(r14);	 Catch:{ IOException -> 0x00c4 }
        r15 = r1.mergeStats(r14, r8);	 Catch:{ IOException -> 0x00c4 }
        r14 = r15;	 Catch:{ IOException -> 0x00c4 }
        r1.putUsageStats(r7, r14);	 Catch:{ IOException -> 0x00c4 }
        r9 = r9 + 1;	 Catch:{ IOException -> 0x00c4 }
        goto L_0x008a;	 Catch:{ IOException -> 0x00c4 }
    L_0x009f:
        r7 = r11.readInt();	 Catch:{ IOException -> 0x00c4 }
        r5 = r7;	 Catch:{ IOException -> 0x00c4 }
    L_0x00a5:
        if (r0 >= r5) goto L_0x00bb;	 Catch:{ IOException -> 0x00c4 }
    L_0x00a7:
        r7 = getIntervalStatsBytes(r11);	 Catch:{ IOException -> 0x00c4 }
        r7 = deserializeIntervalStats(r7);	 Catch:{ IOException -> 0x00c4 }
        r9 = r1.mergeStats(r7, r10);	 Catch:{ IOException -> 0x00c4 }
        r7 = r9;	 Catch:{ IOException -> 0x00c4 }
        r9 = 3;	 Catch:{ IOException -> 0x00c4 }
        r1.putUsageStats(r9, r7);	 Catch:{ IOException -> 0x00c4 }
        r0 = r0 + 1;
        goto L_0x00a5;
    L_0x00bb:
        r17.indexFilesLocked();	 Catch:{ all -> 0x00ea }
        goto L_0x00d7;	 Catch:{ all -> 0x00ea }
    L_0x00bf:
        r17.indexFilesLocked();	 Catch:{ all -> 0x00ea }
        monitor-exit(r2);	 Catch:{ all -> 0x00ea }
        return;
    L_0x00c4:
        r0 = move-exception;
        goto L_0x00cd;
    L_0x00c6:
        r0 = move-exception;
        r13 = r19;
        goto L_0x00d9;
    L_0x00ca:
        r0 = move-exception;
        r13 = r19;
    L_0x00cd:
        r5 = "UsageStatsDatabase";	 Catch:{ all -> 0x00d8 }
        r7 = "Failed to read data from input stream";	 Catch:{ all -> 0x00d8 }
        android.util.Slog.d(r5, r7, r0);	 Catch:{ all -> 0x00d8 }
        r17.indexFilesLocked();	 Catch:{ all -> 0x00ea }
    L_0x00d7:
        goto L_0x00df;	 Catch:{ all -> 0x00ea }
    L_0x00d8:
        r0 = move-exception;	 Catch:{ all -> 0x00ea }
    L_0x00d9:
        r17.indexFilesLocked();	 Catch:{ all -> 0x00ea }
        throw r0;	 Catch:{ all -> 0x00ea }
    L_0x00dd:
        r13 = r19;	 Catch:{ all -> 0x00ea }
    L_0x00df:
        monitor-exit(r2);	 Catch:{ all -> 0x00ea }
        return;	 Catch:{ all -> 0x00ea }
    L_0x00e1:
        r0 = move-exception;	 Catch:{ all -> 0x00ea }
        goto L_0x00e6;	 Catch:{ all -> 0x00ea }
    L_0x00e3:
        r0 = move-exception;	 Catch:{ all -> 0x00ea }
        r3 = r18;	 Catch:{ all -> 0x00ea }
    L_0x00e6:
        r13 = r19;	 Catch:{ all -> 0x00ea }
    L_0x00e8:
        monitor-exit(r2);	 Catch:{ all -> 0x00ea }
        throw r0;
    L_0x00ea:
        r0 = move-exception;
        goto L_0x00e8;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.usage.UsageStatsDatabase.applyRestoredPayload(java.lang.String, byte[]):void");
    }

    private IntervalStats mergeStats(IntervalStats beingRestored, IntervalStats onDevice) {
        if (onDevice == null) {
            return beingRestored;
        }
        if (beingRestored == null) {
            return null;
        }
        beingRestored.activeConfiguration = onDevice.activeConfiguration;
        beingRestored.configurations.putAll(onDevice.configurations);
        beingRestored.events = onDevice.events;
        return beingRestored;
    }

    private void writeIntervalStatsToStream(DataOutputStream out, AtomicFile statsFile) throws IOException {
        IntervalStats stats = new IntervalStats();
        try {
            UsageStatsXml.read(statsFile, stats);
            sanitizeIntervalStatsForBackup(stats);
            byte[] data = serializeIntervalStats(stats);
            out.writeInt(data.length);
            out.write(data);
        } catch (IOException e) {
            Slog.e(TAG, "Failed to read usage stats file", e);
            out.writeInt(0);
        }
    }

    private static byte[] getIntervalStatsBytes(DataInputStream in) throws IOException {
        int length = in.readInt();
        byte[] buffer = new byte[length];
        in.read(buffer, 0, length);
        return buffer;
    }

    private static void sanitizeIntervalStatsForBackup(IntervalStats stats) {
        if (stats != null) {
            stats.activeConfiguration = null;
            stats.configurations.clear();
            if (stats.events != null) {
                stats.events.clear();
            }
        }
    }

    private static byte[] serializeIntervalStats(IntervalStats stats) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream out = new DataOutputStream(baos);
        try {
            out.writeLong(stats.beginTime);
            UsageStatsXml.write(out, stats);
        } catch (IOException ioe) {
            Slog.d(TAG, "Serializing IntervalStats Failed", ioe);
            baos.reset();
        }
        return baos.toByteArray();
    }

    private static IntervalStats deserializeIntervalStats(byte[] data) {
        InputStream in = new DataInputStream(new ByteArrayInputStream(data));
        IntervalStats stats = new IntervalStats();
        try {
            stats.beginTime = in.readLong();
            UsageStatsXml.read(in, stats);
            return stats;
        } catch (IOException ioe) {
            Slog.d(TAG, "DeSerializing IntervalStats Failed", ioe);
            return null;
        }
    }

    private static void deleteDirectoryContents(File directory) {
        for (File file : directory.listFiles()) {
            deleteDirectory(file);
        }
    }

    private static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}
