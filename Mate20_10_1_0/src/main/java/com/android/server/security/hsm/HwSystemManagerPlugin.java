package com.android.server.security.hsm;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Slog;
import com.android.server.security.core.IHwSecurityPlugin;
import huawei.android.security.IHwSystemManagerPlugin;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HwSystemManagerPlugin extends IHwSystemManagerPlugin.Stub implements IHwSecurityPlugin {
    public static final IHwSecurityPlugin.Creator CREATOR = new IHwSecurityPlugin.Creator() {
        /* class com.android.server.security.hsm.HwSystemManagerPlugin.AnonymousClass1 */

        @Override // com.android.server.security.core.IHwSecurityPlugin.Creator
        public IHwSecurityPlugin createPlugin(Context context) {
            HwSystemManagerPlugin access$100;
            synchronized (HwSystemManagerPlugin.serviceLock) {
                if (HwSystemManagerPlugin.sInstance == null) {
                    HwSystemManagerPlugin unused = HwSystemManagerPlugin.sInstance = new HwSystemManagerPlugin(context);
                }
                access$100 = HwSystemManagerPlugin.sInstance;
            }
            return access$100;
        }

        @Override // com.android.server.security.core.IHwSecurityPlugin.Creator
        public String getPluginPermission() {
            return null;
        }
    };
    private static final String PERMISSION = "com.huawei.permission.HWSYSTEMMANAGER_PLUGIN";
    private static final int RET_FAIL = 1;
    private static final int RET_SUCCESS = 0;
    private static final String TAG = "HwSystemManagerPlugin";
    /* access modifiers changed from: private */
    public static volatile HwSystemManagerPlugin sInstance;
    private static final Set<String> sStartComponetBlackList = new HashSet();
    /* access modifiers changed from: private */
    public static final Object serviceLock = new Object();
    private Context mContext;

    private HwSystemManagerPlugin(Context context) {
        this.mContext = context;
    }

    public static HwSystemManagerPlugin getInstance(Context context) {
        HwSystemManagerPlugin hwSystemManagerPlugin;
        synchronized (serviceLock) {
            if (sInstance == null) {
                sInstance = new HwSystemManagerPlugin(context);
            }
            hwSystemManagerPlugin = sInstance;
        }
        return hwSystemManagerPlugin;
    }

    @Override // com.android.server.security.core.IHwSecurityPlugin
    public void onStart() {
        Slog.d(TAG, "HwAddViewChecker - onStart");
    }

    @Override // com.android.server.security.core.IHwSecurityPlugin
    public void onStop() {
        Slog.d(TAG, "HwAddViewChecker - onStop");
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: com.android.server.security.hsm.HwSystemManagerPlugin */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.server.security.core.IHwSecurityPlugin
    public IBinder asBinder() {
        return this;
    }

    public boolean shouldPreventStartComponent(int type, String calleePackage, int callerUid, int callerPid, String callerPackage, int userId) {
        checkPermission(PERMISSION);
        synchronized (sStartComponetBlackList) {
            if (callerPackage == null) {
                try {
                    return false;
                } catch (Throwable th) {
                    throw th;
                }
            } else {
                if (type != 1) {
                    if (type != 3) {
                        if (!sStartComponetBlackList.contains(callerPackage) || callerPackage.equals(calleePackage)) {
                            return false;
                        }
                        return true;
                    }
                }
                return false;
            }
        }
    }

    public int updateAddViewData(Bundle data, int operation) {
        checkPermission(PERMISSION);
        return HwAddViewManager.getInstance(this.mContext).updateAddViewData(data, operation);
    }

    public void setStartComponetBlackList(List<String> pkgs) {
        checkPermission(PERMISSION);
        synchronized (sStartComponetBlackList) {
            sStartComponetBlackList.clear();
            if (pkgs != null) {
                if (!pkgs.isEmpty()) {
                    sStartComponetBlackList.addAll(pkgs);
                }
            }
        }
    }

    private void checkPermission(String permission) {
        Context context = this.mContext;
        context.enforceCallingOrSelfPermission(permission, "Must have " + permission + " permission.");
    }
}
