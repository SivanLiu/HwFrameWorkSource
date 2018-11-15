package com.android.server.pm;

import android.content.pm.PackageInfo;
import android.content.pm.ShortcutInfo;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class ShortcutLauncher extends ShortcutPackageItem {
    private static final String ATTR_LAUNCHER_USER_ID = "launcher-user";
    private static final String ATTR_PACKAGE_NAME = "package-name";
    private static final String ATTR_PACKAGE_USER_ID = "package-user";
    private static final String ATTR_VALUE = "value";
    private static final String TAG = "ShortcutService";
    private static final String TAG_PACKAGE = "package";
    private static final String TAG_PIN = "pin";
    static final String TAG_ROOT = "launcher-pins";
    private final int mOwnerUserId;
    private final ArrayMap<PackageWithUser, ArraySet<String>> mPinnedShortcuts;

    private ShortcutLauncher(ShortcutUser shortcutUser, int ownerUserId, String packageName, int launcherUserId, ShortcutPackageInfo spi) {
        ShortcutPackageInfo shortcutPackageInfo;
        if (spi != null) {
            shortcutPackageInfo = spi;
        } else {
            shortcutPackageInfo = ShortcutPackageInfo.newEmpty();
        }
        super(shortcutUser, launcherUserId, packageName, shortcutPackageInfo);
        this.mPinnedShortcuts = new ArrayMap();
        this.mOwnerUserId = ownerUserId;
    }

    public ShortcutLauncher(ShortcutUser shortcutUser, int ownerUserId, String packageName, int launcherUserId) {
        this(shortcutUser, ownerUserId, packageName, launcherUserId, null);
    }

    public int getOwnerUserId() {
        return this.mOwnerUserId;
    }

    protected boolean canRestoreAnyVersion() {
        return true;
    }

    private void onRestoreBlocked() {
        ArrayList<PackageWithUser> pinnedPackages = new ArrayList(this.mPinnedShortcuts.keySet());
        this.mPinnedShortcuts.clear();
        for (int i = pinnedPackages.size() - 1; i >= 0; i--) {
            ShortcutPackage p = this.mShortcutUser.getPackageShortcutsIfExists(((PackageWithUser) pinnedPackages.get(i)).packageName);
            if (p != null) {
                p.refreshPinnedFlags();
            }
        }
    }

    protected void onRestored(int restoreBlockReason) {
        if (restoreBlockReason != 0) {
            onRestoreBlocked();
        }
    }

    public void pinShortcuts(int packageUserId, String packageName, List<String> ids, boolean forPinRequest) {
        ShortcutPackage packageShortcuts = this.mShortcutUser.getPackageShortcutsIfExists(packageName);
        if (packageShortcuts != null) {
            PackageWithUser pu = PackageWithUser.of(packageUserId, packageName);
            int idSize = ids.size();
            if (idSize == 0) {
                this.mPinnedShortcuts.remove(pu);
            } else {
                ArraySet<String> prevSet = (ArraySet) this.mPinnedShortcuts.get(pu);
                ArraySet<String> newSet = new ArraySet();
                for (int i = 0; i < idSize; i++) {
                    String id = (String) ids.get(i);
                    ShortcutInfo si = packageShortcuts.findShortcutById(id);
                    if (si != null && (si.isDynamic() || si.isManifestShortcut() || ((prevSet != null && prevSet.contains(id)) || forPinRequest))) {
                        newSet.add(id);
                    }
                }
                this.mPinnedShortcuts.put(pu, newSet);
            }
            packageShortcuts.refreshPinnedFlags();
        }
    }

    public ArraySet<String> getPinnedShortcutIds(String packageName, int packageUserId) {
        return (ArraySet) this.mPinnedShortcuts.get(PackageWithUser.of(packageUserId, packageName));
    }

    public boolean hasPinned(ShortcutInfo shortcut) {
        ArraySet<String> pinned = getPinnedShortcutIds(shortcut.getPackage(), shortcut.getUserId());
        return pinned != null && pinned.contains(shortcut.getId());
    }

    public void addPinnedShortcut(String packageName, int packageUserId, String id, boolean forPinRequest) {
        ArrayList<String> pinnedList;
        ArraySet<String> pinnedSet = getPinnedShortcutIds(packageName, packageUserId);
        if (pinnedSet != null) {
            pinnedList = new ArrayList(pinnedSet.size() + 1);
            pinnedList.addAll(pinnedSet);
        } else {
            pinnedList = new ArrayList(1);
        }
        pinnedList.add(id);
        pinShortcuts(packageUserId, packageName, pinnedList, forPinRequest);
    }

    boolean cleanUpPackage(String packageName, int packageUserId) {
        return this.mPinnedShortcuts.remove(PackageWithUser.of(packageUserId, packageName)) != null;
    }

    public void ensurePackageInfo() {
        PackageInfo pi = this.mShortcutUser.mService.getPackageInfoWithSignatures(getPackageName(), getPackageUserId());
        if (pi == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Package not found: ");
            stringBuilder.append(getPackageName());
            Slog.w(str, stringBuilder.toString());
            return;
        }
        getPackageInfo().updateFromPackageInfo(pi);
    }

    public void saveToXml(XmlSerializer out, boolean forBackup) throws IOException {
        if (!forBackup || getPackageInfo().isBackupAllowed()) {
            int size = this.mPinnedShortcuts.size();
            if (size != 0) {
                out.startTag(null, TAG_ROOT);
                ShortcutService.writeAttr(out, ATTR_PACKAGE_NAME, getPackageName());
                ShortcutService.writeAttr(out, ATTR_LAUNCHER_USER_ID, (long) getPackageUserId());
                getPackageInfo().saveToXml(this.mShortcutUser.mService, out, forBackup);
                for (int i = 0; i < size; i++) {
                    PackageWithUser pu = (PackageWithUser) this.mPinnedShortcuts.keyAt(i);
                    if (!forBackup || pu.userId == getOwnerUserId()) {
                        out.startTag(null, "package");
                        ShortcutService.writeAttr(out, ATTR_PACKAGE_NAME, pu.packageName);
                        ShortcutService.writeAttr(out, ATTR_PACKAGE_USER_ID, (long) pu.userId);
                        ArraySet<String> ids = (ArraySet) this.mPinnedShortcuts.valueAt(i);
                        int idSize = ids.size();
                        for (int j = 0; j < idSize; j++) {
                            ShortcutService.writeTagValue(out, TAG_PIN, (String) ids.valueAt(j));
                        }
                        out.endTag(null, "package");
                    }
                }
                out.endTag(null, TAG_ROOT);
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:33:0x0096  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0074  */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x0096  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0074  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static ShortcutLauncher loadFromXml(XmlPullParser parser, ShortcutUser shortcutUser, int ownerUserId, boolean fromBackup) throws IOException, XmlPullParserException {
        XmlPullParser xmlPullParser = parser;
        int i = ownerUserId;
        boolean z = fromBackup;
        ShortcutLauncher ret = new ShortcutLauncher(shortcutUser, i, ShortcutService.parseStringAttribute(xmlPullParser, ATTR_PACKAGE_NAME), z ? i : ShortcutService.parseIntAttribute(xmlPullParser, ATTR_LAUNCHER_USER_ID, i));
        ArraySet<String> ids = null;
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                return ret;
            }
            if (type == 2) {
                next = parser.getDepth();
                String tag = parser.getName();
                Object obj = null;
                if (next == outerDepth + 1) {
                    Object obj2;
                    int hashCode = tag.hashCode();
                    if (hashCode == -1923478059) {
                        if (tag.equals("package-info")) {
                            obj2 = null;
                            switch (obj2) {
                                case null:
                                    break;
                                case 1:
                                    break;
                            }
                        }
                    } else if (hashCode == -807062458 && tag.equals("package")) {
                        obj2 = 1;
                        switch (obj2) {
                            case null:
                                ret.getPackageInfo().loadFromXml(xmlPullParser, z);
                                continue;
                                continue;
                                continue;
                            case 1:
                                String packageName = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_PACKAGE_NAME);
                                hashCode = z ? i : ShortcutService.parseIntAttribute(xmlPullParser, ATTR_PACKAGE_USER_ID, i);
                                ids = new ArraySet();
                                ret.mPinnedShortcuts.put(PackageWithUser.of(hashCode, packageName), ids);
                                continue;
                                continue;
                                continue;
                        }
                    }
                    obj2 = -1;
                    switch (obj2) {
                        case null:
                            break;
                        case 1:
                            break;
                    }
                }
                if (next == outerDepth + 2) {
                    if (!(tag.hashCode() == 110997 && tag.equals(TAG_PIN))) {
                        obj = -1;
                    }
                    if (obj == null) {
                        if (ids == null) {
                            Slog.w(TAG, "pin in invalid place");
                        } else {
                            ids.add(ShortcutService.parseStringAttribute(xmlPullParser, ATTR_VALUE));
                        }
                    }
                }
                ShortcutService.warnForInvalidTag(next, tag);
            }
        }
        return ret;
    }

    public void dump(PrintWriter pw, String prefix, DumpFilter filter) {
        pw.println();
        pw.print(prefix);
        pw.print("Launcher: ");
        pw.print(getPackageName());
        pw.print("  Package user: ");
        pw.print(getPackageUserId());
        pw.print("  Owner user: ");
        pw.print(getOwnerUserId());
        pw.println();
        ShortcutPackageInfo packageInfo = getPackageInfo();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  ");
        packageInfo.dump(pw, stringBuilder.toString());
        pw.println();
        int size = this.mPinnedShortcuts.size();
        for (int i = 0; i < size; i++) {
            pw.println();
            PackageWithUser pu = (PackageWithUser) this.mPinnedShortcuts.keyAt(i);
            pw.print(prefix);
            pw.print("  ");
            pw.print("Package: ");
            pw.print(pu.packageName);
            pw.print("  User: ");
            pw.println(pu.userId);
            ArraySet<String> ids = (ArraySet) this.mPinnedShortcuts.valueAt(i);
            int idSize = ids.size();
            for (int j = 0; j < idSize; j++) {
                pw.print(prefix);
                pw.print("    Pinned: ");
                pw.print((String) ids.valueAt(j));
                pw.println();
            }
        }
    }

    public JSONObject dumpCheckin(boolean clear) throws JSONException {
        return super.dumpCheckin(clear);
    }

    @VisibleForTesting
    ArraySet<String> getAllPinnedShortcutsForTest(String packageName, int packageUserId) {
        return new ArraySet((ArraySet) this.mPinnedShortcuts.get(PackageWithUser.of(packageUserId, packageName)));
    }
}
