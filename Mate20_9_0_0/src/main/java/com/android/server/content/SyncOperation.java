package com.android.server.content;

import android.accounts.Account;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.content.SyncStorageEngine.EndPoint;
import com.android.server.os.HwBootFail;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import java.util.Iterator;

public class SyncOperation {
    public static final int NO_JOB_ID = -1;
    public static final int REASON_ACCOUNTS_UPDATED = -2;
    public static final int REASON_BACKGROUND_DATA_SETTINGS_CHANGED = -1;
    public static final int REASON_IS_SYNCABLE = -5;
    public static final int REASON_MASTER_SYNC_AUTO = -7;
    private static String[] REASON_NAMES = new String[]{"DataSettingsChanged", "AccountsUpdated", "ServiceChanged", "Periodic", "IsSyncable", "AutoSync", "MasterSyncAuto", "UserStart"};
    public static final int REASON_PERIODIC = -4;
    public static final int REASON_SERVICE_CHANGED = -3;
    public static final int REASON_SYNC_AUTO = -6;
    public static final int REASON_USER_START = -8;
    public static final String TAG = "SyncManager";
    public final boolean allowParallelSyncs;
    public long expectedRuntime;
    public final Bundle extras;
    public final long flexMillis;
    public final boolean isPeriodic;
    public int jobId;
    public final String key;
    public final String owningPackage;
    public final int owningUid;
    public final long periodMillis;
    public final int reason;
    int retries;
    public final int sourcePeriodicId;
    public int syncExemptionFlag;
    public final int syncSource;
    public final EndPoint target;
    public String wakeLockName;

    public SyncOperation(Account account, int userId, int owningUid, String owningPackage, int reason, int source, String provider, Bundle extras, boolean allowParallelSyncs, int syncExemptionFlag) {
        this(new EndPoint(account, provider, userId), owningUid, owningPackage, reason, source, extras, allowParallelSyncs, syncExemptionFlag);
    }

    private SyncOperation(EndPoint info, int owningUid, String owningPackage, int reason, int source, Bundle extras, boolean allowParallelSyncs, int syncExemptionFlag) {
        this(info, owningUid, owningPackage, reason, source, extras, allowParallelSyncs, false, -1, 0, 0, syncExemptionFlag);
    }

    public SyncOperation(SyncOperation op, long periodMillis, long flexMillis) {
        SyncOperation syncOperation = op;
        this(syncOperation.target, syncOperation.owningUid, syncOperation.owningPackage, syncOperation.reason, syncOperation.syncSource, new Bundle(syncOperation.extras), syncOperation.allowParallelSyncs, syncOperation.isPeriodic, syncOperation.sourcePeriodicId, periodMillis, flexMillis, 0);
    }

    public SyncOperation(EndPoint info, int owningUid, String owningPackage, int reason, int source, Bundle extras, boolean allowParallelSyncs, boolean isPeriodic, int sourcePeriodicId, long periodMillis, long flexMillis, int syncExemptionFlag) {
        this.target = info;
        this.owningUid = owningUid;
        this.owningPackage = owningPackage;
        this.reason = reason;
        this.syncSource = source;
        this.extras = new Bundle(extras);
        this.allowParallelSyncs = allowParallelSyncs;
        this.isPeriodic = isPeriodic;
        this.sourcePeriodicId = sourcePeriodicId;
        this.periodMillis = periodMillis;
        this.flexMillis = flexMillis;
        this.jobId = -1;
        this.key = toKey();
        this.syncExemptionFlag = syncExemptionFlag;
    }

    public SyncOperation createOneTimeSyncOperation() {
        if (this.isPeriodic) {
            return new SyncOperation(this.target, this.owningUid, this.owningPackage, this.reason, this.syncSource, new Bundle(this.extras), this.allowParallelSyncs, false, this.jobId, this.periodMillis, this.flexMillis, 0);
        }
        return null;
    }

