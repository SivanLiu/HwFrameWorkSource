package com.android.server.connectivity.tethering;

import android.net.INetd;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.RouteInfo;
import android.net.ip.RouterAdvertisementDaemon;
import android.net.ip.RouterAdvertisementDaemon.RaParams;
import android.net.util.NetdService;
import android.net.util.SharedLog;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.util.Slog;
import java.net.Inet6Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Random;

public class IPv6TetheringInterfaceServices {
    private static final IpPrefix LINK_LOCAL_PREFIX = new IpPrefix("fe80::/64");
    private static final String TAG = IPv6TetheringInterfaceServices.class.getSimpleName();
    private byte[] mHwAddr;
    private final String mIfName;
    private LinkProperties mLastIPv6LinkProperties;
    private RaParams mLastRaParams;
    private final SharedLog mLog;
    private final INetworkManagementService mNMService;
    private NetworkInterface mNetworkInterface;
    private RouterAdvertisementDaemon mRaDaemon;

    public IPv6TetheringInterfaceServices(String ifname, INetworkManagementService nms, SharedLog log) {
        this.mIfName = ifname;
        this.mNMService = nms;
        this.mLog = log.forSubComponent(this.mIfName);
    }

    public boolean start() {
        try {
            this.mNetworkInterface = NetworkInterface.getByName(this.mIfName);
            if (this.mNetworkInterface == null) {
                this.mLog.e("Failed to find NetworkInterface");
                stop();
                return false;
            }
            try {
                this.mHwAddr = this.mNetworkInterface.getHardwareAddress();
                this.mRaDaemon = new RouterAdvertisementDaemon(this.mIfName, this.mNetworkInterface.getIndex(), this.mHwAddr);
                if (this.mRaDaemon.start()) {
                    return true;
                }
                stop();
                return false;
            } catch (SocketException e) {
                this.mLog.e("Failed to find hardware address: " + e);
                stop();
                return false;
            }
        } catch (SocketException e2) {
            this.mLog.e("Error looking up NetworkInterfaces: " + e2);
            stop();
            return false;
        }
    }

    public void stop() {
        this.mNetworkInterface = null;
        this.mHwAddr = null;
        setRaParams(null);
        if (this.mRaDaemon != null) {
            this.mRaDaemon.stop();
            this.mRaDaemon = null;
        }
    }

    public void updateUpstreamIPv6LinkProperties(LinkProperties v6only) {
        if (this.mRaDaemon != null && !Objects.equals(this.mLastIPv6LinkProperties, v6only)) {
            RaParams params = null;
            if (v6only != null) {
                params = new RaParams();
                params.mtu = v6only.getMtu();
                params.hasDefaultRoute = v6only.hasIPv6DefaultRoute();
                for (LinkAddress linkAddr : v6only.getLinkAddresses()) {
                    if (linkAddr.getPrefixLength() == 64) {
                        IpPrefix prefix = new IpPrefix(linkAddr.getAddress(), linkAddr.getPrefixLength());
                        params.prefixes.add(prefix);
                        Inet6Address dnsServer = getLocalDnsIpFor(prefix);
                        if (dnsServer != null) {
                            params.dnses.add(dnsServer);
                        }
                    }
                }
            }
            setRaParams(params);
            this.mLastIPv6LinkProperties = v6only;
        }
    }

    private void configureLocalRoutes(HashSet<IpPrefix> deprecatedPrefixes, HashSet<IpPrefix> newPrefixes) {
        if (!deprecatedPrefixes.isEmpty()) {
            try {
                if (this.mNMService.removeRoutesFromLocalNetwork(getLocalRoutesFor(deprecatedPrefixes)) > 0) {
                    this.mLog.e(String.format("Failed to remove %d IPv6 routes from local table.", new Object[]{Integer.valueOf(removalFailures)}));
                }
            } catch (RemoteException e) {
                this.mLog.e("Failed to remove IPv6 routes from local table: " + e);
            }
        }
        if (newPrefixes != null && (newPrefixes.isEmpty() ^ 1) != 0) {
            HashSet<IpPrefix> addedPrefixes = (HashSet) newPrefixes.clone();
            if (this.mLastRaParams != null) {
                addedPrefixes.removeAll(this.mLastRaParams.prefixes);
            }
            if (this.mLastRaParams == null || this.mLastRaParams.prefixes.isEmpty()) {
                addedPrefixes.add(LINK_LOCAL_PREFIX);
            }
            if (!addedPrefixes.isEmpty()) {
                try {
                    this.mNMService.addInterfaceToLocalNetwork(this.mIfName, getLocalRoutesFor(addedPrefixes));
                } catch (RemoteException e2) {
                    this.mLog.e("Failed to add IPv6 routes to local table: " + e2);
                }
            }
        }
    }

