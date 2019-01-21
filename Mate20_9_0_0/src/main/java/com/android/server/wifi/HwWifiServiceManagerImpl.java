package com.android.server.wifi;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager.SoftApCallback;
import android.net.wifi.WifiScanner;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserManager;
import android.provider.SettingsEx.Systemex;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import com.android.internal.app.IBatteryStats;
import com.android.internal.util.AsyncChannel;
import com.android.server.wifi.hotspot2.PasspointNetworkEvaluator;
import com.android.server.wifi.p2p.HwWifiP2pService;
import com.android.server.wifi.p2p.WifiP2pServiceImpl;
import com.android.server.wifi.scanner.WifiScannerImpl.WifiScannerImplFactory;
import com.android.server.wifi.scanner.WifiScanningServiceImpl;
import com.android.server.wifi.util.WifiPermissionsUtil;
import com.android.server.wifi.util.WifiPermissionsWrapper;
import com.android.server.wifi.wifipro.HwSavedNetworkEvaluator;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class HwWifiServiceManagerImpl implements HwWifiServiceManager {
    private static final String TAG = "HwWifiServiceManagerImpl";
    private static HwWifiServiceManager mInstance = new HwWifiServiceManagerImpl();
    private HwWifiP2pService mHwWifiP2PService = null;

    public WifiServiceImpl createHwWifiService(Context context, WifiInjector wifiInjector, AsyncChannel asyncChannel) {
        return new HwWifiService(context, wifiInjector, asyncChannel);
    }

    public WifiController createHwWifiController(Context context, WifiStateMachine wsm, Looper wifiStateMachineLooper, WifiSettingsStore wss, Looper wifiServiceLooper, FrameworkFacade f, WifiStateMachinePrime wsmp) {
        return new HwWifiController(context, wsm, wifiStateMachineLooper, wss, wifiServiceLooper, f, wsmp);
    }

    public static HwWifiServiceManager getDefault() {
        return mInstance;
    }

    public boolean custApConfiguration(WifiApConfigStore s, WifiConfiguration config, Context context) {
        String custConf = Systemex.getString(context.getContentResolver(), "hw_softap_default");
        if (!(custConf == null || custConf.equals(""))) {
            String[] args = custConf.split(",");
            if (3 == args.length) {
                String randomUUID;
                StringBuilder stringBuilder;
                try {
                    if (args[0].equals(" ")) {
                        config.SSID = context.getString(17041411);
                    } else {
                        config.SSID = args[0];
                    }
                    if (args[1].equals(" ")) {
                        config.allowedKeyManagement.set(4);
                    } else {
                        config.allowedKeyManagement.set(Integer.valueOf(args[1]).intValue());
                    }
                    if (args[2].equals(" ")) {
                        randomUUID = UUID.randomUUID().toString();
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(randomUUID.substring(0, 8));
                        stringBuilder.append(randomUUID.substring(9, 13));
                        config.preSharedKey = stringBuilder.toString();
                    } else {
                        config.preSharedKey = args[2];
                    }
                    config.SSID = getAppendSsidWithRandomUuid(config, context);
                    s.setApConfiguration(config);
                    return true;
                } catch (Exception e) {
                    randomUUID = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error parse cust Ap configuration ");
                    stringBuilder.append(e);
                    Log.e(randomUUID, stringBuilder.toString());
                    return false;
                }
            }
        }
        return false;
    }

    public boolean autoConnectByMode(Message message) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("autoConnectByMode CONNECT_NETWORK arg1:");
        stringBuilder.append(message.arg1);
        Log.d(str, stringBuilder.toString());
        return message.arg1 != 100;
    }

    public WifiP2pServiceImpl createHwWifiP2pService(Context context) {
        this.mHwWifiP2PService = new HwWifiP2pService(context);
        return this.mHwWifiP2PService;
    }

    public WifiP2pServiceImpl getHwWifiP2pService() {
        return this.mHwWifiP2PService;
    }

    public void createHwArpVerifier(Context context) {
        HwArpVerifier.newInstance(context);
    }

    public WifiStateMachine createHwWifiStateMachine(Context context, FrameworkFacade facade, Looper looper, UserManager userManager, WifiInjector wifiInjector, BackupManagerProxy backupManagerProxy, WifiCountryCode countryCode, WifiNative wifiNative, WrongPasswordNotifier wrongPasswordNotifier, SarManager sarManager) {
        return new HwWifiStateMachine(context, facade, looper, userManager, wifiInjector, backupManagerProxy, countryCode, wifiNative, wrongPasswordNotifier, sarManager);
    }

    public String getAppendSsidWithRandomUuid(WifiConfiguration config, Context context) {
        if (1 == Systemex.getInt(context.getContentResolver(), "hw_softap_ssid_extend", 0)) {
            String randomUUID = UUID.randomUUID().toString();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(config.SSID);
            stringBuilder.append(randomUUID.substring(13, 18));
            config.SSID = stringBuilder.toString();
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("new SSID:");
            stringBuilder2.append(config.SSID);
            Log.d(str, stringBuilder2.toString());
        }
        return config.SSID;
    }

    private String getMD5str(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes("UTF-8"));
            return new BigInteger(1, md.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e1) {
            e1.printStackTrace();
            return null;
        }
    }

    public String getCustWifiApDefaultName(WifiConfiguration config) {
        String productNameShort;
        StringBuilder sb = new StringBuilder();
        String brandName = SystemProperties.get("ro.product.brand", "");
        String productName = SystemProperties.get("ro.product.name", "");
        String randomUUID = UUID.randomUUID().toString();
        String marketing_name = SystemProperties.get("ro.config.marketing_name");
        if (TextUtils.isEmpty(marketing_name)) {
            if (TextUtils.isEmpty(brandName)) {
                sb.append("HUAWEI");
            } else {
                sb.append(brandName);
            }
            if (!TextUtils.isEmpty(productName)) {
                productNameShort = productName.contains("-") ? productName.substring(0, productName.indexOf(45)) : productName;
                sb.append('_');
                sb.append(productNameShort);
            }
            if (!TextUtils.isEmpty(randomUUID)) {
                sb.append('_');
                sb.append(randomUUID.substring(24, 28));
            }
            config.SSID = sb.toString();
        } else {
            config.SSID = marketing_name;
        }
        productNameShort = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCustWifiApDefaultName returns:");
        stringBuilder.append(config.SSID);
        Log.d(productNameShort, stringBuilder.toString());
        return config.SSID;
    }

    public SavedNetworkEvaluator createHwSavedNetworkEvaluator(Context context, ScoringParams scoringParams, WifiConfigManager configManager, Clock clock, LocalLog localLog, WifiStateMachine wsm, WifiConnectivityHelper connectivityHelper) {
        return new HwSavedNetworkEvaluator(context, scoringParams, configManager, clock, localLog, wsm, connectivityHelper);
    }

    public WifiScanningServiceImpl createHwWifiScanningServiceImpl(Context context, Looper looper, WifiScannerImplFactory scannerImplFactory, IBatteryStats batteryStats, WifiInjector wifiInjector) {
        return new HwWifiScanningServiceImpl(context, looper, scannerImplFactory, batteryStats, wifiInjector);
    }

    public WifiConfigManager createHwWifiConfigManager(Context context, Clock clock, UserManager userManager, TelephonyManager telephonyManager, WifiKeyStore wifiKeyStore, WifiConfigStore wifiConfigStore, WifiConfigStoreLegacy wifiConfigStoreLegacy, WifiPermissionsUtil wifiPermissionsUtil, WifiPermissionsWrapper wifiPermissionsWrapper, NetworkListStoreData networkListStoreData, DeletedEphemeralSsidsStoreData deletedEphemeralSsidsStoreData) {
        return new HwWifiConfigManager(context, clock, userManager, telephonyManager, wifiKeyStore, wifiConfigStore, wifiConfigStoreLegacy, wifiPermissionsUtil, wifiPermissionsWrapper, networkListStoreData, deletedEphemeralSsidsStoreData);
    }

    public WifiCountryCode createHwWifiCountryCode(Context context, WifiNative wifiNative, String oemDefaultCountryCode, boolean revertCountryCodeOnCellularLoss) {
        return new HwWifiCountryCode(context, wifiNative, oemDefaultCountryCode, revertCountryCodeOnCellularLoss);
    }

    public WifiSupplicantControl createHwWifiSupplicantControl(TelephonyManager telephonyManager, WifiNative wifiNative, LocalLog localLog) {
        return null;
    }

    public SoftApManager createHwSoftApManager(Context context, Looper looper, FrameworkFacade frameworkFacade, WifiNative wifiNative, String countryCode, SoftApCallback callback, WifiApConfigStore wifiApConfigStore, SoftApModeConfiguration config, WifiMetrics wifiMetrics) {
        return new HwSoftApManager(context, looper, frameworkFacade, wifiNative, countryCode, callback, wifiApConfigStore, config, wifiMetrics);
    }

    public WifiConnectivityManager createHwWifiConnectivityManager(Context context, ScoringParams scoringParams, WifiStateMachine stateMachine, WifiScanner scanner, WifiConfigManager configManager, WifiInfo wifiInfo, WifiNetworkSelector networkSelector, WifiConnectivityHelper connectivityHelper, WifiLastResortWatchdog wifiLastResortWatchdog, OpenNetworkNotifier openNetworkNotifier, CarrierNetworkNotifier carrierNetworkNotifier, CarrierNetworkConfig carrierNetworkConfig, WifiMetrics wifiMetrics, Looper looper, Clock clock, LocalLog localLog, boolean enable, FrameworkFacade frameworkFacade, SavedNetworkEvaluator savedNetworkEvaluator, ScoredNetworkEvaluator scoredNetworkEvaluator, PasspointNetworkEvaluator passpointNetworkEvaluator) {
        return new HwWifiConnectivityManager(context, scoringParams, stateMachine, scanner, configManager, wifiInfo, networkSelector, connectivityHelper, wifiLastResortWatchdog, openNetworkNotifier, carrierNetworkNotifier, carrierNetworkConfig, wifiMetrics, looper, clock, localLog, enable, frameworkFacade, savedNetworkEvaluator, scoredNetworkEvaluator, passpointNetworkEvaluator);
    }

    public boolean custSttingsEnableSoftap(String packageName) {
        if (SystemProperties.getBoolean("ro.config.custapp.enableAp", false)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setWifiApEnabled packageName is: ");
            stringBuilder.append(packageName);
            Log.d(str, stringBuilder.toString());
            if (!packageName.equals("com.android.settings")) {
                return true;
            }
        }
        return false;
    }
}
