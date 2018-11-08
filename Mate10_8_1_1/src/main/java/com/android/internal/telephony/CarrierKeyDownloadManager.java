package com.android.internal.telephony;

import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.telephony.CarrierConfigManager;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.android.org.bouncycastle.util.io.pem.PemReader;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CarrierKeyDownloadManager {
    private static final int[] CARRIER_KEY_TYPES = new int[]{1, 2};
    private static final int DAY_IN_MILLIS = 86400000;
    private static final int DEFAULT_RENEWAL_WINDOW_DAYS = 7;
    private static final String INTENT_KEY_RENEWAL_ALARM_PREFIX = "com.android.internal.telephony.carrier_key_download_alarm";
    private static final String JSON_CARRIER_KEYS = "carrier-keys";
    private static final String JSON_CERTIFICATE = "certificate";
    private static final String JSON_CERTIFICATE_ALTERNATE = "public-key";
    private static final String JSON_IDENTIFIER = "key-identifier";
    private static final String JSON_TYPE = "key-type";
    private static final String JSON_TYPE_VALUE_EPDG = "EPDG";
    private static final String JSON_TYPE_VALUE_WLAN = "WLAN";
    private static final String LOG_TAG = "CarrierKeyDownloadManager";
    public static final String MCC = "MCC";
    private static final String MCC_MNC_PREF_TAG = "CARRIER_KEY_DM_MCC_MNC";
    public static final String MNC = "MNC";
    private static final String SEPARATOR = ":";
    private static final int UNINITIALIZED_KEY_TYPE = -1;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int slotId = CarrierKeyDownloadManager.this.mPhone.getPhoneId();
            if (action.equals(CarrierKeyDownloadManager.INTENT_KEY_RENEWAL_ALARM_PREFIX + slotId)) {
                Log.d(CarrierKeyDownloadManager.LOG_TAG, "Handling key renewal alarm: " + action);
                CarrierKeyDownloadManager.this.handleAlarmOrConfigChange();
            } else if (action.equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                if (slotId == intent.getIntExtra("phone", -1)) {
                    Log.d(CarrierKeyDownloadManager.LOG_TAG, "Carrier Config changed: " + action);
                    CarrierKeyDownloadManager.this.handleAlarmOrConfigChange();
                }
            } else if (action.equals("android.intent.action.DOWNLOAD_COMPLETE")) {
                Log.d(CarrierKeyDownloadManager.LOG_TAG, "Download Complete");
                long carrierKeyDownloadIdentifier = intent.getLongExtra("extra_download_id", 0);
                String mccMnc = CarrierKeyDownloadManager.this.getMccMncSetFromPref();
                if (CarrierKeyDownloadManager.this.isValidDownload(mccMnc)) {
                    CarrierKeyDownloadManager.this.onDownloadComplete(carrierKeyDownloadIdentifier, mccMnc);
                    CarrierKeyDownloadManager.this.onPostDownloadProcessing(carrierKeyDownloadIdentifier);
                }
            }
        }
    };
    private final Context mContext;
    public final DownloadManager mDownloadManager;
    public int mKeyAvailability = 0;
    private final Phone mPhone;
    private String mURL;

    public CarrierKeyDownloadManager(Phone phone) {
        this.mPhone = phone;
        this.mContext = phone.getContext();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        filter.addAction("android.intent.action.DOWNLOAD_COMPLETE");
        filter.addAction(INTENT_KEY_RENEWAL_ALARM_PREFIX + this.mPhone.getPhoneId());
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter, null, phone);
        this.mDownloadManager = (DownloadManager) this.mContext.getSystemService("download");
    }

    private void onPostDownloadProcessing(long carrierKeyDownloadIdentifier) {
        resetRenewalAlarm();
        cleanupDownloadPreferences(carrierKeyDownloadIdentifier);
    }

    private void handleAlarmOrConfigChange() {
        if (!carrierUsesKeys()) {
            cleanupRenewalAlarms();
        } else if (areCarrierKeysAbsentOrExpiring() && !downloadKey()) {
            resetRenewalAlarm();
        }
    }

    private void cleanupDownloadPreferences(long carrierKeyDownloadIdentifier) {
        Log.d(LOG_TAG, "Cleaning up download preferences: " + carrierKeyDownloadIdentifier);
        Editor editor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        editor.remove(String.valueOf(carrierKeyDownloadIdentifier));
        editor.commit();
    }

    private void cleanupRenewalAlarms() {
        Log.d(LOG_TAG, "Cleaning up existing renewal alarms");
        ((AlarmManager) this.mContext.getSystemService("alarm")).cancel(PendingIntent.getBroadcast(this.mContext, 0, new Intent(INTENT_KEY_RENEWAL_ALARM_PREFIX + this.mPhone.getPhoneId()), 134217728));
    }

    public long getExpirationDate() {
        long minExpirationDate = Long.MAX_VALUE;
        for (int key_type : CARRIER_KEY_TYPES) {
            if (isKeyEnabled(key_type)) {
                ImsiEncryptionInfo imsiEncryptionInfo = this.mPhone.getCarrierInfoForImsiEncryption(key_type);
                if (!(imsiEncryptionInfo == null || imsiEncryptionInfo.getExpirationTime() == null || minExpirationDate <= imsiEncryptionInfo.getExpirationTime().getTime())) {
                    minExpirationDate = imsiEncryptionInfo.getExpirationTime().getTime();
                }
            }
        }
        if (minExpirationDate == Long.MAX_VALUE || minExpirationDate < System.currentTimeMillis() + 604800000) {
            return System.currentTimeMillis() + 86400000;
        }
        return minExpirationDate - 604800000;
    }

    public void resetRenewalAlarm() {
        cleanupRenewalAlarms();
        int slotId = this.mPhone.getPhoneId();
        long minExpirationDate = getExpirationDate();
        Log.d(LOG_TAG, "minExpirationDate: " + new Date(minExpirationDate));
        AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        Intent intent = new Intent(INTENT_KEY_RENEWAL_ALARM_PREFIX + slotId);
        alarmManager.set(2, minExpirationDate, PendingIntent.getBroadcast(this.mContext, 0, intent, 134217728));
        Log.d(LOG_TAG, "setRenewelAlarm: action=" + intent.getAction() + " time=" + new Date(minExpirationDate));
    }

    private String getMccMncSetFromPref() {
        return PreferenceManager.getDefaultSharedPreferences(this.mContext).getString(MCC_MNC_PREF_TAG + this.mPhone.getPhoneId(), null);
    }

    public String getSimOperator() {
        return ((TelephonyManager) this.mContext.getSystemService("phone")).getSimOperator(this.mPhone.getSubId());
    }

    public boolean isValidDownload(String mccMnc) {
        String mccCurrent = "";
        String mncCurrent = "";
        String mccSource = "";
        String mncSource = "";
        String simOperator = getSimOperator();
        if (TextUtils.isEmpty(simOperator) || TextUtils.isEmpty(mccMnc)) {
            Log.e(LOG_TAG, "simOperator or mcc/mnc is empty");
            return false;
        }
        String[] splitValue = mccMnc.split(SEPARATOR);
        mccSource = splitValue[0];
        mncSource = splitValue[1];
        Log.d(LOG_TAG, "values from sharedPrefs mcc, mnc: " + mccSource + "," + mncSource);
        mccCurrent = simOperator.substring(0, 3);
        mncCurrent = simOperator.substring(3);
        Log.d(LOG_TAG, "using values for mcc, mnc: " + mccCurrent + "," + mncCurrent);
        return TextUtils.equals(mncSource, mncCurrent) && TextUtils.equals(mccSource, mccCurrent);
    }

    private void onDownloadComplete(long carrierKeyDownloadIdentifier, String mccMnc) {
        Exception e;
        Throwable th;
        Log.d(LOG_TAG, "onDownloadComplete: " + carrierKeyDownloadIdentifier);
        Query query = new Query();
        query.setFilterById(new long[]{carrierKeyDownloadIdentifier});
        Cursor cursor = this.mDownloadManager.query(query);
        InputStream inputStream = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                if (8 == cursor.getInt(cursor.getColumnIndex("status"))) {
                    try {
                        InputStream source = new FileInputStream(this.mDownloadManager.openDownloadedFile(carrierKeyDownloadIdentifier).getFileDescriptor());
                        try {
                            parseJsonAndPersistKey(convertToString(source), mccMnc);
                            this.mDownloadManager.remove(new long[]{carrierKeyDownloadIdentifier});
                            try {
                                source.close();
                            } catch (IOException e2) {
                                e2.printStackTrace();
                            }
                            inputStream = source;
                        } catch (Exception e3) {
                            e = e3;
                            inputStream = source;
                            try {
                                Log.e(LOG_TAG, "Error in download:" + carrierKeyDownloadIdentifier + ". " + e);
                                this.mDownloadManager.remove(new long[]{carrierKeyDownloadIdentifier});
                                try {
                                    inputStream.close();
                                } catch (IOException e22) {
                                    e22.printStackTrace();
                                }
                                Log.d(LOG_TAG, "Completed downloading keys");
                                cursor.close();
                            } catch (Throwable th2) {
                                th = th2;
                                this.mDownloadManager.remove(new long[]{carrierKeyDownloadIdentifier});
                                try {
                                    inputStream.close();
                                } catch (IOException e222) {
                                    e222.printStackTrace();
                                }
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            inputStream = source;
                            this.mDownloadManager.remove(new long[]{carrierKeyDownloadIdentifier});
                            inputStream.close();
                            throw th;
                        }
                    } catch (Exception e4) {
                        e = e4;
                        Log.e(LOG_TAG, "Error in download:" + carrierKeyDownloadIdentifier + ". " + e);
                        this.mDownloadManager.remove(new long[]{carrierKeyDownloadIdentifier});
                        inputStream.close();
                        Log.d(LOG_TAG, "Completed downloading keys");
                        cursor.close();
                    }
                }
                Log.d(LOG_TAG, "Completed downloading keys");
            }
            cursor.close();
        }
    }

    private boolean carrierUsesKeys() {
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        if (carrierConfigManager == null) {
            return false;
        }
        PersistableBundle b = carrierConfigManager.getConfigForSubId(this.mPhone.getSubId());
        if (b == null) {
            return false;
        }
        this.mKeyAvailability = b.getInt("imsi_key_availability_int");
        this.mURL = b.getString("imsi_key_download_url_string");
        if (TextUtils.isEmpty(this.mURL) || this.mKeyAvailability == 0) {
            Log.d(LOG_TAG, "Carrier not enabled or invalid values");
            return false;
        }
        for (int key_type : CARRIER_KEY_TYPES) {
            if (isKeyEnabled(key_type)) {
                return true;
            }
        }
        return false;
    }

    private static String convertToString(InputStream is) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(is), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    return sb.toString();
                }
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void parseJsonAndPersistKey(String jsonStr, String mccMnc) {
        JSONException e;
        Exception e2;
        if (TextUtils.isEmpty(jsonStr) || TextUtils.isEmpty(mccMnc)) {
            Log.e(LOG_TAG, "jsonStr or mcc, mnc: is empty");
            return;
        }
        PemReader pemReader = null;
        try {
            String mcc = "";
            String mnc = "";
            String[] splitValue = mccMnc.split(SEPARATOR);
            mcc = splitValue[0];
            mnc = splitValue[1];
            JSONArray keys = new JSONObject(jsonStr).getJSONArray(JSON_CARRIER_KEYS);
            int i = 0;
            PemReader reader = null;
            while (i < keys.length()) {
                try {
                    String cert;
                    JSONObject key = keys.getJSONObject(i);
                    if (key.has(JSON_CERTIFICATE)) {
                        cert = key.getString(JSON_CERTIFICATE);
                    } else {
                        cert = key.getString(JSON_CERTIFICATE_ALTERNATE);
                    }
                    String typeString = key.getString(JSON_TYPE);
                    int type = -1;
                    if (typeString.equals(JSON_TYPE_VALUE_WLAN)) {
                        type = 2;
                    } else {
                        if (typeString.equals(JSON_TYPE_VALUE_EPDG)) {
                            type = 1;
                        }
                    }
                    String identifier = key.getString(JSON_IDENTIFIER);
                    PemReader pemReader2 = new PemReader(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(cert.getBytes()))));
                    Pair<PublicKey, Long> keyInfo = getKeyInformation(pemReader2.readPemObject().getContent());
                    pemReader2.close();
                    savePublicKey((PublicKey) keyInfo.first, type, identifier, ((Long) keyInfo.second).longValue(), mcc, mnc);
                    i++;
                    reader = pemReader2;
                } catch (JSONException e3) {
                    e = e3;
                    pemReader = reader;
                } catch (Exception e4) {
                    e2 = e4;
                    pemReader = reader;
                } catch (Throwable th) {
                    Throwable th2 = th;
                    pemReader = reader;
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e22) {
                    Log.e(LOG_TAG, "Exception getting certificate: " + e22);
                }
            }
            pemReader = reader;
        } catch (JSONException e5) {
            e = e5;
        } catch (Exception e6) {
            e22 = e6;
        }
        try {
            Log.e(LOG_TAG, "Exception getting certificate: " + e22);
            if (pemReader != null) {
                try {
                    pemReader.close();
                } catch (Exception e222) {
                    Log.e(LOG_TAG, "Exception getting certificate: " + e222);
                }
            }
        } catch (Throwable th3) {
            th2 = th3;
            if (pemReader != null) {
                try {
                    pemReader.close();
                } catch (Exception e2222) {
                    Log.e(LOG_TAG, "Exception getting certificate: " + e2222);
                }
            }
            throw th2;
        }
        Log.e(LOG_TAG, "Json parsing error: " + e.getMessage());
        if (pemReader != null) {
            try {
                pemReader.close();
            } catch (Exception e22222) {
                Log.e(LOG_TAG, "Exception getting certificate: " + e22222);
            }
        }
    }

    public boolean isKeyEnabled(int keyType) {
        if (((this.mKeyAvailability >> (keyType - 1)) & 1) == 1) {
            return true;
        }
        return false;
    }

    public boolean areCarrierKeysAbsentOrExpiring() {
        boolean z = true;
        for (int key_type : CARRIER_KEY_TYPES) {
            if (isKeyEnabled(key_type)) {
                ImsiEncryptionInfo imsiEncryptionInfo = this.mPhone.getCarrierInfoForImsiEncryption(key_type);
                if (imsiEncryptionInfo == null) {
                    Log.d(LOG_TAG, "Key not found for: " + key_type);
                    return true;
                }
                if (imsiEncryptionInfo.getExpirationTime().getTime() - System.currentTimeMillis() >= 604800000) {
                    z = false;
                }
                return z;
            }
        }
        return false;
    }

    private boolean downloadKey() {
        Log.d(LOG_TAG, "starting download from: " + this.mURL);
        String mcc = "";
        String mnc = "";
        String simOperator = getSimOperator();
        if (TextUtils.isEmpty(simOperator)) {
            Log.e(LOG_TAG, "mcc, mnc: is empty");
            return false;
        }
        mcc = simOperator.substring(0, 3);
        mnc = simOperator.substring(3);
        Log.d(LOG_TAG, "using values for mcc, mnc: " + mcc + "," + mnc);
        try {
            Request request = new Request(Uri.parse(this.mURL));
            request.setAllowedOverMetered(false);
            request.setVisibleInDownloadsUi(false);
            Long carrierKeyDownloadRequestId = Long.valueOf(this.mDownloadManager.enqueue(request));
            Editor editor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
            String mccMnc = mcc + SEPARATOR + mnc;
            int slotId = this.mPhone.getPhoneId();
            Log.d(LOG_TAG, "storing values in sharedpref mcc, mnc, days: " + mcc + "," + mnc + "," + carrierKeyDownloadRequestId);
            editor.putString(MCC_MNC_PREF_TAG + slotId, mccMnc);
            editor.commit();
            return true;
        } catch (Exception e) {
            Log.e(LOG_TAG, "exception trying to dowload key from url: " + this.mURL);
            return false;
        }
    }

    public static Pair<PublicKey, Long> getKeyInformation(byte[] certificate) throws Exception {
        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certificate));
        return new Pair(cert.getPublicKey(), Long.valueOf(cert.getNotAfter().getTime()));
    }

    public void savePublicKey(PublicKey publicKey, int type, String identifier, long expirationDate, String mcc, String mnc) {
        this.mPhone.setCarrierInfoForImsiEncryption(new ImsiEncryptionInfo(mcc, mnc, type, identifier, publicKey, new Date(expirationDate)));
    }
}
