package com.android.server.wifi.aware;

import android.hardware.wifi.V1_0.IWifiNanIfaceEventCallback.Stub;
import android.hardware.wifi.V1_0.NanCapabilities;
import android.hardware.wifi.V1_0.NanClusterEventInd;
import android.hardware.wifi.V1_0.NanDataPathConfirmInd;
import android.hardware.wifi.V1_0.NanDataPathRequestInd;
import android.hardware.wifi.V1_0.NanFollowupReceivedInd;
import android.hardware.wifi.V1_0.NanMatchInd;
import android.hardware.wifi.V1_0.WifiNanStatus;
import android.os.ShellCommand;
import android.util.Log;
import android.util.SparseIntArray;
import com.android.server.wifi.aware.WifiAwareShellCommand.DelegatedShellCommand;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import org.json.JSONException;
import org.json.JSONObject;

public class WifiAwareNativeCallback extends Stub implements DelegatedShellCommand {
    private static final int CB_EV_CLUSTER = 0;
    private static final int CB_EV_DATA_PATH_CONFIRM = 9;
    private static final int CB_EV_DATA_PATH_REQUEST = 8;
    private static final int CB_EV_DATA_PATH_TERMINATED = 10;
    private static final int CB_EV_DISABLED = 1;
    private static final int CB_EV_FOLLOWUP_RECEIVED = 6;
    private static final int CB_EV_MATCH = 4;
    private static final int CB_EV_MATCH_EXPIRED = 5;
    private static final int CB_EV_PUBLISH_TERMINATED = 2;
    private static final int CB_EV_SUBSCRIBE_TERMINATED = 3;
    private static final int CB_EV_TRANSMIT_FOLLOWUP = 7;
    private static final boolean DBG = false;
    private static final String TAG = "WifiAwareNativeCallback";
    private static final boolean VDBG = false;
    private SparseIntArray mCallbackCounter = new SparseIntArray();
    private final WifiAwareStateManager mWifiAwareStateManager;

    public WifiAwareNativeCallback(WifiAwareStateManager wifiAwareStateManager) {
        this.mWifiAwareStateManager = wifiAwareStateManager;
    }

    private void incrementCbCount(int callbackId) {
        this.mCallbackCounter.put(callbackId, this.mCallbackCounter.get(callbackId) + 1);
    }

    public int onCommand(ShellCommand parentShell) {
        PrintWriter pwe = parentShell.getErrPrintWriter();
        PrintWriter pwo = parentShell.getOutPrintWriter();
        if (parentShell.getNextArgRequired().equals("get_cb_count")) {
            String option = parentShell.getNextOption();
            Log.v(TAG, "option='" + option + "'");
            boolean reset = false;
            if (option != null) {
                if ("--reset".equals(option)) {
                    reset = true;
                } else {
                    pwe.println("Unknown option to 'get_cb_count'");
                    return -1;
                }
            }
            JSONObject j = new JSONObject();
            int i = 0;
            while (i < this.mCallbackCounter.size()) {
                try {
                    j.put(Integer.toString(this.mCallbackCounter.keyAt(i)), this.mCallbackCounter.valueAt(i));
                    i++;
                } catch (JSONException e) {
                    Log.e(TAG, "onCommand: get_cb_count e=" + e);
                }
            }
            pwo.println(j.toString());
            if (reset) {
                this.mCallbackCounter.clear();
            }
            return 0;
        }
        pwe.println("Unknown 'wifiaware native_cb <cmd>'");
        return -1;
    }

    public void onReset() {
    }

    public void onHelp(String command, ShellCommand parentShell) {
        PrintWriter pw = parentShell.getOutPrintWriter();
        pw.println("  " + command);
        pw.println("    get_cb_count [--reset]: gets the number of callbacks (and optionally reset count)");
    }

