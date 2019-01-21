package com.android.internal.os;

import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.SystemClock;
import android.text.TextUtils.SimpleStringSplitter;
import android.util.LongSparseLongArray;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class KernelMemoryBandwidthStats {
    private static final boolean DEBUG = false;
    private static final String TAG = "KernelMemoryBandwidthStats";
    private static final String mSysfsFile = "/sys/kernel/memory_state_time/show_stat";
    protected final LongSparseLongArray mBandwidthEntries = new LongSparseLongArray();
    private boolean mStatsDoNotExist = false;

    public void updateStats() {
        if (!this.mStatsDoNotExist) {
            long startTime = SystemClock.uptimeMillis();
            ThreadPolicy policy = StrictMode.allowThreadDiskReads();
            BufferedReader reader;
            try {
                reader = new BufferedReader(new FileReader(mSysfsFile));
                parseStats(reader);
                reader.close();
            } catch (FileNotFoundException e) {
                Slog.w(TAG, "No kernel memory bandwidth stats available");
                this.mBandwidthEntries.clear();
                this.mStatsDoNotExist = true;
            } catch (IOException e2) {
                try {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to read memory bandwidth: ");
                    stringBuilder.append(e2.getMessage());
                    Slog.e(str, stringBuilder.toString());
                    this.mBandwidthEntries.clear();
                } catch (Throwable th) {
                    StrictMode.setThreadPolicy(policy);
                }
            } catch (Throwable th2) {
                r4.addSuppressed(th2);
            }
            StrictMode.setThreadPolicy(policy);
            long readTime = SystemClock.uptimeMillis() - startTime;
            if (readTime > 100) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Reading memory bandwidth file took ");
                stringBuilder2.append(readTime);
                stringBuilder2.append("ms");
                Slog.w(str2, stringBuilder2.toString());
            }
        }
    }

    @VisibleForTesting
    public void parseStats(BufferedReader reader) throws IOException {
        SimpleStringSplitter splitter = new SimpleStringSplitter(' ');
        this.mBandwidthEntries.clear();
        while (true) {
            String readLine = reader.readLine();
            String line = readLine;
            if (readLine != null) {
                splitter.setString(line);
                splitter.next();
                int bandwidth = 0;
                do {
                    int indexOfKey = this.mBandwidthEntries.indexOfKey((long) bandwidth);
                    int index = indexOfKey;
                    if (indexOfKey >= 0) {
                        this.mBandwidthEntries.put((long) bandwidth, this.mBandwidthEntries.valueAt(index) + (Long.parseLong(splitter.next()) / 1000000));
                    } else {
                        this.mBandwidthEntries.put((long) bandwidth, Long.parseLong(splitter.next()) / 1000000);
                    }
                    bandwidth++;
                } while (splitter.hasNext());
            } else {
                return;
            }
        }
    }

    public LongSparseLongArray getBandwidthEntries() {
        return this.mBandwidthEntries;
    }
}
