package com.android.server;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.DropBoxManager.Entry;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.IDropBoxManagerService;
import com.android.internal.os.IDropBoxManagerService.Stub;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.ObjectUtils;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.PackageManagerService;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.GZIPOutputStream;
import libcore.io.IoUtils;

public final class DropBoxManagerService extends SystemService {
    private static final int DEFAULT_AGE_SECONDS = 259200;
    private static final int DEFAULT_MAX_FILES = 1000;
    private static final int DEFAULT_MAX_FILES_LOWRAM = 300;
    private static final int DEFAULT_QUOTA_KB = 5120;
    private static final int DEFAULT_QUOTA_PERCENT = 10;
    private static final int DEFAULT_RESERVE_PERCENT = 10;
    private static final int DEFAULT_RESERVE_PERCENT_FOR_DEBUGGABLE_DEVICES = 1;
    private static final int MSG_SEND_BROADCAST = 1;
    private static final boolean PROFILE_DUMP = false;
    private static final int QUOTA_RESCAN_MILLIS = 5000;
    private static final String TAG = "DropBoxManagerService";
    private FileList mAllFiles;
    private int mBlockSize;
    private volatile boolean mBooted;
    private int mCachedQuotaBlocks;
    private long mCachedQuotaUptimeMillis;
    private final ContentResolver mContentResolver;
    private final File mDropBoxDir;
    private ArrayMap<String, FileList> mFilesByTag;
    private final Handler mHandler;
    private int mMaxFiles;
    private final BroadcastReceiver mReceiver;
    private StatFs mStatFs;
    private final Stub mStub;

    @VisibleForTesting
    static final class EntryFile implements Comparable<EntryFile> {
        public final int blocks;
        public final int flags;
        public final String tag;
        public final long timestampMillis;

        public final int compareTo(EntryFile o) {
            int comp = Long.compare(this.timestampMillis, o.timestampMillis);
            if (comp != 0) {
                return comp;
            }
            comp = ObjectUtils.compare(this.tag, o.tag);
            if (comp != 0) {
                return comp;
            }
            comp = Integer.compare(this.flags, o.flags);
            if (comp != 0) {
                return comp;
            }
            return Integer.compare(hashCode(), o.hashCode());
        }

