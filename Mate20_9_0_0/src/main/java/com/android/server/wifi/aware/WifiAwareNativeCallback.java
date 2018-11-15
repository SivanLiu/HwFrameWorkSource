package com.android.server.wifi.aware;

import android.hardware.wifi.V1_0.NanCapabilities;
import android.hardware.wifi.V1_0.NanClusterEventInd;
import android.hardware.wifi.V1_0.NanDataPathConfirmInd;
import android.hardware.wifi.V1_0.NanDataPathRequestInd;
import android.hardware.wifi.V1_0.NanFollowupReceivedInd;
import android.hardware.wifi.V1_0.NanMatchInd;
import android.hardware.wifi.V1_0.WifiNanStatus;
import android.hardware.wifi.V1_2.IWifiNanIfaceEventCallback.Stub;
import android.hardware.wifi.V1_2.NanDataPathScheduleUpdateInd;
import android.os.ShellCommand;
import android.util.Log;
import android.util.SparseIntArray;
import com.android.server.wifi.aware.WifiAwareShellCommand.DelegatedShellCommand;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import libcore.util.HexEncoding;
import org.json.JSONException;
import org.json.JSONObject;

public class WifiAwareNativeCallback extends Stub implements DelegatedShellCommand {
    private static final int CB_EV_CLUSTER = 0;
    private static final int CB_EV_DATA_PATH_CONFIRM = 9;
    private static final int CB_EV_DATA_PATH_REQUEST = 8;
    private static final int CB_EV_DATA_PATH_SCHED_UPDATE = 11;
    private static final int CB_EV_DATA_PATH_TERMINATED = 10;
    private static final int CB_EV_DISABLED = 1;
    private static final int CB_EV_FOLLOWUP_RECEIVED = 6;
    private static final int CB_EV_MATCH = 4;
    private static final int CB_EV_MATCH_EXPIRED = 5;
    private static final int CB_EV_PUBLISH_TERMINATED = 2;
    private static final int CB_EV_SUBSCRIBE_TERMINATED = 3;
    private static final int CB_EV_TRANSMIT_FOLLOWUP = 7;
    private static final String TAG = "WifiAwareNativeCallback";
    private static final boolean VDBG = false;
    private SparseIntArray mCallbackCounter = new SparseIntArray();
    boolean mDbg = false;
    boolean mIsHal12OrLater = false;
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
        String subCmd = parentShell.getNextArgRequired();
        int i = (subCmd.hashCode() == -1587855368 && subCmd.equals("get_cb_count")) ? 0 : -1;
        if (i != 0) {
            pwe.println("Unknown 'wifiaware native_cb <cmd>'");
            return -1;
        }
        String option = parentShell.getNextOption();
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
        int i2 = 0;
        while (i2 < this.mCallbackCounter.size()) {
            try {
                j.put(Integer.toString(this.mCallbackCounter.keyAt(i2)), this.mCallbackCounter.valueAt(i2));
                i2++;
            } catch (JSONException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onCommand: get_cb_count e=");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
            }
        }
        pwo.println(j.toString());
        if (reset) {
            this.mCallbackCounter.clear();
        }
        return 0;
    }

    public void onReset() {
    }

    public void onHelp(String command, ShellCommand parentShell) {
        PrintWriter pw = parentShell.getOutPrintWriter();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  ");
        stringBuilder.append(command);
        pw.println(stringBuilder.toString());
        pw.println("    get_cb_count [--reset]: gets the number of callbacks (and optionally reset count)");
    }

    public void notifyCapabilitiesResponse(short id, WifiNanStatus status, NanCapabilities capabilities) {
        String str;
        StringBuilder stringBuilder;
        if (this.mDbg) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("notifyCapabilitiesResponse: id=");
            stringBuilder.append(id);
            stringBuilder.append(", status=");
            stringBuilder.append(statusString(status));
            stringBuilder.append(", capabilities=");
            stringBuilder.append(capabilities);
            Log.v(str, stringBuilder.toString());
        }
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
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("notifyCapabilitiesResponse: error code=");
        stringBuilder.append(status.status);
        stringBuilder.append(" (");
        stringBuilder.append(status.description);
        stringBuilder.append(")");
        Log.e(str, stringBuilder.toString());
    }

    public void notifyEnableResponse(short id, WifiNanStatus status) {
        String str;
        if (this.mDbg) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyEnableResponse: id=");
            stringBuilder.append(id);
            stringBuilder.append(", status=");
            stringBuilder.append(statusString(status));
            Log.v(str, stringBuilder.toString());
        }
        if (status.status == 10) {
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("notifyEnableResponse: id=");
            stringBuilder2.append(id);
            stringBuilder2.append(", already enabled!?");
            Log.wtf(str, stringBuilder2.toString());
        }
        if (status.status == 0 || status.status == 10) {
            this.mWifiAwareStateManager.onConfigSuccessResponse(id);
        } else {
            this.mWifiAwareStateManager.onConfigFailedResponse(id, status.status);
        }
    }

    public void notifyConfigResponse(short id, WifiNanStatus status) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyConfigResponse: id=");
            stringBuilder.append(id);
            stringBuilder.append(", status=");
            stringBuilder.append(statusString(status));
            Log.v(str, stringBuilder.toString());
        }
        if (status.status == 0) {
            this.mWifiAwareStateManager.onConfigSuccessResponse(id);
        } else {
            this.mWifiAwareStateManager.onConfigFailedResponse(id, status.status);
        }
    }

    public void notifyDisableResponse(short id, WifiNanStatus status) {
        String str;
        StringBuilder stringBuilder;
        if (this.mDbg) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("notifyDisableResponse: id=");
            stringBuilder.append(id);
            stringBuilder.append(", status=");
            stringBuilder.append(statusString(status));
            Log.v(str, stringBuilder.toString());
        }
        if (status.status != 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("notifyDisableResponse: failure - code=");
            stringBuilder.append(status.status);
            stringBuilder.append(" (");
            stringBuilder.append(status.description);
            stringBuilder.append(")");
            Log.e(str, stringBuilder.toString());
        }
        this.mWifiAwareStateManager.onDisableResponse(id, status.status);
    }

    public void notifyStartPublishResponse(short id, WifiNanStatus status, byte publishId) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyStartPublishResponse: id=");
            stringBuilder.append(id);
            stringBuilder.append(", status=");
            stringBuilder.append(statusString(status));
            stringBuilder.append(", publishId=");
            stringBuilder.append(publishId);
            Log.v(str, stringBuilder.toString());
        }
        if (status.status == 0) {
            this.mWifiAwareStateManager.onSessionConfigSuccessResponse(id, true, publishId);
        } else {
            this.mWifiAwareStateManager.onSessionConfigFailResponse(id, true, status.status);
        }
    }

    public void notifyStopPublishResponse(short id, WifiNanStatus status) {
        String str;
        StringBuilder stringBuilder;
        if (this.mDbg) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("notifyStopPublishResponse: id=");
            stringBuilder.append(id);
            stringBuilder.append(", status=");
            stringBuilder.append(statusString(status));
            Log.v(str, stringBuilder.toString());
        }
        if (status.status != 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("notifyStopPublishResponse: failure - code=");
            stringBuilder.append(status.status);
            stringBuilder.append(" (");
            stringBuilder.append(status.description);
            stringBuilder.append(")");
            Log.e(str, stringBuilder.toString());
        }
    }

    public void notifyStartSubscribeResponse(short id, WifiNanStatus status, byte subscribeId) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyStartSubscribeResponse: id=");
            stringBuilder.append(id);
            stringBuilder.append(", status=");
            stringBuilder.append(statusString(status));
            stringBuilder.append(", subscribeId=");
            stringBuilder.append(subscribeId);
            Log.v(str, stringBuilder.toString());
        }
        if (status.status == 0) {
            this.mWifiAwareStateManager.onSessionConfigSuccessResponse(id, false, subscribeId);
        } else {
            this.mWifiAwareStateManager.onSessionConfigFailResponse(id, false, status.status);
        }
    }

    public void notifyStopSubscribeResponse(short id, WifiNanStatus status) {
        String str;
        StringBuilder stringBuilder;
        if (this.mDbg) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("notifyStopSubscribeResponse: id=");
            stringBuilder.append(id);
            stringBuilder.append(", status=");
            stringBuilder.append(statusString(status));
            Log.v(str, stringBuilder.toString());
        }
        if (status.status != 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("notifyStopSubscribeResponse: failure - code=");
            stringBuilder.append(status.status);
            stringBuilder.append(" (");
            stringBuilder.append(status.description);
            stringBuilder.append(")");
            Log.e(str, stringBuilder.toString());
        }
    }

    public void notifyTransmitFollowupResponse(short id, WifiNanStatus status) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyTransmitFollowupResponse: id=");
            stringBuilder.append(id);
            stringBuilder.append(", status=");
            stringBuilder.append(statusString(status));
            Log.v(str, stringBuilder.toString());
        }
        if (status.status == 0) {
            this.mWifiAwareStateManager.onMessageSendQueuedSuccessResponse(id);
        } else {
            this.mWifiAwareStateManager.onMessageSendQueuedFailResponse(id, status.status);
        }
    }

    public void notifyCreateDataInterfaceResponse(short id, WifiNanStatus status) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyCreateDataInterfaceResponse: id=");
            stringBuilder.append(id);
            stringBuilder.append(", status=");
            stringBuilder.append(statusString(status));
            Log.v(str, stringBuilder.toString());
        }
        this.mWifiAwareStateManager.onCreateDataPathInterfaceResponse(id, status.status == 0, status.status);
    }

    public void notifyDeleteDataInterfaceResponse(short id, WifiNanStatus status) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyDeleteDataInterfaceResponse: id=");
            stringBuilder.append(id);
            stringBuilder.append(", status=");
            stringBuilder.append(statusString(status));
            Log.v(str, stringBuilder.toString());
        }
        this.mWifiAwareStateManager.onDeleteDataPathInterfaceResponse(id, status.status == 0, status.status);
    }

    public void notifyInitiateDataPathResponse(short id, WifiNanStatus status, int ndpInstanceId) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyInitiateDataPathResponse: id=");
            stringBuilder.append(id);
            stringBuilder.append(", status=");
            stringBuilder.append(statusString(status));
            stringBuilder.append(", ndpInstanceId=");
            stringBuilder.append(ndpInstanceId);
            Log.v(str, stringBuilder.toString());
        }
        if (status.status == 0) {
            this.mWifiAwareStateManager.onInitiateDataPathResponseSuccess(id, ndpInstanceId);
        } else {
            this.mWifiAwareStateManager.onInitiateDataPathResponseFail(id, status.status);
        }
    }

    public void notifyRespondToDataPathIndicationResponse(short id, WifiNanStatus status) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyRespondToDataPathIndicationResponse: id=");
            stringBuilder.append(id);
            stringBuilder.append(", status=");
            stringBuilder.append(statusString(status));
            Log.v(str, stringBuilder.toString());
        }
        this.mWifiAwareStateManager.onRespondToDataPathSetupRequestResponse(id, status.status == 0, status.status);
    }

    public void notifyTerminateDataPathResponse(short id, WifiNanStatus status) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyTerminateDataPathResponse: id=");
            stringBuilder.append(id);
            stringBuilder.append(", status=");
            stringBuilder.append(statusString(status));
            Log.v(str, stringBuilder.toString());
        }
        this.mWifiAwareStateManager.onEndDataPathResponse(id, status.status == 0, status.status);
    }

    public void eventClusterEvent(NanClusterEventInd event) {
        String str;
        StringBuilder stringBuilder;
        if (this.mDbg) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("eventClusterEvent: eventType=");
            stringBuilder.append(event.eventType);
            stringBuilder.append(", addr=");
            stringBuilder.append(String.valueOf(HexEncoding.encode(event.addr)));
            Log.v(str, stringBuilder.toString());
        }
        incrementCbCount(0);
        if (event.eventType == 0) {
            this.mWifiAwareStateManager.onInterfaceAddressChangeNotification(event.addr);
        } else if (event.eventType == 1) {
            this.mWifiAwareStateManager.onClusterChangeNotification(0, event.addr);
        } else if (event.eventType == 2) {
            this.mWifiAwareStateManager.onClusterChangeNotification(1, event.addr);
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("eventClusterEvent: invalid eventType=");
            stringBuilder.append(event.eventType);
            Log.e(str, stringBuilder.toString());
        }
    }

    public void eventDisabled(WifiNanStatus status) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("eventDisabled: status=");
            stringBuilder.append(statusString(status));
            Log.v(str, stringBuilder.toString());
        }
        incrementCbCount(1);
        this.mWifiAwareStateManager.onAwareDownNotification(status.status);
    }

    public void eventPublishTerminated(byte sessionId, WifiNanStatus status) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("eventPublishTerminated: sessionId=");
            stringBuilder.append(sessionId);
            stringBuilder.append(", status=");
            stringBuilder.append(statusString(status));
            Log.v(str, stringBuilder.toString());
        }
        incrementCbCount(2);
        this.mWifiAwareStateManager.onSessionTerminatedNotification(sessionId, status.status, true);
    }

    public void eventSubscribeTerminated(byte sessionId, WifiNanStatus status) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("eventSubscribeTerminated: sessionId=");
            stringBuilder.append(sessionId);
            stringBuilder.append(", status=");
            stringBuilder.append(statusString(status));
            Log.v(str, stringBuilder.toString());
        }
        incrementCbCount(3);
        this.mWifiAwareStateManager.onSessionTerminatedNotification(sessionId, status.status, false);
    }

    public void eventMatch(NanMatchInd event) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("eventMatch: discoverySessionId=");
            stringBuilder.append(event.discoverySessionId);
            stringBuilder.append(", peerId=");
            stringBuilder.append(event.peerId);
            stringBuilder.append(", addr=");
            stringBuilder.append(String.valueOf(HexEncoding.encode(event.addr)));
            stringBuilder.append(", serviceSpecificInfo=");
            stringBuilder.append(Arrays.toString(convertArrayListToNativeByteArray(event.serviceSpecificInfo)));
            stringBuilder.append(", ssi.size()=");
            int i = 0;
            stringBuilder.append(event.serviceSpecificInfo == null ? 0 : event.serviceSpecificInfo.size());
            stringBuilder.append(", matchFilter=");
            stringBuilder.append(Arrays.toString(convertArrayListToNativeByteArray(event.matchFilter)));
            stringBuilder.append(", mf.size()=");
            if (event.matchFilter != null) {
                i = event.matchFilter.size();
            }
            stringBuilder.append(i);
            stringBuilder.append(", rangingIndicationType=");
            stringBuilder.append(event.rangingIndicationType);
            stringBuilder.append(", rangingMeasurementInCm=");
            stringBuilder.append(event.rangingMeasurementInCm);
            Log.v(str, stringBuilder.toString());
        }
        incrementCbCount(4);
        this.mWifiAwareStateManager.onMatchNotification(event.discoverySessionId, event.peerId, event.addr, convertArrayListToNativeByteArray(event.serviceSpecificInfo), convertArrayListToNativeByteArray(event.matchFilter), event.rangingIndicationType, event.rangingMeasurementInCm * 10);
    }

    public void eventMatchExpired(byte discoverySessionId, int peerId) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("eventMatchExpired: discoverySessionId=");
            stringBuilder.append(discoverySessionId);
            stringBuilder.append(", peerId=");
            stringBuilder.append(peerId);
            Log.v(str, stringBuilder.toString());
        }
        incrementCbCount(5);
    }

    public void eventFollowupReceived(NanFollowupReceivedInd event) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("eventFollowupReceived: discoverySessionId=");
            stringBuilder.append(event.discoverySessionId);
            stringBuilder.append(", peerId=");
            stringBuilder.append(event.peerId);
            stringBuilder.append(", addr=");
            stringBuilder.append(String.valueOf(HexEncoding.encode(event.addr)));
            stringBuilder.append(", serviceSpecificInfo=");
            stringBuilder.append(Arrays.toString(convertArrayListToNativeByteArray(event.serviceSpecificInfo)));
            stringBuilder.append(", ssi.size()=");
            stringBuilder.append(event.serviceSpecificInfo == null ? 0 : event.serviceSpecificInfo.size());
            Log.v(str, stringBuilder.toString());
        }
        incrementCbCount(6);
        this.mWifiAwareStateManager.onMessageReceivedNotification(event.discoverySessionId, event.peerId, event.addr, convertArrayListToNativeByteArray(event.serviceSpecificInfo));
    }

    public void eventTransmitFollowup(short id, WifiNanStatus status) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("eventTransmitFollowup: id=");
            stringBuilder.append(id);
            stringBuilder.append(", status=");
            stringBuilder.append(statusString(status));
            Log.v(str, stringBuilder.toString());
        }
        incrementCbCount(7);
        if (status.status == 0) {
            this.mWifiAwareStateManager.onMessageSendSuccessNotification(id);
        } else {
            this.mWifiAwareStateManager.onMessageSendFailNotification(id, status.status);
        }
    }

    public void eventDataPathRequest(NanDataPathRequestInd event) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("eventDataPathRequest: discoverySessionId=");
            stringBuilder.append(event.discoverySessionId);
            stringBuilder.append(", peerDiscMacAddr=");
            stringBuilder.append(String.valueOf(HexEncoding.encode(event.peerDiscMacAddr)));
            stringBuilder.append(", ndpInstanceId=");
            stringBuilder.append(event.ndpInstanceId);
            Log.v(str, stringBuilder.toString());
        }
        incrementCbCount(8);
        this.mWifiAwareStateManager.onDataPathRequestNotification(event.discoverySessionId, event.peerDiscMacAddr, event.ndpInstanceId);
    }

    public void eventDataPathConfirm(NanDataPathConfirmInd event) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onDataPathConfirm: ndpInstanceId=");
            stringBuilder.append(event.ndpInstanceId);
            stringBuilder.append(", peerNdiMacAddr=");
            stringBuilder.append(String.valueOf(HexEncoding.encode(event.peerNdiMacAddr)));
            stringBuilder.append(", dataPathSetupSuccess=");
            stringBuilder.append(event.dataPathSetupSuccess);
            stringBuilder.append(", reason=");
            stringBuilder.append(event.status.status);
            Log.v(str, stringBuilder.toString());
        }
        if (this.mIsHal12OrLater) {
            Log.wtf(TAG, "eventDataPathConfirm should not be called by a >=1.2 HAL!");
        }
        incrementCbCount(9);
        this.mWifiAwareStateManager.onDataPathConfirmNotification(event.ndpInstanceId, event.peerNdiMacAddr, event.dataPathSetupSuccess, event.status.status, convertArrayListToNativeByteArray(event.appInfo), null);
    }

    public void eventDataPathConfirm_1_2(android.hardware.wifi.V1_2.NanDataPathConfirmInd event) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("eventDataPathConfirm_1_2: ndpInstanceId=");
            stringBuilder.append(event.V1_0.ndpInstanceId);
            stringBuilder.append(", peerNdiMacAddr=");
            stringBuilder.append(String.valueOf(HexEncoding.encode(event.V1_0.peerNdiMacAddr)));
            stringBuilder.append(", dataPathSetupSuccess=");
            stringBuilder.append(event.V1_0.dataPathSetupSuccess);
            stringBuilder.append(", reason=");
            stringBuilder.append(event.V1_0.status.status);
            Log.v(str, stringBuilder.toString());
        }
        if (this.mIsHal12OrLater) {
            incrementCbCount(9);
            this.mWifiAwareStateManager.onDataPathConfirmNotification(event.V1_0.ndpInstanceId, event.V1_0.peerNdiMacAddr, event.V1_0.dataPathSetupSuccess, event.V1_0.status.status, convertArrayListToNativeByteArray(event.V1_0.appInfo), event.channelInfo);
            return;
        }
        Log.wtf(TAG, "eventDataPathConfirm_1_2 should not be called by a <1.2 HAL!");
    }

    public void eventDataPathScheduleUpdate(NanDataPathScheduleUpdateInd event) {
        if (this.mDbg) {
            Log.v(TAG, "eventDataPathScheduleUpdate");
        }
        if (this.mIsHal12OrLater) {
            incrementCbCount(11);
            this.mWifiAwareStateManager.onDataPathScheduleUpdateNotification(event.peerDiscoveryAddress, event.ndpInstanceIds, event.channelInfo);
            return;
        }
        Log.wtf(TAG, "eventDataPathScheduleUpdate should not be called by a <1.2 HAL!");
    }

    public void eventDataPathTerminated(int ndpInstanceId) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("eventDataPathTerminated: ndpInstanceId=");
            stringBuilder.append(ndpInstanceId);
            Log.v(str, stringBuilder.toString());
        }
        incrementCbCount(10);
        this.mWifiAwareStateManager.onDataPathEndNotification(ndpInstanceId);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("WifiAwareNativeCallback:");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  mCallbackCounter: ");
        stringBuilder.append(this.mCallbackCounter);
        pw.println(stringBuilder.toString());
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
        sb.append(status.status);
        sb.append(" (");
        sb.append(status.description);
        sb.append(")");
        return sb.toString();
    }
}