    public SyncOperation(SyncOperation other) {
        this.target = other.target;
        this.owningUid = other.owningUid;
        this.owningPackage = other.owningPackage;
        this.reason = other.reason;
        this.syncSource = other.syncSource;
        this.allowParallelSyncs = other.allowParallelSyncs;
        this.extras = new Bundle(other.extras);
        this.wakeLockName = other.wakeLockName();
        this.isPeriodic = other.isPeriodic;
        this.sourcePeriodicId = other.sourcePeriodicId;
        this.periodMillis = other.periodMillis;
        this.flexMillis = other.flexMillis;
        this.key = other.key;
        this.syncExemptionFlag = other.syncExemptionFlag;
    }

    PersistableBundle toJobInfoExtras() {
        PersistableBundle jobInfoExtras = new PersistableBundle();
        PersistableBundle syncExtrasBundle = new PersistableBundle();
        for (String key : this.extras.keySet()) {
            Account value = this.extras.get(key);
            if (value instanceof Account) {
                Account account = value;
                PersistableBundle accountBundle = new PersistableBundle();
                accountBundle.putString("accountName", account.name);
                accountBundle.putString("accountType", account.type);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ACCOUNT:");
                stringBuilder.append(key);
                jobInfoExtras.putPersistableBundle(stringBuilder.toString(), accountBundle);
            } else if (value instanceof Long) {
                syncExtrasBundle.putLong(key, ((Long) value).longValue());
            } else if (value instanceof Integer) {
                syncExtrasBundle.putInt(key, ((Integer) value).intValue());
            } else if (value instanceof Boolean) {
                syncExtrasBundle.putBoolean(key, ((Boolean) value).booleanValue());
            } else if (value instanceof Float) {
                syncExtrasBundle.putDouble(key, (double) ((Float) value).floatValue());
            } else if (value instanceof Double) {
                syncExtrasBundle.putDouble(key, ((Double) value).doubleValue());
            } else if (value instanceof String) {
                syncExtrasBundle.putString(key, (String) value);
            } else if (value == null) {
                syncExtrasBundle.putString(key, null);
            } else {
                Slog.e(TAG, "Unknown extra type.");
            }
        }
        jobInfoExtras.putPersistableBundle("syncExtras", syncExtrasBundle);
        jobInfoExtras.putBoolean("SyncManagerJob", true);
        jobInfoExtras.putString("provider", this.target.provider);
        jobInfoExtras.putString("accountName", this.target.account.name);
        jobInfoExtras.putString("accountType", this.target.account.type);
        jobInfoExtras.putInt("userId", this.target.userId);
        jobInfoExtras.putInt("owningUid", this.owningUid);
        jobInfoExtras.putString("owningPackage", this.owningPackage);
        jobInfoExtras.putInt(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY, this.reason);
        jobInfoExtras.putInt("source", this.syncSource);
        jobInfoExtras.putBoolean("allowParallelSyncs", this.allowParallelSyncs);
        jobInfoExtras.putInt("jobId", this.jobId);
        jobInfoExtras.putBoolean("isPeriodic", this.isPeriodic);
        jobInfoExtras.putInt("sourcePeriodicId", this.sourcePeriodicId);
        jobInfoExtras.putLong("periodMillis", this.periodMillis);
        jobInfoExtras.putLong("flexMillis", this.flexMillis);
        jobInfoExtras.putLong("expectedRuntime", this.expectedRuntime);
        jobInfoExtras.putInt("retries", this.retries);
        jobInfoExtras.putInt("syncExemptionFlag", this.syncExemptionFlag);
        return jobInfoExtras;
    }

