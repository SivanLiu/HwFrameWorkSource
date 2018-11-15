package com.android.server;

import android.content.Context;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;
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
import java.util.Locale;

public class HwAutoUpdate {
    private static final String COMMOND_FILE = "/cache/recovery/command";
    private static final int SAFE_BATTERY_LEVEL = 50;
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
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("automatic switch ");
        stringBuilder.append(automatic);
        Log.i(str, stringBuilder.toString());
        if (automatic != 0) {
            return false;
        }
        if (getBatteryLevelValue(context) > 50) {
            AutoupdateCommand autoupdateCommand = getCommand(COMMOND_FILE);
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("autoupdateCommand : ");
            stringBuilder2.append(autoupdateCommand.toString());
            Log.d(str2, stringBuilder2.toString());
            if (autoupdateCommand.isAutoUpdate() && isUserFreeSizeEnough(autoupdateCommand)) {
                StringBuilder reboot_reason = new StringBuilder();
                reboot_reason.append("--reboot");
                reboot_reason.append("=");
                reboot_reason.append(reboot ? UPDATE_REBOOT : UPDATE_POWEROFF);
                writeRebootReason(COMMOND_FILE, reboot_reason.toString());
                return true;
            }
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("current batteryLevel: ");
        stringBuilder.append(getBatteryLevelValue(context));
        stringBuilder.append(" <= safe value(50%), return");
        Log.d(str, stringBuilder.toString());
        return false;
    }

    private AutoupdateCommand getCommand(String filePath) {
        return getCommondFormFile(filePath);
    }

    private AutoupdateCommand getCommondFormFile(String filePath) {
        BufferedReader reader = null;
        FileInputStream in = null;
        AutoupdateCommand autoupdateCommand = new AutoupdateCommand();
        try {
            in = new FileInputStream(filePath);
            reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            while (true) {
                String readLine = reader.readLine();
                String line = readLine;
                if (readLine != null) {
                    parseCommand(line, autoupdateCommand);
                } else {
                    try {
                        break;
                    } catch (IOException e) {
                        Log.w(TAG, "getCommondFormFile FileInputStream close error");
                    }
                }
            }
            in.close();
            try {
                reader.close();
            } catch (IOException e2) {
                Log.w(TAG, "getCommondFormFile BufferedReader close error");
            }
        } catch (IOException e3) {
            Log.e(TAG, "getCommondFormFile ", e3);
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e4) {
                    Log.w(TAG, "getCommondFormFile FileInputStream close error");
                }
            }
            if (reader != null) {
                reader.close();
            }
        } catch (Throwable th) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e5) {
                    Log.w(TAG, "getCommondFormFile FileInputStream close error");
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e6) {
                    Log.w(TAG, "getCommondFormFile BufferedReader close error");
                }
            }
        }
        return autoupdateCommand;
    }

    /* JADX WARNING: Removed duplicated region for block: B:26:0x005a  */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0074  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x006e  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0068  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x0062  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x005a  */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0074  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x006e  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0068  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x0062  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x005a  */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0074  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x006e  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0068  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x0062  */
    /* JADX WARNING: Missing block: B:14:0x0035, code:
            if (r3.equals("--reboot") == false) goto L_0x0056;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void parseCommand(String command, AutoupdateCommand autoupdateCommand) {
        if (!TextUtils.isEmpty(command)) {
            String[] kv = command.split("=");
            int i = 2;
            if (2 == kv.length) {
                String str = kv[0];
                int hashCode = str.hashCode();
                if (hashCode == -1453042472) {
                    if (str.equals("--autoupdate")) {
                        i = 1;
                        switch (i) {
                            case 0:
                                break;
                            case 1:
                                break;
                            case 2:
                                break;
                            case 3:
                                break;
                            default:
                                break;
                        }
                    }
                } else if (hashCode == -22157200) {
                    if (str.equals("--update_package")) {
                        i = 0;
                        switch (i) {
                            case 0:
                                break;
                            case 1:
                                break;
                            case 2:
                                break;
                            case 3:
                                break;
                            default:
                                break;
                        }
                    }
                } else if (hashCode == 653154497) {
                    if (str.equals("--user_freesize")) {
                        i = 3;
                        switch (i) {
                            case 0:
                                autoupdateCommand.setUpdatePackage(kv[1]);
                                break;
                            case 1:
                                autoupdateCommand.setAutoDate(kv[1]);
                                break;
                            case 2:
                                autoupdateCommand.setReboot(kv[1]);
                                break;
                            case 3:
                                autoupdateCommand.setUserFreeSize(kv[1]);
                                break;
                            default:
                                Log.w(TAG, "can not parse command");
                                break;
                        }
                    }
                } else if (hashCode == 1465075013) {
                }
                i = -1;
                switch (i) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                    default:
                        break;
                }
            }
            Log.w(TAG, "Error command");
        }
    }

    private void writeRebootReason(String filePath, String reason) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(filePath), true), "UTF-8"));
            writer.write(reason);
            try {
                writer.close();
            } catch (Exception e) {
                Log.e(TAG, "writeRebootReason writer close error");
            }
        } catch (IOException e2) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("writeRebootReason error : ");
            stringBuilder.append(e2);
            Log.e(str, stringBuilder.toString());
            if (writer != null) {
                writer.close();
            }
        } catch (Throwable th) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e3) {
                    Log.e(TAG, "writeRebootReason writer close error");
                }
            }
        }
    }

    private int getBatteryLevelValue(Context context) {
        BatteryManager batteryManager = (BatteryManager) context.getSystemService("batterymanager");
        if (batteryManager == null) {
            Log.e(TAG, "batteryManager is null error");
            return -1;
        }
        int batteryLevel = batteryManager.getIntProperty(4);
        if (batteryLevel >= 0) {
            return batteryLevel;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getBatteryLevelValue failed, batteryLevel: ");
        stringBuilder.append(batteryLevel);
        Log.e(str, stringBuilder.toString());
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getAvailableSize path : ");
            stringBuilder.append(path);
            stringBuilder.append(" availableSize =  (");
            stringBuilder.append(blocksize);
            stringBuilder.append("*");
            stringBuilder.append(availableBlocks);
            stringBuilder.append(") = ");
            stringBuilder.append(availableSize);
            Log.d(str, stringBuilder.toString());
            return availableSize;
        } catch (IllegalArgumentException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getAvailableSize error : ");
            stringBuilder2.append(path);
            Log.e(str2, stringBuilder2.toString(), e);
            return 0;
        }
    }
}
