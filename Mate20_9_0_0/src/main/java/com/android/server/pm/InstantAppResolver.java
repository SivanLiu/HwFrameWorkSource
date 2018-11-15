package com.android.server.pm;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.AuthorityEntry;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.AuxiliaryResolveInfo;
import android.content.pm.AuxiliaryResolveInfo.AuxiliaryFilter;
import android.content.pm.InstantAppIntentFilter;
import android.content.pm.InstantAppRequest;
import android.content.pm.InstantAppResolveInfo;
import android.content.pm.InstantAppResolveInfo.InstantAppDigest;
import android.metrics.LogMaker;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;
import com.android.internal.logging.MetricsLogger;
import com.android.server.pm.InstantAppResolverConnection.ConnectionException;
import com.android.server.pm.InstantAppResolverConnection.PhaseTwoCallback;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public abstract class InstantAppResolver {
    private static final boolean DEBUG_INSTANT = Build.IS_DEBUGGABLE;
    private static final int RESOLUTION_BIND_TIMEOUT = 2;
    private static final int RESOLUTION_CALL_TIMEOUT = 3;
    private static final int RESOLUTION_FAILURE = 1;
    private static final int RESOLUTION_SUCCESS = 0;
    private static final String TAG = "PackageManager";
    private static MetricsLogger sMetricsLogger;

    @Retention(RetentionPolicy.SOURCE)
    public @interface ResolutionStatus {
    }

    private static MetricsLogger getLogger() {
        if (sMetricsLogger == null) {
            sMetricsLogger = new MetricsLogger();
        }
        return sMetricsLogger;
    }

    public static Intent sanitizeIntent(Intent origIntent) {
        Uri sanitizedUri;
        Intent sanitizedIntent = new Intent(origIntent.getAction());
        Set<String> categories = origIntent.getCategories();
        if (categories != null) {
            for (String category : categories) {
                sanitizedIntent.addCategory(category);
            }
        }
        if (origIntent.getData() == null) {
            sanitizedUri = null;
        } else {
            sanitizedUri = Uri.fromParts(origIntent.getScheme(), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        }
        sanitizedIntent.setDataAndType(sanitizedUri, origIntent.getType());
        sanitizedIntent.addFlags(origIntent.getFlags());
        sanitizedIntent.setPackage(origIntent.getPackage());
        return sanitizedIntent;
    }

    /* JADX WARNING: Removed duplicated region for block: B:35:0x00ae  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x0092  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x0092  */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x00ae  */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x0108  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0075  */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0070  */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x00ae  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x0092  */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x0108  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static AuxiliaryResolveInfo doInstantAppResolutionPhaseOne(InstantAppResolverConnection connection, InstantAppRequest requestObj) {
        InstantAppRequest instantAppRequest = requestObj;
        long startTime = System.currentTimeMillis();
        String token = UUID.randomUUID().toString();
        if (DEBUG_INSTANT) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[");
            stringBuilder.append(token);
            stringBuilder.append("] Phase1; resolving");
            Log.d(str, stringBuilder.toString());
        }
        Intent origIntent = instantAppRequest.origIntent;
        AuxiliaryResolveInfo resolveInfo = null;
        int resolutionStatus = 0;
        int i;
        int resolutionStatus2;
        try {
            List<InstantAppResolveInfo> instantAppResolveInfoList = connection.getInstantAppResolveInfoList(sanitizeIntent(origIntent), instantAppRequest.digest.getDigestPrefixSecure(), token);
            if (instantAppResolveInfoList == null || instantAppResolveInfoList.size() <= 0) {
                i = 2;
                resolutionStatus2 = resolutionStatus;
                if (instantAppRequest.resolveForStart && resolutionStatus2 == 0) {
                    logMetrics(899, startTime, token, resolutionStatus2);
                }
                if (DEBUG_INSTANT && resolveInfo == null) {
                    String str2;
                    StringBuilder stringBuilder2;
                    if (resolutionStatus2 != i) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("[");
                        stringBuilder2.append(token);
                        stringBuilder2.append("] Phase1; bind timed out");
                        Log.d(str2, stringBuilder2.toString());
                    } else if (resolutionStatus2 == 3) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("[");
                        stringBuilder2.append(token);
                        stringBuilder2.append("] Phase1; call timed out");
                        Log.d(str2, stringBuilder2.toString());
                    } else if (resolutionStatus2 != 0) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("[");
                        stringBuilder2.append(token);
                        stringBuilder2.append("] Phase1; service connection error");
                        Log.d(str2, stringBuilder2.toString());
                    } else {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("[");
                        stringBuilder2.append(token);
                        stringBuilder2.append("] Phase1; No results matched");
                        Log.d(str2, stringBuilder2.toString());
                    }
                }
                if (resolveInfo == null || (origIntent.getFlags() & 2048) == 0) {
                    return resolveInfo;
                }
                return new AuxiliaryResolveInfo(token, false, createFailureIntent(origIntent, token), null);
            }
            i = 2;
            try {
                resolveInfo = filterInstantAppIntent(instantAppResolveInfoList, origIntent, instantAppRequest.resolvedType, instantAppRequest.userId, origIntent.getPackage(), instantAppRequest.digest, token);
            } catch (ConnectionException e) {
                resolutionStatus2 = e;
                resolutionStatus = resolutionStatus2.failure != 1 ? 2 : resolutionStatus2.failure == i ? 3 : 1;
                resolutionStatus2 = resolutionStatus;
                logMetrics(899, startTime, token, resolutionStatus2);
                if (resolutionStatus2 != i) {
                }
                if (resolveInfo == null) {
                }
                return resolveInfo;
            }
            resolutionStatus2 = resolutionStatus;
            logMetrics(899, startTime, token, resolutionStatus2);
            if (resolutionStatus2 != i) {
            }
            if (resolveInfo == null) {
            }
            return resolveInfo;
        } catch (ConnectionException e2) {
            resolutionStatus2 = e2;
            i = 2;
            if (resolutionStatus2.failure != 1) {
            }
            resolutionStatus2 = resolutionStatus;
            logMetrics(899, startTime, token, resolutionStatus2);
            if (resolutionStatus2 != i) {
            }
            if (resolveInfo == null) {
            }
            return resolveInfo;
        }
    }

    public static void doInstantAppResolutionPhaseTwo(Context context, InstantAppResolverConnection connection, InstantAppRequest requestObj, ActivityInfo instantAppInstaller, Handler callbackHandler) {
        String token;
        ConnectionException e;
        InstantAppRequest instantAppRequest = requestObj;
        long startTime = System.currentTimeMillis();
        String token2 = instantAppRequest.responseObj.token;
        if (DEBUG_INSTANT) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[");
            stringBuilder.append(token2);
            stringBuilder.append("] Phase2; resolving");
            Log.d(str, stringBuilder.toString());
        }
        Intent origIntent = instantAppRequest.origIntent;
        Intent sanitizedIntent = sanitizeIntent(origIntent);
        final Intent intent = origIntent;
        final InstantAppRequest instantAppRequest2 = instantAppRequest;
        final String str2 = token2;
        final Intent intent2 = sanitizedIntent;
        final ActivityInfo activityInfo = instantAppInstaller;
        final Context context2 = context;
        PhaseTwoCallback anonymousClass1 = new PhaseTwoCallback() {
            void onPhaseTwoResolved(List<InstantAppResolveInfo> instantAppResolveInfoList, long startTime) {
                Intent intent = null;
                if (instantAppResolveInfoList != null && instantAppResolveInfoList.size() > 0) {
                    AuxiliaryResolveInfo instantAppIntentInfo = InstantAppResolver.filterInstantAppIntent(instantAppResolveInfoList, intent, null, 0, intent.getPackage(), instantAppRequest2.digest, str2);
                    if (instantAppIntentInfo != null) {
                        intent = instantAppIntentInfo.failureIntent;
                    }
                }
                intent = InstantAppResolver.buildEphemeralInstallerIntent(instantAppRequest2.origIntent, intent2, intent, instantAppRequest2.callingPackage, instantAppRequest2.verificationBundle, instantAppRequest2.resolvedType, instantAppRequest2.userId, instantAppRequest2.responseObj.installFailureActivity, str2, false, instantAppRequest2.responseObj.filters);
                intent.setComponent(new ComponentName(activityInfo.packageName, activityInfo.name));
                InstantAppResolver.logMetrics(900, startTime, str2, instantAppRequest2.responseObj.filters != null ? 0 : 1);
                context2.startActivity(intent);
            }
        };
        long startTime2;
        try {
            token = token2;
            startTime2 = startTime;
            try {
                connection.getInstantAppIntentFilterList(sanitizedIntent, instantAppRequest.digest.getDigestPrefixSecure(), token2, anonymousClass1, callbackHandler, startTime2);
            } catch (ConnectionException e2) {
                e = e2;
            }
        } catch (ConnectionException e3) {
            e = e3;
            Intent intent3 = origIntent;
            token = token2;
            startTime2 = startTime;
            int resolutionStatus = 1;
            if (e.failure == 1) {
                resolutionStatus = 2;
            }
            logMetrics(900, startTime2, token, resolutionStatus);
            if (!DEBUG_INSTANT) {
                return;
            }
            String str3;
            StringBuilder stringBuilder2;
            if (resolutionStatus == 2) {
                str3 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[");
                stringBuilder2.append(token);
                stringBuilder2.append("] Phase2; bind timed out");
                Log.d(str3, stringBuilder2.toString());
                return;
            }
            str3 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[");
            stringBuilder2.append(token);
            stringBuilder2.append("] Phase2; service connection error");
            Log.d(str3, stringBuilder2.toString());
        }
    }

    public static Intent buildEphemeralInstallerIntent(Intent origIntent, Intent sanitizedIntent, Intent failureIntent, String callingPackage, Bundle verificationBundle, String resolvedType, int userId, ComponentName installFailureActivity, String token, boolean needsPhaseTwo, List<AuxiliaryFilter> filters) {
        Intent intent = origIntent;
        Bundle bundle = verificationBundle;
        ComponentName componentName = installFailureActivity;
        String str = token;
        List<AuxiliaryFilter> list = filters;
        int flags = origIntent.getFlags();
        Intent intent2 = new Intent();
        intent2.setFlags((1073741824 | flags) | DumpState.DUMP_VOLUMES);
        if (str != null) {
            intent2.putExtra("android.intent.extra.EPHEMERAL_TOKEN", str);
            intent2.putExtra("android.intent.extra.INSTANT_APP_TOKEN", str);
        }
        if (origIntent.getData() != null) {
            intent2.putExtra("android.intent.extra.EPHEMERAL_HOSTNAME", origIntent.getData().getHost());
            intent2.putExtra("android.intent.extra.INSTANT_APP_HOSTNAME", origIntent.getData().getHost());
        }
        intent2.putExtra("android.intent.extra.INSTANT_APP_ACTION", origIntent.getAction());
        intent2.putExtra("android.intent.extra.INTENT", sanitizedIntent);
        if (needsPhaseTwo) {
            intent2.setAction("android.intent.action.RESOLVE_INSTANT_APP_PACKAGE");
            String str2 = callingPackage;
        } else {
            boolean z;
            if (!(failureIntent == null && componentName == null)) {
                Intent onFailureIntent;
                if (componentName != null) {
                    try {
                        onFailureIntent = new Intent();
                        onFailureIntent.setComponent(componentName);
                        if (list != null && filters.size() == 1) {
                            onFailureIntent.putExtra("android.intent.extra.SPLIT_NAME", ((AuxiliaryFilter) list.get(0)).splitName);
                        }
                        onFailureIntent.putExtra("android.intent.extra.INTENT", intent);
                    } catch (RemoteException e) {
                    }
                } else {
                    onFailureIntent = failureIntent;
                }
                String str3 = callingPackage;
                IntentSender failureSender = new IntentSender(ActivityManager.getService().getIntentSender(2, str3, null, null, 1, new Intent[]{onFailureIntent}, new String[]{resolvedType}, 1409286144, null, userId));
                intent2.putExtra("android.intent.extra.EPHEMERAL_FAILURE", failureSender);
                intent2.putExtra("android.intent.extra.INSTANT_APP_FAILURE", failureSender);
            }
            Intent successIntent = new Intent(intent);
            successIntent.setLaunchToken(str);
            try {
                IActivityManager service = ActivityManager.getService();
                Intent[] intentArr = new Intent[1];
                z = false;
                try {
                    intentArr[0] = successIntent;
                    IntentSender successSender = new IntentSender(service.getIntentSender(2, callingPackage, null, null, 0, intentArr, new String[]{resolvedType}, 1409286144, null, userId));
                    intent2.putExtra("android.intent.extra.EPHEMERAL_SUCCESS", successSender);
                    intent2.putExtra("android.intent.extra.INSTANT_APP_SUCCESS", successSender);
                } catch (RemoteException e2) {
                }
            } catch (RemoteException e3) {
                z = false;
            }
            if (bundle != null) {
                intent2.putExtra("android.intent.extra.VERIFICATION_BUNDLE", bundle);
            }
            intent2.putExtra("android.intent.extra.CALLING_PACKAGE", callingPackage);
            if (list != null) {
                Bundle[] resolvableFilters = new Bundle[filters.size()];
                int i = 0;
                int max = filters.size();
                while (true) {
                    int max2 = max;
                    if (i >= max2) {
                        break;
                    }
                    Bundle resolvableFilter = new Bundle();
                    AuxiliaryFilter filter = (AuxiliaryFilter) list.get(i);
                    String str4 = "android.intent.extra.UNKNOWN_INSTANT_APP";
                    boolean z2 = (filter.resolveInfo == null || !filter.resolveInfo.shouldLetInstallerDecide()) ? z : true;
                    resolvableFilter.putBoolean(str4, z2);
                    resolvableFilter.putString("android.intent.extra.PACKAGE_NAME", filter.packageName);
                    resolvableFilter.putString("android.intent.extra.SPLIT_NAME", filter.splitName);
                    resolvableFilter.putLong("android.intent.extra.LONG_VERSION_CODE", filter.versionCode);
                    resolvableFilter.putBundle("android.intent.extra.INSTANT_APP_EXTRAS", filter.extras);
                    resolvableFilters[i] = resolvableFilter;
                    if (i == 0) {
                        intent2.putExtras(resolvableFilter);
                        intent2.putExtra("android.intent.extra.VERSION_CODE", (int) filter.versionCode);
                    }
                    i++;
                    max = max2;
                    intent = origIntent;
                    bundle = verificationBundle;
                    componentName = installFailureActivity;
                    str = token;
                }
                intent2.putExtra("android.intent.extra.INSTANT_APP_BUNDLES", resolvableFilters);
            }
            intent2.setAction("android.intent.action.INSTALL_INSTANT_APP_PACKAGE");
        }
        return intent2;
    }

    private static AuxiliaryResolveInfo filterInstantAppIntent(List<InstantAppResolveInfo> instantAppResolveInfoList, Intent origIntent, String resolvedType, int userId, String packageName, InstantAppDigest digest, String token) {
        String str = token;
        int[] shaPrefix = digest.getDigestPrefix();
        byte[][] digestBytes = digest.getDigestBytes();
        boolean z = origIntent.isWebIntent() || (shaPrefix.length > 0 && (origIntent.getFlags() & 2048) == 0);
        boolean requiresPrefixMatch = z;
        boolean requiresSecondPhase = false;
        ArrayList<AuxiliaryFilter> filters = null;
        for (InstantAppResolveInfo instantAppResolveInfo : instantAppResolveInfoList) {
            if (requiresPrefixMatch && instantAppResolveInfo.shouldLetInstallerDecide()) {
                Slog.d(TAG, "InstantAppResolveInfo with mShouldLetInstallerDecide=true when digest required; ignoring");
            } else {
                byte[] filterDigestBytes = instantAppResolveInfo.getDigestBytes();
                if (shaPrefix.length > 0 && (requiresPrefixMatch || filterDigestBytes.length > 0)) {
                    boolean matchFound = false;
                    for (int i = shaPrefix.length - 1; i >= 0; i--) {
                        if (Arrays.equals(digestBytes[i], filterDigestBytes)) {
                            matchFound = true;
                            break;
                        }
                    }
                    if (!matchFound) {
                    }
                }
                List<AuxiliaryFilter> matchFilters = computeResolveFilters(origIntent, resolvedType, userId, packageName, str, instantAppResolveInfo);
                if (matchFilters != null) {
                    if (matchFilters.isEmpty()) {
                        requiresSecondPhase = true;
                    }
                    if (filters == null) {
                        filters = new ArrayList(matchFilters);
                    } else {
                        filters.addAll(matchFilters);
                    }
                }
            }
        }
        if (filters != null && !filters.isEmpty()) {
            return new AuxiliaryResolveInfo(str, requiresSecondPhase, createFailureIntent(origIntent, str), filters);
        }
        Intent intent = origIntent;
        return null;
    }

    private static Intent createFailureIntent(Intent origIntent, String token) {
        Intent failureIntent = new Intent(origIntent);
        failureIntent.setFlags(failureIntent.getFlags() | 512);
        failureIntent.setFlags(failureIntent.getFlags() & -2049);
        failureIntent.setLaunchToken(token);
        return failureIntent;
    }

    private static List<AuxiliaryFilter> computeResolveFilters(Intent origIntent, String resolvedType, int userId, String packageName, String token, InstantAppResolveInfo instantAppInfo) {
        String str = packageName;
        String str2 = token;
        InstantAppResolveInfo instantAppResolveInfo = instantAppInfo;
        if (instantAppInfo.shouldLetInstallerDecide()) {
            return Collections.singletonList(new AuxiliaryFilter(instantAppResolveInfo, null, instantAppInfo.getExtras()));
        }
        if (str != null && !str.equals(instantAppInfo.getPackageName())) {
            return null;
        }
        List<InstantAppIntentFilter> instantAppFilters = instantAppInfo.getIntentFilters();
        int i;
        if (instantAppFilters == null || instantAppFilters.isEmpty()) {
            String str3 = resolvedType;
            i = userId;
            if (origIntent.isWebIntent()) {
                return null;
            }
            if (DEBUG_INSTANT) {
                Log.d(TAG, "No app filters; go to phase 2");
            }
            return Collections.emptyList();
        }
        InstantAppIntentResolver instantAppResolver = new InstantAppIntentResolver();
        for (int j = instantAppFilters.size() - 1; j >= 0; j--) {
            InstantAppIntentFilter instantAppFilter = (InstantAppIntentFilter) instantAppFilters.get(j);
            List<IntentFilter> splitFilters = instantAppFilter.getFilters();
            if (!(splitFilters == null || splitFilters.isEmpty())) {
                for (i = splitFilters.size() - 1; i >= 0; i--) {
                    IntentFilter filter = (IntentFilter) splitFilters.get(i);
                    Iterator<AuthorityEntry> authorities = filter.authoritiesIterator();
                    if ((authorities != null && authorities.hasNext()) || ((!filter.hasDataScheme("http") && !filter.hasDataScheme("https")) || !filter.hasAction("android.intent.action.VIEW") || !filter.hasCategory("android.intent.category.BROWSABLE"))) {
                        instantAppResolver.addFilter(new AuxiliaryFilter(filter, instantAppResolveInfo, instantAppFilter.getSplitName(), instantAppInfo.getExtras()));
                    }
                }
            }
        }
        List<AuxiliaryFilter> matchedResolveInfoList = instantAppResolver.queryIntent(origIntent, resolvedType, null, userId);
        if (matchedResolveInfoList.isEmpty()) {
            if (DEBUG_INSTANT) {
                String str4 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[");
                stringBuilder.append(str2);
                stringBuilder.append("] No matches found package: ");
                stringBuilder.append(instantAppInfo.getPackageName());
                stringBuilder.append(", versionCode: ");
                stringBuilder.append(instantAppInfo.getVersionCode());
                Log.d(str4, stringBuilder.toString());
            }
            return null;
        }
        if (DEBUG_INSTANT) {
            String str5 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[");
            stringBuilder2.append(str2);
            stringBuilder2.append("] Found match(es); ");
            stringBuilder2.append(matchedResolveInfoList);
            Log.d(str5, stringBuilder2.toString());
        }
        return matchedResolveInfoList;
    }

    private static void logMetrics(int action, long startTime, String token, int status) {
        getLogger().write(new LogMaker(action).setType(4).addTaggedData(NetdResponseCode.ApLinkedStaListChangeQCOM, new Long(System.currentTimeMillis() - startTime)).addTaggedData(903, token).addTaggedData(902, new Integer(status)));
    }
}