    private void configureLocalDns(HashSet<Inet6Address> deprecatedDnses, HashSet<Inet6Address> newDnses) {
        String dnsString;
        INetd netd = NetdService.getInstance();
        if (netd == null) {
            if (newDnses != null) {
                newDnses.clear();
            }
            this.mLog.e("No netd service instance available; not setting local IPv6 addresses");
            return;
        }
        if (!deprecatedDnses.isEmpty()) {
            for (Inet6Address dns : deprecatedDnses) {
                dnsString = dns.getHostAddress();
                try {
                    netd.interfaceDelAddress(this.mIfName, dnsString, 64);
                } catch (Exception e) {
                    this.mLog.e("Failed to remove local dns IP " + dnsString + ": " + e);
                }
            }
        }
        if (!(newDnses == null || (newDnses.isEmpty() ^ 1) == 0)) {
            HashSet<Inet6Address> addedDnses = (HashSet) newDnses.clone();
            if (this.mLastRaParams != null) {
                addedDnses.removeAll(this.mLastRaParams.dnses);
            }
            for (Inet6Address dns2 : addedDnses) {
                dnsString = dns2.getHostAddress();
                try {
                    netd.interfaceAddAddress(this.mIfName, dnsString, 64);
                } catch (Exception e2) {
                    this.mLog.e("Failed to add local dns IP " + dnsString + ": " + e2);
                    newDnses.remove(dns2);
                }
            }
        }
        try {
            netd.tetherApplyDnsInterfaces();
        } catch (ServiceSpecificException e3) {
            this.mLog.e("Failed to update local DNS caching server");
            if (newDnses != null) {
                newDnses.clear();
            }
        }
    }

    private void setRaParams(RaParams newParams) {
        HashSet hashSet = null;
        if (this.mRaDaemon != null) {
            HashSet hashSet2;
            RaParams deprecatedParams = RaParams.getDeprecatedRaParams(this.mLastRaParams, newParams);
            HashSet hashSet3 = deprecatedParams.prefixes;
            if (newParams != null) {
                hashSet2 = newParams.prefixes;
            } else {
                hashSet2 = null;
            }
            configureLocalRoutes(hashSet3, hashSet2);
            hashSet2 = deprecatedParams.dnses;
            if (newParams != null) {
                hashSet = newParams.dnses;
            }
            configureLocalDns(hashSet2, hashSet);
            this.mRaDaemon.buildNewRa(deprecatedParams, newParams);
        }
        this.mLastRaParams = newParams;
    }

    private ArrayList<RouteInfo> getLocalRoutesFor(HashSet<IpPrefix> prefixes) {
        ArrayList<RouteInfo> localRoutes = new ArrayList();
        for (IpPrefix ipp : prefixes) {
            localRoutes.add(new RouteInfo(ipp, null, this.mIfName));
        }
        return localRoutes;
    }

    private static Inet6Address getLocalDnsIpFor(IpPrefix localPrefix) {
        byte[] dnsBytes = localPrefix.getRawAddress();
        dnsBytes[dnsBytes.length - 1] = getRandomNonZeroByte();
        try {
            return Inet6Address.getByAddress(null, dnsBytes, 0);
        } catch (UnknownHostException e) {
            Slog.wtf(TAG, "Failed to construct Inet6Address from: " + localPrefix);
            return null;
        }
    }

    private static byte getRandomNonZeroByte() {
        byte random = (byte) new Random().nextInt();
        return random != (byte) 0 ? random : (byte) 1;
    }
}
