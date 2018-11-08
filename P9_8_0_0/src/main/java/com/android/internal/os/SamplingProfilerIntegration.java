package com.android.internal.os;

import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.SystemProperties;
import android.provider.SettingsStringUtil;
import android.util.Log;
import com.android.internal.content.NativeLibraryHelper;
import dalvik.system.profiler.BinaryHprofWriter;
import dalvik.system.profiler.SamplingProfiler;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import libcore.io.IoUtils;

public class SamplingProfilerIntegration {
    public static final String SNAPSHOT_DIR = "/data/snapshots";
    private static final String TAG = "SamplingProfilerIntegration";
    private static final boolean enabled;
    private static final AtomicBoolean pending = new AtomicBoolean(false);
    private static SamplingProfiler samplingProfiler;
    private static final int samplingProfilerDepth = SystemProperties.getInt("persist.sys.profiler_depth", 4);
    private static final int samplingProfilerMilliseconds = SystemProperties.getInt("persist.sys.profiler_ms", 0);
    private static final Executor snapshotWriter;
    private static long startMillis;

    static {
        if (samplingProfilerMilliseconds > 0) {
            File dir = new File(SNAPSHOT_DIR);
            dir.mkdirs();
            dir.setWritable(true, false);
            dir.setExecutable(true, false);
            if (dir.isDirectory()) {
                snapshotWriter = Executors.newSingleThreadExecutor(new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        return new Thread(r, SamplingProfilerIntegration.TAG);
                    }
                });
                enabled = true;
                Log.i(TAG, "Profiling enabled. Sampling interval ms: " + samplingProfilerMilliseconds);
                return;
            }
            snapshotWriter = null;
            enabled = true;
            Log.w(TAG, "Profiling setup failed. Could not create /data/snapshots");
            return;
        }
        snapshotWriter = null;
        enabled = false;
        Log.i(TAG, "Profiling disabled.");
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void start() {
        if (!enabled) {
            return;
        }
        if (samplingProfiler != null) {
            Log.e(TAG, "SamplingProfilerIntegration already started at " + new Date(startMillis));
            return;
        }
        samplingProfiler = new SamplingProfiler(samplingProfilerDepth, SamplingProfiler.newThreadGroupThreadSet(Thread.currentThread().getThreadGroup()));
        samplingProfiler.start(samplingProfilerMilliseconds);
        startMillis = System.currentTimeMillis();
    }

    public static void writeSnapshot(final String processName, final PackageInfo packageInfo) {
        if (!enabled) {
            return;
        }
        if (samplingProfiler == null) {
            Log.e(TAG, "SamplingProfilerIntegration is not started");
            return;
        }
        if (pending.compareAndSet(false, true)) {
            snapshotWriter.execute(new Runnable() {
                public void run() {
                    try {
                        SamplingProfilerIntegration.writeSnapshotFile(processName, packageInfo);
                    } finally {
                        SamplingProfilerIntegration.pending.set(false);
                    }
                }
            });
        }
    }

    public static void writeZygoteSnapshot() {
        if (enabled) {
            writeSnapshotFile("zygote", null);
            samplingProfiler.shutdown();
            samplingProfiler = null;
            startMillis = 0;
        }
    }

    private static void writeSnapshotFile(String processName, PackageInfo packageInfo) {
        IOException e;
        Throwable th;
        if (enabled) {
            samplingProfiler.stop();
            String name = processName.replaceAll(SettingsStringUtil.DELIMITER, ".");
            String path = "/data/snapshots/" + name + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + startMillis + ".snapshot";
            long start = System.currentTimeMillis();
            AutoCloseable autoCloseable = null;
            try {
                OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(path));
                try {
                    PrintStream out = new PrintStream(outputStream);
                    generateSnapshotHeader(name, packageInfo, out);
                    if (out.checkError()) {
                        throw new IOException();
                    }
                    BinaryHprofWriter.write(samplingProfiler.getHprofData(), outputStream);
                    IoUtils.closeQuietly(outputStream);
                    new File(path).setReadable(true, false);
                    Log.i(TAG, "Wrote snapshot " + path + " in " + (System.currentTimeMillis() - start) + "ms.");
                    samplingProfiler.start(samplingProfilerMilliseconds);
                } catch (IOException e2) {
                    e = e2;
                    autoCloseable = outputStream;
                    try {
                        Log.e(TAG, "Error writing snapshot to " + path, e);
                        IoUtils.closeQuietly(autoCloseable);
                    } catch (Throwable th2) {
                        th = th2;
                        IoUtils.closeQuietly(autoCloseable);
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    Object outputStream2 = outputStream;
                    IoUtils.closeQuietly(autoCloseable);
                    throw th;
                }
            } catch (IOException e3) {
                e = e3;
                Log.e(TAG, "Error writing snapshot to " + path, e);
                IoUtils.closeQuietly(autoCloseable);
            }
        }
    }

    private static void generateSnapshotHeader(String processName, PackageInfo packageInfo, PrintStream out) {
        out.println("Version: 3");
        out.println("Process: " + processName);
        if (packageInfo != null) {
            out.println("Package: " + packageInfo.packageName);
            out.println("Package-Version: " + packageInfo.versionCode);
        }
        out.println("Build: " + Build.FINGERPRINT);
        out.println();
    }
}
