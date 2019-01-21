package android.database.sqlite;

import android.app.ActivityThread;
import android.app.Application;
import android.provider.Settings.Global;
import android.text.TextUtils;
import android.util.KeyValueListParser;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;

public class SQLiteCompatibilityWalFlags {
    private static final String TAG = "SQLiteCompatibilityWalFlags";
    private static volatile boolean sCallingGlobalSettings;
    private static volatile boolean sCompatibilityWalSupported;
    private static volatile boolean sFlagsSet;
    private static volatile boolean sInitialized;
    private static volatile String sWALSyncMode;

    @VisibleForTesting
    public static boolean areFlagsSet() {
        initIfNeeded();
        return sFlagsSet;
    }

    @VisibleForTesting
    public static boolean isCompatibilityWalSupported() {
        initIfNeeded();
        return sCompatibilityWalSupported;
    }

    @VisibleForTesting
    public static String getWALSyncMode() {
        initIfNeeded();
        return sWALSyncMode;
    }

    private static void initIfNeeded() {
        if (!sInitialized && !sCallingGlobalSettings) {
            ActivityThread activityThread = ActivityThread.currentActivityThread();
            Application app = activityThread == null ? null : activityThread.getApplication();
            String flags = null;
            if (app == null) {
                Log.w(TAG, "Cannot read global setting sqlite_compatibility_wal_flags - Application state not available");
            } else {
                try {
                    sCallingGlobalSettings = true;
                    flags = Global.getString(app.getContentResolver(), "sqlite_compatibility_wal_flags");
                } finally {
                    sCallingGlobalSettings = false;
                }
            }
            init(flags);
        }
    }

    @VisibleForTesting
    public static void init(String flags) {
        if (TextUtils.isEmpty(flags)) {
            sInitialized = true;
            return;
        }
        KeyValueListParser parser = new KeyValueListParser(',');
        try {
            parser.setString(flags);
            sCompatibilityWalSupported = parser.getBoolean("compatibility_wal_supported", SQLiteGlobal.isCompatibilityWalSupported());
            sWALSyncMode = parser.getString("wal_syncmode", SQLiteGlobal.getWALSyncMode());
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Read compatibility WAL flags: compatibility_wal_supported=");
            stringBuilder.append(sCompatibilityWalSupported);
            stringBuilder.append(", wal_syncmode=");
            stringBuilder.append(sWALSyncMode);
            Log.i(str, stringBuilder.toString());
            sFlagsSet = true;
            sInitialized = true;
        } catch (IllegalArgumentException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Setting has invalid format: ");
            stringBuilder2.append(flags);
            Log.e(str2, stringBuilder2.toString(), e);
            sInitialized = true;
        }
    }

    @VisibleForTesting
    public static void reset() {
        sInitialized = false;
        sFlagsSet = false;
        sCompatibilityWalSupported = false;
        sWALSyncMode = null;
    }
}
