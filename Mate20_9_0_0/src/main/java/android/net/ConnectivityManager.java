package android.net;

import android.Manifest.permission;
import android.annotation.SystemApi;
import android.app.PendingIntent;
import android.common.HwFrameworkFactory;
import android.content.Context;
import android.net.NetworkRequest.Builder;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.INetworkActivityListener;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseIntArray;
import com.android.internal.R;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.ITelephony.Stub;
import com.android.internal.util.Preconditions;
import huawei.cust.HwCustUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import libcore.net.event.NetworkEventDispatcher;

public class ConnectivityManager {
    @Deprecated
    public static final String ACTION_BACKGROUND_DATA_SETTING_CHANGED = "android.net.conn.BACKGROUND_DATA_SETTING_CHANGED";
    public static final String ACTION_BTT_NETWORK_CONNECTION_CHANGED = "android.intent.action.BlueToothTethering_NETWORK_CONNECTION_CHANGED";
    public static final String ACTION_CAPTIVE_PORTAL_SIGN_IN = "android.net.conn.CAPTIVE_PORTAL";
    public static final String ACTION_CAPTIVE_PORTAL_TEST_COMPLETED = "android.net.conn.CAPTIVE_PORTAL_TEST_COMPLETED";
    public static final String ACTION_DATA_ACTIVITY_CHANGE = "android.net.conn.DATA_ACTIVITY_CHANGE";
    public static final String ACTION_LTEDATA_COMPLETED_ACTION = "android.net.wifi.LTEDATA_COMPLETED_ACTION";
    public static final String ACTION_PROMPT_LOST_VALIDATION = "android.net.conn.PROMPT_LOST_VALIDATION";
    public static final String ACTION_PROMPT_UNVALIDATED = "android.net.conn.PROMPT_UNVALIDATED";
    public static final String ACTION_RESTRICT_BACKGROUND_CHANGED = "android.net.conn.RESTRICT_BACKGROUND_CHANGED";
    public static final String ACTION_TETHER_STATE_CHANGED = "android.net.conn.TETHER_STATE_CHANGED";
    private static final NetworkRequest ALREADY_UNREGISTERED = new Builder().clearCapabilities().build();
    private static final int BASE = 524288;
    public static final int CALLBACK_AVAILABLE = 524290;
    public static final int CALLBACK_CAP_CHANGED = 524294;
    public static final int CALLBACK_IP_CHANGED = 524295;
    public static final int CALLBACK_LOSING = 524291;
    public static final int CALLBACK_LOST = 524292;
    public static final int CALLBACK_PRECHECK = 524289;
    public static final int CALLBACK_RESUMED = 524298;
    public static final int CALLBACK_SUSPENDED = 524297;
    public static final int CALLBACK_UNAVAIL = 524293;
    @Deprecated
    public static final String CONNECTIVITY_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    public static final String CONNECTIVITY_ACTION_SUPL = "android.net.conn.CONNECTIVITY_CHANGE_SUPL";
    @Deprecated
    public static final int DEFAULT_NETWORK_PREFERENCE = 1;
    private static final int EXPIRE_LEGACY_REQUEST = 524296;
    public static final String EXTRA_ACTIVE_LOCAL_ONLY = "localOnlyArray";
    public static final String EXTRA_ACTIVE_TETHER = "tetherArray";
    public static final String EXTRA_ADD_TETHER_TYPE = "extraAddTetherType";
    public static final String EXTRA_AVAILABLE_TETHER = "availableArray";
    public static final String EXTRA_BTT_CONNECT_STATE = "btt_connect_state";
    public static final String EXTRA_CAPTIVE_PORTAL = "android.net.extra.CAPTIVE_PORTAL";
    public static final String EXTRA_CAPTIVE_PORTAL_PROBE_SPEC = "android.net.extra.CAPTIVE_PORTAL_PROBE_SPEC";
    public static final String EXTRA_CAPTIVE_PORTAL_URL = "android.net.extra.CAPTIVE_PORTAL_URL";
    public static final String EXTRA_CAPTIVE_PORTAL_USER_AGENT = "android.net.extra.CAPTIVE_PORTAL_USER_AGENT";
    public static final String EXTRA_DEVICE_TYPE = "deviceType";
    public static final String EXTRA_ERRORED_TETHER = "erroredArray";
    public static final String EXTRA_EXTRA_INFO = "extraInfo";
    public static final String EXTRA_INET_CONDITION = "inetCondition";
    public static final String EXTRA_IS_ACTIVE = "isActive";
    public static final String EXTRA_IS_CAPTIVE_PORTAL = "captivePortal";
    public static final String EXTRA_IS_FAILOVER = "isFailover";
    public static final String EXTRA_IS_LTE_MOBILE_DATA_STATUS = "lte_mobile_data_status";
    public static final String EXTRA_NETWORK = "android.net.extra.NETWORK";
    @Deprecated
    public static final String EXTRA_NETWORK_INFO = "networkInfo";
    public static final String EXTRA_NETWORK_REQUEST = "android.net.extra.NETWORK_REQUEST";
    public static final String EXTRA_NETWORK_TYPE = "networkType";
    public static final String EXTRA_NO_CONNECTIVITY = "noConnectivity";
    public static final String EXTRA_OTHER_NETWORK_INFO = "otherNetwork";
    public static final String EXTRA_PROVISION_CALLBACK = "extraProvisionCallback";
    public static final String EXTRA_REALTIME_NS = "tsNanos";
    public static final String EXTRA_REASON = "reason";
    public static final String EXTRA_REM_TETHER_TYPE = "extraRemTetherType";
    public static final String EXTRA_RUN_PROVISION = "extraRunProvision";
    public static final String EXTRA_SET_ALARM = "extraSetAlarm";
    public static final String INET_CONDITION_ACTION = "android.net.conn.INET_CONDITION_ACTION";
    private static final int LISTEN = 1;
    public static final int LTE_STATE_CONNECTED = 1;
    public static final int LTE_STATE_CONNECTTING = 0;
    public static final int LTE_STATE_DISCONNECTED = 3;
    public static final int LTE_STATE_DISCONNECTTING = 2;
    public static final int MAX_NETWORK_TYPE = 47;
    public static final int MAX_RADIO_TYPE = 17;
    private static final int MIN_NETWORK_TYPE = 0;
    public static final int MULTIPATH_PREFERENCE_HANDOVER = 1;
    public static final int MULTIPATH_PREFERENCE_PERFORMANCE = 4;
    public static final int MULTIPATH_PREFERENCE_RELIABILITY = 2;
    public static final int MULTIPATH_PREFERENCE_UNMETERED = 7;
    public static final int NETID_UNSET = 0;
    public static final String PRIVATE_DNS_DEFAULT_MODE_FALLBACK = "off";
    public static final String PRIVATE_DNS_MODE_OFF = "off";
    public static final String PRIVATE_DNS_MODE_OPPORTUNISTIC = "opportunistic";
    public static final String PRIVATE_DNS_MODE_PROVIDER_HOSTNAME = "hostname";
    private static final int REQUEST = 2;
    public static final int REQUEST_ID_UNSET = 0;
    public static final int RESTRICT_BACKGROUND_STATUS_DISABLED = 1;
    public static final int RESTRICT_BACKGROUND_STATUS_ENABLED = 3;
    public static final int RESTRICT_BACKGROUND_STATUS_WHITELISTED = 2;
    private static final String TAG = "ConnectivityManager";
    @SystemApi
    public static final int TETHERING_BLUETOOTH = 2;
    public static final int TETHERING_INVALID = -1;
    public static final int TETHERING_P2P = 3;
    @SystemApi
    public static final int TETHERING_USB = 1;
    @SystemApi
    public static final int TETHERING_WIFI = 0;
    public static final int TETHER_ERROR_DISABLE_NAT_ERROR = 9;
    public static final int TETHER_ERROR_ENABLE_NAT_ERROR = 8;
    public static final int TETHER_ERROR_IFACE_CFG_ERROR = 10;
    public static final int TETHER_ERROR_MASTER_ERROR = 5;
    public static final int TETHER_ERROR_NO_ERROR = 0;
    public static final int TETHER_ERROR_PROVISION_FAILED = 11;
    public static final int TETHER_ERROR_SERVICE_UNAVAIL = 2;
    public static final int TETHER_ERROR_TETHER_IFACE_ERROR = 6;
    public static final int TETHER_ERROR_UNAVAIL_IFACE = 4;
    public static final int TETHER_ERROR_UNKNOWN_IFACE = 1;
    public static final int TETHER_ERROR_UNSUPPORTED = 3;
    public static final int TETHER_ERROR_UNTETHER_IFACE_ERROR = 7;
    @Deprecated
    public static final int TYPE_BLUETOOTH = 7;
    @Deprecated
    public static final int TYPE_DUMMY = 8;
    @Deprecated
    public static final int TYPE_ETHERNET = 9;
    @Deprecated
    public static final int TYPE_MOBILE = 0;
    public static final int TYPE_MOBILE_BIP0 = 38;
    public static final int TYPE_MOBILE_BIP1 = 39;
    public static final int TYPE_MOBILE_BIP2 = 40;
    public static final int TYPE_MOBILE_BIP3 = 41;
    public static final int TYPE_MOBILE_BIP4 = 42;
    public static final int TYPE_MOBILE_BIP5 = 43;
    public static final int TYPE_MOBILE_BIP6 = 44;
    @Deprecated
    public static final int TYPE_MOBILE_CBS = 12;
    public static final int TYPE_MOBILE_CMMAIL = 37;
    public static final int TYPE_MOBILE_DM = 34;
    @Deprecated
    public static final int TYPE_MOBILE_DUN = 4;
    @Deprecated
    public static final int TYPE_MOBILE_EMERGENCY = 15;
    @Deprecated
    public static final int TYPE_MOBILE_FOTA = 10;
    @Deprecated
    public static final int TYPE_MOBILE_HIPRI = 5;
    @Deprecated
    public static final int TYPE_MOBILE_IA = 14;
    @Deprecated
    public static final int TYPE_MOBILE_IMS = 11;
    public static final int TYPE_MOBILE_INTERNAL_DEFAULT = 48;
    @Deprecated
    public static final int TYPE_MOBILE_MMS = 2;
    public static final int TYPE_MOBILE_NET = 36;
    @Deprecated
    public static final int TYPE_MOBILE_SUPL = 3;
    public static final int TYPE_MOBILE_WAP = 35;
    public static final int TYPE_MOBILE_XCAP = 45;
    public static final int TYPE_NONE = -1;
    @Deprecated
    public static final int TYPE_PROXY = 16;
    @Deprecated
    public static final int TYPE_VPN = 17;
    @Deprecated
    public static final int TYPE_WIFI = 1;
    public static final int TYPE_WIFI_MMS = 46;
    @Deprecated
    public static final int TYPE_WIFI_P2P = 13;
    public static final int TYPE_WIFI_XCAP = 47;
    @Deprecated
    public static final int TYPE_WIMAX = 6;
    private static int lastLegacyRequestId = 0;
    private static CallbackHandler sCallbackHandler;
    private static final HashMap<NetworkRequest, NetworkCallback> sCallbacks = new HashMap();
    private static ConnectivityManager sInstance;
    private static HashMap<NetworkCapabilities, LegacyRequest> sLegacyRequests = new HashMap();
    private static final SparseIntArray sLegacyTypeToCapability = new SparseIntArray();
    private static final SparseIntArray sLegacyTypeToTransport = new SparseIntArray();
    private final Context mContext;
    HwCustConnectivityManager mCust = ((HwCustConnectivityManager) HwCustUtils.createObj(HwCustConnectivityManager.class, new Object[0]));
    private INetworkManagementService mNMService;
    private INetworkPolicyManager mNPManager;
    private final ArrayMap<OnNetworkActiveListener, INetworkActivityListener> mNetworkActivityListeners = new ArrayMap();
    private final IConnectivityManager mService;

