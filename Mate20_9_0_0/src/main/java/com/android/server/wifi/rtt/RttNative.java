package com.android.server.wifi.rtt;

import android.hardware.wifi.V1_0.IWifiRttController;
import android.hardware.wifi.V1_0.IWifiRttControllerEventCallback.Stub;
import android.hardware.wifi.V1_0.RttCapabilities;
import android.hardware.wifi.V1_0.RttConfig;
import android.hardware.wifi.V1_0.RttResult;
import android.hardware.wifi.V1_0.WifiStatus;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.ResponderConfig;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.wifi.HalDeviceManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.ListIterator;

public class RttNative extends Stub {
    private static final String TAG = "RttNative";
    private static final boolean VDBG = false;
    boolean mDbg = false;
    private final HalDeviceManager mHalDeviceManager;
    private volatile IWifiRttController mIWifiRttController;
    private Object mLock = new Object();
    private volatile RttCapabilities mRttCapabilities;
    private final RttServiceImpl mRttService;

    public RttNative(RttServiceImpl rttService, HalDeviceManager halDeviceManager) {
        this.mRttService = rttService;
        this.mHalDeviceManager = halDeviceManager;
    }

    public void start(Handler handler) {
        synchronized (this.mLock) {
            this.mHalDeviceManager.initialize();
            this.mHalDeviceManager.registerStatusListener(new -$$Lambda$RttNative$51zuZWl5ad-UD9FpUAuwwPgkpgg(this), handler);
            updateController();
        }
    }

    public boolean isReady() {
        return this.mIWifiRttController != null;
    }

