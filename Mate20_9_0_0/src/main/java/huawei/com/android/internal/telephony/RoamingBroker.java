package huawei.com.android.internal.telephony;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.HwAESCryptoUtil;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConstants;
import java.util.ArrayList;

public class RoamingBroker {
    private static final boolean DBG = true;
    private static final String IMSI_SAVE_FILE_NAME = "imsi";
    private static final String LOG_TAG = "GSM";
    public static final String PreviousIccId = "persist.radio.previousiccid";
    public static final String PreviousImsi = "persist.radio.previousimsi";
    public static final String PreviousOperator = "persist.radio.previousopcode";
    public static final String RBActivated = "gsm.RBActivated";
    private static final String RBActivated_flag_on = "true";
    private static ArrayList<RoamingBrokerSequence> mRBSequenceList = new ArrayList();
    private boolean isHaveSetData;
    private boolean isIccidSet;
    private boolean isImsiSet;
    private String mCurrentIccid;
    private String mCurrentImsi;
    private String mCurrentOp;
    private String mPreviousIccid;
    private String mPreviousOp;
    private int mSlotId;
    private String mVoicemail;

    private static class HelperHolder {
        private static RoamingBroker mRoamingBroker0 = new RoamingBroker();
        private static RoamingBroker mRoamingBroker1 = new RoamingBroker(1);

        private HelperHolder() {
        }
    }

    private static class RoamingBrokerSequence {
        static final int RBSequenceLength = 3;
        String before_rb_mccmnc;
        String name;
        String rb_mccmnc;
        String rb_voicemail;

        private RoamingBrokerSequence() {
            this.name = "";
            this.before_rb_mccmnc = "";
            this.rb_mccmnc = "";
            this.rb_voicemail = "";
        }
    }

