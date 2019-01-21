package com.android.internal.telephony;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.os.UserHandle;
import android.provider.Telephony.CarrierColumns;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import java.util.Date;

public class CarrierInfoManager {
    private static final String KEY_TYPE = "KEY_TYPE";
    private static final String LOG_TAG = "CarrierInfoManager";
    private static final int RESET_CARRIER_KEY_RATE_LIMIT = 43200000;
    private long mLastAccessResetCarrierKey = 0;

    /* JADX WARNING: Unknown top exception splitter block from list: {B:55:0x0118=Splitter:B:55:0x0118, B:50:0x00fb=Splitter:B:50:0x00fb} */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x0135  */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x0135  */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x0135  */
    /* JADX WARNING: Missing block: B:52:0x0111, code skipped:
            if (r1 != null) goto L_0x0113;
     */
    /* JADX WARNING: Missing block: B:53:0x0113, code skipped:
            r1.close();
     */
    /* JADX WARNING: Missing block: B:57:0x012e, code skipped:
            if (r1 != null) goto L_0x0113;
     */
    /* JADX WARNING: Missing block: B:59:0x0132, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static ImsiEncryptionInfo getCarrierInfoForImsiEncryption(int keyType, Context context) {
        IllegalArgumentException e;
        Exception e2;
        int i = keyType;
        String mcc = "";
        String mnc = "";
        String simOperator = ((TelephonyManager) context.getSystemService("phone")).getSimOperator();
        String str;
        StringBuilder stringBuilder;
        if (TextUtils.isEmpty(simOperator)) {
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid networkOperator: ");
            stringBuilder.append(simOperator);
            Log.e(str, stringBuilder.toString());
            return null;
        }
        String mcc2 = simOperator.substring(0, 3);
        String mnc2 = simOperator.substring(3);
        mcc = LOG_TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("using values for mnc, mcc: ");
        stringBuilder2.append(mnc2);
        stringBuilder2.append(",");
        stringBuilder2.append(mcc2);
        Log.i(mcc, stringBuilder2.toString());
        Cursor findCursor = null;
        try {
            Cursor findCursor2;
            Cursor findCursor3 = context.getContentResolver().query(CarrierColumns.CONTENT_URI, new String[]{"public_key", "expiration_time", "key_identifier"}, "mcc=? and mnc=? and key_type=?", new String[]{mcc2, mnc2, String.valueOf(keyType)}, null);
            if (findCursor3 != null) {
                try {
                    if (findCursor3.moveToFirst()) {
                        if (findCursor3.getCount() > 1) {
                            try {
                                mcc = LOG_TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("More than 1 row found for the keyType: ");
                                stringBuilder2.append(i);
                                Log.e(mcc, stringBuilder2.toString());
                            } catch (IllegalArgumentException e3) {
                                e = e3;
                                findCursor = findCursor3;
                            } catch (Exception e4) {
                                e2 = e4;
                                mnc = findCursor3;
                                try {
                                    str = LOG_TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Query failed:");
                                    stringBuilder.append(e2);
                                    Log.e(str, stringBuilder.toString());
                                } catch (Throwable th) {
                                    mcc = th;
                                    if (mnc != null) {
                                    }
                                    throw mcc;
                                }
                            } catch (Throwable th2) {
                                mcc = th2;
                                mnc = findCursor3;
                                if (mnc != null) {
                                }
                                throw mcc;
                            }
                        }
                        byte[] carrier_key = findCursor3.getBlob(0);
                        Date expirationTime = new Date(findCursor3.getLong(1));
                        ImsiEncryptionInfo imsiEncryptionInfo = imsiEncryptionInfo;
                        findCursor2 = findCursor3;
                        try {
                            imsiEncryptionInfo = new ImsiEncryptionInfo(mcc2, mnc2, i, findCursor3.getString(2), carrier_key, expirationTime);
                            if (findCursor2 != null) {
                                findCursor2.close();
                            }
                            return imsiEncryptionInfo;
                        } catch (IllegalArgumentException e5) {
                            e = e5;
                            findCursor = findCursor2;
                        } catch (Exception e6) {
                            e2 = e6;
                            mnc = findCursor2;
                            str = LOG_TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Query failed:");
                            stringBuilder.append(e2);
                            Log.e(str, stringBuilder.toString());
                        } catch (Throwable th3) {
                            mcc = th3;
                            mnc = findCursor2;
                            if (mnc != null) {
                            }
                            throw mcc;
                        }
                    }
                    findCursor2 = findCursor3;
                } catch (IllegalArgumentException e7) {
                    e = e7;
                    findCursor = findCursor3;
                    str = LOG_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Bad arguments:");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                } catch (Exception e8) {
                    e2 = e8;
                    mnc = findCursor3;
                    str = LOG_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Query failed:");
                    stringBuilder.append(e2);
                    Log.e(str, stringBuilder.toString());
                } catch (Throwable th4) {
                    mcc = th4;
                    mnc = findCursor3;
                    if (mnc != null) {
                        mnc.close();
                    }
                    throw mcc;
                }
            }
            findCursor2 = findCursor3;
            mcc = LOG_TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("No rows found for keyType: ");
            stringBuilder2.append(i);
            Log.d(mcc, stringBuilder2.toString());
            if (findCursor2 != null) {
                findCursor2.close();
            }
            return null;
        } catch (IllegalArgumentException e9) {
            e = e9;
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Bad arguments:");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        } catch (Exception e10) {
            e2 = e10;
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Query failed:");
            stringBuilder.append(e2);
            Log.e(str, stringBuilder.toString());
        }
    }

    public static void updateOrInsertCarrierKey(ImsiEncryptionInfo imsiEncryptionInfo, Context context, int phoneId) {
        byte[] keyBytes = imsiEncryptionInfo.getPublicKey().getEncoded();
        ContentResolver mContentResolver = context.getContentResolver();
        TelephonyMetrics tm = TelephonyMetrics.getInstance();
        ContentValues contentValues = new ContentValues();
        contentValues.put("mcc", imsiEncryptionInfo.getMcc());
        contentValues.put("mnc", imsiEncryptionInfo.getMnc());
        contentValues.put("key_type", Integer.valueOf(imsiEncryptionInfo.getKeyType()));
        contentValues.put("key_identifier", imsiEncryptionInfo.getKeyIdentifier());
        contentValues.put("public_key", keyBytes);
        contentValues.put("expiration_time", Long.valueOf(imsiEncryptionInfo.getExpirationTime().getTime()));
        boolean downloadSuccessfull = true;
        try {
            Log.i(LOG_TAG, "Inserting imsiEncryptionInfo into db");
            mContentResolver.insert(CarrierColumns.CONTENT_URI, contentValues);
        } catch (SQLiteConstraintException e) {
            Log.i(LOG_TAG, "Insert failed, updating imsiEncryptionInfo into db");
            ContentValues updatedValues = new ContentValues();
            updatedValues.put("public_key", keyBytes);
            updatedValues.put("expiration_time", Long.valueOf(imsiEncryptionInfo.getExpirationTime().getTime()));
            updatedValues.put("key_identifier", imsiEncryptionInfo.getKeyIdentifier());
            String str;
            StringBuilder stringBuilder;
            try {
                if (mContentResolver.update(CarrierColumns.CONTENT_URI, updatedValues, "mcc=? and mnc=? and key_type=?", new String[]{imsiEncryptionInfo.getMcc(), imsiEncryptionInfo.getMnc(), String.valueOf(imsiEncryptionInfo.getKeyType())}) == 0) {
                    str = LOG_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error updating values:");
                    stringBuilder.append(imsiEncryptionInfo);
                    Log.d(str, stringBuilder.toString());
                    downloadSuccessfull = false;
                }
            } catch (Exception ex) {
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error updating values:");
                stringBuilder.append(imsiEncryptionInfo);
                stringBuilder.append(ex);
                Log.d(str, stringBuilder.toString());
                downloadSuccessfull = false;
            }
        } catch (Exception ex2) {
            String str2 = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Error inserting/updating values:");
            stringBuilder2.append(imsiEncryptionInfo);
            stringBuilder2.append(ex2);
            Log.d(str2, stringBuilder2.toString());
            downloadSuccessfull = false;
        } catch (Throwable th) {
            tm.writeCarrierKeyEvent(phoneId, imsiEncryptionInfo.getKeyType(), downloadSuccessfull);
        }
        tm.writeCarrierKeyEvent(phoneId, imsiEncryptionInfo.getKeyType(), downloadSuccessfull);
    }

    public static void setCarrierInfoForImsiEncryption(ImsiEncryptionInfo imsiEncryptionInfo, Context context, int phoneId) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("inserting carrier key: ");
        stringBuilder.append(imsiEncryptionInfo);
        Log.i(str, stringBuilder.toString());
        updateOrInsertCarrierKey(imsiEncryptionInfo, context, phoneId);
    }

    public void resetCarrierKeysForImsiEncryption(Context context, int mPhoneId) {
        Log.i(LOG_TAG, "resetting carrier key");
        long now = System.currentTimeMillis();
        if (now - this.mLastAccessResetCarrierKey < 43200000) {
            Log.i(LOG_TAG, "resetCarrierKeysForImsiEncryption: Access rate exceeded");
            return;
        }
        this.mLastAccessResetCarrierKey = now;
        deleteCarrierInfoForImsiEncryption(context);
        Intent resetIntent = new Intent("com.android.internal.telephony.ACTION_CARRIER_CERTIFICATE_DOWNLOAD");
        resetIntent.putExtra("phone", mPhoneId);
        context.sendBroadcastAsUser(resetIntent, UserHandle.ALL);
    }

    public static void deleteCarrierInfoForImsiEncryption(Context context) {
        Log.i(LOG_TAG, "deleting carrier key from db");
        String mcc = "";
        String mnc = "";
        String simOperator = ((TelephonyManager) context.getSystemService("phone")).getSimOperator();
        if (TextUtils.isEmpty(simOperator)) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid networkOperator: ");
            stringBuilder.append(simOperator);
            Log.e(str, stringBuilder.toString());
            return;
        }
        mcc = simOperator.substring(0, 3);
        mnc = simOperator.substring(3);
        ContentResolver mContentResolver = context.getContentResolver();
        try {
            String[] whereArgs = new String[]{mcc, mnc};
            mContentResolver.delete(CarrierColumns.CONTENT_URI, "mcc=? and mnc=?", whereArgs);
        } catch (Exception e) {
            String str2 = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Delete failed");
            stringBuilder2.append(e);
            Log.e(str2, stringBuilder2.toString());
        }
    }

    public static void deleteAllCarrierKeysForImsiEncryption(Context context) {
        Log.i(LOG_TAG, "deleting ALL carrier keys from db");
        try {
            context.getContentResolver().delete(CarrierColumns.CONTENT_URI, null, null);
        } catch (Exception e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Delete failed");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        }
    }
}
