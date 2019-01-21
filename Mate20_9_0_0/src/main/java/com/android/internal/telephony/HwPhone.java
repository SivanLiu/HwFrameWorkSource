package com.android.internal.telephony;

import android.os.Message;
import android.telephony.CellLocation;
import android.telephony.Rlog;
import com.android.internal.telephony.PhoneConstants.DataState;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;

public class HwPhone {
    private static final String LOG_TAG = "HwPhone";
    private Phone mPhone;

    /* renamed from: com.android.internal.telephony.HwPhone$1 */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppType = new int[AppType.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppType[AppType.APPTYPE_SIM.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppType[AppType.APPTYPE_USIM.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppType[AppType.APPTYPE_RUIM.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppType[AppType.APPTYPE_CSIM.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppType[AppType.APPTYPE_ISIM.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    public HwPhone(Phone phoneproxy) {
        this.mPhone = phoneproxy;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("init HwPhone mPhone = ");
        stringBuilder.append(this.mPhone);
        log(stringBuilder.toString());
    }

    private void log(String msg) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[PhoneIntfMgr] ");
        stringBuilder.append(msg);
        Rlog.d(str, stringBuilder.toString());
    }

    public String getMeid() {
        return this.mPhone.getMeid();
    }

    public String getPesn() {
        return this.mPhone.getPesn();
    }

    public String getNVESN() {
        return this.mPhone.getNVESN();
    }

    public void closeRrc() {
        this.mPhone.closeRrc();
    }

    public boolean isCDMAPhone() {
        if (this.mPhone == null || ((GsmCdmaPhone) this.mPhone).isPhoneTypeGsm()) {
            return false;
        }
        return true;
    }

    public void setDefaultMobileEnable(boolean enabled) {
        this.mPhone.mDcTracker.setEnabledPublic(0, enabled);
    }

    public void setUserDataEnabledWithoutPromp(boolean enabled) {
        this.mPhone.mDcTracker.setUserDataEnabled(enabled);
    }

    public void setDataEnabledWithoutPromp(boolean enabled) {
        this.mPhone.mDcTracker.setUserDataEnabled(enabled);
    }

    public void setDataRoamingEnabledWithoutPromp(boolean enabled) {
        this.mPhone.mDcTracker.setDataRoamingEnabledByUser(enabled);
    }

    public DataState getDataConnectionState() {
        return this.mPhone.getDataConnectionState();
    }

    public void setPreferredNetworkType(int networkType, Message response) {
        this.mPhone.setPreferredNetworkType(networkType, response);
    }

    public void getPreferredNetworkType(Message response) {
        this.mPhone.getPreferredNetworkType(response);
    }

    public String getImei() {
        return this.mPhone.getImei();
    }

    public Phone getPhone() {
        return this.mPhone;
    }

    public int getHwPhoneType() {
        Rlog.d(LOG_TAG, "[enter]getHwPhoneType");
        return this.mPhone.getPhoneType();
    }

    public String getCdmaGsmImsi() {
        Rlog.d(LOG_TAG, "getCdmaGsmImsi: in HuaweiPhoneService");
        return this.mPhone.getCdmaGsmImsi();
    }

    public int getUiccCardType() {
        Rlog.d(LOG_TAG, "[enter]getUiccCardType");
        return this.mPhone.getUiccCardType();
    }

    public CellLocation getCellLocation() {
        Rlog.d(LOG_TAG, "[enter]getUiccCardType");
        return this.mPhone.getCellLocation();
    }

    public String getCdmaMlplVersion() {
        Rlog.d(LOG_TAG, "getCdmaMlplVersion: in HuaweiPhoneService");
        return this.mPhone.getCdmaMlplVersion();
    }

    public String getCdmaMsplVersion() {
        Rlog.d(LOG_TAG, "getCdmaMsplVersion: in HuaweiPhoneService");
        return this.mPhone.getCdmaMsplVersion();
    }

    public boolean setISMCOEX(String setISMCoex) {
        return this.mPhone.setISMCOEX(setISMCoex);
    }

    public int getUiccAppType() {
        switch (AnonymousClass1.$SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppType[this.mPhone.getCurrentUiccAppType().ordinal()]) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            default:
                return 0;
        }
    }

    public boolean isRadioAvailable() {
        return this.mPhone.isRadioAvailable();
    }

    public void setImsSwitch(boolean value) {
        this.mPhone.setImsSwitch(value);
    }

    public boolean getImsSwitch() {
        return this.mPhone.getImsSwitch();
    }

    public void setImsDomainConfig(int domainType) {
        this.mPhone.setImsDomainConfig(domainType);
    }

    public void handleMapconImsaReq(byte[] Msg) {
        this.mPhone.handleMapconImsaReq(Msg);
    }

    public void getImsDomain(Message Msg) {
        this.mPhone.getImsDomain(Msg);
    }

    public void handleUiccAuth(int auth_type, byte[] rand, byte[] auth, Message Msg) {
        this.mPhone.handleUiccAuth(auth_type, rand, auth, Msg);
    }

    public boolean cmdForECInfo(int event, int action, byte[] buf) {
        try {
            return this.mPhone.cmdForECInfo(event, action, buf);
        } catch (Exception ex) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("cmdForECInfo fail:");
            stringBuilder.append(ex);
            Rlog.e(str, stringBuilder.toString());
            return false;
        }
    }

    public void requestForECInfo(Message msg, int event, byte[] buf) {
        this.mPhone.sendHWSolicited(msg, event, buf);
    }

    public boolean isCtSimCard() {
        String iccId = this.mPhone.getIccSerialNumber();
        if (iccId == null || iccId.length() < 7) {
            return false;
        }
        return HwIccIdUtil.isCT(iccId.substring(0, 7));
    }
}
