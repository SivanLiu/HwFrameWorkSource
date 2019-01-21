package com.android.server.timezone;

import android.app.timezone.RulesUpdaterContract;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.os.FileUtils;
import android.os.SystemClock;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.annotations.VisibleForTesting.Visibility;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Clock;

@VisibleForTesting(visibility = Visibility.PACKAGE)
public class PackageTracker {
    private static final String TAG = "timezone.PackageTracker";
    private int mCheckFailureCount;
    private int mCheckTimeAllowedMillis;
    private boolean mCheckTriggered;
    private final ConfigHelper mConfigHelper;
    private String mDataAppPackageName;
    private int mDelayBeforeReliabilityCheckMillis;
    private final Clock mElapsedRealtimeClock;
    private long mFailedCheckRetryCount;
    private final PackageTrackerIntentHelper mIntentHelper;
    private Long mLastTriggerTimestamp = null;
    private final PackageManagerHelper mPackageManagerHelper;
    private final PackageStatusStorage mPackageStatusStorage;
    private boolean mTrackingEnabled;
    private String mUpdateAppPackageName;

    static PackageTracker create(Context context) {
        Clock elapsedRealtimeClock = SystemClock.elapsedRealtimeClock();
        PackageTrackerHelperImpl helperImpl = new PackageTrackerHelperImpl(context);
        return new PackageTracker(elapsedRealtimeClock, helperImpl, helperImpl, new PackageStatusStorage(FileUtils.createDir(Environment.getDataSystemDirectory(), "timezone")), new PackageTrackerIntentHelperImpl(context));
    }

    PackageTracker(Clock elapsedRealtimeClock, ConfigHelper configHelper, PackageManagerHelper packageManagerHelper, PackageStatusStorage packageStatusStorage, PackageTrackerIntentHelper intentHelper) {
        this.mElapsedRealtimeClock = elapsedRealtimeClock;
        this.mConfigHelper = configHelper;
        this.mPackageManagerHelper = packageManagerHelper;
        this.mPackageStatusStorage = packageStatusStorage;
        this.mIntentHelper = intentHelper;
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    protected synchronized boolean start() {
        this.mTrackingEnabled = this.mConfigHelper.isTrackingEnabled();
        if (this.mTrackingEnabled) {
            this.mUpdateAppPackageName = this.mConfigHelper.getUpdateAppPackageName();
            this.mDataAppPackageName = this.mConfigHelper.getDataAppPackageName();
            this.mCheckTimeAllowedMillis = this.mConfigHelper.getCheckTimeAllowedMillis();
            this.mFailedCheckRetryCount = (long) this.mConfigHelper.getFailedCheckRetryCount();
            this.mDelayBeforeReliabilityCheckMillis = this.mCheckTimeAllowedMillis + 60000;
            throwIfDeviceSettingsOrAppsAreBad();
            this.mCheckTriggered = false;
            this.mCheckFailureCount = 0;
            try {
                this.mPackageStatusStorage.initialize();
                this.mIntentHelper.initialize(this.mUpdateAppPackageName, this.mDataAppPackageName, this);
                this.mIntentHelper.scheduleReliabilityTrigger((long) this.mDelayBeforeReliabilityCheckMillis);
                Slog.i(TAG, "Time zone updater / data package tracking enabled");
                return true;
            } catch (IOException e) {
                Slog.w(TAG, "PackageTracker storage could not be initialized.", e);
                return false;
            }
        }
        Slog.i(TAG, "Time zone updater / data package tracking explicitly disabled.");
        return false;
    }

    private void throwIfDeviceSettingsOrAppsAreBad() {
        throwRuntimeExceptionIfNullOrEmpty(this.mUpdateAppPackageName, "Update app package name missing.");
        throwRuntimeExceptionIfNullOrEmpty(this.mDataAppPackageName, "Data app package name missing.");
        StringBuilder stringBuilder;
        if (this.mFailedCheckRetryCount < 1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("mFailedRetryCount=");
            stringBuilder.append(this.mFailedCheckRetryCount);
            throw logAndThrowRuntimeException(stringBuilder.toString(), null);
        } else if (this.mCheckTimeAllowedMillis >= 1000) {
            StringBuilder stringBuilder2;
            try {
                if (this.mPackageManagerHelper.isPrivilegedApp(this.mUpdateAppPackageName)) {
                    String str = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Update app ");
                    stringBuilder3.append(this.mUpdateAppPackageName);
                    stringBuilder3.append(" is valid.");
                    Slog.d(str, stringBuilder3.toString());
                    try {
                        if (this.mPackageManagerHelper.isPrivilegedApp(this.mDataAppPackageName)) {
                            str = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Data app ");
                            stringBuilder2.append(this.mDataAppPackageName);
                            stringBuilder2.append(" is valid.");
                            Slog.d(str, stringBuilder2.toString());
                            return;
                        }
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Data app ");
                        stringBuilder.append(this.mDataAppPackageName);
                        stringBuilder.append(" must be a priv-app.");
                        throw logAndThrowRuntimeException(stringBuilder.toString(), null);
                    } catch (NameNotFoundException e) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Could not determine data app package details for ");
                        stringBuilder2.append(this.mDataAppPackageName);
                        throw logAndThrowRuntimeException(stringBuilder2.toString(), e);
                    }
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Update app ");
                stringBuilder.append(this.mUpdateAppPackageName);
                stringBuilder.append(" must be a priv-app.");
                throw logAndThrowRuntimeException(stringBuilder.toString(), null);
            } catch (NameNotFoundException e2) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Could not determine update app package details for ");
                stringBuilder2.append(this.mUpdateAppPackageName);
                throw logAndThrowRuntimeException(stringBuilder2.toString(), e2);
            }
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("mCheckTimeAllowedMillis=");
            stringBuilder.append(this.mCheckTimeAllowedMillis);
            throw logAndThrowRuntimeException(stringBuilder.toString(), null);
        }
    }

