package com.android.internal.telephony;

import android.os.Build;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.text.TextUtils;
import android.util.Pair;
import com.android.internal.telephony.dataconnection.ApnSetting;
import huawei.cust.HwCfgFilePolicy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class RetryManager {
    public static final boolean DBG = true;
    private static final long DEFAULT_APN_RETRY_AFTER_DISCONNECT_DELAY = 10000;
    private static final String DEFAULT_DATA_RETRY_CONFIG = "max_retries=3, 5000, 5000, 5000";
    private static String DEFAULT_DATA_RETRY_CONFIG_CUST = SystemProperties.get("ro.gsm.data_retry_config");
    private static final long DEFAULT_INTER_APN_DELAY = 20000;
    private static final long DEFAULT_INTER_APN_DELAY_FOR_PROVISIONING = 3000;
    public static final String LOG_TAG = "RetryManager";
    private static final int MAX_SAME_APN_RETRY = 3;
    public static final long NO_RETRY = -1;
    public static final long NO_SUGGESTED_RETRY_DELAY = -2;
    private static final String OTHERS_APN_TYPE = "others";
    private static String OTHERS_DATA_RETRY_CONFIG_CUST = SystemProperties.get("ro.gsm.2nd_data_retry_config");
    public static final boolean VDBG = false;
    private boolean RETRYFOREVER = SystemProperties.getBoolean("ro.config.hw_pdp_retry_forever", false);
    private long mApnRetryAfterDisconnectDelay;
    private String mApnType;
    private String mConfig;
    private int mCurrentApnIndex = -1;
    private long mFailFastInterApnDelay;
    private long mInterApnDelay;
    private int mMaxRetryCount;
    private long mModemSuggestedDelay = -2;
    private Phone mPhone;
    private ArrayList<RetryRec> mRetryArray = new ArrayList();
    private int mRetryCount = 0;
    private boolean mRetryForever = false;
    private Random mRng = new Random();
    private int mSameApnRetryCount = 0;
    private ArrayList<ApnSetting> mWaitingApns = null;

    private static class RetryRec {
        int mDelayTime;
        int mRandomizationTime;

        RetryRec(int delayTime, int randomizationTime) {
            this.mDelayTime = delayTime;
            this.mRandomizationTime = randomizationTime;
        }
    }

    public RetryManager(Phone phone, String apnType) {
        this.mPhone = phone;
        this.mApnType = apnType;
    }

    private boolean configure(String configStr) {
        if (configStr.startsWith("\"") && configStr.endsWith("\"")) {
            configStr = configStr.substring(1, configStr.length() - 1);
        }
        reset();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("configure: '");
        stringBuilder.append(configStr);
        stringBuilder.append("'");
        log(stringBuilder.toString());
        this.mConfig = configStr;
        if (TextUtils.isEmpty(configStr)) {
            log("configure: cleared");
        } else {
            String[] strArray = configStr.split(",");
            int defaultRandomization = 0;
            for (int i = 0; i < strArray.length; i++) {
                String[] splitStr = strArray[i].split("=", 2);
                splitStr[0] = splitStr[0].trim();
                if (splitStr.length > 1) {
                    splitStr[1] = splitStr[1].trim();
                    Pair<Boolean, Integer> value;
                    if (TextUtils.equals(splitStr[0], "default_randomization")) {
                        value = parseNonNegativeInt(splitStr[0], splitStr[1]);
                        if (!((Boolean) value.first).booleanValue()) {
                            return false;
                        }
                        defaultRandomization = ((Integer) value.second).intValue();
                    } else if (!TextUtils.equals(splitStr[0], "max_retries")) {
                        String str = LOG_TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unrecognized configuration name value pair: ");
                        stringBuilder2.append(strArray[i]);
                        Rlog.e(str, stringBuilder2.toString());
                        return false;
                    } else if (TextUtils.equals("infinite", splitStr[1])) {
                        this.mRetryForever = true;
                    } else {
                        value = parseNonNegativeInt(splitStr[0], splitStr[1]);
                        if (!((Boolean) value.first).booleanValue()) {
                            return false;
                        }
                        this.mMaxRetryCount = ((Integer) value.second).intValue();
                    }
                } else {
                    splitStr = strArray[i].split(":", 2);
                    splitStr[0] = splitStr[0].trim();
                    RetryRec rr = new RetryRec(0, 0);
                    Pair<Boolean, Integer> value2 = parseNonNegativeInt("delayTime", splitStr[0]);
                    if (!((Boolean) value2.first).booleanValue()) {
                        return false;
                    }
                    rr.mDelayTime = ((Integer) value2.second).intValue();
                    if (splitStr.length > 1) {
                        splitStr[1] = splitStr[1].trim();
                        value2 = parseNonNegativeInt("randomizationTime", splitStr[1]);
                        if (!((Boolean) value2.first).booleanValue()) {
                            return false;
                        }
                        rr.mRandomizationTime = ((Integer) value2.second).intValue();
                    } else {
                        rr.mRandomizationTime = defaultRandomization;
                    }
                    this.mRetryArray.add(rr);
                }
            }
            if (this.mRetryArray.size() > this.mMaxRetryCount) {
                this.mMaxRetryCount = this.mRetryArray.size();
            }
        }
        return true;
    }

    private void configureRetry() {
        String configString = null;
        String otherConfigString = null;
        StringBuilder stringBuilder;
        try {
            if (Build.IS_DEBUGGABLE) {
                String config = SystemProperties.get("test.data_retry_config");
                if (!TextUtils.isEmpty(config)) {
                    configure(config);
                    return;
                }
            }
            PersistableBundle b = ((CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config")).getConfigForSubId(this.mPhone.getSubId());
            this.mInterApnDelay = b.getLong("carrier_data_call_apn_delay_default_long", DEFAULT_INTER_APN_DELAY);
            this.mFailFastInterApnDelay = b.getLong("carrier_data_call_apn_delay_faster_long", DEFAULT_INTER_APN_DELAY_FOR_PROVISIONING);
            this.mApnRetryAfterDisconnectDelay = b.getLong("carrier_data_call_apn_retry_after_disconnect_long", DEFAULT_APN_RETRY_AFTER_DISCONNECT_DELAY);
            if ("default".equals(this.mApnType) && !TextUtils.isEmpty(getGsmDefaultDataRetryConfig())) {
                configString = getGsmDefaultDataRetryConfig();
                stringBuilder = new StringBuilder();
                stringBuilder.append("configString = ");
                stringBuilder.append(configString);
                log(stringBuilder.toString());
                configure(configString);
            } else if ("default".equals(this.mApnType) || TextUtils.isEmpty(getGsm2ndDataRetryConfig())) {
                String[] allConfigStrings = b.getStringArray("carrier_data_call_retry_config_strings");
                if (allConfigStrings != null) {
                    int length = allConfigStrings.length;
                    String otherConfigString2 = otherConfigString;
                    int otherConfigString3 = 0;
                    while (otherConfigString3 < length) {
                        try {
                            String s = allConfigStrings[otherConfigString3];
                            if (!TextUtils.isEmpty(s)) {
                                String[] splitStr = s.split(":", 2);
                                if (splitStr.length == 2) {
                                    String apnType = splitStr[0].trim();
                                    if (apnType.equals(this.mApnType)) {
                                        configString = splitStr[1];
                                        break;
                                    } else if (apnType.equals(OTHERS_APN_TYPE)) {
                                        otherConfigString2 = splitStr[1];
                                    }
                                } else {
                                    continue;
                                }
                            }
                            otherConfigString3++;
                        } catch (NullPointerException e) {
                            otherConfigString = otherConfigString2;
                            log("Failed to read configuration! Use the hardcoded default value.");
                            this.mInterApnDelay = DEFAULT_INTER_APN_DELAY;
                            this.mFailFastInterApnDelay = DEFAULT_INTER_APN_DELAY_FOR_PROVISIONING;
                            configString = DEFAULT_DATA_RETRY_CONFIG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("configString = ");
                            stringBuilder.append(configString);
                            log(stringBuilder.toString());
                            configure(configString);
                        }
                    }
                    otherConfigString = otherConfigString2;
                }
                if (configString == null) {
                    if (otherConfigString != null) {
                        configString = otherConfigString;
                    } else {
                        log("Invalid APN retry configuration!. Use the default one now.");
                        configString = DEFAULT_DATA_RETRY_CONFIG;
                    }
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("configString = ");
                stringBuilder.append(configString);
                log(stringBuilder.toString());
                configure(configString);
            } else {
                configString = getGsm2ndDataRetryConfig();
                stringBuilder = new StringBuilder();
                stringBuilder.append("configString = ");
                stringBuilder.append(configString);
                log(stringBuilder.toString());
                configure(configString);
            }
        } catch (NullPointerException e2) {
            log("Failed to read configuration! Use the hardcoded default value.");
            this.mInterApnDelay = DEFAULT_INTER_APN_DELAY;
            this.mFailFastInterApnDelay = DEFAULT_INTER_APN_DELAY_FOR_PROVISIONING;
            configString = DEFAULT_DATA_RETRY_CONFIG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("configString = ");
            stringBuilder.append(configString);
            log(stringBuilder.toString());
            configure(configString);
        }
    }

    private int getRetryTimer() {
        int index;
        int retVal;
        if (this.mRetryCount < this.mRetryArray.size()) {
            index = this.mRetryCount;
        } else {
            index = this.mRetryArray.size() - 1;
        }
        if (index < 0 || index >= this.mRetryArray.size()) {
            retVal = 0;
        } else {
            retVal = ((RetryRec) this.mRetryArray.get(index)).mDelayTime + nextRandomizationTime(index);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getRetryTimer: ");
        stringBuilder.append(retVal);
        stringBuilder.append(" mRetryCount = ");
        stringBuilder.append(this.mRetryCount);
        log(stringBuilder.toString());
        return retVal;
    }

    private Pair<Boolean, Integer> parseNonNegativeInt(String name, String stringValue) {
        Pair<Boolean, Integer> retVal;
        try {
            int value = Integer.parseInt(stringValue);
            retVal = new Pair(Boolean.valueOf(validateNonNegativeInt(name, value)), Integer.valueOf(value));
        } catch (NumberFormatException e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(name);
            stringBuilder.append(" bad value: ");
            stringBuilder.append(stringValue);
            Rlog.e(str, stringBuilder.toString(), e);
            retVal = new Pair(Boolean.valueOf(false), Integer.valueOf(0));
        }
        return retVal;
    }

    private boolean validateNonNegativeInt(String name, int value) {
        if (value >= 0) {
            return true;
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(name);
        stringBuilder.append(" bad value: is < 0");
        Rlog.e(str, stringBuilder.toString());
        return false;
    }

    private int nextRandomizationTime(int index) {
        int randomTime = ((RetryRec) this.mRetryArray.get(index)).mRandomizationTime;
        if (randomTime == 0) {
            return 0;
        }
        return this.mRng.nextInt(randomTime);
    }

    public ApnSetting getNextApnSetting() {
        if (this.mWaitingApns == null || this.mWaitingApns.size() == 0) {
            log("Waiting APN list is null or empty.");
            return null;
        } else if (this.mModemSuggestedDelay == -2 || this.mSameApnRetryCount >= 3) {
            this.mSameApnRetryCount = 0;
            int index = this.mCurrentApnIndex;
            do {
                index++;
                if (index == this.mWaitingApns.size()) {
                    index = 0;
                }
                if (!((ApnSetting) this.mWaitingApns.get(index)).permanentFailed) {
                    this.mCurrentApnIndex = index;
                    return (ApnSetting) this.mWaitingApns.get(this.mCurrentApnIndex);
                }
            } while (index != this.mCurrentApnIndex);
            return null;
        } else {
            this.mSameApnRetryCount++;
            return (ApnSetting) this.mWaitingApns.get(this.mCurrentApnIndex);
        }
    }

    public long getDelayForNextApn(boolean failFastEnabled) {
        if (this.mWaitingApns == null || this.mWaitingApns.size() == 0) {
            log("Waiting APN list is null or empty.");
            return -1;
        } else if (this.mModemSuggestedDelay == -1) {
            log("Modem suggested not retrying.");
            return -1;
        } else {
            boolean isNeedModemSuggestedRetry = TextUtils.isEmpty(getGsmDefaultDataRetryConfig());
            if (this.mModemSuggestedDelay == -2 || this.mSameApnRetryCount >= 3 || !isNeedModemSuggestedRetry) {
                int index = this.mCurrentApnIndex;
                do {
                    index++;
                    if (index >= this.mWaitingApns.size()) {
                        index = 0;
                    }
                    if (!((ApnSetting) this.mWaitingApns.get(index)).permanentFailed) {
                        StringBuilder stringBuilder;
                        long delay;
                        if (index <= this.mCurrentApnIndex) {
                            if (!this.mRetryForever && this.mRetryCount + 1 > this.mMaxRetryCount) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Reached maximum retry count ");
                                stringBuilder.append(this.mMaxRetryCount);
                                stringBuilder.append(".");
                                log(stringBuilder.toString());
                                if (!this.RETRYFOREVER) {
                                    return -1;
                                }
                                log("reset mRetryCount");
                                this.mRetryCount = 0;
                            }
                            delay = (long) getRetryTimer();
                            this.mRetryCount++;
                        } else {
                            delay = this.mInterApnDelay;
                        }
                        if (failFastEnabled && delay > this.mFailFastInterApnDelay) {
                            delay = this.mFailFastInterApnDelay;
                        }
                        if (SystemProperties.getLong("persist.radio.telecom_apn_delay", 0) > delay) {
                            delay = SystemProperties.getLong("persist.radio.telecom_apn_delay", 0);
                        }
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("getDelayForNextApn delay = ");
                        stringBuilder.append(delay);
                        log(stringBuilder.toString());
                        return delay;
                    }
                } while (index != this.mCurrentApnIndex);
                log("All APNs have permanently failed.");
                return -1;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Modem suggested retry in ");
            stringBuilder2.append(this.mModemSuggestedDelay);
            stringBuilder2.append(" ms.");
            log(stringBuilder2.toString());
            return this.mModemSuggestedDelay;
        }
    }

    public void markApnPermanentFailed(ApnSetting apn) {
        if (apn != null) {
            apn.permanentFailed = true;
        }
    }

    private void reset() {
        this.mMaxRetryCount = 0;
        this.mRetryCount = 0;
        this.mCurrentApnIndex = -1;
        this.mSameApnRetryCount = 0;
        this.mModemSuggestedDelay = -2;
        this.mRetryArray.clear();
    }

    public void setWaitingApns(ArrayList<ApnSetting> waitingApns) {
        if (waitingApns == null) {
            log("No waiting APNs provided");
            return;
        }
        this.mWaitingApns = waitingApns;
        configureRetry();
        Iterator it = this.mWaitingApns.iterator();
        while (it.hasNext()) {
            ((ApnSetting) it.next()).permanentFailed = false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Setting ");
        stringBuilder.append(this.mWaitingApns.size());
        stringBuilder.append(" waiting APNs.");
        log(stringBuilder.toString());
    }

    public ArrayList<ApnSetting> getWaitingApns() {
        return this.mWaitingApns;
    }

    public void setModemSuggestedDelay(long delay) {
        this.mModemSuggestedDelay = delay;
    }

    public long getRetryAfterDisconnectDelay() {
        long ApnDelayProper = SystemProperties.getLong("persist.radio.telecom_apn_delay", 0);
        if (ApnDelayProper > this.mApnRetryAfterDisconnectDelay) {
            return ApnDelayProper;
        }
        return this.mApnRetryAfterDisconnectDelay;
    }

    public String toString() {
        if (this.mConfig == null) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("RetryManager: mApnType=");
        stringBuilder.append(this.mApnType);
        stringBuilder.append(" mRetryCount=");
        stringBuilder.append(this.mRetryCount);
        stringBuilder.append(" mMaxRetryCount=");
        stringBuilder.append(this.mMaxRetryCount);
        stringBuilder.append(" mCurrentApnIndex=");
        stringBuilder.append(this.mCurrentApnIndex);
        stringBuilder.append(" mSameApnRtryCount=");
        stringBuilder.append(this.mSameApnRetryCount);
        stringBuilder.append(" mModemSuggestedDelay=");
        stringBuilder.append(this.mModemSuggestedDelay);
        stringBuilder.append(" mRetryForever=");
        stringBuilder.append(this.mRetryForever);
        stringBuilder.append(" mInterApnDelay=");
        stringBuilder.append(this.mInterApnDelay);
        stringBuilder.append(" mApnRetryAfterDisconnectDelay=");
        stringBuilder.append(this.mApnRetryAfterDisconnectDelay);
        stringBuilder.append(" mConfig={");
        stringBuilder.append(this.mConfig);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    private void log(String s) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(this.mApnType);
        stringBuilder.append("] ");
        stringBuilder.append(s);
        Rlog.d(str, stringBuilder.toString());
    }

    public boolean isLastApnSetting() {
        if (this.mCurrentApnIndex <= 0 || this.mWaitingApns == null || this.mCurrentApnIndex != this.mWaitingApns.size() - 1) {
            return false;
        }
        return true;
    }

    public void resetRetryCount() {
        this.mRetryCount = 0;
    }

    private String getGsmDefaultDataRetryConfig() {
        int subId = this.mPhone.getSubId();
        String valueFromCard = (String) HwCfgFilePolicy.getValue("data_retry_config", subId, String.class);
        String valueFromProp = DEFAULT_DATA_RETRY_CONFIG_CUST;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getGsmDefaultDataRetryConfig, subId:");
        stringBuilder.append(subId);
        stringBuilder.append(", card:");
        stringBuilder.append(valueFromCard);
        stringBuilder.append(", prop:");
        stringBuilder.append(valueFromProp);
        log(stringBuilder.toString());
        return valueFromCard != null ? valueFromCard : valueFromProp;
    }

    private String getGsm2ndDataRetryConfig() {
        int subId = this.mPhone.getSubId();
        String valueFromCard = (String) HwCfgFilePolicy.getValue("2nd_data_retry_config", subId, String.class);
        String valueFromProp = OTHERS_DATA_RETRY_CONFIG_CUST;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getGsm2ndDataRetryConfig, subId:");
        stringBuilder.append(subId);
        stringBuilder.append(", card:");
        stringBuilder.append(valueFromCard);
        stringBuilder.append(", prop:");
        stringBuilder.append(valueFromProp);
        log(stringBuilder.toString());
        return valueFromCard != null ? valueFromCard : valueFromProp;
    }
}
