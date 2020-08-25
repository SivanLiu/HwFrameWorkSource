package com.android.server.security;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import com.android.server.SystemService;
import com.android.server.security.antimal.HwAntiMalPlugin;
import com.android.server.security.ccmode.HwCCModePlugin;
import com.android.server.security.core.IHwSecurityPlugin;
import com.android.server.security.deviceusage.HwDeviceUsagePlugin;
import com.android.server.security.eidservice.HwEidPlugin;
import com.android.server.security.fileprotect.HwSfpService;
import com.android.server.security.hsm.HwSystemManagerPlugin;
import com.android.server.security.hwkeychain.HwKeychainService;
import com.android.server.security.permissionmanager.HwPermissionService;
import com.android.server.security.privacyability.IDAnonymizationManagerService;
import com.android.server.security.pwdprotect.PwdProtectService;
import com.android.server.security.securitycenter.SecurityCenterPluginService;
import com.android.server.security.securityprofile.SecurityProfileService;
import com.android.server.security.trustspace.TrustSpaceManagerService;
import com.android.server.wifipro.WifiProCommonUtils;
import com.huawei.server.security.behaviorcollect.HwBehaviorCollectPlugin;
import com.huawei.server.security.securitydiagnose.HwSecurityDiagnosePlugin;
import huawei.android.security.IHwSecurityService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class HwSecurityService extends SystemService {
    private static final int CCMODE_PLUGIN_ID = 9;
    private static final int DEFAULT_PLUGIN_ID = 0;
    private static final int DEFAULT_RESIDENT_PRIORITY = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int DEVICE_SECURE_DIAGNOSE_ID = 2;
    private static final int DEVICE_USAGE_PLUGIN_ID = 1;
    private static final int DYNAMIC_PLUGIN_FLAG = 0;
    private static final int FILE_PROTECT_PLUGIN_ID = 11;
    private static final int HWSYSTEMMANAGER_PLUGIN_ID = 13;
    private static final int HW_ANTIMAL_PLUGIN_ID = 16;
    private static final int HW_BEHAVIOR_AUTH_PLUGIN_ID = 22;
    private static final int HW_EID_PLUGIN_ID = 15;
    private static final int HW_KEYCHAIN_PLUGIN_ID = 20;
    private static final int HW_PERMISSION_PLUGIN_ID = 17;
    private static final int HW_SECCENTER_MANAGER_PLUGIN_ID = 18;
    private static final int ID_ANONYMIZATION_PLUGIN_ID = 21;
    private static final boolean IS_CHINA_AREA = "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    /* access modifiers changed from: private */
    public static final boolean IS_HW_DEBUG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final boolean IS_SUPPORT_CC_MODE = SystemProperties.getBoolean(PROPERTIES_CC_MODE_SUPPORTED, false);
    private static final boolean IS_SUPPORT_HW_BEHAVIOR_AUTH = SystemProperties.getBoolean("hw_mc.authentication.behavior_auth_bot", true);
    private static final boolean IS_SUPPORT_HW_DEVICE_USAGE = SystemProperties.getBoolean("ro.config.support_activationmonitor", true);
    private static final boolean IS_SUPPORT_HW_EID = SystemProperties.getBoolean("ro.config.support_eid", true);
    private static final boolean IS_SUPPORT_HW_IUDF = SystemProperties.getBoolean("ro.config.support_iudf", false);
    private static final boolean IS_SUPPORT_HW_KEYCHAIN = SystemProperties.getBoolean("ro.prop.hwkeychain_switch", false);
    private static final boolean IS_SUPPORT_HW_SEAPP = SystemProperties.getBoolean("ro.config.support_iseapp", false);
    private static final boolean IS_SUPPORT_HW_SECCENTER = SystemProperties.getBoolean("ro.config.support_securitycenter", true);
    private static final boolean IS_SUPPORT_HW_SECURE_DIAGNOSE = SystemProperties.getBoolean("ro.config.support_securitydiagnose", true);
    private static final boolean IS_SUPPORT_HW_TRUSTSPACE = SystemProperties.getBoolean("ro.config.support_trustspace", true);
    private static final boolean IS_SUPPORT_PRIVSPACE = SystemProperties.getBoolean("ro.config.support_privacyspace", false);
    private static final String MANAGE_USE_SECURITY = "com.huawei.permission.MANAGE_USE_SECURITY";
    private static final int PAN_PAY_PLUGIN_ID = 12;
    private static final String PROPERTIES_CC_MODE_SUPPORTED = "ro.config.support_ccmode";
    private static final int PWD_PROTECT_PLUGIN_ID = 10;
    private static final int RESIDENT_PLUGIN_FLAG = 1;
    private static final int SEAPP_RESIDENT_PRIORITY = 100;
    private static final int SECURITY_PROFILE_PLUGIN_ID = 8;
    private static final String SECURITY_SERVICE = "securityserver";
    private static final String TAG = "HwSecurityService";
    private static final int TRUSTSPACE_PLUGIN_ID = 4;
    private Context mContext;
    private ArrayMap<Integer, HwSecurityPluginObj> mMapPlugins = new ArrayMap<>(10);

    public interface IHwPluginRef {
        void bind(IBinder iBinder);

        IHwSecurityPlugin get();

        void set(IHwSecurityPlugin iHwSecurityPlugin);

        boolean unBind(IBinder iBinder);
    }

    public HwSecurityService(Context context) {
        super(context);
        this.mContext = context;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.android.server.security.HwSecurityService */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v1, types: [com.android.server.security.HwSecurityService$HwSecurityServiceWrapper, android.os.IBinder] */
    public void onStart() {
        if (IS_HW_DEBUG) {
            Slog.d(TAG, "Start HwSecurityService");
        }
        publishBinderService(SECURITY_SERVICE, new HwSecurityServiceWrapper());
        registerCorePlugins();
        registerOtherPlugins();
    }

    public void onBootPhase(int phase) {
        startResidentPlugin(phase);
    }

    public void onClientBinderDie(int pluginId, IBinder client) {
        if (client != null) {
            unBindDynamicPlugin(pluginId, client, true);
        }
    }

    private void registerCorePlugins() {
        if (IS_SUPPORT_HW_DEVICE_USAGE) {
            registerPlugin(1, 1, 1000, HwDeviceUsagePlugin.CREATOR, null);
        }
        if (IS_SUPPORT_HW_SECURE_DIAGNOSE) {
            registerPlugin(2, 1, 500, HwSecurityDiagnosePlugin.CREATOR, null);
        }
        if (IS_SUPPORT_HW_TRUSTSPACE) {
            registerPlugin(4, 1, 500, TrustSpaceManagerService.CREATOR, null);
        }
        if (IS_SUPPORT_HW_SECCENTER) {
            registerPlugin(18, new HwSecurityPluginObj(1, 500, SecurityCenterPluginService.CREATOR, null, false), 0);
        }
        if (IS_SUPPORT_HW_SEAPP) {
            registerPlugin(8, new HwSecurityPluginObj(1, 500, SecurityProfileService.CREATOR, null, false), 100);
        }
    }

    private void registerOtherPlugins() {
        if (IS_SUPPORT_CC_MODE) {
            registerPlugin(9, 1, 1000, HwCCModePlugin.CREATOR, HwCCModePlugin.BINDLOCK);
        }
        if (IS_SUPPORT_PRIVSPACE) {
            registerPlugin(10, new HwSecurityPluginObj(1, 1000, PwdProtectService.CREATOR, null, false), 0);
        }
        if (IS_SUPPORT_HW_IUDF) {
            registerPlugin(11, new HwSecurityPluginObj(1, 500, HwSfpService.CREATOR, null, false), 0);
        }
        if (IS_SUPPORT_HW_SECCENTER) {
            registerPlugin(13, new HwSecurityPluginObj(1, 500, HwSystemManagerPlugin.CREATOR, null, false), 0);
        }
        if (IS_SUPPORT_HW_KEYCHAIN) {
            registerPlugin(20, new HwSecurityPluginObj(1, 1000, HwKeychainService.CREATOR, null, false), 0);
        }
        if (HwAntiMalPlugin.isNeedRegisterAntiMalPlugin()) {
            registerPlugin(16, new HwSecurityPluginObj(1, 1000, HwAntiMalPlugin.CREATOR, null, false), 0);
        }
        if (IS_SUPPORT_HW_EID) {
            registerPlugin(15, 1, 500, HwEidPlugin.CREATOR, HwEidPlugin.BINDLOCK);
        }
        if (IDAnonymizationManagerService.isNeedRegisterService()) {
            registerPlugin(21, new HwSecurityPluginObj(1, 1000, IDAnonymizationManagerService.CREATOR, null, false), 0);
        }
        if (IS_SUPPORT_HW_SECCENTER) {
            registerPlugin(17, new HwSecurityPluginObj(1, 500, HwPermissionService.CREATOR, null, false), 0);
        }
        if (IS_SUPPORT_HW_BEHAVIOR_AUTH) {
            registerPlugin(22, 1, 500, HwBehaviorCollectPlugin.CREATOR, null);
        }
    }

    private void startResidentPlugin(int startupTiming) {
        if (!this.mMapPlugins.isEmpty()) {
            List<HwSecurityPluginObj> pluginList = new ArrayList<>(10);
            for (Integer num : this.mMapPlugins.keySet()) {
                HwSecurityPluginObj obj = this.mMapPlugins.get(Integer.valueOf(num.intValue()));
                if (obj.getStartupTiming() == startupTiming && obj.getFlag() == 1) {
                    pluginList.add(obj);
                }
            }
            Collections.sort(pluginList, new ResidentPriorityComparator());
            int size = pluginList.size();
            for (int i = 0; i < size; i++) {
                HwSecurityPluginObj obj2 = pluginList.get(i);
                IHwSecurityPlugin plugin = obj2.getCreator().createPlugin(this.mContext);
                if (IS_HW_DEBUG) {
                    Slog.d(TAG, "createPlugin");
                }
                obj2.getPluginRef().set(plugin);
                if (plugin != null) {
                    if (IS_HW_DEBUG) {
                        Slog.d(TAG, "plugin start");
                    }
                    plugin.onStart();
                } else if (IS_HW_DEBUG) {
                    Slog.d(TAG, "plugin is null");
                }
            }
        }
    }

    private void checkPluginPermission(HwSecurityPluginObj obj) {
        if (obj != null && obj.getCreator() != null && obj.isRequiredPermission()) {
            String pluginPermission = obj.getCreator().getPluginPermission();
            if (pluginPermission != null) {
                checkPermission(pluginPermission);
            } else {
                checkPermission(MANAGE_USE_SECURITY);
            }
        }
    }

    /* access modifiers changed from: private */
    public IBinder bindDynamicPlugin(int pluginId, IBinder client) {
        IBinder asBinder;
        if (IS_HW_DEBUG) {
            Slog.d(TAG, "bindDynamicPlugin");
        }
        if (client == null || !this.mMapPlugins.containsKey(Integer.valueOf(pluginId))) {
            Slog.e(TAG, "client is null or no this dynamic Plugin");
            return null;
        }
        HwSecurityPluginObj obj = this.mMapPlugins.get(Integer.valueOf(pluginId));
        if (obj == null || obj.getFlag() != 0) {
            return null;
        }
        checkPluginPermission(obj);
        synchronized (obj.getLock()) {
            IHwSecurityPlugin plugin = obj.getPluginRef().get();
            if (plugin == null) {
                plugin = obj.getCreator().createPlugin(this.mContext);
                plugin.onStart();
                obj.getPluginRef().set(plugin);
            }
            obj.getPluginRef().bind(client);
            asBinder = plugin.asBinder();
        }
        return asBinder;
    }

    /* access modifiers changed from: private */
    public IBinder queryInterface(int pluginId) {
        if (IS_HW_DEBUG) {
            Slog.d(TAG, "find this Resident Plugin");
        }
        if (!this.mMapPlugins.containsKey(Integer.valueOf(pluginId))) {
            Slog.e(TAG, "not find this Resident Plugin");
            return null;
        }
        HwSecurityPluginObj obj = this.mMapPlugins.get(Integer.valueOf(pluginId));
        if (obj == null || obj.getFlag() != 1) {
            Slog.e(TAG, "obj is null");
            return null;
        }
        checkPluginPermission(obj);
        IHwSecurityPlugin plugin = obj.getPluginRef().get();
        if (plugin != null) {
            return plugin.asBinder();
        }
        return null;
    }

    private void registerPlugin(int pluginId, int flag, int startupTiming, IHwSecurityPlugin.Creator creator, Object lockObj) {
        HwSecurityPluginObj obj = new HwSecurityPluginObj(flag, startupTiming, creator, lockObj, true);
        obj.setResidentPriorityLevel(0);
        registerPlugin(pluginId, obj);
    }

    private void registerPlugin(int pluginId, HwSecurityPluginObj obj, int residentPriorityLevel) {
        if (obj != null) {
            obj.setResidentPriorityLevel(residentPriorityLevel);
            registerPlugin(pluginId, obj);
        }
    }

    private void registerPlugin(int pluginId, HwSecurityPluginObj obj) {
        if (obj != null && obj.getCreator() != null) {
            if (IS_HW_DEBUG) {
                Slog.d(TAG, "registerPlugin pluginId: " + pluginId);
            }
            int flag = obj.getFlag();
            if (flag == 1) {
                obj.setPluginRef(new HwSecurityPluginRef());
            } else if (flag == 0) {
                obj.setPluginRef(new HwSecurityDynamicPluginRef(pluginId, this));
            }
            this.mMapPlugins.put(Integer.valueOf(pluginId), obj);
        }
    }

    /* access modifiers changed from: private */
    public void unBindDynamicPlugin(int pluginId, IBinder client, boolean isInnerCalling) {
        HwSecurityPluginObj obj;
        if (IS_HW_DEBUG) {
            Slog.d(TAG, "unBindDynamicPlugin");
        }
        if (this.mMapPlugins.containsKey(Integer.valueOf(pluginId)) && (obj = this.mMapPlugins.get(Integer.valueOf(pluginId))) != null && obj.getFlag() == 0) {
            if (!isInnerCalling) {
                checkPluginPermission(obj);
            }
            synchronized (obj.getLock()) {
                IHwSecurityPlugin plugin = obj.getPluginRef().get();
                if (obj.getPluginRef().unBind(client)) {
                    plugin.onStop();
                    obj.getPluginRef().set(null);
                }
            }
        }
    }

    private void checkPermission(String permission) {
        Context context = getContext();
        context.enforceCallingOrSelfPermission(permission, "Must have " + permission + " permission.");
    }

    private static class ResidentPriorityComparator implements Comparator<HwSecurityPluginObj>, Serializable {
        private ResidentPriorityComparator() {
        }

        public int compare(HwSecurityPluginObj plugin1, HwSecurityPluginObj plugin2) {
            return Integer.compare(plugin2.getResidentPriorityLevel(), plugin1.getResidentPriorityLevel());
        }
    }

    private final class HwSecurityServiceWrapper extends IHwSecurityService.Stub {
        private HwSecurityServiceWrapper() {
        }

        public IBinder querySecurityInterface(int pluginId) {
            return HwSecurityService.this.queryInterface(pluginId);
        }

        public void unBind(int pluginId, IBinder client) {
            if (client != null) {
                HwSecurityService.this.unBindDynamicPlugin(pluginId, client, false);
            }
        }

        public IBinder bind(int pluginId, IBinder client) {
            return HwSecurityService.this.bindDynamicPlugin(pluginId, client);
        }
    }

    private class HwSecurityPluginRef implements IHwPluginRef {
        private IHwSecurityPlugin mPlugin;

        private HwSecurityPluginRef() {
        }

        @Override // com.android.server.security.HwSecurityService.IHwPluginRef
        public IHwSecurityPlugin get() {
            return this.mPlugin;
        }

        @Override // com.android.server.security.HwSecurityService.IHwPluginRef
        public void set(IHwSecurityPlugin plugin) {
            this.mPlugin = plugin;
        }

        @Override // com.android.server.security.HwSecurityService.IHwPluginRef
        public void bind(IBinder binder) {
        }

        @Override // com.android.server.security.HwSecurityService.IHwPluginRef
        public boolean unBind(IBinder binder) {
            return false;
        }
    }

    private class HwSecurityDynamicPluginRef implements IHwPluginRef {
        private HashMap<IBinder, Death> mClients = new HashMap<>(10);
        private HwSecurityService mParentService;
        private IHwSecurityPlugin mPlugin;
        private int mPluginId;

        HwSecurityDynamicPluginRef(int pluginId, HwSecurityService service) {
            this.mPluginId = pluginId;
            this.mParentService = service;
        }

        @Override // com.android.server.security.HwSecurityService.IHwPluginRef
        public IHwSecurityPlugin get() {
            return this.mPlugin;
        }

        @Override // com.android.server.security.HwSecurityService.IHwPluginRef
        public void set(IHwSecurityPlugin plugin) {
            this.mPlugin = plugin;
        }

        @Override // com.android.server.security.HwSecurityService.IHwPluginRef
        public void bind(IBinder client) {
            if (client != null) {
                if (HwSecurityService.IS_HW_DEBUG) {
                    Slog.d(HwSecurityService.TAG, "HwSecurityDynamicPluginRef, bind");
                }
                Death death = new Death(client);
                try {
                    client.linkToDeath(death, 0);
                } catch (RemoteException e) {
                    Slog.e(HwSecurityService.TAG, "HwSecurityDynamicPluginRef bind exception!");
                }
                this.mClients.put(client, death);
                if (HwSecurityService.IS_HW_DEBUG) {
                    Slog.d(HwSecurityService.TAG, "HwSecurityDynamicPluginRef bind mClients size:" + this.mClients.size() + ", client" + client);
                }
            }
        }

        @Override // com.android.server.security.HwSecurityService.IHwPluginRef
        public boolean unBind(IBinder client) {
            if (client == null) {
                return false;
            }
            if (HwSecurityService.IS_HW_DEBUG) {
                Slog.d(HwSecurityService.TAG, "HwSecurityDynamicPluginRef, unBind");
            }
            client.unlinkToDeath(this.mClients.remove(client), 0);
            if (HwSecurityService.IS_HW_DEBUG) {
                Slog.d(HwSecurityService.TAG, "HwSecurityDynamicPluginRef unbind mClients size:" + this.mClients.size() + ", client" + client);
            }
            return this.mClients.isEmpty();
        }

        /* access modifiers changed from: private */
        public void onClientBinderDie(IBinder client) {
            this.mParentService.onClientBinderDie(this.mPluginId, client);
        }

        private class Death implements IBinder.DeathRecipient {
            private IBinder mToken;

            Death(IBinder token) {
                this.mToken = token;
            }

            public void binderDied() {
                HwSecurityDynamicPluginRef.this.onClientBinderDie(this.mToken);
            }
        }
    }

    private class HwSecurityPluginObj {
        private IHwSecurityPlugin.Creator mCreator;
        private int mFlag;
        private boolean mIsRequiredPermission = true;
        private final Object mLock;
        private IHwPluginRef mPluginRef;
        private int mResidentPriorityLevel;
        private int mStartupTiming;

        HwSecurityPluginObj(int flag, int startupTiming, IHwSecurityPlugin.Creator creator, Object lockObj, boolean isRequiredPermission) {
            this.mFlag = flag;
            this.mStartupTiming = startupTiming;
            this.mCreator = creator;
            this.mLock = lockObj;
            this.mIsRequiredPermission = isRequiredPermission;
        }

        /* access modifiers changed from: private */
        public boolean isRequiredPermission() {
            return this.mIsRequiredPermission;
        }

        /* access modifiers changed from: private */
        public int getFlag() {
            return this.mFlag;
        }

        /* access modifiers changed from: private */
        public IHwPluginRef getPluginRef() {
            return this.mPluginRef;
        }

        /* access modifiers changed from: private */
        public void setPluginRef(IHwPluginRef pluginRef) {
            this.mPluginRef = pluginRef;
        }

        /* access modifiers changed from: private */
        public IHwSecurityPlugin.Creator getCreator() {
            return this.mCreator;
        }

        /* access modifiers changed from: private */
        public Object getLock() {
            return this.mLock;
        }

        /* access modifiers changed from: private */
        public int getStartupTiming() {
            return this.mStartupTiming;
        }

        /* access modifiers changed from: private */
        public int getResidentPriorityLevel() {
            return this.mResidentPriorityLevel;
        }

        /* access modifiers changed from: private */
        public void setResidentPriorityLevel(int residentPriorityLevel) {
            this.mResidentPriorityLevel = residentPriorityLevel;
        }
    }
}
