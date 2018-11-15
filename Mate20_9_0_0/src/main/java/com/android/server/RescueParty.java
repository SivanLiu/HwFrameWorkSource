package com.android.server;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.FileUtils;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.util.ExceptionUtils;
import android.util.MathUtils;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.ArrayUtils;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.pm.PackageManagerServiceUtils;
import java.io.File;

public class RescueParty {
    private static final int LEVEL_FACTORY_RESET = 4;
    private static final int LEVEL_NONE = 0;
    private static final int LEVEL_RESET_SETTINGS_TRUSTED_DEFAULTS = 3;
    private static final int LEVEL_RESET_SETTINGS_UNTRUSTED_CHANGES = 2;
    private static final int LEVEL_RESET_SETTINGS_UNTRUSTED_DEFAULTS = 1;
    private static final String PROP_DISABLE_RESCUE = "persist.sys.disable_rescue";
    private static final String PROP_ENABLE_RESCUE = "persist.sys.enable_rescue";
    private static final String PROP_RESCUE_BOOT_COUNT = "sys.rescue_boot_count";
    private static final String PROP_RESCUE_BOOT_START = "sys.rescue_boot_start";
    private static final String PROP_RESCUE_LEVEL = "sys.rescue_level";
    private static final String PROP_VIRTUAL_DEVICE = "ro.hardware.virtual_device";
    private static final String TAG = "RescueParty";
    private static SparseArray<Threshold> sApps = new SparseArray();
    private static final Threshold sBoot = new BootThreshold();

    private static abstract class Threshold {
        private final int triggerCount;
        private final long triggerWindow;
        private final int uid;

        public abstract int getCount();

        public abstract long getStart();

        public abstract void setCount(int i);

        public abstract void setStart(long j);

        public Threshold(int uid, int triggerCount, long triggerWindow) {
            this.uid = uid;
            this.triggerCount = triggerCount;
            this.triggerWindow = triggerWindow;
        }

        public void reset() {
            setCount(0);
            setStart(0);
        }

        public boolean incrementAndTest() {
            long now = SystemClock.elapsedRealtime();
            long window = now - getStart();
            boolean z = false;
            if (window > this.triggerWindow) {
                setCount(1);
                setStart(now);
                return false;
            }
            int count = getCount() + 1;
            setCount(count);
            EventLogTags.writeRescueNote(this.uid, count, window);
            String str = RescueParty.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Noticed ");
            stringBuilder.append(count);
            stringBuilder.append(" events for UID ");
            stringBuilder.append(this.uid);
            stringBuilder.append(" in last ");
            stringBuilder.append(window / 1000);
            stringBuilder.append(" sec");
            Slog.w(str, stringBuilder.toString());
            if (count >= this.triggerCount) {
                z = true;
            }
            return z;
        }
    }

    private static class AppThreshold extends Threshold {
        private int count;
        private long start;

        public AppThreshold(int uid) {
            super(uid, 5, 30000);
        }

        public int getCount() {
            return this.count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public long getStart() {
            return this.start;
        }

        public void setStart(long start) {
            this.start = start;
        }
    }

    private static class BootThreshold extends Threshold {
        public BootThreshold() {
            super(0, 5, BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS);
        }

        public int getCount() {
            return SystemProperties.getInt(RescueParty.PROP_RESCUE_BOOT_COUNT, 0);
        }

        public void setCount(int count) {
            SystemProperties.set(RescueParty.PROP_RESCUE_BOOT_COUNT, Integer.toString(count));
        }

        public long getStart() {
            return SystemProperties.getLong(RescueParty.PROP_RESCUE_BOOT_START, 0);
        }

        public void setStart(long start) {
            SystemProperties.set(RescueParty.PROP_RESCUE_BOOT_START, Long.toString(start));
        }
    }

    private static boolean isDisabled() {
        if (SystemProperties.getBoolean(PROP_ENABLE_RESCUE, false)) {
            return false;
        }
        if (Build.IS_ENG) {
            Slog.v(TAG, "Disabled because of eng build");
            return true;
        } else if (Build.IS_USERDEBUG && isUsbActive()) {
            Slog.v(TAG, "Disabled because of active USB connection");
            return true;
        } else if (!SystemProperties.getBoolean(PROP_DISABLE_RESCUE, false)) {
            return false;
        } else {
            Slog.v(TAG, "Disabled because of manual property");
            return true;
        }
    }

    public static void noteBoot(Context context) {
        if (!isDisabled() && sBoot.incrementAndTest()) {
            sBoot.reset();
            incrementRescueLevel(sBoot.uid);
            executeRescueLevel(context);
        }
    }

    public static void notePersistentAppCrash(Context context, int uid) {
        if (!isDisabled()) {
            Threshold t = (Threshold) sApps.get(uid);
            if (t == null) {
                t = new AppThreshold(uid);
                sApps.put(uid, t);
            }
            if (t.incrementAndTest()) {
                t.reset();
                incrementRescueLevel(t.uid);
                executeRescueLevel(context);
            }
        }
    }

