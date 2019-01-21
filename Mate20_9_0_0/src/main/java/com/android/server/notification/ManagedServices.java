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
import android.os.Looper;
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
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import com.android.server.job.controllers.JobStatus;
import com.android.server.notification.NotificationManagerService.DumpFilter;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public abstract class ManagedServices {
    static final int APPROVAL_BY_COMPONENT = 1;
    static final int APPROVAL_BY_PACKAGE = 0;
    static final String ATT_APPROVED_LIST = "approved";
    static final String ATT_IS_PRIMARY = "primary";
    static final String ATT_USER_ID = "user";
    static final String ATT_VERSION = "version";
    static final int DB_VERSION = 1;
    protected static final String ENABLED_SERVICES_SEPARATOR = ":";
    private static final int ON_BINDING_DIED_REBIND_DELAY_MS = 10000;
    static final String TAG_MANAGED_SERVICES = "service_listing";
    protected final boolean DEBUG = Log.isLoggable(this.TAG, 3);
    protected final String TAG = getClass().getSimpleName();
    protected int mApprovalLevel;
    private ArrayMap<Integer, ArrayMap<Boolean, ArraySet<String>>> mApproved = new ArrayMap();
    private final Config mConfig;
    protected final Context mContext;
    private ArraySet<ComponentName> mEnabledServicesForCurrentProfiles = new ArraySet();
    private ArraySet<String> mEnabledServicesPackageNames = new ArraySet();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private int[] mLastSeenProfileIds;
    protected final Object mMutex;
    private final IPackageManager mPm;
    private final ArrayList<ManagedServiceInfo> mServices = new ArrayList();
    private final ArrayList<String> mServicesBinding = new ArrayList();
    private final ArraySet<String> mServicesRebinding = new ArraySet();
    private ArraySet<ComponentName> mSnoozingForCurrentProfiles = new ArraySet();
    protected final UserManager mUm;
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
            StringBuilder stringBuilder = new StringBuilder("ManagedServiceInfo[");
            stringBuilder.append("component=");
            stringBuilder.append(this.component);
            stringBuilder.append(",userid=");
            stringBuilder.append(this.userid);
            stringBuilder.append(",isSystem=");
            stringBuilder.append(this.isSystem);
            stringBuilder.append(",targetSdkVersion=");
            stringBuilder.append(this.targetSdkVersion);
            stringBuilder.append(",connection=");
            stringBuilder.append(this.connection == null ? null : "<connection>");
            stringBuilder.append(",service=");
            stringBuilder.append(this.service);
            stringBuilder.append(']');
            return stringBuilder.toString();
        }

        public void writeToProto(ProtoOutputStream proto, long fieldId, ManagedServices host) {
            long token = proto.start(fieldId);
            this.component.writeToProto(proto, 1146756268033L);
            proto.write(1120986464258L, this.userid);
            proto.write(1138166333443L, this.service.getClass().getName());
            proto.write(1133871366148L, this.isSystem);
            proto.write(1133871366149L, isGuest(host));
            proto.end(token);
        }

        public boolean enabledAndUserMatches(int nid) {
            boolean z = false;
            if (!isEnabledForCurrentProfiles()) {
                return false;
            }
            if (this.userid == -1 || this.isSystem || nid == -1 || nid == this.userid) {
                return true;
            }
            if (supportsProfiles() && ManagedServices.this.mUserProfiles.isCurrentProfile(nid) && isPermittedForProfile(nid)) {
                z = true;
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
            boolean z;
            synchronized (this.mCurrentProfiles) {
                UserInfo user = (UserInfo) this.mCurrentProfiles.get(userId);
                z = user != null && user.isManagedProfile();
            }
            return z;
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
        ComponentName cmpt;
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("    Allowed ");
        stringBuilder2.append(getCaption());
        stringBuilder2.append("s:");
        pw.println(stringBuilder2.toString());
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
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("      ");
                        stringBuilder3.append(String.join(ENABLED_SERVICES_SEPARATOR, approved));
                        stringBuilder3.append(" (user: ");
                        stringBuilder3.append(userId);
                        stringBuilder3.append(" isPrimary: ");
                        stringBuilder3.append(isPrimary);
                        stringBuilder3.append(")");
                        pw.println(stringBuilder3.toString());
                    }
                }
            }
        }
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("    All ");
        stringBuilder4.append(getCaption());
        stringBuilder4.append("s (");
        stringBuilder4.append(this.mEnabledServicesForCurrentProfiles.size());
        stringBuilder4.append(") enabled for current profiles:");
        pw.println(stringBuilder4.toString());
        Iterator it = this.mEnabledServicesForCurrentProfiles.iterator();
        while (it.hasNext()) {
            cmpt = (ComponentName) it.next();
            if (filter == null || filter.matches(cmpt)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("      ");
                stringBuilder.append(cmpt);
                pw.println(stringBuilder.toString());
            }
        }
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append("    Live ");
        stringBuilder4.append(getCaption());
        stringBuilder4.append("s (");
        stringBuilder4.append(this.mServices.size());
        stringBuilder4.append("):");
        pw.println(stringBuilder4.toString());
        it = this.mServices.iterator();
        while (it.hasNext()) {
            ManagedServiceInfo info = (ManagedServiceInfo) it.next();
            if (filter == null || filter.matches(info.component)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("      ");
                stringBuilder.append(info.component);
                stringBuilder.append(" (user ");
                stringBuilder.append(info.userid);
                stringBuilder.append("): ");
                stringBuilder.append(info.service);
                stringBuilder.append(info.isSystem ? " SYSTEM" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                stringBuilder.append(info.isGuest(this) ? " GUEST" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                pw.println(stringBuilder.toString());
            }
        }
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append("    Snoozed ");
        stringBuilder4.append(getCaption());
        stringBuilder4.append("s (");
        stringBuilder4.append(this.mSnoozingForCurrentProfiles.size());
        stringBuilder4.append("):");
        pw.println(stringBuilder4.toString());
        it = this.mSnoozingForCurrentProfiles.iterator();
        while (it.hasNext()) {
            cmpt = (ComponentName) it.next();
            stringBuilder = new StringBuilder();
            stringBuilder.append("      ");
            stringBuilder.append(cmpt.flattenToShortString());
            pw.println(stringBuilder.toString());
        }
    }

    public void dump(ProtoOutputStream proto, DumpFilter filter) {
        ProtoOutputStream protoOutputStream = proto;
        DumpFilter dumpFilter = filter;
        protoOutputStream.write(1138166333441L, getCaption());
        int N = this.mApproved.size();
        int i = 0;
        while (i < N) {
            int userId = ((Integer) this.mApproved.keyAt(i)).intValue();
            ArrayMap<Boolean, ArraySet<String>> approvedByType = (ArrayMap) this.mApproved.valueAt(i);
            if (approvedByType != null) {
                int M = approvedByType.size();
                int j = 0;
                while (j < M) {
                    int i2;
                    boolean isPrimary = ((Boolean) approvedByType.keyAt(j)).booleanValue();
                    ArraySet<String> approved = (ArraySet) approvedByType.valueAt(j);
                    if (approvedByType == null || approvedByType.size() <= 0) {
                        i2 = i;
                    } else {
                        long sToken = protoOutputStream.start(2246267895810L);
                        Iterator it = approved.iterator();
                        while (it.hasNext()) {
                            i2 = i;
                            protoOutputStream.write(2237677961217L, (String) it.next());
                            i = i2;
                        }
                        i2 = i;
                        protoOutputStream.write(1120986464258L, userId);
                        protoOutputStream.write(1133871366147L, isPrimary);
                        protoOutputStream.end(sToken);
                    }
                    j++;
                    i = i2;
                }
            }
            i++;
        }
        Iterator it2 = this.mEnabledServicesForCurrentProfiles.iterator();
        while (it2.hasNext()) {
            ComponentName cmpt = (ComponentName) it2.next();
            if (dumpFilter == null || dumpFilter.matches(cmpt)) {
                cmpt.writeToProto(protoOutputStream, 2246267895811L);
            }
        }
        it2 = this.mServices.iterator();
        while (it2.hasNext()) {
            ManagedServiceInfo info = (ManagedServiceInfo) it2.next();
            if (dumpFilter == null || dumpFilter.matches(info.component)) {
                info.writeToProto(protoOutputStream, 2246267895812L, this);
            }
        }
        it2 = this.mSnoozingForCurrentProfiles.iterator();
        while (it2.hasNext()) {
            ((ComponentName) it2.next()).writeToProto(protoOutputStream, 2246267895813L);
        }
    }

    protected void onSettingRestored(String element, String value, int backupSdkInt, int userId) {
        if (!this.mUseXml) {
            String str = this.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Restored managed service setting: ");
            stringBuilder.append(element);
            Slog.d(str, stringBuilder.toString());
            if (this.mConfig.secureSettingName.equals(element) || (this.mConfig.secondarySettingName != null && this.mConfig.secondarySettingName.equals(element))) {
                if (backupSdkInt < 26) {
                    str = getApproved(userId, this.mConfig.secureSettingName.equals(element));
                    if (!TextUtils.isEmpty(str)) {
                        if (TextUtils.isEmpty(value)) {
                            value = str;
                        } else {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(value);
                            stringBuilder.append(ENABLED_SERVICES_SEPARATOR);
                            stringBuilder.append(str);
                            value = stringBuilder.toString();
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
        out.attribute(null, ATT_VERSION, String.valueOf(1));
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

    public void readXml(XmlPullParser parser, Predicate<String> allowedManagedServicePackages) throws XmlPullParserException, IOException {
        int xmlVersion = XmlUtils.readIntAttribute(parser, ATT_VERSION, 0);
        for (UserInfo userInfo : this.mUm.getUsers(true)) {
            upgradeXml(xmlVersion, userInfo.getUserHandle().getIdentifier());
        }
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                break;
            }
            String tag = parser.getName();
            if (type == 3 && getConfig().xmlTag.equals(tag)) {
                break;
            } else if (type == 2 && TAG_MANAGED_SERVICES.equals(tag)) {
                String str = this.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Read ");
                stringBuilder.append(this.mConfig.caption);
                stringBuilder.append(" permissions from xml");
                Slog.i(str, stringBuilder.toString());
                str = XmlUtils.readStringAttribute(parser, ATT_APPROVED_LIST);
                int userId = XmlUtils.readIntAttribute(parser, ATT_USER_ID, 0);
                boolean isPrimary = XmlUtils.readBooleanAttribute(parser, ATT_IS_PRIMARY, true);
                if (allowedManagedServicePackages == null || allowedManagedServicePackages.test(getPackageName(str))) {
                    if (this.mUm.getUserInfo(userId) != null) {
                        addApprovedList(str, userId, isPrimary);
                    }
                    this.mUseXml = true;
                }
            }
        }
        rebindServices(false);
    }

    protected void upgradeXml(int xmlVersion, int userId) {
    }

    private void loadAllowedComponentsFromSettings() {
        for (UserInfo user : this.mUm.getUsers()) {
            ContentResolver cr = this.mContext.getContentResolver();
            addApprovedList(Secure.getStringForUser(cr, getConfig().secureSettingName, user.id), user.id, true);
            if (!TextUtils.isEmpty(getConfig().secondarySettingName)) {
                addApprovedList(Secure.getStringForUser(cr, getConfig().secondarySettingName, user.id), user.id, false);
            }
        }
        Slog.d(this.TAG, "Done loading approved values from settings");
    }

    protected void addApprovedList(String approved, int userId, boolean isPrimary) {
        if (TextUtils.isEmpty(approved)) {
            approved = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        ArrayMap<Boolean, ArraySet<String>> approvedByType = (ArrayMap) this.mApproved.get(Integer.valueOf(userId));
        if (approvedByType == null) {
            approvedByType = new ArrayMap();
            this.mApproved.put(Integer.valueOf(userId), approvedByType);
        }
        ArraySet<String> approvedList = (ArraySet) approvedByType.get(Boolean.valueOf(isPrimary));
        if (approvedList == null) {
            approvedList = new ArraySet();
            approvedByType.put(Boolean.valueOf(isPrimary), approvedList);
        }
        for (String pkgOrComponent : approved.split(ENABLED_SERVICES_SEPARATOR)) {
            String approvedItem = getApprovedValue(pkgOrComponent);
            if (approvedItem != null) {
                approvedList.add(approvedItem);
            }
        }
    }

    protected boolean isComponentEnabledForPackage(String pkg) {
        return this.mEnabledServicesPackageNames.contains(pkg);
    }

    protected void setPackageOrComponentEnabled(String pkgOrComponent, int userId, boolean isPrimary, boolean enabled) {
        String str = this.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(enabled ? " Allowing " : "Disallowing ");
        stringBuilder.append(this.mConfig.caption);
        stringBuilder.append(" ");
        stringBuilder.append(pkgOrComponent);
        Slog.i(str, stringBuilder.toString());
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
            ArraySet<String> allowed = (ArraySet) allowedByType.valueAt(i);
            for (int j = 0; j < allowed.size(); j++) {
                ComponentName cn = ComponentName.unflattenFromString((String) allowed.valueAt(j));
                if (cn != null) {
                    allowedComponents.add(cn);
                }
            }
        }
        return allowedComponents;
    }

    protected List<String> getAllowedPackages(int userId) {
        List<String> allowedPackages = new ArrayList();
        ArrayMap<Boolean, ArraySet<String>> allowedByType = (ArrayMap) this.mApproved.getOrDefault(Integer.valueOf(userId), new ArrayMap());
        for (int i = 0; i < allowedByType.size(); i++) {
            ArraySet<String> allowed = (ArraySet) allowedByType.valueAt(i);
            for (int j = 0; j < allowed.size(); j++) {
                String pkgName = getPackageName((String) allowed.valueAt(j));
                if (!TextUtils.isEmpty(pkgName)) {
                    allowedPackages.add(pkgName);
                }
            }
        }
        return allowedPackages;
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
        if (this.DEBUG) {
            String str = this.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onPackagesChanged removingPackage=");
            stringBuilder.append(removingPackage);
            stringBuilder.append(" pkgList=");
            stringBuilder.append(pkgList == null ? null : Arrays.asList(pkgList));
            stringBuilder.append(" mEnabledServicesPackageNames=");
            stringBuilder.append(this.mEnabledServicesPackageNames);
            Slog.d(str, stringBuilder.toString());
        }
        if (pkgList != null && pkgList.length > 0) {
            int size;
            boolean anyServicesInvolved;
            int i;
            boolean anyServicesInvolved2 = false;
            if (removingPackage) {
                size = Math.min(pkgList.length, uidList.length);
                anyServicesInvolved = false;
                for (i = 0; i < size; i++) {
                    anyServicesInvolved = removeUninstalledItemsFromApprovedLists(UserHandle.getUserId(uidList[i]), pkgList[i]);
                }
                anyServicesInvolved2 = anyServicesInvolved;
            }
            anyServicesInvolved = anyServicesInvolved2;
            for (String pkgName : pkgList) {
                if (this.mEnabledServicesPackageNames.contains(pkgName)) {
                    anyServicesInvolved = true;
                }
            }
            if (anyServicesInvolved) {
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
        String str = this.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Removing approved services for removed user ");
        stringBuilder.append(user);
        Slog.i(str, stringBuilder.toString());
        this.mApproved.remove(Integer.valueOf(user));
        rebindServices(true);
    }

    public void onUserSwitched(int user) {
        if (this.DEBUG) {
            String str = this.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onUserSwitched u=");
            stringBuilder.append(user);
            Slog.d(str, stringBuilder.toString());
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
            String str = this.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onUserUnlocked u=");
            stringBuilder.append(user);
            Slog.d(str, stringBuilder.toString());
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

    protected boolean isServiceTokenValidLocked(IInterface service) {
        if (service == null || getServiceFromTokenLocked(service) == null) {
            return false;
        }
        return true;
    }

    protected ManagedServiceInfo checkServiceTokenLocked(IInterface service) {
        checkNotNull(service);
        ManagedServiceInfo info = getServiceFromTokenLocked(service);
        if (info != null) {
            return info;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Disallowed call from unknown ");
        stringBuilder.append(getCaption());
        stringBuilder.append(": ");
        stringBuilder.append(service);
        stringBuilder.append(" ");
        stringBuilder.append(service.getClass());
        throw new SecurityException(stringBuilder.toString());
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
            String str = this.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(enabled ? "Enabling " : "Disabling ");
            stringBuilder.append("component ");
            stringBuilder.append(component.flattenToShortString());
            Slog.d(str, stringBuilder.toString());
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
        List<ResolveInfo> installedServices = pm.queryIntentServicesAsUser(queryIntent, 132 | extraFlags, userId);
        if (this.DEBUG) {
            String str = this.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mConfig.serviceInterface);
            stringBuilder.append(" services: ");
            stringBuilder.append(installedServices);
            Slog.v(str, stringBuilder.toString());
        }
        if (installedServices != null) {
            int count = installedServices.size();
            for (int i = 0; i < count; i++) {
                ServiceInfo info = ((ResolveInfo) installedServices.get(i)).serviceInfo;
                ComponentName component = new ComponentName(info.packageName, info.name);
                if (this.mConfig.bindPermission.equals(info.permission)) {
                    installed.add(component);
                } else {
                    String str2 = this.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Skipping ");
                    stringBuilder2.append(getCaption());
                    stringBuilder2.append(" service ");
                    stringBuilder2.append(info.packageName);
                    stringBuilder2.append(SliceAuthority.DELIMITER);
                    stringBuilder2.append(info.name);
                    stringBuilder2.append(": it does not require the permission ");
                    stringBuilder2.append(this.mConfig.bindPermission);
                    Slog.w(str2, stringBuilder2.toString());
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
                    String str;
                    StringBuilder stringBuilder;
                    if (!isValidEntry(approvedPackageOrComponent, userId)) {
                        approved.removeAt(k);
                        str = this.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Removing ");
                        stringBuilder.append(approvedPackageOrComponent);
                        stringBuilder.append(" from approved list; no matching services found");
                        Slog.v(str, stringBuilder.toString());
                    } else if (this.DEBUG) {
                        str = this.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Keeping ");
                        stringBuilder.append(approvedPackageOrComponent);
                        stringBuilder.append(" on approved list; matching services found");
                        Slog.v(str, stringBuilder.toString());
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
                            String str = this.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Removing ");
                            stringBuilder.append(packageOrComponent);
                            stringBuilder.append(" from approved list; uninstalled");
                            Slog.v(str, stringBuilder.toString());
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

    protected void rebindServices(boolean forceRebind) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "rebindServices");
        }
        if (this.mConfig.serviceInterface.equals("android.service.notification.NotificationAssistantService")) {
            Slog.w(this.TAG, "rebindServices() ignore for NotificationAssistantService");
            return;
        }
        int N;
        Iterator it;
        ComponentName component;
        int[] userIds = this.mUserProfiles.getCurrentProfileIds();
        int nUserIds = userIds.length;
        SparseArray<ArraySet<ComponentName>> componentsByUser = new SparseArray();
        int i = 0;
        for (int i2 = 0; i2 < nUserIds; i2++) {
            int userId = userIds[i2];
            ArrayMap<Boolean, ArraySet<String>> approvedLists = (ArrayMap) this.mApproved.get(Integer.valueOf(userIds[i2]));
            if (approvedLists != null) {
                N = approvedLists.size();
                for (int j = 0; j < N; j++) {
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
            it = this.mServices.iterator();
            while (it.hasNext()) {
                ManagedServiceInfo service = (ManagedServiceInfo) it.next();
                if (!(service.isSystem || service.isGuest(this))) {
                    removableBoundServices.add(service);
                }
            }
            this.mEnabledServicesForCurrentProfiles.clear();
            this.mEnabledServicesPackageNames.clear();
            for (N = 0; N < nUserIds; N++) {
                ArraySet<ComponentName> userComponents = (ArraySet) componentsByUser.get(userIds[N]);
                if (userComponents == null) {
                    toAdd.put(userIds[N], new ArraySet());
                } else {
                    Set<ComponentName> add = new HashSet(userComponents);
                    add.removeAll(this.mSnoozingForCurrentProfiles);
                    toAdd.put(userIds[N], add);
                    this.mEnabledServicesForCurrentProfiles.addAll(userComponents);
                    for (int j2 = 0; j2 < userComponents.size(); j2++) {
                        this.mEnabledServicesPackageNames.add(((ComponentName) userComponents.valueAt(j2)).getPackageName());
                    }
                }
            }
        }
        Iterator it2 = removableBoundServices.iterator();
        while (it2.hasNext()) {
            ManagedServiceInfo info = (ManagedServiceInfo) it2.next();
            component = info.component;
            int oldUser = info.userid;
            Set<ComponentName> allowedComponents = (Set) toAdd.get(info.userid);
            if (allowedComponents != null) {
                if (!allowedComponents.contains(component) || forceRebind) {
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("disabling ");
                    stringBuilder.append(getCaption());
                    stringBuilder.append(" for user ");
                    stringBuilder.append(oldUser);
                    stringBuilder.append(": ");
                    stringBuilder.append(component);
                    Slog.v(str, stringBuilder.toString());
                    unregisterService(component, oldUser);
                } else {
                    allowedComponents.remove(component);
                }
            }
        }
        while (i < nUserIds) {
            for (ComponentName component2 : (Set) toAdd.get(userIds[i])) {
                try {
                    ServiceInfo info2 = this.mPm.getServiceInfo(component2, 786432, userIds[i]);
                    String str2;
                    StringBuilder stringBuilder2;
                    if (info2 == null) {
                        str2 = this.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Not binding ");
                        stringBuilder2.append(getCaption());
                        stringBuilder2.append(" service ");
                        stringBuilder2.append(component2);
                        stringBuilder2.append(": service not found");
                        Slog.w(str2, stringBuilder2.toString());
                    } else if (this.mConfig.bindPermission.equals(info2.permission)) {
                        str2 = this.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("enabling ");
                        stringBuilder2.append(getCaption());
                        stringBuilder2.append(" for ");
                        stringBuilder2.append(userIds[i]);
                        stringBuilder2.append(": ");
                        stringBuilder2.append(component2);
                        Slog.v(str2, stringBuilder2.toString());
                        registerService(component2, userIds[i]);
                    } else {
                        str2 = this.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Not binding ");
                        stringBuilder2.append(getCaption());
                        stringBuilder2.append(" service ");
                        stringBuilder2.append(component2);
                        stringBuilder2.append(": it does not require the permission ");
                        stringBuilder2.append(this.mConfig.bindPermission);
                        Slog.w(str2, stringBuilder2.toString());
                    }
                } catch (RemoteException e) {
                    e.rethrowFromSystemServer();
                }
            }
            i++;
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
        String str;
        String str2;
        if (this.DEBUG) {
            str = this.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("registerService: ");
            stringBuilder.append(name);
            stringBuilder.append(" u=");
            stringBuilder.append(userid);
            Slog.v(str, stringBuilder.toString());
        }
        str = new StringBuilder();
        str.append(name.toString());
        str.append(SliceAuthority.DELIMITER);
        str.append(userid);
        str = str.toString();
        if (!this.mServicesBinding.contains(str)) {
            this.mServicesBinding.add(str);
        }
        for (int i = this.mServices.size() - 1; i >= 0; i--) {
            ManagedServiceInfo info = (ManagedServiceInfo) this.mServices.get(i);
            if (name.equals(info.component) && info.userid == userid) {
                str2 = this.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("    disconnecting old ");
                stringBuilder2.append(getCaption());
                stringBuilder2.append(": ");
                stringBuilder2.append(info.service);
                Slog.v(str2, stringBuilder2.toString());
                removeServiceLocked(i);
                if (info.connection != null) {
                    try {
                        this.mContext.unbindService(info.connection);
                    } catch (IllegalArgumentException e) {
                        String str3 = this.TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("failed to unbind ");
                        stringBuilder3.append(name);
                        Slog.e(str3, stringBuilder3.toString(), e);
                    }
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
        } catch (NameNotFoundException e2) {
        }
        ApplicationInfo appInfo2 = appInfo;
        final int targetSdkVersion = appInfo2 != null ? appInfo2.targetSdkVersion : 1;
        String str4;
        StringBuilder stringBuilder4;
        try {
            String str5 = this.TAG;
            StringBuilder stringBuilder5 = new StringBuilder();
            stringBuilder5.append("binding: ");
            stringBuilder5.append(intent);
            Slog.v(str5, stringBuilder5.toString());
            str2 = str;
            final int i2 = userid;
            final boolean z = isSystem;
            if (!this.mContext.bindServiceAsUser(intent, new ServiceConnection() {
                IInterface mService;

                /* JADX WARNING: Missing block: B:12:0x0061, code skipped:
            if (r0 == false) goto L_0x0068;
     */
                /* JADX WARNING: Missing block: B:13:0x0063, code skipped:
            r11.this$0.onServiceAdded(r1);
     */
                /* JADX WARNING: Missing block: B:14:0x0068, code skipped:
            return;
     */
                /* Code decompiled incorrectly, please refer to instructions dump. */
                public void onServiceConnected(ComponentName name, IBinder binder) {
                    boolean added = false;
                    ManagedServiceInfo info = null;
                    synchronized (ManagedServices.this.mMutex) {
                        ManagedServices.this.mServicesRebinding.remove(str2);
                        if (ManagedServices.this.mServicesBinding.contains(str2)) {
                            Slog.d(ManagedServices.this.TAG, "onServiceConnected, just remove the servicesBindingTag and add service.");
                            ManagedServices.this.mServicesBinding.remove(str2);
                            try {
                                this.mService = ManagedServices.this.asInterface(binder);
                                info = ManagedServices.this.newServiceInfo(this.mService, name, i2, z, this, targetSdkVersion);
                                binder.linkToDeath(info, 0);
                                added = ManagedServices.this.mServices.add(info);
                            } catch (RemoteException e) {
                            }
                        } else {
                            Slog.d(ManagedServices.this.TAG, "service has been connected, just return.");
                            ManagedServices.this.mContext.unbindService(this);
                        }
                    }
                }

                public void onServiceDisconnected(ComponentName name) {
                    ManagedServices.this.mServicesBinding.remove(str2);
                    String str = ManagedServices.this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(ManagedServices.this.getCaption());
                    stringBuilder.append(" connection lost: ");
                    stringBuilder.append(name);
                    Slog.v(str, stringBuilder.toString());
                }

                public void onBindingDied(final ComponentName name) {
                    String str = ManagedServices.this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(ManagedServices.this.getCaption());
                    stringBuilder.append(" binding died: ");
                    stringBuilder.append(name);
                    Slog.w(str, stringBuilder.toString());
                    synchronized (ManagedServices.this.mMutex) {
                        ManagedServices.this.mServicesBinding.remove(str2);
                        try {
                            ManagedServices.this.mContext.unbindService(this);
                        } catch (IllegalArgumentException e) {
                            String str2 = ManagedServices.this.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("failed to unbind ");
                            stringBuilder2.append(name);
                            Slog.e(str2, stringBuilder2.toString(), e);
                        }
                        if (ManagedServices.this.mServicesRebinding.contains(str2)) {
                            String str3 = ManagedServices.this.TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append(ManagedServices.this.getCaption());
                            stringBuilder3.append(" not rebinding as a previous rebind attempt was made: ");
                            stringBuilder3.append(name);
                            Slog.v(str3, stringBuilder3.toString());
                        } else {
                            ManagedServices.this.mServicesRebinding.add(str2);
                            ManagedServices.this.mHandler.postDelayed(new Runnable() {
                                public void run() {
                                    ManagedServices.this.registerService(name, i2);
                                }
                            }, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                        }
                    }
                }
            }, 83886081, new UserHandle(userid))) {
                this.mServicesBinding.remove(str);
                str4 = this.TAG;
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Unable to bind ");
                stringBuilder4.append(getCaption());
                stringBuilder4.append(" service: ");
                stringBuilder4.append(intent);
                Slog.w(str4, stringBuilder4.toString());
            }
        } catch (SecurityException ex) {
            this.mServicesBinding.remove(str);
            str4 = this.TAG;
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("Unable to bind ");
            stringBuilder4.append(getCaption());
            stringBuilder4.append(" service: ");
            stringBuilder4.append(intent);
            Slog.e(str4, stringBuilder4.toString(), ex);
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
                        String str = this.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(getCaption());
                        stringBuilder.append(" ");
                        stringBuilder.append(name);
                        stringBuilder.append(" could not be unbound: ");
                        stringBuilder.append(ex);
                        Slog.e(str, stringBuilder.toString());
                    }
                }
            }
        }
    }

    private ManagedServiceInfo removeServiceImpl(IInterface service, int userid) {
        if (this.DEBUG) {
            String str = this.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeServiceImpl service=");
            stringBuilder.append(service);
            stringBuilder.append(" u=");
            stringBuilder.append(userid);
            Slog.d(str, stringBuilder.toString());
        }
        ManagedServiceInfo serviceInfo = null;
        synchronized (this.mMutex) {
            for (int i = this.mServices.size() - 1; i >= 0; i--) {
                ManagedServiceInfo info = (ManagedServiceInfo) this.mServices.get(i);
                if (info.service.asBinder() == service.asBinder() && info.userid == userid) {
                    String str2 = this.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Removing active service ");
                    stringBuilder2.append(info.component);
                    Slog.d(str2, stringBuilder2.toString());
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
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Death link does not exist , error msg: ");
                    stringBuilder.append(e.getMessage());
                    Slog.d(str, stringBuilder.toString());
                }
            }
        }
        return info;
    }

    private void checkNotNull(IInterface service) {
        if (service == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getCaption());
            stringBuilder.append(" must not be null");
            throw new IllegalArgumentException(stringBuilder.toString());
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
            } catch (Throwable th) {
            }
        }
        return info;
    }

    private void unregisterServiceImpl(IInterface service, int userid) {
        ManagedServiceInfo info = removeServiceImpl(service, userid);
        if (info != null && info.connection != null && !info.isGuest(this)) {
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
