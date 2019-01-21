package com.android.server.security.hsm;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Slog;
import com.android.server.security.core.IHwSecurityPlugin;
import com.android.server.security.core.IHwSecurityPlugin.Creator;
import huawei.android.security.IHwSystemManagerPlugin.Stub;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HwSystemManagerPlugin extends Stub implements IHwSecurityPlugin {
    public static final Creator CREATOR = new Creator() {
        public IHwSecurityPlugin createPlugin(Context context) {
            HwSystemManagerPlugin access$100;
            synchronized (HwSystemManagerPlugin.serviceLock) {
                if (HwSystemManagerPlugin.sInstance == null) {
                    HwSystemManagerPlugin.sInstance = new HwSystemManagerPlugin(context, null);
                }
                access$100 = HwSystemManagerPlugin.sInstance;
            }
            return access$100;
        }

        public String getPluginPermission() {
            return null;
        }
    };
    private static final String PERMISSION = "com.huawei.permission.HWSYSTEMMANAGER_PLUGIN";
    private static final int RET_FAIL = 1;
    private static final int RET_SUCCESS = 0;
    private static final String TAG = "HwSystemManagerPlugin";
    private static volatile HwSystemManagerPlugin sInstance;
    private static final Set<String> sStartComponetBlackList = new HashSet();
    private static final Object serviceLock = new Object();
    private Context mContext;

    /* synthetic */ HwSystemManagerPlugin(Context x0, AnonymousClass1 x1) {
        this(x0);
    }

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

    public void onStart() {
        Slog.d(TAG, "HwAddViewChecker - onStart");
    }

    public void onStop() {
        Slog.d(TAG, "HwAddViewChecker - onStop");
    }

    public IBinder asBinder() {
        return this;
    }

    /* JADX WARNING: Missing block: B:21:0x0027, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:23:0x0029, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean shouldPreventStartComponent(int type, String calleePackage, int callerUid, int callerPid, String callerPackage, int userId) {
        checkPermission(PERMISSION);
        synchronized (sStartComponetBlackList) {
            if (callerPackage == null) {
                try {
                    return false;
                } catch (Throwable th) {
                }
            } else if (type != 1) {
                if (type != 3) {
                    if (!sStartComponetBlackList.contains(callerPackage) || callerPackage.equals(calleePackage)) {
                    } else {
                        return true;
                    }
                }
            }
        }
    }

    public int updateAddViewData(Bundle data, int operation) {
        checkPermission(PERMISSION);
        return HwAddViewManager.getInstance(this.mContext).updateAddViewData(data, operation);
    }

    /* JADX WARNING: Missing block: B:12:0x001e, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Must have ");
        stringBuilder.append(permission);
        stringBuilder.append(" permission.");
        context.enforceCallingOrSelfPermission(permission, stringBuilder.toString());
    }
}