    public interface Errors {
        public static final int TOO_MANY_REQUESTS = 1;
    }

    private static class LegacyRequest {
        Network currentNetwork;
        int delay;
        int expireSequenceNumber;
        NetworkCallback networkCallback;
        NetworkCapabilities networkCapabilities;
        NetworkRequest networkRequest;

        private LegacyRequest() {
            this.delay = -1;
            this.networkCallback = new NetworkCallback() {
                public void onAvailable(Network network) {
                    LegacyRequest.this.currentNetwork = network;
                    String str = ConnectivityManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("startUsingNetworkFeature got Network:");
                    stringBuilder.append(network);
                    Log.d(str, stringBuilder.toString());
                    ConnectivityManager.setProcessDefaultNetworkForHostResolution(network);
                }

                public void onLost(Network network) {
                    if (network.equals(LegacyRequest.this.currentNetwork)) {
                        LegacyRequest.this.clearDnsBinding();
                    }
                    String str = ConnectivityManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("startUsingNetworkFeature lost Network:");
                    stringBuilder.append(network);
                    Log.d(str, stringBuilder.toString());
                }
            };
        }

        /* synthetic */ LegacyRequest(AnonymousClass1 x0) {
            this();
        }

        private void clearDnsBinding() {
            if (this.currentNetwork != null) {
                this.currentNetwork = null;
                ConnectivityManager.setProcessDefaultNetworkForHostResolution(null);
            }
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface MultipathPreference {
    }

    public static class NetworkCallback {
        private NetworkRequest networkRequest;

        public void onPreCheck(Network network) {
        }

        public void onAvailable(Network network, NetworkCapabilities networkCapabilities, LinkProperties linkProperties) {
            onAvailable(network);
            if (!networkCapabilities.hasCapability(21)) {
                onNetworkSuspended(network);
            }
            onCapabilitiesChanged(network, networkCapabilities);
            onLinkPropertiesChanged(network, linkProperties);
        }

        public void onAvailable(Network network) {
        }

        public void onLosing(Network network, int maxMsToLive) {
        }

        public void onLost(Network network) {
        }

        public void onUnavailable() {
        }

        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
        }

        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
        }

        public void onNetworkSuspended(Network network) {
        }

        public void onNetworkResumed(Network network) {
        }
    }

    public interface OnNetworkActiveListener {
        void onNetworkActive();
    }

    @SystemApi
    public static abstract class OnStartTetheringCallback {
        public void onTetheringStarted() {
        }

        public void onTetheringFailed() {
        }
    }

    public class PacketKeepalive {
        public static final int BINDER_DIED = -10;
        public static final int ERROR_HARDWARE_ERROR = -31;
        public static final int ERROR_HARDWARE_UNSUPPORTED = -30;
        public static final int ERROR_INVALID_INTERVAL = -24;
        public static final int ERROR_INVALID_IP_ADDRESS = -21;
        public static final int ERROR_INVALID_LENGTH = -23;
        public static final int ERROR_INVALID_NETWORK = -20;
        public static final int ERROR_INVALID_PORT = -22;
        public static final int MIN_INTERVAL = 10;
        public static final int NATT_PORT = 4500;
        public static final int NO_KEEPALIVE = -1;
        public static final int SUCCESS = 0;
        private static final String TAG = "PacketKeepalive";
        private final PacketKeepaliveCallback mCallback;
        private final Looper mLooper;
        private final Messenger mMessenger;
        private final Network mNetwork;
        private volatile Integer mSlot;

        /* synthetic */ PacketKeepalive(ConnectivityManager x0, Network x1, PacketKeepaliveCallback x2, AnonymousClass1 x3) {
            this(x1, x2);
        }

        void stopLooper() {
            this.mLooper.quit();
        }

        public void stop() {
            try {
                ConnectivityManager.this.mService.stopKeepalive(this.mNetwork, this.mSlot.intValue());
            } catch (RemoteException e) {
                Log.e(TAG, "Error stopping packet keepalive: ", e);
                stopLooper();
            }
        }

