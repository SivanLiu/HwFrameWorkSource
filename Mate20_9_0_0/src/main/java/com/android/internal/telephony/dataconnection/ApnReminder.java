package com.android.internal.telephony.dataconnection;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.NetworkUtils;
import android.net.Uri;
import android.provider.HwTelephony.NumMatchs;
import android.provider.Settings.System;
import android.provider.Telephony.Carriers;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Xml;
import android.view.ContextThemeWrapper;
import com.android.internal.telephony.HwSubscriptionManager;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConstants;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.vsim.HwVSimConstants;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import huawei.cust.HwCfgFilePolicy;
import huawei.cust.HwCustUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;

public class ApnReminder {
    private static String APN_APN = "apn";
    private static String APN_DISPLAY_NAME = "displayName";
    private static final String APN_ID = "apn_id";
    private static String APN_NAME = NumMatchs.NAME;
    private static String APN_ONS_NAME = "onsName";
    private static String APN_OTHER_APNS = "otherApns";
    private static String APN_REMINDER_DOCUMENT = "apnReminderList";
    private static String APN_VOICEMAIL_NUMBER = "vmNumber";
    private static String APN_VOICEMAIL_TAG = "vmTag";
    private static String ATTRIBUTE_PLMN = "plmn";
    private static String ATTRIBUTE_REMIND_TYPE = "remindType";
    private static String ATTRIBUTE_REMIND_TYPE_APN_FAILED = "apnFailed";
    private static String ATTRIBUTE_REMIND_TYPE_IMSI_CHANGE = "imsiChange";
    private static String ATTRIBUTE_TITLE = "title";
    private static int GID1_VIRGIN_MEDIA = 40;
    private static String LAST_IMSI = "apn_reminder_last_imsi";
    private static String LOG_TAG = "GSM";
    private static String NODE_APN = "apnSetting";
    private static String NODE_APN_REMINDER = "apnReminder";
    private static String PLMN_VIRGIN_MEDIA = "23430";
    private static final Uri PREFERAPN_NO_UPDATE_URI = Uri.parse("content://telephony/carriers/preferapn_no_update");
    private static final Uri PREFERAPN_UPDATE_URI = Uri.parse("content://telephony/carriers/preferapn");
    private static final int QUERY_TYPE_ONS_NAME = 1;
    private static final int QUERY_TYPE_VOICEMAIL_NUMBER = 2;
    private static final int QUERY_TYPE_VOICEMAIL_TAG = 3;
    private static int REMIND_TYPE_ALL_APN_FAILED = 2;
    private static int REMIND_TYPE_IMSI_CHANGE = 1;
    private static final Uri URL_TELEPHONY_USING_SUBID = Uri.parse("content://telephony/carriers/subId");
    private static String custFileName = "apn_reminder.xml";
    private static String custFilePath = "/data/cust/xml/";
    private static String hwCfgPolicyPath = "hwCfgPolicyPath";
    private static ApnReminder instance = null;
    private static ApnReminder instance1 = null;
    private static final boolean isMultiSimEnabled = TelephonyManager.getDefault().isMultiSimEnabled();
    private static String systemFilePath = "/system/etc/";
    boolean allApnFailed = false;
    boolean dialogDispalyed = false;
    boolean imsiChanged = false;
    Context mContext;
    private int mGID1 = HwSubscriptionManager.SUB_INIT_STATE;
    private HwCustApnReminder mHwCustApnReminder;
    String mImsi;
    String mPlmn;
    int mRemindType;
    private String mShowAPNMccMnc;
    int mSlotId = 0;
    String mTitle;
    ArrayList<PopupApnConfig> myPopupApnConfigs;
    ArrayList<PopupApnSettings> myPopupApnSettings;
    boolean restoreApn = false;

    static class PopupApnConfig {
        String apn;
        String displayName;
        String name;
        String onsName;
        String otherApns;
        String vmNumber;
        String vmTag;

        PopupApnConfig() {
        }
    }

    static class PopupApnSettings {
        String displayName;
        int id;
        String onsName;
        ArrayList<Integer> otherApnIds;
        String vmNumber;
        String vmTag;

        PopupApnSettings() {
        }
    }

