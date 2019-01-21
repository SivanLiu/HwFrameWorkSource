package com.android.internal.os;

import android.common.HwFrameworkFactory;
import android.common.HwFrameworkMonitor;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils.SimpleStringSplitter;
import android.util.Slog;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class KernelCpuSpeedReader {
    private static final String TAG = "KernelCpuSpeedReader";
    private final int mCpuNumber;
    private final long[] mDeltaSpeedTimesMs;
    private final long mJiffyMillis;
    private final long[] mLastSpeedTimesMs;
    private HwFrameworkMonitor mMonitor = null;
    private final int mNumSpeedSteps;
    private final String mProcFile;

    public KernelCpuSpeedReader(int cpuNumber, int numSpeedSteps) {
        this.mProcFile = String.format("/sys/devices/system/cpu/cpu%d/cpufreq/stats/time_in_state", new Object[]{Integer.valueOf(cpuNumber)});
        this.mNumSpeedSteps = numSpeedSteps;
        this.mCpuNumber = cpuNumber;
        this.mMonitor = HwFrameworkFactory.getHwFrameworkMonitor();
        this.mLastSpeedTimesMs = new long[numSpeedSteps];
        this.mDeltaSpeedTimesMs = new long[numSpeedSteps];
        this.mJiffyMillis = 1000 / Os.sysconf(OsConstants._SC_CLK_TCK);
    }

    public long[] readDelta() {
        ThreadPolicy policy = StrictMode.allowThreadDiskReads();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(this.mProcFile));
            SimpleStringSplitter splitter = new SimpleStringSplitter(' ');
            int speedIndex = 0;
            while (speedIndex < this.mLastSpeedTimesMs.length) {
                String readLine = reader.readLine();
                String line = readLine;
                if (readLine != null) {
                    splitter.setString(line);
                    readLine = splitter.next();
                    String cpuTime = splitter.next();
                    try {
                        Long.parseLong(readLine);
                        long time = Long.parseLong(cpuTime) * this.mJiffyMillis;
                        if (time < this.mLastSpeedTimesMs[speedIndex]) {
                            this.mDeltaSpeedTimesMs[speedIndex] = time;
                        } else {
                            this.mDeltaSpeedTimesMs[speedIndex] = time - this.mLastSpeedTimesMs[speedIndex];
                        }
                        this.mLastSpeedTimesMs[speedIndex] = time;
                    } catch (NumberFormatException ex) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to parse freq-time[");
                        stringBuilder.append(line);
                        stringBuilder.append("] for ");
                        stringBuilder.append(ex.getMessage());
                        Slog.e(str, stringBuilder.toString());
                        Bundle data = new Bundle();
                        data.putString("cpuState", readLine);
                        data.putString("cpuTime", cpuTime);
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("cpu number:");
                        stringBuilder2.append(this.mCpuNumber);
                        data.putString("extra", stringBuilder2.toString());
                        if (this.mMonitor != null) {
                            this.mMonitor.monitor(907400016, data);
                        }
                    }
                    speedIndex++;
                }
            }
            $closeResource(null, reader);
        } catch (IOException e) {
            try {
                String str2 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Failed to read cpu-freq: ");
                stringBuilder3.append(e.getMessage());
                Slog.e(str2, stringBuilder3.toString());
                Arrays.fill(this.mDeltaSpeedTimesMs, 0);
            } catch (Throwable th) {
                StrictMode.setThreadPolicy(policy);
            }
        } catch (Throwable th2) {
            $closeResource(r2, reader);
        }
        StrictMode.setThreadPolicy(policy);
        return this.mDeltaSpeedTimesMs;
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

    public long[] readAbsolute() {
        ThreadPolicy policy = StrictMode.allowThreadDiskReads();
        long[] speedTimeMs = new long[this.mNumSpeedSteps];
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(this.mProcFile));
            SimpleStringSplitter splitter = new SimpleStringSplitter(' ');
            int speedIndex = 0;
            while (speedIndex < this.mNumSpeedSteps) {
                String readLine = reader.readLine();
                String line = readLine;
                if (readLine != null) {
                    splitter.setString(line);
                    splitter.next();
                    speedTimeMs[speedIndex] = Long.parseLong(splitter.next()) * this.mJiffyMillis;
                    speedIndex++;
                }
            }
            $closeResource(null, reader);
        } catch (IOException e) {
            try {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to read cpu-freq: ");
                stringBuilder.append(e.getMessage());
                Slog.e(str, stringBuilder.toString());
                Arrays.fill(speedTimeMs, 0);
            } catch (Throwable th) {
                StrictMode.setThreadPolicy(policy);
            }
        } catch (Throwable th2) {
            $closeResource(r3, reader);
        }
        StrictMode.setThreadPolicy(policy);
        return speedTimeMs;
    }
}
