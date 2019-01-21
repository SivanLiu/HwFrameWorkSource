package android.net.wifi;

import android.content.pm.ParceledListSlice;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.wifi.hotspot2.IProvisioningCallback;
import android.net.wifi.hotspot2.OsuProvider;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Messenger;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.WorkSource;
import java.util.List;

public interface IWifiManager extends IInterface {

    public static abstract class Stub extends Binder implements IWifiManager {
        private static final String DESCRIPTOR = "android.net.wifi.IWifiManager";
        static final int TRANSACTION_acquireMulticastLock = 41;
        static final int TRANSACTION_acquireWifiLock = 36;
        static final int TRANSACTION_addOrUpdateNetwork = 9;
        static final int TRANSACTION_addOrUpdatePasspointConfiguration = 10;
        static final int TRANSACTION_deauthenticateNetwork = 15;
        static final int TRANSACTION_disableEphemeralNetwork = 60;
        static final int TRANSACTION_disableNetwork = 18;
        static final int TRANSACTION_disconnect = 21;
        static final int TRANSACTION_enableNetwork = 17;
        static final int TRANSACTION_enableTdls = 54;
        static final int TRANSACTION_enableTdlsWithMacAddress = 55;
        static final int TRANSACTION_enableVerboseLogging = 57;
        static final int TRANSACTION_enableWifiConnectivityManager = 59;
        static final int TRANSACTION_factoryReset = 61;
        static final int TRANSACTION_getAllMatchingWifiConfigs = 7;
        static final int TRANSACTION_getConfiguredNetworks = 4;
        static final int TRANSACTION_getConnectionInfo = 24;
        static final int TRANSACTION_getCountryCode = 31;
        static final int TRANSACTION_getCurrentNetwork = 62;
        static final int TRANSACTION_getCurrentNetworkWpsNfcConfigurationToken = 56;
        static final int TRANSACTION_getDhcpInfo = 34;
        static final int TRANSACTION_getMatchingOsuProviders = 8;
        static final int TRANSACTION_getMatchingWifiConfig = 6;
        static final int TRANSACTION_getPasspointConfigurations = 12;
        static final int TRANSACTION_getPrivilegedConfiguredNetworks = 5;
        static final int TRANSACTION_getScanResults = 20;
        static final int TRANSACTION_getSupportedFeatures = 1;
        static final int TRANSACTION_getVerboseLoggingLevel = 58;
        static final int TRANSACTION_getWifiApConfiguration = 51;
        static final int TRANSACTION_getWifiApEnabledState = 50;
        static final int TRANSACTION_getWifiEnabledState = 26;
        static final int TRANSACTION_getWifiServiceMessenger = 53;
        static final int TRANSACTION_initializeMulticastFiltering = 39;
        static final int TRANSACTION_isDualBandSupported = 32;
        static final int TRANSACTION_isMulticastEnabled = 40;
        static final int TRANSACTION_isScanAlwaysAvailable = 35;
        static final int TRANSACTION_matchProviderWithCurrentNetwork = 14;
        static final int TRANSACTION_needs5GHzToAnyApBandConversion = 33;
        static final int TRANSACTION_queryPasspointIcon = 13;
        static final int TRANSACTION_reassociate = 23;
        static final int TRANSACTION_reconnect = 22;
        static final int TRANSACTION_registerSoftApCallback = 67;
        static final int TRANSACTION_releaseMulticastLock = 42;
        static final int TRANSACTION_releaseWifiLock = 38;
        static final int TRANSACTION_removeNetwork = 16;
        static final int TRANSACTION_removePasspointConfiguration = 11;
        static final int TRANSACTION_reportActivityInfo = 2;
        static final int TRANSACTION_requestActivityInfo = 3;
        static final int TRANSACTION_restoreBackupData = 64;
        static final int TRANSACTION_restoreSupplicantBackupData = 65;
        static final int TRANSACTION_retrieveBackupData = 63;
        static final int TRANSACTION_setCountryCode = 30;
        static final int TRANSACTION_setVoWifiDetectMode = 69;
        static final int TRANSACTION_setWifiApConfiguration = 52;
        static final int TRANSACTION_setWifiApStateByManual = 28;
        static final int TRANSACTION_setWifiEnableForP2p = 29;
        static final int TRANSACTION_setWifiEnabled = 25;
        static final int TRANSACTION_setWifiStateByManual = 27;
        static final int TRANSACTION_startLocalOnlyHotspot = 46;
        static final int TRANSACTION_startScan = 19;
        static final int TRANSACTION_startSoftAp = 44;
        static final int TRANSACTION_startSubscriptionProvisioning = 66;
        static final int TRANSACTION_startWatchLocalOnlyHotspot = 48;
        static final int TRANSACTION_stopLocalOnlyHotspot = 47;
        static final int TRANSACTION_stopSoftAp = 45;
        static final int TRANSACTION_stopWatchLocalOnlyHotspot = 49;
        static final int TRANSACTION_unregisterSoftApCallback = 68;
        static final int TRANSACTION_updateInterfaceIpState = 43;
        static final int TRANSACTION_updateWifiLockWorkSource = 37;

