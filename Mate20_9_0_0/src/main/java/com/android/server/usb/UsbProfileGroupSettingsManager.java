package com.android.server.usb;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.XmlResourceParser;
import android.hardware.usb.AccessoryFilter;
import android.hardware.usb.DeviceFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.net.util.NetworkConstants;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.Immutable;
import com.android.internal.app.IntentForwarderActivity;
import com.android.internal.content.PackageMonitor;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.pm.PackageManagerService;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class UsbProfileGroupSettingsManager {
    protected static final String BACKUP_SUB_ID = "subId";
    protected static final String BACKUP_SUB_NAME = "subName";
    private static final boolean DEBUG = false;
    private static final String TAG = UsbProfileGroupSettingsManager.class.getSimpleName();
    private static final File sSingleUserSettingsFile = new File("/data/system/usb_device_manager.xml");
    @GuardedBy("mLock")
    private final HashMap<AccessoryFilter, UserPackage> mAccessoryPreferenceMap = new HashMap();
    private final Context mContext;
    @GuardedBy("mLock")
    private final HashMap<DeviceFilter, UserPackage> mDevicePreferenceMap = new HashMap();
    private final boolean mDisablePermissionDialogs;
    @GuardedBy("mLock")
    private boolean mIsWriteSettingsScheduled;
    private final Object mLock = new Object();
    private final MtpNotificationManager mMtpNotificationManager;
    private final PackageManager mPackageManager;
    MyPackageMonitor mPackageMonitor = new MyPackageMonitor();
    private final UserHandle mParentUser;
    private final AtomicFile mSettingsFile;
    private final UsbSettingsManager mSettingsManager;
    private final UserManager mUserManager;

    private class MyPackageMonitor extends PackageMonitor {
        private MyPackageMonitor() {
        }

        public void onPackageAdded(String packageName, int uid) {
            if (UsbProfileGroupSettingsManager.this.mUserManager.isSameProfileGroup(UsbProfileGroupSettingsManager.this.mParentUser.getIdentifier(), UserHandle.getUserId(uid))) {
                UsbProfileGroupSettingsManager.this.handlePackageAdded(new UserPackage(packageName, UserHandle.getUserHandleForUid(uid)));
            }
        }

        public void onPackageRemoved(String packageName, int uid) {
            if (UsbProfileGroupSettingsManager.this.mUserManager.isSameProfileGroup(UsbProfileGroupSettingsManager.this.mParentUser.getIdentifier(), UserHandle.getUserId(uid))) {
                UsbProfileGroupSettingsManager.this.clearDefaults(packageName, UserHandle.getUserHandleForUid(uid));
            }
        }
    }

    @Immutable
    private static class UserPackage {
        final String packageName;
        final UserHandle user;

        private UserPackage(String packageName, UserHandle user) {
            this.packageName = packageName;
            this.user = user;
        }

        public boolean equals(Object obj) {
            boolean z = false;
            if (!(obj instanceof UserPackage)) {
                return false;
            }
            UserPackage other = (UserPackage) obj;
            if (this.user.equals(other.user) && this.packageName.equals(other.packageName)) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return (31 * this.user.hashCode()) + this.packageName.hashCode();
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.user.getIdentifier());
            stringBuilder.append(SliceAuthority.DELIMITER);
            stringBuilder.append(this.packageName);
            return stringBuilder.toString();
        }

        public void dump(DualDumpOutputStream dump, String idName, long id) {
            long token = dump.start(idName, id);
            dump.write("user_id", 1120986464257L, this.user.getIdentifier());
            dump.write("package_name", 1138166333442L, this.packageName);
            dump.end(token);
        }
    }

    UsbProfileGroupSettingsManager(Context context, UserHandle user, UsbSettingsManager settingsManager) {
        try {
            Context parentUserContext = context.createPackageContextAsUser(PackageManagerService.PLATFORM_PACKAGE_NAME, 0, user);
            this.mContext = context;
            this.mPackageManager = context.getPackageManager();
            this.mSettingsManager = settingsManager;
            this.mUserManager = (UserManager) context.getSystemService("user");
            this.mParentUser = user;
            this.mSettingsFile = new AtomicFile(new File(Environment.getUserSystemDirectory(user.getIdentifier()), "usb_device_manager.xml"), "usb-state");
            this.mDisablePermissionDialogs = context.getResources().getBoolean(17956931);
            synchronized (this.mLock) {
                if (UserHandle.SYSTEM.equals(user)) {
                    upgradeSingleUserLocked();
                }
                readSettingsLocked();
            }
            this.mPackageMonitor.register(context, null, UserHandle.ALL, true);
            this.mMtpNotificationManager = new MtpNotificationManager(parentUserContext, new -$$Lambda$UsbProfileGroupSettingsManager$IQKTzU0q3lyaW9nLL_sbxJPW8ME(this));
        } catch (NameNotFoundException e) {
            throw new RuntimeException("Missing android package");
        }
    }

    void removeAllDefaultsForUser(UserHandle userToRemove) {
        synchronized (this.mLock) {
            boolean needToPersist = false;
            Iterator<Entry<DeviceFilter, UserPackage>> devicePreferenceIt = this.mDevicePreferenceMap.entrySet().iterator();
            while (devicePreferenceIt.hasNext()) {
                if (((UserPackage) ((Entry) devicePreferenceIt.next()).getValue()).user.equals(userToRemove)) {
                    devicePreferenceIt.remove();
                    needToPersist = true;
                }
            }
            Iterator<Entry<AccessoryFilter, UserPackage>> accessoryPreferenceIt = this.mAccessoryPreferenceMap.entrySet().iterator();
            while (accessoryPreferenceIt.hasNext()) {
                if (((UserPackage) ((Entry) accessoryPreferenceIt.next()).getValue()).user.equals(userToRemove)) {
                    accessoryPreferenceIt.remove();
                    needToPersist = true;
                }
            }
            if (needToPersist) {
                scheduleWriteSettingsLocked();
            }
        }
    }

    private void readPreference(XmlPullParser parser) throws XmlPullParserException, IOException {
        String packageName = null;
        UserHandle user = this.mParentUser;
        int count = parser.getAttributeCount();
        for (int i = 0; i < count; i++) {
            if ("package".equals(parser.getAttributeName(i))) {
                packageName = parser.getAttributeValue(i);
            }
            if ("user".equals(parser.getAttributeName(i))) {
                user = this.mUserManager.getUserForSerialNumber((long) Integer.parseInt(parser.getAttributeValue(i)));
            }
        }
        XmlUtils.nextElement(parser);
        if ("usb-device".equals(parser.getName())) {
            DeviceFilter filter = DeviceFilter.read(parser);
            if (user != null) {
                this.mDevicePreferenceMap.put(filter, new UserPackage(packageName, user));
            }
        } else if ("usb-accessory".equals(parser.getName())) {
            AccessoryFilter filter2 = AccessoryFilter.read(parser);
            if (user != null) {
                this.mAccessoryPreferenceMap.put(filter2, new UserPackage(packageName, user));
            }
        }
        XmlUtils.nextElement(parser);
    }

    @GuardedBy("mLock")
    private void upgradeSingleUserLocked() {
        if (sSingleUserSettingsFile.exists()) {
            this.mDevicePreferenceMap.clear();
            this.mAccessoryPreferenceMap.clear();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(sSingleUserSettingsFile);
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(fis, StandardCharsets.UTF_8.name());
                XmlUtils.nextElement(parser);
                while (parser.getEventType() != 1) {
                    if ("preference".equals(parser.getName())) {
                        readPreference(parser);
                    } else {
                        XmlUtils.nextElement(parser);
                    }
                }
            } catch (IOException | XmlPullParserException e) {
                Log.wtf(TAG, "Failed to read single-user settings", e);
            } catch (Throwable th) {
                IoUtils.closeQuietly(null);
            }
            IoUtils.closeQuietly(fis);
            scheduleWriteSettingsLocked();
            sSingleUserSettingsFile.delete();
        }
    }

    @GuardedBy("mLock")
    private void readSettingsLocked() {
        this.mDevicePreferenceMap.clear();
        this.mAccessoryPreferenceMap.clear();
        FileInputStream stream = null;
        try {
            stream = this.mSettingsFile.openRead();
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream, StandardCharsets.UTF_8.name());
            XmlUtils.nextElement(parser);
            while (parser.getEventType() != 1) {
                if ("preference".equals(parser.getName())) {
                    readPreference(parser);
                } else {
                    XmlUtils.nextElement(parser);
                }
            }
        } catch (FileNotFoundException e) {
        } catch (Exception e2) {
            Slog.e(TAG, "error reading settings file, deleting to start fresh", e2);
            this.mSettingsFile.delete();
        } catch (Throwable th) {
            IoUtils.closeQuietly(null);
        }
        IoUtils.closeQuietly(stream);
    }

    @GuardedBy("mLock")
    private void scheduleWriteSettingsLocked() {
        if (!this.mIsWriteSettingsScheduled) {
            this.mIsWriteSettingsScheduled = true;
            AsyncTask.execute(new -$$Lambda$UsbProfileGroupSettingsManager$_G1PjxMa22pAIRMzYCwyomX8uhk(this));
        }
    }

    public static /* synthetic */ void lambda$scheduleWriteSettingsLocked$1(UsbProfileGroupSettingsManager usbProfileGroupSettingsManager) {
        synchronized (usbProfileGroupSettingsManager.mLock) {
            FileOutputStream fos = null;
            try {
                fos = usbProfileGroupSettingsManager.mSettingsFile.startWrite();
                FastXmlSerializer serializer = new FastXmlSerializer();
                serializer.setOutput(fos, StandardCharsets.UTF_8.name());
                serializer.startDocument(null, Boolean.valueOf(true));
                serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                serializer.startTag(null, "settings");
                for (DeviceFilter filter : usbProfileGroupSettingsManager.mDevicePreferenceMap.keySet()) {
                    serializer.startTag(null, "preference");
                    serializer.attribute(null, "package", ((UserPackage) usbProfileGroupSettingsManager.mDevicePreferenceMap.get(filter)).packageName);
                    serializer.attribute(null, "user", String.valueOf(usbProfileGroupSettingsManager.getSerial(((UserPackage) usbProfileGroupSettingsManager.mDevicePreferenceMap.get(filter)).user)));
                    filter.write(serializer);
                    serializer.endTag(null, "preference");
                }
                for (AccessoryFilter filter2 : usbProfileGroupSettingsManager.mAccessoryPreferenceMap.keySet()) {
                    serializer.startTag(null, "preference");
                    serializer.attribute(null, "package", ((UserPackage) usbProfileGroupSettingsManager.mAccessoryPreferenceMap.get(filter2)).packageName);
                    serializer.attribute(null, "user", String.valueOf(usbProfileGroupSettingsManager.getSerial(((UserPackage) usbProfileGroupSettingsManager.mAccessoryPreferenceMap.get(filter2)).user)));
                    filter2.write(serializer);
                    serializer.endTag(null, "preference");
                }
                serializer.endTag(null, "settings");
                serializer.endDocument();
                usbProfileGroupSettingsManager.mSettingsFile.finishWrite(fos);
            } catch (IOException e) {
                Slog.e(TAG, "Failed to write settings", e);
                if (fos != null) {
                    usbProfileGroupSettingsManager.mSettingsFile.failWrite(fos);
                }
            }
            usbProfileGroupSettingsManager.mIsWriteSettingsScheduled = false;
        }
    }

    /* JADX WARNING: Missing block: B:36:0x007d, code skipped:
            if (r2 != null) goto L_0x007f;
     */
    /* JADX WARNING: Missing block: B:37:0x007f, code skipped:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:42:0x00a0, code skipped:
            if (r2 == null) goto L_0x00a3;
     */
    /* JADX WARNING: Missing block: B:43:0x00a3, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean packageMatchesLocked(ResolveInfo info, String metaDataName, UsbDevice device, UsbAccessory accessory) {
        if (isForwardMatch(info)) {
            return true;
        }
        XmlResourceParser parser = null;
        String tagName;
        try {
            parser = info.activityInfo.loadXmlMetaData(this.mPackageManager, metaDataName);
            if (parser == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("no meta-data for ");
                stringBuilder.append(info);
                Slog.w(str, stringBuilder.toString());
                if (parser != null) {
                    parser.close();
                }
                return false;
            }
            XmlUtils.nextElement(parser);
            while (parser.getEventType() != 1) {
                tagName = parser.getName();
                if (device == null || !"usb-device".equals(tagName)) {
                    if (accessory != null) {
                        if ("usb-accessory".equals(tagName) && AccessoryFilter.read(parser).matches(accessory)) {
                            if (parser != null) {
                                parser.close();
                            }
                            return true;
                        }
                    }
                } else if (DeviceFilter.read(parser).matches(device)) {
                    if (parser != null) {
                        parser.close();
                    }
                    return true;
                }
                XmlUtils.nextElement(parser);
            }
        } catch (Exception e) {
            tagName = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unable to load component info ");
            stringBuilder2.append(info.toString());
            Slog.w(tagName, stringBuilder2.toString(), e);
        } catch (Throwable th) {
            if (parser != null) {
                parser.close();
            }
        }
    }

    private ArrayList<ResolveInfo> queryIntentActivitiesForAllProfiles(Intent intent) {
        List<UserInfo> profiles = this.mUserManager.getEnabledProfiles(this.mParentUser.getIdentifier());
        ArrayList<ResolveInfo> resolveInfos = new ArrayList();
        int numProfiles = profiles.size();
        for (int i = 0; i < numProfiles; i++) {
            resolveInfos.addAll(this.mPackageManager.queryIntentActivitiesAsUser(intent, 128, ((UserInfo) profiles.get(i)).id));
        }
        return resolveInfos;
    }

    private boolean isForwardMatch(ResolveInfo match) {
        return match.getComponentInfo().name.equals(IntentForwarderActivity.FORWARD_INTENT_TO_MANAGED_PROFILE);
    }

    private ArrayList<ResolveInfo> preferHighPriority(ArrayList<ResolveInfo> matches) {
        SparseArray<ArrayList<ResolveInfo>> highestPriorityMatchesByUserId = new SparseArray();
        SparseIntArray highestPriorityByUserId = new SparseIntArray();
        ArrayList<ResolveInfo> forwardMatches = new ArrayList();
        int numMatches = matches.size();
        int matchArrayNum = 0;
        for (int matchNum = 0; matchNum < numMatches; matchNum++) {
            ResolveInfo match = (ResolveInfo) matches.get(matchNum);
            if (isForwardMatch(match)) {
                forwardMatches.add(match);
            } else {
                if (highestPriorityByUserId.indexOfKey(match.targetUserId) < 0) {
                    highestPriorityByUserId.put(match.targetUserId, Integer.MIN_VALUE);
                    highestPriorityMatchesByUserId.put(match.targetUserId, new ArrayList());
                }
                int highestPriority = highestPriorityByUserId.get(match.targetUserId);
                ArrayList<ResolveInfo> highestPriorityMatches = (ArrayList) highestPriorityMatchesByUserId.get(match.targetUserId);
                if (match.priority == highestPriority) {
                    highestPriorityMatches.add(match);
                } else if (match.priority > highestPriority) {
                    highestPriorityByUserId.put(match.targetUserId, match.priority);
                    highestPriorityMatches.clear();
                    highestPriorityMatches.add(match);
                }
            }
        }
        ArrayList<ResolveInfo> combinedMatches = new ArrayList(forwardMatches);
        int numMatchArrays = highestPriorityMatchesByUserId.size();
        while (matchArrayNum < numMatchArrays) {
            combinedMatches.addAll((Collection) highestPriorityMatchesByUserId.valueAt(matchArrayNum));
            matchArrayNum++;
        }
        return combinedMatches;
    }

    private ArrayList<ResolveInfo> removeForwardIntentIfNotNeeded(ArrayList<ResolveInfo> rawMatches) {
        ResolveInfo rawMatch;
        int numRawMatches = rawMatches.size();
        int i = 0;
        int numNonParentActivityMatches = 0;
        int numParentActivityMatches = 0;
        for (int i2 = 0; i2 < numRawMatches; i2++) {
            rawMatch = (ResolveInfo) rawMatches.get(i2);
            if (!isForwardMatch(rawMatch)) {
                if (UserHandle.getUserHandleForUid(rawMatch.activityInfo.applicationInfo.uid).equals(this.mParentUser)) {
                    numParentActivityMatches++;
                } else {
                    numNonParentActivityMatches++;
                }
            }
        }
        if (numParentActivityMatches != 0 && numNonParentActivityMatches != 0) {
            return rawMatches;
        }
        ArrayList<ResolveInfo> matches = new ArrayList(numParentActivityMatches + numNonParentActivityMatches);
        while (i < numRawMatches) {
            rawMatch = (ResolveInfo) rawMatches.get(i);
            if (!isForwardMatch(rawMatch)) {
                matches.add(rawMatch);
            }
            i++;
        }
        return matches;
    }

    private ArrayList<ResolveInfo> getDeviceMatchesLocked(UsbDevice device, Intent intent) {
        ArrayList<ResolveInfo> matches = new ArrayList();
        List<ResolveInfo> resolveInfos = queryIntentActivitiesForAllProfiles(intent);
        int count = resolveInfos.size();
        for (int i = 0; i < count; i++) {
            ResolveInfo resolveInfo = (ResolveInfo) resolveInfos.get(i);
            if (packageMatchesLocked(resolveInfo, intent.getAction(), device, null)) {
                matches.add(resolveInfo);
            }
        }
        return removeForwardIntentIfNotNeeded(preferHighPriority(matches));
    }

    private ArrayList<ResolveInfo> getAccessoryMatchesLocked(UsbAccessory accessory, Intent intent) {
        ArrayList<ResolveInfo> matches = new ArrayList();
        List<ResolveInfo> resolveInfos = queryIntentActivitiesForAllProfiles(intent);
        int count = resolveInfos.size();
        for (int i = 0; i < count; i++) {
            ResolveInfo resolveInfo = (ResolveInfo) resolveInfos.get(i);
            if (packageMatchesLocked(resolveInfo, intent.getAction(), null, accessory)) {
                matches.add(resolveInfo);
            }
        }
        return removeForwardIntentIfNotNeeded(preferHighPriority(matches));
    }

    public void deviceAttached(UsbDevice device) {
        Intent intent = createDeviceAttachedIntent(device);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        resolveActivity(intent, device, true);
    }

    private void resolveActivity(Intent intent, UsbDevice device, boolean showMtpNotification) {
        ArrayList<ResolveInfo> matches;
        ActivityInfo defaultActivity;
        synchronized (this.mLock) {
            matches = getDeviceMatchesLocked(device, intent);
            defaultActivity = getDefaultActivityLocked(matches, (UserPackage) this.mDevicePreferenceMap.get(new DeviceFilter(device)));
        }
        if (showMtpNotification && MtpNotificationManager.shouldShowNotification(this.mPackageManager, device) && defaultActivity == null) {
            this.mMtpNotificationManager.showNotification(device);
        } else {
            resolveActivity(intent, matches, defaultActivity, device, null);
        }
    }

    public void deviceAttachedForFixedHandler(UsbDevice device, ComponentName component) {
        Intent intent = createDeviceAttachedIntent(device);
        this.mContext.sendBroadcast(intent);
        try {
            ApplicationInfo appInfo = this.mPackageManager.getApplicationInfoAsUser(component.getPackageName(), 0, this.mParentUser.getIdentifier());
            this.mSettingsManager.getSettingsForUser(UserHandle.getUserId(appInfo.uid)).grantDevicePermission(device, appInfo.uid);
            Intent activityIntent = new Intent(intent);
            activityIntent.setComponent(component);
            try {
                this.mContext.startActivityAsUser(activityIntent, this.mParentUser);
            } catch (ActivityNotFoundException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unable to start activity ");
                stringBuilder.append(activityIntent);
                Slog.e(str, stringBuilder.toString());
            }
        } catch (NameNotFoundException e2) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Default USB handling package (");
            stringBuilder2.append(component.getPackageName());
            stringBuilder2.append(") not found  for user ");
            stringBuilder2.append(this.mParentUser);
            Slog.e(str2, stringBuilder2.toString());
        }
    }

    void usbDeviceRemoved(UsbDevice device) {
        this.mMtpNotificationManager.hideNotification(device.getDeviceId());
    }

    public void accessoryAttached(UsbAccessory accessory) {
        ArrayList<ResolveInfo> matches;
        ActivityInfo defaultActivity;
        Intent intent = new Intent("android.hardware.usb.action.USB_ACCESSORY_ATTACHED");
        intent.putExtra("accessory", accessory);
        intent.addFlags(285212672);
        synchronized (this.mLock) {
            matches = getAccessoryMatchesLocked(accessory, intent);
            defaultActivity = getDefaultActivityLocked(matches, (UserPackage) this.mAccessoryPreferenceMap.get(new AccessoryFilter(accessory)));
        }
        resolveActivity(intent, matches, defaultActivity, null, accessory);
    }

    private void resolveActivity(Intent intent, ArrayList<ResolveInfo> matches, ActivityInfo defaultActivity, UsbDevice device, UsbAccessory accessory) {
        if (matches.size() == 0) {
            if (accessory != null) {
                String uri = accessory.getUri();
                if (uri != null && uri.length() > 0) {
                    Intent dialogIntent = new Intent();
                    dialogIntent.setClassName("com.android.systemui", "com.android.systemui.usb.UsbAccessoryUriActivity");
                    dialogIntent.addFlags(268435456);
                    dialogIntent.putExtra("accessory", accessory);
                    dialogIntent.putExtra("uri", uri);
                    try {
                        this.mContext.startActivityAsUser(dialogIntent, this.mParentUser);
                    } catch (ActivityNotFoundException e) {
                        Slog.e(TAG, "unable to start UsbAccessoryUriActivity");
                    }
                }
            }
            return;
        }
        if (defaultActivity != null) {
            UsbUserSettingsManager defaultRIUserSettings = this.mSettingsManager.getSettingsForUser(UserHandle.getUserId(defaultActivity.applicationInfo.uid));
            if (device != null) {
                defaultRIUserSettings.grantDevicePermission(device, defaultActivity.applicationInfo.uid);
            } else if (accessory != null) {
                defaultRIUserSettings.grantAccessoryPermission(accessory, defaultActivity.applicationInfo.uid);
            }
            try {
                intent.setComponent(new ComponentName(defaultActivity.packageName, defaultActivity.name));
                this.mContext.startActivityAsUser(intent, UserHandle.getUserHandleForUid(defaultActivity.applicationInfo.uid));
            } catch (ActivityNotFoundException e2) {
                Slog.e(TAG, "startActivity failed", e2);
            }
        } else {
            UserHandle user;
            Intent resolverIntent = new Intent();
            resolverIntent.addFlags(268435456);
            if (matches.size() == 1) {
                ResolveInfo rInfo = (ResolveInfo) matches.get(0);
                resolverIntent.setClassName("com.android.systemui", "com.android.systemui.usb.UsbConfirmActivity");
                resolverIntent.putExtra("rinfo", rInfo);
                user = UserHandle.getUserHandleForUid(rInfo.activityInfo.applicationInfo.uid);
                if (device != null) {
                    resolverIntent.putExtra("device", device);
                } else {
                    resolverIntent.putExtra("accessory", accessory);
                }
            } else {
                user = this.mParentUser;
                resolverIntent.setClassName("com.android.systemui", "com.android.systemui.usb.UsbResolverActivity");
                resolverIntent.putParcelableArrayListExtra("rlist", matches);
                resolverIntent.putExtra("android.intent.extra.INTENT", intent);
            }
            try {
                this.mContext.startActivityAsUser(resolverIntent, user);
            } catch (ActivityNotFoundException e22) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unable to start activity ");
                stringBuilder.append(resolverIntent);
                Slog.e(str, stringBuilder.toString(), e22);
            }
        }
    }

    private ActivityInfo getDefaultActivityLocked(ArrayList<ResolveInfo> matches, UserPackage userPackage) {
        if (userPackage != null) {
            Iterator it = matches.iterator();
            while (it.hasNext()) {
                ResolveInfo info = (ResolveInfo) it.next();
                if (info.activityInfo != null && userPackage.equals(new UserPackage(info.activityInfo.packageName, UserHandle.getUserHandleForUid(info.activityInfo.applicationInfo.uid)))) {
                    return info.activityInfo;
                }
            }
        }
        if (matches.size() == 1) {
            ActivityInfo activityInfo = ((ResolveInfo) matches.get(0)).activityInfo;
            if (activityInfo != null) {
                if (this.mDisablePermissionDialogs) {
                    return activityInfo;
                }
                if (activityInfo.applicationInfo == null || (1 & activityInfo.applicationInfo.flags) == 0) {
                    return null;
                }
                return activityInfo;
            }
        }
        return null;
    }

    @GuardedBy("mLock")
    private boolean clearCompatibleMatchesLocked(UserPackage userPackage, DeviceFilter filter) {
        Iterator it;
        ArrayList<DeviceFilter> keysToRemove = new ArrayList();
        for (DeviceFilter device : this.mDevicePreferenceMap.keySet()) {
            if (filter.contains(device) && !((UserPackage) this.mDevicePreferenceMap.get(device)).equals(userPackage)) {
                keysToRemove.add(device);
            }
        }
        if (!keysToRemove.isEmpty()) {
            it = keysToRemove.iterator();
            while (it.hasNext()) {
                this.mDevicePreferenceMap.remove((DeviceFilter) it.next());
            }
        }
        return keysToRemove.isEmpty() ^ 1;
    }

    @GuardedBy("mLock")
    private boolean clearCompatibleMatchesLocked(UserPackage userPackage, AccessoryFilter filter) {
        Iterator it;
        ArrayList<AccessoryFilter> keysToRemove = new ArrayList();
        for (AccessoryFilter accessory : this.mAccessoryPreferenceMap.keySet()) {
            if (filter.contains(accessory) && !((UserPackage) this.mAccessoryPreferenceMap.get(accessory)).equals(userPackage)) {
                keysToRemove.add(accessory);
            }
        }
        if (!keysToRemove.isEmpty()) {
            it = keysToRemove.iterator();
            while (it.hasNext()) {
                this.mAccessoryPreferenceMap.remove((AccessoryFilter) it.next());
            }
        }
        return keysToRemove.isEmpty() ^ 1;
    }

    /* JADX WARNING: Missing block: B:25:0x004d, code skipped:
            if (r0 != null) goto L_0x004f;
     */
    /* JADX WARNING: Missing block: B:26:0x004f, code skipped:
            r0.close();
     */
    /* JADX WARNING: Missing block: B:31:0x0070, code skipped:
            if (r0 == null) goto L_0x0073;
     */
    /* JADX WARNING: Missing block: B:32:0x0073, code skipped:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @GuardedBy("mLock")
    private boolean handlePackageAddedLocked(UserPackage userPackage, ActivityInfo aInfo, String metaDataName) {
        XmlResourceParser parser = null;
        boolean changed = false;
        try {
            parser = aInfo.loadXmlMetaData(this.mPackageManager, metaDataName);
            if (parser == null) {
                if (parser != null) {
                    parser.close();
                }
                return false;
            }
            XmlUtils.nextElement(parser);
            while (parser.getEventType() != 1) {
                String tagName = parser.getName();
                if ("usb-device".equals(tagName)) {
                    if (clearCompatibleMatchesLocked(userPackage, DeviceFilter.read(parser))) {
                        changed = true;
                    }
                } else if ("usb-accessory".equals(tagName) && clearCompatibleMatchesLocked(userPackage, AccessoryFilter.read(parser))) {
                    changed = true;
                }
                XmlUtils.nextElement(parser);
            }
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to load component info ");
            stringBuilder.append(aInfo.toString());
            Slog.w(str, stringBuilder.toString(), e);
        } catch (Throwable th) {
            if (parser != null) {
                parser.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:23:0x0040, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handlePackageAdded(UserPackage userPackage) {
        synchronized (this.mLock) {
            int i = 0;
            boolean changed = false;
            try {
                ActivityInfo[] activities = this.mPackageManager.getPackageInfoAsUser(userPackage.packageName, NetworkConstants.ICMPV6_ECHO_REPLY_TYPE, userPackage.user.getIdentifier()).activities;
                if (activities == null) {
                    return;
                }
                while (i < activities.length) {
                    if (handlePackageAddedLocked(userPackage, activities[i], "android.hardware.usb.action.USB_DEVICE_ATTACHED")) {
                        changed = true;
                    }
                    if (handlePackageAddedLocked(userPackage, activities[i], "android.hardware.usb.action.USB_ACCESSORY_ATTACHED")) {
                        changed = true;
                    }
                    i++;
                }
                if (changed) {
                    scheduleWriteSettingsLocked();
                }
            } catch (NameNotFoundException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handlePackageUpdate could not find package ");
                stringBuilder.append(userPackage);
                Slog.e(str, stringBuilder.toString(), e);
            } catch (Throwable th) {
            }
        }
    }

    private int getSerial(UserHandle user) {
        return this.mUserManager.getUserSerialNumber(user.getIdentifier());
    }

    void setDevicePackage(UsbDevice device, String packageName, UserHandle user) {
        DeviceFilter filter = new DeviceFilter(device);
        synchronized (this.mLock) {
            boolean changed = true;
            if (packageName == null) {
                try {
                    if (this.mDevicePreferenceMap.remove(filter) == null) {
                        changed = false;
                    }
                } finally {
                }
            } else {
                UserPackage userPackage = new UserPackage(packageName, user);
                changed = true ^ userPackage.equals(this.mDevicePreferenceMap.get(filter));
                if (changed) {
                    this.mDevicePreferenceMap.put(filter, userPackage);
                }
            }
            if (changed) {
                scheduleWriteSettingsLocked();
            }
        }
    }

    void setAccessoryPackage(UsbAccessory accessory, String packageName, UserHandle user) {
        AccessoryFilter filter = new AccessoryFilter(accessory);
        synchronized (this.mLock) {
            boolean changed = true;
            if (packageName == null) {
                try {
                    if (this.mAccessoryPreferenceMap.remove(filter) == null) {
                        changed = false;
                    }
                } finally {
                }
            } else {
                UserPackage userPackage = new UserPackage(packageName, user);
                changed = true ^ userPackage.equals(this.mAccessoryPreferenceMap.get(filter));
                if (changed) {
                    this.mAccessoryPreferenceMap.put(filter, userPackage);
                }
            }
            if (changed) {
                scheduleWriteSettingsLocked();
            }
        }
    }

    boolean hasDefaults(String packageName, UserHandle user) {
        UserPackage userPackage = new UserPackage(packageName, user);
        synchronized (this.mLock) {
            if (this.mDevicePreferenceMap.values().contains(userPackage)) {
                return true;
            }
            boolean contains = this.mAccessoryPreferenceMap.values().contains(userPackage);
            return contains;
        }
    }

    void clearDefaults(String packageName, UserHandle user) {
        UserPackage userPackage = new UserPackage(packageName, user);
        synchronized (this.mLock) {
            if (clearPackageDefaultsLocked(userPackage)) {
                scheduleWriteSettingsLocked();
            }
        }
    }

    /* JADX WARNING: Missing block: B:29:0x006f, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean clearPackageDefaultsLocked(UserPackage userPackage) {
        Throwable th;
        boolean cleared = false;
        synchronized (this.mLock) {
            try {
                int i = 0;
                if (this.mDevicePreferenceMap.containsValue(userPackage)) {
                    DeviceFilter[] keys = (DeviceFilter[]) this.mDevicePreferenceMap.keySet().toArray(new DeviceFilter[0]);
                    boolean cleared2 = false;
                    int i2 = 0;
                    while (i2 < keys.length) {
                        try {
                            DeviceFilter key = keys[i2];
                            if (userPackage.equals(this.mDevicePreferenceMap.get(key))) {
                                this.mDevicePreferenceMap.remove(key);
                                cleared2 = true;
                            }
                            i2++;
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                    }
                    cleared = cleared2;
                }
                if (this.mAccessoryPreferenceMap.containsValue(userPackage)) {
                    AccessoryFilter[] keys2 = (AccessoryFilter[]) this.mAccessoryPreferenceMap.keySet().toArray(new AccessoryFilter[0]);
                    while (i < keys2.length) {
                        AccessoryFilter key2 = keys2[i];
                        if (userPackage.equals(this.mAccessoryPreferenceMap.get(key2))) {
                            this.mAccessoryPreferenceMap.remove(key2);
                            cleared = true;
                        }
                        i++;
                    }
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    public void dump(DualDumpOutputStream dump, String idName, long id) {
        DualDumpOutputStream dualDumpOutputStream = dump;
        long token = dump.start(idName, id);
        synchronized (this.mLock) {
            long devicePrefToken;
            dualDumpOutputStream.write("parent_user_id", 1120986464257L, this.mParentUser.getIdentifier());
            for (DeviceFilter filter : this.mDevicePreferenceMap.keySet()) {
                devicePrefToken = dualDumpOutputStream.start("device_preferences", 2246267895810L);
                filter.dump(dualDumpOutputStream, "filter", 1146756268033L);
                ((UserPackage) this.mDevicePreferenceMap.get(filter)).dump(dualDumpOutputStream, "user_package", 1146756268034L);
                dualDumpOutputStream.end(devicePrefToken);
            }
            for (AccessoryFilter filter2 : this.mAccessoryPreferenceMap.keySet()) {
                devicePrefToken = dualDumpOutputStream.start("accessory_preferences", 2246267895811L);
                filter2.dump(dualDumpOutputStream, "filter", 1146756268033L);
                ((UserPackage) this.mAccessoryPreferenceMap.get(filter2)).dump(dualDumpOutputStream, "user_package", 1146756268034L);
                dualDumpOutputStream.end(devicePrefToken);
            }
        }
        dualDumpOutputStream.end(token);
    }

    private static Intent createDeviceAttachedIntent(UsbDevice device) {
        Intent intent = new Intent("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        intent.putExtra("device", device);
        intent.putExtra(BACKUP_SUB_ID, device.getBackupSubProductId());
        intent.putExtra(BACKUP_SUB_NAME, device.getBackupSubDeviceName());
        intent.addFlags(285212672);
        return intent;
    }
}
