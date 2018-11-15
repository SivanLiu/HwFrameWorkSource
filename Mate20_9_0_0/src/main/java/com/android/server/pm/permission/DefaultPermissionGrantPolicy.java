package com.android.server.pm.permission;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PackageManagerInternal.PackagesProvider;
import android.content.pm.PackageManagerInternal.SyncAdapterPackagesProvider;
import android.content.pm.PackageParser.Package;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import com.android.server.LocalServices;
import com.android.server.backup.BackupManagerService;
import com.android.server.pm.PackageManagerService;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class DefaultPermissionGrantPolicy extends AbsDefaultPermissionGrantPolicy {
    private static final String ACTION_TRACK = "com.android.fitness.TRACK";
    private static final String ATTR_FIXED = "fixed";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_PACKAGE = "package";
    private static final String AUDIO_MIME_TYPE = "audio/mpeg";
    private static final Set<String> CALENDAR_PERMISSIONS = new ArraySet();
    private static final Set<String> CAMERA_PERMISSIONS = new ArraySet();
    private static final Set<String> COARSE_LOCATION_PERMISSIONS = new ArraySet();
    private static final Set<String> CONTACTS_PERMISSIONS = new ArraySet();
    private static final boolean DEBUG = false;
    private static final int DEFAULT_FLAGS = 794624;
    protected static final boolean HWFLOW;
    private static final Set<String> LOCATION_PERMISSIONS = new ArraySet();
    private static final Set<String> MICROPHONE_PERMISSIONS = new ArraySet();
    private static final int MSG_READ_DEFAULT_PERMISSION_EXCEPTIONS = 1;
    private static final Set<String> PHONE_PERMISSIONS = new ArraySet();
    private static final Set<String> SENSORS_PERMISSIONS = new ArraySet();
    private static final Set<String> SMS_PERMISSIONS = new ArraySet();
    private static final Set<String> STORAGE_PERMISSIONS = new ArraySet();
    private static final String TAG = "DefaultPermGrantPolicy";
    private static final String TAG_EXCEPTION = "exception";
    private static final String TAG_EXCEPTIONS = "exceptions";
    private static final String TAG_PERMISSION = "permission";
    private final Context mContext;
    private PackagesProvider mDialerAppPackagesProvider;
    private ArrayMap<String, List<DefaultPermissionGrant>> mGrantExceptions;
    private final Handler mHandler;
    private PackagesProvider mLocationPackagesProvider;
    private final Object mLock = new Object();
    private final DefaultPermissionGrantedCallback mPermissionGrantedCallback;
    private final PermissionManagerService mPermissionManager;
    private final PackageManagerInternal mServiceInternal;
    private PackagesProvider mSimCallManagerPackagesProvider;
    private PackagesProvider mSmsAppPackagesProvider;
    private SyncAdapterPackagesProvider mSyncAdapterPackagesProvider;
    private PackagesProvider mUseOpenWifiAppPackagesProvider;
    private PackagesProvider mVoiceInteractionPackagesProvider;

    private static final class DefaultPermissionGrant {
        final boolean fixed;
        final String name;

        public DefaultPermissionGrant(String name, boolean fixed) {
            this.name = name;
            this.fixed = fixed;
        }
    }

    public interface DefaultPermissionGrantedCallback {
        void onDefaultRuntimePermissionsGranted(int i);
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HWFLOW = z;
        PHONE_PERMISSIONS.add("android.permission.READ_PHONE_STATE");
        PHONE_PERMISSIONS.add("android.permission.CALL_PHONE");
        PHONE_PERMISSIONS.add("android.permission.READ_CALL_LOG");
        PHONE_PERMISSIONS.add("android.permission.WRITE_CALL_LOG");
        PHONE_PERMISSIONS.add("com.android.voicemail.permission.ADD_VOICEMAIL");
        PHONE_PERMISSIONS.add("android.permission.USE_SIP");
        PHONE_PERMISSIONS.add("android.permission.PROCESS_OUTGOING_CALLS");
        CONTACTS_PERMISSIONS.add("android.permission.READ_CONTACTS");
        CONTACTS_PERMISSIONS.add("android.permission.WRITE_CONTACTS");
        CONTACTS_PERMISSIONS.add("android.permission.GET_ACCOUNTS");
        LOCATION_PERMISSIONS.add("android.permission.ACCESS_FINE_LOCATION");
        LOCATION_PERMISSIONS.add("android.permission.ACCESS_COARSE_LOCATION");
        COARSE_LOCATION_PERMISSIONS.add("android.permission.ACCESS_COARSE_LOCATION");
        CALENDAR_PERMISSIONS.add("android.permission.READ_CALENDAR");
        CALENDAR_PERMISSIONS.add("android.permission.WRITE_CALENDAR");
        SMS_PERMISSIONS.add("android.permission.SEND_SMS");
        SMS_PERMISSIONS.add("android.permission.RECEIVE_SMS");
        SMS_PERMISSIONS.add("android.permission.READ_SMS");
        SMS_PERMISSIONS.add("android.permission.RECEIVE_WAP_PUSH");
        SMS_PERMISSIONS.add("android.permission.RECEIVE_MMS");
        SMS_PERMISSIONS.add("android.permission.READ_CELL_BROADCASTS");
        MICROPHONE_PERMISSIONS.add("android.permission.RECORD_AUDIO");
        CAMERA_PERMISSIONS.add("android.permission.CAMERA");
        SENSORS_PERMISSIONS.add("android.permission.BODY_SENSORS");
        STORAGE_PERMISSIONS.add("android.permission.READ_EXTERNAL_STORAGE");
        STORAGE_PERMISSIONS.add("android.permission.WRITE_EXTERNAL_STORAGE");
    }

    public DefaultPermissionGrantPolicy(Context context, Looper looper, DefaultPermissionGrantedCallback callback, PermissionManagerService permissionManager) {
        this.mContext = context;
        this.mHandler = new Handler(looper) {
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    synchronized (DefaultPermissionGrantPolicy.this.mLock) {
                        if (DefaultPermissionGrantPolicy.this.mGrantExceptions == null) {
                            DefaultPermissionGrantPolicy.this.mGrantExceptions = DefaultPermissionGrantPolicy.this.readDefaultPermissionExceptionsLocked();
                        }
                    }
                }
            }
        };
        this.mPermissionGrantedCallback = callback;
        this.mPermissionManager = permissionManager;
        this.mServiceInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
    }

    public void setLocationPackagesProvider(PackagesProvider provider) {
        synchronized (this.mLock) {
            this.mLocationPackagesProvider = provider;
        }
    }

    public void setVoiceInteractionPackagesProvider(PackagesProvider provider) {
        synchronized (this.mLock) {
            this.mVoiceInteractionPackagesProvider = provider;
        }
    }

    public void setSmsAppPackagesProvider(PackagesProvider provider) {
        synchronized (this.mLock) {
            this.mSmsAppPackagesProvider = provider;
        }
    }

    public void setDialerAppPackagesProvider(PackagesProvider provider) {
        synchronized (this.mLock) {
            this.mDialerAppPackagesProvider = provider;
        }
    }

    public void setSimCallManagerPackagesProvider(PackagesProvider provider) {
        synchronized (this.mLock) {
            this.mSimCallManagerPackagesProvider = provider;
        }
    }

    public void setUseOpenWifiAppPackagesProvider(PackagesProvider provider) {
        synchronized (this.mLock) {
            this.mUseOpenWifiAppPackagesProvider = provider;
        }
    }

    public void setSyncAdapterPackagesProvider(SyncAdapterPackagesProvider provider) {
        synchronized (this.mLock) {
            this.mSyncAdapterPackagesProvider = provider;
        }
    }

    public void grantDefaultPermissions(int userId) {
        grantPermissionsToSysComponentsAndPrivApps(userId);
        grantDefaultSystemHandlerPermissions(userId);
        grantDefaultPermissionExceptions(userId);
    }

    private void grantRuntimePermissionsForPackage(int userId, Package pkg) {
        Set<String> permissions = new ArraySet();
        Iterator it = pkg.requestedPermissions.iterator();
        while (it.hasNext()) {
            String permission = (String) it.next();
            BasePermission bp = this.mPermissionManager.getPermission(permission);
            if (bp != null) {
                if (bp.isRuntime()) {
                    permissions.add(permission);
                }
            }
        }
        if (!permissions.isEmpty()) {
            grantRuntimePermissions(pkg, permissions, true, userId);
        }
    }

    private void grantAllRuntimePermissions(int userId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Granting all runtime permissions for user ");
        stringBuilder.append(userId);
        Log.i(str, stringBuilder.toString());
        for (String packageName : this.mServiceInternal.getPackageList().getPackageNames()) {
            Package pkg = this.mServiceInternal.getPackage(packageName);
            if (pkg != null) {
                grantRuntimePermissionsForPackage(userId, pkg);
            }
        }
    }

    public void scheduleReadDefaultPermissionExceptions() {
        this.mHandler.sendEmptyMessage(1);
    }

    private void grantPermissionsToSysComponentsAndPrivApps(int userId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Granting permissions to platform components for user ");
        stringBuilder.append(userId);
        Log.i(str, stringBuilder.toString());
        for (String packageName : this.mServiceInternal.getPackageList().getPackageNames()) {
            Package pkg = this.mServiceInternal.getPackage(packageName);
            if (pkg != null) {
                if (isSysComponentOrPersistentPlatformSignedPrivApp(pkg) && doesPackageSupportRuntimePermissions(pkg)) {
                    if (!pkg.requestedPermissions.isEmpty()) {
                        grantRuntimePermissionsForPackage(userId, pkg);
                    }
                }
            }
        }
    }

    public void grantCustSmsApplication(Package pkg, int userId) {
        if (pkg != null) {
            Slog.w(TAG, "grantCustSmsApplication pkg is not null, return");
            return;
        }
        String defaultApplication = Secure.getStringForUser(this.mContext.getContentResolver(), "sms_default_application", userId);
        if (TextUtils.isEmpty(defaultApplication)) {
            String custDefaultSmsApp = SystemProperties.get("ro.config.default_sms_app", this.mContext.getResources().getString(17039922));
            if (TextUtils.isEmpty(custDefaultSmsApp)) {
                Slog.w(TAG, "grantCustSmsApplication custDefaultSmsApp is null, return");
                return;
            }
            Package smsPackage = getSystemPackage(custDefaultSmsApp);
            if (smsPackage != null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("grantCustSmsApplication smsPackage:");
                stringBuilder.append(smsPackage);
                Slog.w(str, stringBuilder.toString());
                grantDefaultPermissionsToDefaultSystemSmsApp(smsPackage, userId);
            } else {
                Slog.w(TAG, "grantCustSmsApplication smsPackage is null");
            }
            return;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("grantCustSmsApplication SMS_DEFAULT_APPLICATION setting has value");
        stringBuilder2.append(defaultApplication);
        stringBuilder2.append(" , return");
        Slog.w(str2, stringBuilder2.toString());
    }

    /* JADX WARNING: Removed duplicated region for block: B:301:0x0704  */
    /* JADX WARNING: Removed duplicated region for block: B:338:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:304:0x070e  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void grantDefaultSystemHandlerPermissions(int userId) {
        PackagesProvider locationPackagesProvider;
        PackagesProvider voiceInteractionPackagesProvider;
        PackagesProvider smsAppPackagesProvider;
        PackagesProvider dialerAppPackagesProvider;
        PackagesProvider simCallManagerPackagesProvider;
        PackagesProvider useOpenWifiAppPackagesProvider;
        SyncAdapterPackagesProvider syncAdapterPackagesProvider;
        String[] calendarSyncAdapterPackages;
        boolean verifierPackage;
        boolean downloadsPackage;
        int length;
        int i;
        int i2;
        int i3;
        List<Package> calendarSyncAdapters;
        int i4;
        Package contactsSyncAdapterCount;
        Package contactsPackage;
        int i5 = userId;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Granting permissions to default platform handlers for user ");
        stringBuilder.append(i5);
        Log.i(str, stringBuilder.toString());
        synchronized (this.mLock) {
            locationPackagesProvider = this.mLocationPackagesProvider;
            voiceInteractionPackagesProvider = this.mVoiceInteractionPackagesProvider;
            smsAppPackagesProvider = this.mSmsAppPackagesProvider;
            dialerAppPackagesProvider = this.mDialerAppPackagesProvider;
            simCallManagerPackagesProvider = this.mSimCallManagerPackagesProvider;
            useOpenWifiAppPackagesProvider = this.mUseOpenWifiAppPackagesProvider;
            syncAdapterPackagesProvider = this.mSyncAdapterPackagesProvider;
        }
        String[] voiceInteractPackageNames = voiceInteractionPackagesProvider != null ? voiceInteractionPackagesProvider.getPackages(i5) : null;
        String[] locationPackageNames = locationPackagesProvider != null ? locationPackagesProvider.getPackages(i5) : null;
        String[] smsAppPackageNames = smsAppPackagesProvider != null ? smsAppPackagesProvider.getPackages(i5) : null;
        String[] dialerAppPackageNames = dialerAppPackagesProvider != null ? dialerAppPackagesProvider.getPackages(i5) : null;
        String[] simCallManagerPackageNames = simCallManagerPackagesProvider != null ? simCallManagerPackagesProvider.getPackages(i5) : null;
        String[] useOpenWifiAppPackageNames = useOpenWifiAppPackagesProvider != null ? useOpenWifiAppPackagesProvider.getPackages(i5) : null;
        String[] contactsSyncAdapterPackages = syncAdapterPackagesProvider != null ? syncAdapterPackagesProvider.getPackages("com.android.contacts", i5) : null;
        if (syncAdapterPackagesProvider != null) {
            calendarSyncAdapterPackages = syncAdapterPackagesProvider.getPackages("com.android.calendar", i5);
        } else {
            calendarSyncAdapterPackages = null;
        }
        String installerPackageName = this.mServiceInternal.getKnownPackageName(2, i5);
        Package installerPackage = getSystemPackage(installerPackageName);
        if (installerPackage == null || !doesPackageSupportRuntimePermissions(installerPackage)) {
        } else {
            grantRuntimePermissions(installerPackage, STORAGE_PERMISSIONS, true, i5);
        }
        installerPackageName = this.mServiceInternal.getKnownPackageName(3, i5);
        Package verifierPackage2 = getSystemPackage(installerPackageName);
        if (verifierPackage2 == null || !doesPackageSupportRuntimePermissions(verifierPackage2)) {
        } else {
            grantRuntimePermissions(verifierPackage2, STORAGE_PERMISSIONS, true, i5);
            grantRuntimePermissions(verifierPackage2, PHONE_PERMISSIONS, false, i5);
            grantRuntimePermissions(verifierPackage2, SMS_PERMISSIONS, false, i5);
        }
        installerPackageName = this.mServiceInternal.getKnownPackageName(1, i5);
        installerPackage = getSystemPackage(installerPackageName);
        if (installerPackage == null || !doesPackageSupportRuntimePermissions(installerPackage)) {
        } else {
            grantRuntimePermissions(installerPackage, PHONE_PERMISSIONS, i5);
            grantRuntimePermissions(installerPackage, CONTACTS_PERMISSIONS, i5);
            grantRuntimePermissions(installerPackage, LOCATION_PERMISSIONS, i5);
            grantRuntimePermissions(installerPackage, CAMERA_PERMISSIONS, i5);
        }
        Intent cameraIntent = new Intent("android.media.action.IMAGE_CAPTURE");
        installerPackage = getDefaultSystemHandlerActivityPackage(cameraIntent, i5);
        if (installerPackage == null || !doesPackageSupportRuntimePermissions(installerPackage)) {
        } else {
            grantRuntimePermissions(installerPackage, CAMERA_PERMISSIONS, i5);
            grantRuntimePermissions(installerPackage, MICROPHONE_PERMISSIONS, i5);
            grantRuntimePermissions(installerPackage, STORAGE_PERMISSIONS, i5);
        }
        Package mediaStorePackage = getDefaultProviderAuthorityPackage("media", i5);
        if (mediaStorePackage != null) {
            verifierPackage = true;
            grantRuntimePermissions(mediaStorePackage, STORAGE_PERMISSIONS, true, i5);
            grantRuntimePermissions(mediaStorePackage, PHONE_PERMISSIONS, true, i5);
        } else {
            Package packageR = verifierPackage2;
            verifierPackage = true;
        }
        installerPackage = getDefaultProviderAuthorityPackage("downloads", i5);
        if (installerPackage != null) {
            grantRuntimePermissions(installerPackage, STORAGE_PERMISSIONS, verifierPackage, i5);
        }
        cameraIntent = new Intent("android.intent.action.VIEW_DOWNLOADS");
        verifierPackage2 = getDefaultSystemHandlerActivityPackage(cameraIntent, i5);
        if (verifierPackage2 == null || !doesPackageSupportRuntimePermissions(verifierPackage2)) {
            Package packageR2 = installerPackage;
            downloadsPackage = true;
        } else {
            downloadsPackage = true;
            grantRuntimePermissions(verifierPackage2, STORAGE_PERMISSIONS, true, i5);
        }
        mediaStorePackage = getDefaultProviderAuthorityPackage("com.android.externalstorage.documents", i5);
        if (mediaStorePackage != null) {
            grantRuntimePermissions(mediaStorePackage, STORAGE_PERMISSIONS, downloadsPackage, i5);
        }
        verifierPackage2 = getSystemPackage(PackageManagerService.DEFAULT_CONTAINER_PACKAGE);
        if (verifierPackage2 != null) {
            grantRuntimePermissions(verifierPackage2, STORAGE_PERMISSIONS, downloadsPackage, i5);
        }
        cameraIntent = new Intent("android.credentials.INSTALL");
        installerPackage = getDefaultSystemHandlerActivityPackage(cameraIntent, i5);
        if (installerPackage == null || !doesPackageSupportRuntimePermissions(installerPackage)) {
            Package packageR3 = verifierPackage2;
        } else {
            grantRuntimePermissions(installerPackage, STORAGE_PERMISSIONS, true, i5);
        }
        Package packageR4;
        if (dialerAppPackageNames == null) {
            verifierPackage2 = getDefaultSystemHandlerActivityPackage(new Intent("android.intent.action.DIAL"), i5);
            if (verifierPackage2 != null) {
                grantDefaultPermissionsToDefaultSystemDialerApp(verifierPackage2, i5);
            }
            packageR4 = installerPackage;
        } else {
            length = dialerAppPackageNames.length;
            i = 0;
            while (i < length) {
                int i6 = length;
                packageR4 = installerPackage;
                installerPackage = getSystemPackage(dialerAppPackageNames[i]);
                if (installerPackage != null) {
                    grantDefaultPermissionsToDefaultSystemDialerApp(installerPackage, i5);
                }
                i++;
                length = i6;
                installerPackage = packageR4;
            }
        }
        if (simCallManagerPackageNames != null) {
            length = simCallManagerPackageNames.length;
            i2 = 0;
            while (i2 < length) {
                int i7 = length;
                mediaStorePackage = getSystemPackage(simCallManagerPackageNames[i2]);
                if (mediaStorePackage != null) {
                    grantDefaultPermissionsToDefaultSimCallManager(mediaStorePackage, i5);
                }
                i2++;
                length = i7;
            }
        }
        if (useOpenWifiAppPackageNames != null) {
            length = useOpenWifiAppPackageNames.length;
            i2 = 0;
            while (i2 < length) {
                int i8 = length;
                mediaStorePackage = getSystemPackage(useOpenWifiAppPackageNames[i2]);
                if (mediaStorePackage != null) {
                    grantDefaultPermissionsToDefaultSystemUseOpenWifiApp(mediaStorePackage, i5);
                }
                i2++;
                length = i8;
            }
        }
        if (smsAppPackageNames == null) {
            cameraIntent = new Intent("android.intent.action.MAIN");
            cameraIntent.addCategory("android.intent.category.APP_MESSAGING");
            installerPackage = getDefaultSystemHandlerActivityPackage(cameraIntent, i5);
            if (installerPackage != null) {
                grantDefaultPermissionsToDefaultSystemSmsApp(installerPackage, i5);
            }
            grantCustSmsApplication(installerPackage, i5);
        } else {
            length = smsAppPackageNames.length;
            i2 = 0;
            while (i2 < length) {
                int i9 = length;
                mediaStorePackage = getSystemPackage(smsAppPackageNames[i2]);
                if (mediaStorePackage != null) {
                    grantDefaultPermissionsToDefaultSystemSmsApp(mediaStorePackage, i5);
                }
                i2++;
                length = i9;
            }
        }
        cameraIntent = new Intent("android.provider.Telephony.SMS_CB_RECEIVED");
        installerPackage = getDefaultSystemHandlerActivityPackage(cameraIntent, i5);
        if (installerPackage != null && doesPackageSupportRuntimePermissions(installerPackage)) {
            grantRuntimePermissions(installerPackage, SMS_PERMISSIONS, i5);
        }
        cameraIntent = new Intent("android.provider.Telephony.SMS_CARRIER_PROVISION");
        verifierPackage2 = getDefaultSystemHandlerServicePackage(cameraIntent, i5);
        if (verifierPackage2 == null || !doesPackageSupportRuntimePermissions(verifierPackage2)) {
            Package packageR5 = installerPackage;
        } else {
            grantRuntimePermissions(verifierPackage2, SMS_PERMISSIONS, null, i5);
        }
        cameraIntent = new Intent("android.intent.action.MAIN");
        cameraIntent.addCategory("android.intent.category.APP_CALENDAR");
        installerPackage = getDefaultSystemHandlerActivityPackage(cameraIntent, i5);
        if (installerPackage == null || !doesPackageSupportRuntimePermissions(installerPackage)) {
        } else {
            grantRuntimePermissions(installerPackage, CALENDAR_PERMISSIONS, i5);
            grantRuntimePermissions(installerPackage, CONTACTS_PERMISSIONS, i5);
        }
        mediaStorePackage = getDefaultProviderAuthorityPackage("com.android.calendar", i5);
        if (mediaStorePackage != null) {
            grantRuntimePermissions(mediaStorePackage, CONTACTS_PERMISSIONS, i5);
            grantRuntimePermissions(mediaStorePackage, CALENDAR_PERMISSIONS, true, i5);
            grantRuntimePermissions(mediaStorePackage, STORAGE_PERMISSIONS, i5);
        } else {
            Package packageR6 = verifierPackage2;
        }
        List<Package> calendarSyncAdapters2 = getHeadlessSyncAdapterPackages(calendarSyncAdapterPackages, i5);
        i = calendarSyncAdapters2.size();
        int i10 = 0;
        while (true) {
            String[] calendarSyncAdapterPackages2 = calendarSyncAdapterPackages;
            i3 = i10;
            if (i3 >= i) {
                break;
            }
            Package calendarProviderPackage = mediaStorePackage;
            mediaStorePackage = (Package) calendarSyncAdapters2.get(i3);
            if (doesPackageSupportRuntimePermissions(mediaStorePackage)) {
                calendarSyncAdapters = calendarSyncAdapters2;
                grantRuntimePermissions(mediaStorePackage, CALENDAR_PERMISSIONS, i5);
            } else {
                calendarSyncAdapters = calendarSyncAdapters2;
            }
            i10 = i3 + 1;
            calendarSyncAdapterPackages = calendarSyncAdapterPackages2;
            mediaStorePackage = calendarProviderPackage;
            calendarSyncAdapters2 = calendarSyncAdapters;
        }
        calendarSyncAdapters = calendarSyncAdapters2;
        Intent contactsIntent = new Intent("android.intent.action.MAIN");
        contactsIntent.addCategory("android.intent.category.APP_CONTACTS");
        mediaStorePackage = getDefaultSystemHandlerActivityPackage(contactsIntent, i5);
        if (mediaStorePackage != null && doesPackageSupportRuntimePermissions(mediaStorePackage)) {
            grantRuntimePermissions(mediaStorePackage, CONTACTS_PERMISSIONS, i5);
            grantRuntimePermissions(mediaStorePackage, PHONE_PERMISSIONS, i5);
        }
        calendarSyncAdapters2 = getHeadlessSyncAdapterPackages(contactsSyncAdapterPackages, i5);
        i3 = calendarSyncAdapters2.size();
        i10 = 0;
        while (true) {
            String[] contactsSyncAdapterPackages2 = contactsSyncAdapterPackages;
            i4 = i10;
            if (i4 >= i3) {
                break;
            }
            int contactsSyncAdapterCount2 = i3;
            contactsSyncAdapterCount = (Package) calendarSyncAdapters2.get(i4);
            if (doesPackageSupportRuntimePermissions(contactsSyncAdapterCount)) {
                contactsPackage = mediaStorePackage;
                grantRuntimePermissions(contactsSyncAdapterCount, CONTACTS_PERMISSIONS, i5);
            } else {
                contactsPackage = mediaStorePackage;
            }
            i10 = i4 + 1;
            contactsSyncAdapterPackages = contactsSyncAdapterPackages2;
            i3 = contactsSyncAdapterCount2;
            mediaStorePackage = contactsPackage;
        }
        contactsPackage = mediaStorePackage;
        contactsSyncAdapterCount = getDefaultProviderAuthorityPackage("com.android.contacts", i5);
        if (contactsSyncAdapterCount != null) {
            grantRuntimePermissions(contactsSyncAdapterCount, CONTACTS_PERMISSIONS, true, i5);
            grantRuntimePermissions(contactsSyncAdapterCount, PHONE_PERMISSIONS, true, i5);
            grantRuntimePermissions(contactsSyncAdapterCount, STORAGE_PERMISSIONS, i5);
        }
        Intent deviceProvisionIntent = new Intent("android.app.action.PROVISION_MANAGED_DEVICE");
        mediaStorePackage = getDefaultSystemHandlerActivityPackage(deviceProvisionIntent, i5);
        if (mediaStorePackage == null || !doesPackageSupportRuntimePermissions(mediaStorePackage)) {
        } else {
            grantRuntimePermissions(mediaStorePackage, CONTACTS_PERMISSIONS, i5);
        }
        contactsIntent = new Intent("android.intent.action.MAIN");
        contactsIntent.addCategory("android.intent.category.APP_MAPS");
        Package mapsPackage = getDefaultSystemHandlerActivityPackage(contactsIntent, i5);
        if (mapsPackage == null || !doesPackageSupportRuntimePermissions(mapsPackage)) {
        } else {
            grantRuntimePermissions(mapsPackage, LOCATION_PERMISSIONS, i5);
        }
        contactsIntent = new Intent("android.intent.action.MAIN");
        contactsIntent.addCategory("android.intent.category.APP_GALLERY");
        mapsPackage = getDefaultSystemHandlerActivityPackage(contactsIntent, i5);
        if (mapsPackage == null || !doesPackageSupportRuntimePermissions(mapsPackage)) {
        } else {
            grantRuntimePermissions(mapsPackage, STORAGE_PERMISSIONS, i5);
        }
        contactsIntent = new Intent("android.intent.action.MAIN");
        contactsIntent.addCategory("android.intent.category.APP_EMAIL");
        mapsPackage = getDefaultSystemHandlerActivityPackage(contactsIntent, i5);
        if (mapsPackage == null || !doesPackageSupportRuntimePermissions(mapsPackage)) {
        } else {
            grantRuntimePermissions(mapsPackage, CONTACTS_PERMISSIONS, i5);
            grantRuntimePermissions(mapsPackage, CALENDAR_PERMISSIONS, i5);
        }
        Package browserPackage = null;
        str = this.mServiceInternal.getKnownPackageName(4, i5);
        if (str != null) {
            browserPackage = getPackage(str);
        }
        if (browserPackage == null) {
            contactsIntent = new Intent("android.intent.action.MAIN");
            contactsIntent.addCategory("android.intent.category.APP_BROWSER");
            contactsSyncAdapterCount = getDefaultSystemHandlerActivityPackage(contactsIntent, i5);
        } else {
            contactsSyncAdapterCount = browserPackage;
        }
        if (contactsSyncAdapterCount != null && doesPackageSupportRuntimePermissions(contactsSyncAdapterCount)) {
            grantRuntimePermissions(contactsSyncAdapterCount, LOCATION_PERMISSIONS, i5);
        }
        Package deviceProvisionPackage;
        if (voiceInteractPackageNames != null) {
            i4 = voiceInteractPackageNames.length;
            i3 = 0;
            while (i3 < i4) {
                int i11 = i4;
                String voiceInteractPackageName = voiceInteractPackageNames[i3];
                deviceProvisionPackage = mediaStorePackage;
                mediaStorePackage = getSystemPackage(voiceInteractPackageName);
                if (mediaStorePackage != null && doesPackageSupportRuntimePermissions(mediaStorePackage)) {
                    grantRuntimePermissions(mediaStorePackage, CONTACTS_PERMISSIONS, i5);
                    grantRuntimePermissions(mediaStorePackage, CALENDAR_PERMISSIONS, i5);
                    grantRuntimePermissions(mediaStorePackage, MICROPHONE_PERMISSIONS, i5);
                    grantRuntimePermissions(mediaStorePackage, PHONE_PERMISSIONS, i5);
                    grantRuntimePermissions(mediaStorePackage, SMS_PERMISSIONS, i5);
                    grantRuntimePermissions(mediaStorePackage, LOCATION_PERMISSIONS, i5);
                }
                i3++;
                i4 = i11;
                mediaStorePackage = deviceProvisionPackage;
            }
        } else {
            deviceProvisionPackage = mediaStorePackage;
        }
        if (ActivityManager.isLowRamDeviceStatic()) {
            contactsIntent = new Intent("android.search.action.GLOBAL_SEARCH");
            mapsPackage = getDefaultSystemHandlerActivityPackage(contactsIntent, i5);
            if (mapsPackage != null && doesPackageSupportRuntimePermissions(mapsPackage)) {
                grantRuntimePermissions(mapsPackage, MICROPHONE_PERMISSIONS, false, i5);
                grantRuntimePermissions(mapsPackage, LOCATION_PERMISSIONS, false, i5);
            }
        }
        contactsIntent = new Intent("android.speech.RecognitionService");
        contactsIntent.addCategory("android.intent.category.DEFAULT");
        mapsPackage = getDefaultSystemHandlerServicePackage(contactsIntent, i5);
        if (mapsPackage != null && doesPackageSupportRuntimePermissions(mapsPackage)) {
            grantRuntimePermissions(mapsPackage, MICROPHONE_PERMISSIONS, i5);
        }
        Package voiceRecoPackage;
        List<Package> contactsSyncAdapters;
        if (locationPackageNames != null) {
            length = locationPackageNames.length;
            i3 = 0;
            while (i3 < length) {
                voiceRecoPackage = mapsPackage;
                mapsPackage = locationPackageNames[i3];
                int i12 = length;
                mediaStorePackage = getSystemPackage(mapsPackage);
                if (mediaStorePackage == null || !doesPackageSupportRuntimePermissions(mediaStorePackage)) {
                    contactsSyncAdapters = calendarSyncAdapters2;
                } else {
                    Package packageName = mapsPackage;
                    grantRuntimePermissions(mediaStorePackage, CONTACTS_PERMISSIONS, i5);
                    grantRuntimePermissions(mediaStorePackage, CALENDAR_PERMISSIONS, i5);
                    grantRuntimePermissions(mediaStorePackage, MICROPHONE_PERMISSIONS, i5);
                    grantRuntimePermissions(mediaStorePackage, PHONE_PERMISSIONS, i5);
                    grantRuntimePermissions(mediaStorePackage, SMS_PERMISSIONS, i5);
                    contactsSyncAdapters = calendarSyncAdapters2;
                    grantRuntimePermissions(mediaStorePackage, LOCATION_PERMISSIONS, true, i5);
                    grantRuntimePermissions(mediaStorePackage, CAMERA_PERMISSIONS, i5);
                    grantRuntimePermissions(mediaStorePackage, SENSORS_PERMISSIONS, i5);
                    grantRuntimePermissions(mediaStorePackage, STORAGE_PERMISSIONS, i5);
                }
                i3++;
                mapsPackage = voiceRecoPackage;
                length = i12;
                calendarSyncAdapters2 = contactsSyncAdapters;
            }
            contactsSyncAdapters = calendarSyncAdapters2;
        } else {
            voiceRecoPackage = mapsPackage;
            contactsSyncAdapters = calendarSyncAdapters2;
        }
        contactsIntent = new Intent("android.intent.action.VIEW");
        contactsIntent.addCategory("android.intent.category.DEFAULT");
        contactsIntent.setDataAndType(Uri.fromFile(new File("foo.mp3")), AUDIO_MIME_TYPE);
        mapsPackage = getDefaultSystemHandlerActivityPackage(contactsIntent, i5);
        if (mapsPackage != null && doesPackageSupportRuntimePermissions(mapsPackage)) {
            grantRuntimePermissions(mapsPackage, STORAGE_PERMISSIONS, i5);
        }
        cameraIntent = new Intent("android.intent.action.MAIN");
        cameraIntent.addCategory("android.intent.category.HOME");
        cameraIntent.addCategory("android.intent.category.LAUNCHER_APP");
        installerPackage = getDefaultSystemHandlerActivityPackage(cameraIntent, i5);
        if (installerPackage == null || !doesPackageSupportRuntimePermissions(installerPackage)) {
            Package packageR7 = mapsPackage;
            i4 = 0;
        } else {
            i4 = 0;
            grantRuntimePermissions(installerPackage, LOCATION_PERMISSIONS, false, i5);
        }
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.watch", i4)) {
            contactsIntent = new Intent("android.intent.action.MAIN");
            contactsIntent.addCategory("android.intent.category.HOME_MAIN");
            mapsPackage = getDefaultSystemHandlerActivityPackage(contactsIntent, i5);
            if (mapsPackage == null || !doesPackageSupportRuntimePermissions(mapsPackage)) {
            } else {
                grantRuntimePermissions(mapsPackage, CONTACTS_PERMISSIONS, null, i5);
                grantRuntimePermissions(mapsPackage, PHONE_PERMISSIONS, true, i5);
                grantRuntimePermissions(mapsPackage, MICROPHONE_PERMISSIONS, false, i5);
                grantRuntimePermissions(mapsPackage, LOCATION_PERMISSIONS, false, i5);
            }
            contactsIntent = new Intent(ACTION_TRACK);
            mediaStorePackage = getDefaultSystemHandlerActivityPackage(contactsIntent, i5);
            if (mediaStorePackage != null && doesPackageSupportRuntimePermissions(mediaStorePackage)) {
                grantRuntimePermissions(mediaStorePackage, SENSORS_PERMISSIONS, false, i5);
                grantRuntimePermissions(mediaStorePackage, LOCATION_PERMISSIONS, false, i5);
            }
        }
        contactsSyncAdapterCount = getSystemPackage("com.android.printspooler");
        if (contactsSyncAdapterCount != null && doesPackageSupportRuntimePermissions(contactsSyncAdapterCount)) {
            grantRuntimePermissions(contactsSyncAdapterCount, LOCATION_PERMISSIONS, true, i5);
        }
        deviceProvisionIntent = new Intent("android.telephony.action.EMERGENCY_ASSISTANCE");
        mediaStorePackage = getDefaultSystemHandlerActivityPackage(deviceProvisionIntent, i5);
        if (mediaStorePackage == null || !doesPackageSupportRuntimePermissions(mediaStorePackage)) {
            Intent intent = deviceProvisionIntent;
        } else {
            grantRuntimePermissions(mediaStorePackage, CONTACTS_PERMISSIONS, true, i5);
            grantRuntimePermissions(mediaStorePackage, PHONE_PERMISSIONS, true, i5);
        }
        contactsIntent = new Intent("android.intent.action.VIEW");
        contactsIntent.setType("vnd.android.cursor.item/ndef_msg");
        mapsPackage = getDefaultSystemHandlerActivityPackage(contactsIntent, i5);
        if (mapsPackage == null || !doesPackageSupportRuntimePermissions(mapsPackage)) {
            Package packageR8 = mediaStorePackage;
        } else {
            grantRuntimePermissions(mapsPackage, CONTACTS_PERMISSIONS, false, i5);
            grantRuntimePermissions(mapsPackage, PHONE_PERMISSIONS, false, i5);
        }
        contactsIntent = new Intent("android.os.storage.action.MANAGE_STORAGE");
        mediaStorePackage = getDefaultSystemHandlerActivityPackage(contactsIntent, i5);
        if (mediaStorePackage == null || !doesPackageSupportRuntimePermissions(mediaStorePackage)) {
            Package packageR9 = mapsPackage;
        } else {
            grantRuntimePermissions(mediaStorePackage, STORAGE_PERMISSIONS, true, i5);
        }
        contactsSyncAdapterCount = getSystemPackage("com.android.companiondevicemanager");
        if (contactsSyncAdapterCount == null || !doesPackageSupportRuntimePermissions(contactsSyncAdapterCount)) {
        } else {
            Package packageR10 = mediaStorePackage;
            grantRuntimePermissions(contactsSyncAdapterCount, LOCATION_PERMISSIONS, true, i5);
        }
        deviceProvisionIntent = new Intent("android.intent.action.RINGTONE_PICKER");
        mediaStorePackage = getDefaultSystemHandlerActivityPackage(deviceProvisionIntent, i5);
        if (mediaStorePackage == null || !doesPackageSupportRuntimePermissions(mediaStorePackage)) {
            Intent intent2 = deviceProvisionIntent;
        } else {
            grantRuntimePermissions(mediaStorePackage, STORAGE_PERMISSIONS, true, i5);
        }
        str = this.mContext.getPackageManager().getSystemTextClassifierPackageName();
        if (!TextUtils.isEmpty(str)) {
            mapsPackage = getSystemPackage(str);
            if (mapsPackage != null && doesPackageSupportRuntimePermissions(mapsPackage)) {
                grantRuntimePermissions(mapsPackage, PHONE_PERMISSIONS, false, i5);
                grantRuntimePermissions(mapsPackage, SMS_PERMISSIONS, false, i5);
                grantRuntimePermissions(mapsPackage, CALENDAR_PERMISSIONS, false, i5);
                grantRuntimePermissions(mapsPackage, LOCATION_PERMISSIONS, false, i5);
                grantRuntimePermissions(mapsPackage, CONTACTS_PERMISSIONS, false, i5);
                contactsSyncAdapterCount = getSystemPackage(BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE);
                if (contactsSyncAdapterCount != null) {
                    grantRuntimePermissions(contactsSyncAdapterCount, STORAGE_PERMISSIONS, true, i5);
                }
                if (this.mPermissionGrantedCallback == null) {
                    this.mPermissionGrantedCallback.onDefaultRuntimePermissionsGranted(i5);
                    return;
                }
                return;
            }
        }
        Package packageR11 = mediaStorePackage;
        contactsSyncAdapterCount = getSystemPackage(BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE);
        if (contactsSyncAdapterCount != null) {
        }
        if (this.mPermissionGrantedCallback == null) {
        }
    }

    private void grantDefaultPermissionsToDefaultSystemDialerApp(Package dialerPackage, int userId) {
        if (doesPackageSupportRuntimePermissions(dialerPackage)) {
            grantRuntimePermissions(dialerPackage, PHONE_PERMISSIONS, this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.watch", 0), userId);
            grantRuntimePermissions(dialerPackage, CONTACTS_PERMISSIONS, userId);
            grantRuntimePermissions(dialerPackage, SMS_PERMISSIONS, userId);
            grantRuntimePermissions(dialerPackage, MICROPHONE_PERMISSIONS, userId);
            grantRuntimePermissions(dialerPackage, CAMERA_PERMISSIONS, userId);
        }
    }

    private void grantDefaultPermissionsToDefaultSystemSmsApp(Package smsPackage, int userId) {
        if (doesPackageSupportRuntimePermissions(smsPackage)) {
            grantRuntimePermissions(smsPackage, PHONE_PERMISSIONS, userId);
            grantRuntimePermissions(smsPackage, CONTACTS_PERMISSIONS, userId);
            grantRuntimePermissions(smsPackage, SMS_PERMISSIONS, userId);
            grantRuntimePermissions(smsPackage, STORAGE_PERMISSIONS, userId);
            grantRuntimePermissions(smsPackage, MICROPHONE_PERMISSIONS, userId);
            grantRuntimePermissions(smsPackage, CAMERA_PERMISSIONS, userId);
        }
    }

    private void grantDefaultPermissionsToDefaultSystemUseOpenWifiApp(Package useOpenWifiPackage, int userId) {
        if (doesPackageSupportRuntimePermissions(useOpenWifiPackage)) {
            grantRuntimePermissions(useOpenWifiPackage, COARSE_LOCATION_PERMISSIONS, userId);
        }
    }

    public void grantDefaultPermissionsToDefaultSmsApp(String packageName, int userId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Granting permissions to default sms app for user:");
        stringBuilder.append(userId);
        Log.i(str, stringBuilder.toString());
        if (packageName != null) {
            Package smsPackage = getPackage(packageName);
            if (smsPackage != null && doesPackageSupportRuntimePermissions(smsPackage)) {
                Package packageR = smsPackage;
                int i = userId;
                grantRuntimePermissions(packageR, PHONE_PERMISSIONS, false, true, i);
                grantRuntimePermissions(packageR, CONTACTS_PERMISSIONS, false, true, i);
                grantRuntimePermissions(packageR, SMS_PERMISSIONS, false, true, i);
                grantRuntimePermissions(packageR, STORAGE_PERMISSIONS, false, true, i);
                grantRuntimePermissions(packageR, MICROPHONE_PERMISSIONS, false, true, i);
                grantRuntimePermissions(packageR, CAMERA_PERMISSIONS, false, true, i);
            }
        }
    }

    public void grantDefaultPermissionsToDefaultDialerApp(String packageName, int userId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Granting permissions to default dialer app for user:");
        stringBuilder.append(userId);
        Log.i(str, stringBuilder.toString());
        if (packageName != null) {
            Package dialerPackage = getPackage(packageName);
            if (dialerPackage != null && doesPackageSupportRuntimePermissions(dialerPackage)) {
                Package packageR = dialerPackage;
                int i = userId;
                grantRuntimePermissions(packageR, PHONE_PERMISSIONS, false, true, i);
                grantRuntimePermissions(packageR, CONTACTS_PERMISSIONS, false, true, i);
                grantRuntimePermissions(packageR, SMS_PERMISSIONS, false, true, i);
                grantRuntimePermissions(packageR, MICROPHONE_PERMISSIONS, false, true, i);
                grantRuntimePermissions(packageR, CAMERA_PERMISSIONS, false, true, i);
            }
        }
    }

    public void grantDefaultPermissionsToDefaultUseOpenWifiApp(String packageName, int userId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Granting permissions to default Use Open WiFi app for user:");
        stringBuilder.append(userId);
        Log.i(str, stringBuilder.toString());
        if (packageName != null) {
            Package useOpenWifiPackage = getPackage(packageName);
            if (useOpenWifiPackage != null && doesPackageSupportRuntimePermissions(useOpenWifiPackage)) {
                grantRuntimePermissions(useOpenWifiPackage, COARSE_LOCATION_PERMISSIONS, false, true, userId);
            }
        }
    }

    private void grantDefaultPermissionsToDefaultSimCallManager(Package simCallManagerPackage, int userId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Granting permissions to sim call manager for user:");
        stringBuilder.append(userId);
        Log.i(str, stringBuilder.toString());
        if (doesPackageSupportRuntimePermissions(simCallManagerPackage)) {
            grantRuntimePermissions(simCallManagerPackage, PHONE_PERMISSIONS, userId);
            grantRuntimePermissions(simCallManagerPackage, MICROPHONE_PERMISSIONS, userId);
        }
    }

    public void grantDefaultPermissionsToDefaultSimCallManager(String packageName, int userId) {
        if (packageName != null) {
            Package simCallManagerPackage = getPackage(packageName);
            if (simCallManagerPackage != null) {
                grantDefaultPermissionsToDefaultSimCallManager(simCallManagerPackage, userId);
            }
        }
    }

    public void grantDefaultPermissionsToEnabledCarrierApps(String[] packageNames, int userId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Granting permissions to enabled carrier apps for user:");
        stringBuilder.append(userId);
        Log.i(str, stringBuilder.toString());
        if (packageNames != null) {
            for (String packageName : packageNames) {
                Package carrierPackage = getSystemPackage(packageName);
                if (carrierPackage != null && doesPackageSupportRuntimePermissions(carrierPackage)) {
                    grantRuntimePermissions(carrierPackage, PHONE_PERMISSIONS, userId);
                    grantRuntimePermissions(carrierPackage, LOCATION_PERMISSIONS, userId);
                    grantRuntimePermissions(carrierPackage, SMS_PERMISSIONS, userId);
                }
            }
        }
    }

    public void grantDefaultPermissionsToEnabledImsServices(String[] packageNames, int userId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Granting permissions to enabled ImsServices for user:");
        stringBuilder.append(userId);
        Log.i(str, stringBuilder.toString());
        if (packageNames != null) {
            for (String packageName : packageNames) {
                Package imsServicePackage = getSystemPackage(packageName);
                if (imsServicePackage != null && doesPackageSupportRuntimePermissions(imsServicePackage)) {
                    grantRuntimePermissions(imsServicePackage, PHONE_PERMISSIONS, userId);
                    grantRuntimePermissions(imsServicePackage, MICROPHONE_PERMISSIONS, userId);
                    grantRuntimePermissions(imsServicePackage, LOCATION_PERMISSIONS, userId);
                    grantRuntimePermissions(imsServicePackage, CAMERA_PERMISSIONS, userId);
                    grantRuntimePermissions(imsServicePackage, CONTACTS_PERMISSIONS, userId);
                }
            }
        }
    }

    public void grantDefaultPermissionsToEnabledTelephonyDataServices(String[] packageNames, int userId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Granting permissions to enabled data services for user:");
        stringBuilder.append(userId);
        Log.i(str, stringBuilder.toString());
        if (packageNames != null) {
            for (String packageName : packageNames) {
                Package dataServicePackage = getSystemPackage(packageName);
                if (dataServicePackage != null && doesPackageSupportRuntimePermissions(dataServicePackage)) {
                    grantRuntimePermissions(dataServicePackage, PHONE_PERMISSIONS, true, userId);
                    grantRuntimePermissions(dataServicePackage, LOCATION_PERMISSIONS, true, userId);
                }
            }
        }
    }

    public void revokeDefaultPermissionsFromDisabledTelephonyDataServices(String[] packageNames, int userId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Revoking permissions from disabled data services for user:");
        stringBuilder.append(userId);
        Log.i(str, stringBuilder.toString());
        if (packageNames != null) {
            for (String packageName : packageNames) {
                Package dataServicePackage = getSystemPackage(packageName);
                if (dataServicePackage != null && doesPackageSupportRuntimePermissions(dataServicePackage)) {
                    revokeRuntimePermissions(dataServicePackage, PHONE_PERMISSIONS, true, userId);
                    revokeRuntimePermissions(dataServicePackage, LOCATION_PERMISSIONS, true, userId);
                }
            }
        }
    }

    public void grantDefaultPermissionsToActiveLuiApp(String packageName, int userId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Granting permissions to active LUI app for user:");
        stringBuilder.append(userId);
        Log.i(str, stringBuilder.toString());
        if (packageName != null) {
            Package luiAppPackage = getSystemPackage(packageName);
            if (luiAppPackage != null && doesPackageSupportRuntimePermissions(luiAppPackage)) {
                grantRuntimePermissions(luiAppPackage, CAMERA_PERMISSIONS, true, userId);
            }
        }
    }

    public void revokeDefaultPermissionsFromLuiApps(String[] packageNames, int userId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Revoke permissions from LUI apps for user:");
        stringBuilder.append(userId);
        Log.i(str, stringBuilder.toString());
        if (packageNames != null) {
            for (String packageName : packageNames) {
                Package luiAppPackage = getSystemPackage(packageName);
                if (luiAppPackage != null && doesPackageSupportRuntimePermissions(luiAppPackage)) {
                    revokeRuntimePermissions(luiAppPackage, CAMERA_PERMISSIONS, true, userId);
                }
            }
        }
    }

    public void grantDefaultPermissionsToDefaultBrowser(String packageName, int userId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Granting permissions to default browser for user:");
        stringBuilder.append(userId);
        Log.i(str, stringBuilder.toString());
        if (packageName != null) {
            Package browserPackage = getSystemPackage(packageName);
            if (browserPackage != null && doesPackageSupportRuntimePermissions(browserPackage)) {
                grantRuntimePermissions(browserPackage, LOCATION_PERMISSIONS, false, false, userId);
            }
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0036, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Package getDefaultSystemHandlerActivityPackage(Intent intent, int userId) {
        ResolveInfo handler = this.mServiceInternal.resolveIntent(intent, intent.resolveType(this.mContext.getContentResolver()), DEFAULT_FLAGS, userId, false, Binder.getCallingUid());
        if (handler == null || handler.activityInfo == null || this.mServiceInternal.isResolveActivityComponent(handler.activityInfo)) {
            return null;
        }
        return getSystemPackage(handler.activityInfo.packageName);
    }

    private Package getDefaultSystemHandlerServicePackage(Intent intent, int userId) {
        List<ResolveInfo> handlers = this.mServiceInternal.queryIntentServices(intent, DEFAULT_FLAGS, Binder.getCallingUid(), userId);
        if (handlers == null) {
            return null;
        }
        int handlerCount = handlers.size();
        for (int i = 0; i < handlerCount; i++) {
            Package handlerPackage = getSystemPackage(((ResolveInfo) handlers.get(i)).serviceInfo.packageName);
            if (handlerPackage != null) {
                return handlerPackage;
            }
        }
        return null;
    }

    private List<Package> getHeadlessSyncAdapterPackages(String[] syncAdapterPackageNames, int userId) {
        List<Package> syncAdapterPackages = new ArrayList();
        Intent homeIntent = new Intent("android.intent.action.MAIN");
        homeIntent.addCategory("android.intent.category.LAUNCHER");
        for (String syncAdapterPackageName : syncAdapterPackageNames) {
            homeIntent.setPackage(syncAdapterPackageName);
            if (this.mServiceInternal.resolveIntent(homeIntent, homeIntent.resolveType(this.mContext.getContentResolver()), DEFAULT_FLAGS, userId, false, Binder.getCallingUid()) == null) {
                Package syncAdapterPackage = getSystemPackage(syncAdapterPackageName);
                if (syncAdapterPackage != null) {
                    syncAdapterPackages.add(syncAdapterPackage);
                }
            }
        }
        return syncAdapterPackages;
    }

    private Package getDefaultProviderAuthorityPackage(String authority, int userId) {
        ProviderInfo provider = this.mServiceInternal.resolveContentProvider(authority, DEFAULT_FLAGS, userId);
        if (provider != null) {
            return getSystemPackage(provider.packageName);
        }
        return null;
    }

    private Package getPackage(String packageName) {
        return this.mServiceInternal.getPackage(packageName);
    }

    protected Package getSystemPackage(String packageName) {
        Package pkg = getPackage(packageName);
        Package packageR = null;
        if (pkg == null || !pkg.isSystem()) {
            return null;
        }
        if (!isSysComponentOrPersistentPlatformSignedPrivApp(pkg)) {
            packageR = pkg;
        }
        return packageR;
    }

    private void grantRuntimePermissions(Package pkg, Set<String> permissions, int userId) {
        grantRuntimePermissions(pkg, permissions, false, false, userId);
    }

    protected void grantRuntimePermissions(Package pkg, Set<String> permissions, boolean systemFixed, int userId) {
        grantRuntimePermissions(pkg, permissions, systemFixed, false, userId);
    }

    private void revokeRuntimePermissions(Package pkg, Set<String> permissions, boolean systemFixed, int userId) {
        if (!pkg.requestedPermissions.isEmpty()) {
            Set<String> revokablePermissions = new ArraySet(pkg.requestedPermissions);
            for (String permission : permissions) {
                if (revokablePermissions.contains(permission)) {
                    int flags = this.mServiceInternal.getPermissionFlagsTEMP(permission, pkg.packageName, userId);
                    if ((flags & 32) != 0) {
                        if ((flags & 4) == 0) {
                            if ((flags & 16) == 0 || systemFixed) {
                                this.mServiceInternal.revokeRuntimePermission(pkg.packageName, permission, userId, false);
                                this.mServiceInternal.updatePermissionFlagsTEMP(permission, pkg.packageName, 32, 0, userId);
                            }
                        }
                    }
                }
            }
        }
    }

    private void grantRuntimePermissions(Package pkg, Set<String> permissions, boolean systemFixed, boolean ignoreSystemPackage, int userId) {
        Package packageR = pkg;
        int i = userId;
        if (!packageR.requestedPermissions.isEmpty()) {
            List<String> requestedPermissions = packageR.requestedPermissions;
            Set<String> grantablePermissions = null;
            if (!ignoreSystemPackage && pkg.isUpdatedSystemApp()) {
                Package disabledPkg = this.mServiceInternal.getDisabledPackage(packageR.packageName);
                if (disabledPkg != null) {
                    if (!disabledPkg.requestedPermissions.isEmpty()) {
                        if (!requestedPermissions.equals(disabledPkg.requestedPermissions)) {
                            grantablePermissions = new ArraySet(requestedPermissions);
                            requestedPermissions = disabledPkg.requestedPermissions;
                        }
                    } else {
                        return;
                    }
                }
            }
            List<String> requestedPermissions2 = requestedPermissions;
            Set<String> grantablePermissions2 = grantablePermissions;
            int grantablePermissionCount = requestedPermissions2.size();
            int i2 = 0;
            while (true) {
                int i3 = i2;
                if (i3 < grantablePermissionCount) {
                    String permission = (String) requestedPermissions2.get(i3);
                    if ((grantablePermissions2 == null || grantablePermissions2.contains(permission)) && permissions.contains(permission)) {
                        String permission2;
                        int flags = this.mServiceInternal.getPermissionFlagsTEMP(permission, packageR.packageName, i);
                        if (flags != 0 && !ignoreSystemPackage) {
                            permission2 = permission;
                        } else if ((flags & 4) == 0) {
                            this.mServiceInternal.grantRuntimePermission(packageR.packageName, permission, i, false);
                            i2 = 32;
                            if (systemFixed) {
                                i2 = 32 | 16;
                            }
                            int newFlags = i2;
                            permission2 = permission;
                            this.mServiceInternal.updatePermissionFlagsTEMP(permission, packageR.packageName, newFlags, newFlags, i);
                        }
                        if (!((flags & 32) == 0 || (flags & 16) == 0 || systemFixed)) {
                            this.mServiceInternal.updatePermissionFlagsTEMP(permission2, packageR.packageName, 16, 0, i);
                        }
                    }
                    i2 = i3 + 1;
                } else {
                    return;
                }
            }
        }
    }

    private boolean isSysComponentOrPersistentPlatformSignedPrivApp(Package pkg) {
        boolean z = true;
        if (UserHandle.getAppId(pkg.applicationInfo.uid) < 10000) {
            return true;
        }
        if (!pkg.isPrivileged()) {
            return false;
        }
        Package disabledPkg = this.mServiceInternal.getDisabledPackage(pkg.packageName);
        if (disabledPkg == null || disabledPkg.applicationInfo == null) {
            if ((pkg.applicationInfo.flags & 8) == 0) {
                return false;
            }
        } else if ((disabledPkg.applicationInfo.flags & 8) == 0) {
            return false;
        }
        Package systemPackage = getPackage(this.mServiceInternal.getKnownPackageName(0, 0));
        if (!(pkg.mSigningDetails.hasAncestorOrSelf(systemPackage.mSigningDetails) || systemPackage.mSigningDetails.checkCapability(pkg.mSigningDetails, 4))) {
            z = false;
        }
        return z;
    }

    private void grantDefaultPermissionExceptions(int userId) {
        this.mHandler.removeMessages(1);
        synchronized (this.mLock) {
            if (this.mGrantExceptions == null) {
                this.mGrantExceptions = readDefaultPermissionExceptionsLocked();
            }
        }
        int exceptionCount = this.mGrantExceptions.size();
        Set<String> permissions = null;
        int i = 0;
        while (i < exceptionCount) {
            Package pkg = getSystemPackage((String) this.mGrantExceptions.keyAt(i));
            List<DefaultPermissionGrant> permissionGrants = (List) this.mGrantExceptions.valueAt(i);
            int permissionGrantCount = permissionGrants.size();
            Set<String> permissions2 = permissions;
            for (int j = 0; j < permissionGrantCount; j++) {
                DefaultPermissionGrant permissionGrant = (DefaultPermissionGrant) permissionGrants.get(j);
                if (permissions2 == null) {
                    permissions2 = new ArraySet();
                } else {
                    permissions2.clear();
                }
                permissions2.add(permissionGrant.name);
                grantRuntimePermissions(pkg, permissions2, permissionGrant.fixed, userId);
            }
            i++;
            permissions = permissions2;
        }
    }

    private File[] getDefaultPermissionFiles() {
        ArrayList<File> ret = new ArrayList();
        File dir = new File(Environment.getRootDirectory(), "etc/default-permissions");
        if (dir.isDirectory() && dir.canRead()) {
            Collections.addAll(ret, dir.listFiles());
        }
        dir = new File(Environment.getVendorDirectory(), "etc/default-permissions");
        if (dir.isDirectory() && dir.canRead()) {
            Collections.addAll(ret, dir.listFiles());
        }
        dir = new File(Environment.getOdmDirectory(), "etc/default-permissions");
        if (dir.isDirectory() && dir.canRead()) {
            Collections.addAll(ret, dir.listFiles());
        }
        dir = new File(Environment.getProductDirectory(), "etc/default-permissions");
        if (dir.isDirectory() && dir.canRead()) {
            Collections.addAll(ret, dir.listFiles());
        }
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.embedded", 0)) {
            dir = new File(Environment.getOemDirectory(), "etc/default-permissions");
            if (dir.isDirectory() && dir.canRead()) {
                Collections.addAll(ret, dir.listFiles());
            }
        }
        return ret.isEmpty() ? null : (File[]) ret.toArray(new File[0]);
    }

    /* JADX WARNING: Removed duplicated region for block: B:31:0x009a A:{Splitter: B:12:0x006d, ExcHandler: org.xmlpull.v1.XmlPullParserException (r5_6 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:31:0x009a, code:
            r5 = move-exception;
     */
    /* JADX WARNING: Missing block: B:32:0x009b, code:
            r6 = TAG;
            r7 = new java.lang.StringBuilder();
            r7.append("Error reading default permissions file ");
            r7.append(r4);
            android.util.Slog.w(r6, r7.toString(), r5);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private ArrayMap<String, List<DefaultPermissionGrant>> readDefaultPermissionExceptionsLocked() {
        File[] files = getDefaultPermissionFiles();
        int i = 0;
        if (files == null) {
            return new ArrayMap(0);
        }
        ArrayMap<String, List<DefaultPermissionGrant>> grantExceptions = new ArrayMap();
        int length = files.length;
        while (i < length) {
            File file = files[i];
            String str;
            StringBuilder stringBuilder;
            if (!file.getPath().endsWith(".xml")) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Non-xml file ");
                stringBuilder.append(file);
                stringBuilder.append(" in ");
                stringBuilder.append(file.getParent());
                stringBuilder.append(" directory, ignoring");
                Slog.i(str, stringBuilder.toString());
            } else if (file.canRead()) {
                InputStream str2;
                try {
                    str2 = new BufferedInputStream(new FileInputStream(file));
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(str2, null);
                    parse(parser, grantExceptions);
                    str2.close();
                } catch (Exception e) {
                } catch (Throwable th) {
                    r6.addSuppressed(th);
                }
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Default permissions file ");
                stringBuilder.append(file);
                stringBuilder.append(" cannot be read");
                Slog.w(str, stringBuilder.toString());
            }
            i++;
        }
        return grantExceptions;
    }

    private void parse(XmlPullParser parser, Map<String, List<DefaultPermissionGrant>> outGrantExceptions) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3) {
                if (type != 4) {
                    if (TAG_EXCEPTIONS.equals(parser.getName())) {
                        parseExceptions(parser, outGrantExceptions);
                    } else {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown tag ");
                        stringBuilder.append(parser.getName());
                        Log.e(str, stringBuilder.toString());
                    }
                }
            }
        }
    }

    private void parseExceptions(XmlPullParser parser, Map<String, List<DefaultPermissionGrant>> outGrantExceptions) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3) {
                if (type != 4) {
                    String packageName;
                    if (TAG_EXCEPTION.equals(parser.getName())) {
                        packageName = parser.getAttributeValue(null, "package");
                        List<DefaultPermissionGrant> packageExceptions = (List) outGrantExceptions.get(packageName);
                        if (packageExceptions == null) {
                            Package pkg = getSystemPackage(packageName);
                            String str;
                            StringBuilder stringBuilder;
                            if (pkg == null) {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Unknown package:");
                                stringBuilder.append(packageName);
                                Log.w(str, stringBuilder.toString());
                                XmlUtils.skipCurrentTag(parser);
                            } else if (doesPackageSupportRuntimePermissions(pkg)) {
                                packageExceptions = new ArrayList();
                                outGrantExceptions.put(packageName, packageExceptions);
                            } else {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Skipping non supporting runtime permissions package:");
                                stringBuilder.append(packageName);
                                Log.w(str, stringBuilder.toString());
                                XmlUtils.skipCurrentTag(parser);
                            }
                        }
                        parsePermission(parser, packageExceptions);
                    } else {
                        packageName = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unknown tag ");
                        stringBuilder2.append(parser.getName());
                        stringBuilder2.append("under <exceptions>");
                        Log.e(packageName, stringBuilder2.toString());
                    }
                }
            }
        }
    }

    private void parsePermission(XmlPullParser parser, List<DefaultPermissionGrant> outPackageExceptions) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3) {
                if (type != 4) {
                    String name;
                    if (TAG_PERMISSION.contains(parser.getName())) {
                        name = parser.getAttributeValue(null, "name");
                        if (name == null) {
                            Log.w(TAG, "Mandatory name attribute missing for permission tag");
                            XmlUtils.skipCurrentTag(parser);
                        } else {
                            outPackageExceptions.add(new DefaultPermissionGrant(name, XmlUtils.readBooleanAttribute(parser, ATTR_FIXED)));
                        }
                    } else {
                        name = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown tag ");
                        stringBuilder.append(parser.getName());
                        stringBuilder.append("under <exception>");
                        Log.e(name, stringBuilder.toString());
                    }
                }
            }
        }
    }

    private static boolean doesPackageSupportRuntimePermissions(Package pkg) {
        return pkg.applicationInfo.targetSdkVersion > 22;
    }
}
