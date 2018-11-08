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
import com.android.internal.os.IDropBoxManagerService;
import com.android.internal.os.IDropBoxManagerService.Stub;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.ObjectUtils;
import com.android.server.job.controllers.JobStatus;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
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
            if ((flags & 1) != 0) {
                throw new IllegalArgumentException();
            }
            this.tag = TextUtils.safeIntern(tag);
            this.timestampMillis = timestampMillis;
            this.flags = flags;
            File file = getFile(dir);
            if (temp.renameTo(file)) {
                this.blocks = (int) (((file.length() + ((long) blockSize)) - 1) / ((long) blockSize));
                return;
            }
            throw new IOException("Can't rename " + temp + " to " + file);
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
                if (name.endsWith(".gz")) {
                    flags = 4;
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
                Slog.wtf(DropBoxManagerService.TAG, "Invalid filename: " + file);
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
            return ((this.flags & 2) != 0 ? ".txt" : ".dat") + ((this.flags & 4) != 0 ? ".gz" : "");
        }

        public String getFilename() {
            return hasFile() ? Uri.encode(this.tag) + "@" + this.timestampMillis + getExtension() : null;
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
        switch (phase) {
            case 500:
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.DEVICE_STORAGE_LOW");
                getContext().registerReceiver(this.mReceiver, filter);
                this.mContentResolver.registerContentObserver(Global.CONTENT_URI, true, new ContentObserver(new Handler()) {
                    public void onChange(boolean selfChange) {
                        DropBoxManagerService.this.mReceiver.onReceive(DropBoxManagerService.this.getContext(), (Intent) null);
                    }
                });
                return;
            case 1000:
                this.mBooted = true;
                return;
            default:
                return;
        }
    }

    public IDropBoxManagerService getServiceStub() {
        return this.mStub;
    }

    public void add(Entry entry) {
        IOException e;
        Throwable th;
        File temp = null;
        AutoCloseable input = null;
        AutoCloseable autoCloseable = null;
        String tag = entry.getTag();
        try {
            int flags = entry.getFlags();
            if ((flags & 1) != 0) {
                throw new IllegalArgumentException();
            }
            init();
            if (isTagEnabled(tag)) {
                long max = trimToFit();
                long lastTrim = System.currentTimeMillis();
                byte[] buffer = new byte[this.mBlockSize];
                input = entry.getInputStream();
                int read = 0;
                while (read < buffer.length) {
                    int n = input.read(buffer, read, buffer.length - read);
                    if (n <= 0) {
                        break;
                    }
                    read += n;
                }
                File file = new File(this.mDropBoxDir, "drop" + Thread.currentThread().getId() + ".tmp");
                try {
                    int bufferSize = this.mBlockSize;
                    if (bufferSize > 4096) {
                        bufferSize = 4096;
                    }
                    if (bufferSize < 512) {
                        bufferSize = 512;
                    }
                    FileOutputStream foutput = new FileOutputStream(file);
                    OutputStream bufferedOutputStream = new BufferedOutputStream(foutput, bufferSize);
                    Object output;
                    try {
                        if (read == buffer.length && (flags & 4) == 0) {
                            autoCloseable = new GZIPOutputStream(bufferedOutputStream);
                            flags |= 4;
                        } else {
                            output = bufferedOutputStream;
                        }
                        do {
                            autoCloseable.write(buffer, 0, read);
                            long now = System.currentTimeMillis();
                            if (now - lastTrim > 30000) {
                                max = trimToFit();
                                lastTrim = now;
                            }
                            read = input.read(buffer);
                            if (read <= 0) {
                                FileUtils.sync(foutput);
                                autoCloseable.close();
                                autoCloseable = null;
                            } else {
                                autoCloseable.flush();
                            }
                            if (file.length() > max) {
                                Slog.w(TAG, "Dropping: " + tag + " (" + file.length() + " > " + max + " bytes)");
                                file.delete();
                                temp = null;
                                break;
                            }
                        } while (read > 0);
                        temp = file;
                        long time = createEntry(temp, tag, flags);
                        Intent dropboxIntent = new Intent("android.intent.action.DROPBOX_ENTRY_ADDED");
                        dropboxIntent.putExtra("tag", tag);
                        dropboxIntent.putExtra("time", time);
                        if (!this.mBooted) {
                            dropboxIntent.addFlags(1073741824);
                        }
                        this.mHandler.sendMessage(this.mHandler.obtainMessage(1, dropboxIntent));
                        IoUtils.closeQuietly(autoCloseable);
                        IoUtils.closeQuietly(input);
                        entry.close();
                    } catch (IOException e2) {
                        e = e2;
                        output = bufferedOutputStream;
                        temp = file;
                        try {
                            Slog.e(TAG, "Can't write: " + tag, e);
                            IoUtils.closeQuietly(autoCloseable);
                            IoUtils.closeQuietly(input);
                            entry.close();
                            if (temp != null) {
                                temp.delete();
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            IoUtils.closeQuietly(autoCloseable);
                            IoUtils.closeQuietly(input);
                            entry.close();
                            if (temp != null) {
                                temp.delete();
                            }
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        output = bufferedOutputStream;
                        temp = file;
                        IoUtils.closeQuietly(autoCloseable);
                        IoUtils.closeQuietly(input);
                        entry.close();
                        if (temp != null) {
                            temp.delete();
                        }
                        throw th;
                    }
                } catch (IOException e3) {
                    e = e3;
                    temp = file;
                } catch (Throwable th4) {
                    th = th4;
                    temp = file;
                }
            }
            IoUtils.closeQuietly(null);
            IoUtils.closeQuietly(null);
            entry.close();
        } catch (IOException e4) {
            e = e4;
            Slog.e(TAG, "Can't write: " + tag, e);
            IoUtils.closeQuietly(autoCloseable);
            IoUtils.closeQuietly(input);
            entry.close();
            if (temp != null) {
                temp.delete();
            }
        }
    }

    public boolean isTagEnabled(String tag) {
        boolean equals;
        long token = Binder.clearCallingIdentity();
        try {
            equals = "disabled".equals(Global.getString(this.mContentResolver, "dropbox:" + tag)) ^ 1;
            return equals;
        } catch (RuntimeException e) {
            equals = TAG;
            Slog.e(equals, "Failure getting tag enabled", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public synchronized Entry getNextEntry(String tag, long millis) {
        if (getContext().checkCallingOrSelfPermission("android.permission.READ_LOGS") != 0) {
            throw new SecurityException("READ_LOGS permission required");
        }
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
                        Slog.wtf(TAG, "Can't read: " + file, e);
                    }
                }
            }
            return null;
        } catch (IOException e2) {
            Slog.e(TAG, "Can't init", e2);
            return null;
        }
    }

    public synchronized void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        InputStreamReader inputStreamReader;
        IOException e;
        Throwable th;
        Entry dbe;
        if (DumpUtils.checkDumpAndUsageStatsPermission(getContext(), TAG, pw)) {
            try {
                init();
                StringBuilder out = new StringBuilder();
                boolean doPrint = false;
                boolean doFile = false;
                ArrayList<String> searchArgs = new ArrayList();
                int i = 0;
                while (args != null && i < args.length) {
                    if (args[i].equals("-p") || args[i].equals("--print")) {
                        doPrint = true;
                    } else if (args[i].equals("-f") || args[i].equals("--file")) {
                        doFile = true;
                    } else if (args[i].equals("-h") || args[i].equals("--help")) {
                        pw.println("Dropbox (dropbox) dump options:");
                        pw.println("  [-h|--help] [-p|--print] [-f|--file] [timestamp]");
                        pw.println("    -h|--help: print this help");
                        pw.println("    -p|--print: print full contents of each entry");
                        pw.println("    -f|--file: print path of each entry's file");
                        pw.println("  [timestamp] optionally filters to only those entries.");
                        return;
                    } else if (args[i].startsWith("-")) {
                        out.append("Unknown argument: ").append(args[i]).append("\n");
                    } else {
                        searchArgs.add(args[i]);
                    }
                    i++;
                }
                out.append("Drop box contents: ").append(this.mAllFiles.contents.size()).append(" entries\n");
                out.append("Max entries: ").append(this.mMaxFiles).append("\n");
                if (!searchArgs.isEmpty()) {
                    out.append("Searching for:");
                    for (String a : searchArgs) {
                        out.append(" ").append(a);
                    }
                    out.append("\n");
                }
                int numFound = 0;
                int numArgs = searchArgs.size();
                Time time = new Time();
                out.append("\n");
                for (EntryFile entry : this.mAllFiles.contents) {
                    time.set(entry.timestampMillis);
                    String date = time.format("%Y-%m-%d %H:%M:%S");
                    boolean match = true;
                    for (i = 0; i < numArgs && match; i++) {
                        String arg = (String) searchArgs.get(i);
                        match = !date.contains(arg) ? arg.equals(entry.tag) : true;
                    }
                    if (match) {
                        numFound++;
                        if (doPrint) {
                            out.append("========================================\n");
                        }
                        out.append(date).append(" ").append(entry.tag == null ? "(no tag)" : entry.tag);
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
                            out.append(", ").append(file.length()).append(" bytes)\n");
                            if (doFile || (doPrint && (entry.flags & 2) == 0)) {
                                if (!doPrint) {
                                    out.append("    ");
                                }
                                out.append(file.getPath()).append("\n");
                            }
                            if ((entry.flags & 2) != 0 && (doPrint || (doFile ^ 1) != 0)) {
                                inputStreamReader = null;
                                try {
                                    dbe = new Entry(entry.tag, entry.timestampMillis, file, entry.flags);
                                    if (doPrint) {
                                        try {
                                            InputStreamReader inputStreamReader2 = new InputStreamReader(dbe.getInputStream());
                                            try {
                                                char[] buf = new char[4096];
                                                boolean newline = false;
                                                while (true) {
                                                    int n = inputStreamReader2.read(buf);
                                                    if (n <= 0) {
                                                        break;
                                                    }
                                                    out.append(buf, 0, n);
                                                    newline = buf[n + -1] == '\n';
                                                    if (out.length() > 65536) {
                                                        pw.write(out.toString());
                                                        out.setLength(0);
                                                    }
                                                }
                                                if (newline) {
                                                    inputStreamReader = inputStreamReader2;
                                                } else {
                                                    out.append("\n");
                                                    inputStreamReader = inputStreamReader2;
                                                }
                                            } catch (IOException e2) {
                                                e = e2;
                                                inputStreamReader = inputStreamReader2;
                                            } catch (Throwable th2) {
                                                th = th2;
                                                inputStreamReader = inputStreamReader2;
                                            }
                                        } catch (IOException e3) {
                                            e = e3;
                                            try {
                                                out.append("*** ").append(e.toString()).append("\n");
                                                if (dbe != null) {
                                                    dbe.close();
                                                }
                                                if (inputStreamReader != null) {
                                                    try {
                                                        inputStreamReader.close();
                                                    } catch (IOException e4) {
                                                    }
                                                }
                                                if (doPrint) {
                                                    continue;
                                                } else {
                                                    out.append("\n");
                                                }
                                            } catch (Throwable th3) {
                                                th = th3;
                                            }
                                        }
                                    } else {
                                        String text = dbe.getText(70);
                                        out.append("    ");
                                        if (text == null) {
                                            out.append("[null]");
                                        } else {
                                            boolean truncated = text.length() == 70;
                                            out.append(text.trim().replace('\n', '/'));
                                            if (truncated) {
                                                out.append(" ...");
                                            }
                                        }
                                        out.append("\n");
                                    }
                                    if (dbe != null) {
                                        dbe.close();
                                    }
                                    if (inputStreamReader != null) {
                                        try {
                                            inputStreamReader.close();
                                        } catch (IOException e5) {
                                        }
                                    }
                                } catch (IOException e6) {
                                    e = e6;
                                    dbe = null;
                                    out.append("*** ").append(e.toString()).append("\n");
                                    if (dbe != null) {
                                        dbe.close();
                                    }
                                    if (inputStreamReader != null) {
                                        inputStreamReader.close();
                                    }
                                    if (doPrint) {
                                        out.append("\n");
                                    } else {
                                        continue;
                                    }
                                } catch (Throwable th4) {
                                    th = th4;
                                    dbe = null;
                                }
                            }
                            if (doPrint) {
                                out.append("\n");
                            } else {
                                continue;
                            }
                        }
                    }
                }
                if (numFound == 0) {
                    out.append("(No entries found.)\n");
                }
                if (args == null || args.length == 0) {
                    if (!doPrint) {
                        out.append("\n");
                    }
                    out.append("Usage: dumpsys dropbox [--print|--file] [YYYY-mm-dd] [HH:MM:SS] [tag]\n");
                }
                pw.write(out.toString());
                return;
            } catch (Throwable e7) {
                pw.println("Can't initialize: " + e7);
                Slog.e(TAG, "Can't init", e7);
                return;
            }
        }
        return;
        if (dbe != null) {
            dbe.close();
        }
        if (inputStreamReader != null) {
            try {
                inputStreamReader.close();
            } catch (IOException e8) {
            }
        }
        throw th;
        throw th;
    }

    private synchronized void init() throws IOException {
        if (!(this.mDropBoxDir.exists() && (this.mDropBoxDir.isDirectory() ^ 1) == 0)) {
            this.mStatFs = null;
            this.mAllFiles = null;
        }
        if (this.mStatFs == null) {
            if (this.mDropBoxDir.isDirectory() || (this.mDropBoxDir.mkdirs() ^ 1) == 0) {
                try {
                    this.mStatFs = new StatFs(this.mDropBoxDir.getPath());
                    this.mBlockSize = this.mStatFs.getBlockSize();
                } catch (IllegalArgumentException e) {
                    throw new IOException("Can't statfs: " + this.mDropBoxDir);
                }
            }
            throw new IOException("Can't mkdir: " + this.mDropBoxDir);
        }
        if (this.mAllFiles == null) {
            File[] files = this.mDropBoxDir.listFiles();
            if (files == null) {
                throw new IOException("Can't list files: " + this.mDropBoxDir);
            }
            this.mAllFiles = new FileList();
            this.mFilesByTag = new ArrayMap();
            for (File file : files) {
                if (file.getName().endsWith(".tmp")) {
                    Slog.i(TAG, "Cleaning temp file: " + file);
                    file.delete();
                } else {
                    EntryFile entry = new EntryFile(file, this.mBlockSize);
                    if (entry.hasFile()) {
                        enrollEntry(entry);
                    }
                }
            }
        }
    }

    private synchronized void enrollEntry(EntryFile entry) {
        this.mAllFiles.contents.add(entry);
        FileList fileList = this.mAllFiles;
        fileList.blocks += entry.blocks;
        if (entry.hasFile() && entry.blocks > 0) {
            FileList tagFiles = (FileList) this.mFilesByTag.get(entry.tag);
            if (tagFiles == null) {
                tagFiles = new FileList();
                this.mFilesByTag.put(TextUtils.safeIntern(entry.tag), tagFiles);
            }
            tagFiles.contents.add(entry);
            tagFiles.blocks += entry.blocks;
        }
    }

    private synchronized long createEntry(File temp, String tag, int flags) throws IOException {
        long t;
        t = System.currentTimeMillis();
        SortedSet<EntryFile> tail = this.mAllFiles.contents.tailSet(new EntryFile(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY + t));
        EntryFile[] entryFileArr = null;
        if (!tail.isEmpty()) {
            entryFileArr = (EntryFile[]) tail.toArray(new EntryFile[tail.size()]);
            tail.clear();
        }
        if (!this.mAllFiles.contents.isEmpty()) {
            t = Math.max(t, ((EntryFile) this.mAllFiles.contents.last()).timestampMillis + 1);
        }
        if (entryFileArr != null) {
            for (EntryFile late : entryFileArr) {
                FileList fileList = this.mAllFiles;
                fileList.blocks -= late.blocks;
                FileList tagFiles = (FileList) this.mFilesByTag.get(late.tag);
                if (tagFiles != null && tagFiles.contents.remove(late)) {
                    tagFiles.blocks -= late.blocks;
                }
                long t2;
                if ((late.flags & 1) == 0) {
                    t2 = t + 1;
                    enrollEntry(new EntryFile(late.getFile(this.mDropBoxDir), this.mDropBoxDir, late.tag, t, late.flags, this.mBlockSize));
                    t = t2;
                } else {
                    t2 = t + 1;
                    enrollEntry(new EntryFile(this.mDropBoxDir, late.tag, t));
                    t = t2;
                }
            }
        }
        if (temp == null) {
            enrollEntry(new EntryFile(this.mDropBoxDir, tag, t));
        } else {
            enrollEntry(new EntryFile(temp, this.mDropBoxDir, tag, t, flags, this.mBlockSize));
        }
        return t;
    }

    private synchronized long trimToFit() throws IOException {
        int ageSeconds = Global.getInt(this.mContentResolver, "dropbox_age_seconds", DEFAULT_AGE_SECONDS);
        this.mMaxFiles = Global.getInt(this.mContentResolver, "dropbox_max_files", ActivityManager.isLowRamDeviceStatic() ? 300 : 1000);
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
            int quotaPercent = Global.getInt(this.mContentResolver, "dropbox_quota_percent", 10);
            int reservePercent = Global.getInt(this.mContentResolver, "dropbox_reserve_percent", 10);
            if (Log.HWINFO) {
                reservePercent = 1;
            }
            int quotaKb = Global.getInt(this.mContentResolver, "dropbox_quota_kb", DEFAULT_QUOTA_KB);
            try {
                this.mStatFs.restat(this.mDropBoxDir.getPath());
                this.mCachedQuotaBlocks = Math.min((quotaKb * 1024) / this.mBlockSize, Math.max(0, ((this.mStatFs.getAvailableBlocks() - ((this.mStatFs.getBlockCount() * reservePercent) / 100)) * quotaPercent) / 100));
                this.mCachedQuotaUptimeMillis = uptimeMillis;
            } catch (IllegalArgumentException e) {
                throw new IOException("Can't restat: " + this.mDropBoxDir);
            }
        }
        if (this.mAllFiles.blocks > this.mCachedQuotaBlocks) {
            int unsqueezed = this.mAllFiles.blocks;
            int squeezed = 0;
            TreeSet<FileList> treeSet = new TreeSet(this.mFilesByTag.values());
            for (FileList tag2 : treeSet) {
                if (squeezed > 0 && tag2.blocks <= (this.mCachedQuotaBlocks - unsqueezed) / squeezed) {
                    break;
                }
                unsqueezed -= tag2.blocks;
                squeezed++;
            }
            int tagQuota = (this.mCachedQuotaBlocks - unsqueezed) / squeezed;
            for (FileList tag22 : treeSet) {
                if (this.mAllFiles.blocks < this.mCachedQuotaBlocks) {
                    break;
                }
                while (tag22.blocks > tagQuota && (tag22.contents.isEmpty() ^ 1) != 0) {
                    entry = (EntryFile) tag22.contents.first();
                    if (tag22.contents.remove(entry)) {
                        tag22.blocks -= entry.blocks;
                    }
                    if (this.mAllFiles.contents.remove(entry)) {
                        fileList = this.mAllFiles;
                        fileList.blocks -= entry.blocks;
                    }
                    try {
                        entry.deleteFile(this.mDropBoxDir);
                        enrollEntry(new EntryFile(this.mDropBoxDir, entry.tag, entry.timestampMillis));
                    } catch (IOException e2) {
                        Slog.e(TAG, "Can't write tombstone file", e2);
                    }
                }
            }
        }
        return (long) (this.mCachedQuotaBlocks * this.mBlockSize);
    }
}
