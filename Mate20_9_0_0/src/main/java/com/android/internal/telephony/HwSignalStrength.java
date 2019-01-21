package com.android.internal.telephony;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.rms.iaware.AppTypeInfo;
import android.telephony.Rlog;
import android.telephony.SignalStrength;
import android.text.TextUtils;
import com.huawei.connectivitylog.ConnectivityLogManager;

public class HwSignalStrength {
    private static final int CDMA_ECIO_QCOM_MULTIPLE = 10;
    public static final int DEFAULT_NUM_SIGNAL_STRENGTH_BINS = 4;
    protected static final String DEFAULT_SIGNAL_CUST_CDMA = "5,false,-112,-106,-99,-92,-85";
    protected static final String DEFAULT_SIGNAL_CUST_CDMALTE = "5,false,-120,-115,-110,-105,-97";
    protected static final String DEFAULT_SIGNAL_CUST_EVDO = "5,false,-112,-106,-99,-92,-85";
    protected static final String DEFAULT_SIGNAL_CUST_GSM = "5,false,-109,-103,-97,-91,-85";
    protected static final String DEFAULT_SIGNAL_CUST_LTE = "5,false,-120,-115,-110,-105,-97";
    protected static final String DEFAULT_SIGNAL_CUST_UMTS = "5,false,-112,-105,-99,-93,-87";
    private static final int ECIO_HISI_MULTIPLE = 1;
    public static final int GSM_STRENGTH_NONE = 0;
    public static final int GSM_STRENGTH_UNKOUWN = 99;
    public static final int INVALID_ECIO = 255;
    public static final int INVALID_RSSI = -1;
    private static final String LOG_TAG = "HwSignalStrength";
    private static final int LTE_RSRQ_QCOM_MULTIPLE = 10;
    public static final int LTE_RSSNR_UNKOUWN_STD = 99;
    public static final int LTE_STRENGTH_UNKOUWN_STD = -20;
    public static final int MAX_ASU = 31;
    public static final int NEW_NUM_SIGNAL_STRENGTH_BINS = 5;
    public static final int SIGNAL_STRENGTH_EXCELLENT = 5;
    private static final int UMTS_ECIO_QCOM_MULTIPLE = 2;
    public static final int WCDMA_ECIO_NONE = 255;
    public static final int WCDMA_STRENGTH_INVALID = Integer.MAX_VALUE;
    public static final int WCDMA_STRENGTH_NONE = 0;
    public static final int WCDMA_STRENGTH_UNKOUWN = 99;
    private static boolean isQualcom = HwModemCapability.isCapabilitySupport(9);
    private static HwSignalStrength mHwSignalStrength;
    private String PROPERTY_SIGNAL_CUST_CDMA = "gsm.sigcust.cdma";
    private String PROPERTY_SIGNAL_CUST_CDMALTE = "gsm.sigcust.cdmalte";
    private String PROPERTY_SIGNAL_CUST_CONFIGURED = "gsm.sigcust.configured";
    private String PROPERTY_SIGNAL_CUST_EVDO = "gsm.sigcust.evdo";
    private String PROPERTY_SIGNAL_CUST_GSM = "gsm.sigcust.gsm";
    private String PROPERTY_SIGNAL_CUST_LTE = "gsm.sigcust.lte";
    private String PROPERTY_SIGNAL_CUST_UMTS = "gsm.sigcust.umts";
    private SignalThreshold mCdmaLteSignalThreshold = new SignalThreshold(SignalType.CDMALTE);
    private SignalThreshold mCdmaSignalThreshold = new SignalThreshold(SignalType.CDMA);
    private SignalThreshold mEvdoSignalThreshold = new SignalThreshold(SignalType.EVDO);
    private SignalThreshold mGsmSignalThreshold = new SignalThreshold(SignalType.GSM);
    private SignalThreshold mLteSignalThreshold = new SignalThreshold(SignalType.LTE);
    private boolean mSigCustConfigured = false;
    private SignalThreshold mUmtsSignalThreshold = new SignalThreshold(SignalType.UMTS);

