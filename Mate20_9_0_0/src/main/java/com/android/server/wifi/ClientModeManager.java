package com.android.server.wifi;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.WifiNative.InterfaceCallback;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class ClientModeManager implements ActiveModeManager {
    private static final String TAG = "WifiClientModeManager";
    private String mClientInterfaceName;
    private final Context mContext;
    private boolean mExpectedStop = false;
    private boolean mIfaceIsUp = false;
    private final Listener mListener;
    private final ScanRequestProxy mScanRequestProxy;
    private final ClientModeStateMachine mStateMachine;
    private final WifiMetrics mWifiMetrics;
    private final WifiNative mWifiNative;
    private final WifiStateMachine mWifiStateMachine;

    private class ClientModeStateMachine extends StateMachine {
        public static final int CMD_INTERFACE_DESTROYED = 4;
        public static final int CMD_INTERFACE_DOWN = 5;
        public static final int CMD_INTERFACE_STATUS_CHANGED = 3;
        public static final int CMD_START = 0;
        private final State mIdleState = new IdleState();
        private final State mStartedState = new StartedState();
        private final InterfaceCallback mWifiNativeInterfaceCallback = new InterfaceCallback() {
            public void onDestroyed(String ifaceName) {
                if (ClientModeManager.this.mClientInterfaceName != null && ClientModeManager.this.mClientInterfaceName.equals(ifaceName)) {
                    String str = ClientModeManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("STA iface ");
                    stringBuilder.append(ifaceName);
                    stringBuilder.append(" was destroyed, stopping client mode");
                    Log.d(str, stringBuilder.toString());
                    ClientModeManager.this.mWifiStateMachine.handleIfaceDestroyed();
                    ClientModeStateMachine.this.sendMessage(4);
                }
            }

            public void onUp(String ifaceName) {
                if (ClientModeManager.this.mClientInterfaceName != null && ClientModeManager.this.mClientInterfaceName.equals(ifaceName)) {
                    ClientModeStateMachine.this.sendMessage(3, 1);
                }
            }

            public void onDown(String ifaceName) {
                if (ClientModeManager.this.mClientInterfaceName != null && ClientModeManager.this.mClientInterfaceName.equals(ifaceName)) {
                    ClientModeStateMachine.this.sendMessage(3, 0);
                }
            }
        };

        private class IdleState extends State {
            private IdleState() {
            }

            public void enter() {
                Log.d(ClientModeManager.TAG, "entering IdleState");
                ClientModeManager.this.mClientInterfaceName = null;
                ClientModeManager.this.mIfaceIsUp = false;
                ClientModeManager.this.mWifiStateMachine.initialFeatureSet(0);
            }

            public boolean processMessage(Message message) {
                if (message.what != 0) {
                    String str = ClientModeManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("received an invalid message: ");
                    stringBuilder.append(message);
                    Log.d(str, stringBuilder.toString());
                    return false;
                }
                ClientModeManager.this.updateWifiState(2, 1);
                ClientModeManager.this.mClientInterfaceName = ClientModeManager.this.mWifiNative.setupInterfaceForClientMode(false, ClientModeStateMachine.this.mWifiNativeInterfaceCallback);
                if (TextUtils.isEmpty(ClientModeManager.this.mClientInterfaceName)) {
                    Log.e(ClientModeManager.TAG, "Failed to create ClientInterface. Sit in Idle");
                    ClientModeManager.this.updateWifiState(4, 2);
                    ClientModeManager.this.updateWifiState(1, 4);
                } else {
                    ClientModeStateMachine.this.sendScanAvailableBroadcast(false);
                    ClientModeManager.this.mScanRequestProxy.enableScanningForHiddenNetworks(false);
                    ClientModeManager.this.mScanRequestProxy.clearScanResults();
                    ClientModeStateMachine.this.transitionTo(ClientModeStateMachine.this.mStartedState);
                }
                return true;
            }
        }

        private class StartedState extends State {
            private StartedState() {
            }

            private void onUpChanged(boolean isUp) {
                if (isUp != ClientModeManager.this.mIfaceIsUp) {
                    ClientModeManager.this.mIfaceIsUp = isUp;
                    if (isUp) {
                        Log.d(ClientModeManager.TAG, "Wifi is ready to use for client mode");
                        ClientModeStateMachine.this.sendScanAvailableBroadcast(true);
                        ClientModeManager.this.mWifiStateMachine.setOperationalMode(1, ClientModeManager.this.mClientInterfaceName);
                        ClientModeManager.this.updateWifiState(3, 2);
                    } else if (!ClientModeManager.this.mWifiStateMachine.isConnectedMacRandomizationEnabled()) {
                        Log.d(ClientModeManager.TAG, "interface down!");
                        ClientModeManager.this.updateWifiState(4, 3);
                        ClientModeManager.this.mStateMachine.sendMessage(5);
                    }
                }
            }

            public void enter() {
                Log.d(ClientModeManager.TAG, "entering StartedState");
                ClientModeManager.this.mIfaceIsUp = false;
                onUpChanged(ClientModeManager.this.mWifiNative.isInterfaceUp(ClientModeManager.this.mClientInterfaceName));
                ClientModeManager.this.mScanRequestProxy.enableScanningForHiddenNetworks(true);
            }

            public boolean processMessage(Message message) {
                int i = message.what;
                if (i != 0) {
                    boolean z = false;
                    switch (i) {
                        case 3:
                            if (message.arg1 == 1) {
                                z = true;
                            }
                            boolean isUp = z;
                            ClientModeManager.this.mWifiStateMachine.loadAndEnableAllNetworksByMode();
                            if (WifiStateMachineHisiExt.hisiWifiEnabled()) {
                                ClientModeManager.this.mWifiStateMachine.mWifiStateMachineHisiExt.startWifiForP2pCheck();
                            }
                            onUpChanged(isUp);
                            break;
                        case 4:
                            Log.d(ClientModeManager.TAG, "interface destroyed - client mode stopping");
                            ClientModeManager.this.updateWifiState(0, 3);
                            ClientModeManager.this.mClientInterfaceName = null;
                            ClientModeStateMachine.this.transitionTo(ClientModeStateMachine.this.mIdleState);
                            break;
                        case 5:
                            Log.e(ClientModeManager.TAG, "Detected an interface down, reporting failure to SelfRecovery");
                            ClientModeManager.this.mWifiStateMachine.failureDetected(2);
                            ClientModeManager.this.updateWifiState(0, 4);
                            ClientModeStateMachine.this.transitionTo(ClientModeStateMachine.this.mIdleState);
                            break;
                        default:
                            return false;
                    }
                }
                return true;
            }

            public void exit() {
                ClientModeManager.this.mWifiStateMachine.setOperationalMode(4, null);
                if (ClientModeManager.this.mClientInterfaceName != null) {
                    ClientModeManager.this.mWifiNative.teardownInterface(ClientModeManager.this.mClientInterfaceName);
                    ClientModeManager.this.mClientInterfaceName = null;
                    ClientModeManager.this.mIfaceIsUp = false;
                }
                ClientModeManager.this.updateWifiState(1, 0);
                ClientModeManager.this.mStateMachine.quitNow();
            }
        }

        ClientModeStateMachine(Looper looper) {
            super(ClientModeManager.TAG, looper);
            addState(this.mIdleState);
            addState(this.mStartedState);
            setInitialState(this.mIdleState);
            start();
        }

        private void sendScanAvailableBroadcast(boolean available) {
            String str = ClientModeManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sending scan available broadcast: ");
            stringBuilder.append(available);
            Log.d(str, stringBuilder.toString());
            Intent intent = new Intent("wifi_scan_available");
            intent.addFlags(67108864);
            if (available) {
                intent.putExtra("scan_enabled", 3);
            } else {
                intent.putExtra("scan_enabled", 1);
            }
            ClientModeManager.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    public interface Listener {
        void onStateChanged(int i);
    }

    ClientModeManager(Context context, Looper looper, WifiNative wifiNative, Listener listener, WifiMetrics wifiMetrics, ScanRequestProxy scanRequestProxy, WifiStateMachine wifiStateMachine) {
        this.mContext = context;
        this.mWifiNative = wifiNative;
        this.mListener = listener;
        this.mWifiMetrics = wifiMetrics;
        this.mScanRequestProxy = scanRequestProxy;
        this.mWifiStateMachine = wifiStateMachine;
        this.mStateMachine = new ClientModeStateMachine(looper);
    }

    public void start() {
        this.mStateMachine.sendMessage(0);
    }

    public void stop() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" currentstate: ");
        stringBuilder.append(getCurrentStateName());
        Log.d(str, stringBuilder.toString());
        this.mExpectedStop = true;
        if (this.mClientInterfaceName != null) {
            if (this.mIfaceIsUp) {
                updateWifiState(0, 3);
            } else {
                updateWifiState(0, 2);
            }
        }
        this.mStateMachine.quitNow();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("--Dump of ClientModeManager--");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("current StateMachine mode: ");
        stringBuilder.append(getCurrentStateName());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mClientInterfaceName: ");
        stringBuilder.append(this.mClientInterfaceName);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mIfaceIsUp: ");
        stringBuilder.append(this.mIfaceIsUp);
        pw.println(stringBuilder.toString());
    }

    private String getCurrentStateName() {
        IState currentState = this.mStateMachine.getCurrentState();
        if (currentState != null) {
            return currentState.getName();
        }
        return "StateMachine not active";
    }

    private void updateWifiState(int newState, int currentState) {
        if (this.mExpectedStop) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("expected stop, not triggering callbacks: newState = ");
            stringBuilder.append(newState);
            Log.d(str, stringBuilder.toString());
        } else {
            this.mListener.onStateChanged(newState);
        }
        if (newState == 4 || newState == 1) {
            this.mExpectedStop = true;
        }
        if (newState != 4) {
            this.mWifiStateMachine.setWifiStateForHw(newState);
            this.mWifiStateMachine.setWifiStateForApiCalls(newState);
            if (this.mWifiStateMachine.checkSelfCureWifiResult(101)) {
                Log.d(TAG, "updateWifiState, ignore to send intent due to wifi self curing.");
                return;
            }
            Intent intent = new Intent("android.net.wifi.WIFI_STATE_CHANGED");
            intent.addFlags(83886080);
            intent.putExtra("wifi_state", newState);
            intent.putExtra("previous_wifi_state", currentState);
            this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
    }
}
