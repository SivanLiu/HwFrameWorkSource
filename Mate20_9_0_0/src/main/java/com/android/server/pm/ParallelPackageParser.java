package com.android.server.pm;

import android.content.pm.PackageParser;
import android.content.pm.PackageParser.Callback;
import android.content.pm.PackageParser.Package;
import android.content.pm.PackageParser.PackageParserException;
import android.os.Trace;
import android.util.DisplayMetrics;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ConcurrentUtils;
import java.io.File;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

class ParallelPackageParser implements AutoCloseable {
    private static final int MAX_THREADS = 4;
    private static final int QUEUE_CAPACITY = 10;
    private final File mCacheDir;
    private volatile String mInterruptedInThread;
    private final DisplayMetrics mMetrics;
    private final boolean mOnlyCore;
    private final Callback mPackageParserCallback;
    private final BlockingQueue<ParseResult> mQueue = new ArrayBlockingQueue(10);
    private final String[] mSeparateProcesses;
    private final ExecutorService mService = ConcurrentUtils.newFixedThreadPool(4, "package-parsing-thread", -2);

    static class ParseResult {
        Package pkg;
        File scanFile;
        Throwable throwable;

        ParseResult() {
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ParseResult{pkg=");
            stringBuilder.append(this.pkg);
            stringBuilder.append(", scanFile=");
            stringBuilder.append(this.scanFile);
            stringBuilder.append(", throwable=");
            stringBuilder.append(this.throwable);
            stringBuilder.append('}');
            return stringBuilder.toString();
        }
    }

    ParallelPackageParser(String[] separateProcesses, boolean onlyCoreApps, DisplayMetrics metrics, File cacheDir, Callback callback) {
        this.mSeparateProcesses = separateProcesses;
        this.mOnlyCore = onlyCoreApps;
        this.mMetrics = metrics;
        this.mCacheDir = cacheDir;
        this.mPackageParserCallback = callback;
    }

    public ParseResult take() {
        try {
            if (this.mInterruptedInThread == null) {
                return (ParseResult) this.mQueue.take();
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Interrupted in ");
            stringBuilder.append(this.mInterruptedInThread);
            throw new InterruptedException(stringBuilder.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    public void submit(File scanFile, int parseFlags) {
        this.mService.submit(new -$$Lambda$ParallelPackageParser$FTtinPrp068lVeI7K6bC1tNE3iM(this, scanFile, parseFlags));
    }

    public static /* synthetic */ void lambda$submit$0(ParallelPackageParser parallelPackageParser, File scanFile, int parseFlags) {
        ParseResult pr = new ParseResult();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("parallel parsePackage [");
        stringBuilder.append(scanFile);
        stringBuilder.append("]");
        Trace.traceBegin(262144, stringBuilder.toString());
        try {
            PackageParser pp = new PackageParser();
            pp.setSeparateProcesses(parallelPackageParser.mSeparateProcesses);
            pp.setOnlyCoreApps(parallelPackageParser.mOnlyCore);
            pp.setDisplayMetrics(parallelPackageParser.mMetrics);
            pp.setCacheDir(parallelPackageParser.mCacheDir);
            pp.setCallback(parallelPackageParser.mPackageParserCallback);
            pr.scanFile = scanFile;
            pr.pkg = parallelPackageParser.parsePackage(pp, scanFile, parseFlags);
        } catch (Throwable th) {
            Trace.traceEnd(262144);
        }
        Trace.traceEnd(262144);
        try {
            parallelPackageParser.mQueue.put(pr);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            parallelPackageParser.mInterruptedInThread = Thread.currentThread().getName();
        }
    }

    @VisibleForTesting
    protected Package parsePackage(PackageParser packageParser, File scanFile, int parseFlags) throws PackageParserException {
        return packageParser.parsePackage(scanFile, parseFlags, true);
    }

    public void close() {
        List<Runnable> unfinishedTasks = this.mService.shutdownNow();
        if (!unfinishedTasks.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Not all tasks finished before calling close: ");
            stringBuilder.append(unfinishedTasks);
            throw new IllegalStateException(stringBuilder.toString());
        }
    }
}
