package com.android.internal.telephony;

import android.app.ActivityManagerNative;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.SystemProperties;
import android.provider.HwTelephony.NumMatchs;
import android.provider.Settings.System;
import android.provider.Telephony.GlobalMatchs;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import org.xmlpull.v1.XmlPullParser;

public class GlobalParamsAdaptor {
    public static final String ACTION_SET_GLOBAL_AUTO_PARAM_DONE = "android.intent.action.ACTION_SET_GLOBAL_AUTO_PARAM_DONE";
    private static final String APGS_SERVERS_DOCUMENT = "agpsServers";
    private static final String CUSTXMLPATH = "/data/cust/xml/";
    private static final int ECC_FAKE_INDEX = 3;
    private static final String HWCFGPOLICYPATH = "hwCfgPolicyPath";
    private static final boolean IS_PRE_POST_PAY = SystemProperties.getBoolean("ro.config.hw_is_pre_post_pay", false);
    private static final boolean IS_SUPPORT_LONG_VMNUM = SystemProperties.getBoolean("ro.config.hw_support_long_vmNum", false);
    private static final int IS_VMN_SHORT_CODE_INDEX = 1;
    static final String LOG_TAG = "GlobalParamsAdaptor";
    private static final String MCCMNC = "mccmnc";
    private static final int MIN_MATCH = 7;
    private static final int NUM_MATCH_INDEX = 3;
    private static final int NUM_MATCH_SHORT_INDEX = 4;
    private static final String SERVER_NAME = "name";
    private static final String SUPL_PORT = "supl_port";
    private static final String SUPL_URL = "supl_host";
    private static final String SYSTEMXMLPATH = "/system/etc/";
    private static final String globalAgpsServersFileName = "globalAgpsServers-conf.xml";
    private static boolean waitingForSetupData = false;
    private int mPhoneId;

