package com.android.server;

import android.os.FileUtils;
import android.util.Slog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

class HwPartitionInfo {
    private static final int BASE_INFO_INVALD = -203;
    public static final int FILE_INVALID_NULL = -201;
    public static final int FILE_INVALID_PATH = -202;
    public static final int GET_PARTION_INFO_ERROR = -205;
    private static final int META_DATA_INVALD = -204;
    private static final String PREFIX_VALID_NODE = "total_valid_node_count: ";
    static final String TAG = "HwPartitionInfo";
    private static final int VALID_NODE_INVALD = -206;
    private static final long sBockLong = 4096;
    private static final long sSectorLong = 512;

    HwPartitionInfo() {
    }

    public static long getPartitionStartPos(String fileName) {
        try {
            return sSectorLong * Long.parseLong(FileUtils.readTextFile(new File(String.format("/sys/class/block/%s/start", new Object[]{fileName})), 0, null).trim());
        } catch (Exception e) {
            Slog.i(TAG, "getPartitionStartPos IOException !", e);
            return -205;
        }
    }

    public static long getPartitionSize(String fileName) {
        try {
            return sSectorLong * Long.parseLong(FileUtils.readTextFile(new File(String.format("/sys/class/block/%s/size", new Object[]{fileName})), 0, null).trim());
        } catch (Exception e) {
            Slog.i(TAG, "getPartitionSize IOException !", e);
            return -205;
        }
    }

    public static long getPartitionMeta(String fileName) {
        try {
            String[] arr = FileUtils.readTextFile(new File(String.format("/proc/fs/f2fs/%s/bd_base_info", new Object[]{fileName})), 0, null).trim().split("\\s+");
            if (arr.length < 2) {
                Slog.i(TAG, "getPartitionMeta error arr.length=" + String.format("%d", new Object[]{Integer.valueOf(arr.length)}));
                return -203;
            }
            long metasize = sBockLong * (Long.parseLong(arr[0]) - Long.parseLong(arr[1]));
            if (metasize >= 0) {
                return metasize;
            }
            Slog.i(TAG, "getPartitionMeta error metasize=" + String.format("%d", new Object[]{Long.valueOf(metasize)}));
            return -204;
        } catch (Exception e) {
            Slog.i(TAG, "getPartitionMeta IOException !", e);
            return -205;
        }
    }

    public static long getPartitionValidNode(String fileName) {
        Exception e;
        Throwable th;
        BufferedReader bufferedReader = null;
        try {
            int startPos;
            String lineStr;
            BufferedReader bufferedReader2 = new BufferedReader(new FileReader(new File(String.format("/proc/fs/f2fs/%s/resizf2fs_info", new Object[]{fileName}))));
            long validNode = -1;
            do {
                try {
                    lineStr = bufferedReader2.readLine();
                    if (lineStr == null) {
                        break;
                    }
                    startPos = lineStr.indexOf(PREFIX_VALID_NODE);
                } catch (IOException e2) {
                    e = e2;
                    bufferedReader = bufferedReader2;
                } catch (Throwable th2) {
                    th = th2;
                    bufferedReader = bufferedReader2;
                }
            } while (-1 == startPos);
            validNode = Long.parseLong(lineStr.substring(startPos + PREFIX_VALID_NODE.length()));
            if (validNode < 0) {
                Slog.i(TAG, "getPartitionValidNode error validNode=" + String.format("%d", new Object[]{Long.valueOf(validNode)}));
                if (bufferedReader2 != null) {
                    try {
                        bufferedReader2.close();
                    } catch (IOException e3) {
                        Slog.i(TAG, "getPartitionValidNode file close IOException !", e3);
                    }
                }
                return -206;
            }
            if (bufferedReader2 != null) {
                try {
                    bufferedReader2.close();
                } catch (IOException e32) {
                    Slog.i(TAG, "getPartitionValidNode file close IOException !", e32);
                }
            }
            return validNode;
        } catch (IOException e4) {
            e = e4;
            try {
                Slog.i(TAG, "getPartitionValidNode IOException !", e);
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e322) {
                        Slog.i(TAG, "getPartitionValidNode file close IOException !", e322);
                    }
                }
                return -205;
            } catch (Throwable th3) {
                th = th3;
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e3222) {
                        Slog.i(TAG, "getPartitionValidNode file close IOException !", e3222);
                    }
                }
                throw th;
            }
        }
    }
}
