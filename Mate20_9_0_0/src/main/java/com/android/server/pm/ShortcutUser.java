package com.android.server.pm;

import android.content.ComponentName;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class ShortcutUser {
    private static final String ATTR_KNOWN_LOCALES = "locales";
    private static final String ATTR_LAST_APP_SCAN_OS_FINGERPRINT = "last-app-scan-fp";
    private static final String ATTR_LAST_APP_SCAN_TIME = "last-app-scan-time2";
    private static final String ATTR_RESTORE_SOURCE_FINGERPRINT = "restore-from-fp";
    private static final String ATTR_VALUE = "value";
    private static final String KEY_LAUNCHERS = "launchers";
    private static final String KEY_PACKAGES = "packages";
    private static final String KEY_USER_ID = "userId";
    private static final String TAG = "ShortcutService";
    private static final String TAG_LAUNCHER = "launcher";
    static final String TAG_ROOT = "user";
    private ComponentName mCachedLauncher;
    private String mKnownLocales;
    private String mLastAppScanOsFingerprint;
    private long mLastAppScanTime;
    private ComponentName mLastKnownLauncher;
    private final ArrayMap<PackageWithUser, ShortcutLauncher> mLaunchers = new ArrayMap();
    private final ArrayMap<String, ShortcutPackage> mPackages = new ArrayMap();
    private String mRestoreFromOsFingerprint;
    final ShortcutService mService;
    private final int mUserId;

    static final class PackageWithUser {
        final String packageName;
        final int userId;

        private PackageWithUser(int userId, String packageName) {
            this.userId = userId;
            this.packageName = (String) Preconditions.checkNotNull(packageName);
        }

        public static PackageWithUser of(int userId, String packageName) {
            return new PackageWithUser(userId, packageName);
        }

        public static PackageWithUser of(ShortcutPackageItem spi) {
            return new PackageWithUser(spi.getPackageUserId(), spi.getPackageName());
        }

        public int hashCode() {
            return this.packageName.hashCode() ^ this.userId;
        }

        public boolean equals(Object obj) {
            boolean z = false;
            if (!(obj instanceof PackageWithUser)) {
                return false;
            }
            PackageWithUser that = (PackageWithUser) obj;
            if (this.userId == that.userId && this.packageName.equals(that.packageName)) {
                z = true;
            }
            return z;
        }

        public String toString() {
            return String.format("[Package: %d, %s]", new Object[]{Integer.valueOf(this.userId), this.packageName});
        }
    }

    public ShortcutUser(ShortcutService service, int userId) {
        this.mService = service;
        this.mUserId = userId;
    }

    public int getUserId() {
        return this.mUserId;
    }

    public long getLastAppScanTime() {
        return this.mLastAppScanTime;
    }

    public void setLastAppScanTime(long lastAppScanTime) {
        this.mLastAppScanTime = lastAppScanTime;
    }

    public String getLastAppScanOsFingerprint() {
        return this.mLastAppScanOsFingerprint;
    }

    public void setLastAppScanOsFingerprint(String lastAppScanOsFingerprint) {
        this.mLastAppScanOsFingerprint = lastAppScanOsFingerprint;
    }

    @VisibleForTesting
    ArrayMap<String, ShortcutPackage> getAllPackagesForTest() {
        return this.mPackages;
    }

    public boolean hasPackage(String packageName) {
        return this.mPackages.containsKey(packageName);
    }

    private void addPackage(ShortcutPackage p) {
        p.replaceUser(this);
        this.mPackages.put(p.getPackageName(), p);
    }

    public ShortcutPackage removePackage(String packageName) {
        ShortcutPackage removed = (ShortcutPackage) this.mPackages.remove(packageName);
        this.mService.cleanupBitmapsForPackage(this.mUserId, packageName);
        return removed;
    }

    @VisibleForTesting
    ArrayMap<PackageWithUser, ShortcutLauncher> getAllLaunchersForTest() {
        return this.mLaunchers;
    }

    private void addLauncher(ShortcutLauncher launcher) {
        launcher.replaceUser(this);
        this.mLaunchers.put(PackageWithUser.of(launcher.getPackageUserId(), launcher.getPackageName()), launcher);
    }

    public ShortcutLauncher removeLauncher(int packageUserId, String packageName) {
        return (ShortcutLauncher) this.mLaunchers.remove(PackageWithUser.of(packageUserId, packageName));
    }

    public ShortcutPackage getPackageShortcutsIfExists(String packageName) {
        ShortcutPackage ret = (ShortcutPackage) this.mPackages.get(packageName);
        if (ret != null) {
            ret.attemptToRestoreIfNeededAndSave();
        }
        return ret;
    }

    public ShortcutPackage getPackageShortcuts(String packageName) {
        ShortcutPackage ret = getPackageShortcutsIfExists(packageName);
        if (ret != null) {
            return ret;
        }
        ret = new ShortcutPackage(this, this.mUserId, packageName);
        this.mPackages.put(packageName, ret);
        return ret;
    }

    public ShortcutLauncher getLauncherShortcuts(String packageName, int launcherUserId) {
        PackageWithUser key = PackageWithUser.of(launcherUserId, packageName);
        ShortcutLauncher ret = (ShortcutLauncher) this.mLaunchers.get(key);
        if (ret == null) {
            ret = new ShortcutLauncher(this, this.mUserId, packageName, launcherUserId);
            this.mLaunchers.put(key, ret);
            return ret;
        }
        ret.attemptToRestoreIfNeededAndSave();
        return ret;
    }

    public void forAllPackages(Consumer<? super ShortcutPackage> callback) {
        int size = this.mPackages.size();
        for (int i = 0; i < size; i++) {
            callback.accept(this.mPackages.valueAt(i));
        }
    }

    public void forAllLaunchers(Consumer<? super ShortcutLauncher> callback) {
        int size = this.mLaunchers.size();
        for (int i = 0; i < size; i++) {
            callback.accept(this.mLaunchers.valueAt(i));
        }
    }

    public void forAllPackageItems(Consumer<? super ShortcutPackageItem> callback) {
        forAllLaunchers(callback);
        forAllPackages(callback);
    }

    public void forPackageItem(String packageName, int packageUserId, Consumer<ShortcutPackageItem> callback) {
        forAllPackageItems(new -$$Lambda$ShortcutUser$XHWlvjfCvG1SoVwGHi3envhmtfM(packageUserId, packageName, callback));
    }

    static /* synthetic */ void lambda$forPackageItem$0(int packageUserId, String packageName, Consumer callback, ShortcutPackageItem spi) {
        if (spi.getPackageUserId() == packageUserId && spi.getPackageName().equals(packageName)) {
            callback.accept(spi);
        }
    }

    public void onCalledByPublisher(String packageName) {
        detectLocaleChange();
        rescanPackageIfNeeded(packageName, false);
    }

    private String getKnownLocales() {
        if (TextUtils.isEmpty(this.mKnownLocales)) {
            this.mKnownLocales = this.mService.injectGetLocaleTagsForUser(this.mUserId);
            this.mService.scheduleSaveUser(this.mUserId);
        }
        return this.mKnownLocales;
    }

    public void detectLocaleChange() {
        String currentLocales = this.mService.injectGetLocaleTagsForUser(this.mUserId);
        if (!getKnownLocales().equals(currentLocales)) {
            this.mKnownLocales = currentLocales;
            forAllPackages(-$$Lambda$ShortcutUser$6rBk7xJFaM9dXyyKHFs-DCus0iM.INSTANCE);
            this.mService.scheduleSaveUser(this.mUserId);
        }
    }

    static /* synthetic */ void lambda$detectLocaleChange$1(ShortcutPackage pkg) {
        pkg.resetRateLimiting();
        pkg.resolveResourceStrings();
    }

    public void rescanPackageIfNeeded(String packageName, boolean forceRescan) {
        boolean isNewApp = this.mPackages.containsKey(packageName) ^ 1;
        if (!getPackageShortcuts(packageName).rescanPackageIfNeeded(isNewApp, forceRescan) && isNewApp) {
            this.mPackages.remove(packageName);
        }
    }

    public void attemptToRestoreIfNeededAndSave(ShortcutService s, String packageName, int packageUserId) {
        forPackageItem(packageName, packageUserId, -$$Lambda$ShortcutUser$bsc89E_40a5X2amehalpqawQ5hY.INSTANCE);
    }

    public void saveToXml(XmlSerializer out, boolean forBackup) throws IOException, XmlPullParserException {
        out.startTag(null, TAG_ROOT);
        if (forBackup) {
            ShortcutService.writeAttr(out, ATTR_RESTORE_SOURCE_FINGERPRINT, this.mService.injectBuildFingerprint());
        } else {
            ShortcutService.writeAttr(out, ATTR_KNOWN_LOCALES, this.mKnownLocales);
            ShortcutService.writeAttr(out, ATTR_LAST_APP_SCAN_TIME, this.mLastAppScanTime);
            ShortcutService.writeAttr(out, ATTR_LAST_APP_SCAN_OS_FINGERPRINT, this.mLastAppScanOsFingerprint);
            ShortcutService.writeAttr(out, ATTR_RESTORE_SOURCE_FINGERPRINT, this.mRestoreFromOsFingerprint);
            ShortcutService.writeTagValue(out, TAG_LAUNCHER, this.mLastKnownLauncher);
        }
        int size = this.mLaunchers.size();
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            saveShortcutPackageItem(out, (ShortcutPackageItem) this.mLaunchers.valueAt(i2), forBackup);
        }
        size = this.mPackages.size();
        while (i < size) {
            saveShortcutPackageItem(out, (ShortcutPackageItem) this.mPackages.valueAt(i), forBackup);
            i++;
        }
        out.endTag(null, TAG_ROOT);
    }

    private void saveShortcutPackageItem(XmlSerializer out, ShortcutPackageItem spi, boolean forBackup) throws IOException, XmlPullParserException {
        if (!forBackup || spi.getPackageUserId() == spi.getOwnerUserId()) {
            spi.saveToXml(out, forBackup);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:36:0x009d A:{Catch:{ RuntimeException -> 0x00c5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x00b5 A:{Catch:{ RuntimeException -> 0x00c5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x00a7 A:{Catch:{ RuntimeException -> 0x00c5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:37:0x009e A:{Catch:{ RuntimeException -> 0x00c5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:36:0x009d A:{Catch:{ RuntimeException -> 0x00c5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x00b5 A:{Catch:{ RuntimeException -> 0x00c5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x00a7 A:{Catch:{ RuntimeException -> 0x00c5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:37:0x009e A:{Catch:{ RuntimeException -> 0x00c5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:36:0x009d A:{Catch:{ RuntimeException -> 0x00c5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x00b5 A:{Catch:{ RuntimeException -> 0x00c5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x00a7 A:{Catch:{ RuntimeException -> 0x00c5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:37:0x009e A:{Catch:{ RuntimeException -> 0x00c5 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static ShortcutUser loadFromXml(ShortcutService s, XmlPullParser parser, int userId, boolean fromBackup) throws IOException, XmlPullParserException, InvalidFileFormatException {
        ShortcutService shortcutService = s;
        XmlPullParser xmlPullParser = parser;
        int i = userId;
        boolean z = fromBackup;
        ShortcutUser ret = new ShortcutUser(shortcutService, i);
        try {
            ret.mKnownLocales = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_KNOWN_LOCALES);
            long lastAppScanTime = ShortcutService.parseLongAttribute(xmlPullParser, ATTR_LAST_APP_SCAN_TIME);
            ret.mLastAppScanTime = lastAppScanTime < s.injectCurrentTimeMillis() ? lastAppScanTime : 0;
            ret.mLastAppScanOsFingerprint = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_LAST_APP_SCAN_OS_FINGERPRINT);
            ret.mRestoreFromOsFingerprint = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_RESTORE_SOURCE_FINGERPRINT);
            int outerDepth = parser.getDepth();
            while (true) {
                int next = parser.next();
                int type = next;
                if (next == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                    return ret;
                }
                if (type == 2) {
                    int depth = parser.getDepth();
                    String tag = parser.getName();
                    if (depth == outerDepth + 1) {
                        Object obj;
                        next = tag.hashCode();
                        if (next == -1407250528) {
                            if (tag.equals(TAG_LAUNCHER)) {
                                obj = null;
                                switch (obj) {
                                    case null:
                                        break;
                                    case 1:
                                        break;
                                    case 2:
                                        break;
                                    default:
                                        break;
                                }
                            }
                        } else if (next == -1146595445) {
                            if (tag.equals("launcher-pins")) {
                                obj = 2;
                                switch (obj) {
                                    case null:
                                        break;
                                    case 1:
                                        break;
                                    case 2:
                                        break;
                                    default:
                                        break;
                                }
                            }
                        } else if (next == -807062458) {
                            if (tag.equals("package")) {
                                obj = 1;
                                switch (obj) {
                                    case null:
                                        ret.mLastKnownLauncher = ShortcutService.parseComponentNameAttribute(xmlPullParser, ATTR_VALUE);
                                        continue;
                                        continue;
                                        continue;
                                        continue;
                                    case 1:
                                        ShortcutPackage shortcuts = ShortcutPackage.loadFromXml(shortcutService, ret, xmlPullParser, z);
                                        ret.mPackages.put(shortcuts.getPackageName(), shortcuts);
                                        continue;
                                        continue;
                                        continue;
                                        continue;
                                    case 2:
                                        ret.addLauncher(ShortcutLauncher.loadFromXml(xmlPullParser, ret, i, z));
                                        continue;
                                        continue;
                                        continue;
                                        continue;
                                    default:
                                        break;
                                }
                            }
                        }
                        obj = -1;
                        switch (obj) {
                            case null:
                                break;
                            case 1:
                                break;
                            case 2:
                                break;
                            default:
                                break;
                        }
                    }
                    ShortcutService.warnForInvalidTag(depth, tag);
                }
            }
            return ret;
        } catch (RuntimeException e) {
            throw new InvalidFileFormatException("Unable to parse file", e);
        }
    }

    public ComponentName getLastKnownLauncher() {
        return this.mLastKnownLauncher;
    }

    public void setLauncher(ComponentName launcherComponent) {
        setLauncher(launcherComponent, false);
    }

    public void clearLauncher() {
        setLauncher(null);
    }

    public void forceClearLauncher() {
        setLauncher(null, true);
    }

    private void setLauncher(ComponentName launcherComponent, boolean allowPurgeLastKnown) {
        this.mCachedLauncher = launcherComponent;
        if (!Objects.equals(this.mLastKnownLauncher, launcherComponent)) {
            if (allowPurgeLastKnown || launcherComponent != null) {
                this.mLastKnownLauncher = launcherComponent;
                this.mService.scheduleSaveUser(this.mUserId);
            }
        }
    }

    public ComponentName getCachedLauncher() {
        return this.mCachedLauncher;
    }

    public void resetThrottling() {
        for (int i = this.mPackages.size() - 1; i >= 0; i--) {
            ((ShortcutPackage) this.mPackages.valueAt(i)).resetThrottling();
        }
    }

    public void mergeRestoredFile(ShortcutUser restored) {
        ShortcutService s = this.mService;
        int[] restoredLaunchers = new int[1];
        int[] restoredPackages = new int[1];
        int[] restoredShortcuts = new int[1];
        this.mLaunchers.clear();
        restored.forAllLaunchers(new -$$Lambda$ShortcutUser$zwhAnw7NjAOfNphKSeWurjAD6OM(this, s, restoredLaunchers));
        restored.forAllPackages(new -$$Lambda$ShortcutUser$078_3k15h1rTyJTkYAHYqf5ltYg(this, s, restoredPackages, restoredShortcuts));
        restored.mLaunchers.clear();
        restored.mPackages.clear();
        this.mRestoreFromOsFingerprint = restored.mRestoreFromOsFingerprint;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Restored: L=");
        stringBuilder.append(restoredLaunchers[0]);
        stringBuilder.append(" P=");
        stringBuilder.append(restoredPackages[0]);
        stringBuilder.append(" S=");
        stringBuilder.append(restoredShortcuts[0]);
        Slog.i(str, stringBuilder.toString());
    }

    public static /* synthetic */ void lambda$mergeRestoredFile$3(ShortcutUser shortcutUser, ShortcutService s, int[] restoredLaunchers, ShortcutLauncher sl) {
        if (!s.isPackageInstalled(sl.getPackageName(), shortcutUser.getUserId()) || s.shouldBackupApp(sl.getPackageName(), shortcutUser.getUserId())) {
            shortcutUser.addLauncher(sl);
            restoredLaunchers[0] = restoredLaunchers[0] + 1;
        }
    }

    public static /* synthetic */ void lambda$mergeRestoredFile$4(ShortcutUser shortcutUser, ShortcutService s, int[] restoredPackages, int[] restoredShortcuts, ShortcutPackage sp) {
        if (!s.isPackageInstalled(sp.getPackageName(), shortcutUser.getUserId()) || s.shouldBackupApp(sp.getPackageName(), shortcutUser.getUserId())) {
            ShortcutPackage previous = shortcutUser.getPackageShortcutsIfExists(sp.getPackageName());
            if (previous != null && previous.hasNonManifestShortcuts()) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Shortcuts for package ");
                stringBuilder.append(sp.getPackageName());
                stringBuilder.append(" are being restored. Existing non-manifeset shortcuts will be overwritten.");
                Log.w(str, stringBuilder.toString());
            }
            shortcutUser.addPackage(sp);
            restoredPackages[0] = restoredPackages[0] + 1;
            restoredShortcuts[0] = restoredShortcuts[0] + sp.getShortcutCount();
        }
    }

    public void dump(PrintWriter pw, String prefix, DumpFilter filter) {
        StringBuilder stringBuilder;
        if (filter.shouldDumpDetails()) {
            pw.print(prefix);
            pw.print("User: ");
            pw.print(this.mUserId);
            pw.print("  Known locales: ");
            pw.print(this.mKnownLocales);
            pw.print("  Last app scan: [");
            pw.print(this.mLastAppScanTime);
            pw.print("] ");
            pw.println(ShortcutService.formatTime(this.mLastAppScanTime));
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append(prefix);
            stringBuilder.append("  ");
            prefix = stringBuilder.toString();
            pw.print(prefix);
            pw.print("Last app scan FP: ");
            pw.println(this.mLastAppScanOsFingerprint);
            pw.print(prefix);
            pw.print("Restore from FP: ");
            pw.print(this.mRestoreFromOsFingerprint);
            pw.println();
            pw.print(prefix);
            pw.print("Cached launcher: ");
            pw.print(this.mCachedLauncher);
            pw.println();
            pw.print(prefix);
            pw.print("Last known launcher: ");
            pw.print(this.mLastKnownLauncher);
            pw.println();
        }
        int i = 0;
        for (int i2 = 0; i2 < this.mLaunchers.size(); i2++) {
            ShortcutLauncher launcher = (ShortcutLauncher) this.mLaunchers.valueAt(i2);
            if (filter.isPackageMatch(launcher.getPackageName())) {
                launcher.dump(pw, prefix, filter);
            }
        }
        while (i < this.mPackages.size()) {
            ShortcutPackage pkg = (ShortcutPackage) this.mPackages.valueAt(i);
            if (filter.isPackageMatch(pkg.getPackageName())) {
                pkg.dump(pw, prefix, filter);
            }
            i++;
        }
        if (filter.shouldDumpDetails()) {
            pw.println();
            pw.print(prefix);
            pw.println("Bitmap directories: ");
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("  ");
            dumpDirectorySize(pw, stringBuilder.toString(), this.mService.getUserBitmapFilePath(this.mUserId));
        }
    }

    private void dumpDirectorySize(PrintWriter pw, String prefix, File path) {
        int numFiles = 0;
        long size = 0;
        if (path.listFiles() != null) {
            for (File child : path.listFiles()) {
                if (child.isFile()) {
                    numFiles++;
                    size += child.length();
                } else if (child.isDirectory()) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(prefix);
                    stringBuilder.append("  ");
                    dumpDirectorySize(pw, stringBuilder.toString(), child);
                }
            }
        }
        pw.print(prefix);
        pw.print("Path: ");
        pw.print(path.getName());
        pw.print("/ has ");
        pw.print(numFiles);
        pw.print(" files, size=");
        pw.print(size);
        pw.print(" (");
        pw.print(Formatter.formatFileSize(this.mService.mContext, size));
        pw.println(")");
    }

    public JSONObject dumpCheckin(boolean clear) throws JSONException {
        JSONObject result = new JSONObject();
        result.put(KEY_USER_ID, this.mUserId);
        JSONArray launchers = new JSONArray();
        int i = 0;
        for (int i2 = 0; i2 < this.mLaunchers.size(); i2++) {
            launchers.put(((ShortcutLauncher) this.mLaunchers.valueAt(i2)).dumpCheckin(clear));
        }
        result.put(KEY_LAUNCHERS, launchers);
        launchers = new JSONArray();
        while (i < this.mPackages.size()) {
            launchers.put(((ShortcutPackage) this.mPackages.valueAt(i)).dumpCheckin(clear));
            i++;
        }
        result.put(KEY_PACKAGES, launchers);
        return result;
    }
}