    private void updateController() {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateController: mIWifiRttController=");
            stringBuilder.append(this.mIWifiRttController);
            Log.v(str, stringBuilder.toString());
        }
        synchronized (this.mLock) {
            IWifiRttController localWifiRttController = this.mIWifiRttController;
            if (!this.mHalDeviceManager.isStarted()) {
                localWifiRttController = null;
            } else if (localWifiRttController == null) {
                localWifiRttController = this.mHalDeviceManager.createRttController();
                if (localWifiRttController == null) {
                    Log.e(TAG, "updateController: Failed creating RTT controller - but Wifi is started!");
                } else {
                    try {
                        localWifiRttController.registerEventCallback(this);
                    } catch (RemoteException e) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("updateController: exception registering callback: ");
                        stringBuilder2.append(e);
                        Log.e(str2, stringBuilder2.toString());
                        localWifiRttController = null;
                    }
                }
            }
            this.mIWifiRttController = localWifiRttController;
            if (this.mIWifiRttController == null) {
                this.mRttService.disable();
            } else {
                this.mRttService.enableIfPossible();
                updateRttCapabilities();
            }
        }
    }

    void updateRttCapabilities() {
        if (this.mRttCapabilities == null) {
            synchronized (this.mLock) {
                try {
                    this.mIWifiRttController.getCapabilities(new -$$Lambda$RttNative$nRSOFcP2WhqxmfStf2OeZAekTCY(this));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("updateController: exception requesting capabilities: ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                }
                if (!(this.mRttCapabilities == null || this.mRttCapabilities.rttFtmSupported)) {
                    Log.wtf(TAG, "Firmware indicates RTT is not supported - but device supports RTT - ignored!?");
                }
            }
        }
    }

    public static /* synthetic */ void lambda$updateRttCapabilities$1(RttNative rttNative, WifiStatus status, RttCapabilities capabilities) {
        String str;
        StringBuilder stringBuilder;
        if (status.code != 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateController: error requesting capabilities -- code=");
            stringBuilder.append(status.code);
            Log.e(str, stringBuilder.toString());
            return;
        }
        if (rttNative.mDbg) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateController: RTT capabilities=");
            stringBuilder.append(capabilities);
            Log.v(str, stringBuilder.toString());
        }
        rttNative.mRttCapabilities = capabilities;
    }

    public boolean rangeRequest(int cmdId, RangingRequest request, boolean isCalledFromPrivilegedContext) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("rangeRequest: cmdId=");
            stringBuilder.append(cmdId);
            stringBuilder.append(", # of requests=");
            stringBuilder.append(request.mRttPeers.size());
            Log.v(str, stringBuilder.toString());
        }
        updateRttCapabilities();
        synchronized (this.mLock) {
            if (isReady()) {
                ArrayList<RttConfig> rttConfig = convertRangingRequestToRttConfigs(request, isCalledFromPrivilegedContext, this.mRttCapabilities);
                if (rttConfig == null) {
                    Log.e(TAG, "rangeRequest: invalid request parameters");
                    return false;
                } else if (rttConfig.size() == 0) {
                    Log.e(TAG, "rangeRequest: all requests invalidated");
                    this.mRttService.onRangingResults(cmdId, new ArrayList());
                    return true;
                } else {
                    String str2;
                    StringBuilder stringBuilder2;
                    try {
                        WifiStatus status = this.mIWifiRttController.rangeRequest(cmdId, rttConfig);
                        if (status.code != 0) {
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("rangeRequest: cannot issue range request -- code=");
                            stringBuilder2.append(status.code);
                            Log.e(str2, stringBuilder2.toString());
                            return false;
                        }
                        return true;
                    } catch (RemoteException e) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("rangeRequest: exception issuing range request: ");
                        stringBuilder2.append(e);
                        Log.e(str2, stringBuilder2.toString());
                        return false;
                    }
                }
            }
            Log.e(TAG, "rangeRequest: RttController is null");
            return false;
        }
    }

    public boolean rangeCancel(int cmdId, ArrayList<byte[]> macAddresses) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("rangeCancel: cmdId=");
            stringBuilder.append(cmdId);
            Log.v(str, stringBuilder.toString());
        }
        synchronized (this.mLock) {
            if (isReady()) {
                String str2;
                StringBuilder stringBuilder2;
                try {
                    WifiStatus status = this.mIWifiRttController.rangeCancel(cmdId, macAddresses);
                    if (status.code != 0) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("rangeCancel: cannot issue range cancel -- code=");
                        stringBuilder2.append(status.code);
                        Log.e(str2, stringBuilder2.toString());
                        return false;
                    }
                    return true;
                } catch (RemoteException e) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("rangeCancel: exception issuing range cancel: ");
                    stringBuilder2.append(e);
                    Log.e(str2, stringBuilder2.toString());
                    return false;
                }
            }
            Log.e(TAG, "rangeCancel: RttController is null");
            return false;
        }
    }

    public void onResults(int cmdId, ArrayList<RttResult> halResults) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onResults: cmdId=");
            stringBuilder.append(cmdId);
            stringBuilder.append(", # of results=");
            stringBuilder.append(halResults.size());
            Log.v(str, stringBuilder.toString());
        }
        if (halResults == null) {
            halResults = new ArrayList();
        }
        ListIterator<RttResult> lit = halResults.listIterator();
        while (lit.hasNext()) {
            if (lit.next() == null) {
                lit.remove();
            }
        }
        this.mRttService.onRangingResults(cmdId, halResults);
    }

    private static ArrayList<RttConfig> convertRangingRequestToRttConfigs(RangingRequest request, boolean isCalledFromPrivilegedContext, RttCapabilities cap) {
        ArrayList<RttConfig> rttConfigs = new ArrayList(request.mRttPeers.size());
        for (ResponderConfig responder : request.mRttPeers) {
            if (isCalledFromPrivilegedContext || responder.supports80211mc) {
                RttConfig config = new RttConfig();
                System.arraycopy(responder.macAddress.toByteArray(), 0, config.addr, 0, config.addr.length);
                try {
                    boolean z = true;
                    config.type = responder.supports80211mc ? 2 : 1;
                    if (config.type != 1 || cap == null || cap.rttOneSidedSupported) {
                        config.peer = halRttPeerTypeFromResponderType(responder.responderType);
                        config.channel.width = halChannelWidthFromResponderChannelWidth(responder.channelWidth);
                        config.channel.centerFreq = responder.frequency;
                        config.channel.centerFreq0 = responder.centerFreq0;
                        config.channel.centerFreq1 = responder.centerFreq1;
                        config.bw = halRttChannelBandwidthFromResponderChannelWidth(responder.channelWidth);
                        config.preamble = halRttPreambleFromResponderPreamble(responder.preamble);
                        if (config.peer == 5) {
                            config.mustRequestLci = false;
                            config.mustRequestLcr = false;
                            config.burstPeriod = 0;
                            config.numBurst = 0;
                            config.numFramesPerBurst = 5;
                            config.numRetriesPerRttFrame = 0;
                            config.numRetriesPerFtmr = 3;
                            config.burstDuration = 9;
                        } else {
                            config.mustRequestLci = isCalledFromPrivilegedContext;
                            config.mustRequestLcr = isCalledFromPrivilegedContext;
                            config.burstPeriod = 0;
                            config.numBurst = 0;
                            config.numFramesPerBurst = 8;
                            config.numRetriesPerRttFrame = config.type == 2 ? 0 : 3;
                            config.numRetriesPerFtmr = 3;
                            config.burstDuration = 9;
                            if (cap != null) {
                                boolean z2 = config.mustRequestLci && cap.lciSupported;
                                config.mustRequestLci = z2;
                                if (!config.mustRequestLcr || !cap.lcrSupported) {
                                    z = false;
                                }
                                config.mustRequestLcr = z;
                                config.bw = halRttChannelBandwidthCapabilityLimiter(config.bw, cap);
                                config.preamble = halRttPreambleCapabilityLimiter(config.preamble, cap);
                            }
                        }
                        rttConfigs.add(config);
                    } else {
                        Log.w(TAG, "Device does not support one-sided RTT");
                    }
                } catch (IllegalArgumentException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid configuration: ");
                    stringBuilder.append(e.getMessage());
                    Log.e(str, stringBuilder.toString());
                }
            } else {
                Log.e(TAG, "Invalid responder: does not support 802.11mc");
            }
        }
        return rttConfigs;
    }

    private static int halRttPeerTypeFromResponderType(int responderType) {
        switch (responderType) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            case 4:
                return 5;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("halRttPeerTypeFromResponderType: bad ");
                stringBuilder.append(responderType);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private static int halChannelWidthFromResponderChannelWidth(int responderChannelWidth) {
        switch (responderChannelWidth) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("halChannelWidthFromResponderChannelWidth: bad ");
                stringBuilder.append(responderChannelWidth);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private static int halRttChannelBandwidthFromResponderChannelWidth(int responderChannelWidth) {
        switch (responderChannelWidth) {
            case 0:
                return 4;
            case 1:
                return 8;
            case 2:
                return 16;
            case 3:
                return 32;
            case 4:
                return 32;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("halRttChannelBandwidthFromHalBandwidth: bad ");
                stringBuilder.append(responderChannelWidth);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private static int halRttPreambleFromResponderPreamble(int responderPreamble) {
        switch (responderPreamble) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 4;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("halRttPreambleFromResponderPreamble: bad ");
                stringBuilder.append(responderPreamble);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private static int halRttChannelBandwidthCapabilityLimiter(int halRttChannelBandwidth, RttCapabilities cap) {
        while (halRttChannelBandwidth != 0 && (cap.bwSupport & halRttChannelBandwidth) == 0) {
            halRttChannelBandwidth >>= 1;
        }
        if (halRttChannelBandwidth != 0) {
            return halRttChannelBandwidth;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("RTT BW=");
        stringBuilder.append(halRttChannelBandwidth);
        stringBuilder.append(", not supported by device capabilities=");
        stringBuilder.append(cap);
        stringBuilder.append(" - and no supported alternative");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private static int halRttPreambleCapabilityLimiter(int halRttPreamble, RttCapabilities cap) {
        while (halRttPreamble != 0 && (cap.preambleSupport & halRttPreamble) == 0) {
            halRttPreamble >>= 1;
        }
        if (halRttPreamble != 0) {
            return halRttPreamble;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("RTT Preamble=");
        stringBuilder.append(halRttPreamble);
        stringBuilder.append(", not supported by device capabilities=");
        stringBuilder.append(cap);
        stringBuilder.append(" - and no supported alternative");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("RttNative:");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  mHalDeviceManager: ");
        stringBuilder.append(this.mHalDeviceManager);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mIWifiRttController: ");
        stringBuilder.append(this.mIWifiRttController);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mRttCapabilities: ");
        stringBuilder.append(this.mRttCapabilities);
        pw.println(stringBuilder.toString());
    }
}