        private static class Proxy implements IWifiManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            public int getSupportedFeatures() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public WifiActivityEnergyInfo reportActivityInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    WifiActivityEnergyInfo _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (WifiActivityEnergyInfo) WifiActivityEnergyInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void requestActivityInfo(ResultReceiver result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (result != null) {
                        _data.writeInt(1);
                        result.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public ParceledListSlice getConfiguredNetworks() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ParceledListSlice _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ParceledListSlice) ParceledListSlice.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public ParceledListSlice getPrivilegedConfiguredNetworks() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ParceledListSlice _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ParceledListSlice) ParceledListSlice.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public WifiConfiguration getMatchingWifiConfig(ScanResult scanResult) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    WifiConfiguration _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (scanResult != null) {
                        _data.writeInt(1);
                        scanResult.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (WifiConfiguration) WifiConfiguration.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<WifiConfiguration> getAllMatchingWifiConfigs(ScanResult scanResult) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (scanResult != null) {
                        _data.writeInt(1);
                        scanResult.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    List<WifiConfiguration> _result = _reply.createTypedArrayList(WifiConfiguration.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<OsuProvider> getMatchingOsuProviders(ScanResult scanResult) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (scanResult != null) {
                        _data.writeInt(1);
                        scanResult.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    List<OsuProvider> _result = _reply.createTypedArrayList(OsuProvider.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int addOrUpdateNetwork(WifiConfiguration config, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (config != null) {
                        _data.writeInt(1);
                        config.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean addOrUpdatePasspointConfiguration(PasspointConfiguration config, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (config != null) {
                        _data.writeInt(1);
                        config.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean removePasspointConfiguration(String fqdn, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(fqdn);
                    _data.writeString(packageName);
                    boolean z = false;
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<PasspointConfiguration> getPasspointConfigurations() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    List<PasspointConfiguration> _result = _reply.createTypedArrayList(PasspointConfiguration.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void queryPasspointIcon(long bssid, String fileName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(bssid);
                    _data.writeString(fileName);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int matchProviderWithCurrentNetwork(String fqdn) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(fqdn);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void deauthenticateNetwork(long holdoff, boolean ess) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(holdoff);
                    _data.writeInt(ess);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean removeNetwork(int netId, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeString(packageName);
                    boolean z = false;
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean enableNetwork(int netId, boolean disableOthers, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeInt(disableOthers);
                    _data.writeString(packageName);
                    boolean z = false;
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean disableNetwork(int netId, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeString(packageName);
                    boolean z = false;
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean startScan(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    boolean z = false;
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<ScanResult> getScanResults(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    List<ScanResult> _result = _reply.createTypedArrayList(ScanResult.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void disconnect(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void reconnect(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void reassociate(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public WifiInfo getConnectionInfo(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    WifiInfo _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (WifiInfo) WifiInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setWifiEnabled(String packageName, boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(enable);
                    boolean z = false;
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getWifiEnabledState() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setWifiStateByManual(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enable);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setWifiApStateByManual(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enable);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setWifiEnableForP2p(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enable);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setCountryCode(String country) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(country);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getCountryCode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isDualBandSupported() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean needs5GHzToAnyApBandConversion() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public DhcpInfo getDhcpInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    DhcpInfo _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (DhcpInfo) DhcpInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isScanAlwaysAvailable() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean acquireWifiLock(IBinder lock, int lockType, String tag, WorkSource ws) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(lock);
                    _data.writeInt(lockType);
                    _data.writeString(tag);
                    boolean _result = true;
                    if (ws != null) {
                        _data.writeInt(1);
                        ws.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void updateWifiLockWorkSource(IBinder lock, WorkSource ws) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(lock);
                    if (ws != null) {
                        _data.writeInt(1);
                        ws.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean releaseWifiLock(IBinder lock) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(lock);
                    boolean z = false;
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void initializeMulticastFiltering() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isMulticastEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void acquireMulticastLock(IBinder binder, String tag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(binder);
                    _data.writeString(tag);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void releaseMulticastLock() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void updateInterfaceIpState(String ifaceName, int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifaceName);
                    _data.writeInt(mode);
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean startSoftAp(WifiConfiguration wifiConfig) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (wifiConfig != null) {
                        _data.writeInt(1);
                        wifiConfig.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(44, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean stopSoftAp() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(45, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int startLocalOnlyHotspot(Messenger messenger, IBinder binder, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (messenger != null) {
                        _data.writeInt(1);
                        messenger.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(binder);
                    _data.writeString(packageName);
                    this.mRemote.transact(46, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void stopLocalOnlyHotspot() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(47, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void startWatchLocalOnlyHotspot(Messenger messenger, IBinder binder) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (messenger != null) {
                        _data.writeInt(1);
                        messenger.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(binder);
                    this.mRemote.transact(48, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void stopWatchLocalOnlyHotspot() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(49, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getWifiApEnabledState() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(50, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public WifiConfiguration getWifiApConfiguration() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    WifiConfiguration _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(51, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (WifiConfiguration) WifiConfiguration.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setWifiApConfiguration(WifiConfiguration wifiConfig, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (wifiConfig != null) {
                        _data.writeInt(1);
                        wifiConfig.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(52, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Messenger getWifiServiceMessenger(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Messenger _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(53, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Messenger) Messenger.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void enableTdls(String remoteIPAddress, boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(remoteIPAddress);
                    _data.writeInt(enable);
                    this.mRemote.transact(54, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void enableTdlsWithMacAddress(String remoteMacAddress, boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(remoteMacAddress);
                    _data.writeInt(enable);
                    this.mRemote.transact(55, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getCurrentNetworkWpsNfcConfigurationToken() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(56, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void enableVerboseLogging(int verbose) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(verbose);
                    this.mRemote.transact(57, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getVerboseLoggingLevel() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(58, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void enableWifiConnectivityManager(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enabled);
                    this.mRemote.transact(59, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void disableEphemeralNetwork(String SSID, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(SSID);
                    _data.writeString(packageName);
                    this.mRemote.transact(60, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void factoryReset(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(61, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Network getCurrentNetwork() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Network _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(62, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Network) Network.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public byte[] retrieveBackupData() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(63, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void restoreBackupData(byte[] data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(data);
                    this.mRemote.transact(64, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void restoreSupplicantBackupData(byte[] supplicantData, byte[] ipConfigData) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(supplicantData);
                    _data.writeByteArray(ipConfigData);
                    this.mRemote.transact(65, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void startSubscriptionProvisioning(OsuProvider provider, IProvisioningCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (provider != null) {
                        _data.writeInt(1);
                        provider.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(66, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void registerSoftApCallback(IBinder binder, ISoftApCallback callback, int callbackIdentifier) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(binder);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeInt(callbackIdentifier);
                    this.mRemote.transact(67, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void unregisterSoftApCallback(int callbackIdentifier) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(callbackIdentifier);
                    this.mRemote.transact(68, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setVoWifiDetectMode(WifiDetectConfInfo info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (info != null) {
                        _data.writeInt(1);
                        info.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(69, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IWifiManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IWifiManager)) {
                return new Proxy(obj);
            }
            return (IWifiManager) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != IBinder.INTERFACE_TRANSACTION) {
                ResultReceiver _arg0 = null;
                boolean _arg1 = false;
                int _result;
                ParceledListSlice _result2;
                ScanResult _arg02;
                WifiConfiguration _arg03;
                int _result3;
                boolean _result4;
                String _arg04;
                boolean _result5;
                IBinder _arg05;
                WorkSource _arg3;
                Messenger _arg06;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        _result = getSupportedFeatures();
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        WifiActivityEnergyInfo _result6 = reportActivityInfo();
                        reply.writeNoException();
                        if (_result6 != null) {
                            reply.writeInt(1);
                            _result6.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ResultReceiver) ResultReceiver.CREATOR.createFromParcel(data);
                        }
                        requestActivityInfo(_arg0);
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        _result2 = getConfiguredNetworks();
                        reply.writeNoException();
                        if (_result2 != null) {
                            reply.writeInt(1);
                            _result2.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        _result2 = getPrivilegedConfiguredNetworks();
                        reply.writeNoException();
                        if (_result2 != null) {
                            reply.writeInt(1);
                            _result2.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (ScanResult) ScanResult.CREATOR.createFromParcel(data);
                        }
                        WifiConfiguration _result7 = getMatchingWifiConfig(_arg02);
                        reply.writeNoException();
                        if (_result7 != null) {
                            reply.writeInt(1);
                            _result7.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 7:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (ScanResult) ScanResult.CREATOR.createFromParcel(data);
                        }
                        List<WifiConfiguration> _result8 = getAllMatchingWifiConfigs(_arg02);
                        reply.writeNoException();
                        reply.writeTypedList(_result8);
                        return true;
                    case 8:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (ScanResult) ScanResult.CREATOR.createFromParcel(data);
                        }
                        List<OsuProvider> _result9 = getMatchingOsuProviders(_arg02);
                        reply.writeNoException();
                        reply.writeTypedList(_result9);
                        return true;
                    case 9:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (WifiConfiguration) WifiConfiguration.CREATOR.createFromParcel(data);
                        }
                        _result3 = addOrUpdateNetwork(_arg03, data.readString());
                        reply.writeNoException();
                        reply.writeInt(_result3);
                        return true;
                    case 10:
                        PasspointConfiguration _arg07;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg07 = (PasspointConfiguration) PasspointConfiguration.CREATOR.createFromParcel(data);
                        }
                        _result4 = addOrUpdatePasspointConfiguration(_arg07, data.readString());
                        reply.writeNoException();
                        reply.writeInt(_result4);
                        return true;
                    case 11:
                        data.enforceInterface(descriptor);
                        _result4 = removePasspointConfiguration(data.readString(), data.readString());
                        reply.writeNoException();
                        reply.writeInt(_result4);
                        return true;
                    case 12:
                        data.enforceInterface(descriptor);
                        List<PasspointConfiguration> _result10 = getPasspointConfigurations();
                        reply.writeNoException();
                        reply.writeTypedList(_result10);
                        return true;
                    case 13:
                        data.enforceInterface(descriptor);
                        queryPasspointIcon(data.readLong(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 14:
                        data.enforceInterface(descriptor);
                        int _result11 = matchProviderWithCurrentNetwork(data.readString());
                        reply.writeNoException();
                        reply.writeInt(_result11);
                        return true;
                    case 15:
                        data.enforceInterface(descriptor);
                        long _arg08 = data.readLong();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        deauthenticateNetwork(_arg08, _arg1);
                        reply.writeNoException();
                        return true;
                    case 16:
                        data.enforceInterface(descriptor);
                        _result4 = removeNetwork(data.readInt(), data.readString());
                        reply.writeNoException();
                        reply.writeInt(_result4);
                        return true;
                    case 17:
                        data.enforceInterface(descriptor);
                        _result = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        boolean _result12 = enableNetwork(_result, _arg1, data.readString());
                        reply.writeNoException();
                        reply.writeInt(_result12);
                        return true;
                    case 18:
                        data.enforceInterface(descriptor);
                        _result4 = disableNetwork(data.readInt(), data.readString());
                        reply.writeNoException();
                        reply.writeInt(_result4);
                        return true;
                    case 19:
                        data.enforceInterface(descriptor);
                        _arg1 = startScan(data.readString());
                        reply.writeNoException();
                        reply.writeInt(_arg1);
                        return true;
                    case 20:
                        data.enforceInterface(descriptor);
                        List<ScanResult> _result13 = getScanResults(data.readString());
                        reply.writeNoException();
                        reply.writeTypedList(_result13);
                        return true;
                    case 21:
                        data.enforceInterface(descriptor);
                        disconnect(data.readString());
                        reply.writeNoException();
                        return true;
                    case 22:
                        data.enforceInterface(descriptor);
                        reconnect(data.readString());
                        reply.writeNoException();
                        return true;
                    case 23:
                        data.enforceInterface(descriptor);
                        reassociate(data.readString());
                        reply.writeNoException();
                        return true;
                    case 24:
                        data.enforceInterface(descriptor);
                        WifiInfo _result14 = getConnectionInfo(data.readString());
                        reply.writeNoException();
                        if (_result14 != null) {
                            reply.writeInt(1);
                            _result14.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 25:
                        data.enforceInterface(descriptor);
                        _arg04 = data.readString();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        _result4 = setWifiEnabled(_arg04, _arg1);
                        reply.writeNoException();
                        reply.writeInt(_result4);
                        return true;
                    case 26:
                        data.enforceInterface(descriptor);
                        _result = getWifiEnabledState();
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 27:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setWifiStateByManual(_arg1);
                        reply.writeNoException();
                        return true;
                    case 28:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setWifiApStateByManual(_arg1);
                        reply.writeNoException();
                        return true;
                    case 29:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setWifiEnableForP2p(_arg1);
                        reply.writeNoException();
                        return true;
                    case 30:
                        data.enforceInterface(descriptor);
                        setCountryCode(data.readString());
                        reply.writeNoException();
                        return true;
                    case 31:
                        data.enforceInterface(descriptor);
                        _arg04 = getCountryCode();
                        reply.writeNoException();
                        reply.writeString(_arg04);
                        return true;
                    case 32:
                        data.enforceInterface(descriptor);
                        _result5 = isDualBandSupported();
                        reply.writeNoException();
                        reply.writeInt(_result5);
                        return true;
                    case 33:
                        data.enforceInterface(descriptor);
                        _result5 = needs5GHzToAnyApBandConversion();
                        reply.writeNoException();
                        reply.writeInt(_result5);
                        return true;
                    case 34:
                        data.enforceInterface(descriptor);
                        DhcpInfo _result15 = getDhcpInfo();
                        reply.writeNoException();
                        if (_result15 != null) {
                            reply.writeInt(1);
                            _result15.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 35:
                        data.enforceInterface(descriptor);
                        _result5 = isScanAlwaysAvailable();
                        reply.writeNoException();
                        reply.writeInt(_result5);
                        return true;
                    case 36:
                        data.enforceInterface(descriptor);
                        _arg05 = data.readStrongBinder();
                        _result3 = data.readInt();
                        String _arg2 = data.readString();
                        if (data.readInt() != 0) {
                            _arg3 = (WorkSource) WorkSource.CREATOR.createFromParcel(data);
                        }
                        boolean _result16 = acquireWifiLock(_arg05, _result3, _arg2, _arg3);
                        reply.writeNoException();
                        reply.writeInt(_result16);
                        return true;
                    case 37:
                        data.enforceInterface(descriptor);
                        _arg05 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            _arg3 = (WorkSource) WorkSource.CREATOR.createFromParcel(data);
                        }
                        updateWifiLockWorkSource(_arg05, _arg3);
                        reply.writeNoException();
                        return true;
                    case 38:
                        data.enforceInterface(descriptor);
                        _arg1 = releaseWifiLock(data.readStrongBinder());
                        reply.writeNoException();
                        reply.writeInt(_arg1);
                        return true;
                    case 39:
                        data.enforceInterface(descriptor);
                        initializeMulticastFiltering();
                        reply.writeNoException();
                        return true;
                    case 40:
                        data.enforceInterface(descriptor);
                        _result5 = isMulticastEnabled();
                        reply.writeNoException();
                        reply.writeInt(_result5);
                        return true;
                    case 41:
                        data.enforceInterface(descriptor);
                        acquireMulticastLock(data.readStrongBinder(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 42:
                        data.enforceInterface(descriptor);
                        releaseMulticastLock();
                        reply.writeNoException();
                        return true;
                    case 43:
                        data.enforceInterface(descriptor);
                        updateInterfaceIpState(data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 44:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (WifiConfiguration) WifiConfiguration.CREATOR.createFromParcel(data);
                        }
                        _arg1 = startSoftAp(_arg03);
                        reply.writeNoException();
                        reply.writeInt(_arg1);
                        return true;
                    case 45:
                        data.enforceInterface(descriptor);
                        _result5 = stopSoftAp();
                        reply.writeNoException();
                        reply.writeInt(_result5);
                        return true;
                    case 46:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg06 = (Messenger) Messenger.CREATOR.createFromParcel(data);
                        }
                        int _result17 = startLocalOnlyHotspot(_arg06, data.readStrongBinder(), data.readString());
                        reply.writeNoException();
                        reply.writeInt(_result17);
                        return true;
                    case 47:
                        data.enforceInterface(descriptor);
                        stopLocalOnlyHotspot();
                        reply.writeNoException();
                        return true;
                    case 48:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg06 = (Messenger) Messenger.CREATOR.createFromParcel(data);
                        }
                        startWatchLocalOnlyHotspot(_arg06, data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 49:
                        data.enforceInterface(descriptor);
                        stopWatchLocalOnlyHotspot();
                        reply.writeNoException();
                        return true;
                    case 50:
                        data.enforceInterface(descriptor);
                        _result = getWifiApEnabledState();
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 51:
                        data.enforceInterface(descriptor);
                        _arg03 = getWifiApConfiguration();
                        reply.writeNoException();
                        if (_arg03 != null) {
                            reply.writeInt(1);
                            _arg03.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 52:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (WifiConfiguration) WifiConfiguration.CREATOR.createFromParcel(data);
                        }
                        _result4 = setWifiApConfiguration(_arg03, data.readString());
                        reply.writeNoException();
                        reply.writeInt(_result4);
                        return true;
                    case 53:
                        data.enforceInterface(descriptor);
                        Messenger _result18 = getWifiServiceMessenger(data.readString());
                        reply.writeNoException();
                        if (_result18 != null) {
                            reply.writeInt(1);
                            _result18.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 54:
                        data.enforceInterface(descriptor);
                        _arg04 = data.readString();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        enableTdls(_arg04, _arg1);
                        reply.writeNoException();
                        return true;
                    case 55:
                        data.enforceInterface(descriptor);
                        _arg04 = data.readString();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        enableTdlsWithMacAddress(_arg04, _arg1);
                        reply.writeNoException();
                        return true;
                    case 56:
                        data.enforceInterface(descriptor);
                        _arg04 = getCurrentNetworkWpsNfcConfigurationToken();
                        reply.writeNoException();
                        reply.writeString(_arg04);
                        return true;
                    case 57:
                        data.enforceInterface(descriptor);
                        enableVerboseLogging(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 58:
                        data.enforceInterface(descriptor);
                        _result = getVerboseLoggingLevel();
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 59:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        enableWifiConnectivityManager(_arg1);
                        reply.writeNoException();
                        return true;
                    case 60:
                        data.enforceInterface(descriptor);
                        disableEphemeralNetwork(data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 61:
                        data.enforceInterface(descriptor);
                        factoryReset(data.readString());
                        reply.writeNoException();
                        return true;
                    case 62:
                        data.enforceInterface(descriptor);
                        Network _result19 = getCurrentNetwork();
                        reply.writeNoException();
                        if (_result19 != null) {
                            reply.writeInt(1);
                            _result19.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 63:
                        data.enforceInterface(descriptor);
                        byte[] _result20 = retrieveBackupData();
                        reply.writeNoException();
                        reply.writeByteArray(_result20);
                        return true;
                    case 64:
                        data.enforceInterface(descriptor);
                        restoreBackupData(data.createByteArray());
                        reply.writeNoException();
                        return true;
                    case 65:
                        data.enforceInterface(descriptor);
                        restoreSupplicantBackupData(data.createByteArray(), data.createByteArray());
                        reply.writeNoException();
                        return true;
                    case 66:
                        OsuProvider _arg09;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg09 = (OsuProvider) OsuProvider.CREATOR.createFromParcel(data);
                        }
                        startSubscriptionProvisioning(_arg09, android.net.wifi.hotspot2.IProvisioningCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 67:
                        data.enforceInterface(descriptor);
                        registerSoftApCallback(data.readStrongBinder(), android.net.wifi.ISoftApCallback.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 68:
                        data.enforceInterface(descriptor);
                        unregisterSoftApCallback(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 69:
                        WifiDetectConfInfo _arg010;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg010 = (WifiDetectConfInfo) WifiDetectConfInfo.CREATOR.createFromParcel(data);
                        }
                        _arg1 = setVoWifiDetectMode(_arg010);
                        reply.writeNoException();
                        reply.writeInt(_arg1);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void acquireMulticastLock(IBinder iBinder, String str) throws RemoteException;

    boolean acquireWifiLock(IBinder iBinder, int i, String str, WorkSource workSource) throws RemoteException;

    int addOrUpdateNetwork(WifiConfiguration wifiConfiguration, String str) throws RemoteException;

    boolean addOrUpdatePasspointConfiguration(PasspointConfiguration passpointConfiguration, String str) throws RemoteException;

    void deauthenticateNetwork(long j, boolean z) throws RemoteException;

    void disableEphemeralNetwork(String str, String str2) throws RemoteException;

    boolean disableNetwork(int i, String str) throws RemoteException;

    void disconnect(String str) throws RemoteException;

    boolean enableNetwork(int i, boolean z, String str) throws RemoteException;

    void enableTdls(String str, boolean z) throws RemoteException;

    void enableTdlsWithMacAddress(String str, boolean z) throws RemoteException;

    void enableVerboseLogging(int i) throws RemoteException;

    void enableWifiConnectivityManager(boolean z) throws RemoteException;

    void factoryReset(String str) throws RemoteException;

    List<WifiConfiguration> getAllMatchingWifiConfigs(ScanResult scanResult) throws RemoteException;

    ParceledListSlice getConfiguredNetworks() throws RemoteException;

    WifiInfo getConnectionInfo(String str) throws RemoteException;

    String getCountryCode() throws RemoteException;

    Network getCurrentNetwork() throws RemoteException;

    String getCurrentNetworkWpsNfcConfigurationToken() throws RemoteException;

    DhcpInfo getDhcpInfo() throws RemoteException;

    List<OsuProvider> getMatchingOsuProviders(ScanResult scanResult) throws RemoteException;

    WifiConfiguration getMatchingWifiConfig(ScanResult scanResult) throws RemoteException;

    List<PasspointConfiguration> getPasspointConfigurations() throws RemoteException;

    ParceledListSlice getPrivilegedConfiguredNetworks() throws RemoteException;

    List<ScanResult> getScanResults(String str) throws RemoteException;

    int getSupportedFeatures() throws RemoteException;

    int getVerboseLoggingLevel() throws RemoteException;

    WifiConfiguration getWifiApConfiguration() throws RemoteException;

    int getWifiApEnabledState() throws RemoteException;

    int getWifiEnabledState() throws RemoteException;

    Messenger getWifiServiceMessenger(String str) throws RemoteException;

    void initializeMulticastFiltering() throws RemoteException;

    boolean isDualBandSupported() throws RemoteException;

    boolean isMulticastEnabled() throws RemoteException;

    boolean isScanAlwaysAvailable() throws RemoteException;

    int matchProviderWithCurrentNetwork(String str) throws RemoteException;

    boolean needs5GHzToAnyApBandConversion() throws RemoteException;

    void queryPasspointIcon(long j, String str) throws RemoteException;

    void reassociate(String str) throws RemoteException;

    void reconnect(String str) throws RemoteException;

    void registerSoftApCallback(IBinder iBinder, ISoftApCallback iSoftApCallback, int i) throws RemoteException;

    void releaseMulticastLock() throws RemoteException;

    boolean releaseWifiLock(IBinder iBinder) throws RemoteException;

    boolean removeNetwork(int i, String str) throws RemoteException;

    boolean removePasspointConfiguration(String str, String str2) throws RemoteException;

    WifiActivityEnergyInfo reportActivityInfo() throws RemoteException;

    void requestActivityInfo(ResultReceiver resultReceiver) throws RemoteException;

    void restoreBackupData(byte[] bArr) throws RemoteException;

    void restoreSupplicantBackupData(byte[] bArr, byte[] bArr2) throws RemoteException;

    byte[] retrieveBackupData() throws RemoteException;

    void setCountryCode(String str) throws RemoteException;

    boolean setVoWifiDetectMode(WifiDetectConfInfo wifiDetectConfInfo) throws RemoteException;

    boolean setWifiApConfiguration(WifiConfiguration wifiConfiguration, String str) throws RemoteException;

    void setWifiApStateByManual(boolean z) throws RemoteException;

    void setWifiEnableForP2p(boolean z) throws RemoteException;

    boolean setWifiEnabled(String str, boolean z) throws RemoteException;

    void setWifiStateByManual(boolean z) throws RemoteException;

    int startLocalOnlyHotspot(Messenger messenger, IBinder iBinder, String str) throws RemoteException;

    boolean startScan(String str) throws RemoteException;

    boolean startSoftAp(WifiConfiguration wifiConfiguration) throws RemoteException;

    void startSubscriptionProvisioning(OsuProvider osuProvider, IProvisioningCallback iProvisioningCallback) throws RemoteException;

    void startWatchLocalOnlyHotspot(Messenger messenger, IBinder iBinder) throws RemoteException;

    void stopLocalOnlyHotspot() throws RemoteException;

    boolean stopSoftAp() throws RemoteException;

    void stopWatchLocalOnlyHotspot() throws RemoteException;

    void unregisterSoftApCallback(int i) throws RemoteException;

    void updateInterfaceIpState(String str, int i) throws RemoteException;

    void updateWifiLockWorkSource(IBinder iBinder, WorkSource workSource) throws RemoteException;
}
