package com.android.server.wifi;

import android.common.HwFrameworkFactory;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.NetworkSelectionStatus;
import android.os.SystemProperties;
import android.util.Log;
import com.android.server.wifi.HwQoE.HidataWechatTraffic;
import com.android.server.wifi.wifipro.WifiHandover;
import com.android.server.wifipro.WifiProCommonUtils;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class HwSelfCureUtils {
    private static final int CURE_OUT_OF_DATE_MS = 7200000;
    public static final int DELAYED_DAYS_HIGH = 432000000;
    public static final int DELAYED_DAYS_LOW = 86400000;
    public static final int DELAYED_DAYS_MID = 259200000;
    public static final String DNS_ERR_MONITOR_FLAG = "hw.wifipro.dns_err_count";
    private static final int DNS_ERR_REFUSED_IDX = 5;
    public static final String DNS_MONITOR_FLAG = "hw.wifipro.dns_fail_count";
    private static final int INTERVAL_SELF_CURE = 60000;
    public static final int MAX_FAILED_CURE = 3;
    public static final int MIN_TCP_FAILED_THRESHOLD = 3;
    private static final String[] PUBLIC_DNS_CHINA = new String[]{"180.76.76.76", "223.5.5.5"};
    private static final String[] PUBLIC_DNS_OVERSEA = new String[]{"8.8.8.8", "208.67.222.222"};
    public static final int RESET_LEVEL_DEAUTH_BSSID = 208;
    public static final int RESET_LEVEL_HIGH_RESET = 205;
    public static final int RESET_LEVEL_IDLE = 200;
    public static final int RESET_LEVEL_LOW_1_DNS = 201;
    public static final int RESET_LEVEL_LOW_2_RENEW_DHCP = 202;
    public static final int RESET_LEVEL_LOW_3_STATIC_IP = 203;
    public static final int RESET_LEVEL_MIDDLE_REASSOC = 204;
    public static final int RESET_LEVEL_RECONNECT_4_INVALID_IP = 207;
    public static final int RESET_REJECTED_BY_STATIC_IP_ENABLED = 206;
    public static final int SCE_WIFI_CONNECT_STATE = 3;
    public static final int SCE_WIFI_CONNET_RETRY = 1;
    public static final int SCE_WIFI_DISABLED_DELAY = 200;
    public static final int SCE_WIFI_OFF_STATE = 1;
    public static final int SCE_WIFI_ON_STATE = 2;
    public static final int SCE_WIFI_REASSOC_STATE = 4;
    public static final int SCE_WIFI_RECONNECT_STATE = 5;
    public static final int SCE_WIFI_STATUS_ABRORT = -3;
    public static final int SCE_WIFI_STATUS_FAIL = -1;
    public static final int SCE_WIFI_STATUS_LOST = -2;
    public static final int SCE_WIFI_STATUS_SUCC = 0;
    public static final int SCE_WIFI_STATUS_UNKOWN = 1;
    public static final int SELFCURE_WIFI_CONNECT_TIMEOUT = 15000;
    public static final int SELFCURE_WIFI_OFF_TIMEOUT = 2000;
    public static final int SELFCURE_WIFI_ON_TIMEOUT = 3000;
    public static final int SELFCURE_WIFI_REASSOC_TIMEOUT = 12000;
    public static final int SELFCURE_WIFI_RECONNECT_TIMEOUT = 15000;
    public static final int SIGNAL_LEVEL_1 = 1;
    public static final int SIGNAL_LEVEL_2 = 2;
    public static final int SIGNAL_LEVEL_3 = 3;
    public static final int WIFIPRO_SOFT_CONNECT_FAILED = -4;
    public static final String WIFI_STAT_FILE = "proc/net/wifi_network_stat";

    public static int getCurrentDnsFailedCounter() {
        try {
            return Integer.parseInt(SystemProperties.get(DNS_MONITOR_FLAG, "0"));
        } catch (NumberFormatException e) {
            Log.d("HwSelfCureEngine", "getCurrentDnsFailedCounter NumberFormatException!");
            return 0;
        }
    }

    public static int getCurrentDnsRefuseCounter() {
        try {
            String[] counterStr = SystemProperties.get(DNS_ERR_MONITOR_FLAG, "0").split(",");
            if (counterStr.length > 5) {
                return Integer.parseInt(counterStr[5]);
            }
            return 0;
        } catch (NumberFormatException e) {
            Log.d("HwSelfCureEngine", "getCurrentDnsRefuseCounter NumberFormatException!");
            return 0;
        }
    }

    public static List<String> getRefreshedCureFailedNetworks(Map<String, CureFailedNetworkInfo> networkCureFailedHistory) {
        List<String> refreshedNetworksKey = new ArrayList();
        for (Entry entry : networkCureFailedHistory.entrySet()) {
            String currKey = (String) entry.getKey();
            if (System.currentTimeMillis() - ((CureFailedNetworkInfo) networkCureFailedHistory.get(currKey)).lastCureFailedTime > 7200000) {
                refreshedNetworksKey.add(currKey);
            }
        }
        return refreshedNetworksKey;
    }

    public static List<String> searchUnstableNetworks(Map<String, WifiConfiguration> autoConnectFailedNetworks, List<ScanResult> scanResults) {
        List<String> unstableKey = new ArrayList();
        if (scanResults == null || scanResults.size() == 0) {
            return unstableKey;
        }
        for (Entry entry : autoConnectFailedNetworks.entrySet()) {
            String currKey = (String) entry.getKey();
            WifiConfiguration currConfig = (WifiConfiguration) entry.getValue();
            boolean outOfRange = true;
            for (int i = 0; i < scanResults.size(); i++) {
                ScanResult nextResult = (ScanResult) scanResults.get(i);
                if (HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(nextResult.frequency, nextResult.level) > 2) {
                    String scanSsid = new StringBuilder();
                    scanSsid.append("\"");
                    scanSsid.append(nextResult.SSID);
                    scanSsid.append("\"");
                    scanSsid = scanSsid.toString();
                    String scanResultEncrypt = nextResult.capabilities;
                    if (currConfig.SSID != null && currConfig.SSID.equals(scanSsid) && WifiProCommonUtils.isSameEncryptType(scanResultEncrypt, currConfig.configKey())) {
                        outOfRange = false;
                        break;
                    }
                }
            }
            if (outOfRange) {
                unstableKey.add(currKey);
            }
        }
        return unstableKey;
    }

    public static void selectDisabledNetworks(List<ScanResult> scanResults, List<WifiConfiguration> savedNetworks, Map<String, WifiConfiguration> autoConnectFailedNetworks, Map<String, Integer> autoConnectFailedNetworksRssi, WifiStateMachine wsm) {
        List list = scanResults;
        List<WifiConfiguration> list2 = savedNetworks;
        Map<String, WifiConfiguration> map = autoConnectFailedNetworks;
        Map<String, Integer> map2 = autoConnectFailedNetworksRssi;
        WifiStateMachine wifiStateMachine = wsm;
        List<WifiConfiguration> disabledNetworks = new ArrayList();
        if (list != null && scanResults.size() != 0 && list2 != null && savedNetworks.size() != 0 && wifiStateMachine != null) {
            int i = 0;
            while (i < scanResults.size()) {
                List<ScanResult> list3;
                ScanResult nextResult = (ScanResult) list.get(i);
                if (HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(nextResult.frequency, nextResult.level) > 2) {
                    String scanSsid = new StringBuilder();
                    scanSsid.append("\"");
                    scanSsid.append(nextResult.SSID);
                    scanSsid.append("\"");
                    scanSsid = scanSsid.toString();
                    String scanResultEncrypt = nextResult.capabilities;
                    int j = 0;
                    while (j < savedNetworks.size()) {
                        WifiConfiguration nextConfig = (WifiConfiguration) list2.get(j);
                        if (nextConfig.SSID != null && nextConfig.SSID.equals(scanSsid) && WifiProCommonUtils.isSameEncryptType(scanResultEncrypt, nextConfig.configKey())) {
                            NetworkSelectionStatus status = nextConfig.getNetworkSelectionStatus();
                            int disableReason = status.getNetworkSelectionDisableReason();
                            if (!(status.isNetworkEnabled() || disableReason < 1 || disableReason > 11 || disableReason == 8 || disableReason == 9)) {
                                map.put(nextConfig.configKey(), nextConfig);
                                map2.put(nextConfig.configKey(), Integer.valueOf(nextResult.level));
                            }
                            if (!map.containsKey(nextConfig.configKey()) && wifiStateMachine.isBssidDisabled(nextResult.BSSID)) {
                                map.put(nextConfig.configKey(), nextConfig);
                                map2.put(nextConfig.configKey(), Integer.valueOf(nextResult.level));
                            }
                        } else {
                            j++;
                            list3 = scanResults;
                        }
                    }
                }
                i++;
                list3 = scanResults;
            }
        }
    }

    public static WifiConfiguration selectHighestFailedNetwork(Map<String, CureFailedNetworkInfo> networkCureFailedHistory, Map<String, WifiConfiguration> autoConnectFailedNetworks, Map<String, Integer> autoConnectFailedNetworksRssi) {
        WifiConfiguration bestSelfCureCandidate = null;
        int bestSelfCureLevel = WifiHandover.INVALID_RSSI;
        CureFailedNetworkInfo bestCureHistory = null;
        for (Entry entry : autoConnectFailedNetworks.entrySet()) {
            String currKey = (String) entry.getKey();
            WifiConfiguration currConfig = (WifiConfiguration) entry.getValue();
            int currLevel = ((Integer) autoConnectFailedNetworksRssi.get(currKey)).intValue();
            CureFailedNetworkInfo currCureHistory = (CureFailedNetworkInfo) networkCureFailedHistory.get(currKey);
            if (currCureHistory != null) {
                if (currCureHistory.cureFailedCounter != 3) {
                    if (System.currentTimeMillis() - currCureHistory.lastCureFailedTime < HidataWechatTraffic.MIN_VALID_TIME) {
                    }
                }
            }
            if (bestSelfCureCandidate == null) {
                bestSelfCureCandidate = currConfig;
                bestSelfCureLevel = currLevel;
                bestCureHistory = currCureHistory;
            } else if (bestCureHistory == null && currCureHistory == null) {
                if (currConfig.noInternetAccess || currConfig.portalNetwork || !(bestSelfCureCandidate.noInternetAccess || currConfig.portalNetwork)) {
                    if (!(bestSelfCureCandidate.noInternetAccess || bestSelfCureCandidate.portalNetwork)) {
                        if (!currConfig.noInternetAccess) {
                            if (currConfig.portalNetwork) {
                            }
                        }
                    }
                    if (currLevel > bestSelfCureLevel) {
                        bestSelfCureCandidate = currConfig;
                        bestSelfCureLevel = currLevel;
                    }
                } else {
                    bestSelfCureCandidate = currConfig;
                    bestSelfCureLevel = currLevel;
                }
            } else if (bestCureHistory != null && currCureHistory == null) {
                bestSelfCureCandidate = currConfig;
                bestSelfCureLevel = currLevel;
                bestCureHistory = null;
            } else if (!(bestCureHistory == null || currCureHistory == null)) {
                if (currCureHistory.cureFailedCounter < bestCureHistory.cureFailedCounter) {
                    bestSelfCureCandidate = currConfig;
                    bestSelfCureLevel = currLevel;
                    bestCureHistory = currCureHistory;
                } else if (currCureHistory.cureFailedCounter == bestCureHistory.cureFailedCounter && currLevel > bestSelfCureLevel) {
                    bestSelfCureCandidate = currConfig;
                    bestSelfCureLevel = currLevel;
                    bestCureHistory = currCureHistory;
                }
            }
        }
        return bestSelfCureCandidate;
    }

    public static ArrayList<String> getPublicDnsServers() {
        if (WifiProCommonUtils.useOperatorOverSea()) {
            return new ArrayList(Arrays.asList(PUBLIC_DNS_OVERSEA));
        }
        return new ArrayList(Arrays.asList(PUBLIC_DNS_CHINA));
    }

    public static InternetSelfCureHistoryInfo string2InternetSelfCureHistoryInfo(String selfCureHistory) {
        InternetSelfCureHistoryInfo info = new InternetSelfCureHistoryInfo();
        if (selfCureHistory != null && selfCureHistory.length() > 0) {
            String[] histories = selfCureHistory.split("\\|");
            if (histories.length == 14) {
                int i = 0;
                while (i < histories.length) {
                    try {
                        if (i == 0) {
                            info.dnsSelfCureFailedCnt = Integer.valueOf(histories[i]).intValue();
                        } else if (i == 1) {
                            info.lastDnsSelfCureFailedTs = Long.valueOf(histories[i]).longValue();
                        } else if (i == 2) {
                            info.renewDhcpSelfCureFailedCnt = Integer.valueOf(histories[i]).intValue();
                        } else if (i == 3) {
                            info.lastRenewDhcpSelfCureFailedTs = Long.valueOf(histories[i]).longValue();
                        } else if (i == 4) {
                            info.staticIpSelfCureFailedCnt = Integer.valueOf(histories[i]).intValue();
                        } else if (i == 5) {
                            info.lastStaticIpSelfCureFailedTs = Long.valueOf(histories[i]).longValue();
                        } else if (i == 6) {
                            info.reassocSelfCureFailedCnt = Integer.valueOf(histories[i]).intValue();
                        } else if (i == 7) {
                            info.lastReassocSelfCureFailedTs = Long.valueOf(histories[i]).longValue();
                        } else if (i == 8) {
                            info.resetSelfCureFailedCnt = Integer.valueOf(histories[i]).intValue();
                        } else if (i == 9) {
                            info.lastResetSelfCureFailedTs = Long.valueOf(histories[i]).longValue();
                        } else if (i == 10) {
                            info.reassocSelfCureConnectFailedCnt = Integer.valueOf(histories[i]).intValue();
                        } else if (i == 11) {
                            info.lastReassocSelfCureConnectFailedTs = Long.valueOf(histories[i]).longValue();
                        } else if (i == 12) {
                            info.resetSelfCureConnectFailedCnt = Integer.valueOf(histories[i]).intValue();
                        } else if (i == 13) {
                            info.lastResetSelfCureConnectFailedTs = Long.valueOf(histories[i]).longValue();
                        }
                        i++;
                    } catch (IllegalArgumentException e) {
                        Log.d("HwSelfCureEngine", "string2InternetSelfCureHistoryInfo IllegalArgumentException!");
                    }
                }
            }
        }
        return info;
    }

    public static String internetSelfCureHistoryInfo2String(InternetSelfCureHistoryInfo info) {
        StringBuilder strHistory = new StringBuilder();
        if (info != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(String.valueOf(info.dnsSelfCureFailedCnt));
            stringBuilder.append("|");
            strHistory.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(String.valueOf(info.lastDnsSelfCureFailedTs));
            stringBuilder.append("|");
            strHistory.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(String.valueOf(info.renewDhcpSelfCureFailedCnt));
            stringBuilder.append("|");
            strHistory.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(String.valueOf(info.lastRenewDhcpSelfCureFailedTs));
            stringBuilder.append("|");
            strHistory.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(String.valueOf(info.staticIpSelfCureFailedCnt));
            stringBuilder.append("|");
            strHistory.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(String.valueOf(info.lastStaticIpSelfCureFailedTs));
            stringBuilder.append("|");
            strHistory.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(String.valueOf(info.reassocSelfCureFailedCnt));
            stringBuilder.append("|");
            strHistory.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(String.valueOf(info.lastReassocSelfCureFailedTs));
            stringBuilder.append("|");
            strHistory.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(String.valueOf(info.resetSelfCureFailedCnt));
            stringBuilder.append("|");
            strHistory.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(String.valueOf(info.lastResetSelfCureFailedTs));
            stringBuilder.append("|");
            strHistory.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(String.valueOf(info.reassocSelfCureConnectFailedCnt));
            stringBuilder.append("|");
            strHistory.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(String.valueOf(info.lastReassocSelfCureConnectFailedTs));
            stringBuilder.append("|");
            strHistory.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(String.valueOf(info.resetSelfCureConnectFailedCnt));
            stringBuilder.append("|");
            strHistory.append(stringBuilder.toString());
            strHistory.append(String.valueOf(info.lastResetSelfCureConnectFailedTs));
        }
        return strHistory.toString();
    }

    public static boolean selectedSelfCureAcceptable(InternetSelfCureHistoryInfo historyInfo, int requestCureLevel) {
        InternetSelfCureHistoryInfo internetSelfCureHistoryInfo = historyInfo;
        int i = requestCureLevel;
        if (internetSelfCureHistoryInfo == null) {
            return false;
        }
        long currentMs = System.currentTimeMillis();
        if (i != 201) {
            boolean z = true;
            if (i == 202) {
                if (internetSelfCureHistoryInfo.renewDhcpSelfCureFailedCnt >= 0) {
                    return z;
                }
            } else if (i == 203) {
                if (internetSelfCureHistoryInfo.staticIpSelfCureFailedCnt <= 4 || ((internetSelfCureHistoryInfo.staticIpSelfCureFailedCnt == 5 && currentMs - internetSelfCureHistoryInfo.lastStaticIpSelfCureFailedTs > 86400000) || ((internetSelfCureHistoryInfo.staticIpSelfCureFailedCnt == 6 && currentMs - internetSelfCureHistoryInfo.lastStaticIpSelfCureFailedTs > 259200000) || (internetSelfCureHistoryInfo.staticIpSelfCureFailedCnt >= 7 && currentMs - internetSelfCureHistoryInfo.lastStaticIpSelfCureFailedTs > 432000000)))) {
                    return true;
                }
            } else if (i == 204) {
                if ((internetSelfCureHistoryInfo.reassocSelfCureFailedCnt == 0 || ((internetSelfCureHistoryInfo.reassocSelfCureFailedCnt == 1 && currentMs - internetSelfCureHistoryInfo.lastReassocSelfCureFailedTs > 86400000) || ((internetSelfCureHistoryInfo.reassocSelfCureFailedCnt == 2 && currentMs - internetSelfCureHistoryInfo.lastReassocSelfCureFailedTs > 259200000) || (internetSelfCureHistoryInfo.reassocSelfCureFailedCnt >= 3 && currentMs - internetSelfCureHistoryInfo.lastReassocSelfCureFailedTs > 432000000)))) && allowSelfCure(historyInfo, requestCureLevel)) {
                    return true;
                }
            } else if (i == 205 && ((internetSelfCureHistoryInfo.resetSelfCureFailedCnt <= 1 || ((internetSelfCureHistoryInfo.resetSelfCureFailedCnt == 2 && currentMs - internetSelfCureHistoryInfo.lastResetSelfCureFailedTs > 86400000) || ((internetSelfCureHistoryInfo.resetSelfCureFailedCnt == 3 && currentMs - internetSelfCureHistoryInfo.lastResetSelfCureFailedTs > 259200000) || (internetSelfCureHistoryInfo.resetSelfCureFailedCnt >= 4 && currentMs - internetSelfCureHistoryInfo.lastResetSelfCureFailedTs > 432000000)))) && allowSelfCure(historyInfo, requestCureLevel))) {
                return true;
            }
        } else if (internetSelfCureHistoryInfo.dnsSelfCureFailedCnt == 0 || ((internetSelfCureHistoryInfo.dnsSelfCureFailedCnt == 1 && currentMs - internetSelfCureHistoryInfo.lastDnsSelfCureFailedTs > 86400000) || ((internetSelfCureHistoryInfo.dnsSelfCureFailedCnt == 2 && currentMs - internetSelfCureHistoryInfo.lastDnsSelfCureFailedTs > 259200000) || (internetSelfCureHistoryInfo.dnsSelfCureFailedCnt >= 3 && currentMs - internetSelfCureHistoryInfo.lastDnsSelfCureFailedTs > 432000000)))) {
            return true;
        }
        return false;
    }

    private static boolean allowSelfCure(InternetSelfCureHistoryInfo historyInfo, int requestCureLevel) {
        if (historyInfo == null) {
            return false;
        }
        long currentMs = System.currentTimeMillis();
        if (requestCureLevel == RESET_LEVEL_MIDDLE_REASSOC) {
            if (historyInfo.reassocSelfCureConnectFailedCnt == 0 || (historyInfo.reassocSelfCureConnectFailedCnt >= 1 && currentMs - historyInfo.lastReassocSelfCureConnectFailedTs > 86400000)) {
                return true;
            }
        } else if (requestCureLevel != RESET_LEVEL_HIGH_RESET || (historyInfo.resetSelfCureConnectFailedCnt != 0 && (historyInfo.resetSelfCureConnectFailedCnt < 1 || currentMs - historyInfo.lastResetSelfCureConnectFailedTs <= 86400000))) {
            return false;
        } else {
            return true;
        }
        return false;
    }

    public static void updateSelfCureHistoryInfo(InternetSelfCureHistoryInfo historyInfo, int requestCureLevel, boolean success) {
        if (historyInfo != null) {
            long currentMs = System.currentTimeMillis();
            if (requestCureLevel == RESET_LEVEL_LOW_1_DNS) {
                if (success) {
                    historyInfo.dnsSelfCureFailedCnt = 0;
                    historyInfo.lastDnsSelfCureFailedTs = 0;
                } else {
                    historyInfo.dnsSelfCureFailedCnt++;
                    historyInfo.lastDnsSelfCureFailedTs = currentMs;
                }
            } else if (requestCureLevel == RESET_LEVEL_LOW_2_RENEW_DHCP || requestCureLevel == RESET_LEVEL_DEAUTH_BSSID) {
                if (success) {
                    historyInfo.renewDhcpSelfCureFailedCnt = 0;
                    historyInfo.lastRenewDhcpSelfCureFailedTs = 0;
                } else {
                    historyInfo.renewDhcpSelfCureFailedCnt++;
                    historyInfo.lastRenewDhcpSelfCureFailedTs = currentMs;
                }
            } else if (requestCureLevel == RESET_LEVEL_LOW_3_STATIC_IP) {
                if (success) {
                    historyInfo.staticIpSelfCureFailedCnt = 0;
                    historyInfo.lastStaticIpSelfCureFailedTs = 0;
                } else {
                    historyInfo.staticIpSelfCureFailedCnt++;
                    historyInfo.lastStaticIpSelfCureFailedTs = currentMs;
                }
            } else if (requestCureLevel == RESET_LEVEL_MIDDLE_REASSOC) {
                if (success) {
                    historyInfo.reassocSelfCureFailedCnt = 0;
                    historyInfo.lastReassocSelfCureFailedTs = 0;
                } else {
                    historyInfo.reassocSelfCureFailedCnt++;
                    historyInfo.lastReassocSelfCureFailedTs = currentMs;
                }
            } else if (requestCureLevel == RESET_LEVEL_HIGH_RESET) {
                if (success) {
                    historyInfo.resetSelfCureFailedCnt = 0;
                    historyInfo.lastResetSelfCureFailedTs = 0;
                } else {
                    historyInfo.resetSelfCureFailedCnt++;
                    historyInfo.lastResetSelfCureFailedTs = currentMs;
                }
            }
        }
    }

    public static void updateSelfCureConnectHistoryInfo(InternetSelfCureHistoryInfo historyInfo, int requestCureLevel, boolean success) {
        if (historyInfo != null) {
            long currentMs = System.currentTimeMillis();
            if (requestCureLevel == RESET_LEVEL_MIDDLE_REASSOC) {
                if (success) {
                    historyInfo.reassocSelfCureConnectFailedCnt = 0;
                    historyInfo.lastReassocSelfCureConnectFailedTs = 0;
                } else {
                    historyInfo.reassocSelfCureConnectFailedCnt++;
                    historyInfo.lastReassocSelfCureConnectFailedTs = currentMs;
                }
            } else if (requestCureLevel == RESET_LEVEL_HIGH_RESET) {
                if (success) {
                    historyInfo.resetSelfCureConnectFailedCnt = 0;
                    historyInfo.lastResetSelfCureConnectFailedTs = 0;
                } else {
                    historyInfo.resetSelfCureConnectFailedCnt++;
                    historyInfo.lastResetSelfCureConnectFailedTs = currentMs;
                }
            }
        }
    }

    public static InetAddress getNextIpAddr(InetAddress gateway, InetAddress currentAddr, ArrayList<InetAddress> testedAddr) {
        ArrayList<InetAddress> arrayList = testedAddr;
        if (!(gateway == null || currentAddr == null || arrayList == null)) {
            int getCnt = 1;
            byte[] ipAddr = currentAddr.getAddress();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getNextIpAddr, gateway = ");
            stringBuilder.append(gateway.getHostAddress());
            Log.d("HwSelfCureEngine", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("getNextIpAddr, currentAddr = ");
            stringBuilder.append(currentAddr.getHostAddress());
            Log.d("HwSelfCureEngine", stringBuilder.toString());
            int newip = -1;
            int MIN = 101;
            while (true) {
                int MIN2 = MIN;
                int getCnt2 = getCnt + 1;
                if (getCnt >= 10) {
                    break;
                }
                boolean reduplicate = false;
                newip = new SecureRandom().nextInt(100) + 101;
                if (!(newip == (gateway.getAddress()[3] & 255) || newip == (ipAddr[3] & 255))) {
                    for (int i = 0; i < testedAddr.size(); i++) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("getNextIpAddr, testedAddr = ");
                        stringBuilder.append(((InetAddress) arrayList.get(i)).getHostAddress());
                        Log.d("HwSelfCureEngine", stringBuilder.toString());
                        if (newip == (((InetAddress) arrayList.get(i)).getAddress()[3] & 255)) {
                            reduplicate = true;
                            break;
                        }
                    }
                    if (newip > 0 && !reduplicate) {
                        break;
                    }
                }
                MIN = MIN2;
                getCnt = getCnt2;
            }
            if (newip > 1 && newip <= 250 && getCnt2 < 10) {
                ipAddr[3] = (byte) newip;
                try {
                    return InetAddress.getByAddress(ipAddr);
                } catch (UnknownHostException e) {
                    UnknownHostException unknownHostException = e;
                    Log.d("HwSelfCureEngine", "getNextIpAddr UnknownHostException!");
                }
            }
        }
        return null;
    }

    public static boolean isOnWlanSettings(Context context) {
        if (context != null) {
            return WifiProCommonUtils.isQueryActivityMatched(context, "com.android.settings.Settings$WifiSettingsActivity");
        }
        return false;
    }
}