    public static boolean isAttemptingFactoryReset() {
        return SystemProperties.getInt(PROP_RESCUE_LEVEL, 0) == 4;
    }

    private static void incrementRescueLevel(int triggerUid) {
        int level = MathUtils.constrain(SystemProperties.getInt(PROP_RESCUE_LEVEL, 0) + 1, 0, 4);
        SystemProperties.set(PROP_RESCUE_LEVEL, Integer.toString(level));
        EventLogTags.writeRescueLevel(level, triggerUid);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Incremented rescue level to ");
        stringBuilder.append(levelToString(level));
        stringBuilder.append(" triggered by UID ");
        stringBuilder.append(triggerUid);
        PackageManagerServiceUtils.logCriticalInfo(5, stringBuilder.toString());
    }

    public static void onSettingsProviderPublished(Context context) {
        executeRescueLevel(context);
    }

    private static void executeRescueLevel(Context context) {
        int level = SystemProperties.getInt(PROP_RESCUE_LEVEL, 0);
        if (level != 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Attempting rescue level ");
            stringBuilder.append(levelToString(level));
            Slog.w(str, stringBuilder.toString());
            try {
                executeRescueLevelInternal(context, level);
                EventLogTags.writeRescueSuccess(level);
                stringBuilder = new StringBuilder();
                stringBuilder.append("Finished rescue level ");
                stringBuilder.append(levelToString(level));
                PackageManagerServiceUtils.logCriticalInfo(3, stringBuilder.toString());
            } catch (Throwable t) {
                String msg = ExceptionUtils.getCompleteMessage(t);
                EventLogTags.writeRescueFailure(level, msg);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed rescue level ");
                stringBuilder2.append(levelToString(level));
                stringBuilder2.append(": ");
                stringBuilder2.append(msg);
                PackageManagerServiceUtils.logCriticalInfo(6, stringBuilder2.toString());
            }
        }
    }

    private static void executeRescueLevelInternal(Context context, int level) throws Exception {
        switch (level) {
            case 1:
                resetAllSettings(context, 2);
                return;
            case 2:
                resetAllSettings(context, 3);
                return;
            case 3:
                resetAllSettings(context, 4);
                return;
            case 4:
                Slog.w(TAG, "Auto factory reset is temporary disabled");
                return;
            default:
                return;
        }
    }

    private static void resetAllSettings(Context context, int mode) throws Exception {
        Exception res = null;
        ContentResolver resolver = context.getContentResolver();
        int i = 0;
        try {
            Global.resetToDefaultsAsUser(resolver, null, mode, 0);
        } catch (Throwable t) {
            res = new RuntimeException("Failed to reset global settings", t);
        }
        int[] allUserIds = getAllUserIds();
        int length = allUserIds.length;
        while (i < length) {
            int userId = allUserIds[i];
            try {
                Secure.resetToDefaultsAsUser(resolver, null, mode, userId);
            } catch (Throwable t2) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to reset secure settings for ");
                stringBuilder.append(userId);
                res = new RuntimeException(stringBuilder.toString(), t2);
            }
            i++;
        }
        if (res != null) {
            throw res;
        }
    }

    private static int[] getAllUserIds() {
        int[] userIds = new int[1];
        int i = 0;
        userIds[0] = 0;
        try {
            File[] listFilesOrEmpty = FileUtils.listFilesOrEmpty(Environment.getDataSystemDeDirectory());
            int length = listFilesOrEmpty.length;
            while (i < length) {
                try {
                    int userId = Integer.parseInt(listFilesOrEmpty[i].getName());
                    if (userId != 0) {
                        userIds = ArrayUtils.appendInt(userIds, userId);
                    }
                } catch (NumberFormatException e) {
                }
                i++;
            }
        } catch (Throwable t) {
            Slog.w(TAG, "Trouble discovering users", t);
        }
        return userIds;
    }

    private static boolean isUsbActive() {
        if (SystemProperties.getBoolean(PROP_VIRTUAL_DEVICE, false)) {
            Slog.v(TAG, "Assuming virtual device is connected over USB");
            return true;
        }
        try {
            return "CONFIGURED".equals(FileUtils.readTextFile(new File("/sys/class/android_usb/android0/state"), 128, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS).trim());
        } catch (Throwable t) {
            Slog.w(TAG, "Failed to determine if device was on USB", t);
            return false;
        }
    }

    private static String levelToString(int level) {
        switch (level) {
            case 0:
                return "NONE";
            case 1:
                return "RESET_SETTINGS_UNTRUSTED_DEFAULTS";
            case 2:
                return "RESET_SETTINGS_UNTRUSTED_CHANGES";
            case 3:
                return "RESET_SETTINGS_TRUSTED_DEFAULTS";
            case 4:
                return "FACTORY_RESET";
            default:
                return Integer.toString(level);
        }
    }
}
