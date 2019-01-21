package com.android.internal.telephony.uicc;

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.HwTelephony.NumMatchs;
import android.provider.Settings.System;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.HwTelephonyFactory;
import com.android.internal.telephony.VirtualNet;
import com.android.internal.telephony.dataconnection.ApnReminder;
import huawei.cust.HwCfgFilePolicy;

public class VoiceMailConstantsEx extends VoiceMailConstants {
    private static final String LOG_TAG = "VoiceMailConstantsEx";
    private static final String MY_TAG = "VoiceMailConstantsEx";
    private static final Uri PREFERAPN_NO_UPDATE_URI = Uri.parse("content://telephony/carriers/preferapn_no_update");
    private static final int SUB1 = 0;
    private static final int SUB2 = 1;
    static final int VMMODE_CUST_SIM_SP = 3;
    private static final int VMMODE_INVALID = -1;
    static final int VMMODE_SIM_CUST_SP = 1;
    static final int VMMODE_SIM_SP_CUST = 2;
    private static final int VOICEMAIL_PRIORITY_MODE = SystemProperties.getInt("ro.config.vm_prioritymode", 3);
    private Context mContext;
    private String mCurrentCarrier = null;
    private boolean mIsVoiceMailFixed = false;
    private int mSlotId = 0;
    private String mVMCurrentMNumber = null;
    private String mVMCurrentTag = null;
    private boolean mVMLoaded = false;
    private String mVMNumberOnSim = null;
    private String mVMTagOnSim = null;
    private int mVoicemailPriorityModeWithCard = -1;
    private int voicemailPriorityMode;

    public VoiceMailConstantsEx(Context c, int slotId) {
        this.mContext = c;
        this.mSlotId = slotId;
        this.voicemailPriorityMode = getVmPriorityMode();
        log("load voicemail number Priority from systemproperty");
    }

    public int getVoicemailPriorityMode() {
        this.voicemailPriorityMode = getVmPriorityMode();
        return this.voicemailPriorityMode;
    }

    public void setVoicemailOnSIM(String voicemailNumber, String voicemailTag) {
        if (voicemailNumber != null) {
            this.mVMNumberOnSim = voicemailNumber.trim();
        } else {
            this.mVMNumberOnSim = null;
        }
        this.mVMTagOnSim = voicemailTag;
        this.mVMLoaded = false;
    }

