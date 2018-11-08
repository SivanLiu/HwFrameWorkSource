package com.android.server;

import android.content.Context;
import android.os.Binder;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.util.Log;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.DumpUtils;
import com.android.server.storage.DiskStatsFileLogger;
import com.android.server.storage.DiskStatsLoggingService;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import libcore.io.IoUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class DiskStatsService extends Binder {
    private static final String DISKSTATS_DUMP_FILE = "/data/system/diskstats_cache.json";
    private static final String TAG = "DiskStatsService";
    private final Context mContext;

    public DiskStatsService(Context context) {
        this.mContext = context;
        DiskStatsLoggingService.schedule(context);
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        IOException e;
        boolean protoFormat;
        boolean fileBased;
        boolean blockBased;
        Throwable th;
        if (DumpUtils.checkDumpAndUsageStatsPermission(this.mContext, TAG, pw)) {
            long after;
            ProtoOutputStream protoOutputStream;
            byte[] junk = new byte[512];
            for (int i = 0; i < junk.length; i++) {
                junk[i] = (byte) i;
            }
            File file = new File(Environment.getDataDirectory(), "system/perftest.tmp");
            FileOutputStream fileOutputStream = null;
            IOException error = null;
            long before = SystemClock.uptimeMillis();
            try {
                FileOutputStream fileOutputStream2 = new FileOutputStream(file);
                try {
                    fileOutputStream2.write(junk);
                    if (fileOutputStream2 != null) {
                        try {
                            fileOutputStream2.close();
                        } catch (IOException e2) {
                        }
                    }
                    fileOutputStream = fileOutputStream2;
                } catch (IOException e3) {
                    e = e3;
                    fileOutputStream = fileOutputStream2;
                    error = e;
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e4) {
                        }
                    }
                    after = SystemClock.uptimeMillis();
                    if (file.exists()) {
                        file.delete();
                    }
                    protoFormat = hasOption(args, "--proto");
                    protoOutputStream = null;
                    if (!protoFormat) {
                        protoOutputStream = new ProtoOutputStream(fd);
                        pw = null;
                        protoOutputStream.write(1155346202625L, error != null);
                        if (error != null) {
                            protoOutputStream.write(1159641169922L, error.toString());
                        } else {
                            protoOutputStream.write(1112396529667L, after - before);
                        }
                    } else if (error != null) {
                        pw.print("Test-Error: ");
                        pw.println(error.toString());
                    } else {
                        pw.print("Latency: ");
                        pw.print(after - before);
                        pw.println("ms [512B Data Write]");
                    }
                    reportFreeSpace(Environment.getDataDirectory(), "Data", pw, protoOutputStream, 0);
                    reportFreeSpace(Environment.getDownloadCacheDirectory(), "Cache", pw, protoOutputStream, 1);
                    reportFreeSpace(new File("/system"), "System", pw, protoOutputStream, 2);
                    fileBased = StorageManager.isFileEncryptedNativeOnly();
                    blockBased = fileBased ? false : StorageManager.isBlockEncrypted();
                    if (protoFormat) {
                        if (!fileBased) {
                            protoOutputStream.write(1168231104517L, 3);
                        } else if (blockBased) {
                            protoOutputStream.write(1168231104517L, 2);
                        } else {
                            protoOutputStream.write(1168231104517L, 1);
                        }
                    } else if (fileBased) {
                        pw.println("File-based Encryption: true");
                    }
                    if (protoFormat) {
                        reportCachedValuesProto(protoOutputStream);
                    } else {
                        reportCachedValues(pw);
                    }
                    if (protoFormat) {
                        protoOutputStream.flush();
                    }
                } catch (Throwable th2) {
                    th = th2;
                    fileOutputStream = fileOutputStream2;
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e5) {
                        }
                    }
                    throw th;
                }
            } catch (IOException e6) {
                e = e6;
                error = e;
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
                after = SystemClock.uptimeMillis();
                if (file.exists()) {
                    file.delete();
                }
                protoFormat = hasOption(args, "--proto");
                protoOutputStream = null;
                if (!protoFormat) {
                    protoOutputStream = new ProtoOutputStream(fd);
                    pw = null;
                    if (error != null) {
                    }
                    protoOutputStream.write(1155346202625L, error != null);
                    if (error != null) {
                        protoOutputStream.write(1112396529667L, after - before);
                    } else {
                        protoOutputStream.write(1159641169922L, error.toString());
                    }
                } else if (error != null) {
                    pw.print("Latency: ");
                    pw.print(after - before);
                    pw.println("ms [512B Data Write]");
                } else {
                    pw.print("Test-Error: ");
                    pw.println(error.toString());
                }
                reportFreeSpace(Environment.getDataDirectory(), "Data", pw, protoOutputStream, 0);
                reportFreeSpace(Environment.getDownloadCacheDirectory(), "Cache", pw, protoOutputStream, 1);
                reportFreeSpace(new File("/system"), "System", pw, protoOutputStream, 2);
                fileBased = StorageManager.isFileEncryptedNativeOnly();
                if (fileBased) {
                }
                if (protoFormat) {
                    if (fileBased) {
                        pw.println("File-based Encryption: true");
                    }
                } else if (!fileBased) {
                    protoOutputStream.write(1168231104517L, 3);
                } else if (blockBased) {
                    protoOutputStream.write(1168231104517L, 1);
                } else {
                    protoOutputStream.write(1168231104517L, 2);
                }
                if (protoFormat) {
                    reportCachedValues(pw);
                } else {
                    reportCachedValuesProto(protoOutputStream);
                }
                if (protoFormat) {
                    protoOutputStream.flush();
                }
            } catch (Throwable th3) {
                th = th3;
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
                throw th;
            }
            after = SystemClock.uptimeMillis();
            if (file.exists()) {
                file.delete();
            }
            protoFormat = hasOption(args, "--proto");
            protoOutputStream = null;
            if (!protoFormat) {
                protoOutputStream = new ProtoOutputStream(fd);
                pw = null;
                if (error != null) {
                }
                protoOutputStream.write(1155346202625L, error != null);
                if (error != null) {
                    protoOutputStream.write(1159641169922L, error.toString());
                } else {
                    protoOutputStream.write(1112396529667L, after - before);
                }
            } else if (error != null) {
                pw.print("Test-Error: ");
                pw.println(error.toString());
            } else {
                pw.print("Latency: ");
                pw.print(after - before);
                pw.println("ms [512B Data Write]");
            }
            reportFreeSpace(Environment.getDataDirectory(), "Data", pw, protoOutputStream, 0);
            reportFreeSpace(Environment.getDownloadCacheDirectory(), "Cache", pw, protoOutputStream, 1);
            reportFreeSpace(new File("/system"), "System", pw, protoOutputStream, 2);
            fileBased = StorageManager.isFileEncryptedNativeOnly();
            if (fileBased) {
            }
            if (protoFormat) {
                if (!fileBased) {
                    protoOutputStream.write(1168231104517L, 3);
                } else if (blockBased) {
                    protoOutputStream.write(1168231104517L, 2);
                } else {
                    protoOutputStream.write(1168231104517L, 1);
                }
            } else if (fileBased) {
                pw.println("File-based Encryption: true");
            }
            if (protoFormat) {
                reportCachedValuesProto(protoOutputStream);
            } else {
                reportCachedValues(pw);
            }
            if (protoFormat) {
                protoOutputStream.flush();
            }
        }
    }

    private void reportFreeSpace(File path, String name, PrintWriter pw, ProtoOutputStream proto, int folderType) {
        try {
            StatFs statfs = new StatFs(path.getPath());
            long bsize = (long) statfs.getBlockSize();
            long avail = (long) statfs.getAvailableBlocks();
            long total = (long) statfs.getBlockCount();
            if (bsize <= 0 || total <= 0) {
                throw new IllegalArgumentException("Invalid stat: bsize=" + bsize + " avail=" + avail + " total=" + total);
            }
            if (proto != null) {
                long freeSpaceToken = proto.start(2272037699588L);
                proto.write(1168231104513L, folderType);
                proto.write(1116691496962L, (avail * bsize) / 1024);
                proto.write(1116691496963L, (total * bsize) / 1024);
                proto.end(freeSpaceToken);
            } else {
                pw.print(name);
                pw.print("-Free: ");
                pw.print((avail * bsize) / 1024);
                pw.print("K / ");
                pw.print((total * bsize) / 1024);
                pw.print("K total = ");
                pw.print((100 * avail) / total);
                pw.println("% free");
            }
        } catch (IllegalArgumentException e) {
            if (proto == null) {
                pw.print(name);
                pw.print("-Error: ");
                pw.println(e.toString());
            }
        }
    }

    private boolean hasOption(String[] args, String arg) {
        for (String opt : args) {
            if (arg.equals(opt)) {
                return true;
            }
        }
        return false;
    }

    private void reportCachedValues(PrintWriter pw) {
        try {
            JSONObject json = new JSONObject(IoUtils.readFileAsString("/data/system/diskstats_cache.json"));
            pw.print("App Size: ");
            pw.println(json.getLong(DiskStatsFileLogger.APP_SIZE_AGG_KEY));
            pw.print("App Cache Size: ");
            pw.println(json.getLong(DiskStatsFileLogger.APP_CACHE_AGG_KEY));
            pw.print("Photos Size: ");
            pw.println(json.getLong(DiskStatsFileLogger.PHOTOS_KEY));
            pw.print("Videos Size: ");
            pw.println(json.getLong(DiskStatsFileLogger.VIDEOS_KEY));
            pw.print("Audio Size: ");
            pw.println(json.getLong(DiskStatsFileLogger.AUDIO_KEY));
            pw.print("Downloads Size: ");
            pw.println(json.getLong(DiskStatsFileLogger.DOWNLOADS_KEY));
            pw.print("System Size: ");
            pw.println(json.getLong(DiskStatsFileLogger.SYSTEM_KEY));
            pw.print("Other Size: ");
            pw.println(json.getLong(DiskStatsFileLogger.MISC_KEY));
            pw.print("Package Names: ");
            pw.println(json.getJSONArray(DiskStatsFileLogger.PACKAGE_NAMES_KEY));
            pw.print("App Sizes: ");
            pw.println(json.getJSONArray(DiskStatsFileLogger.APP_SIZES_KEY));
            pw.print("Cache Sizes: ");
            pw.println(json.getJSONArray(DiskStatsFileLogger.APP_CACHES_KEY));
        } catch (Exception e) {
            Log.w(TAG, "exception reading diskstats cache file", e);
        }
    }

    private void reportCachedValuesProto(ProtoOutputStream proto) {
        try {
            JSONObject json = new JSONObject(IoUtils.readFileAsString("/data/system/diskstats_cache.json"));
            long cachedValuesToken = proto.start(1172526071814L);
            proto.write(1116691496961L, json.getLong(DiskStatsFileLogger.APP_SIZE_AGG_KEY));
            proto.write(1116691496962L, json.getLong(DiskStatsFileLogger.APP_CACHE_AGG_KEY));
            proto.write(1116691496963L, json.getLong(DiskStatsFileLogger.PHOTOS_KEY));
            proto.write(1116691496964L, json.getLong(DiskStatsFileLogger.VIDEOS_KEY));
            proto.write(1116691496965L, json.getLong(DiskStatsFileLogger.AUDIO_KEY));
            proto.write(1116691496966L, json.getLong(DiskStatsFileLogger.DOWNLOADS_KEY));
            proto.write(1116691496967L, json.getLong(DiskStatsFileLogger.SYSTEM_KEY));
            proto.write(1116691496968L, json.getLong(DiskStatsFileLogger.MISC_KEY));
            JSONArray packageNamesArray = json.getJSONArray(DiskStatsFileLogger.PACKAGE_NAMES_KEY);
            JSONArray appSizesArray = json.getJSONArray(DiskStatsFileLogger.APP_SIZES_KEY);
            JSONArray cacheSizesArray = json.getJSONArray(DiskStatsFileLogger.APP_CACHES_KEY);
            int len = packageNamesArray.length();
            if (len == appSizesArray.length() && len == cacheSizesArray.length()) {
                for (int i = 0; i < len; i++) {
                    long packageToken = proto.start(2272037699593L);
                    proto.write(1159641169921L, packageNamesArray.getString(i));
                    proto.write(1116691496962L, appSizesArray.getLong(i));
                    proto.write(1116691496963L, cacheSizesArray.getLong(i));
                    proto.end(packageToken);
                }
            } else {
                Slog.wtf(TAG, "Sizes of packageNamesArray, appSizesArray and cacheSizesArray are not the same");
            }
            proto.end(cachedValuesToken);
        } catch (Exception e) {
            Log.w(TAG, "exception reading diskstats cache file", e);
        }
    }
}
