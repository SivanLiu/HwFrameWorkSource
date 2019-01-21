package android.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.os.RemoteException;
import java.io.FileDescriptor;

public interface INetd extends IInterface {
    public static final int CONF = 1;
    public static final String IPSEC_INTERFACE_PREFIX = "ipsec";
    public static final int IPV4 = 4;
    public static final int IPV6 = 6;
    public static final int IPV6_ADDR_GEN_MODE_DEFAULT = 0;
    public static final int IPV6_ADDR_GEN_MODE_EUI64 = 0;
    public static final int IPV6_ADDR_GEN_MODE_NONE = 1;
    public static final int IPV6_ADDR_GEN_MODE_RANDOM = 3;
    public static final int IPV6_ADDR_GEN_MODE_STABLE_PRIVACY = 2;
    public static final int NEIGH = 2;
    public static final String PERMISSION_NETWORK = "NETWORK";
    public static final String PERMISSION_SYSTEM = "SYSTEM";
    public static final int RESOLVER_PARAMS_COUNT = 4;
    public static final int RESOLVER_PARAMS_MAX_SAMPLES = 3;
    public static final int RESOLVER_PARAMS_MIN_SAMPLES = 2;
    public static final int RESOLVER_PARAMS_SAMPLE_VALIDITY = 0;
    public static final int RESOLVER_PARAMS_SUCCESS_THRESHOLD = 1;
    public static final int RESOLVER_STATS_COUNT = 7;
    public static final int RESOLVER_STATS_ERRORS = 1;
    public static final int RESOLVER_STATS_INTERNAL_ERRORS = 3;
    public static final int RESOLVER_STATS_LAST_SAMPLE_TIME = 5;
    public static final int RESOLVER_STATS_RTT_AVG = 4;
    public static final int RESOLVER_STATS_SUCCESSES = 0;
    public static final int RESOLVER_STATS_TIMEOUTS = 2;
    public static final int RESOLVER_STATS_USABLE = 6;
    public static final int TETHER_STATS_ARRAY_SIZE = 4;
    public static final int TETHER_STATS_RX_BYTES = 0;
    public static final int TETHER_STATS_RX_PACKETS = 1;
    public static final int TETHER_STATS_TX_BYTES = 2;
    public static final int TETHER_STATS_TX_PACKETS = 3;

    public static abstract class Stub extends Binder implements INetd {
        private static final String DESCRIPTOR = "android.net.INetd";
        static final int TRANSACTION_addVirtualTunnelInterface = 31;
        static final int TRANSACTION_bandwidthEnableDataSaver = 3;
        static final int TRANSACTION_firewallReplaceUidChain = 2;
        static final int TRANSACTION_getMetricsReportingLevel = 20;
        static final int TRANSACTION_getNetdPid = 34;
        static final int TRANSACTION_getResolverInfo = 14;
        static final int TRANSACTION_interfaceAddAddress = 17;
        static final int TRANSACTION_interfaceDelAddress = 18;
        static final int TRANSACTION_ipSecAddSecurityAssociation = 24;
        static final int TRANSACTION_ipSecAddSecurityPolicy = 28;
        static final int TRANSACTION_ipSecAllocateSpi = 23;
        static final int TRANSACTION_ipSecApplyTransportModeTransform = 26;
        static final int TRANSACTION_ipSecDeleteSecurityAssociation = 25;
        static final int TRANSACTION_ipSecDeleteSecurityPolicy = 30;
        static final int TRANSACTION_ipSecRemoveTransportModeTransform = 27;
        static final int TRANSACTION_ipSecSetEncapSocketOwner = 22;
        static final int TRANSACTION_ipSecUpdateSecurityPolicy = 29;
        static final int TRANSACTION_isAlive = 1;
        static final int TRANSACTION_networkAddInterface = 7;
        static final int TRANSACTION_networkAddUidRanges = 9;
        static final int TRANSACTION_networkCreatePhysical = 4;
        static final int TRANSACTION_networkCreateVpn = 5;
        static final int TRANSACTION_networkDestroy = 6;
        static final int TRANSACTION_networkRejectNonSecureVpn = 11;
        static final int TRANSACTION_networkRemoveInterface = 8;
        static final int TRANSACTION_networkRemoveUidRanges = 10;
        static final int TRANSACTION_removeVirtualTunnelInterface = 33;
        static final int TRANSACTION_setIPv6AddrGenMode = 37;
        static final int TRANSACTION_setMetricsReportingLevel = 21;
        static final int TRANSACTION_setProcSysNet = 19;
        static final int TRANSACTION_setResolverConfiguration = 13;
        static final int TRANSACTION_socketDestroy = 12;
        static final int TRANSACTION_tetherApplyDnsInterfaces = 15;
        static final int TRANSACTION_tetherGetStats = 16;
        static final int TRANSACTION_trafficCheckBpfStatsEnable = 38;
        static final int TRANSACTION_updateVirtualTunnelInterface = 32;
        static final int TRANSACTION_wakeupAddInterface = 35;
        static final int TRANSACTION_wakeupDelInterface = 36;