        public EntryFile(File temp, File dir, String tag, long timestampMillis, int flags, int blockSize) throws IOException {
            if ((flags & 1) == 0) {
                this.tag = TextUtils.safeIntern(tag);
                this.timestampMillis = timestampMillis;
                this.flags = flags;
                File file = getFile(dir);
                if (temp.renameTo(file)) {
                    this.blocks = (int) (((file.length() + ((long) blockSize)) - 1) / ((long) blockSize));
                    return;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Can't rename ");
                stringBuilder.append(temp);
                stringBuilder.append(" to ");
                stringBuilder.append(file);
                throw new IOException(stringBuilder.toString());
            }
            throw new IllegalArgumentException();
        }

        public EntryFile(File dir, String tag, long timestampMillis) throws IOException {
            this.tag = TextUtils.safeIntern(tag);
            this.timestampMillis = timestampMillis;
            this.flags = 1;
            this.blocks = 0;
            new FileOutputStream(getFile(dir)).close();
        }

        public EntryFile(File file, int blockSize) {
            boolean parseFailure = false;
            String name = file.getName();
            int flags = 0;
            String tag = null;
            long millis = 0;
            int at = name.lastIndexOf(64);
            if (at < 0) {
                parseFailure = true;
            } else {
                tag = Uri.decode(name.substring(0, at));
                if (name.endsWith(PackageManagerService.COMPRESSED_EXTENSION)) {
                    flags = 0 | 4;
                    name = name.substring(0, name.length() - 3);
                }
                if (name.endsWith(".lost")) {
                    flags |= 1;
                    name = name.substring(at + 1, name.length() - 5);
                } else if (name.endsWith(".txt")) {
                    flags |= 2;
                    name = name.substring(at + 1, name.length() - 4);
                } else if (name.endsWith(".dat")) {
                    name = name.substring(at + 1, name.length() - 4);
                } else {
                    parseFailure = true;
                }
                if (!parseFailure) {
                    try {
                        millis = Long.parseLong(name);
                    } catch (NumberFormatException e) {
                        parseFailure = true;
                    }
                }
            }
            if (parseFailure) {
                String str = DropBoxManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid filename: ");
                stringBuilder.append(file);
                Slog.wtf(str, stringBuilder.toString());
                file.delete();
                this.tag = null;
                this.flags = 1;
                this.timestampMillis = 0;
                this.blocks = 0;
                return;
            }
            this.blocks = (int) (((file.length() + ((long) blockSize)) - 1) / ((long) blockSize));
            this.tag = TextUtils.safeIntern(tag);
            this.flags = flags;
            this.timestampMillis = millis;
        }

        public EntryFile(long millis) {
            this.tag = null;
            this.timestampMillis = millis;
            this.flags = 1;
            this.blocks = 0;
        }

        public boolean hasFile() {
            return this.tag != null;
        }

        private String getExtension() {
            if ((this.flags & 1) != 0) {
                return ".lost";
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append((this.flags & 2) != 0 ? ".txt" : ".dat");
            stringBuilder.append((this.flags & 4) != 0 ? PackageManagerService.COMPRESSED_EXTENSION : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            return stringBuilder.toString();
        }

        public String getFilename() {
            if (!hasFile()) {
                return null;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Uri.encode(this.tag));
            stringBuilder.append("@");
            stringBuilder.append(this.timestampMillis);
            stringBuilder.append(getExtension());
            return stringBuilder.toString();
        }

        public File getFile(File dir) {
            return hasFile() ? new File(dir, getFilename()) : null;
        }

        public void deleteFile(File dir) {
            if (hasFile()) {
                getFile(dir).delete();
            }
        }
    }

    private static final class FileList implements Comparable<FileList> {
        public int blocks;
        public final TreeSet<EntryFile> contents;

        private FileList() {
            this.blocks = 0;
            this.contents = new TreeSet();
        }

        /* synthetic */ FileList(AnonymousClass1 x0) {
            this();
        }

        public final int compareTo(FileList o) {
            if (this.blocks != o.blocks) {
                return o.blocks - this.blocks;
            }
            if (this == o) {
                return 0;
            }
            if (hashCode() < o.hashCode()) {
                return -1;
            }
            if (hashCode() > o.hashCode()) {
                return 1;
            }
            return 0;
        }
    }

    public DropBoxManagerService(Context context) {
        this(context, new File("/data/system/dropbox"), FgThread.get().getLooper());
    }

    @VisibleForTesting
    public DropBoxManagerService(Context context, File path, Looper looper) {
        super(context);
        this.mAllFiles = null;
        this.mFilesByTag = null;
        this.mStatFs = null;
        this.mBlockSize = 0;
        this.mCachedQuotaBlocks = 0;
        this.mCachedQuotaUptimeMillis = 0;
        this.mBooted = false;
        this.mMaxFiles = -1;
        this.mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                DropBoxManagerService.this.mCachedQuotaUptimeMillis = 0;
                new Thread() {
                    public void run() {
                        try {
                            DropBoxManagerService.this.init();
                            DropBoxManagerService.this.trimToFit();
                        } catch (IOException e) {
                            Slog.e(DropBoxManagerService.TAG, "Can't init", e);
                        }
                    }
                }.start();
            }
        };
        this.mStub = new Stub() {
            public void add(Entry entry) {
                DropBoxManagerService.this.add(entry);
            }

            public boolean isTagEnabled(String tag) {
                return DropBoxManagerService.this.isTagEnabled(tag);
            }

            public Entry getNextEntry(String tag, long millis) {
                return DropBoxManagerService.this.getNextEntry(tag, millis);
            }

            public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
                DropBoxManagerService.this.dump(fd, pw, args);
            }
        };
        this.mDropBoxDir = path;
        this.mContentResolver = getContext().getContentResolver();
        this.mHandler = new Handler(looper) {
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    DropBoxManagerService.this.getContext().sendBroadcastAsUser((Intent) msg.obj, UserHandle.SYSTEM, "android.permission.READ_LOGS");
                }
            }
        };
    }

    public void onStart() {
        publishBinderService("dropbox", this.mStub);
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.DEVICE_STORAGE_LOW");
            getContext().registerReceiver(this.mReceiver, filter);
            this.mContentResolver.registerContentObserver(Global.CONTENT_URI, true, new ContentObserver(new Handler()) {
                public void onChange(boolean selfChange) {
                    DropBoxManagerService.this.mReceiver.onReceive(DropBoxManagerService.this.getContext(), (Intent) null);
                }
            });
        } else if (phase == 1000) {
            this.mBooted = true;
        }
    }

    public IDropBoxManagerService getServiceStub() {
        return this.mStub;
    }

    /* JADX WARNING: Missing block: B:49:0x0153, code skipped:
            if (null != null) goto L_0x0155;
     */
    /* JADX WARNING: Missing block: B:50:0x0155, code skipped:
            r2.delete();
     */
    /* JADX WARNING: Missing block: B:60:0x018c, code skipped:
            if (r2 == null) goto L_0x018f;
     */
    /* JADX WARNING: Missing block: B:61:0x018f, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void add(Entry entry) {
        File temp = null;
        String tag = entry.getTag();
        try {
            int flags = entry.getFlags();
            if ((flags & 1) == 0) {
                init();
                if (isTagEnabled(tag)) {
                    long max = trimToFit();
                    long lastTrim = System.currentTimeMillis();
                    byte[] buffer = new byte[this.mBlockSize];
                    InputStream input = entry.getInputStream();
                    int read = 0;
                    while (read < buffer.length) {
                        int n = input.read(buffer, read, buffer.length - read);
                        if (n <= 0) {
                            break;
                        }
                        read += n;
                    }
                    File file = this.mDropBoxDir;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("drop");
                    long max2 = max;
                    stringBuilder.append(Thread.currentThread().getId());
                    stringBuilder.append(".tmp");
                    temp = new File(file, stringBuilder.toString());
                    int bufferSize = this.mBlockSize;
                    if (bufferSize > 4096) {
                        bufferSize = 4096;
                    }
                    if (bufferSize < 512) {
                        bufferSize = 512;
                    }
                    FileOutputStream foutput = new FileOutputStream(temp);
                    OutputStream output = new BufferedOutputStream(foutput, bufferSize);
                    if (read == buffer.length && (flags & 4) == 0) {
                        output = new GZIPOutputStream(output);
                        flags |= 4;
                    }
                    while (true) {
                        long lastTrim2;
                        output.write(buffer, 0, read);
                        long now = System.currentTimeMillis();
                        if (now - lastTrim > 30000) {
                            long j = now;
                            lastTrim = trimToFit();
                            lastTrim2 = j;
                        } else {
                            lastTrim2 = lastTrim;
                            lastTrim = max2;
                        }
                        read = input.read(buffer);
                        if (read <= 0) {
                            FileUtils.sync(foutput);
                            output.close();
                            output = null;
                        } else {
                            output.flush();
                        }
                        if (temp.length() > lastTrim) {
                            String str = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Dropping: ");
                            stringBuilder2.append(tag);
                            stringBuilder2.append(" (");
                            stringBuilder2.append(temp.length());
                            stringBuilder2.append(" > ");
                            stringBuilder2.append(lastTrim);
                            stringBuilder2.append(" bytes)");
                            Slog.w(str, stringBuilder2.toString());
                            temp.delete();
                            temp = null;
                            break;
                        }
                        int bufferSize2 = bufferSize;
                        FileOutputStream foutput2 = foutput;
                        if (read <= 0) {
                            break;
                        }
                        int i = flags;
                        max2 = lastTrim;
                        lastTrim = lastTrim2;
                        bufferSize = bufferSize2;
                        foutput = foutput2;
                    }
                    if (temp != null) {
                        FileUtils.setPermissions(temp, 432, -1, -1);
                    }
                    max = createEntry(temp, tag, flags);
                    temp = null;
                    Intent dropboxIntent = new Intent("android.intent.action.DROPBOX_ENTRY_ADDED");
                    dropboxIntent.putExtra("tag", tag);
                    dropboxIntent.putExtra("time", max);
                    if (!this.mBooted) {
                        dropboxIntent.addFlags(1073741824);
                    }
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(1, dropboxIntent));
                    IoUtils.closeQuietly(output);
                    IoUtils.closeQuietly(input);
                    entry.close();
                } else {
                    IoUtils.closeQuietly(null);
                    IoUtils.closeQuietly(null);
                    entry.close();
                    if (temp != null) {
                        temp.delete();
                    }
                    return;
                }
            }
            throw new IllegalArgumentException();
        } catch (IOException e) {
            String str2 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Can't write: ");
            stringBuilder3.append(tag);
            Slog.e(str2, stringBuilder3.toString(), e);
            IoUtils.closeQuietly(null);
            IoUtils.closeQuietly(null);
            entry.close();
        } catch (Throwable th) {
            IoUtils.closeQuietly(null);
            IoUtils.closeQuietly(null);
            entry.close();
            if (temp != null) {
                temp.delete();
            }
        }
    }

    public boolean isTagEnabled(String tag) {
        long token = Binder.clearCallingIdentity();
        boolean e;
        try {
            ContentResolver contentResolver = this.mContentResolver;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("dropbox:");
            stringBuilder.append(tag);
            e = "disabled".equals(Global.getString(contentResolver, stringBuilder.toString())) ^ 1;
            return e;
        } catch (RuntimeException e2) {
            e = e2;
            Slog.e(TAG, "Failure getting tag enabled", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public synchronized Entry getNextEntry(String tag, long millis) {
        if (getContext().checkCallingOrSelfPermission("android.permission.READ_LOGS") == 0) {
            try {
                init();
                FileList list = tag == null ? this.mAllFiles : (FileList) this.mFilesByTag.get(tag);
                if (list == null) {
                    return null;
                }
                for (EntryFile entry : list.contents.tailSet(new EntryFile(1 + millis))) {
                    if (entry.tag != null) {
                        if ((entry.flags & 1) != 0) {
                            return new Entry(entry.tag, entry.timestampMillis);
                        }
                        File file = entry.getFile(this.mDropBoxDir);
                        try {
                            return new Entry(entry.tag, entry.timestampMillis, file, entry.flags);
                        } catch (IOException e) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Can't read: ");
                            stringBuilder.append(file);
                            Slog.wtf(str, stringBuilder.toString(), e);
                        }
                    }
                }
                return null;
            } catch (IOException e2) {
                Slog.e(TAG, "Can't init", e2);
                return null;
            }
        }
        throw new SecurityException("READ_LOGS permission required");
    }

    /* JADX WARNING: Removed duplicated region for block: B:189:0x033e A:{Catch:{ IOException -> 0x0379 }} */
    /* JADX WARNING: Removed duplicated region for block: B:170:0x031c A:{SYNTHETIC, Splitter:B:170:0x031c} */
    /* JADX WARNING: Removed duplicated region for block: B:173:0x0321 A:{SYNTHETIC, Splitter:B:173:0x0321} */
    /* JADX WARNING: Removed duplicated region for block: B:189:0x033e A:{Catch:{ IOException -> 0x0379 }} */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x032b A:{SYNTHETIC, Splitter:B:179:0x032b} */
    /* JADX WARNING: Removed duplicated region for block: B:182:0x0330 A:{SYNTHETIC, Splitter:B:182:0x0330} */
    /* JADX WARNING: Removed duplicated region for block: B:170:0x031c A:{SYNTHETIC, Splitter:B:170:0x031c} */
    /* JADX WARNING: Removed duplicated region for block: B:173:0x0321 A:{SYNTHETIC, Splitter:B:173:0x0321} */
    /* JADX WARNING: Removed duplicated region for block: B:189:0x033e A:{Catch:{ IOException -> 0x0379 }} */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x032b A:{SYNTHETIC, Splitter:B:179:0x032b} */
    /* JADX WARNING: Removed duplicated region for block: B:182:0x0330 A:{SYNTHETIC, Splitter:B:182:0x0330} */
    /* JADX WARNING: Removed duplicated region for block: B:170:0x031c A:{SYNTHETIC, Splitter:B:170:0x031c} */
    /* JADX WARNING: Removed duplicated region for block: B:173:0x0321 A:{SYNTHETIC, Splitter:B:173:0x0321} */
    /* JADX WARNING: Removed duplicated region for block: B:189:0x033e A:{Catch:{ IOException -> 0x0379 }} */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x032b A:{SYNTHETIC, Splitter:B:179:0x032b} */
    /* JADX WARNING: Removed duplicated region for block: B:182:0x0330 A:{SYNTHETIC, Splitter:B:182:0x0330} */
    /* JADX WARNING: Removed duplicated region for block: B:170:0x031c A:{SYNTHETIC, Splitter:B:170:0x031c} */
    /* JADX WARNING: Removed duplicated region for block: B:173:0x0321 A:{SYNTHETIC, Splitter:B:173:0x0321} */
    /* JADX WARNING: Removed duplicated region for block: B:189:0x033e A:{Catch:{ IOException -> 0x0379 }} */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x032b A:{SYNTHETIC, Splitter:B:179:0x032b} */
    /* JADX WARNING: Removed duplicated region for block: B:182:0x0330 A:{SYNTHETIC, Splitter:B:182:0x0330} */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x032b A:{SYNTHETIC, Splitter:B:179:0x032b} */
    /* JADX WARNING: Removed duplicated region for block: B:182:0x0330 A:{SYNTHETIC, Splitter:B:182:0x0330} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        int numArgs;
        Time time;
        int numFound;
        boolean doFile;
        IOException e;
        ArrayList<String> arrayList;
        Object obj;
        PrintWriter printWriter = pw;
        String[] strArr = args;
        synchronized (this) {
            if (DumpUtils.checkDumpAndUsageStatsPermission(getContext(), TAG, printWriter)) {
                StringBuilder out;
                try {
                    init();
                    out = new StringBuilder();
                    ArrayList<String> searchArgs = new ArrayList();
                    boolean doFile2 = false;
                    boolean doPrint = false;
                    int i = 0;
                    while (strArr != null && i < strArr.length) {
                        if (!strArr[i].equals("-p")) {
                            if (!strArr[i].equals("--print")) {
                                if (!strArr[i].equals("-f")) {
                                    if (!strArr[i].equals("--file")) {
                                        if (!strArr[i].equals("-h")) {
                                            if (!strArr[i].equals("--help")) {
                                                if (strArr[i].startsWith("-")) {
                                                    out.append("Unknown argument: ");
                                                    out.append(strArr[i]);
                                                    out.append("\n");
                                                } else {
                                                    searchArgs.add(strArr[i]);
                                                }
                                                i++;
                                            }
                                        }
                                        printWriter.println("Dropbox (dropbox) dump options:");
                                        printWriter.println("  [-h|--help] [-p|--print] [-f|--file] [timestamp]");
                                        printWriter.println("    -h|--help: print this help");
                                        printWriter.println("    -p|--print: print full contents of each entry");
                                        printWriter.println("    -f|--file: print path of each entry's file");
                                        printWriter.println("  [timestamp] optionally filters to only those entries.");
                                        return;
                                    }
                                }
                                doFile2 = true;
                                i++;
                            }
                        }
                        doPrint = true;
                        i++;
                    }
                    out.append("Drop box contents: ");
                    out.append(this.mAllFiles.contents.size());
                    out.append(" entries\n");
                    out.append("Max entries: ");
                    out.append(this.mMaxFiles);
                    out.append("\n");
                    if (!searchArgs.isEmpty()) {
                        out.append("Searching for:");
                        Iterator it = searchArgs.iterator();
                        while (it.hasNext()) {
                            String a = (String) it.next();
                            out.append(" ");
                            out.append(a);
                        }
                        out.append("\n");
                    }
                    i = 0;
                    int numArgs2 = searchArgs.size();
                    Time time2 = new Time();
                    out.append("\n");
                    Iterator it2 = this.mAllFiles.contents.iterator();
                    while (it2.hasNext()) {
                        boolean z;
                        ArrayList<String> searchArgs2;
                        EntryFile entry = (EntryFile) it2.next();
                        time2.set(entry.timestampMillis);
                        String date = time2.format("%Y-%m-%d %H:%M:%S");
                        boolean match = true;
                        int i2 = 0;
                        while (true) {
                            z = true;
                            if (i2 >= numArgs2 || !match) {
                                searchArgs2 = searchArgs;
                            } else {
                                String arg = (String) searchArgs.get(i2);
                                if (date.contains(arg)) {
                                    searchArgs2 = searchArgs;
                                } else {
                                    searchArgs2 = searchArgs;
                                    if (arg.equals(entry.tag) == null) {
                                        z = false;
                                    }
                                }
                                match = z;
                                i2++;
                                searchArgs = searchArgs2;
                            }
                        }
                        searchArgs2 = searchArgs;
                        if (match) {
                            searchArgs = i + 1;
                            if (doPrint) {
                                out.append("========================================\n");
                            }
                            out.append(date);
                            out.append(" ");
                            out.append(entry.tag == null ? "(no tag)" : entry.tag);
                            File file = entry.getFile(this.mDropBoxDir);
                            if (file == null) {
                                out.append(" (no file)\n");
                            } else if ((entry.flags & 1) != 0) {
                                out.append(" (contents lost)\n");
                            } else {
                                out.append(" (");
                                if ((entry.flags & 4) != 0) {
                                    out.append("compressed ");
                                }
                                out.append((entry.flags & 2) != 0 ? "text" : "data");
                                out.append(", ");
                                numArgs = numArgs2;
                                time = time2;
                                out.append(file.length());
                                out.append(" bytes)\n");
                                if (doFile2 || (doPrint && (entry.flags & 2) == 0)) {
                                    if (!doPrint) {
                                        out.append("    ");
                                    }
                                    out.append(file.getPath());
                                    out.append("\n");
                                }
                                File file2;
                                if ((entry.flags & 2) == 0) {
                                    numFound = searchArgs;
                                    file2 = file;
                                    doFile = doFile2;
                                } else if (doPrint || !doFile2) {
                                    numArgs2 = 0;
                                    time2 = null;
                                    try {
                                        doFile = doFile2;
                                        Entry dbe = null;
                                        try {
                                            numFound = searchArgs;
                                            try {
                                                numArgs2 = new Entry(entry.tag, entry.timestampMillis, file, entry.flags);
                                                char c = 10;
                                                if (doPrint) {
                                                    try {
                                                        time2 = new InputStreamReader(numArgs2.getInputStream());
                                                        searchArgs = new char[4096];
                                                        doFile2 = false;
                                                        while (true) {
                                                            i2 = time2.read(searchArgs);
                                                            if (i2 <= 0) {
                                                                break;
                                                            }
                                                            file2 = file;
                                                            try {
                                                                out.append(searchArgs, null, i2);
                                                                doFile2 = searchArgs[i2 + -1] == c;
                                                                if (out.length() > 65536) {
                                                                    printWriter.write(out.toString());
                                                                    out.setLength(0);
                                                                }
                                                                file = file2;
                                                                c = 10;
                                                            } catch (IOException e2) {
                                                                e = e2;
                                                                try {
                                                                    out.append("*** ");
                                                                    out.append(e.toString());
                                                                    out.append("\n");
                                                                    if (numArgs2 != 0) {
                                                                    }
                                                                    if (time2 != null) {
                                                                    }
                                                                    if (doPrint) {
                                                                    }
                                                                    searchArgs = searchArgs2;
                                                                    numArgs2 = numArgs;
                                                                    time2 = time;
                                                                    doFile2 = doFile;
                                                                    i = numFound;
                                                                } catch (Throwable th) {
                                                                    searchArgs = th;
                                                                    if (numArgs2 != 0) {
                                                                    }
                                                                    if (time2 != null) {
                                                                    }
                                                                    throw searchArgs;
                                                                }
                                                            }
                                                        }
                                                        if (!doFile2) {
                                                            try {
                                                                out.append("\n");
                                                            } catch (IOException e3) {
                                                                e = e3;
                                                                file2 = file;
                                                            } catch (Throwable th2) {
                                                                searchArgs = th2;
                                                                file2 = file;
                                                                if (numArgs2 != 0) {
                                                                }
                                                                if (time2 != null) {
                                                                }
                                                                throw searchArgs;
                                                            }
                                                        }
                                                        file2 = file;
                                                    } catch (IOException e4) {
                                                        e = e4;
                                                        file2 = file;
                                                        out.append("*** ");
                                                        out.append(e.toString());
                                                        out.append("\n");
                                                        if (numArgs2 != 0) {
                                                        }
                                                        if (time2 != null) {
                                                        }
                                                        if (doPrint) {
                                                        }
                                                        searchArgs = searchArgs2;
                                                        numArgs2 = numArgs;
                                                        time2 = time;
                                                        doFile2 = doFile;
                                                        i = numFound;
                                                    } catch (Throwable th22) {
                                                        file2 = file;
                                                        searchArgs = th22;
                                                        if (numArgs2 != 0) {
                                                        }
                                                        if (time2 != null) {
                                                        }
                                                        throw searchArgs;
                                                    }
                                                }
                                                file2 = file;
                                                searchArgs = numArgs2.getText(70);
                                                out.append("    ");
                                                if (searchArgs == null) {
                                                    out.append("[null]");
                                                } else {
                                                    if (searchArgs.length() != 70) {
                                                        z = false;
                                                    }
                                                    boolean truncated = z;
                                                    out.append(searchArgs.trim().replace(10, '/'));
                                                    if (truncated) {
                                                        out.append(" ...");
                                                    }
                                                }
                                                out.append("\n");
                                                numArgs2.close();
                                                if (isr != null) {
                                                    try {
                                                        isr.close();
                                                    } catch (IOException e5) {
                                                    }
                                                }
                                            } catch (IOException e6) {
                                                e = e6;
                                                file2 = file;
                                                numArgs2 = dbe;
                                                out.append("*** ");
                                                out.append(e.toString());
                                                out.append("\n");
                                                if (numArgs2 != 0) {
                                                }
                                                if (time2 != null) {
                                                }
                                                if (doPrint) {
                                                }
                                                searchArgs = searchArgs2;
                                                numArgs2 = numArgs;
                                                time2 = time;
                                                doFile2 = doFile;
                                                i = numFound;
                                            } catch (Throwable th222) {
                                                file2 = file;
                                                searchArgs = th222;
                                                numArgs2 = dbe;
                                                if (numArgs2 != 0) {
                                                }
                                                if (time2 != null) {
                                                }
                                                throw searchArgs;
                                            }
                                        } catch (IOException e7) {
                                            e = e7;
                                            numFound = searchArgs;
                                            file2 = file;
                                            numArgs2 = dbe;
                                            out.append("*** ");
                                            out.append(e.toString());
                                            out.append("\n");
                                            if (numArgs2 != 0) {
                                                numArgs2.close();
                                            }
                                            if (time2 != null) {
                                                time2.close();
                                            }
                                            if (doPrint) {
                                            }
                                            searchArgs = searchArgs2;
                                            numArgs2 = numArgs;
                                            time2 = time;
                                            doFile2 = doFile;
                                            i = numFound;
                                        } catch (Throwable th2222) {
                                            arrayList = searchArgs;
                                            file2 = file;
                                            searchArgs = th2222;
                                            numArgs2 = dbe;
                                            if (numArgs2 != 0) {
                                                numArgs2.close();
                                            }
                                            if (time2 != null) {
                                                try {
                                                    time2.close();
                                                } catch (IOException e8) {
                                                }
                                            }
                                            throw searchArgs;
                                        }
                                    } catch (IOException e9) {
                                        e = e9;
                                        numFound = searchArgs;
                                        file2 = file;
                                        doFile = doFile2;
                                        obj = 0;
                                        out.append("*** ");
                                        out.append(e.toString());
                                        out.append("\n");
                                        if (numArgs2 != 0) {
                                        }
                                        if (time2 != null) {
                                        }
                                        if (doPrint) {
                                        }
                                        searchArgs = searchArgs2;
                                        numArgs2 = numArgs;
                                        time2 = time;
                                        doFile2 = doFile;
                                        i = numFound;
                                    } catch (Throwable th22222) {
                                        arrayList = searchArgs;
                                        file2 = file;
                                        doFile = doFile2;
                                        obj = 0;
                                        searchArgs = th22222;
                                        if (numArgs2 != 0) {
                                        }
                                        if (time2 != null) {
                                        }
                                        throw searchArgs;
                                    }
                                } else {
                                    numFound = searchArgs;
                                    file2 = file;
                                    doFile = doFile2;
                                }
                                if (doPrint) {
                                    out.append("\n");
                                }
                                searchArgs = searchArgs2;
                                numArgs2 = numArgs;
                                time2 = time;
                                doFile2 = doFile;
                                i = numFound;
                            }
                            numFound = searchArgs;
                            doFile = doFile2;
                            numArgs = numArgs2;
                            time = time2;
                            searchArgs = searchArgs2;
                            numArgs2 = numArgs;
                            time2 = time;
                            doFile2 = doFile;
                            i = numFound;
                        } else {
                            searchArgs = searchArgs2;
                        }
                    }
                    doFile = doFile2;
                    numArgs = numArgs2;
                    time = time2;
                    if (i == 0) {
                        out.append("(No entries found.)\n");
                    }
                    if (strArr == null || strArr.length == 0) {
                        if (!doPrint) {
                            out.append("\n");
                        }
                        out.append("Usage: dumpsys dropbox [--print|--file] [YYYY-mm-dd] [HH:MM:SS] [tag]\n");
                    }
                    printWriter.write(out.toString());
                    return;
                } catch (IOException e10) {
                    IOException iOException = e10;
                    out = new StringBuilder();
                    out.append("Can't initialize: ");
                    out.append(e10);
                    printWriter.println(out.toString());
                    Slog.e(TAG, "Can't init", e10);
                    return;
                }
            }
        }
    }

    private synchronized void init() throws IOException {
        StringBuilder stringBuilder;
        if (!(this.mDropBoxDir.exists() && this.mDropBoxDir.isDirectory())) {
            this.mStatFs = null;
            this.mAllFiles = null;
        }
        if (this.mStatFs == null) {
            if (!this.mDropBoxDir.isDirectory()) {
                if (!this.mDropBoxDir.mkdirs()) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Can't mkdir: ");
                    stringBuilder2.append(this.mDropBoxDir);
                    throw new IOException(stringBuilder2.toString());
                }
            }
            try {
                FileUtils.setPermissions(this.mDropBoxDir, 504, -1, -1);
                this.mStatFs = new StatFs(this.mDropBoxDir.getPath());
                this.mBlockSize = this.mStatFs.getBlockSize();
            } catch (IllegalArgumentException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Can't statfs: ");
                stringBuilder.append(this.mDropBoxDir);
                throw new IOException(stringBuilder.toString());
            }
        }
        if (this.mAllFiles == null) {
            File[] files = this.mDropBoxDir.listFiles();
            if (files != null) {
                this.mAllFiles = new FileList();
                this.mFilesByTag = new ArrayMap();
                for (File file : files) {
                    if (file.getName().endsWith(".tmp")) {
                        String str = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Cleaning temp file: ");
                        stringBuilder3.append(file);
                        Slog.i(str, stringBuilder3.toString());
                        file.delete();
                    } else {
                        EntryFile entry = new EntryFile(file, this.mBlockSize);
                        if (entry.hasFile()) {
                            enrollEntry(entry);
                        }
                    }
                }
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Can't list files: ");
                stringBuilder.append(this.mDropBoxDir);
                throw new IOException(stringBuilder.toString());
            }
        }
    }

    private synchronized void enrollEntry(EntryFile entry) {
        this.mAllFiles.contents.add(entry);
        FileList fileList = this.mAllFiles;
        fileList.blocks += entry.blocks;
        if (entry.hasFile() && entry.blocks > 0) {
            fileList = (FileList) this.mFilesByTag.get(entry.tag);
            if (fileList == null) {
                fileList = new FileList();
                this.mFilesByTag.put(TextUtils.safeIntern(entry.tag), fileList);
            }
            fileList.contents.add(entry);
            fileList.blocks += entry.blocks;
        }
    }

    private synchronized long createEntry(File temp, String tag, int flags) throws IOException {
        long t;
        synchronized (this) {
            long t2 = System.currentTimeMillis();
            SortedSet<EntryFile> tail = this.mAllFiles.contents.tailSet(new EntryFile(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY + t2));
            EntryFile[] future = null;
            if (!tail.isEmpty()) {
                future = (EntryFile[]) tail.toArray(new EntryFile[tail.size()]);
                tail.clear();
            }
            long j = 1;
            if (!this.mAllFiles.contents.isEmpty()) {
                t2 = Math.max(t2, ((EntryFile) this.mAllFiles.contents.last()).timestampMillis + 1);
            }
            SortedSet<EntryFile> tail2;
            if (future != null) {
                int length = future.length;
                int i = 0;
                t = t2;
                while (i < length) {
                    long j2;
                    t2 = future[i];
                    FileList fileList = this.mAllFiles;
                    fileList.blocks -= t2.blocks;
                    fileList = (FileList) this.mFilesByTag.get(t2.tag);
                    if (fileList != null && fileList.contents.remove(t2)) {
                        fileList.blocks -= t2.blocks;
                    }
                    if ((t2.flags & 1) == 0) {
                        long t3 = t + j;
                        tail2 = tail;
                        EntryFile entryFile = r9;
                        EntryFile entryFile2 = new EntryFile(t2.getFile(this.mDropBoxDir), this.mDropBoxDir, t2.tag, t, t2.flags, this.mBlockSize);
                        enrollEntry(entryFile);
                        t = t3;
                        j2 = 1;
                    } else {
                        tail2 = tail;
                        j2 = 1;
                        long t4 = t + 1;
                        enrollEntry(new EntryFile(this.mDropBoxDir, t2.tag, t));
                        t = t4;
                    }
                    i++;
                    j = j2;
                    tail = tail2;
                }
            } else {
                tail2 = tail;
                t = t2;
            }
            if (temp == null) {
                enrollEntry(new EntryFile(this.mDropBoxDir, tag, t));
            } else {
                String str = tag;
                enrollEntry(new EntryFile(temp, this.mDropBoxDir, str, t, flags, this.mBlockSize));
            }
        }
        return t;
    }

    private synchronized long trimToFit() throws IOException {
        int i;
        long j;
        IOException e;
        long j2;
        synchronized (this) {
            int i2;
            int quotaPercent;
            int reservePercent;
            int quotaKb;
            int ageSeconds = Global.getInt(this.mContentResolver, "dropbox_age_seconds", DEFAULT_AGE_SECONDS);
            ContentResolver contentResolver = this.mContentResolver;
            String str = "dropbox_max_files";
            if (ActivityManager.isLowRamDeviceStatic()) {
                i2 = 300;
            } else {
                i2 = 1000;
            }
            this.mMaxFiles = Global.getInt(contentResolver, str, i2);
            long cutoffMillis = System.currentTimeMillis() - ((long) (ageSeconds * 1000));
            while (!this.mAllFiles.contents.isEmpty()) {
                EntryFile entry = (EntryFile) this.mAllFiles.contents.first();
                if (entry.timestampMillis > cutoffMillis && this.mAllFiles.contents.size() < this.mMaxFiles) {
                    break;
                }
                FileList tag = (FileList) this.mFilesByTag.get(entry.tag);
                if (tag != null && tag.contents.remove(entry)) {
                    tag.blocks -= entry.blocks;
                }
                if (this.mAllFiles.contents.remove(entry)) {
                    FileList fileList = this.mAllFiles;
                    fileList.blocks -= entry.blocks;
                }
                entry.deleteFile(this.mDropBoxDir);
            }
            long uptimeMillis = SystemClock.uptimeMillis();
            if (uptimeMillis > this.mCachedQuotaUptimeMillis + 5000) {
                quotaPercent = Global.getInt(this.mContentResolver, "dropbox_quota_percent", 10);
                reservePercent = Global.getInt(this.mContentResolver, "dropbox_reserve_percent", 10);
                if (Log.HWINFO) {
                    reservePercent = 1;
                }
                int reservePercent2 = reservePercent;
                quotaKb = Global.getInt(this.mContentResolver, "dropbox_quota_kb", DEFAULT_QUOTA_KB);
                try {
                    this.mStatFs.restat(this.mDropBoxDir.getPath());
                    this.mCachedQuotaBlocks = Math.min((quotaKb * 1024) / this.mBlockSize, Math.max(0, ((this.mStatFs.getAvailableBlocks() - ((this.mStatFs.getBlockCount() * reservePercent2) / 100)) * quotaPercent) / 100));
                    this.mCachedQuotaUptimeMillis = uptimeMillis;
                } catch (IllegalArgumentException e2) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Can't restat: ");
                    stringBuilder.append(this.mDropBoxDir);
                    throw new IOException(stringBuilder.toString());
                }
            }
            if (this.mAllFiles.blocks > this.mCachedQuotaBlocks) {
                FileList tag2;
                reservePercent = this.mAllFiles.blocks;
                TreeSet<FileList> tags = new TreeSet(this.mFilesByTag.values());
                Iterator it = tags.iterator();
                int squeezed = 0;
                quotaPercent = reservePercent;
                while (it.hasNext()) {
                    tag2 = (FileList) it.next();
                    if (squeezed > 0 && tag2.blocks <= (this.mCachedQuotaBlocks - quotaPercent) / squeezed) {
                        break;
                    }
                    quotaPercent -= tag2.blocks;
                    squeezed++;
                }
                quotaKb = (this.mCachedQuotaBlocks - quotaPercent) / squeezed;
                Iterator it2 = tags.iterator();
                while (it2.hasNext()) {
                    FileList tag3 = (FileList) it2.next();
                    if (this.mAllFiles.blocks < this.mCachedQuotaBlocks) {
                        i = ageSeconds;
                        j = cutoffMillis;
                        break;
                    }
                    while (tag3.blocks > quotaKb && !tag3.contents.isEmpty()) {
                        EntryFile entry2 = (EntryFile) tag3.contents.first();
                        if (tag3.contents.remove(entry2)) {
                            tag3.blocks -= entry2.blocks;
                        }
                        if (this.mAllFiles.contents.remove(entry2)) {
                            tag2 = this.mAllFiles;
                            tag2.blocks -= entry2.blocks;
                        }
                        try {
                            entry2.deleteFile(this.mDropBoxDir);
                            i = ageSeconds;
                            j = cutoffMillis;
                            try {
                                enrollEntry(new EntryFile(this.mDropBoxDir, entry2.tag, entry2.timestampMillis));
                            } catch (IOException e3) {
                                e = e3;
                            }
                        } catch (IOException e4) {
                            e = e4;
                            i = ageSeconds;
                            j = cutoffMillis;
                            Slog.e(TAG, "Can't write tombstone file", e);
                            ageSeconds = i;
                            cutoffMillis = j;
                        }
                        ageSeconds = i;
                        cutoffMillis = j;
                    }
                    ageSeconds = ageSeconds;
                    cutoffMillis = cutoffMillis;
                }
            }
            j = cutoffMillis;
            j2 = (long) (this.mCachedQuotaBlocks * this.mBlockSize);
        }
        return j2;
    }
}
