package com.android.server;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.text.TextUtils;
import android.util.Log;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.Locale;

public class HwAutoUpdate {
    private static final String BOARD_PLATFORM_TAG = "ro.board.platform";
    private static final String CAPACITY_LEVEL_VALUE_PATH_HISI = "/sys/class/power_supply/Battery/capacity";
    private static final String CAPACITY_LEVEL_VALUE_PATH_QCOM_MTK = "/sys/class/power_supply/battery/capacity";
    private static final String CHIP_PLATFORM_TAG = "ro.config.hw_ChipPlatform";
    private static final String COMMOND_FILE = "/cache/recovery/command";
    private static final String PLATFORM_MTK = "MTK_Platform";
    private static final String PLATFORM_QUALCOMM = "msm";
    private static final int SAFT_BATTERY_LEVEL = 50;
    private static final String TAG = "HwAutoUpdate";
    private static final String UPDATE_POWEROFF = "update_poweroff";
    private static final String UPDATE_REBOOT = "update_reboot";
    private static HwAutoUpdate mInstance = null;

    private static class AutoupdateCommand {
        static final String KEY_AUTOUPDATE = "--autoupdate";
        static final String KEY_REBOOT = "--reboot";
        static final String KEY_UPDATEPACKAGE = "--update_package";
        static final String KEY_USER_FREESIZE = "--user_freesize";
        private String autoupdate;
        private String reboot;
        private String update_package;
        private long user_freesize;

        private AutoupdateCommand() {
            this.user_freesize = 0;
        }

        public void setUpdatePackage(String update_package) {
            this.update_package = update_package.trim();
        }

        public void setAutoDate(String autoupdate) {
            this.autoupdate = autoupdate.trim();
        }

        public void setReboot(String reboot) {
            this.reboot = reboot.trim();
        }

        public void setUserFreeSize(String user_freesize) {
            try {
                this.user_freesize = Long.parseLong(user_freesize.trim());
            } catch (NumberFormatException e) {
                Log.i(HwAutoUpdate.TAG, "auto update command parse user_freesize error");
                this.user_freesize = -1;
            }
        }

        public String getUpdatePackage() {
            return this.update_package;
        }

        public boolean isAutoUpdate() {
            if (this.autoupdate == null) {
                return false;
            }
            return "TRUE".equals(this.autoupdate.toUpperCase(Locale.ENGLISH));
        }

        public String getReboot() {
            return this.reboot;
        }

        public long getUserFreeSize() {
            return this.user_freesize;
        }

        public String toString() {
            StringBuilder command = new StringBuilder();
            if (!TextUtils.isEmpty(this.update_package)) {
                command.append(KEY_UPDATEPACKAGE);
                command.append("=");
                command.append(this.update_package);
                command.append("\r\n");
            }
            if (!TextUtils.isEmpty(this.autoupdate)) {
                command.append(KEY_AUTOUPDATE);
                command.append("=");
                command.append(this.autoupdate);
                command.append("\r\n");
            }
            if (!TextUtils.isEmpty(this.reboot)) {
                command.append(KEY_REBOOT);
                command.append("=");
                command.append(this.reboot);
                command.append("\r\n");
            }
            command.append(KEY_USER_FREESIZE);
            command.append("=");
            command.append(this.user_freesize);
            command.append("\r\n");
            return command.toString();
        }
    }

    public static HwAutoUpdate getInstance() {
        if (mInstance == null) {
            mInstance = new HwAutoUpdate();
        }
        return mInstance;
    }

    public boolean isAutoSystemUpdate(Context context, boolean reboot) {
        int automatic = Global.getInt(context.getContentResolver(), "ota_disable_automatic_update", 0);
        Log.i(TAG, "automatic switch " + automatic);
        if (automatic == 0 && getBatteryLevelValue() > 50) {
            AutoupdateCommand autoupdateCommand = getCommand(COMMOND_FILE);
            Log.d(TAG, "autoupdateCommand : " + autoupdateCommand.toString());
            if (autoupdateCommand.isAutoUpdate() && isUserFreeSizeEnough(autoupdateCommand)) {
                StringBuilder reboot_reason = new StringBuilder();
                reboot_reason.append("--reboot");
                reboot_reason.append("=");
                reboot_reason.append(reboot ? UPDATE_REBOOT : UPDATE_POWEROFF);
                writeRebootReason(COMMOND_FILE, reboot_reason.toString());
                return true;
            }
        }
        return false;
    }

