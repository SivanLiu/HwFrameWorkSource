package com.android.server.devicepolicy;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArraySet;
import android.view.inputmethod.InputMethodInfo;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.internal.view.IInputMethodManager;
import com.android.internal.view.IInputMethodManager.Stub;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class OverlayPackagesProvider {
    protected static final String TAG = "OverlayPackagesProvider";
    private final Context mContext;
    private final IInputMethodManager mIInputMethodManager;
    private final PackageManager mPm;

    public OverlayPackagesProvider(Context context) {
        this(context, getIInputMethodManager());
    }

    @VisibleForTesting
    OverlayPackagesProvider(Context context, IInputMethodManager iInputMethodManager) {
        this.mContext = context;
        this.mPm = (PackageManager) Preconditions.checkNotNull(context.getPackageManager());
        this.mIInputMethodManager = (IInputMethodManager) Preconditions.checkNotNull(iInputMethodManager);
    }

    public Set<String> getNonRequiredApps(ComponentName admin, int userId, String provisioningAction) {
        Set<String> nonRequiredApps = getLaunchableApps(userId);
        nonRequiredApps.removeAll(getRequiredApps(provisioningAction, admin.getPackageName()));
        if ("android.app.action.PROVISION_MANAGED_DEVICE".equals(provisioningAction) || "android.app.action.PROVISION_MANAGED_USER".equals(provisioningAction)) {
            nonRequiredApps.removeAll(getSystemInputMethods());
        }
        nonRequiredApps.addAll(getDisallowedApps(provisioningAction));
        return nonRequiredApps;
    }

    private Set<String> getLaunchableApps(int userId) {
        Intent launcherIntent = new Intent("android.intent.action.MAIN");
        launcherIntent.addCategory("android.intent.category.LAUNCHER");
        List<ResolveInfo> resolveInfos = this.mPm.queryIntentActivitiesAsUser(launcherIntent, 795136, userId);
        Set<String> apps = new ArraySet();
        for (ResolveInfo resolveInfo : resolveInfos) {
            apps.add(resolveInfo.activityInfo.packageName);
        }
        return apps;
    }

    private Set<String> getSystemInputMethods() {
        try {
            List<InputMethodInfo> inputMethods = this.mIInputMethodManager.getInputMethodList();
            Set<String> systemInputMethods = new ArraySet();
            for (InputMethodInfo inputMethodInfo : inputMethods) {
                if (inputMethodInfo.getServiceInfo().applicationInfo.isSystemApp()) {
                    systemInputMethods.add(inputMethodInfo.getPackageName());
                }
            }
            return systemInputMethods;
        } catch (RemoteException e) {
            return null;
        }
    }

    private Set<String> getRequiredApps(String provisioningAction, String dpcPackageName) {
        Set<String> requiredApps = new ArraySet();
        requiredApps.addAll(getRequiredAppsSet(provisioningAction));
        requiredApps.addAll(getVendorRequiredAppsSet(provisioningAction));
        requiredApps.add(dpcPackageName);
        return requiredApps;
    }

    private Set<String> getDisallowedApps(String provisioningAction) {
        Set<String> disallowedApps = new ArraySet();
        disallowedApps.addAll(getDisallowedAppsSet(provisioningAction));
        disallowedApps.addAll(getVendorDisallowedAppsSet(provisioningAction));
        return disallowedApps;
    }

    private static IInputMethodManager getIInputMethodManager() {
        return Stub.asInterface(ServiceManager.getService("input_method"));
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x0036  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x005a  */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0056  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0052  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0036  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x005a  */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0056  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0052  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0036  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x005a  */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0056  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0052  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Set<String> getRequiredAppsSet(String provisioningAction) {
        Object obj;
        int hashCode = provisioningAction.hashCode();
        if (hashCode != -920528692) {
            if (hashCode != -514404415) {
                if (hashCode == -340845101 && provisioningAction.equals("android.app.action.PROVISION_MANAGED_PROFILE")) {
                    obj = 1;
                    switch (obj) {
                        case null:
                            hashCode = 17236073;
                            break;
                        case 1:
                            hashCode = 17236072;
                            break;
                        case 2:
                            hashCode = 17236071;
                            break;
                        default:
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Provisioning type ");
                            stringBuilder.append(provisioningAction);
                            stringBuilder.append(" not supported.");
                            throw new IllegalArgumentException(stringBuilder.toString());
                    }
                    return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(hashCode)));
                }
            } else if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_USER")) {
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
                return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(hashCode)));
            }
        } else if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_DEVICE")) {
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
            return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(hashCode)));
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
        return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(hashCode)));
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x0036  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x005a  */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0056  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0052  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0036  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x005a  */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0056  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0052  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0036  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x005a  */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0056  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0052  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Set<String> getDisallowedAppsSet(String provisioningAction) {
        Object obj;
        int hashCode = provisioningAction.hashCode();
        if (hashCode != -920528692) {
            if (hashCode != -514404415) {
                if (hashCode == -340845101 && provisioningAction.equals("android.app.action.PROVISION_MANAGED_PROFILE")) {
                    obj = 1;
                    switch (obj) {
                        case null:
                            hashCode = 17236057;
                            break;
                        case 1:
                            hashCode = 17236056;
                            break;
                        case 2:
                            hashCode = 17236055;
                            break;
                        default:
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Provisioning type ");
                            stringBuilder.append(provisioningAction);
                            stringBuilder.append(" not supported.");
                            throw new IllegalArgumentException(stringBuilder.toString());
                    }
                    return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(hashCode)));
                }
            } else if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_USER")) {
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
                return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(hashCode)));
            }
        } else if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_DEVICE")) {
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
            return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(hashCode)));
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
        return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(hashCode)));
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x0036  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x005a  */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0056  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0052  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0036  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x005a  */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0056  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0052  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0036  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x005a  */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0056  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0052  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Set<String> getVendorRequiredAppsSet(String provisioningAction) {
        Object obj;
        int hashCode = provisioningAction.hashCode();
        if (hashCode != -920528692) {
            if (hashCode != -514404415) {
                if (hashCode == -340845101 && provisioningAction.equals("android.app.action.PROVISION_MANAGED_PROFILE")) {
                    obj = 1;
                    switch (obj) {
                        case null:
                            hashCode = 17236089;
                            break;
                        case 1:
                            hashCode = 17236088;
                            break;
                        case 2:
                            hashCode = 17236087;
                            break;
                        default:
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Provisioning type ");
                            stringBuilder.append(provisioningAction);
                            stringBuilder.append(" not supported.");
                            throw new IllegalArgumentException(stringBuilder.toString());
                    }
                    return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(hashCode)));
                }
            } else if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_USER")) {
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
                return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(hashCode)));
            }
        } else if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_DEVICE")) {
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
            return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(hashCode)));
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
        return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(hashCode)));
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x0036  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x005a  */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0056  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0052  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0036  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x005a  */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0056  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0052  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0036  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x005a  */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0056  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0052  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Set<String> getVendorDisallowedAppsSet(String provisioningAction) {
        Object obj;
        int hashCode = provisioningAction.hashCode();
        if (hashCode != -920528692) {
            if (hashCode != -514404415) {
                if (hashCode == -340845101 && provisioningAction.equals("android.app.action.PROVISION_MANAGED_PROFILE")) {
                    obj = 1;
                    switch (obj) {
                        case null:
                            hashCode = 17236086;
                            break;
                        case 1:
                            hashCode = 17236085;
                            break;
                        case 2:
                            hashCode = 17236084;
                            break;
                        default:
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Provisioning type ");
                            stringBuilder.append(provisioningAction);
                            stringBuilder.append(" not supported.");
                            throw new IllegalArgumentException(stringBuilder.toString());
                    }
                    return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(hashCode)));
                }
            } else if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_USER")) {
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
                return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(hashCode)));
            }
        } else if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_DEVICE")) {
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
            return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(hashCode)));
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
        return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(hashCode)));
    }
}
