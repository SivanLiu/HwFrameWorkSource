package com.huawei.android.util;

import android.os.SystemProperties;
import android.util.Log;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HwSystemInfo {
    private static final long GB_IN_KB = 1048576;
    static final String LOG_TAG = "DeviceInfo";
    private static int configEmmcSize = SystemProperties.getInt("ro.config.hw_emmcSize", -1);
    private static int configRamSize = SystemProperties.getInt("ro.config.hw_ramSize", -1);
    private static final Pattern sEmmcSizePattern = Pattern.compile("\\s\\d+\\s+\\d+\\s+(\\d+)\\smmcblk0");
    private static final String sKernelCmdLine = getProcInfo("/proc/meminfo");
    private static final String sKernelPartitions = getProcInfo("/proc/partitions");
    private static final Pattern sRamSizePattern = Pattern.compile("MemTotal:\\s*(\\d+)\\s*");

    public static String getDeviceRam() {
        if (-1 != configRamSize) {
            return String.valueOf(configRamSize);
        }
        String ramSize = "";
        Matcher matcher = sRamSizePattern.matcher(sKernelCmdLine);
        if (matcher.find()) {
            ramSize = matcher.group(1);
        } else {
            Log.e(LOG_TAG, "Ram Info not found, display nothing");
        }
        long ramLong = 0;
        try {
            ramLong = Long.parseLong(ramSize);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (ramLong > 0) {
            long timesToGb = ramLong / GB_IN_KB;
            if (ramLong % GB_IN_KB != 0) {
                ramSize = String.valueOf(GB_IN_KB * (1 + timesToGb));
            }
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ramSize =");
        stringBuilder.append(ramSize);
        Log.d(str, stringBuilder.toString());
        return ramSize;
    }

    public static String getDeviceEmmc() {
        if (-1 != configEmmcSize) {
            return String.valueOf(configEmmcSize);
        }
        String emmcSize = "";
        Matcher matcher = sEmmcSizePattern.matcher(sKernelPartitions);
        if (matcher.find()) {
            emmcSize = matcher.group(1);
        } else {
            Log.e(LOG_TAG, "Emmc Info not found, display nothing");
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("emmcSize =");
        stringBuilder.append(emmcSize);
        Log.d(str, stringBuilder.toString());
        return emmcSize;
    }

    private static String getProcInfo(String path) {
        String procInfo = "";
        FileInputStream is = null;
        try {
            is = new FileInputStream(path);
            byte[] buffer = new byte[2048];
            int count = is.read(buffer);
            if (count > 0) {
                procInfo = new String(buffer, 0, count, "UTF-8");
            }
            try {
                is.close();
            } catch (IOException e) {
            }
        } catch (IOException e2) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("No path exception=");
            stringBuilder.append(e2);
            Log.d(str, stringBuilder.toString());
            if (is != null) {
                is.close();
            }
        } catch (Throwable th) {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e3) {
                }
            }
        }
        return procInfo;
    }
}