    static SyncOperation maybeCreateFromJobExtras(PersistableBundle jobExtras) {
        PersistableBundle persistableBundle = jobExtras;
        if (persistableBundle == null || !persistableBundle.getBoolean("SyncManagerJob", false)) {
            return null;
        }
        String accountName = persistableBundle.getString("accountName");
        String accountType = persistableBundle.getString("accountType");
        String provider = persistableBundle.getString("provider");
        int userId = persistableBundle.getInt("userId", HwBootFail.STAGE_BOOT_SUCCESS);
        int owningUid = persistableBundle.getInt("owningUid");
        String owningPackage = persistableBundle.getString("owningPackage");
        int reason = persistableBundle.getInt(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY, HwBootFail.STAGE_BOOT_SUCCESS);
        int source = persistableBundle.getInt("source", HwBootFail.STAGE_BOOT_SUCCESS);
        boolean allowParallelSyncs = persistableBundle.getBoolean("allowParallelSyncs", false);
        boolean isPeriodic = persistableBundle.getBoolean("isPeriodic", false);
        int initiatedBy = persistableBundle.getInt("sourcePeriodicId", -1);
        long periodMillis = persistableBundle.getLong("periodMillis");
        long flexMillis = persistableBundle.getLong("flexMillis");
        int syncExemptionFlag = persistableBundle.getInt("syncExemptionFlag", 0);
        Bundle extras = new Bundle();
        PersistableBundle syncExtras = persistableBundle.getPersistableBundle("syncExtras");
        if (syncExtras != null) {
            extras.putAll(syncExtras);
        }
        Iterator it = jobExtras.keySet().iterator();
        while (it.hasNext()) {
            Iterator it2;
            String key = (String) it.next();
            if (key == null || !key.startsWith("ACCOUNT:")) {
                it2 = it;
            } else {
                String newKey = key.substring(8);
                PersistableBundle accountsBundle = persistableBundle.getPersistableBundle(key);
                it2 = it;
                extras.putParcelable(newKey, new Account(accountsBundle.getString("accountName"), accountsBundle.getString("accountType")));
            }
            it = it2;
        }
        Account account = new Account(accountName, accountType);
        SyncOperation op = new SyncOperation(new EndPoint(account, provider, userId), owningUid, owningPackage, reason, source, extras, allowParallelSyncs, isPeriodic, initiatedBy, periodMillis, flexMillis, syncExemptionFlag);
        op.jobId = persistableBundle.getInt("jobId");
        op.expectedRuntime = persistableBundle.getLong("expectedRuntime");
        op.retries = persistableBundle.getInt("retries");
        return op;
    }

    boolean isConflict(SyncOperation toRun) {
        EndPoint other = toRun.target;
        return this.target.account.type.equals(other.account.type) && this.target.provider.equals(other.provider) && this.target.userId == other.userId && (!this.allowParallelSyncs || this.target.account.name.equals(other.account.name));
    }

    boolean isReasonPeriodic() {
        return this.reason == -4;
    }

    boolean matchesPeriodicOperation(SyncOperation other) {
        if (this.target.matchesSpec(other.target) && SyncManager.syncExtrasEquals(this.extras, other.extras, true) && this.periodMillis == other.periodMillis && this.flexMillis == other.flexMillis) {
            return true;
        }
        return false;
    }

    boolean isDerivedFromFailedPeriodicSync() {
        return this.sourcePeriodicId != -1;
    }

    int findPriority() {
        if (isInitialization()) {
            return 20;
        }
        if (isExpedited()) {
            return 10;
        }
        return 0;
    }

