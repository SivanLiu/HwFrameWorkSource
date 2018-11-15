package com.android.server.pm.permission;

import android.content.pm.PackageParser;
import android.content.pm.PackageParser.Package;
import android.content.pm.PackageParser.Permission;
import android.content.pm.PermissionInfo;
import android.content.pm.Signature;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.PackageSettingBase;
import com.android.server.pm.Settings;
import com.android.server.voiceinteraction.DatabaseHelper.SoundModelContract;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

public final class BasePermission {
    static final String TAG = "PackageManager";
    public static final int TYPE_BUILTIN = 1;
    public static final int TYPE_DYNAMIC = 2;
    public static final int TYPE_NORMAL = 0;
    private int[] gids;
    final String name;
    PermissionInfo pendingPermissionInfo;
    private boolean perUser;
    Permission perm;
    int protectionLevel = 2;
    String sourcePackageName;
    PackageSettingBase sourcePackageSetting;
    final int type;
    int uid;

    @Retention(RetentionPolicy.SOURCE)
    public @interface PermissionType {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ProtectionLevel {
    }

    public BasePermission(String _name, String _sourcePackageName, int _type) {
        this.name = _name;
        this.sourcePackageName = _sourcePackageName;
        this.type = _type;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("BasePermission{");
        stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
        stringBuilder.append(" ");
        stringBuilder.append(this.name);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public String getName() {
        return this.name;
    }

    public int getProtectionLevel() {
        return this.protectionLevel;
    }

    public String getSourcePackageName() {
        return this.sourcePackageName;
    }

    public PackageSettingBase getSourcePackageSetting() {
        return this.sourcePackageSetting;
    }

    public Signature[] getSourceSignatures() {
        return this.sourcePackageSetting.getSignatures();
    }

    public int getType() {
        return this.type;
    }

    public int getUid() {
        return this.uid;
    }

    public void setGids(int[] gids, boolean perUser) {
        this.gids = gids;
        this.perUser = perUser;
    }

    public void setPermission(Permission perm) {
        this.perm = perm;
    }

    public void setSourcePackageSetting(PackageSettingBase sourcePackageSetting) {
        this.sourcePackageSetting = sourcePackageSetting;
    }

    public int[] computeGids(int userId) {
        if (!this.perUser) {
            return this.gids;
        }
        int[] userGids = new int[this.gids.length];
        for (int i = 0; i < this.gids.length; i++) {
            userGids[i] = UserHandle.getUid(userId, this.gids[i]);
        }
        return userGids;
    }

    public int calculateFootprint(BasePermission perm) {
        if (this.uid == perm.uid) {
            return perm.name.length() + perm.perm.info.calculateFootprint();
        }
        return 0;
    }

    public boolean isPermission(Permission perm) {
        return this.perm == perm;
    }

    public boolean isDynamic() {
        return this.type == 2;
    }

    public boolean isNormal() {
        return (this.protectionLevel & 15) == 0;
    }

    public boolean isRuntime() {
        return (this.protectionLevel & 15) == 1;
    }

    public boolean isSignature() {
        return (this.protectionLevel & 15) == 2;
    }

    public boolean isAppOp() {
        return (this.protectionLevel & 64) != 0;
    }

    public boolean isDevelopment() {
        return isSignature() && (this.protectionLevel & 32) != 0;
    }

    public boolean isInstaller() {
        return (this.protectionLevel & 256) != 0;
    }

    public boolean isInstant() {
        return (this.protectionLevel & 4096) != 0;
    }

    public boolean isOEM() {
        return (this.protectionLevel & 16384) != 0;
    }

    public boolean isPre23() {
        return (this.protectionLevel & 128) != 0;
    }

    public boolean isPreInstalled() {
        return (this.protectionLevel & 1024) != 0;
    }

    public boolean isPrivileged() {
        return (this.protectionLevel & 16) != 0;
    }

    public boolean isRuntimeOnly() {
        return (this.protectionLevel & 8192) != 0;
    }

    public boolean isSetup() {
        return (this.protectionLevel & 2048) != 0;
    }

    public boolean isVerifier() {
        return (this.protectionLevel & 512) != 0;
    }

    public boolean isVendorPrivileged() {
        return (this.protectionLevel & 32768) != 0;
    }

    public boolean isSystemTextClassifier() {
        return (this.protectionLevel & 65536) != 0;
    }

    public void transfer(String origPackageName, String newPackageName) {
        if (origPackageName.equals(this.sourcePackageName)) {
            this.sourcePackageName = newPackageName;
            this.sourcePackageSetting = null;
            this.perm = null;
            if (this.pendingPermissionInfo != null) {
                this.pendingPermissionInfo.packageName = newPackageName;
            }
            this.uid = 0;
            setGids(null, false);
        }
    }

    public boolean addToTree(int protectionLevel, PermissionInfo info, BasePermission tree) {
        boolean changed = (this.protectionLevel == protectionLevel && this.perm != null && this.uid == tree.uid && this.perm.owner.equals(tree.perm.owner) && comparePermissionInfos(this.perm.info, info)) ? false : true;
        this.protectionLevel = protectionLevel;
        info = new PermissionInfo(info);
        info.protectionLevel = protectionLevel;
        this.perm = new Permission(tree.perm.owner, info);
        this.perm.info.packageName = tree.perm.info.packageName;
        this.uid = tree.uid;
        return changed;
    }

    public void updateDynamicPermission(Collection<BasePermission> permissionTrees) {
        if (PackageManagerService.DEBUG_SETTINGS) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Dynamic permission: name=");
            stringBuilder.append(getName());
            stringBuilder.append(" pkg=");
            stringBuilder.append(getSourcePackageName());
            stringBuilder.append(" info=");
            stringBuilder.append(this.pendingPermissionInfo);
            Log.v(str, stringBuilder.toString());
        }
        if (this.sourcePackageSetting == null && this.pendingPermissionInfo != null) {
            BasePermission tree = findPermissionTree(permissionTrees, this.name);
            if (tree != null && tree.perm != null) {
                this.sourcePackageSetting = tree.sourcePackageSetting;
                this.perm = new Permission(tree.perm.owner, new PermissionInfo(this.pendingPermissionInfo));
                this.perm.info.packageName = tree.perm.info.packageName;
                this.perm.info.name = this.name;
                this.uid = tree.uid;
            }
        }
    }

