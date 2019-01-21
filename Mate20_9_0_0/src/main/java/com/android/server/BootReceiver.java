package com.android.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager.Stub;
import android.os.Build;
import android.os.DropBoxManager;
import android.os.Environment;
import android.os.FileObserver;
import android.os.FileUtils;
import android.os.RecoverySystem;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.provider.Downloads;
import android.text.TextUtils;
import android.util.AtomicFile;
import android.util.EventLog;
import android.util.Slog;
import android.util.StatsLog;
import android.util.Xml;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.Protocol;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class BootReceiver extends BroadcastReceiver {
    private static final String FSCK_FS_MODIFIED = "FILE SYSTEM WAS MODIFIED";
    private static final String FSCK_PASS_PATTERN = "Pass ([1-9]E?):";
    private static final String FSCK_TREE_OPTIMIZATION_PATTERN = "Inode [0-9]+ extent tree.*could be shorter";
    private static final int FS_STAT_FS_FIXED = 1024;
    private static final String FS_STAT_PATTERN = "fs_stat,[^,]*/([^/,]+),(0x[0-9a-fA-F]+)";
    private static final String LAST_HEADER_FILE = "last-header.txt";
    private static final String[] LAST_KMSG_FILES = new String[]{"/sys/fs/pstore/console-ramoops", "/proc/last_kmsg"};
    private static final String LAST_SHUTDOWN_TIME_PATTERN = "powerctl_shutdown_time_ms:([0-9]+):([0-9]+)";
    private static final String LOG_FILES_FILE = "log-files.xml";
    private static final int LOG_SIZE = (SystemProperties.getInt("ro.debuggable", 0) == 1 ? 98304 : Protocol.BASE_SYSTEM_RESERVED);
    private static final String METRIC_SHUTDOWN_TIME_START = "begin_shutdown";
    private static final String METRIC_SYSTEM_SERVER = "shutdown_system_server";
    private static final String[] MOUNT_DURATION_PROPS_POSTFIX = new String[]{"early", PhoneConstants.APN_TYPE_DEFAULT, "late"};
    private static final String OLD_UPDATER_CLASS = "com.google.android.systemupdater.SystemUpdateReceiver";
    private static final String OLD_UPDATER_PACKAGE = "com.google.android.systemupdater";
    private static final File PSTORE_DIR = new File("/mnt/pstore");
    private static final String SHUTDOWN_METRICS_FILE = "/data/system/shutdown-metrics.txt";
    private static final String SHUTDOWN_TRON_METRICS_PREFIX = "shutdown_";
    private static final String TAG = "BootReceiver";
    private static final String TAG_TOMBSTONE = "SYSTEM_TOMBSTONE";
    private static final File TOMBSTONE_DIR = new File("/data/tombstones");
    private static final int UMOUNT_STATUS_NOT_AVAILABLE = 4;
    private static final File lastHeaderFile = new File(Environment.getDataSystemDirectory(), LAST_HEADER_FILE);
    private static final AtomicFile sFile = new AtomicFile(new File(Environment.getDataSystemDirectory(), LOG_FILES_FILE), "log-files");
    private static FileObserver sPstoreObserver = null;
    private static FileObserver sTombstoneObserver = null;

    public void onReceive(final Context context, Intent intent) {
        new Thread() {
            public void run() {
                try {
                    BootReceiver.this.logBootEvents(context);
                } catch (Exception e) {
                    Slog.e(BootReceiver.TAG, "Can't log boot events", e);
                }
                Exception e2 = null;
                try {
                    e2 = Stub.asInterface(ServiceManager.getService("package")).isOnlyCoreApps();
                } catch (RemoteException e3) {
                }
                if (e2 == null) {
                    try {
                        BootReceiver.this.removeOldUpdatePackages(context);
                    } catch (Exception e22) {
                        Slog.e(BootReceiver.TAG, "Can't remove old update packages", e22);
                    }
                }
            }
        }.start();
    }

    private void removeOldUpdatePackages(Context context) {
        Downloads.removeAllDownloadsByPackage(context, OLD_UPDATER_PACKAGE, OLD_UPDATER_CLASS);
    }

    private String getPreviousBootHeaders() {
        try {
            return FileUtils.readTextFile(lastHeaderFile, 0, null);
        } catch (IOException e) {
            return null;
        }
    }

    private String getCurrentBootHeaders() throws IOException {
        StringBuilder stringBuilder = new StringBuilder(512);
        stringBuilder.append("Build: ");
        stringBuilder.append(Build.FINGERPRINT);
        stringBuilder.append("\n");
        stringBuilder.append("Hardware: ");
        stringBuilder.append(Build.BOARD);
        stringBuilder.append("\n");
        stringBuilder.append("Revision: ");
        stringBuilder.append(SystemProperties.get("ro.revision", ""));
        stringBuilder.append("\n");
        stringBuilder.append("Bootloader: ");
        stringBuilder.append(Build.BOOTLOADER);
        stringBuilder.append("\n");
        stringBuilder.append("Radio: ");
        stringBuilder.append(Build.getRadioVersion());
        stringBuilder.append("\n");
        stringBuilder.append("Kernel: ");
        stringBuilder.append(FileUtils.readTextFile(new File("/proc/version"), 1024, "...\n"));
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }

    private String getBootHeadersToLogAndUpdate() throws IOException {
        String oldHeaders = getPreviousBootHeaders();
        String newHeaders = getCurrentBootHeaders();
        try {
            FileUtils.stringToFile(lastHeaderFile, newHeaders);
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error writing ");
            stringBuilder.append(lastHeaderFile);
            Slog.e(str, stringBuilder.toString(), e);
        }
        StringBuilder stringBuilder2;
        if (oldHeaders == null) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("isPrevious: false\n");
            stringBuilder2.append(newHeaders);
            return stringBuilder2.toString();
        }
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("isPrevious: true\n");
        stringBuilder2.append(oldHeaders);
        return stringBuilder2.toString();
    }

    private void logBootEvents(Context ctx) throws IOException {
        StringBuilder stringBuilder;
        DropBoxManager db = (DropBoxManager) ctx.getSystemService("dropbox");
        String headers = getBootHeadersToLogAndUpdate();
        String bootReason = SystemProperties.get("ro.boot.bootreason", null);
        String recovery = RecoverySystem.handleAftermath(ctx);
        if (!(recovery == null || db == null)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(headers);
            stringBuilder.append(recovery);
            db.addText("SYSTEM_RECOVERY_LOG", stringBuilder.toString());
        }
        String lastKmsgFooter = "";
        if (bootReason != null) {
            stringBuilder = new StringBuilder(512);
            stringBuilder.append("\n");
            stringBuilder.append("Boot info:\n");
            stringBuilder.append("Last boot reason: ");
            stringBuilder.append(bootReason);
            stringBuilder.append("\n");
            lastKmsgFooter = stringBuilder.toString();
        }
        String lastKmsgFooter2 = lastKmsgFooter;
        HashMap<String, Long> timestamps = readTimestamps();
        if (SystemProperties.getLong("ro.runtime.firstboot", 0) == 0) {
            if (!StorageManager.inCryptKeeperBounce()) {
                SystemProperties.set("ro.runtime.firstboot", Long.toString(System.currentTimeMillis()));
            }
            if (db != null) {
                db.addText("SYSTEM_BOOT", headers);
            }
            HashMap<String, Long> hashMap = timestamps;
            String str = headers;
            String str2 = lastKmsgFooter2;
            addFileWithFootersToDropBox(db, hashMap, str, str2, "/proc/last_kmsg", -LOG_SIZE, "SYSTEM_LAST_KMSG");
            addFileWithFootersToDropBox(db, hashMap, str, str2, "/sys/fs/pstore/console-ramoops", -LOG_SIZE, "SYSTEM_LAST_KMSG");
            addFileWithFootersToDropBox(db, hashMap, str, str2, "/sys/fs/pstore/console-ramoops-0", -LOG_SIZE, "SYSTEM_LAST_KMSG");
            addFileToDropBox(db, hashMap, str, "/cache/recovery/log", -LOG_SIZE, "SYSTEM_RECOVERY_LOG");
            addFileToDropBox(db, hashMap, str, "/cache/recovery/last_kmsg", -LOG_SIZE, "SYSTEM_RECOVERY_KMSG");
            addAuditErrorsToDropBox(db, timestamps, headers, -LOG_SIZE, "SYSTEM_AUDIT");
        } else if (db != null) {
            db.addText("SYSTEM_RESTART", headers);
        }
        logFsShutdownTime();
        logFsMountTime();
        addFsckErrorsToDropBoxAndLogFsStat(db, timestamps, headers, -LOG_SIZE, "SYSTEM_FSCK");
        logSystemServerShutdownTimeMetrics();
        File[] tombstoneFiles = TOMBSTONE_DIR.listFiles();
        int i = 0;
        int i2 = 0;
        while (true) {
            int i3 = i2;
            if (tombstoneFiles == null || i3 >= tombstoneFiles.length) {
                writeTimestamps(timestamps);
            } else {
                if (tombstoneFiles[i3].isFile()) {
                    addFileToDropBox(db, timestamps, headers, tombstoneFiles[i3].getPath(), LOG_SIZE, TAG_TOMBSTONE);
                }
                i2 = i3 + 1;
            }
        }
        writeTimestamps(timestamps);
        if (!(TOMBSTONE_DIR.exists() || TOMBSTONE_DIR.mkdirs())) {
            Slog.e(TAG, "Can't create a empty TOMBSTONE_DIR");
        }
        final DropBoxManager dropBoxManager = db;
        final String str3 = headers;
        sTombstoneObserver = new FileObserver(TOMBSTONE_DIR.getPath(), 256) {
            public void onEvent(int event, String path) {
                if (path != null) {
                    HashMap<String, Long> timestamps = BootReceiver.readTimestamps();
                    try {
                        File file = new File(BootReceiver.TOMBSTONE_DIR, path);
                        if (file.isFile() && file.getName().startsWith("tombstone_")) {
                            BootReceiver.addFileToDropBox(dropBoxManager, timestamps, str3, file.getPath(), BootReceiver.LOG_SIZE, BootReceiver.TAG_TOMBSTONE);
                        }
                    } catch (IOException e) {
                        Slog.e(BootReceiver.TAG, "Can't log tombstone", e);
                    } catch (NullPointerException e2) {
                    }
                    BootReceiver.this.writeTimestamps(timestamps);
                }
            }
        };
        sTombstoneObserver.startWatching();
        File[] pstoreFiles = PSTORE_DIR.listFiles();
        while (pstoreFiles != null && i < pstoreFiles.length) {
            File[] pstoreFiles2 = pstoreFiles;
            addFileToDropBox(db, timestamps, headers, pstoreFiles[i].getPath(), -LOG_SIZE, "SYSTEM_PSTORE");
            i++;
            pstoreFiles = pstoreFiles2;
        }
        dropBoxManager = db;
        final HashMap<String, Long> hashMap2 = timestamps;
        final String str4 = headers;
        sPstoreObserver = new FileObserver(PSTORE_DIR.getPath(), 8) {
            public void onEvent(int event, String path) {
                try {
                    BootReceiver.addFileToDropBox(dropBoxManager, hashMap2, str4, new File(BootReceiver.PSTORE_DIR, path).getPath(), -BootReceiver.LOG_SIZE, "SYSTEM_PSTORE");
                } catch (IOException e) {
                    Slog.e(BootReceiver.TAG, "Can't log pstore", e);
                } catch (NullPointerException e2) {
                }
            }
        };
        sPstoreObserver.startWatching();
    }

    private static void addFileToDropBox(DropBoxManager db, HashMap<String, Long> timestamps, String headers, String filename, int maxSize, String tag) throws IOException {
        addFileWithFootersToDropBox(db, timestamps, headers, "", filename, maxSize, tag);
    }

    private static void addFileWithFootersToDropBox(DropBoxManager db, HashMap<String, Long> timestamps, String headers, String footers, String filename, int maxSize, String tag) throws IOException {
        if (db != null && db.isTagEnabled(tag)) {
            File file = new File(filename);
            if (!file.isDirectory()) {
                long fileTime = file.lastModified();
                if (fileTime > 0) {
                    if (!timestamps.containsKey(filename) || ((Long) timestamps.get(filename)).longValue() != fileTime) {
                        timestamps.put(filename, Long.valueOf(fileTime));
                        String fileContents = FileUtils.readTextFile(file, maxSize, "[[TRUNCATED]]\n");
                        String text = new StringBuilder();
                        text.append(headers);
                        text.append(fileContents);
                        text.append(footers);
                        text = text.toString();
                        if (tag.equals(TAG_TOMBSTONE) && fileContents.contains(">>> system_server <<<")) {
                            addTextToDropBox(db, "system_server_native_crash", text, filename, maxSize);
                        }
                        addTextToDropBox(db, tag, text, filename, maxSize);
                    }
                }
            }
        }
    }

    private static void addTextToDropBox(DropBoxManager db, String tag, String text, String filename, int maxSize) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Copying ");
        stringBuilder.append(filename);
        stringBuilder.append(" to DropBox (");
        stringBuilder.append(tag);
        stringBuilder.append(")");
        Slog.i(str, stringBuilder.toString());
        db.addText(tag, text);
        EventLog.writeEvent(DropboxLogTags.DROPBOX_FILE_COPY, new Object[]{filename, Integer.valueOf(maxSize), tag});
    }

    private static void addAuditErrorsToDropBox(DropBoxManager db, HashMap<String, Long> timestamps, String headers, int maxSize, String tag) throws IOException {
        if (db != null && db.isTagEnabled(tag)) {
            Slog.i(TAG, "Copying audit failures to DropBox");
            File file = new File("/proc/last_kmsg");
            long fileTime = file.lastModified();
            if (fileTime <= 0) {
                file = new File("/sys/fs/pstore/console-ramoops");
                fileTime = file.lastModified();
                if (fileTime <= 0) {
                    file = new File("/sys/fs/pstore/console-ramoops-0");
                    fileTime = file.lastModified();
                }
            }
            if (fileTime > 0) {
                if (!timestamps.containsKey(tag) || ((Long) timestamps.get(tag)).longValue() != fileTime) {
                    timestamps.put(tag, Long.valueOf(fileTime));
                    String log = FileUtils.readTextFile(file, maxSize, "[[TRUNCATED]]\n");
                    StringBuilder sb = new StringBuilder();
                    for (String line : log.split("\n")) {
                        if (line.contains("audit")) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(line);
                            stringBuilder.append("\n");
                            sb.append(stringBuilder.toString());
                        }
                    }
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Copied ");
                    stringBuilder2.append(sb.toString().length());
                    stringBuilder2.append(" worth of audits to DropBox");
                    Slog.i(str, stringBuilder2.toString());
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(headers);
                    stringBuilder3.append(sb.toString());
                    db.addText(tag, stringBuilder3.toString());
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:3:0x000b, code skipped:
            if (r6.isTagEnabled(r7) == false) goto L_0x0012;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void addFsckErrorsToDropBoxAndLogFsStat(DropBoxManager db, HashMap<String, Long> timestamps, String headers, int maxSize, String tag) throws IOException {
        String str;
        DropBoxManager dropBoxManager = db;
        boolean uploadEnabled = true;
        if (dropBoxManager != null) {
            str = tag;
        } else {
            str = tag;
            uploadEnabled = false;
        }
        boolean uploadEnabled2 = uploadEnabled;
        Slog.i(TAG, "Checking for fsck errors");
        File file = new File("/dev/fscklogs/log");
        if (file.lastModified() > 0) {
            int lastFsStatLineNumber;
            int i = maxSize;
            String log = FileUtils.readTextFile(file, i, "[[TRUNCATED]]\n");
            Pattern pattern = Pattern.compile(FS_STAT_PATTERN);
            String[] lines = log.split("\n");
            int lastFsStatLineNumber2 = 0;
            int length = lines.length;
            int i2 = 0;
            boolean uploadNeeded = false;
            int lineNumber = 0;
            while (i2 < length) {
                int i3;
                String line = lines[i2];
                if (line.contains(FSCK_FS_MODIFIED)) {
                    uploadNeeded = true;
                } else {
                    if (line.contains("fs_stat")) {
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            handleFsckFsStat(matcher, lines, lastFsStatLineNumber2, lineNumber);
                            lastFsStatLineNumber2 = lineNumber;
                        } else {
                            String str2 = TAG;
                            lastFsStatLineNumber = lastFsStatLineNumber2;
                            StringBuilder stringBuilder = new StringBuilder();
                            i3 = length;
                            stringBuilder.append("cannot parse fs_stat:");
                            stringBuilder.append(line);
                            Slog.w(str2, stringBuilder.toString());
                        }
                    } else {
                        lastFsStatLineNumber = lastFsStatLineNumber2;
                        i3 = length;
                    }
                    lastFsStatLineNumber2 = lastFsStatLineNumber;
                    lineNumber++;
                    i2++;
                    length = i3;
                }
                i3 = length;
                lineNumber++;
                i2++;
                length = i3;
            }
            lastFsStatLineNumber = lastFsStatLineNumber2;
            if (uploadEnabled2 && uploadNeeded) {
                addFileToDropBox(dropBoxManager, timestamps, headers, "/dev/fscklogs/log", i, str);
            } else {
                int i4 = lastFsStatLineNumber;
            }
            file.delete();
        }
    }

    private static void logFsMountTime() {
        for (String propPostfix : MOUNT_DURATION_PROPS_POSTFIX) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ro.boottime.init.mount_all.");
            stringBuilder.append(propPostfix);
            int duration = SystemProperties.getInt(stringBuilder.toString(), 0);
            if (duration != 0) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("boot_mount_all_duration_");
                stringBuilder2.append(propPostfix);
                MetricsLogger.histogram(null, stringBuilder2.toString(), duration);
            }
        }
    }

    private static void logSystemServerShutdownTimeMetrics() {
        String str;
        File metricsFile = new File(SHUTDOWN_METRICS_FILE);
        String metricsStr = null;
        if (metricsFile.exists()) {
            try {
                metricsStr = FileUtils.readTextFile(metricsFile, 0, null);
            } catch (IOException e) {
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Problem reading ");
                stringBuilder.append(metricsFile);
                Slog.e(str, stringBuilder.toString(), e);
            }
        }
        if (!TextUtils.isEmpty(metricsStr)) {
            String duration = null;
            String start_time = null;
            String reason = null;
            str = null;
            for (String keyValueStr : metricsStr.split(",")) {
                String[] keyValue = keyValueStr.split(":");
                if (keyValue.length != 2) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Wrong format of shutdown metrics - ");
                    stringBuilder2.append(metricsStr);
                    Slog.e(str2, stringBuilder2.toString());
                } else {
                    if (keyValue[0].startsWith(SHUTDOWN_TRON_METRICS_PREFIX)) {
                        logTronShutdownMetric(keyValue[0], keyValue[1]);
                        if (keyValue[0].equals(METRIC_SYSTEM_SERVER)) {
                            duration = keyValue[1];
                        }
                    }
                    if (keyValue[0].equals("reboot")) {
                        str = keyValue[1];
                    } else if (keyValue[0].equals("reason")) {
                        reason = keyValue[1];
                    } else if (keyValue[0].equals(METRIC_SHUTDOWN_TIME_START)) {
                        start_time = keyValue[1];
                    }
                }
            }
            logStatsdShutdownAtom(str, reason, start_time, duration);
        }
        metricsFile.delete();
    }

    private static void logTronShutdownMetric(String metricName, String valueStr) {
        try {
            int value = Integer.parseInt(valueStr);
            if (value >= 0) {
                MetricsLogger.histogram(null, metricName, value);
            }
        } catch (NumberFormatException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot parse metric ");
            stringBuilder.append(metricName);
            stringBuilder.append(" int value - ");
            stringBuilder.append(valueStr);
            Slog.e(str, stringBuilder.toString());
        }
    }

    private static void logStatsdShutdownAtom(String rebootStr, String reasonStr, String startStr, String durationStr) {
        NumberFormatException numberFormatException;
        String str;
        StringBuilder stringBuilder;
        String str2 = rebootStr;
        String str3 = startStr;
        boolean reboot = false;
        String reason = "<EMPTY>";
        long start = 0;
        long duration = 0;
        if (str2 == null) {
            Slog.e(TAG, "No value received for reboot");
        } else if (str2.equals("y")) {
            reboot = true;
        } else if (!str2.equals("n")) {
            String str4 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unexpected value for reboot : ");
            stringBuilder2.append(str2);
            Slog.e(str4, stringBuilder2.toString());
        }
        boolean reboot2 = reboot;
        if (reasonStr != null) {
            reason = reasonStr;
        } else {
            Slog.e(TAG, "No value received for shutdown reason");
        }
        if (str3 != null) {
            try {
                start = Long.parseLong(startStr);
            } catch (NumberFormatException e) {
                numberFormatException = e;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot parse shutdown start time: ");
                stringBuilder.append(str3);
                Slog.e(str, stringBuilder.toString());
            }
        } else {
            Slog.e(TAG, "No value received for shutdown start time");
        }
        if (durationStr != null) {
            try {
                duration = Long.parseLong(durationStr);
            } catch (NumberFormatException e2) {
                numberFormatException = e2;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot parse shutdown duration: ");
                stringBuilder.append(str3);
                Slog.e(str, stringBuilder.toString());
            }
        } else {
            Slog.e(TAG, "No value received for shutdown duration");
        }
        StatsLog.write(56, reboot2, reason, start, duration);
    }

    private static void logFsShutdownTime() {
        File f = null;
        for (String fileName : LAST_KMSG_FILES) {
            File file = new File(fileName);
            if (file.exists()) {
                f = file;
                break;
            }
        }
        if (f != null) {
            try {
                Matcher matcher = Pattern.compile(LAST_SHUTDOWN_TIME_PATTERN, 8).matcher(FileUtils.readTextFile(f, -16384, null));
                if (matcher.find()) {
                    MetricsLogger.histogram(null, "boot_fs_shutdown_duration", Integer.parseInt(matcher.group(1)));
                    MetricsLogger.histogram(null, "boot_fs_shutdown_umount_stat", Integer.parseInt(matcher.group(2)));
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("boot_fs_shutdown,");
                    stringBuilder.append(matcher.group(1));
                    stringBuilder.append(",");
                    stringBuilder.append(matcher.group(2));
                    Slog.i(str, stringBuilder.toString());
                } else {
                    MetricsLogger.histogram(null, "boot_fs_shutdown_umount_stat", 4);
                    Slog.w(TAG, "boot_fs_shutdown, string not found");
                }
            } catch (IOException e) {
                Slog.w(TAG, "cannot read last msg", e);
            }
        }
    }

    @VisibleForTesting
    public static int fixFsckFsStat(String partition, int statOrg, String[] lines, int startLineNumber, int endLineNumber) {
        String str = partition;
        int stat = statOrg;
        if ((stat & 1024) != 0) {
            Pattern pattern;
            StringBuilder stringBuilder;
            Pattern passPattern = Pattern.compile(FSCK_PASS_PATTERN);
            Pattern treeOptPattern = Pattern.compile(FSCK_TREE_OPTIMIZATION_PATTERN);
            boolean foundOtherFix = false;
            String otherFixLine = null;
            boolean foundTimestampAdjustment = false;
            boolean foundQuotaFix = false;
            boolean foundTreeOptimization = false;
            String currentPass = "";
            int i = startLineNumber;
            while (i < endLineNumber) {
                String line = lines[i];
                Pattern pattern2;
                if (line.contains(FSCK_FS_MODIFIED)) {
                    pattern2 = passPattern;
                    pattern = treeOptPattern;
                    break;
                }
                if (line.startsWith("Pass ")) {
                    Matcher matcher = passPattern.matcher(line);
                    if (matcher.find()) {
                        currentPass = matcher.group(1);
                    }
                    pattern2 = passPattern;
                    pattern = treeOptPattern;
                } else if (!line.startsWith("Inode ")) {
                    pattern2 = passPattern;
                    pattern = treeOptPattern;
                    if (line.startsWith("[QUOTA WARNING]") != null && currentPass.equals("5") != null) {
                        passPattern = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("fs_stat, partition:");
                        stringBuilder.append(str);
                        stringBuilder.append(" found quota warning:");
                        stringBuilder.append(line);
                        Slog.i(passPattern, stringBuilder.toString());
                        foundQuotaFix = true;
                        if (!foundTreeOptimization) {
                            otherFixLine = line;
                            break;
                        }
                    } else if (line.startsWith("Update quota info") == null || currentPass.equals("5") == null) {
                        if (line.startsWith("Timestamp(s) on inode") == null || line.contains("beyond 2310-04-04 are likely pre-1970") == null || currentPass.equals("1") == null) {
                            passPattern = line.trim();
                            if (!(passPattern.isEmpty() || currentPass.isEmpty())) {
                                foundOtherFix = true;
                                otherFixLine = passPattern;
                                break;
                            }
                        }
                        passPattern = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("fs_stat, partition:");
                        stringBuilder.append(str);
                        stringBuilder.append(" found timestamp adjustment:");
                        stringBuilder.append(line);
                        Slog.i(passPattern, stringBuilder.toString());
                        if (lines[i + 1].contains("Fix? yes") != null) {
                            i++;
                        }
                        foundTimestampAdjustment = true;
                    }
                } else if (!treeOptPattern.matcher(line).find() || !currentPass.equals("1")) {
                    pattern = treeOptPattern;
                    foundOtherFix = true;
                    otherFixLine = line;
                    break;
                } else {
                    foundTreeOptimization = true;
                    String str2 = TAG;
                    pattern2 = passPattern;
                    passPattern = new StringBuilder();
                    pattern = treeOptPattern;
                    passPattern.append("fs_stat, partition:");
                    passPattern.append(str);
                    passPattern.append(" found tree optimization:");
                    passPattern.append(line);
                    Slog.i(str2, passPattern.toString());
                }
                i++;
                passPattern = pattern2;
                treeOptPattern = pattern;
            }
            pattern = treeOptPattern;
            String str3;
            if (foundOtherFix) {
                if (otherFixLine == null) {
                    return stat;
                }
                str3 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("fs_stat, partition:");
                stringBuilder.append(str);
                stringBuilder.append(" fix:");
                stringBuilder.append(otherFixLine);
                Slog.i(str3, stringBuilder.toString());
                return stat;
            } else if (foundQuotaFix && !foundTreeOptimization) {
                str3 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("fs_stat, got quota fix without tree optimization, partition:");
                stringBuilder.append(str);
                Slog.i(str3, stringBuilder.toString());
                return stat;
            } else if ((!foundTreeOptimization || !foundQuotaFix) && !foundTimestampAdjustment) {
                return stat;
            } else {
                str3 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("fs_stat, partition:");
                stringBuilder.append(str);
                stringBuilder.append(" fix ignored");
                Slog.i(str3, stringBuilder.toString());
                return stat & -1025;
            }
        }
        int i2 = endLineNumber;
        return stat;
    }

    private static void handleFsckFsStat(Matcher match, String[] lines, int startLineNumber, int endLineNumber) {
        String partition = match.group(1);
        try {
            int stat = fixFsckFsStat(partition, Integer.decode(match.group(2)).intValue(), lines, startLineNumber, endLineNumber);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("boot_fs_stat_");
            stringBuilder.append(partition);
            MetricsLogger.histogram(null, stringBuilder.toString(), stat);
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("fs_stat, partition:");
            stringBuilder.append(partition);
            stringBuilder.append(" stat:0x");
            stringBuilder.append(Integer.toHexString(stat));
            Slog.i(str, stringBuilder.toString());
        } catch (NumberFormatException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("cannot parse fs_stat: partition:");
            stringBuilder2.append(partition);
            stringBuilder2.append(" stat:");
            stringBuilder2.append(match.group(2));
            Slog.w(str2, stringBuilder2.toString());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:38:0x0098 A:{SYNTHETIC, Splitter:B:38:0x0098} */
    /* JADX WARNING: Removed duplicated region for block: B:15:0x002b A:{Catch:{ all -> 0x00a0, Throwable -> 0x00ac, FileNotFoundException -> 0x0121, IOException -> 0x0106, IllegalStateException -> 0x00ec, NullPointerException -> 0x00d2, XmlPullParserException -> 0x00b8 }} */
    /* JADX WARNING: Missing block: B:35:0x0091, code skipped:
            if (1 == null) goto L_0x0093;
     */
    /* JADX WARNING: Missing block: B:74:0x0143, code skipped:
            if (r2 != false) goto L_0x0147;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static HashMap<String, Long> readTimestamps() {
        HashMap<String, Long> timestamps;
        String str;
        StringBuilder stringBuilder;
        synchronized (sFile) {
            timestamps = new HashMap();
            boolean success = false;
            FileInputStream stream;
            try {
                int type;
                stream = sFile.openRead();
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(stream, StandardCharsets.UTF_8.name());
                while (true) {
                    int next = parser.next();
                    type = next;
                    if (next == 2 || type == 1) {
                        if (type != 2) {
                            next = parser.getDepth();
                            while (true) {
                                int next2 = parser.next();
                                type = next2;
                                if (next2 == 1 || (type == 3 && parser.getDepth() <= next)) {
                                    success = true;
                                } else if (type != 3) {
                                    if (type != 4) {
                                        if (parser.getName().equals("log")) {
                                            timestamps.put(parser.getAttributeValue(null, "filename"), Long.valueOf(Long.valueOf(parser.getAttributeValue(null, "timestamp")).longValue()));
                                        } else {
                                            String str2 = TAG;
                                            StringBuilder stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("Unknown tag: ");
                                            stringBuilder2.append(parser.getName());
                                            Slog.w(str2, stringBuilder2.toString());
                                            XmlUtils.skipCurrentTag(parser);
                                        }
                                    }
                                }
                            }
                            success = true;
                            if (stream != null) {
                                stream.close();
                            }
                        } else {
                            throw new IllegalStateException("no start tag found");
                        }
                    }
                }
                if (type != 2) {
                }
            } catch (FileNotFoundException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("No existing last log timestamp file ");
                stringBuilder.append(sFile.getBaseFile());
                stringBuilder.append("; starting empty");
                Slog.i(str, stringBuilder.toString());
            } catch (IOException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed parsing ");
                stringBuilder.append(e2);
                Slog.w(str, stringBuilder.toString());
                if (!success) {
                    timestamps.clear();
                }
                return timestamps;
            } catch (IllegalStateException e3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed parsing ");
                stringBuilder.append(e3);
                Slog.w(str, stringBuilder.toString());
                if (!success) {
                    timestamps.clear();
                }
                return timestamps;
            } catch (NullPointerException e4) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed parsing ");
                stringBuilder.append(e4);
                Slog.w(str, stringBuilder.toString());
                if (!success) {
                    timestamps.clear();
                }
                return timestamps;
            } catch (XmlPullParserException e5) {
                try {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed parsing ");
                    stringBuilder.append(e5);
                    Slog.w(str, stringBuilder.toString());
                    if (!success) {
                        timestamps.clear();
                    }
                    return timestamps;
                } catch (Throwable th) {
                    if (!success) {
                        timestamps.clear();
                    }
                }
            } catch (Throwable th2) {
                r4.addSuppressed(th2);
            }
        }
        return timestamps;
    }

    private void writeTimestamps(HashMap<String, Long> timestamps) {
        synchronized (sFile) {
            try {
                FileOutputStream stream = sFile.startWrite();
                try {
                    XmlSerializer out = new FastXmlSerializer();
                    out.setOutput(stream, StandardCharsets.UTF_8.name());
                    out.startDocument(null, Boolean.valueOf(true));
                    out.startTag(null, "log-files");
                    for (String filename : timestamps.keySet()) {
                        out.startTag(null, "log");
                        out.attribute(null, "filename", filename);
                        out.attribute(null, "timestamp", ((Long) timestamps.get(filename)).toString());
                        out.endTag(null, "log");
                    }
                    out.endTag(null, "log-files");
                    out.endDocument();
                    sFile.finishWrite(stream);
                } catch (IOException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to write timestamp file, using the backup: ");
                    stringBuilder.append(e);
                    Slog.w(str, stringBuilder.toString());
                    sFile.failWrite(stream);
                } catch (Throwable th) {
                }
            } catch (IOException e2) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed to write timestamp file: ");
                stringBuilder2.append(e2);
                Slog.w(str2, stringBuilder2.toString());
            }
        }
    }
}
