package com.android.server.security.privacyability;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import com.android.server.security.core.IHwSecurityPlugin;
import com.huawei.hiai.awareness.AwarenessInnerConstants;
import huawei.android.security.privacyability.IIDAnonymizationManager;
import java.util.List;

public class IDAnonymizationManagerService extends IIDAnonymizationManager.Stub implements IHwSecurityPlugin {
    public static final Creator CREATOR = new Creator() {
        /* class com.android.server.security.privacyability.IDAnonymizationManagerService.AnonymousClass1 */

        @Override // com.android.server.security.core.IHwSecurityPlugin.Creator
        public IHwSecurityPlugin createPlugin(Context context) {
            Log.d(IDAnonymizationManagerService.TAG, "Create IDAnonymizationManagerService");
            return new IDAnonymizationManagerService(context);
        }

        @Override // com.android.server.security.core.IHwSecurityPlugin.Creator
        public String getPluginPermission() {
            return null;
        }
    };
    private static final String ID_ANONYMIZATION_MANAGER_PERMISSION = "huawei.android.permission.HW_SIGNATURE_OR_SYSTEM";
    private static final boolean IS_ID_ANONYMIZATION_MANAGER = "true".equalsIgnoreCase(SystemProperties.get("ro.product.id_anonmyzation_manager", "true"));
    private static final String TAG = "IDAnonymizationManagerService";
    private Context mContext;
    private IDAnonymizationBroadcastReceiver mIDAnonymizationBroadcastReceiver;

    private IDAnonymizationManagerService(Context context) {
        this.mContext = context;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: com.android.server.security.privacyability.IDAnonymizationManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.server.security.core.IHwSecurityPlugin
    public IBinder asBinder() {
        this.mContext.enforceCallingOrSelfPermission(ID_ANONYMIZATION_MANAGER_PERMISSION, "does not hava ID anonymization manager permission");
        return this;
    }

    @Override // com.android.server.security.core.IHwSecurityPlugin
    public void onStart() {
        if (this.mIDAnonymizationBroadcastReceiver == null) {
            this.mIDAnonymizationBroadcastReceiver = new IDAnonymizationBroadcastReceiver(this.mContext);
        }
        this.mIDAnonymizationBroadcastReceiver.onStart();
    }

    @Override // com.android.server.security.core.IHwSecurityPlugin
    public void onStop() {
        IDAnonymizationBroadcastReceiver iDAnonymizationBroadcastReceiver = this.mIDAnonymizationBroadcastReceiver;
        if (iDAnonymizationBroadcastReceiver != null) {
            iDAnonymizationBroadcastReceiver.onStop();
        }
    }

    public static boolean isNeedRegisterService() {
        return IS_ID_ANONYMIZATION_MANAGER;
    }

    private List<ActivityManager.RunningAppProcessInfo> getRunningProcesses() {
        ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG);
        if (activityManager != null) {
            return activityManager.getRunningAppProcesses();
        }
        Log.e(TAG, "get process status, get ams service failed");
        return null;
    }

    private String getAppNameByPid(int pid) {
        List<ActivityManager.RunningAppProcessInfo> processes = getRunningProcesses();
        if (processes == null) {
            Log.e(TAG, "get app name, get running process failed");
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo processInfo : processes) {
            if (processInfo.pid == pid) {
                return processInfo.processName;
            }
        }
        return null;
    }

    private String getCallingPackageName() throws SecurityException {
        String packageName = getAppNameByPid(Binder.getCallingPid());
        if (packageName != null) {
            String[] packageParts = packageName.split(AwarenessInnerConstants.COLON_KEY);
            return packageParts.length > 0 ? packageParts[0] : packageName;
        }
        throw new SecurityException("get calling package name failed");
    }

    private int getCallingUserId() {
        return UserHandle.getUserId(Binder.getCallingUid());
    }

    public String getCUID() {
        this.mContext.enforceCallingOrSelfPermission(ID_ANONYMIZATION_MANAGER_PERMISSION, "does not hava ID anonymization manager permission");
        return IDAnonymizationDB.getInstance().getCUID(getCallingUserId(), getCallingPackageName());
    }

    public String getCFID(String containerID, String contentProviderTag) {
        this.mContext.enforceCallingOrSelfPermission(ID_ANONYMIZATION_MANAGER_PERMISSION, "does not hava ID anonymization manager permission");
        return ContainerForwardID.getInstance().generateID(containerID, contentProviderTag);
    }

    public int resetCUID() {
        this.mContext.enforceCallingOrSelfPermission(ID_ANONYMIZATION_MANAGER_PERMISSION, "does not hava ID anonymization manager permission");
        return IDAnonymizationDB.getInstance().removeCUID(getCallingUserId(), getCallingPackageName());
    }
}
