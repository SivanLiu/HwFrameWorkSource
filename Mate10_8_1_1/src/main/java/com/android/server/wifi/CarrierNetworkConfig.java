package com.android.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Base64;
import android.util.Log;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CarrierNetworkConfig {
    private static final int CONFIG_ELEMENT_SIZE = 2;
    private static final int EAP_TYPE_INDEX = 1;
    private static final int ENCODED_SSID_INDEX = 0;
    private static final String NETWORK_CONFIG_SEPARATOR = ",";
    private static final String TAG = "CarrierNetworkConfig";
    private final Map<String, NetworkInfo> mCarrierNetworkMap = new HashMap();

    private static class NetworkInfo {
        final String mCarrierName;
        final int mEapType;

        NetworkInfo(int eapType, String carrierName) {
            this.mEapType = eapType;
            this.mCarrierName = carrierName;
        }
    }

    public CarrierNetworkConfig(Context context) {
        updateNetworkConfig(context);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        context.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                CarrierNetworkConfig.this.updateNetworkConfig(context);
            }
        }, filter);
    }

    public boolean isCarrierNetwork(String ssid) {
        return this.mCarrierNetworkMap.containsKey(ssid);
    }

    public int getNetworkEapType(String ssid) {
        NetworkInfo info = (NetworkInfo) this.mCarrierNetworkMap.get(ssid);
        return info == null ? -1 : info.mEapType;
    }

    public String getCarrierName(String ssid) {
        NetworkInfo info = (NetworkInfo) this.mCarrierNetworkMap.get(ssid);
        if (info == null) {
            return null;
        }
        return info.mCarrierName;
    }

    private void updateNetworkConfig(Context context) {
        this.mCarrierNetworkMap.clear();
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) context.getSystemService("carrier_config");
        if (carrierConfigManager != null) {
            SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService("telephony_subscription_service");
            if (subscriptionManager != null) {
                List<SubscriptionInfo> subInfoList = subscriptionManager.getActiveSubscriptionInfoList();
                if (subInfoList != null) {
                    for (SubscriptionInfo subInfo : subInfoList) {
                        CharSequence displayName = subInfo.getDisplayName();
                        if (displayName != null) {
                            processNetworkConfig(carrierConfigManager.getConfigForSubId(subInfo.getSubscriptionId()), displayName.toString());
                        } else {
                            processNetworkConfig(carrierConfigManager.getConfigForSubId(subInfo.getSubscriptionId()), "");
                            Log.e(TAG, "displayName is null with SubscriptionId:" + subInfo.getSubscriptionId());
                        }
                    }
                }
            }
        }
    }

    private void processNetworkConfig(PersistableBundle carrierConfig, String carrierName) {
        if (carrierConfig != null) {
            String[] networkConfigs = carrierConfig.getStringArray("carrier_wifi_string_array");
            if (networkConfigs != null) {
                for (String networkConfig : networkConfigs) {
                    String[] configArr = networkConfig.split(NETWORK_CONFIG_SEPARATOR);
                    if (configArr.length != 2) {
                        Log.e(TAG, "Ignore invalid config: " + networkConfig);
                    } else {
                        try {
                            String ssid = new String(Base64.decode(configArr[0], 0));
                            int eapType = parseEapType(Integer.parseInt(configArr[1]));
                            if (eapType == -1) {
                                Log.e(TAG, "Invalid EAP type: " + configArr[1]);
                            } else {
                                this.mCarrierNetworkMap.put(ssid, new NetworkInfo(eapType, carrierName));
                            }
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Failed to parse EAP type: " + e.getMessage());
                        } catch (IllegalArgumentException e2) {
                            Log.e(TAG, "Failed to decode SSID: " + e2.getMessage());
                        }
                    }
                }
            }
        }
    }

    private static int parseEapType(int eapType) {
        if (eapType == 18) {
            return 4;
        }
        if (eapType == 23) {
            return 5;
        }
        if (eapType == 50) {
            return 6;
        }
        return -1;
    }
}
