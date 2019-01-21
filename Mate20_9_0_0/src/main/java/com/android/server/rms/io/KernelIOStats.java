package com.android.server.rms.io;

import android.rms.utils.Utils;
import android.util.Log;
import java.util.Hashtable;
import java.util.List;

public class KernelIOStats {
    private static final String CHARSET_UTF8 = "UTF-8";
    private static final String CID_FILE_PATH = "/sys/block/mmcblk0/device/cid";
    private static final int ERROR_LIFE_TIME = -1;
    private static final long ERROR_WRITE_BYTES = -1;
    private static final int FILE_BUFFER_SIZE = 1024;
    public static final int HEALTH_TYPE_A = 0;
    public static final int HEALTH_TYPE_B = 1;
    public static final int HEALTH_TYPE_EOL = 2;
    private static final String SPLIT_UID = ",";
    private static final String TAG = "RMS.IO.KernelIOStats";
    private static final String UID_ADD_PATH = "uid_iomonitor_list";
    private static final String UID_MONITOR_BASE_PATH = "/proc/uid_iostats/";
    private static final String UID_REMOVE_PATH = "remove_uid_list";
    private static final String UID_SHOW_DATAS_SPLIT = "\n";
    private static final String UID_SHOW_PATH = "show_uid_iostats";
    private static final String UID_SHOW_READ_WRITE_SPLIT = " ";
    private static final String UID_SHOW_UID_STATS_SPLIT = ":";

    static native String native_read_file(String str);

    static native int native_write_file(String str, String str2, int i);