    public static class SignalThreshold {
        private boolean mConfigured;
        private int mNumOfBins;
        private int mThresholdEcioExcellent;
        private int mThresholdEcioGood;
        private int mThresholdEcioGreat;
        private int mThresholdEcioModerate;
        private int mThresholdEcioPoor;
        private int mThresholdRssiExcellent;
        private int mThresholdRssiGood;
        private int mThresholdRssiGreat;
        private int mThresholdRssiModerate;
        private int mThresholdRssiPoor;
        private SignalType mType;
        private boolean mUseEcio;

        private SignalThreshold(SignalType mSignalType) {
            this.mType = mSignalType;
        }

        public SignalType getSignalType() {
            return this.mType;
        }

        public int getNumofBins() {
            return this.mNumOfBins;
        }

        public boolean isConfigured() {
            return this.mConfigured;
        }

        public void loadConfigItem(String custInfo) {
            String[] config = custInfo.split(",");
            this.mNumOfBins = Integer.parseInt(config[0]);
            int mLeciomulti = 1;
            this.mUseEcio = "true".equals(config[1]);
            this.mThresholdRssiPoor = Integer.parseInt(config[2]);
            this.mThresholdRssiModerate = Integer.parseInt(config[3]);
            this.mThresholdRssiGood = Integer.parseInt(config[4]);
            this.mThresholdRssiGreat = Integer.parseInt(config[5]);
            if (5 == this.mNumOfBins) {
                this.mThresholdRssiExcellent = Integer.parseInt(config[6]);
            }
            if (this.mUseEcio) {
                int mUeciomulti;
                if (HwSignalStrength.isQualcom) {
                    int mUeciomulti2 = 10;
                    mLeciomulti = 10;
                    mUeciomulti = 2;
                } else {
                    mUeciomulti = 1;
                }
                switch (this.mType) {
                    case UMTS:
                        this.mThresholdEcioPoor = Integer.parseInt(config[(6 + this.mNumOfBins) - 4]) * mUeciomulti;
                        this.mThresholdEcioModerate = Integer.parseInt(config[(7 + this.mNumOfBins) - 4]) * mUeciomulti;
                        this.mThresholdEcioGood = Integer.parseInt(config[(8 + this.mNumOfBins) - 4]) * mUeciomulti;
                        this.mThresholdEcioGreat = Integer.parseInt(config[(9 + this.mNumOfBins) - 4]) * mUeciomulti;
                        if (5 == this.mNumOfBins) {
                            this.mThresholdEcioExcellent = Integer.parseInt(config[(10 + this.mNumOfBins) - 4]) * mUeciomulti;
                            return;
                        }
                        return;
                    case CDMA:
                        this.mThresholdEcioPoor = Integer.parseInt(config[(6 + this.mNumOfBins) - 4]) * mUeciomulti;
                        this.mThresholdEcioModerate = Integer.parseInt(config[(7 + this.mNumOfBins) - 4]) * mUeciomulti;
                        this.mThresholdEcioGood = Integer.parseInt(config[(8 + this.mNumOfBins) - 4]) * mUeciomulti;
                        this.mThresholdEcioGreat = Integer.parseInt(config[(9 + this.mNumOfBins) - 4]) * mUeciomulti;
                        if (5 == this.mNumOfBins) {
                            this.mThresholdEcioExcellent = Integer.parseInt(config[(10 + this.mNumOfBins) - 4]) * mUeciomulti;
                            return;
                        }
                        return;
                    case EVDO:
                        this.mThresholdEcioPoor = Integer.parseInt(config[(6 + this.mNumOfBins) - 4]);
                        this.mThresholdEcioModerate = Integer.parseInt(config[(7 + this.mNumOfBins) - 4]);
                        this.mThresholdEcioGood = Integer.parseInt(config[(8 + this.mNumOfBins) - 4]);
                        this.mThresholdEcioGreat = Integer.parseInt(config[(9 + this.mNumOfBins) - 4]);
                        if (5 == this.mNumOfBins) {
                            this.mThresholdEcioExcellent = Integer.parseInt(config[(10 + this.mNumOfBins) - 4]);
                            return;
                        }
                        return;
                    case LTE:
                    case CDMALTE:
                        this.mThresholdEcioPoor = (int) (((float) mLeciomulti) * Float.valueOf(config[(6 + this.mNumOfBins) - 4]).floatValue());
                        this.mThresholdEcioModerate = (int) (((float) mLeciomulti) * Float.valueOf(config[(7 + this.mNumOfBins) - 4]).floatValue());
                        this.mThresholdEcioGood = (int) (((float) mLeciomulti) * Float.valueOf(config[(8 + this.mNumOfBins) - 4]).floatValue());
                        this.mThresholdEcioGreat = (int) (((float) mLeciomulti) * Float.valueOf(config[(9 + this.mNumOfBins) - 4]).floatValue());
                        if (5 == this.mNumOfBins) {
                            this.mThresholdEcioExcellent = (int) (((float) mLeciomulti) * Float.valueOf(config[(10 + this.mNumOfBins) - 4]).floatValue());
                        }
                        String str = HwSignalStrength.LOG_TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("mThresholdEcioPoor");
                        stringBuilder.append(this.mThresholdEcioPoor);
                        stringBuilder.append("mThresholdEcioModerate");
                        stringBuilder.append(this.mThresholdEcioModerate);
                        Rlog.d(str, stringBuilder.toString());
                        return;
                    default:
                        this.mUseEcio = false;
                        return;
                }
            }
        }

