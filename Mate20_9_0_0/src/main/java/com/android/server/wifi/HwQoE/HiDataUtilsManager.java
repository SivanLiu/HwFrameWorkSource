package com.android.server.wifi.HwQoE;

import android.content.Context;
import android.net.wifi.RssiPacketCountInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.server.wifi.HwArpVerifier;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.WifiNative.TxPacketCounters;
import java.util.List;

public class HiDataUtilsManager {
    public static final String TAG = "HiData_WiFiInfoManager";
    private static HiDataUtilsManager mHiDataUtilsManager;
    private boolean isRegisterListener = false;
    PhoneStateListener listenerSim0 = new PhoneStateListener(Integer.valueOf(0)) {
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            HiDataUtilsManager.this.mSignalStrengthSim0 = signalStrength;
        }

        public void onDataConnectionStateChanged(int state, int networkType) {
        }

        public void onServiceStateChanged(ServiceState serviceState) {
        }
    };
    PhoneStateListener listenerSim1 = new PhoneStateListener(Integer.valueOf(1)) {
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            HiDataUtilsManager.this.mSignalStrengthSim1 = signalStrength;
        }

        public void onDataConnectionStateChanged(int state, int networkType) {
        }

        public void onServiceStateChanged(ServiceState serviceState) {
        }
    };
    private Context mContext;
    private int mEvents = 321;
    private SignalStrength mSignalStrengthSim0;
    private SignalStrength mSignalStrengthSim1;
    private TelephonyManager mTelephonyManager;
    private WifiManager mWifiManager;
    private WifiNative mWifiNative;

    private HiDataUtilsManager(Context context) {
        this.mContext = context;
        this.mWifiNative = WifiInjector.getInstance().getWifiNative();
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
    }

    public static synchronized HiDataUtilsManager getInstance(Context context) {
        HiDataUtilsManager hiDataUtilsManager;
        synchronized (HiDataUtilsManager.class) {
            if (mHiDataUtilsManager == null) {
                mHiDataUtilsManager = new HiDataUtilsManager(context);
            }
            hiDataUtilsManager = mHiDataUtilsManager;
        }
        return hiDataUtilsManager;
    }

    public RssiPacketCountInfo getOTAInfo() {
        RssiPacketCountInfo info = new RssiPacketCountInfo();
        TxPacketCounters counters = this.mWifiNative.getTxPacketCounters(this.mWifiNative.getClientInterfaceName());
        if (counters != null) {
            info.txgood = counters.txSucceeded;
            info.txbad = counters.txFailed;
        } else {
            info.txgood = 0;
            info.txbad = 0;
        }
        return info;
    }

    public long checkARPRTT() {
        long timestamp = -1;
        HwArpVerifier mHwArpVerifier = HwArpVerifier.getDefault();
        if (mHwArpVerifier != null) {
            long startTimestamp = System.currentTimeMillis();
            boolean result = mHwArpVerifier.doArpTest(1, 1, true, false);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getARPRtt result = ");
            stringBuilder.append(result);
            HwQoEUtils.logD(stringBuilder.toString());
            if (!result) {
                return -1;
            }
            timestamp = System.currentTimeMillis() - startTimestamp;
        }
        return timestamp;
    }

    public int getOTASnr() {
        return this.mWifiManager.getConnectionInfo().getSnr();
    }

    public int getOTAChannelLoad() {
        return this.mWifiManager.getConnectionInfo().getChload();
    }

    public int getOTANoise() {
        return this.mWifiManager.getConnectionInfo().getNoise();
    }

    private boolean isValid(WifiConfiguration config) {
        boolean z = false;
        if (config == null) {
            return false;
        }
        if (config.allowedKeyManagement.cardinality() <= 1) {
            z = true;
        }
        return z;
    }

    private int getAuthType(int networkId) {
        List<WifiConfiguration> configs = this.mWifiManager.getConfiguredNetworks();
        if (configs == null || configs.size() == 0) {
            return -1;
        }
        for (WifiConfiguration config : configs) {
            if (config != null && isValid(config) && networkId == config.networkId) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getAuthType  networkId= ");
                stringBuilder.append(networkId);
                stringBuilder.append(" config.getAuthType() = ");
                stringBuilder.append(config.getAuthType());
                HwQoEUtils.logD(stringBuilder.toString());
                return config.getAuthType();
            }
        }
        return -1;
    }

    public boolean isPublicAP() {
        int type = getAuthType(this.mWifiManager.getConnectionInfo().getNetworkId());
        if (type == 1 || type == 4) {
            return false;
        }
        return true;
    }

    public void registerListener() {
        this.isRegisterListener = true;
        this.mTelephonyManager.listen(this.listenerSim0, this.mEvents);
        this.mTelephonyManager.listen(this.listenerSim1, this.mEvents);
    }

    public boolean isMobileNetworkReady(int type) {
        if (!this.isRegisterListener) {
            return false;
        }
        int subId = SubscriptionManager.getDefaultDataSubscriptionId();
        if (this.mTelephonyManager.isNetworkRoaming(subId)) {
            HwQoEUtils.logE("isMobileMatch user is roaming");
            return false;
        }
        SignalStrength signalStrength;
        int net_type = this.mTelephonyManager.getNetworkType(subId);
        TelephonyManager telephonyManager = this.mTelephonyManager;
        int RAT_class = TelephonyManager.getNetworkClass(net_type);
        if (subId == 0) {
            signalStrength = this.mSignalStrengthSim0;
        } else {
            signalStrength = this.mSignalStrengthSim1;
        }
        if (1 == RAT_class) {
            return false;
        }
        int ecio;
        StringBuilder stringBuilder;
        if (2 == RAT_class) {
            if (type != 2) {
                return false;
            }
            ecio = -1;
            if (net_type != 15) {
                switch (net_type) {
                    case 3:
                        break;
                    case 4:
                        ecio = signalStrength.getCdmaEcio();
                        break;
                    case 5:
                    case 6:
                        ecio = signalStrength.getEvdoEcio();
                        break;
                    default:
                        switch (net_type) {
                            case 8:
                            case 9:
                            case 10:
                                break;
                        }
                        break;
                }
            }
            ecio = signalStrength.getWcdmaEcio();
            stringBuilder = new StringBuilder();
            stringBuilder.append("isMobileMatch 3G getLevel = ");
            stringBuilder.append(signalStrength.getLevel());
            stringBuilder.append(" ecio = ");
            stringBuilder.append(ecio);
            HwQoEUtils.logD(stringBuilder.toString());
            if (ecio == -1) {
                if (signalStrength.getLevel() >= 3) {
                    return true;
                }
            } else if (signalStrength.getLevel() < 2 || ecio <= -105) {
                return false;
            } else {
                return true;
            }
            return false;
        } else if (3 == RAT_class) {
            int rsrq = signalStrength.getLteRsrp();
            ecio = signalStrength.getLteRssnr();
            stringBuilder = new StringBuilder();
            stringBuilder.append("isMobileMatch LTE getLevel = ");
            stringBuilder.append(signalStrength.getLevel());
            stringBuilder.append(" rsrq = ");
            stringBuilder.append(rsrq);
            stringBuilder.append(" sinr = ");
            stringBuilder.append(ecio);
            HwQoEUtils.logD(stringBuilder.toString());
            if (signalStrength.getLevel() >= 2) {
                return true;
            }
            if (signalStrength.getLevel() != 1 || rsrq <= -12 || ecio <= 11) {
                return false;
            }
            return true;
        } else {
            HwQoEUtils.logE("isMobileMatch unkown RAT!");
            return false;
        }
    }
}
