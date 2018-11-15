package com.android.server.pm.permission;

import android.content.Context;
import android.content.pm.PackageParser.PermissionGroup;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.XmlUtils;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class PermissionSettings {
    @GuardedBy("mLock")
    final ArrayMap<String, ArraySet<String>> mAppOpPermissionPackages = new ArrayMap();
    private final Object mLock;
    @GuardedBy("mLock")
    final ArrayMap<String, PermissionGroup> mPermissionGroups = new ArrayMap();
    public final boolean mPermissionReviewRequired;
    @GuardedBy("mLock")
    final ArrayMap<String, BasePermission> mPermissionTrees = new ArrayMap();
    @GuardedBy("mLock")
    final ArrayMap<String, BasePermission> mPermissions = new ArrayMap();

    PermissionSettings(Context context, Object lock) {
        this.mPermissionReviewRequired = context.getResources().getBoolean(17957000);
        this.mLock = lock;
    }

    public BasePermission getPermission(String permName) {
        BasePermission permissionLocked;
        synchronized (this.mLock) {
            permissionLocked = getPermissionLocked(permName);
        }
        return permissionLocked;
    }

    public void addAppOpPackage(String permName, String packageName) {
        ArraySet<String> pkgs = (ArraySet) this.mAppOpPermissionPackages.get(permName);
        if (pkgs == null) {
            pkgs = new ArraySet();
            this.mAppOpPermissionPackages.put(permName, pkgs);
        }
        pkgs.add(packageName);
    }

    public void transferPermissions(String origPackageName, String newPackageName) {
        synchronized (this.mLock) {
            int i = 0;
            while (i < 2) {
                for (BasePermission bp : (i == 0 ? this.mPermissionTrees : this.mPermissions).values()) {
                    bp.transfer(origPackageName, newPackageName);
                }
                i++;
            }
        }
    }

    public boolean canPropagatePermissionToInstantApp(String permName) {
        boolean z;
        synchronized (this.mLock) {
            BasePermission bp = (BasePermission) this.mPermissions.get(permName);
            z = bp != null && ((bp.isRuntime() || bp.isDevelopment()) && bp.isInstant());
        }
        return z;
    }

    public void readPermissions(XmlPullParser parser) throws IOException, XmlPullParserException {
        synchronized (this.mLock) {
            readPermissions(this.mPermissions, parser);
        }
    }

    public void readPermissionTrees(XmlPullParser parser) throws IOException, XmlPullParserException {
        synchronized (this.mLock) {
            readPermissions(this.mPermissionTrees, parser);
        }
    }

    public void writePermissions(XmlSerializer serializer) throws IOException {
        synchronized (this.mLock) {
            for (BasePermission bp : this.mPermissions.values()) {
                bp.writeLPr(serializer);
            }
        }
    }

    public void writePermissionTrees(XmlSerializer serializer) throws IOException {
        synchronized (this.mLock) {
            for (BasePermission bp : this.mPermissionTrees.values()) {
                bp.writeLPr(serializer);
            }
        }
    }

    public static void readPermissions(ArrayMap<String, BasePermission> out, XmlPullParser parser) throws IOException, XmlPullParserException {
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
                    if (!BasePermission.readLPw(out, parser)) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown element reading permissions: ");
                        stringBuilder.append(parser.getName());
                        stringBuilder.append(" at ");
                        stringBuilder.append(parser.getPositionDescription());
                        PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
                    }
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
    }

    public void dumpPermissions(PrintWriter pw, String packageName, ArraySet<String> permissionNames, boolean externalStorageEnforced, DumpState dumpState) {
        synchronized (this.mLock) {
            boolean printedSomething = false;
            for (BasePermission bp : this.mPermissions.values()) {
                printedSomething = bp.dumpPermissionsLPr(pw, packageName, permissionNames, externalStorageEnforced, printedSomething, dumpState);
            }
            if (packageName == null && permissionNames == null) {
                for (int iperm = 0; iperm < this.mAppOpPermissionPackages.size(); iperm++) {
                    if (iperm == 0) {
                        if (dumpState.onTitlePrinted()) {
                            pw.println();
                        }
                        pw.println("AppOp Permissions:");
                    }
                    pw.print("  AppOp Permission ");
                    pw.print((String) this.mAppOpPermissionPackages.keyAt(iperm));
                    pw.println(":");
                    ArraySet<String> pkgs = (ArraySet) this.mAppOpPermissionPackages.valueAt(iperm);
                    for (int ipkg = 0; ipkg < pkgs.size(); ipkg++) {
                        pw.print("    ");
                        pw.println((String) pkgs.valueAt(ipkg));
                    }
                }
            }
        }
    }

    @GuardedBy("mLock")
    BasePermission getPermissionLocked(String permName) {
        return (BasePermission) this.mPermissions.get(permName);
    }

    @GuardedBy("mLock")
    BasePermission getPermissionTreeLocked(String permName) {
        return (BasePermission) this.mPermissionTrees.get(permName);
    }

    @GuardedBy("mLock")
    void putPermissionLocked(String permName, BasePermission permission) {
        this.mPermissions.put(permName, permission);
    }

    @GuardedBy("mLock")
    void putPermissionTreeLocked(String permName, BasePermission permission) {
        this.mPermissionTrees.put(permName, permission);
    }

    @GuardedBy("mLock")
    void removePermissionLocked(String permName) {
        this.mPermissions.remove(permName);
    }

    @GuardedBy("mLock")
    void removePermissionTreeLocked(String permName) {
        this.mPermissionTrees.remove(permName);
    }

    @GuardedBy("mLock")
    Collection<BasePermission> getAllPermissionsLocked() {
        return this.mPermissions.values();
    }

    @GuardedBy("mLock")
    Collection<BasePermission> getAllPermissionTreesLocked() {
        return this.mPermissionTrees.values();
    }

    BasePermission enforcePermissionTree(String permName, int callingUid) {
        BasePermission enforcePermissionTree;
        synchronized (this.mLock) {
            enforcePermissionTree = BasePermission.enforcePermissionTree(this.mPermissionTrees.values(), permName, callingUid);
        }
        return enforcePermissionTree;
    }

    public boolean isPermissionInstant(String permName) {
        boolean z;
        synchronized (this.mLock) {
            BasePermission bp = (BasePermission) this.mPermissions.get(permName);
            z = bp != null && bp.isInstant();
        }
        return z;
    }

    boolean isPermissionAppOp(String permName) {
        boolean z;
        synchronized (this.mLock) {
            BasePermission bp = (BasePermission) this.mPermissions.get(permName);
            z = bp != null && bp.isAppOp();
        }
        return z;
    }
}
