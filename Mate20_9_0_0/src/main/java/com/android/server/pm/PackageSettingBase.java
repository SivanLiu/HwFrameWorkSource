package com.android.server.pm;

import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.PackageParser.SigningDetails;
import android.content.pm.PackageUserState;
import android.content.pm.Signature;
import android.os.PersistableBundle;
import android.util.ArraySet;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.pm.permission.PermissionsState;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public abstract class PackageSettingBase extends SettingBase {
    static final PackageUserState DEFAULT_USER_STATE = new PackageUserState();
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    int appUseNotchMode;
    int categoryHint = -1;
    List<String> childPackageNames;
    File codePath;
    String codePathString;
    String cpuAbiOverrideString;
    long firstInstallTime;
    boolean installPermissionsFixed;
    String installerPackageName;
    boolean isOrphaned;
    PackageKeySetData keySetData = new PackageKeySetData();
    long lastUpdateTime;
    @Deprecated
    String legacyNativeLibraryPathString;
    float maxAspectRatio;
    public final String name;
    Set<String> oldCodePaths;
    String parentPackageName;
    String primaryCpuAbiString;
    final String realName;
    File resourcePath;
    String resourcePathString;
    String secondaryCpuAbiString;
    PackageSignatures signatures;
    long timeStamp;
    boolean uidError;
    boolean updateAvailable;
    private final SparseArray<PackageUserState> userState = new SparseArray();
    String[] usesStaticLibraries;
    long[] usesStaticLibrariesVersions;
    IntentFilterVerificationInfo verificationInfo;
    long versionCode;
    String volumeUuid;

    public /* bridge */ /* synthetic */ PermissionsState getPermissionsState() {
        return super.getPermissionsState();
    }

    PackageSettingBase(String name, String realName, File codePath, File resourcePath, String legacyNativeLibraryPathString, String primaryCpuAbiString, String secondaryCpuAbiString, String cpuAbiOverrideString, long pVersionCode, int pkgFlags, int pkgPrivateFlags, String parentPackageName, List<String> childPackageNames, String[] usesStaticLibraries, long[] usesStaticLibrariesVersions) {
        List<String> list = childPackageNames;
        super(pkgFlags, pkgPrivateFlags);
        this.name = name;
        this.realName = realName;
        this.parentPackageName = parentPackageName;
        this.childPackageNames = list != null ? new ArrayList(list) : null;
        this.usesStaticLibraries = usesStaticLibraries;
        this.usesStaticLibrariesVersions = usesStaticLibrariesVersions;
        init(codePath, resourcePath, legacyNativeLibraryPathString, primaryCpuAbiString, secondaryCpuAbiString, cpuAbiOverrideString, pVersionCode);
    }

    PackageSettingBase(PackageSettingBase base, String realName) {
        super(base);
        this.name = base.name;
        this.realName = realName;
        doCopy(base);
    }

    void init(File codePath, File resourcePath, String legacyNativeLibraryPathString, String primaryCpuAbiString, String secondaryCpuAbiString, String cpuAbiOverrideString, long pVersionCode) {
        this.codePath = codePath;
        this.codePathString = codePath.toString();
        this.resourcePath = resourcePath;
        this.resourcePathString = resourcePath.toString();
        this.legacyNativeLibraryPathString = legacyNativeLibraryPathString;
        this.primaryCpuAbiString = primaryCpuAbiString;
        this.secondaryCpuAbiString = secondaryCpuAbiString;
        this.cpuAbiOverrideString = cpuAbiOverrideString;
        this.versionCode = pVersionCode;
        this.signatures = new PackageSignatures();
    }

    public void setInstallerPackageName(String packageName) {
        this.installerPackageName = packageName;
    }

    public String getInstallerPackageName() {
        return this.installerPackageName;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getVolumeUuid() {
        return this.volumeUuid;
    }

    public void setTimeStamp(long newStamp) {
        this.timeStamp = newStamp;
    }

    public void setUpdateAvailable(boolean updateAvailable) {
        this.updateAvailable = updateAvailable;
    }

    public boolean isUpdateAvailable() {
        return this.updateAvailable;
    }

    public boolean isSharedUser() {
        return false;
    }

    public Signature[] getSignatures() {
        return this.signatures.mSigningDetails.signatures;
    }

    public SigningDetails getSigningDetails() {
        return this.signatures.mSigningDetails;
    }

    public void copyFrom(PackageSettingBase orig) {
        super.copyFrom(orig);
        doCopy(orig);
    }

    private void doCopy(PackageSettingBase orig) {
        String[] strArr;
        long[] jArr = null;
        this.childPackageNames = orig.childPackageNames != null ? new ArrayList(orig.childPackageNames) : null;
        this.codePath = orig.codePath;
        this.codePathString = orig.codePathString;
        this.cpuAbiOverrideString = orig.cpuAbiOverrideString;
        this.firstInstallTime = orig.firstInstallTime;
        this.installPermissionsFixed = orig.installPermissionsFixed;
        this.installerPackageName = orig.installerPackageName;
        this.isOrphaned = orig.isOrphaned;
        this.keySetData = orig.keySetData;
        this.lastUpdateTime = orig.lastUpdateTime;
        this.legacyNativeLibraryPathString = orig.legacyNativeLibraryPathString;
        this.parentPackageName = orig.parentPackageName;
        this.primaryCpuAbiString = orig.primaryCpuAbiString;
        this.resourcePath = orig.resourcePath;
        this.resourcePathString = orig.resourcePathString;
        this.secondaryCpuAbiString = orig.secondaryCpuAbiString;
        this.signatures = orig.signatures;
        this.timeStamp = orig.timeStamp;
        this.uidError = orig.uidError;
        this.userState.clear();
        for (int i = 0; i < orig.userState.size(); i++) {
            this.userState.put(orig.userState.keyAt(i), (PackageUserState) orig.userState.valueAt(i));
        }
        this.verificationInfo = orig.verificationInfo;
        this.versionCode = orig.versionCode;
        this.volumeUuid = orig.volumeUuid;
        this.categoryHint = orig.categoryHint;
        if (orig.usesStaticLibraries != null) {
            strArr = (String[]) Arrays.copyOf(orig.usesStaticLibraries, orig.usesStaticLibraries.length);
        } else {
            strArr = null;
        }
        this.usesStaticLibraries = strArr;
        if (orig.usesStaticLibrariesVersions != null) {
            jArr = Arrays.copyOf(orig.usesStaticLibrariesVersions, orig.usesStaticLibrariesVersions.length);
        }
        this.usesStaticLibrariesVersions = jArr;
        this.updateAvailable = orig.updateAvailable;
        this.maxAspectRatio = orig.maxAspectRatio;
        this.appUseNotchMode = orig.appUseNotchMode;
    }

    private PackageUserState modifyUserState(int userId) {
        PackageUserState state = (PackageUserState) this.userState.get(userId);
        if (state != null) {
            return state;
        }
        state = new PackageUserState();
        this.userState.put(userId, state);
        return state;
    }

    public PackageUserState readUserState(int userId) {
        PackageUserState state = (PackageUserState) this.userState.get(userId);
        if (state == null) {
            return DEFAULT_USER_STATE;
        }
        state.categoryHint = this.categoryHint;
        return state;
    }

    void setEnabled(int state, int userId, String callingPackage) {
        PackageUserState st = modifyUserState(userId);
        st.enabled = state;
        st.lastDisableAppCaller = callingPackage;
    }

    int getEnabled(int userId) {
        return readUserState(userId).enabled;
    }

    void setMaxAspectRatio(float ar) {
        this.maxAspectRatio = ar;
    }

    float getMaxAspectRatio() {
        return this.maxAspectRatio;
    }

    String getLastDisabledAppCaller(int userId) {
        return readUserState(userId).lastDisableAppCaller;
    }

    void setInstalled(boolean inst, int userId) {
        modifyUserState(userId).installed = inst;
    }

    boolean getInstalled(int userId) {
        return readUserState(userId).installed;
    }

    int getInstallReason(int userId) {
        return readUserState(userId).installReason;
    }

    void setInstallReason(int installReason, int userId) {
        modifyUserState(userId).installReason = installReason;
    }

    void setOverlayPaths(List<String> overlayPaths, int userId) {
        String[] strArr;
        PackageUserState modifyUserState = modifyUserState(userId);
        if (overlayPaths == null) {
            strArr = null;
        } else {
            strArr = (String[]) overlayPaths.toArray(new String[overlayPaths.size()]);
        }
        modifyUserState.overlayPaths = strArr;
    }

    String[] getOverlayPaths(int userId) {
        return readUserState(userId).overlayPaths;
    }

    @VisibleForTesting
    SparseArray<PackageUserState> getUserState() {
        return this.userState;
    }

    boolean isAnyInstalled(int[] users) {
        for (int user : users) {
            if (readUserState(user).installed) {
                return true;
            }
        }
        return false;
    }

    int[] queryInstalledUsers(int[] users, boolean installed) {
        int user;
        int i = 0;
        int num = 0;
        for (int user2 : users) {
            if (getInstalled(user2) == installed) {
                num++;
            }
        }
        int[] res = new int[num];
        int i2 = 0;
        num = users.length;
        while (i < num) {
            user2 = users[i];
            if (getInstalled(user2) == installed) {
                res[i2] = user2;
                i2++;
            }
            i++;
        }
        return res;
    }

    long getCeDataInode(int userId) {
        return readUserState(userId).ceDataInode;
    }

    void setCeDataInode(long ceDataInode, int userId) {
        modifyUserState(userId).ceDataInode = ceDataInode;
    }

    boolean getStopped(int userId) {
        return readUserState(userId).stopped;
    }

    void setStopped(boolean stop, int userId) {
        modifyUserState(userId).stopped = stop;
    }

    boolean getNotLaunched(int userId) {
        return readUserState(userId).notLaunched;
    }

    void setNotLaunched(boolean stop, int userId) {
        modifyUserState(userId).notLaunched = stop;
    }

    boolean getHidden(int userId) {
        return readUserState(userId).hidden;
    }

    void setHidden(boolean hidden, int userId) {
        modifyUserState(userId).hidden = hidden;
    }

    boolean getSuspended(int userId) {
        return readUserState(userId).suspended;
    }

    void setSuspended(boolean suspended, String suspendingPackage, String dialogMessage, PersistableBundle appExtras, PersistableBundle launcherExtras, int userId) {
        PackageUserState existingUserState = modifyUserState(userId);
        existingUserState.suspended = suspended;
        PersistableBundle persistableBundle = null;
        existingUserState.suspendingPackage = suspended ? suspendingPackage : null;
        existingUserState.dialogMessage = suspended ? dialogMessage : null;
        existingUserState.suspendedAppExtras = suspended ? appExtras : null;
        if (suspended) {
            persistableBundle = launcherExtras;
        }
        existingUserState.suspendedLauncherExtras = persistableBundle;
    }

    public boolean getInstantApp(int userId) {
        return readUserState(userId).instantApp;
    }

    void setInstantApp(boolean instantApp, int userId) {
        modifyUserState(userId).instantApp = instantApp;
    }

    boolean getVirtulalPreload(int userId) {
        return readUserState(userId).virtualPreload;
    }

    void setVirtualPreload(boolean virtualPreload, int userId) {
        modifyUserState(userId).virtualPreload = virtualPreload;
    }

    void setUserState(int userId, long ceDataInode, int enabled, boolean installed, boolean stopped, boolean notLaunched, boolean hidden, boolean suspended, String suspendingPackage, String dialogMessage, PersistableBundle suspendedAppExtras, PersistableBundle suspendedLauncherExtras, boolean instantApp, boolean virtualPreload, String lastDisableAppCaller, ArraySet<String> enabledComponents, ArraySet<String> disabledComponents, int domainVerifState, int linkGeneration, int installReason, String harmfulAppWarning) {
        PackageUserState state = modifyUserState(userId);
        state.ceDataInode = ceDataInode;
        state.enabled = enabled;
        state.installed = installed;
        state.stopped = stopped;
        state.notLaunched = notLaunched;
        state.hidden = hidden;
        state.suspended = suspended;
        state.suspendingPackage = suspendingPackage;
        state.dialogMessage = dialogMessage;
        state.suspendedAppExtras = suspendedAppExtras;
        state.suspendedLauncherExtras = suspendedLauncherExtras;
        state.lastDisableAppCaller = lastDisableAppCaller;
        state.enabledComponents = enabledComponents;
        state.disabledComponents = disabledComponents;
        state.domainVerificationStatus = domainVerifState;
        state.appLinkGeneration = linkGeneration;
        state.installReason = installReason;
        state.instantApp = instantApp;
        state.virtualPreload = virtualPreload;
        state.harmfulAppWarning = harmfulAppWarning;
    }

    ArraySet<String> getEnabledComponents(int userId) {
        return readUserState(userId).enabledComponents;
    }

    ArraySet<String> getDisabledComponents(int userId) {
        return readUserState(userId).disabledComponents;
    }

    void setEnabledComponents(ArraySet<String> components, int userId) {
        modifyUserState(userId).enabledComponents = components;
    }

    void setDisabledComponents(ArraySet<String> components, int userId) {
        modifyUserState(userId).disabledComponents = components;
    }

    void setEnabledComponentsCopy(ArraySet<String> components, int userId) {
        modifyUserState(userId).enabledComponents = components != null ? new ArraySet(components) : null;
    }

    void setDisabledComponentsCopy(ArraySet<String> components, int userId) {
        modifyUserState(userId).disabledComponents = components != null ? new ArraySet(components) : null;
    }

    PackageUserState modifyUserStateComponents(int userId, boolean disabled, boolean enabled) {
        PackageUserState state = modifyUserState(userId);
        if (disabled && state.disabledComponents == null) {
            state.disabledComponents = new ArraySet(1);
        }
        if (enabled && state.enabledComponents == null) {
            state.enabledComponents = new ArraySet(1);
        }
        return state;
    }

    void addDisabledComponent(String componentClassName, int userId) {
        modifyUserStateComponents(userId, true, false).disabledComponents.add(componentClassName);
    }

    void addEnabledComponent(String componentClassName, int userId) {
        modifyUserStateComponents(userId, false, true).enabledComponents.add(componentClassName);
    }

    boolean enableComponentLPw(String componentClassName, int userId) {
        boolean changed = false;
        PackageUserState state = modifyUserStateComponents(userId, false, true);
        if (state.disabledComponents != null) {
            changed = state.disabledComponents.remove(componentClassName);
        }
        return changed | state.enabledComponents.add(componentClassName);
    }

    boolean disableComponentLPw(String componentClassName, int userId) {
        boolean changed = false;
        PackageUserState state = modifyUserStateComponents(userId, true, false);
        if (state.enabledComponents != null) {
            changed = state.enabledComponents.remove(componentClassName);
        }
        return changed | state.disabledComponents.add(componentClassName);
    }

    boolean restoreComponentLPw(String componentClassName, int userId) {
        PackageUserState state = modifyUserStateComponents(userId, true, true);
        int i = 0;
        boolean changed = state.disabledComponents != null ? state.disabledComponents.remove(componentClassName) : false;
        if (state.enabledComponents != null) {
            i = state.enabledComponents.remove(componentClassName);
        }
        return changed | i;
    }

    int getCurrentEnabledStateLPr(String componentName, int userId) {
        PackageUserState state = readUserState(userId);
        if (state.enabledComponents != null && state.enabledComponents.contains(componentName)) {
            return 1;
        }
        if (state.disabledComponents == null || !state.disabledComponents.contains(componentName)) {
            return 0;
        }
        return 2;
    }

    void removeUser(int userId) {
        this.userState.delete(userId);
    }

    public int[] getNotInstalledUserIds() {
        int userStateCount = this.userState.size();
        int i = 0;
        int count = 0;
        for (int i2 = 0; i2 < userStateCount; i2++) {
            if (!((PackageUserState) this.userState.valueAt(i2)).installed) {
                count++;
            }
        }
        if (count == 0) {
            return EMPTY_INT_ARRAY;
        }
        int[] excludedUserIds = new int[count];
        int idx = 0;
        while (i < userStateCount) {
            if (!((PackageUserState) this.userState.valueAt(i)).installed) {
                int idx2 = idx + 1;
                excludedUserIds[idx] = this.userState.keyAt(i);
                idx = idx2;
            }
            i++;
        }
        return excludedUserIds;
    }

    IntentFilterVerificationInfo getIntentFilterVerificationInfo() {
        return this.verificationInfo;
    }

    void setIntentFilterVerificationInfo(IntentFilterVerificationInfo info) {
        this.verificationInfo = info;
    }

    long getDomainVerificationStatusForUser(int userId) {
        PackageUserState state = readUserState(userId);
        return ((long) state.appLinkGeneration) | (((long) state.domainVerificationStatus) << 32);
    }

    void setDomainVerificationStatusForUser(int status, int generation, int userId) {
        PackageUserState state = modifyUserState(userId);
        state.domainVerificationStatus = status;
        if (status == 2) {
            state.appLinkGeneration = generation;
        }
    }

    void clearDomainVerificationStatusForUser(int userId) {
        modifyUserState(userId).domainVerificationStatus = 0;
    }

    protected void writeUsersInfoToProto(ProtoOutputStream proto, long fieldId) {
        int count = this.userState.size();
        for (int i = 0; i < count; i++) {
            int installType;
            long userToken = proto.start(fieldId);
            PackageUserState state = (PackageUserState) this.userState.valueAt(i);
            proto.write(1120986464257L, this.userState.keyAt(i));
            if (state.instantApp) {
                installType = 2;
            } else if (state.installed) {
                installType = 1;
            } else {
                installType = 0;
            }
            proto.write(1159641169922L, installType);
            proto.write(1133871366147L, state.hidden);
            proto.write(1133871366148L, state.suspended);
            if (state.suspended) {
                proto.write(1138166333449L, state.suspendingPackage);
            }
            proto.write(1133871366149L, state.stopped);
            proto.write(1133871366150L, state.notLaunched ^ 1);
            proto.write(1159641169927L, state.enabled);
            proto.write(1138166333448L, state.lastDisableAppCaller);
            proto.end(userToken);
        }
    }

    void setHarmfulAppWarning(int userId, String harmfulAppWarning) {
        modifyUserState(userId).harmfulAppWarning = harmfulAppWarning;
    }

    String getHarmfulAppWarning(int userId) {
        return readUserState(userId).harmfulAppWarning;
    }

    void setAppUseNotchMode(int mode) {
        this.appUseNotchMode = mode;
    }

    int getAppUseNotchMode() {
        return this.appUseNotchMode;
    }
}