        public int getSignalLevel(int rssi, int ecio) {
            int rssiLevel;
            int ecioLevel = 5;
            if (rssi < this.mThresholdRssiPoor || !HwSignalStrength.isRssiValid(this.mType, rssi)) {
                rssiLevel = 0;
            } else if (rssi < this.mThresholdRssiModerate) {
                rssiLevel = 1;
            } else if (rssi < this.mThresholdRssiGood) {
                rssiLevel = 2;
            } else if (rssi < this.mThresholdRssiGreat) {
                rssiLevel = 3;
            } else if (4 == this.mNumOfBins || (5 == this.mNumOfBins && rssi < this.mThresholdRssiExcellent)) {
                rssiLevel = 4;
            } else {
                rssiLevel = 5;
            }
            if (this.mUseEcio) {
                boolean ecioValid;
                if (HwSignalStrength.isQualcom) {
                    ecioValid = HwSignalStrength.isEcioValidForQCom(this.mType, ecio);
                } else {
                    ecioValid = HwSignalStrength.isEcioValid(this.mType, ecio);
                }
                if (!ecioValid) {
                    ecioLevel = 0;
                } else if (ecio < this.mThresholdEcioPoor) {
                    ecioLevel = 0;
                } else if (ecio < this.mThresholdEcioModerate) {
                    ecioLevel = 1;
                } else if (ecio < this.mThresholdEcioGood) {
                    ecioLevel = 2;
                } else if (ecio < this.mThresholdEcioGreat) {
                    ecioLevel = 3;
                } else if (4 == this.mNumOfBins || (5 == this.mNumOfBins && ecio < this.mThresholdEcioExcellent)) {
                    ecioLevel = 4;
                }
                int ecioLevel2 = ecioLevel;
                ecioLevel = rssiLevel >= ecioLevel2 ? ecioLevel2 : rssiLevel;
            } else {
                ecioLevel = rssiLevel;
            }
            return ecioLevel;
        }

