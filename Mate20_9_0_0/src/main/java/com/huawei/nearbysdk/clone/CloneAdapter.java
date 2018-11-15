package com.huawei.nearbysdk.clone;

import android.content.Context;
import android.os.Looper;
import android.os.RemoteException;
import com.huawei.nearbysdk.HwLog;
import com.huawei.nearbysdk.INearbyAdapter;
import com.huawei.nearbysdk.NearbyAdapter;
import com.huawei.nearbysdk.NearbyAdapterCallback;
import com.huawei.nearbysdk.NearbyConfig.BusinessTypeEnum;
import com.huawei.nearbysdk.NearbyConfiguration;
import com.huawei.nearbysdk.NearbyDevice;

public final class CloneAdapter {
    private static final String TAG = "CloneAdapter";
    public static final int WIFI_BAND_2GHZ = 1;
    public static final int WIFI_BAND_5GHZ = 2;
    public static final int WIFI_BAND_AUTO = 0;
    private static CloneAdapter mCloneAdapter;
    private static final Object mCloneLock = new Object();
    private WifiStatusListenerTransport mApHostListenerTransport;
    private NearbyAdapter mNearbyAdapter;
    private WifiStatusListenerTransport mWifiSlaveListenerTransport;

    /* synthetic */ CloneAdapter(NearbyAdapter x0, AnonymousClass1 x1) {
        this(x0);
    }

    private CloneAdapter(NearbyAdapter adapter) {
        synchronized (mCloneLock) {
            HwLog.i(TAG, "CloneAdapter init");
            this.mNearbyAdapter = adapter;
        }
    }

    public static void createInstance(Context context, final CloneAdapterCallback callback) {
        synchronized (mCloneLock) {
            HwLog.i(TAG, "CloneAdapter createInstance");
            if (callback != null) {
                NearbyAdapter.createInstance(context, new NearbyAdapterCallback() {
                    public void onAdapterGet(NearbyAdapter adapter) {
                        synchronized (CloneAdapter.mCloneLock) {
                            String str = CloneAdapter.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("CloneAdapter onAdapterGet ");
                            stringBuilder.append(adapter);
                            HwLog.w(str, stringBuilder.toString());
                            if (adapter == null) {
                                CloneAdapter.releaseInstance();
                                callback.onAdapterGet(null);
                                return;
                            }
                            if (CloneAdapter.mCloneAdapter == null) {
                                CloneAdapter.mCloneAdapter = new CloneAdapter(adapter, null);
                            }
                            callback.onAdapterGet(CloneAdapter.mCloneAdapter);
                        }
                    }

                    public void onBinderDied() {
                        synchronized (CloneAdapter.mCloneLock) {
                            HwLog.w(CloneAdapter.TAG, "CloneAdapter onBinderDied");
                            CloneAdapter.releaseInstance();
                            callback.onBinderDied();
                        }
                    }
                });
            } else {
                HwLog.e(TAG, "createInstance callback null");
                throw new IllegalArgumentException("createInstance callback null");
            }
        }
    }

    public static void releaseInstance() {
        synchronized (mCloneLock) {
            HwLog.w(TAG, "CloneAdapter releaseInstance");
            if (mCloneAdapter != null) {
                mCloneAdapter.disconnectWifi(false);
                mCloneAdapter.disableWifiAp();
                mCloneAdapter.mNearbyAdapter = null;
                mCloneAdapter.mApHostListenerTransport = null;
                mCloneAdapter.mWifiSlaveListenerTransport = null;
            }
            mCloneAdapter = null;
            NearbyAdapter.releaseInstance();
        }
    }

    protected void finalize() throws Throwable {
        HwLog.w(TAG, "CloneAdapter finalize");
        super.finalize();
    }

