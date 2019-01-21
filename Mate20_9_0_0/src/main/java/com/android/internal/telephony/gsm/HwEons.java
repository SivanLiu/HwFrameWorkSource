package com.android.internal.telephony.gsm;

import android.os.SystemProperties;
import android.telephony.Rlog;
import android.text.TextUtils;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.uicc.IccUtils;
import java.util.ArrayList;

public final class HwEons {
    private static final boolean DBG = true;
    static final String TAG = "HwEons";
    String mCphsOnsName;
    String mCphsOnsShortName;
    EonsControlState mOplDataState = EonsControlState.INITING;
    HwOplRecords mOplRecords;
    EonsControlState mPnnDataState = EonsControlState.INITING;
    HwPnnRecords mPnnRecords;

    public enum CphsType {
        LONG,
        SHORT;

        public boolean isLong() {
            return this == LONG;
        }

        public boolean isShort() {
            return this == SHORT;
        }
    }

    public enum EonsControlState {
        INITING,
        PRESENT,
        ABSENT;

        public boolean isIniting() {
            return this == INITING;
        }

        public boolean isPresent() {
            return this == PRESENT;
        }

        public boolean isAbsent() {
            return this == ABSENT;
        }
    }

    public enum EonsState {
        INITING,
        DISABLED,
        PNN_PRESENT,
        PNN_AND_OPL_PRESENT;

        public boolean isIniting() {
            return this == INITING;
        }

        public boolean isDisabled() {
            return this == DISABLED;
        }

        public boolean isPnnPresent() {
            return this == PNN_PRESENT;
        }

        public boolean isPnnAndOplPresent() {
            return this == PNN_AND_OPL_PRESENT;
        }
    }

    public HwEons() {
        Rlog.d(TAG, "Constructor init!");
        reset();
    }

    public void reset() {
        this.mPnnDataState = EonsControlState.INITING;
        this.mOplDataState = EonsControlState.INITING;
        this.mOplRecords = null;
        this.mPnnRecords = null;
        this.mCphsOnsName = null;
        this.mCphsOnsShortName = null;
    }

    public void setOplData(ArrayList<byte[]> records) {
        this.mOplDataState = EonsControlState.PRESENT;
        this.mOplRecords = new HwOplRecords(records);
    }

    public void resetOplData() {
        this.mOplDataState = EonsControlState.ABSENT;
        this.mOplRecords = null;
    }

    public void setPnnData(ArrayList<byte[]> records) {
        this.mPnnDataState = EonsControlState.PRESENT;
        this.mPnnRecords = new HwPnnRecords(records);
    }

    public void resetPnnData() {
        this.mPnnDataState = EonsControlState.ABSENT;
        this.mPnnRecords = null;
    }

    public void resetCphsData(CphsType type) {
        if (type.isLong()) {
            this.mCphsOnsName = null;
        } else if (type.isShort()) {
            this.mCphsOnsShortName = null;
        } else {
            this.mCphsOnsName = null;
            this.mCphsOnsShortName = null;
        }
    }

    public void setCphsData(CphsType type, byte[] data) {
        StringBuilder stringBuilder;
        if (type.isLong()) {
            this.mCphsOnsName = IccUtils.adnStringFieldToString(data, 0, data.length);
            stringBuilder = new StringBuilder();
            stringBuilder.append("setCphsData():mCphsOnsName is :");
            stringBuilder.append(this.mCphsOnsName);
            log(stringBuilder.toString());
        } else if (type.isShort()) {
            this.mCphsOnsShortName = IccUtils.adnStringFieldToString(data, 0, data.length);
            stringBuilder = new StringBuilder();
            stringBuilder.append("setCphsData():mCphsOnsShortName is :");
            stringBuilder.append(this.mCphsOnsShortName);
            log(stringBuilder.toString());
        }
    }

    public String getEons() {
        StringBuilder stringBuilder;
        String name = null;
        if (this.mPnnRecords != null) {
            log("getEons():mPnnRecords is not null!");
            name = this.mPnnRecords.getCurrentEons();
            stringBuilder = new StringBuilder();
            stringBuilder.append("getEons():name is :");
            stringBuilder.append(name);
            log(stringBuilder.toString());
        }
        if (!SystemProperties.get("ro.config.CphsOnsEnabled", "true").equals("true") || name != null) {
            return name;
        }
        if (TextUtils.isEmpty(this.mCphsOnsName)) {
            name = this.mCphsOnsShortName;
            stringBuilder = new StringBuilder();
            stringBuilder.append("get name from mCphsOnsShortName :");
            stringBuilder.append(name);
            log(stringBuilder.toString());
            return name;
        }
        name = this.mCphsOnsName;
        stringBuilder = new StringBuilder();
        stringBuilder.append("mCphsOnsName is not null!----get name is :");
        stringBuilder.append(name);
        log(stringBuilder.toString());
        return name;
    }