        public int getHighThresholdBySignalLevel(int level, boolean isEcio) {
            if (!isEcio) {
                switch (level) {
                    case 0:
                        return this.mThresholdRssiPoor - 1;
                    case 1:
                        return this.mThresholdRssiModerate - 1;
                    case 2:
                        return this.mThresholdRssiGood - 1;
                    case 3:
                        return this.mThresholdRssiGreat - 1;
                    case 4:
                        if (5 == this.mNumOfBins) {
                            return this.mThresholdRssiExcellent - 1;
                        }
                        return -1;
                    default:
                        Rlog.d(HwSignalStrength.LOG_TAG, "use default rssi high threshold");
                        return -1;
                }
            } else if (this.mUseEcio) {
                switch (level) {
                    case 0:
                        return this.mThresholdEcioPoor - 1;
                    case 1:
                        return this.mThresholdEcioModerate - 1;
                    case 2:
                        return this.mThresholdEcioGood - 1;
                    case 3:
                        return this.mThresholdEcioGreat - 1;
                    case 4:
                        if (5 == this.mNumOfBins) {
                            return this.mThresholdEcioExcellent - 1;
                        }
                        return -1;
                    default:
                        Rlog.d(HwSignalStrength.LOG_TAG, "use default ecio high threshold");
                        return -1;
                }
            } else {
                Rlog.d(HwSignalStrength.LOG_TAG, "not use ecio");
                return -1;
            }
        }
    }

    public enum SignalType {
        UNKNOWN,
        GSM,
        UMTS,
        CDMA,
        EVDO,
        LTE,
        CDMALTE
    }

    private void logi(String string) {
        Rlog.i(LOG_TAG, string);
    }

    private void loge(String string) {
        Rlog.e(LOG_TAG, string);
    }

    public HwSignalStrength() {
        loadAllCustInfo();
    }

    private void loadAllCustInfo() {
        loadCustInfo(SignalType.GSM);
        loadCustInfo(SignalType.UMTS);
        loadCustInfo(SignalType.CDMA);
        loadCustInfo(SignalType.EVDO);
        loadCustInfo(SignalType.LTE);
        loadCustInfo(SignalType.CDMALTE);
    }

    public static HwSignalStrength getInstance() {
        if (mHwSignalStrength == null) {
            mHwSignalStrength = new HwSignalStrength();
        }
        return mHwSignalStrength;
    }

    private void loadCustInfo(SignalType signalType) {
        String configItem = "";
        String defaultValue = "";
        SignalThreshold signalThreshold = getSignalThreshold(signalType);
        if (signalThreshold == null) {
            signalThreshold = new SignalThreshold(signalType);
        }
        switch (signalType) {
            case GSM:
                configItem = SystemProperties.get(this.PROPERTY_SIGNAL_CUST_GSM, "");
                defaultValue = DEFAULT_SIGNAL_CUST_GSM;
                break;
            case UMTS:
                configItem = SystemProperties.get(this.PROPERTY_SIGNAL_CUST_UMTS, "");
                defaultValue = DEFAULT_SIGNAL_CUST_UMTS;
                break;
            case CDMA:
                configItem = SystemProperties.get(this.PROPERTY_SIGNAL_CUST_CDMA, "");
                defaultValue = "5,false,-112,-106,-99,-92,-85";
                break;
            case EVDO:
                configItem = SystemProperties.get(this.PROPERTY_SIGNAL_CUST_EVDO, "");
                defaultValue = "5,false,-112,-106,-99,-92,-85";
                break;
            case LTE:
                configItem = SystemProperties.get(this.PROPERTY_SIGNAL_CUST_LTE, "");
                defaultValue = "5,false,-120,-115,-110,-105,-97";
                break;
            case CDMALTE:
                configItem = SystemProperties.get(this.PROPERTY_SIGNAL_CUST_CDMALTE, "");
                defaultValue = "5,false,-120,-115,-110,-105,-97";
                break;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("invalid signalType :");
                stringBuilder.append(signalType);
                loge(stringBuilder.toString());
                break;
        }
        if (isCustConfigValid(configItem)) {
            signalThreshold.loadConfigItem(configItem);
            signalThreshold.mConfigured = true;
            logi("loadCustInfo from hw_defaults");
        } else if (defaultValue.isEmpty()) {
            signalThreshold.mConfigured = false;
            loge("Error! Didn't get any cust config!!");
        } else {
            signalThreshold.loadConfigItem(defaultValue);
            signalThreshold.mConfigured = true;
        }
    }

