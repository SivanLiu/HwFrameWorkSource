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
    private SignalStrength mSignalStrengthSim0 = null;
    private SignalStrength mSignalStrengthSim1 = null;
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
        TxPacketCounters counters = this.mWifiNative.getTxPacketCounters();
        if (counters != null) {
            info.txgood = counters.txSucceeded;
            info.txbad = counters.txFailed;
        }
        return info;
    }

    public long checkARPRTT() {
        HwArpVerifier mHwArpVerifier = HwArpVerifier.getDefault();
        if (mHwArpVerifier == null) {
            return -1;
        }
        long timestamp = mHwArpVerifier.getGateWayArpRTT(200);
        HwQoEUtils.logD("checkARPRTT timestamp = " + timestamp);
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
        boolean z = true;
        if (config == null) {
            return false;
        }
        if (config.allowedKeyManagement.cardinality() > 1) {
            z = false;
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
                HwQoEUtils.logD("getAuthType  networkId= " + networkId + " config.getAuthType() = " + config.getAuthType());
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
        if (signalStrength == null || 1 == RAT_class) {
            return false;
        }
        if (2 == RAT_class) {
            if (type != 2) {
                return false;
            }
            int ecio = -1;
            switch (net_type) {
                case 3:
                case 8:
                case 9:
                case 10:
                case 15:
                    ecio = signalStrength.getWcdmaEcio();
                    break;
                case 4:
                    ecio = signalStrength.getCdmaEcio();
                    break;
                case 5:
                case 6:
                    ecio = signalStrength.getEvdoEcio();
                    break;
            }
            HwQoEUtils.logD("isMobileMatch 3G getLevel = " + signalStrength.getLevel() + " ecio = " + ecio);
            if (ecio != -1) {
                return signalStrength.getLevel() >= 2 && ecio > -105;
            } else {
                if (signalStrength.getLevel() >= 3) {
                    return true;
                }
            }
        } else if (3 == RAT_class) {
            int rsrq = signalStrength.getLteRsrp();
            int sinr = signalStrength.getLteRssnr();
            HwQoEUtils.logD("isMobileMatch LTE getLevel = " + signalStrength.getLevel() + " rsrq = " + rsrq + " sinr = " + sinr);
            if (signalStrength.getLevel() >= 2) {
                return true;
            }
            return signalStrength.getLevel() == 1 && rsrq > -12 && sinr > 11;
        } else {
            HwQoEUtils.logE("isMobileMatch unkown RAT!");
            return false;
        }
    }

    public void updateCellInfo(HiDataCHRStallInfo info) {
        if (this.isRegisterListener) {
            SignalStrength signalStrength;
            int subId = SubscriptionManager.getDefaultDataSubscriptionId();
            int net_type = this.mTelephonyManager.getNetworkType(subId);
            TelephonyManager telephonyManager = this.mTelephonyManager;
            int RAT_class = TelephonyManager.getNetworkClass(net_type);
            if (subId == 0) {
                signalStrength = this.mSignalStrengthSim0;
            } else {
                signalStrength = this.mSignalStrengthSim1;
            }
            if (1 == RAT_class) {
                info.mRAT = 1;
            } else if (2 == RAT_class) {
                info.mRAT = 2;
                info.mCellSig = signalStrength.getLevel();
            } else if (3 == RAT_class) {
                int rsrq = signalStrength.getLteRsrp();
                int sinr = signalStrength.getLteRssnr();
                info.mRAT = 3;
                info.mCellSig = signalStrength.getLevel();
                info.mCellRsrq = rsrq;
                info.mCellSinr = sinr;
            }
        }
    }
}