        private PacketKeepalive(Network network, PacketKeepaliveCallback callback) {
            Preconditions.checkNotNull(network, "network cannot be null");
            Preconditions.checkNotNull(callback, "callback cannot be null");
            this.mNetwork = network;
            this.mCallback = callback;
            HandlerThread thread = new HandlerThread(TAG);
            thread.start();
            this.mLooper = thread.getLooper();
            this.mMessenger = new Messenger(new Handler(this.mLooper, ConnectivityManager.this) {
                public void handleMessage(Message message) {
                    if (message.what != NetworkAgent.EVENT_PACKET_KEEPALIVE) {
                        String str = PacketKeepalive.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unhandled message ");
                        stringBuilder.append(Integer.toHexString(message.what));
                        Log.e(str, stringBuilder.toString());
                        return;
                    }
                    int error = message.arg2;
                    if (error == 0) {
                        try {
                            if (PacketKeepalive.this.mSlot == null) {
                                PacketKeepalive.this.mSlot = Integer.valueOf(message.arg1);
                                PacketKeepalive.this.mCallback.onStarted();
                                return;
                            }
                            PacketKeepalive.this.mSlot = null;
                            PacketKeepalive.this.stopLooper();
                            PacketKeepalive.this.mCallback.onStopped();
                            return;
                        } catch (Exception e) {
                            String str2 = PacketKeepalive.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Exception in keepalive callback(");
                            stringBuilder2.append(error);
                            stringBuilder2.append(")");
                            Log.e(str2, stringBuilder2.toString(), e);
                            return;
                        }
                    }
                    PacketKeepalive.this.stopLooper();
                    PacketKeepalive.this.mCallback.onError(error);
                }
            });
        }
    }

    public static class PacketKeepaliveCallback {
        public void onStarted() {
        }

        public void onStopped() {
        }

