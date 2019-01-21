package com.android.server.wifi;

import android.content.Context;
import android.hardware.wifi.hostapd.V1_0.HostapdStatus;
import android.hardware.wifi.hostapd.V1_0.IHostapd;
import android.hardware.wifi.hostapd.V1_0.IHostapd.IfaceParams;
import android.hardware.wifi.hostapd.V1_0.IHostapd.NetworkParams;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.hidl.manager.V1_0.IServiceNotification.Stub;
import android.net.wifi.WifiConfiguration;
import android.os.IHwBinder.DeathRecipient;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.WifiNative.HostapdDeathEventHandler;
import com.android.server.wifi.util.NativeUtil;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class HostapdHal {
    private static final String TAG = "HostapdHal";
    private HostapdDeathEventHandler mDeathEventHandler;
    private final boolean mEnableAcs;
    private final boolean mEnableIeee80211AC;
    private final DeathRecipient mHostapdDeathRecipient = new -$$Lambda$HostapdHal$BanSRPFiiwZZpFD4d63QpeU1xBA(this);
    private IHostapd mIHostapd;
    private IServiceManager mIServiceManager = null;
    private final Object mLock = new Object();
    private final DeathRecipient mServiceManagerDeathRecipient = new -$$Lambda$HostapdHal$ykyXfQPF5iy3e1W0s1ikBBPfH-Y(this);
    private final IServiceNotification mServiceNotificationCallback = new Stub() {
        public void onRegistration(String fqName, String name, boolean preexisting) {
            synchronized (HostapdHal.this.mLock) {
                if (HostapdHal.this.mVerboseLoggingEnabled) {
                    String str = HostapdHal.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("IServiceNotification.onRegistration for: ");
                    stringBuilder.append(fqName);
                    stringBuilder.append(", ");
                    stringBuilder.append(name);
                    stringBuilder.append(" preexisting=");
                    stringBuilder.append(preexisting);
                    Log.i(str, stringBuilder.toString());
                }
                if (HostapdHal.this.initHostapdService()) {
                    Log.i(HostapdHal.TAG, "Completed initialization of IHostapd.");
                } else {
                    Log.e(HostapdHal.TAG, "initalizing IHostapd failed.");
                    HostapdHal.this.hostapdServiceDiedHandler();
                }
            }
        }
    };
    private boolean mVerboseLoggingEnabled = false;

    public static /* synthetic */ void lambda$new$0(HostapdHal hostapdHal, long cookie) {
        synchronized (hostapdHal.mLock) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("IServiceManager died: cookie=");
            stringBuilder.append(cookie);
            Log.w(str, stringBuilder.toString());
            hostapdHal.hostapdServiceDiedHandler();
            hostapdHal.mIServiceManager = null;
        }
    }

    public static /* synthetic */ void lambda$new$1(HostapdHal hostapdHal, long cookie) {
        synchronized (hostapdHal.mLock) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("IHostapd/IHostapd died: cookie=");
            stringBuilder.append(cookie);
            Log.w(str, stringBuilder.toString());
            hostapdHal.hostapdServiceDiedHandler();
        }
    }

    public HostapdHal(Context context) {
        this.mEnableAcs = context.getResources().getBoolean(17957086);
        this.mEnableIeee80211AC = context.getResources().getBoolean(17957087);
    }

    void enableVerboseLogging(boolean enable) {
        synchronized (this.mLock) {
            this.mVerboseLoggingEnabled = enable;
        }
    }

    private boolean linkToServiceManagerDeath() {
        synchronized (this.mLock) {
            if (this.mIServiceManager == null) {
                return false;
            }
            try {
                if (this.mIServiceManager.linkToDeath(this.mServiceManagerDeathRecipient, 0)) {
                    return true;
                }
                Log.wtf(TAG, "Error on linkToDeath on IServiceManager");
                hostapdServiceDiedHandler();
                this.mIServiceManager = null;
                return false;
            } catch (RemoteException e) {
                Log.e(TAG, "IServiceManager.linkToDeath exception", e);
                this.mIServiceManager = null;
                return false;
            }
        }
    }

    public boolean initialize() {
        synchronized (this.mLock) {
            if (this.mVerboseLoggingEnabled) {
                Log.i(TAG, "Registering IHostapd service ready callback.");
            }
            this.mIHostapd = null;
            if (this.mIServiceManager != null) {
                return true;
            }
            try {
                this.mIServiceManager = getServiceManagerMockable();
                if (this.mIServiceManager == null) {
                    Log.e(TAG, "Failed to get HIDL Service Manager");
                    return false;
                } else if (!linkToServiceManagerDeath()) {
                    return false;
                } else if (this.mIServiceManager.registerForNotifications(IHostapd.kInterfaceName, "", this.mServiceNotificationCallback)) {
                    return true;
                } else {
                    Log.e(TAG, "Failed to register for notifications to android.hardware.wifi.hostapd@1.0::IHostapd");
                    this.mIServiceManager = null;
                    return false;
                }
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception while trying to register a listener for IHostapd service: ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                hostapdServiceDiedHandler();
                this.mIServiceManager = null;
                return false;
            }
        }
    }

    private boolean linkToHostapdDeath() {
        synchronized (this.mLock) {
            if (this.mIHostapd == null) {
                return false;
            }
            try {
                if (this.mIHostapd.linkToDeath(this.mHostapdDeathRecipient, 0)) {
                    return true;
                }
                Log.wtf(TAG, "Error on linkToDeath on IHostapd");
                hostapdServiceDiedHandler();
                return false;
            } catch (RemoteException e) {
                Log.e(TAG, "IHostapd.linkToDeath exception", e);
                return false;
            }
        }
    }

    private boolean initHostapdService() {
        synchronized (this.mLock) {
            try {
                this.mIHostapd = getHostapdMockable();
                if (this.mIHostapd == null) {
                    Log.e(TAG, "Got null IHostapd service. Stopping hostapd HIDL startup");
                    return false;
                } else if (linkToHostapdDeath()) {
                    return true;
                } else {
                    return false;
                }
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("IHostapd.getService exception: ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                return false;
            } catch (Throwable th) {
            }
        }
    }

    public boolean addAccessPoint(String ifaceName, WifiConfiguration config) {
        synchronized (this.mLock) {
            String methodStr = "addAccessPoint";
            IfaceParams ifaceParams = new IfaceParams();
            ifaceParams.ifaceName = ifaceName;
            ifaceParams.hwModeParams.enable80211N = true;
            ifaceParams.hwModeParams.enable80211AC = this.mEnableIeee80211AC;
            try {
                ifaceParams.channelParams.band = getBand(config);
                if (this.mEnableAcs) {
                    ifaceParams.channelParams.enableAcs = true;
                    ifaceParams.channelParams.acsShouldExcludeDfs = true;
                } else {
                    if (ifaceParams.channelParams.band == 2) {
                        Log.d(TAG, "ACS is not supported on this device, using 2.4 GHz band.");
                        ifaceParams.channelParams.band = 0;
                    }
                    ifaceParams.channelParams.enableAcs = false;
                    ifaceParams.channelParams.channel = config.apChannel;
                }
                NetworkParams nwParams = new NetworkParams();
                try {
                    nwParams.ssid.addAll(NativeUtil.stringToByteArrayList(config.SSID));
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "addAccessPoint: cannot be utf-8 encoded");
                    nwParams.ssid.addAll(NativeUtil.byteArrayToArrayList(NativeUtil.stringToByteArray(config.SSID)));
                }
                nwParams.isHidden = config.hiddenSSID;
                nwParams.encryptionType = getEncryptionType(config);
                nwParams.pskPassphrase = config.preSharedKey != null ? config.preSharedKey : "";
                if (checkHostapdAndLogFailure("addAccessPoint")) {
                    try {
                        boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mIHostapd.addAccessPoint(ifaceParams, nwParams), "addAccessPoint");
                        return checkStatusAndLogFailure;
                    } catch (RemoteException e2) {
                        handleRemoteException(e2, "addAccessPoint");
                        return false;
                    }
                }
                return false;
            } catch (IllegalArgumentException e3) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unrecognized apBand ");
                stringBuilder.append(config.apBand);
                Log.e(str, stringBuilder.toString());
                return false;
            }
        }
    }

    public boolean removeAccessPoint(String ifaceName) {
        synchronized (this.mLock) {
            String methodStr = "removeAccessPoint";
            if (checkHostapdAndLogFailure("removeAccessPoint")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mIHostapd.removeAccessPoint(ifaceName), "removeAccessPoint");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "removeAccessPoint");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean registerDeathHandler(HostapdDeathEventHandler handler) {
        if (this.mDeathEventHandler != null) {
            Log.e(TAG, "Death handler already present");
        }
        this.mDeathEventHandler = handler;
        return true;
    }

    public boolean deregisterDeathHandler() {
        if (this.mDeathEventHandler == null) {
            Log.e(TAG, "No Death handler present");
        }
        this.mDeathEventHandler = null;
        return true;
    }

    private void clearState() {
        synchronized (this.mLock) {
            this.mIHostapd = null;
        }
    }

    private void hostapdServiceDiedHandler() {
        synchronized (this.mLock) {
            clearState();
            if (this.mDeathEventHandler != null) {
                this.mDeathEventHandler.onDeath();
            }
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
        boolean z;
        synchronized (this.mLock) {
            z = this.mIHostapd != null;
        }
        return z;
    }

    public void terminate() {
        synchronized (this.mLock) {
            String methodStr = "terminate";
            if (checkHostapdAndLogFailure("terminate")) {
                try {
                    this.mIHostapd.terminate();
                } catch (RemoteException e) {
                    handleRemoteException(e, "terminate");
                }
            } else {
                return;
            }
        }
    }

    @VisibleForTesting
    protected IServiceManager getServiceManagerMockable() throws RemoteException {
        IServiceManager service;
        synchronized (this.mLock) {
            service = IServiceManager.getService();
        }
        return service;
    }

    @VisibleForTesting
    protected IHostapd getHostapdMockable() throws RemoteException {
        IHostapd service;
        synchronized (this.mLock) {
            service = IHostapd.getService();
        }
        return service;
    }

    private static int getEncryptionType(WifiConfiguration localConfig) {
        int authType = localConfig.getAuthType();
        if (authType == 4) {
            return 2;
        }
        switch (authType) {
            case 0:
                return 0;
            case 1:
                return 1;
            default:
                return 0;
        }
    }

    private static int getBand(WifiConfiguration localConfig) {
        switch (localConfig.apBand) {
            case -1:
                return 2;
            case 0:
                return 0;
            case 1:
                return 1;
            default:
                throw new IllegalArgumentException();
        }
    }

    private boolean checkHostapdAndLogFailure(String methodStr) {
        synchronized (this.mLock) {
            if (this.mIHostapd == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Can't call ");
                stringBuilder.append(methodStr);
                stringBuilder.append(", IHostapd is null");
                Log.e(str, stringBuilder.toString());
                return false;
            }
            return true;
        }
    }

    private boolean checkStatusAndLogFailure(HostapdStatus status, String methodStr) {
        synchronized (this.mLock) {
            String str;
            StringBuilder stringBuilder;
            if (status.code != 0) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("IHostapd.");
                stringBuilder.append(methodStr);
                stringBuilder.append(" failed: ");
                stringBuilder.append(status.code);
                stringBuilder.append(", ");
                stringBuilder.append(status.debugMessage);
                Log.e(str, stringBuilder.toString());
                return false;
            }
            if (this.mVerboseLoggingEnabled) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("IHostapd.");
                stringBuilder.append(methodStr);
                stringBuilder.append(" succeeded");
                Log.d(str, stringBuilder.toString());
            }
            return true;
        }
    }

    private void handleRemoteException(RemoteException e, String methodStr) {
        synchronized (this.mLock) {
            hostapdServiceDiedHandler();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("IHostapd.");
            stringBuilder.append(methodStr);
            stringBuilder.append(" failed with exception");
            Log.e(str, stringBuilder.toString(), e);
        }
    }

    private static void logd(String s) {
        Log.d(TAG, s);
    }

    private static void logi(String s) {
        Log.i(TAG, s);
    }

    private static void loge(String s) {
        Log.e(TAG, s);
    }
}
