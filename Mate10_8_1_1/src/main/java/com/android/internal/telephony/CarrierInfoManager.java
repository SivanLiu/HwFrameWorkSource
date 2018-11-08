package com.android.internal.telephony;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.provider.Telephony.CarrierColumns;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import java.util.Date;

public class CarrierInfoManager {
    private static final String LOG_TAG = "CarrierInfoManager";

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static ImsiEncryptionInfo getCarrierInfoForImsiEncryption(int keyType, Context mContext) {
        String mcc = "";
        String mnc = "";
        String networkOperator = ((TelephonyManager) mContext.getSystemService("phone")).getNetworkOperator();
        if (TextUtils.isEmpty(networkOperator)) {
            Log.e(LOG_TAG, "Invalid networkOperator: " + networkOperator);
            return null;
        }
        mcc = networkOperator.substring(0, 3);
        mnc = networkOperator.substring(3);
        Log.i(LOG_TAG, "using values for mnc, mcc: " + mnc + "," + mcc);
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(CarrierColumns.CONTENT_URI, new String[]{"public_key", "expiration_time", "key_identifier"}, "mcc=? and mnc=? and key_type=?", new String[]{mcc, mnc, String.valueOf(keyType)}, null);
            if (cursor == null || (cursor.moveToFirst() ^ 1) != 0) {
                Log.d(LOG_TAG, "No rows found for keyType: " + keyType);
                if (cursor != null) {
                    cursor.close();
                }
                return null;
            }
            if (cursor.getCount() > 1) {
                Log.e(LOG_TAG, "More than 1 row found for the keyType: " + keyType);
            }
            String str = mcc;
            String str2 = mnc;
            int i = keyType;
            ImsiEncryptionInfo imsiEncryptionInfo = new ImsiEncryptionInfo(str, str2, i, cursor.getString(2), cursor.getBlob(0), new Date(cursor.getLong(1)));
            if (cursor != null) {
                cursor.close();
            }
            return imsiEncryptionInfo;
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "Bad arguments:" + e);
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e2) {
            Log.e(LOG_TAG, "Query failed:" + e2);
            if (cursor != null) {
                cursor.close();
            }
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static void updateOrInsertCarrierKey(ImsiEncryptionInfo imsiEncryptionInfo, Context mContext) {
        byte[] keyBytes = imsiEncryptionInfo.getPublicKey().getEncoded();
        ContentResolver mContentResolver = mContext.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put("mcc", imsiEncryptionInfo.getMcc());
        contentValues.put("mnc", imsiEncryptionInfo.getMnc());
        contentValues.put("key_type", Integer.valueOf(imsiEncryptionInfo.getKeyType()));
        contentValues.put("key_identifier", imsiEncryptionInfo.getKeyIdentifier());
        contentValues.put("public_key", keyBytes);
        contentValues.put("expiration_time", Long.valueOf(imsiEncryptionInfo.getExpirationTime().getTime()));
        try {
            Log.i(LOG_TAG, "Inserting imsiEncryptionInfo into db");
            mContentResolver.insert(CarrierColumns.CONTENT_URI, contentValues);
        } catch (SQLiteConstraintException e) {
            Log.i(LOG_TAG, "Insert failed, updating imsiEncryptionInfo into db");
            ContentValues updatedValues = new ContentValues();
            updatedValues.put("public_key", keyBytes);
            updatedValues.put("expiration_time", Long.valueOf(imsiEncryptionInfo.getExpirationTime().getTime()));
            updatedValues.put("key_identifier", imsiEncryptionInfo.getKeyIdentifier());
            try {
                if (mContentResolver.update(CarrierColumns.CONTENT_URI, updatedValues, "mcc=? and mnc=? and key_type=?", new String[]{imsiEncryptionInfo.getMcc(), imsiEncryptionInfo.getMnc(), String.valueOf(imsiEncryptionInfo.getKeyType())}) == 0) {
                    Log.d(LOG_TAG, "Error updating values:" + imsiEncryptionInfo);
                }
            } catch (Exception ex) {
                Log.d(LOG_TAG, "Error updating values:" + imsiEncryptionInfo + ex);
            }
        } catch (Exception e2) {
            Log.d(LOG_TAG, "Error inserting/updating values:" + imsiEncryptionInfo + e2);
        }
    }

    public static void setCarrierInfoForImsiEncryption(ImsiEncryptionInfo imsiEncryptionInfo, Context mContext) {
        Log.i(LOG_TAG, "inserting carrier key: " + imsiEncryptionInfo);
        updateOrInsertCarrierKey(imsiEncryptionInfo, mContext);
    }
}
