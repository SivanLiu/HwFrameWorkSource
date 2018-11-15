package com.android.server.wifi;

import android.net.IpConfiguration;
import android.net.wifi.WifiConfiguration;
import android.os.Environment;
import android.util.Log;
import android.util.SparseArray;
import com.android.server.net.IpConfigStore;
import com.android.server.wifi.hotspot2.LegacyPasspointConfig;
import com.android.server.wifi.hotspot2.LegacyPasspointConfigParser;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class WifiConfigStoreLegacy {
    private static final File IP_CONFIG_FILE = new File(Environment.getDataMiscDirectory(), "wifi/ipconfig.txt");
    private static final File NETWORK_HISTORY_FILE = new File(WifiNetworkHistory.NETWORK_HISTORY_CONFIG_FILE);
    private static final File PPS_FILE = new File(Environment.getDataMiscDirectory(), "wifi/PerProviderSubscription.conf");
    private static final String TAG = "WifiConfigStoreLegacy";
    private final IpConfigStoreWrapper mIpconfigStoreWrapper;
    private final LegacyPasspointConfigParser mPasspointConfigParser;
    private final WifiNative mWifiNative;
    private final WifiNetworkHistory mWifiNetworkHistory;

    public static class IpConfigStoreWrapper {
        public SparseArray<IpConfiguration> readIpAndProxyConfigurations(String filePath) {
            return IpConfigStore.readIpAndProxyConfigurations(filePath);
        }
    }

    private interface MaskedWpaSupplicantFieldSetter {
        void setValue(WifiConfiguration wifiConfiguration, String str);
    }

    public static class WifiConfigStoreDataLegacy {
        private List<WifiConfiguration> mConfigurations;
        private Set<String> mDeletedEphemeralSSIDs;

        WifiConfigStoreDataLegacy(List<WifiConfiguration> configurations, Set<String> deletedEphemeralSSIDs) {
            this.mConfigurations = configurations;
            this.mDeletedEphemeralSSIDs = deletedEphemeralSSIDs;
        }

        public List<WifiConfiguration> getConfigurations() {
            return this.mConfigurations;
        }

        public Set<String> getDeletedEphemeralSSIDs() {
            return this.mDeletedEphemeralSSIDs;
        }
    }

    WifiConfigStoreLegacy(WifiNetworkHistory wifiNetworkHistory, WifiNative wifiNative, IpConfigStoreWrapper ipConfigStore, LegacyPasspointConfigParser passpointConfigParser) {
        this.mWifiNetworkHistory = wifiNetworkHistory;
        this.mWifiNative = wifiNative;
        this.mIpconfigStoreWrapper = ipConfigStore;
        this.mPasspointConfigParser = passpointConfigParser;
    }

    private static WifiConfiguration lookupWifiConfigurationUsingConfigKeyHash(Map<String, WifiConfiguration> configurationMap, int hashCode) {
        for (Entry<String, WifiConfiguration> entry : configurationMap.entrySet()) {
            if (entry.getKey() != null && ((String) entry.getKey()).hashCode() == hashCode) {
                return (WifiConfiguration) entry.getValue();
            }
        }
        return null;
    }

    private void loadFromIpConfigStore(Map<String, WifiConfiguration> configurationMap) {
        SparseArray<IpConfiguration> ipConfigurations = this.mIpconfigStoreWrapper.readIpAndProxyConfigurations(IP_CONFIG_FILE.getAbsolutePath());
        if (ipConfigurations == null || ipConfigurations.size() == 0) {
            Log.w(TAG, "No ip configurations found in ipconfig store");
            return;
        }
        for (int i = 0; i < ipConfigurations.size(); i++) {
            int id = ipConfigurations.keyAt(i);
            WifiConfiguration config = lookupWifiConfigurationUsingConfigKeyHash(configurationMap, id);
            if (config == null || config.ephemeral) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("configuration found for missing network, nid=");
                stringBuilder.append(id);
                stringBuilder.append(", ignored, networks.size=");
                stringBuilder.append(Integer.toString(ipConfigurations.size()));
                Log.w(str, stringBuilder.toString());
            } else {
                config.setIpConfiguration((IpConfiguration) ipConfigurations.valueAt(i));
            }
        }
    }

    private void loadFromNetworkHistory(Map<String, WifiConfiguration> configurationMap, Set<String> deletedEphemeralSSIDs) {
        this.mWifiNetworkHistory.readNetworkHistory(configurationMap, new HashMap(), deletedEphemeralSSIDs);
    }

    private void loadFromWpaSupplicant(Map<String, WifiConfiguration> configurationMap, SparseArray<Map<String, String>> networkExtras) {
        if (!this.mWifiNative.migrateNetworksFromSupplicant(this.mWifiNative.getClientInterfaceName(), configurationMap, networkExtras)) {
            Log.wtf(TAG, "Failed to load wifi configurations from wpa_supplicant");
        } else if (configurationMap.isEmpty()) {
            Log.w(TAG, "No wifi configurations found in wpa_supplicant");
        }
    }

    private void loadFromPasspointConfigStore(Map<String, WifiConfiguration> configurationMap, SparseArray<Map<String, String>> networkExtras) {
        Map<String, LegacyPasspointConfig> passpointConfigMap = null;
        try {
            passpointConfigMap = this.mPasspointConfigParser.parseConfig(PPS_FILE.getAbsolutePath());
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to read/parse Passpoint config file: ");
            stringBuilder.append(e.getMessage());
            Log.w(str, stringBuilder.toString());
        }
        List<String> entriesToBeRemoved = new ArrayList();
        for (Entry<String, WifiConfiguration> entry : configurationMap.entrySet()) {
            WifiConfiguration wifiConfig = (WifiConfiguration) entry.getValue();
            if (wifiConfig.enterpriseConfig != null) {
                if (wifiConfig.enterpriseConfig.getEapMethod() != -1) {
                    Map<String, String> extras = (Map) networkExtras.get(wifiConfig.networkId);
                    if (extras != null) {
                        if (extras.containsKey("fqdn")) {
                            String fqdn = (String) ((Map) networkExtras.get(wifiConfig.networkId)).get("fqdn");
                            if (passpointConfigMap == null || !passpointConfigMap.containsKey(fqdn)) {
                                entriesToBeRemoved.add((String) entry.getKey());
                            } else {
                                LegacyPasspointConfig passpointConfig = (LegacyPasspointConfig) passpointConfigMap.get(fqdn);
                                wifiConfig.isLegacyPasspointConfig = true;
                                wifiConfig.FQDN = fqdn;
                                wifiConfig.providerFriendlyName = passpointConfig.mFriendlyName;
                                if (passpointConfig.mRoamingConsortiumOis != null) {
                                    wifiConfig.roamingConsortiumIds = Arrays.copyOf(passpointConfig.mRoamingConsortiumOis, passpointConfig.mRoamingConsortiumOis.length);
                                }
                                if (passpointConfig.mImsi != null) {
                                    wifiConfig.enterpriseConfig.setPlmn(passpointConfig.mImsi);
                                }
                                if (passpointConfig.mRealm != null) {
                                    wifiConfig.enterpriseConfig.setRealm(passpointConfig.mRealm);
                                }
                            }
                        }
                    }
                }
            }
        }
        for (String key : entriesToBeRemoved) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Remove incomplete Passpoint configuration: ");
            stringBuilder2.append(key);
            Log.w(str2, stringBuilder2.toString());
            configurationMap.remove(key);
        }
    }

    public WifiConfigStoreDataLegacy read() {
        Map<String, WifiConfiguration> configurationMap = new HashMap();
        SparseArray<Map<String, String>> networkExtras = new SparseArray();
        Set<String> deletedEphemeralSSIDs = new HashSet();
        loadFromWpaSupplicant(configurationMap, networkExtras);
        loadFromNetworkHistory(configurationMap, deletedEphemeralSSIDs);
        loadFromIpConfigStore(configurationMap);
        loadFromPasspointConfigStore(configurationMap, networkExtras);
        return new WifiConfigStoreDataLegacy(new ArrayList(configurationMap.values()), deletedEphemeralSSIDs);
    }

    public boolean areStoresPresent() {
        return new File(WifiNetworkHistory.NETWORK_HISTORY_CONFIG_FILE).exists();
    }

    public boolean removeStores() {
        if (!this.mWifiNative.removeAllNetworks(this.mWifiNative.getClientInterfaceName())) {
            Log.e(TAG, "Removing networks from wpa_supplicant failed");
        }
        if (!IP_CONFIG_FILE.delete()) {
            Log.e(TAG, "Removing ipconfig.txt failed");
        }
        if (!NETWORK_HISTORY_FILE.delete()) {
            Log.e(TAG, "Removing networkHistory.txt failed");
        }
        if (!PPS_FILE.delete()) {
            Log.e(TAG, "Removing PerProviderSubscription.conf failed");
        }
        Log.i(TAG, "All legacy stores removed!");
        return true;
    }
}