    public boolean updateEons(String regOperator, int lac, String hplmn) {
        if (getEonsState().isPnnAndOplPresent() && this.mOplRecords != null && this.mPnnRecords != null) {
            updateEonsFromOplAndPnn(regOperator, lac);
            return true;
        } else if (getEonsState().isPnnPresent() && this.mPnnRecords != null) {
            updateEonsIfHplmn(regOperator, hplmn);
            return true;
        } else if (!getEonsState().isIniting()) {
            return true;
        } else {
            log("[HwEons] Reading data from EF_OPL or EF_PNN is not complete. Suppress operator name display until all EF_OPL/EF_PNN data is read.");
            return false;
        }
    }

    public ArrayList<OperatorInfo> getEonsForAvailableNetworks(ArrayList<OperatorInfo> avlNetworks) {
        ArrayList<OperatorInfo> eonsNetworkNames = null;
        if (!getEonsState().isPnnAndOplPresent() || this.mPnnRecords == null || this.mOplRecords == null) {
            loge("[HwEons] OPL/PNN data is not available. Use the network names from Ril.");
            return null;
        }
        if (avlNetworks == null || avlNetworks.size() <= 0) {
            loge("[HwEons] Available Networks List is empty");
        } else {
            int size = avlNetworks.size();
            eonsNetworkNames = new ArrayList(size);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[HwEons] Available Networks List Size = ");
            stringBuilder.append(size);
            log(stringBuilder.toString());
            for (int i = 0; i < size; i++) {
                OperatorInfo oi = (OperatorInfo) avlNetworks.get(i);
                String pnnName = this.mPnnRecords.getNameFromPnnRecord(this.mOplRecords.getMatchingPnnRecord(oi.getOperatorNumericWithoutAct(), -1, false), false);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[HwEons] PLMN = ");
                stringBuilder2.append(oi.getOperatorNumeric());
                stringBuilder2.append(", ME Name = ");
                stringBuilder2.append(oi.getOperatorAlphaLong());
                stringBuilder2.append(", PNN Name = ");
                stringBuilder2.append(pnnName);
                log(stringBuilder2.toString());
                if (pnnName == null) {
                    pnnName = oi.getOperatorAlphaLong();
                } else {
                    pnnName = pnnName.concat(getRadioTechString(oi));
                }
                if (("334050".equals(oi.getOperatorNumericWithoutAct()) || "334090".equals(oi.getOperatorNumericWithoutAct())) && pnnName != null && pnnName.startsWith("AT&T")) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("AT&T");
                    stringBuilder2.append(getRadioTechString(oi));
                    pnnName = stringBuilder2.toString();
                }
                eonsNetworkNames.add(new OperatorInfo(pnnName, oi.getOperatorAlphaShort(), oi.getOperatorNumeric(), oi.getState()));
            }
        }
        return eonsNetworkNames;
    }

    private String getRadioTechString(OperatorInfo info) {
        String radioTechStr = "";
        String operatorName = info.getOperatorAlphaLong();
        if (operatorName == null) {
            operatorName = info.getOperatorAlphaShort();
        }
        if (operatorName == null) {
            return radioTechStr;
        }
        int longNameIndex = operatorName.lastIndexOf(32);
        if (-1 == longNameIndex) {
            return radioTechStr;
        }
        radioTechStr = operatorName.substring(longNameIndex);
        if (radioTechStr.equals(" 2G") || radioTechStr.equals(" 3G") || radioTechStr.equals(" 4G")) {
            return radioTechStr;
        }
        return "";
    }

    private void updateEonsFromOplAndPnn(String regOperator, int lac) {
        int pnnRecord = this.mOplRecords.getMatchingPnnRecord(regOperator, lac, true);
        String pnnName = this.mPnnRecords.getNameFromPnnRecord(pnnRecord, true);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[HwEons] Fetched HwEons name from EF_PNN record = ");
        stringBuilder.append(pnnRecord);
        stringBuilder.append(", name = ");
        stringBuilder.append(pnnName);
        log(stringBuilder.toString());
    }

    private void updateEonsIfHplmn(String regOperator, String hplmn) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[HwEons] Comparing hplmn, ");
        stringBuilder.append(hplmn);
        stringBuilder.append(" with registered plmn ");
        stringBuilder.append(regOperator);
        log(stringBuilder.toString());
        if (hplmn != null && hplmn.equals(regOperator)) {
            String pnnName = this.mPnnRecords.getNameFromPnnRecord(1, true);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[HwEons] Fetched HwEons name from EF_PNN's first record, name = ");
            stringBuilder2.append(pnnName);
            log(stringBuilder2.toString());
        }
    }

    private EonsState getEonsState() {
        if (this.mPnnDataState.isIniting() || this.mOplDataState.isIniting()) {
            return EonsState.INITING;
        }
        if (!this.mPnnDataState.isPresent()) {
            return EonsState.DISABLED;
        }
        if (this.mOplDataState.isPresent()) {
            return EonsState.PNN_AND_OPL_PRESENT;
        }
        return EonsState.PNN_PRESENT;
    }

    public boolean isEonsDisabled() {
        return getEonsState().isDisabled();
    }

    private void log(String s) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[HwEons] ");
        stringBuilder.append(s);
        Rlog.d(str, stringBuilder.toString());
    }

    private void loge(String s) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[HwEons] ");
        stringBuilder.append(s);
        Rlog.e(str, stringBuilder.toString());
    }
}