    private static void log(String text) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[RoamingBroker] ");
        stringBuilder.append(text);
        Log.d(str, stringBuilder.toString());
    }

    private static void loge(String text, Exception e) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[RoamingBroker] ");
        stringBuilder.append(text);
        stringBuilder.append(e);
        Log.e(str, stringBuilder.toString());
    }

    private String printIccid(String iccid) {
        if (this.mPreviousIccid == null) {
            return "null";
        }
        if (this.mPreviousIccid.length() <= 6) {
            return "less than 6 digits";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.mPreviousIccid.substring(0, 6));
        stringBuilder.append(new String(new char[(this.mPreviousIccid.length() - 6)]).replace(0, '*'));
        return stringBuilder.toString();
    }

    private RoamingBroker() {
        this.mCurrentImsi = null;
        this.mSlotId = 0;
        this.mPreviousIccid = null;
        this.mPreviousOp = null;
        this.mCurrentIccid = null;
        this.mCurrentOp = null;
        this.mVoicemail = null;
        this.isHaveSetData = false;
        this.isIccidSet = false;
        this.isImsiSet = false;
        this.mPreviousOp = SystemProperties.get(PreviousOperator, "");
        log(String.format("Previously saved operator code is %s", new Object[]{this.mPreviousOp}));
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("init state: ");
        stringBuilder.append(PhoneFactory.getInitState());
        log(stringBuilder.toString());
        if (PhoneFactory.getInitState()) {
            this.mPreviousIccid = decryptInfo(PhoneFactory.getDefaultPhone().getContext(), PreviousIccId);
        } else {
            this.mPreviousIccid = "";
        }
        log(String.format("Previously saved Iccid is %s", new Object[]{printIccid(this.mPreviousIccid)}));
    }

    private void loadRBSequenceMap() {
        mRBSequenceList.clear();
        try {
            String data = System.getString(PhoneFactory.getDefaultPhone().getContext().getContentResolver(), "roamingBrokerSequenceList");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("get raw RB list with voicemail");
            stringBuilder.append(data);
            log(stringBuilder.toString());
            if (data != null) {
                for (String s : data.split("\\|")) {
                    if (s != null) {
                        String[] tmp = s.split(",");
                        if (tmp.length >= 3) {
                            RoamingBrokerSequence rbs = new RoamingBrokerSequence();
                            rbs.name = tmp[0];
                            rbs.before_rb_mccmnc = tmp[1];
                            rbs.rb_mccmnc = tmp[2];
                            if (tmp.length > 3) {
                                rbs.rb_voicemail = tmp[3];
                            }
                            mRBSequenceList.add(rbs);
                        } else {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("RB list contains invalid config: ");
                            stringBuilder2.append(s);
                            log(stringBuilder2.toString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            loge("Failed to load RB Sequence list. ", e);
        }
    }

    private void unloadRBSequenceMap() {
        mRBSequenceList.clear();
    }

    public static RoamingBroker getDefault() {
        return HelperHolder.mRoamingBroker0;
    }

    public void setOperator(String operatorCode) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("received operatorCode of value: ");
        stringBuilder.append(operatorCode);
        log(stringBuilder.toString());
        if (operatorCode != null && !operatorCode.equals(this.mCurrentOp)) {
            this.mCurrentOp = operatorCode;
        }
    }

    public void setIccId(String IccId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Previous Iccid value: ");
        stringBuilder.append(printIccid(IccId));
        log(stringBuilder.toString());
        boolean bNeedClrIccid = false;
        this.isIccidSet = true;
        if (!(IccId == null || IccId.equals(this.mCurrentIccid))) {
            this.mCurrentIccid = IccId;
            if (this.isHaveSetData) {
                setData();
                this.isHaveSetData = false;
                bNeedClrIccid = true;
            }
        }
        if (this.isIccidSet && this.isImsiSet) {
            log("check specfic Iccid for romaing broker state");
            if (checkSpecRBState()) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("set roamingbroker state on and set PreviousOperator ");
                stringBuilder2.append(this.mPreviousOp);
                log(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(RBActivated);
                stringBuilder2.append(this.mSlotId);
                SystemProperties.set(stringBuilder2.toString(), RBActivated_flag_on);
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(PreviousOperator);
                stringBuilder2.append(this.mSlotId);
                SystemProperties.set(stringBuilder2.toString(), this.mPreviousOp);
            }
            this.isIccidSet = false;
            this.isImsiSet = false;
        }
        if (bNeedClrIccid) {
            this.mCurrentIccid = null;
        }
    }

    private void setData() {
        if (this.mCurrentIccid != null && this.mCurrentOp != null && this.mCurrentImsi != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(RBActivated);
            stringBuilder.append(this.mSlotId);
            SystemProperties.set(stringBuilder.toString(), "");
            Context context = PhoneFactory.getDefaultPhone().getContext();
            this.isHaveSetData = false;
            StringBuilder stringBuilder2;
            if (this.mCurrentOp.equals(this.mPreviousOp)) {
                if (!this.mCurrentIccid.equals(this.mPreviousIccid)) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(PreviousIccId);
                    stringBuilder2.append(this.mSlotId);
                    encryptInfo(context, stringBuilder2.toString(), this.mCurrentIccid);
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(PreviousImsi);
                    stringBuilder2.append(this.mSlotId);
                    encryptImsi(context, stringBuilder2.toString(), this.mCurrentImsi);
                    this.mPreviousIccid = this.mCurrentIccid;
                    log(String.format("different sim card with same operatorCode %s. Set iccId: %s for roaming broker", new Object[]{this.mPreviousOp, printIccid(this.mPreviousIccid)}));
                }
            } else if (!this.mCurrentIccid.equals(this.mPreviousIccid)) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(PreviousOperator);
                stringBuilder2.append(this.mSlotId);
                SystemProperties.set(stringBuilder2.toString(), this.mCurrentOp);
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(PreviousIccId);
                stringBuilder2.append(this.mSlotId);
                encryptInfo(context, stringBuilder2.toString(), this.mCurrentIccid);
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(PreviousImsi);
                stringBuilder2.append(this.mSlotId);
                encryptImsi(context, stringBuilder2.toString(), this.mCurrentImsi);
                this.mPreviousIccid = this.mCurrentIccid;
                this.mPreviousOp = this.mCurrentOp;
                log(String.format("different sim card. Set operatorCode: %s, iccId: %s for roaming broker", new Object[]{this.mPreviousOp, printIccid(this.mPreviousIccid)}));
            } else if (isValidRBSequence()) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(RBActivated);
                stringBuilder3.append(this.mSlotId);
                SystemProperties.set(stringBuilder3.toString(), RBActivated_flag_on);
            } else {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(PreviousOperator);
                stringBuilder2.append(this.mSlotId);
                SystemProperties.set(stringBuilder2.toString(), this.mCurrentOp);
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(PreviousImsi);
                stringBuilder2.append(this.mSlotId);
                encryptImsi(context, stringBuilder2.toString(), this.mCurrentImsi);
                this.mPreviousOp = this.mCurrentOp;
                log(String.format("same sim card. Set operatorCode: %s for roaming broker", new Object[]{this.mPreviousOp}));
            }
        }
    }

    private void encryptInfo(Context context, String encryptTag, String sensitiveInfo) {
        encryptInfo(PreferenceManager.getDefaultSharedPreferences(context), encryptTag, sensitiveInfo);
    }

    private void encryptImsi(Context context, String encryptTag, String sensitiveInfo) {
        encryptInfo(context.getSharedPreferences("imsi", 0), encryptTag, sensitiveInfo);
    }

    private void encryptInfo(SharedPreferences sp, String encryptTag, String sensitiveInfo) {
        Editor editor = sp.edit();
        try {
            sensitiveInfo = HwAESCryptoUtil.encrypt(HwFullNetworkConstants.MASTER_PASSWORD, sensitiveInfo);
        } catch (Exception ex) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HwAESCryptoUtil encryptInfo excepiton:");
            stringBuilder.append(ex.getMessage());
            log(stringBuilder.toString());
        }
        editor.putString(encryptTag, sensitiveInfo);
        editor.apply();
    }

    private String decryptInfo(Context context, String encryptTag) {
        return decryptInfo(PreferenceManager.getDefaultSharedPreferences(context), encryptTag);
    }

    private String decryptImsi(Context context, String encryptTag) {
        return decryptInfo(context.getSharedPreferences("imsi", 0), encryptTag);
    }

    private String decryptInfo(SharedPreferences sp, String encryptTag) {
        String sensitiveInfo = sp.getString(encryptTag, "");
        if ("".equals(sensitiveInfo)) {
            return sensitiveInfo;
        }
        try {
            return HwAESCryptoUtil.decrypt(HwFullNetworkConstants.MASTER_PASSWORD, sensitiveInfo);
        } catch (Exception ex) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HwAESCryptoUtil decryptInfo excepiton:");
            stringBuilder.append(ex.getMessage());
            log(stringBuilder.toString());
            return sensitiveInfo;
        }
    }

    private boolean checkSpecRBState() {
        Exception e;
        boolean bRbActived = false;
        if (isRoamingBrokerActivated(Integer.valueOf(this.mSlotId)) || this.mCurrentOp == null || this.mCurrentIccid == null) {
            return false;
        }
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkSpecRBState mCurrentOp ");
            stringBuilder.append(this.mCurrentOp);
            log(stringBuilder.toString());
            String specIccRBSeqList = System.getString(PhoneFactory.getDefaultPhone().getContext().getContentResolver(), "specIccidRBSeqList");
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("get raw RB list with specific Iccid ");
            stringBuilder2.append(specIccRBSeqList);
            log(stringBuilder2.toString());
            if (TextUtils.isEmpty(specIccRBSeqList)) {
                return false;
            }
            boolean z;
            String[] custArrays = specIccRBSeqList.trim().split(";");
            int length = custArrays.length;
            boolean bIccidMatch = false;
            int bIccidMatch2 = 0;
            while (bIccidMatch2 < length) {
                try {
                    String[] items = custArrays[bIccidMatch2].split(":");
                    if (items.length >= 2) {
                        if (this.mCurrentOp.equals(items[0])) {
                            for (String iccid : items[1].split(",")) {
                                if (this.mCurrentIccid.startsWith(iccid)) {
                                    bIccidMatch = true;
                                    break;
                                }
                            }
                            if (bIccidMatch) {
                                break;
                            }
                        }
                    }
                    bIccidMatch2++;
                } catch (Exception e2) {
                    e = e2;
                    z = bIccidMatch;
                    loge("Failed to load spefic iccid RB Sequence list.", e);
                    return bRbActived;
                }
            }
            z = bIccidMatch;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("checkSpecRBState return IccidMatch ");
            stringBuilder3.append(z);
            log(stringBuilder3.toString());
            if (!z) {
                return false;
            }
            loadRBSequenceMap();
            if (mRBSequenceList == null) {
                return false;
            }
            length = mRBSequenceList.size();
            for (int i = 0; i < length; i++) {
                RoamingBrokerSequence rbs = (RoamingBrokerSequence) mRBSequenceList.get(i);
                if (rbs != null) {
                    if (rbs.rb_mccmnc != null) {
                        if (this.mCurrentOp.equals(rbs.rb_mccmnc)) {
                            bRbActived = true;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("match spefic Iccid Rbcfg and rbSeqList together and set PreviousOp: ");
                            stringBuilder4.append(rbs.before_rb_mccmnc);
                            stringBuilder4.append(" and set PreviousIccid");
                            log(stringBuilder4.toString());
                            this.mVoicemail = rbs.rb_voicemail;
                            this.mPreviousOp = rbs.before_rb_mccmnc;
                            this.mPreviousIccid = this.mCurrentIccid;
                            break;
                        }
                    }
                }
            }
            unloadRBSequenceMap();
            return bRbActived;
        } catch (Exception e3) {
            e = e3;
            loge("Failed to load spefic iccid RB Sequence list.", e);
            return bRbActived;
        }
    }

    private boolean isValidRBSequence() {
        boolean result = false;
        if (!(this.mPreviousOp == null || this.mCurrentOp == null)) {
            loadRBSequenceMap();
            if (mRBSequenceList != null) {
                int list_size = mRBSequenceList.size();
                for (int i = 0; i < list_size; i++) {
                    RoamingBrokerSequence rbs = (RoamingBrokerSequence) mRBSequenceList.get(i);
                    if (this.mCurrentOp.equals(rbs.rb_mccmnc) && this.mPreviousOp.equals(rbs.before_rb_mccmnc)) {
                        result = true;
                        this.mVoicemail = rbs.rb_voicemail;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(rbs.name);
                        stringBuilder.append(" Roaming broker is activated");
                        log(stringBuilder.toString());
                        break;
                    }
                }
                unloadRBSequenceMap();
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("isValidRBSequence: ");
        stringBuilder2.append(result);
        log(stringBuilder2.toString());
        return result;
    }

    public static boolean isRoamingBrokerActivated() {
        boolean result = RBActivated_flag_on.equals(SystemProperties.get("gsm.RBActivated0"));
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isRoamingBrokerActivated returns ");
        stringBuilder.append(result);
        log(stringBuilder.toString());
        return result;
    }

    public static String updateSelectionForRoamingBroker(String selection) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateSelection for: ");
        stringBuilder.append(selection);
        log(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("numeric[ ]*=[ ]*[\"|']");
        stringBuilder.append(getDefault().mCurrentOp);
        stringBuilder.append("[\"|']");
        String result = stringBuilder.toString();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("numeric=\"");
        stringBuilder2.append(getDefault().mPreviousOp);
        stringBuilder2.append("\"");
        result = selection.replaceAll(result, stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("updated Selection: ");
        stringBuilder2.append(result);
        log(stringBuilder2.toString());
        return result;
    }

    public static String getRBOperatorNumeric() {
        return getDefault().mPreviousOp;
    }

    public static String getRBVoicemail() {
        return getDefault().mVoicemail;
    }

    private RoamingBroker(int slotId) {
        this.mCurrentImsi = null;
        this.mSlotId = 0;
        this.mPreviousIccid = null;
        this.mPreviousOp = null;
        this.mCurrentIccid = null;
        this.mCurrentOp = null;
        this.mVoicemail = null;
        this.isHaveSetData = false;
        this.isIccidSet = false;
        this.isImsiSet = false;
        this.mSlotId = slotId;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("###RoamingBroker init,mSlotId = ");
        stringBuilder.append(slotId);
        log(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(PreviousOperator);
        stringBuilder.append(slotId);
        this.mPreviousOp = SystemProperties.get(stringBuilder.toString(), "");
        log(String.format("Previously saved operator code is %s", new Object[]{this.mPreviousOp}));
        stringBuilder = new StringBuilder();
        stringBuilder.append("init state: ");
        stringBuilder.append(PhoneFactory.getInitState());
        log(stringBuilder.toString());
        if (PhoneFactory.getInitState()) {
            Context context = PhoneFactory.getDefaultPhone().getContext();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PreviousIccId);
            stringBuilder2.append(slotId);
            this.mPreviousIccid = decryptInfo(context, stringBuilder2.toString());
        } else {
            this.mPreviousIccid = "";
        }
        log(String.format("Previously saved Iccid is %s", new Object[]{printIccid(this.mPreviousIccid)}));
    }

    public static RoamingBroker getDefault(Integer slotId) {
        if (slotId.intValue() == 0) {
            return HelperHolder.mRoamingBroker0;
        }
        return HelperHolder.mRoamingBroker1;
    }

    public void setImsi(String Imsi) {
        this.isImsiSet = true;
        if (!(Imsi == null || Imsi.equals(this.mCurrentImsi))) {
            this.mCurrentImsi = Imsi;
            this.isHaveSetData = true;
            setData();
        }
        if (this.isIccidSet && this.isImsiSet) {
            log("check specfic Iccid for romaing broker state");
            if (checkSpecRBState()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("set roamingbroker state on and set PreviousOperator ");
                stringBuilder.append(this.mPreviousOp);
                log(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(RBActivated);
                stringBuilder.append(this.mSlotId);
                SystemProperties.set(stringBuilder.toString(), RBActivated_flag_on);
                stringBuilder = new StringBuilder();
                stringBuilder.append(PreviousOperator);
                stringBuilder.append(this.mSlotId);
                SystemProperties.set(stringBuilder.toString(), this.mPreviousOp);
            }
            this.isIccidSet = false;
            this.isImsiSet = false;
        }
    }

    public boolean isRoamingBrokerActivated(Integer slotId) {
        boolean result = RBActivated_flag_on;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(RBActivated);
        stringBuilder.append(slotId);
        result = result.equals(SystemProperties.get(stringBuilder.toString()));
        stringBuilder = new StringBuilder();
        stringBuilder.append("###isRoamingBrokerActivated returns ");
        stringBuilder.append(result);
        log(stringBuilder.toString());
        return result;
    }

    public String updateSelectionForRoamingBroker(String selection, int slotId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("###updateSelection for: ");
        stringBuilder.append(selection);
        log(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("numeric[ ]*=[ ]*[\"|']");
        stringBuilder.append(getDefault(Integer.valueOf(slotId)).mCurrentOp);
        stringBuilder.append("[\"|']");
        String result = stringBuilder.toString();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("numeric=\"");
        stringBuilder2.append(getDefault(Integer.valueOf(slotId)).mPreviousOp);
        stringBuilder2.append("\"");
        result = selection.replaceAll(result, stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("###updated Selection: ");
        stringBuilder2.append(result);
        log(stringBuilder2.toString());
        return result;
    }

    public String getRBImsi() {
        Context context = PhoneFactory.getDefaultPhone().getContext();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(PreviousImsi);
        stringBuilder.append(this.mSlotId);
        return decryptImsi(context, stringBuilder.toString());
    }

    public String getRBOperatorNumeric(Integer slotId) {
        return getDefault(slotId).mPreviousOp;
    }

    public String getRBVoicemail(Integer slotId) {
        return getDefault(slotId).mVoicemail;
    }
}