    static BasePermission createOrUpdate(BasePermission bp, Permission p, Package pkg, Collection<BasePermission> permissionTrees, boolean chatty) {
        String str;
        StringBuilder stringBuilder;
        PackageSettingBase pkgSetting = pkg.mExtras;
        if (!(bp == null || Objects.equals(bp.sourcePackageName, p.info.packageName))) {
            boolean currentOwnerIsSystem = bp.perm != null && bp.perm.owner.isSystem();
            if (p.owner.isSystem()) {
                if (bp.type == 1 && bp.perm == null) {
                    bp.sourcePackageSetting = pkgSetting;
                    bp.perm = p;
                    bp.uid = pkg.applicationInfo.uid;
                    bp.sourcePackageName = p.info.packageName;
                    PermissionInfo permissionInfo = p.info;
                    permissionInfo.flags |= 1073741824;
                } else if (!currentOwnerIsSystem) {
                    String msg = new StringBuilder();
                    msg.append("New decl ");
                    msg.append(p.owner);
                    msg.append(" of permission  ");
                    msg.append(p.info.name);
                    msg.append(" is system; overriding ");
                    msg.append(bp.sourcePackageName);
                    PackageManagerService.reportSettingsProblem(5, msg.toString());
                    bp = null;
                }
            }
        }
        if (bp == null) {
            bp = new BasePermission(p.info.name, p.info.packageName, 0);
        }
        StringBuilder r = null;
        if (bp.perm == null) {
            if (bp.sourcePackageName == null || bp.sourcePackageName.equals(p.info.packageName)) {
                BasePermission tree = findPermissionTree(permissionTrees, p.info.name);
                if (tree == null || tree.sourcePackageName.equals(p.info.packageName)) {
                    bp.sourcePackageSetting = pkgSetting;
                    bp.perm = p;
                    bp.uid = pkg.applicationInfo.uid;
                    bp.sourcePackageName = p.info.packageName;
                    PermissionInfo permissionInfo2 = p.info;
                    permissionInfo2.flags = 1073741824 | permissionInfo2.flags;
                    if (chatty) {
                        if (r == null) {
                            r = new StringBuilder(256);
                        } else {
                            r.append(' ');
                        }
                        r.append(p.info.name);
                    }
                } else {
                    str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Permission ");
                    stringBuilder2.append(p.info.name);
                    stringBuilder2.append(" from package ");
                    stringBuilder2.append(p.info.packageName);
                    stringBuilder2.append(" ignored: base tree ");
                    stringBuilder2.append(tree.name);
                    stringBuilder2.append(" is from package ");
                    stringBuilder2.append(tree.sourcePackageName);
                    Slog.w(str, stringBuilder2.toString());
                }
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Permission ");
                stringBuilder.append(p.info.name);
                stringBuilder.append(" from package ");
                stringBuilder.append(p.info.packageName);
                stringBuilder.append(" ignored: original from ");
                stringBuilder.append(bp.sourcePackageName);
                Slog.w(str, stringBuilder.toString());
            }
        } else if (chatty) {
            if (r == null) {
                r = new StringBuilder(256);
            } else {
                r.append(' ');
            }
            r.append("DUP:");
            r.append(p.info.name);
        }
        if (bp.perm == p) {
            bp.protectionLevel = p.info.protectionLevel;
        }
        if (PackageManagerService.DEBUG_PACKAGE_SCANNING && r != null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Permissions: ");
            stringBuilder.append(r);
            Log.d(str, stringBuilder.toString());
        }
        return bp;
    }

