package com.android.internal.telephony;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemProperties;
import android.provider.HwTelephony.VirtualNets;
import android.provider.Settings.System;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import com.android.internal.telephony.DctConstants.State;
import com.android.internal.telephony.dataconnection.ApnContext;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.SIMRecords;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.util.XmlUtils;
import huawei.com.android.internal.telephony.RoamingBroker;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class VirtualNet {
    static final String APN_ID = "apn_id";
    private static final String CTRoamingNumeric = "20404";
    private static final boolean DBG = true;
    static final String FILE_FROM_CUST_DIR = "/data/cust/xml/specialImsiList-conf.xml";
    static final String FILE_FROM_SYSTEM_ETC_DIR = "/system/etc/specialImsiList-conf.xml";
    static final int IMSIEND = 1;
    static final int IMSISTART = 0;
    private static final String LOG_TAG = "GSM";
    private static final int MAX_PHONE_COUNT = TelephonyManager.getDefault().getPhoneCount();
    static final String PARAM_SPECIALIMSI_PATH = "etc/specialImsiList-conf.xml";
    static final Uri PREFERAPN_NO_UPDATE_URI = Uri.parse("content://telephony/carriers/preferapn_no_update");
    static final String SPECIAL_IMSI_CONFIG_FILE = "specialImsiList-conf.xml";
    static final int SPECIAL_IMSI_CONFIG_SIZE = 3;
    private static final String SPN_EMPTY = "spn_null";
    private static final String SPN_START = "voda";
    private static final int SUB1 = 0;
    private static final int SUB2 = 1;
    static final int VNKEY = 2;
    private static final boolean isMultiSimEnabled = TelephonyManager.getDefault().isMultiSimEnabled();
    private static ArrayList<String[]> mSpecialImsiList;
    private static UiccCard[] mUiccCards = new UiccCard[MAX_PHONE_COUNT];
    private static VirtualNet mVirtualNet = null;
    private static VirtualNet mVirtualNet1 = null;
    private static Map<SpecialFile, byte[]> specialFilesMap = new HashMap();
    private static Map<SpecialFile, byte[]> specialFilesMap1 = new HashMap();
    private static boolean specialImsiLoaded = false;
    private String apnFilter;
    private String eccNoCard;
    private String eccWithCard;
    public boolean isRealNetwork = false;
    private int maxMessageSize;
    private int numMatch;
    private int numMatchShort;
    private String numeric;
    private String operatorName;
    private int plmnSameImsiStartCount = 0;
    private int sms7BitEnabled;
    private int smsCodingNational;
    private int smsToMmsTextThreshold;
    private String voicemailNumber;
    private String voicemailTag;

    private static class SpecialFile {
        public String fileId;
        public String filePath;

        public SpecialFile(String filePath, String fileId) {
            this.filePath = filePath;
            this.fileId = fileId;
        }

        public boolean equals(Object obj) {
            boolean z = false;
            if (!(obj instanceof SpecialFile)) {
                return false;
            }
            SpecialFile other = (SpecialFile) obj;
            if (this.filePath != null && this.fileId != null && this.filePath.equals(other.filePath) && this.fileId.equals(other.fileId)) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            if (this.filePath == null || this.fileId == null) {
                return 0;
            }
            return (this.filePath.hashCode() * 31) + this.fileId.hashCode();
        }
    }

    public static void addSpecialFile(String filePath, String fileId, byte[] bytes) {
        specialFilesMap.put(new SpecialFile(filePath, fileId), bytes);
    }

    public static void addSpecialFile(String filePath, String fileId, byte[] bytes, int slotId) {
        SpecialFile specialFile = new SpecialFile(filePath, fileId);
        if (slotId == 1) {
            specialFilesMap1.put(specialFile, bytes);
        } else if (slotId == 0) {
            specialFilesMap.put(specialFile, bytes);
        }
    }

    public static VirtualNet getCurrentVirtualNet() {
        if (!isMultiSimEnabled) {
            return mVirtualNet;
        }
        if (hasIccCard(0)) {
            return mVirtualNet;
        }
        if (hasIccCard(1)) {
            return mVirtualNet1;
        }
        return null;
    }

    public static VirtualNet getCurrentVirtualNet(int slotId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCurrentVirtualNet, slotId=");
        stringBuilder.append(slotId);
        logd(stringBuilder.toString());
        if (slotId == 1) {
            return mVirtualNet1;
        }
        if (slotId == 0) {
            return mVirtualNet;
        }
        return null;
    }

    public static boolean isVirtualNet() {
        boolean z = true;
        if (!isMultiSimEnabled) {
            if (mVirtualNet == null) {
                z = false;
            }
            return z;
        } else if (hasIccCard(0)) {
            return isVirtualNet(0);
        } else {
            if (hasIccCard(1)) {
                return isVirtualNet(1);
            }
            return false;
        }
    }

    public static boolean isVirtualNet(int slotId) {
        boolean z = false;
        if (slotId == 1) {
            if (mVirtualNet1 != null) {
                z = true;
            }
            return z;
        } else if (slotId != 0) {
            return false;
        } else {
            if (mVirtualNet != null) {
                z = true;
            }
            return z;
        }
    }

    private static void logd(String text) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[VirtualNet] ");
        stringBuilder.append(text);
        Log.d(str, stringBuilder.toString());
    }

    private static void loge(String text, Exception e) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[VirtualNet] ");
        stringBuilder.append(text);
        stringBuilder.append(e);
        Log.e(str, stringBuilder.toString());
    }

    private static void loge(String text) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[VirtualNet] ");
        stringBuilder.append(text);
        Log.e(str, stringBuilder.toString());
    }

    public static void loadSpecialFiles(String numeric, SIMRecords simRecords) {
        if (numeric == null) {
            loge("number is null,loadSpecialFiles will return");
            return;
        }
        int slotId = simRecords.getSlotId();
        if (isMultiSimEnabled) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("start loadSpecialFiles:slotId= ");
            stringBuilder.append(slotId);
            logd(stringBuilder.toString());
            if (slotId == 1) {
                specialFilesMap1.clear();
            } else {
                specialFilesMap.clear();
            }
        } else {
            specialFilesMap.clear();
        }
        String[] projection = new String[]{"numeric", VirtualNets.VIRTUAL_NET_RULE, VirtualNets.MATCH_PATH, VirtualNets.MATCH_FILE};
        Cursor cursor = PhoneFactory.getDefaultPhone().getContext().getContentResolver().query(VirtualNets.CONTENT_URI, projection, "numeric = ? AND virtual_net_rule = ?", new String[]{numeric, Integer.toString(4)}, null);
        if (cursor == null) {
            loge("query virtual net db got a null cursor");
            return;
        }
        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String matchPath = cursor.getString(cursor.getColumnIndex(VirtualNets.MATCH_PATH));
                String matchFile = cursor.getString(cursor.getColumnIndex(VirtualNets.MATCH_FILE));
                SpecialFile specialFile = new SpecialFile(matchPath, matchFile);
                StringBuilder stringBuilder2;
                if (slotId == 1) {
                    if (!specialFilesMap1.containsKey(specialFile)) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("load specialFilesMap1 matchPath=");
                        stringBuilder2.append(matchPath);
                        stringBuilder2.append(";matchFile=");
                        stringBuilder2.append(matchFile);
                        logd(stringBuilder2.toString());
                        specialFilesMap1.put(specialFile, null);
                        simRecords.loadFile(matchPath, matchFile);
                    }
                } else if (!specialFilesMap.containsKey(specialFile)) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("load specialFile matchPath=");
                    stringBuilder2.append(matchPath);
                    stringBuilder2.append(";matchFile=");
                    stringBuilder2.append(matchFile);
                    logd(stringBuilder2.toString());
                    specialFilesMap.put(specialFile, null);
                    simRecords.loadFile(matchPath, matchFile);
                }
                cursor.moveToNext();
            }
        } catch (Exception e) {
            e.printStackTrace();
            loge("loadVirtualNet got Exception", e);
        } catch (Throwable th) {
            cursor.close();
        }
        cursor.close();
    }

    public static void removeVirtualNet(int slotId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("removeVirtualNet: slotId= ");
        stringBuilder.append(slotId);
        logd(stringBuilder.toString());
        if (isMultiSimEnabled) {
            if (slotId == 1) {
                mVirtualNet1 = null;
            } else {
                mVirtualNet = null;
            }
            if (PhoneFactory.getDefaultPhone() != null && PhoneFactory.getDefaultPhone().getContext() != null) {
                ContentResolver contentResolver = PhoneFactory.getDefaultPhone().getContext().getContentResolver();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(VirtualNets.VN_KEY);
                stringBuilder2.append(slotId);
                System.putString(contentResolver, stringBuilder2.toString(), "");
                contentResolver = PhoneFactory.getDefaultPhone().getContext().getContentResolver();
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(VirtualNets.VN_KEY_FOR_SPECIALIMSI);
                stringBuilder2.append(slotId);
                System.putString(contentResolver, stringBuilder2.toString(), "");
                return;
            }
            return;
        }
        mVirtualNet = null;
        if (PhoneFactory.getDefaultPhone() != null && PhoneFactory.getDefaultPhone().getContext() != null) {
            System.putString(PhoneFactory.getDefaultPhone().getContext().getContentResolver(), "vn_key0", "");
            System.putString(PhoneFactory.getDefaultPhone().getContext().getContentResolver(), "vn_key_for_specialimsi0", "");
        }
    }

    private static String getSimRecordsImsi(SIMRecords simRecords) {
        String imsi = simRecords.getIMSI();
        int slotId = simRecords.getSlotId();
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            if (!HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated(Integer.valueOf(slotId))) {
                return imsi;
            }
            imsi = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerImsi(Integer.valueOf(slotId));
            Log.d(LOG_TAG, "VirtualNet RoamingBrokerActivated, set homenetwork imsi");
            return imsi;
        } else if (!HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated()) {
            return imsi;
        } else {
            imsi = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerImsi();
            Log.d(LOG_TAG, "VirtualNet RoamingBrokerActivated, set homenetwork imsi");
            return imsi;
        }
    }

    private static void clearVirtualNet(int slotId) {
        if (isMultiSimEnabled) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("start loadVirtualNet: slotId= ");
            stringBuilder.append(slotId);
            logd(stringBuilder.toString());
            if (slotId == 1) {
                mVirtualNet1 = null;
                return;
            } else {
                mVirtualNet = null;
                return;
            }
        }
        mVirtualNet = null;
    }

    /* JADX WARNING: Removed duplicated region for block: B:65:0x03a2  */
    /* JADX WARNING: Removed duplicated region for block: B:65:0x03a2  */
    /* JADX WARNING: Missing block: B:52:0x0367, code skipped:
            r9.moveToNext();
     */
    /* JADX WARNING: Missing block: B:53:0x036a, code skipped:
            r4 = r10;
            r5 = r39;
            r11 = r40;
            r1 = r41;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void loadVirtualNet(String numeric, SIMRecords simRecords) {
        Exception e;
        String vn_key;
        Throwable th;
        String str = numeric;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("start loadVirtualNet: numeric= ");
        stringBuilder.append(str);
        logd(stringBuilder.toString());
        int slotId = simRecords.getSlotId();
        int i = 1;
        boolean z = HwTelephonyManagerInner.getDefault().isCTSimCard(slotId) && CTRoamingNumeric.equals(str);
        boolean isCTRoaming = z;
        if (isCTRoaming) {
            logd("CT sim card 20404 is not virtualnet");
            removeVirtualNet(slotId);
            clearCurVirtualNetsDb(slotId);
        } else if (str == null) {
            loge("number is null,loadVirtualNet will return");
        } else {
            clearVirtualNet(slotId);
            stringBuilder = new StringBuilder();
            stringBuilder.append("thread = ");
            stringBuilder.append(Thread.currentThread().getName());
            logd(stringBuilder.toString());
            String imsi = getSimRecordsImsi(simRecords);
            byte[] gid1 = simRecords.getGID1();
            String spn = simRecords.getServiceProviderName();
            stringBuilder = new StringBuilder();
            stringBuilder.append("start loadVirtualNet: numeric=");
            stringBuilder.append(str);
            stringBuilder.append("; gid1=");
            stringBuilder.append(IccUtils.bytesToHexString(gid1));
            stringBuilder.append("; spn=");
            stringBuilder.append(spn);
            logd(stringBuilder.toString());
            String[] selectionArgs = new String[]{str};
            Phone phone = PhoneFactory.getDefaultPhone();
            String[] projection = new String[]{"numeric", VirtualNets.VIRTUAL_NET_RULE, VirtualNets.IMSI_START, VirtualNets.GID1, VirtualNets.GID_MASK, "spn", VirtualNets.MATCH_PATH, VirtualNets.MATCH_FILE, VirtualNets.MATCH_VALUE, VirtualNets.MATCH_MASK, VirtualNets.APN_FILTER, VirtualNets.VOICEMAIL_NUMBER, VirtualNets.VOICEMAIL_TAG, "num_match", "num_match_short", "sms_7bit_enabled", "sms_coding_national", VirtualNets.ONS_NAME, "max_message_size", "sms_to_mms_textthreshold", VirtualNets.ECC_WITH_CARD, VirtualNets.ECC_NO_CARD, VirtualNets.VN_KEY};
            saveLastVirtualNetsDb(slotId);
            clearCurVirtualNetsDb(slotId);
            Cursor cursor = phone.getContext().getContentResolver().query(VirtualNets.CONTENT_URI, projection, "numeric = ?", selectionArgs, null);
            if (cursor == null) {
                loge("query virtual net db got a null cursor");
                return;
            }
            StringBuilder stringBuilder2;
            ContentResolver contentResolver;
            boolean isCTRoaming2;
            String[] projection2;
            try {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    int tmpVirtualNetRule = cursor.getInt(cursor.getColumnIndex(VirtualNets.VIRTUAL_NET_RULE));
                    ContentResolver contentResolver2;
                    StringBuilder stringBuilder3;
                    int i2;
                    String tmpGidMask;
                    StringBuilder stringBuilder4;
                    switch (tmpVirtualNetRule) {
                        case 1:
                            isCTRoaming2 = isCTRoaming;
                            projection2 = projection;
                            str = cursor.getString(cursor.getColumnIndex(VirtualNets.IMSI_START));
                            if (isImsiVirtualNet(imsi, str)) {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("find imsi virtual net imsiStart=");
                                stringBuilder2.append(str);
                                logd(stringBuilder2.toString());
                                createVirtualNet(cursor, slotId);
                                contentResolver2 = phone.getContext().getContentResolver();
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(VirtualNets.VIRTUAL_NET_RULE);
                                stringBuilder3.append(slotId);
                                i2 = 1;
                                System.putInt(contentResolver2, stringBuilder3.toString(), 1);
                                contentResolver2 = phone.getContext().getContentResolver();
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(VirtualNets.IMSI_START);
                                stringBuilder3.append(slotId);
                                System.putString(contentResolver2, stringBuilder3.toString(), str);
                                break;
                            }
                        case 2:
                            isCTRoaming2 = isCTRoaming;
                            projection2 = projection;
                            str = cursor.getString(cursor.getColumnIndex(VirtualNets.GID1));
                            tmpGidMask = cursor.getString(cursor.getColumnIndex(VirtualNets.GID_MASK));
                            if (isGid1VirtualNet(gid1, str, tmpGidMask)) {
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("find gid1 virtual net spn=");
                                stringBuilder3.append(str);
                                logd(stringBuilder3.toString());
                                createVirtualNet(cursor, slotId);
                                ContentResolver contentResolver3 = phone.getContext().getContentResolver();
                                StringBuilder stringBuilder5 = new StringBuilder();
                                stringBuilder5.append(VirtualNets.VIRTUAL_NET_RULE);
                                stringBuilder5.append(slotId);
                                System.putInt(contentResolver3, stringBuilder5.toString(), 2);
                                contentResolver3 = phone.getContext().getContentResolver();
                                stringBuilder5 = new StringBuilder();
                                stringBuilder5.append(VirtualNets.GID1);
                                stringBuilder5.append(slotId);
                                System.putString(contentResolver3, stringBuilder5.toString(), str);
                                contentResolver3 = phone.getContext().getContentResolver();
                                stringBuilder5 = new StringBuilder();
                                stringBuilder5.append(VirtualNets.GID_MASK);
                                stringBuilder5.append(slotId);
                                System.putString(contentResolver3, stringBuilder5.toString(), tmpGidMask);
                            }
                            i2 = 1;
                            break;
                        case 3:
                            isCTRoaming2 = isCTRoaming;
                            projection2 = projection;
                            str = cursor.getString(cursor.getColumnIndex("spn"));
                            if (isSpnVirtualNet(spn, str)) {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("find spn virtual net spn=");
                                stringBuilder2.append(str);
                                logd(stringBuilder2.toString());
                                createVirtualNet(cursor, slotId);
                                contentResolver2 = phone.getContext().getContentResolver();
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(VirtualNets.VIRTUAL_NET_RULE);
                                stringBuilder3.append(slotId);
                                System.putInt(contentResolver2, stringBuilder3.toString(), 3);
                                contentResolver2 = phone.getContext().getContentResolver();
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("spn");
                                stringBuilder3.append(slotId);
                                System.putString(contentResolver2, stringBuilder3.toString(), str);
                            }
                            i2 = 1;
                            break;
                        case 4:
                            String tmpMatchPath = cursor.getString(cursor.getColumnIndex(VirtualNets.MATCH_PATH));
                            String tmpMatchFile = cursor.getString(cursor.getColumnIndex(VirtualNets.MATCH_FILE));
                            String tmpMatchValue = cursor.getString(cursor.getColumnIndex(VirtualNets.MATCH_VALUE));
                            tmpGidMask = cursor.getString(cursor.getColumnIndex(VirtualNets.MATCH_MASK));
                            if (!isSpecialFileVirtualNet(tmpMatchPath, tmpMatchFile, tmpMatchValue, tmpGidMask, slotId)) {
                                isCTRoaming2 = isCTRoaming;
                                projection2 = projection;
                                i2 = 1;
                                break;
                            }
                            stringBuilder4 = new StringBuilder();
                            isCTRoaming2 = isCTRoaming;
                            try {
                                stringBuilder4.append("find special file virtual net matchValue =");
                                stringBuilder4.append(tmpMatchValue);
                                logd(stringBuilder4.toString());
                                createVirtualNet(cursor, slotId);
                                contentResolver = phone.getContext().getContentResolver();
                                stringBuilder3 = new StringBuilder();
                                projection2 = projection;
                                stringBuilder3.append(VirtualNets.VIRTUAL_NET_RULE);
                                stringBuilder3.append(slotId);
                                System.putInt(contentResolver, stringBuilder3.toString(), 4);
                                contentResolver = phone.getContext().getContentResolver();
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(VirtualNets.MATCH_PATH);
                                stringBuilder3.append(slotId);
                                System.putString(contentResolver, stringBuilder3.toString(), tmpMatchPath);
                                contentResolver = phone.getContext().getContentResolver();
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(VirtualNets.MATCH_FILE);
                                stringBuilder3.append(slotId);
                                System.putString(contentResolver, stringBuilder3.toString(), tmpMatchFile);
                                contentResolver = phone.getContext().getContentResolver();
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(VirtualNets.MATCH_VALUE);
                                stringBuilder3.append(slotId);
                                System.putString(contentResolver, stringBuilder3.toString(), tmpMatchValue);
                                contentResolver = phone.getContext().getContentResolver();
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(VirtualNets.MATCH_MASK);
                                stringBuilder3.append(slotId);
                                System.putString(contentResolver, stringBuilder3.toString(), tmpGidMask);
                                i2 = 1;
                                break;
                            } catch (Exception e2) {
                                e = e2;
                                projection2 = projection;
                                try {
                                    e.printStackTrace();
                                    loge("loadVirtualNet got Exception", e);
                                    cursor.close();
                                    deletePreferApnIfNeed(slotId);
                                    loadSpecialImsiList();
                                    vn_key = getVnKeyFromSpecialIMSIList(imsi);
                                    if (!TextUtils.isEmpty(vn_key)) {
                                    }
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                projection2 = projection;
                                cursor.close();
                                throw th;
                            }
                            break;
                        default:
                            i2 = i;
                            isCTRoaming2 = isCTRoaming;
                            projection2 = projection;
                            try {
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("unhandled case: ");
                                stringBuilder4.append(tmpVirtualNetRule);
                                logd(stringBuilder4.toString());
                                break;
                            } catch (Exception e3) {
                                e = e3;
                                break;
                            }
                    }
                }
                projection2 = projection;
            } catch (Exception e4) {
                e = e4;
                isCTRoaming2 = isCTRoaming;
                projection2 = projection;
                e.printStackTrace();
                loge("loadVirtualNet got Exception", e);
                cursor.close();
                deletePreferApnIfNeed(slotId);
                loadSpecialImsiList();
                vn_key = getVnKeyFromSpecialIMSIList(imsi);
                if (TextUtils.isEmpty(vn_key)) {
                }
            } catch (Throwable th4) {
                th = th4;
                isCTRoaming2 = isCTRoaming;
                projection2 = projection;
                cursor.close();
                throw th;
            }
            cursor.close();
            deletePreferApnIfNeed(slotId);
            loadSpecialImsiList();
            vn_key = getVnKeyFromSpecialIMSIList(imsi);
            if (TextUtils.isEmpty(vn_key)) {
                contentResolver = phone.getContext().getContentResolver();
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(VirtualNets.VN_KEY_FOR_SPECIALIMSI);
                stringBuilder2.append(slotId);
                System.putString(contentResolver, stringBuilder2.toString(), vn_key);
            }
        }
    }

    private static void deletePreferApnIfNeed(int slotId) {
        if (isVirtualNet(slotId) && !isVirtualNetEqual(slotId)) {
            logd("find different virtual net,so setPreferredApn: delete");
            Phone phone = PhoneFactory.getPhone(slotId);
            ContentResolver resolver = phone.getContext().getContentResolver();
            if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                resolver.delete(ContentUris.withAppendedId(PREFERAPN_NO_UPDATE_URI, (long) slotId), null, null);
            } else {
                resolver.delete(PREFERAPN_NO_UPDATE_URI, null, null);
            }
            if (phone.mDcTracker != null) {
                ApnContext apnContext = (ApnContext) phone.mDcTracker.mApnContexts.get("default");
                if (apnContext != null) {
                    ApnSetting apn = apnContext.getApnSetting();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("current apnSetting is ");
                    stringBuilder.append(apn);
                    logd(stringBuilder.toString());
                    if (apn != null && apnContext.getState() == State.CONNECTED) {
                        ContentValues values = new ContentValues();
                        values.put(APN_ID, Integer.valueOf(apn.id));
                        logd("insert prefer apn");
                        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                            resolver.insert(ContentUris.withAppendedId(PREFERAPN_NO_UPDATE_URI, (long) slotId), values);
                        } else {
                            resolver.insert(PREFERAPN_NO_UPDATE_URI, values);
                        }
                    }
                }
            }
        }
    }

    private static String getStringFromSettingsEx(ContentResolver resolver, String key, String defaultValue) {
        String value = System.getString(resolver, key);
        return value == null ? defaultValue : value;
    }

    private static int getIntFromSettingsEx(ContentResolver resolver, String key) {
        return System.getInt(resolver, key, -99);
    }

    private static void saveLastVirtualNetsDb(int slotId) {
        int i = slotId;
        Phone phone = PhoneFactory.getDefaultPhone();
        int tmpVirtualNetRule = phone.getContext().getContentResolver();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(VirtualNets.VIRTUAL_NET_RULE);
        stringBuilder.append(i);
        tmpVirtualNetRule = getIntFromSettingsEx(tmpVirtualNetRule, stringBuilder.toString());
        String tmpImsiStart = phone.getContext().getContentResolver();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(VirtualNets.IMSI_START);
        stringBuilder2.append(i);
        tmpImsiStart = getStringFromSettingsEx(tmpImsiStart, stringBuilder2.toString(), "");
        String tmpGid1Value = phone.getContext().getContentResolver();
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append(VirtualNets.GID1);
        stringBuilder3.append(i);
        tmpGid1Value = getStringFromSettingsEx(tmpGid1Value, stringBuilder3.toString(), "");
        String tmpGidMask = phone.getContext().getContentResolver();
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append(VirtualNets.GID_MASK);
        stringBuilder4.append(i);
        tmpGidMask = getStringFromSettingsEx(tmpGidMask, stringBuilder4.toString(), "");
        String tmpSpn = phone.getContext().getContentResolver();
        StringBuilder stringBuilder5 = new StringBuilder();
        stringBuilder5.append("spn");
        stringBuilder5.append(i);
        tmpSpn = getStringFromSettingsEx(tmpSpn, stringBuilder5.toString(), "");
        String tmpMatchPath = phone.getContext().getContentResolver();
        StringBuilder stringBuilder6 = new StringBuilder();
        stringBuilder6.append(VirtualNets.MATCH_PATH);
        stringBuilder6.append(i);
        tmpMatchPath = getStringFromSettingsEx(tmpMatchPath, stringBuilder6.toString(), "");
        String tmpMatchFile = phone.getContext().getContentResolver();
        StringBuilder stringBuilder7 = new StringBuilder();
        stringBuilder7.append(VirtualNets.MATCH_FILE);
        stringBuilder7.append(i);
        tmpMatchFile = getStringFromSettingsEx(tmpMatchFile, stringBuilder7.toString(), "");
        String tmpMatchValue = phone.getContext().getContentResolver();
        StringBuilder stringBuilder8 = new StringBuilder();
        stringBuilder8.append(VirtualNets.MATCH_VALUE);
        stringBuilder8.append(i);
        tmpMatchValue = getStringFromSettingsEx(tmpMatchValue, stringBuilder8.toString(), "");
        String tmpMatchMask = phone.getContext().getContentResolver();
        StringBuilder stringBuilder9 = new StringBuilder();
        stringBuilder9.append(VirtualNets.MATCH_MASK);
        stringBuilder9.append(i);
        tmpMatchMask = getStringFromSettingsEx(tmpMatchMask, stringBuilder9.toString(), "");
        String tmpNumeric = phone.getContext().getContentResolver();
        StringBuilder stringBuilder10 = new StringBuilder();
        stringBuilder10.append("numeric");
        stringBuilder10.append(i);
        tmpNumeric = getStringFromSettingsEx(tmpNumeric, stringBuilder10.toString(), "");
        String tmpApnFilter = phone.getContext().getContentResolver();
        StringBuilder stringBuilder11 = new StringBuilder();
        stringBuilder11.append(VirtualNets.APN_FILTER);
        stringBuilder11.append(i);
        tmpApnFilter = getStringFromSettingsEx(tmpApnFilter, stringBuilder11.toString(), "");
        String tmpVoicemalNumber = phone.getContext().getContentResolver();
        StringBuilder stringBuilder12 = new StringBuilder();
        stringBuilder12.append(VirtualNets.VOICEMAIL_NUMBER);
        stringBuilder12.append(i);
        tmpVoicemalNumber = getStringFromSettingsEx(tmpVoicemalNumber, stringBuilder12.toString(), "");
        ContentResolver contentResolver = phone.getContext().getContentResolver();
        StringBuilder stringBuilder13 = new StringBuilder();
        String tmpVoicemalNumber2 = tmpVoicemalNumber;
        stringBuilder13.append(VirtualNets.VOICEMAIL_TAG);
        stringBuilder13.append(i);
        tmpVoicemalNumber = getStringFromSettingsEx(contentResolver, stringBuilder13.toString(), "");
        contentResolver = phone.getContext().getContentResolver();
        stringBuilder13 = new StringBuilder();
        String tmpVoicemalTag = tmpVoicemalNumber;
        stringBuilder13.append("num_match");
        stringBuilder13.append(i);
        int tmpNumMatch = getIntFromSettingsEx(contentResolver, stringBuilder13.toString());
        contentResolver = phone.getContext().getContentResolver();
        stringBuilder13 = new StringBuilder();
        int tmpNumMatch2 = tmpNumMatch;
        stringBuilder13.append("num_match_short");
        stringBuilder13.append(i);
        tmpNumMatch = getIntFromSettingsEx(contentResolver, stringBuilder13.toString());
        contentResolver = phone.getContext().getContentResolver();
        stringBuilder13 = new StringBuilder();
        int tmpNumMatchShort = tmpNumMatch;
        stringBuilder13.append("sms_7bit_enabled");
        stringBuilder13.append(i);
        tmpNumMatch = getIntFromSettingsEx(contentResolver, stringBuilder13.toString());
        contentResolver = phone.getContext().getContentResolver();
        stringBuilder13 = new StringBuilder();
        int tmpSms7BitEnabled = tmpNumMatch;
        stringBuilder13.append("sms_coding_national");
        stringBuilder13.append(i);
        tmpNumMatch = getIntFromSettingsEx(contentResolver, stringBuilder13.toString());
        contentResolver = phone.getContext().getContentResolver();
        stringBuilder13 = new StringBuilder();
        int tmpSmsCodingNational = tmpNumMatch;
        stringBuilder13.append(VirtualNets.ONS_NAME);
        stringBuilder13.append(i);
        tmpVoicemalNumber = getStringFromSettingsEx(contentResolver, stringBuilder13.toString(), "");
        contentResolver = phone.getContext().getContentResolver();
        stringBuilder13 = new StringBuilder();
        String tmpOperatorName = tmpVoicemalNumber;
        stringBuilder13.append(VirtualNets.SAVED_VIRTUAL_NET_RULE);
        stringBuilder13.append(i);
        System.putInt(contentResolver, stringBuilder13.toString(), tmpVirtualNetRule);
        ContentResolver contentResolver2 = phone.getContext().getContentResolver();
        stringBuilder12 = new StringBuilder();
        stringBuilder12.append(VirtualNets.SAVED_IMSI_START);
        stringBuilder12.append(i);
        System.putString(contentResolver2, stringBuilder12.toString(), tmpImsiStart);
        contentResolver2 = phone.getContext().getContentResolver();
        stringBuilder12 = new StringBuilder();
        stringBuilder12.append(VirtualNets.SAVED_GID1);
        stringBuilder12.append(i);
        System.putString(contentResolver2, stringBuilder12.toString(), tmpGid1Value);
        contentResolver2 = phone.getContext().getContentResolver();
        stringBuilder12 = new StringBuilder();
        stringBuilder12.append(VirtualNets.SAVED_GID_MASK);
        stringBuilder12.append(i);
        System.putString(contentResolver2, stringBuilder12.toString(), tmpGidMask);
        contentResolver2 = phone.getContext().getContentResolver();
        stringBuilder12 = new StringBuilder();
        stringBuilder12.append(VirtualNets.SAVED_SPN);
        stringBuilder12.append(i);
        System.putString(contentResolver2, stringBuilder12.toString(), tmpSpn);
        contentResolver2 = phone.getContext().getContentResolver();
        stringBuilder12 = new StringBuilder();
        stringBuilder12.append(VirtualNets.SAVED_MATCH_PATH);
        stringBuilder12.append(i);
        System.putString(contentResolver2, stringBuilder12.toString(), tmpMatchPath);
        contentResolver2 = phone.getContext().getContentResolver();
        stringBuilder12 = new StringBuilder();
        stringBuilder12.append(VirtualNets.SAVED_MATCH_FILE);
        stringBuilder12.append(i);
        System.putString(contentResolver2, stringBuilder12.toString(), tmpMatchFile);
        contentResolver2 = phone.getContext().getContentResolver();
        stringBuilder12 = new StringBuilder();
        stringBuilder12.append(VirtualNets.SAVED_MATCH_VALUE);
        stringBuilder12.append(i);
        System.putString(contentResolver2, stringBuilder12.toString(), tmpMatchValue);
        contentResolver2 = phone.getContext().getContentResolver();
        stringBuilder12 = new StringBuilder();
        stringBuilder12.append(VirtualNets.SAVED_MATCH_MASK);
        stringBuilder12.append(i);
        System.putString(contentResolver2, stringBuilder12.toString(), tmpMatchMask);
        contentResolver2 = phone.getContext().getContentResolver();
        stringBuilder12 = new StringBuilder();
        stringBuilder12.append(VirtualNets.SAVED_NUMERIC);
        stringBuilder12.append(i);
        System.putString(contentResolver2, stringBuilder12.toString(), tmpNumeric);
        contentResolver2 = phone.getContext().getContentResolver();
        stringBuilder12 = new StringBuilder();
        stringBuilder12.append(VirtualNets.SAVED_APN_FILTER);
        stringBuilder12.append(i);
        System.putString(contentResolver2, stringBuilder12.toString(), tmpApnFilter);
        contentResolver2 = phone.getContext().getContentResolver();
        stringBuilder12 = new StringBuilder();
        stringBuilder12.append(VirtualNets.SAVED_VOICEMAIL_NUMBER);
        stringBuilder12.append(i);
        System.putString(contentResolver2, stringBuilder12.toString(), tmpVoicemalNumber2);
        contentResolver2 = phone.getContext().getContentResolver();
        stringBuilder12 = new StringBuilder();
        stringBuilder12.append(VirtualNets.SAVED_VOICEMAIL_TAG);
        stringBuilder12.append(i);
        System.putString(contentResolver2, stringBuilder12.toString(), tmpVoicemalTag);
        ContentResolver contentResolver3 = phone.getContext().getContentResolver();
        stringBuilder11 = new StringBuilder();
        stringBuilder11.append(VirtualNets.SAVED_NUM_MATCH);
        stringBuilder11.append(i);
        System.putInt(contentResolver3, stringBuilder11.toString(), tmpNumMatch2);
        contentResolver3 = phone.getContext().getContentResolver();
        stringBuilder = new StringBuilder();
        stringBuilder.append(VirtualNets.SAVED_NUM_MATCH_SHORT);
        stringBuilder.append(i);
        int tmpNumMatchShort2 = tmpNumMatchShort;
        System.putInt(contentResolver3, stringBuilder.toString(), tmpNumMatchShort2);
        contentResolver3 = phone.getContext().getContentResolver();
        stringBuilder = new StringBuilder();
        stringBuilder.append(VirtualNets.SAVED_SMS_7BIT_ENABLED);
        stringBuilder.append(i);
        tmpNumMatchShort2 = tmpSms7BitEnabled;
        System.putInt(contentResolver3, stringBuilder.toString(), tmpNumMatchShort2);
        contentResolver3 = phone.getContext().getContentResolver();
        stringBuilder = new StringBuilder();
        stringBuilder.append(VirtualNets.SAVED_SMS_CODING_NATIONAL);
        stringBuilder.append(i);
        System.putInt(contentResolver3, stringBuilder.toString(), tmpSmsCodingNational);
        contentResolver3 = phone.getContext().getContentResolver();
        stringBuilder = new StringBuilder();
        stringBuilder.append(VirtualNets.SAVED_ONS_NAME);
        stringBuilder.append(i);
        System.putString(contentResolver3, stringBuilder.toString(), tmpOperatorName);
    }

    private static void clearCurVirtualNetsDb(int slotId) {
        Phone phone = PhoneFactory.getDefaultPhone();
        ContentResolver contentResolver = phone.getContext().getContentResolver();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(VirtualNets.VIRTUAL_NET_RULE);
        stringBuilder.append(slotId);
        System.putInt(contentResolver, stringBuilder.toString(), -99);
        contentResolver = phone.getContext().getContentResolver();
        stringBuilder = new StringBuilder();
        stringBuilder.append(VirtualNets.IMSI_START);
        stringBuilder.append(slotId);
        System.putString(contentResolver, stringBuilder.toString(), "");
        contentResolver = phone.getContext().getContentResolver();
        stringBuilder = new StringBuilder();
        stringBuilder.append(VirtualNets.GID1);
        stringBuilder.append(slotId);
        System.putString(contentResolver, stringBuilder.toString(), "");
        contentResolver = phone.getContext().getContentResolver();
        stringBuilder = new StringBuilder();
        stringBuilder.append(VirtualNets.GID_MASK);
        stringBuilder.append(slotId);
        System.putString(contentResolver, stringBuilder.toString(), "");
        contentResolver = phone.getContext().getContentResolver();
        stringBuilder = new StringBuilder();
        stringBuilder.append("spn");
        stringBuilder.append(slotId);
        System.putString(contentResolver, stringBuilder.toString(), "");
        contentResolver = phone.getContext().getContentResolver();
        stringBuilder = new StringBuilder();
        stringBuilder.append(VirtualNets.MATCH_PATH);
        stringBuilder.append(slotId);
        System.putString(contentResolver, stringBuilder.toString(), "");
        contentResolver = phone.getContext().getContentResolver();
        stringBuilder = new StringBuilder();
        stringBuilder.append(VirtualNets.MATCH_FILE);
        stringBuilder.append(slotId);
        System.putString(contentResolver, stringBuilder.toString(), "");
        contentResolver = phone.getContext().getContentResolver();
        stringBuilder = new StringBuilder();
        stringBuilder.append(VirtualNets.MATCH_VALUE);
        stringBuilder.append(slotId);
        System.putString(contentResolver, stringBuilder.toString(), "");
        contentResolver = phone.getContext().getContentResolver();
        stringBuilder = new StringBuilder();
        stringBuilder.append(VirtualNets.MATCH_MASK);
        stringBuilder.append(slotId);
        System.putString(contentResolver, stringBuilder.toString(), "");
        contentResolver = phone.getContext().getContentResolver();
        stringBuilder = new StringBuilder();
        stringBuilder.append("numeric");
        stringBuilder.append(slotId);
        System.putString(contentResolver, stringBuilder.toString(), "");
        contentResolver = phone.getContext().getContentResolver();
        stringBuilder = new StringBuilder();
        stringBuilder.append(VirtualNets.APN_FILTER);
        stringBuilder.append(slotId);
        System.putString(contentResolver, stringBuilder.toString(), "");
        contentResolver = phone.getContext().getContentResolver();
        stringBuilder = new StringBuilder();
        stringBuilder.append(VirtualNets.VOICEMAIL_NUMBER);
        stringBuilder.append(slotId);
        System.putString(contentResolver, stringBuilder.toString(), "");
        contentResolver = phone.getContext().getContentResolver();
        stringBuilder = new StringBuilder();
        stringBuilder.append(VirtualNets.VOICEMAIL_TAG);
        stringBuilder.append(slotId);
        System.putString(contentResolver, stringBuilder.toString(), "");
        contentResolver = phone.getContext().getContentResolver();
        stringBuilder = new StringBuilder();
        stringBuilder.append("num_match");
        stringBuilder.append(slotId);
        System.putInt(contentResolver, stringBuilder.toString(), -99);
        contentResolver = phone.getContext().getContentResolver();
        stringBuilder = new StringBuilder();
        stringBuilder.append("num_match_short");
        stringBuilder.append(slotId);
        System.putInt(contentResolver, stringBuilder.toString(), -99);
        contentResolver = phone.getContext().getContentResolver();
        stringBuilder = new StringBuilder();
        stringBuilder.append("sms_7bit_enabled");
        stringBuilder.append(slotId);
        System.putInt(contentResolver, stringBuilder.toString(), -99);
        contentResolver = phone.getContext().getContentResolver();
        stringBuilder = new StringBuilder();
        stringBuilder.append("sms_coding_national");
        stringBuilder.append(slotId);
        System.putInt(contentResolver, stringBuilder.toString(), -99);
        contentResolver = phone.getContext().getContentResolver();
        stringBuilder = new StringBuilder();
        stringBuilder.append(VirtualNets.ONS_NAME);
        stringBuilder.append(slotId);
        System.putString(contentResolver, stringBuilder.toString(), "");
        contentResolver = phone.getContext().getContentResolver();
        stringBuilder = new StringBuilder();
        stringBuilder.append(VirtualNets.VN_KEY);
        stringBuilder.append(slotId);
        System.putString(contentResolver, stringBuilder.toString(), "");
    }

    /* JADX WARNING: Removed duplicated region for block: B:76:0x06ab  */
    /* JADX WARNING: Removed duplicated region for block: B:75:0x06a8  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean isVirtualNetEqual(int slotId) {
        int i = slotId;
        Phone phone = PhoneFactory.getDefaultPhone();
        if (isVirtualNet(slotId)) {
            int curVirtualNetRule = phone.getContext().getContentResolver();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(VirtualNets.VIRTUAL_NET_RULE);
            stringBuilder.append(i);
            curVirtualNetRule = getIntFromSettingsEx(curVirtualNetRule, stringBuilder.toString());
            String curImsiStart = phone.getContext().getContentResolver();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(VirtualNets.IMSI_START);
            stringBuilder2.append(i);
            curImsiStart = getStringFromSettingsEx(curImsiStart, stringBuilder2.toString(), "");
            String curGid1Value = phone.getContext().getContentResolver();
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(VirtualNets.GID1);
            stringBuilder3.append(i);
            curGid1Value = getStringFromSettingsEx(curGid1Value, stringBuilder3.toString(), "");
            String curGidMask = phone.getContext().getContentResolver();
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append(VirtualNets.GID_MASK);
            stringBuilder4.append(i);
            curGidMask = getStringFromSettingsEx(curGidMask, stringBuilder4.toString(), "");
            String curSpn = phone.getContext().getContentResolver();
            StringBuilder stringBuilder5 = new StringBuilder();
            stringBuilder5.append("spn");
            stringBuilder5.append(i);
            curSpn = getStringFromSettingsEx(curSpn, stringBuilder5.toString(), "");
            String curMatchPath = phone.getContext().getContentResolver();
            StringBuilder stringBuilder6 = new StringBuilder();
            stringBuilder6.append(VirtualNets.MATCH_PATH);
            stringBuilder6.append(i);
            curMatchPath = getStringFromSettingsEx(curMatchPath, stringBuilder6.toString(), "");
            String curMatchFile = phone.getContext().getContentResolver();
            StringBuilder stringBuilder7 = new StringBuilder();
            stringBuilder7.append(VirtualNets.MATCH_FILE);
            stringBuilder7.append(i);
            curMatchFile = getStringFromSettingsEx(curMatchFile, stringBuilder7.toString(), "");
            String curMatchValue = phone.getContext().getContentResolver();
            StringBuilder stringBuilder8 = new StringBuilder();
            stringBuilder8.append(VirtualNets.MATCH_VALUE);
            stringBuilder8.append(i);
            curMatchValue = getStringFromSettingsEx(curMatchValue, stringBuilder8.toString(), "");
            String curMatchMask = phone.getContext().getContentResolver();
            StringBuilder stringBuilder9 = new StringBuilder();
            stringBuilder9.append(VirtualNets.MATCH_MASK);
            stringBuilder9.append(i);
            curMatchMask = getStringFromSettingsEx(curMatchMask, stringBuilder9.toString(), "");
            String curNumeric = phone.getContext().getContentResolver();
            StringBuilder stringBuilder10 = new StringBuilder();
            stringBuilder10.append("numeric");
            stringBuilder10.append(i);
            curNumeric = getStringFromSettingsEx(curNumeric, stringBuilder10.toString(), "");
            String curApnFilter = phone.getContext().getContentResolver();
            StringBuilder stringBuilder11 = new StringBuilder();
            stringBuilder11.append(VirtualNets.APN_FILTER);
            stringBuilder11.append(i);
            curApnFilter = getStringFromSettingsEx(curApnFilter, stringBuilder11.toString(), "");
            ContentResolver contentResolver = phone.getContext().getContentResolver();
            StringBuilder stringBuilder12 = new StringBuilder();
            stringBuilder12.append(VirtualNets.VOICEMAIL_NUMBER);
            stringBuilder12.append(i);
            String curVoicemalNumber = getStringFromSettingsEx(contentResolver, stringBuilder12.toString(), "");
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder12 = new StringBuilder();
            String curVoicemalNumber2 = curVoicemalNumber;
            stringBuilder12.append(VirtualNets.VOICEMAIL_TAG);
            stringBuilder12.append(i);
            curVoicemalNumber = getStringFromSettingsEx(contentResolver, stringBuilder12.toString(), "");
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder12 = new StringBuilder();
            String curVoicemalTag = curVoicemalNumber;
            stringBuilder12.append("num_match");
            stringBuilder12.append(i);
            int curNumMatch = getIntFromSettingsEx(contentResolver, stringBuilder12.toString());
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder12 = new StringBuilder();
            int curNumMatch2 = curNumMatch;
            stringBuilder12.append("num_match_short");
            stringBuilder12.append(i);
            curNumMatch = getIntFromSettingsEx(contentResolver, stringBuilder12.toString());
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder12 = new StringBuilder();
            int curNumMatchShort = curNumMatch;
            stringBuilder12.append("sms_7bit_enabled");
            stringBuilder12.append(i);
            curNumMatch = getIntFromSettingsEx(contentResolver, stringBuilder12.toString());
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder12 = new StringBuilder();
            int curSms7BitEnabled = curNumMatch;
            stringBuilder12.append("sms_coding_national");
            stringBuilder12.append(i);
            curNumMatch = getIntFromSettingsEx(contentResolver, stringBuilder12.toString());
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder12 = new StringBuilder();
            int curSmsCodingNational = curNumMatch;
            stringBuilder12.append(VirtualNets.ONS_NAME);
            stringBuilder12.append(i);
            curVoicemalNumber = getStringFromSettingsEx(contentResolver, stringBuilder12.toString(), "");
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder12 = new StringBuilder();
            String curOperatorName = curVoicemalNumber;
            stringBuilder12.append(VirtualNets.SAVED_VIRTUAL_NET_RULE);
            stringBuilder12.append(i);
            curNumMatch = getIntFromSettingsEx(contentResolver, stringBuilder12.toString());
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder12 = new StringBuilder();
            String curApnFilter2 = curApnFilter;
            stringBuilder12.append(VirtualNets.SAVED_IMSI_START);
            stringBuilder12.append(i);
            curApnFilter = getStringFromSettingsEx(contentResolver, stringBuilder12.toString(), "");
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder12 = new StringBuilder();
            String curNumeric2 = curNumeric;
            stringBuilder12.append(VirtualNets.SAVED_GID1);
            stringBuilder12.append(i);
            curNumeric = getStringFromSettingsEx(contentResolver, stringBuilder12.toString(), "");
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder12 = new StringBuilder();
            String curImsiStart2 = curImsiStart;
            stringBuilder12.append(VirtualNets.SAVED_GID_MASK);
            stringBuilder12.append(i);
            curImsiStart = getStringFromSettingsEx(contentResolver, stringBuilder12.toString(), "");
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder12 = new StringBuilder();
            String lastImsiStart = curApnFilter;
            stringBuilder12.append(VirtualNets.SAVED_SPN);
            stringBuilder12.append(i);
            curApnFilter = getStringFromSettingsEx(contentResolver, stringBuilder12.toString(), "");
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder12 = new StringBuilder();
            String lastGidMask = curImsiStart;
            stringBuilder12.append(VirtualNets.SAVED_MATCH_PATH);
            stringBuilder12.append(i);
            curImsiStart = getStringFromSettingsEx(contentResolver, stringBuilder12.toString(), "");
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder12 = new StringBuilder();
            String curGidMask2 = curGidMask;
            stringBuilder12.append(VirtualNets.SAVED_MATCH_FILE);
            stringBuilder12.append(i);
            curGidMask = getStringFromSettingsEx(contentResolver, stringBuilder12.toString(), "");
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder12 = new StringBuilder();
            String curGid1Value2 = curGid1Value;
            stringBuilder12.append(VirtualNets.SAVED_MATCH_VALUE);
            stringBuilder12.append(i);
            curGid1Value = getStringFromSettingsEx(contentResolver, stringBuilder12.toString(), "");
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder12 = new StringBuilder();
            String lastGid1Value = curNumeric;
            stringBuilder12.append(VirtualNets.SAVED_MATCH_MASK);
            stringBuilder12.append(i);
            curNumeric = getStringFromSettingsEx(contentResolver, stringBuilder12.toString(), "");
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder12 = new StringBuilder();
            String curSpn2 = curSpn;
            stringBuilder12.append(VirtualNets.SAVED_NUMERIC);
            stringBuilder12.append(i);
            curSpn = getStringFromSettingsEx(contentResolver, stringBuilder12.toString(), "");
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder12 = new StringBuilder();
            String lastNumeric = curSpn;
            stringBuilder12.append(VirtualNets.SAVED_APN_FILTER);
            stringBuilder12.append(i);
            curSpn = getStringFromSettingsEx(contentResolver, stringBuilder12.toString(), "");
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder12 = new StringBuilder();
            String lastApnFilter = curSpn;
            stringBuilder12.append(VirtualNets.SAVED_VOICEMAIL_NUMBER);
            stringBuilder12.append(i);
            curSpn = getStringFromSettingsEx(contentResolver, stringBuilder12.toString(), "");
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder12 = new StringBuilder();
            String lastVoicemalNumber = curSpn;
            stringBuilder12.append(VirtualNets.SAVED_VOICEMAIL_TAG);
            stringBuilder12.append(i);
            curSpn = getStringFromSettingsEx(contentResolver, stringBuilder12.toString(), "");
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder12 = new StringBuilder();
            String lastVoicemalTag = curSpn;
            stringBuilder12.append(VirtualNets.SAVED_NUM_MATCH);
            stringBuilder12.append(i);
            int lastNumMatch = getIntFromSettingsEx(contentResolver, stringBuilder12.toString());
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder12 = new StringBuilder();
            int lastNumMatch2 = lastNumMatch;
            stringBuilder12.append(VirtualNets.SAVED_NUM_MATCH_SHORT);
            stringBuilder12.append(i);
            lastNumMatch = getIntFromSettingsEx(contentResolver, stringBuilder12.toString());
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder12 = new StringBuilder();
            int lastNumMatchShort = lastNumMatch;
            stringBuilder12.append(VirtualNets.SAVED_SMS_7BIT_ENABLED);
            stringBuilder12.append(i);
            lastNumMatch = getIntFromSettingsEx(contentResolver, stringBuilder12.toString());
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder12 = new StringBuilder();
            int lastSms7BitEnabled = lastNumMatch;
            stringBuilder12.append(VirtualNets.SAVED_SMS_CODING_NATIONAL);
            stringBuilder12.append(i);
            lastNumMatch = getIntFromSettingsEx(contentResolver, stringBuilder12.toString());
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder12 = new StringBuilder();
            stringBuilder12.append(VirtualNets.SAVED_ONS_NAME);
            stringBuilder12.append(i);
            phone = getStringFromSettingsEx(contentResolver, stringBuilder12.toString(), "");
            if (curVirtualNetRule == 0) {
                logd("RULE_NONE different virtual net");
                return false;
            } else if (curVirtualNetRule != curNumMatch) {
                logd("curVirtualNetRule != lastVirtualNetRule different virtual net");
                return false;
            } else {
                String curImsiStart3;
                String lastImsiStart2;
                Phone lastOperatorName;
                boolean z;
                boolean anyValueNotMatch;
                String str;
                StringBuilder stringBuilder13;
                String str2;
                String str3;
                String str4;
                String str5;
                String str6;
                String str7;
                int i2;
                switch (curVirtualNetRule) {
                    case 1:
                        str2 = curGidMask2;
                        str3 = curGid1Value2;
                        str4 = lastGid1Value;
                        str5 = curSpn2;
                        str6 = lastGidMask;
                        curImsiStart3 = curImsiStart2;
                        lastImsiStart2 = lastImsiStart;
                        if (curImsiStart3.equals(lastImsiStart2)) {
                            break;
                        }
                        str7 = curImsiStart3;
                        logd("RULE_IMSI different virtual net");
                        return false;
                    case 2:
                        curImsiStart3 = lastGid1Value;
                        if (curGid1Value2.equals(curImsiStart3) != null) {
                            str4 = curImsiStart3;
                            i2 = curVirtualNetRule;
                            curVirtualNetRule = lastGidMask;
                            curImsiStart3 = curGidMask2;
                            if (curImsiStart3.equals(curVirtualNetRule) != null) {
                                str2 = curImsiStart3;
                                Object obj = curVirtualNetRule;
                                str7 = curImsiStart2;
                                lastImsiStart2 = lastImsiStart;
                                break;
                            }
                        }
                        i2 = curVirtualNetRule;
                        curVirtualNetRule = lastGidMask;
                        curImsiStart3 = curGidMask2;
                        str2 = curImsiStart3;
                        logd("RULE_GID1 different virtual net");
                        return false;
                    case 3:
                        if (curSpn2.equals(curApnFilter) != null) {
                            i2 = curVirtualNetRule;
                            str7 = curImsiStart2;
                            lastImsiStart2 = lastImsiStart;
                            str6 = lastGidMask;
                            str2 = curGidMask2;
                            str3 = curGid1Value2;
                            str4 = lastGid1Value;
                            break;
                        }
                        logd("RULE_SPN different virtual net");
                        return false;
                    case 4:
                        boolean fileNotMatch = (curMatchPath.equals(curImsiStart) && curMatchFile.equals(curGidMask) && curMatchValue.equals(curGid1Value) && curMatchMask.equals(curNumeric)) ? false : true;
                        if (!fileNotMatch) {
                            i2 = curVirtualNetRule;
                            str7 = curImsiStart2;
                            lastImsiStart2 = lastImsiStart;
                            str6 = lastGidMask;
                            str2 = curGidMask2;
                            str3 = curGid1Value2;
                            str4 = lastGid1Value;
                            str5 = curSpn2;
                            break;
                        }
                        logd("RULE_MATCH_FILE different virtual net");
                        return false;
                        break;
                    default:
                        lastOperatorName = phone;
                        curVirtualNetRule = lastSms7BitEnabled;
                        logd("RULE unkown different virtual net");
                        return false;
                }
                String str8 = lastImsiStart2;
                curImsiStart3 = curNumeric2;
                lastImsiStart2 = lastNumeric;
                String str9;
                String str10;
                String str11;
                String str12;
                String str13;
                String str14;
                String str15;
                int i3;
                int i4;
                int curSmsCodingNational2;
                int curSms7BitEnabled2;
                int curNumMatchShort2;
                int curNumMatch3;
                if (curImsiStart3.equals(lastImsiStart2) != null) {
                    String str16 = curImsiStart3;
                    str9 = lastImsiStart2;
                    curImsiStart3 = curApnFilter2;
                    lastImsiStart2 = lastApnFilter;
                    if (curImsiStart3.equals(lastImsiStart2) != null) {
                        str10 = curImsiStart3;
                        str11 = lastImsiStart2;
                        curImsiStart3 = curVoicemalNumber2;
                        lastImsiStart2 = lastVoicemalNumber;
                        if (curImsiStart3.equals(lastImsiStart2) != null) {
                            str12 = curImsiStart3;
                            str13 = lastImsiStart2;
                            curImsiStart3 = curVoicemalTag;
                            lastImsiStart2 = lastVoicemalTag;
                            if (curImsiStart3.equals(lastImsiStart2)) {
                                str14 = curImsiStart3;
                                str15 = lastImsiStart2;
                                curImsiStart3 = curNumMatch2;
                                curVirtualNetRule = lastNumMatch2;
                                if (curImsiStart3 == curVirtualNetRule) {
                                    String str17 = curImsiStart3;
                                    i3 = curVirtualNetRule;
                                    curImsiStart3 = curNumMatchShort;
                                    curVirtualNetRule = lastNumMatchShort;
                                    if (curImsiStart3 == curVirtualNetRule) {
                                        String str18 = curImsiStart3;
                                        i4 = curVirtualNetRule;
                                        curImsiStart3 = curSms7BitEnabled;
                                        if (curImsiStart3 == lastSms7BitEnabled) {
                                            String str19 = curImsiStart3;
                                            curImsiStart3 = curSmsCodingNational;
                                            if (curImsiStart3 == lastNumMatch) {
                                                String str20 = curImsiStart3;
                                                curImsiStart3 = curOperatorName;
                                                if (curImsiStart3.equals(phone)) {
                                                    z = false;
                                                    anyValueNotMatch = z;
                                                    str = curImsiStart3;
                                                    stringBuilder13 = new StringBuilder();
                                                    lastOperatorName = phone;
                                                    stringBuilder13.append("anyValueNotMatch = ");
                                                    phone = anyValueNotMatch;
                                                    stringBuilder13.append(phone);
                                                    logd(stringBuilder13.toString());
                                                    return phone != null;
                                                }
                                            }
                                            curSmsCodingNational2 = curImsiStart3;
                                            curImsiStart3 = curOperatorName;
                                        } else {
                                            curSms7BitEnabled2 = curImsiStart3;
                                            curSmsCodingNational2 = curSmsCodingNational;
                                            curImsiStart3 = curOperatorName;
                                        }
                                    } else {
                                        curNumMatchShort2 = curImsiStart3;
                                        i4 = curVirtualNetRule;
                                        curSms7BitEnabled2 = curSms7BitEnabled;
                                        curSmsCodingNational2 = curSmsCodingNational;
                                        curImsiStart3 = curOperatorName;
                                        curVirtualNetRule = lastSms7BitEnabled;
                                    }
                                } else {
                                    curNumMatch3 = curImsiStart3;
                                    i3 = curVirtualNetRule;
                                    curNumMatchShort2 = curNumMatchShort;
                                    curSms7BitEnabled2 = curSms7BitEnabled;
                                    curSmsCodingNational2 = curSmsCodingNational;
                                    curImsiStart3 = curOperatorName;
                                    i4 = lastNumMatchShort;
                                    lastImsiStart2 = lastSms7BitEnabled;
                                }
                            } else {
                                str14 = curImsiStart3;
                                str15 = lastImsiStart2;
                                curNumMatch3 = curNumMatch2;
                                curNumMatchShort2 = curNumMatchShort;
                                curSms7BitEnabled2 = curSms7BitEnabled;
                                curSmsCodingNational2 = curSmsCodingNational;
                                curImsiStart3 = curOperatorName;
                                i3 = lastNumMatch2;
                                i4 = lastNumMatchShort;
                                lastImsiStart2 = lastSms7BitEnabled;
                            }
                        } else {
                            str12 = curImsiStart3;
                            str13 = lastImsiStart2;
                            str14 = curVoicemalTag;
                            curNumMatch3 = curNumMatch2;
                            curNumMatchShort2 = curNumMatchShort;
                            curSms7BitEnabled2 = curSms7BitEnabled;
                            curSmsCodingNational2 = curSmsCodingNational;
                            curImsiStart3 = curOperatorName;
                            str15 = lastVoicemalTag;
                            i3 = lastNumMatch2;
                            i4 = lastNumMatchShort;
                            lastImsiStart2 = lastSms7BitEnabled;
                        }
                    } else {
                        str10 = curImsiStart3;
                        str11 = lastImsiStart2;
                        str12 = curVoicemalNumber2;
                        str14 = curVoicemalTag;
                        curNumMatch3 = curNumMatch2;
                        curNumMatchShort2 = curNumMatchShort;
                        curSms7BitEnabled2 = curSms7BitEnabled;
                        curSmsCodingNational2 = curSmsCodingNational;
                        curImsiStart3 = curOperatorName;
                        str13 = lastVoicemalNumber;
                        str15 = lastVoicemalTag;
                        i3 = lastNumMatch2;
                        i4 = lastNumMatchShort;
                        lastImsiStart2 = lastSms7BitEnabled;
                    }
                } else {
                    str9 = lastImsiStart2;
                    str12 = curVoicemalNumber2;
                    str14 = curVoicemalTag;
                    curNumMatch3 = curNumMatch2;
                    curNumMatchShort2 = curNumMatchShort;
                    curSms7BitEnabled2 = curSms7BitEnabled;
                    curSmsCodingNational2 = curSmsCodingNational;
                    curImsiStart3 = curOperatorName;
                    str10 = curApnFilter2;
                    str11 = lastApnFilter;
                    str13 = lastVoicemalNumber;
                    str15 = lastVoicemalTag;
                    i3 = lastNumMatch2;
                    i4 = lastNumMatchShort;
                }
                z = true;
                anyValueNotMatch = z;
                str = curImsiStart3;
                stringBuilder13 = new StringBuilder();
                lastOperatorName = phone;
                stringBuilder13.append("anyValueNotMatch = ");
                phone = anyValueNotMatch;
                stringBuilder13.append(phone);
                logd(stringBuilder13.toString());
                if (phone != null) {
                }
                return phone != null;
            }
        }
        return false;
    }

    protected static boolean isSpecialFileVirtualNet(String matchPath, String matchFile, String matchValue, String matchMask, int slotId) {
        byte[] bytes;
        SpecialFile specialFile = new SpecialFile(matchPath, matchFile);
        if (!isMultiSimEnabled) {
            bytes = (byte[]) specialFilesMap.get(specialFile);
        } else if (slotId == 1) {
            logd("isSpecialFileVirtualNet: slotId == SUB2");
            bytes = (byte[]) specialFilesMap1.get(specialFile);
        } else {
            bytes = (byte[]) specialFilesMap.get(specialFile);
        }
        if (bytes == null) {
            return false;
        }
        return matchByteWithMask(bytes, matchValue, matchMask);
    }

    protected static boolean isSpnVirtualNet(String spn1, String spn2) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isSpnVirtualNet spn1 = ");
        stringBuilder.append(spn1);
        stringBuilder.append("; spn2 = ");
        stringBuilder.append(spn2);
        logd(stringBuilder.toString());
        boolean z = true;
        if (TextUtils.isEmpty(spn1) && spn2 != null && spn2.equals(SPN_EMPTY)) {
            return true;
        }
        if (!TextUtils.isEmpty(spn1) && spn1.toUpperCase().startsWith(SPN_START.toUpperCase()) && !TextUtils.isEmpty(spn2) && spn2.equals(SPN_EMPTY)) {
            return true;
        }
        if (spn1 == null || spn2 == null || !spn1.equals(spn2)) {
            z = false;
        }
        return z;
    }

    protected static boolean isGid1VirtualNet(byte[] gid1, String gid1Value, String gidMask) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isGid1VirtualNet gid1 = ");
        stringBuilder.append(IccUtils.bytesToHexString(gid1));
        stringBuilder.append("; gid1Value = ");
        stringBuilder.append(gid1Value);
        stringBuilder.append("; gidMask = ");
        stringBuilder.append(gidMask);
        logd(stringBuilder.toString());
        return matchByteWithMask(gid1, gid1Value, gidMask);
    }

    private static boolean matchByteWithMask(byte[] data, String value, String mask) {
        boolean isValidOddMask = true;
        int i = 0;
        boolean inValidParams = data == null || value == null || mask == null || data.length * 2 < value.length() - 2 || value.length() < 2 || !value.substring(0, 2).equalsIgnoreCase("0x") || mask.length() < 2 || !mask.substring(0, 2).equalsIgnoreCase("0x");
        if (inValidParams) {
            return false;
        }
        if (!isEmptySimFile(data) || value.equalsIgnoreCase("0xFF")) {
            String valueString = value.substring(2);
            String maskString = mask.substring(2);
            String gid1String = IccUtils.bytesToHexString(data);
            if (maskString == null || gid1String == null || valueString == null || maskString.length() % 2 != 1) {
                isValidOddMask = false;
            }
            if (isValidOddMask) {
                int maskStringLength = maskString.length();
                int gid1StringLength = gid1String.length();
                int valueStringLength = valueString.length();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Gid1 length is odd ,Gid1 length:");
                stringBuilder.append(maskString.length());
                logd(stringBuilder.toString());
                if (gid1StringLength < maskStringLength || maskStringLength != valueStringLength) {
                    logd("Gid1 length is not match");
                    return false;
                }
                int i2 = 0;
                while (i2 < maskStringLength) {
                    if (maskString.charAt(i2) == 'F' && gid1String.charAt(i2) == valueString.charAt(i2)) {
                        i2++;
                    } else {
                        logd("Gid1 mask did not match");
                        return false;
                    }
                }
                for (i2 = maskStringLength; i2 < gid1StringLength; i2++) {
                    if (gid1String.charAt(i2) != 'f') {
                        logd("Gid1 string did not match");
                        return false;
                    }
                }
                return true;
            }
            byte[] valueBytes = IccUtils.hexStringToBytes(valueString);
            byte[] maskBytes = IccUtils.hexStringToBytes(maskString);
            if (valueBytes.length != maskBytes.length) {
                return false;
            }
            boolean match = true;
            while (i < maskBytes.length) {
                if ((data[i] & maskBytes[i]) != valueBytes[i]) {
                    match = false;
                }
                i++;
            }
            return match;
        }
        logd("matchByteWithMask data is null");
        return false;
    }

    private static boolean isEmptySimFile(byte[] gid1) {
        boolean isEmptyFile = true;
        for (byte gid1Byte : gid1) {
            if (gid1Byte != (byte) -1) {
                isEmptyFile = false;
            }
        }
        return isEmptyFile;
    }

    /* JADX WARNING: Missing block: B:8:0x000f, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected static boolean isImsiVirtualNet(String imsi, String tmpImsiStart) {
        if (imsi == null || tmpImsiStart == null || !imsi.startsWith(tmpImsiStart)) {
            return false;
        }
        return true;
    }

    private static void createVirtualNet(Cursor cursor, int slotId) {
        StringBuilder stringBuilder;
        if (!isMultiSimEnabled) {
            mVirtualNet = getVirtualNet(cursor, 0);
            stringBuilder = new StringBuilder();
            stringBuilder.append("createVirtualNet sigelcard mVirtualNet =");
            stringBuilder.append(mVirtualNet);
            logd(stringBuilder.toString());
        } else if (slotId == 1) {
            mVirtualNet1 = getVirtualNet(cursor, slotId);
            stringBuilder = new StringBuilder();
            stringBuilder.append("createVirtualNet slotId");
            stringBuilder.append(slotId);
            stringBuilder.append(" mVirtualNet1 =");
            stringBuilder.append(mVirtualNet1);
            logd(stringBuilder.toString());
        } else if (slotId == 0) {
            mVirtualNet = getVirtualNet(cursor, slotId);
            stringBuilder = new StringBuilder();
            stringBuilder.append("createVirtualNet slotId");
            stringBuilder.append(slotId);
            stringBuilder.append(" mVirtualNet =");
            stringBuilder.append(mVirtualNet);
            logd(stringBuilder.toString());
        }
    }

    private static VirtualNet getVirtualNet(Cursor cursor, int slotId) {
        Cursor cursor2 = cursor;
        int i = slotId;
        if (cursor2 == null) {
            return null;
        }
        String tmpNumeric = cursor2.getString(cursor2.getColumnIndex("numeric"));
        String tmpApnFilter = cursor2.getString(cursor2.getColumnIndex(VirtualNets.APN_FILTER));
        String tmpVoicemalNumber = cursor2.getString(cursor2.getColumnIndex(VirtualNets.VOICEMAIL_NUMBER));
        String tmpVoicemalTag = cursor2.getString(cursor2.getColumnIndex(VirtualNets.VOICEMAIL_TAG));
        int tmpNumMatch = cursor2.getInt(cursor2.getColumnIndex("num_match"));
        int tmpNumMatchShort = cursor2.getInt(cursor2.getColumnIndex("num_match_short"));
        int i2 = cursor2.getInt(cursor2.getColumnIndex("sms_7bit_enabled"));
        int tmpSmsCodingNational = cursor2.getInt(cursor2.getColumnIndex("sms_coding_national"));
        String tmpOperatorName = cursor2.getString(cursor2.getColumnIndex(VirtualNets.ONS_NAME));
        int tmpmaxmessagesize = cursor2.getInt(cursor2.getColumnIndex("max_message_size"));
        int tmpsmstommstextthreshold = cursor2.getInt(cursor2.getColumnIndex("sms_to_mms_textthreshold"));
        String tempEccWithCard = cursor2.getString(cursor2.getColumnIndex(VirtualNets.ECC_WITH_CARD));
        String tempEccNoCard = cursor2.getString(cursor2.getColumnIndex(VirtualNets.ECC_NO_CARD));
        String tmpVnKey = cursor2.getString(cursor2.getColumnIndex(VirtualNets.VN_KEY));
        VirtualNet virtualNet = null;
        String tmpApnFilter2;
        if (tmpNumeric == null || tmpNumeric.trim().length() <= 0) {
            tmpApnFilter2 = tmpApnFilter;
            String str = tmpVoicemalTag;
            tmpVnKey = str;
            int i3 = tmpNumMatch;
            int tmpNumMatch2 = i3;
            int i4 = tmpNumMatchShort;
            tmpSmsCodingNational = i4;
        } else {
            String tmpOperatorName2 = tmpOperatorName;
            int tmpSmsCodingNational2 = tmpSmsCodingNational;
            int tmpSms7BitEnabled = i2;
            String tmpVnKey2 = tmpVnKey;
            int tmpNumMatchShort2 = tmpNumMatchShort;
            int tmpNumMatch3 = tmpNumMatch;
            String tmpVoicemalTag2 = tmpVoicemalTag;
            String tmpVoicemalNumber2 = tmpVoicemalNumber;
            tmpApnFilter2 = tmpApnFilter;
            virtualNet = new VirtualNet(tmpNumeric, tmpApnFilter, tmpVoicemalNumber, tmpVoicemalTag, tmpNumMatch, tmpNumMatchShort, i2, tmpSmsCodingNational2, tmpOperatorName2, tmpmaxmessagesize, tmpsmstommstextthreshold, tempEccWithCard, tempEccNoCard);
            Phone phone = PhoneFactory.getDefaultPhone();
            ContentResolver contentResolver = phone.getContext().getContentResolver();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("numeric");
            stringBuilder.append(i);
            System.putString(contentResolver, stringBuilder.toString(), tmpNumeric);
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder = new StringBuilder();
            stringBuilder.append(VirtualNets.APN_FILTER);
            stringBuilder.append(i);
            System.putString(contentResolver, stringBuilder.toString(), tmpApnFilter2);
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder = new StringBuilder();
            stringBuilder.append(VirtualNets.VOICEMAIL_NUMBER);
            stringBuilder.append(i);
            System.putString(contentResolver, stringBuilder.toString(), tmpVoicemalNumber2);
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder = new StringBuilder();
            stringBuilder.append(VirtualNets.VOICEMAIL_TAG);
            stringBuilder.append(i);
            System.putString(contentResolver, stringBuilder.toString(), tmpVoicemalTag2);
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder = new StringBuilder();
            stringBuilder.append("num_match");
            stringBuilder.append(i);
            System.putInt(contentResolver, stringBuilder.toString(), tmpNumMatch3);
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder = new StringBuilder();
            stringBuilder.append("num_match_short");
            stringBuilder.append(i);
            System.putInt(contentResolver, stringBuilder.toString(), tmpNumMatchShort2);
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder = new StringBuilder();
            stringBuilder.append("sms_7bit_enabled");
            stringBuilder.append(i);
            System.putInt(contentResolver, stringBuilder.toString(), tmpSms7BitEnabled);
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder = new StringBuilder();
            stringBuilder.append("sms_coding_national");
            stringBuilder.append(i);
            System.putInt(contentResolver, stringBuilder.toString(), tmpSmsCodingNational2);
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder = new StringBuilder();
            stringBuilder.append(VirtualNets.ONS_NAME);
            stringBuilder.append(i);
            System.putString(contentResolver, stringBuilder.toString(), tmpOperatorName2);
            contentResolver = phone.getContext().getContentResolver();
            stringBuilder = new StringBuilder();
            stringBuilder.append(VirtualNets.VN_KEY);
            stringBuilder.append(i);
            System.putString(contentResolver, stringBuilder.toString(), tmpVnKey2);
        }
        VirtualNet virtualNet2 = virtualNet;
        Cursor cursor3 = cursor;
        int tmpVirtualNetRule = cursor3.getInt(cursor3.getColumnIndex(VirtualNets.VIRTUAL_NET_RULE));
        tmpVoicemalNumber = cursor3.getString(cursor3.getColumnIndex(VirtualNets.IMSI_START));
        if (tmpVirtualNetRule != 1 || tmpNumeric == null || !tmpNumeric.equals(tmpVoicemalNumber) || virtualNet2 == null) {
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getVirtualNet find a realNetwork tmpNumeric = ");
            stringBuilder2.append(tmpNumeric);
            logd(stringBuilder2.toString());
            virtualNet2.isRealNetwork = true;
            if (i == 1) {
                if (mVirtualNet1 == null) {
                    virtualNet2.plmnSameImsiStartCount = 1;
                } else {
                    virtualNet2.plmnSameImsiStartCount = mVirtualNet1.plmnSameImsiStartCount + 1;
                }
            } else if (mVirtualNet == null) {
                virtualNet2.plmnSameImsiStartCount = 1;
            } else {
                virtualNet2.plmnSameImsiStartCount = mVirtualNet.plmnSameImsiStartCount + 1;
            }
        }
        return virtualNet2;
    }

    public VirtualNet(String tmpNumeric, String tmpApnFilter, String tmpVoicemalNumber, String tmpVoicemalTag, int tmpNumMatch, int tmpNumMatchShort, int tmpSms7BitEnabled, int tmpSmsCodingNational, String tmpOperatorName, int tmpmaxmessagesize, int tmpsmstommstextthreshold) {
        this.numeric = tmpNumeric;
        this.apnFilter = tmpApnFilter;
        this.voicemailNumber = tmpVoicemalNumber;
        this.voicemailTag = tmpVoicemalTag;
        this.numMatch = tmpNumMatch;
        this.numMatchShort = tmpNumMatchShort;
        this.sms7BitEnabled = tmpSms7BitEnabled;
        this.smsCodingNational = tmpSmsCodingNational;
        this.operatorName = tmpOperatorName;
        this.maxMessageSize = tmpmaxmessagesize;
        this.smsToMmsTextThreshold = tmpsmstommstextthreshold;
    }

    public VirtualNet(String tmpNumeric, String tmpApnFilter, String tmpVoicemalNumber, String tmpVoicemalTag, int tmpNumMatch, int tmpNumMatchShort, int tmpSms7BitEnabled, int tmpSmsCodingNational, String tmpOperatorName, int tmpmaxmessagesize, int tmpsmstommstextthreshold, String tempEccWithCard, String tempEccNoCard) {
        this.numeric = tmpNumeric;
        this.apnFilter = tmpApnFilter;
        this.voicemailNumber = tmpVoicemalNumber;
        this.voicemailTag = tmpVoicemalTag;
        this.numMatch = tmpNumMatch;
        this.numMatchShort = tmpNumMatchShort;
        this.sms7BitEnabled = tmpSms7BitEnabled;
        this.smsCodingNational = tmpSmsCodingNational;
        this.operatorName = tmpOperatorName;
        this.maxMessageSize = tmpmaxmessagesize;
        this.smsToMmsTextThreshold = tmpsmstommstextthreshold;
        this.eccNoCard = tempEccNoCard;
        this.eccWithCard = tempEccWithCard;
    }

    public String getEccWithCard() {
        return this.eccWithCard;
    }

    public String getEccNoCard() {
        return this.eccNoCard;
    }

    public String getNumeric() {
        return this.numeric;
    }

    public static String getApnFilter() {
        VirtualNet virtualNet = getCurrentVirtualNet();
        if (virtualNet != null) {
            return virtualNet.apnFilter;
        }
        return null;
    }

    public static String getApnFilter(int slotId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getApnFilter, slotId=");
        stringBuilder.append(slotId);
        logd(stringBuilder.toString());
        VirtualNet virtualNet = getCurrentVirtualNet(slotId);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getApnFilter, virtualNet=");
        stringBuilder2.append(virtualNet);
        logd(stringBuilder2.toString());
        if (virtualNet == null) {
            return null;
        }
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getApnFilter, apnFilter=");
        stringBuilder2.append(virtualNet.apnFilter);
        logd(stringBuilder2.toString());
        return virtualNet.apnFilter;
    }

    public String getVoiceMailNumber() {
        return this.voicemailNumber;
    }

    public String getVoicemailTag() {
        return this.voicemailTag;
    }

    public int getNumMatch() {
        return this.numMatch;
    }

    public int getNumMatchShort() {
        return this.numMatchShort;
    }

    public int getSms7BitEnabled() {
        return this.sms7BitEnabled;
    }

    public int getSmsCodingNational() {
        return this.smsCodingNational;
    }

    public String getOperatorName() {
        return this.operatorName;
    }

    public int getMaxMessageSize() {
        return this.maxMessageSize;
    }

    public int getSmsToMmsTextThreshold() {
        return this.smsToMmsTextThreshold;
    }

    public boolean validNetConfig() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("validNetConfig isRealNetwork = ");
        stringBuilder.append(this.isRealNetwork);
        logd(stringBuilder.toString());
        boolean z = true;
        if (("26207".equals(this.numeric) || "23210".equals(this.numeric)) && this.isRealNetwork) {
            return this.isRealNetwork ^ 1;
        }
        if (!this.isRealNetwork) {
            return true;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("validNetConfig plmnSameImsiStartCount = ");
        stringBuilder.append(this.plmnSameImsiStartCount);
        logd(stringBuilder.toString());
        if (this.plmnSameImsiStartCount > 1) {
            z = false;
        }
        return z;
    }

    public static void saveUiccCardsToVirtualNet(UiccCard[] uiccCards) {
        mUiccCards = uiccCards;
    }

    private static boolean isCardPresent(int slotId) {
        boolean z = false;
        if (slotId < 0 || slotId >= MAX_PHONE_COUNT) {
            return false;
        }
        try {
            if (mUiccCards[slotId] != null && CardState.CARDSTATE_PRESENT == mUiccCards[slotId].getCardState()) {
                z = true;
            }
            return z;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean hasIccCard(int slotId) {
        boolean bRet = false;
        if (isMultiSimEnabled) {
            try {
                return isCardPresent(slotId);
            } catch (Exception e) {
                e.printStackTrace();
                loge("call isCardPresent got Exception", e);
                return false;
            }
        }
        try {
            bRet = isCardPresent(0);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("hasIccCard, bRet=");
            stringBuilder.append(bRet);
            stringBuilder.append(" for single card");
            logd(stringBuilder.toString());
            return bRet;
        } catch (Exception e2) {
            e2.printStackTrace();
            loge("call isCardPresent got Exception", e2);
            return bRet;
        }
    }

    public static String getOperatorKey(Context context) {
        if (!isMultiSimEnabled) {
            return getOperatorKey(context, 0);
        }
        String op_key = getOperatorKey(context, 0);
        if (TextUtils.isEmpty(op_key)) {
            return getOperatorKey(context, 1);
        }
        return op_key;
    }

    public static String getOperatorKey(Context context, int slotId) {
        if ((slotId != 0 && slotId != 1) || context == null) {
            return null;
        }
        String specialVnkey = context.getContentResolver();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(VirtualNets.VN_KEY_FOR_SPECIALIMSI);
        stringBuilder.append(slotId);
        specialVnkey = getStringFromSettingsEx(specialVnkey, stringBuilder.toString(), "");
        StringBuilder stringBuilder2;
        if (TextUtils.isEmpty(specialVnkey)) {
            ContentResolver contentResolver = context.getContentResolver();
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(VirtualNets.VN_KEY);
            stringBuilder3.append(slotId);
            String op_key = getStringFromSettingsEx(contentResolver, stringBuilder3.toString(), null);
            if (TextUtils.isEmpty(op_key)) {
                if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated(Integer.valueOf(slotId))) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(RoamingBroker.PreviousOperator);
                    stringBuilder2.append(slotId);
                    op_key = SystemProperties.get(stringBuilder2.toString(), "");
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("getOperatorKey, it is in roaming broker, op_key= ");
                    stringBuilder2.append(op_key);
                    stringBuilder2.append(", slotId: ");
                    stringBuilder2.append(slotId);
                    logd(stringBuilder2.toString());
                } else {
                    op_key = TelephonyManager.getDefault().getSimOperator(slotId);
                }
                return op_key;
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getOperatorKey, op_key= ");
            stringBuilder2.append(op_key);
            stringBuilder2.append(", slotId: ");
            stringBuilder2.append(slotId);
            logd(stringBuilder2.toString());
            return op_key;
        }
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getOperatorKey, specialVnkey = ");
        stringBuilder2.append(specialVnkey);
        stringBuilder2.append(", slotId: ");
        stringBuilder2.append(slotId);
        logd(stringBuilder2.toString());
        return specialVnkey;
    }

    public static FileReader getSpecialImsiFileReader() {
        File confFile = new File(FILE_FROM_SYSTEM_ETC_DIR);
        File sImsiFileCust = new File(FILE_FROM_CUST_DIR);
        File sImsiFile = new File(Environment.getRootDirectory(), PARAM_SPECIALIMSI_PATH);
        try {
            File cfg = HwCfgFilePolicy.getCfgFile(String.format("/xml/%s", new Object[]{SPECIAL_IMSI_CONFIG_FILE}), 0);
            if (cfg != null) {
                confFile = cfg;
                logd("load specialImsiList-conf.xml from HwCfgFilePolicy folder");
            } else if (sImsiFileCust.exists()) {
                confFile = sImsiFileCust;
                logd("load specialImsiList-conf.xml from cust folder");
            } else {
                confFile = sImsiFile;
                logd("load specialImsiList-conf.xml from etc folder");
            }
        } catch (NoClassDefFoundError e) {
            loge("NoClassDefFoundError : HwCfgFilePolicy ");
        }
        FileReader sImsiReader = null;
        try {
            return new FileReader(confFile);
        } catch (FileNotFoundException e2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can't open ");
            stringBuilder.append(Environment.getRootDirectory());
            stringBuilder.append("/");
            stringBuilder.append(PARAM_SPECIALIMSI_PATH);
            loge(stringBuilder.toString());
            return null;
        }
    }

    private static void loadSpecialImsiList() {
        StringBuilder stringBuilder;
        if (!specialImsiLoaded) {
            logd("loadSpecialImsiList begin!");
            mSpecialImsiList = new ArrayList();
            FileReader sImsiReader = getSpecialImsiFileReader();
            if (sImsiReader == null) {
                loge("loadSpecialImsiList failed!");
                return;
            }
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(sImsiReader);
                XmlUtils.beginDocument(parser, "specialImsiList");
                while (true) {
                    XmlUtils.nextElement(parser);
                    if (!"specialImsiList".equals(parser.getName())) {
                        break;
                    }
                    mSpecialImsiList.add(new String[]{parser.getAttributeValue(null, "imsiStart"), parser.getAttributeValue(null, "imsiEnd"), parser.getAttributeValue(null, "vnKey")});
                }
                if (sImsiReader != null) {
                    try {
                        sImsiReader.close();
                    } catch (IOException e) {
                        loge("IOException happen.close failed.");
                    }
                }
            } catch (XmlPullParserException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception in specialImsiList parser ");
                stringBuilder.append(e2);
                logd(stringBuilder.toString());
                if (sImsiReader != null) {
                    sImsiReader.close();
                }
            } catch (IOException e3) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception in specialImsiList parser ");
                stringBuilder.append(e3);
                logd(stringBuilder.toString());
                if (sImsiReader != null) {
                    sImsiReader.close();
                }
            } catch (Throwable th) {
                if (sImsiReader != null) {
                    try {
                        sImsiReader.close();
                    } catch (IOException e4) {
                        loge("IOException happen.close failed.");
                    }
                }
            }
            specialImsiLoaded = true;
        }
    }

    public static String getVnKeyFromSpecialIMSIList(String imsi) {
        if (TextUtils.isEmpty(imsi) || mSpecialImsiList.size() <= 0) {
            loge("imsi or mSpecialImsiList is empty, return null");
            return null;
        }
        Iterator<String[]> iter = mSpecialImsiList.iterator();
        while (iter.hasNext()) {
            String[] data = (String[]) iter.next();
            if (3 == data.length) {
                int compareImsiStart = imsi.compareTo(data[0]);
                int compareImsiEnd = imsi.compareTo(data[1]);
                if (compareImsiStart >= 0 && compareImsiEnd <= 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("return the special VnKey = ");
                    stringBuilder.append(data[2]);
                    logd(stringBuilder.toString());
                    return data[2];
                }
            }
        }
        return null;
    }
}
