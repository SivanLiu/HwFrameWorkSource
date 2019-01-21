package com.android.server.wifi.aware;

import android.hardware.wifi.V1_0.NanBandSpecificConfig;
import android.hardware.wifi.V1_0.NanConfigRequest;
import android.hardware.wifi.V1_0.NanDiscoveryCommonConfig;
import android.hardware.wifi.V1_0.NanEnableRequest;
import android.hardware.wifi.V1_0.NanInitiateDataPathRequest;
import android.hardware.wifi.V1_0.NanPublishRequest;
import android.hardware.wifi.V1_0.NanRespondToDataPathIndicationRequest;
import android.hardware.wifi.V1_0.NanSubscribeRequest;
import android.hardware.wifi.V1_0.NanTransmitFollowupRequest;
import android.hardware.wifi.V1_0.WifiStatus;
import android.hardware.wifi.V1_2.IWifiNanIface;
import android.hardware.wifi.V1_2.NanConfigRequestSupplemental;
import android.net.wifi.aware.ConfigRequest;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.SubscribeConfig;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.util.Log;
import android.util.SparseIntArray;
import com.android.server.wifi.aware.WifiAwareShellCommand.DelegatedShellCommand;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import libcore.util.HexEncoding;

public class WifiAwareNativeApi implements DelegatedShellCommand {
    static final String PARAM_DISCOVERY_BEACON_INTERVAL_MS = "disc_beacon_interval_ms";
    private static final int PARAM_DISCOVERY_BEACON_INTERVAL_MS_DEFAULT = 0;
    private static final int PARAM_DISCOVERY_BEACON_INTERVAL_MS_IDLE = 0;
    private static final int PARAM_DISCOVERY_BEACON_INTERVAL_MS_INACTIVE = 0;
    static final String PARAM_DW_24GHZ = "dw_24ghz";
    private static final int PARAM_DW_24GHZ_DEFAULT = -1;
    private static final int PARAM_DW_24GHZ_IDLE = 4;
    private static final int PARAM_DW_24GHZ_INACTIVE = 4;
    static final String PARAM_DW_5GHZ = "dw_5ghz";
    private static final int PARAM_DW_5GHZ_DEFAULT = -1;
    private static final int PARAM_DW_5GHZ_IDLE = 0;
    private static final int PARAM_DW_5GHZ_INACTIVE = 0;
    static final String PARAM_ENABLE_DW_EARLY_TERM = "enable_dw_early_term";
    private static final int PARAM_ENABLE_DW_EARLY_TERM_DEFAULT = 0;
    private static final int PARAM_ENABLE_DW_EARLY_TERM_IDLE = 0;
    private static final int PARAM_ENABLE_DW_EARLY_TERM_INACTIVE = 0;
    static final String PARAM_MAC_RANDOM_INTERVAL_SEC = "mac_random_interval_sec";
    private static final int PARAM_MAC_RANDOM_INTERVAL_SEC_DEFAULT = 1800;
    static final String PARAM_NUM_SS_IN_DISCOVERY = "num_ss_in_discovery";
    private static final int PARAM_NUM_SS_IN_DISCOVERY_DEFAULT = 0;
    private static final int PARAM_NUM_SS_IN_DISCOVERY_IDLE = 0;
    private static final int PARAM_NUM_SS_IN_DISCOVERY_INACTIVE = 0;
    static final String POWER_PARAM_DEFAULT_KEY = "default";
    static final String POWER_PARAM_IDLE_KEY = "idle";
    static final String POWER_PARAM_INACTIVE_KEY = "inactive";
    private static final String SERVICE_NAME_FOR_OOB_DATA_PATH = "Wi-Fi Aware Data Path";
    private static final String TAG = "WifiAwareNativeApi";
    private static final boolean VDBG = false;
    boolean mDbg = false;
    private final WifiAwareNativeManager mHal;
    private Map<String, Integer> mSettableParameters = new HashMap();
    private Map<String, Map<String, Integer>> mSettablePowerParameters = new HashMap();
    private SparseIntArray mTransactionIds;

    public WifiAwareNativeApi(WifiAwareNativeManager wifiAwareNativeManager) {
        this.mHal = wifiAwareNativeManager;
        onReset();
    }

    private void recordTransactionId(int transactionId) {
    }

    public IWifiNanIface mockableCastTo_1_2(android.hardware.wifi.V1_0.IWifiNanIface iface) {
        return IWifiNanIface.castFrom(iface);
    }

