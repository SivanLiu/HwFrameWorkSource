package com.android.internal.telephony;

import android.app.AppOpsManager;
import android.common.HwFrameworkFactory;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Process;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Secure;
import android.telephony.Rlog;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.content.PackageMonitor;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.huawei.pgmng.PGAction;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public final class SmsApplication {
    private static final String BLUETOOTH_PACKAGE_NAME = "com.android.bluetooth";
    private static final String CONTACTS_PACKAGE_NAME = "com.android.contacts";
    private static final boolean DEBUG_MULTIUSER = false;
    private static final String HWSYSTEMMANAGER_PACKAGE_NAME = "com.huawei.systemmanager";
    static final String LOG_TAG = "SmsApplication";
    private static final String MMS_PACKAGE_NAME = "com.android.mms";
    private static final String MMS_SERVICE_PACKAGE_NAME = "com.android.mms.service";
    private static final String PHONE_PACKAGE_NAME = "com.android.phone";
    private static final String PROP_CUST_DFLT_SMS_APP = "ro.config.default_sms_app";
    private static final String SCHEME_MMS = "mms";
    private static final String SCHEME_MMSTO = "mmsto";
    private static final String SCHEME_SMS = "sms";
    private static final String SCHEME_SMSTO = "smsto";
    private static final String TELEPHONY_PROVIDER_PACKAGE_NAME = "com.android.providers.telephony";
    private static SmsPackageMonitor sSmsPackageMonitor = null;

    public static class SmsApplicationData {
        private String mApplicationName;
        private String mMmsReceiverClass;
        public String mPackageName;
        private String mProviderChangedReceiverClass;
        private String mRespondViaMessageClass;
        private String mSendToClass;
        private String mSimFullReceiverClass;
        private String mSmsAppChangedReceiverClass;
        private String mSmsReceiverClass;
        private int mUid;

        public boolean isComplete() {
            return (this.mSmsReceiverClass == null || this.mMmsReceiverClass == null || this.mRespondViaMessageClass == null || this.mSendToClass == null) ? false : true;
        }

        public SmsApplicationData(String packageName, int uid) {
            this.mPackageName = packageName;
            this.mUid = uid;
        }

        public String getApplicationName(Context context) {
            if (this.mApplicationName == null) {
                PackageManager pm = context.getPackageManager();
                String str = null;
                try {
                    ApplicationInfo appInfo = pm.getApplicationInfoAsUser(this.mPackageName, 0, UserHandle.getUserId(this.mUid));
                    if (appInfo != null) {
                        CharSequence label = pm.getApplicationLabel(appInfo);
                        if (label != null) {
                            str = label.toString();
                        }
                        this.mApplicationName = str;
                    }
                } catch (NameNotFoundException e) {
                    return null;
                }
            }
            return this.mApplicationName;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" mPackageName: ");
            stringBuilder.append(this.mPackageName);
            stringBuilder.append(" mSmsReceiverClass: ");
            stringBuilder.append(this.mSmsReceiverClass);
            stringBuilder.append(" mMmsReceiverClass: ");
            stringBuilder.append(this.mMmsReceiverClass);
            stringBuilder.append(" mRespondViaMessageClass: ");
            stringBuilder.append(this.mRespondViaMessageClass);
            stringBuilder.append(" mSendToClass: ");
            stringBuilder.append(this.mSendToClass);
            stringBuilder.append(" mSmsAppChangedClass: ");
            stringBuilder.append(this.mSmsAppChangedReceiverClass);
            stringBuilder.append(" mProviderChangedReceiverClass: ");
            stringBuilder.append(this.mProviderChangedReceiverClass);
            stringBuilder.append(" mSimFullReceiverClass: ");
            stringBuilder.append(this.mSimFullReceiverClass);
            stringBuilder.append(" mUid: ");
            stringBuilder.append(this.mUid);
            return stringBuilder.toString();
        }
    }

    private static final class SmsPackageMonitor extends PackageMonitor {
        final Context mContext;

        public SmsPackageMonitor(Context context) {
            this.mContext = context;
        }

        public void onPackageDisappeared(String packageName, int reason) {
            onPackageChanged();
        }

        public void onPackageAppeared(String packageName, int reason) {
            onPackageChanged();
        }

        public void onPackageModified(String packageName) {
            onPackageChanged();
        }

        private void onPackageChanged() {
            PackageManager packageManager = this.mContext.getPackageManager();
            Context userContext = this.mContext;
            int userId = getSendingUserId();
            if (userId != 0) {
                try {
                    userContext = this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, new UserHandle(userId));
                } catch (NameNotFoundException e) {
                }
            }
            ComponentName componentName = SmsApplication.getDefaultSendToApplication(userContext, true);
            if (componentName != null) {
                SmsApplication.configurePreferredActivity(packageManager, componentName, userId);
            }
        }
    }

    private static int getIncomingUserId(Context context) {
        int contextUserId = context.getUserId();
        int callingUid = Binder.getCallingUid();
        if (UserHandle.getAppId(callingUid) < PGAction.PG_ID_DEFAULT_FRONT) {
            return contextUserId;
        }
        return UserHandle.getUserId(callingUid);
    }

    public static Collection<SmsApplicationData> getApplicationCollection(Context context) {
        int userId = getIncomingUserId(context);
        long token = Binder.clearCallingIdentity();
        try {
            Collection<SmsApplicationData> applicationCollectionInternal = getApplicationCollectionInternal(context, userId);
            return applicationCollectionInternal;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private static Collection<SmsApplicationData> getApplicationCollectionInternal(Context context, int userId) {
        SmsApplicationData smsApplicationData;
        ActivityInfo activityInfo;
        SmsApplicationData smsApplicationData2;
        ActivityInfo activityInfo2;
        SmsApplicationData smsApplicationData3;
        int i = userId;
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> smsReceivers = packageManager.queryBroadcastReceiversAsUser(new Intent("android.provider.Telephony.SMS_DELIVER"), 0, i);
        HashMap<String, SmsApplicationData> receivers = new HashMap();
        for (ResolveInfo resolveInfo : smsReceivers) {
            ActivityInfo activityInfo3 = resolveInfo.activityInfo;
            if (activityInfo3 != null) {
                if ("android.permission.BROADCAST_SMS".equals(activityInfo3.permission)) {
                    String packageName = activityInfo3.packageName;
                    if (!receivers.containsKey(packageName)) {
                        SmsApplicationData smsApplicationData4 = new SmsApplicationData(packageName, activityInfo3.applicationInfo.uid);
                        smsApplicationData4.mSmsReceiverClass = activityInfo3.name;
                        receivers.put(packageName, smsApplicationData4);
                    }
                }
            }
        }
        Intent intent = new Intent("android.provider.Telephony.WAP_PUSH_DELIVER");
        intent.setDataAndType(null, "application/vnd.wap.mms-message");
        for (ResolveInfo resolveInfo2 : packageManager.queryBroadcastReceiversAsUser(intent, 0, i)) {
            ActivityInfo activityInfo4 = resolveInfo2.activityInfo;
            if (activityInfo4 != null) {
                if ("android.permission.BROADCAST_WAP_PUSH".equals(activityInfo4.permission)) {
                    SmsApplicationData smsApplicationData5 = (SmsApplicationData) receivers.get(activityInfo4.packageName);
                    if (smsApplicationData5 != null) {
                        smsApplicationData5.mMmsReceiverClass = activityInfo4.name;
                    }
                }
            }
        }
        for (ResolveInfo resolveInfo3 : packageManager.queryIntentServicesAsUser(new Intent("android.intent.action.RESPOND_VIA_MESSAGE", Uri.fromParts(SCHEME_SMSTO, "", null)), 0, i)) {
            ServiceInfo serviceInfo = resolveInfo3.serviceInfo;
            if (serviceInfo != null) {
                if ("android.permission.SEND_RESPOND_VIA_MESSAGE".equals(serviceInfo.permission)) {
                    smsApplicationData = (SmsApplicationData) receivers.get(serviceInfo.packageName);
                    if (smsApplicationData != null) {
                        smsApplicationData.mRespondViaMessageClass = serviceInfo.name;
                    }
                }
            }
        }
        for (ResolveInfo resolveInfo32 : packageManager.queryIntentActivitiesAsUser(new Intent("android.intent.action.SENDTO", Uri.fromParts(SCHEME_SMSTO, "", null)), 0, i)) {
            ActivityInfo activityInfo5 = resolveInfo32.activityInfo;
            if (activityInfo5 != null) {
                smsApplicationData = (SmsApplicationData) receivers.get(activityInfo5.packageName);
                if (smsApplicationData != null) {
                    smsApplicationData.mSendToClass = activityInfo5.name;
                }
            }
        }
        for (ResolveInfo resolveInfo4 : packageManager.queryBroadcastReceiversAsUser(new Intent("android.provider.action.DEFAULT_SMS_PACKAGE_CHANGED"), 0, i)) {
            activityInfo = resolveInfo4.activityInfo;
            if (activityInfo != null) {
                smsApplicationData2 = (SmsApplicationData) receivers.get(activityInfo.packageName);
                if (smsApplicationData2 != null) {
                    smsApplicationData2.mSmsAppChangedReceiverClass = activityInfo.name;
                }
            }
        }
        for (ResolveInfo resolveInfo5 : packageManager.queryBroadcastReceiversAsUser(new Intent("android.provider.action.EXTERNAL_PROVIDER_CHANGE"), 0, i)) {
            activityInfo2 = resolveInfo5.activityInfo;
            if (activityInfo2 != null) {
                smsApplicationData3 = (SmsApplicationData) receivers.get(activityInfo2.packageName);
                if (smsApplicationData3 != null) {
                    smsApplicationData3.mProviderChangedReceiverClass = activityInfo2.name;
                }
            }
        }
        for (ResolveInfo resolveInfo52 : packageManager.queryBroadcastReceiversAsUser(new Intent("android.provider.Telephony.SIM_FULL"), null, i)) {
            activityInfo2 = resolveInfo52.activityInfo;
            if (activityInfo2 != null) {
                smsApplicationData3 = (SmsApplicationData) receivers.get(activityInfo2.packageName);
                if (smsApplicationData3 != null) {
                    smsApplicationData3.mSimFullReceiverClass = activityInfo2.name;
                }
                i = userId;
            }
        }
        for (ResolveInfo resolveInfo42 : smsReceivers) {
            activityInfo = resolveInfo42.activityInfo;
            if (activityInfo != null) {
                String packageName2 = activityInfo.packageName;
                smsApplicationData2 = (SmsApplicationData) receivers.get(packageName2);
                if (!(smsApplicationData2 == null || smsApplicationData2.isComplete())) {
                    receivers.remove(packageName2);
                }
            }
        }
        return receivers.values();
    }

    private static SmsApplicationData getApplicationForPackage(Collection<SmsApplicationData> applications, String packageName) {
        if (packageName == null) {
            return null;
        }
        for (SmsApplicationData application : applications) {
            if (application.mPackageName.contentEquals(packageName)) {
                return application;
            }
        }
        return null;
    }

    private static SmsApplicationData getApplication(Context context, boolean updateIfNeeded, int userId) {
        assignWriteSmsPermissionToAFW(context, updateIfNeeded, userId);
        if (!((TelephonyManager) context.getSystemService(PhoneConstants.PHONE_KEY)).isSmsCapable()) {
            return null;
        }
        String custDefaultSmsApp;
        StringBuilder stringBuilder;
        Collection<SmsApplicationData> applications = getApplicationCollectionInternal(context, userId);
        String defaultApplication = Secure.getStringForUser(context.getContentResolver(), "sms_default_application", userId);
        SmsApplicationData applicationData = null;
        if (defaultApplication != null) {
            applicationData = getApplicationForPackage(applications, defaultApplication);
        }
        if (updateIfNeeded && applicationData == null) {
            String defaultPackage = context.getResources().getString(17039923);
            custDefaultSmsApp = SystemProperties.get(PROP_CUST_DFLT_SMS_APP, defaultPackage);
            String str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("custDefaultSmsApp = ");
            stringBuilder.append(custDefaultSmsApp);
            stringBuilder.append(", defaultPackage = ");
            stringBuilder.append(defaultPackage);
            Rlog.d(str, stringBuilder.toString());
            applicationData = getApplicationForPackage(applications, custDefaultSmsApp);
            if (applicationData == null && applications.size() != 0) {
                applicationData = applications.toArray()[0];
            }
            if (applicationData != null) {
                setDefaultApplicationInternal(applicationData.mPackageName, context, userId);
            }
        }
        if (applicationData != null) {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService("appops");
            if ((updateIfNeeded || applicationData.mUid == Process.myUid()) && appOps.checkOp(15, applicationData.mUid, applicationData.mPackageName) != 0) {
                custDefaultSmsApp = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(applicationData.mPackageName);
                stringBuilder.append(" lost OP_WRITE_SMS: ");
                stringBuilder.append(updateIfNeeded ? " (fixing)" : " (no permission to fix)");
                Rlog.e(custDefaultSmsApp, stringBuilder.toString());
                if (updateIfNeeded) {
                    appOps.setMode(15, applicationData.mUid, applicationData.mPackageName, 0);
                } else {
                    applicationData = null;
                }
            }
            if (updateIfNeeded) {
                PackageManager packageManager = context.getPackageManager();
                configurePreferredActivity(packageManager, new ComponentName(applicationData.mPackageName, applicationData.mSendToClass), userId);
                assignWriteSmsPermissionToSystemApp(context, packageManager, appOps, PHONE_PACKAGE_NAME);
                assignWriteSmsPermissionToSystemApp(context, packageManager, appOps, BLUETOOTH_PACKAGE_NAME);
                assignWriteSmsPermissionToSystemApp(context, packageManager, appOps, MMS_SERVICE_PACKAGE_NAME);
                assignWriteSmsPermissionToSystemApp(context, packageManager, appOps, TELEPHONY_PROVIDER_PACKAGE_NAME);
                assignWriteSmsPermissionToSystemUid(appOps, 1001);
                assignWriteSmsPermissionToSystemApp(context, packageManager, appOps, HWSYSTEMMANAGER_PACKAGE_NAME);
                assignWriteSmsPermissionToSystemApp(context, packageManager, appOps, CONTACTS_PACKAGE_NAME);
                assignWriteSmsPermissionToSystemApp(context, packageManager, appOps, MMS_PACKAGE_NAME);
            }
        }
        return applicationData;
    }

    public static void setDefaultApplication(String packageName, Context context) {
        if (!HwFrameworkFactory.getHwBaseInnerSmsManager().shouldSetDefaultApplicationForPackage(packageName, context)) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("packageName ");
            stringBuilder.append(packageName);
            stringBuilder.append("is not allowed to set to default application, return.");
            Rlog.d(str, stringBuilder.toString());
        } else if (((TelephonyManager) context.getSystemService(PhoneConstants.PHONE_KEY)).isSmsCapable()) {
            int userId = getIncomingUserId(context);
            long token = Binder.clearCallingIdentity();
            try {
                setDefaultApplicationInternal(packageName, context, userId);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    private static void setDefaultApplicationInternal(String packageName, Context context, int userId) {
        String oldPackageName = Secure.getStringForUser(context.getContentResolver(), "sms_default_application", userId);
        if (packageName == null || oldPackageName == null || !packageName.equals(oldPackageName)) {
            PackageManager packageManager = context.getPackageManager();
            Collection<SmsApplicationData> applications = getApplicationCollection(context);
            SmsApplicationData oldAppData = oldPackageName != null ? getApplicationForPackage(applications, oldPackageName) : null;
            SmsApplicationData applicationData = getApplicationForPackage(applications, packageName);
            if (applicationData != null) {
                Intent oldAppIntent;
                AppOpsManager appOps = (AppOpsManager) context.getSystemService("appops");
                if (oldPackageName != null) {
                    try {
                        appOps.setMode(15, packageManager.getPackageInfoAsUser(oldPackageName, 0, userId).applicationInfo.uid, oldPackageName, 1);
                    } catch (NameNotFoundException e) {
                        String str = LOG_TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Old SMS package not found: ");
                        stringBuilder.append(oldPackageName);
                        Rlog.w(str, stringBuilder.toString());
                    }
                }
                Secure.putStringForUser(context.getContentResolver(), "sms_default_application", applicationData.mPackageName, userId);
                configurePreferredActivity(packageManager, new ComponentName(applicationData.mPackageName, applicationData.mSendToClass), userId);
                appOps.setMode(15, applicationData.mUid, applicationData.mPackageName, 0);
                assignWriteSmsPermissionToSystemApp(context, packageManager, appOps, PHONE_PACKAGE_NAME);
                assignWriteSmsPermissionToSystemApp(context, packageManager, appOps, BLUETOOTH_PACKAGE_NAME);
                assignWriteSmsPermissionToSystemApp(context, packageManager, appOps, MMS_SERVICE_PACKAGE_NAME);
                assignWriteSmsPermissionToSystemApp(context, packageManager, appOps, TELEPHONY_PROVIDER_PACKAGE_NAME);
                assignWriteSmsPermissionToSystemUid(appOps, 1001);
                if (!(oldAppData == null || oldAppData.mSmsAppChangedReceiverClass == null)) {
                    oldAppIntent = new Intent("android.provider.action.DEFAULT_SMS_PACKAGE_CHANGED");
                    oldAppIntent.setComponent(new ComponentName(oldAppData.mPackageName, oldAppData.mSmsAppChangedReceiverClass));
                    oldAppIntent.putExtra("android.provider.extra.IS_DEFAULT_SMS_APP", false);
                    context.sendBroadcast(oldAppIntent);
                }
                if (applicationData.mSmsAppChangedReceiverClass != null) {
                    oldAppIntent = new Intent("android.provider.action.DEFAULT_SMS_PACKAGE_CHANGED");
                    oldAppIntent.setComponent(new ComponentName(applicationData.mPackageName, applicationData.mSmsAppChangedReceiverClass));
                    oldAppIntent.putExtra("android.provider.extra.IS_DEFAULT_SMS_APP", true);
                    context.sendBroadcast(oldAppIntent);
                }
                MetricsLogger.action(context, (int) MetricsEvent.ACTION_DEFAULT_SMS_APP_CHANGED, applicationData.mPackageName);
                context.sendBroadcast(new Intent("com.android.telephony.DEFAULT_SMS_CHANGED"));
            }
        }
    }

    private static void assignWriteSmsPermissionToSystemApp(Context context, PackageManager packageManager, AppOpsManager appOps, String packageName) {
        if (packageManager.checkSignatures(context.getPackageName(), packageName) == 0 || HwFrameworkFactory.getHwBaseInnerSmsManager().allowToSetSmsWritePermission(packageName)) {
            try {
                PackageInfo info = packageManager.getPackageInfo(packageName, 0);
                if (appOps.checkOp(15, info.applicationInfo.uid, packageName) != 0) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(packageName);
                    stringBuilder.append(" does not have OP_WRITE_SMS:  (fixing)");
                    Rlog.w(str, stringBuilder.toString());
                    appOps.setMode(15, info.applicationInfo.uid, packageName, 0);
                }
            } catch (NameNotFoundException e) {
                String str2 = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Package not found: ");
                stringBuilder2.append(packageName);
                Rlog.e(str2, stringBuilder2.toString());
            }
            return;
        }
        String str3 = LOG_TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append(packageName);
        stringBuilder3.append(" does not have system signature");
        Rlog.e(str3, stringBuilder3.toString());
    }

    private static void assignWriteSmsPermissionToSystemUid(AppOpsManager appOps, int uid) {
        appOps.setUidMode(15, uid, 0);
    }

    public static void initSmsPackageMonitor(Context context) {
        sSmsPackageMonitor = new SmsPackageMonitor(context);
        sSmsPackageMonitor.register(context, context.getMainLooper(), UserHandle.ALL, false);
    }

    private static void configurePreferredActivity(PackageManager packageManager, ComponentName componentName, int userId) {
        replacePreferredActivity(packageManager, componentName, userId, SCHEME_SMS);
        replacePreferredActivity(packageManager, componentName, userId, SCHEME_SMSTO);
        replacePreferredActivity(packageManager, componentName, userId, "mms");
        replacePreferredActivity(packageManager, componentName, userId, SCHEME_MMSTO);
    }

    private static void replacePreferredActivity(PackageManager packageManager, ComponentName componentName, int userId, String scheme) {
        List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivitiesAsUser(new Intent("android.intent.action.SENDTO", Uri.fromParts(scheme, "", null)), 65600, userId);
        int n = resolveInfoList.size();
        ComponentName[] set = new ComponentName[n];
        for (int i = 0; i < n; i++) {
            ResolveInfo info = (ResolveInfo) resolveInfoList.get(i);
            set[i] = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SENDTO");
        intentFilter.addCategory("android.intent.category.DEFAULT");
        intentFilter.addDataScheme(scheme);
        packageManager.replacePreferredActivityAsUser(intentFilter, 2129920, set, componentName, userId);
    }

    public static SmsApplicationData getSmsApplicationData(String packageName, Context context) {
        return getApplicationForPackage(getApplicationCollection(context), packageName);
    }

    public static ComponentName getDefaultSmsApplication(Context context, boolean updateIfNeeded) {
        int userId = getIncomingUserId(context);
        long token = Binder.clearCallingIdentity();
        ComponentName component = null;
        try {
            SmsApplicationData smsApplicationData = getApplication(context, updateIfNeeded, userId);
            if (smsApplicationData != null) {
                component = new ComponentName(smsApplicationData.mPackageName, smsApplicationData.mSmsReceiverClass);
            }
            Binder.restoreCallingIdentity(token);
            return component;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static ComponentName getDefaultMmsApplication(Context context, boolean updateIfNeeded) {
        int userId = getIncomingUserId(context);
        long token = Binder.clearCallingIdentity();
        ComponentName component = null;
        try {
            SmsApplicationData smsApplicationData = getApplication(context, updateIfNeeded, userId);
            if (smsApplicationData != null) {
                component = new ComponentName(smsApplicationData.mPackageName, smsApplicationData.mMmsReceiverClass);
            }
            Binder.restoreCallingIdentity(token);
            return component;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static ComponentName getDefaultRespondViaMessageApplication(Context context, boolean updateIfNeeded) {
        int userId = getIncomingUserId(context);
        long token = Binder.clearCallingIdentity();
        ComponentName component = null;
        try {
            SmsApplicationData smsApplicationData = getApplication(context, updateIfNeeded, userId);
            if (smsApplicationData != null) {
                component = new ComponentName(smsApplicationData.mPackageName, smsApplicationData.mRespondViaMessageClass);
            }
            Binder.restoreCallingIdentity(token);
            return component;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static ComponentName getDefaultSendToApplication(Context context, boolean updateIfNeeded) {
        int userId = getIncomingUserId(context);
        long token = Binder.clearCallingIdentity();
        ComponentName component = null;
        try {
            SmsApplicationData smsApplicationData = getApplication(context, updateIfNeeded, userId);
            if (smsApplicationData != null) {
                component = new ComponentName(smsApplicationData.mPackageName, smsApplicationData.mSendToClass);
            }
            Binder.restoreCallingIdentity(token);
            return component;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static ComponentName getDefaultExternalTelephonyProviderChangedApplication(Context context, boolean updateIfNeeded) {
        int userId = getIncomingUserId(context);
        long token = Binder.clearCallingIdentity();
        ComponentName component = null;
        try {
            SmsApplicationData smsApplicationData = getApplication(context, updateIfNeeded, userId);
            if (!(smsApplicationData == null || smsApplicationData.mProviderChangedReceiverClass == null)) {
                component = new ComponentName(smsApplicationData.mPackageName, smsApplicationData.mProviderChangedReceiverClass);
            }
            Binder.restoreCallingIdentity(token);
            return component;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static ComponentName getDefaultSimFullApplication(Context context, boolean updateIfNeeded) {
        int userId = getIncomingUserId(context);
        long token = Binder.clearCallingIdentity();
        ComponentName component = null;
        try {
            SmsApplicationData smsApplicationData = getApplication(context, updateIfNeeded, userId);
            if (!(smsApplicationData == null || smsApplicationData.mSimFullReceiverClass == null)) {
                component = new ComponentName(smsApplicationData.mPackageName, smsApplicationData.mSimFullReceiverClass);
            }
            Binder.restoreCallingIdentity(token);
            return component;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static boolean shouldWriteMessageForPackage(String packageName, Context context) {
        if (SmsManager.getDefault().getAutoPersisting()) {
            return true;
        }
        return isDefaultSmsApplication(context, packageName) ^ 1;
    }

    public static boolean isDefaultSmsApplication(Context context, String packageName) {
        if (packageName == null) {
            return false;
        }
        String defaultSmsPackage = getDefaultSmsApplicationPackageName(context);
        if ((defaultSmsPackage == null || !defaultSmsPackage.equals(packageName)) && !BLUETOOTH_PACKAGE_NAME.equals(packageName)) {
            return false;
        }
        return true;
    }

    private static String getDefaultSmsApplicationPackageName(Context context) {
        ComponentName component = getDefaultSmsApplication(context, null);
        if (component != null) {
            return component.getPackageName();
        }
        return null;
    }

    private static void assignWriteSmsPermissionToAFW(Context context, boolean updateIfNeeded, int userId) {
        UserManager um = (UserManager) context.getSystemService("user");
        AppOpsManager appOps = (AppOpsManager) context.getSystemService("appops");
        PackageManager packageManager = context.getPackageManager();
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updatedNeeded = ");
        stringBuilder.append(updateIfNeeded);
        stringBuilder.append(" for userId = ");
        stringBuilder.append(userId);
        Log.i(str, stringBuilder.toString());
        if (um.isManagedProfile(userId) && updateIfNeeded) {
            assignWriteSmsPermissionToSystemApp(context, packageManager, appOps, MMS_PACKAGE_NAME);
        }
    }
}
