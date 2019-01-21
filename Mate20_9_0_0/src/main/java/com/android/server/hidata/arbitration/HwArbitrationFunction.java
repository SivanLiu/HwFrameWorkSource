package com.android.server.hidata.arbitration;

import android.content.ContentResolver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.provider.Settings.Global;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.telephony.HwTelephonyManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Xml;
import com.android.server.hidata.appqoe.HwAPPQoEUtils;
import com.android.server.hidata.appqoe.HwAPPStateInfo;
import huawei.android.net.hwmplink.MpLinkCommonUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.xmlpull.v1.XmlPullParser;

public class HwArbitrationFunction {
    private static final String COUNTRY_CODE_CHINA = "460";
    private static final String TAG;
    private static int mCurrentDataTech = 0;
    private static int mCurrentServiceState = 1;
    private static boolean mDataRoamingState = false;
    private static boolean mDataTechSuitable = false;
    private static boolean mIsPvpScene = false;
    private static boolean mIsScreenOn = true;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(HwArbitrationDEFS.BASE_TAG);
        stringBuilder.append(HwArbitrationFunction.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    public static boolean isAllowMpLink(Context context, int uid) {
        if (!HwArbitrationCommonUtils.MAINLAND_REGION) {
            Log.d(TAG, "region is not CN");
            return false;
        } else if (!HwArbitrationCommonUtils.hasSimCard(context)) {
            Log.d(TAG, "No Sim");
            return false;
        } else if (isInAirplaneMode(context)) {
            Log.d(TAG, "cell in AirplaneMode");
            return false;
        } else if (!isStateInService()) {
            Log.d(TAG, "SIM not in service");
            return false;
        } else if (!isChina()) {
            Log.d(TAG, "not China operator");
            return false;
        } else if (!HwArbitrationCommonUtils.isCellEnable(context)) {
            Log.d(TAG, "Cell is not enabled");
            return false;
        } else if (isInVPNMode(context)) {
            Log.d(TAG, "cell in VPNMode");
            return false;
        } else if (!MpLinkCommonUtils.isMpLinkEnabled(context)) {
            Log.d(TAG, "WLAN+ off");
            return false;
        } else if (!isCell4Gor3G(context)) {
            Log.d(TAG, "not 4G or 3G");
            return false;
        } else if (isDataRoaming()) {
            Log.d(TAG, "celluar data is roaming");
            return false;
        } else if (Integer.MIN_VALUE != uid && isUidPolicyNotAllowCell(uid)) {
            Log.d(TAG, "uid policy is not allow cellar");
            return false;
        } else if (HwArbitrationCommonUtils.isDefaultPhoneCSCalling(context)) {
            Log.d(TAG, "Default Sim is CS calling");
            return false;
        } else if (HwArbitrationCommonUtils.isVicePhoneCalling(context)) {
            Log.d(TAG, "Vice Sim is calling");
            return false;
        } else if (!isVSimEnabled()) {
            return true;
        } else {
            Log.d(TAG, "skyTone VSim is Enabled");
            return false;
        }
    }

    private static boolean isChina() {
        String operator = TelephonyManager.getDefault().getNetworkOperator();
        return (operator == null || operator.length() == 0 || !operator.startsWith("460")) ? false : true;
    }

    private static boolean isInAirplaneMode(Context context) {
        boolean z = false;
        try {
            if (Global.getInt(context.getContentResolver(), "airplane_mode_on") == 1) {
                z = true;
            }
            return z;
        } catch (SettingNotFoundException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AirplaneMode error is: ");
            stringBuilder.append(e.toString());
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    public static boolean isInVPNMode(Context context) {
        try {
            return getSettingsSystemBoolean(context.getContentResolver(), "wifipro_network_vpn_state", false);
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("VPN Mode error is: ");
            stringBuilder.append(e.toString());
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    private static boolean getSettingsSystemBoolean(ContentResolver cr, String name, boolean def) {
        return System.getInt(cr, name, def) == 1;
    }

    public static int getCurrentNetwork(Context context, int UID) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCurrentNetwork,UID =");
        stringBuilder.append(UID);
        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
        if (HwArbitrationStateMachine.getInstance(context) != null) {
            return HwArbitrationStateMachine.getInstance(context).getCurrentNetwork(context, UID);
        }
        return HwArbitrationCommonUtils.getActiveConnectType(context);
    }

    public static int getNetworkID(Context mContext, int network) {
        int networkType = -1;
        if (network == 800) {
            networkType = getWifiNetwork(mContext);
        }
        if (network == 801) {
            networkType = getCellNetwork(mContext);
        }
        ConnectivityManager mConnectivityManager = (ConnectivityManager) mContext.getSystemService("connectivity");
        if (!(mConnectivityManager == null || mConnectivityManager.getAllNetworks() == null)) {
            Network[] networks = mConnectivityManager.getAllNetworks();
            int length = networks.length;
            for (int i = 0; i < length; i++) {
                NetworkInfo netInfo = mConnectivityManager.getNetworkInfo(networks[i]);
                if (netInfo != null && netInfo.getType() == networkType) {
                    Network myNetwork = networks[i];
                    if (myNetwork != null) {
                        return myNetwork.netId;
                    }
                }
            }
        }
        return -1;
    }

    public static int getNetwork(Context mContext, int netId) {
        int result = -1;
        ConnectivityManager mConnectivityManager = (ConnectivityManager) mContext.getSystemService("connectivity");
        if (!(mConnectivityManager == null || mConnectivityManager.getAllNetworks() == null)) {
            Network[] networks = mConnectivityManager.getAllNetworks();
            int length = networks.length;
            for (int i = 0; i < length; i++) {
                NetworkInfo netInfo = mConnectivityManager.getNetworkInfo(networks[i]);
                Network myNetwork = networks[i];
                if (!(myNetwork == null || myNetwork.netId != netId || netInfo == null)) {
                    result = netInfo.getType();
                }
            }
        }
        if (getWifiNetwork(mContext) == result) {
            return 800;
        }
        if (getCellNetwork(mContext) == result) {
            return 801;
        }
        return 802;
    }

    public static int getWifiNetwork(Context mContext) {
        mContext.getSystemService("connectivity");
        return 1;
    }

    public static int getCellNetwork(Context mContext) {
        mContext.getSystemService("connectivity");
        return 0;
    }

    private static boolean isCell4Gor3G(Context mContext) {
        int networkClass = TelephonyManager.getNetworkClass(((TelephonyManager) mContext.getSystemService("phone")).getNetworkType());
        return networkClass == 2 || networkClass == 3;
    }

    public static boolean isInLTE(Context mContext) {
        int networkType = ((TelephonyManager) mContext.getSystemService("phone")).getNetworkType();
        if (networkType == 13 || networkType == 19) {
            return true;
        }
        return false;
    }

    private static boolean isUidPolicyNotAllowCell(int uid) {
        boolean out;
        IOException e3;
        String str;
        StringBuilder stringBuilder;
        XmlPullParser parser = Xml.newPullParser();
        if (parser == null) {
            Log.e(TAG, "parser is null!!!");
            return false;
        }
        out = false;
        InputStream inStream = null;
        try {
            inStream = new FileInputStream(new File(HwArbitrationDEFS.UID_POLICY_FILE_PATH));
            parser.setInput(inStream, "UTF-8");
            for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                if (eventType != 0) {
                    if (eventType == 2) {
                        String name = parser.getName();
                        if (name != null && name.equalsIgnoreCase("uid-policy")) {
                            int xmlUid = Integer.parseInt(parser.getAttributeValue(null, "uid"));
                            int xmlPolicy = Integer.parseInt(parser.getAttributeValue(null, "policy"));
                            if (xmlUid == uid && (xmlPolicy == 1 || xmlPolicy == 2 || xmlPolicy == 3)) {
                                String str2 = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("uid: ");
                                stringBuilder2.append(xmlUid);
                                stringBuilder2.append(", policy: ");
                                stringBuilder2.append(xmlPolicy);
                                HwArbitrationCommonUtils.logD(str2, stringBuilder2.toString());
                                out = true;
                            }
                        }
                    }
                }
            }
            try {
                inStream.close();
            } catch (IOException e) {
                e3 = e;
                str = TAG;
                stringBuilder = new StringBuilder();
            }
        } catch (RuntimeException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("RuntimeException: ");
            stringBuilder.append(e2.toString());
            Log.e(str, stringBuilder.toString());
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e4) {
                    e3 = e4;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (Exception e5) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception: ");
            stringBuilder.append(e5.toString());
            Log.e(str, stringBuilder.toString());
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e6) {
                    e3 = e6;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (Throwable th) {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e32) {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("IOException: ");
                    stringBuilder3.append(e32.toString());
                    Log.e(str3, stringBuilder3.toString());
                }
            }
        }
        return out;
        stringBuilder.append("IOException: ");
        stringBuilder.append(e3.toString());
        Log.e(str, stringBuilder.toString());
        return out;
    }

    public static boolean isStreamingScene(HwAPPStateInfo appInfo) {
        if (appInfo == null) {
            return false;
        }
        if (appInfo.mScenceId == HwAPPQoEUtils.SCENE_DOUYIN || appInfo.mScenceId == HwAPPQoEUtils.SCENE_VIDEO || appInfo.mScenceId == HwAPPQoEUtils.SCENE_AUDIO) {
            return true;
        }
        return false;
    }

    public static void setPvpScene(boolean isPvpScene) {
        mIsPvpScene = isPvpScene;
    }

    public static boolean isPvpScene() {
        return mIsPvpScene;
    }

    public static void setScreenState(boolean isScreenOn) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mIsScreenOn:");
        stringBuilder.append(isScreenOn);
        Log.d(str, stringBuilder.toString());
        mIsScreenOn = isScreenOn;
    }

    public static boolean isScreenOn() {
        return mIsScreenOn;
    }

    public static void setDataRoamingState(boolean isDataRoaming) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mDataRoamingState:");
        stringBuilder.append(isDataRoaming);
        Log.d(str, stringBuilder.toString());
        mDataRoamingState = isDataRoaming;
    }