    /* JADX WARNING: Removed duplicated region for block: B:22:0x004f  */
    /* JADX WARNING: Removed duplicated region for block: B:55:0x0197  */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x0103  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x00c9  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x0055  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x004f  */
    /* JADX WARNING: Removed duplicated region for block: B:55:0x0197  */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x0103  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x00c9  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x0055  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x004f  */
    /* JADX WARNING: Removed duplicated region for block: B:55:0x0197  */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x0103  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x00c9  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x0055  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x004f  */
    /* JADX WARNING: Removed duplicated region for block: B:55:0x0197  */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x0103  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x00c9  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x0055  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int onCommand(ShellCommand parentShell) {
        PrintWriter pw = parentShell.getErrPrintWriter();
        String subCmd = parentShell.getNextArgRequired();
        int hashCode = subCmd.hashCode();
        if (hashCode == -502265894) {
            if (subCmd.equals("set-power")) {
                hashCode = 1;
                switch (hashCode) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == -287648818) {
            if (subCmd.equals("get-power")) {
                hashCode = 3;
                switch (hashCode) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 102230) {
            if (subCmd.equals("get")) {
                hashCode = 2;
                switch (hashCode) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 113762 && subCmd.equals("set")) {
            hashCode = 0;
            String name;
            String valueStr;
            StringBuilder stringBuilder;
            StringBuilder stringBuilder2;
            switch (hashCode) {
                case 0:
                    name = parentShell.getNextArgRequired();
                    if (this.mSettableParameters.containsKey(name)) {
                        valueStr = parentShell.getNextArgRequired();
                        try {
                            this.mSettableParameters.put(name, Integer.valueOf(Integer.valueOf(valueStr).intValue()));
                            return 0;
                        } catch (NumberFormatException e) {
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Can't convert value to integer -- '");
                            stringBuilder3.append(valueStr);
                            stringBuilder3.append("'");
                            pw.println(stringBuilder3.toString());
                            return -1;
                        }
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown parameter name -- '");
                    stringBuilder.append(name);
                    stringBuilder.append("'");
                    pw.println(stringBuilder.toString());
                    return -1;
                case 1:
                    name = parentShell.getNextArgRequired();
                    valueStr = parentShell.getNextArgRequired();
                    String valueStr2 = parentShell.getNextArgRequired();
                    if (!this.mSettablePowerParameters.containsKey(name)) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unknown mode name -- '");
                        stringBuilder2.append(name);
                        stringBuilder2.append("'");
                        pw.println(stringBuilder2.toString());
                        return -1;
                    } else if (((Map) this.mSettablePowerParameters.get(name)).containsKey(valueStr)) {
                        try {
                            ((Map) this.mSettablePowerParameters.get(name)).put(valueStr, Integer.valueOf(Integer.valueOf(valueStr2).intValue()));
                            return 0;
                        } catch (NumberFormatException e2) {
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Can't convert value to integer -- '");
                            stringBuilder4.append(valueStr2);
                            stringBuilder4.append("'");
                            pw.println(stringBuilder4.toString());
                            return -1;
                        }
                    } else {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unknown parameter name '");
                        stringBuilder2.append(valueStr);
                        stringBuilder2.append("' in mode '");
                        stringBuilder2.append(name);
                        stringBuilder2.append("'");
                        pw.println(stringBuilder2.toString());
                        return -1;
                    }
                case 2:
                    name = parentShell.getNextArgRequired();
                    if (this.mSettableParameters.containsKey(name)) {
                        parentShell.getOutPrintWriter().println(((Integer) this.mSettableParameters.get(name)).intValue());
                        return 0;
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown parameter name -- '");
                    stringBuilder.append(name);
                    stringBuilder.append("'");
                    pw.println(stringBuilder.toString());
                    return -1;
                case 3:
                    name = parentShell.getNextArgRequired();
                    valueStr = parentShell.getNextArgRequired();
                    if (!this.mSettablePowerParameters.containsKey(name)) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unknown mode -- '");
                        stringBuilder2.append(name);
                        stringBuilder2.append("'");
                        pw.println(stringBuilder2.toString());
                        return -1;
                    } else if (((Map) this.mSettablePowerParameters.get(name)).containsKey(valueStr)) {
                        parentShell.getOutPrintWriter().println(((Integer) ((Map) this.mSettablePowerParameters.get(name)).get(valueStr)).intValue());
                        return 0;
                    } else {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unknown parameter name -- '");
                        stringBuilder2.append(valueStr);
                        stringBuilder2.append("' in mode '");
                        stringBuilder2.append(name);
                        stringBuilder2.append("'");
                        pw.println(stringBuilder2.toString());
                        return -1;
                    }
                default:
                    pw.println("Unknown 'wifiaware native_api <cmd>'");
                    return -1;
            }
        }
        hashCode = -1;
        switch (hashCode) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            default:
                break;
        }
    }

    public void onReset() {
        Map<String, Integer> defaultMap = new HashMap();
        defaultMap.put(PARAM_DW_24GHZ, Integer.valueOf(-1));
        defaultMap.put(PARAM_DW_5GHZ, Integer.valueOf(-1));
        defaultMap.put(PARAM_DISCOVERY_BEACON_INTERVAL_MS, Integer.valueOf(0));
        defaultMap.put(PARAM_NUM_SS_IN_DISCOVERY, Integer.valueOf(0));
        defaultMap.put(PARAM_ENABLE_DW_EARLY_TERM, Integer.valueOf(0));
        Map<String, Integer> inactiveMap = new HashMap();
        inactiveMap.put(PARAM_DW_24GHZ, Integer.valueOf(4));
        inactiveMap.put(PARAM_DW_5GHZ, Integer.valueOf(0));
        inactiveMap.put(PARAM_DISCOVERY_BEACON_INTERVAL_MS, Integer.valueOf(0));
        inactiveMap.put(PARAM_NUM_SS_IN_DISCOVERY, Integer.valueOf(0));
        inactiveMap.put(PARAM_ENABLE_DW_EARLY_TERM, Integer.valueOf(0));
        Map<String, Integer> idleMap = new HashMap();
        idleMap.put(PARAM_DW_24GHZ, Integer.valueOf(4));
        idleMap.put(PARAM_DW_5GHZ, Integer.valueOf(0));
        idleMap.put(PARAM_DISCOVERY_BEACON_INTERVAL_MS, Integer.valueOf(0));
        idleMap.put(PARAM_NUM_SS_IN_DISCOVERY, Integer.valueOf(0));
        idleMap.put(PARAM_ENABLE_DW_EARLY_TERM, Integer.valueOf(0));
        this.mSettablePowerParameters.put("default", defaultMap);
        this.mSettablePowerParameters.put(POWER_PARAM_INACTIVE_KEY, inactiveMap);
        this.mSettablePowerParameters.put(POWER_PARAM_IDLE_KEY, idleMap);
        this.mSettableParameters.put(PARAM_MAC_RANDOM_INTERVAL_SEC, Integer.valueOf(PARAM_MAC_RANDOM_INTERVAL_SEC_DEFAULT));
    }

    public void onHelp(String command, ShellCommand parentShell) {
        PrintWriter pw = parentShell.getOutPrintWriter();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  ");
        stringBuilder.append(command);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("    set <name> <value>: sets named parameter to value. Names: ");
        stringBuilder.append(this.mSettableParameters.keySet());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("    set-power <mode> <name> <value>: sets named power parameter to value. Modes: ");
        stringBuilder.append(this.mSettablePowerParameters.keySet());
        stringBuilder.append(", Names: ");
        stringBuilder.append(((Map) this.mSettablePowerParameters.get("default")).keySet());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("    get <name>: gets named parameter value. Names: ");
        stringBuilder.append(this.mSettableParameters.keySet());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("    get-power <mode> <name>: gets named parameter value. Modes: ");
        stringBuilder.append(this.mSettablePowerParameters.keySet());
        stringBuilder.append(", Names: ");
        stringBuilder.append(((Map) this.mSettablePowerParameters.get("default")).keySet());
        pw.println(stringBuilder.toString());
    }

    public boolean getCapabilities(short transactionId) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getCapabilities: transactionId=");
            stringBuilder.append(transactionId);
            Log.v(str, stringBuilder.toString());
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "getCapabilities: null interface");
            return false;
        }
        String str2;
        StringBuilder stringBuilder2;
        try {
            WifiStatus status = iface.getCapabilitiesRequest(transactionId);
            if (status.code == 0) {
                return true;
            }
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getCapabilities: error: ");
            stringBuilder2.append(statusString(status));
            Log.e(str2, stringBuilder2.toString());
            return false;
        } catch (RemoteException e) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getCapabilities: exception: ");
            stringBuilder2.append(e);
            Log.e(str2, stringBuilder2.toString());
            return false;
        }
    }

    public boolean enableAndConfigure(short transactionId, ConfigRequest configRequest, boolean notifyIdentityChange, boolean initialConfiguration, boolean isInteractive, boolean isIdle) {
        StringBuilder stringBuilder;
        RemoteException e;
        short s = transactionId;
        ConfigRequest configRequest2 = configRequest;
        boolean z = notifyIdentityChange;
        boolean z2 = initialConfiguration;
        boolean z3 = isInteractive;
        boolean z4 = isIdle;
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("enableAndConfigure: transactionId=");
            stringBuilder2.append(s);
            stringBuilder2.append(", configRequest=");
            stringBuilder2.append(configRequest2);
            stringBuilder2.append(", notifyIdentityChange=");
            stringBuilder2.append(z);
            stringBuilder2.append(", initialConfiguration=");
            stringBuilder2.append(z2);
            stringBuilder2.append(", isInteractive=");
            stringBuilder2.append(z3);
            stringBuilder2.append(", isIdle=");
            stringBuilder2.append(z4);
            Log.v(str, stringBuilder2.toString());
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "enableAndConfigure: null interface");
            return false;
        }
        WifiStatus status;
        WifiStatus status2;
        IWifiNanIface iface12 = mockableCastTo_1_2(iface);
        NanConfigRequestSupplemental configSupplemental12 = new NanConfigRequestSupplemental();
        if (iface12 != null) {
            configSupplemental12.discoveryBeaconIntervalMs = 0;
            configSupplemental12.numberOfSpatialStreamsInDiscovery = 0;
            configSupplemental12.enableDiscoveryWindowEarlyTermination = false;
            configSupplemental12.enableRanging = true;
        }
        NanBandSpecificConfig config24;
        NanBandSpecificConfig config5;
        if (z2) {
            try {
                NanEnableRequest req = new NanEnableRequest();
                req.operateInBand[0] = true;
                req.operateInBand[1] = configRequest2.mSupport5gBand;
                req.hopCountMax = (byte) 2;
                req.configParams.masterPref = (byte) configRequest2.mMasterPreference;
                req.configParams.disableDiscoveryAddressChangeIndication = z ^ 1;
                req.configParams.disableStartedClusterIndication = z ^ 1;
                req.configParams.disableJoinedClusterIndication = z ^ 1;
                req.configParams.includePublishServiceIdsInBeacon = true;
                req.configParams.numberOfPublishServiceIdsInBeacon = (byte) 0;
                req.configParams.includeSubscribeServiceIdsInBeacon = true;
                req.configParams.numberOfSubscribeServiceIdsInBeacon = (byte) 0;
                req.configParams.rssiWindowSize = (short) 8;
                req.configParams.macAddressRandomizationIntervalSec = ((Integer) this.mSettableParameters.get(PARAM_MAC_RANDOM_INTERVAL_SEC)).intValue();
                config24 = new NanBandSpecificConfig();
                config24.rssiClose = (byte) 60;
                config24.rssiMiddle = (byte) 70;
                config24.rssiCloseProximity = (byte) 60;
                config24.dwellTimeMs = (byte) -56;
                config24.scanPeriodSec = (short) 20;
                if (configRequest2.mDiscoveryWindowInterval[0] == -1) {
                    config24.validDiscoveryWindowIntervalVal = false;
                } else {
                    config24.validDiscoveryWindowIntervalVal = true;
                    config24.discoveryWindowIntervalVal = (byte) configRequest2.mDiscoveryWindowInterval[0];
                }
                req.configParams.bandSpecificConfig[0] = config24;
                config5 = new NanBandSpecificConfig();
                config5.rssiClose = (byte) 60;
                config5.rssiMiddle = (byte) 75;
                config5.rssiCloseProximity = (byte) 60;
                config5.dwellTimeMs = (byte) -56;
                config5.scanPeriodSec = (short) 20;
                if (configRequest2.mDiscoveryWindowInterval[1] == -1) {
                    config5.validDiscoveryWindowIntervalVal = false;
                } else {
                    config5.validDiscoveryWindowIntervalVal = true;
                    config5.discoveryWindowIntervalVal = (byte) configRequest2.mDiscoveryWindowInterval[1];
                }
                req.configParams.bandSpecificConfig[1] = config5;
                req.debugConfigs.validClusterIdVals = true;
                req.debugConfigs.clusterIdTopRangeVal = (short) configRequest2.mClusterHigh;
                req.debugConfigs.clusterIdBottomRangeVal = (short) configRequest2.mClusterLow;
                req.debugConfigs.validIntfAddrVal = false;
                req.debugConfigs.validOuiVal = false;
                req.debugConfigs.ouiVal = 0;
                req.debugConfigs.validRandomFactorForceVal = false;
                req.debugConfigs.randomFactorForceVal = (byte) 0;
                req.debugConfigs.validHopCountForceVal = false;
                req.debugConfigs.hopCountForceVal = (byte) 0;
                req.debugConfigs.validDiscoveryChannelVal = false;
                req.debugConfigs.discoveryChannelMhzVal[0] = 0;
                req.debugConfigs.discoveryChannelMhzVal[1] = 0;
                req.debugConfigs.validUseBeaconsInBandVal = false;
                req.debugConfigs.useBeaconsInBandVal[0] = true;
                req.debugConfigs.useBeaconsInBandVal[1] = true;
                req.debugConfigs.validUseSdfInBandVal = false;
                req.debugConfigs.useSdfInBandVal[0] = true;
                req.debugConfigs.useSdfInBandVal[1] = true;
                updateConfigForPowerSettings(req.configParams, configSupplemental12, z3, z4);
                if (iface12 != null) {
                    status = iface12.enableRequest_1_2(s, req, configSupplemental12);
                } else {
                    status = iface.enableRequest(s, req);
                }
                status2 = status;
            } catch (RemoteException e2) {
                String str2 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("enableAndConfigure: exception: ");
                stringBuilder.append(e2);
                Log.e(str2, stringBuilder.toString());
                return false;
            }
        }
        int i;
        NanConfigRequest req2 = new NanConfigRequest();
        req2.masterPref = (byte) configRequest2.mMasterPreference;
        req2.disableDiscoveryAddressChangeIndication = z ^ 1;
        req2.disableStartedClusterIndication = z ^ 1;
        req2.disableJoinedClusterIndication = z ^ 1;
        req2.includePublishServiceIdsInBeacon = true;
        req2.numberOfPublishServiceIdsInBeacon = (byte) 0;
        req2.includeSubscribeServiceIdsInBeacon = true;
        req2.numberOfSubscribeServiceIdsInBeacon = (byte) 0;
        req2.rssiWindowSize = (short) 8;
        req2.macAddressRandomizationIntervalSec = ((Integer) this.mSettableParameters.get(PARAM_MAC_RANDOM_INTERVAL_SEC)).intValue();
        config24 = new NanBandSpecificConfig();
        config24.rssiClose = (byte) 60;
        config24.rssiMiddle = (byte) 70;
        config24.rssiCloseProximity = (byte) 60;
        config24.dwellTimeMs = (byte) -56;
        config24.scanPeriodSec = (short) 20;
        if (configRequest2.mDiscoveryWindowInterval[0] == -1) {
            config24.validDiscoveryWindowIntervalVal = false;
            i = 0;
        } else {
            config24.validDiscoveryWindowIntervalVal = true;
            i = 0;
            config24.discoveryWindowIntervalVal = (byte) configRequest2.mDiscoveryWindowInterval[0];
        }
        req2.bandSpecificConfig[i] = config24;
        config5 = new NanBandSpecificConfig();
        config5.rssiClose = (byte) 60;
        config5.rssiMiddle = (byte) 75;
        config5.rssiCloseProximity = (byte) 60;
        config5.dwellTimeMs = (byte) -56;
        config5.scanPeriodSec = (short) 20;
        if (configRequest2.mDiscoveryWindowInterval[1] == -1) {
            config5.validDiscoveryWindowIntervalVal = false;
        } else {
            config5.validDiscoveryWindowIntervalVal = true;
            config5.discoveryWindowIntervalVal = (byte) configRequest2.mDiscoveryWindowInterval[1];
        }
        req2.bandSpecificConfig[1] = config5;
        updateConfigForPowerSettings(req2, configSupplemental12, z3, z4);
        status2 = iface12 != null ? iface12.configRequest_1_2(s, req2, configSupplemental12) : iface.configRequest(s, req2);
        status = status2;
        if (status.code == 0) {
            return true;
        }
        e2 = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("enableAndConfigure: error: ");
        stringBuilder.append(statusString(status));
        Log.e(e2, stringBuilder.toString());
        return false;
    }

    public boolean disable(short transactionId) {
        if (this.mDbg) {
            Log.d(TAG, "disable");
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "disable: null interface");
            return false;
        }
        String str;
        StringBuilder stringBuilder;
        try {
            WifiStatus status = iface.disableRequest(transactionId);
            if (status.code == 0) {
                return true;
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("disable: error: ");
            stringBuilder.append(statusString(status));
            Log.e(str, stringBuilder.toString());
            return false;
        } catch (RemoteException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("disable: exception: ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    public boolean publish(short transactionId, byte publishId, PublishConfig publishConfig) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("publish: transactionId=");
            stringBuilder.append(transactionId);
            stringBuilder.append(", publishId=");
            stringBuilder.append(publishId);
            stringBuilder.append(", config=");
            stringBuilder.append(publishConfig);
            Log.d(str, stringBuilder.toString());
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "publish: null interface");
            return false;
        }
        NanPublishRequest req = new NanPublishRequest();
        req.baseConfigs.sessionId = publishId;
        req.baseConfigs.ttlSec = (short) publishConfig.mTtlSec;
        req.baseConfigs.discoveryWindowPeriod = (short) 1;
        req.baseConfigs.discoveryCount = (byte) 0;
        convertNativeByteArrayToArrayList(publishConfig.mServiceName, req.baseConfigs.serviceName);
        req.baseConfigs.discoveryMatchIndicator = 2;
        convertNativeByteArrayToArrayList(publishConfig.mServiceSpecificInfo, req.baseConfigs.serviceSpecificInfo);
        convertNativeByteArrayToArrayList(publishConfig.mMatchFilter, publishConfig.mPublishType == 0 ? req.baseConfigs.txMatchFilter : req.baseConfigs.rxMatchFilter);
        req.baseConfigs.useRssiThreshold = false;
        req.baseConfigs.disableDiscoveryTerminationIndication = publishConfig.mEnableTerminateNotification ^ 1;
        req.baseConfigs.disableMatchExpirationIndication = true;
        req.baseConfigs.disableFollowupReceivedIndication = false;
        req.autoAcceptDataPathRequests = false;
        req.baseConfigs.rangingRequired = publishConfig.mEnableRanging;
        req.baseConfigs.securityConfig.securityType = 0;
        req.publishType = publishConfig.mPublishType;
        req.txType = 0;
        String str2;
        StringBuilder stringBuilder2;
        try {
            WifiStatus status = iface.startPublishRequest(transactionId, req);
            if (status.code == 0) {
                return true;
            }
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("publish: error: ");
            stringBuilder2.append(statusString(status));
            Log.e(str2, stringBuilder2.toString());
            return false;
        } catch (RemoteException e) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("publish: exception: ");
            stringBuilder2.append(e);
            Log.e(str2, stringBuilder2.toString());
            return false;
        }
    }

    public boolean subscribe(short transactionId, byte subscribeId, SubscribeConfig subscribeConfig) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("subscribe: transactionId=");
            stringBuilder.append(transactionId);
            stringBuilder.append(", subscribeId=");
            stringBuilder.append(subscribeId);
            stringBuilder.append(", config=");
            stringBuilder.append(subscribeConfig);
            Log.d(str, stringBuilder.toString());
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "subscribe: null interface");
            return false;
        }
        NanSubscribeRequest req = new NanSubscribeRequest();
        req.baseConfigs.sessionId = subscribeId;
        req.baseConfigs.ttlSec = (short) subscribeConfig.mTtlSec;
        req.baseConfigs.discoveryWindowPeriod = (short) 1;
        req.baseConfigs.discoveryCount = (byte) 0;
        convertNativeByteArrayToArrayList(subscribeConfig.mServiceName, req.baseConfigs.serviceName);
        req.baseConfigs.discoveryMatchIndicator = 0;
        convertNativeByteArrayToArrayList(subscribeConfig.mServiceSpecificInfo, req.baseConfigs.serviceSpecificInfo);
        convertNativeByteArrayToArrayList(subscribeConfig.mMatchFilter, subscribeConfig.mSubscribeType == 1 ? req.baseConfigs.txMatchFilter : req.baseConfigs.rxMatchFilter);
        req.baseConfigs.useRssiThreshold = false;
        req.baseConfigs.disableDiscoveryTerminationIndication = subscribeConfig.mEnableTerminateNotification ^ 1;
        req.baseConfigs.disableMatchExpirationIndication = true;
        req.baseConfigs.disableFollowupReceivedIndication = false;
        NanDiscoveryCommonConfig nanDiscoveryCommonConfig = req.baseConfigs;
        boolean z = subscribeConfig.mMinDistanceMmSet || subscribeConfig.mMaxDistanceMmSet;
        nanDiscoveryCommonConfig.rangingRequired = z;
        req.baseConfigs.configRangingIndications = 0;
        if (subscribeConfig.mMinDistanceMmSet) {
            req.baseConfigs.distanceEgressCm = (short) Math.min(subscribeConfig.mMinDistanceMm / 10, 32767);
            nanDiscoveryCommonConfig = req.baseConfigs;
            nanDiscoveryCommonConfig.configRangingIndications |= 4;
        }
        if (subscribeConfig.mMaxDistanceMmSet) {
            req.baseConfigs.distanceIngressCm = (short) Math.min(subscribeConfig.mMaxDistanceMm / 10, 32767);
            nanDiscoveryCommonConfig = req.baseConfigs;
            nanDiscoveryCommonConfig.configRangingIndications |= 2;
        }
        req.baseConfigs.securityConfig.securityType = 0;
        req.subscribeType = subscribeConfig.mSubscribeType;
        String str2;
        StringBuilder stringBuilder2;
        try {
            WifiStatus status = iface.startSubscribeRequest(transactionId, req);
            if (status.code == 0) {
                return true;
            }
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("subscribe: error: ");
            stringBuilder2.append(statusString(status));
            Log.e(str2, stringBuilder2.toString());
            return false;
        } catch (RemoteException e) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("subscribe: exception: ");
            stringBuilder2.append(e);
            Log.e(str2, stringBuilder2.toString());
            return false;
        }
    }

    public boolean sendMessage(short transactionId, byte pubSubId, int requestorInstanceId, byte[] dest, byte[] message, int messageId) {
        if (this.mDbg) {
            Object obj;
            int i;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendMessage: transactionId=");
            stringBuilder.append(transactionId);
            stringBuilder.append(", pubSubId=");
            stringBuilder.append(pubSubId);
            stringBuilder.append(", requestorInstanceId=");
            stringBuilder.append(requestorInstanceId);
            stringBuilder.append(", dest=");
            stringBuilder.append(String.valueOf(HexEncoding.encode(dest)));
            stringBuilder.append(", messageId=");
            stringBuilder.append(messageId);
            stringBuilder.append(", message=");
            if (message == null) {
                obj = "<null>";
            } else {
                obj = HexEncoding.encode(message);
            }
            stringBuilder.append(obj);
            stringBuilder.append(", message.length=");
            if (message == null) {
                i = 0;
            } else {
                i = message.length;
            }
            stringBuilder.append(i);
            Log.d(str, stringBuilder.toString());
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "sendMessage: null interface");
            return false;
        }
        NanTransmitFollowupRequest req = new NanTransmitFollowupRequest();
        req.discoverySessionId = pubSubId;
        req.peerId = requestorInstanceId;
        copyArray(dest, req.addr);
        req.isHighPriority = false;
        req.shouldUseDiscoveryWindow = true;
        convertNativeByteArrayToArrayList(message, req.serviceSpecificInfo);
        req.disableFollowupResultIndication = false;
        StringBuilder stringBuilder2;
        try {
            WifiStatus status = iface.transmitFollowupRequest(transactionId, req);
            if (status.code == 0) {
                return true;
            }
            String str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("sendMessage: error: ");
            stringBuilder2.append(statusString(status));
            Log.e(str2, stringBuilder2.toString());
            return false;
        } catch (RemoteException e) {
            String str3 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("sendMessage: exception: ");
            stringBuilder2.append(e);
            Log.e(str3, stringBuilder2.toString());
            return false;
        }
    }

    public boolean stopPublish(short transactionId, byte pubSubId) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("stopPublish: transactionId=");
            stringBuilder.append(transactionId);
            stringBuilder.append(", pubSubId=");
            stringBuilder.append(pubSubId);
            Log.d(str, stringBuilder.toString());
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "stopPublish: null interface");
            return false;
        }
        String str2;
        StringBuilder stringBuilder2;
        try {
            WifiStatus status = iface.stopPublishRequest(transactionId, pubSubId);
            if (status.code == 0) {
                return true;
            }
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("stopPublish: error: ");
            stringBuilder2.append(statusString(status));
            Log.e(str2, stringBuilder2.toString());
            return false;
        } catch (RemoteException e) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("stopPublish: exception: ");
            stringBuilder2.append(e);
            Log.e(str2, stringBuilder2.toString());
            return false;
        }
    }

    public boolean stopSubscribe(short transactionId, byte pubSubId) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("stopSubscribe: transactionId=");
            stringBuilder.append(transactionId);
            stringBuilder.append(", pubSubId=");
            stringBuilder.append(pubSubId);
            Log.d(str, stringBuilder.toString());
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "stopSubscribe: null interface");
            return false;
        }
        String str2;
        StringBuilder stringBuilder2;
        try {
            WifiStatus status = iface.stopSubscribeRequest(transactionId, pubSubId);
            if (status.code == 0) {
                return true;
            }
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("stopSubscribe: error: ");
            stringBuilder2.append(statusString(status));
            Log.e(str2, stringBuilder2.toString());
            return false;
        } catch (RemoteException e) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("stopSubscribe: exception: ");
            stringBuilder2.append(e);
            Log.e(str2, stringBuilder2.toString());
            return false;
        }
    }

    public boolean createAwareNetworkInterface(short transactionId, String interfaceName) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("createAwareNetworkInterface: transactionId=");
            stringBuilder.append(transactionId);
            stringBuilder.append(", interfaceName=");
            stringBuilder.append(interfaceName);
            Log.v(str, stringBuilder.toString());
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "createAwareNetworkInterface: null interface");
            return false;
        }
        String str2;
        StringBuilder stringBuilder2;
        try {
            WifiStatus status = iface.createDataInterfaceRequest(transactionId, interfaceName);
            if (status.code == 0) {
                return true;
            }
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("createAwareNetworkInterface: error: ");
            stringBuilder2.append(statusString(status));
            Log.e(str2, stringBuilder2.toString());
            return false;
        } catch (RemoteException e) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("createAwareNetworkInterface: exception: ");
            stringBuilder2.append(e);
            Log.e(str2, stringBuilder2.toString());
            return false;
        }
    }

    public boolean deleteAwareNetworkInterface(short transactionId, String interfaceName) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("deleteAwareNetworkInterface: transactionId=");
            stringBuilder.append(transactionId);
            stringBuilder.append(", interfaceName=");
            stringBuilder.append(interfaceName);
            Log.v(str, stringBuilder.toString());
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "deleteAwareNetworkInterface: null interface");
            return false;
        }
        String str2;
        StringBuilder stringBuilder2;
        try {
            WifiStatus status = iface.deleteDataInterfaceRequest(transactionId, interfaceName);
            if (status.code == 0) {
                return true;
            }
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("deleteAwareNetworkInterface: error: ");
            stringBuilder2.append(statusString(status));
            Log.e(str2, stringBuilder2.toString());
            return false;
        } catch (RemoteException e) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("deleteAwareNetworkInterface: exception: ");
            stringBuilder2.append(e);
            Log.e(str2, stringBuilder2.toString());
            return false;
        }
    }

    public boolean initiateDataPath(short transactionId, int peerId, int channelRequestType, int channel, byte[] peer, String interfaceName, byte[] pmk, String passphrase, boolean isOutOfBand, Capabilities capabilities) {
        String str;
        short s = transactionId;
        int i = peerId;
        int i2 = channelRequestType;
        int i3 = channel;
        String str2 = interfaceName;
        byte[] bArr = pmk;
        Capabilities capabilities2 = capabilities;
        if (this.mDbg) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("initiateDataPath: transactionId=");
            stringBuilder.append(s);
            stringBuilder.append(", peerId=");
            stringBuilder.append(i);
            stringBuilder.append(", channelRequestType=");
            stringBuilder.append(i2);
            stringBuilder.append(", channel=");
            stringBuilder.append(i3);
            stringBuilder.append(", peer=");
            stringBuilder.append(String.valueOf(HexEncoding.encode(peer)));
            stringBuilder.append(", interfaceName=");
            stringBuilder.append(str2);
            Log.v(str, stringBuilder.toString());
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "initiateDataPath: null interface");
            return false;
        } else if (capabilities2 == null) {
            Log.e(TAG, "initiateDataPath: null capabilities");
            return false;
        } else {
            NanInitiateDataPathRequest req = new NanInitiateDataPathRequest();
            req.peerId = i;
            copyArray(peer, req.peerDiscMacAddr);
            req.channelRequestType = i2;
            req.channel = i3;
            req.ifaceName = str2;
            req.securityConfig.securityType = 0;
            if (!(bArr == null || bArr.length == 0)) {
                req.securityConfig.cipherType = getStrongestCipherSuiteType(capabilities2.supportedCipherSuites);
                req.securityConfig.securityType = 1;
                copyArray(bArr, req.securityConfig.pmk);
            }
            if (!(passphrase == null || passphrase.length() == 0)) {
                req.securityConfig.cipherType = getStrongestCipherSuiteType(capabilities2.supportedCipherSuites);
                req.securityConfig.securityType = 2;
                convertNativeByteArrayToArrayList(passphrase.getBytes(), req.securityConfig.passphrase);
            }
            if (req.securityConfig.securityType != 0 && isOutOfBand) {
                convertNativeByteArrayToArrayList(SERVICE_NAME_FOR_OOB_DATA_PATH.getBytes(StandardCharsets.UTF_8), req.serviceNameOutOfBand);
            }
            try {
                WifiStatus status = iface.initiateDataPathRequest(s, req);
                if (status.code == 0) {
                    return true;
                }
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("initiateDataPath: error: ");
                stringBuilder2.append(statusString(status));
                Log.e(str, stringBuilder2.toString());
                return false;
            } catch (RemoteException e) {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("initiateDataPath: exception: ");
                stringBuilder3.append(e);
                Log.e(str3, stringBuilder3.toString());
                return false;
            }
        }
    }

    public boolean respondToDataPathRequest(short transactionId, boolean accept, int ndpId, String interfaceName, byte[] pmk, String passphrase, boolean isOutOfBand, Capabilities capabilities) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("respondToDataPathRequest: transactionId=");
            stringBuilder.append(transactionId);
            stringBuilder.append(", accept=");
            stringBuilder.append(accept);
            stringBuilder.append(", int ndpId=");
            stringBuilder.append(ndpId);
            stringBuilder.append(", interfaceName=");
            stringBuilder.append(interfaceName);
            Log.v(str, stringBuilder.toString());
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "respondToDataPathRequest: null interface");
            return false;
        } else if (capabilities == null) {
            Log.e(TAG, "initiateDataPath: null capabilities");
            return false;
        } else {
            NanRespondToDataPathIndicationRequest req = new NanRespondToDataPathIndicationRequest();
            req.acceptRequest = accept;
            req.ndpInstanceId = ndpId;
            req.ifaceName = interfaceName;
            req.securityConfig.securityType = 0;
            if (!(pmk == null || pmk.length == 0)) {
                req.securityConfig.cipherType = getStrongestCipherSuiteType(capabilities.supportedCipherSuites);
                req.securityConfig.securityType = 1;
                copyArray(pmk, req.securityConfig.pmk);
            }
            if (!(passphrase == null || passphrase.length() == 0)) {
                req.securityConfig.cipherType = getStrongestCipherSuiteType(capabilities.supportedCipherSuites);
                req.securityConfig.securityType = 2;
                convertNativeByteArrayToArrayList(passphrase.getBytes(), req.securityConfig.passphrase);
            }
            if (req.securityConfig.securityType != 0 && isOutOfBand) {
                convertNativeByteArrayToArrayList(SERVICE_NAME_FOR_OOB_DATA_PATH.getBytes(StandardCharsets.UTF_8), req.serviceNameOutOfBand);
            }
            StringBuilder stringBuilder2;
            try {
                WifiStatus status = iface.respondToDataPathIndicationRequest(transactionId, req);
                if (status.code == 0) {
                    return true;
                }
                String str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("respondToDataPathRequest: error: ");
                stringBuilder2.append(statusString(status));
                Log.e(str2, stringBuilder2.toString());
                return false;
            } catch (RemoteException e) {
                String str3 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("respondToDataPathRequest: exception: ");
                stringBuilder2.append(e);
                Log.e(str3, stringBuilder2.toString());
                return false;
            }
        }
    }

    public boolean endDataPath(short transactionId, int ndpId) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("endDataPath: transactionId=");
            stringBuilder.append(transactionId);
            stringBuilder.append(", ndpId=");
            stringBuilder.append(ndpId);
            Log.v(str, stringBuilder.toString());
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "endDataPath: null interface");
            return false;
        }
        String str2;
        StringBuilder stringBuilder2;
        try {
            WifiStatus status = iface.terminateDataPathRequest(transactionId, ndpId);
            if (status.code == 0) {
                return true;
            }
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("endDataPath: error: ");
            stringBuilder2.append(statusString(status));
            Log.e(str2, stringBuilder2.toString());
            return false;
        } catch (RemoteException e) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("endDataPath: exception: ");
            stringBuilder2.append(e);
            Log.e(str2, stringBuilder2.toString());
            return false;
        }
    }

    private void updateConfigForPowerSettings(NanConfigRequest req, NanConfigRequestSupplemental configSupplemental12, boolean isInteractive, boolean isIdle) {
        String key = "default";
        if (isIdle) {
            key = POWER_PARAM_IDLE_KEY;
        } else if (!isInteractive) {
            key = POWER_PARAM_INACTIVE_KEY;
        }
        boolean z = true;
        updateSingleConfigForPowerSettings(req.bandSpecificConfig[1], ((Integer) ((Map) this.mSettablePowerParameters.get(key)).get(PARAM_DW_5GHZ)).intValue());
        updateSingleConfigForPowerSettings(req.bandSpecificConfig[0], ((Integer) ((Map) this.mSettablePowerParameters.get(key)).get(PARAM_DW_24GHZ)).intValue());
        configSupplemental12.discoveryBeaconIntervalMs = ((Integer) ((Map) this.mSettablePowerParameters.get(key)).get(PARAM_DISCOVERY_BEACON_INTERVAL_MS)).intValue();
        configSupplemental12.numberOfSpatialStreamsInDiscovery = ((Integer) ((Map) this.mSettablePowerParameters.get(key)).get(PARAM_NUM_SS_IN_DISCOVERY)).intValue();
        if (((Integer) ((Map) this.mSettablePowerParameters.get(key)).get(PARAM_ENABLE_DW_EARLY_TERM)).intValue() == 0) {
            z = false;
        }
        configSupplemental12.enableDiscoveryWindowEarlyTermination = z;
    }

    private void updateSingleConfigForPowerSettings(NanBandSpecificConfig cfg, int override) {
        if (override != -1) {
            cfg.validDiscoveryWindowIntervalVal = true;
            cfg.discoveryWindowIntervalVal = (byte) override;
        }
    }

    private int getStrongestCipherSuiteType(int supportedCipherSuites) {
        if ((supportedCipherSuites & 2) != 0) {
            return 2;
        }
        if ((supportedCipherSuites & 1) != 0) {
            return 1;
        }
        return 0;
    }

    private ArrayList<Byte> convertNativeByteArrayToArrayList(byte[] from, ArrayList<Byte> to) {
        int i = 0;
        if (from == null) {
            from = new byte[0];
        }
        if (to == null) {
            to = new ArrayList(from.length);
        } else {
            to.ensureCapacity(from.length);
        }
        while (i < from.length) {
            to.add(Byte.valueOf(from[i]));
            i++;
        }
        return to;
    }

    private void copyArray(byte[] from, byte[] to) {
        if (from == null || to == null || from.length != to.length) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("copyArray error: from=");
            stringBuilder.append(from);
            stringBuilder.append(", to=");
            stringBuilder.append(to);
            Log.e(str, stringBuilder.toString());
            return;
        }
        for (int i = 0; i < from.length; i++) {
            to[i] = from[i];
        }
    }

    private static String statusString(WifiStatus status) {
        if (status == null) {
            return "status=null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(status.code);
        sb.append(" (");
        sb.append(status.description);
        sb.append(")");
        return sb.toString();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("WifiAwareNativeApi:");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  mSettableParameters: ");
        stringBuilder.append(this.mSettableParameters);
        pw.println(stringBuilder.toString());
        this.mHal.dump(fd, pw, args);
    }
}