    public void updateSigCustInfoFromXML(Context context) {
        updateCustInfo(context);
        loadAllCustInfo();
        SystemProperties.set(this.PROPERTY_SIGNAL_CUST_CONFIGURED, "true");
        this.mSigCustConfigured = true;
        logi("updateSigCustInfoFromXML finish, set gsm.sigcust.configured true");
    }

    private void updateCustInfo(Context context) {
        logi("updateCustInfo start");
        String sig_cust_strings = getSigCustString(context);
        if (sig_cust_strings == null || "".equals(sig_cust_strings)) {
            logi("no cust_signal_thresholds found");
            return;
        }
        String[] network_type_sig_custs = sig_cust_strings.split(";");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("cust_signal_thresholds : ");
        stringBuilder.append(sig_cust_strings);
        logi(stringBuilder.toString());
        if (network_type_sig_custs != null) {
            for (String configstr : network_type_sig_custs) {
                setCustConfigFromString(configstr);
            }
        }
    }

    private static String getSigCustString(Context context) {
        try {
            return System.getString(context.getContentResolver(), "cust_signal_thresholds");
        } catch (Exception e) {
            return "";
        }
    }

    private void setCustConfigFromString(String configstr) {
        if (configstr != null) {
            if (configstr.contains(",")) {
                int pos = configstr.indexOf(",");
                String configType = configstr.substring(null, pos);
                String configInfo = configstr.substring(pos + 1, configstr.length());
                StringBuilder stringBuilder;
                switch (typeString2Enum(configType)) {
                    case GSM:
                        SystemProperties.set(this.PROPERTY_SIGNAL_CUST_GSM, configInfo);
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("set gsm sig cust to ");
                        stringBuilder.append(configInfo);
                        logi(stringBuilder.toString());
                        break;
                    case UMTS:
                        SystemProperties.set(this.PROPERTY_SIGNAL_CUST_UMTS, configInfo);
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("set umts sig cust to ");
                        stringBuilder.append(configInfo);
                        logi(stringBuilder.toString());
                        break;
                    case CDMA:
                        SystemProperties.set(this.PROPERTY_SIGNAL_CUST_CDMA, configInfo);
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("set cdma sig cust to ");
                        stringBuilder.append(configInfo);
                        logi(stringBuilder.toString());
                        break;
                    case EVDO:
                        SystemProperties.set(this.PROPERTY_SIGNAL_CUST_EVDO, configInfo);
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("set evdo sig cust to ");
                        stringBuilder.append(configInfo);
                        logi(stringBuilder.toString());
                        break;
                    case LTE:
                        SystemProperties.set(this.PROPERTY_SIGNAL_CUST_LTE, configInfo);
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("set lte sig cust to ");
                        stringBuilder.append(configInfo);
                        logi(stringBuilder.toString());
                        break;
                    default:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("invalid sig cust ");
                        stringBuilder.append(configInfo);
                        logi(stringBuilder.toString());
                        break;
                }
            }
            logi("configstr format is wrong,cannot analyze!");
        }
    }

    private SignalType typeString2Enum(String typeStr) {
        if (typeStr == null || "".equals(typeStr)) {
            return SignalType.UNKNOWN;
        }
        if (typeStr.equals("gsm")) {
            return SignalType.GSM;
        }
        if (typeStr.equals("umts")) {
            return SignalType.UMTS;
        }
        if (typeStr.equals("lte")) {
            return SignalType.LTE;
        }
        if (typeStr.equals("cdma")) {
            return SignalType.CDMA;
        }
        if (typeStr.equals("evdo")) {
            return SignalType.EVDO;
        }
        return SignalType.UNKNOWN;
    }