    public void enableWifiAp(String wifiSsid, String wifiPwd, int wifiBand, int serverPort, WifiStatusListener listener, int timeoutMs) {
        synchronized (mCloneLock) {
            HwLog.i(TAG, "enableWifiAp start");
            boolean result = false;
            if (wifiSsid == null || wifiPwd == null || listener == null) {
                HwLog.e(TAG, "enableWifiAp wifiSsid\\wifiPwd or listener null");
                throw new IllegalArgumentException("enableWifiAp wifiSsid\\wifiPwd or listener null");
            } else if (this.mApHostListenerTransport == null) {
                if (this.mNearbyAdapter != null) {
                    INearbyAdapter nearbyService = this.mNearbyAdapter.getNearbyService();
                    INearbyAdapter nearbyService2 = nearbyService;
                    if (nearbyService != null) {
                        Looper looper = this.mNearbyAdapter.getLooper();
                        Looper looper2 = looper;
                        if (looper != null) {
                            NearbyConfiguration configuration = new NearbyConfiguration(5, wifiSsid, wifiPwd, wifiBand, null, serverPort, timeoutMs);
                            INearbyAdapter nearbyService3 = nearbyService2;
                            this.mApHostListenerTransport = new WifiStatusListenerTransport(BusinessTypeEnum.Token, 4, configuration, listener, looper2);
                            try {
                                result = nearbyService3.registerConnectionListener(BusinessTypeEnum.Token.toNumber(), 4, configuration, this.mApHostListenerTransport);
                            } catch (RemoteException e) {
                                String str = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("enableWifiAp registerConnectionListener ERROR:");
                                stringBuilder.append(e.getLocalizedMessage());
                                HwLog.e(str, stringBuilder.toString());
                                this.mApHostListenerTransport.onStatusChange(-1);
                            }
                            if (result) {
                            } else {
                                HwLog.e(TAG, "enableWifiAp registerConnectionListener ERROR");
                                throw new IllegalArgumentException("enableWifiAp registerConnectionListener error");
                            }
                        }
                    }
                }
                HwLog.e(TAG, "enableWifiAp NearbyService is null. createInstance nearby ERROR");
                throw new IllegalArgumentException("enableWifiAp createInstance nearby ERROR");
            } else {
                HwLog.e(TAG, "enableWifiAp WifiStatusListener already registered");
                throw new IllegalArgumentException("enableWifiAp WifiStatusListener already registered");
            }
        }
    }

