package com.android.server.security.permissionmanager.packageinstaller;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.hwsystemmanager.HsmSecurityProxy;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.LongSparseArray;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.mtm.iaware.appmng.appstart.datamgr.AppStartupDataMgr;
import com.android.server.security.hsm.HwAddViewHelper;
import com.android.server.security.permissionmanager.HwPermDbAdapter;
import com.android.server.security.permissionmanager.sms.smsutils.Utils;
import com.android.server.security.permissionmanager.util.PermissionClass;
import com.android.server.security.permissionmanager.util.PermissionType;
import com.android.server.wm.HwActivityStartInterceptor;
import com.huawei.permission.cloud.PackageInstallerPermissionInfo;
import com.huawei.securitycenter.IHwSecService;
import com.huawei.securitycenter.SecCenterServiceHolder;
import com.huawei.securitycenter.permission.ui.model.DbPermissionItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PackageInstallerPermissionHelper {
    private static final String ACCESS_BROWSER_RECORDS = "ACCESS_BROWSER_RECORDS";
    private static final String ADDVIEW = "ADDVIEW";
    private static final int APPLY_PERMISSION = 1;
    private static final String BROWSER_HISTORY_ICON_KEY = "browser_history_icon";
    private static final String CALL_FORWARD = "CALL_FORWARD";
    private static final int DEFAULT_SIZE = 16;
    private static final int FLAGS_PERMISSION_WHITELIST_ALL = 7;
    private static final String GROUP_PHONE = "android.permission-group.PHONE";
    private static final String GROUP_SMS = "android.permission-group.SMS";
    private static final long ILLEGAL_PERM_TYPE = 0;
    private static final int ILLEGAL_VALUE = -1;
    private static final boolean IS_SUPPORT_BROWSER_HISTORY = SystemProperties.getBoolean("ro.config.browser_history_perm", false);
    private static final String KEY_GRANT_PERMISSION_LIST = "grant_permission";
    private static final String KEY_PERMISSION_LIST = "hwPermissionList";
    private static final String KEY_PKG_NAME = "packageName";
    private static final String KEY_UID = "uid";
    private static final int OP_REQUEST_INSTALL_PACKAGES = 66;
    private static final int OP_SYSTEM_ALERT_WINDOW = 24;
    private static final int PERMISSION_CONTAINER_SIZE = 6;
    private static final int PERMISSION_TYPE_ALLOWED = 0;
    private static final int PERMISSION_TYPE_BLOCKED = 2;
    private static final int PERM_RESTRICTED = -1;
    private static final int PERM_UNRESTRICTED = 0;
    private static final String QUERY_BROWSER_HISTORY_ICON_METHOD = "method_browser_history_icon";
    private static final String REQUEST_INSTALL_PACKAGES = "REQUEST_INSTALL_PACKAGES";
    private static final String SEND_MMS = "SEND_MMS";
    private static final String SHORTCUT = "SHORTCUT";
    private static final String TAG = "PackageInstallerPermissionHelper";
    private static LongSparseArray<Integer> sIconIdMap = new LongSparseArray<>(6);
    private static SparseArray<Integer> sPackageInstallerConstMap = new SparseArray<>(6);
    private static ArrayMap<String, Long> sPermNameToTypeMap = new ArrayMap<>(6);
    private static ArrayMap<Long, String> sPermTypeToNameMap = new ArrayMap<>(6);
    private static LongSparseArray<String> sPermissionGroupMap = new LongSparseArray<>(6);
    private static LongSparseArray<Integer> sStringIdMap = new LongSparseArray<>(6);

    static {
        ArrayMap<String, Long> arrayMap = sPermNameToTypeMap;
        Long valueOf = Long.valueOf((long) PermissionType.EDIT_SHORTCUT);
        arrayMap.put(SHORTCUT, valueOf);
        ArrayMap<String, Long> arrayMap2 = sPermNameToTypeMap;
        Long valueOf2 = Long.valueOf((long) PermissionType.SYSTEM_ALERT_WINDOW);
        arrayMap2.put(ADDVIEW, valueOf2);
        sPermNameToTypeMap.put(CALL_FORWARD, 1048576L);
        ArrayMap<String, Long> arrayMap3 = sPermNameToTypeMap;
        Long valueOf3 = Long.valueOf((long) PermissionType.SEND_MMS);
        arrayMap3.put(SEND_MMS, valueOf3);
        ArrayMap<String, Long> arrayMap4 = sPermNameToTypeMap;
        Long valueOf4 = Long.valueOf((long) PermissionType.REQUEST_INSTALL_PACKAGES);
        arrayMap4.put(REQUEST_INSTALL_PACKAGES, valueOf4);
        if (IS_SUPPORT_BROWSER_HISTORY) {
            sPermNameToTypeMap.put(ACCESS_BROWSER_RECORDS, 1073741824L);
        }
        sPermTypeToNameMap.put(valueOf, SHORTCUT);
        sPermTypeToNameMap.put(valueOf2, ADDVIEW);
        sPermTypeToNameMap.put(1048576L, CALL_FORWARD);
        sPermTypeToNameMap.put(valueOf3, SEND_MMS);
        sPermTypeToNameMap.put(valueOf4, REQUEST_INSTALL_PACKAGES);
        if (IS_SUPPORT_BROWSER_HISTORY) {
            sPermTypeToNameMap.put(1073741824L, ACCESS_BROWSER_RECORDS);
        }
        sStringIdMap.put(PermissionType.EDIT_SHORTCUT, 33685668);
        sStringIdMap.put(PermissionType.SYSTEM_ALERT_WINDOW, 33685662);
        sStringIdMap.put(1048576, 33686128);
        sStringIdMap.put(PermissionType.SEND_MMS, 33686188);
        sStringIdMap.put(PermissionType.REQUEST_INSTALL_PACKAGES, 33685796);
        if (IS_SUPPORT_BROWSER_HISTORY) {
            sStringIdMap.put(1073741824, 33686127);
        }
        sIconIdMap.put(PermissionType.EDIT_SHORTCUT, 33752035);
        sIconIdMap.put(PermissionType.SYSTEM_ALERT_WINDOW, 33752034);
        sIconIdMap.put(PermissionType.REQUEST_INSTALL_PACKAGES, 33752033);
        sPermissionGroupMap.put(1048576, GROUP_PHONE);
        sPermissionGroupMap.put(PermissionType.SEND_MMS, GROUP_SMS);
        sPackageInstallerConstMap.put(0, 1);
        sPackageInstallerConstMap.put(2, 2);
    }

    public Bundle getHwPermInfo(Context context, String pkgName, Bundle params) {
        Bundle result = new Bundle();
        if (context == null) {
            return result;
        }
        int uid = params.getInt("uid", -1);
        Slog.i(TAG, "getHwPermInfoForPackageInstaller: pkgName =" + pkgName + ", uid: " + uid);
        ArrayList<PackageInstallerPermissionInfo> permissionList = getPermissionList(context, pkgName, uid);
        result.putString("packageName", pkgName);
        result.putParcelableArrayList(KEY_PERMISSION_LIST, permissionList);
        Slog.i(TAG, " Send permissions info:" + permissionList);
        return result;
    }

    private ArrayList<PackageInstallerPermissionInfo> getPermissionList(Context context, String pkgName, int uid) {
        boolean isRestricted;
        ArrayList<PackageInstallerPermissionInfo> result = new ArrayList<>(16);
        if (!Utils.IS_CHINA) {
            return result;
        }
        HsmSecurityProxy.MaliciousAppInfo maliciousAppInfo = HsmSecurityProxy.getMaliciousAppInfo(pkgName, 1);
        if (maliciousAppInfo != null) {
            isRestricted = maliciousAppInfo.isRestricted;
        } else {
            isRestricted = false;
        }
        int userId = UserHandle.getUserId(uid);
        updateCloneAppInfo(context, pkgName, userId, uid);
        for (Long l : sPermTypeToNameMap.keySet()) {
            long permissionType = l.longValue();
            int permissionStatus = getPermissionStatus(context, userId, uid, pkgName, permissionType);
            if (permissionStatus == -1) {
                Slog.i(TAG, "getPermissionList not permission type = " + permissionType);
            } else {
                PackageInstallerPermissionInfo permissionInfo = new PackageInstallerPermissionInfo();
                permissionInfo.setStatus(permissionStatus);
                permissionInfo.setName(sPermTypeToNameMap.get(Long.valueOf(permissionType)));
                int restricted = -1;
                permissionInfo.setLabelRes(sStringIdMap.get(permissionType, -1).intValue());
                if (permissionInfo.getLabelRes() == -1) {
                    Slog.e(TAG, "Res id not found for permission: " + permissionType);
                } else {
                    boolean isForbidden = isForbiddenPermMode(permissionType);
                    if (!isRestricted || !isForbidden) {
                        restricted = 0;
                    }
                    permissionInfo.setIsRestricted(restricted);
                    permissionInfo.setPermissionGroup(sPermissionGroupMap.get(permissionType));
                    permissionInfo.setIconRes(sIconIdMap.get(permissionType, 0).intValue());
                    permissionInfo.setResPackage(AppStartupDataMgr.HWPUSH_PKGNAME);
                    if (permissionType == 1073741824) {
                        permissionInfo.setIconRes(getBrowserHistoryIcon());
                        permissionInfo.setResPackage(HwActivityStartInterceptor.ACTION_CONFIRM_APPLOCK_PACKAGENAME);
                    }
                    result.add(permissionInfo);
                }
            }
        }
        return result;
    }

    private int transferPackageInstallerConst(int packageInstallerConst) {
        Integer constInteger = sPackageInstallerConstMap.get(packageInstallerConst);
        if (constInteger == null) {
            return -1;
        }
        return constInteger.intValue();
    }

    private int getPermissionStatus(Context context, int userId, int uid, String pkgName, long permissionType) {
        int permissionStatus = -1;
        if (PermissionClass.isClassEType(permissionType)) {
            if (permissionType == PermissionType.SEND_MMS) {
                PackageManager pm = context.getPackageManager();
                if (pm == null) {
                    return -1;
                }
                try {
                    PermissionInfo permissionInfo = pm.getPermissionInfo("android.permission.SEND_SMS", 0);
                    Set<String> whitePermissions = pm.getWhitelistedRestrictedPermissions(pkgName, 7);
                    Slog.i(TAG, pkgName + " , flag:" + permissionInfo.flags + " ,permInfo pkgName: " + permissionInfo.packageName);
                    if ((permissionInfo.flags & 4) != 0 && !whitePermissions.contains("android.permission.SEND_SMS")) {
                        Slog.i(TAG, pkgName + " is restricted");
                        return -1;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.e(TAG, "getPermissionStatus, NameNotFoundException");
                    return -1;
                }
            }
            int state = HwPermDbAdapter.getInstance(context).checkHwPerm(pkgName, permissionType, userId);
            if (state == 1) {
                permissionStatus = 0;
            } else if (state == 2) {
                permissionStatus = 2;
            } else {
                Slog.i(TAG, "pkgName: " + pkgName + " don't have permission, type: " + permissionType + ", state: " + state);
            }
            return permissionStatus;
        } else if ((PermissionType.SYSTEM_ALERT_WINDOW & permissionType) != 0) {
            int permissionStatus2 = HwAddViewHelper.getInstance(context).checkAddviewPermission(pkgName, uid);
            Slog.i(TAG, "getPermissionStatus, addview = " + permissionType + ", status:" + permissionStatus2);
            return permissionStatus2;
        } else if (PermissionClass.isClassFType(permissionType)) {
            int permissionStatus3 = checkOp(context, uid, pkgName, "android:request_install_packages", permissionType);
            Slog.i(TAG, "getPermissionStatus, isClassF = " + permissionType + ", status:" + permissionStatus3);
            return permissionStatus3;
        } else {
            Slog.e(TAG, "illegal permissionType = " + permissionType);
            return -1;
        }
    }

    private void updateCloneAppInfo(Context context, String pkgName, int userId, int uid) {
        if (UserHandle.isClonedProfile(userId)) {
            Slog.i(TAG, "User isClonedProfile: " + userId);
            DbPermissionItem item = HwPermDbAdapter.getInstance(context).getHwPermInfo(pkgName, userId);
            if (item == null || item.getUid() == -1) {
                DbPermissionItem ownerDbPermissionItem = HwPermDbAdapter.getInstance(context).getHwPermInfo(pkgName, 0);
                if (ownerDbPermissionItem != null) {
                    if (ownerDbPermissionItem.getUid() != -1) {
                        long permCode = ownerDbPermissionItem.getPermissionCode();
                        HwPermDbAdapter.getInstance(context).replaceHwPermInfo(pkgName, userId, permCode, permCode, uid);
                        Slog.i(TAG, "clone pkgName update completed: " + pkgName + ", code:" + permCode + ", uid:" + uid);
                        return;
                    }
                }
                Slog.e(TAG, "ownerDbPermissionItem is null:");
                return;
            }
            Slog.i(TAG, "cache contain package:" + item.getPackageName() + ", " + item.getUid());
        }
    }

    /* JADX INFO: finally extract failed */
    public void setHwPermission(Context context, Bundle params) {
        if (params == null) {
            Slog.e(TAG, "setHwPermission: args is null");
            return;
        }
        int uid = params.getInt("uid", -1);
        String pkgName = params.getString("packageName");
        Slog.i(TAG, "setHwPermission: " + pkgName + ", uid:" + uid);
        if (!TextUtils.isEmpty(pkgName) && uid >= 0) {
            int callingUid = Binder.getCallingUid();
            long identity = Binder.clearCallingIdentity();
            try {
                setHwPermissionInner(context, pkgName, (HashMap) params.getSerializable(KEY_GRANT_PERMISSION_LIST), uid, callingUid);
                Binder.restoreCallingIdentity(identity);
                Slog.i(TAG, "end setHwPermissionForPackageInstaller");
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
                throw th;
            }
        }
    }

    private void setHwPermissionInner(Context context, String pkgName, HashMap<String, Integer> permInfoMap, int uid, int callingUid) {
        if (permInfoMap == null || permInfoMap.isEmpty()) {
            Slog.e(TAG, "setHwPermission invalid parameter!");
            return;
        }
        for (Map.Entry<String, Integer> entry : permInfoMap.entrySet()) {
            String permName = entry.getKey();
            int operation = entry.getValue().intValue();
            if (!TextUtils.isEmpty(permName)) {
                long permissionType = sPermNameToTypeMap.get(permName) == null ? 0 : sPermNameToTypeMap.get(permName).longValue();
                if (permissionType == PermissionType.SYSTEM_ALERT_WINDOW) {
                    HwAddViewHelper.getInstance(context).setAddviewPermission(pkgName, operation == 0, UserHandle.getUserId(uid), uid);
                } else if (PermissionClass.isClassEType(permissionType)) {
                    HwPermDbAdapter.getInstance(context).setHwPermission(pkgName, UserHandle.getUserId(uid), permissionType, transferPackageInstallerConst(operation), callingUid);
                } else if (PermissionClass.isClassFType(permissionType)) {
                    setOpMode(context, OP_REQUEST_INSTALL_PACKAGES, uid, pkgName, operation);
                } else {
                    Slog.e(TAG, "illegal type: " + permissionType);
                }
            }
        }
    }

    private void setOpMode(Context context, int code, int uid, String pkg, int operation) {
        try {
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService("appops");
            int mode = 2;
            if (operation == 0) {
                mode = 0;
            }
            Slog.i(TAG, "setOpMode:" + mode + ", pkg: " + pkg + ", code: " + code + ", uid: " + uid);
            appOpsManager.setMode(code, uid, pkg, mode);
        } catch (SecurityException ex) {
            Slog.e(TAG, "setOpMode securityException" + ex.getMessage());
        }
    }

    private int checkOp(Context context, int uid, String pkgName, String op, long permissionType) {
        int permCodeState = HwPermDbAdapter.getInstance(context).checkHwPermCode(pkgName, permissionType, UserHandle.getUserId(uid));
        if (permCodeState != 1) {
            Slog.i(TAG, "getPermissionStatus, isClassF = " + permCodeState);
            return -1;
        }
        int checkOpResult = ((AppOpsManager) context.getSystemService("appops")).checkOpNoThrow(op, uid, pkgName);
        Slog.i(TAG, "checkOp: " + pkgName + ", uid = " + uid + ", op = " + op);
        return checkOpResult == 0 ? 0 : 2;
    }

    private static boolean isForbiddenPermMode(long permCode) {
        return (PermissionClass.ALWAYS_FORBIDDEN_PERMS & permCode) != 0;
    }

    private int getBrowserHistoryIcon() {
        int iconId;
        IHwSecService service = SecCenterServiceHolder.getHwSecService();
        if (service == null) {
            return -1;
        }
        try {
            Bundle resultBundle = service.call(QUERY_BROWSER_HISTORY_ICON_METHOD, (Bundle) null);
            if (resultBundle == null || (iconId = resultBundle.getInt(BROWSER_HISTORY_ICON_KEY, -1)) == -1) {
                return -1;
            }
            return iconId;
        } catch (RemoteException e) {
            Slog.e(TAG, "getBrowserHistoryIcon : " + e.getMessage());
        }
    }
}
