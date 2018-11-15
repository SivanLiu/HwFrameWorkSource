package com.android.server;

import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.NetworkScorerAppData;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@VisibleForTesting
public class NetworkScorerAppManager {
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final String TAG = "NetworkScorerAppManager";
    private static final boolean VERBOSE = Log.isLoggable(TAG, 2);
    private final Context mContext;
    private final SettingsFacade mSettingsFacade;

    public static class SettingsFacade {
        public boolean putString(Context context, String name, String value) {
            return Global.putString(context.getContentResolver(), name, value);
        }

        public String getString(Context context, String name) {
            return Global.getString(context.getContentResolver(), name);
        }

        public boolean putInt(Context context, String name, int value) {
            return Global.putInt(context.getContentResolver(), name, value);
        }

        public int getInt(Context context, String name, int defaultValue) {
            return Global.getInt(context.getContentResolver(), name, defaultValue);
        }

        public int getSecureInt(Context context, String name, int defaultValue) {
            return Secure.getInt(context.getContentResolver(), name, defaultValue);
        }
    }

    public NetworkScorerAppManager(Context context) {
        this(context, new SettingsFacade());
    }

    @VisibleForTesting
    public NetworkScorerAppManager(Context context, SettingsFacade settingsFacade) {
        this.mContext = context;
        this.mSettingsFacade = settingsFacade;
    }