    public void clearVoicemailLoadedFlag() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Before clearVoicemailLoadedFlag, mVMLoaded = ");
        stringBuilder.append(this.mVMLoaded);
        Log.d("VoiceMailConstantsEx", stringBuilder.toString());
        this.mVMLoaded = false;
    }

    public boolean containsCarrier(String carrier) {
        boolean vnContainsVM = false;
        if (HwTelephonyFactory.getHwPhoneManager().isVirtualNet() && carrier != null) {
            boolean z = carrier.equals(HwTelephonyFactory.getHwPhoneManager().getVirtualNetNumeric()) && HwTelephonyFactory.getHwPhoneManager().getVirtualNetVoiceMailNumber() != null && HwTelephonyFactory.getHwPhoneManager().getVirtualNetVoiceMailNumber().length() > 0;
            vnContainsVM = z;
            log("containsCarrier load vitualNet Config");
            if (!ApnReminder.getInstance(this.mContext).isPopupApnSettingsEmpty()) {
                vnContainsVM = carrier.equals(HwTelephonyFactory.getHwPhoneManager().getVirtualNetNumeric());
                log("containsCarrier load apnReminder Config");
            }
        } else if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated()) {
            String number = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerVoicemail();
            if (!(number == null || number.isEmpty()) || super.containsCarrier(carrier)) {
                log("containsCarrier load voicemail number from RoamingBroker config");
                vnContainsVM = true;
            }
        } else {
            vnContainsVM = super.containsCarrier(carrier);
        }
        if (!vnContainsVM) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("containsCarrier VoiceMailConfig doesn't contains the carrier");
            stringBuilder.append(carrier);
            log(stringBuilder.toString());
        }
        return vnContainsVM;
    }

    public boolean containsCarrier(String carrier, int slotId) {
        boolean vnContainsVM = false;
        int subId = slotId;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("containsCarrier slotId = ");
        stringBuilder.append(slotId);
        log(stringBuilder.toString());
        boolean z = true;
        if (!(slotId == 0 || 1 == slotId)) {
            subId = 0;
        }
        if (VirtualNet.isVirtualNet(subId) && carrier != null) {
            if (!carrier.equals(VirtualNet.getCurrentVirtualNet(subId).getNumeric()) || VirtualNet.getCurrentVirtualNet(subId).getVoiceMailNumber() == null || VirtualNet.getCurrentVirtualNet(subId).getVoiceMailNumber().length() <= 0) {
                z = false;
            }
            vnContainsVM = z;
            log("containsCarrier load vitualNet Config");
            if (!ApnReminder.getInstance(this.mContext, subId).isPopupApnSettingsEmpty()) {
                vnContainsVM = carrier.equals(VirtualNet.getCurrentVirtualNet(subId).getNumeric());
                log("containsCarrier load apnReminder Config");
            }
        } else if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated(Integer.valueOf(subId))) {
            String number = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerVoicemail();
            if (!(number == null || number.isEmpty()) || super.containsCarrier(carrier)) {
                log("containsCarrier load voicemail number from RoamingBroker config");
                vnContainsVM = true;
            }
        } else {
            vnContainsVM = super.containsCarrier(carrier);
        }
        if (!vnContainsVM) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("containsCarrier VoiceMailConfig doesn't contains the carrier");
            stringBuilder.append(carrier);
            stringBuilder.append(" for sub");
            stringBuilder.append(subId);
            log(stringBuilder.toString());
        }
        return vnContainsVM;
    }

    public void getVoiceMailConfig(String carrier) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("voicemail number Priority = ");
        stringBuilder.append(getVoicemailPriorityMode());
        Log.d("VoiceMailConstantsEx", stringBuilder.toString());
        this.mCurrentCarrier = carrier;
        if (!TextUtils.isEmpty(this.mVMNumberOnSim) && this.voicemailPriorityMode != 3) {
            Log.d("VoiceMailConstantsEx", "load voicemail number from sim");
            this.mIsVoiceMailFixed = false;
            this.mVMCurrentMNumber = this.mVMNumberOnSim;
            this.mVMCurrentTag = this.mVMTagOnSim;
            this.mVMLoaded = true;
        } else if (TextUtils.isEmpty(this.mVMNumberOnSim) && this.voicemailPriorityMode == 2) {
            StringBuilder stringBuilder2;
            this.mIsVoiceMailFixed = false;
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mContext);
            String mIccId = ((TelephonyManager) this.mContext.getSystemService("phone")) != null ? TelephonyManager.getDefault().getSimSerialNumber() : null;
            if (TextUtils.isEmpty(mIccId)) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("vm_number_key");
            } else {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(mIccId);
            }
            stringBuilder2.append(this.mSlotId);
            String number = sp.getString(stringBuilder2.toString(), null);
            if (number != null) {
                Log.d("VoiceMailConstantsEx", "load voicemail number from SP");
                this.mVMCurrentMNumber = number;
                this.mVMLoaded = true;
                return;
            }
        } else if (this.voicemailPriorityMode == 3 || this.voicemailPriorityMode == 1) {
            this.mIsVoiceMailFixed = true;
        }
        if (!this.mVMLoaded && HwTelephonyFactory.getHwPhoneManager().isVirtualNet()) {
            Log.d("VoiceMailConstantsEx", "try to load voicemail number from virtualNet");
            this.mVMCurrentMNumber = HwTelephonyFactory.getHwPhoneManager().getVirtualNetVoiceMailNumber();
            this.mVMCurrentTag = HwTelephonyFactory.getHwPhoneManager().getVirtualNetVoicemailTag();
            ApnReminder apnReminder = ApnReminder.getInstance(this.mContext);
            if (!apnReminder.isPopupApnSettingsEmpty()) {
                this.mVMCurrentMNumber = apnReminder.getVoiceMailNumberByPreferedApn(getPreferedApnId(), this.mVMCurrentMNumber);
                this.mVMCurrentTag = apnReminder.getVoiceMailTagByPreferedApn(getPreferedApnId(), this.mVMCurrentTag);
                log("load voicemail number and tag from apnReminder");
                this.mVMLoaded = true;
                return;
            } else if (!TextUtils.isEmpty(this.mVMCurrentMNumber)) {
                Log.d("VoiceMailConstantsEx", "loaded voicemail number from virtualNet");
                this.mVMLoaded = true;
                return;
            }
        }
        if (!this.mVMLoaded) {
            Log.d("VoiceMailConstantsEx", "load voicemail number from cust");
            this.mVMCurrentMNumber = super.getVoiceMailNumber(carrier);
            if (!isVMTagNotFromConf(carrier)) {
                this.mVMCurrentTag = super.getVoiceMailTag(carrier);
            }
        }
        this.mVMLoaded = true;
    }

    public void getVoiceMailConfig(String carrier, int slotId) {
        this.voicemailPriorityMode = getVoicemailPriorityMode();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("voicemail number Priority = ");
        stringBuilder.append(this.voicemailPriorityMode);
        Log.d("VoiceMailConstantsEx", stringBuilder.toString());
        this.mCurrentCarrier = carrier;
        int subId = slotId;
        stringBuilder = new StringBuilder();
        stringBuilder.append("getVoiceMailConfig slotId = ");
        stringBuilder.append(slotId);
        log(stringBuilder.toString());
        boolean isNotValidSlotId = (slotId == 0 || 1 == slotId) ? false : true;
        if (isNotValidSlotId) {
            subId = 0;
        }
        if (TextUtils.isEmpty(this.mVMNumberOnSim) || this.voicemailPriorityMode == 3) {
            if (TextUtils.isEmpty(this.mVMNumberOnSim) && this.voicemailPriorityMode == 2) {
                this.mIsVoiceMailFixed = false;
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mContext);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("vm_number_key");
                stringBuilder2.append(subId);
                String number = sp.getString(stringBuilder2.toString(), null);
                if (number != null) {
                    Log.d("VoiceMailConstantsEx", "load voicemail number from SP");
                    this.mVMCurrentMNumber = number;
                    this.mVMLoaded = true;
                    return;
                }
            } else if (this.voicemailPriorityMode == 3 || this.voicemailPriorityMode == 1) {
                this.mIsVoiceMailFixed = true;
            }
            ApnReminder apnReminder = ApnReminder.getInstance(this.mContext, subId);
            if (apnReminder.isPopupApnSettingsEmpty()) {
                if (VirtualNet.isVirtualNet(subId)) {
                    Log.d("VoiceMailConstantsEx", "try to load voicemail number from virtualNet");
                    this.mVMCurrentMNumber = VirtualNet.getCurrentVirtualNet(subId).getVoiceMailNumber();
                    this.mVMCurrentTag = VirtualNet.getCurrentVirtualNet(subId).getVoicemailTag();
                    if (!TextUtils.isEmpty(this.mVMCurrentMNumber)) {
                        Log.d("VoiceMailConstantsEx", "loaded voicemail number from virtualNet");
                        this.mVMLoaded = true;
                        return;
                    }
                }
                if (!this.mVMLoaded) {
                    Log.d("VoiceMailConstantsEx", "load voicemail number from cust");
                    this.mVMCurrentMNumber = super.getVoiceMailNumber(carrier);
                    if (!isVMTagNotFromConf(carrier)) {
                        this.mVMCurrentTag = super.getVoiceMailTag(carrier);
                    }
                }
                this.mVMLoaded = true;
                return;
            }
            this.mVMCurrentMNumber = apnReminder.getVoiceMailNumberByPreferedApn(getPreferedApnId(subId), this.mVMCurrentMNumber);
            this.mVMCurrentTag = apnReminder.getVoiceMailTagByPreferedApn(getPreferedApnId(subId), this.mVMCurrentTag);
            log("load voicemail number and tag from apnReminder");
            this.mVMLoaded = true;
            return;
        }
        Log.d("VoiceMailConstantsEx", "load voicemail number from sim");
        this.mIsVoiceMailFixed = false;
        this.mVMCurrentMNumber = this.mVMNumberOnSim;
        this.mVMCurrentTag = this.mVMTagOnSim;
        this.mVMLoaded = true;
    }

    private boolean isVMTagNotFromConf(String carrier) {
        String strVMTagNotFromConf = System.getString(this.mContext.getContentResolver(), "hw_vmtag_not_from_conf");
        if (!(TextUtils.isEmpty(strVMTagNotFromConf) || TextUtils.isEmpty(carrier))) {
            for (String area : strVMTagNotFromConf.split(",")) {
                if (area.equals(carrier)) {
                    Log.d("VoiceMailConstantsEx", "not load voicemail Tag from cust");
                    return true;
                }
            }
        }
        Log.d("VoiceMailConstantsEx", "load voicemail Tag from cust");
        return false;
    }

    public boolean getVoiceMailFixed(String carrier) {
        if (!(this.mVMLoaded && carrier.equals(this.mCurrentCarrier))) {
            getVoiceMailConfig(carrier);
        }
        if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated()) {
            String number = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerVoicemail();
            if (!(number == null || number.isEmpty())) {
                log("getVoiceMailFixed load voicemail number from RoamingBroker config");
                return true;
            }
        }
        return this.mIsVoiceMailFixed;
    }

    public boolean getVoiceMailFixed(String carrier, int slotId) {
        if (!(this.mVMLoaded && carrier.equals(this.mCurrentCarrier))) {
            getVoiceMailConfig(carrier, slotId);
        }
        if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated(Integer.valueOf(slotId))) {
            String number = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerVoicemail();
            if (!(number == null || number.isEmpty())) {
                log("getVoiceMailFixed load voicemail number from RoamingBroker config");
                return true;
            }
        }
        return this.mIsVoiceMailFixed;
    }

    public String getVoiceMailNumber(String carrier) {
        if (!(this.mVMLoaded && carrier.equals(this.mCurrentCarrier))) {
            getVoiceMailConfig(carrier);
        }
        if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated()) {
            String number = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerVoicemail();
            if (!(number == null || number.isEmpty())) {
                log("load voicemail number from RoamingBroker config");
                return HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerVoicemail();
            }
        }
        return this.mVMCurrentMNumber;
    }

    public String getVoiceMailNumber(String carrier, int slotId) {
        if (!(this.mVMLoaded && carrier.equals(this.mCurrentCarrier))) {
            getVoiceMailConfig(carrier, slotId);
        }
        if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated(Integer.valueOf(slotId))) {
            String number = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerVoicemail();
            if (!(number == null || number.isEmpty())) {
                log("load voicemail number from RoamingBroker config");
                return HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerVoicemail();
            }
        }
        return this.mVMCurrentMNumber;
    }

    public String getVoiceMailTag(String carrier) {
        if (!(this.mVMLoaded && carrier.equals(this.mCurrentCarrier))) {
            getVoiceMailConfig(carrier);
        }
        if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated()) {
            String number = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerVoicemail();
            if (!(number == null || number.isEmpty())) {
                log("load voicemail number from RoamingBroker config");
                return null;
            }
        }
        return this.mVMCurrentTag;
    }

    public String getVoiceMailTag(String carrier, int slotId) {
        if (!(this.mVMLoaded && carrier.equals(this.mCurrentCarrier))) {
            getVoiceMailConfig(carrier, slotId);
        }
        if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated(Integer.valueOf(slotId))) {
            String number = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerVoicemail();
            if (!(number == null || number.isEmpty())) {
                log("load voicemail number from RoamingBroker config");
                return null;
            }
        }
        return this.mVMCurrentTag;
    }

    public void resetVoiceMailLoadFlag() {
        this.mVMLoaded = false;
    }

    private int getPreferedApnId() {
        int apnId = -1;
        Cursor cursor = this.mContext.getContentResolver().query(PREFERAPN_NO_UPDATE_URI, new String[]{"_id", NumMatchs.NAME, "apn"}, null, null, NumMatchs.DEFAULT_SORT_ORDER);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            apnId = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            String apnName = cursor.getString(cursor.getColumnIndexOrThrow("apn"));
            String carrierName = cursor.getString(cursor.getColumnIndexOrThrow(NumMatchs.NAME));
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getPreferedApnId: ");
            stringBuilder.append(apnId);
            stringBuilder.append(", apn: ");
            stringBuilder.append(apnName);
            stringBuilder.append(", name: ");
            stringBuilder.append(carrierName);
            log(stringBuilder.toString());
        }
        if (cursor != null) {
            cursor.close();
        }
        return apnId;
    }

    private int getPreferedApnId(int slotId) {
        int apnId = -1;
        Cursor cursor = this.mContext.getContentResolver().query(ContentUris.withAppendedId(PREFERAPN_NO_UPDATE_URI, (long) slotId), new String[]{"_id", NumMatchs.NAME, "apn"}, null, null, NumMatchs.DEFAULT_SORT_ORDER);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            apnId = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            String apnName = cursor.getString(cursor.getColumnIndexOrThrow("apn"));
            String carrierName = cursor.getString(cursor.getColumnIndexOrThrow(NumMatchs.NAME));
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getPreferedApnId: ");
            stringBuilder.append(apnId);
            stringBuilder.append(", apn: ");
            stringBuilder.append(apnName);
            stringBuilder.append(", name: ");
            stringBuilder.append(carrierName);
            stringBuilder.append(" for slot");
            stringBuilder.append(slotId);
            log(stringBuilder.toString());
        }
        if (cursor != null) {
            cursor.close();
        }
        return apnId;
    }

    private void log(String string) {
        Rlog.d("VoiceMailConstantsEx", string);
    }

    public void setVoicemailInClaro(int voicemailPriorityMode) {
        Rlog.d("VoiceMailConstantsEx", "setVoicemailInClaro from hw_default.xml");
        this.voicemailPriorityMode = voicemailPriorityMode;
        this.mVoicemailPriorityModeWithCard = voicemailPriorityMode;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setVoicemailInClaro ");
        stringBuilder.append(this.voicemailPriorityMode);
        Rlog.d("VoiceMailConstantsEx", stringBuilder.toString());
    }

    private int getVmPriorityMode() {
        int valueFromProp = VOICEMAIL_PRIORITY_MODE;
        Integer valueFromCard = (Integer) HwCfgFilePolicy.getValue("vm_prioritymode", this.mSlotId, Integer.class);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getVmPriorityModeFromCard, slotId:");
        stringBuilder.append(this.mSlotId);
        stringBuilder.append(", card:");
        stringBuilder.append(valueFromCard);
        stringBuilder.append(", card(old):");
        stringBuilder.append(this.mVoicemailPriorityModeWithCard);
        stringBuilder.append(", prop: ");
        stringBuilder.append(valueFromProp);
        Rlog.d("VoiceMailConstantsEx", stringBuilder.toString());
        if (valueFromCard != null) {
            return valueFromCard.intValue();
        }
        if (this.mVoicemailPriorityModeWithCard != -1) {
            return this.mVoicemailPriorityModeWithCard;
        }
        return valueFromProp;
    }

    public String loadVoiceMailConfigFromCard(String configName, String carrier) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("loadVoiceMailConfigFromCard carrier:");
        stringBuilder.append(carrier);
        stringBuilder.append(", key:");
        stringBuilder.append(configName);
        log(stringBuilder.toString());
        if (TextUtils.isEmpty(carrier)) {
            return null;
        }
        String carrierFromCard = null;
        try {
            carrierFromCard = (String) HwCfgFilePolicy.getValue(configName, this.mSlotId, String.class);
        } catch (Exception e) {
            Rlog.d("VoiceMailConstantsEx", "read voicemail error : ", e);
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("loadVoiceMailConfigFromCard, mSlotId:");
        stringBuilder2.append(this.mSlotId);
        stringBuilder2.append(", carrierFromCard:");
        stringBuilder2.append(carrierFromCard);
        Rlog.d("VoiceMailConstantsEx", stringBuilder2.toString());
        return carrierFromCard;
    }
}
