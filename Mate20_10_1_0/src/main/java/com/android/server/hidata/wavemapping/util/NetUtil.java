package com.android.server.hidata.wavemapping.util;

import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.huawei.lcagent.client.LogCollectManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetUtil {
    private static final int DEFAULT_LENGTH = 6;
    private static final int DEFAULT_TRAFFIC_LENGTH = 2;
    private static final int INVALID_VALUE = -1;
    private static final String KEY_CELL_FREQ = "cellFreq";
    private static final String KEY_CELL_ID = "cellId";
    private static final String KEY_CELL_RAT = "cellRAT";
    private static final String KEY_CELL_RSSI = "cellRssi";
    private static final String KEY_CELL_SERVICE = "cellService";
    private static final String KEY_CELL_STATE = "cellState";
    private static final String KEY_DATA_STATE = "dataState";
    private static final String KEY_DEFAULT_NETWORK_FREQ = "defaultNwFreq";
    private static final String KEY_DEFAULT_NETWORK_ID = "defaultNwId";
    private static final String KEY_DEFAULT_NETWORK_NAME = "defaultNwName";
    private static final String KEY_DEFAULT_TYPE = "defaultType";
    private static final String KEY_DISABLED = "DISABLED";
    private static final String KEY_DISABLING = "DISABLING";
    private static final String KEY_ENABLED = "ENABLED";
    private static final String KEY_ENABLING = "ENABLING";
    private static final String KEY_MOBILE = "mobile";
    private static final String KEY_PREFERRED_MODE = "preferredMode";
    private static final String KEY_UNKNOWN = "UNKNOWN";
    private static final String KEY_WIFI = "wifi";
    private static final String KEY_WIFI_AP = "wifiAp";
    private static final String KEY_WIFI_CH = "wifiCh";
    private static final String KEY_WIFI_LS = "wifiLS";
    private static final String KEY_WIFI_MAC = "wifiMAC";
    private static final String KEY_WIFI_RSSI = "wifiRssi";
    private static final String KEY_WIFI_STATE = "wifiState";
    private static final int MOBILE_STATE_INDEX = 0;
    private static final String TAG = ("WMapping." + NetUtil.class.getSimpleName());
    public static final String UNKNOWN_STR = "UNKNOWN";
    private static final int WIFI_STATE_INDEX = 1;

    private NetUtil() {
    }

    public static Bundle getWifiStateString(Context context) {
        Bundle output = new Bundle();
        try {
            WifiManager myWifiManager = (WifiManager) context.getSystemService("wifi");
            if (myWifiManager == null) {
                LogUtil.d(false, "getWifiStateString:myWifiManager == null", new Object[0]);
                return output;
            }
            ConnectivityManager myConMgr = (ConnectivityManager) context.getSystemService("connectivity");
            if (myConMgr == null) {
                LogUtil.d(false, "getWifiStateString:myConMgr == null", new Object[0]);
                return output;
            }
            String wifiStateStr = getWifiStateStr(myWifiManager);
            if (!checkWifiConnection(myConMgr)) {
                LogUtil.d(false, "wifi NOT connected", new Object[0]);
                wifiStateStr = "DISCONNECTED";
            }
            writeToBundle(myWifiManager, output);
            output.putString(KEY_WIFI_STATE, wifiStateStr);
            if (!TextUtils.isEmpty(output.getString(KEY_WIFI_AP, "UNKNOWN")) && !TextUtils.isEmpty(output.getString(KEY_WIFI_STATE, "UNKNOWN"))) {
                LogUtil.i(false, " getWifiStateString, current Ssid=%{private}s, state=%{public}s", output.getString(KEY_WIFI_AP, "UNKNOWN"), output.getString(KEY_WIFI_STATE, "UNKNOWN"));
            }
            return output;
        } catch (RuntimeException e) {
            LogUtil.e(false, "getWifiStateString: RuntimeException ", new Object[0]);
        } catch (Exception e2) {
            LogUtil.e(false, "getWifiStateString failed by Exception", new Object[0]);
        }
    }

    private static void writeToBundle(WifiManager wifiManager, Bundle bundle) {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null && wifiInfo.getBSSID() != null) {
            String wifiAp = wifiInfo.getSSID();
            String wifiMac = wifiInfo.getBSSID();
            String wifiCh = Integer.toString(wifiInfo.getFrequency());
            String wifiLinkSpeed = Integer.toString(wifiInfo.getLinkSpeed());
            String wifiRssi = Integer.toString(wifiInfo.getRssi());
            bundle.putString(KEY_WIFI_AP, wifiAp);
            bundle.putString(KEY_WIFI_MAC, wifiMac);
            bundle.putString(KEY_WIFI_CH, wifiCh);
            bundle.putString(KEY_WIFI_LS, wifiLinkSpeed);
            bundle.putString(KEY_WIFI_RSSI, wifiRssi);
        }
    }

    private static String getWifiStateStr(WifiManager wifiManager) {
        int wifiState = wifiManager.getWifiState();
        if (wifiState == 0) {
            return KEY_DISABLING;
        }
        if (wifiState == 1) {
            return KEY_DISABLED;
        }
        if (wifiState == 2) {
            return KEY_ENABLING;
        }
        if (wifiState != 3) {
            return "UNKNOWN";
        }
        return KEY_ENABLED;
    }

    private static boolean checkWifiConnection(ConnectivityManager connectivityManager) {
        Network[] networks = connectivityManager.getAllNetworks();
        if (networks == null || networks.length == 0) {
            return false;
        }
        LogUtil.i(false, "networks size is %{public}d", Integer.valueOf(networks.length));
        for (Network network : networks) {
            NetworkInfo netInfo = connectivityManager.getNetworkInfo(network);
            if (netInfo == null) {
                return false;
            }
            LogUtil.v(false, "networksInfo is %{public}s", netInfo.toString());
            if (netInfo.getType() == 1 && netInfo.isConnected()) {
                return true;
            }
        }
        return false;
    }

    public static String getMobileDataScrbId(Context context, LogCollectManager collectManager) {
        String scribId;
        String scriptionId = "NA";
        if (collectManager == null) {
            try {
                LogUtil.w(false, " no mCollectManger", new Object[0]);
                return scriptionId;
            } catch (RuntimeException e) {
                LogUtil.e(false, "getMobileDataSubId: RuntimeException ", new Object[0]);
            } catch (Exception e2) {
                LogUtil.e(false, "getMobileDataSubId failed by Exception", new Object[0]);
            }
        } else {
            TelephonyManager defaultTelephonyManager = (TelephonyManager) context.getSystemService("phone");
            if (defaultTelephonyManager == null) {
                LogUtil.w(false, " no defaultTelephonyManager", new Object[0]);
                return scriptionId;
            }
            int subId = SubscriptionManager.getDefaultDataSubscriptionId();
            if (subId == -1) {
                subId = SubscriptionManager.getDefaultSubscriptionId();
                LogUtil.w(false, " no default DATA sub: %{public}d", Integer.valueOf(subId));
            }
            if (subId == -1) {
                LogUtil.w(false, " no default sub: %{public}d", Integer.valueOf(subId));
                return scriptionId;
            }
            int phoneType = TelephonyManager.getPhoneType(subId);
            if (phoneType == 1) {
                String scribId2 = defaultTelephonyManager.getSubscriberId(subId);
                if (scribId2 != null) {
                    scriptionId = collectManager.doEncrypt(scribId2);
                }
            } else if (phoneType == 2 && (scribId = defaultTelephonyManager.getMeid(subId)) != null) {
                scriptionId = collectManager.doEncrypt(scribId);
            }
            LogUtil.i(false, "getMobileDataSubId: %{public}s, subId=%{public}d, phoneType=%{public}d", scriptionId, Integer.valueOf(subId), Integer.valueOf(phoneType));
            return scriptionId;
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r2v1, resolved type: int */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX INFO: Multiple debug info for r3v5 java.util.List<android.telephony.CellInfo>: [D('defaultTelephonyManager' android.telephony.TelephonyManager), D('cellInfos' java.util.List<android.telephony.CellInfo>)] */
    /* JADX WARN: Type inference failed for: r2v0 */
    /* JADX WARN: Type inference failed for: r2v4 */
    /* JADX WARN: Type inference failed for: r2v5 */
    /* JADX WARN: Type inference failed for: r2v6 */
    /* JADX WARN: Type inference failed for: r2v7 */
    /* JADX WARN: Type inference failed for: r2v8 */
    /* JADX WARN: Type inference failed for: r2v9 */
    /* JADX WARN: Type inference failed for: r2v12 */
    /* JADX WARN: Type inference failed for: r2v13 */
    /* JADX WARNING: Code restructure failed: missing block: B:102:0x02a4, code lost:
        r2 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:107:0x02c8, code lost:
        r2 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:109:0x02db, code lost:
        r8 = r25;
        r9 = r26;
        r10 = r27;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:119:0x0377, code lost:
        r2 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:121:0x037b, code lost:
        r2 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:123:0x037e, code lost:
        r2 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:125:0x0381, code lost:
        com.android.server.hidata.wavemapping.util.LogUtil.e(false, "getMobileDataState failed by Exception", new java.lang.Object[0]);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:69:0x0196, code lost:
        r2 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:84:0x020c, code lost:
        r2 = 0;
     */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Removed duplicated region for block: B:124:0x0380 A[ExcHandler: Exception (e java.lang.Exception), Splitter:B:1:0x000d] */
    public static Bundle getMobileDataState(Context context) {
        String dataSwitch;
        String nwPlmn;
        int simpleType;
        int nwType;
        String cellPlmn;
        String cellId;
        String cellId2;
        String cellId3;
        String cellFreq;
        String cellRssi;
        boolean isDataOn;
        String cellRssi2;
        int targetIdx;
        CellInfo cellInfo;
        String cellRat = "UNKNOWN";
        Bundle output = new Bundle();
        int i = 0;
        try {
            TelephonyManager defaultTelephonyManager = (TelephonyManager) context.getSystemService("phone");
            if (defaultTelephonyManager == null) {
                LogUtil.w(false, " no defaultTelephonyManager", new Object[0]);
                return output;
            }
            int subId = SubscriptionManager.getDefaultDataSubscriptionId();
            if (subId == -1) {
                subId = SubscriptionManager.getDefaultSubscriptionId();
                LogUtil.w(false, " no default DATA sub", new Object[0]);
            }
            if (subId == -1) {
                LogUtil.w(false, " no default sub", new Object[0]);
                return output;
            }
            TelephonyManager subTelephonyManager = defaultTelephonyManager.createForSubscriptionId(subId);
            if (subTelephonyManager == null) {
                LogUtil.e(false, " no TelephonyManager, subId:%{public}d", Integer.valueOf(subId));
                return output;
            }
            String cellId4 = cellRat;
            String cellFreq2 = cellRat;
            String cellRssi3 = cellRat;
            boolean isDataOn2 = defaultTelephonyManager.isDataEnabled();
            if (isDataOn2) {
                dataSwitch = KEY_ENABLED;
            } else {
                dataSwitch = KEY_DISABLED;
            }
            int preferredMode = defaultTelephonyManager.getPreferredNetworkType(subId);
            int mServiceState = subTelephonyManager.getServiceState().getState();
            int dataState = subTelephonyManager.getDataState();
            String nwPlmn2 = subTelephonyManager.getNetworkOperator();
            if (nwPlmn2 == null) {
                Object[] objArr = new Object[1];
                objArr[0] = Integer.valueOf(subId);
                LogUtil.d(false, " no PLMN, subId:%{public}d", objArr);
                nwPlmn = cellRat;
            } else {
                nwPlmn = nwPlmn2;
            }
            int nwType2 = subTelephonyManager.getDataNetworkType();
            switch (nwType2) {
                case 1:
                case 2:
                case 16:
                    cellRat = "2G";
                    simpleType = 16;
                    break;
                case 3:
                case 8:
                case 9:
                case 10:
                case 15:
                case 17:
                    cellRat = "3G";
                    simpleType = 3;
                    break;
                case 4:
                case 7:
                    cellRat = "CDMA";
                    simpleType = 4;
                    break;
                case 5:
                case 6:
                case 12:
                case 14:
                    cellRat = "C3G";
                    simpleType = 4;
                    break;
                case 11:
                default:
                    simpleType = 0;
                    break;
                case 13:
                    cellRat = "4G";
                    simpleType = 13;
                    break;
            }
            int targetIdx2 = processTargetId(subTelephonyManager, defaultTelephonyManager, subId);
            List<CellInfo> cellInfos = defaultTelephonyManager.getAllCellInfo();
            if (cellInfos == null || cellInfos.size() == 0) {
                nwType = nwType2;
                LogUtil.i(false, " no cellInfo ", new Object[0]);
                cellId2 = cellId4;
                cellId = cellFreq2;
                cellPlmn = cellRssi3;
            } else {
                int cellnum = cellInfos.size();
                int targetIdx3 = targetIdx2;
                int i2 = 0;
                String cellPlmn2 = "";
                while (true) {
                    if (i2 < cellnum) {
                        CellInfo cellInfo2 = cellInfos.get(i2);
                        cellId3 = cellId4;
                        Object[] objArr2 = new Object[2];
                        cellFreq = cellFreq2;
                        objArr2[0] = Integer.valueOf(i2);
                        objArr2[1] = cellInfo2.toString();
                        LogUtil.v(false, "cellInfo(%{public}d): %{private}s", objArr2);
                        if (!cellInfo2.isRegistered()) {
                            cellRssi2 = cellRssi3;
                            targetIdx = targetIdx3;
                            isDataOn = isDataOn2;
                            nwType = nwType2;
                        } else if (targetIdx3 == 0) {
                            nwType = nwType2;
                            cellRssi = cellRssi3;
                        } else {
                            if (targetIdx3 > 1) {
                                targetIdx3--;
                                nwType = nwType2;
                                cellRssi2 = cellRssi3;
                                isDataOn = isDataOn2;
                            } else {
                                if (simpleType == 16) {
                                    cellInfo = cellInfo2;
                                    if (cellInfo instanceof CellInfoGsm) {
                                        CellInfoGsm cellInfoGsm = (CellInfoGsm) cellInfo;
                                        CellIdentityGsm cellIdentityGsm = cellInfoGsm.getCellIdentity();
                                        cellRssi2 = cellRssi3;
                                        String cellPlmn3 = cellIdentityGsm.getMccString() + cellIdentityGsm.getMncString();
                                        isDataOn = isDataOn2;
                                        Object[] objArr3 = new Object[1];
                                        objArr3[0] = cellPlmn3;
                                        LogUtil.i(false, "PLMN(%{private}s)", objArr3);
                                        if (cellPlmn3.equals(nwPlmn)) {
                                            LogUtil.i(false, "PLMN the same", new Object[0]);
                                            String cellRssi4 = Integer.toString(cellInfoGsm.getCellSignalStrength().getDbm());
                                            cellId2 = Integer.toString(cellIdentityGsm.getCid());
                                            nwType = nwType2;
                                            cellId = Integer.toString(cellIdentityGsm.getArfcn());
                                            cellPlmn = cellRssi4;
                                        } else {
                                            nwType = nwType2;
                                            cellPlmn2 = cellPlmn3;
                                            targetIdx3 = targetIdx3;
                                        }
                                    } else {
                                        cellRssi2 = cellRssi3;
                                        targetIdx = targetIdx3;
                                        isDataOn = isDataOn2;
                                    }
                                } else {
                                    cellRssi2 = cellRssi3;
                                    targetIdx = targetIdx3;
                                    isDataOn = isDataOn2;
                                    cellInfo = cellInfo2;
                                }
                                if (simpleType != 13 || !(cellInfo instanceof CellInfoLte)) {
                                    nwType = nwType2;
                                    if (simpleType == 3 && (cellInfo instanceof CellInfoWcdma)) {
                                        CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfo;
                                        CellIdentityWcdma cellIdentityWcdma = cellInfoWcdma.getCellIdentity();
                                        String cellPlmn4 = cellIdentityWcdma.getMccString() + cellIdentityWcdma.getMncString();
                                        LogUtil.i(false, "PLMN(%{private}s)", cellPlmn4);
                                        if (cellPlmn4.equals(nwPlmn)) {
                                            LogUtil.i(false, "PLMN the same", new Object[0]);
                                            String cellRssi5 = Integer.toString(cellInfoWcdma.getCellSignalStrength().getDbm());
                                            cellId2 = Integer.toString(cellIdentityWcdma.getCid());
                                            cellId = Integer.toString(cellIdentityWcdma.getUarfcn());
                                            cellPlmn = cellRssi5;
                                        } else {
                                            cellPlmn2 = cellPlmn4;
                                            targetIdx3 = targetIdx;
                                        }
                                    } else if (simpleType != 4 || !(cellInfo instanceof CellInfoCdma)) {
                                        LogUtil.d(false, "not GWLC", new Object[0]);
                                    } else {
                                        CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfo;
                                        cellPlmn = Integer.toString(cellInfoCdma.getCellSignalStrength().getDbm());
                                        cellId2 = Integer.toString(cellInfoCdma.getCellIdentity().getBasestationId());
                                        cellId = cellFreq;
                                    }
                                } else {
                                    CellInfoLte cellInfoLte = (CellInfoLte) cellInfo;
                                    CellIdentityLte cellIdentityLte = cellInfoLte.getCellIdentity();
                                    String cellPlmn5 = cellIdentityLte.getMccString() + cellIdentityLte.getMncString();
                                    nwType = nwType2;
                                    Object[] objArr4 = new Object[1];
                                    objArr4[0] = cellPlmn5;
                                    LogUtil.i(false, "PLMN(%{private}s)", objArr4);
                                    if (cellPlmn5.equals(nwPlmn)) {
                                        LogUtil.i(false, "PLMN the same", new Object[0]);
                                        String cellRssi6 = Integer.toString(cellInfoLte.getCellSignalStrength().getDbm());
                                        String cellId5 = Integer.toString(cellIdentityLte.getCi());
                                        cellPlmn = cellRssi6;
                                        cellId = Integer.toString(cellIdentityLte.getEarfcn());
                                        cellId2 = cellId5;
                                    } else {
                                        cellPlmn2 = cellPlmn5;
                                        targetIdx3 = targetIdx;
                                    }
                                }
                            }
                            i2++;
                            cellInfos = cellInfos;
                            subTelephonyManager = subTelephonyManager;
                            nwType2 = nwType;
                            cellnum = cellnum;
                            cellId4 = cellId3;
                            cellFreq2 = cellFreq;
                            cellRssi3 = cellRssi2;
                            isDataOn2 = isDataOn;
                        }
                        targetIdx3 = targetIdx;
                        i2++;
                        cellInfos = cellInfos;
                        subTelephonyManager = subTelephonyManager;
                        nwType2 = nwType;
                        cellnum = cellnum;
                        cellId4 = cellId3;
                        cellFreq2 = cellFreq;
                        cellRssi3 = cellRssi2;
                        isDataOn2 = isDataOn;
                    } else {
                        nwType = nwType2;
                        cellId3 = cellId4;
                        cellFreq = cellFreq2;
                        cellRssi = cellRssi3;
                    }
                }
            }
            output.putString(KEY_CELL_STATE, dataSwitch);
            output.putInt(KEY_DATA_STATE, dataState);
            output.putString(KEY_CELL_ID, cellId2);
            output.putString(KEY_CELL_FREQ, cellId);
            output.putString(KEY_CELL_RSSI, cellPlmn);
            output.putString(KEY_CELL_RAT, cellRat);
            output.putInt(KEY_CELL_SERVICE, mServiceState);
            output.putInt(KEY_PREFERRED_MODE, preferredMode);
            Object[] objArr5 = new Object[8];
            objArr5[0] = Integer.valueOf(subId);
            objArr5[1] = Integer.valueOf(nwType);
            objArr5[2] = cellRat;
            objArr5[3] = cellId;
            objArr5[4] = dataSwitch;
            objArr5[5] = Integer.valueOf(dataState);
            objArr5[6] = Integer.valueOf(mServiceState);
            objArr5[7] = Integer.valueOf(preferredMode);
            LogUtil.i(false, " getMobileDataState: subId=%{public}d, nwType=%{public}d, cellRat=%{private}s, cellFreq=%{public}s, cellState=%{public}s, dataState=%{public}d, cellService=%{public}d, preferredMode=%{public}d", objArr5);
            LogUtil.v(false, " getMobileDataState: cellId=%{private}s, PLMN=%{private}s", cellId2, nwPlmn);
            return output;
        } catch (RuntimeException e) {
            i = 0;
        } catch (Exception e2) {
        }
        LogUtil.e(i, "getMobileDataState: RuntimeException ", new Object[i]);
        return output;
    }

    private static int processTargetId(TelephonyManager subTelephonyManager, TelephonyManager defaultTelephonyManager, int subId) {
        int phoneCnt = subTelephonyManager.getPhoneCount();
        int regCnt = 0;
        for (int k = 0; k < phoneCnt; k++) {
            TelephonyManager subTel = defaultTelephonyManager.createForSubscriptionId(k);
            if (subTel != null) {
                int regV = subTel.getServiceState().getVoiceRegState();
                int regD = subTel.getServiceState().getDataRegState();
                LogUtil.i(false, "RegState(%{public}d): voice=%{public}d, data=%{public}d", Integer.valueOf(k), Integer.valueOf(regV), Integer.valueOf(regD));
                if (regV == 0 || regD == 0) {
                    regCnt++;
                    if (k == subId) {
                        return regCnt;
                    }
                }
            }
        }
        return 0;
    }

    public static String getNetworkType(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
        if (cm == null) {
            return "UNKNOWN";
        }
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
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
        if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
            return activeNetwork.getType();
        }
        return 8;
    }

    public static Bundle getConnectedNetworkState(Context context) {
        Bundle output = new Bundle();
        boolean isMobileConnected = false;
        boolean isWifiConnected = false;
        int defaultConnectedType = 8;
        String networkId = "UNKNOWN";
        String networkName = "UNKNOWN";
        String networkFreq = "UNKNOWN";
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
        if (cm != null) {
            NetworkInfo defaultInfo = cm.getActiveNetworkInfo();
            Map<Integer, Boolean> stateMap = judgeNetworkState(cm);
            isMobileConnected = stateMap.get(0).booleanValue();
            isWifiConnected = stateMap.get(1).booleanValue();
            if (defaultInfo == null) {
                LogUtil.d(false, " no active network", new Object[0]);
            } else if (defaultInfo.getState() == NetworkInfo.State.CONNECTED && defaultInfo.isAvailable()) {
                defaultConnectedType = defaultInfo.getType();
                if (defaultConnectedType == 1) {
                    Bundle wifiState = getWifiStateString(context);
                    if (wifiState == null) {
                        LogUtil.e(false, "wifiState is null", new Object[0]);
                        return output;
                    } else if (wifiState.getString(KEY_WIFI_MAC, "UNKNOWN") != null) {
                        networkId = wifiState.getString(KEY_WIFI_MAC, "UNKNOWN");
                        networkName = wifiState.getString(KEY_WIFI_AP, "UNKNOWN");
                        networkFreq = wifiState.getString(KEY_WIFI_CH, "UNKNOWN");
                    }
                }
                if (defaultConnectedType == 0) {
                    Bundle mobileState = getMobileDataState(context);
                    if (mobileState == null) {
                        LogUtil.e(false, "mobileState is null", new Object[0]);
                        return output;
                    } else if (mobileState.getString(KEY_CELL_ID, "UNKNOWN") != null) {
                        networkId = mobileState.getString(KEY_CELL_ID, "UNKNOWN");
                        networkName = mobileState.getString(KEY_CELL_RAT, "UNKNOWN");
                        networkFreq = mobileState.getString(KEY_CELL_FREQ, "UNKNOWN");
                    }
                }
                LogUtil.i(false, " network is connected, default type = %{public}d, Name = %{public}s", Integer.valueOf(defaultConnectedType), networkName);
            }
        }
        output.putBoolean(KEY_MOBILE, isMobileConnected);
        output.putBoolean("wifi", isWifiConnected);
        output.putInt(KEY_DEFAULT_TYPE, defaultConnectedType);
        output.putString(KEY_DEFAULT_NETWORK_ID, networkId);
        output.putString(KEY_DEFAULT_NETWORK_NAME, networkName);
        output.putString(KEY_DEFAULT_NETWORK_FREQ, networkFreq);
        return output;
    }

    private static Map<Integer, Boolean> judgeNetworkState(ConnectivityManager cm) {
        boolean isMobileConnected = false;
        boolean isWifiConnected = false;
        Network[] networks = cm.getAllNetworks();
        Map<Integer, Boolean> map = new HashMap<>();
        map.put(0, false);
        map.put(1, false);
        if (networks == null) {
            return map;
        }
        LogUtil.i(false, "networks size is %{public}d", Integer.valueOf(networks.length));
        for (Network network : networks) {
            NetworkInfo netInfo = cm.getNetworkInfo(network);
            if (netInfo != null) {
                LogUtil.v(false, "networksInfo is %{public}s", netInfo.toString());
                if (netInfo.isConnected()) {
                    if (netInfo.getType() == 0) {
                        LogUtil.i(false, "mobile network is connected", new Object[0]);
                        isMobileConnected = true;
                    } else if (netInfo.getType() == 1) {
                        LogUtil.i(false, "wifi network is connected", new Object[0]);
                        isWifiConnected = true;
                    }
                }
            }
        }
        map.put(0, Boolean.valueOf(isMobileConnected));
        map.put(1, Boolean.valueOf(isWifiConnected));
        return map;
    }

    public static boolean isMobileCallStateIdle(Context context) {
        try {
            TelephonyManager defaultTelephonyManager = (TelephonyManager) context.getSystemService("phone");
            if (defaultTelephonyManager == null) {
                LogUtil.w(false, " no defaultTelephonyManager", new Object[0]);
                return false;
            }
            int subId = SubscriptionManager.getDefaultDataSubscriptionId();
            if (subId == -1) {
                subId = SubscriptionManager.getDefaultSubscriptionId();
                LogUtil.w(false, " no default DATA sub", new Object[0]);
            }
            if (subId == -1) {
                LogUtil.w(false, " no default sub", new Object[0]);
                return false;
            }
            TelephonyManager subTelephonyManager = defaultTelephonyManager.createForSubscriptionId(subId);
            if (subTelephonyManager == null) {
                LogUtil.e(false, " no TelephonyManager", new Object[0]);
                return false;
            }
            int mCallState = subTelephonyManager.getCallState();
            LogUtil.i(false, " getMobileCallState, current call state=%{public}d, subId=%{public}d", Integer.valueOf(mCallState), Integer.valueOf(subId));
            if (mCallState == 0) {
                return true;
            }
            return false;
        } catch (RuntimeException e) {
            LogUtil.e(false, "getMobileCallState: RuntimeException ", new Object[0]);
            return false;
        } catch (Exception e2) {
            LogUtil.e(false, "getMobileCallState failed by Exception", new Object[0]);
            return false;
        }
    }

    public static boolean isWifiEnabled(Context context) {
        try {
            WifiManager myWifiManager = (WifiManager) context.getSystemService("wifi");
            if (myWifiManager == null) {
                LogUtil.d(false, "isWifiEnabled:myWifiManager == null", new Object[0]);
                return false;
            }
            myWifiManager.getWifiState();
            return myWifiManager.isWifiEnabled();
        } catch (RuntimeException e) {
            LogUtil.e(false, "isWifiEnabled: RuntimeException ", new Object[0]);
            return false;
        } catch (Exception e2) {
            LogUtil.e(false, "isWifiEnabled failed by Exception", new Object[0]);
            return false;
        }
    }

    public static long[] getTraffic(long startTime, long endTime, int network, Context context) {
        int subId;
        long[] traffic = new long[2];
        if (network != 0 && network != 1) {
            LogUtil.e(false, "network is invalid:%{public}d", Integer.valueOf(network));
            return traffic;
        } else if (endTime < startTime) {
            LogUtil.w(false, "Time is invalid: start=%{public}s, end=%{public}s", String.valueOf(startTime), String.valueOf(endTime));
            return traffic;
        } else {
            NetworkStatsManager mNetworkStatsManager = (NetworkStatsManager) context.getSystemService("netstats");
            TelephonyManager defaultTelephonyManager = (TelephonyManager) context.getSystemService("phone");
            if (defaultTelephonyManager == null) {
                LogUtil.w(false, " no defaultTelephonyManager", new Object[0]);
                return traffic;
            } else if (mNetworkStatsManager == null) {
                LogUtil.w(false, " no mNetworkStatsManager", new Object[0]);
                return traffic;
            } else {
                int subId2 = SubscriptionManager.getDefaultDataSubscriptionId();
                if (subId2 == -1) {
                    int subId3 = SubscriptionManager.getDefaultSubscriptionId();
                    LogUtil.w(false, " no default DATA sub", new Object[0]);
                    subId = subId3;
                } else {
                    subId = subId2;
                }
                if (subId == -1) {
                    LogUtil.w(false, " no default sub", new Object[0]);
                    return traffic;
                }
                String subscriberId = defaultTelephonyManager.getSubscriberId(subId);
                if (subscriberId == null) {
                    LogUtil.w(false, " no subscriber", new Object[0]);
                    return traffic;
                }
                processScrbIdPrint(startTime, endTime, network, subscriberId);
                try {
                    processTraffic(mNetworkStatsManager.querySummary(network, subscriberId, startTime, endTime), traffic);
                } catch (SecurityException e) {
                    LogUtil.e(false, "getTraffic Exception %{public}s", e.getMessage());
                } catch (RemoteException e2) {
                    LogUtil.e(false, "getTraffic Exception %{public}s", e2.getMessage());
                }
                return traffic;
            }
        }
    }

    private static void processScrbIdPrint(long startTime, long endTime, int network, String subscriberId) {
        String scrbIdPrint;
        if (subscriberId.length() > 6) {
            scrbIdPrint = subscriberId.substring(0, 6);
        } else {
            scrbIdPrint = subscriberId;
        }
        LogUtil.i(false, "getTraffic begin: startTime=%{public}s, endTime=%{public}s, network=%{public}d, =%{private}s", String.valueOf(startTime), String.valueOf(endTime), Integer.valueOf(network), scrbIdPrint);
    }

    private static void processTraffic(NetworkStats networkStats, long[] traffic) {
        NetworkStats.Bucket summaryBucket = new NetworkStats.Bucket();
        long rxBytes = 0;
        long txBytes = 0;
        if (networkStats != null) {
            do {
                networkStats.getNextBucket(summaryBucket);
                rxBytes += summaryBucket.getRxBytes();
                txBytes += summaryBucket.getTxBytes();
            } while (networkStats.hasNextBucket());
            traffic[0] = rxBytes;
            traffic[1] = txBytes;
            LogUtil.i(false, "getTraffic: rx=%{public}s, tx=%{public}s", String.valueOf(rxBytes), String.valueOf(txBytes));
            return;
        }
        LogUtil.e(false, "mNetworkStats == null", new Object[0]);
    }
}