    public synchronized void triggerUpdateIfNeeded(boolean packageChanged) {
        if (this.mTrackingEnabled) {
            boolean updaterAppManifestValid = validateUpdaterAppManifest();
            boolean dataAppManifestValid = validateDataAppManifest();
            if (updaterAppManifestValid) {
                if (dataAppManifestValid) {
                    if (!packageChanged) {
                        if (!this.mCheckTriggered) {
                            Slog.d(TAG, "triggerUpdateIfNeeded: First reliability trigger.");
                        } else if (isCheckInProgress()) {
                            if (!isCheckResponseOverdue()) {
                                Slog.d(TAG, "triggerUpdateIfNeeded: checkComplete call is not yet overdue. Not triggering.");
                                this.mIntentHelper.scheduleReliabilityTrigger((long) this.mDelayBeforeReliabilityCheckMillis);
                                return;
                            }
                        } else if (((long) this.mCheckFailureCount) > this.mFailedCheckRetryCount) {
                            Slog.i(TAG, "triggerUpdateIfNeeded: number of allowed consecutive check failures exceeded. Stopping reliability triggers until next reboot or package update.");
                            this.mIntentHelper.unscheduleReliabilityTrigger();
                            return;
                        } else if (this.mCheckFailureCount == 0) {
                            Slog.i(TAG, "triggerUpdateIfNeeded: No reliability check required. Last check was successful.");
                            this.mIntentHelper.unscheduleReliabilityTrigger();
                            return;
                        }
                    }
                    PackageVersions currentInstalledVersions = lookupInstalledPackageVersions();
                    if (currentInstalledVersions == null) {
                        Slog.e(TAG, "triggerUpdateIfNeeded: currentInstalledVersions was null");
                        this.mIntentHelper.unscheduleReliabilityTrigger();
                        return;
                    }
                    PackageStatus packageStatus = this.mPackageStatusStorage.getPackageStatus();
                    String str;
                    StringBuilder stringBuilder;
                    if (packageStatus == null) {
                        Slog.i(TAG, "triggerUpdateIfNeeded: No package status data found. Data check needed.");
                    } else if (packageStatus.mVersions.equals(currentInstalledVersions)) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("triggerUpdateIfNeeded: Stored package versions match currently installed versions, currentInstalledVersions=");
                        stringBuilder.append(currentInstalledVersions);
                        stringBuilder.append(", packageStatus.mCheckStatus=");
                        stringBuilder.append(packageStatus.mCheckStatus);
                        Slog.i(str, stringBuilder.toString());
                        if (packageStatus.mCheckStatus == 2) {
                            Slog.i(TAG, "triggerUpdateIfNeeded: Prior check succeeded. No need to trigger.");
                            this.mIntentHelper.unscheduleReliabilityTrigger();
                            return;
                        }
                    } else {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("triggerUpdateIfNeeded: Stored package versions=");
                        stringBuilder.append(packageStatus.mVersions);
                        stringBuilder.append(", do not match current package versions=");
                        stringBuilder.append(currentInstalledVersions);
                        stringBuilder.append(". Triggering check.");
                        Slog.i(str, stringBuilder.toString());
                    }
                    CheckToken checkToken = this.mPackageStatusStorage.generateCheckToken(currentInstalledVersions);
                    if (checkToken == null) {
                        Slog.w(TAG, "triggerUpdateIfNeeded: Unable to generate check token. Not sending check request.");
                        this.mIntentHelper.scheduleReliabilityTrigger((long) this.mDelayBeforeReliabilityCheckMillis);
                        return;
                    }
                    this.mIntentHelper.sendTriggerUpdateCheck(checkToken);
                    this.mCheckTriggered = true;
                    setCheckInProgress();
                    this.mIntentHelper.scheduleReliabilityTrigger((long) this.mDelayBeforeReliabilityCheckMillis);
                    return;
                }
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("No update triggered due to invalid application manifest entries. updaterApp=");
            stringBuilder2.append(updaterAppManifestValid);
            stringBuilder2.append(", dataApp=");
            stringBuilder2.append(dataAppManifestValid);
            Slog.e(str2, stringBuilder2.toString());
            this.mIntentHelper.unscheduleReliabilityTrigger();
            return;
        }
        throw new IllegalStateException("Unexpected call. Tracking is disabled.");
    }