    public static synchronized ApnReminder getInstance(Context context) {
        ApnReminder apnReminder;
        synchronized (ApnReminder.class) {
            if (instance == null) {
                instance = new ApnReminder(context);
            }
            apnReminder = instance;
        }
        return apnReminder;
    }

    public static synchronized ApnReminder getInstance(Context context, int slotId) {
        synchronized (ApnReminder.class) {
            ApnReminder apnReminder;
            if (slotId == 1) {
                if (instance1 == null) {
                    instance1 = new ApnReminder(context, slotId);
                }
                apnReminder = instance1;
                return apnReminder;
            }
            if (instance == null) {
                instance = new ApnReminder(context, slotId);
            }
            apnReminder = instance;
            return apnReminder;
        }
    }

    private ApnReminder(Context context) {
        this.mContext = context;
        this.mHwCustApnReminder = (HwCustApnReminder) HwCustUtils.createObj(HwCustApnReminder.class, new Object[0]);
        this.mShowAPNMccMnc = System.getString(this.mContext.getContentResolver(), "hw_show_add_apn_plmn");
    }

    private ApnReminder(Context context, int slotId) {
        this.mContext = context;
        this.mSlotId = slotId;
        this.mHwCustApnReminder = (HwCustApnReminder) HwCustUtils.createObj(HwCustApnReminder.class, new Object[0]);
        this.mShowAPNMccMnc = System.getString(this.mContext.getContentResolver(), "hw_show_add_apn_plmn");
    }

    private void callForLog() {
        if (loadApnReminder(hwCfgPolicyPath)) {
            logd("loadApnReminder from hwCfgPolicyPath success");
        } else if (loadApnReminder(custFilePath)) {
            logd("loadApnReminder from cust success");
        } else if (loadApnReminder(systemFilePath)) {
            logd("loadApnReminder from system/etc success");
        } else {
            logd("can't find apn_reminder.xml, load failed!");
        }
    }

    private void loadAllApnIfNeeded() {
        if (this.mRemindType > 0) {
            if (this.imsiChanged || this.restoreApn) {
                logd("imsiChanged delete preference apn");
                ContentResolver resolver = this.mContext.getContentResolver();
                if (isMultiSimEnabled) {
                    resolver.delete(ContentUris.withAppendedId(PREFERAPN_NO_UPDATE_URI, (long) this.mSlotId), null, null);
                } else {
                    resolver.delete(PREFERAPN_NO_UPDATE_URI, null, null);
                }
            }
            loadAllApn();
        }
    }

