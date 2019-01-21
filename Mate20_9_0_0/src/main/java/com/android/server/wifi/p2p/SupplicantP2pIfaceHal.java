package com.android.server.wifi.p2p;

import android.hardware.wifi.supplicant.V1_0.ISupplicant;
import android.hardware.wifi.supplicant.V1_0.ISupplicant.IfaceInfo;
import android.hardware.wifi.supplicant.V1_0.ISupplicantIface;
import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork;
import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface;
import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.FreqRange;
import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pNetwork;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatusCode;
import android.hardware.wifi.supplicant.V1_0.WpsConfigMethods;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.hidl.manager.V1_0.IServiceNotification.Stub;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pGroupList;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.os.IHwBinder.DeathRecipient;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.util.ArrayUtils;
import com.android.server.wifi.ScoringParams;
import com.android.server.wifi.util.NativeUtil;
import com.android.server.wifi.util.StringUtil;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantP2pIfaceCallback;

public class SupplicantP2pIfaceHal {
    private static final boolean DBG = true;
    private static final int DEFAULT_GROUP_OWNER_INTENT = 6;
    private static final int DEFAULT_OPERATING_CLASS = 81;
    private static final int RESULT_NOT_VALID = -1;
    private static final String TAG = "SupplicantP2pIfaceHal";
    private static final Pattern WPS_DEVICE_TYPE_PATTERN = Pattern.compile("^(\\d{1,2})-([0-9a-fA-F]{8})-(\\d{1,2})$");
    private SupplicantP2pIfaceCallback mCallback = null;
    private ISupplicantIface mHidlSupplicantIface = null;
    private IServiceManager mIServiceManager = null;
    private ISupplicant mISupplicant = null;
    private ISupplicantP2pIface mISupplicantP2pIface = null;
    private Object mLock = new Object();
    private final WifiP2pMonitor mMonitor;
    private final DeathRecipient mServiceManagerDeathRecipient = new -$$Lambda$SupplicantP2pIfaceHal$Wvwk6xCSAknWmsVUgpUqV_3NQiE(this);
    private final IServiceNotification mServiceNotificationCallback = new Stub() {
        public void onRegistration(String fqName, String name, boolean preexisting) {
            synchronized (SupplicantP2pIfaceHal.this.mLock) {
                String str = SupplicantP2pIfaceHal.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("IServiceNotification.onRegistration for: ");
                stringBuilder.append(fqName);
                stringBuilder.append(", ");
                stringBuilder.append(name);
                stringBuilder.append(" preexisting=");
                stringBuilder.append(preexisting);
                Log.i(str, stringBuilder.toString());
                if (SupplicantP2pIfaceHal.this.initSupplicantService()) {
                    Log.i(SupplicantP2pIfaceHal.TAG, "Completed initialization of ISupplicant interfaces.");
                } else {
                    Log.e(SupplicantP2pIfaceHal.TAG, "initalizing ISupplicant failed.");
                    SupplicantP2pIfaceHal.this.supplicantServiceDiedHandler();
                }
            }
        }
    };
    private final DeathRecipient mSupplicantDeathRecipient = new -$$Lambda$SupplicantP2pIfaceHal$AwvLtkH4UyCOhUYx__3ExZj_7jQ(this);

    private static class SupplicantResult<E> {
        private boolean mHasRecordHalTime = false;
        private String mMethodName;
        private long mP2pHalCallStartTime;
        private SupplicantStatus mStatus;
        private E mValue;

        SupplicantResult(String methodName) {
            this.mMethodName = methodName;
            this.mStatus = null;
            this.mValue = null;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("entering ");
            stringBuilder.append(this.mMethodName);
            SupplicantP2pIfaceHal.logd(stringBuilder.toString());
            this.mP2pHalCallStartTime = SystemClock.uptimeMillis();
            this.mHasRecordHalTime = false;
        }

        public void setResult(SupplicantStatus status, E value) {
            SupplicantP2pIfaceHal.logCompletion(this.mMethodName, status);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("leaving ");
            stringBuilder.append(this.mMethodName);
            stringBuilder.append(" with result = ");
            stringBuilder.append(value);
            SupplicantP2pIfaceHal.logd(stringBuilder.toString());
            this.mStatus = status;
            this.mValue = value;
            checkHalCallThresholdMs();
        }

        public void setResult(SupplicantStatus status) {
            SupplicantP2pIfaceHal.logCompletion(this.mMethodName, status);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("leaving ");
            stringBuilder.append(this.mMethodName);
            SupplicantP2pIfaceHal.logd(stringBuilder.toString());
            this.mStatus = status;
            checkHalCallThresholdMs();
        }

        public boolean isSuccess() {
            checkHalCallThresholdMs();
            return this.mStatus != null && (this.mStatus.code == 0 || this.mStatus.code == 5);
        }

        public E getResult() {
            checkHalCallThresholdMs();
            return isSuccess() ? this.mValue : null;
        }

        private void checkHalCallThresholdMs() {
            if (!this.mHasRecordHalTime) {
                long mP2pHalCallEndTime = SystemClock.uptimeMillis();
                int statusCode = -1;
                if (this.mStatus != null) {
                    statusCode = this.mStatus.code;
                }
                if (mP2pHalCallEndTime - this.mP2pHalCallStartTime > 300) {
                    String str = SupplicantP2pIfaceHal.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Hal call took ");
                    stringBuilder.append(mP2pHalCallEndTime - this.mP2pHalCallStartTime);
                    stringBuilder.append("ms on ");
                    stringBuilder.append(this.mMethodName);
                    stringBuilder.append(", status.code:");
                    stringBuilder.append(SupplicantStatusCode.toString(statusCode));
                    Log.w(str, stringBuilder.toString(), new Exception());
                }
                this.mHasRecordHalTime = true;
            }
        }
    }

    private class VendorSupplicantP2pIfaceHalCallbackV2_0 extends ISupplicantP2pIfaceCallback.Stub {
        private static final String TAG = "SupplicantP2pIfaceCallback";
        SupplicantP2pIfaceCallback mCallback;
        private final String mInterface;
        private final WifiP2pMonitor mMonitor;

        VendorSupplicantP2pIfaceHalCallbackV2_0(String iface, WifiP2pMonitor monitor, SupplicantP2pIfaceCallback callback) {
            this.mInterface = iface;
            this.mMonitor = monitor;
            this.mCallback = callback;
        }

        public void onNetworkAdded(int networkId) {
            this.mCallback.onNetworkAdded(networkId);
        }

        public void onNetworkRemoved(int networkId) {
            this.mCallback.onNetworkRemoved(networkId);
        }

        public void onDeviceFound(byte[] srcAddress, byte[] p2pDeviceAddress, byte[] primaryDeviceType, String deviceName, short configMethods, byte deviceCapabilities, int groupCapabilities, byte[] wfdDeviceInfo) {
            this.mCallback.onDeviceFound(srcAddress, p2pDeviceAddress, primaryDeviceType, deviceName, configMethods, deviceCapabilities, groupCapabilities, wfdDeviceInfo);
        }

        public void onDeviceLost(byte[] p2pDeviceAddress) {
            this.mCallback.onDeviceLost(p2pDeviceAddress);
        }

        public void onFindStopped() {
            this.mCallback.onFindStopped();
        }

        public void onGoNegotiationRequest(byte[] srcAddress, short passwordId) {
            this.mCallback.onGoNegotiationRequest(srcAddress, passwordId);
        }

        public void onGoNegotiationCompleted(int status) {
            this.mCallback.onGoNegotiationCompleted(status);
        }

        public void onGroupFormationSuccess() {
            this.mCallback.onGroupFormationSuccess();
        }

        public void onGroupFormationFailure(String failureReason) {
            this.mCallback.onGroupFormationFailure(failureReason);
        }

        public void onGroupStarted(String groupIfName, boolean isGo, ArrayList<Byte> ssid, int frequency, byte[] psk, String passphrase, byte[] goDeviceAddress, boolean isPersistent) {
            this.mCallback.onGroupStarted(groupIfName, isGo, ssid, frequency, psk, passphrase, goDeviceAddress, isPersistent);
        }

        public void onGroupRemoved(String groupIfName, boolean isGo) {
            this.mCallback.onGroupRemoved(groupIfName, isGo);
        }

        public void onInvitationReceived(byte[] srcAddress, byte[] goDeviceAddress, byte[] bssid, int persistentNetworkId, int operatingFrequency) {
            this.mCallback.onInvitationReceived(srcAddress, goDeviceAddress, bssid, persistentNetworkId, operatingFrequency);
        }

        public void onInvitationResult(byte[] bssid, int status) {
            this.mCallback.onInvitationResult(bssid, status);
        }

        public void onProvisionDiscoveryCompleted(byte[] p2pDeviceAddress, boolean isRequest, byte status, short configMethods, String generatedPin) {
            this.mCallback.onProvisionDiscoveryCompleted(p2pDeviceAddress, isRequest, status, configMethods, generatedPin);
        }

        public void onServiceDiscoveryResponse(byte[] srcAddress, short updateIndicator, ArrayList<Byte> tlvs) {
            this.mCallback.onServiceDiscoveryResponse(srcAddress, updateIndicator, tlvs);
        }

        public void onStaAuthorized(byte[] srcAddress, byte[] p2pDeviceAddress) {
            this.mCallback.onStaAuthorized(srcAddress, p2pDeviceAddress);
        }

        public void onStaDeauthorized(byte[] srcAddress, byte[] p2pDeviceAddress) {
            this.mCallback.onStaDeauthorized(srcAddress, p2pDeviceAddress);
        }

        public void onP2pInterfaceCreated(String dataString) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onP2pInterfaceCreated ");
            stringBuilder.append(dataString);
            Log.d(str, stringBuilder.toString());
            if (dataString != null) {
                String[] tokens = dataString.split(" ");
                if (tokens.length >= 3) {
                    if (tokens[1].startsWith("GO")) {
                        this.mMonitor.broadcastP2pGoInterfaceCreated(this.mInterface, tokens[2]);
                    } else {
                        this.mMonitor.broadcastP2pGcInterfaceCreated(this.mInterface, tokens[2]);
                    }
                }
            }
        }

