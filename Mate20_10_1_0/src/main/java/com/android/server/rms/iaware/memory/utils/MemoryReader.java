package com.android.server.rms.iaware.memory.utils;

import android.os.Process;
import android.rms.iaware.AwareLog;
import com.android.server.rms.collector.MemInfoReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MemoryReader {
    private static final int BACKUP_APP_ADJ = 300;
    private static final int CACHED_APP_MAX_ADJ = 999;
    private static final int CACHED_APP_MIN_ADJ = 900;
    private static final String CAN_KERNEL_COMPRESS = "/sys/kernel/rcc/can_compress";
    private static final int DEFAULT_PARSE_MEMDATA_STRINGBUILDER_SIZE = 256;
    private static final int DEFULAT_SHIFT = 2;
    private static final int FOREGROUND_APP_ADJ = 0;
    private static int INVALID_VALUE = -1;
    private static final String LMK_KILL_COUNT = "/sys/module/lowmemorykiller/parameters/kill_count";
    private static final int LMK_MINFREE_SIZE = 6;
    private static final Object LOCK = new Object();
    private static final int MAX_TOPN_LINE_LEN = 100;
    private static final String MEMINFO_ALLOC_COUNT = "/sys/kernel/debug/slowpath_count";
    private static final int PARSE_ALLOC_FAILED = 9;
    private static final int PARSE_COMPACT_STALL = 7;
    private static final int PARSE_COMPACT_SUCESS = 8;
    private static final int PARSE_FILE_CACHE_HIT = 11;
    private static final int PARSE_FILE_CACHE_MISS = 13;
    private static final int PARSE_SLOWPATH_FOUR_ORDER = 6;
    private static final int PARSE_SLOWPATH_ONE_ORDER = 3;
    private static final int PARSE_SLOWPATH_THREE_ORDER = 5;
    private static final int PARSE_SLOWPATH_TWO_ORDER = 4;
    private static final int PARSE_SLOWPATH_ZERO_ORDER = 2;
    private static final int PERCEPTIBLE_APP_ADJ = 200;
    private static final int PREVIOUS_APP_ADJ = 700;
    private static final String PROCESS_MEM = "/proc/driver/process_mem";
    private static final int[] PROCESS_STATM_FORMAT = {32, 8224, 8224, 32, 32, 32, 32};
    private static final int PROCESS_STATM_SHARED = 1;
    private static final int PROCESS_STATM_VSS = 0;
    private static final int PROCESS_SWAPS_TOTAL = 0;
    private static final int PROCESS_SWAPS_USED = 1;
    private static final int PROC_COLON_TERM = 58;
    private static final int PROC_FILE_ARGS_LENGTH = 2;
    private static final int PROC_LINE_TERM = 10;
    private static final int[] PROC_MEMINFO_FORMAT = {PROC_COLON_TERM, 8250, 8250};
    private static final String PROC_MEMINFO_NAME = "/proc/meminfo_lite";
    private static final int[] PROC_MMONITOR_FORMAT = {8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224};
    private static final String PROC_MMONITOR_NAME = "/proc/mmonitor";
    private static final int[] PROC_SWAPS_FORMAT = {266, 265, 8201, 8201};
    private static final String PROC_SWAPS_NAME = "/proc/swaps";
    private static final int SERVICE_B_ADJ = 800;
    private static final String TAG = "AwareMem_MemReader";
    private static final int TOPN_INIT_LEN = 1000;
    private static final int VISIBLE_APP_ADJ = 100;
    private static MemoryReader sReader;
    private long mMemAvailable = 0;
    private long mMemFree = 0;
    private MemInfoReader mMemInfo = new MemInfoReader();
    private long mMemTotal = 0;

    private MemoryReader() {
    }

    public static MemoryReader getInstance() {
        MemoryReader memoryReader;
        synchronized (LOCK) {
            if (sReader == null) {
                sReader = new MemoryReader();
            }
            memoryReader = sReader;
        }
        return memoryReader;
    }

    public long getTotalRam() {
        long j;
        synchronized (LOCK) {
            if (this.mMemTotal == 0) {
                this.mMemInfo.readMemInfo();
                this.mMemTotal = this.mMemInfo.getTotalSizeKb();
            }
            j = this.mMemTotal;
        }
        return j;
    }

    public long getFreeRam() {
        synchronized (LOCK) {
            if (updateMemoryInfo() != 0) {
                return (long) INVALID_VALUE;
            }
            return this.mMemFree;
        }
    }

    public long getMemAvailable() {
        synchronized (LOCK) {
            if (updateMemoryInfo() != 0) {
                return (long) INVALID_VALUE;
            }
            return this.mMemAvailable;
        }
    }

    public static boolean isZramOk() {
        long[] out = new long[2];
        if (Process.readProcFile(PROC_SWAPS_NAME, PROC_SWAPS_FORMAT, null, out, null)) {
            long total = out[0];
            long used = out[1];
            if (used < 0 || total < 0 || total - used <= MemoryConstant.getReservedZramSpace()) {
                return false;
            }
            return true;
        }
        AwareLog.e(TAG, "getUsedZram failed");
        return false;
    }

    public static boolean canKernelCompress() {
        long[] occurCount = new long[1];
        if (!Process.readProcFile(CAN_KERNEL_COMPRESS, new int[]{8224}, null, occurCount, null)) {
            return false;
        }
        if (occurCount[0] == 1) {
            return true;
        }
        return false;
    }

    public static long getPssForPid(int pid) {
        if (pid < 1) {
            return 0;
        }
        long[] statmData = new long[2];
        if (Process.readProcFile("/proc/" + pid + "/statm", PROCESS_STATM_FORMAT, null, statmData, null)) {
            long vss = statmData[0];
            long shared = statmData[1];
            if (vss > shared) {
                return (vss - shared) << 2;
            }
        }
        return 0;
    }

    private int updateMemoryInfo() {
        long[] memData = new long[2];
        if (Process.readProcFile(PROC_MEMINFO_NAME, PROC_MEMINFO_FORMAT, null, memData, null)) {
            this.mMemFree = memData[0];
            this.mMemAvailable = memData[1];
        } else if (this.mMemInfo.readMemInfo() != 0) {
            return -1;
        } else {
            this.mMemFree = this.mMemInfo.getFreeSizeKb();
            this.mMemAvailable = this.mMemInfo.getCachedSizeKb() + this.mMemInfo.getFreeSizeKb();
        }
        return 0;
    }

    public static long getLmkOccurCount() {
        long[] occurCount = new long[1];
        if (Process.readProcFile(LMK_KILL_COUNT, new int[]{8224}, null, occurCount, null)) {
            return occurCount[0];
        }
        return 0;
    }

    public static String[] getMeminfoAllocCount() {
        String[] meminfoAllocCount = new String[2];
        if (Process.readProcFile(MEMINFO_ALLOC_COUNT, new int[]{4106, 4106}, meminfoAllocCount, null, null)) {
            return meminfoAllocCount;
        }
        return new String[0];
    }

    public static String getMmonitorData() {
        int[] iArr = PROC_MMONITOR_FORMAT;
        long[] mmonitorData = new long[iArr.length];
        if (Process.readProcFile(PROC_MMONITOR_NAME, iArr, null, mmonitorData, null)) {
            return parseMemData(mmonitorData);
        }
        return null;
    }

    private static String parseMemData(long[] data) {
        if (data == null || data.length < PROC_MMONITOR_FORMAT.length) {
            AwareLog.e(TAG, "parseMemData: data null");
            return null;
        }
        StringBuilder sb = new StringBuilder(256);
        sb.append("[");
        sb.append("{\"pgAlloc\":\"");
        sb.append(data[1]);
        sb.append("\",\"slowpath0\":\"");
        sb.append(data[2]);
        sb.append("\",\"slowpath1\":\"");
        sb.append(data[3]);
        sb.append("\",\"slowpath2\":\"");
        sb.append(data[4]);
        sb.append("\",\"slowpath3\":\"");
        sb.append(data[5]);
        sb.append("\",\"slowpath4\":\"");
        sb.append(data[6]);
        sb.append("\",\"compactStall\":\"");
        sb.append(data[7]);
        sb.append("\",\"compactSuc\":\"");
        sb.append(data[8]);
        sb.append("\",\"warnAllocFailed\":\"");
        sb.append(data[9]);
        sb.append("\",\"fcache\":\"");
        sb.append(data[11]);
        sb.append("\",\"fcacheMiss\":\"");
        sb.append(data[13]);
        sb.append("\"}");
        sb.append("]");
        AwareLog.d(TAG, "parseMemData: " + sb.toString());
        return sb.toString();
    }

    private static boolean setTopnValue(String line) {
        int topN = 0;
        if (line == null) {
            return false;
        }
        try {
            topN = Integer.parseInt(line);
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "number format exception, check input value: " + line);
        }
        if (topN == MemoryConstant.getTopnMemProcNum()) {
            return false;
        }
        AwareLog.d(TAG, "modify topN value from: " + topN + " to: " + MemoryConstant.getTopnMemProcNum());
        MemoryUtils.setTopnProcMem(MemoryConstant.getTopnMemProcNum());
        return true;
    }

    private static String getLine(BufferedReader bufferedReader) throws IOException {
        StringBuffer result = new StringBuffer();
        if (bufferedReader == null) {
            AwareLog.e(TAG, "input BufferReader is null");
            return null;
        }
        do {
            int getChar = bufferedReader.read();
            if (getChar == -1) {
                return null;
            }
            char cTemp = (char) getChar;
            if (cTemp == '\n') {
                return result.toString();
            }
            result.append(cTemp);
        } while (result.length() <= 100);
        throw new IOException("file line too long, exceed:100");
    }

    private static void safeClose(BufferedReader bufferedReader) {
        if (bufferedReader != null) {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                AwareLog.e(TAG, "close kernel node file exception");
            }
        }
    }

    /* JADX INFO: finally extract failed */
    public static String getTopnProcMem() {
        File file = new File(PROCESS_MEM);
        BufferedReader bufferedReader = null;
        StringBuilder result = new StringBuilder(1000);
        if (!file.exists()) {
            AwareLog.e(TAG, "kernel node not exist");
            return result.toString();
        }
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            result.append("TopN Processes:");
            String line = getLine(bufferedReader);
            if (setTopnValue(line)) {
                safeClose(bufferedReader);
                bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            } else {
                result.append(line);
                result.append(";");
            }
            while (true) {
                String line2 = getLine(bufferedReader);
                if (line2 != null) {
                    result.append(line2);
                    result.append(";");
                } else {
                    String sb = result.toString();
                    safeClose(bufferedReader);
                    return sb;
                }
            }
        } catch (IOException e) {
            AwareLog.e(TAG, "read/write kernel node exception");
            safeClose(bufferedReader);
            return result.toString();
        } catch (Throwable th) {
            safeClose(bufferedReader);
            throw th;
        }
    }
}
