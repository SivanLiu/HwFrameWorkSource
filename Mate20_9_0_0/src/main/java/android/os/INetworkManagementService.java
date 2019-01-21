package android.os;

import android.net.INetd;
import android.net.INetworkManagementEventObserver;
import android.net.ITetheringStatsProvider;
import android.net.InterfaceConfiguration;
import android.net.Network;
import android.net.NetworkStats;
import android.net.RouteInfo;
import android.net.UidRange;
import java.util.List;

public interface INetworkManagementService extends IInterface {

    public static abstract class Stub extends Binder implements INetworkManagementService {
        private static final String DESCRIPTOR = "android.os.INetworkManagementService";
        static final int TRANSACTION_addIdleTimer = 55;
        static final int TRANSACTION_addInterfaceToLocalNetwork = 85;
        static final int TRANSACTION_addInterfaceToNetwork = 75;
        static final int TRANSACTION_addLegacyRouteForNetId = 77;
        static final int TRANSACTION_addRoute = 14;
        static final int TRANSACTION_addUpstreamV6Interface = 35;
        static final int TRANSACTION_addVpnUidRanges = 64;
        static final int TRANSACTION_allowProtect = 83;
        static final int TRANSACTION_attachPppd = 38;
        static final int TRANSACTION_clearDefaultNetId = 79;
        static final int TRANSACTION_clearInterfaceAddresses = 7;
        static final int TRANSACTION_clearPermission = 82;
        static final int TRANSACTION_createPhysicalNetwork = 72;
        static final int TRANSACTION_createVirtualNetwork = 73;
        static final int TRANSACTION_denyProtect = 84;
        static final int TRANSACTION_detachPppd = 39;
        static final int TRANSACTION_disableIpv6 = 11;
        static final int TRANSACTION_disableNat = 31;
        static final int TRANSACTION_enableIpv6 = 12;
        static final int TRANSACTION_enableNat = 30;
        static final int TRANSACTION_getDnsForwarders = 27;
        static final int TRANSACTION_getInterfaceConfig = 5;
        static final int TRANSACTION_getIpForwardingEnabled = 18;
        static final int TRANSACTION_getNetdPid = 90;
        static final int TRANSACTION_getNetdService = 3;
        static final int TRANSACTION_getNetworkStatsDetail = 42;
        static final int TRANSACTION_getNetworkStatsSummaryDev = 40;
        static final int TRANSACTION_getNetworkStatsSummaryXt = 41;
        static final int TRANSACTION_getNetworkStatsTethering = 44;
        static final int TRANSACTION_getNetworkStatsUidDetail = 43;
        static final int TRANSACTION_isBandwidthControlEnabled = 54;
        static final int TRANSACTION_isClatdStarted = 68;
        static final int TRANSACTION_isFirewallEnabled = 59;
        static final int TRANSACTION_isNetworkActive = 71;
        static final int TRANSACTION_isNetworkRestricted = 89;
        static final int TRANSACTION_isTetheringStarted = 22;
        static final int TRANSACTION_listInterfaces = 4;
        static final int TRANSACTION_listTetheredInterfaces = 25;
        static final int TRANSACTION_listTtys = 37;
        static final int TRANSACTION_registerNetworkActivityListener = 69;
        static final int TRANSACTION_registerObserver = 1;
        static final int TRANSACTION_registerTetheringStatsProvider = 32;
        static final int TRANSACTION_removeIdleTimer = 56;
        static final int TRANSACTION_removeInterfaceAlert = 48;
        static final int TRANSACTION_removeInterfaceFromLocalNetwork = 86;
        static final int TRANSACTION_removeInterfaceFromNetwork = 76;
        static final int TRANSACTION_removeInterfaceQuota = 46;
        static final int TRANSACTION_removeNetwork = 74;
        static final int TRANSACTION_removeRoute = 15;
        static final int TRANSACTION_removeRoutesFromLocalNetwork = 87;
        static final int TRANSACTION_removeUpstreamV6Interface = 36;
        static final int TRANSACTION_removeVpnUidRanges = 65;
        static final int TRANSACTION_setAllowOnlyVpnForUids = 88;
        static final int TRANSACTION_setDataSaverModeEnabled = 52;
        static final int TRANSACTION_setDefaultNetId = 78;
        static final int TRANSACTION_setDnsConfigurationForNetwork = 57;
        static final int TRANSACTION_setDnsForwarders = 26;
        static final int TRANSACTION_setFirewallChainEnabled = 63;
        static final int TRANSACTION_setFirewallEnabled = 58;
        static final int TRANSACTION_setFirewallInterfaceRule = 60;
        static final int TRANSACTION_setFirewallUidRule = 61;
        static final int TRANSACTION_setFirewallUidRules = 62;
        static final int TRANSACTION_setGlobalAlert = 49;
        static final int TRANSACTION_setIPv6AddrGenMode = 13;
        static final int TRANSACTION_setInterfaceAlert = 47;
        static final int TRANSACTION_setInterfaceConfig = 6;
        static final int TRANSACTION_setInterfaceDown = 8;
        static final int TRANSACTION_setInterfaceIpv6PrivacyExtensions = 10;
        static final int TRANSACTION_setInterfaceQuota = 45;
        static final int TRANSACTION_setInterfaceUp = 9;
        static final int TRANSACTION_setIpForwardingEnabled = 19;
        static final int TRANSACTION_setMtu = 16;
        static final int TRANSACTION_setNetworkPermission = 80;
        static final int TRANSACTION_setPermission = 81;
        static final int TRANSACTION_setUidCleartextNetworkPolicy = 53;
        static final int TRANSACTION_setUidMeteredNetworkBlacklist = 50;
        static final int TRANSACTION_setUidMeteredNetworkWhitelist = 51;
        static final int TRANSACTION_shutdown = 17;
        static final int TRANSACTION_startClatd = 66;
        static final int TRANSACTION_startInterfaceForwarding = 28;
        static final int TRANSACTION_startTethering = 20;
        static final int TRANSACTION_stopClatd = 67;
        static final int TRANSACTION_stopInterfaceForwarding = 29;
        static final int TRANSACTION_stopTethering = 21;
        static final int TRANSACTION_tetherInterface = 23;
        static final int TRANSACTION_tetherLimitReached = 34;
        static final int TRANSACTION_unregisterNetworkActivityListener = 70;
        static final int TRANSACTION_unregisterObserver = 2;
        static final int TRANSACTION_unregisterTetheringStatsProvider = 33;
        static final int TRANSACTION_untetherInterface = 24;