    private AutoupdateCommand getCommand(String filePath) {
        return getCommondFormFile(filePath);
    }

    private AutoupdateCommand getCommondFormFile(String filePath) {
        IOException e;
        Throwable th;
        BufferedReader reader = null;
        FileInputStream fileInputStream = null;
        AutoupdateCommand autoupdateCommand = new AutoupdateCommand();
        try {
            FileInputStream in = new FileInputStream(filePath);
            try {
                BufferedReader reader2 = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                while (true) {
                    try {
                        String line = reader2.readLine();
                        if (line == null) {
                            break;
                        }
                        parseCommand(line, autoupdateCommand);
                    } catch (IOException e2) {
                        e = e2;
                        fileInputStream = in;
                        reader = reader2;
                    } catch (Throwable th2) {
                        th = th2;
                        fileInputStream = in;
                        reader = reader2;
                    }
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e3) {
                        Log.w(TAG, "getCommondFormFile FileInputStream close error");
                    }
                }
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (IOException e4) {
                        Log.w(TAG, "getCommondFormFile BufferedReader close error");
                    }
                }
                fileInputStream = in;
            } catch (IOException e5) {
                e = e5;
                fileInputStream = in;
                try {
                    Log.e(TAG, "getCommondFormFile ", e);
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e6) {
                            Log.w(TAG, "getCommondFormFile FileInputStream close error");
                        }
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e7) {
                            Log.w(TAG, "getCommondFormFile BufferedReader close error");
                        }
                    }
                    return autoupdateCommand;
                } catch (Throwable th3) {
                    th = th3;
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e8) {
                            Log.w(TAG, "getCommondFormFile FileInputStream close error");
                        }
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e9) {
                            Log.w(TAG, "getCommondFormFile BufferedReader close error");
                        }
                    }
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                fileInputStream = in;
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                if (reader != null) {
                    reader.close();
                }
                throw th;
            }
        } catch (IOException e10) {
            e = e10;
            Log.e(TAG, "getCommondFormFile ", e);
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (reader != null) {
                reader.close();
            }
            return autoupdateCommand;
        }
        return autoupdateCommand;
    }

    private void parseCommand(String command, AutoupdateCommand autoupdateCommand) {
        if (!TextUtils.isEmpty(command)) {
            String[] kv = command.split("=");
            if (2 == kv.length) {
                String str = kv[0];
                if (str.equals("--update_package")) {
                    autoupdateCommand.setUpdatePackage(kv[1]);
                } else if (str.equals("--autoupdate")) {
                    autoupdateCommand.setAutoDate(kv[1]);
                } else if (str.equals("--reboot")) {
                    autoupdateCommand.setReboot(kv[1]);
                } else if (str.equals("--user_freesize")) {
                    autoupdateCommand.setUserFreeSize(kv[1]);
                } else {
                    Log.w(TAG, "can not parse command");
                }
            } else {
                Log.w(TAG, "Error command");
            }
        }
    }

    private void writeRebootReason(String filePath, String reason) {
        IOException e;
        Throwable th;
        BufferedWriter bufferedWriter = null;
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(filePath), true), "UTF-8"));
            try {
                writer.write(reason);
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (Exception e2) {
                        Log.e(TAG, "writeRebootReason writer close error");
                    }
                }
                bufferedWriter = writer;
            } catch (IOException e3) {
                e = e3;
                bufferedWriter = writer;
                try {
                    Log.e(TAG, "writeRebootReason error : " + e);
                    if (bufferedWriter != null) {
                        try {
                            bufferedWriter.close();
                        } catch (Exception e4) {
                            Log.e(TAG, "writeRebootReason writer close error");
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (bufferedWriter != null) {
                        try {
                            bufferedWriter.close();
                        } catch (Exception e5) {
                            Log.e(TAG, "writeRebootReason writer close error");
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                bufferedWriter = writer;
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
                throw th;
            }
        } catch (IOException e6) {
            e = e6;
            Log.e(TAG, "writeRebootReason error : " + e);
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
        }
    }

    private String getBatteryPath() {
        if (SystemProperties.get(BOARD_PLATFORM_TAG, "").startsWith(PLATFORM_QUALCOMM)) {
            return CAPACITY_LEVEL_VALUE_PATH_QCOM_MTK;
        }
        if (PLATFORM_MTK.equals(SystemProperties.get(CHIP_PLATFORM_TAG, ""))) {
            return CAPACITY_LEVEL_VALUE_PATH_QCOM_MTK;
        }
        return CAPACITY_LEVEL_VALUE_PATH_HISI;
    }

    private int getBatteryLevelValue() {
        IOException e;
        Throwable th;
        String lineValue;
        String path = getBatteryPath();
        File file = new File(getBatteryPath());
        if (!file.exists() || (file.canRead() ^ 1) != 0) {
            return -1;
        }
        Reader reader = null;
        FileInputStream fileInputStream = null;
        char[] tempChars = new char[512];
        StringBuilder battery_level = new StringBuilder();
        try {
            FileInputStream input = new FileInputStream(path);
            try {
                Reader inputStreamReader = new InputStreamReader(input, "UTF-8");
                while (true) {
                    try {
                        int charRead = inputStreamReader.read(tempChars, 0, tempChars.length);
                        if (charRead == -1) {
                            break;
                        }
                        battery_level.append(tempChars, 0, charRead);
                    } catch (IOException e2) {
                        e = e2;
                        fileInputStream = input;
                        reader = inputStreamReader;
                    } catch (Throwable th2) {
                        th = th2;
                        fileInputStream = input;
                        reader = inputStreamReader;
                    }
                }
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e3) {
                        Log.e(TAG, "getBatteryLevelValue input close error");
                    }
                }
                if (inputStreamReader != null) {
                    try {
                        inputStreamReader.close();
                    } catch (IOException e4) {
                        Log.e(TAG, "getBatteryLevelValue reader close error");
                    }
                }
                fileInputStream = input;
                reader = inputStreamReader;
            } catch (IOException e5) {
                e = e5;
                fileInputStream = input;
                try {
                    Log.e(TAG, "getBatteryLevelValue ", e);
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e6) {
                            Log.e(TAG, "getBatteryLevelValue input close error");
                        }
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e7) {
                            Log.e(TAG, "getBatteryLevelValue reader close error");
                        }
                    }
                    lineValue = battery_level.toString();
                    if (!lineValue.equals("")) {
                        return Integer.parseInt(lineValue.trim());
                    }
                    Log.w(TAG, "readFileByInt return invalid value");
                    return -1;
                } catch (Throwable th3) {
                    th = th3;
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e8) {
                            Log.e(TAG, "getBatteryLevelValue input close error");
                        }
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e9) {
                            Log.e(TAG, "getBatteryLevelValue reader close error");
                        }
                    }
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                fileInputStream = input;
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                if (reader != null) {
                    reader.close();
                }
                throw th;
            }
        } catch (IOException e10) {
            e = e10;
            Log.e(TAG, "getBatteryLevelValue ", e);
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (reader != null) {
                reader.close();
            }
            lineValue = battery_level.toString();
            if (lineValue.equals("")) {
                return Integer.parseInt(lineValue.trim());
            }
            Log.w(TAG, "readFileByInt return invalid value");
            return -1;
        }
        lineValue = battery_level.toString();
        try {
            if (lineValue.equals("")) {
                return Integer.parseInt(lineValue.trim());
            }
        } catch (NumberFormatException e11) {
            Log.e(TAG, "readFileByInt catch NumberFormatException");
        } catch (Exception ex) {
            Log.e(TAG, "getBatteryLevelValue: parseInt", ex);
        }
        Log.w(TAG, "readFileByInt return invalid value");
        return -1;
    }

    private boolean isUserFreeSizeEnough(AutoupdateCommand autoupdateCommand) {
        long needUserFreeSize = autoupdateCommand.getUserFreeSize();
        if (-1 == needUserFreeSize) {
            return false;
        }
        if (0 == needUserFreeSize || getAvailableSize(Environment.getDataDirectory().getPath()) >= needUserFreeSize) {
            return true;
        }
        Log.i(TAG, "free size not enough");
        return false;
    }

    private long getAvailableSize(String path) {
        try {
            StatFs state = new StatFs(path);
            long blocksize = (long) state.getBlockSize();
            long availableBlocks = (long) state.getAvailableBlocks();
            long availableSize = blocksize * availableBlocks;
            Log.d(TAG, "getAvailableSize path : " + path + " availableSize =  " + "(" + blocksize + "*" + availableBlocks + ") = " + availableSize);
            return availableSize;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "getAvailableSize error : " + path, e);
            return 0;
        }
    }
}
