package com.android.server.intellicom.networkslice.model;

import android.net.NetworkRequest;
import android.os.Messenger;
import android.util.Log;
import com.android.server.intellicom.networkslice.css.NetworkSliceCallback;
import com.android.server.intellicom.networkslice.model.TrafficDescriptors;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class NetworkSliceInfo {
    public static final int INVALID_NET_ID = -1;
    private static final String SEPARATOR_FOR_NORMAL_DATA = ",";
    private FqdnIps mFqdnIps;
    private Map<Integer, Messenger> mMessengers = new ConcurrentHashMap();
    private int mNetId = -1;
    private NetworkSliceCallback mNetworkCallback;
    private int mNetworkCapability;
    private NetworkRequest mNetworkRequest;
    private Map<Integer, NetworkRequest> mNetworkRequests = new ConcurrentHashMap();
    private RouteSelectionDescriptor mRouteSelectionDescriptor;
    private TrafficDescriptors mTrafficDescriptors;
    private Set<Integer> mUids = new HashSet();
    private Set<Integer> mUsedUids = new HashSet();
    private Set<FqdnIps> mWaittingFqdnIps = new HashSet();

    public enum ParaType {
        NETWORK_CALLBACK,
        NETWORK_REQUEST,
        ROUTE_SELECTION_DESCRIPTOR
    }

    public boolean hasAllreadyBinded(int uid, FqdnIps fqdnIps) {
        if (this.mTrafficDescriptors == null || this.mUids == null) {
            return true;
        }
        int i = AnonymousClass1.$SwitchMap$com$android$server$intellicom$networkslice$model$TrafficDescriptors$RouteBindType[this.mTrafficDescriptors.getRouteBindType().ordinal()];
        if (i == 1) {
            log("mUids.contains(uid)=" + this.mUids.contains(Integer.valueOf(uid)));
            return this.mUids.contains(Integer.valueOf(uid));
        } else if (i == 2) {
            return isNoNewFqdnIp(fqdnIps);
        } else {
            if (i != 3) {
                log("Invalid TrafficDescriptors RouteBindType.");
                return true;
            } else if (!this.mUids.contains(Integer.valueOf(uid)) || !isNoNewFqdnIp(fqdnIps)) {
                return false;
            } else {
                return true;
            }
        }
    }

    public boolean isNeedCacheUid() {
        if (this.mTrafficDescriptors == null) {
            return false;
        }
        int i = AnonymousClass1.$SwitchMap$com$android$server$intellicom$networkslice$model$TrafficDescriptors$RouteBindType[this.mTrafficDescriptors.getRouteBindType().ordinal()];
        if (i != 1) {
            if (i != 2) {
                if (i != 3) {
                    if (i != 4) {
                        return false;
                    }
                }
            }
            return false;
        }
        return true;
    }

    public TrafficDescriptors.RouteBindType getRouteBindType() {
        TrafficDescriptors trafficDescriptors = this.mTrafficDescriptors;
        if (trafficDescriptors == null) {
            return TrafficDescriptors.RouteBindType.INVALID_TDS;
        }
        return trafficDescriptors.getRouteBindType();
    }

    public String getUidsStr() {
        return (String) this.mUids.stream().map($$Lambda$NetworkSliceInfo$EswCzJ6GYs8Kp_7iyPdnM5eKhNQ.INSTANCE).collect(Collectors.joining(","));
    }

    private boolean isNoNewFqdnIp(FqdnIps fqdnIps) {
        FqdnIps fqdnIps2 = this.mFqdnIps;
        if (fqdnIps2 == null) {
            return true;
        }
        return fqdnIps2.getNewFqdnIps(fqdnIps).isEmpty();
    }

    /* renamed from: com.android.server.intellicom.networkslice.model.NetworkSliceInfo$1  reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$android$server$intellicom$networkslice$model$NetworkSliceInfo$ParaType = new int[ParaType.values().length];
        static final /* synthetic */ int[] $SwitchMap$com$android$server$intellicom$networkslice$model$TrafficDescriptors$RouteBindType = new int[TrafficDescriptors.RouteBindType.values().length];

        static {
            try {
                $SwitchMap$com$android$server$intellicom$networkslice$model$NetworkSliceInfo$ParaType[ParaType.NETWORK_REQUEST.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$intellicom$networkslice$model$NetworkSliceInfo$ParaType[ParaType.NETWORK_CALLBACK.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$server$intellicom$networkslice$model$NetworkSliceInfo$ParaType[ParaType.ROUTE_SELECTION_DESCRIPTOR.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$server$intellicom$networkslice$model$TrafficDescriptors$RouteBindType[TrafficDescriptors.RouteBindType.UID_TDS.ordinal()] = 1;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$server$intellicom$networkslice$model$TrafficDescriptors$RouteBindType[TrafficDescriptors.RouteBindType.IP_TDS.ordinal()] = 2;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$server$intellicom$networkslice$model$TrafficDescriptors$RouteBindType[TrafficDescriptors.RouteBindType.UID_IP_TDS.ordinal()] = 3;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$server$intellicom$networkslice$model$TrafficDescriptors$RouteBindType[TrafficDescriptors.RouteBindType.INVALID_TDS.ordinal()] = 4;
            } catch (NoSuchFieldError e7) {
            }
        }
    }

    public boolean isRightNetworkSlice(Object o, ParaType type) {
        try {
            int i = AnonymousClass1.$SwitchMap$com$android$server$intellicom$networkslice$model$NetworkSliceInfo$ParaType[type.ordinal()];
            if (i != 1) {
                if (i != 2) {
                    if (i != 3) {
                        return false;
                    }
                    if (o != null) {
                        return ((RouteSelectionDescriptor) o).equals(getRouteSelectionDescriptor());
                    }
                    if (getRouteSelectionDescriptor() == null) {
                        return true;
                    }
                    return false;
                } else if (o != null) {
                    return ((NetworkSliceCallback) o).equals(getNetworkCallback());
                } else {
                    if (getNetworkCallback() == null) {
                        return true;
                    }
                    return false;
                }
            } else if (o != null) {
                return ((NetworkRequest) o).equals(getNetworkRequest());
            } else {
                if (getNetworkRequest() == null) {
                    return true;
                }
                return false;
            }
        } catch (ClassCastException e) {
            return false;
        }
    }

    public FqdnIps getFqdnIps() {
        return this.mFqdnIps;
    }

    public void mergeFqdnIps(FqdnIps newFqdnIps) {
        this.mFqdnIps.mergeFqdnIps(newFqdnIps);
    }

    public void setFqdnIps(FqdnIps fqdnIps) {
        this.mFqdnIps = fqdnIps;
    }

    public int getNetId() {
        return this.mNetId;
    }

    public void setNetId(int netId) {
        this.mNetId = netId;
    }

    public Set<Integer> getUids() {
        return this.mUids;
    }

    public void setUids(Set<Integer> uids) {
        this.mUids = uids;
    }

    public void cleanUids() {
        this.mUids.clear();
    }

    public int getNetworkCapability() {
        return this.mNetworkCapability;
    }

    public void setNetworkCapability(int networkCapability) {
        this.mNetworkCapability = networkCapability;
    }

    public NetworkSliceCallback getNetworkCallback() {
        return this.mNetworkCallback;
    }

    public void setNetworkCallback(NetworkSliceCallback networkCallback) {
        this.mNetworkCallback = networkCallback;
    }

    public NetworkRequest getNetworkRequest() {
        return this.mNetworkRequest;
    }

    public void setNetworkRequest(NetworkRequest networkRequest) {
        this.mNetworkRequest = networkRequest;
    }

    public RouteSelectionDescriptor getRouteSelectionDescriptor() {
        return this.mRouteSelectionDescriptor;
    }

    public void setRouteSelectionDescriptor(RouteSelectionDescriptor routeSelectionDescriptor) {
        this.mRouteSelectionDescriptor = routeSelectionDescriptor;
    }

    public TrafficDescriptors getTrafficDescriptors() {
        return this.mTrafficDescriptors;
    }

    public void setTrafficDescriptors(TrafficDescriptors trafficDescriptors) {
        this.mTrafficDescriptors = trafficDescriptors;
    }

    public void addMessenger(int requestId, Messenger messenger) {
        this.mMessengers.put(Integer.valueOf(requestId), messenger);
    }

    public void removeMessager(int requestId) {
        this.mMessengers.remove(Integer.valueOf(requestId));
    }

    public Messenger getMessenger(int requestId) {
        return this.mMessengers.get(Integer.valueOf(requestId));
    }

    public Map<Integer, Messenger> getMessengers() {
        return this.mMessengers;
    }

    public void putCreatedNetworkRequest(NetworkRequest networkRequest) {
        this.mNetworkRequests.put(Integer.valueOf(networkRequest.requestId), networkRequest);
    }

    public NetworkRequest getNetworkRequestByRequestId(int requestId) {
        return this.mNetworkRequests.get(Integer.valueOf(requestId));
    }

    public Set<FqdnIps> getWaittingFqdnIps() {
        return this.mWaittingFqdnIps;
    }

    public void setWaittingFqdnIps(Set<FqdnIps> waittingFqdnIps) {
        this.mWaittingFqdnIps = waittingFqdnIps;
    }

    public Set<Integer> getUsedUids() {
        return this.mUsedUids;
    }

    public void cleanWaittingFqdnIps() {
        this.mWaittingFqdnIps.clear();
    }

    public void cleanUsedUids() {
        this.mUsedUids.clear();
    }

    public boolean isMatchAll() {
        TrafficDescriptors trafficDescriptors = this.mTrafficDescriptors;
        if (trafficDescriptors == null) {
            return false;
        }
        return trafficDescriptors.isMatchAll();
    }

    public boolean isIpTriad() {
        TrafficDescriptors trafficDescriptors = this.mTrafficDescriptors;
        if (trafficDescriptors == null) {
            return false;
        }
        return trafficDescriptors.isIpTriad();
    }

    public String toString() {
        return "NetworkSliceInfo{mNetId=" + this.mNetId + ", mUids=" + this.mUids + " + mUsedUids = " + this.mUsedUids + "mNetworkCapability=" + this.mNetworkCapability + ", mNetworkCallback=" + this.mNetworkCallback + ", mNetworkRequest=" + this.mNetworkRequest + ", mRouteSelectionDescriptor=" + this.mRouteSelectionDescriptor + ", mTrafficDescriptors=" + this.mTrafficDescriptors + '}';
    }

    private void log(String msg) {
        Log.i("NetworkSliceInfo", msg);
    }
}
