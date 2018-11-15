package com.android.server.backup;

import android.content.ContentResolver;
import android.os.Handler;
import android.provider.Settings.Global;
import android.util.KeyValueListParser;
import android.util.KeyValueSettingObserver;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

public class BackupAgentTimeoutParameters extends KeyValueSettingObserver {
    @VisibleForTesting
    public static final long DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS = 300000;
    @VisibleForTesting
    public static final long DEFAULT_KV_BACKUP_AGENT_TIMEOUT_MILLIS = 30000;
    @VisibleForTesting
    public static final long DEFAULT_RESTORE_AGENT_FINISHED_TIMEOUT_MILLIS = 30000;
    @VisibleForTesting
    public static final long DEFAULT_RESTORE_AGENT_TIMEOUT_MILLIS = 60000;
    @VisibleForTesting
    public static final long DEFAULT_SHARED_BACKUP_AGENT_TIMEOUT_MILLIS = 1800000;
    @VisibleForTesting
    public static final String SETTING = "backup_agent_timeout_parameters";
    @VisibleForTesting
    public static final String SETTING_FULL_BACKUP_AGENT_TIMEOUT_MILLIS = "full_backup_agent_timeout_millis";
    @VisibleForTesting
    public static final String SETTING_KV_BACKUP_AGENT_TIMEOUT_MILLIS = "kv_backup_agent_timeout_millis";
    @VisibleForTesting
    public static final String SETTING_RESTORE_AGENT_FINISHED_TIMEOUT_MILLIS = "restore_agent_finished_timeout_millis";
    @VisibleForTesting
    public static final String SETTING_RESTORE_AGENT_TIMEOUT_MILLIS = "restore_agent_timeout_millis";
    @VisibleForTesting
    public static final String SETTING_SHARED_BACKUP_AGENT_TIMEOUT_MILLIS = "shared_backup_agent_timeout_millis";
    private static final String TAG = "BackupAgentTimeout";
    @GuardedBy("mLock")
    private long mFullBackupAgentTimeoutMillis;
    @GuardedBy("mLock")
    private long mKvBackupAgentTimeoutMillis;
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private long mRestoreAgentFinishedTimeoutMillis;
    @GuardedBy("mLock")
    private long mRestoreAgentTimeoutMillis;
    @GuardedBy("mLock")
    private long mSharedBackupAgentTimeoutMillis;

    public BackupAgentTimeoutParameters(Handler handler, ContentResolver resolver) {
        super(handler, resolver, Global.getUriFor(SETTING));
    }

    public String getSettingValue(ContentResolver resolver) {
        return Global.getString(resolver, SETTING);
    }

    public void update(KeyValueListParser parser) {
        synchronized (this.mLock) {
            this.mKvBackupAgentTimeoutMillis = parser.getLong(SETTING_KV_BACKUP_AGENT_TIMEOUT_MILLIS, 30000);
            this.mFullBackupAgentTimeoutMillis = parser.getLong(SETTING_FULL_BACKUP_AGENT_TIMEOUT_MILLIS, DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS);
            this.mSharedBackupAgentTimeoutMillis = parser.getLong(SETTING_SHARED_BACKUP_AGENT_TIMEOUT_MILLIS, 1800000);
            this.mRestoreAgentTimeoutMillis = parser.getLong(SETTING_RESTORE_AGENT_TIMEOUT_MILLIS, 60000);
            this.mRestoreAgentFinishedTimeoutMillis = parser.getLong(SETTING_RESTORE_AGENT_FINISHED_TIMEOUT_MILLIS, 30000);
        }
    }

    public long getKvBackupAgentTimeoutMillis() {
        long j;
        synchronized (this.mLock) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getKvBackupAgentTimeoutMillis(): ");
            stringBuilder.append(this.mKvBackupAgentTimeoutMillis);
            Slog.v(str, stringBuilder.toString());
            j = this.mKvBackupAgentTimeoutMillis;
        }
        return j;
    }

    public long getFullBackupAgentTimeoutMillis() {
        long j;
        synchronized (this.mLock) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getFullBackupAgentTimeoutMillis(): ");
            stringBuilder.append(this.mFullBackupAgentTimeoutMillis);
            Slog.v(str, stringBuilder.toString());
            j = this.mFullBackupAgentTimeoutMillis;
        }
        return j;
    }

    public long getSharedBackupAgentTimeoutMillis() {
        long j;
        synchronized (this.mLock) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getSharedBackupAgentTimeoutMillis(): ");
            stringBuilder.append(this.mSharedBackupAgentTimeoutMillis);
            Slog.v(str, stringBuilder.toString());
            j = this.mSharedBackupAgentTimeoutMillis;
        }
        return j;
    }

    public long getRestoreAgentTimeoutMillis() {
        long j;
        synchronized (this.mLock) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getRestoreAgentTimeoutMillis(): ");
            stringBuilder.append(this.mRestoreAgentTimeoutMillis);
            Slog.v(str, stringBuilder.toString());
            j = this.mRestoreAgentTimeoutMillis;
        }
        return j;
    }

    public long getRestoreAgentFinishedTimeoutMillis() {
        long j;
        synchronized (this.mLock) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getRestoreAgentFinishedTimeoutMillis(): ");
            stringBuilder.append(this.mRestoreAgentFinishedTimeoutMillis);
            Slog.v(str, stringBuilder.toString());
            j = this.mRestoreAgentFinishedTimeoutMillis;
        }
        return j;
    }
}
