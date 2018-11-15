package com.android.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CarrierNetworkConfig {
    private static final int CONFIG_ELEMENT_SIZE = 2;
    private static final Uri CONTENT_URI = Uri.parse("content://carrier_information/carrier");
    private static final int EAP_TYPE_INDEX = 1;
    private static final int ENCODED_SSID_INDEX = 0;
    private static final String NETWORK_CONFIG_SEPARATOR = ",";
    private static final String TAG = "CarrierNetworkConfig";
    private final Map<String, NetworkInfo> mCarrierNetworkMap = new HashMap();
    private boolean mIsCarrierImsiEncryptionInfoAvailable = false;

    private static class NetworkInfo {
        final String mCarrierName;
        final int mEapType;

        NetworkInfo(int eapType, String carrierName) {
            this.mEapType = eapType;
            this.mCarrierName = carrierName;
        }
    }

    public CarrierNetworkConfig(final Context context, Looper looper, FrameworkFacade framework) {
        updateNetworkConfig(context);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        context.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                CarrierNetworkConfig.this.updateNetworkConfig(context);
            }
        }, filter);
        framework.registerContentObserver(context, CONTENT_URI, false, new ContentObserver(new Handler(looper)) {
            public void onChange(boolean selfChange) {
                CarrierNetworkConfig.this.updateNetworkConfig(context);
            }
        });
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
        return info == null ? null : info.mCarrierName;
    }

    public boolean isCarrierEncryptionInfoAvailable() {
        return this.mIsCarrierImsiEncryptionInfoAvailable;
    }

    private boolean verifyCarrierImsiEncryptionInfoIsAvailable(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        if (telephonyManager == null) {
            return false;
        }
        try {
            if (telephonyManager.getCarrierInfoForImsiEncryption(2) == null) {
                return false;
            }
            return true;
        } catch (RuntimeException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to get imsi encryption info: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    private void updateNetworkConfig(Context context) {
        this.mIsCarrierImsiEncryptionInfoAvailable = verifyCarrierImsiEncryptionInfoIsAvailable(context);
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
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("displayName is null with SubscriptionId:");
                            stringBuilder.append(subInfo.getSubscriptionId());
                            Log.e(str, stringBuilder.toString());
                        }
                    }
                }
            }
        }
    }

    private void processNetworkConfig(PersistableBundle carrierConfig, String carrierName) {
        String str;
        StringBuilder stringBuilder;
        if (carrierConfig != null) {
            String[] networkConfigs = carrierConfig.getStringArray("carrier_wifi_string_array");
            if (networkConfigs != null) {
                for (String networkConfig : networkConfigs) {
                    String[] configArr = networkConfig.split(NETWORK_CONFIG_SEPARATOR);
                    String str2;
                    if (configArr.length != 2) {
                        str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Ignore invalid config: ");
                        stringBuilder2.append(networkConfig);
                        Log.e(str2, stringBuilder2.toString());
                    } else {
                        try {
                            str2 = new String(Base64.decode(configArr[0], 0));
                            int eapType = parseEapType(Integer.parseInt(configArr[1]));
                            if (eapType == -1) {
                                String str3 = TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("Invalid EAP type: ");
                                stringBuilder3.append(configArr[1]);
                                Log.e(str3, stringBuilder3.toString());
                            } else {
                                this.mCarrierNetworkMap.put(str2, new NetworkInfo(eapType, carrierName));
                            }
                        } catch (NumberFormatException e) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Failed to parse EAP type: ");
                            stringBuilder.append(e.getMessage());
                            Log.e(str, stringBuilder.toString());
                        } catch (IllegalArgumentException e2) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Failed to decode SSID: ");
                            stringBuilder.append(e2.getMessage());
                            Log.e(str, stringBuilder.toString());
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