    /* JADX WARNING: Missing block: B:22:0x00c9, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @VisibleForTesting(visibility = Visibility.PACKAGE)
    protected synchronized void recordCheckResult(CheckToken checkToken, boolean success) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("recordOperationResult: checkToken=");
        stringBuilder.append(checkToken);
        stringBuilder.append(" success=");
        stringBuilder.append(success);
        Slog.i(str, stringBuilder.toString());
        if (!this.mTrackingEnabled) {
            if (checkToken == null) {
                Slog.d(TAG, "recordCheckResult: Tracking is disabled and no token has been provided. Resetting tracking state.");
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("recordCheckResult: Tracking is disabled and a token ");
                stringBuilder.append(checkToken);
                stringBuilder.append(" has been unexpectedly provided. Resetting tracking state.");
                Slog.w(str, stringBuilder.toString());
            }
            this.mPackageStatusStorage.resetCheckState();
        } else if (checkToken == null) {
            Slog.i(TAG, "recordCheckResult: Unexpectedly missing checkToken, resetting storage state.");
            this.mPackageStatusStorage.resetCheckState();
            this.mIntentHelper.scheduleReliabilityTrigger((long) this.mDelayBeforeReliabilityCheckMillis);
            this.mCheckFailureCount = 0;
        } else if (this.mPackageStatusStorage.markChecked(checkToken, success)) {
            setCheckComplete();
            if (success) {
                this.mIntentHelper.unscheduleReliabilityTrigger();
                this.mCheckFailureCount = 0;
            } else {
                this.mIntentHelper.scheduleReliabilityTrigger((long) this.mDelayBeforeReliabilityCheckMillis);
                this.mCheckFailureCount++;
            }
        } else {
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("recordCheckResult: could not update token=");
            stringBuilder2.append(checkToken);
            stringBuilder2.append(" with success=");
            stringBuilder2.append(success);
            stringBuilder2.append(". Optimistic lock failure");
            Slog.i(str, stringBuilder2.toString());
            this.mIntentHelper.scheduleReliabilityTrigger((long) this.mDelayBeforeReliabilityCheckMillis);
            this.mCheckFailureCount++;
        }
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    protected int getCheckFailureCountForTests() {
        return this.mCheckFailureCount;
    }

    private void setCheckInProgress() {
        this.mLastTriggerTimestamp = Long.valueOf(this.mElapsedRealtimeClock.millis());
    }

    private void setCheckComplete() {
        this.mLastTriggerTimestamp = null;
    }

    private boolean isCheckInProgress() {
        return this.mLastTriggerTimestamp != null;
    }

    private boolean isCheckResponseOverdue() {
        boolean z = false;
        if (this.mLastTriggerTimestamp == null) {
            return false;
        }
        if (this.mElapsedRealtimeClock.millis() > this.mLastTriggerTimestamp.longValue() + ((long) this.mCheckTimeAllowedMillis)) {
            z = true;
        }
        return z;
    }

    private PackageVersions lookupInstalledPackageVersions() {
        try {
            return new PackageVersions(this.mPackageManagerHelper.getInstalledPackageVersion(this.mUpdateAppPackageName), this.mPackageManagerHelper.getInstalledPackageVersion(this.mDataAppPackageName));
        } catch (NameNotFoundException e) {
            Slog.w(TAG, "lookupInstalledPackageVersions: Unable to resolve installed package versions", e);
            return null;
        }
    }

    private boolean validateDataAppManifest() {
        if (this.mPackageManagerHelper.contentProviderRegistered("com.android.timezone", this.mDataAppPackageName)) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("validateDataAppManifest: Data app ");
        stringBuilder.append(this.mDataAppPackageName);
        stringBuilder.append(" does not expose the required provider with authority=");
        stringBuilder.append("com.android.timezone");
        Slog.w(str, stringBuilder.toString());
        return false;
    }

    private boolean validateUpdaterAppManifest() {
        try {
            if (!this.mPackageManagerHelper.usesPermission(this.mUpdateAppPackageName, "android.permission.UPDATE_TIME_ZONE_RULES")) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("validateUpdaterAppManifest: Updater app ");
                stringBuilder.append(this.mDataAppPackageName);
                stringBuilder.append(" does not use permission=");
                stringBuilder.append("android.permission.UPDATE_TIME_ZONE_RULES");
                Slog.w(str, stringBuilder.toString());
                return false;
            } else if (this.mPackageManagerHelper.receiverRegistered(RulesUpdaterContract.createUpdaterIntent(this.mUpdateAppPackageName), "android.permission.TRIGGER_TIME_ZONE_RULES_CHECK")) {
                return true;
            } else {
                return false;
            }
        } catch (NameNotFoundException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("validateUpdaterAppManifest: Updater app ");
            stringBuilder2.append(this.mDataAppPackageName);
            stringBuilder2.append(" does not expose the required broadcast receiver.");
            Slog.w(str2, stringBuilder2.toString(), e);
            return false;
        }
    }

    private static void throwRuntimeExceptionIfNullOrEmpty(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw logAndThrowRuntimeException(message, null);
        }
    }

    private static RuntimeException logAndThrowRuntimeException(String message, Throwable cause) {
        Slog.wtf(TAG, message, cause);
        throw new RuntimeException(message, cause);
    }

    public void dump(PrintWriter fout) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PackageTrackerState: ");
        stringBuilder.append(toString());
        fout.println(stringBuilder.toString());
        this.mPackageStatusStorage.dump(fout);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PackageTracker{mTrackingEnabled=");
        stringBuilder.append(this.mTrackingEnabled);
        stringBuilder.append(", mUpdateAppPackageName='");
        stringBuilder.append(this.mUpdateAppPackageName);
        stringBuilder.append('\'');
        stringBuilder.append(", mDataAppPackageName='");
        stringBuilder.append(this.mDataAppPackageName);
        stringBuilder.append('\'');
        stringBuilder.append(", mCheckTimeAllowedMillis=");
        stringBuilder.append(this.mCheckTimeAllowedMillis);
        stringBuilder.append(", mDelayBeforeReliabilityCheckMillis=");
        stringBuilder.append(this.mDelayBeforeReliabilityCheckMillis);
        stringBuilder.append(", mFailedCheckRetryCount=");
        stringBuilder.append(this.mFailedCheckRetryCount);
        stringBuilder.append(", mLastTriggerTimestamp=");
        stringBuilder.append(this.mLastTriggerTimestamp);
        stringBuilder.append(", mCheckTriggered=");
        stringBuilder.append(this.mCheckTriggered);
        stringBuilder.append(", mCheckFailureCount=");
        stringBuilder.append(this.mCheckFailureCount);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