    @VisibleForTesting
    public List<NetworkScorerAppData> getAllValidScorers() {
        NetworkScorerAppManager networkScorerAppManager = this;
        if (VERBOSE) {
            Log.v(TAG, "getAllValidScorers()");
        }
        PackageManager pm = networkScorerAppManager.mContext.getPackageManager();
        Intent serviceIntent = new Intent("android.net.action.RECOMMEND_NETWORKS");
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(serviceIntent, 128);
        String str;
        if (resolveInfos == null || resolveInfos.isEmpty()) {
            if (DEBUG) {
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Found 0 Services able to handle ");
                stringBuilder.append(serviceIntent);
                Log.d(str, stringBuilder.toString());
            }
            return Collections.emptyList();
        }
        List<NetworkScorerAppData> appDataList = new ArrayList();
        int i = 0;
        while (i < resolveInfos.size()) {
            ServiceInfo serviceInfo = ((ResolveInfo) resolveInfos.get(i)).serviceInfo;
            if (networkScorerAppManager.hasPermissions(serviceInfo.applicationInfo.uid, serviceInfo.packageName)) {
                if (VERBOSE) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(serviceInfo.packageName);
                    stringBuilder2.append(" is a valid scorer/recommender.");
                    Log.v(str2, stringBuilder2.toString());
                }
                NetworkScorerAppData networkScorerAppData = r9;
                NetworkScorerAppData networkScorerAppData2 = new NetworkScorerAppData(serviceInfo.applicationInfo.uid, new ComponentName(serviceInfo.packageName, serviceInfo.name), networkScorerAppManager.getRecommendationServiceLabel(serviceInfo, pm), networkScorerAppManager.findUseOpenWifiNetworksActivity(serviceInfo), getNetworkAvailableNotificationChannelId(serviceInfo));
                appDataList.add(networkScorerAppData);
            } else if (VERBOSE) {
                str = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(serviceInfo.packageName);
                stringBuilder3.append(" is NOT a valid scorer/recommender.");
                Log.v(str, stringBuilder3.toString());
            }
            i++;
            networkScorerAppManager = this;
        }
        return appDataList;
    }

    private String getRecommendationServiceLabel(ServiceInfo serviceInfo, PackageManager pm) {
        if (serviceInfo.metaData != null) {
            String label = serviceInfo.metaData.getString("android.net.scoring.recommendation_service_label");
            if (!TextUtils.isEmpty(label)) {
                return label;
            }
        }
        CharSequence label2 = serviceInfo.loadLabel(pm);
        return label2 == null ? null : label2.toString();
    }

    private ComponentName findUseOpenWifiNetworksActivity(ServiceInfo serviceInfo) {
        String str;
        if (serviceInfo.metaData == null) {
            if (DEBUG) {
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("No metadata found on ");
                stringBuilder.append(serviceInfo.getComponentName());
                Log.d(str, stringBuilder.toString());
            }
            return null;
        }
        str = serviceInfo.metaData.getString("android.net.wifi.use_open_wifi_package");
        if (TextUtils.isEmpty(str)) {
            if (DEBUG) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("No use_open_wifi_package metadata found on ");
                stringBuilder2.append(serviceInfo.getComponentName());
                Log.d(str2, stringBuilder2.toString());
            }
            return null;
        }
        Intent enableUseOpenWifiIntent = new Intent("android.net.scoring.CUSTOM_ENABLE").setPackage(str);
        ResolveInfo resolveActivityInfo = this.mContext.getPackageManager().resolveActivity(enableUseOpenWifiIntent, 0);
        if (VERBOSE) {
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Resolved ");
            stringBuilder3.append(enableUseOpenWifiIntent);
            stringBuilder3.append(" to ");
            stringBuilder3.append(resolveActivityInfo);
            Log.d(str3, stringBuilder3.toString());
        }
        if (resolveActivityInfo == null || resolveActivityInfo.activityInfo == null) {
            return null;
        }
        return resolveActivityInfo.activityInfo.getComponentName();
    }

    private static String getNetworkAvailableNotificationChannelId(ServiceInfo serviceInfo) {
        if (serviceInfo.metaData != null) {
            return serviceInfo.metaData.getString("android.net.wifi.notification_channel_id_network_available");
        }
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("No metadata found on ");
            stringBuilder.append(serviceInfo.getComponentName());
            Log.d(str, stringBuilder.toString());
        }
        return null;
    }

    @VisibleForTesting
    public NetworkScorerAppData getActiveScorer() {
        if (getNetworkRecommendationsEnabledSetting() == -1) {
            return null;
        }
        return getScorer(getNetworkRecommendationsPackage());
    }

    private NetworkScorerAppData getScorer(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return null;
        }
        List<NetworkScorerAppData> apps = getAllValidScorers();
        for (int i = 0; i < apps.size(); i++) {
            NetworkScorerAppData app = (NetworkScorerAppData) apps.get(i);
            if (app.getRecommendationServicePackageName().equals(packageName)) {
                return app;
            }
        }
        return null;
    }

    private boolean hasPermissions(int uid, String packageName) {
        return hasScoreNetworksPermission(packageName) && canAccessLocation(uid, packageName);
    }

    private boolean hasScoreNetworksPermission(String packageName) {
        return this.mContext.getPackageManager().checkPermission("android.permission.SCORE_NETWORKS", packageName) == 0;
    }

    private boolean canAccessLocation(int uid, String packageName) {
        PackageManager pm = this.mContext.getPackageManager();
        AppOpsManager appOpsManager = (AppOpsManager) this.mContext.getSystemService("appops");
        if (isLocationModeEnabled() && pm.checkPermission("android.permission.ACCESS_COARSE_LOCATION", packageName) == 0 && appOpsManager.noteOp(0, uid, packageName) == 0) {
            return true;
        }
        return false;
    }

    private boolean isLocationModeEnabled() {
        return this.mSettingsFacade.getSecureInt(this.mContext, "location_mode", 0) != 0;
    }

    @VisibleForTesting
    public boolean setActiveScorer(String packageName) {
        String oldPackageName = getNetworkRecommendationsPackage();
        if (TextUtils.equals(oldPackageName, packageName)) {
            return true;
        }
        String str;
        StringBuilder stringBuilder;
        if (TextUtils.isEmpty(packageName)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Network scorer forced off, was: ");
            stringBuilder.append(oldPackageName);
            Log.i(str, stringBuilder.toString());
            setNetworkRecommendationsPackage(null);
            setNetworkRecommendationsEnabledSetting(-1);
            return true;
        } else if (getScorer(packageName) != null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Changing network scorer from ");
            stringBuilder.append(oldPackageName);
            stringBuilder.append(" to ");
            stringBuilder.append(packageName);
            Log.i(str, stringBuilder.toString());
            setNetworkRecommendationsPackage(packageName);
            setNetworkRecommendationsEnabledSetting(1);
            return true;
        } else {
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Requested network scorer is not valid: ");
            stringBuilder2.append(packageName);
            Log.w(str, stringBuilder2.toString());
            return false;
        }
    }

    @VisibleForTesting
    public void updateState() {
        if (getNetworkRecommendationsEnabledSetting() == -1) {
            if (DEBUG) {
                Log.d(TAG, "Recommendations forced off.");
            }
            return;
        }
        String currentPackageName = getNetworkRecommendationsPackage();
        if (getScorer(currentPackageName) != null) {
            if (VERBOSE) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(currentPackageName);
                stringBuilder.append(" is the active scorer.");
                Log.v(str, stringBuilder.toString());
            }
            setNetworkRecommendationsEnabledSetting(1);
            return;
        }
        int newEnabledSetting = 0;
        String defaultPackageName = getDefaultPackageSetting();
        if (!(TextUtils.equals(currentPackageName, defaultPackageName) || getScorer(defaultPackageName) == null)) {
            if (DEBUG) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Defaulting the network recommendations app to: ");
                stringBuilder2.append(defaultPackageName);
                Log.d(str2, stringBuilder2.toString());
            }
            setNetworkRecommendationsPackage(defaultPackageName);
            newEnabledSetting = 1;
        }
        setNetworkRecommendationsEnabledSetting(newEnabledSetting);
    }

    @VisibleForTesting
    public void migrateNetworkScorerAppSettingIfNeeded() {
        String scorerAppPkgNameSetting = this.mSettingsFacade.getString(this.mContext, "network_scorer_app");
        if (!TextUtils.isEmpty(scorerAppPkgNameSetting)) {
            NetworkScorerAppData currentAppData = getActiveScorer();
            if (currentAppData != null) {
                String str;
                if (DEBUG) {
                    String str2 = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Migrating Settings.Global.NETWORK_SCORER_APP (");
                    stringBuilder.append(scorerAppPkgNameSetting);
                    stringBuilder.append(")...");
                    Log.d(str2, stringBuilder.toString());
                }
                ComponentName enableUseOpenWifiActivity = currentAppData.getEnableUseOpenWifiActivity();
                if (TextUtils.isEmpty(this.mSettingsFacade.getString(this.mContext, "use_open_wifi_package")) && enableUseOpenWifiActivity != null && scorerAppPkgNameSetting.equals(enableUseOpenWifiActivity.getPackageName())) {
                    this.mSettingsFacade.putString(this.mContext, "use_open_wifi_package", scorerAppPkgNameSetting);
                    if (DEBUG) {
                        str = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Settings.Global.USE_OPEN_WIFI_PACKAGE set to '");
                        stringBuilder2.append(scorerAppPkgNameSetting);
                        stringBuilder2.append("'.");
                        Log.d(str, stringBuilder2.toString());
                    }
                }
                this.mSettingsFacade.putString(this.mContext, "network_scorer_app", null);
                if (DEBUG) {
                    Log.d(TAG, "Settings.Global.NETWORK_SCORER_APP migration complete.");
                    str = this.mSettingsFacade.getString(this.mContext, "use_open_wifi_package");
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Settings.Global.USE_OPEN_WIFI_PACKAGE is: '");
                    stringBuilder3.append(str);
                    stringBuilder3.append("'.");
                    Log.d(str3, stringBuilder3.toString());
                }
            }
        }
    }

    private String getDefaultPackageSetting() {
        return this.mContext.getResources().getString(17039787);
    }

    private String getNetworkRecommendationsPackage() {
        return this.mSettingsFacade.getString(this.mContext, "network_recommendations_package");
    }

    private void setNetworkRecommendationsPackage(String packageName) {
        this.mSettingsFacade.putString(this.mContext, "network_recommendations_package", packageName);
        if (VERBOSE) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("network_recommendations_package set to ");
            stringBuilder.append(packageName);
            Log.d(str, stringBuilder.toString());
        }
    }

    private int getNetworkRecommendationsEnabledSetting() {
        return this.mSettingsFacade.getInt(this.mContext, "network_recommendations_enabled", 0);
    }

    private void setNetworkRecommendationsEnabledSetting(int value) {
        this.mSettingsFacade.putInt(this.mContext, "network_recommendations_enabled", value);
        if (VERBOSE) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("network_recommendations_enabled set to ");
            stringBuilder.append(value);
            Log.d(str, stringBuilder.toString());
        }
    }
}
