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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ApnUtils {
    private static final String APN_ID = "apn_id";
    private static final String[] APN_KEYS = {KEY_ID, "name", KEY_APN, "proxy", "port", "mmsproxy", "mmsport", "server", "user", "password", "mmsc", KEY_MCC, KEY_MNC, KEY_NUMERIC, "authtype", "type", "ppppwd", "protocol", "carrier_enabled", "bearer", "roaming_protocol", "mvno_match_data", "mvno_type"};
    private static final String APN_OP_KEY = "opKey";
    private static final String APN_SUB_ID = "sub_id";
    private static final Uri APN_URI = Uri.parse("content://telephony/carriers");
    private static final boolean IS_APN_DEBUG = false;
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

    private ApnUtils() {
        Log.i(TAG, "Hide tool class public constructor ");
    }

    public static void addApn(ContentResolver cr, Map<String, String> apnInfo) {
        if (getApnSelectionArgs(apnInfo).length == 0) {
            Log.e(TAG, "addApn: apn params invalid");
        } else if (TextUtils.isEmpty(apnInfo.get("name"))) {
            Log.e(TAG, "addApn: name cannot be empty");
        } else if (TextUtils.isEmpty(apnInfo.get(KEY_APN))) {
            Log.e(TAG, "addApn: apn cannot be empty");
        } else {
            String mcc = apnInfo.get(KEY_MCC);
            if (mcc == null || !PATTERN_MCC.matcher(mcc).matches()) {
                Log.e(TAG, "addApn: mcc empty or invalid");
                return;
            }
            String mnc = apnInfo.get(KEY_MNC);
            if (mnc == null || !PATTERN_MNC.matcher(mnc).matches()) {
                Log.e(TAG, "addApn: mnc empty or invalid");
                return;
            }
            ContentValues contentValues = new ContentValues();
            for (Map.Entry<String, String> entry : apnInfo.entrySet()) {
                String key = entry.getKey();
                if (!KEY_ID.equals(key) && !KEY_NUMERIC.equals(key)) {
                    contentValues.put(key, entry.getValue());
                }
            }
            if (apnInfo.get("type") == null) {
                contentValues.put("type", MemoryConstant.MEM_SCENE_DEFAULT);
            }
            contentValues.put(KEY_NUMERIC, mcc + mnc);
            String opKey = HwCfgFilePolicy.getOpKey(SubscriptionManager.getDefaultSubscriptionId());
            if (!TextUtils.isEmpty(opKey)) {
                contentValues.put(APN_OP_KEY, opKey);
            }
            cr.insert(APN_URI, contentValues);
        }
    }

    public static void deleteApn(ContentResolver cr, String apnId) {
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
            for (Map.Entry<String, String> entry : apnInfo.entrySet()) {
                String key = entry.getKey();
                if (!KEY_ID.equals(key)) {
                    values.put(key, entry.getValue());
                }
            }
            cr.update(APN_URI, values, "_id=?", new String[]{apnId});
        }
    }

    public static List<String> queryApn(ContentResolver cr, Map<String, String> apnInfo) {
        String selection = getApnSelection(apnInfo);
        if (TextUtils.isEmpty(selection)) {
            Log.e(TAG, "queryApn: illegal query params");
            return null;
        }
        String[] selectionArgs = getApnSelectionArgs(apnInfo);
        if (selectionArgs.length == 0) {
            Log.e(TAG, "queryApn: apn params invalid");
            return null;
        }
        Cursor cursor = cr.query(APN_URI, null, selection, selectionArgs, null);
        if (cursor == null) {
            return null;
        }
        List<String> idlist = new ArrayList<>();
        while (cursor.moveToNext()) {
            idlist.add(cursor.getString(cursor.getColumnIndex(KEY_ID)));
        }
        cursor.close();
        return idlist;
    }

    public static Map<String, String> getApnInfo(ContentResolver cr, String apnId) {
        Cursor cursor = cr.query(APN_URI, null, "_id=?", new String[]{apnId}, null);
        if (cursor == null) {
            return null;
        }
        Map<String, String> apnInfo = new HashMap<>();
        String[] columns = cursor.getColumnNames();
        if (cursor.moveToNext()) {
            for (String field : columns) {
                apnInfo.put(field, cursor.getString(cursor.getColumnIndex(field)));
            }
        }
        cursor.close();
        return apnInfo;
    }

    public static void setPreferApn(ContentResolver cr, String apnId) {
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
                try {
                    subId = Integer.parseInt(apnInfo.get(APN_SUB_ID));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "subId : NumberFormatException");
                }
            }
            cr.update(ContentUris.withAppendedId(PREFERAPN_URI, (long) subId), values, null, null);
            return;
        }
        cr.update(PREFERAPN_URI, values, null, null);
    }

    private static String getApnSelection(Map<String, String> apnInfo) {
        String result = "";
        for (String key : apnInfo.keySet()) {
            if (isApnKey(key)) {
                if (result.length() > 0) {
                    result = result + " and ";
                }
                result = result + key + "=?";
            } else {
                Log.e(TAG, "invalid key: " + key);
                return null;
            }
        }
        return result;
    }

    private static boolean isApnKey(String key) {
        if (TextUtils.isEmpty(key)) {
            return false;
        }
        int i = 0;
        while (true) {
            String[] strArr = APN_KEYS;
            if (i >= strArr.length) {
                return false;
            }
            if (key.equals(strArr[i])) {
                return true;
            }
            i++;
        }
    }

    private static String[] getApnSelectionArgs(Map<String, String> apnInfo) {
        int i = 0;
        String[] results = new String[apnInfo.keySet().size()];
        for (Map.Entry<String, String> entry : apnInfo.entrySet()) {
            String key = entry.getKey();
            if (!isApnKey(key)) {
                Log.e(TAG, "invalid key: " + key);
                return new String[0];
            }
            results[i] = entry.getValue();
            if (results[i] == null) {
                Log.e(TAG, "null value for key: " + key);
                return new String[0];
            }
            i++;
        }
        return results;
    }
}