        public void onError(int error) {
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface RestrictBackgroundStatus {
    }

    public static class TooManyRequestsException extends RuntimeException {
    }

    private class CallbackHandler extends Handler {
        private static final boolean DBG = false;
        private static final String TAG = "ConnectivityManager.CallbackHandler";

        CallbackHandler(Looper looper) {
            super(looper);
        }

        CallbackHandler(ConnectivityManager connectivityManager, Handler handler) {
            this(((Handler) Preconditions.checkNotNull(handler, "Handler cannot be null.")).getLooper());
        }

        public void handleMessage(Message message) {
            if (message.what == ConnectivityManager.EXPIRE_LEGACY_REQUEST) {
                ConnectivityManager.this.expireRequest((NetworkCapabilities) message.obj, message.arg1);
                return;
            }
            NetworkCallback callback;
            NetworkRequest request = (NetworkRequest) getObject(message, NetworkRequest.class);
            Network network = (Network) getObject(message, Network.class);
            synchronized (ConnectivityManager.sCallbacks) {
                callback = (NetworkCallback) ConnectivityManager.sCallbacks.get(request);
            }
            if (callback == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("callback not found for ");
                stringBuilder.append(ConnectivityManager.getCallbackName(message.what));
                stringBuilder.append(" message");
                Log.w(str, stringBuilder.toString());
                return;
            }
            switch (message.what) {
                case ConnectivityManager.CALLBACK_PRECHECK /*524289*/:
                    callback.onPreCheck(network);
                    break;
                case ConnectivityManager.CALLBACK_AVAILABLE /*524290*/:
                    callback.onAvailable(network, (NetworkCapabilities) getObject(message, NetworkCapabilities.class), (LinkProperties) getObject(message, LinkProperties.class));
                    break;
                case ConnectivityManager.CALLBACK_LOSING /*524291*/:
                    callback.onLosing(network, message.arg1);
                    break;
                case ConnectivityManager.CALLBACK_LOST /*524292*/:
                    callback.onLost(network);
                    break;
                case ConnectivityManager.CALLBACK_UNAVAIL /*524293*/:
                    callback.onUnavailable();
                    break;
                case ConnectivityManager.CALLBACK_CAP_CHANGED /*524294*/:
                    callback.onCapabilitiesChanged(network, (NetworkCapabilities) getObject(message, NetworkCapabilities.class));
                    break;
                case ConnectivityManager.CALLBACK_IP_CHANGED /*524295*/:
                    callback.onLinkPropertiesChanged(network, (LinkProperties) getObject(message, LinkProperties.class));
                    break;
                case ConnectivityManager.CALLBACK_SUSPENDED /*524297*/:
                    callback.onNetworkSuspended(network);
                    break;
                case ConnectivityManager.CALLBACK_RESUMED /*524298*/:
                    callback.onNetworkResumed(network);
                    break;
            }
        }

        private <T> T getObject(Message msg, Class<T> c) {
            return msg.getData().getParcelable(c.getSimpleName());
        }
    }

    static {
        sLegacyTypeToTransport.put(0, 0);
        sLegacyTypeToTransport.put(12, 0);
        sLegacyTypeToTransport.put(4, 0);
        sLegacyTypeToTransport.put(10, 0);
        sLegacyTypeToTransport.put(5, 0);
        sLegacyTypeToTransport.put(11, 0);
        sLegacyTypeToTransport.put(2, 0);
        sLegacyTypeToTransport.put(3, 0);
        sLegacyTypeToTransport.put(38, 0);
        sLegacyTypeToTransport.put(39, 0);
        sLegacyTypeToTransport.put(40, 0);
        sLegacyTypeToTransport.put(41, 0);
        sLegacyTypeToTransport.put(42, 0);
        sLegacyTypeToTransport.put(43, 0);
        sLegacyTypeToTransport.put(44, 0);
        sLegacyTypeToTransport.put(45, 0);
        sLegacyTypeToTransport.put(1, 1);
        sLegacyTypeToTransport.put(13, 1);
        sLegacyTypeToTransport.put(46, 1);
        sLegacyTypeToTransport.put(47, 1);
        sLegacyTypeToTransport.put(7, 2);
        sLegacyTypeToTransport.put(9, 3);
        sLegacyTypeToTransport.put(48, 30);
        sLegacyTypeToCapability.put(12, 5);
        sLegacyTypeToCapability.put(4, 2);
        sLegacyTypeToCapability.put(10, 3);
        sLegacyTypeToCapability.put(11, 4);
        sLegacyTypeToCapability.put(2, 0);
        sLegacyTypeToCapability.put(3, 1);
        sLegacyTypeToCapability.put(13, 6);
        sLegacyTypeToCapability.put(38, 23);
        sLegacyTypeToCapability.put(39, 24);
        sLegacyTypeToCapability.put(40, 25);
        sLegacyTypeToCapability.put(41, 26);
        sLegacyTypeToCapability.put(42, 27);
        sLegacyTypeToCapability.put(43, 28);
        sLegacyTypeToCapability.put(44, 29);
        sLegacyTypeToCapability.put(45, 9);
        sLegacyTypeToCapability.put(46, 0);
        sLegacyTypeToCapability.put(47, 9);
        sLegacyTypeToCapability.put(48, 30);
    }

    private static int getNewLastLegacyRequestId() {
        if (lastLegacyRequestId == Integer.MAX_VALUE) {
            lastLegacyRequestId = 0;
        }
        int i = lastLegacyRequestId;
        lastLegacyRequestId = i + 1;
        return i;
    }

    @Deprecated
    public static boolean isNetworkTypeValid(int networkType) {
        return (networkType >= 0 && networkType <= 17) || ((networkType >= 34 && networkType <= 45) || ((networkType >= 46 && networkType <= 47) || networkType == 48));
    }

    @Deprecated
    public static String getNetworkTypeName(int type) {
        switch (type) {
            case -1:
                return "NONE";
            case 0:
                return "MOBILE";
            case 1:
                return "WIFI";
            case 2:
                return "MOBILE_MMS";
            case 3:
                return "MOBILE_SUPL";
            case 4:
                return "MOBILE_DUN";
            case 5:
                return "MOBILE_HIPRI";
            case 6:
                return "WIMAX";
            case 7:
                return "BLUETOOTH";
            case 8:
                return "DUMMY";
            case 9:
                return "ETHERNET";
            case 10:
                return "MOBILE_FOTA";
            case 11:
                return "MOBILE_IMS";
            case 12:
                return "MOBILE_CBS";
            case 13:
                return "WIFI_P2P";
            case 14:
                return "MOBILE_IA";
            case 15:
                return "MOBILE_EMERGENCY";
            case 16:
                return "PROXY";
            case 17:
                return "VPN";
            default:
                switch (type) {
                    case 38:
                        return "MOBILE_BIP0";
                    case 39:
                        return "MOBILE_BIP1";
                    case 40:
                        return "MOBILE_BIP2";
                    case 41:
                        return "MOBILE_BIP3";
                    case 42:
                        return "MOBILE_BIP4";
                    case 43:
                        return "MOBILE_BIP5";
                    case 44:
                        return "MOBILE_BIP6";
                    case 45:
                        return "MOBILE_XCAP";
                    case 46:
                        return "WIFI_MMS";
                    case 47:
                        return "WIFI_XCAP";
                    case 48:
                        return "MOBILE_INTERNAL_DEFAULT";
                    default:
                        return Integer.toString(type);
                }
        }
    }

    @Deprecated
    public static boolean isNetworkTypeMobile(int networkType) {
        if (!(networkType == 0 || networkType == 48)) {
            switch (networkType) {
                case 2:
                case 3:
                case 4:
                case 5:
                    break;
                default:
                    switch (networkType) {
                        case 10:
                        case 11:
                        case 12:
                            break;
                        default:
                            switch (networkType) {
                                case 14:
                                case 15:
                                    break;
                                default:
                                    switch (networkType) {
                                        case 38:
                                        case 39:
                                        case 40:
                                        case 41:
                                        case 42:
                                        case 43:
                                        case 44:
                                        case 45:
                                            break;
                                        default:
                                            return false;
                                    }
                            }
                    }
            }
        }
        return true;
    }

    @Deprecated
    public static boolean isNetworkTypeWifi(int networkType) {
        if (!(networkType == 1 || networkType == 13)) {
            switch (networkType) {
                case 46:
                case 47:
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    @Deprecated
    public void setNetworkPreference(int preference) {
    }

    @Deprecated
    public int getNetworkPreference() {
        return -1;
    }

    public NetworkInfo getActiveNetworkInfo() {
        try {
            return this.mService.getActiveNetworkInfo();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Network getActiveNetwork() {
        try {
            return this.mService.getActiveNetwork();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Network getActiveNetworkForUid(int uid) {
        return getActiveNetworkForUid(uid, false);
    }

    public Network getActiveNetworkForUid(int uid, boolean ignoreBlocked) {
        try {
            return this.mService.getActiveNetworkForUid(uid, ignoreBlocked);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isAlwaysOnVpnPackageSupportedForUser(int userId, String vpnPackage) {
        try {
            return this.mService.isAlwaysOnVpnPackageSupported(userId, vpnPackage);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean setAlwaysOnVpnPackageForUser(int userId, String vpnPackage, boolean lockdownEnabled) {
        try {
            return this.mService.setAlwaysOnVpnPackage(userId, vpnPackage, lockdownEnabled);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getAlwaysOnVpnPackageForUser(int userId) {
        try {
            return this.mService.getAlwaysOnVpnPackage(userId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public NetworkInfo getActiveNetworkInfoForUid(int uid) {
        return getActiveNetworkInfoForUid(uid, false);
    }

    public NetworkInfo getActiveNetworkInfoForUid(int uid, boolean ignoreBlocked) {
        try {
            return this.mService.getActiveNetworkInfoForUid(uid, ignoreBlocked);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public NetworkInfo getNetworkInfo(int networkType) {
        try {
            return this.mService.getNetworkInfo(networkType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public NetworkInfo getNetworkInfo(Network network) {
        return getNetworkInfoForUid(network, Process.myUid(), false);
    }

    public NetworkInfo getNetworkInfoForUid(Network network, int uid, boolean ignoreBlocked) {
        try {
            return this.mService.getNetworkInfoForUid(network, uid, ignoreBlocked);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public NetworkInfo[] getAllNetworkInfo() {
        try {
            return this.mService.getAllNetworkInfo();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public Network getNetworkForType(int networkType) {
        try {
            return this.mService.getNetworkForType(networkType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Network[] getAllNetworks() {
        try {
            return this.mService.getAllNetworks();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public NetworkCapabilities[] getDefaultNetworkCapabilitiesForUser(int userId) {
        try {
            return this.mService.getDefaultNetworkCapabilitiesForUser(userId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public LinkProperties getActiveLinkProperties() {
        try {
            return this.mService.getActiveLinkProperties();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public LinkProperties getLinkProperties(int networkType) {
        try {
            return this.mService.getLinkPropertiesForType(networkType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public LinkProperties getLinkProperties(Network network) {
        try {
            return this.mService.getLinkProperties(network);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public NetworkCapabilities getNetworkCapabilities(Network network) {
        try {
            return this.mService.getNetworkCapabilities(network);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public String getCaptivePortalServerUrl() {
        try {
            return this.mService.getCaptivePortalServerUrl();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* JADX WARNING: Missing block: B:23:0x007d, code skipped:
            if (r2 == null) goto L_0x0097;
     */
    /* JADX WARNING: Missing block: B:24:0x007f, code skipped:
            r1 = TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("starting startUsingNetworkFeature for request ");
            r3.append(r2);
            android.util.Log.d(r1, r3.toString());
     */
    /* JADX WARNING: Missing block: B:25:0x0096, code skipped:
            return 1;
     */
    /* JADX WARNING: Missing block: B:26:0x0097, code skipped:
            android.util.Log.d(TAG, " request Failed");
     */
    /* JADX WARNING: Missing block: B:27:0x009e, code skipped:
            return 3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @Deprecated
    public int startUsingNetworkFeature(int networkType, String feature) {
        checkLegacyRoutingApiAccess();
        NetworkCapabilities netCap = networkCapabilitiesForFeature(networkType, feature);
        if (netCap == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can't satisfy startUsingNetworkFeature for ");
            stringBuilder.append(networkType);
            stringBuilder.append(", ");
            stringBuilder.append(feature);
            Log.d(str, stringBuilder.toString());
            return 3;
        } else if (this.mCust != null && this.mCust.enforceStartUsingNetworkFeaturePermissionFail(this.mContext, legacyTypeForNetworkCapabilities(netCap))) {
            return 3;
        } else {
            HwFrameworkFactory.getHwInnerConnectivityManager().checkHwFeature(feature, netCap, networkType);
            synchronized (sLegacyRequests) {
                LegacyRequest l = (LegacyRequest) sLegacyRequests.get(netCap);
                if (l != null) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("renewing startUsingNetworkFeature request ");
                    stringBuilder2.append(l.networkRequest);
                    Log.d(str2, stringBuilder2.toString());
                    renewRequestLocked(l);
                    if (l.currentNetwork != null) {
                        return 0;
                    }
                    return 1;
                }
                NetworkRequest request = requestNetworkForFeatureLocked(netCap);
            }
        }
    }

    @Deprecated
    public int stopUsingNetworkFeature(int networkType, String feature) {
        checkLegacyRoutingApiAccess();
        NetworkCapabilities netCap = networkCapabilitiesForFeature(networkType, feature);
        String str;
        StringBuilder stringBuilder;
        if (netCap == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Can't satisfy stopUsingNetworkFeature for ");
            stringBuilder.append(networkType);
            stringBuilder.append(", ");
            stringBuilder.append(feature);
            Log.d(str, stringBuilder.toString());
            return -1;
        }
        HwFrameworkFactory.getHwInnerConnectivityManager().checkHwFeature(feature, netCap, networkType);
        if (removeRequestForFeature(netCap)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("stopUsingNetworkFeature for ");
            stringBuilder.append(networkType);
            stringBuilder.append(", ");
            stringBuilder.append(feature);
            Log.d(str, stringBuilder.toString());
        }
        return 1;
    }

    /* JADX WARNING: Missing block: B:47:0x00c7, code skipped:
            if (r2.equals("enableBIP5") != false) goto L_0x00ff;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private NetworkCapabilities networkCapabilitiesForFeature(int networkType, String feature) {
        int i = networkType;
        String feature2 = feature;
        int i2 = 13;
        if (i == 0) {
            String[] result = HwFrameworkFactory.getHwInnerConnectivityManager().getFeature(feature2);
            if (!(result[0] == null || "".equals(result[0]))) {
                feature2 = result[0];
            }
            int hashCode = feature2.hashCode();
            switch (hashCode) {
                case 1892665450:
                    if (feature2.equals("enableBIP0")) {
                        i2 = 8;
                        break;
                    }
                case 1892665451:
                    if (feature2.equals("enableBIP1")) {
                        i2 = 9;
                        break;
                    }
                case 1892665452:
                    if (feature2.equals("enableBIP2")) {
                        i2 = 10;
                        break;
                    }
                case 1892665453:
                    if (feature2.equals("enableBIP3")) {
                        i2 = 11;
                        break;
                    }
                case 1892665454:
                    if (feature2.equals("enableBIP4")) {
                        i2 = 12;
                        break;
                    }
                case 1892665455:
                    break;
                case 1892665456:
                    if (feature2.equals("enableBIP6")) {
                        i2 = 14;
                        break;
                    }
                default:
                    switch (hashCode) {
                        case -2106358591:
                            if (feature2.equals("enableInternalDefault")) {
                                i2 = 16;
                                break;
                            }
                        case -1451370941:
                            if (feature2.equals("enableHIPRI")) {
                                i2 = 4;
                                break;
                            }
                        case -631682191:
                            if (feature2.equals("enableCBS")) {
                                i2 = 0;
                                break;
                            }
                        case -631680646:
                            if (feature2.equals("enableDUN")) {
                                i2 = 1;
                                break;
                            }
                        case -631676084:
                            if (feature2.equals("enableIMS")) {
                                i2 = 5;
                                break;
                            }
                        case -631672240:
                            if (feature2.equals("enableMMS")) {
                                i2 = 6;
                                break;
                            }
                        case -595311794:
                            if (feature2.equals("enableEmergency")) {
                                i2 = 17;
                                break;
                            }
                        case 1892790521:
                            if (feature2.equals("enableFOTA")) {
                                i2 = 3;
                                break;
                            }
                        case 1893183457:
                            if (feature2.equals("enableSUPL")) {
                                i2 = 7;
                                break;
                            }
                        case 1893314653:
                            if (feature2.equals("enableXCAP")) {
                                i2 = 15;
                                break;
                            }
                        case 1998933033:
                            if (feature2.equals("enableDUNAlways")) {
                                i2 = 2;
                                break;
                            }
                    }
                    i2 = -1;
                    break;
            }
            switch (i2) {
                case 0:
                    return networkCapabilitiesForType(12);
                case 1:
                case 2:
                    return networkCapabilitiesForType(4);
                case 3:
                    return networkCapabilitiesForType(10);
                case 4:
                    return networkCapabilitiesForType(5);
                case 5:
                    return networkCapabilitiesForType(11);
                case 6:
                    return networkCapabilitiesForType(2);
                case 7:
                    return networkCapabilitiesForType(3);
                case 8:
                    return networkCapabilitiesForType(38);
                case 9:
                    return networkCapabilitiesForType(39);
                case 10:
                    return networkCapabilitiesForType(40);
                case 11:
                    return networkCapabilitiesForType(41);
                case 12:
                    return networkCapabilitiesForType(42);
                case 13:
                    return networkCapabilitiesForType(43);
                case 14:
                    return networkCapabilitiesForType(44);
                case 15:
                    return networkCapabilitiesForType(45);
                case 16:
                    return networkCapabilitiesForType(48);
                case 17:
                    if (this.mCust != null) {
                        return this.mCust.networkCapabilitiesForEimsType(15);
                    }
                    break;
                default:
                    return null;
            }
        } else if (i == 1 && "p2p".equals(feature2)) {
            return networkCapabilitiesForType(13);
        }
        return null;
    }

    private int inferLegacyTypeForNetworkCapabilities(NetworkCapabilities netCap) {
        if (netCap == null || !netCap.hasTransport(0)) {
            return -1;
        }
        if (!netCap.hasCapability(1) && (this.mCust == null || !this.mCust.canHandleEimsNetworkCapabilities(netCap))) {
            return -1;
        }
        String type = null;
        int result = -1;
        if (netCap.hasCapability(5)) {
            type = "enableCBS";
            result = 12;
        } else if (netCap.hasCapability(4)) {
            type = "enableIMS";
            result = 11;
        } else if (netCap.hasCapability(3)) {
            type = "enableFOTA";
            result = 10;
        } else if (netCap.hasCapability(2)) {
            type = "enableDUN";
            result = 4;
        } else if (netCap.hasCapability(1)) {
            type = "enableSUPL";
            result = 3;
        } else if (netCap.hasCapability(12)) {
            type = "enableHIPRI";
            result = 5;
        } else if (netCap.hasCapability(23)) {
            type = "enableBIP0";
            result = 38;
        } else if (netCap.hasCapability(24)) {
            type = "enableBIP1";
            result = 39;
        } else if (netCap.hasCapability(25)) {
            type = "enableBIP2";
            result = 40;
        } else if (netCap.hasCapability(26)) {
            type = "enableBIP3";
            result = 41;
        } else if (netCap.hasCapability(27)) {
            type = "enableBIP4";
            result = 42;
        } else if (netCap.hasCapability(28)) {
            type = "enableBIP5";
            result = 43;
        } else if (netCap.hasCapability(29)) {
            type = "enableBIP6";
            result = 44;
        } else if (netCap.hasCapability(9)) {
            type = "enableXCAP";
            result = 45;
        } else if (netCap.hasCapability(30)) {
            type = "enableInternalDefault";
            result = 48;
        } else if (this.mCust != null && this.mCust.canHandleEimsNetworkCapabilities(netCap)) {
            type = "enableEmergency";
            result = 15;
            sLegacyTypeToTransport.put(15, 0);
            sLegacyTypeToCapability.put(15, 10);
        }
        if (type != null) {
            NetworkCapabilities testCap = networkCapabilitiesForFeature(0, type);
            if (testCap.equalsNetCapabilities(netCap) && testCap.equalsTransportTypes(netCap)) {
                return result;
            }
            return -1;
        }
        return -1;
    }

    private int legacyTypeForNetworkCapabilities(NetworkCapabilities netCap) {
        if (netCap == null) {
            return -1;
        }
        if (netCap.hasCapability(5)) {
            return 12;
        }
        if (netCap.hasCapability(4)) {
            return 11;
        }
        if (netCap.hasCapability(3)) {
            return 10;
        }
        if (netCap.hasCapability(2)) {
            return 4;
        }
        if (netCap.hasCapability(1)) {
            return 3;
        }
        if (netCap.hasCapability(0)) {
            return 2;
        }
        if (netCap.hasCapability(12)) {
            return 5;
        }
        if (netCap.hasCapability(6)) {
            return 13;
        }
        if (netCap.hasCapability(23)) {
            return 38;
        }
        if (netCap.hasCapability(24)) {
            return 39;
        }
        if (netCap.hasCapability(25)) {
            return 40;
        }
        if (netCap.hasCapability(26)) {
            return 41;
        }
        if (netCap.hasCapability(27)) {
            return 42;
        }
        if (netCap.hasCapability(28)) {
            return 43;
        }
        if (netCap.hasCapability(29)) {
            return 44;
        }
        if (netCap.hasCapability(9)) {
            if (netCap.hasTransport(1)) {
                return 47;
            }
            if (netCap.hasTransport(0)) {
                return 45;
            }
        }
        if (netCap.hasCapability(30)) {
            return 48;
        }
        if (this.mCust == null || !this.mCust.canHandleEimsNetworkCapabilities(netCap)) {
            return -1;
        }
        return 15;
    }

    private NetworkRequest findRequestForFeature(NetworkCapabilities netCap) {
        synchronized (sLegacyRequests) {
            LegacyRequest l = (LegacyRequest) sLegacyRequests.get(netCap);
            if (l != null) {
                NetworkRequest networkRequest = l.networkRequest;
                return networkRequest;
            }
            return null;
        }
    }

    private void renewRequestLocked(LegacyRequest l) {
        l.expireSequenceNumber = getNewLastLegacyRequestId();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("renewing request to seqNum ");
        stringBuilder.append(l.expireSequenceNumber);
        Log.d(str, stringBuilder.toString());
        sendExpireMsgForFeature(l.networkCapabilities, l.expireSequenceNumber, l.delay);
    }

    /* JADX WARNING: Missing block: B:11:0x001b, code skipped:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("expireRequest with ");
            r2.append(r0);
            r2.append(", ");
            r2.append(r6);
            android.util.Log.d(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:12:0x0039, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void expireRequest(NetworkCapabilities netCap, int sequenceNum) {
        synchronized (sLegacyRequests) {
            LegacyRequest l = (LegacyRequest) sLegacyRequests.get(netCap);
            if (l == null) {
                return;
            }
            int ourSeqNum = l.expireSequenceNumber;
            if (l.expireSequenceNumber == sequenceNum) {
                removeRequestForFeature(netCap);
            }
        }
    }

    private NetworkRequest requestNetworkForFeatureLocked(NetworkCapabilities netCap) {
        int type = legacyTypeForNetworkCapabilities(netCap);
        try {
            int delay = this.mService.getRestoreDefaultNetworkDelay(type);
            LegacyRequest l = new LegacyRequest();
            l.networkCapabilities = netCap;
            l.delay = delay;
            l.expireSequenceNumber = getNewLastLegacyRequestId();
            l.networkRequest = sendRequestForNetwork(netCap, l.networkCallback, 0, 2, type, getDefaultHandler());
            if (l.networkRequest == null) {
                return null;
            }
            sLegacyRequests.put(netCap, l);
            sendExpireMsgForFeature(netCap, l.expireSequenceNumber, delay);
            return l.networkRequest;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void sendExpireMsgForFeature(NetworkCapabilities netCap, int seqNum, int delay) {
        if (delay >= 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sending expire msg with seqNum ");
            stringBuilder.append(seqNum);
            stringBuilder.append(" and delay ");
            stringBuilder.append(delay);
            Log.d(str, stringBuilder.toString());
            CallbackHandler handler = getDefaultHandler();
            handler.sendMessageDelayed(handler.obtainMessage(EXPIRE_LEGACY_REQUEST, seqNum, 0, netCap), (long) delay);
        }
    }

    private boolean removeRequestForFeature(NetworkCapabilities netCap) {
        LegacyRequest l;
        synchronized (sLegacyRequests) {
            l = (LegacyRequest) sLegacyRequests.remove(netCap);
        }
        if (l == null) {
            return false;
        }
        unregisterNetworkCallback(l.networkCallback);
        l.clearDnsBinding();
        return true;
    }

    public static NetworkCapabilities networkCapabilitiesForType(int type) {
        NetworkCapabilities nc = new NetworkCapabilities();
        int transport = sLegacyTypeToTransport.get(type, -1);
        boolean z = transport != -1;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unknown legacy type: ");
        stringBuilder.append(type);
        Preconditions.checkArgument(z, stringBuilder.toString());
        nc.addTransportType(transport);
        nc.addCapability(sLegacyTypeToCapability.get(type, 12));
        nc.maybeMarkCapabilitiesRestricted();
        return nc;
    }

    public PacketKeepalive startNattKeepalive(Network network, int intervalSeconds, PacketKeepaliveCallback callback, InetAddress srcAddr, int srcPort, InetAddress dstAddr) {
        Network network2 = network;
        PacketKeepalive k = new PacketKeepalive(this, network2, callback, null);
        try {
            this.mService.startNattKeepalive(network2, intervalSeconds, k.mMessenger, new Binder(), srcAddr.getHostAddress(), srcPort, dstAddr.getHostAddress());
            return k;
        } catch (RemoteException e) {
            Log.e(TAG, "Error starting packet keepalive: ", e);
            k.stopLooper();
            return null;
        }
    }

    @Deprecated
    public boolean requestRouteToHost(int networkType, int hostAddress) {
        return requestRouteToHostAddress(networkType, NetworkUtils.intToInetAddress(hostAddress));
    }

    @Deprecated
    public boolean requestRouteToHostAddress(int networkType, InetAddress hostAddress) {
        checkLegacyRoutingApiAccess();
        try {
            return this.mService.requestRouteToHostAddress(networkType, hostAddress.getAddress());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public boolean getBackgroundDataSetting() {
        return true;
    }

    @Deprecated
    public void setBackgroundDataSetting(boolean allowBackgroundData) {
    }

    @Deprecated
    public NetworkQuotaInfo getActiveNetworkQuotaInfo() {
        try {
            return this.mService.getActiveNetworkQuotaInfo();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public boolean getMobileDataEnabled() {
        IBinder b = ServiceManager.getService(Context.TELEPHONY_SERVICE);
        if (b != null) {
            try {
                ITelephony it = Stub.asInterface(b);
                int subId = SubscriptionManager.getDefaultDataSubscriptionId();
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getMobileDataEnabled()+ subId=");
                stringBuilder.append(subId);
                Log.d(str, stringBuilder.toString());
                boolean retVal = it.isUserDataEnabled(subId);
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getMobileDataEnabled()- subId=");
                stringBuilder2.append(subId);
                stringBuilder2.append(" retVal=");
                stringBuilder2.append(retVal);
                Log.d(str2, stringBuilder2.toString());
                return retVal;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Log.d(TAG, "getMobileDataEnabled()- remote exception retVal=false");
        return false;
    }

    private INetworkManagementService getNetworkManagementService() {
        synchronized (this) {
            if (this.mNMService != null) {
                INetworkManagementService iNetworkManagementService = this.mNMService;
                return iNetworkManagementService;
            }
            this.mNMService = INetworkManagementService.Stub.asInterface(ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));
            INetworkManagementService iNetworkManagementService2 = this.mNMService;
            return iNetworkManagementService2;
        }
    }

    public void addDefaultNetworkActiveListener(final OnNetworkActiveListener l) {
        INetworkActivityListener rl = new INetworkActivityListener.Stub() {
            public void onNetworkActive() throws RemoteException {
                l.onNetworkActive();
            }
        };
        try {
            getNetworkManagementService().registerNetworkActivityListener(rl);
            this.mNetworkActivityListeners.put(l, rl);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeDefaultNetworkActiveListener(OnNetworkActiveListener l) {
        INetworkActivityListener rl = (INetworkActivityListener) this.mNetworkActivityListeners.get(l);
        Preconditions.checkArgument(rl != null, "Listener was not registered.");
        try {
            getNetworkManagementService().unregisterNetworkActivityListener(rl);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isDefaultNetworkActive() {
        try {
            return getNetworkManagementService().isNetworkActive();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ConnectivityManager(Context context, IConnectivityManager service) {
        this.mContext = (Context) Preconditions.checkNotNull(context, "missing context");
        this.mService = (IConnectivityManager) Preconditions.checkNotNull(service, "missing IConnectivityManager");
        sInstance = this;
    }

    public static ConnectivityManager from(Context context) {
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static final void enforceChangePermission(Context context) {
        int uid = Binder.getCallingUid();
        Settings.checkAndNoteChangeNetworkStateOperation(context, uid, Settings.getPackageNameForUid(context, uid), true);
    }

    public static final void enforceTetherChangePermission(Context context, String callingPkg) {
        Preconditions.checkNotNull(context, "Context cannot be null");
        Preconditions.checkNotNull(callingPkg, "callingPkg cannot be null");
        if (context.getResources().getStringArray(R.array.config_mobile_hotspot_provision_app).length == 2) {
            context.enforceCallingOrSelfPermission(permission.TETHER_PRIVILEGED, "ConnectivityService");
        } else {
            Settings.checkAndNoteWriteSettingsOperation(context, Binder.getCallingUid(), callingPkg, true);
        }
    }

    @Deprecated
    static ConnectivityManager getInstanceOrNull() {
        return sInstance;
    }

    @Deprecated
    private static ConnectivityManager getInstance() {
        if (getInstanceOrNull() != null) {
            return getInstanceOrNull();
        }
        throw new IllegalStateException("No ConnectivityManager yet constructed");
    }

    public String[] getTetherableIfaces() {
        try {
            return this.mService.getTetherableIfaces();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String[] getTetheredIfaces() {
        try {
            return this.mService.getTetheredIfaces();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String[] getTetheringErroredIfaces() {
        try {
            return this.mService.getTetheringErroredIfaces();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String[] getTetheredDhcpRanges() {
        try {
            return this.mService.getTetheredDhcpRanges();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int tether(String iface) {
        try {
            String pkgName = this.mContext.getOpPackageName();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("tether caller:");
            stringBuilder.append(pkgName);
            Log.i(str, stringBuilder.toString());
            return this.mService.tether(iface, pkgName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int untether(String iface) {
        try {
            String pkgName = this.mContext.getOpPackageName();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("untether caller:");
            stringBuilder.append(pkgName);
            Log.i(str, stringBuilder.toString());
            return this.mService.untether(iface, pkgName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public boolean isTetheringSupported() {
        try {
            return this.mService.isTetheringSupported(this.mContext.getOpPackageName());
        } catch (SecurityException e) {
            return false;
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void startTethering(int type, boolean showProvisioningUi, OnStartTetheringCallback callback) {
        startTethering(type, showProvisioningUi, callback, null);
    }

    @SystemApi
    public void startTethering(int type, boolean showProvisioningUi, final OnStartTetheringCallback callback, Handler handler) {
        Preconditions.checkNotNull(callback, "OnStartTetheringCallback cannot be null.");
        ResultReceiver wrappedCallback = new ResultReceiver(handler) {
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == 0) {
                    callback.onTetheringStarted();
                } else {
                    callback.onTetheringFailed();
                }
            }
        };
        try {
            String pkgName = this.mContext.getOpPackageName();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startTethering caller:");
            stringBuilder.append(pkgName);
            Log.i(str, stringBuilder.toString());
            this.mService.startTethering(type, wrappedCallback, showProvisioningUi, pkgName);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception trying to start tethering.", e);
            wrappedCallback.send(2, null);
        }
    }

    @SystemApi
    public void stopTethering(int type) {
        try {
            String pkgName = this.mContext.getOpPackageName();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("stopTethering caller:");
            stringBuilder.append(pkgName);
            Log.i(str, stringBuilder.toString());
            this.mService.stopTethering(type, pkgName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String[] getTetherableUsbRegexs() {
        try {
            return this.mService.getTetherableUsbRegexs();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String[] getTetherableWifiRegexs() {
        try {
            return this.mService.getTetherableWifiRegexs();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String[] getTetherableBluetoothRegexs() {
        try {
            return this.mService.getTetherableBluetoothRegexs();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int setUsbTethering(boolean enable) {
        try {
            String pkgName = this.mContext.getOpPackageName();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setUsbTethering caller:");
            stringBuilder.append(pkgName);
            Log.i(str, stringBuilder.toString());
            return this.mService.setUsbTethering(enable, pkgName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getLastTetherError(String iface) {
        try {
            return this.mService.getLastTetherError(iface);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void reportInetCondition(int networkType, int percentage) {
        try {
            this.mService.reportInetCondition(networkType, percentage);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void reportBadNetwork(Network network) {
        try {
            this.mService.reportNetworkConnectivity(network, true);
            this.mService.reportNetworkConnectivity(network, false);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void reportNetworkConnectivity(Network network, boolean hasConnectivity) {
        try {
            this.mService.reportNetworkConnectivity(network, hasConnectivity);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setGlobalProxy(ProxyInfo p) {
        try {
            this.mService.setGlobalProxy(p);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ProxyInfo getGlobalProxy() {
        try {
            return this.mService.getGlobalProxy();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ProxyInfo getProxyForNetwork(Network network) {
        try {
            return this.mService.getProxyForNetwork(network);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ProxyInfo getDefaultProxy() {
        return getProxyForNetwork(getBoundNetworkForProcess());
    }

    @Deprecated
    public boolean isNetworkSupported(int networkType) {
        try {
            return this.mService.isNetworkSupported(networkType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isActiveNetworkMetered() {
        try {
            return this.mService.isActiveNetworkMetered();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean updateLockdownVpn() {
        try {
            return this.mService.updateLockdownVpn();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int checkMobileProvisioning(int suggestedTimeOutMs) {
        try {
            return this.mService.checkMobileProvisioning(suggestedTimeOutMs);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getMobileProvisioningUrl() {
        try {
            return this.mService.getMobileProvisioningUrl();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void setProvisioningNotificationVisible(boolean visible, int networkType, String action) {
        try {
            this.mService.setProvisioningNotificationVisible(visible, networkType, action);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setAirplaneMode(boolean enable) {
        try {
            this.mService.setAirplaneMode(enable);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void registerNetworkFactory(Messenger messenger, String name) {
        try {
            this.mService.registerNetworkFactory(messenger, name);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void unregisterNetworkFactory(Messenger messenger) {
        try {
            this.mService.unregisterNetworkFactory(messenger);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int registerNetworkAgent(Messenger messenger, NetworkInfo ni, LinkProperties lp, NetworkCapabilities nc, int score, NetworkMisc misc) {
        try {
            return this.mService.registerNetworkAgent(messenger, ni, lp, nc, score, misc);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static RuntimeException convertServiceException(ServiceSpecificException e) {
        if (e.errorCode == 1) {
            return new TooManyRequestsException();
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown service error code ");
        stringBuilder.append(e.errorCode);
        Log.w(str, stringBuilder.toString());
        return new RuntimeException(e);
    }

    public static String getCallbackName(int whichCallback) {
        switch (whichCallback) {
            case CALLBACK_PRECHECK /*524289*/:
                return "CALLBACK_PRECHECK";
            case CALLBACK_AVAILABLE /*524290*/:
                return "CALLBACK_AVAILABLE";
            case CALLBACK_LOSING /*524291*/:
                return "CALLBACK_LOSING";
            case CALLBACK_LOST /*524292*/:
                return "CALLBACK_LOST";
            case CALLBACK_UNAVAIL /*524293*/:
                return "CALLBACK_UNAVAIL";
            case CALLBACK_CAP_CHANGED /*524294*/:
                return "CALLBACK_CAP_CHANGED";
            case CALLBACK_IP_CHANGED /*524295*/:
                return "CALLBACK_IP_CHANGED";
            case EXPIRE_LEGACY_REQUEST /*524296*/:
                return "EXPIRE_LEGACY_REQUEST";
            case CALLBACK_SUSPENDED /*524297*/:
                return "CALLBACK_SUSPENDED";
            case CALLBACK_RESUMED /*524298*/:
                return "CALLBACK_RESUMED";
            default:
                return Integer.toString(whichCallback);
        }
    }

    private CallbackHandler getDefaultHandler() {
        CallbackHandler callbackHandler;
        synchronized (sCallbacks) {
            if (sCallbackHandler == null) {
                sCallbackHandler = new CallbackHandler(ConnectivityThread.getInstanceLooper());
            }
            callbackHandler = sCallbackHandler;
        }
        return callbackHandler;
    }

    private NetworkRequest sendRequestForNetwork(NetworkCapabilities need, NetworkCallback callback, int timeoutMs, int action, int legacyType, CallbackHandler handler) {
        Throwable th;
        CallbackHandler callbackHandler;
        RemoteException e;
        ServiceSpecificException e2;
        NetworkCapabilities networkCapabilities = need;
        NetworkCallback networkCallback = callback;
        int i = action;
        checkCallbackNotNull(callback);
        boolean z = i == 2 || networkCapabilities != null;
        Preconditions.checkArgument(z, "null NetworkCapabilities");
        if (this.mCust == null || !this.mCust.isDisableRequestBySIM2(networkCapabilities)) {
            try {
                synchronized (sCallbacks) {
                    try {
                        NetworkRequest request;
                        if (!(callback.networkRequest == null || callback.networkRequest == ALREADY_UNREGISTERED)) {
                            Log.e(TAG, "NetworkCallback was already registered");
                        }
                        Messenger messenger = new Messenger(handler);
                        Binder binder = new Binder();
                        if (i == 1) {
                            request = this.mService.listenForNetwork(networkCapabilities, messenger, binder);
                        } else {
                            String pkgName = this.mContext.getOpPackageName();
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("requestNetwork and the calling app is: ");
                            stringBuilder.append(pkgName);
                            Log.d(str, stringBuilder.toString());
                            request = this.mService.requestNetwork(networkCapabilities, messenger, timeoutMs, binder, legacyType);
                        }
                        if (request != null) {
                            sCallbacks.put(request, networkCallback);
                        }
                        networkCallback.networkRequest = request;
                        return request;
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                }
            } catch (RemoteException e3) {
                e = e3;
                callbackHandler = handler;
                throw e.rethrowFromSystemServer();
            } catch (ServiceSpecificException e4) {
                e2 = e4;
                callbackHandler = handler;
                throw convertServiceException(e2);
            }
        }
        Log.d(TAG, "SIM2 data disable by cust");
        return null;
    }

    public void requestNetwork(NetworkRequest request, NetworkCallback networkCallback, int timeoutMs, int legacyType, Handler handler) {
        sendRequestForNetwork(request.networkCapabilities, networkCallback, timeoutMs, 2, legacyType, new CallbackHandler(this, handler));
    }

    public void requestNetwork(NetworkRequest request, NetworkCallback networkCallback) {
        requestNetwork(request, networkCallback, getDefaultHandler());
    }

    public void requestNetwork(NetworkRequest request, NetworkCallback networkCallback, Handler handler) {
        requestNetwork(request, networkCallback, 0, inferLegacyTypeForNetworkCapabilities(request.networkCapabilities), new CallbackHandler(this, handler));
    }

    public void requestNetwork(NetworkRequest request, NetworkCallback networkCallback, int timeoutMs) {
        checkTimeout(timeoutMs);
        requestNetwork(request, networkCallback, timeoutMs, inferLegacyTypeForNetworkCapabilities(request.networkCapabilities), getDefaultHandler());
    }

    public void requestNetwork(NetworkRequest request, NetworkCallback networkCallback, Handler handler, int timeoutMs) {
        checkTimeout(timeoutMs);
        requestNetwork(request, networkCallback, timeoutMs, inferLegacyTypeForNetworkCapabilities(request.networkCapabilities), new CallbackHandler(this, handler));
    }

    public void requestNetwork(NetworkRequest request, PendingIntent operation) {
        checkPendingIntentNotNull(operation);
        try {
            this.mService.pendingRequestForNetwork(request.networkCapabilities, operation);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e2) {
            throw convertServiceException(e2);
        }
    }

    public void releaseNetworkRequest(PendingIntent operation) {
        checkPendingIntentNotNull(operation);
        try {
            this.mService.releasePendingNetworkRequest(operation);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static void checkPendingIntentNotNull(PendingIntent intent) {
        Preconditions.checkNotNull(intent, "PendingIntent cannot be null.");
    }

    private static void checkCallbackNotNull(NetworkCallback callback) {
        Preconditions.checkNotNull(callback, "null NetworkCallback");
    }

    private static void checkTimeout(int timeoutMs) {
        Preconditions.checkArgumentPositive(timeoutMs, "timeoutMs must be strictly positive.");
    }

    public void registerNetworkCallback(NetworkRequest request, NetworkCallback networkCallback) {
        registerNetworkCallback(request, networkCallback, getDefaultHandler());
    }

    public void registerNetworkCallback(NetworkRequest request, NetworkCallback networkCallback, Handler handler) {
        sendRequestForNetwork(request.networkCapabilities, networkCallback, 0, 1, -1, new CallbackHandler(this, handler));
    }

    public void registerNetworkCallback(NetworkRequest request, PendingIntent operation) {
        checkPendingIntentNotNull(operation);
        try {
            this.mService.pendingListenForNetwork(request.networkCapabilities, operation);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e2) {
            throw convertServiceException(e2);
        }
    }

    public void registerDefaultNetworkCallback(NetworkCallback networkCallback) {
        registerDefaultNetworkCallback(networkCallback, getDefaultHandler());
    }

    public void registerDefaultNetworkCallback(NetworkCallback networkCallback, Handler handler) {
        sendRequestForNetwork(null, networkCallback, 0, 2, -1, new CallbackHandler(this, handler));
    }

    public boolean requestBandwidthUpdate(Network network) {
        try {
            return this.mService.requestBandwidthUpdate(network);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void unregisterNetworkCallback(NetworkCallback networkCallback) {
        checkCallbackNotNull(networkCallback);
        List<NetworkRequest> reqs = new ArrayList();
        synchronized (sCallbacks) {
            boolean z = false;
            Preconditions.checkArgument(networkCallback.networkRequest != null, "NetworkCallback was not registered");
            if (networkCallback.networkRequest != ALREADY_UNREGISTERED) {
                z = true;
            }
            Preconditions.checkArgument(z, "NetworkCallback was already unregistered");
            for (Entry<NetworkRequest, NetworkCallback> e : sCallbacks.entrySet()) {
                if (e.getValue() == networkCallback) {
                    reqs.add((NetworkRequest) e.getKey());
                }
            }
            for (NetworkRequest r : reqs) {
                try {
                    this.mService.releaseNetworkRequest(r);
                    sCallbacks.remove(r);
                } catch (RemoteException e2) {
                    throw e2.rethrowFromSystemServer();
                }
            }
            networkCallback.networkRequest = ALREADY_UNREGISTERED;
        }
    }

    public void unregisterNetworkCallback(PendingIntent operation) {
        checkPendingIntentNotNull(operation);
        releaseNetworkRequest(operation);
    }

    public void setAcceptUnvalidated(Network network, boolean accept, boolean always) {
        try {
            this.mService.setAcceptUnvalidated(network, accept, always);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setAvoidUnvalidated(Network network) {
        try {
            this.mService.setAvoidUnvalidated(network);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void startCaptivePortalApp(Network network) {
        try {
            this.mService.startCaptivePortalApp(network);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getMultipathPreference(Network network) {
        try {
            return this.mService.getMultipathPreference(network);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void factoryReset() {
        try {
            this.mService.factoryReset();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean bindProcessToNetwork(Network network) {
        return setProcessDefaultNetwork(network);
    }

    @Deprecated
    public static boolean setProcessDefaultNetwork(Network network) {
        int netId = network == null ? 0 : network.netId;
        if (netId == NetworkUtils.getBoundNetworkForProcess()) {
            return true;
        }
        if (!NetworkUtils.bindProcessToNetwork(netId)) {
            return false;
        }
        try {
            Proxy.setHttpProxySystemProperty(getInstance().getDefaultProxy());
        } catch (SecurityException e) {
            Log.e(TAG, "Can't set proxy properties", e);
        }
        InetAddress.clearDnsCache();
        NetworkEventDispatcher.getInstance().onNetworkConfigurationChanged();
        return true;
    }

    public Network getBoundNetworkForProcess() {
        return getProcessDefaultNetwork();
    }

    @Deprecated
    public static Network getProcessDefaultNetwork() {
        int netId = NetworkUtils.getBoundNetworkForProcess();
        if (netId == 0) {
            return null;
        }
        return new Network(netId);
    }

    private void unsupportedStartingFrom(int version) {
        if (Process.myUid() != 1000 && this.mContext.getApplicationInfo().targetSdkVersion >= version) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("This method is not supported in target SDK version ");
            stringBuilder.append(version);
            stringBuilder.append(" and above");
            throw new UnsupportedOperationException(stringBuilder.toString());
        }
    }

    private void checkLegacyRoutingApiAccess() {
        String permForOmadm = "com.android.permission.INJECT_OMADM_SETTINGS";
        if (this.mContext.checkCallingOrSelfPermission(permission.USE_LEGACY_INTERFACE) != 0 && this.mContext.checkCallingOrSelfPermission(permForOmadm) != 0) {
            unsupportedStartingFrom(23);
        }
    }

    @Deprecated
    public static boolean setProcessDefaultNetworkForHostResolution(Network network) {
        return NetworkUtils.bindProcessToNetworkForHostResolution(network == null ? 0 : network.netId);
    }

    private INetworkPolicyManager getNetworkPolicyManager() {
        synchronized (this) {
            INetworkPolicyManager iNetworkPolicyManager;
            if (this.mNPManager != null) {
                iNetworkPolicyManager = this.mNPManager;
                return iNetworkPolicyManager;
            }
            this.mNPManager = INetworkPolicyManager.Stub.asInterface(ServiceManager.getService(Context.NETWORK_POLICY_SERVICE));
            iNetworkPolicyManager = this.mNPManager;
            return iNetworkPolicyManager;
        }
    }

    public int getRestrictBackgroundStatus() {
        try {
            return getNetworkPolicyManager().getRestrictBackgroundByCaller();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public byte[] getNetworkWatchlistConfigHash() {
        try {
            return this.mService.getNetworkWatchlistConfigHash();
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get watchlist config hash");
            throw e.rethrowFromSystemServer();
        }
    }

    public void setLteMobileDataEnabled(boolean enable) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[enter]setLteMobileDataEnabled ");
        stringBuilder.append(enable);
        Log.d(str, stringBuilder.toString());
        try {
            this.mService.setLteMobileDataEnabled(enable);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public int checkLteConnectState() {
        Log.d(TAG, "[enter]checkLteConnectState");
        try {
            return this.mService.checkLteConnectState();
        } catch (RemoteException e) {
            e.printStackTrace();
            return 3;
        }
    }

    public long getLteTotalRxBytes() {
        Log.d(TAG, "[enter]getLteTotalRxBytes");
        try {
            return this.mService.getLteTotalRxBytes();
        } catch (RemoteException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public long getLteTotalTxBytes() {
        Log.d(TAG, "[enter]getLteTotalTxBytes");
        try {
            return this.mService.getLteTotalTxBytes();
        } catch (RemoteException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
