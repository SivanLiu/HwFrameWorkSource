package com.android.server.notification;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.IInterface;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import com.android.server.notification.NotificationManagerService.DumpFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public abstract class ManagedServices {
    static final int APPROVAL_BY_COMPONENT = 1;
    static final int APPROVAL_BY_PACKAGE = 0;
    static final String ATT_APPROVED_LIST = "approved";
    static final String ATT_IS_PRIMARY = "primary";
    static final String ATT_USER_ID = "user";
    protected static final String ENABLED_SERVICES_SEPARATOR = ":";
    static final String TAG_MANAGED_SERVICES = "service_listing";
    protected final boolean DEBUG = Log.isLoggable(this.TAG, 3);
    protected final String TAG = getClass().getSimpleName();
    protected int mApprovalLevel;
    private ArrayMap<Integer, ArrayMap<Boolean, ArraySet<String>>> mApproved = new ArrayMap();
    private final Config mConfig;
    protected final Context mContext;
    private ArraySet<ComponentName> mEnabledServicesForCurrentProfiles = new ArraySet();
    private ArraySet<String> mEnabledServicesPackageNames = new ArraySet();
    private int[] mLastSeenProfileIds;
    protected final Object mMutex;
    private final IPackageManager mPm;
    private final ArrayList<ManagedServiceInfo> mServices = new ArrayList();
    private final ArrayList<String> mServicesBinding = new ArrayList();
    private ArraySet<ComponentName> mSnoozingForCurrentProfiles = new ArraySet();
    private final UserManager mUm;
    private boolean mUseXml;
    private final UserProfiles mUserProfiles;

    public static class Config {
        public String bindPermission;
        public String caption;
        public int clientLabel;
        public String secondarySettingName;
        public String secureSettingName;
        public String serviceInterface;
        public String settingsAction;
        public String xmlTag;
    }

    public class ManagedServiceInfo implements DeathRecipient {
        public ComponentName component;
        public ServiceConnection connection;
        public boolean isSystem;
        public IInterface service;
        public int targetSdkVersion;
        public int userid;

        public ManagedServiceInfo(IInterface service, ComponentName component, int userid, boolean isSystem, ServiceConnection connection, int targetSdkVersion) {
            this.service = service;
            this.component = component;
            this.userid = userid;
            this.isSystem = isSystem;
            this.connection = connection;
            this.targetSdkVersion = targetSdkVersion;
        }

        public boolean isGuest(ManagedServices host) {
            return ManagedServices.this != host;
        }

        public ManagedServices getOwner() {
            return ManagedServices.this;
        }

        public String toString() {
            String str = null;
            StringBuilder append = new StringBuilder("ManagedServiceInfo[").append("component=").append(this.component).append(",userid=").append(this.userid).append(",isSystem=").append(this.isSystem).append(",targetSdkVersion=").append(this.targetSdkVersion).append(",connection=");
            if (this.connection != null) {
                str = "<connection>";
            }
            return append.append(str).append(",service=").append(this.service).append(']').toString();
        }

        public boolean enabledAndUserMatches(int nid) {
            boolean z = false;
            if (!isEnabledForCurrentProfiles()) {
                return false;
            }
            if (this.userid == -1 || this.isSystem || nid == -1 || nid == this.userid) {
                return true;
            }
            if (supportsProfiles() && ManagedServices.this.mUserProfiles.isCurrentProfile(nid)) {
                z = isPermittedForProfile(nid);
            }
            return z;
        }

        public boolean supportsProfiles() {
            return this.targetSdkVersion >= 21;
        }

        public void binderDied() {
            if (ManagedServices.this.DEBUG) {
                Slog.d(ManagedServices.this.TAG, "binderDied");
            }
            ManagedServices.this.removeServiceImpl(this.service, this.userid);
        }

        public boolean isEnabledForCurrentProfiles() {
            if (this.isSystem) {
                return true;
            }
            if (this.connection == null) {
                return false;
            }
            boolean contains;
            synchronized (ManagedServices.this.mMutex) {
                contains = ManagedServices.this.mEnabledServicesForCurrentProfiles.contains(this.component);
            }
            return contains;
        }

        public boolean isPermittedForProfile(int userId) {
            if (!ManagedServices.this.mUserProfiles.isManagedProfile(userId)) {
                return true;
            }
            DevicePolicyManager dpm = (DevicePolicyManager) ManagedServices.this.mContext.getSystemService("device_policy");
            long identity = Binder.clearCallingIdentity();
            try {
                boolean isNotificationListenerServicePermitted = dpm.isNotificationListenerServicePermitted(this.component.getPackageName(), userId);
                return isNotificationListenerServicePermitted;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public static class UserProfiles {
        private final SparseArray<UserInfo> mCurrentProfiles = new SparseArray();

        public void updateCache(Context context) {
            UserManager userManager = (UserManager) context.getSystemService(ManagedServices.ATT_USER_ID);
            if (userManager != null) {
                List<UserInfo> profiles = userManager.getProfiles(ActivityManager.getCurrentUser());
                synchronized (this.mCurrentProfiles) {
                    this.mCurrentProfiles.clear();
                    for (UserInfo user : profiles) {
                        this.mCurrentProfiles.put(user.id, user);
                    }
                }
            }
        }

        public int[] getCurrentProfileIds() {
            int[] users;
            synchronized (this.mCurrentProfiles) {
                users = new int[this.mCurrentProfiles.size()];
                int N = this.mCurrentProfiles.size();
                for (int i = 0; i < N; i++) {
                    users[i] = this.mCurrentProfiles.keyAt(i);
                }
            }
            return users;
        }

        public boolean isCurrentProfile(int userId) {
            boolean z;
            synchronized (this.mCurrentProfiles) {
                z = this.mCurrentProfiles.get(userId) != null;
            }
            return z;
        }

        public boolean isManagedProfile(int userId) {
            boolean isManagedProfile;
            synchronized (this.mCurrentProfiles) {
                UserInfo user = (UserInfo) this.mCurrentProfiles.get(userId);
                isManagedProfile = user != null ? user.isManagedProfile() : false;
            }
            return isManagedProfile;
        }
    }

    protected abstract IInterface asInterface(IBinder iBinder);

    protected abstract boolean checkType(IInterface iInterface);

    protected abstract Config getConfig();

    protected abstract void onServiceAdded(ManagedServiceInfo managedServiceInfo);

    public ManagedServices(Context context, Object mutex, UserProfiles userProfiles, IPackageManager pm) {
        this.mContext = context;
        this.mMutex = mutex;
        this.mUserProfiles = userProfiles;
        this.mPm = pm;
        this.mConfig = getConfig();
        this.mApprovalLevel = 1;
        this.mUm = (UserManager) this.mContext.getSystemService(ATT_USER_ID);
    }

    private String getCaption() {
        return this.mConfig.caption;
    }

    protected List<ManagedServiceInfo> getServices() {
        List<ManagedServiceInfo> services;
        synchronized (this.mMutex) {
            services = new ArrayList(this.mServices);
        }
        return services;
    }

    protected void onServiceRemovedLocked(ManagedServiceInfo removed) {
    }

    private ManagedServiceInfo newServiceInfo(IInterface service, ComponentName component, int userId, boolean isSystem, ServiceConnection connection, int targetSdkVersion) {
        return new ManagedServiceInfo(service, component, userId, isSystem, connection, targetSdkVersion);
    }

    public void onBootPhaseAppsCanStart() {
    }

    public void dump(PrintWriter pw, DumpFilter filter) {
        pw.println("    Allowed " + getCaption() + "s:");
        int N = this.mApproved.size();
        for (int i = 0; i < N; i++) {
            int userId = ((Integer) this.mApproved.keyAt(i)).intValue();
            ArrayMap<Boolean, ArraySet<String>> approvedByType = (ArrayMap) this.mApproved.valueAt(i);
            if (approvedByType != null) {
                int M = approvedByType.size();
                for (int j = 0; j < M; j++) {
                    boolean isPrimary = ((Boolean) approvedByType.keyAt(j)).booleanValue();
                    ArraySet<String> approved = (ArraySet) approvedByType.valueAt(j);
                    if (approvedByType != null && approvedByType.size() > 0) {
                        pw.println("      " + String.join(ENABLED_SERVICES_SEPARATOR, approved) + " (user: " + userId + " isPrimary: " + isPrimary + ")");
                    }
                }
            }
        }
        pw.println("    All " + getCaption() + "s (" + this.mEnabledServicesForCurrentProfiles.size() + ") enabled for current profiles:");
        for (ComponentName cmpt : this.mEnabledServicesForCurrentProfiles) {
            if (filter == null || (filter.matches(cmpt) ^ 1) == 0) {
                pw.println("      " + cmpt);
            }
        }
        pw.println("    Live " + getCaption() + "s (" + this.mServices.size() + "):");
        for (ManagedServiceInfo info : this.mServices) {
            if (filter != null) {
                if ((filter.matches(info.component) ^ 1) != 0) {
                }
            }
            pw.println("      " + info.component + " (user " + info.userid + "): " + info.service + (info.isSystem ? " SYSTEM" : "") + (info.isGuest(this) ? " GUEST" : ""));
        }
        pw.println("    Snoozed " + getCaption() + "s (" + this.mSnoozingForCurrentProfiles.size() + "):");
        for (ComponentName name : this.mSnoozingForCurrentProfiles) {
            pw.println("      " + name.flattenToShortString());
        }
    }

    protected void onSettingRestored(String element, String value, int backupSdkInt, int userId) {
        if (!this.mUseXml) {
            Slog.d(this.TAG, "Restored managed service setting: " + element);
            if (this.mConfig.secureSettingName.equals(element) || (this.mConfig.secondarySettingName != null && this.mConfig.secondarySettingName.equals(element))) {
                if (backupSdkInt < 26) {
                    String currentSetting = getApproved(userId, this.mConfig.secureSettingName.equals(element));
                    if (!TextUtils.isEmpty(currentSetting)) {
                        if (TextUtils.isEmpty(value)) {
                            value = currentSetting;
                        } else {
                            value = value + ENABLED_SERVICES_SEPARATOR + currentSetting;
                        }
                    }
                }
                Secure.putStringForUser(this.mContext.getContentResolver(), element, value, userId);
                loadAllowedComponentsFromSettings();
                rebindServices(false);
            }
        }
    }

    public void writeXml(XmlSerializer out, boolean forBackup) throws IOException {
        out.startTag(null, getConfig().xmlTag);
        if (forBackup) {
            trimApprovedListsAccordingToInstalledServices();
        }
        int N = this.mApproved.size();
        for (int i = 0; i < N; i++) {
            int userId = ((Integer) this.mApproved.keyAt(i)).intValue();
            ArrayMap<Boolean, ArraySet<String>> approvedByType = (ArrayMap) this.mApproved.valueAt(i);
            if (approvedByType != null) {
                int M = approvedByType.size();
                for (int j = 0; j < M; j++) {
                    boolean isPrimary = ((Boolean) approvedByType.keyAt(j)).booleanValue();
                    Set<String> approved = (Set) approvedByType.valueAt(j);
                    if (approved != null) {
                        String allowedItems = String.join(ENABLED_SERVICES_SEPARATOR, approved);
                        out.startTag(null, TAG_MANAGED_SERVICES);
                        out.attribute(null, ATT_APPROVED_LIST, allowedItems);
                        out.attribute(null, ATT_USER_ID, Integer.toString(userId));
                        out.attribute(null, ATT_IS_PRIMARY, Boolean.toString(isPrimary));
                        out.endTag(null, TAG_MANAGED_SERVICES);
                        if (!forBackup && isPrimary) {
                            Secure.putStringForUser(this.mContext.getContentResolver(), getConfig().secureSettingName, allowedItems, userId);
                        }
                    }
                }
            }
        }
        out.endTag(null, getConfig().xmlTag);
    }

    protected void migrateToXml() {
        loadAllowedComponentsFromSettings();
    }

    public void readXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        while (true) {
            int type = parser.next();
            if (type == 1) {
                break;
            }
            String tag = parser.getName();
            if (type == 3 && getConfig().xmlTag.equals(tag)) {
                break;
            } else if (type == 2 && TAG_MANAGED_SERVICES.equals(tag)) {
                Slog.i(this.TAG, "Read " + this.mConfig.caption + " permissions from xml");
                String approved = XmlUtils.readStringAttribute(parser, ATT_APPROVED_LIST);
                int userId = XmlUtils.readIntAttribute(parser, ATT_USER_ID, 0);
                boolean isPrimary = XmlUtils.readBooleanAttribute(parser, ATT_IS_PRIMARY, true);
                if (this.mUm.getUserInfo(userId) != null) {
                    addApprovedList(approved, userId, isPrimary);
                }
                this.mUseXml = true;
            }
        }
        rebindServices(false);
    }

    private void loadAllowedComponentsFromSettings() {
        for (UserInfo user : ((UserManager) this.mContext.getSystemService(ATT_USER_ID)).getUsers()) {
            ContentResolver cr = this.mContext.getContentResolver();
            addApprovedList(Secure.getStringForUser(cr, getConfig().secureSettingName, user.id), user.id, true);
            if (!TextUtils.isEmpty(getConfig().secondarySettingName)) {
                addApprovedList(Secure.getStringForUser(cr, getConfig().secondarySettingName, user.id), user.id, false);
            }
        }
        Slog.d(this.TAG, "Done loading approved values from settings");
    }

    private void addApprovedList(String approved, int userId, boolean isPrimary) {
        if (TextUtils.isEmpty(approved)) {
            approved = "";
        }
        ArrayMap<Boolean, ArraySet<String>> approvedByType = (ArrayMap) this.mApproved.get(Integer.valueOf(userId));
        if (approvedByType == null) {
            approvedByType = new ArrayMap();
            this.mApproved.put(Integer.valueOf(userId), approvedByType);
        }
        String[] approvedArray = approved.split(ENABLED_SERVICES_SEPARATOR);
        ArraySet<String> approvedList = new ArraySet();
        for (String pkgOrComponent : approvedArray) {
            String approvedItem = getApprovedValue(pkgOrComponent);
            if (approvedItem != null) {
                approvedList.add(approvedItem);
            }
        }
        approvedByType.put(Boolean.valueOf(isPrimary), approvedList);
    }

    protected boolean isComponentEnabledForPackage(String pkg) {
        return this.mEnabledServicesPackageNames.contains(pkg);
    }

    protected void setPackageOrComponentEnabled(String pkgOrComponent, int userId, boolean isPrimary, boolean enabled) {
        Slog.i(this.TAG, (enabled ? " Allowing " : "Disallowing ") + this.mConfig.caption + " " + pkgOrComponent);
        ArrayMap<Boolean, ArraySet<String>> allowedByType = (ArrayMap) this.mApproved.get(Integer.valueOf(userId));
        if (allowedByType == null) {
            allowedByType = new ArrayMap();
            this.mApproved.put(Integer.valueOf(userId), allowedByType);
        }
        ArraySet<String> approved = (ArraySet) allowedByType.get(Boolean.valueOf(isPrimary));
        if (approved == null) {
            approved = new ArraySet();
            allowedByType.put(Boolean.valueOf(isPrimary), approved);
        }
        String approvedItem = getApprovedValue(pkgOrComponent);
        if (approvedItem != null) {
            if (enabled) {
                approved.add(approvedItem);
            } else {
                approved.remove(approvedItem);
            }
        }
        rebindServices(false);
    }

    private String getApprovedValue(String pkgOrComponent) {
        if (this.mApprovalLevel != 1) {
            return getPackageName(pkgOrComponent);
        }
        if (ComponentName.unflattenFromString(pkgOrComponent) != null) {
            return pkgOrComponent;
        }
        return null;
    }

    protected String getApproved(int userId, boolean primary) {
        return String.join(ENABLED_SERVICES_SEPARATOR, (ArraySet) ((ArrayMap) this.mApproved.getOrDefault(Integer.valueOf(userId), new ArrayMap())).getOrDefault(Boolean.valueOf(primary), new ArraySet()));
    }

    protected List<ComponentName> getAllowedComponents(int userId) {
        List<ComponentName> allowedComponents = new ArrayList();
        ArrayMap<Boolean, ArraySet<String>> allowedByType = (ArrayMap) this.mApproved.getOrDefault(Integer.valueOf(userId), new ArrayMap());
        for (int i = 0; i < allowedByType.size(); i++) {
            allowedComponents.addAll((Collection) ((ArraySet) allowedByType.valueAt(i)).stream().map(-$Lambda$wiIPCfqsozYSTZSw1Uj-TFpH6Dk.$INST$0).filter(com.android.server.notification.-$Lambda$wiIPCfqsozYSTZSw1Uj-TFpH6Dk.AnonymousClass1.$INST$0).collect(Collectors.toList()));
        }
        return allowedComponents;
    }

    static /* synthetic */ boolean lambda$-com_android_server_notification_ManagedServices_17298(ComponentName out) {
        return out != null;
    }

    protected List<String> getAllowedPackages(int userId) {
        List<String> allowedPackages = new ArrayList();
        ArrayMap<Boolean, ArraySet<String>> allowedByType = (ArrayMap) this.mApproved.getOrDefault(Integer.valueOf(userId), new ArrayMap());
        for (int i = 0; i < allowedByType.size(); i++) {
            allowedPackages.addAll((Collection) ((ArraySet) allowedByType.valueAt(i)).stream().map(new com.android.server.notification.-$Lambda$wiIPCfqsozYSTZSw1Uj-TFpH6Dk.AnonymousClass2(this)).collect(Collectors.toList()));
        }
        return allowedPackages;
    }

    /* synthetic */ String -com_android_server_notification_ManagedServices-mthref-1(String str) {
        return getPackageName(str);
    }

    protected boolean isPackageOrComponentAllowed(String pkgOrComponent, int userId) {
        ArrayMap<Boolean, ArraySet<String>> allowedByType = (ArrayMap) this.mApproved.getOrDefault(Integer.valueOf(userId), new ArrayMap());
        for (int i = 0; i < allowedByType.size(); i++) {
            if (((ArraySet) allowedByType.valueAt(i)).contains(pkgOrComponent)) {
                return true;
            }
        }
        return false;
    }

    public void onPackagesChanged(boolean removingPackage, String[] pkgList, int[] uidList) {
        Object obj = null;
        if (this.DEBUG) {
            String str = this.TAG;
            StringBuilder append = new StringBuilder().append("onPackagesChanged removingPackage=").append(removingPackage).append(" pkgList=");
            if (pkgList != null) {
                obj = Arrays.asList(pkgList);
            }
            Slog.d(str, append.append(obj).append(" mEnabledServicesPackageNames=").append(this.mEnabledServicesPackageNames).toString());
        }
        if (pkgList != null && pkgList.length > 0) {
            boolean z = false;
            if (removingPackage) {
                int size = Math.min(pkgList.length, uidList.length);
                for (int i = 0; i < size; i++) {
                    z = removeUninstalledItemsFromApprovedLists(UserHandle.getUserId(uidList[i]), pkgList[i]);
                }
            }
            for (String pkgName : pkgList) {
                if (this.mEnabledServicesPackageNames.contains(pkgName)) {
                    z = true;
                }
            }
            if (z) {
                rebindServices(false);
            }
            if (pkgList == null || pkgList.length <= 0 || !ArrayUtils.contains(pkgList, "com.huawei.bone")) {
                rebindServices(false);
            } else {
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        Slog.d(ManagedServices.this.TAG, "rebind com.huawei.bone service 500ms later");
                        ManagedServices.this.rebindServices(false);
                    }
                }, 500);
            }
        }
    }

    public void onUserRemoved(int user) {
        Slog.i(this.TAG, "Removing approved services for removed user " + user);
        this.mApproved.remove(Integer.valueOf(user));
        rebindServices(true);
    }

    public void onUserSwitched(int user) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "onUserSwitched u=" + user);
        }
        if (Arrays.equals(this.mLastSeenProfileIds, this.mUserProfiles.getCurrentProfileIds())) {
            if (this.DEBUG) {
                Slog.d(this.TAG, "Current profile IDs didn't change, skipping rebindServices().");
            }
            return;
        }
        rebindServices(true);
    }

    public void onUserUnlocked(int user) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "onUserUnlocked u=" + user);
        }
        rebindServices(false);
    }

    private ManagedServiceInfo getServiceFromTokenLocked(IInterface service) {
        if (service == null) {
            return null;
        }
        IBinder token = service.asBinder();
        int N = this.mServices.size();
        for (int i = 0; i < N; i++) {
            ManagedServiceInfo info = (ManagedServiceInfo) this.mServices.get(i);
            if (info.service.asBinder() == token) {
                return info;
            }
        }
        return null;
    }

    protected ManagedServiceInfo checkServiceTokenLocked(IInterface service) {
        checkNotNull(service);
        ManagedServiceInfo info = getServiceFromTokenLocked(service);
        if (info != null) {
            return info;
        }
        throw new SecurityException("Disallowed call from unknown " + getCaption() + ": " + service + " " + service.getClass());
    }

    public void unregisterService(IInterface service, int userid) {
        checkNotNull(service);
        unregisterServiceImpl(service, userid);
    }

    public void registerService(IInterface service, ComponentName component, int userid) {
        checkNotNull(service);
        ManagedServiceInfo info = registerServiceImpl(service, component, userid);
        if (info != null) {
            onServiceAdded(info);
        }
    }

    protected void registerGuestService(ManagedServiceInfo guest) {
        checkNotNull(guest.service);
        if (!checkType(guest.service)) {
            throw new IllegalArgumentException();
        } else if (registerServiceImpl(guest) != null) {
            onServiceAdded(guest);
        }
    }

    protected void setComponentState(ComponentName component, boolean enabled) {
        if ((this.mSnoozingForCurrentProfiles.contains(component) ^ 1) != enabled) {
            if (enabled) {
                this.mSnoozingForCurrentProfiles.remove(component);
            } else {
                this.mSnoozingForCurrentProfiles.add(component);
            }
            Slog.d(this.TAG, (enabled ? "Enabling " : "Disabling ") + "component " + component.flattenToShortString());
            synchronized (this.mMutex) {
                for (int userId : this.mUserProfiles.getCurrentProfileIds()) {
                    if (enabled) {
                        registerServiceLocked(component, userId);
                    } else {
                        unregisterServiceLocked(component, userId);
                    }
                }
            }
        }
    }

    private ArraySet<ComponentName> loadComponentNamesFromValues(ArraySet<String> approved, int userId) {
        if (approved == null || approved.size() == 0) {
            return new ArraySet();
        }
        ArraySet<ComponentName> result = new ArraySet(approved.size());
        for (int i = 0; i < approved.size(); i++) {
            String packageOrComponent = (String) approved.valueAt(i);
            if (!TextUtils.isEmpty(packageOrComponent)) {
                ComponentName component = ComponentName.unflattenFromString(packageOrComponent);
                if (component != null) {
                    result.add(component);
                } else {
                    result.addAll(queryPackageForServices(packageOrComponent, userId));
                }
            }
        }
        return result;
    }

    protected Set<ComponentName> queryPackageForServices(String packageName, int userId) {
        return queryPackageForServices(packageName, 0, userId);
    }

    protected Set<ComponentName> queryPackageForServices(String packageName, int extraFlags, int userId) {
        Set<ComponentName> installed = new ArraySet();
        PackageManager pm = this.mContext.getPackageManager();
        Intent queryIntent = new Intent(this.mConfig.serviceInterface);
        if (!TextUtils.isEmpty(packageName)) {
            queryIntent.setPackage(packageName);
        }
        List<ResolveInfo> installedServices = pm.queryIntentServicesAsUser(queryIntent, extraFlags | 132, userId);
        if (this.DEBUG) {
            Slog.v(this.TAG, this.mConfig.serviceInterface + " services: " + installedServices);
        }
        if (installedServices != null) {
            int count = installedServices.size();
            for (int i = 0; i < count; i++) {
                ServiceInfo info = ((ResolveInfo) installedServices.get(i)).serviceInfo;
                ComponentName component = new ComponentName(info.packageName, info.name);
                if (this.mConfig.bindPermission.equals(info.permission)) {
                    installed.add(component);
                } else {
                    Slog.w(this.TAG, "Skipping " + getCaption() + " service " + info.packageName + "/" + info.name + ": it does not require the permission " + this.mConfig.bindPermission);
                }
            }
        }
        return installed;
    }

    private void trimApprovedListsAccordingToInstalledServices() {
        int N = this.mApproved.size();
        for (int i = 0; i < N; i++) {
            int userId = ((Integer) this.mApproved.keyAt(i)).intValue();
            ArrayMap<Boolean, ArraySet<String>> approvedByType = (ArrayMap) this.mApproved.valueAt(i);
            int M = approvedByType.size();
            for (int j = 0; j < M; j++) {
                ArraySet<String> approved = (ArraySet) approvedByType.valueAt(j);
                for (int k = approved.size() - 1; k >= 0; k--) {
                    String approvedPackageOrComponent = (String) approved.valueAt(k);
                    if (!isValidEntry(approvedPackageOrComponent, userId)) {
                        approved.removeAt(k);
                        Slog.v(this.TAG, "Removing " + approvedPackageOrComponent + " from approved list; no matching services found");
                    } else if (this.DEBUG) {
                        Slog.v(this.TAG, "Keeping " + approvedPackageOrComponent + " on approved list; matching services found");
                    }
                }
            }
        }
    }

    private boolean removeUninstalledItemsFromApprovedLists(int uninstalledUserId, String pkg) {
        ArrayMap<Boolean, ArraySet<String>> approvedByType = (ArrayMap) this.mApproved.get(Integer.valueOf(uninstalledUserId));
        if (approvedByType != null) {
            int M = approvedByType.size();
            for (int j = 0; j < M; j++) {
                ArraySet<String> approved = (ArraySet) approvedByType.valueAt(j);
                for (int k = approved.size() - 1; k >= 0; k--) {
                    String packageOrComponent = (String) approved.valueAt(k);
                    if (TextUtils.equals(pkg, getPackageName(packageOrComponent))) {
                        approved.removeAt(k);
                        if (this.DEBUG) {
                            Slog.v(this.TAG, "Removing " + packageOrComponent + " from approved list; uninstalled");
                        }
                    }
                }
            }
        }
        return false;
    }

    protected String getPackageName(String packageOrComponent) {
        ComponentName component = ComponentName.unflattenFromString(packageOrComponent);
        if (component != null) {
            return component.getPackageName();
        }
        return packageOrComponent;
    }

    protected boolean isValidEntry(String packageOrComponent, int userId) {
        return hasMatchingServices(packageOrComponent, userId);
    }

    private boolean hasMatchingServices(String packageOrComponent, int userId) {
        boolean z = false;
        if (TextUtils.isEmpty(packageOrComponent)) {
            return false;
        }
        if (queryPackageForServices(getPackageName(packageOrComponent), userId).size() > 0) {
            z = true;
        }
        return z;
    }

    private void rebindServices(boolean forceRebind) {
        int i;
        int j;
        if (this.DEBUG) {
            Slog.d(this.TAG, "rebindServices");
        }
        int[] userIds = this.mUserProfiles.getCurrentProfileIds();
        int nUserIds = userIds.length;
        SparseArray<ArraySet<ComponentName>> componentsByUser = new SparseArray();
        for (i = 0; i < nUserIds; i++) {
            int userId = userIds[i];
            ArrayMap<Boolean, ArraySet<String>> approvedLists = (ArrayMap) this.mApproved.get(Integer.valueOf(userIds[i]));
            if (approvedLists != null) {
                int N = approvedLists.size();
                for (j = 0; j < N; j++) {
                    ArraySet<ComponentName> approvedByUser = (ArraySet) componentsByUser.get(userId);
                    if (approvedByUser == null) {
                        approvedByUser = new ArraySet();
                        componentsByUser.put(userId, approvedByUser);
                    }
                    approvedByUser.addAll(loadComponentNamesFromValues((ArraySet) approvedLists.valueAt(j), userId));
                }
            }
        }
        ArrayList<ManagedServiceInfo> removableBoundServices = new ArrayList();
        SparseArray<Set<ComponentName>> toAdd = new SparseArray();
        synchronized (this.mMutex) {
            for (ManagedServiceInfo service : this.mServices) {
                if (!(service.isSystem || (service.isGuest(this) ^ 1) == 0)) {
                    removableBoundServices.add(service);
                }
            }
            this.mEnabledServicesForCurrentProfiles.clear();
            this.mEnabledServicesPackageNames.clear();
            for (i = 0; i < nUserIds; i++) {
                ArraySet<ComponentName> userComponents = (ArraySet) componentsByUser.get(userIds[i]);
                if (userComponents == null) {
                    toAdd.put(userIds[i], new ArraySet());
                } else {
                    Set<ComponentName> add = new HashSet(userComponents);
                    add.removeAll(this.mSnoozingForCurrentProfiles);
                    toAdd.put(userIds[i], add);
                    this.mEnabledServicesForCurrentProfiles.addAll(userComponents);
                    for (j = 0; j < userComponents.size(); j++) {
                        ComponentName component = (ComponentName) userComponents.valueAt(j);
                        this.mEnabledServicesPackageNames.add(component.getPackageName());
                    }
                }
            }
        }
        for (ManagedServiceInfo info : removableBoundServices) {
            component = info.component;
            int oldUser = info.userid;
            Set<ComponentName> allowedComponents = (Set) toAdd.get(info.userid);
            if (allowedComponents != null) {
                if (!allowedComponents.contains(component) || (forceRebind ^ 1) == 0) {
                    Slog.v(this.TAG, "disabling " + getCaption() + " for user " + oldUser + ": " + component);
                    unregisterService(component, oldUser);
                } else {
                    allowedComponents.remove(component);
                }
            }
        }
        for (i = 0; i < nUserIds; i++) {
            for (ComponentName component2 : (Set) toAdd.get(userIds[i])) {
                try {
                    ServiceInfo info2 = this.mPm.getServiceInfo(component2, 786432, userIds[i]);
                    if (info2 == null || (this.mConfig.bindPermission.equals(info2.permission) ^ 1) != 0) {
                        Slog.w(this.TAG, "Not binding " + getCaption() + " service " + component2 + ": it does not require the permission " + this.mConfig.bindPermission);
                    } else {
                        Slog.v(this.TAG, "enabling " + getCaption() + " for " + userIds[i] + ": " + component2);
                        registerService(component2, userIds[i]);
                    }
                } catch (RemoteException e) {
                    e.rethrowFromSystemServer();
                }
            }
        }
        this.mLastSeenProfileIds = userIds;
    }

    private void registerService(ComponentName name, int userid) {
        synchronized (this.mMutex) {
            registerServiceLocked(name, userid);
        }
    }

    public void registerSystemService(ComponentName name, int userid) {
        synchronized (this.mMutex) {
            registerServiceLocked(name, userid, true);
        }
    }

    private void registerServiceLocked(ComponentName name, int userid) {
        registerServiceLocked(name, userid, false);
    }

    private void registerServiceLocked(ComponentName name, int userid, boolean isSystem) {
        if (this.DEBUG) {
            Slog.v(this.TAG, "registerService: " + name + " u=" + userid);
        }
        final String servicesBindingTag = name.toString() + "/" + userid;
        if (!this.mServicesBinding.contains(servicesBindingTag)) {
            this.mServicesBinding.add(servicesBindingTag);
        }
        for (int i = this.mServices.size() - 1; i >= 0; i--) {
            ManagedServiceInfo info = (ManagedServiceInfo) this.mServices.get(i);
            if (name.equals(info.component) && info.userid == userid) {
                Slog.v(this.TAG, "    disconnecting old " + getCaption() + ": " + info.service);
                removeServiceLocked(i);
                if (info.connection != null) {
                    this.mContext.unbindService(info.connection);
                }
            }
        }
        Intent intent = new Intent(this.mConfig.serviceInterface);
        intent.setComponent(name);
        intent.putExtra("android.intent.extra.client_label", this.mConfig.clientLabel);
        intent.putExtra("android.intent.extra.client_intent", PendingIntent.getActivity(this.mContext, 0, new Intent(this.mConfig.settingsAction), 0));
        ApplicationInfo appInfo = null;
        try {
            appInfo = this.mContext.getPackageManager().getApplicationInfo(name.getPackageName(), 0);
        } catch (NameNotFoundException e) {
        }
        final int targetSdkVersion = appInfo != null ? appInfo.targetSdkVersion : 1;
        try {
            Slog.v(this.TAG, "binding: " + intent);
            final int i2 = userid;
            final boolean z = isSystem;
            if (!this.mContext.bindServiceAsUser(intent, new ServiceConnection() {
                IInterface mService;

                public void onServiceConnected(ComponentName name, IBinder binder) {
                    boolean added = false;
                    ManagedServiceInfo managedServiceInfo = null;
                    synchronized (ManagedServices.this.mMutex) {
                        if (ManagedServices.this.mServicesBinding.contains(servicesBindingTag)) {
                            Slog.d(ManagedServices.this.TAG, "onServiceConnected, just remove the servicesBindingTag and add service.");
                            ManagedServices.this.mServicesBinding.remove(servicesBindingTag);
                            try {
                                this.mService = ManagedServices.this.asInterface(binder);
                                managedServiceInfo = ManagedServices.this.newServiceInfo(this.mService, name, i2, z, this, targetSdkVersion);
                                binder.linkToDeath(managedServiceInfo, 0);
                                added = ManagedServices.this.mServices.add(managedServiceInfo);
                            } catch (RemoteException e) {
                            }
                        } else {
                            Slog.d(ManagedServices.this.TAG, "service has been connected, just return.");
                            ManagedServices.this.mContext.unbindService(this);
                            return;
                        }
                    }
                    if (added) {
                        ManagedServices.this.onServiceAdded(managedServiceInfo);
                    }
                }

                public void onServiceDisconnected(ComponentName name) {
                    ManagedServices.this.mServicesBinding.remove(servicesBindingTag);
                    Slog.v(ManagedServices.this.TAG, ManagedServices.this.getCaption() + " connection lost: " + name);
                }
            }, 83886081, new UserHandle(userid))) {
                this.mServicesBinding.remove(servicesBindingTag);
                Slog.w(this.TAG, "Unable to bind " + getCaption() + " service: " + intent);
            }
        } catch (SecurityException ex) {
            this.mServicesBinding.remove(servicesBindingTag);
            Slog.e(this.TAG, "Unable to bind " + getCaption() + " service: " + intent, ex);
        }
    }

    private void unregisterService(ComponentName name, int userid) {
        synchronized (this.mMutex) {
            unregisterServiceLocked(name, userid);
        }
    }

    private void unregisterServiceLocked(ComponentName name, int userid) {
        for (int i = this.mServices.size() - 1; i >= 0; i--) {
            ManagedServiceInfo info = (ManagedServiceInfo) this.mServices.get(i);
            if (name.equals(info.component) && info.userid == userid) {
                removeServiceLocked(i);
                if (info.connection != null) {
                    try {
                        this.mContext.unbindService(info.connection);
                    } catch (IllegalArgumentException ex) {
                        Slog.e(this.TAG, getCaption() + " " + name + " could not be unbound: " + ex);
                    }
                }
            }
        }
    }

    private ManagedServiceInfo removeServiceImpl(IInterface service, int userid) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "removeServiceImpl service=" + service + " u=" + userid);
        }
        ManagedServiceInfo serviceInfo = null;
        synchronized (this.mMutex) {
            for (int i = this.mServices.size() - 1; i >= 0; i--) {
                ManagedServiceInfo info = (ManagedServiceInfo) this.mServices.get(i);
                if (info.service.asBinder() == service.asBinder() && info.userid == userid) {
                    Slog.d(this.TAG, "Removing active service " + info.component);
                    serviceInfo = removeServiceLocked(i);
                }
            }
        }
        return serviceInfo;
    }

    private ManagedServiceInfo removeServiceLocked(int i) {
        ManagedServiceInfo info = (ManagedServiceInfo) this.mServices.remove(i);
        onServiceRemovedLocked(info);
        if (info != null) {
            try {
                info.service.asBinder().unlinkToDeath(info, 0);
            } catch (Exception e) {
                if (this.DEBUG) {
                    Slog.d(this.TAG, "Death link does not exist , error msg: " + e.getMessage());
                }
            }
        }
        return info;
    }

    private void checkNotNull(IInterface service) {
        if (service == null) {
            throw new IllegalArgumentException(getCaption() + " must not be null");
        }
    }

    private ManagedServiceInfo registerServiceImpl(IInterface service, ComponentName component, int userid) {
        return registerServiceImpl(newServiceInfo(service, component, userid, true, null, 21));
    }

    private ManagedServiceInfo registerServiceImpl(ManagedServiceInfo info) {
        synchronized (this.mMutex) {
            try {
                info.service.asBinder().linkToDeath(info, 0);
                this.mServices.add(info);
            } catch (RemoteException e) {
                return null;
            }
        }
        return info;
    }

    private void unregisterServiceImpl(IInterface service, int userid) {
        ManagedServiceInfo info = removeServiceImpl(service, userid);
        if (info != null && info.connection != null && (info.isGuest(this) ^ 1) != 0) {
            this.mContext.unbindService(info.connection);
        }
    }

    public boolean isComponentEnabledForCurrentProfiles(ComponentName component) {
        boolean contains;
        synchronized (this.mMutex) {
            contains = this.mEnabledServicesForCurrentProfiles.contains(component);
        }
        return contains;
    }
}