    private boolean isCustConfigValid(String custInfo) {
        if (TextUtils.isEmpty(custInfo)) {
            return false;
        }
        String[] config = custInfo.split(",");
        StringBuilder stringBuilder;
        if (config.length < 6) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Cust config length Error!!:");
            stringBuilder.append(config.length);
            loge(stringBuilder.toString());
            return false;
        } else if ("4".equals(config[0]) || "5".equals(config[0])) {
            if ("true".equals(config[1])) {
                if (("4".equals(config[0]) && config.length != 10) || ("5".equals(config[0]) && config.length != 12)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Length of config Error!!:");
                    stringBuilder.append(config.length);
                    loge(stringBuilder.toString());
                    return false;
                }
            } else if (("4".equals(config[0]) && config.length != 6) || ("5".equals(config[0]) && config.length != 7)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Length of config Error!!:");
                stringBuilder.append(config.length);
                loge(stringBuilder.toString());
                return false;
            }
            logi("Cust config is valid!");
            return true;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Num of bins Error!!:");
            stringBuilder.append(config[0]);
            loge(stringBuilder.toString());
            return false;
        }
    }

    public SignalThreshold getSignalThreshold(SignalType type) {
        switch (type) {
            case GSM:
                return this.mGsmSignalThreshold;
            case UMTS:
                return this.mUmtsSignalThreshold;
            case CDMA:
                return this.mCdmaSignalThreshold;
            case EVDO:
                return this.mEvdoSignalThreshold;
            case LTE:
                return this.mLteSignalThreshold;
            case CDMALTE:
                return this.mCdmaLteSignalThreshold;
            default:
                return null;
        }
    }

    public void validateInput(SignalStrength newSignalStrength) {
        int gsmSignalStrength;
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Signal before HW validate=");
        stringBuilder.append(newSignalStrength);
        Rlog.d(str, stringBuilder.toString());
        if (newSignalStrength.getGsmSignalStrength() > 0) {
            gsmSignalStrength = newSignalStrength.getGsmSignalStrength() * -1;
        } else {
            gsmSignalStrength = -1;
        }
        newSignalStrength.setGsmSignalStrength(gsmSignalStrength);
        if (newSignalStrength.getWcdmaRscp() > 0) {
            gsmSignalStrength = newSignalStrength.getWcdmaRscp() * -1;
        } else {
            gsmSignalStrength = -1;
        }
        newSignalStrength.setWcdmaRscp(gsmSignalStrength);
        int i = 255;
        if (newSignalStrength.getWcdmaEcio() >= 0) {
            gsmSignalStrength = newSignalStrength.getWcdmaEcio() * -1;
        } else {
            gsmSignalStrength = 255;
        }
        newSignalStrength.setWcdmaEcio(gsmSignalStrength);
        if (newSignalStrength.getCdmaDbm() > 0) {
            gsmSignalStrength = newSignalStrength.getCdmaDbm() * -1;
        } else {
            gsmSignalStrength = -1;
        }
        newSignalStrength.setCdmaDbm(gsmSignalStrength);
        if (newSignalStrength.getCdmaEcio() > 0) {
            gsmSignalStrength = newSignalStrength.getCdmaEcio() * -1;
        } else {
            gsmSignalStrength = 255;
        }
        newSignalStrength.setCdmaEcio(gsmSignalStrength);
        if (newSignalStrength.getEvdoDbm() <= 0 || newSignalStrength.getEvdoDbm() >= ConnectivityLogManager.WIFI_WIFIPRO_DUALBAND_EXCEPTION_EVENT) {
            gsmSignalStrength = -1;
        } else {
            gsmSignalStrength = newSignalStrength.getEvdoDbm() * -1;
        }
        newSignalStrength.setEvdoDbm(gsmSignalStrength);
        if (newSignalStrength.getEvdoEcio() >= 0) {
            gsmSignalStrength = newSignalStrength.getEvdoEcio() * -1;
        } else {
            gsmSignalStrength = 255;
        }
        newSignalStrength.setEvdoEcio(gsmSignalStrength);
        if (newSignalStrength.getEvdoSnr() > 0 && newSignalStrength.getEvdoSnr() <= 8) {
            i = newSignalStrength.getEvdoSnr();
        }
        newSignalStrength.setEvdoSnr(i);
        newSignalStrength.setLteSignalStrength(newSignalStrength.getLteSignalStrength() >= 0 ? newSignalStrength.getLteSignalStrength() : 99);
        gsmSignalStrength = newSignalStrength.getLteRsrp();
        int i2 = WCDMA_STRENGTH_INVALID;
        gsmSignalStrength = (gsmSignalStrength < 44 || newSignalStrength.getLteRsrp() > 140) ? WCDMA_STRENGTH_INVALID : newSignalStrength.getLteRsrp() * -1;
        newSignalStrength.setLteRsrp(gsmSignalStrength);
        gsmSignalStrength = (newSignalStrength.getLteRsrq() < 3 || newSignalStrength.getLteRsrq() > 20) ? WCDMA_STRENGTH_INVALID : newSignalStrength.getLteRsrq() * -1;
        newSignalStrength.setLteRsrq(gsmSignalStrength);
        if (newSignalStrength.getLteRssnr() >= -200 && newSignalStrength.getLteRssnr() <= AppTypeInfo.PG_TYPE_BASE) {
            i2 = newSignalStrength.getLteRssnr();
        }
        newSignalStrength.setLteRssnr(i2);
        str = LOG_TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Signal after HW validate=");
        stringBuilder.append(newSignalStrength);
        Rlog.d(str, stringBuilder.toString());
    }

    public int getLevel(SignalType type, int rssi, int ecio) {
        if (!this.mSigCustConfigured && SystemProperties.getBoolean(this.PROPERTY_SIGNAL_CUST_CONFIGURED, false)) {
            loadAllCustInfo();
            this.mSigCustConfigured = true;
        }
        SignalThreshold signalThreshold = getSignalThreshold(type);
        if (signalThreshold == null || !signalThreshold.isConfigured()) {
            return -1;
        }
        return signalThreshold.getSignalLevel(rssi, ecio);
    }

    public static boolean isRssiValid(SignalType type, int rssi) {
        switch (type) {
            case GSM:
                if (rssi == 0 || rssi == 99 || rssi > 0 || -1 == rssi) {
                    return false;
                }
            case UMTS:
                if (rssi == 0 || rssi == WCDMA_STRENGTH_INVALID || rssi == 99 || rssi > 0 || -1 == rssi) {
                    return false;
                }
            case CDMA:
            case EVDO:
                if (rssi > 0 || -1 == rssi) {
                    return false;
                }
            case LTE:
            case CDMALTE:
                if (WCDMA_STRENGTH_INVALID == rssi || rssi > -20) {
                    return false;
                }
            default:
                return false;
        }
        return true;
    }

    public static boolean isEcioValid(SignalType type, int ecio) {
        switch (type) {
            case GSM:
                if (255 == ecio) {
                    return false;
                }
                break;
            case UMTS:
                if (ecio == 255 || ecio == WCDMA_STRENGTH_INVALID || ecio == 255) {
                    return false;
                }
            case CDMA:
            case EVDO:
                if (255 == ecio) {
                    return false;
                }
                break;
            case LTE:
            case CDMALTE:
                if (WCDMA_STRENGTH_INVALID == ecio || ecio > 99) {
                    return false;
                }
            default:
                return false;
        }
        return true;
    }

    public static boolean isEcioValidForQCom(SignalType type, int ecio) {
        switch (type) {
            case GSM:
            case UMTS:
            case CDMA:
            case EVDO:
                if (255 == ecio) {
                    return false;
                }
                break;
            case LTE:
            case CDMALTE:
                if (WCDMA_STRENGTH_INVALID == ecio) {
                    return false;
                }
                break;
            default:
                return false;
        }
        return true;
    }
}