    static BasePermission enforcePermissionTree(Collection<BasePermission> permissionTrees, String permName, int callingUid) {
        if (permName != null) {
            BasePermission bp = findPermissionTree(permissionTrees, permName);
            if (bp != null) {
                if (bp.uid == UserHandle.getAppId(callingUid)) {
                    return bp;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Calling uid ");
                stringBuilder.append(callingUid);
                stringBuilder.append(" is not allowed to add to permission tree ");
                stringBuilder.append(bp.name);
                stringBuilder.append(" owned by uid ");
                stringBuilder.append(bp.uid);
                throw new SecurityException(stringBuilder.toString());
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("No permission tree found for ");
        stringBuilder2.append(permName);
        throw new SecurityException(stringBuilder2.toString());
    }

    public void enforceDeclaredUsedAndRuntimeOrDevelopment(Package pkg) {
        StringBuilder stringBuilder;
        if (pkg.requestedPermissions.indexOf(this.name) == -1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Package ");
            stringBuilder.append(pkg.packageName);
            stringBuilder.append(" has not requested permission ");
            stringBuilder.append(this.name);
            throw new SecurityException(stringBuilder.toString());
        } else if (!isRuntime() && !isDevelopment()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Permission ");
            stringBuilder.append(this.name);
            stringBuilder.append(" is not a changeable permission type");
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private static BasePermission findPermissionTree(Collection<BasePermission> permissionTrees, String permName) {
        for (BasePermission bp : permissionTrees) {
            if (permName.startsWith(bp.name) && permName.length() > bp.name.length() && permName.charAt(bp.name.length()) == '.') {
                return bp;
            }
        }
        return null;
    }

    public PermissionInfo generatePermissionInfo(String groupName, int flags) {
        if (groupName == null) {
            if (this.perm == null || this.perm.info.group == null) {
                return generatePermissionInfo(this.protectionLevel, flags);
            }
        } else if (this.perm != null && groupName.equals(this.perm.info.group)) {
            return PackageParser.generatePermissionInfo(this.perm, flags);
        }
        return null;
    }

    public PermissionInfo generatePermissionInfo(int adjustedProtectionLevel, int flags) {
        if (this.perm != null) {
            boolean protectionLevelChanged = this.protectionLevel != adjustedProtectionLevel;
            PermissionInfo permissionInfo = PackageParser.generatePermissionInfo(this.perm, flags);
            if (protectionLevelChanged && permissionInfo == this.perm.info) {
                permissionInfo = new PermissionInfo(permissionInfo);
                permissionInfo.protectionLevel = adjustedProtectionLevel;
            }
            return permissionInfo;
        }
        PermissionInfo permissionInfo2 = new PermissionInfo();
        permissionInfo2.name = this.name;
        permissionInfo2.packageName = this.sourcePackageName;
        permissionInfo2.nonLocalizedLabel = this.name;
        permissionInfo2.protectionLevel = this.protectionLevel;
        return permissionInfo2;
    }

    public static boolean readLPw(Map<String, BasePermission> out, XmlPullParser parser) {
        if (!parser.getName().equals(Settings.TAG_ITEM)) {
            return false;
        }
        String name = parser.getAttributeValue(null, Settings.ATTR_NAME);
        String sourcePackage = parser.getAttributeValue(null, "package");
        String ptype = parser.getAttributeValue(null, SoundModelContract.KEY_TYPE);
        if (name == null || sourcePackage == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error in package manager settings: permissions has no name at ");
            stringBuilder.append(parser.getPositionDescription());
            PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
            return false;
        }
        boolean dynamic = "dynamic".equals(ptype);
        BasePermission bp = (BasePermission) out.get(name);
        if (bp == null || bp.type != 1) {
            bp = new BasePermission(name.intern(), sourcePackage, dynamic ? 2 : 0);
        }
        bp.protectionLevel = readInt(parser, null, "protection", 0);
        bp.protectionLevel = PermissionInfo.fixProtectionLevel(bp.protectionLevel);
        if (dynamic) {
            PermissionInfo pi = new PermissionInfo();
            pi.packageName = sourcePackage.intern();
            pi.name = name.intern();
            pi.icon = readInt(parser, null, "icon", 0);
            pi.nonLocalizedLabel = parser.getAttributeValue(null, "label");
            pi.protectionLevel = bp.protectionLevel;
            bp.pendingPermissionInfo = pi;
        }
        out.put(bp.name, bp);
        return true;
    }

    private static int readInt(XmlPullParser parser, String ns, String name, int defValue) {
        String v = parser.getAttributeValue(ns, name);
        if (v == null) {
            return defValue;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error in package manager settings: attribute ");
            stringBuilder.append(name);
            stringBuilder.append(" has bad integer value ");
            stringBuilder.append(v);
            stringBuilder.append(" at ");
            stringBuilder.append(parser.getPositionDescription());
            PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
            return defValue;
        }
    }

    public void writeLPr(XmlSerializer serializer) throws IOException {
        if (this.sourcePackageName != null) {
            serializer.startTag(null, Settings.TAG_ITEM);
            serializer.attribute(null, Settings.ATTR_NAME, this.name);
            serializer.attribute(null, "package", this.sourcePackageName);
            if (this.protectionLevel != 0) {
                serializer.attribute(null, "protection", Integer.toString(this.protectionLevel));
            }
            if (this.type == 2) {
                PermissionInfo pi = this.perm != null ? this.perm.info : this.pendingPermissionInfo;
                if (pi != null) {
                    serializer.attribute(null, SoundModelContract.KEY_TYPE, "dynamic");
                    if (pi.icon != 0) {
                        serializer.attribute(null, "icon", Integer.toString(pi.icon));
                    }
                    if (pi.nonLocalizedLabel != null) {
                        serializer.attribute(null, "label", pi.nonLocalizedLabel.toString());
                    }
                }
            }
            serializer.endTag(null, Settings.TAG_ITEM);
        }
    }

    private static boolean compareStrings(CharSequence s1, CharSequence s2) {
        boolean z = false;
        if (s1 == null) {
            if (s2 == null) {
                z = true;
            }
            return z;
        } else if (s2 != null && s1.getClass() == s2.getClass()) {
            return s1.equals(s2);
        } else {
            return false;
        }
    }

    private static boolean comparePermissionInfos(PermissionInfo pi1, PermissionInfo pi2) {
        if (pi1.icon == pi2.icon && pi1.logo == pi2.logo && pi1.protectionLevel == pi2.protectionLevel && compareStrings(pi1.name, pi2.name) && compareStrings(pi1.nonLocalizedLabel, pi2.nonLocalizedLabel) && compareStrings(pi1.packageName, pi2.packageName)) {
            return true;
        }
        return false;
    }

    public boolean dumpPermissionsLPr(PrintWriter pw, String packageName, Set<String> permissionNames, boolean readEnforced, boolean printedSomething, DumpState dumpState) {
        if (packageName != null && !packageName.equals(this.sourcePackageName)) {
            return false;
        }
        if (permissionNames != null && !permissionNames.contains(this.name)) {
            return false;
        }
        if (!printedSomething) {
            if (dumpState.onTitlePrinted()) {
                pw.println();
            }
            pw.println("Permissions:");
        }
        pw.print("  Permission [");
        pw.print(this.name);
        pw.print("] (");
        pw.print(Integer.toHexString(System.identityHashCode(this)));
        pw.println("):");
        pw.print("    sourcePackage=");
        pw.println(this.sourcePackageName);
        pw.print("    uid=");
        pw.print(this.uid);
        pw.print(" gids=");
        pw.print(Arrays.toString(computeGids(0)));
        pw.print(" type=");
        pw.print(this.type);
        pw.print(" prot=");
        pw.println(PermissionInfo.protectionToString(this.protectionLevel));
        if (this.perm != null) {
            pw.print("    perm=");
            pw.println(this.perm);
            if ((this.perm.info.flags & 1073741824) == 0 || (this.perm.info.flags & 2) != 0) {
                pw.print("    flags=0x");
                pw.println(Integer.toHexString(this.perm.info.flags));
            }
        }
        if (this.sourcePackageSetting != null) {
            pw.print("    packageSetting=");
            pw.println(this.sourcePackageSetting);
        }
        if ("android.permission.READ_EXTERNAL_STORAGE".equals(this.name)) {
            pw.print("    enforced=");
            pw.println(readEnforced);
        }
        return true;
    }
}
