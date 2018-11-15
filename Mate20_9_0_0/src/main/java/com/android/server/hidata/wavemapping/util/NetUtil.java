package com.android.server.hidata.wavemapping.util;

import android.app.usage.NetworkStats;
import android.app.usage.NetworkStats.Bucket;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.telephony.CellIdentityGsm;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.huawei.lcagent.client.LogCollectManager;
import java.util.List;

public class NetUtil {
    private static final String TAG;
    public static final String UNKNOWN_STR = "UNKNOWN";

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(NetUtil.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    public static Bundle getWifiStateString(Context context) {
        Context context2 = context;
        Bundle output = new Bundle();
        boolean wifiConnected = false;
        String wifiStateStr = "UNKNOWN";
        String wifiAp = "UNKNOWN";
        String wifiMAC = "UNKNOWN";
        String wifiCh = "UNKNOWN";
        String wifiLinkSpeed = "UNKNOWN";
        String wifiRssi = "UNKNOWN";
        try {
            WifiManager myWifiManager = (WifiManager) context2.getSystemService("wifi");
            if (myWifiManager == null) {
                LogUtil.d("getWifiStateString:myWifiManager == null");
                return output;
            }
            ConnectivityManager myConMgr = (ConnectivityManager) context2.getSystemService("connectivity");
            if (myConMgr == null) {
                LogUtil.d("getWifiStateString:myConMgr == null");
                return output;
            }
            switch (myWifiManager.getWifiState()) {
                case 0:
                    wifiStateStr = "DISABLING";
                    break;
                case 1:
                    wifiStateStr = "DISABLED";
                    break;
                case 2:
                    wifiStateStr = "ENABLING";
                    break;
                case 3:
                    wifiStateStr = "ENABLED";
                    break;
                default:
                    wifiStateStr = "UNKNOWN";
                    break;
            }
            if (myWifiManager.isWifiEnabled()) {
                Network[] networks = myConMgr.getAllNetworks();
                if (networks != null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("networks size is ");
                    stringBuilder.append(String.valueOf(networks.length));
                    LogUtil.i(stringBuilder.toString());
                    int i = 0;
                    while (i < networks.length) {
                        NetworkInfo netInfo = myConMgr.getNetworkInfo(networks[i]);
                        if (netInfo != null) {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("networksInfo is ");
                            stringBuilder2.append(netInfo.toString());
                            LogUtil.v(stringBuilder2.toString());
                            if (netInfo.getType() == 1 && DetailedState.CONNECTED == netInfo.getDetailedState()) {
                                wifiConnected = true;
                            }
                        }
                        i++;
                        context2 = context;
                    }
                }
                if (!wifiConnected) {
                    LogUtil.d("wifi NOT connected");
                    wifiStateStr = "DISCONNECTED";
                }
                WifiInfo wifiInfo = myWifiManager.getConnectionInfo();
                if (!(wifiInfo == null || wifiInfo.getBSSID() == null)) {
                    wifiAp = wifiInfo.getSSID();
                    wifiMAC = wifiInfo.getBSSID();
                    wifiCh = Integer.toString(wifiInfo.getFrequency());
                    wifiLinkSpeed = Integer.toString(wifiInfo.getLinkSpeed());
                    wifiRssi = Integer.toString(wifiInfo.getRssi());
                    output.putString("wifiState", wifiStateStr);
                    output.putString("wifiAp", wifiAp);
                    output.putString("wifiMAC", wifiMAC);
                    output.putString("wifiCh", wifiCh);
                    output.putString("wifiLS", wifiLinkSpeed);
                    output.putString("wifiRssi", wifiRssi);
                }
            } else {
                LogUtil.i("wifi NOT enabled");
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(" getWifiStateString, current Ssid=");
            stringBuilder3.append(output.getString("wifiAp", "UNKNOWN"));
            stringBuilder3.append(", state=");
            stringBuilder3.append(output.getString("wifiState", "UNKNOWN"));
            LogUtil.i(stringBuilder3.toString());
            return output;
        } catch (RuntimeException e) {
            LogUtil.e("getWifiStateString: RuntimeException ");
        } catch (Exception e2) {
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("getWifiStateString:");
            stringBuilder4.append(e2.getMessage());
            LogUtil.e(stringBuilder4.toString());
        }
    }

    public static String getMobileDataScrbId(Context context) {
        String scriptionId = "NA";
        try {
            TelephonyManager defaultTelephonyManager = (TelephonyManager) context.getSystemService("phone");
            if (defaultTelephonyManager == null) {
                LogUtil.w(" no defaultTelephonyManager");
                return scriptionId;
            }
            int subId = SubscriptionManager.getDefaultDataSubscriptionId();
            if (-1 == subId) {
                subId = SubscriptionManager.getDefaultSubscriptionId();
                LogUtil.w(" no default DATA sub");
            }
            if (-1 == subId) {
                LogUtil.w(" no default sub");
                return scriptionId;
            }
            TelephonyManager subTelephonyManager = defaultTelephonyManager.createForSubscriptionId(subId);
            if (subTelephonyManager == null) {
                LogUtil.e(" no TelephonyManager");
                return scriptionId;
            }
            LogCollectManager mCollectManger = new LogCollectManager(context);
            String scribId = "";
            int phoneType = subTelephonyManager.getPhoneType();
            if (1 == phoneType) {
                scriptionId = mCollectManger.doEncrypt(subTelephonyManager.getSubscriberId());
            } else if (2 == phoneType) {
                scriptionId = mCollectManger.doEncrypt(subTelephonyManager.getMeid());
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getMobileDataSubId: ");
            stringBuilder.append(scriptionId);
            stringBuilder.append(", subId=");
            stringBuilder.append(subId);
            stringBuilder.append(", phoneType=");
            stringBuilder.append(phoneType);
            LogUtil.i(stringBuilder.toString());
            return scriptionId;
        } catch (RuntimeException e) {
            LogUtil.e("getMobileDataSubId: RuntimeException ");
        } catch (Exception e2) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getMobileDataSubId,e");
            stringBuilder2.append(e2.getMessage());
            LogUtil.e(stringBuilder2.toString());
        }
    }

    public static Bundle getMobileDataState(Context context) {
        Bundle output = new Bundle();
        try {
            TelephonyManager defaultTelephonyManager = (TelephonyManager) context.getSystemService("phone");
            if (defaultTelephonyManager == null) {
                LogUtil.w(" no defaultTelephonyManager");
                return output;
            }
            int subId = SubscriptionManager.getDefaultDataSubscriptionId();
            if (-1 == subId) {
                subId = SubscriptionManager.getDefaultSubscriptionId();
                LogUtil.w(" no default DATA sub");
            }
            if (-1 == subId) {
                LogUtil.w(" no default sub");
                return output;
            }
            TelephonyManager subTelephonyManager = defaultTelephonyManager.createForSubscriptionId(subId);
            if (subTelephonyManager == null) {
                LogUtil.e(" no TelephonyManager");
                return output;
            }
            String dataSwitch;
            String cellId = "UNKNOWN";
            String cellFreq = "UNKNOWN";
            String cellRssi = "UNKNOWN";
            String cellRAT = "UNKNOWN";
            String cellPLMN = "";
            boolean isDataOn = subTelephonyManager.isDataEnabled();
            String nwPLMN = subTelephonyManager.getNetworkOperator();
            if (nwPLMN == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" no PLMN, subId:");
                stringBuilder.append(subId);
                LogUtil.d(stringBuilder.toString());
                nwPLMN = "UNKNOWN";
            }
            if (isDataOn) {
                dataSwitch = "ENABLED";
            } else {
                dataSwitch = "DISABLED";
            }
            int mServiceState = subTelephonyManager.getServiceState().getState();
            List<CellInfo> cellInfos = subTelephonyManager.getAllCellInfo();
            int i;
            TelephonyManager telephonyManager;
            boolean z;
            if (cellInfos == null || cellInfos.size() == 0) {
                i = subId;
                telephonyManager = subTelephonyManager;
                z = isDataOn;
                LogUtil.i(" no cellInfo ");
            } else {
                for (CellInfo defaultTelephonyManager2 : cellInfos) {
                    TelephonyManager defaultTelephonyManager3 = defaultTelephonyManager;
                    if (defaultTelephonyManager2.isRegistered()) {
                        if (defaultTelephonyManager2 instanceof CellInfoGsm) {
                            CellInfoGsm cellInfoGsm = (CellInfoGsm) defaultTelephonyManager2;
                            CellIdentityGsm cellIdentityGsm = cellInfoGsm.getCellIdentity();
                            i = subId;
                            subId = new StringBuilder();
                            telephonyManager = subTelephonyManager;
                            z = isDataOn;
                            CellIdentityGsm subTelephonyManager2 = cellIdentityGsm;
                            subId.append(subTelephonyManager2.getMccString());
                            subId.append(subTelephonyManager2.getMncString());
                            subId = subId.toString();
                            Object obj;
                            if (subId.equals(nwPLMN)) {
                                LogUtil.i("PLMN the same");
                                isDataOn = Integer.toString(cellInfoGsm.getCellSignalStrength().getDbm());
                                cellId = Integer.toString(subTelephonyManager2.getCid());
                                cellFreq = Integer.toString(subTelephonyManager2.getArfcn());
                                cellRssi = "2G";
                                obj = subId;
                            } else {
                                obj = subId;
                            }
                        } else {
                            i = subId;
                            telephonyManager = subTelephonyManager;
                            z = isDataOn;
                            String cellPLMN2;
                            if (defaultTelephonyManager2 instanceof CellInfoLte) {
                                CellInfoLte cellInfoLte = (CellInfoLte) defaultTelephonyManager2;
                                subId = cellInfoLte.getCellIdentity();
                                cellPLMN2 = new StringBuilder();
                                cellPLMN2.append(subId.getMccString());
                                cellPLMN2.append(subId.getMncString());
                                cellPLMN2 = cellPLMN2.toString();
                                if (cellPLMN2.equals(nwPLMN)) {
                                    LogUtil.i("PLMN the same");
                                    isDataOn = Integer.toString(cellInfoLte.getCellSignalStrength().getDbm());
                                    cellId = Integer.toString(subId.getCi());
                                    cellFreq = Integer.toString(subId.getEarfcn());
                                    cellRssi = "4G";
                                }
                            } else if (defaultTelephonyManager2 instanceof CellInfoWcdma) {
                                CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) defaultTelephonyManager2;
                                subId = cellInfoWcdma.getCellIdentity();
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(subId.getMccString());
                                stringBuilder2.append(subId.getMncString());
                                cellPLMN2 = stringBuilder2.toString();
                                if (cellPLMN2.equals(nwPLMN)) {
                                    LogUtil.i("PLMN the same");
                                    isDataOn = Integer.toString(cellInfoWcdma.getCellSignalStrength().getDbm());
                                    cellId = Integer.toString(subId.getCid());
                                    cellFreq = Integer.toString(subId.getUarfcn());
                                    cellRssi = "3G";
                                }
                            } else if (defaultTelephonyManager2 instanceof CellInfoCdma) {
                                CellInfoCdma cellInfoCdma = (CellInfoCdma) defaultTelephonyManager2;
                                cellRssi = Integer.toString(cellInfoCdma.getCellSignalStrength().getDbm());
                                cellRAT = "CDMA";
                                cellId = Integer.toString(cellInfoCdma.getCellIdentity().getBasestationId());
                                break;
                            } else {
                                LogUtil.d("not GWLC");
                            }
                        }
                        cellRAT = cellRssi;
                        cellRssi = isDataOn;
                        break;
                    }
                    i = subId;
                    telephonyManager = subTelephonyManager;
                    z = isDataOn;
                    defaultTelephonyManager = defaultTelephonyManager3;
                    subId = i;
                    subTelephonyManager = telephonyManager;
                    isDataOn = z;
                    Context context2 = context;
                }
                i = subId;
                telephonyManager = subTelephonyManager;
                z = isDataOn;
            }
            output.putString("cellState", dataSwitch);
            output.putString("cellId", cellId);
            output.putString("cellFreq", cellFreq);
            output.putString("cellRssi", cellRssi);
            output.putString("cellRAT", cellRAT);
            output.putInt("cellService", mServiceState);
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(" getMobileDataStateg, current cellId=");
            stringBuilder3.append(cellId);
            stringBuilder3.append(", cellRAT=");
            stringBuilder3.append(cellRAT);
            stringBuilder3.append(", cellFreq=");
            stringBuilder3.append(cellFreq);
            stringBuilder3.append(", cellState=");
            stringBuilder3.append(dataSwitch);
            stringBuilder3.append(", cellService=");
            stringBuilder3.append(mServiceState);
            LogUtil.i(stringBuilder3.toString());
            return output;
        } catch (RuntimeException e) {
            LogUtil.e("getMobileDataState: RuntimeException ");
        } catch (Exception e2) {
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("getMobileDataState,e");
            stringBuilder4.append(e2.getMessage());
            LogUtil.e(stringBuilder4.toString());
        }
    }