    public GlobalParamsAdaptor(int phoneId) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("contructor phoneId = ");
        stringBuilder.append(phoneId);
        Rlog.d(str, stringBuilder.toString());
        this.mPhoneId = phoneId;
    }

    private boolean arrayContains(String[] array, String value) {
        for (String equalsIgnoreCase : array) {
            if (equalsIgnoreCase.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    public void checkPrePostPay(String currentMccmnc, String currentImsi, Context context) {
        if (currentMccmnc != null && currentImsi != null) {
            String old_imsi_string = null;
            try {
                String prepay_postpay_mccmncs_strings = System.getString(context.getContentResolver(), "prepay_postpay_mccmncs");
                try {
                    old_imsi_string = System.getString(context.getContentResolver(), "old_imsi");
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Could not load default locales", e);
                }
                if (prepay_postpay_mccmncs_strings != null) {
                    boolean isContainer = arrayContains(prepay_postpay_mccmncs_strings.split(","), currentMccmnc);
                    boolean isEqual;
                    if (old_imsi_string != null) {
                        isEqual = currentImsi.equals(old_imsi_string.trim());
                    } else {
                        isEqual = false;
                    }
                    System.putString(context.getContentResolver(), "old_imsi", currentImsi);
                    if (isContainer && !isEqual) {
                        if (true == IS_PRE_POST_PAY) {
                            setWaitingForSetupData(true);
                        } else {
                            tryToActionPrePostPay();
                        }
                    }
                }
            } catch (Exception e2) {
                Log.e(LOG_TAG, "Could not load default locales", e2);
            }
        }
    }

    private boolean isVirtualNet() {
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            return VirtualNet.isVirtualNet(this.mPhoneId);
        }
        return VirtualNet.isVirtualNet();
    }

    private VirtualNet getCurrentVirtualNet() {
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            return VirtualNet.getCurrentVirtualNet(this.mPhoneId);
        }
        return VirtualNet.getCurrentVirtualNet();
    }

    private void checkMultiSimMaxMessageSize() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("gsm.sms.max.message.size");
        int i = 1;
        stringBuilder.append(this.mPhoneId == 0 ? 1 : 0);
        StringBuilder stringBuilder2;
        if (SystemProperties.getInt(stringBuilder.toString(), 0) == 0) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("gsm.sms.max.message.size");
            stringBuilder2.append(this.mPhoneId);
            SystemProperties.set("gsm.sms.max.message.size", Integer.toString(SystemProperties.getInt(stringBuilder2.toString(), 0)));
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("gsm.sms.max.message.size");
        stringBuilder.append(this.mPhoneId);
        int i2 = SystemProperties.getInt(stringBuilder.toString(), 0);
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("gsm.sms.max.message.size");
        stringBuilder2.append(this.mPhoneId == 0 ? 1 : 0);
        if (i2 < SystemProperties.getInt(stringBuilder2.toString(), 0)) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("gsm.sms.max.message.size");
            stringBuilder2.append(this.mPhoneId);
            SystemProperties.set("gsm.sms.max.message.size", Integer.toString(SystemProperties.getInt(stringBuilder2.toString(), 0)));
            return;
        }
        String str = "gsm.sms.max.message.size";
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("gsm.sms.max.message.size");
        if (this.mPhoneId != 0) {
            i = 0;
        }
        stringBuilder2.append(i);
        SystemProperties.set(str, Integer.toString(SystemProperties.getInt(stringBuilder2.toString(), 0)));
    }

    private void checkMultiSmsToMmsTextThreshold() {
        String str;
        StringBuilder stringBuilder;
        int set_threshold;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("gsm.sms.to.mms.textthreshold");
        stringBuilder2.append(this.mPhoneId);
        int cur = SystemProperties.getInt(stringBuilder2.toString(), 0);
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("gsm.sms.to.mms.textthreshold");
        stringBuilder3.append(this.mPhoneId == 0 ? 1 : 0);
        int ant = SystemProperties.getInt(stringBuilder3.toString(), 0);
        String str2 = LOG_TAG;
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("checkMultiSmsToMmsTextThreshold>>mPhoneId=");
        stringBuilder4.append(this.mPhoneId);
        stringBuilder4.append(", cur= ");
        stringBuilder4.append(cur);
        stringBuilder4.append(", ant= ");
        stringBuilder4.append(ant);
        Log.d(str2, stringBuilder4.toString());
        if ((this.mPhoneId == 1 && TelephonyManager.getDefault().getSimState(0) == 1) || (this.mPhoneId == 0 && TelephonyManager.getDefault().getSimState(1) == 1)) {
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("checkMultiSmsToMmsTextThreshold>>#one card on#mPhoneId=");
            stringBuilder.append(this.mPhoneId);
            stringBuilder.append(", cur = ");
            stringBuilder.append(cur);
            Log.d(str, stringBuilder.toString());
            set_threshold = cur;
        } else {
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("checkMultiSmsToMmsTextThreshold>>#dual card on#mPhoneId=");
            stringBuilder.append(this.mPhoneId);
            stringBuilder.append(", cur = ");
            stringBuilder.append(cur);
            stringBuilder.append(", ant = ");
            stringBuilder.append(ant);
            Log.d(str, stringBuilder.toString());
            if (cur == 0) {
                set_threshold = ant;
            } else if (cur == -1) {
                if (ant == -1 || ant == 0) {
                    set_threshold = cur;
                } else {
                    set_threshold = ant;
                }
            } else if (ant == -1 || ant == 0) {
                set_threshold = cur;
            } else {
                set_threshold = ant < cur ? ant : cur;
            }
        }
        str = LOG_TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("checkMultiSmsToMmsTextThreshold set_threshold= ");
        stringBuilder.append(set_threshold);
        Log.d(str, stringBuilder.toString());
        SystemProperties.set("gsm.sms.to.mms.textthreshold", Integer.toString(set_threshold));
    }

    private boolean isCustPlmn(Context context, String simMccMnc) {
        String custPlmnsString = System.getString(context.getContentResolver(), "hw_cust_7bit_enabled_mcc");
        if (TextUtils.isEmpty(custPlmnsString) || TextUtils.isEmpty(simMccMnc) || simMccMnc.length() < 3) {
            return false;
        }
        String[] custPlmns = custPlmnsString.split(";");
        int i = 0;
        while (i < custPlmns.length) {
            if (simMccMnc.substring(0, 3).equals(custPlmns[i]) || simMccMnc.equals(custPlmns[i])) {
                return true;
            }
            i++;
        }
        return false;
    }

    private void globalAutoMatch() {
        Integer num_match_slot;
        Integer num_match_short_slot;
        Integer max_message_size_slot;
        Integer sms_to_mms_textthreshold_slot;
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            num_match_slot = (Integer) HwCfgFilePolicy.getValue("num_match", this.mPhoneId, Integer.class);
            if (num_match_slot != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("gsm.hw.matchnum");
                stringBuilder.append(this.mPhoneId);
                SystemProperties.set(stringBuilder.toString(), num_match_slot.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("gsm.hw.matchnum.short");
                stringBuilder.append(this.mPhoneId);
                SystemProperties.set(stringBuilder.toString(), num_match_slot.toString());
            }
            num_match_short_slot = (Integer) HwCfgFilePolicy.getValue("num_match_short", this.mPhoneId, Integer.class);
            if (num_match_short_slot != null) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("gsm.hw.matchnum.short");
                stringBuilder2.append(this.mPhoneId);
                SystemProperties.set(stringBuilder2.toString(), num_match_short_slot.toString());
            }
            checkMultiSimNumMatch();
            max_message_size_slot = (Integer) HwCfgFilePolicy.getValue("max_message_size", this.mPhoneId, Integer.class);
            if (max_message_size_slot != null) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("gsm.sms.max.message.size");
                stringBuilder3.append(this.mPhoneId);
                SystemProperties.set(stringBuilder3.toString(), max_message_size_slot.toString());
            }
            checkMultiSimMaxMessageSize();
            sms_to_mms_textthreshold_slot = (Integer) HwCfgFilePolicy.getValue("sms_to_mms_textthreshold", this.mPhoneId, Integer.class);
            if (sms_to_mms_textthreshold_slot != null && sms_to_mms_textthreshold_slot.intValue() >= -1) {
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("gsm.sms.to.mms.textthreshold");
                stringBuilder4.append(this.mPhoneId);
                SystemProperties.set(stringBuilder4.toString(), sms_to_mms_textthreshold_slot.toString());
            }
            checkMultiSmsToMmsTextThreshold();
        } else {
            num_match_slot = (Integer) HwCfgFilePolicy.getValue("num_match", Integer.class);
            if (num_match_slot != null) {
                SystemProperties.set("gsm.hw.matchnum", num_match_slot.toString());
                SystemProperties.set("gsm.hw.matchnum.short", num_match_slot.toString());
            }
            num_match_short_slot = (Integer) HwCfgFilePolicy.getValue("num_match_short", Integer.class);
            if (num_match_short_slot != null) {
                SystemProperties.set("gsm.hw.matchnum.short", num_match_short_slot.toString());
            }
            max_message_size_slot = (Integer) HwCfgFilePolicy.getValue("max_message_size", Integer.class);
            if (max_message_size_slot != null) {
                SystemProperties.set("gsm.sms.max.message.size", max_message_size_slot.toString());
            }
            sms_to_mms_textthreshold_slot = (Integer) HwCfgFilePolicy.getValue("sms_to_mms_textthreshold", Integer.class);
            if (sms_to_mms_textthreshold_slot != null && sms_to_mms_textthreshold_slot.intValue() >= -1) {
                SystemProperties.set("gsm.sms.to.mms.textthreshold", sms_to_mms_textthreshold_slot.toString());
            }
        }
        num_match_slot = (Integer) HwCfgFilePolicy.getValue("sms_7bit_enabled", Integer.class);
        if (num_match_slot != null) {
            SystemProperties.set("gsm.sms.7bit.enabled", num_match_slot.toString());
        }
        Integer sms_coding_national = (Integer) HwCfgFilePolicy.getValue("sms_coding_national", Integer.class);
        if (sms_coding_national != null) {
            SystemProperties.set("gsm.sms.coding.national", sms_coding_national.toString());
        }
    }

    public void checkGlobalAutoMatchParam(String currentMccmnc, Context context) {
        globalAutoMatch();
        String where = new StringBuilder();
        where.append("numeric=\"");
        where.append(currentMccmnc);
        where.append("\"");
        try {
            Cursor cursor = context.getContentResolver().query(NumMatchs.CONTENT_URI, new String[]{"_id", NumMatchs.IS_VMN_SHORT_CODE}, where.toString(), null, NumMatchs.DEFAULT_SORT_ORDER);
            if (cursor == null) {
                Log.e(LOG_TAG, "SIMRecords:checkGlobalAutoMatchParam: No matched auto match params in db.");
                return;
            }
            String str;
            StringBuilder stringBuilder;
            try {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    int is_vmn_short_code = cursor.getInt(1);
                    if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                        str = LOG_TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("checkGlobalAutoMatchParam mPhoneId = ");
                        stringBuilder.append(this.mPhoneId);
                        stringBuilder.append(" simState = ");
                        stringBuilder.append(TelephonyManager.getDefault().getSimState(this.mPhoneId));
                        Log.d(str, stringBuilder.toString());
                        if (this.mPhoneId == 1 && TelephonyManager.getDefault().getSimState(0) == 5) {
                            Log.d(LOG_TAG, "card 2 ready, card 1 ready, just return, don't go into send broadcast below");
                            cursor.close();
                            return;
                        }
                    }
                    SystemProperties.set("gsm.hw.matchnum.vmn_shortcode", Integer.toString(is_vmn_short_code));
                    str = LOG_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("SIMRecords:checkGlobalAutoMatchParam: after setprop numMatch = ");
                    stringBuilder.append(SystemProperties.getInt("gsm.hw.matchnum", 0));
                    stringBuilder.append(", numMatchShort = ");
                    stringBuilder.append(SystemProperties.getInt("gsm.hw.matchnum.short", 0));
                    stringBuilder.append(", sms7BitEnabled = ");
                    stringBuilder.append(SystemProperties.getBoolean("gsm.sms.7bit.enabled", false));
                    stringBuilder.append(", smsCodingNational = ");
                    stringBuilder.append(SystemProperties.getInt("gsm.sms.coding.national", 0));
                    stringBuilder.append(", max_message_size = ");
                    stringBuilder.append(SystemProperties.getInt("gsm.sms.max.message.size", 0));
                    stringBuilder.append(", sms_to_mms_textthreshold = ");
                    stringBuilder.append(SystemProperties.getInt("gsm.sms.to.mms.textthreshold", 0));
                    Log.d(str, stringBuilder.toString());
                    cursor.moveToNext();
                }
            } catch (Exception ex) {
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("SIMRecords:checkGlobalAutoMatchParam: global version cause exception!");
                stringBuilder.append(ex.toString());
                Log.e(str, stringBuilder.toString());
            } catch (Throwable th) {
                cursor.close();
            }
            cursor.close();
            int[] temp = new int[1];
            if (SystemProperties.getInt("ro.config.smsCoding_National", 0) != 0) {
                temp[0] = SystemProperties.getInt("ro.config.smsCoding_National", 0);
                GsmAlphabet.setEnabledSingleShiftTables(temp);
            } else if (SystemProperties.getInt("gsm.sms.coding.national", 0) != 0) {
                temp[0] = SystemProperties.getInt("gsm.sms.coding.national", 0);
                GsmAlphabet.setEnabledSingleShiftTables(temp);
            }
            if (TelephonyManager.getDefault().isMultiSimEnabled() && this.mPhoneId == 1 && TelephonyManager.getDefault().getSimState(0) != 1) {
                Log.d(LOG_TAG, " card 2 ready, card 1 inserted, maybe not ready, don't send broadcast");
                return;
            }
            Intent intentSetGlobalParamDone = new Intent(ACTION_SET_GLOBAL_AUTO_PARAM_DONE);
            intentSetGlobalParamDone.putExtra("mccMnc", currentMccmnc);
            context.sendStickyBroadcast(intentSetGlobalParamDone);
        } catch (Exception e) {
            Log.e(LOG_TAG, "checkGlobalAutoMatchParam: unable to open database file.");
        }
    }

    public void checkGlobalEccNum(String currentMccmnc, Context context) {
        if (currentMccmnc != null) {
            String[] custNumItem;
            String custEccNumsStrFromGlobalMatch = null;
            String custEccNumsStr = null;
            String where = new StringBuilder();
            where.append("numeric=\"");
            where.append(currentMccmnc);
            where.append("\"");
            Cursor cursor = context.getContentResolver().query(GlobalMatchs.CONTENT_URI, new String[]{"_id", "name", "numeric", "ecc_fake"}, where.toString(), null, NumMatchs.DEFAULT_SORT_ORDER);
            if (cursor != null) {
                try {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        custEccNumsStrFromGlobalMatch = cursor.getString(3);
                        cursor.moveToNext();
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "global version cause exception!", e);
                } catch (Throwable th) {
                    cursor.close();
                }
                cursor.close();
            }
            if (custEccNumsStrFromGlobalMatch == null || custEccNumsStrFromGlobalMatch.equals("")) {
                try {
                    custEccNumsStr = System.getString(context.getContentResolver(), "global_cust_ecc_nums");
                } catch (Exception e2) {
                    Log.e(LOG_TAG, "Could not load default locales", e2);
                    return;
                }
            }
            try {
                if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("gsm.hw.cust.ecclist");
                    stringBuilder.append(this.mPhoneId);
                    SystemProperties.set(stringBuilder.toString(), null);
                } else {
                    SystemProperties.set("gsm.hw.cust.ecclist", null);
                }
            } catch (IllegalArgumentException e3) {
                Log.e(LOG_TAG, "Failed to save ril.ecclist to system property", e3);
            }
            StringBuilder stringBuilder2;
            if (custEccNumsStrFromGlobalMatch != null && !custEccNumsStrFromGlobalMatch.equals("")) {
                try {
                    if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("gsm.hw.cust.ecclist");
                        stringBuilder2.append(this.mPhoneId);
                        SystemProperties.set(stringBuilder2.toString(), custEccNumsStrFromGlobalMatch);
                    } else {
                        SystemProperties.set("gsm.hw.cust.ecclist", custEccNumsStrFromGlobalMatch);
                    }
                } catch (Exception e22) {
                    Log.e(LOG_TAG, "Failed to save ril.ecclist to system property", e22);
                }
            } else if (!(custEccNumsStr == null || custEccNumsStr.equals(""))) {
                String[] custEccNumsItems = custEccNumsStr.split(";");
                custNumItem = null;
                for (String split : custEccNumsItems) {
                    custNumItem = split.split(":");
                    if (2 == custNumItem.length && (custNumItem[0].equalsIgnoreCase(currentMccmnc) || custNumItem[0].equalsIgnoreCase(currentMccmnc.substring(0, 3)))) {
                        try {
                            if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("gsm.hw.cust.ecclist");
                                stringBuilder2.append(this.mPhoneId);
                                SystemProperties.set(stringBuilder2.toString(), custNumItem[1]);
                            } else {
                                SystemProperties.set("gsm.hw.cust.ecclist", custNumItem[1]);
                            }
                        } catch (Exception e222) {
                            Log.e(LOG_TAG, "Failed to save ril.ecclist to system property", e222);
                        }
                    }
                }
            }
            custNumItem = null;
        }
    }

    public void checkAgpsServers(String currentMccmnc) {
        if (currentMccmnc != null) {
            if (loadAgpsServer(currentMccmnc, HWCFGPOLICYPATH)) {
                Log.d(LOG_TAG, "loadAgpsServer from hwCfgPolicyPath sucess");
            } else if (loadAgpsServer(currentMccmnc, CUSTXMLPATH)) {
                Log.d(LOG_TAG, "loadAgpsServer from cust sucess");
            } else if (loadAgpsServer(currentMccmnc, SYSTEMXMLPATH)) {
                Log.d(LOG_TAG, "loadAgpsServer from system/etc sucess");
            } else {
                Log.d(LOG_TAG, "loadAgpsServer faild,can't find globalAgpsServers-conf.xml");
            }
        }
    }

    private boolean loadAgpsServer(String currentMccmnc, String filePath) {
        String str;
        StringBuilder stringBuilder;
        File confFile = new File("/data/cust", "xml/globalAgpsServers-conf.xml");
        if (HWCFGPOLICYPATH.equals(filePath)) {
            try {
                File cfg = HwCfgFilePolicy.getCfgFile("xml/globalAgpsServers-conf.xml", 0);
                if (cfg == null) {
                    return false;
                }
                confFile = cfg;
            } catch (NoClassDefFoundError e) {
                Log.w(LOG_TAG, "NoClassDefFoundError : HwCfgFilePolicy ");
                return false;
            }
        }
        confFile = new File(filePath, globalAgpsServersFileName);
        InputStreamReader confreader = null;
        try {
            confreader = new InputStreamReader(new FileInputStream(confFile), Charset.defaultCharset());
            XmlPullParser confparser = Xml.newPullParser();
            if (confparser != null) {
                ContentValues row;
                confparser.setInput(confreader);
                XmlUtils.beginDocument(confparser, APGS_SERVERS_DOCUMENT);
                while (true) {
                    XmlUtils.nextElement(confparser);
                    row = getAgpsServerRow(confparser);
                    if (row != null) {
                        if (currentMccmnc.equals(row.getAsString(MCCMNC))) {
                            break;
                        } else if (currentMccmnc.substring(0, 3).equals(row.getAsString(MCCMNC))) {
                            break;
                        }
                    }
                }
                broadcastAgpsServerConf(row);
            }
            try {
                confreader.close();
                String str2 = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("AgpsServer file is successfully load from filePath:");
                stringBuilder2.append(filePath);
                Log.d(str2, stringBuilder2.toString());
                return true;
            } catch (IOException e2) {
            }
            return false;
        } catch (FileNotFoundException e3) {
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("File not found: '");
            stringBuilder.append(confFile.getAbsolutePath());
            stringBuilder.append("'");
            Log.e(str, stringBuilder.toString());
            if (confreader != null) {
                confreader.close();
            }
            return false;
        } catch (Exception e4) {
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception while parsing '");
            stringBuilder.append(confFile.getAbsolutePath());
            stringBuilder.append("'");
            Log.e(str, stringBuilder.toString(), e4);
            if (confreader != null) {
                try {
                    confreader.close();
                } catch (IOException e5) {
                }
            }
            return false;
        } catch (Throwable th) {
            if (confreader != null) {
                confreader.close();
            }
        }
    }

    private ContentValues getAgpsServerRow(XmlPullParser parser) {
        if (!"agpsServer".equals(parser.getName())) {
            return null;
        }
        ContentValues map = new ContentValues();
        map.put("name", parser.getAttributeValue(null, "name"));
        map.put(MCCMNC, parser.getAttributeValue(null, MCCMNC));
        map.put(SUPL_PORT, parser.getAttributeValue(null, SUPL_PORT));
        map.put(SUPL_URL, parser.getAttributeValue(null, SUPL_URL));
        return map;
    }

    private void broadcastAgpsServerConf(ContentValues row) {
        if (row != null) {
            Log.d(LOG_TAG, "broadcast HwTelephonyIntentsInner.ACTION_AGPS_SERVERS");
            Intent intent = new Intent("android.intent.action.ACTION_AGPS_SERVERS");
            intent.addFlags(536870912);
            intent.putExtra(SUPL_URL, row.getAsString(SUPL_URL));
            intent.putExtra(SUPL_PORT, row.getAsString(SUPL_PORT));
            ActivityManagerNative.broadcastStickyIntent(intent, null, 0);
        }
    }

    public static boolean getPrePostPayPreCondition() {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("waitingForSetupData = ");
        stringBuilder.append(waitingForSetupData);
        Log.d(str, stringBuilder.toString());
        str = LOG_TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("IS_PRE_POST_PAY = ");
        stringBuilder.append(IS_PRE_POST_PAY);
        Log.d(str, stringBuilder.toString());
        if (true == waitingForSetupData && true == IS_PRE_POST_PAY) {
            return true;
        }
        return false;
    }

    private static void setWaitingForSetupData(boolean value) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setWaitingForSetupData, value = ");
        stringBuilder.append(value);
        Log.d(str, stringBuilder.toString());
        waitingForSetupData = value;
    }

    public static void tryToActionPrePostPay() {
        Log.d(LOG_TAG, "broadcast HwTelephonyIntentsInner.ACTION_PRE_POST_PAY");
        Intent intent = new Intent("android.intent.action.ACTION_PRE_POST_PAY");
        intent.addFlags(536870912);
        intent.putExtra("prePostPayState", true);
        ActivityManagerNative.broadcastStickyIntent(intent, null, 0);
    }

    private void checkMultiSimNumMatch() {
        matchArray = new int[4];
        int i = 2;
        matchArray[2] = SystemProperties.getInt("gsm.hw.matchnum1", -1);
        matchArray[3] = SystemProperties.getInt("gsm.hw.matchnum.short1", -1);
        Arrays.sort(matchArray);
        int numMatch = matchArray[3];
        int numMatchShort = numMatch;
        while (i >= 0) {
            if (matchArray[i] < numMatch && matchArray[i] > 0) {
                numMatchShort = matchArray[i];
            }
            i--;
        }
        if (numMatch >= 0) {
            SystemProperties.set("gsm.hw.matchnum", Integer.toString(numMatch));
        }
        if (numMatchShort >= 0) {
            SystemProperties.set("gsm.hw.matchnum.short", Integer.toString(numMatchShort));
        }
    }

    /* JADX WARNING: Missing block: B:24:0x0055, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void checkCustLongVMNum(String currentMccmnc, Context context) {
        if (getHwSupportLongVmnum() && currentMccmnc != null && !loadCustLongVmnumFromCard()) {
            String custLongVMNumStr = null;
            try {
                custLongVMNumStr = System.getString(context.getContentResolver(), "hw_cust_long_vmNum");
            } catch (Exception e) {
                Rlog.e(LOG_TAG, "Failed to load vmNum from SettingsEx", e);
            }
            if (!TextUtils.isEmpty(custLongVMNumStr)) {
                String[] custLongVMNumsItems = custLongVMNumStr.split(";");
                for (String[] custNumItem : custLongVMNumsItems) {
                    String[] custNumItem2 = custNumItem2.split(":");
                    if (2 == custNumItem2.length && custNumItem2[0].equalsIgnoreCase(currentMccmnc)) {
                        setPropForCustLongVmnum(custNumItem2[1]);
                        break;
                    }
                }
            }
        }
    }

    public void queryRoamingNumberMatchRuleByNetwork(String mccmnc, Context context) {
        SystemProperties.set("gsm.hw.matchnum.roaming", Integer.toString(-1));
        SystemProperties.set("gsm.hw.matchnum.short.roaming", Integer.toString(-1));
        String where = new StringBuilder();
        where.append("numeric=\"");
        where.append(mccmnc);
        where.append("\"");
        try {
            Cursor cursor = context.getContentResolver().query(NumMatchs.CONTENT_URI, new String[]{"_id", "name", "numeric", "num_match", "num_match_short"}, where.toString(), null, NumMatchs.DEFAULT_SORT_ORDER);
            if (cursor == null) {
                Log.e(LOG_TAG, "queryNumberMatchRuleByNetwork: No matched number match params in db.");
                return;
            }
            try {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    int numMatch = cursor.getInt(3);
                    int numMatchShort = cursor.getInt(4);
                    if (numMatchShort == 0) {
                        numMatchShort = numMatch;
                    }
                    SystemProperties.set("gsm.hw.matchnum.roaming", Integer.toString(numMatch));
                    SystemProperties.set("gsm.hw.matchnum.short.roaming", Integer.toString(numMatchShort));
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("queryNumberMatchRuleByNetwork: after setprop numMatch = ");
                    stringBuilder.append(SystemProperties.getInt("gsm.hw.matchnum.roaming", -1));
                    stringBuilder.append(", numMatchShort = ");
                    stringBuilder.append(SystemProperties.getInt("gsm.hw.matchnum.short.roaming", -1));
                    Log.d(str, stringBuilder.toString());
                    cursor.moveToNext();
                }
            } catch (Exception ex) {
                String str2 = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("queryNumberMatchRuleByNetwork: global version cause exception!");
                stringBuilder2.append(ex.toString());
                Log.e(str2, stringBuilder2.toString());
            } catch (Throwable th) {
                cursor.close();
            }
            cursor.close();
        } catch (Exception e) {
            Log.e(LOG_TAG, "queryNumberMatchRuleByNetwork: unable to open database file.");
        }
    }

    public void checkValidityOfRoamingNumberMatchRule() {
        int numMatch = SystemProperties.getInt("gsm.hw.matchnum.roaming", -1);
        int numMatchShort = SystemProperties.getInt("gsm.hw.matchnum.short.roaming", -1);
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("checkValidityOfRoamingNumberMatchRule: numMatch = ");
        stringBuilder.append(numMatch);
        stringBuilder.append(", numMatchShort = ");
        stringBuilder.append(numMatchShort);
        Log.d(str, stringBuilder.toString());
        if (numMatch <= 0 || numMatchShort <= 0) {
            numMatchShort = 7;
            numMatch = 7;
            SystemProperties.set("gsm.hw.matchnum.roaming", Integer.toString(numMatch));
            SystemProperties.set("gsm.hw.matchnum.short.roaming", Integer.toString(numMatchShort));
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("checkValidityOfRoamingNumberMatchRule: after validity check numMatch = ");
            stringBuilder.append(numMatch);
            stringBuilder.append(", numMatchShort = ");
            stringBuilder.append(numMatchShort);
            Log.d(str, stringBuilder.toString());
        }
    }

    private boolean getHwSupportLongVmnum() {
        Boolean valueFromCard = (Boolean) HwCfgFilePolicy.getValue("hw_support_long_vmNum", this.mPhoneId, Boolean.class);
        boolean valueFromProp = IS_SUPPORT_LONG_VMNUM;
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getHwSupportLongVmnum, card:");
        stringBuilder.append(valueFromCard);
        stringBuilder.append(", prop:");
        stringBuilder.append(valueFromProp);
        stringBuilder.append(", mPhoneId: ");
        stringBuilder.append(this.mPhoneId);
        Rlog.d(str, stringBuilder.toString());
        return valueFromCard != null ? valueFromCard.booleanValue() : valueFromProp;
    }

    private boolean loadCustLongVmnumFromCard() {
        String longVmNum = (String) HwCfgFilePolicy.getValue("hw_cust_long_vmNum", this.mPhoneId, String.class);
        if (longVmNum == null) {
            return false;
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("loadCustLongVmnumFromCard: longVmNum's length:");
        stringBuilder.append(longVmNum.length());
        stringBuilder.append(", mPhoneId: ");
        stringBuilder.append(this.mPhoneId);
        Rlog.d(str, stringBuilder.toString());
        setPropForCustLongVmnum(longVmNum);
        return true;
    }

    private void setPropForCustLongVmnum(String longVmnum) {
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("gsm.hw.cust.longvmnum");
            stringBuilder.append(this.mPhoneId);
            SystemProperties.set(stringBuilder.toString(), longVmnum);
            return;
        }
        SystemProperties.set("gsm.hw.cust.longvmnum", longVmnum);
    }
}