        public void onGroupRemoveAndReform(String iface) {
            this.mMonitor.broadcastP2pGroupRemoveAndReform(this.mInterface);
        }
    }

    public static /* synthetic */ void lambda$new$0(SupplicantP2pIfaceHal supplicantP2pIfaceHal, long cookie) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("IServiceManager died: cookie=");
        stringBuilder.append(cookie);
        Log.w(str, stringBuilder.toString());
        synchronized (supplicantP2pIfaceHal.mLock) {
            supplicantP2pIfaceHal.supplicantServiceDiedHandler();
            supplicantP2pIfaceHal.mIServiceManager = null;
        }
    }

    public static /* synthetic */ void lambda$new$1(SupplicantP2pIfaceHal supplicantP2pIfaceHal, long cookie) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ISupplicant/ISupplicantStaIface died: cookie=");
        stringBuilder.append(cookie);
        Log.w(str, stringBuilder.toString());
        synchronized (supplicantP2pIfaceHal.mLock) {
            supplicantP2pIfaceHal.supplicantServiceDiedHandler();
        }
    }

    public SupplicantP2pIfaceHal(WifiP2pMonitor monitor) {
        this.mMonitor = monitor;
    }

    private boolean linkToServiceManagerDeath() {
        if (this.mIServiceManager == null) {
            return false;
        }
        try {
            if (this.mIServiceManager.linkToDeath(this.mServiceManagerDeathRecipient, 0)) {
                return true;
            }
            Log.wtf(TAG, "Error on linkToDeath on IServiceManager");
            supplicantServiceDiedHandler();
            this.mIServiceManager = null;
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "IServiceManager.linkToDeath exception", e);
            return false;
        }
    }

    public boolean initialize() {
        Log.i(TAG, "Registering ISupplicant service ready callback.");
        synchronized (this.mLock) {
            if (this.mIServiceManager != null) {
                Log.i(TAG, "Supplicant HAL already initialized.");
                return true;
            }
            this.mISupplicant = null;
            this.mISupplicantP2pIface = null;
            try {
                this.mIServiceManager = getServiceManagerMockable();
                if (this.mIServiceManager == null) {
                    Log.e(TAG, "Failed to get HIDL Service Manager");
                    return false;
                } else if (!linkToServiceManagerDeath()) {
                    return false;
                } else if (this.mIServiceManager.registerForNotifications(ISupplicant.kInterfaceName, "", this.mServiceNotificationCallback)) {
                    return true;
                } else {
                    Log.e(TAG, "Failed to register for notifications to android.hardware.wifi.supplicant@1.0::ISupplicant");
                    this.mIServiceManager = null;
                    return false;
                }
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception while trying to register a listener for ISupplicant service: ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                supplicantServiceDiedHandler();
                return false;
            }
        }
    }

    private boolean linkToSupplicantDeath() {
        if (this.mISupplicant == null) {
            return false;
        }
        try {
            if (this.mISupplicant.linkToDeath(this.mSupplicantDeathRecipient, 0)) {
                return true;
            }
            Log.wtf(TAG, "Error on linkToDeath on ISupplicant");
            supplicantServiceDiedHandler();
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "ISupplicant.linkToDeath exception", e);
            return false;
        }
    }

    private boolean initSupplicantService() {
        synchronized (this.mLock) {
            try {
                this.mISupplicant = getSupplicantMockable();
                if (this.mISupplicant == null) {
                    Log.e(TAG, "Got null ISupplicant service. Stopping supplicant HIDL startup");
                    return false;
                } else if (linkToSupplicantDeath()) {
                    return true;
                } else {
                    return false;
                }
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ISupplicant.getService exception: ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                return false;
            } catch (Throwable th) {
            }
        }
    }

    private boolean linkToSupplicantP2pIfaceDeath() {
        if (this.mISupplicantP2pIface == null) {
            return false;
        }
        try {
            if (this.mISupplicantP2pIface.linkToDeath(this.mSupplicantDeathRecipient, 0)) {
                return true;
            }
            Log.wtf(TAG, "Error on linkToDeath on ISupplicantP2pIface");
            supplicantServiceDiedHandler();
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "ISupplicantP2pIface.linkToDeath exception", e);
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:37:0x0068, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean setupIface(String ifaceName) {
        if (ifaceName == null) {
            Log.e(TAG, "Got null ifaceName when setting up p2p Iface");
            return false;
        }
        synchronized (this.mLock) {
            if (this.mISupplicantP2pIface != null) {
                return false;
            }
            ISupplicantIface ifaceHwBinder;
            if (isV1_1()) {
                ifaceHwBinder = addIfaceV1_1(ifaceName);
            } else {
                ifaceHwBinder = getIfaceV1_0(ifaceName);
            }
            if (ifaceHwBinder == null) {
                Log.e(TAG, "initSupplicantP2pIface got null iface");
                return false;
            } else if (trySetupForVendorV2_0(ifaceHwBinder, ifaceName)) {
                return true;
            } else {
                this.mISupplicantP2pIface = getP2pIfaceMockable(ifaceHwBinder);
                if (!linkToSupplicantP2pIfaceDeath()) {
                    return false;
                } else if (!(this.mISupplicantP2pIface == null || this.mMonitor == null)) {
                    this.mCallback = new SupplicantP2pIfaceCallback(ifaceName, this.mMonitor);
                    if (!registerCallback(this.mCallback)) {
                        Log.e(TAG, "Callback registration failed. Initialization incomplete.");
                        return false;
                    }
                }
            }
        }
    }

    private ISupplicantIface getIfaceV1_0(String ifaceName) {
        ArrayList<IfaceInfo> supplicantIfaces = new ArrayList();
        try {
            this.mISupplicant.listInterfaces(new -$$Lambda$SupplicantP2pIfaceHal$xxcrLmh4P3s14clwwWnJ79St0UM(supplicantIfaces));
            if (supplicantIfaces.size() == 0) {
                Log.e(TAG, "Got zero HIDL supplicant ifaces. Stopping supplicant HIDL startup.");
                supplicantServiceDiedHandler();
                return null;
            }
            SupplicantResult<ISupplicantIface> supplicantIface = new SupplicantResult("getInterface()");
            Iterator it = supplicantIfaces.iterator();
            while (it.hasNext()) {
                IfaceInfo ifaceInfo = (IfaceInfo) it.next();
                if (ifaceInfo.type == 1 && ifaceName.equals(ifaceInfo.name)) {
                    try {
                        this.mISupplicant.getInterface(ifaceInfo, new -$$Lambda$SupplicantP2pIfaceHal$l7P05UXArQDgqCiDU1muZnDZyB4(supplicantIface));
                        break;
                    } catch (RemoteException e) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("ISupplicant.getInterface exception: ");
                        stringBuilder.append(e);
                        Log.e(str, stringBuilder.toString());
                        supplicantServiceDiedHandler();
                        return null;
                    }
                }
            }
            return (ISupplicantIface) supplicantIface.getResult();
        } catch (RemoteException e2) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("ISupplicant.listInterfaces exception: ");
            stringBuilder2.append(e2);
            Log.e(str2, stringBuilder2.toString());
            return null;
        }
    }

    static /* synthetic */ void lambda$getIfaceV1_0$2(ArrayList supplicantIfaces, SupplicantStatus status, ArrayList ifaces) {
        if (status.code != 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Getting Supplicant Interfaces failed: ");
            stringBuilder.append(status.code);
            Log.e(str, stringBuilder.toString());
            return;
        }
        supplicantIfaces.addAll(ifaces);
    }

    static /* synthetic */ void lambda$getIfaceV1_0$3(SupplicantResult supplicantIface, SupplicantStatus status, ISupplicantIface iface) {
        if (status.code != 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to get ISupplicantIface ");
            stringBuilder.append(status.code);
            Log.e(str, stringBuilder.toString());
            return;
        }
        supplicantIface.setResult(status, iface);
    }

    private ISupplicantIface addIfaceV1_1(String ifaceName) {
        synchronized (this.mLock) {
            IfaceInfo ifaceInfo = new IfaceInfo();
            ifaceInfo.name = ifaceName;
            ifaceInfo.type = 1;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("addInterface(");
            stringBuilder.append(ifaceInfo);
            stringBuilder.append(")");
            SupplicantResult<ISupplicantIface> supplicantIface = new SupplicantResult(stringBuilder.toString());
            try {
                android.hardware.wifi.supplicant.V1_1.ISupplicant supplicant_v1_1 = getSupplicantMockableV1_1();
                if (supplicant_v1_1 == null) {
                    Log.e(TAG, "Can't call addIface: ISupplicantP2pIface is null");
                    return null;
                }
                supplicant_v1_1.addInterface(ifaceInfo, new -$$Lambda$SupplicantP2pIfaceHal$uOALwzLWCrgwsgrkWxQlW6drzT8(supplicantIface));
                ISupplicantIface iSupplicantIface = (ISupplicantIface) supplicantIface.getResult();
                return iSupplicantIface;
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("ISupplicant.addInterface exception: ");
                stringBuilder2.append(e);
                Log.e(str, stringBuilder2.toString());
                supplicantServiceDiedHandler();
                return null;
            }
        }
    }

    static /* synthetic */ void lambda$addIfaceV1_1$4(SupplicantResult supplicantIface, SupplicantStatus status, ISupplicantIface iface) {
        if (status.code == 0 || status.code == 5) {
            supplicantIface.setResult(status, iface);
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Failed to get ISupplicantIface ");
        stringBuilder.append(status.code);
        Log.e(str, stringBuilder.toString());
    }

    public boolean teardownIface(String ifaceName) {
        synchronized (this.mLock) {
            if (this.mISupplicantP2pIface == null) {
                return false;
            } else if (isV1_1()) {
                boolean removeIfaceV1_1 = removeIfaceV1_1(ifaceName);
                return removeIfaceV1_1;
            } else {
                return true;
            }
        }
    }

    private boolean removeIfaceV1_1(String ifaceName) {
        synchronized (this.mLock) {
            try {
                android.hardware.wifi.supplicant.V1_1.ISupplicant supplicant_v1_1 = getSupplicantMockableV1_1();
                if (supplicant_v1_1 == null) {
                    Log.e(TAG, "Can't call removeIface: ISupplicantP2pIface is null");
                    return false;
                }
                IfaceInfo ifaceInfo = new IfaceInfo();
                ifaceInfo.name = ifaceName;
                ifaceInfo.type = 1;
                SupplicantStatus status = supplicant_v1_1.removeInterface(ifaceInfo);
                if (status.code != 0) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to remove iface ");
                    stringBuilder.append(status.code);
                    Log.e(str, stringBuilder.toString());
                    return false;
                }
                this.mCallback = null;
                this.mISupplicantP2pIface = null;
                return true;
            } catch (RemoteException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("ISupplicant.removeInterface exception: ");
                stringBuilder2.append(e);
                Log.e(str2, stringBuilder2.toString());
                supplicantServiceDiedHandler();
                return false;
            } catch (Throwable th) {
            }
        }
    }

    private void supplicantServiceDiedHandler() {
        synchronized (this.mLock) {
            this.mISupplicant = null;
            this.mISupplicantP2pIface = null;
        }
    }

    public boolean isInitializationStarted() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mIServiceManager != null;
        }
        return z;
    }

    public boolean isInitializationComplete() {
        return this.mISupplicant != null;
    }

    protected IServiceManager getServiceManagerMockable() throws RemoteException {
        return IServiceManager.getService();
    }

    protected ISupplicant getSupplicantMockable() throws RemoteException {
        try {
            return ISupplicant.getService();
        } catch (NoSuchElementException e) {
            Log.e(TAG, "Failed to get ISupplicant", e);
            return null;
        }
    }

    protected android.hardware.wifi.supplicant.V1_1.ISupplicant getSupplicantMockableV1_1() throws RemoteException {
        android.hardware.wifi.supplicant.V1_1.ISupplicant castFrom;
        synchronized (this.mLock) {
            try {
                castFrom = android.hardware.wifi.supplicant.V1_1.ISupplicant.castFrom(ISupplicant.getService());
            } catch (NoSuchElementException e) {
                Log.e(TAG, "Failed to get ISupplicant", e);
                return null;
            } catch (Throwable th) {
            }
        }
        return castFrom;
    }

    protected ISupplicantP2pIface getP2pIfaceMockable(ISupplicantIface iface) {
        return ISupplicantP2pIface.asInterface(iface.asBinder());
    }

    protected ISupplicantP2pNetwork getP2pNetworkMockable(ISupplicantNetwork network) {
        return ISupplicantP2pNetwork.asInterface(network.asBinder());
    }

    private boolean isV1_1() {
        boolean z;
        synchronized (this.mLock) {
            z = false;
            try {
                if (getSupplicantMockableV1_1() != null) {
                    z = true;
                }
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ISupplicant.getService exception: ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                supplicantServiceDiedHandler();
                return false;
            } catch (Throwable th) {
            }
        }
        return z;
    }

    protected static void logd(String s) {
        Log.d(TAG, s);
    }

    protected static void logCompletion(String operation, SupplicantStatus status) {
        String str;
        StringBuilder stringBuilder;
        if (status == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(operation);
            stringBuilder.append(" failed: no status code returned.");
            Log.w(str, stringBuilder.toString());
        } else if (status.code == 0) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(operation);
            stringBuilder2.append(" completed successfully.");
            logd(stringBuilder2.toString());
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(operation);
            stringBuilder.append(" failed: ");
            stringBuilder.append(status.code);
            stringBuilder.append(" (");
            stringBuilder.append(status.debugMessage);
            stringBuilder.append(")");
            Log.w(str, stringBuilder.toString());
        }
    }

    private boolean checkSupplicantP2pIfaceAndLogFailure(String method) {
        if (this.mISupplicantP2pIface != null) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Can't call ");
        stringBuilder.append(method);
        stringBuilder.append(": ISupplicantP2pIface is null");
        Log.e(str, stringBuilder.toString());
        return false;
    }

    private int wpsInfoToConfigMethod(int info) {
        switch (info) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
            case 3:
                return 2;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported WPS provision method: ");
                stringBuilder.append(info);
                Log.e(str, stringBuilder.toString());
                return -1;
        }
    }

    public String getName() {
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("getName")) {
                SupplicantResult<String> result = new SupplicantResult("getName()");
                try {
                    this.mISupplicantP2pIface.getName(new -$$Lambda$SupplicantP2pIfaceHal$qdAVPJtKWPe5tcjcdhPA5D2APmU(result));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ISupplicantP2pIface exception: ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                    supplicantServiceDiedHandler();
                }
                String str2 = (String) result.getResult();
                return str2;
            }
            return null;
        }
    }

    public boolean registerCallback(android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIfaceCallback receiver) {
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("registerCallback")) {
                SupplicantResult<Void> result = new SupplicantResult("registerCallback()");
                try {
                    result.setResult(this.mISupplicantP2pIface.registerCallback(receiver));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ISupplicantP2pIface exception: ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                    supplicantServiceDiedHandler();
                }
                boolean isSuccess = result.isSuccess();
                return isSuccess;
            }
            return false;
        }
    }

    public boolean find(int timeout) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("find")) {
                return false;
            } else if (timeout < 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid timeout value: ");
                stringBuilder.append(timeout);
                Log.e(str, stringBuilder.toString());
                return false;
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("find(");
                stringBuilder2.append(timeout);
                stringBuilder2.append(")");
                SupplicantResult<Void> result = new SupplicantResult(stringBuilder2.toString());
                try {
                    result.setResult(this.mISupplicantP2pIface.find(timeout));
                } catch (RemoteException e) {
                    String str2 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("ISupplicantP2pIface exception: ");
                    stringBuilder3.append(e);
                    Log.e(str2, stringBuilder3.toString());
                    supplicantServiceDiedHandler();
                }
                boolean isSuccess = result.isSuccess();
                return isSuccess;
            }
        }
    }

    public boolean stopFind() {
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("stopFind")) {
                SupplicantResult<Void> result = new SupplicantResult("stopFind()");
                try {
                    result.setResult(this.mISupplicantP2pIface.stopFind());
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ISupplicantP2pIface exception: ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                    supplicantServiceDiedHandler();
                }
                boolean isSuccess = result.isSuccess();
                return isSuccess;
            }
            return false;
        }
    }

    public boolean flush() {
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("flush")) {
                SupplicantResult<Void> result = new SupplicantResult("flush()");
                try {
                    result.setResult(this.mISupplicantP2pIface.flush());
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ISupplicantP2pIface exception: ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                    supplicantServiceDiedHandler();
                }
                boolean isSuccess = result.isSuccess();
                return isSuccess;
            }
            return false;
        }
    }

    public boolean serviceFlush() {
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("serviceFlush")) {
                SupplicantResult<Void> result = new SupplicantResult("serviceFlush()");
                try {
                    result.setResult(this.mISupplicantP2pIface.flushServices());
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ISupplicantP2pIface exception: ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                    supplicantServiceDiedHandler();
                }
                boolean isSuccess = result.isSuccess();
                return isSuccess;
            }
            return false;
        }
    }

    public boolean setPowerSave(String groupIfName, boolean enable) {
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("setPowerSave")) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setPowerSave(");
                stringBuilder.append(groupIfName);
                stringBuilder.append(", ");
                stringBuilder.append(enable);
                stringBuilder.append(")");
                SupplicantResult<Void> result = new SupplicantResult(stringBuilder.toString());
                try {
                    result.setResult(this.mISupplicantP2pIface.setPowerSave(groupIfName, enable));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ISupplicantP2pIface exception: ");
                    stringBuilder2.append(e);
                    Log.e(str, stringBuilder2.toString());
                    supplicantServiceDiedHandler();
                }
                boolean isSuccess = result.isSuccess();
                return isSuccess;
            }
            return false;
        }
    }

    public boolean setGroupIdle(String groupIfName, int timeoutInSec) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("setGroupIdle")) {
                return false;
            } else if (timeoutInSec < 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid group timeout value ");
                stringBuilder.append(timeoutInSec);
                Log.e(str, stringBuilder.toString());
                return false;
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setGroupIdle(");
                stringBuilder2.append(groupIfName);
                stringBuilder2.append(", ");
                stringBuilder2.append(timeoutInSec);
                stringBuilder2.append(")");
                SupplicantResult<Void> result = new SupplicantResult(stringBuilder2.toString());
                try {
                    result.setResult(this.mISupplicantP2pIface.setGroupIdle(groupIfName, timeoutInSec));
                } catch (RemoteException e) {
                    String str2 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("ISupplicantP2pIface exception: ");
                    stringBuilder3.append(e);
                    Log.e(str2, stringBuilder3.toString());
                    supplicantServiceDiedHandler();
                }
                boolean isSuccess = result.isSuccess();
                return isSuccess;
            }
        }
    }

    public boolean setSsidPostfix(String postfix) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("setSsidPostfix")) {
                return false;
            } else if (postfix == null) {
                Log.e(TAG, "Invalid SSID postfix value (null).");
                return false;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setSsidPostfix(");
                stringBuilder.append(postfix);
                stringBuilder.append(")");
                SupplicantResult<Void> result = new SupplicantResult(stringBuilder.toString());
                StringBuilder stringBuilder2;
                try {
                    ISupplicantP2pIface iSupplicantP2pIface = this.mISupplicantP2pIface;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("\"");
                    stringBuilder2.append(postfix);
                    stringBuilder2.append("\"");
                    result.setResult(iSupplicantP2pIface.setSsidPostfix(NativeUtil.decodeSsid(stringBuilder2.toString())));
                } catch (RemoteException e) {
                    String str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ISupplicantP2pIface exception: ");
                    stringBuilder2.append(e);
                    Log.e(str, stringBuilder2.toString());
                    supplicantServiceDiedHandler();
                } catch (IllegalArgumentException e2) {
                    Log.e(TAG, "Could not decode SSID.", e2);
                    return false;
                }
                boolean isSuccess = result.isSuccess();
                return isSuccess;
            }
        }
    }

    public String connect(WifiP2pConfig config, boolean joinExistingGroup) {
        if (config == null) {
            return null;
        }
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("setSsidPostfix")) {
                return null;
            } else if (config.deviceAddress == null) {
                Log.e(TAG, "Could not parse null mac address.");
                return null;
            } else if (config.wps.setup != 0 || TextUtils.isEmpty(config.wps.pin)) {
                byte[] peerAddress = null;
                try {
                    byte[] peerAddress2 = NativeUtil.macAddressToByteArray(config.deviceAddress);
                    peerAddress = wpsInfoToConfigMethod(config.wps.setup);
                    StringBuilder stringBuilder;
                    if (peerAddress == -1) {
                        String str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Invalid WPS config method: ");
                        stringBuilder.append(config.wps.setup);
                        Log.e(str, stringBuilder.toString());
                        return null;
                    }
                    String preSelectedPin = TextUtils.isEmpty(config.wps.pin) ? "" : config.wps.pin;
                    boolean persistent = config.netId == -2;
                    int goIntent = 0;
                    if (!joinExistingGroup) {
                        int groupOwnerIntent = config.groupOwnerIntent;
                        if (groupOwnerIntent < 0 || groupOwnerIntent > 15) {
                            groupOwnerIntent = 6;
                        }
                        goIntent = groupOwnerIntent;
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("connect(");
                    stringBuilder.append(StringUtil.safeDisplayBssid(config.deviceAddress));
                    stringBuilder.append(")");
                    SupplicantResult<String> result = new SupplicantResult(stringBuilder.toString());
                    try {
                        this.mISupplicantP2pIface.connect(peerAddress2, peerAddress, preSelectedPin, joinExistingGroup, persistent, goIntent, new -$$Lambda$SupplicantP2pIfaceHal$cZS-3bDskhEdL9pvSQ9NTW85EOo(result));
                    } catch (RemoteException e) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("ISupplicantP2pIface exception: ");
                        stringBuilder2.append(e);
                        Log.e(str2, stringBuilder2.toString());
                        supplicantServiceDiedHandler();
                    }
                    String str3 = (String) result.getResult();
                    return str3;
                } catch (Exception e2) {
                    Log.e(TAG, "Could not parse peer mac address.", e2);
                    return null;
                }
            } else {
                Log.e(TAG, "Expected empty pin for PBC.");
                return null;
            }
        }
    }

    public boolean cancelConnect() {
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("cancelConnect")) {
                SupplicantResult<Void> result = new SupplicantResult("cancelConnect()");
                try {
                    result.setResult(this.mISupplicantP2pIface.cancelConnect());
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ISupplicantP2pIface exception: ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                    supplicantServiceDiedHandler();
                }
                boolean isSuccess = result.isSuccess();
                return isSuccess;
            }
            return false;
        }
    }

    public boolean provisionDiscovery(WifiP2pConfig config) {
        if (config == null) {
            return false;
        }
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("provisionDiscovery")) {
                int targetMethod = wpsInfoToConfigMethod(config.wps.setup);
                StringBuilder stringBuilder;
                if (targetMethod == -1) {
                    String str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unrecognized WPS configuration method: ");
                    stringBuilder.append(config.wps.setup);
                    Log.e(str, stringBuilder.toString());
                    return false;
                }
                if (targetMethod == 1) {
                    targetMethod = 2;
                } else if (targetMethod == 2) {
                    targetMethod = 1;
                }
                if (config.deviceAddress == null) {
                    Log.e(TAG, "Cannot parse null mac address.");
                    return false;
                }
                try {
                    byte[] macAddress = NativeUtil.macAddressToByteArray(config.deviceAddress);
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("provisionDiscovery(");
                    stringBuilder.append(config.deviceAddress);
                    stringBuilder.append(", ");
                    stringBuilder.append(config.wps.setup);
                    stringBuilder.append(")");
                    byte[] macAddress2 = new SupplicantResult(stringBuilder.toString());
                    try {
                        macAddress2.setResult(this.mISupplicantP2pIface.provisionDiscovery(macAddress, targetMethod));
                    } catch (RemoteException e) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("ISupplicantP2pIface exception: ");
                        stringBuilder2.append(e);
                        Log.e(str2, stringBuilder2.toString());
                        supplicantServiceDiedHandler();
                    }
                    boolean isSuccess = macAddress2.isSuccess();
                    return isSuccess;
                } catch (Exception e2) {
                    Log.e(TAG, "Could not parse peer mac address.", e2);
                    return false;
                }
            }
            return false;
        }
    }

    public boolean invite(WifiP2pGroup group, String peerAddress) {
        if (TextUtils.isEmpty(peerAddress)) {
            return false;
        }
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("invite")) {
                return false;
            } else if (group == null) {
                Log.e(TAG, "Cannot invite to null group.");
                return false;
            } else if (group.getOwner() == null) {
                Log.e(TAG, "Cannot invite to group with null owner.");
                return false;
            } else if (group.getOwner().deviceAddress == null) {
                Log.e(TAG, "Group owner has no mac address.");
                return false;
            } else {
                try {
                    byte[] ownerMacAddress = NativeUtil.macAddressToByteArray(group.getOwner().deviceAddress);
                    if (peerAddress == null) {
                        Log.e(TAG, "Cannot parse peer mac address.");
                        return false;
                    }
                    try {
                        byte[] peerMacAddress = NativeUtil.macAddressToByteArray(peerAddress);
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("invite(");
                        stringBuilder.append(group.getInterface());
                        stringBuilder.append(", ");
                        stringBuilder.append(group.getOwner().deviceAddress);
                        stringBuilder.append(", ");
                        stringBuilder.append(peerAddress);
                        stringBuilder.append(")");
                        SupplicantResult<Void> result = new SupplicantResult(stringBuilder.toString());
                        try {
                            result.setResult(this.mISupplicantP2pIface.invite(group.getInterface(), ownerMacAddress, peerMacAddress));
                        } catch (RemoteException e) {
                            String str = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("ISupplicantP2pIface exception: ");
                            stringBuilder2.append(e);
                            Log.e(str, stringBuilder2.toString());
                            supplicantServiceDiedHandler();
                        }
                        boolean isSuccess = result.isSuccess();
                        return isSuccess;
                    } catch (Exception e2) {
                        Log.e(TAG, "Peer mac address parse error.", e2);
                        return false;
                    }
                } catch (Exception e22) {
                    Log.e(TAG, "Group owner mac address parse error.", e22);
                    return false;
                }
            }
        }
    }

    public boolean reject(String peerAddress) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("reject")) {
                return false;
            } else if (peerAddress == null) {
                Log.e(TAG, "Cannot parse rejected peer's mac address.");
                return false;
            } else {
                try {
                    byte[] macAddress = NativeUtil.macAddressToByteArray(peerAddress);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("reject(");
                    stringBuilder.append(peerAddress);
                    stringBuilder.append(")");
                    SupplicantResult<Void> result = new SupplicantResult(stringBuilder.toString());
                    try {
                        result.setResult(this.mISupplicantP2pIface.reject(macAddress));
                    } catch (RemoteException e) {
                        String str = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("ISupplicantP2pIface exception: ");
                        stringBuilder2.append(e);
                        Log.e(str, stringBuilder2.toString());
                        supplicantServiceDiedHandler();
                    }
                    boolean isSuccess = result.isSuccess();
                    return isSuccess;
                } catch (IllegalArgumentException e2) {
                    Log.e(TAG, "IllegalArgumentException Could not parse peer mac address.", e2);
                    return false;
                } catch (Exception e3) {
                    Log.e(TAG, "Could not parse peer mac address.", e3);
                    return false;
                }
            }
        }
    }

    public String getDeviceAddress() {
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("getDeviceAddress")) {
                SupplicantResult<String> result = new SupplicantResult("getDeviceAddress()");
                try {
                    this.mISupplicantP2pIface.getDeviceAddress(new -$$Lambda$SupplicantP2pIfaceHal$WkGSeTaZoJDTkSe2fqKEjLQpWuk(result));
                    String str = (String) result.getResult();
                    return str;
                } catch (RemoteException e) {
                    String str2 = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ISupplicantP2pIface exception: ");
                    stringBuilder.append(e);
                    Log.e(str2, stringBuilder.toString());
                    supplicantServiceDiedHandler();
                    return null;
                }
            }
            return null;
        }
    }

    static /* synthetic */ void lambda$getDeviceAddress$7(SupplicantResult result, SupplicantStatus status, byte[] address) {
        String parsedAddress = null;
        try {
            parsedAddress = NativeUtil.macAddressFromByteArray(address);
        } catch (Exception e) {
            Log.e(TAG, "Could not process reported address.", e);
        }
        result.setResult(status, parsedAddress);
    }

    public String getSsid(String address) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("getSsid")) {
                return null;
            } else if (address == null) {
                Log.e(TAG, "Cannot parse peer mac address.");
                return null;
            } else {
                byte[] macAddress = null;
                try {
                    macAddress = NativeUtil.macAddressToByteArray(address);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getSsid(");
                    stringBuilder.append(address);
                    stringBuilder.append(")");
                    SupplicantResult<String> result = new SupplicantResult(stringBuilder.toString());
                    try {
                        this.mISupplicantP2pIface.getSsid(macAddress, new -$$Lambda$SupplicantP2pIfaceHal$kpewDgmpbiuLCclRVxVxZiaoom8(result));
                        String str = (String) result.getResult();
                        return str;
                    } catch (RemoteException e) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("ISupplicantP2pIface exception: ");
                        stringBuilder2.append(e);
                        Log.e(str2, stringBuilder2.toString());
                        supplicantServiceDiedHandler();
                        return null;
                    }
                } catch (Exception e2) {
                    Log.e(TAG, "Could not parse mac address.", e2);
                    return null;
                }
            }
        }
    }

    static /* synthetic */ void lambda$getSsid$8(SupplicantResult result, SupplicantStatus status, ArrayList ssid) {
        String ssidString = null;
        if (ssid != null) {
            try {
                ssidString = NativeUtil.removeEnclosingQuotes(NativeUtil.encodeSsid(ssid));
            } catch (Exception e) {
                Log.e(TAG, "Could not encode SSID.", e);
            }
        }
        result.setResult(status, ssidString);
    }

    public boolean reinvoke(int networkId, String peerAddress) {
        if (TextUtils.isEmpty(peerAddress) || networkId < 0) {
            return false;
        }
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("reinvoke")) {
                return false;
            } else if (peerAddress == null) {
                Log.e(TAG, "Cannot parse peer mac address.");
                return false;
            } else {
                try {
                    byte[] macAddress = NativeUtil.macAddressToByteArray(peerAddress);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("reinvoke(");
                    stringBuilder.append(networkId);
                    stringBuilder.append(", ");
                    stringBuilder.append(peerAddress);
                    stringBuilder.append(")");
                    byte[] macAddress2 = new SupplicantResult(stringBuilder.toString());
                    try {
                        macAddress2.setResult(this.mISupplicantP2pIface.reinvoke(networkId, macAddress));
                    } catch (RemoteException e) {
                        String str = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("ISupplicantP2pIface exception: ");
                        stringBuilder2.append(e);
                        Log.e(str, stringBuilder2.toString());
                        supplicantServiceDiedHandler();
                    }
                    boolean isSuccess = macAddress2.isSuccess();
                    return isSuccess;
                } catch (Exception e2) {
                    Log.e(TAG, "Could not parse mac address.", e2);
                    return false;
                }
            }
        }
    }

    public boolean groupAdd(int networkId, boolean isPersistent) {
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("groupAdd")) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("groupAdd(");
                stringBuilder.append(networkId);
                stringBuilder.append(", ");
                stringBuilder.append(isPersistent);
                stringBuilder.append(")");
                SupplicantResult<Void> result = new SupplicantResult(stringBuilder.toString());
                try {
                    result.setResult(this.mISupplicantP2pIface.addGroup(isPersistent, networkId));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ISupplicantP2pIface exception: ");
                    stringBuilder2.append(e);
                    Log.e(str, stringBuilder2.toString());
                    supplicantServiceDiedHandler();
                }
                boolean isSuccess = result.isSuccess();
                return isSuccess;
            }
            return false;
        }
    }

    public boolean groupAdd(boolean isPersistent) {
        return groupAdd(-1, isPersistent);
    }

    public boolean groupRemove(String groupName) {
        if (TextUtils.isEmpty(groupName)) {
            return false;
        }
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("groupRemove")) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("groupRemove(");
                stringBuilder.append(groupName);
                stringBuilder.append(")");
                SupplicantResult<Void> result = new SupplicantResult(stringBuilder.toString());
                try {
                    result.setResult(this.mISupplicantP2pIface.removeGroup(groupName));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ISupplicantP2pIface exception: ");
                    stringBuilder2.append(e);
                    Log.e(str, stringBuilder2.toString());
                    supplicantServiceDiedHandler();
                }
                boolean isSuccess = result.isSuccess();
                return isSuccess;
            }
            return false;
        }
    }

    public int getGroupCapability(String peerAddress) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("getGroupCapability")) {
                return -1;
            } else if (peerAddress == null) {
                Log.e(TAG, "Cannot parse peer mac address.");
                return -1;
            } else {
                try {
                    byte[] macAddress = NativeUtil.macAddressToByteArray(peerAddress);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getGroupCapability(");
                    stringBuilder.append(peerAddress);
                    stringBuilder.append(")");
                    SupplicantResult<Integer> capability = new SupplicantResult(stringBuilder.toString());
                    try {
                        this.mISupplicantP2pIface.getGroupCapability(macAddress, new -$$Lambda$SupplicantP2pIfaceHal$fAd_Ie2bVgQhtfKKOMlJdzPJyMU(capability));
                    } catch (RemoteException e) {
                        String str = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("ISupplicantP2pIface exception: ");
                        stringBuilder2.append(e);
                        Log.e(str, stringBuilder2.toString());
                        supplicantServiceDiedHandler();
                    }
                    if (capability.isSuccess()) {
                        int intValue = ((Integer) capability.getResult()).intValue();
                        return intValue;
                    }
                    return -1;
                } catch (IllegalArgumentException e2) {
                    Log.e(TAG, "IllegalArgumentException Could not parse group address.", e2);
                    return -1;
                } catch (Exception e3) {
                    Log.e(TAG, "Could not parse group address.", e3);
                    return -1;
                }
            }
        }
    }

    public boolean configureExtListen(boolean enable, int periodInMillis, int intervalInMillis) {
        if (enable && intervalInMillis < periodInMillis) {
            return false;
        }
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("configureExtListen")) {
                if (!enable) {
                    periodInMillis = 0;
                    intervalInMillis = 0;
                }
                if (periodInMillis >= 0) {
                    if (intervalInMillis >= 0) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("configureExtListen(");
                        stringBuilder.append(periodInMillis);
                        stringBuilder.append(", ");
                        stringBuilder.append(intervalInMillis);
                        stringBuilder.append(")");
                        SupplicantResult<Void> result = new SupplicantResult(stringBuilder.toString());
                        try {
                            result.setResult(this.mISupplicantP2pIface.configureExtListen(periodInMillis, intervalInMillis));
                        } catch (RemoteException e) {
                            String str = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("ISupplicantP2pIface exception: ");
                            stringBuilder2.append(e);
                            Log.e(str, stringBuilder2.toString());
                            supplicantServiceDiedHandler();
                        }
                        boolean isSuccess = result.isSuccess();
                        return isSuccess;
                    }
                }
                String str2 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Invalid parameters supplied to configureExtListen: ");
                stringBuilder3.append(periodInMillis);
                stringBuilder3.append(", ");
                stringBuilder3.append(intervalInMillis);
                Log.e(str2, stringBuilder3.toString());
                return false;
            }
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:46:0x00ec, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean setListenChannel(int listenChannel, int operatingChannel) {
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("setListenChannel")) {
                if (listenChannel >= 1 && listenChannel <= 11) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("setListenChannel(");
                    stringBuilder.append(listenChannel);
                    stringBuilder.append(", ");
                    stringBuilder.append(81);
                    stringBuilder.append(")");
                    SupplicantResult<Void> result = new SupplicantResult(stringBuilder.toString());
                    try {
                        result.setResult(this.mISupplicantP2pIface.setListenChannel(listenChannel, 81));
                    } catch (RemoteException e) {
                        String str = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("ISupplicantP2pIface exception: ");
                        stringBuilder2.append(e);
                        Log.e(str, stringBuilder2.toString());
                        supplicantServiceDiedHandler();
                    }
                    if (!result.isSuccess()) {
                        return false;
                    }
                } else if (listenChannel != 0) {
                    return false;
                }
                if (operatingChannel < 0 || operatingChannel > 165) {
                } else {
                    ArrayList<FreqRange> ranges = new ArrayList();
                    if (operatingChannel >= 1 && operatingChannel <= 165) {
                        int freq = (operatingChannel <= 14 ? 2407 : ScoringParams.BAND5) + (operatingChannel * 5);
                        FreqRange range1 = new FreqRange();
                        range1.min = 1000;
                        range1.max = freq - 5;
                        FreqRange range2 = new FreqRange();
                        range2.min = freq + 5;
                        range2.max = 6000;
                        ranges.add(range1);
                        ranges.add(range2);
                    }
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("setDisallowedFrequencies(");
                    stringBuilder3.append(ranges);
                    stringBuilder3.append(")");
                    SupplicantResult<Void> result2 = new SupplicantResult(stringBuilder3.toString());
                    try {
                        result2.setResult(this.mISupplicantP2pIface.setDisallowedFrequencies(ranges));
                    } catch (RemoteException e2) {
                        String str2 = TAG;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("ISupplicantP2pIface exception: ");
                        stringBuilder4.append(e2);
                        Log.e(str2, stringBuilder4.toString());
                        supplicantServiceDiedHandler();
                    }
                    boolean isSuccess = result2.isSuccess();
                    return isSuccess;
                }
            }
            return false;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:79:0x0159 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x0150 A:{Catch:{ RemoteException -> 0x0134 }} */
    /* JADX WARNING: Missing block: B:72:0x015a, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean serviceAdd(WifiP2pServiceInfo servInfo) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("serviceAdd")) {
                return false;
            } else if (servInfo == null) {
                Log.e(TAG, "Null service info passed.");
                return false;
            } else {
                for (String s : servInfo.getSupplicantQueryList()) {
                    if (s == null) {
                        Log.e(TAG, "Invalid service description (null).");
                        return false;
                    }
                    String[] data = s.split(" ");
                    if (data.length < 3) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Service specification invalid: ");
                        stringBuilder.append(s);
                        Log.e(str, stringBuilder.toString());
                        return false;
                    }
                    ArrayList<Byte> response = null;
                    SupplicantResult<Void> result = null;
                    try {
                        if ("upnp".equals(data[0])) {
                            int version = 0;
                            try {
                                response = Integer.parseInt(data[1], 16);
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("addUpnpService(");
                                stringBuilder2.append(data[1]);
                                stringBuilder2.append(", ");
                                stringBuilder2.append(data[2]);
                                stringBuilder2.append(")");
                                result = new SupplicantResult(stringBuilder2.toString());
                                result.setResult(this.mISupplicantP2pIface.addUpnpService(response, data[2]));
                                if (result != null) {
                                    if (result.isSuccess()) {
                                    }
                                }
                            } catch (NumberFormatException e) {
                                String str2 = TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("UPnP Service specification invalid: ");
                                stringBuilder3.append(s);
                                Log.e(str2, stringBuilder3.toString(), e);
                                return false;
                            }
                        } else if ("bonjour".equals(data[0])) {
                            if (!(data[1] == null || data[2] == null)) {
                                ArrayList<Byte> request = null;
                                try {
                                    request = NativeUtil.byteArrayToArrayList(NativeUtil.hexStringToByteArray(data[1]));
                                    response = NativeUtil.byteArrayToArrayList(NativeUtil.hexStringToByteArray(data[2]));
                                } catch (IllegalArgumentException e2) {
                                    Log.e(TAG, "IllegalArgumentException Invalid argument.");
                                } catch (Exception e3) {
                                    Log.e(TAG, "Invalid bonjour service description.");
                                    return false;
                                }
                                StringBuilder stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("addBonjourService(");
                                stringBuilder4.append(data[1]);
                                stringBuilder4.append(", ");
                                stringBuilder4.append(data[2]);
                                stringBuilder4.append(")");
                                result = new SupplicantResult(stringBuilder4.toString());
                                result.setResult(this.mISupplicantP2pIface.addBonjourService(request, response));
                            }
                            if (result != null) {
                            }
                        } else {
                            return false;
                        }
                    } catch (RemoteException e4) {
                        String str3 = TAG;
                        StringBuilder stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("ISupplicantP2pIface exception: ");
                        stringBuilder5.append(e4);
                        Log.e(str3, stringBuilder5.toString());
                        supplicantServiceDiedHandler();
                    }
                }
                return true;
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:80:0x0157 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x014e A:{Catch:{ RemoteException -> 0x0132 }} */
    /* JADX WARNING: Missing block: B:73:0x0158, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean serviceRemove(WifiP2pServiceInfo servInfo) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("serviceRemove")) {
                return false;
            } else if (servInfo == null) {
                Log.e(TAG, "Null service info passed.");
                return false;
            } else {
                for (String s : servInfo.getSupplicantQueryList()) {
                    if (s == null) {
                        Log.e(TAG, "Invalid service description (null).");
                        return false;
                    }
                    String[] data = s.split(" ");
                    if (data.length < 3) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Service specification invalid: ");
                        stringBuilder.append(s);
                        Log.e(str, stringBuilder.toString());
                        return false;
                    }
                    SupplicantResult<Void> result = null;
                    try {
                        ArrayList<Byte> request;
                        StringBuilder stringBuilder2;
                        if ("upnp".equals(data[0])) {
                            int version = 0;
                            try {
                                request = Integer.parseInt(data[1], 16);
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("removeUpnpService(");
                                stringBuilder2.append(data[1]);
                                stringBuilder2.append(", ");
                                stringBuilder2.append(data[2]);
                                stringBuilder2.append(")");
                                result = new SupplicantResult(stringBuilder2.toString());
                                result.setResult(this.mISupplicantP2pIface.removeUpnpService(request, data[2]));
                                if (result != null) {
                                    if (result.isSuccess()) {
                                    }
                                }
                            } catch (NumberFormatException e) {
                                String str2 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("UPnP Service specification invalid: ");
                                stringBuilder2.append(s);
                                Log.e(str2, stringBuilder2.toString(), e);
                                return false;
                            }
                        } else if ("bonjour".equals(data[0])) {
                            if (data[1] != null) {
                                try {
                                    request = NativeUtil.byteArrayToArrayList(NativeUtil.hexStringToByteArray(data[1]));
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("removeBonjourService(");
                                    stringBuilder2.append(data[1]);
                                    stringBuilder2.append(")");
                                    result = new SupplicantResult(stringBuilder2.toString());
                                    result.setResult(this.mISupplicantP2pIface.removeBonjourService(request));
                                } catch (IllegalArgumentException e2) {
                                    Log.e(TAG, "IllegalArgumentException occur when byteArrayToArrayList");
                                    return false;
                                } catch (Exception e3) {
                                    Log.e(TAG, "Invalid bonjour service description.");
                                    return false;
                                }
                            }
                            if (result != null) {
                            }
                        } else {
                            String str3 = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Unknown / unsupported P2P service requested: ");
                            stringBuilder3.append(data[0]);
                            Log.e(str3, stringBuilder3.toString());
                            return false;
                        }
                    } catch (RemoteException e4) {
                        String str4 = TAG;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("ISupplicantP2pIface exception: ");
                        stringBuilder4.append(e4);
                        Log.e(str4, stringBuilder4.toString());
                        supplicantServiceDiedHandler();
                    }
                }
                return true;
            }
        }
    }

    public String requestServiceDiscovery(String peerAddress, String query) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("requestServiceDiscovery")) {
                return null;
            } else if (peerAddress == null) {
                Log.e(TAG, "Cannot parse peer mac address.");
                return null;
            } else {
                byte[] macAddress = null;
                try {
                    macAddress = NativeUtil.macAddressToByteArray(peerAddress);
                    if (query == null) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Cannot parse service discovery query: ");
                        stringBuilder.append(query);
                        Log.e(str, stringBuilder.toString());
                        return null;
                    }
                    ArrayList<Byte> binQuery = null;
                    try {
                        binQuery = NativeUtil.byteArrayToArrayList(NativeUtil.hexStringToByteArray(query));
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("requestServiceDiscovery(");
                        stringBuilder2.append(peerAddress);
                        stringBuilder2.append(", ");
                        stringBuilder2.append(query);
                        stringBuilder2.append(")");
                        SupplicantResult<Long> result = new SupplicantResult(stringBuilder2.toString());
                        try {
                            this.mISupplicantP2pIface.requestServiceDiscovery(macAddress, binQuery, new -$$Lambda$SupplicantP2pIfaceHal$izMglHV1zYg-bjEUsC4ooS9V9rc(result));
                        } catch (RemoteException e) {
                            String str2 = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("ISupplicantP2pIface exception: ");
                            stringBuilder3.append(e);
                            Log.e(str2, stringBuilder3.toString());
                            supplicantServiceDiedHandler();
                        }
                        Long value = (Long) result.getResult();
                        if (value == null) {
                            return null;
                        }
                        String l = value.toString();
                        return l;
                    } catch (Exception e2) {
                        Log.e(TAG, "Could not parse service query.", e2);
                        return null;
                    }
                } catch (IllegalArgumentException e3) {
                    Log.e(TAG, "IllegalArgumentException Could not process peer MAC address.", e3);
                    return null;
                } catch (Exception e4) {
                    Log.e(TAG, "Could not process peer MAC address.", e4);
                    return null;
                }
            }
        }
    }

    public boolean cancelServiceDiscovery(String identifier) {
        String str;
        StringBuilder stringBuilder;
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("cancelServiceDiscovery")) {
                return false;
            } else if (identifier == null) {
                Log.e(TAG, "cancelServiceDiscovery requires a valid tag.");
                return false;
            } else {
                try {
                    long id = Long.parseLong(identifier);
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("cancelServiceDiscovery(");
                    stringBuilder2.append(identifier);
                    stringBuilder2.append(")");
                    long id2 = new SupplicantResult(stringBuilder2.toString());
                    try {
                        id2.setResult(this.mISupplicantP2pIface.cancelServiceDiscovery(id));
                    } catch (RemoteException e) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("ISupplicantP2pIface exception: ");
                        stringBuilder.append(e);
                        Log.e(str, stringBuilder.toString());
                        supplicantServiceDiedHandler();
                    }
                    boolean isSuccess = id2.isSuccess();
                    return isSuccess;
                } catch (NumberFormatException e2) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Service discovery identifier invalid: ");
                    stringBuilder.append(identifier);
                    Log.e(str, stringBuilder.toString(), e2);
                    return false;
                }
            }
        }
    }

    public boolean setMiracastMode(int mode) {
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("setMiracastMode")) {
                byte targetMode = (byte) 0;
                switch (mode) {
                    case 1:
                        targetMode = (byte) 1;
                        break;
                    case 2:
                        targetMode = (byte) 2;
                        break;
                    default:
                        break;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setMiracastMode(");
                stringBuilder.append(mode);
                stringBuilder.append(")");
                SupplicantResult<Void> result = new SupplicantResult(stringBuilder.toString());
                try {
                    result.setResult(this.mISupplicantP2pIface.setMiracastMode(targetMode));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ISupplicantP2pIface exception: ");
                    stringBuilder2.append(e);
                    Log.e(str, stringBuilder2.toString());
                    supplicantServiceDiedHandler();
                }
                boolean isSuccess = result.isSuccess();
                return isSuccess;
            }
            return false;
        }
    }

    public boolean startWpsPbc(String groupIfName, String bssid) {
        if (TextUtils.isEmpty(groupIfName)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Group name required when requesting WPS PBC. Got (");
            stringBuilder.append(groupIfName);
            stringBuilder.append(")");
            Log.e(str, stringBuilder.toString());
            return false;
        }
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("startWpsPbc")) {
                byte[] bArr = new byte[6];
                bArr = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
                try {
                    byte[] macAddress = NativeUtil.macAddressToByteArray(bssid);
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("startWpsPbc(");
                    stringBuilder2.append(groupIfName);
                    stringBuilder2.append(", ");
                    stringBuilder2.append(bssid);
                    stringBuilder2.append(")");
                    bArr = new SupplicantResult(stringBuilder2.toString());
                    try {
                        bArr.setResult(this.mISupplicantP2pIface.startWpsPbc(groupIfName, macAddress));
                    } catch (RemoteException e) {
                        String str2 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("ISupplicantP2pIface exception: ");
                        stringBuilder3.append(e);
                        Log.e(str2, stringBuilder3.toString());
                        supplicantServiceDiedHandler();
                    }
                    boolean isSuccess = bArr.isSuccess();
                    return isSuccess;
                } catch (Exception e2) {
                    Log.e(TAG, "Could not parse BSSID.", e2);
                    return false;
                }
            }
            return false;
        }
    }

    public boolean startWpsPinKeypad(String groupIfName, String pin) {
        if (TextUtils.isEmpty(groupIfName) || TextUtils.isEmpty(pin)) {
            return false;
        }
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("startWpsPinKeypad")) {
                return false;
            } else if (groupIfName == null) {
                Log.e(TAG, "Group name required when requesting WPS KEYPAD.");
                return false;
            } else if (pin == null) {
                Log.e(TAG, "PIN required when requesting WPS KEYPAD.");
                return false;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("startWpsPinKeypad(");
                stringBuilder.append(groupIfName);
                stringBuilder.append(", ");
                stringBuilder.append(pin);
                stringBuilder.append(")");
                SupplicantResult<Void> result = new SupplicantResult(stringBuilder.toString());
                try {
                    result.setResult(this.mISupplicantP2pIface.startWpsPinKeypad(groupIfName, pin));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ISupplicantP2pIface exception: ");
                    stringBuilder2.append(e);
                    Log.e(str, stringBuilder2.toString());
                    supplicantServiceDiedHandler();
                }
                boolean isSuccess = result.isSuccess();
                return isSuccess;
            }
        }
    }

    public String startWpsPinDisplay(String groupIfName, String bssid) {
        if (TextUtils.isEmpty(groupIfName)) {
            return null;
        }
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("startWpsPinDisplay")) {
                return null;
            } else if (groupIfName == null) {
                Log.e(TAG, "Group name required when requesting WPS KEYPAD.");
                return null;
            } else {
                byte[] bArr = new byte[6];
                bArr = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
                try {
                    byte[] macAddress = NativeUtil.macAddressToByteArray(bssid);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("startWpsPinDisplay(");
                    stringBuilder.append(groupIfName);
                    stringBuilder.append(", ");
                    stringBuilder.append(bssid);
                    stringBuilder.append(")");
                    bArr = new SupplicantResult(stringBuilder.toString());
                    try {
                        this.mISupplicantP2pIface.startWpsPinDisplay(groupIfName, macAddress, new -$$Lambda$SupplicantP2pIfaceHal$FjqymoOlHfh38YnKZwagVaL8Jog(bArr));
                    } catch (RemoteException e) {
                        String str = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("ISupplicantP2pIface exception: ");
                        stringBuilder2.append(e);
                        Log.e(str, stringBuilder2.toString());
                        supplicantServiceDiedHandler();
                    }
                    String str2 = (String) bArr.getResult();
                    return str2;
                } catch (Exception e2) {
                    Log.e(TAG, "Could not parse BSSID.", e2);
                    return null;
                }
            }
        }
    }

    public boolean cancelWps(String groupIfName) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("cancelWps")) {
                return false;
            } else if (groupIfName == null) {
                Log.e(TAG, "Group name required when requesting WPS KEYPAD.");
                return false;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("cancelWps(");
                stringBuilder.append(groupIfName);
                stringBuilder.append(")");
                SupplicantResult<Void> result = new SupplicantResult(stringBuilder.toString());
                try {
                    result.setResult(this.mISupplicantP2pIface.cancelWps(groupIfName));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ISupplicantP2pIface exception: ");
                    stringBuilder2.append(e);
                    Log.e(str, stringBuilder2.toString());
                    supplicantServiceDiedHandler();
                }
                boolean isSuccess = result.isSuccess();
                return isSuccess;
            }
        }
    }

    public boolean enableWfd(boolean enable) {
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("enableWfd")) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("enableWfd(");
                stringBuilder.append(enable);
                stringBuilder.append(")");
                SupplicantResult<Void> result = new SupplicantResult(stringBuilder.toString());
                try {
                    result.setResult(this.mISupplicantP2pIface.enableWfd(enable));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ISupplicantP2pIface exception: ");
                    stringBuilder2.append(e);
                    Log.e(str, stringBuilder2.toString());
                    supplicantServiceDiedHandler();
                }
                boolean isSuccess = result.isSuccess();
                return isSuccess;
            }
            return false;
        }
    }

    public boolean setWfdDeviceInfo(String info) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("setWfdDeviceInfo")) {
                return false;
            } else if (info == null) {
                Log.e(TAG, "Cannot parse null WFD info string.");
                return false;
            } else {
                try {
                    byte[] wfdInfo = NativeUtil.hexStringToByteArray(info);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("setWfdDeviceInfo(");
                    stringBuilder.append(info);
                    stringBuilder.append(")");
                    SupplicantResult<Void> result = new SupplicantResult(stringBuilder.toString());
                    try {
                        result.setResult(this.mISupplicantP2pIface.setWfdDeviceInfo(wfdInfo));
                    } catch (RemoteException e) {
                        String str = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("ISupplicantP2pIface exception: ");
                        stringBuilder2.append(e);
                        Log.e(str, stringBuilder2.toString());
                        supplicantServiceDiedHandler();
                    }
                    boolean isSuccess = result.isSuccess();
                    return isSuccess;
                } catch (Exception e2) {
                    Log.e(TAG, "Could not parse WFD Device Info string.");
                    return false;
                }
            }
        }
    }

    public boolean removeNetwork(int networkId) {
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("removeNetwork")) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("removeNetwork(");
                stringBuilder.append(networkId);
                stringBuilder.append(")");
                SupplicantResult<Void> result = new SupplicantResult(stringBuilder.toString());
                try {
                    result.setResult(this.mISupplicantP2pIface.removeNetwork(networkId));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ISupplicantP2pIface exception: ");
                    stringBuilder2.append(e);
                    Log.e(str, stringBuilder2.toString());
                    supplicantServiceDiedHandler();
                }
                boolean isSuccess = result.isSuccess();
                return isSuccess;
            }
            return false;
        }
    }

    private List<Integer> listNetworks() {
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("listNetworks")) {
                SupplicantResult<ArrayList> result = new SupplicantResult("listNetworks()");
                try {
                    this.mISupplicantP2pIface.listNetworks(new -$$Lambda$SupplicantP2pIfaceHal$EtDVjv9sBbwd_VKqTeizuRtV3z4(result));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ISupplicantP2pIface exception: ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                    supplicantServiceDiedHandler();
                }
                List list = (List) result.getResult();
                return list;
            }
            return null;
        }
    }

    private ISupplicantP2pNetwork getNetwork(int networkId) {
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("getNetwork")) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getNetwork(");
                stringBuilder.append(networkId);
                stringBuilder.append(")");
                SupplicantResult<ISupplicantNetwork> result = new SupplicantResult(stringBuilder.toString());
                try {
                    this.mISupplicantP2pIface.getNetwork(networkId, new -$$Lambda$SupplicantP2pIfaceHal$PuXtgEcUoHfMGA1SHt2CZh5_b1Q(result));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ISupplicantP2pIface exception: ");
                    stringBuilder2.append(e);
                    Log.e(str, stringBuilder2.toString());
                    supplicantServiceDiedHandler();
                }
                if (result.getResult() == null) {
                    Log.e(TAG, "getNetwork got null network");
                    return null;
                }
                ISupplicantP2pNetwork p2pNetworkMockable = getP2pNetworkMockable((ISupplicantNetwork) result.getResult());
                return p2pNetworkMockable;
            }
            return null;
        }
    }

    /* JADX WARNING: Missing block: B:68:0x01db, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean loadGroups(WifiP2pGroupList groups) {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("loadGroups")) {
                List<Integer> networkIds = listNetworks();
                if (networkIds != null) {
                    if (!networkIds.isEmpty()) {
                        for (Integer networkId : networkIds) {
                            ISupplicantP2pNetwork network = getNetwork(networkId.intValue());
                            StringBuilder stringBuilder3;
                            if (network == null) {
                                String str = TAG;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("Failed to retrieve network object for ");
                                stringBuilder3.append(networkId);
                                Log.e(str, stringBuilder3.toString());
                            } else {
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("isCurrent(");
                                stringBuilder3.append(networkId);
                                stringBuilder3.append(")");
                                SupplicantResult<Boolean> resultIsCurrent = new SupplicantResult(stringBuilder3.toString());
                                try {
                                    network.isCurrent(new -$$Lambda$SupplicantP2pIfaceHal$DZ5hjM0K-k-jbWASpzD6nJ3e6xU(resultIsCurrent));
                                } catch (RemoteException e) {
                                    String str2 = TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("ISupplicantP2pIface exception: ");
                                    stringBuilder.append(e);
                                    Log.e(str2, stringBuilder.toString());
                                    supplicantServiceDiedHandler();
                                }
                                if (resultIsCurrent.isSuccess()) {
                                    if (!((Boolean) resultIsCurrent.getResult()).booleanValue()) {
                                        WifiP2pGroup group = new WifiP2pGroup();
                                        group.setNetworkId(networkId.intValue());
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("getSsid(");
                                        stringBuilder.append(networkId);
                                        stringBuilder.append(")");
                                        SupplicantResult<ArrayList> resultSsid = new SupplicantResult(stringBuilder.toString());
                                        try {
                                            network.getSsid(new -$$Lambda$SupplicantP2pIfaceHal$JzKiJ4oLypdiaI_2kjk3anuHsPQ(resultSsid));
                                        } catch (RemoteException e2) {
                                            String str3 = TAG;
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("ISupplicantP2pIface exception: ");
                                            stringBuilder2.append(e2);
                                            Log.e(str3, stringBuilder2.toString());
                                            supplicantServiceDiedHandler();
                                        }
                                        if (!(!resultSsid.isSuccess() || resultSsid.getResult() == null || ((ArrayList) resultSsid.getResult()).isEmpty())) {
                                            group.setNetworkName(NativeUtil.removeEnclosingQuotes(NativeUtil.encodeSsid((ArrayList) resultSsid.getResult())));
                                        }
                                        StringBuilder stringBuilder4 = new StringBuilder();
                                        stringBuilder4.append("getBssid(");
                                        stringBuilder4.append(networkId);
                                        stringBuilder4.append(")");
                                        SupplicantResult<byte[]> resultBssid = new SupplicantResult(stringBuilder4.toString());
                                        try {
                                            network.getBssid(new -$$Lambda$SupplicantP2pIfaceHal$dFKn5oY7OFr4d91vo-vY6YUffTI(resultBssid));
                                        } catch (RemoteException e3) {
                                            String str4 = TAG;
                                            StringBuilder stringBuilder5 = new StringBuilder();
                                            stringBuilder5.append("ISupplicantP2pIface exception: ");
                                            stringBuilder5.append(e3);
                                            Log.e(str4, stringBuilder5.toString());
                                            supplicantServiceDiedHandler();
                                        }
                                        if (resultBssid.isSuccess() && !ArrayUtils.isEmpty((byte[]) resultBssid.getResult())) {
                                            WifiP2pDevice device = new WifiP2pDevice();
                                            device.deviceAddress = NativeUtil.macAddressFromByteArray((byte[]) resultBssid.getResult());
                                            group.setOwner(device);
                                        }
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("isGo(");
                                        stringBuilder2.append(networkId);
                                        stringBuilder2.append(")");
                                        SupplicantResult<Boolean> resultIsGo = new SupplicantResult(stringBuilder2.toString());
                                        try {
                                            network.isGo(new -$$Lambda$SupplicantP2pIfaceHal$NNtqsQiP2_K4VCIPid6vvSLYwJg(resultIsGo));
                                        } catch (RemoteException e4) {
                                            String str5 = TAG;
                                            StringBuilder stringBuilder6 = new StringBuilder();
                                            stringBuilder6.append("ISupplicantP2pIface exception: ");
                                            stringBuilder6.append(e4);
                                            Log.e(str5, stringBuilder6.toString());
                                            supplicantServiceDiedHandler();
                                        }
                                        if (resultIsGo.isSuccess()) {
                                            group.setIsGroupOwner(((Boolean) resultIsGo.getResult()).booleanValue());
                                        }
                                        groups.add(group);
                                    }
                                }
                                Log.i(TAG, "Skipping current network");
                            }
                        }
                        return true;
                    }
                }
            } else {
                return false;
            }
        }
    }

    public boolean setWpsDeviceName(String name) {
        if (name == null) {
            return false;
        }
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("setWpsDeviceName")) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setWpsDeviceName(");
                stringBuilder.append(name);
                stringBuilder.append(")");
                SupplicantResult<Void> result = new SupplicantResult(stringBuilder.toString());
                try {
                    result.setResult(this.mISupplicantP2pIface.setWpsDeviceName(name));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ISupplicantP2pIface exception: ");
                    stringBuilder2.append(e);
                    Log.e(str, stringBuilder2.toString());
                    supplicantServiceDiedHandler();
                }
                boolean isSuccess = result.isSuccess();
                return isSuccess;
            }
            return false;
        }
    }

    public boolean setWpsDeviceType(String typeStr) {
        String str;
        StringBuilder stringBuilder;
        try {
            Matcher match = WPS_DEVICE_TYPE_PATTERN.matcher(typeStr);
            if (match.find()) {
                if (match.groupCount() == 3) {
                    short categ = Short.parseShort(match.group(1));
                    byte[] oui = NativeUtil.hexStringToByteArray(match.group(2));
                    short subCateg = Short.parseShort(match.group(3));
                    byte[] bytes = new byte[8];
                    ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
                    byteBuffer.putShort(categ);
                    byteBuffer.put(oui);
                    byteBuffer.putShort(subCateg);
                    synchronized (this.mLock) {
                        if (checkSupplicantP2pIfaceAndLogFailure("setWpsDeviceType")) {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("setWpsDeviceType(");
                            stringBuilder2.append(typeStr);
                            stringBuilder2.append(")");
                            SupplicantResult<Void> result = new SupplicantResult(stringBuilder2.toString());
                            try {
                                result.setResult(this.mISupplicantP2pIface.setWpsDeviceType(bytes));
                            } catch (RemoteException e) {
                                String str2 = TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("ISupplicantP2pIface exception: ");
                                stringBuilder3.append(e);
                                Log.e(str2, stringBuilder3.toString());
                                supplicantServiceDiedHandler();
                            }
                            boolean isSuccess = result.isSuccess();
                            return isSuccess;
                        }
                        return false;
                    }
                }
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Malformed WPS device type ");
            stringBuilder.append(typeStr);
            Log.e(str, stringBuilder.toString());
            return false;
        } catch (IllegalArgumentException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal argument ");
            stringBuilder.append(typeStr);
            Log.e(str, stringBuilder.toString(), e2);
            return false;
        }
    }

    public boolean setWpsConfigMethods(String configMethodsStr) {
        synchronized (this.mLock) {
            int i = 0;
            if (checkSupplicantP2pIfaceAndLogFailure("setWpsConfigMethods")) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setWpsConfigMethods(");
                stringBuilder.append(configMethodsStr);
                stringBuilder.append(")");
                SupplicantResult<Void> result = new SupplicantResult(stringBuilder.toString());
                short configMethodsMask = (short) 0;
                String[] configMethodsStrArr = configMethodsStr.split("\\s+");
                while (i < configMethodsStrArr.length) {
                    configMethodsMask = (short) (stringToWpsConfigMethod(configMethodsStrArr[i]) | configMethodsMask);
                    i++;
                }
                try {
                    result.setResult(this.mISupplicantP2pIface.setWpsConfigMethods(configMethodsMask));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ISupplicantP2pIface exception: ");
                    stringBuilder2.append(e);
                    Log.e(str, stringBuilder2.toString());
                    supplicantServiceDiedHandler();
                }
                boolean isSuccess = result.isSuccess();
                return isSuccess;
            }
            return false;
        }
    }

    public String getNfcHandoverRequest() {
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("getNfcHandoverRequest")) {
                SupplicantResult<ArrayList> result = new SupplicantResult("getNfcHandoverRequest()");
                try {
                    this.mISupplicantP2pIface.createNfcHandoverRequestMessage(new -$$Lambda$SupplicantP2pIfaceHal$E4Spq_q7PRsXiNIycR53oa-9H68(result));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ISupplicantP2pIface exception: ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                    supplicantServiceDiedHandler();
                }
                if (result.isSuccess()) {
                    String hexStringFromByteArray = NativeUtil.hexStringFromByteArray(NativeUtil.byteArrayFromArrayList((ArrayList) result.getResult()));
                    return hexStringFromByteArray;
                }
                return null;
            }
            return null;
        }
    }

    public String getNfcHandoverSelect() {
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("getNfcHandoverSelect")) {
                SupplicantResult<ArrayList> result = new SupplicantResult("getNfcHandoverSelect()");
                try {
                    this.mISupplicantP2pIface.createNfcHandoverSelectMessage(new -$$Lambda$SupplicantP2pIfaceHal$ek6pjXj3dGTF-HTMzJ4YwbyD3Dc(result));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ISupplicantP2pIface exception: ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                    supplicantServiceDiedHandler();
                }
                if (result.isSuccess()) {
                    String hexStringFromByteArray = NativeUtil.hexStringFromByteArray(NativeUtil.byteArrayFromArrayList((ArrayList) result.getResult()));
                    return hexStringFromByteArray;
                }
                return null;
            }
            return null;
        }
    }

    public boolean initiatorReportNfcHandover(String selectMessage) {
        if (selectMessage == null) {
            return false;
        }
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("initiatorReportNfcHandover")) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("initiatorReportNfcHandover(");
                stringBuilder.append(selectMessage);
                stringBuilder.append(")");
                SupplicantResult<Void> result = new SupplicantResult(stringBuilder.toString());
                try {
                    result.setResult(this.mISupplicantP2pIface.reportNfcHandoverInitiation(NativeUtil.byteArrayToArrayList(NativeUtil.hexStringToByteArray(selectMessage))));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ISupplicantP2pIface exception: ");
                    stringBuilder2.append(e);
                    Log.e(str, stringBuilder2.toString());
                    supplicantServiceDiedHandler();
                } catch (IllegalArgumentException e2) {
                    String str2 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Illegal argument ");
                    stringBuilder3.append(selectMessage);
                    Log.e(str2, stringBuilder3.toString(), e2);
                    return false;
                }
                boolean isSuccess = result.isSuccess();
                return isSuccess;
            }
            return false;
        }
    }

    public boolean responderReportNfcHandover(String requestMessage) {
        if (requestMessage == null) {
            return false;
        }
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("responderReportNfcHandover")) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("responderReportNfcHandover(");
                stringBuilder.append(requestMessage);
                stringBuilder.append(")");
                SupplicantResult<Void> result = new SupplicantResult(stringBuilder.toString());
                try {
                    result.setResult(this.mISupplicantP2pIface.reportNfcHandoverResponse(NativeUtil.byteArrayToArrayList(NativeUtil.hexStringToByteArray(requestMessage))));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ISupplicantP2pIface exception: ");
                    stringBuilder2.append(e);
                    Log.e(str, stringBuilder2.toString());
                    supplicantServiceDiedHandler();
                } catch (IllegalArgumentException e2) {
                    String str2 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Illegal argument ");
                    stringBuilder3.append(requestMessage);
                    Log.e(str2, stringBuilder3.toString(), e2);
                    return false;
                }
                boolean isSuccess = result.isSuccess();
                return isSuccess;
            }
            return false;
        }
    }

    public boolean setClientList(int networkId, String clientListStr) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("setClientList")) {
                return false;
            } else if (TextUtils.isEmpty(clientListStr)) {
                Log.e(TAG, "Invalid client list");
                return false;
            } else {
                ISupplicantP2pNetwork network = getNetwork(networkId);
                if (network == null) {
                    Log.e(TAG, "Invalid network id ");
                    return false;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setClientList(");
                stringBuilder.append(networkId);
                stringBuilder.append(", ");
                stringBuilder.append(clientListStr);
                stringBuilder.append(")");
                SupplicantResult<Void> result = new SupplicantResult(stringBuilder.toString());
                try {
                    ArrayList<byte[]> clients = new ArrayList();
                    for (String clientStr : Arrays.asList(clientListStr.split("\\s+"))) {
                        clients.add(NativeUtil.macAddressToByteArray(clientStr));
                    }
                    result.setResult(network.setClientList(clients));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ISupplicantP2pIface exception: ");
                    stringBuilder2.append(e);
                    Log.e(str, stringBuilder2.toString());
                    supplicantServiceDiedHandler();
                } catch (IllegalArgumentException e2) {
                    String str2 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Illegal argument ");
                    stringBuilder3.append(clientListStr);
                    Log.e(str2, stringBuilder3.toString(), e2);
                    return false;
                }
                boolean isSuccess = result.isSuccess();
                return isSuccess;
            }
        }
    }

    public String getClientList(int networkId) {
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("getClientList")) {
                ISupplicantP2pNetwork network = getNetwork(networkId);
                if (network == null) {
                    Log.e(TAG, "Invalid network id ");
                    return null;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getClientList(");
                stringBuilder.append(networkId);
                stringBuilder.append(")");
                SupplicantResult<ArrayList> result = new SupplicantResult(stringBuilder.toString());
                try {
                    network.getClientList(new -$$Lambda$SupplicantP2pIfaceHal$bXMI596Kq7T2WYp5S1uvxkboxxk(result));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ISupplicantP2pIface exception: ");
                    stringBuilder2.append(e);
                    Log.e(str, stringBuilder2.toString());
                    supplicantServiceDiedHandler();
                }
                if (result.isSuccess()) {
                    String str2 = (String) ((ArrayList) result.getResult()).stream().map(-$$Lambda$22Qhg7RQJlX-ihi83tqGgsfF-Ms.INSTANCE).collect(Collectors.joining(" "));
                    return str2;
                }
                return null;
            }
            return null;
        }
    }

    public boolean saveConfig() {
        synchronized (this.mLock) {
            if (checkSupplicantP2pIfaceAndLogFailure("saveConfig")) {
                SupplicantResult<Void> result = new SupplicantResult("saveConfig()");
                try {
                    result.setResult(this.mISupplicantP2pIface.saveConfig());
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ISupplicantP2pIface exception: ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                    supplicantServiceDiedHandler();
                }
                boolean isSuccess = result.isSuccess();
                return isSuccess;
            }
            return false;
        }
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static short stringToWpsConfigMethod(String configMethod) {
        short s;
        switch (configMethod.hashCode()) {
            case -1781962557:
                if (configMethod.equals("virtual_push_button")) {
                    s = (short) 9;
                    break;
                }
            case -1419358249:
                if (configMethod.equals("ethernet")) {
                    s = (short) 1;
                    break;
                }
            case -1134657068:
                if (configMethod.equals("keypad")) {
                    s = (short) 8;
                    break;
                }
            case -614489202:
                if (configMethod.equals("virtual_display")) {
                    s = (short) 12;
                    break;
                }
            case -522593958:
                if (configMethod.equals("physical_display")) {
                    s = (short) 13;
                    break;
                }
            case -423872603:
                if (configMethod.equals("nfc_interface")) {
                    s = (short) 6;
                    break;
                }
            case -416734217:
                if (configMethod.equals("push_button")) {
                    s = (short) 7;
                    break;
                }
            case 3388229:
                if (configMethod.equals("p2ps")) {
                    s = (short) 11;
                    break;
                }
            case 3599197:
                if (configMethod.equals("usba")) {
                    s = (short) 0;
                    break;
                }
            case 102727412:
                if (configMethod.equals("label")) {
                    s = (short) 2;
                    break;
                }
            case 179612103:
                if (configMethod.equals("ext_nfc_token")) {
                    s = (short) 5;
                    break;
                }
            case 1146869903:
                if (configMethod.equals("physical_push_button")) {
                    s = (short) 10;
                    break;
                }
            case 1671764162:
                if (configMethod.equals("display")) {
                    s = (short) 3;
                    break;
                }
            case 2010140181:
                if (configMethod.equals("int_nfc_token")) {
                    s = (short) 4;
                    break;
                }
            default:
                s = (short) -1;
                break;
        }
        switch (s) {
            case (short) 0:
                return (short) 1;
            case (short) 1:
                return (short) 2;
            case (short) 2:
                return (short) 4;
            case (short) 3:
                return (short) 8;
            case (short) 4:
                return (short) 32;
            case (short) 5:
                return (short) 16;
            case (short) 6:
                return (short) 64;
            case (short) 7:
                return WpsConfigMethods.PUSHBUTTON;
            case (short) 8:
                return WpsConfigMethods.KEYPAD;
            case (short) 9:
                return WpsConfigMethods.VIRT_PUSHBUTTON;
            case (short) 10:
                return WpsConfigMethods.PHY_PUSHBUTTON;
            case (short) 11:
                return WpsConfigMethods.P2PS;
            case (short) 12:
                return WpsConfigMethods.VIRT_DISPLAY;
            case (short) 13:
                return WpsConfigMethods.PHY_DISPLAY;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid WPS config method: ");
                stringBuilder.append(configMethod);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private boolean trySetupForVendorV2_0(ISupplicantIface ifaceHwBinder, String ifaceName) {
        ISupplicantP2pIface supplicantP2pIface = vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantP2pIface.castFrom(ifaceHwBinder);
        if (supplicantP2pIface == null) {
            return false;
        }
        Log.i(TAG, "Start to setup vendor ISupplicantP2pIface");
        this.mISupplicantP2pIface = supplicantP2pIface;
        if (!linkToSupplicantP2pIfaceDeath()) {
            return false;
        }
        if (this.mISupplicantP2pIface == null || this.mMonitor == null || hwP2pRegisterCallback(new VendorSupplicantP2pIfaceHalCallbackV2_0(ifaceName, this.mMonitor, new SupplicantP2pIfaceCallback(ifaceName, this.mMonitor)))) {
            Log.i(TAG, "Successfully setup vendor ISupplicantP2pIface");
            return true;
        }
        Log.e(TAG, "Vendor callback registration failed. Initialization incomplete.");
        return false;
    }

    private vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantP2pIface checkVendorSupplicantP2pIfaceAndLogFailure(String method) {
        if (this.mISupplicantP2pIface == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can't call ");
            stringBuilder.append(method);
            stringBuilder.append(": ISupplicantP2pIface is null");
            Log.e(str, stringBuilder.toString());
            return null;
        }
        vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantP2pIface vendorP2pIface = vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantP2pIface.castFrom(this.mISupplicantP2pIface);
        if (vendorP2pIface != null) {
            return vendorP2pIface;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Can't call ");
        stringBuilder2.append(method);
        stringBuilder2.append(": fail to cast ISupplicantP2pIface to vendor 2.0");
        Log.e(str2, stringBuilder2.toString());
        return null;
    }

    private boolean hwP2pRegisterCallback(ISupplicantP2pIfaceCallback receiver) {
        synchronized (this.mLock) {
            vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantP2pIface vendorP2pIface = checkVendorSupplicantP2pIfaceAndLogFailure("hwP2pRegisterCallback");
            if (vendorP2pIface == null) {
                return false;
            }
            SupplicantResult<Void> result = new SupplicantResult("hwP2pRegisterCallback()");
            try {
                result.setResult(vendorP2pIface.hwP2pRegisterCallback(receiver));
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ISupplicantP2pIface exception: ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                supplicantServiceDiedHandler();
            }
            boolean isSuccess = result.isSuccess();
            return isSuccess;
        }
    }

    public boolean groupAddWithFreq(int networkId, boolean isPersistent, String freq) {
        synchronized (this.mLock) {
            vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantP2pIface vendorP2pIface = checkVendorSupplicantP2pIfaceAndLogFailure("hwP2pRegisterCallback");
            if (vendorP2pIface == null) {
                return false;
            } else if (freq == null) {
                Log.e(TAG, "freq try to create is null");
                return false;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("groupAddWithFreq(");
                stringBuilder.append(networkId);
                stringBuilder.append(", ");
                stringBuilder.append(isPersistent);
                stringBuilder.append(", ");
                stringBuilder.append(freq);
                stringBuilder.append(")");
                SupplicantResult<Void> result = new SupplicantResult(stringBuilder.toString());
                try {
                    result.setResult(vendorP2pIface.addGroupWithFreq(isPersistent, networkId, freq));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ISupplicantP2pIface exception: ");
                    stringBuilder2.append(e);
                    Log.e(str, stringBuilder2.toString());
                    supplicantServiceDiedHandler();
                }
                boolean isSuccess = result.isSuccess();
                return isSuccess;
            }
        }
    }

    public boolean magiclinkConnect(String cmd) {
        synchronized (this.mLock) {
            vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantP2pIface vendorP2pIface = checkVendorSupplicantP2pIfaceAndLogFailure("hwP2pRegisterCallback");
            if (vendorP2pIface == null) {
                return false;
            } else if (cmd == null) {
                Log.e(TAG, "cmd try to connect is null");
                return false;
            } else {
                SupplicantResult<Void> result = new SupplicantResult("magiclinkConnect([secrecy parameters])");
                try {
                    result.setResult(vendorP2pIface.magiclinkConnect(cmd));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ISupplicantP2pIface exception: ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                    supplicantServiceDiedHandler();
                }
                boolean isSuccess = result.isSuccess();
                return isSuccess;
            }
        }
    }

    public boolean addP2pRptGroup(String config) {
        synchronized (this.mLock) {
            vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantP2pIface vendorP2pIface = checkVendorSupplicantP2pIfaceAndLogFailure("hwP2pRegisterCallback");
            if (vendorP2pIface == null) {
                return false;
            }
            SupplicantResult<Void> result = new SupplicantResult("rptP2pAddGroup()");
            try {
                result.setResult(vendorP2pIface.rptP2pAddGroup(config));
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ISupplicantP2pIface exception: ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                supplicantServiceDiedHandler();
            }
            boolean isSuccess = result.isSuccess();
            return isSuccess;
        }
    }

    public int getP2pLinkspeed(String ifaceName) {
        synchronized (this.mLock) {
            if (TextUtils.isEmpty(ifaceName)) {
                return -1;
            }
            vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantP2pIface vendorP2pIface = checkVendorSupplicantP2pIfaceAndLogFailure("hwP2pRegisterCallback");
            if (vendorP2pIface == null) {
                return -1;
            }
            SupplicantResult<Integer> result = new SupplicantResult("getP2pLinkspeed()");
            try {
                vendorP2pIface.getP2pLinkspeed(ifaceName, new -$$Lambda$SupplicantP2pIfaceHal$3zapzn-CcC1guWThXscIB8Q43MM(result));
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ISupplicantP2pIface exception: ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                supplicantServiceDiedHandler();
            }
            if (result.isSuccess()) {
                int intValue = ((Integer) result.getResult()).intValue();
                return intValue;
            }
            return -1;
        }
    }
}