    static {
        try {
            System.loadLibrary("iostats_jni");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "iostats_jni not found");
        } catch (Exception e2) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("an Exception occurs:");
            stringBuilder.append(e2.getMessage());
            Log.e(str, stringBuilder.toString());
        }
    }

    public static IOStatsCollection readUidIOStatsFromKernel(Hashtable<Integer, String> uidPkgTable) {
        String str;
        StringBuilder stringBuilder;
        IOStatsCollection ioStatsResult = new IOStatsCollection();
        if (uidPkgTable == null || uidPkgTable.size() == 0) {
            Log.e(TAG, "readUidIOStatsFromKernel:the uidPkgTable is empty");
            return ioStatsResult;
        }
        try {
            String ioStatsBuffer = native_read_file("/proc/uid_iostats/show_uid_iostats");
            if (ioStatsBuffer != null) {
                if (ioStatsBuffer.length() != 0) {
                    if (Utils.DEBUG) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("readUidIOStatsFromKernel,ioStatsBuffer:");
                        stringBuilder2.append(ioStatsBuffer);
                        Log.d(str2, stringBuilder2.toString());
                    }
                    recordHistoryByKernelNodeInfor(ioStatsBuffer, uidPkgTable, ioStatsResult);
                    return ioStatsResult;
                }
            }
            Log.e(TAG, "readUidIOStatsFromKernel io_stats file is empty");
            return ioStatsResult;
        } catch (RuntimeException ex) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("readUidIOStats:an RuntimeException occurs:");
            stringBuilder.append(ex.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (Exception ex2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("readUidIOStats:an Exception occurs:");
            stringBuilder.append(ex2.getMessage());
            Log.e(str, stringBuilder.toString());
        }
    }

    private static void recordHistoryByKernelNodeInfor(String ioStatsBuffer, Hashtable<Integer, String> uidPkgTable, IOStatsCollection ioStatsResult) {
        long readNum;
        int uid;
        long currentTime = Utils.getShortDateFormatValue(System.currentTimeMillis());
        int uid2 = 0;
        String[] splitsArray = ioStatsBuffer.split(UID_SHOW_DATAS_SPLIT);
        int length = splitsArray.length;
        long writeNum = 0;
        long readNum2 = 0;
        String[] readWriteArray = null;
        String[] uidSplitArray = null;
        int uidSplitArray2 = 0;
        while (uidSplitArray2 < length) {
            String[] uidSplitArray3;
            String[] readWriteArray2;
            String ioStats = splitsArray[uidSplitArray2];
            uidSplitArray = ioStats.split(UID_SHOW_UID_STATS_SPLIT);
            String[] readWriteArray3 = readWriteArray;
            String str;
            IOStatsCollection iOStatsCollection;
            if (uidSplitArray.length < 2) {
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                readNum = readNum2;
                stringBuilder.append("uidSplitArray's length is invalid:");
                stringBuilder.append(ioStats);
                Log.e(str, stringBuilder.toString());
                iOStatsCollection = ioStatsResult;
                uidSplitArray3 = uidSplitArray;
                readWriteArray2 = readWriteArray3;
                readNum2 = readNum;
            } else {
                readNum = readNum2;
                readWriteArray2 = uidSplitArray[1].trim().split(UID_SHOW_READ_WRITE_SPLIT);
                if (readWriteArray2.length < 2) {
                    str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    uid = uid2;
                    stringBuilder2.append("readWriteArray's length is invalid :");
                    stringBuilder2.append(uidSplitArray[1]);
                    Log.e(str, stringBuilder2.toString());
                    iOStatsCollection = ioStatsResult;
                    uidSplitArray3 = uidSplitArray;
                    readNum2 = readNum;
                    uid2 = uid;
                } else {
                    uid2 = Integer.parseInt(uidSplitArray[0]);
                    long readNum3 = Long.parseLong(readWriteArray2[0]);
                    long writeNum2 = Long.parseLong(readWriteArray2[1]);
                    if (readNum3 == 0 && writeNum2 == 0) {
                        iOStatsCollection = ioStatsResult;
                        uidSplitArray3 = uidSplitArray;
                        readNum2 = readNum3;
                        writeNum = writeNum2;
                    } else {
                        String pkgName = (String) uidPkgTable.get(Integer.valueOf(uid2));
                        String[] readWriteArray4 = readWriteArray2;
                        int uid3 = uid2;
                        uidSplitArray3 = uidSplitArray;
                        IOStatsHistory iOStatsHistory = r4;
                        IOStatsHistory iOStatsHistory2 = new IOStatsHistory(uid2, pkgName, currentTime, readNum3, writeNum2);
                        ioStatsResult.recordHistory(iOStatsHistory);
                        readNum2 = readNum3;
                        writeNum = writeNum2;
                        String str2 = pkgName;
                        readWriteArray2 = readWriteArray4;
                        uid2 = uid3;
                    }
                }
            }
            uidSplitArray2++;
            readWriteArray = readWriteArray2;
            uidSplitArray = uidSplitArray3;
        }
        readNum = readNum2;
        uid = uid2;
        readWriteArray = ioStatsResult;
    }

    public static String getCIDNodeInformation() {
        String totalWriteBytes = "";
        try {
            return native_read_file(CID_FILE_PATH);
        } catch (RuntimeException e) {
            Log.e(TAG, "getCIDNodeInformation,the RuntimeException occurs");
            return totalWriteBytes;
        } catch (Exception e2) {
            Log.e(TAG, "getCIDNodeInformation Exception occurs");
            return totalWriteBytes;
        }
    }

    public static void writeUidList(List<Integer> removeUidList, List<Integer> addUidList) {
        if ((removeUidList == null || removeUidList.size() == 0) && (addUidList == null || addUidList.size() == 0)) {
            Log.e(TAG, "writeUidList,both the removeUidList and addUidList are empty");
            return;
        }
        String removeUidConnection = convert(removeUidList);
        String addUidConnection = convert(addUidList);
        if (Utils.DEBUG) {
            Log.d(TAG, String.format("writeUidList,removeUidConnection:%s,addUidConnection:%s", new Object[]{removeUidConnection, addUidConnection}));
        }
        if (removeUidConnection != null) {
            writeToUidMonitorNode(removeUidConnection, "/proc/uid_iostats/remove_uid_list");
        }
        if (addUidConnection != null) {
            writeToUidMonitorNode(addUidConnection, "/proc/uid_iostats/uid_iomonitor_list");
        }
    }

    private static String convert(List<Integer> uidList) {
        if (uidList == null || uidList.size() == 0) {
            if (Utils.DEBUG) {
                Log.d(TAG, "convert, the uidList is empty");
            }
            return null;
        }
        StringBuilder resultBuilder = new StringBuilder();
        for (Integer uid : uidList) {
            resultBuilder.append(uid.intValue());
            resultBuilder.append(",");
        }
        return resultBuilder.toString().substring(0, resultBuilder.length() - 1);
    }

    private static void writeToUidMonitorNode(String uidConnection, String path) {
        try {
            if (Utils.DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("writeToUidMonitorNode,filePath:");
                stringBuilder.append(path);
                Log.d(str, stringBuilder.toString());
            }
            if (uidConnection != null) {
                if (uidConnection.length() != 0) {
                    native_write_file(path, uidConnection, uidConnection.length());
                    return;
                }
            }
            Log.e(TAG, "writeToUidMonitorNode,the uidConnection is empty");
        } catch (RuntimeException e) {
            Log.e(TAG, "fail to writeToUidMonitorNode:the RuntimeException");
        } catch (Exception e2) {
            Log.e(TAG, "fail to writeToUidMonitorNode:the other exception");
        }
    }
}