    public void disableWifiAp() {
        synchronized (mCloneLock) {
            boolean result = false;
            HwLog.d(TAG, "disableWifiAp start");
            if (!(this.mApHostListenerTransport == null || this.mNearbyAdapter == null)) {
                INearbyAdapter nearbyService = this.mNearbyAdapter.getNearbyService();
                INearbyAdapter nearbyService2 = nearbyService;
                if (nearbyService != null) {
                    this.mApHostListenerTransport.quit();
                    try {
                        result = nearbyService2.unRegisterConnectionListener(this.mApHostListenerTransport);
                    } catch (RemoteException e) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("disableWifiAp unRegisterConnectionListener ERROR:");
                        stringBuilder.append(e.getLocalizedMessage());
                        HwLog.e(str, stringBuilder.toString());
                    }
                    if (result) {
                        this.mApHostListenerTransport.waitQuit();
                    }
                    this.mApHostListenerTransport = null;
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("disableWifiAp ");
                    stringBuilder2.append(result);
                    HwLog.i(str2, stringBuilder2.toString());
                    return;
                }
            }
            HwLog.e(TAG, "disableWifiAp already done");
        }
    }

    public void connectWifi(String wifiSsid, String wifiPwd, int wifiBand, int serverPort, WifiStatusListener listener, int timeoutMs) {
        synchronized (mCloneLock) {
            HwLog.i(TAG, "connectWifi start");
            boolean result = false;
            if (wifiSsid == null || wifiPwd == null || listener == null) {
                HwLog.e(TAG, "connectWifi wifiSsid\\wifiPwd or listener null");
                throw new IllegalArgumentException("connectWifi wifiSsid\\wifiPwd or listener null");
            } else if (this.mWifiSlaveListenerTransport == null) {
                if (this.mNearbyAdapter != null) {
                    INearbyAdapter nearbyService = this.mNearbyAdapter.getNearbyService();
                    INearbyAdapter nearbyService2 = nearbyService;
                    if (nearbyService != null) {
                        Looper looper = this.mNearbyAdapter.getLooper();
                        Looper looper2 = looper;
                        if (looper != null) {
                            NearbyDevice nearbyDevice = new NearbyDevice(wifiSsid, wifiPwd, wifiBand, null, serverPort);
                            INearbyAdapter nearbyService3 = nearbyService2;
                            this.mWifiSlaveListenerTransport = new WifiStatusListenerTransport(BusinessTypeEnum.Token, 4, nearbyDevice, listener, looper2);
                            try {
                                result = nearbyService3.registerConnectionListener(BusinessTypeEnum.Token.toNumber(), 4, null, this.mWifiSlaveListenerTransport);
                            } catch (RemoteException e) {
                                String str = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("connectWifi registerConnectionListener ERROR:");
                                stringBuilder.append(e.getLocalizedMessage());
                                HwLog.e(str, stringBuilder.toString());
                                this.mWifiSlaveListenerTransport.onStatusChange(-1);
                            }
                            if (result) {
                                if (!this.mNearbyAdapter.open(BusinessTypeEnum.Token, 5, 4, nearbyDevice, timeoutMs)) {
                                    HwLog.e(TAG, "connectWifi open ERROR");
                                    this.mWifiSlaveListenerTransport.onStatusChange(-1);
                                }
                            } else {
                                HwLog.e(TAG, "connectWifi registerConnectionListener ERROR");
                                throw new IllegalArgumentException("connectWifi registerConnectionListener error");
                            }
                        }
                    }
                }
                HwLog.e(TAG, "connectWifi NearbyService is null, createInstance nearby ERROR");
                throw new IllegalArgumentException("connectWifi createInstance nearby ERROR");
            } else {
                HwLog.e(TAG, "connectWifi mWifiSlaveListenerTransport already registered");
                throw new IllegalArgumentException("connectWifi mWifiSlaveListenerTransport already registered");
            }
        }
    }

    public void disconnectWifi(boolean isNeedClose) {
        synchronized (mCloneLock) {
            HwLog.d(TAG, "disconnectWifi start");
            boolean result = false;
            if (!(this.mWifiSlaveListenerTransport == null || this.mNearbyAdapter == null)) {
                INearbyAdapter nearbyService = this.mNearbyAdapter.getNearbyService();
                INearbyAdapter nearbyService2 = nearbyService;
                if (nearbyService != null) {
                    WifiStatusListenerTransport transport = this.mWifiSlaveListenerTransport;
                    NearbyDevice nearbyDevice = transport.getNearbyDevice();
                    if (nearbyDevice != null) {
                        nearbyDevice.setNeedClose(isNeedClose);
                    }
                    transport.quit();
                    this.mNearbyAdapter.close(transport.getBusinessType(), transport.getBusinessId(), nearbyDevice);
                    try {
                        result = nearbyService2.unRegisterConnectionListener(transport);
                    } catch (RemoteException e) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("disconnectWifi unRegisterConnectionListener ERROR:");
                        stringBuilder.append(e.getLocalizedMessage());
                        HwLog.e(str, stringBuilder.toString());
                    }
                    if (result) {
                        transport.waitQuit();
                    }
                    this.mWifiSlaveListenerTransport = null;
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("disconnectWifi ");
                    stringBuilder2.append(result);
                    HwLog.i(str2, stringBuilder2.toString());
                    return;
                }
            }
            HwLog.e(TAG, "disconnectWifi already done");
        }
    }
}
