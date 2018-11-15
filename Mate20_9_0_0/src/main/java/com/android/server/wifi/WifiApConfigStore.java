package com.android.server.wifi;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.os.Binder;
import android.os.Environment;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public class WifiApConfigStore {
    @VisibleForTesting
    static final int AP_CHANNEL_DEFAULT = 0;
    private static final int AP_CONFIG_FILE_VERSION = 2;
    private static final String DEFAULT_AP_CONFIG_FILE;
    @VisibleForTesting
    static final int PSK_MAX_LEN = 63;
    @VisibleForTesting
    static final int PSK_MIN_LEN = 8;
    private static final int RAND_SSID_INT_MAX = 9999;
    private static final int RAND_SSID_INT_MIN = 1000;
    @VisibleForTesting
    static final int SSID_MAX_LEN = 32;
    @VisibleForTesting
    static final int SSID_MIN_LEN = 1;
    private static final String TAG = "WifiApConfigStore";
    private static final int WAPI_CERT_CONST_VALUE_IN_L = 5;
    private static final int WAPI_PSK_CONST_VALUE_IN_L = 4;
    private static final int WPA2_PSK_CONST_VALUE_IN_L = 6;
    private static String mMarketingName = SystemProperties.get("ro.config.marketing_name");
    private ArrayList<Integer> mAllowed2GChannel;
    private final String mApConfigFile;
    private final BackupManagerProxy mBackupManagerProxy;
    private final Context mContext;
    private boolean mRequiresApBandConversion;
    private WifiConfiguration mWifiApConfig;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Environment.getDataDirectory());
        stringBuilder.append("/misc/wifi/softap.conf");
        DEFAULT_AP_CONFIG_FILE = stringBuilder.toString();
    }

    WifiApConfigStore(Context context, BackupManagerProxy backupManagerProxy) {
        this(context, backupManagerProxy, DEFAULT_AP_CONFIG_FILE);
    }

    WifiApConfigStore(Context context, BackupManagerProxy backupManagerProxy, String apConfigFile) {
        this.mWifiApConfig = null;
        this.mAllowed2GChannel = null;
        int i = 0;
        this.mRequiresApBandConversion = false;
        this.mContext = context;
        this.mBackupManagerProxy = backupManagerProxy;
        this.mApConfigFile = apConfigFile;
        String ap2GChannelListStr = this.mContext.getResources().getString(17039847);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("2G band allowed channels are:");
        stringBuilder.append(ap2GChannelListStr);
        Log.d(str, stringBuilder.toString());
        if (ap2GChannelListStr != null) {
            this.mAllowed2GChannel = new ArrayList();
            String[] channelList = ap2GChannelListStr.split(",");
            int length = channelList.length;
            while (i < length) {
                this.mAllowed2GChannel.add(Integer.valueOf(Integer.parseInt(channelList[i])));
                i++;
            }
        }
        this.mRequiresApBandConversion = this.mContext.getResources().getBoolean(17957072);
        this.mWifiApConfig = loadApConfiguration(this.mApConfigFile);
        if (this.mWifiApConfig == null) {
            Log.d(TAG, "Fallback to use default AP configuration");
            this.mWifiApConfig = getDefaultApConfiguration();
            writeApConfiguration(this.mApConfigFile, this.mWifiApConfig);
        }
    }

    public synchronized WifiConfiguration getApConfiguration() {
        WifiConfiguration config = apBandCheckConvert(this.mWifiApConfig);
        if (this.mWifiApConfig != config) {
            Log.d(TAG, "persisted config was converted, need to resave it");
            this.mWifiApConfig = config;
            persistConfigAndTriggerBackupManagerProxy(this.mWifiApConfig);
        }
        return this.mWifiApConfig;
    }

    public synchronized void setApConfiguration(WifiConfiguration config) {
        if (config == null) {
            this.mWifiApConfig = getDefaultApConfiguration();
        } else {
            this.mWifiApConfig = apBandCheckConvert(config);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setApConfiguration, apBand =");
        stringBuilder.append(this.mWifiApConfig.apBand);
        Log.d(str, stringBuilder.toString());
        persistConfigAndTriggerBackupManagerProxy(this.mWifiApConfig);
    }

    public ArrayList<Integer> getAllowed2GChannel() {
        return this.mAllowed2GChannel;
    }

    private WifiConfiguration apBandCheckConvert(WifiConfiguration config) {
        WifiConfiguration convertedConfig;
        if (this.mRequiresApBandConversion) {
            if (config.apBand == 1) {
                Log.w(TAG, "Supplied ap config band was 5GHz only, converting to ANY");
                convertedConfig = new WifiConfiguration(config);
                convertedConfig.apBand = -1;
                convertedConfig.apChannel = 0;
                return convertedConfig;
            }
        } else if (config.apBand == -1) {
            Log.w(TAG, "Supplied ap config band was ANY, converting to 5GHz");
            convertedConfig = new WifiConfiguration(config);
            convertedConfig.apBand = 1;
            convertedConfig.apChannel = 0;
            return convertedConfig;
        }
        return config;
    }

    private void persistConfigAndTriggerBackupManagerProxy(WifiConfiguration config) {
        writeApConfiguration(this.mApConfigFile, this.mWifiApConfig);
        if (!TextUtils.isEmpty(mMarketingName)) {
            long ident = Binder.clearCallingIdentity();
            try {
                if (mMarketingName.equals(this.mWifiApConfig.SSID)) {
                    System.putInt(this.mContext.getContentResolver(), "softap_name_changed", 0);
                } else {
                    System.putInt(this.mContext.getContentResolver(), "softap_name_changed", 1);
                }
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        }
        this.mBackupManagerProxy.notifyDataChanged();
    }

    private WifiConfiguration loadApConfiguration(String filename) {
        IOException e;
        String str;
        StringBuilder stringBuilder;
        DataInputStream in = null;
        WifiConfiguration config = new WifiConfiguration();
        in = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));
        int version = in.readInt();
        if (version == 1 || version == 2) {
            long ident;
            try {
                config.SSID = in.readUTF();
                ident = Binder.clearCallingIdentity();
                if (!TextUtils.isEmpty(mMarketingName) && System.getInt(this.mContext.getContentResolver(), "softap_name_changed", 0) == 0) {
                    config.SSID = mMarketingName;
                }
                Binder.restoreCallingIdentity(ident);
                if (version >= 2) {
                    config.apBand = in.readInt();
                    config.apChannel = in.readInt();
                }
                int authType = mapApAuth(in.readInt(), version);
                config.allowedKeyManagement.set(authType);
                if (authType != 0) {
                    config.preSharedKey = in.readUTF();
                }
                try {
                    in.close();
                } catch (IOException e2) {
                    e = e2;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            } catch (IOException e3) {
                try {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error reading hotspot configuration ");
                    stringBuilder.append(e3);
                    Log.e(str, stringBuilder.toString());
                    config = null;
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e4) {
                            e3 = e4;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                        }
                    }
                } catch (Throwable th) {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e5) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Error closing hotspot configuration during read");
                            stringBuilder.append(e5);
                            Log.e(TAG, stringBuilder.toString());
                        }
                    }
                }
            } catch (Throwable th2) {
                Binder.restoreCallingIdentity(ident);
            }
            return config;
        }
        Log.e(TAG, "Bad version on hotspot configuration file");
        try {
            in.close();
        } catch (IOException e6) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Error closing hotspot configuration during read");
            stringBuilder2.append(e6);
            Log.e(str2, stringBuilder2.toString());
        }
        return null;
        stringBuilder.append("Error closing hotspot configuration during read");
        stringBuilder.append(e3);
        Log.e(str, stringBuilder.toString());
        return config;
    }

    private static void writeApConfiguration(String filename, WifiConfiguration config) {
        DataOutputStream out;
        try {
            out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
            out.writeInt(2);
            out.writeUTF(config.SSID);
            out.writeInt(config.apBand);
            out.writeInt(config.apChannel);
            int authType = config.getAuthType();
            out.writeInt(authType);
            if (!(authType == 0 || config.preSharedKey == null)) {
                out.writeUTF(config.preSharedKey);
            }
            out.close();
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error writing hotspot configuration");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            r1.addSuppressed(th);
        }
    }

    private WifiConfiguration getDefaultApConfiguration() {
        WifiConfiguration config = new WifiConfiguration();
        config.apBand = 0;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.mContext.getResources().getString(17041410));
        stringBuilder.append("_");
        stringBuilder.append(getRandomIntForDefaultSsid());
        config.SSID = stringBuilder.toString();
        config.allowedKeyManagement.set(4);
        String randomUUID = UUID.randomUUID().toString();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(randomUUID.substring(0, 8));
        stringBuilder2.append(randomUUID.substring(9, 13));
        config.preSharedKey = stringBuilder2.toString();
        config.SSID = HwWifiServiceFactory.getHwWifiServiceManager().getCustWifiApDefaultName(config);
        return config;
    }

    private static int getRandomIntForDefaultSsid() {
        return new Random().nextInt(9000) + RAND_SSID_INT_MIN;
    }

    private int mapApAuth(int softApAuthType, int softApversion) {
        if (softApversion == 2) {
            return softApAuthType;
        }
        String connectivity_chipType = SystemProperties.get("ro.connectivity.chiptype");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("connectivity_chipType = ");
        stringBuilder.append(connectivity_chipType);
        Log.d(str, stringBuilder.toString());
        if (softApversion != 1 || !"Qualcomm".equalsIgnoreCase(connectivity_chipType)) {
            return softApAuthType;
        }
        switch (softApAuthType) {
            case 4:
                return 8;
            case 5:
                return 9;
            case 6:
                return 4;
            default:
                return softApAuthType;
        }
    }

    public static WifiConfiguration generateLocalOnlyHotspotConfig(Context context) {
        WifiConfiguration config = new WifiConfiguration();
        config.apBand = 0;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(context.getResources().getString(17041395));
        stringBuilder.append("_");
        stringBuilder.append(getRandomIntForDefaultSsid());
        config.SSID = stringBuilder.toString();
        config.allowedKeyManagement.set(4);
        config.networkId = -2;
        String randomUUID = UUID.randomUUID().toString();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(randomUUID.substring(0, 8));
        stringBuilder2.append(randomUUID.substring(9, 13));
        config.preSharedKey = stringBuilder2.toString();
        return config;
    }

    private static boolean validateApConfigSsid(String ssid) {
        if (TextUtils.isEmpty(ssid)) {
            Log.d(TAG, "SSID for softap configuration must be set.");
            return false;
        } else if (ssid.length() < 1 || ssid.length() > 32) {
            Log.d(TAG, "SSID for softap configuration string size must be at least 1 and not more than 32");
            return false;
        } else {
            try {
                ssid.getBytes(StandardCharsets.UTF_8);
                return true;
            } catch (IllegalArgumentException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("softap config SSID verification failed: malformed string ");
                stringBuilder.append(ssid);
                Log.e(str, stringBuilder.toString());
                return false;
            }
        }
    }

    private static boolean validateApConfigPreSharedKey(String preSharedKey) {
        if (preSharedKey.length() < 8 || preSharedKey.length() > 63) {
            Log.d(TAG, "softap network password string size must be at least 8 and no more than 63");
            return false;
        }
        try {
            preSharedKey.getBytes(StandardCharsets.UTF_8);
            return true;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "softap network password verification failed: malformed string");
            return false;
        }
    }

    static boolean validateApWifiConfiguration(WifiConfiguration apConfig) {
        if (!validateApConfigSsid(apConfig.SSID)) {
            return false;
        }
        if (apConfig.allowedKeyManagement == null) {
            Log.d(TAG, "softap config key management bitset was null");
            return false;
        }
        String preSharedKey = apConfig.preSharedKey;
        boolean hasPreSharedKey = TextUtils.isEmpty(preSharedKey) ^ true;
        try {
            int authType = apConfig.getAuthType();
            if (authType == 0) {
                if (hasPreSharedKey) {
                    Log.d(TAG, "open softap network should not have a password");
                    return false;
                }
            } else if (authType != 4) {
                Log.d(TAG, "softap configs must either be open or WPA2 PSK networks");
                return false;
            } else if (!hasPreSharedKey) {
                Log.d(TAG, "softap network password must be set");
                return false;
            } else if (!validateApConfigPreSharedKey(preSharedKey)) {
                return false;
            }
            return true;
        } catch (IllegalStateException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to get AuthType for softap config: ");
            stringBuilder.append(e.getMessage());
            Log.d(str, stringBuilder.toString());
            return false;
        }
    }
}
