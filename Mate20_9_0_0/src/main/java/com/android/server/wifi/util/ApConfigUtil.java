package com.android.server.wifi.util;

import android.net.wifi.WifiConfiguration;
import android.util.Log;
import com.android.server.wifi.WifiNative;
import java.util.ArrayList;
import java.util.Random;

public class ApConfigUtil {
    public static final int DEFAULT_AP_BAND = 0;
    public static final int DEFAULT_AP_CHANNEL = 6;
    public static final int ERROR_GENERIC = 2;
    public static final int ERROR_NO_CHANNEL = 1;
    public static final int SUCCESS = 0;
    private static final String TAG = "ApConfigUtil";
    private static final Random sRandom = new Random();

    public static int convertFrequencyToChannel(int frequency) {
        if (frequency >= 2412 && frequency <= 2472) {
            return ((frequency - 2412) / 5) + 1;
        }
        if (frequency == 2484) {
            return 14;
        }
        if (frequency < 5170 || frequency > 5825) {
            return -1;
        }
        return ((frequency - 5170) / 5) + 34;
    }

    public static int chooseApChannel(int apBand, ArrayList<Integer> allowed2GChannels, int[] allowed5GFreqList) {
        if (apBand != 0 && apBand != 1 && apBand != -1) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid band: ");
            stringBuilder.append(apBand);
            Log.e(str, stringBuilder.toString());
            return -1;
        } else if (apBand == 0 || apBand == -1) {
            if (allowed2GChannels != null && allowed2GChannels.size() != 0) {
                return ((Integer) allowed2GChannels.get(sRandom.nextInt(allowed2GChannels.size()))).intValue();
            }
            Log.d(TAG, "2GHz allowed channel list not specified");
            return 6;
        } else if (allowed5GFreqList == null || allowed5GFreqList.length <= 0) {
            Log.e(TAG, "No available channels on 5GHz band");
            return -1;
        } else {
            int j = 0;
            int len = allowed5GFreqList.length;
            for (int i = 0; i < allowed5GFreqList.length; i++) {
                if (convertFrequencyToChannel(allowed5GFreqList[i]) == 165) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("updateApChannelConfig exclude AP_CHANNEL_165_20MHZ ");
                    stringBuilder2.append(allowed5GFreqList[i]);
                    Log.w(str2, stringBuilder2.toString());
                    len--;
                }
            }
            int[] allowed5GFreqListNew = new int[len];
            for (int i2 = 0; i2 < allowed5GFreqList.length; i2++) {
                if (convertFrequencyToChannel(allowed5GFreqList[i2]) != 165) {
                    int j2 = j + 1;
                    allowed5GFreqListNew[j] = allowed5GFreqList[i2];
                    j = j2;
                }
            }
            if (allowed5GFreqListNew.length > 0) {
                allowed5GFreqList = allowed5GFreqListNew;
            }
            return convertFrequencyToChannel(allowed5GFreqList[sRandom.nextInt(allowed5GFreqList.length)]);
        }
    }

    public static int updateApChannelConfig(WifiNative wifiNative, String countryCode, ArrayList<Integer> allowed2GChannels, WifiConfiguration config) {
        if (!wifiNative.isHalStarted()) {
            config.apBand = 0;
            config.apChannel = 6;
            return 0;
        } else if (config.apBand == 1 && countryCode == null) {
            Log.e(TAG, "5GHz band is not allowed without country code");
            return 2;
        } else {
            if (config.apChannel == 0) {
                config.apChannel = chooseApChannel(config.apBand, allowed2GChannels, wifiNative.getChannelsForBand(2));
                if (config.apChannel == -1) {
                    Log.e(TAG, "Failed to get available channel.");
                    return 1;
                }
            }
            return 0;
        }
    }
}