    private String toKey() {
        StringBuilder sb = new StringBuilder();
        sb.append("provider: ");
        sb.append(this.target.provider);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" account {name=");
        stringBuilder.append(this.target.account.name);
        stringBuilder.append(", user=");
        stringBuilder.append(this.target.userId);
        stringBuilder.append(", type=");
        stringBuilder.append(this.target.account.type);
        stringBuilder.append("}");
        sb.append(stringBuilder.toString());
        sb.append(" isPeriodic: ");
        sb.append(this.isPeriodic);
        sb.append(" period: ");
        sb.append(this.periodMillis);
        sb.append(" flex: ");
        sb.append(this.flexMillis);
        sb.append(" extras: ");
        extrasToStringBuilder(this.extras, sb);
        return sb.toString();
    }

    public String toString() {
        return dump(null, true, null);
    }

    String dump(PackageManager pm, boolean shorter, SyncAdapterStateFetcher appStates) {
        StringBuilder sb = new StringBuilder();
        sb.append("JobId: ");
        sb.append(this.jobId);
        sb.append(", ");
        sb.append("XXXXXXXXX");
        sb.append(" u");
        sb.append(this.target.userId);
        sb.append(" (");
        sb.append(this.target.account.type);
        sb.append(" u");
        sb.append(this.target.userId);
        sb.append(" [");
        sb.append(this.target.provider);
        sb.append("] ");
        sb.append(SyncStorageEngine.SOURCES[this.syncSource]);
        if (this.expectedRuntime != 0) {
            sb.append(" ExpectedIn=");
            SyncManager.formatDurationHMS(sb, this.expectedRuntime - SystemClock.elapsedRealtime());
        }
        if (this.extras.getBoolean("expedited", false)) {
            sb.append(" EXPEDITED");
        }
        switch (this.syncExemptionFlag) {
            case 0:
                break;
            case 1:
                sb.append(" STANDBY-EXEMPTED");
                break;
            case 2:
                sb.append(" STANDBY-EXEMPTED(TOP)");
                break;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" ExemptionFlag=");
                stringBuilder.append(this.syncExemptionFlag);
                sb.append(stringBuilder.toString());
                break;
        }
        sb.append(" Reason=");
        sb.append(reasonToString(pm, this.reason));
        if (this.isPeriodic) {
            sb.append(" (period=");
            SyncManager.formatDurationHMS(sb, this.periodMillis);
            sb.append(" flex=");
            SyncManager.formatDurationHMS(sb, this.flexMillis);
            sb.append(")");
        }
        if (this.retries > 0) {
            sb.append(" Retries=");
            sb.append(this.retries);
        }
        if (!shorter) {
            sb.append(" Owner={");
            UserHandle.formatUid(sb, this.owningUid);
            sb.append(" ");
            sb.append(this.owningPackage);
            if (appStates != null) {
                sb.append(" [");
                sb.append(appStates.getStandbyBucket(UserHandle.getUserId(this.owningUid), this.owningPackage));
                sb.append("]");
                if (appStates.isAppActive(this.owningUid)) {
                    sb.append(" [ACTIVE]");
                }
            }
            sb.append("}");
            if (!this.extras.keySet().isEmpty()) {
                sb.append(" ");
                extrasToStringBuilder(this.extras, sb);
            }
        }
        return sb.toString();
    }

    static String reasonToString(PackageManager pm, int reason) {
        if (reason < 0) {
            int index = (-reason) - 1;
            if (index >= REASON_NAMES.length) {
                return String.valueOf(reason);
            }
            return REASON_NAMES[index];
        } else if (pm == null) {
            return String.valueOf(reason);
        } else {
            String[] packages = pm.getPackagesForUid(reason);
            if (packages != null && packages.length == 1) {
                return packages[0];
            }
            String name = pm.getNameForUid(reason);
            if (name != null) {
                return name;
            }
            return String.valueOf(reason);
        }
    }

    boolean isInitialization() {
        return this.extras.getBoolean("initialize", false);
    }

    boolean isExpedited() {
        return this.extras.getBoolean("expedited", false);
    }

    boolean ignoreBackoff() {
        return this.extras.getBoolean("ignore_backoff", false);
    }

    boolean isNotAllowedOnMetered() {
        return this.extras.getBoolean("allow_metered", false);
    }

    boolean isManual() {
        return this.extras.getBoolean("force", false);
    }

    boolean isIgnoreSettings() {
        return this.extras.getBoolean("ignore_settings", false);
    }

    boolean isAppStandbyExempted() {
        return this.syncExemptionFlag != 0;
    }

    static void extrasToStringBuilder(Bundle bundle, StringBuilder sb) {
        if (bundle == null) {
            sb.append("null");
            return;
        }
        sb.append("[");
        for (String key : bundle.keySet()) {
            sb.append(key);
            sb.append("=");
            sb.append(bundle.get(key));
            sb.append(" ");
        }
        sb.append("]");
    }

    static String extrasToString(Bundle bundle) {
        StringBuilder sb = new StringBuilder();
        extrasToStringBuilder(bundle, sb);
        return sb.toString();
    }

    String wakeLockName() {
        if (this.wakeLockName != null) {
            return this.wakeLockName;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.target.provider);
        stringBuilder.append(SliceAuthority.DELIMITER);
        stringBuilder.append(this.target.account.type);
        String stringBuilder2 = stringBuilder.toString();
        this.wakeLockName = stringBuilder2;
        return stringBuilder2;
    }

    public Object[] toEventLog(int event) {
        return new Object[]{Integer.valueOf(event), Integer.valueOf(this.syncSource), this.target.provider, Integer.valueOf(this.target.account.name.hashCode())};
    }
}
