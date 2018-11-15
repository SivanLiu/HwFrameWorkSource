package com.android.server.devicepolicy;

import android.app.AppOpsManagerInternal;
import android.app.admin.SystemUpdateInfo;
import android.app.admin.SystemUpdatePolicy;
import android.content.ComponentName;
import android.content.pm.PackageManagerInternal;
import android.content.pm.UserInfo;
import android.os.Binder;
import android.os.Environment;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.Xml;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.LocalServices;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class Owners {
    private static final String ATTR_COMPONENT_NAME = "component";
    private static final String ATTR_FREEZE_RECORD_END = "end";
    private static final String ATTR_FREEZE_RECORD_START = "start";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_PACKAGE = "package";
    private static final String ATTR_REMOTE_BUGREPORT_HASH = "remoteBugreportHash";
    private static final String ATTR_REMOTE_BUGREPORT_URI = "remoteBugreportUri";
    private static final String ATTR_USERID = "userId";
    private static final String ATTR_USER_RESTRICTIONS_MIGRATED = "userRestrictionsMigrated";
    private static final boolean DEBUG = false;
    private static final String DEVICE_OWNER_XML = "device_owner_2.xml";
    private static final String DEVICE_OWNER_XML_LEGACY = "device_owner.xml";
    private static final String PROFILE_OWNER_XML = "profile_owner.xml";
    private static final String TAG = "DevicePolicyManagerService";
    private static final String TAG_DEVICE_INITIALIZER = "device-initializer";
    private static final String TAG_DEVICE_OWNER = "device-owner";
    private static final String TAG_DEVICE_OWNER_CONTEXT = "device-owner-context";
    private static final String TAG_FREEZE_PERIOD_RECORD = "freeze-record";
    private static final String TAG_PENDING_OTA_INFO = "pending-ota-info";
    private static final String TAG_PROFILE_OWNER = "profile-owner";
    private static final String TAG_ROOT = "root";
    private static final String TAG_SYSTEM_UPDATE_POLICY = "system-update-policy";
    private OwnerInfo mDeviceOwner;
    private int mDeviceOwnerUserId;
    private final Injector mInjector;
    private final Object mLock;
    private final PackageManagerInternal mPackageManagerInternal;
    private final ArrayMap<Integer, OwnerInfo> mProfileOwners;
    private boolean mSystemReady;
    private LocalDate mSystemUpdateFreezeEnd;
    private LocalDate mSystemUpdateFreezeStart;
    private SystemUpdateInfo mSystemUpdateInfo;
    private SystemUpdatePolicy mSystemUpdatePolicy;
    private final UserManager mUserManager;
    private final UserManagerInternal mUserManagerInternal;

    private static abstract class FileReadWriter {
        private final File mFile;

        abstract boolean readInner(XmlPullParser xmlPullParser, int i, String str);

        abstract boolean shouldWrite();

        abstract void writeInner(XmlSerializer xmlSerializer) throws IOException;

        protected FileReadWriter(File file) {
            this.mFile = file;
        }

        void writeToFileLocked() {
            if (shouldWrite()) {
                AtomicFile f = new AtomicFile(this.mFile);
                FileOutputStream outputStream = null;
                try {
                    outputStream = f.startWrite();
                    XmlSerializer out = new FastXmlSerializer();
                    out.setOutput(outputStream, StandardCharsets.UTF_8.name());
                    out.startDocument(null, Boolean.valueOf(true));
                    out.startTag(null, Owners.TAG_ROOT);
                    writeInner(out);
                    out.endTag(null, Owners.TAG_ROOT);
                    out.endDocument();
                    out.flush();
                    f.finishWrite(outputStream);
                } catch (IOException e) {
                    Slog.e(Owners.TAG, "Exception when writing", e);
                    if (outputStream != null) {
                        f.failWrite(outputStream);
                    }
                }
                return;
            }
            if (this.mFile.exists() && !this.mFile.delete()) {
                String str = Owners.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to remove ");
                stringBuilder.append(this.mFile.getPath());
                Slog.e(str, stringBuilder.toString());
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:25:0x006d A:{PHI: r1 , Splitter: B:4:0x0011, ExcHandler: org.xmlpull.v1.XmlPullParserException (r2_2 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:25:0x006d, code:
            r2 = move-exception;
     */
        /* JADX WARNING: Missing block: B:27:?, code:
            android.util.Slog.e(com.android.server.devicepolicy.Owners.TAG, "Error parsing owners information file", r2);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        void readFromFileLocked() {
            if (this.mFile.exists()) {
                InputStream input = null;
                try {
                    input = new AtomicFile(this.mFile).openRead();
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(input, StandardCharsets.UTF_8.name());
                    int depth = 0;
                    while (true) {
                        int next = parser.next();
                        int type = next;
                        if (next != 1) {
                            switch (type) {
                                case 2:
                                    depth++;
                                    String tag = parser.getName();
                                    if (depth != 1) {
                                        if (readInner(parser, depth, tag)) {
                                            break;
                                        }
                                        IoUtils.closeQuietly(input);
                                        return;
                                    } else if (Owners.TAG_ROOT.equals(tag)) {
                                        continue;
                                    } else {
                                        String str = Owners.TAG;
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("Invalid root tag: ");
                                        stringBuilder.append(tag);
                                        Slog.e(str, stringBuilder.toString());
                                        IoUtils.closeQuietly(input);
                                        return;
                                    }
                                case 3:
                                    depth--;
                                    break;
                            }
                        }
                    }
                } catch (Exception e) {
                } catch (Throwable th) {
                    IoUtils.closeQuietly(input);
                }
                IoUtils.closeQuietly(input);
            }
        }
    }

    @VisibleForTesting
    public static class Injector {
        File environmentGetDataSystemDirectory() {
            return Environment.getDataSystemDirectory();
        }

        File environmentGetUserSystemDirectory(int userId) {
            return Environment.getUserSystemDirectory(userId);
        }
    }

    static class OwnerInfo {
        public final ComponentName admin;
        public final String name;
        public final String packageName;
        public String remoteBugreportHash;
        public String remoteBugreportUri;
        public boolean userRestrictionsMigrated;

        public OwnerInfo(String name, String packageName, boolean userRestrictionsMigrated, String remoteBugreportUri, String remoteBugreportHash) {
            this.name = name;
            this.packageName = packageName;
            this.admin = new ComponentName(packageName, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            this.userRestrictionsMigrated = userRestrictionsMigrated;
            this.remoteBugreportUri = remoteBugreportUri;
            this.remoteBugreportHash = remoteBugreportHash;
        }

        public OwnerInfo(String name, ComponentName admin, boolean userRestrictionsMigrated, String remoteBugreportUri, String remoteBugreportHash) {
            this.name = name;
            this.admin = admin;
            this.packageName = admin.getPackageName();
            this.userRestrictionsMigrated = userRestrictionsMigrated;
            this.remoteBugreportUri = remoteBugreportUri;
            this.remoteBugreportHash = remoteBugreportHash;
        }

        public void writeToXml(XmlSerializer out, String tag) throws IOException {
            out.startTag(null, tag);
            out.attribute(null, "package", this.packageName);
            if (this.name != null) {
                out.attribute(null, "name", this.name);
            }
            if (this.admin != null) {
                out.attribute(null, Owners.ATTR_COMPONENT_NAME, this.admin.flattenToString());
            }
            out.attribute(null, Owners.ATTR_USER_RESTRICTIONS_MIGRATED, String.valueOf(this.userRestrictionsMigrated));
            if (this.remoteBugreportUri != null) {
                out.attribute(null, Owners.ATTR_REMOTE_BUGREPORT_URI, this.remoteBugreportUri);
            }
            if (this.remoteBugreportHash != null) {
                out.attribute(null, Owners.ATTR_REMOTE_BUGREPORT_HASH, this.remoteBugreportHash);
            }
            out.endTag(null, tag);
        }

        public static OwnerInfo readFromXml(XmlPullParser parser) {
            String packageName = parser.getAttributeValue(null, "package");
            String name = parser.getAttributeValue(null, "name");
            String componentName = parser.getAttributeValue(null, Owners.ATTR_COMPONENT_NAME);
            boolean userRestrictionsMigrated = "true".equals(parser.getAttributeValue(null, Owners.ATTR_USER_RESTRICTIONS_MIGRATED));
            String remoteBugreportUri = parser.getAttributeValue(null, Owners.ATTR_REMOTE_BUGREPORT_URI);
            String remoteBugreportHash = parser.getAttributeValue(null, Owners.ATTR_REMOTE_BUGREPORT_HASH);
            if (componentName != null) {
                ComponentName admin = ComponentName.unflattenFromString(componentName);
                if (admin != null) {
                    return new OwnerInfo(name, admin, userRestrictionsMigrated, remoteBugreportUri, remoteBugreportHash);
                }
                String str = Owners.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error parsing owner file. Bad component name ");
                stringBuilder.append(componentName);
                Slog.e(str, stringBuilder.toString());
            }
            return new OwnerInfo(name, packageName, userRestrictionsMigrated, remoteBugreportUri, remoteBugreportHash);
        }

        public void dump(String prefix, PrintWriter pw) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("admin=");
            stringBuilder.append(this.admin);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("name=");
            stringBuilder.append(this.name);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("package=");
            stringBuilder.append(this.packageName);
            pw.println(stringBuilder.toString());
        }
    }

    private class DeviceOwnerReadWriter extends FileReadWriter {
        protected DeviceOwnerReadWriter() {
            super(Owners.this.getDeviceOwnerFile());
        }

        boolean shouldWrite() {
            return (Owners.this.mDeviceOwner == null && Owners.this.mSystemUpdatePolicy == null && Owners.this.mSystemUpdateInfo == null) ? false : true;
        }

        void writeInner(XmlSerializer out) throws IOException {
            if (Owners.this.mDeviceOwner != null) {
                Owners.this.mDeviceOwner.writeToXml(out, Owners.TAG_DEVICE_OWNER);
                out.startTag(null, Owners.TAG_DEVICE_OWNER_CONTEXT);
                out.attribute(null, Owners.ATTR_USERID, String.valueOf(Owners.this.mDeviceOwnerUserId));
                out.endTag(null, Owners.TAG_DEVICE_OWNER_CONTEXT);
            }
            if (Owners.this.mSystemUpdatePolicy != null) {
                out.startTag(null, Owners.TAG_SYSTEM_UPDATE_POLICY);
                Owners.this.mSystemUpdatePolicy.saveToXml(out);
                out.endTag(null, Owners.TAG_SYSTEM_UPDATE_POLICY);
            }
            if (Owners.this.mSystemUpdateInfo != null) {
                Owners.this.mSystemUpdateInfo.writeToXml(out, Owners.TAG_PENDING_OTA_INFO);
            }
            if (Owners.this.mSystemUpdateFreezeStart != null || Owners.this.mSystemUpdateFreezeEnd != null) {
                out.startTag(null, Owners.TAG_FREEZE_PERIOD_RECORD);
                if (Owners.this.mSystemUpdateFreezeStart != null) {
                    out.attribute(null, Owners.ATTR_FREEZE_RECORD_START, Owners.this.mSystemUpdateFreezeStart.toString());
                }
                if (Owners.this.mSystemUpdateFreezeEnd != null) {
                    out.attribute(null, Owners.ATTR_FREEZE_RECORD_END, Owners.this.mSystemUpdateFreezeEnd.toString());
                }
                out.endTag(null, Owners.TAG_FREEZE_PERIOD_RECORD);
            }
        }

        /* JADX WARNING: Missing block: B:18:0x003e, code:
            if (r9.equals(com.android.server.devicepolicy.Owners.TAG_DEVICE_INITIALIZER) != false) goto L_0x004d;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        boolean readInner(XmlPullParser parser, int depth, String tag) {
            String str;
            boolean z = true;
            if (depth > 2) {
                return true;
            }
            switch (tag.hashCode()) {
                case -2101756875:
                    if (tag.equals(Owners.TAG_PENDING_OTA_INFO)) {
                        z = true;
                        break;
                    }
                case -2038823445:
                    break;
                case -2020438916:
                    if (tag.equals(Owners.TAG_DEVICE_OWNER)) {
                        z = false;
                        break;
                    }
                case -1900517026:
                    if (tag.equals(Owners.TAG_DEVICE_OWNER_CONTEXT)) {
                        z = true;
                        break;
                    }
                case 1303827527:
                    if (tag.equals(Owners.TAG_FREEZE_PERIOD_RECORD)) {
                        z = true;
                        break;
                    }
                case 1748301720:
                    if (tag.equals(Owners.TAG_SYSTEM_UPDATE_POLICY)) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            String userIdString;
            switch (z) {
                case false:
                    Owners.this.mDeviceOwner = OwnerInfo.readFromXml(parser);
                    Owners.this.mDeviceOwnerUserId = 0;
                    break;
                case true:
                    userIdString = parser.getAttributeValue(null, Owners.ATTR_USERID);
                    try {
                        Owners.this.mDeviceOwnerUserId = Integer.parseInt(userIdString);
                        break;
                    } catch (NumberFormatException e) {
                        str = Owners.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Error parsing user-id ");
                        stringBuilder.append(userIdString);
                        Slog.e(str, stringBuilder.toString());
                        break;
                    }
                case true:
                    break;
                case true:
                    Owners.this.mSystemUpdatePolicy = SystemUpdatePolicy.restoreFromXml(parser);
                    break;
                case true:
                    Owners.this.mSystemUpdateInfo = SystemUpdateInfo.readFromXml(parser);
                    break;
                case true:
                    userIdString = parser.getAttributeValue(null, Owners.ATTR_FREEZE_RECORD_START);
                    str = parser.getAttributeValue(null, Owners.ATTR_FREEZE_RECORD_END);
                    if (!(userIdString == null || str == null)) {
                        Owners.this.mSystemUpdateFreezeStart = LocalDate.parse(userIdString);
                        Owners.this.mSystemUpdateFreezeEnd = LocalDate.parse(str);
                        if (Owners.this.mSystemUpdateFreezeStart.isAfter(Owners.this.mSystemUpdateFreezeEnd)) {
                            Slog.e(Owners.TAG, "Invalid system update freeze record loaded");
                            Owners.this.mSystemUpdateFreezeStart = null;
                            Owners.this.mSystemUpdateFreezeEnd = null;
                            break;
                        }
                    }
                    break;
                default:
                    userIdString = Owners.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unexpected tag: ");
                    stringBuilder2.append(tag);
                    Slog.e(userIdString, stringBuilder2.toString());
                    return false;
            }
            return true;
        }
    }

    private class ProfileOwnerReadWriter extends FileReadWriter {
        private final int mUserId;

        ProfileOwnerReadWriter(int userId) {
            super(Owners.this.getProfileOwnerFile(userId));
            this.mUserId = userId;
        }

        boolean shouldWrite() {
            return Owners.this.mProfileOwners.get(Integer.valueOf(this.mUserId)) != null;
        }

        void writeInner(XmlSerializer out) throws IOException {
            OwnerInfo profileOwner = (OwnerInfo) Owners.this.mProfileOwners.get(Integer.valueOf(this.mUserId));
            if (profileOwner != null) {
                profileOwner.writeToXml(out, Owners.TAG_PROFILE_OWNER);
            }
        }

        boolean readInner(XmlPullParser parser, int depth, String tag) {
            if (depth > 2) {
                return true;
            }
            boolean z = true;
            if (tag.hashCode() == 2145316239 && tag.equals(Owners.TAG_PROFILE_OWNER)) {
                z = false;
            }
            if (z) {
                String str = Owners.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected tag: ");
                stringBuilder.append(tag);
                Slog.e(str, stringBuilder.toString());
                return false;
            }
            Owners.this.mProfileOwners.put(Integer.valueOf(this.mUserId), OwnerInfo.readFromXml(parser));
            return true;
        }
    }

    public Owners(UserManager userManager, UserManagerInternal userManagerInternal, PackageManagerInternal packageManagerInternal) {
        this(userManager, userManagerInternal, packageManagerInternal, new Injector());
    }

    @VisibleForTesting
    Owners(UserManager userManager, UserManagerInternal userManagerInternal, PackageManagerInternal packageManagerInternal, Injector injector) {
        this.mDeviceOwnerUserId = -10000;
        this.mProfileOwners = new ArrayMap();
        this.mLock = new Object();
        this.mUserManager = userManager;
        this.mUserManagerInternal = userManagerInternal;
        this.mPackageManagerInternal = packageManagerInternal;
        this.mInjector = injector;
    }

    void load() {
        synchronized (this.mLock) {
            File legacy = getLegacyConfigFile();
            List<UserInfo> users = this.mUserManager.getUsers(true);
            if (readLegacyOwnerFileLocked(legacy)) {
                writeDeviceOwner();
                for (Integer userId : getProfileOwnerKeys()) {
                    writeProfileOwner(userId.intValue());
                }
                if (!legacy.delete()) {
                    Slog.e(TAG, "Failed to remove the legacy setting file");
                }
            } else {
                new DeviceOwnerReadWriter().readFromFileLocked();
                for (UserInfo ui : users) {
                    new ProfileOwnerReadWriter(ui.id).readFromFileLocked();
                }
            }
            this.mUserManagerInternal.setDeviceManaged(hasDeviceOwner());
            for (UserInfo ui2 : users) {
                this.mUserManagerInternal.setUserManaged(ui2.id, hasProfileOwner(ui2.id));
            }
            if (hasDeviceOwner() && hasProfileOwner(getDeviceOwnerUserId())) {
                Slog.w(TAG, String.format("User %d has both DO and PO, which is not supported", new Object[]{Integer.valueOf(getDeviceOwnerUserId())}));
            }
            pushToPackageManagerLocked();
            pushToAppOpsLocked();
        }
    }

    private void pushToPackageManagerLocked() {
        SparseArray<String> po = new SparseArray();
        for (int i = this.mProfileOwners.size() - 1; i >= 0; i--) {
            po.put(((Integer) this.mProfileOwners.keyAt(i)).intValue(), ((OwnerInfo) this.mProfileOwners.valueAt(i)).packageName);
        }
        this.mPackageManagerInternal.setDeviceAndProfileOwnerPackages(this.mDeviceOwnerUserId, this.mDeviceOwner != null ? this.mDeviceOwner.packageName : null, po);
    }

    String getDeviceOwnerPackageName() {
        String str;
        synchronized (this.mLock) {
            str = this.mDeviceOwner != null ? this.mDeviceOwner.packageName : null;
        }
        return str;
    }

    int getDeviceOwnerUserId() {
        int i;
        synchronized (this.mLock) {
            i = this.mDeviceOwnerUserId;
        }
        return i;
    }

    Pair<Integer, ComponentName> getDeviceOwnerUserIdAndComponent() {
        synchronized (this.mLock) {
            if (this.mDeviceOwner == null) {
                return null;
            }
            Pair<Integer, ComponentName> create = Pair.create(Integer.valueOf(this.mDeviceOwnerUserId), this.mDeviceOwner.admin);
            return create;
        }
    }

    String getDeviceOwnerName() {
        String str;
        synchronized (this.mLock) {
            str = this.mDeviceOwner != null ? this.mDeviceOwner.name : null;
        }
        return str;
    }

    ComponentName getDeviceOwnerComponent() {
        ComponentName componentName;
        synchronized (this.mLock) {
            componentName = this.mDeviceOwner != null ? this.mDeviceOwner.admin : null;
        }
        return componentName;
    }

    String getDeviceOwnerRemoteBugreportUri() {
        String str;
        synchronized (this.mLock) {
            str = this.mDeviceOwner != null ? this.mDeviceOwner.remoteBugreportUri : null;
        }
        return str;
    }

    String getDeviceOwnerRemoteBugreportHash() {
        String str;
        synchronized (this.mLock) {
            str = this.mDeviceOwner != null ? this.mDeviceOwner.remoteBugreportHash : null;
        }
        return str;
    }

    void setDeviceOwner(ComponentName admin, String ownerName, int userId) {
        if (userId < 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid user id for device owner user: ");
            stringBuilder.append(userId);
            Slog.e(str, stringBuilder.toString());
            return;
        }
        synchronized (this.mLock) {
            setDeviceOwnerWithRestrictionsMigrated(admin, ownerName, userId, true);
        }
    }

    void setDeviceOwnerWithRestrictionsMigrated(ComponentName admin, String ownerName, int userId, boolean userRestrictionsMigrated) {
        synchronized (this.mLock) {
            this.mDeviceOwner = new OwnerInfo(ownerName, admin, userRestrictionsMigrated, null, null);
            this.mDeviceOwnerUserId = userId;
            this.mUserManagerInternal.setDeviceManaged(true);
            pushToPackageManagerLocked();
            pushToAppOpsLocked();
        }
    }

    void clearDeviceOwner() {
        synchronized (this.mLock) {
            this.mDeviceOwner = null;
            this.mDeviceOwnerUserId = -10000;
            this.mUserManagerInternal.setDeviceManaged(false);
            pushToPackageManagerLocked();
            pushToAppOpsLocked();
        }
    }

    void setProfileOwner(ComponentName admin, String ownerName, int userId) {
        synchronized (this.mLock) {
            this.mProfileOwners.put(Integer.valueOf(userId), new OwnerInfo(ownerName, admin, true, null, null));
            this.mUserManagerInternal.setUserManaged(userId, true);
            pushToPackageManagerLocked();
            pushToAppOpsLocked();
        }
    }

    void removeProfileOwner(int userId) {
        synchronized (this.mLock) {
            this.mProfileOwners.remove(Integer.valueOf(userId));
            this.mUserManagerInternal.setUserManaged(userId, false);
            pushToPackageManagerLocked();
            pushToAppOpsLocked();
        }
    }

    void transferProfileOwner(ComponentName target, int userId) {
        synchronized (this.mLock) {
            OwnerInfo ownerInfo = (OwnerInfo) this.mProfileOwners.get(Integer.valueOf(userId));
            this.mProfileOwners.put(Integer.valueOf(userId), new OwnerInfo(target.getPackageName(), target, ownerInfo.userRestrictionsMigrated, ownerInfo.remoteBugreportUri, ownerInfo.remoteBugreportHash));
            pushToPackageManagerLocked();
            pushToAppOpsLocked();
        }
    }

    void transferDeviceOwnership(ComponentName target) {
        synchronized (this.mLock) {
            this.mDeviceOwner = new OwnerInfo(null, target, this.mDeviceOwner.userRestrictionsMigrated, this.mDeviceOwner.remoteBugreportUri, this.mDeviceOwner.remoteBugreportHash);
            pushToPackageManagerLocked();
            pushToAppOpsLocked();
        }
    }

    ComponentName getProfileOwnerComponent(int userId) {
        ComponentName componentName;
        synchronized (this.mLock) {
            OwnerInfo profileOwner = (OwnerInfo) this.mProfileOwners.get(Integer.valueOf(userId));
            componentName = profileOwner != null ? profileOwner.admin : null;
        }
        return componentName;
    }

    String getProfileOwnerName(int userId) {
        String str;
        synchronized (this.mLock) {
            OwnerInfo profileOwner = (OwnerInfo) this.mProfileOwners.get(Integer.valueOf(userId));
            str = profileOwner != null ? profileOwner.name : null;
        }
        return str;
    }

    String getProfileOwnerPackage(int userId) {
        String str;
        synchronized (this.mLock) {
            OwnerInfo profileOwner = (OwnerInfo) this.mProfileOwners.get(Integer.valueOf(userId));
            str = profileOwner != null ? profileOwner.packageName : null;
        }
        return str;
    }

    Set<Integer> getProfileOwnerKeys() {
        Set<Integer> keySet;
        synchronized (this.mLock) {
            keySet = this.mProfileOwners.keySet();
        }
        return keySet;
    }

    SystemUpdatePolicy getSystemUpdatePolicy() {
        SystemUpdatePolicy systemUpdatePolicy;
        synchronized (this.mLock) {
            systemUpdatePolicy = this.mSystemUpdatePolicy;
        }
        return systemUpdatePolicy;
    }

    void setSystemUpdatePolicy(SystemUpdatePolicy systemUpdatePolicy) {
        synchronized (this.mLock) {
            this.mSystemUpdatePolicy = systemUpdatePolicy;
        }
    }

    void clearSystemUpdatePolicy() {
        synchronized (this.mLock) {
            this.mSystemUpdatePolicy = null;
        }
    }

    Pair<LocalDate, LocalDate> getSystemUpdateFreezePeriodRecord() {
        Pair<LocalDate, LocalDate> pair;
        synchronized (this.mLock) {
            pair = new Pair(this.mSystemUpdateFreezeStart, this.mSystemUpdateFreezeEnd);
        }
        return pair;
    }

    String getSystemUpdateFreezePeriodRecordAsString() {
        StringBuilder freezePeriodRecord = new StringBuilder();
        freezePeriodRecord.append("start: ");
        if (this.mSystemUpdateFreezeStart != null) {
            freezePeriodRecord.append(this.mSystemUpdateFreezeStart.toString());
        } else {
            freezePeriodRecord.append("null");
        }
        freezePeriodRecord.append("; end: ");
        if (this.mSystemUpdateFreezeEnd != null) {
            freezePeriodRecord.append(this.mSystemUpdateFreezeEnd.toString());
        } else {
            freezePeriodRecord.append("null");
        }
        return freezePeriodRecord.toString();
    }

    boolean setSystemUpdateFreezePeriodRecord(LocalDate start, LocalDate end) {
        boolean changed = false;
        synchronized (this.mLock) {
            if (!Objects.equals(this.mSystemUpdateFreezeStart, start)) {
                this.mSystemUpdateFreezeStart = start;
                changed = true;
            }
            if (!Objects.equals(this.mSystemUpdateFreezeEnd, end)) {
                this.mSystemUpdateFreezeEnd = end;
                changed = true;
            }
        }
        return changed;
    }

    boolean hasDeviceOwner() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mDeviceOwner != null;
        }
        return z;
    }

    boolean isDeviceOwnerUserId(int userId) {
        boolean z;
        synchronized (this.mLock) {
            z = this.mDeviceOwner != null && this.mDeviceOwnerUserId == userId;
        }
        return z;
    }

    boolean hasProfileOwner(int userId) {
        boolean z;
        synchronized (this.mLock) {
            z = getProfileOwnerComponent(userId) != null;
        }
        return z;
    }

    boolean getDeviceOwnerUserRestrictionsNeedsMigration() {
        boolean z;
        synchronized (this.mLock) {
            z = (this.mDeviceOwner == null || this.mDeviceOwner.userRestrictionsMigrated) ? false : true;
        }
        return z;
    }

    boolean getProfileOwnerUserRestrictionsNeedsMigration(int userId) {
        boolean z;
        synchronized (this.mLock) {
            OwnerInfo profileOwner = (OwnerInfo) this.mProfileOwners.get(Integer.valueOf(userId));
            z = (profileOwner == null || profileOwner.userRestrictionsMigrated) ? false : true;
        }
        return z;
    }

    void setDeviceOwnerUserRestrictionsMigrated() {
        synchronized (this.mLock) {
            if (this.mDeviceOwner != null) {
                this.mDeviceOwner.userRestrictionsMigrated = true;
            }
            writeDeviceOwner();
        }
    }

    void setDeviceOwnerRemoteBugreportUriAndHash(String remoteBugreportUri, String remoteBugreportHash) {
        synchronized (this.mLock) {
            if (this.mDeviceOwner != null) {
                this.mDeviceOwner.remoteBugreportUri = remoteBugreportUri;
                this.mDeviceOwner.remoteBugreportHash = remoteBugreportHash;
            }
            writeDeviceOwner();
        }
    }

    void setProfileOwnerUserRestrictionsMigrated(int userId) {
        synchronized (this.mLock) {
            OwnerInfo profileOwner = (OwnerInfo) this.mProfileOwners.get(Integer.valueOf(userId));
            if (profileOwner != null) {
                profileOwner.userRestrictionsMigrated = true;
            }
            writeProfileOwner(userId);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:39:0x0106 A:{Splitter: B:4:0x000b, ExcHandler: org.xmlpull.v1.XmlPullParserException (e org.xmlpull.v1.XmlPullParserException)} */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x0104 A:{Splitter: B:7:0x000f, ExcHandler: org.xmlpull.v1.XmlPullParserException (e org.xmlpull.v1.XmlPullParserException)} */
    /* JADX WARNING: Missing block: B:38:0x0104, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:39:0x0106, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:40:0x0107, code:
            r4 = r23;
     */
    /* JADX WARNING: Missing block: B:41:0x0109, code:
            android.util.Slog.e(TAG, "Error parsing device-owner file", r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean readLegacyOwnerFileLocked(File file) {
        if (!file.exists()) {
            return false;
        }
        try {
            try {
                InputStream input = new AtomicFile(file).openRead();
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(input, StandardCharsets.UTF_8.name());
                while (true) {
                    int next = parser.next();
                    int type = next;
                    if (next == 1) {
                        input.close();
                        break;
                    } else if (type == 2) {
                        String tag = parser.getName();
                        if (tag.equals(TAG_DEVICE_OWNER)) {
                            this.mDeviceOwner = new OwnerInfo(parser.getAttributeValue(null, "name"), parser.getAttributeValue(null, "package"), false, null, null);
                            this.mDeviceOwnerUserId = 0;
                        } else if (!tag.equals(TAG_DEVICE_INITIALIZER)) {
                            if (tag.equals(TAG_PROFILE_OWNER)) {
                                String profileOwnerPackageName = parser.getAttributeValue(null, "package");
                                String profileOwnerName = parser.getAttributeValue(null, "name");
                                String profileOwnerComponentStr = parser.getAttributeValue(null, ATTR_COMPONENT_NAME);
                                int userId = Integer.parseInt(parser.getAttributeValue(null, ATTR_USERID));
                                OwnerInfo profileOwnerInfo = null;
                                if (profileOwnerComponentStr != null) {
                                    ComponentName admin = ComponentName.unflattenFromString(profileOwnerComponentStr);
                                    if (admin != null) {
                                        profileOwnerInfo = new OwnerInfo(profileOwnerName, admin, false, null, null);
                                    } else {
                                        String str = TAG;
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("Error parsing device-owner file. Bad component name ");
                                        stringBuilder.append(profileOwnerComponentStr);
                                        Slog.e(str, stringBuilder.toString());
                                    }
                                }
                                OwnerInfo profileOwnerInfo2 = profileOwnerInfo;
                                if (profileOwnerInfo2 == null) {
                                    profileOwnerInfo2 = new OwnerInfo(profileOwnerName, profileOwnerPackageName, false, null, null);
                                }
                                this.mProfileOwners.put(Integer.valueOf(userId), profileOwnerInfo2);
                            } else if (TAG_SYSTEM_UPDATE_POLICY.equals(tag)) {
                                this.mSystemUpdatePolicy = SystemUpdatePolicy.restoreFromXml(parser);
                            } else {
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Unexpected tag in device owner file: ");
                                stringBuilder2.append(tag);
                                throw new XmlPullParserException(stringBuilder2.toString());
                            }
                        }
                    }
                }
            } catch (XmlPullParserException e) {
            }
        } catch (XmlPullParserException e2) {
        }
        return true;
    }

    void writeDeviceOwner() {
        synchronized (this.mLock) {
            new DeviceOwnerReadWriter().writeToFileLocked();
        }
    }

    void writeProfileOwner(int userId) {
        synchronized (this.mLock) {
            new ProfileOwnerReadWriter(userId).writeToFileLocked();
        }
    }

    boolean saveSystemUpdateInfo(SystemUpdateInfo newInfo) {
        synchronized (this.mLock) {
            if (Objects.equals(newInfo, this.mSystemUpdateInfo)) {
                return false;
            }
            this.mSystemUpdateInfo = newInfo;
            new DeviceOwnerReadWriter().writeToFileLocked();
            return true;
        }
    }

    public SystemUpdateInfo getSystemUpdateInfo() {
        SystemUpdateInfo systemUpdateInfo;
        synchronized (this.mLock) {
            systemUpdateInfo = this.mSystemUpdateInfo;
        }
        return systemUpdateInfo;
    }

    void pushToAppOpsLocked() {
        if (this.mSystemReady) {
            long ident = Binder.clearCallingIdentity();
            try {
                int uid;
                SparseIntArray owners = new SparseIntArray();
                if (this.mDeviceOwner != null) {
                    uid = this.mPackageManagerInternal.getPackageUid(this.mDeviceOwner.packageName, 4333568, this.mDeviceOwnerUserId);
                    if (uid >= 0) {
                        owners.put(this.mDeviceOwnerUserId, uid);
                    }
                }
                if (this.mProfileOwners != null) {
                    for (uid = this.mProfileOwners.size() - 1; uid >= 0; uid--) {
                        int uid2 = this.mPackageManagerInternal.getPackageUid(((OwnerInfo) this.mProfileOwners.valueAt(uid)).packageName, 4333568, ((Integer) this.mProfileOwners.keyAt(uid)).intValue());
                        if (uid2 >= 0) {
                            owners.put(((Integer) this.mProfileOwners.keyAt(uid)).intValue(), uid2);
                        }
                    }
                }
                AppOpsManagerInternal appops = (AppOpsManagerInternal) LocalServices.getService(AppOpsManagerInternal.class);
                if (appops != null) {
                    appops.setDeviceAndProfileOwners(owners.size() > 0 ? owners : null);
                }
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    public void systemReady() {
        synchronized (this.mLock) {
            this.mSystemReady = true;
            pushToAppOpsLocked();
        }
    }

    public void dump(String prefix, PrintWriter pw) {
        StringBuilder stringBuilder;
        boolean needBlank = false;
        if (this.mDeviceOwner != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("Device Owner: ");
            pw.println(stringBuilder.toString());
            OwnerInfo ownerInfo = this.mDeviceOwner;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(prefix);
            stringBuilder2.append("  ");
            ownerInfo.dump(stringBuilder2.toString(), pw);
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("  User ID: ");
            stringBuilder.append(this.mDeviceOwnerUserId);
            pw.println(stringBuilder.toString());
            needBlank = true;
        }
        if (this.mSystemUpdatePolicy != null) {
            if (needBlank) {
                pw.println();
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("System Update Policy: ");
            stringBuilder.append(this.mSystemUpdatePolicy);
            pw.println(stringBuilder.toString());
            needBlank = true;
        }
        if (this.mProfileOwners != null) {
            for (Entry<Integer, OwnerInfo> entry : this.mProfileOwners.entrySet()) {
                if (needBlank) {
                    pw.println();
                }
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(prefix);
                stringBuilder3.append("Profile Owner (User ");
                stringBuilder3.append(entry.getKey());
                stringBuilder3.append("): ");
                pw.println(stringBuilder3.toString());
                OwnerInfo ownerInfo2 = (OwnerInfo) entry.getValue();
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append(prefix);
                stringBuilder4.append("  ");
                ownerInfo2.dump(stringBuilder4.toString(), pw);
                needBlank = true;
            }
        }
        if (this.mSystemUpdateInfo != null) {
            if (needBlank) {
                pw.println();
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("Pending System Update: ");
            stringBuilder.append(this.mSystemUpdateInfo);
            pw.println(stringBuilder.toString());
            needBlank = true;
        }
        if (this.mSystemUpdateFreezeStart != null || this.mSystemUpdateFreezeEnd != null) {
            if (needBlank) {
                pw.println();
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("System update freeze record: ");
            stringBuilder.append(getSystemUpdateFreezePeriodRecordAsString());
            pw.println(stringBuilder.toString());
        }
    }

    @VisibleForTesting
    File getLegacyConfigFile() {
        return new File(this.mInjector.environmentGetDataSystemDirectory(), DEVICE_OWNER_XML_LEGACY);
    }

    @VisibleForTesting
    File getDeviceOwnerFile() {
        return new File(this.mInjector.environmentGetDataSystemDirectory(), DEVICE_OWNER_XML);
    }

    @VisibleForTesting
    File getProfileOwnerFile(int userId) {
        return new File(this.mInjector.environmentGetUserSystemDirectory(userId), PROFILE_OWNER_XML);
    }
}