    public void setPlmnAndImsi(String plmn, String imsi) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setPlmnAndImsi plmn = ");
        stringBuilder.append(plmn);
        logd(stringBuilder.toString());
        if (plmn != null && imsi != null) {
            this.mPlmn = plmn;
            this.mImsi = imsi;
            if (this.mContext != null) {
                String oldImsi;
                SharedPreferences sp = this.mContext.getSharedPreferences("ApnReminderImsi", 0);
                if (isMultiSimEnabled) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(LAST_IMSI);
                    stringBuilder2.append(this.mSlotId);
                    oldImsi = sp.getString(stringBuilder2.toString(), null);
                } else {
                    oldImsi = sp.getString(LAST_IMSI, null);
                }
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("setPlmnAndImsi for card");
                stringBuilder3.append(this.mSlotId);
                logd(stringBuilder3.toString());
                if (oldImsi != null) {
                    oldImsi = new String(Base64.decode(oldImsi, 0));
                }
                if (this.mImsi.trim().length() <= 0 || this.mImsi.equals(oldImsi)) {
                    this.imsiChanged = false;
                    logd("setPlmnAndImsi not imsiChanged");
                } else {
                    this.imsiChanged = true;
                    this.dialogDispalyed = false;
                    this.allApnFailed = false;
                    this.restoreApn = false;
                    logd("setPlmnAndImsi imsiChanged");
                    this.mHwCustApnReminder.notifyDisableAp(this.mContext, oldImsi);
                    this.mHwCustApnReminder.deletePreferApn(this.mContext, this.mImsi, this.mSlotId);
                }
                Editor editor = sp.edit();
                if (isMultiSimEnabled) {
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append(LAST_IMSI);
                    stringBuilder4.append(this.mSlotId);
                    editor.putString(stringBuilder4.toString(), new String(Base64.encode(this.mImsi.getBytes(), 0)));
                } else {
                    editor.putString(LAST_IMSI, new String(Base64.encode(this.mImsi.getBytes(), 0)));
                }
                editor.commit();
            }
            this.myPopupApnConfigs = new ArrayList();
            callForLog();
            loadAllApnIfNeeded();
        }
    }

    private boolean loadApnReminder(String filePath) {
        StringBuilder stringBuilder;
        File confFile = new File("/data/cust", "xml/apn_reminder.xml");
        if (hwCfgPolicyPath.equals(filePath)) {
            try {
                File cfg = HwCfgFilePolicy.getCfgFile("xml/apn_reminder.xml", 0);
                if (cfg == null) {
                    return false;
                }
                confFile = cfg;
            } catch (NoClassDefFoundError e) {
                Log.w(LOG_TAG, "NoClassDefFoundError : HwCfgFilePolicy ");
                return false;
            }
        }
        confFile = new File(filePath, custFileName);
        FileInputStream fin = null;
        try {
            StringBuilder stringBuilder2;
            fin = new FileInputStream(confFile);
            XmlPullParser confparser = Xml.newPullParser();
            if (confparser != null) {
                confparser.setInput(fin, "UTF-8");
                XmlUtils.beginDocument(confparser, APN_REMINDER_DOCUMENT);
                while (true) {
                    int next = confparser.next();
                    int nodeType = next;
                    if (next != 1) {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("parse xml confparser.getName() = ");
                        stringBuilder3.append(confparser.getName());
                        logd(stringBuilder3.toString());
                        if (NODE_APN_REMINDER.equals(confparser.getName())) {
                            if (nodeType == 2) {
                                StringBuilder stringBuilder4 = new StringBuilder();
                                stringBuilder4.append(NODE_APN_REMINDER);
                                stringBuilder4.append(" tag parse");
                                logd(stringBuilder4.toString());
                                String nodePlmn = confparser.getAttributeValue(null, ATTRIBUTE_PLMN);
                                StringBuilder stringBuilder5 = new StringBuilder();
                                stringBuilder5.append("parse plmn is ");
                                stringBuilder5.append(nodePlmn);
                                logd(stringBuilder5.toString());
                                if (this.mPlmn.equals(nodePlmn)) {
                                    String nodeRemindType = confparser.getAttributeValue(null, ATTRIBUTE_REMIND_TYPE);
                                    this.mTitle = confparser.getAttributeValue(null, ATTRIBUTE_TITLE);
                                    int remindType = 0;
                                    if (ATTRIBUTE_REMIND_TYPE_IMSI_CHANGE.equals(nodeRemindType)) {
                                        remindType = REMIND_TYPE_IMSI_CHANGE;
                                    } else if (ATTRIBUTE_REMIND_TYPE_APN_FAILED.equals(nodeRemindType)) {
                                        remindType = REMIND_TYPE_ALL_APN_FAILED;
                                    }
                                    StringBuilder stringBuilder6 = new StringBuilder();
                                    stringBuilder6.append("parse remindType is ");
                                    stringBuilder6.append(remindType);
                                    logd(stringBuilder6.toString());
                                    if (remindType > 0) {
                                        this.mRemindType = remindType;
                                        while (true) {
                                            int next2 = confparser.next();
                                            nodeType = next2;
                                            if (next2 != 1) {
                                                stringBuilder6 = new StringBuilder();
                                                stringBuilder6.append("parse confparser name ");
                                                stringBuilder6.append(confparser.getName());
                                                logd(stringBuilder6.toString());
                                                if (!NODE_APN.equals(confparser.getName()) || nodeType != 2) {
                                                    if (NODE_APN_REMINDER.equals(confparser.getName()) && nodeType == 2) {
                                                        stringBuilder2 = new StringBuilder();
                                                        stringBuilder2.append(NODE_APN_REMINDER);
                                                        stringBuilder2.append(" node end");
                                                        logd(stringBuilder2.toString());
                                                        break;
                                                    }
                                                    logd("skip this node");
                                                } else {
                                                    PopupApnConfig popupApnConfig = new PopupApnConfig();
                                                    popupApnConfig.vmTag = confparser.getAttributeValue(null, APN_VOICEMAIL_TAG);
                                                    popupApnConfig.vmNumber = confparser.getAttributeValue(null, APN_VOICEMAIL_NUMBER);
                                                    popupApnConfig.otherApns = confparser.getAttributeValue(null, APN_OTHER_APNS);
                                                    popupApnConfig.onsName = confparser.getAttributeValue(null, APN_ONS_NAME);
                                                    popupApnConfig.displayName = confparser.getAttributeValue(null, APN_DISPLAY_NAME);
                                                    popupApnConfig.apn = confparser.getAttributeValue(null, APN_APN);
                                                    popupApnConfig.name = confparser.getAttributeValue(null, APN_NAME);
                                                    this.myPopupApnConfigs.add(popupApnConfig);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        logd("skip this node");
                    }
                }
            }
            try {
                fin.close();
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("ApnReminder file is successfully load from filePath:");
                stringBuilder2.append(filePath);
                logd(stringBuilder2.toString());
                return true;
            } catch (IOException e2) {
            }
            return false;
        } catch (FileNotFoundException e3) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("File not found: '");
            stringBuilder.append(confFile.getAbsolutePath());
            stringBuilder.append("'");
            logd(stringBuilder.toString());
            if (fin != null) {
                fin.close();
            }
            return false;
        } catch (Exception e4) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception while parsing '");
            stringBuilder.append(confFile.getAbsolutePath());
            stringBuilder.append("'");
            stringBuilder.append(e4);
            logd(stringBuilder.toString());
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException e5) {
                }
            }
            return false;
        } catch (Throwable th) {
            if (fin != null) {
                fin.close();
            }
        }
    }

    private void loadAllApn() {
        if (this.mPlmn != null) {
            String selection = new StringBuilder();
            selection.append("numeric = '");
            selection.append(this.mPlmn);
            selection.append("'");
            selection = selection.toString();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(selection);
            stringBuilder.append(" and carrier_enabled = 1");
            selection = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append("createAllApnList: selection=");
            stringBuilder.append(selection);
            logd(stringBuilder.toString());
            Cursor cursor = this.mContext.getContentResolver().query(getCarriersUriBySlot(), null, selection, null, null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    setAllApn(createApnList(cursor));
                }
                cursor.close();
            }
        }
    }

    private Uri getCarriersUriBySlot() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCarriersUriBySlot, mSlotId is: ");
        stringBuilder.append(this.mSlotId);
        logd(stringBuilder.toString());
        if (this.mSlotId == 0) {
            return Carriers.CONTENT_URI;
        }
        if (this.mSlotId == 1) {
            return ContentUris.withAppendedId(URL_TELEPHONY_USING_SUBID, (long) this.mSlotId);
        }
        return Carriers.CONTENT_URI;
    }

    private ArrayList<ApnSetting> createApnList(Cursor cursor) {
        Cursor cursor2 = cursor;
        ArrayList<ApnSetting> result = new ArrayList();
        if (cursor.moveToFirst()) {
            do {
                result.add(new ApnSetting(cursor2.getInt(cursor2.getColumnIndexOrThrow("_id")), cursor2.getString(cursor2.getColumnIndexOrThrow("numeric")), cursor2.getString(cursor2.getColumnIndexOrThrow(NumMatchs.NAME)), cursor2.getString(cursor2.getColumnIndexOrThrow("apn")), NetworkUtils.trimV4AddrZeros(cursor2.getString(cursor2.getColumnIndexOrThrow("proxy"))), cursor2.getString(cursor2.getColumnIndexOrThrow("port")), NetworkUtils.trimV4AddrZeros(cursor2.getString(cursor2.getColumnIndexOrThrow("mmsc"))), NetworkUtils.trimV4AddrZeros(cursor2.getString(cursor2.getColumnIndexOrThrow("mmsproxy"))), cursor2.getString(cursor2.getColumnIndexOrThrow("mmsport")), cursor2.getString(cursor2.getColumnIndexOrThrow("user")), cursor2.getString(cursor2.getColumnIndexOrThrow("password")), cursor2.getInt(cursor2.getColumnIndexOrThrow("authtype")), parseTypes(cursor2.getString(cursor2.getColumnIndexOrThrow(HwVSimConstants.EXTRA_NETWORK_SCAN_TYPE))), cursor2.getString(cursor2.getColumnIndexOrThrow("protocol")), cursor2.getString(cursor2.getColumnIndexOrThrow("roaming_protocol")), cursor2.getInt(cursor2.getColumnIndexOrThrow("carrier_enabled")) == 1, cursor2.getInt(cursor2.getColumnIndexOrThrow("bearer")), cursor2.getInt(cursor2.getColumnIndexOrThrow("bearer_bitmask")), cursor2.getInt(cursor2.getColumnIndexOrThrow("profile_id")), cursor2.getInt(cursor2.getColumnIndexOrThrow("modem_cognitive")) == 1, cursor2.getInt(cursor2.getColumnIndexOrThrow("max_conns")), cursor2.getInt(cursor2.getColumnIndexOrThrow("wait_time")), cursor2.getInt(cursor2.getColumnIndexOrThrow("max_conns_time")), cursor2.getInt(cursor2.getColumnIndexOrThrow("mtu")), cursor2.getString(cursor2.getColumnIndexOrThrow("mvno_type")), cursor2.getString(cursor2.getColumnIndexOrThrow("mvno_match_data"))));
            } while (cursor.moveToNext());
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("createApnList: X result=");
        stringBuilder.append(result);
        logd(stringBuilder.toString());
        return result;
    }

    private String[] parseTypes(String types) {
        if (types != null && !types.equals("")) {
            return types.split(",");
        }
        return new String[]{"*"};
    }

    private void popupApnListIfNeed() {
        if (this.mImsi == null && this.mPlmn == null) {
            logd("popupApnListIfNeed imsi or plmn not loaded");
        } else if (this.myPopupApnSettings == null || this.myPopupApnSettings.size() == 0) {
            logd("popupApnListIfNeed no myPopupApnSettings content");
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("popupApnListIfNeed mRemindType = ");
            stringBuilder.append(this.mRemindType);
            logd(stringBuilder.toString());
            if ((this.mRemindType == REMIND_TYPE_IMSI_CHANGE && this.imsiChanged) || (this.mRemindType == REMIND_TYPE_IMSI_CHANGE && this.restoreApn)) {
                int gid1 = getGID1();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("popupApnListIfNeed get gid1 = ");
                stringBuilder2.append(gid1);
                stringBuilder2.append(" mPlmn =");
                stringBuilder2.append(this.mPlmn);
                logd(stringBuilder2.toString());
                if (PLMN_VIRGIN_MEDIA.equals(this.mPlmn) && GID1_VIRGIN_MEDIA == gid1) {
                    logd("Virgin Media simcard, do not popup dialog");
                    return;
                }
                logd("popupApnListIfNeed imsi changed");
                showAlertDialog();
            } else if (this.mRemindType == REMIND_TYPE_ALL_APN_FAILED && this.allApnFailed && this.imsiChanged) {
                logd("popupApnListIfNeed allApnFailed");
                showAlertDialog();
            }
        }
    }

    private void showAlertDialog() {
        if (!this.dialogDispalyed) {
            boolean isSetCardTwo = true;
            this.dialogDispalyed = true;
            int themeID = this.mContext.getResources().getIdentifier("androidhwext:style/Theme.Emui.Dialog.Alert", null, null);
            Builder builder = new Builder(new ContextThemeWrapper(this.mContext, themeID), themeID);
            int i = 0;
            if (isMultiSimEnabled) {
                StringBuilder stringBuilder;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("showAlertDialog for double card :");
                stringBuilder2.append(this.mSlotId);
                logd(stringBuilder2.toString());
                boolean isSetCardOne = this.mSlotId == 0 && this.mTitle != null && this.mTitle.trim().length() > 0;
                if (isSetCardOne) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(this.mTitle);
                    stringBuilder.append(" for card1");
                    this.mTitle = stringBuilder.toString();
                }
                if (this.mSlotId != 1 || this.mTitle == null || this.mTitle.trim().length() <= 0) {
                    isSetCardTwo = false;
                }
                if (isSetCardTwo) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(this.mTitle);
                    stringBuilder.append(" for card2");
                    this.mTitle = stringBuilder.toString();
                }
                builder.setTitle(this.mTitle);
            } else {
                logd("showAlertDialog for single card");
                if (this.mTitle == null || this.mTitle.trim().length() <= 0) {
                    builder.setTitle("choose your apn");
                } else {
                    builder.setTitle(this.mTitle);
                }
            }
            builder.setCancelable(false);
            String[] apnChoices = new String[this.myPopupApnSettings.size()];
            int myApnSettingSize = this.myPopupApnSettings.size();
            while (i < myApnSettingSize) {
                apnChoices[i] = ((PopupApnSettings) this.myPopupApnSettings.get(i)).displayName;
                i++;
            }
            builder.setSingleChoiceItems(apnChoices, -1, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which >= 0 && which < ApnReminder.this.myPopupApnSettings.size()) {
                        if (ApnReminder.this.mHwCustApnReminder != null) {
                            ApnReminder.this.mHwCustApnReminder.dataRoamingSwitchForCust(((PopupApnSettings) ApnReminder.this.myPopupApnSettings.get(which)).displayName, ApnReminder.this.mContext, ApnReminder.this.mSlotId, ApnReminder.isMultiSimEnabled);
                        }
                        ApnReminder.this.setSelectedApnKey(which);
                        dialog.dismiss();
                    }
                }
            });
            if (isShowAddAPN(this.mPlmn) && this.mHwCustApnReminder != null) {
                this.mHwCustApnReminder.showNewAddAPN(this.mContext, builder);
            }
            AlertDialog alertDialog = builder.create();
            alertDialog.getWindow().setType(HwFullNetworkConstants.EVENT_GET_PREF_NETWORK_MODE_DONE);
            alertDialog.show();
        }
    }

    public boolean isShowAddAPN(String currentMccMnc) {
        if (!(TextUtils.isEmpty(this.mShowAPNMccMnc) || TextUtils.isEmpty(currentMccMnc))) {
            String[] mccmnc = this.mShowAPNMccMnc.trim().split(",");
            for (Object equals : mccmnc) {
                if (currentMccMnc.equals(equals)) {
                    logd("isShowAddAPN = true");
                    return true;
                }
            }
        }
        logd("isShowAddAPN = false");
        return false;
    }

    private void setSelectedApnKey(int index) {
        logd("setSelectedApnKey: delete");
        ContentResolver resolver = this.mContext.getContentResolver();
        if (isMultiSimEnabled) {
            resolver.delete(ContentUris.withAppendedId(PREFERAPN_NO_UPDATE_URI, (long) this.mSlotId), null, null);
        } else {
            resolver.delete(PREFERAPN_NO_UPDATE_URI, null, null);
        }
        if (index >= 0 && index < this.myPopupApnSettings.size()) {
            int pos = ((PopupApnSettings) this.myPopupApnSettings.get(index)).id;
            if (pos >= 0) {
                logd("setPreferredApn: update");
                ContentValues values = new ContentValues();
                values.put(APN_ID, Integer.valueOf(pos));
                if (isMultiSimEnabled) {
                    resolver.update(ContentUris.withAppendedId(PREFERAPN_UPDATE_URI, (long) this.mSlotId), values, null, null);
                } else {
                    resolver.update(PREFERAPN_UPDATE_URI, values, null, null);
                }
            }
            this.mContext.sendBroadcast(new Intent("android.intent.action.refreshapn"));
        }
    }

    public void allApnActiveFailed() {
        logd("allApnActiveFailed");
        this.allApnFailed = true;
        popupApnListIfNeed();
    }

    private void setAllApn(ArrayList<ApnSetting> apns) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setAllApn myPopupApnConfigs = ");
        stringBuilder.append(this.myPopupApnConfigs);
        logd(stringBuilder.toString());
        this.myPopupApnSettings = new ArrayList();
        if (this.myPopupApnConfigs != null && apns != null) {
            int config_list_size = this.myPopupApnConfigs.size();
            for (int i = 0; i < config_list_size; i++) {
                PopupApnConfig apnConfig = (PopupApnConfig) this.myPopupApnConfigs.get(i);
                int apn_list_size = apns.size();
                for (int j = 0; j < apn_list_size; j++) {
                    ApnSetting apnSetting = (ApnSetting) apns.get(j);
                    if (apnSetting.canHandleType("default")) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("apnConfig.apn = ");
                        stringBuilder2.append(apnConfig.apn);
                        logd(stringBuilder2.toString());
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("apnConfig.name = ");
                        stringBuilder2.append(apnConfig.name);
                        logd(stringBuilder2.toString());
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("apnSetting.apn = ");
                        stringBuilder2.append(apnSetting.apn);
                        logd(stringBuilder2.toString());
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("apnSetting.carrier = ");
                        stringBuilder2.append(apnSetting.carrier);
                        logd(stringBuilder2.toString());
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("apnConfig.displayName = ");
                        stringBuilder2.append(apnConfig.displayName);
                        logd(stringBuilder2.toString());
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("apnConfig.onsName = ");
                        stringBuilder2.append(apnConfig.onsName);
                        logd(stringBuilder2.toString());
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("apnConfig.otherApns = ");
                        stringBuilder2.append(apnConfig.otherApns);
                        logd(stringBuilder2.toString());
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("apnConfig.vmTag = ");
                        stringBuilder2.append(apnConfig.vmTag);
                        logd(stringBuilder2.toString());
                        try {
                            if (!(apnConfig.apn == null || !apnConfig.apn.equals(apnSetting.apn) || apnConfig.name == null || apnSetting.carrier == null || !ArrayUtils.equals(apnConfig.name.getBytes("UTF-8"), apnSetting.carrier.getBytes("UTF-8"), apnConfig.name.getBytes("UTF-8").length) || apnConfig.displayName == null)) {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("find match popupApnSettings displayName = ");
                                stringBuilder2.append(apnConfig.displayName);
                                logd(stringBuilder2.toString());
                                PopupApnSettings popupApnSettings = new PopupApnSettings();
                                popupApnSettings.id = apnSetting.id;
                                popupApnSettings.displayName = apnConfig.displayName;
                                popupApnSettings.onsName = apnConfig.onsName;
                                if (!(apnConfig.otherApns == null || "".equals(apnConfig.otherApns))) {
                                    setOtherApnIds(apns, popupApnSettings, apnConfig);
                                }
                                popupApnSettings.vmNumber = apnConfig.vmNumber;
                                popupApnSettings.vmTag = apnConfig.vmTag;
                                this.myPopupApnSettings.add(popupApnSettings);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            this.myPopupApnConfigs = null;
            popupApnListIfNeed();
        }
    }

    private static void logd(String msg) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[ApnReminder] ");
        stringBuilder.append(msg);
        Log.d(str, stringBuilder.toString());
    }

    public void setGID1(byte[] gid1) {
        if (gid1 != null && gid1.length > 0) {
            int i = 0;
            this.mGID1 = gid1[0];
            int sum = 0;
            String gid1String = IccUtils.bytesToHexString(gid1);
            if (!(gid1String == null || gid1String.length() <= 2 || gid1String.charAt(2) == 'f')) {
                int gid1Length = 0;
                int gid1StringLength = gid1String.length();
                for (int i2 = 0; i2 < gid1StringLength; i2++) {
                    if (gid1String.charAt(i2) == 'f') {
                        gid1Length = i2;
                        break;
                    }
                }
                while (i < gid1Length) {
                    if (gid1String.charAt(i) >= '0' && gid1String.charAt(i) <= '9') {
                        sum = ((sum * 16) + gid1String.charAt(i)) - 48;
                    }
                    if (gid1String.charAt(i) >= 'A' && gid1String.charAt(i) <= 'F') {
                        sum = (((sum * 16) + gid1String.charAt(i)) - 65) + 10;
                    }
                    i++;
                }
                this.mGID1 = sum;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setGID1 mGID1 = ");
            stringBuilder.append(this.mGID1);
            logd(stringBuilder.toString());
        }
    }

    private int getGID1() {
        return this.mGID1;
    }

    public void restoreApn(String plmn, String imsi) {
        logd("restoreApn");
        this.restoreApn = true;
        this.dialogDispalyed = false;
        setPlmnAndImsi(plmn, imsi);
    }

    public boolean isPopupApnSettingsEmpty() {
        if (this.myPopupApnSettings == null || this.myPopupApnSettings.size() == 0) {
            return true;
        }
        return false;
    }

    public String getOnsNameByPreferedApn(int apnId, String plmnValue) {
        return queryValueByPreferedApn(apnId, 1, plmnValue);
    }

    private void setOtherApnIds(ArrayList<ApnSetting> apns, PopupApnSettings popupApnSettings, PopupApnConfig apnConfig) {
        ArrayList<ApnSetting> arrayList = apns;
        PopupApnSettings popupApnSettings2 = popupApnSettings;
        PopupApnConfig popupApnConfig = apnConfig;
        if (arrayList != null && popupApnSettings2 != null && popupApnConfig != null && popupApnConfig.otherApns != null && !"".equals(popupApnConfig.otherApns)) {
            popupApnSettings2.otherApnIds = new ArrayList();
            String[] otherNameApnArray = popupApnConfig.otherApns.split(";");
            int length = otherNameApnArray.length;
            int i = 0;
            int i2 = 0;
            while (i2 < length) {
                String[] nameApn = otherNameApnArray[i2].split(":");
                String name = nameApn[i];
                String apn = nameApn[1];
                int list_size = apns.size();
                for (int i3 = 0; i3 < list_size; i3++) {
                    ApnSetting apnSetting = (ApnSetting) arrayList.get(i3);
                    if (apnSetting.canHandleType("default") && apn != null) {
                        try {
                            if (apn.equals(apnSetting.apn) && name != null && apnSetting.carrier != null && ArrayUtils.equals(name.getBytes("UTF-8"), apnSetting.carrier.getBytes("UTF-8"), name.getBytes("UTF-8").length)) {
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("find match other apn = ");
                                stringBuilder.append(apn);
                                stringBuilder.append(" name = ");
                                stringBuilder.append(name);
                                logd(stringBuilder.toString());
                                popupApnSettings2.otherApnIds.add(Integer.valueOf(apnSetting.id));
                                break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                i2++;
                i = 0;
            }
        }
    }

    public String getVoiceMailNumberByPreferedApn(int apnId, String vmNumber) {
        return queryValueByPreferedApn(apnId, 2, vmNumber);
    }

    public String getVoiceMailTagByPreferedApn(int apnId, String vmTag) {
        return queryValueByPreferedApn(apnId, 3, vmTag);
    }

    private String queryValueByPreferedApn(int apnId, int queryType, String queryValue) {
        if (!isPopupApnSettingsEmpty()) {
            int myApnSettingSize = this.myPopupApnSettings.size();
            int i = 0;
            while (i < myApnSettingSize) {
                StringBuilder stringBuilder;
                if (apnId == ((PopupApnSettings) this.myPopupApnSettings.get(i)).id) {
                    queryValue = getValueByQueryType(queryType, queryValue, (PopupApnSettings) this.myPopupApnSettings.get(i));
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("find matched value by apnId: ");
                    stringBuilder.append(apnId);
                    stringBuilder.append(" queryType: ");
                    stringBuilder.append(queryType);
                    logd(stringBuilder.toString());
                    break;
                }
                if (((PopupApnSettings) this.myPopupApnSettings.get(i)).otherApnIds != null && ((PopupApnSettings) this.myPopupApnSettings.get(i)).otherApnIds.size() > 0) {
                    int myApnSettingOtherApnIdSize = ((PopupApnSettings) this.myPopupApnSettings.get(i)).otherApnIds.size();
                    for (int j = 0; j < myApnSettingOtherApnIdSize; j++) {
                        if (apnId == ((Integer) ((PopupApnSettings) this.myPopupApnSettings.get(i)).otherApnIds.get(j)).intValue()) {
                            queryValue = getValueByQueryType(queryType, queryValue, (PopupApnSettings) this.myPopupApnSettings.get(i));
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("find matched value by other apnId: ");
                            stringBuilder.append(apnId);
                            stringBuilder.append(" queryType: ");
                            stringBuilder.append(queryType);
                            logd(stringBuilder.toString());
                            return queryValue;
                        }
                    }
                    continue;
                }
                i++;
            }
        }
        return queryValue;
    }

    private String getValueByQueryType(int queryType, String queryValue, PopupApnSettings popupApnSettings) {
        switch (queryType) {
            case 1:
                return popupApnSettings.onsName;
            case 2:
                return popupApnSettings.vmNumber;
            case 3:
                return popupApnSettings.vmTag;
            default:
                return queryValue;
        }
    }

    public HwCustApnReminder getCust() {
        return this.mHwCustApnReminder;
    }
}