    public void notifyCapabilitiesResponse(short id, WifiNanStatus status, NanCapabilities capabilities) {
        if (status.status == 0) {
            Capabilities frameworkCapabilities = new Capabilities();
            frameworkCapabilities.maxConcurrentAwareClusters = capabilities.maxConcurrentClusters;
            frameworkCapabilities.maxPublishes = capabilities.maxPublishes;
            frameworkCapabilities.maxSubscribes = capabilities.maxSubscribes;
            frameworkCapabilities.maxServiceNameLen = capabilities.maxServiceNameLen;
            frameworkCapabilities.maxMatchFilterLen = capabilities.maxMatchFilterLen;
            frameworkCapabilities.maxTotalMatchFilterLen = capabilities.maxTotalMatchFilterLen;
            frameworkCapabilities.maxServiceSpecificInfoLen = capabilities.maxServiceSpecificInfoLen;
            frameworkCapabilities.maxExtendedServiceSpecificInfoLen = capabilities.maxExtendedServiceSpecificInfoLen;
            frameworkCapabilities.maxNdiInterfaces = capabilities.maxNdiInterfaces;
            frameworkCapabilities.maxNdpSessions = capabilities.maxNdpSessions;
            frameworkCapabilities.maxAppInfoLen = capabilities.maxAppInfoLen;
            frameworkCapabilities.maxQueuedTransmitMessages = capabilities.maxQueuedTransmitFollowupMsgs;
            frameworkCapabilities.maxSubscribeInterfaceAddresses = capabilities.maxSubscribeInterfaceAddresses;
            frameworkCapabilities.supportedCipherSuites = capabilities.supportedCipherSuites;
            this.mWifiAwareStateManager.onCapabilitiesUpdateResponse(id, frameworkCapabilities);
            return;
        }
        Log.e(TAG, "notifyCapabilitiesResponse: error code=" + status.status + " (" + status.description + ")");
    }

    public void notifyEnableResponse(short id, WifiNanStatus status) {
        if (status.status == 10) {
            Log.wtf(TAG, "notifyEnableResponse: id=" + id + ", already enabled!?");
        }
        if (status.status == 0 || status.status == 10) {
            this.mWifiAwareStateManager.onConfigSuccessResponse(id);
        } else {
            this.mWifiAwareStateManager.onConfigFailedResponse(id, status.status);
        }
    }

    public void notifyConfigResponse(short id, WifiNanStatus status) {
        if (status.status == 0) {
            this.mWifiAwareStateManager.onConfigSuccessResponse(id);
        } else {
            this.mWifiAwareStateManager.onConfigFailedResponse(id, status.status);
        }
    }

    public void notifyDisableResponse(short id, WifiNanStatus status) {
        if (status.status != 0) {
            Log.e(TAG, "notifyDisableResponse: failure - code=" + status.status + " (" + status.description + ")");
        }
        this.mWifiAwareStateManager.onDisableResponse(id, status.status);
    }

    public void notifyStartPublishResponse(short id, WifiNanStatus status, byte publishId) {
        if (status.status == 0) {
            this.mWifiAwareStateManager.onSessionConfigSuccessResponse(id, true, publishId);
        } else {
            this.mWifiAwareStateManager.onSessionConfigFailResponse(id, true, status.status);
        }
    }

    public void notifyStopPublishResponse(short id, WifiNanStatus status) {
        if (status.status != 0) {
            Log.e(TAG, "notifyStopPublishResponse: failure - code=" + status.status + " (" + status.description + ")");
        }
    }

    public void notifyStartSubscribeResponse(short id, WifiNanStatus status, byte subscribeId) {
        if (status.status == 0) {
            this.mWifiAwareStateManager.onSessionConfigSuccessResponse(id, false, subscribeId);
        } else {
            this.mWifiAwareStateManager.onSessionConfigFailResponse(id, false, status.status);
        }
    }

    public void notifyStopSubscribeResponse(short id, WifiNanStatus status) {
        if (status.status != 0) {
            Log.e(TAG, "notifyStopSubscribeResponse: failure - code=" + status.status + " (" + status.description + ")");
        }
    }

    public void notifyTransmitFollowupResponse(short id, WifiNanStatus status) {
        if (status.status == 0) {
            this.mWifiAwareStateManager.onMessageSendQueuedSuccessResponse(id);
        } else {
            this.mWifiAwareStateManager.onMessageSendQueuedFailResponse(id, status.status);
        }
    }

    public void notifyCreateDataInterfaceResponse(short id, WifiNanStatus status) {
        boolean z = false;
        WifiAwareStateManager wifiAwareStateManager = this.mWifiAwareStateManager;
        if (status.status == 0) {
            z = true;
        }
        wifiAwareStateManager.onCreateDataPathInterfaceResponse(id, z, status.status);
    }

    public void notifyDeleteDataInterfaceResponse(short id, WifiNanStatus status) {
        boolean z = false;
        WifiAwareStateManager wifiAwareStateManager = this.mWifiAwareStateManager;
        if (status.status == 0) {
            z = true;
        }
        wifiAwareStateManager.onDeleteDataPathInterfaceResponse(id, z, status.status);
    }

    public void notifyInitiateDataPathResponse(short id, WifiNanStatus status, int ndpInstanceId) {
        if (status.status == 0) {
            this.mWifiAwareStateManager.onInitiateDataPathResponseSuccess(id, ndpInstanceId);
        } else {
            this.mWifiAwareStateManager.onInitiateDataPathResponseFail(id, status.status);
        }
    }

