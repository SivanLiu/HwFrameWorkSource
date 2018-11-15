package com.android.server.devicepolicy;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import huawei.cust.HwCfgFilePolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public class ApnUtils {
    private static final boolean APN_DEBUG = true;
    private static final String APN_ID = "apn_id";
    private static final String[] APN_KEYS = new String[]{KEY_ID, "name", KEY_APN, "proxy", "port", "mmsproxy", "mmsport", "server", "user", "password", "mmsc", KEY_MCC, KEY_MNC, KEY_NUMERIC, "authtype", "type", "ppppwd", "protocol", "carrier_enabled", "bearer", "roaming_protocol", "mvno_match_data", "mvno_type"};
    private static final String APN_OP_KEY = "opKey";
    private static final String APN_SUB_ID = "sub_id";
    private static final Uri APN_URI = Uri.parse("content://telephony/carriers");
    private static final String KEY_APN = "apn";
    private static final String KEY_ID = "_id";
    private static final String KEY_MCC = "mcc";
    private static final String KEY_MNC = "mnc";
    private static final String KEY_NAME = "name";
    private static final String KEY_NUMERIC = "numeric";
    private static final String KEY_TYPE = "type";
    private static final Pattern PATTERN_MCC = Pattern.compile("[0-9]{3}");
    private static final Pattern PATTERN_MNC = Pattern.compile("[0-9]{2,3}");
    private static final Uri PREFERAPN_URI = Uri.parse("content://telephony/carriers/preferapn");
    private static final String TAG = "ApnUtils";

    public static void addApn(ContentResolver cr, Map<String, String> apnInfo) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("addApn: ");
        stringBuilder.append(apnInfo);
        Log.i(str, stringBuilder.toString());
        if (getApnSelectionArgs(apnInfo).length == 0) {
            Log.e(TAG, "addApn: apn params invalid");
        } else if (TextUtils.isEmpty((String) apnInfo.get("name"))) {
            Log.e(TAG, "addApn: name cannot be empty");
        } else if (TextUtils.isEmpty((String) apnInfo.get(KEY_APN))) {
            Log.e(TAG, "addApn: apn cannot be empty");
        } else {
            String mcc = (String) apnInfo.get(KEY_MCC);
            if (mcc == null || !PATTERN_MCC.matcher(mcc).matches()) {
                Log.e(TAG, "addApn: mcc empty or invalid");
                return;
            }
            String mnc = (String) apnInfo.get(KEY_MNC);
            if (mnc == null || !PATTERN_MNC.matcher(mnc).matches()) {
                Log.e(TAG, "addApn: mnc empty or invalid");
                return;
            }
            String key;
            ContentValues contentValues = new ContentValues();
            for (Entry<String, String> entry : apnInfo.entrySet()) {
                key = (String) entry.getKey();
                if (!(KEY_ID.equals(key) || KEY_NUMERIC.equals(key))) {
                    contentValues.put(key, (String) entry.getValue());
                }
            }
            if (((String) apnInfo.get("type")) == null) {
                contentValues.put("type", MemoryConstant.MEM_SCENE_DEFAULT);
            }
            String str2 = KEY_NUMERIC;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(mcc);
            stringBuilder2.append(mnc);
            contentValues.put(str2, stringBuilder2.toString());
            key = HwCfgFilePolicy.getOpKey(SubscriptionManager.getDefaultSubscriptionId());
            if (!TextUtils.isEmpty(key)) {
                contentValues.put(APN_OP_KEY, key);
            }
            cr.insert(APN_URI, contentValues);
        }
    }

    public static void deleteApn(ContentResolver cr, String apnId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("deleteApn id:");
        stringBuilder.append(apnId);
        Log.i(str, stringBuilder.toString());
        if (TextUtils.isEmpty(apnId)) {
            Log.e(TAG, "deleteApn: apnId cannot be empty");
            return;
        }
        Map<String, String> apnInfo = getApnInfo(cr, apnId);
        if (apnInfo == null || apnInfo.isEmpty()) {
            Log.e(TAG, "deleteApn: apnId does not exist");
            return;
        }
        cr.delete(APN_URI, "_id=?", new String[]{apnId});
    }

    public static void updateApn(ContentResolver cr, Map<String, String> apnInfo, String apnId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateApn_map: ");
        stringBuilder.append(apnInfo);
        stringBuilder.append(", id=");
        stringBuilder.append(apnId);
        Log.i(str, stringBuilder.toString());
        if (TextUtils.isEmpty(apnId)) {
            Log.e(TAG, "updateApn: apnId cannot be empty");
            return;
        }
        Map<String, String> apn = getApnInfo(cr, apnId);
        if (apn == null || apn.isEmpty()) {
            Log.e(TAG, "updateApn: apnId does not exist");
        } else if (getApnSelectionArgs(apnInfo).length == 0) {
            Log.e(TAG, "updateApn: apn params invalid");
        } else {
            ContentValues values = new ContentValues();
            for (Entry<String, String> entry : apnInfo.entrySet()) {
                String key = (String) entry.getKey();
                if (!KEY_ID.equals(key)) {
                    values.put(key, (String) entry.getValue());
                }
            }
            cr.update(APN_URI, values, "_id=?", new String[]{apnId});
        }
    }

    public static List<String> queryApn(ContentResolver cr, Map<String, String> apnInfo) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("queryApn: ");
        stringBuilder.append(apnInfo);
        Log.i(str, stringBuilder.toString());
        str = getApnSelection(apnInfo);
        if (TextUtils.isEmpty(str)) {
            Log.e(TAG, "queryApn: illegal query params");
            return null;
        }
        String[] selectionArgs = getApnSelectionArgs(apnInfo);
        if (selectionArgs.length == 0) {
            Log.e(TAG, "queryApn: apn params invalid");
            return null;
        }
        Cursor cursor = cr.query(APN_URI, null, str, selectionArgs, null);
        if (cursor == null) {
            return null;
        }
        List<String> idlist = new ArrayList();
        while (cursor.moveToNext()) {
            idlist.add(cursor.getString(cursor.getColumnIndex(KEY_ID)));
        }
        cursor.close();
        return idlist;
    }

    public static Map<String, String> getApnInfo(ContentResolver cr, String apnId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getApnParameter id: ");
        stringBuilder.append(apnId);
        Log.i(str, stringBuilder.toString());
        String[] strArr = new String[1];
        int i = 0;
        strArr[0] = apnId;
        Cursor cursor = cr.query(APN_URI, null, "_id=?", strArr, null);
        if (cursor == null) {
            return null;
        }
        Map<String, String> apnInfo = new HashMap();
        String[] columns = cursor.getColumnNames();
        if (cursor.moveToNext()) {
            int length = columns.length;
            while (i < length) {
                String field = columns[i];
                apnInfo.put(field, cursor.getString(cursor.getColumnIndex(field)));
                i++;
            }
        }
        cursor.close();
        return apnInfo;
    }

    public static void setPreferApn(ContentResolver cr, String apnId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setPreferApn:");
        stringBuilder.append(apnId);
        Log.i(str, stringBuilder.toString());
        Map<String, String> apnInfo = getApnInfo(cr, apnId);
        if (apnInfo == null || apnInfo.isEmpty()) {
            Log.e(TAG, "set prefapn: apn does not exist");
            return;
        }
        ContentValues values = new ContentValues();
        values.put(APN_ID, apnId);
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            int subId = SubscriptionManager.getDefaultSubscriptionId();
            if (apnInfo.containsKey(APN_SUB_ID)) {
                subId = Integer.parseInt((String) apnInfo.get(APN_SUB_ID));
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("setPreferApn with sub ");
            stringBuilder2.append(subId);
            Log.i(str2, stringBuilder2.toString());
            cr.update(ContentUris.withAppendedId(PREFERAPN_URI, (long) subId), values, null, null);
        } else {
            cr.update(PREFERAPN_URI, values, null, null);
        }
    }

    private static String getApnSelection(Map<String, String> apnInfo) {
        String str;
        String result = "";
        for (String key : apnInfo.keySet()) {
            StringBuilder stringBuilder;
            if (isApnKey(key)) {
                if (result.length() > 0) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(result);
                    stringBuilder.append(" and ");
                    result = stringBuilder.toString();
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append(result);
                stringBuilder.append(key);
                stringBuilder.append("=?");
                result = stringBuilder.toString();
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("invalid key: ");
                stringBuilder.append(key);
                Log.e(str, stringBuilder.toString());
                return null;
            }
        }
        str = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("selection: ");
        stringBuilder2.append(result);
        Log.i(str, stringBuilder2.toString());
        return result;
    }

    private static boolean isApnKey(String key) {
        if (TextUtils.isEmpty(key)) {
            return false;
        }
        for (Object equals : APN_KEYS) {
            if (key.equals(equals)) {
                return true;
            }
        }
        return false;
    }

    private static String[] getApnSelectionArgs(Map<String, String> apnInfo) {
        String str;
        int i = 0;
        String[] results = new String[apnInfo.keySet().size()];
        for (Entry<String, String> entry : apnInfo.entrySet()) {
            String key = (String) entry.getKey();
            StringBuilder stringBuilder;
            if (isApnKey(key)) {
                results[i] = (String) entry.getValue();
                if (results[i] == null) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("null value for key: ");
                    stringBuilder.append(key);
                    Log.e(str, stringBuilder.toString());
                    return new String[0];
                }
                i++;
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("invalid key: ");
                stringBuilder.append(key);
                Log.e(str, stringBuilder.toString());
                return new String[0];
            }
        }
        str = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("selectionArgs: ");
        stringBuilder2.append(Arrays.toString(results));
        Log.i(str, stringBuilder2.toString());
        return results;
    }
}
