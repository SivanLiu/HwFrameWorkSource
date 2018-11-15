package android.content.pm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;

public final class AuxiliaryResolveInfo extends IntentFilter {
    public final Intent failureIntent;
    public final ComponentName installFailureActivity;
    public final boolean needsPhaseTwo;
    public final String packageName;
    public final InstantAppResolveInfo resolveInfo;
    public final String splitName;
    public final String token;
    public final int versionCode;

    public AuxiliaryResolveInfo(InstantAppResolveInfo resolveInfo, IntentFilter orig, String splitName, String token, boolean needsPhase2, Intent failureIntent) {
        super(orig);
        this.resolveInfo = resolveInfo;
        this.packageName = resolveInfo.getPackageName();
        this.splitName = splitName;
        this.token = token;
        this.needsPhaseTwo = needsPhase2;
        this.versionCode = resolveInfo.getVersionCode();
        this.failureIntent = failureIntent;
        this.installFailureActivity = null;
    }

    public AuxiliaryResolveInfo(String packageName, String splitName, ComponentName failureActivity, int versionCode, Intent failureIntent) {
        this.packageName = packageName;
        this.installFailureActivity = failureActivity;
        this.splitName = splitName;
        this.versionCode = versionCode;
        this.resolveInfo = null;
        this.token = null;
        this.needsPhaseTwo = false;
        this.failureIntent = failureIntent;
    }
}