    public static boolean isDataRoaming() {
        return mDataRoamingState;
    }

    public static void setDataTechSuitable(boolean dataTechSuitable) {
        mDataTechSuitable = dataTechSuitable;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mDataTechSuitable:");
        stringBuilder.append(mDataTechSuitable);
        Log.d(str, stringBuilder.toString());
    }

    public static boolean isDataTechSuitable() {
        return mDataTechSuitable;
    }

    public static void setServiceState(int currentServiceState) {
        mCurrentServiceState = currentServiceState;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mCurrentServiceState:");
        stringBuilder.append(mCurrentServiceState);
        Log.d(str, stringBuilder.toString());
    }

    public static boolean isStateInService() {
        return mCurrentServiceState == 0;
    }

    public static void setDataTech(int dataTech) {
        mCurrentDataTech = dataTech;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mCurrentDataTech:");
        stringBuilder.append(mCurrentDataTech);
        Log.d(str, stringBuilder.toString());
    }

    public static int getDataTech() {
        return mCurrentDataTech;
    }

    private static boolean isVSimEnabled() {
        return HwTelephonyManager.getDefault().isVSimEnabled();
    }

    public static boolean isInMPLink(Context context, int uid) {
        boolean result = false;
        if (HwArbitrationStateMachine.getInstance(context) != null) {
            result = HwArbitrationStateMachine.getInstance(context).isInMPLink(uid);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isInMPLink:");
        stringBuilder.append(result);
        Log.d(str, stringBuilder.toString());
        return result;
    }
}
