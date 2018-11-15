package com.android.server.pm;

import android.content.pm.PackageParser.Package;
import android.content.pm.UserInfo;
import android.util.proto.ProtoOutputStream;
import com.android.server.pm.permission.PermissionsState;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import java.io.File;
import java.util.List;

public final class PackageSetting extends PackageSettingBase {
    int appId;
    Package pkg;
    SharedUserSetting sharedUser;
    private int sharedUserId;

    PackageSetting(String name, String realName, File codePath, File resourcePath, String legacyNativeLibraryPathString, String primaryCpuAbiString, String secondaryCpuAbiString, String cpuAbiOverrideString, long pVersionCode, int pkgFlags, int privateFlags, String parentPackageName, List<String> childPackageNames, int sharedUserId, String[] usesStaticLibraries, long[] usesStaticLibrariesVersions) {
        super(name, realName, codePath, resourcePath, legacyNativeLibraryPathString, primaryCpuAbiString, secondaryCpuAbiString, cpuAbiOverrideString, pVersionCode, pkgFlags, privateFlags, parentPackageName, childPackageNames, usesStaticLibraries, usesStaticLibrariesVersions);
        this.sharedUserId = sharedUserId;
    }

    PackageSetting(PackageSetting orig) {
        super(orig, orig.realName);
        doCopy(orig);
    }

    PackageSetting(PackageSetting orig, String realPkgName) {
        super(orig, realPkgName);
        doCopy(orig);
    }

    public int getSharedUserId() {
        if (this.sharedUser != null) {
            return this.sharedUser.userId;
        }
        return this.sharedUserId;
    }

    public SharedUserSetting getSharedUser() {
        return this.sharedUser;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PackageSetting{");
        stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
        stringBuilder.append(" ");
        stringBuilder.append(this.name);
        stringBuilder.append(SliceAuthority.DELIMITER);
        stringBuilder.append(this.appId);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public void copyFrom(PackageSetting orig) {
        super.copyFrom((PackageSettingBase) orig);
        doCopy(orig);
    }

    private void doCopy(PackageSetting orig) {
        this.appId = orig.appId;
        this.pkg = orig.pkg;
        this.sharedUser = orig.sharedUser;
        this.sharedUserId = orig.sharedUserId;
    }

    public PermissionsState getPermissionsState() {
        if (this.sharedUser != null) {
            return this.sharedUser.getPermissionsState();
        }
        return super.getPermissionsState();
    }

    public Package getPackage() {
        return this.pkg;
    }

    public int getAppId() {
        return this.appId;
    }

    public void setInstallPermissionsFixed(boolean fixed) {
        this.installPermissionsFixed = fixed;
    }

    public boolean areInstallPermissionsFixed() {
        return this.installPermissionsFixed;
    }

    public boolean isPrivileged() {
        return (this.pkgPrivateFlags & 8) != 0;
    }

    public boolean isOem() {
        return (this.pkgPrivateFlags & 131072) != 0;
    }

    public boolean isVendor() {
        return (this.pkgPrivateFlags & 262144) != 0;
    }

    public boolean isProduct() {
        return (this.pkgPrivateFlags & DumpState.DUMP_FROZEN) != 0;
    }

    public boolean isForwardLocked() {
        return (this.pkgPrivateFlags & 4) != 0;
    }

    public boolean isSystem() {
        return (this.pkgFlags & 1) != 0;
    }

    public boolean isUpdatedSystem() {
        return (this.pkgFlags & 128) != 0;
    }

    public boolean isSharedUser() {
        return this.sharedUser != null;
    }

    public boolean isMatch(int flags) {
        if ((DumpState.DUMP_DEXOPT & flags) != 0) {
            return isSystem();
        }
        return true;
    }

    public boolean hasChildPackages() {
        return (this.childPackageNames == null || this.childPackageNames.isEmpty()) ? false : true;
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId, List<UserInfo> list) {
        ProtoOutputStream protoOutputStream = proto;
        long packageToken = proto.start(fieldId);
        protoOutputStream.write(1138166333441L, this.realName != null ? this.realName : this.name);
        protoOutputStream.write(1120986464258L, this.appId);
        protoOutputStream.write(1120986464259L, this.versionCode);
        protoOutputStream.write(1138166333444L, this.pkg.mVersionName);
        protoOutputStream.write(1112396529669L, this.firstInstallTime);
        protoOutputStream.write(1112396529670L, this.lastUpdateTime);
        protoOutputStream.write(1138166333447L, this.installerPackageName);
        if (this.pkg != null) {
            long splitToken = protoOutputStream.start(2246267895816L);
            protoOutputStream.write(1138166333441L, "base");
            protoOutputStream.write(1120986464258L, this.pkg.baseRevisionCode);
            protoOutputStream.end(splitToken);
            if (this.pkg.splitNames != null) {
                for (int i = 0; i < this.pkg.splitNames.length; i++) {
                    splitToken = protoOutputStream.start(2246267895816L);
                    protoOutputStream.write(1138166333441L, this.pkg.splitNames[i]);
                    protoOutputStream.write(1120986464258L, this.pkg.splitRevisionCodes[i]);
                    protoOutputStream.end(splitToken);
                }
            }
        }
        writeUsersInfoToProto(protoOutputStream, 2246267895817L);
        protoOutputStream.end(packageToken);
    }
}
