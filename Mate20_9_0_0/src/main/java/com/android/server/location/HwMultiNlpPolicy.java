package com.android.server.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.telephony.HwTelephonyManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Slog;
import com.android.server.wifipro.WifiProCommonUtils;

public class HwMultiNlpPolicy {
    public static final int NLP_AB = 2;
    public static final int NLP_GOOGLE = 1;
    public static final int NLP_NONE = 0;
    private static final String PROPERTY_GLOBAL_OPERATOR_NUMERIC = "ril.operator.numeric";
    private static final String TAG = "HwMultiNlpPolicy";
    private static volatile HwMultiNlpPolicy instance;
    private boolean isChineseSimCard = false;
    private boolean isRegistedInChineseNetwork = false;
    private boolean isSimCardReady = false;
    private boolean isVSimEnabled = false;
    private boolean isWifiNetworkConnected = false;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String str = HwMultiNlpPolicy.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("receive broadcast intent, action: ");
            stringBuilder.append(action);
            Slog.d(str, stringBuilder.toString());
            if ("android.net.conn.CONNECTIVITY_CHANGE".equals(action)) {
                NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                if (info != null && info.getType() == 1) {
                    info = ((ConnectivityManager) HwMultiNlpPolicy.this.mContext.getSystemService("connectivity")).getNetworkInfo(info.getType());
                    if (info != null) {
                        HwMultiNlpPolicy.this.isWifiNetworkConnected = info.isConnected();
                    }
                    String str2 = HwMultiNlpPolicy.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("isWifiNetworkConnected: ");
                    stringBuilder2.append(HwMultiNlpPolicy.this.isWifiNetworkConnected);
                    Slog.d(str2, stringBuilder2.toString());
                }
            } else if ("android.intent.action.SIM_STATE_CHANGED".equals(action)) {
                HwMultiNlpPolicy.this.setOrUpdateChineseSimCard(HwMultiNlpPolicy.this.mDataSub);
            } else if ("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED".equals(action)) {
                HwMultiNlpPolicy.this.mDataSub = intent.getIntExtra("subscription", -1);
                HwMultiNlpPolicy.this.setOrUpdateChineseSimCard(HwMultiNlpPolicy.this.mDataSub);
            }
        }
    };
    private Context mContext;
    private int mDataSub = -1;
    private int mLastChoiceNlp = 0;
    private PhoneStateListener[] mPhoneStateListener;
    private TelephonyManager mTelephonyManager;

    public static HwMultiNlpPolicy getDefault(Context context) {
        if (instance == null) {
            instance = new HwMultiNlpPolicy(context);
        }
        return instance;
    }

    public static HwMultiNlpPolicy getDefault() {
        return instance;
    }

    public HwMultiNlpPolicy(Context context) {
        this.mContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addCategory("android.net.conn.CONNECTIVITY_CHANGE@hwBrExpand@ConnectStatus=WIFIDATACON|ConnectStatus=WIFIDATADSCON");
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        filter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
        registerPhoneStateListener();
        getDataSubscription();
    }

    private void registerPhoneStateListener() {
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mPhoneStateListener = new PhoneStateListener[3];
        for (int i = 0; i < 3; i++) {
            this.mPhoneStateListener[i] = getPhoneStateListener(i);
            this.mTelephonyManager.listen(this.mPhoneStateListener[i], 1);
        }
    }

    private PhoneStateListener getPhoneStateListener(int subId) {
        return new PhoneStateListener(Integer.valueOf(subId)) {
            public void onServiceStateChanged(ServiceState state) {
                if (state != null) {
                    String str = HwMultiNlpPolicy.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("PhoneStateListener ");
                    stringBuilder.append(this.mSubId);
                    stringBuilder.append(", mDataSub ");
                    stringBuilder.append(HwMultiNlpPolicy.this.mDataSub);
                    Slog.d(str, stringBuilder.toString());
                    if (this.mSubId.intValue() == HwMultiNlpPolicy.this.mDataSub && HwMultiNlpPolicy.this.mDataSub != -1) {
                        HwMultiNlpPolicy.this.setSimCardState(state.getDataRegState());
                        HwMultiNlpPolicy.this.setRegistedNetwork(state.getOperatorNumeric());
                    } else if (this.mSubId.intValue() == 2) {
                        HwMultiNlpPolicy.this.isVSimEnabled = HwTelephonyManager.getDefault().isVSimEnabled();
                    }
                }
            }
        };
    }

    public boolean shouldUseGoogleNLP(boolean update) {
        return 1 == choiceNLP(update);
    }

    public boolean shouldBeRecheck() {
        return this.mLastChoiceNlp != choiceNLP(false);
    }

    public int shouldUseNLP() {
        return choiceNLP(false);
    }

    private int choiceNLP(boolean update) {
        int result;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("choiceNLP isSimCardReady: ");
        stringBuilder.append(this.isSimCardReady);
        stringBuilder.append(" isWifiNetworkConnected: ");
        stringBuilder.append(this.isWifiNetworkConnected);
        stringBuilder.append(" isRegistedInChineseNetwork: ");
        stringBuilder.append(this.isRegistedInChineseNetwork);
        stringBuilder.append(" isVSimEnabled: ");
        stringBuilder.append(this.isVSimEnabled);
        stringBuilder.append(" isChineseSimCard: ");
        stringBuilder.append(this.isChineseSimCard);
        Slog.d(str, stringBuilder.toString());
        if (this.isSimCardReady) {
            if (this.isWifiNetworkConnected) {
                if (this.isRegistedInChineseNetwork) {
                    result = 2;
                } else {
                    result = 1;
                }
            } else if (this.isVSimEnabled) {
                if (isVsimRegistedInChineseNetwork()) {
                    result = 2;
                } else {
                    result = 1;
                }
            } else if (this.isChineseSimCard) {
                result = 2;
            } else {
                result = 1;
            }
        } else if (this.isVSimEnabled) {
            if (isVsimRegistedInChineseNetwork()) {
                result = 2;
            } else {
                result = 1;
            }
        } else if (isRegistedInChineseNetworkWithoutSimCard()) {
            result = 2;
        } else if (isRegistedInForeginNetworkWithoutSimCard()) {
            result = 1;
        } else if (isChineseVersion()) {
            result = 2;
        } else {
            result = 1;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("choiceNLP ");
        stringBuilder.append(result);
        Slog.d(str, stringBuilder.toString());
        if (update) {
            this.mLastChoiceNlp = result;
        }
        return result;
    }

    public static boolean isChineseVersion() {
        return "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    }

    private void getDataSubscription() {
        this.mDataSub = Global.getInt(this.mContext.getContentResolver(), "multi_sim_data_call", 0);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getDataSubscription mDataSub: ");
        stringBuilder.append(this.mDataSub);
        Slog.d(str, stringBuilder.toString());
    }

    public int getPreferredDataSubscription() {
        return SubscriptionManager.getDefaultDataSubscriptionId();
    }

    private void setSimCardState(int simState) {
        this.isSimCardReady = simState == 0;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setSimCardReady ");
        stringBuilder.append(simState);
        Slog.d(str, stringBuilder.toString());
    }

    private void setOrUpdateChineseSimCard(int dataSub) {
        String simCountry;
        if (dataSub != -1) {
            simCountry = TelephonyManager.getDefault().getSimCountryIso(dataSub);
            boolean isCtSimCard = HwTelephonyManager.getDefault().isCTSimCard(dataSub);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isChineseSimCard datasub ");
            stringBuilder.append(dataSub);
            stringBuilder.append(", ");
            stringBuilder.append(simCountry);
            Slog.d(str, stringBuilder.toString());
            if ((simCountry == null || !simCountry.equalsIgnoreCase("CN")) && !isCtSimCard) {
                this.isChineseSimCard = false;
            } else {
                this.isChineseSimCard = true;
            }
        }
        simCountry = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("isChineseSimCard ");
        stringBuilder2.append(this.isChineseSimCard);
        stringBuilder2.append(" , dataSub ");
        stringBuilder2.append(dataSub);
        Slog.d(simCountry, stringBuilder2.toString());
    }

    private void setRegistedNetwork(String numeric) {
        if (numeric == null || numeric.length() < 5 || !numeric.substring(0, 5).equals("99999")) {
            if (numeric != null && numeric.length() >= 3 && numeric.substring(0, 3).equals(WifiProCommonUtils.COUNTRY_CODE_CN)) {
                this.isRegistedInChineseNetwork = true;
            } else if (!(numeric == null || numeric.equals(""))) {
                this.isRegistedInChineseNetwork = false;
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setRegistedNetwork ");
        stringBuilder.append(numeric);
        Slog.d(str, stringBuilder.toString());
    }

    private boolean isVsimRegistedInChineseNetwork() {
        boolean ret = false;
        String networkCountry = SystemProperties.get("gsm.operator.iso-country.vsim");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isVsimRegistedInChineseNetwork ");
        stringBuilder.append(networkCountry);
        Slog.d(str, stringBuilder.toString());
        if (networkCountry != null && networkCountry.equalsIgnoreCase("CN")) {
            ret = true;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("isVsimRegistedInChineseNetwork ");
        stringBuilder.append(ret);
        Slog.d(str, stringBuilder.toString());
        return ret;
    }

    private boolean isRegistedInChineseNetworkWithoutSimCard() {
        boolean ret = false;
        String rplmns = SystemProperties.get(PROPERTY_GLOBAL_OPERATOR_NUMERIC, "");
        if (rplmns != null && rplmns.length() > 3 && rplmns.substring(0, 3).equals(WifiProCommonUtils.COUNTRY_CODE_CN)) {
            ret = true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isRegistedInChineseNetworkWithoutSimCard rplmns ");
        stringBuilder.append(rplmns);
        stringBuilder.append(", ret ");
        stringBuilder.append(ret);
        Slog.d(str, stringBuilder.toString());
        return ret;
    }

    private boolean isRegistedInForeginNetworkWithoutSimCard() {
        boolean ret = false;
        String rplmns = SystemProperties.get(PROPERTY_GLOBAL_OPERATOR_NUMERIC, "");
        if (!(rplmns == null || rplmns.length() <= 3 || rplmns.substring(0, 3).equals(WifiProCommonUtils.COUNTRY_CODE_CN))) {
            ret = true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isRegistedInForeginNetworkWithoutSimCard rplmns ");
        stringBuilder.append(rplmns);
        stringBuilder.append(", ret ");
        stringBuilder.append(ret);
        Slog.d(str, stringBuilder.toString());
        return ret;
    }
}