    public void notifyRespondToDataPathIndicationResponse(short id, WifiNanStatus status) {
        boolean z = false;
        WifiAwareStateManager wifiAwareStateManager = this.mWifiAwareStateManager;
        if (status.status == 0) {
            z = true;
        }
        wifiAwareStateManager.onRespondToDataPathSetupRequestResponse(id, z, status.status);
    }

    public void notifyTerminateDataPathResponse(short id, WifiNanStatus status) {
        boolean z = false;
        WifiAwareStateManager wifiAwareStateManager = this.mWifiAwareStateManager;
        if (status.status == 0) {
            z = true;
        }
        wifiAwareStateManager.onEndDataPathResponse(id, z, status.status);
    }

    public void eventClusterEvent(NanClusterEventInd event) {
        incrementCbCount(0);
        if (event.eventType == 0) {
            this.mWifiAwareStateManager.onInterfaceAddressChangeNotification(event.addr);
        } else if (event.eventType == 1) {
            this.mWifiAwareStateManager.onClusterChangeNotification(0, event.addr);
        } else if (event.eventType == 2) {
            this.mWifiAwareStateManager.onClusterChangeNotification(1, event.addr);
        } else {
            Log.e(TAG, "eventClusterEvent: invalid eventType=" + event.eventType);
        }
    }

    public void eventDisabled(WifiNanStatus status) {
        incrementCbCount(1);
        this.mWifiAwareStateManager.onAwareDownNotification(status.status);
    }

    public void eventPublishTerminated(byte sessionId, WifiNanStatus status) {
        incrementCbCount(2);
        this.mWifiAwareStateManager.onSessionTerminatedNotification(sessionId, status.status, true);
    }

    public void eventSubscribeTerminated(byte sessionId, WifiNanStatus status) {
        incrementCbCount(3);
        this.mWifiAwareStateManager.onSessionTerminatedNotification(sessionId, status.status, false);
    }

    public void eventMatch(NanMatchInd event) {
        incrementCbCount(4);
        this.mWifiAwareStateManager.onMatchNotification(event.discoverySessionId, event.peerId, event.addr, convertArrayListToNativeByteArray(event.serviceSpecificInfo), convertArrayListToNativeByteArray(event.matchFilter));
    }

    public void eventMatchExpired(byte discoverySessionId, int peerId) {
        incrementCbCount(5);
    }

    public void eventFollowupReceived(NanFollowupReceivedInd event) {
        incrementCbCount(6);
        this.mWifiAwareStateManager.onMessageReceivedNotification(event.discoverySessionId, event.peerId, event.addr, convertArrayListToNativeByteArray(event.serviceSpecificInfo));
    }

    public void eventTransmitFollowup(short id, WifiNanStatus status) {
        incrementCbCount(7);
        if (status.status == 0) {
            this.mWifiAwareStateManager.onMessageSendSuccessNotification(id);
        } else {
            this.mWifiAwareStateManager.onMessageSendFailNotification(id, status.status);
        }
    }

    public void eventDataPathRequest(NanDataPathRequestInd event) {
        incrementCbCount(8);
        this.mWifiAwareStateManager.onDataPathRequestNotification(event.discoverySessionId, event.peerDiscMacAddr, event.ndpInstanceId);
    }

    public void eventDataPathConfirm(NanDataPathConfirmInd event) {
        incrementCbCount(9);
        this.mWifiAwareStateManager.onDataPathConfirmNotification(event.ndpInstanceId, event.peerNdiMacAddr, event.dataPathSetupSuccess, event.status.status, convertArrayListToNativeByteArray(event.appInfo));
    }

    public void eventDataPathTerminated(int ndpInstanceId) {
        incrementCbCount(10);
        this.mWifiAwareStateManager.onDataPathEndNotification(ndpInstanceId);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("WifiAwareNativeCallback:");
        pw.println("  mCallbackCounter: " + this.mCallbackCounter);
    }

    private byte[] convertArrayListToNativeByteArray(ArrayList<Byte> from) {
        if (from == null) {
            return null;
        }
        byte[] to = new byte[from.size()];
        for (int i = 0; i < from.size(); i++) {
            to[i] = ((Byte) from.get(i)).byteValue();
        }
        return to;
    }

    private static String statusString(WifiNanStatus status) {
        if (status == null) {
            return "status=null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(status.status).append(" (").append(status.description).append(")");
        return sb.toString();
    }
}