    public static String getNetworkType(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
        if (cm == null) {
            return "UNKNOWN";
        }
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (isConnected) {
            return activeNetwork.getTypeName();
        }
        return "UNKNOWN";
    }

    public static int getNetworkTypeInfo(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
        if (cm == null) {
            return -1;
        }
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (isConnected) {
            return activeNetwork.getType();
        }
        return 8;
    }

    public static Bundle getConnectedNetworkState(Context context) {
        Bundle output = new Bundle();
        boolean mobileConnected = false;
        boolean wifiConnected = false;
        int defaultConnectedType = 8;
        String networkId = "UNKNOWN";
        String networkName = "UNKNOWN";
        String networkFreq = "UNKNOWN";
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
        if (cm != null) {
            NetworkInfo defaultInfo = cm.getActiveNetworkInfo();
            Network[] networks = cm.getAllNetworks();
            if (networks != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("networks size is ");
                stringBuilder.append(String.valueOf(networks.length));
                LogUtil.i(stringBuilder.toString());
                for (Network networkInfo : networks) {
                    NetworkInfo netInfo = cm.getNetworkInfo(networkInfo);
                    if (netInfo != null) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("networksInfo is ");
                        stringBuilder2.append(netInfo.toString());
                        LogUtil.v(stringBuilder2.toString());
                        if (netInfo.isConnected()) {
                            if (netInfo.getType() == 0) {
                                LogUtil.i("mobile network is connected");
                                mobileConnected = true;
                            } else if (netInfo.getType() == 1) {
                                LogUtil.i("wifi network is connected");
                                wifiConnected = true;
                            }
                        }
                    }
                }
            }
            if (defaultInfo == null) {
                LogUtil.d(" no active network");
            } else if (State.CONNECTED == defaultInfo.getState() && defaultInfo.isAvailable()) {
                Bundle wifiState;
                defaultConnectedType = defaultInfo.getType();
                if (1 == defaultConnectedType) {
                    wifiState = getWifiStateString(context);
                    if (wifiState.getString("wifiMAC", "UNKNOWN") != null) {
                        networkId = wifiState.getString("wifiMAC", "UNKNOWN");
                        networkName = wifiState.getString("wifiAp", "UNKNOWN");
                        networkFreq = wifiState.getString("wifiCh", "UNKNOWN");
                    }
                }
                if (defaultConnectedType == 0) {
                    wifiState = getMobileDataState(context);
                    if (wifiState.getString("cellId", "UNKNOWN") != null) {
                        networkId = wifiState.getString("cellId", "UNKNOWN");
                        networkName = wifiState.getString("cellRAT", "UNKNOWN");
                        networkFreq = wifiState.getString("cellFreq", "UNKNOWN");
                    }
                }
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(" network is connected, default type = ");
                stringBuilder3.append(defaultConnectedType);
                stringBuilder3.append(", Name = ");
                stringBuilder3.append(networkName);
                LogUtil.i(stringBuilder3.toString());
            }
        }
        output.putBoolean("mobile", mobileConnected);
        output.putBoolean("wifi", wifiConnected);
        output.putInt("defaultType", defaultConnectedType);
        output.putString("defaultNwId", networkId);
        output.putString("defaultNwName", networkName);
        output.putString("defaultNwFreq", networkFreq);
        return output;
    }

    public static Bundle getSpecifiedDataState(Context context, int networkType) {
        Bundle wifiInfo;
        Bundle output = new Bundle();
        boolean valid = false;
        String nwName = "UNKNOWN";
        String nwId = "UNKNOWN";
        String nwFreq = "UNKNOWN";
        String signal = "UNKNOWN";
        if (1 == networkType) {
            wifiInfo = getWifiStateString(context);
            nwName = wifiInfo.getString("wifiAp", "UNKNOWN");
            nwId = wifiInfo.getString("wifiMAC", "UNKNOWN");
            nwFreq = wifiInfo.getString("wifiCh", "UNKNOWN");
            signal = wifiInfo.getString("wifiRssi", "UNKNOWN");
            if (!(nwName == null || nwId == null || nwFreq == null)) {
                valid = true;
            }
        }
        if (networkType == 0) {
            wifiInfo = getMobileDataState(context);
            nwName = wifiInfo.getString("cellRAT", "UNKNOWN");
            nwId = wifiInfo.getString("cellId", "UNKNOWN");
            nwFreq = wifiInfo.getString("cellFreq", "UNKNOWN");
            signal = wifiInfo.getString("cellRssi", "UNKNOWN");
            if (!(nwName == null || nwId == null || nwFreq == null)) {
                valid = true;
            }
        }
        output.putBoolean("VALID", valid);
        output.putString("ID", nwId);
        output.putString("NAME", nwName);
        output.putString("FREQ", nwFreq);
        output.putString("SIGNAL", signal);
        return output;
    }

    public static boolean isMobileCallStateIdle(Context context) {
        boolean result = false;
        try {
            TelephonyManager defaultTelephonyManager = (TelephonyManager) context.getSystemService("phone");
            if (defaultTelephonyManager == null) {
                LogUtil.w(" no defaultTelephonyManager");
                return false;
            }
            int subId = SubscriptionManager.getDefaultDataSubscriptionId();
            if (-1 == subId) {
                subId = SubscriptionManager.getDefaultSubscriptionId();
                LogUtil.w(" no default DATA sub");
            }
            if (-1 == subId) {
                LogUtil.w(" no default sub");
                return false;
            }
            TelephonyManager subTelephonyManager = defaultTelephonyManager.createForSubscriptionId(subId);
            if (subTelephonyManager == null) {
                LogUtil.e(" no TelephonyManager");
                return false;
            }
            int mCallState = subTelephonyManager.getCallState();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" getMobileCallState, current call state=");
            stringBuilder.append(mCallState);
            stringBuilder.append(", subId=");
            stringBuilder.append(subId);
            LogUtil.i(stringBuilder.toString());
            if (mCallState == 0) {
                result = true;
            }
            return result;
        } catch (RuntimeException e) {
            LogUtil.e("getMobileCallState: RuntimeException ");
        } catch (Exception e2) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getMobileCallState,e:");
            stringBuilder2.append(e2.getMessage());
            LogUtil.e(stringBuilder2.toString());
        }
    }

    public static long[] getTraffic(long startTime, long endTime, int network, Context context) {
        SecurityException e;
        RemoteException e2;
        String str;
        TelephonyManager telephonyManager;
        String str2;
        TelephonyManager telephonyManager2;
        int i;
        long j = startTime;
        long j2 = endTime;
        int i2 = network;
        Context context2 = context;
        long[] traffic = new long[2];
        String subscriberId = "";
        StringBuilder stringBuilder;
        if (i2 != 0 && 1 != i2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("network is invalid:");
            stringBuilder.append(i2);
            LogUtil.e(stringBuilder.toString());
            return traffic;
        } else if (j2 < j) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Time is invalid: start=");
            stringBuilder.append(j);
            stringBuilder.append(", end=");
            stringBuilder.append(j2);
            LogUtil.w(stringBuilder.toString());
            return traffic;
        } else {
            NetworkStatsManager mNetworkStatsManager = (NetworkStatsManager) context2.getSystemService("netstats");
            TelephonyManager defaultTelephonyManager = (TelephonyManager) context2.getSystemService("phone");
            if (defaultTelephonyManager == null) {
                LogUtil.w(" no defaultTelephonyManager");
                return traffic;
            } else if (mNetworkStatsManager == null) {
                LogUtil.w(" no mNetworkStatsManager");
                return traffic;
            } else {
                int subId = SubscriptionManager.getDefaultDataSubscriptionId();
                if (-1 == subId) {
                    subId = SubscriptionManager.getDefaultSubscriptionId();
                    LogUtil.w(" no default DATA sub");
                }
                int subId2 = subId;
                if (-1 == subId2) {
                    LogUtil.w(" no default sub");
                    return traffic;
                }
                TelephonyManager subTelephonyManager = defaultTelephonyManager.createForSubscriptionId(subId2);
                if (subTelephonyManager == null) {
                    LogUtil.e(" no TelephonyManager");
                    return traffic;
                }
                String subscriberId2 = subTelephonyManager.getSubscriberId();
                subscriberId = "";
                if (subscriberId2.length() > 6) {
                    subscriberId = subscriberId2.substring(0, 6);
                } else {
                    subscriberId = subscriberId2;
                }
                String ScrbId_print = subscriberId;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getTraffic begin: startTime=");
                stringBuilder2.append(j);
                stringBuilder2.append(", endTime=");
                stringBuilder2.append(j2);
                stringBuilder2.append(", network=");
                stringBuilder2.append(i2);
                stringBuilder2.append(", =");
                stringBuilder2.append(ScrbId_print);
                LogUtil.i(stringBuilder2.toString());
                try {
                    Bucket summaryBucket = new Bucket();
                    int i3 = 0;
                    try {
                        NetworkStats mNetworkStats = mNetworkStatsManager.querySummary(i2, subscriberId2, j, j2);
                        subTelephonyManager = 0;
                        defaultTelephonyManager = 0;
                        if (mNetworkStats != null) {
                            do {
                                mNetworkStats.getNextBucket(summaryBucket);
                                subTelephonyManager += summaryBucket.getRxBytes();
                                defaultTelephonyManager += summaryBucket.getTxBytes();
                            } while (mNetworkStats.hasNextBucket());
                            traffic[i3] = subTelephonyManager;
                            traffic[1] = defaultTelephonyManager;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("getTraffic: rx=");
                            stringBuilder3.append(subTelephonyManager);
                            stringBuilder3.append(", tx=");
                            stringBuilder3.append(defaultTelephonyManager);
                            LogUtil.i(stringBuilder3.toString());
                        } else {
                            LogUtil.e("mNetworkStats == null");
                        }
                    } catch (SecurityException e3) {
                        e = e3;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("getTraffic Exception");
                        stringBuilder.append(e);
                        LogUtil.e(stringBuilder.toString());
                        return traffic;
                    } catch (RemoteException e4) {
                        e2 = e4;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("getTraffic Exception");
                        stringBuilder.append(e2);
                        LogUtil.e(stringBuilder.toString());
                        return traffic;
                    }
                } catch (SecurityException e5) {
                    e = e5;
                    str = ScrbId_print;
                    telephonyManager = subTelephonyManager;
                    str2 = subscriberId2;
                    telephonyManager2 = defaultTelephonyManager;
                    i = subId2;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getTraffic Exception");
                    stringBuilder.append(e);
                    LogUtil.e(stringBuilder.toString());
                    return traffic;
                } catch (RemoteException e6) {
                    e2 = e6;
                    str = ScrbId_print;
                    telephonyManager = subTelephonyManager;
                    str2 = subscriberId2;
                    telephonyManager2 = defaultTelephonyManager;
                    i = subId2;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getTraffic Exception");
                    stringBuilder.append(e2);
                    LogUtil.e(stringBuilder.toString());
                    return traffic;
                }
                return traffic;
            }
        }
    }
}
