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
import android.content.SharedPreferences;
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
import com.android.internal.annotations.VisibleForTesting;
import com.android.org.bouncycastle.util.io.pem.PemReader;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CarrierKeyDownloadManager {
    private static final int[] CARRIER_KEY_TYPES = new int[]{1, 2};
    private static final int DAY_IN_MILLIS = 86400000;
    private static final int END_RENEWAL_WINDOW_DAYS = 7;
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
    private static final int START_RENEWAL_WINDOW_DAYS = 21;
    private static final int UNINITIALIZED_KEY_TYPE = -1;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int slotId = CarrierKeyDownloadManager.this.mPhone.getPhoneId();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(CarrierKeyDownloadManager.INTENT_KEY_RENEWAL_ALARM_PREFIX);
            stringBuilder.append(slotId);
            String str;
            StringBuilder stringBuilder2;
            if (action.equals(stringBuilder.toString())) {
                str = CarrierKeyDownloadManager.LOG_TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Handling key renewal alarm: ");
                stringBuilder2.append(action);
                Log.d(str, stringBuilder2.toString());
                CarrierKeyDownloadManager.this.handleAlarmOrConfigChange();
            } else if (action.equals("com.android.internal.telephony.ACTION_CARRIER_CERTIFICATE_DOWNLOAD")) {
                if (slotId == intent.getIntExtra("phone", -1)) {
                    str = CarrierKeyDownloadManager.LOG_TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Handling reset intent: ");
                    stringBuilder2.append(action);
                    Log.d(str, stringBuilder2.toString());
                    CarrierKeyDownloadManager.this.handleAlarmOrConfigChange();
                }
            } else if (action.equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                if (slotId == intent.getIntExtra("phone", -1)) {
                    str = CarrierKeyDownloadManager.LOG_TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Carrier Config changed: ");
                    stringBuilder2.append(action);
                    Log.d(str, stringBuilder2.toString());
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
    @VisibleForTesting
    public int mKeyAvailability = 0;
    private final Phone mPhone;
    private String mURL;

    public CarrierKeyDownloadManager(Phone phone) {
        this.mPhone = phone;
        this.mContext = phone.getContext();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        filter.addAction("android.intent.action.DOWNLOAD_COMPLETE");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(INTENT_KEY_RENEWAL_ALARM_PREFIX);
        stringBuilder.append(this.mPhone.getPhoneId());
        filter.addAction(stringBuilder.toString());
        filter.addAction("com.android.internal.telephony.ACTION_CARRIER_CERTIFICATE_DOWNLOAD");
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
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cleaning up download preferences: ");
        stringBuilder.append(carrierKeyDownloadIdentifier);
        Log.d(str, stringBuilder.toString());
        Editor editor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        editor.remove(String.valueOf(carrierKeyDownloadIdentifier));
        editor.commit();
    }

    private void cleanupRenewalAlarms() {
        Log.d(LOG_TAG, "Cleaning up existing renewal alarms");
        int slotId = this.mPhone.getPhoneId();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(INTENT_KEY_RENEWAL_ALARM_PREFIX);
        stringBuilder.append(slotId);
        PendingIntent carrierKeyDownloadIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(stringBuilder.toString()), 134217728);
        Context context = this.mContext;
        Context context2 = this.mContext;
        ((AlarmManager) context.getSystemService("alarm")).cancel(carrierKeyDownloadIntent);
    }

    @VisibleForTesting
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
        return minExpirationDate - ((long) (new Random().nextInt(1814400000 - 604800000) + 604800000));
    }

    @VisibleForTesting
    public void resetRenewalAlarm() {
        cleanupRenewalAlarms();
        int slotId = this.mPhone.getPhoneId();
        long minExpirationDate = getExpirationDate();
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("minExpirationDate: ");
        stringBuilder.append(new Date(minExpirationDate));
        Log.d(str, stringBuilder.toString());
        AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(INTENT_KEY_RENEWAL_ALARM_PREFIX);
        stringBuilder2.append(slotId);
        Intent intent = new Intent(stringBuilder2.toString());
        alarmManager.set(2, minExpirationDate, PendingIntent.getBroadcast(this.mContext, 0, intent, 134217728));
        String str2 = LOG_TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("setRenewelAlarm: action=");
        stringBuilder3.append(intent.getAction());
        stringBuilder3.append(" time=");
        stringBuilder3.append(new Date(minExpirationDate));
        Log.d(str2, stringBuilder3.toString());
    }

    private String getMccMncSetFromPref() {
        int slotId = this.mPhone.getPhoneId();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(MCC_MNC_PREF_TAG);
        stringBuilder.append(slotId);
        return preferences.getString(stringBuilder.toString(), null);
    }

    @VisibleForTesting
    public String getSimOperator() {
        return ((TelephonyManager) this.mContext.getSystemService("phone")).getSimOperator(this.mPhone.getSubId());
    }

    @VisibleForTesting
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
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("values from sharedPrefs mcc, mnc: ");
        stringBuilder.append(mccSource);
        stringBuilder.append(",");
        stringBuilder.append(mncSource);
        Log.d(str, stringBuilder.toString());
        mccCurrent = simOperator.substring(0, 3);
        mncCurrent = simOperator.substring(3);
        str = LOG_TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("using values for mcc, mnc: ");
        stringBuilder.append(mccCurrent);
        stringBuilder.append(",");
        stringBuilder.append(mncCurrent);
        Log.d(str, stringBuilder.toString());
        if (TextUtils.equals(mncSource, mncCurrent) && TextUtils.equals(mccSource, mccCurrent)) {
            return true;
        }
        return false;
    }

    private void onDownloadComplete(long carrierKeyDownloadIdentifier, String mccMnc) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onDownloadComplete: ");
        stringBuilder.append(carrierKeyDownloadIdentifier);
        Log.d(str, stringBuilder.toString());
        Query query = new Query();
        query.setFilterById(new long[]{carrierKeyDownloadIdentifier});
        Cursor cursor = this.mDownloadManager.query(query);
        InputStream source = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                if (8 == cursor.getInt(cursor.getColumnIndex("status"))) {
                    try {
                        source = new FileInputStream(this.mDownloadManager.openDownloadedFile(carrierKeyDownloadIdentifier).getFileDescriptor());
                        parseJsonAndPersistKey(convertToString(source), mccMnc);
                        this.mDownloadManager.remove(new long[]{carrierKeyDownloadIdentifier});
                        try {
                            source.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (Exception e2) {
                        String str2 = LOG_TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Error in download:");
                        stringBuilder2.append(carrierKeyDownloadIdentifier);
                        stringBuilder2.append(". ");
                        stringBuilder2.append(e2);
                        Log.e(str2, stringBuilder2.toString());
                        this.mDownloadManager.remove(new long[]{carrierKeyDownloadIdentifier});
                        source.close();
                    } catch (Throwable th) {
                        this.mDownloadManager.remove(new long[]{carrierKeyDownloadIdentifier});
                        try {
                            source.close();
                        } catch (IOException e3) {
                            e3.printStackTrace();
                        }
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
                String readLine = reader.readLine();
                String line = readLine;
                if (readLine == null) {
                    return sb.toString();
                }
                sb.append(line);
                sb.append(10);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:75:0x015a=Splitter:B:75:0x015a, B:66:0x012f=Splitter:B:66:0x012f} */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x018a A:{SYNTHETIC, Splitter:B:85:0x018a} */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x0176 A:{SYNTHETIC, Splitter:B:78:0x0176} */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x0147 A:{SYNTHETIC, Splitter:B:69:0x0147} */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x018a A:{SYNTHETIC, Splitter:B:85:0x018a} */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x0176 A:{SYNTHETIC, Splitter:B:78:0x0176} */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x0147 A:{SYNTHETIC, Splitter:B:69:0x0147} */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x018a A:{SYNTHETIC, Splitter:B:85:0x018a} */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x0176 A:{SYNTHETIC, Splitter:B:78:0x0176} */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x0147 A:{SYNTHETIC, Splitter:B:69:0x0147} */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x018a A:{SYNTHETIC, Splitter:B:85:0x018a} */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x0176 A:{SYNTHETIC, Splitter:B:78:0x0176} */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x0147 A:{SYNTHETIC, Splitter:B:69:0x0147} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @VisibleForTesting
    public void parseJsonAndPersistKey(String jsonStr, String mccMnc) {
        JSONException e;
        String str;
        StringBuilder stringBuilder;
        Exception e2;
        Exception exception;
        Throwable th;
        PemReader reader;
        Throwable th2;
        String str2;
        String str3;
        if (TextUtils.isEmpty(jsonStr) || TextUtils.isEmpty(mccMnc)) {
            str2 = jsonStr;
            str3 = mccMnc;
            Log.e(LOG_TAG, "jsonStr or mcc, mnc: is empty");
            return;
        }
        PemReader reader2 = null;
        try {
            int i;
            String mcc;
            String mnc;
            String mcc2 = "";
            str2 = "";
            try {
                String[] splitValue = mccMnc.split(SEPARATOR);
                i = 0;
                mcc = splitValue[0];
                mnc = splitValue[1];
            } catch (JSONException e3) {
                e = e3;
                str2 = jsonStr;
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Json parsing error: ");
                stringBuilder.append(e.getMessage());
                Log.e(str, stringBuilder.toString());
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (Exception e4) {
                        e2 = e4;
                        exception = e2;
                        str = LOG_TAG;
                        stringBuilder = new StringBuilder();
                    }
                }
            } catch (Exception e5) {
                e2 = e5;
                str2 = jsonStr;
                try {
                    str = LOG_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception getting certificate: ");
                    stringBuilder.append(e2);
                    Log.e(str, stringBuilder.toString());
                    if (reader2 != null) {
                        try {
                            reader2.close();
                        } catch (Exception e6) {
                            e2 = e6;
                            exception = e2;
                            str = LOG_TAG;
                            stringBuilder = new StringBuilder();
                        }
                    }
                } catch (Throwable th3) {
                    th = th3;
                    reader = reader2;
                    th2 = th;
                    if (reader != null) {
                    }
                    throw th2;
                }
            } catch (Throwable th4) {
                th = th4;
                str2 = jsonStr;
                reader = reader2;
                th2 = th;
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e22) {
                        Exception exception2 = e22;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Exception getting certificate: ");
                        stringBuilder.append(e22);
                        Log.e(LOG_TAG, stringBuilder.toString());
                    }
                }
                throw th2;
            }
            try {
                JSONObject jsonObj = new JSONObject(jsonStr);
                JSONArray keys = jsonObj.getJSONArray(JSON_CARRIER_KEYS);
                while (i < keys.length()) {
                    String cert;
                    JSONObject key = keys.getJSONObject(i);
                    if (key.has(JSON_CERTIFICATE)) {
                        cert = key.getString(JSON_CERTIFICATE);
                    } else {
                        cert = key.getString(JSON_CERTIFICATE_ALTERNATE);
                    }
                    String cert2 = cert;
                    String typeString = key.getString(JSON_TYPE);
                    int type = -1;
                    if (typeString.equals(JSON_TYPE_VALUE_WLAN)) {
                        type = 2;
                    } else if (typeString.equals(JSON_TYPE_VALUE_EPDG)) {
                        type = 1;
                    }
                    int type2 = type;
                    String identifier = key.getString(JSON_IDENTIFIER);
                    ByteArrayInputStream inStream = new ByteArrayInputStream(cert2.getBytes());
                    Reader fReader = new BufferedReader(new InputStreamReader(inStream));
                    reader2 = new PemReader(fReader);
                    PemReader reader3;
                    try {
                        Pair<PublicKey, Long> keyInfo = getKeyInformation(reader2.readPemObject().getContent());
                        reader2.close();
                        JSONObject jsonObj2 = jsonObj;
                        PublicKey publicKey = (PublicKey) keyInfo.first;
                        reader3 = reader2;
                        try {
                            PublicKey publicKey2 = publicKey;
                            savePublicKey(publicKey2, type2, identifier, ((Long) keyInfo.second).longValue(), mcc, mnc);
                            i++;
                            jsonObj = jsonObj2;
                            reader2 = reader3;
                        } catch (JSONException e7) {
                            e = e7;
                            reader2 = reader3;
                            str = LOG_TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Json parsing error: ");
                            stringBuilder.append(e.getMessage());
                            Log.e(str, stringBuilder.toString());
                            if (reader2 != null) {
                            }
                        } catch (Exception e8) {
                            e22 = e8;
                            reader2 = reader3;
                            str = LOG_TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Exception getting certificate: ");
                            stringBuilder.append(e22);
                            Log.e(str, stringBuilder.toString());
                            if (reader2 != null) {
                            }
                        } catch (Throwable th5) {
                            th2 = th5;
                            reader = reader3;
                            if (reader != null) {
                            }
                            throw th2;
                        }
                    } catch (JSONException e9) {
                        e = e9;
                        reader3 = reader2;
                        str = LOG_TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Json parsing error: ");
                        stringBuilder.append(e.getMessage());
                        Log.e(str, stringBuilder.toString());
                        if (reader2 != null) {
                        }
                    } catch (Exception e10) {
                        e22 = e10;
                        reader3 = reader2;
                        str = LOG_TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Exception getting certificate: ");
                        stringBuilder.append(e22);
                        Log.e(str, stringBuilder.toString());
                        if (reader2 != null) {
                        }
                    } catch (Throwable th52) {
                        reader3 = reader2;
                        th2 = th52;
                        reader = reader3;
                        if (reader != null) {
                        }
                        throw th2;
                    }
                }
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (Exception e11) {
                        e22 = e11;
                        exception = e22;
                        str = LOG_TAG;
                        stringBuilder = new StringBuilder();
                    }
                }
            } catch (JSONException e12) {
                e = e12;
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Json parsing error: ");
                stringBuilder.append(e.getMessage());
                Log.e(str, stringBuilder.toString());
                if (reader2 != null) {
                }
            } catch (Exception e13) {
                e22 = e13;
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception getting certificate: ");
                stringBuilder.append(e22);
                Log.e(str, stringBuilder.toString());
                if (reader2 != null) {
                }
            }
        } catch (JSONException e14) {
            e = e14;
            str2 = jsonStr;
            str3 = mccMnc;
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Json parsing error: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            if (reader2 != null) {
            }
        } catch (Exception e15) {
            e22 = e15;
            str2 = jsonStr;
            str3 = mccMnc;
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception getting certificate: ");
            stringBuilder.append(e22);
            Log.e(str, stringBuilder.toString());
            if (reader2 != null) {
            }
        } catch (Throwable th6) {
            th52 = th6;
            str2 = jsonStr;
            str3 = mccMnc;
            reader = reader2;
            th2 = th52;
            if (reader != null) {
            }
            throw th2;
        }
        stringBuilder.append("Exception getting certificate: ");
        stringBuilder.append(e22);
        Log.e(str, stringBuilder.toString());
    }

    @VisibleForTesting
    public boolean isKeyEnabled(int keyType) {
        if (((this.mKeyAvailability >> (keyType - 1)) & 1) == 1) {
            return true;
        }
        return false;
    }

    @VisibleForTesting
    public boolean areCarrierKeysAbsentOrExpiring() {
        for (int key_type : CARRIER_KEY_TYPES) {
            if (isKeyEnabled(key_type)) {
                ImsiEncryptionInfo imsiEncryptionInfo = this.mPhone.getCarrierInfoForImsiEncryption(key_type);
                boolean z = true;
                if (imsiEncryptionInfo == null) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Key not found for: ");
                    stringBuilder.append(key_type);
                    Log.d(str, stringBuilder.toString());
                    return true;
                }
                if (imsiEncryptionInfo.getExpirationTime().getTime() - System.currentTimeMillis() >= 1814400000) {
                    z = false;
                }
                return z;
            }
        }
        return false;
    }

    private boolean downloadKey() {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("starting download from: ");
        stringBuilder.append(this.mURL);
        Log.d(str, stringBuilder.toString());
        str = "";
        String mnc = "";
        String simOperator = getSimOperator();
        if (TextUtils.isEmpty(simOperator)) {
            Log.e(LOG_TAG, "mcc, mnc: is empty");
            return false;
        }
        str = simOperator.substring(0, 3);
        mnc = simOperator.substring(3);
        String str2 = LOG_TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("using values for mcc, mnc: ");
        stringBuilder2.append(str);
        stringBuilder2.append(",");
        stringBuilder2.append(mnc);
        Log.d(str2, stringBuilder2.toString());
        try {
            Request request = new Request(Uri.parse(this.mURL));
            request.setAllowedOverMetered(false);
            request.setVisibleInDownloadsUi(false);
            request.setNotificationVisibility(2);
            Long carrierKeyDownloadRequestId = Long.valueOf(this.mDownloadManager.enqueue(request));
            Editor editor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
            String mccMnc = new StringBuilder();
            mccMnc.append(str);
            mccMnc.append(SEPARATOR);
            mccMnc.append(mnc);
            mccMnc = mccMnc.toString();
            int slotId = this.mPhone.getPhoneId();
            String str3 = LOG_TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("storing values in sharedpref mcc, mnc, days: ");
            stringBuilder3.append(str);
            stringBuilder3.append(",");
            stringBuilder3.append(mnc);
            stringBuilder3.append(",");
            stringBuilder3.append(carrierKeyDownloadRequestId);
            Log.d(str3, stringBuilder3.toString());
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append(MCC_MNC_PREF_TAG);
            stringBuilder4.append(slotId);
            editor.putString(stringBuilder4.toString(), mccMnc);
            editor.commit();
            return true;
        } catch (Exception e) {
            String str4 = LOG_TAG;
            StringBuilder stringBuilder5 = new StringBuilder();
            stringBuilder5.append("exception trying to dowload key from url: ");
            stringBuilder5.append(this.mURL);
            Log.e(str4, stringBuilder5.toString());
            return false;
        }
    }

    @VisibleForTesting
    public static Pair<PublicKey, Long> getKeyInformation(byte[] certificate) throws Exception {
        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certificate));
        return new Pair(cert.getPublicKey(), Long.valueOf(cert.getNotAfter().getTime()));
    }

    @VisibleForTesting
    public void savePublicKey(PublicKey publicKey, int type, String identifier, long expirationDate, String mcc, String mnc) {
        this.mPhone.setCarrierInfoForImsiEncryption(new ImsiEncryptionInfo(mcc, mnc, type, identifier, publicKey, new Date(expirationDate)));
    }
}
