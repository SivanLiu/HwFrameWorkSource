package com.android.server;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.wifi.PPPOEConfig;
import android.net.wifi.PPPOEInfo;
import android.net.wifi.PPPOEInfo.Status;
import android.os.INetworkManagementService;
import android.os.INetworkManagementService.Stub;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PPPOEStateMachine extends StateMachine {
    private static final boolean DEBUG = true;
    public static final int MAX_LOG_SIZE = 25;
    private static final String NETWORKTYPE = "WIFI_PPPOE";
    public static final String PHASE_AUTHENTICATE = "5";
    public static final String PHASE_CALLBACK = "6";
    public static final String PHASE_DEAD = "0";
    public static final String PHASE_DISCONNECT = "10";
    public static final String PHASE_DORMANT = "3";
    public static final String PHASE_ESTABLISH = "4";
    public static final String PHASE_HOLDOFF = "11";
    public static final String PHASE_INITIALIZE = "1";
    public static final String PHASE_MASTER = "12";
    public static final String PHASE_NETWORK = "7";
    public static final String PHASE_RUNNING = "8";
    public static final String PHASE_SERIALCONN = "2";
    public static final String PHASE_TERMINATE = "9";
    public static final int PPPOE_EVENT_CODE = 652;
    private static final String PPPOE_TAG = "PPPOEConnector";
    public static final int RESPONSE_QUEUE_SIZE = 10;
    private static final String TAG = "PPPOEStateMachine";
    static final Set<String> stateCode = new HashSet();
    private ConnectivityManager mCm;
    private ConnectedState mConnectedState = new ConnectedState();
    private final AtomicLong mConnectedTimeAtomicLong = new AtomicLong(0);
    private ConnectingState mConnectingState = new ConnectingState();
    private Context mContext;
    private CountDownLatch mCountDownLatch = new CountDownLatch(1);
    private RouteInfo mDefRoute = null;
    private DefaultState mDefaultState = new DefaultState();
    private DisconnectedState mDisconnectedState = new DisconnectedState();
    private DisconnectingState mDisconnectingState = new DisconnectingState();
    private InitialState mInitialState = new InitialState();
    private NativeDaemonConnector mNativeConnetor;
    private final AtomicInteger mPPPOEState = new AtomicInteger(Status.OFFLINE.ordinal());
    private StartFailedState mStartFailedState = new StartFailedState();
    private StopFailedState mStopFailedState = new StopFailedState();
    private WakeLock mWakeLock;

    private class ConnectedState extends State {
        private ConnectedState() {
        }

        public void enter() {
            Log.d(PPPOEStateMachine.TAG, "success to connect pppoe.");
            PPPOEStateMachine.this.mPPPOEState.set(Status.ONLINE.ordinal());
            PPPOEStateMachine.this.mConnectedTimeAtomicLong.set(System.currentTimeMillis());
            Intent completeIntent = new Intent("android.net.wifi.PPPOE_COMPLETED_ACTION");
            completeIntent.addFlags(67108864);
            completeIntent.putExtra("pppoe_result_status", "SUCCESS");
            PPPOEStateMachine.this.mContext.sendStickyBroadcast(completeIntent);
            Intent connectIntent = new Intent("android.net.wifi.PPPOE_STATE_CHANGED");
            connectIntent.addFlags(67108864);
            connectIntent.putExtra("pppoe_state", "PPPOE_STATE_CONNECTED");
            PPPOEStateMachine.this.mContext.sendStickyBroadcast(connectIntent);
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            if (i == 151555) {
                if (PPPOEStateMachine.PHASE_DISCONNECT.equals(NativeDaemonEvent.unescapeArgs(msg.obj)[1])) {
                    Log.w(PPPOEStateMachine.TAG, "lost pppoe service.");
                    PPPOEStateMachine.this.transitionTo(PPPOEStateMachine.this.mDisconnectingState);
                }
            } else if (i != 589826) {
                return false;
            } else {
                PPPOEStateMachine.this.transitionTo(PPPOEStateMachine.this.mDisconnectingState);
            }
            return true;
        }
    }

    private class ConnectingState extends State {
        private ConnectingState() {
        }

        public void enter() {
            final Message message = new Message();
            message.copyFrom(PPPOEStateMachine.this.getCurrentMessage());
            new Thread(new Runnable() {
                public void run() {
                    PPPOEStateMachine.this.mWakeLock.acquire();
                    PPPOEStateMachine.this.mPPPOEState.set(Status.CONNECTING.ordinal());
                    Intent intent = new Intent("android.net.wifi.PPPOE_STATE_CHANGED");
                    intent.addFlags(67108864);
                    intent.putExtra("pppoe_state", "PPPOE_STATE_CONNECTING");
                    PPPOEStateMachine.this.mContext.sendStickyBroadcast(intent);
                    PPPOEConfig serverInfoConfig = message.obj;
                    Log.d(PPPOEStateMachine.TAG, "start PPPOE");
                    if (!PPPOEStateMachine.this.startPPPOE(serverInfoConfig)) {
                        PPPOEStateMachine.this.mStartFailedState.setErrorCode("FAILURE_INTERNAL_ERROR");
                        PPPOEStateMachine.this.transitionTo(PPPOEStateMachine.this.mStartFailedState);
                    }
                    PPPOEStateMachine.this.mWakeLock.release();
                }
            }).start();
        }

        public boolean processMessage(Message msg) {
            if (msg.what != 151555) {
                return false;
            }
            String[] cooked = NativeDaemonEvent.unescapeArgs(msg.obj);
            if (PPPOEStateMachine.PHASE_RUNNING.equals(cooked[1])) {
                if (updateConfig()) {
                    PPPOEStateMachine.this.transitionTo(PPPOEStateMachine.this.mConnectedState);
                    return true;
                }
                PPPOEStateMachine.this.mStartFailedState.setErrorCode("FAILURE_INTERNAL_ERROR");
                PPPOEStateMachine.this.transitionTo(PPPOEStateMachine.this.mStartFailedState);
                return true;
            } else if (PPPOEStateMachine.PHASE_DISCONNECT.equals(cooked[1])) {
                PPPOEStateMachine.this.mStartFailedState.setErrorCode("FAILURE_INTERNAL_ERROR");
                PPPOEStateMachine.this.transitionTo(PPPOEStateMachine.this.mStartFailedState);
                return true;
            } else if (PPPOEStateMachine.stateCode.contains(cooked[1])) {
                return true;
            } else {
                PPPOEStateMachine.this.mStartFailedState.setErrorCode(cooked[1]);
                PPPOEStateMachine.this.transitionTo(PPPOEStateMachine.this.mStartFailedState);
                return true;
            }
        }

        private boolean updateConfig() {
            INetworkManagementService netService = Stub.asInterface(ServiceManager.getService("network_management"));
            ConnectivityManager connectivityManager = (ConnectivityManager) PPPOEStateMachine.this.mContext.getSystemService("connectivity");
            LinkProperties prop = connectivityManager.getActiveLinkProperties();
            Network wifiNetwork = connectivityManager.getNetworkForType(1);
            if (wifiNetwork == null) {
                Log.e(PPPOEStateMachine.TAG, "ConnectingState, updateConfig, No Network for type WIFI!");
                return false;
            }
            String str;
            if (prop != null) {
                for (RouteInfo routeInfo : prop.getRoutes()) {
                    if (routeInfo.isDefaultRoute()) {
                        PPPOEStateMachine.this.mDefRoute = routeInfo;
                        break;
                    }
                }
                if (PPPOEStateMachine.this.mDefRoute != null) {
                    str = PPPOEStateMachine.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Remove default route ");
                    stringBuilder.append(PPPOEStateMachine.this.mDefRoute.toString());
                    Log.d(str, stringBuilder.toString());
                    try {
                        netService.removeRoute(wifiNetwork.netId, PPPOEStateMachine.this.mDefRoute);
                    } catch (Exception e) {
                        str = PPPOEStateMachine.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to remove route ");
                        stringBuilder.append(PPPOEStateMachine.this.mDefRoute);
                        Log.e(str, stringBuilder.toString(), e);
                        return false;
                    }
                }
            }
            str = SystemProperties.get("ppp.interface", "ppp0");
            InetAddress destAddr = NetworkUtils.numericToInetAddress(SystemProperties.get("ppp.InetAddress", "0.0.0.0"));
            RouteInfo route = new RouteInfo(new LinkAddress(destAddr, 0), NetworkUtils.numericToInetAddress(SystemProperties.get("ppp.gateway", "0.0.0.0")), str);
            List<RouteInfo> routes = new ArrayList();
            routes.add(route);
            String str2 = PPPOEStateMachine.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("add route ");
            stringBuilder2.append(route.toString());
            Log.d(str2, stringBuilder2.toString());
            String dns1;
            ConnectivityManager connectivityManager2;
            LinkProperties prop2;
            Network wifiNetwork2;
            try {
                netService.addInterfaceToLocalNetwork(str, routes);
                if (prop != null) {
                    String[] args;
                    Collection<InetAddress> dnses = prop.getDnsServers();
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("net.");
                    stringBuilder2.append(str);
                    stringBuilder2.append(".dns2");
                    String dns2 = SystemProperties.get(stringBuilder2.toString());
                    if (dns2 != null && dns2.trim().length() > 0) {
                        prop.addDnsServer(NetworkUtils.numericToInetAddress(dns2));
                        SystemProperties.set("net.dns2", dns2);
                    }
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("net.");
                    stringBuilder3.append(str);
                    stringBuilder3.append(".dns1");
                    dns1 = SystemProperties.get(stringBuilder3.toString());
                    if (dns1 == null || dns1.trim().length() <= 0) {
                    } else {
                        prop.addDnsServer(NetworkUtils.numericToInetAddress(dns1));
                        SystemProperties.set("net.dns1", dns1);
                    }
                    netService = NetworkUtils.makeStrings(dnses);
                    int length = netService.length;
                    int dnses2 = 0;
                    while (dnses2 < length) {
                        connectivityManager2 = connectivityManager;
                        String arg = netService[dnses2];
                        args = netService;
                        netService = PPPOEStateMachine.TAG;
                        prop2 = prop;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        wifiNetwork2 = wifiNetwork;
                        stringBuilder4.append("after set dns servers: ");
                        stringBuilder4.append(arg);
                        Log.e(netService, stringBuilder4.toString());
                        dnses2++;
                        connectivityManager = connectivityManager2;
                        netService = args;
                        prop = prop2;
                        wifiNetwork = wifiNetwork2;
                    }
                    args = netService;
                    connectivityManager2 = connectivityManager;
                    prop2 = prop;
                    wifiNetwork2 = wifiNetwork;
                    String str3 = PPPOEStateMachine.TAG;
                    netService = new StringBuilder();
                    netService.append("set net.dns1 :");
                    netService.append(dns1);
                    netService.append(" net.dns2: ");
                    netService.append(dns2);
                    Log.d(str3, netService.toString());
                    return true;
                }
                connectivityManager2 = connectivityManager;
                prop2 = prop;
                wifiNetwork2 = wifiNetwork;
                return false;
            } catch (Exception e2) {
                INetworkManagementService iNetworkManagementService = netService;
                connectivityManager2 = connectivityManager;
                prop2 = prop;
                wifiNetwork2 = wifiNetwork;
                Exception exception = e2;
                dns1 = PPPOEStateMachine.TAG;
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append("Failed to add route ");
                stringBuilder5.append(route);
                Log.e(dns1, stringBuilder5.toString(), e2);
                return false;
            }
        }
    }

    private class DefaultState extends State {
        private DefaultState() {
        }

        public boolean processMessage(Message msg) {
            String str;
            StringBuilder stringBuilder;
            switch (msg.what) {
                case 69632:
                    if (msg.arg1 == 0) {
                        Log.d(PPPOEStateMachine.TAG, "New client listening to asynchronous messages");
                        return true;
                    }
                    str = PPPOEStateMachine.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Client connection failure, error=");
                    stringBuilder.append(msg.arg1);
                    Log.e(str, stringBuilder.toString());
                    return true;
                case 69633:
                case 151555:
                    return true;
                case 69636:
                    if (msg.arg1 == 2) {
                        Log.e(PPPOEStateMachine.TAG, "Send failed, client connection lost");
                        return true;
                    }
                    str = PPPOEStateMachine.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Client connection lost with reason: ");
                    stringBuilder.append(msg.arg1);
                    Log.d(str, stringBuilder.toString());
                    return true;
                case 589825:
                    Log.d(PPPOEStateMachine.TAG, "start PPPOE");
                    if (PPPOEStateMachine.this.mPPPOEState.get() == Status.OFFLINE.ordinal()) {
                        PPPOEStateMachine.this.transitionTo(PPPOEStateMachine.this.mConnectingState);
                        return true;
                    }
                    Log.w(PPPOEStateMachine.TAG, "the pppoe is already online.");
                    Intent completeIntent = new Intent("android.net.wifi.PPPOE_COMPLETED_ACTION");
                    completeIntent.addFlags(67108864);
                    completeIntent.putExtra("pppoe_result_status", "ALREADY_ONLINE");
                    PPPOEStateMachine.this.mContext.sendStickyBroadcast(completeIntent);
                    return true;
                case 589826:
                    Log.d(PPPOEStateMachine.TAG, "stop PPPOE");
                    if (PPPOEStateMachine.this.mPPPOEState.get() != Status.ONLINE.ordinal()) {
                        return true;
                    }
                    PPPOEStateMachine.this.transitionTo(PPPOEStateMachine.this.mDisconnectingState);
                    return true;
                default:
                    return false;
            }
        }
    }

    private class DisconnectedState extends State {
        private DisconnectedState() {
        }

        public void enter() {
            updateConfig();
            PPPOEStateMachine.this.mConnectedTimeAtomicLong.set(0);
            PPPOEStateMachine.this.mPPPOEState.set(Status.OFFLINE.ordinal());
            Intent connectIntent = new Intent("android.net.wifi.PPPOE_STATE_CHANGED");
            connectIntent.addFlags(67108864);
            connectIntent.putExtra("pppoe_state", "PPPOE_STATE_DISCONNECTED");
            PPPOEStateMachine.this.mContext.sendStickyBroadcast(connectIntent);
        }

        private boolean updateConfig() {
            INetworkManagementService netService = Stub.asInterface(ServiceManager.getService("network_management"));
            ConnectivityManager connectivityManager = (ConnectivityManager) PPPOEStateMachine.this.mContext.getSystemService("connectivity");
            LinkProperties prop = connectivityManager.getActiveLinkProperties();
            Network wifiNetwork = connectivityManager.getNetworkForType(1);
            if (wifiNetwork == null) {
                Log.e(PPPOEStateMachine.TAG, "DisconnectedState, updateConfig, No Network for type WiFi!");
                return false;
            }
            if (prop != null) {
                Collection<InetAddress> dnses = prop.getDnsServers();
            }
            String interfaceName = SystemProperties.get("wifi.interface", "wlan0");
            if (PPPOEStateMachine.this.mDefRoute == null) {
                return false;
            }
            String str = PPPOEStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Reset default route via %s");
            stringBuilder.append(interfaceName);
            Log.d(str, stringBuilder.toString());
            try {
                netService.addRoute(wifiNetwork.netId, PPPOEStateMachine.this.mDefRoute);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("dhcp.");
                stringBuilder2.append(interfaceName);
                stringBuilder2.append(".dns1");
                String dns1 = SystemProperties.get(stringBuilder2.toString());
                if (dns1 != null && dns1.trim().length() > 0) {
                    InetAddress dns = NetworkUtils.numericToInetAddress(dns1);
                    if (prop != null) {
                        prop.addDnsServer(dns);
                    }
                    SystemProperties.set("net.dns1", dns1);
                }
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("dhcp.");
                stringBuilder3.append(interfaceName);
                stringBuilder3.append(".dns2");
                str = SystemProperties.get(stringBuilder3.toString());
                if (str != null && str.trim().length() > 0) {
                    InetAddress dns2 = NetworkUtils.numericToInetAddress(str);
                    if (prop != null) {
                        prop.addDnsServer(dns2);
                    }
                    SystemProperties.set("net.dns2", str);
                }
                String str2 = PPPOEStateMachine.TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("set net.dns1 :");
                stringBuilder4.append(dns1);
                stringBuilder4.append(" net.dns2: ");
                stringBuilder4.append(str);
                Log.d(str2, stringBuilder4.toString());
                return true;
            } catch (Exception e) {
                str = PPPOEStateMachine.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to add route ");
                stringBuilder.append(PPPOEStateMachine.this.mDefRoute);
                Log.e(str, stringBuilder.toString(), e);
                return false;
            }
        }
    }

    private class DisconnectingState extends State {
        private DisconnectingState() {
        }

        public void enter() {
            new Message().copyFrom(PPPOEStateMachine.this.getCurrentMessage());
            new Thread(new Runnable() {
                public void run() {
                    PPPOEStateMachine.this.mWakeLock.acquire();
                    Intent intent = new Intent("android.net.wifi.PPPOE_STATE_CHANGED");
                    intent.addFlags(67108864);
                    intent.putExtra("pppoe_state", "PPPOE_STATE_DISCONNECTING");
                    PPPOEStateMachine.this.mContext.sendStickyBroadcast(intent);
                    Log.d(PPPOEStateMachine.TAG, "stop PPPOE");
                    if (!PPPOEStateMachine.this.stopPPPOE()) {
                        PPPOEStateMachine.this.mStopFailedState.setErrorCode("FAILURE_INTERNAL_ERROR");
                        PPPOEStateMachine.this.transitionTo(PPPOEStateMachine.this.mStopFailedState);
                    }
                    PPPOEStateMachine.this.mWakeLock.release();
                }
            }).start();
        }

        public boolean processMessage(Message msg) {
            if (msg.what != 151555) {
                return false;
            }
            String[] cooked = NativeDaemonEvent.unescapeArgs(msg.obj);
            if ("0".equals(cooked[1])) {
                PPPOEStateMachine.this.transitionTo(PPPOEStateMachine.this.mDisconnectedState);
                return true;
            } else if (PPPOEStateMachine.stateCode.contains(cooked[1])) {
                return true;
            } else {
                PPPOEStateMachine.this.mStopFailedState.setErrorCode(cooked[1]);
                PPPOEStateMachine.this.transitionTo(PPPOEStateMachine.this.mStopFailedState);
                return true;
            }
        }
    }

    private class InitialState extends State {
        private InitialState() {
        }

        public boolean processMessage(Message msg) {
            if (msg.what != 589825) {
                return false;
            }
            if (PPPOEStateMachine.this.mPPPOEState.get() == Status.OFFLINE.ordinal()) {
                PPPOEStateMachine.this.transitionTo(PPPOEStateMachine.this.mConnectingState);
                return true;
            }
            Intent completeIntent = new Intent("android.net.wifi.PPPOE_COMPLETED_ACTION");
            completeIntent.addFlags(67108864);
            completeIntent.putExtra("pppoe_result_status", "ALREADY_ONLINE");
            PPPOEStateMachine.this.mContext.sendStickyBroadcast(completeIntent);
            return true;
        }
    }

    public class NativeDaemonConnectorCallbacks implements INativeDaemonConnectorCallbacks {
        public void onDaemonConnected() {
            Log.d(PPPOEStateMachine.TAG, "Start native daemon connector success.");
            PPPOEStateMachine.this.mCountDownLatch.countDown();
        }

        public boolean onCheckHoldWakeLock(int code) {
            return false;
        }

        public boolean onEvent(int code, String raw, String[] cooked) {
            if (code == PPPOEStateMachine.PPPOE_EVENT_CODE) {
                String str = PPPOEStateMachine.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onEvent receive native daemon connector event, code=");
                stringBuilder.append(code);
                stringBuilder.append(",raw=");
                stringBuilder.append(raw);
                Log.d(str, stringBuilder.toString());
                PPPOEStateMachine.this.sendMessage(151555, raw);
            }
            return true;
        }
    }

    private class StartFailedState extends State {
        private String errorCode;

        private StartFailedState() {
        }

        public void enter() {
            String str = PPPOEStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to start PPPOE, error code is ");
            stringBuilder.append(this.errorCode);
            Log.e(str, stringBuilder.toString());
            PPPOEStateMachine.this.stopPPPOE();
            PPPOEStateMachine.this.mPPPOEState.set(Status.OFFLINE.ordinal());
            Intent intent = new Intent("android.net.wifi.PPPOE_COMPLETED_ACTION");
            intent.addFlags(67108864);
            intent.putExtra("pppoe_result_status", "FAILURE");
            intent.putExtra("pppoe_result_error_code", this.errorCode);
            PPPOEStateMachine.this.mContext.sendStickyBroadcast(intent);
            PPPOEStateMachine.this.transitionTo(PPPOEStateMachine.this.mInitialState);
        }

        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }
    }

    private class StopFailedState extends State {
        private String errorCode;

        private StopFailedState() {
        }

        public void enter() {
            PPPOEStateMachine.this.mConnectedTimeAtomicLong.set(0);
            PPPOEStateMachine.this.mPPPOEState.set(Status.OFFLINE.ordinal());
            String str = PPPOEStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to stop PPPOE , error code is ");
            stringBuilder.append(this.errorCode);
            Log.e(str, stringBuilder.toString());
        }

        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }
    }

    static {
        stateCode.add("0");
        stateCode.add("1");
        stateCode.add("2");
        stateCode.add("3");
        stateCode.add("4");
        stateCode.add("5");
        stateCode.add("6");
        stateCode.add("7");
        stateCode.add(PHASE_RUNNING);
        stateCode.add(PHASE_TERMINATE);
        stateCode.add(PHASE_DISCONNECT);
        stateCode.add(PHASE_HOLDOFF);
        stateCode.add("12");
    }

    public PPPOEStateMachine(Context context, String name) {
        super(name);
        Log.d(TAG, " create PPPOE state Machine");
        this.mContext = context;
        this.mWakeLock = ((PowerManager) this.mContext.getSystemService("power")).newWakeLock(1, TAG);
        addState(this.mDefaultState);
        addState(this.mInitialState, this.mDefaultState);
        addState(this.mConnectingState, this.mDefaultState);
        addState(this.mConnectedState, this.mDefaultState);
        addState(this.mStartFailedState, this.mDefaultState);
        addState(this.mDisconnectingState, this.mDefaultState);
        addState(this.mDisconnectedState, this.mDefaultState);
        addState(this.mStopFailedState, this.mDefaultState);
        setInitialState(this.mInitialState);
        this.mNativeConnetor = new NativeDaemonConnector(new NativeDaemonConnectorCallbacks(), "netd", 10, PPPOE_TAG, 25, null);
    }

    public void start() {
        new Thread(this.mNativeConnetor, PPPOE_TAG).start();
        try {
            this.mCountDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.start();
    }

    private void checkAndSetConnectivityInstance() {
        if (this.mCm == null) {
            this.mCm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
    }

    private boolean stopPPPOE() {
        Log.d(TAG, "execute stop PPPOE");
        try {
            this.mNativeConnetor.execute("pppoed", new Object[]{"stop"});
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop PPPOE.", e);
            return false;
        }
    }

    private boolean startPPPOE(PPPOEConfig serverConfig) {
        Log.d(TAG, "startPPPOE");
        try {
            this.mNativeConnetor.execute("pppoed", buildStartArgs(serverConfig));
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to start PPPOE", e);
            return false;
        }
    }

    public PPPOEInfo getPPPOEInfo() {
        Log.d(TAG, "getPPPOEInfo");
        Status status = Status.fromInt(this.mPPPOEState.get());
        long onLineTime = 0;
        if (status.equals(Status.ONLINE)) {
            onLineTime = (System.currentTimeMillis() - this.mConnectedTimeAtomicLong.get()) / 1000;
        }
        return new PPPOEInfo(status, onLineTime);
    }

    private Object[] buildStartArgs(PPPOEConfig serverConfig) {
        Object[] cfgArgs = serverConfig.getArgs();
        int i = 1;
        int argc = cfgArgs.length + 1;
        Object[] argv = new Object[argc];
        argv[0] = "start";
        while (i < argc) {
            argv[i] = cfgArgs[i - 1];
            i++;
        }
        return argv;
    }
}
