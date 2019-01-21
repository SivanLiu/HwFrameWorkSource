package com.android.server.backup;

import android.app.AppGlobals;
import android.app.backup.BlobBackupHelper;
import android.content.pm.IPackageManager;
import android.util.Slog;

public class PreferredActivityBackupHelper extends BlobBackupHelper {
    private static final boolean DEBUG = false;
    private static final String KEY_DEFAULT_APPS = "default-apps";
    private static final String KEY_INTENT_VERIFICATION = "intent-verification";
    private static final String KEY_PREFERRED = "preferred-activity";
    private static final int STATE_VERSION = 3;
    private static final String TAG = "PreferredBackup";

    public PreferredActivityBackupHelper() {
        super(3, new String[]{KEY_PREFERRED, KEY_DEFAULT_APPS, KEY_INTENT_VERIFICATION});
    }

    protected byte[] getBackupPayload(String key) {
        IPackageManager pm = AppGlobals.getPackageManager();
        int i = -1;
        try {
            int hashCode = key.hashCode();
            if (hashCode != -696985986) {
                if (hashCode != -429170260) {
                    if (hashCode == 1336142555) {
                        if (key.equals(KEY_PREFERRED)) {
                            i = 0;
                        }
                    }
                } else if (key.equals(KEY_INTENT_VERIFICATION)) {
                    i = 2;
                }
            } else if (key.equals(KEY_DEFAULT_APPS)) {
                i = 1;
            }
            switch (i) {
                case 0:
                    return pm.getPreferredActivityBackup(0);
                case 1:
                    return pm.getDefaultAppsBackup(0);
                case 2:
                    return pm.getIntentFilterVerificationBackup(0);
                default:
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unexpected backup key ");
                    stringBuilder.append(key);
                    Slog.w(str, stringBuilder.toString());
                    break;
            }
        } catch (Exception e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unable to store payload ");
            stringBuilder2.append(key);
            Slog.e(str2, stringBuilder2.toString());
        }
        return null;
    }

    protected void applyRestoredPayload(String key, byte[] payload) {
        IPackageManager pm = AppGlobals.getPackageManager();
        int i = -1;
        try {
            int hashCode = key.hashCode();
            if (hashCode != -696985986) {
                if (hashCode != -429170260) {
                    if (hashCode == 1336142555) {
                        if (key.equals(KEY_PREFERRED)) {
                            i = 0;
                        }
                    }
                } else if (key.equals(KEY_INTENT_VERIFICATION)) {
                    i = 2;
                }
            } else if (key.equals(KEY_DEFAULT_APPS)) {
                i = 1;
            }
            switch (i) {
                case 0:
                    pm.restorePreferredActivities(payload, 0);
                    return;
                case 1:
                    pm.restoreDefaultApps(payload, 0);
                    return;
                case 2:
                    pm.restoreIntentFilterVerification(payload, 0);
                    return;
                default:
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unexpected restore key ");
                    stringBuilder.append(key);
                    Slog.w(str, stringBuilder.toString());
                    return;
            }
        } catch (Exception e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unable to restore key ");
            stringBuilder2.append(key);
            Slog.w(str2, stringBuilder2.toString());
        }
    }
}
