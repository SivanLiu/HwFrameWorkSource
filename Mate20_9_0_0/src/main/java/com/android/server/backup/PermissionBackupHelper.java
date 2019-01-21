package com.android.server.backup;

import android.app.AppGlobals;
import android.app.backup.BlobBackupHelper;
import android.content.pm.IPackageManager;
import android.util.Slog;

public class PermissionBackupHelper extends BlobBackupHelper {
    private static final boolean DEBUG = false;
    private static final String KEY_PERMISSIONS = "permissions";
    private static final int STATE_VERSION = 1;
    private static final String TAG = "PermissionBackup";

    public PermissionBackupHelper() {
        super(1, new String[]{KEY_PERMISSIONS});
    }

    protected byte[] getBackupPayload(String key) {
        IPackageManager pm = AppGlobals.getPackageManager();
        int i = -1;
        try {
            if (key.hashCode() == 1133704324) {
                if (key.equals(KEY_PERMISSIONS)) {
                    i = 0;
                }
            }
            if (i == 0) {
                return pm.getPermissionGrantBackup(0);
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected backup key ");
            stringBuilder.append(key);
            Slog.w(str, stringBuilder.toString());
            return null;
        } catch (Exception e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unable to store payload ");
            stringBuilder2.append(key);
            Slog.e(str2, stringBuilder2.toString());
        }
    }

    protected void applyRestoredPayload(String key, byte[] payload) {
        IPackageManager pm = AppGlobals.getPackageManager();
        int i = -1;
        try {
            if (key.hashCode() == 1133704324) {
                if (key.equals(KEY_PERMISSIONS)) {
                    i = 0;
                }
            }
            if (i != 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected restore key ");
                stringBuilder.append(key);
                Slog.w(str, stringBuilder.toString());
                return;
            }
            pm.restorePermissionGrants(payload, 0);
        } catch (Exception e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unable to restore key ");
            stringBuilder2.append(key);
            Slog.w(str2, stringBuilder2.toString());
        }
    }
}