        private static class Proxy implements INetd {
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

            public boolean isAlive() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(1, _data, _reply, 0);
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

            public boolean firewallReplaceUidChain(String chainName, boolean isWhitelist, int[] uids) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(chainName);
                    _data.writeInt(isWhitelist);
                    _data.writeIntArray(uids);
                    boolean z = false;
                    this.mRemote.transact(2, _data, _reply, 0);
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

            public boolean bandwidthEnableDataSaver(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enable);
                    boolean z = false;
                    this.mRemote.transact(3, _data, _reply, 0);
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

            public void networkCreatePhysical(int netId, String permission) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeString(permission);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void networkCreateVpn(int netId, boolean hasDns, boolean secure) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeInt(hasDns);
                    _data.writeInt(secure);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void networkDestroy(int netId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void networkAddInterface(int netId, String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeString(iface);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void networkRemoveInterface(int netId, String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeString(iface);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void networkAddUidRanges(int netId, UidRange[] uidRanges) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeTypedArray(uidRanges, 0);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void networkRemoveUidRanges(int netId, UidRange[] uidRanges) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeTypedArray(uidRanges, 0);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void networkRejectNonSecureVpn(boolean add, UidRange[] uidRanges) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(add);
                    _data.writeTypedArray(uidRanges, 0);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void socketDestroy(UidRange[] uidRanges, int[] exemptUids) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedArray(uidRanges, 0);
                    _data.writeIntArray(exemptUids);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setResolverConfiguration(int netId, String[] servers, String[] domains, int[] params, String tlsName, String[] tlsServers, String[] tlsFingerprints) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeStringArray(servers);
                    _data.writeStringArray(domains);
                    _data.writeIntArray(params);
                    _data.writeString(tlsName);
                    _data.writeStringArray(tlsServers);
                    _data.writeStringArray(tlsFingerprints);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void getResolverInfo(int netId, String[] servers, String[] domains, int[] params, int[] stats) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    if (servers == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(servers.length);
                    }
                    if (domains == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(domains.length);
                    }
                    if (params == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(params.length);
                    }
                    if (stats == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(stats.length);
                    }
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    _reply.readStringArray(servers);
                    _reply.readStringArray(domains);
                    _reply.readIntArray(params);
                    _reply.readIntArray(stats);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean tetherApplyDnsInterfaces() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(15, _data, _reply, 0);
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

            public PersistableBundle tetherGetStats() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    PersistableBundle _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (PersistableBundle) PersistableBundle.CREATOR.createFromParcel(_reply);
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

            public void interfaceAddAddress(String ifName, String addrString, int prefixLength) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeString(addrString);
                    _data.writeInt(prefixLength);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void interfaceDelAddress(String ifName, String addrString, int prefixLength) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeString(addrString);
                    _data.writeInt(prefixLength);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setProcSysNet(int family, int which, String ifname, String parameter, String value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(family);
                    _data.writeInt(which);
                    _data.writeString(ifname);
                    _data.writeString(parameter);
                    _data.writeString(value);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getMetricsReportingLevel() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setMetricsReportingLevel(int level) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(level);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void ipSecSetEncapSocketOwner(FileDescriptor socket, int newUid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeRawFileDescriptor(socket);
                    _data.writeInt(newUid);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int ipSecAllocateSpi(int transformId, String sourceAddress, String destinationAddress, int spi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(transformId);
                    _data.writeString(sourceAddress);
                    _data.writeString(destinationAddress);
                    _data.writeInt(spi);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void ipSecAddSecurityAssociation(int transformId, int mode, String sourceAddress, String destinationAddress, int underlyingNetId, int spi, int markValue, int markMask, String authAlgo, byte[] authKey, int authTruncBits, String cryptAlgo, byte[] cryptKey, int cryptTruncBits, String aeadAlgo, byte[] aeadKey, int aeadIcvBits, int encapType, int encapLocalPort, int encapRemotePort) throws RemoteException {
                Throwable th;
                int i;
                int i2;
                int i3;
                int i4;
                String str;
                byte[] bArr;
                int i5;
                String str2;
                byte[] bArr2;
                String str3;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(transformId);
                    _data.writeInt(mode);
                    try {
                        _data.writeString(sourceAddress);
                        try {
                            _data.writeString(destinationAddress);
                        } catch (Throwable th2) {
                            th = th2;
                            i = underlyingNetId;
                            i2 = spi;
                            i3 = markValue;
                            i4 = markMask;
                            str = authAlgo;
                            bArr = authKey;
                            i5 = authTruncBits;
                            str2 = cryptAlgo;
                            bArr2 = cryptKey;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        str3 = destinationAddress;
                        i = underlyingNetId;
                        i2 = spi;
                        i3 = markValue;
                        i4 = markMask;
                        str = authAlgo;
                        bArr = authKey;
                        i5 = authTruncBits;
                        str2 = cryptAlgo;
                        bArr2 = cryptKey;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeInt(underlyingNetId);
                        try {
                            _data.writeInt(spi);
                            try {
                                _data.writeInt(markValue);
                            } catch (Throwable th4) {
                                th = th4;
                                i4 = markMask;
                                str = authAlgo;
                                bArr = authKey;
                                i5 = authTruncBits;
                                str2 = cryptAlgo;
                                bArr2 = cryptKey;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            i3 = markValue;
                            i4 = markMask;
                            str = authAlgo;
                            bArr = authKey;
                            i5 = authTruncBits;
                            str2 = cryptAlgo;
                            bArr2 = cryptKey;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        i2 = spi;
                        i3 = markValue;
                        i4 = markMask;
                        str = authAlgo;
                        bArr = authKey;
                        i5 = authTruncBits;
                        str2 = cryptAlgo;
                        bArr2 = cryptKey;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeInt(markMask);
                        try {
                            _data.writeString(authAlgo);
                            try {
                                _data.writeByteArray(authKey);
                            } catch (Throwable th7) {
                                th = th7;
                                i5 = authTruncBits;
                                str2 = cryptAlgo;
                                bArr2 = cryptKey;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th8) {
                            th = th8;
                            bArr = authKey;
                            i5 = authTruncBits;
                            str2 = cryptAlgo;
                            bArr2 = cryptKey;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th9) {
                        th = th9;
                        str = authAlgo;
                        bArr = authKey;
                        i5 = authTruncBits;
                        str2 = cryptAlgo;
                        bArr2 = cryptKey;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeInt(authTruncBits);
                        try {
                            _data.writeString(cryptAlgo);
                            try {
                                _data.writeByteArray(cryptKey);
                                _data.writeInt(cryptTruncBits);
                                _data.writeString(aeadAlgo);
                                _data.writeByteArray(aeadKey);
                                _data.writeInt(aeadIcvBits);
                                _data.writeInt(encapType);
                                _data.writeInt(encapLocalPort);
                                _data.writeInt(encapRemotePort);
                                this.mRemote.transact(24, _data, _reply, 0);
                                _reply.readException();
                                _reply.recycle();
                                _data.recycle();
                            } catch (Throwable th10) {
                                th = th10;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th11) {
                            th = th11;
                            bArr2 = cryptKey;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th12) {
                        th = th12;
                        str2 = cryptAlgo;
                        bArr2 = cryptKey;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th13) {
                    th = th13;
                    String str4 = sourceAddress;
                    str3 = destinationAddress;
                    i = underlyingNetId;
                    i2 = spi;
                    i3 = markValue;
                    i4 = markMask;
                    str = authAlgo;
                    bArr = authKey;
                    i5 = authTruncBits;
                    str2 = cryptAlgo;
                    bArr2 = cryptKey;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            public void ipSecDeleteSecurityAssociation(int transformId, String sourceAddress, String destinationAddress, int spi, int markValue, int markMask) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(transformId);
                    _data.writeString(sourceAddress);
                    _data.writeString(destinationAddress);
                    _data.writeInt(spi);
                    _data.writeInt(markValue);
                    _data.writeInt(markMask);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void ipSecApplyTransportModeTransform(FileDescriptor socket, int transformId, int direction, String sourceAddress, String destinationAddress, int spi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeRawFileDescriptor(socket);
                    _data.writeInt(transformId);
                    _data.writeInt(direction);
                    _data.writeString(sourceAddress);
                    _data.writeString(destinationAddress);
                    _data.writeInt(spi);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void ipSecRemoveTransportModeTransform(FileDescriptor socket) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeRawFileDescriptor(socket);
                    this.mRemote.transact(Stub.TRANSACTION_ipSecRemoveTransportModeTransform, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void ipSecAddSecurityPolicy(int transformId, int direction, String sourceAddress, String destinationAddress, int spi, int markValue, int markMask) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(transformId);
                    _data.writeInt(direction);
                    _data.writeString(sourceAddress);
                    _data.writeString(destinationAddress);
                    _data.writeInt(spi);
                    _data.writeInt(markValue);
                    _data.writeInt(markMask);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void ipSecUpdateSecurityPolicy(int transformId, int direction, String sourceAddress, String destinationAddress, int spi, int markValue, int markMask) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(transformId);
                    _data.writeInt(direction);
                    _data.writeString(sourceAddress);
                    _data.writeString(destinationAddress);
                    _data.writeInt(spi);
                    _data.writeInt(markValue);
                    _data.writeInt(markMask);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void ipSecDeleteSecurityPolicy(int transformId, int direction, String sourceAddress, String destinationAddress, int markValue, int markMask) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(transformId);
                    _data.writeInt(direction);
                    _data.writeString(sourceAddress);
                    _data.writeString(destinationAddress);
                    _data.writeInt(markValue);
                    _data.writeInt(markMask);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void addVirtualTunnelInterface(String deviceName, String localAddress, String remoteAddress, int iKey, int oKey) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(deviceName);
                    _data.writeString(localAddress);
                    _data.writeString(remoteAddress);
                    _data.writeInt(iKey);
                    _data.writeInt(oKey);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void updateVirtualTunnelInterface(String deviceName, String localAddress, String remoteAddress, int iKey, int oKey) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(deviceName);
                    _data.writeString(localAddress);
                    _data.writeString(remoteAddress);
                    _data.writeInt(iKey);
                    _data.writeInt(oKey);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeVirtualTunnelInterface(String deviceName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(deviceName);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getNetdPid() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void wakeupAddInterface(String ifName, String prefix, int mark, int mask) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeString(prefix);
                    _data.writeInt(mark);
                    _data.writeInt(mask);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void wakeupDelInterface(String ifName, String prefix, int mark, int mask) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeString(prefix);
                    _data.writeInt(mark);
                    _data.writeInt(mask);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setIPv6AddrGenMode(String ifName, int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeInt(mode);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean trafficCheckBpfStatsEnable() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
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
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INetd asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof INetd)) {
                return new Proxy(obj);
            }
            return (INetd) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            Parcel parcel2 = reply;
            String descriptor = DESCRIPTOR;
            boolean z;
            String descriptor2;
            Parcel parcel3;
            if (i != 1598968902) {
                boolean _arg0 = false;
                int _result;
                switch (i) {
                    case 1:
                        z = true;
                        descriptor2 = descriptor;
                        parcel.enforceInterface(descriptor2);
                        _arg0 = isAlive();
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return z;
                    case 2:
                        z = true;
                        descriptor2 = descriptor;
                        parcel.enforceInterface(descriptor2);
                        String _arg02 = data.readString();
                        if (data.readInt() != 0) {
                            _arg0 = z;
                        }
                        boolean _result2 = firewallReplaceUidChain(_arg02, _arg0, data.createIntArray());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return z;
                    case 3:
                        z = true;
                        descriptor2 = descriptor;
                        parcel.enforceInterface(descriptor2);
                        if (data.readInt() != 0) {
                            _arg0 = z;
                        }
                        boolean _result3 = bandwidthEnableDataSaver(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return z;
                    case 4:
                        z = true;
                        descriptor2 = descriptor;
                        parcel.enforceInterface(descriptor2);
                        networkCreatePhysical(data.readInt(), data.readString());
                        reply.writeNoException();
                        return z;
                    case 5:
                        z = true;
                        descriptor2 = descriptor;
                        parcel.enforceInterface(descriptor2);
                        int _arg03 = data.readInt();
                        boolean _arg1 = data.readInt() != 0 ? z : false;
                        if (data.readInt() != 0) {
                            _arg0 = z;
                        }
                        networkCreateVpn(_arg03, _arg1, _arg0);
                        reply.writeNoException();
                        return z;
                    case 6:
                        z = true;
                        descriptor2 = descriptor;
                        parcel.enforceInterface(descriptor2);
                        networkDestroy(data.readInt());
                        reply.writeNoException();
                        return z;
                    case 7:
                        z = true;
                        descriptor2 = descriptor;
                        parcel.enforceInterface(descriptor2);
                        networkAddInterface(data.readInt(), data.readString());
                        reply.writeNoException();
                        return z;
                    case 8:
                        z = true;
                        descriptor2 = descriptor;
                        parcel.enforceInterface(descriptor2);
                        networkRemoveInterface(data.readInt(), data.readString());
                        reply.writeNoException();
                        return z;
                    case 9:
                        z = true;
                        descriptor2 = descriptor;
                        parcel3 = parcel;
                        parcel3.enforceInterface(descriptor2);
                        networkAddUidRanges(data.readInt(), (UidRange[]) parcel3.createTypedArray(UidRange.CREATOR));
                        reply.writeNoException();
                        return z;
                    case 10:
                        z = true;
                        descriptor2 = descriptor;
                        parcel3 = parcel;
                        parcel3.enforceInterface(descriptor2);
                        networkRemoveUidRanges(data.readInt(), (UidRange[]) parcel3.createTypedArray(UidRange.CREATOR));
                        reply.writeNoException();
                        return z;
                    case 11:
                        z = true;
                        descriptor2 = descriptor;
                        parcel3 = parcel;
                        parcel3.enforceInterface(descriptor2);
                        if (data.readInt() != 0) {
                            _arg0 = z;
                        }
                        networkRejectNonSecureVpn(_arg0, (UidRange[]) parcel3.createTypedArray(UidRange.CREATOR));
                        reply.writeNoException();
                        return z;
                    case 12:
                        z = true;
                        descriptor2 = descriptor;
                        parcel3 = parcel;
                        parcel3.enforceInterface(descriptor2);
                        socketDestroy((UidRange[]) parcel3.createTypedArray(UidRange.CREATOR), data.createIntArray());
                        reply.writeNoException();
                        return z;
                    case 13:
                        z = true;
                        descriptor2 = descriptor;
                        parcel.enforceInterface(descriptor2);
                        setResolverConfiguration(data.readInt(), data.createStringArray(), data.createStringArray(), data.createIntArray(), data.readString(), data.createStringArray(), data.createStringArray());
                        reply.writeNoException();
                        return z;
                    case 14:
                        String[] _arg12;
                        int[] _arg3;
                        z = true;
                        descriptor2 = descriptor;
                        parcel.enforceInterface(descriptor2);
                        int _arg04 = data.readInt();
                        int _arg1_length = data.readInt();
                        if (_arg1_length < 0) {
                            _arg12 = null;
                        } else {
                            _arg12 = new String[_arg1_length];
                        }
                        String[] _arg13 = _arg12;
                        i = data.readInt();
                        if (i < 0) {
                            _arg12 = null;
                        } else {
                            _arg12 = new String[i];
                        }
                        String[] _arg2 = _arg12;
                        int _arg3_length = data.readInt();
                        if (_arg3_length < 0) {
                            _arg3 = null;
                        } else {
                            _arg3 = new int[_arg3_length];
                        }
                        int[] iArr = _arg3;
                        int _arg4_length = data.readInt();
                        if (_arg4_length < 0) {
                            _arg3 = null;
                        } else {
                            _arg3 = new int[_arg4_length];
                        }
                        int[] _arg4 = _arg3;
                        int[] _arg32 = iArr;
                        getResolverInfo(_arg04, _arg13, _arg2, iArr, _arg4);
                        reply.writeNoException();
                        parcel2.writeStringArray(_arg13);
                        parcel2.writeStringArray(_arg2);
                        parcel2.writeIntArray(_arg32);
                        parcel2.writeIntArray(_arg4);
                        return z;
                    case 15:
                        z = true;
                        descriptor2 = descriptor;
                        parcel.enforceInterface(descriptor2);
                        _arg0 = tetherApplyDnsInterfaces();
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return z;
                    case 16:
                        int i2 = 1;
                        descriptor2 = descriptor;
                        parcel.enforceInterface(descriptor2);
                        PersistableBundle _result4 = tetherGetStats();
                        reply.writeNoException();
                        if (_result4 != null) {
                            parcel2.writeInt(i2);
                            _result4.writeToParcel(parcel2, i2);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return i2;
                    case 17:
                        z = true;
                        descriptor2 = descriptor;
                        parcel.enforceInterface(descriptor2);
                        interfaceAddAddress(data.readString(), data.readString(), data.readInt());
                        reply.writeNoException();
                        return z;
                    case 18:
                        z = true;
                        descriptor2 = descriptor;
                        parcel.enforceInterface(descriptor2);
                        interfaceDelAddress(data.readString(), data.readString(), data.readInt());
                        reply.writeNoException();
                        return z;
                    case 19:
                        z = true;
                        descriptor2 = descriptor;
                        parcel.enforceInterface(descriptor2);
                        setProcSysNet(data.readInt(), data.readInt(), data.readString(), data.readString(), data.readString());
                        reply.writeNoException();
                        return z;
                    case 20:
                        z = true;
                        descriptor2 = descriptor;
                        parcel.enforceInterface(descriptor2);
                        _result = getMetricsReportingLevel();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return z;
                    case 21:
                        z = true;
                        descriptor2 = descriptor;
                        parcel.enforceInterface(descriptor2);
                        setMetricsReportingLevel(data.readInt());
                        reply.writeNoException();
                        return z;
                    case 22:
                        z = true;
                        descriptor2 = descriptor;
                        parcel.enforceInterface(descriptor2);
                        ipSecSetEncapSocketOwner(data.readRawFileDescriptor(), data.readInt());
                        reply.writeNoException();
                        return z;
                    case 23:
                        z = true;
                        data.enforceInterface(descriptor);
                        int _result5 = ipSecAllocateSpi(data.readInt(), data.readString(), data.readString(), data.readInt());
                        reply.writeNoException();
                        reply.writeInt(_result5);
                        return z;
                    case 24:
                        parcel.enforceInterface(descriptor);
                        ipSecAddSecurityAssociation(data.readInt(), data.readInt(), data.readString(), data.readString(), data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readString(), data.createByteArray(), data.readInt(), data.readString(), data.createByteArray(), data.readInt(), data.readString(), data.createByteArray(), data.readInt(), data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 25:
                        parcel.enforceInterface(descriptor);
                        ipSecDeleteSecurityAssociation(data.readInt(), data.readString(), data.readString(), data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 26:
                        parcel.enforceInterface(descriptor);
                        ipSecApplyTransportModeTransform(data.readRawFileDescriptor(), data.readInt(), data.readInt(), data.readString(), data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case TRANSACTION_ipSecRemoveTransportModeTransform /*27*/:
                        parcel.enforceInterface(descriptor);
                        ipSecRemoveTransportModeTransform(data.readRawFileDescriptor());
                        reply.writeNoException();
                        return true;
                    case 28:
                        parcel.enforceInterface(descriptor);
                        ipSecAddSecurityPolicy(data.readInt(), data.readInt(), data.readString(), data.readString(), data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 29:
                        parcel.enforceInterface(descriptor);
                        ipSecUpdateSecurityPolicy(data.readInt(), data.readInt(), data.readString(), data.readString(), data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 30:
                        parcel.enforceInterface(descriptor);
                        ipSecDeleteSecurityPolicy(data.readInt(), data.readInt(), data.readString(), data.readString(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 31:
                        parcel.enforceInterface(descriptor);
                        addVirtualTunnelInterface(data.readString(), data.readString(), data.readString(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 32:
                        parcel.enforceInterface(descriptor);
                        updateVirtualTunnelInterface(data.readString(), data.readString(), data.readString(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 33:
                        parcel.enforceInterface(descriptor);
                        removeVirtualTunnelInterface(data.readString());
                        reply.writeNoException();
                        return true;
                    case 34:
                        parcel.enforceInterface(descriptor);
                        _result = getNetdPid();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 35:
                        parcel.enforceInterface(descriptor);
                        wakeupAddInterface(data.readString(), data.readString(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 36:
                        parcel.enforceInterface(descriptor);
                        wakeupDelInterface(data.readString(), data.readString(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 37:
                        parcel.enforceInterface(descriptor);
                        setIPv6AddrGenMode(data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 38:
                        parcel.enforceInterface(descriptor);
                        _arg0 = trafficCheckBpfStatsEnable();
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            z = true;
            descriptor2 = descriptor;
            parcel3 = parcel;
            parcel2.writeString(descriptor2);
            return z;
        }
    }

    void addVirtualTunnelInterface(String str, String str2, String str3, int i, int i2) throws RemoteException;

    boolean bandwidthEnableDataSaver(boolean z) throws RemoteException;

    boolean firewallReplaceUidChain(String str, boolean z, int[] iArr) throws RemoteException;

    int getMetricsReportingLevel() throws RemoteException;

    int getNetdPid() throws RemoteException;

    void getResolverInfo(int i, String[] strArr, String[] strArr2, int[] iArr, int[] iArr2) throws RemoteException;

    void interfaceAddAddress(String str, String str2, int i) throws RemoteException;

    void interfaceDelAddress(String str, String str2, int i) throws RemoteException;

    void ipSecAddSecurityAssociation(int i, int i2, String str, String str2, int i3, int i4, int i5, int i6, String str3, byte[] bArr, int i7, String str4, byte[] bArr2, int i8, String str5, byte[] bArr3, int i9, int i10, int i11, int i12) throws RemoteException;

    void ipSecAddSecurityPolicy(int i, int i2, String str, String str2, int i3, int i4, int i5) throws RemoteException;

    int ipSecAllocateSpi(int i, String str, String str2, int i2) throws RemoteException;

    void ipSecApplyTransportModeTransform(FileDescriptor fileDescriptor, int i, int i2, String str, String str2, int i3) throws RemoteException;

    void ipSecDeleteSecurityAssociation(int i, String str, String str2, int i2, int i3, int i4) throws RemoteException;

    void ipSecDeleteSecurityPolicy(int i, int i2, String str, String str2, int i3, int i4) throws RemoteException;

    void ipSecRemoveTransportModeTransform(FileDescriptor fileDescriptor) throws RemoteException;

    void ipSecSetEncapSocketOwner(FileDescriptor fileDescriptor, int i) throws RemoteException;

    void ipSecUpdateSecurityPolicy(int i, int i2, String str, String str2, int i3, int i4, int i5) throws RemoteException;

    boolean isAlive() throws RemoteException;

    void networkAddInterface(int i, String str) throws RemoteException;

    void networkAddUidRanges(int i, UidRange[] uidRangeArr) throws RemoteException;

    void networkCreatePhysical(int i, String str) throws RemoteException;

    void networkCreateVpn(int i, boolean z, boolean z2) throws RemoteException;

    void networkDestroy(int i) throws RemoteException;

    void networkRejectNonSecureVpn(boolean z, UidRange[] uidRangeArr) throws RemoteException;

    void networkRemoveInterface(int i, String str) throws RemoteException;

    void networkRemoveUidRanges(int i, UidRange[] uidRangeArr) throws RemoteException;

    void removeVirtualTunnelInterface(String str) throws RemoteException;

    void setIPv6AddrGenMode(String str, int i) throws RemoteException;

    void setMetricsReportingLevel(int i) throws RemoteException;

    void setProcSysNet(int i, int i2, String str, String str2, String str3) throws RemoteException;

    void setResolverConfiguration(int i, String[] strArr, String[] strArr2, int[] iArr, String str, String[] strArr3, String[] strArr4) throws RemoteException;

    void socketDestroy(UidRange[] uidRangeArr, int[] iArr) throws RemoteException;

    boolean tetherApplyDnsInterfaces() throws RemoteException;

    PersistableBundle tetherGetStats() throws RemoteException;

    boolean trafficCheckBpfStatsEnable() throws RemoteException;

    void updateVirtualTunnelInterface(String str, String str2, String str3, int i, int i2) throws RemoteException;

    void wakeupAddInterface(String str, String str2, int i, int i2) throws RemoteException;

    void wakeupDelInterface(String str, String str2, int i, int i2) throws RemoteException;
}