        private static class Proxy implements INetworkManagementService {
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

            public void registerObserver(INetworkManagementEventObserver obs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(obs != null ? obs.asBinder() : null);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void unregisterObserver(INetworkManagementEventObserver obs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(obs != null ? obs.asBinder() : null);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public INetd getNetdService() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    INetd _result = android.net.INetd.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String[] listInterfaces() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public InterfaceConfiguration getInterfaceConfig(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    InterfaceConfiguration _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (InterfaceConfiguration) InterfaceConfiguration.CREATOR.createFromParcel(_reply);
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

            public void setInterfaceConfig(String iface, InterfaceConfiguration cfg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    if (cfg != null) {
                        _data.writeInt(1);
                        cfg.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void clearInterfaceAddresses(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setInterfaceDown(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setInterfaceUp(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setInterfaceIpv6PrivacyExtensions(String iface, boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeInt(enable);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void disableIpv6(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void enableIpv6(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setIPv6AddrGenMode(String iface, int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeInt(mode);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void addRoute(int netId, RouteInfo route) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    if (route != null) {
                        _data.writeInt(1);
                        route.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeRoute(int netId, RouteInfo route) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    if (route != null) {
                        _data.writeInt(1);
                        route.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setMtu(String iface, int mtu) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeInt(mtu);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void shutdown() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean getIpForwardingEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
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

            public void setIpForwardingEnabled(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enabled);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void startTethering(String[] dhcpRanges) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringArray(dhcpRanges);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void stopTethering() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isTetheringStarted() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(22, _data, _reply, 0);
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

            public void tetherInterface(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void untetherInterface(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String[] listTetheredInterfaces() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setDnsForwarders(Network network, String[] dns) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (network != null) {
                        _data.writeInt(1);
                        network.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStringArray(dns);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String[] getDnsForwarders() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void startInterfaceForwarding(String fromIface, String toIface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(fromIface);
                    _data.writeString(toIface);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void stopInterfaceForwarding(String fromIface, String toIface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(fromIface);
                    _data.writeString(toIface);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void enableNat(String internalInterface, String externalInterface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(internalInterface);
                    _data.writeString(externalInterface);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void disableNat(String internalInterface, String externalInterface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(internalInterface);
                    _data.writeString(externalInterface);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void registerTetheringStatsProvider(ITetheringStatsProvider provider, String name) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(provider != null ? provider.asBinder() : null);
                    _data.writeString(name);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void unregisterTetheringStatsProvider(ITetheringStatsProvider provider) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(provider != null ? provider.asBinder() : null);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void tetherLimitReached(ITetheringStatsProvider provider) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(provider != null ? provider.asBinder() : null);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void addUpstreamV6Interface(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeUpstreamV6Interface(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String[] listTtys() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void attachPppd(String tty, String localAddr, String remoteAddr, String dns1Addr, String dns2Addr) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(tty);
                    _data.writeString(localAddr);
                    _data.writeString(remoteAddr);
                    _data.writeString(dns1Addr);
                    _data.writeString(dns2Addr);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void detachPppd(String tty) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(tty);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public NetworkStats getNetworkStatsSummaryDev() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    NetworkStats _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (NetworkStats) NetworkStats.CREATOR.createFromParcel(_reply);
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

            public NetworkStats getNetworkStatsSummaryXt() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    NetworkStats _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (NetworkStats) NetworkStats.CREATOR.createFromParcel(_reply);
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

            public NetworkStats getNetworkStatsDetail() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    NetworkStats _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (NetworkStats) NetworkStats.CREATOR.createFromParcel(_reply);
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

            public NetworkStats getNetworkStatsUidDetail(int uid, String[] ifaces) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    NetworkStats _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeStringArray(ifaces);
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (NetworkStats) NetworkStats.CREATOR.createFromParcel(_reply);
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

            public NetworkStats getNetworkStatsTethering(int how) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    NetworkStats _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(how);
                    this.mRemote.transact(44, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (NetworkStats) NetworkStats.CREATOR.createFromParcel(_reply);
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

            public void setInterfaceQuota(String iface, long quotaBytes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeLong(quotaBytes);
                    this.mRemote.transact(45, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeInterfaceQuota(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(46, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setInterfaceAlert(String iface, long alertBytes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeLong(alertBytes);
                    this.mRemote.transact(47, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeInterfaceAlert(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(48, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setGlobalAlert(long alertBytes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(alertBytes);
                    this.mRemote.transact(49, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setUidMeteredNetworkBlacklist(int uid, boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(enable);
                    this.mRemote.transact(50, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setUidMeteredNetworkWhitelist(int uid, boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(enable);
                    this.mRemote.transact(51, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setDataSaverModeEnabled(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enable);
                    boolean z = false;
                    this.mRemote.transact(52, _data, _reply, 0);
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

            public void setUidCleartextNetworkPolicy(int uid, int policy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(policy);
                    this.mRemote.transact(53, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isBandwidthControlEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(54, _data, _reply, 0);
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

            public void addIdleTimer(String iface, int timeout, int type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeInt(timeout);
                    _data.writeInt(type);
                    this.mRemote.transact(55, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeIdleTimer(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(56, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setDnsConfigurationForNetwork(int netId, String[] servers, String[] domains, int[] params, String tlsHostname, String[] tlsServers) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeStringArray(servers);
                    _data.writeStringArray(domains);
                    _data.writeIntArray(params);
                    _data.writeString(tlsHostname);
                    _data.writeStringArray(tlsServers);
                    this.mRemote.transact(57, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setFirewallEnabled(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enabled);
                    this.mRemote.transact(58, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isFirewallEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(59, _data, _reply, 0);
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

            public void setFirewallInterfaceRule(String iface, boolean allow) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeInt(allow);
                    this.mRemote.transact(60, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setFirewallUidRule(int chain, int uid, int rule) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(chain);
                    _data.writeInt(uid);
                    _data.writeInt(rule);
                    this.mRemote.transact(61, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setFirewallUidRules(int chain, int[] uids, int[] rules) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(chain);
                    _data.writeIntArray(uids);
                    _data.writeIntArray(rules);
                    this.mRemote.transact(62, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setFirewallChainEnabled(int chain, boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(chain);
                    _data.writeInt(enable);
                    this.mRemote.transact(63, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void addVpnUidRanges(int netId, UidRange[] ranges) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeTypedArray(ranges, 0);
                    this.mRemote.transact(64, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeVpnUidRanges(int netId, UidRange[] ranges) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeTypedArray(ranges, 0);
                    this.mRemote.transact(65, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void startClatd(String interfaceName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(interfaceName);
                    this.mRemote.transact(66, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void stopClatd(String interfaceName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(interfaceName);
                    this.mRemote.transact(67, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isClatdStarted(String interfaceName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(interfaceName);
                    boolean z = false;
                    this.mRemote.transact(68, _data, _reply, 0);
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

            public void registerNetworkActivityListener(INetworkActivityListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(69, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void unregisterNetworkActivityListener(INetworkActivityListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(70, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isNetworkActive() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(71, _data, _reply, 0);
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

            public void createPhysicalNetwork(int netId, String permission) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeString(permission);
                    this.mRemote.transact(72, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void createVirtualNetwork(int netId, boolean hasDNS, boolean secure) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeInt(hasDNS);
                    _data.writeInt(secure);
                    this.mRemote.transact(73, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeNetwork(int netId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    this.mRemote.transact(74, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void addInterfaceToNetwork(String iface, int netId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeInt(netId);
                    this.mRemote.transact(75, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeInterfaceFromNetwork(String iface, int netId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeInt(netId);
                    this.mRemote.transact(76, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void addLegacyRouteForNetId(int netId, RouteInfo routeInfo, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    if (routeInfo != null) {
                        _data.writeInt(1);
                        routeInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(uid);
                    this.mRemote.transact(77, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setDefaultNetId(int netId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    this.mRemote.transact(78, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void clearDefaultNetId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(79, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setNetworkPermission(int netId, String permission) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeString(permission);
                    this.mRemote.transact(80, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setPermission(String permission, int[] uids) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(permission);
                    _data.writeIntArray(uids);
                    this.mRemote.transact(81, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void clearPermission(int[] uids) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeIntArray(uids);
                    this.mRemote.transact(82, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void allowProtect(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(83, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void denyProtect(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(84, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void addInterfaceToLocalNetwork(String iface, List<RouteInfo> routes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeTypedList(routes);
                    this.mRemote.transact(85, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeInterfaceFromLocalNetwork(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(86, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int removeRoutesFromLocalNetwork(List<RouteInfo> routes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(routes);
                    this.mRemote.transact(87, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setAllowOnlyVpnForUids(boolean enable, UidRange[] uidRanges) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enable);
                    _data.writeTypedArray(uidRanges, 0);
                    this.mRemote.transact(88, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isNetworkRestricted(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    boolean z = false;
                    this.mRemote.transact(89, _data, _reply, 0);
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

            public int getNetdPid() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(90, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INetworkManagementService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof INetworkManagementService)) {
                return new Proxy(obj);
            }
            return (INetworkManagementService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            Parcel parcel2 = reply;
            String descriptor = DESCRIPTOR;
            if (i != 1598968902) {
                IBinder _arg1 = null;
                boolean _arg12 = false;
                String[] _result;
                String _arg0;
                int _arg02;
                RouteInfo _arg13;
                boolean _result2;
                NetworkStats _result3;
                int _arg03;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        registerObserver(android.net.INetworkManagementEventObserver.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        unregisterObserver(android.net.INetworkManagementEventObserver.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        INetd _result4 = getNetdService();
                        reply.writeNoException();
                        if (_result4 != null) {
                            _arg1 = _result4.asBinder();
                        }
                        parcel2.writeStrongBinder(_arg1);
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        _result = listInterfaces();
                        reply.writeNoException();
                        parcel2.writeStringArray(_result);
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        InterfaceConfiguration _result5 = getInterfaceConfig(data.readString());
                        reply.writeNoException();
                        if (_result5 != null) {
                            parcel2.writeInt(1);
                            _result5.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 6:
                        InterfaceConfiguration _arg14;
                        parcel.enforceInterface(descriptor);
                        String _arg04 = data.readString();
                        if (data.readInt() != 0) {
                            _arg14 = (InterfaceConfiguration) InterfaceConfiguration.CREATOR.createFromParcel(parcel);
                        }
                        setInterfaceConfig(_arg04, _arg14);
                        reply.writeNoException();
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        clearInterfaceAddresses(data.readString());
                        reply.writeNoException();
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        setInterfaceDown(data.readString());
                        reply.writeNoException();
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        setInterfaceUp(data.readString());
                        reply.writeNoException();
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readString();
                        if (data.readInt() != 0) {
                            _arg12 = true;
                        }
                        setInterfaceIpv6PrivacyExtensions(_arg0, _arg12);
                        reply.writeNoException();
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        disableIpv6(data.readString());
                        reply.writeNoException();
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        enableIpv6(data.readString());
                        reply.writeNoException();
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        setIPv6AddrGenMode(data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg13 = (RouteInfo) RouteInfo.CREATOR.createFromParcel(parcel);
                        }
                        addRoute(_arg02, _arg13);
                        reply.writeNoException();
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg13 = (RouteInfo) RouteInfo.CREATOR.createFromParcel(parcel);
                        }
                        removeRoute(_arg02, _arg13);
                        reply.writeNoException();
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        setMtu(data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        shutdown();
                        reply.writeNoException();
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        _result2 = getIpForwardingEnabled();
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg12 = true;
                        }
                        setIpForwardingEnabled(_arg12);
                        reply.writeNoException();
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        startTethering(data.createStringArray());
                        reply.writeNoException();
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        stopTethering();
                        reply.writeNoException();
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        _result2 = isTetheringStarted();
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        tetherInterface(data.readString());
                        reply.writeNoException();
                        return true;
                    case 24:
                        parcel.enforceInterface(descriptor);
                        untetherInterface(data.readString());
                        reply.writeNoException();
                        return true;
                    case 25:
                        parcel.enforceInterface(descriptor);
                        _result = listTetheredInterfaces();
                        reply.writeNoException();
                        parcel2.writeStringArray(_result);
                        return true;
                    case 26:
                        Network _arg05;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg05 = (Network) Network.CREATOR.createFromParcel(parcel);
                        }
                        setDnsForwarders(_arg05, data.createStringArray());
                        reply.writeNoException();
                        return true;
                    case 27:
                        parcel.enforceInterface(descriptor);
                        _result = getDnsForwarders();
                        reply.writeNoException();
                        parcel2.writeStringArray(_result);
                        return true;
                    case 28:
                        parcel.enforceInterface(descriptor);
                        startInterfaceForwarding(data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 29:
                        parcel.enforceInterface(descriptor);
                        stopInterfaceForwarding(data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 30:
                        parcel.enforceInterface(descriptor);
                        enableNat(data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 31:
                        parcel.enforceInterface(descriptor);
                        disableNat(data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 32:
                        parcel.enforceInterface(descriptor);
                        registerTetheringStatsProvider(android.net.ITetheringStatsProvider.Stub.asInterface(data.readStrongBinder()), data.readString());
                        reply.writeNoException();
                        return true;
                    case 33:
                        parcel.enforceInterface(descriptor);
                        unregisterTetheringStatsProvider(android.net.ITetheringStatsProvider.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 34:
                        parcel.enforceInterface(descriptor);
                        tetherLimitReached(android.net.ITetheringStatsProvider.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 35:
                        parcel.enforceInterface(descriptor);
                        addUpstreamV6Interface(data.readString());
                        reply.writeNoException();
                        return true;
                    case 36:
                        parcel.enforceInterface(descriptor);
                        removeUpstreamV6Interface(data.readString());
                        reply.writeNoException();
                        return true;
                    case 37:
                        parcel.enforceInterface(descriptor);
                        _result = listTtys();
                        reply.writeNoException();
                        parcel2.writeStringArray(_result);
                        return true;
                    case 38:
                        parcel.enforceInterface(descriptor);
                        attachPppd(data.readString(), data.readString(), data.readString(), data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 39:
                        parcel.enforceInterface(descriptor);
                        detachPppd(data.readString());
                        reply.writeNoException();
                        return true;
                    case 40:
                        parcel.enforceInterface(descriptor);
                        _result3 = getNetworkStatsSummaryDev();
                        reply.writeNoException();
                        if (_result3 != null) {
                            parcel2.writeInt(1);
                            _result3.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 41:
                        parcel.enforceInterface(descriptor);
                        _result3 = getNetworkStatsSummaryXt();
                        reply.writeNoException();
                        if (_result3 != null) {
                            parcel2.writeInt(1);
                            _result3.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 42:
                        parcel.enforceInterface(descriptor);
                        _result3 = getNetworkStatsDetail();
                        reply.writeNoException();
                        if (_result3 != null) {
                            parcel2.writeInt(1);
                            _result3.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 43:
                        parcel.enforceInterface(descriptor);
                        NetworkStats _result6 = getNetworkStatsUidDetail(data.readInt(), data.createStringArray());
                        reply.writeNoException();
                        if (_result6 != null) {
                            parcel2.writeInt(1);
                            _result6.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 44:
                        parcel.enforceInterface(descriptor);
                        NetworkStats _result7 = getNetworkStatsTethering(data.readInt());
                        reply.writeNoException();
                        if (_result7 != null) {
                            parcel2.writeInt(1);
                            _result7.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 45:
                        parcel.enforceInterface(descriptor);
                        setInterfaceQuota(data.readString(), data.readLong());
                        reply.writeNoException();
                        return true;
                    case 46:
                        parcel.enforceInterface(descriptor);
                        removeInterfaceQuota(data.readString());
                        reply.writeNoException();
                        return true;
                    case 47:
                        parcel.enforceInterface(descriptor);
                        setInterfaceAlert(data.readString(), data.readLong());
                        reply.writeNoException();
                        return true;
                    case 48:
                        parcel.enforceInterface(descriptor);
                        removeInterfaceAlert(data.readString());
                        reply.writeNoException();
                        return true;
                    case 49:
                        parcel.enforceInterface(descriptor);
                        setGlobalAlert(data.readLong());
                        reply.writeNoException();
                        return true;
                    case 50:
                        parcel.enforceInterface(descriptor);
                        _arg03 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg12 = true;
                        }
                        setUidMeteredNetworkBlacklist(_arg03, _arg12);
                        reply.writeNoException();
                        return true;
                    case 51:
                        parcel.enforceInterface(descriptor);
                        _arg03 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg12 = true;
                        }
                        setUidMeteredNetworkWhitelist(_arg03, _arg12);
                        reply.writeNoException();
                        return true;
                    case 52:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg12 = true;
                        }
                        _arg12 = setDataSaverModeEnabled(_arg12);
                        reply.writeNoException();
                        parcel2.writeInt(_arg12);
                        return true;
                    case 53:
                        parcel.enforceInterface(descriptor);
                        setUidCleartextNetworkPolicy(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 54:
                        parcel.enforceInterface(descriptor);
                        _result2 = isBandwidthControlEnabled();
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 55:
                        parcel.enforceInterface(descriptor);
                        addIdleTimer(data.readString(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 56:
                        parcel.enforceInterface(descriptor);
                        removeIdleTimer(data.readString());
                        reply.writeNoException();
                        return true;
                    case 57:
                        parcel.enforceInterface(descriptor);
                        setDnsConfigurationForNetwork(data.readInt(), data.createStringArray(), data.createStringArray(), data.createIntArray(), data.readString(), data.createStringArray());
                        reply.writeNoException();
                        return true;
                    case 58:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg12 = true;
                        }
                        setFirewallEnabled(_arg12);
                        reply.writeNoException();
                        return true;
                    case 59:
                        parcel.enforceInterface(descriptor);
                        _result2 = isFirewallEnabled();
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 60:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readString();
                        if (data.readInt() != 0) {
                            _arg12 = true;
                        }
                        setFirewallInterfaceRule(_arg0, _arg12);
                        reply.writeNoException();
                        return true;
                    case 61:
                        parcel.enforceInterface(descriptor);
                        setFirewallUidRule(data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 62:
                        parcel.enforceInterface(descriptor);
                        setFirewallUidRules(data.readInt(), data.createIntArray(), data.createIntArray());
                        reply.writeNoException();
                        return true;
                    case 63:
                        parcel.enforceInterface(descriptor);
                        _arg03 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg12 = true;
                        }
                        setFirewallChainEnabled(_arg03, _arg12);
                        reply.writeNoException();
                        return true;
                    case 64:
                        parcel.enforceInterface(descriptor);
                        addVpnUidRanges(data.readInt(), (UidRange[]) parcel.createTypedArray(UidRange.CREATOR));
                        reply.writeNoException();
                        return true;
                    case 65:
                        parcel.enforceInterface(descriptor);
                        removeVpnUidRanges(data.readInt(), (UidRange[]) parcel.createTypedArray(UidRange.CREATOR));
                        reply.writeNoException();
                        return true;
                    case 66:
                        parcel.enforceInterface(descriptor);
                        startClatd(data.readString());
                        reply.writeNoException();
                        return true;
                    case 67:
                        parcel.enforceInterface(descriptor);
                        stopClatd(data.readString());
                        reply.writeNoException();
                        return true;
                    case 68:
                        parcel.enforceInterface(descriptor);
                        _arg12 = isClatdStarted(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_arg12);
                        return true;
                    case 69:
                        parcel.enforceInterface(descriptor);
                        registerNetworkActivityListener(android.os.INetworkActivityListener.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 70:
                        parcel.enforceInterface(descriptor);
                        unregisterNetworkActivityListener(android.os.INetworkActivityListener.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 71:
                        parcel.enforceInterface(descriptor);
                        _result2 = isNetworkActive();
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 72:
                        parcel.enforceInterface(descriptor);
                        createPhysicalNetwork(data.readInt(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 73:
                        parcel.enforceInterface(descriptor);
                        _arg03 = data.readInt();
                        boolean _arg15 = data.readInt() != 0;
                        if (data.readInt() != 0) {
                            _arg12 = true;
                        }
                        createVirtualNetwork(_arg03, _arg15, _arg12);
                        reply.writeNoException();
                        return true;
                    case 74:
                        parcel.enforceInterface(descriptor);
                        removeNetwork(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 75:
                        parcel.enforceInterface(descriptor);
                        addInterfaceToNetwork(data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 76:
                        parcel.enforceInterface(descriptor);
                        removeInterfaceFromNetwork(data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 77:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg13 = (RouteInfo) RouteInfo.CREATOR.createFromParcel(parcel);
                        }
                        addLegacyRouteForNetId(_arg02, _arg13, data.readInt());
                        reply.writeNoException();
                        return true;
                    case 78:
                        parcel.enforceInterface(descriptor);
                        setDefaultNetId(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 79:
                        parcel.enforceInterface(descriptor);
                        clearDefaultNetId();
                        reply.writeNoException();
                        return true;
                    case 80:
                        parcel.enforceInterface(descriptor);
                        setNetworkPermission(data.readInt(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 81:
                        parcel.enforceInterface(descriptor);
                        setPermission(data.readString(), data.createIntArray());
                        reply.writeNoException();
                        return true;
                    case 82:
                        parcel.enforceInterface(descriptor);
                        clearPermission(data.createIntArray());
                        reply.writeNoException();
                        return true;
                    case 83:
                        parcel.enforceInterface(descriptor);
                        allowProtect(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 84:
                        parcel.enforceInterface(descriptor);
                        denyProtect(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 85:
                        parcel.enforceInterface(descriptor);
                        addInterfaceToLocalNetwork(data.readString(), parcel.createTypedArrayList(RouteInfo.CREATOR));
                        reply.writeNoException();
                        return true;
                    case 86:
                        parcel.enforceInterface(descriptor);
                        removeInterfaceFromLocalNetwork(data.readString());
                        reply.writeNoException();
                        return true;
                    case 87:
                        parcel.enforceInterface(descriptor);
                        _arg02 = removeRoutesFromLocalNetwork(parcel.createTypedArrayList(RouteInfo.CREATOR));
                        reply.writeNoException();
                        parcel2.writeInt(_arg02);
                        return true;
                    case 88:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg12 = true;
                        }
                        setAllowOnlyVpnForUids(_arg12, (UidRange[]) parcel.createTypedArray(UidRange.CREATOR));
                        reply.writeNoException();
                        return true;
                    case 89:
                        parcel.enforceInterface(descriptor);
                        _arg12 = isNetworkRestricted(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg12);
                        return true;
                    case 90:
                        parcel.enforceInterface(descriptor);
                        _arg03 = getNetdPid();
                        reply.writeNoException();
                        parcel2.writeInt(_arg03);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel2.writeString(descriptor);
            return true;
        }
    }

    void addIdleTimer(String str, int i, int i2) throws RemoteException;

    void addInterfaceToLocalNetwork(String str, List<RouteInfo> list) throws RemoteException;

    void addInterfaceToNetwork(String str, int i) throws RemoteException;

    void addLegacyRouteForNetId(int i, RouteInfo routeInfo, int i2) throws RemoteException;

    void addRoute(int i, RouteInfo routeInfo) throws RemoteException;

    void addUpstreamV6Interface(String str) throws RemoteException;

    void addVpnUidRanges(int i, UidRange[] uidRangeArr) throws RemoteException;

    void allowProtect(int i) throws RemoteException;

    void attachPppd(String str, String str2, String str3, String str4, String str5) throws RemoteException;

    void clearDefaultNetId() throws RemoteException;

    void clearInterfaceAddresses(String str) throws RemoteException;

    void clearPermission(int[] iArr) throws RemoteException;

    void createPhysicalNetwork(int i, String str) throws RemoteException;

    void createVirtualNetwork(int i, boolean z, boolean z2) throws RemoteException;

    void denyProtect(int i) throws RemoteException;

    void detachPppd(String str) throws RemoteException;

    void disableIpv6(String str) throws RemoteException;

    void disableNat(String str, String str2) throws RemoteException;

    void enableIpv6(String str) throws RemoteException;

    void enableNat(String str, String str2) throws RemoteException;

    String[] getDnsForwarders() throws RemoteException;

    InterfaceConfiguration getInterfaceConfig(String str) throws RemoteException;

    boolean getIpForwardingEnabled() throws RemoteException;

    int getNetdPid() throws RemoteException;

    INetd getNetdService() throws RemoteException;

    NetworkStats getNetworkStatsDetail() throws RemoteException;

    NetworkStats getNetworkStatsSummaryDev() throws RemoteException;

    NetworkStats getNetworkStatsSummaryXt() throws RemoteException;

    NetworkStats getNetworkStatsTethering(int i) throws RemoteException;

    NetworkStats getNetworkStatsUidDetail(int i, String[] strArr) throws RemoteException;

    boolean isBandwidthControlEnabled() throws RemoteException;

    boolean isClatdStarted(String str) throws RemoteException;

    boolean isFirewallEnabled() throws RemoteException;

    boolean isNetworkActive() throws RemoteException;

    boolean isNetworkRestricted(int i) throws RemoteException;

    boolean isTetheringStarted() throws RemoteException;

    String[] listInterfaces() throws RemoteException;

    String[] listTetheredInterfaces() throws RemoteException;

    String[] listTtys() throws RemoteException;

    void registerNetworkActivityListener(INetworkActivityListener iNetworkActivityListener) throws RemoteException;

    void registerObserver(INetworkManagementEventObserver iNetworkManagementEventObserver) throws RemoteException;

    void registerTetheringStatsProvider(ITetheringStatsProvider iTetheringStatsProvider, String str) throws RemoteException;

    void removeIdleTimer(String str) throws RemoteException;

    void removeInterfaceAlert(String str) throws RemoteException;

    void removeInterfaceFromLocalNetwork(String str) throws RemoteException;

    void removeInterfaceFromNetwork(String str, int i) throws RemoteException;

    void removeInterfaceQuota(String str) throws RemoteException;

    void removeNetwork(int i) throws RemoteException;

    void removeRoute(int i, RouteInfo routeInfo) throws RemoteException;

    int removeRoutesFromLocalNetwork(List<RouteInfo> list) throws RemoteException;

    void removeUpstreamV6Interface(String str) throws RemoteException;

    void removeVpnUidRanges(int i, UidRange[] uidRangeArr) throws RemoteException;

    void setAllowOnlyVpnForUids(boolean z, UidRange[] uidRangeArr) throws RemoteException;

    boolean setDataSaverModeEnabled(boolean z) throws RemoteException;

    void setDefaultNetId(int i) throws RemoteException;

    void setDnsConfigurationForNetwork(int i, String[] strArr, String[] strArr2, int[] iArr, String str, String[] strArr3) throws RemoteException;

    void setDnsForwarders(Network network, String[] strArr) throws RemoteException;

    void setFirewallChainEnabled(int i, boolean z) throws RemoteException;

    void setFirewallEnabled(boolean z) throws RemoteException;

    void setFirewallInterfaceRule(String str, boolean z) throws RemoteException;

    void setFirewallUidRule(int i, int i2, int i3) throws RemoteException;

    void setFirewallUidRules(int i, int[] iArr, int[] iArr2) throws RemoteException;

    void setGlobalAlert(long j) throws RemoteException;

    void setIPv6AddrGenMode(String str, int i) throws RemoteException;

    void setInterfaceAlert(String str, long j) throws RemoteException;

    void setInterfaceConfig(String str, InterfaceConfiguration interfaceConfiguration) throws RemoteException;

    void setInterfaceDown(String str) throws RemoteException;

    void setInterfaceIpv6PrivacyExtensions(String str, boolean z) throws RemoteException;

    void setInterfaceQuota(String str, long j) throws RemoteException;

    void setInterfaceUp(String str) throws RemoteException;

    void setIpForwardingEnabled(boolean z) throws RemoteException;

    void setMtu(String str, int i) throws RemoteException;

    void setNetworkPermission(int i, String str) throws RemoteException;

    void setPermission(String str, int[] iArr) throws RemoteException;

    void setUidCleartextNetworkPolicy(int i, int i2) throws RemoteException;

    void setUidMeteredNetworkBlacklist(int i, boolean z) throws RemoteException;

    void setUidMeteredNetworkWhitelist(int i, boolean z) throws RemoteException;

    void shutdown() throws RemoteException;

    void startClatd(String str) throws RemoteException;

    void startInterfaceForwarding(String str, String str2) throws RemoteException;

    void startTethering(String[] strArr) throws RemoteException;

    void stopClatd(String str) throws RemoteException;

    void stopInterfaceForwarding(String str, String str2) throws RemoteException;

    void stopTethering() throws RemoteException;

    void tetherInterface(String str) throws RemoteException;

    void tetherLimitReached(ITetheringStatsProvider iTetheringStatsProvider) throws RemoteException;

    void unregisterNetworkActivityListener(INetworkActivityListener iNetworkActivityListener) throws RemoteException;

    void unregisterObserver(INetworkManagementEventObserver iNetworkManagementEventObserver) throws RemoteException;

    void unregisterTetheringStatsProvider(ITetheringStatsProvider iTetheringStatsProvider) throws RemoteException;

    void untetherInterface(String str) throws RemoteException;
}
